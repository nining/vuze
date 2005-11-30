/**
 * 
 */
package org.gudy.azureus2.core3.logging;

import org.gudy.azureus2.core3.logging.impl.FileLogging;
import org.gudy.azureus2.core3.logging.impl.LoggerImpl;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;

/**
 * A static implementation of the LoggerImpl class.
 * 
 * @note Currently, LoggerImpl and Logger could be combined, but they are split
 *        for future consideration (ie. allowing multiple LoggerImpl) 
 * 
 * @author TuxPaper
 * @since 2.3.0.7
 * 
 * XXX This class is currently routes to LGLogger until switchover is complete!
 */
public class Logger {
	private static final LogIDs LOGID = LogIDs.LOGGER;

	// Temporary until switch
	private static boolean bUseOldLogger = true;
	
	private static LoggerImpl loggerImpl = null;

	private static FileLogging fileLogging = new FileLogging();

	static {
		if (!bUseOldLogger)
		try {
			loggerImpl = new LoggerImpl();
			loggerImpl.init();

			fileLogging.initialize();

			log(new LogEvent(LOGID, "**** Logging starts: "
					+ Constants.AZUREUS_VERSION + " ****"));

			log(new LogEvent(LOGID, "java.home=" + System.getProperty("java.home")));

			log(new LogEvent(LOGID, "java.version="
					+ System.getProperty("java.version")));

			log(new LogEvent(LOGID, "os=" + System.getProperty("os.arch") + "/"
					+ System.getProperty("os.name") + "/"
					+ System.getProperty("os.version")));

			log(new LogEvent(LOGID, "user.dir=" + System.getProperty("user.dir")));

			log(new LogEvent(LOGID, "user.home=" + System.getProperty("user.home")));
		} catch (Throwable t) {
			Debug.out("Error initializing Logger", t);
			// loggerImpl will always be set, except for cases where there wasn't
			// enough memory. In that case, app will blork with null pointer exception
			// on first Logger.* call.  However, since there's not enough memory,
			// application will probably blork somewhere else in the code first. 
		}
	}

	/**
	 * Determines whether events are logged
	 * 
	 * @return true if events are logged
	 */
	public static boolean isEnabled() {
		if (bUseOldLogger)
			return LGLogger.isEnabled();
		return loggerImpl.isEnabled();
	}

	/**
	 * Log an event
	 * 
	 * @param event
	 *            event to log
	 */
	public static void log(LogEvent event) {
		if (bUseOldLogger) {
			StringBuffer text = new StringBuffer("{" + event.logID + "} ");
			
			boolean needLF = false;

			if (event.relatedTo != null) {
				for (int i = 0; i < event.relatedTo.length; i++) {
					Object obj = event.relatedTo[i];
					
					if (obj == null)
						continue;

					needLF = true;
					if (i > 0)
						text.append("; ");

					if (obj instanceof LogRelation) {
						text.append(((LogRelation)obj).getRelationText());
					} else {
						text.append("RelatedTo[" + obj.toString() + "]");
					}
				}
			}

			if (needLF) {
				text.append("\r\n");
				
				int len = 16;
				char[] padding = new char[len];
				while (len > 0)
					padding[--len] = ' ';
				text.append(padding);
			}

			text.append(event.text);
			
			int componentID = 0;
			if (event.logID == LogIDs.TRACKER)
				componentID = 2;
			else if (event.logID == LogIDs.PEER)
				componentID = 1;

			if (event.err == null)
				LGLogger.log(componentID, 0, event.entryType, text.toString());
			else
				LGLogger.log(componentID, 0, text.toString(), event.err);
			return;
		}
		loggerImpl.log(event);
	}

