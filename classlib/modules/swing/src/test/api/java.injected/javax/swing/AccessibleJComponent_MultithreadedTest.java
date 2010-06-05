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
 * Created on 07.10.2004

 */
package javax.swing;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.InvocationTargetException;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;

public class AccessibleJComponent_MultithreadedTest extends BasicSwingTestCase {
    protected JComponent panel;

    protected JFrame frame;

    protected JComponent.AccessibleJComponent aContext;

    protected class ConcreteFocusListener implements FocusListener {
        public boolean state = false;

        public void focusGained(final FocusEvent arg0) {
            state = true;
        }

        public void focusLost(final FocusEvent arg0) {
            state = true;
        }
    };

    protected class SynchronizedPropertyListener extends PropertyChangeController {
        private static final long serialVersionUID = 1L;

        public final Object lock = new Object();
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JFrame();
        panel = new JPanel();
        aContext = (JComponent.AccessibleJComponent) panel.getAccessibleContext();
    }

    @Override
    protected void tearDown() throws Exception {
        panel = null;
        aContext = null;
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
        super.tearDown();
    }

    protected void waitListener(final SynchronizedPropertyListener listener) {
        synchronized (listener.lock) {
            if (!listener.isChanged()) {
                try {
                    listener.lock.wait(1000);
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            }
        }
    }

    /*
     * Class under test for void removePropertyChangeListener(PropertyChangeListener)
     */
    @SuppressWarnings("deprecation")
    public void testRemovePropertyChangeListenerPropertyChangeListener()
            throws InterruptedException, InvocationTargetException {
        SynchronizedPropertyListener listener1 = new SynchronizedPropertyListener();
        SynchronizedPropertyListener listener2 = new SynchronizedPropertyListener();
        aContext.addPropertyChangeListener(listener1);
        JComponent button1 = new JPanel();
        JComponent button2 = new JPanel();
        panel.add(button1);
        waitListener(listener1);
        listener1.checkLastPropertyFired(aContext, AccessibleContext.ACCESSIBLE_CHILD_PROPERTY,
                null, button1.getAccessibleContext());
        listener1.reset();
        aContext.addPropertyChangeListener(listener2);
        aContext.removePropertyChangeListener(listener1);
        panel.add(button2);
        waitListener(listener2);
        assertFalse(listener1.isChanged());
        listener2.checkLastPropertyFired(aContext, AccessibleContext.ACCESSIBLE_CHILD_PROPERTY,
                null, button2.getAccessibleContext());
        listener1.reset();
        listener2.reset();
        ConcreteFocusListener focusListener = new ConcreteFocusListener();
        AccessibleContext bContext = button1.getAccessibleContext();
        bContext.addPropertyChangeListener(listener1);
        frame.getContentPane().add(panel);
        panel.add(button2);
        button1.addFocusListener(focusListener);
        listener1.reset();
        frame.pack();
        frame.show();
        SwingWaitTestCase.requestFocusInWindowForComponent(button1);
        listener1.checkLastPropertyFired(button1.getAccessibleContext(),
                AccessibleContext.ACCESSIBLE_STATE_PROPERTY, null, AccessibleState.FOCUSED);
        listener1.reset();
        bContext.removePropertyChangeListener(listener1);
        SwingWaitTestCase.requestFocusInWindowForComponent(button2);
        assertFalse(listener1.isChanged());
    }

    /*
     * Class under test for void addPropertyChangeListener(PropertyChangeListener)
     */
    @SuppressWarnings("deprecation")
    public void testAddPropertyChangeListenerPropertyChangeListener()
            throws InterruptedException, InvocationTargetException {
        ConcreteFocusListener focusListener = new ConcreteFocusListener();
        SynchronizedPropertyListener listener = new SynchronizedPropertyListener();
        aContext.addPropertyChangeListener(listener);
        JComponent button1 = new JPanel();
        JComponent button2 = new JPanel();
        JComponent button3 = new JPanel();
        panel.add(button1);
        waitListener(listener);
        listener.checkLastPropertyFired(aContext, AccessibleContext.ACCESSIBLE_CHILD_PROPERTY,
                null, button1.getAccessibleContext());
        listener.reset();
        panel.add(button2);
        waitListener(listener);
        listener.checkLastPropertyFired(aContext, AccessibleContext.ACCESSIBLE_CHILD_PROPERTY,
                null, button2.getAccessibleContext());
        listener.reset();
        panel.remove(button1);
        waitListener(listener);
        listener.checkLastPropertyFired(aContext, AccessibleContext.ACCESSIBLE_CHILD_PROPERTY,
                button1.getAccessibleContext(), null);
        listener.reset();
        panel.add(button1);
        AccessibleContext bContext = button1.getAccessibleContext();
        bContext.addPropertyChangeListener(listener);
        frame.getContentPane().add(panel);
        panel.add(button2);
        button1.addFocusListener(focusListener);
        listener.reset();
        frame.pack();
        frame.show();
        SwingWaitTestCase.requestFocusInWindowForComponent(button1);
        listener.checkLastPropertyFired(button1.getAccessibleContext(),
                AccessibleContext.ACCESSIBLE_STATE_PROPERTY, null, AccessibleState.FOCUSED);
        listener.reset();
        SwingWaitTestCase.requestFocusInWindowForComponent(button2);
        listener.checkLastPropertyFired(button1.getAccessibleContext(),
                AccessibleContext.ACCESSIBLE_STATE_PROPERTY, AccessibleState.FOCUSED, null);
        listener.reset();
        panel.add(button3);
        waitListener(listener);
        listener.checkLastPropertyFired(aContext, AccessibleContext.ACCESSIBLE_CHILD_PROPERTY,
                null, button3.getAccessibleContext());
        listener.reset();
        panel.remove(button2);
        waitListener(listener);
        listener.checkLastPropertyFired(aContext, AccessibleContext.ACCESSIBLE_CHILD_PROPERTY,
                button2.getAccessibleContext(), null);
        listener.reset();
        SwingWaitTestCase.requestFocusInWindowForComponent(button1);
        listener.checkLastPropertyFired(button1.getAccessibleContext(),
                AccessibleContext.ACCESSIBLE_STATE_PROPERTY, null, AccessibleState.FOCUSED);
    }

    /*
     * Class under test for AccessibleStateSet getAccessibleStateSet()
     */
    @SuppressWarnings("deprecation")
    public void testGetAccessibleStateSet() throws InterruptedException,
            InvocationTargetException {
        assertTrue("Enabled", aContext.getAccessibleStateSet()
                .contains(AccessibleState.ENABLED));
        assertFalse("Focused", aContext.getAccessibleStateSet().contains(
                AccessibleState.FOCUSED));
        assertTrue("Focusable", aContext.getAccessibleStateSet().contains(
                AccessibleState.FOCUSABLE));
        assertTrue("Visible", aContext.getAccessibleStateSet()
                .contains(AccessibleState.VISIBLE));
        assertFalse("Showing", aContext.getAccessibleStateSet().contains(
                AccessibleState.SHOWING));
        assertTrue("Opaque", aContext.getAccessibleStateSet().contains(AccessibleState.OPAQUE));
        assertEquals("Number of states", 4, aContext.getAccessibleStateSet().toArray().length);
        frame.getContentPane().add(panel);
        frame.pack();
        frame.show();
        SwingWaitTestCase.requestFocusInWindowForComponent(panel);
        assertTrue("Enabled", aContext.getAccessibleStateSet()
                .contains(AccessibleState.ENABLED));
        assertTrue("Focused", aContext.getAccessibleStateSet()
                .contains(AccessibleState.FOCUSED));
        assertTrue("Focusable", aContext.getAccessibleStateSet().contains(
                AccessibleState.FOCUSABLE));
        assertTrue("Visible", aContext.getAccessibleStateSet()
                .contains(AccessibleState.VISIBLE));
        assertTrue("Showing", aContext.getAccessibleStateSet()
                .contains(AccessibleState.SHOWING));
        assertTrue("Opaque", aContext.getAccessibleStateSet().contains(AccessibleState.OPAQUE));
        assertEquals("Number of states", 6, aContext.getAccessibleStateSet().toArray().length);
        panel.setVisible(false);
        panel.setOpaque(false);
        panel.setEnabled(false);
        JButton button = new JButton();
        frame.getContentPane().add(button);
        SwingWaitTestCase.requestFocusInWindowForComponent(button);
        assertFalse("Enabled", aContext.getAccessibleStateSet().contains(
                AccessibleState.ENABLED));
        assertFalse("Focused", aContext.getAccessibleStateSet().contains(
                AccessibleState.FOCUSED));
        assertTrue("Focusable", aContext.getAccessibleStateSet().contains(
                AccessibleState.FOCUSABLE));
        assertFalse("Visible", aContext.getAccessibleStateSet().contains(
                AccessibleState.VISIBLE));
        assertFalse("Showing", aContext.getAccessibleStateSet().contains(
                AccessibleState.SHOWING));
        assertFalse("Opaque", aContext.getAccessibleStateSet().contains(AccessibleState.OPAQUE));
        assertEquals("Number of states", 1, aContext.getAccessibleStateSet().toArray().length);
    }
}
