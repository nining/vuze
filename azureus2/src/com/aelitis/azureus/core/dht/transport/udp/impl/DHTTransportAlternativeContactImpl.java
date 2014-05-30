/*
 * Created on May 29, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.dht.transport.udp.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.dht.transport.DHTTransportAlternativeContact;

public class 
DHTTransportAlternativeContactImpl
	implements DHTTransportAlternativeContact
{
	private final byte		network_type;
	private final byte		version;
	private final short		initial_age;
	private final byte[]	encoded;
	
	private final int		id;
	
	private final int			start_time		= (int)( SystemTime.getMonotonousTime()/1000 );
	
	private final int			last_alive_marker;
	
	protected
	DHTTransportAlternativeContactImpl(
		byte			_network_type,
		byte			_version,
		short			_age,
		byte[]			_encoded )
	{
		network_type	= _network_type;
		version			= _version;
		initial_age		= _age<0?Short.MAX_VALUE:_age;
		encoded			= _encoded;
		
			// add in a constant to avoid negative values for aethetics

		last_alive_marker = start_time + Short.MAX_VALUE - initial_age ;
		
		id = Arrays.hashCode( encoded );
	}
	
	public int
	getNetworkType()
	{
		return( network_type&0xff );
	}
	
	public int
	getVersion()
	{
		return( version&0xff );
	}
	
	public int
	getID()
	{
		return( id );
	}
	
	public int
	getLastAlive()
	{		
		return( last_alive_marker );
	}
	
	public int
	getAge()
	{
		if ( initial_age < 0 ){
			
			return( Short.MAX_VALUE );
		}
		
		int elapsed = ((int)( SystemTime.getMonotonousTime()/1000 )) - start_time;
		
		int rem = Short.MAX_VALUE - initial_age;
		
		if ( rem < elapsed ){
			
			return( Short.MAX_VALUE );
			
		}else{
			
			return((short)( initial_age + elapsed ));
		}
	}
	
	public Map<String,Object>
	getProperties()
	{
		try{
			return( BDecoder.decode( encoded ));
			
		}catch( Throwable e ){
			
			return( new HashMap<String,Object>());
		}
	}
}
