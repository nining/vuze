/*
 * Created on Jan 27, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.devices.impl;

import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.ipc.IPCInterface;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.messenger.config.PlatformDevicesMessenger;
import com.aelitis.azureus.core.util.*;

public class 
DeviceManagerImpl 
	implements DeviceManager, AEDiagnosticsEvidenceGenerator
{
	private static final String	LOGGER_NAME 			= "Devices";
	private static final String	CONFIG_FILE 			= "devices.config";
	private static final String	AUTO_SEARCH_CONFIG_KEY	= "devices.config.auto_search";
	
	protected static final int	DEVICE_UPDATE_PERIOD	= 5*1000;
	
	private static DeviceManagerImpl		singleton;
	
	public static void
	preInitialise()
	{
	}
	
	public static DeviceManager
	getSingleton()
	{
		synchronized( DeviceManagerImpl.class ){
			
			if ( singleton == null ){
				
				singleton = new DeviceManagerImpl();
			}
		}
		
		return( singleton );
	}
	
	
	
	private List<DeviceImpl>			device_list = new ArrayList<DeviceImpl>();
	private Map<String,DeviceImpl>		device_map	= new HashMap<String, DeviceImpl>();
	
	private DeviceManagerUPnPImpl	upnp_manager;
	
	private CopyOnWriteList<DeviceManagerListener>	listeners	= new CopyOnWriteList<DeviceManagerListener>();
	
	private boolean	auto_search;
	private boolean	closing;
	
	private boolean	config_unclean;
	private boolean	config_dirty;
	
	private int		explicit_search;
	
	private TranscodeManagerImpl	transcode_manager;
	
	private AEDiagnosticsLogger		logger;
	
	protected
	DeviceManagerImpl()
	{
		AEDiagnostics.addEvidenceGenerator( this );

		upnp_manager = new DeviceManagerUPnPImpl( this );

		loadConfig();
		
		new DeviceiTunesManager( this );
		
		transcode_manager = new TranscodeManagerImpl( this );
		
		COConfigurationManager.addAndFireParameterListener(
			AUTO_SEARCH_CONFIG_KEY,
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String name ) 
				{
					auto_search = COConfigurationManager.getBooleanParameter( name, true );
				}
			});
		
		AzureusCoreFactory.getSingleton().addLifecycleListener(
			new AzureusCoreLifecycleAdapter()
			{
				public void
				stopping(
					AzureusCore		core )
				{					
					synchronized( DeviceManagerImpl.this ){
				
						if ( config_dirty || config_unclean ){
							
							saveConfig();
						}
						
						closing	= true;
						
						transcode_manager.close();
						
						DeviceImpl[] devices = getDevices();
						
						for ( DeviceImpl device: devices ){
							
							device.close();
						}
					}
				}
			});
		
		upnp_manager.initialise();
		
		SimpleTimer.addPeriodicEvent(
				"DeviceManager:update",
				DEVICE_UPDATE_PERIOD,
				new TimerEventPerformer()
				{
					private int tick_count = 0;
					
					public void 
					perform(
						TimerEvent event ) 
					{
						List<DeviceImpl> copy;
						
						tick_count++;
						
						transcode_manager.updateStatus( tick_count );
						
						synchronized( DeviceManagerImpl.this ){

							if( device_list.size() == 0 ){
								
								return;
							}
							
							copy = new ArrayList<DeviceImpl>( device_list );
						}
						
						for ( DeviceImpl device: copy ){
							
							device.updateStatus( tick_count );
						}
					}
				});
	}
	
	protected DeviceManagerUPnPImpl 
	getUPnPManager()
	{
		return( upnp_manager );
	}
	
	public Device
	createDevice(
		int						device_type,
		String					name )
	
		throws DeviceManagerException
	{
		if ( device_type == Device.DT_MEDIA_RENDERER ){
			
			DeviceImpl res = new DeviceMediaRendererImpl( this, name );
			
			addDevice( res );
			
			return( res );
			
		}else{
			
			throw( new DeviceManagerException( "Can't manually create this device type" ));
		}
	}
	
	public void
	search(
		final int					millis,
		final DeviceSearchListener	listener )
	{
		new AEThread2( "DM:search", true )
		{
			public void
			run()
			{
				synchronized( DeviceManagerImpl.this ){
				
					explicit_search++;
				}
				
				AESemaphore	sem = new AESemaphore( "DM:search" );
				
				DeviceManagerListener	dm_listener =
					new DeviceManagerListener()
					{
						public void
						deviceAdded(
							Device		device )
						{
							listener.deviceFound( device );
						}
						
						public void
						deviceChanged(
							Device		device )
						{
						}
						
						public void
						deviceAttentionRequest(
							Device		device )
						{	
						}
						
						public void
						deviceRemoved(
							Device		device )
						{
						}
					};
					
				try{
					addListener( dm_listener );
				
					upnp_manager.search();
					
					sem.reserve( millis );
					
				}finally{
					
					synchronized( DeviceManagerImpl.this ){
						
						explicit_search--;
					}
					
					removeListener( dm_listener );
					
					listener.complete();
				}
			}
		}.start();
	}
	
	protected DeviceImpl
	getDevice(
		String		id )
	{
		synchronized( this ){

			return( device_map.get( id ));
		}
	}
	
	protected DeviceImpl
	addDevice(
		DeviceImpl		device )
	{
			// for xbox (currently) we automagically replace a manual entry with an auto one as we may have
			// added the manual one when receiving a previous browse before getting the UPnP renderer details
			
		DeviceImpl	existing = null;
		
		synchronized( this ){
						
			existing = device_map.get( device.getID());
			
			if ( existing != null ){
				
				existing.updateFrom( device );
												
			}else{
			
				if ( device.getType() == Device.DT_MEDIA_RENDERER ){
					
					DeviceMediaRenderer renderer = (DeviceMediaRenderer)device;
					
					if ( renderer.getRendererSpecies() == DeviceMediaRenderer.RS_XBOX && !renderer.isManual()){
						
						for ( DeviceImpl d: device_list ){
							
							if ( d.getType() == Device.DT_MEDIA_RENDERER ){
								
								DeviceMediaRenderer r = (DeviceMediaRenderer)d;
								
								if ( r.getRendererSpecies() == DeviceMediaRenderer.RS_XBOX && r.isManual()){
									
									existing = d;

									log( "Merging " + device.getString() + " -> " + existing.getString());
										
									String	secondary_id = device.getID();
									
									existing.setSecondaryID( secondary_id );
									
									existing.updateFrom( device );
								}
							}
						}
					}
				}
			}
			
			if ( existing == null ){
			
				device_list.add( device );
				
				device_map.put( device.getID(), device );
			}
		}
		
		if ( existing != null ){
			
				// don't trigger config save here, if anything has changed it will have been handled
				// by the updateFrom call above
			
			deviceChanged( existing, false );
			
			return( existing );
		}
					
		device.initialise();
		
		device.alive();
		
		deviceAdded( device );
		
		configDirty();
		
		return( device );
	}
	
	protected void
	removeDevice(
		DeviceImpl		device )
	{
		synchronized( this ){
			
			DeviceImpl existing = device_map.remove( device.getID());
			
			if ( existing == null ){
				
				return;
			}
			
			device_list.remove( device );
			
			String secondary_id = device.getSecondaryID();
			
			if ( secondary_id != null ){
				
				device_map.remove( secondary_id );
			}
		}
		
		device.destroy();
		
		deviceRemoved( device );
		
		configDirty();
	}

	public DeviceImpl[]
  	getDevices()
	{
		synchronized( this ){
			
			return( device_list.toArray( new DeviceImpl[ device_list.size()] ));
		}
	}
  		
	public boolean
	getAutoSearch()
	{
		return( auto_search );
	}
	
	public void
	setAutoSearch(
		boolean	auto )
	{
		COConfigurationManager.setParameter( AUTO_SEARCH_CONFIG_KEY, auto );
	}
	
	protected boolean
	isExplicitSearch()
	{
		synchronized( this ){
			
			return( explicit_search > 0 );
		}
	}
	
	protected boolean
	isClosing()
	{
		return( closing );
	}
	
	protected void
	loadConfig()
	{
		if ( !FileUtil.resilientConfigFileExists( CONFIG_FILE )){
			
			return;
		}
		
		log( "Loading configuration" );
				
		synchronized( this ){
			
			Map map = FileUtil.readResilientConfigFile( CONFIG_FILE );
			
			List	l_devices = (List)map.get( "devices" );
			
			if ( l_devices != null ){
				
				for (int i=0;i<l_devices.size();i++){
					
					Map	m = (Map)l_devices.get(i);
					
					try{
						DeviceImpl device = DeviceImpl.importFromBEncodedMapStatic(this,  m );
						
						device_list.add( device );
						
						device_map.put( device.getID(), device );
						
						String secondary_id = device.getSecondaryID();
						
						if ( secondary_id != null ){
							
							device_map.put( secondary_id, device );
						}
							
						device.initialise();
					
						log( "    loaded " + device.getString());
						
					}catch( Throwable e ){
						
						log( "Failed to import subscription from " + m, e );
					}
				}
			}
		}
	}
	
	protected void
	configDirty(
		DeviceImpl		device,
		boolean			save_changes )
	{
		deviceChanged( device, save_changes );
	}
	
	protected void
	configDirty()
	{
		synchronized( this ){
			
			if ( config_dirty ){
				
				return;
			}
			
			config_dirty = true;
		
			new DelayedEvent( 
				"Subscriptions:save", 5000,
				new AERunnable()
				{
					public void 
					runSupport() 
					{
						synchronized( DeviceManagerImpl.this ){
							
							if ( !config_dirty ){

								return;
							}
							
							saveConfig();
						}	
					}
				});
		}
	}
	
	protected void
	saveConfig()
	{
		log( "Saving configuration" );
		
		synchronized( this ){
			
			if ( closing ){
				
					// to late to try writing
				
				return;
			}
			
			config_dirty 	= false;
			config_unclean	= false;
			
			if ( device_list.size() == 0 ){
				
				FileUtil.deleteResilientConfigFile( CONFIG_FILE );
				
			}else{
				
				Map map = new HashMap();
				
				List	l_devices = new ArrayList();
				
				map.put( "devices", l_devices );
				
				Iterator<DeviceImpl>	it = device_list.iterator();
				
				while( it.hasNext()){
					
					DeviceImpl device = it.next();
						
					try{
						Map d = new HashMap();
						
						device.exportToBEncodedMap( d );
						
						l_devices.add( d );
						
					}catch( Throwable e ){
						
						log( "Failed to save device " + device.getString(), e );
					}
				}
				
				FileUtil.writeResilientConfigFile( CONFIG_FILE, map );
			}
		}
	}
	
	protected void
	deviceAdded(
		DeviceImpl		device )
	{
		configDirty();
		
		// I'd rather put this in a listener, but for now this will ensure
		// it gets QOS'd even before any listeners are added
		try {
			PlatformDevicesMessenger.qosFoundDevice(device);
		} catch (Exception e) {
			Debug.out(e);
		}
		
		for ( DeviceManagerListener listener: listeners ){
			
			try{
				listener.deviceAdded( device );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	
	protected void
	deviceChanged(
		DeviceImpl		device,
		boolean			save_changes )
	{
		if ( save_changes ){
			
			configDirty();
			
		}else{
			
			config_unclean = true;
		}
		
		for ( DeviceManagerListener listener: listeners ){
			
			try{
				listener.deviceChanged( device );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected void
	deviceRemoved(
		DeviceImpl		device )
	{
		configDirty();
		
		for ( DeviceManagerListener listener: listeners ){
			
			try{
				listener.deviceRemoved( device );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected void
	requestAttention(
		DeviceImpl		device )
	{
		for ( DeviceManagerListener listener: listeners ){
			
			try{
				listener.deviceAttentionRequest( device );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected URL
	getStreamURL(
		TranscodeFileImpl		file )
	{
		IPCInterface ipc = upnp_manager.getUPnPAVIPC();
		
		if ( ipc != null ){

			try{
				DiskManagerFileInfo f = file.getTargetFile();
				
				String str = (String)ipc.invoke( "getContentURL", new Object[]{ f });
				
				if ( str != null && str.length() > 0 ){
					
					return( new URL( str ));
				}
			}catch( Throwable e ){
				
			}
		}
		
		return( null );
	}
	
	public TranscodeManagerImpl
	getTranscodeManager()
	{
		return( transcode_manager );
	}
	
	public UnassociatedDevice[]
	getUnassociatedDevices()
	{
		return( upnp_manager.getUnassociatedDevices());
	}
	
  	public void
  	addListener(
  		DeviceManagerListener		listener )
  	{
  		listeners.add( listener );
  	}
  	
  	public void
  	removeListener(
  		DeviceManagerListener		listener )
  	{
  		listeners.remove( listener );
  	}
  	
	protected synchronized AEDiagnosticsLogger
	getLogger()
	{
		if ( logger == null ){
			
			logger = AEDiagnostics.getLogger( LOGGER_NAME );
		}
		
		return( logger );
	}
	
	public void 
	log(
		String 		s,
		Throwable 	e )
	{
		AEDiagnosticsLogger diag_logger = getLogger();
		
		diag_logger.log( s );
		diag_logger.log( e );
	}
	
	public void 
	log(
		String 	s )
	{
		AEDiagnosticsLogger diag_logger = getLogger();
		
		diag_logger.log( s );
	}
 	
	public void
	generate(
		IndentWriter		writer )
	{
		writer.println( "Devices" );
			
		try{
			writer.indent();
			
			DeviceImpl[] devices = getDevices();
			
			for ( DeviceImpl device: devices ){
				
				device.generate( writer );
			}
			
			transcode_manager.generate( writer );
		}finally{
			
			writer.exdent();
		}
	}
}
