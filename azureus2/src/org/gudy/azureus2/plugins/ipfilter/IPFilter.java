/*
 * File    : IPFilter.java
 * Created : 02-Mar-2004
 * By      : parg
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

package org.gudy.azureus2.plugins.ipfilter;

/**
 * @author parg
 *
 */

import java.io.File;

public interface 
IPFilter
{
	public File
	getFile();

		/**
		 * creates a new range but *doesn't* add it to the list. Use the add method
		 * to add it
		 * @param this_session_only	// not persisted if "this_session_only" is true
		 * @return
		 */
	
	public IPRange
	createRange(
		boolean this_session_only );
	
		/**
		 * Adds a range. Only ranges created with "create" above can be added
		 * @param range
		 */
	
	public void
	addRange(
		IPRange		range );
	
	public void
	reload()
	
		throws IPFilterException;
	
	public IPRange[]
	getRanges();

	public boolean 
	isInRange(
		String IPAddress );
	
	public IPBlocked[]
	getBlockedIPs();
	
	public void 
	block(
		String IPAddress);
}
