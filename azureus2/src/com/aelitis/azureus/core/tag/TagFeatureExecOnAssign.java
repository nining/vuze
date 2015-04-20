/*
 * Created on Apr 18, 2015
 * Created by Paul Gardner
 * 
 * Copyright 2015 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
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


package com.aelitis.azureus.core.tag;

public interface 
TagFeatureExecOnAssign
	extends TagFeature
{
	public static final int ACTION_NONE		= 0x0000;
	public static final int ACTION_DESTROY	= 0x0001;
	
	public int
	getSupportedActions();
	
	public boolean
	supportsAction(
		int		action );
	
	public boolean
	isActionEnabled(
		int		action );
	
	public void
	setActionEnabled(
		int			action,
		boolean		enabled );
}
