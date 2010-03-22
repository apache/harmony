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
 * Created on 01.02.2005

 */
package javax.swing;

import java.awt.event.ActionEvent;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.ButtonUI;

public class JButtonTest extends AbstractButtonTest {
    protected JButton jbutton;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jbutton = new JButton();
        button = jbutton;
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for void JButton(String, Icon)
     */
    public void testJButtonStringIcon() {
        Icon icon = createNewIcon();
        String text = "texttext";
        jbutton = new JButton(text, icon);
        assertEquals("icon ", icon, jbutton.getIcon());
        assertEquals("text ", text, jbutton.getText());
    }

    /*
     * Class under test for void JButton(Icon)
     */
    public void testJButtonIcon() {
        Icon icon = createNewIcon();
        jbutton = new JButton(icon);
        assertEquals("icon ", icon, jbutton.getIcon());
        assertEquals("text ", "", jbutton.getText());
    }

    /*
     * Class under test for void JButton(Action)
     */
    public void testJButtonAction() {
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
        Icon icon = createNewIcon();
        String text = "texttext";
        MyAction action = new MyAction(text, icon);
        button = new JButton(action);
        assertEquals("icon ", icon, button.getIcon());
        assertEquals("text ", text, button.getText());
        assertEquals("action", action, button.getAction());
        assertEquals("command ", command, button.getActionCommand());
        assertFalse("selected ", button.isSelected());
        assertTrue("enabled ", button.isEnabled());
        action.setEnabled(false);
        button = new JButton(action);
        assertEquals("icon ", icon, button.getIcon());
        assertEquals("text ", text, button.getText());
        assertEquals("action", action, button.getAction());
        assertEquals("command ", command, button.getActionCommand());
        assertFalse("selected ", button.isSelected());
        assertFalse("enabled ", button.isEnabled());
        button = new JButton((Action) null);
        assertNull("icon ", button.getIcon());
        assertNull("text ", button.getText());
        assertNull("action", button.getAction());
        assertNull("command ", button.getActionCommand());
        assertFalse("selected ", button.isSelected());
        assertTrue("enabled ", button.isEnabled());
        assertTrue("button model is of the proper type",
                button.getModel() instanceof DefaultButtonModel);
    }

    /*
     * Class under test for void JButton(String)
     */
    public void testJButtonString() {
        String text = "texttext";
        jbutton = new JButton(text);
        assertNull("icon ", jbutton.getIcon());
        assertEquals("text ", text, jbutton.getText());
    }

    /*
     * Class under test for void JButton()
     */
    public void testJButton() {
        assertNotNull("default buttonModel ", button.getModel());
        assertNull("icon ", jbutton.getIcon());
        assertEquals("text ", "", jbutton.getText());
    }

    public void testGetAccessibleContext() {
        boolean assertedValue = (jbutton.getAccessibleContext() != null && jbutton
                .getAccessibleContext().getClass().getName().equals(
                        "javax.swing.JButton$AccessibleJButton"));
        assertTrue("AccessibleContext created properly ", assertedValue);
        assertEquals("AccessibleRole", AccessibleRole.PUSH_BUTTON, jbutton
                .getAccessibleContext().getAccessibleRole());
    }

    /*
     * Class under test for String paramString()
     */
    @Override
    public void testParamString() {
        assertTrue("ParamString returns a string ", jbutton.toString() != null);
    }

    public void testRemoveNotify() {
        JRootPane pane = new JRootPane();
        assertFalse("initial value for defaultButton ", jbutton.isDefaultButton());
        pane.getContentPane().add(jbutton);
        pane.getContentPane().add(new JButton());
        pane.setDefaultButton(jbutton);
        assertTrue("'ve become defaultButton now ", jbutton.isDefaultButton());
        jbutton.removeNotify();
        assertFalse("is not defaultButton now ", jbutton.isDefaultButton());
        assertNull("rootPane now 've no default jbutton ", pane.getDefaultButton());
    }

    public void testGetUIClassID() {
        assertEquals("UI class ID", "ButtonUI", jbutton.getUIClassID());
    }

    @Override
    public void testUpdateUI() {
        ButtonUI ui = new ButtonUI() {
        };
        jbutton.setUI(ui);
        assertEquals(ui, jbutton.getUI());
        jbutton.updateUI();
        assertNotSame(ui, jbutton.getUI());
    }

