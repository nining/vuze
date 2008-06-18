package com.aelitis.azureus.ui.swt.views.skin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.widgets.PaginationWidget;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.VuzeBuddyListener;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;
import com.aelitis.azureus.ui.swt.layout.SimpleReorderableListLayout;
import com.aelitis.azureus.ui.swt.layout.SimpleReorderableListLayoutData;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.FAQTopics;

public class BuddiesViewer
	extends SkinView
{

	private static final boolean SHOW_ONLINE_STATUS = System.getProperty(
			"az.buddy.show_online", "1").equals("1");

	public static final int none_active_mode = 0;

	public static final int edit_mode = 1;

	public static final int share_mode = 2;

	public static final int invite_mode = 3;

	public static final int add_buddy_mode = 4;

	public static final int disabled_mode = 5;

	private Composite content = null;

	private Composite avatarsPanel = null;

	private Composite parent = null;

	private SWTSkin skin = null;

	private int avatarHightLightBorder;

	private int avatarImageBorder;

	private Point avatarImageSize = null;

	private Point avatarNameSize = null;

	private Point avatarSize = null;

	private int hSpacing;

	private int avatarWidthPlusSpacing;

	private List avatarWidgets = new ArrayList();

	private boolean isShareMode = false;

	private boolean isEditMode = false;

	private boolean isAddBuddyMode = false;

	private boolean isEnabled = true;

	private Color textColor = null;

	private Color textLinkColor = null;

	private Color imageBorderColor = null;

	private Color selectedColor = null;

	private Color highlightedColor = null;

	private SWTSkinObject soNoBuddies;

	private int currentPage = 0;

	private int pageCount = 1;

	/**
	 * The width of the visible window for the avatarPanel
	 */
	private int pageWindowWidth = 0;

	private DetailPanel detailPanel;

	private SharePage sharePage;

	private PaginationWidget pWidget;

	private int[] pageXOffsets = null;

	private int avatarsPerPage = 1;

	private List buddiesList;

	private SWTSkinObject leftScroll;

	private SWTSkinObject rightScroll;

	public BuddiesViewer() {
	}

	public Object showSupport(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();

		soNoBuddies = skin.getSkinObject("buddies-viewer-nobuddies-panel");

		SWTSkinObject viewer = skin.getSkinObject(SkinConstants.VIEWID_BUDDIES_VIEWER);

		if (null != viewer) {

			SkinView detailPanelView = SkinViewManager.get(DetailPanel.class);
			if (detailPanelView instanceof DetailPanel) {
				detailPanel = ((DetailPanel) detailPanelView);
				sharePage = (SharePage) detailPanel.getPage(SharePage.PAGE_ID);

			} else {
				throw new IllegalArgumentException(
						"Oops.. looks like the DetailPanel skin is not properly initialized");
			}

			parent = (Composite) viewer.getControl();

			content = new Composite(parent, SWT.NONE);
			FormData fd = new FormData();
			fd.top = new FormAttachment(0, 0);
			fd.bottom = new FormAttachment(100, 0);
			fd.left = new FormAttachment(0, 0);
			fd.right = new FormAttachment(100, 0);
			content.setLayoutData(fd);

			avatarsPanel = new Composite(content, SWT.NONE);
			avatarsPanel.setLocation(0, 0);

			/*
			 * Specify avatar dimensions and attributes before creating the avatars
			 */
			textColor = skin.getSkinProperties().getColor("color.links.normal");
			textLinkColor = skin.getSkinProperties().getColor("color.links.hover");
			imageBorderColor = ColorCache.getColor(avatarsPanel.getDisplay(), 55, 55,
					55);

			selectedColor = ColorCache.getColor(avatarsPanel.getDisplay(), 16, 16, 16);

			highlightedColor = ColorCache.getColor(avatarsPanel.getDisplay(), 45, 45,
					45);

			avatarHightLightBorder = 0;
			avatarImageBorder = 1;
			hSpacing = 1;
			avatarImageSize = new Point(40, 40);
			avatarNameSize = new Point(60, 16);
			avatarSize = new Point(0, 0);
			avatarSize.x = Math.max(avatarNameSize.x, avatarImageSize.x)
					+ (2 * (avatarHightLightBorder + avatarImageBorder));
			avatarSize.y = avatarNameSize.y + avatarImageSize.y
					+ (2 * (avatarHightLightBorder + avatarImageBorder) + 6);
			avatarWidthPlusSpacing = hSpacing + avatarSize.x;

			fillBuddies(avatarsPanel);

			/* UNCOMMENT THIS SECTION TO REVERT TO A ROW LAYOUT
			RowLayout rLayout = new RowLayout(SWT.HORIZONTAL);
			rLayout.wrap = false;
			rLayout.spacing = hSpacing;
			avatarsPanel.setLayout(rLayout);*/

			// COMMENT THIS SECTION TO REVERT TO A ROW LAYOUT
			SimpleReorderableListLayout rLayout = new SimpleReorderableListLayout();
			rLayout.margin = hSpacing;
			avatarsPanel.setLayout(rLayout);

			avatarsPanel.pack();

			avatarsPanel.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					select(null, false, false);
				}
			});

			content.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					select(null, false, false);
				}
			});

			parent.layout(true);
			calculatePagination();
			hookPaginationWidget();
			hookScrollers();
			hookFAQLink();
		}

		return null;

	}

	private void hookPaginationWidget() {
		final SWTSkinObject paginationObject = skin.getSkinObject("panel-navigation-thumbnails");
		if (null != paginationObject) {
			Composite control = (Composite) paginationObject.getControl();
			pWidget = new PaginationWidget(control);
			pWidget.addPageSelectionListener(new PaginationWidget.PageSelectionListener() {
				public void pageSelected(int pageNumber) {
					setCurrentPage(pageNumber);
				}
			});
		}
	}

	private void showPage(final int pageNumber, boolean animateTransition) {

		if (avatarsPanel.getLocation().x == -pageXOffsets[pageNumber]) {
			//Do nothing if page is still the same
			return;
		}

		if (false == animateTransition) {
			avatarsPanel.setLocation(-pageXOffsets[pageNumber],
					avatarsPanel.getLocation().y);
		} else {
			//No animation yet
		}

		enableScroll();
	}

	private void calculatePagination() {

		Utils.execSWTThreadLater(0, new AERunnable() {

			public void runSupport() {
				pageWindowWidth = content.getClientArea().width;

				int totalAvatars = avatarWidgets.size();

				/*
				 * Avoid divide by zero when the viewer is collapsed
				 */
				if (pageWindowWidth < 1) {
					pageCount = 1;

					/*
					 * Single page offset
					 */
					pageXOffsets = new int[] {
						0
					};
				} else {
					avatarsPerPage = 0;
					/*
					 * If windowWidth can only fully show 1 avatar then the number of pages
					 * would equal the number of avatars
					 */
					if (pageWindowWidth <= avatarWidthPlusSpacing) {
						pageCount = avatarWidgets.size();
						avatarsPerPage = 1;
					} else {

						avatarsPerPage = (pageWindowWidth / avatarWidthPlusSpacing);
						pageCount = Math.max(1, avatarsPanel.getClientArea().width
								/ (avatarsPerPage * avatarWidthPlusSpacing));

						if (avatarsPerPage < totalAvatars
								&& (totalAvatars % avatarsPerPage) != 0) {
							pageCount++;
						}
					}

					/*
					 * Create the new offset array which is used for pagination
					 */
					pageXOffsets = new int[pageCount];

					int xOffset = 0;
					/*
					 * First page has no offset
					 */
					pageXOffsets[0] = 0;

					for (int i = 1; i < pageXOffsets.length; i++) {
						xOffset += (avatarsPerPage * avatarWidthPlusSpacing);
						pageXOffsets[i] = xOffset;
					}
				}
				if (null != pWidget) {
					pWidget.setPageCount(pageCount);
					pWidget.setItemsPerPage(avatarsPerPage);
					pWidget.setItemsTotal(totalAvatars);
				}
			}
		});

	}

	private void hookScrollers() {
		leftScroll = skin.getSkinObject("buddies-left-scroller");
		if (null != leftScroll) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(leftScroll);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (getCurrentPage() > 0) {
						setCurrentPage(getCurrentPage() - 1);
					}
				}
			});
		}

		rightScroll = skin.getSkinObject("buddies-right-scroller");
		if (null != rightScroll) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(rightScroll);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (getCurrentPage() < getPageCount() - 1) {
						setCurrentPage(getCurrentPage() + 1);
					}
				}
			});
		}

		enableScroll();
		parent.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				calculatePagination();
				enableScroll();
			}
		});

	}

	private void enableScroll() {
		if (null == leftScroll || null == rightScroll) {
			return;
		}
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				if (avatarsPanel.getSize().x > parent.getSize().x
						|| avatarsPanel.getLocation().x < 0) {
					leftScroll.setVisible(true);
					if (currentPage != 0) {
						leftScroll.switchSuffix("");
					} else {
						leftScroll.switchSuffix("-disabled");
					}
					rightScroll.setVisible(true);
					if (currentPage != pageCount - 1) {
						rightScroll.switchSuffix("");
					} else {
						rightScroll.switchSuffix("-disabled");
					}
				} else {
					if (true == leftScroll.isVisible()) {
						leftScroll.setVisible(false);
					}
					if (true == rightScroll.isVisible()) {
						rightScroll.setVisible(false);
					}
				}
			}
		});
	}

	public boolean isEditMode() {
		return isEditMode;
	}

	public void setEditMode(boolean value) {
		if (isEditMode != value) {
			isEditMode = value;
			for (Iterator iterator = avatarWidgets.iterator(); iterator.hasNext();) {
				AvatarWidget widget = (AvatarWidget) iterator.next();
				widget.refreshVisual();
			}

			if (true == value) {
				setShareMode(false);
				setAddBuddyMode(false);
			}
		}
	}

	private void fillBuddies(Composite composite) {

		List buddies = getBuddies();

		showNoBuddiesPanel(buddies.size() == 0);

		for (Iterator iterator = buddies.iterator(); iterator.hasNext();) {
			VuzeBuddySWT vuzeBuddy = (VuzeBuddySWT) iterator.next();
			createBuddyControls(composite, vuzeBuddy);
		}
		composite.layout();
		Point size = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		composite.setSize(size);

	}

	private void showNoBuddiesPanel(boolean value) {
		if (soNoBuddies != null && soNoBuddies.isVisible() != value) {
			soNoBuddies.setVisible(value);
		}
	}

	private AvatarWidget createBuddyControls(Composite composite,
			final VuzeBuddySWT vuzeBuddy) {
		AvatarWidget avatarWidget = new AvatarWidget(this, avatarSize,
				avatarImageSize, avatarNameSize, vuzeBuddy);
		avatarWidget.setBorderWidth(avatarHightLightBorder);
		avatarWidget.setTextColor(textColor);
		avatarWidget.setTextLinkColor(textLinkColor);
		avatarWidget.setImageBorderColor(imageBorderColor);
		avatarWidget.setImageBorder(avatarImageBorder);
		avatarWidget.setSelectedColor(selectedColor);
		avatarWidget.setHighlightedColor(highlightedColor);

		/* UNCOMMENT THIS SECTION TO REVERT TO A ROW LAYOUT
		RowData rData = new RowData();
		rData.width = avatarSize.x;
		rData.height = avatarSize.y;
		avatarWidget.getControl().setLayoutData(rData);*/

		// COMMENT THIS SECTION TO REVERT TO A ROW LAYOUT
		SimpleReorderableListLayoutData rData = new SimpleReorderableListLayoutData();
		rData.width = avatarSize.x;
		rData.height = avatarSize.y;
		rData.position = (int) VuzeBuddyManager.getBuddyPosition(vuzeBuddy);
		avatarWidget.getControl().setLayoutData(rData);

		avatarWidgets.add(avatarWidget);

		return avatarWidget;
	}

	/**
	 * Returns whether the given <code>AvatarWidget</code> is fully visible in the view port of the viewer
	 */
	public boolean isFullyVisible(AvatarWidget avatarWidget) {
		if (null != avatarWidget && null != avatarWidget.getControl()
				&& false == avatarWidget.getControl().isDisposed()) {

			Rectangle controlBounds = avatarWidget.getControl().getBounds();
			if (controlBounds.x + controlBounds.width < content.getBounds().width
					- avatarsPanel.getBounds().x) {
				return true;
			}
		}
		return false;
	}

	public void removeBuddy(final AvatarWidget widget) {
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				avatarWidgets.remove(widget);
				widget.dispose(true, new AvatarWidget.AfterDisposeListener() {
					public void disposed() {
						avatarsPanel.setSize(avatarsPanel.computeSize(SWT.DEFAULT,
								SWT.DEFAULT, true));
						if (avatarWidgets.size() < 1) {
							showNoBuddiesPanel(true);
						}
					}
				});

			}
		});
	}

	public void removeBuddy(VuzeBuddy buddy) {
		AvatarWidget widget = findWidget(buddy);
		if (null != widget) {
			removeBuddy(widget);
		} else {
			Debug.out("Unknown VuzeBuddy; can not remove from viewer since we don't have it.");
		}
	}

	public void updateBuddy(final VuzeBuddy buddy) {
		if (buddy instanceof VuzeBuddySWT) {

			Utils.execSWTThreadLater(0, new AERunnable() {

				public void runSupport() {
					AvatarWidget widget = findWidget(buddy);
					if (null != widget) {
						widget.setVuzeBuddy((VuzeBuddySWT) buddy);
					} else {
						/*
						 * If not found yet then we create the avatar for it; this really should not happen
						 * but we'll handle it just in case
						 */
						addBuddy(buddy);
					}
				}
			});

		}
	}

	public void addBuddy(final VuzeBuddy buddy) {
		if (buddy instanceof VuzeBuddySWT) {
			Utils.execSWTThreadLater(0, new AERunnable() {
				public void runSupport() {
					AvatarWidget widget = findWidget(buddy);
					if (widget == null) {
						if (soNoBuddies != null) {
							soNoBuddies.setVisible(false);
						}
						createBuddyControls(avatarsPanel, (VuzeBuddySWT) buddy);
						avatarsPanel.layout();
						Point size = avatarsPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT,
								true);
						avatarsPanel.setSize(size);
					}
				}
			});

		} else {
			Debug.out("Wrong type VuzeBuddy... must be of type VuzeBuddySWT");
		}
	}

	private AvatarWidget findWidget(VuzeBuddy buddy) {
		if (null != buddy) {
			for (Iterator iterator = avatarWidgets.iterator(); iterator.hasNext();) {
				AvatarWidget widget = (AvatarWidget) iterator.next();
				if (null != widget.getVuzeBuddy()) {
					if (true == buddy.getLoginID().equals(
							widget.getVuzeBuddy().getLoginID())) {
						return widget;
					}
				}
			}
		}

		return null;
	}

	/**
	 * Return a list of <code>VuzeBuddySWT</code> that are currently selected
	 * @return
	 */
	public List getSelection() {
		List selected = new ArrayList();
		for (Iterator iterator = avatarWidgets.iterator(); iterator.hasNext();) {
			AvatarWidget widget = (AvatarWidget) iterator.next();
			if (true == widget.isSelected()) {
				selected.add(widget.getVuzeBuddy());
			}
		}
		return selected;
	}

	public void select(VuzeBuddySWT buddy, boolean value, boolean appendSelection) {

		if (null != buddy) {
			for (Iterator iterator = avatarWidgets.iterator(); iterator.hasNext();) {
				AvatarWidget widget = (AvatarWidget) iterator.next();
				if (true == buddy.equals(widget.getVuzeBuddy())) {
					widget.setSelected(value);
					if (true == appendSelection) {
						break;
					}
				} else if (false == appendSelection) {
					if (true == value) {
						if (widget.isSelected() != false) {
							widget.setSelected(false);
							widget.refreshVisual();
						}
					} else {
						widget.setSelected(false);
						widget.refreshVisual();
					}
				}
			}
		}
		/*
		 * De-select all buddies if the given 'buddy' is null
		 */
		else {
			for (Iterator iterator = avatarWidgets.iterator(); iterator.hasNext();) {
				AvatarWidget widget = (AvatarWidget) iterator.next();
				if (true == widget.isSelected()) {
					widget.setSelected(false);
					widget.refreshVisual();
				}
			}
		}
	}

	private void recomputeOrder() {

		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {

				/* UNCOMMENT THIS SECTION TO REVERT TO A ROW LAYOUT
				return;
				*/

				// COMMENT THIS SECTION TO REVERT TO A ROW LAYOUT
				if (avatarsPanel.isDisposed())
					return;

				final List buddies = VuzeBuddyManager.getAllVuzeBuddies();

				//Only sort by online status if we show it
				if (SHOW_ONLINE_STATUS) {
					Collections.sort(buddies, new Comparator() {
						public int compare(Object o1, Object o2) {
							VuzeBuddy v1 = (VuzeBuddy) o1;
							VuzeBuddy v2 = (VuzeBuddy) o2;
							int score = 0;
							score -= v1.isOnline() ? 1 : 0;
							score += v2.isOnline() ? 1 : 0;
							return score;
						}
					});
				}

				boolean changed = false;
				for (int i = 0; i < buddies.size(); i++) {
					VuzeBuddy buddy = (VuzeBuddy) buddies.get(i);
					AvatarWidget widget = findWidget(buddy);
					if (widget != null) {
						Control control = widget.getControl();
						if (control != null && !control.isDisposed()) {
							SimpleReorderableListLayoutData rData = (SimpleReorderableListLayoutData) widget.getControl().getLayoutData();
							if (rData.position != i) {
								rData.position = i;
								changed = true;
							}
						}
					}
				}
				if (changed) {
					avatarsPanel.layout();
				}
			}
		});
	}

	private List getBuddies() {

		/*
		 * Add the listener only once at the beginning
		 */
		if (null == buddiesList) {
			VuzeBuddyManager.addListener(new VuzeBuddyListener() {

				public void buddyRemoved(VuzeBuddy buddy) {
					removeBuddy(buddy);
					recomputeOrder();
				}

				public void buddyChanged(VuzeBuddy buddy) {
					updateBuddy(buddy);
					recomputeOrder();
				}

				public void buddyAdded(VuzeBuddy buddy, int position) {
					addBuddy(buddy);
					recomputeOrder();
				}

				public void buddyOrderChanged() {

				}
			}, false);
		}

		buddiesList = VuzeBuddyManager.getAllVuzeBuddies();
		return buddiesList;
	}

	public Composite getControl() {
		return avatarsPanel;
	}

	public boolean isShareMode() {
		return isShareMode;
	}

	public void addAllToShare() {
		addToShare(avatarWidgets);
	}

	public void removeAllFromShare() {
		removeFromShare(avatarWidgets);
	}

	public void addToShare(List avatars) {

		for (Iterator iterator = avatars.iterator(); iterator.hasNext();) {
			Object object = (Object) iterator.next();
			if (object instanceof AvatarWidget) {
				addToShare((AvatarWidget) object);
			}
		}
	}

	public void addToShare(AvatarWidget widget) {
		sharePage.addBuddy(widget.getVuzeBuddy());
		widget.setSharedAlready(true);
	}

	public void removeFromShare(List avatars) {

		for (Iterator iterator = avatars.iterator(); iterator.hasNext();) {
			Object object = (Object) iterator.next();
			if (object instanceof AvatarWidget) {
				removeFromShare((AvatarWidget) object);
			}
		}
	}

	public void removeFromShare(AvatarWidget widget) {
		sharePage.removeBuddy(widget.getVuzeBuddy());
		widget.setSharedAlready(false);
	}

	public void addToShare(VuzeBuddy buddy) {
		AvatarWidget widget = findWidget(buddy);
		if (null != widget) {
			if (false == widget.isSharedAlready()) {
				addToShare(widget);
			}
		}
	}

	public void addSelectionToShare() {

		for (Iterator iterator = avatarWidgets.iterator(); iterator.hasNext();) {
			AvatarWidget widget = (AvatarWidget) iterator.next();
			if (true == widget.isSelected()) {
				addToShare(widget);
			}
		}
	}

	public void removeFromShare(VuzeBuddy buddy) {

		if (null != buddy) {
			for (Iterator iterator = avatarWidgets.iterator(); iterator.hasNext();) {
				AvatarWidget widget = (AvatarWidget) iterator.next();
				if (null != widget.getVuzeBuddy()) {
					if (true == buddy.getLoginID().equals(
							widget.getVuzeBuddy().getLoginID())) {
						sharePage.removeBuddy(widget.getVuzeBuddy());
						widget.setSharedAlready(false);
						break;
					}
				}
			}
		}

	}

	public void setShareMode(boolean isShareMode) {

		if (this.isShareMode != isShareMode) {
			this.isShareMode = isShareMode;
			for (Iterator iterator = avatarWidgets.iterator(); iterator.hasNext();) {
				AvatarWidget widget = (AvatarWidget) iterator.next();
				if (false == isShareMode) {
					widget.setSharedAlready(false);
				}
				widget.refreshVisual();
			}

			if (true == isShareMode) {
				setEditMode(false);
				setAddBuddyMode(false);
			}
		}

	}

	public boolean isAddBuddyMode() {
		return isAddBuddyMode;
	}

	public void setAddBuddyMode(boolean isAddBuddyMode) {
		this.isAddBuddyMode = isAddBuddyMode;
		/*
		 * Turn off share mode when we enter add buddy flow
		 */
		if (true == isAddBuddyMode) {
			setShareMode(false);
			setEditMode(false);
		}
	}

	public void setMode(int mode) {
		if (mode == none_active_mode) {
			setShareMode(false);
			setEditMode(false);
			setAddBuddyMode(false);
		} else if (mode == edit_mode) {
			setEditMode(true);
		} else if (mode == share_mode) {
			setShareMode(true);
		} else if (mode == add_buddy_mode) {
			setAddBuddyMode(true);
		}

		if (mode == disabled_mode) {
			setEnabled(false);
		} else {
			setEnabled(true);
		}
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(int currentPage) {
		if (this.currentPage != currentPage) {
			this.currentPage = currentPage;
			showPage(currentPage, false);
			pWidget.setCurrentPage(currentPage);

		}
	}

	public int getPageCount() {
		return pageCount;
	}

	public void hookFAQLink() {
		SWTSkinObject FAQObject = skin.getSkinObject("buddies-viewer-nobuddies-link");
		if (null != FAQObject) {
			SWTSkinButtonUtility FAQButton = new SWTSkinButtonUtility(FAQObject);
			FAQButton.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					String url = Constants.URL_FAQ_BY_TOPIC_ENTRY
							+ FAQTopics.FAQ_TOPIC_WHAT_ARE_FRIENDS;
					Utils.launch(url);
				}
			});
		}
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		if (this.isEnabled != isEnabled) {
			this.isEnabled = isEnabled;
			avatarsPanel.setEnabled(isEnabled);
			avatarsPanel.layout(true);

		}
	}

	public int getAvatarsPerPage() {
		return avatarsPerPage;
	}
}
