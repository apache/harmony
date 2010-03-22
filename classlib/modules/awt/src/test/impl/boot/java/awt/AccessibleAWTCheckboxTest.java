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

import java.awt.Checkbox.AccessibleAWTCheckbox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;

import junit.framework.TestCase;

/**
 * AccessibleAWTCheckboxTest
 */
public class AccessibleAWTCheckboxTest extends TestCase {

    private Checkbox checkbox;
    private AccessibleContext ac;
    private PropertyChangeEvent lastPropEvent;
    private PropertyChangeListener propListener;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        checkbox = new Checkbox();
        assertEquals(0, checkbox.getItemListeners().length);
        ac = checkbox.getAccessibleContext();
        assertSame(ac, checkbox.getItemListeners()[0]);
        lastPropEvent = null;
        assertNotNull(ac);

        propListener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent pce) {
                lastPropEvent = pce;
            }
        };
    }

    public final void testGetAccessibleAction() {
        AccessibleAction aa = ac.getAccessibleAction();
        assertNotNull(aa);
        assertTrue(aa instanceof AccessibleAWTCheckbox);
    }

    public final void testGetAccessibleRole() {
        assertSame(AccessibleRole.CHECK_BOX, ac.getAccessibleRole());
    }

    public final void testGetAccessibleStateSet() {
        AccessibleStateSet ass = ac.getAccessibleStateSet();
        assertFalse(ass.contains(AccessibleState.CHECKED));
        checkbox.setState(true);
        ass = ac.getAccessibleStateSet();
        assertTrue(ass.contains(AccessibleState.CHECKED));

    }

    public final void testGetAccessibleValue() {
        AccessibleValue av = ac.getAccessibleValue();
        assertNotNull(av);
        assertTrue(av instanceof AccessibleAWTCheckbox);
    }

    public final void testAccessibleAWTCheckbox() {
        assertEquals(1, checkbox.getItemListeners().length);
        ac = checkbox.new AccessibleAWTCheckbox();
        // constructor has side-effect:
        // item listener is added
        assertEquals(2, checkbox.getItemListeners().length);
        assertSame(ac, checkbox.getItemListeners()[1]);
    }

    public final void testItemStateChanged() {
        ac.addPropertyChangeListener(propListener);
        assertNull(lastPropEvent);
        ((ItemListener) ac).itemStateChanged(new ItemEvent(checkbox, ItemEvent.ITEM_STATE_CHANGED,
                                                           checkbox, ItemEvent.DESELECTED));
        assertNotNull(lastPropEvent);
        assertSame(AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                   lastPropEvent.getPropertyName());
    }

    public final void testGetAccessibleActionCount() {
       assertEquals("no accessible actions", 0,
                    ac.getAccessibleAction().getAccessibleActionCount());
    }

    public final void testDoAccessibleAction() {
        assertFalse(ac.getAccessibleAction().doAccessibleAction(0));
    }

    public final void testGetAccessibleActionDescription() {
        assertNull(ac.getAccessibleAction().getAccessibleActionDescription(0));
    }

    public final void testGetCurrentAccessibleValue() {
        assertNull(ac.getAccessibleValue().getCurrentAccessibleValue());
    }

    public final void testGetMaximumAccessibleValue() {
        assertNull(ac.getAccessibleValue().getMaximumAccessibleValue());
    }

    public final void testGetMinimumAccessibleValue() {
        assertNull(ac.getAccessibleValue().getMinimumAccessibleValue());
    }

    public final void testSetCurrentAccessibleValue() {
        AccessibleValue av = ac.getAccessibleValue();
        assertFalse(av.setCurrentAccessibleValue(new Integer(0)));
        assertFalse(av.setCurrentAccessibleValue(new Integer(1)));

    }
}
