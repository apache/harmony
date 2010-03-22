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

import java.awt.Frame.AccessibleAWTFrame;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

import junit.framework.TestCase;

/**
 * AccessibleAWTFrameTest
 */
public class AccessibleAWTFrameTest extends TestCase {

    AccessibleContext ac;
    private Frame frame;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new Frame();
        ac = frame.getAccessibleContext();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if ((frame != null) && frame.isDisplayable()) {
            frame.dispose();
        }
    }

    public final void testGetAccessibleRole() {
        assertSame(AccessibleRole.FRAME, ac.getAccessibleRole());
    }

    public final void testGetAccessibleStateSet() {
        AccessibleStateSet aStateSet = ac.getAccessibleStateSet();
        assertFalse("accessible frame is active",
                   aStateSet.contains(AccessibleState.ACTIVE));
        assertTrue("accessible frame is resizable",
                   aStateSet.contains(AccessibleState.RESIZABLE));
        frame.setResizable(false);
        aStateSet = ac.getAccessibleStateSet();
        assertFalse("accessible frame is NOT resizable",
                   aStateSet.contains(AccessibleState.RESIZABLE));

    }

    public final void testAccessibleAWTFrame() {
        assertTrue(ac instanceof AccessibleAWTFrame);
    }
}
