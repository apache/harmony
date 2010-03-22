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

package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import org.apache.harmony.x.swing.Utilities;


public class MetalTabbedPaneUI extends BasicTabbedPaneUI {

    public class TabbedPaneLayout extends BasicTabbedPaneUI.TabbedPaneLayout {
        protected void normalizeTabRuns(final int tabPlacement,
                                        final int tabCount,
                                        final int start,
                                        final int max) {
            super.normalizeTabRuns(tabPlacement, tabCount, start, max);
        }

        protected void padSelectedTab(final int tabPlacement,
                                      final int selectedIndex) {
            // overridden to do nothing
        }

        protected void rotateTabRuns(final int tabPlacement,
                                     final int selectedRun) {
            // overridden to do nothing
        }
    }

    public static ComponentUI createUI(final JComponent c) {
        return new MetalTabbedPaneUI();
    }

    protected int minTabWidth = 40;
    protected Color selectColor;
    protected Color selectHighlight;
    protected Color tabAreaBackground;

    protected int calculateMaxTabHeight(final int tabPlacement) {
        return super.calculateMaxTabHeight(tabPlacement);
    }

    protected LayoutManager createLayoutManager() {
        if (tabPane.getTabLayoutPolicy() == JTabbedPane.WRAP_TAB_LAYOUT) {
            return new TabbedPaneLayout();
        } else {
            return super.createLayoutManager();
        }
    }

    protected Color getColorForGap(final int currentRun,
                                   final int x, final int y) {
        int adjacentTabIndex = getAdjacentTab(x, y);

        if (adjacentTabIndex == -1) {
            return tabPane.getBackground();
        }

        return tabPane.getSelectedIndex() == adjacentTabIndex
            ? selectColor
            : tabPane.getBackgroundAt(adjacentTabIndex);
    }

    protected int getTabLabelShiftX(final int tabPlacement, final int tabIndex,
                                    final boolean isSelected) {
        return 0;
    }

    protected int getTabLabelShiftY(final int tabPlacement, final int tabIndex,
                                    final boolean isSelected) {
        return 0;
    }

    protected int getTabRunOverlay(final int tabPlacement) {
        if (tabPlacement == TOP || tabPlacement == BOTTOM) {
            return 2;
        }

        return 6;
    }

    protected void installDefaults() {
        super.installDefaults();

        selectColor = UIManager.getColor("TabbedPane.selected");
        selectHighlight = UIManager.getColor("TabbedPane.selectHighlight");
        tabAreaBackground = UIManager.getColor("TabbedPane.tabAreaBackground");
    }

    public void paint(final Graphics g, final JComponent c) {
        super.paint(g, c);
    }

    protected void paintContentBorderBottomEdge(final Graphics g,
                                                final int tabPlacement,
                                                final int selectedIndex,
                                                final int x,
                                                final int y,
                                                final int w,
                                                final int h) {
        g.setColor(darkShadow);
        g.drawRect(x, y + h - 1, w - 1, 0);
        g.setColor(selectColor);
        g.fillRect(x + 1, y + h - 3, w - 2, 2);
        if (tabPlacement == BOTTOM && isTabAdjacentToContentAreaBorder(selectedIndex)) {
            getTabBounds(selectedIndex, calcRect);
            g.drawRect(calcRect.x + 1, y + h - 1, calcRect.width - 2, 0);
        }
    }

    protected void paintContentBorderLeftEdge(final Graphics g,
                                              final int tabPlacement,
                                              final int selectedIndex,
                                              final int x,
                                              final int y,
                                              final int w,
                                              final int h) {
        g.setColor(selectHighlight);
        g.drawRect(x, y, 0, h - 1);
        g.setColor(selectColor);
        g.drawRect(x + 1, y + 1, 0, h - 2);
        if (tabPlacement == LEFT && isTabAdjacentToContentAreaBorder(selectedIndex)) {
            getTabBounds(selectedIndex, calcRect);
            g.drawRect(x, calcRect.y + 2, 0, calcRect.height - 3);
        }
    }

