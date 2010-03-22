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
import javax.accessibility.AccessibleValue;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicDesktopIconUI;

public class JInternalFrame$JDesktopIconTest extends SwingTestCase {
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

    private JInternalFrame.JDesktopIcon icon;

    private JInternalFrame frame;

    public JInternalFrame$JDesktopIconTest(final String name) {
        super(name);
    }

    //public static void main(String[] args) {
    //}
    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JInternalFrame();
        icon = new JInternalFrame.JDesktopIcon(frame);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for void updateUI()
     */
    public void testUpdateUI() {
        icon.setBounds(0, 0, 0, 0);
        icon.updateUI();
        ComponentUI ui1 = frame.getUI();
        ComponentUI ui2 = UIManager.getUI(frame);
        // at least names of classes must be the same
        assertEquals(ui2.getClass().getName(), ui1.getClass().getName());
        assertTrue("size is set to preferred", icon.getSize().equals(icon.getPreferredSize()));
    }

    /*
     * Class under test for void JDesktopIcon(JInternalFrame)
     */
    public void testJDesktopIcon() {
        frame = new JInternalFrame();
        icon = new JInternalFrame.JDesktopIcon(frame);
        assertTrue("frame is set", icon.getInternalFrame() == frame);
        assertTrue("ui != null", icon.getUI() != null);
        assertTrue("size is set to preferred", icon.getSize().equals(icon.getPreferredSize()));
        assertFalse("isVisible", icon.isVisible());
    }

    /*
     * Class under test for
     *     void setUI(DesktopIconUI)
     *     DesktopIconUI getUI()
     */
    public void testSetGetUI() {
        BasicDesktopIconUI ui = new BasicDesktopIconUI();
        icon.setUI(ui);
        assertTrue(icon.getUI() == ui);
    }

    /*
     * Class under test for
     *     void setInternalFrame(JInternalFrame)
     *     JInternalFrame getInternalFrame()
     */
    public void testSetGetInternalFrame() {
        MyPropertyChangeListener l = new MyPropertyChangeListener();
        icon.addPropertyChangeListener(l);
        // test valid set
        frame = new JInternalFrame();
        icon.setInternalFrame(frame);
        assertTrue(icon.getInternalFrame() == frame);
        assertFalse("internalFrame is not a bound property", l.ok);
        // test set to null
        icon.setInternalFrame(null);
        assertNull(icon.getInternalFrame());
    }

    /*
     * Class under test for JDesktopPane getDesktopPane()
     */
    public void testGetDesktopPane() {
        assertNull("null by default", icon.getDesktopPane());
        // test when not iconified
        JDesktopPane desktop = new JDesktopPane();
        desktop.add(frame);
        assertTrue("desktop is set", icon.getDesktopPane() == desktop);
        // test when iconified
        try {
            frame.setIcon(true);
        } catch (PropertyVetoException e) {
            assertTrue("no exception", false);
        }
        assertTrue("desktop is set", icon.getDesktopPane() == desktop);
    }

    /*
     * Class under test for AccessibleContext getAccessibleContext()
     */
    public void testGetAccessibleContext() {
        AccessibleContext c = icon.getAccessibleContext();
        assertTrue("instanceof AccessibleJDesktopIcon",
                c instanceof JInternalFrame.JDesktopIcon.AccessibleJDesktopIcon);
        // test getAccessibleRole()
        assertTrue("AccessibleRole ok", c.getAccessibleRole() == AccessibleRole.DESKTOP_ICON);
        // test getAccessibleValue()
        assertTrue("AccessibleValue ok", c.getAccessibleValue() == c);
        // test setCurrentAccessibleValue(), getCurrentAccessibleValue()
        AccessibleValue value = c.getAccessibleValue();
        assertTrue("currentAccessibleValue == 0",
                value.getCurrentAccessibleValue().intValue() == 0);
        Integer currentAccessibleValue = new Integer(4);
        boolean set = value.setCurrentAccessibleValue(currentAccessibleValue);
        assertTrue("setCurrentAccessibleValue returns true", set);
        set = value.setCurrentAccessibleValue(new Float(5));
        assertTrue("setCurrentAccessibleValue returns true", set);
        assertTrue("currentAccessibleValue == 5",
                value.getCurrentAccessibleValue().intValue() == 5);
        assertTrue("the object is not the same",
                value.getCurrentAccessibleValue() != currentAccessibleValue);
        set = value.setCurrentAccessibleValue(null);
        assertFalse("setCurrentAccessibleValue returns false", set);
        // test getMinimumAccessibleValue()
        assertTrue("minimumAccessibleValue ok",
                value.getMinimumAccessibleValue().intValue() == Integer.MIN_VALUE);
        // test getMaximumAccessibleValue()
        assertTrue("maximumAccessibleValue ok",
                value.getMaximumAccessibleValue().intValue() == Integer.MAX_VALUE);
        // test other methods
        assertNull("AccessibleDescription is ok", c.getAccessibleDescription());
        assertTrue("AccessibleChildrenCount == 2", c.getAccessibleChildrenCount() == 2);
    }

    /*
     * Class under test for String getUIClassID()
     */
    public void testGetUIClassID() {
        assertTrue("getUIClassID() ok", icon.getUIClassID() == "DesktopIconUI");
    }
}
