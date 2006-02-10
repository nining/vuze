/*
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.gudy.azureus2.core3.util.TimeFormatter;

import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;


public class ConnectedTimeItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public ConnectedTimeItem() {
    super( "connected_time", ALIGN_TRAIL, POSITION_INVISIBLE, 70, TableManager.TABLE_TORRENT_PEERS);
    setRefreshInterval(INTERVAL_LIVE);
  }

  public void refresh(TableCell cell) {
    PEPeerTransport peer = (PEPeerTransport)cell.getDataSource();
        
    long value = (peer == null) ? 0 : peer.getTimeSinceConnectionEstablished();

    if( !cell.setSortValue( value ) && cell.isValid() ) {
      return;
    }

    cell.setText( TimeFormatter.format( value / 1000 ) );
  }
}
