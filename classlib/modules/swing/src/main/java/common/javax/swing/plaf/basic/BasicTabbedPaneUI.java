/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Vadim L. Bogdanov
 */
package javax.swing.plaf.basic;


import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TabbedPaneUI;
import javax.swing.plaf.UIResource;

import javax.swing.text.View;

import org.apache.harmony.x.swing.ButtonCommons;
import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

public class BasicTabbedPaneUI extends TabbedPaneUI implements SwingConstants {

    public class FocusHandler extends FocusAdapter {
        public void focusGained(final FocusEvent e) {
            tabPane.repaint();
        }

        public void focusLost(final FocusEvent e) {
            tabPane.repaint();
        }
    }

    public class MouseHandler extends MouseAdapter {
        public void mousePressed(final MouseEvent e) {
            int index = calculateTabIndexByMouseEvent(e);

            if (index != -1 && tabPane.isEnabledAt(index)) {
                // tab is clicked
                if (!isSelectedTab(index)) {
                    boolean visibleComponentFocused = getFocusIndex() == -1;
                    tabPane.setSelectedIndex(index);
                    if (visibleComponentFocused && getVisibleComponent() != null) {
                        getVisibleComponent().requestFocus();
                    }
                } else {
                    tabPane.requestFocus();
                }
            }
        }
    }

    private class MouseMotionHandler extends MouseMotionAdapter {
        public void mouseMoved(final MouseEvent e) {
            setRolloverTab(calculateTabIndexByMouseEvent(e));
        }
    }

