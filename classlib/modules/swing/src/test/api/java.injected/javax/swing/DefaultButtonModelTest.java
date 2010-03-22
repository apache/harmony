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
 * @author Alexander T. Simbirtsev
 * Created on 31.01.2005

 */
package javax.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EventListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class DefaultButtonModelTest extends SwingTestCase {
    protected int find(final Object[] array, final Object value) {
        int found = 0;
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].equals(value)) {
                    found++;
                }
            }
        }
        return found;
    }

    class ConcreteActionListener implements ActionListener {
        public ActionEvent eventHappened = null;

        public void actionPerformed(final ActionEvent event) {
            eventHappened = event;
        }
    };

    class ConcreteItemListener implements ItemListener {
        public ItemEvent eventHappened = null;

        public void itemStateChanged(final ItemEvent event) {
            eventHappened = event;
        }
    };

    class ConcreteChangeListener implements ChangeListener {
        public ChangeEvent eventHappened = null;

        public void stateChanged(final ChangeEvent event) {
            eventHappened = event;
        }
    };

    protected DefaultButtonModel buttonModel;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        buttonModel = new DefaultButtonModel();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        buttonModel = null;
    }

    public void testDefaultButtonModel() {
        assertEquals("default mnemonic ", 0, buttonModel.getMnemonic());
        assertNull("default action command ", buttonModel.getActionCommand());
        assertEquals("default actionListeners array ", 0,
                buttonModel.getActionListeners().length);
        assertEquals("default changeListeners array ", 0,
                buttonModel.getChangeListeners().length);
        assertEquals("default itemListeners array ", 0, buttonModel.getItemListeners().length);
        assertNull("default group ", buttonModel.getGroup());
        assertNull("default selected objects array ", buttonModel.getSelectedObjects());
        assertFalse("default armed state ", buttonModel.isArmed());
        assertTrue("default enabled state ", buttonModel.isEnabled());
        assertFalse("default pressed state ", buttonModel.isPressed());
        assertFalse("default rollover state ", buttonModel.isRollover());
        assertFalse("default selected state ", buttonModel.isSelected());
    }

    public void testGetListeners() {
        ChangeListener listener1 = new ConcreteChangeListener();
        ChangeListener listener2 = new ConcreteChangeListener();
        ChangeListener listener3 = new ConcreteChangeListener();
        ItemListener listener4 = new ConcreteItemListener();
        ItemListener listener5 = new ConcreteItemListener();
        ItemListener listener6 = new ConcreteItemListener();
        ActionListener listener7 = new ConcreteActionListener();
        ActionListener listener8 = new ConcreteActionListener();
        ActionListener listener9 = new ConcreteActionListener();
        EventListener[] listeners = buttonModel.getListeners(ChangeListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 0);
        buttonModel.addChangeListener(listener1);
        buttonModel.addChangeListener(listener2);
        listeners = buttonModel.getListeners(ChangeListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 2);
        assertTrue("listener's added successfully ", find(listeners, listener1) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener2) == 1);
        buttonModel.addChangeListener(listener3);
        buttonModel.addItemListener(listener5);
        buttonModel.addItemListener(listener4);
        listeners = buttonModel.getListeners(ChangeListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 3);
        assertTrue("listener's added successfully ", find(listeners, listener1) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener2) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener3) == 1);
        listeners = buttonModel.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 2);
        assertTrue("listener's added successfully ", find(listeners, listener4) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener5) == 1);
        buttonModel.addItemListener(listener6);
        buttonModel.addActionListener(listener7);
        buttonModel.addActionListener(listener8);
        buttonModel.addActionListener(listener9);
        listeners = buttonModel.getListeners(ChangeListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 3);
        assertTrue("listener's added successfully ", find(listeners, listener1) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener2) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener3) == 1);
        listeners = buttonModel.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 3);
        assertTrue("listener's added successfully ", find(listeners, listener4) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener5) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener6) == 1);
        listeners = buttonModel.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 3);
        assertTrue("listener's added successfully ", find(listeners, listener7) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener8) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener9) == 1);
    }

    public void testRemoveChangeListener() {
        ChangeListener listener1 = new ConcreteChangeListener();
        ChangeListener listener2 = new ConcreteChangeListener();
        ChangeListener listener3 = new ConcreteChangeListener();
        buttonModel.addChangeListener(listener1);
        buttonModel.addChangeListener(listener2);
        buttonModel.addChangeListener(listener2);
        buttonModel.addChangeListener(listener3);
        EventListener[] listeners = buttonModel.getListeners(ChangeListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 4);
        buttonModel.removeChangeListener(listener1);
        listeners = buttonModel.getListeners(ChangeListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 3);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 2);
        buttonModel.removeChangeListener(listener2);
        listeners = buttonModel.getListeners(ChangeListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 2);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 1);
        buttonModel.removeChangeListener(listener2);
        listeners = buttonModel.getListeners(ChangeListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 0);
        buttonModel.removeChangeListener(listener2);
        listeners = buttonModel.getListeners(ChangeListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 0);
        buttonModel.removeChangeListener(listener3);
        listeners = buttonModel.getListeners(ChangeListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 0);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 0);
    }

    public void testAddChangeListener() {
        ChangeListener listener1 = new ConcreteChangeListener();
        ChangeListener listener2 = new ConcreteChangeListener();
        buttonModel.addChangeListener(listener1);
        EventListener[] listeners = buttonModel.getListeners(ChangeListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 1);
        assertEquals("listener's added successfully ", listeners[0], listener1);
        buttonModel.addChangeListener(listener2);
        listeners = buttonModel.getListeners(ChangeListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 2);
        assertTrue("listener's added successfully ", find(listeners, listener1) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener2) == 1);
        buttonModel.addChangeListener(listener2);
        listeners = buttonModel.getListeners(ChangeListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 3);
        assertTrue("listener's added successfully ", find(listeners, listener1) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener2) == 2);
    }

    /**
     * this method is being tested mostly with testAddChangeListener()
     * and testRemoveChangeListener()
     */
    public void testGetChangeListeners() {
        EventListener[] listeners = buttonModel.getListeners(ChangeListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 0);
    }

    public void testSetGroup() {
        ButtonGroup group1 = new ButtonGroup();
        ButtonGroup group2 = new ButtonGroup();
        ButtonGroup group3 = new ButtonGroup();
        assertNull("default group ", buttonModel.getGroup());
        buttonModel.setGroup(group1);
        assertEquals("group ", group1, buttonModel.getGroup());
        buttonModel.setGroup(group2);
        assertEquals("group ", group2, buttonModel.getGroup());
        buttonModel.setGroup(group3);
        assertEquals("group ", group3, buttonModel.getGroup());
    }

    public void testGetGroup() {
        assertNull("default group ", buttonModel.getGroup());
    }

    public void testSetActionCommand() {
        String command1 = "command1";
        String command2 = "command2";
        String command3 = "command3";
        assertNull("default action command ", buttonModel.getActionCommand());
        buttonModel.setActionCommand(command1);
        assertEquals("action command ", command1, buttonModel.getActionCommand());
        buttonModel.setActionCommand(command2);
        assertEquals("action command ", command2, buttonModel.getActionCommand());
        buttonModel.setActionCommand(command3);
        assertEquals("action command ", command3, buttonModel.getActionCommand());
    }

    public void testGetActionCommand() {
        assertNull("default action command ", buttonModel.getActionCommand());
    }

    public void testGetSelectedObjects() {
        assertNull("selected objects ", buttonModel.getSelectedObjects());
    }

    public void testRemoveItemListener() {
        ItemListener listener1 = new ConcreteItemListener();
        ItemListener listener2 = new ConcreteItemListener();
        ItemListener listener3 = new ConcreteItemListener();
        buttonModel.addItemListener(listener1);
        buttonModel.addItemListener(listener2);
        buttonModel.addItemListener(listener2);
        buttonModel.addItemListener(listener3);
        EventListener[] listeners = buttonModel.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 4);
        buttonModel.removeItemListener(listener1);
        listeners = buttonModel.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 3);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 2);
        buttonModel.removeItemListener(listener2);
        listeners = buttonModel.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 2);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 1);
        buttonModel.removeItemListener(listener2);
        listeners = buttonModel.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 0);
        buttonModel.removeItemListener(listener2);
        listeners = buttonModel.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 0);
        buttonModel.removeItemListener(listener3);
        listeners = buttonModel.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 0);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 0);
    }

    public void testAddItemListener() {
        ItemListener listener1 = new ConcreteItemListener();
        ItemListener listener2 = new ConcreteItemListener();
        buttonModel.addItemListener(listener1);
        EventListener[] listeners = buttonModel.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 1);
        assertEquals("listener's added successfully ", listeners[0], listener1);
        buttonModel.addItemListener(listener2);
        listeners = buttonModel.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 2);
        assertTrue("listener's added successfully ", find(listeners, listener1) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener2) == 1);
        buttonModel.addItemListener(listener2);
        listeners = buttonModel.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 3);
        assertTrue("listener's added successfully ", find(listeners, listener1) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener2) == 2);
    }

    /**
     * this method is being tested mostly with testAddItemListener()
     * and testRemoveItemListener()
     */
    public void testGetItemListeners() {
        EventListener[] listeners = buttonModel.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 0);
    }

    public void testFireItemStateChanged() {
        Object item1 = "item1";
        Object item2 = "item2";
        ItemEvent event1 = new ItemEvent(buttonModel, 11, item1, 2);
        ItemEvent event2 = new ItemEvent(buttonModel, 111, item2, 1);
        ConcreteItemListener listener1 = new ConcreteItemListener();
        ConcreteItemListener listener2 = new ConcreteItemListener();
        buttonModel.addItemListener(listener1);
        buttonModel.addItemListener(listener2);
        buttonModel.fireItemStateChanged(event1);
        assertEquals("event fired ", event1, listener1.eventHappened);
        assertEquals("event fired ", event1, listener2.eventHappened);
        buttonModel.fireItemStateChanged(event2);
        assertEquals("event fired ", event2, listener1.eventHappened);
        assertEquals("event fired ", event2, listener2.eventHappened);
        buttonModel.fireItemStateChanged(null);
        assertNull("event fired ", listener1.eventHappened);
        assertNull("event fired ", listener2.eventHappened);
    }

    public void testFireActionPerformed1() {
        String command1 = "command1";
        String command2 = "command2";
        ActionEvent event1 = new ActionEvent(buttonModel, 11, command1, 2);
        ActionEvent event2 = new ActionEvent(buttonModel, 111, command2, 1);
        ConcreteActionListener listener1 = new ConcreteActionListener();
        ConcreteActionListener listener2 = new ConcreteActionListener();
        buttonModel.addActionListener(listener1);
        buttonModel.addActionListener(listener2);
        buttonModel.fireActionPerformed(event1);
        assertEquals("event fired ", event1, listener1.eventHappened);
        assertEquals("event fired ", event1, listener2.eventHappened);
        buttonModel.fireActionPerformed(event2);
        assertEquals("event fired ", event2, listener1.eventHappened);
        assertEquals("event fired ", event2, listener2.eventHappened);
        buttonModel.fireActionPerformed(null);
        assertNull("event fired ", listener1.eventHappened);
        assertNull("event fired ", listener2.eventHappened);
    }

    public void testFireActionPerformed2() {
        ConcreteActionListener listener1 = new ConcreteActionListener();
        ConcreteActionListener listener2 = new ConcreteActionListener();
        buttonModel.addActionListener(listener1);
        buttonModel.addActionListener(listener2);
        buttonModel.setPressed(true);
        buttonModel.setArmed(true);
        assertNull("actionListeners aren't triggered", listener1.eventHappened);
        assertNull("actionListeners aren't triggered", listener1.eventHappened);
        buttonModel.setPressed(false);
        assertTrue("actionListeners are triggered", listener1.eventHappened != null);
        assertTrue("actionListeners are triggered", listener2.eventHappened != null);
    }

    public void testFireStateChanged() {
        ChangeEvent event1 = null;
        ChangeEvent event2 = null;
        ConcreteChangeListener listener1 = new ConcreteChangeListener();
        ConcreteChangeListener listener2 = new ConcreteChangeListener();
        buttonModel.addChangeListener(listener1);
        buttonModel.addChangeListener(listener2);
        buttonModel.fireStateChanged();
        event1 = listener1.eventHappened;
        assertTrue("event fired ", listener1.eventHappened != null);
        assertTrue("event fired ", listener2.eventHappened != null);
        assertTrue("one event fired ", listener1.eventHappened == listener2.eventHappened);
        assertEquals("event fired properly ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired properly ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        assertEquals("event fired properly ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired properly ", buttonModel, listener2.eventHappened.getSource());
        buttonModel.fireStateChanged();
        event2 = listener1.eventHappened;
        assertTrue("event fired ", listener1.eventHappened != null);
        assertTrue("event fired ", listener2.eventHappened != null);
        assertTrue("one event fired ", listener1.eventHappened == listener2.eventHappened);
        assertEquals("event fired properly ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired properly ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        assertEquals("event fired properly ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired properly ", buttonModel, listener2.eventHappened.getSource());
        assertTrue("the same event is fired always ", event1 == event2);
    }

    public void testRemoveActionListener() {
        ActionListener listener1 = new ConcreteActionListener();
        ActionListener listener2 = new ConcreteActionListener();
        ActionListener listener3 = new ConcreteActionListener();
        buttonModel.addActionListener(listener1);
        buttonModel.addActionListener(listener2);
        buttonModel.addActionListener(listener2);
        buttonModel.addActionListener(listener3);
        EventListener[] listeners = buttonModel.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 4);
        buttonModel.removeActionListener(listener1);
        listeners = buttonModel.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 3);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 2);
        buttonModel.removeActionListener(listener2);
        listeners = buttonModel.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 2);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 1);
        buttonModel.removeActionListener(listener2);
        listeners = buttonModel.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 0);
        buttonModel.removeActionListener(listener2);
        listeners = buttonModel.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 0);
        buttonModel.removeActionListener(listener3);
        listeners = buttonModel.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 0);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 0);
    }

    public void testAddActionListener() {
        ActionListener listener1 = new ConcreteActionListener();
        ActionListener listener2 = new ConcreteActionListener();
        buttonModel.addActionListener(listener1);
        EventListener[] listeners = buttonModel.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 1);
        assertEquals("listener's added successfully ", listeners[0], listener1);
        buttonModel.addActionListener(listener2);
        listeners = buttonModel.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 2);
        assertTrue("listener's added successfully ", find(listeners, listener1) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener2) == 1);
        buttonModel.addActionListener(listener2);
        listeners = buttonModel.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 3);
        assertTrue("listener's added successfully ", find(listeners, listener1) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener2) == 2);
    }

    /**
     * this method is being tested mostly with testAddActionListener()
     * and testRemoveActionListener()
     */
    public void testGetActionListeners() {
        EventListener[] listeners = buttonModel.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 0);
    }

    public void testSetSelected() {
        boolean value1 = true;
        boolean value2 = false;
        ConcreteChangeListener listener1 = new ConcreteChangeListener();
        ConcreteChangeListener listener2 = new ConcreteChangeListener();
        ConcreteItemListener listener3 = new ConcreteItemListener();
        ConcreteItemListener listener4 = new ConcreteItemListener();
        buttonModel.addChangeListener(listener1);
        buttonModel.addChangeListener(listener2);
        buttonModel.addItemListener(listener3);
        buttonModel.addItemListener(listener4);
        buttonModel.setSelected(value1);
        assertEquals("Selected ", value1, buttonModel.isSelected());
        assertEquals("event fired source ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener2.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener3.eventHappened.getItem());
        assertEquals("event fired value ", 1, listener3.eventHappened.getStateChange());
        assertEquals("event fired source ", buttonModel, listener4.eventHappened.getItem());
        assertEquals("event fired value ", 1, listener4.eventHappened.getStateChange());
        buttonModel.setSelected(value2);
        assertEquals("Selected ", value2, buttonModel.isSelected());
        assertEquals("event fired source ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener2.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener3.eventHappened.getItem());
        assertEquals("event fired value ", 2, listener3.eventHappened.getStateChange());
        assertEquals("event fired source ", buttonModel, listener4.eventHappened.getItem());
        assertEquals("event fired value ", 2, listener4.eventHappened.getStateChange());
        buttonModel.setSelected(value1);
        assertEquals("Selected ", value1, buttonModel.isSelected());
        assertEquals("event fired source ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener2.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener3.eventHappened.getItem());
        assertEquals("event fired value ", 1, listener3.eventHappened.getStateChange());
        assertEquals("event fired source ", buttonModel, listener4.eventHappened.getItem());
        assertEquals("event fired value ", 1, listener4.eventHappened.getStateChange());
        buttonModel.setSelected(value2);
        assertEquals("Selected ", value2, buttonModel.isSelected());
        assertEquals("event fired source ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener2.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener3.eventHappened.getItem());
        assertEquals("event fired value ", 2, listener3.eventHappened.getStateChange());
        assertEquals("event fired source ", buttonModel, listener4.eventHappened.getItem());
        assertEquals("event fired value ", 2, listener4.eventHappened.getStateChange());
        listener1.eventHappened = null;
        listener2.eventHappened = null;
        listener3.eventHappened = null;
        listener4.eventHappened = null;
        buttonModel.setSelected(value2);
        assertEquals("Selected  ", value2, buttonModel.isSelected());
        assertNull("event wasn't fired ", listener1.eventHappened);
        assertNull("event wasn't fired ", listener2.eventHappened);
        assertNull("event wasn't fired ", listener3.eventHappened);
        assertNull("event wasn't fired ", listener4.eventHappened);
        buttonModel.setEnabled(true);
        buttonModel.setSelected(false);
        buttonModel.setEnabled(false);
        buttonModel.setSelected(true);
        assertTrue(buttonModel.isSelected());
    }

    public void testSetRollover() {
        boolean value1 = true;
        boolean value2 = false;
        ConcreteChangeListener listener1 = new ConcreteChangeListener();
        ConcreteChangeListener listener2 = new ConcreteChangeListener();
        buttonModel.addChangeListener(listener1);
        buttonModel.addChangeListener(listener2);
        buttonModel.setRollover(value1);
        assertEquals("Rollover ", value1, buttonModel.isRollover());
        assertEquals("event fired source ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener2.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        buttonModel.setRollover(value2);
        assertEquals("Rollover ", value2, buttonModel.isRollover());
        assertEquals("event fired source ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener2.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        listener1.eventHappened = null;
        listener2.eventHappened = null;
        buttonModel.setRollover(value2);
        assertEquals("Rollover ", value2, buttonModel.isRollover());
        assertNull("event wasn't fired ", listener1.eventHappened);
        assertNull("event wasn't fired ", listener2.eventHappened);
        buttonModel.setEnabled(true);
        buttonModel.setRollover(false);
        buttonModel.setEnabled(false);
        buttonModel.setRollover(true);
        assertFalse(buttonModel.isRollover());
    }

    public void testSetPressed() {
        boolean value1 = true;
        boolean value2 = false;
        ConcreteChangeListener listener1 = new ConcreteChangeListener();
        ConcreteChangeListener listener2 = new ConcreteChangeListener();
        ConcreteActionListener listener3 = new ConcreteActionListener();
        buttonModel.addChangeListener(listener1);
        buttonModel.addChangeListener(listener2);
        buttonModel.addActionListener(listener3);
        buttonModel.setPressed(value1);
        assertEquals("Pressed ", value1, buttonModel.isPressed());
        assertEquals("event fired source ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener2.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        buttonModel.setPressed(value2);
        assertEquals("Pressed ", value2, buttonModel.isPressed());
        assertEquals("event fired source ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener2.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        listener1.eventHappened = null;
        listener2.eventHappened = null;
        buttonModel.setPressed(value2);
        assertEquals("Pressed ", value2, buttonModel.isPressed());
        assertNull("event wasn't fired ", listener1.eventHappened);
        assertNull("event wasn't fired ", listener2.eventHappened);
        buttonModel.setArmed(true);
        buttonModel.setPressed(true);
        listener3.eventHappened = null;
        buttonModel.setPressed(false);
        assertNotNull(listener3.eventHappened);
        buttonModel.setEnabled(true);
        buttonModel.setPressed(false);
        buttonModel.setEnabled(false);
        buttonModel.setPressed(true);
        assertFalse(buttonModel.isPressed());
    }

    public void testSetEnabled1() {
        boolean value1 = false;
        boolean value2 = true;
        ConcreteChangeListener listener1 = new ConcreteChangeListener();
        ConcreteChangeListener listener2 = new ConcreteChangeListener();
        buttonModel.addChangeListener(listener1);
        buttonModel.addChangeListener(listener2);
        buttonModel.setEnabled(value1);
        assertEquals("Enabled ", value1, buttonModel.isEnabled());
        assertEquals("event fired source ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener2.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        buttonModel.setEnabled(value2);
        assertEquals("Enabled ", value2, buttonModel.isEnabled());
        assertEquals("event fired source ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener2.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        listener1.eventHappened = null;
        listener2.eventHappened = null;
        buttonModel.setEnabled(value2);
        assertEquals("Enabled ", value2, buttonModel.isEnabled());
        assertNull("event wasn't fired ", listener1.eventHappened);
        assertNull("event wasn't fired ", listener2.eventHappened);
        buttonModel.setEnabled(true);
    }

    public void testSetEnabled2() {
        buttonModel.setEnabled(true);
        buttonModel.setPressed(true);
        buttonModel.setRollover(true);
        buttonModel.setSelected(true);
        buttonModel.setArmed(true);
        buttonModel.setEnabled(false);
        assertFalse(buttonModel.isArmed());
        assertFalse(buttonModel.isPressed());
        assertTrue(buttonModel.isSelected());
        if (isHarmony()) {
            assertFalse(buttonModel.isRollover());
        } else {
            assertTrue(buttonModel.isRollover());
        }
    }

    public void testSetArmed() {
        boolean value1 = true;
        boolean value2 = false;
        ConcreteChangeListener listener1 = new ConcreteChangeListener();
        ConcreteChangeListener listener2 = new ConcreteChangeListener();
        buttonModel.addChangeListener(listener1);
        buttonModel.addChangeListener(listener2);
        buttonModel.setArmed(value1);
        assertEquals("Armed ", value1, buttonModel.isArmed());
        assertEquals("event fired source ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener2.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        buttonModel.setArmed(value2);
        assertEquals("Armed ", value2, buttonModel.isArmed());
        assertEquals("event fired source ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener2.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        listener1.eventHappened = null;
        listener2.eventHappened = null;
        buttonModel.setArmed(value2);
        assertEquals("Armed ", value2, buttonModel.isArmed());
        assertNull("event wasn't fired ", listener1.eventHappened);
        assertNull("event wasn't fired ", listener2.eventHappened);
        buttonModel.setEnabled(true);
        buttonModel.setArmed(false);
        buttonModel.setEnabled(false);
        buttonModel.setArmed(true);
        assertFalse(buttonModel.isArmed());
    }

    public void testSetMnemonic() {
        int value1 = 123;
        int value2 = 234;
        ConcreteChangeListener listener1 = new ConcreteChangeListener();
        ConcreteChangeListener listener2 = new ConcreteChangeListener();
        buttonModel.addChangeListener(listener1);
        buttonModel.addChangeListener(listener2);
        buttonModel.setMnemonic(value1);
        assertEquals("mnemonic ", value1, buttonModel.getMnemonic());
        assertEquals("event fired source ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener2.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        buttonModel.setMnemonic(value2);
        assertEquals("mnemonic ", value2, buttonModel.getMnemonic());
        assertEquals("event fired source ", buttonModel, listener1.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired source ", buttonModel, listener2.eventHappened.getSource());
        assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        listener1.eventHappened = null;
        listener2.eventHappened = null;
        buttonModel.setMnemonic(value2);
        assertEquals("mnemonic ", value2, buttonModel.getMnemonic());
        if (!isHarmony()) {
            assertEquals("event fired source ", buttonModel, listener1.eventHappened
                    .getSource());
            assertEquals("event fired class ", ChangeEvent.class, listener1.eventHappened
                    .getClass());
            assertEquals("event fired source ", buttonModel, listener2.eventHappened
                    .getSource());
            assertEquals("event fired class ", ChangeEvent.class, listener2.eventHappened
                    .getClass());
        } else {
            assertNull("event hasn't fired", listener1.eventHappened);
            assertNull("event hasn't fired", listener2.eventHappened);
        }
    }

    public void testIsSelected() {
        assertFalse("default selected state ", buttonModel.isSelected());
    }

    public void testIsRollover() {
        assertFalse("default rollover state ", buttonModel.isRollover());
    }

    public void testIsPressed() {
        assertFalse("default pressed state ", buttonModel.isPressed());
    }

    public void testIsEnabled() {
        assertTrue("default enabled state ", buttonModel.isEnabled());
    }

    public void testIsArmed() {
        assertFalse("default armed state ", buttonModel.isArmed());
    }

    public void testGetMnemonic() {
        assertEquals("default mnemonic ", 0, buttonModel.getMnemonic());
    }

    public void testWriteObject() throws Exception {
        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(fo);
        so.writeObject(buttonModel);
        so.flush();
    }

    public void testReadObject() throws Exception {
        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(fo);
        so.writeObject(buttonModel);
        so.flush();
        InputStream fi = new ByteArrayInputStream(fo.toByteArray());
        ObjectInputStream si = new ObjectInputStream(fi);
        DefaultButtonModel ressurectedButtonModel = (DefaultButtonModel) si.readObject();
        assertNotNull(ressurectedButtonModel);
    }
}
