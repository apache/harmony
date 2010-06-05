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
 * @author Anton Avtamonov, Alexander T. Simbirtsev
 */
package javax.swing.event;

import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import javax.swing.BasicSwingTestCase;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class AncestorEventTest extends BasicSwingTestCase {
    protected static class TestAncestorListener extends EventsController implements
            AncestorListener {
        private static final long serialVersionUID = 1L;

        public TestAncestorListener() {
            super(false);
        }

        public TestAncestorListener(final boolean verbose) {
            super(verbose);
        }

        public void ancestorAdded(final AncestorEvent e) {
            addEvent(new Integer(getNumEvents()), e);
            if (isVerbose()) {
                System.out.println(">>>>>>> added: owner=" + getName(e.getComponent())
                        + ", ancestor=" + getName(e.getAncestor()) + ", ancestorParent="
                        + getName(e.getAncestorParent()));
            }
        }

        public void ancestorMoved(final AncestorEvent e) {
            addEvent(new Integer(getNumEvents()), e);
            if (isVerbose()) {
                System.out.println(">>>>>>> moved: owner=" + getName(e.getComponent())
                        + ", ancestor=" + getName(e.getAncestor()) + ", ancestorParent="
                        + getName(e.getAncestorParent()));
            }
        }

        public void ancestorRemoved(final AncestorEvent e) {
            addEvent(new Integer(getNumEvents()), e);
            if (isVerbose()) {
                System.out.println(">>>>>>> removed: owner=" + getName(e.getComponent())
                        + ", ancestor=" + getName(e.getAncestor()) + ", ancestorParent="
                        + getName(e.getAncestorParent()));
            }
        }

        @Override
        public void reset() {
            super.reset();
            if (isVerbose()) {
                System.out.println("listener's been reset");
            }
        }

        public AncestorEvent getEvent() {
            return (AncestorEvent) getLastEvent();
        }

        public int getEventType() {
            return getEvent().getID();
        }

        private String getName(final Object obj) {
            return obj != null ? obj.getClass().getName() : "null";
        }
    }

    private JPanel ancestor;

    private JButton component;

    private TestAncestorListener listener;

    private JFrame frame;

    @Override
    protected void setUp() throws Exception {
        ancestor = new JPanel();
        component = new JButton();
        ancestor.add(component);
        listener = new TestAncestorListener();
        component.addAncestorListener(listener);
        waitForIdle();
    }

    @Override
    protected void tearDown() throws Exception {
        listener.setVerbose(false);
        ancestor = null;
        component = null;
        listener = null;
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public void testAncestorEvent() throws Exception {
        int id = 123;
        JComponent source = new JPanel();
        AncestorEvent event = new AncestorEvent(source, id, null, null);
        assertEquals(id, event.getID());
        assertEquals(source, event.getSource());
    }

    public void testGetComponent() throws Exception {
        AncestorEvent event = new AncestorEvent(component, 0, null, null);
        assertEquals(component, event.getComponent());
    }

    public void testGetAncestor() throws Exception {
        AncestorEvent event = new AncestorEvent(component, 0, ancestor, null);
        assertEquals(ancestor, event.getAncestor());
    }

    public void testGetAncestorParent() throws Exception {
        AncestorEvent event = new AncestorEvent(component, 0, null, ancestor);
        assertEquals(ancestor, event.getAncestorParent());
    }

    public void testAncestorAddedEventWhenAncestorVisible() throws Exception {
        frame = createVisibleFrameWithAncestor();
        ancestor.setVisible(false);
        component.setVisible(true);
        waitForIdle();
        listener.reset();
        ancestor.setVisible(true);
        waitForIdle();
        assertTrue(listener.getNumEvents() >= 1);
        performChecksInQueue(ancestor, frame.getContentPane(), AncestorEvent.ANCESTOR_ADDED);
    }

    public void testAncestorAddedEventWhenComponentVisible() throws Exception {
        frame = createVisibleFrameWithAncestor();
        ancestor.setVisible(true);
        component.setVisible(false);
        waitForIdle();
        listener.reset();
        component.setVisible(true);
        waitForIdle();
        assertTrue(listener.getNumEvents() >= 1);
        performChecksInQueue(component, ancestor, AncestorEvent.ANCESTOR_ADDED);
    }

    public void testAncestorAddedEventWhenInvisibleComponentAncestorVisible() throws Exception {
        frame = createVisibleFrameWithAncestor();
        ancestor.setVisible(false);
        component.setVisible(false);
        waitForIdle();
        listener.reset();
        ancestor.setVisible(true);
        waitForIdle();
        assertEquals(0, listener.getNumEvents());
    }

    public void testAncestorAddedEventWhenInvisibleAncestorComponentVisible() throws Exception {
        frame = createVisibleFrameWithAncestor();
        ancestor.setVisible(false);
        component.setVisible(false);
        waitForIdle();
        listener.reset();
        component.setVisible(true);
        waitForIdle();
        assertEquals(0, listener.getNumEvents());
    }

    public void testAncestorAddedEventWhenComponentAdded() throws Exception {
        frame = createVisibleFrameWithAncestor();
        ancestor.remove(component);
        waitForIdle();
        listener.reset();
        ComponentListener compListener = addComponentListener(component);
        ancestor.add(component);
        waitForIdle();
        synchronized (compListener) {
            compListener.wait(1000);
        }
        assertTrue(listener.getNumEvents() >= 1);
        performChecksInQueue(component, ancestor, AncestorEvent.ANCESTOR_ADDED);
    }

    public void testAncestorAddedEventWhenAncestorAdded() throws Exception {
        frame = createVisibleFrameWithAncestor();
        waitForIdle();
        assertTrue(listener.getNumEvents() >= 3);
        performChecksInQueue(ancestor.getRootPane(), frame, AncestorEvent.ANCESTOR_MOVED);
        performChecksInQueue(component, ancestor, AncestorEvent.ANCESTOR_MOVED);
        if (!isHarmony()) {
            performChecksInQueue(ancestor, frame.getContentPane(), AncestorEvent.ANCESTOR_ADDED);
        } else {
            performChecksInQueue(frame, null, AncestorEvent.ANCESTOR_ADDED);
        }
    }

    public void testAncestorMovedEventWhenAncestorInvisibleAncestorMoved() throws Exception {
        frame = createVisibleFrameWithAncestor();
        ancestor.setBounds(10, 10, 10, 10);
        ancestor.setVisible(false);
        waitForIdle();
        listener.reset();
        ComponentListener compListener = addComponentListener(component);
        ancestor.setBounds(20, 20, 20, 20);
        waitForIdle();
        synchronized (compListener) {
            compListener.wait(1000);
        }
        assertTrue(listener.getNumEvents() >= 1);
        performChecksInQueue(ancestor, frame.getContentPane(), AncestorEvent.ANCESTOR_MOVED);
    }

    public void testAncestorMovedEventWhenComponentInvisibleAncestorMoved() throws Exception {
        frame = createVisibleFrameWithAncestor();
        ancestor.setBounds(10, 10, 10, 10);
        component.setVisible(false);
        waitForIdle();
        listener.reset();
        ComponentListener compListener = addComponentListener(component);
        ancestor.setBounds(20, 20, 20, 20);
        waitForIdle();
        synchronized (compListener) {
            compListener.wait(1000);
        }
        assertEquals(0, listener.getNumEvents());
    }

    public void testAncestorMovedEventWhenAncestorInvisibleComponentMoved() throws Exception {
        frame = createVisibleFrameWithAncestor();
        component.setBounds(10, 10, 10, 10);
        ancestor.setVisible(false);
        waitForIdle();
        listener.reset();
        ComponentListener compListener = addComponentListener(component);
        component.setBounds(20, 20, 20, 20);
        waitForIdle();
        synchronized (compListener) {
            compListener.wait(1000);
        }
        assertTrue(listener.getNumEvents() >= 1);
        performChecksInQueue(component, ancestor, AncestorEvent.ANCESTOR_MOVED);
    }

    public void testAncestorMovedEventWhenComponentInvisibleComponentMoved() throws Exception {
        frame = createVisibleFrameWithAncestor();
        component.setBounds(10, 10, 10, 10);
        component.setVisible(false);
        waitForIdle();
        listener.reset();
        ComponentListener compListener = addComponentListener(component);
        component.setBounds(20, 20, 20, 20);
        waitForIdle();
        synchronized (compListener) {
            compListener.wait(1000);
        }
        assertEquals(1, listener.getNumEvents());
        performChecks(component, ancestor, AncestorEvent.ANCESTOR_MOVED);
    }

    public void testAncestorMovedEventWhenAncestorMoved() throws Exception {
        frame = createVisibleFrameWithAncestor();
        ancestor.setBounds(10, 10, 10, 10);
        waitForIdle();
        listener.reset();
        ComponentListener compListener = addComponentListener(component);
        ancestor.setBounds(20, 20, 20, 20);
        waitForIdle();
        synchronized (compListener) {
            compListener.wait(1000);
        }
        assertTrue(listener.getNumEvents() >= 1);
        performChecksInQueue(ancestor, frame.getContentPane(), AncestorEvent.ANCESTOR_MOVED);
    }

    public void testAncestorMovedEventWhenComponentMoved() throws Exception {
        component.setSize(200, 200);
        waitForIdle();
        assertEquals(0, listener.getNumEvents());
        ComponentListener compListener = addComponentListener(component);
        component.setLocation(20, 20);
        waitForIdle();
        synchronized (compListener) {
            compListener.wait(1000);
        }
        assertEquals(1, listener.getNumEvents());
        performChecks(component, ancestor, AncestorEvent.ANCESTOR_MOVED);
    }

    public void testAncestorRemovedEventWhenComponentInvisible() throws Exception {
        frame = createVisibleFrameWithAncestor();
        waitForIdle();
        listener.reset();
        component.setVisible(false);
        waitForIdle();
        assertEquals(1, listener.getNumEvents());
        performChecks(component, ancestor, AncestorEvent.ANCESTOR_REMOVED);
    }

    public void testAncestorRemovedEventWhenComponentRemoved() throws Exception {
        frame = createVisibleFrameWithAncestor();
        waitForIdle();
        listener.reset();
        ancestor.remove(component);
        waitForIdle();
        assertEquals(1, listener.getNumEvents());
        performChecks(component, ancestor, AncestorEvent.ANCESTOR_REMOVED);
    }

    public void testAncestorRemovedEventWhenAncestorInvisible() throws Exception {
        frame = createVisibleFrameWithAncestor();
        waitForIdle();
        listener.reset();
        ancestor.setVisible(false);
        waitForIdle();
        assertTrue(listener.getNumEvents() >= 1);
        performChecksInQueue(ancestor, frame.getContentPane(), AncestorEvent.ANCESTOR_REMOVED);
    }

    public void testAncestorRemovedEventWhenAncestorRemoved() throws Exception {
        frame = createVisibleFrameWithAncestor();
        waitForIdle();
        listener.reset();
        frame.getContentPane().remove(ancestor);
        waitForIdle();
        assertEquals(1, listener.getNumEvents());
        if (isHarmony()) {
            performChecks(ancestor, frame.getContentPane(), AncestorEvent.ANCESTOR_REMOVED);
        }
    }

    private JFrame createVisibleFrameWithAncestor() {
        JFrame result = new JFrame();
        result.getContentPane().add(ancestor);
        result.setSize(200, 200);
        result.setVisible(true);
        return result;
    }

    private void performChecks(final Component ancestor, final Component ancestorParent,
            final int eventType) {
        final AncestorEvent event = listener.getEvent();
        performChecks(event, ancestor, ancestorParent, eventType);
    }

    private void performChecksInQueue(final Component ancestor, final Component ancestorParent,
            final int eventType) {
        AncestorEvent event = null;
        for (int i = 0; i < listener.getNumEvents(); i++) {
            final Integer key = new Integer(i);
            AncestorEvent e = (AncestorEvent) listener.getEvent(key);
            if (e != null && e.getID() == eventType && e.getAncestor() == ancestor) {
                event = e;
                break;
            }
        }
        assertNotNull("no event is found for given parameters", event);
        performChecks(event, ancestor, ancestorParent, eventType);
    }

    private void performChecks(final AncestorEvent event, final Component ancestor,
            final Component ancestorParent, final int eventType) {
        assertNotNull(event);
        assertEquals("source", component, event.getSource());
        assertEquals("component", component, event.getComponent());
        assertEquals("eventType", eventType, event.getID());
        assertEquals("ancestor", ancestor, event.getAncestor());
        assertEquals("ancestorParent", ancestorParent, event.getAncestorParent());
    }

    private ComponentListener addComponentListener(final Component c) {
        final ComponentListener compListener = new ComponentListener() {
            public void componentMoved(final ComponentEvent e) {
                synchronized (this) {
                    notifyAll();
                }
            }

            public void componentResized(final ComponentEvent e) {
            }

            public void componentShown(final ComponentEvent e) {
            }

            public void componentHidden(final ComponentEvent e) {
            }
        };
        c.addComponentListener(compListener);
        return compListener;
    }
}
