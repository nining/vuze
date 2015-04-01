/*
 * File    : ViewUtils.java
 * Created : 24-Oct-2003
 * By      : parg
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
 
package org.gudy.azureus2.ui.swt.views;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.common.table.impl.CoreTableColumn;

/**
 * @author parg
 */

public class 
ViewUtils 
{
	private static SimpleDateFormat formatOverride = null;
	
	static{
		COConfigurationManager.addAndFireParameterListener(
			"Table.column.dateformat", new ParameterListener() {
				public void parameterChanged(String parameterName) {
					String temp = COConfigurationManager.getStringParameter(
										"Table.column.dateformat", "");
					
					if ( temp == null || temp.trim().length() == 0 ){
						
						formatOverride = null;
						
					}else{
						
						try{
							SimpleDateFormat format = new SimpleDateFormat( temp.trim());
							
							format.format(new Date());
							
							formatOverride = format;
							
						}catch( Throwable e ){
							
							formatOverride = null;
						}
					}
				}
			});
	}
	
	public static String
	formatETA(
		long				value,
		boolean				absolute,
		SimpleDateFormat	override )
	{
		SimpleDateFormat df = override!=null?override:formatOverride;
		
		if (	absolute && 
				df != null && 
				value > 0 && 
				!(value == Constants.CRAPPY_INFINITY_AS_INT || value >= Constants.CRAPPY_INFINITE_AS_LONG )){
			
			try{
				return( df.format( new Date( SystemTime.getCurrentTime() + 1000*value )));
				
			}catch( Throwable e ){
			}
		}
		
		return( DisplayFormatters.formatETA( value, absolute ));
	}
	

	public static class
	CustomDateFormat
	{
		private CoreTableColumn			column;
		private TableContextMenuItem	custom_date_menu;
		private SimpleDateFormat		custom_date_format;

		private
		CustomDateFormat(
			CoreTableColumn	_column )
		{
			column	= _column;
			
			custom_date_menu = column.addContextMenuItem(
					"label.date.format", CoreTableColumn.MENU_STYLE_HEADER );
			custom_date_menu.setStyle(TableContextMenuItem.STYLE_PUSH);
			
			custom_date_menu.addListener(new MenuItemListener() {
				public void selected(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object target){
					
					Object existing_o = column.getUserData( "CustomDate" );
					
					String existing_text = "";
					
					if ( existing_o instanceof String ){
						existing_text = (String)existing_o;
					}else if ( existing_o instanceof byte[] ){
						try{
							existing_text = new String((byte[])existing_o, "UTF-8" );
						}catch( Throwable e ){
						}
					}
					SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
							"ConfigView.section.style.customDateFormat",
							"label.date.format");
					
					entryWindow.setPreenteredText( existing_text, false );
					
					entryWindow.prompt(new UIInputReceiverListener() {
						public void UIInputReceiverClosed(UIInputReceiver entryWindow) {
							if (!entryWindow.hasSubmittedInput()) {
								return;
							}
							String date_format = entryWindow.getSubmittedInput();
							
							if ( date_format == null ){
								return;
							}
							
							date_format = date_format.trim();
							
							column.setUserData( "CustomDate", date_format );
							
							column.invalidateCells();
							
							update();
						}
					});
				}
			});
		}
			
		public void
		update()
		{
			Object cd = column.getUserData( "CustomDate" );
			
			String	format = null;
			
			if ( cd instanceof byte[]){
				
				try{
					cd = new String((byte[])cd, "UTF-8");
					
				}catch( Throwable e ){
					
				}
			}
			
			if ( cd instanceof String ){
				
				String	str = (String)cd;
				
				str = str.trim();
				
				if ( str.length() > 0 ){
					
					format = str;
				}
			}
			
			if ( format == null ){
				
				format = MessageText.getString( "label.table.default" );
				
				custom_date_format = null;
				
			}else{
				
				try{
					custom_date_format = new SimpleDateFormat( format );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
			
			custom_date_menu.setText( MessageText.getString( "label.date.format" )  + " <" + format + "> ..." );
		}
		
		public SimpleDateFormat
		getDateFormat()
		{
			return( custom_date_format );
		}
	}
	
	public static CustomDateFormat
	addCustomDateFormat(
		CoreTableColumn	column )
	{
		return( new CustomDateFormat( column ));
	}
	
	public static final String SM_PROP_PERMIT_UPLOAD_DISABLE	= "enable_upload_disable";
	public static final String SM_PROP_PERMIT_DOWNLOAD_DISABLE	= "enable_download_disable";
	
	private static final Map<String,Object>	SM_DEFAULTS = new HashMap<String, Object>();
	
	static{
		SM_DEFAULTS.put( SM_PROP_PERMIT_UPLOAD_DISABLE, false );
		SM_DEFAULTS.put( SM_PROP_PERMIT_DOWNLOAD_DISABLE, false );
	}
	
	public static void
	addSpeedMenu(
		final Shell 		shell,
		Menu				menuAdvanced,
		boolean				doUpMenu,
		boolean				doDownMenu,
		boolean				isTorrentContext,
		boolean				hasSelection,
		boolean				downSpeedDisabled,
		boolean				downSpeedUnlimited,
		long				totalDownSpeed,
		long				downSpeedSetMax,
		long				maxDownload,
		boolean				upSpeedDisabled,
		boolean				upSpeedUnlimited,
		long				totalUpSpeed,
		long				upSpeedSetMax,
		long				maxUpload,
		final int			num_entries,
		Map<String,Object>	_properties,
		final SpeedAdapter	adapter )
 {
		if (doDownMenu) {
			// advanced > Download Speed Menu //
			final MenuItem itemDownSpeed = new MenuItem(menuAdvanced, SWT.CASCADE);
			Messages.setLanguageText(itemDownSpeed,
					"MyTorrentsView.menu.setDownSpeed"); //$NON-NLS-1$
			Utils.setMenuItemImage(itemDownSpeed, "speed");

			Menu menuDownSpeed = new Menu(shell, SWT.DROP_DOWN);
			itemDownSpeed.setMenu(menuDownSpeed);

			addSpeedMenuDown(shell, menuDownSpeed, isTorrentContext, hasSelection,
					downSpeedDisabled, downSpeedUnlimited, totalDownSpeed,
					downSpeedSetMax, maxDownload, num_entries, _properties, adapter);
		}

		if (doUpMenu) {
			// advanced >Upload Speed Menu //
			final MenuItem itemUpSpeed = new MenuItem(menuAdvanced, SWT.CASCADE);
			Messages.setLanguageText(itemUpSpeed, "MyTorrentsView.menu.setUpSpeed"); //$NON-NLS-1$
			Utils.setMenuItemImage(itemUpSpeed, "speed");

			Menu menuUpSpeed = new Menu(shell, SWT.DROP_DOWN);
			itemUpSpeed.setMenu(menuUpSpeed);
			addSpeedMenuUp(shell, menuUpSpeed, isTorrentContext, hasSelection,
					upSpeedDisabled, upSpeedUnlimited, totalUpSpeed, upSpeedSetMax,
					maxUpload, num_entries, _properties, adapter);
		}
	}

	public static void
	addSpeedMenuUp(
		final Shell 		shell,
		Menu				menuSpeed,
		boolean				isTorrentContext,
		boolean				hasSelection,
		boolean				upSpeedDisabled,
		boolean				upSpeedUnlimited,
		long				totalUpSpeed,
		long				upSpeedSetMax,
		long				maxUpload,
		final int			num_entries,
		Map<String,Object>	_properties,
		final SpeedAdapter	adapter )
	{
		Map<String,Object>	properties = new HashMap<String, Object>( SM_DEFAULTS );
		if ( _properties != null ){
			properties.putAll( _properties );
		}
		
		String menu_key = "MyTorrentsView.menu.manual";
		if (num_entries > 1) {menu_key += (isTorrentContext?".per_torrent":".per_peer" );}

		if ( menuSpeed != null ){
	
			final MenuItem itemCurrentUpSpeed = new MenuItem(menuSpeed, SWT.PUSH);
			itemCurrentUpSpeed.setEnabled(false);
			String separator = "";
			StringBuffer speedText = new StringBuffer();
			//itemUpSpeed.                   
			if (upSpeedDisabled) {
				speedText.append(MessageText
						.getString("MyTorrentsView.menu.setSpeed.disabled"));
				separator = " / ";
			}
			if (upSpeedUnlimited) {
				speedText.append(separator);
				speedText.append(MessageText
						.getString("MyTorrentsView.menu.setSpeed.unlimited"));
				separator = " / ";
			}
			if (totalUpSpeed > 0) {
				speedText.append(separator);
				speedText.append(DisplayFormatters
						.formatByteCountToKiBEtcPerSec(totalUpSpeed));
			}
			itemCurrentUpSpeed.setText(speedText.toString());
	
			// ---
			new MenuItem(menuSpeed, SWT.SEPARATOR);
				
			Listener itemsUpSpeedListener = new Listener() {
				public void handleEvent(Event e) {
					if (e.widget != null && e.widget instanceof MenuItem) {
						MenuItem item = (MenuItem) e.widget;
						int speed = item.getData("maxul") == null ? 0 : ((Integer) item
								.getData("maxul")).intValue();
						adapter.setUpSpeed(speed);
					}
				}
			};
	
			if ( num_entries > 1 || !upSpeedUnlimited ){
				MenuItem mi = new MenuItem(menuSpeed, SWT.PUSH);
				Messages.setLanguageText(mi,
						"MyTorrentsView.menu.setSpeed.unlimit");
				mi.setData("maxul", new Integer(0));
				mi.addListener(SWT.Selection, itemsUpSpeedListener);
			}
			
			boolean allowDisable = (Boolean)properties.get( SM_PROP_PERMIT_UPLOAD_DISABLE );

			if ( allowDisable && !upSpeedDisabled ){
				MenuItem mi = new MenuItem(menuSpeed, SWT.PUSH);
				Messages.setLanguageText(mi,
						"MyTorrentsView.menu.setSpeed.disable");
				mi.setData("maxul", new Integer(-1));
				mi.addListener(SWT.Selection, itemsUpSpeedListener);
			}
			
			if (hasSelection) {
				//using 75KiB/s as the default limit when no limit set.
				if (maxUpload == 0){
					maxUpload = 75 * 1024;
				}else{
					if ( upSpeedSetMax <= 0 ){
						maxUpload = 200 * 1024;
					}else{
						maxUpload = 4 * ( upSpeedSetMax/1024 ) * 1024;
					}
				}
				for (int i = 0; i < 10; i++) {
					MenuItem mi = new MenuItem(menuSpeed, SWT.PUSH);
					mi.addListener(SWT.Selection, itemsUpSpeedListener);
	
					int limit = (int)( maxUpload / (10 * num_entries) * (10 - i));
					String speed = DisplayFormatters.formatByteCountToKiBEtcPerSec(limit
							* num_entries);
					if (num_entries > 1) {
						speed = MessageText.getString("MyTorrentsView.menu.setSpeed.multi",
								new String[] {
									speed,
									String.valueOf(num_entries),
									DisplayFormatters.formatByteCountToKiBEtcPerSec(limit)
								});
					}

					mi.setText(speed);
					mi.setData("maxul", new Integer(limit));
				}
			}
	
			new MenuItem(menuSpeed, SWT.SEPARATOR);
	
			final MenuItem itemUpSpeedManualSingle = new MenuItem(menuSpeed, SWT.PUSH);
			Messages.setLanguageText(itemUpSpeedManualSingle, menu_key);
			itemUpSpeedManualSingle.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					int speed_value = getManualSpeedValue(shell, false);
					if (speed_value > 0) {adapter.setUpSpeed(speed_value);}
				}
			});
			
			if (num_entries > 1) {
				final MenuItem itemUpSpeedManualShared = new MenuItem(menuSpeed, SWT.PUSH);
				Messages.setLanguageText(itemUpSpeedManualShared, isTorrentContext?"MyTorrentsView.menu.manual.shared_torrents":"MyTorrentsView.menu.manual.shared_peers" );
				itemUpSpeedManualShared.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						int speed_value = getManualSharedSpeedValue(shell, false, num_entries);
						if (speed_value > 0) {adapter.setUpSpeed(speed_value);}
					}
				});
			}
		}
	}

	public static void
	addSpeedMenuDown(
		final Shell 		shell,
		Menu				menuSpeed,
		boolean				isTorrentContext,
		boolean				hasSelection,
		boolean				downSpeedDisabled,
		boolean				downSpeedUnlimited,
		long				totalDownSpeed,
		long				downSpeedSetMax,
		long				maxDownload,
		final int			num_entries,
		Map<String,Object>	_properties,
		final SpeedAdapter	adapter )
	{
		Map<String,Object>	properties = new HashMap<String, Object>( SM_DEFAULTS );
		if ( _properties != null ){
			properties.putAll( _properties );
		}
		
		String menu_key = "MyTorrentsView.menu.manual";
		if (num_entries > 1) {menu_key += (isTorrentContext?".per_torrent":".per_peer" );}

		if ( menuSpeed != null ){
			final MenuItem itemCurrentDownSpeed = new MenuItem(menuSpeed, SWT.PUSH);
			itemCurrentDownSpeed.setEnabled(false);
			StringBuffer speedText = new StringBuffer();
			String separator = "";
			//itemDownSpeed.                   
			if (downSpeedDisabled) {
				speedText.append(MessageText
						.getString("MyTorrentsView.menu.setSpeed.disabled"));
				separator = " / ";
			}
			if (downSpeedUnlimited) {
				speedText.append(separator);
				speedText.append(MessageText
						.getString("MyTorrentsView.menu.setSpeed.unlimited"));
				separator = " / ";
			}
			if (totalDownSpeed > 0) {
				speedText.append(separator);
				speedText.append(DisplayFormatters
						.formatByteCountToKiBEtcPerSec(totalDownSpeed));
			}
			itemCurrentDownSpeed.setText(speedText.toString());
	
			new MenuItem(menuSpeed, SWT.SEPARATOR);
	
			Listener itemsDownSpeedListener = new Listener() {
				public void handleEvent(Event e) {
					if (e.widget != null && e.widget instanceof MenuItem) {
						MenuItem item = (MenuItem) e.widget;
						int speed = item.getData("maxdl") == null ? 0 : ((Integer) item
								.getData("maxdl")).intValue();
						adapter.setDownSpeed(speed);
					}
				}
			};
	
			if ( num_entries > 1 || !downSpeedUnlimited ){
				MenuItem mi = new MenuItem(menuSpeed, SWT.PUSH);
				Messages.setLanguageText(mi,
						"MyTorrentsView.menu.setSpeed.unlimit");
				mi.setData("maxdl", new Integer(0));
				mi.addListener(SWT.Selection, itemsDownSpeedListener);
			}
			
			boolean allowDisable = (Boolean)properties.get( SM_PROP_PERMIT_DOWNLOAD_DISABLE );

			if ( allowDisable && !downSpeedDisabled ){
				MenuItem mi = new MenuItem(menuSpeed, SWT.PUSH);
				Messages.setLanguageText(mi,
						"MyTorrentsView.menu.setSpeed.down.disable");
				mi.setData("maxdl", new Integer(-1));
				mi.addListener(SWT.Selection, itemsDownSpeedListener);
			}
			
			if (hasSelection) {
	
				//using 200KiB/s as the default limit when no limit set.
				if (maxDownload == 0){		
					if ( downSpeedSetMax <= 0 ){
						maxDownload = 200 * 1024;
					}else{
						maxDownload	= 4 * ( downSpeedSetMax/1024 ) * 1024;
					}
				}
	
				for (int i = 0; i < 10; i++) {
					MenuItem mi = new MenuItem(menuSpeed, SWT.PUSH);
					mi.addListener(SWT.Selection, itemsDownSpeedListener);
		
					// dms.length has to be > 0 when hasSelection
					int limit = (int)(maxDownload / (10 * num_entries) * (10 - i));
					String speed = DisplayFormatters.formatByteCountToKiBEtcPerSec(limit
							* num_entries);
					if (num_entries > 1) {
						speed = MessageText.getString("MyTorrentsView.menu.setSpeed.multi", new String[] {
							speed,
							String.valueOf(num_entries),
							DisplayFormatters.formatByteCountToKiBEtcPerSec(limit)
						});
					}
					mi.setText(speed);
					mi.setData("maxdl", new Integer(limit));
				}
			}
	
			// ---
			new MenuItem(menuSpeed, SWT.SEPARATOR);
				
			final MenuItem itemDownSpeedManualSingle = new MenuItem(menuSpeed, SWT.PUSH);
			Messages.setLanguageText(itemDownSpeedManualSingle, menu_key);
			itemDownSpeedManualSingle.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					int speed_value = getManualSpeedValue(shell, true);
					if (speed_value > 0) {adapter.setDownSpeed(speed_value);}
				}
			});
			
			if (num_entries > 1) {
				final MenuItem itemDownSpeedManualShared = new MenuItem(menuSpeed, SWT.PUSH);
				Messages.setLanguageText(itemDownSpeedManualShared, isTorrentContext?"MyTorrentsView.menu.manual.shared_torrents":"MyTorrentsView.menu.manual.shared_peers");
				itemDownSpeedManualShared.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						int speed_value = getManualSharedSpeedValue(shell, true, num_entries);
						if (speed_value > 0) {adapter.setDownSpeed(speed_value);}
					}
				});
			}
		}
	}

	public static int getManualSpeedValue(Shell shell, boolean for_download) {
		String kbps_str = MessageText.getString("MyTorrentsView.dialog.setNumber.inKbps",
				new String[]{ DisplayFormatters.getRateUnit(DisplayFormatters.UNIT_KB ) });
		
		String set_num_str = MessageText.getString("MyTorrentsView.dialog.setNumber." +
				((for_download) ? "download" : "upload"));

		SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow();
		entryWindow.initTexts(
				"MyTorrentsView.dialog.setSpeed.title",
				new String[] {set_num_str},
				"MyTorrentsView.dialog.setNumber.text",
				new String[] {
						kbps_str,
						set_num_str
				});

		entryWindow.prompt();
		if (!entryWindow.hasSubmittedInput()) {
			return -1;
		}
		String sReturn = entryWindow.getSubmittedInput();
		
		if (sReturn == null)
			return -1;

		try {
			int result = (int) (Double.valueOf(sReturn).doubleValue() * 1024);
			
			if ( DisplayFormatters.isRateUsingBits()){
				
				result /= 8;
			}
			
			if (result <= 0) {throw new NumberFormatException("non-positive number entered");}
			return result;
		} catch (NumberFormatException er) {
			MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			mb.setText(MessageText
					.getString("MyTorrentsView.dialog.NumberError.title"));
			mb.setMessage(MessageText
					.getString("MyTorrentsView.dialog.NumberError.text"));

			mb.open();
			return -1;
		}
	}
	
	public static int getManualSharedSpeedValue(Shell shell, boolean for_download, int num_entries) {
		int result = getManualSpeedValue(shell, for_download);
		if (result == -1) {return -1;}
		result = result / num_entries;
		if (result == 0) {result = 1;}
		return result;
	}
	
	public static void setViewRequiresOneDownload(Composite genComposite) {
		if (genComposite == null || genComposite.isDisposed()) {
			return;
		}
		Utils.disposeComposite(genComposite, false);

		Label lab = new Label(genComposite, SWT.NULL);
		GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
		gridData.verticalIndent = 10;
		lab.setLayoutData(gridData);
		Messages.setLanguageText(lab, "view.one.download.only");

		genComposite.layout(true);

		Composite parent = genComposite.getParent();
		if (parent instanceof ScrolledComposite) {
			ScrolledComposite scrolled_comp = (ScrolledComposite) parent;
			
			Rectangle r = scrolled_comp.getClientArea();
			scrolled_comp.setMinSize(genComposite.computeSize(r.width, SWT.DEFAULT ));
		}

	}
	
	public interface
	SpeedAdapter
	{
		public void
		setUpSpeed(
			int		val );
		
		public void
		setDownSpeed(
			int		val );
	}
	
	public interface
	ViewTitleExtraInfo
	{
		public void
		update(
			Composite	composite,
			int			count,
			int			active );
			
		public void
		setEnabled(
			Composite	composite,
			boolean		enabled );
			
	}
}
