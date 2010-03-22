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
package javax.swing.plaf.basic;

import javax.swing.plaf.ComponentUI;

public class BasicCheckBoxMenuItemUITest extends BasicMenuItemUITest {
    protected BasicCheckBoxMenuItemUI checkBoxUI;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        checkBoxUI = new BasicCheckBoxMenuItemUI();
        menuItemUI = checkBoxUI;
        prefix = "CheckBoxMenuItem.";
    }

    @Override
    protected void tearDown() throws Exception {
        checkBoxUI = null;
        menuItemUI = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicCheckBoxMenuItemUI.createUI(JComponent)'
     */
    @Override
    public void testCreateUI() {
        ComponentUI ui1 = BasicCheckBoxMenuItemUI.createUI(null);
        ComponentUI ui2 = BasicCheckBoxMenuItemUI.createUI(null);
        assertTrue(ui1 instanceof BasicCheckBoxMenuItemUI);
        assertNotSame(ui1, ui2);
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicCheckBoxMenuItemUI.getPropertyPrefix()'
     */
    @Override
    public void testGetPropertyPrefix() {
        assertEquals("CheckBoxMenuItem", menuItemUI.getPropertyPrefix());
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicCheckBoxMenuItemUI.processMouseEvent(JMenuItem, MouseEvent, MenuElement[], MenuSelectionManager)'
     */
    public void testProcessMouseEvent() {
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicCheckBoxMenuItemUI.getMinimumSize(null)/getMaximumSize(null)'
     */
    public void testGetSizes() {
        try { //Regression test for HARMONY-2695
            checkBoxUI.getMinimumSize(null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        try { //Regression test for HARMONY-2695
            checkBoxUI.getMaximumSize(null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }
}
