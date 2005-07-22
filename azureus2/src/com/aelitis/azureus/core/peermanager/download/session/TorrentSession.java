/*
 * Created on Jul 5, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.peermanager.download.session;

import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue;
import com.aelitis.azureus.core.peermanager.connection.AZPeerConnection;
import com.aelitis.azureus.core.peermanager.download.TorrentDownload;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;
import com.aelitis.azureus.core.peermanager.messaging.azureus.session.*;


public class TorrentSession {
  private static int next_session_id = 0;
  private static final AEMonitor session_mon = new AEMonitor( "TorrentSession" );

  private int local_session_id;
  private int remote_session_id;
  private TorrentDownload download;
  private AZPeerConnection connection;
  private TimerEvent syn_timeout_timer;

  
  private final IncomingMessageQueue.MessageQueueListener incoming_q_listener = new IncomingMessageQueue.MessageQueueListener() {
    public boolean messageReceived( Message message ) {
      //ID_AZ_SESSION_ACK
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_ACK ) ) {          
        AZSessionAck ack = (AZSessionAck)message;
        if( Arrays.equals( ack.getInfoHash(), download.getInfoHash() ) ) {
          remote_session_id = ack.getSessionID();  //capture send-to id
          syn_timeout_timer.cancel();  //abort timeout check
          try{
            download.getSessionAuthenticator().verifySessionAck( ack.getSessionInfo() );
            startSessionProcessing();
          }
          catch( AuthenticatorException ae ) {
            endSession( "AuthenticatorException:: " +ae.getMessage() );
          }
          ack.destroy();
          return true;
        }
      }
      
      //ID_AZ_SESSION_END
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_END ) ) {          
        AZSessionEnd end = (AZSessionEnd)message;
        if( Arrays.equals( end.getInfoHash(), download.getInfoHash() ) ) {
          System.out.println( "AZ_TORRENT_SESSION_END received: " +end.getEndReason() );          
          destroy();  //close session       
          end.destroy();
          return true;
        } 
      }
      
      //ID_AZ_SESSION_BITFIELD
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_BITFIELD ) ) {          
        AZSessionBitfield bitf = (AZSessionBitfield)message;
        if( bitf.getSessionID() == local_session_id ) {
          download.receivedSessionBitfield( TorrentSession.this, bitf.getBitfield() );       
          bitf.destroy();
          return true;
        }
      }
      
      //ID_AZ_SESSION_REQUEST
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_REQUEST ) ) {          
        AZSessionRequest req = (AZSessionRequest)message;
        if( req.getSessionID() == local_session_id ) {
          download.receivedSessionRequest( TorrentSession.this, req.getUnchokeID(), req.getPieceNumber(), req.getPieceOffset(), req.getLength() );
          req.destroy();
          return true;
        }
      }
      
      //ID_AZ_SESSION_CANCEL
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_CANCEL ) ) {          
        AZSessionCancel cancel = (AZSessionCancel)message;
        if( cancel.getSessionID() == local_session_id ) {
          download.receivedSessionCancel( TorrentSession.this, cancel.getPieceNumber(), cancel.getPieceOffset(), cancel.getLength() );
          cancel.destroy();
          return true;
        }
      }
      
      //ID_AZ_SESSION_HAVE
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_HAVE ) ) {          
        AZSessionHave have = (AZSessionHave)message;
        if( have.getSessionID() == local_session_id ) {
          int[] piecenums = have.getPieceNumbers();
          for( int i=0; i < piecenums.length; i++ ) {
            download.receivedSessionHave( TorrentSession.this, piecenums[i] );
          }     
          have.destroy();
          return true;
        }
      }
      
      //ID_AZ_SESSION_PIECE
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_PIECE ) ) {          
        AZSessionPiece piece = (AZSessionPiece)message;
        if( piece.getSessionID() == local_session_id ) {
          try{
            DirectByteBuffer data = download.getSessionAuthenticator().decodeSessionData( piece.getPieceData() );
            download.receivedSessionPiece( TorrentSession.this, piece.getPieceNumber(), piece.getPieceOffset(), data );
          }
          catch( AuthenticatorException ae ) {
            piece.getPieceData().returnToPool();
            endSession( "AuthenticatorException:: " +ae.getMessage() );
          }
          piece.destroy();
          return true;
        }
      }

      return false;
    }

    public void protocolBytesReceived( int byte_count ){}
    public void dataBytesReceived( int byte_count ){}
  };
  
  
  
  
  //INCOMING SESSION INIT
  protected TorrentSession( TorrentDownload download, AZPeerConnection peer, int remote_id ) {
    init( download, peer, remote_id );  
  }
  
  
  //OUTGOING SESSION INIT
  protected TorrentSession( TorrentDownload download, AZPeerConnection peer ) {
    init( download, peer, -1 );
  }

  
  
  private void init( TorrentDownload d, AZPeerConnection p, int r ) {
    this.connection = p;
    this.download = d;
    this.remote_session_id = r;
    
    try{ session_mon.enter();
      local_session_id = next_session_id;
      next_session_id++;
    }
    finally{ session_mon.exit();  }
    
    connection.getNetworkConnection().getIncomingMessageQueue().registerQueueListener( incoming_q_listener );
  }
  
  

  protected void authenticate( Map incoming_syn ) {
    if( incoming_syn == null ) {  //outgoing session
      //send out the session request
      AZSessionSyn syn = new AZSessionSyn( download.getInfoHash(), local_session_id, download.getSessionAuthenticator().createSessionSyn() );
      connection.getNetworkConnection().getOutgoingMessageQueue().addMessage( syn, false );
      
      //set a timeout timer in case the other peer forgets to send an ACK or END reply
      syn_timeout_timer = SimpleTimer.addEvent( SystemTime.getCurrentTime() + 60*1000, new TimerEventPerformer() {
        public void  perform( TimerEvent  event ) {
          endSession( "no session ACK received after 60sec, request timed out" );
        }
      });
    }
    else {  //incoming session
      try{
        Map ack_reply = download.getSessionAuthenticator().verifySessionSyn( incoming_syn );
        
        //send out the session acceptance
        AZSessionAck ack = new AZSessionAck( download.getInfoHash(), local_session_id, ack_reply );
        connection.getNetworkConnection().getOutgoingMessageQueue().addMessage( ack, false );
        
        startSessionProcessing();
      }
      catch( AuthenticatorException ae ) {  //on syn authentication error
        endSession( "AuthenticatorException:: " +ae.getMessage() );
      }      
    }
  }
  
  

  private void endSession( String end_reason ){
    System.out.println( "endSession:: " +end_reason );
    
    //send end notice
    AZSessionEnd end = new AZSessionEnd( download.getInfoHash(), end_reason );
    connection.getNetworkConnection().getOutgoingMessageQueue().addMessage( end, false );
    
    destroy();
  }

  
  
  private void destroy(){
    if( syn_timeout_timer != null )  syn_timeout_timer.cancel();  //abort timeout check if running
    connection.getNetworkConnection().getIncomingMessageQueue().cancelQueueListener( incoming_q_listener );
    stopSessionProcessing();
  }
  
  
  
  private void startSessionProcessing() {
    download.registerTorrentSession( this );
    //TODO ?
  }
  
  
  
  private void stopSessionProcessing() {
    download.deregisterTorrentSession( this );
    //TODO ?
  }
  

  
}
