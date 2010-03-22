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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.EventListener;

import junit.framework.TestCase;

public class TextComponentTest extends TestCase {

    TextComponent textComp;
    Frame frame;
    TextListener listener;
    private boolean eventProcessed;

    private class MyTextListener implements TextListener {

        public void textValueChanged(TextEvent te) {
            eventProcessed = true;
        }

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        textComp = new TextField();
        listener = new MyTextListener();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if ((frame != null) && frame.isDisplayable()) {
            frame.dispose();
        }
    }

    /*
     * Test method for 'java.awt.TextComponent.addNotify()'
     */
    public void testAddNotify() {
        frame = new Frame();
        frame.add(textComp);
        assertNull(textComp.getGraphics());
        assertFalse(textComp.isDisplayable());
        frame.addNotify();
        assertTrue(textComp.isDisplayable());
        assertNotNull(textComp.getGraphics());

    }

    /*
     * Test method for 'java.awt.TextComponent.getAccessibleContext()'
     */
    public void testGetAccessibleContext() {
        assertTrue(textComp.getAccessibleContext() instanceof 
                   TextComponent.AccessibleAWTTextComponent);
    }

    /*
     * Test method for 'java.awt.TextComponent.getBackground()'
     */
    public void testGetBackground() {
        assertNull(textComp.getBackground());
        frame = new Frame();
        frame.add(textComp);
        frame.addNotify();
        Color bkColor = frame.getBackground();
        assertEquals(bkColor, textComp.getBackground());
        textComp.setEditable(false);
        assertSame(bkColor = SystemColor.control, textComp.getBackground());
        textComp.setEditable(true);
        assertEquals(bkColor = frame.getBackground(), textComp.getBackground());

    }

    /*
     * Test method for 'java.awt.TextComponent.paramString()'
     */
    public void testParamString() {
        String paramStr = textComp.paramString();
        assertTrue(paramStr.indexOf(",text=") >= 0);
        assertTrue(paramStr.indexOf(",editable") >= 0);
        assertTrue(paramStr.indexOf(",selection=0-0") >= 0);

    }

    /*
     * Test method for 'java.awt.TextComponent.removeNotify()'
     */
    public void testRemoveNotify() {
        frame = new Frame();
        frame.add(textComp);
        frame.addNotify();
        assertNotNull(textComp.getGraphics());
        textComp.removeNotify();
        assertFalse(textComp.isDisplayable());
        assertNull(textComp.getGraphics());
    }

    /*
     * Test method for 'java.awt.TextComponent.setBackground(Color)'
     */
    public void testSetBackground() {
        Color bkColor = Color.GREEN;
        textComp.setBackground(bkColor);
        assertSame(bkColor, textComp.getBackground());
        frame = new Frame();
        frame.add(textComp);
        frame.addNotify();
        textComp.setBackground(null);
        bkColor = frame.getBackground();
        assertEquals(bkColor, textComp.getBackground());
    }

