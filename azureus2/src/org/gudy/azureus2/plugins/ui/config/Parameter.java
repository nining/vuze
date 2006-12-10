/*
 * File    : Parameter.java
 * Created : 30 nov. 2003
 * By      : Olivier
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
 
package org.gudy.azureus2.plugins.ui.config;

import org.gudy.azureus2.plugins.config.ConfigParameter;

/**
 * represents a generic parameter description
 * @author Olivier
 *
 */
public interface
Parameter
	extends ConfigParameter
{
	/**
	 * Sets whether the UI object for this parameter is enabled (changeable) or
	 * disabled (not changeable, and usualy grayed out)
	 * 
	 * @param enabled The new enabled state
	 * 
	 * @since 2.3.0.0
	 */
	public void
	setEnabled(
		boolean	enabled );
	
	/**
	 * Retrieves the enabled state for the UI object for this parameter
	 * 
	 * @return The enabled state
	 * 
	 * @since 2.3.0.0
	 */
	public boolean
	isEnabled();

	
	/**
	 * Sets whether the UI object for this parameter is visble to the user
	 * 
	 * @param visible The new visibility state
	 * 
	 * @since 2.3.0.4
	 */
	public void
	setVisible(
		boolean	visible );
	
	/**
	 * Retrieves the visiblility state for the UI object for this parameter
	 * 
	 * @return The visibility state
	 * 
	 * @since 2.3.0.4
	 */
	public boolean
	isVisible();
	
	/**
	 * Adds a listener triggered when the parameter is changed by the user
	 * 
	 * @param l Listener to add
	 * 
	 * @since 2.1.0.2
	 */
	public void
	addListener(
		ParameterListener	l );
	
	/**
	 * Removes a previously added listener
	 * 
	 * @param l Listener to remove.
	 * 
	 * @since 2.1.0.2
	 */
	public void
	removeListener(
		ParameterListener	l );
	
	/**
	 * Retrieve the actual text of the label associated with this parameter.
	 * This is the text after it has been looked up in the language bundle.
	 * 
	 * @return The label's text
	 * 
	 * @since 2.3.0.6
	 */
	public String getLabelText();
	
	/**
	 * Set the text of the label associated to with this parameter to the literal
	 * text supplied.
	 * 
	 * @param sText The actual text to assign to the label
	 * 
	 * @since 2.3.0.6
	 */
	public void setLabelText(String sText);
	
	/**
	 * Retrieve the language bundle key for the label associated with this 
	 * parameter.
	 * 
	 * @return The language bundle key, or null if the label is using literal
	 *          text
	 * 
	 * @since 2.3.0.6
	 */
	public String getLabelKey();
	
	/**
	 * Set the label to use the supplied language bundle key for the label
	 * associated with this parameter
	 * 
	 * @param sLabelKey The language bundle key to use.
	 * 
	 * @since 2.3.0.6
	 */
	public void setLabelKey(String sLabelKey);
}
