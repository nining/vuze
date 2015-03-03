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

import org.gudy.azureus2.plugins.ui.tables.TableColumn;

import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.ui.swt.columns.ColumnCheckBox;



public class 
ColumnTagVisible
	extends ColumnCheckBox
{	
	public static String COLUMN_ID = "tag.visible";

	public 
	ColumnTagVisible(
		TableColumn column ) 
	{
		super( column, 60 );
	}
	
	@Override
	protected Boolean 
	getCheckBoxState(
		Object datasource ) 
	{
		Tag tag = (Tag)datasource;
		
		if ( tag != null ){
						
			return( tag.isVisible());
		}
		
		return( null );
	}
	
	@Override
	protected void 
	setCheckBoxState(
		Object 	datasource,
		boolean set ) 
	{
		Tag tag = (Tag)datasource;
		
		if ( tag != null ){
							
			tag.setVisible( set );
		}
	}
}
