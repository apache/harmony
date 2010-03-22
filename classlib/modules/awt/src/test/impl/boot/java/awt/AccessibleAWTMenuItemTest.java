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

import java.awt.MenuItem.AccessibleAWTMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;

import junit.framework.TestCase;

/**
 * AccessibleAWTMenuItemTest
 */
public class AccessibleAWTMenuItemTest extends TestCase {
    MenuItem item;
    AccessibleContext ac;
    AccessibleValue value;
    AccessibleAction action;
    ActionEvent event;
    private Robot robot;
    private ActionListener actionListener;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        item = new MenuItem();
        ac = item.getAccessibleContext();
        action = ac.getAccessibleAction();
        value = ac.getAccessibleValue();
        robot = new Robot();
        event = null;
        actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                event = ae;
            }
        };
    }

    public final void testAccessibleAWTMenuItem() {
        assertTrue(ac instanceof AccessibleAWTMenuItem);
    }

    public final void testGetAccessibleName() {
        String name = "name";
        assertEquals(item.getLabel(), ac.getAccessibleName());
        item.setLabel(name);
        assertEquals(name, ac.getAccessibleName());
        String accessibleName = "accessible name";
        ac.setAccessibleName(accessibleName);
        assertEquals(accessibleName, ac.getAccessibleName());
        ac.setAccessibleName(null);
        assertEquals(name, ac.getAccessibleName());
    }

    public final void testGetAccessibleRole() {
        assertSame(AccessibleRole.MENU_ITEM, ac.getAccessibleRole());
    }

    public final void testGetAccessibleAction() {
        assertSame(ac, ac.getAccessibleAction());
    }

    public final void testGetAccessibleValue() {
        assertSame(ac, ac.getAccessibleValue());
    }

    public final void testGetAccessibleActionCount() {
        assertEquals(1, action.getAccessibleActionCount());
    }

    public final void testDoAccessibleAction() {
        assertFalse(action.doAccessibleAction(1));
        assertFalse(action.doAccessibleAction(-1));
        String command = "action";
        item.setActionCommand(command);
        item.addActionListener(actionListener);
        assertNull(event);
        assertTrue(action.doAccessibleAction(0));
        waitForAction(); // event is posted to the event queue, so have to wait
        assertNotNull(event);
        assertEquals(ActionEvent.ACTION_PERFORMED, event.getID());
        assertEquals(command, event.getActionCommand());
        assertEquals(0, event.getModifiers());
        long delta = System.currentTimeMillis() - event.getWhen();
        assertTrue(delta >= 0);
        assertTrue(delta < 1000);
    }

    public final void testGetAccessibleActionDescription() {
        assertNull(action.getAccessibleActionDescription(-1));
        assertEquals("click", action.getAccessibleActionDescription(0));
        assertNull(action.getAccessibleActionDescription(1));
    }

    public final void testGetCurrentAccessibleValue() {
        assertEquals(new Integer(0), value.getCurrentAccessibleValue());
    }

    public final void testGetMaximumAccessibleValue() {
        assertEquals(new Integer(0), value.getMaximumAccessibleValue());
    }

    public final void testGetMinimumAccessibleValue() {
        assertEquals(new Integer(0), value.getMinimumAccessibleValue());
    }

    public final void testSetCurrentAccessibleValue() {
        assertFalse(value.setCurrentAccessibleValue(new Integer(-1)));
        assertEquals(new Integer(0), value.getCurrentAccessibleValue());
        assertFalse(value.setCurrentAccessibleValue(new Integer(0)));
        assertFalse(value.setCurrentAccessibleValue(new Integer(1)));
        assertEquals(new Integer(0), value.getCurrentAccessibleValue());
    }

    private void waitForAction() {
        int time = 0;
        int timeout = 32;
        int threshold = 60000;
        while ((event == null) && (time < threshold)) {
            robot.delay(timeout);
            time += timeout;
            timeout <<= 1;
        }
    }

}
