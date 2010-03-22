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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.TreePath;

import org.apache.harmony.x.swing.TreeCommons;


public class MetalTreeUI extends BasicTreeUI {
    private static final String LINE_STYLE_PROPERTY = "JTree.lineStyle";
    private static final int ANGLED = 1;
    private static final int HORIZONTAL = 2;
    private static final int NONE = 3;
    private static int lineStyle = ANGLED;

    private PropertyChangeListener clientPropertyListener;
    private Color lineColor;

    private TreeCommons.PaintTreeContext paintContext = new TreeCommons.PaintTreeContext() {
        public JTree getTree() {
            return tree;
        }

        public Hashtable getDrawingCache() {
            return drawingCache;
        }

        public AbstractLayoutCache getLayoutCache() {
            return treeState;
        }

        public boolean paintHandles() {
            return lineStyle == ANGLED;
        }

        public boolean paintHorizontalSeparators() {
            return lineStyle == HORIZONTAL;
        }

        public boolean isEditing(final TreePath path) {
            return MetalTreeUI.this.isEditing(tree) && editingPath.equals(path);
        }

        public void paintVerticalPartOfLeg(final Graphics g, final Rectangle clipBounds, final Insets insets, final TreePath path) {
            MetalTreeUI.this.paintVerticalPartOfLeg(g, clipBounds, insets, path);
        }

        public void paintHorizontalPartOfLeg(final Graphics g, final Rectangle clipBounds, final Insets insets, final Rectangle bounds, final TreePath path, final int row, final boolean isExpanded, final boolean hasBeenExpanded, final boolean isLeaf) {
            MetalTreeUI.this.paintHorizontalPartOfLeg(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
        }

        public void paintExpandControl(final Graphics g, final Rectangle clipBounds, final Insets insets, final Rectangle bounds, final TreePath path, final int row, final boolean isExpanded, final boolean hasBeenExpanded, final boolean isLeaf) {
            MetalTreeUI.this.paintExpandControl(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
        }

        public void paintRow(final Graphics g, final Rectangle clipBounds, final Insets insets, final Rectangle bounds, final TreePath path, final int row, final boolean isExpanded, final boolean hasBeenExpanded, final boolean isLeaf) {
            MetalTreeUI.this.paintRow(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
        }

        public void paintHorizontalSeparators(final Graphics g, final JComponent tree) {
            MetalTreeUI.this.paintHorizontalSeparators(g, tree);
        }
    };


    public static ComponentUI createUI(final JComponent c) {
        return new MetalTreeUI();
    }

    public void installUI(final JComponent c) {
        super.installUI(c);

        lineColor = UIManager.getColor("Tree.line");

        clientPropertyListener = createClientPropertyChangeHandler();
        tree.addPropertyChangeListener(clientPropertyListener);
    }

    public void uninstallUI(final JComponent c) {
        super.uninstallUI(c);
        tree.removePropertyChangeListener(clientPropertyListener);
        clientPropertyListener = null;
    }

    protected int getHorizontalLegBuffer() {
        return 4;
    }

    protected void decodeLineStyle(final Object lineStyleFlag) {
        if (lineStyleFlag == null) {
            return;
        }

        String lineStyleString = lineStyleFlag.toString();
        if ("Angled".equals(lineStyleString)) {
            lineStyle = ANGLED;
        }
        if ("Horizontal".equals(lineStyleString)) {
            lineStyle = HORIZONTAL;
        }
        if ("None".equals(lineStyleString)) {
            lineStyle = NONE;
        }
    }

    protected boolean isLocationInExpandControl(final int row, final int rowLevel,
                                                final int mouseX, final int mouseY) {
        int coordExp = 3;
        TreePath path = tree.getPathForRow(rowLevel);

        if (treeModel == null || path == null || treeModel.isLeaf(path.getLastPathComponent())) {
            return false;
        }

        Rectangle pathBounds = getPathBounds(tree, path);

        int expandIconWidth = expandedIcon != null ? expandedIcon.getIconWidth() : 8;
        int startExpandZone = pathBounds.x - rightChildIndent - expandIconWidth / 2 - coordExp;
        int endExpandZone = startExpandZone + expandIconWidth + coordExp * 2;

        return mouseX >= startExpandZone && mouseX <= endExpandZone;
    }

    public void paint(final Graphics g, final JComponent c) {
        TreeCommons.paintTree(g, paintContext);
    }

    protected void paintHorizontalSeparators(final Graphics g, final JComponent c) {
        g.setColor(lineColor);

        Rectangle bounds = tree.getBounds();
        Insets insets = tree.getInsets();

        Object root = getModel().getRoot();
        int rootChildCount = getModel().getChildCount(root);
        if (rootChildCount == 0) {
            return;
        }

        TreePath rootPath = new TreePath(root);
        for (int i = 0; i < rootChildCount; i++) {
            TreePath path = rootPath.pathByAddingChild(getModel().getChild(root, i));
            Rectangle pathBounds = tree.getPathBounds(path);
            paintHorizontalLine(g, c, pathBounds.y, insets.left,
                                bounds.width - insets.right - 1);
        }
    }

    private PropertyChangeListener createClientPropertyChangeHandler() {
        return new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent e) {
                if (LINE_STYLE_PROPERTY.equals(e.getPropertyName())) {
                    decodeLineStyle(e.getNewValue());
                }
            }
        };
    }
}
