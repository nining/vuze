/*
 * File    : OverallStatsImpl.java
 * Created : 2 mars 2004
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
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
package org.gudy.azureus2.core3.stats.transfer.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.transport.DHTTransportStats;
import com.aelitis.azureus.core.stats.AzureusCoreStats;
import com.aelitis.azureus.core.stats.AzureusCoreStatsProvider;
import com.aelitis.azureus.plugins.dht.DHTPlugin;


/**
 * @author Olivier
 * 
 */
public class 
OverallStatsImpl 
	extends GlobalManagerAdapter
	implements OverallStats
{
  
  	// sizes in MB
  
  private static final long TEN_YEARS 		= 60*60*24*365*10L;
  
  private static final int	STATS_PERIOD	= 60*1000;		// 1 min
  private static final int	SAVE_PERIOD		= 10*60*1000;	// 10 min
  private static final int	SAVE_TICKS		= SAVE_PERIOD / STATS_PERIOD;
  
  private AzureusCore			core;
  private GlobalManagerStats	gm_stats;
  
  private DHT[] dhts;
  
  private long totalDownloaded;
  private long totalUploaded;
  private long totalUptime;
  
  private long totalDHTUploaded;
  private long totalDHTDownloaded;
  
  private long lastDownloaded;
  private long lastUploaded;
  private long lastUptime;
  
  private long lastDHTUploaded;
  private long lastDHTDownloaded;
  
  
  	// separate stats
  
  private long totalProtocolUploaded;
  private long totalDataUploaded;
  private long totalProtocolDownloaded;
  private long totalDataDownloaded;
  
  private long lastProtocolUploaded;
  private long lastDataUploaded;
  private long lastProtocolDownloaded;
  private long lastDataDownloaded;
  
  private long[]	lastSnapshot;
  
  private long session_start_time = SystemTime.getCurrentTime();
  
  protected AEMonitor	this_mon	= new AEMonitor( "OverallStats" );

  private int 	tick_count;
  
  private Map 
  load(String filename) 
  {
    return( FileUtil.readResilientConfigFile( filename ));
  }
  
  private Map load() {
	  return( load("azureus.statistics"));
	}
  
  private void 
  save(String filename,
		Map	map ) 
  {  	  
  	try{
  		this_mon.enter();
  	  		
  		FileUtil.writeResilientConfigFile( filename, map );
  		
  	}finally{
  		
  		this_mon.exit();
  	}
  }
  
  private void save( Map map ) {
	  save("azureus.statistics", map);
	}
  
  private void validateAndLoadValues(
	Map	statisticsMap ) {
	  
    lastUptime = SystemTime.getCurrentTime() / 1000;

    Map overallMap = (Map) statisticsMap.get("all");

    totalDownloaded = getLong( overallMap, "downloaded" );
	totalUploaded = getLong( overallMap, "uploaded" );
	totalUptime = getLong( overallMap, "uptime" );	
	
	totalDHTDownloaded = getLong( overallMap, "dht_down" );
	totalDHTUploaded = getLong( overallMap, "dht_up" );
	
    totalProtocolUploaded 	= getLong( overallMap, "p_uploaded" );
    totalDataUploaded 		= getLong( overallMap, "d_uploaded" );
    totalProtocolDownloaded = getLong( overallMap, "p_downloaded" );
    totalDataDownloaded 	= getLong( overallMap, "d_downloaded" );
    
    
    long	current_total_d_received 	= gm_stats.getTotalDataBytesReceived();
    long	current_total_p_received 	= gm_stats.getTotalProtocolBytesReceived();

    long	current_total_d_sent		= gm_stats.getTotalDataBytesSent();
    long	current_total_p_sent		= gm_stats.getTotalProtocolBytesSent();

	lastSnapshot = 
		 new long[]{ 	totalProtocolUploaded, totalDataUploaded, 
			 			totalProtocolDownloaded, totalDataDownloaded,
			 			totalDHTUploaded, totalDHTDownloaded, 
			 			current_total_p_sent, current_total_d_sent,
			 			current_total_p_received, current_total_d_received,
			 			0, 0 };
  }
  
  protected long
  getLong(
	Map		map,
	String	name )
  {
	  if ( map == null ){
		  return( 0 );
	  }
	
	  Object	obj = map.get(name);
	
	  if (!(obj instanceof Long )){
		return(0);
	  }
	
	  return(((Long)obj).longValue());
  }
  
  public 
  OverallStatsImpl(
	AzureusCore 		_core,
	GlobalManagerStats	_gm_stats )
  {
	core		= _core;
	gm_stats	= _gm_stats;
	
    Map 	stats = load();
    
    validateAndLoadValues(stats);

	Set	types = new HashSet();	
	
	types.add( AzureusCoreStats.ST_XFER_UPLOADED_PROTOCOL_BYTES );
	types.add( AzureusCoreStats.ST_XFER_UPLOADED_DATA_BYTES );
	types.add( AzureusCoreStats.ST_XFER_DOWNLOADED_PROTOCOL_BYTES );
	types.add( AzureusCoreStats.ST_XFER_DOWNLOADED_DATA_BYTES );

	AzureusCoreStats.registerProvider( 
		types, 
		new AzureusCoreStatsProvider()
		{
			public void
			updateStats(
				Set		types,
				Map		values )
			{	
			  	try{
			  		this_mon.enter();
			  		
			  		if ( core.isStarted()){
			  				
						if ( types.contains( AzureusCoreStats.ST_XFER_UPLOADED_PROTOCOL_BYTES )){
							
							values.put( 
								AzureusCoreStats.ST_XFER_UPLOADED_PROTOCOL_BYTES, 
								new Long( totalProtocolUploaded + ( gm_stats.getTotalProtocolBytesSent() - lastProtocolUploaded )));
						}
						if ( types.contains( AzureusCoreStats.ST_XFER_UPLOADED_DATA_BYTES )){
							
							values.put( 
								AzureusCoreStats.ST_XFER_UPLOADED_DATA_BYTES, 
								new Long( totalDataUploaded + ( gm_stats.getTotalDataBytesSent() - lastDataUploaded )));
						}
						if ( types.contains( AzureusCoreStats.ST_XFER_DOWNLOADED_PROTOCOL_BYTES )){
							
							values.put( 
								AzureusCoreStats.ST_XFER_DOWNLOADED_PROTOCOL_BYTES, 
								new Long( totalProtocolDownloaded + ( gm_stats.getTotalProtocolBytesReceived() - lastProtocolDownloaded )));
						}
						if ( types.contains( AzureusCoreStats.ST_XFER_DOWNLOADED_DATA_BYTES )){
							
							values.put( 
								AzureusCoreStats.ST_XFER_DOWNLOADED_DATA_BYTES, 
								new Long( totalDataDownloaded + ( gm_stats.getTotalDataBytesReceived() - lastDataDownloaded )));
						}
			  		}
			  	}finally{
			  	  	
			  		this_mon.exit();
			  	}
			}
		});
	
    core.addLifecycleListener(
    	new AzureusCoreLifecycleAdapter()
    	{
    		public void
    		componentCreated(
    			AzureusCore				core,
    			AzureusCoreComponent	component )
    		{
    			if ( component instanceof GlobalManager ){
    				
    				GlobalManager	gm = (GlobalManager)component;
    				
    				gm.addListener( OverallStatsImpl.this, false );
    				   
    			    SimpleTimer.addPeriodicEvent(
    			    	"OverallStats", 
    			    	STATS_PERIOD, 
    			    	new TimerEventPerformer()
    			    	{
    			    		public void 
    			    		perform(TimerEvent event) 
    			    		{
    			    			updateStats( false );
    			    		}
    			    	});
    			}
    		}
    	});

  }
  
	public int getAverageDownloadSpeed() {
		if(totalUptime > 1) {
      return (int)(totalDownloaded / totalUptime);
    }
    return 0;
	}

	public int getAverageUploadSpeed() {
    if(totalUptime > 1) {
      return (int)(totalUploaded / totalUptime);
    }
    return 0;
	}

	public long getDownloadedBytes() {
		return totalDownloaded;
	}

	public long getUploadedBytes() {
		return totalUploaded;
	}

	public long getTotalUpTime() {
		return totalUptime;
  }

  public long getSessionUpTime() {
    return (SystemTime.getCurrentTime() - session_start_time) / 1000;
  }
  
  public void destroyInitiated() {
    updateStats( true );
  }

  protected long[]
  getLastSnapshot()
  {
  	try{
  		this_mon.enter();
  		
  		return( lastSnapshot );
  		
 	}finally{
 	  	
  		this_mon.exit();
  	}
  }
  private void updateStats( boolean force ) 
  {
  	try{
  		this_mon.enter();
  	
	    long current_time = SystemTime.getCurrentTime() / 1000;
	    
	    if ( current_time < lastUptime ) {  //time went backwards
	      lastUptime = current_time;
	      return;
	    }
	    	    
	    long	current_total_d_received 	= gm_stats.getTotalDataBytesReceived();
	    long	current_total_p_received 	= gm_stats.getTotalProtocolBytesReceived();

	    long	current_total_d_sent		= gm_stats.getTotalDataBytesSent();
	    long	current_total_p_sent		= gm_stats.getTotalProtocolBytesSent();
   
	    long	current_total_received 	= current_total_d_received + current_total_p_received;
	    long	current_total_sent		= current_total_d_sent + current_total_p_sent;
	    
	    	// overall totals
	    
	    totalDownloaded +=  current_total_received - lastDownloaded;
	    lastDownloaded = current_total_received;    
	    if( totalDownloaded < 0 )  totalDownloaded = 0;

	    totalUploaded +=  current_total_sent - lastUploaded;
	    lastUploaded = current_total_sent;
	    if( totalUploaded < 0 )  totalUploaded = 0;
	    
	    	// split totals
	    
	    totalDataDownloaded +=  current_total_d_received - lastDataDownloaded;
	    lastDataDownloaded = current_total_d_received;    
	    if( totalDataDownloaded < 0 )  totalDataDownloaded = 0;

	    totalProtocolDownloaded +=  current_total_p_received - lastProtocolDownloaded;
	    lastProtocolDownloaded = current_total_p_received;    
	    if( totalProtocolDownloaded < 0 )  totalProtocolDownloaded = 0;

	    totalDataUploaded +=  current_total_d_sent - lastDataUploaded;
	    lastDataUploaded = current_total_d_sent;    
	    if( totalDataUploaded < 0 )  totalDataUploaded = 0;

	    totalProtocolUploaded +=  current_total_p_sent - lastProtocolUploaded;
	    lastProtocolUploaded = current_total_p_sent;    
	    if( totalProtocolUploaded < 0 )  totalProtocolUploaded = 0;
	    
	    	// DHT
	    
	    if ( dhts == null ){
	    	
		    try{
		    	PluginManager pm = core.getPluginManager();
		    	
		    	if ( pm.isInitialized()){
		    
			        PluginInterface dht_pi = pm.getPluginInterfaceByClass( DHTPlugin.class );
			          
			        if ( dht_pi == null ){
			           
			        	dhts = new DHT[0];
			        	
			        }else{
			        	
			        	DHTPlugin plugin = (DHTPlugin)dht_pi.getPlugin();
			        	
			        	if ( !plugin.isInitialising()){
			        		
				        	if ( plugin.isEnabled()){
				        	
				        		dhts = ((DHTPlugin)dht_pi.getPlugin()).getDHTs();
				        		
				        	}else{
				        		
				        		dhts = new DHT[0];
				        	}
			        	}
			        }
		    	}
		    }catch( Throwable e ){
		    	
		    	dhts = new DHT[0];
		    }
	    }
	    
	    long current_total_dht_up 	= 0;
	    long current_total_dht_down = 0;
	    
	    if ( dhts != null ){
	    	
		    for ( DHT dht: dhts ){
		    	
		    	DHTTransportStats stats = dht.getTransport().getStats();
		    	
		    	current_total_dht_up 	+= stats.getBytesSent();
		    	current_total_dht_down 	+= stats.getBytesReceived();
		    }
	    }
	    
	    totalDHTUploaded +=  current_total_dht_up - lastDHTUploaded;
	    lastDHTUploaded = current_total_dht_up;    
	    if( totalDHTUploaded < 0 )  totalDHTUploaded = 0;
	    
	    totalDHTDownloaded +=  current_total_dht_down - lastDHTDownloaded;
	    lastDHTDownloaded = current_total_dht_down;    
	    if( totalDHTDownloaded < 0 )  totalDHTDownloaded = 0;

	    	// TIME
	    
	    long delta = current_time - lastUptime;
	    
	    if( delta > 100 || delta < 0 ) { //make sure the time diff isn't borked
	      lastUptime = current_time;
	      return;
	    }
	    
	    if( totalUptime > TEN_YEARS ) {  //total uptime > 10years is an error, reset
	      totalUptime = 0;
	    }
	    
	    if( totalUptime < 0 )  totalUptime = 0;
	    
	    totalUptime += delta;
	    lastUptime = current_time;

		lastSnapshot = 
			 new long[]{ 	totalProtocolUploaded, totalDataUploaded, 
				 			totalProtocolDownloaded, totalDataDownloaded,
				 			totalDHTUploaded, 
				 			totalDHTDownloaded,
				 			current_total_p_sent, current_total_d_sent,
				 			current_total_p_received, current_total_d_received,
				 			current_total_dht_up, 
				 			current_total_dht_down };
	    
	    HashMap	overallMap = new HashMap();
	    
	    overallMap.put("downloaded",new Long(totalDownloaded));
	    overallMap.put("uploaded",new Long(totalUploaded));
	    overallMap.put("uptime",new Long(totalUptime));

	    overallMap.put("dht_down",new Long(totalDHTDownloaded));
	    overallMap.put("dht_up",new Long(totalDHTUploaded));

	    overallMap.put("p_uploaded",new Long(totalProtocolUploaded));
	    overallMap.put("d_uploaded",new Long(totalDataUploaded));
	    overallMap.put("p_downloaded",new Long(totalProtocolDownloaded));
	    overallMap.put("d_downloaded",new Long(totalDataDownloaded));

	    Map	map = new HashMap();
	    
	    map.put( "all", overallMap );
	    
	    tick_count++;

	    if ( force || tick_count % SAVE_TICKS == 0 ){
	    
	    	save( map );
	    }
  	}finally{
  	
  		this_mon.exit();
  	}
  }
}
