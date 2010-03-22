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
 * @author Anton Avtamonov
 */
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;

public class BasicComboBoxRendererTest extends SwingTestCase {
    private BasicComboBoxRenderer renderer;

    public BasicComboBoxRendererTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        renderer = new BasicComboBoxRenderer();
    }

    @Override
    protected void tearDown() throws Exception {
        renderer = null;
    }

    public void testBasicComboBoxRenderer() throws Exception {
        assertNotNull(BasicComboBoxRenderer.noFocusBorder);
        assertEquals(BasicComboBoxRenderer.noFocusBorder, renderer.getBorder());
        BasicComboBoxRenderer.noFocusBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);
        assertNotSame(BasicComboBoxRenderer.noFocusBorder, renderer.getBorder());
        assertEquals(BasicComboBoxRenderer.noFocusBorder, new BasicComboBoxRenderer()
                .getBorder());
        assertEquals("", renderer.getText());
        // Regression test for HARMONY-2646 
        assertEquals(SwingConstants.LEADING, new BasicComboBoxRenderer()
                .getHorizontalAlignment()); 
        
    }

    public void testGetListCellRendererComponent() throws Exception {
        JList list = new JList();
        assertEquals(renderer, renderer.getListCellRendererComponent(list, "aaa", 0, false,
                false));
        assertTrue(renderer.isOpaque());
        Font newFont = list.getFont().deriveFont(20f);
        list.setFont(newFont);
        assertNotSame(newFont, renderer.getFont());
        renderer.getListCellRendererComponent(list, "aaa", 0, false, false);
        assertEquals(newFont, renderer.getFont());
        list.setForeground(Color.red);
        list.setBackground(Color.blue);
        list.setSelectionForeground(Color.yellow);
        list.setSelectionBackground(Color.pink);
        assertNotSame(Color.red, renderer.getForeground());
        assertNotSame(Color.blue, renderer.getBackground());
        renderer.getListCellRendererComponent(list, "aaa", 0, false, false);
        assertEquals(Color.red, renderer.getForeground());
        assertEquals(Color.blue, renderer.getBackground());
        renderer.getListCellRendererComponent(list, "aaa", 0, true, false);
        assertEquals(Color.yellow, renderer.getForeground());
        assertEquals(Color.pink, renderer.getBackground());
        list.setEnabled(false);
        renderer.getListCellRendererComponent(list, "aaa", 0, false, false);
        assertTrue(renderer.isEnabled());
        assertEquals(Color.red, renderer.getForeground());
        assertEquals(Color.blue, renderer.getBackground());
    }

    public void testGetPreferredSize() throws Exception {
        JLabel l = new JLabel(" ");
        l.setBorder(BasicComboBoxRenderer.noFocusBorder);
        assertEquals(l.getPreferredSize(), renderer.getPreferredSize());
        renderer.getListCellRendererComponent(new JList(), "aaa", 0, false, false);
        l.setText("aaa");
        assertEquals(l.getPreferredSize(), renderer.getPreferredSize());
        assertEquals(l.getPreferredSize(), renderer.getPreferredSize());
        renderer.setText(null);
        l.setText(" ");
        assertEquals(l.getPreferredSize(), renderer.getPreferredSize());
        renderer.setText("");
        assertEquals(l.getPreferredSize(), renderer.getPreferredSize());
        renderer.setPreferredSize(new Dimension(100, 100));
        assertEquals(new Dimension(100, 100), renderer.getPreferredSize());
    }
}
