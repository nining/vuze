/*
 * File    : ShareTester.java
 * Created : 30-Dec-2003
 * By      : parg
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

package org.gudy.azureus2.pluginsimpl.sharing.test;



import java.util.*;
import java.io.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class 
ShareTester
	implements Plugin, PluginListener, ShareManagerListener
{
	protected static Semaphore			init_sem = new Semaphore();
	
	protected static ShareTester		singleton;
	
	protected Map	seed_transport_map	= new HashMap();
	
	public static synchronized ShareTester
	getSingleton()
	{
		if ( singleton == null ){
			
			new Thread( "plugin initialiser ")
			{
				public void
				run()
				{
					PluginManager.registerPlugin( ShareTester.class );
	
					Properties props = new Properties();
					
					props.put( PluginManager.PR_MULTI_INSTANCE, "true" );
					
					PluginManager.startAzureus( PluginManager.UI_SWT, props );
				}
			}.start();
		
			init_sem.reserve();
		}
		
		return( singleton );
	}	
	
	protected PluginInterface		plugin_interface;
	
	public void 
	initialize(
		PluginInterface _pi )
	{
		System.out.println( "plugin initialize called");
	
		plugin_interface = _pi;
		
		singleton = this;
		
		init_sem.release();
		
		LoggerChannel log = plugin_interface.getLogger().getChannel("Plugin Test");
		
		log.log(LoggerChannel.LT_INFORMATION, "Plugin Initialised");
		
		plugin_interface.addListener( this );
	}
	
	public void
	initializationComplete()
	{
		try{
			ShareManager	sm = plugin_interface.getShareManager();
		
			sm.addListener( this );
			
			sm.initialise();
			
			// ShareResourceDirContents res = sm.addDirContents( new File("D:\\music\\cd1"), false);
			
			//Torrent t = res.getItem().getTorrent();
			
			//System.out.println( t.getHash());
			
		}catch( ShareException e ){
			
			e.printStackTrace();
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	
	}
	
	public void
	resourceAdded(
		ShareResource		resource )
	{
		System.out.println( "resource added:" + resource.getName());
		
		if ( resource.getType() == ShareResource.ST_DIR_CONTENTS ){
			
			ShareResourceDirContents c = (ShareResourceDirContents)resource;
			
			ShareResource[]	kids = c.getChildren();
			
			for (int i=0;i<kids.length;i++){
				
				System.out.println( "\t" + kids[i].getName());
			}
		}
	}
	
	public void
	resourceModified(
		ShareResource		resource )
	{
		System.out.println( "resource modified:" + resource.getName());
	}
	
	public void
	resourceDeleted(
		ShareResource		resource )
	{
		System.out.println( "resource deleted:" + resource.getName());
	}
	
	public void
	reportProgress(
		int		percent_complete )
	{
	}
	
	public void
	reportCurrentTask(
		String	task_description )
	{
		System.out.println( task_description );
	}
	
	public static void
	main(
		String[]	args )
	{
		getSingleton();
	}
}
