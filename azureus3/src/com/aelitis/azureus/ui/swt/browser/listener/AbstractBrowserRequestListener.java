package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.HashMap;
import java.util.Map;

import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.util.MapUtils;

/**
 * This is a convenience abstract base class for listeners to browser requests;
 * it will automatically parse the <code>BrowserMessage</code> and call the appropriate methods.
 * <p>
 * This implementation also caches the parameter values and exposes them through accessor methods
 * for convenience so subclasses do not have to parser and store them locally.
 * <p>
 * Subclasses must provide the implementation for {@link IBrowserRequestListener#handleOpenURL()},
 * and may override other handlers as needed. 
 * @author knguyen
 *
 */
public abstract class AbstractBrowserRequestListener
	extends AbstractMessageListener
	implements IBrowserRequestListener
{

	protected Map decodedMap = new HashMap(0);

	private String url = null;

	private String prefixVerifier = null;

	private int width = -1;

	private int height = -1;

	private boolean isMovable = false;

	private boolean isResizable = false;

	private String statusMessage = null;

	public AbstractBrowserRequestListener(String listenerID) {
		super(listenerID);
	}

	public void handleMessage(BrowserMessage message) {

		String opID = message.getOperationId();
		decodedMap = message.getDecodedMap();
		if (true == OP_OPEN_URL.equals(opID)) {
			handleOpenURL();
		} else if (true == OP_CLOSE.equals(opID)) {
			handleClose();
		} else if (true == OP_REFRESH.equals(opID)) {
			handleRefresh();
		} else if (true == OP_RESIZE.equals(opID)) {
			handleResize();
		}

	}

	public int getHeight() {
		if (true == decodedMap.containsKey(OP_OPEN_URL_PARAM_HEIGHT)) {
			height = MapUtils.getMapInt(decodedMap, OP_OPEN_URL_PARAM_HEIGHT, -1);
		}

		return height;
	}

	public String getPrefixVerifier() {
		if (true == decodedMap.containsKey(OP_OPEN_URL_PARAM_TITLE_PREFIX_VERIFIER)) {
			prefixVerifier = MapUtils.getMapString(decodedMap,
					OP_OPEN_URL_PARAM_TITLE_PREFIX_VERIFIER, "");
		}

		return prefixVerifier;
	}

	public String getURL() {
		if (true == decodedMap.containsKey(OP_OPEN_URL_PARAM_URL)) {
			url = MapUtils.getMapString(decodedMap, OP_OPEN_URL_PARAM_URL, "");
		}

		return url;
	}

	public int getWidth() {
		if (true == decodedMap.containsKey(OP_OPEN_URL_PARAM_WIDTH)) {
			width = MapUtils.getMapInt(decodedMap, OP_OPEN_URL_PARAM_WIDTH, -1);
		}

		return width;
	}

	public boolean isMovable() {
		if (true == decodedMap.containsKey(OP_OPEN_URL_PARAM_MOVABLE)) {
			isMovable = MapUtils.getMapBoolean(decodedMap, OP_OPEN_URL_PARAM_MOVABLE,
					false);
		}
		return isMovable;
	}

	public boolean isResizable() {
		if (true == decodedMap.containsKey(OP_OPEN_URL_PARAM_RESIZABLE)) {
			isResizable = MapUtils.getMapBoolean(decodedMap,
					OP_OPEN_URL_PARAM_RESIZABLE, false);
		}
		return isResizable;
	}

	public String getStatusMessage() {
		if (true == decodedMap.containsKey(OP_CLOSE_PARAM_STATUS)) {
			statusMessage = MapUtils.getMapString(decodedMap, OP_CLOSE_PARAM_STATUS,
					null);
		}
		return statusMessage;
	}

	public void handleClose() {
		// Do nothing by default; subclass may override
	}

	public void handleRefresh() {
		// Do nothing by default; subclass may override
	}

	public void handleResize() {
		// Do nothing by default; subclass may override
	}

}
