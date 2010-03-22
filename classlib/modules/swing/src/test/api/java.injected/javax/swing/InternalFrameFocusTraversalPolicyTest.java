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

import java.awt.Container;
import java.awt.Component;

public class InternalFrameFocusTraversalPolicyTest extends SwingTestCase {
    /*
     * InternalFrameFocusTraversalPolicy is an abstract class.
     * This class implements its abstract methods.
     */
    private class TestInternalFrameFocusTraversalPolicy extends
            InternalFrameFocusTraversalPolicy {
        @Override
        public Component getComponentAfter(final Container aContainer,
                final Component aComponent) {
            return null;
        }

        @Override
        public Component getComponentBefore(final Container aContainer,
                final Component aComponent) {
            return null;
        }

        @Override
        public Component getFirstComponent(final Container aContainer) {
            return null;
        }

        @Override
        public Component getLastComponent(final Container aContainer) {
            return null;
        }

        @Override
        public Component getDefaultComponent(final Container aContainer) {
            if (aContainer == null) {
                throw new IllegalArgumentException();
            }
            return mostRecentFocusOwner;
        }
    }

    private TestInternalFrameFocusTraversalPolicy policy;

    JInternalFrame frame;

    JPanel mostRecentFocusOwner;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        policy = new TestInternalFrameFocusTraversalPolicy();
        frame = new JInternalFrame();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     *
     */
    public InternalFrameFocusTraversalPolicyTest(final String name) {
        super(name);
    }

    /*
     * Class under test for InternalFrameFocusTraversalPolicy()
     */
    public void testInternalFrameFocusTraversalPolicy() {
        // nothing to test
    }

    /*
     * Class under test for Component getInitialComponent(JInternalFrame)
     */
    public void testGetInitialComponent() {
        // no components in the frame, shold return null
        assertNull("null", policy.getInitialComponent(frame));
        // test with 'null' parameter
        boolean ok = false;
        try {
            policy.getInitialComponent((JInternalFrame) null);
        } catch (IllegalArgumentException e) {
            ok = true;
        } finally {
            assertTrue("exception", ok);
        }
        // there are components
        mostRecentFocusOwner = new JPanel();
        frame.getContentPane().add(mostRecentFocusOwner);
        assertTrue("mostRecentFocusOwner",
                policy.getInitialComponent(frame) == mostRecentFocusOwner);
    }
}
