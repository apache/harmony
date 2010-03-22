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
package javax.swing.plaf.basic;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SplitPaneUI;

import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class BasicSplitPaneUI extends SplitPaneUI {

    public class BasicHorizontalLayoutManager implements LayoutManager2 {
        final int COMPONENTS_COUNT = 3;
        final int LEFT_COMPONENT_INDEX = 0;
        final int RIGHT_COMPONENT_INDEX = 1;
        final int DIVIDER_INDEX = 2;

        protected int[] sizes = new int[3];
        protected Component[] components = new Component[COMPONENTS_COUNT];

        private BasicHorizontalLayoutManager(final BasicSplitPaneUI ui) {
        }

        public void layoutContainer(final Container container) {
            Insets insets = container.getInsets();
            
            if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                calculateNewDividerLocation(container.getWidth(), insets.left, insets.right);

                int divX = dividerLocation;
                int divY = insets.top;
                int divWidth = dividerSize;

                int leftCompX = insets.left;
                int leftCompY = insets.top;
                int leftCompWidth = divX - insets.left;

                int rightCompX = divX + divWidth;
                int rightCompY = leftCompY;
                int rightCompWidth = container.getWidth() - leftCompWidth - divWidth - insets.left - insets.right;
                
                if ((components[RIGHT_COMPONENT_INDEX] != null)
                        && (components[LEFT_COMPONENT_INDEX] != null)
                        && rightCompWidth < components[RIGHT_COMPONENT_INDEX].getMinimumSize().width
                        && leftCompWidth > components[LEFT_COMPONENT_INDEX].getMinimumSize().width) {
                    
                    rightCompWidth = components[RIGHT_COMPONENT_INDEX].getMinimumSize().width;
                    leftCompWidth = container.getWidth() - rightCompWidth - divWidth - insets.left - insets.right;
                    
                    if (leftCompWidth < components[LEFT_COMPONENT_INDEX].getMinimumSize().width) {
                        leftCompWidth = components[LEFT_COMPONENT_INDEX].getMinimumSize().width;
                        rightCompWidth = container.getWidth() - leftCompWidth - divWidth - insets.left - insets.right;
                    }

                    dividerLocation = leftCompWidth + insets.left;
                    if (dividerLocation >= insets.left && dividerLocation != splitPane.getDividerLocation() && isDisplayed) {
                        splitPane.setDividerLocation(dividerLocation);
                    }
                }

                int height = container.getHeight() - insets.top - insets.bottom;

                if (components[LEFT_COMPONENT_INDEX] != null) {
                    components[LEFT_COMPONENT_INDEX].setBounds(leftCompX, leftCompY, leftCompWidth, height);
                    sizes[LEFT_COMPONENT_INDEX] = leftCompWidth;
                }
                if (components[DIVIDER_INDEX] != null) {
                    components[DIVIDER_INDEX].setBounds(divX, divY, divWidth, height);
                    sizes[DIVIDER_INDEX] = divWidth;
                }
                if (components[RIGHT_COMPONENT_INDEX] != null) {
                    components[RIGHT_COMPONENT_INDEX].setBounds(rightCompX, rightCompY, rightCompWidth, height);
                    sizes[RIGHT_COMPONENT_INDEX] = rightCompWidth;
                }
            } else {
                calculateNewDividerLocation(container.getHeight(), insets.top, insets.bottom);

                int divX = insets.left;
                int divY = dividerLocation;
                int divHeight = dividerSize;

                int leftCompX = insets.left;
                int leftCompY = insets.top;
                int leftCompHeight = divY - insets.top;

                int rightCompX = insets.left;
                int rightCompY = divY + divHeight;
                int rightCompHeight = container.getHeight() - leftCompHeight - divHeight - insets.top - insets.bottom;
                
                if ((components[RIGHT_COMPONENT_INDEX] != null)
                        && (components[LEFT_COMPONENT_INDEX] != null)
                        && rightCompHeight < components[RIGHT_COMPONENT_INDEX]
                                .getMinimumSize().height
                        && leftCompHeight > components[LEFT_COMPONENT_INDEX]
                                .getMinimumSize().height) {
                    
                    rightCompHeight = components[RIGHT_COMPONENT_INDEX].getMinimumSize().height;
                    leftCompHeight = container.getHeight() - rightCompHeight - divHeight - insets.top - insets.bottom;
                    
                    if (leftCompHeight < components[LEFT_COMPONENT_INDEX].getMinimumSize().height) {
                        leftCompHeight = components[LEFT_COMPONENT_INDEX].getMinimumSize().height;
                        rightCompHeight = container.getHeight() - leftCompHeight - divHeight - insets.top - insets.bottom;
                    }
                    
                    dividerLocation = leftCompHeight + insets.top;
                    if (dividerLocation >= insets.top && dividerLocation != splitPane.getDividerLocation() && isDisplayed) {
                        splitPane.setDividerLocation(dividerLocation);
                    }
                }
                
                int width = container.getWidth() - insets.left - insets.right;
                if (components[LEFT_COMPONENT_INDEX] != null) {
                    components[LEFT_COMPONENT_INDEX].setBounds(leftCompX, leftCompY, width, leftCompHeight);
                    sizes[LEFT_COMPONENT_INDEX] = leftCompHeight;
                }
                if (components[DIVIDER_INDEX] != null) {
                    components[DIVIDER_INDEX].setBounds(divX, divY, width, divHeight);
                    sizes[DIVIDER_INDEX] = divHeight;
                }
                if (components[RIGHT_COMPONENT_INDEX] != null) {
                    components[RIGHT_COMPONENT_INDEX].setBounds(rightCompX, rightCompY, width, rightCompHeight);
                    sizes[RIGHT_COMPONENT_INDEX] = rightCompHeight;
                }
            }
        }

        public void addLayoutComponent(final String place, final Component component) {
            if (place != null && !JSplitPane.TOP.equals(place)
                                  && !JSplitPane.LEFT.equals(place)
                                  && !JSplitPane.BOTTOM.equals(place)
                                  && !JSplitPane.RIGHT.equals(place)
                                  && !JSplitPane.DIVIDER.equals(place)) {
                throw new IllegalArgumentException(Messages.getString("swing.73")); //$NON-NLS-1$
            }
            if (JSplitPane.DIVIDER.equals(place)) {
                components[DIVIDER_INDEX] = component;
                return;
            }
            if (JSplitPane.TOP.equals(place) || JSplitPane.LEFT.equals(place)) {
                components[LEFT_COMPONENT_INDEX] = component;
                return;
            }
            if (JSplitPane.BOTTOM.equals(place) || JSplitPane.RIGHT.equals(place)) {
                components[RIGHT_COMPONENT_INDEX] = component;
            }
            
            //resetToPreferredSizes();
        }

        public Dimension minimumLayoutSize(final Container container) {
            int width;
            int height;
            Insets insets = splitPane.getInsets();
            if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                width = insets.left + insets.right
                        + componentMinWidth(LEFT_COMPONENT_INDEX)
                        + componentMinWidth(RIGHT_COMPONENT_INDEX)
                        + sizes[DIVIDER_INDEX];
                height = Math.max(componentMinHeight(LEFT_COMPONENT_INDEX),
                                  componentMinHeight(RIGHT_COMPONENT_INDEX));
                height = Math.max(height, sizes[DIVIDER_INDEX])
                         + insets.top + insets.bottom;
            } else {
                width = Math.max(componentMinWidth(LEFT_COMPONENT_INDEX),
                        componentMinWidth(RIGHT_COMPONENT_INDEX));
                width = Math.max(width, sizes[DIVIDER_INDEX])
                        + insets.left + insets.right;
                height = insets.top + insets.bottom
                        + componentMinHeight(LEFT_COMPONENT_INDEX)
                        + componentMinHeight(RIGHT_COMPONENT_INDEX)
                        + sizes[DIVIDER_INDEX];
            }
            return new Dimension(width, height);
        }

        public Dimension preferredLayoutSize(final Container container) {
            int width;
            int height;
            Insets insets = splitPane.getInsets();
            if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                width = insets.left + insets.right
                        + componentPrefWidth(LEFT_COMPONENT_INDEX)
                        + componentPrefWidth(RIGHT_COMPONENT_INDEX)
                        + dividerSize;
                height = Math.max(componentPrefHeight(LEFT_COMPONENT_INDEX),
                                  componentPrefHeight(RIGHT_COMPONENT_INDEX));
                height = Math.max(height, sizes[DIVIDER_INDEX])
                         + insets.top + insets.bottom;
            } else {
                width = Math.max(componentPrefWidth(LEFT_COMPONENT_INDEX),
                        componentPrefWidth(RIGHT_COMPONENT_INDEX));
                width = Math.max(width, sizes[DIVIDER_INDEX])
                        + insets.left + insets.right;
                height = insets.top + insets.bottom
                        + componentPrefHeight(LEFT_COMPONENT_INDEX)
                        + componentPrefHeight(RIGHT_COMPONENT_INDEX)
                        + dividerSize;
            }
            return new Dimension(width, height);
        }

        public void removeLayoutComponent(final Component component) {
            for (int i = 0; i < COMPONENTS_COUNT; i++) {
                if (components[i] == component) {
                    components[i] = null;
                }
            }
        }

        public void addLayoutComponent(final Component comp, final Object constraints) {
            if (!(constraints instanceof String)) {
                throw new IllegalArgumentException(Messages.getString("swing.74")); //$NON-NLS-1$
            }
            addLayoutComponent((String)constraints, comp);
        }

        public float getLayoutAlignmentX(final Container target) {
            return 0;
        }

        public float getLayoutAlignmentY(final Container target) {
            return 0;
        }

        public void invalidateLayout(final Container c) {
        }

        public Dimension maximumLayoutSize(final Container target) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        public void resetToPreferredSizes() {
            if (components[LEFT_COMPONENT_INDEX] == null) {
                return;
            }
            
            int prefLocation = getPreferredSizeOfComponent(components[LEFT_COMPONENT_INDEX])
                              + getInitialLocation(splitPane.getInsets());
            int minLocation = getMinimumSizeOfComponent(components[LEFT_COMPONENT_INDEX])
                              + getInitialLocation(splitPane.getInsets());
            dividerLocation = prefLocation > minLocation ? prefLocation : minLocation;
            splitPane.setDividerLocation(dividerLocation);
        }

        protected void resetSizeAt(final int index) {
            if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                sizes[index] = components[index] == null ? 0 : components[index].getMinimumSize().width;
            } else {
                sizes[index] = components[index] == null ? 0 : components[index].getMinimumSize().height;
            }
        }

        protected void setSizes(final int[] newSizes) {
            System.arraycopy(sizes, 0, newSizes, 0, sizes.length);
        }

        protected int[] getSizes() {
            int[] result = new int[COMPONENTS_COUNT];
            System.arraycopy(result, 0, sizes, 0, sizes.length);

            return result;
        }

        protected int getPreferredSizeOfComponent(final Component c) {
            if (c == null) {
                return 0;
            }

            if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                return c.getPreferredSize().width;
            } else {
                return c.getPreferredSize().height;
            }
        }

        protected int getSizeOfComponent(final Component c) {
            if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                return c.getSize().width;
            } else {
                return c.getSize().height;
            }
        }

        protected int getAvailableSize(final Dimension containerSize, final Insets insets) {
            if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                return containerSize.width - insets.left - insets.right;
            } else {
                return containerSize.height - insets.top - insets.bottom;
            }
        }

        protected int getInitialLocation(final Insets insets) {
            if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                return insets.left;
            } else {
                return insets.top;
            }
        }

        protected void setComponentToSize(final Component c, final int size, final int location,
                                          final Insets insets, final Dimension containerSize) {
            if (c == null) {
                return;
            }
            if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                c.setBounds(location, insets.top, size, containerSize.height - insets.top - insets.bottom);
            } else {
                c.setBounds(insets.left, location, containerSize.width - insets.left - insets.right, size);
            }
        }

        protected void updateComponents() {
            components[LEFT_COMPONENT_INDEX] = splitPane.getLeftComponent();
            components[RIGHT_COMPONENT_INDEX] = splitPane.getRightComponent();
            components[DIVIDER_INDEX] = getDivider();
        }

        int componentPrefWidth(final int index) {
            return components[index] == null ? 0 : components[index].getPreferredSize().width;
        }

        int componentPrefHeight(final int index) {
            return components[index] == null ? 0 : components[index].getPreferredSize().height;
        }

        private int componentMinWidth(final int index) {
            return components[index] == null ? 0 : components[index].getMinimumSize().width;
        }

        private int componentMinHeight(final int index) {
            return components[index] == null ? 0 : components[index].getMinimumSize().height;
        }

        private void calculateNewDividerLocation(final int containerSize, final int topLeft, final int bottomRight) {
            if (dividerLocation != splitPane.getDividerLocation()) {
                if (splitPane.getDividerLocation() < 0 || splitPane.getDividerLocation() < topLeft) {
                    dividerLocation = topLeft;
                } else if (splitPane.getDividerLocation() > containerSize - bottomRight - dividerSize) {
                    dividerLocation = containerSize - bottomRight - dividerSize;
                } else {
                    dividerLocation = splitPane.getDividerLocation();
                }
                
                if (dividerLocation >= topLeft && dividerLocation != splitPane.getDividerLocation() && isDisplayed) {
                    splitPane.setDividerLocation(dividerLocation);
                }
            }
            if ((components[RIGHT_COMPONENT_INDEX] != null) && (components[LEFT_COMPONENT_INDEX] == null)) {
                dividerLocation = topLeft;
                if (dividerLocation != splitPane.getDividerLocation() && isDisplayed) {
                    splitPane.setDividerLocation(dividerLocation);
                }
            }            
            if ((components[RIGHT_COMPONENT_INDEX] == null) && (components[LEFT_COMPONENT_INDEX] != null)) {
                dividerLocation = containerSize - bottomRight - dividerSize;
                if (dividerLocation != splitPane.getDividerLocation() && isDisplayed) {
                    splitPane.setDividerLocation(dividerLocation);
                }
            }            
        }
    }

    public class BasicVerticalLayoutManager extends BasicHorizontalLayoutManager {
        public BasicVerticalLayoutManager() {
            super(BasicSplitPaneUI.this);
        }
    }

    public class FocusHandler extends FocusAdapter {
        @Override
        public void focusGained(final FocusEvent ev) {
            divider.repaint();
        }

        @Override
        public void focusLost(final FocusEvent ev) {
            divider.repaint();
        }
    }

    public class KeyboardDownRightHandler implements ActionListener {
        public void actionPerformed(final ActionEvent ev) {
        }
    }

    public class KeyboardEndHandler implements ActionListener {
        public void actionPerformed(final ActionEvent ev) {
        }
    }

    public class KeyboardHomeHandler implements ActionListener {
        public void actionPerformed(final ActionEvent ev) {
        }
    }

    public class KeyboardResizeToggleHandler implements ActionListener {
        public void actionPerformed(final ActionEvent ev) {
        }
    }

    public class KeyboardUpLeftHandler implements ActionListener {
        public void actionPerformed(final ActionEvent ev) {
        }
    }

    public class PropertyHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent e) {
            if (JSplitPane.CONTINUOUS_LAYOUT_PROPERTY.equals(e.getPropertyName())) {
                setContinuousLayout(((Boolean)e.getNewValue()).booleanValue());
            } else if (JSplitPane.ORIENTATION_PROPERTY.equals(e.getPropertyName())) {
                setOrientation(((Integer)e.getNewValue()).intValue());
            } else if (JSplitPane.DIVIDER_SIZE_PROPERTY.equals(e.getPropertyName())) {
                dividerSize = ((Integer)e.getNewValue()).intValue();
                divider.setDividerSize(dividerSize);
                layoutManager.sizes[layoutManager.DIVIDER_INDEX] = dividerSize;
            }
            splitPane.revalidate();
        }
    }

    protected static final String NON_CONTINUOUS_DIVIDER = "nonContinuousDivider";
    protected static int KEYBOARD_DIVIDER_MOVE_OFFSET;
    protected JSplitPane splitPane;
    protected BasicHorizontalLayoutManager layoutManager;
    protected BasicSplitPaneDivider divider;
    protected PropertyChangeListener propertyChangeListener;
    protected FocusListener focusListener;
    protected int dividerSize;
    protected Component nonContinuousLayoutDivider;
    protected boolean draggingHW;
    protected int beginDragDividerLocation;
    /**
     * @deprecated
     */
    protected KeyStroke upKey;
    /**
     * @deprecated
     */
    protected KeyStroke downKey;
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
    protected KeyStroke homeKey;
    /**
     * @deprecated
     */
    protected KeyStroke endKey;
    /**
     * @deprecated
     */
    protected KeyStroke dividerResizeToggleKey;
    /**
     * @deprecated
     */
    protected ActionListener keyboardUpLeftListener;
    /**
     * @deprecated
     */
    protected ActionListener keyboardDownRightListener;
    /**
     * @deprecated
     */
    protected ActionListener keyboardHomeListener;
    /**
     * @deprecated
     */
    protected ActionListener keyboardEndListener;
    /**
     * @deprecated
     */
    protected ActionListener keyboardResizeToggleListener;

    private boolean isContinuousLayout;
    private int orientation;
    private int lastDragLocation;
    private int dividerLocation;
    private boolean isDisplayed;

    public static ComponentUI createUI(final JComponent c) {
        return new BasicSplitPaneUI();
    }

    @Override
    public void installUI(final JComponent c) {
        splitPane = (JSplitPane)c;

        setOrientation(splitPane.getOrientation());
        setContinuousLayout(splitPane.isContinuousLayout());

        resetLayoutManager();

        installDefaults();
        installListeners();
        installKeyboardActions();

        divider.oneTouchExpandableChanged();
        resetToPreferredSizes(getSplitPane());

        setLastDragLocation(-1);
    }

    protected void installDefaults() {
        LookAndFeel.installProperty(splitPane, "opaque", Boolean.TRUE);

        Color backgroundColor = splitPane.getBackground();
        if (Utilities.isUIResource(backgroundColor)) {
            splitPane.setForeground(UIManager.getColor("SplitPane.background"));
        }

        if (splitPane.getLeftComponent() != null) {
            layoutManager.addLayoutComponent(splitPane.getLeftComponent(), JSplitPane.LEFT);
        }
        if (splitPane.getRightComponent() != null) {
            layoutManager.addLayoutComponent(splitPane.getRightComponent(), JSplitPane.RIGHT);
        }

        dividerSize = UIManager.getInt("SplitPane.dividerSize");
        divider = createDefaultDivider();
        divider.setDividerSize(dividerSize);
        LookAndFeel.installBorder(splitPane, "SplitPane.border");
        splitPane.setDividerSize(dividerSize);

        setNonContinuousLayoutDivider(createDefaultNonContinuousLayoutDivider());

        splitPane.add(divider, JSplitPane.DIVIDER);
    }

    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        splitPane.addPropertyChangeListener(propertyChangeListener);

        focusListener = createFocusListener();
        splitPane.addFocusListener(focusListener);
    }

    protected void installKeyboardActions() {
        BasicSplitPaneKeyboardActions.installKeyboardActions(splitPane);
    }

    @Override
    public void uninstallUI(final JComponent c) {
        splitPane = (JSplitPane)c;

        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();
    }

    protected void uninstallDefaults() {
        Utilities.uninstallColorsAndFont(splitPane);
    }

    protected void uninstallKeyboardActions() {
        Utilities.uninstallKeyboardActions(splitPane, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    protected void uninstallListeners() {
        splitPane.removePropertyChangeListener(propertyChangeListener);
        propertyChangeListener = null;

        splitPane.removeFocusListener(focusListener);
        focusListener = null;
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyHandler();
    }

    protected FocusListener createFocusListener() {
        return new FocusHandler();
    }

    /**
     * @deprecated
     */
    protected ActionListener createKeyboardUpLeftListener() {
        return new KeyboardUpLeftHandler();
    }
    /**
     * @deprecated
     */
    protected ActionListener createKeyboardDownRightListener() {
        return new KeyboardDownRightHandler();
    }
    /**
     * @deprecated
     */
    protected ActionListener createKeyboardHomeListener() {
        return new KeyboardHomeHandler();
    }
    /**
     * @deprecated
     */
    protected ActionListener createKeyboardEndListener() {
        return new KeyboardEndHandler();
    }
    /**
     * @deprecated
     */
    protected ActionListener createKeyboardResizeToggleListener() {
        return new KeyboardResizeToggleHandler();
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(final int orientation) {
        this.orientation = orientation;
    }

    public boolean isContinuousLayout() {
        return isContinuousLayout;
    }

    public void setContinuousLayout(final boolean b) {
        isContinuousLayout = b;
    }

    public int getLastDragLocation() {
        return lastDragLocation ;
    }

    public void setLastDragLocation(final int l) {
        lastDragLocation = l;
    }

    public BasicSplitPaneDivider getDivider() {
        return divider;
    }

    protected Component createDefaultNonContinuousLayoutDivider() {
        return new Canvas() {
            // Note: this is not a guaratee for correct serialization/deserialization
            // but rather a performance optimization
            private static final long serialVersionUID = 1L;

            @Override
            public void paint(final Graphics g) {
                Color oldColor = g.getColor();
                g.setColor(Color.DARK_GRAY);

                int x;
                int y;
                int w;
                int h;
                if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                    x = lastDragLocation;
                    y = 0;
                    w = dividerSize;
                    h = splitPane.getHeight();
                } else {
                    x = 0;
                    y = lastDragLocation;
                    w = splitPane.getWidth();
                    h = dividerSize;
                }

                g.fillRect(x, y, w, h);

                g.setColor(oldColor);
            }
        };
    }

    protected void setNonContinuousLayoutDivider(final Component newDivider) {
        setNonContinuousLayoutDivider(newDivider, true);
    }

    protected void setNonContinuousLayoutDivider(final Component newDivider, final boolean rememberSizes) {
        nonContinuousLayoutDivider = newDivider;
    }

    public Component getNonContinuousLayoutDivider() {
        return nonContinuousLayoutDivider;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    public BasicSplitPaneDivider createDefaultDivider() {
        return new BasicSplitPaneDivider(this);
    }

    @Override
    public void resetToPreferredSizes(final JSplitPane jc) {
        layoutManager.resetToPreferredSizes();
        splitPane.repaint();
    }

    @Override
    public void setDividerLocation(final JSplitPane jc, final int location) {
        splitPane.revalidate();
    }

    @Override
    public int getDividerLocation(final JSplitPane jc) {
        if (jc == null) { // Fix for HARMONY-2661, for compatibility with RI
            throw new NullPointerException(Messages.getString("swing.03","jc")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return dividerLocation;
    }

    @Override
    public int getMinimumDividerLocation(final JSplitPane jc) {
        if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            return jc.getLeftComponent() == null ? 0 : jc.getLeftComponent().getMinimumSize().width + jc.getInsets().left;
        } else {
            return jc.getLeftComponent() == null ? 0 : jc.getLeftComponent().getMinimumSize().height + jc.getInsets().top;
        }
    }

    @Override
    public int getMaximumDividerLocation(final JSplitPane jc) {
        Insets insets = jc.getInsets();
        if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            return jc.getRightComponent() == null
                    ? jc.getWidth() - dividerSize - insets.right
                    : jc.getWidth() - dividerSize - jc.getRightComponent().getMinimumSize().width - insets.right;
        } else {
            return jc.getRightComponent() == null
                    ? jc.getHeight() - dividerSize - insets.bottom
                    : jc.getHeight() - dividerSize - jc.getRightComponent().getMinimumSize().height - insets.bottom;
        }
    }

    @Override
    public void finishedPaintingChildren(final JSplitPane jc, final Graphics g) {
        g.setClip(0, 0, jc.getWidth(), jc.getHeight());
        if (!isContinuousLayout() && lastDragLocation != -1) {
            nonContinuousLayoutDivider.paint(g);
        }
    }

    @Override
    public void paint(final Graphics g, final JComponent jc) {
        isDisplayed = true;
    }

    @Override
    public Dimension getPreferredSize(final JComponent jc) {
        return ((layoutManager != null)
                ? layoutManager.preferredLayoutSize(jc)
                : new Dimension(0, 0));
    }

    @Override
    public Dimension getMinimumSize(final JComponent jc) {
        return ((layoutManager != null)
                ? layoutManager.minimumLayoutSize(jc)
                : new Dimension(0, 0));
    }

    @Override
    public Dimension getMaximumSize(final JComponent jc) {
        return ((layoutManager != null)
                ? layoutManager.maximumLayoutSize(jc)
                : new Dimension(0, 0));
    }

    public Insets getInsets(final JComponent jc) {
        return jc.getBorder().getBorderInsets(jc);
    }

    protected void resetLayoutManager() {
        if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            layoutManager = new BasicHorizontalLayoutManager(this);
        } else {
            layoutManager = new BasicVerticalLayoutManager();
        }
        splitPane.setLayout(layoutManager);
    }

    protected void startDragging() {
        beginDragDividerLocation = splitPane.getDividerLocation();
        lastDragLocation = splitPane.getDividerLocation();
        splitPane.repaint();
    }

    protected void dragDividerTo(final int location) {
        if (isContinuousLayout()) {
            layoutManager.layoutContainer(splitPane);
        } else {
            lastDragLocation = location;
            splitPane.repaint();
        }
    }

    protected void finishDraggingTo(final int location) {
        splitPane.setDividerLocation(location);
        layoutManager.layoutContainer(splitPane);
        splitPane.repaint();
        lastDragLocation = -1;
    }

    /**
     * @deprecated
     */
    protected int getDividerBorderSize() {
        return 0;
    }

    private int getMinimumSizeOfComponent(final Component c) {
        if (c == null) {
            return 0;
        }

        if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            return c.getMinimumSize().width;
        } else {
            return c.getMinimumSize().height;
        }
    }
}
