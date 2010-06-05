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
 * @author Vadim L. Bogdanov
 */
package javax.swing.plaf.basic;

import java.beans.PropertyChangeListener;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.SwingTestCase;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import java.util.EventListener;

public class BasicRootPaneUITest extends SwingTestCase {
    private JRootPane rootPane;

    private BasicRootPaneUI ui;

    public BasicRootPaneUITest(final String name) {
        super(name);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        rootPane = new JRootPane();
        ui = (BasicRootPaneUI) BasicRootPaneUI.createUI(rootPane);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for constructor
     */
    public void testBasicRootPaneUI() {
        ui = new BasicRootPaneUI();
        assertTrue(ui != null);
    }

    /*
     *
     */
    protected boolean isListenerInstalled(final JComponent c,
            final PropertyChangeListener listener) {
        EventListener[] listeners = rootPane.getPropertyChangeListeners();
        boolean result = false;
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] == listener) {
                result = true;
                break;
            }
        }
        return result;
    }

    /*
     * Class under test for:
     *     void installUI(JComponent)
     *     void uninstallUI(JComponent)
     */
    public void testInstallUninstallUI() {
        ui.installUI(rootPane);
        // check install listeners
        assertTrue(isListenerInstalled(rootPane, ui));
        // check install keyboard actions
        int inputMapType = isHarmony() ? JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
                : JComponent.WHEN_IN_FOCUSED_WINDOW;
        assertTrue("inputMap installed",
                SwingUtilities.getUIInputMap(rootPane, inputMapType) != null);
        int inputMapLength = SwingUtilities.getUIInputMap(rootPane, inputMapType).size();
        assertTrue(SwingUtilities.getUIActionMap(rootPane) != null);
        int actionMapLength = SwingUtilities.getUIActionMap(rootPane).size();
        ui.uninstallUI(rootPane);
        // check uninstall keyboard actions
        InputMap inputMap = SwingUtilities.getUIInputMap(rootPane, inputMapType);
        if (inputMap != null) {
            assertTrue("keys were uninstalled", inputMap.size() < inputMapLength);
        }
        ActionMap actionMap = SwingUtilities.getUIActionMap(rootPane);
        if (actionMap != null) {
            assertTrue("actions were uninstalled", actionMap.size() < actionMapLength);
        }
        // check uninstall listeners
        assertFalse(isListenerInstalled(rootPane, ui));
    }

    /*
     * Class under test for void propertyChange()
     */
    public void testPropertyChange() {
        JFrame frame = new JFrame();
        rootPane = frame.getRootPane();
        rootPane.setUI(ui);
        int inputMapType = isHarmony() ? JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
                : JComponent.WHEN_IN_FOCUSED_WINDOW;
        InputMap inputMap = SwingUtilities.getUIInputMap(rootPane, inputMapType);
        assertTrue("inputMap != null", inputMap != null);
        Object[] keys = SwingUtilities.getUIInputMap(rootPane, inputMapType).keys();
        int keysLength = (keys == null) ? 0 : keys.length;
        // defaultButton is null, keysLength may be != 0 in 1.5
        //assertTrue("", keys == null || keys.length == 0);
        rootPane.setDefaultButton(new JButton());
        keys = SwingUtilities.getUIInputMap(rootPane, inputMapType).keys();
        // defaultButton != null
        assertTrue("keys were added", keys.length > keysLength);
        rootPane.setDefaultButton(null);
        keys = SwingUtilities.getUIInputMap(rootPane, inputMapType).keys();
        // defaultButton is null
        assertTrue("keys were removed", keysLength == 0 && keys == null
                || keys.length == keysLength);
    }

    /*
     * Class under test for ComponentUI createUI(JComponent)
     */
    public void testCreateUI() {
        ComponentUI ui = BasicRootPaneUI.createUI(rootPane);
        assertTrue(ui != null);
        assertTrue(ui instanceof BasicRootPaneUI);
        ComponentUI ui2 = BasicRootPaneUI.createUI(rootPane);
        assertTrue("stateless", ui == ui2);
    }
}
