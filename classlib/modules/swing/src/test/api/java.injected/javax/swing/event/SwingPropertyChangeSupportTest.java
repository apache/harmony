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
 */
package javax.swing.event;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;
import java.beans.VetoableChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.swing.SwingTestCase;

public class SwingPropertyChangeSupportTest extends SwingTestCase {
    public static class FindableListener {
        public String valueChangedKey = "";

        public Object valueChangedOld = null;

        public Object valueChangedNew = null;

        public void reset() {
            valueChangedKey = "";
            valueChangedOld = null;
            valueChangedNew = null;
        }

        public int findMe(final Object[] listenersArray) {
            int found = 0;
            for (int i = 0; i < listenersArray.length; i++) {
                if (listenersArray[i] == this) {
                    found++;
                }
            }
            return found;
        }
    }

    public static class ConcreteVetoableChangeListener extends FindableListener implements
            VetoableChangeListener {
        public void vetoableChange(final PropertyChangeEvent evt) {
            valueChangedKey = evt.getPropertyName();
            valueChangedOld = evt.getOldValue();
            valueChangedNew = evt.getNewValue();
        }
    };

    public class ConcretePropertyChangeListener extends PropertyChangeController implements
            Serializable {
        private static final long serialVersionUID = 1L;

        public ConcretePropertyChangeListener() {
            super();
        }

        public int findMyProxy(final Object[] listenersArray, final String property) {
            int found = 0;
            for (int i = 0; i < listenersArray.length; i++) {
                Object curListener = listenersArray[i];
                if (curListener instanceof PropertyChangeListenerProxy) {
                    PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) curListener;
                    if (proxy.getListener() == this && proxy.getPropertyName().equals(property)) {
                        found++;
                    }
                }
            }
            return found;
        }

        private void writeObject(final ObjectOutputStream outStream) throws IOException {
            outStream.defaultWriteObject();
        }

