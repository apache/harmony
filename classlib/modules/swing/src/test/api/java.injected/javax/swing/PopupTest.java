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
 * @author Anton Avtamonov
 */
package javax.swing;

import java.awt.Component;
import java.awt.Point;

public class PopupTest extends BasicSwingTestCase {
    private Popup popup;

    public PopupTest(final String name) {
        super(name);
    }

    @Override
    protected void tearDown() throws Exception {
        popup = null;
    }

    public void testPopup() throws Exception {
        Component content = new JButton("content");
        assertNull(content.getParent());
        popup = new Popup(null, content, 10, 10);
        assertNotNull(content.getParent());
        assertFalse(content.isShowing());
        popup.show();
        assertTrue(content.isShowing());
        assertEquals(new Point(10, 10), content.getLocationOnScreen());
        popup.show();
        assertTrue(content.isShowing());
        popup.hide();
        assertFalse(content.isShowing());
        JPanel owner = new JPanel();
        popup = new Popup(owner, content, 10, 10);
        assertNotNull(content.getParent());
        assertFalse(content.isShowing());
        popup.show();
        assertTrue(content.isShowing());
        assertEquals(new Point(10, 10), content.getLocationOnScreen());
        JFrame ownedFrame = new JFrame();
        ownedFrame.setLocation(100, 100);
        ownedFrame.getContentPane().add(owner);
        ownedFrame.setVisible(true);
        SwingWaitTestCase.isRealized(ownedFrame);
        popup = new Popup(owner, content, 10, 10);
        popup.show();
        assertEquals(new Point(10, 10), content.getLocationOnScreen());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new Popup(null, null, 10, 10);
            }
        });
    }

    public void testShowHide() throws Exception {
        Component content = new JButton("content");
        assertNull(SwingUtilities.getWindowAncestor(content));
        popup = new Popup(null, content, 100, 200);
        assertNotNull(SwingUtilities.getWindowAncestor(content));
        popup.show();
        assertTrue(content.isShowing());
        assertEquals(new Point(100, 200), content.getLocationOnScreen());
        popup.hide();
        assertNull(SwingUtilities.getWindowAncestor(content));
        assertFalse(content.isShowing());
    }
}