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
/*
 * Created on 15.02.2005
 *
 */
package java.awt;

import java.awt.event.*;

import junit.framework.TestCase;
@SuppressWarnings("serial")
public class MenuTest extends TestCase {
    public void testAddRemove() {
        Component comp = new Component() {};
        // create topmost menu
        PopupMenu popup = new PopupMenu();
        comp.add(popup);
        assertTrue(popup.getParent() == comp);

        // create submenu
        Menu menu = new Menu("Menu");
        popup.add(menu);
        assertEquals(popup, menu.getParent());
        assertEquals(1, popup.getItemCount());

        // add item to submenu
        MenuItem item = new MenuItem("Item");
        menu.add(item);
        assertEquals(menu, item.getParent());
        assertEquals(1, menu.getItemCount());

        // move item from submenu to topmost menu
        popup.add(item);
        assertEquals(popup, item.getParent());
        assertEquals(0, menu.getItemCount());
        assertEquals(2, popup.getItemCount());

        // remove submenu from topmost menu
        popup.remove(menu);
        assertEquals(1, popup.getItemCount());

        // remove item from topmost menu
        popup.remove(item);
        assertEquals(0, popup.getItemCount());
    }

    public void testAddRemoveListener() {
        MenuItem item = new MenuItem("Item");
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }};
        item.addActionListener(listener);
        assertNotNull(item.getActionListeners());
        assertEquals(1, item.getActionListeners().length);
        assertEquals(listener, item.getActionListeners()[0]);
    }
}
