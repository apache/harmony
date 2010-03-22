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
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import javax.swing.BasicSwingTestCase;
import javax.swing.SwingUtilities;
import javax.swing.SwingWaitTestCase;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.basic.BasicTreeUI.ComponentHandler;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.FixedHeightLayoutCache;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;
import javax.swing.tree.VariableHeightLayoutCache;
import javax.swing.tree.AbstractLayoutCache.NodeDimensions;

public class BasicTreeUITest extends BasicSwingTestCase {
    private BasicTreeUI ui;

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
            UIManager.setLookAndFeel(new BasicLookAndFeel() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isNativeLookAndFeel() {
                    return true;
                }

                @Override
                public boolean isSupportedLookAndFeel() {
                    return true;
                }

                @Override
                public String getDescription() {
                    return null;
                }

                @Override
                public String getID() {
                    return null;
                }

                @Override
                public String getName() {
                    return null;
                }
            });
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
        root.add(node1);
        node2.add(node21);
        node2.add(node22);
        root.add(node2);
        root.add(node3);
        tree = new JTree(root);
        ui = new BasicTreeUI();
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
        g.setClip(0, 0, 100, 100);
        ui.paint(g, tree);
    }

    public void testGetPreferredSize() {
        assertFalse(ui.validCachedPreferredSize);
        assertEquals(new Dimension(0, 0), ui.preferredSize);
        assertNotSame(new Dimension(0, 0), ui.getPreferredSize(tree));
        assertTrue(ui.validCachedPreferredSize);
        assertEquals(ui.preferredSize, ui.getPreferredSize(tree));
        assertNotSame(ui.preferredSize, ui.getPreferredSize(tree));
        assertNotSame(ui.getPreferredSize(tree), ui.getPreferredSize(tree));
        ui.preferredSize = new Dimension(100, 100);
        assertNotNull(ui.getPreferredSize(tree));
        assertEquals(ui.preferredSize, ui.getPreferredSize(tree));
        assertEquals(ui.preferredSize, new Dimension(100, 100));
        assertEquals(new Dimension(100, 100), ui.getPreferredSize(tree, false));
        assertEquals(new Dimension(100, 100), ui.getPreferredSize(tree, true));
        assertTrue(ui.validCachedPreferredSize);
        ui.validCachedPreferredSize = false;
        assertNotSame(new Dimension(100, 100), ui.getPreferredSize(tree, true));
    }

    public void testGetMinimumSize() {
        assertEquals(new Dimension(0, 0), ui.getMinimumSize(tree));
        ui.preferredMinSize = new Dimension(100, 100);
        assertEquals(ui.preferredMinSize, ui.getMinimumSize(tree));
        ui.preferredMinSize = new Dimension(-100, -100);
        assertEquals(ui.preferredMinSize, ui.getMinimumSize(tree));
        ui.preferredMinSize = null;
        assertEquals(new Dimension(0, 0), ui.getMinimumSize(tree));
    }

    public void testGetMaximumSize() {
        assertEquals(ui.getPreferredSize(null), ui.getMaximumSize(null));
        ui.preferredSize = new Dimension(-100, -100);
        assertEquals(new Dimension(-100, -100), ui.getMaximumSize(tree));
        ui = new BasicTreeUI() {
            @Override
            public Dimension getPreferredSize(final JComponent c) {
                return null;
            }
        };
        ui.installUI(tree);
        assertNull(ui.getMaximumSize(tree));
    }

    public void testUninstallUI() {
    }

    public void testInstallUI() {
        ui.uninstallUI(tree);
        ui.installUI(tree);
        assertNotNull(ui.treeModel);
        assertNotNull(ui.treeSelectionModel);
    }

    public void testCreateUI() {
        assertNotSame(BasicTreeUI.createUI(tree), BasicTreeUI.createUI(tree));
    }

    public void testGetClosestPathForLocation() {
        assertEquals(ui.getClosestPathForLocation(tree, 5, 5), ui.treeState.getPathClosestTo(5,
                5));
    }

    public void testGetPathBounds() {
        tree.setBorder(BorderFactory.createEmptyBorder(5, 12, 15, 20));
        TreePath p1 = new TreePath(new Object[] { root, node2 });
        TreePath p2 = new TreePath(new Object[] { root, node3 });
        Rectangle bounds = ui.treeState.getBounds(p1, new Rectangle());
        bounds.x += 12;
        bounds.y += 5;
        assertEquals(ui.getPathBounds(tree, p1), bounds);
        bounds = ui.treeState.getBounds(p2, new Rectangle());
        bounds.x += 12;
        bounds.y += 5;
        assertEquals(ui.getPathBounds(tree, p2), bounds);
    }

    public void testGetPathForRow() {
        TreePath p = new TreePath(new Object[] { root, node2 });
        assertTrue(tree.isRootVisible());
        assertNull(ui.getPathForRow(tree, 7));
        assertEquals(p, ui.treeState.getPathForRow(2));
        assertEquals(p, ui.getPathForRow(new JTree(), 2));
        tree.expandPath(p);
        assertEquals(new TreePath(new Object[] { root, node2, node22 }), ui.getPathForRow(tree,
                4));
        assertEquals(new TreePath(new Object[] { root, node3 }), ui.getPathForRow(tree, 5));
        ui.treeState = new VariableHeightLayoutCache() {
            @Override
            public TreePath getPathForRow(final int row) {
                return new TreePath(new Object[] { node3 });
            }
        };
        assertEquals(new TreePath(new Object[] { node3 }), ui.getPathForRow(tree, -400));
        assertEquals(ui.treeState.getPathForRow(2), ui.getPathForRow(tree, 5));
    }

    public void testGetRowCount() {
        assertTrue(tree.isRootVisible());
        assertEquals(4, ui.treeState.getRowCount());
        assertEquals(4, ui.getRowCount(new JTree()));
        TreePath p = new TreePath(new Object[] { root, node2 });
        tree.expandPath(p);
        assertEquals(6, ui.getRowCount(tree));
        ui.treeState = new VariableHeightLayoutCache() {
            @Override
            public int getRowCount() {
                return -200;
            }
        };
        assertEquals(ui.getRowCount(new JTree()), ui.treeState.getRowCount());
        assertEquals(-200, ui.getRowCount(tree));
    }

    public void testGetRowForPath() {
        assertTrue(tree.isRootVisible());
        TreePath p = new TreePath(new Object[] { root, node2 });
        assertEquals(2, ui.getRowForPath(tree, p));
        assertEquals(2, ui.getRowForPath(new JTree(), p));
        tree.expandPath(new TreePath(new Object[] { root, node1 }));
        ui.treeState = new VariableHeightLayoutCache() {
            @Override
            public int getRowForPath(final TreePath p) {
                return -200;
            }
        };
        assertEquals(ui.getRowForPath(tree, p), ui.treeState.getRowForPath(p));
        assertEquals(-200, ui.getRowForPath(tree, p));
    }

    public void testIsEditing() {
        assertFalse(ui.isEditing(tree));
        ui.editingComponent = new JLabel();
        assertTrue(ui.isEditing(tree));
    }

    public void testGetEditingPath() {
        assertNull(ui.getEditingPath(tree));
        ui.editingPath = new TreePath(root);
        assertNotNull(ui.getEditingPath(tree));
    }

    public void testStartEditingAtPath() {
        assertFalse(ui.isEditing(tree));
        ui.startEditingAtPath(tree, new TreePath(root));
        assertFalse(ui.isEditing(tree));
        assertNull(ui.getCellEditor());
        tree.setEditable(true);
        assertNotNull(ui.getCellEditor());
        assertNull(ui.editingComponent);
        assertNull(ui.editingPath);
        assertEquals(0, ui.editingRow);
        TreePath path = new TreePath(root).pathByAddingChild(node1);
        ui.startEditingAtPath(tree, path);
        assertTrue(ui.isEditing(tree));
        assertEquals(path, ui.getEditingPath(tree));
        assertEquals(1, ui.editingRow);
        assertNotNull(ui.editingComponent);
        assertEquals(node1.getUserObject(), ui.getCellEditor().getCellEditorValue());
        ui.cancelEditing(tree);
        assertFalse(ui.isEditing(tree));
        ui.startEditingAtPath(tree, null);
        assertFalse(ui.isEditing(tree));
    }

    public void testStartEditing() {
        assertFalse(ui.isEditing(tree));
        ui.startEditing(new TreePath(root), null);
        assertFalse(ui.isEditing(tree));
        tree.setEditable(true);
        assertFalse(ui.isEditing(tree));
        ui.startEditing(new TreePath(root), null);
        assertTrue(ui.isEditing(tree));
        ui.cancelEditing(tree);
        assertFalse(ui.isEditing(tree));
        tree.setCellEditor(new DefaultTreeCellEditor(tree, (DefaultTreeCellRenderer) tree
                .getCellRenderer()) {
            @Override
            public boolean shouldSelectCell(final EventObject event) {
                return false;
            }
        });
        ui.startEditing(new TreePath(root), null);
        assertTrue(ui.isEditing(tree));
    }

    public void testCancelEditing() {
        String initialValue = node1.getUserObject().toString();
        tree.setEditable(true);
        ui.startEditing(new TreePath(root).pathByAddingChild(node1), null);
        JTextComponent editor = (JTextComponent) ((Container) ui.editingComponent)
                .getComponent(0);
        assertEquals(initialValue, editor.getText());
        editor.setText("any value");
        ui.cancelEditing(tree);
        assertEquals(initialValue, node1.getUserObject());
        assertNull(ui.editingComponent);
        assertNull(ui.editingPath);
        assertEquals(1, ui.editingRow);
    }

    public void testStopEditing() {
        String initialValue = node1.getUserObject().toString();
        tree.setEditable(true);
        ui.startEditing(new TreePath(root).pathByAddingChild(node1), null);
        JTextComponent editor = (JTextComponent) ((Container) ui.editingComponent)
                .getComponent(0);
        assertEquals(initialValue, editor.getText());
        editor.setText("new value");
        assertTrue(ui.stopEditing(tree));
        assertEquals("new value", node1.getUserObject());
        assertNull(ui.editingComponent);
        assertNull(ui.editingPath);
        assertEquals(1, ui.editingRow);
        assertFalse(ui.stopEditing(tree));
    }

    public void testSetGetHashColor() {
        assertEquals(UIManager.getColor("Tree.hash"), ui.getHashColor());
        ui.setHashColor(Color.RED);
        assertEquals(Color.RED, ui.getHashColor());
    }

    public void testSetGetLeftChildIndent() {
        assertEquals(UIManager.getInt("Tree.leftChildIndent"), ui.getLeftChildIndent());
        ui.setLeftChildIndent(20);
        assertEquals(20, ui.getLeftChildIndent());
        ui.setLeftChildIndent(-20);
        assertEquals(-20, ui.getLeftChildIndent());
    }

    public void testSetGetRightChildIndent() {
        assertEquals(UIManager.getInt("Tree.rightChildIndent"), ui.getRightChildIndent());
        ui.setRightChildIndent(20);
        assertEquals(20, ui.getRightChildIndent());
        ui.setRightChildIndent(-20);
        assertEquals(-20, ui.getRightChildIndent());
    }

    public void testSetGetExpandedIcon() {
        assertNull(ui.getExpandedIcon());
        ImageIcon imageIcon = new ImageIcon("icon");
        ui.setExpandedIcon(imageIcon);
        assertEquals(imageIcon, ui.getExpandedIcon());
    }

    public void testSetGetCollapsedIcon() {
        assertNull(ui.getCollapsedIcon());
        ImageIcon imageIcon = new ImageIcon("icon");
        ui.setCollapsedIcon(imageIcon);
        assertEquals(imageIcon, ui.getCollapsedIcon());
    }

    public void testSetIsLargeModel() {
        assertFalse(ui.isLargeModel());
        ui.setLargeModel(true);
        assertTrue(ui.isLargeModel());
    }

    public void testSetGetRowHeight() {
        assertEquals(UIManager.getInt("Tree.rowHeight"), ui.getRowHeight());
        tree.setRowHeight(20);
        assertEquals(20, ui.getRowHeight());
        tree.setRowHeight(-20);
        assertEquals(-20, ui.getRowHeight());
    }

    public void testSetGetCellRenderer() {
        assertTrue(ui.getCellRenderer() instanceof DefaultTreeCellRenderer);
        DefaultTreeCellRenderer r = new DefaultTreeCellRenderer();
        tree.setCellRenderer(r);
        assertEquals(r, ui.getCellRenderer());
        assertEquals(r, ui.currentCellRenderer);
        ui.setCellRenderer(new DefaultTreeCellRenderer());
        assertEquals(r, ui.currentCellRenderer);
        assertEquals(r, ui.getCellRenderer());
        ui.createdRenderer = false;
        assertFalse(ui.createdRenderer);
        ui.currentCellRenderer = null;
        ui.setCellRenderer(null);
        assertFalse(ui.createdRenderer);
        assertEquals(r, ui.currentCellRenderer);
        assertEquals(r, ui.getCellRenderer());
        tree.setCellRenderer(new DefaultTreeCellRenderer());
        assertNotNull(tree.getCellRenderer());
        assertNotNull(ui.getCellRenderer());
    }

    public void testSetGetModel() {
        assertTrue(ui.getModel() instanceof DefaultTreeModel);
        DefaultTreeModel m = new DefaultTreeModel(new DefaultMutableTreeNode("root"));
        ui.setModel(m);
        assertSame(m, ui.getModel());
        assertSame(m, ui.treeModel);
    }

    public void testSetIsRootVisible() {
        assertTrue(ui.isRootVisible());
        tree.setRootVisible(false);
        assertFalse(ui.isRootVisible());
        assertFalse(ui.treeState.isRootVisible());
        ui.setRootVisible(true);
        assertFalse(ui.isRootVisible());
        assertTrue(ui.treeState.isRootVisible());
    }

    public void testSetGetShowsRootHandles() {
        assertFalse(ui.getShowsRootHandles());
        tree.setShowsRootHandles(true);
        assertTrue(ui.getShowsRootHandles());
        if (!isHarmony()) {
            ui.setShowsRootHandles(false);
            assertTrue(ui.getShowsRootHandles());
        }
    }

    public void testSetGetCellEditor() {
        assertNull(ui.getCellEditor());
        DefaultTreeCellEditor editor = new DefaultTreeCellEditor(tree,
                new DefaultTreeCellRenderer());
        tree.setCellEditor(editor);
        assertEquals(editor, ui.getCellEditor());
        ui.setCellEditor(new DefaultTreeCellEditor(tree, new DefaultTreeCellRenderer()));
        assertEquals(editor, ui.getCellEditor());
    }

    public void testSetIsEditable() {
        assertFalse(ui.isEditable());
        assertNull(ui.getCellEditor());
        tree.setEditable(true);
        assertTrue(ui.isEditable());
        assertNotNull(ui.getCellEditor());
        ui.setEditable(false);
        assertTrue(ui.isEditable());
        assertNotNull(ui.getCellEditor());
        tree.setEditable(false);
        assertFalse(ui.isEditable());
        assertNotNull(ui.getCellEditor());
    }

    public void testSetGetSelectionModel() {
        assertTrue(ui.getSelectionModel() instanceof DefaultTreeSelectionModel);
        DefaultTreeSelectionModel m = new DefaultTreeSelectionModel();
        ui.setSelectionModel(m);
        assertSame(m, ui.getSelectionModel());
        assertSame(m, ui.treeSelectionModel);
    }

    public void testPrepareForUIInstallUninstall() {
        ui = new BasicTreeUI();
        ui.tree = tree;
        ui.prepareForUIInstall();
        assertEquals(new Dimension(), ui.preferredSize);
        assertEquals(0, ui.drawingCache.size());
        ui.prepareForUIUninstall();
    }

    public void testCompleteUIInstallUninstall() {
        assertEquals(0, ui.drawingCache.size());
        assertEquals(tree.getModel(), ui.treeModel);
        ui.completeUIUninstall();
        assertNull(ui.drawingCache);
        assertNull(ui.treeModel);
    }

    public void testInstallUninstallDefaults() {
        assertNull(ui.collapsedIcon);
        assertNull(ui.expandedIcon);
        assertNull(ui.preferredMinSize);
        assertSame(tree, ui.tree);
        assertNotNull(ui.currentCellRenderer);
        assertNull(ui.cellEditor);
        assertNotNull(ui.rendererPane);
        assertNotNull(ui.preferredSize);
        assertNotNull(ui.treeState);
        assertNotNull(ui.drawingCache);
        assertNotNull(ui.nodeDimensions);
        assertNotNull(ui.treeModel);
        assertNotNull(ui.treeSelectionModel);
        assertNull(ui.editingComponent);
        assertNull(ui.editingPath);
        assertEquals(UIManager.getInt("Tree.leftChildIndent"), ui.leftChildIndent);
        assertEquals(UIManager.getInt("Tree.rightChildIndent"), ui.rightChildIndent);
        assertEquals(ui.leftChildIndent + ui.rightChildIndent, ui.totalChildIndent);
        assertEquals(-1, ui.lastSelectedRow);
        assertEquals(0, ui.depthOffset);
        assertEquals(0, ui.editingRow);
        assertTrue(ui.createdRenderer);
        assertFalse(ui.createdCellEditor);
        assertTrue(ui.stopEditingInCompleteEditing);
        assertFalse(ui.validCachedPreferredSize);
        assertFalse(ui.editorHasDifferentSize);
        assertFalse(ui.largeModel);
        ui.uninstallDefaults();
    }

    public void testInstallUninstallListeners() {
        if (!isHarmony()) {
            return;
        }
        assertTrue(hasListener(tree.getFocusListeners(), BasicTreeUI.FocusHandler.class));
        assertTrue(hasListener(tree.getKeyListeners(), BasicTreeUI.KeyHandler.class));
        assertTrue(hasListener(tree.getMouseListeners(), BasicTreeUI.MouseHandler.class));
        assertTrue(hasListener(tree.getMouseMotionListeners(), BasicTreeUI.MouseHandler.class));
        assertTrue(hasListener(tree.getPropertyChangeListeners(),
                BasicTreeUI.PropertyChangeHandler.class));
        assertTrue(hasListener(tree.getTreeExpansionListeners(),
                BasicTreeUI.TreeExpansionHandler.class));
        assertTrue(hasListener(((DefaultTreeModel) tree.getModel()).getTreeModelListeners(),
                BasicTreeUI.TreeModelHandler.class));
        assertTrue(hasListener(((DefaultTreeSelectionModel) tree.getSelectionModel())
                .getTreeSelectionListeners(), BasicTreeUI.TreeSelectionHandler.class));
        assertTrue(hasListener(((DefaultTreeSelectionModel) tree.getSelectionModel())
                .getPropertyChangeListeners(),
                BasicTreeUI.SelectionModelPropertyChangeHandler.class));
        ui.uninstallListeners();
        assertFalse(hasListener(tree.getFocusListeners(), BasicTreeUI.FocusHandler.class));
        assertFalse(hasListener(tree.getKeyListeners(), BasicTreeUI.KeyHandler.class));
        assertFalse(hasListener(tree.getMouseListeners(), BasicTreeUI.MouseHandler.class));
        assertFalse(hasListener(tree.getMouseMotionListeners(), BasicTreeUI.MouseHandler.class));
        assertFalse(hasListener(tree.getPropertyChangeListeners(),
                BasicTreeUI.PropertyChangeHandler.class));
        assertFalse(hasListener(tree.getTreeExpansionListeners(),
                BasicTreeUI.TreeExpansionHandler.class));
        assertFalse(hasListener(tree.getTreeSelectionListeners(),
                BasicTreeUI.TreeSelectionHandler.class));
        assertFalse(hasListener(((DefaultTreeModel) tree.getModel()).getTreeModelListeners(),
                BasicTreeUI.TreeModelHandler.class));
        assertFalse(hasListener(((DefaultTreeSelectionModel) tree.getSelectionModel())
                .getTreeSelectionListeners(), BasicTreeUI.TreeSelectionHandler.class));
        assertFalse(hasListener(((DefaultTreeSelectionModel) tree.getSelectionModel())
                .getPropertyChangeListeners(),
                BasicTreeUI.SelectionModelPropertyChangeHandler.class));
    }

    public void testInstallUninstallKeyboardActions() {
        ui.installKeyboardActions();
        assertNotNull(SwingUtilities.getUIInputMap(tree, JComponent.WHEN_FOCUSED));
        assertNotNull(SwingUtilities.getUIInputMap(tree,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
        ui.uninstallKeyboardActions();
        assertNull(SwingUtilities.getUIInputMap(tree, JComponent.WHEN_FOCUSED));
        assertNull(SwingUtilities.getUIInputMap(tree,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
    }

    public void testInstallUninstallComponents() {
        ui.uninstallComponents();
        assertEquals(0, tree.getComponentCount());
        ui.installComponents();
        assertEquals(1, tree.getComponentCount());
        assertSame(ui.rendererPane, tree.getComponent(0));
        assertEquals(new Rectangle(), ui.rendererPane.getBounds());
        assertFalse(ui.rendererPane.isVisible());
        ui.uninstallComponents();
        assertEquals(0, tree.getComponentCount());
        assertNotNull(ui.rendererPane);
    }

    public void testCreateNodeDimensions() throws Exception {
        tree.setRowHeight(40);
        ui.setLeftChildIndent(10);
        ui.setRightChildIndent(20);
        ui.depthOffset = 3;
        assertNotNull(ui.createNodeDimensions());
        assertTrue(ui.createNodeDimensions() instanceof BasicTreeUI.NodeDimensionsHandler);
        assertNotSame(ui.createNodeDimensions(), ui.createNodeDimensions());
        tree.setFont(tree.getFont().deriveFont(40f));
        NodeDimensions n = ui.createNodeDimensions();
        Component c = ui.getCellRenderer().getTreeCellRendererComponent(tree, root.toString(),
                false, false, false, 0, false);
        assertEquals(new Rectangle(ui.totalChildIndent * ui.depthOffset, 0, c
                .getPreferredSize().width, c.getPreferredSize().height), n.getNodeDimensions(
                root, 0, 0, true, new Rectangle()));
        c = ui.getCellRenderer().getTreeCellRendererComponent(tree, node1.toString(), false,
                false, false, 0, false);
        assertEquals(new Rectangle(ui.totalChildIndent * ui.depthOffset, 0, c
                .getPreferredSize().width, c.getPreferredSize().height), n.getNodeDimensions(
                node1, 0, 0, true, new Rectangle()));
        Rectangle rectangle = new Rectangle(10, 10, 20, 20);
        c = ui.getCellRenderer().getTreeCellRendererComponent(tree, root.toString(), false,
                false, false, 0, false);
        assertEquals(new Rectangle(ui.totalChildIndent * ui.depthOffset + ui.totalChildIndent
                * 3, rectangle.y, c.getPreferredSize().width, c.getPreferredSize().height), n
                .getNodeDimensions(root, 2, 3, true, rectangle));
    }

    public void testCreateNodeDimensionsRTL() throws Exception {
        tree.setRowHeight(40);
        tree.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        NodeDimensions n = ui.createNodeDimensions();
        Component c = ui.getCellRenderer().getTreeCellRendererComponent(tree, root.toString(),
                false, false, false, 0, false);
        assertEquals(new Rectangle(tree.getWidth()
                - (ui.totalChildIndent * ui.depthOffset + ui.totalChildIndent * 0)
                - c.getPreferredSize().width, 0, c.getPreferredSize().width, c
                .getPreferredSize().height), n.getNodeDimensions(root, 0, 0, true,
                new Rectangle()));
        assertEquals(new Rectangle(tree.getWidth()
                - (ui.totalChildIndent * ui.depthOffset + ui.totalChildIndent * 2)
                - c.getPreferredSize().width, 0, c.getPreferredSize().width, c
                .getPreferredSize().height), n.getNodeDimensions(root, 0, 2, true,
                new Rectangle()));
        c = ui.getCellRenderer().getTreeCellRendererComponent(tree, node1.toString(), false,
                false, false, 0, false);
        assertEquals(new Rectangle(tree.getWidth()
                - (ui.totalChildIndent * ui.depthOffset + ui.totalChildIndent * 0)
                - c.getPreferredSize().width, 0, c.getPreferredSize().width, c
                .getPreferredSize().height), n.getNodeDimensions(node1, 0, 0, true,
                new Rectangle()));
        assertEquals(new Rectangle(tree.getWidth()
                - (ui.totalChildIndent * ui.depthOffset + ui.totalChildIndent * 2)
                - c.getPreferredSize().width, 0, c.getPreferredSize().width, c
                .getPreferredSize().height), n.getNodeDimensions(node1, 0, 2, true,
                new Rectangle()));
    }

    public void testCreatePropertyChangeListener() {
        assertNotNull(ui.createPropertyChangeListener());
        if (isHarmony()) {
            assertTrue(ui.createPropertyChangeListener() instanceof BasicTreeUI.PropertyChangeHandler);
            assertNotSame(ui.createPropertyChangeListener(), ui.createPropertyChangeListener());
        }
    }

    public void testCreateMouseListener() {
        assertNotNull(ui.createMouseListener());
        if (isHarmony()) {
            assertTrue(ui.createMouseListener() instanceof BasicTreeUI.MouseHandler);
            assertNotSame(ui.createMouseListener(), ui.createMouseListener());
        }
    }

    public void testCreateFocusListener() {
        assertNotNull(ui.createFocusListener());
        if (isHarmony()) {
            assertTrue(ui.createFocusListener() instanceof BasicTreeUI.FocusHandler);
            assertNotSame(ui.createFocusListener(), ui.createFocusListener());
        }
    }

    public void testCreateKeyListener() {
        assertNotNull(ui.createKeyListener());
        if (isHarmony()) {
            assertTrue(ui.createKeyListener() instanceof BasicTreeUI.KeyHandler);
            assertNotSame(ui.createKeyListener(), ui.createKeyListener());
        }
    }

    public void testCreateSelectionModelPropertyChangeListener() {
        assertNotNull(ui.createSelectionModelPropertyChangeListener());
        if (isHarmony()) {
            assertTrue(ui.createSelectionModelPropertyChangeListener() instanceof BasicTreeUI.SelectionModelPropertyChangeHandler);
            assertNotSame(ui.createSelectionModelPropertyChangeListener(), ui
                    .createSelectionModelPropertyChangeListener());
        }
    }

    public void testCreateTreeSelectionListener() {
        assertNotNull(ui.createTreeSelectionListener());
        if (isHarmony()) {
            assertTrue(ui.createTreeSelectionListener() instanceof BasicTreeUI.TreeSelectionHandler);
            assertNotSame(ui.createTreeSelectionListener(), ui.createTreeSelectionListener());
        }
    }

    public void testCreateCellEditorListener() {
        assertNotNull(ui.createCellEditorListener());
        if (isHarmony()) {
            assertTrue(ui.createCellEditorListener() instanceof BasicTreeUI.CellEditorHandler);
            assertNotSame(ui.createCellEditorListener(), ui.createCellEditorListener());
        }
    }

    public void testCreateComponentListener() {
        assertNotNull(ui.createComponentListener());
        assertTrue(ui.createComponentListener() instanceof BasicTreeUI.ComponentHandler);
        assertNotSame(ui.createComponentListener(), ui.createComponentListener());
    }

    public void testCreateTreeExpansionListener() {
        assertNotNull(ui.createTreeExpansionListener());
        if (isHarmony()) {
            assertTrue(ui.createTreeExpansionListener() instanceof BasicTreeUI.TreeExpansionHandler);
            assertNotSame(ui.createTreeExpansionListener(), ui.createTreeExpansionListener());
        }
    }

    public void testCreateLayoutCache() {
        tree.setRowHeight(0);
        assertNotNull(ui.createLayoutCache());
        assertNotSame(ui.createLayoutCache(), ui.createLayoutCache());
        assertTrue(ui.createLayoutCache() instanceof VariableHeightLayoutCache);
        assertTrue(ui.treeState instanceof VariableHeightLayoutCache);
        tree.setRowHeight(10);
        assertTrue(ui.createLayoutCache() instanceof VariableHeightLayoutCache);
        assertTrue(ui.treeState instanceof VariableHeightLayoutCache);
        tree.setRowHeight(0);
        tree.setLargeModel(true);
        assertTrue(ui.createLayoutCache() instanceof VariableHeightLayoutCache);
        assertTrue(ui.treeState instanceof VariableHeightLayoutCache);
        tree.setRowHeight(10);
        assertTrue(ui.createLayoutCache() instanceof FixedHeightLayoutCache);
        assertTrue(ui.treeState instanceof FixedHeightLayoutCache);
    }

    public void testCreateCellRendererPane() {
        assertNotNull(ui.createCellRendererPane());
        assertNotSame(ui.createCellRendererPane(), ui.createCellRendererPane());
    }

    public void testCreateDefaultCellEditor() {
        assertTrue(ui.createDefaultCellEditor() instanceof DefaultTreeCellEditor);
        assertNotSame(ui.createDefaultCellEditor(), ui.createDefaultCellEditor());
    }

    public void testCreateDefaultCellRenderer() {
        assertTrue(ui.createDefaultCellRenderer() instanceof DefaultTreeCellRenderer);
        assertNotSame(ui.createDefaultCellRenderer(), ui.createDefaultCellRenderer());
    }

    public void testCreateTreeModelListener() {
        assertNotNull(ui.createTreeModelListener());
        if (isHarmony()) {
            assertTrue(ui.createTreeModelListener() instanceof BasicTreeUI.TreeModelHandler);
            assertNotSame(ui.createTreeModelListener(), ui.createTreeModelListener());
        }
    }

    public void testShouldPaintExpandControl() {
        TreePath rootPath = new TreePath(root);
        assertFalse(ui.shouldPaintExpandControl(rootPath, -10, false, false, false));
        assertFalse(ui.shouldPaintExpandControl(rootPath, 10, false, false, false));
        assertFalse(ui.shouldPaintExpandControl(rootPath, 100, true, true, false));
        TreePath path1 = rootPath.pathByAddingChild(node1);
        assertFalse(ui.shouldPaintExpandControl(path1, -10, false, false, true));
        assertTrue(ui.shouldPaintExpandControl(path1, 10, false, false, false));
        assertTrue(ui.shouldPaintExpandControl(path1, 100, false, true, false));
        assertTrue(ui.shouldPaintExpandControl(path1, -100, true, false, false));
        tree.setRootVisible(false);
        assertFalse(ui.shouldPaintExpandControl(rootPath, -1, false, false, false));
        assertFalse(ui.shouldPaintExpandControl(path1, 0, true, true, false));
        tree.setShowsRootHandles(true);
        assertTrue(ui.shouldPaintExpandControl(rootPath, -1, false, false, false));
        assertTrue(ui.shouldPaintExpandControl(path1, 0, true, true, false));
        tree.setShowsRootHandles(false);
        TreePath path11 = path1.pathByAddingChild(node11);
        assertFalse(ui.shouldPaintExpandControl(path11, -1, false, false, true));
        assertTrue(ui.shouldPaintExpandControl(path11, -1, false, false, false));
        tree.setRootVisible(true);
        TreePath unexisted = new TreePath(node1).pathByAddingChild(node11);
        assertTrue(ui.shouldPaintExpandControl(unexisted, -1, false, false, false));
    }

    public void testGetVerticalLegBuffer() {
        assertEquals(0, ui.getVerticalLegBuffer());
    }

    public void testGetHorizontalLegBuffer() {
        assertEquals(0, ui.getHorizontalLegBuffer());
    }

    public void testGetRowX() {
        assertEquals(0, ui.getRowX(-10, 0));
        assertEquals(ui.totalChildIndent, ui.getRowX(-10, 1));
        ui.totalChildIndent = 15;
        assertEquals(ui.totalChildIndent, ui.getRowX(-10, 1));
        assertEquals(3 * ui.totalChildIndent, ui.getRowX(-10, 3));
        ui.depthOffset = 10;
        assertEquals(ui.depthOffset * ui.totalChildIndent, ui.getRowX(-100, 0));
        assertEquals(ui.depthOffset * ui.totalChildIndent + 2 * ui.totalChildIndent, ui
                .getRowX(-100, 2));
        ui.depthOffset = 0;
        tree.setRootVisible(false);
        assertEquals(-ui.totalChildIndent, ui.getRowX(-10, 0));
        assertEquals(0, ui.getRowX(-10, 1));
        assertEquals(2 * ui.totalChildIndent, ui.getRowX(-10, 3));
        tree.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 20));
        assertEquals(-ui.totalChildIndent, ui.getRowX(-10, 0));
        assertEquals(0, ui.getRowX(-10, 1));
        assertEquals(2 * ui.totalChildIndent, ui.getRowX(-10, 3));
    }

    public void testUpdateLayoutCacheExpandedNodes() {
        ui.treeModel = null;
        ui.updateLayoutCacheExpandedNodes();
    }

    public void testUpdateExpandedDescendants() {
        TreePath pathToExpand = new TreePath(tree.getModel().getRoot())
                .pathByAddingChild(node1);
        tree.expandPath(pathToExpand);
        assertTrue(ui.treeState.isExpanded(pathToExpand));
        ui.treeState = new VariableHeightLayoutCache();
        ui.treeState.setModel(tree.getModel());
        assertFalse(ui.treeState.isExpanded(pathToExpand));
        ui.updateExpandedDescendants(new TreePath(tree.getModel().getRoot()));
        assertTrue(ui.treeState.isExpanded(pathToExpand));
    }

    public void testGetLastChildPath() {
        TreePath rootPath = new TreePath(root);
        TreePath lastChildPath = rootPath.pathByAddingChild(tree.getModel().getChild(root,
                tree.getModel().getChildCount(root) - 1));
        assertEquals(lastChildPath, ui.getLastChildPath(rootPath));
        TreePath leafPath = rootPath.pathByAddingChild(node1).pathByAddingChild(node11);
        assertNull(ui.getLastChildPath(leafPath));
    }

    public void testUpdateDepthOffset() {
        ui.depthOffset = -100;
        ui.updateDepthOffset();
        assertEquals(0, ui.depthOffset);
        tree.setRootVisible(false);
        ui.depthOffset = -100;
        ui.updateDepthOffset();
        assertEquals(-1, ui.depthOffset);
        tree.setRootVisible(true);
        assertEquals(0, ui.depthOffset);
        tree.setShowsRootHandles(true);
        assertEquals(1, ui.depthOffset);
    }

    public void testUpdateCellEditor() {
        DefaultTreeCellEditor e1 = new DefaultTreeCellEditor(tree,
                new DefaultTreeCellRenderer());
        tree.setCellEditor(e1);
        assertSame(e1, tree.getCellEditor());
        assertNull(ui.cellEditor);
        assertSame(e1, ui.getCellEditor());
        tree.setEditable(true);
        assertSame(e1, ui.cellEditor);
        assertSame(e1, ui.getCellEditor());
        DefaultTreeCellEditor e2 = new DefaultTreeCellEditor(tree,
                new DefaultTreeCellRenderer());
        ui.cellEditor = e2;
        assertSame(e1, ui.getCellEditor());
        assertSame(e2, ui.cellEditor);
        ui.updateCellEditor();
        assertSame(e1, ui.cellEditor);
        assertSame(e1, tree.getCellEditor());
        ui.cellEditor = null;
        assertFalse(ui.createdCellEditor);
        ui.createdCellEditor = true;
        assertTrue(ui.createdCellEditor);
        ui.updateCellEditor();
        assertFalse(ui.createdCellEditor);
        assertSame(e1, ui.cellEditor);
        assertSame(e1, tree.getCellEditor());
        tree.setCellRenderer(new DefaultTreeCellRenderer());
        assertFalse(ui.createdCellEditor);
        assertSame(e1, ui.cellEditor);
        assertSame(e1, tree.getCellEditor());
        ui.createdCellEditor = true;
        tree.setCellRenderer(new DefaultTreeCellRenderer());
        assertTrue(ui.createdCellEditor);
        assertNotSame(e1, ui.cellEditor);
        assertNotSame(e1, tree.getCellEditor());
    }

    public void testUpdateRenderer() {
        DefaultTreeCellRenderer r1 = new DefaultTreeCellRenderer();
        tree.setCellRenderer(r1);
        assertSame(r1, tree.getCellRenderer());
        assertSame(r1, ui.currentCellRenderer);
        assertSame(r1, ui.getCellRenderer());
        DefaultTreeCellRenderer r2 = new DefaultTreeCellRenderer();
        ui.currentCellRenderer = r2;
        assertSame(r2, ui.getCellRenderer());
        ui.updateRenderer();
        assertSame(r1, ui.currentCellRenderer);
        assertSame(r1, tree.getCellRenderer());
        ui.currentCellRenderer = null;
        assertFalse(ui.createdRenderer);
        ui.createdRenderer = true;
        assertTrue(ui.createdRenderer);
        ui.updateRenderer();
        assertFalse(ui.createdRenderer);
        assertSame(r1, ui.currentCellRenderer);
        assertSame(r1, tree.getCellRenderer());
    }

    public void testConfigureLayoutCache() {
        TreePath expandedPath = new TreePath(tree.getModel().getRoot())
                .pathByAddingChild(node1);
        tree.expandPath(expandedPath);
        ui.treeState = new VariableHeightLayoutCache();
        ui.configureLayoutCache();
        assertSame(tree.getModel(), ui.treeState.getModel());
        assertSame(tree.getSelectionModel(), ui.treeState.getSelectionModel());
        assertSame(ui.nodeDimensions, ui.treeState.getNodeDimensions());
        assertEquals(tree.getRowHeight(), ui.treeState.getRowHeight());
        assertTrue(ui.treeState.isExpanded(expandedPath));
        assertTrue(ui.treeState.isRootVisible());
    }

    public void testUpdateSize() {
        ui.preferredSize = new Dimension(100, 100);
        ui.preferredMinSize = new Dimension(200, 200);
        ui.validCachedPreferredSize = true;
        ui.updateSize();
        assertFalse(ui.validCachedPreferredSize);
        assertEquals(ui.preferredSize, new Dimension(100, 100));
        assertEquals(ui.preferredMinSize, new Dimension(200, 200));
    }

    public void testUpdateCachedPreferredSize() {
        Dimension originalSize = ui.getPreferredSize(tree);
        ui.preferredSize = new Dimension(100, 100);
        assertTrue(ui.validCachedPreferredSize);
        ui.validCachedPreferredSize = false;
        ui.updateCachedPreferredSize();
        assertTrue(ui.validCachedPreferredSize);
        assertEquals(ui.preferredSize, originalSize);
        assertEquals(new Dimension(ui.treeState.getPreferredWidth(null), ui.treeState
                .getPreferredHeight()), ui.preferredSize);
    }

    public void testPathWasExpanded() {
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(node1);
        assertFalse(ui.treeState.isExpanded(path1));
        ui.pathWasExpanded(path1);
        assertTrue(ui.treeState.isExpanded(path1));
        assertFalse(tree.isExpanded(path1));
    }

    public void testPathWasCollapsed() {
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(node1);
        tree.expandPath(path1);
        assertTrue(ui.treeState.isExpanded(path1));
        ui.pathWasCollapsed(path1);
        assertFalse(ui.treeState.isExpanded(path1));
    }

    public void testGetSetPreferredMinSize() {
        assertNull(ui.preferredMinSize);
        assertNull(ui.getPreferredMinSize());
        Dimension prefMinSize = new Dimension(10, 10);
        ui.setPreferredMinSize(prefMinSize);
        assertEquals(ui.preferredMinSize, ui.getPreferredMinSize());
        assertEquals(ui.preferredMinSize, prefMinSize);
    }

    public void testCompleteEditing() {
        final Marker stopMarker = new Marker();
        final Marker cancelMarker = new Marker();
        final Marker valueMarker = new Marker();
        ui = new BasicTreeUI() {
            @Override
            protected void completeEditing(final boolean messageStop,
                    final boolean messageCancel, final boolean messageTree) {
                super.completeEditing(messageStop, messageCancel, messageTree);
                stopMarker.setOccurred(messageStop);
                cancelMarker.setOccurred(messageCancel);
                valueMarker.setOccurred(messageTree);
            }
        };
        ui.installUI(tree);
        final Marker editorMarker = new Marker();
        TreeCellEditor editor = new DefaultTreeCellEditor(tree, new DefaultTreeCellRenderer()) {
            @Override
            public boolean stopCellEditing() {
                editorMarker.setOccurred();
                return super.stopCellEditing();
            }
        };
        tree.setCellEditor(editor);
        editorMarker.reset();
        stopMarker.reset();
        cancelMarker.reset();
        valueMarker.reset();
        ui.completeEditing();
        assertFalse(editorMarker.isOccurred());
        assertFalse(stopMarker.isOccurred());
        assertTrue(cancelMarker.isOccurred());
        assertFalse(valueMarker.isOccurred());
        editorMarker.reset();
        stopMarker.reset();
        cancelMarker.reset();
        valueMarker.reset();
        tree.setInvokesStopCellEditing(true);
        ui.completeEditing();
        assertFalse(editorMarker.isOccurred());
        if (isHarmony()) {
            assertTrue(stopMarker.isOccurred());
            assertFalse(cancelMarker.isOccurred());
        } else {
            assertFalse(stopMarker.isOccurred());
            assertTrue(cancelMarker.isOccurred());
        }
        assertFalse(valueMarker.isOccurred());
        tree.setEditable(true);
        ui.startEditingAtPath(tree, new TreePath(root));
        assertTrue(ui.isEditing(tree));
        ui.completeEditing(false, false, false);
        assertFalse(ui.isEditing(tree));
    }

    public void testCheckForClickInExpandControl() {
        TreePath path1 = new TreePath(root).pathByAddingChild(node1);
        assertFalse(tree.isExpanded(path1));
        assertTrue(ui.isLocationInExpandControl(path1, 8, 20));
        ui.checkForClickInExpandControl(path1, 8, 20);
        assertTrue(tree.isExpanded(path1));
        ui.checkForClickInExpandControl(path1, 8, 20);
        assertFalse(tree.isExpanded(path1));
    }

    public void testIsLocationInExpandControl() {
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(node1);
        assertFalse(ui.isLocationInExpandControl(rootPath, 0, 5));
        assertFalse(ui.isLocationInExpandControl(path1, 2, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 3, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 11, 20));
        assertFalse(ui.isLocationInExpandControl(path1, 12, 20));
        ui.setLeftChildIndent(10);
        ui.setRightChildIndent(20);
        assertEquals(30, ui.totalChildIndent);
        assertFalse(ui.isLocationInExpandControl(path1, 5, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 6, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 14, 20));
        assertFalse(ui.isLocationInExpandControl(path1, 15, 20));
        ui.setLeftChildIndent(11);
        assertEquals(31, ui.totalChildIndent);
        assertFalse(ui.isLocationInExpandControl(path1, 6, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 7, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 15, 20));
        assertFalse(ui.isLocationInExpandControl(path1, 16, 20));
        ui.setRightChildIndent(21);
        assertEquals(32, ui.totalChildIndent);
        assertFalse(ui.isLocationInExpandControl(path1, 6, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 7, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 15, 20));
        assertFalse(ui.isLocationInExpandControl(path1, 16, 20));
        ui.setLeftChildIndent(1);
        assertEquals(22, ui.totalChildIndent);
        assertTrue(ui.isLocationInExpandControl(path1, 0, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 5, 20));
        assertFalse(ui.isLocationInExpandControl(path1, 6, 20));
        ui.setLeftChildIndent(2);
        assertEquals(23, ui.totalChildIndent);
        assertTrue(ui.isLocationInExpandControl(path1, 0, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 6, 20));
        assertFalse(ui.isLocationInExpandControl(path1, 7, 20));
        ui.setLeftChildIndent(3);
        assertTrue(ui.isLocationInExpandControl(path1, 0, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 7, 20));
        assertFalse(ui.isLocationInExpandControl(path1, 8, 20));
        ui.setLeftChildIndent(4);
        assertTrue(ui.isLocationInExpandControl(path1, 0, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 8, 20));
        assertFalse(ui.isLocationInExpandControl(path1, 9, 20));
        ui.setLeftChildIndent(5);
        assertFalse(ui.isLocationInExpandControl(path1, 0, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 1, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 9, 20));
        assertFalse(ui.isLocationInExpandControl(path1, 10, 20));
        ui.expandedIcon = new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
            }

            public int getIconWidth() {
                return 14;
            }

            public int getIconHeight() {
                return 100;
            }
        };
        ui.setLeftChildIndent(30);
        ui.setRightChildIndent(100);
        assertFalse(ui.isLocationInExpandControl(path1, 22, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 23, 20));
        assertTrue(ui.isLocationInExpandControl(path1, 37, 20));
        assertFalse(ui.isLocationInExpandControl(path1, 38, 20));
    }

    public void testHandleExpandControlClick() {
        TreePath path1 = new TreePath(root).pathByAddingChild(node1);
        assertFalse(tree.isExpanded(path1));
        ui.handleExpandControlClick(path1, -10, -10);
        assertTrue(tree.isExpanded(path1));
        ui.handleExpandControlClick(path1, -10, -10);
        assertFalse(tree.isExpanded(path1));
    }

    public void testToggleExpandState() {
        TreePath path1 = new TreePath(root).pathByAddingChild(node1);
        assertFalse(tree.isExpanded(path1));
        ui.toggleExpandState(path1);
        assertTrue(tree.isExpanded(path1));
        ui.toggleExpandState(path1);
        assertFalse(tree.isExpanded(path1));
    }

    public void testIsToggleSelectionEvent() {
        assertTrue(ui.isToggleSelectionEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.CTRL_DOWN_MASK, 0, 0, 1, false, MouseEvent.BUTTON1)));
        assertTrue(ui.isToggleSelectionEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.CTRL_DOWN_MASK, 0, 0, 2, false, MouseEvent.BUTTON1)));
        assertFalse(ui.isToggleSelectionEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.CTRL_DOWN_MASK, 0, 0, 1, false, MouseEvent.BUTTON2)));
        assertFalse(ui.isToggleSelectionEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.CTRL_DOWN_MASK, 0, 0, 1, false, MouseEvent.BUTTON3)));
        assertFalse(ui.isToggleSelectionEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.SHIFT_DOWN_MASK, 0, 0, 1, false, MouseEvent.BUTTON1)));
    }

    public void testIsMultiSelectEvent() {
        assertTrue(ui.isMultiSelectEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.SHIFT_DOWN_MASK, 0, 0, 1, false, MouseEvent.BUTTON1)));
        assertTrue(ui.isMultiSelectEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.SHIFT_DOWN_MASK, 0, 0, 2, false, MouseEvent.BUTTON1)));
        assertFalse(ui.isMultiSelectEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.SHIFT_DOWN_MASK, 0, 0, 1, false, MouseEvent.BUTTON2)));
        assertFalse(ui.isMultiSelectEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.SHIFT_DOWN_MASK, 0, 0, 1, false, MouseEvent.BUTTON3)));
        assertFalse(ui.isMultiSelectEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.CTRL_DOWN_MASK, 0, 0, 1, false, MouseEvent.BUTTON1)));
    }

    public void testIsToggleEvent() {
        assertTrue(ui.isToggleEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0,
                2, false, MouseEvent.BUTTON1)));
        assertFalse(ui.isToggleEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0,
                3, false, MouseEvent.BUTTON1)));
        assertFalse(ui.isToggleEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0,
                1, false, MouseEvent.BUTTON1)));
        assertFalse(ui.isToggleEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0,
                2, false, MouseEvent.BUTTON2)));
        assertFalse(ui.isToggleEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0,
                2, false, MouseEvent.BUTTON3)));
        assertTrue(ui.isToggleEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED,
                InputEvent.CTRL_DOWN_MASK, 0, 0, 0, 2, false, MouseEvent.BUTTON1)));
        assertTrue(ui.isToggleEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED,
                InputEvent.SHIFT_DOWN_MASK, 0, 0, 0, 2, false, MouseEvent.BUTTON1)));
    }

    public void testSelectPathForEvent() {
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(node1);
        TreePath path11 = path1.pathByAddingChild(node11);
        assertTrue(tree.isSelectionEmpty());
        MouseEvent toggleSelectionEvent = new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.CTRL_DOWN_MASK, 0, 0, 1, false, MouseEvent.BUTTON1);
        ui.selectPathForEvent(rootPath, toggleSelectionEvent);
        assertEquals(new TreePath[] { rootPath }, tree.getSelectionPaths());
        ui.selectPathForEvent(rootPath, toggleSelectionEvent);
        assertTrue(tree.isSelectionEmpty());
        ui.selectPathForEvent(rootPath, toggleSelectionEvent);
        ui.selectPathForEvent(path1, toggleSelectionEvent);
        assertEquals(new TreePath[] { rootPath, path1 }, tree.getSelectionPaths());
        MouseEvent toggleEvent = new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, 2,
                false, MouseEvent.BUTTON1);
        ui.selectPathForEvent(rootPath, toggleEvent);
        assertFalse(tree.isExpanded(rootPath));
        assertEquals(new TreePath[] { rootPath }, tree.getSelectionPaths());
        ui.selectPathForEvent(path1, toggleEvent);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertEquals(new TreePath[] { path1 }, tree.getSelectionPaths());
        MouseEvent multiSelectionEvent = new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.SHIFT_DOWN_MASK, 0, 0, 1, false, MouseEvent.BUTTON1);
        ui.selectPathForEvent(rootPath, multiSelectionEvent);
        assertEquals(new TreePath[] { rootPath, path1 }, tree.getSelectionPaths());
        MouseEvent pressEvent = new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, 1,
                false, MouseEvent.BUTTON1);
        ui.selectPathForEvent(rootPath, pressEvent);
        assertEquals(new TreePath[] { rootPath }, tree.getSelectionPaths());
        ui.selectPathForEvent(path11, pressEvent);
        assertEquals(new TreePath[] { path11 }, tree.getSelectionPaths());
        MouseEvent wrongPressEvent = new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0,
                1, false, MouseEvent.BUTTON3);
        ui.selectPathForEvent(rootPath, wrongPressEvent);
        assertEquals(new TreePath[] { path11 }, tree.getSelectionPaths());
        MouseEvent notPressEvent = new MouseEvent(tree, MouseEvent.MOUSE_DRAGGED, 0, 0, 0, 0,
                1, false, MouseEvent.BUTTON1);
        ui.selectPathForEvent(rootPath, notPressEvent);
        assertEquals(new TreePath[] { path11 }, tree.getSelectionPaths());
    }

    public void testIsLeaf() {
        assertFalse(ui.isLeaf(0));
        assertFalse(ui.isLeaf(1));
        tree.expandRow(1);
        assertTrue(ui.isLeaf(2));
    }

    public void testKeyHandler() throws Exception {
        BasicTreeUI.KeyHandler handler = ui.new KeyHandler();
        assertFalse(handler.isKeyDown);
        assertNull(handler.repeatKeyAction);
        handler.keyPressed(new KeyEvent(tree, KeyEvent.KEY_PRESSED, 0, 0, 'a', 'a'));
        assertFalse(handler.isKeyDown);
        assertNull(handler.repeatKeyAction);
        handler.isKeyDown = true;
        handler.keyReleased(new KeyEvent(tree, KeyEvent.KEY_RELEASED, 0, 0, 'a', 'a'));
        assertTrue(handler.isKeyDown);
        assertNull(handler.repeatKeyAction);
        assertTrue(tree.isSelectionEmpty());
        handler.keyTyped(new KeyEvent(tree, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED,
                'a'));
        assertTrue(tree.isSelectionEmpty());
        handler.isKeyDown = false;
        JFrame f = new JFrame();
        f.getContentPane().add(tree);
        f.pack();
        f.setVisible(true);
        SwingWaitTestCase.isRealized(f);
        tree.requestFocus();
        assertTrue(waitForFocus(tree));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(node1);
        TreePath path2 = rootPath.pathByAddingChild(node2);
        handler.keyTyped(new KeyEvent(tree, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED,
                'n', KeyEvent.KEY_LOCATION_UNKNOWN));
        assertEquals(new TreePath[] { path1 }, tree.getSelectionPaths());
        handler.keyTyped(new KeyEvent(tree, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED,
                'o', KeyEvent.KEY_LOCATION_UNKNOWN));
        assertEquals(new TreePath[] { path1 }, tree.getSelectionPaths());
        handler.keyTyped(new KeyEvent(tree, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED,
                'd', KeyEvent.KEY_LOCATION_UNKNOWN));
        assertEquals(new TreePath[] { path1 }, tree.getSelectionPaths());
        handler.keyTyped(new KeyEvent(tree, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED,
                'e', KeyEvent.KEY_LOCATION_UNKNOWN));
        assertEquals(new TreePath[] { path1 }, tree.getSelectionPaths());
        handler.keyTyped(new KeyEvent(tree, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED,
                '2', KeyEvent.KEY_LOCATION_UNKNOWN));
        assertEquals(new TreePath[] { path2 }, tree.getSelectionPaths());
        handler.keyTyped(new KeyEvent(tree, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED,
                '1', KeyEvent.KEY_LOCATION_UNKNOWN));
        assertEquals(new TreePath[] { path2 }, tree.getSelectionPaths());
        f.dispose();
    }

    public void testComponentHandler() throws Exception {
        JScrollPane pane = new JScrollPane(tree);
        assertFalse(hasListener(tree.getComponentListeners(),
                BasicTreeUI.ComponentHandler.class));
        tree.setLargeModel(true);
        assertTrue(hasListener(tree.getComponentListeners(), BasicTreeUI.ComponentHandler.class));
        tree.setRowHeight(0);
        assertFalse(hasListener(tree.getComponentListeners(),
                BasicTreeUI.ComponentHandler.class));
        tree.setRowHeight(10);
        assertTrue(hasListener(tree.getComponentListeners(), BasicTreeUI.ComponentHandler.class));
        BasicTreeUI.ComponentHandler handler = (ComponentHandler) getListener(tree
                .getComponentListeners(), BasicTreeUI.ComponentHandler.class);
        assertNull(handler.timer);
        assertNull(handler.scrollBar);
        assertSame(pane, handler.getScrollPane());
        ui.getPreferredSize(tree);
        assertTrue(ui.validCachedPreferredSize);
        handler.getScrollPane().getVerticalScrollBar().setValueIsAdjusting(true);
        handler.componentMoved(new ComponentEvent(tree, ComponentEvent.COMPONENT_MOVED));
        assertNotNull(handler.timer);
        assertSame(handler.scrollBar, pane.getVerticalScrollBar());
        assertEquals(200, handler.timer.getDelay());
        assertSame(handler, handler.timer.getActionListeners()[0]);
        assertTrue(handler.timer.isRunning());
        ui.validCachedPreferredSize = true;
        assertTrue(ui.validCachedPreferredSize);
        handler.actionPerformed(null);
        assertTrue(handler.timer.isRunning());
        assertTrue(ui.validCachedPreferredSize);
        handler.getScrollPane().getVerticalScrollBar().setValueIsAdjusting(false);
        assertTrue(handler.timer.isRunning());
        Timer timer = handler.timer;
        handler.actionPerformed(null);
        if (isHarmony()) {
            assertNotNull(handler.timer);
        } else {
            assertNull(handler.timer);
        }
        assertFalse(timer.isRunning());
        assertFalse(ui.validCachedPreferredSize);
    }
}
