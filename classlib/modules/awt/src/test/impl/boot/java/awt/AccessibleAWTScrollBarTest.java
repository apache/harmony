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

import java.awt.Scrollbar.AccessibleAWTScrollBar;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;

import junit.framework.TestCase;

/**
 * AccessibleAWTScrollBarTest
 */
public class AccessibleAWTScrollBarTest extends TestCase {
    Scrollbar scrollbar;
    AccessibleContext ac;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scrollbar = new Scrollbar();
        ac = scrollbar.getAccessibleContext();
    }

    public final void testGetAccessibleRole() {
        assertSame(AccessibleRole.SCROLL_BAR, ac.getAccessibleRole());
    }

    public final void testGetAccessibleStateSet() {
        AccessibleStateSet ass = ac.getAccessibleStateSet();
        assertTrue(ass.contains(AccessibleState.VERTICAL));
        scrollbar = new Scrollbar(Scrollbar.HORIZONTAL);
        ac = scrollbar.getAccessibleContext();
        ass = ac.getAccessibleStateSet();
        assertFalse(ass.contains(AccessibleState.VERTICAL));
        assertTrue(ass.contains(AccessibleState.HORIZONTAL));
    }

    public final void testGetAccessibleValue() {
        assertNotNull(ac.getAccessibleValue());
        assertTrue(ac.getAccessibleValue() instanceof AccessibleAWTScrollBar);
    }

    public final void testAccessibleAWTScrollBar() {
        assertTrue(ac instanceof AccessibleAWTScrollBar);
    }

    public final void testGetCurrentAccessibleValue() {
        AccessibleValue av = ac.getAccessibleValue();
        assertEquals(new Integer(0), av.getCurrentAccessibleValue());
        int val = 13;
        scrollbar.setValue(val);
        assertEquals(new Integer(val), av.getCurrentAccessibleValue());
    }

    public final void testGetMaximumAccessibleValue() {
        AccessibleValue av = ac.getAccessibleValue();
        assertEquals(new Integer(100), av.getMaximumAccessibleValue());
        int val = 666;
        scrollbar.setMaximum(val);
        assertEquals(new Integer(val), av.getMaximumAccessibleValue());
    }

    public final void testGetMinimumAccessibleValue() {
        AccessibleValue av = ac.getAccessibleValue();
        assertEquals(new Integer(0), av.getMinimumAccessibleValue());
        int val = -10;
        scrollbar.setMinimum(val);
        assertEquals(new Integer(val), av.getMinimumAccessibleValue());
    }

    public final void testSetCurrentAccessibleValue() {
        AccessibleValue av = ac.getAccessibleValue();
        int val = 25;
        assertTrue(av.setCurrentAccessibleValue(new Integer(25)));
        assertEquals(val, scrollbar.getValue());
        assertEquals(new Integer(val), av.getCurrentAccessibleValue());
        assertTrue(av.setCurrentAccessibleValue(new Integer(Integer.MAX_VALUE)));
        assertEquals(90, scrollbar.getValue());
    }

}