        private void readObject(final ObjectInputStream inStream) throws IOException,
                ClassNotFoundException {
            inStream.defaultReadObject();
        }
    };

    public static class SerializableListener implements PropertyChangeListener, Serializable {
        private static final long serialVersionUID = 1L;

        private String name;

        public SerializableListener() {
            super();
            name = "";
        }

        public SerializableListener(final String name) {
            super();
            this.name = name;
        }

        private void writeObject(final ObjectOutputStream outStream) throws IOException {
            outStream.defaultWriteObject();
            outStream.writeObject(name);
        }

        private void readObject(final ObjectInputStream inStream) throws IOException,
                ClassNotFoundException {
            inStream.defaultReadObject();
            name = (String) inStream.readObject();
        }

        public void propertyChange(PropertyChangeEvent e) {
        }
    };

    protected Object panel;

    protected PropertyChangeSupport propertyChangeSupport;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        panel = new SerializableListener("Panel");
        propertyChangeSupport = new SwingPropertyChangeSupport(panel);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        panel = null;
        propertyChangeSupport = null;
        super.tearDown();
    }

    /*
     * Class under test for void firePropertyChange(String, Object, Object)
     */
    public void testFirePropertyChangeStringObjectObject() {
        ConcretePropertyChangeListener changeListener1 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener2 = new ConcretePropertyChangeListener();
        propertyChangeSupport.addPropertyChangeListener("first", changeListener1);
        propertyChangeSupport.addPropertyChangeListener("second", changeListener2);
        String oldValue = "old";
        String newValue = "new";
        propertyChangeSupport.firePropertyChange("first", oldValue, newValue);
        changeListener1.checkLastPropertyFired(panel, "first", oldValue, newValue);
        assertFalse(changeListener2.isChanged());
        changeListener1.reset();
        changeListener2.reset();
        propertyChangeSupport.firePropertyChange("second", oldValue, newValue);
        assertFalse(changeListener1.isChanged());
        changeListener2.checkLastPropertyFired(panel, "second", oldValue, newValue);
        changeListener1.reset();
        changeListener2.reset();
        propertyChangeSupport.addPropertyChangeListener("first", changeListener2);
        propertyChangeSupport.firePropertyChange("first", oldValue, newValue);
        changeListener1.checkLastPropertyFired(panel, "first", oldValue, newValue);
        changeListener2.checkLastPropertyFired(panel, "first", oldValue, newValue);
        changeListener1.reset();
        changeListener2.reset();
        propertyChangeSupport.removePropertyChangeListener("first", changeListener2);
        propertyChangeSupport.addPropertyChangeListener(changeListener2);
        propertyChangeSupport.firePropertyChange("first", oldValue, newValue);
        changeListener1.checkLastPropertyFired(panel, "first", oldValue, newValue);
        changeListener2.checkLastPropertyFired(panel, "first", oldValue, newValue);
        changeListener1.reset();
        changeListener2.reset();
        propertyChangeSupport.firePropertyChange("second", oldValue, newValue);
        assertFalse(changeListener1.isChanged());
        changeListener2.checkLastPropertyFired(panel, "second", oldValue, newValue);
        changeListener1.reset();
        changeListener2.reset();
        propertyChangeSupport.firePropertyChange("second", newValue, newValue);
        assertFalse(changeListener1.isChanged());
        assertFalse(changeListener2.isChanged());
        changeListener1.reset();
        changeListener2.reset();
        propertyChangeSupport.firePropertyChange("second", null, null);
        assertFalse(changeListener1.isChanged());
        changeListener2.checkLastPropertyFired(panel, "second", null, null);
    }

    /*
     * Class under test for void removePropertyChangeListener(String, PropertyChangeListener)
     */
    public void testRemovePropertyChangeListenerStringPropertyChangeListener() {
        ConcretePropertyChangeListener changeListener1 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener2 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener3 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener4 = new ConcretePropertyChangeListener();
        PropertyChangeListener[] propertyListeners = null;
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 0);
        propertyChangeSupport.addPropertyChangeListener("first", changeListener1);
        propertyChangeSupport.addPropertyChangeListener("second", changeListener2);
        propertyChangeSupport.addPropertyChangeListener(changeListener3);
        propertyChangeSupport.addPropertyChangeListener("first", changeListener1);
        propertyChangeSupport.addPropertyChangeListener("first", changeListener2);
        propertyChangeSupport.addPropertyChangeListener(changeListener4);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 6);
        propertyChangeSupport.removePropertyChangeListener("third", changeListener3);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 6);
        assertTrue(changeListener3.findMe(propertyListeners) == 1);
        propertyChangeSupport.removePropertyChangeListener("first", changeListener2);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 5);
        assertTrue(changeListener2.findMyProxy(propertyListeners, "second") == 1);
        assertTrue(changeListener1.findMyProxy(propertyListeners, "first") == 2);
        propertyChangeSupport.removePropertyChangeListener(changeListener1);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 5);
        assertTrue(changeListener1.findMyProxy(propertyListeners, "first") == 2);
        propertyChangeSupport.removePropertyChangeListener("first", changeListener1);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 4);
        assertTrue(changeListener1.findMyProxy(propertyListeners, "first") == 1);
        propertyChangeSupport.removePropertyChangeListener("first", changeListener1);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 3);
        assertTrue(changeListener1.findMyProxy(propertyListeners, "first") == 0);
    }

    /*
     * Class under test for void addPropertyChangeListener(String, PropertyChangeListener)
     */
    public void testAddPropertyChangeListenerStringPropertyChangeListener() {
        ConcretePropertyChangeListener changeListener1 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener2 = new ConcretePropertyChangeListener();
        PropertyChangeListener[] propertyListeners = null;
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 0);
        propertyChangeSupport.addPropertyChangeListener("first", changeListener1);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 1);
        assertTrue(changeListener1.findMyProxy(propertyListeners, "first") == 1);
        propertyChangeSupport.addPropertyChangeListener("second", changeListener2);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 2);
        assertTrue(changeListener1.findMyProxy(propertyListeners, "first") == 1);
        assertTrue(changeListener2.findMyProxy(propertyListeners, "second") == 1);
        propertyChangeSupport.addPropertyChangeListener("third", changeListener2);
        propertyChangeSupport.addPropertyChangeListener(changeListener1);
        propertyChangeSupport.addPropertyChangeListener("first", changeListener1);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 5);
        assertTrue(changeListener1.findMyProxy(propertyListeners, "first") == 2);
        assertTrue(changeListener2.findMyProxy(propertyListeners, "second") == 1);
        assertTrue(changeListener2.findMyProxy(propertyListeners, "third") == 1);
    }

    /*
     * Class under test for PropertyChangeListener[] getPropertyChangeListeners(String)
     */
    public void testGetPropertyChangeListenersString() {
        ConcretePropertyChangeListener changeListener1 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener2 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener3 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener4 = new ConcretePropertyChangeListener();
        PropertyChangeListener[] propertyListeners = null;
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners("first");
        assertTrue(propertyListeners != null && propertyListeners.length == 0);
        propertyChangeSupport.addPropertyChangeListener("first", changeListener1);
        propertyChangeSupport.addPropertyChangeListener("second", changeListener1);
        propertyChangeSupport.addPropertyChangeListener(changeListener2);
        propertyChangeSupport.addPropertyChangeListener(changeListener3);
        propertyChangeSupport.addPropertyChangeListener("second", changeListener4);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners("first");
        assertTrue(propertyListeners != null && propertyListeners.length == 1);
        assertTrue(changeListener1.findMe(propertyListeners) == 1);
        assertTrue(changeListener2.findMe(propertyListeners) == 0);
        assertTrue(changeListener3.findMe(propertyListeners) == 0);
        assertTrue(changeListener4.findMe(propertyListeners) == 0);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners("second");
        assertTrue(propertyListeners != null && propertyListeners.length == 2);
        assertTrue(changeListener1.findMe(propertyListeners) == 1);
        assertTrue(changeListener2.findMe(propertyListeners) == 0);
        assertTrue(changeListener3.findMe(propertyListeners) == 0);
        assertTrue(changeListener4.findMe(propertyListeners) == 1);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners("null");
        assertTrue(propertyListeners != null && propertyListeners.length == 0);
    }

    /*
     * Class under test for boolean hasListeners(String)
     */
    public void testHasListenersString() {
        ConcretePropertyChangeListener changeListener1 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener2 = new ConcretePropertyChangeListener();
        propertyChangeSupport.addPropertyChangeListener("first", changeListener1);
        assertTrue(propertyChangeSupport.hasListeners("first"));
        assertFalse(propertyChangeSupport.hasListeners("second"));
        assertFalse(propertyChangeSupport.hasListeners("third"));
        assertFalse(propertyChangeSupport.hasListeners("forth"));
        propertyChangeSupport.addPropertyChangeListener("second", changeListener2);
        assertTrue(propertyChangeSupport.hasListeners("first"));
        assertTrue(propertyChangeSupport.hasListeners("second"));
        assertFalse(propertyChangeSupport.hasListeners("third"));
        assertFalse(propertyChangeSupport.hasListeners("forth"));
        propertyChangeSupport.addPropertyChangeListener("third", changeListener1);
        assertTrue(propertyChangeSupport.hasListeners("first"));
        assertTrue(propertyChangeSupport.hasListeners("second"));
        assertTrue(propertyChangeSupport.hasListeners("third"));
        assertFalse(propertyChangeSupport.hasListeners("forth"));
        propertyChangeSupport.addPropertyChangeListener(changeListener2);
        assertTrue(propertyChangeSupport.hasListeners("first"));
        assertTrue(propertyChangeSupport.hasListeners("second"));
        assertTrue(propertyChangeSupport.hasListeners("third"));
        assertTrue(propertyChangeSupport.hasListeners("forth"));
        propertyChangeSupport.removePropertyChangeListener("first", changeListener1);
        assertTrue(propertyChangeSupport.hasListeners("first"));
        assertTrue(propertyChangeSupport.hasListeners("second"));
        assertTrue(propertyChangeSupport.hasListeners("third"));
        assertTrue(propertyChangeSupport.hasListeners("forth"));
        propertyChangeSupport.removePropertyChangeListener("first", changeListener2);
        assertTrue(propertyChangeSupport.hasListeners("first"));
        assertTrue(propertyChangeSupport.hasListeners("second"));
        assertTrue(propertyChangeSupport.hasListeners("third"));
        assertTrue(propertyChangeSupport.hasListeners("forth"));
    }

    /*
     * Class under test for void removePropertyChangeListener(PropertyChangeListener)
     */
    public void testRemovePropertyChangeListenerPropertyChangeListener() {
        ConcretePropertyChangeListener changeListener1 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener2 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener3 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener4 = new ConcretePropertyChangeListener();
        PropertyChangeListener[] propertyListeners = null;
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 0);
        propertyChangeSupport.addPropertyChangeListener(changeListener1);
        propertyChangeSupport.addPropertyChangeListener(changeListener2);
        propertyChangeSupport.addPropertyChangeListener(changeListener3);
        propertyChangeSupport.addPropertyChangeListener(changeListener1);
        propertyChangeSupport.addPropertyChangeListener("first", changeListener2);
        propertyChangeSupport.addPropertyChangeListener(changeListener4);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 6);
        propertyChangeSupport.removePropertyChangeListener(changeListener3);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 5);
        assertTrue(changeListener1.findMe(propertyListeners) == 2);
        assertTrue(changeListener3.findMe(propertyListeners) == 0);
        propertyChangeSupport.removePropertyChangeListener(changeListener1);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 4);
        assertTrue(changeListener1.findMe(propertyListeners) == 1);
        propertyChangeSupport.removePropertyChangeListener("first", changeListener1);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 4);
        assertTrue(changeListener1.findMe(propertyListeners) == 1);
        propertyChangeSupport.removePropertyChangeListener(changeListener2);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 3);
        assertTrue(changeListener2.findMe(propertyListeners) == 0);
        propertyChangeSupport.removePropertyChangeListener(changeListener2);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 3);
        assertTrue(changeListener2.findMe(propertyListeners) == 0);
    }

    /*
     * Class under test for void addPropertyChangeListener(PropertyChangeListener)
     */
    public void testAddPropertyChangeListenerPropertyChangeListener() {
        ConcretePropertyChangeListener changeListener1 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener2 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener3 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener4 = new ConcretePropertyChangeListener();
        PropertyChangeListener[] propertyListeners = null;
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 0);
        propertyChangeSupport.addPropertyChangeListener(changeListener1);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 1);
        assertTrue(changeListener1.findMe(propertyListeners) == 1);
        propertyChangeSupport.addPropertyChangeListener(changeListener2);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 2);
        assertTrue(changeListener1.findMe(propertyListeners) == 1);
        assertTrue(changeListener2.findMe(propertyListeners) == 1);
        propertyChangeSupport.addPropertyChangeListener(changeListener3);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 3);
        assertTrue(changeListener1.findMe(propertyListeners) == 1);
        assertTrue(changeListener2.findMe(propertyListeners) == 1);
        assertTrue(changeListener3.findMe(propertyListeners) == 1);
        propertyChangeSupport.addPropertyChangeListener(changeListener1);
        propertyChangeSupport.addPropertyChangeListener(changeListener4);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 5);
        assertTrue(changeListener1.findMe(propertyListeners) == 2);
        assertTrue(changeListener2.findMe(propertyListeners) == 1);
        assertTrue(changeListener3.findMe(propertyListeners) == 1);
        assertTrue(changeListener4.findMe(propertyListeners) == 1);
    }

    /*
     * Class under test for PropertyChangeListener[] getPropertyChangeListeners()
     */
    public void testGetPropertyChangeListeners() {
        ConcretePropertyChangeListener changeListener1 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener2 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener3 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener4 = new ConcretePropertyChangeListener();
        PropertyChangeListener[] propertyListeners = null;
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 0);
        propertyChangeSupport.addPropertyChangeListener("first", changeListener1);
        propertyChangeSupport.addPropertyChangeListener("second", changeListener1);
        propertyChangeSupport.addPropertyChangeListener(changeListener2);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 3);
        assertTrue(changeListener1.findMe(propertyListeners) == 0);
        assertTrue(changeListener2.findMe(propertyListeners) == 1);
        assertTrue(changeListener3.findMe(propertyListeners) == 0);
        assertTrue(changeListener4.findMe(propertyListeners) == 0);
        propertyChangeSupport.addPropertyChangeListener(changeListener3);
        propertyChangeSupport.addPropertyChangeListener("second", changeListener4);
        propertyListeners = propertyChangeSupport.getPropertyChangeListeners();
        assertTrue(propertyListeners != null && propertyListeners.length == 5);
        assertTrue(changeListener1.findMe(propertyListeners) == 0);
        assertTrue(changeListener2.findMe(propertyListeners) == 1);
        assertTrue(changeListener3.findMe(propertyListeners) == 1);
        assertTrue(changeListener4.findMe(propertyListeners) == 0);
    }

    /*
     * Class under test for void firePropertyChange(PropertyChangeEvent)
     */
    public void testFirePropertyChangePropertyChangeEvent() {
        ConcretePropertyChangeListener changeListener1 = new ConcretePropertyChangeListener();
        ConcretePropertyChangeListener changeListener2 = new ConcretePropertyChangeListener();
        propertyChangeSupport.addPropertyChangeListener("first", changeListener1);
        propertyChangeSupport.addPropertyChangeListener("second", changeListener2);
        String oldValue = "old";
        String newValue = "new";
        propertyChangeSupport.firePropertyChange(new PropertyChangeEvent(panel, "first",
                oldValue, newValue));
        changeListener1.checkLastPropertyFired(panel, "first", oldValue, newValue);
        assertFalse(changeListener2.isChanged());
        changeListener1.reset();
        changeListener2.reset();
        propertyChangeSupport.firePropertyChange(new PropertyChangeEvent(panel, "second",
                oldValue, newValue));
        assertFalse(changeListener1.isChanged());
        changeListener2.checkLastPropertyFired(panel, "second", oldValue, newValue);
        changeListener1.reset();
        changeListener2.reset();
        propertyChangeSupport.addPropertyChangeListener("first", changeListener2);
        propertyChangeSupport.firePropertyChange(new PropertyChangeEvent(panel, "first",
                oldValue, newValue));
        changeListener1.checkLastPropertyFired(panel, "first", oldValue, newValue);
        changeListener2.checkLastPropertyFired(panel, "first", oldValue, newValue);
        changeListener1.reset();
        changeListener2.reset();
        propertyChangeSupport.removePropertyChangeListener("first", changeListener2);
        propertyChangeSupport.addPropertyChangeListener(changeListener2);
        propertyChangeSupport.firePropertyChange(new PropertyChangeEvent(panel, "first",
                oldValue, newValue));
        changeListener1.checkLastPropertyFired(panel, "first", oldValue, newValue);
        changeListener2.checkLastPropertyFired(panel, "first", oldValue, newValue);
        changeListener1.reset();
        changeListener2.reset();
        propertyChangeSupport.firePropertyChange(new PropertyChangeEvent(panel, "second",
                oldValue, newValue));
        assertFalse(changeListener1.isChanged());
        changeListener2.checkLastPropertyFired(panel, "second", oldValue, newValue);
    }

    public void testReadWriteObject() throws IOException, ClassNotFoundException {
        String name1 = "name1";
        String name2 = "name2";
        PropertyChangeListener changeListener1 = new SerializableListener(name1);
        PropertyChangeListener changeListener2 = new SerializableListener(name2);
        propertyChangeSupport.addPropertyChangeListener(name1, changeListener1);
        propertyChangeSupport.addPropertyChangeListener(name2, changeListener2);
        PropertyChangeSupport resurrectedList = (PropertyChangeSupport) serializeObject(propertyChangeSupport);
        assertEquals(1, resurrectedList.getPropertyChangeListeners(name1).length);
        assertEquals(name1, ((SerializableListener) resurrectedList
                .getPropertyChangeListeners(name1)[0]).name);
        assertEquals(1, resurrectedList.getPropertyChangeListeners(name2).length);
        assertEquals(name2, ((SerializableListener) resurrectedList
                .getPropertyChangeListeners(name2)[0]).name);
    }
}
