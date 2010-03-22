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
 * Created on 25.04.2005

 */
package javax.swing;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.ButtonUI;

public class JRadioButtonTest extends SwingTestCase {
    protected AbstractButton button = null;

    /*
     * @see JToggleButtonTest#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        button = new JRadioButton();
    }

    /*
     * @see JToggleButtonTest#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetAccessibleContext() {
        boolean assertedValue = (button.getAccessibleContext() != null && button
                .getAccessibleContext().getClass().getName().equals(
                        "javax.swing.JRadioButton$AccessibleJRadioButton"));
        assertTrue("AccessibleContext created properly ", assertedValue);
        assertEquals("AccessibleRole", AccessibleRole.RADIO_BUTTON, button
                .getAccessibleContext().getAccessibleRole());
    }

    public void testParamString() {
        assertTrue("ParamString returns a string ", button.toString() != null);
    }

    public void testGetUIClassID() {
        assertEquals("UI class ID", "RadioButtonUI", button.getUIClassID());
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

    public void testConfigurePropertiesFromAction() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text1 = "texttext1";
        String text2 = "texttext2";
        String text3 = "texttext3";
        String text4 = "texttext4";
        AbstractAction action1 = new AbstractAction(text1, icon1) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent event) {
            }
        };
        AbstractAction action2 = new AbstractAction(text2, icon2) {
            private static final long serialVersionUID = 1L;

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

    public void testJRadioButton() {
        assertNull(button.getIcon());
        assertEquals("", button.getText());
        assertFalse(button.isSelected());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
    }

    /*
     * Class under test for void JRadioButton(Action)
     */
    public void testJRadioButtonAction() {
        final String command = "dnammoc";
        class MyAction extends AbstractAction {
            private static final long serialVersionUID = 1L;

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
        button = new JRadioButton(action);
        assertNull("icon ", button.getIcon());
        assertEquals("text ", text, button.getText());
        assertEquals("action", action, button.getAction());
        assertEquals("command ", command, button.getActionCommand());
        assertFalse("selected ", button.isSelected());
        assertFalse("enabled ", button.isEnabled());
        button = new JRadioButton((Action) null);
        assertNull("icon ", button.getIcon());
        assertNull("text ", button.getText());
        assertNull("action", button.getAction());
        assertNull("command ", button.getActionCommand());
        assertFalse("selected ", button.isSelected());
        assertTrue("enabled ", button.isEnabled());
    }

    /*
     * Class under test for void JRadioButton(Icon)
     */
    public void testJRadioButtonIcon() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text1 = "";
        String text2 = "";
        boolean state1 = false;
        boolean state2 = false;
        button = new JRadioButton(icon1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JRadioButton(icon2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
    }

    /*
     * Class under test for void JRadioButton(Icon, boolean)
     */
    public void testJRadioButtonIconboolean() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text1 = "";
        String text2 = "";
        boolean state1 = true;
        boolean state2 = false;
        button = new JRadioButton(icon1, state1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JRadioButton(icon2, state2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
    }

    /*
     * Class under test for void JRadioButton(String)
     */
    public void testJRadioButtonString() {
        Icon icon1 = null;
        Icon icon2 = null;
        String text1 = "texttext1";
        String text2 = "texttext2";
        boolean state1 = false;
        boolean state2 = false;
        button = new JRadioButton(text1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JRadioButton(text2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
    }

    /*
     * Class under test for void JRadioButton(String, boolean)
     */
    public void testJRadioButtonStringboolean() {
        Icon icon1 = null;
        Icon icon2 = null;
        String text1 = "texttext1";
        String text2 = "texttext2";
        boolean state1 = true;
        boolean state2 = false;
        button = new JRadioButton(text1, state1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JRadioButton(text2, state2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
    }

    /*
     * Class under test for void JRadioButton(String, Icon)
     */
    public void testJRadioButtonStringIcon() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text1 = "texttext1";
        String text2 = "texttext2";
        boolean state1 = false;
        boolean state2 = false;
        button = new JRadioButton(text1, icon1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JRadioButton(text2, icon2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
    }

    /*
     * Class under test for void JRadioButton(String, Icon, boolean)
     */
    public void testJRadioButtonStringIconboolean() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text1 = "texttext1";
        String text2 = "texttext2";
        boolean state1 = true;
        boolean state2 = false;
        button = new JRadioButton(text1, icon1, state1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JRadioButton(text2, icon2, state2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
    }
}
