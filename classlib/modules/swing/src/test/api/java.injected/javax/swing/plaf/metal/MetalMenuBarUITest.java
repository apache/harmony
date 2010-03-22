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
package javax.swing.plaf.metal;

import javax.swing.JMenuBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuBarUITest;

public class MetalMenuBarUITest extends BasicMenuBarUITest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        menuBarUI = new MetalMenuBarUI();
    }

    /*
     * Test method for 'javax.swing.plaf.metal.MetalMenuBarUI.createUI(JComponent)'
     */
    @Override
    public void testCreateUI() {
        JMenuBar bar = new JMenuBar();
        ComponentUI ui1 = MetalMenuBarUI.createUI(bar);
        ComponentUI ui2 = MetalMenuBarUI.createUI(bar);
        assertTrue(ui1 instanceof MetalMenuBarUI);
        assertNotSame(ui1, ui2);
    }
}
