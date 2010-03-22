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

import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class InputMapTest extends SwingTestCase {
    protected InputMap map;

    protected InputMap parent;

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
        map = new InputMap();
        parent = new InputMap();
    }

    public void testPut() {
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
        map.put(keyStroke1, "1");
        map.put(keyStroke2, "2");
        assertTrue(map.get(keyStroke1).equals("1"));
        assertTrue(map.get(keyStroke2).equals("2"));
        map.put(keyStroke2, "1");
        map.put(keyStroke1, "2");
        assertTrue(map.get(keyStroke1).equals("2"));
        assertTrue(map.get(keyStroke2).equals("1"));
        map.put(keyStroke1, null);
        map.put(keyStroke2, null);
        assertTrue(map.size() == 0);
    }

    public void testGet() {
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
        assertNull(map.get(keyStroke1));
        assertNull(map.get(keyStroke2));
        map.put(keyStroke1, "1");
        map.put(keyStroke2, "1");
        assertTrue(map.get(keyStroke1).equals("1"));
        assertTrue(map.get(keyStroke2).equals("1"));
        map.put(keyStroke2, "2");
        assertTrue(map.get(keyStroke2).equals("2"));
        map.put(keyStroke2, null);
        assertNull(map.get(keyStroke2));
        InputMap childMap = new InputMap();
        childMap.setParent(map);
        map.put(keyStroke1, "1");
        KeyStroke keyStroke11 = KeyStroke.getKeyStroke(KeyEvent.VK_1, 0);
        childMap.put(keyStroke11, "1");
        assertTrue(childMap.get(keyStroke11).equals("1"));
        assertTrue(childMap.get(keyStroke1).equals("1"));
        map.put(keyStroke2, "2");
        assertTrue(childMap.get(keyStroke2).equals("2"));
        childMap.put(keyStroke2, "1");
        assertTrue(childMap.get(keyStroke2).equals("1"));
    }

    public void testRemove() {
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
        map.put(keyStroke1, "1");
        map.put(keyStroke2, "2");
        assertTrue(map.get(keyStroke1).equals("1"));
        assertTrue(map.get(keyStroke2).equals("2"));
        map.remove(keyStroke1);
        assertNull(map.get(keyStroke1));
        assertTrue(map.get(keyStroke2).equals("2"));
        map.remove(keyStroke2);
        assertNull(map.get(keyStroke2));
        assertTrue(map.size() == 0);
    }

    public void testKeys() {
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
        KeyStroke keyStroke3 = KeyStroke.getKeyStroke(KeyEvent.VK_1, 0);
        KeyStroke keyStroke4 = KeyStroke.getKeyStroke(KeyEvent.VK_0, 0);
        Object[] keys = map.keys();
        assertEquals(0, map.size());
        if (isHarmony()) {
            assertNotNull(keys);
            assertEquals(0, keys.length);
        } else {
            assertNull(keys);
        }
        map.put(keyStroke1, "1");
        map.put(keyStroke2, "2");
        map.put(keyStroke3, "1");
        map.put(keyStroke4, "2");
        keys = map.keys();
        assertTrue("array size's correct ", keys != null && keys.length == 4);
        assertTrue(find(keys, keyStroke1));
        assertTrue(find(keys, keyStroke2));
        assertTrue(find(keys, keyStroke3));
        assertTrue(find(keys, keyStroke4));
        map.put(keyStroke1, null);
        map.put(keyStroke2, null);
        map.put(keyStroke3, null);
        map.put(keyStroke4, null);
        assertEquals(0, map.size());
        assertNotNull(keys);
    }

    public void testAllKeys() {
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_1, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_2, 0);
        KeyStroke keyStroke3 = KeyStroke.getKeyStroke(KeyEvent.VK_3, 0);
        KeyStroke keyStroke4 = KeyStroke.getKeyStroke(KeyEvent.VK_4, 0);
        KeyStroke keyStroke5 = KeyStroke.getKeyStroke(KeyEvent.VK_5, 0);
        KeyStroke keyStroke6 = KeyStroke.getKeyStroke(KeyEvent.VK_6, 0);
        Object[] keys = map.allKeys();
        map.setParent(parent);
        assertEquals(0, map.size());
        if (isHarmony()) {
            assertNotNull(keys);
            assertEquals(0, keys.length);
        } else {
            assertNull(keys);
        }
        parent.put(keyStroke1, "1");
        parent.put(keyStroke2, "2");
        parent.put(keyStroke3, "1");
        parent.put(keyStroke4, "2");
        map.put(keyStroke3, "1");
        map.put(keyStroke4, "2");
        map.put(keyStroke5, "1");
        map.put(keyStroke6, "2");
        keys = map.allKeys();
        assertTrue(find(keys, keyStroke1));
        assertTrue(find(keys, keyStroke2));
        assertTrue(find(keys, keyStroke3));
        assertTrue(find(keys, keyStroke4));
        assertTrue(find(keys, keyStroke5));
        assertTrue(find(keys, keyStroke6));
    }

    public void testSetGetParent() {
        InputMap parent1 = new InputMap();
        InputMap parent2 = new InputMap();
        InputMap parent3 = null;
        assertNull(map.getParent());
        map.setParent(parent1);
        assertTrue(map.getParent() == parent1);
        map.setParent(parent3);
        assertTrue(map.getParent() == parent3);
        map.setParent(parent2);
        assertTrue(map.getParent() == parent2);
    }

    public void testClear() {
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
        assertTrue(map.size() == 0);
        map.put(keyStroke1, "1");
        map.put(keyStroke2, "2");
        assertTrue(map.size() == 2);
        map.clear();
        assertTrue(map.size() == 0);
        assertTrue("keys", map.keys() != null);
        assertTrue("keys", map.keys().length == 0);
        map.put(keyStroke1, "1");
        assertTrue(map.size() == 1);
    }

    public void testSize() {
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_1, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_2, 0);
        KeyStroke keyStroke3 = KeyStroke.getKeyStroke(KeyEvent.VK_3, 0);
        KeyStroke keyStroke4 = KeyStroke.getKeyStroke(KeyEvent.VK_4, 0);
        assertTrue(map.size() == 0);
        map.put(keyStroke1, "1");
        map.put(keyStroke2, "2");
        assertTrue(map.size() == 2);
        map.put(keyStroke3, "1");
        map.put(keyStroke4, "2");
        assertTrue(map.size() == 4);
        map.put(keyStroke1, null);
        map.put(keyStroke2, null);
        assertTrue(map.size() == 2);
    }

    public void testWriteObject() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(output);
        so.writeObject(map);
        so.flush();
        assertTrue(output.size() > 0);
    }

    public void testReadObject() throws Exception {
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_1, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_2, 0);
        Object object1 = "object1";
        Object object2 = "object2";
        map.setParent(parent);
        map.put(keyStroke1, object1);
        map.put(keyStroke2, object2);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(output);
        so.writeObject(map);
        so.flush();
        ObjectInputStream si = new ObjectInputStream(new ByteArrayInputStream(output
                .toByteArray()));
        InputMap ressurectedMap = (InputMap) si.readObject();
        assertTrue(ressurectedMap.getParent() != null);
        assertTrue(ressurectedMap.get(keyStroke1).equals(object1));
        assertTrue(ressurectedMap.get(keyStroke2).equals(object2));
    }
}
