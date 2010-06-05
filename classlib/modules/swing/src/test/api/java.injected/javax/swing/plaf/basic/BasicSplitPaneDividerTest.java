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
package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.awt.Insets;
import java.lang.reflect.Field;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.SwingTestCase;
import javax.swing.border.Border;

public class BasicSplitPaneDividerTest extends SwingTestCase {
    private BasicSplitPaneDivider divider;

    private JSplitPane pane;

    public BasicSplitPaneDividerTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        pane = new JSplitPane();
        divider = new BasicSplitPaneDivider((BasicSplitPaneUI) pane.getUI());
    }

    @Override
    protected void tearDown() throws Exception {
        divider = null;
        pane = null;
    }

    public void testBasicSplitPaneDivider() throws Exception {
        assertEquals(pane.getUI(), divider.splitPaneUI);
        assertEquals(pane, divider.splitPane);
        assertNotNull(divider.mouseHandler);
        assertNull(divider.leftButton);
        assertNull(divider.rightButton);
        assertNull(divider.dragger);
        assertNull(divider.hiddenDivider);
        assertEquals(0, divider.getDividerSize());
        assertEquals(0, divider.getDividerSize());
        assertEquals(JSplitPane.HORIZONTAL_SPLIT, divider.orientation);
    }

    public void testGetSetBasicSplitPaneUI() throws Exception {
        JSplitPane newPane = new JSplitPane();
        divider.setBasicSplitPaneUI((BasicSplitPaneUI) newPane.getUI());
        assertEquals(newPane.getUI(), divider.splitPaneUI);
        assertEquals(newPane.getUI(), divider.getBasicSplitPaneUI());
        assertEquals(newPane, divider.splitPane);
    }

    public void testGetSetDividerSize() throws Exception {
        assertEquals(0, divider.getDividerSize());
        divider.setDividerSize(20);
        assertEquals(20, divider.getDividerSize());
        divider.setDividerSize(-20);
        assertEquals(-20, divider.getDividerSize());
    }

    public void testGetSetBorder() throws Exception {
        assertNull(divider.getBorder());
        Border b = BorderFactory.createEmptyBorder();
        divider.setBorder(b);
        assertEquals(b, divider.getBorder());
    }

    public void testGetInsets() throws Exception {
        assertEquals(new Insets(0, 0, 0, 0), divider.getInsets());
        divider.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 20));
        assertEquals(new Insets(5, 10, 15, 20), divider.getInsets());
    }

    public void testIsSetMouseOver() throws Exception {
        assertFalse(divider.isMouseOver());
        divider.setMouseOver(true);
        assertTrue(divider.isMouseOver());
    }

    public void testGetMinimumMaximumPreferredSize() throws Exception {
        assertEquals(new Dimension(0, 1), divider.getPreferredSize());
        assertEquals(new Dimension(0, 1), divider.getMinimumSize());
        assertEquals(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE), divider.getMaximumSize());
        divider.setDividerSize(20);
        assertEquals(new Dimension(20, 1), divider.getPreferredSize());
        assertEquals(new Dimension(20, 1), divider.getMinimumSize());
        assertEquals(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE), divider.getMaximumSize());
        divider.orientation = JSplitPane.VERTICAL_SPLIT;
        assertEquals(new Dimension(1, 20), divider.getPreferredSize());
        assertEquals(new Dimension(1, 20), divider.getMinimumSize());
        assertEquals(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE), divider.getMaximumSize());
    }

    //TODO
    public void testPropertyChange() throws Exception {
    }

    public void testOneTouchExpandableChanged() throws Exception {
        assertFalse(pane.isOneTouchExpandable());
        assertNull(divider.leftButton);
        assertNull(divider.rightButton);
        setProperty("oneTouchExpandable", Boolean.TRUE);
        divider.oneTouchExpandableChanged();
        assertNotNull(divider.leftButton);
        assertNotNull(divider.rightButton);
    }

    public void testCreateLeftOneTouchButton() throws Exception {
        JButton left = divider.createLeftOneTouchButton();
        assertNotNull(left);
        assertNotSame(left, divider.createLeftOneTouchButton());
    }

    public void testCreateRightOneTouchButton() throws Exception {
        JButton right = divider.createRightOneTouchButton();
        assertNotNull(right);
        assertNotSame(right, divider.createRightOneTouchButton());
    }

    private void setProperty(final String propertyName, final Object value) throws Exception {
        Field property = JSplitPane.class.getDeclaredField(propertyName);
        if (property != null) {
            property.setAccessible(true);
            property.set(pane, value);
        } else {
            fail("Cannot access property " + propertyName);
        }
    }
}
