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

import junit.framework.TestCase;

/**
 * CheckboxGroupTest
 */
public class CheckboxGroupTest extends TestCase {

    CheckboxGroup group;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        group = new CheckboxGroup();
    }

    public final void testCheckboxGroup() {
        assertNotNull(group);
        assertNull(group.getSelectedCheckbox());
    }

    public final void testGetSelectedCheckbox() {
        assertNull(group.getSelectedCheckbox());
    }

    public final void testSetSelectedCheckbox() {
        Checkbox checkbox = new Checkbox();
        group.setSelectedCheckbox(checkbox);
        assertNull(group.getSelectedCheckbox());
        checkbox.setCheckboxGroup(group);
        group.setSelectedCheckbox(checkbox);
        assertSame(checkbox, group.getSelectedCheckbox());

    }

    @SuppressWarnings("deprecation")
    public final void testGetCurrent() {
        assertNull(group.getCurrent());
    }

    @SuppressWarnings("deprecation")
    public final void testSetCurrent() {
        Checkbox cb1 = new Checkbox();
        Checkbox cb2 = new Checkbox();
        group.setCurrent(cb1);
        assertNull(group.getSelectedCheckbox());
        cb1.setCheckboxGroup(group);
        assertNull(group.getSelectedCheckbox());
        cb2.setCheckboxGroup(group);
        assertNull(group.getSelectedCheckbox());
        group.setCurrent(cb1);
        assertSame(cb1, group.getSelectedCheckbox());
        assertTrue(cb1.getState());
        group.setCurrent(cb2);
        assertSame(cb2, group.getSelectedCheckbox());
        assertTrue(cb2.getState());
        assertFalse(cb1.getState());
        group.setCurrent(null);
        assertNull(group.getSelectedCheckbox());

    }

    public final void testMoveCheckBox() {
        CheckboxGroup group1 = new CheckboxGroup();
        Checkbox cb = new Checkbox();
        Checkbox cb1 = new Checkbox();
        Checkbox cb2 = new Checkbox("", true);
        cb.setCheckboxGroup(group1);
        cb1.setCheckboxGroup(group);
        cb2.setCheckboxGroup(group);
        assertSame(cb2, group.getSelectedCheckbox());
        assertNull(group1.getSelectedCheckbox());
        cb2.setCheckboxGroup(group1);
        assertSame(cb2, group1.getSelectedCheckbox());
        assertNull(group.getSelectedCheckbox());
        cb1.setState(true);
        assertSame(cb1, group.getSelectedCheckbox());
        cb1.setCheckboxGroup(group1);
        assertNull(group.getSelectedCheckbox());
        assertSame(cb2, group1.getSelectedCheckbox());
        assertFalse(cb1.getState());

    }

}
