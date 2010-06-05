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

@SuppressWarnings("unchecked")
public class SortingFocusTraversalPolicyTest extends BasicSwingTestCase {
    private SortingFocusTraversalPolicy policy;

    private JButton button1;

    private JButton button2;

    private JButton button3;

    private JButton button4;

    private JFrame frame;

    public SortingFocusTraversalPolicyTest(final String name) {
        super(name);
    }

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

    public void testSortingFocusTraversalPolicy() throws Exception {
        Comparator cmp = new TestComparator();
        policy = new SortingFocusTraversalPolicy(cmp);
        assertEquals(cmp, policy.getComparator());
    }

    public void testSetComparator() throws Exception {
        Comparator cmp = new TestComparator();
        policy.setComparator(cmp);
        assertEquals(cmp, policy.getComparator());
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
        Component acceptable = new JPanel();
        frame.getContentPane().add(acceptable);
        assertTrue(policy.accept(acceptable));
    }

    public void testGetComponentBeforeNoInnerCycleRoots() throws Exception {
        JPanel cycleRoot = createPanelWithButtons();
        frame.getContentPane().add(cycleRoot);
        cycleRoot.setFocusCycleRoot(true);
        cycleRoot.setFocusable(false);
        frame.setVisible(true);
        SwingWaitTestCase.isRealized(frame);
        assertEquals(button4, policy.getComponentBefore(cycleRoot, button1));
        assertEquals(button1, policy.getComponentBefore(cycleRoot, button2));
        assertEquals(button2, policy.getComponentBefore(cycleRoot, button3));
        assertEquals(button3, policy.getComponentBefore(cycleRoot, button4));
    }

