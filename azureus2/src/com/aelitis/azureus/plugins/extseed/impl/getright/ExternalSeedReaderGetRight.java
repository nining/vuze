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

package com.aelitis.azureus.plugins.extseed.impl.getright;

import java.util.*;
import java.net.URL;
import java.net.URLEncoder;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;

import com.aelitis.azureus.plugins.extseed.ExternalSeedException;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPlugin;
import com.aelitis.azureus.plugins.extseed.ExternalSeedReader;
import com.aelitis.azureus.plugins.extseed.impl.ExternalSeedReaderImpl;
import com.aelitis.azureus.plugins.extseed.impl.ExternalSeedReaderRequest;
import com.aelitis.azureus.plugins.extseed.util.ExternalSeedHTTPDownloader;
import com.aelitis.azureus.plugins.extseed.util.ExternalSeedHTTPDownloaderListener;

public class 
ExternalSeedReaderGetRight
	extends ExternalSeedReaderImpl
{
	private static final int	TARGET_REQUEST_SIZE_DEFAULT	= 256*1024;
	
	private URL			url;
	private String		ip;
	private int			port;
	
	private ExternalSeedHTTPDownloader[]	http_downloaders;
	private long[]							downloader_offsets;
	private long[]							downloader_lengths;
	
	private int			piece_size;

	private int			piece_group_size;
		
	protected
	ExternalSeedReaderGetRight(
		ExternalSeedPlugin 		_plugin,
		Torrent					_torrent,	
		URL						_url,
		Map						_params )
	
		throws Exception
	{
		super( _plugin, _torrent, _params );
				
		int target_request_size	= getIntParam( _params, "req_size", TARGET_REQUEST_SIZE_DEFAULT );
		
		url		= _url;
		
		ip		= url.getHost();
		port	= url.getPort();
		
		if ( port == -1 ){
			
			port = url.getDefaultPort();
		}
			
		piece_size = (int)getTorrent().getPieceSize();
		
		piece_group_size = target_request_size / piece_size;
		
		if ( piece_group_size == 0 ){
			
			piece_group_size	= 1;
		}
		
		TOTorrent	to_torrent = ((TorrentImpl)_torrent).getTorrent();

		String	ua = getUserAgent();
		
		if ( to_torrent.isSimpleTorrent()){
			
			http_downloaders = new ExternalSeedHTTPDownloader[]{ new ExternalSeedHTTPDownloader( url, ua )};
			
			downloader_offsets 	= new long[]{ 0 };
			downloader_lengths	= new long[]{ to_torrent.getSize() };
			
		}else{
			
			TOTorrentFile[]	files = to_torrent.getFiles();
			
			http_downloaders = new ExternalSeedHTTPDownloader[ files.length ];
			
			downloader_offsets 	= new long[ files.length ];
			downloader_lengths	= new long[ files.length ];

			long	offset	= 0;
			
				// encoding is a problem, assume ISO-8859-1
			
			String	base_url = url.toString();
			
			if ( base_url.endsWith( "/" )){
				
				base_url = base_url.substring( 0, base_url.length()-1 );
			}
			
			base_url += "/" + URLEncoder.encode( new String( to_torrent.getName(), "ISO-8859-1" ), "ISO-8859-1" );
			
			for (int i=0;i<files.length;i++ ){
				
				TOTorrentFile	file = files[i];
				
				long length = file.getLength();
					
				String	file_url_str = base_url;
				
				byte[][] bits = file.getPathComponents();
				
				for (int j=0;j<bits.length;j++){
					
					file_url_str += "/" + URLEncoder.encode( new String( bits[j], "ISO-8859-1" ), "ISO-8859-1" );
				}
				
				http_downloaders[i] = new ExternalSeedHTTPDownloader( new URL( file_url_str), ua );
				
				downloader_offsets[i]	= offset;
				downloader_lengths[i]	= length;
				
				offset += length;
			}
		}
	}
	
	public boolean
	sameAs(
		ExternalSeedReader	other )
	{
		if ( other instanceof ExternalSeedReaderGetRight ){
			
			return( url.toString().equals(((ExternalSeedReaderGetRight)other).url.toString()));
		}
		
		return( false );
	}
	
	public String
	getName()
	{
		return( "GR: " + url );
	}
	
	public String
	getIP()
	{
		return( ip );
	}
	
	public int
	getPort()
	{
		return( port );
	}
	
	protected int
	getPieceGroupSize()
	{
		return( piece_group_size );
	}
	
	protected boolean
	getRequestCanSpanPieces()
	{
		return( true );
	}
	
	protected void
	readData(
		final ExternalSeedReaderRequest	request )
	
		throws ExternalSeedException
	{
		setReconnectDelay( RECONNECT_DEFAULT, false );
		
		long	request_start 	= request.getStartPieceNumber() * piece_size + request.getStartPieceOffset();
		int		request_length	= request.getLength();
		
		if ( http_downloaders.length == 1 ){
		
			ExternalSeedHTTPDownloader http_downloader = http_downloaders[0];
			
	        try{
				http_downloader.downloadRange( 
						request_start, 
						request_length,
						request,
						isTransient());
	
	        }catch( ExternalSeedException ese ){
	        	
	        	if ( http_downloader.getLastResponse() == 503 && http_downloader.getLast503RetrySecs() >= 0 ){
			
					int	retry_secs = http_downloader.getLast503RetrySecs();
					
					setReconnectDelay( retry_secs * 1000, true );
					
					throw( new ExternalSeedException( "Server temporarily unavailable, retrying in " + retry_secs + " seconds" ));
	        		
	        	}else{
	        		
	        		throw(ese);                	
	        	}
	        }
		}else{
			
			long	request_end = request_start + request_length;
			
			System.out.println( "Req: start=" + request_start + ", len=" + request_length );
			
			final	byte[][] overlap_buffer 			= { null };
			final	int[]	 overlap_buffer_position 	= { 0 };
			
				// we've got to multiplex the (possible) multiple request buffers onto (possible) multi files
			
			for (int i=0;i<http_downloaders.length;i++){
				
				long	this_start 	= downloader_offsets[i];
				long	this_end	= this_start + downloader_lengths[i];

				if ( this_end <= request_start ){
					
					continue;
				}
					
				if ( this_start >= request_end ){
					
					break;
				}
				
				long	sub_request_start 	= Math.max( request_start, 	this_start );
				long	sub_request_end		= Math.min( request_end,	this_end );
	
				final int	sub_len = (int)( sub_request_end - sub_request_start );
				
				if ( sub_len == 0 ){
					
					continue;
				}
				
				ExternalSeedHTTPDownloader http_downloader = http_downloaders[i];
				
				System.out.println( "    sub_req: start=" + sub_request_start + ", len=" + sub_len + ",url=" + http_downloader.getURL());
				
				ExternalSeedHTTPDownloaderListener sub_request = 
					new ExternalSeedHTTPDownloaderListener()
					{
						private int bytes_read;
							
						private byte[]	current_buffer 			= overlap_buffer[0];
						private int		current_buffer_position	= overlap_buffer_position[0];
						private int		current_buffer_length	= current_buffer==null?-1:Math.min( current_buffer.length, current_buffer_position + sub_len );
						
						public byte[]
			        	getBuffer()
			        	
			        		throws ExternalSeedException
			        	{
							if ( current_buffer == null ){
								
								current_buffer 			= request.getBuffer();
								current_buffer_position	= 0;
								current_buffer_length	= Math.min( current_buffer.length, sub_len - bytes_read );
							}
							
							return( current_buffer );
			        	}
			        	
			        	public void
			        	setBufferPosition(
			        		int	position )
			        	{
			        		current_buffer_position	= position;
			        		
			        		request.setBufferPosition( position );
			        	}
			        	
			        	public int
			        	getBufferPosition()
			        	{
			        		return( current_buffer_position );
			        	}
			        	
			        	public int
			        	getBufferLength()
			        	{
			        		return( current_buffer_length );
			        	}
			        	
			        	public int
			        	getPermittedBytes()
			        	
			        		throws ExternalSeedException
			        	{
			        		return( request.getPermittedBytes());
			        	}
			        	
			        	public void
			        	reportBytesRead(
			        		int		num )
			        	{
			        		bytes_read += num;
			        		
			        		request.reportBytesRead( num );
			        	}
			        	
			        	public void
			        	done()
			        	{
			        			// the current buffer is full up to the declared length
			        		
		        			int rem = current_buffer.length - current_buffer_length;
		        			
			        		if ( bytes_read == sub_len ){
			        			
			        				// this request is complete. save any partial buffer for
			        				// next request
			        			
			        			if ( rem == 0 ){
			        				
			        				overlap_buffer[0] 			= null;
			        				overlap_buffer_position[0]	= 0;
			        				
			        			}else{
			        				
			        				overlap_buffer[0]			= current_buffer;
			        				overlap_buffer_position[0]	= current_buffer_length;
			        			}
			        		}
			        		
			        			// prepare for next buffer if needed
			        		
			        		current_buffer = null;
			        		
			        		if ( rem == 0 ){
			        			
			        			request.done();
			        		}
			        	}
					};
					
		        try{
					http_downloader.downloadRange( 
							sub_request_start - this_start, 
							sub_len,
							sub_request,
							isTransient());
		
		        }catch( ExternalSeedException ese ){
		        	
		        	if ( http_downloader.getLastResponse() == 503 && http_downloader.getLast503RetrySecs() >= 0 ){
				
						int	retry_secs = http_downloader.getLast503RetrySecs();
						
						setReconnectDelay( retry_secs * 1000, true );
						
						throw( new ExternalSeedException( "Server temporarily unavailable, retrying in " + retry_secs + " seconds" ));
		        		
		        	}else{
		        		
		        		throw(ese);                	
		        	}
		        }
			}
		}
	}
}
