/*
 * Created on 15-Jun-2004
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.upnp.impl.device;

import org.gudy.azureus2.core3.upnp.UPnPAction;
import org.gudy.azureus2.core3.upnp.UPnPActionInvocation;
import org.gudy.azureus2.core3.upnp.UPnPException;

/**
 * @author parg
 *
 */

public class 
UPnPSSWANConnectionImpl 
{
	protected UPnPServiceImpl		service;
	
	protected
	UPnPSSWANConnectionImpl(
		UPnPServiceImpl		_service )
	{
		service	= _service;
	}
	
	public void
	addPortMapping(
		boolean		tcp,			// false -> UDP
		int			port,
		String		description )
	
		throws UPnPException
	{
		UPnPAction act = service.getAction( "AddPortMapping" );
		
		UPnPActionInvocation inv = act.getInvocation();
		
		inv.addArgument( "NewRemoteHost", 				"" );		// "" = wildcard for hosts, 0 = wildcard for ports
		inv.addArgument( "NewExternalPort", 			"" + port );
		inv.addArgument( "NewProtocol", 				tcp?"TCP":"UDP" );
		inv.addArgument( "NewInternalPort", 			"" + port );
		inv.addArgument( "NewInternalClient",			service.getDevice().getLocalAddress().getHostAddress());
		inv.addArgument( "NewEnabled", 					"1" );
		inv.addArgument( "NewPortMappingDescription", 	description );
		inv.addArgument( "NewLeaseDuration",			"0" );		// 0 -> infinite (?)
		
		inv.invoke();
		
	}
}
