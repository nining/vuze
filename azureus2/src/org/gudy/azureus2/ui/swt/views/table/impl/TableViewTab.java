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

package org.gudy.azureus2.ui.swt.views.table.impl;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;

import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.common.table.TableViewFilterCheck.TableViewFilterCheckEx;
import com.aelitis.azureus.ui.mdi.MdiEntry;

/**
 * An {@link UISWTView} that contains a {@link TableView}.  Usually is
 * an view in a  {@link MdiEntry}, or a TableView's subview.
 */
public abstract class TableViewTab<DATASOURCETYPE>
	implements UISWTViewCoreEventListener, AEDiagnosticsEvidenceGenerator
{
	private TableViewSWT<DATASOURCETYPE> tv;
	private Object parentDataSource;
	private final String propertiesPrefix;
	private Composite composite;
	private UISWTView swtView;
	private Text filterTextControl;
	private TableViewFilterCheckEx<DATASOURCETYPE> filterCheck;

	
	public TableViewTab(String propertiesPrefix) {
		this.propertiesPrefix = propertiesPrefix;
	}
	
	public TableViewSWT<DATASOURCETYPE> getTableView() {
		return tv;
	}

	public final void initialize(Composite composite) {
		tv = initYourTableView();
		if (parentDataSource != null) {
			tv.setParentDataSource(parentDataSource);
		}
		Composite parent = initComposite(composite);
		tv.initialize(swtView, parent);
		if (parent != composite) {
			this.composite = composite;
		} else {
			this.composite = tv.getComposite();
		}
		
		if (filterCheck != null) {
			tv.enableFilterCheck(filterTextControl, filterCheck);
		}
		
		tableViewTabInitComplete();
	}
	
	public void tableViewTabInitComplete() {
	}

	public Composite initComposite(Composite composite) {
		return composite;
	}

	public abstract TableViewSWT<DATASOURCETYPE> initYourTableView();

	public final void dataSourceChanged(Object newDataSource) {
		this.parentDataSource = newDataSource;
		if (tv != null) {
			tv.setParentDataSource(newDataSource);
		}
	}

	public final void refresh() {
		if (tv != null) {
			tv.refreshTable(false);
		}
	}

	private final void delete() {
		if (tv != null) {
			tv.delete();
		}
		tv = null;
	}

	public String getFullTitle() {
		return MessageText.getString(getPropertiesPrefix() + ".title.full");
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator#generate(org.gudy.azureus2.core3.util.IndentWriter)
	 */
	public void generate(IndentWriter writer) {
		if (tv != null) {
			tv.generate(writer);
		}
	}
	
	public Composite getComposite() {
		return composite;
	}

	public String getPropertiesPrefix() {
		return propertiesPrefix;
	}
	
	public Menu getPrivateMenu() {
		return null;
	}
	
	public void viewActivated() {
		// cheap hack.. calling isVisible freshens table's visible status (and
		// updates subviews)
		if (tv instanceof TableViewSWT) {
			((TableViewSWT<?>)tv).isVisible();
		}
	}
	
	private void viewDeactivated() {
		if (tv instanceof TableViewSWT) {
			((TableViewSWT<?>)tv).isVisible();
		}
	}

	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				swtView = (UISWTView) event.getData();
				swtView.setTitle(getFullTitle());
				break;

			case UISWTViewEvent.TYPE_DESTROY:
				delete();
				break;

			case UISWTViewEvent.TYPE_INITIALIZE:
				initialize((Composite) event.getData());
				break;

			case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
				swtView.setTitle(getFullTitle());
				updateLanguage();
				Messages.updateLanguageForControl(composite);
				break;

			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
				dataSourceChanged(event.getData());
				break;

			case UISWTViewEvent.TYPE_FOCUSGAINED:
				viewActivated();
				break;

			case UISWTViewEvent.TYPE_FOCUSLOST:
				viewDeactivated();
				break;

			case UISWTViewEvent.TYPE_REFRESH:
				refresh();
				break;
		}

		return true;
	}

	public void updateLanguage() {
	}

	public UISWTView getSWTView() {
		return swtView;
	}

	public void enableFilterCheck(Text textControl,
			TableViewFilterCheckEx<DATASOURCETYPE> filter_check_handler) {
		if (tv != null) {
			tv.enableFilterCheck(textControl, filter_check_handler);
		} else {
			filterTextControl = textControl;
			filterCheck = filter_check_handler;
		}
	}
}
