/*
 * Created on Aug 6, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.subs;

public interface 
SubscriptionHistory 
{
	public boolean
	isEnabled();
	
	public void
	setEnabled(
		boolean		enabled );
	
	public boolean
	isAutoDownload();
	
	public void
	setAutoDownload(
		boolean		auto_dl );

	public void
	setDetails(
		boolean		enabled,
		boolean		auto_dl );
	
	public long
	getLastScanTime();
	
	public long
	getLastNewResultTime();
	
	public int
	getNumUnread();
	
	public int
	getNumRead();
	
	public SubscriptionResult[]
	getResults();
}
