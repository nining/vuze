/*
 * Created on 22-Sep-2004
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

package org.gudy.azureus2.core3.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;

/**
 * @author parg
 *
 */
public class 
AEDiagnosticsLogger 
{
	private static final int	MAX_PENDING = 8*1024;
	
	private String 			name;
	private int				max_size;
	private File			debug_dir;
	
	private boolean			first_file				= true;
	private boolean			first_write			 	= true;
	
	private LinkedList<StringBuilder>	pending;
	private int							pending_size;
	private boolean						direct_writes;
	
	protected
	AEDiagnosticsLogger(
		File		_debug_dir,
		String		_name,
		int			_max_size,
		boolean		_direct_writes )
	{
		debug_dir		= _debug_dir;
		name			= _name;
		max_size		= _max_size;
		direct_writes	= _direct_writes;
		
		try{
			File	f1 = getLogFile();
			
			first_file = false;
			
			File	f2 = getLogFile();
			
			first_file = true;

				// if we were writing to the second file, carry on from there
			
			if ( f1.exists() && f2.exists()){
	
				if ( f1.lastModified() < f2.lastModified()){
					
					first_file = false;
				}
			}
		}catch( Throwable ignore ){
			
		}
	}
	
	protected String
	getName()
	{
		return( name );
	}
		
	public void
	log(
		Throwable				e )
	{
		try{
			ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			
			PrintWriter	pw = new PrintWriter( new OutputStreamWriter( baos ));
			
			e.printStackTrace( pw );
			
			pw.close();
			
			log( baos.toString());
			
		}catch( Throwable ignore ){
			
		}
	}
	
	public void
	logAndOut(
		String		str )
	{
		logAndOut( str, false );
	}
	
	public void
	logAndOut(
		String		str,
		boolean		stderr )
	{
		if ( stderr ){
			
			System.err.println( str );
		}else{
			
			System.out.println( str );
		}
		
		log( str );
	}
	
	public void
	logAndOut(
		Throwable 	e )
	{
		e.printStackTrace();
		
		log( e );
	}
	
	public void
	log(
		String	_str )
	{
		if ( !AEDiagnostics.loggers_enabled ){
			
			return;
		}
		
		Calendar now = GregorianCalendar.getInstance();

		StringBuilder str = new StringBuilder( _str.length() + 20 );
		
		String timeStamp =
			"[" + format(now.get(Calendar.DAY_OF_MONTH))+format(now.get(Calendar.MONTH)+1) + " " + 
			format(now.get(Calendar.HOUR_OF_DAY))+ ":" + format(now.get(Calendar.MINUTE)) + ":" + format(now.get(Calendar.SECOND)) + "] ";        

		synchronized( this ){

			if ( first_write ){
				
				first_write = false;
				
				str.append( "\r\n[" );
				str.append( now.get(Calendar.YEAR));
				str.append( "] Log File Opened for " );
				str.append(  Constants.APP_NAME );
				str.append( " " );
				str.append(  Constants.AZUREUS_VERSION );
				str.append( "\r\n" );
			}
			
			str.append( timeStamp );
			str.append( _str );
		
			if ( !direct_writes ){
				
				if ( pending == null ){
					
					pending = new LinkedList<StringBuilder>();
				}
				
				pending.add( str );
				
				pending_size += str.length();
				
				if ( pending_size > MAX_PENDING ){
					
					writePending();
				}
				
				return;
			}
				
			write( str );
		}
	}
	
	private void
	write(
		StringBuilder		str )
	{
		PrintWriter	pw = null;
		
		try{	
			File	log_file	= getLogFile();
			
				/**
				 *  log_file.length will return 0 if the file doesn't exist, so we don't need
				 *  to explicitly check for its existence.
				 */
			
			if ( log_file.length() >= max_size ){
				
				first_file = !first_file;
				
				log_file	= getLogFile();
			
					// If the file doesn't exist, this will just return false.
				
				log_file.delete();
			}
					
			pw = new PrintWriter(new FileWriter( log_file, true ));
		
			pw.println( str );
			
		}catch( Throwable e ){
			
		}finally{
			
			if ( pw != null ){
									
				pw.close();
			}
		}
	}
	
	protected void
	writePending()
	{
		synchronized( this ){
			
			if ( pending == null ){
				
				return;
			}
			
			// System.out.println( getName() + ": flushing " + pending_size );
			
			PrintWriter	pw = null;
			
			try{	
				File	log_file	= getLogFile();
				
					/**
					 *  log_file.length will return 0 if the file doesn't exist, so we don't need
					 *  to explicitly check for its existence.
					 */
				
				if ( log_file.length() >= max_size ){
					
					first_file = !first_file;
					
					log_file	= getLogFile();
				
						// If the file doesn't exist, this will just return false.
					
					log_file.delete();
				}
						
				pw = new PrintWriter(new FileWriter( log_file, true ));
			
				for ( StringBuilder str: pending ){
				
					pw.println( str );
				}
			}catch( Throwable e ){
				
			}finally{
				
				direct_writes 	= true;
				pending			= null;
				
				if ( pw != null ){
										
					pw.close();
				}
			}
		}
	}
	
	private File
	getLogFile()
	{
		return( new File( debug_dir, getName() + "_" + (first_file?"1":"2") + ".log" ));
	}
	
	private static String 
	format(
		int 	n ) 
	{
		if (n < 10){
	   	
			return( "0" + n );
	   }
		
	   return( String.valueOf(n));
	}
}
