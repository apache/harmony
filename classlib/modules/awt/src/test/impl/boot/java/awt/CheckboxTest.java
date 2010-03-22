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
 * @author Dmitry A. Durnev
 */
package java.awt;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import junit.framework.TestCase;

/**
 * CheckboxTest
 */
@SuppressWarnings("serial")
public class CheckboxTest extends TestCase {

    class TestCheckbox extends Checkbox {
       
        public TestCheckbox() throws HeadlessException {
            super();
        }

        public TestCheckbox(String arg0) throws HeadlessException {
            super(arg0);
        }

        public TestCheckbox(String arg0, boolean arg1) throws HeadlessException {
            super(arg0, arg1);
        }

        public TestCheckbox(String arg0, boolean arg1, CheckboxGroup arg2)
                throws HeadlessException {
            super(arg0, arg1, arg2);
        }

        public TestCheckbox(String arg0, CheckboxGroup arg1, boolean arg2)
                throws HeadlessException {
            super(arg0, arg1, arg2);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                }
            });
        }
    }

    private TestCheckbox checkbox;
    private boolean eventProcessed;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();        
        checkbox = new TestCheckbox("Checkbox");
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCheckbox() {
        checkbox = new TestCheckbox();

        assertEquals("", checkbox.getLabel());
        assertFalse(checkbox.getState());
        assertNull(checkbox.getCheckboxGroup());
    }

    public void testCheckboxStringBoolean() {
        String text = "checkbox";
        boolean checked = true;
        checkbox = new TestCheckbox(text, checked);
        checkCheckbox(text, checked, null);
    }

    public void testCheckboxString() {
        checkCheckbox("Checkbox", false, null);
    }

    public void testCheckboxStringBooleanCheckboxGroup() {
        String text = "checkbox";
        boolean checked = false;
        CheckboxGroup group = new CheckboxGroup();
        checkbox = new TestCheckbox(text, checked, group);
        checkCheckbox(text, checked, group);
    }

    public void testCheckboxStringCheckboxGroupBoolean() {
        String text = null;
        boolean checked = true;
        CheckboxGroup group = new CheckboxGroup();
        checkbox = new TestCheckbox(text, group, checked);
        checkCheckbox(text, checked, group);
    }

    private void checkCheckbox(String text, boolean state, CheckboxGroup group) {
        assertEquals(text, checkbox.getLabel());
        assertEquals(state, checkbox.getState());
        assertSame(group, checkbox.getCheckboxGroup());
    }

    public void testGetSetLabel() {
        checkbox.setLabel(null);
        assertNull(checkbox.getLabel());

        String text = "Checkbox";
        checkbox.setLabel(text);
        assertEquals(text, checkbox.getLabel());
    }

    public void testGetSetState() {
        assertFalse(checkbox.getState());
        checkbox.setState(true);
        assertTrue(checkbox.getState());
        checkbox.setState(false);
        assertFalse(checkbox.getState());
    }

    public void testGetSelectedObjects() {
        assertNull(checkbox.getSelectedObjects());
        checkbox.setState(true);
        Object[] selected = checkbox.getSelectedObjects();
        assertNotNull(selected);
        assertEquals(1, selected.length);
        assertTrue(selected[0] instanceof String);
        String strSelected = (String) selected[0];
        assertSame(checkbox.getLabel(), strSelected);
    }

    public void testGetSetCheckboxGroup() {
        assertNull(checkbox.getCheckboxGroup());
        CheckboxGroup group = new CheckboxGroup();

        checkbox.setCheckboxGroup(group);
        assertSame(group, checkbox.getCheckboxGroup());
        checkbox.setCheckboxGroup(null);
        assertNull(checkbox.getCheckboxGroup());

    }

    public void testAddGetRemoveItemListener() {
        assertEquals(0, checkbox.getItemListeners().length);

        ItemListener listener = new ItemListener() {
            public void itemStateChanged(ItemEvent ie) {
            }
        };
        checkbox.addItemListener(listener);
        assertEquals(1, checkbox.getItemListeners().length);
        assertSame(listener, checkbox.getItemListeners()[0]);

        checkbox.removeItemListener(listener);
        assertEquals(0, checkbox.getItemListeners().length);
    }

    public void testProcessItemEvent() {
        eventProcessed = false;
        checkbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ie) {
                eventProcessed = true;
            }
        });
        checkbox.processEvent(new ItemEvent(checkbox,
                                              ItemEvent.ITEM_STATE_CHANGED,
                                              checkbox, ItemEvent.SELECTED));
        assertTrue(eventProcessed);
    }

    public void testGetListenersClass() {
        Class<ItemListener> cls = ItemListener.class;
        assertEquals(0, checkbox.getListeners(cls).length);

        ItemListener listener = new ItemListener() {

            public void itemStateChanged(ItemEvent ie) {
            }};
        checkbox.addItemListener(listener);
        assertEquals(1, checkbox.getListeners(cls).length);
        assertSame(listener, checkbox.getListeners(cls)[0]);

        checkbox.removeItemListener(listener);
        assertEquals(0, checkbox.getListeners(cls).length);
    }
    
    public void testDeadLoop4887() {
        final int count[] = new int[1];
        Component c = new Checkbox() {
            public void paint(Graphics g) {
                count[0]++;
                setState(true);
                setEnabled(true);
            }
        };
        
        Tools.checkDeadLoop(c, count);
    }

}