    /*
     * Class under test for void configurePropertiesFromAction(Action)
     */
    public void testConfigurePropertiesFromActionAction() {
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        String text1 = "texttext1";
        String text2 = "texttext2";
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
        jbutton.configurePropertiesFromAction(action1);
        assertEquals("icon ", icon1, jbutton.getIcon());
        assertEquals("text ", text1, jbutton.getText());
        jbutton.configurePropertiesFromAction(action2);
        assertEquals("icon ", icon2, jbutton.getIcon());
        assertEquals("text ", text2, jbutton.getText());
        jbutton.configurePropertiesFromAction(null);
        assertNull("icon ", jbutton.getIcon());
        assertNull("text ", jbutton.getText());
        assertNull("action", jbutton.getAction());
        assertNull("command ", jbutton.getActionCommand());
        assertFalse("selected ", jbutton.isSelected());
        assertTrue("enabled ", jbutton.isEnabled());
    }

    public void testSetDefaultCapable() {
        jbutton.setDefaultCapable(false);
        assertFalse("DefaultCapable", jbutton.isDefaultCapable());
        PropertyChangeController listener = new PropertyChangeController();
        jbutton.addPropertyChangeListener(listener);
        jbutton.setDefaultCapable(true);
        assertTrue("DefaultCapable", jbutton.isDefaultCapable());
        listener.checkPropertyFired(jbutton, "defaultCapable", Boolean.FALSE, Boolean.TRUE);
        jbutton.setDefaultCapable(false);
        assertFalse("DefaultCapable", jbutton.isDefaultCapable());
        listener.checkPropertyFired(jbutton, "defaultCapable", Boolean.TRUE, Boolean.FALSE);
    }

    public void testIsDefaultCapable() {
        assertTrue("initial DefaultCapable value", jbutton.isDefaultCapable());
    }

    public void testIsDefaultButton() {
        JRootPane pane = new JRootPane();
        assertFalse("initial value for defaultButton ", jbutton.isDefaultButton());
        pane.getContentPane().add(jbutton);
        pane.getContentPane().add(new JButton());
        assertFalse("is not defaultButton yet ", jbutton.isDefaultButton());
        pane.setDefaultButton(jbutton);
        assertTrue("'ve become defaultButton now ", jbutton.isDefaultButton());
    }

    public void testWriteObject() {
        /*
         String text1 = "can you read betwwen the lines?";
         String text2 = "can you look through the time?";
         JButton button1 = new JButton(text1);
         JButton button2 = new JButton(text2);
         try {
         FileOutputStream fo = new FileOutputStream("tmp");
         ObjectOutputStream so = new ObjectOutputStream(fo);
         so.writeObject(button1);
         so.flush();
         } catch (Exception e) {
         System.out.println(e);
         fail("serialization failed");
         }
         try {
         FileOutputStream fo = new FileOutputStream("tmp");
         ObjectOutputStream so = new ObjectOutputStream(fo);
         so.writeObject(button2);
         so.flush();
         } catch (Exception e) {
         fail("serialization failed");
         }
         */
    }

    public void testReadObject() {
        /*
         String text1 = "can you read betwwen the lines?";
         String text2 = "can you look through the time?";
         JButton button1 = new JButton(text1);
         JButton button2 = new JButton(text2);
         try {
         FileOutputStream fo = new FileOutputStream("tmp");
         ObjectOutputStream so = new ObjectOutputStream(fo);
         so.writeObject(button1);
         so.flush();
         } catch (Exception e) {
         fail("serialization failed");
         }
         try {
         FileInputStream fi = new FileInputStream("tmp");
         ObjectInputStream si = new ObjectInputStream(fi);
         JButton ressurectedButton = (JButton)si.readObject();
         assertEquals("text ", text1, ressurectedButton.getText());
         } catch (Exception e) {
         fail("deserialization failed");
         }

         try {
         FileOutputStream fo = new FileOutputStream("tmp");
         ObjectOutputStream so = new ObjectOutputStream(fo);
         so.writeObject(button2);
         so.flush();
         } catch (Exception e) {
         fail("serialization failed");
         }
         try {
         FileInputStream fi = new FileInputStream("tmp");
         ObjectInputStream si = new ObjectInputStream(fi);
         JButton ressurectedButton = (JButton)si.readObject();
         assertEquals("text ", text2, ressurectedButton.getText());
         } catch (Exception e) {
         fail("deserialization failed");
         }
         */
    }

    @Override
    public void testGetAlignmentXY() {
        assertEquals("alignmentX ", button.getAlignmentX(), 0f, 1e-5);
        assertEquals("alignmentY ", button.getAlignmentY(), 0.5f, 1e-5);
    }

    @Override
    public void testGetUI() {
        assertTrue("ui is returned ", button.getUI() != null);
    }
}
