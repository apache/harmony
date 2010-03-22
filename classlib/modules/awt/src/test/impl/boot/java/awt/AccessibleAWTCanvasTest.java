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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;

import junit.framework.TestCase;

/**
 * AccessibleAWTCanvas
 */
public class AccessibleAWTCanvasTest extends TestCase {
    private Canvas canvas;
    private PropertyChangeListener propListener;
    private PropertyChangeEvent lastPropEvent;
    private Robot robot;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        robot = new Robot();
        canvas = new Canvas();
        lastPropEvent = null;
        propListener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent pce) {
                lastPropEvent = pce;
            }

        };
    }

    public final void testAccessibleAWTCanvas() {
        assertNotNull(canvas);
    }

    public final void testGetAccessibleRole() {
        assertSame(AccessibleRole.CANVAS,
                   canvas.getAccessibleContext().getAccessibleRole());
    }

    public final void testAddPropertyChangeListener() {
        String propName = AccessibleContext.ACCESSIBLE_STATE_PROPERTY;
        canvas.getAccessibleContext().addPropertyChangeListener(propListener);
        assertNull(lastPropEvent);
        Frame f = new Frame();
        f.add(canvas);
        canvas.setFocusable(true); //!
        f.setVisible(true);
        waitForEvent();

        // focus events:
        assertNotNull(lastPropEvent);
        assertEquals(propName, lastPropEvent.getPropertyName());
        assertEquals(AccessibleState.FOCUSED, lastPropEvent.getNewValue());
        assertNull(lastPropEvent.getOldValue());

        // component events:
        lastPropEvent = null;
        canvas.setVisible(false);
        waitForEvent();
        assertNotNull(lastPropEvent);
        assertEquals(propName, lastPropEvent.getPropertyName());
        assertEquals(AccessibleState.VISIBLE, lastPropEvent.getOldValue());
        assertNull(lastPropEvent.getNewValue());
        f.dispose();
    }

    private void waitForEvent() {
        int time = 0;
        int timeout = 16;
        int threshold = 60000;
        while ((lastPropEvent == null) && (time < threshold)) {
            robot.delay(timeout);
            time += timeout;
            timeout <<= 1;
        }
    }
}
