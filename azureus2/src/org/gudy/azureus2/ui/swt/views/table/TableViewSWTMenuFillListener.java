/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt.views.table;

import org.eclipse.swt.widgets.Menu;

/**
 * @author TuxPaper
 * @created Feb 2, 2007
 *
 */
public interface TableViewSWTMenuFillListener
{
	public void fillMenu(String sColumnName, Menu menu);

	/** 
	 * Create a SubMenu for column specific tasks.  Everytime the user opens
	 * the context menu, the "This Column" submenu is cleared, and this function
	 * is called to refill it.
	 *
	 * @param sColumnName The name of the column the user clicked on
	 * @param menuThisColumn the menu to fill with MenuItems
	 */
	public void addThisColumnSubMenu(String sColumnName, Menu menuThisColumn);
}
