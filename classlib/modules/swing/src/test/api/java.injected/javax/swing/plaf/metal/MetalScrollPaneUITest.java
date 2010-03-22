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
package javax.swing.plaf.metal;

import javax.swing.JScrollPane;
import javax.swing.SwingTestCase;

public class MetalScrollPaneUITest extends SwingTestCase {
    private MetalScrollPaneUI ui;

    public MetalScrollPaneUITest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        ui = new MetalScrollPaneUI();
    }

    @Override
    protected void tearDown() throws Exception {
        ui = null;
    }

    public void testCreateUI() throws Exception {
        assertTrue(MetalScrollPaneUI.createUI(null) instanceof MetalScrollPaneUI);
        assertNotSame(MetalScrollPaneUI.createUI(null), MetalScrollPaneUI.createUI(null));
    }

    public void testCreateScrollBarSwapListener() throws Exception {
        assertNotNull(ui.createScrollBarSwapListener());
    }

    public void testInstallUninstallListeners() throws Exception {
        JScrollPane pane = new JScrollPane();
        ui.installUI(pane);
        int listenerCount = pane.getPropertyChangeListeners().length;
        ui.installListeners(pane);
        assertEquals(listenerCount + 2, pane.getPropertyChangeListeners().length);
        ui.uninstallListeners(pane);
        assertEquals(listenerCount, pane.getPropertyChangeListeners().length);
    }
}
