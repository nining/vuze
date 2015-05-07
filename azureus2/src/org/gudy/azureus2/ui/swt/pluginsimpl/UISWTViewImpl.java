/*
 * File    : UISWTViewImpl.java
 * Created : Oct 14, 2005
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

package org.gudy.azureus2.ui.swt.pluginsimpl;

import java.awt.Frame;
import java.awt.Panel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.LightHashMap;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarEnablerBase;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.views.IViewAlwaysInitialize;

import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.util.MapUtils;

/**
 * This class creates an view that triggers {@link UISWTViewEventListener} 
 * appropriately
 * 
 * @author TuxPaper
 *
 */
public class UISWTViewImpl
	implements UISWTViewCore, UIPluginViewToolBarListener
{
	public static final String CFG_PREFIX = "Views.plugins.";

	private boolean delayInitializeToFirstActivate = true;

	private static final boolean DEBUG_TRIGGERS = false;

	// TODO: not protected
	protected PluginUISWTSkinObject skinObject;

	private Object initialDatasource;

	// different from parentID?
	private UISWTView parentView;

	/* Always Core */
	protected Object datasource;

	private boolean useCoreDataSource = false;

	private UISWTViewEventListener eventListener;

	// This is the same as TabbedEntry.swtItem.getControl and something in SideBarEntry
	// TODO: not protected
	protected Composite composite;

	protected final String id;

	private String title;
	
	private String titleID;

	private int iControlType = UISWTView.CONTROLTYPE_SWT;

	private Boolean hasFocus = null;

	private Map<Object, Object> user_data;

	private boolean haveSentInitialize = false;

	private boolean created = false;

	private String parentViewID;

	private boolean destroyOnDeactivate;

	private Composite masterComposite;

	private Set<UIPluginViewToolBarListener> setToolBarEnablers = new HashSet<UIPluginViewToolBarListener>(1);

	public UISWTViewImpl(String id, String parentViewID, boolean destroyOnDeactivate) {
		this.id = id;
		this.parentViewID = parentViewID;
		this.destroyOnDeactivate = destroyOnDeactivate;
		this.titleID = CFG_PREFIX + this.id + ".title";
		if (!MessageText.keyExists(titleID) && MessageText.keyExists(this.id)){
			this.titleID = id;
		}
	}

	public void setEventListener(UISWTViewEventListener _eventListener,
			boolean doCreate)
					throws UISWTViewEventCancelledException {
		this.eventListener = _eventListener;
	
		if (eventListener == null) {
			return;
		}
	
		if (_eventListener instanceof UISWTViewEventListenerHolder) {
			UISWTViewEventListenerHolder h = (UISWTViewEventListenerHolder) _eventListener;
			UISWTViewEventListener delegatedEventListener = h.getDelegatedEventListener(
					this);
			if (delegatedEventListener != null) {
				this.eventListener = delegatedEventListener;
			}
		}
	
		if (eventListener instanceof IViewAlwaysInitialize) {
			delayInitializeToFirstActivate = false;
		}
	
		if (eventListener instanceof UISWTViewCoreEventListener) {
			setUseCoreDataSource(true);
		}
	
		// >> from UISWTViewImpl
		// we could pass the parentid as the data for the create call but unfortunately
		// there's a bunch of crap out there that assumes that data is the view object :(
		if (doCreate && !triggerBooleanEvent(UISWTViewEvent.TYPE_CREATE, this)) {
			throw new UISWTViewEventCancelledException();
		}
		// <<
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.MdiEntrySWT#getEventListener()
	 */
	public UISWTViewEventListener getEventListener() {
		return eventListener;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#getInitialDataSource()
	 */
	public Object getInitialDataSource() {
		return initialDatasource;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#setDatasource(java.lang.Object)
	 */
	public void setDatasource(Object datasource) {
		if (initialDatasource == null) {
			initialDatasource = datasource;
		}
		triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, datasource);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#getDataSource()
	 */
	// XXX There's also a getDatasource().. lowercase S :(
	// It doesn't use useCoreDataSource and should be either removed or renamed
	// to getDataSourcePlugin
	public Object getDataSource() {
		return PluginCoreUtils.convert(datasource, useCoreDataSource());
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#setParentView(org.gudy.azureus2.ui.swt.plugins.UISWTView)
	 */
	public void setParentView(UISWTView parentView) {
		this.parentView = parentView;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#getParentView()
	 */
	public UISWTView getParentView() {
		return parentView;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginView#getViewID()
	 */
	// XXX Same as getID().. remove getID?
	public String getViewID() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginView#closeView()
	 */
	public void closeView() {
		try {

			// In theory mdi.closeEntry will dispose of the swtItem (ctabitem or other)
			// via #close(false).  Not sure what happens to composite though,
			// so I don't know fi that TYPE_DESTROY actually gets called
			// The CTabItem scan seems pointless now, though

			Composite c = getComposite();

			if (c != null && !c.isDisposed()) {

				Composite parent = c.getParent();

				triggerEvent(UISWTViewEvent.TYPE_DESTROY, null);

				if (parent instanceof CTabFolder) {

					for (CTabItem item : ((CTabFolder) parent).getItems()) {

						if (item.getControl() == c) {

							item.dispose();
						}
					}
				}
			}
		} catch (Throwable e) {
			Debug.out(e);
		}

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#setControlType(int)
	 */
	public void setControlType(int iControlType) {
		if (iControlType == CONTROLTYPE_AWT || iControlType == CONTROLTYPE_SWT
				|| iControlType == CONTROLTYPE_SKINOBJECT) {
			this.iControlType = iControlType;
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#getControlType()
	 */
	public int getControlType() {
		return iControlType;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#triggerEvent(int, java.lang.Object)
	 */
	public void triggerEvent(int eventType, Object data) {
		try {
			triggerBooleanEvent(eventType, data);
		} catch (Exception e) {
			// TODO: Better error
			Debug.out(e);
		}
	}
	
	private static String padRight(String s, int n) {
    return String.format("%1$-" + n + "s", s);  
	}

	private boolean triggerBooleanEvent(int eventType, Object data) {
		if (DEBUG_TRIGGERS) {
			if (eventListener == null || eventType != UISWTViewEvent.TYPE_REFRESH) {
				System.out.println(System.currentTimeMillis() + "." + padRight(id, 20)
						+ "] " + "trigger "
						+ padRight((eventType < UISWTViewEvent.DEBUG_TYPES.length
								? UISWTViewEvent.DEBUG_TYPES[eventType] : "" + eventType), 6)
						+ ", " + (eventListener == null ? "null" : "nonn") + ";data="
						+ (data instanceof Object[] ? Arrays.toString((Object[]) data)
								: data)
						+ "/ds="
						+ (datasource instanceof Object[]
								? Arrays.toString((Object[]) datasource) : datasource)
						+ ";" + title + ";" + Debug.getCompressedStackTrace());
			}
		}
		if (eventListener == null
				&& eventType != UISWTViewEvent.TYPE_DATASOURCE_CHANGED) {
			return false;
		}
	
		if (eventType == UISWTViewEvent.TYPE_INITIALIZE) {
			if (haveSentInitialize) {
				if (DEBUG_TRIGGERS) {
					System.out.println("  -> already haveSentInitialize");
				}
				return false;
			}
			if (!created) {
				// create will set DS changed
				triggerBooleanEvent(UISWTViewEvent.TYPE_CREATE, this);
			} else if (datasource != null) {
				triggerBooleanEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, datasource);
			}
			haveSentInitialize = true;
		}
	
		if (eventType == UISWTViewEvent.TYPE_CREATE) {
			created = true;
		}
	
		if (delayInitializeToFirstActivate
				&& eventType == UISWTViewEvent.TYPE_FOCUSGAINED
				&& !haveSentInitialize) {
			swt_triggerInitialize();
		}
		// prevent double fire of focus gained/lost
		if (eventType == UISWTViewEvent.TYPE_FOCUSGAINED && hasFocus != null
				&& hasFocus) {
			if (DEBUG_TRIGGERS) {
				System.out.println("  -> already hasFocus");
			}
			return true;
		}
		if (eventType == UISWTViewEvent.TYPE_FOCUSLOST && hasFocus != null
				&& !hasFocus) {
			if (DEBUG_TRIGGERS) {
				System.out.println("  -> already !hasFocus");
			}
			return true;
		}
	
		if (eventType == UISWTViewEvent.TYPE_DATASOURCE_CHANGED) {
			Object newDataSource = PluginCoreUtils.convert(data, true);
			if (datasource == newDataSource) {
				if (DEBUG_TRIGGERS) {
					System.out.println("  -> same DS, skip");
				}
				return true;
			}
			if (newDataSource instanceof Object[] && datasource instanceof Object[]) {
				if (Arrays.equals((Object[]) newDataSource, (Object[]) datasource)) {
					if (DEBUG_TRIGGERS) {
						System.out.println("  -> same DS[], skip");
					}
					return true;
				}
			}
			datasource = newDataSource;
			data = PluginCoreUtils.convert(datasource, useCoreDataSource);
			if (initialDatasource == null) {
				initialDatasource = datasource;
			}
			if (eventListener == null) {
				return true;
			}
			// TODO: What about triggering skinObject's EVENT_DATASOURCE_CHANGED?
		} else if (eventType == UISWTViewEvent.TYPE_LANGUAGEUPDATE) {
			//lastFullTitle = "";
			System.out.println(eventListener.getClass().getSimpleName());
			if (eventListener.getClass().getSimpleName().startsWith("SBC_My")) {
				System.out.println("STOP");
			}
			Messages.updateLanguageForControl(getComposite());
		} else if (eventType == UISWTViewEvent.TYPE_OBFUSCATE
				&& (eventListener instanceof ObfusticateImage)) {
			if (data instanceof Map) {
				((ObfusticateImage) eventListener).obfusticatedImage(
						(Image) MapUtils.getMapObject((Map<?, ?>) data, "image", null,
								Image.class));
			}
		} else if (eventType == UISWTViewEvent.TYPE_FOCUSGAINED) {
			hasFocus = true;
			if (!haveSentInitialize) {
				swt_triggerInitialize();
			}
		} else if (eventType == UISWTViewEvent.TYPE_FOCUSLOST) {
			hasFocus = false;
			if (isDestroyOnDeactivate()) {
				triggerEvent(UISWTViewEvent.TYPE_DESTROY, null);
			}
		} else if (eventType == UISWTViewEvent.TYPE_DESTROY) {
			if (hasFocus != null && hasFocus) {
				triggerEvent(UISWTViewEvent.TYPE_FOCUSLOST, null);
			}
			// focus lost may have destroyed us already
			if (!created && !haveSentInitialize && getComposite() == null) {
				return true;
			}
		}
	
		boolean result = false;
		try {
			result = eventListener.eventOccurred(
					new UISWTViewEventImpl(parentViewID, this, eventType, data));
		} catch (Throwable t) {
			Debug.out("ViewID=" + id + "; EventID="
					+ UISWTViewEvent.DEBUG_TYPES[eventType] + "; data=" + data, t);
			//throw (new UIRuntimeException("UISWTView.triggerEvent:: ViewID="
			//		+ sViewID + "; EventID=" + eventType + "; data=" + data, t));
		}
	
		if (eventType == UISWTViewEvent.TYPE_DESTROY) {
			if (masterComposite != null && !masterComposite.isDisposed()) {
				Composite parent = masterComposite.getParent();
				Utils.disposeComposite(masterComposite);
				Utils.relayoutUp(parent);
			}
			masterComposite = null;
			composite = null;
			haveSentInitialize = false;
			hasFocus = false;
			created = false;
			initialDatasource = datasource;
			datasource = null;
		} else if (eventType == UISWTViewEvent.TYPE_CREATE) {
			if (eventListener instanceof UISWTViewEventListenerHolder) {
				UISWTViewEventListenerHolder h = (UISWTViewEventListenerHolder) eventListener;
				UISWTViewEventListener delegatedEventListener = h.getDelegatedEventListener(
						this);
				if (delegatedEventListener != null) {
					try {
						setEventListener(delegatedEventListener, false);
					} catch (UISWTViewEventCancelledException e) {
					}
				}
			}

			if (DEBUG_TRIGGERS) {
				System.out.println(" -> raw DS Change");
			}
			triggerEventRaw(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, PluginCoreUtils.convert(datasource, useCoreDataSource));
		}
	
		return result;
	}

	protected boolean triggerEventRaw(int eventType, Object data) {
		if (eventListener == null) {
			System.err.println(
					"null eventListener for " + UISWTViewEvent.DEBUG_TYPES[eventType] + " " + Debug.getCompressedStackTrace());
			return eventType == UISWTViewEvent.TYPE_CLOSE ? true : false;
		}
		try {
			return eventListener.eventOccurred(
					new UISWTViewEventImpl(parentViewID, this, eventType, data));
		} catch (Throwable t) {
			throw (new UIRuntimeException("UISWTView.triggerEvent:: ViewID=" + id
					+ "; EventID=" + eventType + "; data=" + data, t));
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		if (title == null) {
			return;
		}
		if (title.startsWith("{") && title.endsWith("}") && title.length() > 2) {
			setTitleID(title.substring(1, title.length() - 1));
			return;
		}
		if (title.equals(this.title)) {
			return;
		}
		if (title.contains(".") && MessageText.keyExists(title)) {
			setTitleID(title);
			return;
		}
	
		this.title = title;
		this.titleID = null;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#setTitleID(java.lang.String)
	 */
	public void setTitleID(String titleID) {
		if (titleID != null
				&& (MessageText.keyExists(titleID) || titleID.startsWith("!"))) {
			this.titleID = titleID;
			this.title = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#getPluginInterface()
	 */
	public PluginInterface getPluginInterface() {
		if (eventListener instanceof UISWTViewEventListenerHolder) {
			return (((UISWTViewEventListenerHolder) eventListener).getPluginInterface());
		}
	
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getComposite()
	 */
	public Composite getComposite() {
		return composite;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getTitleID()
	 */
	// XXX Might not be needed once StatsView, SBC_TDV, and TVSWT_TC are converted
	public String getTitleID() {
		if (title == null) {
			// still need this crappy check because some plugins still expect their
			// view id to be their name
			if (MessageText.keyExists(id)) {
				return id;
			}
			String id = CFG_PREFIX + this.id + ".title";
			if (MessageText.keyExists(id)) {
				return id;
			}
			return "!" + id + "!";
		}
		return "!" + title + "!";
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getFullTitle()
	 */
	public String getFullTitle() {
		if (titleID != null) {
			return MessageText.getString(titleID);
		}
		return title;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#initialize(org.eclipse.swt.widgets.Composite)
	 */
	public void initialize(Composite parent) {
		this.masterComposite = parent; 
		if (iControlType == UISWTView.CONTROLTYPE_SWT) {
			GridData gridData;
			Layout parentLayout = parent.getLayout();
			if (parentLayout instanceof FormLayout) {
				composite = parent;
			} else {
				composite = new Composite(parent, SWT.NONE);
				GridLayout layout = new GridLayout(1, false);
				layout.marginHeight = 0;
				layout.marginWidth = 0;
				composite.setLayout(layout);
				gridData = new GridData(GridData.FILL_BOTH);
				composite.setLayoutData(gridData);
			}
			
			Listener showListener = new Listener() {
				public void handleEvent(Event event) {
					if (composite == null || composite.isDisposed()) {
						return;
					}
					Composite parent = composite.getParent();
					if (parent instanceof CTabFolder) {
						CTabFolder tabFolder = (CTabFolder) parent;
						Control selectedControl = tabFolder.getSelection().getControl();
						if (selectedControl != composite) {
							return;
						}
					} else if (parent instanceof TabFolder) {
						TabFolder tabFolder = (TabFolder) parent;
						TabItem[] selectedControl = tabFolder.getSelection();
						if (selectedControl != null && selectedControl.length == 1
								&& selectedControl[0].getControl() != composite) {
							return;
						}
					}
					// Delay trigger of FOCUSGAINED a bit, so that parent is visible
					Utils.execSWTThreadLater(0, new AERunnable() {
						@Override
						public void runSupport() {
							triggerEvent(UISWTViewEvent.TYPE_FOCUSGAINED, null);
						}
					});
				}
			};
	
			composite.addListener(SWT.Show, showListener);
			if (parent != composite) {
				parent.addListener(SWT.Show, showListener);
			}
			if (composite.isVisible()) {
				boolean focusGained = true;
				if (parent instanceof CTabFolder || (parent instanceof TabFolder)) {
					// can't be gaining the focus yet.. we just created it and
					// it hasn't been assigned to TabFolder yet
					focusGained = false;
				}
				if (focusGained) {
					triggerEvent(UISWTViewEvent.TYPE_FOCUSGAINED, null);
				}
			}
			if (delayInitializeToFirstActivate) {
				return;
			}
			swt_triggerInitialize();
		} else if (iControlType == UISWTView.CONTROLTYPE_AWT) {
			composite = new Composite(parent, SWT.EMBEDDED);
			FillLayout layout = new FillLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			composite.setLayout(layout);
			GridData gridData = new GridData(GridData.FILL_BOTH);
			composite.setLayoutData(gridData);
	
			Frame f = SWT_AWT.new_Frame(composite);
	
			Panel pan = new Panel();
	
			f.add(pan);
	
			triggerEvent(UISWTViewEvent.TYPE_INITIALIZE, pan);
		} else if (iControlType == UISWTViewCore.CONTROLTYPE_SKINOBJECT) {
			triggerEvent(UISWTViewEvent.TYPE_INITIALIZE, getPluginSkinObject());
		}
	}

	private void swt_triggerInitialize() {
		if (haveSentInitialize) {
			return;
		}
	
		if (!created) {
			triggerBooleanEvent(UISWTViewEvent.TYPE_CREATE, this);
		}
	
		composite.setRedraw(false);
		composite.setLayoutDeferred(true);
		triggerEvent(UISWTViewEvent.TYPE_INITIALIZE, composite);
	
		if (composite.getLayout() instanceof GridLayout) {
			// Force children to have GridData layoutdata.
			Control[] children = composite.getChildren();
			for (int i = 0; i < children.length; i++) {
				Control control = children[i];
				Object layoutData = control.getLayoutData();
				if (layoutData == null || !(layoutData instanceof GridData)) {
					if (layoutData != null) {
						Logger.log(
								new LogEvent(LogIDs.PLUGIN, LogEvent.LT_WARNING,
										"Plugin View '" + id + "' tried to setLayoutData of "
												+ control + " to a "
												+ layoutData.getClass().getName()));
					}
	
					GridData gridData;
					if (children.length == 1) {
						gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
					} else {
						gridData = new GridData();
					}
	
					control.setLayoutData(gridData);
				}
			}
		}
		composite.layout();
		composite.setLayoutDeferred(false);
		composite.setRedraw(true);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#useCoreDataSource()
	 */
	public boolean useCoreDataSource() {
		return useCoreDataSource;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#setUseCoreDataSource(boolean)
	 */
	public void setUseCoreDataSource(boolean useCoreDataSource) {
		if (this.useCoreDataSource == useCoreDataSource) {
			return;
		}
	
		this.useCoreDataSource = useCoreDataSource;
		if (datasource != null) {
			setDatasource(datasource);
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getSkinObject()
	 */
	public PluginUISWTSkinObject getPluginSkinObject() {
		return skinObject;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#setSkinObject(org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject, org.eclipse.swt.widgets.Composite)
	 */
	// TODO: Combine this with the other setSkinObject..
	public void setPluginSkinObject(PluginUISWTSkinObject so) {
		this.skinObject = so;
	}

	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		UIToolBarEnablerBase[] toolbarEnablers = getToolbarEnablers();
		for (UIToolBarEnablerBase tbEnablerBase : toolbarEnablers) {
			if (tbEnablerBase instanceof UIPluginViewToolBarListener) {
				UIPluginViewToolBarListener tbEnabler = (UIPluginViewToolBarListener) tbEnablerBase;
				if (tbEnabler.toolBarItemActivated(item, activationType, datasource)) {
					return true;
				}
			}
		}
		return false;
	}

	public void refreshToolBarItems(Map<String, Long> list) {
		UIToolBarEnablerBase[] toolbarEnablers = getToolbarEnablers();
		for (UIToolBarEnablerBase tbEnablerBase : toolbarEnablers) {
			if (tbEnablerBase instanceof UIPluginViewToolBarListener) {
				UIPluginViewToolBarListener tbEnabler = (UIPluginViewToolBarListener) tbEnablerBase;
				tbEnabler.refreshToolBarItems(list);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginView#setToolBarListener(org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener)
	 */
	public void setToolBarListener(UIPluginViewToolBarListener l) {
		addToolbarEnabler(l);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginView#getToolBarListener()
	 */
	public UIPluginViewToolBarListener getToolBarListener() {
		return setToolBarEnablers.size() == 0 ? null : setToolBarEnablers.iterator().next();
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#getToolbarEnablers()
	 */
	public UIToolBarEnablerBase[] getToolbarEnablers() {
		// XXX What if eventListener is of UIPluginViewToolBarListener (as per UISWTViewImpl's check)
		return setToolBarEnablers.toArray(new UIToolBarEnablerBase[0]);
	}
	
	public boolean hasToolbarEnableers() {
		return setToolBarEnablers.size() > 0;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#addToolbarEnabler(org.gudy.azureus2.plugins.ui.toolbar.UIToolBarEnablerBase)
	 */
	public void addToolbarEnabler(UIToolBarEnablerBase enabler) {
		if (setToolBarEnablers.contains(enabler)) {
			return;
		}
		setToolBarEnablers.add((UIPluginViewToolBarListener) enabler);
		setToolbarVisibility(setToolBarEnablers.size() > 0);
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#removeToolbarEnabler(org.gudy.azureus2.plugins.ui.toolbar.UIToolBarEnablerBase)
	 */
	public void removeToolbarEnabler(UIToolBarEnablerBase enabler) {
		setToolBarEnablers.remove(enabler);
		setToolbarVisibility(setToolBarEnablers.size() > 0);
	}

	protected void setToolbarVisibility(boolean visible) {
	}

	public void setUserData(Object key, Object data) {
		synchronized (this) {
	
			if (user_data == null) {
	
				user_data = new LightHashMap<Object, Object>();
			}
	
			if (data == null) {
	
				user_data.remove(key);
	
				if (user_data.isEmpty()) {
	
					user_data = null;
				}
			} else {
	
				user_data.put(key, data);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getUserData(java.lang.Object)
	 */
	public Object getUserData(Object key) {
		synchronized (this) {
	
			if (user_data == null) {
	
				return (null);
			}
	
			return (user_data.get(key));
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#setDestroyOnDeactivate(boolean)
	 */
	public void setDestroyOnDeactivate(boolean b) {
		destroyOnDeactivate = b;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#isDestroyOnDeactivate()
	 */
	public boolean isDestroyOnDeactivate() {
		return destroyOnDeactivate;
	}

	public boolean isDelayInitializeToFirstActivate() {
		return delayInitializeToFirstActivate;
	}

	public void setDelayInitializeToFirstActivate(
			boolean delayInitializeToFirstActivate) {
		this.delayInitializeToFirstActivate = delayInitializeToFirstActivate;
	}

}
