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
 * @author Sergey Burlak
 */
package javax.swing.plaf.metal;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.tree.DefaultMutableTreeNode;

public class MetalTreeUITest extends SwingTestCase {
    private MetalTreeUI ui;

    private JTree tree;

    private DefaultMutableTreeNode root;

    private DefaultMutableTreeNode node1;

    private DefaultMutableTreeNode node2;

    private DefaultMutableTreeNode node3;

    private DefaultMutableTreeNode node11;

    private DefaultMutableTreeNode node21;

    private DefaultMutableTreeNode node22;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        root = new DefaultMutableTreeNode("root");
        node1 = new DefaultMutableTreeNode("node1");
        node2 = new DefaultMutableTreeNode("node2");
        node3 = new DefaultMutableTreeNode("node3");
        node11 = new DefaultMutableTreeNode("node11");
        node21 = new DefaultMutableTreeNode("node21");
        node22 = new DefaultMutableTreeNode("node22");
        root.add(node1);
        node1.add(node11);
        node11.add(new DefaultMutableTreeNode("node111"));
        root.add(node1);
        node2.add(node21);
        node2.add(node22);
        root.add(node2);
        root.add(node3);
        tree = new JTree(root);
        ui = new MetalTreeUI();
        tree.setUI(ui);
    }

    @Override
    protected void tearDown() throws Exception {
        ui = null;
        tree = null;
        root = null;
        node1 = null;
        node2 = null;
        node3 = null;
        node11 = null;
        node21 = null;
        node22 = null;
        super.tearDown();
    }

    public void testPaint() {
        Graphics g = createTestGraphics();
        g.setClip(0, 0, 10, 10);
        ui.paint(g, tree);
    }

    public void testUninstallUI() {
        ui.uninstallUI(tree);
        assertNotNull(ui.getExpandedIcon());
        assertNotNull(ui.getCollapsedIcon());
    }

    public void testInstallUI() {
        ui.uninstallUI(tree);
        ui.installUI(tree);
        assertNotNull(ui.getExpandedIcon());
        assertNotNull(ui.getCollapsedIcon());
    }

    public void testCreateUI() {
        assertNotSame(MetalTreeUI.createUI(tree), MetalTreeUI.createUI(tree));
    }

    public void testGetHorizontalLegBuffer() {
        assertEquals(4, ui.getHorizontalLegBuffer());
    }

    public void testIsLocationInExpandControl() throws Exception {
        tree.setShowsRootHandles(false);
        tree.expandPath(tree.getPathForRow(1));
        assertFalse(ui.isLocationInExpandControl(0, 0, -26, 25));
        assertTrue(ui.isLocationInExpandControl(0, 0, -25, 25));
        assertTrue(ui.isLocationInExpandControl(0, 0, -1, 25));
        assertFalse(ui.isLocationInExpandControl(0, 0, 0, 25));
        assertFalse(ui.isLocationInExpandControl(1, 0, -26, 25));
        assertTrue(ui.isLocationInExpandControl(1, 0, -25, 25));
        assertTrue(ui.isLocationInExpandControl(1, 0, -1, 25));
        assertFalse(ui.isLocationInExpandControl(1, 0, 0, 25));
        assertFalse(ui.isLocationInExpandControl(2, 0, -26, 25));
        assertTrue(ui.isLocationInExpandControl(2, 0, -25, 25));
        assertTrue(ui.isLocationInExpandControl(2, 0, -1, 25));
        assertFalse(ui.isLocationInExpandControl(2, 0, 0, 25));
        assertFalse(ui.isLocationInExpandControl(3, 0, -26, 25));
        assertTrue(ui.isLocationInExpandControl(3, 0, -25, 25));
        assertTrue(ui.isLocationInExpandControl(3, 0, -1, 25));
        assertFalse(ui.isLocationInExpandControl(3, 0, 0, 25));
        assertFalse(ui.isLocationInExpandControl(0, 1, -6, 25));
        assertTrue(ui.isLocationInExpandControl(0, 1, -5, 25));
        assertTrue(ui.isLocationInExpandControl(0, 1, 19, 25));
        assertFalse(ui.isLocationInExpandControl(0, 1, 20, 25));
        assertFalse(ui.isLocationInExpandControl(1, 1, -6, 20));
        assertTrue(ui.isLocationInExpandControl(1, 1, -5, 20));
        assertTrue(ui.isLocationInExpandControl(1, 1, 19, 20));
        assertFalse(ui.isLocationInExpandControl(1, 1, 20, 20));
        assertFalse(ui.isLocationInExpandControl(2, 1, -6, 20));
        assertTrue(ui.isLocationInExpandControl(2, 1, -5, 20));
        assertTrue(ui.isLocationInExpandControl(2, 1, 19, 20));
        assertFalse(ui.isLocationInExpandControl(2, 1, 20, 20));
        assertFalse(ui.isLocationInExpandControl(0, 2, 14, -20));
        assertTrue(ui.isLocationInExpandControl(0, 2, 15, -20));
        assertTrue(ui.isLocationInExpandControl(0, 2, 39, -20));
        assertFalse(ui.isLocationInExpandControl(0, 2, 40, -20));
        assertFalse(ui.isLocationInExpandControl(1, 2, 14, -20));
        assertTrue(ui.isLocationInExpandControl(1, 2, 15, -20));
        assertTrue(ui.isLocationInExpandControl(1, 2, 39, -20));
        assertFalse(ui.isLocationInExpandControl(1, 2, 40, -20));
        assertFalse(ui.isLocationInExpandControl(2, 2, 14, -20));
        assertTrue(ui.isLocationInExpandControl(2, 2, 15, -20));
        assertTrue(ui.isLocationInExpandControl(2, 2, 39, -20));
        assertFalse(ui.isLocationInExpandControl(2, 2, 40, -20));
        tree.setShowsRootHandles(true);
        assertFalse(ui.isLocationInExpandControl(0, 0, -6, 25));
        assertTrue(ui.isLocationInExpandControl(0, 0, -5, 25));
        assertTrue(ui.isLocationInExpandControl(0, 0, 19, 25));
        assertFalse(ui.isLocationInExpandControl(0, 0, 20, 25));
        tree.setShowsRootHandles(false);
        ui.setExpandedIcon(new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
            }

            public int getIconWidth() {
                return 0;
            }

            public int getIconHeight() {
                return 100;
            }
        });
        assertFalse(ui.isLocationInExpandControl(0, 0, -17, 25));
        assertTrue(ui.isLocationInExpandControl(0, 0, -16, 25));
        assertTrue(ui.isLocationInExpandControl(0, 0, -10, 25));
        assertFalse(ui.isLocationInExpandControl(0, 0, -9, 25));
    }
}
