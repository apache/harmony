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
 * @author Anton Avtamonov
 */
package javax.swing.tree;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import javax.swing.BasicSwingTestCase;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.plaf.ColorUIResource;
import javax.swing.tree.DefaultTreeCellEditor.DefaultTextField;

public class DefaultTreeCellEditorTest extends BasicSwingTestCase {
    private DefaultTreeCellEditor editor;

    private DefaultTreeCellRenderer renderer;

    private JTree tree;

    public DefaultTreeCellEditorTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        tree = new JTree();
        renderer = new DefaultTreeCellRenderer();
        editor = new DefaultTreeCellEditor(tree, renderer);
    }

    @Override
    protected void tearDown() throws Exception {
        editor = null;
        tree = null;
        renderer = null;
    }

    public void testDefaultTextField() throws Exception {
        editor.getTreeCellEditorComponent(tree, "any", false, true, false, 0);
        assertTrue(editor.editingComponent instanceof DefaultTreeCellEditor.DefaultTextField);
        DefaultTreeCellEditor.DefaultTextField defaultTextField = (DefaultTreeCellEditor.DefaultTextField) editor.editingComponent;
        assertEquals(UIManager.getBorder("Tree.editorBorder"), defaultTextField.border);
        Border b = BorderFactory.createBevelBorder(0);
        defaultTextField = editor.new DefaultTextField(b);
        assertSame(b, defaultTextField.border);
    }

    public void testDefaultTextField_getSetBorder() throws Exception {
        Border b = BorderFactory.createBevelBorder(0);
        DefaultTreeCellEditor.DefaultTextField defaultTextField = editor.new DefaultTextField(b);
        assertSame(b, defaultTextField.getBorder());
        propertyChangeController = new PropertyChangeController();
        defaultTextField.addPropertyChangeListener(propertyChangeController);
        b = BorderFactory.createEmptyBorder();
        defaultTextField.setBorder(b);
        assertSame(b, defaultTextField.getBorder());
        assertTrue(propertyChangeController.isChanged("border"));
    }

    public void testDefaultTextField_getFont() throws Exception {
        DefaultTreeCellEditor.DefaultTextField defaultTextField = editor.new DefaultTextField(
                BorderFactory.createBevelBorder(0));
        assertEquals(UIManager.getFont("TextField.font"), defaultTextField.getFont());
        Font f = new Font("font", 0, 30);
        defaultTextField.setFont(f);
        assertEquals(f, defaultTextField.getFont());
        defaultTextField.setFont(null);
        assertNull(defaultTextField.getFont());
        editor.setFont(f);
        assertNull(defaultTextField.getFont());
        editor.renderer.setFont(f);
        assertNull(defaultTextField.getFont());
        editor.tree.setFont(f);
        assertNull(defaultTextField.getFont());
    }

    public void testDefaultTextField_getPreferredSize() throws Exception {
        DefaultTreeCellEditor.DefaultTextField defaultTextField = editor.new DefaultTextField(
                BorderFactory.createBevelBorder(0));
        defaultTextField.setText("value");
        assertEquals(new Dimension(new JTextField("value").getPreferredSize().width, 0),
                defaultTextField.getPreferredSize());
        editor.renderer.setPreferredSize(new Dimension(100, 200));
        assertEquals(new Dimension(new JTextField("value").getPreferredSize().width, 200),
                defaultTextField.getPreferredSize());
    }

    public void testEditorContainer() throws Exception {
        DefaultTreeCellEditor.EditorContainer container = editor.new EditorContainer();
        assertEquals(0, container.getComponentCount());
        assertEquals(new Rectangle(), container.getBounds());
    }

    public void testEditorContainer_getPreferredSize() throws Exception {
        DefaultTreeCellEditor.EditorContainer container = editor.new EditorContainer();
        assertEquals(new Dimension(), container.getPreferredSize());
        editor.editingComponent = new JTextField("a");
        assertEquals(new Dimension(100, editor.editingComponent.getPreferredSize().height),
                container.getPreferredSize());
        editor.offset = 1000;
        assertEquals(new Dimension(1000 + editor.editingComponent.getPreferredSize().width + 5,
                editor.editingComponent.getPreferredSize().height), container
                .getPreferredSize());
        editor.renderer.setPreferredSize(new Dimension(2000, 10));
        assertEquals(new Dimension(1000 + editor.editingComponent.getPreferredSize().width + 5,
                editor.editingComponent.getPreferredSize().height), container
                .getPreferredSize());
        editor.renderer.setPreferredSize(new Dimension(2000, 30));
        assertEquals(new Dimension(1000 + editor.editingComponent.getPreferredSize().width + 5,
                30), container.getPreferredSize());
    }

    public void testEditorContainer_doLayout() throws Exception {
        DefaultTreeCellEditor.EditorContainer container = editor.new EditorContainer();
        editor.editingComponent = new JTextField("any");
        assertEquals(new Rectangle(0, 0, 0, 0), editor.editingComponent.getBounds());
        container.doLayout();
        assertEquals(new Rectangle(0, 0, 0, 0), editor.editingComponent.getBounds());
        editor.offset = 10;
        container.doLayout();
        assertEquals(new Rectangle(10, 0, -10, 0), editor.editingComponent.getBounds());
        container.setBounds(0, 0, 100, 100);
        container.doLayout();
        assertEquals(new Rectangle(10, 0, 90, 100), editor.editingComponent.getBounds());
    }

    public void testDefaultTreeCellEditor() throws Exception {
        assertNull(editor.borderSelectionColor);
        assertFalse(editor.canEdit);
        assertNull(editor.editingComponent);
        assertTrue(editor.editingContainer instanceof DefaultTreeCellEditor.EditorContainer);
        assertNull(editor.editingIcon);
        assertNull(editor.font);
        assertNull(editor.lastPath);
        assertEquals(0, editor.lastRow);
        assertEquals(0, editor.offset);
        assertTrue(editor.realEditor instanceof DefaultCellEditor);
        assertSame(renderer, editor.renderer);
        assertNull(editor.timer);
        assertSame(tree, editor.tree);
        assertEquals(1, ((DefaultCellEditor) editor.realEditor).getClickCountToStart());
        DefaultCellEditor realEditor = new DefaultCellEditor(new JTextField());
        realEditor.setClickCountToStart(4);
        editor = new DefaultTreeCellEditor(tree, renderer, realEditor);
        assertSame(realEditor, editor.realEditor);
        assertEquals(4, ((DefaultCellEditor) editor.realEditor).getClickCountToStart());
    }

    public void testGetSetBorderSelectionColor() throws Exception {
        assertNull(editor.getBorderSelectionColor());
        Color c = Color.RED;
        editor.setBorderSelectionColor(c);
        assertSame(c, editor.getBorderSelectionColor());
        c = new ColorUIResource(Color.BLUE);
        editor.setBorderSelectionColor(c);
        assertSame(c, editor.getBorderSelectionColor());
    }

    public void testGetSetFont() throws Exception {
        assertNull(editor.getFont());
        Font font = new Font("font", 0, 20);
        renderer.setFont(font);
        assertNull(editor.getFont());
        assertSame(font, editor.getTreeCellEditorComponent(tree, "value", false, false, false,
                0).getFont());
        font = new Font("font", 0, 50);
        editor.setFont(font);
        assertSame(font, editor.getFont());
        assertSame(font, editor.getTreeCellEditorComponent(tree, "value", false, false, false,
                0).getFont());
    }

    public void testGetTreeCellEditorComponent() throws Exception {
        Font font = new Font("font", 0, 30);
        tree.setFont(font);
        assertNull(editor.editingComponent);
        assertSame(editor.editingContainer, editor.getTreeCellEditorComponent(tree, "value",
                false, false, false, 0));
        assertEquals(1, editor.editingContainer.getComponentCount());
        assertSame(font, editor.editingContainer.getFont());
        assertSame(font, editor.editingComponent.getFont());
        assertSame(editor.realEditor.getTreeCellEditorComponent(tree, "value", false, false,
                false, 0), editor.editingContainer.getComponent(0));
        assertSame(editor.editingContainer.getComponent(0), editor.editingComponent);
        assertTrue(editor.editingComponent instanceof DefaultTreeCellEditor.DefaultTextField);
        DefaultTreeCellEditor.DefaultTextField textField = (DefaultTextField) editor.editingComponent;
        assertEquals("value", textField.getText());
        assertEquals(tree.getFont(), textField.getFont());
    }

    public void testGetCellEditorValue() throws Exception {
        assertEquals("", editor.getCellEditorValue());
        editor.editingComponent = new JTextField("any");
        assertEquals("", editor.getCellEditorValue());
        editor.realEditor.getTreeCellEditorComponent(tree, "value", false, false, false, 0);
        assertEquals("value", editor.getCellEditorValue());
        assertEquals("any", ((JTextField) editor.editingComponent).getText());
        editor.getTreeCellEditorComponent(tree, "value2", false, false, false, 0);
        assertEquals("value2", editor.getCellEditorValue());
        assertEquals("value2", ((JTextField) editor.editingComponent).getText());
    }

    public void testIsCellEditable() throws Exception {
        final Marker m = new Marker();
        editor = new DefaultTreeCellEditor(tree, renderer) {
            @Override
            protected void prepareForEditing() {
                m.setOccurred();
            }
        };
        assertTrue(editor.isCellEditable(null));
        assertTrue(m.isOccurred());
    }

    public void testShouldSelectCell() throws Exception {
        final Marker m = new Marker();
        editor = new DefaultTreeCellEditor(tree, renderer) {
            @Override
            public boolean shouldSelectCell(final EventObject e) {
                m.setOccurred();
                m.setAuxiliary(e);
                return true;
            }
        };
        EventObject eo = new EventObject(this);
        assertTrue(editor.shouldSelectCell(eo));
        assertTrue(m.isOccurred());
        assertSame(eo, m.getAuxiliary());
    }

    public void testStopCellEditing() throws Exception {
        editor.getTreeCellEditorComponent(tree, "value", false, false, false, 0);
        assertNotNull(editor.editingComponent);
        assertEquals(1, editor.editingContainer.getComponentCount());
        assertTrue(editor.stopCellEditing());
        assertNull(editor.editingComponent);
        assertEquals(0, editor.editingContainer.getComponentCount());
    }

    public void testCancelCellEditing() throws Exception {
        editor.getTreeCellEditorComponent(tree, "value", false, false, false, 0);
        assertNotNull(editor.editingComponent);
        assertEquals(1, editor.editingContainer.getComponentCount());
        editor.cancelCellEditing();
        assertNull(editor.editingComponent);
        assertEquals(0, editor.editingContainer.getComponentCount());
    }

    public void testAddRemoveGetCellEditorListener() throws Exception {
        assertEquals(0, editor.getCellEditorListeners().length);
        assertEquals(0, ((DefaultCellEditor) editor.realEditor).getCellEditorListeners().length);
        CellEditorListener l = new CellEditorListener() {
            public void editingStopped(ChangeEvent e) {
            }

            public void editingCanceled(ChangeEvent e) {
            }
        };
        editor.addCellEditorListener(l);
        assertEquals(1, editor.getCellEditorListeners().length);
        assertEquals(1, ((DefaultCellEditor) editor.realEditor).getCellEditorListeners().length);
        editor.removeCellEditorListener(l);
        assertEquals(0, ((DefaultCellEditor) editor.realEditor).getCellEditorListeners().length);
    }

    public void testValueChanged() throws Exception {
        TreePath treePath = new TreePath("root");
        editor.valueChanged(new TreeSelectionEvent(tree, treePath, true, null, treePath));
        if (isHarmony()) {
            assertEquals(treePath, editor.lastPath);
        } else {
            assertNull(editor.lastPath);
        }
    }

    public void testActionPerformed() throws Exception {
        editor.lastPath = new TreePath(tree.getModel().getRoot());
        editor.lastRow = 10;
        editor.actionPerformed(null);
        if (isHarmony()) {
            assertEquals(0, editor.lastRow);
        } else {
            assertEquals(10, editor.lastRow);
        }
    }

    public void testSetTree() throws Exception {
        JTree anotherTree = new JTree();
        assertEquals(0, anotherTree.getTreeSelectionListeners().length);
        editor.setTree(anotherTree);
        assertEquals(1, anotherTree.getTreeSelectionListeners().length);
        assertSame(anotherTree, editor.tree);
    }

    public void testShouldStartEditingTimer() throws Exception {
        assertFalse(editor.shouldStartEditingTimer(null));
        assertTrue(editor.shouldStartEditingTimer(new MouseEvent(tree,
                MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK, 1, 1, 1, false,
                MouseEvent.BUTTON1)));
        assertFalse(editor.shouldStartEditingTimer(new MouseEvent(tree,
                MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK, 1, 1, 2, false,
                MouseEvent.BUTTON1)));
    }

    public void testStartEditingTimer() throws Exception {
        assertNull(editor.timer);
        editor.startEditingTimer();
        assertTrue(editor.timer.isRunning());
        assertEquals(1200, editor.timer.getDelay());
    }

    @SuppressWarnings("deprecation")
    public void testCanEditimmediately() throws Exception {
        assertTrue(editor.canEditImmediately(null));
        assertTrue(editor.canEditImmediately(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                0, 1, 1, 3, false, MouseEvent.BUTTON1)));
        assertTrue(editor.canEditImmediately(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                0, 1, 1, 4, false, MouseEvent.BUTTON1)));
        assertFalse(editor.canEditImmediately(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                0, 1, 1, 3, false, MouseEvent.BUTTON2)));
        assertFalse(editor.canEditImmediately(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0,
                0, 1, 1, 2, false, MouseEvent.BUTTON1)));
        assertFalse(editor.canEditImmediately(new KeyEvent(tree, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_1)));
    }

    public void testInHitRegion() throws Exception {
        assertFalse(editor.inHitRegion(0, 0));
        assertTrue(editor.inHitRegion(1, 0));
        if (isHarmony()) {
            assertFalse(editor.inHitRegion(1000, 1000));
        } else {
            assertTrue(editor.inHitRegion(1000, 1000));
        }
        editor.offset = 30;
        assertFalse(editor.inHitRegion(30, 1));
        assertTrue(editor.inHitRegion(31, 1));
    }

    public void testDetermineOffset() throws Exception {
        assertEquals(0, editor.offset);
        editor.determineOffset(tree, "value", false, false, false, 0);
        assertEquals(20, editor.offset);
        editor.offset = 10;
        editor.determineOffset(tree, "value", false, false, false, 0);
        assertEquals(20, editor.offset);
    }

    public void testPrepareForEditing() throws Exception {
        assertEquals(0, editor.editingContainer.getComponentCount());
        editor.prepareForEditing();
        assertEquals(0, editor.editingContainer.getComponentCount());
        editor.editingComponent = new JTextField("any");
        editor.prepareForEditing();
        assertEquals(1, editor.editingContainer.getComponentCount());
    }

    public void testCreateContainer() throws Exception {
        assertNotSame(editor.createContainer(), editor.createContainer());
        assertTrue(editor.createContainer() instanceof DefaultTreeCellEditor.EditorContainer);
    }

    public void testCreateTreeCellEditor() throws Exception {
        assertNotSame(editor.createTreeCellEditor(), editor.createTreeCellEditor());
        assertTrue(editor.createTreeCellEditor() instanceof DefaultCellEditor);
        assertTrue(editor.createTreeCellEditor().getTreeCellEditorComponent(tree, "any", false,
                true, false, 0) instanceof DefaultTreeCellEditor.DefaultTextField);
        assertEquals(1, ((DefaultCellEditor) editor.createTreeCellEditor())
                .getClickCountToStart());
    }
}