    public class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent e) {
            if ("tabPlacement".equals(e.getPropertyName())) {
                if (tabPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT) {
                    installScrollableTabsComponents();
                }
                tabPane.revalidate();
                tabPane.repaint();
            } else if ("tabLayoutPolicy".equals(e.getPropertyName())) {
                if (tabPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT) {
                    installScrollableTabsComponents();
                } else {
                    uninstallScrollableTabsComponents();
                }
                tabPane.setLayout(createLayoutManager());
                tabPane.revalidate();
                tabPane.repaint();
            }
        }
    }

    public class TabbedPaneLayout implements LayoutManager {
        public void addLayoutComponent(final String name, final Component comp) {
            if (!(comp instanceof UIResource)) {
                calculateLayoutInfo();
            }
        }

        public void calculateLayoutInfo() {
            if (tabPane == null) {
                return;
            }
            int tabCount = tabPane.getTabCount();
            final Component selectedComponent = tabPane.getSelectedComponent();
            if (selectedComponent != null) {
                setVisibleComponent(selectedComponent);
            }

            assureRectsCreated(tabCount);
            calculateTabRects(tabPane.getTabPlacement(), tabCount);
        }

        /**
         * Implements both minimum and preferred size calculations.
         */
        protected Dimension calculateSize(final boolean minimum) {
            int tabPlacement = tabPane.getTabPlacement();
            Dimension size = calculateTabAreaSize(tabPlacement);

            Dimension contentAreaSize = calculateContentAreaSize(minimum);
            Utilities.addInsets(contentAreaSize,
                                getContentBorderInsets(tabPlacement));

            if (isVerticalRun(tabPlacement)) {
                size.width += contentAreaSize.width;
                size.height = Math.max(size.height, contentAreaSize.height);
            } else {
                size.height += contentAreaSize.height;
                size.width = Math.max(size.width, contentAreaSize.width);
            }

            Utilities.addInsets(size, tabPane.getInsets());
            return size;
        }

        private Dimension calculateTabAreaSize(final int tabPlacement) {
            Dimension size = new Dimension();
            Insets insets = getTabAreaInsets(tabPlacement);

            if (isVerticalRun(tabPlacement)) {
                size.height = calculateMaxTabHeight(tabPlacement);
                size.height += insets.top + insets.bottom;
                size.width = preferredTabAreaWidth(tabPlacement, size.height);
            } else {
                size.width = calculateMaxTabWidth(tabPlacement);
                size.width += insets.left + insets.right;
                size.height = preferredTabAreaHeight(tabPlacement, size.width);
            }

            return size;
        }

        private Dimension calculateContentAreaSize(final boolean minimum) {
            Dimension contentAreaSize = new Dimension();

            for (int i = 0; i < tabPane.getComponentCount(); i++) {
                Dimension size;
                if (minimum) {
                    size = tabPane.getComponentAt(i).getMinimumSize();
                } else {
                    size = tabPane.getComponentAt(i).getPreferredSize();
                }
                if (size.width > contentAreaSize.width) {
                    contentAreaSize.width = size.width;
                }
                if (size.height > contentAreaSize.height) {
                    contentAreaSize.height = size.height;
                }
            }

            return contentAreaSize;
        }

        private void getNewTabRunOffsets(final int tabPlacement,
                                         final int runCount,
                                         final Point initialOffset,
                                         final Point newTabRunOffset) {
            newTabRunOffset.setLocation(initialOffset);
            int tabRunIndent = getTabRunIndent(tabPlacement, runCount);
            if (isVerticalRun(tabPlacement)) {
                newTabRunOffset.y += tabRunIndent;
            } else {
                newTabRunOffset.x += tabRunIndent;
            }
        }

        void calculateAvailableRectangleToPlaceTabs(final int tabPlacement,
                final Rectangle tabsRect, final Rectangle tabAreaInnerBounds) {
            SwingUtilities.calculateInnerArea(tabPane, tabsRect);
            Insets tabAreaInsets = getTabAreaInsets(tabPlacement);
            calculateInnerArea(tabsRect, tabAreaInsets);
            tabAreaInnerBounds.setBounds(tabsRect);
        }

        protected void calculateTabRects(final int tabPlacement,
                                         final int tabCount) {
            if (tabCount == 0) {
                return;
            }

            runCount = 1;
            tabRuns[0] = 0;

            FontMetrics fm = getFontMetrics();
            maxTabHeight = calculateMaxTabHeight(tabPlacement);
            maxTabWidth = calculateMaxTabWidth(tabPlacement);

            calculateAvailableRectangleToPlaceTabs(tabPlacement, calcRect,
                                                   tabAreaInnerBounds);

            // split tabs into runs
            Point initialOffset = calcRect.getLocation();
            Point currentOffset = new Point();
            getNewTabRunOffsets(tabPlacement, runCount, initialOffset, currentOffset);
            boolean isVerticalRun = isVerticalRun(tabPlacement);
            boolean justStartedNewRun = true; // not very good to use this flag
            for (int i = 0; i < tabCount; i++) {
                int tabWidth = calculateTabWidth(tabPlacement, i, fm);
                int tabHeight = calculateTabHeight(tabPlacement, i, fm.getHeight());
                if (isVerticalRun) {
                    rects[i].setSize(maxTabWidth, tabHeight);
                } else {
                    rects[i].setSize(tabWidth, maxTabHeight);
                }
                rects[i].setLocation(currentOffset);
                if (!calcRect.contains(rects[i]) && !justStartedNewRun) {
                    // start a new tab run
                    getNewTabRunOffsets(tabPlacement, runCount,
                                        initialOffset, currentOffset);
                    if (runCount >= tabRuns.length) {
                        expandTabRunsArray();
                    }
                    tabRuns[runCount] = i;
                    runCount++;
                }
                rects[i].setLocation(currentOffset);
                if (isVerticalRun) {
                    currentOffset.y += tabHeight;
                } else {
                    currentOffset.x += tabWidth;
                }
                justStartedNewRun = false;
            }
            int start = isVerticalRun
                        ? tabAreaInnerBounds.y
                        : tabAreaInnerBounds.x;
            int length = isVerticalRun
                         ? tabAreaInnerBounds.height
                         : tabAreaInnerBounds.width;
            normalizeTabRuns(tabPlacement, tabCount, start, start + length);

            selectedRun = getRunForTab(tabCount, tabPane.getSelectedIndex());
            if (shouldRotateTabRuns(tabPlacement)) {
                rotateTabRuns(tabPlacement, selectedRun);
            }

            // calculate remaining coordinate (x or y depending on tabPlacement)
            for (int i = 0; i < tabCount; i++) {
                int sign = 1;
                if (tabPlacement == JTabbedPane.BOTTOM) {
                    sign = -1;
                    rects[i].y += tabAreaInnerBounds.height - maxTabHeight;
                } else if (tabPlacement == JTabbedPane.RIGHT) {
                    sign = -1;
                    rects[i].x += tabAreaInnerBounds.width - maxTabWidth;
                }
                if (isVerticalRun) {
                    rects[i].x += sign * (runCount - getRunForTab(tabCount, i) - 1)
                                  * (maxTabWidth - getTabRunOverlay(tabPlacement));
                } else {
                    rects[i].y += sign * (runCount - getRunForTab(tabCount, i) - 1)
                                  * (maxTabHeight - getTabRunOverlay(tabPlacement));
                }
            }

            // pad tab runs
            int lastTabInRun = -1;
            int curRun = getRunForTab(tabCount, 0);
            for (int i = 0; i < runCount; i++) {
                int firstTabInRun = getNextElementInRing(lastTabInRun, tabCount);
                lastTabInRun = lastTabInRun(tabCount, curRun);
                if (shouldPadTabRun(tabPlacement, curRun)) {
                    int max = isVerticalRun ? calcRect.height : calcRect.width;
                    padTabRun(tabPlacement, firstTabInRun, lastTabInRun, max);
                }
                curRun = getNextTabRun(curRun);
            }

            padSelectedTab(tabPlacement, tabPane.getSelectedIndex());
        }

        public void layoutContainer(final Container parent) {
            calculateLayoutInfo();

            int tabPlacement = tabPane.getTabPlacement();

            int tabAreaSize;
            if (isVerticalRun(tabPlacement)) {
                tabAreaSize = calculateTabAreaWidth(tabPlacement,
                                                    runCount, maxTabWidth);
            } else {
                tabAreaSize = calculateTabAreaHeight(tabPlacement,
                                                     runCount, maxTabHeight);
            }

            Rectangle rect = SwingUtilities.calculateInnerArea(tabPane, null);
            if (tabPlacement == JTabbedPane.TOP) {
                rect.y += tabAreaSize;
                rect.height -= tabAreaSize;
            } else if (tabPlacement == JTabbedPane.BOTTOM) {
                rect.height -= tabAreaSize;
            } else if (tabPlacement == JTabbedPane.LEFT) {
                rect.x += tabAreaSize;
                rect.width -= tabAreaSize;
            } else if (tabPlacement == JTabbedPane.RIGHT) {
                rect.width -= tabAreaSize;
            }
            contentAreaBounds.setBounds(rect);

            calculateInnerArea(rect, getContentBorderInsets(tabPlacement));

            if (getVisibleComponent() != null) {
                getVisibleComponent().setBounds(rect);
            }

            calculateTabAreaClipRect(tabPlacement);
        }

        public Dimension minimumLayoutSize(final Container parent) {
            return calculateSize(true);
        }

        /**
         * This function prevents from appearing of runs with
         * small number of tabs. If it is necessary, tabs are
         * moved from the next to last tab run to the last tab run.
         */
        protected void normalizeTabRuns(final int tabPlacement,
                                        final int tabCount,
                                        final int start,
                                        final int max) {
            if (runCount < 2) {
                return;
            }

            final float NORMALIZE_THRESHOLD = 5f / 8f;

            // find out the last tab in the next to last tab run
            boolean isVerticalRun = isVerticalRun(tabPlacement);
            int lastTabPos = isVerticalRun
                             ? (int)rects[tabCount - 1].getMaxY()
                             : (int)rects[tabCount - 1].getMaxX();
            int desiredPos = start + (int)((max - start) * NORMALIZE_THRESHOLD);
            int lastTab = lastTabInRun(tabCount, runCount - 2);
            int firstTab = firstTabInRun(tabCount, runCount - 2);
            while (lastTab - firstTab >= 2 && lastTabPos < desiredPos) {
                lastTabPos += isVerticalRun
                              ? rects[lastTab].height
                              : rects[lastTab].width;
                lastTab--;
            }

            lastTab = getNextTabIndex(lastTab);

            if (tabRuns[runCount - 1] == lastTab) {
                return; // no tabs were moved
            }

            tabRuns[runCount - 1] = lastTab;

            // correct bounds of tabs in the last tab run
            lastTabPos = start;
            for (; lastTab < tabCount; lastTab++) {
                if (isVerticalRun) {
                    rects[lastTab].y = lastTabPos;
                    lastTabPos += rects[lastTab].height;
                } else {
                    rects[lastTab].x = lastTabPos;
                    lastTabPos += rects[lastTab].width;
                }
            }
        }

        /**
         * Increases size of the selected tab using selected tab pad insets.
         */
        protected void padSelectedTab(final int tabPlacement,
                                      final int selectedIndex) {
            if (selectedIndex == -1) {
                return;
            }

            Insets insets = getSelectedTabPadInsets(tabPlacement);
            Rectangle rect = rects[selectedIndex];

            rect.translate(-insets.left, -insets.top);
            rect.width += insets.left + insets.right;
            rect.height += insets.top + insets.bottom;
        }

        /**
         * Increases size of tabs in the given tab run to make width or height
         * of the tab run (depending on <code>tabPlacement</code>) equal
         * to <code>max</code>.
         *
         * @param tabPlacement the tab placement
         * @param start index of the first tab in the tab run
         * @param end index of the last tab in the tab run
         * @param max the desired width or height of the tab run
         */
        protected void padTabRun(final int tabPlacement, final int start,
                                 final int end, final int max) {
            int size = 0;
            for (int i = start; i <= end; i++) {
                size += isVerticalRun(tabPlacement) ? rects[i].height : rects[i].width;
            }
            int increment = (max - size) / (end - start + 1);
            int additionalIncrementToLast = (max - size) % (end - start + 1);

            for (int i = 0; i <= end - start; i++) {
                if (isVerticalRun(tabPlacement)) {
                    rects[start + i].height += increment;
                    rects[start + i].y += i * increment;
                } else {
                    rects[start + i].width += increment;
                    rects[start + i].x += i * increment;
                }
            }
            if (isVerticalRun(tabPlacement)) {
                rects[end].height += additionalIncrementToLast;
            } else {
                rects[end].width += additionalIncrementToLast;
            }
        }

        public Dimension preferredLayoutSize(final Container parent) {
            return calculateSize(false);
        }

        protected int preferredTabAreaHeight(final int tabPlacement,
                                             final int width) {
            int horizRunCount = isVerticalRun(tabPlacement)
                                ? 0
                                : getTabRunCount(tabPane);
            int height = calculateTabAreaHeight(tabPlacement, horizRunCount,
                                            calculateMaxTabHeight(tabPlacement));
            return height;
        }

        protected int preferredTabAreaWidth(final int tabPlacement,
                                            final int height) {
            int vertRunCount = isVerticalRun(tabPlacement)
                               ? getTabRunCount(tabPane)
                               : 0;
            int width = calculateTabAreaWidth(tabPlacement, vertRunCount,
                                              calculateMaxTabWidth(tabPlacement));
            return width;
        }

        public void removeLayoutComponent(final Component comp) {
            if (!(comp instanceof UIResource)) {
                calculateLayoutInfo();
            }
        }

        protected void rotateTabRuns(final int tabPlacement,
                                     final int selectedRun) {
            // rotate tabRuns to move selectedRun to the last position
            int[] temp = new int[selectedRun];
            System.arraycopy(tabRuns, 0, temp, 0, selectedRun);
            System.arraycopy(tabRuns, selectedRun,
                             tabRuns, 0, runCount - selectedRun);
            System.arraycopy(temp, 0, tabRuns,
                             runCount - selectedRun, selectedRun);
        }

        void calculateTabAreaClipRect(final int tabPlacement) {
            SwingUtilities.calculateInnerArea(tabPane, tabAreaClipRect);
            if (tabPlacement == TOP) {
                tabAreaClipRect.height = contentAreaBounds.y - tabAreaClipRect.y;
            }  else if (tabPlacement == LEFT) {
                tabAreaClipRect.width = contentAreaBounds.x - tabAreaClipRect.x;
            } else if (tabPlacement == BOTTOM) {
                tabAreaClipRect.y = (int)contentAreaBounds.getMaxY();
                tabAreaClipRect.height -= tabAreaClipRect.y;
            }  else if (tabPlacement == RIGHT) {
                tabAreaClipRect.x = (int)contentAreaBounds.getMaxX();
                tabAreaClipRect.width -= tabAreaClipRect.x;
            }
        }
    }

    private class ScrollableTabLayout extends TabbedPaneLayout {
        void calculateAvailableRectangleToPlaceTabs(final int tabPlacement,
                final Rectangle tabsRect, final Rectangle tabAreaInnerBounds) {
            super.calculateAvailableRectangleToPlaceTabs(tabPlacement, tabsRect,
                                                         tabAreaInnerBounds);
            tabsRect.setSize(Short.MAX_VALUE, Short.MAX_VALUE);

            if (isVerticalRun(tabPlacement)) {
                tabAreaInnerBounds.height -= leftScrollButton.getHeight()
                        + rightScrollButton.getHeight();
            } else {
                tabAreaInnerBounds.width -= leftScrollButton.getWidth()
                + rightScrollButton.getWidth();
            }
        }

        public void layoutContainer(final Container parent) {
            super.layoutContainer(parent);
            updateScrollButtons();
            layoutScrollButtons();
        }

        protected void padSelectedTab(final int tabPlacement,
                                      final int selectedIndex) {
            // overridden to do nothing
        }

        protected void calculateTabRects(final int tabPlacement,
                                         final int tabCount) {
            super.calculateTabRects(tabPlacement, tabCount);
        }

        private void layoutScrollButtons() {
            if (!leftScrollButton.isVisible()) {
                return;
            }

            rightScrollButton.getBounds(calcRect);

            int tabPlacement = tabPane.getTabPlacement();
            Rectangle c = contentAreaBounds;
            Rectangle b = rightScrollButton.getBounds();

            if (tabPlacement == TOP) {
                b.setLocation(c.x + c.width - b.width, c.y - b.height);
                rightScrollButton.setBounds(b);
                b.translate(-b.width, 0);
                leftScrollButton.setBounds(b);
            } else if (tabPlacement == BOTTOM) {
                b.setLocation(c.x + c.width - b.width, c.y + c.height);
                rightScrollButton.setBounds(b);
                b.translate(-b.width, 0);
                leftScrollButton.setBounds(b);
            } else if (tabPlacement == LEFT) {
                b.setLocation(c.x - b.width, c.y + c.height - b.height);
                rightScrollButton.setBounds(b);
                b.translate(0, -b.height);
                leftScrollButton.setBounds(b);
            } else if (tabPlacement == RIGHT) {
                b.setLocation(c.x + c.width, c.y + c.height - b.height);
                rightScrollButton.setBounds(b);
                b.translate(0, -b.height);
                leftScrollButton.setBounds(b);
            }
        }

        void calculateTabAreaClipRect(final int tabPlacement) {
            super.calculateTabAreaClipRect(tabPlacement);
            if (isVerticalRun(tabPlacement)) {
                tabAreaClipRect.y = tabAreaInnerBounds.y;
                tabAreaClipRect.height = tabAreaInnerBounds.height;
            } else {
                tabAreaClipRect.x = tabAreaInnerBounds.x;
                tabAreaClipRect.width = tabAreaInnerBounds.width;
            }
        }
    }

    private class ScrollButton extends BasicArrowButton
        implements UIResource, ActionListener {

        public ScrollButton(final int direction) {
            super(direction);

            setSize(16, 16);
            setFocusable(false);
            addActionListener(this);
        }

        public void actionPerformed(final ActionEvent e) {
            if (direction == EAST || direction == SOUTH) {
                scrollToShowTab(true);
            } else if (direction == WEST || direction == NORTH) {
                scrollToShowTab(false);
            }
            tabPane.repaint();
        }
    }

    public class TabSelectionHandler implements ChangeListener {
        public void stateChanged(final ChangeEvent e) {
            // we have to relayout immediatelly; in other case we can get
            // in listeners/actions inconsistent state (ex.: NavigateAction)
            tabPane.doLayout();

            scrollToShowTab(tabPane.getSelectedIndex());
            tabPane.revalidate();
            tabPane.repaint();
        }
    }

    private class NavigateAction extends AbstractAction {
        private int direction;

        public NavigateAction(final int direction) {
            this.direction = direction;
        }

        public void actionPerformed(final ActionEvent e) {
            boolean visibleComponentFocused = getFocusIndex() == -1;
            navigateSelectedTab(direction);
            if (visibleComponentFocused && getVisibleComponent() != null) {
                getVisibleComponent().requestFocus();
            }
        }
    }

    private static class MnemonicAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            JTabbedPane tabPane = (JTabbedPane)e.getSource();

            int keyCode = Utilities.keyCharToKeyCode(e.getActionCommand().charAt(0));
            for (int i = 0; i < tabPane.getTabCount(); i++) {
                if (keyCode == tabPane.getMnemonicAt(i)) {
                    tabPane.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    public static ComponentUI createUI(final JComponent c) {
        return new BasicTabbedPaneUI();
    }

    protected static void rotateInsets(final Insets topInsets,
                                       final Insets targetInsets,
                                       final int targetPlacement) {
        if (targetPlacement == JTabbedPane.TOP) {
            targetInsets.set(topInsets.top, topInsets.left,
                             topInsets.bottom, topInsets.right);
        } else if (targetPlacement == JTabbedPane.LEFT) {
            targetInsets.set(topInsets.left, topInsets.top,
                             topInsets.right, topInsets.bottom);
        } else if (targetPlacement == JTabbedPane.BOTTOM) {
            targetInsets.set(topInsets.bottom, topInsets.left,
                             topInsets.top, topInsets.right);
        } else if (targetPlacement == JTabbedPane.RIGHT) {
            targetInsets.set(topInsets.left, topInsets.bottom,
                             topInsets.right, topInsets.top);
        } else {
            assert false : "incorrect targetPlacement";
        }
    }

    private static int TAB_RUNS_ARRAY_SIZE_INCREMENT = 5;
    private static AbstractAction MNEMONIC_ACTION = new MnemonicAction();

    protected transient Rectangle calcRect = new Rectangle();

    protected Color highlight;
    protected Color lightHighlight;
    protected Color shadow;
    protected Color darkShadow;
    protected Color focus;

    protected int maxTabHeight;
    protected int maxTabWidth;

    protected FocusListener focusListener;
    protected MouseListener mouseListener;
    protected ChangeListener tabChangeListener;
    protected PropertyChangeListener propertyChangeListener;

    protected JTabbedPane tabPane;

    protected Rectangle[] rects = {new Rectangle()};
    protected int runCount;
    protected int selectedRun;

    protected Insets contentBorderInsets;
    protected Insets selectedTabPadInsets;
    protected Insets tabAreaInsets;
    protected Insets tabInsets;
    protected int tabRunOverlay;

    private JButton leftScrollButton;
    private JButton rightScrollButton;
    private Component visibleComponent;

    private ActionMap actionMap;

    private MouseMotionAdapter mouseMotionListener;

    /**
     * Tab area's inner area bounds (excluding tab area insets).
     * It is calculated by a layout manager.
     */
    private Rectangle tabAreaInnerBounds = new Rectangle();

    /**
     * Tab area's clip region. It is used to prevent from painting on
     * content area and on scroll buttons when <code>SCROLL_TAB_LAYOUT</code>
     * is used.
     */
    private Rectangle tabAreaClipRect = new Rectangle();
    private int scrollableTabsOffset = 0;

    /**
     * Content area bounds including content area insets but excluding
     * tabbed pane insets. It is calculated by a layout manager.
     */
    private Rectangle contentAreaBounds = new Rectangle();

    private Color selectedTabBackground;

    private int rolloverTab = -1;

    protected int[] tabRuns = new int[5];
    protected int textIconGap;

    /**
     * @deprecated
     */
    protected KeyStroke leftKey;
    /**
     * @deprecated
     */
    protected KeyStroke rightKey;
    /**
     * @deprecated
     */
    protected KeyStroke upKey;
    /**
     * @deprecated
     */
    protected KeyStroke downKey;

    protected void assureRectsCreated(final int tabCount) {
        Rectangle[] oldRects = rects;
        if (rects.length < tabCount) {
            rects = new Rectangle[tabCount];
        }

        System.arraycopy(oldRects, 0, rects, 0, oldRects.length);
        for (int i = oldRects.length; i < rects.length; i++) {
            rects[i] = new Rectangle();
        }
    }

    protected int calculateMaxTabHeight(final int tabPlacement) {
        int height = 0;
        int fontHeight = getFontMetrics().getHeight();
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            height = Math.max(calculateTabHeight(tabPlacement, i, fontHeight),
                              height);
        }
        return height;
    }

    protected int calculateMaxTabWidth(final int tabPlacement) {
        int width = 0;
        FontMetrics fm = getFontMetrics();
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            width = Math.max(calculateTabWidth(tabPlacement, i, fm),
                             width);
        }
        return width;
    }

    protected int calculateTabAreaHeight(final int tabPlacement,
                                         final int horizRunCount,
                                         final int maxTabHeight) {
        int height = maxTabHeight * horizRunCount
                     - getTabRunOverlay(tabPlacement) * (horizRunCount - 1);
        Insets tabAreaInsets = getTabAreaInsets(tabPlacement);
        return height + tabAreaInsets.top + tabAreaInsets.bottom;
    }

    protected int calculateTabAreaWidth(final int tabPlacement,
                                        final int vertRunCount,
                                        final int maxTabWidth) {
        int width = maxTabWidth * vertRunCount
                    - getTabRunOverlay(tabPlacement) * (vertRunCount - 1);
        Insets tabAreaInsets = getTabAreaInsets(tabPlacement);
        return width + tabAreaInsets.left + tabAreaInsets.right;
    }

    protected int calculateTabHeight(final int tabPlacement,
                                     final int tabIndex,
                                     final int fontHeight) {
        int height = fontHeight;

        Icon icon = getIconForTab(tabIndex);
        if (icon != null) {
            height = Math.max(height, icon.getIconHeight());
        }

        height += Math.abs(getTabLabelShiftY(tabPlacement, tabIndex, true) -
                           getTabLabelShiftY(tabPlacement, tabIndex, false));

        // add space to paint a focus indicator and a tab's border
        height += 4;

        Insets insets = getTabInsets(tabPlacement, tabIndex);
        return height + insets.top + insets.bottom;
    }

    protected int calculateTabWidth(final int tabPlacement,
                                    final int tabIndex,
                                    final FontMetrics fm) {
        int width = fm.stringWidth(tabPane.getTitleAt(tabIndex));

        Icon icon = getIconForTab(tabIndex);
        if (icon != null) {
            width += textIconGap + icon.getIconWidth();
        }

        width += Math.abs(getTabLabelShiftX(tabPlacement, tabIndex, true) -
                          getTabLabelShiftX(tabPlacement, tabIndex, false));

        // add space to paint a focus indicator and a tab's border
        width += 3;

        Insets insets = getTabInsets(tabPlacement, tabIndex);
        return width + insets.left + insets.right;
    }

    protected ChangeListener createChangeListener() {
        return new TabSelectionHandler();
    }

    protected FocusListener createFocusListener() {
        return new FocusHandler();
    }

    protected LayoutManager createLayoutManager() {
        if (tabPane.getTabLayoutPolicy() == JTabbedPane.WRAP_TAB_LAYOUT) {
            return new TabbedPaneLayout();
        } else {
            return new ScrollableTabLayout();
        }
    }

    protected MouseListener createMouseListener() {
        return new MouseHandler();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    protected JButton createScrollButton(final int direction) {
        if (direction != NORTH && direction != SOUTH && direction != EAST && direction != WEST) {
            throw new IllegalArgumentException();
        }

        return new ScrollButton(direction);
    }

    protected void expandTabRunsArray() {
        int[] oldTabRuns = tabRuns;
        tabRuns = new int[oldTabRuns.length + TAB_RUNS_ARRAY_SIZE_INCREMENT];
        System.arraycopy(oldTabRuns, 0, tabRuns, 0, oldTabRuns.length);
    }

    protected Insets getContentBorderInsets(final int tabPlacement) {
        return contentBorderInsets;
    }

    protected int getFocusIndex() {
        Component focusOwner =
            KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner != tabPane) {
            return -1;
        }

        return tabPane.getSelectedIndex();
    }

    protected FontMetrics getFontMetrics() {
        return Utilities.getFontMetrics(tabPane);
    }

    protected Icon getIconForTab(final int index) {
        return tabPane.isEnabledAt(index)
               ? tabPane.getIconAt(index)
               : tabPane.getDisabledIconAt(index);
    }

    public Dimension getMaximumSize(final JComponent c) {
        return null;
    }

    public Dimension getMinimumSize(final JComponent c) {
        return null;
    }

    protected int getNextTabIndex(final int base) {
        return getNextElementInRing(base, tabPane.getTabCount());
    }

    protected int getPreviousTabIndex(final int base) {
        return getPreviousElementInRing(base, tabPane.getTabCount());
    }

    protected int getNextTabIndexInRun(final int tabCount, final int base) {
        int run = getRunForTab(tabCount, base);
        int firstTabInRun = firstTabInRun(tabCount, run);
        int lastTabInRun = lastTabInRun(tabCount, run);
        return firstTabInRun
            + getNextElementInRing(base - firstTabInRun,
                                   lastTabInRun - firstTabInRun + 1);
    }

    protected int getPreviousTabIndexInRun(final int tabCount, final int base) {
        int run = getRunForTab(tabCount, base);
        int firstTabInRun = firstTabInRun(tabCount, run);
        int lastTabInRun = lastTabInRun(tabCount, run);
        return firstTabInRun
            + getPreviousElementInRing(base - firstTabInRun,
                                       lastTabInRun - firstTabInRun + 1);
    }

    protected int getNextTabRun(final int baseRun) {
        // using getTabRunCount() instead of runCount leads to stack overflow
        return getNextElementInRing(baseRun, runCount);
    }

    protected int getPreviousTabRun(final int baseRun) {
        return getPreviousElementInRing(baseRun, runCount);
    }

    protected int getRunForTab(final int tabCount, final int tabIndex) {
        for (int run = 0; run < runCount; run++) {
            int firstTabInRun = tabRuns[run];
            if (firstTabInRun <= tabIndex
                    && tabIndex <= lastTabInRun(tabCount, run)) {
                return run;
            }
        }
        return 0;
    }

    protected Insets getSelectedTabPadInsets(final int tabPlacement) {
        Insets rotatedInsets = new Insets(0, 0, 0, 0);
        rotateInsets(selectedTabPadInsets, rotatedInsets, tabPlacement);
        return rotatedInsets;
    }

    protected Insets getTabAreaInsets(final int tabPlacement) {
        Insets rotatedInsets = new Insets(0, 0, 0, 0);
        rotateInsets(tabAreaInsets, rotatedInsets, tabPlacement);
        return rotatedInsets;
    }

    protected Rectangle getTabBounds(final int tabIndex, final Rectangle dest) {
        dest.setBounds(rects[tabIndex]);
        if (isVerticalRun(tabPane.getTabPlacement())) {
            dest.translate(0, scrollableTabsOffset);
        } else {
            dest.translate(scrollableTabsOffset, 0);
        }
        return dest;
    }

    public Rectangle getTabBounds(final JTabbedPane pane, final int index) {
        Rectangle result = new Rectangle();
        return getTabBounds(index, result);
    }

    protected Insets getTabInsets(final int tabPlacement, final int tabIndex) {
        return tabInsets;
    }

    protected int getTabLabelShiftX(final int tabPlacement, final int tabIndex,
                                    final boolean isSelected) {
        int offset = -1;
        if (isSelected) {
            if (tabPlacement == RIGHT) {
                offset = 1;
            }
        } else {
            if (tabPlacement == LEFT) {
                offset = 1;
            }
        }

        return offset;
    }

    protected int getTabLabelShiftY(final int tabPlacement, final int tabIndex,
                                    final boolean isSelected) {
        int offset = 1;
        if (isSelected) {
            if (tabPlacement == TOP) {
                offset = -1;
            }
        } else {
            if (tabPlacement == BOTTOM) {
                offset = -1;
            }
        }

        return offset;
    }

    public int getTabRunCount(final JTabbedPane pane) {
        tabPane.doLayout();
        return runCount;
    }

    protected int getTabRunIndent(final int tabPlacement, final int run) {
        return 0;
    }

    protected int getTabRunOffset(final int tabPlacement, final int tabCount,
                                  final int tabIndex, final boolean forward) {
        int curRun = getRunForTab(tabCount, tabIndex);
        int newRun = !forward
                     ? getNextTabRun(curRun)
                     : getPreviousTabRun(curRun);

        int lastTab = lastTabInRun(tabCount, newRun);
        int rc;
        if (isVerticalRun(tabPlacement)) {
            rc = (int)rects[lastTab].getCenterX()
                 - (int)rects[tabIndex].getCenterX();
        } else {
            rc = (int)rects[lastTab].getCenterY()
                 - (int)rects[tabIndex].getCenterY();
        }
        return rc;
    }

    protected int getTabRunOverlay(final int tabPlacement) {
        return tabRunOverlay;
    }

    protected View getTextViewForTab(final int tabIndex) {
        //TODO: implement when HTML styled text is supported
        return null;
    }

    protected void installComponents() {
        if (tabPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT) {
            installScrollableTabsComponents();
        }
    }

    protected void uninstallComponents() {
        if (tabPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT) {
            uninstallScrollableTabsComponents();
        }
    }

    protected void installDefaults() {
        LookAndFeel.installColorsAndFont(tabPane, "TabbedPane.background",
                                         "TabbedPane.foreground",
                                         "TabbedPane.font");

        darkShadow = UIManager.getColor("TabbedPane.darkShadow");
        shadow = UIManager.getColor("TabbedPane.shadow");
        highlight = UIManager.getColor("TabbedPane.light");
        lightHighlight = UIManager.getColor("TabbedPane.highlight");
        focus = UIManager.getColor("TabbedPane.focus");

        contentBorderInsets = UIManager.getInsets("TabbedPane.contentBorderInsets");
        selectedTabPadInsets = UIManager.getInsets("TabbedPane.selectedTabPadInsets");
        tabAreaInsets = UIManager.getInsets("TabbedPane.tabAreaInsets");
        tabInsets = UIManager.getInsets("TabbedPane.tabInsets");

        tabRunOverlay = UIManager.getInt("TabbedPane.tabRunOverlay");
        textIconGap = UIManager.getInt("TabbedPane.textIconGap");

        selectedTabBackground = UIManager.getColor("control");
    }

    protected void uninstallDefaults() {
    }

    private ActionMap getUIActionMap() {
        if (actionMap != null) {
            return actionMap;
        }

        actionMap = new ActionMapUIResource();
        final AbstractAction navigateEastAction = new NavigateAction(EAST);
        actionMap.put("navigateRight", navigateEastAction);
        final AbstractAction navigateWestAction = new NavigateAction(WEST);
        actionMap.put("navigateLeft", navigateWestAction);
        actionMap.put("navigateUp", new NavigateAction(NORTH));
        actionMap.put("navigateDown", new NavigateAction(SOUTH));

        // "ctrl DOWN", "ctrl KP_DOWN"
        actionMap.put("requestFocusForVisibleComponent",
                      new AbstractAction() {
                        public void actionPerformed(final ActionEvent e) {
                            if (getVisibleComponent() != null) {
                                getVisibleComponent().requestFocus();
                            }
                        }
        });

        // "ctrl PAGE_DOWN", "navigatePageDown"
        actionMap.put("navigatePageDown", navigateWestAction);
        // "ctrl PAGE_UP", "navigatePageUp"
        actionMap.put("navigatePageUp", navigateEastAction);

        // "ctrl KP_UP", "ctrl UP"
        actionMap.put("requestFocus",
                      new AbstractAction() {
                        public void actionPerformed(final ActionEvent e) {
                            tabPane.requestFocus();
                        }
        });

        actionMap.put(StringConstants.MNEMONIC_ACTION, MNEMONIC_ACTION);

        return actionMap;
    }

    protected void installKeyboardActions() {
        SwingUtilities.replaceUIInputMap(tabPane,
            JComponent.WHEN_FOCUSED,
            (InputMap)UIManager.get("TabbedPane.focusInputMap"));

        SwingUtilities.replaceUIInputMap(tabPane,
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
            (InputMap)UIManager.get("TabbedPane.ancestorInputMap"));

        SwingUtilities.replaceUIActionMap(tabPane, getUIActionMap());
    }

    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIInputMap(tabPane,
            JComponent.WHEN_FOCUSED, null);
        SwingUtilities.replaceUIInputMap(tabPane,
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
        SwingUtilities.replaceUIActionMap(tabPane, null);
    }

    protected void installListeners() {
        if (focusListener == null) {
            focusListener = createFocusListener();
        }
        tabPane.addFocusListener(focusListener);

        if (mouseListener == null) {
            mouseListener = createMouseListener();
        }
        tabPane.addMouseListener(mouseListener);

        if (mouseMotionListener == null) {
            mouseMotionListener = new MouseMotionHandler();
        }
        tabPane.addMouseMotionListener(mouseMotionListener);

        if (tabChangeListener == null) {
            tabChangeListener = createChangeListener();
        }
        tabPane.addChangeListener(tabChangeListener);

        if (propertyChangeListener == null) {
            propertyChangeListener = createPropertyChangeListener();
        }
        tabPane.addPropertyChangeListener(propertyChangeListener);
    }

    protected void uninstallListeners() {
        tabPane.removeFocusListener(focusListener);
        tabPane.removeMouseListener(mouseListener);
        tabPane.removeMouseMotionListener(mouseMotionListener);
        tabPane.removeChangeListener(tabChangeListener);
        tabPane.removePropertyChangeListener(propertyChangeListener);
    }

    public void installUI(final JComponent c) {
        tabPane = (JTabbedPane) c;
        setRolloverTab(-1);

        installDefaults();
        tabPane.setLayout(createLayoutManager());
        installComponents();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(final JComponent c) {
        uninstallDefaults();
        tabPane.setLayout(null);
        uninstallComponents();
        uninstallListeners();
        uninstallKeyboardActions();
    }

    protected int lastTabInRun(final int tabCount, final int run) {
        int nextRun = getNextTabRun(run);
        int firstIndex = firstTabInRun(tabCount, nextRun);
        return getPreviousTabIndex(firstIndex);
    }

    private int firstTabInRun(final int tabCount, final int run) {
        return tabRuns[run];
    }

    protected void layoutLabel(final int tabPlacement,
                               final FontMetrics metrics,
                               final int tabIndex,
                               final String title,
                               final Icon icon,
                               final Rectangle tabRect,
                               final Rectangle iconRect,
                               final Rectangle textRect,
                               final boolean isSelected) {
        iconRect.setBounds(0, 0, 0, 0);
        textRect.setBounds(0, 0, 0, 0);

        // calculate inner tab area
        calcRect.setBounds(tabRect);
        calculateInnerArea(calcRect, getTabInsets(tabPlacement, tabIndex));
        calcRect.translate(getTabLabelShiftX(tabPlacement, tabIndex, isSelected),
                           getTabLabelShiftY(tabPlacement, tabIndex, isSelected));
        boolean isLTR = tabPane.getComponentOrientation().isLeftToRight();
        SwingUtilities.layoutCompoundLabel(getFontMetrics(),
                                           title, icon,
                                           CENTER, CENTER,
                                           CENTER,
                                           isLTR ? RIGHT : LEFT,
                                           calcRect, iconRect, textRect, textIconGap);
    }

    protected void navigateSelectedTab(final int direction) {
        int tabPlacement = tabPane.getTabPlacement();
        int correctedDirection = rotateDirection(tabPlacement, direction);
        int selectedIndex = tabPane.getSelectedIndex();

        if (correctedDirection == EAST) {
            selectNextTabInRun(selectedIndex);
        } else if (correctedDirection == WEST) {
            selectPreviousTabInRun(selectedIndex);
        } else {
            int tabRunOffset = getTabRunOffset(tabPlacement,
                                               tabPane.getTabCount(),
                                               selectedIndex,
                                               correctedDirection == SOUTH);
            selectAdjacentRunTab(tabPlacement, selectedIndex, tabRunOffset);
        }
    }

    public void paint(final Graphics g, final JComponent c) {
	if (g == null) {
            throw new NullPointerException();
        }

        if (tabPane.getTabCount() == 0) {
            return;
        }

        int tabPlacement = tabPane.getTabPlacement();
        int selectedIndex = tabPane.getSelectedIndex();

        paintTabArea(g, tabPlacement, selectedIndex);
        paintContentBorder(g, tabPlacement, selectedIndex);
    }

    protected void paintContentBorder(final Graphics g, final int tabPlacement,
                                      final int selectedIndex) {
        paintContentBorderTopEdge(g, tabPlacement, selectedIndex,
                                  contentAreaBounds.x, contentAreaBounds.y,
                                  contentAreaBounds.width,
                                  contentAreaBounds.height);
        paintContentBorderLeftEdge(g, tabPlacement, selectedIndex,
                                   contentAreaBounds.x, contentAreaBounds.y,
                                   contentAreaBounds.width,
                                   contentAreaBounds.height);
        paintContentBorderBottomEdge(g, tabPlacement, selectedIndex,
                                     contentAreaBounds.x, contentAreaBounds.y,
                                     contentAreaBounds.width,
                                     contentAreaBounds.height);
        paintContentBorderRightEdge(g, tabPlacement, selectedIndex,
                                    contentAreaBounds.x, contentAreaBounds.y,
                                    contentAreaBounds.width,
                                    contentAreaBounds.height);
    }

    /**
     * x, y, w, h are bounds of the content area including content area
     * insets.
     */
    protected void paintContentBorderBottomEdge(final Graphics g,
                                                final int tabPlacement,
                                                final int selectedIndex,
                                                final int x,
                                                final int y,
                                                final int w,
                                                final int h) {
        int xx = x;
        if (tabPlacement == BOTTOM) {
            getTabBounds(selectedIndex, calcRect);
            if (calcRect.x == x) {
                xx++;
                calcRect.x++;
                calcRect.width--;
            }
        }
        g.setColor(darkShadow);
        g.fillRect(xx, y + h - 1, w, 1);

        g.setColor(highlight);
        g.fillRect(x + 2, y + h - 3, w - 4, 2);

        if (tabPlacement == BOTTOM
                && isTabAdjacentToContentAreaBorder(selectedIndex)) {
            g.setColor(selectedTabBackground);
            g.fillRect(calcRect.x, y + h - 2, calcRect.width, 1);
            g.fillRect(calcRect.x, y + h - 1, calcRect.width - 1, 1);
        }
    }

    /**
     * x, y, w, h are bounds of the content area including content area
     * insets.
     */
    protected void paintContentBorderLeftEdge(final Graphics g,
                                              final int tabPlacement,
                                              final int selectedIndex,
                                              final int x,
                                              final int y,
                                              final int w,
                                              final int h) {
        g.setColor(lightHighlight);
        g.fillRect(x, y, 1, h - 1);

        g.setColor(highlight);
        g.fillRect(x + 1, y + 1, 1, h - 3);

        if (tabPlacement == LEFT) {
            getTabBounds(selectedIndex, calcRect);
            if (calcRect.y == y) {
                calcRect.y++;
                calcRect.height--;
            }
            if (isTabAdjacentToContentAreaBorder(selectedIndex)) {
                g.setColor(selectedTabBackground);
                g.fillRect(x, calcRect.y, 1, calcRect.height);
            }
        }
    }

    /**
     * x, y, w, h are bounds of the content area including content area
     * insets.
     */
    protected void paintContentBorderRightEdge(final Graphics g,
                                               final int tabPlacement,
                                               final int selectedIndex,
                                               final int x,
                                               final int y,
                                               final int w,
                                               final int h) {
        int yy = y;
        if (tabPlacement == RIGHT) {
            getTabBounds(selectedIndex, calcRect);
            if (calcRect.y == y) {
                yy++;
                calcRect.y++;
                calcRect.height--;
            }
        }
        g.setColor(darkShadow);
        g.fillRect(x + w - 1, yy, 1, h);

        g.setColor(highlight);
        g.fillRect(x + w - 3, y + 2, 2, h - 4);

        if (tabPlacement == RIGHT
                && isTabAdjacentToContentAreaBorder(selectedIndex)) {
            g.setColor(selectedTabBackground);
            g.fillRect(x + w - 1, calcRect.y, 1, calcRect.height - 1);
            g.fillRect(x + w - 2, calcRect.y, 1, calcRect.height);
        }
    }

    /**
     * x, y, w, h are bounds of the content area including content area
     * insets.
     */
    protected void paintContentBorderTopEdge(final Graphics g,
                                             final int tabPlacement,
                                             final int selectedIndex,
                                             final int x,
                                             final int y,
                                             final int w,
                                             final int h) {
        g.setColor(lightHighlight);
        g.fillRect(x, y, w - 1, 1);

        g.setColor(highlight);
        g.fillRect(x + 1, y + 1, w - 3, 1);

        if (tabPlacement == TOP && isTabAdjacentToContentAreaBorder(selectedIndex)) {
            getTabBounds(selectedIndex, calcRect);
            g.setColor(selectedTabBackground);
            g.fillRect(calcRect.x, y, calcRect.width, 1);
        }
    }

    protected void paintFocusIndicator(final Graphics g, final int tabPlacement,
                                       final Rectangle[] rects, final int tabIndex,
                                       final Rectangle iconRect,
                                       final Rectangle textRect,
                                       final boolean isSelected) {
        if (!isFocusPainted(tabIndex)) {
            return;
        }

        calcRect.setBounds(rects[tabIndex]);
        calcRect.grow(-4, -4);
        ButtonCommons.paintFocus(g, calcRect, focus);
    }

    private boolean isFocusPainted(final int tabIndex) {
        if (tabIndex != getFocusIndex()) {
            return false;
        }

        return tabPane.getIconAt(tabIndex) != null
                || !Utilities.isEmptyString(tabPane.getTitleAt(tabIndex));
    }

    protected void paintIcon(final Graphics g, final int tabPlacement,
                             final int tabIndex, final Icon icon,
                             final Rectangle iconRect, final boolean isSelected) {
        if (icon != null) {
            icon.paintIcon(tabPane, g, iconRect.x, iconRect.y);
        }
    }

    protected void paintTab(final Graphics g, final int tabPlacement,
                            final Rectangle[] rects, final int tabIndex,
                            final Rectangle iconRect, final Rectangle textRect) {
        Rectangle r = rects[tabIndex];
        boolean isSelected = isSelectedTab(tabIndex);
        String title = tabPane.getTitleAt(tabIndex);

        paintTabBackground(g, tabPlacement, tabIndex, r.x, r.y,
                           r.width, r.height, isSelected);

        Icon icon = getIconForTab(tabIndex);
        layoutLabel(tabPlacement, getFontMetrics(), tabIndex, title,
                    icon, r, iconRect, textRect,
                    isSelected);

        paintIcon(g, tabPlacement, tabIndex, icon, iconRect, isSelected);
        paintText(g, tabPlacement, tabPane.getFont(), getFontMetrics(),
                  tabIndex, title, textRect, isSelected);
        paintFocusIndicator(g, tabPlacement, rects, tabIndex,
                            iconRect, textRect, isSelected);
        paintTabBorder(g, tabPlacement, tabIndex, r.x, r.y,
                       r.width, r.height, isSelected);
    }

    protected void paintTabArea(final Graphics g, final int tabPlacement,
                                final int selectedIndex) {
        Shape oldClip = g.getClip();
        g.clipRect(tabAreaClipRect.x, tabAreaClipRect.y,
                   tabAreaClipRect.width, tabAreaClipRect.height);

        Point translate = new Point();
        if (tabPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT) {
            if (isVerticalRun(tabPlacement)) {
                translate.setLocation(0, scrollableTabsOffset);
            } else {
                translate.setLocation(scrollableTabsOffset, 0);
            }
            g.translate(translate.x, translate.y);
        }

        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();
        int tabCount = tabPane.getTabCount();

        for(int run = runCount - 1; run >=0; run--) {
            for (int i = firstTabInRun(tabCount, run);
                 i <= lastTabInRun(tabCount, run); i++) {

                paintTab(g, tabPlacement, rects, i, iconRect, textRect);
            }
            if (run == selectedRun) {
                paintTab(g, tabPlacement, rects, tabPane.getSelectedIndex(),
                         iconRect, textRect);
            }
        }

        g.setClip(oldClip);
        g.translate(-translate.x, -translate.y);
    }

    protected void paintTabBackground(final Graphics g, final int tabPlacement,
                                      final int tabIndex,
                                      final int x, final int y, final int w,
                                      final int h, final boolean isSelected) {
        Color background = isSelected
                           ? selectedTabBackground
                           : tabPane.getBackgroundAt(tabIndex);
        int xx = x;
        int yy = y;
        int ww = w;
        int hh = h;
        if (tabPlacement == JTabbedPane.TOP) {
            hh += 2;
        } else if (tabPlacement == JTabbedPane.BOTTOM) {
            hh += 2;
            yy -= 2;
        } else if (tabPlacement == JTabbedPane.LEFT) {
            ww += 2;
        } else if (tabPlacement == JTabbedPane.RIGHT) {
            ww += 2;
            xx -= 2;
        }

        g.setColor(background);
        g.fillRect(xx + 1, yy + 1, ww - 2, hh - 2);
    }

    protected void paintTabBorder(final Graphics g, final int tabPlacement,
                                  final int tabIndex,
                                  final int x, final int y, final int w,
                                  final int h, final boolean isSelected) {
        int xx = x;
        int yy = y;
        int ww = w;
        int hh = h;
        if (tabPlacement == JTabbedPane.TOP) {
            hh += 2;
        } else if (tabPlacement == JTabbedPane.BOTTOM) {
            hh += 2;
            yy -= 2;
        } else if (tabPlacement == JTabbedPane.LEFT) {
            ww += 2;
        } else if (tabPlacement == JTabbedPane.RIGHT) {
            ww += 2;
            xx -= 2;
        }

        int[] highlightX = {xx, xx, xx + ww - 2};
        int[] highlightY = {yy + hh - 2, yy, yy};
        g.setColor(lightHighlight);
        g.drawPolyline(highlightX, highlightY, highlightX.length);

        int[] shadowX = {xx + ww - 1, xx + ww - 1, xx + 1};
        int[] shadowY = {yy + 1, yy + hh - 1, yy + hh - 1};
        g.setColor(darkShadow);
        g.drawPolyline(shadowX, shadowY, shadowX.length);

        g.setColor(shadow);
        g.drawRect(xx + ww - 2, yy + 1, 0, hh - 3);
        g.drawRect(xx + 1, yy + hh - 2, ww - 3, 0);
    }

    protected void paintText(final Graphics g, final int tabPlacement,
                             final Font font, final FontMetrics metrics,
                             final int tabIndex, final String title,
                             final Rectangle textRect, final boolean isSelected) {
        Color color = tabPane.isEnabledAt(tabIndex)
            ? tabPane.getForegroundAt(tabIndex)
            : tabPane.getBackgroundAt(tabIndex).darker();
        ButtonCommons.paintText(g, metrics, title,
                                tabPane.getDisplayedMnemonicIndexAt(tabIndex),
                                textRect, title, color);
    }

    protected void selectAdjacentRunTab(final int tabPlacement,
                                        final int tabIndex,
                                        final int offset) {
        getTabBounds(tabIndex, calcRect);
        int x = (int)calcRect.getCenterX();
        int y = (int)calcRect.getCenterY();
        if (isVerticalRun(tabPlacement)) {
            x += offset;
        } else {
            y += offset;
        }
        int newTabIndex = tabForCoordinate(tabPane, x, y);
        if (newTabIndex != -1) {
            if (tabPane.isEnabledAt(newTabIndex)) {
                tabPane.setSelectedIndex(newTabIndex);
            } else {
                selectNextTab(newTabIndex);
            }
        }
    }

    protected void selectNextTab(final int current) {
        int i = current;
        do {
            i = getNextTabIndex(i);
        } while (i != current && !tabPane.isEnabledAt(i));

        tabPane.setSelectedIndex(i);
    }

    protected void selectNextTabInRun(final int current) {
        int tabCount = tabPane.getTabCount();

        int i = current;
        do {
            i = getNextTabIndexInRun(tabCount, i);
        } while (i != current && !tabPane.isEnabledAt(i));

        tabPane.setSelectedIndex(i);
    }

    protected void selectPreviousTab(final int current) {
        int i = current;
        do {
            i = getPreviousTabIndex(i);
        } while (i != current && !tabPane.isEnabledAt(i));

        tabPane.setSelectedIndex(i);
    }

    protected void selectPreviousTabInRun(final int current) {
        int tabCount = tabPane.getTabCount();

        int i = current;
        do {
            i = getPreviousTabIndexInRun(tabCount, i);
        } while (i != current && !tabPane.isEnabledAt(i));

        tabPane.setSelectedIndex(i);
    }

    protected void setRolloverTab(final int index) {
        rolloverTab = index;
    }

    protected int getRolloverTab() {
        return rolloverTab;
    }

    protected void setVisibleComponent(final Component component) {
        Component oldVisible = getVisibleComponent();
        if (oldVisible != component) {
        if (oldVisible != null) {
            oldVisible.setVisible(false);
        }
        visibleComponent = component;
        }

        if (visibleComponent != null) {
            visibleComponent.setVisible(true);
        }
    }

    protected Component getVisibleComponent() {
        return visibleComponent;
    }

    protected boolean shouldPadTabRun(final int tabPlacement, final int run) {
        return runCount > 1;
    }

    protected boolean shouldRotateTabRuns(final int tabPlacement) {
        return true;
    }

    public int tabForCoordinate(final JTabbedPane pane,
                                final int x, final int y) {
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            getTabBounds(i, calcRect);
            if (calcRect.contains(x, y)) {
                return i;
            }
        }

        return -1;
    }

    private int rotateDirection(final int tabPlacement, final int direction) {
        if (tabPlacement == LEFT || tabPlacement == RIGHT) {
            switch (direction) {
            case NORTH:
                return WEST;
            case SOUTH:
                return EAST;
            case WEST:
                return NORTH;
            case EAST:
                return SOUTH;
            }
        }
        return direction;
    }

    private int getNextElementInRing(final int index, final int ringSize) {
        return (index + 1) % ringSize;
    }

    private int getPreviousElementInRing(final int index, final int ringSize) {
        return (index + ringSize - 1) % ringSize;
    }

    private void installScrollableTabsComponents() {
//        if (scrollableTabArea == null) {
//            scrollableTabArea = new ScrollableTabPanel();
//        }
//        tabPane.add(scrollableTabArea);
        uninstallScrollableTabsComponents();

        if (isVerticalRun(tabPane.getTabPlacement())) {
            leftScrollButton = createScrollButton(NORTH);
            rightScrollButton = createScrollButton(SOUTH);
        } else {
            leftScrollButton = createScrollButton(WEST);
            rightScrollButton = createScrollButton(EAST);
        }
        tabPane.add(leftScrollButton);
        tabPane.add(rightScrollButton);
    }

    private void uninstallScrollableTabsComponents() {
//        tabPane.remove(scrollableTabArea);
        tabPane.remove(leftScrollButton);
        tabPane.remove(rightScrollButton);
        scrollableTabsOffset = 0;
    }

    private void calculateInnerArea(final Rectangle rect,
                                    final Insets insets) {
        rect.x += insets.left;
        rect.y += insets.top;
        rect.width -= insets.left + insets.right;
        rect.height -= insets.top + insets.bottom;
    }

    private boolean isVerticalRun(final int tabPlacement) {
        return tabPlacement == LEFT || tabPlacement == RIGHT;
    }

    private boolean isSelectedTab(final int tabIndex) {
        return tabPane.getSelectedIndex() == tabIndex;
    }

    private int findTruncatedTab(final boolean forward) {
        int inc;
        int start;
        int end;

        if (forward) {
            inc = -1;
            start = tabPane.getTabCount() - 1;
            end = 0;
        } else {
            inc = 1;
            start = 0;
            end = tabPane.getTabCount() - 1;
        }

        int rc = end;
        for (int i = start; i != end; i += inc) {
            if (tabAreaInnerBounds.intersects(getTabBounds(i, calcRect))) {
                rc = i;
                break;
            }
        }

        // correct the result if border of tabAreaInnerBounds lies
        // exactly _between_ tabs
        getTabBounds(rc, calcRect);
        if (forward) {
            // "- 1" because contains() returns false for points on the border
            if (tabAreaInnerBounds.contains(calcRect.getMaxX() - 1, calcRect.getMaxY() - 1)
                    && rc < tabPane.getTabCount() - 1) {
                rc++;
            }
        } else {
            // "+ 1" because contains() returns false for points on the border
            if (tabAreaInnerBounds.contains(calcRect.x + 1, calcRect.y + 1)
                    && rc > 0) {
                rc--;
            }
        }

        return rc;
    }

    private int calculateScrollDelta(final Rectangle r, final boolean forward) {
        int delta = 0;

        if (forward) {
            delta = Math.min(tabAreaInnerBounds.x + tabAreaInnerBounds.width
                             - (r.x + r.width),
                             tabAreaInnerBounds.y + tabAreaInnerBounds.height
                             - (r.y + r.height));
        } else {
            delta = Math.max(tabAreaInnerBounds.x - r.x,
                             tabAreaInnerBounds.y - r.y);
        }

        return delta;
    }

    private void scrollToShowTab(final boolean  forward) {
        if (tabPane.getTabLayoutPolicy() != JTabbedPane.SCROLL_TAB_LAYOUT) {
            return;
        }

        int tabIndex = findTruncatedTab(forward);
        updateScrollableTabOffset(tabIndex, forward);
    }

    private void scrollToShowTab(final int selectedIndex) {
        if (tabPane.getTabLayoutPolicy() != JTabbedPane.SCROLL_TAB_LAYOUT) {
            return;
        }

        getTabBounds(selectedIndex, calcRect);
        if (tabAreaInnerBounds.contains(calcRect)) {
            return;
        }

        boolean forward = tabAreaInnerBounds.contains(calcRect.x, calcRect.y);
        updateScrollableTabOffset(selectedIndex, forward);
    }

    private void updateScrollableTabOffset(final int tabIndex,
                                           final boolean forward) {
        getTabBounds(tabIndex, calcRect);
        int delta = calculateScrollDelta(calcRect, forward);
        if (tabIndex > 0 && tabIndex < tabPane.getTabCount() - 1
/*                || tabIndex == 0 && forward
                || tabIndex == tabPane.getTabCount() - 1 && !forward*/) {
            // the idea of this code is to make visible part of the next tab
            if (delta > 0) {
                delta += 5;
            } else if (delta < 0) {
                delta -= 5;
            }
        }
        scrollableTabsOffset += delta;
        updateScrollButtons();
    }

    private int calculateTabIndexByMouseEvent(final MouseEvent e) {
        Point p = SwingUtilities.convertPoint(e.getComponent(),
                                              e.getX(), e.getY(), tabPane);
        if (!tabAreaInnerBounds.contains(p)) {
            return -1;
        }
        return tabForCoordinate(tabPane, p.x, p.y);
    }

    private boolean isTabAdjacentToContentAreaBorder(final int selectedIndex) {
        return getRunForTab(tabPane.getTabCount(), selectedIndex) == 0;
    }

    private void updateScrollButtons() {
        leftScrollButton.setEnabled(!tabAreaInnerBounds.contains(
                getTabBounds(0, calcRect)));
        rightScrollButton.setEnabled(!tabAreaInnerBounds.contains(
                getTabBounds(tabPane.getTabCount() - 1, calcRect)));

        if (!leftScrollButton.isEnabled() && !rightScrollButton.isEnabled()) {
            leftScrollButton.setVisible(false);
            rightScrollButton.setVisible(false);
        } else {
            leftScrollButton.setVisible(true);
            rightScrollButton.setVisible(true);
        }
    }
}
