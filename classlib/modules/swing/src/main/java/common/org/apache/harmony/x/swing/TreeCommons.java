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
 * @author Alexander T. Simbirtsev, Anton Avtamonov
 */
package org.apache.harmony.x.swing;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * Storage of the utility methods for tree-related calculations.
 *
 */
public class TreeCommons {
    /**
     * Painting context. Used by
     * {@link TreeCommons#paintTree(Graphics, TreeCommons.PaintTreeContext)}
     * method to parameterize painting differences of Basic and Metal L&Fs.
     */
    public interface PaintTreeContext {
        /**
         * @return Painting tree
         */
        JTree getTree();

        /**
         * @return Drawing cache used for painting optimization
         */
        Hashtable getDrawingCache();

        /**
         * @return Layout cache used by the tree UI
         */
        AbstractLayoutCache getLayoutCache();

        /**
         * @return <code>true</code> if root handles should be drawn; <code>false</code> otherwise
         */
        boolean paintHandles();

        /**
         * @return <code>true</code> if horizontal children separators should be drawn; <code>false</code> otherwise
         */
        boolean paintHorizontalSeparators();

        /**
         * Determines if the specified path is being edited.
         *
         * @param path TreePath to be checked against being edited or not
         *
         * @return <code>true</code> if the specified path is edited; <code>false</code> otherwise
         */
        boolean isEditing(TreePath path);

        /**
         * Draws vertical tree leg.
         *
         * @param g Graphics to paint on
         * @param clipBounds Rectangle representing current Graphics's clip
         * @param insets Insets of the tree (tree's border)
         * @param path TreePath for which vertical leg should be drawn
         */
        void paintVerticalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets, TreePath path);

