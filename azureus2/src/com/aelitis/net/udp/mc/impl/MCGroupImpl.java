/*
 * Created on 14-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
*  AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.net.udp.mc.impl;

import java.net.*;
import java.util.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.pluginsimpl.local.utils.UTTimerImpl;

import com.aelitis.net.udp.mc.MCGroup;
import com.aelitis.net.udp.mc.MCGroupAdapter;
import com.aelitis.net.udp.mc.MCGroupException;


/**
 * @author parg
 *
 */

public class 
MCGroupImpl 
	implements MCGroup
{
	private static final LogIDs LOGID = LogIDs.NET;
	
	private final static int		TTL					= 4;
	
	private final static int		PACKET_SIZE		= 8192;
			

	private static Map			singletons	= new HashMap();
	private static AEMonitor	class_mon 	= new AEMonitor( "MCGroup:class" );

	public static MCGroupImpl
	getSingleton(
		MCGroupAdapter		adapter,
		String				group_address,
		int					group_port,
		int					control_port )
	
		throws MCGroupException
	{
		try{
			class_mon.enter();
		
			String	key = group_address + ":" + group_port + ":" + control_port;
			
			MCGroupImpl	singleton = (MCGroupImpl)singletons.get( key );
			
			if ( singleton == null ){
				
				singleton = new MCGroupImpl( adapter, group_address, group_port, control_port );
				
				singletons.put( key, singleton );
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	private MCGroupAdapter		adapter;
	
	private String				group_address_str;
	private int					group_port;
	private int					control_port;
	private InetSocketAddress 	group_address;

	private boolean		ttl_problem_reported	= true;	// remove these diagnostic reports on win98
	private boolean		sso_problem_reported	= true; // remove these diagnostic reports on win98
		
	protected AEMonitor		this_mon	= new AEMonitor( "MCGroup" );

	private Map	current_registrations = new HashMap();
	
	public
	MCGroupImpl(
		MCGroupAdapter		_adapter,
		String				_group_address,
		int					_group_port,
		int					_control_port )
	
		throws MCGroupException
	{	
		adapter	= _adapter;

		group_address_str	= _group_address;
		group_port			= _group_port;
		control_port		= _control_port;
			
		try{	
			group_address = new InetSocketAddress(InetAddress.getByName(group_address_str), 0 );

			processNetworkInterfaces( true );
		
			UTTimer timer = new UTTimerImpl( "MCGroup:refresher", true );
			
			timer.addPeriodicEvent(
				60*1000,
				new UTTimerEventPerformer()
				{
					public void 
					perform(
						UTTimerEvent event )
					{
						try{
							processNetworkInterfaces( false );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				});
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			throw( new MCGroupException( "Failed to initialise MCGroup", e ));
		}
	}
	
	protected void
	processNetworkInterfaces(
		boolean		log_ignored )
	
		throws SocketException
	{
		Map			new_registrations	= new HashMap();
		
		try{
			this_mon.enter();
			
			Enumeration network_interfaces = NetworkInterface.getNetworkInterfaces();
			
			while (network_interfaces.hasMoreElements()){
				
				final NetworkInterface network_interface = (NetworkInterface)network_interfaces.nextElement();
	
				Set old_address_set = (Set)current_registrations.get( network_interface );
					
				if ( old_address_set == null ){
				
					old_address_set	= new HashSet();
				}
				
				Set	new_address_set = new HashSet();
				
				new_registrations.put( network_interface, new_address_set );
				
				Enumeration ni_addresses = network_interface.getInetAddresses();
				
				while (ni_addresses.hasMoreElements()){
					
					final InetAddress ni_address = (InetAddress)ni_addresses.nextElement();
	
					new_address_set.add( ni_address );

					if ( old_address_set.contains( ni_address )){
								
							// already established
						
						continue;
					}
						// turn on loopback to see if it helps for local host UPnP devices
						// nah, turn it off again, it didn;t
					
					if ( ni_address.isLoopbackAddress()){
						
						if ( log_ignored ){
							
							adapter.trace( "ignoring loopback address " + ni_address );
						}
						
						continue;
					}
					
					if ( ni_address instanceof Inet6Address ){
			
						if ( log_ignored ){
							
							adapter.trace( "ignoring IPv6 address " + ni_address );
						}
						
						continue;
					}
										
					try{
							// set up group
						
						final MulticastSocket mc_sock = new MulticastSocket( group_port );
										
						mc_sock.setReuseAddress(true);
						
							// windows 98 doesn't support setTimeToLive
						
						try{
							mc_sock.setTimeToLive(TTL);
							
						}catch( Throwable e ){
							
							if ( !ttl_problem_reported ){
								
								ttl_problem_reported	= true;
								
								Debug.printStackTrace( e );
							}
						}
						
						String	addresses_string = "";
							
						Enumeration it = network_interface.getInetAddresses();
						
						while (it.hasMoreElements()){
							
							InetAddress addr = (InetAddress)it.nextElement();
							
							addresses_string += (addresses_string.length()==0?"":",") + addr;
						}
						
						adapter.trace( "group = " + group_address +"/" + 
										network_interface.getName()+":"+ 
										network_interface.getDisplayName() + "-" + addresses_string +": started" );
						
						mc_sock.joinGroup( group_address, network_interface );
					
						mc_sock.setNetworkInterface( network_interface );
						
							// note that false ENABLES loopback mode which is what we want 
						
						mc_sock.setLoopbackMode(false);
											
						Runtime.getRuntime().addShutdownHook(
								new AEThread("MCGroup:VMShutdown")
								{
									public void
									runSupport()
									{
										try{
											mc_sock.leaveGroup( group_address, network_interface );
											
										}catch( Throwable e ){
											
											Debug.printStackTrace( e );
										}
									}
								});
						
						new AEThread("MCGroup:MCListener", true )
							{
								public void
								runSupport()
								{
									handleSocket( network_interface, ni_address, mc_sock );
								}
							}.start();
						
					}catch( Throwable e ){
						
						Debug.printStackTrace( e );
					}						
				
						// now do the incoming control listener
					
					try{
						final DatagramSocket control_socket = new DatagramSocket( null );
							
						control_socket.setReuseAddress( true );
							
						control_socket.bind( new InetSocketAddress(ni_address, control_port ));
			
						new AEThread( "MCGroup:CtrlListener", true )
							{
								public void
								runSupport()
								{
									handleSocket( network_interface, ni_address, control_socket );
								}
							}.start();
														
					}catch( Throwable e ){
					
						Debug.printStackTrace( e );
					}
				}
			}
		}finally{
			
			current_registrations	= new_registrations;
			
			this_mon.exit();
		}
	}
	
	protected boolean
	validNetworkAddress(
		final NetworkInterface	network_interface,
		final InetAddress		ni_address )
	{
		try{
			this_mon.enter();
		
			Set	set = (Set)current_registrations.get( network_interface );
			
			if ( set == null ){
				
				return( false );
			}
			
			return( set.contains( ni_address ));
			
		}finally{
			
			this_mon.exit();
		}
	}
	

	public void
	sendToGroup(
		byte[]	data )
	
		throws MCGroupException
	{
		try{
			Enumeration	x = NetworkInterface.getNetworkInterfaces();
			
			while( x != null && x.hasMoreElements()){
				
				NetworkInterface	network_interface = (NetworkInterface)x.nextElement();
				
				if ( !network_interface.getInetAddresses().hasMoreElements()){
					
						// skip any interface that have no addresses as this will
						// cause an error when we try and set the mc_socks's NI
					
					continue;
				}
				
				try{
					
					MulticastSocket mc_sock = new MulticastSocket(null);
	
					mc_sock.setReuseAddress(true);
					
					try{
						mc_sock.setTimeToLive( TTL );
						
					}catch( Throwable e ){
						
						if ( !ttl_problem_reported ){
							
							ttl_problem_reported	= true;
							
							Debug.printStackTrace( e );
						}
					}
					
					mc_sock.bind( new InetSocketAddress( control_port ));
	
					mc_sock.setNetworkInterface( network_interface );
					
					// System.out.println( "querying interface " + network_interface );
					
					DatagramPacket packet = new DatagramPacket(data, data.length, group_address.getAddress(), group_port );
					
					mc_sock.send(packet);
					
					mc_sock.close();
						
				}catch( Throwable e ){
				
					if ( !sso_problem_reported ){
						
						sso_problem_reported	= true;
					
						Debug.printStackTrace( e );
					}
				}
			}
		}catch( Throwable e ){
			
			throw( new MCGroupException( "sendToGroup failed", e ));
		}
	}
	
	protected void
	handleSocket(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		DatagramSocket		socket )
	{
		long	successful_accepts 	= 0;
		long	failed_accepts		= 0;

		int	port = socket.getLocalPort();
		
		try{
				// introduce a timeout so that when a Network interface changes we don't sit here
				// blocking forever and thus never realise that we should shutdown
			
			socket.setSoTimeout( 30000 );
			
		}catch( Throwable e ){
			
		}
		
		while(true){
			
			if ( !validNetworkAddress( network_interface, local_address )){
				
				adapter.trace( 
						"group = " + group_address +"/" + 
						network_interface.getName()+":"+ 
						network_interface.getDisplayName() + " - " + local_address + ": stopped" );
				
				return;
			}
			
			try{
				byte[] buf = new byte[PACKET_SIZE];
				
				DatagramPacket packet = new DatagramPacket(buf, buf.length );
								
				socket.receive( packet );
					
				successful_accepts++;
				
				failed_accepts	 = 0;
				
				receivePacket( network_interface, local_address, packet );
				
			}catch( SocketTimeoutException e ){
				
			}catch( Throwable e ){
				
				failed_accepts++;
				
				Logger.log(new LogEvent(LOGID, "MCGroup: receive failed on port " + port, e)); 

				if (( failed_accepts > 100 && successful_accepts == 0 ) || failed_accepts > 1000 ){
					
						Logger.logTextResource(new LogAlert(LogAlert.UNREPEATABLE,
							LogAlert.AT_ERROR, "Network.alert.acceptfail"), new String[] {
							"" + port, "UDP" });
			
					break;
				}
			}
		}
	}
	
	protected void
	receivePacket(
		NetworkInterface	network_interface,
		InetAddress			local_address,
	    DatagramPacket		packet )
	{
		adapter.received( 
				network_interface, 
				local_address, 
				(InetSocketAddress)packet.getSocketAddress(), 
				packet.getData(), 
				packet.getLength());
	}
	
	public void
	sendToMember(
		InetSocketAddress	address,
		byte[]				data )
	
		throws MCGroupException
	{
		DatagramSocket	reply_socket	= null;
				
		try{
			reply_socket = new DatagramSocket();
			
			DatagramPacket reply_packet = new DatagramPacket(data,data.length,address);
			
			reply_socket.send( reply_packet );
			
		}catch( Throwable e ){
			
			throw( new MCGroupException( "sendToMember failed", e ));
			
		}finally{
			
			if ( reply_socket != null ){
				
				try{
					reply_socket.close();
					
				}catch( Throwable e ){
				}
			}
		}	
	}
}
