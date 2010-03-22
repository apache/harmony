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
package javax.swing.event;

import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import junit.framework.TestCase;

public class MenuKeyEventTest extends TestCase {
    protected MenuKeyEvent event;

    @Override
    protected void tearDown() throws Exception {
        event = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.event.MenuKeyEvent.MenuKeyEvent(Component, int, long, int, int, char, MenuElement[], MenuSelectionManager)'
     */
    public void testMenuKeyEvent() {
        // TODO implement
    }

    /*
     * Test method for 'javax.swing.event.MenuKeyEvent.getPath()'
     */
    public void testGetPath() {
        MenuElement[] path = new MenuElement[1];
        event = new MenuKeyEvent(new JMenuItem(), 1, 2, 3, 4, 'a', path,
                new MenuSelectionManager());
        assertSame(path, event.getPath());
        event = new MenuKeyEvent(new JMenuItem(), 1, 2, 3, 4, 'a', null,
                new MenuSelectionManager());
        assertSame(null, event.getPath());
    }

    /*
     * Test method for 'javax.swing.event.MenuKeyEvent.getMenuSelectionManager()'
     */
    public void testGetMenuSelectionManager() {
        // TODO implement
    }
}
