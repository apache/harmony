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
 * Created on 31.01.2005

 */
package javax.swing;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.beans.PropertyChangeListener;
import java.util.EventListener;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class AbstractButtonTest extends SwingTestCase {
    class TestButtonModel extends DefaultButtonModel {
        private static final long serialVersionUID = 1L;

        boolean wasPressed;

        boolean wasArmed;

        @Override
        public void setPressed(boolean b) {
            wasPressed = true;
            super.setPressed(b);
        }

        @Override
        public void setArmed(boolean b) {
            wasArmed = true;
            super.setArmed(b);
        }
    }

    class ConcreteActionListener implements ActionListener {
        public ActionEvent eventHappened = null;

        private boolean debugOut = false;

        public ConcreteActionListener() {
        }

        public ConcreteActionListener(final boolean debugOut) {
            this.debugOut = debugOut;
        }

        public void actionPerformed(final ActionEvent event) {
            eventHappened = event;
            if (debugOut) {
                System.out.println("actionPerformed");
                System.out.println("ID " + event.getID());
                System.out.println("Source " + event.getSource());
                System.out.println("Modifiers " + event.getModifiers());
                System.out.println("Command " + event.getActionCommand());
                System.out.println("When " + event.getWhen());
                System.out.println();
            }
        }
    };

    class ConcreteItemListener implements ItemListener {
        public ItemEvent eventHappened = null;

        private boolean debugOut = false;

        public ConcreteItemListener() {
        }

        public ConcreteItemListener(final boolean debugOut) {
            this.debugOut = debugOut;
        }

        public void itemStateChanged(final ItemEvent event) {
            eventHappened = event;
            if (debugOut) {
                System.out.println("itemStateChanged");
                System.out.println("ID " + event.getID());
                System.out.println("Source " + event.getSource());
                System.out.println("StateChange " + event.getStateChange());
                System.out.println("Item " + event.getItem());
                System.out.println("ItemSelectable " + event.getItemSelectable());
                System.out.println();
            }
        }
    };

    class ConcreteChangeListener implements ChangeListener {
        public ChangeEvent eventHappened = null;

        private boolean debugOut = false;

        public ConcreteChangeListener() {
        }

        public ConcreteChangeListener(final boolean debugOut) {
            this.debugOut = debugOut;
        }

        public void stateChanged(final ChangeEvent event) {
            eventHappened = event;
            if (debugOut) {
                System.out.println("stateChanged");
                System.out.println("Class " + event.getClass());
                System.out.println("Source " + event.getSource());
                System.out.println();
            }
        }
    };

    class ConcreteAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        ActionEvent eventHappened;

        public void actionPerformed(ActionEvent e) {
            eventHappened = e;
        }
    }

    protected AbstractButton button = null;

    protected final Icon icon1 = createNewIcon();

    protected final Icon icon2 = createNewIcon();

    protected final String text1 = "texttext1";

    protected final String text2 = "texttext2";

    protected final String text3 = "texttext3";

    protected final String text4 = "texttext4";

    protected AbstractAction action1;

    protected AbstractAction action2;

    public AbstractButtonTest() {
        super("");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        button = new AbstractButton() {
            private static final long serialVersionUID = 1L;
        };
        if (button.getModel() == null) {
            button.setModel(new DefaultButtonModel());
        }
        action1 = new AbstractAction(text1, icon1) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent event) {
            }
        };
        action2 = new AbstractAction(text2, icon2) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent event) {
            }
        };
    }

    /*
     * @see JComponentTest#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        button = null;
        super.tearDown();
    }

    public void testParamString() {
        assertTrue("ParamString returns a string ", button.toString() != null);
    }

    public void testSetEnabled() {
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        ConcreteChangeListener listener3 = new ConcreteChangeListener();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.addChangeListener(listener3);
        button.setEnabled(false);
        assertFalse(button.isEnabled());
        listener1.checkPropertyFired(button, "enabled", Boolean.TRUE, Boolean.FALSE);
        listener2.checkPropertyFired(button, "enabled", Boolean.TRUE, Boolean.FALSE);
        assertTrue("state event's been fired ", listener3.eventHappened != null);
        assertEquals("state event fired properly ", ChangeEvent.class, listener3.eventHappened
                .getClass());
        assertEquals("state event fired properly ", button, listener3.eventHappened.getSource());
        listener1.reset();
        listener2.reset();
        listener3.eventHappened = null;
        button.setEnabled(true);
        assertTrue(button.isEnabled());
        listener1.checkPropertyFired(button, "enabled", Boolean.FALSE, Boolean.TRUE);
        listener2.checkPropertyFired(button, "enabled", Boolean.FALSE, Boolean.TRUE);
        assertTrue("state event's been fired ", listener3.eventHappened != null);
        assertEquals("state event fired properly ", ChangeEvent.class, listener3.eventHappened
                .getClass());
        assertEquals("state event fired properly ", button, listener3.eventHappened.getSource());
        listener1.reset();
        listener2.reset();
        listener3.eventHappened = null;
        button.setEnabled(true);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        assertNull("event's not been fired ", listener3.eventHappened);
        listener1.reset();
        listener2.reset();
    }

    public void testPaintBorder() {
        class ThisBorder implements Border {
            public boolean haveBeenPainted = false;

            public boolean isBorderOpaque() {
                return true;
            }

            public void paintBorder(final Component arg0, final Graphics arg1, final int arg2,
                    final int arg3, final int arg4, final int arg5) {
                haveBeenPainted = true;
            }

            public Insets getBorderInsets(final Component c) {
                return new Insets(1, 2, 3, 4);
            }
        }
        ;
        ThisBorder border = new ThisBorder();
        button.setBorder(border);
        button.setBorderPainted(false);
        button.paintBorder(button.getGraphics());
        assertFalse("painted", border.haveBeenPainted);
        button.setBorderPainted(true);
        button.paintBorder(button.getGraphics());
        assertTrue("painted", border.haveBeenPainted);
    }

    public void testImageUpdate() {
        Image image1 = new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB);
        Image image2 = new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB);
        Icon icon1 = new Icon() {
            public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            }

            public int getIconWidth() {
                return 0;
            }

            public int getIconHeight() {
                return 0;
            }
        };
        Icon icon2 = new ImageIcon(image1);
        Icon icon3 = new ImageIcon(image2);
        assertFalse(button.imageUpdate(image1, ImageObserver.SOMEBITS, 1, 1, 1, 1));
        button.setIcon(icon1);
        assertFalse(button.imageUpdate(image1, ImageObserver.SOMEBITS, 1, 1, 1, 1));
        button.setIcon(icon2);
        assertTrue(button.imageUpdate(image1, ImageObserver.SOMEBITS, 1, 1, 1, 1));
        button.setSelectedIcon(icon3);
        button.setSelected(true);
        assertFalse(button.imageUpdate(image1, ImageObserver.SOMEBITS, 1, 1, 1, 1));
        assertTrue(button.imageUpdate(image2, ImageObserver.SOMEBITS, 1, 1, 1, 1));
        button.setSelected(false);
        assertTrue(button.imageUpdate(image1, ImageObserver.SOMEBITS, 1, 1, 1, 1));
        assertFalse(button.imageUpdate(image2, ImageObserver.SOMEBITS, 1, 1, 1, 1));
    }

    /**
     * since this method has empty implementation, there's no need to test it
     */
    public void testUpdateUI() {
    }

    /**
     * as method setUI() just calls super.setUI(), it is being tested by JCompomentTest.setUI()
     * nevertheless it is also beeeing tested by testInit()
     */
    public void testSetUIButtonUI() {
    }

    public void testGetUI() {
        assertNull("ui is returned ", button.getUI());
    }

    @SuppressWarnings("serial")
    public void testAbstractButton() {
        button = new AbstractButton() {
        };
        assertNull(button.getIcon());
        assertEquals("", button.getText());
        assertNull(button.getModel());
        assertNull(button.getUI());
    }

    public void testGetSelectedObjects() {
        String text = "Om mani padme hum";
        button.setText(text);
        button.setSelected(false);
        assertNull("no selected objects", button.getSelectedObjects());
        button.setSelected(true);
        assertTrue("there are selected objects", button.getSelectedObjects() != null
                && button.getSelectedObjects().length > 0);
        assertEquals("selected object ", text, button.getSelectedObjects()[0]);
    }

    public void testConfigurePropertiesFromAction() {
        action1.setEnabled(true);
        action1.putValue(Action.SHORT_DESCRIPTION, text3);
        action1.putValue(Action.MNEMONIC_KEY, new Integer(1));
        button.setAction(action1);
        assertEquals("action ", action1, button.getAction());
        assertTrue("enabled ", button.isEnabled());
        assertTrue("enabled ", action1.isEnabled());
        action1.setEnabled(false);
        assertFalse(button.isEnabled());
        assertFalse("enabled ", button.isEnabled());
        assertFalse("enabled ", action1.isEnabled());
        assertEquals("icon ", icon1, button.getIcon());
        action1.putValue(Action.SMALL_ICON, icon2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("mnemonic ", 1, button.getMnemonic());
        action1.putValue(Action.MNEMONIC_KEY, new Integer(27));
        assertEquals("mnemonic ", 27, button.getMnemonic());
        assertEquals("text ", text1, button.getText());
        action1.putValue(Action.NAME, text2);
        assertEquals("text ", text2, button.getText());
    }

    public void testConfigurePropertiesFromAction_ShortDescription() {
        action1.putValue(Action.SHORT_DESCRIPTION, text3);
        button.setAction(action1);
        assertEquals("ToolTipText ", text3, button.getToolTipText());
        action1.putValue(Action.SHORT_DESCRIPTION, text4);
        assertEquals("ToolTipText ", text4, button.getToolTipText());
        button.setAction(action2);
        action1.putValue(Action.SHORT_DESCRIPTION, text4);
        assertNull("ToolTipText ", button.getToolTipText());
        action2.putValue(Action.SHORT_DESCRIPTION, text4);
        assertEquals("ToolTipText ", text4, button.getToolTipText());
    }

    public void testInit() {
        PropertyChangeController listener = new PropertyChangeController();
        button.addPropertyChangeListener(listener);
        button.init(text1, icon1);
        assertEquals(text1, button.getText());
        assertEquals(icon1, button.getIcon());
        listener.checkPropertyFired(button, "text", "", text1);
        listener.checkPropertyFired(button, "icon", null, icon1);
        button.init(null, null);
        assertEquals(text1, button.getText());
        assertEquals(icon1, button.getIcon());
        button.setText("");
        button.init(text1, null);
        assertEquals(text1, button.getText());
        assertEquals(icon1, button.getIcon());
        button = new AbstractButton() {
        };
        button.init(null, null);
        assertEquals(button.getText(), "");
        assertNull(button.getIcon());
        button.init(null, icon1);
        assertEquals(button.getText(), "");
        assertEquals(icon1, button.getIcon());
    }

    public void testSetText1() {
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setText(text1);
        listener1.checkPropertyFired(button, "text", "", text1);
        listener2.checkPropertyFired(button, "text", "", text1);
        listener1.reset();
        listener2.reset();
        button.setText(text2);
        listener1.checkPropertyFired(button, "text", text1, text2);
        listener2.checkPropertyFired(button, "text", text1, text2);
        listener1.reset();
        listener2.reset();
        button.setText(text2);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
        button.setText(null);
        assertNull("text ", button.getText());
    }

    public void testSetText2() {
        button.setText(text1);
        button.setMnemonic(KeyEvent.VK_X);
        assertEquals(2, button.getDisplayedMnemonicIndex());
        assertEquals(KeyEvent.VK_X, button.getMnemonic());
        button.setText(text2);
        assertEquals(2, button.getDisplayedMnemonicIndex());
        assertEquals(KeyEvent.VK_X, button.getMnemonic());
        button.setText(null);
        assertEquals(-1, button.getDisplayedMnemonicIndex());
        assertEquals(KeyEvent.VK_X, button.getMnemonic());
    }

    public void testSetText3() {
        button.setText(text1);
        button.setMnemonic(KeyEvent.VK_X);
        button.setDisplayedMnemonicIndex(3);
        assertEquals(3, button.getDisplayedMnemonicIndex());
        assertEquals(KeyEvent.VK_X, button.getMnemonic());
        button.setText(text2);
        assertEquals(2, button.getDisplayedMnemonicIndex());
        assertEquals(KeyEvent.VK_X, button.getMnemonic());
        button.setDisplayedMnemonicIndex(3);
        button.setMnemonic(KeyEvent.VK_D);
        button.setText(text1);
        assertEquals(-1, button.getDisplayedMnemonicIndex());
        assertEquals(KeyEvent.VK_D, button.getMnemonic());
    }

    public void testGetText() {
        String text1 = "text1";
        String text2 = "text2";
        assertEquals("default text ", "", button.getText());
        button.setText(text1);
        assertEquals("text ", text1, button.getText());
        button.setText(text2);
        assertEquals("text ", text2, button.getText());
    }

    @SuppressWarnings("deprecation")
    public void testSetLabel() {
        String text1 = "text1";
        String text2 = "text2";
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setLabel(text1);
        listener1.checkPropertyFired(button, "text", "", text1);
        listener2.checkPropertyFired(button, "text", "", text1);
        listener1.reset();
        listener2.reset();
        button.setLabel(text2);
        listener1.checkPropertyFired(button, "text", text1, text2);
        listener2.checkPropertyFired(button, "text", text1, text2);
        listener1.reset();
        listener2.reset();
        button.setLabel(text2);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    @SuppressWarnings("deprecation")
    public void testGetLabel() {
        String text1 = "text1";
        String text2 = "text2";
        assertEquals("default label ", "", button.getLabel());
        button.setLabel(text1);
        assertEquals("text ", text1, button.getText());
        assertEquals("label ", text1, button.getLabel());
        button.setLabel(text2);
        assertEquals("text ", text2, button.getText());
        assertEquals("label ", text2, button.getLabel());
    }

    public void testSetAction1() {
        PropertyChangeController listener1 = new PropertyChangeController();
        ConcreteChangeListener listener2 = new ConcreteChangeListener();
        button.setEnabled(false);
        button.addPropertyChangeListener(listener1);
        button.addChangeListener(listener2);
        action1.setEnabled(true);
        action1.putValue(Action.SHORT_DESCRIPTION, text3);
        action1.putValue(Action.MNEMONIC_KEY, new Integer(1));
        button.setAction(action1);
        assertTrue("event's been fired ", listener1.isChanged());
        listener1.checkPropertyFired(button, "mnemonic", new Integer(0), new Integer(1));
        listener1.checkPropertyFired(button, "ToolTipText", null, text3);
        listener1.checkPropertyFired(button, "icon", null, icon1);
        listener1.checkPropertyFired(button, "enabled", Boolean.FALSE, Boolean.TRUE);
        listener1.checkPropertyFired(button, "text", "", text1);
        listener1.checkPropertyFired(button, "action", null, action1);
        assertTrue("state event's been fired ", listener2.eventHappened != null);
        assertEquals("state event fired properly ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        assertEquals("state event fired properly ", button, listener2.eventHappened.getSource());
        listener1.reset();
        listener2.eventHappened = null;
        button.setAction(action2);
        listener1.checkPropertyFired(button, "mnemonic", new Integer(1), new Integer(0));
        listener1.checkPropertyFired(button, "ToolTipText", text3, null);
        listener1.checkPropertyFired(button, "icon", icon1, icon2);
        listener1.checkPropertyFired(button, "text", text1, text2);
        listener1.checkPropertyFired(button, "action", action1, action2);
        assertTrue("state event's been fired ", listener2.eventHappened != null);
        assertEquals("state event fired properly ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        assertEquals("state event fired properly ", button, listener2.eventHappened.getSource());
        listener1.reset();
        listener2.eventHappened = null;
        button.setAction(action2);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertNull("event's not been fired ", listener2.eventHappened);
        listener1.reset();
        listener2.eventHappened = null;
    }

    public void testSetAction2() {
        action1.setEnabled(true);
        action1.putValue(Action.SHORT_DESCRIPTION, text3);
        action1.putValue(Action.MNEMONIC_KEY, new Integer(1));
        button.setAction(action1);
        assertTrue("enabled ", button.isEnabled());
        assertTrue("enabled ", action1.isEnabled());
        action1.setEnabled(false);
        button.isEnabled();
        assertFalse("enabled ", button.isEnabled());
        assertFalse("enabled ", action1.isEnabled());
        assertEquals("icon ", icon1, button.getIcon());
        action1.putValue(Action.SMALL_ICON, icon2);
        assertEquals("icon ", icon2, button.getIcon());
        assertEquals("mnemonic ", 1, button.getMnemonic());
        action1.putValue(Action.MNEMONIC_KEY, new Integer(27));
        assertEquals("mnemonic ", 27, button.getMnemonic());
        assertEquals("text ", text1, button.getText());
        action1.putValue(Action.NAME, text2);
        assertEquals("text ", text2, button.getText());
    }

    public void testSetAction3() {
        Action action1 = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                putValue("performed", e);
            }
        };
        Action action2 = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                putValue("performed", e);
            }
        };
        ActionEvent event1 = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "1");
        ActionEvent event2 = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "2");
        button.setAction(action1);
        button.fireActionPerformed(event1);
        assertEquals("1", ((ActionEvent) action1.getValue("performed")).getActionCommand());
        button.setAction(action2);
        button.fireActionPerformed(event2);
        assertEquals("1", ((ActionEvent) action1.getValue("performed")).getActionCommand());
        assertEquals("2", ((ActionEvent) action2.getValue("performed")).getActionCommand());
    }

    public void testSetAction4() {
        button.setAction(null);
        button.setText("text");
        button.setIcon(icon1);
        button.setAction(null);
        assertNull(button.getText());
        assertNull(button.getIcon());
    }

    public void testSetAction5() {
        if (!isHarmony()) {
            return;
        }
        button.addActionListener(action1);
        button.addActionListener(action1);
        button.setAction(action1);
        assertEquals(2, button.getActionListeners().length);
        button.setAction(null);
        assertEquals(2, button.getActionListeners().length);
        button.removeActionListener(action1);
        button.removeActionListener(action1);
        assertEquals(0, button.getActionListeners().length);
        button.setAction(action1);
        button.addActionListener(action1);
        button.addActionListener(action1);
        assertEquals(3, button.getActionListeners().length);
        button.setAction(null);
        assertEquals(2, button.getActionListeners().length);
        button.removeActionListener(action1);
        button.removeActionListener(action1);
        assertEquals(0, button.getActionListeners().length);
    }

    public void setActionGetToolTipText() {
        assertEquals("ToolTipText ", text3, button.getToolTipText());
        action1.putValue(Action.SHORT_DESCRIPTION, text4);
        assertEquals("ToolTipText ", text4, button.getToolTipText());
        button.setAction(action2);
        action1.putValue(Action.SHORT_DESCRIPTION, text4);
        assertNull("ToolTipText ", button.getToolTipText());
        action2.putValue(Action.SHORT_DESCRIPTION, text4);
        assertEquals("ToolTipText ", text4, button.getToolTipText());
    }

    public void testGetAction() {
        Action action1 = new AbstractAction() {
            public void actionPerformed(final ActionEvent event) {
            }
        };
        Action action2 = new AbstractAction() {
            public void actionPerformed(final ActionEvent event) {
            }
        };
        assertNull("default action ", button.getAction());
        button.setAction(action1);
        assertEquals("action ", action1, button.getAction());
        button.setAction(action2);
        assertEquals("action ", action2, button.getAction());
    }

    public void testSetSelectedIcon() {
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setSelectedIcon(icon1);
        listener1.checkPropertyFired(button, "selectedIcon", null, icon1);
        listener2.checkPropertyFired(button, "selectedIcon", null, icon1);
        listener1.reset();
        listener2.reset();
        button.setSelectedIcon(icon2);
        listener1.checkPropertyFired(button, "selectedIcon", icon1, icon2);
        listener2.checkPropertyFired(button, "selectedIcon", icon1, icon2);
        listener1.reset();
        listener2.reset();
        button.setSelectedIcon(icon2);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testSetRolloverSelectedIcon() {
        PropertyChangeController listener = new PropertyChangeController();
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        button.setRolloverEnabled(true);
        button.addPropertyChangeListener(listener);
        button.setRolloverSelectedIcon(icon1);
        listener.checkLastPropertyFired(button, "rolloverSelectedIcon", null, icon1);
        button.setRolloverEnabled(false);
        listener.reset();
        button.setRolloverSelectedIcon(icon2);
        listener.checkPropertyFired(button, "rolloverSelectedIcon", icon1, icon2);
        listener.checkPropertyFired(button, "rolloverEnabled", Boolean.FALSE, Boolean.TRUE);
        listener.reset();
        button.setRolloverSelectedIcon(icon2);
        assertFalse("event's not been fired ", listener.isChanged());
        button.setRolloverSelectedIcon(null);
        button.setRolloverEnabled(false);
        listener.reset();
        if (isHarmony()) {
            button.setRolloverSelectedIcon(null);
            assertFalse("event fired", listener.isChanged());
        }
    }

    public void testSetRolloverIcon() {
        PropertyChangeController listener = new PropertyChangeController();
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        button.setRolloverEnabled(true);
        button.addPropertyChangeListener(listener);
        button.setRolloverIcon(icon1);
        listener.checkLastPropertyFired(button, "rolloverIcon", null, icon1);
        button.setRolloverEnabled(false);
        listener.reset();
        button.setRolloverIcon(icon2);
        listener.checkPropertyFired(button, "rolloverIcon", icon1, icon2);
        listener.checkPropertyFired(button, "rolloverEnabled", Boolean.FALSE, Boolean.TRUE);
        listener.reset();
        button.setRolloverIcon(icon2);
        assertFalse("event's not been fired ", listener.isChanged());
        button.setRolloverIcon(null);
        button.setRolloverEnabled(false);
        listener.reset();
        if (isHarmony()) {
            button.setRolloverIcon(null);
            assertFalse("event's not been fired ", listener.isChanged());
        }
    }

    public void testSetPressedIcon() {
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setPressedIcon(icon1);
        listener1.checkPropertyFired(button, "pressedIcon", null, icon1);
        listener2.checkPropertyFired(button, "pressedIcon", null, icon1);
        listener1.reset();
        listener2.reset();
        button.setPressedIcon(icon2);
        listener1.checkPropertyFired(button, "pressedIcon", icon1, icon2);
        listener2.checkPropertyFired(button, "pressedIcon", icon1, icon2);
        listener1.reset();
        listener2.reset();
        button.setPressedIcon(icon2);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testSetIcon() {
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setIcon(icon1);
        listener1.checkPropertyFired(button, "icon", null, icon1);
        listener2.checkPropertyFired(button, "icon", null, icon1);
        listener1.reset();
        listener2.reset();
        button.setIcon(icon2);
        listener1.checkPropertyFired(button, "icon", icon1, icon2);
        listener2.checkPropertyFired(button, "icon", icon1, icon2);
        listener1.reset();
        listener2.reset();
        button.setIcon(icon2);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testSetDisabledSelectedIcon() {
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setDisabledSelectedIcon(icon1);
        listener1.checkPropertyFired(button, "disabledSelectedIcon", null, icon1);
        listener2.checkPropertyFired(button, "disabledSelectedIcon", null, icon1);
        listener1.reset();
        listener2.reset();
        button.setDisabledSelectedIcon(icon2);
        listener1.checkPropertyFired(button, "disabledSelectedIcon", icon1, icon2);
        listener2.checkPropertyFired(button, "disabledSelectedIcon", icon1, icon2);
        listener1.reset();
        listener2.reset();
        button.setDisabledSelectedIcon(icon2);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testSetDisabledIcon() {
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setDisabledIcon(icon1);
        listener1.checkPropertyFired(button, "disabledIcon", null, icon1);
        listener2.checkPropertyFired(button, "disabledIcon", null, icon1);
        listener1.reset();
        listener2.reset();
        button.setDisabledIcon(icon2);
        listener1.checkPropertyFired(button, "disabledIcon", icon1, icon2);
        listener2.checkPropertyFired(button, "disabledIcon", icon1, icon2);
        listener1.reset();
        listener2.reset();
        button.setDisabledIcon(icon2);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testGetSelectedIcon() {
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        assertNull("default Selected Icon ", button.getSelectedIcon());
        button.setSelectedIcon(icon1);
        assertEquals("Selected Icon ", icon1, button.getSelectedIcon());
        button.setSelectedIcon(icon2);
        assertEquals("Selected Icon ", icon2, button.getSelectedIcon());
    }

    public void testGetRolloverSelectedIcon() {
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        assertNull("default Rollover Selected Icon ", button.getRolloverSelectedIcon());
        button.setRolloverSelectedIcon(icon1);
        assertEquals("Rollover Selected Icon ", icon1, button.getRolloverSelectedIcon());
        button.setRolloverSelectedIcon(icon2);
        assertEquals("Rollover Selected Icon ", icon2, button.getRolloverSelectedIcon());
    }

    public void testGetRolloverIcon() {
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        assertNull("default Rollover Icon ", button.getRolloverIcon());
        button.setRolloverIcon(icon1);
        assertEquals("Rollover Icon ", icon1, button.getRolloverIcon());
        button.setRolloverIcon(icon2);
        assertEquals("Rollover Icon ", icon2, button.getRolloverIcon());
    }

    public void testGetPressedIcon() {
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        assertNull("default Pressed Icon ", button.getPressedIcon());
        button.setPressedIcon(icon1);
        assertEquals("Pressed Icon ", icon1, button.getPressedIcon());
        button.setPressedIcon(icon2);
        assertEquals("Pressed Icon ", icon2, button.getPressedIcon());
    }

    public void testGetIcon() {
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        assertNull("default Icon ", button.getIcon());
        button.setIcon(icon1);
        assertEquals("Icon ", icon1, button.getIcon());
        button.setIcon(icon2);
        assertEquals("Icon ", icon2, button.getIcon());
    }

    public void testGetDisabledSelectedIcon() {
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        Icon icon3 = createNewIcon();
        assertNull("default Icon ", button.getDisabledSelectedIcon());
        button.setDisabledSelectedIcon(icon1);
        assertEquals("DisabledSelected Icon ", icon1, button.getDisabledSelectedIcon());
        button.setDisabledSelectedIcon(icon2);
        assertEquals("DisabledSelected Icon ", icon2, button.getDisabledSelectedIcon());
        button.setIcon(icon1);
        button.setDisabledSelectedIcon(null);
        button.setDisabledIcon(null);
        assertNotNull(button.getDisabledSelectedIcon());
        assertEquals(icon1.getIconHeight(), button.getDisabledSelectedIcon().getIconHeight());
        assertEquals(icon1.getIconWidth(), button.getDisabledSelectedIcon().getIconWidth());
        button.setIcon(null);
        button.setSelectedIcon(icon2);
        assertNotNull(button.getDisabledSelectedIcon());
        assertEquals(icon2.getIconHeight(), button.getDisabledSelectedIcon().getIconHeight());
        assertEquals(icon2.getIconWidth(), button.getDisabledSelectedIcon().getIconWidth());
        button.setSelectedIcon(null);
        button.setDisabledIcon(icon1);
        assertNotNull(button.getDisabledSelectedIcon());
        assertEquals(icon1.getIconHeight(), button.getDisabledSelectedIcon().getIconHeight());
        assertEquals(icon1.getIconWidth(), button.getDisabledSelectedIcon().getIconWidth());
        button.setSelectedIcon(icon2);
        assertNotNull(button.getDisabledSelectedIcon());
        assertEquals(icon2.getIconHeight(), button.getDisabledSelectedIcon().getIconHeight());
        assertEquals(icon2.getIconWidth(), button.getDisabledSelectedIcon().getIconWidth());
        button.setIcon(icon3);
        assertNotNull(button.getDisabledSelectedIcon());
        assertEquals(icon2.getIconHeight(), button.getDisabledSelectedIcon().getIconHeight());
        assertEquals(icon2.getIconWidth(), button.getDisabledSelectedIcon().getIconWidth());
    }

    public void testGetDisabledIcon() {
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        assertNull("default Icon ", button.getDisabledIcon());
        button.setDisabledIcon(icon1);
        assertEquals("Disabled Icon ", icon1, button.getDisabledIcon());
        button.setDisabledIcon(icon2);
        assertEquals("Disabled Icon ", icon2, button.getDisabledIcon());
        button.setIcon(icon1);
        button.setDisabledIcon(null);
        assertNotNull(button.getDisabledIcon());
        assertEquals(icon1.getIconHeight(), button.getDisabledIcon().getIconHeight());
        assertEquals(icon1.getIconWidth(), button.getDisabledIcon().getIconWidth());
        button.setIcon(new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
            }

            public int getIconWidth() {
                return 10;
            }

            public int getIconHeight() {
                return 10;
            }
        });
        assertNull(button.getDisabledIcon());
    }

    public void testSetModel() {
        DefaultButtonModel model1 = new DefaultButtonModel();
        DefaultButtonModel model2 = new DefaultButtonModel();
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        ButtonModel oldModel = button.getModel();
        button.setModel(model1);
        assertSame(model1, button.getModel());
        listener1.checkPropertyFired(button, "model", oldModel, model1);
        listener2.checkPropertyFired(button, "model", oldModel, model1);
        listener1.reset();
        listener2.reset();
        button.setModel(model2);
        assertSame(model2, button.getModel());
        listener1.checkPropertyFired(button, "model", model1, model2);
        listener2.checkPropertyFired(button, "model", model1, model2);
        assertEquals("model's action listeners ", 0, model1.getActionListeners().length);
        assertEquals("model's change listeners ", 0, model1.getChangeListeners().length);
        listener1.reset();
        listener2.reset();
        button.setModel(model2);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testNumberOfModelListeners() {
        button.setUI(null);
        DefaultButtonModel model = (DefaultButtonModel) button.getModel();
        assertEquals("model's action listeners ", 1, model.getActionListeners().length);
        assertEquals("model's item listeners ", 1, model.getItemListeners().length);
        assertEquals("model's change listeners ", 1, model.getChangeListeners().length);
    }

    public void testGetModel() {
        ButtonModel model1 = new DefaultButtonModel();
        ButtonModel model2 = new DefaultButtonModel();
        assertNotNull("default buttonModel ", button.getModel());
        button.setModel(model1);
        assertEquals("buttonModel ", model1, button.getModel());
        button.setModel(model2);
        assertEquals("buttonModel ", model2, button.getModel());
        button = new AbstractButton() {
        };
        assertNull("default buttonModel", button.getModel());
    }

    public void testSetActionCommand() {
        String command1 = "When one door is closed";
        String command2 = "Don't you know other is opened?";
        button.setActionCommand(command1);
        assertEquals("comman action ", command1, button.getActionCommand());
        button.setActionCommand(command2);
        assertEquals("comman action ", command2, button.getActionCommand());
    }

    public void testGetActionCommand() {
        assertEquals("default comman action ", "", button.getActionCommand());
    }

    public void testCheckVerticalKey() {
        String exceptionText = "exceptionText";
        int res = button.checkVerticalKey(SwingConstants.TOP, exceptionText);
        assertEquals("returned value ", 1, res);
        try {
            res = button.checkVerticalKey(SwingConstants.WEST, exceptionText);
        } catch (IllegalArgumentException e) {
            assertEquals("exception's message ", exceptionText, e.getMessage());
            return;
        }

        fail("Expected IllegalArgumentException to be thrown");
    }

    public void testCheckHorizontalKey() {
        String exceptionText = "exceptionText";
        int res = button.checkHorizontalKey(SwingConstants.TRAILING, exceptionText);
        assertEquals("returned value ", SwingConstants.TRAILING, res);
        try {
            res = button.checkHorizontalKey(SwingConstants.TOP, exceptionText);
        } catch (IllegalArgumentException e) {
            assertEquals("exception's message ", exceptionText, e.getMessage());
            return;
        }
        
        fail("Expected IllegalArgumentException to be thrown");
    }

    public void testCreateActionPropertyChangeListener() {
        Object res1 = null;
        Object res2 = null;
        AbstractAction action1 = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent event) {
            }
        };
        res1 = button.createActionPropertyChangeListener(action1);
        assertNotNull(res1);
        res2 = button.createActionPropertyChangeListener(null);
        assertNotNull(res2);
    }

    public void testCreateActionListener() throws Exception {
        Object res1 = button.createActionListener();
        Object res2 = button.createActionListener();
        assertNotNull(res1);
        assertNotNull(res2);
        if (isHarmony()) {
            assertSame("created listener is shared ", res1, res2);
        }
    }

    public void testCreateItemListener() {
        button.itemListener = null;
        Object res1 = button.createItemListener();
        Object res2 = button.createItemListener();
        assertNotNull(res1);
        assertNotNull(res2);
        assertNull(button.itemListener);
    }

    public void testCreateChangeListener() {
        button.changeListener = null;
        Object res1 = button.createChangeListener();
        Object res2 = button.createChangeListener();
        assertNotNull(res1);
        assertNotNull(res2);
        assertNull(button.changeListener);
    }

    public void testRemoveChangeListener() {
        ChangeListener listener1 = new ConcreteChangeListener();
        ChangeListener listener2 = new ConcreteChangeListener();
        ChangeListener listener3 = new ConcreteChangeListener();
        button.addChangeListener(listener1);
        button.addChangeListener(listener2);
        button.addChangeListener(listener2);
        button.addChangeListener(listener3);
        EventListener[] listeners = button.getListeners(ChangeListener.class);
        assertTrue("listener's array is valid ", listeners != null);
        button.removeChangeListener(listener1);
        listeners = button.getListeners(ChangeListener.class);
        assertTrue("listener's array is valid ", listeners != null);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 2);
        button.removeChangeListener(listener2);
        listeners = button.getListeners(ChangeListener.class);
        assertTrue("listener's array is valid ", listeners != null);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 1);
        button.removeChangeListener(listener2);
        listeners = button.getListeners(ChangeListener.class);
        assertTrue("listener's array is valid ", listeners != null);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 0);
        button.removeChangeListener(listener2);
        listeners = button.getListeners(ChangeListener.class);
        assertTrue("listener's array is valid ", listeners != null);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 0);
        button.removeChangeListener(listener3);
        listeners = button.getListeners(ChangeListener.class);
        assertTrue("listener's array is valid ", listeners != null);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 0);
    }

    public void testAddChangeListener() {
        ChangeListener listener1 = new ConcreteChangeListener();
        ChangeListener listener2 = new ConcreteChangeListener();
        EventListener[] listeners = button.getChangeListeners();
        int numListeners = listeners.length;
        button.addChangeListener(null);
        listeners = button.getChangeListeners();
        assertEquals("listener's array is valid ", numListeners, listeners.length);
        button.addChangeListener(listener1);
        listeners = button.getListeners(ChangeListener.class);
        assertTrue("listener's array is valid ", listeners != null);
        assertTrue("listener's added successfully ", find(listeners, listener1) == 1);
        button.addChangeListener(listener2);
        listeners = button.getListeners(ChangeListener.class);
        assertTrue("listener's array is valid ", listeners != null);
        assertTrue("listener's added successfully ", find(listeners, listener1) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener2) == 1);
        button.addChangeListener(listener2);
        listeners = button.getListeners(ChangeListener.class);
        assertTrue("listener's array is valid ", listeners != null);
        assertTrue("listener's added successfully ", find(listeners, listener1) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener2) == 2);
    }

    /**
     * this method is being tested mostly with testAddChangeListener()
     * and testRemoveChangeListener()
     */
    public void testGetChangeListeners() {
        EventListener[] listeners = button.getListeners(ChangeListener.class);
        assertTrue("listener's array is not null ", listeners != null);
    }

    public void testFireStateChanged() {
        ChangeEvent event1 = null;
        ChangeEvent event2 = null;
        ConcreteChangeListener listener1 = new ConcreteChangeListener();
        ConcreteChangeListener listener2 = new ConcreteChangeListener();
        button.addChangeListener(listener1);
        button.addChangeListener(listener2);
        button.fireStateChanged();
        event1 = listener1.eventHappened;
        assertTrue("event fired ", listener1.eventHappened != null);
        assertTrue("event fired ", listener2.eventHappened != null);
        assertTrue("one event fired ", listener1.eventHappened == listener2.eventHappened);
        assertEquals("event fired properly ", ChangeEvent.class, listener1.eventHappened
                .getClass());
        assertEquals("event fired properly ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        assertEquals("event's source ", button, listener1.eventHappened.getSource());
        assertEquals("event's source ", button, listener2.eventHappened.getSource());
        button.fireStateChanged();
        event2 = listener1.eventHappened;
        assertTrue("event fired ", listener1.eventHappened != null);
        assertTrue("event fired ", listener2.eventHappened != null);
        assertTrue("one event fired ", listener1.eventHappened == listener2.eventHappened);
        assertEquals("event's class ", ChangeEvent.class, listener1.eventHappened.getClass());
        assertEquals("event's class ", ChangeEvent.class, listener2.eventHappened.getClass());
        assertEquals("event's source ", button, listener1.eventHappened.getSource());
        assertEquals("event's source ", button, listener2.eventHappened.getSource());
        assertTrue("the same event is fired always ", event1 == event2);
    }

    public void testRemoveItemListener() {
        ItemListener listener1 = new ConcreteItemListener();
        ItemListener listener2 = new ConcreteItemListener();
        ItemListener listener3 = new ConcreteItemListener();
        button.addItemListener(listener1);
        button.addItemListener(listener2);
        button.addItemListener(listener2);
        button.addItemListener(listener3);
        EventListener[] listeners = button.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 4);
        button.removeItemListener(listener1);
        listeners = button.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 3);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 2);
        button.removeItemListener(listener2);
        listeners = button.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 2);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 1);
        button.removeItemListener(listener2);
        listeners = button.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 0);
        button.removeItemListener(listener2);
        listeners = button.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 0);
        button.removeItemListener(listener3);
        listeners = button.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 0);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 0);
    }

    public void testAddItemListener() {
        ItemListener listener1 = new ConcreteItemListener();
        ItemListener listener2 = new ConcreteItemListener();
        button.addItemListener(listener1);
        EventListener[] listeners = button.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 1);
        assertEquals("listener's added successfully ", listeners[0], listener1);
        button.addItemListener(listener2);
        listeners = button.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 2);
        assertTrue("listener's added successfully ", find(listeners, listener1) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener2) == 1);
        button.addItemListener(listener2);
        listeners = button.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 3);
        assertTrue("listener's added successfully ", find(listeners, listener1) == 1);
        assertTrue("listener's added successfully ", find(listeners, listener2) == 2);
    }

    /**
     * this method is being tested mostly with testAddItemListener()
     * and testRemoveItemListener()
     */
    public void testGetItemListeners() {
        EventListener[] listeners = button.getListeners(ItemListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 0);
    }

    public void testFireItemStateChanged() {
        Object item1 = "item1";
        Object item2 = "item2";
        ItemEvent event1 = new ItemEvent(button, 11, item1, 2);
        ItemEvent event2 = new ItemEvent(button, 111, item2, 1);
        ConcreteItemListener listener1 = new ConcreteItemListener();
        ConcreteItemListener listener2 = new ConcreteItemListener();
        button.addItemListener(listener1);
        button.addItemListener(listener2);
        button.fireItemStateChanged(event1);
        assertEquals("event's ID ", ItemEvent.ITEM_STATE_CHANGED, listener1.eventHappened
                .getID());
        assertEquals("event's item ", button, listener1.eventHappened.getItem());
        assertEquals("event's source ", button, listener1.eventHappened.getSource());
        assertEquals("event's StateChange ", ItemEvent.DESELECTED, listener1.eventHappened
                .getStateChange());
        assertEquals("event's ID ", ItemEvent.ITEM_STATE_CHANGED, listener2.eventHappened
                .getID());
        assertEquals("event's item ", button, listener2.eventHappened.getItem());
        assertEquals("event's source ", button, listener2.eventHappened.getSource());
        assertEquals("event's StateChange ", ItemEvent.DESELECTED, listener2.eventHappened
                .getStateChange());
        button.fireItemStateChanged(event2);
        assertEquals("event's ID ", ItemEvent.ITEM_STATE_CHANGED, listener1.eventHappened
                .getID());
        assertEquals("event's item ", button, listener1.eventHappened.getItem());
        assertEquals("event's source ", button, listener1.eventHappened.getSource());
        assertEquals("event's StateChange ", ItemEvent.SELECTED, listener1.eventHappened
                .getStateChange());
        assertEquals("event's ID ", ItemEvent.ITEM_STATE_CHANGED, listener2.eventHappened
                .getID());
        assertEquals("event's item ", button, listener2.eventHappened.getItem());
        assertEquals("event's source ", button, listener2.eventHappened.getSource());
        assertEquals("event's StateChange ", ItemEvent.SELECTED, listener2.eventHappened
                .getStateChange());
    }

    public void testRemoveActionListener() {
        final int startLength = button.getListeners(ActionListener.class).length;
        ActionListener listener1 = new ConcreteActionListener();
        ActionListener listener2 = new ConcreteActionListener();
        ActionListener listener3 = new ConcreteActionListener();
        button.addActionListener(listener1);
        button.addActionListener(listener2);
        button.addActionListener(listener2);
        button.addActionListener(listener3);
        EventListener[] listeners = button.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 4 + startLength);
        button.removeActionListener(listener1);
        listeners = button.getListeners(ActionListener.class);
        assertEquals(listeners.length, 3 + startLength);
        assertEquals(find(listeners, listener3), 1);
        assertEquals(find(listeners, listener2), 2);
        button.removeActionListener(listener2);
        listeners = button.getListeners(ActionListener.class);
        assertEquals(listeners.length, 2 + startLength);
        assertEquals(find(listeners, listener3), 1);
        assertEquals(find(listeners, listener2), 1);
        button.removeActionListener(listener2);
        listeners = button.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 1 + startLength);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 0);
        button.removeActionListener(listener2);
        listeners = button.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 1 + startLength);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 1);
        assertTrue("listener's removed successfully ", find(listeners, listener2) == 0);
        button.removeActionListener(listener3);
        listeners = button.getListeners(ActionListener.class);
        assertTrue("listener's array has the proper size ", listeners != null
                && listeners.length == 0 + startLength);
        assertTrue("listener's removed successfully ", find(listeners, listener3) == 0);
    }

    public void testAddActionListener() {
        final int startLength = button.getListeners(ActionListener.class).length;
        ActionListener listener1 = new ConcreteActionListener();
        ActionListener listener2 = new ConcreteActionListener();
        button.addActionListener(listener1);
        EventListener[] listeners = button.getListeners(ActionListener.class);
        assertEquals(listeners.length, 1 + startLength);
        assertEquals(listeners[0], listener1);
        button.addActionListener(listener2);
        listeners = button.getListeners(ActionListener.class);
        assertEquals(listeners.length, 2 + startLength);
        assertEquals(find(listeners, listener1), 1);
        assertEquals(find(listeners, listener2), 1);
        button.addActionListener(listener2);
        listeners = button.getListeners(ActionListener.class);
        assertEquals(listeners.length, 3 + startLength);
        assertEquals(find(listeners, listener1), 1);
        assertEquals(find(listeners, listener2), 2);
    }

    /**
     * this method is being tested mostly with testAddActionListener()
     * and testRemoveActionListener()
     */
    public void testGetActionListeners() {
        button.setUI(null);
        EventListener[] listeners = button.getActionListeners();
        assertEquals(listeners.length, 0);
    }

    public void testFireActionPerformed() {
        String command1 = "command1";
        String command2 = "command2";
        String command3 = "command3";
        ActionEvent event1 = new ActionEvent(button, 11, command1, 2);
        ActionEvent event2 = new ActionEvent(button, 111, command2, 1);
        ActionEvent event3 = new ActionEvent(button, 1111, null, 1);
        ConcreteActionListener listener1 = new ConcreteActionListener();
        ConcreteActionListener listener2 = new ConcreteActionListener();
        button.setText(command3);
        button.addActionListener(listener1);
        button.addActionListener(listener2);
        button.fireActionPerformed(event1);
        assertEquals("event's source ", event1.getSource(), listener1.eventHappened.getSource());
        assertEquals("event's command ", event1.getActionCommand(), listener1.eventHappened
                .getActionCommand());
        assertEquals("event's ID ", ActionEvent.ACTION_PERFORMED, listener1.eventHappened
                .getID());
        assertEquals("event's modifiers ", event1.getModifiers(), listener1.eventHappened
                .getModifiers());
        assertEquals("event's source ", event1.getSource(), listener2.eventHappened.getSource());
        assertEquals("event's command ", event1.getActionCommand(), listener2.eventHappened
                .getActionCommand());
        assertEquals("event's ID ", ActionEvent.ACTION_PERFORMED, listener2.eventHappened
                .getID());
        assertEquals("event's modifiers ", event1.getModifiers(), listener2.eventHappened
                .getModifiers());
        button.fireActionPerformed(event2);
        assertEquals("event's source ", event2.getSource(), listener1.eventHappened.getSource());
        assertEquals("event's command ", event2.getActionCommand(), listener1.eventHappened
                .getActionCommand());
        assertEquals("event's ID ", ActionEvent.ACTION_PERFORMED, listener1.eventHappened
                .getID());
        assertEquals("event's modifiers ", event2.getModifiers(), listener1.eventHappened
                .getModifiers());
        assertEquals("event's source ", event2.getSource(), listener2.eventHappened.getSource());
        assertEquals("event's command ", event2.getActionCommand(), listener2.eventHappened
                .getActionCommand());
        assertEquals("event's ID ", ActionEvent.ACTION_PERFORMED, listener2.eventHappened
                .getID());
        assertEquals("event's modifiers ", event2.getModifiers(), listener2.eventHappened
                .getModifiers());
        button.fireActionPerformed(event3);
        assertEquals("event's source ", event2.getSource(), listener1.eventHappened.getSource());
        assertEquals("event's command ", command3, listener1.eventHappened.getActionCommand());
        assertEquals("event's ID ", ActionEvent.ACTION_PERFORMED, listener1.eventHappened
                .getID());
        assertEquals("event's modifiers ", event2.getModifiers(), listener1.eventHappened
                .getModifiers());
    }

    public void testSetIconTextGap() {
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        int value1 = 20;
        int value2 = 13;
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setIconTextGap(value1);
        listener1
                .checkPropertyFired(button, "iconTextGap", new Integer(4), new Integer(value1));
        listener2
                .checkPropertyFired(button, "iconTextGap", new Integer(4), new Integer(value1));
        listener1.reset();
        listener2.reset();
        button.setIconTextGap(value2);
        listener1.checkPropertyFired(button, "iconTextGap", new Integer(value1), new Integer(
                value2));
        listener2.checkPropertyFired(button, "iconTextGap", new Integer(value1), new Integer(
                value2));
        listener1.reset();
        listener2.reset();
        button.setIconTextGap(value2);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testGetIconTextGap() {
        int value1 = -100;
        int value2 = 1;
        int value3 = 100;
        assertEquals("default IconTextGap", 4, button.getIconTextGap());
        button.setIconTextGap(value1);
        assertEquals("IconTextGap", value1, button.getIconTextGap());
        button.setIconTextGap(value2);
        assertEquals("IconTextGap", value2, button.getIconTextGap());
        button.setIconTextGap(value3);
        assertEquals("IconTextGap", value3, button.getIconTextGap());
    }

    public void testSetVerticalTextPosition() {
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setVerticalTextPosition(SwingConstants.TOP);
        listener1.checkPropertyFired(button, "verticalTextPosition", new Integer(
                SwingConstants.CENTER), new Integer(SwingConstants.TOP));
        listener2.checkPropertyFired(button, "verticalTextPosition", new Integer(
                SwingConstants.CENTER), new Integer(SwingConstants.TOP));
        listener1.reset();
        listener2.reset();
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        listener1.checkPropertyFired(button, "verticalTextPosition", new Integer(
                SwingConstants.TOP), new Integer(SwingConstants.BOTTOM));
        listener2.checkPropertyFired(button, "verticalTextPosition", new Integer(
                SwingConstants.TOP), new Integer(SwingConstants.BOTTOM));
        listener1.reset();
        listener2.reset();
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testSetVerticalAlignment() {
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setVerticalAlignment(SwingConstants.TOP);
        listener1.checkPropertyFired(button, "verticalAlignment", new Integer(
                SwingConstants.CENTER), new Integer(SwingConstants.TOP));
        listener2.checkPropertyFired(button, "verticalAlignment", new Integer(
                SwingConstants.CENTER), new Integer(SwingConstants.TOP));
        listener1.reset();
        listener2.reset();
        button.setVerticalAlignment(SwingConstants.BOTTOM);
        listener1.checkPropertyFired(button, "verticalAlignment", new Integer(
                SwingConstants.TOP), new Integer(SwingConstants.BOTTOM));
        listener2.checkPropertyFired(button, "verticalAlignment", new Integer(
                SwingConstants.TOP), new Integer(SwingConstants.BOTTOM));
        listener1.reset();
        listener2.reset();
        button.setVerticalAlignment(SwingConstants.BOTTOM);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testGetVerticalTextPosition() {
        int value1 = SwingConstants.LEFT;
        int value2 = SwingConstants.TOP;
        int value3 = SwingConstants.BOTTOM;
        assertEquals("default VerticalTextPosition", SwingConstants.CENTER, button
                .getVerticalTextPosition());
        boolean thrown = false;
        try {
            button.setVerticalTextPosition(value1);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue("exception is thrown", thrown);
        button.setVerticalTextPosition(value2);
        assertEquals("VerticalTextPosition", value2, button.getVerticalTextPosition());
        button.setVerticalTextPosition(value3);
        assertEquals("VerticalTextPosition", value3, button.getVerticalTextPosition());
    }

    public void testGetVerticalAlignment() {
        int value1 = SwingConstants.RIGHT;
        int value2 = SwingConstants.TOP;
        int value3 = SwingConstants.BOTTOM;
        assertEquals("default VerticalAlignment", SwingConstants.CENTER, button
                .getVerticalAlignment());
        boolean thrown = false;
        try {
            button.setVerticalAlignment(value1);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue("exception is thrown", thrown);
        button.setVerticalAlignment(value2);
        assertEquals("VerticalAlignment", value2, button.getVerticalAlignment());
        button.setVerticalAlignment(value3);
        assertEquals("VerticalAlignment", value3, button.getVerticalAlignment());
    }

    public void testSetHorizontalAlignment() {
        button.setHorizontalAlignment(SwingConstants.CENTER);
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        listener1.checkLastPropertyFired(button, "horizontalAlignment", new Integer(
                SwingConstants.CENTER), new Integer(SwingConstants.LEFT));
        listener2.checkLastPropertyFired(button, "horizontalAlignment", new Integer(
                SwingConstants.CENTER), new Integer(SwingConstants.LEFT));
        listener1.reset();
        listener2.reset();
        button.setHorizontalAlignment(SwingConstants.RIGHT);
        listener1.checkLastPropertyFired(button, "horizontalAlignment", new Integer(
                SwingConstants.LEFT), new Integer(SwingConstants.RIGHT));
        listener2.checkLastPropertyFired(button, "horizontalAlignment", new Integer(
                SwingConstants.LEFT), new Integer(SwingConstants.RIGHT));
        listener1.reset();
        listener2.reset();
        button.setHorizontalAlignment(SwingConstants.RIGHT);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testSetHorizontalTextPosition() {
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setHorizontalTextPosition(SwingConstants.LEFT);
        listener1.checkLastPropertyFired(button, "horizontalTextPosition", new Integer(
                SwingConstants.TRAILING), new Integer(SwingConstants.LEFT));
        listener2.checkLastPropertyFired(button, "horizontalTextPosition", new Integer(
                SwingConstants.TRAILING), new Integer(SwingConstants.LEFT));
        listener1.reset();
        listener2.reset();
        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        listener1.checkLastPropertyFired(button, "horizontalTextPosition", new Integer(
                SwingConstants.LEFT), new Integer(SwingConstants.RIGHT));
        listener2.checkLastPropertyFired(button, "horizontalTextPosition", new Integer(
                SwingConstants.LEFT), new Integer(SwingConstants.RIGHT));
        listener1.reset();
        listener2.reset();
        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testGetHorizontalTextPosition() {
        int value1 = SwingConstants.WEST;
        int value2 = SwingConstants.LEFT;
        int value3 = SwingConstants.TRAILING;
        assertEquals("default HorizontalTextPosition", SwingConstants.TRAILING, button
                .getHorizontalTextPosition());
        boolean thrown = false;
        try {
            button.setHorizontalTextPosition(value1);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue("exception is thrown", thrown);
        button.setHorizontalTextPosition(value2);
        assertEquals("HorizontalTextPosition", value2, button.getHorizontalTextPosition());
        button.setHorizontalTextPosition(value3);
        assertEquals("HorizontalTextPosition", value3, button.getHorizontalTextPosition());
    }

    public void testGetHorizontalAlignment() {
        int value1 = SwingConstants.WEST;
        int value2 = SwingConstants.RIGHT;
        int value3 = SwingConstants.LEFT;
        assertEquals("default HorizontalAlignment", SwingConstants.CENTER, button
                .getHorizontalAlignment());
        boolean thrown = false;
        try {
            button.setHorizontalAlignment(value1);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue("exception is thrown", thrown);
        button.setHorizontalAlignment(value2);
        assertEquals("HorizontalAlignment", value2, button.getHorizontalAlignment());
        button.setHorizontalAlignment(value3);
        assertEquals("HorizontalAlignment", value3, button.getHorizontalAlignment());
    }

    protected void checkFiringEvents() {
        ConcreteActionListener listener1 = new ConcreteActionListener(true);
        ConcreteChangeListener listener2 = new ConcreteChangeListener(true);
        ConcreteItemListener listener3 = new ConcreteItemListener(true);
        PropertyChangeListener listener4 = new PropertyChangeController(true);
        button.addActionListener(listener1);
        button.addItemListener(listener3);
        button.addChangeListener(listener2);
        button.addPropertyChangeListener(listener4);
    }

    public void testSetMargin() {
        Insets defaultMargin = new Insets(2, 14, 2, 14);
        button.setMargin(defaultMargin);
        Insets margin1 = new Insets(1, 1, 1, 1);
        Insets margin2 = new Insets(2, 2, 2, 2);
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setMargin(margin1);
        listener1.checkPropertyFired(button, "margin", defaultMargin, margin1);
        listener2.checkPropertyFired(button, "margin", defaultMargin, margin1);
        listener1.reset();
        listener2.reset();
        button.setMargin(margin2);
        listener1.checkPropertyFired(button, "margin", margin1, margin2);
        listener2.checkPropertyFired(button, "margin", margin1, margin2);
        listener1.reset();
        listener2.reset();
        button.setMargin(margin2);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testGetMargin() {
        Insets margin1 = new Insets(1, 1, 1, 1);
        Insets margin2 = new Insets(2, 2, 2, 2);
        Insets margin3 = new Insets(3, 3, 3, 3);
        button.setMargin(margin1);
        assertEquals("Margin", margin1, button.getMargin());
        button.setMargin(margin2);
        assertEquals("Margin", margin2, button.getMargin());
        button.setMargin(margin3);
        assertEquals("Margin", margin3, button.getMargin());
    }

    public void testSetSelected() {
        ConcreteItemListener listener1 = new ConcreteItemListener();
        ConcreteItemListener listener2 = new ConcreteItemListener();
        ConcreteChangeListener listener3 = new ConcreteChangeListener();
        button.addItemListener(listener1);
        button.addItemListener(listener2);
        button.addChangeListener(listener3);
        button.setSelected(true);
        assertTrue("event's been fired ", listener1.eventHappened != null);
        assertEquals("event's source ", button, listener1.eventHappened.getSource());
        assertEquals("event's ID ", ItemEvent.ITEM_STATE_CHANGED, listener1.eventHappened
                .getID());
        assertEquals("event's item ", button, listener1.eventHappened.getItem());
        assertEquals("event's StateChange ", 1, listener1.eventHappened.getStateChange());
        assertTrue("event's been fired ", listener2.eventHappened != null);
        assertEquals("event's source ", button, listener2.eventHappened.getSource());
        assertEquals("event's ID ", ItemEvent.ITEM_STATE_CHANGED, listener2.eventHappened
                .getID());
        assertEquals("event's item ", button, listener2.eventHappened.getItem());
        assertEquals("event's StateChange ", 1, listener2.eventHappened.getStateChange());
        assertTrue("state event's been fired ", listener3.eventHappened != null);
        assertEquals("state event fired properly ", ChangeEvent.class, listener3.eventHappened
                .getClass());
        assertEquals("state event fired properly ", button, listener3.eventHappened.getSource());
        listener1.eventHappened = null;
        listener2.eventHappened = null;
        listener3.eventHappened = null;
        button.setSelected(false);
        assertTrue("event's been fired ", listener1.eventHappened != null);
        assertEquals("event's source ", button, listener1.eventHappened.getSource());
        assertEquals("event's ID ", ItemEvent.ITEM_STATE_CHANGED, listener1.eventHappened
                .getID());
        assertEquals("event's item ", button, listener1.eventHappened.getItem());
        assertEquals("event's StateChange ", 2, listener1.eventHappened.getStateChange());
        assertTrue("event's been fired ", listener2.eventHappened != null);
        assertEquals("event's source ", button, listener2.eventHappened.getSource());
        assertEquals("event's ID ", ItemEvent.ITEM_STATE_CHANGED, listener2.eventHappened
                .getID());
        assertEquals("event's item ", button, listener2.eventHappened.getItem());
        assertEquals("event's StateChange ", 2, listener2.eventHappened.getStateChange());
        assertTrue("state event's been fired ", listener3.eventHappened != null);
        assertEquals("state event fired properly ", ChangeEvent.class, listener3.eventHappened
                .getClass());
        assertEquals("state event fired properly ", button, listener3.eventHappened.getSource());
        listener1.eventHappened = null;
        listener2.eventHappened = null;
        listener3.eventHappened = null;
        button.setSelected(false);
        assertNull("event's not been fired ", listener1.eventHappened);
        assertNull("event's not been fired ", listener2.eventHappened);
        assertNull("event's not been fired ", listener3.eventHappened);
        listener1.eventHappened = null;
        listener2.eventHappened = null;
        button.getModel().setSelected(true);
        assertTrue("selected ", button.isSelected());
        assertTrue("event's been fired ", listener1.eventHappened != null);
        assertEquals("event's source ", button, listener1.eventHappened.getSource());
        assertEquals("event's ID ", ItemEvent.ITEM_STATE_CHANGED, listener1.eventHappened
                .getID());
        assertEquals("event's item ", button, listener1.eventHappened.getItem());
        assertEquals("event's StateChange ", 1, listener1.eventHappened.getStateChange());
        assertTrue("event's been fired ", listener2.eventHappened != null);
        assertEquals("event's source ", button, listener2.eventHappened.getSource());
        assertEquals("event's ID ", ItemEvent.ITEM_STATE_CHANGED, listener2.eventHappened
                .getID());
        assertEquals("event's item ", button, listener2.eventHappened.getItem());
        assertEquals("event's StateChange ", 1, listener2.eventHappened.getStateChange());
        assertTrue("state event's been fired ", listener3.eventHappened != null);
        assertEquals("state event fired properly ", ChangeEvent.class, listener3.eventHappened
                .getClass());
        assertEquals("state event fired properly ", button, listener3.eventHappened.getSource());
    }

    public void testIsSelected() {
        assertFalse("default Selected", button.isSelected());
        button.setSelected(true);
        assertTrue("Selected", button.isSelected());
        button.setSelected(false);
        assertFalse("Selected", button.isSelected());
        button.setSelected(true);
        assertTrue("Selected", button.isSelected());
    }

    public void testSetRolloverEnabled() {
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setRolloverEnabled(true);
        listener1
                .checkLastPropertyFired(button, "rolloverEnabled", Boolean.FALSE, Boolean.TRUE);
        listener2
                .checkLastPropertyFired(button, "rolloverEnabled", Boolean.FALSE, Boolean.TRUE);
        listener1.reset();
        listener2.reset();
        button.setRolloverEnabled(false);
        listener1
                .checkLastPropertyFired(button, "rolloverEnabled", Boolean.TRUE, Boolean.FALSE);
        listener2
                .checkLastPropertyFired(button, "rolloverEnabled", Boolean.TRUE, Boolean.FALSE);
        listener1.reset();
        listener2.reset();
        button.setRolloverEnabled(false);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testIsRolloverEnabled() {
        assertFalse("default RolloverEnabled", button.isRolloverEnabled());
        button.setRolloverEnabled(true);
        assertTrue("RolloverEnabled", button.isRolloverEnabled());
        button.setRolloverEnabled(false);
        assertFalse("RolloverEnabled", button.isRolloverEnabled());
        button.setRolloverEnabled(true);
        assertTrue("RolloverEnabled", button.isRolloverEnabled());
    }

    public void testSetFocusPainted() {
        button.setFocusPainted(true);
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setFocusPainted(false);
        listener1.checkLastPropertyFired(button, "focusPainted", Boolean.TRUE, Boolean.FALSE);
        listener2.checkLastPropertyFired(button, "focusPainted", Boolean.TRUE, Boolean.FALSE);
        listener1.reset();
        listener2.reset();
        button.setFocusPainted(true);
        listener1.checkLastPropertyFired(button, "focusPainted", Boolean.FALSE, Boolean.TRUE);
        listener2.checkLastPropertyFired(button, "focusPainted", Boolean.FALSE, Boolean.TRUE);
        listener1.reset();
        listener2.reset();
        button.setFocusPainted(true);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testIsFocusPainted() {
        assertTrue("default FocusPainted", button.isFocusPainted());
        button.setFocusPainted(false);
        assertFalse("FocusPainted", button.isFocusPainted());
        button.setFocusPainted(true);
        assertTrue("FocusPainted", button.isFocusPainted());
        button.setFocusPainted(false);
        assertFalse("FocusPainted", button.isFocusPainted());
    }

    public void testSetContentAreaFilled() {
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setContentAreaFilled(false);
        listener1.checkLastPropertyFired(button, "contentAreaFilled", Boolean.TRUE,
                Boolean.FALSE);
        listener2.checkLastPropertyFired(button, "contentAreaFilled", Boolean.TRUE,
                Boolean.FALSE);
        listener1.reset();
        listener2.reset();
        button.setContentAreaFilled(true);
        listener1.checkLastPropertyFired(button, "contentAreaFilled", Boolean.FALSE,
                Boolean.TRUE);
        listener2.checkLastPropertyFired(button, "contentAreaFilled", Boolean.FALSE,
                Boolean.TRUE);
        listener1.reset();
        listener2.reset();
        button.setContentAreaFilled(true);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testIsContentAreaFilled() {
        assertTrue("default ContentAreaFilled", button.isContentAreaFilled());
        button.setContentAreaFilled(false);
        assertFalse("ContentAreaFilled", button.isContentAreaFilled());
        button.setContentAreaFilled(true);
        assertTrue("ContentAreaFilled", button.isContentAreaFilled());
        button.setContentAreaFilled(false);
        assertFalse("ContentAreaFilled", button.isContentAreaFilled());
    }

    public void testSetBorderPainted() {
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.setBorderPainted(false);
        listener1.checkLastPropertyFired(button, "borderPainted", Boolean.TRUE, Boolean.FALSE);
        listener1.checkLastPropertyFired(button, "borderPainted", Boolean.TRUE, Boolean.FALSE);
        listener1.reset();
        listener2.reset();
        button.setBorderPainted(true);
        listener1.checkLastPropertyFired(button, "borderPainted", Boolean.FALSE, Boolean.TRUE);
        listener1.checkLastPropertyFired(button, "borderPainted", Boolean.FALSE, Boolean.TRUE);
        listener1.reset();
        listener2.reset();
        button.setBorderPainted(true);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
        listener1.reset();
        listener2.reset();
    }

    public void testIsBorderPainted() {
        assertTrue("default BorderPainted", button.isBorderPainted());
        button.setBorderPainted(false);
        assertFalse("BorderPainted", button.isBorderPainted());
        button.setBorderPainted(true);
        assertTrue("BorderPainted", button.isBorderPainted());
        button.setBorderPainted(false);
        assertFalse("BorderPainted", button.isBorderPainted());
    }

    /*
     * is being tested by testGetMultiClickThreshhold()
     */
    public void testSetMultiClickThreshhold() {
    }

    public void testGetMultiClickThreshhold() {
        long value1 = 100l;
        long value2 = 200l;
        assertEquals("default MultiClickThreshhold", 0, button.getMultiClickThreshhold());
        button.setMultiClickThreshhold(value1);
        assertEquals("MultiClickThreshhold", value1, button.getMultiClickThreshhold());
        button.setMultiClickThreshhold(value2);
        assertEquals("MultiClickThreshhold", value2, button.getMultiClickThreshhold());
        button.setMultiClickThreshhold(value2);
        assertEquals("MultiClickThreshhold", value2, button.getMultiClickThreshhold());
    }

    public void testSetMnemonicint2() {
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        ConcreteChangeListener listener3 = new ConcreteChangeListener();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.addChangeListener(listener3);
        button.getModel().setMnemonic(KeyEvent.VK_C);
        listener1.checkLastPropertyFired(button, "mnemonic", new Integer(0), new Integer(
                KeyEvent.VK_C));
        listener2.checkLastPropertyFired(button, "mnemonic", new Integer(0), new Integer(
                KeyEvent.VK_C));
        assertTrue("state event's been fired ", listener3.eventHappened != null);
        assertEquals("state event fired properly ", ChangeEvent.class, listener3.eventHappened
                .getClass());
        assertEquals("state event fired properly ", button, listener3.eventHappened.getSource());
        listener1.reset();
        listener2.reset();
        listener3.eventHappened = null;
    }

    public void testSetMnemonicint1() {
        PropertyChangeController listener1 = new PropertyChangeController();
        PropertyChangeController listener2 = new PropertyChangeController();
        ConcreteChangeListener listener3 = new ConcreteChangeListener();
        button.addPropertyChangeListener(listener1);
        button.addPropertyChangeListener(listener2);
        button.addChangeListener(listener3);
        button.setMnemonic(KeyEvent.VK_C);
        listener1.checkLastPropertyFired(button, "mnemonic", new Integer(0), new Integer(
                KeyEvent.VK_C));
        listener2.checkLastPropertyFired(button, "mnemonic", new Integer(0), new Integer(
                KeyEvent.VK_C));
        assertTrue("state event's been fired ", listener3.eventHappened != null);
        assertEquals("state event fired properly ", ChangeEvent.class, listener3.eventHappened
                .getClass());
        assertEquals("state event fired properly ", button, listener3.eventHappened.getSource());
        listener1.reset();
        listener2.reset();
        listener3.eventHappened = null;
        button.setMnemonic(KeyEvent.VK_D);
        listener1.checkLastPropertyFired(button, "mnemonic", new Integer(KeyEvent.VK_C),
                new Integer(KeyEvent.VK_D));
        listener2.checkLastPropertyFired(button, "mnemonic", new Integer(KeyEvent.VK_C),
                new Integer(KeyEvent.VK_D));
        assertTrue("state event's been fired ", listener3.eventHappened != null);
        assertEquals("state event fired properly ", ChangeEvent.class, listener3.eventHappened
                .getClass());
        assertEquals("state event fired properly ", button, listener3.eventHappened.getSource());
        listener1.reset();
        listener2.reset();
        listener3.eventHappened = null;
        button.setMnemonic(KeyEvent.VK_D);
        assertFalse("event's not been fired ", listener1.isChanged());
        assertFalse("event's not been fired ", listener2.isChanged());
    }

    public void testSetMnemonicchar() {
        button.setMnemonic('c');
        assertEquals("mnemonic", KeyEvent.VK_C, button.getMnemonic());
        button.setMnemonic('f');
        assertEquals("mnemonic", KeyEvent.VK_F, button.getMnemonic());
        button.setMnemonic('-');
        assertEquals("mnemonic", KeyEvent.VK_MINUS, button.getMnemonic());
        button.setMnemonic('\u00FF');
        assertEquals("mnemonic", 255, button.getMnemonic());
        button.setMnemonic('\u01FF');
        assertEquals("mnemonic", 511, button.getMnemonic());
    }

    public void testGetMnemonic() {
        assertEquals("default mnemonic", 0, button.getMnemonic());
        button.setMnemonic(KeyEvent.VK_C);
        assertEquals("mnemonic", KeyEvent.VK_C, button.getMnemonic());
        button.setMnemonic(KeyEvent.VK_F);
        assertEquals("mnemonic", KeyEvent.VK_F, button.getMnemonic());
    }

    public void testSetDisplayedMnemonicIndex() {
        PropertyChangeController listener1 = new PropertyChangeController();
        ConcreteChangeListener listener2 = new ConcreteChangeListener();
        assertEquals("MnemonicIndex", -1, button.getDisplayedMnemonicIndex());
        button.setText("vroooom");
        assertEquals("MnemonicIndex", -1, button.getDisplayedMnemonicIndex());
        button.addPropertyChangeListener(listener1);
        button.addChangeListener(listener2);
        button.setMnemonic(KeyEvent.VK_O);
        listener1.checkPropertyFired(button, "mnemonic", new Integer(0), new Integer(
                KeyEvent.VK_O));
        listener1.checkPropertyFired(button, "displayedMnemonicIndex", new Integer(-1),
                new Integer(2));
        assertTrue("state event's been fired ", listener2.eventHappened != null);
        assertEquals("state event fired properly ", ChangeEvent.class, listener2.eventHappened
                .getClass());
        assertEquals("state event fired properly ", button, listener2.eventHappened.getSource());
        listener1.reset();
        listener2.eventHappened = null;
        button.setDisplayedMnemonicIndex(5);
        listener1.checkPropertyFired(button, "displayedMnemonicIndex", new Integer(2),
                new Integer(5));
        assertNull("state event's not been fired ", listener2.eventHappened);
        listener1.reset();
        button.setDisplayedMnemonicIndex(5);
        assertFalse("state event's not been fired ", listener1.isChanged());
        assertNull("state event's not been fired ", listener2.eventHappened);
        boolean thrown = false;
        try {
            button.setDisplayedMnemonicIndex(7);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue("exception's bees thrown", thrown);
    }

    public void testGetDisplayedMnemonicIndex() {
        assertEquals("MnemonicIndex", -1, button.getDisplayedMnemonicIndex());
        button.setText("vroooom");
        assertEquals("MnemonicIndex", -1, button.getDisplayedMnemonicIndex());
        button.setMnemonic(KeyEvent.VK_O);
        assertEquals("MnemonicIndex", 2, button.getDisplayedMnemonicIndex());
        button.setDisplayedMnemonicIndex(5);
        assertEquals("MnemonicIndex", 5, button.getDisplayedMnemonicIndex());
        button.setDisplayedMnemonicIndex(6);
        assertEquals("MnemonicIndex", 6, button.getDisplayedMnemonicIndex());
        button.setDisplayedMnemonicIndex(-1);
        assertEquals("MnemonicIndex", -1, button.getDisplayedMnemonicIndex());
    }

    public void testGetAlignmentXY() {
        assertEquals("alignmentX ", button.getAlignmentX(), 0.5f, 1e-5);
        assertEquals("alignmentY ", button.getAlignmentY(), 0.5f, 1e-5);
    }

    public void testDoClick() {
        TestButtonModel model = new TestButtonModel();
        button.setModel(model);
        ConcreteAction action = new ConcreteAction();
        button.setAction(action);
        String name = "namename";
        button.setText(name);
        ConcreteActionListener listener = new ConcreteActionListener();
        button.addActionListener(listener);
        button.doClick(0);
        assertTrue(model.wasArmed);
        assertTrue(model.wasPressed);
        assertNotNull(listener.eventHappened);
        assertNotNull(action.eventHappened);
        assertEquals(name, listener.eventHappened.getActionCommand());
        if (!isHarmony()) {
            model.wasArmed = false;
            model.wasPressed = false;
            action.eventHappened = null;
            listener.eventHappened = null;
            button.doClick(10);
            assertTrue(model.wasArmed);
            assertTrue(model.wasPressed);
            assertNotNull(listener.eventHappened);
            assertNotNull(action.eventHappened);
            assertEquals(name, listener.eventHappened.getActionCommand());
        }
    }
    
    /**
     * Regression test for H4655: setMargin(null) causes to default margin
     * */
    public void testH4655() {

        JRadioButton rb = new JRadioButton();
        Insets newInsets = new Insets(10, 10, 10, 10);
        Insets defaultInsets = rb.getMargin();

        rb.setMargin(null);
        assertEquals(defaultInsets, rb.getMargin());
        rb.setMargin(newInsets);
        assertEquals(newInsets, rb.getMargin());
        rb.setMargin(null);
        assertEquals(defaultInsets, rb.getMargin());
    }

    protected int find(final Object[] array, final Object value) {
        int found = 0;
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].equals(value)) {
                    found++;
                }
            }
        }
        return found;
    }

    protected ImageIcon createNewIcon() {
        return new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB));
    }
}
