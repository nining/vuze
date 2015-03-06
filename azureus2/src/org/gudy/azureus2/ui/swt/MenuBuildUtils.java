/*
 * Created on 25-Jan-2007
 * Created by Allan Crooks
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
package org.gudy.azureus2.ui.swt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.GraphicURI;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.pluginsimpl.local.ui.menus.MenuItemImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.FormattersImpl;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.plugins.I2PHelpers;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginUtils;


/**
 * A class which helps generate build SWT menus.
 * 
 * It provides two main functions. The first provides the ability to create
 * regenerateable menus (it will dispose old items when not displayed and
 * invoke a callback method to regenerate it).
 * 
 * The second provides the ability to create SWT menus based on the plugin
 * API for menu creation - this allows internal code that generates menus
 * to include plugins to append to their own internal menus. 
 * 
 * @author Allan Crooks
 */
public class MenuBuildUtils {

	/**
	 * An interface to be used for addMaintenanceListenerForMenu. 
	 */
	public static interface MenuBuilder {
		public void buildMenu(Menu root_menu, MenuEvent menuEvent);
	}

	/**
	 * Creates and adds a listener object to implement regeneratable menus.
	 * 
	 * The first piece of functionality it offers is the ability to call a
	 * callback method to generate the menu when it is about to be displayed
	 * (the callback method is done by passing an object implementing the
	 * MenuBuilder interface).
	 * 
	 * This means that the caller of this method only needs to provide the
	 * logic to construct the menu's contents. This is helpful if you want
	 * to update an existing menu.
	 * 
	 * The second piece of functionality is that it automatically does what
	 * is required to dispose of existing menu items when the menu is hidden.
	 */
	public static void addMaintenanceListenerForMenu(final Menu menu,
			final MenuBuilder builder) {

		if (Constants.isLinux) { // Hack for Ubuntu Unity -- Show not called when no items
			new org.eclipse.swt.widgets.MenuItem(menu, SWT.SEPARATOR);
		}
		// Was taken from TableView.java
		menu.addMenuListener(new MenuListener() {
			boolean bShown = false;

			public void menuHidden(MenuEvent e) {
				bShown = false;

				if (Constants.isOSX)
					return;

				// Must dispose in an asyncExec, otherwise SWT.Selection doesn't
				// get fired (async workaround provided by Eclipse Bug #87678)
				e.widget.getDisplay().asyncExec(new AERunnable() {
					public void runSupport() {
						if (bShown || menu.isDisposed())
							return;
						org.eclipse.swt.widgets.MenuItem[] items = menu
								.getItems();
						for (int i = 0; i < items.length; i++) {
							items[i].dispose();
						}
						if (Constants.isLinux) { // Hack for Ubuntu Unity -- Show not called when no items
							new org.eclipse.swt.widgets.MenuItem(menu, SWT.SEPARATOR);
						}
					}
				});
			};

			public void menuShown(MenuEvent e) {
				try{
					org.eclipse.swt.widgets.MenuItem[] items = menu.getItems();
					for (int i = 0; i < items.length; i++){
						items[i].dispose();
					}
				}catch( Throwable f ){
					// getting java.lang.NegativeArraySizeException sometimes on linux. ignore
				}
				
				bShown = true;
				builder.buildMenu(menu, e);
				if (Constants.isLinux) { // Hack for Ubuntu Unity -- Show not called when no items
					if (menu.getItemCount() == 0) {
						new org.eclipse.swt.widgets.MenuItem(menu, SWT.SEPARATOR);
					}
				}
			}
		});
	}

	/**
	 * This is an interface used by addPluginMenuItems.
	 */
	public static interface PluginMenuController {
		
		/**
		 * This method should create a listener object which should be invoked
		 * when the given menu item is selected.
		 */
		public Listener makeSelectionListener(MenuItem plugin_menu_item);
		
		/**
		 * This method will be invoked just before the given menu item is
		 * displayed in a menu.
		 */
		public void notifyFillListeners(MenuItem menu_item);
		
		public void buildSubmenu(MenuItem parent);
	}

