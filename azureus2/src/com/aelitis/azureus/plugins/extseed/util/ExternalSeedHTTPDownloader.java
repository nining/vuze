/*
 * Created on 16-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.plugins.extseed.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.StringTokenizer;

import com.aelitis.azureus.plugins.extseed.ExternalSeedException;

public class 
ExternalSeedHTTPDownloader 
{
	public static final String	NL = "\r\n";
	

	private URL			url;
	private String		user_agent;
	
	private int			last_response;
	private byte[]		last_response_data;
	
	public
	ExternalSeedHTTPDownloader(
		URL		_url,
		String	_user_agent )
	{
		url			= _url;
		user_agent	= _user_agent;
	}
	
	public void
	download(
		int									length,
		ExternalSeedHTTPDownloaderListener	listener )
	
		throws ExternalSeedException
	{
		download( new String[0], new String[0], length, listener );
	}
	
	public void
	downloadRange(
		long								offset,
		int									length,
		ExternalSeedHTTPDownloaderListener	listener )
	
		throws ExternalSeedException
	{
		download( 
					new String[]{ "Range" }, new String[]{ "bytes=" + offset + "-" + (offset+length-1)},
					length,
					listener );
	}
	
	public void
	download(
		String[]							prop_names,
		String[]							prop_values,
		int									length,
		ExternalSeedHTTPDownloaderListener	listener )
	
		throws ExternalSeedException
	{
		try{
			HttpURLConnection	connection = (HttpURLConnection)url.openConnection();
			
			connection.setRequestProperty( "Connection", "Keep-Alive" );
			connection.setRequestProperty( "User-Agent", user_agent );
			
			for (int i=0;i<prop_names.length;i++){
				
				connection.setRequestProperty( prop_names[i], prop_values[i] );
			}
			
			int	response = connection.getResponseCode();
			
			last_response	= response;
			
			if ( 	response == HttpURLConnection.HTTP_ACCEPTED || 
					response == HttpURLConnection.HTTP_OK ||
					response == HttpURLConnection.HTTP_PARTIAL ){
								
				InputStream	is = connection.getInputStream();
				
				try{
					int	pos = 0;
					
					byte[]	buffer 		= null;
					int		buffer_pos	= 0;

					while( pos < length ){
						
						if ( buffer == null ){
							
							buffer = listener.getBuffer();
						}

						int	len = is.read( buffer, buffer_pos, buffer.length-buffer_pos );
						
						if ( len < 0 ){
							
							break;
						}
						
						pos	+= len;
						
						buffer_pos	+= len;
						
						if ( buffer_pos == buffer.length ){
							
							listener.done();
							
							buffer		= null;
							buffer_pos	= 0;
						}
					}
					
					if ( pos != length ){
						
						String	log_str;
						
						if ( buffer == null ){
							
							log_str = "No buffer assigned";
							
						}else{
							
							log_str =  new String( buffer, 0, length );
							
							if ( log_str.length() > 64 ){
								
								log_str = log_str.substring( 0, 64 );
							}
						}
						
						throw( new ExternalSeedException("Connection failed: data too short - " + length + "/" + pos + " [" + log_str + "]" ));
					}
					
					// System.out.println( "download length: " + pos );
					
				}finally{
					
					is.close();
				}
				
			}else{
				
				ExternalSeedException	error = new ExternalSeedException("Connection failed: " + connection.getResponseMessage());
				
				error.setPermanentFailure( true );
				
				throw( error );
			}
		}catch( Throwable e ){
			
			if ( e instanceof ExternalSeedException ){
				
				throw((ExternalSeedException)e);
			}
			
			throw( new ExternalSeedException("Connection failed", e ));
		}
	}
	
	public void
	downloadSocket(
		int									length,
		ExternalSeedHTTPDownloaderListener	listener )
	        	
	    throws ExternalSeedException
	{
		downloadSocket( new String[0], new String[0], length, listener );
	}
	
	public void
	downloadSocket(
		String[]							prop_names,
		String[]							prop_values,
		int									length,
		ExternalSeedHTTPDownloaderListener	listener )
	
		throws ExternalSeedException
	{
		Socket	socket	= null;
		
		try{
			ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			
			PrintWriter	pw = new  PrintWriter( baos );
					
			pw.print( "GET " + url.getPath() + "?" + url.getQuery() + " HTTP/1.0" + NL );
			pw.print( "Host: " + url.getHost() + (url.getPort()==-1?"":":" + url.getPort()) + NL );	// TODO: HTTPS
			pw.print( "Accept: */*" + NL );
			pw.print( "Connection: Keep-Alive" + NL );
			pw.print( "User-Agent: " + user_agent + NL );
		
			for (int i=0;i<prop_names.length;i++){
				
				pw.print( prop_names[i] + ":" + prop_values[i] + NL );
			}
			
			pw.print( NL );
			pw.flush();
			
			socket = new Socket(  url.getHost(), url.getPort()==-1?url.getDefaultPort():url.getPort());
			
			OutputStream	os = socket.getOutputStream();
			
			os.write( baos.toByteArray());
			
			os.flush();
			
			InputStream is = new BufferedInputStream( socket.getInputStream());
			
			try{
				String	header = "";
				
				while( true ){
					
					byte[]	buffer = new byte[1];
					
					int	len = is.read( buffer );
					
					if ( len < 0 ){
						
						throw( new IOException( "input too short reading header" ));
					}
					
					header	+= (char)buffer[0];
					
					if ( header.endsWith(NL+NL)){
					
						break;
					}
				}
								
				// HTTP/1.1 403 Forbidden
				
				int	line_end = header.indexOf(NL);
				
				if ( line_end == -1 ){
					
					throw( new IOException( "header too short" ));
				}
				
				String	first_line = header.substring(0,line_end);
				
				StringTokenizer	tok = new StringTokenizer(first_line, " " );
				
				tok.nextToken();
				
				int	response = Integer.parseInt( tok.nextToken());
				
				last_response	= response;
	
				String	response_str	= tok.nextToken();				
				
				if ( 	response == HttpURLConnection.HTTP_ACCEPTED || 
						response == HttpURLConnection.HTTP_OK ||
						response == HttpURLConnection.HTTP_PARTIAL ){
					
					byte[]	buffer 		= null;
					int		buffer_pos	= 0;
					
					int	pos = 0;
					
					while( pos < length ){
						
						if ( buffer == null ){
							
							buffer = listener.getBuffer();
						}
						
						int	len = is.read( buffer, buffer_pos, buffer.length-buffer_pos );
						
						if ( len < 0 ){
							
							break;
						}
						
						pos	+= len;
						
						buffer_pos	+= len;
						
						if ( buffer_pos == buffer.length ){
							
							listener.done();
							
							buffer		= null;
							buffer_pos	= 0;
						}
					}
					
					if ( pos != length ){
						
						String	log_str;
						
						if ( buffer == null ){
							
							log_str = "No buffer assigned";
							
						}else{
							
							log_str =  new String( buffer, 0, length );
							
							if ( log_str.length() > 64 ){
								
								log_str = log_str.substring( 0, 64 );
							}
						}
						
						throw( new ExternalSeedException("Connection failed: data too short - " + length + "/" + pos + " [" + log_str + "]" ));
					}
					
					// System.out.println( "download length: " + pos );
										
				}else if ( 	response == 503 ){
					
						// webseed support for temp unavail - read the data
					
					String	data_str = "";
					
					while( true ){
						
						byte[]	buffer = new byte[1];
						
						int	len = is.read( buffer );
						
						if ( len < 0 ){
							
							break;
						}
						
						data_str += (char)buffer[0];
					}
					
					last_response_data = data_str.getBytes();
				
				}else{
					
					ExternalSeedException	error = new ExternalSeedException("Connection failed: " + response_str );
					
					error.setPermanentFailure( true );
					
					throw( error );
				}
			}finally{
				
				is.close();
			}
			
		}catch( Throwable e ){
			
			if ( e instanceof ExternalSeedException ){
				
				throw((ExternalSeedException)e);
			}
			
			throw( new ExternalSeedException("Connection failed", e ));
			
		}finally{
			
			if ( socket != null ){
				
				try{
					socket.close();
					
				}catch( Throwable e ){
				}
			}
		}
	}
	
	public int
	getLastResponse()
	{
		return( last_response );
	}
	
	public byte[]
	getLast503ResponseData()
	{
		return( last_response_data );
	}
}