    public void testGetComponentBeforeForNotCycleRoot() throws Exception {
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                policy.getComponentBefore(createNotCycleRootPanel(), button1);
            }
        });
    }

    public void testGetComponentBeforeForNullCycleRoot() throws Exception {
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                policy.getComponentBefore(null, button1);
            }
        });
    }

    public void testGetComponentBeforeForNullComponent() throws Exception {
        final JPanel cycleRoot = createPanelWithButtons();
        frame.getContentPane().add(cycleRoot);
        cycleRoot.setFocusCycleRoot(true);
        cycleRoot.setFocusable(false);
        frame.setVisible(true);
        SwingWaitTestCase.isRealized(frame);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                policy.getComponentBefore(cycleRoot, null);
            }
        });
    }

    public void testGetComponentAfterNoInnerCycleRoots() throws Exception {
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
    }

    public void testGetComponentAfterForNotCycleRoot() throws Exception {
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                policy.getComponentAfter(createNotCycleRootPanel(), button1);
            }
        });
    }

    public void testGetComponentAfterForNullCycleRoot() throws Exception {
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                policy.getComponentAfter(null, button1);
            }
        });
    }

    public void testGetComponentAfterForNullComponent() throws Exception {
        JFrame f = new JFrame();
        f.setVisible(true);
        final JPanel cycleRoot = createPanelWithButtons();
        f.getContentPane().add(cycleRoot);
        cycleRoot.setFocusCycleRoot(true);
        cycleRoot.setFocusable(false);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                policy.getComponentAfter(cycleRoot, null);
            }
        });
    }

    public void testGetLastComponent() throws Exception {
        JPanel cycleRoot = createPanelWithButtons();
        frame.getContentPane().add(cycleRoot);
        cycleRoot.setFocusCycleRoot(true);
        cycleRoot.setFocusable(false);
        frame.setVisible(true);
        SwingWaitTestCase.isRealized(frame);
        assertEquals(button4, policy.getLastComponent(cycleRoot));
    }

    public void testGetLastComponentForNotCycleRoot() throws Exception {
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                policy.getLastComponent(null);
            }
        });
    }

    public void testGetFirstComponent() throws Exception {
        JPanel cycleRoot = createPanelWithButtons();
        frame.getContentPane().add(cycleRoot);
        cycleRoot.setFocusCycleRoot(true);
        cycleRoot.setFocusable(false);
        frame.setVisible(true);
        SwingWaitTestCase.isRealized(frame);
        assertEquals(button1, policy.getFirstComponent(cycleRoot));
    }

    public void testGetDefaultComponent() throws Exception {
        JPanel cycleRoot = createPanelWithButtons();
        frame.getContentPane().add(cycleRoot);
        cycleRoot.setFocusCycleRoot(true);
        cycleRoot.setFocusable(false);
        frame.setVisible(true);
        SwingWaitTestCase.isRealized(frame);
        assertEquals(policy.getFirstComponent(cycleRoot), policy.getDefaultComponent(cycleRoot));
    }

    public void testComponentIsInACycleRoot() throws Exception {
        final JPanel cycleRoot = createPanelWithButtons();
        JPanel innerCycleRoot = new JPanel();
        innerCycleRoot.setName("9 - the latest");
        innerCycleRoot.setFocusCycleRoot(true);
        final JButton innerButton1 = createButton("1");
        innerCycleRoot.add(innerButton1);
        cycleRoot.add(innerCycleRoot);
        frame.getContentPane().add(cycleRoot);
        cycleRoot.setFocusCycleRoot(true);
        cycleRoot.setFocusable(false);
        frame.setVisible(true);
        SwingWaitTestCase.isRealized(frame);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                policy.getComponentAfter(cycleRoot, innerButton1);
            }
        });
    }

    public void testInnerCycleRootsProcessingWithImplicitTraversal() throws Exception {
        JPanel cycleRoot = createPanelWithButtons();
        JPanel innerCycleRoot = new JPanel();
        innerCycleRoot.setName("9 - the latest");
        innerCycleRoot.setFocusCycleRoot(true);
        innerCycleRoot.setFocusTraversalPolicy(policy);
        JButton innerButton1 = createButton("1");
        JButton innerButton2 = createButton("2");
        JButton innerButton3 = createButton("3");
        innerCycleRoot.add(innerButton2);
        innerCycleRoot.add(innerButton3);
        innerCycleRoot.add(innerButton1);
        cycleRoot.add(innerCycleRoot);
        frame.getContentPane().add(cycleRoot);
        cycleRoot.setFocusCycleRoot(true);
        cycleRoot.setFocusable(false);
        frame.setVisible(true);
        SwingWaitTestCase.isRealized(frame);
        assertEquals(innerCycleRoot, policy.getComponentAfter(cycleRoot, button4));
        assertEquals(innerButton1, policy.getComponentAfter(cycleRoot, innerCycleRoot));
        assertEquals(innerButton1, policy.getComponentAfter(innerCycleRoot, innerCycleRoot));
        assertEquals(innerButton2, policy.getComponentAfter(innerCycleRoot, innerButton1));
        assertEquals(innerButton3, policy.getComponentAfter(innerCycleRoot, innerButton2));
        assertEquals(innerCycleRoot, policy.getComponentAfter(innerCycleRoot, innerButton3));
        assertEquals(innerButton1, policy.getComponentAfter(innerCycleRoot, innerCycleRoot));
        assertEquals(innerCycleRoot, policy.getComponentBefore(innerCycleRoot, innerButton1));
        assertEquals(innerButton3, policy.getComponentBefore(innerCycleRoot, innerCycleRoot));
        assertEquals(button4, policy.getComponentBefore(cycleRoot, innerCycleRoot));
        policy.setImplicitDownCycleTraversal(false);
        assertEquals(innerCycleRoot, policy.getComponentAfter(cycleRoot, button4));
        assertEquals(button1, policy.getComponentAfter(cycleRoot, innerCycleRoot));
        assertEquals(innerCycleRoot, policy.getComponentBefore(cycleRoot, button1));
        assertEquals(button4, policy.getComponentBefore(cycleRoot, innerCycleRoot));
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

    private JPanel createNotCycleRootPanel() {
        JPanel result = createPanelWithButtons();
        frame.getContentPane().add(result);
        result.setFocusCycleRoot(false);
        result.setFocusable(false);
        frame.setVisible(true);
        SwingWaitTestCase.isRealized(frame);
        return result;
    }

    private JButton createButton(final String name) {
        JButton result = new JButton(name);
        result.setName(name);
        return result;
    }

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
