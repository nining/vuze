/*
 * Created on Dec 4, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package org.gudy.azureus2.core3.tracker.client.impl;

import java.net.URL;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerDataProvider;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerException;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.tracker.client.impl.bt.TRTrackerBTAnnouncerImpl;
import org.gudy.azureus2.core3.tracker.client.impl.dht.TRTrackerDHTAnnouncerImpl;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;

public class 
TRTrackerAnnouncerMuxer
	extends TRTrackerAnnouncerImpl
{
	private TRTrackerAnnouncer	main_announcer;
	
	protected
	TRTrackerAnnouncerMuxer(
		TOTorrent		torrent,
		String[]		networks,
		boolean			manual )
	
		throws TRTrackerAnnouncerException
	{
		super( torrent );
		
		TOTorrentAnnounceURLSet[]	sets = torrent.getAnnounceURLGroup().getAnnounceURLSets();
		
		if ( TorrentUtils.isDecentralised( torrent )){
			
			main_announcer	= new TRTrackerDHTAnnouncerImpl( torrent, networks, manual, getHelper());
			
		}else{
			

			main_announcer = new TRTrackerBTAnnouncerImpl( torrent, sets, networks, manual, getHelper());
		}
	}
	
	public void
	setAnnounceDataProvider(
		TRTrackerAnnouncerDataProvider		provider )
	{
		main_announcer.setAnnounceDataProvider( provider );
		
		//System.out.println( "announcer set" );
	}
	
	public TOTorrent
	getTorrent()
	{
		return( main_announcer.getTorrent());
	}
	
	public URL
	getTrackerURL()
	{
		return( main_announcer.getTrackerURL());
	}
	
	public void
	setTrackerURL(
		URL		url )
	{
		main_announcer.setTrackerURL( url );
	}
		
	public void 
	setTrackerURLs(
		TOTorrentAnnounceURLSet[] 	sets ) 
	{
		Debug.out( "Not implemented" );
	}
	
	public void
	resetTrackerUrl(
		boolean	shuffle )
	{
		main_announcer.setTrackerURLs( getTorrent().getAnnounceURLGroup().getAnnounceURLSets());
		
		main_announcer.resetTrackerUrl( shuffle );
	}
	
	public void
	setIPOverride(
		String		override )
	{
		main_announcer.setIPOverride( override );
	}
	
	public void
	cloneFrom(
		TRTrackerAnnouncer	other )
	{
		main_announcer.cloneFrom( other );
	}
	
	public void
	clearIPOverride()
	{
		main_announcer.clearIPOverride();
	}
	
	public byte[]
	getPeerId()
	{
		return( main_announcer.getPeerId());
	}
	
	public void
	setRefreshDelayOverrides(
		int		percentage )
	{
		main_announcer.setRefreshDelayOverrides( percentage );
	}
	
	public int
	getTimeUntilNextUpdate()
	{
		return( main_announcer.getTimeUntilNextUpdate());
	}
	
	public int
	getLastUpdateTime()
	{
		return( main_announcer.getLastUpdateTime());
	}
			
	public void
	update(
		boolean	force )
	{
		main_announcer.update(force);
		
		//System.out.println( "update" );
	}
	
	public void
	complete(
		boolean	already_reported )
	{
		main_announcer.complete(already_reported);
		
		//System.out.println( "complete" );
	}
	
	public void
	stop(
		boolean	for_queue )
	{
		main_announcer.stop( for_queue );
		
		//System.out.println( "stop" );
	}
	
	public void
	destroy()
	{
		TRTrackerAnnouncerFactoryImpl.destroy( this );

		main_announcer.destroy();
		
		//System.out.println( "destroy" );
	}
	
	public int
	getStatus()
	{
		return( main_announcer.getStatus());
	}
	
	public boolean
	isManual()
	{
		return( main_announcer.isManual());
	}
	
	public String
	getStatusString()
	{
		return( main_announcer.getStatusString());
	}
	
	public TRTrackerAnnouncerResponse
	getLastResponse()
	{
		return( main_announcer.getLastResponse());
	}
	
	
	public void
	refreshListeners()
	{
		main_announcer.refreshListeners();	
	}
	
	public void
	setAnnounceResult(
		DownloadAnnounceResult	result )
	{
		main_announcer.setAnnounceResult(result);
	}
		
	public void 
	generateEvidence(
		IndentWriter writer )
	{
		main_announcer.generateEvidence(writer);
	}
}
