/*
 * File    : TorrentAnnounceURLListImpl.java
 * Created : 03-Mar-2004
 * By      : parg
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

package org.gudy.azureus2.pluginsimpl.torrent;

/**
 * @author parg
 *
 */

import java.net.URL;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.pluginsimpl.download.*;

public class 
TorrentAnnounceURLListImpl
	implements TorrentAnnounceURLList
{
	protected TOTorrent		torrent;
	
	protected
	TorrentAnnounceURLListImpl(
		TOTorrent	_torrent )
	{
		torrent	= _torrent;
	}
	
	public TorrentAnnounceURLListSet[]
	getSets()
	{
		TOTorrentAnnounceURLGroup	group = torrent.getAnnounceURLGroup();
		
		TOTorrentAnnounceURLSet[]	sets = group.getAnnounceURLSets();
		
		TorrentAnnounceURLListSet[]	res = new TorrentAnnounceURLListSet[sets.length];
		
		for (int i=0;i<res.length;i++){
			
			res[i] = new TorrentAnnounceURLListSetImpl( this, sets[i]);
		}
		
		return( res );
	}
	
	public void
	setSets(
		TorrentAnnounceURLListSet[]		sets )
	{
		TOTorrentAnnounceURLGroup	group = torrent.getAnnounceURLGroup();
				
		TOTorrentAnnounceURLSet[]	res = new TOTorrentAnnounceURLSet[sets.length];
		
		for (int i=0;i<res.length;i++){
			
			res[i] = ((TorrentAnnounceURLListSetImpl)sets[i]).getSet();
		}
		
		group.setAnnounceURLSets( res );
		
		updated();
	}
	
	public TorrentAnnounceURLListSet
	create(
		URL[]		urls )
	{
		return( new TorrentAnnounceURLListSetImpl( this, torrent.getAnnounceURLGroup().createAnnounceURLSet(urls)));
	}
	
	public void
	addSet(
		URL[]		urls )
	{
		TorrentUtils.announceGroupsInsertLast( torrent, urls );
		
		updated();
	}
	
	public void
	insertSetAtFront(
		URL[]		urls )
	{
		TorrentUtils.announceGroupsInsertFirst( torrent, urls );
		
		updated();
	}
	
	protected void
	updated()
	{
		try{
			DownloadImpl dm = (DownloadImpl)DownloadManagerImpl.getDownloadStatic( torrent );
		
			if ( dm != null ){
			
				dm.torrentChanged();
			}
		}catch( DownloadException e ){
			
			// torrent may not be running
		}
	}
}
