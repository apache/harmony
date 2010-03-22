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
 * @author Sergey Burlak, Anton Avtamonov
 */

package javax.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ScrollPaneUI;
import javax.swing.plaf.UIResource;

import org.apache.harmony.x.swing.StringConstants;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class JScrollPane extends JComponent implements ScrollPaneConstants, Accessible {
    //TODO: implement
    protected class AccessibleJScrollPane extends AccessibleJComponent implements ChangeListener, PropertyChangeListener {
        public AccessibleJScrollPane() {
        }

        protected JViewport viewPort;

        public void resetViewPort() {
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SCROLL_PANE;
        }

        public void stateChanged(final ChangeEvent e) {
        }

        public void propertyChange(final PropertyChangeEvent e) {
        }
    }

    protected class ScrollBar extends JScrollBar implements UIResource {
        private boolean unitIncrementSet;
        private boolean blockIncrementSet;

        public ScrollBar(final int orientation) {
            super(orientation);
        }

        public void setUnitIncrement(final int unitIncrement) {
            super.setUnitIncrement(unitIncrement);
            unitIncrementSet = true;
        }

        public int getUnitIncrement(final int direction) {
            return !unitIncrementSet && getViewport().getView() instanceof Scrollable ? ((Scrollable)getViewport().getView()).getScrollableUnitIncrement(getViewport().getViewRect(), getOrientation(), direction)
                                                                                        : super.getUnitIncrement(direction);
        }

        public void setBlockIncrement(final int blockIncrement) {
            super.setBlockIncrement(blockIncrement);
            blockIncrementSet = true;
        }

        public int getBlockIncrement(final int direction) {
            return !blockIncrementSet && getViewport().getView() instanceof Scrollable ? ((Scrollable)getViewport().getView()).getScrollableBlockIncrement(getViewport().getViewRect(), getOrientation(), direction)
                                                                                        : super.getBlockIncrement(direction);
        }
    }


    protected JViewport columnHeader;
    protected JViewport rowHeader;
    protected JScrollBar horizontalScrollBar;
    protected JScrollBar verticalScrollBar;
    protected int horizontalScrollBarPolicy;
    protected int verticalScrollBarPolicy;
    protected Component lowerLeft;
    protected Component lowerRight;
    protected Component upperLeft;
    protected Component upperRight;
    protected JViewport viewport;

    private Border viewportBorder;
    private boolean wheelScrollingEnabled = true;
    private Insets cachedInsets = new Insets(0, 0, 0, 0);

    private static final String UI_CLASS_ID = "ScrollPaneUI";

    private static final String WHEEL_SCROLLING_ENABLED_PROPERTY = "wheelScrollingEnabled";
    private static final String VIEWPORT_BORDER_PROPERTY = "viewportBorder";

    public JScrollPane() {
        this(null, VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    public JScrollPane(final Component view) {
        this(view, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    public JScrollPane(final int vsbPolicy, final int hsbPolicy) {
        this(null, vsbPolicy, hsbPolicy);
    }

    public JScrollPane(final Component view, final int vsbPolicy, final int hsbPolicy) {
        setVerticalScrollBarPolicy(vsbPolicy);
        setHorizontalScrollBarPolicy(hsbPolicy);

        JViewport vp = createViewport();
        vp.setView(view);
        setViewport(vp);

        setVerticalScrollBar(createVerticalScrollBar());
        setHorizontalScrollBar(createHorizontalScrollBar());

        updateUI();

        int initialViewXPos = getComponentOrientation().isLeftToRight() ? 0 : Short.MAX_VALUE;
        int initialViewYPos = getComponentOrientation().isHorizontal() ? 0 : Short.MAX_VALUE;

        vp.setViewPosition(new Point(initialViewXPos, initialViewYPos));
    }


    public ScrollPaneUI getUI() {
        return (ScrollPaneUI)ui;
    }

    public void setUI(final ScrollPaneUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
        setUI((ScrollPaneUI)UIManager.getUI(this));
    }

    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public void setLayout(final LayoutManager layout) {
        super.setLayout(layout);
        if (layout != null) {
            ((ScrollPaneLayout)layout).syncWithScrollPane(this);
        }
    }

    public boolean isValidateRoot() {
        return true;
    }

    public int getVerticalScrollBarPolicy() {
        return verticalScrollBarPolicy;
    }

    public void setVerticalScrollBarPolicy(final int policy) {
        if (policy != VERTICAL_SCROLLBAR_AS_NEEDED
            && policy != VERTICAL_SCROLLBAR_NEVER
            && policy != VERTICAL_SCROLLBAR_ALWAYS) {

            throw new IllegalArgumentException(Messages.getString("swing.25")); //$NON-NLS-1$
        }
        if (this.verticalScrollBarPolicy != policy) {
            int oldValue = this.verticalScrollBarPolicy;
            this.verticalScrollBarPolicy = policy;
            firePropertyChange(StringConstants.VERTICAL_SCROLLBAR_POLICY_PROPERTY, oldValue, policy);
        }
    }

    public int getHorizontalScrollBarPolicy() {
        return horizontalScrollBarPolicy;
    }

    public void setHorizontalScrollBarPolicy(final int policy) {
        if (policy != HORIZONTAL_SCROLLBAR_AS_NEEDED
            && policy != HORIZONTAL_SCROLLBAR_NEVER
            && policy != HORIZONTAL_SCROLLBAR_ALWAYS) {

            throw new IllegalArgumentException(Messages.getString("swing.25")); //$NON-NLS-1$
        }
        if (this.horizontalScrollBarPolicy != policy) {
            int oldValue = this.horizontalScrollBarPolicy;
            this.horizontalScrollBarPolicy = policy;
            firePropertyChange(StringConstants.HORIZONTAL_SCROLLBAR_POLICY_PROPERTY, oldValue, policy);
        }
    }

    public Border getViewportBorder() {
        return viewportBorder;
    }

    public void setViewportBorder(final Border viewportBorder) {
        if (this.viewportBorder != viewportBorder) {
            Border oldValue = this.viewportBorder;
            this.viewportBorder = viewportBorder;
            firePropertyChange(VIEWPORT_BORDER_PROPERTY, oldValue, viewportBorder);
        }
    }

    public Rectangle getViewportBorderBounds() {
        Rectangle result = new Rectangle(getBounds());
        Insets insets = getInsets(cachedInsets);
        result.x = insets.left;
        result.y = insets.top;
        result.width -= insets.left + insets.right;
        result.height -= insets.top + insets.bottom;

        if (rowHeader != null) {
            Rectangle rowHeaderBounds = rowHeader.getBounds();
            if (getComponentOrientation().isLeftToRight()) {
                result.x += rowHeaderBounds.width;
            }
            result.width -= rowHeaderBounds.width;
        }
        if (columnHeader != null) {
            Rectangle columnHeaderBounds = columnHeader.getBounds();
            result.y += columnHeaderBounds.height;
            result.height -= columnHeaderBounds.height;
        }
        if (verticalScrollBar != null) {
            Rectangle verticalScrollbarBounds = verticalScrollBar.getBounds();
            if (!getComponentOrientation().isLeftToRight()) {
                result.x += verticalScrollbarBounds.width;
            }
            result.width -= verticalScrollbarBounds.width;
        }
        if (horizontalScrollBar != null) {
            Rectangle horizontalScrollbarBounds = horizontalScrollBar.getBounds();
            result.height -= horizontalScrollbarBounds.height;
        }

        return result;
    }

    public JScrollBar createHorizontalScrollBar() {
        return new ScrollBar(JScrollBar.HORIZONTAL);
    }

    public JScrollBar createVerticalScrollBar() {
        return new ScrollBar(JScrollBar.VERTICAL);
    }

    public JScrollBar getHorizontalScrollBar() {
        return horizontalScrollBar;
    }

    public JScrollBar getVerticalScrollBar() {
        return verticalScrollBar;
    }

    public void setHorizontalScrollBar(final JScrollBar horizontalScrollBar) {
        if (this.horizontalScrollBar != horizontalScrollBar) {
            JScrollBar oldValue = this.horizontalScrollBar;
            this.horizontalScrollBar = horizontalScrollBar;
            this.horizontalScrollBar.setComponentOrientation(getComponentOrientation());
            updateComponent(HORIZONTAL_SCROLLBAR, oldValue, horizontalScrollBar, StringConstants.HORIZONTAL_SCROLLBAR_PROPERTY);
        }
    }

    public void setVerticalScrollBar(final JScrollBar verticalScrollBar) {
        if (this.verticalScrollBar != verticalScrollBar) {
            JScrollBar oldValue = this.verticalScrollBar;
            this.verticalScrollBar = verticalScrollBar;
            this.verticalScrollBar.setComponentOrientation(getComponentOrientation());
            updateComponent(VERTICAL_SCROLLBAR, oldValue, verticalScrollBar, StringConstants.VERTICAL_SCROLLBAR_PROPERTY);
        }
    }




    public JViewport getViewport() {
        return viewport;
    }

    public void setViewport(final JViewport viewport) {
        JViewport oldValue = this.viewport;
        this.viewport = viewport;
        updateComponent(VIEWPORT, oldValue, viewport, StringConstants.VIEWPORT_PROPERTY);
    }

    public void setViewportView(final Component view) {
        if (viewport == null) {
            setViewport(createViewport());
        }
        viewport.setView(view);
    }

    public JViewport getRowHeader() {
        return rowHeader;
    }

    public void setRowHeader(final JViewport rowHeader) {
        JViewport oldValue = this.rowHeader;
        this.rowHeader = rowHeader;
        updateComponent(ROW_HEADER, oldValue, rowHeader, StringConstants.ROW_HEADER_PROPERTY);
    }

    public void setRowHeaderView(final Component view) {
        if (rowHeader == null) {
            setRowHeader(createViewport());
        }
        rowHeader.setView(view);
    }

    public JViewport getColumnHeader() {
        return columnHeader;
    }

    public void setColumnHeader(final JViewport columnHeader) {
        JViewport oldValue = this.columnHeader;
        this.columnHeader = columnHeader;
        updateComponent(COLUMN_HEADER, oldValue, columnHeader, StringConstants.COLUMN_HEADER_PROPERTY);
    }


    public void setColumnHeaderView(final Component view) {
        if (columnHeader == null) {
            setColumnHeader(createViewport());
        }
        columnHeader.setView(view);
    }

    public Component getCorner(final String key) {
        if (JScrollPane.LOWER_LEFT_CORNER.equals(key)
            || JScrollPane.LOWER_LEADING_CORNER.equals(key)) {

            return lowerLeft;
        } else if (JScrollPane.LOWER_RIGHT_CORNER.equals(key)
                   || JScrollPane.LOWER_TRAILING_CORNER.equals(key)) {

            return lowerRight;
        } else if (JScrollPane.UPPER_LEFT_CORNER.equals(key)
                   || JScrollPane.UPPER_LEADING_CORNER.equals(key)) {

            return upperLeft;
        } else if (JScrollPane.UPPER_RIGHT_CORNER.equals(key)
                   || JScrollPane.UPPER_TRAILING_CORNER.equals(key)) {

            return upperRight;
        }

        return null;
    }

    public void setCorner(final String key, final Component corner) {
        if (JScrollPane.LOWER_LEFT_CORNER.equals(key)
            || JScrollPane.LOWER_LEADING_CORNER.equals(key) && getComponentOrientation().isLeftToRight()
            || JScrollPane.LOWER_TRAILING_CORNER.equals(key) && !getComponentOrientation().isLeftToRight()) {

            Component oldValue = lowerLeft;
            lowerLeft = corner;
            updateComponent(JScrollPane.LOWER_LEFT_CORNER, oldValue, corner, JScrollPane.LOWER_LEFT_CORNER);
        } else if (JScrollPane.LOWER_RIGHT_CORNER.equals(key)
                   || JScrollPane.LOWER_LEADING_CORNER.equals(key) && !getComponentOrientation().isLeftToRight()
                   || JScrollPane.LOWER_TRAILING_CORNER.equals(key) && getComponentOrientation().isLeftToRight()) {

            Component oldValue = lowerRight;
            lowerRight = corner;
            updateComponent(JScrollPane.LOWER_RIGHT_CORNER, oldValue, corner, JScrollPane.LOWER_RIGHT_CORNER);
        } else if (JScrollPane.UPPER_LEFT_CORNER.equals(key)
                   || JScrollPane.UPPER_LEADING_CORNER.equals(key) && getComponentOrientation().isLeftToRight()
                   || JScrollPane.UPPER_TRAILING_CORNER.equals(key) && !getComponentOrientation().isLeftToRight()) {

            Component oldValue = upperLeft;
            upperLeft = corner;
            updateComponent(JScrollPane.UPPER_LEFT_CORNER, oldValue, corner, JScrollPane.UPPER_LEFT_CORNER);
        } else if (JScrollPane.UPPER_RIGHT_CORNER.equals(key)
                   || JScrollPane.UPPER_LEADING_CORNER.equals(key) && !getComponentOrientation().isLeftToRight()
                   || JScrollPane.UPPER_TRAILING_CORNER.equals(key) && getComponentOrientation().isLeftToRight()) {

            Component oldValue = upperRight;
            upperRight = corner;
            updateComponent(JScrollPane.UPPER_RIGHT_CORNER, oldValue, corner, JScrollPane.UPPER_RIGHT_CORNER);
        } else {
            throw new IllegalArgumentException(Messages.getString("swing.26", key)); //$NON-NLS-1$
        }
    }

    public void setComponentOrientation(final ComponentOrientation co) {
        verticalScrollBar.setComponentOrientation(co);
        horizontalScrollBar.setComponentOrientation(co);
        super.setComponentOrientation(co);
    }

    public boolean isWheelScrollingEnabled() {
        return wheelScrollingEnabled;
    }

    public void setWheelScrollingEnabled(final boolean wheelScrollingEnabled) {
        if (this.wheelScrollingEnabled != wheelScrollingEnabled) {
            boolean oldValue = this.wheelScrollingEnabled;
            this.wheelScrollingEnabled = wheelScrollingEnabled;
            firePropertyChange(WHEEL_SCROLLING_ENABLED_PROPERTY, oldValue, wheelScrollingEnabled);
        }
    }

    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJScrollPane();
        }

        return accessibleContext;
    }

    protected JViewport createViewport() {
        return new JViewport();
    }

    protected String paramString() {
        return super.paramString();
    }

    private void updateComponent(final String key, final Component oldComponent, final Component newComponent, final String changedPropertyName) {
        if (oldComponent != null) {
            remove(oldComponent);
        }
        if (newComponent != null) {
            add(newComponent, key);
        }

        firePropertyChange(changedPropertyName, oldComponent, newComponent);
    }
}
