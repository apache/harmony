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
 * @author Sergey Burlak
 */
package javax.swing;

public class ToolTipManagerTest extends SwingTestCase {
    ToolTipManager m = ToolTipManager.sharedInstance();

    public void testSharedInstance() throws Exception {
        assertNotNull(ToolTipManager.sharedInstance());
        assertEquals(ToolTipManager.sharedInstance(), ToolTipManager.sharedInstance());
    }

    public void testSetGetReshowDelay() throws Exception {
        assertEquals(500, m.getReshowDelay());
        m.setReshowDelay(30);
        assertEquals(30, m.getReshowDelay());
        m.setReshowDelay(0);
        assertEquals(0, m.getReshowDelay());
        try {
            m.setReshowDelay(-50);
            fail("illegal argumant exception shall be thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testSetGetInitialDelay() throws Exception {
        assertEquals(750, m.getInitialDelay());
        m.setInitialDelay(30);
        assertEquals(30, m.getInitialDelay());
        m.setInitialDelay(0);
        assertEquals(0, m.getInitialDelay());
        try {
            m.setInitialDelay(-50);
            fail("illegal argumant exception shall be thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testSetGetDismissDelay() throws Exception {
        assertEquals(4000, m.getDismissDelay());
        m.setDismissDelay(30);
        assertEquals(30, m.getDismissDelay());
        m.setDismissDelay(0);
        assertEquals(0, m.getDismissDelay());
        try {
            m.setDismissDelay(-50);
            fail("illegal argumant exception shall be thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testLightWeightPopupEnabled() throws Exception {
        assertTrue(m.isLightWeightPopupEnabled());
        assertTrue(m.lightWeightPopupEnabled);
        m.setLightWeightPopupEnabled(false);
        assertFalse(m.isLightWeightPopupEnabled());
        assertFalse(m.lightWeightPopupEnabled);
    }

    public void testHeavyWeightPopupEnabled() throws Exception {
        assertFalse(m.heavyWeightPopupEnabled);
    }

    public void testSetGetEnabled() throws Exception {
        assertTrue(m.isEnabled());
        m.setLightWeightPopupEnabled(true);
        m.setEnabled(false);
        assertFalse(m.isEnabled());
        assertTrue(m.isLightWeightPopupEnabled());
        assertFalse(m.heavyWeightPopupEnabled);
    }

    public void testRegisterUnregisterComponent() throws Exception {
        JPanel panel = new JPanel();
        m.registerComponent(panel);
        m.unregisterComponent(panel);
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                m.registerComponent(null);
            }
        });
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                m.unregisterComponent(null);
            }
        });
    }
}
