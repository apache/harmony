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

import java.awt.Button.AccessibleAWTButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;

import junit.framework.TestCase;

/**
 * AccessibleAWTButtonTest
 */
public class AccessibleAWTButtonTest extends TestCase {
    private Button button;
    private AccessibleContext ac;
    private ActionEvent action;
    private Robot robot;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        button = new Button();
        ac = button.getAccessibleContext();
        action = null;
        robot = new Robot();
        assertNotNull(ac);
    }

    public final void testGetAccessibleName() {
        assertEquals("", ac.getAccessibleName());
        String label = "button";
        button.setLabel(label);
        assertEquals(label, ac.getAccessibleName());
    }

    public final void testGetAccessibleAction() {
        AccessibleAction aa = ac.getAccessibleAction();
        assertNotNull(aa);
        assertTrue(aa instanceof AccessibleAWTButton);
    }

    public final void testGetAccessibleRole() {
        assertSame(AccessibleRole.PUSH_BUTTON, ac.getAccessibleRole());
    }

    public final void testGetAccessibleValue() {
        AccessibleValue av = ac.getAccessibleValue();
        assertNotNull(av);
        assertTrue(av instanceof AccessibleAWTButton);
    }

    public final void testAccessibleAWTButton() {
        assertNotNull(button.new AccessibleAWTButton());
    }

    public final void testGetAccessibleActionCount() {
        assertEquals("button has 1 accessible action", 1,
                     ac.getAccessibleAction().getAccessibleActionCount());
    }

    public final void testGetAccessibleActionDescription() {
        AccessibleAction aa = ac.getAccessibleAction();
        assertNull(aa.getAccessibleActionDescription(-1));
        assertEquals("click", aa.getAccessibleActionDescription(0));
        assertNull(aa.getAccessibleActionDescription(1));
    }

    private void waitForAction() {
        int time = 0;
        int timeout = 32;
        int threshold = 60000;
        while ((action == null) && (time < threshold)) {
            robot.delay(timeout);
            time += timeout;
            timeout <<= 1;
        }
    }

    public final void testDoAccessibleAction() {
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                action = ae;

            }
        });
        assertFalse(ac.getAccessibleAction().doAccessibleAction(-1));
        assertFalse(ac.getAccessibleAction().doAccessibleAction(1));
        assertTrue(ac.getAccessibleAction().doAccessibleAction(0));

        waitForAction();
        assertNotNull(action);
        assertEquals(0, action.getModifiers());
        assertEquals(0l, action.getWhen());
    }

    public final void testGetCurrentAccessibleValue() {
        AccessibleValue av = ac.getAccessibleValue();
        assertEquals(new Integer(0), av.getCurrentAccessibleValue());
    }

    public final void testSetCurrentAccessibleValue() {
        AccessibleValue av = ac.getAccessibleValue();
        Integer value = new Integer(-1);
        assertFalse(av.setCurrentAccessibleValue(value));
        assertEquals(new Integer(0), av.getCurrentAccessibleValue());
        assertFalse(av.setCurrentAccessibleValue(new Integer(Integer.MAX_VALUE)));
        assertEquals(new Integer(0), av.getCurrentAccessibleValue());
    }

    public final void testGetMinimumAccessibleValue() {
        AccessibleValue av = ac.getAccessibleValue();
        assertEquals(new Integer(0), av.getMinimumAccessibleValue());
    }

    public final void testGetMaximumAccessibleValue() {
        AccessibleValue av = ac.getAccessibleValue();
        assertEquals(0, av.getMaximumAccessibleValue().intValue());
    }

}
