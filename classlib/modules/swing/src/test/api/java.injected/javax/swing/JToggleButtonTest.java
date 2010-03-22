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

public class JToggleButtonTest extends SwingTestCase {
    protected JToggleButton button;

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(JToggleButtonTest.class);
    }

    /*
     * @see AbstractButtonTest#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        button = new JToggleButton();
    }

    /*
     * @see AbstractButtonTest#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetAccessibleContext() {
        boolean assertedValue = (button.getAccessibleContext() != null && button
                .getAccessibleContext().getClass().getName().equals(
                        "javax.swing.JToggleButton$AccessibleJToggleButton"));
        assertTrue("AccessibleContext created properly ", assertedValue);
        assertEquals("AccessibleRole", AccessibleRole.TOGGLE_BUTTON, button
                .getAccessibleContext().getAccessibleRole());
    }

    public void testParamString() {
        assertTrue("ParamString returns a string ", button.toString() != null);
    }

    public void testGetUIClassID() {
        assertEquals("UI class ID", "ToggleButtonUI", button.getUIClassID());
    }

    public void testUpdateUI() {
        ButtonUI ui = new ButtonUI() {
        };
        button.setUI(ui);
        assertEquals(ui, button.getUI());
        button.updateUI();
        assertNotSame(ui, button.getUI());
    }

    /*
     * Class under test for void JToggleButton(String, Icon, boolean)
     */
    public void testJToggleButtonStringIconboolean() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text1 = "texttext1";
        String text2 = "texttext2";
        boolean state1 = true;
        boolean state2 = false;
        button = new JToggleButton(text1, icon1, state1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JToggleButton(text2, icon2, state2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
        assertTrue("button model is of the proper type",
                button.getModel() instanceof JToggleButton.ToggleButtonModel);
    }

    /*
     * Class under test for void JToggleButton(String, Icon)
     */
    public void testJToggleButtonStringIcon() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text1 = "texttext1";
        String text2 = "texttext2";
        boolean state2 = false;
        button = new JToggleButton(text1, icon1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state2, button.isSelected());
        button = new JToggleButton(text2, icon2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
        assertTrue("button model is of the proper type",
                button.getModel() instanceof JToggleButton.ToggleButtonModel);
    }

    /*
     * Class under test for void JToggleButton(Icon, boolean)
     */
    public void testJToggleButtonIconboolean() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text1 = "";
        String text2 = "";
        boolean state1 = true;
        boolean state2 = false;
        button = new JToggleButton(icon1, state1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JToggleButton(icon2, state2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
        assertTrue("button model is of the proper type",
                button.getModel() instanceof JToggleButton.ToggleButtonModel);
    }

    /*
     * Class under test for void JToggleButton(Icon)
     */
    public void testJToggleButtonIcon() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text1 = "";
        String text2 = "";
        boolean state1 = false;
        boolean state2 = false;
        button = new JToggleButton(icon1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JToggleButton(icon2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
        assertTrue("button model is of the proper type",
                button.getModel() instanceof JToggleButton.ToggleButtonModel);
    }

    /*
     * Class under test for void JToggleButton(Action)
     */
    public void testJToggleButtonAction() {
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
        button = new JToggleButton(action);
        assertEquals("icon ", icon, button.getIcon());
        assertEquals("text ", text, button.getText());
        assertEquals("action", action, button.getAction());
        assertEquals("command ", command, button.getActionCommand());
        assertFalse("selected ", button.isSelected());
        assertTrue("enabled ", button.isEnabled());
        action.setEnabled(false);
        button = new JToggleButton(action);
        assertEquals("icon ", icon, button.getIcon());
        assertEquals("text ", text, button.getText());
        assertEquals("action", action, button.getAction());
        assertEquals("command ", command, button.getActionCommand());
        assertFalse("selected ", button.isSelected());
        assertFalse("enabled ", button.isEnabled());
        button = new JToggleButton((Action) null);
        assertNull("icon ", button.getIcon());
        assertNull("text ", button.getText());
        assertNull("action", button.getAction());
        assertNull("command ", button.getActionCommand());
        assertFalse("selected ", button.isSelected());
        assertTrue("enabled ", button.isEnabled());
        assertTrue("button model is of the proper type",
                button.getModel() instanceof JToggleButton.ToggleButtonModel);
    }

    /*
     * Class under test for void JToggleButton(String, boolean)
     */
    public void testJToggleButtonStringboolean() {
        Icon icon1 = null;
        Icon icon2 = null;
        String text1 = "texttext1";
        String text2 = "texttext2";
        boolean state1 = true;
        boolean state2 = false;
        button = new JToggleButton(text1, state1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JToggleButton(text2, state2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
        assertTrue("button model is of the proper type",
                button.getModel() instanceof JToggleButton.ToggleButtonModel);
    }

    /*
     * Class under test for void JToggleButton(String)
     */
    public void testJToggleButtonString() {
        Icon icon1 = null;
        Icon icon2 = null;
        String text1 = "texttext1";
        String text2 = "texttext2";
        boolean state1 = false;
        boolean state2 = false;
        button = new JToggleButton(text1);
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        button = new JToggleButton(text2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("text ", text2, button.getText());
        assertEquals("selected ", state2, button.isSelected());
        assertTrue("button model is of the proper type",
                button.getModel() instanceof JToggleButton.ToggleButtonModel);
    }

    /*
     * Class under test for void JToggleButton()
     */
    public void testJToggleButton() {
        Icon icon1 = null;
        String text1 = "";
        boolean state1 = false;
        button = new JToggleButton();
        assertEquals("icon ", icon1, button.getIcon());
        assertEquals("text ", text1, button.getText());
        assertEquals("selected ", state1, button.isSelected());
        assertTrue("button model is of the proper type",
                button.getModel() instanceof JToggleButton.ToggleButtonModel);
    }

    /*
     * stub to override AbstractButton testcase incorrect for this class
     */
    public void testSetAction1() {
    }
}
