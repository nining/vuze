/*
 * File    : TRTrackerResponsePeerImpl.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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

package org.gudy.azureus2.core3.tracker.client.impl;


import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.StringInterner;

public class 
TRTrackerAnnouncerResponsePeerImpl
	implements TRTrackerAnnouncerResponsePeer
{
	private String		source;
	private byte[]		peer_id;
	private String		address;
	private short		tcp_port;
	private short		udp_port;
	private short		http_port;
	private short		crypto;
	private byte		az_version;
	private short		up_speed;
	
	public
	TRTrackerAnnouncerResponsePeerImpl(
		String		_source,
		byte[]		_peer_id,
		String		_address,
		int			_tcp_port,
		int			_udp_port,
		int			_http_port,
		short		_crypto,
		byte		_az_version,
		int			_up_speed )
	{
		source		= StringInterner.intern(_source);
		peer_id		= _peer_id;
		address		= StringInterner.intern(_address);
		tcp_port	= (short)_tcp_port;
		udp_port	= (short)_udp_port;
		http_port	= (short)_http_port;
		crypto		= _crypto;
		az_version	= _az_version;
		up_speed	= (short)_up_speed;
	}
	
	public String
	getSource()
	{
		return( source );
	}
	
	public byte[]
	getPeerID()
	{
		return( peer_id );
	}
	
	public String
	getAddress()
	{
		return( address );
	}
	
	public int
	getPort()
	{
		return( tcp_port&0xffff );
	}
	
	public int
	getUDPPort()
	{
		return( udp_port&0xffff );
	}
	
	public int
	getHTTPPort()
	{
		return( http_port&0xffff );
	}
	
	public short
	getProtocol()
	{
		return( crypto );
	}
	
	public byte
	getAZVersion()
	{
		return( az_version );
	}
	
	public int
	getUploadSpeed()
	{
		return( up_speed&0xffff );
	}
	
	protected String
	getKey()
	{
		return( address + ":" + tcp_port );
	}
	
	public String
	getString()
	{
		return( "ip=" + address + 
					(tcp_port==0?"":(",tcp_port=" + getPort())) + 
					(udp_port==0?"":(",udp_port=" + getUDPPort())) + 
					(http_port==0?"":(",http_port=" + getHTTPPort())) + 
					",prot=" + crypto + 
					(up_speed==0?"":(",up=" + getUploadSpeed())) + 
					",ver=" + az_version );
	}
}
