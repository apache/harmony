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

import java.awt.Window;

public class PopupFactoryTest extends BasicSwingTestCase {
    public PopupFactoryTest(final String name) {
        super(name);
    }

    public void testGetSetSharedInstance() throws Exception {
        assertNotNull(PopupFactory.getSharedInstance());
        PopupFactory factory = new PopupFactory();
        PopupFactory.setSharedInstance(factory);
        assertSame(factory, PopupFactory.getSharedInstance());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                PopupFactory.setSharedInstance(null);
            }
        });
    }

    public void testGetPopup() throws Exception {
        JButton content1 = new JButton();
        Popup p1 = PopupFactory.getSharedInstance().getPopup(null, content1, 10, 10);
        p1.show();
        Window w1 = SwingUtilities.getWindowAncestor(content1);
        assertNotNull(w1);
        assertTrue(w1.isVisible());
        p1.hide();
        assertFalse(w1.isVisible());
        Popup p11 = PopupFactory.getSharedInstance().getPopup(null, content1, 10, 10);
        if (isHarmony()) {
            assertSame(p1, p11);
        }
        JFrame frame = new JFrame();
        JButton content2 = new JButton();
        frame.getContentPane().add(content1);
        frame.setVisible(true);
        Popup p2 = PopupFactory.getSharedInstance().getPopup(frame, content2, 10, 10);
        p2.show();
        Window w2 = SwingUtilities.getWindowAncestor(content2);
        assertTrue(w2.isVisible());
        frame.setVisible(false);
        assertFalse(w2.isVisible());
        if (isHarmony()) {
            frame.dispose();
            Popup p21 = PopupFactory.getSharedInstance().getPopup(frame, content2, 10, 10);
            assertSame(p2, p21);
        }
    }
}
