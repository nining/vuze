/*
 * Created on May 16, 2004
 * Created by Olivier Chalouhi
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
package com.aelitis.azureus.core.update.impl;

import java.io.*;
import java.util.Properties;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.platform.unix.ScriptAfterShutdown;
import org.gudy.azureus2.platform.win32.access.AEWin32Access;
import org.gudy.azureus2.platform.win32.access.AEWin32Manager;
import org.gudy.azureus2.update.UpdaterUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.update.AzureusRestarter;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;

public class 
AzureusRestarterImpl 
	implements AzureusRestarter
{    
	private static final LogIDs LOGID = LogIDs.CORE;
	private static final String MAIN_CLASS 		= "org.gudy.azureus2.update.Updater";
	private static final String UPDATER_JAR 	= "Updater.jar";
	private static final String EXE_UPDATER		= "AzureusUpdater.exe";
	
  
	public static final String		UPDATE_PROPERTIES	= "update.properties";

	protected static boolean	restarted		= false;
	
	private static String JAVA_EXEC_DIR = System.getProperty("java.home") +
	 																		  System.getProperty("file.separator") +
	 																		  "bin" +
	 																		  System.getProperty("file.separator");
	
	
	protected AzureusCore	azureus_core;
	protected String		classpath_prefix;
	
	public
	AzureusRestarterImpl(
		AzureusCore		_azureus_core )
	{
		azureus_core	= _azureus_core;
	}
	
	public void
	restart(
		boolean	update_only )
	{
		if ( restarted ){
			
			Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
					"AzureusRestarter: already restarted!!!!"));
			
			return;
		}
		
		restarted	= true;
		
		PluginInterface pi = azureus_core.getPluginManager().getPluginInterfaceByID( "azupdater" );
		
		if ( pi == null ){
			Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR,
					"Can't restart, mandatory plugin 'azupdater' not found"));
			
			return;
		}
		
		String	updater_dir = pi.getPluginDirectoryName();
		
		classpath_prefix = updater_dir + File.separator + UPDATER_JAR;
		
 	 	String	app_path = SystemProperties.getApplicationPath();
	  	
	  	while( app_path.endsWith(File.separator)){
	  		
	  		app_path = app_path.substring(0,app_path.length()-1);
	  	}
	  	
	 	String	user_path = SystemProperties.getUserPath();
	  	
	  	while( user_path.endsWith(File.separator)){
	  		
	  		user_path = user_path.substring(0,user_path.length()-1);
	  	}
	  	
	  	String config_override = System.getProperty( SystemProperties.SYS_PROP_CONFIG_OVERRIDE );
	  	
	  	if ( config_override == null ){
	  		
	  		config_override = "";
	  	}
	  	
	  	String[]	parameters = {
	  			update_only?"updateonly":"restart",
	  			app_path,
	  			user_path,
				config_override,
	  	};
	  	
	  	FileOutputStream	fos	= null;
	  	
	  	try{
	  		Properties	restart_properties = new Properties();
	  	
	  		long	max_mem = Runtime.getRuntime().maxMemory();
	  			  			  			
	  		restart_properties.put( "max_mem", ""+max_mem );
	  		restart_properties.put( "app_name", SystemProperties.getApplicationName());
	  		restart_properties.put( "app_entry", SystemProperties.getApplicationEntryPoint());
	  		
	  		if ( System.getProperty( "azureus.nativelauncher" ) != null || Constants.isOSX ){
	  			//NOTE: new 2306 osx bundle now sets azureus.nativelauncher=1, but older bundles dont
	  			
	  			try{
		  			String	cmd = PlatformManagerFactory.getPlatformManager().getApplicationCommandLine();
		  			
		  			if ( cmd != null ){
		  				
		  				restart_properties.put( "app_cmd", cmd );
		  			}
	  			}catch( Throwable e ){
	  				
	  				Debug.printStackTrace(e);
	  			}
	  		}	  		
	  		
	  		
	  		fos	= new FileOutputStream( new File( user_path, UPDATE_PROPERTIES ));
	  		
	  			// this handles unicode chars by writing \\u escapes
	  		
	  		restart_properties.store(fos, "Azureus restart properties" );
	  		
	  	}catch( Throwable e ){
	  		
	  		Debug.printStackTrace( e );
	  		
	  	}finally{
	  		
	  		if ( fos != null ){
	  			
	  			try{
	  				
	  				fos.close();
	  				
	  			}catch( Throwable e ){
	  				
	  				Debug.printStackTrace(e);
	  			}
	  		}
	  	}
	  	
	  	String[]	properties = { "-Duser.dir=\"" + app_path + "\"" };
	  	
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		restartAzureus(new PrintWriter(os) {
			public void println(String str) {
				// we intercept these logs and log immediately
				Logger.log(new LogEvent(LOGID, str));
			}

		}, MAIN_CLASS, properties, parameters, update_only);
		
			// just check if any non-logged data exists
		
		byte[]	bytes = os.toByteArray();
		
		if ( bytes.length > 0 ){
			
			Logger.log(new LogEvent(LOGID, "AzureusRestarter: extra log - "
					+ new String(bytes)));
		}
	}
  
	
	private String
	getClassPath()
	{
		String classPath = System.getProperty("java.class.path");
	    	
    	classPath = classpath_prefix + System.getProperty("path.separator") + classPath;
	    
	    return( "-classpath \"" + classPath + "\" " );
	}
	  
	
	private boolean
	win32NativeRestart(
		PrintWriter	log,
		String		exec )
	{
	    try{
	    		// we need to spawn without inheriting handles
	    	
	    	PlatformManager pm = PlatformManagerFactory.getPlatformManager();
	    	
	    	pm.createProcess( exec, false );
	    
	    	return( true );
	    	
	    }catch(Throwable e) {
	        e.printStackTrace(log);
	        
	        return( false );
	    }
	}
	

	private String getExeUpdater(PrintWriter log) {
		try {
			boolean isVistaOrHigher = false;
			if (Constants.isWindows) {
				Float ver = null;
				try {
					ver = new Float(System.getProperty("os.version"));
				} catch (Exception e) {
				}
				isVistaOrHigher = ver != null && ver.floatValue() >= 6;
			}

			// Vista test: We will need to run an elevated EXE updater if we can't
			//             write to the program dir.
			
			if (isVistaOrHigher) {
				if (AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface().getUpdateManager().getInstallers().length > 0) {
					log.println("Vista restart w/Updates.. checking if EXE needed");
					try {
						final File writeFile = FileUtil.getApplicationFile("write.dll");
						// should fail if no perms, but sometimes it's created in
						// virtualstore (if ran from java(w).exe for example)
						FileOutputStream fos = new FileOutputStream(writeFile);
						fos.write(32);
						fos.close();

						writeFile.delete();

						File renameFile = FileUtil.getApplicationFile("License.txt");
						if (renameFile != null && renameFile.exists()) {
							File oldFile = FileUtil.getApplicationFile("License.txt");
							String oldName = renameFile.getName();
							File newFile = new File(renameFile.getParentFile(), oldName
									+ ".bak");
							renameFile.renameTo(newFile);

							if (oldFile.exists()) {
								log.println("Requiring EXE because rename test failed");
								return EXE_UPDATER; 
							}

							newFile.renameTo(oldFile);
						} else {
							log.println("Could not try Permission Test 2. File " + renameFile
									+ " not found");
						}

					} catch (Exception e) {
						log.println("Permission Test Failed. " + e.getMessage() + ";"
								+ Debug.getCompressedStackTrace());
						return EXE_UPDATER; 
					}
				}
			}
		} catch (Throwable t) {
			// ignore vista test
		}

		return null;
	}

  private boolean restartViaEXE(PrintWriter log,
  		String exeUpdater,
      String[]  properties,
      String[]  parameters,
      String backupJavaRunString,
      boolean update_only) 
  {
		String azRunner = null;
		File fileRestart = null;
		if (!update_only) {
  		try {
  			azRunner = PlatformManagerFactory.getPlatformManager().getApplicationCommandLine();
  		} catch (PlatformManagerException e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  		}
		}

		try {
			int result;
			AEWin32Access accessor = AEWin32Manager.getAccessor(true);
			if (accessor == null) {
				result = -123;
			} else {
				if (azRunner != null) {
					// create a batch file to run the updater, then to restart azureus
					// bceause the updater would restart azureus as administrator user
					// and confuse the user
					fileRestart = FileUtil.getUserFile("restart.bat");
					String s = "title Azureus Updater Runner\r\n";
					s += exeUpdater + " \"updateonly\"";
					for (int i = 1; i < parameters.length; i++) {
						s += " \"" + parameters[i].replaceAll("\\\"", "") + "\"";
					}
					s += "\r\n";
					s += "start \"\" \"" + azRunner + "\"";
					FileUtil.writeBytesAsFile(fileRestart.getAbsolutePath(), s.getBytes());

					result = accessor.shellExecute(null, fileRestart.getAbsolutePath(),
							null, SystemProperties.getApplicationPath(),
							AEWin32Access.SW_SHOWMINIMIZED);
				} else {
					String execEXE = "\"-J" + getClassPath().replaceAll("\\\"", "")
							+ "\" ";

					for (int i = 0; i < properties.length; i++) {
						execEXE += "\"-J" + properties[i].replaceAll("\\\"", "") + "\" ";
					}

					for (int i = 0; i < parameters.length; i++) {
						execEXE += " \"" + parameters[i].replaceAll("\\\"", "") + "\"";
					}

					log.println("Launch via " + exeUpdater + " params " + execEXE);
					result = accessor.shellExecute(null, exeUpdater, execEXE,
							SystemProperties.getApplicationPath(), AEWin32Access.SW_NORMAL);
				}
			}

			/*
			 * Some results:
			 * 0: OOM
			 * 2: FNF
			 * 3: Path Not Foud
			 * 5: Access Denied (User clicked cancel on admin access dialog)
			 * 8: OOM
			 * 11: Bad Format
			 * 26: Sharing Violation
			 * 27: Association incomplete
			 * 28: DDE Timeout
			 * 29: DDE Fail
			 * 30: DDE Busy
			 * 31: No Association
			 * 32: DLL Not found
			 * >32: OK!
			 */
			log.println("   -> " + result);

			if (result <= 32) {
				String sErrorReason = "";
				String key = null;

				switch (result) {
					case 0:
					case 8:
						key = "oom";
						break;

					case 2:
						key = "fnf";
						break;

					case 3:
						key = "pnf";
						break;

					case 5:
						key = "denied";
						break;

					case 11:
						key = "bad";
						break;

					case -123:
						key = "nowin32";
						break;

					default:
						sErrorReason = "" + result;
						break;
				}
				if (key != null) {
					sErrorReason = MessageText.getString("restart.error." + key,
							new String[] {
								exeUpdater,
								SystemProperties.getApplicationPath(),
							});
				}
				Logger.log(new LogAlert(false, LogAlert.AT_ERROR,
						MessageText.getString("restart.error", new String[] {
							sErrorReason
						})));
				return false;
			}
		} catch (Throwable f) {

			f.printStackTrace(log);

			return javaSpawn(log, backupJavaRunString);
		}

		return true;
	}
  

  // ****************** This code is copied into Restarter / Updater so make changes there too !!!
  
  


  public void 
  restartAzureus(
      PrintWriter log, 
    String    mainClass,
    String[]  properties,
    String[]  parameters,
    boolean update_only) 
  {
    if(Constants.isOSX){
    	
    	restartAzureus_OSX(log,mainClass,properties,parameters);
    	
    }else if( Constants.isUnix ){
    	
    	restartAzureus_Unix(log,mainClass,properties,parameters);
      
    }else{
    	
    	restartAzureus_win32(log,mainClass,properties,parameters,update_only);
    }
  }
  
  private void 
  restartAzureus_win32(
      PrintWriter log,
    String    mainClass,
    String[]  properties,
    String[]  parameters,
    boolean	update_only) 
  {
  	String exeUpdater = getExeUpdater(log);  // Not for Updater.java

  	String exec;

		//Classic restart way using Runtime.exec directly on java(w)
		exec = "\"" + JAVA_EXEC_DIR + "javaw\" " + getClassPath() + getLibraryPath();

		for (int i = 0; i < properties.length; i++) {
			exec += properties[i] + " ";
		}

		exec += mainClass;

		for (int i = 0; i < parameters.length; i++) {
			exec += " \"" + parameters[i] + "\"";
		}

		if (exeUpdater != null) {
			restartViaEXE(log, exeUpdater, properties, parameters, exec, update_only);
		} else {
			if (log != null) {
				log.println("  " + exec);
			}

			if (!win32NativeRestart(log, exec)) {
				javaSpawn(log, exec);
			}
		}
	}
  

	private boolean
	javaSpawn(
		PrintWriter log, 
		String execString) 
	{
		try {
			// hmm, try java method - this WILL inherit handles but might work :)

			log.println("Using java spawn");

			//NOTE: no logging done here, as we need the method to return right away, before the external process completes
			Process p = Runtime.getRuntime().exec(execString);

			log.println("    -> " + p);

			return true;
		} catch (Throwable g) {

			g.printStackTrace();
			return false;
		}
	}

	private void 
  restartAzureus_OSX(
      PrintWriter log,
    String mainClass,
    String[]  properties,
    String[] parameters) 
  {

     String exec = "\"" + JAVA_EXEC_DIR + "java\" " + getClassPath() + getLibraryPath();
  	 
     for (int i=0;i<properties.length;i++){
    	 exec += properties[i] + " ";
     }
    
     exec += mainClass ;
    
     for(int i = 0 ; i < parameters.length ; i++) {
    	 exec += " \"" + parameters[i] + "\"";
     }

     runExternalCommandViaUnixShell( log, exec );
  }
  
  
  
  private int getUnixScriptVersion() {
		String sVersion = System.getProperty("azureus.script.version", "0");
		int version = 0;
		try {
			version = Integer.parseInt(sVersion);
		} catch (Throwable t) {
		}
		return version;
  }

  private void 
  restartAzureus_Unix(
    PrintWriter log,
  String    mainClass,
  String[]  properties,
  String[]  parameters) 
  {
    
    String exec = "\"" + JAVA_EXEC_DIR + "java\" " + getClassPath() +	getLibraryPath();
    
    for (int i=0;i<properties.length;i++){
      exec += properties[i] + " ";
    }
    
    int scriptVersion = getUnixScriptVersion();
    boolean restartByScript = Constants.compareVersions(
				UpdaterUtils.getUpdaterPluginVersion(), "1.8.5") >= 0
				&& scriptVersion > 0; 
    if (restartByScript) {
    	exec += "-Dazureus.script.version=\"" + scriptVersion + "\" ";
    }
    
    exec += mainClass ;
    
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
  	if (restartByScript) {
  		// run script after az shutdown to launch updater and then re-run az
  		ScriptAfterShutdown.addExtraCommand("echo \"Applying (possible) patches before restarting..\"\n"
  				+ exec + "\n"
					+ "echo \"Restarting Azureus..\"\n"
					+ "$0\n");
			ScriptAfterShutdown.setRequiresExit(true);
  	} else {
  		runExternalCommandViaUnixShell( log, exec );
  	}
  }
  
  
  
  private String
  getLibraryPath()
  {
    String libraryPath = System.getProperty("java.library.path");
    
    if ( libraryPath == null ){
    	
      libraryPath = "";
      
    }else{
    	
    		// remove any quotes from the damn thing
    	
    	String	temp = "";
    	
    	for (int i=0;i<libraryPath.length();i++){
    		
    		char	c = libraryPath.charAt(i);
    		
    		if ( c != '"' ){
    			
    			temp += c;
    		}
    	}
    	
    	libraryPath	= temp;
    	
    		// remove trailing separator chars if they exist as they stuff up
    		// the following "
    	
    	while( libraryPath.endsWith(File.separator)){
    	
    		libraryPath = libraryPath.substring( 0, libraryPath.length()-1 );
    	}
    	
    	if ( libraryPath.length() > 0 ){
  
    		libraryPath = "-Djava.library.path=\"" + libraryPath + "\" ";
    	}
    }
    
    return( libraryPath );
  }
  
  /*
  private void logStream(String message,InputStream stream,PrintWriter log) {
    BufferedReader br = new BufferedReader (new InputStreamReader(stream));
    String line = null;
    boolean first = true;
    
    try {
      while((line = br.readLine()) != null) {
      	if( first ) {
      		log.println(message);
      		first = false;
      	}
      	
        log.println(line);
      }
    } catch(Exception e) {
       log.println(e);
       e.printStackTrace(log);
    }
  }
  
  
  private void chMod(String fileName,String rights,PrintWriter log) {
    String[] execStr = new String[3];
    execStr[0] = "chmod";
    execStr[1] = rights;
    execStr[2] = fileName;
    
    runExternalCommandsLogged( log, execStr );
  }
  
  
  private Process runExternalCommandLogged( PrintWriter log, String command ) {  //NOTE: will not return until external command process has completed
  	log.println("About to execute: R:[" +command+ "]" );
  	
  	try {
  		Process runner = Runtime.getRuntime().exec( command );
  		runner.waitFor();		
  		logStream( "runtime.exec() output:", runner.getInputStream(), log);
      logStream( "runtime.exec() error:", runner.getErrorStream(), log);
      return runner;
  	}
  	catch( Throwable t ) {
  		log.println( t.getMessage() != null ? t.getMessage() : "<null>" );
  		log.println( t );
  		t.printStackTrace( log );
  		return null;
  	}
  }
  
  private Process runExternalCommandsLogged( PrintWriter log, String[] commands ) {  //NOTE: will not return until external command process has completed
  	String cmd = "About to execute: R:[";
  	for( int i=0; i < commands.length; i++ ) {
  		cmd += commands[i];
  		if( i < commands.length -1 )  cmd += " ";
  	}
  	cmd += "]";
  	
  	log.println( cmd );
  	
  	try {
  		Process runner = Runtime.getRuntime().exec( commands );
  		runner.waitFor();		
  		logStream( "runtime.exec() output:", runner.getInputStream(), log);
      logStream( "runtime.exec() error:", runner.getErrorStream(), log);
      return runner;
  	}
  	catch( Throwable t ) {
  		log.println( t.getMessage() != null ? t.getMessage() : "<null>" );
  		log.println( t );
  		t.printStackTrace( log );
  		return null;
  	}
  }
  */
  
  
  private void runExternalCommandViaUnixShell( PrintWriter log, String command ) {
  	String[] to_run = new String[3];
  	to_run[0] = "/bin/sh";
  	to_run[1] = "-c";
  	to_run[2] = command;
   	 
  	if( log != null )  log.println("Executing: R:[" +to_run[0]+ " " +to_run[1]+ " " +to_run[2]+ "]" );

  	try {
  		//NOTE: no logging done here, as we need the method to return right away, before the external process completes
  		Runtime.getRuntime().exec( to_run );	
  	}
  	catch(Throwable t) {
  		if( log != null )  {
  			log.println( t.getMessage() != null ? t.getMessage() : "<null>" );
  			log.println( t );
  			t.printStackTrace( log );
  		}
  		else {
  			t.printStackTrace();
  		}
  	}
  }
  
  
}
