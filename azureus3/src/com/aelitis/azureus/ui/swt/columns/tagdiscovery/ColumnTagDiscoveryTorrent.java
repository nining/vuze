/**
 * Copyright (C) 2014 Azureus Software, Inc. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

package com.aelitis.azureus.ui.swt.columns.tagdiscovery;

import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.*;

import com.aelitis.azureus.core.tag.TagDiscovery;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;

public class ColumnTagDiscoveryTorrent
	implements TableCellRefreshListener, TableColumnExtraInfoListener
{
	public static String COLUMN_ID = "tag.discovery.torrent";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			TableColumn.CAT_ESSENTIAL,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	/** Default Constructor */
	public ColumnTagDiscoveryTorrent(TableColumn column) {
		column.setWidth(200);
		column.addListeners(this);
		TableContextMenuItem menuShowTorrent = column.addContextMenuItem(
				"ConfigView.option.dm.dblclick.details",
				TableColumn.MENU_STYLE_COLUMN_DATA);

		menuShowTorrent.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof TagDiscovery) {
					UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
					if (uiFunctions != null) {
						byte[] hash = ((TagDiscovery) target).getHash();
						uiFunctions.getMDI().showEntryByID(
								MultipleDocumentInterface.SIDEBAR_SECTION_TORRENT_DETAILS,
								hash);
					}
				}
			}
		});
	}

	public void refresh(TableCell cell) {
		TagDiscovery discovery = (TagDiscovery) cell.getDataSource();
		cell.setText(discovery.getTorrentName());
	}
}
