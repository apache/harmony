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

import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingTestCase;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class BasicComboPopupTest extends SwingTestCase {
    private BasicComboPopup popup;

    private JComboBox comboBox;

    public BasicComboPopupTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        comboBox = new JComboBox(new Object[] { "1", "2", "3" });
        popup = new BasicComboPopup(comboBox);
        ((BasicComboBoxUI) comboBox.getUI()).popup = popup;
    }

    @Override
    protected void tearDown() throws Exception {
        comboBox = null;
        popup = null;
    }

    public void testBasicComboPopup() throws Exception {
        assertNotNull(popup.comboBox);
        assertNotNull(popup.list);
        assertNotNull(popup.scroller);
        assertEquals(3, popup.list.getModel().getSize());
        popup = new BasicComboPopup(new JComboBox());
        assertNotNull(popup.list);
        assertEquals(0, popup.list.getModel().getSize());
        assertTrue(popup.comboBox.getModel() == popup.list.getModel());
        assertEquals(popup.getComponent(), popup);
        assertEquals(1, popup.getComponentCount());
        assertEquals(popup.scroller, popup.getComponent(0));
        assertEquals(ListSelectionModel.SINGLE_SELECTION, popup.list.getSelectionMode());
        assertTrue(popup.getBorder().getClass() == LineBorder.class);
        assertEquals(new Insets(1, 1, 1, 1), popup.getInsets());
        assertFalse(popup.isAutoScrolling);
    }

    public void testShowHide() throws Exception {
        createVisibleComboBox();
        popup.show();
        assertTrue(popup.isShowing());
        popup.show();
        assertTrue(popup.isShowing());
        popup.hide();
        assertFalse(popup.isShowing());
        popup.hide();
        assertFalse(popup.isShowing());
        popup.show();
        assertTrue(popup.isShowing());
    }

    public void testGetList() throws Exception {
        assertNotNull(popup.getList());
        assertEquals(popup.list, popup.getList());
        JList newList = new JList();
        popup.list = newList;
        assertEquals(newList, popup.getList());
    }

    public void testGetMouseListener() throws Exception {
        assertNull(popup.mouseListener);
        assertEquals(popup.getMouseListener(), popup.mouseListener);
        if (isHarmony()) {
            assertTrue(popup.getMouseListener().getClass() == BasicComboPopup.InvocationMouseHandler.class);
        }
    }

    public void testGetMouseMotionListener() throws Exception {
        assertNull(popup.mouseMotionListener);
        assertEquals(popup.getMouseMotionListener(), popup.mouseMotionListener);
        if (isHarmony()) {
            assertTrue(popup.getMouseMotionListener().getClass() == BasicComboPopup.InvocationMouseMotionHandler.class);
        }
    }

    public void testGetKeyListener() throws Exception {
        assertNull(popup.keyListener);
        assertNull(popup.getKeyListener());
    }

    public void testUninstallingUI() throws Exception {
        comboBox = new JComboBox();
        int mouseListenerCount = comboBox.getMouseListeners().length;
        int mouseMotionListenerCount = comboBox.getMouseMotionListeners().length;
        int itemListenerCount = comboBox.getItemListeners().length;
        int propertyChangeListenerCount = comboBox.getPropertyChangeListeners().length;
        int keyListenerCount = comboBox.getKeyListeners().length;
        popup = new BasicComboPopup(comboBox);
        assertEquals(mouseListenerCount, comboBox.getMouseListeners().length);
        assertEquals(mouseMotionListenerCount, comboBox.getMouseMotionListeners().length);
        assertEquals(keyListenerCount, comboBox.getKeyListeners().length);
        assertEquals(itemListenerCount + 1, comboBox.getItemListeners().length);
        assertEquals(propertyChangeListenerCount + 1,
                comboBox.getPropertyChangeListeners().length);
        popup.uninstallingUI();
        assertEquals(mouseListenerCount, comboBox.getMouseListeners().length);
        assertEquals(mouseMotionListenerCount, comboBox.getMouseMotionListeners().length);
        assertEquals(keyListenerCount, comboBox.getKeyListeners().length);
        assertEquals(itemListenerCount, comboBox.getItemListeners().length);
        assertEquals(propertyChangeListenerCount, comboBox.getPropertyChangeListeners().length);
    }

    public void testFirePopupMenuWillBecomeVisibleInvisibleCanceled() throws Exception {
        PopupMenuController comboController = new PopupMenuController();
        PopupMenuController popupController = new PopupMenuController();
        popup.addPopupMenuListener(popupController);
        popup.comboBox.addPopupMenuListener(comboController);
        popup.firePopupMenuCanceled();
        assertNotNull(popupController.getEvent());
        assertNotNull(comboController.getEvent());
        assertEquals(PopupMenuController.MENU_CANCELED, popupController.getEventType());
        assertEquals(PopupMenuController.MENU_CANCELED, comboController.getEventType());
        popupController.reset();
        comboController.reset();
        popup.firePopupMenuWillBecomeInvisible();
        assertNotNull(popupController.getEvent());
        assertNotNull(comboController.getEvent());
        assertEquals(PopupMenuController.MENU_INVISIBLE, popupController.getEventType());
        assertEquals(PopupMenuController.MENU_INVISIBLE, comboController.getEventType());
        popupController.reset();
        comboController.reset();
        popup.firePopupMenuWillBecomeVisible();
        assertNotNull(popupController.getEvent());
        assertNotNull(comboController.getEvent());
        assertEquals(PopupMenuController.MENU_VISIBLE, popupController.getEventType());
        assertEquals(PopupMenuController.MENU_VISIBLE, comboController.getEventType());
    }

    public void testCreateMouseListener() throws Exception {
        if (isHarmony()) {
            assertTrue(popup.createMouseListener().getClass() == BasicComboPopup.InvocationMouseHandler.class);
            assertFalse(popup.createMouseListener() == popup.createMouseListener());
        }
    }

    public void testCreateMouseMotionListener() throws Exception {
        if (isHarmony()) {
            assertTrue(popup.createMouseMotionListener().getClass() == BasicComboPopup.InvocationMouseMotionHandler.class);
            assertFalse(popup.createMouseMotionListener() == popup.createMouseMotionListener());
        }
    }

    public void testCreateKeyListener() throws Exception {
        assertNull(popup.createKeyListener());
    }

    public void testCreateListSelectionListener() throws Exception {
        assertNull(popup.createListSelectionListener());
        assertNull(popup.listSelectionListener);
    }

    public void testCreateListDataListener() throws Exception {
        assertNull(popup.createListDataListener());
        assertNull(popup.listDataListener);
    }

    public void testCreateListMouseListener() throws Exception {
        if (isHarmony()) {
            assertTrue(popup.listMouseListener.getClass() == BasicComboPopup.ListMouseHandler.class);
            assertTrue(popup.createListMouseListener().getClass() == BasicComboPopup.ListMouseHandler.class);
            assertFalse(popup.createListMouseListener() == popup.createListMouseListener());
        }
    }

    public void testCreateListMouseMotionListener() throws Exception {
        if (isHarmony()) {
            assertTrue(popup.listMouseMotionListener.getClass() == BasicComboPopup.ListMouseMotionHandler.class);
            assertTrue(popup.createListMouseMotionListener().getClass() == BasicComboPopup.ListMouseMotionHandler.class);
            assertFalse(popup.createListMouseMotionListener() == popup
                    .createListMouseMotionListener());
        }
    }

    public void testCreatePropertyChangeListener() throws Exception {
        if (isHarmony()) {
            assertTrue(popup.propertyChangeListener.getClass() == BasicComboPopup.PropertyChangeHandler.class);
            assertTrue(popup.createPropertyChangeListener().getClass() == BasicComboPopup.PropertyChangeHandler.class);
            assertFalse(popup.createPropertyChangeListener() == popup
                    .createPropertyChangeListener());
        }
    }

    public void testCreateItemListener() throws Exception {
        if (isHarmony()) {
            assertTrue(popup.itemListener.getClass() == BasicComboPopup.ItemHandler.class);
            assertTrue(popup.createItemListener().getClass() == BasicComboPopup.ItemHandler.class);
            assertFalse(popup.createItemListener() == popup.createItemListener());
        }
    }

    public void testCreateList() throws Exception {
        assertNotSame(popup.createList(), popup.createList());
        assertNotSame(popup.createList(), popup.list);
    }

    public void testConfigureList() throws Exception {
        popup.list = new JList();
        int mouseListenerCount = popup.list.getMouseListeners().length;
        int mouseMotionListenerCount = popup.list.getMouseMotionListeners().length;
        popup.configureList();
        assertEquals(mouseListenerCount + 1, popup.list.getMouseListeners().length);
        assertEquals(mouseMotionListenerCount + 1, popup.list.getMouseMotionListeners().length);
        assertEquals(ListSelectionModel.SINGLE_SELECTION, popup.list.getSelectionMode());
    }

    public void testCreateScroller() throws Exception {
        assertNotNull(popup.scroller);
        assertNotSame(popup.createScroller(), popup.createScroller());
        assertNotSame(popup.createScroller(), popup.scroller);
    }

    public void testConfigureScroller() throws Exception {
        popup.scroller = new JScrollPane();
        popup.configureScroller();
    }

    public void testInstallUninstallComboBoxModelListeners() throws Exception {
        popup.installComboBoxModelListeners(null);
        popup.uninstallComboBoxModelListeners(null);
    }

    public void testInstallUninstallComboBoxListeners() throws Exception {
        int mouseListenerCount = comboBox.getMouseListeners().length;
        int mouseMotionListenerCount = comboBox.getMouseMotionListeners().length;
        int itemListenerCount = comboBox.getItemListeners().length;
        int propertyChangeListenerCount = comboBox.getPropertyChangeListeners().length;
        int keyListenerCount = comboBox.getKeyListeners().length;
        popup.installComboBoxListeners();
        assertEquals(mouseListenerCount, comboBox.getMouseListeners().length);
        assertEquals(mouseMotionListenerCount, comboBox.getMouseMotionListeners().length);
        assertEquals(keyListenerCount, comboBox.getKeyListeners().length);
        assertEquals(itemListenerCount + 1, comboBox.getItemListeners().length);
        assertEquals(propertyChangeListenerCount + 1,
                comboBox.getPropertyChangeListeners().length);
    }

    public void testInstallUninstallKeyboardActions() throws Exception {
        int count = popup.comboBox.getActionMap().allKeys().length;
        popup.uninstallKeyboardActions();
        assertEquals(count, popup.comboBox.getActionMap().allKeys().length);
        popup.installKeyboardActions();
        assertEquals(count, popup.comboBox.getActionMap().allKeys().length);
    }

    public void testInstallListListeners() throws Exception {
        int mouseListenerCount = popup.list.getMouseListeners().length;
        int mouseMotionListenerCount = popup.list.getMouseMotionListeners().length;
        int selectionListenerCount = popup.list.getListSelectionListeners().length;
        popup.installListListeners();
        assertEquals(mouseListenerCount + 1, popup.list.getMouseListeners().length);
        assertEquals(mouseMotionListenerCount + 1, popup.list.getMouseMotionListeners().length);
        assertEquals(selectionListenerCount, popup.list.getListSelectionListeners().length);
    }

    public void testIsFocusTraversable() throws Exception {
        assertFalse(popup.isFocusTraversable());
    }

    public void testStartStopAutoscrolloing() throws Exception {
        assertNull(popup.autoscrollTimer);
        assertFalse(popup.isAutoScrolling);
        popup.startAutoScrolling(BasicComboPopup.SCROLL_UP);
        assertNotNull(popup.autoscrollTimer);
        assertTrue(popup.isAutoScrolling);
        assertEquals(BasicComboPopup.SCROLL_UP, popup.scrollDirection);
        popup.startAutoScrolling(BasicComboPopup.SCROLL_DOWN);
        assertNotNull(popup.autoscrollTimer);
        assertTrue(popup.isAutoScrolling);
        assertEquals(BasicComboPopup.SCROLL_DOWN, popup.scrollDirection);
        popup.stopAutoScrolling();
        assertFalse(popup.isAutoScrolling);
    }

    public void testAutoScrollUpDown() throws Exception {
        if (isHarmony()) {
            createVisibleComboBox();
            popup.show();
            popup.list.setSelectedIndex(2);
            popup.autoScrollUp();
            assertEquals(0, popup.list.getSelectedIndex());
            popup.autoScrollUp();
            assertEquals(0, popup.list.getSelectedIndex());
            popup.autoScrollDown();
            assertEquals(2, popup.list.getSelectedIndex());
            popup.autoScrollDown();
            assertEquals(2, popup.list.getSelectedIndex());
            popup.autoScrollUp();
            assertEquals(0, popup.list.getSelectedIndex());
        }
    }

    public void testGetAccessibleContext() throws Exception {
        assertNotNull(popup.getAccessibleContext());
        //        Is not clear how it should be
        //        assertEquals(popup.comboBox, popup.getAccessibleContext().getAccessibleParent());
    }

    public void testTogglePopup() throws Exception {
        createVisibleComboBox();
        assertFalse(popup.isShowing());
        popup.togglePopup();
        assertTrue(popup.isShowing());
        popup.togglePopup();
        assertFalse(popup.isShowing());
    }

    public void testConvertMouseEvent() throws Exception {
        MouseEvent original = createMouseEvent(0, 0);
        assertNotSame(original, popup.convertMouseEvent(original));
        comboBox.setLocation(0, 0);
        assertEquals(new Point(10, 20), popup.convertMouseEvent(createMouseEvent(10, 20))
                .getPoint());
        assertEquals(new Point(-10, -20), popup.convertMouseEvent(createMouseEvent(-10, -20))
                .getPoint());
        comboBox.setLocation(100, 200);
        assertEquals(new Point(110, 220), popup.convertMouseEvent(createMouseEvent(10, 20))
                .getPoint());
        assertEquals(new Point(90, 180), popup.convertMouseEvent(createMouseEvent(-10, -20))
                .getPoint());
    }

    public void testGetPopupHeightForRowCount() throws Exception {
        popup = new BasicComboPopup(new JComboBox());
        assertEquals(100, popup.getPopupHeightForRowCount(0));
        assertEquals(100, popup.getPopupHeightForRowCount(1));
        assertEquals(100, popup.getPopupHeightForRowCount(100));
        popup = new BasicComboPopup(new JComboBox(new Object[] { "1" }));
        popup.list.setFont(comboBox.getFont().deriveFont(40f));
        int oneElemHeight = popup.getPopupHeightForRowCount(1);
        assertTrue(oneElemHeight > 0 && oneElemHeight != 100);
        popup = new BasicComboPopup(new JComboBox(new Object[] { "1", "2", "3" }));
        popup.list.setFont(comboBox.getFont().deriveFont(40f));
        assertEquals(oneElemHeight, popup.getPopupHeightForRowCount(1));
        assertEquals(2 * oneElemHeight, popup.getPopupHeightForRowCount(2));
        assertEquals(3 * oneElemHeight, popup.getPopupHeightForRowCount(3));
        assertEquals(3 * oneElemHeight, popup.getPopupHeightForRowCount(100));
        assertEquals(100, popup.getPopupHeightForRowCount(0));
    }

    private MouseEvent createMouseEvent(final int x, final int y) {
        return new MouseEvent(comboBox, MouseEvent.MOUSE_CLICKED, 0, 0, x, y, 0, false);
    }

    @SuppressWarnings("deprecation")
    private void createVisibleComboBox() {
        JFrame frame = new JFrame();
        frame.getContentPane().add(comboBox);
        frame.pack();
        frame.show();
    }

    private class PopupMenuController implements PopupMenuListener {
        public static final int MENU_CANCELED = 1;

        public static final int MENU_VISIBLE = 2;

        public static final int MENU_INVISIBLE = 3;

        private PopupMenuEvent event;

        private int eventType;

        public void popupMenuCanceled(final PopupMenuEvent e) {
            event = e;
            eventType = MENU_CANCELED;
        }

        public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
            event = e;
            eventType = MENU_INVISIBLE;
        }

        public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
            event = e;
            eventType = MENU_VISIBLE;
        }

        public PopupMenuEvent getEvent() {
            return event;
        }

        public int getEventType() {
            return eventType;
        }

        public void reset() {
            event = null;
            eventType = 0;
        }
    }
}
