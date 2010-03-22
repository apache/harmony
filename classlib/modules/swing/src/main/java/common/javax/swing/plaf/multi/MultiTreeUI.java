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

package javax.swing.plaf.multi;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.TreePath;

/**
 * All the methods described in public api
 */
public class MultiTreeUI extends TreeUI {

	protected Vector uis = new Vector();

	/**
	 * Used in cycles. numberOfUIs = Correct number of UIs + 1, but the variable
	 * used in that sence
	 */
	private int numberOfUIs;

	public static ComponentUI createUI(JComponent a) {
		MultiTreeUI mui = new MultiTreeUI();
		ComponentUI result = MultiLookAndFeel.createUIs(mui, mui.uis, a);
		mui.numberOfUIs = mui.uis.size();
		return result;
	}
	
	@Override
	public boolean contains(JComponent a, int b, int c) {
		for (int i = 1; i < numberOfUIs; i++) {
			((ComponentUI) uis.get(i)).contains(a, b, c);
		}
		return ((ComponentUI) uis.firstElement()).contains(a, b, c);
	}

	@Override
	public Accessible getAccessibleChild(JComponent a, int b) {
		for (int i = 1; i < numberOfUIs; i++) {
			((ComponentUI) uis.get(i)).getAccessibleChild(a, b);
		}
		return ((ComponentUI) uis.firstElement()).getAccessibleChild(a, b);
	}

	@Override
	public int getAccessibleChildrenCount(JComponent a) {
		for (int i = 1; i < numberOfUIs; i++) {
			((ComponentUI) uis.get(i)).getAccessibleChildrenCount(a);
		}
		return ((ComponentUI) uis.firstElement()).getAccessibleChildrenCount(a);
	}

	@Override
	public Dimension getMaximumSize(JComponent a) {
		for (int i = 1; i < numberOfUIs; i++) {
			((ComponentUI) uis.get(i)).getMaximumSize(a);
		}
		return ((ComponentUI) uis.firstElement()).getMaximumSize(a);
	}

	@Override
	public Dimension getMinimumSize(JComponent a) {
		for (int i = 1; i < numberOfUIs; i++) {
			((ComponentUI) uis.get(i)).getMinimumSize(a);
		}
		return ((ComponentUI) uis.firstElement()).getMinimumSize(a);
	}

	@Override
	public Dimension getPreferredSize(JComponent a) {
		for (int i = 1; i < numberOfUIs; i++) {
			((ComponentUI) uis.get(i)).getPreferredSize(a);
		}
		return ((ComponentUI) uis.firstElement()).getPreferredSize(a);
	}

	public ComponentUI[] getUIs() {
		return MultiLookAndFeel.uisToArray(uis);
	}

	@Override
	public void installUI(JComponent a) {
		for (Object ui : uis) {
			((ComponentUI) ui).installUI(a);
		}
	}

	@Override
	public void paint(Graphics a, JComponent b) {
		for (Object ui : uis) {
			((ComponentUI) ui).paint(a, b);
		}
	}

	@Override
	public void uninstallUI(JComponent a) {
		for (Object ui : uis) {
			((ComponentUI) ui).uninstallUI(a);
		}
	}

	@Override
	public void update(Graphics a, JComponent b) {
		for (Object ui : uis) {
			((ComponentUI) ui).update(a, b);
		}
	}
	
	@Override
	public Rectangle getPathBounds(JTree tree, TreePath path) {
		for (int i = 1; i < numberOfUIs; i++) {
			((TreeUI) uis.get(i)).getPathBounds(tree, path);
		}
		return ((TreeUI) uis.firstElement()).getPathBounds(tree, path);
	}

	@Override
	public TreePath getPathForRow(JTree tree, int row) {
		for (int i = 1; i < numberOfUIs; i++) {
			((TreeUI) uis.get(i)).getPathForRow(tree, row);
		}
		return ((TreeUI) uis.firstElement()).getPathForRow(tree, row);
	}

	@Override
	public int getRowForPath(JTree tree, TreePath path) {
		for (int i = 1; i < numberOfUIs; i++) {
			((TreeUI) uis.get(i)).getRowForPath(tree, path);
		}
		return ((TreeUI) uis.firstElement()).getRowForPath(tree, path);
	}

	@Override
	public int getRowCount(JTree tree) {
		for (int i = 1; i < numberOfUIs; i++) {
			((TreeUI) uis.get(i)).getRowCount(tree);
		}
		return ((TreeUI) uis.firstElement()).getRowCount(tree);
	}

	@Override
	public TreePath getClosestPathForLocation(JTree tree, int x, int y) {
		for (int i = 1; i < numberOfUIs; i++) {
			((TreeUI) uis.get(i)).getClosestPathForLocation(tree,x,y);
		}
		return ((TreeUI) uis.firstElement()).getClosestPathForLocation(tree,x,y);
	}

	@Override
	public boolean isEditing(JTree tree) {
		for (int i = 1; i < numberOfUIs; i++) {
			((TreeUI) uis.get(i)).isEditing(tree);
		}
		return ((TreeUI) uis.firstElement()).isEditing(tree);
	}

	@Override
	public boolean stopEditing(JTree path) {
		for (int i = 1; i < numberOfUIs; i++) {
			((TreeUI) uis.get(i)).stopEditing(path);
		}
		return ((TreeUI) uis.firstElement()).stopEditing(path);
	}

	@Override
	public void cancelEditing(JTree tree) {
		for (Object ui : uis) {
			((TreeUI) ui).cancelEditing(tree);
		}
	}

	@Override
	public void startEditingAtPath(JTree tree, TreePath path) {
		for (Object ui : uis) {
			((TreeUI) ui).startEditingAtPath(tree, path);
		}
	}

	@Override
	public TreePath getEditingPath(JTree tree) {
		for (int i = 1; i < numberOfUIs; i++) {
			((TreeUI) uis.get(i)).getEditingPath(tree);
		}
		return ((TreeUI) uis.firstElement()).getEditingPath(tree);
	}
}
