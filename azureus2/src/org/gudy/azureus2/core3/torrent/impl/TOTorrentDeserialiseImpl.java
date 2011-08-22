/*
 * File    : TOTorrentDeserialiseImpl.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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

package org.gudy.azureus2.core3.torrent.impl;


import java.io.*;
import java.net.*;
import java.util.*;

import org.gudy.azureus2.core3.html.HTMLUtils;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class 
TOTorrentDeserialiseImpl
	extends TOTorrentImpl
{
	public
	TOTorrentDeserialiseImpl(
		File		file )
		
		throws TOTorrentException
	{	
		if ( !file.exists()){
			throw( new TOTorrentException( 	"Torrent file '" + file.toString() + "' does not exist",
					TOTorrentException.RT_FILE_NOT_FOUND ));

		}
		
		if(!file.isFile()) {
			
			throw( new TOTorrentException( 	"Torrent must be a file ('" + file.toString() + "')",
											TOTorrentException.RT_FILE_NOT_FOUND ));
		}

		if ( file.length() == 0 ){
		
			throw( new TOTorrentException( 	"Torrent is zero length ('" + file.toString() + "')",
											TOTorrentException.RT_ZERO_LENGTH ));
			
		}
			// parg: there used to be a check made that the torrent file wasn't larger than 1MB.
			// However, this as been exceeded! (see bug 826617)
			// As there is no technical reason for this limit I have removed it
		
		FileInputStream fis = null;
		
		try{
				
			fis = new FileInputStream(file);
	
			construct( fis );
	
		}catch( Throwable e ){
			
			throw( new TOTorrentException( "Error reading torrent file '" + file.toString() + " - " + Debug.getNestedExceptionMessage(e),
											TOTorrentException.RT_READ_FAILS ));
			
		}finally{
			
			if ( fis != null ){
				
				try{
					
					fis.close();
					
				}catch( IOException e ){
					
					Debug.printStackTrace( e );
				}
			}
		}
	}
	
	public
	TOTorrentDeserialiseImpl(
		InputStream		is )
		
		throws TOTorrentException
	{		
		construct( is );
	}
	
	public
	TOTorrentDeserialiseImpl(
		byte[]		bytes )
		
		throws TOTorrentException
	{
		construct( bytes );
	}
	public
	TOTorrentDeserialiseImpl(
		Map			map )
		
		throws TOTorrentException
	{
		construct( map );
	}

	protected void
	construct(
		InputStream		is )
	
		throws TOTorrentException
	{
		ByteArrayOutputStream metaInfo = new ByteArrayOutputStream();
		
		try{
			byte[] buf = new byte[32*1024];	// raised this limit as 2k was rather too small
			
			// do a check to see if it's a BEncode file.
			int iFirstByte = is.read();
			
			if (	iFirstByte != 'd' &&
					iFirstByte != 'e' &&
					iFirstByte != 'i' &&
					!(iFirstByte >= '0' && iFirstByte <= '9')){
				
					// often people download an HTML file by accident - if it looks like HTML
					// then produce a more informative error
				
				try{
					metaInfo.write(iFirstByte);
					
					int nbRead;
							
					while ((nbRead = is.read(buf)) > 0 && metaInfo.size() < 32000 ){
						
						metaInfo.write(buf, 0, nbRead);
					}
					
					String	char_data = new String( metaInfo.toByteArray());
					
					if ( char_data.toLowerCase().indexOf( "html") != -1 ){
						
						char_data = HTMLUtils.convertHTMLToText2( char_data );
						
						char_data = HTMLUtils.splitWithLineLength( char_data, 80 );
						
						if ( char_data.length() > 400 ){
							
							char_data = char_data.substring(0,400) + "...";
						}
						
						throw( 	new TOTorrentException( 
									"Contents maybe HTML:\n" + char_data,
									TOTorrentException.RT_DECODE_FAILS ));
					}
				}catch( Throwable e ){
				
					if ( e instanceof TOTorrentException ){
						
						throw((TOTorrentException)e);
					}
					
						// ignore this
				}
				
				throw( new TOTorrentException( "Contents invalid - bad header",
						TOTorrentException.RT_DECODE_FAILS ));
      
			}
			      
			    
			
			metaInfo.write(iFirstByte);
		
			int nbRead;
				
			while ((nbRead = is.read(buf)) > 0){
			
				metaInfo.write(buf, 0, nbRead);
			}
		}catch( Throwable e ){
			
			throw( new TOTorrentException( "Error reading torrent: " + Debug.getNestedExceptionMessage(e),
											TOTorrentException.RT_READ_FAILS ));
		}
		
		construct( metaInfo.toByteArray());
	}
	
	protected void
	construct(
		byte[]		bytes )
		
		throws TOTorrentException
	{
		try{
			Map meta_data = BDecoder.decode(bytes);
	
			// print( "", "", meta_data );
			
			construct( meta_data );
			
		}catch( IOException e ){
			
			throw( new TOTorrentException( 	"Error reading torrent: " + Debug.getNestedExceptionMessage(e),
											TOTorrentException.RT_DECODE_FAILS, e ));
		}
	}
	
	protected void
	construct(
		Map		meta_data )
		
		throws TOTorrentException
	{
		try{
		
			String	announce_url		= null;
			
			boolean	got_announce		= false;
			boolean	got_announce_list	= false;
			
			boolean	bad_announce		= false;
			
				// decode the stuff
			
			Iterator root_it = meta_data.keySet().iterator();
	
			while( root_it.hasNext()){
				
				String	key = (String)root_it.next();
				
				if ( key.equalsIgnoreCase( TK_ANNOUNCE )){
							
					got_announce	= true;
					
					announce_url = readStringFromMetaData( meta_data, TK_ANNOUNCE );
					
					if ( announce_url == null ){
						
						bad_announce = true;
						
					}else{
						
						announce_url = announce_url.replaceAll( " ", "" );
						
						try{
						
							setAnnounceURL( new URL(announce_url));
							
						}catch( MalformedURLException e ){
							
							if ( announce_url.indexOf( "://" ) == -1 ){
								
								announce_url = "http:/" + (announce_url.startsWith("/")?"":"/") + announce_url;
							}
							
							try{
								
								setAnnounceURL(new URL(announce_url));
									
							}catch( MalformedURLException f ){
									
								bad_announce	= true;
							}
						}
					}
					
				}else if ( key.equalsIgnoreCase( TK_ANNOUNCE_LIST )){
	
					got_announce_list	= true;
					
					List	announce_list = null;
					
					Object	ann_list = meta_data.get( TK_ANNOUNCE_LIST );
					
					if ( ann_list instanceof List ){   //some malformed torrents have this key as a zero-sized string instead of a zero-sized list
						
						announce_list = (List)ann_list;
					}
					
					if ( announce_list != null && announce_list.size() > 0 ){
            
			            announce_url = readStringFromMetaData( meta_data, TK_ANNOUNCE );
			            
			            if ( announce_url != null ){
			            	
				            announce_url = announce_url.replaceAll( " ", "" );
			            }
			            
			            boolean announce_url_found = false;
            
						for (int i=0;i<announce_list.size();i++){
							
							Object temp = announce_list.get(i);
							
								// sometimes we just get a byte[]! turn into a list
							
							if ( temp instanceof byte[] ){
							
								List l = new ArrayList();
								
								l.add( temp );
								
								temp = l;
							}
							
							if ( temp instanceof List ){
								
								List	set = (List)temp;
									
								Vector urls = new Vector();
									
								for (int j=0;j<set.size();j++){
									
									String url_str = readStringFromMetaData((byte[])set.get(j));
										
									url_str = url_str.replaceAll( " ", "" );
	                  
						                	//check to see if the announce url is somewhere in the announce-list
																			
						            try{
						            	urls.add( new URL( StringInterner.intern(url_str) ));		
							    
						            	if ( url_str.equalsIgnoreCase( announce_url )) {
						                	
						            		announce_url_found = true;
						            	}
							
						            }catch( MalformedURLException e ){
										
						            	if ( url_str.indexOf( "://" ) == -1 ){
												
						            		url_str = "http:/" + (url_str.startsWith("/")?"":"/") + url_str;
										}
								         
										try{
							           		urls.add( new URL( StringInterner.intern(url_str) ));		
							          
							           		if ( url_str.equalsIgnoreCase( announce_url )) {
							                	
							            		announce_url_found = true;
							            	}
							       
										}catch( MalformedURLException f ){
					
											Debug.printStackTrace( f );
										} 
									}
								}
							
								if ( urls.size() > 0 ){
								
									URL[]	url_array = new URL[urls.size()];
									
									urls.copyInto( url_array );
									
									addTorrentAnnounceURLSet( url_array );
								}								
							}else{
								
								Debug.out( "Torrent has invalid url-list entry (" + temp + ") - ignoring: meta=" + meta_data );
							}
						}
            
			            	// if the original announce url isn't found, add it to the list
							// watch out for those invalid torrents with announce url missing
						
			            if ( !announce_url_found && announce_url != null && announce_url.length() > 0) {
			              try {
			              	Vector urls = new Vector();
			              	urls.add( new URL( StringInterner.intern(announce_url) ));
			              	URL[] url_array = new URL[ urls.size() ];
			              	urls.copyInto( url_array );
			              	addTorrentAnnounceURLSet( url_array );
			              }
			              catch( Exception e ){ 
			            	  Debug.out( "Invalid URL '" + announce_url + "' - meta=" + meta_data, e ); 
			              }
			            }
					}
				}else if ( key.equalsIgnoreCase( TK_COMMENT )){
					
					setComment((byte[])meta_data.get( TK_COMMENT ));
						
				}else if ( key.equalsIgnoreCase( TK_CREATED_BY )){
			
					setCreatedBy((byte[])meta_data.get( TK_CREATED_BY ));

				}else if ( key.equalsIgnoreCase( TK_CREATION_DATE )){
			
					// non standard, don't fail if format wrong
						
					try{
				
						Long creation_date = (Long)meta_data.get( TK_CREATION_DATE );
					
						if ( creation_date != null ){
						
							setCreationDate( creation_date.longValue());
						}
					}catch( Exception e ){
						
						System.out.println( "creation_date extraction fails, ignoring");
					}
									
				}else if ( key.equalsIgnoreCase( TK_INFO )){
					
					// processed later
					
				}else{
					
					Object	prop = meta_data.get( key );
					
					if ( prop instanceof byte[] ){
						
						setAdditionalByteArrayProperty( key, (byte[])prop );
						
					}else if ( prop instanceof Long ){
						
						setAdditionalLongProperty( key, (Long)prop );
						
					}else if ( prop instanceof List ){
						
						setAdditionalListProperty( key, (List)prop );
						
					}else{
						
						setAdditionalMapProperty( key, (Map)prop );
					}
				}
			}

			if ( bad_announce ){
				
				if ( got_announce_list ){
			
					TOTorrentAnnounceURLSet[] sets = getAnnounceURLGroup().getAnnounceURLSets();
					
					if ( sets.length > 0 ){
						
						setAnnounceURL( sets[0].getAnnounceURLs()[0]);
					}else{
						
						throw( new TOTorrentException( 	"ANNOUNCE_URL malformed ('" + announce_url + "' and no usable announce list)",
														TOTorrentException.RT_DECODE_FAILS ));
						
					}
					
				}else{
			
					throw( new TOTorrentException( 	"ANNOUNCE_URL malformed ('" + announce_url + "'",
													TOTorrentException.RT_DECODE_FAILS ));
				}
			}
			
			if ( ! ( got_announce_list || got_announce )){
				
				setAnnounceURL( TorrentUtils.getDecentralisedEmptyURL());
			}

			Map	info = (Map)meta_data.get( TK_INFO );

			if ( info == null ){
				
				throw( new TOTorrentException( "Decode fails, 'info' element not found'",
												TOTorrentException.RT_DECODE_FAILS ));
			}
		
			boolean hasUTF8Keys = info.containsKey(TK_NAME_UTF8);
			
			setName((byte[])info.get( TK_NAME ));
				
			long	piece_length = ((Long)info.get( TK_PIECE_LENGTH )).longValue();
			
			if ( piece_length <= 0 ){
				
				throw( new TOTorrentException( "Decode fails, piece-length is invalid",
						TOTorrentException.RT_DECODE_FAILS ));
			}
			
			setPieceLength( piece_length );
						
			setHashFromInfo( info );
			
			Long simple_file_length = (Long)info.get( TK_LENGTH );
			
			long	total_length = 0;
			
			String encoding = getAdditionalStringProperty("encoding");
			hasUTF8Keys &= encoding == null || encoding.equals(ENCODING_ACTUALLY_UTF8_KEYS);
			
			if ( simple_file_length != null ){
			
				setSimpleTorrent( true );
				
				total_length = simple_file_length.longValue();

				if (hasUTF8Keys) {
					setNameUTF8((byte[])info.get( TK_NAME_UTF8 ));
					setAdditionalStringProperty("encoding", ENCODING_ACTUALLY_UTF8_KEYS);
				}
				
				setFiles( new TOTorrentFileImpl[]{ new TOTorrentFileImpl( this, 0, total_length, new byte[][]{getName()})});
				
			}else{
				
				setSimpleTorrent( false );  
	
				List	meta_files = (List)info.get( TK_FILES );
			
				TOTorrentFileImpl[] files = new TOTorrentFileImpl[ meta_files.size()];

				if (hasUTF8Keys) {
  				for (int i=0;i<files.length;i++){
  					Map	file_map = (Map)meta_files.get(i);
  					
  					hasUTF8Keys &= file_map.containsKey(TK_PATH_UTF8);
  					if (!hasUTF8Keys) {
  						break;
  					}
  				}

  				if (hasUTF8Keys) {
  					setNameUTF8((byte[])info.get( TK_NAME_UTF8 ));
  					setAdditionalStringProperty("encoding", ENCODING_ACTUALLY_UTF8_KEYS);
  				}
				}
				
				for (int i=0;i<files.length;i++){
					
					Map	file_map = (Map)meta_files.get(i);
					
					long	len = ((Long)file_map.get( TK_LENGTH )).longValue();
					
					List	paths = (List)file_map.get( TK_PATH );
					List	paths8 = (List)file_map.get( TK_PATH_UTF8 );
					
					byte[][]	path_comps = null;
					if (paths != null) {
  					path_comps = new byte[paths.size()][];
  					
  					for (int j=0;j<paths.size();j++){
  					
  						path_comps[j] = (byte[])paths.get(j);
  					}
					}
					
					TOTorrentFileImpl file;

					if (hasUTF8Keys) {
  					byte[][]	path_comps8 = new byte[paths8.size()][];
  					
  					for (int j=0;j<paths8.size();j++){
  					
  						path_comps8[j] = (byte[])paths8.get(j);
  					}
					
						file = files[i] = new TOTorrentFileImpl( this, total_length, len, path_comps, path_comps8 );
					} else {
						file = files[i] = new TOTorrentFileImpl( this, total_length, len, path_comps );
					}
					
					total_length += len;
					
						// preserve any non-standard attributes
											
					Iterator file_it = file_map.keySet().iterator();

					while( file_it.hasNext()){
						
						String	key = (String)file_it.next();
						
						if ( 	key.equals( TK_LENGTH ) ||
								key.equals( TK_PATH )){
									
							// standard
							// we don't skip TK_PATH_UTF8 because some code might assume getAdditionalProperty can get it
						}else{
							
							file.setAdditionalProperty( key, file_map.get( key ));
						}
					}
				}
				
				setFiles( files );
			}
							
			byte[]	flat_pieces = (byte[])info.get( TK_PIECES );
			
				// work out how many pieces we require for the torrent
			
			int	pieces_required = (int)((total_length + (piece_length-1)) / piece_length);
			
			int		pieces_supplied = flat_pieces.length/20;
			
			if ( pieces_supplied < pieces_required ){
				
				throw( new TOTorrentException( "Decode fails, insufficient pieces supplied",
						TOTorrentException.RT_DECODE_FAILS ));
			}
			
			if ( pieces_supplied > pieces_required ){
				
				Debug.out( "Torrent '" + new String( getName()) + "' has too many pieces (required=" + pieces_required + ",supplied=" + pieces_supplied + ") - ignoring excess" );
			}

			byte[][]pieces = new byte[pieces_supplied][20];
			
			for (int i=0;i<pieces.length;i++){
				
				System.arraycopy( flat_pieces, i*20, pieces[i], 0, 20 );
			}	
				
			setPieces( pieces );	
			
				// extract and additional info elements
				
			Iterator info_it = info.keySet().iterator();

			while( info_it.hasNext()){

				String	key = (String)info_it.next();
				
				if ( 	key.equals( TK_NAME ) ||
						key.equals( TK_LENGTH ) ||
						key.equals( TK_FILES ) ||
						key.equals( TK_PIECE_LENGTH ) ||
						key.equals( TK_PIECES )){
			
					// standard attributes
									
				}else{
					
					addAdditionalInfoProperty( key, info.get( key ));
				}
			}

			try{
				byte[] ho = (byte[])info.get( TK_HASH_OVERRIDE );
				
				if ( ho != null ){
					
					setHashOverride( ho );
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}catch( Throwable e ){
			
			if ( e instanceof TOTorrentException){
				
				throw((TOTorrentException)e);	
			}
			
			throw( new TOTorrentException( "Torrent decode fails '" + Debug.getNestedExceptionMessageAndStack(e) + "'",
											TOTorrentException.RT_DECODE_FAILS, e ));
		}
	}
	

	public void
	printMap()
	{
		try{
		
			print( "", "root", serialiseToMap());
			
		}catch( TOTorrentException e ){
		
			Debug.printStackTrace( e );
		}
	}
	
	protected void
	print(
		String		indent,
		String		name,
		Map			map )
	{
		System.out.println( indent + name + "{map}" );
				
		Iterator it = map.keySet().iterator();
		
		while( it.hasNext()){
			
			String	key = (String)it.next();
			
			Object	value =  map.get( key );
			
			if ( value instanceof Map ){
				
				print( indent+"  ", key, (Map)value);
				
			}else if ( value instanceof List ){
				
				print( indent+"  ", key, (List)value );
				
			}else if ( value instanceof Long ){
				
				print( indent+"  ", key, (Long)value );
				
			}else{
				
				print( indent+"  ", key, (byte[])value);
			}
		}
	}
	
	protected void
	print(
		String		indent,
		String		name,
		List		list )
	{
		System.out.println( indent + name + "{list}" );
		
		Iterator it = list.iterator();
		
		int	index = 0;
		
		while( it.hasNext()){
			
			Object	value =  it.next();
			
			if ( value instanceof Map ){
				
				print( indent+"  ", "[" + index + "]", (Map)value);
				
			}else if ( value instanceof List ){
				
				print( indent+"  ", "[" + index + "]", (List)value );
				
			}else if ( value instanceof Long ){
				
				print( indent+"  ", "[" + index + "]", (Long)value );
				
			}else{
				
				print( indent+"  ", "[" + index + "]", (byte[])value);
			}
			
			index++;
		}
	}
	protected void
	print(
		String		indent,
		String		name,
		Long		value )
	{
		System.out.println( indent + name + "{long} = " + value.longValue());
	}
	
	protected void
	print(
		String		indent,
		String		name,
		byte[]		value )
	{		
		String	x = new String(value);
		
		boolean	print = true;
		
		for (int i=0;i<x.length();i++){
			
			char	c = x.charAt(i);
			
			if ( c < 128 ){
							
			}else{
				
				print = false;
				
				break;
			}
		}
		
		if ( print ){
			
			System.out.println( indent + name + "{byte[]} = " + x );
			
		}else{
				 
			System.out.println( indent + name + "{byte[], length " + value.length + "}" );
		}
	}
}