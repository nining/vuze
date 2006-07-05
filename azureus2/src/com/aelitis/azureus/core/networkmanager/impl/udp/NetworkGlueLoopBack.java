/*
 * Created on 22 Jun 2006
 * Created by Paul Gardner
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl.udp;

import java.util.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerException;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;
import com.aelitis.net.udp.uc.PRUDPPrimordialHandler;

public class 
NetworkGlueLoopBack
	implements NetworkGlue, PRUDPPrimordialHandler
 
{
	private boolean	UDP_TEST	= true;
	private int latency			= 0;
	
	private NetworkGlueListener		listener;

	private PRUDPPacketHandler handler;
	
	private List	message_queue	= new ArrayList();
	
	private Random	random = new Random();
	
	protected
	NetworkGlueLoopBack(
		NetworkGlueListener		_listener,
		int						_udp_port )
	{
		listener	= _listener;
				
		handler = PRUDPPacketHandlerFactory.getHandler( _udp_port );

		handler.setPrimordialHandler( this );
		
		COConfigurationManager.addListener(
			new COConfigurationListener()
			{
				public void
				configurationSaved()
				{
					int	port = UDPNetworkManager.getSingleton().getUDPListeningPortNumber();
					
					if ( port != handler.getPort()){
						
						handler = PRUDPPacketHandlerFactory.getHandler( port );

						handler.setPrimordialHandler( NetworkGlueLoopBack.this );

					}
				}
			});
				
		new AEThread( "NetworkGlueLoopBack", true )
		{
			public void
			runSupport()
			{
				while( true ){
					
					try{
						Thread.sleep(1);
						
					}catch( Throwable e ){
						
					}
				
					InetSocketAddress	target_address 	= null;
					InetSocketAddress	source_address 	= null;
					byte[]				data			= null;
					
					long	now = SystemTime.getCurrentTime();
					
					synchronized( message_queue ){
						
						if ( message_queue.size() > 0 ){
							
							Object[]	entry = (Object[])message_queue.get(0);
							
							if (((Long)entry[0]).longValue() < now ){
								
								message_queue.remove(0);
								
								source_address	= (InetSocketAddress)entry[1];
								target_address 	= (InetSocketAddress)entry[2];
								data			= (byte[])entry[3];
							}
						}
					}
					
					if ( source_address != null ){
						
						if ( UDP_TEST ){
							
							try{
								handler.primordialSend( data, target_address );
								
							}catch( PRUDPPacketHandlerException	e ){
								
								e.printStackTrace();
							}
						}else{
							listener.receive( target_address.getPort(), source_address, data, data.length );
						}
					}
				}
			}
		}.start();
	}
	
	public boolean
	packetReceived(
		DatagramPacket	packet )
	{
		if ( packet.getLength() >= 12 ){
								
			byte[]	data = packet.getData();
			
				// first or third word must have something set in mask: 0xfffff800
			
			if ( 	(	( data[0] & 0xff ) != 0 ||
						( data[1] & 0xff ) != 0 ||
						( data[2] & 0xf8 ) != 0 ) &&
					
					(	( data[8] & 0xff ) != 0 ||
						( data[9] & 0xff ) != 0 ||
						( data[10]& 0xf8 ) != 0 )){
				
				return( listener.receive( handler.getPort(), new InetSocketAddress( packet.getAddress(), packet.getPort()), packet.getData(), packet.getLength()));
			}
		}
		
		return( false );
	}
	
	public int
	send(
		int					local_port,
		InetSocketAddress	target,
		byte[]				data )
	
		throws IOException
	{	
		if ( UDP_TEST && latency == 0 ){
			
			try{
				handler.primordialSend( data, target );
				
			}catch( PRUDPPacketHandlerException	e ){
				
				throw( new IOException( e.getMessage()));
			}
			
		}else{
			Long	expires = new Long( SystemTime.getCurrentTime() + latency );
			
			InetSocketAddress local_address = new InetSocketAddress( target.getAddress(), local_port );
			
			synchronized( message_queue ){
				
				if ( random.nextInt(10) != 0 ){
					
					message_queue.add( new Object[]{ expires, local_address, target, data });
				}
			}
		}
		
		return( data.length );
	}
}
