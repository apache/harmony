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
 * @author Anton Avtamonov
 */

package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.AccessibleContext;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class BasicComboPopup extends JPopupMenu implements ComboPopup {
    /**
     * This class is replaced with ActionMap/InputMap functionality and does nothing
     */
    public class InvocationKeyHandler extends KeyAdapter {
        public void keyReleased(final KeyEvent e) {
        }
    }

    /**
     * Is obsolete and is not used. Replaced with item listener
     *
     */
    public class ListDataHandler implements ListDataListener {
        public void intervalAdded(final ListDataEvent e) {
        }

        public void intervalRemoved(final ListDataEvent e) {
        }

        public void contentsChanged(final ListDataEvent e) {
        }
    }

    /**
     * Is obsolete and is not used.
     *
     */
    protected class ListSelectionHandler implements ListSelectionListener {
        public void valueChanged(final ListSelectionEvent e) {
        }
    }

    protected class InvocationMouseHandler extends MouseAdapter {
        private boolean mouseInside(final MouseEvent event) {
            return event.getComponent().contains(event.getPoint());
        }

        public void mousePressed(final MouseEvent e) {
            if ((e.getButton() != MouseEvent.BUTTON1) || !mouseInside(e)) {
                return;
            }
            if (!comboBox.isEnabled()) {
                return;
            }
            delegateFocus(e);
            togglePopup();
        }

        public void mouseReleased(final MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) {
                return;
            }

            stopAutoScrolling();
            if (!isShowing()) {
                return;
            }

            if (hasEntered) {
                comboBox.setSelectedIndex(list.getSelectedIndex());
            }

            if (hasEntered || leftComboBox) {
                comboBox.setPopupVisible(false);
                hasEntered = false;
                leftComboBox = false;
            }
        }
    }

    protected class InvocationMouseMotionHandler extends MouseMotionAdapter {
        public void mouseDragged(final MouseEvent e) {
            if (!comboBox.isEnabled()) {
                return;
            }
            Rectangle listVisibleRect = list.getVisibleRect();
            MouseEvent convertedEvent = convertMouseEvent(e);
            hasEntered = listVisibleRect.contains(convertedEvent.getPoint());
            if (!leftComboBox) {
                leftComboBox = e.getPoint().x < 0
                               || e.getPoint().x > comboBox.getWidth()
                               || e.getPoint().y < 0
                               || e.getPoint().y > comboBox.getHeight();
            }

            if (hasEntered) {
                updateListBoxSelectionForEvent(convertedEvent, false);
                stopAutoScrolling();
            } else if (leftComboBox) {
                if (convertedEvent.getPoint().y < listVisibleRect.y) {
                    startAutoScrolling(SCROLL_UP);
                } else if (convertedEvent.getPoint().y > listVisibleRect.y + list.getVisibleRect().height) {
                    startAutoScrolling(SCROLL_DOWN);
                } else {
                    stopAutoScrolling();
                }
            }
        }
    }

    protected class ItemHandler implements ItemListener {
        public void itemStateChanged(final ItemEvent e) {
            int selectedIndex = comboBox.getSelectedIndex();
            if (selectedIndex != -1) {
                list.setSelectedIndex(selectedIndex);
                list.ensureIndexIsVisible(selectedIndex);
            }
        }
    }

    protected class ListMouseHandler extends MouseAdapter {
        public void mousePressed(final MouseEvent e) {

        }

        public void mouseReleased(final MouseEvent e) {
            BasicListUI ui = (BasicListUI)list.getUI();
            if (!ui.extendedSupportEnabled || ui.isChoosable(list.getSelectedIndex())) {
                comboBox.setPopupVisible(false);
                comboBox.setSelectedIndex(list.getSelectedIndex());
            }
        }
    }

    protected class ListMouseMotionHandler extends MouseMotionAdapter {
        public void mouseMoved(final MouseEvent event) {
            updateListBoxSelectionForEvent(event, false);
        }
    }

    protected class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent event) {
            if (StringConstants.FONT_PROPERTY_CHANGED.equals(event.getPropertyName())) {
                list.setFont((Font)event.getNewValue());
            } else if (StringConstants.BACKGROUND_PROPERTY_CHANGED.equals(event.getPropertyName())) {
                list.setBackground((Color)event.getNewValue());
            } else if (StringConstants.FOREGROUND_PROPERTY_CHANGED.equals(event.getPropertyName())) {
                list.setForeground((Color)event.getNewValue());
            } else if (StringConstants.RENDERER_PROPERTY_CHANGED.equals(event.getPropertyName())) {
                list.setCellRenderer((ListCellRenderer)event.getNewValue());
            } else if (StringConstants.COMPONENT_ORIENTATION.equals(event.getPropertyName())) {
                applyComponentOrientation((ComponentOrientation)event.getNewValue());
            } else if (StringConstants.LIGHTWEIGHT_POPUP_ENABLED_PROPERTY_CHANGED.equals(event.getPropertyName())) {
                setLightWeightPopupEnabled(((Boolean)event.getNewValue()).booleanValue());
            }
        }
    }

    protected JComboBox comboBox;
    protected JList list;
    protected boolean hasEntered;
    protected boolean isAutoScrolling;
    protected int scrollDirection;
    protected JScrollPane scroller;
    protected boolean valueIsAdjusting;
    protected Timer autoscrollTimer;


    protected ItemListener itemListener;
    protected KeyListener keyListener;
    protected ListDataListener listDataListener;
    protected MouseListener listMouseListener;
    protected MouseMotionListener listMouseMotionListener;
    protected ListSelectionListener listSelectionListener;
    protected MouseListener mouseListener;
    protected MouseMotionListener mouseMotionListener;
    protected PropertyChangeListener propertyChangeListener;

    protected static final int SCROLL_UP = 0;
    protected static final int SCROLL_DOWN = 1;

    private boolean leftComboBox;
    private Insets cachedInsets = new Insets(0, 0, 0, 0);

    public BasicComboPopup(final JComboBox combo) {
        comboBox = combo;
        list = createList();
        configureList();

        scroller = createScroller();
        configureScroller();

        add(scroller);

        configurePopup();

        installComboBoxListeners();
        installComboBoxModelListeners(comboBox.getModel());
        installKeyboardActions();

        setBorder(BorderFactory.createLineBorder(Color.black, 1));
    }

    public void hide() {
        setVisible(false);
    }

    public void show() {
        if (isShowing()) {
            return;
        }
        leftComboBox = false;
        hasEntered = false;

        Point screenPosition = comboBox.getLocationOnScreen();
        Insets comboInsets = comboBox.getInsets(cachedInsets);
        Rectangle popupBounds = computePopupBounds(screenPosition.x + comboInsets.left, screenPosition.y, comboBox.getWidth() - comboInsets.left - comboInsets.right, getPopupHeightForRowCount(comboBox.getMaximumRowCount()));
        Point popupPosition = new Point(popupBounds.x, popupBounds.y);
        Insets popupInsets = getInsets();
        scroller.setPreferredSize(new Dimension(popupBounds.width - popupInsets.left - popupInsets.right, popupBounds.height));

        list.setSelectedIndex(comboBox.getSelectedIndex());

        setLocation(popupPosition.x, popupPosition.y);
        super.setVisible(true);

        int selectedIndex = list.getSelectedIndex();
        if (selectedIndex != -1) {
            list.ensureIndexIsVisible(selectedIndex);
        }
    }

    public JList getList() {
        return list;
    }

    public MouseListener getMouseListener() {
        if (mouseListener == null) {
            mouseListener = createMouseListener();
        }
        return mouseListener;
    }

    public MouseMotionListener getMouseMotionListener() {
        if (mouseMotionListener == null) {
            mouseMotionListener = createMouseMotionListener();
        }
        return mouseMotionListener;
    }

    public KeyListener getKeyListener() {
        return keyListener;
    }

    public void uninstallingUI() {
        uninstallKeyboardActions();
        uninstallComboBoxModelListeners(comboBox.getModel());
        uninstallComboBoxListeners();
    }

    public boolean isFocusTraversable() {
        return false;
    }

    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = super.getAccessibleContext();
            accessibleContext.setAccessibleParent(comboBox);
        }

        return accessibleContext;
    }


    protected void installComboBoxListeners() {
        itemListener = createItemListener();
        if (itemListener != null) {
            comboBox.addItemListener(itemListener);
        }

        propertyChangeListener = createPropertyChangeListener();
        if (propertyChangeListener != null) {
            comboBox.addPropertyChangeListener(propertyChangeListener);
        }
    }

    protected void installComboBoxModelListeners(final ComboBoxModel model) {
    }

    protected void uninstallComboBoxModelListeners(final ComboBoxModel model) {

    }

    protected void installKeyboardActions() {

    }


    protected void uninstallKeyboardActions() {

    }

    protected JList createList() {
        return new JList(comboBox.getModel());
    }

    protected void configureList() {
        installListListeners();
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFocusable(false);
        list.setBorder(null);
        list.setSelectionBackground(UIManager.getColor("ComboBox.selectionBackground"));
        list.setSelectionForeground(UIManager.getColor("ComboBox.selectionForeground"));
        list.setCellRenderer(comboBox.getRenderer());
        list.setBackground(comboBox.getBackground());
        list.setForeground(comboBox.getForeground());
        list.setFont(comboBox.getFont());
    }

    protected JScrollPane createScroller() {
        return new JScrollPane(list);
    }

    protected void configureScroller() {
        scroller.setFocusable(true);
        scroller.setBorder(null);
    }

    protected void configurePopup() {
        setInvoker(comboBox);
        putClientProperty(StringConstants.HIDE_ON_INVOKER_PRESSED_PROPERTY, Boolean.TRUE);
    }

    protected void installListListeners() {
        listDataListener = createListDataListener();
        if (listDataListener != null) {
            list.getModel().addListDataListener(listDataListener);
        }

        listMouseListener = createListMouseListener();
        if (listMouseListener != null) {
            list.addMouseListener(listMouseListener);
        }

        listMouseMotionListener = createListMouseMotionListener();
        if (listMouseMotionListener != null) {
            list.addMouseMotionListener(listMouseMotionListener);
        }

        listSelectionListener = createListSelectionListener();
        if (listSelectionListener != null) {
            list.addListSelectionListener(listSelectionListener);
        }
    }


    protected void firePopupMenuWillBecomeVisible() {
        super.firePopupMenuWillBecomeVisible();
        comboBox.firePopupMenuWillBecomeVisible();
    }

    protected void firePopupMenuWillBecomeInvisible() {
        super.firePopupMenuWillBecomeInvisible();
        comboBox.firePopupMenuWillBecomeInvisible();
    }

    protected void firePopupMenuCanceled() {
        super.firePopupMenuCanceled();
        comboBox.firePopupMenuCanceled();
    }


    protected MouseListener createMouseListener() {
        return new InvocationMouseHandler();
    }

    protected MouseMotionListener createMouseMotionListener() {
        return new InvocationMouseMotionHandler();
    }

    protected KeyListener createKeyListener() {
        return null;
    }

    protected ListSelectionListener createListSelectionListener() {
        return null;
    }

    protected ListDataListener createListDataListener() {
        return null;
    }

    protected MouseListener createListMouseListener() {
        return new ListMouseHandler();
    }

    protected MouseMotionListener createListMouseMotionListener() {
        return new ListMouseMotionHandler();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    protected ItemListener createItemListener() {
        return new ItemHandler();
    }


    protected void startAutoScrolling(final int direction) {
        if (list.getModel().getSize() == 0) {
            return;
        }
        scrollDirection = direction;
        if (!isAutoScrolling) {
            isAutoScrolling = true;
            getTimer().start();
        }
    }

    protected void stopAutoScrolling() {
        if (!isAutoScrolling) {
            return;
        }
        getTimer().stop();
        isAutoScrolling = false;
    }

    protected void autoScrollUp() {
        int selection = list.getSelectedIndex();
        int firstVisible = list.getFirstVisibleIndex();
        if (selection == firstVisible && selection > 0) {
            list.setSelectedIndex(selection - 1);
            list.ensureIndexIsVisible(selection - 1);
        } else if (selection != firstVisible) {
            list.setSelectedIndex(firstVisible);
            list.ensureIndexIsVisible(firstVisible);
        }
    }

    protected void autoScrollDown() {
        int selection = list.getSelectedIndex();
        int lastVisible = list.getLastVisibleIndex();
        if (selection == lastVisible && selection < list.getModel().getSize() - 1) {
            list.setSelectedIndex(selection + 1);
            list.ensureIndexIsVisible(selection + 1);
        } else if (selection != lastVisible) {
            list.setSelectedIndex(lastVisible);
            list.ensureIndexIsVisible(lastVisible);
        }
    }

    protected void delegateFocus(final MouseEvent e) {
        if (comboBox.isEditable()) {
            comboBox.getEditor().getEditorComponent().requestFocus();
        } else {
            comboBox.requestFocus();
        }
    }

    protected void togglePopup() {
        comboBox.setPopupVisible(!comboBox.isPopupVisible());
    }

    protected MouseEvent convertMouseEvent(final MouseEvent e) {
        return SwingUtilities.convertMouseEvent((Component)e.getSource(), e, list);
    }

    protected int getPopupHeightForRowCount(final int maxRowCount) {
        int maxRow = Math.min(list.getModel().getSize(), maxRowCount);
        if (maxRow == 0) {
            return 100;
        } else {
            return list.getCellBounds(0, maxRow - 1).height;
        }
    }

    protected Rectangle computePopupBounds(final int px, final int py, final int pw, final int ph) {
        Insets insets = comboBox.getInsets(cachedInsets);
        Rectangle anchor = new Rectangle(px, py, comboBox.getWidth() - insets.left - insets.right, comboBox.getHeight() - insets.top - insets.bottom);
        Point location = Utilities.getPopupLocation(anchor, new Dimension(pw,
                ph), comboBox.getComponentOrientation().isLeftToRight(), false,
                comboBox.getGraphicsConfiguration());
        return new Rectangle(location.x, location.y, pw, ph);
    }

    protected void updateListBoxSelectionForEvent(final MouseEvent e, final boolean shouldScroll) {
        int index = list.locationToIndex(e.getPoint());
        if (index != -1) {
            list.setSelectedIndex(index);
            if (shouldScroll) {
                list.ensureIndexIsVisible(index);
            }
        }
    }

    private void uninstallComboBoxListeners() {
        if (itemListener != null) {
            comboBox.removeItemListener(itemListener);
        }
        itemListener = null;

        if (propertyChangeListener != null) {
            comboBox.removePropertyChangeListener(propertyChangeListener);
        }
        propertyChangeListener = null;
    }

    private Timer getTimer() {
        if (autoscrollTimer == null) {
            autoscrollTimer = new Timer(100, new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    if (scrollDirection == SCROLL_UP) {
                        autoScrollUp();
                    } else if (scrollDirection == SCROLL_DOWN) {
                        autoScrollDown();
                    } else {
                        throw new IllegalArgumentException(Messages.getString("swing.6E")); //$NON-NLS-1$
                    }
                }
            });
        }

        return autoscrollTimer;
    }
}
