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
package javax.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.GridLayout;
import javax.swing.plaf.ComboBoxUI;

public class LayoutFocusTraversalPolicyTest extends BasicSwingTestCase {
    private LayoutFocusTraversalPolicy policy;

    private JButton button1;

    private JButton button2;

    private JButton button3;

    private JButton button4;

    private JFrame frame;

    public LayoutFocusTraversalPolicyTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        policy = new LayoutFocusTraversalPolicy();
        frame = new JFrame();
        button1 = createButton("1");
        button2 = createButton("2");
        button3 = createButton("3");
        button4 = createButton("4");
    }

    @Override
    protected void tearDown() throws Exception {
        policy = null;
        button1 = null;
        button2 = null;
        button3 = null;
        button4 = null;
        frame.dispose();
        frame = null;
    }

    public void testAccept() throws Exception {
        frame.setVisible(true);
        SwingWaitTestCase.isRealized(frame);
        Component invisible = new JPanel();
        frame.getContentPane().add(invisible);
        invisible.setVisible(false);
        assertFalse(policy.accept(invisible));
        Component undisplayable = new JPanel();
        assertFalse(policy.accept(undisplayable));
        Component disabled = new JPanel();
        frame.getContentPane().add(disabled);
        disabled.setEnabled(false);
        assertFalse(policy.accept(disabled));
        Component unfocusable = new JPanel();
        frame.getContentPane().add(unfocusable);
        unfocusable.setFocusable(false);
        assertFalse(policy.accept(unfocusable));
        JComboBox comboBox = new JComboBox();
        comboBox.setUI(new TestComboBoxUI(false));
        frame.getContentPane().add(comboBox);
        assertFalse(policy.accept(comboBox));
        comboBox = new JComboBox();
        comboBox.setUI(new TestComboBoxUI(true));
        frame.getContentPane().add(comboBox);
        assertTrue(policy.accept(comboBox));
        JComponent panel = new JPanel();
        frame.getContentPane().add(panel);
        panel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke('a'), "anything");
        assertTrue(policy.accept(panel));
        panel.getInputMap(JComponent.WHEN_FOCUSED).clear();
        assertFalse(policy.accept(panel));
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        assertFalse(policy.accept(menuBar));
        JMenu menu = new JMenu();
        menuBar.add(menu);
        assertFalse(policy.accept(menu));
        //TODO: check for DefaultFocusTraversalPolicy.accept() should be provided here
    }

    public void testGetComponentBefore() throws Exception {
        JPanel cycleRoot = createTestPanel(ComponentOrientation.LEFT_TO_RIGHT);
        assertEquals(button1, policy.getComponentBefore(cycleRoot, button2));
        assertEquals(button2, policy.getComponentBefore(cycleRoot, button3));
        assertEquals(button3, policy.getComponentBefore(cycleRoot, button4));
        assertEquals(button4, policy.getComponentBefore(cycleRoot, button1));
        cycleRoot = createTestPanel(ComponentOrientation.RIGHT_TO_LEFT);
        assertEquals(button1, policy.getComponentBefore(cycleRoot, button2));
        assertEquals(button2, policy.getComponentBefore(cycleRoot, button3));
        assertEquals(button3, policy.getComponentBefore(cycleRoot, button4));
        assertEquals(button4, policy.getComponentBefore(cycleRoot, button1));
    }

    public void testGetComponentBefore_Null() throws Exception {
        JPanel cycleRoot = createTestPanel(ComponentOrientation.LEFT_TO_RIGHT);

        try {
            policy.getComponentBefore(cycleRoot, null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            policy.getComponentBefore(null, button1);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            policy.getComponentBefore(null, null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testGetComponentAfter() throws Exception {
        JPanel cycleRoot = createTestPanel(ComponentOrientation.LEFT_TO_RIGHT);
        assertEquals(button1, policy.getComponentAfter(cycleRoot, button4));
        assertEquals(button2, policy.getComponentAfter(cycleRoot, button1));
        assertEquals(button3, policy.getComponentAfter(cycleRoot, button2));
        assertEquals(button4, policy.getComponentAfter(cycleRoot, button3));
        cycleRoot = createTestPanel(ComponentOrientation.RIGHT_TO_LEFT);
        assertEquals(button1, policy.getComponentAfter(cycleRoot, button4));
        assertEquals(button2, policy.getComponentAfter(cycleRoot, button1));
        assertEquals(button3, policy.getComponentAfter(cycleRoot, button2));
        assertEquals(button4, policy.getComponentAfter(cycleRoot, button3));
    }

    public void testGetComponentAfter_Null() throws Exception {
        JPanel cycleRoot = createTestPanel(ComponentOrientation.LEFT_TO_RIGHT);

        try {
            policy.getComponentAfter(cycleRoot, null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            policy.getComponentAfter(null, button1);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            policy.getComponentAfter(null, null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testGetLastComponent() throws Exception {
        JPanel cycleRoot = createTestPanel(ComponentOrientation.LEFT_TO_RIGHT);
        assertEquals(button1, policy.getLastComponent(cycleRoot));
        cycleRoot = createTestPanel(ComponentOrientation.RIGHT_TO_LEFT);
        assertEquals(button1, policy.getLastComponent(cycleRoot));
    }

    public void testGetLastComponent_Null() throws Exception {
        JPanel cycleRoot = createTestPanel(ComponentOrientation.LEFT_TO_RIGHT);

        try {
            policy.getLastComponent(null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testGetFirstComponent() throws Exception {
        JPanel cycleRoot = createTestPanel(ComponentOrientation.LEFT_TO_RIGHT);
        assertEquals(button2, policy.getFirstComponent(cycleRoot));
        cycleRoot = createTestPanel(ComponentOrientation.RIGHT_TO_LEFT);
        assertEquals(button2, policy.getFirstComponent(cycleRoot));
    }

    public void testGetFirstComponent_Null() throws Exception {
        JPanel cycleRoot = createTestPanel(ComponentOrientation.LEFT_TO_RIGHT);

        try {
            policy.getFirstComponent(null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    private JPanel createTestPanel(final ComponentOrientation co) throws Exception {
        JPanel result = new JPanel(new GridLayout(2, 2));
        frame.getContentPane().add(result);
        result.setFocusCycleRoot(true);
        result.setFocusable(false);
        result.setFocusTraversalPolicy(policy);
        result.setComponentOrientation(co);
        result.add(button2);
        result.add(button3);
        result.add(button4);
        result.add(button1);
        frame.pack();
        frame.setVisible(true);
        SwingWaitTestCase.isRealized(frame);
        return result;
    }

    private JButton createButton(final String name) {
        JButton result = new JButton(name);
        result.setName(name);
        return result;
    }

    private class TestComboBoxUI extends ComboBoxUI {
        private final boolean isFocusTraversable;

        public TestComboBoxUI(final boolean isFocusTraversable) {
            this.isFocusTraversable = isFocusTraversable;
        }

        @Override
        public boolean isFocusTraversable(final JComboBox comboBox) {
            return isFocusTraversable;
        }

        @Override
        public boolean isPopupVisible(final JComboBox comboBox) {
            return false;
        }

        @Override
        public void setPopupVisible(final JComboBox comboBox, final boolean isVisible) {
        }
    }
}
