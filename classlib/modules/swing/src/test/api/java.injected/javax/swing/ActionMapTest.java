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
 * Created on 30.09.2004

 */
package javax.swing;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ActionMapTest extends SwingTestCase {
    private static class ActionProxy implements Action, Serializable {
        private static final long serialVersionUID = 1L;

        public String name = "";

        public ActionProxy() {
        }

        public ActionProxy(final String name) {
            this.name = name;
        }

        public boolean isEnabled() {
            return true;
        }

        public void setEnabled(final boolean enabled) {
        }

        public void addPropertyChangeListener(final PropertyChangeListener listener) {
        }

        public void removePropertyChangeListener(final PropertyChangeListener listener) {
        }

        public Object getValue(final String valueName) {
            return null;
        }

        public void putValue(final String valueName, final Object value) {
        }

        public void actionPerformed(final ActionEvent event) {
        }

        private void writeObject(final ObjectOutputStream outStream) throws IOException {
            outStream.defaultWriteObject();
        }

        private void readObject(final ObjectInputStream inStream) throws IOException,
                ClassNotFoundException {
            inStream.defaultReadObject();
        }
    }

    protected ActionMap map;

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

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        map = new ActionMap();
    }

    public void testPut() {
        Action action1 = new ActionProxy();
        Action action2 = new ActionProxy();
        map.put("1", action1);
        map.put("2", action2);
        assertTrue(map.get("1") == action1);
        assertTrue(map.get("2") == action2);
        map.put("2", action1);
        map.put("1", action2);
        assertTrue(map.get("1") == action2);
        assertTrue(map.get("2") == action1);
        map.put("1", null);
        map.put("2", null);
        assertTrue(map.size() == 0);
    }

    public void testGet() {
        Action action1 = new ActionProxy();
        Action action2 = new ActionProxy();
        Action action3 = new ActionProxy();
        assertNull(map.get("1"));
        assertNull(map.get("2"));
        map.put("1", action1);
        map.put("2", action1);
        assertTrue(map.get("1") == action1);
        assertTrue(map.get("2") == action1);
        map.put("1", action2);
        assertTrue(map.get("1") == action2);
        map.put("1", null);
        assertNull(map.get("1"));
        map.clear();
        ActionMap childMap = new ActionMap();
        childMap.setParent(map);
        map.put("1", action1);
        childMap.put("2", action2);
        assertTrue(childMap.get("1") == action1);
        assertTrue(childMap.get("2") == action2);
        map.put("2", action3);
        assertTrue(childMap.get("2") == action2);
        childMap.put("1", action3);
        assertTrue(childMap.get("1") == action3);
    }

    public void testSetGetParent() {
        ActionMap parent1 = new ActionMap();
        ActionMap parent2 = new ActionMap();
        ActionMap parent3 = null;
        assertNull(map.getParent());
        map.setParent(parent1);
        assertTrue(map.getParent() == parent1);
        map.setParent(parent3);
        assertTrue(map.getParent() == parent3);
        map.setParent(parent2);
        assertTrue(map.getParent() == parent2);
    }

    public void testRemove() {
        Action action1 = new ActionProxy();
        Action action2 = new ActionProxy();
        map.put("1", action1);
        map.put("2", action2);
        assertTrue(map.get("1") == action1);
        assertTrue(map.get("2") == action2);
        map.remove("2");
        assertNull(map.get("2"));
        assertTrue(map.get("1") == action1);
        map.remove("1");
        assertNull(map.get("1"));
        assertTrue(map.size() == 0);
    }

    public void testKeys() {
        Action action1 = new ActionProxy();
        Action action2 = new ActionProxy();
        Object[] keys = map.keys();
        assertTrue(map.size() == 0);
        if (isHarmony()) {
            assertTrue(keys != null);
            assertTrue(keys.length == 0);
        }
        map.put("1", action1);
        map.put("2", action2);
        map.put("3", action1);
        map.put("4", action2);
        keys = map.keys();
        assertTrue(keys != null && keys.length == 4);
        assertTrue(find(keys, "1"));
        assertTrue(find(keys, "2"));
        assertTrue(find(keys, "3"));
        assertTrue(find(keys, "4"));
    }

    public void testAllKeys() {
        ActionMap parent = new ActionMap();
        Action action1 = new ActionProxy();
        Action action2 = new ActionProxy();
        Object[] keys = map.allKeys();
        map.setParent(parent);
        assertTrue(map.size() == 0);
        if (isHarmony()) {
            assertTrue(keys != null);
            assertTrue(keys.length == 0);
        }
        parent.put("1", action1);
        parent.put("2", action2);
        parent.put("3", action1);
        parent.put("4", action2);
        map.put("3", action1);
        map.put("4", action2);
        map.put("5", action1);
        map.put("6", action2);
        keys = map.allKeys();
        assertTrue(keys != null && keys.length == 6);
        assertTrue(find(keys, "1"));
        assertTrue(find(keys, "2"));
        assertTrue(find(keys, "3"));
        assertTrue(find(keys, "4"));
        assertTrue(find(keys, "5"));
        assertTrue(find(keys, "6"));
    }

    public void testClear() {
        Action action1 = new ActionProxy();
        Action action2 = new ActionProxy();
        assertTrue(map.size() == 0);
        map.put("1", action1);
        map.put("2", action2);
        assertTrue(map.size() == 2);
        map.clear();
        assertTrue(map.size() == 0);
        if (isHarmony()) {
            assertTrue("keys", map.keys() != null);
            assertTrue("keys", map.keys().length == 0);
        }
        map.put("1", action1);
        assertTrue(map.size() == 1);
    }

    public void testSize() {
        Action action1 = new ActionProxy();
        Action action2 = new ActionProxy();
        assertTrue(map.size() == 0);
        map.put("1", action1);
        map.put("2", action2);
        assertTrue(map.size() == 2);
        map.put("3", action1);
        map.put("4", action2);
        assertTrue(map.size() == 4);
        map.put("3", null);
        map.put("4", null);
        assertTrue(map.size() == 2);
    }

    public void testWriteObject() throws Exception {
        String name1 = "action1";
        String name2 = "action2";
        Action action1 = new ActionProxy(name1);
        Action action2 = new ActionProxy(name2);
        Object object1 = "object1";
        Object object2 = "object2";
        ActionMap parent = new ActionMap();
        map.setParent(parent);
        map.put(object1, action1);
        map.put(object2, action2);
        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(fo);
        so.writeObject(map);
        so.flush();
        assertTrue(fo.size() > 0);
    }

    public void testReadObject() throws Exception {
        String name1 = "action1";
        String name2 = "action2";
        Action action1 = new ActionProxy(name1);
        Action action2 = new ActionProxy(name2);
        Object object1 = "object1";
        Object object2 = "object2";
        ActionMap parent = new ActionMap();
        map.setParent(parent);
        map.put(object1, action1);
        map.put(object2, action2);
        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(fo);
        so.writeObject(map);
        so.flush();
        InputStream fi = new ByteArrayInputStream(fo.toByteArray());
        ObjectInputStream si = new ObjectInputStream(fi);
        ActionMap ressurectedMap = (ActionMap) si.readObject();
        assertTrue(ressurectedMap.getParent() != null);
        assertTrue(ressurectedMap.get(object1) instanceof ActionProxy);
        assertTrue(((ActionProxy) ressurectedMap.get(object1)).name.equals(name1));
        assertTrue(ressurectedMap.get(object2) instanceof ActionProxy);
        assertTrue(((ActionProxy) ressurectedMap.get(object2)).name.equals(name2));
    }
}