    /*
     * Test method for 'java.awt.TextComponent.processEvent(AWTEvent)'
     */
    public void testProcessEvent() {
        eventProcessed = false;
        textComp.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent a0) {
                eventProcessed = true;
            }
        });
        textComp.processEvent(new KeyEvent(textComp, KeyEvent.KEY_PRESSED,
                                           0, 0, 0, 's'));
        assertTrue(eventProcessed);
    }

    /*
     * Test method for 'java.awt.TextComponent.getListeners(Class)'
     */
    public void testGetListeners() {
        Class<TextListener> cls = TextListener.class;
        EventListener[] listeners = textComp.getListeners(cls);
        assertEquals(0, listeners.length);
        textComp.addTextListener(listener);
        assertEquals(1, (listeners = textComp.getListeners(cls)).length);
        assertSame(listener, listeners[0]);

    }

    /*
     * Test method for 'java.awt.TextComponent.enableInputMethods(boolean)'
     */
    public void testEnableInputMethods() {
        // TODO: write test when implemented

    }

    /*
     * Test method for 'java.awt.TextComponent.TextComponent()'
     */
    public void testTextComponent() {
        assertNotNull(textComp);
        assertNull(textComp.textListener);
    }

    /*
     * Test method for 'java.awt.TextComponent.getText()'
     */
    public void testGetText() {
        assertEquals("text is an empty string by default",
                     "", textComp.getText());
    }

    /*
     * Test method for 'java.awt.TextComponent.getCaretPosition()'
     */
    public void testGetCaretPosition() {
        String text = "txt";
        assertEquals(0, textComp.getCaretPosition());
        textComp.setText(text);
        assertEquals(0, textComp.getCaretPosition());
        frame = new Frame();
        frame.add(textComp);
        assertEquals(0, textComp.getCaretPosition());
        frame.setSize(100, 100);
        frame.setVisible(true);

        assertEquals("caret position not updated by setText()",
                     0, textComp.getCaretPosition());
    }

    /*
     * Test method for 'java.awt.TextComponent.getSelectedText()'
     */
    public void testGetSelectedText() {
        assertEquals("empty string is selected", "", textComp.getSelectedText());
    }

    /*
     * Test method for 'java.awt.TextComponent.getSelectionEnd()'
     */
    public void testGetSelectionEnd() {
        assertEquals("default selection end is 0", 0, textComp.getSelectionEnd());
    }

    /*
     * Test method for 'java.awt.TextComponent.getSelectionStart()'
     */
    public void testGetSelectionStart() {
        assertEquals("default selection start is 0", 0,
                     textComp.getSelectionStart());
    }

    /*
     * Test method for 'java.awt.TextComponent.isEditable()'
     */
    public void testIsEditable() {
        assertTrue("editable by default", textComp.isEditable());

    }

    /*
     * Test method for 'java.awt.TextComponent.select(int, int)'
     */
    public void testSelect() {
        textComp.setText("First line of text.");
        int start = 6;
        int end = 10;
        textComp.select(start, end); // select
        assertEquals(start, textComp.getSelectionStart());
        assertEquals(end, textComp.getSelectionEnd());
        assertEquals("line", textComp.getSelectedText());
        textComp.select(start = 13, end = start); // deselect
        assertEquals(start, textComp.getSelectionStart());
        assertEquals(end, textComp.getSelectionEnd());
        assertEquals("", textComp.getSelectedText());

    }

    /*
     * Test method for 'java.awt.TextComponent.selectAll()'
     */
    public void testSelectAll() {
        String text = "Some text";
        textComp.setText(text);
        assertEquals("", textComp.getSelectedText());
        textComp.selectAll();
        assertEquals(0, textComp.getSelectionStart());
        assertEquals(text.length(), textComp.getSelectionEnd());
        assertEquals(text, textComp.getSelectedText());

    }

    /*
     * Test method for 'java.awt.TextComponent.setCaretPosition(int)'
     */
    public void testSetCaretPosition() {
        int pos = 5;
        textComp.setCaretPosition(pos);
        assertEquals(0, textComp.getCaretPosition());
        textComp.setText("Some text");
        textComp.setCaretPosition(pos);
        assertEquals(pos, textComp.getCaretPosition());
        frame = new Frame();
        frame.add(textComp);
        textComp.setText("new");
        assertEquals(pos, textComp.getCaretPosition());
        frame.addNotify();
        assertEquals("making components displayable corrects" +
                     " invalid caret position",
                     textComp.getText().length(),
                     textComp.getCaretPosition());
        textComp.setText("new text");
        assertEquals("setText() on displayable component resets caret position",
                     0, textComp.getCaretPosition());
        textComp.setCaretPosition(pos = 8);
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(textComp.getText().length(), textComp.getCaretPosition());
        try {
            textComp.setCaretPosition(pos = -1);
        } catch (IllegalArgumentException iae) {
            assertEquals("caret is at the end of document",
                         textComp.getText().length(),
                         textComp.getCaretPosition());
            return;
        }
        fail("No exception was thrown!");

    }

    /*
     * Test method for 'java.awt.TextComponent.setEditable(boolean)'
     */
    public void testSetEditable() {
        textComp.setEditable(false);
        assertFalse(textComp.isEditable());
        textComp.setEditable(true);
        assertTrue(textComp.isEditable());

    }

    /*
     * Test method for 'java.awt.TextComponent.setSelectionEnd(int)'
     */
    public void testSetSelectionEnd() {
        textComp.setText("This is some text.");
        int end = 4;
        textComp.setSelectionEnd(end);
        assertEquals(end, textComp.getSelectionEnd());
        assertEquals("This", textComp.getSelectedText());
        textComp.setSelectionStart(5);
        end = textComp.getSelectionStart() - 1;
        textComp.setSelectionEnd(end);
        assertEquals(textComp.getSelectionStart(), textComp.getSelectionEnd());
        textComp.setSelectionEnd(end = 1000);
        assertEquals(textComp.getText().length(),
                     textComp.getSelectionEnd());

    }

    /*
     * Test method for 'java.awt.TextComponent.setSelectionStart(int)'
     */
    public void testSetSelectionStart() {
        textComp.setText("This is some text.");
        int start = 13;
        textComp.setSelectionStart(start);
        textComp.setSelectionEnd(start + 4);
        assertEquals(start, textComp.getSelectionStart());
        assertEquals("text", textComp.getSelectedText());
        textComp.setSelectionStart(textComp.getSelectionEnd() + 1);
        assertEquals(textComp.getSelectionEnd(), textComp.getSelectionStart());
        textComp.setSelectionStart(start= -1);
        assertEquals(0, textComp.getSelectionStart());
    }

    /*
     * Test method for 'java.awt.TextComponent.setText(String)'
     */
    public void testSetText() {
        String text = "Some text";
        textComp.setText(text);
        assertEquals(text, textComp.getText());
        textComp.setText(null);
        assertEquals("", textComp.getText());
    }

    /*
     * Test method for 'java.awt.TextComponent.addTextListener(TextListener)'
     */
    public void testAddRemoveTextListener() {
        textComp.addTextListener(listener);
        assertSame(listener, textComp.textListener);
        TextListener newListener = new MyTextListener();
        textComp.addTextListener(newListener);
        assertTrue("if there are several listeners multicaster is used",
                   textComp.textListener instanceof AWTEventMulticaster);
        AWTEventMulticaster aem = (AWTEventMulticaster) textComp.textListener;
        assertSame(listener, aem.a);
        textComp.removeTextListener(listener);
        assertTrue("if there is only one listener then it is used",
                   textComp.textListener instanceof MyTextListener);
        assertSame(newListener, textComp.textListener);
        textComp.removeTextListener(newListener);
        assertNull(textComp.textListener);

    }

    /*
     * Test method for 'java.awt.TextComponent.getTextListeners()'
     */
    public void testGetTextListeners() {
        TextListener[] listeners = textComp.getTextListeners();
        assertEquals(0, listeners.length);
        textComp.addTextListener(listener);
        assertEquals(1, (listeners = textComp.getTextListeners()).length);
        assertSame(listener, listeners[0]);
    }

    /*
     * Test method for 'java.awt.TextComponent.processTextEvent(TextEvent)'
     */
    public void testProcessTextEvent() {
        eventProcessed = false;
        textComp.addTextListener(listener);
        textComp.processEvent(new TextEvent(textComp, TextEvent.TEXT_VALUE_CHANGED));
        assertTrue("text event processed", eventProcessed);
    }

    public void testDeadLoop4887() {
        final int count[] = new int[1];
        Component c = new TextArea() {
            public void paint(Graphics g) {
                count[0]++;
                setEditable(true);
                setEnabled(true);
            }
        };
        
        Tools.checkDeadLoop(c, count);
    }
}
