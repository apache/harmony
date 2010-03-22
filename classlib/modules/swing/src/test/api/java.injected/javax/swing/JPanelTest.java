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
 * @author Alexander T. Simbirtsev
 */
package javax.swing;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.io.IOException;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.PanelUI;
import javax.swing.plaf.basic.BasicPanelUI;

public class JPanelTest extends SwingTestCase {
    /**
     * @param arg0
     */
    public JPanelTest(final String arg0) {
        super(arg0);
    }

    protected JPanel panel = null;

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(JPanelTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        panel = new JPanel();
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
        panel.setUI(null);
        panel.updateUI();
        assertTrue(panel.getUI() != null);
    }

    /*
     * Class under test for void JPanel(LayoutManager, boolean)
     */
    public void testJPanelLayoutManagerboolean() {
        final LayoutManager layout1 = new GridLayout(2, 2);
        panel = new JPanel(layout1, true);
        assertTrue(panel.isOpaque());
        assertSame(layout1, panel.getLayout());
        assertTrue(panel.isDoubleBuffered());
        final LayoutManager layout2 = new FlowLayout();
        panel = new JPanel(layout2, false);
        assertTrue(panel.isOpaque());
        assertSame(layout2, panel.getLayout());
        assertFalse(panel.isDoubleBuffered());
    }

    /*
     * Class under test for void JPanel(LayoutManager)
     */
    public void testJPanelLayoutManager() {
        final LayoutManager layout1 = new GridLayout(2, 2);
        panel = new JPanel(layout1);
        assertTrue(panel.isOpaque());
        assertSame(layout1, panel.getLayout());
        assertTrue(panel.isDoubleBuffered());
        final LayoutManager layout2 = new FlowLayout();
        panel = new JPanel(layout2);
        assertTrue(panel.isOpaque());
        assertSame(layout2, panel.getLayout());
        assertTrue(panel.isDoubleBuffered());
    }

    /*
     * Class under test for void JPanel(boolean)
     */
    public void testJPanelboolean() {
        panel = new JPanel(true);
        assertTrue(panel.isOpaque());
        assertTrue(panel.getLayout().getClass() == FlowLayout.class);
        assertTrue(panel.isDoubleBuffered());
        panel = new JPanel(false);
        assertTrue(panel.isOpaque());
        assertTrue(panel.getLayout().getClass() == FlowLayout.class);
        assertFalse(panel.isDoubleBuffered());
    }

    /*
     * Class under test for void JPanel()
     */
    public void testJPanel() {
        assertTrue(panel.isOpaque());
        assertTrue(panel.getLayout().getClass() == FlowLayout.class);
        assertTrue(panel.isDoubleBuffered());
    }

    /*
     * Class under test for void setUI(PanelUI)
     */
    public void testSetUIPanelUI() {
        PanelUI panelUI = null;
        panel.setUI(panelUI);
        assertEquals(panelUI, panel.getUI());
        panelUI = new BasicPanelUI();
        panel.setUI(panelUI);
        assertEquals(panelUI, panel.getUI());
    }

    public void testGetUI() {
        testSetUIPanelUI();
    }

    /*
     * Class under test for AccessibleContext getAccessibleContext()
     */
    public void testGetAccessibleContext() {
        boolean assertedValue = (panel.getAccessibleContext() != null && panel
                .getAccessibleContext().getClass().getName().equals(
                        "javax.swing.JPanel$AccessibleJPanel"));
        assertTrue(assertedValue);
        assertTrue(panel.getAccessibleContext().getAccessibleRole()
                .equals(AccessibleRole.PANEL));
    }

    /*
     * Class under test for String paramString()
     */
    public void testParamString() {
        class TestingPanel extends JPanel {
            private static final long serialVersionUID = 1L;

            public String getParamString() {
                return paramString();
            }
        }
        TestingPanel testingPanel = new TestingPanel();
        assertTrue(testingPanel.getParamString() != null);
        assertTrue(testingPanel.getParamString() != "");
    }

    /*
     * Class under test for String getUIClassID()
     */
    public void testGetUIClassID() {
        assertEquals("PanelUI", panel.getUIClassID());
    }

    public void testWriteObject() throws IOException {
        /*
         JPanel button1 = new JPanel();
         JPanel button2 = new JPanel();
         FileOutputStream fo = new FileOutputStream("tmp");
         ObjectOutputStream so = new ObjectOutputStream(fo);
         so.writeObject(button1);
         so.flush();
         fo = new FileOutputStream("tmp");
         so = new ObjectOutputStream(fo);
         so.writeObject(button1);
         so.flush();
         */
    }

    public void testReadObject() throws IOException, ClassNotFoundException {
        /*
         JPanel button1 = new JPanel();
         JPanel button2 = new JPanel();
         FileOutputStream fo = new FileOutputStream("tmp");
         ObjectOutputStream so = new ObjectOutputStream(fo);
         so.writeObject(button1);
         so.flush();
         FileInputStream fi = new FileInputStream("tmp");
         ObjectInputStream si = new ObjectInputStream(fi);
         JPanel ressurectedButton = (JPanel)si.readObject();
         fo = new FileOutputStream("tmp");
         so = new ObjectOutputStream(fo);
         so.writeObject(button2);
         so.flush();
         fi = new FileInputStream("tmp");
         si = new ObjectInputStream(fi);
         ressurectedButton = (JPanel)si.readObject();
         */
    }
}
