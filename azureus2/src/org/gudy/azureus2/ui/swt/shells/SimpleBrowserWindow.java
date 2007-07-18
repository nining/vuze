/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

/**
 * @author TuxPaper
 * @created Jul 18, 2007
 *
 */
public class SimpleBrowserWindow
{
	private Shell shell;

	public SimpleBrowserWindow(Shell parent, String url, double wPct, double hPct,
			boolean allowResize, boolean isModal) {
		if (parent == null) {
			init(parent, url, 0, 0, allowResize, isModal);
		} else {
			Rectangle clientArea = parent.getClientArea();
			init(parent, url, (int) (clientArea.width * wPct),
					(int) (clientArea.height * hPct), allowResize, isModal);
		}
	}

	public SimpleBrowserWindow(Shell parent, String url, int w, int h,
			boolean allowResize, boolean isModal) {
		init(parent, url, w, h, allowResize, isModal);
	}

	private void init(Shell parent, String url, int w, int h, boolean allowResize,
			boolean isModal) {
		if (parent == null) {
			parent = Utils.findAnyShell();
		}

		int style = SWT.DIALOG_TRIM;
		if (allowResize) {
			style |= SWT.RESIZE;
		}
		if (isModal) {
			style |= SWT.APPLICATION_MODAL;
		}
		shell = ShellFactory.createShell(parent, style);

		shell.setLayout(new FillLayout());

		Utils.setShellIcon(shell);

		Browser browser = null;
		
		try {
			browser = new Browser(shell, SWT.None);
		} catch (Throwable t) {
			shell.dispose();
			return;
		}
		
		if (browser == null) {
			shell.dispose();
			return;
		}

		browser.addProgressListener(new ProgressListener() {
			public void completed(ProgressEvent event) {
				shell.open();
			}

			public void changed(ProgressEvent event) {
			}
		});

		browser.addCloseWindowListener(new CloseWindowListener() {
			public void close(WindowEvent event) {
				shell.dispose();
			}
		});

		browser.addTitleListener(new TitleListener() {

			public void changed(TitleEvent event) {
				shell.setText(event.title);
			}

		});

		if (w > 0 && h > 0) {
			shell.setSize(w, h);
		}

		Utils.centerWindowRelativeTo(shell, parent);
		browser.setUrl(url);
		browser.setData("StartURL", url);
	}

	public void waitUntilClosed() {
		Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display, SWT.DIALOG_TRIM);
		shell.setSize(800, 600);

		new SimpleBrowserWindow(shell, "http://google.com", 0.8, 0.5, true, false);

		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
}
