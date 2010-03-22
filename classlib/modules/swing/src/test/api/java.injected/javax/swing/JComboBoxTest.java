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
package javax.swing;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.event.ListDataListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.BasicComboBoxEditor;

@SuppressWarnings("serial")
public class JComboBoxTest extends SwingTestCase {
    private JComboBox comboBox;

    private JFrame frame;

    public JComboBoxTest(final String name) {
        super(name);
        setIgnoreNotImplemented(true);
    }

    @Override
    protected void setUp() throws Exception {
        comboBox = new JComboBox();
        propertyChangeController = new PropertyChangeController();
        comboBox.addPropertyChangeListener(propertyChangeController);
    }

    @Override
    protected void tearDown() throws Exception {
        comboBox = null;
        propertyChangeController = null;
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public void testJComboBox() throws Exception {
        assertNotNull(comboBox.dataModel);
        assertEquals(comboBox.dataModel, comboBox.getModel());
        assertTrue(comboBox.dataModel instanceof DefaultComboBoxModel);
        DefaultComboBoxModel newModel = new DefaultComboBoxModel();
        comboBox = new JComboBox(newModel);
        assertEquals(newModel, comboBox.getModel());
        comboBox = new JComboBox(new Object[] { "1", "2", "3" });
        assertEquals(3, comboBox.getModel().getSize());
        Vector<String> newData = new Vector<String>();
        newData.add("1");
        newData.add("2");
        comboBox = new JComboBox(newData);
        assertEquals(2, comboBox.getModel().getSize());
        assertFalse(propertyChangeController.isChanged());
    }

    public void testSetUI() throws Exception {
        assertNotNull(comboBox.getUI());
        ComboBoxUI newUI = new ComboBoxUI() {
            @Override
            public boolean isFocusTraversable(final JComboBox arg0) {
                return false;
            }

            @Override
            public boolean isPopupVisible(final JComboBox arg0) {
                return false;
            }

            @Override
            public void setPopupVisible(final JComboBox arg0, final boolean arg1) {
            }
        };
        comboBox.setUI(newUI);
        assertEquals(newUI, comboBox.getUI());
    }

    public void testGetUIClassID() throws Exception {
        assertEquals("ComboBoxUI", comboBox.getUIClassID());
    }

    public void testGetSetModel() throws Exception {
        assertNotNull(comboBox.getModel());
        DefaultComboBoxModel newModel = new DefaultComboBoxModel();
        comboBox.setModel(newModel);
        assertEquals(newModel, comboBox.getModel());
        assertTrue(propertyChangeController.isChanged("model"));
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                comboBox.setModel(null);
            }
        });
    }

    public void testSetIsLightWeightPopupEnabled() throws Exception {
        assertTrue(comboBox.isLightWeightPopupEnabled());
        comboBox.setLightWeightPopupEnabled(true);
        assertFalse(propertyChangeController.isChanged());
        comboBox.setLightWeightPopupEnabled(false);
        assertFalse(comboBox.isLightWeightPopupEnabled());
        assertTrue(propertyChangeController.isChanged("lightWeightPopupEnabled"));
    }

    public void testSetIsEditable() throws Exception {
        assertFalse(comboBox.isEditable());
        comboBox.setEditable(true);
        assertTrue(comboBox.isEditable());
        assertTrue(propertyChangeController.isChanged("editable"));
    }

    public void testGetSetMaximumRowCount() throws Exception {
        assertEquals(8, comboBox.getMaximumRowCount());
        comboBox.setMaximumRowCount(-3);
        assertEquals(-3, comboBox.getMaximumRowCount());
        assertTrue(propertyChangeController.isChanged("maximumRowCount"));
        propertyChangeController.reset();
        comboBox.setMaximumRowCount(5);
        assertEquals(5, comboBox.getMaximumRowCount());
        assertTrue(propertyChangeController.isChanged("maximumRowCount"));
    }

    public void testGetSetRenderer() throws Exception {
        assertNotNull(comboBox.getRenderer());
        DefaultListCellRenderer newRenderer = new DefaultListCellRenderer();
        comboBox.setRenderer(newRenderer);
        assertEquals(newRenderer, comboBox.getRenderer());
        assertTrue(propertyChangeController.isChanged("renderer"));
    }

    public void testGetSetEditor() throws Exception {
        assertNotNull(comboBox.getEditor());
        ComboBoxEditor newEditor = new BasicComboBoxEditor();
        comboBox.setEditor(newEditor);
        assertEquals(newEditor, comboBox.getEditor());
        assertTrue(propertyChangeController.isChanged("editor"));
        comboBox.setEditor(null);
        assertNull(comboBox.getEditor());
    }

    public void testSetGetSelectedItem() throws Exception {
        ItemController itemController = new ItemController();
        comboBox.addItemListener(itemController);
        ActionController actionController = new ActionController();
        comboBox.addActionListener(actionController);
        assertNull(comboBox.getSelectedItem());
        comboBox.setSelectedItem("a");
        assertNull(comboBox.getSelectedItem());
        assertEquals(-1, comboBox.getSelectedIndex());
        assertNull(actionController.getEvent());
        assertTrue(itemController.getEvents().isEmpty());
        actionController.reset();
        itemController.reset();
        comboBox.setEditable(true);
        comboBox.setSelectedItem("a");
        assertEquals("a", comboBox.getSelectedItem());
        assertEquals(-1, comboBox.getSelectedIndex());
        assertEquals(1, itemController.getEvents().size());
        assertEquals(ItemEvent.SELECTED, itemController.getEvents().get(0).getStateChange());
        assertEquals(ItemEvent.ITEM_STATE_CHANGED, itemController.getEvents().get(0).getID());
        assertNotNull(actionController.getEvent());
        actionController.reset();
        itemController.reset();
        comboBox.setSelectedItem("b");
        assertEquals("b", comboBox.getSelectedItem());
        assertEquals(-1, comboBox.getSelectedIndex());
        assertEquals(2, itemController.getEvents().size());
        assertEquals(ItemEvent.DESELECTED, itemController.getEvents().get(0).getStateChange());
        assertEquals(ItemEvent.ITEM_STATE_CHANGED, itemController.getEvents().get(0).getID());
        assertEquals(ItemEvent.SELECTED, itemController.getEvents().get(1).getStateChange());
        assertEquals(ItemEvent.ITEM_STATE_CHANGED, itemController.getEvents().get(1).getID());
        assertNotNull(actionController.getEvent());
        actionController.reset();
        itemController.reset();
        assertEquals("b", comboBox.getSelectedItem());
        comboBox.setSelectedItem("b");
        assertTrue(itemController.getEvents().isEmpty());
        assertNotNull(actionController.getEvent());
        actionController.reset();
        itemController.reset();
        comboBox.setEditable(false);
        comboBox.addItem("a");
        comboBox.addItem("b");
        assertEquals(1, comboBox.getSelectedIndex());
        comboBox.setSelectedItem("c");
        assertEquals("b", comboBox.getSelectedItem());
        assertEquals(1, comboBox.getSelectedIndex());
        assertNull(actionController.getEvent());
        assertTrue(itemController.getEvents().isEmpty());
        assertEquals(1, comboBox.getSelectedIndex());
        comboBox.setSelectedItem("b");
        assertEquals(1, comboBox.getSelectedIndex());
        assertNotNull(actionController.getEvent());
        assertTrue(itemController.getEvents().isEmpty());
        comboBox.setSelectedItem("a");
        assertEquals("a", comboBox.getSelectedItem());
        assertEquals(0, comboBox.getSelectedIndex());
        assertNotNull(actionController.getEvent());
        assertEquals(2, itemController.getEvents().size());
        comboBox.removeItem("a");
        assertEquals("b", comboBox.getSelectedItem());
        assertEquals(0, comboBox.getSelectedIndex());
    }

    public void testGetSetSelectedIndex() throws Exception {
        assertEquals(-1, comboBox.getSelectedIndex());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                comboBox.setSelectedIndex(0);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                comboBox.setSelectedIndex(-2);
            }
        });
        comboBox.setSelectedIndex(-1);
        assertEquals(-1, comboBox.getSelectedIndex());
        comboBox.addItem("a");
        comboBox.addItem("b");
        assertEquals(0, comboBox.getSelectedIndex());
        assertEquals("a", comboBox.getSelectedItem());
        comboBox.setSelectedIndex(0);
        assertEquals(0, comboBox.getSelectedIndex());
        assertEquals("a", comboBox.getSelectedItem());
        comboBox.removeItem("a");
        assertEquals(0, comboBox.getSelectedIndex());
        assertEquals("b", comboBox.getSelectedItem());
        comboBox.addItem("c");
        comboBox.addItem("d");
        comboBox.addItem("e");
        comboBox.setSelectedItem("d");
        assertEquals(2, comboBox.getSelectedIndex());
        comboBox.removeItem("d");
        assertEquals(1, comboBox.getSelectedIndex());
        assertEquals("c", comboBox.getSelectedItem());
        comboBox.setEditable(true);
        comboBox.setSelectedItem("f");
        assertEquals(-1, comboBox.getSelectedIndex());
    }

    public void testPrototypeDisplayValue() throws Exception {
        assertNull(comboBox.getPrototypeDisplayValue());
        comboBox.setPrototypeDisplayValue("a");
        assertEquals("a", comboBox.getPrototypeDisplayValue());
        assertTrue(propertyChangeController.isChanged("prototypeDisplayValue"));
    }

    public void testAddItem() throws Exception {
        ItemController itemController = new ItemController();
        comboBox.addItemListener(itemController);
        ActionController actionController = new ActionController();
        comboBox.addActionListener(actionController);
        assertNull(comboBox.getSelectedItem());
        assertEquals(-1, comboBox.getSelectedIndex());
        comboBox.addItem("a");
        assertEquals(1, comboBox.getModel().getSize());
        assertEquals(1, itemController.getEvents().size());
        assertNotNull(actionController.getEvent());
        assertEquals("a", comboBox.getSelectedItem());
        assertEquals(0, comboBox.getSelectedIndex());
        itemController.reset();
        actionController.reset();
        comboBox.addItem("b");
        assertEquals(0, itemController.getEvents().size());
        assertNull(actionController.getEvent());
        ComboBoxModel immutableModel = new ComboBoxModel() {
            public Object getSelectedItem() {
                return null;
            }

            public void setSelectedItem(final Object value) {
            }

            public void addListDataListener(final ListDataListener l) {
            }

            public Object getElementAt(final int index) {
                return null;
            }

            public int getSize() {
                return 0;
            }

            public void removeListDataListener(final ListDataListener l) {
            }
        };
        comboBox.setModel(immutableModel);
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                comboBox.addItem("a");
            }
        });
    }

    public void testInsertItemAt() throws Exception {
        ItemController itemController = new ItemController();
        comboBox.addItemListener(itemController);
        ActionController actionController = new ActionController();
        comboBox.addActionListener(actionController);
        assertNull(comboBox.getSelectedItem());
        assertEquals(-1, comboBox.getSelectedIndex());
        comboBox.insertItemAt("a", 0);
        assertEquals(1, comboBox.getModel().getSize());
        assertEquals(0, itemController.getEvents().size());
        assertNull(actionController.getEvent());
        assertNull(comboBox.getSelectedItem());
        assertEquals(-1, comboBox.getSelectedIndex());
        itemController.reset();
        actionController.reset();
        comboBox.insertItemAt("b", 1);
        assertEquals(0, itemController.getEvents().size());
        assertNull(actionController.getEvent());
        ComboBoxModel immutableModel = new ComboBoxModel() {
            public Object getSelectedItem() {
                return null;
            }

            public void setSelectedItem(final Object value) {
            }

            public void addListDataListener(final ListDataListener l) {
            }

            public Object getElementAt(final int index) {
                return null;
            }

            public int getSize() {
                return 0;
            }

            public void removeListDataListener(final ListDataListener l) {
            }
        };
        comboBox.setModel(immutableModel);
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                comboBox.insertItemAt("c", 0);
            }
        });
    }

    public void testRemoveItem() throws Exception {
        ItemController itemController = new ItemController();
        comboBox.addItemListener(itemController);
        ActionController actionController = new ActionController();
        comboBox.addActionListener(actionController);
        comboBox.addItem("a");
        comboBox.addItem("b");
        assertEquals("a", comboBox.getSelectedItem());
        itemController.reset();
        actionController.reset();
        comboBox.removeItem("a");
        assertEquals("b", comboBox.getSelectedItem());
        assertEquals(1, comboBox.getModel().getSize());
        assertEquals(2, itemController.getEvents().size());
        assertNotNull(actionController.getEvent());
        itemController.reset();
        actionController.reset();
        comboBox.removeItem("a");
        assertEquals(1, comboBox.getModel().getSize());
        assertEquals(0, itemController.getEvents().size());
        assertNull(actionController.getEvent());
    }

    public void testRemoveItemAt() throws Exception {
        ItemController itemController = new ItemController();
        comboBox.addItemListener(itemController);
        ActionController actionController = new ActionController();
        comboBox.addActionListener(actionController);
        comboBox.addItem("a");
        comboBox.addItem("b");
        assertEquals("a", comboBox.getSelectedItem());
        itemController.reset();
        actionController.reset();
        comboBox.removeItemAt(0);
        assertEquals("b", comboBox.getSelectedItem());
        assertEquals(1, comboBox.getModel().getSize());
        assertEquals(2, itemController.getEvents().size());
        assertNotNull(actionController.getEvent());
        itemController.reset();
        actionController.reset();
        comboBox.removeItemAt(0);
        assertEquals(0, comboBox.getModel().getSize());
        assertNull(comboBox.getSelectedItem());
        assertEquals(1, itemController.getEvents().size());
        assertNotNull(actionController.getEvent());
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                comboBox.removeItemAt(0);
            }
        });
    }

    public void testGetItemCount() throws Exception {
        assertEquals(0, comboBox.getItemCount());
        comboBox.addItem("a");
        assertEquals(1, comboBox.getItemCount());
        comboBox.addItem("b");
        assertEquals(2, comboBox.getItemCount());
        comboBox.removeItem("b");
        assertEquals(1, comboBox.getItemCount());
    }

    public void testGetItemAt() throws Exception {
        assertNull(comboBox.getItemAt(0));
        assertNull(comboBox.getItemAt(-1));
        comboBox.addItem("a");
        comboBox.addItem("b");
        assertEquals("a", comboBox.getItemAt(0));
        assertEquals("b", comboBox.getItemAt(1));
        assertNull(comboBox.getItemAt(2));
    }

    public void testAddRemoveGetFireItemListener() throws Exception {
        comboBox.getUI().uninstallUI(comboBox);
        assertEquals(0, comboBox.getItemListeners().length);
        ItemController l = new ItemController();
        comboBox.addItemListener(l);
        assertEquals(1, comboBox.getItemListeners().length);
        comboBox.addItemListener(new ItemController());
        assertEquals(2, comboBox.getItemListeners().length);
        comboBox.fireItemStateChanged(new ItemEvent(comboBox, ItemEvent.ITEM_STATE_CHANGED,
                "a", ItemEvent.SELECTED));
        assertEquals(1, l.getEvents().size());
        assertEquals(comboBox, l.getEvents().get(0).getSource());
        comboBox.removeItemListener(l);
        assertEquals(1, comboBox.getItemListeners().length);
    }

    public void testAddRemoveGetFireActionListener() throws Exception {
        comboBox.getUI().uninstallUI(comboBox);
        assertEquals(0, comboBox.getItemListeners().length);
        ActionController l = new ActionController();
        comboBox.addActionListener(l);
        assertEquals(1, comboBox.getActionListeners().length);
        comboBox.addActionListener(new ActionController());
        assertEquals(2, comboBox.getActionListeners().length);
        comboBox.fireActionEvent();
        assertNotNull(l.getEvent());
        assertEquals(comboBox, l.getEvent().getSource());
        comboBox.removeActionListener(l);
        assertEquals(1, comboBox.getActionListeners().length);
    }

    public void testAddRemoveGetFirePopupListener() throws Exception {
        comboBox.getUI().uninstallUI(comboBox);
        assertEquals(0, comboBox.getItemListeners().length);
        PopupMenuController l = new PopupMenuController();
        comboBox.addPopupMenuListener(l);
        assertEquals(1, comboBox.getPopupMenuListeners().length);
        comboBox.addPopupMenuListener(new PopupMenuController());
        assertEquals(2, comboBox.getPopupMenuListeners().length);
        comboBox.firePopupMenuCanceled();
        assertNotNull(l.getEvent());
        assertEquals(PopupMenuController.CANCELLED, l.getEventType());
        l.reset();
        comboBox.firePopupMenuWillBecomeVisible();
        assertNotNull(l.getEvent());
        assertEquals(PopupMenuController.VISIBLE, l.getEventType());
        assertEquals(comboBox, l.getEvent().getSource());
        l.reset();
        comboBox.firePopupMenuWillBecomeInvisible();
        assertNotNull(l.getEvent());
        assertEquals(PopupMenuController.INVISIBLE, l.getEventType());
        comboBox.removePopupMenuListener(l);
        assertEquals(1, comboBox.getPopupMenuListeners().length);
    }

    public void testGetSetActionCommand() throws Exception {
        assertEquals("comboBoxChanged", comboBox.getActionCommand());
        comboBox.setActionCommand("anotherCommand");
        assertEquals("anotherCommand", comboBox.getActionCommand());
        assertFalse(propertyChangeController.isChanged());
        ActionController actionController = new ActionController();
        comboBox.addActionListener(actionController);
        comboBox.addItem("any");
        assertEquals("anotherCommand", actionController.getEvent().getActionCommand());
    }

    public void testGetSelectedObjects() throws Exception {
        assertNull(comboBox.getSelectedItem());
        assertEquals(0, comboBox.getSelectedObjects().length);
        comboBox.setEditable(true);
        comboBox.setSelectedItem("a");
        assertEquals("a", comboBox.getSelectedObjects()[0]);
    }

    public void testSetEnabled() throws Exception {
        assertTrue(comboBox.isEnabled());
        comboBox.setEnabled(false);
        assertFalse(comboBox.isEnabled());
        assertTrue(propertyChangeController.isChanged("enabled"));
    }

    public void testSetGetAction() throws Exception {
        assertEquals(0, comboBox.getActionListeners().length);
        assertNull(comboBox.getAction());
        TestAction action = new TestAction();
        comboBox.setAction(action);
        assertTrue(propertyChangeController.isChanged("action"));
        assertEquals(action, comboBox.getAction());
        assertEquals(1, comboBox.getActionListeners().length);
        propertyChangeController.reset();
        comboBox.setAction(action);
        assertFalse(propertyChangeController.isChanged());
        assertEquals(1, comboBox.getActionListeners().length);
        action.reset();
        comboBox.fireActionEvent();
        assertEquals(1, action.getEvents().size());
        action.reset();
        comboBox.addActionListener(action);
        comboBox.fireActionEvent();
        assertEquals(2, action.getEvents().size());
        assertEquals(2, comboBox.getActionListeners().length);
        action.reset();
        comboBox.setAction(null);
        assertNull(comboBox.getAction());
        comboBox.fireActionEvent();
        assertEquals(1, action.getEvents().size());
        assertEquals(1, comboBox.getActionListeners().length);
        action.reset();
        comboBox.setAction(action);
        comboBox.fireActionEvent();
        assertEquals(1, action.getEvents().size());
        assertEquals(1, comboBox.getActionListeners().length);
    }

    public void testIsSetPopupVisible() throws Exception {
        createVisibleComboBox();
        assertFalse(comboBox.isPopupVisible());
        assertFalse(comboBox.getUI().isPopupVisible(comboBox));
        PopupMenuController pmc = new PopupMenuController();
        comboBox.addPopupMenuListener(pmc);
        comboBox.setPopupVisible(true);
        assertTrue(comboBox.isPopupVisible());
        assertTrue(comboBox.getUI().isPopupVisible(comboBox));
        assertNotNull(pmc.getEvent());
        assertEquals(PopupMenuController.VISIBLE, pmc.getEventType());
        pmc.reset();
        comboBox.getUI().setPopupVisible(comboBox, false);
        assertFalse(comboBox.isPopupVisible());
        assertNotNull(pmc.getEvent());
        assertEquals(PopupMenuController.INVISIBLE, pmc.getEventType());
    }

    public void testShowHidePopup() throws Exception {
        createVisibleComboBox();
        assertFalse(comboBox.isPopupVisible());
        comboBox.showPopup();
        assertTrue(comboBox.isPopupVisible());
        comboBox.hidePopup();
        assertFalse(comboBox.isPopupVisible());
    }

    public void testCreateDefaultKeySelectionManager() throws Exception {
        JComboBox.KeySelectionManager ksm = comboBox.createDefaultKeySelectionManager();
        assertNotNull(ksm);
        comboBox.setKeySelectionManager(null);
        comboBox.selectWithKeyChar('a');
        assertNotNull(comboBox.getKeySelectionManager());
    }

    public void testDefaultKeySelectionManager() throws Exception {
        JComboBox.KeySelectionManager ksm = comboBox.createDefaultKeySelectionManager();
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        assertEquals(-1, ksm.selectionForKey('a', model));
        model.addElement("a 0");
        model.addElement("b 0");
        model.addElement(" b 0");
        assertEquals(0, ksm.selectionForKey('a', model));
        assertEquals(1, ksm.selectionForKey('b', model));
        assertEquals(2, ksm.selectionForKey(' ', model));
    }

    @SuppressWarnings("deprecation")
    public void testProcessKeyEvent() throws Exception {
        comboBox.setKeySelectionManager(null);
        createVisibleComboBox();
        PopupMenuController pmc = new PopupMenuController();
        comboBox.addPopupMenuListener(pmc);
        KeyEvent event = new KeyEvent(comboBox, KeyEvent.KEY_PRESSED, EventQueue
                .getMostRecentEventTime(), 0, KeyEvent.VK_A);
        comboBox.processKeyEvent(event);
        assertNull(pmc.getEvent());
        event = new KeyEvent(comboBox, KeyEvent.KEY_PRESSED, EventQueue
                .getMostRecentEventTime(), 0, KeyEvent.VK_TAB);
        comboBox.processKeyEvent(event);
        assertNull(pmc.getEvent());
        comboBox.setPopupVisible(true);
        assertNotNull(pmc.getEvent());
        assertEquals(PopupMenuController.VISIBLE, pmc.getEventType());
        pmc.reset();
        comboBox.addItem("a");
        comboBox.addItem("b");
        event = new KeyEvent(comboBox, KeyEvent.KEY_PRESSED, EventQueue
                .getMostRecentEventTime(), 0, KeyEvent.VK_A);
        comboBox.processKeyEvent(event);
        assertNull(pmc.getEvent());
        assertEquals("a", comboBox.getSelectedItem());
        pmc.reset();
        event = new KeyEvent(comboBox, KeyEvent.KEY_PRESSED, EventQueue
                .getMostRecentEventTime(), 0, KeyEvent.VK_B);
        comboBox.processKeyEvent(event);
        assertNull(pmc.getEvent());
        assertEquals("b", comboBox.getSelectedItem());
        pmc.reset();
        event = new KeyEvent(comboBox, KeyEvent.KEY_PRESSED, EventQueue
                .getMostRecentEventTime(), 0, KeyEvent.VK_A);
        comboBox.processKeyEvent(event);
        assertNull(pmc.getEvent());
        assertEquals("a", comboBox.getSelectedItem());
        pmc.reset();
        event = new KeyEvent(comboBox, KeyEvent.KEY_PRESSED, EventQueue
                .getMostRecentEventTime(), 0, KeyEvent.VK_TAB);
        comboBox.processKeyEvent(event);
        assertNotNull(pmc.getEvent());
        assertEquals(PopupMenuController.INVISIBLE, pmc.getEventType());
        assertEquals("a", comboBox.getSelectedItem());
        comboBox.setKeySelectionManager(new JComboBox.KeySelectionManager() {
            public int selectionForKey(final char key, final ComboBoxModel model) {
                return -1;
            }
        });
        pmc.reset();
        event = new KeyEvent(comboBox, KeyEvent.KEY_PRESSED, EventQueue
                .getMostRecentEventTime(), 0, KeyEvent.VK_TAB);
        comboBox.processKeyEvent(event);
        assertNull(pmc.getEvent());
        assertEquals("a", comboBox.getSelectedItem());
        pmc.reset();
        event = new KeyEvent(comboBox, KeyEvent.KEY_PRESSED, EventQueue
                .getMostRecentEventTime(), 0, KeyEvent.VK_A);
        comboBox.processKeyEvent(event);
        assertNull(pmc.getEvent());
        assertEquals("a", comboBox.getSelectedItem());
        pmc.reset();
        event = new KeyEvent(comboBox, KeyEvent.KEY_PRESSED, EventQueue
                .getMostRecentEventTime(), 0, KeyEvent.VK_B);
        comboBox.processKeyEvent(event);
        assertNull(pmc.getEvent());
        assertEquals("a", comboBox.getSelectedItem());
    }

    public void testSelectWithKeyChar() throws Exception {
        comboBox.setKeySelectionManager(null);
        PopupMenuController pmc = new PopupMenuController();
        comboBox.addPopupMenuListener(pmc);
        assertFalse(comboBox.selectWithKeyChar('a'));
        assertNull(pmc.getEvent());
        comboBox.addItem("a1");
        comboBox.addItem("a2");
        comboBox.addItem("a3");
        comboBox.addItem("b1");
        assertEquals("a1", comboBox.getSelectedItem());
        assertFalse(comboBox.selectWithKeyChar('c'));
        assertTrue(comboBox.selectWithKeyChar('A'));
        assertEquals("a2", comboBox.getSelectedItem());
        assertNull(pmc.getEvent());
        assertTrue(comboBox.selectWithKeyChar('a'));
        assertEquals("a3", comboBox.getSelectedItem());
        assertNull(pmc.getEvent());
        assertTrue(comboBox.selectWithKeyChar('A'));
        assertEquals("a1", comboBox.getSelectedItem());
        assertNull(pmc.getEvent());
        assertTrue(comboBox.selectWithKeyChar('b'));
        assertEquals("b1", comboBox.getSelectedItem());
        assertNull(pmc.getEvent());
        assertTrue(comboBox.selectWithKeyChar('b'));
        assertEquals("b1", comboBox.getSelectedItem());
        assertNull(pmc.getEvent());
        comboBox.setKeySelectionManager(new JComboBox.KeySelectionManager() {
            public int selectionForKey(final char key, final ComboBoxModel model) {
                return -1;
            }
        });
        assertFalse(comboBox.selectWithKeyChar('a'));
        assertEquals("b1", comboBox.getSelectedItem());
        assertFalse(comboBox.selectWithKeyChar('b'));
        assertEquals("b1", comboBox.getSelectedItem());
        comboBox.setKeySelectionManager(new JComboBox.KeySelectionManager() {
            public int selectionForKey(final char key, final ComboBoxModel model) {
                return 1;
            }
        });
        assertTrue(comboBox.selectWithKeyChar('a'));
        assertEquals("a2", comboBox.getSelectedItem());
        assertTrue(comboBox.selectWithKeyChar('b'));
        assertEquals("a2", comboBox.getSelectedItem());
        assertTrue(comboBox.selectWithKeyChar('c'));
        assertEquals("a2", comboBox.getSelectedItem());
    }

    public void testGetSetKeySelectionManager() throws Exception {
        assertNotNull(comboBox.getKeySelectionManager());
        JComboBox.KeySelectionManager manager = new JComboBox.KeySelectionManager() {
            public int selectionForKey(final char key, final ComboBoxModel model) {
                return 0;
            }
        };
        comboBox.setKeySelectionManager(manager);
        assertEquals(manager, comboBox.getKeySelectionManager());
        assertFalse(propertyChangeController.isChanged());
    }

    public void testCreateActionPropertyChangeListener() throws Exception {
        Action action1 = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        comboBox.setAction(action1);
        assertTrue(comboBox.isEnabled());
        action1.setEnabled(false);
        assertFalse(comboBox.isEnabled());
        action1.setEnabled(true);
        assertTrue(comboBox.isEnabled());
        Action action2 = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        comboBox.setAction(action2);
        action1.setEnabled(false);
        assertTrue(comboBox.isEnabled());
        action2.setEnabled(false);
        assertFalse(comboBox.isEnabled());
        action2.setEnabled(true);
        assertTrue(comboBox.isEnabled());
        comboBox.setAction(null);
        assertTrue(comboBox.isEnabled());
        action2.setEnabled(false);
        assertTrue(comboBox.isEnabled());
    }

    public void testConfigurePropertiesFromAction() throws Exception {
        comboBox.setToolTipText("combo tooltip");
        comboBox.setEnabled(false);
        assertEquals("combo tooltip", comboBox.getToolTipText());
        assertFalse(comboBox.isEnabled());
        Action action = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "action tooltip");
        comboBox.setAction(action);
        assertEquals("action tooltip", comboBox.getToolTipText());
        assertTrue(comboBox.isEnabled());
        comboBox.setAction(null);
        assertNull(comboBox.getToolTipText());
        assertTrue(comboBox.isEnabled());
    }

    public void testInstallAncestorListener() throws Exception {
        assertEquals(1, comboBox.getAncestorListeners().length);
    }
    
    public void testHarmony5223() {
        ComboBoxEditor editor = new NullComboBoxEditor();
        comboBox.setEditor(editor);
        assertEquals(editor, comboBox.getEditor());
    }
    
    public class NullComboBoxEditor extends BasicComboBoxEditor {
        public NullComboBoxEditor() {
            super();
        }

        public Component getEditorComponent() {
            return null;
        }
    }


    private class ActionController implements ActionListener {
        private ActionEvent event;

        public void actionPerformed(final ActionEvent e) {
            event = e;
        }

        public void reset() {
            event = null;
        }

        public ActionEvent getEvent() {
            return event;
        }
    }

    private class TestAction extends AbstractAction {
        private List<ActionEvent> events = new ArrayList<ActionEvent>();

        public void actionPerformed(final ActionEvent e) {
            events.add(e);
        }

        public void reset() {
            events.clear();
        }

        public List<ActionEvent> getEvents() {
            return events;
        }
    }

    private class ItemController implements ItemListener {
        private List<ItemEvent> eventList = new ArrayList<ItemEvent>();

        public void itemStateChanged(final ItemEvent e) {
            eventList.add(e);
        }

        public void reset() {
            eventList.clear();
        }

        public List<ItemEvent> getEvents() {
            return eventList;
        }
    }

    private static class PopupMenuController implements PopupMenuListener {
        public static final int CANCELLED = 0;

        public static final int VISIBLE = 1;

        public static final int INVISIBLE = 2;

        private PopupMenuEvent event;

        private int eventType = -1;

        public void reset() {
            event = null;
            eventType = -1;
        }

        public PopupMenuEvent getEvent() {
            return event;
        }

        public int getEventType() {
            return eventType;
        }

        public void popupMenuCanceled(final PopupMenuEvent e) {
            event = e;
            eventType = CANCELLED;
        }

        public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
            event = e;
            eventType = INVISIBLE;
        }

        public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
            event = e;
            eventType = VISIBLE;
        }
    }

    @SuppressWarnings("deprecation")
    private void createVisibleComboBox() {
        frame = new JFrame();
        frame.getContentPane().add(comboBox);
        frame.show();
    }
}
