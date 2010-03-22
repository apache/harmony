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
package javax.swing.plaf.metal;

import java.awt.LayoutManager;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.SwingTestCase;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;

public class MetalRootPaneUITest extends SwingTestCase {
    private JRootPane rootPane;

    private MetalRootPaneUI ui;

    private JFrame frame;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JFrame();
        rootPane = frame.getRootPane();
        ui = new MetalRootPaneUI();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Constructor for MetalRootPaneUITest.
     */
    public MetalRootPaneUITest(final String name) {
        super(name);
    }

    /*
     * Class under test for void installUI(JComponent)
     */
    public void testInstallUI() {
        // test install with windowDecorationStyle = JRootPane.NONE
        Border saveBorder = rootPane.getBorder();
        LayoutManager saveLayout = rootPane.getLayout();
        int saveComponentCount = rootPane.getLayeredPane().getComponentCount();
        ui.installUI(rootPane);
        assertTrue("didn't install border", rootPane.getBorder() == saveBorder);
        assertTrue("didn't install layout", rootPane.getLayout() == saveLayout);
        assertTrue("didn't install titlePane",
                rootPane.getLayeredPane().getComponentCount() == saveComponentCount);
        // test install with windowDecorationStyle = JRootPane.FRAME
        rootPane.setWindowDecorationStyle(JRootPane.FRAME);
        ui.uninstallUI(rootPane);
        saveBorder = rootPane.getBorder();
        saveLayout = rootPane.getLayout();
        saveComponentCount = rootPane.getLayeredPane().getComponentCount();
        ui.installUI(rootPane);
        assertTrue("border != null", rootPane.getBorder() != null);
        assertTrue("installed border", rootPane.getBorder() != saveBorder);
        assertTrue("layout != null", rootPane.getBorder() != null);
        assertTrue("installed layout", rootPane.getLayout() != saveLayout);
        assertTrue("installed titlePane",
                rootPane.getLayeredPane().getComponentCount() == saveComponentCount + 1);
    }

    /*
     * Class under test for void uninstallUI(JComponent)
     */
    public void testUninstallUI() {
        // test uninstall with windowDecorationStyle = JRootPane.NONE
        ui.installUI(rootPane);
        Border saveBorder = rootPane.getBorder();
        LayoutManager saveLayout = rootPane.getLayout();
        int saveComponentCount = rootPane.getLayeredPane().getComponentCount();
        ui.uninstallUI(rootPane);
        assertTrue("didn't uninstall border", rootPane.getBorder() == saveBorder);
        assertTrue("didn't uninstall layout", rootPane.getLayout() == saveLayout);
        assertTrue("didn't uninstall titlePane",
                rootPane.getLayeredPane().getComponentCount() == saveComponentCount);
        // test uninstall with windowDecorationStyle = JRootPane.FRAME
        ui.installUI(rootPane);
        rootPane.setWindowDecorationStyle(JRootPane.FRAME);
        saveBorder = rootPane.getBorder();
        saveLayout = rootPane.getLayout();
        saveComponentCount = rootPane.getLayeredPane().getComponentCount();
        ui.uninstallUI(rootPane);
        assertTrue("uninstalled border", rootPane.getBorder() != saveBorder);
        assertTrue("uninstalled layout", rootPane.getLayout() != saveLayout);
        assertTrue("uninstalled titlePane",
                rootPane.getLayeredPane().getComponentCount() == saveComponentCount - 1);
    }

    /*
     * Class under test for void propertyChange(PropertyChangeEvent)
     */
    public void testPropertyChange() {
        rootPane.setUI(ui);
        Border saveBorder = rootPane.getBorder();
        LayoutManager saveLayout = rootPane.getLayout();
        int saveComponentCount = rootPane.getLayeredPane().getComponentCount();
        // test windowDecorationStyle = JRootPane.FRAME
        rootPane.setWindowDecorationStyle(JRootPane.FRAME);
        assertTrue("border != null", rootPane.getBorder() != null);
        assertTrue("installed border", rootPane.getBorder() != saveBorder);
        assertTrue("layout != null", rootPane.getBorder() != null);
        assertTrue("installed layout", rootPane.getLayout() != saveLayout);
        assertTrue("installed titlePane",
                rootPane.getLayeredPane().getComponentCount() == saveComponentCount + 1);
        // test windowDecorationStyle = JRootPane.NONE
        rootPane.setWindowDecorationStyle(JRootPane.NONE);
        assertTrue("uninstalled border", rootPane.getBorder() == saveBorder);
        assertTrue("uninstalled layout", rootPane.getLayout() == saveLayout);
        assertTrue("uninstalled titlePane",
                rootPane.getLayeredPane().getComponentCount() == saveComponentCount);
        // the border is not instanceof UIResource, must not be changed
        saveBorder = BorderFactory.createEmptyBorder();
        rootPane.setBorder(saveBorder);
        assertTrue("didn't install border", rootPane.getBorder() == saveBorder);
    }

    /*
     * Class under test for ComponentUI createUI(JComponent)
     */
    public void testCreateUI() {
        ComponentUI ui = MetalRootPaneUI.createUI(rootPane);
        assertTrue("not null", ui != null);
        assertTrue("instanceof MetalRootPaneUI", ui instanceof MetalRootPaneUI);
        ComponentUI ui2 = MetalRootPaneUI.createUI(rootPane);
        assertTrue("stateful", ui != ui2);
    }

    /*
     * Class under test for MetalRootPaneUI()
     */
    public void testMetalRootPaneUI() {
        // test that it doesn't crash
        ui = new MetalRootPaneUI();
    }
}
