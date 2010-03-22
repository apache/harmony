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
package javax.swing.plaf.metal;

import java.awt.ComponentOrientation;
import javax.swing.CellRendererPane;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.SwingTestCase;

public class MetalComboBoxButtonTest extends SwingTestCase {
    private MetalComboBoxButton button;

    public MetalComboBoxButtonTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        button = new MetalComboBoxButton(new JComboBox(), null, null, null);
    }

    @Override
    protected void tearDown() throws Exception {
        button = null;
    }

    public void testMetalComboBoxButton() throws Exception {
        assertNotNull(button.comboBox);
        assertNull(button.comboIcon);
        assertNull(button.listBox);
        assertNull(button.rendererPane);
        assertFalse(button.isIconOnly());
        JComboBox comboBox = new JComboBox();
        Icon comboIcon = new ImageIcon();
        CellRendererPane rendererPane = new CellRendererPane();
        JList list = new JList();
        button = new MetalComboBoxButton(comboBox, comboIcon, rendererPane, list);
        assertEquals(comboBox, button.comboBox);
        assertEquals(comboIcon, button.comboIcon);
        assertEquals(rendererPane, button.rendererPane);
        assertEquals(list, button.listBox);
        assertFalse(button.isIconOnly());
        button = new MetalComboBoxButton(comboBox, comboIcon, true, rendererPane, list);
        assertEquals(comboBox, button.comboBox);
        assertEquals(comboIcon, button.comboIcon);
        assertEquals(rendererPane, button.rendererPane);
        assertEquals(list, button.listBox);
        assertTrue(button.isIconOnly());
        comboBox.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        button = new MetalComboBoxButton(comboBox, null, null, null);
        assertTrue(button.getComponentOrientation().isLeftToRight());
        comboBox.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        assertTrue(button.getComponentOrientation().isLeftToRight());
    }

    public void testGetSetComboBox() throws Exception {
        JComboBox comboBox = new JComboBox();
        button.setComboBox(comboBox);
        assertEquals(comboBox, button.getComboBox());
        assertEquals(comboBox, button.comboBox);
    }

    public void testGetSetComboIcon() throws Exception {
        Icon comboIcon = new ImageIcon();
        button.setComboIcon(comboIcon);
        assertEquals(comboIcon, button.getComboIcon());
        assertEquals(comboIcon, button.comboIcon);
    }

    public void testIsSetIconOnly() throws Exception {
        assertFalse(button.isIconOnly());
        button.setIconOnly(true);
        assertTrue(button.isIconOnly());
        button.setIconOnly(false);
        assertFalse(button.isIconOnly());
    }

    public void testIsFocusTraversable() throws Exception {
        assertFalse(button.isFocusTraversable());
        button.getComboBox().setEditable(true);
        assertFalse(button.isFocusTraversable());
    }

    public void testSetEnabled() throws Exception {
        assertTrue(button.isEnabled());
        button.setEnabled(false);
        assertFalse(button.isEnabled());
        JComboBox comboBox = new JComboBox();
        comboBox.setEnabled(false);
        button = new MetalComboBoxButton(comboBox, null, null, null);
        assertFalse(button.isEnabled());
    }

    public void testPaintComponent() throws Exception {
        button.paintComponent(createTestGraphics());
    }
}