	public static void log(LogAlert alert) {
		if (bUseOldLogger) {
			if (alert.err == null)
				if (alert.repeatable)
					LGLogger.logRepeatableAlert(alert.entryType, alert.text);
				else
					LGLogger.logUnrepeatableAlert(alert.entryType, alert.text);
			else
				if (alert.repeatable)
					LGLogger.logRepeatableAlert(alert.text, alert.err);
				else
					LGLogger.logUnrepeatableAlert(alert.text, alert.err);
			return;
		}
		loggerImpl.log(alert);
	}

	/**
	 * Log an event, loading text from out messagebundle. Fill event.text with
	 * resource id.
	 * 
	 * @param event
	 *            event to log
	 */
	public static void logTextResource(LogEvent event) {
		if (bUseOldLogger)
			return;
		loggerImpl.logTextResource(event);
	}

	public static void logTextResource(LogEvent event, String params[]) {
		if (bUseOldLogger)
			return;
		loggerImpl.logTextResource(event, params);
	}

	public static void logTextResource(LogAlert alert) {
		if (bUseOldLogger) {
			LGLogger.logUnrepeatableAlertUsingResource(alert.entryType, alert.text);
			return;
		}
		loggerImpl.logTextResource(alert);
	}

	public static void logTextResource(LogAlert alert, String params[]) {
		if (bUseOldLogger) {
			LGLogger.logUnrepeatableAlertUsingResource(alert.entryType, alert.text, params);
			return;
		}
		loggerImpl.logTextResource(alert, params);
	}

	/**
	 * Redirect stdout and stderr to Logger.
	 */
	public static void doRedirects() {
		if (bUseOldLogger)
			return;
		loggerImpl.doRedirects();
	}

	/**
	 * Add a listener that's triggered when an event is logged.
	 * 
	 * @param aListener
	 *            Listener to call when an event is logged
	 */
	public static void addListener(ILoggerListener aListener) {
		if (bUseOldLogger)
			return;
		loggerImpl.addListener(aListener);
	}

	/**
	 * Remove a previously added log listener
	 * 
	 * @param aListener
	 *            Listener to remove
	 */
	public static void removeListener(ILoggerListener aListener) {
		if (bUseOldLogger)
			return;
		loggerImpl.removeListener(aListener);
	}

	/**
	 * Add a listener that's triggered when an event is logged.
	 * 
	 * @param aListener
	 *            Listener to call when an event is logged
	 */
	public static void addListener(ILogEventListener aListener) {
		if (bUseOldLogger)
			return;
		loggerImpl.addListener(aListener);
	}

	/**
	 * Add a listener that's triggered when an alert is logged.
	 * 
	 * @param aListener
	 *            Listener to call when an alert is logged
	 */
	public static void addListener(LGAlertListener aListener) {
		if (bUseOldLogger) {
			LGLogger.addAlertListener(aListener);
			return;
		}
		loggerImpl.addListener(aListener);
	}

	/**
	 * Add a listener that's triggered when an alert is logged.
	 * 
	 * @param aListener
	 *            Listener to call when an alert is logged
	 */
	public static void addListener(ILogAlertListener aListener) {
		if (bUseOldLogger)
			return;
		loggerImpl.addListener(aListener);
	}

	/**
	 * Remove a previously added log listener
	 * 
	 * @param aListener
	 *            Listener to remove
	 */
	public static void removeListener(ILogEventListener aListener) {
		if (bUseOldLogger)
			return;
		loggerImpl.removeListener(aListener);
	}

	/**
	 * Remove a previously added log listener
	 * 
	 * @param aListener
	 *            Listener to remove
	 */
	public static void removeListener(LGAlertListener aListener) {
		if (bUseOldLogger) {
			LGLogger.removeAlertListener(aListener);
			return;
		}
		loggerImpl.removeListener(aListener);
	}

	/**
	 * Remove a previously added log listener
	 * 
	 * @param aListener
	 *            Listener to remove
	 */
	public static void removeListener(ILogAlertListener aListener) {
		if (bUseOldLogger)
			return;
		loggerImpl.removeListener(aListener);
	}

}
