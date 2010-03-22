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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

public class DefaultTreeCellEditor implements ActionListener, TreeCellEditor, TreeSelectionListener {
    public class DefaultTextField extends JTextField {
        protected Border border;

        public DefaultTextField(final Border border) {
            setBorder(border);
        }

        public void setBorder(final Border border) {
            this.border = border;
            super.setBorder(border);
        }

        public Border getBorder() {
            return border;
        }

        public Font getFont() {
            return super.getFont();
        }

        public Dimension getPreferredSize() {
            Dimension result = super.getPreferredSize();
            if (renderer != null) {
                result.height = renderer.getPreferredSize().height;
            }

            return result;
        }
    }

    public class EditorContainer extends Container {
        public EditorContainer() {
        }

        //This method looks like a misprint or bug in specification, however we have
        // to put it here to be API-compatible.
        public void EditorContainer() {
        }

        public void paint(final Graphics g) {
            if (icon != null) {
                Rectangle viewR = new Rectangle(0, 0, getWidth(), getHeight());
                Rectangle iconR = new Rectangle();
                Rectangle textR = new Rectangle();

                SwingUtilities.layoutCompoundLabel(renderer,
                                                   renderer.getFontMetrics(renderer.getFont()),
                                                   renderer.getText(),
                                                   icon,
                                                   renderer.getVerticalAlignment(),
                                                   renderer.getHorizontalAlignment(),
                                                   renderer.getVerticalTextPosition(),
                                                   renderer.getHorizontalTextPosition(),
                                                   viewR,
                                                   iconR,
                                                   textR, renderer.getIconTextGap());

                if (!tree.getComponentOrientation().isLeftToRight()) {
                    iconR.x = getWidth() - offset + (offset - icon.getIconWidth()) / 2;
                }

                icon.paintIcon(this, g, iconR.x, iconR.y);
            }
            super.paint(g);
        }

        public void doLayout() {
            if (tree.getComponentOrientation().isLeftToRight()) {
                editingComponent.setBounds(offset, 0, this.getWidth() - offset, this.getHeight());
            } else {
                editingComponent.setBounds(0, 0, this.getWidth() - offset, this.getHeight());
            }
        }

        public void requestFocus() {
            if (editingComponent != null) {
                editingComponent.requestFocus();
            } else {
                super.requestFocus();
            }
        }

        public Dimension getPreferredSize() {
            if (editingComponent != null) {
                Dimension editorPrefSize = editingComponent.getPreferredSize();
                editorPrefSize.width = Math.max(editorPrefSize.width + offset + 5, 100);
                if (renderer != null) {
                    editorPrefSize.height = Math.max(editorPrefSize.height, renderer.getPreferredSize().height);
                }
                return editorPrefSize;
            }
            return new Dimension();
        }
    }


    protected TreeCellEditor realEditor;
    protected DefaultTreeCellRenderer renderer;
    protected Container editingContainer;
    protected transient Component editingComponent;
    protected boolean canEdit;
    protected transient int offset;
    protected transient JTree tree;
    protected transient TreePath lastPath;
    protected transient Timer timer;
    protected transient int lastRow;
    protected Color borderSelectionColor;
    protected transient Icon editingIcon;
    protected Font font;

    private Icon icon;

    private static final int EDITING_DELAY = 1200;

    public DefaultTreeCellEditor(final JTree tree, final DefaultTreeCellRenderer renderer) {
        this(tree, renderer, null);
    }

    public DefaultTreeCellEditor(final JTree tree, final DefaultTreeCellRenderer renderer,
                                 final TreeCellEditor editor) {
        setTree(tree);
        this.renderer = renderer;
        this.realEditor = editor != null ? editor : createTreeCellEditor();

        editingContainer = createContainer();
    }

    public void setBorderSelectionColor(final Color color) {
        borderSelectionColor = color;
    }

    public Color getBorderSelectionColor() {
        return borderSelectionColor;
    }

    public void setFont(final Font font) {
        this.font = font;
    }

    public Font getFont() {
        return font;
    }

    public Component getTreeCellEditorComponent(final JTree tree,
                                                final Object value,
                                                final boolean isSelected,
                                                final boolean expanded,
                                                final boolean leaf,
                                                final int row) {

        editingComponent = realEditor.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);
        Font font = getFont();
        if (font == null && renderer != null) {
            font = renderer.getFont();
        }
        if (font == null) {
            font = tree.getFont();
        }
        editingContainer.setFont(font);
        editingComponent.setFont(font);

