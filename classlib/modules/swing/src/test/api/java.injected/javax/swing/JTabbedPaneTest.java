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
 * @author Vadim L. Bogdanov
 */
package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicIconFactory;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

public class JTabbedPaneTest extends SwingTestCase {
    private class MyChangeListener implements ChangeListener {
        public boolean eventFired;

        public ChangeEvent event;

        public void stateChanged(final ChangeEvent e) {
            eventFired = true;
            event = e;
        }
    }

    private class MyPropertyChangeListener implements PropertyChangeListener {
        public boolean eventFired;

        public void propertyChange(final PropertyChangeEvent event) {
            eventFired = true;
        }
    }

    private static Icon someIcon = BasicIconFactory.createEmptyFrameIcon();

    private String tabTitle1 = "tab1";

    private Icon tabIcon = null;

    private JComponent tabComponent1 = new JPanel();

    private String tabTip1 = "tip1";

    private int tabIndex1 = 0;

    private String tabTitle2 = "tab2";

    private JComponent tabComponent2 = new JPanel();

    private String tabTip2 = "tip2";

    private JComponent tabComponent3 = new JPanel();

    private JTabbedPane tabbed;

    public JTabbedPaneTest(final String name) {
        super(name);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tabbed = new JTabbedPane();
        tabbed.setSize(100, 50);
        tabbed.insertTab(tabTitle1, tabIcon, tabComponent1, tabTip1, tabIndex1);
        tabbed.insertTab(tabTitle2, tabIcon, tabComponent2, tabTip2, tabIndex1);
        tabbed.insertTab(tabTitle1, tabIcon, tabComponent3, tabTip1, 0);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testRemoveAll() {
        tabbed.removeAll();
        assertEquals("removed tabs", 0, tabbed.getTabCount());
        assertEquals("removed components", 0, tabbed.getComponentCount());
        assertEquals("no selected component", -1, tabbed.getSelectedIndex());
    }

    public void testUpdateUI() {
        tabbed.updateUI();
        ComponentUI ui1 = tabbed.getUI();
        ComponentUI ui2 = UIManager.getUI(tabbed);
        // at least names of classes must be the same
        assertEquals(ui2.getClass().getName(), ui1.getClass().getName());
    }

    public void testGetTabCount() {
        assertEquals(3, tabbed.getTabCount());
    }

    public void testGetTabRunCount() {
        assertEquals(tabbed.getUI().getTabRunCount(tabbed), tabbed.getTabRunCount());
        tabbed.setUI(null);
        assertEquals(0, tabbed.getTabRunCount());
    }

    public void testFireStateChanged() {
        MyChangeListener l = new MyChangeListener();
        tabbed.addChangeListener(l);
        tabbed.fireStateChanged();
        assertTrue(l.eventFired);
        assertSame("source", tabbed, l.event.getSource());
    }

    public void testJTabbedPane() {
        tabbed = new JTabbedPane();
        assertEquals("placement", SwingConstants.TOP, tabbed.getTabPlacement());
        assertEquals("tabLayout", JTabbedPane.WRAP_TAB_LAYOUT, tabbed.getTabLayoutPolicy());
        assertTrue("ui != null", tabbed.getUI() != null);
        assertEquals("empty", 0, tabbed.getTabCount());
    }

    public void testJTabbedPaneint() {
        tabbed = new JTabbedPane(SwingConstants.BOTTOM);
        assertEquals("placement", SwingConstants.BOTTOM, tabbed.getTabPlacement());
        assertEquals("tabLayout", JTabbedPane.WRAP_TAB_LAYOUT, tabbed.getTabLayoutPolicy());
    }

    public void testJTabbedPaneintint() {
        tabbed = new JTabbedPane(SwingConstants.RIGHT, JTabbedPane.SCROLL_TAB_LAYOUT);
        assertEquals("placement", SwingConstants.RIGHT, tabbed.getTabPlacement());
        assertEquals("tabLayout", JTabbedPane.SCROLL_TAB_LAYOUT, tabbed.getTabLayoutPolicy());
    }

    public void testRemoveTabAt() {
        int oldTabCount = tabbed.getTabCount();
        tabbed.removeTabAt(tabbed.indexOfComponent(tabComponent3));
        assertFalse("removed", tabbed.isAncestorOf(tabComponent3));
        assertTrue("visible", tabComponent3.isVisible());
        assertEquals("count -= 1", oldTabCount - 1, tabbed.getTabCount());
    }

    /*
     * Class under test for void remove(Component)
     */
    public void testRemoveComponent() {
        int oldTabCount = tabbed.getTabCount();
        tabbed.remove(new JLabel());
        assertEquals("count didn't change", oldTabCount, tabbed.getTabCount());
        tabbed.remove((Component) null);
        assertEquals("count didn't change", oldTabCount, tabbed.getTabCount());
        tabbed.remove(tabComponent3);
        assertFalse("removed", tabbed.isAncestorOf(tabComponent3));
        assertTrue("visible", tabComponent3.isVisible());
        assertEquals("count -= 1", oldTabCount - 1, tabbed.getTabCount());
    }

    /*
     * Class under test for void remove(int)
     */
    public void testRemoveint() {
        int oldTabCount = tabbed.getTabCount();
        tabbed.remove(tabbed.indexOfComponent(tabComponent3));
        assertFalse("removed", tabbed.isAncestorOf(tabComponent3));
        assertTrue("visible", tabComponent3.isVisible());
        assertEquals("count -= 1", oldTabCount - 1, tabbed.getTabCount());
    }

    public void testSetGetSelectedIndex() {
        assertEquals("0 by default", 2, tabbed.getSelectedIndex());
        assertFalse("invisible", tabbed.getComponentAt(1).isVisible());
        tabbed.setSelectedIndex(1);
        assertFalse("invisible", tabbed.getComponentAt(0).isVisible());
        assertEquals("set to 1", 1, tabbed.getSelectedIndex());
        assertEquals("set in model", 1, tabbed.getModel().getSelectedIndex());
        if (isHarmony()) {
            assertTrue("visible", tabbed.getSelectedComponent().isVisible());
        }
        boolean caught = false;
        try {
            tabbed.setSelectedIndex(100);
        } catch (IndexOutOfBoundsException e) {
            caught = true;
        }
        assertTrue("caught", caught);
        tabbed = new JTabbedPane();
        assertEquals("no selection", -1, tabbed.getSelectedIndex());
    }

    public void testSetGetTabLayoutPolicy() {
        assertEquals("default", JTabbedPane.WRAP_TAB_LAYOUT, tabbed.getTabLayoutPolicy());
        MyPropertyChangeListener l = new MyPropertyChangeListener();
        tabbed.addPropertyChangeListener("tabLayoutPolicy", l);
        tabbed.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        assertEquals("set", JTabbedPane.SCROLL_TAB_LAYOUT, tabbed.getTabLayoutPolicy());
        assertTrue(l.eventFired);
        boolean caught = false;
        try {
            tabbed.setTabLayoutPolicy(-4);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assertTrue("IllegalArgumentException", caught);
    }

    public void testSetGetTabPlacement() {
        assertEquals("default", SwingConstants.TOP, tabbed.getTabPlacement());
        MyPropertyChangeListener l = new MyPropertyChangeListener();
        tabbed.addPropertyChangeListener("tabPlacement", l);
        tabbed.setTabPlacement(SwingConstants.LEFT);
        assertEquals("set", SwingConstants.LEFT, tabbed.getTabPlacement());
        assertTrue(l.eventFired);
        boolean caught = false;
        try {
            tabbed.setTabPlacement(-4);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assertTrue("IllegalArgumentException", caught);
    }

    public void testIndexAtLocation() {
        int x = 2;
        int y = 2;
        assertEquals(tabbed.getUI().tabForCoordinate(tabbed, x, y), tabbed
                .indexAtLocation(x, y));
        tabbed.setUI(null);
        assertEquals(-1, tabbed.indexAtLocation(x, y));
    }

    public void testSetGetDisplayedMnemonicIndexAt() {
        assertEquals(-1, tabbed.getDisplayedMnemonicIndexAt(0));
        tabbed.setDisplayedMnemonicIndexAt(0, 1);
        assertEquals(1, tabbed.getDisplayedMnemonicIndexAt(0));
    }

    public void testSetGetMnemonicAt() {
        assertEquals(-1, tabbed.getMnemonicAt(1));
        tabbed.setMnemonicAt(1, KeyEvent.VK_X);
        assertEquals(KeyEvent.VK_X, tabbed.getMnemonicAt(1));
    }

    public void testSetIsEnabledAt() {
        assertTrue("by default", tabbed.isEnabledAt(1));
        tabbed.setEnabledAt(1, false);
        assertFalse("set to false", tabbed.isEnabledAt(1));
        tabbed.setEnabledAt(1, true);
        assertTrue("set to true", tabbed.isEnabledAt(1));
    }

    /*
     * Class under test for Component add(Component)
     */
    public void testAddComponent() {
        JComponent comp = new JLabel("label");
        comp.setName("label");
        Component result = tabbed.add(comp);
        assertEquals("result", comp, result);
        assertEquals("index", 3, tabbed.indexOfComponent(comp));
        assertEquals("title", "label", tabbed.getTitleAt(3));
        assertNull("tip", tabbed.getToolTipTextAt(3));
        class UIResourceButton extends JButton implements UIResource {
            private static final long serialVersionUID = 1L;
        }
        int tabCount = tabbed.getTabCount();
        comp = new UIResourceButton();
        result = tabbed.add(comp);
        assertSame(comp, result);
        assertEquals("no new tab for UIResource", tabCount, tabbed.getTabCount());
    }

    /*
     * Class under test for Component add(Component, int)
     */
    public void testAddComponentint() {
        JComponent comp = new JLabel("label");
        comp.setName("label");
        Component result = tabbed.add(comp, 2);
        assertEquals("result", comp, result);
        assertEquals("index", 2, tabbed.indexOfComponent(comp));
        assertEquals("title", "label", tabbed.getTitleAt(2));
        assertNull("tip", tabbed.getToolTipTextAt(2));
    }

    /*
     * Class under test for void add(Component, Object)
     */
    public void testAddComponentObject() {
        int index = 3;
        JComponent comp = new JLabel("label");
        comp.setName("labelName");
        Object constraints = "label";
        tabbed.add(comp, constraints);
        assertEquals("index", index, tabbed.indexOfComponent(comp));
        assertEquals("title", constraints, tabbed.getTitleAt(index));
        tabbed.remove(comp);
        comp = new JLabel("label");
        comp.setName("labelName");
        constraints = someIcon;
        tabbed.add(comp, constraints);
        assertEquals("title", "", tabbed.getTitleAt(index));
        assertEquals("icon", constraints, tabbed.getIconAt(index));
        tabbed.remove(comp);
        comp = new JLabel("label");
        comp.setName("labelName");
        constraints = new Integer(3); // just some Object
        tabbed.add(comp, constraints);
        assertEquals("title", "labelName", tabbed.getTitleAt(tabbed.indexOfComponent(comp)));
        assertNull("icon", tabbed.getIconAt(tabbed.indexOfComponent(comp)));
        tabbed.remove(comp);
    }

    /*
     * Class under test for void add(Component, Object, int)
     */
    public void testAddComponentObjectint() {
        int index = 2;
        JComponent comp = new JLabel("label");
        comp.setName("labelName");
        Object constraints = "label";
        tabbed.add(comp, constraints, index);
        assertEquals("index", index, tabbed.indexOfComponent(comp));
        assertEquals("title", constraints, tabbed.getTitleAt(index));
        tabbed.remove(comp);
        comp = new JLabel("label");
        comp.setName("labelName");
        constraints = BasicIconFactory.createEmptyFrameIcon(); // just some icon
        tabbed.add(comp, constraints, index);
        assertEquals("title", "", tabbed.getTitleAt(index));
        assertEquals("icon", constraints, tabbed.getIconAt(index));
        tabbed.remove(comp);
        comp = new JLabel("label");
        comp.setName("labelName");
        constraints = new Integer(3); // just some Object
        tabbed.add(comp, constraints, 1);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals("title", "labelName", tabbed.getTitleAt(tabbed.indexOfComponent(comp)));
        }
        assertNull("icon", tabbed.getIconAt(tabbed.indexOfComponent(comp)));
        tabbed.remove(comp);
    }

    /*
     * Class under test for Component add(String, Component)
     */
    public void testAddStringComponent() {
        JComponent comp = new JLabel("label");
        Component result = tabbed.add("label", comp);
        assertEquals("result", comp, result);
        assertEquals("index", 3, tabbed.indexOfComponent(comp));
        assertEquals("title", "label", tabbed.getTitleAt(3));
        assertNull("tip", tabbed.getToolTipTextAt(3));
    }

    /*
     * Class under test for void addChangeListener(ChangeListener)
     */
    public void testAddRemoveChangeListener() {
        ChangeListener l = new MyChangeListener();
        int len = tabbed.getChangeListeners().length;
        tabbed.addChangeListener(l);
        assertEquals("added", len + 1, tabbed.getChangeListeners().length);
        tabbed.removeChangeListener(l);
        assertEquals("removed", len, tabbed.getChangeListeners().length);
        tabbed.addChangeListener(null);
        assertEquals("adding null: no action", len, tabbed.getChangeListeners().length);
        tabbed.removeChangeListener(null);
        assertEquals("removing null: no action", len, tabbed.getChangeListeners().length);
    }

    /*
     * Class under test for void addTab(String, Component)
     */
    public void testAddTabStringComponent() {
        JComponent comp = new JLabel("label");
        tabbed.addTab("label", comp);
        assertEquals("index", 3, tabbed.indexOfComponent(comp));
        assertEquals("title", "label", tabbed.getTitleAt(3));
        assertNull("tip", tabbed.getToolTipTextAt(3));
    }

    /*
     * Class under test for void addTab(String, Icon, Component)
     */
    public void testAddTabStringIconComponent() {
        JComponent comp = new JLabel("label");
        tabbed.addTab("label", someIcon, comp);
        assertEquals("index", 3, tabbed.indexOfComponent(comp));
        assertEquals("title", "label", tabbed.getTitleAt(3));
        assertEquals("icon", someIcon, tabbed.getIconAt(3));
        assertNull("tip", tabbed.getToolTipTextAt(3));
    }

    /*
     * Class under test for void addTab(String, Icon, Component, String)
     */
    public void testAddTabStringIconComponentString() {
        JComponent comp = new JLabel("label");
        tabbed.addTab("label", someIcon, comp, "tip");
        assertEquals("index", 3, tabbed.indexOfComponent(comp));
        assertEquals("title", "label", tabbed.getTitleAt(3));
        assertEquals("icon", someIcon, tabbed.getIconAt(3));
        assertEquals("tip", "tip", tabbed.getToolTipTextAt(3));
    }

    /*
     * Class under test for ChangeListener createChangeListener()
     */
    public void testCreateChangeListener() {
        ChangeListener l1 = tabbed.createChangeListener();
        assertNotNull("not null", l1);
        assertNotSame("not same", l1, tabbed.changeListener);
    }

    /*
     * Class under test for AccessibleContext getAccessibleContext()
     */
    public void testGetAccessibleContext() {
        assertTrue(tabbed.getAccessibleContext() instanceof JTabbedPane.AccessibleJTabbedPane);
    }

    /*
     * Class under test for Rectangle getBoundsAt(int)
     */
    public void testGetBoundsAt() {
        assertEquals(tabbed.getBoundsAt(1), tabbed.getUI().getTabBounds(tabbed, 1));
        tabbed.setUI(null);
        assertNull(tabbed.getBoundsAt(1));
    }

    /*
     * Class under test for ChangeListener[] getChangeListeners()
     */
    public void testGetChangeListeners() {
        tabbed.setUI(null);
        assertEquals("empty array", 0, tabbed.getChangeListeners().length);
        tabbed.addChangeListener(new MyChangeListener());
        assertEquals("1 element", 1, tabbed.getChangeListeners().length);
    }

    /*
     * Class under test for void setTitleAt(int, String)
     */
    public void testSetTitleAt() {
        String newTitle = "newTitle";
        tabbed.setTitleAt(1, newTitle);
        assertEquals("newTitle is set", newTitle, tabbed.getTitleAt(1));
        boolean catched = false;
        try {
            tabbed.setTitleAt(-1, newTitle);
        } catch (IndexOutOfBoundsException e) {
            catched = true;
        }
        assertTrue("IndexOutOfBoundsException: index < 0", catched);
        catched = false;
        try {
            tabbed.setTitleAt(tabbed.getTabCount(), newTitle);
        } catch (IndexOutOfBoundsException e) {
            catched = true;
        }
        assertTrue("IndexOutOfBoundsException: index >= tab count", catched);
    }

    /*
     * Class under test for String getTitleAt(int)
     */
    public void testGetTitleAt() {
        assertEquals("title1", tabTitle1, tabbed.getTitleAt(0));
        assertEquals("title2", tabTitle2, tabbed.getTitleAt(1));
        assertEquals("title1_2", tabTitle1, tabbed.getTitleAt(2));
        boolean catched = false;
        try {
            tabbed.getTitleAt(-1);
        } catch (IndexOutOfBoundsException e) {
            catched = true;
        }
        assertTrue("IndexOutOfBoundsException: index < 0", catched);
        catched = false;
        try {
            tabbed.getTitleAt(tabbed.getTabCount());
        } catch (IndexOutOfBoundsException e) {
            catched = true;
        }
        assertTrue("IndexOutOfBoundsException: index >= tab count", catched);
    }

    /*
     * Class under test for String getToolTipText(MouseEvent)
     */
    public void testGetToolTipTextMouseEvent() {
        Rectangle bounds = tabbed.getBoundsAt(1);
        tabbed.setToolTipTextAt(1, "tooltip");
        MouseEvent e = new MouseEvent(tabbed, MouseEvent.MOUSE_MOVED, 0L, 0, bounds.x + 1,
                bounds.y + 1, 1, false);
        assertEquals("tooltip", tabbed.getToolTipText(e));
        e = new MouseEvent(tabbed, MouseEvent.MOUSE_MOVED, 0L, 0, Short.MAX_VALUE,
                Short.MAX_VALUE, 1, false);
        assertNull(tabbed.getToolTipText(e));
    }

    /*
     * Class under test for String getUIClassID()
     */
    public void testGetUIClassID() {
        assertEquals("uiClassID", "TabbedPaneUI", tabbed.getUIClassID());
    }

    /*
     * Class under test for int indexOfComponent(Component)
     */
    public void testIndexOfComponent() {
        assertEquals("index of null", -1, tabbed.indexOfComponent(null));
        tabbed.setComponentAt(2, tabComponent1);
        assertEquals("comp1", 2, tabbed.indexOfComponent(tabComponent1));
    }

    /*
     * Class under test for int indexOfTab(Icon)
     */
    public void testIndexOfTabIcon() {
        Icon otherIcon = BasicIconFactory.getCheckBoxIcon();
        assertEquals("index of null", 0, tabbed.indexOfTab((Icon) null));
        tabbed.setIconAt(1, someIcon);
        assertEquals(1, tabbed.indexOfTab(someIcon));
        assertEquals("no icon", -1, tabbed.indexOfTab(otherIcon));
    }

    /*
     * Class under test for int indexOfTab(String)
     */
    public void testIndexOfTabString() {
        assertEquals("index of null", -1, tabbed.indexOfTab((String) null));
        tabbed.setTitleAt(1, "someTitle");
        assertEquals(1, tabbed.indexOfTab("someTitle"));
        assertEquals("no icon", -1, tabbed.indexOfTab("otherTitle"));
    }

    public void testInsertTab() {
        tabbed.removeAll();
        tabbed = new JTabbedPane();
        assertEquals(-1, tabbed.getSelectedIndex());
        tabbed.insertTab(tabTitle1, tabIcon, tabComponent1, tabTip1, 0);
        assertEquals(0, tabbed.getSelectedIndex());
        tabbed.insertTab(tabTitle2, tabIcon, tabComponent2, tabTip2, 0);
        assertEquals(2, tabbed.getTabCount());
        assertEquals(1, tabbed.getSelectedIndex());
        assertSame(tabComponent1, tabbed.getComponent(0));
        assertSame(tabComponent2, tabbed.getComponent(1));
        assertSame(tabComponent2, tabbed.getComponentAt(0));
        assertSame(tabComponent1, tabbed.getComponentAt(1));
        assertEquals(1, tabbed.indexOfComponent(tabComponent1));
        assertEquals(0, tabbed.indexOfComponent(tabComponent2));
        assertEquals("title1", tabbed.getTitleAt(1), tabTitle1);
        assertEquals("component1", tabbed.getComponentAt(1), tabComponent1);
        assertEquals("tip1", tabbed.getToolTipTextAt(1), tabTip1);
        if (isHarmony()) {
            assertTrue("component1.isVisible()", tabComponent1.isVisible());
        } else {
            assertFalse("component1.isVisible()", tabComponent1.isVisible());
        }
        assertNotNull("background", tabbed.getBackgroundAt(1));
        assertNotNull("foreground", tabbed.getForegroundAt(1));
        assertEquals("title2", tabbed.getTitleAt(0), tabTitle2);
        assertEquals("component2", tabbed.getComponentAt(0), tabComponent2);
        assertEquals("tip2", tabbed.getToolTipTextAt(0), tabTip2);
        assertFalse("component2.isVisible()", tabComponent2.isVisible());
        tabbed.insertTab(tabTitle1, tabIcon, tabComponent3, tabTip1, 2);
        assertSame(tabComponent1, tabbed.getComponent(0));
        assertSame(tabComponent2, tabbed.getComponent(1));
        assertSame(tabComponent3, tabbed.getComponent(2));
        assertSame(tabComponent2, tabbed.getComponentAt(0));
        assertSame(tabComponent1, tabbed.getComponentAt(1));
        assertSame(tabComponent3, tabbed.getComponentAt(2));
        assertEquals(1, tabbed.indexOfComponent(tabComponent1));
        assertEquals(0, tabbed.indexOfComponent(tabComponent2));
        assertEquals(2, tabbed.indexOfComponent(tabComponent3));
        assertEquals(1, tabbed.getSelectedIndex());
        assertEquals(3, tabbed.getTabCount());
        assertFalse(tabComponent3.isVisible());
        tabbed.insertTab(tabTitle1, tabIcon, tabComponent3, tabTip1, 0);
        assertSame(tabComponent1, tabbed.getComponent(0));
        assertSame(tabComponent2, tabbed.getComponent(1));
        assertSame(tabComponent3, tabbed.getComponent(2));
        assertSame(tabComponent3, tabbed.getComponentAt(0));
        assertSame(tabComponent2, tabbed.getComponentAt(1));
        assertSame(tabComponent1, tabbed.getComponentAt(2));
        assertEquals(2, tabbed.indexOfComponent(tabComponent1));
        assertEquals(1, tabbed.indexOfComponent(tabComponent2));
        assertEquals(0, tabbed.indexOfComponent(tabComponent3));
        assertEquals(2, tabbed.getSelectedIndex());
        assertEquals(3, tabbed.getTabCount());
        tabbed.insertTab(null, tabIcon, tabComponent3, tabTip1, 0);
        assertSame(tabComponent1, tabbed.getComponent(0));
        assertSame(tabComponent2, tabbed.getComponent(1));
        assertSame(tabComponent3, tabbed.getComponent(2));
        assertSame(tabComponent3, tabbed.getComponentAt(0));
        assertSame(tabComponent2, tabbed.getComponentAt(1));
        assertSame(tabComponent1, tabbed.getComponentAt(2));
        assertEquals(2, tabbed.indexOfComponent(tabComponent1));
        assertEquals(1, tabbed.indexOfComponent(tabComponent2));
        assertEquals(0, tabbed.indexOfComponent(tabComponent3));
        assertEquals(2, tabbed.getSelectedIndex());
        assertEquals("tabCount == 2", 3, tabbed.getTabCount());
        assertEquals("title is empty, not null", "", tabbed.getTitleAt(0));
        JButton tabComponent4 = new JButton();
        tabbed.insertTab(null, tabIcon, tabComponent4, tabTip1, 1);
        assertSame(tabComponent1, tabbed.getComponent(0));
        assertSame(tabComponent2, tabbed.getComponent(1));
        assertSame(tabComponent3, tabbed.getComponent(2));
        assertSame(tabComponent4, tabbed.getComponent(3));
        assertSame(tabComponent3, tabbed.getComponentAt(0));
        assertSame(tabComponent4, tabbed.getComponentAt(1));
        assertSame(tabComponent2, tabbed.getComponentAt(2));
        assertSame(tabComponent1, tabbed.getComponentAt(3));
        assertEquals(3, tabbed.indexOfComponent(tabComponent1));
        assertEquals(2, tabbed.indexOfComponent(tabComponent2));
        assertEquals(1, tabbed.indexOfComponent(tabComponent4));
        assertEquals(0, tabbed.indexOfComponent(tabComponent3));
        assertEquals(3, tabbed.getSelectedIndex());
    }

    /*
     * Class under test for String paramString()
     */
    public void testParamString() {
        String paramString = tabbed.paramString();
        assertNotNull(paramString);
        assertFalse("".equals(paramString));
    }

    /*
     * Class under test for void setBackgroundAt(int, Color)
     */
    public void testSetBackgroundAt() {
        tabbed.setBackgroundAt(1, Color.RED);
        assertEquals(Color.RED, tabbed.getBackgroundAt(1));
        tabbed.setBackgroundAt(1, null);
        assertEquals("not null", tabbed.getBackground(), tabbed.getBackgroundAt(1));
    }

    /*
     * Class under test for Color getBackgroundAt(int)
     */
    public void testGetBackgroundAt() {
        assertTrue("instanceof UIResource", tabbed.getBackgroundAt(1) instanceof UIResource);
    }

    /*
     * Class under test for void setForegroundAt(int, Color)
     */
    public void testSetForegroundAt() {
        tabbed.setForegroundAt(1, Color.RED);
        assertEquals(Color.RED, tabbed.getForegroundAt(1));
        tabbed.setForegroundAt(1, null);
        assertEquals("not null", tabbed.getForeground(), tabbed.getForegroundAt(1));
    }

    /*
     * Class under test for Color getForegroundAt(int)
     */
    public void testGetForegroundAt() {
        assertTrue("instanceof UIResource", tabbed.getForegroundAt(1) instanceof UIResource);
    }

    public void testSetGetComponentAt() {
        JComponent newComp = new JLabel("new");
        int tabCount = tabbed.getTabCount();
        int index = tabbed.indexOfComponent(tabComponent2);
        assertSame(tabComponent1, tabbed.getComponent(0));
        assertSame(tabComponent2, tabbed.getComponent(1));
        assertSame(tabComponent3, tabbed.getComponent(2));
        assertSame(tabComponent3, tabbed.getComponentAt(0));
        assertSame(tabComponent2, tabbed.getComponentAt(1));
        assertSame(tabComponent1, tabbed.getComponentAt(2));
        assertEquals(2, tabbed.indexOfComponent(tabComponent1));
        assertEquals(1, tabbed.indexOfComponent(tabComponent2));
        assertEquals(0, tabbed.indexOfComponent(tabComponent3));
        tabbed.setComponentAt(index, newComp);
        assertSame(tabComponent1, tabbed.getComponent(0));
        assertSame(tabComponent3, tabbed.getComponent(1));
        assertSame(newComp, tabbed.getComponent(2));
        assertSame(tabComponent3, tabbed.getComponentAt(0));
        assertSame(newComp, tabbed.getComponentAt(1));
        assertSame(tabComponent1, tabbed.getComponentAt(2));
        assertEquals(2, tabbed.indexOfComponent(tabComponent1));
        assertEquals(1, tabbed.indexOfComponent(newComp));
        assertEquals(0, tabbed.indexOfComponent(tabComponent3));
        assertEquals(-1, tabbed.indexOfComponent(tabComponent2));
        assertEquals("tabCount", tabCount, tabbed.getTabCount());
        assertSame("component", newComp, tabbed.getComponentAt(index));
        assertFalse("newComp is not visible", newComp.isVisible());
        tabbed.setComponentAt(index, tabComponent3);
        assertSame(tabComponent1, tabbed.getComponent(0));
        assertSame(tabComponent3, tabbed.getComponent(1));
        assertSame(tabComponent3, tabbed.getComponentAt(0));
        assertSame(tabComponent1, tabbed.getComponentAt(1));
        assertEquals(0, tabbed.indexOfComponent(tabComponent3));
        assertEquals(1, tabbed.indexOfComponent(tabComponent1));
        assertEquals("tabCount - 1", tabCount - 1, tabbed.getTabCount());
        assertEquals("visibility", !BasicSwingTestCase.isHarmony(), tabComponent3.isVisible());
    }

    public void testSetGetDisabledIconAt() {
        assertNull(tabbed.getDisabledIconAt(0));
        tabbed.setDisabledIconAt(0, someIcon);
        assertSame(someIcon, tabbed.getDisabledIconAt(0));
    }

    public void testSetGetIconAt() {
        tabbed.setIconAt(1, someIcon);
        assertEquals(someIcon, tabbed.getIconAt(1));
    }

    public void testSetGetModel() {
        assertNotNull("default", tabbed.getModel());
        PropertyChangeController cont = new PropertyChangeController();
        tabbed.addPropertyChangeListener(cont);
        DefaultSingleSelectionModel model = new DefaultSingleSelectionModel();
        tabbed.setModel(model);
        assertEquals("set", model, tabbed.getModel());
        assertTrue("fired property change event", cont.isChanged("model"));
        assertTrue("listener", Arrays.asList(
                ((DefaultSingleSelectionModel) tabbed.getModel()).getChangeListeners())
                .contains(tabbed.changeListener));
        // set model with another selected index, no state change event is fired
        tabbed.setModel(null);
        MyChangeListener changeListener = new MyChangeListener();
        tabbed.addChangeListener(changeListener);
        model.setSelectedIndex(2);
        tabbed.setModel(model);
        assertFalse(changeListener.eventFired);
    }

    public void testSetGetSelectedComponentComponent() {
        tabbed.setSelectedComponent(tabComponent2);
        assertSame(tabComponent2, tabbed.getSelectedComponent());
        assertEquals(tabbed.indexOfComponent(tabComponent2), tabbed.getSelectedIndex());
        boolean caught = false;
        try {
            tabbed.setSelectedComponent(new JLabel());
        } catch (final IllegalArgumentException e) {
            caught = true;
        }
        assertTrue(caught);
    }

    public void testSetGetToolTipTextAt() {
        JComponent comp = new JLabel();
        tabbed.add(comp);
        int index = tabbed.indexOfComponent(comp);
        assertNull("by default", tabbed.getToolTipTextAt(index));
        tabbed.setToolTipTextAt(index, "newTip");
        assertEquals("newTip", tabbed.getToolTipTextAt(index));
        tabbed.setToolTipTextAt(index, null);
        assertNull(tabbed.getToolTipTextAt(index));
    }

    public void testSetGetUI() {
        BasicTabbedPaneUI ui = new BasicTabbedPaneUI();
        tabbed.setUI(ui);
        assertSame(ui, tabbed.getUI());
    }
}
