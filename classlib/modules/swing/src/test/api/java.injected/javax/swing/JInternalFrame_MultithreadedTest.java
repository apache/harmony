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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;

/**
 * Tests for JInternalFrame class that cannot be run in Event Dispatch Thread.
 * These are test of focus subsystem mainly.
 *
 * These tests cannot be run on Event Dispatch Thread because we cannot
 * request focus on some component synchronously (auxiliary
 * function requestFocusForComponent() cannot be synchronous).
 *
 */
public class JInternalFrame_MultithreadedTest extends BasicSwingTestCase {
    private JInternalFrame frame;

    // is used in tests where frame.isShowing() must be true
    private JFrame rootFrame;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JInternalFrame();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        if (rootFrame != null) {
            rootFrame.dispose();
            rootFrame = null;
        }
        super.tearDown();
    }

    /**
     * Constructor for JInternalFrameTest.
     * @param name
     */
    public JInternalFrame_MultithreadedTest(final String name) {
        super(name);
    }

    /*
     * Class under test for void restoreSubcomponentFocus()
     */
    public void testRestoreSubcomponentFocus() throws InterruptedException,
            InvocationTargetException {
        final Component comp1 = new JPanel();
        final Component comp2 = new JPanel();
        final Component comp3 = new JPanel();
        frame.getContentPane().add(comp1, BorderLayout.NORTH);
        frame.getContentPane().add(comp2, BorderLayout.SOUTH);
        frame.getContentPane().add(comp3, BorderLayout.CENTER);
        createAndShowRootFrame();
        setSelectedFrame(frame, true);
        SwingWaitTestCase.requestFocusInWindowForComponent(comp2);
        setSelectedFrame(frame, false);
        setSelectedFrame(frame, true);
        assertTrue("focus is restored", frame.getFocusOwner() == comp2);
    }

    /*
     * Creates and shows rootFrame. This method is used when JInternalFrame
     * need to be selected (isSelected() == true) for testing purposes.
     */
    protected void createAndShowRootFrame() {
        frame.setSize(70, 100);
        rootFrame = new JFrame();
        JDesktopPane desktop = new JDesktopPane();
        rootFrame.setContentPane(desktop);
        rootFrame.getContentPane().add(frame);
        rootFrame.setSize(100, 200);
        frame.setVisible(true);
        rootFrame.setVisible(true);
        SwingWaitTestCase.isRealized(rootFrame);
    }

    /*
     * Thread safe function to make the internal frame selected
     * or deselected
     */
    protected void setSelectedFrame(final JInternalFrame frame, final boolean selected) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        frame.setSelected(selected);
                    } catch (PropertyVetoException e) {
                    }
                }
            });
        } catch (Exception e) {
            assertFalse("exception", true);
        }
    }

    /*
     * Class under test for Component getMostRecentFocusOwner()
     */
    public void testGetMostRecentFocusOwner() throws PropertyVetoException,
            InterruptedException, InvocationTargetException {
        final Component initial = new JPanel(); // initial focus component
        final Component def = new JPanel(); // default focus component
        final Component some = new JPanel(); // some another component
        frame.getContentPane().add(initial, BorderLayout.NORTH);
        frame.getContentPane().add(def, BorderLayout.SOUTH);
        frame.getContentPane().add(some, BorderLayout.CENTER);
        assertNull("null by default", frame.getMostRecentFocusOwner());
        class MyFocusTraversalPolicy extends SortingFocusTraversalPolicy {
            Component initial;

            Component def;

            public MyFocusTraversalPolicy() {
                setComparator(new Comparator<Object>() {
                    public int compare(final Object arg0, final Object arg1) {
                        return System.identityHashCode(arg0) - System.identityHashCode(arg1);
                    }
                });
            }

            @Override
            public Component getInitialComponent(final javax.swing.JInternalFrame frame) {
                return initial;
            }

            @Override
            public Component getDefaultComponent(final Container focusCycleRoot) {
                return def;
            }
        }
        MyFocusTraversalPolicy traversal = new MyFocusTraversalPolicy();
        traversal.def = def;
        frame.setFocusTraversalPolicy(traversal);
        if (BasicSwingTestCase.isHarmony()) {
            assertSame("== def (JRockit fails)", def, frame.getMostRecentFocusOwner());
        }
        // no one component had ever the focus, initial is returned
        traversal.initial = initial;
        createAndShowRootFrame();
        assertTrue("== initial", frame.getMostRecentFocusOwner() == initial);
        // request focus for 'some' component, this component must be returned by
        // getMostRecentFocusOwner()
        setSelectedFrame(frame, true);
        SwingWaitTestCase.requestFocusInWindowForComponent(some);
        setSelectedFrame(frame, false);
        assertTrue("== some", frame.getMostRecentFocusOwner() == some);
        // frame is selected, returns the same component as getFocusOwner()
        setSelectedFrame(frame, true);
        SwingWaitTestCase.requestFocusInWindowForComponent(def);
        assertTrue("== getFocusOwner()", frame.getMostRecentFocusOwner() == frame
                .getFocusOwner());
    }

    /*
     * Class under test for Component getFocusOwner()
     */
    public void testGetFocusOwner() throws InterruptedException, InvocationTargetException {
        final Component comp1 = new JPanel();
        final Component comp2 = new JPanel();
        final Component comp3 = new JPanel();
        frame.getContentPane().add(comp1, BorderLayout.NORTH);
        frame.getContentPane().add(comp2, BorderLayout.SOUTH);
        frame.getContentPane().add(comp3, BorderLayout.CENTER);
        assertNull("== null", frame.getFocusOwner());
        createAndShowRootFrame();
        // frame is selected, comp2 has focus
        setSelectedFrame(frame, true);
        SwingWaitTestCase.requestFocusInWindowForComponent(comp2);
        assertSame("== comp2", comp2, frame.getFocusOwner());
        // frame is not selected
        setSelectedFrame(frame, false);
        assertNull("== null", frame.getFocusOwner());
    }
}
