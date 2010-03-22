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
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SplitPaneUI;

/**
 * All the methods described in public api
 */
public class MultiSplitPaneUI extends SplitPaneUI {

	protected Vector uis = new Vector();

	/**
	 * Used in cycles. numberOfUIs = Correct number of UIs + 1, but the variable
	 * used in that sence
	 */
	private int numberOfUIs;

	public static ComponentUI createUI(JComponent a) {
		MultiSplitPaneUI mui = new MultiSplitPaneUI();
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
	public void resetToPreferredSizes(JSplitPane jc) {
		for (Object ui : uis) {
			((SplitPaneUI) ui).resetToPreferredSizes(jc);
		}
	}

	@Override
	public void setDividerLocation(JSplitPane jc, int location) {
		for (Object ui : uis) {
			((SplitPaneUI) ui).setDividerLocation(jc, location);
		}
	}

	@Override
	public int getDividerLocation(JSplitPane jc) {
		for (int i = 1; i < numberOfUIs; i++) {
			((SplitPaneUI) uis.get(i)).getDividerLocation(jc);
		}
		return ((SplitPaneUI) uis.firstElement()).getDividerLocation(jc);
	}

	@Override
	public int getMinimumDividerLocation(JSplitPane jc) {
		for (int i = 1; i < numberOfUIs; i++) {
			((SplitPaneUI) uis.get(i)).getMinimumDividerLocation(jc);
		}
		return ((SplitPaneUI) uis.firstElement()).getMinimumDividerLocation(jc);
	}

	@Override
	public int getMaximumDividerLocation(JSplitPane jc) {
		for (int i = 1; i < numberOfUIs; i++) {
			((SplitPaneUI) uis.get(i)).getMaximumDividerLocation(jc);
		}
		return ((SplitPaneUI) uis.firstElement()).getMaximumDividerLocation(jc);
	}

	@Override
	public void finishedPaintingChildren(JSplitPane jc, Graphics g) {
		for (Object ui : uis) {
			((SplitPaneUI) ui).finishedPaintingChildren(jc, g);
		}
	}
}
