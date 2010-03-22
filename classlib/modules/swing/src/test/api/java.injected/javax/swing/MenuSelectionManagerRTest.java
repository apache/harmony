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
 */
package javax.swing;

public class MenuSelectionManagerRTest extends SwingTestCase {
    protected MenuSelectionManager manager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        manager = new MenuSelectionManager();
    }

    @Override
    protected void tearDown() throws Exception {
        manager = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.MenuSelectionManager.isComponentPartOfCurrentMenu(Component)'
     */
    public void testIsComponentPartOfCurrentMenu() {
        final JPopupMenu menu8 = new JPopupMenu();
        final JMenu menu9 = new JMenu();
        final JMenuItem menu10 = new JMenuItem();
        final JMenuItem menu11 = new JMenuItem();
        menu8.add(menu9);
        menu8.add(menu10);
        menu9.add(menu11);
        MenuElement[] path7 = new MenuElement[] { menu8 };
        MenuElement[] path8 = new MenuElement[] { menu8, menu9 };
        MenuElement[] path9 = new MenuElement[] { menu8, menu10 };
        manager.setSelectedPath(path7);
        assertTrue(manager.isComponentPartOfCurrentMenu(menu8));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu9));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu10));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu11));
        manager.setSelectedPath(path8);
        assertTrue(manager.isComponentPartOfCurrentMenu(menu8));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu9));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu10));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu11));
        manager.setSelectedPath(path9);
        assertTrue(manager.isComponentPartOfCurrentMenu(menu8));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu9));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu10));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu11));
    }
}
