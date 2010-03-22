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

import java.awt.EventQueue;
import java.awt.Toolkit;
import org.apache.harmony.x.swing.StringConstants;

public class JComponent_AddRemoveNotifyTest extends BasicSwingTestCase {
    private JPanel ancestorParent;

    private JPanel ancestor;

    private JButton component;

    private PropertyChangeController listener;

    private JFrame frame;

    @Override
    protected void setUp() throws Exception {
        ancestorParent = new JPanel();
        ancestor = new JPanel();
        component = new JButton();
        ancestor.add(component);
        listener = new PropertyChangeController();
        component.addPropertyChangeListener(StringConstants.ANCESTOR_PROPERTY_NAME, listener);
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

    public void testAddNotifyWhenComponentAdded() throws Exception {
        frame = createVisibleFrameWithAncestor();
        ancestor.remove(component);
        waitEventQueueEmptiness();
        listener.reset();
        ancestor.add(component);
        waitEventQueueEmptiness();
        assertEquals(1, listener.getNumEvents());
        listener.checkLastPropertyFired(component, StringConstants.ANCESTOR_PROPERTY_NAME,
                null, ancestor);
    }

    public void testAddNotifyWhenAncestorAdded() throws Exception {
        frame = createVisibleFrameWithAncestor();
        waitEventQueueEmptiness();
        assertEquals(1, listener.getNumEvents());
        listener.checkLastPropertyFired(component, StringConstants.ANCESTOR_PROPERTY_NAME,
                null, ancestor);
    }

    public void testAddNotifyWhenAncestorParentAdded() throws Exception {
        frame = createVisibleFrameWithAncestorParent();
        waitEventQueueEmptiness();
        assertEquals(1, listener.getNumEvents());
        listener.checkLastPropertyFired(component, StringConstants.ANCESTOR_PROPERTY_NAME,
                null, ancestor);
    }

    public void testRemoveNotifyWhenComponentRemoved() throws Exception {
        frame = createVisibleFrameWithAncestor();
        listener.reset();
        ancestor.remove(component);
        waitEventQueueEmptiness();
        assertEquals(1, listener.getNumEvents());
        listener.checkLastPropertyFired(component, StringConstants.ANCESTOR_PROPERTY_NAME,
                ancestor, null);
    }

    public void testRemoveNotifyWhenAncestorRemoved() throws Exception {
        frame = createVisibleFrameWithAncestor();
        listener.reset();
        frame.getContentPane().remove(ancestor);
        waitEventQueueEmptiness();
        assertEquals(1, listener.getNumEvents());
        listener.checkLastPropertyFired(component, StringConstants.ANCESTOR_PROPERTY_NAME,
                ancestor, null);
    }

    public void testRemoveNotifyWhenAncestorParentRemoved() throws Exception {
        frame = createVisibleFrameWithAncestorParent();
        listener.reset();
        frame.getContentPane().remove(ancestorParent);
        waitEventQueueEmptiness();
        assertEquals(1, listener.getNumEvents());
        listener.checkLastPropertyFired(component, StringConstants.ANCESTOR_PROPERTY_NAME,
                ancestor, null);
    }

    private JFrame createVisibleFrameWithAncestor() {
        JFrame result = new JFrame();
        result.getContentPane().add(ancestor);
        result.setSize(200, 200);
        result.setVisible(true);
        return result;
    }

    private JFrame createVisibleFrameWithAncestorParent() {
        JFrame result = new JFrame();
        ancestorParent.add(ancestor);
        result.getContentPane().add(ancestorParent);
        result.setSize(200, 200);
        result.setVisible(true);
        return result;
    }

    private void waitEventQueueEmptiness() {
        try {
            while (Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent() != null
                    || EventQueue.getCurrentEvent() != null) {
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
        }
    }
}
