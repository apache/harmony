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
 * Created on 02.03.2005

 */
package javax.swing.text;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingTestCase;

public class TextActionTest extends SwingTestCase {
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(TextActionTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testAugmentList() {
        String name1 = "name1";
        String name2 = "name2";
        String name3 = "name3";
        String name5 = "name5";
        Action action1 = new TextAction(name1) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
            }
        };
        Action action2 = new AbstractAction(name2) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
            }
        };
        Action action3 = new AbstractAction(name3) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
            }
        };
        Action action4 = new AbstractAction(name1) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
            }
        };
        Action action5 = new AbstractAction(name5) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
            }
        };
        Action[] list1 = new Action[] { action1, action2 };
        Action[] list2 = new Action[] { action3, action4 };
        Action[] list3 = new Action[] { action4, action3 };
        Action[] list4 = new Action[] { action2 };
        Action[] list5 = new Action[] { action3, action4, action5 };
        Action[] res = null;
        res = TextAction.augmentList(list1, list2);
        assertTrue("result's not null", res != null);
        assertEquals("result's length ", 3, res.length);
        assertEquals("resulted list item", action3, res[0]);
        assertEquals("resulted list item", action2, res[1]);
        assertEquals("resulted list item", action4, res[2]);
        res = TextAction.augmentList(list2, list1);
        assertTrue("result's not null", res != null);
        assertEquals("result's length ", 3, res.length);
        assertEquals("resulted list item", action3, res[0]);
        assertEquals("resulted list item", action2, res[1]);
        assertEquals("resulted list item", action1, res[2]);
        res = TextAction.augmentList(list1, list3);
        assertTrue("result's not null", res != null);
        assertEquals("result's length ", 3, res.length);
        assertEquals("resulted list item", action3, res[0]);
        assertEquals("resulted list item", action2, res[1]);
        assertEquals("resulted list item", action4, res[2]);
        res = TextAction.augmentList(list3, list1);
        assertTrue("result's not null", res != null);
        assertEquals("result's length ", 3, res.length);
        assertEquals("resulted list item", action3, res[0]);
        assertEquals("resulted list item", action2, res[1]);
        assertEquals("resulted list item", action1, res[2]);
        res = TextAction.augmentList(list4, list5);
        assertTrue("result's not null", res != null);
        assertEquals("result's length ", 4, res.length);
        assertEquals("resulted list item", action5, res[0]);
        assertEquals("resulted list item", action3, res[1]);
        assertEquals("resulted list item", action2, res[2]);
        assertEquals("resulted list item", action4, res[3]);
    }

    /*
     * Class under test for void TextAction(String)
     */
    public void testTextActionString() {
        String str1 = "string1";
        String str2 = "string2";
        TextAction action1 = new TextAction(str1) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
            }
        };
        TextAction action2 = new TextAction(str2) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
            }
        };
        TextAction action3 = new TextAction(null) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
            }
        };
        assertEquals("Number of keys", 1, action1.getKeys().length);
        assertEquals("name ", str1, action1.getValue(Action.NAME));
        assertTrue("action is enabled initially", action1.isEnabled());
        assertEquals("Number of keys", 1, action2.getKeys().length);
        assertEquals("name ", str2, action2.getValue(Action.NAME));
        assertTrue("action is enabled initially", action2.isEnabled());
        assertEquals("Number of keys", 0, action3.getKeys().length);
        assertNull("name ", action3.getValue(Action.NAME));
        assertTrue("action is enabled initially", action3.isEnabled());
    }

    public void testTextActionNullEvent() {
        Action[] actions = new DefaultEditorKit().getActions();
        for (int i = 0; i < actions.length; i++) {
            try {
                actions[i].actionPerformed(null);
            } catch (NullPointerException e) {
                fail("Null event is not supported in: " + actions[i].getValue(Action.NAME));
            }
        }
    }
}