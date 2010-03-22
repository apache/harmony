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
package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.border.Border;

public class DefaultListCellRendererTest extends SwingTestCase {
    private DefaultListCellRenderer renderer;

    public DefaultListCellRendererTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        renderer = new DefaultListCellRenderer();
    }

    @Override
    protected void tearDown() throws Exception {
        renderer = null;
        super.tearDown();
    }

    public void testGetListCellRendererComponent() throws Exception {
        JList list = new JList();
        list.setForeground(Color.red);
        list.setBackground(Color.blue);
        list.setSelectionForeground(Color.pink);
        list.setSelectionBackground(Color.yellow);
        Component result = renderer.getListCellRendererComponent(list, "one", 0, false, false);
        assertTrue(result == renderer);
        JLabel label = (JLabel) result;
        assertEquals("one", label.getText());
        assertEquals(Color.red, label.getForeground());
        assertEquals(Color.blue, label.getBackground());
        assertEquals(DefaultListCellRenderer.noFocusBorder, label.getBorder());
        result = renderer.getListCellRendererComponent(list, "two", 0, true, false);
        assertTrue(result == renderer);
        label = (JLabel) result;
        assertEquals("two", label.getText());
        assertEquals(Color.pink, label.getForeground());
        assertEquals(Color.yellow, label.getBackground());
        assertEquals(DefaultListCellRenderer.noFocusBorder, label.getBorder());
        Border border = BorderFactory.createEtchedBorder();
        DefaultListCellRenderer.noFocusBorder = border;
        result = renderer.getListCellRendererComponent(list, "one", 0, false, false);
        assertTrue(result == renderer);
        label = (JLabel) result;
        assertEquals("one", label.getText());
        assertEquals(Color.red, label.getForeground());
        assertEquals(Color.blue, label.getBackground());
        assertEquals(border, label.getBorder());
        border = BorderFactory.createRaisedBevelBorder();
        UIManager.put("List.focusCellHighlightBorder", border);
        result = renderer.getListCellRendererComponent(list, "three", 0, true, true);
        assertTrue(result == renderer);
        label = (JLabel) result;
        assertEquals("three", label.getText());
        assertEquals(Color.pink, label.getForeground());
        assertEquals(Color.yellow, label.getBackground());
        assertEquals(border, label.getBorder());
    }

    public void testGetListCellRendererComponentChangesAreReflected() throws Exception {
        JList list = new JList();
        list.setForeground(Color.red);
        list.setBackground(Color.blue);
        list.setSelectionForeground(Color.pink);
        list.setSelectionBackground(Color.yellow);
        Component result = renderer.getListCellRendererComponent(list, "one", 0, false, false);
        assertTrue(result == renderer);
        JLabel label = (JLabel) result;
        assertEquals("one", label.getText());
        assertEquals(Color.red, label.getForeground());
        assertEquals(Color.blue, label.getBackground());
        assertEquals(list.getFont(), label.getFont());
        assertTrue(label.isEnabled());
        result = renderer.getListCellRendererComponent(list, "two", 0, true, false);
        assertTrue(result == renderer);
        label = (JLabel) result;
        assertEquals("two", label.getText());
        assertEquals(Color.pink, label.getForeground());
        assertEquals(Color.yellow, label.getBackground());
        list.setForeground(Color.black);
        list.setBackground(Color.green);
        list.setSelectionForeground(Color.gray);
        list.setSelectionBackground(Color.yellow);
        Font font = list.getFont().deriveFont(100);
        list.setFont(font);
        list.setEnabled(false);
        result = renderer.getListCellRendererComponent(list, "one", 0, false, false);
        assertTrue(result == renderer);
        label = (JLabel) result;
        assertEquals("one", label.getText());
        assertEquals(Color.black, label.getForeground());
        assertEquals(Color.green, label.getBackground());
        assertEquals(font, label.getFont());
        assertFalse(label.isEnabled());
        result = renderer.getListCellRendererComponent(list, "two", 0, true, false);
        assertTrue(result == renderer);
        label = (JLabel) result;
        assertEquals("two", label.getText());
        assertEquals(Color.gray, label.getForeground());
        assertEquals(Color.yellow, label.getBackground());
    }

    public void testFirePropertyChange() throws Exception {
        TestListener listener = new TestListener();
        renderer.addPropertyChangeListener(listener);
        renderer.firePropertyChange("qqq", true, false);
        renderer.firePropertyChange("qqq", (byte) 5, (byte) 7);
        renderer.firePropertyChange("qqq", 'a', 'b');
        renderer.firePropertyChange("qqq", 5d, 7d);
        renderer.firePropertyChange("qqq", 5f, 7f);
        renderer.firePropertyChange("qqq", 5, 7);
        renderer.firePropertyChange("qqq", (long) 5, (long) 7);
        renderer.firePropertyChange("qqq", (short) 5, (short) 7);
        renderer.firePropertyChange("qqq", "a", "b");
        assertNull(listener.getEvent());
    }

    public void testIsOpaque() throws Exception {
        // Regression test for HARMONY-2572
        assertTrue(new DefaultListCellRenderer().isOpaque());
        assertTrue(new DefaultListCellRenderer.UIResource().isOpaque());
    }

    public void testGetInheritsPopupMenu() throws Exception {
        // Regression test for HARMONY-2570
        assertTrue(new DefaultListCellRenderer().getInheritsPopupMenu());
        assertTrue(new DefaultListCellRenderer.UIResource().getInheritsPopupMenu());
    }

    private class TestListener implements PropertyChangeListener {
        private PropertyChangeEvent event;

        public void propertyChange(final PropertyChangeEvent event) {
            this.event = event;
        }

        public PropertyChangeEvent getEvent() {
            return event;
        }
    }
}
