/**
 * Created on May 3, 2010
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */
 
package org.gudy.azureus2.ui.swt.views.table.impl;

import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT;

/**
 * @author TuxPaper
 * @created May 3, 2010
 *
 */
public class TreeItemDelegate implements TableItemOrTreeItem
{
	TreeItem item;

	public TreeItemDelegate(TreeItem treeItem) {
		this.item = treeItem;
	}

	public TreeItemDelegate(TableOrTreeSWT tree, int style) {
		item = new TreeItem((Tree) tree, style);
	}

	public void addListener(int eventType, Listener listener) {
		item.addListener(eventType, listener);
	}

	public void addDisposeListener(DisposeListener listener) {
		item.addDisposeListener(listener);
	}

	public void clear(int index, boolean all) {
		item.clear(index, all);
	}

	public void clearAll(boolean all) {
		item.clearAll(all);
	}

	public void dispose() {
		item.dispose();
	}

	public boolean equals(Object obj) {
		if (obj instanceof TableItemOrTreeItem) {
			return item.equals(((TableItemOrTreeItem) obj).getItem());
		}
		return item.equals(obj);
	}

	public Color getBackground() {
		return item.getBackground();
	}

	public Color getBackground(int index) {
		return item.getBackground(index);
	}

	public Rectangle getBounds() {
		return item.getBounds();
	}

	public Rectangle getBounds(int index) {
		return item.getBounds(index);
	}

	public Object getData() {
		return item.getData();
	}

	public Object getData(String key) {
		return item.getData(key);
	}

	public Display getDisplay() {
		return item.getDisplay();
	}

	public boolean getChecked() {
		return item.getChecked();
	}

	public Listener[] getListeners(int eventType) {
		return item.getListeners(eventType);
	}

	public boolean getExpanded() {
		return item.getExpanded();
	}

	public int getStyle() {
		return item.getStyle();
	}

	public Font getFont() {
		return item.getFont();
	}

	public Font getFont(int index) {
		return item.getFont(index);
	}

	public Color getForeground() {
		return item.getForeground();
	}

	public Color getForeground(int index) {
		return item.getForeground(index);
	}

	public boolean getGrayed() {
		return item.getGrayed();
	}

	public void notifyListeners(int eventType, Event event) {
		item.notifyListeners(eventType, event);
	}

	public TableItemOrTreeItem getItem(int index) {
		return new TreeItemDelegate(item.getItem(index));
	}

	public int getItemCount() {
		return item.getItemCount();
	}

	public TableItemOrTreeItem[] getItems() {
		TreeItem[] items = item.getItems();
		TableItemOrTreeItem[] returnItems = new TableItemOrTreeItem[items.length];
		for (int i = 0; i < returnItems.length; i++) {
			returnItems[i] = new TreeItemDelegate(items[i]);
		}
		return returnItems;
	}

	public Image getImage() {
		return item.getImage();
	}

	public Image getImage(int index) {
		return item.getImage(index);
	}

	public Rectangle getImageBounds(int index) {
		return item.getImageBounds(index);
	}

	public void removeListener(int eventType, Listener listener) {
		item.removeListener(eventType, listener);
	}

	public TableOrTreeSWT getParent() {
		return new TreeDelegate(item.getParent());
	}

	public TableItemOrTreeItem getParentItem() {
		return new TreeItemDelegate(item.getParentItem());
	}

	public String getText() {
		return item.getText();
	}

	public String getText(int index) {
		return item.getText(index);
	}

	public void removeDisposeListener(DisposeListener listener) {
		item.removeDisposeListener(listener);
	}

	public Rectangle getTextBounds(int index) {
		return item.getTextBounds(index);
	}

	public int hashCode() {
		return item.hashCode();
	}

	public boolean isDisposed() {
		return item.isDisposed();
	}

	public boolean isListening(int eventType) {
		return item.isListening(eventType);
	}

	public int indexOf(TableItemOrTreeItem item2) {
		return item.indexOf((TreeItem) item2.getItem());
	}

	public void removeAll() {
		item.removeAll();
	}

	public void setBackground(Color color) {
		item.setBackground(color);
	}

	public void setBackground(int index, Color color) {
		item.setBackground(index, color);
	}

	public void setData(Object data) {
		item.setData(data);
	}

	public void setData(String key, Object value) {
		item.setData(key, value);
	}

	public void setChecked(boolean checked) {
		item.setChecked(checked);
	}

	public void setExpanded(boolean expanded) {
		item.setExpanded(expanded);
	}

	public void setFont(Font font) {
		item.setFont(font);
	}

	public String toString() {
		return item.toString();
	}

	public void setFont(int index, Font font) {
		item.setFont(index, font);
	}

	public void setForeground(Color color) {
		item.setForeground(color);
	}

	public void setForeground(int index, Color color) {
		item.setForeground(index, color);
	}

	public void setGrayed(boolean grayed) {
		item.setGrayed(grayed);
	}

	public void setImage(Image[] images) {
		item.setImage(images);
	}

	public void setImage(int index, Image image) {
		item.setImage(index, image);
	}

	public void setImage(Image image) {
		item.setImage(image);
	}

	public void setItemCount(int count) {
		item.setItemCount(count);
	}

	public void setText(String[] strings) {
		item.setText(strings);
	}

	public void setText(int index, String string) {
		item.setText(index, string);
	}

	public void setText(String string) {
		item.setText(string);
	}

	//////
	
	public Item getItem() {
		return item;
	}
}
