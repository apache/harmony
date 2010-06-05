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

import java.awt.Window.AccessibleAWTWindow;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

import junit.framework.TestCase;

/**
 * AccessibleAWTWindowTest
 */
public class AccessibleAWTWindowTest extends TestCase {

    private Window window;
    AccessibleContext ac;
    private Frame frame;
    private Robot robot;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new Frame();
        window = new Window(frame);
        ac = window.getAccessibleContext();
        robot = new Robot();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if ((frame != null) && frame.isDisplayable()) {
            frame.dispose();
        }
    }

    public final void testGetAccessibleRole() {
        assertSame(AccessibleRole.WINDOW, ac.getAccessibleRole());
    }

    @SuppressWarnings("deprecation")
    public final void testGetAccessibleStateSet() {
        frame.show();
        window.setFocusable(true);
        window.setVisible(true);
        waitFocus();
        assertTrue(window.isFocusOwner());
        AccessibleStateSet aStateSet = ac.getAccessibleStateSet();
        assertTrue("accessible window is active",
                   aStateSet.contains(AccessibleState.ACTIVE));
        assertTrue("accessible window is showing",
                   aStateSet.contains(AccessibleState.SHOWING));
        assertTrue("accessible window is focusable",
                   aStateSet.contains(AccessibleState.FOCUSABLE));
        assertTrue("accessible window is focused",
                   aStateSet.contains(AccessibleState.FOCUSED));
        assertFalse("accessible window is NOT resizable",
                    aStateSet.contains(AccessibleState.RESIZABLE));
        assertTrue(frame.isActive());
        aStateSet = frame.getAccessibleContext().getAccessibleStateSet();
        assertFalse("accessible frame is NOT active",
                    aStateSet.contains(AccessibleState.ACTIVE));
    }

    public final void testAccessibleAWTWindow() {
        assertTrue(ac instanceof AccessibleAWTWindow);
    }

    private void waitFocus() {
        int time = 0;
        int timeout = 32;
        int threshold = 60000;
        while (!window.isFocused() && (time < threshold)) {
            robot.delay(timeout);
            time += timeout;
            timeout <<= 1;
        }
    }

}
