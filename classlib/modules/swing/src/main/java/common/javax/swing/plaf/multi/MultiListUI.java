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
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ListUI;

/**
 * All the methods described in public api
 */
public class MultiListUI extends ListUI {

	protected Vector uis = new Vector();

	/**
	 * Used in cycles. numberOfUIs = Correct number of UIs + 1, but the variable
	 * used in that sence
	 */
	private int numberOfUIs;

	public static ComponentUI createUI(JComponent a) {
		MultiListUI mui = new MultiListUI();
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
	public int locationToIndex(JList list, Point location) {
		for (int i = 1; i < numberOfUIs; i++) {
			((ListUI) uis.get(i)).locationToIndex(list, location);
		}
		return ((ListUI) uis.firstElement()).locationToIndex(list, location);
	}

	@Override
	public Point indexToLocation(JList list, int index) {
		for (int i = 1; i < numberOfUIs; i++) {
			((ListUI) uis.get(i)).indexToLocation(list, index);
		}
		return ((ListUI) uis.firstElement()).indexToLocation(list, index);
	}

	@Override
	public Rectangle getCellBounds(JList list, int index1, int index2) {
		for (int i = 1; i < numberOfUIs; i++) {
			((ListUI) uis.get(i)).getCellBounds(list, index1, index2);
		}
		return ((ListUI) uis.firstElement()).getCellBounds(list, index1, index2);
	}
}
