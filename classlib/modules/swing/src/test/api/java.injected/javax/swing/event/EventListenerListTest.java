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
 * Created on 21.09.2004

 */
package javax.swing.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.EventListener;
import javax.swing.SwingTestCase;

public class EventListenerListTest extends SwingTestCase {
    static class ConcreteListener implements EventListener {
        protected String name = null;

        public ConcreteListener() {
        }

        public ConcreteListener(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public int findMe(final Object[] listenersArray, final Class<?> myClass) {
            int found = 0;
            for (int i = 0; i < listenersArray.length; i += 2) {
                if (listenersArray[i] == myClass && listenersArray[i + 1] == this) {
                    found++;
                }
            }
            return found;
        }

        public int findMe(final EventListener[] listenersArray) {
            int found = 0;
            for (int i = 0; i < listenersArray.length; i++) {
                if (listenersArray[i] == this) {
                    found++;
                }
            }
            return found;
        }
    }

    static class ConcreteSerializableListener extends ConcreteListener implements Serializable {
        private static final long serialVersionUID = 1L;

        public ConcreteSerializableListener() {
        }

        public ConcreteSerializableListener(final String name) {
            super(name);
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
    }

    protected EventListenerList list = null;

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(EventListenerListTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        list = new EventListenerList();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testRemove() {
        ConcreteListener listener1 = new ConcreteListener("1");
        ConcreteListener listener2 = new ConcreteListener("2");
        ConcreteListener listener3 = new ConcreteListener("3");
        list.add(ConcreteListener.class, listener1);
        list.add(ConcreteListener.class, listener2);
        list.add(ConcreteListener.class, listener3);
        assertTrue(list.getListenerCount() == 3);
        list.remove(ConcreteListener.class, listener2);
        assertTrue(list.getListenerCount() == 2);
        list.remove(ConcreteListener.class, listener1);
        assertTrue(list.getListenerCount() == 1);
        list.add(ConcreteListener.class, listener2);
        list.add(EventListener.class, listener1);
        list.add(EventListener.class, listener3);
        assertTrue(list.getListenerCount() == 4);
        list.remove(ConcreteListener.class, listener1);
        assertTrue(list.getListenerCount() == 4);
        list.remove(ConcreteListener.class, listener2);
        assertTrue(list.getListenerCount() == 3);
        list.remove(ConcreteListener.class, listener3);
        assertTrue(list.getListenerCount() == 2);
        list.remove(EventListener.class, listener3);
        assertTrue(list.getListenerCount() == 1);
        list.remove(EventListener.class, null);
        list.remove(EventListener.class, listener1);
        assertTrue(list.getListenerCount() == 0);
    }

    public void testAdd() {
        ConcreteListener listener1 = new ConcreteListener("1");
        ConcreteListener listener2 = new ConcreteListener("2");
        ConcreteListener listener3 = new ConcreteListener("3");
        Object[] listenersList = null;
        list.add(ConcreteListener.class, listener1);
        assertTrue(list.getListenerCount() == 1);
        listenersList = list.getListenerList();
        assertTrue(listener1.findMe(listenersList, ConcreteListener.class) == 1);
        list.add(ConcreteListener.class, listener2);
        assertTrue(list.getListenerCount() == 2);
        listenersList = list.getListenerList();
        assertTrue(listener1.findMe(listenersList, ConcreteListener.class) == 1);
        assertTrue(listener2.findMe(listenersList, ConcreteListener.class) == 1);
        list.add(ConcreteListener.class, listener3);
        assertTrue(list.getListenerCount() == 3);
        listenersList = list.getListenerList();
        assertTrue(listener1.findMe(listenersList, ConcreteListener.class) == 1);
        assertTrue(listener2.findMe(listenersList, ConcreteListener.class) == 1);
        assertTrue(listener3.findMe(listenersList, ConcreteListener.class) == 1);
        list.add(ConcreteListener.class, listener1);
        assertTrue(list.getListenerCount() == 4);
        listenersList = list.getListenerList();
        assertTrue(listener1.findMe(listenersList, ConcreteListener.class) == 2);
        assertTrue(listener2.findMe(listenersList, ConcreteListener.class) == 1);
        assertTrue(listener3.findMe(listenersList, ConcreteListener.class) == 1);
        list.add(EventListener.class, listener1);
        list.add(EventListener.class, listener2);
        assertTrue(list.getListenerCount() == 6);
        listenersList = list.getListenerList();
        assertTrue(listener1.findMe(listenersList, ConcreteListener.class) == 2);
        assertTrue(listener1.findMe(listenersList, EventListener.class) == 1);
        assertTrue(listener2.findMe(listenersList, ConcreteListener.class) == 1);
        assertTrue(listener2.findMe(listenersList, EventListener.class) == 1);
        assertTrue(listener3.findMe(listenersList, ConcreteListener.class) == 1);
    }

    public void testGetListeners() {
        ConcreteListener listener1 = new ConcreteListener("1");
        ConcreteListener listener2 = new ConcreteListener("2");
        ConcreteListener listener3 = new ConcreteListener("3");
        EventListener[] listenersList = null;
        listenersList = list.getListeners(ConcreteListener.class);
        assertTrue(listener1.findMe(listenersList) == 0);
        list.add(ConcreteListener.class, listener1);
        listenersList = list.getListeners(ConcreteListener.class);
        assertTrue(listener1.findMe(listenersList) == 1);
        list.add(ConcreteListener.class, listener2);
        listenersList = list.getListeners(ConcreteListener.class);
        assertTrue(listener1.findMe(listenersList) == 1);
        assertTrue(listener2.findMe(listenersList) == 1);
        list.add(ConcreteListener.class, listener3);
        listenersList = list.getListeners(ConcreteListener.class);
        assertTrue(listener1.findMe(listenersList) == 1);
        assertTrue(listener2.findMe(listenersList) == 1);
        assertTrue(listener3.findMe(listenersList) == 1);
        list.add(ConcreteListener.class, listener3);
        list.add(EventListener.class, listener1);
        list.add(EventListener.class, listener2);
        assertTrue(list.getListenerCount() == 6);
        listenersList = list.getListeners(EventListener.class);
        assertTrue(listener1.findMe(listenersList) == 1);
        assertTrue(listener2.findMe(listenersList) == 1);
        assertTrue(listener3.findMe(listenersList) == 0);
        listenersList = list.getListeners(ConcreteListener.class);
        assertTrue(listener1.findMe(listenersList) == 1);
        assertTrue(listener2.findMe(listenersList) == 1);
        assertTrue(listener3.findMe(listenersList) == 2);
        assertEquals(listenersList[3], listener1);
        assertEquals(listenersList[1], listener3);
    }

    /*
     * Class under test for String toString()
     */
    public void testToString() {
        ConcreteListener listener1 = new ConcreteListener("1");
        ConcreteListener listener2 = new ConcreteListener("2");
        ConcreteListener listener3 = new ConcreteListener("3");
        list.add(ConcreteListener.class, listener1);
        list.add(ConcreteListener.class, listener2);
        list.add(ConcreteListener.class, listener3);
        assertFalse(list.toString() == null && list.toString().equals(""));
    }

    /*
     * this function is being tested by testAdd()
     */
    public void testGetListenerList() {
    }

    /*
     * Class under test for int getListenerCount(Class)
     */
    public void testGetListenerCountClass() {
        ConcreteListener listener1 = new ConcreteListener("1");
        ConcreteListener listener2 = new ConcreteListener("2");
        ConcreteListener listener3 = new ConcreteListener("3");
        assertTrue(list.getListenerCount(ConcreteListener.class) == 0);
        list.add(ConcreteListener.class, listener1);
        list.add(ConcreteListener.class, listener2);
        list.add(ConcreteListener.class, listener3);
        assertTrue(list.getListenerCount(ConcreteListener.class) == 3);
        list.add(EventListener.class, listener1);
        list.add(EventListener.class, listener2);
        list.add(ConcreteListener.class, listener1);
        assertTrue(list.getListenerCount(ConcreteListener.class) == 4);
        assertTrue(list.getListenerCount(EventListener.class) == 2);
    }

    /*
     * Class under test for int getListenerCount()
     */
    public void testGetListenerCount() {
        ConcreteListener listener1 = new ConcreteListener("1");
        ConcreteListener listener2 = new ConcreteListener("2");
        ConcreteListener listener3 = new ConcreteListener("3");
        assertTrue(list.getListenerCount() == 0);
        list.add(ConcreteListener.class, listener1);
        list.add(ConcreteListener.class, listener2);
        list.add(ConcreteListener.class, listener3);
        assertTrue(list.getListenerCount() == 3);
        list.add(ConcreteListener.class, listener1);
        assertTrue(list.getListenerCount() == 4);
        list.add(EventListener.class, listener1);
        list.add(EventListener.class, listener2);
        assertTrue(list.getListenerCount() == 6);
    }

    public void testWriteObject() throws IOException {
        ConcreteListener listener1 = new ConcreteSerializableListener("1");
        ConcreteSerializableListener listener2 = new ConcreteSerializableListener("2");
        ConcreteListener listener3 = new ConcreteListener("3");
        list.add(ConcreteListener.class, listener1);
        list.add(ConcreteSerializableListener.class, listener2);
        list.add(EventListener.class, listener3);
        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(fo);
        so.writeObject(list);
        so.flush();
    }

    public void testReadObject() throws IOException, ClassNotFoundException {
        ConcreteListener listener1 = new ConcreteSerializableListener("1");
        ConcreteListener listener2 = new ConcreteSerializableListener("2");
        ConcreteListener listener3 = new ConcreteListener("3");
        list.add(ConcreteListener.class, listener1);
        list.add(ConcreteListener.class, listener3);
        list.add(EventListener.class, listener2);
        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(fo);
        so.writeObject(list);
        so.flush();
        InputStream fi = new ByteArrayInputStream(fo.toByteArray());
        ObjectInputStream si = new ObjectInputStream(fi);
        EventListenerList resurrectedList = (EventListenerList) si.readObject();
        assertTrue(resurrectedList.getListeners(ConcreteListener.class).length == 1);
        assertTrue((resurrectedList.getListeners(ConcreteListener.class)[0]).name.equals("1"));
        assertTrue(resurrectedList.getListeners(EventListener.class).length == 1);
        assertTrue(((ConcreteListener) resurrectedList.getListeners(EventListener.class)[0]).name
                .equals("2"));
    }
}
