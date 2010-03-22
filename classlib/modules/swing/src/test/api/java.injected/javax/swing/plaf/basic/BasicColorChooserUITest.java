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

import javax.swing.BasicSwingTestCase;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.colorchooser.DefaultColorSelectionModel;

public class BasicColorChooserUITest extends BasicSwingTestCase {
    private JColorChooser ch;

    private BasicColorChooserUI ui;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ch = new JColorChooser();
        ui = (BasicColorChooserUI) ch.getUI();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        ch = null;
        ui = null;
    }

    public void testCreateUI() throws Exception {
        assertNotNull(BasicColorChooserUI.createUI(ch));
        assertNotSame(BasicColorChooserUI.createUI(ch), BasicColorChooserUI.createUI(ch));
        assertSame(BasicColorChooserUI.class, BasicColorChooserUI.createUI(ch).getClass());
    }

    public void testCreateDefaultChoosers() throws Exception {
        assertNotNull(ui.createDefaultChoosers());
        assertNotSame(ui.createDefaultChoosers(), ui.createDefaultChoosers());
        assertEquals(3, ui.createDefaultChoosers().length);
    }

    public void testCreatePropertychangeListener() throws Exception {
        assertNotNull(ui.createPropertyChangeListener());
        if (isHarmony()) {
            assertNotSame(ui.createPropertyChangeListener(), ui.createPropertyChangeListener());
            assertSame(BasicColorChooserUI.PropertyHandler.class, ui
                    .createPropertyChangeListener().getClass());
        }
    }

    public void testInstallUninstallPreviewPanel() throws Exception {
        assertEquals(2, ch.getComponentCount());
        assertNotNull(findComponent(ch, JTabbedPane.class, true));
        ch.remove(ch.getComponent(1));
        assertEquals(1, ch.getComponentCount());
        ch.setPreviewPanel(new JPanel());
        assertEquals(1, ch.getComponentCount());
        ui.installPreviewPanel();
        assertEquals(1, ch.getComponentCount());
    }

    public void testUninstallDefaultChoosers() throws Exception {
        assertEquals(2, ch.getComponentCount());
        assertNotNull(findComponent(ch, JTabbedPane.class, true));
        assertEquals(3, ((JTabbedPane) findComponent(ch, JTabbedPane.class, true))
                .getTabCount());
        ch.removeChooserPanel(ch.getChooserPanels()[0]);
        assertEquals(2, ch.getComponentCount());
        assertNotNull(findComponent(ch, JTabbedPane.class, true));
        assertEquals(2, ((JTabbedPane) findComponent(ch, JTabbedPane.class, true))
                .getTabCount());
        ch.removeChooserPanel(ch.getChooserPanels()[0]);
        assertEquals(2, ch.getComponentCount());
        assertNull(findComponent(ch, JTabbedPane.class, true));
        ch.removeChooserPanel(ch.getChooserPanels()[0]);
        assertEquals(2, ch.getComponentCount());
        assertNull(findComponent(ch, JTabbedPane.class, true));
        ui.defaultChoosers = new AbstractColorChooserPanel[0];
        ui.uninstallDefaultChoosers();
        assertEquals(2, ch.getComponentCount());
        assertNull(findComponent(ch, JTabbedPane.class, true));
    }

    public void testInstallUninstallListeners() throws Exception {
        ui.uninstallListeners();
        int propChangeListCount = ch.getPropertyChangeListeners().length;
        int changeListcount = ((DefaultColorSelectionModel) ch.getSelectionModel())
                .getChangeListeners().length;
        ui.installListeners();
        assertEquals(propChangeListCount + 1, ch.getPropertyChangeListeners().length);
        assertEquals(changeListcount + 1, ((DefaultColorSelectionModel) ch.getSelectionModel())
                .getChangeListeners().length);
        ui.uninstallListeners();
        assertEquals(propChangeListCount, ch.getPropertyChangeListeners().length);
        assertEquals(changeListcount, ((DefaultColorSelectionModel) ch.getSelectionModel())
                .getChangeListeners().length);
    }
}
