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

import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

public class MetalPopupMenuSeparatorUITest extends MetalSeparatorUITest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ui = new MetalPopupMenuSeparatorUI();
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicSeparatorUI.getPreferredSize(JComponent)'
     */
    @Override
    public void testGetSizes() {
        JSeparator separator1 = new JSeparator(SwingConstants.HORIZONTAL);
        JSeparator separator2 = new JSeparator(SwingConstants.VERTICAL);
        assertNull(ui.getMinimumSize(new JButton()));
        separator1.setUI(ui);
        assertEquals(new Dimension(0, 4), ui.getPreferredSize(separator1));
        separator2.setUI(ui);
        assertEquals(new Dimension(0, 4), ui.getPreferredSize(separator2));
        assertNull(ui.getMaximumSize(new JButton()));
    }

    /*
     * Test method for 'javax.swing.plaf.metal.MetalPopupMenuSeparatorUI.createUI(JComponent)'
     */
    @Override
    public void testCreateUI() {
        assertNotNull("created UI is not null", MetalPopupMenuSeparatorUI
                .createUI(new JButton()));
        assertTrue("created UI is of the proper class", MetalPopupMenuSeparatorUI
                .createUI(null) instanceof MetalPopupMenuSeparatorUI);
        assertNotSame("created UI is of unique", MetalPopupMenuSeparatorUI.createUI(null),
                MetalPopupMenuSeparatorUI.createUI(null));
    }
}