        /**
         * Draws horizontal tree leg.
         *
         * @param g Graphics to paint on
         * @param clipBounds Rectangle representing current Graphics's clip
         * @param insets Insets of the tree (tree's border)
         * @param path TreePath for which vertical leg should be drawn
         * @param row int value representing painting row
         * @param isExpanded boolean value representing of current row is expanded
         * @param hasBeenExpanded boolean value representing of current row has been ever expanded
         * @param isLeaf boolean value representing of current row is leaf
         */
        void paintHorizontalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets, Rectangle bounds, TreePath path, int row,
                                      boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf);

        /**
         * Draws expand control (control which being clicked expand/collapse row).
         *
         * @param g Graphics to paint on
         * @param clipBounds Rectangle representing current Graphics's clip
         * @param insets Insets of the tree (tree's border)
         * @param path TreePath for which vertical leg should be drawn
         * @param row int value representing painting row
         * @param isExpanded boolean value representing of current row is expanded
         * @param hasBeenExpanded boolean value representing of current row has been ever expanded
         * @param isLeaf boolean value representing of current row is leaf
         */
        void paintExpandControl(Graphics g, Rectangle clipBounds, Insets insets, Rectangle bounds, TreePath path, int row,
                                boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf);

        /**
         * Draws the row.
         *
         * @param g Graphics to paint on
         * @param clipBounds Rectangle representing current Graphics's clip
         * @param insets Insets of the tree (tree's border)
         * @param path TreePath for which vertical leg should be drawn
         * @param row int value representing painting row
         * @param isExpanded boolean value representing of current row is expanded
         * @param hasBeenExpanded boolean value representing of current row has been ever expanded
         * @param isLeaf boolean value representing of current row is leaf
         */
        void paintRow(Graphics g, Rectangle clipBounds, Insets insets, Rectangle bounds, TreePath path, int row,
                      boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf);

        /**
         * Draws horizontal separators. Is called if paintHorizontalSeparators() returns <code>true</code>.
         *
         * @param g Graphics to paint on
         * @param tree JTree to be painted
         */
        void paintHorizontalSeparators(Graphics g, JComponent tree);
    }

    /**
     * Returns tree path from the specified ancestor to a node.
     *
     * @param node TreeNode which is the path end
     * @param ancestor TreeNode which is the path top
     *
     * @return path from an ancestor to a node
     */
    public static TreeNode[] getPathToAncestor(final TreeNode node,
                                               final TreeNode ancestor) {
        return getPathToAncestor(node, ancestor, 0);
    }

    /**
     * Returns tree path from the specified ancestor to a node limited by the depth.
     *
     * @param node TreeNode which is the path end
     * @param ancestor TreeNode which is the path top
     * @param depth int value representing the maximum path length
     *
     * @return path from an ancestor to a node
     */
    public static TreeNode[] getPathToAncestor(final TreeNode node,
                                           final TreeNode ancestor,
                                           final int depth) {
        if (node == null) {
            return new TreeNode[depth];
        }

        if (node == ancestor) {
            TreeNode[] result = new TreeNode[depth + 1];
            result[0] = ancestor;
            return result;
        }

        TreeNode[] result = getPathToAncestor(node.getParent(), ancestor, depth + 1);
        result[result.length - depth - 1] = node;
        return result;
    }

    /**
     * Paints a tree basing on the data from the parameterized context.
     *
     * @param g Graphics to paint on
     * @param context PaintTreeContext specified by particular UI.
     * @see TreeCommons.PaintTreeContext
     */
    public static void paintTree(final Graphics g, final PaintTreeContext context) {
        JTree tree = context.getTree();
        Insets insets = tree.getInsets();

        Rectangle clipBounds = g.getClipBounds();
        if (clipBounds == null) {
            clipBounds = SwingUtilities.getLocalBounds(tree);
        }
        TreePath startPath = tree.getClosestPathForLocation(clipBounds.x, clipBounds.y);
        if (startPath == null) {
            return;
        }
        TreePath endPath = tree.getClosestPathForLocation(clipBounds.x + clipBounds.width,
                                                                       clipBounds.y + clipBounds.height);

        if (context.getDrawingCache().isEmpty()) {
            Enumeration expanded = tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
            while (expanded.hasMoreElements()) {
                TreePath path = (TreePath)expanded.nextElement();
                context.paintVerticalPartOfLeg(g, clipBounds, insets, path);
                context.getDrawingCache().put(path, Boolean.TRUE);
            }
        } else {
            Enumeration keys = context.getDrawingCache().keys();
            while (keys.hasMoreElements()) {
                TreePath path = (TreePath)keys.nextElement();
                context.paintVerticalPartOfLeg(g, clipBounds, insets, path);
            }
        }

        Enumeration paths = context.getLayoutCache().getVisiblePathsFrom(startPath);
        while (paths.hasMoreElements()) {
            TreePath path = (TreePath)paths.nextElement();

            int rowForPath = tree.getRowForPath(path);
            Rectangle bounds = tree.getPathBounds(path);
            boolean isExpanded = tree.isExpanded(path);
            boolean hasBeenExpanded = tree.hasBeenExpanded(path);
            boolean isLeaf = tree.getModel().isLeaf(path.getLastPathComponent());

            if (context.paintHandles()) {
                context.paintHorizontalPartOfLeg(g, clipBounds, insets, bounds, path, rowForPath,
                        isExpanded, hasBeenExpanded, isLeaf);
                if (isExpanded) {
                    context.paintVerticalPartOfLeg(g, clipBounds, insets, path);
                }
            }
            context.paintExpandControl(g, clipBounds, insets, bounds, path, rowForPath, isExpanded, hasBeenExpanded, isLeaf);
            if (!context.isEditing(path)) {
                context.paintRow(g, clipBounds, insets, bounds, path, rowForPath, isExpanded, hasBeenExpanded, isLeaf);
            }

            if (endPath.equals(path)) {
                break;
            }
        }
        if (context.paintHorizontalSeparators()) {
            context.paintHorizontalSeparators(g, tree);
        }
    }
}
