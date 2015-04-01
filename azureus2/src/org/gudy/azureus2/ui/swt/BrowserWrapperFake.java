/*
 * Created on Apr 1, 2015
 * Created by Paul Gardner
 * 
 * Copyright 2015 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.gudy.azureus2.ui.swt;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.CloseWindowListener;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;


public class 
BrowserWrapperFake
	extends BrowserWrapper
{
	private Composite		parent;
	
	private Composite		browser;
	
	private String 		url;
	
	private List<LocationListener>		location_listeners 	= new ArrayList<LocationListener>();
	private List<ProgressListener>		progress_listeners 	= new ArrayList<ProgressListener>();
	private List<TitleListener>			title_listeners 	= new ArrayList<TitleListener>();
	
	protected
	BrowserWrapperFake(
		Composite		_parent,
		int				style )
	{
		parent	= _parent;
		
		browser = new Composite( parent, SWT.NULL );
		
		browser.setBackground( Colors.red );
	}
	
	public Composite
	getControl()
	{
		return( browser );
	}
	
	public void
	setBrowser(
		WindowEvent		event )
	{
	}
	
	public void
	setVisible(
		boolean		visible )
	{
		browser.setVisible( visible );
	}
	
	public boolean
	isVisible()
	{
		return( browser.isVisible());
	}
	
	public boolean
	isDisposed()
	{
		return( browser.isDisposed());
	}
	
	public void
	dispose()
	{
		browser.dispose();
	}
	
	public boolean
	execute(
		String		str )
	{
		return( false );
	}
	
	public boolean
	isBackEnabled()
	{
		return( false );
	}
	
	public String
	getUrl()
	{
		return( url );
	}
	
	public void
	setUrl(
		String		_url )
	{
		url		= _url;
		
		for ( LocationListener l: location_listeners ){
			
			try{
				LocationEvent event = new LocationEvent( browser );
				
				event.top 		= true;
				event.location 	= _url;
				
				l.changed( event );
				
			}catch( Throwable e){
				
				Debug.out( e );
			}
		}
		
		for ( ProgressListener l: progress_listeners ){
			
			try{
				ProgressEvent event = new ProgressEvent( browser );
				
				l.completed( event );
				
			}catch( Throwable e){
				
				Debug.out( e );
			}
		}
		
		for (TitleListener l: title_listeners ){
			
			try{
				TitleEvent event = new TitleEvent( browser );
				
				event.title = "Fake Title";
				
				l.changed( event );
				
			}catch( Throwable e){
				
				Debug.out( e );
			}
		}
	}
	
	public void
	setText(
		String		text )
	{
	}
	
	public void
	setData(
		String		key,
		Object		value )
	{
		browser.setData(key, value);
	}

	public Object
	getData(
		String	key )
	{
		return( browser.getData( key ));
	}
	
	public void
	back()
	{
	}
	
	public void
	refresh()
	{
	}
	
	public void
	update()
	{
		browser.update();
	}
	
	public Shell
	getShell()
	{
		return( browser.getShell());
	}
	
	public Display
	getDisplay()
	{
		return( browser.getDisplay());
	}
	
	public Composite
	getParent()
	{
		return( browser.getParent());
	}
	
	public Object
	getLayoutData()
	{
		return( browser.getLayoutData());
	}
	
	public void
	setLayoutData(
		Object	data )
	{
		browser.setLayoutData( data );
	}
	
	public void
	setFocus()
	{
		browser.setFocus();
	}
	
	public void
	addListener(
		int			type,
		Listener	l )
	{
		browser.addListener( type, l );
	}
	
	public void
	addLocationListener(
		LocationListener		l )
	{
		location_listeners.add( l );
	}
	
	public void
	removeLocationListener(
		LocationListener		l )
	{
		location_listeners.remove( l );
	}
	
	public void
	addTitleListener(
		TitleListener		l )
	{
		title_listeners.add( l );
	}
	
	public void
	addProgressListener(
		ProgressListener		l )
	{
		progress_listeners.add( l );
	}
	
	public void
	removeProgressListener(
		ProgressListener		l )
	{
		progress_listeners.remove( l );
	}
	
	public void
	addOpenWindowListener(
		OpenWindowListener		l )
	{
	}
	
	public void
	addCloseWindowListener(
		CloseWindowListener		l )
	{
	}
	
	public void
	addDisposeListener(
		DisposeListener		l )
	{
		browser.addDisposeListener( l );
	}
	
	public void
	removeDisposeListener(
		DisposeListener		l )
	{
		browser.removeDisposeListener( l );
	}
	
	public void
	addStatusTextListener(
		StatusTextListener		l )
	{
	}
	
	public void
	removeStatusTextListener(
		StatusTextListener		l )
	{
	}
	
	public BrowserFunction
	addBrowserFunction(
		String						name,
		final BrowserFunction		bf )
	{	
		return( new BrowserFunctionFake( bf ));
	}
	
	public static class
	BrowserFunctionFake
		extends BrowserFunction
	{
		private final BrowserFunction	bf;
		
		private boolean	disposed;
		
		private 
		BrowserFunctionFake(
			BrowserFunction	_bf )
		{
			bf		= _bf;
			
			bf.bind( this );
		}
		
		public Object 
		function(
			Object[] arguments )
		{
			return( bf.function( arguments ));
		}

		public boolean
		isDisposed()
		{
			return( disposed );
		}
		
		public void
		dispose()
		{
			disposed	= true;
		}
	}
}
