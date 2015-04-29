/**
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FrequencyLimitedDispatcher;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.toolbar.*;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.pluginsimpl.UIToolBarItemImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UIToolBarManagerCore;
import org.gudy.azureus2.ui.swt.pluginsimpl.UIToolBarManagerImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UIToolBarManagerImpl.ToolBarManagerListener;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.selectedcontent.*;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.mdi.MdiEntrySWT;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItemSO;
import com.aelitis.azureus.util.DLReferals;
import com.aelitis.azureus.util.PlayUtils;

/**
 * @author TuxPaper
 * @created Jul 20, 2008
 */
public class ToolBarView
	extends SkinView
	implements SelectedContentListener, ToolBarManagerListener,
	ToolBarItem.ToolBarItemListener
{
	private static boolean DEBUG = false;

	private static toolbarButtonListener buttonListener;

	private Map<UIToolBarItem, ToolBarItemSO> mapToolBarItemToSO = new HashMap<UIToolBarItem, ToolBarItemSO>();

	private boolean showText = true;

	private boolean initComplete = false;

	private boolean showCalled = false;

	private ArrayList<ToolBarViewListener> listeners = new ArrayList<ToolBarViewListener>(
			1);

	private UIToolBarManagerCore tbm;

	private boolean firstTimeEver = true;

	public ToolBarView() {
		tbm = (UIToolBarManagerCore) UIToolBarManagerImpl.getInstance();
	}

	private ToolBarItem createItem(ToolBarView tbv, String id, String imageid,
			String textID) {
		UIToolBarItemImpl base = new UIToolBarItemImpl(id);
		base.setImageID(imageid);
		base.setTextID(textID);
		return base;
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		boolean uiClassic = COConfigurationManager.getStringParameter("ui").equals(
				"az2");

		if (uiClassic && !"global-toolbar".equals(skinObject.getViewID())) {
			skinObject.setVisible(false);
			return null;
		}

			// walk up skins to see if toolbar explicitly disabled (for pop-out views for example)
		
		SWTSkinObject temp = skinObject;
		
		while( temp != null ){
			
			int visible = temp.getSkin().getSkinProperties().getIntValue( "mdientry.toolbar.visible", 1 );
			
			if ( visible == 0 ){
				
				skinObject.setVisible(false);
				
				return null;	
			}
			
			temp = temp.getParent();
		}
		
		buttonListener = new toolbarButtonListener();


		if (firstTimeEver) {
			firstTimeEver = false;
			setupToolBarItems(uiClassic);
		}
		tbm.addListener(this);

		if (uiClassic) {
			bulkSetupItems("classic", "toolbar.area.sitem");
		}
		bulkSetupItems(UIToolBarManager.GROUP_MAIN, "toolbar.area.sitem");
		bulkSetupItems("views", "toolbar.area.vitem");
		
		String[] groupIDs = tbm.getGroupIDs();
		for (String groupID : groupIDs) {
			if ("classic".equals(groupID)
					|| UIToolBarManager.GROUP_MAIN.equals(groupID)
					|| "views".equals(groupID)) {
				continue;
			}
			bulkSetupItems(groupID, "toolbar.area.sitem");
		}

		initComplete = true;

		synchronized (listeners) {
			for (ToolBarViewListener l : listeners) {
				try {
					l.toolbarViewInitialized(this);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}

		return null;
	}

	private void setupToolBarItems(boolean uiClassic) {
		ToolBarItem item;

		if (uiClassic) {
			// ==OPEN
			item = createItem(this, "open", "image.toolbar.open", "Button.add");
			item.setDefaultActivationListener(new UIToolBarActivationListener() {
				public boolean toolBarItemActivated(ToolBarItem item,
						long activationType, Object datasource) {
					if (activationType != ACTIVATIONTYPE_NORMAL) {
						return false;
					}
					UIFunctionsManagerSWT.getUIFunctionsSWT().openTorrentWindow();
					return true;
				}
			});
			item.setAlwaysAvailable(true);
			item.setGroupID("classic");
			tbm.addToolBarItem(item, false);

			// ==SEARCH
			item = createItem(this, "search", "search", "Button.search");
			item.setDefaultActivationListener(new UIToolBarActivationListener() {
				public boolean toolBarItemActivated(ToolBarItem item,
						long activationType, Object datasource) {
					if (activationType != ACTIVATIONTYPE_NORMAL) {
						return false;
					}
					UIFunctionsManagerSWT.getUIFunctionsSWT().promptForSearch();
					return true;
				}
			});
			item.setAlwaysAvailable(true);
			item.setGroupID("classic");
			tbm.addToolBarItem(item, false);
		}

		if (!uiClassic) {
			// ==play
			item = createItem(this, "play", "image.button.play", "iconBar.play");
			item.setDefaultActivationListener(new UIToolBarActivationListener() {
				public boolean toolBarItemActivated(ToolBarItem item,
						long activationType, Object datasource) {
					if (activationType != ACTIVATIONTYPE_NORMAL) {
						return false;
					}
					ISelectedContent[] sc = SelectedContentManager.getCurrentlySelectedContent();
					if (sc != null && sc.length > 0) {

						if (PlayUtils.canStreamDS(sc[0], sc[0].getFileIndex(),true)) {
							TorrentListViewsUtils.playOrStreamDataSource(sc[0],
									DLReferals.DL_REFERAL_TOOLBAR, true, false);
						} else {
							TorrentListViewsUtils.playOrStreamDataSource(sc[0],
									DLReferals.DL_REFERAL_TOOLBAR, false, true);
						}
					}
					return false;
				}
			});
			tbm.addToolBarItem(item, false);
		}

		// ==run
		item = createItem(this, "run", "image.toolbar.run", "iconBar.run");
		item.setDefaultActivationListener(new UIToolBarActivationListener() {
			public boolean toolBarItemActivated(ToolBarItem item,
					long activationType, Object datasource) {
				if (activationType != ACTIVATIONTYPE_NORMAL) {
					return false;
				}
				TableView tv = SelectedContentManager.getCurrentlySelectedTableView();
				Object[] ds;
				if (tv != null) {
					ds = tv.getSelectedDataSources().toArray();
				} else {
					ds = SelectedContentManager.getDMSFromSelectedContent();
				}
				if (ds != null) {
					TorrentUtil.runDataSources(ds);
					return true;
				}
				return false;
			}
		});
		tbm.addToolBarItem(item, false);
		//addToolBarItem(item, "toolbar.area.sitem", so2nd);

		if (uiClassic) {
			// ==TOP
			item = createItem(this, "top", "image.toolbar.top", "iconBar.top");
			item.setDefaultActivationListener(new UIToolBarActivationListener() {
				public boolean toolBarItemActivated(ToolBarItem item,
						long activationType, Object datasource) {
					if (activationType == ACTIVATIONTYPE_NORMAL) {
						return moveTop();
					}

					return false;
				}
			});
			tbm.addToolBarItem(item, false);
		}

		// ==UP
		item = createItem(this, "up", "image.toolbar.up", "v3.iconBar.up");
		item.setDefaultActivationListener(new UIToolBarActivationListener() {
			public boolean toolBarItemActivated(ToolBarItem item,
					long activationType, Object datasource) {
				if (activationType == ACTIVATIONTYPE_NORMAL) {
					if (!AzureusCoreFactory.isCoreRunning()) {
						return false;
					}
					DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
					if (dms != null) {
						Arrays.sort(dms, new Comparator<DownloadManager>() {
							public int compare(DownloadManager a, DownloadManager b) {
								return a.getPosition() - b.getPosition();
							}
						});
						GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
						for (int i = 0; i < dms.length; i++) {
							DownloadManager dm = dms[i];
							if (gm.isMoveableUp(dm)) {
								gm.moveUp(dm);
							}
						}
					}
				} else if (activationType == ACTIVATIONTYPE_HELD) {
					return moveTop();
				}
				return false;
			}
		});
		tbm.addToolBarItem(item, false);

		// ==down
		item = createItem(this, "down", "image.toolbar.down", "v3.iconBar.down");
		item.setDefaultActivationListener(new UIToolBarActivationListener() {
			public boolean toolBarItemActivated(ToolBarItem item,
					long activationType, Object datasource) {
				if (activationType == ACTIVATIONTYPE_NORMAL) {
					if (!AzureusCoreFactory.isCoreRunning()) {
						return false;
					}

					GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
					DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
					if (dms != null) {
						Arrays.sort(dms, new Comparator<DownloadManager>() {
							public int compare(DownloadManager a, DownloadManager b) {
								return b.getPosition() - a.getPosition();
							}
						});
						for (int i = 0; i < dms.length; i++) {
							DownloadManager dm = dms[i];
							if (gm.isMoveableDown(dm)) {
								gm.moveDown(dm);
							}
						}
						return true;
					}
				} else if (activationType == ACTIVATIONTYPE_HELD) {
					return moveBottom();
				}
				return false;
			}
		});
		tbm.addToolBarItem(item, false);

		if (uiClassic) {
			// ==BOTTOM
			item = createItem(this, "bottom", "image.toolbar.bottom",
					"iconBar.bottom");
			item.setDefaultActivationListener(new UIToolBarActivationListener() {
				public boolean toolBarItemActivated(ToolBarItem item,
						long activationType, Object datasource) {
					if (activationType != ACTIVATIONTYPE_NORMAL) {
						return false;
					}
					return moveBottom();
				}
			});
			tbm.addToolBarItem(item, false);
		}
		/*
				// ==start
				item = createItemSO(this, "start", "image.toolbar.start", "iconBar.start");
				item.setDefaultActivation(new UIToolBarActivationListener() {
					public boolean toolBarItemActivated(ToolBarItem item, long activationType) {
						if (activationType != ACTIVATIONTYPE_NORMAL) {
							return false;
						}
						DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
						if (dms != null) {
							TorrentUtil.queueDataSources(dms, true);
							return true;
						}
						return false;
					}
				});
				addToolBarItem(item, "toolbar.area.sitem", so2nd);
				//SWTSkinObjectContainer so = (SWTSkinObjectContainer) item.getSkinButton().getSkinObject();
				//so.setDebugAndChildren(true);
				addSeperator(so2nd);

				// ==stop
				item = createItemSO(this, "stop", "image.toolbar.stop", "iconBar.stop");
				item.setDefaultActivation(new UIToolBarActivationListener() {
					public boolean toolBarItemActivated(ToolBarItem item, long activationType) {
						if (activationType != ACTIVATIONTYPE_NORMAL) {
							return false;
						}
		 				ISelectedContent[] currentContent = SelectedContentManager.getCurrentlySelectedContent();
						TorrentUtil.stopDataSources(currentContent);
						return true;
					}
				});
				addToolBarItem(item, "toolbar.area.sitem", so2nd);
				addSeperator(so2nd);
		*/
		// ==startstop
		item = createItem(this, "startstop", "image.toolbar.startstop.start",
				"iconBar.startstop");
		item.setDefaultActivationListener(new UIToolBarActivationListener() {
			public boolean toolBarItemActivated(ToolBarItem item,
					long activationType, Object datasource) {
				if (activationType != ACTIVATIONTYPE_NORMAL) {
					return false;
				}
				ISelectedContent[] currentContent = SelectedContentManager.getCurrentlySelectedContent();
				TorrentUtil.stopOrStartDataSources(currentContent);
				return true;
			}
		});
		tbm.addToolBarItem(item, false);

		// ==remove
		item = createItem(this, "remove", "image.toolbar.remove", "iconBar.remove");
		item.setDefaultActivationListener(new UIToolBarActivationListener() {
			public boolean toolBarItemActivated(ToolBarItem item,
					long activationType, Object datasource) {
				if (activationType != ACTIVATIONTYPE_NORMAL) {
					return false;
				}
				TorrentUtil.removeDataSources(
						SelectedContentManager.getCurrentlySelectedContent());
				return true;
			}
		});
		tbm.addToolBarItem(item, false);

		///////////////////////

		// == mode big
		item = createItem(this, "modeBig", "image.toolbar.table_large",
				"v3.iconBar.view.big");
		item.setGroupID("views");
		tbm.addToolBarItem(item, false);

		// == mode small
		item = createItem(this, "modeSmall", "image.toolbar.table_normal",
				"v3.iconBar.view.small");
		item.setGroupID("views");
		tbm.addToolBarItem(item, false);
	}

	public void currentlySelectedContentChanged(
			ISelectedContent[] currentContent, String viewID) {
		//System.err.println("currentlySelectedContentChanged " + viewID + ";" + currentContent + ";" + getMainSkinObject() + this + " via " + Debug.getCompressedStackTrace());
		refreshCoreToolBarItems();
		UIFunctionsManagerSWT.getUIFunctionsSWT().refreshTorrentMenu();
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {

		if (showCalled) {
			return null;
		}
		showCalled = true;

		Object object = super.skinObjectShown(skinObject, params);
		
  	ToolBarItem[] allToolBarItems = tbm.getAllSWTToolBarItems();
  	for (int i = 0; i < allToolBarItems.length; i++) {
  		ToolBarItem toolBarItem = allToolBarItems[i];
  		toolBarItem.addToolBarItemListener(this);
  		uiFieldChanged(toolBarItem);
  	}

		
		SelectedContentManager.addCurrentlySelectedContentListener(this);
		return object;
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		showCalled = false;
		SelectedContentManager.removeCurrentlySelectedContentListener(this);

  	ToolBarItem[] allToolBarItems = tbm.getAllSWTToolBarItems();
  	for (int i = 0; i < allToolBarItems.length; i++) {
  		ToolBarItem toolBarItem = allToolBarItems[i];
  		toolBarItem.removeToolBarItemListener(this);
  	}

		return super.skinObjectHidden(skinObject, params);
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectDestroyed(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		tbm.removeListener(this);

		return super.skinObjectDestroyed(skinObject, params);
	}

	public boolean triggerToolBarItem(ToolBarItem item, long activationType,
			Object datasource) {
		if (!isVisible()) {
			if (DEBUG) {
				Debug.out("Trying to triggerToolBarItem when toolbar is not visible");
			}
			return false;
		}
		if (triggerViewToolBar(item, activationType, datasource)) {
			return true;
		}

		UIToolBarActivationListener defaultActivation = item.getDefaultActivationListener();
		if (defaultActivation != null) {
			return defaultActivation.toolBarItemActivated(item, activationType,
					datasource);
		}

		if (DEBUG) {
			String viewID = SelectedContentManager.getCurrentySelectedViewID();
			System.out.println("Warning: Fallback of toolbar button " + item.getID()
					+ " via " + viewID + " view");
		}

		return false;
	}

	protected boolean moveBottom() {
		if (!AzureusCoreFactory.isCoreRunning()) {
			return false;
		}

		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
		if (dms != null) {
			gm.moveEnd(dms);
		}
		return true;
	}

	protected boolean moveTop() {
		if (!AzureusCoreFactory.isCoreRunning()) {
			return false;
		}
		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
		if (dms != null) {
			gm.moveTop(dms);
		}
		return true;
	}

	private FrequencyLimitedDispatcher refresh_limiter = new FrequencyLimitedDispatcher(
			new AERunnable() {
				private AERunnable lock = this;

				private boolean refresh_pending;

				public void runSupport() {
					synchronized (lock) {

						if (refresh_pending) {

							return;
						}
						refresh_pending = true;
					}

					if (DEBUG) {
						System.out.println("refreshCoreItems via "
								+ Debug.getCompressedStackTrace());
					}

					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {

							synchronized (lock) {

								refresh_pending = false;
							}

							_refreshCoreToolBarItems();
						}
					});
				}
			}, 250);

	private IdentityHashMap<DownloadManager, DownloadManagerListener> dm_listener_map = new IdentityHashMap<DownloadManager, DownloadManagerListener>();

	private SWTSkinObject soLastGroup;

	public void refreshCoreToolBarItems() {
		if (DEBUG) {
			System.out.println("refreshCoreItems Start via "
					+ Debug.getCompressedStackTrace());
		}
		refresh_limiter.dispatch();
	}

	public void _refreshCoreToolBarItems() {
		if (DEBUG && !isVisible()) {
			Debug.out("Trying to refresh core toolbar items when toolbar is not visible "
					+ this + getMainSkinObject());
		}

		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();

		if (mdi != null) {
			UIToolBarItem[] allToolBarItems = tbm.getAllToolBarItems();
			MdiEntrySWT entry = mdi.getCurrentEntrySWT();
			Map<String, Long> mapStates = new HashMap<String, Long>();
			if (entry != null) {
				UIToolBarEnablerBase[] enablers = entry.getToolbarEnablers();
				for (UIToolBarEnablerBase enabler : enablers) {
					if (enabler instanceof UIPluginViewToolBarListener) {
						try {
							((UIPluginViewToolBarListener) enabler).refreshToolBarItems(mapStates);
						} catch (Throwable e) {
							Debug.out(e); // don't trust them plugins
						}
					}
				}
			}

			ISelectedContent[] currentContent = SelectedContentManager.getCurrentlySelectedContent();
			//System.out.println("_refreshCoreToolBarItems(" + currentContent.length + ", " + entry + " via " + Debug.getCompressedStackTrace());

			synchronized (dm_listener_map) {

				Map<DownloadManager, DownloadManagerListener> copy = new IdentityHashMap<DownloadManager, DownloadManagerListener>(
						dm_listener_map);

				for (ISelectedContent content : currentContent) {

					DownloadManager dm = content.getDownloadManager();

					if (dm != null) {

						copy.remove(dm);

						// so in files view we can have multiple selections that map onto the SAME download manager
						// - ensure that we only add the listener once!

						if (!dm_listener_map.containsKey(dm)) {

							DownloadManagerListener l = new DownloadManagerListener() {
								public void stateChanged(DownloadManager manager, int state) {
									refreshCoreToolBarItems();
								}

								public void downloadComplete(DownloadManager manager) {
									refreshCoreToolBarItems();
								}

								public void completionChanged(DownloadManager manager,
										boolean bCompleted) {
									refreshCoreToolBarItems();
								}

								public void positionChanged(DownloadManager download,
										int oldPosition, int newPosition) {
									refreshCoreToolBarItems();
								}

								public void filePriorityChanged(DownloadManager download,
										DiskManagerFileInfo file) {
									refreshCoreToolBarItems();
								}
							};

							dm.addListener(l, false);

							dm_listener_map.put(dm, l);

							//System.out.println( "Added " + dm.getDisplayName() + " - size=" + dm_listener_map.size());
						}
					}
				}

				for (Map.Entry<DownloadManager, DownloadManagerListener> e : copy.entrySet()) {

					DownloadManager dm = e.getKey();

					dm.removeListener(e.getValue());

					dm_listener_map.remove(dm);

					//System.out.println( "Removed " + dm.getDisplayName() + " - size=" + dm_listener_map.size());
				}
			}

			boolean has1Selection = currentContent.length == 1;

			boolean can_play = false;
			boolean can_stream = false;

			boolean stream_permitted = false;

			if (has1Selection) {

				if (!(currentContent[0] instanceof ISelectedVuzeFileContent)) {

					can_play = PlayUtils.canPlayDS(currentContent[0],
							currentContent[0].getFileIndex(),false);
					can_stream = PlayUtils.canStreamDS(currentContent[0],
							currentContent[0].getFileIndex(),false);

					if (can_stream) {

						stream_permitted = PlayUtils.isStreamPermitted();
					}
				}
			}

			// allow a tool-bar enabler to manually handle play/stream events

			if (mapStates.containsKey("play")) {
				can_play |= (mapStates.get("play") & UIToolBarItem.STATE_ENABLED) > 0;
			}
			if (mapStates.containsKey("stream")) {
				can_stream |= (mapStates.get("stream") & UIToolBarItem.STATE_ENABLED) > 0;
			}

			mapStates.put("play", can_play | can_stream ? UIToolBarItem.STATE_ENABLED
					: 0);

			UIToolBarItem pitem = tbm.getToolBarItem("play");

			if (pitem != null) {

				if (can_stream) {

					pitem.setImageID(stream_permitted ? "image.button.stream"
							: "image.button.pstream");
					pitem.setTextID(stream_permitted ? "iconBar.stream"
							: "iconBar.pstream");

				} else {

					pitem.setImageID("image.button.play");
					pitem.setTextID("iconBar.play");
				}
			}

			UIToolBarItem ssItem = tbm.getToolBarItem("startstop");
			if (ssItem != null){
				
				boolean shouldStopGroup;
				
					// if no selected content set then use the 'start' key to determine the start/stop
					// toolbar state (required for archived downloads)
					// alternative solution would be for the view to start updating the current selected
					// content which is a little painful
				
				if ( 	currentContent.length == 0 &&
						mapStates.containsKey( "start" ) && 
						!mapStates.containsKey( "stop" ) && 
						( mapStates.get("start") & UIToolBarItem.STATE_ENABLED) > 0 ){
					
					shouldStopGroup = false;
					
				}else{
					shouldStopGroup = TorrentUtil.shouldStopGroup(currentContent);
				}
				
				ssItem.setTextID(shouldStopGroup ? "iconBar.stop" : "iconBar.start");
				ssItem.setImageID("image.toolbar.startstop."
						+ (shouldStopGroup ? "stop" : "start"));
			}

			
			Map<String, Long> fallBackStates = TorrentUtil.calculateToolbarStates(currentContent, null);
			for (String key : fallBackStates.keySet()) {
				if (!mapStates.containsKey(key)) {
					mapStates.put(key, fallBackStates.get(key));
				}
			}


			final String[] TBKEYS = new String[] {
				"play",
				"run",
				"top",
				"up",
				"down",
				"bottom",
				"start",
				"stop",
				"startstop",
				"remove"
			};
			for (String key : TBKEYS) {
				if (!mapStates.containsKey(key)) {
					mapStates.put(key, 0L);
				}
			}

			for (int i = 0; i < allToolBarItems.length; i++) {
				UIToolBarItem toolBarItem = allToolBarItems[i];
				Long state = mapStates.get(toolBarItem.getID());
				if (state != null) {
					toolBarItem.setState(state);
				}
			}

			if (ssItem != null) {

				// fallback to handle start/stop settings when no explicit selected content (e.g. for devices transcode view)

				if (currentContent.length == 0 && !mapStates.containsKey("startstop")) {

					boolean can_stop = mapStates.containsKey("stop")
							&& (mapStates.get("stop") & UIToolBarItem.STATE_ENABLED) > 0;
					boolean can_start = mapStates.containsKey("start")
							&& (mapStates.get("start") & UIToolBarItem.STATE_ENABLED) > 0;

					if (can_start && can_stop) {

						can_stop = false;
					}

					if (can_start | can_stop) {
						ssItem.setTextID(can_stop ? "iconBar.stop" : "iconBar.start");
						ssItem.setImageID("image.toolbar.startstop."
								+ (can_stop ? "stop" : "start"));

						ssItem.setState(1);

					} else {

						ssItem.setState(0);
					}
				}
			}
		}
		
	}

	private boolean triggerViewToolBar(ToolBarItem item, long activationType,
			Object datasource) {
		if (DEBUG && !isVisible()) {
			Debug.out("Trying to triggerViewToolBar when toolbar is not visible");
			return false;
		}
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			MdiEntrySWT entry = mdi.getCurrentEntrySWT();
			UIToolBarEnablerBase[] enablers = entry.getToolbarEnablers();
			for (UIToolBarEnablerBase enabler : enablers) {
				if (enabler instanceof UIPluginViewToolBarListener) {
					if (((UIPluginViewToolBarListener) enabler).toolBarItemActivated(
							item, activationType, datasource)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private void bulkSetupItems(String groupID, String templatePrefix) {
		String[] idsByGroup = tbm.getToolBarIDsByGroup(groupID);
		SWTSkinObjectContainer groupSO = getGroupSO(groupID);
		SWTSkinObject[] children = groupSO.getChildren();
		for (SWTSkinObject so : children) {
			so.dispose();
		}
		
		for (int i = 0; i < idsByGroup.length; i++) {
			String itemID = idsByGroup[i];
			UIToolBarItem item = tbm.getToolBarItem(itemID);
			if (item instanceof ToolBarItem) {


				int position = 0;
				int size = idsByGroup.length;
				if (size == 1) {
					position = SWT.SINGLE;
				} else if (i == 0) {
					position = SWT.LEFT;
				} else if (i == size - 1) {
					addSeperator(groupID);
					position = SWT.RIGHT;
				} else {
					addSeperator(groupID);
				}
				createItemSO((ToolBarItem) item, templatePrefix, position);
			}
		}

		addNonToolBar("toolbar.area.sitem.left2", groupID);
	}

	private Control getLastControl(String groupID) {
		SWTSkinObjectContainer groupSO = getGroupSO(groupID);
		SWTSkinObject[] children = groupSO.getChildren();
		if (children == null || children.length == 0) {
			return null;
		}
		return children[children.length - 1].getControl();
	}
	
	private void createItemSO(ToolBarItem item, String templatePrefix,
			 int position) {

		ToolBarItemSO existingItemSO = mapToolBarItemToSO.get(item);
		if (existingItemSO != null) {
			SWTSkinObject so = existingItemSO.getSO();
			if (so != null) {
				so.dispose();
			}
		}

		String templateID = templatePrefix;
		if (position == SWT.RIGHT) {
			templateID += ".right";
		} else if (position == SWT.LEFT) {
			templateID += ".left";
		} else if (position == SWT.SINGLE) {
			templateID += ".lr";
		}

		Control attachToControl = getLastControl(item.getGroupID());
		String id = "toolbar:" + item.getID();
		SWTSkinObject so = skin.createSkinObject(id, templateID, getGroupSO(item.getGroupID()));
		if (so != null) {
			ToolBarItemSO itemSO;
			itemSO = new ToolBarItemSO((UIToolBarItemImpl) item, so);

			if (attachToControl != null) {
				FormData fd = (FormData) so.getControl().getLayoutData();
				fd.left = new FormAttachment(attachToControl);
			}
			
			initSO(so, itemSO);

			if (initComplete) {
				Utils.relayout(so.getControl().getParent());
			}
		}
	}
	
	private SWTSkinObjectContainer getGroupSO(String groupID) {
		String soID = "toolbar-group-" + groupID;
		SWTSkinObjectContainer soGroup = (SWTSkinObjectContainer) skin.getSkinObjectByID(
				soID, soMain);

		if (soGroup == null) {
			soGroup = (SWTSkinObjectContainer) skin.createSkinObject(soID,
					"toolbar.group", soMain);
			FormData fd = (FormData) soGroup.getControl().getLayoutData();
			if (soLastGroup != null) {
				fd.left = new FormAttachment(soLastGroup.getControl(), 0, SWT.RIGHT);
			} else {
				fd.left = new FormAttachment(0, 2);
			}
		}

		soLastGroup = soGroup;

		return soGroup;
	}

	private void initSO(SWTSkinObject so, ToolBarItemSO itemSO) {
		ToolBarItem item = itemSO.getBase();
		itemSO.setSO(so);
		String toolTip = item.getToolTip();
		if (toolTip != null) {
			so.setTooltipID("!" + toolTip + "!");
		} else {
			so.setTooltipID(item.getTooltipID());
		}
		so.setData("toolbaritem", item);
		SWTSkinButtonUtility btn = (SWTSkinButtonUtility) so.getData("btn");
		if (btn == null) {
			btn = new SWTSkinButtonUtility(so, "toolbar-item-image");
			so.setData("btn", btn);
		}
		btn.setImage(item.getImageID());
		btn.addSelectionListener(buttonListener);
		itemSO.setSkinButton(btn);

		SWTSkinObject soTitle = skin.getSkinObject("toolbar-item-title", so);
		if (soTitle instanceof SWTSkinObjectText) {
			((SWTSkinObjectText) soTitle).setTextID(item.getTextID());
			itemSO.setSkinTitle((SWTSkinObjectText) soTitle);
		}
		mapToolBarItemToSO.put(item, itemSO);
	}

	// @see com.aelitis.azureus.ui.common.ToolBarItem.ToolBarItemListener#uiFieldChanged(com.aelitis.azureus.ui.common.ToolBarItem)
	public void uiFieldChanged(ToolBarItem item) {
		ToolBarItemSO itemSO = mapToolBarItemToSO.get(item);
		if (itemSO != null) {
			itemSO.updateUI();
		}
	}

	private void addSeperator(String groupID) {
		addSeperator("toolbar.area.sitem.sep", groupID);
	}

	private void addSeperator(String id, String groupID) {
		SWTSkinObjectContainer soGroup = getGroupSO(groupID);
		Control lastControl = getLastControl(groupID);
		SWTSkinObject so = skin.createSkinObject("toolbar_sep" + Math.random(), id,
				soGroup);
		if (so != null) {
			if (lastControl != null) {
				FormData fd = (FormData) so.getControl().getLayoutData();
				fd.left = new FormAttachment(lastControl, fd.left == null ? 0
						: fd.left.offset);
			}
		}
	}

	private void addNonToolBar(String skinid, String groupID) {
		SWTSkinObjectContainer soGroup = getGroupSO(groupID);
		Control lastControl = getLastControl(groupID);
		SWTSkinObject so = skin.createSkinObject("toolbar_d" + Math.random(),
				skinid, soGroup);
		if (so != null) {
			if (lastControl != null) {
				FormData fd = (FormData) so.getControl().getLayoutData();
				fd.left = new FormAttachment(lastControl, fd.left == null ? 0
						: fd.left.offset);
			}
		}
	}

	/**
	 * @param showText the showText to set
	 */
	public void setShowText(boolean showText) {
		this.showText = showText;
		UIToolBarItem[] allToolBarItems = tbm.getAllToolBarItems();
		for (int i = 0; i < allToolBarItems.length; i++) {
			UIToolBarItem tbi = allToolBarItems[i];
			SWTSkinObject so = ((ToolBarItemSO) tbi).getSkinButton().getSkinObject();
			SWTSkinObject soTitle = skin.getSkinObject("toolbar-item-title", so);
			if (soTitle != null) {
				soTitle.setVisible(showText);
			}
		}
	}

	/**
	 * @return the showText
	 */
	public boolean getShowText() {
		return showText;
	}

	private static class toolbarButtonListener
		extends ButtonListenerAdapter
	{
		public void pressed(SWTSkinButtonUtility buttonUtility,
				SWTSkinObject skinObject, int stateMask) {
			ToolBarItem item = (ToolBarItem) buttonUtility.getSkinObject().getData(
					"toolbaritem");
			boolean rightClick = (stateMask & (SWT.BUTTON3 | SWT.MOD4)) > 0;
			Object o = SelectedContentManager.convertSelectedContentToObject(null);
			item.triggerToolBarItem(rightClick
					? UIToolBarActivationListener.ACTIVATIONTYPE_RIGHTCLICK
					: UIToolBarActivationListener.ACTIVATIONTYPE_NORMAL, o);
		}

		public boolean held(SWTSkinButtonUtility buttonUtility) {
			ToolBarItem item = (ToolBarItem) buttonUtility.getSkinObject().getData(
					"toolbaritem");
			buttonUtility.getSkinObject().switchSuffix("", 0, false, true);

			Object o = SelectedContentManager.convertSelectedContentToObject(null);
			boolean triggerToolBarItemHold = item.triggerToolBarItem(
					UIToolBarActivationListener.ACTIVATIONTYPE_HELD, o);
			return triggerToolBarItemHold;
		}
	}

	public void addListener(ToolBarViewListener l) {
		synchronized (listeners) {
			listeners.add(l);

			if (initComplete) {
				try {
					l.toolbarViewInitialized(this);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
	}

	public void removeListener(ToolBarViewListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}

	public interface ToolBarViewListener
	{
		public void toolbarViewInitialized(ToolBarView tbv);
	}

	// @see org.gudy.azureus2.ui.swt.pluginsimpl.UIToolBarManagerImpl.ToolBarManagerListener#toolbarItemRemoved(org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem)
	public void toolbarItemRemoved(final UIToolBarItem toolBarItem) {
		ToolBarItemSO itemSO = mapToolBarItemToSO.get(toolBarItem);
		if (itemSO == null) {
			return;
		}

		itemSO.dispose();
		final SWTSkinObject so = itemSO.getSO();
		if (so != null) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {

					String groupID = toolBarItem.getGroupID();
					
					final String[] idsByGroup = tbm.getToolBarIDsByGroup(groupID);
					
					if (idsByGroup.length <= 1) {
						boolean b = initComplete;
						initComplete = false;
						bulkSetupItems(groupID, "toolbar.area.sitem");
						initComplete = b;
						so.getParent().relayout();
						return;
					}
					
					int posToolBarItem = -1;
					String id = toolBarItem.getID();


					Control soControl = so.getControl();
					
					
					SWTSkinObject middleSO = mapToolBarItemToSO.get(
							tbm.getToolBarItem(idsByGroup[idsByGroup.length / 2])).getSO();
					

					SWTSkinObject[] children = ((SWTSkinObjectContainer) so.getParent()).getChildren();
					int middle = -1;
					for (int i = 0; i < children.length; i++) {
						if (children[i] == middleSO) {
							middle = i;
							break;
						}
					}
					
					if (middle == -1) {
						return;
					}
					
					
					children[middle].dispose();
					children[middle + 1].dispose();
					
					Control controlLeft = children[middle - 1].getControl();
					FormData fd = (FormData) children[middle + 2].getControl().getLayoutData();
					fd.left.control = controlLeft;
					Utils.relayout(children[middle + 2].getControl());
					
					int positionInGroup = 0;
					UIToolBarItem curItem = tbm.getToolBarItem(idsByGroup[positionInGroup]);

					children = ((SWTSkinObjectContainer) so.getParent()).getChildren();
					for (int i = 0; i < children.length; i++) {
						SWTSkinObject child = children[i];
						
						ToolBarItem item = (ToolBarItem) child.getData("toolbaritem");
						if (item != null && item.getGroupID().equals(groupID)) {
						
							ToolBarItemSO toolBarItemSO = mapToolBarItemToSO.get(curItem);
							initSO(child, toolBarItemSO);
							positionInGroup++;
							if (positionInGroup >= idsByGroup.length) {
								break;
							}
							curItem = tbm.getToolBarItem(idsByGroup[positionInGroup]);
						}
					}

					so.getParent().relayout();
				}
			});
		}
		mapToolBarItemToSO.remove(toolBarItem);
	}

	// @see org.gudy.azureus2.ui.swt.pluginsimpl.UIToolBarManagerImpl.ToolBarManagerListener#toolbarItemAdded(org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem)
	public void toolbarItemAdded(final UIToolBarItem item) {
		if (isVisible()) {
  		if (item instanceof ToolBarItem) {
  			ToolBarItem toolBarItem = (ToolBarItem) item;
  			toolBarItem.addToolBarItemListener(this);
  		}
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				boolean b = initComplete;
				initComplete = false;
				bulkSetupItems(item.getGroupID(), "toolbar.area.sitem");
				initComplete = b;
				
				Utils.execSWTThreadLater(0, new Runnable() {
					public void run() {
						Utils.relayout(soMain.getControl());
					}
				});
			}
		});
	}

}
