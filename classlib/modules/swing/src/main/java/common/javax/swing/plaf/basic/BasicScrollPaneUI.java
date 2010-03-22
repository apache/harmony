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

package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.LookAndFeel;
import javax.swing.ScrollPaneConstants;
import javax.swing.ScrollPaneLayout;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ScrollPaneUI;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;


public class BasicScrollPaneUI extends ScrollPaneUI implements ScrollPaneConstants {

    public class HSBChangeListener implements ChangeListener {
        public void stateChanged(final ChangeEvent e) {
            JScrollBar hsb = scrollpane.getHorizontalScrollBar();
            int value = hsb.getValue();
            boolean isLeftToRight = hsb.getComponentOrientation().isLeftToRight();
            scrollViewport(scrollpane.getViewport(), value, isLeftToRight);
            scrollViewport(scrollpane.getColumnHeader(), value, isLeftToRight);
        }

        private void scrollViewport(final JViewport viewport, final int value, final boolean isLeftToRight) {
            if (viewport != null) {
                viewport.setViewPosition(new Point(getHorizontalPosition(viewport, value, isLeftToRight), viewport.getViewPosition().y));
            }
        }
    }

    public class VSBChangeListener implements ChangeListener {
        public void stateChanged(final ChangeEvent e) {
            JScrollBar vsb = scrollpane.getVerticalScrollBar();
            int value = vsb.getValue();
            scrollViewport(scrollpane.getViewport(), value);
            scrollViewport(scrollpane.getRowHeader(), value);
        }

        private void scrollViewport(final JViewport viewport, final int value) {
            if (viewport != null) {
                viewport.setViewPosition(new Point(viewport.getViewPosition().x, value));
            }
        }
    }

