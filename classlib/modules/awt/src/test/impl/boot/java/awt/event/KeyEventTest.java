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
 * @author Michael Danilov
 */
package java.awt.event;

import java.awt.Button;
import java.awt.Container;

import junit.framework.TestCase;

public class KeyEventTest extends TestCase {

    public final void testGetKeyText() throws StringIndexOutOfBoundsException {
        assertEquals(KeyEvent.getKeyText(KeyEvent.VK_1), "1");
        assertEquals(KeyEvent.getKeyText(KeyEvent.VK_SHIFT), "Shift");
        assertEquals(KeyEvent.getKeyText(KeyEvent.VK_INVERTED_EXCLAMATION_MARK),
                "Inverted Exclamation Mark");
//        assertTrue(KeyEvent.getKeyText(KeyEvent.VK_SEPARATOR).startsWith("NumPad ,"));
        assertEquals(KeyEvent.getKeyText(-16), "Unknown keyCode: -0x10");
        
        // Regression test for HARMONY-2406
        KeyEvent.getKeyText(4);
    }

    public final void testGetKeyModifiersText() {
        assertTrue(KeyEvent.getKeyModifiersText(InputEvent.ALT_DOWN_MASK).indexOf("Alt") != -1);
        assertTrue(KeyEvent.getKeyModifiersText(InputEvent.ALT_GRAPH_DOWN_MASK).indexOf("Alt Graph") != -1);
        assertTrue(KeyEvent.getKeyModifiersText(InputEvent.CTRL_DOWN_MASK).indexOf("Ctrl") != -1);
        assertTrue(KeyEvent.getKeyModifiersText(InputEvent.SHIFT_DOWN_MASK).indexOf("Shift") != -1);
        assertTrue(KeyEvent.getKeyModifiersText(InputEvent.META_DOWN_MASK).indexOf("Meta") != -1);
        assertTrue(KeyEvent.getKeyModifiersText(InputEvent.ALT_MASK).indexOf("Alt") != -1);
        assertTrue(KeyEvent.getKeyModifiersText(InputEvent.ALT_GRAPH_MASK).indexOf("Alt Graph") != -1);
        assertTrue(KeyEvent.getKeyModifiersText(InputEvent.CTRL_MASK).indexOf("Ctrl") != -1);
        assertTrue(KeyEvent.getKeyModifiersText(InputEvent.SHIFT_MASK).indexOf("Shift") != -1);
        assertTrue(KeyEvent.getKeyModifiersText(InputEvent.META_MASK).indexOf("Meta") != -1);
    }

