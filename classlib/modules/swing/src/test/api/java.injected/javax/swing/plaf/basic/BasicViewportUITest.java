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
 * @author Sergey Burlak
 */
package javax.swing.plaf.basic;

import javax.swing.JViewport;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;

public class BasicViewportUITest extends SwingTestCase {
    private JViewport viewport;

    @Override
    protected void setUp() throws Exception {
        viewport = new JViewport();
    }

    @Override
    protected void tearDown() throws Exception {
        viewport = null;
    }

    public void testCreateUI() {
        assertNotNull(BasicViewportUI.createUI(viewport));
        assertTrue(BasicViewportUI.createUI(new JViewport()) == BasicViewportUI
                .createUI(new JViewport()));
    }

    public void testDefaultValues() {
        BasicViewportUI.createUI(viewport);
        assertEquals(UIManager.getColor("Viewport.background"), viewport.getBackground());
        assertEquals(UIManager.getColor("Viewport.foreground"), viewport.getForeground());
        assertEquals(UIManager.getFont("Viewport.font"), viewport.getFont());
    }
}