    public class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent e) {
            String changedProperty = e.getPropertyName();
            if (StringConstants.VERTICAL_SCROLLBAR_PROPERTY.equals(changedProperty)
                || StringConstants.HORIZONTAL_SCROLLBAR_PROPERTY.equals(changedProperty)
                || StringConstants.VERTICAL_SCROLLBAR_POLICY_PROPERTY.equals(changedProperty)
                || StringConstants.HORIZONTAL_SCROLLBAR_POLICY_PROPERTY.equals(changedProperty)) {

                updateScrollBarDisplayPolicy(e);
            } else if (StringConstants.COLUMN_HEADER_PROPERTY.equals(changedProperty)) {
                updateColumnHeader(e);
            } else if (StringConstants.ROW_HEADER_PROPERTY.equals(changedProperty)) {
                updateRowHeader(e);
            } else if (StringConstants.VIEWPORT_PROPERTY.equals(changedProperty)) {
                updateViewport(e);
            } else if (StringConstants.COMPONENT_ORIENTATION.equals(changedProperty)) {
                uninstallKeyboardActions(scrollpane);
                installKeyboardActions(scrollpane);
            }

            scrollpane.revalidate();
            scrollpane.repaint();
        }
    }

    public class ViewportChangeHandler implements ChangeListener {
        public void stateChanged(final ChangeEvent e) {
            syncScrollPaneWithViewport();
        }
    }

    protected class MouseWheelHandler implements MouseWheelListener {
        public void mouseWheelMoved(final MouseWheelEvent e) {
            if (scrollpane.isWheelScrollingEnabled()) {
                if (scrollpane.getVerticalScrollBar().isVisible()) {
                    scroll(scrollpane.getVerticalScrollBar(), e);
                } else if (scrollpane.getHorizontalScrollBar().isVisible()) {
                    scroll(scrollpane.getHorizontalScrollBar(), e);
                }
            }
        }

        private void scroll(final JScrollBar sb, final MouseWheelEvent e) {
            int totalAmount = 0;
            if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                totalAmount = e.getUnitsToScroll() * sb.getUnitIncrement(e.getWheelRotation());
            } else if (e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
                totalAmount = e.getWheelRotation() * sb.getBlockIncrement(e.getWheelRotation());
            }
            sb.setValue(sb.getValue() + totalAmount);
        }
    }

    private class HSBPropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if (StringConstants.MODEL_PROPERTY_CHANGED.equals(propertyName)) {
                BoundedRangeModel oldValue = (BoundedRangeModel)e.getOldValue();
                if (oldValue != null) {
                    oldValue.removeChangeListener(hsbChangeListener);
                }

                BoundedRangeModel newValue = (BoundedRangeModel)e.getNewValue();
                if (newValue != null) {
                    newValue.addChangeListener(hsbChangeListener);
                }
            } else if (StringConstants.COMPONENT_ORIENTATION.equals(propertyName)) {
                hsbChangeListener.stateChanged(new ChangeEvent(e.getSource()));
            }
        }
    }

    private class VSBPropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if (StringConstants.MODEL_PROPERTY_CHANGED.equals(propertyName)) {
                BoundedRangeModel oldValue = (BoundedRangeModel)e.getOldValue();
                if (oldValue != null) {
                    oldValue.removeChangeListener(vsbChangeListener);
                }

                BoundedRangeModel newValue = (BoundedRangeModel)e.getNewValue();
                if (newValue != null) {
                    newValue.addChangeListener(vsbChangeListener);
                }
            }
        }
    }

    protected JScrollPane scrollpane;
    protected ChangeListener hsbChangeListener;
    protected ChangeListener vsbChangeListener;
    protected ChangeListener viewportChangeListener;
    protected PropertyChangeListener spPropertyChangeListener;
    private PropertyChangeListener hsbPropertyChangeListener;
    private PropertyChangeListener vsbPropertyChangeListener;

    private MouseWheelListener mouseWheelListener;


    public static ComponentUI createUI(final JComponent x) {
        return new BasicScrollPaneUI();
    }

    public void paint(final Graphics g, final JComponent c) {
        Border viewportBorder = scrollpane.getViewportBorder();
        if (viewportBorder != null) {
            Rectangle borderBounds = scrollpane.getViewportBorderBounds();
            viewportBorder.paintBorder(scrollpane, g, borderBounds.x, borderBounds.y, borderBounds.width, borderBounds.height);
        }
    }

    public Dimension getMaximumSize(final JComponent c) {
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    public void installUI(final JComponent c) {
        scrollpane = (JScrollPane)c;
        installDefaults(scrollpane);
        installListeners(scrollpane);
        installKeyboardActions(scrollpane);

        scrollpane.setLayout(new ScrollPaneLayout());
    }

    public void uninstallUI(final JComponent c) {
        uninstallKeyboardActions(scrollpane);
        uninstallListeners(scrollpane);
        uninstallDefaults(scrollpane);

        scrollpane.setLayout(null);
        scrollpane = null;
    }



    protected void installDefaults(final JScrollPane scrollPane) {
        LookAndFeel.installProperty(scrollpane, "opaque", Boolean.TRUE);
        LookAndFeel.installBorder(scrollPane, "ScrollPane.border");
        LookAndFeel.installColorsAndFont(scrollPane, "ScrollPane.background", "ScrollPane.foreground", "ScrollPane.font");
    }

    protected void uninstallDefaults(final JScrollPane scrollPane) {
        LookAndFeel.uninstallBorder(scrollPane);
        Utilities.uninstallColorsAndFont(scrollPane);
    }

    protected void installListeners(final JScrollPane scrollPane) {
        vsbChangeListener = createVSBChangeListener();
        if (scrollpane.getVerticalScrollBar() != null) {
            scrollpane.getVerticalScrollBar().getModel().addChangeListener(vsbChangeListener);
        }

        hsbChangeListener = createHSBChangeListener();
        if (scrollpane.getHorizontalScrollBar() != null) {
            scrollpane.getHorizontalScrollBar().getModel().addChangeListener(hsbChangeListener);
        }

        viewportChangeListener = createViewportChangeListener();
        if (scrollpane.getViewport() != null) {
            scrollpane.getViewport().addChangeListener(viewportChangeListener);
        }

        spPropertyChangeListener = createPropertyChangeListener();
        scrollpane.addPropertyChangeListener(spPropertyChangeListener);

        hsbPropertyChangeListener = new HSBPropertyChangeHandler();
        if (scrollpane.getHorizontalScrollBar() != null) {
            scrollpane.getHorizontalScrollBar().addPropertyChangeListener(hsbPropertyChangeListener);
        }

        vsbPropertyChangeListener = new VSBPropertyChangeHandler();
        if (scrollpane.getVerticalScrollBar() != null) {
            scrollpane.getVerticalScrollBar().addPropertyChangeListener(vsbPropertyChangeListener);
        }

        mouseWheelListener = createMouseWheelListener();
        scrollpane.addMouseWheelListener(mouseWheelListener);
    }

    protected void uninstallListeners(final JComponent c) {
        if (scrollpane.getVerticalScrollBar() != null) {
            scrollpane.getVerticalScrollBar().getModel().removeChangeListener(vsbChangeListener);
        }
        vsbChangeListener = null;

        if (scrollpane.getHorizontalScrollBar() != null) {
            scrollpane.getHorizontalScrollBar().getModel().removeChangeListener(hsbChangeListener);
        }
        hsbChangeListener = null;

        if (scrollpane.getViewport() != null) {
            scrollpane.getViewport().removeChangeListener(viewportChangeListener);
        }
        viewportChangeListener = null;

        scrollpane.removePropertyChangeListener(spPropertyChangeListener);
        spPropertyChangeListener = null;

        scrollpane.removeMouseWheelListener(mouseWheelListener);
        mouseWheelListener = null;
    }

    protected void installKeyboardActions(final JScrollPane scrollPane) {
        BasicScrollPaneKeyboardActions.installKeyboardActions(scrollPane);
    }

    protected void uninstallKeyboardActions(final JScrollPane scrollPane) {
        BasicScrollPaneKeyboardActions.uninstallKeyboardActions(scrollPane);
    }

    protected ChangeListener createViewportChangeListener() {
        return new ViewportChangeHandler();
    }

    protected ChangeListener createHSBChangeListener() {
        return new HSBChangeListener();
    }

    protected ChangeListener createVSBChangeListener() {
        return new VSBChangeListener();
    }

    protected MouseWheelListener createMouseWheelListener() {
        return new MouseWheelHandler();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    protected void syncScrollPaneWithViewport() {
        JViewport viewport = scrollpane.getViewport();
        Point viewPosition = viewport.getViewPosition();
        Dimension extentSize = viewport.getExtentSize();
        Dimension maximumSize = viewport.getViewSize();
        if (scrollpane.getVerticalScrollBar() != null) {
            JScrollBar vsb = scrollpane.getVerticalScrollBar();
            vsb.setValues(viewPosition.y, extentSize.height, 0, maximumSize.height);
        }
        if (scrollpane.getHorizontalScrollBar() != null) {
            JScrollBar hsb = scrollpane.getHorizontalScrollBar();
            int newValue = getHorizontalPosition(viewport, viewPosition.x, hsb.getComponentOrientation().isLeftToRight());
            hsb.setValues(newValue, extentSize.width, 0, maximumSize.width);
        }
    }

    protected void updateScrollBarDisplayPolicy(final PropertyChangeEvent e) {
        String changedProperty = e.getPropertyName();
        if (StringConstants.VERTICAL_SCROLLBAR_PROPERTY.equals(changedProperty)) {
            JScrollBar oldValue = (JScrollBar)e.getOldValue();
            if (oldValue != null) {
                oldValue.getModel().removeChangeListener(vsbChangeListener);
                oldValue.removePropertyChangeListener(vsbPropertyChangeListener);
            }
            JScrollBar newValue = (JScrollBar)e.getNewValue();
            if (newValue != null) {
                newValue.getModel().addChangeListener(vsbChangeListener);
                newValue.addPropertyChangeListener(vsbPropertyChangeListener);
            }
        } else if (StringConstants.HORIZONTAL_SCROLLBAR_PROPERTY.equals(changedProperty)) {
            JScrollBar oldValue = (JScrollBar)e.getOldValue();
            if (oldValue != null) {
                oldValue.getModel().removeChangeListener(hsbChangeListener);
                oldValue.removePropertyChangeListener(hsbPropertyChangeListener);
            }
            JScrollBar newValue = (JScrollBar)e.getNewValue();
            if (newValue != null) {
                newValue.getModel().addChangeListener(hsbChangeListener);
                newValue.addPropertyChangeListener(hsbPropertyChangeListener);
            }
        } else if (StringConstants.HORIZONTAL_SCROLLBAR_POLICY_PROPERTY.equals(changedProperty)) {
            ((ScrollPaneLayout)scrollpane.getLayout()).setHorizontalScrollBarPolicy(((Integer)e.getNewValue()).intValue());
        } else if (StringConstants.VERTICAL_SCROLLBAR_POLICY_PROPERTY.equals(changedProperty)) {
            ((ScrollPaneLayout)scrollpane.getLayout()).setVerticalScrollBarPolicy(((Integer)e.getNewValue()).intValue());
        }
    }

    protected void updateViewport(final PropertyChangeEvent e) {
        JViewport oldValue = (JViewport)e.getOldValue();
        if (oldValue != null) {
            oldValue.removeChangeListener(viewportChangeListener);
        }

        JViewport newValue = (JViewport)e.getNewValue();
        if (newValue != null) {
            newValue.addChangeListener(viewportChangeListener);
            if (oldValue != null) {
                newValue.setViewPosition(oldValue.getViewPosition());
            } else {

            }
            syncUpColumnHeader();
            syncUpRowHeader();
        }
    }

    protected void updateRowHeader(final PropertyChangeEvent e) {
        syncUpRowHeader();
    }

    protected void updateColumnHeader(final PropertyChangeEvent e) {
        syncUpColumnHeader();
    }

    private int getHorizontalPosition(final JViewport viewport, final int value, final boolean isLeftToRight) {
        return isLeftToRight ? value : viewport.getViewSize().width - viewport.getExtentSize().width - value;
    }

    private void syncUpColumnHeader() {
        if (scrollpane.getColumnHeader() != null && scrollpane.getViewport() != null) {
            scrollpane.getColumnHeader().setViewPosition(new Point(scrollpane.getViewport().getViewPosition().x, scrollpane.getColumnHeader().getViewPosition().y));
        }
    }

    private void syncUpRowHeader() {
        if (scrollpane.getRowHeader() != null && scrollpane.getViewport() != null) {
            scrollpane.getRowHeader().setViewPosition(new Point(scrollpane.getRowHeader().getViewPosition().x, scrollpane.getViewport().getViewPosition().y));
        }
    }

}
