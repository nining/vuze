/*
 * File    : TRTrackerServerTorrent.java
 * Created : 26-Oct-2003
 * By      : stuff
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.tracker.server.impl;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.util.*;

public class 
TRTrackerServerTorrent 
{
	protected TRTrackerServerImpl	server;
	protected HashWrapper			hash;

	protected Map				peer_map = new HashMap();
	
	protected TRTrackerServerStatsImpl	stats = new TRTrackerServerStatsImpl();
		
	protected
	TRTrackerServerTorrent(
		TRTrackerServerImpl	_server,
		HashWrapper			_hash )
	{
		server		= _server;
		hash		= _hash;
	}
	
	
	protected synchronized void
	peerContact(
		String		event,
		String		peer_id,
		int			port,
		String		ip_address,
		long		uploaded,
		long		downloaded,
		long		left,
		int			num_peers )
	{
		boolean	stopped = event != null && event.equalsIgnoreCase("stopped");
		
		TRTrackerServerPeerImpl	peer = (TRTrackerServerPeerImpl)peer_map.get( peer_id );
		
		if ( peer == null ){
		
				// check to see if this peer already has an entry against this torrent
				// and if so delete it (assumption is that the client has quit and
				// restarted with new peer id
									
			Iterator	it = peer_map.values().iterator();
							
			while (it.hasNext()){
							
				TRTrackerServerPeerImpl this_peer = (TRTrackerServerPeerImpl)it.next();
															
				if (	this_peer.getPort() == port &&
						new String(this_peer.getIP()).equals( ip_address )){
									
					System.out.println( "removing dead client '" + peer.getString());
									
					it.remove();
				}
			}
				
			if ( !stopped ){			
			
				peer = new TRTrackerServerPeerImpl( peer_id.getBytes(), ip_address.getBytes(), port );
							
				peer_map.put( peer_id, peer );
			}
		}else{
			
			if ( stopped ){
				
				peer_map.remove( peer_id );
			}
		}
		
		if ( peer != null ){
		
			peer.setLastContactTime( System.currentTimeMillis());
		
			peer.setStats( uploaded, downloaded, left, num_peers );
		}
		
		stats.addAnnounce();
	}
	
	protected synchronized void
	exportPeersToMap(
		Map		map )
	{
		long	now = System.currentTimeMillis();
		
		List	rep_peers = new ArrayList();
			
		map.put( "peers", rep_peers );
	
		Iterator	it = peer_map.values().iterator();
					
		while(it.hasNext()){
	
			TRTrackerServerPeerImpl	peer = (TRTrackerServerPeerImpl)it.next();
							
			if ( (now - peer.getLastContactTime()) > server.getTimeoutIntervalInMillis() ){
							
				System.out.println( "removing timed out client '" + peer.getString());
								
				it.remove();
								
			}else{
								
				Map rep_peer = new HashMap();
	
				rep_peers.add( rep_peer );
														 
				rep_peer.put( "peer id", peer.getPeerId() );
				rep_peer.put( "ip", peer.getIP() );
				rep_peer.put( "port", new Long( peer.getPort()));
			}
		}
	}
	
	protected TRTrackerServerStats
	getStats()
	{
		return( stats );
	}
	
	protected synchronized TRTrackerServerPeer[]
	getPeers()
	{
		TRTrackerServerPeer[]	res = new TRTrackerServerPeer[peer_map.size()];
		
		peer_map.values().toArray( res );
		
		return( res );
	}
	
	protected HashWrapper
	getHash()
	{
		return( hash );
	}
}
