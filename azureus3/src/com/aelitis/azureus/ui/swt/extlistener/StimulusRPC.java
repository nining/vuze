/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.extlistener;

import java.util.Collections;
import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.donations.DonationWindow;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.BrowserMessageDispatcher;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfo;
import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfoContentNetwork;
import com.aelitis.azureus.ui.swt.browser.listener.ConfigListener;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
import com.aelitis.azureus.ui.swt.browser.listener.TorrentListener;
import com.aelitis.azureus.ui.swt.shells.main.MainWindow;
import com.aelitis.azureus.ui.swt.utils.TorrentUIUtilsV3;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;
import com.aelitis.azureus.util.*;

/**
 * @author TuxPaper
 * @created Feb 7, 2008
 *
 */
public class StimulusRPC
{
	/**
	 * Hooks some listeners
	 * @param mainWindow
	 */
	public static void hookListeners(final AzureusCore core,
			final MainWindow mainWindow) {
		/*
		 * This code block was moved here from being in-line in MainWindow
		 */
		ExternalStimulusHandler.addListener(new ExternalStimulusListener() {
			public boolean receive(String name, Map values) {
				try {
					if (values == null) {
						return false;
					}

					if (!name.equals("AZMSG")) {
						return false;
					}

					Object valueObj = values.get("value");
					if (!(valueObj instanceof String)) {
						return false;
					}

					String value = (String) valueObj;

					ClientMessageContext context = PlatformMessenger.getClientMessageContext();
					if (context == null) {
						return false;
					}
					
					// AZMSG;x;listener-id;op-id;params
					String[] splitVal = value.split(";", 5);
					if (splitVal.length != 5) {
						return false;
					}
					String lId = splitVal[2];
					String opId = splitVal[3];
					Map decodedMap = JSONUtils.decodeJSON(splitVal[4]);
					if (decodedMap == null) {
						decodedMap = Collections.EMPTY_MAP;
					}

					if (opId.equals(DisplayListener.OP_OPEN_URL)) {
						String url = MapUtils.getMapString(decodedMap, "url", null);
						if (!decodedMap.containsKey("target")) {
							context.debug("no target for url: " + url);
						} else if (UrlFilter.getInstance().urlIsBlocked(url)) {
							context.debug("url blocked: " + url);
						} else if (!UrlFilter.getInstance().urlCanRPC(url)) {
							context.debug("url not in whitelistL " + url);
						} else {
							// implicit bring to front
							final UIFunctions functions = UIFunctionsManager.getUIFunctions();
							if (functions != null) {
								functions.bringToFront();
							}

							// this is actually sync.. so we could add a completion listener
							// and return the boolean result if we wanted/needed
							BrowserMessageDispatcher dispatcher = context.getDispatcher();
							if (dispatcher != null) {
								dispatcher.dispatch(new BrowserMessage(lId, opId, decodedMap));
							} else {
								context.debug("No dispatcher for StimulusRPC" + opId);
							}

							return true;
						}

					} else if (opId.equals(TorrentListener.OP_LOAD_TORRENT)) {
						if (decodedMap.containsKey("b64")) {
							String b64 = MapUtils.getMapString(decodedMap, "b64", null);
							return TorrentListener.loadTorrentByB64(core, b64);
						} else if (decodedMap.containsKey("url")) {
							String url = MapUtils.getMapString(decodedMap, "url", null);

							boolean blocked = UrlFilter.getInstance().urlIsBlocked(url);
							// Security: Only allow torrents from whitelisted urls
							if (blocked) {
								Debug.out("stopped loading torrent URL because it's not in whitelist");
								return false;
							}

							boolean playNow = MapUtils.getMapBoolean(decodedMap, "play-now",
									false);
							boolean playPrepare = MapUtils.getMapBoolean(decodedMap,
									"play-prepare", false);
							boolean bringToFront = MapUtils.getMapBoolean(decodedMap,
									"bring-to-front", true);
							
							// Content Network of context is invalid because it's the
							// internal one used for anythin. Get network id from params instead
							long contentNetworkID = MapUtils.getMapLong(decodedMap,
									"content-network", ConstantsVuze.getDefaultContentNetwork().getID());
							ContentNetwork cn = ContentNetworkManagerFactory.getSingleton().getContentNetwork(contentNetworkID);
							if (cn == null) {
								cn = ConstantsVuze.getDefaultContentNetwork();
							}

							DownloadUrlInfo dlInfo = new DownloadUrlInfoContentNetwork(url,
									cn);
							dlInfo.setReferer(MapUtils.getMapString(decodedMap, "referer",
									null));

							TorrentUIUtilsV3.loadTorrent(dlInfo, playNow, playPrepare,
									bringToFront, false);

							return true;
						}
					} else if (opId.equals("is-ready")) {
						// The platform needs to know when it can call open-url, and it
						// determines this by the is-ready function
						return mainWindow.isReady();
					} else if (opId.equals("is-version-ge")) {
						if (decodedMap.containsKey("version")) {
							String id = MapUtils.getMapString(decodedMap, "id", "client");
							String version = MapUtils.getMapString(decodedMap, "version", "");
							if (id.equals("client")) {
								return org.gudy.azureus2.core3.util.Constants.compareVersions(
										org.gudy.azureus2.core3.util.Constants.AZUREUS_VERSION,
										version) >= 0;
							}
						}
						return false;

					} else if (opId.equals("is-active-tab")) {
						if (decodedMap.containsKey("tab")) {
							String tabID = MapUtils.getMapString(decodedMap, "tab", "");
							if (tabID.length() > 0) {
								// 3.2 TODO: Should we be checking for partial matches?
								MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
								MdiEntry entry = mdi.getCurrentEntry();
								if (entry != null) {
									return entry.getId().equals(tabID);
								}
							}
						}

						return false;

					} else if (ConfigListener.DEFAULT_LISTENER_ID.equals(lId)) {
						if (ConfigListener.OP_NEW_INSTALL.equals(opId)) {
							return COConfigurationManager.isNewInstall();
						} else if (ConfigListener.OP_CHECK_FOR_UPDATES.equals(opId)) {
							ConfigListener.checkForUpdates();
							return true;
						} else if (ConfigListener.OP_LOG_DIAGS.equals(opId)) {
							ConfigListener.logDiagnostics();
							return true;
						}
					} else if (DisplayListener.DEFAULT_LISTENER_ID.equals(lId)) {
						if (DisplayListener.OP_REFRESH_TAB.equals(opId)) {
							DisplayListener.refreshTab(MapUtils.getMapString(decodedMap, "browser-id", ""));
						} else if (DisplayListener.OP_SWITCH_TO_TAB.equals(opId)) {
							DisplayListener.switchToTab(MapUtils.getMapString(decodedMap,
									"target", ""), MapUtils.getMapString(decodedMap,
									"source-ref", null));
						}

					} else if (DisplayListener.OP_SHOW_DONATION_WINDOW.equals(lId)) {
						DonationWindow.open(true, MapUtils.getMapString(decodedMap,
								"source-ref", "SRPC"));
					}


					if (System.getProperty(
							"browser.route.all.external.stimuli.for.testing", "false").equalsIgnoreCase(
							"true")) {

						BrowserMessageDispatcher dispatcher = context.getDispatcher();
						if (dispatcher != null) {
							dispatcher.dispatch(new BrowserMessage(lId, opId, decodedMap));
						}
					} else {

						System.err.println("Unhandled external stimulus: " + value);
					}
				} catch (Exception e) {
					Debug.out(e);
				}
				return false;
			}

			public int query(String name, Map values) {
				return (Integer.MIN_VALUE);
			}
		});
	}
}
