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
package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FocusTraversalPolicy;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicLookAndFeel;

public class JComponent_MultithreadedTest extends BasicSwingTestCase {
    protected JComponent panel;

    protected JFrame window;

    class MyInputVerifier extends InputVerifier {
        public boolean invoked = false;

        private boolean returnedValue = true;

        MyInputVerifier() {
        }

        MyInputVerifier(final boolean returnedValue) {
            this.returnedValue = returnedValue;
        }

        @Override
        public boolean verify(final JComponent input) {
            invoked = true;
            return returnedValue;
        }
    }

    class RunnableResulted implements Runnable {
        public boolean result = false;

        public void run() {
            result = true;
        }
    }

    /*
     * Requests focus for the component and waits until it really
     * becomes focused.
     */
    protected boolean requestFocusInWindowForComponent(final JComponent c, int maxWaitTime)
            throws Exception {
        FocusListener listener = addFocusListener(c);
        RunnableResulted thread = new RunnableResulted() {
            @Override
            public void run() {
                result = c.requestFocusInWindow();
            }
        };
        SwingUtilities.invokeAndWait(thread);
        if (!thread.result) {
            return false;
        }
        synchronized (listener) {
            listener.wait(maxWaitTime);
        }
        waitForIdle();
        if (!c.isFocusOwner()) {
            fail();
        }
        return true;
    }

    /*
     * Requests focus for the component and waits until it really
     * becomes focused.
     */
    protected boolean requestFocusInWindowForComponent(final JComponent c, final boolean temporarily,
                                                       int maxWaitTime) throws Exception {
        FocusListener listener = addFocusListener(c);
        RunnableResulted thread = new RunnableResulted() {
            @Override
            public void run() {
                result = c.requestFocusInWindow(temporarily);
            }
        };
        SwingUtilities.invokeAndWait(thread);
        if (!thread.result) {
            return false;
        }
        synchronized (listener) {
            listener.wait(maxWaitTime);
        }
        waitForIdle();
        if (!c.isFocusOwner()) {
            fail();
        }
        return true;
    }

