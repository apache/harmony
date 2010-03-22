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
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.OverlayLayout;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.PopupMenuUITest;

public class BasicPopupMenuUITest extends PopupMenuUITest {
    protected BasicPopupMenuUI basicPopupUI;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        basicPopupUI = new BasicPopupMenuUI();
        popupUI = basicPopupUI;
    }

    @Override
    protected void tearDown() throws Exception {
        basicPopupUI = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicPopupMenuUI.createUI(JComponent)'
     */
    public void testCreateUI() {
        JPanel panel = new JPanel();
        ComponentUI ui1 = BasicPopupMenuUI.createUI(null);
        BasicPopupMenuUI ui2 = (BasicPopupMenuUI) BasicPopupMenuUI.createUI(panel);
        assertTrue(ui1 instanceof BasicPopupMenuUI);
        assertNotSame(ui1, ui2);
    }

    /*
     * Test method for 'javax.swing.plaf.PopupMenuUI.isPopupTrigger(MouseEvent)'
     */
    @Override
    public void testIsPopupTrigger() {
        Component source1 = new JPanel();
        JPopupMenu source2 = new JPopupMenu();
        MouseEvent event1 = new MouseEvent(source1, MouseEvent.MOUSE_ENTERED, EventQueue
                .getMostRecentEventTime(), 0, 5, 5, 0, false);
        MouseEvent event2 = new MouseEvent(source1, MouseEvent.MOUSE_WHEEL, EventQueue
                .getMostRecentEventTime(), 0, 5, 5, 0, true);
        MouseEvent event3 = new MouseEvent(source1, MouseEvent.MOUSE_PRESSED, EventQueue
                .getMostRecentEventTime(), 0, 5, 5, 0, true);
        MouseEvent event4 = new MouseEvent(source2, MouseEvent.MOUSE_RELEASED, EventQueue
                .getMostRecentEventTime(), InputEvent.BUTTON1_DOWN_MASK, 1, 1, 1, true);
        source2.setUI(popupUI);
        assertFalse(popupUI.isPopupTrigger(event1));
        assertFalse(popupUI.isPopupTrigger(event2));
        assertFalse(popupUI.isPopupTrigger(event3));
        assertFalse(popupUI.isPopupTrigger(event4));

        // regression for HARMONY-2512
        try {    
            BasicPopupMenuUI m = new BasicPopupMenuUI();

            m.isPopupTrigger(null);
            fail("NPE should be thrown"); 
        } catch (NullPointerException npe) {                
            // PASSED            
        }
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicPopupMenuUI.installDefaults()'
     */
    public void testInstallUninstallUI() {
        JPopupMenu menu = new JPopupMenu();
        menu.setUI(basicPopupUI);
        LayoutManager oldManager = menu.getLayout();
        basicPopupUI.uninstallUI(menu);
        basicPopupUI.installUI(menu);
        assertNotNull(menu.getLayout());
        assertNotSame(oldManager, menu.getLayout());
        basicPopupUI.uninstallUI(menu);
        oldManager = new OverlayLayout(menu);
        menu.setLayout(oldManager);
        basicPopupUI.installUI(menu);
        assertEquals(oldManager, menu.getLayout());
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicPopupMenuUI.installDefaults()'
     */
    public void testInstallUninstallDefaults() {
        JPopupMenu menu = new JPopupMenu();
        UIManager.getDefaults().put("PopupMenu.background", new ColorUIResource(Color.red));
        UIManager.getDefaults().put("PopupMenu.foreground", new ColorUIResource(Color.yellow));
        Font font = new FontUIResource(menu.getFont().deriveFont(100f));
        UIManager.getDefaults().put("PopupMenu.font", font);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.getDefaults().put("PopupMenu.border", border);
        menu.setUI(basicPopupUI);
        basicPopupUI.installDefaults();
        assertEquals(Color.red, menu.getBackground());
        assertEquals(Color.yellow, menu.getForeground());
        assertEquals(font, menu.getFont());
        assertEquals(border, menu.getBorder());
        assertTrue(menu.isOpaque());
        basicPopupUI.uninstallDefaults();
        assertNull(menu.getBackground());
        assertNull(menu.getForeground());
        assertNull(menu.getFont());
        assertNull(menu.getBorder());
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicPopupMenuUI.installKeyboardActions()'
     */
    public void testInstallUninstallKeyboardActions() {
        if (!isHarmony()) {
            return;
        }
        JPopupMenu menu = new JPopupMenu();
        menu.setUI(null);
        basicPopupUI.popupMenu = menu;
        menu.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        basicPopupUI.installKeyboardActions();
        assertEquals(0, menu.getInputMap().keys().length);
        assertNotNull(menu.getInputMap().getParent());
        assertEquals(11, menu.getInputMap().getParent().keys().length);
        assertNull(menu.getInputMap().getParent().getParent());
        basicPopupUI.uninstallKeyboardActions();
        assertEquals(0, menu.getInputMap().allKeys().length);
        menu.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        basicPopupUI.installKeyboardActions();
        assertEquals(0, menu.getInputMap().keys().length);
        assertNotNull(menu.getInputMap().getParent());
        assertEquals(4, menu.getInputMap().getParent().keys().length);
        assertNotNull(menu.getInputMap().getParent().getParent());
        assertEquals(11, menu.getInputMap().getParent().getParent().keys().length);
        assertNull(menu.getInputMap().getParent().getParent().getParent());
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicPopupMenuUI.installListeners()'
     */
    public void testInstallUninstallListeners() {
        JPopupMenu menu = new JPopupMenu();
        menu.setUI(null);
        basicPopupUI.popupMenu = menu;
        basicPopupUI.installListeners();
        if (!isHarmony()) {
            assertEquals(1, menu.getPopupMenuListeners().length);
        }
        basicPopupUI.uninstallListeners();
        if (!isHarmony()) {
            assertEquals(0, menu.getPopupMenuListeners().length);
        }
    }
    
    /**
     * Regression test for HARMONY-2654 
     * */
    public void testInstallKeyboardActions() throws NullPointerException { 
        BasicPopupMenuUIForTest localBasicPopupMenuUI = new BasicPopupMenuUIForTest(); 
        localBasicPopupMenuUI.installKeyboardActions(); 
    } 


    class BasicPopupMenuUIForTest extends BasicPopupMenuUI { 
        public void installKeyboardActions () { 
            super.installKeyboardActions(); 
        } 
    }
}
