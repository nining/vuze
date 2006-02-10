/*
 * Created on 19-Jul-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.core3.ipfilter.impl;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.ipfilter.*;

public class 
IpFilterManagerImpl
	implements IpFilterManager
{
	protected static IpFilterManagerImpl		singleton	= new IpFilterManagerImpl();
	
	public static IpFilterManager
	getSingleton()
	{
		return( singleton );
	}
	
	public IpFilter
	getIPFilter()
	{
		return( IpFilterImpl.getInstance());
	}
	
	public BadIps
	getBadIps()
	{
		return (BadIpsImpl.getInstance());
	}
}
