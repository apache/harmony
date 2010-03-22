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
 * Created on 08.09.2004

 */
package javax.swing.plaf.basic;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPanelTest;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.PanelUI;
import junit.framework.TestCase;

public class BasicPanelUITest extends TestCase {
    protected BasicPanelUI panelUI = null;

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(JPanelTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        panelUI = new BasicPanelUI();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testUninstallUI() {
        JPanel panel = new JPanel();
        panel.setUI(panelUI);
        Border border1 = BorderFactory.createEmptyBorder();
        panel.setBorder(border1);
        panelUI.uninstallUI(panel);
        assertNotNull(panel.getBackground());
        assertNotNull(panel.getForeground());
        assertNotNull(panel.getBorder());
        UIManager.put("Panel.background", Color.red);
        Border border2 = new BorderUIResource(BorderFactory.createEmptyBorder());
        UIManager.put("Panel.border", border2);
        panel.setUI(panelUI);
        panelUI.uninstallUI(panel);
        assertEquals(Color.red, panel.getBackground());
        assertNotNull(panel.getForeground());
        assertEquals(border1, panel.getBorder());
    }

    public void testInstallUI() {
        JPanel panel = new JPanel();
        assertTrue("opaque", panel.isOpaque());
        UIManager.put("Panel.background", new ColorUIResource(Color.red));
        UIManager.put("Panel.foreground", new ColorUIResource(Color.yellow));
        Border border2 = new BorderUIResource(BorderFactory.createEmptyBorder());
        UIManager.put("Panel.border", border2);
        panel.setOpaque(false);
        panel.setUI(panelUI);
        panelUI.installUI(panel);
        assertEquals(Color.red, panel.getBackground());
        assertEquals(Color.yellow, panel.getForeground());
        assertEquals(border2, panel.getBorder());
        assertFalse("opaque", panel.isOpaque());
        Border border1 = BorderFactory.createEmptyBorder();
        panel.setBorder(border1);
        panel.setUI(panelUI);
        panelUI.installUI(panel);
        assertEquals(border1, panel.getBorder());
    }

    public void testCreateUI() {
        JComponent component = new JPanel();
        ComponentUI ui = BasicPanelUI.createUI(component);
        assertTrue(ui != null && (ui instanceof PanelUI));
        component = new JButton();
        ui = BasicPanelUI.createUI(component);
        assertTrue(ui != null);
    }
}
