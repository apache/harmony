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
 * @author Vadim L. Bogdanov
 */
package javax.swing.plaf.basic;

import java.awt.Dimension;
import javax.swing.JToolBar;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

public class BasicToolBarSeparatorUITest extends SwingTestCase {
    private JToolBar.Separator separator;

    private BasicToolBarSeparatorUI ui;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        separator = new JToolBar.Separator();
        ui = new BasicToolBarSeparatorUI();
        separator.setUI(ui);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public BasicToolBarSeparatorUITest(final String name) {
        super(name);
    }

    public void testPaint() {
        // Note: painting code, cannot test
    }

    public void testGetPreferredSize() {
        assertEquals(UIManager.getDimension("ToolBar.separatorSize"), ui
                .getPreferredSize(separator));
        ui = new BasicToolBarSeparatorUI();
        assertEquals(UIManager.getDimension("ToolBar.separatorSize"), ui
                .getPreferredSize(separator));
    }

    public void testCreateUI() {
        ComponentUI ui1 = BasicToolBarSeparatorUI.createUI(separator);
        assertTrue(ui1 instanceof BasicToolBarSeparatorUI);
        ComponentUI ui2 = BasicToolBarSeparatorUI.createUI(separator);
        assertNotSame(ui1, ui2);
    }

    public void testInstallDefaults() {
        ui.installDefaults(separator);
        assertNull(separator.getBackground());
        assertNull(separator.getForeground());
        assertEquals(UIManager.getDimension("ToolBar.separatorSize"), separator
                .getSeparatorSize());
        Dimension size = new Dimension(1, 2);
        separator.setSeparatorSize(size);
        ui.installDefaults(separator);
        assertEquals(size, separator.getSeparatorSize());
    }

    public void testBasicToolBarSeparatorUI() {
        // nothing to test
    }
}
