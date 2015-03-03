/*
 * File    : CategoryItem.java
 * Created : 01 feb. 2004
 * By      : TuxPaper
 *
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

package com.aelitis.azureus.ui.swt.columns.tag;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnExtraInfoListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;

import com.aelitis.azureus.core.tag.Tag;



public class ColumnTagGroup
	implements TableCellRefreshListener, TableColumnExtraInfoListener
{
	public static String COLUMN_ID = "tag.group";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
				TableColumn.CAT_CONTENT,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	/** Default Constructor */
	public ColumnTagGroup(TableColumn column) {
		column.setWidth(70);
		column.setRefreshInterval(TableColumn.INTERVAL_LIVE);
		column.setPosition( TableColumn.POSITION_INVISIBLE );
		column.addListeners(this);
	}

	public void refresh(TableCell cell) {
		Tag tag = (Tag) cell.getDataSource();
		
		String text = null;
		
		if ( tag != null ){
			
			text = tag.getGroup();
		}
		
		if ( text == null ){
			
			text = "";
		}

		if ( !cell.setSortValue( text ) && cell.isValid()){
			
			return;
		}

		if ( !cell.isShown()){
			
			return;
		}
		
		cell.setText( text );
	}
}
