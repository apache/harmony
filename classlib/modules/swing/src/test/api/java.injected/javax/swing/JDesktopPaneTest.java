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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicDesktopPaneUI;

public class JDesktopPaneTest extends SwingTestCase {
    /*
     * This class is used to test protected methods.
     */
    private class TestJDesktopPane extends JDesktopPane {
        private static final long serialVersionUID = 1L;

        @Override
        public String paramString() {
            return super.paramString();
        }
    }

    /*
     * This class is used to test that some property is (or is not) a bound property
     */
    private class MyPropertyChangeListener implements PropertyChangeListener {
        public boolean ok;

        MyPropertyChangeListener() {
            ok = false;
        }

        public void propertyChange(final PropertyChangeEvent e) {
            ok = true;
        }
    }

    private JDesktopPane desktop;

    public JDesktopPaneTest(final String name) {
        super(name);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        desktop = new JDesktopPane();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for boolean isOpaque()
     */
    public void testIsOpaque() {
        assertTrue("always returns true", desktop.isOpaque());
        desktop.setOpaque(false);
        assertTrue("always returns true", desktop.isOpaque());
    }

    /*
     * Class under test for void updateUI()
     */
    public void testUpdateUI() {
        desktop.updateUI();
        ComponentUI ui1 = desktop.getUI();
        ComponentUI ui2 = UIManager.getUI(desktop);
        // at least names of classes must be the same
        assertEquals(ui2.getClass().getName(), ui1.getClass().getName());
    }

    /*
     * Class under test for AccessibleContext getAccessibleContext()
     */
    public void testGetAccessibleContext() {
        AccessibleContext c = desktop.getAccessibleContext();
        assertTrue("instanceof AccessibleJDesktopPane",
                c instanceof JDesktopPane.AccessibleJDesktopPane);
        assertTrue("AccessibleRole is ok", c.getAccessibleRole() == AccessibleRole.DESKTOP_PANE);
    }

    /*
     * Class under test for String paramString()
     */
    public void testParamString() {
        TestJDesktopPane desktop = new TestJDesktopPane();
        assertTrue(desktop.paramString() != null);
    }

    public void testJDesktopPane() {
        desktop = new JDesktopPane();
        assertTrue(desktop.isFocusCycleRoot());
        assertFalse(desktop.isFocusTraversalPolicyProvider());
        assertTrue("ui != null", desktop.getUI() != null);
    }

    /*
     * Class under test for
     *     void setUI(DesktopPaneUI)
     *     DesktopPaneUI getUI()
     */
    public void testSetGetUI() {
        BasicDesktopPaneUI ui = new BasicDesktopPaneUI();
        desktop.setUI(ui);
        assertTrue("UI is set", desktop.getUI() == ui);
    }

    /*
     * Class under test for
     *     void setSelectedFrame(JInternalFrame)
     *     JInternalFrame getSelectedFrame()
     */
    public void testSetGetSelectedFrame() {
        JInternalFrame f = new JInternalFrame();
        assertNull("null by default", desktop.getSelectedFrame());
        desktop.setSelectedFrame(f);
        assertTrue("is set", desktop.getSelectedFrame() == f);
        desktop.setSelectedFrame(null);
        assertNull("is set to null", desktop.getSelectedFrame());
    }

    /*
     * Adds some components to the desktop.
     */
    private void addComponents() {
        // add one frame, layer 0
        JInternalFrame frame1 = new JInternalFrame("frame1");
        frame1.setVisible(true);
        desktop.add(frame1);
        // add iconified frame, layer 1
        JInternalFrame frame2 = new JInternalFrame("frame2");
        frame2.setVisible(true);
        frame2.setLayer(1);
        desktop.add(frame2);
        try {
            frame2.setIcon(true);
        } catch (PropertyVetoException e) {
            assertFalse("exception", true);
        }
        // add some non JInternalFrame component
        JComponent comp = new JPanel();
        desktop.add(comp);
    }

    /*
     * Class under test for JInternalFrame[] getAllFrames()
     */
    public void testGetAllFrames() {
        JInternalFrame[] frames = desktop.getAllFrames();
        assertTrue("empty array", frames.length == 0);
        addComponents();
        frames = desktop.getAllFrames();
        assertTrue("2 frames", frames.length == 2);
    }

    /*
     * Class under test for JInternalFrame[] getAllFramesInLayer(int)
     */
    public void testGetAllFramesInLayer() {
        JInternalFrame[] frames; // = desktop.getAllFramesInLayer(1);
        //assertTrue("empty array", frames.length == 0);
        addComponents();
        frames = desktop.getAllFramesInLayer(0);
        assertTrue("1 frame", frames.length == 1);
        assertTrue("frame1", frames[0].getTitle() == "frame1");
        // invisible frames are counted
        frames[0].setVisible(false);
        frames = desktop.getAllFramesInLayer(0);
        assertTrue("1 frame", frames.length == 1);
        // iconified frames are counted
        frames = desktop.getAllFramesInLayer(1);
        assertTrue("1 frame", frames.length == 1);
        assertTrue("frame2", frames[0].getTitle() == "frame2");
        // no frames in this layer
        frames = desktop.getAllFramesInLayer(2);
        assertTrue("empty array", frames.length == 0);
    }

    /*
     * Class under test for
     *     void setDesktopManager(DesktopManager)
     *     DesktopManager getDesktopManager()
     */
    public void testSetGetDesktopManager() {
        MyPropertyChangeListener l = new MyPropertyChangeListener();
        DesktopManager m = new DefaultDesktopManager();
        assertTrue("not null by default", desktop.getDesktopManager() != null);
        desktop.addPropertyChangeListener("desktopManager", l);
        desktop.setDesktopManager(m);
        assertTrue("is set", desktop.getDesktopManager() == m);
        assertTrue("bound property", l.ok);
        desktop.setDesktopManager(null);
        assertTrue("is not set to null", desktop.getDesktopManager() != null);
    }

    /*
     * Class under test for String getUIClassID()
     */
    public void testGetUIClassID() {
        assertTrue("", desktop.getUIClassID() == "DesktopPaneUI");
    }

    /*
     * Class under test for
     *     void setDragMode(int)
     *     int getDragMode()
     */
    public void testSetGetDragMode() {
        assertTrue("initial ok", desktop.getDragMode() == JDesktopPane.LIVE_DRAG_MODE);
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        assertTrue("set", desktop.getDragMode() == JDesktopPane.OUTLINE_DRAG_MODE);
        desktop.setDragMode(JDesktopPane.LIVE_DRAG_MODE);
        assertTrue("set", desktop.getDragMode() == JDesktopPane.LIVE_DRAG_MODE);
    }
}
