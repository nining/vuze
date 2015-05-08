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

package com.aelitis.azureus.ui.swt.shells.main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Menu;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationChecker;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.host.TRHost;
import org.gudy.azureus2.core3.tracker.host.TRHostListener;
import org.gudy.azureus2.core3.tracker.host.TRHostTorrent;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncController;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadStub;
import org.gudy.azureus2.plugins.download.DownloadStubEvent;
import org.gudy.azureus2.plugins.download.DownloadStubListener;
import org.gudy.azureus2.plugins.sharing.ShareManager;
import org.gudy.azureus2.plugins.sharing.ShareManagerListener;
import org.gudy.azureus2.plugins.sharing.ShareResource;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener2;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.MenuFactory;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.clientstats.ClientStatsView;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.feature.FeatureManagerUI;
import com.aelitis.azureus.ui.swt.mdi.*;
import com.aelitis.azureus.ui.swt.views.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.DataSourceUtils;
import com.aelitis.azureus.util.FeatureUtils;

public class MainMDISetup
{
	public static void setupSideBar(final MultipleDocumentInterfaceSWT mdi,
			final MdiListener l) {
		if (Utils.isAZ2UI()) {
			setupSidebarClassic(mdi);
		} else {
			setupSidebarVuzeUI(mdi);
		}

		mdi.registerEntry(SideBar.SIDEBAR_SECTION_TORRENT_DETAILS + ".*",
				new MdiEntryCreationListener2() {
					public MdiEntry createMDiEntry(MultipleDocumentInterface mdi,
							String id, Object datasource, Map<?, ?> params) {
						String hash = DataSourceUtils.getHash(datasource);
						if (hash != null) {
							id = MultipleDocumentInterface.SIDEBAR_SECTION_TORRENT_DETAILS 
									+ "_" + hash;
						}
						return createTorrentDetailEntry(mdi, id, datasource);
					}
				});

		PluginInterface pi = PluginInitializer.getDefaultInterface();

		pi.getUIManager().addUIListener(
				new UIManagerListener2() {
					public void UIDetached(UIInstance instance) {
					}

					public void UIAttached(UIInstance instance) {
					}

					public void UIAttachedComplete(UIInstance instance) {

						PluginInitializer.getDefaultInterface().getUIManager().removeUIListener(
								this);

						MdiEntry currentEntry = mdi.getCurrentEntry();
						if (currentEntry != null) {
							// User or another plugin selected an entry
							return;
						}

						final String CFG_STARTTAB = "v3.StartTab";
						final String CFG_STARTTAB_DS = "v3.StartTab.ds";
						String startTab;
						String datasource = null;
						boolean showWelcome = COConfigurationManager.getBooleanParameter("v3.Show Welcome");
						if (ConfigurationChecker.isNewVersion()) {
							showWelcome = true;
						}

						ContentNetwork startupCN = ContentNetworkManagerFactory.getSingleton().getStartupContentNetwork();
						if (startupCN == null || !startupCN.isServiceSupported(ContentNetwork.SERVICE_WELCOME)) {
							showWelcome = false;
						}

						if (showWelcome) {
							startTab = SideBar.SIDEBAR_SECTION_WELCOME;
						} else {
							if (!COConfigurationManager.hasParameter(CFG_STARTTAB, true)) {
								COConfigurationManager.setParameter(CFG_STARTTAB,
										SideBar.SIDEBAR_SECTION_LIBRARY);
							}
							startTab = COConfigurationManager.getStringParameter(CFG_STARTTAB);
							datasource = COConfigurationManager.getStringParameter(
									CFG_STARTTAB_DS, null);
						}
						if (startTab.equals(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS)) {
							SBC_PlusFTUX.setSourceRef("lastview");
						}
						if (!mdi.loadEntryByID(startTab, true, false, datasource)) {
							mdi.showEntryByID(SideBar.SIDEBAR_SECTION_LIBRARY);
						}
						if (l != null) {
							mdi.addListener(l);
						}
					}
				});
		;

		COConfigurationManager.addAndFireParameterListener(
				"Beta Programme Enabled", new ParameterListener() {
					public void parameterChanged(String parameterName) {
						boolean enabled = COConfigurationManager.getBooleanParameter("Beta Programme Enabled");
						if (enabled) {
							mdi.loadEntryByID(
									MultipleDocumentInterface.SIDEBAR_SECTION_BETAPROGRAM, false);
						}
					}
				});

		mdi.registerEntry(StatsView.VIEW_ID, new MdiEntryCreationListener() {
			public MdiEntry createMDiEntry(String id) {
				MdiEntry entry = mdi.createEntryFromEventListener(
						MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS, new StatsView(),
						id, true, null, null);
				return entry;
			}
		});

		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_ALLPEERS,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromEventListener(
								MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
								new PeersSuperView(), id, true, null, null);
						entry.setImageLeftID("image.sidebar.allpeers");
						return entry;
					}
				});
		
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_LOGGER,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromEventListener(
								MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS,
								new LoggerView(), id, true, null, null);
						return entry;
					}
				});

		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_TAGS,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromSkinRef(
								MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
								MultipleDocumentInterface.SIDEBAR_SECTION_TAGS, "tagsview",
								"{mdi.entry.tagsoverview}", null, null, true, null);
						entry.setImageLeftID("image.sidebar.tag-overview");
						return entry;
					}
				});
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_TAG_DISCOVERY,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromSkinRef(
								MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
								MultipleDocumentInterface.SIDEBAR_SECTION_TAG_DISCOVERY, "tagdiscoveryview",
								"{mdi.entry.tagdiscovery}", null, null, true, null);
						entry.setImageLeftID("image.sidebar.tag-overview");
						return entry;
					}
				});
		
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_CHAT,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromSkinRef(
								MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
								MultipleDocumentInterface.SIDEBAR_SECTION_CHAT, "chatsview",
								"{mdi.entry.chatsoverview}", null, null, true, null);
						entry.setImageLeftID("image.sidebar.chat-overview");
						return entry;
					}
				});
		
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_ARCHIVED_DOWNLOADS,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						
						final org.gudy.azureus2.plugins.download.DownloadManager download_manager = PluginInitializer.getDefaultInterface().getDownloadManager();
						
						final ViewTitleInfo title_info = 
							new ViewTitleInfo() 
							{
								public Object 
								getTitleInfoProperty(
									int propertyID) 
								{
									if ( propertyID == TITLE_INDICATOR_TEXT ){
										
										int num = download_manager.getDownloadStubCount();
																				
										return( String.valueOf( num ) );
									}
									
									return null;
								}
							};
						
						MdiEntry entry = mdi.createEntryFromSkinRef(
								MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
								MultipleDocumentInterface.SIDEBAR_SECTION_ARCHIVED_DOWNLOADS, "archivedlsview",
								"{mdi.entry.archiveddownloadsview}", 
								title_info, null, true, null);
						
						entry.setImageLeftID( "image.sidebar.archive" );
						
						final DownloadStubListener stub_listener =
							new DownloadStubListener() 
							{	
								public void 
								downloadStubEventOccurred(
									DownloadStubEvent event )
								{
									ViewTitleInfoManager.refreshTitleInfo( title_info );
								}
							};
							
						download_manager.addDownloadStubListener( stub_listener, false );
						
						entry.addListener(
							new MdiCloseListener() {
								
								public void 
								mdiEntryClosed(
									MdiEntry entry, boolean userClosed) 
								{
									download_manager.removeDownloadStubListener( stub_listener );
								}
							});
						
						entry.addListener(
								new MdiEntryDropListener()
								{	
									public boolean 
									mdiEntryDrop(
										MdiEntry 		entry, 
										Object 			data ) 
									{
										if ( data instanceof String ){
										
											String str = (String)data;
											
											if ( str.startsWith( "DownloadManager\n" )){
												
												String[] bits = str.split( "\n" );
												
												org.gudy.azureus2.plugins.download.DownloadManager dm = PluginInitializer.getDefaultInterface().getDownloadManager();
												
												List<Download> downloads = new ArrayList<Download>();
												
												boolean	failed = false;
												
												for ( int i=1;i<bits.length;i++ ){
													
													byte[]	 hash = Base32.decode( bits[i] );
													
													try{
														Download download = dm.getDownload( hash );
														
														if ( download.canStubbify()){
															
															downloads.add( download );
															
														}else{
															
															failed = true;
														}
													}catch( Throwable e ){	
													}
												}
																									
												final boolean f_failed = failed;
												
												ManagerUtils.moveToArchive( 
													downloads, 
													new ManagerUtils.ArchiveCallback()
													{
														boolean error = f_failed;
														
														public void
														failed(
															DownloadStub		original,
															Throwable			e )
														{
															error = true;
														}
														
														public void
														completed()
														{
															if ( error ){
																
																String title 	= MessageText.getString( "archive.failed.title" );
																String text 	= MessageText.getString( "archive.failed.text" );
																
																MessageBoxShell prompter = 
																	new MessageBoxShell(
																		title, text, 
																		new String[] { MessageText.getString("Button.ok") }, 0 );
																																
																prompter.setAutoCloseInMS(0);
																
																prompter.open( null );
															}
														}
													});
											}
											
											return( true );
										}
										
										return false;
									}
								});
						
						return entry;
					}
				});
		
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_TORRENT_OPTIONS,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromEventListener(
								MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
								TorrentOptionsView.class,
								MultipleDocumentInterface.SIDEBAR_SECTION_TORRENT_OPTIONS, true,
								null, null);
						return entry;
					}
				});
		
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_MY_SHARES,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromEventListener(
								MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
								MySharesView.class,
								MultipleDocumentInterface.SIDEBAR_SECTION_MY_SHARES, true,
								null, null);
						return entry;
					}
				});
		
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_MY_TRACKER,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromEventListener(
								MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
								MyTrackerView.class,
								MultipleDocumentInterface.SIDEBAR_SECTION_MY_TRACKER, true,
								null, null);
						return entry;
					}
				});
		
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_CLIENT_STATS,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromEventListener(
								MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS,
								ClientStatsView.class,
								MultipleDocumentInterface.SIDEBAR_SECTION_CLIENT_STATS, true,
								null, null);
						return entry;
					}
				});
		
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_CONFIG,
				new MdiEntryCreationListener2() {

					public MdiEntry createMDiEntry(MultipleDocumentInterface mdi,
							String id, Object datasource, Map<?, ?> params) {

						String section = (datasource instanceof String)
								? ((String) datasource) : null;

						boolean uiClassic = COConfigurationManager.getStringParameter(
								"ui").equals("az2");
						if (uiClassic || COConfigurationManager.getBooleanParameter(
								"Show Options In Side Bar")) {
  						MdiEntry entry = ((MultipleDocumentInterfaceSWT) mdi).createEntryFromEventListener(
  								MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS,
  								ConfigView.class,
  								MultipleDocumentInterface.SIDEBAR_SECTION_CONFIG, true, null,
  								null);
  						return entry;
						}
						
						ConfigShell.getInstance().open(section);
						return null;
					}
				});
		
		try {
			final ShareManager share_manager = pi.getShareManager();
			if (share_manager.getShares().length > 0) {
				mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_MY_SHARES);
			} else {
  			share_manager.addListener(new ShareManagerListener() {
  				
  				public void resourceModified(ShareResource old_resource,
  						ShareResource new_resource) {
  				}
  				
  				public void resourceDeleted(ShareResource resource) {
  				}
  				
  				public void resourceAdded(ShareResource resource) {
  					share_manager.removeListener(this);
						mdi.loadEntryByID(
								MultipleDocumentInterface.SIDEBAR_SECTION_MY_SHARES, false);
  				}
  				
  				public void reportProgress(int percent_complete) {
  				}
  				
  				public void reportCurrentTask(String task_description) {
  				}
  			});
			}
			

		} catch (Throwable t) {
		}
		
		// Load Tracker View on first host of file
		TRHost trackerHost = AzureusCoreFactory.getSingleton().getTrackerHost();
		trackerHost.addListener(new TRHostListener() {
			boolean done = false;
			
			public void torrentRemoved(TRHostTorrent t) {
			}
			
			public void torrentChanged(TRHostTorrent t) {
			}
			
			public void torrentAdded(TRHostTorrent t) {
				if (done) {
					return;
				}
				TRHost trackerHost = AzureusCoreFactory.getSingleton().getTrackerHost();
				trackerHost.removeListener(this);
				done = true;
				mdi.loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_MY_TRACKER,
						false);
			}
			
			public boolean handleExternalRequest(InetSocketAddress client_address,
					String user, String url, URL absolute_url, String header, InputStream is,
					OutputStream os, AsyncController async)
							throws IOException {
				return false;
			}
		});

		UIManager uim = pi.getUIManager();
		if (uim != null) {
			MenuItem menuItem = uim.getMenuManager().addMenuItem(
					MenuManager.MENU_MENUBAR, "tags.view.heading");
			menuItem.addListener(new MenuItemListener() {
				public void selected(MenuItem menu, Object target) {
					UIFunctionsManager.getUIFunctions().getMDI().showEntryByID(
							MultipleDocumentInterface.SIDEBAR_SECTION_TAGS);
				}
			});

			menuItem = uim.getMenuManager().addMenuItem(
					MenuManager.MENU_MENUBAR, "tag.discovery.view.heading");
			menuItem.addListener(new MenuItemListener() {
				public void selected(MenuItem menu, Object target) {
					UIFunctionsManager.getUIFunctions().getMDI().showEntryByID(
							MultipleDocumentInterface.SIDEBAR_SECTION_TAG_DISCOVERY);
				}
			});

			menuItem = uim.getMenuManager().addMenuItem(
					MenuManager.MENU_MENUBAR, "chats.view.heading");
			menuItem.addListener(new MenuItemListener() {
				public void selected(MenuItem menu, Object target) {
					UIFunctionsManager.getUIFunctions().getMDI().showEntryByID(
							MultipleDocumentInterface.SIDEBAR_SECTION_CHAT);
				}
			});
			
			menuItem = uim.getMenuManager().addMenuItem(
					MenuManager.MENU_MENUBAR, "archivedlsview.view.heading");
			menuItem.addListener(new MenuItemListener() {
				public void selected(MenuItem menu, Object target) {
					UIFunctionsManager.getUIFunctions().getMDI().showEntryByID(
							MultipleDocumentInterface.SIDEBAR_SECTION_ARCHIVED_DOWNLOADS );
				}
			});
		}

		//		System.out.println("Activate sidebar " + startTab + " took "
		//				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		//		startTime = SystemTime.getCurrentTime();
	}

	private static void setupSidebarClassic(final MultipleDocumentInterfaceSWT mdi) {
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY,
				new MdiEntryCreationListener() {

					public MdiEntry createMDiEntry(String id) {
						boolean uiClassic = COConfigurationManager.getStringParameter("ui").equals(
								"az2");
						String title = uiClassic ? "{MyTorrentsView.mytorrents}"
								: ("{sidebar."
										+ MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY + "}");
						MdiEntry entry = mdi.createEntryFromSkinRef(null,
								MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY, "library",
								title, null, null, false,
								MultipleDocumentInterface.SIDEBAR_POS_FIRST);
						entry.setImageLeftID("image.sidebar.library");
						return entry;
					}
				});
		
		mdi.registerEntry("Tag\\..*", new MdiEntryCreationListener2() {
			
			public MdiEntry createMDiEntry(MultipleDocumentInterface mdi, String id,
					Object datasource, Map<?, ?> params) {
				// TODO: auto-open entries will have null datasource and id of:
				//"Tag." + tag.getTagType().getTagType() + "." + tag.getTagID();
				
				if (datasource instanceof Tag) {
					Tag tag = (Tag) datasource;
					
					return SB_Transfers.setupTag(tag);
				}
				
				return null;
			}
		});

		mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY);
	}

	private static void setupSidebarVuzeUI(final MultipleDocumentInterfaceSWT mdi) {
		MdiEntry entry;

		String[] preferredOrder = new String[] {
			MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
			MultipleDocumentInterface.SIDEBAR_HEADER_VUZE,
			MultipleDocumentInterface.SIDEBAR_HEADER_DISCOVERY,
			MultipleDocumentInterface.SIDEBAR_HEADER_DEVICES,
			MultipleDocumentInterface.SIDEBAR_HEADER_DVD,
			MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS,
		};
		mdi.setPreferredOrder(preferredOrder);

		for (int i = 0; i < preferredOrder.length; i++) {
			String id = preferredOrder[i];
			mdi.registerEntry(id, new MdiEntryCreationListener() {
				public MdiEntry createMDiEntry(String id) {
					MdiEntry entry = mdi.createHeader(id, "sidebar." + id, null);
					
					if ( entry == null ){
						
						return( null );
					}
					
					entry.setDefaultExpanded(true);

					if (id.equals(MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS)) {
						entry.addListener(new MdiChildCloseListener() {
							public void mdiChildEntryClosed(MdiEntry parent, MdiEntry child,
									boolean user) {
								if (mdi.getChildrenOf(parent.getId()).size() == 0) {
									parent.close(true);
								}
							}
						});

						PluginInterface pi = PluginInitializer.getDefaultInterface();
						UIManager uim = pi.getUIManager();
						MenuManager menuManager = uim.getMenuManager();
						MenuItem menuItem;

						menuItem = menuManager.addMenuItem("sidebar."
								+ MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS,
								"label.plugin.options");

						menuItem.addListener(new MenuItemListener() {
							public void selected(MenuItem menu, Object target) {
								UIFunctions uif = UIFunctionsManager.getUIFunctions();

								if (uif != null) {
									uif.getMDI().showEntryByID(
											MultipleDocumentInterface.SIDEBAR_SECTION_CONFIG,
											"plugins");
								}
							}
						});
					}

					return entry;
				}
			});
		}

		/*
		ContentNetworkManager cnm = ContentNetworkManagerFactory.getSingleton();
		if (cnm != null) {
			ContentNetwork[] contentNetworks = cnm.getContentNetworks();
			for (ContentNetwork cn : contentNetworks) {
				if (cn == null) {
					continue;
				}
				if (cn.getID() == ConstantsVuze.getDefaultContentNetwork().getID()) {
					cn.setPersistentProperty(ContentNetwork.PP_ACTIVE, Boolean.TRUE);
					continue;
				}

				Object oIsActive = cn.getPersistentProperty(ContentNetwork.PP_ACTIVE);
				boolean isActive = (oIsActive instanceof Boolean)
						? ((Boolean) oIsActive).booleanValue() : false;
				if (isActive) {
					mdi.createContentNetworkSideBarEntry(cn);
				}
			}
		}
		*/

		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_ABOUTPLUGINS,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromSkinRef(
								MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS,
								MultipleDocumentInterface.SIDEBAR_SECTION_ABOUTPLUGINS,
								"main.generic.browse", "{mdi.entry.about.plugins}", null, null,
								true, MultipleDocumentInterface.SIDEBAR_POS_FIRST);
						String url = ConstantsVuze.getDefaultContentNetwork().getSiteRelativeURL(
								"plugins", true);
						entry.setDatasource(url);
						entry.setImageLeftID("image.sidebar.plugin");
						return entry;
					}
				});
		//loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_ABOUTPLUGINS, true, false);

		// building plugin views needs UISWTInstance, which needs core.
		final int burnInfoShown = COConfigurationManager.getIntParameter(
				"burninfo.shown", 0);
		if (burnInfoShown == 0) {
			AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
				public void azureusCoreRunning(AzureusCore core) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							if (FeatureManagerUI.enabled) {
								// blah, can't add until plugin initialization is done

								mdi.loadEntryByID(
										MultipleDocumentInterface.SIDEBAR_SECTION_PLUS, false);

								if (!FeatureUtils.hasFullBurn()) {
									mdi.loadEntryByID(
											MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO,
											false);
								}

								COConfigurationManager.setParameter("burninfo.shown",
										burnInfoShown + 1);
							}
						}
					});
				}
			});
		}

		SB_Transfers.setup(mdi);
		new SB_Vuze(mdi);
		new SB_Discovery(mdi);

		mdi.loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY, false);
		mdi.loadEntryByID(
				MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY_UNOPENED, false);
		mdi.loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_SUBSCRIPTIONS,
				false);
		mdi.loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_DEVICES, false);
		mdi.loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_ACTIVITIES, false);
	}

	protected static MdiEntry createTorrentDetailEntry(
			MultipleDocumentInterface mdi, String id, Object ds) {
		if (ds == null) {
			return null;
		}
		final MdiEntry torrentDetailEntry = mdi.createEntryFromSkinRef(
				SideBar.SIDEBAR_HEADER_TRANSFERS, id, "torrentdetails", "", null, ds,
				true, null);

		final ViewTitleInfo viewTitleInfo = new ViewTitleInfo() {

			public Object getTitleInfoProperty(int propertyID) {
				Object ds = ((BaseMdiEntry) torrentDetailEntry).getDatasourceCore();
				if (propertyID == TITLE_EXPORTABLE_DATASOURCE) {
					return DataSourceUtils.getHash(ds);
				} else if (propertyID == TITLE_LOGID) {
					return "DMDetails";
				} else if (propertyID == TITLE_IMAGEID) {
					return "image.sidebar.details";
				}

				DownloadManager manager = DataSourceUtils.getDM(ds);
				if (manager == null) {
					return null;
				}

				if (propertyID == TITLE_TEXT) {
					if (Utils.isAZ2UI()) {
						int completed = manager.getStats().getPercentDoneExcludingDND();
						return DisplayFormatters.formatPercentFromThousands(completed)
								+ " : " + manager.getDisplayName();
					}

					return manager.getDisplayName();
				}

				if (propertyID == TITLE_INDICATOR_TEXT && !Utils.isAZ2UI()) {
					int completed = manager.getStats().getPercentDoneExcludingDND();
					if (completed != 1000) {
						return (completed / 10) + "%";
					}
				} else if (propertyID == TITLE_INDICATOR_TEXT_TOOLTIP) {
					String s = "";
					int completed = manager.getStats().getPercentDoneExcludingDND();
					if (completed != 1000) {
						s = (completed / 10) + "% Complete\n";
					}
					String eta = DisplayFormatters.formatETA(manager.getStats().getSmoothedETA());
					if (eta.length() > 0) {
						s += MessageText.getString("TableColumn.header.eta") + ": " + eta
								+ "\n";
					}

					return manager.getDisplayName() + ( s.length()==0?"":( ": " + s));
				}
				return null;
			}
		};

		if (torrentDetailEntry instanceof MdiEntrySWT) {
			((MdiEntrySWT) torrentDetailEntry).addListener(new MdiSWTMenuHackListener() {
				public void menuWillBeShown(MdiEntry entry, Menu menuTree) {
					// todo: This even work?
					TableView<?> tv = SelectedContentManager.getCurrentlySelectedTableView();
					menuTree.setData("TableView", tv);
					DownloadManager manager = DataSourceUtils.getDM(torrentDetailEntry.getDatasource());
					if (manager != null) {
						menuTree.setData("downloads", new DownloadManager[] {
							manager
						});
					}
					menuTree.setData("is_detailed_view", new Boolean(true));

					MenuFactory.buildTorrentMenu(menuTree);
				}
			});
		}

		torrentDetailEntry.addListener(new MdiEntryDatasourceListener() {
			public void mdiEntryDatasourceChanged(MdiEntry entry) {
				Object newDataSource = entry.getDatasource();
				if (newDataSource instanceof String) {
					final String s = (String) newDataSource;
					if (!AzureusCoreFactory.isCoreRunning()) {
						AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
							public void azureusCoreRunning(AzureusCore core) {
								torrentDetailEntry.setDatasource(DataSourceUtils.getDM(s));
							}
						});
						return;
					}
				}

				ViewTitleInfoManager.refreshTitleInfo(viewTitleInfo);
			}
		});
		torrentDetailEntry.setViewTitleInfo(viewTitleInfo);

		return torrentDetailEntry;
	}
}