        determineOffset(tree, value, isSelected, expanded, leaf, row);
        prepareForEditing();

        return editingContainer;
    }

    public Object getCellEditorValue() {
        return realEditor.getCellEditorValue();
    }

    public boolean isCellEditable(final EventObject event) {
        if (realEditor.isCellEditable(event) && canEditImmediately(event)) {
            prepareForEditing();
            return true;
        }

        if (event instanceof MouseEvent) {
            MouseEvent me = (MouseEvent)event;
            TreePath clickPath = tree.getPathForLocation(me.getX(), me.getY());

            if (lastPath != null && lastPath.equals(clickPath) && shouldStartEditingTimer(event)) {
                startEditingTimer();
            } else if (timer != null) {
                timer.stop();
            }
        }

        return false;
    }

    public boolean shouldSelectCell(final EventObject event) {
        return realEditor.shouldSelectCell(event);
    }

    public boolean stopCellEditing() {
        if (realEditor.stopCellEditing()) {
            removeEditor();
            return true;
        }

        return false;
    }

    public void cancelCellEditing() {
        realEditor.cancelCellEditing();
        removeEditor();
    }

    public void addCellEditorListener(final CellEditorListener l) {
        realEditor.addCellEditorListener(l);
    }

    public void removeCellEditorListener(final CellEditorListener l) {
        realEditor.removeCellEditorListener(l);
    }

    public CellEditorListener[] getCellEditorListeners() {
        return ((DefaultCellEditor)realEditor).getCellEditorListeners();
    }

    public void valueChanged(final TreeSelectionEvent e) {
        lastPath = e.getNewLeadSelectionPath();
    }

    public void actionPerformed(final ActionEvent e) {
        lastRow = tree.getRowForPath(lastPath);
        tree.startEditingAtPath(lastPath);
    }

    protected void setTree(final JTree tree) {
        this.tree = tree;
        tree.addTreeSelectionListener(this);
    }

    protected boolean shouldStartEditingTimer(final EventObject event) {
        if (!(event instanceof MouseEvent)) {
            return false;
        }
        MouseEvent me = (MouseEvent)event;
        if (me.getClickCount() != 1 || !SwingUtilities.isLeftMouseButton(me)) {
            return false;
        }
        return inHitRegion(me.getX(), me.getY());
    }

    protected void startEditingTimer() {
        if (timer == null) {
            timer = new Timer(EDITING_DELAY, this);
            timer.setRepeats(false);
            timer.start();
        } else {
            timer.restart();
        }
    }

    protected boolean canEditImmediately(final EventObject event) {
        if (event == null) {
            return true;
        }

        if (event instanceof MouseEvent) {
            MouseEvent me = (MouseEvent)event;
            return SwingUtilities.isLeftMouseButton(me)
                   && me.getClickCount() > 2
                   && realEditor.isCellEditable(event)
                   && inHitRegion(me.getX(), me.getY());
        }

        return false;
    }

    protected boolean inHitRegion(final int x, final int y) {
        TreePath hitPath = tree.getPathForLocation(x, y);
        if (hitPath == null) {
            return false;
        }
        Rectangle pathBounds = tree.getPathBounds(hitPath);

        return x > pathBounds.x + offset && y >= 0;
    }

    protected void determineOffset(final JTree tree,
                                   final Object value,
                                   final boolean isSelected,
                                   final boolean expanded,
                                   final boolean leaf,
                                   final int row) {

        if (renderer != null) {
            renderer.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, false);
            icon = renderer.getIcon();
            offset = icon != null ? icon.getIconWidth() + renderer.getIconTextGap() : 0;
        } else {
            offset = 0;
        }
    }

    protected void prepareForEditing() {
        if (editingComponent != null) {
            editingContainer.add(editingComponent);
        }
    }

    protected Container createContainer() {
        return new EditorContainer();
    }

    protected TreeCellEditor createTreeCellEditor() {
        DefaultCellEditor result = new DefaultCellEditor(new DefaultTextField(UIManager.getBorder("Tree.editorBorder")));
        result.setClickCountToStart(1);

        return result;
    }


    private void removeEditor() {
        editingContainer.remove(editingComponent);
        editingComponent = null;
    }
}
