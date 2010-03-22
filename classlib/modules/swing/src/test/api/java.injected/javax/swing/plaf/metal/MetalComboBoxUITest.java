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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.CellRendererPane;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

public class MetalComboBoxUITest extends SwingTestCase {
    private TestUI ui;

    private JComboBox comboBox;

    public MetalComboBoxUITest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        ui = new TestUI();
        comboBox = new JComboBox();
    }

    @Override
    protected void tearDown() throws Exception {
        ui = null;
        comboBox = null;
    }

    public void testMetalComboBoxUI() throws Exception {
        assertNotNull(ui.getCurrentValuePane());
        assertFalse(ui.getCurrentValuePane().isVisible());
    }

    public void testCreateUI() throws Exception {
        ComponentUI ui1 = MetalComboBoxUI.createUI(comboBox);
        assertTrue(ui1.getClass() == MetalComboBoxUI.class);
        ComponentUI ui2 = MetalComboBoxUI.createUI(comboBox);
        assertNotSame(ui1, ui2);
    }

    public void testPaint() throws Exception {
        ui.installUI(comboBox);
        ui.paint(createGraphics(), null);
    }

    public void testCreatePropertyChangeListener() throws Exception {
        assertTrue(ui.createPropertyChangeListener().getClass() == MetalComboBoxUI.MetalPropertyChangeListener.class);
    }

    public void testConfigureUnconfigureEditor() throws Exception {
        ui.setComboBox(comboBox);
        ui.setEditor(new JTextField());
        ui.configureEditor();
    }

    //TODO
    public void testLayoutComboBox() throws Exception {
        ui.setComboBox(comboBox);
        ui.layoutComboBox(null, (MetalComboBoxUI.MetalComboBoxLayoutManager) ui
                .createLayoutManager());
    }

    public void testGetMinimumSize() throws Exception {
        ui.setComboBox(comboBox);
        ComboPopup popup = new BasicComboPopup(comboBox);
        ui.setPopup(popup);
        ui.setListBox(popup.getList());
        ui.installListeners();
        comboBox.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        Dimension listPart = new BasicComboBoxRenderer().getListCellRendererComponent(
                popup.getList(), "", -1, false, false).getPreferredSize();
        Dimension expectedSize = new Dimension(listPart.width + listPart.height + 8,
                listPart.height + 8);
        assertEquals(expectedSize, ui.getMinimumSize(null));
        assertEquals(expectedSize, ui.getCachedMinimumSize());
        ui.setCachedMinimumSize(new Dimension(100, 100));
        assertEquals(ui.getCachedMinimumSize(), ui.getMinimumSize(null));
        comboBox.addItem("aaa");
        listPart = new BasicComboBoxRenderer().getListCellRendererComponent(popup.getList(),
                "aaa", -1, false, false).getPreferredSize();
        expectedSize = new Dimension(listPart.width + listPart.height + 8, listPart.height + 8);
        assertEquals(expectedSize, ui.getMinimumSize(null));
    }

    public void testCreateEditor() throws Exception {
        assertTrue(ui.createEditor().getClass() == MetalComboBoxEditor.UIResource.class);
    }

    public void testCreateArrowButton() throws Exception {
        ui.setComboBox(comboBox);
        assertTrue(ui.createArrowButton().getClass() == MetalComboBoxButton.class);
        MetalComboBoxButton arrowButton = (MetalComboBoxButton) ui.createArrowButton();
        assertEquals("", arrowButton.getText());
        assertEquals(comboBox, arrowButton.getComboBox());
        assertTrue(arrowButton.getComboIcon().getClass() == MetalComboBoxIcon.class);
        if (isHarmony()) {
            assertFalse(arrowButton.isIconOnly());
        } else {
            assertTrue(arrowButton.isIconOnly());
        }
        assertEquals(SwingConstants.TRAILING, arrowButton.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, arrowButton.getHorizontalAlignment());
        assertEquals(new Insets(3, 4, 4, 6), arrowButton.getBorder().getBorderInsets(
                arrowButton));
    }

    public void testCreateLayoutManager() throws Exception {
        assertTrue(ui.createLayoutManager().getClass() == MetalComboBoxUI.MetalComboBoxLayoutManager.class);
    }

    public void testCreatePopup() throws Exception {
        ui.setComboBox(comboBox);
        assertTrue(ui.createPopup() instanceof BasicComboPopup);
    }

    private Graphics createGraphics() {
        return new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR).createGraphics();
    }

    private class TestUI extends MetalComboBoxUI {
        public void setComboBox(final JComboBox comboBox) {
            this.comboBox = comboBox;
        }

        public void setListBox(final JList list) {
            this.listBox = list;
        }

        public void setPopup(final ComboPopup popup) {
            this.popup = popup;
        }

        public void setEditor(final Component c) {
            editor = c;
        }

        public Component getEditor() {
            return editor;
        }

        @Override
        public void installListeners() {
            super.installListeners();
        }

        public JButton getArrowButton() {
            return arrowButton;
        }

        public void setArrowButton(final JButton b) {
            arrowButton = b;
        }

        public CellRendererPane getCurrentValuePane() {
            return currentValuePane;
        }

        public Dimension getCachedMinimumSize() {
            return cachedMinimumSize;
        }

        public void setCachedMinimumSize(final Dimension d) {
            cachedMinimumSize = d;
        }
    }
}
