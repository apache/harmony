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
 * Created on 01.05.2005

 */
package javax.swing;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.ButtonUI;

public class JCheckBoxTest extends SwingTestCase {
    protected AbstractButton button = null;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        button = new JCheckBox();
    }

    public void testGetAccessibleContext() {
        boolean assertedValue = (button.getAccessibleContext() != null && button
                .getAccessibleContext().getClass().getName().equals(
                        "javax.swing.JCheckBox$AccessibleJCheckBox"));
        assertTrue("AccessibleContext created properly ", assertedValue);
        assertEquals("AccessibleRole", AccessibleRole.CHECK_BOX, button.getAccessibleContext()
                .getAccessibleRole());
    }

    public void testParamString() {
        assertTrue("ParamString returns a string ", button.toString() != null);
    }

    public void testGetUIClassID() {
        assertEquals("UI class ID", "CheckBoxUI", button.getUIClassID());
    }

    public void testUpdateUI() {
        ButtonUI ui = new ButtonUI() {
        };
        button.setUI(ui);
        assertEquals(ui, button.getUI());
        button.updateUI();
        assertNotSame(ui, button.getUI());
    }

    public void testCreateActionPropertyChangeListener() {
        Object res1 = null;
        Object res2 = null;
        AbstractAction action1 = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent event) {
            }
        };
        AbstractAction action2 = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent event) {
            }
        };
        res1 = button.createActionPropertyChangeListener(action1);
        assertNotNull("returned value is not null", res1);
        res2 = button.createActionPropertyChangeListener(action2);
        assertNotNull("returned value is not null", res2);
        res2 = button.createActionPropertyChangeListener(null);
        assertNotNull("returned value is not null", res2);
    }

    @SuppressWarnings("serial")
    public void testConfigurePropertiesFromAction() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text1 = "texttext1";
        String text2 = "texttext2";
        String text3 = "texttext3";
        String text4 = "texttext4";
        AbstractAction action1 = new AbstractAction(text1, icon1) {
            public void actionPerformed(final ActionEvent event) {
            }
        };
        AbstractAction action2 = new AbstractAction(text2, icon2) {
            public void actionPerformed(final ActionEvent event) {
            }
        };
        action1.setEnabled(true);
        action1.putValue(Action.SHORT_DESCRIPTION, text3);
        action1.putValue(Action.MNEMONIC_KEY, new Integer(1));
        button.setAction(action1);
        assertEquals("action ", action1, button.getAction());
        assertTrue("enabled ", button.isEnabled());
        assertTrue("enabled ", action1.isEnabled());
        action1.setEnabled(false);
        button.isEnabled();
        assertFalse("enabled ", button.isEnabled());
        assertFalse("enabled ", action1.isEnabled());
        assertNull("icon ", button.getIcon());
        action1.putValue(Action.SMALL_ICON, icon2);
        assertNull("icon ", button.getIcon());
        button.setIcon(icon2);
        action1.putValue(Action.SMALL_ICON, null);
        assertEquals("icon ", icon2, button.getIcon());
        if (isHarmony()) {
            assertEquals("mnemonic ", 1, button.getMnemonic());
            action1.putValue(Action.MNEMONIC_KEY, new Integer(27));
            assertEquals("mnemonic ", 27, button.getMnemonic());
        }
        assertEquals("text ", text1, button.getText());
        action1.putValue(Action.NAME, text2);
        assertEquals("text ", text2, button.getText());
        assertEquals("ToolTipText ", text3, button.getToolTipText());
        action1.putValue(Action.SHORT_DESCRIPTION, text4);
        assertEquals("ToolTipText ", text4, button.getToolTipText());
        button.setAction(action2);
        action1.putValue(Action.SHORT_DESCRIPTION, text4);
        assertNull("ToolTipText ", button.getToolTipText());
        action2.putValue(Action.SHORT_DESCRIPTION, text4);
        assertEquals("ToolTipText ", text4, button.getToolTipText());
    }

    /*
     * Class under test for void JCheckBox()
     */
    public void testJCheckBox() {
        assertNull(button.getIcon());
        assertEquals("", button.getText());
        assertFalse(button.isSelected());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
    }

    /*
     * Class under test for void JCheckBox(Action)
     */
    @SuppressWarnings("serial")
    public void testJCheckBoxAction() {
        final String command = "dnammoc";
        class MyAction extends AbstractAction {
            public MyAction(final String text, final Icon icon) {
                super(text, icon);
                putValue(Action.ACTION_COMMAND_KEY, command);
            }

            public void actionPerformed(final ActionEvent e) {
            }
        }
        ;
        Icon icon = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text = "texttext";
        MyAction action = new MyAction(text, icon);
        action.setEnabled(false);
        button = new JCheckBox(action);
        assertNull("icon ", button.getIcon());
        assertEquals("text ", text, button.getText());
        assertEquals("action", action, button.getAction());
        assertEquals("command ", command, button.getActionCommand());
        assertFalse("selected ", button.isSelected());
        assertFalse("enabled ", button.isEnabled());
        button = new JCheckBox((Action) null);
        assertNull("icon ", button.getIcon());
        assertNull("text ", button.getText());
        assertNull("action", button.getAction());
        assertNull("command ", button.getActionCommand());
        assertFalse("selected ", button.isSelected());
        assertTrue("enabled ", button.isEnabled());
    }

    /*
     * Class under test for void JCheckBox(Icon)
     */
    public void testJCheckBoxIcon() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text1 = "";
        String text2 = "";
        boolean state1 = false;
        boolean state2 = false;
        button = new JCheckBox(icon1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JCheckBox(icon2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
    }

    /*
     * Class under test for void JCheckBox(Icon, boolean)
     */
    public void testJCheckBoxIconboolean() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text1 = "";
        String text2 = "";
        boolean state1 = true;
        boolean state2 = false;
        button = new JCheckBox(icon1, state1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JCheckBox(icon2, state2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
    }

    /*
     * Class under test for void JCheckBox(String)
     */
    public void testJCheckBoxString() {
        Icon icon1 = null;
        Icon icon2 = null;
        String text1 = "texttext1";
        String text2 = "texttext2";
        boolean state1 = false;
        boolean state2 = false;
        button = new JCheckBox(text1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JCheckBox(text2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
    }

    /*
     * Class under test for void JCheckBox(String, boolean)
     */
    public void testJCheckBoxStringboolean() {
        Icon icon1 = null;
        Icon icon2 = null;
        String text1 = "texttext1";
        String text2 = "texttext2";
        boolean state1 = true;
        boolean state2 = false;
        button = new JCheckBox(text1, state1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JCheckBox(text2, state2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
    }

    /*
     * Class under test for void JCheckBox(String, Icon)
     */
    public void testJCheckBoxStringIcon() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text1 = "texttext1";
        String text2 = "texttext2";
        boolean state1 = false;
        boolean state2 = false;
        button = new JCheckBox(text1, icon1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JCheckBox(text2, icon2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
    }

    /*
     * Class under test for void JCheckBox(String, Icon, boolean)
     */
    public void testJCheckBoxStringIconboolean() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text1 = "texttext1";
        String text2 = "texttext2";
        boolean state1 = true;
        boolean state2 = false;
        button = new JCheckBox(text1, icon1, state1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JCheckBox(text2, icon2, state2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
    }

    public void testIsBorderPaintedFlat() {
        assertFalse("paintedFlat", ((JCheckBox) button).isBorderPaintedFlat());
    }

    public void testSetBorderPaintedFlat() {
        PropertyChangeController listener1 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        ((JCheckBox) button).setBorderPaintedFlat(true);
        listener1.checkPropertyFired(button, "borderPaintedFlat", Boolean.FALSE, Boolean.TRUE);
        assertTrue("borderPaintedFlat", ((JCheckBox) button).isBorderPaintedFlat());
        listener1.reset();
        ((JCheckBox) button).setBorderPaintedFlat(false);
        listener1.checkPropertyFired(button, "borderPaintedFlat", Boolean.TRUE, Boolean.FALSE);
        assertFalse("borderPaintedFlat", ((JCheckBox) button).isBorderPaintedFlat());
    }
}
