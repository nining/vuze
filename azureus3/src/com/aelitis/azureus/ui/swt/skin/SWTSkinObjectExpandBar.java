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

package com.aelitis.azureus.ui.swt.skin;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

/**
 * Container that hold ExpandItems
 * 
 */
public class SWTSkinObjectExpandBar
	extends SWTSkinObjectContainer
{

	private ExpandBar expandBar;

	private List<SWTSkinObjectExpandItem> expandItems = new ArrayList<SWTSkinObjectExpandItem>();

	private List<SWTSkinObjectExpandItem> fillHeightItems = new ArrayList<SWTSkinObjectExpandItem>();

	public SWTSkinObjectExpandBar(SWTSkin skin, SWTSkinProperties properties,
			String sID, String sConfigID, SWTSkinObject parent) {
		super(skin, properties, null, sID, sConfigID, "expandbar", parent);
		createExpandBar();
	}

	private void createExpandBar() {
		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}

		int style = SWT.NONE;
		if (properties.getIntValue(sConfigID + ".border", 0) == 1) {
			style = SWT.BORDER;
		}

		expandBar = new ExpandBar(createOn, style); // | SWT.V_SCROLL);
		// ensure no layout for expandbar (children don't setlayoutdata because they are expanditems)
		expandBar.setLayout(null);
		expandBar.setSpacing(1);

		expandBar.addListener(SWT.Resize, new Listener() {
			public void handleEvent(final Event event) {
				handleResize(null);
			}
		});

		triggerListeners(SWTSkinObjectListener.EVENT_CREATED);
		setControl(expandBar);
	}
	
	
	protected void handleResize(ExpandItem itemResizing) {
		SWTSkinObjectExpandItem foundItem = null;
		if (itemResizing != null) {
  		SWTSkinObjectExpandItem[] children = getChildren();
  		for (SWTSkinObjectExpandItem item : children) {
  			if (item.getExpandItem() == itemResizing) {
  				foundItem = item;
  				item.resizeComposite();
  				break;
  			}
  		}
		}
		
		for (SWTSkinObjectExpandItem autoItem : fillHeightItems) {
			if (autoItem != foundItem) {
				autoItem.resizeComposite();
			}
		}
		
	}

	public void
	relayout()
	{
		super.relayout();
		handleResize(null);
	}
	
	protected void addExpandItem(SWTSkinObjectExpandItem item) {
		expandItems.add(item);

		if (item.fillsHeight()) {
			fillHeightItems.add(item);
		}
	}

	protected void removeExpandItem(SWTSkinObjectExpandItem item) {
		expandItems.remove(item);
		fillHeightItems.remove( item );
	}

	public SWTSkinObjectExpandItem[] getChildren() {
		return expandItems.toArray(new SWTSkinObjectExpandItem[0]);
	}

	public ExpandBar getExpandbar() {
		return expandBar;
	}
}
