/*
 * Created on May 1, 2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt;

import java.util.*;

import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.ILogAlertListener;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

import com.aelitis.azureus.util.MapUtils;

/**
 * Utility methods to display popup window
 */
public class Alerts
{

	/**
	 * alert queue is used at startup, prior to initialization to collect
	 * and incoming alerts and start them.  Once initialization is complete,
	 * the queue is processed (and moved to alert_history) and cleared
	 */
	private static List<LogAlert> alert_queue = new ArrayList<LogAlert>();

	private static AEMonitor alert_queue_mon = new AEMonitor("Alerts:Q");

	private static ArrayList<String> alert_history = new ArrayList<String>(0);
	
	private static ArrayList<LogAlert> listUnviewedLogAlerts = new ArrayList<LogAlert>(0);

	private static AEMonitor alert_history_mon = new AEMonitor("Alerts:H");

	private static ArrayList<AlertHistoryListener> listMessageHistoryListeners = new ArrayList<AlertHistoryListener>(1); 

	private static boolean initialisation_complete = false;

	private static transient boolean stopping;

	private static List<AlertListener> listeners = new ArrayList<AlertListener>();

	private Alerts() {
	}
	/**
	 * @param alert
	 *
	 * @since 3.0.0.9
	 */
	protected static void showAlert(LogAlert alert) {
		System.out.println("showlare " + alert.getText());
		final Display display = SWTThread.getInstance().getDisplay();

		if (alert.err != null) {
			alert.details = Debug.getStackTrace(alert.err);
		}

		/*
		final String message2;
		if (alert.text != null
				&& COConfigurationManager.getBooleanParameter("Show Timestamp For Alerts")) {
			message2 = "["
					+ DisplayFormatters.formatDateShort(SystemTime.getCurrentTime())
					+ "] " + alert.text;
		} else {
			message2 = (alert.text == null) ? "" : alert.text;
		}
		*/

		for (Iterator<AlertListener> iter = listeners.iterator(); iter.hasNext();) {
			AlertListener l = (AlertListener) iter.next();
			if (!l.allowPopup(alert.relatedTo, alert.entryType)) {
				return;
			}
		}
		
	
		if (stopping || display.isDisposed()) {

			try {
				alert_queue_mon.enter();

				List close_alerts = COConfigurationManager.getListParameter(
						"Alerts.raised.at.close", new ArrayList());

				Map alert_map = new HashMap();

				alert_map.put("type", new Long(alert.entryType));
				alert_map.put("message", alert.text);
				alert_map.put("timeout", new Long( alert.getGivenTimeoutSecs()));
				
				if (alert.details != null) {
					alert_map.put("details", alert.details);
				}

				close_alerts.add(alert_map);

				COConfigurationManager.setParameter("Alerts.raised.at.close",
						close_alerts);

				return;
			} finally {
				alert_queue_mon.exit();
			}
		}

		if (display.isDisposed()) {
			return;
		}

		
		String key = (alert.err == null) ? alert.text : alert.text + ":"
				+ alert.err.toString();
		try {
			alert_history_mon.enter();

			if (!alert.repeatable) {
				if (alert_history.contains(key)) {
					return;
				}

				alert_history.add(key);

				if (alert_history.size() > 512) {
					alert_history.remove(0);
				}
			}

			listUnviewedLogAlerts.add(alert);
		} finally {
			alert_history_mon.exit();
		}
		
		AlertHistoryListener[] array = listMessageHistoryListeners.toArray(new AlertHistoryListener[0]);
		for (AlertHistoryListener l : array) {
			l.alertHistoryAdded(alert);
		}
	}

	public static void initComplete() {
		new AEThread2("Init Complete",true) {
			public void run() {
				try {
					alert_queue_mon.enter();

					initialisation_complete = true;

					for (int i = 0; i < alert_queue.size(); i++) {
						LogAlert alert = alert_queue.get(i);

						showAlert(alert);
					}

					List close_alerts = COConfigurationManager.getListParameter(
							"Alerts.raised.at.close", new ArrayList());

					
					if (close_alerts.size() > 0) {

						COConfigurationManager.setParameter("Alerts.raised.at.close",
								new ArrayList());

						String intro = MessageText.getString("alert.raised.at.close")
								+ "\n";

						for (int i = 0; i < close_alerts.size(); i++) {

							try {
								Map alert_map = (Map) close_alerts.get(i);

								BDecoder.decodeStrings(alert_map);
								
								String details = MapUtils.getMapString(alert_map, "details", null);
								
								int timeout = MapUtils.getMapInt(alert_map, "timeout", -1);
								
								int entryType = MapUtils.getMapInt(alert_map, "type", 0);
								
								String message = intro + MapUtils.getMapString(alert_map, "message", "");
								
								LogAlert logAlert = new LogAlert(false, entryType, message, timeout);
								logAlert.details = details;
								
								showAlert(logAlert);

							} catch (Throwable e) {

								Debug.printStackTrace(e);
							}
						}
					}

					alert_queue.clear();

				} finally {

					alert_queue_mon.exit();
				}
			}
		}.start();
	}

	public static void stopInitiated() {
		System.out.println("stopping");
		stopping = true;
	}

	public static void init() {
		Logger.addListener(new ILogAlertListener() {
			/* (non-Javadoc)
			 * @see org.gudy.azureus2.core3.logging.ILogAlertListener#alertRaised(org.gudy.azureus2.core3.logging.LogAlert)
			 */
			public void alertRaised(LogAlert alert) {
				if (!initialisation_complete) {
					try {
						alert_queue_mon.enter();

						alert_queue.add(alert);

					} finally {

						alert_queue_mon.exit();
					}

					return;
				}

				showAlert(alert);
			}
		});
	}

	
	public static void addListener(AlertListener l) {
		listeners .add(l);
	}
	
	public static interface AlertListener {
		public boolean allowPopup(Object[] relatedObjects, int configID);
	}

	
	public static ArrayList<LogAlert> getUnviewedLogAlerts() {
		return new ArrayList<LogAlert>(listUnviewedLogAlerts);
	}

	public static void addMessageHistoryListener(AlertHistoryListener l) {
		listMessageHistoryListeners.add(l);
	}
	
	public static void markAlertAsViewed(LogAlert alert) {
		boolean removed = listUnviewedLogAlerts.remove(alert);
		if (removed) {
			AlertHistoryListener[] array = listMessageHistoryListeners.toArray(new AlertHistoryListener[0]);
			for (AlertHistoryListener l : array) {
				l.alertHistoryRemoved(alert);
			}
		}
	}
	
	public interface AlertHistoryListener {
		public void alertHistoryAdded(LogAlert alert);
		public void alertHistoryRemoved(LogAlert alert);
	}
}
