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
import java.awt.LayoutManager;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.SwingTestCase;

/**
 * Test BasicInternalFrameUI.InternalFrameLayout class
 *
 */
public class BasicInternalFrameUI$InternalFrameLayoutTest extends SwingTestCase {
    private JInternalFrame frame;

    private BasicInternalFrameUI ui;

    private LayoutManager layout;

    private JComponent titlePane;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JInternalFrame();
        frame.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 20));
        ui = new BasicInternalFrameUI(frame);
        frame.setUI(ui);
        layout = frame.getLayout();
        titlePane = (JComponent) frame.getComponent(1);
        titlePane.setPreferredSize(new Dimension(100, 20));
        titlePane.setMinimumSize(new Dimension(80, 10));
    }

    @Override
    protected void tearDown() throws Exception {
        titlePane = null;
        ui = null;
        layout = null;
        frame.dispose();
        frame = null;
        super.tearDown();
    }

    public void testPreferredLayoutSize() {
        Dimension rootPanePreferredSize = new Dimension(32, 21);
        frame.getRootPane().setPreferredSize(rootPanePreferredSize);
        // only northPane is set
        // 1.5 seems to add Insests before calculating max between TitlePane width
        // and RootPane width which is likely a bug.
        if (isHarmony()) {
            assertEquals("1 pane", new Dimension(130, 61), layout.preferredLayoutSize(frame));
        } else {
            assertEquals("1 pane", new Dimension(100, 61), layout.preferredLayoutSize(frame));
        }
        assertEquals("rootPane's preferred size not changed", rootPanePreferredSize, frame
                .getRootPane().getPreferredSize());
        titlePane.setPreferredSize(new Dimension(10, 20));
        assertEquals("1 pane", new Dimension(62, 61), layout.preferredLayoutSize(frame));
        assertEquals("rootPane's preferred size not changed", rootPanePreferredSize, frame
                .getRootPane().getPreferredSize());
        // all panes are set
        ui.getNorthPane().setPreferredSize(new Dimension(14, 18));
        ui.setWestPane(new JPanel());
        ui.getWestPane().setPreferredSize(new Dimension(11, 22));
        ui.setSouthPane(new JPanel());
        ui.getSouthPane().setPreferredSize(new Dimension(12, 23));
        ui.setEastPane(new JPanel());
        ui.getEastPane().setPreferredSize(new Dimension(13, 24));
        assertEquals("all panes", new Dimension(86, 82), layout.preferredLayoutSize(frame));
    }

    /*
     * Class under test for Dimension minimumLayoutSize(Container)
     */
    public void testMinimumLayoutSize() {
        frame.getRootPane().setMinimumSize(new Dimension(32, 21));
        // only northPane is set
        assertEquals("1 pane", new Dimension(110, 30), layout.minimumLayoutSize(frame));
        // all panes are set
        ui.getNorthPane().setMinimumSize(new Dimension(14, 18));
        ui.setWestPane(new JPanel());
        ui.getWestPane().setMinimumSize(new Dimension(11, 22));
        ui.setSouthPane(new JPanel());
        ui.getSouthPane().setMinimumSize(new Dimension(12, 23));
        ui.setEastPane(new JPanel());
        ui.getEastPane().setMinimumSize(new Dimension(13, 24));
        assertEquals("all panes", new Dimension(44, 38), layout.minimumLayoutSize(frame));
    }

    /*
     * Class under test for void layoutContainer(Container)
     */
    public void testLayoutContainer() {
        frame.getRootPane().setPreferredSize(new Dimension(9, 21));
        frame.setSize(100, 200);
        // only northPane is set
        layout.layoutContainer(frame);
        assertEquals(new Rectangle(10, 25, 70, 160), frame.getRootPane().getBounds());
        assertEquals(new Rectangle(10, 5, 70, 20), ui.getNorthPane().getBounds());
        // all panes are set
        JPanel button = new JPanel();
        button.setPreferredSize(new Dimension(11, 12));
        ui.setWestPane(button);
        button = new JPanel();
        button.setPreferredSize(new Dimension(11, 12));
        ui.setSouthPane(button);
        button = new JPanel();
        button.setPreferredSize(new Dimension(11, 12));
        ui.setEastPane(button);
        layout.layoutContainer(frame);
        assertEquals(new Rectangle(21, 25, 48, 148), frame.getRootPane().getBounds());
        assertEquals(new Rectangle(10, 5, 70, 20), ui.getNorthPane().getBounds());
        assertEquals(new Rectangle(10, 25, 11, 148), ui.getWestPane().getBounds());
        assertEquals(new Rectangle(10, 173, 70, 12), ui.getSouthPane().getBounds());
        // 1.5 fails because of its bug in layout
        if (isHarmony()) {
            assertEquals(new Rectangle(69, 25, 11, 148), ui.getEastPane().getBounds());
        } else {
            assertEquals(new Rectangle(48, 25, 11, 148), ui.getEastPane().getBounds());
        }
    }

    /*
     * Class under test for void addLayoutComponent(String, Component)
     */
    public void testAddLayoutComponent() {
        JPanel button = new JPanel();
        layout.addLayoutComponent("ok", button);
        assertFalse("not added", frame.isAncestorOf(button));
    }

    /*
     * Class under test for void removeLayoutComponent(Component)
     */
    public void testRemoveLayoutComponent() {
        layout.layoutContainer(frame);
        Rectangle bounds = frame.getRootPane().getBounds();
        layout.removeLayoutComponent(ui.getNorthPane());
        layout.layoutContainer(frame);
        assertEquals("nothing changed", bounds, frame.getRootPane().getBounds());
    }
}
