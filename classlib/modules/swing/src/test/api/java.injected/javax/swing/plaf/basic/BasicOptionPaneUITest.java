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

import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.OptionPaneUI;
import javax.swing.plaf.basic.BasicOptionPaneUI.ButtonAreaLayout;

public class BasicOptionPaneUITest extends SwingTestCase {
    protected BasicOptionPaneUI paneUI;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        paneUI = new BasicOptionPaneUI() {
            public FontMetrics getFontMetrics(Font font) {
                return BasicOptionPaneUITest.this.getFontMetrics(font);
            }
        };
    }

    class MyLayoutManager implements LayoutManager {
        private final Dimension size;

        MyLayoutManager(final Dimension size) {
            this.size = size;
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public void layoutContainer(Container parent) {
        }

        public Dimension minimumLayoutSize(Container parent) {
            return size;
        }

        public Dimension preferredLayoutSize(Container parent) {
            return size;
        }

        public void removeLayoutComponent(Component comp) {
        }
    };

    public void testGetPreferredSize() {
        JOptionPane pane = new JOptionPane() {
            private static final long serialVersionUID = 1L;

            @Override
            public FontMetrics getFontMetrics(Font font) {
                return BasicOptionPaneUITest.this.getFontMetrics(font);
            }
        };
        Border messageAreaBorder = new BorderUIResource(BorderFactory.createEmptyBorder(3, 3,
                3, 3));
        UIManager.put("OptionPane.messageAreaBorder", messageAreaBorder);
        messageAreaBorder = new BorderUIResource(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        UIManager.put("OptionPane.messageAreaBorder", messageAreaBorder);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        UIManager.put("OptionPane.border", border);
        Dimension minimumSize = new DimensionUIResource(123, 456);
        UIManager.put("OptionPane.minimumSize", minimumSize);
        pane.setUI(paneUI);
        assertNull(paneUI.getPreferredSize(null));
        Dimension preferredSize = paneUI.getPreferredSize(pane);
        assertNotNull(preferredSize);
        assertTrue(preferredSize.width > 0);
        assertTrue(preferredSize.height > 0);
        LayoutManager layout = new MyLayoutManager(new Dimension(200, 500));
        pane.setLayout(layout);
        assertEquals(new Dimension(200, 500), paneUI.getPreferredSize(pane));
        layout = new MyLayoutManager(new Dimension(200, 300));
        pane.setLayout(layout);
        assertEquals(new Dimension(200, 456), paneUI.getPreferredSize(pane));
        layout = new MyLayoutManager(new Dimension(100, 500));
        pane.setLayout(layout);
        assertEquals(new Dimension(123, 500), paneUI.getPreferredSize(pane));
        layout = new MyLayoutManager(new Dimension(10, 10));
        pane.setLayout(layout);
        assertEquals(new Dimension(123, 456), paneUI.getPreferredSize(pane));
    }

    public void testInstallUI() {
        JOptionPane pane = new JOptionPane();
        pane.removeAll();
        pane.setLayout(null);
        paneUI.installUI(pane);
        assertNotNull(pane.getBackground());
        assertNotNull(paneUI.optionPane);
        assertEquals(2, pane.getComponentCount());
        assertNotNull(paneUI.optionPane.getLayout());
        assertTrue(paneUI.optionPane.getLayout() instanceof BoxLayout);
    }

    public void testUninstallUI() {
        JOptionPane pane = new JOptionPane();
        pane.setUI(paneUI);
        paneUI.uninstallUI(pane);
        assertEquals(0, pane.getComponentCount());
        assertNull(paneUI.optionPane);
    }

    public void testCreateUI() {
        assertTrue("created UI is not null", null != BasicOptionPaneUI
                .createUI(new JOptionPane()));
        assertTrue("created UI is of the proper class",
                BasicOptionPaneUI.createUI(null) instanceof BasicOptionPaneUI);
        assertNotSame("created UIs are unique", BasicOptionPaneUI.createUI(null),
                BasicOptionPaneUI.createUI(null));
    }

    public void testSelectInitialValue() {
        //TODO Implement selectInitialValue().
    }

    public void testContainsCustomComponents() {
        OptionPaneUI ui = null;
        JOptionPane optionPane = null;
        optionPane = new JOptionPane();
        assertFalse(paneUI.containsCustomComponents(optionPane));
        assertFalse(paneUI.containsCustomComponents(null));
        paneUI.hasCustomComponents = true;
        assertTrue(paneUI.containsCustomComponents(optionPane));
        assertTrue(paneUI.containsCustomComponents(null));
        optionPane = new JOptionPane("Message", JOptionPane.ERROR_MESSAGE);
        ui = optionPane.getUI();
        assertFalse(ui.containsCustomComponents(optionPane));
        assertFalse(ui.containsCustomComponents(null));
        optionPane = new JOptionPane(new JButton("Message"), JOptionPane.ERROR_MESSAGE);
        ui = optionPane.getUI();
        assertTrue(ui.containsCustomComponents(optionPane));
        assertTrue(ui.containsCustomComponents(null));
        optionPane = new JOptionPane("Message", JOptionPane.ERROR_MESSAGE,
                JOptionPane.CLOSED_OPTION, null, new Object[] { "1", "2" });
        ui = optionPane.getUI();
        assertFalse(ui.containsCustomComponents(optionPane));
        assertFalse(ui.containsCustomComponents(null));
        optionPane = new JOptionPane("Message", JOptionPane.ERROR_MESSAGE,
                JOptionPane.CLOSED_OPTION, null, new Object[] { new Button("1"), "2" });
        ui = optionPane.getUI();
        assertTrue(ui.containsCustomComponents(optionPane));
        assertTrue(ui.containsCustomComponents(null));
    }

    public void testInstallDefaults() {
        JOptionPane pane = new JOptionPane();
        UIManager.put("OptionPane.background", new ColorUIResource(Color.red));
        UIManager.put("OptionPane.foreground", new ColorUIResource(Color.yellow));
        UIManager.put("OptionPane.messageForeground", new ColorUIResource(Color.green));
        Font font = new FontUIResource(pane.getFont().deriveFont(100f));
        UIManager.put("OptionPane.font", font);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        UIManager.put("OptionPane.border", border);
        Dimension minimumSize = new DimensionUIResource(123, 456);
        UIManager.put("OptionPane.minimumSize", minimumSize);
        pane.setUI(paneUI);
        pane.setOptions(new Object[] { "button" });
        paneUI.installDefaults();
        assertEquals(Color.red, pane.getBackground());
        assertEquals(Color.yellow, pane.getForeground());
        assertEquals(font, pane.getFont());
        assertEquals(border, pane.getBorder());
        assertEquals(minimumSize, paneUI.getMinimumOptionPaneSize());
    }

    public void testUninstallDefaults() {
        JOptionPane pane = new JOptionPane();
        pane.setUI(paneUI);
        assertNotNull(pane.getBackground());
        assertNotNull(pane.getForeground());
        assertNotNull(pane.getFont());
        assertNotNull(pane.getBorder());
        assertNotNull(paneUI.getMinimumOptionPaneSize());
        paneUI.uninstallDefaults();
        assertNotNull(pane.getBackground());
        assertNotNull(pane.getForeground());
        assertNotNull(pane.getFont());
        assertNull(pane.getBorder());
        assertNotNull(paneUI.getMinimumOptionPaneSize());
    }

    public void testInstallComponents() {
        JOptionPane pane = new JOptionPane();
        pane.removeAll();
        paneUI.optionPane = pane;
        JCheckBox fake = new JCheckBox();
        paneUI.inputComponent = fake;
        paneUI.installComponents();
        assertEquals(2, pane.getComponentCount());
        assertTrue(pane.getComponent(0) instanceof JPanel);
        assertTrue(pane.getComponent(1) instanceof JPanel);
        assertTrue(((JPanel) pane.getComponent(0)).getComponent(0) instanceof JPanel);
        assertTrue(((JPanel) pane.getComponent(1)).getComponent(0) instanceof JButton);
        assertNull(paneUI.inputComponent);
        paneUI.inputComponent = fake;
        paneUI.optionPane.setWantsInput(true);
        paneUI.installComponents();
        assertNotNull(paneUI.inputComponent);
    }

    public void testUninstallComponents() {
        JOptionPane pane = new JOptionPane();
        assertEquals(2, pane.getComponentCount());
        paneUI.optionPane = pane;
        pane.add(new JCheckBox());
        paneUI.inputComponent = new JCheckBox();
        paneUI.uninstallComponents();
        assertEquals(0, pane.getComponentCount());
        assertNotNull(paneUI.optionPane);
        assertNull(paneUI.inputComponent);
    }

    public void testCreateLayoutManager() {
        LayoutManager layout1 = paneUI.createLayoutManager();
        LayoutManager layout2 = paneUI.createLayoutManager();
        assertTrue("LayoutManager is not null", layout1 != null);
        assertEquals("LayoutManager's class ", "javax.swing.BoxLayout", layout1.getClass()
                .getName());
        assertFalse("layout2 is not shared", layout1 == layout2);
    }

    public void testInstallListeners() {
        paneUI.optionPane = new JOptionPane();
        assertEquals(1, paneUI.optionPane.getPropertyChangeListeners().length);
    }

    public void testUninstallListeners() {
        paneUI.optionPane = new JOptionPane();
        paneUI.optionPane.setUI(paneUI);
        assertEquals(1, paneUI.optionPane.getPropertyChangeListeners().length);
        assertNotNull(paneUI.propertyChangeListener);
        paneUI.uninstallListeners();
        assertEquals(0, paneUI.optionPane.getPropertyChangeListeners().length);
        assertNull(paneUI.propertyChangeListener);
    }

    // Regression for HARMONY-2901
    public void testUninstallListenersNull() {
        assertNull(paneUI.optionPane);
        paneUI.uninstallListeners(); // no exception is expected
    }

    public void testCreatePropertyChangeListener() {
        assertNotNull(paneUI.createPropertyChangeListener());
    }

    public void testInstallUninstallKeyboardActions() {
        paneUI.optionPane = new JOptionPane();
        Object[] keys = null;
        paneUI.uninstallKeyboardActions();
        keys = paneUI.optionPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .allKeys();
        assertTrue(keys == null || keys.length == 0);
        keys = paneUI.optionPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).allKeys();
        assertTrue(keys == null || keys.length == 0);
        keys = paneUI.optionPane.getInputMap(JComponent.WHEN_FOCUSED).allKeys();
        assertTrue(keys == null || keys.length == 0);
        keys = paneUI.optionPane.getActionMap().allKeys();
        assertTrue(keys == null || keys.length == 0);
        assertNull(paneUI.optionPane.getActionMap().getParent());
        paneUI.installKeyboardActions();
        keys = paneUI.optionPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .allKeys();
        assertTrue(keys == null || keys.length == 0);
        keys = paneUI.optionPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).allKeys();
        assertNotNull(keys);
        assertEquals(1, keys.length);
        assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), keys[0]);
        keys = paneUI.optionPane.getInputMap(JComponent.WHEN_FOCUSED).allKeys();
        assertTrue(keys == null || keys.length == 0);
        keys = paneUI.optionPane.getActionMap().keys();
        assertTrue(keys == null || keys.length == 0);
        keys = paneUI.optionPane.getActionMap().getParent().keys();
        assertNotNull(keys);
        assertEquals(1, keys.length);
        assertEquals("close", keys[0]);
    }

    public void testGetMinimumOptionPaneSize() {
        assertEquals(new Dimension(262, 90), paneUI.getMinimumOptionPaneSize());
    }

    public void testCreateMessageArea() {
        JOptionPane pane = new JOptionPane();
        String message = "message message message message";
        pane.setUI(paneUI);
        pane.setMessageType(JOptionPane.ERROR_MESSAGE);
        pane.setMessage(message);
        Border messageAreaBorder = new BorderUIResource(BorderFactory.createEmptyBorder(3, 3,
                3, 3));
        UIManager.put("OptionPane.messageAreaBorder", messageAreaBorder);
        paneUI.installDefaults();
        assertEquals(messageAreaBorder, ((JComponent) paneUI.createMessageArea()).getBorder());
        messageAreaBorder = new BorderUIResource(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        UIManager.put("OptionPane.messageAreaBorder", messageAreaBorder);
        paneUI.installDefaults();
        assertEquals(messageAreaBorder, ((JComponent) paneUI.createMessageArea()).getBorder());
        JComponent messageArea = (JComponent) paneUI.createMessageArea();
        assertTrue(messageArea instanceof JPanel);
        assertTrue(messageArea.getComponent(0) instanceof JPanel);
        JPanel panel = (JPanel) messageArea.getComponent(0);
        assertTrue(panel.getComponent(0) instanceof JPanel);
        assertEquals(0, ((JPanel) panel.getComponent(0)).getComponentCount());
        assertEquals(new Dimension(15, 1), ((JPanel) panel.getComponent(0)).getPreferredSize());
        assertTrue(panel.getComponent(1) instanceof JPanel);
        assertEquals(1, ((JPanel) panel.getComponent(1)).getComponentCount());
        assertTrue(((JPanel) panel.getComponent(1)).getComponent(0) instanceof JLabel);
        JLabel label = (JLabel) ((JPanel) panel.getComponent(1)).getComponent(0);
        assertEquals("message", message, label.getText());
        assertTrue(messageArea.getComponent(1) instanceof JLabel);
        JLabel iconLabel = (JLabel) messageArea.getComponent(1);
        assertEquals(paneUI.getIconForType(JOptionPane.ERROR_MESSAGE), iconLabel.getIcon());
        pane = new JOptionPane();
        pane.setUI(paneUI);
        pane.setInitialValue(message + message);
        pane.setWantsInput(true);
        pane.setMessageType(JOptionPane.ERROR_MESSAGE);
        pane.setMessage(message);
        messageArea = (JComponent) paneUI.createMessageArea();
        assertTrue(messageArea instanceof JPanel);
        assertTrue(messageArea.getComponent(0) instanceof JPanel);
        panel = (JPanel) messageArea.getComponent(0);
        assertTrue(panel.getComponent(0) instanceof JPanel);
        assertEquals(0, ((JPanel) panel.getComponent(0)).getComponentCount());
        assertEquals(new Dimension(15, 1), ((JPanel) panel.getComponent(0)).getPreferredSize());
        assertTrue(panel.getComponent(1) instanceof JPanel);
        assertEquals(2, ((JPanel) panel.getComponent(1)).getComponentCount());
        assertTrue(((JPanel) panel.getComponent(1)).getComponent(0) instanceof JLabel);
        label = (JLabel) ((JPanel) panel.getComponent(1)).getComponent(0);
        assertEquals("message", message, label.getText());
        assertTrue(((JPanel) panel.getComponent(1)).getComponent(1) instanceof JTextField);
        JTextField text = (JTextField) ((JPanel) panel.getComponent(1)).getComponent(1);
        assertEquals("message", "", text.getText());
        assertTrue(messageArea.getComponent(1) instanceof JLabel);
        iconLabel = (JLabel) messageArea.getComponent(1);
        assertEquals(paneUI.getIconForType(JOptionPane.ERROR_MESSAGE), iconLabel.getIcon());
    }

    public void testAddMessageComponents() {
        Container messageContainer = new JPanel();
        paneUI.optionPane = new JOptionPane();
        Component child = null, parent = null;
        Object message1 = new JButton("Tarara");
        Object message2 = new ImageIcon();
        Object message3 = "Wu Tang";
        Object message4 = new Integer(111);
        Object[] message5 = new Object[] { "1", "2", "3" };
        GridBagConstraints constrains = new GridBagConstraints(100, 200, 300, 400, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0),
                111, 222);
        paneUI.addMessageComponents(messageContainer, constrains, message1, -1, true);
        assertEquals("number of children", 1, messageContainer.getComponentCount());
        child = messageContainer.getComponent(0);
        assertEquals(message1, child);
        assertEquals(201, constrains.gridy);
        paneUI.addMessageComponents(messageContainer, constrains, message2, -1, true);
        assertEquals("number of children", 2, messageContainer.getComponentCount());
        child = messageContainer.getComponent(1);
        assertTrue(child instanceof JLabel);
        assertSame(message2, ((JLabel) child).getIcon());
        assertEquals(202, constrains.gridy);
        paneUI.addMessageComponents(messageContainer, constrains, message3, 10000, true);
        assertEquals("number of children", 3, messageContainer.getComponentCount());
        child = messageContainer.getComponent(2);
        assertTrue(child instanceof JLabel);
        assertSame(message3, ((JLabel) child).getText());
        assertEquals(203, constrains.gridy);
        paneUI.addMessageComponents(messageContainer, constrains, message3, 1, true);
        assertEquals("number of children", 4, messageContainer.getComponentCount());
        parent = messageContainer.getComponent(3);
        assertTrue(parent instanceof Box);
        assertEquals(2, ((Box) parent).getComponentCount());
        child = ((Box) parent).getComponent(0);
        assertTrue(child instanceof JLabel);
        assertEquals("Wu", ((JLabel) child).getText());
        child = ((Box) parent).getComponent(1);
        assertTrue(child instanceof JLabel);
        assertEquals("Tang", ((JLabel) child).getText());
        assertEquals(204, constrains.gridy);
        paneUI.addMessageComponents(messageContainer, constrains, message4, -1, true);
        assertEquals("number of children", 5, messageContainer.getComponentCount());
        child = messageContainer.getComponent(4);
        assertTrue(child instanceof Box);
        child = ((Box) child).getComponent(0);
        assertTrue(child instanceof JLabel);
        assertEquals(message4.toString(), ((JLabel) child).getText());
        assertEquals(205, constrains.gridy);
        paneUI.addMessageComponents(messageContainer, constrains, message5, -1, true);
        assertEquals("number of children", 8, messageContainer.getComponentCount());
        child = messageContainer.getComponent(5);
        assertTrue(child instanceof Box);
        child = ((Box) child).getComponent(0);
        assertTrue(child instanceof JLabel);
        assertEquals("1", ((JLabel) child).getText());
        child = messageContainer.getComponent(6);
        assertTrue(child instanceof Box);
        child = ((Box) child).getComponent(0);
        assertTrue(child instanceof JLabel);
        assertEquals("2", ((JLabel) child).getText());
        child = messageContainer.getComponent(7);
        assertTrue(child instanceof Box);
        child = ((Box) child).getComponent(0);
        assertTrue(child instanceof JLabel);
        assertEquals("3", ((JLabel) child).getText());
        assertEquals(208, constrains.gridy);
        messageContainer = new JPanel();
        constrains.gridy = 0;
        paneUI.addMessageComponents(messageContainer, constrains, "", 1000, false);
        assertEquals(0, messageContainer.getComponentCount());
        assertEquals(0, constrains.gridy);
        paneUI.addMessageComponents(messageContainer, constrains, "\n\n", 1000, false);
        assertEquals(2, messageContainer.getComponentCount());
        assertEquals(2, constrains.gridy);
        messageContainer = new JPanel();
        constrains.gridy = 0;
        paneUI.addMessageComponents(messageContainer, constrains, "1\n2\n\n3\n", 1000, false);
        assertEquals(4, constrains.gridy);
        assertEquals(4, messageContainer.getComponentCount());
        child = messageContainer.getComponent(0);
        assertTrue(child instanceof JLabel);
        assertEquals("1", ((JLabel) child).getText());
        child = messageContainer.getComponent(1);
        assertTrue(child instanceof JLabel);
        assertEquals("2", ((JLabel) child).getText());
        child = messageContainer.getComponent(3);
        assertTrue(child instanceof JLabel);
        assertEquals("3", ((JLabel) child).getText());
        messageContainer = new JPanel();
        constrains.gridy = 0;
        paneUI.addMessageComponents(messageContainer, constrains, "123", 1, false);
        assertEquals(1, constrains.gridy);
        assertEquals(1, messageContainer.getComponentCount());
    }

    public void testGetMessage() {
        Object message = new JButton();
        paneUI.optionPane = new JOptionPane();
        paneUI.optionPane.setMessage(message);
        assertEquals(message, paneUI.getMessage());
    }

    public void testAddIcon() {
        JPanel panel = new JPanel();
        paneUI.optionPane = new JOptionPane();
        paneUI.optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
        paneUI.addIcon(panel);
        JComponent label = (JComponent) panel.getComponent(0);
        assertTrue(label instanceof JLabel);
        assertEquals(paneUI.getIconForType(JOptionPane.INFORMATION_MESSAGE), ((JLabel) label)
                .getIcon());
        assertEquals(SwingConstants.TOP, ((JLabel) label).getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, ((JLabel) label).getHorizontalAlignment());
        assertEquals(SwingConstants.CENTER, ((JLabel) label).getVerticalTextPosition());
        assertEquals(SwingConstants.TRAILING, ((JLabel) label).getHorizontalTextPosition());
        paneUI.optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
        paneUI.addIcon(panel);
        label = (JComponent) panel.getComponent(0);
        assertTrue(label instanceof JLabel);
        assertEquals(paneUI.getIconForType(JOptionPane.INFORMATION_MESSAGE), ((JLabel) label)
                .getIcon());
        label = (JComponent) panel.getComponent(1);
        assertTrue(label instanceof JLabel);
        assertEquals(paneUI.getIconForType(JOptionPane.ERROR_MESSAGE), ((JLabel) label)
                .getIcon());
        assertEquals(SwingConstants.TOP, ((JLabel) label).getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, ((JLabel) label).getHorizontalAlignment());
        assertEquals(SwingConstants.CENTER, ((JLabel) label).getVerticalTextPosition());
        assertEquals(SwingConstants.TRAILING, ((JLabel) label).getHorizontalTextPosition());
    }

    public void testGetIcon() {
        Icon icon = null;
        assertNull(paneUI.getIcon());
        JOptionPane optionPane = new JOptionPane();
        paneUI.installUI(optionPane);
        optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
        assertEquals(paneUI.getIconForType(JOptionPane.INFORMATION_MESSAGE), paneUI.getIcon());
        optionPane.setIcon(null);
        assertEquals(paneUI.getIconForType(JOptionPane.INFORMATION_MESSAGE), paneUI.getIcon());
        icon = new ImageIcon();
        optionPane.setIcon(icon);
        assertEquals(icon, paneUI.getIcon());
        optionPane.setMessageType(JOptionPane.PLAIN_MESSAGE);
        assertEquals(icon, paneUI.getIcon());
        optionPane.setIcon(null);
        assertNull(paneUI.getIcon());
    }

    public void testGetIconForType() throws InterruptedException {
        paneUI.optionPane = new JOptionPane();
        paneUI.optionPane.setUI(paneUI);
        Icon icon11 = new IconUIResource(new ImageIcon(new BufferedImage(10, 20,
                BufferedImage.TYPE_INT_RGB)));
        Icon icon21 = new IconUIResource(new ImageIcon(new BufferedImage(30, 40,
                BufferedImage.TYPE_INT_RGB)));
        Icon icon31 = new IconUIResource(new ImageIcon(new BufferedImage(50, 60,
                BufferedImage.TYPE_INT_RGB)));
        Icon icon41 = new IconUIResource(new ImageIcon(new BufferedImage(70, 80,
                BufferedImage.TYPE_INT_RGB)));
        UIManager.put("OptionPane.errorIcon", icon11);
        UIManager.put("OptionPane.informationIcon", icon21);
        UIManager.put("OptionPane.questionIcon", icon31);
        UIManager.put("OptionPane.warningIcon", icon41);
        Icon icon12 = paneUI.getIconForType(JOptionPane.ERROR_MESSAGE);
        Icon icon22 = paneUI.getIconForType(JOptionPane.INFORMATION_MESSAGE);
        Icon icon32 = paneUI.getIconForType(JOptionPane.QUESTION_MESSAGE);
        Icon icon42 = paneUI.getIconForType(JOptionPane.WARNING_MESSAGE);
        Icon icon52 = paneUI.getIconForType(JOptionPane.PLAIN_MESSAGE);
        Icon icon62 = paneUI.getIconForType(100);
        assertEquals(icon11, icon12);
        assertEquals(icon21, icon22);
        assertEquals(icon31, icon32);
        assertEquals(icon41, icon42);
        assertNull(icon52);
        assertNull(icon62);
        assertSame("icons are shared", icon12, paneUI.getIconForType(JOptionPane.ERROR_MESSAGE));
        assertSame("icons are shared", icon22, paneUI
                .getIconForType(JOptionPane.INFORMATION_MESSAGE));
        assertSame("icons are shared", icon32, paneUI
                .getIconForType(JOptionPane.QUESTION_MESSAGE));
        assertSame("icons are shared", icon42, paneUI
                .getIconForType(JOptionPane.WARNING_MESSAGE));
    }

    public void testGetIconForType_Null() throws InterruptedException {
        try { // Regression test for HARMONY-2903
            new BasicOptionPaneUI().getIconForType(0);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    public void testGetMaxCharactersPerLineCount() {
        paneUI.optionPane = new JOptionPane();
        paneUI.optionPane.setUI(paneUI);
        assertEquals(Integer.MAX_VALUE, paneUI.getMaxCharactersPerLineCount());
    }

    // Regression for HARMONY-2902
    public void testGetMaxCharactersPerLineCount_OptionPane() {
        final Marker marker = new Marker();
        paneUI.optionPane = new JOptionPane() {
            private static final long serialVersionUID = 1L;

            @Override
            public int getMaxCharactersPerLineCount() {
                marker.setOccurred();
                return super.getMaxCharactersPerLineCount();
            }
        };
        paneUI.optionPane.setUI(paneUI);
        marker.reset();
        assertEquals(Integer.MAX_VALUE, paneUI.getMaxCharactersPerLineCount());
        assertTrue(marker.isOccurred());
    }

    // Regression for HARMONY-2902
    public void testGetMaxCharactersPerLineCount_Null() {
        assertNull(paneUI.optionPane);
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                paneUI.getMaxCharactersPerLineCount();
            }
        });
    }

    public void testBurstStringInto() {
        String message = "message ";
        JPanel panel = new JPanel();
        paneUI.burstStringInto(panel, message, 2);
        assertEquals(1, panel.getComponentCount());
        assertEquals("message", ((JLabel) panel.getComponent(0)).getText());
        message = "message \n\n  message";
        panel = new JPanel();
        paneUI.burstStringInto(panel, message, 2);
        assertEquals(3, panel.getComponentCount());
        assertEquals("message", ((JLabel) panel.getComponent(0)).getText());
        assertEquals("\n\n", ((JLabel) panel.getComponent(1)).getText());
        assertEquals(" message", ((JLabel) panel.getComponent(2)).getText());
        panel = new JPanel();
        paneUI.burstStringInto(panel, message, 20);
        assertEquals(1, panel.getComponentCount());
        assertEquals(message, ((JLabel) panel.getComponent(0)).getText());
        panel = new JPanel();
        message = "";
        for (int i = 0; i < 4; i++) {
            message += "messagemessage   \n";
        }
        paneUI.burstStringInto(panel, message, 50);
        assertEquals(2, panel.getComponentCount());
        assertEquals("messagemessage   \nmessagemessage   \nmessagemessage", ((JLabel) panel
                .getComponent(0)).getText());
        assertEquals("  \nmessagemessage   \n", ((JLabel) panel.getComponent(1)).getText());
    }

    public void testCreateSeparator() {
        assertNull(paneUI.createSeparator());
    }

    private void checkButton(final JButton button, final String text, final long threshold,
            final int mnemonic, final int numListeners, final Insets margin) {
        assertEquals("button text", text, button.getText());
        assertEquals("Threshold", threshold, button.getMultiClickThreshhold());
        assertEquals("button mnemonic", mnemonic, button.getMnemonic());
        assertEquals("listener", numListeners, button.getActionListeners().length);
        assertEquals("margin", margin, button.getMargin());
    }

    public void testCreateButtonArea() {
        Container buttonArea = null;
        paneUI.optionPane = new JOptionPane();
        JButton button;
        paneUI.optionPane.setOptionType(JOptionPane.YES_NO_OPTION);
        int threshold = 111;
        UIManager.put("OptionPane.buttonClickThreshhold", new Integer(threshold));
        Border buttonAreaBorder = new BorderUIResource(BorderFactory.createEmptyBorder(2, 2, 2,
                2));
        UIManager.put("OptionPane.buttonAreaBorder", buttonAreaBorder);
        paneUI.installDefaults();
        buttonArea = paneUI.createButtonArea();
        assertEquals("class", JPanel.class, buttonArea.getClass());
        assertEquals("layout", BasicOptionPaneUI.ButtonAreaLayout.class, buttonArea.getLayout()
                .getClass());
        assertEquals("layout padding", 6, ((BasicOptionPaneUI.ButtonAreaLayout) buttonArea
                .getLayout()).getPadding());
        assertEquals("border", buttonAreaBorder, ((JComponent) buttonArea).getBorder());
        assertEquals("border insets", new Insets(2, 2, 2, 2), ((JPanel) buttonArea).getBorder()
                .getBorderInsets(buttonArea));
        assertEquals("number of buttons", 2, buttonArea.getComponentCount());
        assertEquals("button class", JButton.class, buttonArea.getComponent(0).getClass());
        button = (JButton) buttonArea.getComponent(0);
        checkButton(button, "Yes", threshold, KeyEvent.VK_Y, 1, new Insets(2, 8, 2, 8));
        assertFalse("custom", paneUI.containsCustomComponents(null));
        assertEquals("button class", JButton.class, buttonArea.getComponent(1).getClass());
        button = (JButton) buttonArea.getComponent(1);
        checkButton(button, "No", threshold, KeyEvent.VK_N, 1, new Insets(2, 8, 2, 8));
        assertFalse("custom", paneUI.containsCustomComponents(null));
        paneUI.optionPane.setOptionType(JOptionPane.YES_NO_CANCEL_OPTION);
        buttonArea = paneUI.createButtonArea();
        assertEquals("number of buttons", 3, buttonArea.getComponentCount());
        assertEquals("button class", JButton.class, buttonArea.getComponent(0).getClass());
        button = (JButton) buttonArea.getComponent(0);
        checkButton(button, "Yes", threshold, KeyEvent.VK_Y, 1, new Insets(2, 4, 2, 4));
        assertFalse("custom", paneUI.containsCustomComponents(null));
        assertEquals("button class", JButton.class, buttonArea.getComponent(1).getClass());
        button = (JButton) buttonArea.getComponent(1);
        checkButton(button, "No", threshold, KeyEvent.VK_N, 1, new Insets(2, 4, 2, 4));
        assertFalse("custom", paneUI.containsCustomComponents(null));
        assertEquals("button class", JButton.class, buttonArea.getComponent(2).getClass());
        button = (JButton) buttonArea.getComponent(2);
        checkButton(button, "Cancel", threshold, 0, 1, new Insets(2, 4, 2, 4));
        assertFalse("custom", paneUI.containsCustomComponents(null));
        paneUI.optionPane.setOptionType(JOptionPane.CANCEL_OPTION);
        buttonArea = paneUI.createButtonArea();
        assertEquals("number of buttons", 2, buttonArea.getComponentCount());
        assertEquals("button class", JButton.class, buttonArea.getComponent(0).getClass());
        button = (JButton) (buttonArea.getComponent(0));
        checkButton(button, "OK", threshold, 0, 1, new Insets(2, 8, 2, 8));
        assertFalse("custom", paneUI.containsCustomComponents(null));
        assertEquals("button class", JButton.class, buttonArea.getComponent(1).getClass());
        button = (JButton) (buttonArea.getComponent(1));
        button = (JButton) buttonArea.getComponent(1);
        checkButton(button, "Cancel", threshold, 0, 1, new Insets(2, 8, 2, 8));
        assertFalse("custom", paneUI.containsCustomComponents(null));
        paneUI.optionPane.setOptionType(JOptionPane.CLOSED_OPTION);
        buttonArea = paneUI.createButtonArea();
        assertEquals("number of buttons", 1, buttonArea.getComponentCount());
        assertEquals("button class", JButton.class, buttonArea.getComponent(0).getClass());
        button = (JButton) (buttonArea.getComponent(0));
        checkButton(button, "OK", threshold, 0, 1, new Insets(2, 8, 2, 8));
        assertFalse("custom", paneUI.containsCustomComponents(null));
        Object option1 = new JButton("Tarara");
        Object option2 = new Integer(100);
        Object option3 = "Eminem must tsum menimE";
        Object option4 = new Button("Tarara");
        Object option5 = new ImageIcon();
        paneUI.optionPane
                .setOptions(new Object[] { option1, option2, option3, option4, option5 });
        buttonArea = paneUI.createButtonArea();
        assertEquals("number of buttons", 5, buttonArea.getComponentCount());
        assertTrue("custom", paneUI.containsCustomComponents(null));
        assertEquals("button class", JButton.class, buttonArea.getComponent(0).getClass());
        button = (JButton) (buttonArea.getComponent(0));
        checkButton(button, "Tarara", 0, 0, 0, ((JButton) option1).getMargin());
        assertEquals("button class", JButton.class, buttonArea.getComponent(1).getClass());
        button = (JButton) (buttonArea.getComponent(1));
        checkButton(button, "100", threshold, 0, 1, new Insets(2, 14, 2, 14));
        assertEquals("button class", JButton.class, buttonArea.getComponent(2).getClass());
        button = (JButton) (buttonArea.getComponent(2));
        checkButton(button, option3.toString(), threshold, 0, 1, new Insets(2, 14, 2, 14));
        assertEquals("button class", Button.class, buttonArea.getComponent(3).getClass());
        assertEquals("button text", "Tarara", ((Button) (buttonArea.getComponent(3)))
                .getLabel());
        assertEquals("button class", JButton.class, buttonArea.getComponent(4).getClass());
        button = (JButton) (buttonArea.getComponent(4));
        assertEquals("button Icon", option5, button.getIcon());
        checkButton(button, "", threshold, 0, 1, new Insets(2, 14, 2, 14));
    }

    public void testAddButtonComponents() {
        Object option1 = new JRadioButton("Tarara");
        Object option2 = new Integer(100);
        Object option3 = "Eminem must tsum menimE";
        Object option4 = new Button("Tarara");
        Container buttonArea = new JPanel();
        buttonArea.setLayout(new ButtonAreaLayout(true, 6));
        paneUI.addButtonComponents(buttonArea, new Object[] { option1, option2, option3,
                option4 }, 0);
        assertEquals("number of buttons", 4, buttonArea.getComponentCount());
        assertEquals("button class", JRadioButton.class, buttonArea.getComponent(0).getClass());
        assertEquals("button text", "Tarara", ((JRadioButton) (buttonArea.getComponent(0)))
                .getText());
        assertEquals("button class", JButton.class, buttonArea.getComponent(1).getClass());
        assertEquals("button text", "100", ((JButton) (buttonArea.getComponent(1))).getText());
        assertEquals("button class", JButton.class, buttonArea.getComponent(2).getClass());
        assertEquals("button text", option3, ((JButton) (buttonArea.getComponent(2))).getText());
        assertEquals("button class", Button.class, buttonArea.getComponent(3).getClass());
        assertEquals("button text", "Tarara", ((Button) (buttonArea.getComponent(3)))
                .getLabel());
    }

    public void testCreateButtonActionListener() {
        ActionListener listener1 = paneUI.createButtonActionListener(0);
        ActionListener listener2 = paneUI.createButtonActionListener(1);
        assertTrue("listener is not null", listener1 != null);
        assertTrue("listener is not null", listener2 != null);
        assertEquals("listener's class ",
                "javax.swing.plaf.basic.BasicOptionPaneUI$ButtonActionListener", listener1
                        .getClass().getName());
        assertTrue("listener is not shared", listener1 != listener2);
    }

    public void testGetButtons() {
        Object[] buttons = null;
        paneUI.optionPane = new JOptionPane();
        paneUI.installDefaults();
        paneUI.optionPane.setOptionType(JOptionPane.YES_NO_OPTION);
        buttons = paneUI.getButtons();
        assertEquals("number of buttons", 2, buttons.length);
        assertEquals("button text", "Yes", buttons[0].toString());
        assertEquals("button text", "No", buttons[1].toString());
        paneUI.optionPane.setOptionType(JOptionPane.YES_NO_CANCEL_OPTION);
        buttons = paneUI.getButtons();
        assertEquals("number of buttons", 3, buttons.length);
        assertEquals("button text", "Yes", buttons[0].toString());
        assertEquals("button text", "No", buttons[1].toString());
        assertEquals("button text", "Cancel", buttons[2].toString());
        paneUI.optionPane.setOptionType(JOptionPane.CANCEL_OPTION);
        buttons = paneUI.getButtons();
        assertEquals("number of buttons", 2, buttons.length);
        assertEquals("button text", "OK", buttons[0].toString());
        assertEquals("button text", "Cancel", buttons[1].toString());
        paneUI.optionPane.setOptionType(JOptionPane.CLOSED_OPTION);
        buttons = paneUI.getButtons();
        assertEquals("number of buttons", 1, buttons.length);
        assertEquals("button text", "OK", buttons[0].toString());
        Object option1 = new JButton("Tarara");
        Object option2 = new Integer(100);
        Object option3 = "Eminem must tsum menimE";
        paneUI.optionPane.setOptions(new Object[] { option1, option2, option3 });
        buttons = paneUI.getButtons();
        assertEquals("number of buttons", 3, buttons.length);
        assertEquals("button ", option1, buttons[0]);
        assertEquals("button ", option2, buttons[1]);
        assertEquals("button ", option3, buttons[2]);
    }

    // Regression for HARMONY-2901
    public void testGetButtonsNull() {
        assertNull(paneUI.optionPane);
        assertNull(paneUI.getButtons());
    }

    public void testGetSizeButtonsToSameWidth() {
        assertTrue(paneUI.getSizeButtonsToSameWidth());
    }

    public void testGetInitialValueIndex() {
        JOptionPane optionPane = new JOptionPane("Message", JOptionPane.ERROR_MESSAGE,
                JOptionPane.CLOSED_OPTION, null, new Object[] { "1", "2", "3" }, "1");
        paneUI = (BasicOptionPaneUI) optionPane.getUI();
        assertEquals(0, paneUI.getInitialValueIndex());
        optionPane = new JOptionPane("Message", JOptionPane.ERROR_MESSAGE,
                JOptionPane.CLOSED_OPTION, null, new Object[] { "1", "2", "3", "2" }, "3");
        paneUI = (BasicOptionPaneUI) optionPane.getUI();
        assertEquals(2, paneUI.getInitialValueIndex());
        optionPane = new JOptionPane("Message", JOptionPane.ERROR_MESSAGE,
                JOptionPane.CLOSED_OPTION, null, new Object[] { "1", "2", "3", "2", "2" }, "2");
        paneUI = (BasicOptionPaneUI) optionPane.getUI();
        assertEquals(4, paneUI.getInitialValueIndex());
        optionPane = new JOptionPane("Message", JOptionPane.ERROR_MESSAGE,
                JOptionPane.CLOSED_OPTION, null, new Object[] { "1", "2", "3", "2" }, null);
        paneUI = (BasicOptionPaneUI) optionPane.getUI();
        assertEquals(-1, paneUI.getInitialValueIndex());
        optionPane = new JOptionPane("Message", JOptionPane.ERROR_MESSAGE,
                JOptionPane.CLOSED_OPTION, null, new Object[] { "1", "2", "3", "2" }, "4");
        paneUI = (BasicOptionPaneUI) optionPane.getUI();
        assertEquals(-1, paneUI.getInitialValueIndex());
        optionPane = new JOptionPane("Message", JOptionPane.ERROR_MESSAGE,
                JOptionPane.CLOSED_OPTION, null, null, null);
        paneUI = (BasicOptionPaneUI) optionPane.getUI();
        assertEquals(0, paneUI.getInitialValueIndex());
    }

    // Regression for HARMONY-2901
    public void testGetInitialValueIndexNull() throws Exception {
        assertNull(paneUI.optionPane);
        assertEquals(-1, paneUI.getInitialValueIndex());
    }

    public void testResetInputValue() {
        //TODO Implement resetInputValue().
    }
}
