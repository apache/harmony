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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.CellRendererPane;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;
import javax.swing.plaf.ComponentUI;

public class BasicComboBoxUITest extends SwingTestCase {
    private BasicComboBoxUI ui;

    private JComboBox comboBox;

    private JFrame frame;

    public BasicComboBoxUITest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        ui = new BasicComboBoxUI();
        comboBox = new JComboBox();
        frame = new JFrame();
    }

    @Override
    protected void tearDown() throws Exception {
        ui = null;
        comboBox = null;
        frame.dispose();
    }

    public void testBasicComboBoxUI() throws Exception {
        assertNotNull(ui.currentValuePane);
        assertFalse(ui.currentValuePane.isVisible());
    }

    public void testCreateUI() throws Exception {
        ComponentUI ui1 = BasicComboBoxUI.createUI(new JComboBox());
        assertNotNull(ui1);
        ComponentUI ui2 = BasicComboBoxUI.createUI(new JComboBox());
        assertNotNull(ui2);
        assertNotSame(ui1, ui2);
    }

    public void testCreatePopup() throws Exception {
        assertNull(ui.popup);
        ui.comboBox = comboBox;
        assertNotSame(ui.createPopup(), ui.createPopup());
        assertNull(ui.popup);
    }

    public void testCreateKeyListener() throws Exception {
        if (isHarmony()) {
            assertTrue(ui.createKeyListener() instanceof BasicComboBoxUI.KeyHandler);
        }
    }

    public void testCreateFocusListener() throws Exception {
        if (isHarmony()) {
            assertTrue(ui.createFocusListener() instanceof BasicComboBoxUI.FocusHandler);
        }
    }

    public void testCreateListDataListener() throws Exception {
        if (isHarmony()) {
            assertTrue(ui.createListDataListener() instanceof BasicComboBoxUI.ListDataHandler);
        }
    }

    public void testCreateItemListener() throws Exception {
        assertNull(ui.createItemListener());
    }

    public void testCreatePropertyChangeListener() throws Exception {
        if (isHarmony()) {
            assertTrue(ui.createPropertyChangeListener() instanceof BasicComboBoxUI.PropertyChangeHandler);
        }
    }

    public void testCreateLayoutManager() throws Exception {
        if (isHarmony()) {
            assertTrue(ui.createLayoutManager() instanceof BasicComboBoxUI.ComboBoxLayoutManager);
        }
    }

    public void testCreateRenderer() throws Exception {
        assertTrue(ui.createRenderer() instanceof BasicComboBoxRenderer.UIResource);
    }

    public void testCreateEditor() throws Exception {
        assertTrue(ui.createEditor().getClass() == BasicComboBoxEditor.UIResource.class);
    }

    public void testAddRemoveEditor() throws Exception {
        ui.comboBox = comboBox;
        ui.uninstallComponents();
        assertEquals(0, ui.comboBox.getComponentCount());
        assertNull(ui.editor);
        ui.addEditor();
        assertEquals(1, ui.comboBox.getComponentCount());
        assertNotNull(ui.editor);
        assertTrue(ui.editor instanceof JTextField);
        ui.removeEditor();
        assertNull(ui.editor);
        assertEquals(0, ui.comboBox.getComponentCount());
    }

    public void testCreateArrowButton() throws Exception {
        JButton arrowButton = ui.createArrowButton();
        assertTrue(arrowButton instanceof BasicArrowButton);
        assertEquals(SwingConstants.SOUTH, ((BasicArrowButton) arrowButton).getDirection());
        assertEquals("", arrowButton.getText());
    }

    public void testIsPopupVisible() throws Exception {
        createVisibleCombo();
        ui.popup = new BasicComboPopup(comboBox);
        assertFalse(ui.isPopupVisible(comboBox));
        assertFalse(ui.popup.isVisible());
        ui.popup.show();
        assertTrue(ui.isPopupVisible(comboBox));
        assertTrue(ui.popup.isVisible());
    }

    public void testSetPopupVisible() throws Exception {
        createVisibleCombo();
        ui.popup = new BasicComboPopup(comboBox);
        assertFalse(ui.isPopupVisible(comboBox));
        ui.setPopupVisible(comboBox, true);
        assertTrue(ui.isPopupVisible(comboBox));
        ui.setPopupVisible(comboBox, false);
        assertFalse(ui.isPopupVisible(comboBox));
    }

    public void testIsFocusTraversable() throws Exception {
        ui.comboBox = comboBox;
        assertTrue(ui.isFocusTraversable(comboBox));
        comboBox.setEditable(true);
        assertFalse(ui.isFocusTraversable(comboBox));
    }

    public void testGetPreferredSize() throws Exception {
        ui.comboBox = comboBox;
        ui.popup = new BasicComboPopup(comboBox);
        ui.listBox = ui.popup.getList();
        ui.installListeners();
        assertEquals(ui.getMinimumSize(comboBox), ui.getPreferredSize(comboBox));
        comboBox.setEditable(true);
        assertEquals(ui.getMinimumSize(comboBox), ui.getPreferredSize(comboBox));
        assertEquals(ui.getMinimumSize(null), ui.getPreferredSize(null));
    }

    public void testGetMinimumSize() throws Exception {
        ui.comboBox = comboBox;
        ui.installUI(comboBox);
        comboBox.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        comboBox.setFont(comboBox.getFont().deriveFont(1f));
        Dimension listPart = new BasicComboBoxRenderer().getListCellRendererComponent(
                ui.listBox, "", -1, false, false).getPreferredSize();
        Dimension expectedSize = new Dimension(listPart.width + listPart.height + 8,
                listPart.height + 8);
        assertEquals(expectedSize, ui.getMinimumSize(null));
        assertEquals(expectedSize, ui.cachedMinimumSize);
        ui.cachedMinimumSize = new Dimension(100, 100);
        assertEquals(ui.cachedMinimumSize, ui.getMinimumSize(null));
        comboBox.addItem("aaa");
        listPart = new BasicComboBoxRenderer().getListCellRendererComponent(ui.listBox, "aaa",
                -1, false, false).getPreferredSize();
        expectedSize = new Dimension(listPart.width + listPart.height + 8, listPart.height + 8);
        assertEquals(expectedSize, ui.getMinimumSize(null));
        assertEquals(expectedSize, ui.cachedMinimumSize);
    }

    public void testSelectNextPossibleValue() throws Exception {
        ui.comboBox = comboBox;
        assertEquals(-1, comboBox.getSelectedIndex());
        ui.selectNextPossibleValue();
        assertEquals(-1, comboBox.getSelectedIndex());
        comboBox.addItem("a");
        comboBox.addItem("b");
        comboBox.addItem("c");
        assertEquals(0, comboBox.getSelectedIndex());
        ui.selectNextPossibleValue();
        assertEquals(1, comboBox.getSelectedIndex());
        ui.selectNextPossibleValue();
        assertEquals(2, comboBox.getSelectedIndex());
        ui.selectNextPossibleValue();
        assertEquals(2, comboBox.getSelectedIndex());
    }

    public void testSelectPreviousPossibleValue() throws Exception {
        ui.comboBox = comboBox;
        assertEquals(-1, comboBox.getSelectedIndex());
        ui.selectPreviousPossibleValue();
        assertEquals(-1, comboBox.getSelectedIndex());
        comboBox.addItem("a");
        comboBox.addItem("b");
        comboBox.addItem("c");
        assertEquals(0, comboBox.getSelectedIndex());
        ui.selectPreviousPossibleValue();
        assertEquals(0, comboBox.getSelectedIndex());
        comboBox.setSelectedIndex(2);
        ui.selectPreviousPossibleValue();
        assertEquals(1, comboBox.getSelectedIndex());
        ui.selectPreviousPossibleValue();
        assertEquals(0, comboBox.getSelectedIndex());
        ui.selectPreviousPossibleValue();
        assertEquals(0, comboBox.getSelectedIndex());
    }

    public void testToggleOpenClose() throws Exception {
        createVisibleCombo();
        ui.popup = new BasicComboPopup(comboBox);
        assertFalse(ui.isPopupVisible(comboBox));
        ui.toggleOpenClose();
        assertTrue(ui.isPopupVisible(comboBox));
        ui.toggleOpenClose();
        assertFalse(ui.isPopupVisible(comboBox));
    }

    public void testRectangleForCurrentValue() throws Exception {
        ui.comboBox = comboBox;
        Rectangle r1 = ui.rectangleForCurrentValue();
        assertEquals(new Rectangle(0, 0, 0, 0), r1);
        Rectangle r2 = ui.rectangleForCurrentValue();
        assertFalse(r1 == r2);
    }

    // Regression test for HARMONY-2896
    public void testGetAccessibleChildrenCount() throws Exception {
        ui.comboBox = null;
        try {
            ui.getAccessibleChildrenCount(new JComponent() {});
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            ui.getAccessibleChildrenCount(null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    public void testGetInsets() throws Exception {
        ui.comboBox = comboBox;
        assertEquals(comboBox.getInsets(), ui.getInsets());
        comboBox.setBorder(BorderFactory.createEmptyBorder(1, 2, 3, 4));
        assertEquals(comboBox.getInsets(), ui.getInsets());
        assertEquals(new Insets(1, 2, 3, 4), ui.getInsets());
    }

    public void testGetDefaultSize() throws Exception {
        ui.comboBox = comboBox;
        ui.listBox = new BasicComboPopup(comboBox).getList();
        if (isHarmony()) {
            comboBox.setFont(comboBox.getFont().deriveFont(40f));
        }
        assertEquals(comboBox.getRenderer().getListCellRendererComponent(ui.listBox, "", 0,
                false, false).getPreferredSize(), ui.getDefaultSize());
        comboBox.addItem("a");
        assertEquals(comboBox.getRenderer().getListCellRendererComponent(ui.listBox, "", 0,
                false, false).getPreferredSize(), ui.getDefaultSize());
    }

    public void testGetDisplaySize() throws Exception {
        ui.comboBox = comboBox;
        ui.popup = new BasicComboPopup(comboBox);
        ui.listBox = ui.popup.getList();
        ui.installListeners();
        comboBox.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        assertEquals(new BasicComboBoxRenderer().getListCellRendererComponent(ui.listBox, "",
                -1, false, false).getPreferredSize(), ui.getDisplaySize());
        comboBox.addItem("a");
        assertEquals(new BasicComboBoxRenderer().getListCellRendererComponent(ui.listBox, "a",
                -1, false, false).getPreferredSize(), ui.getDisplaySize());
        comboBox.addItem("aaaa");
        comboBox.addItem("aa");
        assertEquals(new BasicComboBoxRenderer().getListCellRendererComponent(ui.listBox,
                "aaaa", -1, false, false).getPreferredSize(), ui.getDisplaySize());
    }

    public void testInstallDefaults() throws Exception {
        comboBox.setFont(null);
        comboBox.setForeground(null);
        comboBox.setBackground(null);
        ui.comboBox = comboBox;
        ui.listBox = new BasicComboPopup(comboBox).getList();
        ui.installDefaults();
        assertNotNull(comboBox.getFont());
        assertNotNull(comboBox.getForeground());
        assertNotNull(comboBox.getBackground());
        ui.uninstallDefaults();
    }

    public void testInstallUninstallListeners() throws Exception {
        ui.comboBox = comboBox;
        ui.popup = new BasicComboPopup(comboBox);
        ui.uninstallListeners();
        int focusListenerCount = ui.comboBox.getFocusListeners().length;
        int itemListenerCount = ui.comboBox.getItemListeners().length;
        int keyListenerCount = ui.comboBox.getKeyListeners().length;
        int actionListenerCount = ui.comboBox.getActionListeners().length;
        int propertyChangeListenerCount = ui.comboBox.getPropertyChangeListeners().length;
        assertNull(ui.focusListener);
        assertNull(ui.itemListener);
        assertNull(ui.keyListener);
        assertNull(ui.listDataListener);
        assertNull(ui.propertyChangeListener);
        assertNull(ui.popupMouseListener);
        assertNull(ui.popupMouseMotionListener);
        assertNull(ui.popupKeyListener);
        ui.installListeners();
        assertEquals(focusListenerCount + 1, ui.comboBox.getFocusListeners().length);
        assertEquals(keyListenerCount + 1, ui.comboBox.getKeyListeners().length);
        assertEquals(propertyChangeListenerCount + 1,
                ui.comboBox.getPropertyChangeListeners().length);
        assertEquals(itemListenerCount, ui.comboBox.getItemListeners().length);
        assertEquals(actionListenerCount, ui.comboBox.getActionListeners().length);
        assertNotNull(ui.focusListener);
        assertNull(ui.itemListener);
        assertNotNull(ui.keyListener);
        assertNotNull(ui.listDataListener);
        assertNotNull(ui.propertyChangeListener);
        assertNotNull(ui.popupMouseListener);
        assertNotNull(ui.popupMouseMotionListener);
        assertNull(ui.popupKeyListener);
        ui.uninstallListeners();
        assertEquals(focusListenerCount, ui.comboBox.getFocusListeners().length);
        assertEquals(keyListenerCount, ui.comboBox.getKeyListeners().length);
        assertEquals(propertyChangeListenerCount,
                ui.comboBox.getPropertyChangeListeners().length);
        assertEquals(itemListenerCount, ui.comboBox.getItemListeners().length);
        assertEquals(actionListenerCount, ui.comboBox.getActionListeners().length);
        //        assertNull(ui.focusListener);
        //        assertNull(ui.itemListener);
        //        assertNull(ui.keyListener);
        //        assertNull(ui.listDataListener);
    }

    public void testInstallUninstallComponents() throws Exception {
        ui.comboBox = comboBox;
        ui.popup = new BasicComboPopup(comboBox);
        ui.uninstallComponents();
        assertEquals(0, ui.comboBox.getComponentCount());
        assertNull(ui.arrowButton);
        assertNull(ui.editor);
        ui.installComponents();
        assertEquals(2, ui.comboBox.getComponentCount());
        assertTrue(ui.comboBox.getComponent(0) instanceof BasicArrowButton);
        assertTrue(ui.comboBox.getComponent(1) instanceof CellRendererPane);
        assertNotNull(ui.arrowButton);
        assertNull(ui.editor);
        ui.comboBox.add(new JLabel());
        ui.uninstallComponents();
        assertEquals(0, ui.comboBox.getComponentCount());
        assertNull(ui.arrowButton);
        assertNull(ui.editor);
        comboBox.setEditable(true);
        ui.installComponents();
        assertNotNull(ui.arrowButton);
        assertNotNull(ui.editor);
    }

    public void testInstallUninstallUI() throws Exception {
        ui.comboBox = comboBox;
        ui.popup = new BasicComboPopup(comboBox);
        ui.uninstallUI(null);
        assertNull(comboBox.getLayout());
        assertNotNull(ui.currentValuePane);
        assertNull(ui.listBox);
        int itemListenerCount = comboBox.getItemListeners().length;
        ui.installUI(comboBox);
        assertEquals(itemListenerCount + 1, comboBox.getItemListeners().length);
        assertNotNull(comboBox.getLayout());
        assertTrue(ui.listBox == ui.popup.getList());
        assertTrue(ui.listBox.getFont() == comboBox.getFont());
    }

    public void testPaintCurrentValueBackground() throws Exception {
        ui.comboBox = comboBox;
        ui.paintCurrentValueBackground(createTestGraphics(), new Rectangle(0, 0, 10, 10), true);
    }

    // Regression test for HARMONY-2898
    public void testPaintCurrentValueBackground_Null() throws Exception {
        ui.comboBox = null;
        try {
            ui.paintCurrentValueBackground(createTestGraphics(), new Rectangle(0, 0, 10, 10), true);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    public void testPaintCurrentValue() throws Exception {
        ui.comboBox = comboBox;
        ui.popup = new BasicComboPopup(comboBox);
        ui.listBox = ui.popup.getList();
        ui.paintCurrentValue(createTestGraphics(), new Rectangle(0, 0, 10, 10), true);
    }

    public void testPaint() throws Exception {
        ui.comboBox = comboBox;
        ui.popup = new BasicComboPopup(comboBox);
        ui.listBox = ui.popup.getList();
        ui.paint(createTestGraphics(), null);
    }

    public void testConfigureUnconfigureEditor() throws Exception {
        ui.comboBox = comboBox;
        ui.editor = new JTextField();
        int focusListenerCount = ui.editor.getFocusListeners().length;
        ui.configureEditor();
        assertEquals(focusListenerCount + 1, ui.editor.getFocusListeners().length);
        ui.unconfigureEditor();
        assertEquals(focusListenerCount, ui.editor.getFocusListeners().length);
    }

    public void testConfigureUnconfigureArrowButton() throws Exception {
        ui.configureArrowButton();
        ui.unconfigureArrowButton();
        ui.popup = new BasicComboPopup(comboBox);
        ui.comboBox = comboBox;
        ui.arrowButton = new JButton();
        int mouseListenerCount = ui.arrowButton.getMouseListeners().length;
        int mouseMotionListenerCount = ui.arrowButton.getMouseMotionListeners().length;
        ui.configureArrowButton();
        assertEquals(mouseListenerCount + 1, ui.arrowButton.getMouseListeners().length);
        assertEquals(mouseMotionListenerCount + 1,
                ui.arrowButton.getMouseMotionListeners().length);
        ui.unconfigureArrowButton();
        assertEquals(mouseListenerCount, ui.arrowButton.getMouseListeners().length);
        assertEquals(mouseMotionListenerCount, ui.arrowButton.getMouseMotionListeners().length);
    }

    public void testPropertyChangeHandler() throws Exception {
        ui.installUI(comboBox);
        Font newFont = comboBox.getFont().deriveFont(30f);
        comboBox.setFont(newFont);
        assertEquals(newFont, comboBox.getFont());
        assertEquals(newFont, ui.listBox.getFont());
        assertEquals(comboBox.getToolTipText(), ui.arrowButton.getToolTipText());
        assertEquals(comboBox.getRenderer(), ui.listBox.getCellRenderer());
        comboBox.setEnabled(false);
        assertFalse(comboBox.isEnabled());
        assertFalse(ui.arrowButton.isEnabled());
        assertTrue(ui.listBox.isEnabled());
        assertTrue(comboBox.getRenderer().getListCellRendererComponent(ui.listBox, "", -1,
                false, false).isEnabled());
        ui.paintCurrentValue(createTestGraphics(), new Rectangle(0, 0, 10, 10), false);
        assertTrue(comboBox.getRenderer().getListCellRendererComponent(ui.listBox, "", -1,
                false, false).isEnabled());
        String newTooltip = "combo tooltip";
        comboBox.setToolTipText(newTooltip);
        assertEquals(newTooltip, ui.arrowButton.getToolTipText());
        ui.listBox.setCellRenderer(new DefaultListCellRenderer());
        ListCellRenderer newRenderer = new DefaultListCellRenderer();
        assertNotSame(ui.listBox.getCellRenderer(), comboBox.getRenderer());
        comboBox.setRenderer(newRenderer);
        assertEquals(newRenderer, ui.listBox.getCellRenderer());
        assertEquals(newRenderer, ui.popup.getList().getCellRenderer());
    }

    public void testRemove_NullEditor() throws Exception{
        // Regression test for Harmony-1749
        BasicComboBoxUI cb = new BasicComboBoxUI();
        cb.removeEditor();
    }

    public void testComboBoxLayoutManager() throws Exception {
        BasicComboBoxUI ui = new BasicComboBoxUI();
        BasicComboBoxUI.ComboBoxLayoutManager b = ui.new ComboBoxLayoutManager();

        // Regression test for HARMONY-2886
        try {
            b.minimumLayoutSize(null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            b.preferredLayoutSize(null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }
    
    @SuppressWarnings("deprecation")
    private void createVisibleCombo() {
        frame.getContentPane().add(comboBox);
        frame.show();
    }
}
