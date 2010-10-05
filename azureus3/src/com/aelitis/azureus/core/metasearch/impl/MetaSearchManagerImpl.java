/*
 * Created on May 6, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.metasearch.impl;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.xml.util.XUXmlWriter;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.plugins.utils.FeatureManager.FeatureDetails;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence;
import org.gudy.azureus2.plugins.utils.search.Search;
import org.gudy.azureus2.plugins.utils.search.SearchException;
import org.gudy.azureus2.plugins.utils.search.SearchInitiator;
import org.gudy.azureus2.plugins.utils.search.SearchInstance;
import org.gudy.azureus2.plugins.utils.search.SearchListener;
import org.gudy.azureus2.plugins.utils.search.SearchObserver;
import org.gudy.azureus2.plugins.utils.search.SearchProvider;
import org.gudy.azureus2.plugins.utils.search.SearchProviderResults;
import org.gudy.azureus2.plugins.utils.search.SearchResult;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.utils.UtilitiesImpl;

import com.aelitis.azureus.core.custom.Customization;
import com.aelitis.azureus.core.custom.CustomizationManager;
import com.aelitis.azureus.core.custom.CustomizationManagerFactory;
import com.aelitis.azureus.core.messenger.config.PlatformMetaSearchMessenger;
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.MetaSearch;
import com.aelitis.azureus.core.metasearch.MetaSearchException;
import com.aelitis.azureus.core.metasearch.MetaSearchManager;
import com.aelitis.azureus.core.metasearch.MetaSearchManagerListener;
import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.ResultListener;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionManager;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.core.vuzefile.VuzeFileProcessor;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.ImportExportUtils;

public class 
MetaSearchManagerImpl
	implements MetaSearchManager, UtilitiesImpl.searchManager, AEDiagnosticsEvidenceGenerator
{
	private static final boolean AUTO_MODE_DEFAULT		= true;
	
	
	private static final String	LOGGER_NAME = "MetaSearch";
	
	private static final int REFRESH_MILLIS = 23*60*60*1000;
	
	private static MetaSearchManagerImpl singleton;	
	
	public static void
	preInitialise()
	{
		VuzeFileHandler.getSingleton().addProcessor(
			new VuzeFileProcessor()
			{
				public void
				process(
					VuzeFile[]		files,
					int				expected_types )
				{
					for (int i=0;i<files.length;i++){
						
						VuzeFile	vf = files[i];
						
						VuzeFileComponent[] comps = vf.getComponents();
						
						for (int j=0;j<comps.length;j++){
							
							VuzeFileComponent comp = comps[j];
							
							int	comp_type = comp.getType();
							
							if ( comp_type == VuzeFileComponent.COMP_TYPE_METASEARCH_TEMPLATE ){
								
								try{
									Engine e = 
										getSingleton().importEngine(
											comp.getContent(), 
											(expected_types & VuzeFileComponent.COMP_TYPE_METASEARCH_TEMPLATE) == 0 );
									
									comp.setProcessed();
									
									if ( e != null ){
										
										comp.setData( Engine.VUZE_FILE_COMPONENT_ENGINE_KEY, e );
									}
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
								}
							}else if ( comp_type == VuzeFileComponent.COMP_TYPE_METASEARCH_OPERATION ){
								
								getSingleton().addOperation( comp.getContent());
								
								comp.setProcessed();
							}
						}
					}
				}
			});	
		
		TorrentUtils.addTorrentAttributeListener(
				new TorrentUtils.torrentAttributeListener()
				{
					public void 
					attributeSet(
						TOTorrent 	torrent,
						String 		attribute, 
						Object 		value )
					{
						if ( 	attribute == TorrentUtils.TORRENT_AZ_PROP_OBTAINED_FROM && 
								!TorrentUtils.isReallyPrivate( torrent )){
							
							try{
								getSingleton().checkPotentialAssociations( torrent.getHash(), (String)value );
								
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
							}
						}
					}
				});
	}
	
	public static synchronized MetaSearchManagerImpl
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new MetaSearchManagerImpl();
		}
		return( singleton );
	}
	
	private MetaSearchImpl	meta_search;
	private AsyncDispatcher	dispatcher = new AsyncDispatcher( 10000 );
	
	private AESemaphore	initial_refresh_sem = new AESemaphore( "MetaSearch:initrefresh" );
	
	private AESemaphore	refresh_sem = new AESemaphore( "MetaSearch:refresh", 1 );
	
	private boolean	checked_customization;
	
	private AsyncDispatcher					op_dispatcher 	= new AsyncDispatcher(5*1000);
	private List<MetaSearchManagerListener>	listeners 		= new ArrayList<MetaSearchManagerListener>();
	private List<Map>						operations		= new ArrayList<Map>();
	
	private String		extension_key;
	
	private Map<String,EngineImpl>	potential_associations = 
		new LinkedHashMap<String,EngineImpl>(32,0.75f,true)
		{
			protected boolean 
			removeEldestEntry(
		   		Map.Entry<String,EngineImpl> eldest) 
			{
				return size() > 32;
			}
		};
	protected
	MetaSearchManagerImpl()
	{
		meta_search = new MetaSearchImpl( this );
		
		AEDiagnostics.addEvidenceGenerator( this );
		
		extension_key = COConfigurationManager.getStringParameter( "metasearch.extkey.latest", "" );

		if ( extension_key.length() == 0 ){
			
			extension_key = null;
		}
		
		setupExtensions();
		
		SimpleTimer.addPeriodicEvent(
			"MetaSearchRefresh",
			REFRESH_MILLIS,
			new TimerEventPerformer()
			{
				public void 
				perform(
					TimerEvent 	event ) 
				{
					refresh();
				}
			});
		
		refresh();
		
		UtilitiesImpl.addSearchManager( this );
	}
	
	public void 
	addProvider(
		PluginInterface		pi,
		SearchProvider 		provider ) 
	{
		String	id = pi.getPluginID() + "." + provider.getProperty( SearchProvider.PR_NAME );
		
		try{
			meta_search.importFromPlugin( id, provider );
			
		}catch( Throwable e ){
			
			Debug.out( "Failed to add search provider '" + id + "' (" + provider + ")", e );
		}
	}
	
	public SearchProvider[]
  	getProviders()
	{
		Engine[] engines = meta_search.getEngines( true, false );
		
		SearchProvider[] result = new SearchProvider[engines.length];
		
		for (int i=0;i<engines.length;i++){
				
			result[i] = new engineInfo( engines[i] );
		}
		
		return( result );
	}
	
	public Search 
	createSearch(
		String 		provider_ids,
		String 		properties_str )
	
		throws SearchException 
	{
		String[]	bits = XUXmlWriter.splitWithEscape( provider_ids, ',' );
		
		long[]	pids = new long[ bits.length ];
		
		for ( int i=0; i<bits.length; i++ ){
			
			pids[i] = Long.parseLong( bits[i] );
		}

		Map<String,String>	properties = new HashMap<String, String>();
		
		bits = XUXmlWriter.splitWithEscape( properties_str, ',' );

		for ( int i=0; i<bits.length; i++ ){

			String[] x = XUXmlWriter.splitWithEscape( bits[i], '=' );
			
			properties.put( x[0].trim(), x[1].trim());
		}
		
		return( createSearch( pids, properties, null ));
	}
	
  	public Search
  	createSearch(
  		SearchProvider[]	providers,
  		Map<String,String>	properties,
  		SearchListener		listener )
  	
  		throws SearchException
  	{
  		long[]	pids;
  		
  		if ( providers == null ){
  			
  			pids = new long[0];
  			
  		}else{
  			
  			pids = new long[providers.length];
  			
  			for (int i=0;i<pids.length;i++){
			
  				Long	id = (Long)providers[i].getProperty( SearchProvider.PR_ID );
			
  				if ( id == null ){
				
  					throw( new SearchException( "Unknown provider - no id available" ));
  				}
  				
  				pids[i] = id;
			}
  		}
  		
  		return( createSearch( pids, properties, listener ));
  	}
  	
  	protected Search
  	createSearch(
  		long[]				provider_ids,
  		Map<String,String>	properties,
  		SearchListener		listener )
  	
  		throws SearchException
  	{
		List<SearchParameter>	sps = new ArrayList<SearchParameter>();

 		String	search_term = properties.get( SearchInitiator.PR_SEARCH_TERM );
 		
 		if ( search_term == null ){
 			
 			throw( new SearchException( "Search term is mandatory" ));
 		}
 		
		sps.add( new SearchParameter( "s", search_term ));

 		String	mature 		= properties.get( SearchInitiator.PR_MATURE );
  		
		if ( mature != null ){
			
			sps.add( new SearchParameter( "m", mature.toString()));
		}
	
		SearchParameter[] parameters = (SearchParameter[])sps.toArray(new SearchParameter[ sps.size()] );

		Map<String,String>	context = new HashMap<String, String>();
		
		context.put( Engine.SC_FORCE_FULL, "true" );

		String	headers 		= null;
		int		max_per_engine	= 256;
		
		SearchObject search = new SearchObject( listener );
		
		Engine[]	used_engines;
		
		if ( provider_ids.length == 0 ){
			
			used_engines = getMetaSearch().search( search, parameters, headers, context, max_per_engine );

		}else{
			
			List<Engine>	selected_engines = new ArrayList<Engine>();
			
			for ( long id: provider_ids ){
				
				Engine engine = meta_search.getEngine( id );
				
				if ( engine == null ){
					
					throw( new SearchException( "Unknown engine id - " + id ));
					
				}else{
					
					selected_engines.add( engine );
				}
			}
			
			Engine[] engines = selected_engines.toArray( new Engine[ selected_engines.size()] );
	
			used_engines = getMetaSearch().search( engines, search, parameters, headers, context, max_per_engine );
		}
		
		search.setEnginesUsed( used_engines );
		
		return( search );
  	}
	
	protected void
	refresh()
	{
		dispatcher.dispatch(
			new AERunnable()
			{
				public void 
				runSupport() 
				{
					if ( dispatcher.getQueueSize() == 0 ){
						
						try{
							syncRefresh();
														
						}catch( Throwable e ){
							
						}
					}
				}
			});
	}
	
	protected void
	ensureEnginesUpToDate()
	{
		long timeout = meta_search.getEngineCount() == 0?(30*1000):(10*1000);
		
		if ( !initial_refresh_sem.reserve( timeout )){
			
			log( "Timeout waiting for initial refresh to complete, continuing" );
		}
	}
	
	protected void
	syncRefresh()
	
		throws MetaSearchException
	{
		boolean refresh_completed 	= false;
		boolean	first_run			= false;
		
		try{
			refresh_sem.reserve();
			
			first_run = COConfigurationManager.getBooleanParameter( "metasearch.refresh.first_run", true );
			
			if ( !checked_customization ){
				
				checked_customization = true;
				
				CustomizationManager cust_man = CustomizationManagerFactory.getSingleton();
				
				Customization cust = cust_man.getActiveCustomization();
				
				if ( cust != null ){
					
					String cust_name 	= COConfigurationManager.getStringParameter( "metasearch.custom.name", "" );
					String cust_version = COConfigurationManager.getStringParameter( "metasearch.custom.version", "0" );
					
					boolean	new_name 	= !cust_name.equals( cust.getName());
					boolean	new_version = org.gudy.azureus2.core3.util.Constants.compareVersions( cust_version, cust.getVersion() ) < 0;
					
					if ( new_name || new_version ){

						log( "Customization: checking templates for " + cust.getName() + "/" + cust.getVersion());
						
						try{
							InputStream[] streams = cust.getResources( Customization.RT_META_SEARCH_TEMPLATES );
							
							if ( streams.length > 0 && new_name ){
								
									// reset engines
								
								log( "    setting auto-mode to false" );
								
								setAutoMode( false );
								
								/*
								Engine[]	engines = meta_search.getEngines( false, false );

								for (int i=0;i<engines.length;i++){
									
									Engine engine = engines[i];
									
									if ( engine.getSelectionState()) == Engine.SEL_STATE_MANUAL_SELECTED ){
										
									}
								}
								*/
							}
							for (int i=0;i<streams.length;i++){
								
								InputStream is = streams[i];
								
								try{
									VuzeFile vf = VuzeFileHandler.getSingleton().loadVuzeFile(is);
									
									if ( vf != null ){
										
										VuzeFileComponent[] comps = vf.getComponents();
										
										for (int j=0;j<comps.length;j++){
											
											VuzeFileComponent comp = comps[j];
											
											if ( comp.getType() == VuzeFileComponent.COMP_TYPE_METASEARCH_TEMPLATE ){
												
												try{
													Engine e = 
														getSingleton().importEngine( comp.getContent(), false ); 
													
													log( "    updated " + e.getName());
													
													e.setSelectionState( Engine.SEL_STATE_MANUAL_SELECTED );
													
												}catch( Throwable e ){
													
													Debug.printStackTrace(e);
												}
											}
										}
									}
								}finally{
									
									try{
										is.close();
										
									}catch( Throwable e ){
									}
								}
							}
						}finally{
							
							COConfigurationManager.setParameter( "metasearch.custom.name", cust.getName());
							COConfigurationManager.setParameter( "metasearch.custom.version", cust.getVersion());
						}
					}
				}
			}
			
			log( "Refreshing engines" );
					
				// featured templates are always shown - can't be deselected
				// popular ones are selected if in 'auto' mode
				// manually selected ones are, well, manually selected
			
			Map<Long,PlatformMetaSearchMessenger.templateInfo>		vuze_selected_ids 		= new HashMap<Long, PlatformMetaSearchMessenger.templateInfo>();
			Map<Long,PlatformMetaSearchMessenger.templateInfo>		vuze_preload_ids 		= new HashMap<Long,PlatformMetaSearchMessenger.templateInfo>();
			
			Set<Long>		featured_ids 			= new HashSet<Long>();
			Set<Long>		popular_ids 			= new HashSet<Long>();
			Set<Long>		manual_vuze_ids 		= new HashSet<Long>();
			
			boolean		auto_mode = isAutoMode();
					
			Engine[]	engines = meta_search.getEngines( false, false );
	
			String	fud = meta_search.getFUD();
			
			try{
				PlatformMetaSearchMessenger.templateInfo[] featured = PlatformMetaSearchMessenger.listFeaturedTemplates( extension_key, fud );
				
				String featured_str = "";
				
				for (int i=0;i<featured.length;i++){
					
					PlatformMetaSearchMessenger.templateInfo template = featured[i];
					
					if ( !template.isVisible()){
						
						continue;
					}
					
					Long key = new Long( template.getId());
					
					vuze_selected_ids.put( key,	template );
					
					featured_ids.add( key );
					
					featured_str += (featured_str.length()==0?"":",") + key;
				}
					
				log( "Featured templates: " + featured_str );
				
				if ( auto_mode || first_run ){
					
					PlatformMetaSearchMessenger.templateInfo[] popular = PlatformMetaSearchMessenger.listTopPopularTemplates( extension_key, fud );
					
					String popular_str = "";
					String preload_str = "";
					
					for (int i=0;i<popular.length;i++){
						
						PlatformMetaSearchMessenger.templateInfo template = popular[i];
						
						if ( !template.isVisible()){
							
							continue;
						}
						
						Long	key = new Long( template.getId());
						
						if ( auto_mode ){
							
							if ( !vuze_selected_ids.containsKey( key )){
								
								vuze_selected_ids.put( key,	template );
								
								popular_ids.add( key );
								
								popular_str += (popular_str.length()==0?"":",") + key;
							}
						}else{
							
							if ( !vuze_preload_ids.containsKey( key )){
								
								vuze_preload_ids.put( key, template );
																
								preload_str += (preload_str.length()==0?"":",") + key;
							}
						}
					}
					
					log( "Popular templates: " + popular_str );
					
					if ( preload_str.length() > 0 ){
						
						log( "Pre-load templates: " + popular_str );
					}
				}
						
					// pick up explicitly selected vuze ones
				
				String manual_str = "";
				
				for (int i=0;i<engines.length;i++){
					
					Engine	engine = engines[i];
					
					Long key = new Long( engine.getId());
					
					if ( 	engine.getSource() == Engine.ENGINE_SOURCE_VUZE &&
							engine.getSelectionState() == Engine.SEL_STATE_MANUAL_SELECTED &&
							!vuze_selected_ids.containsKey( key )){
						
						manual_vuze_ids.add( key );
					}
				}
				
				if ( manual_vuze_ids.size() > 0 ){
					
					long[]	manual_ids = new long[manual_vuze_ids.size()];
					
					Iterator<Long> it = manual_vuze_ids.iterator();
					
					int	pos = 0;
					
					while( it.hasNext()){
						
						manual_ids[pos++] = it.next().longValue();
					}
					
					PlatformMetaSearchMessenger.templateInfo[] manual = PlatformMetaSearchMessenger.getTemplateDetails( extension_key, manual_ids );
										
					for (int i=0;i<manual.length;i++){
						
						PlatformMetaSearchMessenger.templateInfo template = manual[i];
						
						if ( !template.isVisible()){
							
							continue;
						}
						
						Long	key = new Long( template.getId());
													
						vuze_selected_ids.put( key, template );
														
						manual_str += (manual_str.length()==0?"":",") + key;
					}
				}
				
				log( "Manual templates: " + manual_str );
				
				Map<Long,Engine> existing_engine_map = new HashMap<Long,Engine>();
				
				String existing_str = "";
				
				for (int i=0;i<engines.length;i++){
					
					Engine	engine = engines[i];
					
					Long key = new Long( engine.getId());
	
					existing_engine_map.put( key, engine );
					
					existing_str += (existing_str.length()==0?"":",") + key + 
										"[source=" + Engine.ENGINE_SOURCE_STRS[engine.getSource()] +
										",type=" + engine.getType() + 
										",selected=" + Engine.SEL_STATE_STRINGS[engine.getSelectionState()] + "]";
				}
				
				log( "Existing templates: " + existing_str );
				
					// we've compiled a list of the engines we should have and their latest dates
				
					// update any that are out of date
				
				Iterator<Map.Entry<Long,PlatformMetaSearchMessenger.templateInfo>> it = vuze_selected_ids.entrySet().iterator();
				
				while( it.hasNext()){
					
					Map.Entry<Long,PlatformMetaSearchMessenger.templateInfo> entry = it.next();
					
					vuze_preload_ids.remove( entry.getKey());
					
					long	id 			= entry.getKey().longValue();
					
					PlatformMetaSearchMessenger.templateInfo template = entry.getValue();
					
					long	modified 	= template.getModifiedDate();
									
					Engine this_engine = (Engine)existing_engine_map.get( new Long(id));
					
					boolean	update = this_engine == null || this_engine.getLastUpdated() < modified;
	
					if ( update ){
						
						PlatformMetaSearchMessenger.templateDetails details = PlatformMetaSearchMessenger.getTemplate( extension_key, id );
	
						log( "Downloading definition of template " + id );
						log( details.getValue());
						
						if ( details.isVisible()){
							
							try{
								this_engine = 
									meta_search.importFromJSONString( 
										details.getType()==PlatformMetaSearchMessenger.templateDetails.ENGINE_TYPE_JSON?Engine.ENGINE_TYPE_JSON:Engine.ENGINE_TYPE_REGEX,
										details.getId(),
										details.getModifiedDate(),
										details.getRankBias(),
										details.getName(),
										details.getValue());
								
								this_engine.setSource( Engine.ENGINE_SOURCE_VUZE );
																
								meta_search.addEngine( this_engine );
								
							}catch( Throwable e ){
								
								log( "Failed to import engine '" + details.getValue() + "'", e );
							}
						}	
					}else if ( this_engine.getRankBias() != template.getRankBias()){
						
						this_engine.setRankBias( template.getRankBias());
						
						log( "Updating rank bias for " + this_engine.getString() + " to " + template.getRankBias());

					}else{
						
						log( "Not updating " + this_engine.getString() + " as unchanged" );
					}
					
					if ( this_engine != null ){
							
						int sel_state = this_engine.getSelectionState();
						
						if ( sel_state == Engine.SEL_STATE_DESELECTED ){
						
							log( "Auto-selecting " + this_engine.getString());
							
							this_engine.setSelectionState( Engine.SEL_STATE_AUTO_SELECTED );
							
						}else if ( auto_mode && sel_state == Engine.SEL_STATE_MANUAL_SELECTED ){
							
							log( "Switching Manual to Auto select for " + this_engine.getString());
							
							this_engine.setSelectionState( Engine.SEL_STATE_AUTO_SELECTED );
						}
					}
				}
				
					// do any pre-loads
				
				it = vuze_preload_ids.entrySet().iterator();
				
				while( it.hasNext()){
					
					Map.Entry<Long,PlatformMetaSearchMessenger.templateInfo> entry = it.next();
					
					long	id 			= ((Long)entry.getKey()).longValue();
									
					Engine this_engine = (Engine)existing_engine_map.get( new Long(id));
						
					if ( this_engine == null ){
						
						PlatformMetaSearchMessenger.templateDetails details = PlatformMetaSearchMessenger.getTemplate( extension_key, id );
	
						log( "Downloading pre-load definition of template " + id );
						log( details.getValue());
						
						if ( details.isVisible()){
							
							try{
								this_engine = 
									meta_search.importFromJSONString( 
										details.getType()==PlatformMetaSearchMessenger.templateDetails.ENGINE_TYPE_JSON?Engine.ENGINE_TYPE_JSON:Engine.ENGINE_TYPE_REGEX,
										details.getId(),
										details.getModifiedDate(),
										details.getRankBias(),
										details.getName(),
										details.getValue());
								
								this_engine.setSource( Engine.ENGINE_SOURCE_VUZE );
								
								this_engine.setSelectionState( Engine.SEL_STATE_DESELECTED );
																
								meta_search.addEngine( this_engine );
								
							}catch( Throwable e ){
								
								log( "Failed to import engine '" + details.getValue() + "'", e );
							}
						}			
					}
				}
				
					// deselect any not in use
				
				for (int i=0;i<engines.length;i++){
					
					Engine	engine = engines[i];
					
					if ( 	engine.getSource() == Engine.ENGINE_SOURCE_VUZE &&
							engine.getSelectionState() == Engine.SEL_STATE_AUTO_SELECTED &&
							!vuze_selected_ids.containsKey( new Long( engine.getId()))){
						
						log( "Deselecting " + engine.getString() + " as no longer visible on Vuze");
						
						engine.setSelectionState( Engine.SEL_STATE_DESELECTED );
					}
				}
				
					// finally pick up any unreported selection changes and re-affirm positive selections
				
				for (int i=0;i<engines.length;i++){
					
					Engine	engine = engines[i];
					
					if ( 	engine.getSource() == Engine.ENGINE_SOURCE_VUZE &&
							engine.getSelectionState() == Engine.SEL_STATE_MANUAL_SELECTED ){
						
						engine.recordSelectionState();
						
					}else{
						
						engine.checkSelectionStateRecorded();
					}
				}
				
				refresh_completed = true;
				
			}catch( Throwable e ){
				
				log( "Refresh failed", e );
				
				throw( new MetaSearchException( "Refresh failed", e ));
			}
		}finally{
			
			if ( first_run && refresh_completed ){
				
				COConfigurationManager.setParameter( "metasearch.refresh.first_run", false );
			}
			
			refresh_sem.release();
			
			initial_refresh_sem.releaseForever();
		}
	}
	
	public MetaSearch 
	getMetaSearch() 
	{
		return( meta_search );
	}
	
	public boolean
	isAutoMode()
	{
		return( COConfigurationManager.getBooleanParameter( "metasearch.auto.mode", AUTO_MODE_DEFAULT ));
	}
	
	protected void
	setAutoMode(
		boolean	auto )
	{
		COConfigurationManager.setParameter( "metasearch.auto.mode", auto );
	}
	
	public void
	setSelectedEngines(
		long[]		ids,
		boolean		auto )
	
		throws MetaSearchException
	{
		try{
			String	s = "";
			
			for (int i=0;i<ids.length;i++){
				
				s += (i==0?"":",") + ids[i];
			}
			
			log( "setSelectedIds: " + s + ", auto=" + auto );
			
				// first update state of auto and existing engines 
			
			COConfigurationManager.setParameter( "metasearch.auto.mode", auto );

			Engine[]	engines = meta_search.getEngines( false, false );
			
			Map<Long,Engine>	engine_map = new HashMap<Long,Engine>();
			
			for( int i=0;i<engines.length;i++){
				
				engine_map.put( new Long( engines[i].getId()), engines[i] );
			}
				
			Set<Engine> selected_engine_set = new HashSet<Engine>();
			
			for (int i=0;i<ids.length;i++){
				
				long	 id = ids[i];
				
				Engine existing = (Engine)engine_map.get(new Long(id));
				
				if ( existing != null ){
					
					existing.setSelectionState( Engine.SEL_STATE_MANUAL_SELECTED );
					
					selected_engine_set.add( existing );
				}
			}
		
				// now refresh - this will pick up latest state of things
			
			syncRefresh();
			
			engines = meta_search.getEngines( false, false );

				// next add in any missing engines
			
			for (int i=0;i<ids.length;i++){
				
				long	 id = ids[i];
				
				Engine existing = (Engine)engine_map.get(new Long(id));
				
				if ( existing == null ){
														
					PlatformMetaSearchMessenger.templateDetails details = PlatformMetaSearchMessenger.getTemplate( extension_key, id );
	
					log( "Downloading definition of template " + id );
					log( details.getValue());
					
					Engine new_engine = 
						meta_search.importFromJSONString( 
							details.getType()==PlatformMetaSearchMessenger.templateDetails.ENGINE_TYPE_JSON?Engine.ENGINE_TYPE_JSON:Engine.ENGINE_TYPE_REGEX,
							details.getId(),
							details.getModifiedDate(),
							details.getRankBias(),
							details.getName(),
							details.getValue());
							
					new_engine.setSelectionState( Engine.SEL_STATE_MANUAL_SELECTED );
										
					new_engine.setSource( Engine.ENGINE_SOURCE_VUZE );
						
					meta_search.addEngine( new_engine );
					
					selected_engine_set.add( new_engine );
				}
			}
			
				// deselect any existing manually selected ones that are no longer selected
			
			for( int i=0;i<engines.length;i++){

				Engine e = engines[i];
				
				if ( e.getSelectionState() == Engine.SEL_STATE_MANUAL_SELECTED ){
					
					if ( !selected_engine_set.contains( e )){
				
						e.setSelectionState( Engine.SEL_STATE_DESELECTED  );
					}
				}
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			if ( e instanceof MetaSearchException ){
				
				throw((MetaSearchException)e);
			}
			
			throw( new MetaSearchException( "Failed to set selected engines", e ));
		}
	}
	
	public Engine
	addEngine(
		long		id,
		int			type,
		String		name,
		String		json_value )
	
		throws MetaSearchException
	{
		if ( id == -1 ){
			
			id = getLocalTemplateID();
		}
		
		try{
			Engine engine = 
				meta_search.importFromJSONString( 
					type, id, SystemTime.getCurrentTime(), 1, name, json_value );
			
			engine.setSource( Engine.ENGINE_SOURCE_LOCAL );
			
			engine.setSelectionState( Engine.SEL_STATE_MANUAL_SELECTED );
			
			meta_search.addEngine( engine );
			
			return( engine );
			
		}catch( Throwable e ){
			
			throw( new MetaSearchException( "Failed to add engine", e ));
		}
	}
	
	public Engine
	importEngine(
		Map			map,
		boolean		warn_user )
	
		throws MetaSearchException
	{
		try{
			EngineImpl engine = (EngineImpl)meta_search.importFromBEncodedMap(map);
			
			long	id = engine.getId();
			
			Engine existing = meta_search.getEngine( id );
			
			if ( existing != null ){
				
				if ( existing.sameLogicAs( engine )){
					
					if ( warn_user ){
						
						UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
						
						String details = MessageText.getString(
								"metasearch.addtemplate.dup.desc",
								new String[]{ engine.getName() });
						
						ui_manager.showMessageBox(
								"metasearch.addtemplate.dup.title",
								"!" + details + "!",
								UIManagerEvent.MT_OK );
					}
					
					return( existing );
				}
			}else{
									
				try{						
					Engine[] engines = meta_search.getEngines( false, false );
						
					for ( Engine e: engines ){
							
						if ( e.sameLogicAs( engine )){
								
							return( e );
						}
					}
				}catch( Throwable e ){
					
				}
			}
			
			if ( warn_user ){
				
				UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
				
				String details = MessageText.getString(
						"metasearch.addtemplate.desc",
						new String[]{ engine.getName() });
				
				long res = ui_manager.showMessageBox(
						"metasearch.addtemplate.title",
						"!" + details + "!",
						UIManagerEvent.MT_YES | UIManagerEvent.MT_NO );
				
				if ( res != UIManagerEvent.MT_YES ){
					
					throw( new MetaSearchException( "User declined the template" ));
				}
			}
				// if local template then we try to use the id as is otherwise we emsure that
				// it is a local one
			
			if ( id >= 0 && id < Integer.MAX_VALUE ){
				
				id = getLocalTemplateID();
				
				engine.setId( id );
			}
			
			engine.setSource( Engine.ENGINE_SOURCE_LOCAL );
			
			engine.setSelectionState( Engine.SEL_STATE_MANUAL_SELECTED );
			
			meta_search.addEngine( engine );
			
			if ( warn_user ){
				
				UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
				
				String details = MessageText.getString(
						"metasearch.addtemplate.done.desc",
						new String[]{ engine.getName() });
				
				ui_manager.showMessageBox(
						"metasearch.addtemplate.done.title",
						"!" + details + "!",
						UIManagerEvent.MT_OK );
			}
			
			return( engine );
			
		}catch( Throwable e ){
			
			if ( warn_user ){
				
				UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
				
				String details = MessageText.getString(
						"metasearch.addtemplate.failed.desc",
						new String[]{ Debug.getNestedExceptionMessage(e) });
				
				ui_manager.showMessageBox(
						"metasearch.addtemplate.failed.title",
						"!" + details + "!",
						UIManagerEvent.MT_OK );
			}
			
			throw( new MetaSearchException( "Failed to add engine", e ));
		}
	}
	
	protected void
	addPotentialAssociation(
		EngineImpl		engine,
		String			key )
	{		
		if ( engine.isShareable() && !engine.isAuthenticated()){
			
			synchronized( potential_associations ){
				
				potential_associations.put( key, engine );
			}
		}
	}
	
	private void
	checkPotentialAssociations(
		byte[]		hash,
		String		key )
	{
		EngineImpl	engine;
		
		synchronized( potential_associations ){
			
			engine = potential_associations.remove( key );
		}
	
		if ( engine != null ){
	
			try{
				VuzeFile vf = engine.exportToVuzeFile( true );
				
				byte[] bytes = vf.exportToBytes();
												
				String url_str = "vuze://?body=" + new String( bytes, Constants.BYTE_ENCODING );
								
				SubscriptionManager sub_man = SubscriptionManagerFactory.getSingleton();
		
				Subscription subs =
					sub_man.createSingletonRSS(
						vf.getName() + ": " + engine.getName(),
						new URL( url_str ),
						Integer.MAX_VALUE );
			
				subs.setSubscribed( true );
			
				subs.addAssociation( hash );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	public Engine[]
	loadFromVuzeFile(
		File		file )
	{
		VuzeFile vf = VuzeFileHandler.getSingleton().loadVuzeFile( file.getAbsolutePath());
		
		if ( vf != null ){
			
			return( loadFromVuzeFile( vf ));
		}
		
		return( new Engine[0]);
	}
	
	public Engine[]
	loadFromVuzeFile(
		VuzeFile		vf )
	{
		List<Engine>	result = new ArrayList<Engine>();
		
		VuzeFileComponent[] comps = vf.getComponents();
		
		for (int j=0;j<comps.length;j++){
			
			VuzeFileComponent comp = comps[j];
			
			if ( comp.getType() == VuzeFileComponent.COMP_TYPE_METASEARCH_TEMPLATE ){
				
				try{
					result.add( importEngine( comp.getContent(), false ));
											
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		return(result.toArray(new Engine[result.size()]));
	}
	
	public long
	getLocalTemplateID()
	{
		synchronized( this ){
			
			Random random = new Random();
			
			while( true ){
			
				long id = ((long)Integer.MAX_VALUE) + random.nextInt( Integer.MAX_VALUE );
				
				if ( meta_search.getEngine( id ) == null ){
					
					return( id );
				}
			}
		}
	}
	
	public void
	addListener(
		MetaSearchManagerListener		listener )
	{			
		synchronized( listeners ){
			
			listeners.add( listener );
		}
			
		dispatchOps();
	}
	
	public void
	removeListener(
		MetaSearchManagerListener		listener )
	{
		
	}
	
	private void
	addOperation(
		Map		map )
	{
		synchronized( listeners ){
			
			operations.add( map );
		}
		
		dispatchOps();
	}
	
	private void
	dispatchOps()
	{
		op_dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					List<MetaSearchManagerListener>	l;
					List<Map>						o;
					
					synchronized( listeners ){
					
						if ( listeners.size() == 0 || operations.size() == 0 ){
							
							return;
						}
						
						l = new ArrayList<MetaSearchManagerListener>( listeners );
						
						o = new ArrayList<Map>( operations );
						
						operations.clear();
					}
					
					for ( MetaSearchManagerListener listener: l ){
						
						for ( Map operation: o ){
					
							try{

								int	type = ImportExportUtils.importInt( operation, "type", -1 );
								
								if ( type == 1 ){
									
									String	term = ImportExportUtils.importString( operation, "term", null );
								
									if ( term == null ){
										
										Debug.out( "search term missing" );
										
									}else{
										
										listener.searchRequest( term );
									}
								}else{
								
									Debug.out( "unknown operation type " + type );
								}								
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
					}
				}
			});
	}
	
	private void
	setupExtensions()
	{
		PluginInterface pi = PluginInitializer.getDefaultInterface();
		
		final FeatureManager fm = pi.getUtilities().getFeatureManager();
					
		fm.addListener(
			new FeatureManager.FeatureManagerListener()
			{
				public void
				licenceAdded(
					Licence	licence )
				{
					getExtensions( fm, false );
				}
				
				public void
				licenceChanged(
					Licence	licence )
				{
					getExtensions( fm, false );
				}
				
				public void
				licenceRemoved(
					Licence	licence )
				{
					getExtensions( fm, false );
				}
			});
		
		if ( pi.getPluginState().isInitialisationComplete()){
			
			getExtensions( fm, true );
			
		}else{
			
			pi.addListener(
				new PluginListener()
				{
					public void
					initializationComplete()
					{
						getExtensions( fm, false );
					}
					
					public void
					closedownInitiated()
					{
					}
					
					public void
					closedownComplete()
					{	
					}
				});
		}
	}
	
	private void
	getExtensions(
		FeatureManager		fm,
		boolean				init )
	{
		String	existing_ext 	= extension_key;
		String	latest_ext		= null;
		
		FeatureDetails[] fds = fm.getFeatureDetails( "core" );
		
		for ( FeatureDetails fd: fds ){
				
			if ( !fd.hasExpired()){
				
				String finger_print = (String)fd.getProperty( FeatureDetails.PR_FINGERPRINT );
					
				if ( finger_print != null ){
				
					latest_ext = fd.getLicence().getShortID() + "-" + finger_print;
					
					break;
				}
			}
		}

		if ( existing_ext != latest_ext ){
			
			if ( existing_ext == null || latest_ext == null || !existing_ext.equals( latest_ext )){
				
				extension_key	= latest_ext;
				
				COConfigurationManager.setParameter( "metasearch.extkey.latest", latest_ext==null?"":latest_ext );
				
				if ( !init ){
					
					refresh();
				}
			}
		}
	}
	
	protected String
	getExtensionKey()
	{
		return( extension_key );
	}
	
	public void 
	log(
		String 		s,
		Throwable 	e )
	{
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger( LOGGER_NAME );
		
		diag_logger.log( s );
		diag_logger.log( e );
		
		if ( ConstantsVuze.DIAG_TO_STDOUT ){
			
			System.out.println(Thread.currentThread().getName() + "|"
					+ System.currentTimeMillis() + "] " + s + ": " + Debug.getNestedExceptionMessage(e));
		}	
	}
	
	public void 
	log(
		String 	s )
	{
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger( LOGGER_NAME );
		
		diag_logger.log( s );
		
		if ( ConstantsVuze.DIAG_TO_STDOUT ){
			
			System.out.println(Thread.currentThread().getName() + "|"
					+ System.currentTimeMillis() + "] " + s);
		}
	}
	
	public void
	generate(
		IndentWriter		writer )
	{
		writer.println( "Metasearch: auto=" + isAutoMode());
			
		try{
			writer.indent();

			meta_search.generate( writer );
			
		}finally{
			
			writer.exdent();
		}
	}
	
	protected static class
	SearchObject
		implements Search, ResultListener
	{
		private SearchListener		listener;
		
		private Map<Long,engineInfo>	engine_map = new HashMap<Long, engineInfo>();
		private boolean					engines_set;
		
		private List<SearchProviderResults>	pending_results = new ArrayList<SearchProviderResults>();
		
		private boolean	is_complete;
		
		protected
		SearchObject(
			SearchListener	_listener )
		{
			listener = _listener;
		}
		
		protected void
		setEnginesUsed(
			Engine[]	engines )
		{
			boolean	report_complete;
			
			synchronized( engine_map ){
				
				for ( Engine e: engines ){
					
					getInfo( e );
				}
				
				engines_set = true;
				
				report_complete = reportOverallComplete();
			}
			
			if ( listener != null && report_complete ){
					
				listener.completed();
			}
		}
		
		private boolean
		reportOverallComplete()
		{
			if ( is_complete || !engines_set ){
				
				return( false );
			}
			
			for ( engineInfo info: engine_map.values()){
				
				if ( !info.isComplete()){
					
					return( false );
				}
			}
			
			is_complete = true;
			
			return( true );
		}
		
		protected engineInfo
		getInfo(
			Engine		engine )
		{
			synchronized( engine_map ){

				engineInfo res = engine_map.get( engine.getId());
				
				if ( res == null ){
					
					res = new engineInfo( engine );
					
					engine_map.put( engine.getId(), res );
				}
				
				return( res );
			}
		}
		
		public void 
		contentReceived(
			Engine 		engine, 
			String 		content )
		{	
				// boring
		}
		
		public void 
		matchFound( 
			Engine 		engine, 
			String[] 	fields )
		{	
			// boring
		}
		
		public void 
		resultsReceived(
			Engine 			engine,
			final Result[] 	results )
		{
			SearchProviderResults	result;

			synchronized( engine_map ){
				
				final engineInfo info = getInfo( engine );
				
				result = 
					new SearchProviderResults()
					{
						public SearchProvider
						getProvider()
						{
							return( info );
						}
						
						public SearchResult[]
						getResults()
						{
							return( wrapResults( results ));
						}
						
						public boolean
						isComplete()
						{
							return( false );
						}
						
						public SearchException
						getError()
						{
							return( null );
						}
					};
					
				pending_results.add( result );
			}
			
			if ( listener != null ){
				
				listener.receivedResults( new SearchProviderResults[]{ result });
			}
		}
		
		public void 
		resultsComplete(
			Engine 		engine )
		{
			boolean					report_complete;
			SearchProviderResults	result;
			
			synchronized( engine_map ){
				
				final engineInfo info = getInfo( engine );
				
				info.setComplete();
				
				report_complete = reportOverallComplete();
			
				result = 
					new SearchProviderResults()
					{
						public SearchProvider
						getProvider()
						{
							return( info );
						}
						
						public SearchResult[]
						getResults()
						{
							return( new SearchResult[0] );
						}
						
						public boolean
						isComplete()
						{
							return( true );
						}
						
						public SearchException
						getError()
						{
							return( null );
						}
					};
					
				pending_results.add( result );
			}
			
			if ( listener != null ){
				
				listener.receivedResults( new SearchProviderResults[]{ result });
				
				if ( report_complete ){
					
					listener.completed();
				}
			}
		}
		
		protected void
		failed(
			Engine						engine,
			final SearchException		error )
		{
			boolean					report_complete;
			SearchProviderResults	result;
			
			synchronized( engine_map ){
				
				final engineInfo info = getInfo( engine );
				
				info.setComplete();
				
				report_complete = reportOverallComplete();
				
				result = 
						new SearchProviderResults()
						{
							public SearchProvider
							getProvider()
							{
								return( info );
							}
							
							public SearchResult[]
							getResults()
							{
								return( new SearchResult[0] );
							}
							
							public boolean
							isComplete()
							{
								return( false );
							}
							
							public SearchException
							getError()
							{
								return( error );
							}
						};
						
				pending_results.add( result );
			}
			
			if ( listener != null ){
				
				listener.receivedResults( new SearchProviderResults[]{ result });
				
				if ( report_complete ){
					
					listener.completed();
				}
			}
		}
		
		public void 
		engineFailed(
			Engine 		engine, 
			Throwable 	cause )
		{
			failed( engine, new SearchException( "Search failed", cause ));
		}
		
		public void 
		engineRequiresLogin(
			Engine 		engine, 
			Throwable 	cause )
		{
			failed( engine, new SearchException( "Authentication required", cause ));
		}
		
		protected SearchResult[]
		wrapResults(
			Result[]	res )
		{
			SearchResult[] x = new SearchResult[ res.length ];
			
			for ( int i=0;i<x.length;i++){
				
				x[i] = new resultWrapper( res[i] );
			}
			
			return( x );
		}
		
		public SearchProviderResults[]
     	getResults()
		{
     		synchronized( engine_map ){

     			SearchProviderResults[] result = pending_results.toArray( new SearchProviderResults[pending_results.size()]);
     			
     			pending_results.clear();
     			
     			return( result );
     		}
		}
     	
     	public boolean
     	isComplete()
     	{
     		synchronized( engine_map ){
     			
     			if ( !is_complete ){
     				
     				return( false );
     			}
     			
     			if ( pending_results.size() > 0 ){
     				
     				return( false );
     			}
     			
     			return( true );
     		}
     	}
     	
     	protected static class
     	resultWrapper
     		implements SearchResult
     	{
     		private Result	result;
     		
     		protected
     		resultWrapper(
     			Result		_result )
     		{
     			result	= _result;
     		}
     		
     		public Object
     		getProperty(
     			int		property_name )
     		{
     			switch( property_name ){
	     			case PR_NAME:{
	     			
	     				return( result.getName());
	     			}
	     			case PR_PUB_DATE:{
	     				
	     				return( result.getPublishedDate());
	     			}
	     			case PR_SIZE:{
	     				
	     				return( result.getSize());
	     			}
	     			case PR_LEECHER_COUNT:{
	     				
	     				return( new Long( result.getNbPeers()));
	     			}
	     			case PR_SEED_COUNT:{
	     				
	     				return( new Long( result.getNbSeeds()));
	     			}
	     			case PR_SUPER_SEED_COUNT:{
	     				
	     				return( new Long( result.getNbSuperSeeds()));
	     			}
	     			case PR_CATEGORY:{
	     				
	     				return( result.getCategory());
	     			}
	     			case PR_COMMENTS:{
	     				
	     				return( new Long( result.getComments()));
	     			}
	     			case PR_VOTES:{
	     				
	     				return( new Long( result.getVotes()));
	     			}
	     			case PR_CONTENT_TYPE:{
	     				
	     				return( result.getContentType());
	     			}
	     			case PR_DETAILS_LINK:{
	     				
	     				return( result.getCDPLink());
	     			}
	     			case PR_DOWNLOAD_LINK:{
	     				
	     				return( result.getDownloadLink());
	     			}
	     			case PR_PLAY_LINK:{
	     				
	     				return( result.getPlayLink());
	     			}
	     			case PR_PRIVATE:{
	     				
	     				return( result.isPrivate());
	     			}
	     			case PR_DRM_KEY:{
	     				
	     				return( result.getDRMKey());
	     			}
	     			case PR_DOWNLOAD_BUTTON_LINK:{
	     				
	     				return( result.getDownloadButtonLink());
	     			}
	     			case PR_RANK:{
	     			
	     				float rank = result.getRank();
	     				
	     				return( new Long(rank==-1?-1:(long)( rank * 100 )));
	     			}
	     			case PR_ACCURACY:{
	     			
	     				float accuracy = result.getAccuracy();
	     				
	     				return( new Long(accuracy==-1?-1:(long)( accuracy * 100 )));
	     			}
	     			case PR_VOTES_DOWN:{
	     				
	     				return( new Long( result.getVotesDown()));
	     			}
	     			case PR_UID:{
	     			
	     				return( result.getUID());
	     			}
	     			case PR_HASH:{
	     				
	     				String base32_hash = result.getHash();
	     				
	     				if ( base32_hash != null ){
	     					
	     					return( Base32.decode( base32_hash ));
	     				}
	     				
	     				return( null );
	     			}
	     			default:{
	     				
	     				Debug.out( "Unknown property type " + property_name );
	     			}
     			}
     			
     			return( null );
     		}
     	}
	}
	
    protected static class
    engineInfo
    	implements SearchProvider
    {
    	private Engine		engine;
     		
    	private boolean		complete;
    	
    	protected
    	engineInfo(
    		Engine		_engine )
    	{
    		engine	= _engine;
    	}
    
    	protected void
    	setComplete()
    	{
    		complete	= true;
    	}
    	
    	protected boolean
    	isComplete()
    	{
    		return( complete );
    	}
    	
		public SearchInstance
		search(
			Map<String,Object>	search_parameters,
			SearchObserver		observer )
		
			throws SearchException
		{
			throw( new SearchException( "Not supported" ));
		}
		
		public Object
		getProperty(
			int			property )
		{
			if ( property == PR_ID ){
				
				return( engine.getId());
				
			}else if ( property == PR_NAME ){
				
				return( engine.getName());
				
			}else{
				
				return( null );
			}
		}
		
		public void
		setProperty(
			int			property,
			Object		value )
		{
			Debug.out( "Not supported" );
		}
	}
    
    public static void
    main(
    	String[]	args )
    {
    	try{
			VuzeFile	vf = VuzeFileHandler.getSingleton().create();
			
			Map contents = new HashMap();
			
			contents.put( "type", new Long( 1 ));
			contents.put( "term", "donkey" );
			
			vf.addComponent(
				VuzeFileComponent.COMP_TYPE_METASEARCH_OPERATION,
				contents);
			
			vf.write( new File( "C:\\temp\\search.vuze" ));
			
    	}catch( Throwable e ){
    		
    		e.printStackTrace();
    	}
    }
}
