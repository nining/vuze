/*
 * Created on 2 juil. 2003
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
 *
 */
package com.aelitis.azureus.ui.swt.views.skin;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateTab;
import org.gudy.azureus2.ui.swt.mainwindow.MenuFactory;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance.UISWTViewEventListenerWrapper;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventListenerHolder;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;
import org.gudy.azureus2.ui.swt.views.PeersView;
import org.gudy.azureus2.ui.swt.views.piece.PieceInfoView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.common.table.TableViewFilterCheck;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MdiListener;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentListener;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.mdi.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectTextbox;
import com.aelitis.azureus.util.DataSourceUtils;

/**
 * Torrent download view, consisting of several information tabs
 * 
 * @author Olivier
 * 
 */
public class SBC_TorrentDetailsView
	extends SkinView
	implements DownloadManagerListener, ObfusticateTab,
	UIUpdatable, UIPluginViewToolBarListener, SelectedContentListener
{

	private DownloadManager manager;

	private TabbedMdiInterface tabbedMDI;

	int lastCompleted = -1;

	private GlobalManagerAdapter gmListener;

	private Composite parent;

	private FilterCheckHandler filter_check_handler;

	private volatile int selection_count = 0;
	private volatile long selection_size;
	private volatile long selection_done;
	
	private SWTSkinObjectTextbox soFilterTextBox;

	private SWTSkinObjectText soInfoArea;

	private MdiEntrySWT mdi_entry;

	private Object dataSource;

	/**
	 * 
	 */
	public SBC_TorrentDetailsView() {
		// assumed if we are opening a Download Manager View that we
		// have a DownloadManager and thus an AzureusCore
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
				gmListener = new GlobalManagerAdapter() {
					public void downloadManagerRemoved(DownloadManager dm) {
						if (dm.equals(manager)) {
							Utils.execSWTThread(new AERunnable() {
								public void runSupport() {
									getMainSkinObject().dispose();
								}
							});
						}
					}
				};
				gm.addListener(gmListener, false);
			}
		});
	}

	private void dataSourceChanged(Object newDataSource) {
		this.dataSource = newDataSource;
		
		if (manager != null) {
			manager.removeListener(this);
		}

		manager = DataSourceUtils.getDM(newDataSource);
		
		if (tabbedMDI != null && newDataSource instanceof Object[]
				&& ((Object[]) newDataSource)[0] instanceof PEPeer) {
			tabbedMDI.showEntryByID(PeersView.MSGID_PREFIX);
		}

		if (manager != null) {
			manager.addListener(this);
		}
		
		if (tabbedMDI != null) {
  		MdiEntry[] entries = tabbedMDI.getEntries();
  		for (MdiEntry entry : entries) {
  			entry.setDatasource(newDataSource);
  		}
		}

		refreshTitle();
	}

	private void delete() {
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null) {
			uiFunctions.getUIUpdater().removeUpdater(this);
		}
		if (manager != null) {
			manager.removeListener(this);
		}

		try {
			GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
			gm.removeListener(gmListener);
		} catch (Exception e) {
			Debug.out(e);
		}

		SelectedContentManager.removeCurrentlySelectedContentListener(this);

		Utils.disposeSWTObjects(new Object[] {
			parent
		});
	}

	private void initialize(Composite composite) {

		Composite main_area = new Composite(composite, SWT.NULL);
		main_area.setLayout(new FormLayout());

		//boolean az2 = Utils.isAZ2UI();
		//Color bg_color = ColorCache.getColor(composite.getDisplay(), "#c0cbd4");

		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();

		this.parent = composite;
		if (tabbedMDI == null) {
			tabbedMDI = uiFunctions.createTabbedMDI(main_area, "detailsview");
		} else {
			System.out.println("ManagerView::initialize : folder isn't null !!!");
		}

		if (composite.getLayout() instanceof FormLayout) {
			main_area.setLayoutData(Utils.getFilledFormData());
		} else if (composite.getLayout() instanceof GridLayout) {
			main_area.setLayoutData(new GridData(GridData.FILL_BOTH));
		}
		composite.layout();

		// Call plugin listeners
		if (uiFunctions != null) {
			UISWTInstance pluginUI = uiFunctions.getUISWTInstance();

			if ( pluginUI != null ){
				
				MyTorrentsView.registerPluginViews(pluginUI);
	
				// unfortunately views for the manager view are currently registered
				// against 'MyTorrents'...
	
				for (String id : new String[] {
					UISWTInstance.VIEW_MYTORRENTS,
					UISWTInstance.VIEW_TORRENT_DETAILS
				}) {
	
					UISWTViewEventListenerWrapper[] pluginViews = pluginUI.getViewListeners(id);
	
					for (UISWTViewEventListenerWrapper l : pluginViews) {
	
						if (id == UISWTInstance.VIEW_MYTORRENTS
								&& l.getViewID() == PieceInfoView.MSGID_PREFIX) {
							// Simple hack to exlude PieceInfoView tab as it's already within Pieces View
							continue;
						}
	
						if (l != null) {
	
							try {
								tabbedMDI.createEntryFromEventListener(null,
										UISWTInstance.VIEW_TORRENT_DETAILS, l, l.getViewID(), false,
										manager, null);
	
							} catch (Throwable e) {
	
								Debug.out(e);
							}
						}
					}
				}
			}
		}

		SelectedContentManager.addCurrentlySelectedContentListener(this);

		tabbedMDI.addListener(new MdiSWTMenuHackListener() {
			
			public void menuWillBeShown(MdiEntry entry, Menu menuTree) {
				menuTree.setData("downloads", new DownloadManager[] {
					manager
				});
				menuTree.setData("is_detailed_view", true);
				
				MenuFactory.buildTorrentMenu(menuTree);
			}
		});
		
		tabbedMDI.addListener(new MdiListener() {
			public void mdiEntrySelected(MdiEntry newEntry, MdiEntry oldEntry) {
				// TODO Auto-generated method stub
				if (oldEntry != null) {
  				MdiEntrySWT oldEntrySWT = ((MdiEntrySWT) oldEntry);
  				UISWTViewEventListener listener = oldEntrySWT.getEventListener();
  				if (listener instanceof UISWTViewEventListenerHolder) {
  					listener = ((UISWTViewEventListenerHolder) listener).getDelegatedEventListener(oldEntrySWT);
  				}
  
  				// unhook filtering
  
  				if (listener instanceof TableViewTab<?>
  						&& listener instanceof TableViewFilterCheck<?>) {
  
  					TableViewTab<?> tvt = (TableViewTab<?>) listener;
  
  					TableViewSWT tv = tvt.getTableView();
  
  					if (tv != null) {
  						tv.disableFilterCheck();
  					}
  				}
				}

				// hook in filtering

				MdiEntrySWT newEntrySWT = ((MdiEntrySWT) newEntry);
				UISWTViewEventListener listener = newEntrySWT.getEventListener();
				if (listener instanceof TableViewTab
						&& listener instanceof TableViewFilterCheck) {

					TableViewTab tvt = (TableViewTab) listener;

					TableViewFilterCheck delegate = (TableViewFilterCheck) tvt;

					soFilterTextBox.setVisible(true);

					filter_check_handler = new FilterCheckHandler(tvt, delegate);

					tvt.enableFilterCheck(soFilterTextBox.getTextControl(),
							filter_check_handler);

				} else {
					filter_check_handler = null;

					soFilterTextBox.setVisible(false);
				}

				refresh();
				if ( mdi_entry != null ){
					mdi_entry.redraw();
					ViewTitleInfoManager.refreshTitleInfo(mdi_entry.getViewTitleInfo());
				}
			}
		});
		
		if (dataSource instanceof Object[]
				&& ((Object[]) dataSource)[0] instanceof PEPeer) {
			tabbedMDI.showEntryByID(PeersView.MSGID_PREFIX);
		} else {
  		MdiEntry[] entries = tabbedMDI.getEntries();
  		if (entries.length > 0) {
  			tabbedMDI.showEntry(entries[0]);
  		}
		}
	}


	public void currentlySelectedContentChanged(
			ISelectedContent[] currentContent, String viewId) {
		selection_count = currentContent.length;

		long	total_size 	= 0;
		long	total_done	= 0;
		
		for ( ISelectedContent sc: currentContent ){
			
			DownloadManager dm = sc.getDownloadManager();
			
			if ( dm != null ){
				
				int	file_index = sc.getFileIndex();
				
				if ( file_index == -1 ){
				
					DiskManagerFileInfo[] file_infos = dm.getDiskManagerFileInfoSet().getFiles();
					
					for ( DiskManagerFileInfo file_info: file_infos ){
						
						if ( !file_info.isSkipped()){
							
							total_size 	+= file_info.getLength();
							total_done	+= file_info.getDownloaded();
						}
					}
				}else{
					
					DiskManagerFileInfo file_info = dm.getDiskManagerFileInfoSet().getFiles()[file_index];
					
					if ( !file_info.isSkipped()){
					
						total_size 	+= file_info.getLength();
						total_done	+= file_info.getDownloaded();
					}
				}
			}
		}
		
		selection_size	= total_size;
		selection_done	= total_done;
		
		if (filter_check_handler != null) {

			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					filter_check_handler.updateHeader();
				}
			});
		} else if (soInfoArea != null) {
			TableView view = SelectedContentManager.getCurrentlySelectedTableView();
			String s = "";
			if (view != null) {
				int total = view.size(false);

				s = MessageText.getString("library.unopened.header"
						+ (total > 1 ? ".p" : ""), new String[] {
					String.valueOf(total)
				});

				if (selection_count > 1) {

					s += getSelectionText();
				}
			}

			soInfoArea.setText(s);
		}
	}
	
	private String
	getSelectionText()
	{
		String str = ", " + 
				MessageText.getString(
				"label.num_selected", new String[]{ String.valueOf( selection_count )});
		
		if ( selection_size > 0 ){
			
			if ( selection_size == selection_done ){
				
				str += " (" + DisplayFormatters.formatByteCountToKiBEtc( selection_size ) + ")";
			}else{
				str += " (" + DisplayFormatters.formatByteCountToKiBEtc( selection_done ) + "/" + DisplayFormatters.formatByteCountToKiBEtc( selection_size ) + ")";

			}
		}
		
		return( str );
	}

	/**
	 * Called when view is visible
	 */
	private void refresh() {
		tabbedMDI.updateUI();
	}

	/**
	 * 
	 *
	 * @since 3.1.0.1
	 */
	private void refreshTitle() {
		int completed = manager == null ? -1 : manager.getStats().getPercentDoneExcludingDND();
		if (lastCompleted != completed) {
			if (mdi_entry != null) {
				ViewTitleInfoManager.refreshTitleInfo(mdi_entry.getViewTitleInfo());
			}
			lastCompleted = completed;
		}
	}

	protected static String escapeAccelerators(String str) {
		if (str == null) {

			return (str);
		}

		return (str.replaceAll("&", "&&"));
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener#refreshToolBarItems(java.util.Map)
	 */
	public void refreshToolBarItems(Map<String, Long> list) {
		BaseMdiEntry activeView = getActiveView();
		if (activeView == null) {
			return;
		}
		activeView.refreshToolBarItems(list);
	};
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener#toolBarItemActivated(com.aelitis.azureus.ui.common.ToolBarItem, long, java.lang.Object)
	 */
	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		BaseMdiEntry activeView = getActiveView();
		if (activeView == null) {
			return false;
		}
		return activeView.toolBarItemActivated(item, activationType, datasource);
	}

	public void downloadComplete(DownloadManager manager) {
	}

	public void completionChanged(DownloadManager manager, boolean bCompleted) {
	}

	public void filePriorityChanged(DownloadManager download,
			org.gudy.azureus2.core3.disk.DiskManagerFileInfo file) {
	}

	public void stateChanged(DownloadManager manager, int state) {
		if (tabbedMDI == null || tabbedMDI.isDisposed()) {
			return;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}
		});
	}

	public void positionChanged(DownloadManager download, int oldPosition,
			int newPosition) {
	}

	public String getObfusticatedHeader() {
		int completed = manager.getStats().getCompleted();
		return DisplayFormatters.formatPercentFromThousands(completed) + " : "
				+ manager;
	}

	public DownloadManager getDownload() {
		return manager;
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isSelected(java.lang.String)
	public boolean isSelected(String itemKey) {
		return false;
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return "DMDetails";
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#updateUI()
	public void updateUI() {
		refreshTitle();
		refresh();
	}

	private class FilterCheckHandler
		implements TableViewFilterCheck.TableViewFilterCheckEx<Object>
	{
		private TableViewTab<Object> tvt;

		private TableViewFilterCheck delegate;

		boolean enabled;

		int value;

		private FilterCheckHandler(TableViewTab<Object> _tvt,
				TableViewFilterCheck _delegate)

		{
			tvt = _tvt;
			delegate = _delegate;

			updateHeader();
		}

		/* (non-Javadoc)
		 * @see com.aelitis.azureus.ui.common.table.TableViewFilterCheck#filterCheck(java.lang.Object, java.lang.String, boolean)
		 */
		public boolean filterCheck(Object ds, String filter, boolean regex) {
			return (delegate.filterCheck(ds, filter, regex));
		};

		public void filterSet(String filter) {
			boolean was_enabled = enabled;

			enabled = filter != null && filter.length() > 0;

			delegate.filterSet(filter);

			if (enabled != was_enabled) {

				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						updateHeader();
					}
				});
			}
		}

		public void viewChanged(TableView<Object> view) {
			value = view.size(false);

			if (enabled) {

				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						updateHeader();
					}
				});
			}
		}

		private void updateHeader() {
			int total = manager.getNumFileInfos();

			String s = MessageText.getString("library.unopened.header"
					+ (total > 1 ? ".p" : ""), new String[] {
				String.valueOf(total)
			});

			if (enabled) {

				String extra = MessageText.getString("filter.header.matches1",
						new String[] {
							String.valueOf(value)
						});

				s += " " + extra;
			}

			if (selection_count > 1) {

				s += getSelectionText();
			}

			if (soInfoArea != null) {
				soInfoArea.setText(s);
			}
		}
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectInitialShow(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		SWTSkinObject soListArea = getSkinObject("torrentdetails-list-area");
		if (soListArea == null) {
			return null;
		}
		soFilterTextBox = (SWTSkinObjectTextbox) getSkinObject("torrentdetails-filter");
		soInfoArea = (SWTSkinObjectText) getSkinObject("torrentdetails-info");

		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();

		if (mdi != null) {

			mdi_entry = mdi.getEntryFromSkinObject(skinObject);

			if ( mdi_entry == null ){
				
					// We *really* need to not use 'current' here as it is inaccurate (try opening multiple torrent details view
					// at once to see this)
				
				Debug.out( "Failed to get MDI entry from skin object, reverting to using 'current'" );
				
				mdi_entry = mdi.getCurrentEntrySWT();
			}
			
		}

		initialize((Composite) soListArea.getControl());
		return null;
	}
	
	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectDestroyed(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		delete();
		return super.skinObjectDestroyed(skinObject, params);
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#dataSourceChanged(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object dataSourceChanged(SWTSkinObject skinObject, Object params) {
		dataSourceChanged(params);
		return null;
	}
	
	private BaseMdiEntry getActiveView() {
		if (tabbedMDI == null || tabbedMDI.isDisposed()) {
			return null;
		}
		return (BaseMdiEntry) tabbedMDI.getCurrentEntrySWT();
	}
}
