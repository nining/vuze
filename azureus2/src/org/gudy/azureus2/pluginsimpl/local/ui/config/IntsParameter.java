/*
 * File    : GenericParameter.java
 * Created : Nov 21, 2003
 * By      : epall
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
 
package org.gudy.azureus2.pluginsimpl.local.ui.config;


/**
 * @author epall
 *
 */
public class IntsParameter extends ParameterImpl
{
	private int defaultValue;
	private int[] values;
	private String[] labels;
	
	
	public IntsParameter(
	    String key,
			String label,
			int defaultValue,
			int[] values,
			String[] labels)
	{ 
		super(key, label);
    this.defaultValue = defaultValue;
    this.values = values;
    this.labels = labels;
	}


	public int getDefaultValue()
	{
		return defaultValue;
	}
	
	public int[] getValues()
	{
	  return values;
	}
	
	public String[] getLabels()
	{
	  return labels;
	}

}
