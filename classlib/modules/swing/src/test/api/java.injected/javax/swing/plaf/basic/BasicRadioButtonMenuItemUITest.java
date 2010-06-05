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

public class BasicRadioButtonMenuItemUITest extends BasicMenuItemUITest {
    protected BasicRadioButtonMenuItemUI radioUI;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        radioUI = new BasicRadioButtonMenuItemUI();
        menuItemUI = radioUI;
        prefix = "RadioButtonMenuItem.";
    }

    @Override
    protected void tearDown() throws Exception {
        radioUI = null;
        menuItemUI = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicRadioButtonMenuItemUI.createUI(JComponent)'
     */
    @Override
    public void testCreateUI() {
        ComponentUI ui1 = BasicRadioButtonMenuItemUI.createUI(null);
        ComponentUI ui2 = BasicRadioButtonMenuItemUI.createUI(null);
        assertTrue(ui1 instanceof BasicRadioButtonMenuItemUI);
        assertNotSame(ui1, ui2);
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicRadioButtonMenuItemUI.getPropertyPrefix()'
     */
    @Override
    public void testGetPropertyPrefix() {
        assertEquals("RadioButtonMenuItem", menuItemUI.getPropertyPrefix());
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicRadioButtonMenuItemUI.processMouseEvent(JMenuItem, MouseEvent, MenuElement[], MenuSelectionManager)'
     * as method has an empty body we wo't test it
     */
    public void testProcessMouseEvent() {
    }
}
