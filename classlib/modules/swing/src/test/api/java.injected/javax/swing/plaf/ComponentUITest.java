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
 * Created on 08.09.2004

 */
package javax.swing.plaf;

import javax.accessibility.Accessible;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingTestCase;

public class ComponentUITest extends SwingTestCase {
    protected ComponentUI componentUI = null;

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(ComponentUITest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        componentUI = new ComponentUI() {
        };
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Constructor for ComponentUITest.
     * @param name
     */
    public ComponentUITest(final String name) {
        super(name);
    }

    // "update" method should be tested by MyComponentTest.testUpdate()
    public void testUpdate() {
    }

    public void testGetPreferredSize() {
        JPanel panel = new JPanel();
        assertNull(componentUI.getPreferredSize(panel));
    }

    public void testGetMinimumSize() {
        JPanel panel = new JPanel();
        assertNull(componentUI.getMinimumSize(panel));
    }

    public void testGetMaximumSize() {
        JPanel panel = new JPanel();
        assertNull(componentUI.getMaximumSize(panel));
    }

    // "contains" method is beeing tested by MyComponentTest.testContainsintint()
    public void testContains() {
    }

    public void testGetAccessibleChild() {
        JPanel panel = new JPanel();
        Accessible child = componentUI.getAccessibleChild(panel, 0);
        assertNull(child);
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        panel.add(panel1);
        panel.add(panel2);
        child = componentUI.getAccessibleChild(panel, 0);
        assertTrue(child == panel1);
        child = componentUI.getAccessibleChild(panel, 1);
        assertTrue(child == panel2);
    }

    public void testGetAccessibleChildrenCount() {
        JPanel panel = new JPanel();
        assertTrue(componentUI.getAccessibleChildrenCount(panel) == 0);
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        panel.add(panel1);
        panel.add(panel2);
        assertTrue(componentUI.getAccessibleChildrenCount(panel) == 2);
    }

    public void testCreateUI() {
        JComponent component = new JPanel();
        boolean isExceptionThrown = false;
        try {
            ComponentUI.createUI(component);
        } catch (Error e) {
            isExceptionThrown = true;
        } finally {
            assertTrue(isExceptionThrown);
        }
    }
}