	/**
	 * This is an implementation of the PluginMenuController interface for use with
	 * MenuItemImpl classes - note that this is not intended for use by subclasses of
	 * MenuItemImpl (like TableContextMenuItemImpl).
	 * 
	 * The object passed at construction time is the object to be passed when selection
	 * listeners and fill listeners are notified.
	 */
	public static class MenuItemPluginMenuControllerImpl implements
			PluginMenuController {

		private Object[] objects;

		public MenuItemPluginMenuControllerImpl(Object[] o) {
			this.objects = o;
		}

		public Listener makeSelectionListener(MenuItem menu_item) {
			final MenuItemImpl mii = (MenuItemImpl) menu_item;
			return new Listener() {
				public void handleEvent(Event e) {
					mii.invokeListenersMulti(objects);
				}
			};
		}

		public void notifyFillListeners(MenuItem menu_item) {
			((MenuItemImpl) menu_item).invokeMenuWillBeShownListeners(objects);
		}

		// @see org.gudy.azureus2.ui.swt.MenuBuildUtils.PluginMenuController#buildSubmenu(org.gudy.azureus2.plugins.ui.menus.MenuItem)
		public void buildSubmenu(MenuItem parent) {
			org.gudy.azureus2.plugins.ui.menus.MenuBuilder submenuBuilder = ((MenuItemImpl) parent).getSubmenuBuilder();
			if (submenuBuilder != null) {
				try {
					parent.removeAllChildItems();
					submenuBuilder.buildSubmenu(parent, objects);
				} catch (Throwable t) {
					Debug.out(t);
				}
			}
		}
	}

	/**
	 * An instance of MenuItemPluginMenuControllerImpl with a default value of
	 * null - this will be the value passed when notifying selection and fill
	 * listeners.
	 */
	public static final PluginMenuController BASIC_MENU_ITEM_CONTROLLER = new MenuItemPluginMenuControllerImpl(null);

