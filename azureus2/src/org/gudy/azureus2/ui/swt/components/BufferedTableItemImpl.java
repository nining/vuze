/*
 * File    : BufferedTableItem.java
 * Created : 24 nov. 2003
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

package org.gudy.azureus2.ui.swt.components;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Table;

/**
 * @author Olivier
 *
 */
public abstract class BufferedTableItemImpl implements BufferedTableItem
{

	protected BufferedTableRow row;

	private int position;

	private Color ourFGColor = null;

	public BufferedTableItemImpl(BufferedTableRow row, int position) {
		this.row = row;
		this.position = position;
	}

	public String getText() {
		if (position != -1)
			return row.getText(position);
		return "";
	}

	public boolean setText(String text) {
		if (position != -1)
			return row.setText(position, text);
		return false;
	}

	public void setImage(Image img) {
		if (position != -1)
			row.setImage(position, img);
	}

	public void setRowForeground(Color color) {
		row.setForeground(color);
	}

	public boolean setItemForeground(Color color) {
		if (position == -1)
			return false;

		boolean ok;
		if (ourFGColor != null) {
			ok = row.setForeground(position, color);
			if (ok) {
				if (!color.isDisposed())
					color.dispose();
				ourFGColor = null;
			}
		} else {
			ok = row.setForeground(position, color);
		}
		return ok;
	}
	
	public Color getItemForeground() {
		if (position == -1)
			return null;

		return row.getForeground(position);
	}

	public boolean setItemForeground(int red, int green, int blue) {
		if (position == -1)
			return false;

		Color oldColor = row.getForeground(position);

		RGB newRGB = new RGB(red, green, blue);

		if (oldColor != null && oldColor.getRGB().equals(newRGB)) {
			return false;
		}

		Color newColor = new Color(row.getTable().getDisplay(), newRGB);
		boolean ok = row.setForeground(position, newColor);
		if (ok) {
			if (ourFGColor != null && !ourFGColor.isDisposed())
				ourFGColor.dispose();
			ourFGColor = newColor;
		} else {
			if (!newColor.isDisposed())
				newColor.dispose();
		}

		return ok;
	}

	public Color getBackground() {
		return row.getBackground();
	}

	public Rectangle getBounds() {
		if (position != -1)
			return row.getBounds(position);
		return null;
	}

	public Table getTable() {
		return row.getTable();
	}

	public void dispose() {
		if (ourFGColor != null && !ourFGColor.isDisposed())
			ourFGColor.dispose();
	}

	public boolean isShown() {
		return position != -1;
	}

	public boolean needsPainting() {
		return false;
	}

	public void doPaint(GC gc) {
	}

	public void locationChanged() {
	}

	public int getPosition() {
		return position;
	}

	public String getColumnName() {
		if (!isShown())
			return null;
		Table table = row.getTable();
		if (table != null && !table.isDisposed() && position >= 0
				&& position < table.getColumnCount())
			return table.getColumn(position).getText();

		return null;
	}
	
	public Image getBackgroundImage() {
		Table table = row.getTable();
		
		Rectangle bounds = getBounds();
		
		if (bounds.isEmpty()) {
			return null;
		}
		
		Image image = new Image(table.getDisplay(), bounds.width, bounds.height);
		
		GC gc = new GC(table);
		gc.copyArea(image, bounds.x, bounds.y);
		gc.dispose();
		
		return image;
	}
}
