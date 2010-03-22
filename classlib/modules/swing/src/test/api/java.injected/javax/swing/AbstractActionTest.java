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
 * Created on 16.12.2004

 */
package javax.swing;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;

public class AbstractActionTest extends SwingTestCase {
    protected boolean find(final Object[] array, final Object value) {
        boolean found = false;
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].equals(value)) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    protected AbstractAction action;

    public static class ConcreteSerializableListener implements PropertyChangeListener {
        public ConcreteSerializableListener() {
        }

        public ConcreteSerializableListener(final String name) {
        }

        private void writeObject(final ObjectOutputStream outStream) throws IOException {
            outStream.defaultWriteObject();
        }

        private void readObject(final ObjectInputStream inStream) throws IOException,
                ClassNotFoundException {
            inStream.defaultReadObject();
        }

        public void propertyChange(PropertyChangeEvent event) {
        }
    };

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        action = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent event) {
            }

            private void writeObject(final ObjectOutputStream outStream) throws IOException {
            }

            private void readObject(final ObjectInputStream inStream) throws IOException,
                    ClassNotFoundException {
            }

            @Override
            public Object clone() throws CloneNotSupportedException {
                return super.clone();
            }
        };
    }

    /*
     * Class under test for void AbstractAction(String, Icon)
     */
    public void testAbstractActionStringIcon() {
        Icon icon = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        action = new AbstractAction("ActionName", icon) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent event) {
            }
        };
        assertEquals("icon initialized properly", icon, action.getValue(Action.SMALL_ICON));
        assertEquals("name initialized properly", "ActionName", action.getValue(Action.NAME));
        assertTrue("enabled property init state is true", action.isEnabled());
    }

    /*
     * Class under test for void AbstractAction(String)
     */
    public void testAbstractActionString() {
        action = new AbstractAction("ActionName") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent event) {
            }
        };
        assertNull("icon initialized properly", action.getValue(Action.SMALL_ICON));
        assertEquals("name initialized properly", "ActionName", action.getValue(Action.NAME));
        assertTrue("enabled property init state is true", action.isEnabled());
    }

    /*
     * Class under test for void AbstractAction()
     */
    public void testAbstractAction() {
        action = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent event) {
            }
        };
        assertNull("icon initialized properly", action.getValue(Action.SMALL_ICON));
        assertNull("name initialized properly", action.getValue(Action.NAME));
        assertTrue("enabled property init state is true", action.isEnabled());
    }

    public void testPutValue() {
        String name1 = "name1";
        String name2 = "name2";
        String value1 = "value1";
        String value2 = "value2";
        PropertyChangeController changeListener = new PropertyChangeController();
        assertNull("value is not stored initially", action.getValue(name1));
        assertNull("value is not stored initially", action.getValue(name2));
        action.addPropertyChangeListener(changeListener);
        action.putValue(name1, value1);
        assertEquals("value is stored properly", value1, action.getValue(name1));
        changeListener.checkLastPropertyFired(action, name1, null, value1);
        changeListener.reset();
        action.putValue(name2, value2);
        assertEquals("value is stored properly", value1, action.getValue(name1));
        assertEquals("value is stored properly", value2, action.getValue(name2));
        changeListener.checkLastPropertyFired(action, name2, null, value2);
        changeListener.reset();
        action.putValue(name1, null);
        assertNull("value is stored properly", action.getValue(name1));
        assertEquals("value is stored properly", value2, action.getValue(name2));
        changeListener.checkLastPropertyFired(action, name1, value1, null);
    }

    public void testGetValue() {
        String name1 = "name1";
        String name2 = "name2";
        String value1 = "value1";
        String value2 = "value2";
        assertNull("value is not stored initially", action.getValue(name1));
        assertNull("value is not stored initially", action.getValue(name2));
        action.putValue(name1, value1);
        assertEquals("value is stored properly", value1, action.getValue(name1));
        action.putValue(name2, value2);
        assertEquals("value is stored properly", value1, action.getValue(name1));
        assertEquals("value is stored properly", value2, action.getValue(name2));
        action.putValue(name1, null);
        assertNull("value is stored properly", action.getValue(name1));
        assertEquals("value is stored properly", value2, action.getValue(name2));
    }

    public void testGetKeys() {
        String name1 = "name1";
        String name2 = "name2";
        String name3 = "name3";
        String name4 = "name4";
        String value1 = "value1";
        String value2 = "value2";
        String value3 = "value3";
        String value4 = "value4";
        Object[] keys = action.getKeys();
        assertTrue("with default constructor the initial number of keys is 0", keys == null
                || keys.length == 0);
        action.putValue(name1, value1);
        action.putValue(name2, value2);
        action.putValue(name3, value3);
        action.putValue(name4, value4);
        keys = action.getKeys();
        assertTrue("the number of keys is correct", keys != null && keys.length == 4);
        assertTrue(find(keys, name1));
        assertTrue(find(keys, name2));
        assertTrue(find(keys, name3));
        assertTrue(find(keys, name4));
    }

    /*
     * Class under test for Object clone()
     */
    public void testClone() throws IllegalArgumentException, SecurityException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        AbstractAction actionClone = null;
        String name1 = "name1";
        String name2 = "name2";
        String value1 = "value1";
        String value2 = "value2";
        PropertyChangeController changeListener = new PropertyChangeController();
        action.addPropertyChangeListener(changeListener);
        action.putValue(name1, value1);
        action.putValue(name2, value2);
        actionClone = (AbstractAction) action.getClass().getMethod("clone").invoke(action);
        assertEquals("values in table coincide ", action.getValue(name1), actionClone
                .getValue(name1));
        assertEquals("values in table coincide ", action.getValue(name2), actionClone
                .getValue(name2));
        assertEquals("listeners coincide ", action.getPropertyChangeListeners().length,
                actionClone.getPropertyChangeListeners().length);
        assertEquals("listeners coincide ", action.getPropertyChangeListeners()[0], actionClone
                .getPropertyChangeListeners()[0]);
        assertTrue("listeners coincide ", action.getPropertyChangeListeners()[0] == actionClone
                .getPropertyChangeListeners()[0]);
    }

    public void testRemovePropertyChangeListener() {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        PropertyChangeController changeListener3 = new PropertyChangeController();
        PropertyChangeController changeListener4 = new PropertyChangeController();
        PropertyChangeListener[] listenersArray = null;
        action.addPropertyChangeListener(changeListener1);
        action.addPropertyChangeListener(changeListener2);
        listenersArray = action.getPropertyChangeListeners();
        assertTrue("listeners added successfully", listenersArray.length == 2);
        action.removePropertyChangeListener(changeListener1);
        listenersArray = action.getPropertyChangeListeners();
        assertTrue("listener is removed successfully", listenersArray.length == 1);
        assertEquals("it was the right listener that was removed", listenersArray[0],
                changeListener2);
        action.removePropertyChangeListener(changeListener2);
        listenersArray = action.getPropertyChangeListeners();
        assertTrue("listener is removed successfully", listenersArray.length == 0);
        action.addPropertyChangeListener(changeListener1);
        action.addPropertyChangeListener(changeListener2);
        action.addPropertyChangeListener(changeListener3);
        action.addPropertyChangeListener(changeListener4);
        listenersArray = action.getPropertyChangeListeners();
        assertTrue("listeners added successfully", listenersArray.length == 4);
        action.removePropertyChangeListener(changeListener3);
        listenersArray = action.getPropertyChangeListeners();
        assertTrue("listener is removed successfully", listenersArray.length == 3);
        assertTrue("it was the right listener that was removed", changeListener3
                .findMe(listenersArray) == 0);
    }

    public void testAddPropertyChangeListener() {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        PropertyChangeListener[] listenersArray = null;
        action.addPropertyChangeListener(changeListener1);
        listenersArray = action.getPropertyChangeListeners();
        assertTrue(listenersArray.length == 1);
        assertTrue(changeListener1.findMe(listenersArray) > 0);
        action.addPropertyChangeListener(changeListener2);
        listenersArray = action.getPropertyChangeListeners();
        assertTrue(listenersArray.length == 2);
        assertTrue(changeListener1.findMe(listenersArray) > 0);
        assertTrue(changeListener2.findMe(listenersArray) > 0);
        action.addPropertyChangeListener(changeListener2);
        listenersArray = action.getPropertyChangeListeners();
        assertTrue(listenersArray.length == 3);
    }

    public void testGetPropertyChangeListeners() {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        PropertyChangeListener[] listenersArray = null;
        listenersArray = action.getPropertyChangeListeners();
        assertTrue(listenersArray.length == 0);
        action.addPropertyChangeListener(changeListener1);
        listenersArray = action.getPropertyChangeListeners();
        assertTrue(listenersArray.length == 1);
        assertTrue(changeListener1.findMe(listenersArray) == 1);
        action.addPropertyChangeListener(changeListener2);
        listenersArray = action.getPropertyChangeListeners();
        assertTrue(listenersArray.length == 2);
        assertTrue(changeListener1.findMe(listenersArray) == 1);
        assertTrue(changeListener2.findMe(listenersArray) == 1);
    }

    /*
     * this method is being tested by testSetEnabled()
     */
    public void testFirePropertyChange() {
    }

    public void testSetEnabled() {
        class PropertyChangeListenerFalse implements PropertyChangeListener {
            public boolean isChanged = false;

            public void propertyChange(final PropertyChangeEvent evt) {
                if ("enabled".equals(evt.getPropertyName()) && (evt.getNewValue() != null)) {
                    assertTrue(evt.getNewValue().equals(Boolean.FALSE));
                    assertTrue(evt.getOldValue().equals(Boolean.TRUE));
                    isChanged = true;
                }
            }
        }
        ;
        class PropertyChangeListenerTrue implements PropertyChangeListener {
            public boolean isChanged = false;

            public void propertyChange(final PropertyChangeEvent evt) {
                if ("enabled".equals(evt.getPropertyName()) && (evt.getNewValue() != null)) {
                    assertTrue(evt.getNewValue().equals(Boolean.TRUE));
                    assertTrue(evt.getOldValue().equals(Boolean.FALSE));
                    isChanged = true;
                }
            }
        }
        ;
        PropertyChangeListenerFalse falseListener = new PropertyChangeListenerFalse();
        PropertyChangeListenerTrue trueListener = new PropertyChangeListenerTrue();
        action.addPropertyChangeListener(trueListener);
        action.setEnabled(true);
        assertFalse("state changing event's not fired", trueListener.isChanged);
        assertTrue("state's not changed", action.isEnabled());
        action.removePropertyChangeListener(trueListener);
        action.addPropertyChangeListener(falseListener);
        action.setEnabled(false);
        assertTrue("state changing event's fired", falseListener.isChanged);
        assertFalse("state's changed", action.isEnabled());
        action.removePropertyChangeListener(falseListener);
        action.addPropertyChangeListener(trueListener);
        action.setEnabled(true);
        assertTrue("state changing event's fired", trueListener.isChanged);
        assertTrue("state's changed", action.isEnabled());
    }

    public void testIsEnabled() {
        assertTrue("action is enabled initially", action.isEnabled());
        action.setEnabled(false);
        assertFalse("action is disabled", action.isEnabled());
        action.setEnabled(true);
        assertTrue("action is enabled", action.isEnabled());
    }

    public void testWriteObject() throws Exception {
        String name1 = "name1";
        String name2 = "name2";
        String value1 = "value1";
        String value2 = "value2";
        ConcreteSerializableListener changeListener = new ConcreteSerializableListener();
        action.addPropertyChangeListener(changeListener);
        action.putValue(name1, value1);
        action.putValue(name2, value2);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(output);
        so.writeObject(action);
        so.flush();
        assertTrue(output.size() > 1);
    }

    public void testReadObject() throws Exception {
        String name1 = "name1";
        String name2 = "name2";
        String value1 = "value1";
        String value2 = "value2";
        ConcreteSerializableListener changeListener = new ConcreteSerializableListener();
        action.addPropertyChangeListener(changeListener);
        action.putValue(name1, value1);
        action.putValue(name2, value2);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(output);
        so.writeObject(action);
        so.flush();
        assertTrue(output.size() > 1);
        ObjectInputStream si = new ObjectInputStream(new ByteArrayInputStream(output
                .toByteArray()));
        AbstractAction ressurectedAction = (AbstractAction) si.readObject();
        assertEquals("values in table coincide ", action.getValue(name1), ressurectedAction
                .getValue(name1));
        assertEquals("values in table coincide ", action.getValue(name2), ressurectedAction
                .getValue(name2));
        assertEquals("no listeners resurrected ", 0, ressurectedAction
                .getPropertyChangeListeners().length);
    }
}
