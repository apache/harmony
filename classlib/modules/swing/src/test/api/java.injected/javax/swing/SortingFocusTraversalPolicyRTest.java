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
import java.util.Comparator;

public class SortingFocusTraversalPolicyRTest extends BasicSwingTestCase {
    private SortingFocusTraversalPolicy policy;

    private JButton button1;

    private JButton button2;

    private JButton button3;

    private JButton button4;

    private JFrame frame;

    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        policy = new SortingFocusTraversalPolicy(new TestComparator());
        button1 = createButton("1");
        button2 = createButton("2");
        button3 = createButton("3");
        button4 = createButton("4");
        frame = new JFrame();
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
        super.tearDown();
    }

    public void testGetComponentAfterInNonDisplayableHierarchy() throws Exception {
        JFrame f = new JFrame();
        f.setVisible(true);
        SwingWaitTestCase.isRealized(f);
        JPanel cycleRoot = createPanelWithButtons();
        f.getContentPane().add(cycleRoot);
        cycleRoot.setFocusCycleRoot(true);
        cycleRoot.setFocusable(false);
        assertEquals(button2, policy.getComponentAfter(cycleRoot, button1));
        assertEquals(button3, policy.getComponentAfter(cycleRoot, button2));
        assertEquals(button4, policy.getComponentAfter(cycleRoot, button3));
        assertEquals(button1, policy.getComponentAfter(cycleRoot, button4));
        f.setVisible(false);
        assertNull(policy.getComponentAfter(cycleRoot, button1));
        f.setVisible(true);
        assertEquals(button2, policy.getComponentAfter(cycleRoot, button1));
        f.getContentPane().remove(cycleRoot);
        assertNull(policy.getComponentAfter(cycleRoot, button1));
    }

    public void testGetComponentBeforeAfterNotAcceptable() throws Exception {
        JFrame f = new JFrame();
        f.setVisible(true);
        SwingWaitTestCase.isRealized(f);
        JPanel cycleRoot = createPanelWithButtons();
        f.getContentPane().add(cycleRoot);
        cycleRoot.setFocusCycleRoot(true);
        cycleRoot.setFocusable(false);
        button2.setEnabled(false);
        f.setVisible(true);
        assertEquals(button3, policy.getComponentAfter(cycleRoot, button1));
        assertEquals(button3, policy.getComponentAfter(cycleRoot, button2));
        assertEquals(button4, policy.getComponentAfter(cycleRoot, button3));
        assertEquals(button1, policy.getComponentAfter(cycleRoot, button4));
        assertEquals(button4, policy.getComponentBefore(cycleRoot, button1));
        assertEquals(button1, policy.getComponentBefore(cycleRoot, button2));
        assertEquals(button1, policy.getComponentBefore(cycleRoot, button3));
        assertEquals(button3, policy.getComponentBefore(cycleRoot, button4));
    }

    private JPanel createPanelWithButtons() {
        JPanel result = new JPanel();
        result.setFocusTraversalPolicy(policy);
        result.add(button3);
        result.add(button4);
        result.add(button1);
        result.add(button2);
        return result;
    }

    private JButton createButton(final String name) {
        JButton result = new JButton(name);
        result.setName(name);
        return result;
    }

    @SuppressWarnings("unchecked")
    private class TestComparator implements Comparator {
        public int compare(final Object o1, final Object o2) {
            Component c1 = (Component) o1;
            Component c2 = (Component) o2;
            if (c1.getName() == null) {
                return -1;
            } else if (c2.getName() == null) {
                return 1;
            } else {
                return c1.getName().compareTo(c2.getName());
            }
        }
    }
}
