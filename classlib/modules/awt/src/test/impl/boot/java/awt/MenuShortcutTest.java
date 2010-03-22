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
 * @author Pavel Dolgov
 */
package java.awt;

import java.awt.event.KeyEvent;

import junit.framework.TestCase;

/**
 * MenuShortcutTest
 */

public class MenuShortcutTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MenuShortcutTest.class);
    }

    public void testMenuShortcut() {
        MenuShortcut s = new MenuShortcut(KeyEvent.VK_SPACE);
        MenuShortcut t = new MenuShortcut(KeyEvent.VK_T, true);
        MenuShortcut u = new MenuShortcut(KeyEvent.VK_U, false);

        assertEquals(KeyEvent.VK_SPACE, s.getKey());
        assertFalse(s.usesShiftModifier());
        assertEquals("Ctrl+Space", s.toString());
        assertEquals("key=32", s.paramString());

        assertEquals(KeyEvent.VK_T, t.getKey());
        assertTrue(t.usesShiftModifier());
        assertEquals("Ctrl+Shift+T", t.toString());
        assertEquals("key=84,usesShiftModifier", t.paramString());

        assertEquals(KeyEvent.VK_U, u.getKey());
        assertFalse(u.usesShiftModifier());
        assertEquals("Ctrl+U", u.toString());
        assertEquals("key=85", u.paramString());

        assertTrue(s.equals(new MenuShortcut(KeyEvent.VK_SPACE)));
        assertFalse(s.equals(new MenuShortcut(KeyEvent.VK_SPACE, true)));
        assertFalse(s.equals(new MenuShortcut(KeyEvent.VK_ENTER)));
        assertFalse(s.equals("Ctrl+Space"));
        assertFalse(s.equals(null));
        assertFalse(s.equals(t));

        assertTrue(new MenuShortcut(KeyEvent.VK_T, true).equals(t));
        assertFalse(new MenuShortcut(KeyEvent.VK_U, true).equals(u));

        assertEquals(new MenuShortcut(KeyEvent.VK_T, true).hashCode(), t.hashCode());
        assertFalse(new MenuShortcut(KeyEvent.VK_U, true).hashCode() == u.hashCode());
    }

}
