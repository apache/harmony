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
package javax.swing.plaf.basic;

import java.awt.AWTError;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingTestCase;

public class DefaultMenuLayoutTest extends SwingTestCase {
    protected BoxLayout layout = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.plaf.basic.DefaultMenuLayout.preferredLayoutSize(Container)'
     */
    public void testPreferredLayoutSize() {
        JComponent container1 = new JPanel();
        JComponent component11 = new JPanel();
        JComponent component21 = new JPanel();
        JComponent component31 = new JPanel();
        BoxLayout layout1 = new DefaultMenuLayout(container1, BoxLayout.X_AXIS);
        BoxLayout layout2 = new DefaultMenuLayout(container1, BoxLayout.Y_AXIS);
        component11.setMinimumSize(new Dimension(5, 5));
        component21.setMinimumSize(new Dimension(6, 6));
        component31.setMinimumSize(new Dimension(7, 7));
        component11.setPreferredSize(new Dimension(50, 50));
        component21.setPreferredSize(new Dimension(60, 60));
        component31.setPreferredSize(new Dimension(70, 70));
        container1.add(component11);
        container1.add(component21);
        assertEquals(new Dimension(110, 60), layout1.preferredLayoutSize(container1));
        assertEquals(new Dimension(60, 110), layout2.preferredLayoutSize(container1));
        layout1.invalidateLayout(container1);
        layout2.invalidateLayout(container1);
        container1.add(component31);
        assertEquals(new Dimension(180, 70), layout1.preferredLayoutSize(container1));
        assertEquals(new Dimension(70, 180), layout2.preferredLayoutSize(container1));
    }

    /*
     * Test method for 'javax.swing.plaf.basic.DefaultMenuLayout.DefaultMenuLayout(Container, int)'
     */
    public void testDefaultMenuLayout() {
        Container container = new JPanel();
        boolean thrown = false;
        String text = null;
        try {
            layout = new DefaultMenuLayout(container, BoxLayout.LINE_AXIS);
        } catch (AWTError e) {
            thrown = true;
        }
        assertFalse("No exception thrown", thrown);
        thrown = false;
        text = null;
        try {
            layout = new DefaultMenuLayout(container, 300);
        } catch (AWTError e) {
            thrown = true;
            text = e.getMessage();
        }
        assertTrue("AWTError exception thrown", thrown);
        assertEquals(text, "Invalid axis");
        thrown = false;
        text = null;
        try {
            layout = new BoxLayout(null, BoxLayout.Y_AXIS);
        } catch (AWTError e) {
            thrown = true;
        }
        assertFalse("No exception thrown", thrown);
    }
}
