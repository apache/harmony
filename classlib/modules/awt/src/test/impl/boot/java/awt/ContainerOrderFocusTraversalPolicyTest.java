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
 * @author Dmitry A. Durnev
 */
package java.awt;

import junit.framework.TestCase;
@SuppressWarnings("serial")
public class ContainerOrderFocusTraversalPolicyTest extends TestCase {
    Frame frame;
    SimpleComponent comp1, comp2, comp3;
    SimpleContainer cont1;
    ContainerOrderFocusTraversalPolicy policy;
    public class SimpleContainer extends Container {
    }

    public class SimpleComponent extends Component {
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new Frame();
        policy = new ContainerOrderFocusTraversalPolicy();
        comp1 = new SimpleComponent();
        comp2 = new SimpleComponent();
        comp3 = new SimpleComponent();
        cont1 = new SimpleContainer();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (frame != null) {
            frame.dispose();
        }
    }

    public final void testGetComponentAfter() {
        assertNotNull(frame);
        assertNotNull(comp1);
        assertNull(policy.getComponentAfter(frame, frame));
        frame.setVisible(true);
        assertSame(frame, policy.getComponentAfter(frame, frame));
        frame.add(comp1);
        assertSame(comp1, policy.getComponentAfter(frame, frame));
        assertSame(frame, policy.getComponentAfter(frame, comp1));
        comp1.setEnabled(false);
        assertSame(frame, policy.getComponentAfter(frame, comp1));
        assertSame(frame, policy.getComponentAfter(frame, frame));
        assertNotNull(cont1);
        frame.add(cont1);
        assertNotNull(comp2);
        cont1.add(comp2);
        assertSame(frame, policy.getComponentAfter(frame, comp2));
        assertNotNull(comp3);
        frame.add(comp3);
        assertSame(comp3, policy.getComponentAfter(frame, comp2));
        assertSame(cont1, policy.getComponentAfter(frame, frame));
        assertSame(cont1, policy.getComponentAfter(frame, comp1));
        assertSame(comp2, policy.getComponentAfter(frame, cont1));
    }

    public final void testGetComponentBefore() {
        assertNotNull(frame);
        assertNotNull(comp1);
        assertNull(policy.getComponentBefore(frame, frame));
        frame.setVisible(true);
        assertSame(frame, policy.getComponentBefore(frame, frame));
        frame.add(comp1);
        assertSame(comp1, policy.getComponentBefore(frame, frame));
        assertSame(frame, policy.getComponentBefore(frame, comp1));
        comp1.setEnabled(false);
        assertSame(frame, policy.getComponentBefore(frame, comp1));
        assertSame(frame, policy.getComponentBefore(frame, frame));
        assertNotNull(cont1);
        frame.add(cont1);
        assertNotNull(comp2);
        cont1.add(comp2);
        assertNotNull(comp3);
        frame.add(comp3);
        assertSame(comp2, policy.getComponentBefore(frame, comp3));
        assertSame(cont1, policy.getComponentBefore(frame, comp2));
        assertSame(frame, policy.getComponentBefore(frame, cont1));
        assertSame(frame, policy.getComponentBefore(frame, comp1));
        assertSame(comp3, policy.getComponentBefore(frame, frame));
    }

    public final void testGetDefaultComponent() {
        assertNotNull(frame);
        assertSame(policy.getFirstComponent(frame),
                   policy.getDefaultComponent(frame));
    }

    public final void testGetFirstComponent() {
        assertNotNull(frame);
        assertNotNull(comp1);
        frame.add(comp1);
        assertNull(policy.getFirstComponent(frame));
        frame.setVisible(true);
        assertSame(frame, policy.getFirstComponent(frame));
        frame.setFocusable(false);
        assertSame(comp1, policy.getFirstComponent(frame));
        frame.setEnabled(false);
        assertNull(policy.getFirstComponent(frame));
        frame.setEnabled(true);
        assertNotNull(comp2);
        frame.add(comp2);
        comp1.setEnabled(false);
        assertSame(comp2, policy.getFirstComponent(frame));
    }

    public final void testGetLastComponent() {
        assertNotNull(frame);
        assertNotNull(comp1);
        frame.add(comp1);
        assertNull(policy.getLastComponent(frame));
        frame.setVisible(true);
        assertSame(comp1, policy.getLastComponent(frame));
        frame.setFocusable(false);
        assertSame(comp1, policy.getLastComponent(frame));
        frame.setEnabled(false);
        assertNull(policy.getLastComponent(frame));
        frame.setEnabled(true);
        assertNotNull(comp2);
        frame.add(comp2);
        //comp1.setEnabled(false);
        assertSame(comp2, policy.getLastComponent(frame));
        comp2.setVisible(false);
        assertSame(comp1, policy.getLastComponent(frame));
        comp1.setFocusable(false);
        frame.setFocusable(true);
        assertSame(frame, policy.getLastComponent(frame));
    }

    public final void testAccept() {
        assertNotNull(comp1);
        assertFalse(policy.accept(comp1));
        frame.add(comp1);
        assertFalse(policy.accept(comp1));
        frame.setVisible(true);
        assertTrue(policy.accept(comp1));
        comp1.setEnabled(false);
        assertFalse(policy.accept(comp1));
        comp1.setEnabled(true);
        assertTrue(policy.accept(comp1));
        comp1.setFocusable(false);
        assertFalse(policy.accept(comp1));
        comp1.setFocusable(true);
        assertTrue(policy.accept(comp1));
        comp1.setVisible(false);
        assertFalse(policy.accept(comp1));
        comp1.setVisible(true);
        assertTrue(policy.accept(comp1));
    }

    public final void testGetImplicitDownCycleTraversal() {
        assertTrue(policy.getImplicitDownCycleTraversal());
        assertNotNull(comp1);
        assertNotNull(cont1);
        cont1.add(comp1);
        frame.add(cont1);
        frame.setVisible(true);
        cont1.setFocusCycleRoot(true);
        cont1.setFocusTraversalPolicy(policy);
        Component expectedComp = policy.getDefaultComponent(cont1);
        assertSame(expectedComp, policy.getComponentAfter(frame, cont1));
    }

    public final void testSetImplicitDownCycleTraversal() {
        policy.setImplicitDownCycleTraversal(false);
        assertFalse(policy.getImplicitDownCycleTraversal());
        assertNotNull(comp1);
        assertNotNull(cont1);
        cont1.add(comp1);
        frame.add(cont1);
        frame.setVisible(true);
        cont1.setFocusCycleRoot(true);
        cont1.setFocusTraversalPolicy(policy);
        Component expectedComp = frame;
        assertSame(expectedComp, policy.getComponentAfter(frame, cont1));
    }

}
