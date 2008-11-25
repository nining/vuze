/*
 * Created on Aug 30, 2006
 * Created by Alon Rohter
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package com.aelitis.azureus.util;

import java.net.URL;
import java.util.Locale;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.Base32;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.crypto.VuzeCryptoManager;

/**
 * 
 */
public class ConstantsV3
{
	// isOS* constants copied from AZ2 for ease of use/access
	public static boolean isOSX = org.gudy.azureus2.core3.util.Constants.isOSX;

	public static boolean isWindows = org.gudy.azureus2.core3.util.Constants.isWindows;

	public static boolean isUnix = org.gudy.azureus2.core3.util.Constants.isUnix;

	public static final String AZID = Base32.encode(VuzeCryptoManager.getSingleton().getPlatformAZID());

	public static final ContentNetwork	DEFAULT_CONTENT_NETWORK = ContentNetworkManagerFactory.getSingleton().getContentNetwork( ContentNetwork.CONTENT_NETWORK_VUZE );

	/**
	 * Most of these to go away!
	 */
	
	private static final String DEFAULT_ADDRESS = "www.vuze.com"; //DO NOT TOUCH !!!!  use the -Dplatform_address=ip override instead

	private static final String DEFAULT_PORT = "80";

	private static String URL_ADDRESS = System.getProperty("platform_address",DEFAULT_ADDRESS);

	private static String URL_PORT = System.getProperty("platform_port", DEFAULT_PORT);
	
		// **** EMP dependencies start
	
	public static final String URL_PREFIX = "http://" + URL_ADDRESS + ":" + URL_PORT + "/";
	
		// used in azemp so can't fix up properly until that changes...
	
	public static final String URL_COMMENTS = "comment/";

	public static String URL_SUFFIX;

	public static void initialize( AzureusCore core ){}
	
	static{

			// this is purely here to support azemp's direct use of URL_SUFFIX until we fix
		
		COConfigurationManager.addAndFireParameterListener("locale",
				new ParameterListener() 
				{
					public void 
					parameterChanged(String parameterName) 
					{
						// Don't change the order of the params
						URL_SUFFIX = "azid=" + AZID + "&azv="
								+ org.gudy.azureus2.core3.util.Constants.AZUREUS_VERSION
								+ "&locale=" + Locale.getDefault().toString();
						
						Constants.update();
					}
				});
	}
			
		// EMP dependencies end
			
	// WARNING: TODO -- This is temporary and must be removed once the buddies features are complete
	public static final boolean DISABLE_BUDDIES_BAR = System.getProperty(
			"debug.buddies.bar", "1").equals("0");

	/**
	 * This verifier value is only used to validate that the page we're loading is
	 * in-fact a page from Vuze; mainly required by the LightBoxBrowserWindow
	 */
	public static final String URL_PAGE_VERIFIER_VALUE = "vuzePage";

	public static final boolean DIAG_TO_STDOUT = System.getProperty(
			"DIAG_TO_STDOUT", "0").equals("1");

	public static final String DL_REFERAL_PLAYDASHACTIVITY = "playdashboardactivity";
	
	public static final String DL_REFERAL_UNKNOWN = "unknown";

	public static final String DL_REFERAL_LAUNCH = "launch";

	public static final String DL_REFERAL_PLAYDM = "playdownloadmanager";

	public static final String DL_REFERAL_SELCONTENT = "selectedcontent";

	public static final String DL_REFERAL_DBLCLICK = "dblclick";

	public static final String DL_REFERAL_TOOLBAR = "toolbar";

	public static final String DL_REFERAL_DASHACTIVITY = "dashboardactivity";
	
	public static String appendURLSuffix(String url) {
		if (url.indexOf("azid=") < 0) {
			url += (url.indexOf('?') < 0 ? "?" : "&") + ConstantsV3.URL_SUFFIX;
		}
		return url;
	}
	
	public static String urlRelativeToPlatform(String url) {
		if (!url.startsWith("http")) {
			url = ConstantsV3.URL_PREFIX
					+ (url.startsWith("/") ? url.substring(1) : url);
		}
		return url;
	}
}
