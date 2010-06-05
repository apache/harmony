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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import javax.swing.Action;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.plaf.metal.MetalInternalFrameTitlePane;

public class BasicInternalFrameTitlePaneTest extends SwingTestCase {
    private BasicInternalFrameTitlePane pane;

    private JInternalFrame frame;

    private JDesktopPane desktop;

    private JFrame rootFrame;

    public BasicInternalFrameTitlePaneTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JInternalFrame();
        pane = new BasicInternalFrameTitlePane(frame);
        desktop = new JDesktopPane();
        desktop.add(frame);
        frame.addNotify(); // leads to enabling of move/size actions
        rootFrame = null;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (rootFrame != null) {
            rootFrame.dispose();
        }
    }

    private void checkButtonIcons() {
        assertTrue("closeButton ok", pane.closeButton.getIcon() == pane.closeIcon);
        if (frame.isIcon()) {
            assertTrue("iconButton ok", pane.iconButton.getIcon() == pane.minIcon);
        } else {
            assertTrue("iconButton ok", pane.iconButton.getIcon() == pane.iconIcon);
        }
        if (frame.isMaximum() && !frame.isIcon()) {
            assertTrue("maxButton ok", pane.maxButton.getIcon() == pane.minIcon);
        } else {
            assertTrue("maxButton ok", pane.maxButton.getIcon() == pane.maxIcon);
        }
        assertTrue("closeButton tooltip ok", pane.closeButton.getToolTipText() != null);
        assertTrue("iconButton tooltip ok", pane.iconButton.getToolTipText() != null);
        assertTrue("maxButton tooltip ok", pane.maxButton.getToolTipText() != null);
    }

    private void checkEnabledActions() {
        assertEquals("iconifyAction", !frame.isIcon() && frame.isIconifiable(),
                pane.iconifyAction.isEnabled());
        assertEquals("closeAction", frame.isClosable(), pane.closeAction.isEnabled());
        assertEquals("maximizeAction", frame.isMaximizable()
                && (!frame.isMaximum() || frame.isIcon() && frame.isIconifiable()),
                pane.maximizeAction.isEnabled());
        assertEquals("moveAction", isHarmony() && frame.getDesktopPane() != null
                && !frame.isMaximum(), pane.moveAction.isEnabled());
        assertEquals("sizeAction",
                !frame.isMaximum() && frame.isResizable() && !frame.isIcon(), pane.sizeAction
                        .isEnabled());
        assertEquals("restoreAction", frame.isMaximum() && frame.isMaximizable()
                || frame.isIcon() && frame.isIconifiable(), pane.restoreAction.isEnabled());
    }

    private boolean belongs(final Object o, final Object[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == o) {
                return true;
            }
        }
        return false;
    }

    public void testAddSubComponents() {
        frame = new JInternalFrame("", true, true, true, true);
        pane = new BasicInternalFrameTitlePane(frame);
        assertEquals(4, pane.getComponentCount());
        frame = new JInternalFrame("", true, true, true, false);
        pane = new BasicInternalFrameTitlePane(frame);
        if (isHarmony()) {
            assertEquals(3, pane.getComponentCount());
        }
    }

    public void testCreateActions() {
        pane.createActions();
        // test created actions
        assertTrue("closeAction != null", pane.closeAction != null);
        assertTrue("closeAction instanceof ok",
                pane.closeAction instanceof BasicInternalFrameTitlePane.CloseAction);
        assertTrue("iconifyAction != null", pane.iconifyAction != null);
        assertTrue("iconifyAction instanceof ok",
                pane.iconifyAction instanceof BasicInternalFrameTitlePane.IconifyAction);
        assertTrue("maximizeAction != null", pane.maximizeAction != null);
        assertTrue("maximizeAction instanceof ok",
                pane.maximizeAction instanceof BasicInternalFrameTitlePane.MaximizeAction);
        assertTrue("moveAction != null", pane.moveAction != null);
        assertTrue("moveAction instanceof ok",
                pane.moveAction instanceof BasicInternalFrameTitlePane.MoveAction);
        assertTrue("restoreAction != null", pane.restoreAction != null);
        assertTrue("restoreAction instanceof ok",
                pane.restoreAction instanceof BasicInternalFrameTitlePane.RestoreAction);
        assertTrue("sizeAction != null", pane.sizeAction != null);
        assertTrue("sizeAction instanceof ok",
                pane.sizeAction instanceof BasicInternalFrameTitlePane.SizeAction);
    }

    public void testCreateButtons() {
        pane.createButtons();
        // test created buttons
        assertTrue("maxButton != null", pane.maxButton != null);
        assertTrue("maxButton tooltip", pane.maxButton.getToolTipText() != null);
        assertTrue("closeButton != null", pane.closeButton != null);
        assertTrue("closeButton tooltip", pane.closeButton.getToolTipText() != null);
        assertTrue("iconButton != null", pane.iconButton != null);
        assertTrue("iconButton tooltip", pane.iconButton.getToolTipText() != null);
    }

    public void testEnableActions() {
        // iconifyAction test
        frame.setIconifiable(false);
        checkEnabledActions();
        frame.setIconifiable(true);
        checkEnabledActions();
        // closeAction test
        frame.setClosable(false);
        checkEnabledActions();
        frame.setClosable(true);
        checkEnabledActions();
        // maximizeAction test
        frame.setMaximizable(false);
        checkEnabledActions();
        frame.setMaximizable(true);
        checkEnabledActions();
        // restoreAction test
        checkEnabledActions(); // normal bounds - disabled
        frame.setMaximizable(true);
        try {
            frame.setMaximum(true);
        } catch (PropertyVetoException e) {
            assertTrue("exception", false);
        }
        checkEnabledActions(); // enabled
        frame.setMaximizable(false);
        if (isHarmony()) {
            checkEnabledActions(); // disabled
        }
        frame.setMaximizable(true);
        frame.setIconifiable(true);
        try {
            frame.setIcon(true);
        } catch (PropertyVetoException e) {
            assertTrue("exception", false);
        }
        checkEnabledActions(); // enabled
        frame.setIconifiable(false);
        if (isHarmony()) {
            checkEnabledActions(); // disabled
        }
    }

    public void testInstallDefaults() {
        assertSame(UIManager.getIcon("InternalFrame.closeIcon"), pane.closeIcon);
        assertSame(UIManager.getIcon("InternalFrame.maximizeIcon"), pane.maxIcon);
        assertSame(UIManager.getIcon("InternalFrame.minimizeIcon"), pane.minIcon);
        assertSame(UIManager.getIcon("InternalFrame.iconifyIcon"), pane.iconIcon);
        assertSame(UIManager.getColor("InternalFrame.activeTitleBackground"),
                pane.selectedTitleColor);
        assertSame(UIManager.getColor("InternalFrame.activeTitleForeground"),
                pane.selectedTextColor);
        assertSame(UIManager.getColor("InternalFrame.inactiveTitleBackground"),
                pane.notSelectedTitleColor);
        assertSame(UIManager.getColor("InternalFrame.inactiveTitleForeground"),
                pane.notSelectedTextColor);
        assertSame(UIManager.getFont("InternalFrame.titleFont"), pane.getFont());
    }

    public void testInstallUninstallListeners() {
        pane.uninstallListeners();
        assertFalse("listener was uninstalled", belongs(pane.propertyChangeListener, frame
                .getPropertyChangeListeners()));
        pane.installListeners();
        assertTrue("listener != null", pane.propertyChangeListener != null);
        assertTrue("listener was installed", belongs(pane.propertyChangeListener, frame
                .getPropertyChangeListeners()));
        pane.uninstallListeners();
        assertFalse("listener was uninstalled", belongs(pane.propertyChangeListener, frame
                .getPropertyChangeListeners()));
    }

    public void testInstallTitlePane() {
        pane.installTitlePane();
        assertSame(UIManager.getIcon("InternalFrame.closeIcon"), pane.closeIcon);
        assertTrue("listener != null", pane.propertyChangeListener != null);
        assertTrue("listener was installed", belongs(pane.propertyChangeListener, frame
                .getPropertyChangeListeners()));
        assertTrue("closeAction != null", pane.closeAction != null);
        assertNotNull(pane.getLayout());
        assertTrue("maxButton != null", pane.maxButton != null);
        assertTrue("windowMenu != null", pane.windowMenu != null);
        assertTrue(pane.getComponentCount() > 1);
        checkEnabledActions();
    }

    public void testSetButtonIcons() {
        checkButtonIcons();
        // test icons in Maximum state
        frame.setMaximizable(true);
        try {
            frame.setMaximum(true);
        } catch (PropertyVetoException e) {
            assertTrue("exception", false);
        }
        checkButtonIcons();
        // test icons in Icon state
        frame.setIconifiable(true);
        try {
            frame.setIcon(true);
        } catch (PropertyVetoException e) {
            assertTrue("exception", false);
        }
        checkButtonIcons();
    }

    protected void createAndShowRootFrame() {
        rootFrame = new JFrame();
        rootFrame.setSize(90, 90);
        rootFrame.setContentPane(desktop);
        frame.setSize(80, 50);
        frame.setVisible(true);
        desktop.add(pane, BorderLayout.NORTH);
        rootFrame.setVisible(true);
    }

    public void testShowSystemMenu() {
        createAndShowRootFrame();
        pane.showSystemMenu();
        assertTrue("", pane.windowMenu.isPopupMenuVisible());
        pane.windowMenu = null;
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                pane.showSystemMenu();
            }
        });
    }

    public void testUninstallDefaults() {
        // nothing to test
    }

    public void testBasicInternalFrameTitlePane() {
        pane = new BasicInternalFrameTitlePane(frame);
        assertTrue("frame is set", pane.frame == frame);
        assertTrue("layout", pane.getLayout() != null);
    }

    public void testGetTitle() {
        createAndShowRootFrame();
        final String title = "Document #1";
        Font font = new Font("Fixed", Font.PLAIN, 10);
        FontMetrics fm = getFontMetrics(font);
        assertEquals(title, pane.getTitle(title, fm, 110));
        assertEquals("Documen...", pane.getTitle(title, fm, 109));
        assertEquals("Do...", pane.getTitle(title, fm, 50));
        assertEquals("...", pane.getTitle(title, fm, 4));
    }

    public void testCreateSystemMenuBar() {
        JMenuBar menuBar = pane.createSystemMenuBar();
        assertEquals(0, menuBar.getMenuCount());
    }

    public void testAddSystemMenuItems() {
        JMenu menu = new JMenu();
        pane.addSystemMenuItems(menu);
        assertEquals(7, menu.getItemCount());
    }

    public void testCreateSystemMenu() {
        JMenu menu = pane.createSystemMenu();
        assertEquals(0, menu.getItemCount());
    }

    public void testAssembleSystemMenu() {
        pane.assembleSystemMenu();
        assertTrue("windowMenu != null", pane.windowMenu != null);
        assertEquals("7 items", 7, pane.windowMenu.getItemCount());
        JMenuBar menuBar = pane.menuBar;
        assertTrue("menuBar contains windowMenu", menuBar.getMenu(0) == pane.windowMenu);
    }

    public void testPostClosingEvent() {
        class MyInternalFrameAdapter extends InternalFrameAdapter {
            boolean ok = false;

            @Override
            public void internalFrameClosing(final InternalFrameEvent e) {
                ok = true;
            }
        }
        MyInternalFrameAdapter listener = new MyInternalFrameAdapter();
        frame.addInternalFrameListener(listener);
        pane.postClosingEvent(frame);
        if (isHarmony()) {
            assertTrue("event ok", listener.ok);
        }
    }

    public void testCreatePropertyChangeListener() {
        PropertyChangeListener listener = pane.createPropertyChangeListener();
        assertTrue("!= null", listener != null);
        if (isHarmony()) {
            assertTrue("instanceof TitlePaneLayout",
                    listener instanceof BasicInternalFrameTitlePane.PropertyChangeHandler);
        }
    }

    public void testCreateLayout() {
        LayoutManager layout = pane.createLayout();
        assertTrue("!= null", layout != null);
        if (isHarmony()) {
            assertTrue("instanceof TitlePaneLayout",
                    layout instanceof BasicInternalFrameTitlePane.TitlePaneLayout);
        }
    }

    public void testCloseButton() {
        // test with isClosable == false
        pane.closeButton.doClick(0);
        assertFalse("not closed", frame.isClosed());
        // test with isClosable == true
        frame.setClosable(true);
        pane.closeButton.doClick(0);
        assertTrue("closed", frame.isClosed());
    }

    public void testIconifyButton() {
        // test with isIconifiable == false
        pane.iconButton.doClick(0);
        assertFalse("not iconified", frame.isIcon());
        // test with isIconifiable == true
        frame.setIconifiable(true);
        pane.iconButton.doClick(0);
        assertTrue("iconified", frame.isIcon());
        // test iconify again
        pane.iconButton.doClick(0);
        assertFalse("deiconified", frame.isIcon());
        // test iconify from maximized state
        frame.setMaximizable(true);
        pane.maxButton.doClick(0);
        assertTrue("maximized", frame.isMaximum());
        pane.iconButton.doClick(0);
        assertTrue("iconified", frame.isIcon());
        assertTrue("maximized", frame.isMaximum());
        // test deiconify after iconify from maximized state
        pane.iconButton.doClick(0);
        assertFalse("deiconified", frame.isIcon());
        assertTrue("maximized", frame.isMaximum());
    }

    public void testMaximizeButton() {
        // test with isMaximizable == false
        pane.maxButton.doClick(0);
        assertFalse("not maximized", frame.isMaximum());
        // test with isMaximizable == true
        frame.setMaximizable(true);
        pane.maxButton.doClick(0);
        assertTrue("maximized", frame.isMaximum());
        // test maximize again
        pane.maxButton.doClick(0);
        assertFalse("restored", frame.isMaximum());
        // test maximize from icon
        frame.setIconifiable(true);
        pane.iconButton.doClick(0);
        assertTrue("iconified", frame.isIcon());
        frame.setIconifiable(false);
        pane.maxButton.doClick(0);
        assertFalse("deiconified", frame.isIcon());
        assertTrue("maximized", frame.isMaximum());
        // test maximize again
        frame.setIconifiable(true);
        pane.iconButton.doClick(0);
        assertTrue("iconified", frame.isIcon());
        pane.maxButton.doClick(0);
        assertFalse("deiconified", frame.isIcon());
        assertTrue("maximized", frame.isMaximum());
    }

    public void testRestoreAction() {
        frame.setMaximizable(true);
        pane.maxButton.doClick(0);
        assertTrue("maximized", frame.isMaximum());
        // test with isMaximizable == false
        frame.setMaximizable(false);
        JMenuItem menuItem = pane.menuBar.getMenu(0).getItem(0);
        assertEquals("name == Restore", "Restore", menuItem.getText());
        menuItem.doClick(0);
        assertTrue("restored", frame.isMaximum());
        // test with isMaximizable == true
        frame.setMaximizable(true);
        menuItem.doClick(0);
        assertFalse("restored", frame.isMaximum());
        // test restore again
        menuItem.doClick(0);
        assertFalse("no change", frame.isMaximum());
        // iconify the frame
        frame.setIconifiable(true);
        pane.iconButton.doClick(0);
        assertTrue("iconified", frame.isIcon());
        // test restore of the iconified frame with isIconifiable == false
        frame.setIconifiable(false);
        menuItem.doClick(0);
        assertTrue("not restored", frame.isIcon());
        // test restore of the iconified frame with isIconifiable == true
        frame.setIconifiable(true);
        menuItem.doClick(0);
        assertFalse("restored", frame.isIcon());
    }

    public void testSizeAction() {
        // cannot test
    }

    public void testMoveAction() {
        // cannot test
    }

    public void testTitlePaneLayout() {
        LayoutManager layout = pane.new TitlePaneLayout();
        pane.setSize(200, 31);
        final Rectangle menuBarBounds = new Rectangle(2, 7, 16, 16);
        final Rectangle zeroBounds = new Rectangle();
        final Rectangle closeButtonBounds = new Rectangle(182, 7, 16, 16);
        final Rectangle iconButtonBounds = new Rectangle(146, 7, 16, 16);
        final Rectangle maximizeButtonBounds = new Rectangle(164, 7, 16, 16);
        // non-iconifiable, non-maximizable, non-closable
        layout.layoutContainer(null);
        assertEquals("menuBar", menuBarBounds, pane.menuBar.getBounds());
        assertEquals("iconButton", zeroBounds, pane.iconButton.getBounds());
        assertEquals("maximizeButton", zeroBounds, pane.maxButton.getBounds());
        assertEquals("closeButton", zeroBounds, pane.closeButton.getBounds());
        if (!isHarmony()) {
            return;
        }
        // iconifiable, non-maximizable, non-closable
        frame.setIconifiable(true);
        layout.layoutContainer(null);
        assertEquals("menuBar", menuBarBounds, pane.menuBar.getBounds());
        assertEquals("iconButton", closeButtonBounds, pane.iconButton.getBounds());
        assertEquals("maximizeButton", zeroBounds, pane.maxButton.getBounds());
        assertEquals("closeButton", zeroBounds, pane.closeButton.getBounds());
        // iconifiable, maximizable, closable
        frame.setMaximizable(true);
        frame.setClosable(true);
        layout.layoutContainer(null);
        assertEquals("menuBar", menuBarBounds, pane.menuBar.getBounds());
        assertEquals("iconButton", iconButtonBounds, pane.iconButton.getBounds());
        assertEquals("maximizeButton", maximizeButtonBounds, pane.maxButton.getBounds());
        assertEquals("closeButton", closeButtonBounds, pane.closeButton.getBounds());
        // minimumLayoutSize(), preferredLayoutSize() implementations
        assertTrue("", layout.minimumLayoutSize(pane) != null);
        assertTrue("", layout.preferredLayoutSize(pane) != null);
    }

    @SuppressWarnings("deprecation")
    public void testSystemMenuBar() {
        JMenuBar menuBar = pane.new SystemMenuBar();
        assertTrue("opaque", menuBar.isOpaque());
        assertFalse("isFocusTraversable", menuBar.isFocusTraversable());
        assertFalse(menuBar.isFocusable());
    }

    public void testPropertyChangeHandler() {
        // test "iconable" property change
        frame.setIconifiable(false);
        frame.setIconifiable(true);
        assertTrue("icon button added", belongs(pane.iconButton, pane.getComponents()));
        frame.setIconifiable(false);
        assertFalse("icon button removed", belongs(pane.iconButton, pane.getComponents()));
        // test "closable" property change
        frame.setClosable(false);
        frame.setClosable(true);
        assertTrue("close button added", belongs(pane.closeButton, pane.getComponents()));
        frame.setClosable(false);
        assertFalse("close button removed", belongs(pane.closeButton, pane.getComponents()));
        // test "maximizable" property change
        frame.setMaximizable(false);
        frame.setMaximizable(true);
        assertTrue("max button added", belongs(pane.maxButton, pane.getComponents()));
        frame.setMaximizable(false);
        assertFalse("max button removed", belongs(pane.maxButton, pane.getComponents()));
    }

    public void testPaintTitleBackground() {
        // Note: painting code, cannot test
    }

    
    public void testConstructor() {
        try {     
            new BasicInternalFrameTitlePane((JInternalFrame) null); 
            fail("NPE should be thrown");
        } catch (NullPointerException npe) {    
            // PASSED          
        }
    }
    public void testPaintComponent() {
        // Note: painting code, cannot test
    }
    
    /**
     * Regression test for HARMONY-2608
     * */
    public void testMoveActionKey() {
        BasicInternalFrameTitlePane.MoveAction m = pane.new MoveAction();
        assertEquals(1, m.getKeys().length);
        String key = (String)m.getKeys()[0];
        assertEquals(Action.NAME, key);
        assertEquals("Move", m.getValue(key));
    }
    
    /**
     * Regression test for HARMONY-2608
     * */
    public void testMoveActionPerformed() {
        BasicInternalFrameTitlePane.MoveAction m = pane.new MoveAction();
        try {
            m.actionPerformed(null);
        } catch ( NullPointerException e) { 
            fail("NPE shouldn't be thrown");
        }
    }
    
    /**
     * Regression test for HARMONY-2604
     * */
    public void testSizeActionPerformed() {
        String str = "test string";
        JInternalFrame jf = new JInternalFrame(str);
        MetalInternalFrameTitlePane jp = new MetalInternalFrameTitlePane(jf);
        BasicInternalFrameTitlePane.SizeAction m = jp.new SizeAction();
        try {
            m.actionPerformed(null);
        } catch (NullPointerException e) {
            fail("NPE shouldn't be thrown");
        }
    }
    
    /**
     * Regression test for HARMONY-2588
     * */
    public void testSizeActionKey() {
        String str = "test string";
        JInternalFrame jf = new JInternalFrame(str);
        MetalInternalFrameTitlePane jp = new MetalInternalFrameTitlePane(jf);
        BasicInternalFrameTitlePane.SizeAction m = jp.new SizeAction();
        assertEquals(1, m.getKeys().length);
        String key = (String)m.getKeys()[0];
        assertEquals(Action.NAME, key);
        assertEquals("Size", m.getValue(key));
   } 
}
