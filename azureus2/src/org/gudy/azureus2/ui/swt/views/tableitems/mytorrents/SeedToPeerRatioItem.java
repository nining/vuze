/*
 * Created : 11 nov. 2004
 * By      : Alon Rohter
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;



public class SeedToPeerRatioItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
	public static final Class DATASOURCE_TYPE = Download.class;


  public static final String COLUMN_ID = "seed_to_peer_ratio";

	public SeedToPeerRatioItem(String sTableID) {
    super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_TRAIL, 70, sTableID);
    setRefreshInterval(INTERVAL_LIVE);
    setMinWidthAuto(true);
  }

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_SWARM,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_ADVANCED);
	}

  public void refresh(TableCell cell) {
    float ratio = -1;
    

    DownloadManager dm = (DownloadManager)cell.getDataSource();
    if( dm != null ) {
      TRTrackerScraperResponse response = dm.getTrackerScrapeResponse();
      int seeds;
      int peers;
      
      if( response != null && response.isValid() ) {
        seeds = Math.max( dm.getNbSeeds(), response.getSeeds() );
        
        int trackerPeerCount = response.getPeers();
        peers = dm.getNbPeers();
        if (peers == 0 || trackerPeerCount > peers) {
        	if (trackerPeerCount <= 0) {
          	peers = dm.getActivationCount();
        	} else {
        		peers = trackerPeerCount;
        	}
        }
      }
      else {
        seeds = dm.getNbSeeds();
        peers = dm.getNbPeers();
      }
      
      if (peers < 0 || seeds < 0) {
      	ratio = 0;
      } else {
	      if (peers == 0) {
	      	if (seeds == 0)
	      		ratio = 0;
	      	else
	        	ratio = Float.POSITIVE_INFINITY;
	      } else {
	      	ratio = (float)seeds / peers;
	      }
      }
    }

    if( !cell.setSortValue( ratio ) && cell.isValid() ) {
      return;
    }
    
    if (ratio == -1) {
			cell.setText("");
		} else if (ratio == 0) {
			cell.setText("??");
		} else {
			cell.setText(DisplayFormatters.formatDecimal(ratio, 3));
		}
  }

}
