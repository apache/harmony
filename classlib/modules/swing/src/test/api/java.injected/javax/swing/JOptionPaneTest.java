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
 * Created on 04.05.2005

 */
package javax.swing;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.OptionPaneUI;

public class JOptionPaneTest extends SwingTestCase {
    JOptionPane pane = null;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pane = new JOptionPane();
    }

    public void testGetAccessibleContext() {
        boolean assertedValue = (pane.getAccessibleContext() != null && pane
                .getAccessibleContext().getClass().getName().equals(
                        "javax.swing.JOptionPane$AccessibleJOptionPane"));
        assertTrue("AccessibleContext created properly ", assertedValue);
        assertEquals("AccessibleRole", AccessibleRole.OPTION_PANE, pane.getAccessibleContext()
                .getAccessibleRole());
    }

    public void testParamString() {
        assertTrue("ParamString returns a string ", pane.toString() != null);
    }

    public void testGetUIClassID() {
        assertEquals("UI class ID", "OptionPaneUI", pane.getUIClassID());
    }

    public void testUpdateUI() {
        OptionPaneUI ui = new OptionPaneUI() {
            @Override
            public void selectInitialValue(JOptionPane arg0) {
            }

            @Override
            public boolean containsCustomComponents(JOptionPane arg0) {
                return false;
            }
        };
        pane.setUI(ui);
        assertEquals(ui, pane.getUI());
        pane.updateUI();
        assertNotSame(ui, pane.getUI());
    }

    /*
     * Class under test for void JOptionPane()
     */
    public void testJOptionPane() {
        assertEquals("message", "JOptionPane message", pane.getMessage());
        assertEquals("message type", JOptionPane.PLAIN_MESSAGE, pane.getMessageType());
        assertEquals("option type", JOptionPane.DEFAULT_OPTION, pane.getOptionType());
        assertNull("icon", pane.getIcon());
        assertTrue("options", pane.getOptions() == null || pane.getOptions().length == 0);
        assertNull("initial value", pane.getInitialValue());
        assertEquals("input value", JOptionPane.UNINITIALIZED_VALUE, pane.getInputValue());
        assertEquals(1, pane.getPropertyChangeListeners().length);
    }

    /*
     * Class under test for void JOptionPane(Object)
     */
    public void testJOptionPaneObject() {
        String message = "message";
        pane = new JOptionPane(message);
        assertEquals("message", message, pane.getMessage());
        assertEquals("message type", JOptionPane.PLAIN_MESSAGE, pane.getMessageType());
        assertEquals("option type", JOptionPane.DEFAULT_OPTION, pane.getOptionType());
        assertNull("icon", pane.getIcon());
        assertTrue("options", pane.getOptions() == null || pane.getOptions().length == 0);
        assertNull("initial value", pane.getInitialValue());
        assertEquals("input value", JOptionPane.UNINITIALIZED_VALUE, pane.getInputValue());
    }

    /*
     * Class under test for void JOptionPane(Object, int)
     */
    public void testJOptionPaneObjectint() {
        String message = "message";
        int messageType1 = JOptionPane.ERROR_MESSAGE;
        int messageType2 = -100;
        pane = new JOptionPane(message, messageType1);
        assertEquals("message", message, pane.getMessage());
        assertEquals("message type", messageType1, pane.getMessageType());
        assertEquals("option type", JOptionPane.DEFAULT_OPTION, pane.getOptionType());
        assertNull("icon", pane.getIcon());
        assertTrue("options", pane.getOptions() == null || pane.getOptions().length == 0);
        assertNull("initial value", pane.getInitialValue());
        assertEquals("input value", JOptionPane.UNINITIALIZED_VALUE, pane.getInputValue());
        boolean thrown = false;
        try {
            pane = new JOptionPane(message, messageType2);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue("exception thrown", thrown);
    }

    /*
     * Class under test for void JOptionPane(Object, int, int)
     */
    public void testJOptionPaneObjectintint() {
        String message = "message";
        int messageType1 = JOptionPane.ERROR_MESSAGE;
        int messageType2 = -100;
        int optionType1 = JOptionPane.CANCEL_OPTION;
        int optionType2 = -100;
        pane = new JOptionPane(message, messageType1, optionType1);
        assertEquals("message", message, pane.getMessage());
        assertEquals("message type", messageType1, pane.getMessageType());
        assertEquals("option type", optionType1, pane.getOptionType());
        assertNull("icon", pane.getIcon());
        assertTrue("options", pane.getOptions() == null || pane.getOptions().length == 0);
        assertNull("initial value", pane.getInitialValue());
        assertEquals("input value", JOptionPane.UNINITIALIZED_VALUE, pane.getInputValue());
        boolean thrown = false;
        try {
            pane = new JOptionPane(message, messageType2, optionType1);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue("exception thrown", thrown);
        thrown = false;
        try {
            pane = new JOptionPane(message, messageType1, optionType2);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue("exception thrown", thrown);
    }

    /*
     * Class under test for void JOptionPane(Object, int, int, Icon)
     */
    public void testJOptionPaneObjectintintIcon() {
        String message = "message";
        Icon icon1 = new ImageIcon();
        Icon icon2 = null;
        int messageType1 = JOptionPane.ERROR_MESSAGE;
        int messageType2 = -100;
        int optionType1 = JOptionPane.CANCEL_OPTION;
        int optionType2 = -100;
        pane = new JOptionPane(message, messageType1, optionType1, icon1);
        assertEquals("message", message, pane.getMessage());
        assertEquals("message type", messageType1, pane.getMessageType());
        assertEquals("option type", optionType1, pane.getOptionType());
        assertEquals("icon", icon1, pane.getIcon());
        assertTrue("options", pane.getOptions() == null || pane.getOptions().length == 0);
        assertNull("initial value", pane.getInitialValue());
        assertEquals("input value", JOptionPane.UNINITIALIZED_VALUE, pane.getInputValue());
        boolean thrown = false;
        try {
            pane = new JOptionPane(message, messageType2, optionType1, icon1);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue("exception's been thrown", thrown);
        thrown = false;
        try {
            pane = new JOptionPane(message, messageType1, optionType2, icon1);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue("exception's been thrown", thrown);
        thrown = false;
        try {
            pane = new JOptionPane(message, messageType1, optionType1, icon2);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertFalse("exception's not been thrown", thrown);
    }

    /*
     * Class under test for void JOptionPane(Object, int, int, Icon, Object[])
     */
    public void testJOptionPaneObjectintintIconObjectArray() {
        String message = "message";
        Icon icon1 = new ImageIcon();
        Icon icon2 = null;
        int messageType1 = JOptionPane.ERROR_MESSAGE;
        int messageType2 = -100;
        int optionType1 = JOptionPane.CANCEL_OPTION;
        int optionType2 = -100;
        Object[] options1 = new Object[] { new JPanel(), "message" };
        Object[] options2 = new Object[] { new InputMap(), new ActionMap() };
        pane = new JOptionPane(message, messageType1, optionType1, icon1, options1);
        assertEquals("message", message, pane.getMessage());
        assertEquals("message type", messageType1, pane.getMessageType());
        assertEquals("option type", optionType1, pane.getOptionType());
        assertEquals("icon", icon1, pane.getIcon());
        assertEquals("options", options1.length, pane.getOptions().length);
        assertEquals("options", options1[0], pane.getOptions()[0]);
        assertEquals("options", options1[1], pane.getOptions()[1]);
        assertNull("initial value", pane.getInitialValue());
        assertEquals("input value", JOptionPane.UNINITIALIZED_VALUE, pane.getInputValue());
        boolean thrown = false;
        try {
            pane = new JOptionPane(message, messageType2, optionType1, icon1, options1);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue("exception's been thrown", thrown);
        thrown = false;
        try {
            pane = new JOptionPane(message, messageType1, optionType2, icon1, options1);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue("exception's been thrown", thrown);
        thrown = false;
        try {
            pane = new JOptionPane(message, messageType1, optionType1, icon2, options1);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertFalse("exception's not been thrown", thrown);
        thrown = false;
        try {
            pane = new JOptionPane(message, messageType1, optionType1, icon1, options2);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertFalse("exception's not been thrown", thrown);
    }

    /*
     * Class under test for void JOptionPane(Object, int, int, Icon, Object[], Object)
     */
    public void testJOptionPaneObjectintintIconObjectArrayObject() {
        String message = "message";
        Icon icon1 = new ImageIcon();
        Icon icon2 = null;
        int messageType1 = JOptionPane.ERROR_MESSAGE;
        int messageType2 = -100;
        int optionType1 = JOptionPane.CANCEL_OPTION;
        int optionType2 = -100;
        Object initialSelection = "asdasd";
        Object[] options1 = new Object[] { new JPanel(), initialSelection };
        Object[] options2 = new Object[] { new InputMap(), initialSelection };
        pane = new JOptionPane(message, messageType1, optionType1, icon1, options1,
                initialSelection);
        assertEquals("message", message, pane.getMessage());
        assertEquals("message type", messageType1, pane.getMessageType());
        assertEquals("option type", optionType1, pane.getOptionType());
        assertEquals("icon", icon1, pane.getIcon());
        assertEquals("options", options1.length, pane.getOptions().length);
        assertEquals("options", options1[0], pane.getOptions()[0]);
        assertEquals("options", options1[1], pane.getOptions()[1]);
        assertEquals("initial value", initialSelection, pane.getInitialValue());
        assertEquals("input value", JOptionPane.UNINITIALIZED_VALUE, pane.getInputValue());
        boolean thrown = false;
        try {
            pane = new JOptionPane(message, messageType2, optionType1, icon1, options1,
                    initialSelection);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue("exception's been thrown", thrown);
        thrown = false;
        try {
            pane = new JOptionPane(message, messageType1, optionType2, icon1, options1,
                    initialSelection);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue("exception's been thrown", thrown);
        thrown = false;
        try {
            pane = new JOptionPane(message, messageType1, optionType1, icon2, options1,
                    initialSelection);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertFalse("exception's not been thrown", thrown);
        pane = new JOptionPane(message, messageType1, optionType1, icon1, options2, null);
        assertEquals("message", message, pane.getMessage());
        assertEquals("message type", messageType1, pane.getMessageType());
        assertEquals("option type", optionType1, pane.getOptionType());
        assertEquals("icon", icon1, pane.getIcon());
        assertEquals("options", options1.length, pane.getOptions().length);
        assertEquals("options", options2[0], pane.getOptions()[0]);
        assertEquals("options", options2[1], pane.getOptions()[1]);
        assertNull("initial value", pane.getInitialValue());
        pane = new JOptionPane(message, messageType1, optionType1, icon1, null,
                initialSelection);
        assertEquals("message", message, pane.getMessage());
        assertEquals("message type", messageType1, pane.getMessageType());
        assertEquals("option type", optionType1, pane.getOptionType());
        assertEquals("icon", icon1, pane.getIcon());
        assertEquals("initial value", initialSelection, pane.getInitialValue());
    }

    /*
     * Class under test for String showInputDialog(Object)
     */
    public void testShowInputDialogObject() {
        //TODO Implement showInputDialog().
    }

    /*
     * Class under test for String showInputDialog(Object, Object)
     */
    public void testShowInputDialogObjectObject() {
        //TODO Implement showInputDialog().
    }

    /*
     * Class under test for String showInputDialog(Component, Object)
     */
    public void testShowInputDialogComponentObject() {
        //TODO Implement showInputDialog().
    }

    /*
     * Class under test for String showInputDialog(Component, Object, Object)
     */
    public void testShowInputDialogComponentObjectObject() {
        //TODO Implement showInputDialog().
    }

    /*
     * Class under test for String showInputDialog(Component, Object, String, int)
     */
    public void testShowInputDialogComponentObjectStringint() {
        //TODO Implement showInputDialog().
    }

    /*
     * Class under test for Object showInputDialog(Component, Object, String, int, Icon, Object[], Object)
     */
    public void testShowInputDialogComponentObjectStringintIconObjectArrayObject() {
        //TODO Implement showInputDialog().
    }

    /*
     * Class under test for void showMessageDialog(Component, Object)
     */
    public void testShowMessageDialogComponentObject() {
        //TODO Implement showMessageDialog().
    }

    /*
     * Class under test for void showMessageDialog(Component, Object, String, int)
     */
    public void testShowMessageDialogComponentObjectStringint() {
        //TODO Implement showMessageDialog().
    }

    /*
     * Class under test for void showMessageDialog(Component, Object, String, int, Icon)
     */
    public void testShowMessageDialogComponentObjectStringintIcon() {
        //TODO Implement showMessageDialog().
    }

    /*
     * Class under test for int showConfirmDialog(Component, Object)
     */
    public void testShowConfirmDialogComponentObject() {
        //TODO Implement showConfirmDialog().
    }

    /*
     * Class under test for int showConfirmDialog(Component, Object, String, int)
     */
    public void testShowConfirmDialogComponentObjectStringint() {
        //TODO Implement showConfirmDialog().
    }

    /*
     * Class under test for int showConfirmDialog(Component, Object, String, int, int)
     */
    public void testShowConfirmDialogComponentObjectStringintint() {
        //TODO Implement showConfirmDialog().
    }

    /*
     * Class under test for int showConfirmDialog(Component, Object, String, int, int, Icon)
     */
    public void testShowConfirmDialogComponentObjectStringintintIcon() {
        //TODO Implement showConfirmDialog().
    }

    public void testShowOptionDialog() {
        //TODO Implement showOptionDialog().
    }

    public void testCreateInternalFrame() {
        JDesktopPane deskTop = new JDesktopPane();
        JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        String title = "title-title";
        JInternalFrame iFrame = null;
        panel.setPreferredSize(new Dimension(300, 300));
        boolean thrown = false;
        try {
            iFrame = pane.createInternalFrame(panel, title);
        } catch (RuntimeException e) {
            assertEquals("message",
                    "JOptionPane: parentComponent does not have a valid parent", e.getMessage());
            thrown = true;
        }
        assertTrue("runtime exception's been thrown", thrown);
        frame.getContentPane().add(panel);
        iFrame = pane.createInternalFrame(panel, title);
        assertEquals("title", title, iFrame.getTitle());
        assertEquals("parent", panel.getParent(), iFrame.getParent());
        assertEquals("pane", pane, ((JPanel) iFrame.getRootPane().getLayeredPane()
                .getComponent(0)).getComponent(0));
        deskTop.add(panel);
        iFrame = pane.createInternalFrame(panel, title);
        assertEquals("title", title, iFrame.getTitle());
        assertEquals("parent", deskTop, iFrame.getParent());
        assertEquals("pane", pane, ((JPanel) ((JLayeredPane) ((JRootPane) iFrame
                .getComponent(0)).getComponent(1)).getComponent(0)).getComponent(0));
        frame.dispose();
    }

    public void testCreateDialog() {
        JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        String title = "title-title";
        panel.setPreferredSize(new Dimension(300, 300));
        frame.getContentPane().add(panel);
        JDialog dialog = pane.createDialog(panel, title);
        assertEquals("title", title, dialog.getTitle());
        assertFalse("resizable", dialog.isResizable());
        assertEquals("pane", pane, ((JPanel) ((JLayeredPane) ((JRootPane) dialog
                .getComponent(0)).getComponent(1)).getComponent(0)).getComponent(0));
        assertTrue(dialog.isModal());
        frame.dispose();
    }

    /*
     * Class under test for void showInternalMessageDialog(Component, Object)
     */
    public void testShowInternalMessageDialogComponentObject() {
        //TODO Implement showInternalMessageDialog().
        //        JFrame frame = new JFrame();
        //        JPanel panel = new JPanel();
        //        panel.setPreferredSize(new Dimension(300, 300));
        //        frame.getContentPane().add(panel);
        //        frame.pack();
        //        frame.setVisible(true);
        //        JOptionPane.showInternalMessageDialog(frame.getContentPane(), "Bom shankar!");
    }

    /*
     * Class under test for void showInternalMessageDialog(Component, Object, String, int)
     */
    public void testShowInternalMessageDialogComponentObjectStringint() {
        //TODO Implement showInternalMessageDialog().
    }

    /*
     * Class under test for void showInternalMessageDialog(Component, Object, String, int, Icon)
     */
    public void testShowInternalMessageDialogComponentObjectStringintIcon() {
        //TODO Implement showInternalMessageDialog().
    }

    /*
     * Class under test for int showInternalConfirmDialog(Component, Object)
     */
    public void testShowInternalConfirmDialogComponentObject() {
        //TODO Implement showInternalConfirmDialog().
    }

    /*
     * Class under test for int showInternalConfirmDialog(Component, Object, String, int)
     */
    public void testShowInternalConfirmDialogComponentObjectStringint() {
        //TODO Implement showInternalConfirmDialog().
    }

    /*
     * Class under test for int showInternalConfirmDialog(Component, Object, String, int, int)
     */
    public void testShowInternalConfirmDialogComponentObjectStringintint() {
        //TODO Implement showInternalConfirmDialog().
    }

    /*
     * Class under test for int showInternalConfirmDialog(Component, Object, String, int, int, Icon)
     */
    public void testShowInternalConfirmDialogComponentObjectStringintintIcon() {
        //TODO Implement showInternalConfirmDialog().
    }

    public void testShowInternalOptionDialog() {
        //TODO Implement showInternalOptionDialog().
    }

    /*
     * Class under test for String showInternalInputDialog(Component, Object)
     */
    public void testShowInternalInputDialogComponentObject() {
        //TODO Implement showInternalInputDialog().
    }

    /*
     * Class under test for String showInternalInputDialog(Component, Object, String, int)
     */
    public void testShowInternalInputDialogComponentObjectStringint() {
        //TODO Implement showInternalInputDialog().
    }

    /*
     * Class under test for Object showInternalInputDialog(Component, Object, String, int, Icon, Object[], Object)
     */
    public void testShowInternalInputDialogComponentObjectStringintIconObjectArrayObject() {
        //TODO Implement showInternalInputDialog().
    }

    public void testGetFrameForComponent() {
        Frame defaultFrame = JOptionPane.getRootFrame();
        JPanel panel = new JPanel();
        JFrame frame = new JFrame();
        JFrame rootFrame = new JFrame();
        assertEquals("frame", defaultFrame, JOptionPane.getFrameForComponent(null));
        assertEquals("frame", defaultFrame, JOptionPane.getFrameForComponent(panel));
        frame.getContentPane().add(panel);
        assertEquals("frame", frame, JOptionPane.getFrameForComponent(panel));
        assertEquals("frame", frame, JOptionPane.getFrameForComponent(frame));
        JOptionPane.setRootFrame(rootFrame);
        assertEquals("frame", rootFrame, JOptionPane.getFrameForComponent(null));
        assertEquals("frame", frame, JOptionPane.getFrameForComponent(panel));
        frame.dispose();
        rootFrame.dispose();
    }

    public void testGetDesktopPaneForComponent() {
        JPanel panel = new JPanel();
        JDesktopPane frame = new JDesktopPane();
        assertNull("frame", JOptionPane.getDesktopPaneForComponent(null));
        assertNull("frame", JOptionPane.getDesktopPaneForComponent(panel));
        frame.add(panel);
        assertEquals("frame", frame, JOptionPane.getDesktopPaneForComponent(panel));
    }

    public void testSetRootFrame() {
        Frame frame1 = new Frame();
        Frame frame2 = new JFrame();
        JOptionPane.setRootFrame(frame1);
        assertEquals("root frame ", frame1, JOptionPane.getRootFrame());
        JOptionPane.setRootFrame(frame2);
        assertEquals("root frame ", frame2, JOptionPane.getRootFrame());
        JOptionPane.setRootFrame(null);
        frame1.dispose();
        frame2.dispose();
    }

    public void testGetRootFrame() {
        Frame frame1 = JOptionPane.getRootFrame();
        frame1.add(new JPanel());
        frame1.add(new JPanel());
        frame1.add(new JPanel());
        Frame frame2 = JOptionPane.getRootFrame();
        assertNotNull("root frame is not null", frame1);
        assertTrue("root frame is shared", frame1 == frame2);
    }

    /**
     * is beinng tested in BasicOptionPaneUITest
     */
    public void testSetUIOptionPaneUI() {
    }

    public void testGetUI() {
        assertTrue("ui is returned ", pane.getUI() != null);
    }

    public void testSetMessage() {
        PropertyChangeController listener1 = new PropertyChangeController();
        String message1 = "message1";
        String message2 = "message2";
        pane.addPropertyChangeListener(listener1);
        pane.setMessage(message1);
        listener1.checkPropertyFired(pane, "message", "JOptionPane message", message1);
        assertEquals("message", message1, pane.getMessage());
        listener1.reset();
        pane.setMessage(message2);
        listener1.checkPropertyFired(pane, "message", message1, message2);
        assertEquals("message", message2, pane.getMessage());
        listener1.reset();
        pane.setMessage(message2);
        assertFalse("event's not been fired ", listener1.isChanged());
        listener1.reset();
    }

    public void testGetMessage() {
        assertEquals("message", "JOptionPane message", pane.getMessage());
    }

    public void testSetIcon() {
        Icon icon1 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        Icon icon2 = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        PropertyChangeController listener1 = new PropertyChangeController();
        pane.addPropertyChangeListener(listener1);
        pane.setIcon(icon1);
        listener1.checkPropertyFired(pane, "icon", null, icon1);
        assertEquals("icon", icon1, pane.getIcon());
        listener1.reset();
        pane.setIcon(icon2);
        listener1.checkPropertyFired(pane, "icon", icon1, icon2);
        assertEquals("icon", icon2, pane.getIcon());
        listener1.reset();
        pane.setIcon(icon2);
        assertFalse("event's not been fired ", listener1.isChanged());
        listener1.reset();
        pane.setIcon(null);
        listener1.checkPropertyFired(pane, "icon", icon2, null);
        assertNull("icon", pane.getIcon());
        listener1.reset();
    }

    public void testGetIcon() {
        assertNull("icon", pane.getIcon());
    }

    public void testSetValue() {
        JButton button1 = new JButton("1");
        JButton button2 = new JButton("2");
        PropertyChangeController listener1 = new PropertyChangeController();
        pane.addPropertyChangeListener(listener1);
        pane.setValue(button1);
        listener1.checkPropertyFired(pane, "value", JOptionPane.UNINITIALIZED_VALUE, button1);
        assertEquals("value", button1, pane.getValue());
        listener1.reset();
        pane.setValue(button2);
        listener1.checkPropertyFired(pane, "value", button1, button2);
        assertEquals("value", button2, pane.getValue());
    }

    public void testGetValue() {
        assertEquals("value", JOptionPane.UNINITIALIZED_VALUE, pane.getValue());
    }

    public void testSetOptions() {
        Object options2[] = new Object[] { new JButton("1"), new ImageIcon(), "asdasd" };
        Object options22[] = options2.clone();
        Object options3[] = new Object[] { "asd", new InputMap(), new JPanel() };
        Object options32[] = options3.clone();
        PropertyChangeController listener1 = new PropertyChangeController();
        pane.addPropertyChangeListener(listener1);
        pane.setOptions(options2);
        listener1.checkPropertyFired(pane, "options", null, options2);
        assertTrue("unique", options2 != pane.getOptions());
        assertEquals("options", options22.length, pane.getOptions().length);
        assertEquals("options", options22[0], pane.getOptions()[0]);
        assertEquals("options", options22[1], pane.getOptions()[1]);
        assertEquals("options", options22[2], pane.getOptions()[2]);
        pane.setOptions(options3);
        listener1.checkPropertyFired(pane, "options", options2, options3);
        assertEquals("options", options32.length, pane.getOptions().length);
        assertEquals("options", options32[0], pane.getOptions()[0]);
        assertEquals("options", options32[1], pane.getOptions()[1]);
        assertEquals("options", options32[2], pane.getOptions()[2]);
        listener1.reset();
        pane.setOptions(options3);
        assertFalse("event's not been fired ", listener1.isChanged());
    }

    public void testGetOptions() {
        assertTrue("options", pane.getOptions() == null || pane.getOptions().length == 0);

        assertNull(new JOptionPane().getOptions());
    }

    public void testSetInitialValue() {
        JButton button1 = new JButton("1");
        JButton button2 = new JButton("2");
        pane.setWantsInput(false);
        PropertyChangeController listener1 = new PropertyChangeController();
        pane.addPropertyChangeListener(listener1);
        pane.setInitialValue(button1);
        listener1.checkPropertyFired(pane, "initialValue", null, button1);
        assertEquals("InitialValue", button1, pane.getInitialValue());
        pane.setWantsInput(true);
        listener1.reset();
        pane.setInitialValue(button2);
        listener1.checkPropertyFired(pane, "initialValue", button1, button2);
        assertEquals("InitialValue", button2, pane.getInitialValue());
    }

    public void testGetInitialValue() {
        assertNull("InitialValue", pane.getInitialValue());
    }

    public void testSetMessageType() {
        PropertyChangeController listener1 = new PropertyChangeController();
        pane.addPropertyChangeListener(listener1);
        pane.setMessageType(JOptionPane.ERROR_MESSAGE);
        listener1.checkPropertyFired(pane, "messageType",
                new Integer(JOptionPane.PLAIN_MESSAGE), new Integer(JOptionPane.ERROR_MESSAGE));
        assertEquals("message type", JOptionPane.ERROR_MESSAGE, pane.getMessageType());
        listener1.reset();
        pane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
        listener1.checkPropertyFired(pane, "messageType",
                new Integer(JOptionPane.ERROR_MESSAGE), new Integer(
                        JOptionPane.INFORMATION_MESSAGE));
        assertEquals("message type", JOptionPane.INFORMATION_MESSAGE, pane.getMessageType());
        listener1.reset();
        boolean thrown = false;
        try {
            pane.setMessageType(100);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue("exception is thrown", thrown);
        pane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
        assertFalse("event's not been fired ", listener1.isChanged());
        listener1.reset();
    }

    public void testGetMessageType() {
        assertEquals("message type", JOptionPane.PLAIN_MESSAGE, pane.getMessageType());
    }

    public void testSetOptionType() {
        PropertyChangeController listener1 = new PropertyChangeController();
        pane.addPropertyChangeListener(listener1);
        pane.setOptionType(JOptionPane.CANCEL_OPTION);
        listener1.checkPropertyFired(pane, "optionType",
                new Integer(JOptionPane.CLOSED_OPTION), new Integer(JOptionPane.CANCEL_OPTION));
        assertEquals("option type", JOptionPane.CANCEL_OPTION, pane.getOptionType());
        listener1.reset();
        pane.setOptionType(JOptionPane.OK_OPTION);
        listener1.checkPropertyFired(pane, "optionType",
                new Integer(JOptionPane.CANCEL_OPTION), new Integer(JOptionPane.OK_OPTION));
        assertEquals("option type", JOptionPane.OK_OPTION, pane.getOptionType());
        listener1.reset();
        boolean thrown = false;
        try {
            pane.setOptionType(100);
        } catch (RuntimeException e) {
            thrown = true;
        }
        assertTrue("exception is thrown", thrown);
        pane.setOptionType(JOptionPane.OK_OPTION);
        assertFalse("event's not been fired ", listener1.isChanged());
        listener1.reset();
    }

    public void testGetOptionType() {
        assertEquals("option type", JOptionPane.CLOSED_OPTION, pane.getOptionType());
    }

    public void testSetSelectionValues() {
        Object buttons2[] = new Object[] { new JButton("1"), new JButton("2") };
        Object buttons3[] = new Object[] { new JButton("1"), new JButton("2") };
        PropertyChangeController listener1 = new PropertyChangeController();
        pane.addPropertyChangeListener(listener1);
        pane.setSelectionValues(buttons2);
        listener1.checkPropertyFired(pane, "selectionValues", null, buttons2);
        listener1.checkPropertyFired(pane, "wantsInput", Boolean.FALSE, Boolean.TRUE);
        assertEquals("SelectionValues", buttons2, pane.getSelectionValues());
        assertTrue("SelectionValues", buttons2 == pane.getSelectionValues());
        assertTrue("wantsInput", pane.getWantsInput());
        pane.setWantsInput(false);
        listener1.reset();
        pane.setSelectionValues(buttons3);
        listener1.checkPropertyFired(pane, "selectionValues", buttons2, buttons3);
        listener1.checkPropertyFired(pane, "wantsInput", Boolean.FALSE, Boolean.TRUE);
        assertEquals("SelectionValues", buttons3, pane.getSelectionValues());
        assertTrue("SelectionValues", buttons3 == pane.getSelectionValues());
        assertTrue("wantsInput", pane.getWantsInput());
        pane.setWantsInput(false);
        listener1.reset();
        pane.setSelectionValues(buttons3);
        listener1.checkLastPropertyFired(pane, "wantsInput", Boolean.FALSE, Boolean.TRUE);
        assertEquals("SelectionValues", buttons3, pane.getSelectionValues());
        assertTrue("SelectionValues", buttons3 == pane.getSelectionValues());
        assertTrue("wantsInput", pane.getWantsInput());
        pane = new JOptionPane();
        pane.setWantsInput(false);
        pane.setSelectionValues(null);
        assertFalse(pane.getWantsInput());
    }

    public void testGetSelectionValues() {
        Object buttons2[] = new Object[] { new JButton("1"), new JButton("2") };
        Object buttons3[] = new Object[] { new JButton("1"), new JButton("2") };
        assertNull("SelectionValues", pane.getSelectionValues());
        pane.setSelectionValues(buttons2);
        assertEquals("SelectionValues", buttons2, pane.getSelectionValues());
        assertTrue("SelectionValues", buttons2 == pane.getSelectionValues());
        pane.setSelectionValues(buttons3);
        assertEquals("SelectionValues", buttons3, pane.getSelectionValues());
        assertTrue("SelectionValues", buttons3 == pane.getSelectionValues());
    }

    public void testSetInitialSelectionValue() {
        Object str1 = "String1";
        Object str2 = "String2";
        pane.setWantsInput(false);
        PropertyChangeController listener1 = new PropertyChangeController();
        pane.addPropertyChangeListener(listener1);
        pane.setInitialSelectionValue(str1);
        listener1.checkPropertyFired(pane, "initialSelectionValue", null, str1);
        pane.setWantsInput(true);
        listener1.reset();
        pane.setInitialSelectionValue(str2);
        listener1.checkPropertyFired(pane, "initialSelectionValue", str1, str2);
    }

    public void testGetInitialSelectionValue() {
        JButton button1 = new JButton("1");
        JButton button2 = new JButton("2");
        assertNull("InitialSelectionValue", pane.getInitialSelectionValue());
        pane.setWantsInput(false);
        pane.setInitialSelectionValue(button1);
        assertEquals("InitialSelectionValue", button1, pane.getInitialSelectionValue());
        pane.setWantsInput(true);
        pane.setInitialSelectionValue(button2);
        assertEquals("InitialSelectionValue", button2, pane.getInitialSelectionValue());
    }

    public void testSetInputValue() {
        PropertyChangeController listener1 = new PropertyChangeController();
        JButton button1 = new JButton("1");
        JButton button2 = new JButton("2");
        pane.addPropertyChangeListener(listener1);
        pane.setInputValue(button1);
        listener1.checkPropertyFired(pane, "inputValue", JOptionPane.UNINITIALIZED_VALUE,
                button1);
        assertEquals("input value", button1, pane.getInputValue());
        listener1.reset();
        pane.setInputValue(button2);
        listener1.checkPropertyFired(pane, "inputValue", button1, button2);
        assertEquals("input value", button2, pane.getInputValue());
        listener1.reset();
        pane.setInputValue(button2);
        assertFalse("event's not been fired ", listener1.isChanged());
    }

    public void testGetInputValue() {
        assertEquals("input value", JOptionPane.UNINITIALIZED_VALUE, pane.getInputValue());
    }

    public void testGetMaxCharactersPerLineCount() {
        assertEquals("num characters", Integer.MAX_VALUE, pane.getMaxCharactersPerLineCount());
    }

    public void testSelectInitialValue() {
        //TODO Implement selectInitialValue().
    }

    public void testSetWantsInput() {
        PropertyChangeController listener1 = new PropertyChangeController();
        pane.addPropertyChangeListener(listener1);
        pane.setWantsInput(true);
        listener1.checkPropertyFired(pane, "wantsInput", Boolean.FALSE, Boolean.TRUE);
        assertTrue("wants input", pane.getWantsInput());
        listener1.reset();
        pane.setWantsInput(false);
        listener1.checkPropertyFired(pane, "wantsInput", Boolean.TRUE, Boolean.FALSE);
        assertFalse("wants input", pane.getWantsInput());
        listener1.reset();
        pane.setWantsInput(false);
        assertFalse("event's not been fired ", listener1.isChanged());
        listener1.reset();
    }

    public void testGetWantsInput() {
        assertFalse("wants input", pane.getWantsInput());
    }
}
