/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.aelitis.azureus.ui.swt.mdi;

import org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;

import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;

/**
 * @author TuxPaper
 * @created Jan 29, 2010
 *
 */
public interface MultipleDocumentInterfaceSWT
	extends MultipleDocumentInterface
{
	public MdiEntry getEntryBySkinView(Object skinView);

	public UISWTViewCore getCoreViewFromID(String id);

	/**
	 * If you prefix the 'preferedAfterID' string with '~' then the operation will actually
	 * switch to 'preferedBeforeID'
	 */
	public MdiEntry createEntryFromEventListener(String parentEntryID,
			UISWTViewEventListener l, String id, boolean closeable, 
			Object datasource, String preferredAfterID);


	public MdiEntry createEntryFromEventListener(String parentEntryID,
			String parentViewID,
			UISWTViewEventListener l, String id, boolean closeable, 
			Object datasource, String preferredAfterID);

	public MdiEntry createEntryFromEventListener(String parentEntryID,
			Class<? extends UISWTViewEventListener> cla, String id, boolean closeable,
			Object data, String preferedAfterID);

	public MdiEntrySWT getEntrySWT(String id);

	public MdiEntrySWT getCurrentEntrySWT();

	public MdiEntrySWT getEntryFromSkinObject(PluginUISWTSkinObject skinObject);

	/**
	 * @param closeableConfigFile
	 * @since 5.6.0.1
	 */
	void setCloseableConfigFile(String closeableConfigFile);
}
