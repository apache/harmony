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
 * Created on 16.12.2004

 */
package javax.swing;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class KeyStrokeTest extends SwingTestCase {
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(KeyStrokeTest.class);
    }

    /*
     * Class under test for KeyStroke getKeyStroke(String)
     */
    public void testGetKeyStrokeString() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke("INSERT");
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", KeyEvent.VK_INSERT, keyStroke.getKeyCode());
        assertEquals("modifiers are correct", 0, keyStroke.getModifiers());
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke("control DELETE");
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", KeyEvent.VK_DELETE, keyStroke.getKeyCode());
        assertTrue("modifiers are correct",
                (keyStroke.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0);
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke("alt shift X");
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", KeyEvent.VK_X, keyStroke.getKeyCode());
        assertTrue("modifiers are correct",
                (keyStroke.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0);
        assertTrue("modifiers are correct",
                (keyStroke.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0);
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke("altGraph X");
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", KeyEvent.VK_X, keyStroke.getKeyCode());
        assertTrue("modifiers are correct",
                (keyStroke.getModifiers() & InputEvent.ALT_GRAPH_DOWN_MASK) != 0);
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke("alt shift released X");
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", KeyEvent.VK_X, keyStroke.getKeyCode());
        assertTrue("modifiers are correct",
                (keyStroke.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0);
        assertTrue("modifiers are correct",
                (keyStroke.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0);
        assertTrue("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke("typed a");
        assertEquals("keyChar's correct", 'a', keyStroke.getKeyChar());
        assertEquals("keyCode's correct", 0, keyStroke.getKeyCode());
        assertEquals("modifiers are correct", 0, keyStroke.getModifiers());
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke("typed a");
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke("typed a");
        assertTrue("keyStrokes are shared properly", keyStroke1 == keyStroke2);
    }

    /*
     * Class under test for KeyStroke getKeyStroke(Character, int)
     */
    public void testGetKeyStrokeCharacterint() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(new Character('A'),
                InputEvent.ALT_DOWN_MASK);
        assertEquals("keyChar's correct", 'A', keyStroke.getKeyChar());
        assertEquals("keyCode's correct", 0, keyStroke.getKeyCode());
        assertTrue("modifiers are correct", (InputEvent.ALT_DOWN_MASK & keyStroke
                .getModifiers()) != 0);
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke(new Character('t'), InputEvent.CTRL_DOWN_MASK);
        assertEquals("keyChar's correct", 't', keyStroke.getKeyChar());
        assertEquals("keyCode's correct", 0, keyStroke.getKeyCode());
        assertTrue("modifiers are correct", (InputEvent.CTRL_DOWN_MASK & keyStroke
                .getModifiers()) != 0);
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke(new Character('_'), InputEvent.SHIFT_DOWN_MASK);
        assertEquals("keyChar's correct", '_', keyStroke.getKeyChar());
        assertEquals("keyCode's correct", 0, keyStroke.getKeyCode());
        assertTrue("modifiers are correct", (InputEvent.SHIFT_DOWN_MASK & keyStroke
                .getModifiers()) != 0);
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(new Character('_'),
                InputEvent.SHIFT_DOWN_MASK);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(new Character('_'),
                InputEvent.SHIFT_DOWN_MASK);
        assertTrue("keyStrokes are shared properly", keyStroke1 == keyStroke2);
    }

    public void testGetKeyStrokeForEvent() {
        JComponent source = new JPanel();
        KeyEvent event = new KeyEvent(source, KeyEvent.KEY_PRESSED, 0,
                InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_B, 'B');
        KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(event);
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", KeyEvent.VK_B, keyStroke.getKeyCode());
        assertTrue("modifiers are correct", (InputEvent.SHIFT_DOWN_MASK & keyStroke
                .getModifiers()) != 0);
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        event = new KeyEvent(source, KeyEvent.KEY_RELEASED, 0, InputEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_C, 'C');
        keyStroke = KeyStroke.getKeyStrokeForEvent(event);
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", KeyEvent.VK_C, keyStroke.getKeyCode());
        assertTrue("modifiers are correct", (InputEvent.SHIFT_DOWN_MASK & keyStroke
                .getModifiers()) != 0);
        assertTrue("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        event = new KeyEvent(source, KeyEvent.KEY_TYPED, 0, InputEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_UNDEFINED, 'T');
        keyStroke = KeyStroke.getKeyStrokeForEvent(event);
        assertEquals("keyChar's correct", 'T', keyStroke.getKeyChar());
        assertEquals("keyCode's correct", KeyEvent.VK_UNDEFINED, keyStroke.getKeyCode());
        assertTrue("modifiers are correct", (InputEvent.SHIFT_DOWN_MASK & keyStroke
                .getModifiers()) != 0);
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        KeyStroke keyStroke1 = KeyStroke.getKeyStrokeForEvent(event);
        KeyStroke keyStroke2 = KeyStroke.getKeyStrokeForEvent(event);
        assertTrue("keyStrokes are shared properly", keyStroke1 == keyStroke2);
    }

    /*
     * Class under test for KeyStroke getKeyStroke(int, int, boolean)
     */
    public void testGetKeyStrokeintintboolean() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.ALT_DOWN_MASK,
                true);
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", KeyEvent.VK_0, keyStroke.getKeyCode());
        assertTrue("modifiers are correct", (InputEvent.ALT_DOWN_MASK & keyStroke
                .getModifiers()) != 0);
        assertTrue("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK, false);
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", KeyEvent.VK_A, keyStroke.getKeyCode());
        assertTrue("modifiers are correct", (InputEvent.CTRL_DOWN_MASK & keyStroke
                .getModifiers()) != 0);
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, InputEvent.SHIFT_DOWN_MASK, true);
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", KeyEvent.VK_SHIFT, keyStroke.getKeyCode());
        assertTrue("modifiers are correct", (InputEvent.SHIFT_DOWN_MASK & keyStroke
                .getModifiers()) != 0);
        assertTrue("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT,
                InputEvent.SHIFT_DOWN_MASK, true);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT,
                InputEvent.SHIFT_DOWN_MASK, true);
        assertTrue("keyStrokes are shared properly", keyStroke1 == keyStroke2);
    }

    /*
     * Class under test for KeyStroke getKeyStroke(int, int)
     */
    public void testGetKeyStrokeintint() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.ALT_DOWN_MASK);
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", KeyEvent.VK_0, keyStroke.getKeyCode());
        assertTrue("modifiers are correct", (InputEvent.ALT_DOWN_MASK & keyStroke
                .getModifiers()) != 0);
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK);
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", KeyEvent.VK_A, keyStroke.getKeyCode());
        assertTrue("modifiers are correct", (InputEvent.CTRL_DOWN_MASK & keyStroke
                .getModifiers()) != 0);
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, InputEvent.SHIFT_DOWN_MASK);
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", KeyEvent.VK_SHIFT, keyStroke.getKeyCode());
        assertTrue("modifiers are correct", (InputEvent.SHIFT_DOWN_MASK & keyStroke
                .getModifiers()) != 0);
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT,
                InputEvent.SHIFT_DOWN_MASK);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT,
                InputEvent.SHIFT_DOWN_MASK);
        assertTrue("keyStrokes are shared properly", keyStroke1 == keyStroke2);
    }

    /*
     * Class under test for KeyStroke getKeyStroke(char, boolean)
     */
    @SuppressWarnings("deprecation")
    public void testGetKeyStrokecharboolean() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.CHAR_UNDEFINED, false);
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", 0, keyStroke.getKeyCode());
        assertEquals("modifiers are correct", 0, keyStroke.getModifiers());
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke('a', true);
        assertEquals("keyChar's correct", 'a', keyStroke.getKeyChar());
        assertEquals("keyCode's correct", 0, keyStroke.getKeyCode());
        assertEquals("modifiers are correct", 0, keyStroke.getModifiers());
        assertTrue("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke('T', true);
        assertEquals("keyChar's correct", 'T', keyStroke.getKeyChar());
        assertEquals("keyCode's correct", 0, keyStroke.getKeyCode());
        assertEquals("modifiers are correct", 0, keyStroke.getModifiers());
        assertTrue("onKeyRelease is correct", keyStroke.isOnKeyRelease());
    }

    /*
     * Class under test for KeyStroke getKeyStroke(char)
     */
    public void testGetKeyStrokechar() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke((char) -1);
        assertEquals("keyChar's correct", KeyEvent.CHAR_UNDEFINED, keyStroke.getKeyChar());
        assertEquals("keyCode's correct", 0, keyStroke.getKeyCode());
        assertEquals("modifiers are correct", 0, keyStroke.getModifiers());
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke('a');
        assertEquals("keyChar's correct", 'a', keyStroke.getKeyChar());
        assertEquals("keyCode's correct", 0, keyStroke.getKeyCode());
        assertEquals("modifiers are correct", 0, keyStroke.getModifiers());
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        keyStroke = KeyStroke.getKeyStroke('T');
        assertEquals("keyChar's correct", 'T', keyStroke.getKeyChar());
        assertEquals("keyCode's correct", 0, keyStroke.getKeyCode());
        assertEquals("modifiers are correct", 0, keyStroke.getModifiers());
        assertFalse("onKeyRelease is correct", keyStroke.isOnKeyRelease());
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke('a');
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke('a');
        assertTrue("keyStrokes are shared properly", keyStroke1 == keyStroke2);
    }
}