    public final void testKeyEventComponentintlongintintchar() {
        long when = 1000000000;
        Button button = new Button("Button");
        KeyEvent event = new KeyEvent(button, KeyEvent.KEY_PRESSED, when,
                InputEvent.ALT_GRAPH_MASK, KeyEvent.VK_A, 'a');

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), KeyEvent.KEY_PRESSED);
        assertEquals(event.getWhen(), when);
        assertEquals(event.getModifiers(), InputEvent.ALT_GRAPH_MASK);
        assertEquals(event.getModifiersEx(), InputEvent.ALT_GRAPH_DOWN_MASK);
        assertEquals(event.getKeyCode(), KeyEvent.VK_A);
        assertEquals(event.getKeyChar(), 'a');
        assertEquals(event.getKeyLocation(), KeyEvent.KEY_LOCATION_UNKNOWN);

        boolean typedKeyCode = false;
        try {
            event = new KeyEvent(button, KeyEvent.KEY_TYPED, when,
                    InputEvent.ALT_GRAPH_MASK, KeyEvent.VK_A, 'a');
        } catch (Throwable t) {
            typedKeyCode = true;
        }
        assertTrue(typedKeyCode);

        boolean typedUndefinedChar = false;
        try {
            event = new KeyEvent(button, KeyEvent.KEY_TYPED, when,
                    InputEvent.ALT_GRAPH_MASK, KeyEvent.VK_UNDEFINED, KeyEvent.CHAR_UNDEFINED);
        } catch (Throwable t) {
            typedUndefinedChar = true;
        }
        assertTrue(typedUndefinedChar);
    }

    public final void testKeyEventComponentintlongintintcharint() {
        long when = 1000000000;
        Button button = new Button("Button");
        KeyEvent event = new KeyEvent(button, KeyEvent.KEY_PRESSED, when,
                InputEvent.ALT_GRAPH_MASK, KeyEvent.VK_A, 'a', KeyEvent.KEY_LOCATION_NUMPAD);

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), KeyEvent.KEY_PRESSED);
        assertEquals(event.getWhen(), when);
        assertEquals(event.getModifiers(), InputEvent.ALT_GRAPH_MASK);
        assertEquals(event.getModifiersEx(), InputEvent.ALT_GRAPH_DOWN_MASK);
        assertEquals(event.getKeyCode(), KeyEvent.VK_A);
        assertEquals(event.getKeyChar(), 'a');
        assertEquals(event.getKeyLocation(), KeyEvent.KEY_LOCATION_NUMPAD);

        boolean typedKeyCode = false;
        try {
            event = new KeyEvent(button, KeyEvent.KEY_TYPED, when,
                    InputEvent.ALT_GRAPH_MASK, KeyEvent.VK_A, 'a', KeyEvent.KEY_LOCATION_NUMPAD);
        } catch (Throwable t) {
            typedKeyCode = true;
        }
        assertTrue(typedKeyCode);

        boolean typedUndefinedChar = false;
        try {
            event = new KeyEvent(button, KeyEvent.KEY_TYPED, when,
                    InputEvent.ALT_GRAPH_MASK, KeyEvent.VK_UNDEFINED, KeyEvent.CHAR_UNDEFINED,
                    KeyEvent.KEY_LOCATION_NUMPAD);
        } catch (Throwable t) {
            typedUndefinedChar = true;
        }
        assertTrue(typedUndefinedChar);
        
        // Regression for HARMONY-2491
        try {
            new KeyEvent(new Container(), (-1), 62554L, 5, 1, '\1', -1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        try {
            new KeyEvent(new Container(), (-1), 62554L, 5, 1, '\1', 5);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public final void testGetSetKeyCode() {
        Button button = new Button("Button");
        KeyEvent event = new KeyEvent(button, KeyEvent.KEY_PRESSED, 0l,
                InputEvent.ALT_GRAPH_MASK, KeyEvent.VK_A, 'a');

        event.setKeyCode(KeyEvent.VK_1);
        assertEquals(event.getKeyCode(), KeyEvent.VK_1);
        event.setKeyCode(KeyEvent.VK_UNDEFINED);
        assertEquals(event.getKeyCode(), KeyEvent.VK_UNDEFINED);

        event = new KeyEvent(button, KeyEvent.KEY_TYPED, 0l,
                InputEvent.ALT_GRAPH_MASK, KeyEvent.VK_UNDEFINED, 'a', KeyEvent.KEY_LOCATION_NUMPAD);
        boolean typedKeyCode = false;
        try {
            event.setKeyCode(KeyEvent.VK_A);
        } catch (Throwable t) {
            typedKeyCode = true;
        }
        assertFalse(typedKeyCode);
    }

    public final void testGetSetKeyChar() {
        Button button = new Button("Button");
        KeyEvent event = new KeyEvent(button, KeyEvent.KEY_PRESSED, 0l,
                InputEvent.ALT_GRAPH_MASK, KeyEvent.VK_A, 'a');

        event.setKeyChar('1');
        assertEquals(event.getKeyChar(), '1');
        event.setKeyChar(KeyEvent.CHAR_UNDEFINED);
        assertEquals(event.getKeyChar(), KeyEvent.CHAR_UNDEFINED);

        event = new KeyEvent(button, KeyEvent.KEY_TYPED, 0l,
                InputEvent.ALT_GRAPH_MASK, KeyEvent.VK_UNDEFINED, 'a', KeyEvent.KEY_LOCATION_NUMPAD);
        boolean typedKeyChar = false;
        try {
            event.setKeyChar(KeyEvent.CHAR_UNDEFINED);
        } catch (Throwable t) {
            typedKeyChar = true;
        }
        assertFalse(typedKeyChar);
    }

    public final void testIsActionKey() {
        Button button = new Button("Button");
        KeyEvent event = new KeyEvent(button, KeyEvent.KEY_PRESSED, 0l,
                InputEvent.ALT_GRAPH_MASK, KeyEvent.VK_A, 'a');

        assertFalse(event.isActionKey());
        event = new KeyEvent(button, KeyEvent.KEY_PRESSED, 0l,
                InputEvent.ALT_GRAPH_MASK, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED);
        assertTrue(event.isActionKey());
        event.setKeyChar('1');
        assertFalse(event.isActionKey());
        event.setKeyChar(KeyEvent.CHAR_UNDEFINED);
        event.setKeyCode(KeyEvent.VK_SHIFT);
        assertFalse(event.isActionKey());
        event.setKeyChar(KeyEvent.CHAR_UNDEFINED);
        event.setKeyCode(KeyEvent.VK_UNDEFINED);
        assertFalse(event.isActionKey());
    }

    public final void testParamString() {
        Button button = new Button("Button");
        KeyEvent event = new KeyEvent(button, KeyEvent.KEY_PRESSED, 0l,
                InputEvent.ALT_GRAPH_MASK, KeyEvent.VK_A, 'a', KeyEvent.KEY_LOCATION_NUMPAD);

        assertEquals(event.paramString(),
                "KEY_PRESSED,keyCode=65,keyChar='a',modifiers=Alt Graph,extModifiers=Alt Graph,keyLocation=KEY_LOCATION_NUMPAD");
        event = new KeyEvent(button, KeyEvent.KEY_PRESSED + 1024, 0l,
                InputEvent.ALT_GRAPH_MASK, KeyEvent.VK_A, 'a', KeyEvent.KEY_LOCATION_NUMPAD);
        assertEquals(event.paramString(),
                "unknown type,keyCode=65,keyChar='a',modifiers=Alt Graph,extModifiers=Alt Graph,keyLocation=KEY_LOCATION_NUMPAD");
    }
}
