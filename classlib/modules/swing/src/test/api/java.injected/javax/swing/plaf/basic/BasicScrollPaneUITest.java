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
package javax.swing.plaf.basic;

import java.awt.Dimension;
import javax.swing.JScrollPane;
import javax.swing.SwingTestCase;

public class BasicScrollPaneUITest extends SwingTestCase {
    private BasicScrollPaneUI ui;

    public BasicScrollPaneUITest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        ui = new BasicScrollPaneUI();
    }

    @Override
    protected void tearDown() throws Exception {
        ui = null;
    }

    public void testCreateUI() throws Exception {
        assertTrue(BasicScrollPaneUI.createUI(null) instanceof BasicScrollPaneUI);
        assertNotSame(BasicScrollPaneUI.createUI(null), BasicScrollPaneUI.createUI(null));
    }

    public void testCreateViewportChangeListener() throws Exception {
        assertNotNull(ui.createViewportChangeListener());
        if (isHarmony()) {
            assertTrue(ui.createViewportChangeListener() instanceof BasicScrollPaneUI.ViewportChangeHandler);
            assertNotSame(ui.createViewportChangeListener(), ui.createViewportChangeListener());
        }
    }

    public void testCreateHSBChangeListener() throws Exception {
        assertNotNull(ui.createHSBChangeListener());
        if (isHarmony()) {
            assertTrue(ui.createHSBChangeListener() instanceof BasicScrollPaneUI.HSBChangeListener);
            assertNotSame(ui.createHSBChangeListener(), ui.createHSBChangeListener());
        }
    }

    public void testCreateVSBChangeListener() throws Exception {
        assertNotNull(ui.createVSBChangeListener());
        if (isHarmony()) {
            assertTrue(ui.createVSBChangeListener() instanceof BasicScrollPaneUI.VSBChangeListener);
            assertNotSame(ui.createVSBChangeListener(), ui.createVSBChangeListener());
        }
    }

    public void testCreateMouseWheelListener() throws Exception {
        assertNotNull(ui.createMouseWheelListener());
        if (isHarmony()) {
            assertTrue(ui.createMouseWheelListener() instanceof BasicScrollPaneUI.MouseWheelHandler);
            assertNotSame(ui.createMouseWheelListener(), ui.createMouseWheelListener());
        }
    }

    public void testCreatePropertyChangeListener() throws Exception {
        assertNotNull(ui.createPropertyChangeListener());
        if (isHarmony()) {
            assertTrue(ui.createPropertyChangeListener() instanceof BasicScrollPaneUI.PropertyChangeHandler);
            assertNotSame(ui.createPropertyChangeListener(), ui.createPropertyChangeListener());
        }
    }

    public void testInstallUninstallListeners() throws Exception {
        ui.scrollpane = new JScrollPane();
        int viewportListenersCount = ui.scrollpane.getViewport().getChangeListeners().length;
        assertNull(ui.viewportChangeListener);
        int scrollPaneListenersCount = ui.scrollpane.getPropertyChangeListeners().length;
        assertNull(ui.spPropertyChangeListener);
        assertNull(ui.hsbChangeListener);
        assertNull(ui.vsbChangeListener);
        ui.installListeners(null);
        assertEquals(viewportListenersCount + 1, ui.scrollpane.getViewport()
                .getChangeListeners().length);
        assertNotNull(ui.viewportChangeListener);
        assertEquals(scrollPaneListenersCount + 1,
                ui.scrollpane.getPropertyChangeListeners().length);
        assertNotNull(ui.spPropertyChangeListener);
        assertNotNull(ui.hsbChangeListener);
        assertNotNull(ui.vsbChangeListener);
        ui.uninstallListeners(null);
        assertEquals(viewportListenersCount,
                ui.scrollpane.getViewport().getChangeListeners().length);
        assertNull(ui.viewportChangeListener);
        assertEquals(scrollPaneListenersCount,
                ui.scrollpane.getPropertyChangeListeners().length);
        assertNull(ui.spPropertyChangeListener);
        assertNull(ui.hsbChangeListener);
        assertNull(ui.vsbChangeListener);
    }

    public void testGetMaximumSize() throws Exception {
        assertEquals(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE), ui.getMaximumSize(null));
        assertNotSame(ui.getMaximumSize(null), ui.getMaximumSize(null));
    }
}
