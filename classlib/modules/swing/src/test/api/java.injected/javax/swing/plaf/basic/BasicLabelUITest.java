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
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;

public class BasicLabelUITest extends SwingTestCase {
    private JLabel label;

    private BasicLabelUI ui;

    public BasicLabelUITest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        label = new JLabel();
        ui = new BasicLabelUI();
    }

    @Override
    protected void tearDown() throws Exception {
        ui = null;
        label = null;
    }

    public void testCreateUI() throws Exception {
        ComponentUI componentUI = BasicLabelUI.createUI(new JLabel());
        assertTrue(componentUI instanceof BasicLabelUI);
        assertEquals(componentUI, BasicLabelUI.createUI(new JLabel()));
    }

    public void testGetMinimumMaximumPreferredSize() throws Exception {
        label.setText("Any");
        label.setFont(label.getFont().deriveFont(20f));
        Dimension prefSize = ui.getPreferredSize(label);
        assertTrue(prefSize.width > 0);
        assertTrue(prefSize.height > 0);
        final Dimension d = new Dimension(100, 100);
        ui = new BasicLabelUI() {
            @Override
            public Dimension getPreferredSize(final JComponent c) {
                return d;
            }
        };
        assertSame(d, ui.getPreferredSize(label));
        assertSame(d, ui.getMinimumSize(label));
        assertSame(d, ui.getMaximumSize(label));
    }

    public void testInstallUI() throws Exception {
        Color c = new ColorUIResource(Color.black);
        label.setForeground(new ColorUIResource(c));
        label.setBackground(new ColorUIResource(c));
        ui.installUI(label);
        assertEquals(UIManager.getColor("Label.foreground"), label.getForeground());
        assertEquals(UIManager.getColor("Label.background"), label.getBackground());
    }

    public void testInstallUninstallKeyboardActions() throws Exception {
        label.getInputMap().clear();
        label.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).clear();
        label.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).clear();
        assertEquals(0, label.getInputMap().size());
        assertEquals(0, label.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).size());
        assertEquals(0, label.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).size());
        ui.installKeyboardActions(label);
        assertEquals(0, label.getInputMap().size());
        assertEquals(0, label.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).size());
        assertEquals(0, label.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).size());
    }

    public void testInstallUninstallComponents() throws Exception {
        label.removeAll();
        assertEquals(0, label.getComponentCount());
        ui.installComponents(label);
        assertEquals(0, label.getComponentCount());
    }
    
    private class BasicLabelUIForTest extends BasicLabelUI { 
        public BasicLabelUIForTest(){ 
            super(); 
        } 

        public void uninstallDefaults(JLabel l) { 
            super.uninstallDefaults(l); 
        } 
    } 
    
    /**
     * Regression test for HARMONY-2637
     * */
    public void testUninstallDefaults() throws NullPointerException { 
       BasicLabelUIForTest bu = new BasicLabelUIForTest(); 
       bu.uninstallDefaults(null); 
    } 

    public void testInstallUI_getInheritsPopupMenu() throws NullPointerException { 
       // Regression test for HARMONY-2570
       JLabel label = new JLabel();
       ui.installUI(label);
       assertTrue(label.getInheritsPopupMenu());
    } 
}
