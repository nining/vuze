/*
 * Created on Oct 2, 2012
 * Created by Paul Gardner
 * 
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */


package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.browser.CloseWindowListener;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.Debug;


public abstract class 
BrowserWrapper 
{
	public static BrowserWrapper
	createBrowser(
		Composite		composite,
		int				style )
	{
		return( new BrowserWrapperSWT( composite, style ));
	}
	
	protected
	BrowserWrapper()
	{
	}
	
	public abstract Composite
	getControl();
	
	public abstract void
	setBrowser(
		WindowEvent		event );
	
	public abstract  void
	setVisible(
		boolean		visible );
	
	public abstract boolean
	isVisible();
	
	public abstract  boolean
	isDisposed();
	
	public abstract void
	dispose();
	
	public abstract  boolean
	execute(
		String		str );
	
	public abstract boolean
	isBackEnabled();
	
	public abstract String
	getUrl();
	
	public abstract void
	setUrl(
		String		url );
	
	public abstract void
	setText(
		String		text );
	
	public abstract void
	setData(
		String		key,
		Object		value );
	
	public abstract Object
	getData(
		String	key );
	
	public abstract void
	back();
	
	public abstract void
	refresh();
	
	public abstract void
	update();
	
	public abstract Shell
	getShell();
	
	public abstract Display
	getDisplay();
	
	public abstract Composite
	getParent();
	
	public abstract Object
	getLayoutData();
	
	public abstract void
	setLayoutData(
		Object	data );
	
	public abstract void
	setFocus();
	
	public abstract void
	addListener(
		int			type,
		Listener	l );
	
	public abstract void
	addLocationListener(
		LocationListener		l );
	
	public abstract void
	removeLocationListener(
		LocationListener		l );
	
	public abstract void
	addTitleListener(
		TitleListener		l );
	
	public abstract void
	addProgressListener(
		ProgressListener		l );
	
	public abstract void
	removeProgressListener(
		ProgressListener		l );
	
	public abstract void
	addOpenWindowListener(
		OpenWindowListener		l );
	
	public abstract void
	addCloseWindowListener(
		CloseWindowListener		l );
	
	public abstract void
	addDisposeListener(
		DisposeListener		l );
	
	public abstract void
	removeDisposeListener(
		DisposeListener		l );
	
	public abstract void
	addStatusTextListener(
		StatusTextListener		l );
	
	public abstract void
	removeStatusTextListener(
		StatusTextListener		l );
	
	public abstract BrowserFunction
	addBrowserFunction(
		String				name,
		BrowserFunction		bf );
	
	public static abstract class
	BrowserFunction
	{
		private BrowserFunction		delegate;
		
		protected void
		bind(
			BrowserFunction		_delegate )
		{
			delegate = _delegate;
		}
		
		public abstract Object 
		function(
			Object[] arguments );

		public boolean
		isDisposed()
		{
			if ( delegate != null ){
				
				return( delegate.isDisposed());
			}
			
			Debug.out( "wrong" );
			
			return( false );
		}
		
		public void
		dispose()
		{
			if ( delegate != null ){
				
				delegate.dispose();
			}
			
			Debug.out( "wrong" );
		}
	}
}
