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

import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

public class BasicSeparatorUITest extends SwingTestCase {
    protected BasicSeparatorUI ui;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ui = new BasicSeparatorUI();
    }

    @Override
    protected void tearDown() throws Exception {
        ui = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicSeparatorUI.getPreferredSize(JComponent)'
     */
    public void testGetSizes() {
        JSeparator separator1 = new JSeparator(SwingConstants.HORIZONTAL);
        JSeparator separator2 = new JSeparator(SwingConstants.VERTICAL);
        assertNull(ui.getMinimumSize(new JButton()));
        separator1.setUI(ui);
        assertEquals(new Dimension(0, 2), ui.getPreferredSize(separator1));
        separator2.setUI(ui);
        assertEquals(new Dimension(2, 0), ui.getPreferredSize(separator2));
        assertNull(ui.getMaximumSize(new JButton()));
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicSeparatorUI.createUI(JComponent)'
     */
    public void testCreateUI() {
        assertNotNull("created UI is not null", BasicSeparatorUI.createUI(new JButton()));
        assertTrue("created UI is of the proper class",
                BasicSeparatorUI.createUI(null) instanceof BasicSeparatorUI);
        assertNotSame("created UI is of unique", BasicSeparatorUI.createUI(null),
                BasicSeparatorUI.createUI(null));
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicSeparatorUI.installDefaults(JSeparator)'
     */
    public void testInstallUninstallDefaults() {
        JSeparator separator = new JSeparator();
        separator.setUI(ui);
        ui.uninstallDefaults(separator);
        assertNull(ui.highlight);
        assertNull(ui.shadow);
        UIManager.put("Separator.highlight", new ColorUIResource(1, 2, 3));
        UIManager.put("Separator.shadow", new ColorUIResource(10, 20, 30));
        UIManager.put("Separator.foreground", new ColorUIResource(11, 22, 33));
        UIManager.put("Separator.background", new ColorUIResource(22, 33, 44));
        ui.installDefaults(separator);
        ui.installUI(separator);
        assertEquals(new ColorUIResource(22, 33, 44), separator.getBackground());
        assertEquals(new ColorUIResource(11, 22, 33), separator.getForeground());
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicSeparatorUI.installListeners(JSeparator)'
     */
    public void testInstallUninstallListeners() {
    }
    
    /**
     * Auxiliary class for testUninstallDefaults()
     * */
    class BasicSeparatorUIForTest extends BasicSeparatorUI
    {
        public void uninstallDefaults(JSeparator s){
            super.uninstallDefaults(s);
        }
    }

    /**
     * Regression test for HARMONY-2636
     * */
    public void testUninstallDefaults() throws NullPointerException {
        BasicSeparatorUIForTest localBasicSeparatorUI = new BasicSeparatorUIForTest();
        localBasicSeparatorUI.uninstallDefaults(null);
    }
}
