/*
 * Created on 29 nov. 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.pluginsinstaller;

import java.util.Iterator;
import java.util.List;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.installer.StandardPlugin;
import org.gudy.azureus2.ui.swt.wizard.Wizard;
import com.aelitis.azureus.core.AzureusCore;

/**
 * @author Olivier Chalouhi
 *
 */
public class InstallPluginWizard extends Wizard {
      
  List plugins;
  
  public InstallPluginWizard(
      	AzureusCore	azureus_core,	
 		Display 	display )
	{
		super(azureus_core,display,"installPluginsWizard.title");			
		
		IPWModePanel mode_panel = new IPWModePanel(this,null);
	
		setFirstPanel(mode_panel);
	}
  
  	public void 
	onClose() 
	{
		// Call the parent class to clean up resources
		super.onClose();	
	}
  	
  	public void setPluginList(List plugins) {
  	  this.plugins = plugins;
  	}
  	
  	public void performInstall() {
  	  if(plugins == null) return;
  	  
  	  Iterator iter = plugins.iterator();
  	  
  	  StandardPlugin[]	ps = new StandardPlugin[ plugins.size()];
  	  
  	  plugins.toArray( ps );
  	  
  	  if ( ps.length > 0 ){
  	    try {
  	      ps[0].getInstaller().install(ps,false);
  	      
  	    } catch(Exception e) {
  	      Debug.printStackTrace(e);
  	    }
  	  }
  	}
}
