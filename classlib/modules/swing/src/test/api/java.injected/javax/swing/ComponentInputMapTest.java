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
 * Created on 01.10.2004

 */
package javax.swing;

import java.io.IOException;

public class ComponentInputMapTest extends InputMapTest {
    protected JComponent component = null;

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(ComponentInputMapTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        component = new JPanel();
        map = new ComponentInputMap(component);
        parent = new ComponentInputMap(component);
    }

    @Override
    public void testSetGetParent() {
        parent = new ComponentInputMap(component);
        map.setParent(parent);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                map.setParent(new InputMap());
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                map.setParent(new ComponentInputMap(new JPanel()));
            }
        });
        map.setParent(null);
        assertNull(map.getParent());
    }

    public void testComponentInputMap() {
        boolean thrown = false;
        try {
            map = new ComponentInputMap(component);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertFalse(thrown);
        component = null;
        try {
            map = new ComponentInputMap(component);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    public void testGetComponent() {
        assertTrue(((ComponentInputMap) map).getComponent() == component);
    }

    @Override
    public void testWriteObject() throws IOException {
        //super.testWriteObject();
    }

    @Override
    public void testReadObject() {
        /*
         KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_1, 0);
         KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_2, 0);
         Object object1 = "object1";
         Object object2 = "object2";
         ComponentInputMap parent = new ComponentInputMap(component);
         map.setParent(parent);
         map.put(keyStroke1, object1);
         map.put(keyStroke2, object2);
         try {
         FileOutputStream fo = new FileOutputStream("tmp");
         ObjectOutputStream so = new ObjectOutputStream(fo);
         so.writeObject(map);
         so.flush();
         } catch (Exception e) {
         assertFalse(true);
         }
         try {
         FileInputStream fi = new FileInputStream("tmp");
         ObjectInputStream si = new ObjectInputStream(fi);
         ComponentInputMap ressurectedMap = (ComponentInputMap)si.readObject();
         assertTrue(ressurectedMap.getParent() != null);
         assertTrue(ressurectedMap.getComponent() != null);
         assertTrue(ressurectedMap.get(keyStroke1).equals(object1));
         assertTrue(ressurectedMap.get(keyStroke2).equals(object2));
         } catch (Exception e) {
         System.out.println(e);
         assertFalse(true);
         }
         */
    }
}
