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
package javax.swing.plaf;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.SwingTestCase;

public class PopupMenuUITest extends SwingTestCase {
    protected PopupMenuUI popupUI;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        popupUI = new PopupMenuUI() {
        };
    }

    @Override
    protected void tearDown() throws Exception {
        popupUI = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.plaf.PopupMenuUI.getPopup(JPopupMenu, int, int)'
     */
    public void testGetPopup() {
        // TODO implement
    }

    /*
     * Test method for 'javax.swing.plaf.PopupMenuUI.isPopupTrigger(MouseEvent)'
     */
    public void testIsPopupTrigger() {
        Component source = new JPanel();
        MouseEvent event1 = new MouseEvent(source, MouseEvent.MOUSE_ENTERED, EventQueue
                .getMostRecentEventTime(), 0, 5, 5, 0, false);
        MouseEvent event2 = new MouseEvent(source, MouseEvent.MOUSE_WHEEL, EventQueue
                .getMostRecentEventTime(), 0, 5, 5, 0, true);
        assertFalse(popupUI.isPopupTrigger(event1));
        assertTrue(popupUI.isPopupTrigger(event2));
    }
}
