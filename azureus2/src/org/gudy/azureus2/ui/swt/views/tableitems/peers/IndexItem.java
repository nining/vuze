/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;


public class IndexItem
       extends CoreTableColumnSWT 
       implements TableCellRefreshListener
{


	public static final String COLUMN_ID = "#";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
				CAT_SETTINGS,
		});
	}

	public IndexItem(String table_id) {
		super(COLUMN_ID, ALIGN_TRAIL, POSITION_INVISIBLE, 20, table_id);
		setRefreshInterval(INTERVAL_LIVE);
	}

	public void refresh(TableCell cell) {

		TableRow row = cell.getTableRow();

		if ( row != null ){
			int	index = row.getIndex();
	
			if (!cell.setSortValue(index) && cell.isValid())
				return;
	
			cell.setText("" + (index+1));
		}
	}
}