	/**
	 * Creates menu items inside the given menu based on the plugin API MenuItem
	 * instances provided. This method is provided mainly as a utility method to
	 * make it easier for menus to incorporate menu components specified by
	 * plugins.
	 * 
	 * Usually, the list of array items will be extracted from a class like
	 * MenuItemManager or TableContextMenuManager, where plugins will usually
	 * register menu items they have created.
	 * 
	 * @param items The list of plugin MenuItem to add
	 * @param parent The SWT Menu to add to.
	 * @param prev_was_separator Indicates if the previous item in the menu is
	 *            a separator or not
	 * @param enable_items Indicates whether you want generated items to be
	 *            enabled or not. If false, all items will be disabled. If true,
	 *            then the items *may* be enabled, depending on how each MenuItem
	 *            is configured.  
	 * @param controller The callback object used by this method when creating the
	 *            SWT menus (used for invoking fill and selection listeners).
	 */
	public static void addPluginMenuItems(MenuItem[] items, Menu parent,
			boolean prev_was_separator,
			final boolean enable_items, final PluginMenuController controller) {
		
		for (int i = 0; i < items.length; i++) {
			final MenuItemImpl az_menuitem = (MenuItemImpl) items[i];
			
			controller.notifyFillListeners(az_menuitem);
			if (!az_menuitem.isVisible()) {continue;}
			
			final int style = az_menuitem.getStyle();
			final int swt_style;

			boolean this_is_separator = false;

			// Do we have any children? If so, we override any manually defined
			// style.
			boolean is_container = false;
			

			if (style == TableContextMenuItem.STYLE_MENU) {
				swt_style = SWT.CASCADE;
				is_container = true;
			} else if (style == TableContextMenuItem.STYLE_PUSH) {
				swt_style = SWT.PUSH;
			} else if (style == TableContextMenuItem.STYLE_CHECK) {
				swt_style = SWT.CHECK;
			} else if (style == TableContextMenuItem.STYLE_RADIO) {
				swt_style = SWT.RADIO;
			} else if (style == TableContextMenuItem.STYLE_SEPARATOR) {
				this_is_separator = true;
				swt_style = SWT.SEPARATOR;
			} else {
				swt_style = SWT.PUSH;
			}

			if (prev_was_separator && this_is_separator) {continue;} // Skip contiguous separators
			if (this_is_separator && i == items.length - 1) {continue;} // Skip trailing separator

			prev_was_separator = this_is_separator;

			final org.eclipse.swt.widgets.MenuItem menuItem = new org.eclipse.swt.widgets.MenuItem(
					parent, swt_style);

			if (swt_style == SWT.SEPARATOR) {continue;}			

			if (enable_items) {

				if (style == TableContextMenuItem.STYLE_CHECK
						|| style == TableContextMenuItem.STYLE_RADIO) {

					Boolean selection_value = (Boolean) az_menuitem.getData();
					if (selection_value == null) {
						throw new RuntimeException(
								"MenuItem with resource name \""
										+ az_menuitem.getResourceKey()
										+ "\" needs to have a boolean value entered via setData before being used!");
					}
					menuItem.setSelection(selection_value.booleanValue());
				}
			}
			
			final Listener main_listener = controller.makeSelectionListener(az_menuitem);
			menuItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					if (az_menuitem.getStyle() == MenuItem.STYLE_CHECK
							|| az_menuitem.getStyle() == MenuItem.STYLE_RADIO) {
						if (!menuItem.isDisposed()) {
							az_menuitem.setData(new Boolean(menuItem.getSelection()));
						}
					}
					main_listener.handleEvent(e);
				}
			});
			
			if (is_container) {
				Menu this_menu = new Menu(parent);
				menuItem.setMenu(this_menu);

				addMaintenanceListenerForMenu(this_menu, new MenuBuilder() {
					public void buildMenu(Menu root_menu, MenuEvent menuEvent) {
						controller.buildSubmenu(az_menuitem);
						addPluginMenuItems(az_menuitem.getItems(), root_menu, false,
								enable_items, controller);
					}
				});
			}
			
			String custom_title = az_menuitem.getText();
			menuItem.setText(custom_title);

			Graphic g = az_menuitem.getGraphic();
			if (g instanceof UISWTGraphic) {
				Utils.setMenuItemImage(menuItem, ((UISWTGraphic) g).getImage());
			} else if (g instanceof GraphicURI) {
				Utils.setMenuItemImage(menuItem, ((GraphicURI) g).getURI().toString());
			}

			menuItem.setEnabled(enable_items && az_menuitem.isEnabled());
			
		}
	}
	
	/**
	 * 
	 * @param flat_entries		Overall list of menu entry names
	 * @param split_after		Split if more than this
	 * @return					Entries are either a String or Object[]{ submeuname, List<String> submenu entries }
	 */
	
	public static List<Object>
	splitLongMenuListIntoHierarchy(
		List<String>	flat_entries,
		int				split_after )
	{
		List<Object>	result = new ArrayList<Object>();
	
		int	flat_entry_count = flat_entries.size();

		if ( flat_entry_count == 0 ){
			
			return( result );
		}
		
		Collections.sort(
			flat_entries,
			new Comparator<String>()
			{
				final Comparator<String> comp = new FormattersImpl().getAlphanumericComparator( true );
				
				public int 
				compare(
					String o1, String o2) 
				{
					return( comp.compare( o1, o2 ));
				}
			});	
				
		int[] buckets = new int[split_after];
		
		for ( int i=0;i<flat_entry_count;i++){
			
			buckets[i%buckets.length]++;
		}
		
		List<char[]>	edges = new ArrayList<char[]>();
		
		int	pos = 0;
		
		for ( int i=0;i<buckets.length;i++){
			
			int	entries = buckets[i];
			
			edges.add( flat_entries.get( pos ).toCharArray());

			if ( entries > 1 ){
				
				edges.add( flat_entries.get( pos + entries - 1 ).toCharArray());
				
				pos += entries;
				
			}else{
								
				break;
			}
		}
			
		int[]	edge_lens = new int[edges.size()];
		
		for ( int i=0;i<edges.size()-1;i++){
			
			char[] c1 = edges.get(i);
			char[] c2 = edges.get(i+1);
			
			int	j;
			
			for ( j=0;j<Math.min(Math.min(c1.length,c2.length),5); j++ ){
				
				if ( c1[j] != c2[j]){
					
					break;
				}
			}
			
			j++;
			
			edge_lens[i] 	= Math.min( c1.length,Math.max( edge_lens[i], j )); 
			edge_lens[i+1] 	= j;
		}
		
		int	bucket_pos 	= 0;
		int	edge_pos	= 0;
		
		Iterator<String>tag_it = flat_entries.iterator();
				
		while( tag_it.hasNext()){
			
			int	bucket_entries = buckets[bucket_pos++];
							
			List<String>	bucket_tags = new ArrayList<String>();
			
			for ( int i=0;i<bucket_entries;i++){
				
				bucket_tags.add( tag_it.next());
			}
						
			if ( bucket_entries == 1 ){
				
				result.add( bucket_tags.get(0));
				
			}else{
				
				
				String level_name = new String( edges.get( edge_pos ), 0, edge_lens[ edge_pos++ ]) + " - " + new String( edges.get( edge_pos ), 0, edge_lens[ edge_pos++ ]);
				
				result.add( new Object[]{ level_name, bucket_tags });
			}
		}	
			
		return( result );
	}
	
	private static AtomicBoolean	pub_chat_pending 	= new AtomicBoolean();
	private static AtomicBoolean	anon_chat_pending 	= new AtomicBoolean();

	public static void
	addChatMenu(
		Menu			menu,
		String			menu_resource_key,
		final String	chat_key )
	{
		if ( BuddyPluginUtils.isBetaChatAvailable()){
						
			final Menu chat_menu = new Menu(menu.getShell(), SWT.DROP_DOWN);
			
			final org.eclipse.swt.widgets.MenuItem chat_item = new org.eclipse.swt.widgets.MenuItem(menu, SWT.CASCADE);
			
			Messages.setLanguageText( chat_item, menu_resource_key );
			
			chat_item.setMenu(chat_menu);

			org.eclipse.swt.widgets.MenuItem chat_pub = new org.eclipse.swt.widgets.MenuItem(chat_menu, SWT.PUSH);
			
			Messages.setLanguageText(chat_pub, "label.public");
			
			chat_pub.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event){
					
					pub_chat_pending.set( true );
					
					BuddyPluginUtils.createBetaChat(
						AENetworkClassifier.AT_PUBLIC, 
						chat_key,
						new Runnable()
						{
							public void
							run()
							{
								pub_chat_pending.set( false );
							}
						});
				}});
			
			if ( pub_chat_pending.get()){
				
				chat_pub.setEnabled( false );
				chat_pub.setText( chat_pub.getText() + " (" + MessageText.getString( "PeersView.state.pending" ) + ")" );
			}
			
			if ( BuddyPluginUtils.isBetaChatAnonAvailable()){
				
				org.eclipse.swt.widgets.MenuItem chat_priv = new org.eclipse.swt.widgets.MenuItem(chat_menu, SWT.PUSH);
				
				Messages.setLanguageText(chat_priv, "label.anon");
				
				chat_priv.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event){
						
						anon_chat_pending.set( true );
						
						BuddyPluginUtils.createBetaChat( 
							AENetworkClassifier.AT_I2P, 
							chat_key,
							new Runnable()
							{
								public void
								run()
								{
									anon_chat_pending.set( false );
								}
							});
					}});
				
				if ( anon_chat_pending.get()){
					
					chat_priv.setEnabled( false );
					chat_priv.setText( chat_priv.getText() + " (" + MessageText.getString( "PeersView.state.pending" ) + ")" );

				}
			}else{
				
				org.eclipse.swt.widgets.MenuItem chat_priv = new org.eclipse.swt.widgets.MenuItem(chat_menu, SWT.PUSH);
				
				chat_priv.setText( MessageText.getString("label.anon") + "..." );
				
				chat_priv.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event){
						
						I2PHelpers.installI2PHelper( null, null, null );
					}});
				
				if ( I2PHelpers.isInstallingI2PHelper()){
					
					chat_priv.setEnabled( false );
					chat_priv.setText( chat_priv.getText() + " (" + MessageText.getString( "PeersView.state.pending" ) + ")" );

				}
			}
		}
	}
	
	public interface
	ChatKeyResolver
	{
		public String
		getChatKey(
			Object		object );
	}
	
	public static MenuItem
	addChatMenu(
		final MenuManager		menu_manager,
		final MenuItem			chat_item,
		final ChatKeyResolver	chat_key_resolver )
	{
		chat_item.setStyle( MenuItem.STYLE_MENU );
			
		chat_item.addFillListener(
			new MenuItemFillListener() 
			{
				
				public void 
				menuWillBeShown(
					MenuItem 	menu, 
					Object 		data) 
				{
					menu.removeAllChildItems();
					
					{
						MenuItem chat_pub = menu_manager.addMenuItem(chat_item,  "label.public");
						
						chat_pub.addMultiListener(
								new MenuItemListener() {
									
									public void 
									selected(
										MenuItem 	menu, 
										Object 		target) 
									{
										Object[]	rows = (Object[])target;
										
										if ( rows.length > 0 ){
											
											final AtomicInteger count = new AtomicInteger( rows.length );
											
											pub_chat_pending.set( true );
	
											for ( Object obj: rows ){
												
												String chat_key = chat_key_resolver.getChatKey( obj );
												
												if ( chat_key != null ){
													
													BuddyPluginUtils.createBetaChat(
														AENetworkClassifier.AT_PUBLIC, 
														chat_key,
														new Runnable()
														{
															public void
															run()
															{
																if ( count.decrementAndGet() == 0 ){
																
																	pub_chat_pending.set( false );
																}
															}
														});
												}
											}
										}
									}
								});
						
						if ( pub_chat_pending.get()){
							
							chat_pub.setEnabled( false );
							chat_pub.setText( chat_pub.getText() + " (" + MessageText.getString( "PeersView.state.pending" ) + ")" );
						}
					}
					
					if ( BuddyPluginUtils.isBetaChatAnonAvailable()){
						
						MenuItem chat_priv = menu_manager.addMenuItem(chat_item,  "label.anon");
						
						chat_priv.addMultiListener(
							new MenuItemListener()
							{	
								public void 
								selected(
									MenuItem menu, 
									Object target) 
								{
									Object[]	rows = (Object[])target;
									
									if ( rows.length > 0 ){
										
										final AtomicInteger count = new AtomicInteger( rows.length );
										
										anon_chat_pending.set( true );

										for ( Object obj: rows ){
											
											String chat_key = chat_key_resolver.getChatKey( obj );
											
											if ( chat_key != null ){
																					
												BuddyPluginUtils.createBetaChat(
													AENetworkClassifier.AT_I2P, 
													chat_key,
													new Runnable()
													{
														public void
														run()
														{
															if ( count.decrementAndGet() == 0 ){
															
																anon_chat_pending.set( false );
															}
														}
													});	
											}
										}
									}
								}
							});
						
						if ( anon_chat_pending.get()){
							
							chat_priv.setEnabled( false );
							chat_priv.setText( chat_priv.getText() + " (" + MessageText.getString( "PeersView.state.pending" ) + ")" );
						}
					
					}else{
						
						MenuItem chat_priv = menu_manager.addMenuItem(chat_item,  "label.anon");
						
						chat_priv.setText( MessageText.getString("label.anon") + "..." );
											
						chat_priv.addMultiListener(
							new MenuItemListener()
							{	
								public void 
								selected(
									MenuItem menu, 
									Object target) 
								{
								
									I2PHelpers.installI2PHelper( null, null, null );
								}
							});
						
						if ( I2PHelpers.isInstallingI2PHelper()){
							
							chat_priv.setEnabled( false );
							chat_priv.setText( chat_priv.getText() + " (" + MessageText.getString( "PeersView.state.pending" ) + ")" );
						}
					}
				}
			});
	
		
		return( chat_item );
	}
}