    protected void paintContentBorderRightEdge(final Graphics g,
                                               final int tabPlacement,
                                               final int selectedIndex,
                                               final int x,
                                               final int y,
                                               final int w,
                                               final int h) {
        g.setColor(darkShadow);
        g.drawRect(x + w - 1, y, 0, h - 1);
        g.setColor(selectColor);
        g.fillRect(x + w - 3, y + 1, 2, h - 2);
        if (tabPlacement == RIGHT && isTabAdjacentToContentAreaBorder(selectedIndex)) {
            getTabBounds(selectedIndex, calcRect);
            g.drawRect(x + w - 1, calcRect.y + 1, 0, calcRect.height - 2);
        }
    }

    protected void paintContentBorderTopEdge(final Graphics g,
                                             final int tabPlacement,
                                             final int selectedIndex,
                                             final int x,
                                             final int y,
                                             final int w,
                                             final int h) {
        g.setColor(selectHighlight);
        g.drawRect(x, y, w - 1, 0);
        g.setColor(selectColor);
        g.drawRect(x + 1, y + 1, w - 2, 0);
        if (tabPlacement == TOP && isTabAdjacentToContentAreaBorder(selectedIndex)) {
            getTabBounds(selectedIndex, calcRect);
            g.drawRect(calcRect.x + 2, y, calcRect.width - 3, 0);
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
        calcRect.grow(-3, -3);

        g.setColor(focus);
        g.drawRoundRect(calcRect.x, calcRect.y,
                        calcRect.width, calcRect.height, 5, 5);
    }

    protected void paintHighlightBelowTab() {
        // this method seems to be unused
    }

    protected void paintBottomTabBorder(final int tabIndex, final Graphics g,
                                        final int x, final int y,
                                        final int w, final int h,
                                        final int btm, final int rght,
                                        final boolean isSelected) {
        int[] xs = {x, x, x + 1, x + 3, x + 6, rght - 5, rght - 2, rght, rght + 1, rght + 1};
        int[] ys = {y - 2, btm - 5, btm - 2, btm, btm + 1, btm + 1, btm, btm - 2, btm - 5, y - 3};

        g.setColor(darkShadow);
        g.drawPolyline(xs, ys, xs.length);

        int[] highlightXs = {x + 1, x + 1, x + 3};
        int[] highlightYs = {y - 2, btm - 4, btm - 1};
        g.setColor(isSelected ? selectHighlight : highlight);
        g.drawPolyline(highlightXs, highlightYs, highlightXs.length);
    }

    protected void paintLeftTabBorder(final int tabIndex, final Graphics g,
                                      final int x, final int y,
                                      final int w, final int h,
                                      final int btm, final int rght,
                                      final boolean isSelected) {
        int[] xs = {rght, x + 6, x + 3, x + 1, x, x, x + 1, x + 3, x + 6, rght + 1};
        int[] ys = {btm + 1, btm + 1, btm, btm - 2, btm - 5, y + 6, y + 3, y + 1, y, y};

        g.setColor(darkShadow);
        g.drawPolyline(xs, ys, xs.length);

        int[] highlightXs = {x + 1, x + 1, x + 3, x + 6, rght + 1};
        int[] highlightYs = {btm - 4, y + 5, y + 2, y + 1, y + 1};
        g.setColor(isSelected ? selectHighlight : highlight);
        g.drawPolyline(highlightXs, highlightYs, highlightXs.length);
    }

    protected void paintRightTabBorder(final int tabIndex, final Graphics g,
                                       final int x, final int y,
                                       final int w, final int h,
                                       final int btm, final int rght,
                                       final boolean isSelected) {
        int[] xs = {x - 1, rght - 5, rght - 2, rght, rght + 1, rght + 1, rght, rght - 2, rght - 5, x - 1};
        int[] ys = {btm + 1, btm + 1, btm, btm - 2, btm - 5, y + 6, y + 3, y + 1, y, y};

        g.setColor(darkShadow);
        g.drawPolyline(xs, ys, xs.length);

        int[] highlightXs = {x, rght - 4, rght - 1};
        int[] highlightYs = {y + 1, y + 1, y + 3};
        g.setColor(isSelected ? selectHighlight : highlight);
        g.drawPolyline(highlightXs, highlightYs, highlightXs.length);
    }

    protected void paintTopTabBorder(final int tabIndex, final Graphics g,
                                     final int x, final int y,
                                     final int w, final int h,
                                     final int btm, final int rght,
                                     final boolean isSelected) {
        int[] xs = {x, x, x + 1, x + 3, x + 6, rght - 5, rght - 2, rght, rght + 1, rght + 1};
        int[] ys = {btm + 3, y + 6, y + 3, y + 1, y, y, y + 1, y + 3, y + 6, btm + 4};
        g.setColor(darkShadow);
        g.drawPolyline(xs, ys, xs.length);

        int[] highlightXs = {x + 1, x + 1, x + 3, x + 6, rght - 4, rght - 1};
        int[] highlightYs = {btm + 3, y + 5, y + 2, y + 1, y + 1, y + 3};
        g.setColor(isSelected ? selectHighlight : highlight);
        g.drawPolyline(highlightXs, highlightYs, highlightXs.length);
    }

    protected void paintTabBackground(final Graphics g, final int tabPlacement,
                                      final int tabIndex,
                                      final int x, final int y, final int w,
                                      final int h, final boolean isSelected) {
        Color background = isSelected
            ? selectColor
            : tabPane.getBackgroundAt(tabIndex);
        g.setColor(background);

        g.fillRoundRect(x, y, w + 1, h + 1, 12, 12);

        if (tabPlacement == TOP) {
            g.fillRect(x + 1, y + h - 2, w - 1, 5);
        } else if (tabPlacement == LEFT) {
            g.fillRect(x + w - 2, y + 1, 4, h - 1);
        } else if (tabPlacement == BOTTOM) {
            g.fillRect(x + 1, y - 2, w - 1, 5);
        } else if (tabPlacement == RIGHT) {
            g.fillRect(x, y + 1, 4, h - 1);
        }
    }

    protected void paintTabBorder(final Graphics g, final int tabPlacement,
                                  final int tabIndex,
                                  final int x, final int y, final int w,
                                  final int h, final boolean isSelected) {
        int btm = y + h - 1;
        int rght = x + w - 1;
        if (tabPlacement == TOP) {
            paintTopTabBorder(tabIndex, g, x, y, w, h, btm, rght, isSelected);
        } else if (tabPlacement == LEFT) {
            paintLeftTabBorder(tabIndex, g, x, y, w, h, btm, rght, isSelected);
        } else if (tabPlacement == RIGHT) {
            paintRightTabBorder(tabIndex, g, x, y, w, h, btm, rght, isSelected);
        } else if (tabPlacement == BOTTOM) {
            paintBottomTabBorder(tabIndex, g, x, y, w, h, btm, rght, isSelected);
        }
    }

    protected boolean shouldFillGap(final int currentRun, final int tabIndex,
                                    final int x, final int y) {
        return getAdjacentTab(x, y) != -1;
    }

    protected boolean shouldPadTabRun(final int tabPlacement, final int run) {
        if (runCount <= 1) {
            return false;
        }

        return run != runCount - 1;
    }

    protected boolean shouldRotateTabRuns(final int tabPlacement,
                                          final int selectedRun) {
        return false;
    }

    public void update(final Graphics g, final JComponent c) {
        super.update(g, c);
    }

    private boolean isTabAdjacentToContentAreaBorder(final int selectedIndex) {
        return getRunForTab(tabPane.getTabCount(), selectedIndex) == 0;
    }

    private int getAdjacentTab(final int x, final int y) {
        int newX = x;
        int newY = y;
        int tabPlacement = tabPane.getTabPlacement();
        switch (tabPlacement) {
        case TOP:
        case LEFT:
        case RIGHT:
            newY--;
            break;
        case BOTTOM:
            newY++;
            break;
        }

        return tabForCoordinate(tabPane, newX, newY);
    }

    private boolean isFocusPainted(final int tabIndex) {
        if (tabIndex != getFocusIndex()) {
            return false;
        }

        return tabPane.getIconAt(tabIndex) != null
                || !Utilities.isEmptyString(tabPane.getTitleAt(tabIndex));
    }
}