    /*
     * Requests focus for the component and waits until it really
     * becomes focused.
     */
    protected void requestFocusForComponent(final JComponent c, int maxWaitTime) throws Exception {
        FocusListener listener = addFocusListener(c);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                c.requestFocus();
            }
        });
        synchronized (listener) {
            listener.wait(maxWaitTime);
        }
        waitForIdle();
    }

    /*
     * Requests focus for the component and waits until it really
     * becomes focused.
     */
    protected void requestDefaultFocusForComponent(final JComponent c, final Component newFocusOwner,
                                                   int maxWaitTime) throws Exception {
        FocusListener listener = addFocusListener(newFocusOwner);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                c.requestDefaultFocus();
            }
        });
        synchronized (listener) {
            listener.wait(maxWaitTime);
        }
        waitForIdle();
    }

    /*
     * Requests focus for the component and waits until it really
     * becomes focused.
     */
    protected void requestFocusForComponent(final JComponent c, final boolean temporarily,
                                            int maxWaitTime) throws Exception {
        FocusListener listener = addFocusListener(c);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                c.requestFocus(temporarily);
            }
        });

        synchronized (listener) {
            listener.wait(maxWaitTime);
        }
        waitForIdle();
        if (!c.isFocusOwner()) {
            fail();
        }
    }

    /*
     * Requests focus for the component and waits until it really
     * becomes focused.
     */
    protected void grabFocusForComponent(final JComponent c, int maxWaitTime) throws Exception {
        FocusListener listener = addFocusListener(c);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                c.grabFocus();
            }
        });

        synchronized (listener) {
            listener.wait(maxWaitTime);
        }
        waitForIdle();
        if (!c.isFocusOwner()) {
            fail();
        }
    }

    /**
     * Constructor for JComponentTest_Multithreaded.
     */
    public JComponent_MultithreadedTest(final String str) {
        super(str);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        panel = new JPanel();
        window = new JFrame();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        window.dispose();
        super.tearDown();
    }

    /*
     * Class under test for void requestFocus()
     */
    @SuppressWarnings("deprecation")
    public void testRequestFocus() throws Exception {
        MyInputVerifier verifier = new MyInputVerifier();
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        JComponent panel3 = new JPanel();
        JComponent panel4 = new JPanel();
        window.getContentPane().add(panel1);
        window.getContentPane().add(panel2);
        window.getContentPane().add(panel3);
        window.getContentPane().add(panel4);
        window.pack();
        window.show();
        waitForIdle();
        SwingWaitTestCase.requestFocusInWindowForComponent(panel1);
        panel1.setInputVerifier(verifier);
        panel2.setVerifyInputWhenFocusTarget(true);
        requestFocusForComponent(panel2, 1000);
        assertTrue("verifier's invoked ", verifier.invoked);
        assertTrue("focus's gained ", panel2.isFocusOwner());
        verifier.invoked = false;
        requestFocusForComponent(panel1, 1000);
        assertTrue("focus's gained ", panel1.isFocusOwner());
        panel2.setVerifyInputWhenFocusTarget(false);
        requestFocusForComponent(panel2, 1000);
        assertFalse("verifier's not invoked ", verifier.invoked);
        assertTrue("focus's gained ", panel2.isFocusOwner());
    }

    /*
     * Class under test for boolean requestFocusInWindow()
     */
    @SuppressWarnings("deprecation")
    public void testRequestFocusInWindow() throws Exception {
        MyInputVerifier verifier = new MyInputVerifier();
        MyInputVerifier verifier2 = new MyInputVerifier(false);
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        JComponent panel3 = new JPanel();
        JComponent panel4 = new JPanel();
        window.getContentPane().add(panel1);
        window.getContentPane().add(panel2);
        window.getContentPane().add(panel3);
        window.getContentPane().add(panel4);
        window.pack();
        window.show();
        waitForIdle();
        SwingWaitTestCase.requestFocusInWindowForComponent(panel1);
        panel1.setInputVerifier(verifier);
        panel2.setVerifyInputWhenFocusTarget(true);
        assertTrue("focus can be gained ", requestFocusInWindowForComponent(panel2, 1000));
        assertTrue("verifier's invoked ", verifier.invoked);
        assertTrue("focus's gained ", panel2.isFocusOwner());
        verifier.invoked = false;
        assertTrue("focus can be gained ", requestFocusInWindowForComponent(panel1, 1000));
        assertTrue("focus's gained ", panel1.isFocusOwner());
        panel2.setVerifyInputWhenFocusTarget(false);
        assertTrue("focus can be gained ", requestFocusInWindowForComponent(panel2, 1000));
        assertFalse("verifier's not invoked ", verifier.invoked);
        assertTrue("focus's gained ", panel2.isFocusOwner());
        panel1.setVerifyInputWhenFocusTarget(true);
        panel2.setInputVerifier(verifier2);
        assertFalse("focus can be gained ", requestFocusInWindowForComponent(panel1, true, 1000));
        assertTrue("verifier's invoked ", verifier2.invoked);
        assertFalse("focus's gained ", panel1.isFocusOwner());
        verifier.invoked = false;
    }

    /*
     * Class under test for boolean requestFocus(boolean)
     */
    @SuppressWarnings("deprecation")
    public void testRequestFocusboolean() throws Exception {
        MyInputVerifier verifier = new MyInputVerifier();
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        JComponent panel3 = new JPanel();
        JComponent panel4 = new JPanel();
        window.getContentPane().add(panel1);
        window.getContentPane().add(panel2);
        window.getContentPane().add(panel3);
        window.getContentPane().add(panel4);
        window.pack();
        window.show();
        waitForIdle();
        SwingWaitTestCase.requestFocusInWindowForComponent(panel1);
        panel1.setInputVerifier(verifier);
        panel2.setVerifyInputWhenFocusTarget(true);
        requestFocusForComponent(panel2, false, 1000);
        assertTrue("verifier's invoked ", verifier.invoked);
        assertTrue("focus's gained ", panel2.isFocusOwner());
        verifier.invoked = false;
        requestFocusForComponent(panel1, false, 1000);
        assertTrue("focus's gained ", panel1.isFocusOwner());
        panel2.setVerifyInputWhenFocusTarget(false);
        requestFocusForComponent(panel2, false, 1000);
        assertFalse("verifier's not invoked ", verifier.invoked);
        assertTrue("focus's gained ", panel2.isFocusOwner());
        requestFocusForComponent(panel1, false, 1000);
        assertEquals("focus's gained ", true, panel1.isFocusOwner());
        panel2.setVerifyInputWhenFocusTarget(true);
        requestFocusForComponent(panel2, true, 1000);
        assertTrue("verifier's invoked ", verifier.invoked);
        assertTrue("focus's gained ", panel2.isFocusOwner());
        verifier.invoked = false;
        requestFocusForComponent(panel1, true, 1000);
        assertTrue("focus's gained ", panel1.isFocusOwner());
        panel2.setVerifyInputWhenFocusTarget(false);
        requestFocusForComponent(panel2, true, 1000);
        assertFalse("verifier's not invoked ", verifier.invoked);
        assertTrue("focus's gained ", panel2.isFocusOwner());
    }

    /*
     * Class under test for boolean requestFocusInWindow(boolean)
     */
    @SuppressWarnings("deprecation")
    public void testRequestFocusInWindowboolean() throws Exception {
        MyInputVerifier verifier = new MyInputVerifier();
        MyInputVerifier verifier2 = new MyInputVerifier(false);
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        JComponent panel3 = new JPanel();
        JComponent panel4 = new JPanel();
        window.getContentPane().add(panel1);
        window.getContentPane().add(panel2);
        window.getContentPane().add(panel3);
        window.getContentPane().add(panel4);
        window.pack();
        window.show();
        waitForIdle();
        SwingWaitTestCase.requestFocusInWindowForComponent(panel1);
        panel1.setInputVerifier(verifier);
        panel2.setVerifyInputWhenFocusTarget(true);
        assertTrue("focus can be gained ", requestFocusInWindowForComponent(panel2, false, 1000));
        assertTrue("verifier's invoked ", verifier.invoked);
        assertTrue("focus's gained ", panel2.isFocusOwner());
        verifier.invoked = false;
        assertTrue("focus can be gained ", requestFocusInWindowForComponent(panel1, false, 1000));
        assertTrue("focus's gained ", panel1.isFocusOwner());
        panel2.setVerifyInputWhenFocusTarget(false);
        assertTrue("focus can be gained ", requestFocusInWindowForComponent(panel2, false, 1000));
        assertFalse("verifier's not invoked ", verifier.invoked);
        assertTrue("focus's gained ", panel2.isFocusOwner());
        assertTrue("focus can be gained ", requestFocusInWindowForComponent(panel1, false, 1000));
        assertTrue("focus's gained ", panel1.isFocusOwner());
        panel2.setVerifyInputWhenFocusTarget(true);
        assertTrue("focus can be gained ", requestFocusInWindowForComponent(panel2, true, 1000));
        assertTrue("verifier's invoked ", verifier.invoked);
        assertTrue("focus's gained ", panel2.isFocusOwner());
        verifier.invoked = false;
        assertTrue("focus can be gained ", requestFocusInWindowForComponent(panel1, true, 1000));
        assertTrue("focus's gained ", panel1.isFocusOwner());
        panel2.setVerifyInputWhenFocusTarget(false);
        assertTrue("focus can be gained ", requestFocusInWindowForComponent(panel2, true, 1000));
        assertFalse("verifier's not invoked ", verifier.invoked);
        assertTrue("focus's gained ", panel2.isFocusOwner());
        panel1.setVerifyInputWhenFocusTarget(true);
        panel2.setInputVerifier(verifier2);
        assertFalse("focus can be gained ", requestFocusInWindowForComponent(panel1, true, 1000));
        assertTrue("verifier's invoked ", verifier2.invoked);
        assertFalse("focus's gained ", panel1.isFocusOwner());
        verifier.invoked = false;
    }

    @SuppressWarnings("deprecation")
    public void testGrabFocus() throws Exception {
        MyInputVerifier verifier = new MyInputVerifier();
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        JComponent panel3 = new JPanel();
        JComponent panel4 = new JPanel();
        window.getContentPane().add(panel1);
        window.getContentPane().add(panel2);
        window.getContentPane().add(panel3);
        window.getContentPane().add(panel4);
        window.pack();
        window.show();
        waitForIdle();
        SwingWaitTestCase.requestFocusInWindowForComponent(panel1);
        panel1.setInputVerifier(verifier);
        panel2.setVerifyInputWhenFocusTarget(true);
        grabFocusForComponent(panel2, 1000);
        assertTrue("verifier's invoked ", verifier.invoked);
        assertTrue("focus's gained ", panel2.isFocusOwner());
        verifier.invoked = false;
        grabFocusForComponent(panel1, 1000);
        assertTrue("focus's gained ", panel1.isFocusOwner());
        panel2.setVerifyInputWhenFocusTarget(false);
        grabFocusForComponent(panel2, 1000);
        assertFalse("verifier's not invoked ", verifier.invoked);
        assertTrue("focus's gained ", panel2.isFocusOwner());
    }

    @SuppressWarnings("deprecation")
    public void testRequestDefaultFocus() throws Exception {
        final JComponent panel1 = new JPanel();
        final JComponent panel2 = new JPanel();
        final JComponent panel3 = new JPanel();
        final JComponent panel4 = new JPanel(); // this component is to be returned
                                                // by our FocusTraversalPolicy()
        FocusTraversalPolicy policy = new FocusTraversalPolicy() {
            @Override
            public Component getComponentAfter(final Container a0, final Component a1) {
                return null;
            }

            @Override
            public Component getComponentBefore(final Container a0, final Component a1) {
                return null;
            }

            @Override
            public Component getDefaultComponent(final Container a0) {
                return panel4;
            }

            @Override
            public Component getFirstComponent(final Container a0) {
                return null;
            }

            @Override
            public Component getLastComponent(final Container a0) {
                return null;
            }
        };
        window.getContentPane().add(panel1);
        window.getContentPane().add(panel2);
        window.getContentPane().add(panel3);
        window.getContentPane().add(panel4);
        window.pack();
        window.show();
        waitForIdle();
        requestDefaultFocusForComponent(panel2, window, 100);
        assertTrue("focus's gained ", window.isFocusOwner());
        panel3.setFocusCycleRoot(false);
        panel3.setFocusTraversalPolicy(policy);
        requestDefaultFocusForComponent(panel3, window, 100);
        assertTrue("focus's gained ", window.isFocusOwner());
        panel3.setFocusCycleRoot(true);
        requestDefaultFocusForComponent(panel3, panel4, 100);
        assertTrue("focus's gained ", panel4.isFocusOwner());
        panel3.setFocusCycleRoot(false);
        requestDefaultFocusForComponent(panel3, window, 100);
        assertFalse("focus's gained ", window.isFocusOwner());
    }

    public void testUpdateUI() throws Exception {
        LookAndFeel laf = UIManager.getLookAndFeel();
        try {
            JButton button = new JButton();
            BasicLookAndFeel lookAndFeel1 = new BasicLookAndFeel() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isSupportedLookAndFeel() {
                    return true;
                }

                @Override
                public boolean isNativeLookAndFeel() {
                    return false;
                }

                @Override
                public String getName() {
                    return "BasicLookAndFeel";
                }

                @Override
                public String getID() {
                    return "BasicLookAndFeel";
                }

                @Override
                public String getDescription() {
                    return "BasicLookAndFeel";
                }
            };
            UIManager.setLookAndFeel(lookAndFeel1);
            ComponentUI ui = button.getUI();
            assertTrue("current component's ui is correct ", ui.getClass().getName().endsWith(
                    "MetalButtonUI"));
            button.updateUI();
            ui = button.getUI();
            assertTrue("L&F change affected component's ui ", ui.getClass().getName().endsWith(
                    "BasicButtonUI"));
        } finally {
            UIManager.setLookAndFeel(laf);
        }
    }

    public void testRevalidate() throws Exception {
        final JButton button = new JButton("test");
        final JFrame frame = new JFrame();
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                frame.getContentPane().add(button);
                frame.setVisible(true);
                assertTrue(button.isValid());
                button.revalidate();
                assertFalse(button.isValid());
            }
        });
        waitForIdle();
        EventQueue.invokeAndWait(new Thread());
        assertTrue(button.isValid());
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                frame.dispose();
            }
        });
    }

    @SuppressWarnings("deprecation")
    public void testAddNotify() throws Exception {
        PropertyChangeController listener = new PropertyChangeController();
        JButton panel1 = new JButton();
        JPanel panel2 = new JPanel();
        JPanel panel3 = new JPanel();
        panel1.addPropertyChangeListener(listener);
        window.getContentPane().add(panel2);
        window.getContentPane().add(panel3);
        window.pack();
        window.show();
        waitForIdle();
        SwingWaitTestCase.requestFocusInWindowForComponent(panel2);
        panel2.add(panel1);
        listener.checkPropertyFired(panel1, "ancestor", null, panel2);
        listener.reset();
        panel3.add(panel1);
        listener.checkPropertyFired(panel1, "ancestor", null, panel3);
    }

    @SuppressWarnings("deprecation")
    public void testRemoveNotify() throws Exception {
        PropertyChangeController listener = new PropertyChangeController();
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        JPanel panel3 = new JPanel();
        panel1.addPropertyChangeListener(listener);
        window.getContentPane().add(panel2);
        window.getContentPane().add(panel3);
        window.pack();
        window.show();
        waitForIdle();
        SwingWaitTestCase.requestFocusInWindowForComponent(panel2);
        panel2.add(panel1);
        listener.checkPropertyFired(panel1, "ancestor", null, panel2);
        listener.reset();
        panel3.add(panel1);
        listener.checkPropertyFired(panel1, "ancestor", null, panel3);
    }

    private FocusListener addFocusListener(final Component c) {
        FocusListener listener = new FocusListener() {
            public void focusGained(FocusEvent e) {
                synchronized (this) {
                    notifyAll();
                }
            }

            public void focusLost(FocusEvent e) {
            }
        };
        c.addFocusListener(listener);
        return listener;
    }
}