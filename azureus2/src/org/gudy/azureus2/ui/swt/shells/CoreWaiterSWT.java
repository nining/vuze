/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;

public class CoreWaiterSWT
{
	private static boolean DEBUG = false;

	public enum TriggerInThread {
		SWT_THREAD, ANY_THREAD, NEW_THREAD
	}

	private static Shell shell;

	public static void waitForCoreRunning(final AzureusCoreRunningListener l) {
		waitForCore(TriggerInThread.SWT_THREAD, l);
	}

	public static void waitForCore(final TriggerInThread triggerInThread,
			final AzureusCoreRunningListener l) {
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(final AzureusCore core) {
				if (triggerInThread == TriggerInThread.ANY_THREAD) {
					l.azureusCoreRunning(core);
				} else if (triggerInThread == TriggerInThread.NEW_THREAD) {
					new AEThread2("CoreWaiterInvoke", true) {
						public void run() {
							l.azureusCoreRunning(core);
						}
					}.start();
				}
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						// TODO: Need to detect cancel (can't rely on shell status since it may never open)
						if (shell != null && !shell.isDisposed()) {
							shell.dispose();
							shell = null;
						}

						if (triggerInThread == TriggerInThread.SWT_THREAD) {
							l.azureusCoreRunning(core);
						}
					}
				});
			}
		});

		if (!AzureusCoreFactory.isCoreRunning()) {
			if (DEBUG) {
				System.out.println("NOT AVAIL FOR " + Debug.getCompressedStackTrace());
			}
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					showWaitWindow();
				}
			});
		} else if (DEBUG) {
			System.out.println("NO NEED TO WAIT.. CORE AVAIL! "
					+ Debug.getCompressedStackTrace());
		}

	}

	protected static void showWaitWindow() {
		if (shell != null && !shell.isDisposed()) {
			shell.forceActive();
			return;
		}
		
		shell = UIFunctionsManagerSWT.getUIFunctionsSWT().showCoreWaitDlg();
	}
}
