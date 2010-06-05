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

import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRootPane;
import javax.swing.SwingTestCase;

public class MetalRootPaneUI$MetalRootLayoutTest extends SwingTestCase {
    private JRootPane rootPane;

    private MetalRootPaneUI ui;

    private JFrame frame;

    private LayoutManager2 layout;

    private JComponent titlePane;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
        JFrame.setDefaultLookAndFeelDecorated(true);
        frame = new JFrame();
        rootPane = frame.getRootPane();
        ((JComponent) rootPane).setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 20));
        ui = new MetalRootPaneUI();
        rootPane.setUI(ui);
        layout = (LayoutManager2) rootPane.getLayout();
        titlePane = (JComponent) rootPane.getLayeredPane().getComponent(1);
    }

    public MetalRootPaneUI$MetalRootLayoutTest(final String name) {
        super(name);
    }

    public void testPreferredLayoutSize() {
        ((JComponent) frame.getContentPane()).setPreferredSize(new Dimension(200, 300));
        titlePane.setPreferredSize(new Dimension(20, 20));
        assertEquals(new Dimension(230, 340), layout.preferredLayoutSize(rootPane));
    }

    public void testMinimumLayoutSize() {
        ((JComponent) frame.getContentPane()).setMinimumSize(new Dimension(200, 300));
        titlePane.setMinimumSize(new Dimension(20, 20));
        assertEquals(new Dimension(230, 340), layout.minimumLayoutSize(rootPane));
    }

    public void testMaximumLayoutSize() {
        ((JComponent) frame.getContentPane()).setMaximumSize(new Dimension(200, 300));
        titlePane.setMaximumSize(new Dimension(30, 400));
        if (isHarmony()) {
            assertEquals(new Dimension(230, 720), layout.maximumLayoutSize(rootPane));
        } else {
            assertEquals(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE), layout
                    .maximumLayoutSize(rootPane));
        }
    }

    public void testLayoutContainer() {
        final Dimension rootPaneDimension = new Dimension(640, 480);
        final Rectangle glassPaneBounds = new Rectangle(10, 5, 610, 460);
        final Rectangle titlePaneBounds = new Rectangle(0, 0, 610, 20);
        frame.setSize(rootPaneDimension);
        rootPane.setSize(rootPaneDimension);
        titlePane.setPreferredSize(new Dimension(100, 20));
        layout.layoutContainer(rootPane);
        // test without menu
        assertEquals(glassPaneBounds, rootPane.getGlassPane().getBounds());
        assertEquals(glassPaneBounds, rootPane.getLayeredPane().getBounds());
        assertEquals(titlePaneBounds, titlePane.getBounds());
        assertEquals(new Rectangle(0, 20, 610, 440), rootPane.getContentPane().getBounds());
        // test with menu
        JMenuBar menuBar = new JMenuBar();
        menuBar.setPreferredSize(new Dimension(150, 30));
        menuBar.add(new JMenu("Menu"));
        rootPane.setJMenuBar(menuBar);
        layout.layoutContainer(rootPane);
        assertEquals(glassPaneBounds, rootPane.getGlassPane().getBounds());
        assertEquals(glassPaneBounds, rootPane.getLayeredPane().getBounds());
        assertEquals(titlePaneBounds, titlePane.getBounds());
        assertEquals(new Rectangle(0, 20, 610, 30), rootPane.getJMenuBar().getBounds());
        assertEquals(new Rectangle(0, 50, 610, 410), rootPane.getContentPane().getBounds());
    }
}
