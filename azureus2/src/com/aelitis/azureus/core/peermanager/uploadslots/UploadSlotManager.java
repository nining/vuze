/*
 * Created on Jul 15, 2006
 * Created by Alon Rohter
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package com.aelitis.azureus.core.peermanager.uploadslots;

import java.util.*;



/**
 * 
 */
public class UploadSlotManager {
	
	private static final int EXPIRE_NORMAL			= 1;   //1 round = 10sec upload
	private static final int EXPIRE_OPTIMISTIC	= 3;   //3 rounds = 30sec upload
	private static final int EXPIRE_SEED				= 6;   //6 rounds = 60sec upload
	
	
	public static boolean AUTO_SLOT_ENABLE = false;	//TODO
	
	
	
	private static final UploadSlotManager instance = new UploadSlotManager();
	
	public static UploadSlotManager getSingleton() {  return instance;  }
	
	
	private final UploadSessionPicker picker = new UploadSessionPicker();
	
	//init with empty slots, optimistic first in line
	private final UploadSlot[] slots = new UploadSlot[] {	new UploadSlot( UploadSlot.TYPE_OPTIMISTIC ),  //TODO dynamic # of slots
																												new UploadSlot( UploadSlot.TYPE_NORMAL ),
																												new UploadSlot( UploadSlot.TYPE_NORMAL ),
																												new UploadSlot( UploadSlot.TYPE_NORMAL ) };
	

	
	private long current_round = 0;
	
	
	
	private UploadSlotManager() {
		/*nothing*/
	}
	
	
	
	public void registerHelper( UploadHelper helper ) {
		if( AUTO_SLOT_ENABLE ) {
			picker.registerHelper( helper );
		}
	}
	
	
	public void deregisterHelper( UploadHelper helper ) {
		if( AUTO_SLOT_ENABLE ) {
			picker.deregisterHelper( helper );
		}
	}
	
	
	/**
	 * Notify of helper state change (i.e. priority changed)
	 * @param helper
	 */
	public void updateHelper( UploadHelper helper ) {
		if( AUTO_SLOT_ENABLE ) {
			picker.updateHelper( helper );
		}
	}
	
	
	
	
	public void process() {
		
		if( !AUTO_SLOT_ENABLE )  return;
		
		current_round++;
		
		ArrayList to_stop = new ArrayList();
		
		//get a list of the best sessions, peers who are uploading to us in download mode
		LinkedList best_sessions = picker.pickBestDownloadSessions( slots.length );
		
		//go through all currently expired slots and pick sessions for next round
		for( int i=0; i < slots.length; i++ ) {
			UploadSlot slot = slots[i];
			
			if( slot.getExpireRound() <= current_round ) {  //expired			
				UploadSession session = slot.getSession();
				
				if( session != null ) {
					to_stop.add( session );  //make sure it gets stopped
					slot.setSession( null );  //clear slot					
				}
				
				if( slot.getSlotType() == UploadSlot.TYPE_OPTIMISTIC ) {		//optimistic
					//pick new session for optimistic upload
					session = pickOptSession();
					
					if( session == null ) {
						continue;
					}
					
					if( session.getSessionType() == UploadSession.TYPE_SEED ) {  //place first seed session in a normal slot
						best_sessions.addFirst( session );  //put at front of good list to ensure it gets picked						
						//pick a new optimistic session, whatever type
						session = pickOptSession();						
						if( session == null )  continue;
					}
					
					slot.setSession( session );  //place the new session in the slot
					slot.setExpireRound( current_round + EXPIRE_OPTIMISTIC );  //set the new expire time
				}
				else {   //normal					
					if( best_sessions.isEmpty() ) {  //no download mode peers, must be only seeding				
						session = pickOptSession();
						if( session == null )  continue;
					}
					else {
						session = (UploadSession)best_sessions.removeFirst();
						//TODO check if this session is already in a slot somewhere
					}
					
					slot.setSession( session );  //place the session in the slot
					slot.setExpireRound( current_round + ( session.getSessionType() == UploadSession.TYPE_SEED ? EXPIRE_SEED : EXPIRE_NORMAL ) );  //set the new expire time
				}
				
			}
		}
				
		//start and stop sessions for the round
	
		//filter out sessions allowed to continue another round, so we don't stop-start them
		for( Iterator it = to_stop.iterator(); it.hasNext(); ) {
			UploadSession stop_s = (UploadSession)it.next();
			
			for( int i=0; i < slots.length; i++ ) {
				if( stop_s.isSameSession( slots[i].getSession() ) ) {  //need to do this because two session objects can represent the same peer
					it.remove();
					break;
				}
			}		
		}
		
		//stop discontinued sessions
		for( Iterator it = to_stop.iterator(); it.hasNext(); ) {  
			UploadSession session = (UploadSession)it.next();
			session.stop();
		}

		//ensure sessions are started
		for( int i=0; i < slots.length; i++ ) {
			UploadSession s = slots[i].getSession();
			if( s != null )  s.start();
		}
		
		printSlotStats();		
	}
	
	
	
	private UploadSession pickOptSession() {
		
		UploadSession session = picker.pickNextOptimisticSession();
		
		if( session == null )  return null;   //no sessions to pick
		
		if( !isAlreadySlotted( session ) )  return session;
		
		return pickOptSession();  //oops, we already picked this session for a slot, so pick another
	}
	
	
	
	
	private boolean isAlreadySlotted( UploadSession session ) {
		for( int i=0; i < slots.length; i++ ) {
			UploadSession s = slots[i].getSession();
			if( s != null && s.isSameSession( session ) )  return true;			
		}
		
		return false;
	}
	
	
	
	
	private void printSlotStats() {		
		System.out.println( "\nUPLOAD SLOTS [" +current_round+ "x]:" );
		
		for( int i=0; i < slots.length; i++ ) {
			UploadSlot slot = slots[i];
			
			System.out.print( "[" +i+ "]: " );
			
			String slot_type = slot.getSlotType() == UploadSlot.TYPE_NORMAL ? "NORM" : "OPTI";
			
			long rem = slot.getExpireRound() - current_round;
			String remaining = rem < 0 ? "" : " [" +rem+ "]rr";		
			
			String ses_trace = slot.getSession() == null ? "EMPTY" : slot.getSession().getStatsTrace();
			
			System.out.println( slot_type + remaining+ " : " +ses_trace );			
		}		
	}
	
	
	
	

}
