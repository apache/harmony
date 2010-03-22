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
 */
package javax.swing;

import javax.accessibility.AccessibleRole;
import javax.swing.plaf.SeparatorUI;
import javax.swing.plaf.basic.BasicSeparatorUI;
import junit.framework.TestCase;

public class JSeparatorTest extends TestCase {
    protected JSeparator separator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        separator = new JSeparator();
    }

    @Override
    protected void tearDown() throws Exception {
        separator = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.JSeparator.JSeparator()'
     */
    public void testJSeparator() {
        assertEquals(SwingConstants.HORIZONTAL, separator.getOrientation());
    }

    /*
     * Test method for 'javax.swing.JSeparator.JSeparator(int)'
     */
    public void testJSeparatorInt() {
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        assertEquals(SwingConstants.HORIZONTAL, separator.getOrientation());
        separator = new JSeparator(SwingConstants.VERTICAL);
        assertEquals(SwingConstants.VERTICAL, separator.getOrientation());
        try {
            separator = new JSeparator(1000);
            fail("no exception has been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * Test method for 'javax.swing.JSeparator.getAccessibleContext()'
     */
    public void testGetAccessibleContext() {
        boolean assertedValue = (separator.getAccessibleContext() != null && separator
                .getAccessibleContext().getClass().getName().equals(
                        "javax.swing.JSeparator$AccessibleJSeparator"));
        assertTrue("AccessibleContext created properly ", assertedValue);
        assertEquals("AccessibleRole", AccessibleRole.SEPARATOR, separator
                .getAccessibleContext().getAccessibleRole());
    }

    /*
     * Test method for 'javax.swing.JSeparator.getUIClassID()'
     */
    public void testGetUIClassID() {
        assertEquals("SeparatorUI", separator.getUIClassID());
    }

    /*
     * Test method for 'javax.swing.JSeparator.getUI()'
     */
    public void testGetUI() {
        assertNotNull("ui is returned ", separator.getUI());
    }

    /*
     * Test method for 'javax.swing.JSeparator.setUI(SeparatorUI)'
     */
    public void testSetUISeparatorUI() {
        SeparatorUI ui1 = new BasicSeparatorUI();
        SeparatorUI ui2 = new BasicSeparatorUI();
        separator.setUI(ui1);
        assertEquals(ui1, separator.ui);
        assertEquals(ui1, separator.getUI());
        separator.setUI(ui2);
        assertEquals(ui2, separator.ui);
        assertEquals(ui2, separator.getUI());
    }

    /*
     * Test method for 'javax.swing.JSeparator.setOrientation(int)'
     */
    public void testGetSetOrientation() {
        separator.setOrientation(SwingConstants.HORIZONTAL);
        assertEquals(SwingConstants.HORIZONTAL, separator.getOrientation());
        separator.setOrientation(SwingConstants.VERTICAL);
        assertEquals(SwingConstants.VERTICAL, separator.getOrientation());
        try {
            separator.setOrientation(1000);
            fail("no exception has been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testIsFocusable() {
        // Regression test for HARMONY-2631
        assertFalse(new JSeparator().isFocusable());
    }
}
