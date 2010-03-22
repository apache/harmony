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
import java.util.Enumeration;
import java.util.HashSet;

import junit.framework.TestCase;

/**
 * MenuBarTest
 */
public class MenuBarTest extends TestCase {
    
    MenuBar mb;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mb = new MenuBar();
        Menu file = new Menu("File");
        mb.add(file);
        file.add("New");
        file.add("Open ...");
        file.add("Save");
        file.addSeparator();
        file.add("Exit");
        file.getItem(0).setShortcut(newShortcut(KeyEvent.VK_N));
        file.getItem(1).setShortcut(newShortcut(KeyEvent.VK_O));
        file.getItem(2).setShortcut(newShortcut(KeyEvent.VK_S));
        
        Menu edit = new Menu("Edit");
        mb.add(edit);
        edit.add("Cut");
        edit.add("Copy");
        edit.add("Paste");
        edit.getItem(0).setShortcut(newShortcut(KeyEvent.VK_X));
        edit.getItem(1).setShortcut(newShortcut(KeyEvent.VK_C));
        edit.getItem(2).setShortcut(newShortcut(KeyEvent.VK_V));

        Menu help = new Menu("Help");
        help.add("About");
        mb.add(help);
        mb.setHelpMenu(help);
    }
    
    private MenuShortcut newShortcut(int vkey) {
        return new MenuShortcut(vkey, false);
    }
    
    public void testShortcuts() {
        Enumeration<MenuShortcut> sh = mb.shortcuts();
        assertNotNull(sh);
        HashSet<MenuShortcut> s = new HashSet<MenuShortcut>();
        while (sh.hasMoreElements()) {
            s.add(sh.nextElement());
        }
        checkShortcut(s, KeyEvent.VK_N);
        checkShortcut(s, KeyEvent.VK_O);
        checkShortcut(s, KeyEvent.VK_S);
        checkShortcut(s, KeyEvent.VK_X);
        checkShortcut(s, KeyEvent.VK_C);
        checkShortcut(s, KeyEvent.VK_V);
        assertEquals(0, s.size());
    }

    private void checkShortcut(HashSet<MenuShortcut> s, int vkey) {
        MenuShortcut ms = newShortcut(vkey);
        assertTrue(s.contains(ms));
        s.remove(ms);
    }

    public void testGetShortcutItem() {
        checkShortcutItem("New", KeyEvent.VK_N);
        checkShortcutItem("Open ...", KeyEvent.VK_O);
        checkShortcutItem("Save", KeyEvent.VK_S);
        checkShortcutItem("Cut", KeyEvent.VK_X);
        checkShortcutItem("Copy", KeyEvent.VK_C);
        checkShortcutItem("Paste", KeyEvent.VK_V);
        checkShortcutItemNotExists(KeyEvent.VK_N, true);
        checkShortcutItemNotExists(KeyEvent.VK_Z, false);
        checkShortcutItemNotExists(KeyEvent.VK_Z, true);
    }

    private void checkShortcutItem(String label, int vkey) {
        MenuShortcut ms = newShortcut(vkey);
        MenuItem mi = mb.getShortcutMenuItem(ms);
        assertNotNull(mi);
        assertEquals(label, mi.getLabel());
    }

    private void checkShortcutItemNotExists(int vkey, boolean shift) {
        MenuShortcut ms = new MenuShortcut(vkey, shift);
        MenuItem mi = mb.getShortcutMenuItem(ms);
        assertNull(mi);
    }
    
    public void testRemove1() {
        MenuBar m = new MenuBar();
        m.remove(null);
        assertTrue(true);
    }
    
    public void testSetHelpMenu() {
        MenuBar m = new MenuBar();
        m.setHelpMenu(null);
        assertTrue(true);
    }
}
