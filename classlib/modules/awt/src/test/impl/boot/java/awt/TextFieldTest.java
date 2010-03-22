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
 * @author Dmitry A. Durnev
 */
package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.EventListener;

import junit.framework.TestCase;

public class TextFieldTest extends TestCase {
    TextField tf;
    Frame frame;
    private boolean eventProcessed;
    ActionListener listener;

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent ae) {
            eventProcessed = true;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tf = new TextField();
        listener = new MyActionListener();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if ((frame != null) && frame.isDisplayable()) {
            frame.dispose();
        }
    }

    /*
     * Test method for 'java.awt.TextField.addNotify()'
     */
    public void testAddNotify() {
        frame = new Frame();
        frame.add(tf);
        assertNull(tf.getGraphics());
        assertFalse(tf.isDisplayable());
        frame.addNotify();
        assertTrue(tf.isDisplayable());
        assertNotNull(tf.getGraphics());
    }

    /*
     * Test method for 'java.awt.TextField.getAccessibleContext()'
     */
    public void testGetAccessibleContext() {
        assertTrue(tf.getAccessibleContext() instanceof 
                   TextField.AccessibleAWTTextField);
    }

    /*
     * Test method for 'java.awt.TextField.paramString()'
     */
    public void testParamString() {
        tf.setEchoChar('q');
        String paramStr = tf.paramString();
        assertEquals(0, paramStr.indexOf("textfield"));
        assertTrue(paramStr.indexOf(",echo=q") >= 0);
    }

    /*
     * Test method for 'java.awt.TextField.getMinimumSize()'
     */
    public void testGetMinimumSize() {
        Dimension minSize = new Dimension();
        assertEquals(minSize, tf.getMinimumSize());
        minSize.setSize(130, 160);
        tf.setMinimumSize(minSize);
        assertNotSame(minSize, tf.getMinimumSize());
        assertEquals(minSize, tf.getMinimumSize());
        tf.setMinimumSize(null);
        assertEquals(new Dimension(), tf.getMinimumSize());
        makeDisplayable();
        assertEquals("By default minimum size is set for 0 columns",
                     tf.getMinimumSize(0), tf.getMinimumSize());
        tf.setText("__");
        assertEquals("for 2 chars minimum size is 2 columns",
                     tf.getMinimumSize(2), tf.getMinimumSize());
    }

    /*
     * Test method for 'java.awt.TextField.minimumSize()'
     */
    @SuppressWarnings("deprecation")
    public void testMinimumSize() {
        Dimension minSize = new Dimension();
        assertEquals(minSize, tf.minimumSize());
        minSize.setSize(60, 10);
        tf.setMinimumSize(minSize);
        assertNotSame(minSize, tf.minimumSize());
        assertEquals(minSize, tf.minimumSize());
        tf.setMinimumSize(null);
        assertEquals(new Dimension(), tf.minimumSize());
        makeDisplayable();
        assertEquals("By default minimum size is set for 0 columns",
                     tf.minimumSize(0), tf.minimumSize());

        int cols = 80;
        tf.setColumns(cols);
        assertEquals(tf.minimumSize(cols), tf.minimumSize());
    }

    /*
     * Test method for 'java.awt.TextField.getPreferredSize()'
     */
    public void testGetPreferredSize() {
        Dimension prefSize = new Dimension();
        assertEquals(prefSize, tf.getPreferredSize());
        prefSize.setSize(5, 6);
        tf.setPreferredSize(prefSize);
        assertNotSame(prefSize, tf.getPreferredSize());
        assertEquals(prefSize, tf.getPreferredSize());
        tf.setPreferredSize(null);
        assertEquals(new Dimension(), tf.getPreferredSize());
        makeDisplayable();
        assertEquals("By default preferred size is equal to minimum size",
                     tf.getMinimumSize(), tf.getPreferredSize());
    }

    /*
     * Test method for 'java.awt.TextField.preferredSize()'
     */
    @SuppressWarnings("deprecation")
    public void testPreferredSize() {
        Dimension prefSize = new Dimension();
        assertEquals(tf.minimumSize(), tf.preferredSize());
        prefSize.setSize(80, 20);
        tf.setPreferredSize(prefSize);
        assertNotSame(prefSize, tf.preferredSize());
        assertEquals(prefSize, tf.preferredSize());
        tf.setPreferredSize(null);
        assertEquals(new Dimension(), tf.preferredSize());
        makeDisplayable();
        assertEquals("By default preferred size is equal to minimum size",
                     tf.minimumSize(), tf.preferredSize());
    }

    /*
     * Test method for 'java.awt.TextField.processEvent(AWTEvent)'
     */
    public void testProcessEvent() {
        eventProcessed = false;
        tf.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent ke) {
                eventProcessed = true;
            }
        });
        tf.processEvent(new KeyEvent(tf, KeyEvent.KEY_TYPED,
                                           0, 0, 0, 's'));
        assertTrue(eventProcessed);
    }

    /*
     * Test method for 'java.awt.TextField.getListeners(Class)'
     */
    public void testGetListeners() {
        Class<ActionListener> cls = ActionListener.class;
        EventListener[] listeners = tf.getListeners(cls);
        assertEquals(0, listeners.length);
        tf.addActionListener(listener);
        assertEquals(1, (listeners = tf.getListeners(cls)).length);
        assertSame(listener, listeners[0]);
    }

    /*
     * Test method for 'java.awt.TextField.setText(String)'
     */
    public void testSetText() {
        String text = "Some text";
        tf.setText(text);
        assertEquals(text, tf.getText());
        text += "\nsecond line";
        tf.setText(text);
        // multi-line text is allowed:
        assertEquals(text, tf.getText());
        tf.setText(null);
        assertEquals("", tf.getText());
        tf.setText(text = "some old text");
        int pos = 8;
        tf.setCaretPosition(pos);
//        assertEquals(pos, tf.getCaretPosition());
        tf.setText(text = "new");
        assertEquals(text, tf.getText());
        // caret position remains invalid:
        assertEquals(pos, tf.getCaretPosition());
        makeDisplayable();
        frame.setSize(100, 100);
        frame.setVisible(true);

        // on a displayable component caret position
        // is always correct:
        assertEquals(text.length(), tf.getCaretPosition());
    }

    /*
     * Test method for 'java.awt.TextField.TextField(String)'
     */
    public void testTextFieldString() {
        String text = "text";
        tf = new TextField(text);
        assertEquals(text, tf.getText());
        assertEquals(text.length(), tf.getColumns());

        tf = new TextField(text = null);
        assertEquals("", tf.getText());
        assertEquals(0, tf.getColumns());
    }

    /*
     * Test method for 'java.awt.TextField.TextField(int)'
     */
    public void testTextFieldInt() {
        int cols = 80;
        tf = new TextField(cols);
        assertEquals("", tf.getText());
        assertEquals(cols, tf.getColumns());
        tf = new TextField(cols = -1);
        assertEquals("", tf.getText());
        assertEquals(0, tf.getColumns());
    }

    /*
     * Test method for 'java.awt.TextField.TextField()'
     */
    public void testTextField() {
        assertNotNull(tf);
        assertEquals("", tf.getText());
        assertEquals(0, tf.getColumns());
    }

    /*
     * Test method for 'java.awt.TextField.TextField(String, int)'
     */
    public void testTextFieldStringInt() {
        int cols = 2;
        String text = "text";
        tf = new TextField(text, cols);
        assertEquals(text, tf.getText());
        assertEquals(cols, tf.getColumns());
    }

    /*
     * Test method for 'java.awt.TextField.getColumns()'
     */
    public void testGetColumns() {
        assertEquals(0, tf.getColumns());
        tf.setText("some text");
        assertEquals(0, tf.getColumns());

    }

    /*
     * Test method for 'java.awt.TextField.getMinimumSize(int)'
     */
    public void testGetMinimumSizeInt() {
        int cols = 1;
        Dimension minSize = new Dimension();
        assertEquals(minSize, tf.getMinimumSize(cols));
        minSize.setSize(120, 130);
        tf.setMinimumSize(minSize);
        assertEquals(minSize, tf.getMinimumSize(cols));
        tf.setMinimumSize(null);
        assertEquals(new Dimension(), tf.getMinimumSize(cols));
        makeDisplayable();
        minSize.setSize(tf.getMinimumSize(cols));
        int w =  minSize.width;
        int h = minSize.height;
        assertTrue(w > 0);
        assertTrue(tf.getMinimumSize(0).width > 0);
        assertTrue(tf.getMinimumSize(cols).height > 0);
        assertEquals(tf.getMinimumSize(0).height, h);
        int dw = (tf.getMinimumSize(cols * 2).width - w);
        int dw1 = (tf.getMinimumSize(cols * 3).width -
                tf.getMinimumSize(cols * 2).width);
        assertEquals(dw, dw1);

    }

    /*
     * Test method for 'java.awt.TextField.getPreferredSize(int)'
     */
    public void testGetPreferredSizeInt() {
        int cols = 3;
        Dimension prefSize = new Dimension();
        assertEquals(tf.getMinimumSize(cols),
                     tf.getPreferredSize(cols));
        prefSize.setSize(120, 13);
        tf.setPreferredSize(prefSize);
        assertEquals(prefSize, tf.getPreferredSize(cols));
        tf.setPreferredSize(null);
        assertEquals(new Dimension(), tf.getPreferredSize(cols));
        makeDisplayable();
        assertEquals(tf.getMinimumSize(cols), tf.getPreferredSize(cols));
        tf.setPreferredSize(prefSize);
        assertEquals(tf.getMinimumSize(cols), tf.getPreferredSize(cols));
    }

    /*
     * Test method for 'java.awt.TextField.minimumSize(int)'
     */
    @SuppressWarnings("deprecation")
    public void testMinimumSizeInt() {
        makeDisplayable();
        int cols = 25;
        assertEquals(tf.getMinimumSize(cols), tf.minimumSize(cols));
    }

    private void makeDisplayable() {
        frame = new Frame();
        frame.add(tf);
        frame.addNotify();
    }

    /*
     * Test method for 'java.awt.TextField.preferredSize(int)'
     */
    @SuppressWarnings("deprecation")
    public void testPreferredSizeInt() {
        makeDisplayable();
        int cols = 125;
        assertEquals(tf.getPreferredSize(cols), tf.preferredSize(cols));
    }

    /*
     * Test method for 'java.awt.TextField.setColumns(int)'
     */
    public void testSetColumns() {
        int cols = 80;
        tf.setColumns(cols);
        assertEquals(cols, tf.getColumns());
        try {
            tf.setColumns(-1);
        } catch (IllegalArgumentException iae) {
            assertEquals(cols, tf.getColumns());
            return;
        }
        fail("no exception was thrown!");
    }

    /*
     * Test method for 'java.awt.TextField.echoCharIsSet()'
     */
    public void testEchoCharIsSet() {
        assertFalse(tf.echoCharIsSet());

    }

    /*
     * Test method for 'java.awt.TextField.getEchoChar()'
     */
    public void testGetEchoChar() {
        assertEquals('\0', tf.getEchoChar());

    }

    /*
     * Test method for 'java.awt.TextField.setEchoChar(char)'
     */
    public void testSetEchoChar() {
        char echoChar = 'q';
        tf.setEchoChar(echoChar);
        assertTrue(tf.echoCharIsSet());
        assertEquals(echoChar, tf.getEchoChar());
        tf.setEchoChar(echoChar = '\0');
        assertFalse(tf.echoCharIsSet());
        assertEquals(echoChar, tf.getEchoChar());

    }

    /*
     * Test method for 'java.awt.TextField.setEchoCharacter(char)'
     */
    @SuppressWarnings("deprecation")
    public void testSetEchoCharacter() {
        char echoChar = '*';
        tf.setEchoCharacter(echoChar);
        assertTrue(tf.echoCharIsSet());
        assertEquals(echoChar, tf.getEchoChar());
    }

    /*
     * Test method for 'java.awt.TextField.addActionListener(ActionListener)'
     */
    public void testAddGetRemoveActionListener() {
        assertTrue(tf.getActionListeners().length == 0);

        tf.addActionListener(listener);
        assertTrue(tf.getActionListeners().length == 1);
        assertTrue(tf.getActionListeners()[0] == listener);

        tf.removeActionListener(listener);
        assertTrue(tf.getActionListeners().length == 0);
    }

    /*
     * Test method for 'java.awt.TextField.processActionEvent(ActionEvent)'
     */
    public void testProcessActionEvent() {
        eventProcessed = false;
        tf.addActionListener(listener);
        tf.processEvent(new ActionEvent(tf, ActionEvent.ACTION_PERFORMED,
                                        null, 0, 0));
        assertTrue(eventProcessed);
    }
    
    public void testDeadLoop4887() {
        final int count[] = new int[1];
        Component c = new TextField() {
            public void paint(Graphics g) {
                count[0]++;
                setEchoChar(' ');
            }
        };
        
        Tools.checkDeadLoop(c, count);
    }

}
