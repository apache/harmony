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
package javax.swing;

import java.awt.event.ActionEvent;
import javax.accessibility.AccessibleRole;
import javax.swing.JToggleButton.ToggleButtonModel;

public class JCheckBoxMenuItemTest extends JMenuItemTest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        menuItem = new JCheckBoxMenuItem();
        button = menuItem;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.JCheckBoxMenuItem.JCheckBoxMenuItem()'
     */
    public void testJCheckBoxMenuItem() {
        assertFalse(menuItem.isSelected());
        assertTrue("default buttonModel ", button.getModel() instanceof ToggleButtonModel);
        assertNull("icon ", button.getIcon());
        assertEquals("text ", "", button.getText());
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
    }

    /*
     * Test method for 'javax.swing.JCheckBoxMenuItem.JCheckBoxMenuItem(Icon)'
     */
    public void testJCheckBoxMenuItemIcon() {
        Icon icon = createNewIcon();
        menuItem = new JCheckBoxMenuItem(icon);
        assertTrue("default buttonModel ", button.getModel() instanceof ToggleButtonModel);
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", "", menuItem.getText());
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
    }

    /*
     * Test method for 'javax.swing.JCheckBoxMenuItem.JCheckBoxMenuItem(String)'
     */
    public void testJCheckBoxMenuItemString() {
        String text = "texttext";
        menuItem = new JCheckBoxMenuItem(text);
        assertFalse(menuItem.isSelected());
        assertTrue("default buttonModel ", button.getModel() instanceof ToggleButtonModel);
        assertNull("icon ", menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
    }

    /*
     * Test method for 'javax.swing.JCheckBoxMenuItem.JCheckBoxMenuItem(String, Icon)'
     */
    public void testJCheckBoxMenuItemStringIcon() {
        Icon icon = createNewIcon();
        String text = "texttext";
        menuItem = new JCheckBoxMenuItem(text, icon);
        assertFalse(menuItem.isSelected());
        assertTrue("default buttonModel ", button.getModel() instanceof ToggleButtonModel);
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
    }

    /*
     * Test method for 'javax.swing.JCheckBoxMenuItem.JCheckBoxMenuItem(Action)'
     */
    public void testJCheckBoxMenuItemAction() {
        final String command = "dnammoc";
        final KeyStroke accelerator = KeyStroke.getKeyStroke('a');
        class MyAction extends AbstractAction {
            private static final long serialVersionUID = 1L;

            public MyAction(final String text, final Icon icon) {
                super(text, icon);
                putValue(Action.ACTION_COMMAND_KEY, command);
                putValue(Action.ACCELERATOR_KEY, accelerator);
            }

            public void actionPerformed(final ActionEvent e) {
            }
        }
        ;
        Icon icon = createNewIcon();
        String text = "texttext";
        MyAction action = new MyAction(text, icon);
        menuItem = new JCheckBoxMenuItem(action);
        assertFalse(menuItem.isSelected());
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        assertEquals("action", action, menuItem.getAction());
        assertEquals("command ", command, menuItem.getActionCommand());
        assertFalse("selected ", menuItem.isSelected());
        assertTrue("enabled ", menuItem.isEnabled());
        assertEquals("accelerator ", accelerator, menuItem.getAccelerator());
        action.setEnabled(false);
        menuItem = new JCheckBoxMenuItem(action);
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        assertEquals("action", action, menuItem.getAction());
        assertEquals("command ", command, menuItem.getActionCommand());
        assertFalse("selected ", menuItem.isSelected());
        assertFalse("enabled ", menuItem.isEnabled());
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
        menuItem = new JCheckBoxMenuItem((Action) null);
        assertNull("icon ", menuItem.getIcon());
        assertNull("text ", menuItem.getText());
        assertNull("action", menuItem.getAction());
        assertNull("command ", menuItem.getActionCommand());
        assertFalse("selected ", menuItem.isSelected());
        assertTrue("enabled ", menuItem.isEnabled());
        assertTrue("default buttonModel ", button.getModel() instanceof ToggleButtonModel);
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
    }

    /*
     * Test method for 'javax.swing.JCheckBoxMenuItem.JCheckBoxMenuItem(String, Icon, boolean)'
     */
    public void testJCheckBoxMenuItemStringIconBoolean() {
        Icon icon = createNewIcon();
        String text = "texttext";
        menuItem = new JCheckBoxMenuItem(text, icon, true);
        assertTrue("default buttonModel ", button.getModel() instanceof ToggleButtonModel);
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
        assertTrue(menuItem.isSelected());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
        menuItem = new JCheckBoxMenuItem(text, icon, false);
        assertFalse(menuItem.isSelected());
    }

    /*
     * Test method for 'javax.swing.JCheckBoxMenuItem.getAccessibleContext()'
     */
    @Override
    public void testGetAccessibleContext() {
        boolean assertedValue = (menuItem.getAccessibleContext() != null && menuItem
                .getAccessibleContext().getClass().getName().equals(
                        "javax.swing.JCheckBoxMenuItem$AccessibleJCheckBoxMenuItem"));
        assertTrue("AccessibleContext created properly ", assertedValue);
        assertEquals("AccessibleRole", AccessibleRole.CHECK_BOX, menuItem
                .getAccessibleContext().getAccessibleRole());
    }

    /*
     * Test method for 'javax.swing.JCheckBoxMenuItem.getUIClassID()'
     */
    @Override
    public void testGetUIClassID() {
        assertEquals("CheckBoxMenuItemUI", menuItem.getUIClassID());
    }

    /*
     * Test method for 'javax.swing.JCheckBoxMenuItem.getSelectedObjects()'
     */
    @Override
    public void testGetSelectedObjects() {
        String text = "texttext";
        menuItem = new JCheckBoxMenuItem(text);
        assertNull(((JCheckBoxMenuItem) menuItem).getSelectedObjects());
        menuItem.setSelected(true);
        assertEquals(1, ((JCheckBoxMenuItem) menuItem).getSelectedObjects().length);
        assertEquals(menuItem.getText(), ((JCheckBoxMenuItem) menuItem).getSelectedObjects()[0]);
    }

    /*
     * Test method for 'javax.swing.JCheckBoxMenuItem.getState()'
     */
    public void testGetSetState() {
        assertEquals("default Selected", ((JCheckBoxMenuItem) menuItem).getState(), menuItem
                .isSelected());
        menuItem.setSelected(true);
        assertEquals("Selected", ((JCheckBoxMenuItem) menuItem).getState(), menuItem
                .isSelected());
        ((JCheckBoxMenuItem) menuItem).setState(false);
        assertEquals("Selected", ((JCheckBoxMenuItem) menuItem).getState(), menuItem
                .isSelected());
    }
}
