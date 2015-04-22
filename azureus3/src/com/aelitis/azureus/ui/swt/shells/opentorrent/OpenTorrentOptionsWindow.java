/**
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

package com.aelitis.azureus.ui.swt.shells.opentorrent;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.StringIterator;
import org.gudy.azureus2.core3.config.StringList;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerAvailability;
import org.gudy.azureus2.core3.download.DownloadManagerFactory;
import org.gudy.azureus2.core3.internat.LocaleTorrentUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.torrent.impl.TorrentOpenFileOptions;
import org.gudy.azureus2.core3.torrent.impl.TorrentOpenOptions;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.MenuBuildUtils.MenuBuilder;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.config.generic.GenericIntParameter;
import org.gudy.azureus2.ui.swt.config.generic.GenericParameterAdapter;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.maketorrent.MultiTrackerEditor;
import org.gudy.azureus2.ui.swt.maketorrent.TrackerEditorListener;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.TrackerAvailView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.TrackerNameItem;
import org.gudy.azureus2.ui.swt.views.utils.TagUIUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.content.ContentException;
import com.aelitis.azureus.core.content.RelatedAttributeLookupListener;
import com.aelitis.azureus.core.content.RelatedContentManager;
import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.util.RegExUtil;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta.ChatInstance;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginUtils;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginViewInterface;
import com.aelitis.azureus.ui.IUIIntializer;
import com.aelitis.azureus.ui.InitializerListener;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionListener;
import com.aelitis.azureus.ui.common.table.TableViewFilterCheck;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.shells.main.UIFunctionsImpl;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.uiupdater.UIUpdaterSWT;
import com.aelitis.azureus.ui.swt.utils.TagUIUtilsV3;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener;
import com.aelitis.azureus.ui.swt.views.skin.StandardButtonsArea;
import com.aelitis.azureus.util.ImportExportUtils;

@SuppressWarnings({
	"unchecked",
	"rawtypes"
})
public class OpenTorrentOptionsWindow
	implements UIUpdatable
{
	private static final Map<HashWrapper,OpenTorrentOptionsWindow>	active_windows = new HashMap<HashWrapper, OpenTorrentOptionsWindow>();
	
	private final static class FileStatsCacheItem
	{
		boolean exists;

		long freeSpace;

		public FileStatsCacheItem(final File f) {
			exists = f.exists();
			if (exists)
				freeSpace = FileUtil.getUsableSpace(f);
			else
				freeSpace = -1;
		}
	}

	private final static class Partition
	{
		long bytesToConsume = 0;

		long freeSpace = 0;

		final File root;

		public Partition(File root) {
			this.root = root;
		}
	}

	private final static String PARAM_DEFSAVEPATH = "Default save path";

	private final static String[] queueLocations = {
		"first",
		"last"
	};

	private final static String[] startModes = {
		"queued",
		"stopped",
		"forceStarted",
		"seeding"
	};

	public static final String TABLEID_TORRENTS = "OpenTorrentTorrent";
	public static final String TABLEID_FILES 	= "OpenTorrentFile";



	public static void main(String[] args) {
		try{
			SWTThread.createInstance(
				new IUIIntializer() {
					
					public void stopIt(boolean isForRestart, boolean isCloseAreadyInProgress) {
						// TODO Auto-generated method stub
						
					}
					
					public void runInSWTThread() {
						// TODO Auto-generated method stub
						
					}
					
					public void run() {
						AzureusCore core = AzureusCoreFactory.create();
						core.start();
				
						UIConfigDefaultsSWT.initialize();
								
						Colors.getInstance();
				
						UIFunctionsImpl uiFunctions = new UIFunctionsImpl(null);
						UIFunctionsManager.setUIFunctions(uiFunctions);
				
						File file1 = new File("C:\\temp\\test.torrent");
						File file2 = new File("C:\\temp\\test1.torrent");
				
						TOTorrent torrent1 = null;
						try {
							torrent1 = TOTorrentFactory.deserialiseFromBEncodedFile(file1);
						} catch (TOTorrentException e) {
							e.printStackTrace();
						};
				
						TOTorrent torrent2 = null;
						try {
							torrent2 = TOTorrentFactory.deserialiseFromBEncodedFile(file2);
						} catch (TOTorrentException e) {
							e.printStackTrace();
						};
						
						COConfigurationManager.setParameter( ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_SEP, false );
						COConfigurationManager.setParameter( "User Mode", 2 );
						
						addTorrent(	new TorrentOpenOptions(null, torrent1, false));
						
						addTorrent(	new TorrentOpenOptions(null, torrent2, false));
					}
					
					public void reportPercent(int percent) {
						// TODO Auto-generated method stub
						
					}
					
					public void reportCurrentTask(String currentTaskString) {
						// TODO Auto-generated method stub
						
					}
					
					public void removeListener(InitializerListener listener) {
						// TODO Auto-generated method stub
						
					}
					
					public void initializationComplete() {
						// TODO Auto-generated method stub
						
					}
					
					public void increaseProgress() {
						// TODO Auto-generated method stub
						
					}
					
					public void addListener(InitializerListener listener) {
						// TODO Auto-generated method stub
						
					}
					
					public void abortProgress() {
						// TODO Auto-generated method stub
						
					}
				});

		}catch( Throwable e ){
			e.printStackTrace();
		}
	}

	
	private SkinnedDialog 			dlg;
	private ImageLoader 			image_loader;
	private SWTSkinObjectSash 		sash_object;
	private StackLayout				expand_stack;
	private	Composite 				expand_stack_area;
	private StandardButtonsArea 	buttonsArea;
	private boolean 				window_initialised;
	
	private Button	buttonTorrentUp;
	private Button	buttonTorrentDown;
	private Button	buttonTorrentRemove;
	
	private List<String>	images_to_dispose = new ArrayList<String>();
	
	
	private TableViewSWT<OpenTorrentInstance> 	tvTorrents;
	private Label								torrents_info_label;
	
	private OpenTorrentInstanceListener	optionListener;
	
	private List<OpenTorrentInstance>	open_instances 		= new ArrayList<OpenTorrentOptionsWindow.OpenTorrentInstance>();
	private List<OpenTorrentInstance>	selected_instances 	= new ArrayList<OpenTorrentOptionsWindow.OpenTorrentInstance>();
	
	private OpenTorrentInstance			multi_selection_instance;

	protected List<String> listDiscoveredTags = new ArrayList<String>();
	protected List<String> listTagsToCreate = new ArrayList<String>();
	
	public static OpenTorrentOptionsWindow
	addTorrent(
		final TorrentOpenOptions torrentOptions )
	{
		TOTorrent torrent = torrentOptions.getTorrent();
		
		try{
			final HashWrapper hw = torrent.getHashWrapper();
			
			synchronized( active_windows ){

				final OpenTorrentOptionsWindow existing = active_windows.get( hw );
				
				if ( existing != null ){
										
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
					
							existing.swt_activate();
						}
					});
					
					return( existing );
				}
				
				boolean	separate_dialogs = COConfigurationManager.getBooleanParameter( ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_SEP );
				
				if ( !separate_dialogs ){
					
					if ( active_windows.size() > 0 ){
						
						final OpenTorrentOptionsWindow reuse_window = active_windows.values().iterator().next();
													
						active_windows.put( hw,  reuse_window );
						
						Utils.execSWTThread(new AERunnable() {
							public void runSupport()
							{
								reuse_window.swt_addTorrent( hw, torrentOptions );
							}
						});
						
						
						return( reuse_window );
					}
				}
				
				final OpenTorrentOptionsWindow new_window = new OpenTorrentOptionsWindow();
				
				active_windows.put( hw,  new_window );
				
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
				
						new_window.swt_addTorrent( hw, torrentOptions );
					}
				});
				
				return( new_window );
			}
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( null );
		}

	}
	
	private
	OpenTorrentOptionsWindow()
	{
		image_loader = SWTSkinFactory.getInstance().getImageLoader(SWTSkinFactory.getInstance().getSkinProperties());

		optionListener = 
			new OpenTorrentInstanceListener()
		{
			public void 
			instanceChanged(
				OpenTorrentInstance instance )
			{
				updateInstanceInfo();
			}
		};
	}

	protected void 
	swt_addTorrent(
		HashWrapper				hash,
		TorrentOpenOptions		torrentOptions )
	{		
		try{
			if ( dlg == null ){
				
				dlg = new SkinnedDialog("skin3_dlg_opentorrent_options", "shell",
						SWT.RESIZE | SWT.MAX | SWT.DIALOG_TRIM);
						
				final SWTSkin skin_outter = dlg.getSkin();
												
				SWTSkinObject so;
				
				if (COConfigurationManager.hasParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS, true)) {
		  		
					so = skin_outter.getSkinObject("showagain-area");
					
					if (so != null) {
						so.setVisible(false);
					}
				}
					
				SWTSkinObject soButtonArea = skin_outter.getSkinObject("button-area");
				
				if (soButtonArea instanceof SWTSkinObjectContainer) {
					buttonsArea = new StandardButtonsArea() {
						protected void clicked(int intValue) {
							if (intValue == SWT.OK) {
								boolean	all_ok = true;
							
								AsyncDispatcher dispatcher = new AsyncDispatcher();
								
								for ( final OpenTorrentInstance instance: new ArrayList<OpenTorrentInstance>( open_instances )){
									
									String dataDir = instance.cmbDataDir.getText();
	
									if ( !instance.okPressed(dataDir)){
									
										all_ok = false;
										
									}else{
										
											// serialise additions in correct order
										
										dispatcher.dispatch(
											new AERunnable()
											{
												public void
												runSupport()
												{
													TorrentOpener.addTorrent( instance.getOptions());

												}
											});
										
										removeInstance( instance );
									}
								}
								
								if ( all_ok ){
									if (dlg != null){
										dlg.close();
									}
								}
							}else if (dlg != null){
								dlg.close();
							}
						}
					};
					buttonsArea.setButtonIDs(new String[] {
						MessageText.getString("Button.ok"),
						MessageText.getString("Button.cancel")
					});
					buttonsArea.setButtonVals(new Integer[] {
						SWT.OK,
						SWT.CANCEL
					});
					buttonsArea.swt_createButtons(((SWTSkinObjectContainer) soButtonArea).getComposite());
				}
					
				sash_object = (SWTSkinObjectSash)skin_outter.getSkinObject("multi-sash");
									
				SWTSkinObjectContainer select_area = (SWTSkinObjectContainer)skin_outter.getSkinObject( "torrents-table" );

				setupTVTorrents( select_area.getComposite());
				
				SWTSkinObjectContainer torrents_info = (SWTSkinObjectContainer)skin_outter.getSkinObject( "torrents-info" );

				Composite info_area = torrents_info.getComposite();
				
				info_area.setLayout( new GridLayout());
				
				torrents_info_label = new Label( info_area, SWT.NULL );
				torrents_info_label.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ));				
				
				sash_object.setVisible( false );
				sash_object.setAboveVisible( false );
				
				so = skin_outter.getSkinObject("expand-area");
		
				expand_stack_area = ((SWTSkinObjectContainer)so).getComposite();
		
				expand_stack	= new StackLayout();
				
				expand_stack_area.setLayout( expand_stack );
				
				Composite expand_area = new Composite( expand_stack_area, SWT.NULL );
				
				expand_area.setLayout( new FormLayout());
				
				expand_stack.topControl = expand_area;
				
				OpenTorrentInstance instance = new OpenTorrentInstance( hash, expand_area, torrentOptions, optionListener );

				addInstance( instance );
								
				selected_instances.add( instance );
								
				UIUpdaterSWT.getInstance().addUpdater(this);
				
				setupShowAgainOptions(skin_outter);
	
					/* 
					 * The bring-to-front logic for torrent addition is controlled by other parts of the code so we don't
					 * want the dlg to override this behaviour (main example here is torrents passed from, say, a browser,
					 * and the user has disabled the 'show vuze on external torrent add' feature)
					 */
							
				dlg.open("otow",false);
				
				synchronized( active_windows ){
					
					int	num_active_windows = active_windows.size();
					
					if ( num_active_windows > 1 ){
						
						int	max_x = 0;
						int max_y = 0;
						
						for ( OpenTorrentOptionsWindow window: active_windows.values()){
							
							if ( window == this || !window.isInitialised()){
								
								continue;
							}
							
							Rectangle rect = window.getBounds();
							
							max_x = Math.max( max_x, rect.x );
							max_y = Math.max( max_y, rect.y );
						}
						
						Shell shell = dlg.getShell();
						
						Rectangle rect = shell.getBounds();
										
						rect.x = max_x + 16;
						rect.y = max_y + 16;
						
						shell.setBounds( rect );
												
						Utils.verifyShellRect( shell, true );
					}
				}
				
				dlg.addCloseListener(new SkinnedDialogClosedListener() {
					public void skinDialogClosed(SkinnedDialog dialog) {
						try{
							dispose();
							
						}finally{
							
							synchronized( active_windows ){
								
								Iterator<OpenTorrentOptionsWindow> it = active_windows.values().iterator();
								
								while( it.hasNext()){
									
									if ( it.next() == OpenTorrentOptionsWindow.this ){
										
										it.remove();
									}
								}
							}
						}
					}
				});
				
				window_initialised = true;
				
			}else{
				
				Composite expand_area = new Composite( expand_stack_area, SWT.NULL );
				
				expand_area.setLayout( new FormLayout());
								
				OpenTorrentInstance instance = new OpenTorrentInstance( hash, expand_area, torrentOptions, optionListener );

				addInstance( instance );
												
				if ( !sash_object.isVisible()){
					
					sash_object.setVisible( true );
					
					sash_object.setAboveVisible( true );
					
					Utils.execSWTThreadLater(
						0,
						new Runnable()
						{
							public void
							run()
							{
								tvTorrents.processDataSourceQueueSync();
								
								List<TableRowCore> rows = new ArrayList<TableRowCore>();
								
								for ( OpenTorrentInstance instance: selected_instances ){
									
									TableRowCore row = tvTorrents.getRow( instance );
									
									if ( row != null ){
										
										rows.add( row );
									}
								}
								
								if ( rows.size() > 0 ){
									
									tvTorrents.setSelectedRows( rows.toArray( new TableRowCore[ rows.size() ]));
								}
							}
						});
						
				}
			}

		}catch( Throwable e ){
			
			Debug.out( e );
			
			synchronized( active_windows ){
				
				active_windows.remove( hash );
			}
		}
	}
	
	private boolean
	isInitialised()
	{
		return( window_initialised );
	}
	
	private void setupShowAgainOptions(SWTSkin skin) {
		SWTSkinObjectCheckbox soNever = (SWTSkinObjectCheckbox) skin.getSkinObject("showagain-never");
		SWTSkinObjectCheckbox soAlways = (SWTSkinObjectCheckbox) skin.getSkinObject("showagain-always");
		SWTSkinObjectCheckbox soMany = (SWTSkinObjectCheckbox) skin.getSkinObject("showagain-manyfile");

		String showAgainMode = COConfigurationManager.getStringParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS);
		boolean hasUserChosen = COConfigurationManager.hasParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS, true);

		if (soNever != null) {
			soNever.addSelectionListener(new SWTSkinCheckboxListener() {
				public void checkboxChanged(SWTSkinObjectCheckbox so, boolean checked) {
					COConfigurationManager.setParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS, ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_NEVER);
				}
			});
			if (hasUserChosen) {
				soNever.setChecked(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_NEVER.equals(showAgainMode));
			}
		}

		if (soAlways != null) {
			soAlways.addSelectionListener(new SWTSkinCheckboxListener() {
				public void checkboxChanged(SWTSkinObjectCheckbox so, boolean checked) {
					COConfigurationManager.setParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS, ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_ALWAYS);
				}
			});
			if (hasUserChosen) {
				soAlways.setChecked(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_ALWAYS.equals(showAgainMode));
			}
		}

		if (soMany != null) {
			soMany.addSelectionListener(new SWTSkinCheckboxListener() {
				public void checkboxChanged(SWTSkinObjectCheckbox so, boolean checked) {
					COConfigurationManager.setParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS, ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_MANY);
				}
			});
			if (hasUserChosen) {
				soMany.setChecked(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_MANY.equals(showAgainMode));
			}
		}
	}
	
	private void 
	setupTVTorrents(
		Composite		parent )
	{
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = layout.verticalSpacing = 0;
		parent.setLayout( layout );
		GridData gd;
		
		// table
		
		Composite table_area = new Composite( parent, SWT.NULL );
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = layout.verticalSpacing = 0;
		table_area.setLayout( layout );
		gd = new GridData( GridData.FILL_BOTH );
		table_area.setLayoutData( gd );
		
			// toolbar area
		
		Composite button_area = new Composite( parent, SWT.NULL );
		layout = new GridLayout(5,false);
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = layout.verticalSpacing = 0;
		layout.marginTop = 5;
		button_area.setLayout( layout);
		gd = new GridData( GridData.FILL_HORIZONTAL );
		button_area.setLayoutData( gd );
		
		Label label = new Label( button_area, SWT.NULL );
		gd = new GridData( GridData.FILL_HORIZONTAL );
		label.setLayoutData( gd );
		
		buttonTorrentUp = new Button(button_area, SWT.PUSH);
		buttonTorrentUp.setImage( loadImage( "image.toolbar.up" ));
		buttonTorrentUp.setToolTipText(MessageText.getString("Button.moveUp"));
		buttonTorrentUp.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				List<OpenTorrentInstance> selected = (List<OpenTorrentInstance>)(Object)tvTorrents.getSelectedDataSources();
				if ( selected.size() > 1 ){
					Collections.sort(
						selected,
						new Comparator<OpenTorrentInstance>()
						{
							public int 
							compare(
								OpenTorrentInstance o1,
								OpenTorrentInstance o2) 
							{
								return( o1.getIndex() - o2.getIndex());
							}
						});
				}
				
				boolean modified = false;
				for ( OpenTorrentInstance instance: selected ){
										
					int index = instance.getIndex();
					if ( index > 0 ){
						open_instances.remove( instance );
						open_instances.add( index-1, instance );
						modified = true;
					}
				}
				if ( modified ){
					swt_updateTVTorrentButtons();
						
					refreshTVTorrentIndexes();
				}
			}});

		
		buttonTorrentDown = new Button(button_area, SWT.PUSH);
		buttonTorrentDown.setImage( loadImage( "image.toolbar.down" ));
		buttonTorrentDown.setToolTipText(MessageText.getString("Button.moveDown"));
		buttonTorrentDown.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				List<OpenTorrentInstance> selected = (List<OpenTorrentInstance>)(Object)tvTorrents.getSelectedDataSources();
				if ( selected.size() > 1 ){
					Collections.sort(
						selected,
						new Comparator<OpenTorrentInstance>()
						{
							public int 
							compare(
								OpenTorrentInstance o1,
								OpenTorrentInstance o2) 
							{
								return( o2.getIndex() - o1.getIndex());
							}
						});
				}
				boolean modified = false;
				for ( Object obj: selected ){
					
					OpenTorrentInstance	instance = (OpenTorrentInstance)obj;
					int index = instance.getIndex();
					if ( index < open_instances.size() - 1 ){
						open_instances.remove( instance );
						open_instances.add( index+1, instance );
						modified = true;
					}
				}
				
				if ( modified ){
					swt_updateTVTorrentButtons();
						
					refreshTVTorrentIndexes();
				}
			}});
		
		buttonTorrentRemove = new Button(button_area, SWT.PUSH);
		buttonTorrentRemove.setToolTipText(MessageText.getString("OpenTorrentWindow.torrent.remove"));
		buttonTorrentRemove.setImage( loadImage( "image.toolbar.remove" ));
		buttonTorrentRemove.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				List<Object> selected = tvTorrents.getSelectedDataSources();
				for ( Object obj: selected ){
					
					OpenTorrentInstance	instance = (OpenTorrentInstance)obj;
					
					removeInstance( instance );
				}
			}});
		
		buttonTorrentUp.setEnabled( false );
		buttonTorrentDown.setEnabled( false );
		buttonTorrentRemove.setEnabled( false );
		
		label = new Label( button_area, SWT.NULL );
		gd = new GridData( GridData.FILL_HORIZONTAL );
		label.setLayoutData( gd );
		
	
		
		TableColumnManager tcm = TableColumnManager.getInstance();
		
		if (tcm.getDefaultColumnNames(TABLEID_TORRENTS) == null) {

			tcm.registerColumn(OpenTorrentInstance.class,
					TableColumnOTOT_Position.COLUMN_ID, new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new TableColumnOTOT_Position(column);
						}
					});

			tcm.registerColumn(OpenTorrentInstance.class,
					TableColumnOTOT_Name.COLUMN_ID, new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new TableColumnOTOT_Name(column);
						}
					});

			tcm.registerColumn(OpenTorrentInstance.class,
					TableColumnOTOT_Size.COLUMN_ID, new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new TableColumnOTOT_Size(column);
						}
					});
			
			tcm.setDefaultColumnNames(TABLEID_TORRENTS, new String[] {
				
				TableColumnOTOT_Position.COLUMN_ID,
				TableColumnOTOT_Name.COLUMN_ID,
				TableColumnOTOT_Size.COLUMN_ID,
			});
			
			tcm.setDefaultSortColumnName(TABLEID_TORRENTS, TableColumnOTOT_Position.COLUMN_ID);
		}

		tvTorrents = TableViewFactory.createTableViewSWT(OpenTorrentInstance.class,
				TABLEID_TORRENTS, TABLEID_TORRENTS, null, "#", SWT.BORDER
						| SWT.FULL_SELECTION | SWT.MULTI );
		
		tvTorrents.initialize( table_area );
		
		tvTorrents.setRowDefaultHeight(20);


		tvTorrents.addMenuFillListener(
			new TableViewSWTMenuFillListener() 
			{
				public void 
				fillMenu(
					String 		sColumnName, 
					Menu 		menu )
				{
					final List<Object> selected = tvTorrents.getSelectedDataSources();
					
					if ( selected.size() > 0 ){
							
						final List<OpenTorrentInstance> instances = new ArrayList<OpenTorrentOptionsWindow.OpenTorrentInstance>( selected.size());
							
						final List<OpenTorrentInstance> non_simple_instances = new ArrayList<OpenTorrentOptionsWindow.OpenTorrentInstance>();
								
						boolean can_rtlf = false;
						
						for ( Object o: selected ){
							
							OpenTorrentInstance oti = (OpenTorrentInstance)o;
							
							instances.add( oti );
							
							if ( !oti.getOptions().isSimpleTorrent()){
								
								non_simple_instances.add( oti );
								
								if ( oti.canRemoveTopLevelFolder()){
									
									can_rtlf = true;
								}
							}
						}
							
						{
							MenuItem item = new MenuItem(menu, SWT.PUSH);
							
							Messages.setLanguageText(item, "OpenTorrentWindow.fileList.changeDestination");
							
							item.addSelectionListener(
								new SelectionAdapter() 
								{
									public void 
									widgetSelected(
										SelectionEvent e )
									{
										for ( Object obj: selected ){
											
											OpenTorrentInstance	instance = (OpenTorrentInstance)obj;
											
											instance.setSavePath();
										}
									}
								});
						}
						
						{
							MenuItem item = new MenuItem(menu, SWT.PUSH);
							
							Messages.setLanguageText(item, "OpenTorrentWindow.tlf.remove");
							
							item.addSelectionListener(
								new SelectionAdapter() 
								{
									public void 
									widgetSelected(
										SelectionEvent e )
									{
										for ( Object obj: selected ){
											
											OpenTorrentInstance	instance = (OpenTorrentInstance)obj;
											
											if ( instance.canRemoveTopLevelFolder()){
											
												instance.removeTopLevelFolder();
											}
										}
									}
								});
							
							item.setEnabled( can_rtlf );
						}
						
						{
							MenuItem item = new MenuItem(menu, SWT.CHECK );
								
							 item.setData( COConfigurationManager.getBooleanParameter( "open.torrent.window.rename.on.tlf.change" ));
								
							 Messages.setLanguageText(item, "OpenTorrentWindow.tlf.rename");

							 item.addSelectionListener(
								 new SelectionAdapter() 
								 {
									 public void 
									 widgetSelected(
											 SelectionEvent e )
									 {
										 COConfigurationManager.setParameter(
												"open.torrent.window.rename.on.tlf.change", 
												((MenuItem)e.widget).getSelection());
									 }
								 });
							
							item.setEnabled( non_simple_instances.size() > 0 );
						}
						
						new MenuItem(menu, SWT.SEPARATOR);

						MenuItem item = new MenuItem(menu, SWT.PUSH);
						
						Messages.setLanguageText(item, "Button.remove");
						
						item.addSelectionListener(
							new SelectionAdapter() 
							{
								public void 
								widgetSelected(
									SelectionEvent e )
								{
									for ( Object obj: selected ){
										
										OpenTorrentInstance	instance = (OpenTorrentInstance)obj;
										
										removeInstance( instance );
									}
								}
							});
						
						new MenuItem(menu, SWT.SEPARATOR);
					}
				}
				
	
				public void 
				addThisColumnSubMenu(
					String 	sColumnName, 
					Menu 	menuThisColumn) 
				{
				}
			});
		
		
		tvTorrents.addSelectionListener(
			new TableSelectionListener() 
			{
				public void 
				selected(
					TableRowCore[] rows_not_used ) 
				{
					TableRowCore[] rows = tvTorrents.getSelectedRows();
					
					List<OpenTorrentInstance> instances = new ArrayList<OpenTorrentOptionsWindow.OpenTorrentInstance>();
					
					for ( TableRowCore row: rows ){
					
						instances.add((OpenTorrentInstance)row.getDataSource());
					}
					
					selectInstances( instances );
					
					updateButtons();
				}
				
				public void mouseExit(TableRowCore row) {
				}
				
				public void mouseEnter(TableRowCore row) {
				}
				
				public void focusChanged(TableRowCore focus) {
				}
				
				public void 
				deselected(TableRowCore[] rows) 
				{
					selected( rows );
				}
			
				private void
				updateButtons()
				{
					Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								swt_updateTVTorrentButtons();
							}
						});
				}
				public void defaultSelected(TableRowCore[] rows, int stateMask) {
				}
				
			}, false);
	}
	
	private void
	addInstance(
		OpenTorrentInstance		instance )
	{
		open_instances.add( instance );
		
		updateDialogTitle();
		
		instance.initialize();
			
		tvTorrents.addDataSources( new OpenTorrentInstance[]{ instance });

		updateInstanceInfo();

		swt_updateTVTorrentButtons();
	}
	
	private void
	selectInstance(
		OpenTorrentInstance		instance )
	{
		List<OpenTorrentInstance>	instances = new ArrayList<OpenTorrentOptionsWindow.OpenTorrentInstance>();
		
		if ( instance != null ){
			
			instances.add( instance );
		}
		
		selectInstances( instances );
	}
	
	private void
	selectInstances(
		List<OpenTorrentInstance>		_instances )
	{
		if ( _instances.equals( selected_instances )){
			
			return;
		}
		
		final List<OpenTorrentInstance> instances = new ArrayList<OpenTorrentInstance>( _instances );
		
		Iterator<OpenTorrentInstance>	it = instances.iterator();
		
		while( it.hasNext()){
			
			if ( !open_instances.contains( it.next())){
				
				it.remove();
			}
		}
		
		if ( instances.size() == 0 ){
			
			if ( selected_instances.size() > 0 && open_instances.contains( selected_instances.get(0))){
				
				instances.add( selected_instances.get(0));
				
			}else if ( open_instances.size() > 0 ){
				
				instances.add( open_instances.get(0));
			}
		}
		
		selected_instances.clear();
			
		selected_instances.addAll( instances );
					
		Utils.execSWTThread(
			new Runnable()
			{
				public void
				run()
				{
					if ( multi_selection_instance != null ){
						
						multi_selection_instance.getComposite().dispose();
						
						multi_selection_instance = null;
					}
					
					if ( instances.size() == 1 ){
						
						OpenTorrentInstance first_instance = instances.get(0);
												
						expand_stack.topControl = first_instance.getComposite();
		
						expand_stack_area.layout(true);
						
						first_instance.layout();
						
					}else{
						Composite expand_area = new Composite( expand_stack_area, SWT.NULL );
						
						expand_area.setLayout( new FormLayout());

						List<TorrentOpenOptions> toos = new ArrayList<TorrentOpenOptions>();
								
						for ( OpenTorrentInstance oti: instances ){
							
							toos.add( oti.getOptions());
						}
						
						multi_selection_instance = new OpenTorrentInstance( expand_area, toos, optionListener );
						
						multi_selection_instance.initialize();
						
						expand_stack.topControl = multi_selection_instance.getComposite();
						
						expand_stack_area.layout(true);
						
						multi_selection_instance.layout();
					}
				}
			});
			
		List<TableRowCore> rows = new ArrayList<TableRowCore>();
		
		for ( OpenTorrentInstance instance: instances ){
		
			TableRowCore row = tvTorrents.getRow( instance );
		
			if ( row != null ){
			
				rows.add( row );
			}
		}
		
		tvTorrents.setSelectedRows( rows.toArray( new TableRowCore[rows.size()]));
	}
		
	private void
	removeInstance(
		OpenTorrentInstance		instance )
	{
		synchronized( active_windows ){
			
			active_windows.remove( instance.getHash());
		}
		
		int index = open_instances.indexOf( instance );
		
		open_instances.remove( instance );
		
		updateDialogTitle();
		
		tvTorrents.removeDataSource( instance );
				
		instance.getComposite().dispose();
		
		updateInstanceInfo();
		
		if ( selected_instances.contains( instance ) && selected_instances.size() > 1 ){
			
			List<OpenTorrentInstance> temp = new ArrayList<OpenTorrentOptionsWindow.OpenTorrentInstance>( selected_instances );
			
			temp.remove( instance );
		
			selectInstances( temp );
			
		}else{
			
			int	num_instances = open_instances.size();
			
			if ( num_instances > index ){
				
				selectInstance( open_instances.get( index ));
				
			}else if ( num_instances > 0 ){
				
				selectInstance( open_instances.get( num_instances-1 ));
				
			}else{
				
				selectInstance( null );
			}
		}
		
		swt_updateTVTorrentButtons();
		
		refreshTVTorrentIndexes();
		
		instance.dispose();
	}
	
	private void
	updateDialogTitle()
	{
		int num = open_instances.size();
		
		String text;
		
		if ( num == 1 ){
			
				// use a display name consistent with core
			
			TorrentOpenOptions options = open_instances.get(0).getOptions();

			text = options.getTorrentName();
			
			TOTorrent t = options.getTorrent();
			
			if ( t != null ){
			
				String str = PlatformTorrentUtils.getContentTitle( t );
				
				if ( str != null && str.length() > 0 ){
					
					text = str;
				}
			}
		}else{
			
			text =  MessageText.getString("label.num.torrents",new String[]{ String.valueOf( open_instances.size())});
		}
		
		dlg.setTitle(MessageText.getString("OpenTorrentOptions.title") + " [" + text + "]");
	}
	
	private void
	swt_updateTVTorrentButtons()
	{
		List<Object> selected = tvTorrents.getSelectedDataSources();
		
		buttonTorrentRemove.setEnabled( selected.size() > 0 );

		if ( selected.size() > 0 ){
			
			int	min_index 	= Integer.MAX_VALUE;
			int max_index	= -1;
			
			for ( Object obj: selected ){
			
				OpenTorrentInstance instance = (OpenTorrentInstance)obj;
			
				int index = instance.getIndex();
				
				min_index = Math.min( min_index, index );
				max_index = Math.max( max_index, index );
			}
			
			buttonTorrentUp.setEnabled( min_index > 0 );
			
			buttonTorrentDown.setEnabled( max_index < open_instances.size()-1);
			
		}else{
			
			buttonTorrentUp.setEnabled( false );
			
			buttonTorrentDown.setEnabled( false );
		}
	}
	
	private void
	refreshTVTorrentIndexes()
	{
		Utils.execSWTThreadLater(
				0,
				new Runnable()
				{
					public void
					run()
					{
						tvTorrents.columnInvalidate( "#" );
						
						tvTorrents.refreshTable( true );
					}
				});
	}
	
	private void
	updateInstanceInfo()
	{	
		if ( torrents_info_label == null ){
			
			return;
		}
		
		long	total_size		= 0;
		long	selected_size 	= 0;
		
		for ( OpenTorrentInstance instance: open_instances ){
			
			total_size		+= instance.getOptions().getTorrent().getSize();
			selected_size 	+= instance.getSelectedDataSize();
		}
		
		String	sel_str = DisplayFormatters.formatByteCountToKiBEtc(selected_size);
		String	tot_str = DisplayFormatters.formatByteCountToKiBEtc(total_size);
		
		
		String text;
		
		if ( sel_str.equals( tot_str )){
			
			text = MessageText.getString("label.n.will.be.downloaded", new String[] { tot_str	});

		}else{
			
			text = MessageText.getString("OpenTorrentWindow.filesInfo", new String[] { sel_str,	tot_str	});
		}
		
		torrents_info_label.setText( text );
	}
	
	public void
	updateUI()
	{
		if ( tvTorrents != null ){
			
			tvTorrents.refreshTable( false );
		}
		
		for( OpenTorrentInstance instance: open_instances ){
		
			instance.updateUI();
		}
		
		if ( multi_selection_instance != null ){
			
			multi_selection_instance.updateUI();
		}
	}
	
	public String getUpdateUIName() {
		return null;
	}

	private void
	swt_activate()
	{
		Shell shell = dlg.getShell();
		
		if ( !shell.isDisposed()){
		
			shell.forceActive();
		}
	}
	
	private Rectangle
	getBounds()
	{
		return( dlg.getShell().getBounds());
	}
	
	private Image
	loadImage(
		String		key )
	{
		 Image img = image_loader.getImage( key );
		 
		 if ( img != null ){
			 
			 images_to_dispose.add( key );
		 }
		 
		 return( img );
	}
	
	private void
	unloadImage(
		String	key )
	{
		image_loader.releaseImage( key );
	}
	
	protected void 
	dispose()
	{
		UIUpdaterSWT.getInstance().removeUpdater(this);
		
		for ( OpenTorrentInstance instance: open_instances ){
			
			instance.dispose();
		}
		
		for ( String key: images_to_dispose ){
			
			unloadImage( key );
		}
		
		images_to_dispose.clear();
		
		tvTorrents.delete();
	}
	
	protected class
	OpenTorrentInstance
		implements TableViewFilterCheck<TorrentOpenFileOptions>
	{
		final private HashWrapper						hash;
		final private TorrentOpenOptions 				torrentOptions;
		final private List<TorrentOpenOptions>			torrentOptionsMulti;
		final private OpenTorrentInstanceListener		changeListener;

		final private Composite	parent;
		final private Shell		shell;
		
		private SWTSkin skin;
		
		/* prevents loop of modifications */
		protected boolean bSkipDataDirModify = false;

		private Button btnCheckComments;
		private Button btnCheckAvailability;
		private Button btnSwarmIt;

		private Combo cmbDataDir;

		private Combo cmbQueueLocation;

		private Combo cmbStartMode;

		private StringList dirList;

		private volatile boolean diskFreeInfoRefreshPending = false;

		private volatile boolean diskFreeInfoRefreshRunning = false;

		private Composite diskspaceComp;

		private long	currentSelectedDataSize;

		private final Map fileStatCache = new WeakHashMap(20);

		private final Map parentToRootCache = new WeakHashMap(20);

		private SWTSkinObjectExpandItem soExpandItemFiles;

		private SWTSkinObjectExpandItem soExpandItemSaveTo;

		private SWTSkinObjectExpandItem soExpandItemTorrentInfo;

		private SWTSkinObjectText soFileAreaInfo;

		private TableViewSWT<TorrentOpenFileOptions> tvFiles;

		private SWTSkinObjectExpandItem soStartOptionsExpandItem;

		//private SWTSkinObjectExpandItem soExpandItemPeer;

		private AtomicInteger settingToDownload = new AtomicInteger(0);
		
		private Button btnSelectAll;
		private Button btnMarkSelected;
		private Button btnUnmarkSelected;
		private Button btnRename;
		private Button btnRetarget;
		private Composite tagButtonsArea;
		
		private 
		OpenTorrentInstance(
			HashWrapper						_hash,
			Composite						_parent,	
			TorrentOpenOptions				_torrentOptions,
			OpenTorrentInstanceListener		_changeListener )
		{
			hash				= _hash;
			parent				= _parent;
			torrentOptions 		= _torrentOptions;
			torrentOptionsMulti	= new ArrayList<TorrentOpenOptions>();
			changeListener		= _changeListener;

			torrentOptionsMulti.add( torrentOptions );
			
			shell = parent.getShell();

			torrentOptions.addListener(new TorrentOpenOptions.FileListener() {
				public void toDownloadChanged(TorrentOpenFileOptions fo, boolean toDownload) {
					TableRowCore row = tvFiles.getRow(fo);
					if (row != null) {
						row.invalidate(true);
						row.refresh(true);
					}
					if ( settingToDownload.get() == 0 ){
						updateFileButtons();
						updateSize();
					}
				}
				public void priorityChanged(TorrentOpenFileOptions fo, int priority) {
					TableRowCore row = tvFiles.getRow(fo);
					if (row != null) {
						row.invalidate(true);
						row.refresh(true);
					}
				}
				public void parentDirChanged(){
					if ( torrentOptions != null && cmbDataDir != null ){
						String toText = torrentOptions.getParentDir();
						String text = cmbDataDir.getText();
						
						if ( !text.equals( toText )){
							
							cmbDataDir.setText( toText );
						}
					}
				}
			});
			
			if ( TagManagerFactory.getTagManager().isEnabled()){

				try {
					RelatedContentManager rcm = RelatedContentManager.getSingleton();
					String[] networks = torrentOptions.getEnabledNetworks().keySet().toArray(new String[0]);
					rcm.lookupAttributes(hash.getBytes(), networks,
							new RelatedAttributeLookupListener() {

								public void lookupStart() {
								}

								public void tagFound(String tag, String network) {
									if (listDiscoveredTags.contains(tag)) {
										return;
									}
									if (checkAlreadyHaveTag(torrentOptions.getInitialTags(), tag)) {
										return;
									}
									listDiscoveredTags.add(tag);
									Utils.execSWTThread(new Runnable() {
										public void run() {
											if (tagButtonsArea == null
													|| tagButtonsArea.isDisposed()) {
												return;
											}
											buildTagButtonPanel(tagButtonsArea, true);
										}
									});

								}

								public void lookupComplete() {
								}

								public void lookupFailed(ContentException error) {
								}
							});
				} catch (ContentException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}				

		}
		
		private 
		OpenTorrentInstance(
			Composite						_parent,	
			List<TorrentOpenOptions>		_torrentOptionsMulti,
			OpenTorrentInstanceListener		_changeListener )
		{
			hash				= null;
			parent				= _parent;
			torrentOptions 		= null;
			torrentOptionsMulti = new ArrayList<TorrentOpenOptions>( _torrentOptionsMulti );
			changeListener		= _changeListener;

			shell = parent.getShell();
		}
		
		private HashWrapper
		getHash()
		{
			return( hash );
		}
		
		protected TorrentOpenOptions
		getOptions()
		{
			return( torrentOptions );
		}
		
		protected int
		getIndex()
		{
			return( open_instances.indexOf( this ));
		}
		
		protected Composite
		getComposite()
		{
			return( parent );
		}
		
		private void
		initialize()
		{						
			skin = SWTSkinFactory.getNonPersistentInstance(
						getClass().getClassLoader(), 
						"com/aelitis/azureus/ui/skin", "skin3_dlg_opentorrent_options_instance.properties" );			
		
			skin.initialize( parent, "expandview");

			if ( torrentOptions != null ){
				SWTSkinObject so = skin.getSkinObject("filearea-table");
				if (so instanceof SWTSkinObjectContainer) {
					setupTVFiles((SWTSkinObjectContainer) so, (SWTSkinObjectTextbox)skin.getSkinObject("filearea-filter"));
				}
		
				so = skin.getSkinObject("filearea-buttons");
				if (so instanceof SWTSkinObjectContainer) {
					setupFileAreaButtons((SWTSkinObjectContainer) so);
				}
			}
			
			SWTSkinObject so = skin.getSkinObject("disk-space");
			if (so instanceof SWTSkinObjectContainer) {
				diskspaceComp = (Composite) so.getControl();
				GridLayout gl = new GridLayout(2, false);
				gl.marginHeight = gl.marginWidth = 0;
				diskspaceComp.setLayout(gl);
				Label l = new Label(diskspaceComp, SWT.NONE);
				l.setText( "" );	// start with this to avoid UI re-layout from moving suff as user enters text etc
			}
		
			if ( torrentOptions != null ){
				so = skin.getSkinObject("filearea-info");
				if (so instanceof SWTSkinObjectText) {
					setupFileAreaInfo((SWTSkinObjectText) so);
				}
		
				so = skin.getSkinObject("start-options");
				if (so instanceof SWTSkinObjectExpandItem) {
					setupStartOptions((SWTSkinObjectExpandItem) so);
				}
		
				so = skin.getSkinObject("peer-sources");
				if (so instanceof SWTSkinObjectContainer) {
					setupPeerSourcesAndNetworkOptions((SWTSkinObjectContainer) so);
				}
		
				so = skin.getSkinObject("trackers");
				if (so instanceof SWTSkinObjectContainer) {
					setupTrackers((SWTSkinObjectContainer) so);
				}
				
				so = skin.getSkinObject("updownlimit");
				if (so instanceof SWTSkinObjectContainer) {
					setupUpDownLimitOption((SWTSkinObjectContainer) so);
				}
			
				so = skin.getSkinObject("ipfilter");
				if (so instanceof SWTSkinObjectContainer) {
					setupIPFilterOption((SWTSkinObjectContainer) so);
				}
			}
				
			SWTSkinObject so1 = skin.getSkinObject("saveto-textarea");
			SWTSkinObject so2 = skin.getSkinObject("saveto-browse");
			if ((so1 instanceof SWTSkinObjectContainer)
					&& (so2 instanceof SWTSkinObjectButton)) {
				setupSaveLocation((SWTSkinObjectContainer) so1, (SWTSkinObjectButton) so2);
			}
	
			so = skin.getSkinObject("expanditem-saveto");
			if (so instanceof SWTSkinObjectExpandItem) {
				soExpandItemSaveTo = (SWTSkinObjectExpandItem) so;
			}
			if ( torrentOptions != null ){
		
				so = skin.getSkinObject("expanditem-files");
				if (so instanceof SWTSkinObjectExpandItem) {
					soExpandItemFiles = (SWTSkinObjectExpandItem) so;
				}
		
				/*
				so = skin.getSkinObject("expanditem-peer");
				if (so instanceof SWTSkinObjectExpandItem) {
					soExpandItemPeer = (SWTSkinObjectExpandItem) so;
				}
				*/
				
				so = skin.getSkinObject("expanditem-torrentinfo");
				if (so instanceof SWTSkinObjectExpandItem) {
					soExpandItemTorrentInfo = (SWTSkinObjectExpandItem) so;
					soExpandItemTorrentInfo.setText(MessageText.getString("OpenTorrentOptions.header.torrentinfo")
							+ ": " + torrentOptions.getTorrentName());
				}
					
				setupInfoFields(skin);
		
				updateStartOptionsHeader();
				cmbDataDirChanged();
				updateSize();
			}else{
				
				cmbDataDirChanged();
			}
				
			skin.layout();
		}
		
		private void
		layout()
		{
			SWTSkinObjectExpandItem so = (SWTSkinObjectExpandItem)skin.getSkinObject("expanditem-files");
				
			SWTSkinObjectExpandBar bar = (SWTSkinObjectExpandBar)so.getParent();
			
			bar.relayout();
			
			for ( SWTSkinObjectExpandItem item: bar.getChildren()){
				
				item.relayout();
			}
		}
		
		private void
		showAvailability()
		{
			final Shell avail_shell = ShellFactory.createShell( shell, SWT.DIALOG_TRIM | SWT.RESIZE );

			Utils.setShellIcon(avail_shell);
			 
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			avail_shell.setLayout(layout);
			
			Utils.verifyShellRect(avail_shell, true);

			TOTorrent t = torrentOptions.getTorrent();

			final TrackerAvailView view = new TrackerAvailView();
			
			String[] enabled_peer_sources = PEPeerSource.PS_SOURCES;
			
			if (torrentOptions.peerSource != null) {
				List<String>	temp = new ArrayList<String>(Arrays.asList(enabled_peer_sources));
				for (String peerSource : torrentOptions.peerSource.keySet()) {
					boolean enable = torrentOptions.peerSource.get(peerSource);
					if ( !enable ){
						temp.remove( peerSource );
					}
				}
				enabled_peer_sources = temp.toArray( new String[temp.size()]);
			}

			String[] enabled_networks = AENetworkClassifier.AT_NETWORKS;
			
			Map<String,Boolean> enabledNetworks = torrentOptions.getEnabledNetworks();
			
			if ( enabledNetworks != null ){
				List<String>	temp = new ArrayList<String>(Arrays.asList(enabled_networks));
				for (String net : enabledNetworks.keySet()) {
					boolean enable = enabledNetworks.get(net);
					if ( !enable ){
						temp.remove( net );
					}
				}
				enabled_networks = temp.toArray( new String[temp.size()]);
			}
					
			final DownloadManagerAvailability availability = 
				DownloadManagerFactory.getAvailability( 
					t, 
					torrentOptions.getTrackers( true ), 
					enabled_peer_sources,
					enabled_networks );
			
			view.dataSourceChanged( availability );

			Composite comp = new Composite( avail_shell, SWT.NULL );
			GridData gridData = new GridData( GridData.FILL_BOTH );
			comp.setLayoutData(gridData);
			
			layout = new GridLayout();
			layout.numColumns = 1;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			comp.setLayout(layout);
			
			view.initialize(comp);

			view.viewActivated();
			view.refresh();

			final UIUpdatable viewUpdater = new UIUpdatable() {
				public void updateUI() {
					view.refresh();
				}

				public String getUpdateUIName() {
					return view.getFullTitle();
				}
			};
			
			UIUpdaterSWT.getInstance().addUpdater(viewUpdater);

				// progress
			
			Composite progressComp = new Composite( comp, SWT.NULL );
			gridData = new GridData( GridData.FILL_HORIZONTAL );
			progressComp.setLayoutData(gridData);
			
			layout = new GridLayout();
			layout.numColumns = 2;
			progressComp.setLayout(layout);
			
			Label progLabel = new Label(progressComp,SWT.NULL);
			progLabel.setText( MessageText.getString("label.checking.sources"));
			
			final Composite progBarComp = new Composite( progressComp, SWT.NULL );
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			//gridData.widthHint = 400;
			progBarComp.setLayoutData(gridData);
			
			//Label padLabel = new Label(progressComp,SWT.NULL);
			//gridData = new GridData(GridData.FILL_HORIZONTAL);
			//padLabel.setLayoutData(gridData);
			
			final StackLayout	progStackLayout = new StackLayout();
			
			progBarComp.setLayout( progStackLayout);
			
			final ProgressBar progBarIndeterminate = new ProgressBar(progBarComp, SWT.HORIZONTAL | SWT.INDETERMINATE);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			progBarIndeterminate.setLayoutData(gridData);
			
			final ProgressBar progBarComplete = new ProgressBar(progBarComp, SWT.HORIZONTAL );
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			progBarComplete.setLayoutData(gridData);
			progBarComplete.setMaximum( 1 );
			progBarComplete.setSelection( 1 );
			
			progStackLayout.topControl = progBarIndeterminate;
			
			new AEThread2( "ProgChecker" )
			{
				public void
				run()
				{
					boolean	currently_updating = true;
					
					while( true ){
						
						if ( avail_shell.isDisposed()){
							
							return;
						}
						
						final boolean	updating = view.isUpdating();
						
						if ( updating != currently_updating ){
							
							currently_updating = updating;
							
							Utils.execSWTThread(
								new Runnable()
								{
									public void
									run()
									{
										if ( !avail_shell.isDisposed()){
										
											progStackLayout.topControl = updating?progBarIndeterminate:progBarComplete;
										
											progBarComp.layout();
										}
									}
								});
						}
						
						try{
							Thread.sleep(500);
							
						}catch( Throwable e ){
							
						}
					}
				}
			}.start();
			
				// line
			
			Label labelSeparator = new Label( comp, SWT.SEPARATOR | SWT.HORIZONTAL);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			labelSeparator.setLayoutData(gridData);
			
				// buttons
			
			Composite buttonComp = new Composite( comp, SWT.NULL );
			gridData = new GridData( GridData.FILL_HORIZONTAL );
			buttonComp.setLayoutData(gridData);
			
			layout = new GridLayout();
			layout.numColumns = 2;
			buttonComp.setLayout(layout);
			
			new Label(buttonComp,SWT.NULL);
			
			Composite buttonArea = new Composite(buttonComp,SWT.NULL);
			gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
			gridData.grabExcessHorizontalSpace = true;
			buttonArea.setLayoutData(gridData);
			GridLayout layoutButtons = new GridLayout();
			layoutButtons.numColumns = 1;
			buttonArea.setLayout(layoutButtons);
			
			List<Button>	buttons = new ArrayList<Button>();
			
			Button bOK = new Button(buttonArea,SWT.PUSH);
			buttons.add( bOK );
			
			bOK.setText(MessageText.getString("Button.ok"));
			gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
			gridData.grabExcessHorizontalSpace = true;
			gridData.widthHint = 70;
			bOK.setLayoutData(gridData);
			bOK.addListener(SWT.Selection,new Listener() {
				public void handleEvent(Event e) {
					avail_shell.dispose();
				}
			});
			
			Utils.makeButtonsEqualWidth( buttons );
			
			avail_shell.setDefaultButton( bOK );
			
			avail_shell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					try{
						UIUpdaterSWT.getInstance().removeUpdater(viewUpdater);
						
						if ( !btnCheckAvailability.isDisposed()){
						
							btnCheckAvailability.setEnabled( true );
						}
					}finally{
						
						availability.destroy();
					}
				}
			});

			btnCheckAvailability.setEnabled( false );
			
			avail_shell.setSize( 800, 400 );
			avail_shell.layout(true, true);
			
			Utils.centerWindowRelativeTo(avail_shell,shell);
			
			String title = torrentOptions.getTorrentName();
						
			if ( t != null ){
			
				String str = PlatformTorrentUtils.getContentTitle( t );
				
				if ( str != null && str.length() > 0 ){
					
					title = str;
				}
			}
			
			Messages.setLanguageText( avail_shell, "torrent.avail.title", new String[]{ title });

			avail_shell.open();
		}
		
		private void
		showComments()
		{
			final Shell comments_shell = ShellFactory.createShell( shell, SWT.DIALOG_TRIM | SWT.RESIZE );

			Utils.setShellIcon(comments_shell);
			 
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			comments_shell.setLayout(layout);
			
			Utils.verifyShellRect(comments_shell, true);

			TOTorrent torrent = torrentOptions.getTorrent();
			
			String[] enabled_networks = AENetworkClassifier.AT_NETWORKS;
			
			Map<String,Boolean> enabledNetworks = torrentOptions.getEnabledNetworks();
			
			if ( enabledNetworks != null ){
				List<String>	temp = new ArrayList<String>(Arrays.asList(enabled_networks));
				for (String net : enabledNetworks.keySet()) {
					boolean enable = enabledNetworks.get(net);
					if ( !enable ){
						temp.remove( net );
					}
				}
				enabled_networks = temp.toArray( new String[temp.size()]);
			}
					
			Composite comp = new Composite( comments_shell, SWT.NULL );
			
			GridData gridData = new GridData( GridData.FILL_BOTH );
			comp.setLayoutData(gridData);
			
			layout = new GridLayout();
			layout.numColumns = 1;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			comp.setLayout(layout);
			
			Composite topComp = new Composite( comp, SWT.NULL );
			layout = new GridLayout();
			layout.numColumns = 1;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			topComp.setLayout(layout);
			gridData = new GridData( GridData.FILL_BOTH );
			topComp.setLayoutData(gridData);
			
			String active_networks_str = "";
			
			for ( String net: enabled_networks ){
				
				active_networks_str += 
						(active_networks_str.length()==0?"":", ") + 
						MessageText.getString( "ConfigView.section.connection.networks." + net );
			}
			
			if ( active_networks_str.length() == 0 ){
				
				active_networks_str = MessageText.getString( "PeersView.uniquepiece.none" );
			}
			
			Label info_label = new Label( topComp, SWT.WRAP );
			info_label.setText( MessageText.getString( "torrent.comments.info", new String[]{active_networks_str}));
			gridData = new GridData( GridData.FILL_HORIZONTAL );
			gridData.horizontalIndent = 8;
			gridData.verticalIndent = 8;
			info_label.setLayoutData(gridData);
			
				// azrating plugin
			
			Group ratingComp = new Group( topComp, SWT.NULL );
			layout = new GridLayout();
			layout.numColumns = 1;
			
			ratingComp.setLayout(layout);
			gridData = new GridData( GridData.FILL_HORIZONTAL );
			ratingComp.setLayoutData(gridData);
			
			ratingComp.setText( "Rating Plugin" );
			
			Composite ratingComp2 = new Composite( ratingComp, SWT.BORDER );
			layout = new GridLayout();
			layout.numColumns = 1;
			layout.marginWidth = 4;
			layout.marginHeight = 4;
			ratingComp2.setLayout(layout);
			gridData = new GridData( GridData.FILL_BOTH );
			ratingComp2.setLayoutData(gridData);
			ratingComp2.setBackground( Colors.white );
			
			final Label ratingText = new Label( ratingComp2, SWT.WRAP );
			gridData = new GridData( GridData.FILL_HORIZONTAL );
			gridData.heightHint=ratingText.getFont().getFontData()[0].getHeight() * 2 + 16;
			ratingText.setLayoutData(gridData);
			ratingText.setBackground( Colors.white );
			
			final boolean[]	az_rating_in_progress = { false };
			
			try{
				PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
				
				PluginInterface rating_pi = pm.getPluginInterfaceByID( "azrating" );
					
				if ( rating_pi != null ){
					
					final IPCInterface ipc = rating_pi.getIPC();
					
					if ( ipc.canInvoke( "lookupRatingByHash", new Object[]{ new String[0], new byte[0] })){
							
						final String[] f_enabled_networks = enabled_networks;
						
						az_rating_in_progress[0] = true;
						
						ratingText.setText( MessageText.getString( "label.searching" ));
								
						new AEThread2( "oto:rat" )
						{
							public void
							run()
							{	Map result = null;
							
								try{
									result = (Map)ipc.invoke( "lookupRatingByHash", new Object[]{ f_enabled_networks, hash.getBytes() });
																			
								}catch( Throwable e ){
									
									e.printStackTrace();
									
								}finally{
									
									synchronized( az_rating_in_progress ){
									
										az_rating_in_progress[0] = false;
									}
									
									if ( !ratingText.isDisposed()){
									
										final Map f_result = result;
										
										Utils.execSWTThread(
											new Runnable()
											{
												public void
												run()
												{
													if ( !ratingText.isDisposed()){
														
														String text 	= "";
														String	tooltip = "";
														
														if ( f_result != null ){
															
															List<Map> ratings = (List<Map>)f_result.get( "ratings" );
															
															if ( ratings != null ){
																
																String 			scores_str = "";
																List<String>	comments = new ArrayList<String>();
																
																double	total_score = 0;
																int		score_num	= 0;
																
																for ( Map map: ratings ){
																	
																	try{
																		int 	score	= ((Number)map.get( "score" )).intValue();
																		
																		total_score += score;
																		score_num++;
																		
																		scores_str += (scores_str.length()==0?"":", ") + score;
																		
																		String comment 	= ImportExportUtils.importString(map, "comment" );
																		
																		if ( comment != null ){
																			
																			comment = comment.trim();
																			
																			if ( comment.length() > 0 ){
																				
																				comments.add( comment );
																			}
																		}
																	}catch( Throwable e ){
																		
																	}
																}
																
																if ( score_num > 0 ){
																	
																	double average = total_score/score_num;
																	
																	text = MessageText.getString( 
																				"torrent.comment.rat1", 
																				new String[]{
																					DisplayFormatters.formatDecimal(average,1),
																					scores_str
																				});
																	
																	int num_comments = comments.size();
																	
																	if ( num_comments > 0 ){
																		
																		text += "\n    " + MessageText.getString( 
																				"torrent.comment.rat2", 
																				new String[]{
																					comments.get(0) + (num_comments==1?"":"..." )
																				});
																																					
																		for ( String comment: comments ){
																				
																			tooltip += (tooltip.length()==0?"":"\n") + comment;
																		}
																	}
																}
															}
															
															
														}
														
														if ( text.length()==0 ){
															
															text = MessageText.getString( "PeersView.uniquepiece.none" );
														}
														
														ratingText.setText( text );
														ratingText.setToolTipText( tooltip );
													}
												}
											});
									}
								}
							}
						}.start();
						
					}else{
						
						ratingText.setText( "Rating Plugin needs updating" );
					}
				}else{
					
					ratingText.setText( MessageText.getString( "torrent.comment.azrating.install" ));
				}
			}catch( Throwable e ){
				
				ratingText.setText( "Rating Plugin failed: " + Debug.getNestedExceptionMessage(e));
			}
			
				// chat
			
			Group chatComp = new Group( topComp, SWT.NULL );
			layout = new GridLayout();
			layout.numColumns = 1;
			chatComp.setLayout(layout);
			gridData = new GridData( GridData.FILL_HORIZONTAL );
			chatComp.setLayoutData(gridData);
			
			chatComp.setText( "Chat Plugin" );
			
			Map<String,Object>	chat_properties = new HashMap<String, Object>();
			
			chat_properties.put( BuddyPluginViewInterface.VP_SWT_COMPOSITE, chatComp );
			
			final String[]f_enabled_networks = enabled_networks;
			
			final String  chat_key = BuddyPluginUtils.getChatKey( torrent );
			
			BuddyPluginViewInterface.DownloadAdapter 
				adapter = new BuddyPluginViewInterface.DownloadAdapter(){
					
					public String[] 
					getNetworks() 
					{
						return( f_enabled_networks );
					}
					
					public DownloadManager 
					getCoreDownload() 
					{
						return( null );
					}
					
					public String 
					getChatKey() 
					{
						return( chat_key );
					}
				};
				
			chat_properties.put( BuddyPluginViewInterface.VP_DOWNLOAD, adapter );

			final Set<ChatInstance>	activated_chats = new HashSet<ChatInstance>();
			
			final BuddyPluginViewInterface.View chat_view = 
				BuddyPluginUtils.buildChatView( 
					chat_properties,
					new BuddyPluginViewInterface.ViewListener() {
						
						public void 
						chatActivated(
							ChatInstance chat) 
						{
							synchronized( az_rating_in_progress ){
								
								activated_chats.add( chat );
							}
						}
					});
				
			if ( chat_view == null ){
				
				Composite chatComp2 = new Composite( chatComp, SWT.BORDER );
				layout = new GridLayout();
				layout.numColumns = 1;
				layout.marginWidth = 4;
				layout.marginHeight = 4;
				chatComp2.setLayout(layout);
				gridData = new GridData( GridData.FILL_BOTH );
				chatComp2.setLayoutData(gridData);
				chatComp2.setBackground( Colors.white );
				
				final Label chatText = new Label( chatComp2, SWT.WRAP );
				gridData = new GridData( GridData.FILL_HORIZONTAL );
				gridData.heightHint=ratingText.getFont().getFontData()[0].getHeight() * 2 + 16;
				chatText.setLayoutData(gridData);
				chatText.setBackground( Colors.white );

				chatText.setText( MessageText.getString( "torrent.comment.azmsgsync.install" ));
			}
			
				// progress
			
			Composite progressComp = new Composite( comp, SWT.NULL );
			gridData = new GridData( GridData.FILL_HORIZONTAL );
			progressComp.setLayoutData(gridData);
			
			layout = new GridLayout();
			layout.numColumns = 2;
			progressComp.setLayout(layout);
			
			Label progLabel = new Label(progressComp,SWT.NULL);
			progLabel.setText( MessageText.getString("label.checking.comments"));
			
			final Composite progBarComp = new Composite( progressComp, SWT.NULL );
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			//gridData.widthHint = 300;
			progBarComp.setLayoutData(gridData);
			
			//Label padLabel = new Label(progressComp,SWT.NULL);
			//gridData = new GridData(GridData.FILL_HORIZONTAL);
			//padLabel.setLayoutData(gridData);
			
			final StackLayout	progStackLayout = new StackLayout();
			
			progBarComp.setLayout( progStackLayout);
			
			final ProgressBar progBarIndeterminate = new ProgressBar(progBarComp, SWT.HORIZONTAL | SWT.INDETERMINATE);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			progBarIndeterminate.setLayoutData(gridData);
			
			final ProgressBar progBarComplete = new ProgressBar(progBarComp, SWT.HORIZONTAL );
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			progBarComplete.setLayoutData(gridData);
			progBarComplete.setMaximum( 1 );
			progBarComplete.setSelection( 1 );
			
			progStackLayout.topControl = progBarIndeterminate;
			
			new AEThread2( "ProgChecker" )
			{
				public void
				run()
				{
					boolean	currently_updating = true;
					
					while( true ){
						
						if ( comments_shell.isDisposed()){
							
							return;
						}
						
						boolean in_progress = false;
						
						synchronized( az_rating_in_progress ){
						
							if ( az_rating_in_progress[0] ){
								
								in_progress = true;
							}
							
							for ( ChatInstance inst: activated_chats ){
								
								if ( inst.getIncomingSyncState() != 0 ){
									
									in_progress = true;
								}
							}
						}
						
						final boolean	updating = in_progress;
						
						if ( updating != currently_updating ){
							
							currently_updating = updating;
							
							Utils.execSWTThread(
								new Runnable()
								{
									public void
									run()
									{
										if ( !comments_shell.isDisposed()){
										
											progStackLayout.topControl = updating?progBarIndeterminate:progBarComplete;
										
											progBarComp.layout();
										}
									}
								});
						}
						
						try{
							Thread.sleep(500);
							
						}catch( Throwable e ){
							
						}
					}
				}
			}.start();
			
				// line
			
			Label labelSeparator = new Label( comp, SWT.SEPARATOR | SWT.HORIZONTAL);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			labelSeparator.setLayoutData(gridData);
			
				// buttons
			
			Composite buttonComp = new Composite( comp, SWT.NULL );
			gridData = new GridData( GridData.FILL_HORIZONTAL );
			buttonComp.setLayoutData(gridData);
			
			layout = new GridLayout();
			layout.numColumns = 2;
			buttonComp.setLayout(layout);
			
			new Label(buttonComp,SWT.NULL);
			
			Composite buttonArea = new Composite(buttonComp,SWT.NULL);
			gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
			gridData.grabExcessHorizontalSpace = true;
			buttonArea.setLayoutData(gridData);
			GridLayout layoutButtons = new GridLayout();
			layoutButtons.numColumns = 1;
			buttonArea.setLayout(layoutButtons);
			
			List<Button>	buttons = new ArrayList<Button>();
			
			Button bOK = new Button(buttonArea,SWT.PUSH);
			buttons.add( bOK );
			
			bOK.setText(MessageText.getString("Button.ok"));
			gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
			gridData.grabExcessHorizontalSpace = true;
			gridData.widthHint = 70;
			bOK.setLayoutData(gridData);
			bOK.addListener(SWT.Selection,new Listener() {
				public void handleEvent(Event e) {
					comments_shell.dispose();
				}
			});
			
			Utils.makeButtonsEqualWidth( buttons );
			
			comments_shell.setDefaultButton( bOK );
			
			comments_shell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					try{
						if ( !btnCheckComments.isDisposed()){
						
							btnCheckComments.setEnabled( true );
						}
						
						if ( chat_view != null ){
							
							chat_view.destroy();
						}
					}finally{
						
						
					}
				}
			});

			btnCheckComments.setEnabled( false );
			
			comments_shell.setSize( 600, 600 );
			comments_shell.layout(true, true);
			
			Utils.centerWindowRelativeTo(comments_shell,shell);
			
			String title = torrentOptions.getTorrentName();
						
			if ( torrent != null ){
			
				String str = PlatformTorrentUtils.getContentTitle( torrent );
				
				if ( str != null && str.length() > 0 ){
					
					title = str;
				}
			}
			
			Messages.setLanguageText( comments_shell, "torrent.comments.title", new String[]{ title });

			comments_shell.open();
		}
		
		
		
		
		private void checkSeedingMode() {
			if ( torrentOptions == null ){
				return;
			}
			
			// Check for seeding
			boolean bTorrentValid = true;
	
			if (torrentOptions.iStartID == TorrentOpenOptions.STARTMODE_SEEDING) {
				// check if all selected files exist
				TorrentOpenFileOptions[] files = torrentOptions.getFiles();
				for (int j = 0; j < files.length; j++) {
					TorrentOpenFileOptions fileInfo = files[j];
					if (!fileInfo.isToDownload())
						continue;
	
					File file = fileInfo.getInitialLink();
	
					if (file == null) {
	
						file = fileInfo.getDestFileFullName();
					}
	
					if (!file.exists()) {
						fileInfo.isValid = false;
						bTorrentValid = false;
					} else if (!fileInfo.isValid) {
						fileInfo.isValid = true;
					}
				}
			}
	
			torrentOptions.isValid = bTorrentValid;
		}
	
		protected void cmbDataDirChanged() {
	
			if (bSkipDataDirModify || cmbDataDir == null) {
				return;
			}
			String dirText = cmbDataDir.getText();
			
			for ( TorrentOpenOptions too: torrentOptionsMulti ){
				too.setParentDir( dirText);
			}
			
			checkSeedingMode();
	
			if (!Utils.isCocoa || SWT.getVersion() > 3600) { // See Eclipse Bug 292449
				File file = new File( dirText );
				if (!file.isDirectory()) {
					cmbDataDir.setBackground(Colors.colorErrorBG);
					// make the error state visible
					soExpandItemSaveTo.setExpanded(true);
				} else {
					cmbDataDir.setBackground(null);
				}
				cmbDataDir.redraw();
				cmbDataDir.update();
			}
	
			if (soExpandItemSaveTo != null) {
				String s = MessageText.getString("OpenTorrentOptions.header.saveto",
						new String[] { dirText });
				soExpandItemSaveTo.setText(s);
			}
			diskFreeInfoRefreshPending = true;
		}
	
		private long getCachedDirFreeSpace(File directory) {
			FileStatsCacheItem item = (FileStatsCacheItem) fileStatCache.get(directory);
			if (item == null)
				fileStatCache.put(directory, item = new FileStatsCacheItem(directory));
			return item.freeSpace;
		}
	
		private boolean getCachedExistsStat(File directory) {
			FileStatsCacheItem item = (FileStatsCacheItem) fileStatCache.get(directory);
			if (item == null)
				fileStatCache.put(directory, item = new FileStatsCacheItem(directory));
			return item.exists;
		}
	
		protected void setSelectedQueueLocation(int iLocation) {
			torrentOptions.iQueueLocation = iLocation;
	
			updateStartOptionsHeader();
		}
	
		private void updateStartOptionsHeader() {
			if (soStartOptionsExpandItem == null) {
				return;
			}
	
			String optionText = MessageText.getString("OpenTorrentWindow.startMode."
					+ startModes[torrentOptions.iStartID])
					+ ", "
					+ MessageText.getString("OpenTorrentWindow.addPosition."
							+ queueLocations[torrentOptions.iQueueLocation]);
	
			String s = MessageText.getString("OpenTorrentOptions.header.startoptions",
					new String[] {
						optionText
					});
			
			List<Tag> initialtags = torrentOptions.getInitialTags();
			
			String tag_str = null;
			int numTags = 0;
			
			if ( initialtags.size() > 0 ){
				
				tag_str = "";
				
				for ( Tag t: initialtags ){
					numTags++;
					tag_str += (tag_str==""?"":", ") + t.getTagName( true );
				}
			}


			String[] tagsToCreate = listTagsToCreate.toArray(new String[0]);
			for (String tagName : tagsToCreate) {
				numTags++;
				if (tag_str == null) {
					tag_str = tagName;
				} else {
					tag_str += (tag_str==""?"":", ") + tagName;
				}
			}
			
			if (numTags == 0) {
				tag_str = MessageText.getString( "label.none" );
			}

			
			s += "        " + MessageText.getString( "OpenTorrentOptions.header.tags", new String[]{ tag_str });
			
			soStartOptionsExpandItem.setText(s);
		}
	
		protected void setSelectedStartMode(int iStartID) {
			torrentOptions.iStartID = iStartID;
	
			checkSeedingMode();
			updateStartOptionsHeader();
		}
	
		private void setupFileAreaButtons(SWTSkinObjectContainer so) {
			
			PluginInterface	swarm_pi = null;
			
			try{
				if (COConfigurationManager.getBooleanParameter("rcm.overall.enabled",
						true) && AzureusCoreFactory.isCoreRunning()) {
					final PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
							"aercm");
	
					if (pi != null && pi.getPluginState().isOperational()
							&& pi.getIPC().canInvoke("lookupBySize", new Object[] {
								new Long(0)
							})){
						
						swarm_pi = pi;
					}
				}
			}catch( Throwable e ){
					
			}
			
			Composite cButtonsArea = so.getComposite();
			GridLayout layout = new GridLayout(1,false);
			layout.marginWidth = layout.marginHeight = layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
			cButtonsArea.setLayout(layout);

			Composite cButtonsTop = new Composite(cButtonsArea, SWT.NULL);
			layout = new GridLayout(swarm_pi==null?6:7,false);
			layout.marginWidth = layout.marginHeight = layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
			cButtonsTop.setLayout(layout);
			GridData gridData = new GridData( GridData.FILL_HORIZONTAL);
			cButtonsTop.setLayoutData( gridData );

			
			Canvas line = new Canvas(cButtonsArea,SWT.NO_BACKGROUND);
			line.addListener(SWT.Paint, new Listener() {
				public void handleEvent(Event e) {
					Rectangle clientArea = ((Canvas) e.widget).getClientArea();
					e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
					e.gc.drawRectangle(clientArea);
					clientArea.y++;
					e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
					e.gc.drawRectangle(clientArea);
				}
			});
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.heightHint = 2;
			line.setLayoutData(gridData);

			Composite cButtonsBottom = new Composite(cButtonsArea, SWT.NULL);
			layout = new GridLayout(3,false);
			layout.marginWidth = layout.marginHeight = layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
			cButtonsBottom.setLayout(layout);
			gridData = new GridData( GridData.FILL_HORIZONTAL);
			cButtonsBottom.setLayoutData( gridData );
			
			List<Button>	buttons = new ArrayList<Button>();
			
			btnSelectAll = new Button(cButtonsTop, SWT.PUSH);
			buttons.add( btnSelectAll );
			Messages.setLanguageText(btnSelectAll, "Button.selectAll");
			btnSelectAll.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					tvFiles.selectAll();
				}
			});
			
			btnMarkSelected = new Button(cButtonsTop, SWT.PUSH);
			buttons.add( btnMarkSelected );
			Messages.setLanguageText(btnMarkSelected, "Button.mark");
			btnMarkSelected.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(new TorrentOpenFileOptions[0]);
					setToDownload( infos, true );
				}
			});
			
			btnUnmarkSelected = new Button(cButtonsTop, SWT.PUSH);
			buttons.add( btnUnmarkSelected );
			Messages.setLanguageText(btnUnmarkSelected, "Button.unmark");
			btnUnmarkSelected.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(new TorrentOpenFileOptions[0]);
					setToDownload( infos, false );
	
				}
			});
			
			btnRename = new Button(cButtonsTop, SWT.PUSH);
			buttons.add( btnRename );
			Messages.setLanguageText(btnRename, "Button.rename");
			btnRename.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(
							new TorrentOpenFileOptions[0]);
					renameFilenames(infos);
				}
			});
	
			btnRetarget = new Button(cButtonsTop, SWT.PUSH);
			buttons.add( btnRetarget );
			Messages.setLanguageText(btnRetarget, "Button.retarget");
			btnRetarget.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(
							new TorrentOpenFileOptions[0]);
					changeFileDestination(infos, false );
				}
			});
			
			Label pad1 = new Label(cButtonsTop, SWT.NONE);
			gridData = new GridData( GridData.FILL_HORIZONTAL);
			pad1.setLayoutData( gridData );
			
			// swarm-it button
			
			if ( swarm_pi != null ){
				final PluginInterface f_pi = swarm_pi;
				
				btnSwarmIt = new Button(cButtonsTop, SWT.PUSH);
				buttons.add( btnSwarmIt );
				Messages.setLanguageText(btnSwarmIt, "Button.swarmit");
	
				btnSwarmIt.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						List<Object> selectedDataSources = tvFiles.getSelectedDataSources();
						for (Object ds : selectedDataSources) {
							TorrentOpenFileOptions file = (TorrentOpenFileOptions) ds;
	
							try {
								f_pi.getIPC().invoke("lookupBySize", new Object[] {
									new Long(file.lSize)
								});
	
							} catch (Throwable e) {
	
								Debug.out(e);
							}
							break;
						}
					}
				});
	
				btnSwarmIt.setEnabled(false);
			}
		
			
			Label pad2 = new Label(cButtonsBottom, SWT.NONE);
			gridData = new GridData( GridData.FILL_HORIZONTAL);
			pad2.setLayoutData( gridData );

				// ratings etc
			
			btnCheckComments = new Button(cButtonsBottom, SWT.PUSH);
			buttons.add( btnCheckComments );
			Messages.setLanguageText(btnCheckComments, "label.comments");
			
			btnCheckComments.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					showComments();
				}
			});
				// availability button
			
			btnCheckAvailability = new Button(cButtonsBottom, SWT.PUSH);
			buttons.add( btnCheckAvailability );
			Messages.setLanguageText(btnCheckAvailability, "label.check.avail");

			btnCheckAvailability.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					showAvailability();
				}
			});
			
			

	
			Utils.makeButtonsEqualWidth( buttons );
			
			updateFileButtons();
	
		}
	
		private void
		setToDownload(
			TorrentOpenFileOptions[]	infos,
			boolean						download )	
		{
			boolean changed = false;
			try{
				settingToDownload.incrementAndGet();
			
				for (TorrentOpenFileOptions info: infos ){
					
					if ( info.isToDownload() != download ){
						
						info.setToDownload( download );
						
						changed = true;
					}
				}
			}finally{
				
				settingToDownload.decrementAndGet();
			}
			
			if ( changed ){
				updateFileButtons();
				updateSize();
			}
		}
		
		private void setupFileAreaInfo(SWTSkinObjectText so) {
			soFileAreaInfo = so;
		}
	
		private void setupSaveLocation(SWTSkinObjectContainer soInputArea,
				SWTSkinObjectButton soBrowseButton) {
			cmbDataDir = new Combo(soInputArea.getComposite(), SWT.NONE);
			cmbDataDir.setLayoutData(Utils.getFilledFormData());
	
			cmbDataDir.addKeyListener(
				new KeyListener(){					
					public void keyReleased(KeyEvent e) {
						if ( e.keyCode == SWT.SPACE && ( e.stateMask & SWT.MODIFIER_MASK ) != 0 ){
							
							e.doit = false;
						}
					}
					
					public void keyPressed(KeyEvent e) {
												
						if ( e.keyCode == SWT.SPACE && ( e.stateMask & SWT.MODIFIER_MASK ) != 0 ){
						
							e.doit = false;
							
							Menu menu = cmbDataDir.getMenu();
							
							if ( menu != null && !menu.isDisposed()){
								
								menu.dispose();
							}
							
							menu = new Menu( cmbDataDir );
														
							String current_text = cmbDataDir.getText();
							
							String def = COConfigurationManager.getStringParameter(PARAM_DEFSAVEPATH);
							
							List<String> items = new ArrayList<String>( Arrays.asList( cmbDataDir.getItems()));
							
							if ( !items.contains( def )){
								
								items.add( def );
							}
							
							List<String>	suggestions = new ArrayList<String>();
							
							for ( String item: items ){
								
								if ( item.toLowerCase(Locale.US).contains( current_text.toLowerCase( Locale.US ))){
									
									suggestions.add( item );
								}
							}
						
							if ( suggestions.size() == 0 ){
							
								MenuItem mi = new MenuItem( menu, SWT.PUSH );
								
								mi.setText( MessageText.getString( "label.no.suggestions" ));
								
								mi.setEnabled( false );
								
							}else{
								
								//Collections.sort( suggestions );
								
								for ( final String str: suggestions ){
									
									MenuItem mi = new MenuItem( menu, SWT.PUSH );
									
									mi.setText( str );
									
									mi.addSelectionListener(
										new SelectionAdapter() {
											
											public void 
											widgetSelected(SelectionEvent e) {
												
												cmbDataDir.setText( str );
											}
										});
								}
							}
							
							cmbDataDir.setMenu( menu );
							
							final Point cursorLocation = Display.getCurrent().getCursorLocation();
														
							menu.setLocation( cursorLocation.x-10, cursorLocation.y-10 );
							
							menu.setVisible( true );
							
							Utils.execSWTThread(
								new Runnable() {
									
									public void run() {
										// need to do this to get the menu item selected correctly
										Display.getCurrent().setCursorLocation(cursorLocation.x+1,cursorLocation.y);
									}
								}, true );
						}
					}
				});
			
			cmbDataDir.setToolTipText( MessageText.getString( "label.ctrl.space.for.suggestion" ));
			
			cmbDataDir.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					cmbDataDirChanged();
				}
			});
			cmbDataDir.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					cmbDataDirChanged();
				}
			});
	
			updateDataDirCombo();
			dirList = COConfigurationManager.getStringListParameter("saveTo_list");
			StringIterator iter = dirList.iterator();
			while (iter.hasNext()) {
				String s = iter.next();
				if (torrentOptions==null || !s.equals(torrentOptions.getParentDir())) {
					cmbDataDir.add(s);
				}
			}
	
			soBrowseButton.addSelectionListener(new ButtonListenerAdapter() {
				@Override
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					String sSavePath;
					String sDefPath = cmbDataDir.getText();
	
					File f = new File(sDefPath);
					if (sDefPath.length() > 0) {
						while (!f.exists()) {
							f = f.getParentFile();
							if (f == null) {
								f = new File(sDefPath);
								break;
							}
						}
					}
	
					DirectoryDialog dDialog = new DirectoryDialog(cmbDataDir.getShell(),
							SWT.SYSTEM_MODAL);
					dDialog.setFilterPath(f.getAbsolutePath());
					dDialog.setMessage(MessageText.getString("MainWindow.dialog.choose.savepath_forallfiles"));
					sSavePath = dDialog.open();
	
					if (sSavePath != null) {
						cmbDataDir.setText(sSavePath);
					}
				}
			});
		}
	
		private void setupStartOptions(SWTSkinObjectExpandItem so) {
			soStartOptionsExpandItem = so;
			Composite cTorrentOptions = so.getComposite();
	
			Composite cTorrentModes = new Composite(cTorrentOptions, SWT.NONE);
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			cTorrentModes.setLayoutData(Utils.getFilledFormData());
			GridLayout layout = new GridLayout();
			layout.numColumns = 4;
			layout.marginWidth = 5;
			layout.marginHeight = 5;
			cTorrentModes.setLayout(layout);
	
			Label label = new Label(cTorrentModes, SWT.NONE);
			gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "OpenTorrentWindow.startMode");
	
			cmbStartMode = new Combo(cTorrentModes, SWT.BORDER | SWT.READ_ONLY);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			cmbStartMode.setLayoutData(gridData);
			updateStartModeCombo();
			cmbStartMode.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setSelectedStartMode(cmbStartMode.getSelectionIndex());
				}
			});
	
			label = new Label(cTorrentModes, SWT.NONE);
			gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "OpenTorrentWindow.addPosition");
	
			cmbQueueLocation = new Combo(cTorrentModes, SWT.BORDER | SWT.READ_ONLY);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			cmbQueueLocation.setLayoutData(gridData);
			updateQueueLocationCombo();
			cmbQueueLocation.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setSelectedQueueLocation(cmbQueueLocation.getSelectionIndex());
				}
			});
	
			if ( TagManagerFactory.getTagManager().isEnabled()){
				
					// tag area
				
				Composite tagLeft 	= new Composite( cTorrentModes, SWT.NULL);
				tagLeft.setLayoutData( new GridData(GridData.VERTICAL_ALIGN_CENTER ));
				Composite tagRight 	= new Composite( cTorrentModes, SWT.NULL);
				gridData = new GridData(GridData.FILL_HORIZONTAL );
				gridData.horizontalSpan=3;
				tagRight.setLayoutData(gridData);
				
				layout = new GridLayout();
				layout.numColumns = 1;
				layout.marginWidth  = 0;
				layout.marginHeight = 0;
				tagLeft.setLayout(layout);
				
				layout = new GridLayout();
				layout.numColumns = 2;
				layout.marginWidth  = 0;
				layout.marginHeight = 0;
				tagRight.setLayout(layout);
				
				label = new Label(tagLeft, SWT.NONE);
				gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
				Messages.setLanguageText(label, "label.initial_tags");
		
				
				tagButtonsArea 	= new Composite( tagRight, SWT.NULL);
				gridData = new GridData(GridData.FILL_HORIZONTAL );
				tagButtonsArea.setLayoutData(gridData);
		
				RowLayout tagLayout = new RowLayout();
				tagLayout.pack = false;
				tagButtonsArea.setLayout( tagLayout);
				
				buildTagButtonPanel( tagButtonsArea );
				
				Button addTag = new Button( tagRight, SWT.NULL );
				addTag.setLayoutData( new GridData(GridData.VERTICAL_ALIGN_CENTER ));
				Messages.setLanguageText( addTag, "label.add.tag" );
				
				addTag.addSelectionListener(
					new SelectionAdapter() {
						
						public void 
						widgetSelected(
							SelectionEvent e) 
						{
							TagUIUtilsV3.showCreateTagDialog(null);
						}
					});
			}
		}
		
		private void
		buildTagButtonPanel(
			Composite	parent )
		{
			buildTagButtonPanel( parent, false );
		}
		
		private void
		buildTagButtonPanel(
			final 	Composite	parent,
			boolean	is_rebuild )
		{
			if ( parent.isDisposed()){
				
				return;
			}
			
			final String SP_KEY = "oto:tag:initsp";
					
			if ( is_rebuild ){
				
				Utils.disposeComposite( parent, false );
				
			}else{
				
				parent.setData( SP_KEY, getSavePath());
			}
			
			final TagType tt = TagManagerFactory.getTagManager().getTagType( TagType.TT_DOWNLOAD_MANUAL );
			
			List<Tag> initialTags = torrentOptions.getInitialTags();
			
			for ( final Tag tag: TagUIUtils.sortTags( tt.getTags())){
				
				if ( tag.canBePublic() && !tag.isTagAuto()){
					
					final Button but = new Button( parent, SWT.TOGGLE );
				
					but.setText( tag.getTagName( true ));
					
					but.setToolTipText( TagUIUtils.getTagTooltip(tag));
					
					if ( initialTags.contains( tag )){
						
						but.setSelection( true );
					}
					
					but.addSelectionListener(
						new SelectionAdapter() {
							
							public void 
							widgetSelected(
								SelectionEvent e) 
							{
								List<Tag>  tags = torrentOptions.getInitialTags();
								
								if ( but.getSelection()){
									
									tags.add( tag );
									
									TagFeatureFileLocation fl = (TagFeatureFileLocation)tag;
	
									if ( fl.supportsTagInitialSaveFolder()){
										
										File save_loc = fl.getTagInitialSaveFolder();
										
										if ( save_loc != null ){
											
											setSavePath( save_loc.getAbsolutePath());
										}
									}
								}else{
									
									tags.remove( tag );
									
									TagFeatureFileLocation fl = (TagFeatureFileLocation)tag;
	
									if ( fl.supportsTagInitialSaveFolder()){
										
										File save_loc = fl.getTagInitialSaveFolder();
										
										if ( save_loc != null && getSavePath().equals( save_loc.getAbsolutePath())){
											
											String old = (String)parent.getData( SP_KEY );
											
											if ( old != null ){
											
												setSavePath( old );
											}
										}
									}
								}
								
								torrentOptions.setInitialTags( tags );
								
								updateStartOptionsHeader();
							}
						});
					
					Menu menu = new Menu( but );
					
					but.setMenu( menu );
					MenuBuildUtils.addMaintenanceListenerForMenu(menu, new MenuBuilder() {
						public void buildMenu(Menu root_menu, MenuEvent menuEvent) {
							TagUIUtils.createSideBarMenuItems(root_menu, tag);
						}
					});
					
				}
			}
			
			String[] discoveredTags = listDiscoveredTags.toArray(new String[0]);
			for (final String tagName : discoveredTags) {
				boolean alreadyHave = checkAlreadyHaveTag(initialTags, tagName);
				if (alreadyHave) {
					break;
				}
				
				final Button but = new Button( parent, SWT.TOGGLE );
				but.setImage(ImageLoader.getInstance().getImage("image.sidebar.rcm"));

				
				if ( listTagsToCreate.contains( tagName )){
					but.setSelection( true );
				}

				
				but.setText( tagName );
				
				but.setToolTipText(MessageText.getString("tagtype.discovered"));
				
				but.addSelectionListener(
					new SelectionAdapter() {
						
						public void 
						widgetSelected(
							SelectionEvent e) 
						{
							List<Tag>  tags = torrentOptions.getInitialTags();
							
							if ( but.getSelection()){
								
								listTagsToCreate.add(tagName);
								
							}else{
								
								listTagsToCreate.remove(tagName);
								
							}
							
							
							updateStartOptionsHeader();
						}
					});
				
			}
			
			if ( is_rebuild ){
			
				parent.getParent().layout( true,  true );
				
				return;
			}
			
			tt.addTagTypeListener(
				new TagTypeListener()
				{
					
					public void 
					tagTypeChanged(
						TagType tag_type) 
					{					
					}
					
					public void 
					tagRemoved(
						Tag tag ) 
					{
						rebuild();
					}
					
					public void 
					tagChanged(
						Tag tag) 
					{
						rebuild();
					}
					
					public void 
					tagAdded(
						Tag tag) 
					{
						rebuild();
					}
					
					private void
					rebuild()
					{
						if ( parent.isDisposed()){
							
							tt.removeTagTypeListener( this );
						}else{
							
							Utils.execSWTThread(
								new Runnable()
								{
									public void
									run()
									{
										buildTagButtonPanel( parent, true );
									}
								});
						}
					}
				}, false );
		}
	
		private void setupTVFiles(SWTSkinObjectContainer soFilesTable, SWTSkinObjectTextbox soFilesFilter ) {
			TableColumnManager tcm = TableColumnManager.getInstance();
			if (tcm.getDefaultColumnNames(TABLEID_FILES) == null) {
				tcm.registerColumn(TorrentOpenFileOptions.class,
						TableColumnOTOF_Position.COLUMN_ID,
						new TableColumnCreationListener() {
							public void tableColumnCreated(TableColumn column) {
								new TableColumnOTOF_Position(column);
							}
						});
				tcm.registerColumn(TorrentOpenFileOptions.class,
						TableColumnOTOF_Download.COLUMN_ID,
						new TableColumnCreationListener() {
							public void tableColumnCreated(TableColumn column) {
								new TableColumnOTOF_Download(column);
							}
						});
				tcm.registerColumn(TorrentOpenFileOptions.class,
						TableColumnOTOF_Name.COLUMN_ID, new TableColumnCreationListener() {
							public void tableColumnCreated(TableColumn column) {
								new TableColumnOTOF_Name(column);
							}
						});
				tcm.registerColumn(TorrentOpenFileOptions.class,
						TableColumnOTOF_Size.COLUMN_ID, new TableColumnCreationListener() {
							public void tableColumnCreated(TableColumn column) {
								new TableColumnOTOF_Size(column);
							}
						});
				tcm.registerColumn(TorrentOpenFileOptions.class,
						TableColumnOTOF_Path.COLUMN_ID, new TableColumnCreationListener() {
							public void tableColumnCreated(TableColumn column) {
								new TableColumnOTOF_Path(column);
							}
						});
				tcm.registerColumn(TorrentOpenFileOptions.class,
						TableColumnOTOF_Ext.COLUMN_ID, new TableColumnCreationListener() {
							public void tableColumnCreated(TableColumn column) {
								new TableColumnOTOF_Ext(column);
							}
						});
				
				tcm.registerColumn(TorrentOpenFileOptions.class,
						TableColumnOTOF_Priority.COLUMN_ID, new TableColumnCreationListener() {
							public void tableColumnCreated(TableColumn column) {
								new TableColumnOTOF_Priority(column);
							}
						});
	
				tcm.setDefaultColumnNames(TABLEID_FILES, new String[] {
					TableColumnOTOF_Position.COLUMN_ID,
					TableColumnOTOF_Download.COLUMN_ID,
					TableColumnOTOF_Name.COLUMN_ID,
					TableColumnOTOF_Size.COLUMN_ID,
					TableColumnOTOF_Path.COLUMN_ID,
					TableColumnOTOF_Priority.COLUMN_ID
				});
				tcm.setDefaultSortColumnName(TABLEID_FILES, TableColumnOTOF_Position.COLUMN_ID);
			}
	
			tvFiles = TableViewFactory.createTableViewSWT(TorrentOpenFileOptions.class,
					TABLEID_FILES, TABLEID_FILES, null, "#", SWT.BORDER
							| SWT.FULL_SELECTION | SWT.MULTI);
			tvFiles.initialize(soFilesTable.getComposite());
			tvFiles.setRowDefaultHeight(20);
	
			if ( torrentOptions.getFiles().length > 1 && soFilesFilter != null ){
				
				soFilesFilter.setVisible( true );
				
				Text text = soFilesFilter.getTextControl();
								
				tvFiles.enableFilterCheck(text, this);
		
			}else{
				if ( soFilesFilter != null ){
					
					soFilesFilter.setVisible( false );
				}
			}
			
			tvFiles.addKeyListener(new KeyListener() {
	
				public void keyPressed(KeyEvent e) {
				}
	
				public void keyReleased(KeyEvent e) {
					if (e.keyCode == SWT.SPACE) {
						TableRowCore focusedRow = tvFiles.getFocusedRow();
						if ( focusedRow != null ){
							TorrentOpenFileOptions tfi_focus = ((TorrentOpenFileOptions) focusedRow.getDataSource());
							boolean download = !tfi_focus.isToDownload();
		
							TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(new TorrentOpenFileOptions[0]);
							setToDownload( infos, download );
						}
					}
					if (e.keyCode == SWT.F2 && (e.stateMask & SWT.MODIFIER_MASK) == 0) {
						TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(
								new TorrentOpenFileOptions[0]);
						renameFilenames(infos);
						e.doit = false;
						return;
					}
				}
			});
	
			tvFiles.addMenuFillListener(new TableViewSWTMenuFillListener() {
	
				public void fillMenu(String sColumnName, Menu menu) {
					final Shell shell = menu.getShell();
					MenuItem item;
					TableRowCore focusedRow = tvFiles.getFocusedRow();
					final TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(new TorrentOpenFileOptions[0]);
					final TorrentOpenFileOptions tfi_focus = ((TorrentOpenFileOptions) focusedRow.getDataSource());
					boolean download = tfi_focus.isToDownload();
	
					item = new MenuItem(menu, SWT.CHECK);
					Messages.setLanguageText(item, "label.download.file");
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							TableRowCore focusedRow = tvFiles.getFocusedRow();
							TorrentOpenFileOptions tfi_focus = ((TorrentOpenFileOptions) focusedRow.getDataSource());
							boolean download = !tfi_focus.isToDownload();
	
							setToDownload( infos, download );
						}
					});
					item.setSelection(download);
	
					
						// priority
					
					final MenuItem itemPriority = new MenuItem(menu, SWT.CASCADE);
					Messages.setLanguageText(itemPriority, "FilesView.menu.setpriority");
	
					final Menu menuPriority = new Menu(shell, SWT.DROP_DOWN);
					itemPriority.setMenu(menuPriority);
	
					final MenuItem itemHigh = new MenuItem(menuPriority, SWT.CASCADE);
	
					Messages.setLanguageText(itemHigh, "FilesView.menu.setpriority.high"); 
	
					final MenuItem itemNormal = new MenuItem(menuPriority, SWT.CASCADE);
		
					Messages.setLanguageText(itemNormal, "FilesView.menu.setpriority.normal");
					
					final MenuItem itemLow = new MenuItem(menuPriority, SWT.CASCADE);
		
					Messages.setLanguageText(itemLow, "FileItem.low");
	
					final MenuItem itemNumeric = new MenuItem(menuPriority, SWT.CASCADE);
		
					Messages.setLanguageText(itemNumeric, "FilesView.menu.setpriority.numeric"); 
	
					final MenuItem itemNumericAuto = new MenuItem(menuPriority, SWT.CASCADE);
		
					Messages.setLanguageText(itemNumericAuto, "FilesView.menu.setpriority.numeric.auto"); 
					
					Listener priorityListener = new Listener() {
						public void handleEvent(Event event) {
						
							Widget widget = event.widget;
							
							int	priority;
							
							if ( widget == itemHigh ){
								
								priority = 1;
								
							}else if ( widget == itemNormal ){
								
								priority = 0;
															
							}else if ( widget == itemLow ){
								
								priority = -1;
								
							}else if ( widget == itemNumeric ){
								
								SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
										"FilesView.dialog.priority.title",
										"FilesView.dialog.priority.text");
								
								entryWindow.prompt(
									new UIInputReceiverListener() {
										public void UIInputReceiverClosed(UIInputReceiver entryWindow) {
											if (!entryWindow.hasSubmittedInput()) {
												return;
											}
											String sReturn = entryWindow.getSubmittedInput();
											
											if (sReturn == null)
												return;
											
											int priority = 0;
											
											try {
												priority = Integer.valueOf(sReturn).intValue();
											} catch (NumberFormatException er) {
												
												Debug.out( "Invalid priority: " + sReturn );
												
												new MessageBoxShell(SWT.ICON_ERROR | SWT.OK,
														MessageText.getString("FilePriority.invalid.title"),
														MessageText.getString("FilePriority.invalid.text", new String[]{ sReturn })).open(null);
												
												return;
											}
											
											for (TorrentOpenFileOptions torrentFileInfo : infos) {
												
												torrentFileInfo.setPriority( priority );
											}
										}
									});
								
								return;
								
							}else if ( widget == itemNumericAuto ){
								
								int	next_priority = 0;
	
								TorrentOpenFileOptions[] all_files = torrentOptions.getFiles();
								
								if ( all_files.length != infos.length ){
									
									Set<Integer>	affected_indexes = new HashSet<Integer>();
									
									for ( TorrentOpenFileOptions file: infos ){
										
										affected_indexes.add( file.getIndex());
									}
										
									for ( TorrentOpenFileOptions file: all_files ){
									
										if ( !( affected_indexes.contains( file.getIndex()) || !file.isToDownload())){
											
											next_priority = Math.max( next_priority, file.getPriority()+1);
										}
									}
								}
								
								next_priority += infos.length;
								
								for ( TorrentOpenFileOptions file: infos ){
									
									file.setPriority( --next_priority );
								}
								
								return;
								
							}else{
								
								return;
							}
							
							for (TorrentOpenFileOptions torrentFileInfo : infos) {
								
								torrentFileInfo.setPriority( priority );
							}
						}
					};
	
					itemNumeric.addListener(SWT.Selection, priorityListener);
					itemNumericAuto.addListener(SWT.Selection, priorityListener);
					itemHigh.addListener(SWT.Selection, priorityListener);
					itemNormal.addListener(SWT.Selection, priorityListener);
					itemLow.addListener(SWT.Selection, priorityListener);
					
						// rename
					
					item = new MenuItem(menu, SWT.PUSH);
					Messages.setLanguageText(item, "FilesView.menu.rename_only");
					item.addSelectionListener(new SelectionAdapter() {
	
						public void widgetSelected(SelectionEvent e) {
	
							renameFilenames(infos);
						}
					});
	
					item = new MenuItem(menu, SWT.PUSH);
					Messages.setLanguageText(item,
							"OpenTorrentWindow.fileList.changeDestination");
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							changeFileDestination(infos,false);
						}
					});
	
					if ( infos.length > 1 && torrentOptions.iStartID != TorrentOpenOptions.STARTMODE_SEEDING ){
						item = new MenuItem(menu, SWT.PUSH);
						Messages.setLanguageText(item,
								"OpenTorrentWindow.fileList.changeDestination.all", new String[]{ String.valueOf( infos.length )});
						item.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent e) {
								changeFileDestination(infos,true);
							}
						});
					}
					
					new MenuItem(menu, SWT.SEPARATOR);
	
					item = new MenuItem(menu, SWT.PUSH);
					Messages.setLanguageText(item, "Button.selectAll");
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							tvFiles.selectAll();
						}
					});
	
					
					String dest_path = tfi_focus.getDestPathName();
					String parentDir = tfi_focus.parent.getParentDir();
					
					List<String> folder_list = new ArrayList<String>();
					
					folder_list.add( dest_path );
	
					if ( dest_path.startsWith( parentDir ) && dest_path.length() > parentDir.length()){
											
						String relativePath = dest_path.substring( parentDir.length() + 1 );
						
						while ( relativePath.contains( File.separator )){
							
							int	pos = relativePath.lastIndexOf( File.separator );
							
							relativePath = relativePath.substring( 0,  pos );
							
							folder_list.add( parentDir + File.separator + relativePath );
						}
					}
					
					for ( int i=folder_list.size()-1;i>=0;i-- ){
						
						final String this_dest_path = folder_list.get(i);
								
						item = new MenuItem(menu, SWT.PUSH);
						Messages.setLanguageText(item, "menu.selectfilesinfolder", new String[] {
								this_dest_path
						});
						
						item.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent e) {							
								TableRowCore[] rows = tvFiles.getRows();
								for (TableRowCore row : rows) {
									Object dataSource = row.getDataSource();
									if (dataSource instanceof TorrentOpenFileOptions) {
										TorrentOpenFileOptions fileOptions = (TorrentOpenFileOptions) dataSource;
										if ( fileOptions.getDestPathName().startsWith( this_dest_path )){
											row.setSelected(true);
										}
									}
								}
		
							}
						});
					}
					
					if ( !torrentOptions.isSimpleTorrent()){
						
						 new MenuItem(menu, SWT.SEPARATOR );
						 
						 item = new MenuItem(menu, SWT.PUSH);
						
						 Messages.setLanguageText(item, "OpenTorrentWindow.set.savepath");
						
						 item.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent e) {
									setSavePath();
								}
							});
							
						 item = new MenuItem(menu, SWT.PUSH);
						
						 Messages.setLanguageText(item, "OpenTorrentWindow.tlf.remove");
						
						 item.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent e) {
									removeTopLevelFolder();
								}
							});
						 
						 item.setEnabled( canRemoveTopLevelFolder());
						 
						 item = new MenuItem(menu, SWT.CHECK );
						
						 item.setSelection( COConfigurationManager.getBooleanParameter( "open.torrent.window.rename.on.tlf.change" ));
							
						 Messages.setLanguageText(item, "OpenTorrentWindow.tlf.rename");

						 item.addSelectionListener(
							 new SelectionAdapter() 
							 {
								 public void 
								 widgetSelected(
										 SelectionEvent e )
								 {
									 COConfigurationManager.setParameter(
											"open.torrent.window.rename.on.tlf.change", 
											((MenuItem)e.widget).getSelection());
								 }
							 });
						 
						 new MenuItem(menu, SWT.SEPARATOR );
					}
				}
	
				public void addThisColumnSubMenu(String sColumnName, Menu menuThisColumn) {
				}
			});
			
			tvFiles.addSelectionListener(new TableSelectionListener() {
				
				public void selected(TableRowCore[] row) {
					updateFileButtons();
				}
				
				public void mouseExit(TableRowCore row) {
				}
				
				public void mouseEnter(TableRowCore row) {
				}
				
				public void focusChanged(TableRowCore focus) {
				}
				
				public void deselected(TableRowCore[] rows) {
					updateFileButtons();
				}
				
				public void defaultSelected(TableRowCore[] rows, int stateMask) {
				}
			}, false);
			
	
			tvFiles.addDataSources(torrentOptions.getFiles());
		}
	
		public boolean 
		filterCheck(
			TorrentOpenFileOptions 	ds, 
			String 					filter, 
			boolean 				regex )
		{
			if ( filter == null || filter.length() == 0 ){
				
				return( true );
			}
			
			try {
				File file = ds.getDestFileFullName();

				String name = filter.contains( File.separator )?file.getAbsolutePath():file.getName();
				
				String s = regex ? filter : "\\Q" + filter.replaceAll("\\s*[|;]\\s*", "\\\\E|\\\\Q") + "\\E";
				
				boolean	match_result = true;
				
				if ( regex && s.startsWith( "!" )){
					
					s = s.substring(1);
					
					match_result = false;
				}
				
				Pattern pattern = RegExUtil.getCachedPattern( "fv:search", s, Pattern.CASE_INSENSITIVE);
	  
				return( pattern.matcher(name).find() == match_result );
				
			} catch (Exception e) {
				
				return true;
			}	
		}
		
		public void filterSet(String filter)
		{
		}
		
		protected void updateFileButtons() {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					TableRowCore[] rows = tvFiles.getSelectedRows();
					
					boolean hasRowsSelected = rows.length > 0;
					if (btnRename != null && !btnRename.isDisposed()) {
						btnRename.setEnabled(hasRowsSelected);
					}
					if (btnRetarget != null && !btnRetarget.isDisposed()) {
						btnRetarget.setEnabled(hasRowsSelected);
					}
					
					boolean all_marked 		= true;
					boolean all_unmarked 	= true;
					
					for ( TableRowCore row: rows ){
						TorrentOpenFileOptions tfi = ((TorrentOpenFileOptions) row.getDataSource());
						if ( tfi.isToDownload()){
							all_unmarked 	= false;
						}else{
							all_marked 		= false;
						}
					}
					
					btnSelectAll.setEnabled(  rows.length < torrentOptions.getFiles().length );
					
					btnMarkSelected.setEnabled( hasRowsSelected && !all_marked );
					btnUnmarkSelected.setEnabled( hasRowsSelected && !all_unmarked );
					
					if (btnSwarmIt != null && !btnSwarmIt.isDisposed()){
						boolean	enable=false;
						if ( rows.length == 1 ){
							TorrentOpenFileOptions tfi = ((TorrentOpenFileOptions) rows[0].getDataSource());	
							enable = tfi.lSize >= 50*1024*1024;
						}
						btnSwarmIt.setEnabled(enable);
					}
				}
			});
		}
	
		protected void renameFilenames(TorrentOpenFileOptions[] infos) {
			for (TorrentOpenFileOptions torrentFileInfo : infos) {
				String renameFilename = askForRenameFilename(torrentFileInfo);
				if (renameFilename == null) {
					break;
				}
				torrentFileInfo.setDestFileName(renameFilename,true);
				TableRowCore row = tvFiles.getRow(torrentFileInfo);
				if (row != null) {
					row.invalidate(true);
					row.refresh(true);
				}
			}
		}
	
		private String askForRenameFilename(TorrentOpenFileOptions fileInfo) {
			SimpleTextEntryWindow dialog = new SimpleTextEntryWindow(
					"FilesView.rename.filename.title", "FilesView.rename.filename.text");
			dialog.setPreenteredText(fileInfo.orgFileName, false); // false -> it's not "suggested", it's a previous value
			dialog.allowEmptyInput(false);
			dialog.prompt();
			if (!dialog.hasSubmittedInput()) {
				return null;
			}
			return dialog.getSubmittedInput();
		}
	
		private void
		setSavePath()
		{
			if ( torrentOptions.isSimpleTorrent()){
				
				changeFileDestination( torrentOptions.getFiles(), false );
				
			}else{
				DirectoryDialog dDialog = new DirectoryDialog(shell,SWT.SYSTEM_MODAL);
				
				File filterPath = new File( torrentOptions.getDataDir());
				
				if ( !filterPath.exists()){
					filterPath = filterPath.getParentFile();
				}
				dDialog.setFilterPath( filterPath.getAbsolutePath());
				dDialog.setMessage(MessageText.getString("MainWindow.dialog.choose.savepath")
						+ " (" + torrentOptions.getTorrentName() + ")");
				String sNewDir = dDialog.open();
	
				if (sNewDir == null){
					return;
				}
	
				File newDir = new File(sNewDir).getAbsoluteFile();
				
				if ( !newDir.isDirectory()){
					
					if ( newDir.exists()){
						
						Debug.out( "new dir isn't a dir!" );
						
						return;
						
					}else if ( !newDir.mkdirs()){
						
						Debug.out( "Failed to create '" + newDir + "'" );
						
						return;
					}
				}
	
				File new_parent = newDir.getParentFile();
				
				if ( new_parent == null ){
					
					Debug.out( "Invalid save path, parent folder is null" );
					
					return;
				}
				
				torrentOptions.setExplicitDataDir( new_parent.getAbsolutePath(), newDir.getName());
				
				if ( COConfigurationManager.getBooleanParameter( "open.torrent.window.rename.on.tlf.change" )){
				
					torrentOptions.setManualRename( new_parent.getName());
					
				}else{
					
					torrentOptions.setManualRename( null );
				}
				
				updateDataDirCombo();
				
				cmbDataDirChanged();
				
				
				/* old window used to reset this - not sure why, if the user's
				 * made some per-file changes already then we should keep them
				for ( TorrentOpenFileOptions tfi: torrentOptions.getFiles()){
					
					tfi.setFullDestName( null );
				}
				*/
			}
		}
		
		private boolean
		canRemoveTopLevelFolder()
		{
			if ( torrentOptions.isSimpleTorrent()){
				
				return( false );
				
			}else{
				
				File oldDir = new File( torrentOptions.getDataDir());
	
				File newDir = oldDir.getParentFile();
				
				File newParent  = newDir.getParentFile();
				
					// newParent will be null if trying to remove the top level dir when already at a file system
					// root (e.g. C:\) 
					// we should of course be able to support this, but unfortunately there's lots of code in Vuze
					// in places-i-don't-want-to-change that borks if we try and do this (feel free to try...)
				
				return( newParent != null );
			}
		}
		
		private void
		removeTopLevelFolder()
		{
			if ( torrentOptions.isSimpleTorrent()){
				
				
			}else{
				
				File oldDir = new File( torrentOptions.getDataDir());
	
				File newDir = oldDir.getParentFile();
				
				File newParent  = newDir.getParentFile();
				if ( newParent == null ){
					
					Debug.out( "Invalid save path, parent folder is null" );
					
					return;
				}
				
				torrentOptions.setExplicitDataDir( newParent.getAbsolutePath(), newDir.getName());
				
				if ( COConfigurationManager.getBooleanParameter( "open.torrent.window.rename.on.tlf.change" )){
				
					torrentOptions.setManualRename( newParent.getName());
					
				}else{
					
					torrentOptions.setManualRename( null );
				}
				
				updateDataDirCombo();
				
				cmbDataDirChanged();
				
				
				/* old window used to reset this - not sure why, if the user's
				 * made some per-file changes already then we should keep them
				for ( TorrentOpenFileOptions tfi: torrentOptions.getFiles()){
					
					tfi.setFullDestName( null );
				}
				*/
			}
		}
		
		private void changeFileDestination(TorrentOpenFileOptions[] infos, boolean allAtOnce ) {
	
			if ( allAtOnce && infos.length > 1 ){
				
					// find a common ancestor if it exists
				
				String current_parent = null;
				
				for (TorrentOpenFileOptions fileInfo : infos) {
					
					String dest = fileInfo.getDestPathName();
					
					if ( current_parent == null ){
						
						current_parent = dest;
						
					}else{
						
						if ( !current_parent.equals( dest )){
							
							char[] cp_chars = current_parent.toCharArray();
							char[] p_chars	= dest.toCharArray();
							
							int cp_len 	= cp_chars.length;
							int	p_len	= p_chars.length;
									
							int	min = Math.min( cp_len, p_len );
									
							int pos = 0;
							
							while ( pos < min && cp_chars[pos] == p_chars[pos] ){
								
								pos++;
							}
							
							if ( pos < cp_len ){
															
								File f = new File( new String( cp_chars, 0, pos ) + "x" );
								
								File pf = f.getParentFile();
								
								if ( pf == null ){
									
									current_parent = "";
									
								}else{
									
									current_parent = pf.toString();
								}
							}
						}
					}
				}
				
				DirectoryDialog dDialog = new DirectoryDialog( shell, SWT.SYSTEM_MODAL );
				
				if ( current_parent.length() > 0 ){
					
					dDialog.setFilterPath( current_parent );
				}
				
				dDialog.setMessage(MessageText.getString("MainWindow.dialog.choose.savepath_forallfiles"));
				
				String sSavePath = dDialog.open();

				if ( sSavePath != null) {
					
					if ( sSavePath.endsWith( File.separator )){
						
						sSavePath = sSavePath.substring( 0, sSavePath.length() - 1 );
					}
					
					int prefix_len = current_parent.length();
					
					for ( TorrentOpenFileOptions fileInfo: infos ){
						
						String dest = fileInfo.getDestPathName();
						
						if ( prefix_len == 0 ){
							
							File f = new File( dest );
							
							while( f.getParentFile() != null ){
								
								f = f.getParentFile();
							}
							
							dest = dest.substring( f.toString().length());
							
						}else{
			
							dest = dest.substring( prefix_len );
						}
						
						if ( dest.startsWith( File.separator )){
							
							dest = dest.substring( 1 );
						}
						
						if ( dest.length() > 0 ){
						
							fileInfo.setDestPathName( sSavePath + File.separator + dest );
							
						}else{
							
							fileInfo.setDestPathName( sSavePath );
						}
					}
				}
			}else{
				for (TorrentOpenFileOptions fileInfo : infos) {
					int style = (fileInfo.parent.iStartID == TorrentOpenOptions.STARTMODE_SEEDING)
							? SWT.OPEN : SWT.SAVE;
					FileDialog fDialog = new FileDialog(shell, SWT.SYSTEM_MODAL
							| style);
		
					String sFilterPath = fileInfo.getDestPathName();
					String sFileName = fileInfo.orgFileName;
		
					File f = new File(sFilterPath);
					if (!f.isDirectory()) {
						// Move up the tree until we have an existing path
						while (sFilterPath != null) {
							String parentPath = f.getParent();
							if (parentPath == null)
								break;
		
							sFilterPath = parentPath;
							f = new File(sFilterPath);
							if (f.isDirectory())
								break;
						}
					}
		
					if (sFilterPath != null){
						fDialog.setFilterPath(sFilterPath);
					}
					
					fDialog.setFileName(sFileName);
					fDialog.setText(MessageText.getString("MainWindow.dialog.choose.savepath")
							+ " (" + fileInfo.orgFullName + ")");
					String sNewName = fDialog.open();
		
					if (sNewName == null)
						return;
		
					if (fileInfo.parent.iStartID == TorrentOpenOptions.STARTMODE_SEEDING) {
						File file = new File(sNewName);
						if (file.length() == fileInfo.lSize)
							fileInfo.setFullDestName(sNewName);
						else {
							MessageBoxShell mb = new MessageBoxShell(SWT.OK,
									"OpenTorrentWindow.mb.badSize", new String[] {
										file.getName(),
										fileInfo.orgFullName
									});
							mb.setParent(shell);
							mb.open(null);
						}
					} else
						fileInfo.setFullDestName(sNewName);
		
				} // for i
			}
			
			checkSeedingMode();
			updateDataDirCombo();
			diskFreeInfoRefreshPending = true;
		}
	
		private void setupInfoFields(SWTSkin skin) {
			SWTSkinObject so;
			so = skin.getSkinObject("torrentinfo-name");
			TOTorrent torrent = torrentOptions.getTorrent();

			if (so instanceof SWTSkinObjectText) {
				
				String hash_str = null;
				
				try{
					hash_str = ByteFormatter.encodeString(torrentOptions.getTorrent().getHash());
					
				}catch( Throwable e ){
				}
				
				SWTSkinObjectText text = (SWTSkinObjectText)so;
				
				text.setText( torrentOptions.getTorrentName() +  (hash_str==null?"":("\u00a0\u00a0\u00a0\u00a0[" + hash_str + "]")));
				
				if ( hash_str != null ){
					
					final String f_hash_str = hash_str;
					
					ClipboardCopy.addCopyToClipMenu(
						text.getControl(),
						new ClipboardCopy.copyToClipProvider2() {
							
							public String 
							getMenuResource() 
							{
								return( "menu.copy.hash.to.clipboard" );
							}
							
							public String 
							getText() 
							{
								return( f_hash_str );
							}
						});
				}
			}
	
			so = skin.getSkinObject("torrentinfo-trackername");
			
			
			if ( torrent != null ){
				if (so instanceof SWTSkinObjectText) {
					((SWTSkinObjectText) so).setText(TrackerNameItem.getTrackerName(torrent) + ((torrent==null||!torrent.getPrivate())?"":(" (private)")));
				}
		
				so = skin.getSkinObject("torrentinfo-comment");
				if (so instanceof SWTSkinObjectText) {
		
					try {
						LocaleUtilDecoder decoder = LocaleTorrentUtil.getTorrentEncoding(torrent);
						String s = decoder.decodeString(torrent.getComment());
						((SWTSkinObjectText) so).setText(s);
					} catch (UnsupportedEncodingException e) {
					} catch (TOTorrentException e) {
					}
				}
		
				so = skin.getSkinObject("torrentinfo-createdon");
				if (so instanceof SWTSkinObjectText) {
					String creation_date = DisplayFormatters.formatDate(torrent.getCreationDate() * 1000l);
					((SWTSkinObjectText) so).setText(creation_date);
				}
			}
		}
	
		private void setupTrackers(SWTSkinObjectContainer so) {
			Composite parent = so.getComposite();
	
			Button button = new Button( parent, SWT.PUSH );
			Messages.setLanguageText( button, "label.edit.trackers" );

			TOTorrent torrent = torrentOptions.getTorrent();
			
			button.setEnabled( torrent != null && !TorrentUtils.isReallyPrivate( torrent ));
			
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					List<List<String>> trackers = torrentOptions.getTrackers( false );
					new MultiTrackerEditor( shell, null, trackers, new TrackerEditorListener() {
						public void trackersChanged(String str, String str2, List<List<String>> updatedTrackers) {
							torrentOptions.setTrackers(updatedTrackers);
						}
					}, true, true );
				}});
		}
		
		private void setupUpDownLimitOption(SWTSkinObjectContainer so) {
			Composite parent = so.getComposite();
	
			parent.setBackgroundMode( SWT.INHERIT_FORCE );	// win 7 classic theme shows grey background without this
			parent.setLayout( new GridLayout(4, false));
			
			GridData gridData = new GridData();
			Label label = new Label(parent, SWT.NULL);
			label.setText( MessageText.getString( "TableColumn.header.maxupspeed") + "[" + DisplayFormatters.getRateUnit(DisplayFormatters.UNIT_KB  ) + "]" );

			gridData = new GridData();
			GenericIntParameter paramMaxUploadSpeed = 
				new GenericIntParameter(
					new IntAdapter()
					{
						public void
						setIntValue(
							String	key,
							int		value )
						{
							torrentOptions.setMaxUploadSpeed( value );
						}
					},
					parent,
					"torrentoptions.config.uploadspeed", 0, Integer.MAX_VALUE );
			
			paramMaxUploadSpeed.setLayoutData(gridData);

			label = new Label(parent, SWT.NULL);
			label.setText( MessageText.getString( "TableColumn.header.maxdownspeed") + "[" + DisplayFormatters.getRateUnit(DisplayFormatters.UNIT_KB  ) + "]" );

			gridData = new GridData();
			GenericIntParameter paramMaxDownloadSpeed = 
				new GenericIntParameter(
					new IntAdapter()
					{
						public void
						setIntValue(
							String	key,
							int		value )
						{
							torrentOptions.setMaxDownloadSpeed( value );
						}
					},
					parent,
					"torrentoptions.config.downloadspeed", 0, Integer.MAX_VALUE );
			
			paramMaxDownloadSpeed.setLayoutData(gridData);
		}
		
		private void setupIPFilterOption(SWTSkinObjectContainer so) {
			Composite parent = so.getComposite();
	
			parent.setBackgroundMode( SWT.INHERIT_FORCE );	// win 7 classic theme shows grey background without this
			parent.setLayout( new GridLayout());
			
			Button button = new Button(parent, SWT.CHECK | SWT.WRAP );
			Messages.setLanguageText(button, "MyTorrentsView.menu.ipf_enable");
			GridData gd = new GridData();
			gd.verticalAlignment = SWT.CENTER;
			button.setLayoutData( gd);
			button.setSelection(!torrentOptions.disableIPFilter);
	
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					torrentOptions.disableIPFilter = !((Button) e.widget).getSelection();
				}
			});
	
		}
	
		private void setupPeerSourcesAndNetworkOptions(SWTSkinObjectContainer so) {
			Composite parent = so.getComposite();
			parent.setBackgroundMode( SWT.INHERIT_FORCE );	// win 7 classic theme shows grey background without this

		
			Group peer_sources_group = new Group(parent, SWT.NULL);
			
			{
				Messages.setLanguageText(peer_sources_group,
						"ConfigView.section.connection.group.peersources");
				GridLayout peer_sources_layout = new GridLayout(3, true);
				peer_sources_group.setLayout(peer_sources_layout);
				
				FormData form_data = Utils.getFilledFormData();
				form_data.bottom = null;
				peer_sources_group.setLayoutData(form_data);
		
				//		Label label = new Label(peer_sources_group, SWT.WRAP);
				//		Messages.setLanguageText(label,
				//				"ConfigView.section.connection.group.peersources.info");
				//		GridData gridData = new GridData();
				//		label.setLayoutData(gridData);
		
				for (int i = 0; i < PEPeerSource.PS_SOURCES.length; i++) {
		
					final String p = PEPeerSource.PS_SOURCES[i];
		
					String config_name = "Peer Source Selection Default." + p;
					String msg_text = "ConfigView.section.connection.peersource." + p;
		
					Button button = new Button(peer_sources_group, SWT.CHECK);
					Messages.setLanguageText(button, msg_text);
		
					button.setSelection(COConfigurationManager.getBooleanParameter(config_name));
					
					button.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							torrentOptions.peerSource.put(p, ((Button)e.widget).getSelection());
						}
					});
		
					GridData gridData = new GridData();
					button.setLayoutData(gridData);
				}
			}
	
				// networks
			
			{				
				Group network_group = new Group(parent, SWT.NULL);
				Messages.setLanguageText(network_group,
						"ConfigView.section.connection.group.networks");
				GridLayout network_layout = new GridLayout(3, true);
				network_group.setLayout(network_layout);
		
				FormData form_data = Utils.getFilledFormData();
				form_data.top = new FormAttachment( peer_sources_group );
				network_group.setLayoutData(form_data);
		
				for (int i = 0; i < AENetworkClassifier.AT_NETWORKS.length; i++) {
		
					final String nn = AENetworkClassifier.AT_NETWORKS[i];
		
					String msg_text = "ConfigView.section.connection.networks." + nn;
		
					Button button = new Button(network_group, SWT.CHECK);
					Messages.setLanguageText(button, msg_text);
		
					Map<String,Boolean> enabledNetworks = torrentOptions.getEnabledNetworks();

					button.setSelection(enabledNetworks.get( nn ));
					
					button.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							torrentOptions.setNetworkEnabled(nn, ((Button)e.widget).getSelection());
						}
					});
		
					GridData gridData = new GridData();
					button.setLayoutData(gridData);
				}
			}
		}
	
		private void updateDataDirCombo() {
	
			if (cmbDataDir == null) {
				return;
			}
	
			try{
				bSkipDataDirModify = true;
	
				if ( torrentOptions == null ){
					
					String prev_parent = null;
					
					boolean not_same = false;
					
					for ( TorrentOpenOptions to: torrentOptionsMulti ){
						
						String parent = to.getParentDir();
						
						if ( prev_parent != null && !prev_parent.equals( parent )){
							
							not_same = true;
							
							break;
						}
						
						prev_parent = parent;
						
					}
					
					if ( not_same ){
											
						cmbDataDir.setText( COConfigurationManager.getStringParameter(PARAM_DEFSAVEPATH));
						
					}else{
						
							// prev_parent can be null when we're tearing down the dialog...
						
						cmbDataDir.setText( prev_parent==null?"":prev_parent );
					}
				}else{
					
					cmbDataDir.setText( torrentOptions.getParentDir());
				}
				
			}finally{
				
				bSkipDataDirModify = false;
			}
		}
	
		private void
		setSavePath(
			String	path )
		{		
			cmbDataDir.setText( path );
		}
		
		private String
		getSavePath()
		{
			return( torrentOptions.getParentDir());
		}
		
		private void updateQueueLocationCombo() {
			if (cmbQueueLocation == null)
				return;
	
			String[] sItemsText = new String[queueLocations.length];
			for (int i = 0; i < queueLocations.length; i++) {
				String sText = MessageText.getString("OpenTorrentWindow.addPosition."
						+ queueLocations[i]);
				sItemsText[i] = sText;
			}
			cmbQueueLocation.setItems(sItemsText);
			cmbQueueLocation.select(torrentOptions.iQueueLocation);
		}
	
		private void updateSize() {
	
			if (soFileAreaInfo == null && soExpandItemFiles == null) {
				return;
			}
	
			/*
			 * determine info for selected torrents only
			 */
			long totalSize = 0;
			long checkedSize = 0;
			long numToDownload = 0;
	
			TorrentOpenFileOptions[] dataFiles = torrentOptions.getFiles();
			for (TorrentOpenFileOptions file : dataFiles) {
				totalSize += file.lSize;
	
				if (file.isToDownload()) {
					checkedSize += file.lSize;
					numToDownload++;
				}
			}
	
			boolean	changed = checkedSize != currentSelectedDataSize;
			
			currentSelectedDataSize = checkedSize;
			
			String text;
			// build string and set label
			if (totalSize == 0) {
				text = "";
			} else if (checkedSize == totalSize) {
				text = DisplayFormatters.formatByteCountToKiBEtc(totalSize);
			} else {
				text = MessageText.getString("OpenTorrentWindow.filesInfo", new String[] {
					DisplayFormatters.formatByteCountToKiBEtc(checkedSize),
					DisplayFormatters.formatByteCountToKiBEtc(totalSize)
				});
			}
	
			if (soFileAreaInfo != null) {
				soFileAreaInfo.setText(text);
			}
			if (soExpandItemFiles != null) {
				String id = "OpenTorrentOptions.header.filesInfo."
						+ (numToDownload == dataFiles.length ? "all" : "some");
				soExpandItemFiles.setText(MessageText.getString(id, new String[] {
					String.valueOf(numToDownload),
					String.valueOf(dataFiles.length),
					text
				}));
			}
	
			diskFreeInfoRefreshPending = true;
	
			if ( changed ){
				
				changeListener.instanceChanged( this );
			}
		}
	
		protected long
		getSelectedDataSize()
		{
			return( currentSelectedDataSize );
		}
		
		private void updateStartModeCombo() {
			if (cmbStartMode == null)
				return;
	
			String[] sItemsText = new String[startModes.length];
			for (int i = 0; i < startModes.length; i++) {
				String sText = MessageText.getString("OpenTorrentWindow.startMode."
						+ startModes[i]);
				sItemsText[i] = sText;
			}
			cmbStartMode.setItems(sItemsText);
			cmbStartMode.select(torrentOptions.iStartID);
			cmbStartMode.layout(true);
		}
	
		public void updateUI() {
			if ( tvFiles != null ){
				tvFiles.refreshTable(false);
			}
			
			if (diskFreeInfoRefreshPending && !diskFreeInfoRefreshRunning
					&& FileUtil.getUsableSpaceSupported()) {
				diskFreeInfoRefreshRunning = true;
				diskFreeInfoRefreshPending = false;
	
				final HashSet FSroots = new HashSet(Arrays.asList(File.listRoots()));
				final HashMap partitions = new HashMap();
	
				for ( TorrentOpenOptions too: torrentOptionsMulti ){
					TorrentOpenFileOptions[] files = too.getFiles();
					for (int j = 0; j < files.length; j++) {
						TorrentOpenFileOptions file = files[j];
						if (!file.isToDownload())
							continue;
		
						// reduce each file to its partition root
						File root = file.getDestFileFullName().getAbsoluteFile();
		
						Partition part = (Partition) partitions.get(parentToRootCache.get(root.getParentFile()));
		
						if (part == null) {
							File next;
							while (true) {
								root = root.getParentFile();
								next = root.getParentFile();
								if (next == null)
									break;
		
								// bubble up until we hit an existing directory
								if (!getCachedExistsStat(root) || !root.isDirectory())
									continue;
		
								// check for mount points (different free space) or simple loops in the directory structure
								if (FSroots.contains(root) || root.equals(next)
										|| getCachedDirFreeSpace(next) != getCachedDirFreeSpace(root))
									break;
							}
		
							parentToRootCache.put(
									file.getDestFileFullName().getAbsoluteFile().getParentFile(),
									root);
		
							part = (Partition) partitions.get(root);
		
							if (part == null) {
								part = new Partition(root);
								part.freeSpace = getCachedDirFreeSpace(root);
								partitions.put(root, part);
							}
						}
		
						part.bytesToConsume += file.lSize;
					}
				}
	
				// clear child objects
				if (diskspaceComp != null && !diskspaceComp.isDisposed()) {
					Control[] labels = diskspaceComp.getChildren();
					for (int i = 0; i < labels.length; i++)
						labels[i].dispose();
	
					// build labels
					Iterator it = partitions.values().iterator();
					while (it.hasNext()) {
						Partition part = (Partition) it.next();
	
						boolean filesTooBig = part.bytesToConsume > part.freeSpace;
						
						String s = MessageText.getString("OpenTorrentWindow.diskUsage",
								new String[] {
							DisplayFormatters.formatByteCountToKiBEtc(part.bytesToConsume),
							DisplayFormatters.formatByteCountToKiBEtc(part.freeSpace)
						});
						
						Label l;
						l = new Label(diskspaceComp, SWT.NONE);
						l.setForeground(filesTooBig ? Colors.colorError : null);
						l.setText(part.root.getPath());
						l.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
	
						l = new Label(diskspaceComp, SWT.NONE);
						l.setForeground(filesTooBig ? Colors.colorError : null);
						l.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
						l.setText(s);
					}
	
					// hack to force resize :(
					diskspaceComp.layout(true);
					soExpandItemSaveTo.relayout();
				}
	
				diskFreeInfoRefreshRunning = false;
			}
		}
	
		private boolean 
		okPressed(
			String dataDirPassed) 
		{
			File filePassed = new File(dataDirPassed);
	
			File fileDefSavePath = new File(
					COConfigurationManager.getStringParameter(PARAM_DEFSAVEPATH));
	
			if (filePassed.equals(fileDefSavePath) && !fileDefSavePath.isDirectory()) {
				FileUtil.mkdirs(fileDefSavePath);
			}
	
			boolean isPathInvalid = dataDirPassed.length() == 0 || filePassed.isFile();
			if (!isPathInvalid && !filePassed.isDirectory()) {
				MessageBoxShell mb = new MessageBoxShell(SWT.YES | SWT.NO
						| SWT.ICON_QUESTION, "OpenTorrentWindow.mb.askCreateDir",
						new String[] {
							filePassed.toString()
						});
				mb.setParent(shell);
				mb.open(null);
				int doCreate = mb.waitUntilClosed();
	
				if (doCreate == SWT.YES)
					isPathInvalid = !FileUtil.mkdirs(filePassed);
				else {
					cmbDataDir.setFocus();
					return false;
				}
			}
	
			if (isPathInvalid) {
				MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.ICON_ERROR,
						"OpenTorrentWindow.mb.noGlobalDestDir", new String[] {
							filePassed.toString()
						});
				mb.setParent(shell);
				mb.open(null);
				cmbDataDir.setFocus();
				return false;
			}
	
			String sExistingFiles = "";
			int iNumExistingFiles = 0;
	
			File torrentOptionsDataDir = new File(torrentOptions.getDataDir());
	
			// Need to make directory now, or single file torrent will take the 
			// "dest dir" as their filename.  ie:
			// 1) Add single file torrent with named "hi.exe"
			// 2) type a non-existant directory c:\test\moo
			// 3) unselect the torrent
			// 4) change the global def directory to a real one
			// 5) click ok.  "hi.exe" will be written as moo in c:\test		
	
			if ( !torrentOptions.isSimpleTorrent()){
				torrentOptionsDataDir = torrentOptionsDataDir.getParentFile();	// for non-simple this points to the top folder in downoad
			}
			
			if (!torrentOptionsDataDir.isDirectory() && !FileUtil.mkdirs(torrentOptionsDataDir)) {
				MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.ICON_ERROR,
						"OpenTorrentWindow.mb.noDestDir", new String[] {
						torrentOptionsDataDir.toString(),
							torrentOptions.getTorrentName()
						});
				mb.setParent(shell);
				mb.open(null);
				return false;
			}

			if (!torrentOptions.isValid) {
				MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.ICON_ERROR,
						"OpenTorrentWindow.mb.notValid", new String[] {
							torrentOptions.getTorrentName()
						});
				mb.setParent(shell);
				mb.open(null);
				return false;
			}
	
			TorrentOpenFileOptions[] files = torrentOptions.getFiles();
			for (int j = 0; j < files.length; j++) {
				TorrentOpenFileOptions fileInfo = files[j];
				if (fileInfo.getDestFileFullName().exists()) {
					sExistingFiles += fileInfo.orgFullName + " - "
							+ torrentOptions.getTorrentName() + "\n";
					iNumExistingFiles++;
					if (iNumExistingFiles > 5) {
						// this has the potential effect of adding 5 files from the first 
						// torrent and then 1 file from each of the remaining torrents
						break;
					}
				}
			}
	
			if (sExistingFiles.length() > 0) {
				if (iNumExistingFiles > 5) {
					sExistingFiles += MessageText.getString(
							"OpenTorrentWindow.mb.existingFiles.partialList", new String[] {
								"" + iNumExistingFiles
							}) + "\n";
				}
	
				MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.CANCEL
						| SWT.ICON_WARNING, "OpenTorrentWindow.mb.existingFiles",
						new String[] {
							sExistingFiles
						});
				mb.setParent(shell);
				mb.open(null);
				if (mb.waitUntilClosed() != SWT.OK) {
					return false;
				}
			}
	
			String sDefaultPath = COConfigurationManager.getStringParameter(PARAM_DEFSAVEPATH);
			
			String dataDir;
			
			if ( torrentOptions.isExplicitDataDir()){
				
				dataDir = torrentOptions.getDataDir();
			
			}else{
				
				dataDir = torrentOptions.getParentDir();
			}
			
			if (!dataDir.equals(sDefaultPath)) {
				
				int	 limit = COConfigurationManager.getIntParameter( "saveTo_list.max_entries" );

				if ( limit >= 0 ){

					// Move sDestDir to top of list
		
					// First, check to see if sDestDir is already in the list
					File fDestDir = new File(dataDir);
					int iDirPos = -1;
					for (int i = 0; i < dirList.size(); i++) {
						String sDirName = dirList.get(i);
						File dir = new File(sDirName);
						if (dir.equals(fDestDir)) {
							iDirPos = i;
							break;
						}
					}
		
					// If already in list, remove it
					if (iDirPos > 0 && iDirPos < dirList.size())
						dirList.remove(iDirPos);
		
					// and add it to the top
					dirList.add(0, dataDir );
		
					// Limit
					if (limit > 0 && dirList.size() > limit){
						dirList.remove(dirList.size() - 1);
					}
					
					// Temporary list cleanup
					try {
						for (int j = 0; j < dirList.size(); j++) {
							File dirJ = new File(dirList.get(j));
							for (int i = 0; i < dirList.size(); i++) {
								try {
									if (i == j)
										continue;
		
									File dirI = new File(dirList.get(i));
		
									if (dirI.equals(dirJ)) {
										dirList.remove(i);
										// dirList shifted up, fix indexes
										if (j > i)
											j--;
										i--;
									}
								} catch (Exception e) {
									// Ignore
								}
							}
						}
					} catch (Exception e) {
						// Ignore
					}
		
					COConfigurationManager.setParameter("saveTo_list", dirList);
					COConfigurationManager.save();
				}
			}
	
			if (COConfigurationManager.getBooleanParameter("DefaultDir.AutoUpdate")){
				COConfigurationManager.setParameter( PARAM_DEFSAVEPATH, dataDir );
			}
		
			
			if (listTagsToCreate.size() > 0) {
				TagManager tagManager = TagManagerFactory.getTagManager();
				TagType tagType = tagManager.getTagType(TagType.TT_DOWNLOAD_MANUAL);
				String[] tagsToCreate = listTagsToCreate.toArray(new String[0]);
				
				List<Tag> initialTags = torrentOptions.getInitialTags();
				
				for (String tagName : tagsToCreate) {
					try {
						Tag tag = tagType.createTag(tagName, true);
						tag.setPublic(true);
						listTagsToCreate.remove(tagName);
						initialTags.add(tag);
					} catch (TagException e) {
					}
				}
				torrentOptions.setInitialTags(initialTags);
			}
			
			return true;
		}
		
		private void
		dispose()
		{
			tvFiles.delete();
		}
	}
	
	public interface
	OpenTorrentInstanceListener
	{
		public void
		instanceChanged(
			OpenTorrentInstance		instance );
	}
	
	private class
	IntAdapter
		extends GenericParameterAdapter
	{
		public int
		getIntValue(
			String	key )
		{
			return( 0 );
		}
		
		public int
		getIntValue(
			String	key,
			int		def )
		{
			return( def );
		}
		
		
		public boolean
		resetIntDefault(
			String	key )
		{
			return( false );
		}	
	}

	public boolean checkAlreadyHaveTag(List<Tag> initialTags, String tagName) {
		boolean alreadyHave = false;
		for (Tag tag : initialTags) {
			if (tagName.equalsIgnoreCase(tag.getTagName(false))
					|| tagName.equalsIgnoreCase(tag.getTagName(true))) {
				alreadyHave = true;
				break;
			}
		}
		return alreadyHave;
	}
}
