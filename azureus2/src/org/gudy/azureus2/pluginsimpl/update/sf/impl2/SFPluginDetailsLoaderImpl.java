/*
 * Created on 27-Apr-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.update.sf.impl2;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.URL;
import java.net.URLEncoder;
import java.io.IOException;
import java.io.InputStream;

import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginState;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.update.sf.*;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.core3.logging.*;

import com.aelitis.azureus.core.versioncheck.VersionCheckClient;

public class 
SFPluginDetailsLoaderImpl 
	implements SFPluginDetailsLoader, ResourceDownloaderListener
{
	private static final LogIDs LOGID = LogIDs.CORE;

	private static final String	site_prefix_default = Constants.SF_WEB_SITE;
	
	private static String site_prefix;
	
	static{
		try{
			Map data = VersionCheckClient.getSingleton().getVersionCheckInfo( VersionCheckClient.REASON_PLUGIN_UPDATE );
			
			byte[] b_sp = (byte[])data.get("plugin_update_url");
			
			if ( b_sp == null ){
				
				site_prefix = site_prefix_default;
				
			}else{
				
				site_prefix = new String( b_sp );
			}
		}catch( Throwable e ){
			
			site_prefix = site_prefix_default;
		}
	}
	private static String	base_url_params;
	
	static{
		
		base_url_params = "version=" + Constants.AZUREUS_VERSION + "&app=" + SystemProperties.getApplicationName();
		
		try{
			base_url_params += "&os=" + URLEncoder.encode(System.getProperty( "os.name"),"UTF-8" );
			
			base_url_params += "&osv=" + URLEncoder.encode(System.getProperty( "os.version" ),"UTF-8" );
			
			base_url_params += "&arch=" + URLEncoder.encode(System.getProperty( "os.arch"),"UTF-8" );
			
			base_url_params += "&ui=" + URLEncoder.encode(COConfigurationManager.getStringParameter("ui"),"UTF-8" );
			
			base_url_params += "&java=" + URLEncoder.encode(System.getProperty( "java.version" ),"UTF-8" );
			
			  try {
			      Class c = Class.forName( "org.eclipse.swt.SWT" );
			      
			      String swt_platform = (String)c.getMethod( "getPlatform", new Class[]{} ).invoke( null, new Object[]{} );
			      
			      base_url_params += "&swt_platform=" + swt_platform;
			      
			      Integer swt_version = (Integer)c.getMethod( "getVersion", new Class[]{} ).invoke( null, new Object[]{} );
			      
			      base_url_params += "&swt_version=" + swt_version;
			      
			  }catch( Throwable e ){
			  }
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	private static String	page_url 	= site_prefix + "update/pluginlist3.php?type=&" + base_url_params;

	
	static{
		try{
			PlatformManager pm = PlatformManagerFactory.getPlatformManager();
			
			if ( pm.hasCapability( PlatformManagerCapabilities.GetVersion )){
				
				page_url += "&pmv=" + pm.getVersion();
			}
			
		}catch( Throwable e ){
			
		}
	}
	
	private static SFPluginDetailsLoaderImpl		singleton;
	private static AEMonitor		class_mon		= new AEMonitor( "SFPluginDetailsLoader:class" );

	private static final int		RELOAD_MIN_TIME	= 60*60*1000;
	
	public static SFPluginDetailsLoader
	getSingleton()
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
				
				singleton	= new SFPluginDetailsLoaderImpl();
			}
			
			return( singleton );
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected boolean	plugin_ids_loaded;
	protected long		plugin_ids_loaded_at;
	
	protected List		plugin_ids;
	protected Map		plugin_map;
	
	protected List		listeners			= new ArrayList();
	
	protected ResourceDownloaderFactory rd_factory = ResourceDownloaderFactoryImpl.getSingleton();

	protected AEMonitor		this_mon		= new AEMonitor( "SFPluginDetailsLoader" );

	protected
	SFPluginDetailsLoaderImpl()
	{
		reset();
	}
	
	protected String
	getRelativeURLBase()
	{
		return( site_prefix );
	}
	
	protected void
	loadPluginList()
	
		throws SFPluginDetailsException
	{
		try{
			String	page_url_to_use = addEPIDS( page_url );
			
			ResourceDownloader dl = rd_factory.create( new URL(page_url_to_use));
			
			dl = rd_factory.getRetryDownloader( dl, 5 );
			
			dl.addListener( this );
			
			Properties	details = new Properties();
			
			InputStream is = dl.download();
			
			details.load( is );
			
			is.close();
			
			Iterator it = details.keySet().iterator();
			
			while( it.hasNext()){
				
				String	plugin_id 	= (String)it.next();
				
				String	data			= (String)details.get(plugin_id);

				int	pos = 0;
				
				List	bits = new ArrayList();
				
				while( pos < data.length()){
					
					int	p1 = data.indexOf(';',pos);
					
					if ( p1 == -1 ){
						
						bits.add( data.substring(pos).trim());
					
						break;
					}else{
						
						bits.add( data.substring(pos,p1).trim());
						
						pos = p1+1;
					}
				}
				
				if (bits.size() < 3) {
					Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
							"SF loadPluginList failed for plugin '" + plugin_id
									+ "'.  Details array is " + bits.size() + " (3 min)"));
				} else {
					String version = (String) bits.get(0);
					String cvs_version = (String) bits.get(1);
					String name = (String) bits.get(2);
					String category = "";

					if (bits.size() > 3) {
						category = (String) bits.get(3);
					}

					plugin_ids.add(plugin_id);

					plugin_map.put(plugin_id.toLowerCase(MessageText.LOCALE_ENGLISH), new SFPluginDetailsImpl(this,
							plugin_id, version, cvs_version, name, category));
				}
			}
			
			plugin_ids_loaded	= true;
			
			plugin_ids_loaded_at	= SystemTime.getCurrentTime();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			throw( new SFPluginDetailsException( "Plugin list load failed", e ));
		}
	}
	
	private String
	addEPIDS(
		String	str )
	{
		try{
			String pids = "";
			
			PluginInterface[] pis = PluginInitializer.getDefaultInterface().getPluginManager().getPluginInterfaces();
			
			for ( PluginInterface pi: pis ){
				
				PluginState ps = pi.getPluginState();
				
				if ( !( ps.isBuiltIn() || ps.isDisabled())){
					
					String version = pi.getPluginVersion();
					
					if ( version != null && Constants.compareVersions( version, "0" ) > 0 ){
						
						String pid = pi.getPluginID();
						
						if ( pid != null && pid.length() > 0 ){
					
							pids += pid + ":";
						}
					}
				}
			}

			str += "&epids=" + UrlUtils.encode( pids );
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
		
		return( str );
	}
	
	protected void
	loadPluginDetails(
		SFPluginDetailsImpl		details )
	
		throws SFPluginDetailsException
	{
		try{			
			String page_url_to_use = site_prefix + "update/pluginlist3.php?plugin="
					+ UrlUtils.encode(details.getId()) + "&" + base_url_params;
			
			page_url_to_use = addEPIDS( page_url_to_use );
			
			try{
				PluginInterface defPI = PluginInitializer.getDefaultInterface();
				PluginInterface pi = defPI == null ? null : defPI.getPluginManager().getPluginInterfaceByID( details.getId(), false );
	
				if ( pi != null ){
					
					String existing_version = pi.getPluginVersion();
				
					if ( existing_version != null ){
						
						page_url_to_use += "&ver_" + details.getId() + "=" + UrlUtils.encode( existing_version );
					}
				}
			}catch( Throwable e ){
				
				Debug.out( e );
			}
			
			ResourceDownloader p_dl = rd_factory.create( new URL( page_url_to_use ));
		
			p_dl = rd_factory.getRetryDownloader( p_dl, 5 );
		
			p_dl.addListener( this );
			
			InputStream is = p_dl.download();

			try {
  			if ( !processPluginStream( details, is )){
  							
  				throw( new SFPluginDetailsException( "Plugin details load fails for '" + details.getId() + "': data not found" ));
  			}
			} finally {
				is.close();
			}
					
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			throw( new SFPluginDetailsException( "Plugin details load fails", e ));
		}
	}

	protected boolean
	processPluginStream(SFPluginDetailsImpl details, InputStream is) {
    Properties properties = new Properties();
    try {
			properties.load(is);
			
			String pid = details.getId();
			
			String download_url = properties.getProperty(pid + ".dl_link", "");
			download_url = download_url.length() == 0 ? "<unknown>" : site_prefix + download_url;

			String author = properties.getProperty(pid + ".author", "");
			String desc = properties.getProperty(pid + ".description", "");
			String cvs_download_url = properties.getProperty(pid + ".dl_link_cvs", null);
			cvs_download_url = (cvs_download_url==null||cvs_download_url.length() == 0 )? "<unknown>" : site_prefix + cvs_download_url;

			String comment = properties.getProperty(pid + ".comment", "");
			// I don't think this one is ever set (not even in the old html scraping code)
			String info_url = properties.getProperty(pid + ".info_url", null);

			details.setDetails(
					download_url,
					author,
					cvs_download_url,
					desc,
					comment,
					info_url);
			return true;
		} catch (IOException e) {
			Debug.out(e);
		}
		return false;
	}
	
	public String[]
	getPluginIDs()
		
		throws SFPluginDetailsException
	{
		try{
			this_mon.enter();
		
			if ( !plugin_ids_loaded ){
				
				loadPluginList();
			}
			
			String[]	res = new String[plugin_ids.size()];
			
			plugin_ids.toArray( res );
			
			return( res );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public SFPluginDetails
	getPluginDetails(
		String		name )
	
		throws SFPluginDetailsException
	{
		try{
			this_mon.enter();
		
				// make sure details are loaded
			
			getPluginIDs();
			
			SFPluginDetails details = (SFPluginDetails)plugin_map.get(name.toLowerCase(MessageText.LOCALE_ENGLISH)); 
			
			if ( details == null ){
				
				throw( new SFPluginDetailsException( "Plugin '" + name + "' not found" ));
			}
			
			return( details );
			
		}finally{
			this_mon.exit();
		}
	}
	
	public SFPluginDetails[]
	getPluginDetails()
	
		throws SFPluginDetailsException	
	{
		String[]	plugin_ids = getPluginIDs();
		
		SFPluginDetails[]	res = new SFPluginDetails[plugin_ids.length];
	
		for (int i=0;i<plugin_ids.length;i++){
			
			res[i] = getPluginDetails(plugin_ids[i]);
		}
		
		return( res );
	}
	
	public void
	reportPercentComplete(
		ResourceDownloader	downloader,
		int					percentage )
	{
	}
	
	public void
	reportAmountComplete(
		ResourceDownloader	downloader,
		long				amount )
	{
	}
	
	public void
	reportActivity(
		ResourceDownloader	downloader,
		String				activity )
	{
		informListeners( activity );
	}
	
	public boolean
	completed(
		ResourceDownloader	downloader,
		InputStream			data )
	{
		return( true );
	}
	
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		informListeners( "Error: " + e.getMessage());
	}

	protected void
	informListeners(
		String		log )
	{
		for (int i=0;i<listeners.size();i++){
			
			((SFPluginDetailsLoaderListener)listeners.get(i)).log( log );
		}
	}
	
	public void
	reset()
	{
		try{
			this_mon.enter();
		
			long	now = SystemTime.getCurrentTime();
			
				// handle backward time changes
			
			if ( now < plugin_ids_loaded_at ){
				
				plugin_ids_loaded_at	= 0;
			}
			
			if ( now - plugin_ids_loaded_at > RELOAD_MIN_TIME ){
				
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID,
							"SFPluginDetailsLoader: resetting values"));
				
				plugin_ids_loaded	= false;
			
				plugin_ids		= new ArrayList();
				plugin_map			= new HashMap();
				
			}else{
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
							"SFPluginDetailsLoader: not resetting, " + "cache still valid"));
			}
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	addListener(
		SFPluginDetailsLoaderListener		l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		SFPluginDetailsLoaderListener		l )
	{
		listeners.remove(l);
	}
}
