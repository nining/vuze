/*
 * Created on 16-Jan-2006
 * Created by Paul Gardner
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */

package org.gudy.azureus2.ui.swt.config.generic;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * SWT widgets representing an Int Parameter, backed by a {@link GenericParameterAdapter}
 */
public class GenericIntParameter
{
	private static boolean DEBUG = false;

	private GenericParameterAdapter adapter;

	private int iMinValue = Integer.MIN_VALUE;

	private int iMaxValue = Integer.MAX_VALUE;

	private int iDefaultValue;

	private String sParamName;

	private boolean bGenerateIntermediateEvents = false;

	// OSX doesn't send selection events while typing, so we need to trigger save
	// on focus out
	private boolean bTriggerOnFocusOut = Utils.isCarbon;

	private Spinner spinner;

	private TimerEvent timedSaveEvent = null;

	private TimerEventPerformer timerEventSave;

	private final boolean delayIntialSet = Utils.isCarbon && System.getProperty("os.version", "").startsWith("10.6");

	public GenericIntParameter(GenericParameterAdapter adapter,
			Composite composite, final String name) {
		iDefaultValue = adapter.getIntValue(name);
		initialize(adapter, composite, name);
	}

	/** @deprecated */
	public GenericIntParameter(GenericParameterAdapter adapter,
			Composite composite, final String name, int defaultValue) {
		iDefaultValue = defaultValue;
		initialize(adapter, composite, name);
	}

	public GenericIntParameter(GenericParameterAdapter adapter,
			Composite composite, String name, int minValue, int maxValue) {
		iDefaultValue = adapter.getIntValue(name);
		
		if ( maxValue < minValue ){
			Debug.out( "max < min, not good" );
				// common mistake to use -1 to indicate no-limit

			maxValue = Integer.MAX_VALUE;
		}
		
		iMinValue = minValue;
		iMaxValue = maxValue;
		initialize(adapter, composite, name);
	}

	public void initialize(GenericParameterAdapter _adapter, Composite composite,
			String name) {
		adapter = _adapter;
		sParamName = name;

		timerEventSave = new TimerEventPerformer() {
			public void perform(TimerEvent event) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (spinner.isDisposed()) {
							return;
						}
						if (DEBUG) {
							debug("setIntValue to " + spinner.getSelection()
									+ " via timeEventSave");
						}
						adapter.setIntValue(sParamName, spinner.getSelection());
					}
				});
			}
		};

		int value = adapter.getIntValue(name, iDefaultValue);

		spinner = new Spinner(composite, SWT.BORDER);
		setMinimumValue(iMinValue);
		setMaximumValue(iMaxValue);
		spinner.setSelection(value);
		
		if (delayIntialSet) {
  		Utils.execSWTThreadLater(0, new AERunnable() {
  			public void runSupport() {
  				spinner.setSelection(adapter.getIntValue(sParamName, iDefaultValue));
  			}
  		});
		}

		spinner.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (bGenerateIntermediateEvents || !spinner.isFocusControl()) {
					adapter.setIntValue(sParamName, spinner.getSelection());
				} else {
					bTriggerOnFocusOut = true;
					cancelTimedSaveEvent();

					if (DEBUG) {
						debug("create timeSaveEvent (" + spinner.getSelection() + ") ");
					}
					timedSaveEvent = SimpleTimer.addEvent("IntParam Saver",
							SystemTime.getOffsetTime(750), timerEventSave);
				}
			}
		});

		/*
		 * Primarily for OSX, since it doesn't validate or trigger selection
		 * while typing numbers.
		 * 
		 * Force into next tab, which will result in a selection
		 */
		spinner.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event event) {
				if (spinner.isFocusControl()) {
					if (DEBUG) {
						debug("next");
					}
					spinner.traverse(SWT.TRAVERSE_TAB_NEXT);
				}
			}
		});

		spinner.addListener(SWT.FocusOut, new Listener() {
			public void handleEvent(Event event) {
				if (bTriggerOnFocusOut) {
					if (DEBUG) {
						debug("focus out setIntValue(" + spinner.getSelection()
								+ "/trigger");
					}
					cancelTimedSaveEvent();
					adapter.setIntValue(sParamName, spinner.getSelection());
				}
			}
		});
	}

	private void cancelTimedSaveEvent() {
		if (timedSaveEvent != null
				&& (!timedSaveEvent.hasRun() || !timedSaveEvent.isCancelled())) {
			if (DEBUG) {
				debug("cancel timeSaveEvent");
			}
			timedSaveEvent.cancel();
		}
	}

	/**
	 * @param string
	 */
	private void debug(String string) {
		System.out.println("[GenericIntParameter:" + sParamName + "] " + string);
	}

	public void setMinimumValue(final int value) {
		iMinValue = value;
		if (iMinValue != Integer.MIN_VALUE && getValue() < iMinValue) {
			setValue(iMinValue);
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				spinner.setMinimum(value);
			}
		});
	}

	public void setMaximumValue(final int value) {
		iMaxValue = value;
		if (iMaxValue != Integer.MAX_VALUE && getValue() > iMaxValue) {
			setValue(iMaxValue);
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				spinner.setMaximum(value);
			}
		});
	}

	public String getName() {
		return (sParamName);
	}

	public void setValue(int value) {
		int newValue;
		if (iMaxValue != Integer.MAX_VALUE && value > iMaxValue) {
			newValue = iMaxValue;
		} else if (iMinValue != Integer.MIN_VALUE && value < iMinValue) {
			newValue = iMinValue;
		} else {
			newValue = value;
		}
		
		final int finalNewValue = newValue;
		
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (!spinner.isDisposed()) {
					if (spinner.getSelection() != finalNewValue) {
						if (DEBUG) {
							debug("spinner.setSelection(" + finalNewValue + ")");
						}
						spinner.setSelection(finalNewValue);
					}
					if (DEBUG) {
						debug("setIntValue to " + spinner.getSelection()
								+ " via setValue(int)");
					}
				}
			}
		});
		if (finalNewValue != getValue()) {
			adapter.setIntValue(sParamName, finalNewValue);
		}
	}

	public void setValue(final int value, boolean force_adapter_set) {
		if (force_adapter_set) {
			setValue(value);
		} else {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (spinner.getSelection() != value) {
						spinner.setSelection(value);
					}
				}
			});
		}
	}

	public int getValue() {
		return (adapter.getIntValue(sParamName, iDefaultValue));
	}

	public void resetToDefault() {
		if (adapter.resetIntDefault(sParamName)) {
			setValue(adapter.getIntValue(sParamName));
		} else {
			setValue(getValue());
		}
	}

	public void setLayoutData(Object layoutData) {
		spinner.setLayoutData(layoutData);
	}

	public Control getControl() {
		return spinner;
	}

	public boolean isGeneratingIntermediateEvents() {
		return bGenerateIntermediateEvents;
	}

	public void setGenerateIntermediateEvents(boolean generateIntermediateEvents) {
		bGenerateIntermediateEvents = generateIntermediateEvents;
	}
}