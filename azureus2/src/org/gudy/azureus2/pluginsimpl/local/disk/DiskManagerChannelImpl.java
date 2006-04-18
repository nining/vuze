/*
 * Created on 29-Mar-2006
 * Created by Paul Gardner
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.disk;

import java.util.*;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfoListener;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.plugins.disk.DiskManagerChannel;
import org.gudy.azureus2.plugins.disk.DiskManagerEvent;
import org.gudy.azureus2.plugins.disk.DiskManagerListener;
import org.gudy.azureus2.plugins.disk.DiskManagerRequest;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.pluginsimpl.local.utils.PooledByteBufferImpl;

public class 
DiskManagerChannelImpl 
	implements DiskManagerChannel, DiskManagerFileInfoListener
{
	private static final int COMPACT_DELAY	= 32;
	
	private static Comparator comparator = new
		Comparator()
		{
			public int 
		   	compare(
		   		Object _o1, 
				Object _o2)
			{
				dataEntry	o1 = (dataEntry)_o1;
				dataEntry	o2 = (dataEntry)_o2;
				
				long	offset1 = o1.getOffset();
				long	length1	= o1.getLength();
				
				long	offset2 = o2.getOffset();
				long	length2	= o2.getLength();
			
		   	
				long	res;
				
				if ( offset1 == offset2 ){
					
					res = length1 - length2;
					
				}else{
					
					res = offset1 - offset2;
				}
				
				if ( res == 0 ){
					return(0);
				}else if ( res < 0 ){
					return(-1);
				}else{
					return(1);
				}
			}
		};
		
	private DiskManagerFileInfoImpl		file;

	private Set	data_written = new TreeSet( comparator );
	
	private int compact_delay	= COMPACT_DELAY;
	
	private List	waiters	= new ArrayList();

	private Average	byte_rate = Average.getInstance( 1000, 20 );
	
	protected
	DiskManagerChannelImpl(
		DiskManagerFileInfoImpl		_file )
	{
		file		= _file;
		
		file.getCore().addListener( this );
	}
	
	public DiskManagerRequest
	createRequest()
	{
		return( new request());
	}
	
	public void
	dataWritten(
		long	offset,
		long	length )
	{
		// System.out.println( "data written:" + offset + "/" + length );
		
		dataEntry	entry = new dataEntry( offset, length );
		
		synchronized( this ){
			
			data_written.add( entry );
			
			compact_delay--;
			
			if ( compact_delay == 0 ){
				
				compact_delay	= COMPACT_DELAY;
				
				Iterator	it = data_written.iterator();
				
				dataEntry	prev_e	= null;
				
				while( it.hasNext()){
					
					dataEntry	this_e = (dataEntry)it.next();
					
					if ( prev_e == null ){
						
						prev_e = this_e;
						
					}else{
						
						long	prev_offset = prev_e.getOffset();
						long	prev_length	= prev_e.getLength();
						long	this_offset = this_e.getOffset();
						long	this_length	= this_e.getLength();
						
						if ( this_offset <= prev_offset + prev_length ){
							
							// System.out.println( "merging: " + prev_e.getString()  + "/" + this_e.getString());
							
							it.remove();
							
							prev_e.setLength( Math.max( prev_offset + prev_length, this_offset + this_length ) - prev_offset );
						
						}else{
							
							prev_e = this_e;
						}
					}
				}
			}
			
			for (int i=0;i<waiters.size();i++){
				
				((AESemaphore)waiters.get(i)).release();
			}
		}
	}
	
	public void
	dataChecked(
		long	offset,
		long	length )
	{
		// System.out.println( "data checked:" + offset + "/" + length );
	}
	
	public void
	destroy()
	{
		file.getCore().removeListener( this );
	}
	
	protected class
	request 
		implements DiskManagerRequest
	{
		private int		request_type;
		private long	request_offset;
		private long	request_length;
		private List	listeners	= new ArrayList();
		
		private volatile boolean	cancelled;
		
		AESemaphore	wait_sem = new AESemaphore( "DiskManagerChannelImpl:wait" );
		
		public void
		setType(
			int			_type )
		{
			request_type		= _type;
		}
		
		public void
		setOffset(
			long		_offset )
		{
			request_offset	= _offset;
		}
		
		public void
		setLength(
			long		_length )
		{
			request_length	= _length;
		}
		
		
		public void
		run()
		{
			int	max_chunk = 65536;
			
			long	rem = request_length;
			
			long	pos = request_offset;
			
			try{

				while( rem > 0 && !cancelled ){
					
					int	len = 0;
					
					synchronized( data_written ){
						
						Iterator	it = data_written.iterator();
						
						while( it.hasNext()){
							
							dataEntry	entry = (dataEntry)it.next();
							
							long	entry_offset = entry.getOffset();
							
							if ( entry_offset > pos ){
																
								break;
							}
							
							long	entry_length = entry.getLength();
							
							long	available = entry_offset + entry_length - pos;
							
							if ( available > 0 ){
								
								len = (int)( available<max_chunk?available:max_chunk);
								
								break;
							}
						}
					}				

						// TODO: use byte_rate * some buffering size to dynamically prioritise pieces
						// for downloading
					
					if ( len > 0 ){
						
						DirectByteBuffer buffer = file.getCore().read( pos, len );
	
						byte_rate.addValue( len );
												
						inform( new event( new PooledByteBufferImpl( buffer ), pos, len ));
						
						pos += len;
						
						rem -= len;
						
					}else{
						
						inform( new event( pos ));
						
						synchronized( this ){
							
							waiters.add( wait_sem );
						}
						
						try{
							wait_sem.reserve();
							
						}finally{
							
							synchronized( this ){
								
								waiters.remove( wait_sem );
							}
						}
					}
				}
			}catch( Throwable e ){
				
				inform( e );
			}
		}
		
		public void
		cancel()
		{
			cancelled	= true;
						
			inform( new Throwable( "Request cancelled" ));

			wait_sem.release();
		}
		
		protected void
		inform(
			Throwable e )
		{
			inform( new event( e ));
		}
		
		protected void
		inform(
			event		ev )
		{
			for (int i=0;i<listeners.size();i++){
				
				try{
					((DiskManagerListener)listeners.get(i)).eventOccurred( ev );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		public void
		addListener(
			DiskManagerListener	listener )
		{
			listeners.add( listener );
		}
	
		public void
		removeListener(
			DiskManagerListener	listener )
		{
			listeners.remove( listener );
		}
		
		protected class
		event
			implements DiskManagerEvent
		{
			private int					event_type;
			private Throwable			error;
			private PooledByteBuffer	buffer;
			private long				event_offset;
			private int					event_length;
			
			protected
			event(
				Throwable		_error )
			{
				event_type	= DiskManagerEvent.EVENT_TYPE_FAILED;
				error		= _error;
			}
			
			protected 
			event(
				long				_offset )
			{
				event_type		= DiskManagerEvent.EVENT_TYPE_BLOCKED;

				event_offset	= _offset;	
			}
			
			protected
			event(
				PooledByteBuffer	_buffer,
				long				_offset,
				int					_length )
			{
				event_type		= DiskManagerEvent.EVENT_TYPE_SUCCESS;
				buffer			= _buffer;
				event_offset	= _offset;
				event_length	= _length;
			}
			
			public int
			getType()
			{
				return( event_type );
			}
			
			public DiskManagerRequest
			getRequest()
			{
				return( request.this );
			}
			
			public long
			getOffset()
			{
				return( event_offset );
			}
			
			public int
			getLength()
			{
				return( event_length );
			}
			
			public PooledByteBuffer
			getBuffer()
			{
				return( buffer );
			}
			
			public Throwable
			getFailure()
			{
				return( error );
			}
		}
	}
	
	protected static class
	dataEntry
	{
		private long	offset;
		private long	length;
	
		protected
		dataEntry(
			long		_offset,
			long		_length )
		{
			offset	= _offset;
			length	= _length;
		}
		
		protected long
		getOffset()
		{
			return( offset );
		}
		
		protected long
		getLength()
		{
			return( length );
		}
		
		protected void
		setLength(
			long	_length )
		{
			length	= _length;
		}
		
		protected String
		getString()
		{
			return( "offset=" + offset + ",length=" + length );
		}
	}
}
