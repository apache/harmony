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
package javax.swing;

import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class DefaultDesktopManagerTest extends SwingTestCase {
    private DefaultDesktopManager manager;

    private JDesktopPane desktop;

    private JInternalFrame frame;

    private JFrame rootFrame;

    public DefaultDesktopManagerTest(final String name) {
        super(name);
    }

    /*
     * Creates and shows rootFrame. This method is used when JInternalFrame
     * need to be selected (isSelected() == true) for testing purposes.
     */
    protected void createAndShowRootFrame() {
        frame.setSize(70, 100);
        rootFrame = new JFrame();
        rootFrame.setContentPane(desktop);
        //rootFrame.getContentPane().add(frame);
        rootFrame.setSize(100, 200);
        frame.setVisible(true);
        rootFrame.setVisible(true);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        desktop = new JDesktopPane();
        manager = new DefaultDesktopManager();
        desktop.setDesktopManager(manager);
        frame = new JInternalFrame();
        desktop.add(frame);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (rootFrame != null) {
            rootFrame.dispose();
            rootFrame = null;
        }
    }

    /*
     * Class under test for void DefaultDesktopManager()
     */
    public void testDefaultDesktopManager() {
        // nothing to test
    }

    /*
     * Class under test for
     *     void setIcon(JInternalFrame, Boolean)
     *     boolean wasIcon(JInternalFrame)
     */
    public void testSetWasIcon() {
        desktop.setDesktopManager(manager);
        assertFalse("wasIcon is false by default", manager.wasIcon(frame));
        // test set to true
        manager.setWasIcon(frame, Boolean.TRUE);
        assertTrue("wasIcon is set to true", manager.wasIcon(frame));
        // test set to false
        manager.setWasIcon(frame, Boolean.FALSE);
        assertFalse("wasIcon is set to false", manager.wasIcon(frame));
    }

    /*
     * Class under test for
     *     void setPreviousBounds(JInternalFrame, Rectangle)
     *     Rectangle getPreviousBounds(JInternalFrame)
     */
    public void testSetGetPreviousBounds() {
        Rectangle bounds = new Rectangle(1, 1, 12, 13);
        desktop.setDesktopManager(manager);
        manager.setPreviousBounds(frame, bounds);
        assertTrue("bounds are set", manager.getPreviousBounds(frame) == bounds);
        assertTrue("bounds are set as normal bounds", frame.getNormalBounds() == bounds);
    }

    /*
     * Class under test for void getBoundsForIconOf(JInternalFrame)
     */
    public void testGetBoundsForIconOf() {
        final int totalWidth = 330;
        final int totalHeight = 65;
        desktop.setDesktopManager(manager);
        desktop.setSize(totalWidth, totalHeight);
        JInternalFrame frame2 = new JInternalFrame("frame 2");
        desktop.add(frame2);
        JInternalFrame frame3 = new JInternalFrame("frame 3");
        desktop.add(frame3);
        desktop.add(new JPanel());
        int width = frame.getDesktopIcon().getWidth();
        int height = frame.getDesktopIcon().getHeight();
        Rectangle bounds = manager.getBoundsForIconOf(frame);
        assertEquals("x", bounds.x, 0);
        assertEquals("y", bounds.y, totalHeight - height);
        assertEquals("width", bounds.width, width);
        assertEquals("height", bounds.height, height);
        width = frame2.getDesktopIcon().getWidth();
        height = frame2.getDesktopIcon().getHeight();
        manager.iconifyFrame(frame);
        manager.iconifyFrame(frame);
        bounds = manager.getBoundsForIconOf(frame2);
        assertEquals("x", bounds.x, width);
        assertEquals("y", bounds.y, totalHeight - height);
        assertEquals("width", bounds.width, width);
        assertEquals("height", bounds.height, height);
        width = frame3.getDesktopIcon().getWidth();
        height = frame3.getDesktopIcon().getHeight();
        manager.iconifyFrame(frame2);
        bounds = manager.getBoundsForIconOf(frame3);
        assertEquals("x", bounds.x, 0);
        assertEquals("y", bounds.y, totalHeight - 2 * height);
        assertEquals("width", bounds.width, width);
        assertEquals("height", bounds.height, height);
        JInternalFrame frame4 = new JInternalFrame();
        width = frame4.getDesktopIcon().getWidth();
        height = frame4.getDesktopIcon().getHeight();
        bounds = manager.getBoundsForIconOf(frame4);
        assertEquals("x", bounds.x, 0);
        assertEquals("y", bounds.y, 0);
        assertEquals("width", bounds.width, width);
        assertEquals("height", bounds.height, height);
    }

    /*
     * Class under test for void removeIconFor(JInternalFrame)
     */
    public void testRemoveIconFor() {
        desktop.setDesktopManager(manager);
        // test when f is not iconified
        manager.removeIconFor(frame);
        // test when f is iconified
        manager.iconifyFrame(frame);
        manager.removeIconFor(frame);
        assertFalse("icon is removed", desktop.isAncestorOf(frame));
        // test when the internal frame is not inside any desktop pane
        manager.removeIconFor(new JInternalFrame());
    }

    /*
     * Class under test for void openFrame(JInternalFrame)
     */
    public void testOpenFrame() {
        JInternalFrame frame2 = new JInternalFrame();
        // test openFrame for the internal frame outside the container
        manager.openFrame(frame2);
        // test ordinary openFrame
        desktop.add(frame2.getDesktopIcon());
        manager.openFrame(frame2);
        assertFalse("desktopIcon is removed", desktop.isAncestorOf(frame2.getDesktopIcon()));
        assertTrue("frame is added", desktop.isAncestorOf(frame2));
        // test openFrame for the frame that is already opened
        manager.openFrame(frame2);
    }

    /*
     * Class under test for void minimizeFrame(JInternalFrame)
     */
    public void testMinimizeFrame() {
        desktop.setSize(100, 200);
        Rectangle bounds = new Rectangle(1, 1, 50, 60);
        frame.setBounds(bounds);
        manager.maximizeFrame(frame);
        // test ordinary minimize
        manager.minimizeFrame(frame);
        assertTrue("the bounds are restored", frame.getBounds().equals(bounds));
        assertFalse("the copy of bounds is used", frame.getBounds() == bounds);
        // test minimize the second time
        manager.minimizeFrame(frame);
        assertTrue("the bounds are restored", frame.getBounds().equals(bounds));
        // test minimize some separate frame
        manager.minimizeFrame(new JInternalFrame());
    }

    /*
     * Class under test for maximizeFrame(JInternalFrame)
     */
    public void testMaximizeFrame() {
        desktop.setSize(100, 200);
        Rectangle bounds = new Rectangle(1, 1, 50, 60);
        frame.setBounds(bounds);
        // test simple maximize
        manager.maximizeFrame(frame);
        assertTrue("x ok", frame.getX() == 0);
        assertTrue("y ok", frame.getY() == 0);
        assertTrue("width ok", frame.getWidth() == desktop.getWidth());
        assertTrue("height ok", frame.getHeight() == desktop.getHeight());
        assertTrue("normal bounds are saved", frame.getNormalBounds().equals(bounds));
        assertFalse("bounds are save in a copy", frame.getNormalBounds() == bounds);
        // test maximize for the internal frame that is not inside of any
        // desktop pane - NullPointernException
        //manager.maximizeFrame(new JInternalFrame());
    }

    /*
     * Class under test for
     *     void iconifyFrame(JInternalFrame)
     *     void deiconifyFrame(JInternalFrame)
     */
    public void testIconifyDeiconifyFrame() {
        desktop.setDesktopManager(manager);
        frame.setLayer(1);
        createAndShowRootFrame();
        // test correct iconify
        //try {
        //    SwingUtilities.invokeAndWait(new Runnable() {
        //        public void run() {
        manager.activateFrame(frame);
        manager.iconifyFrame(frame);
        //        }
        //    });
        //} catch (Exception e) {
        //    System.out.println(e.getCause());
        //    assertFalse("exception", true);
        //}
        assertFalse("frame is removed", desktop.isAncestorOf(frame));
        assertTrue("icon is added", desktop.isAncestorOf(frame.getDesktopIcon()));
        assertTrue("wasIcon is true", manager.wasIcon(frame));
        assertTrue("layer is set", JLayeredPane.getLayer(frame.getDesktopIcon()) == frame
                .getLayer());
        //assertTrue("isIcon is true", frame.isIcon());
        // test iconify for the frame that is already iconified
        manager.iconifyFrame(frame);
        // call with null argument causes to NullPointerException
        // test correct deiconify
        //try {
        //    SwingUtilities.invokeAndWait(new Runnable() {
        //        public void run() {
        manager.deiconifyFrame(frame);
        //        }
        //    });
        //} catch (Exception e) {
        //    assertFalse("exception", true);
        //}
        assertTrue("frame is added", desktop.isAncestorOf(frame));
        assertFalse("icon is removed", desktop.isAncestorOf(frame.getDesktopIcon()));
        assertTrue("wasIcon is true", manager.wasIcon(frame));
        // test deiconify for the deiconified frame
        manager.deiconifyFrame(frame);
        // test iconify for the internal frame without desktop pane
        manager.iconifyFrame(new JInternalFrame());
    }

    /*
     * Class under test for void deactivateFrame(JInternalFrame)
     */
    public void testDeactivateFrame() {
        manager.activateFrame(frame);
        assertTrue("activated", desktop.getSelectedFrame() == frame);
        // test ordinary deactivate
        manager.deactivateFrame(frame);
        assertNull("deactivated", desktop.getSelectedFrame());
        // test deactivate of the internal frame without desktop pane
        manager.deactivateFrame(new JInternalFrame());
    }

    /*
     * Class under test for void closeFrame(JInternalFrame)
     */
    public void testCloseFrame() {
        // test ordinary close
        manager.closeFrame(frame);
        assertFalse("frame is removed", desktop.isAncestorOf(frame));
        assertFalse("desktopIcon is removed", desktop.isAncestorOf(frame.getDesktopIcon()));
        // test close of the iconified internal frame
        desktop.add(frame);
        manager.iconifyFrame(frame);
        manager.closeFrame(frame);
        assertFalse("frame is removed", desktop.isAncestorOf(frame));
        assertFalse("desktopIcon is removed", desktop.isAncestorOf(frame.getDesktopIcon()));
        // test close of the closed internal frame
        manager.closeFrame(frame);
    }

    /*
     * Class under test for void activateFrame(JInternalFrame)
     */
    public void testActivateFrame() {
        JInternalFrame frame2 = new JInternalFrame("frame2");
        frame2.setVisible(true);
        desktop.add(frame2);
        JInternalFrame frame3 = new JInternalFrame("frame3");
        frame3.setVisible(true);
        frame3.setLayer(1);
        desktop.add(frame3);
        createAndShowRootFrame();
        try {
            frame3.setSelected(true);
        } catch (PropertyVetoException e) {
            assertFalse("exception", true);
        }
        assertTrue("frame3 is selected", frame3.isSelected());
        // test activate
        manager.activateFrame(frame);
        assertTrue("moved to the front", desktop.getIndexOf(frame) == 1);
        assertFalse("frame3 is not selected", frame3.isSelected());
        assertTrue("", desktop.getSelectedFrame() == frame);
        // test activate of already activated frame
        manager.activateFrame(frame);
        // test activate some separate internal frame
        manager.activateFrame(new JInternalFrame());
    }

    /*
     * Class under test for
     *     void setBoundsForFrame(JInternalFrame, int, int, int, int)
     */
    public void testSetBoundsForFrame() {
        Rectangle bounds = new Rectangle(1, 2, 50, 60);
        // test ordinary setBoundsForFrame
        manager.setBoundsForFrame(frame, bounds.x, bounds.y, bounds.width, bounds.height);
        assertTrue("the bounds are set", frame.getBounds().equals(bounds));
        // test setBoundsForFrame for a separate frame
        manager.setBoundsForFrame(new JInternalFrame(), bounds.x, bounds.y, bounds.width,
                bounds.height);
        // test setBoundsForFrame for some component
        manager
                .setBoundsForFrame(new JPanel(), bounds.x, bounds.y, bounds.width,
                        bounds.height);
        // Note: could test that repaint was called
    }

    /*
     * Class under test for void resizeFrame(JComponent, int, int, int, int)
     */
    public void testResizeFrame() {
        Rectangle bounds = new Rectangle(1, 2, 50, 60);
        createAndShowRootFrame();
        // OUTLINE_DRAG_MODE, the frame's bounds are not changed
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        manager.beginResizingFrame(frame, 0);
        manager.resizeFrame(frame, bounds.x, bounds.y, bounds.width, bounds.height);
        assertFalse("the bounds aren't set", frame.getBounds().equals(bounds));
        // LIVE_DRAG_MODE, the frame bounds are changed
        desktop.setDragMode(JDesktopPane.LIVE_DRAG_MODE);
        manager.beginResizingFrame(frame, 0);
        manager.resizeFrame(frame, bounds.x, bounds.y, bounds.width, bounds.height);
        assertTrue("the bounds are set", frame.getBounds().equals(bounds));
    }

    /*
     * Class under test for void dragFrame(JComponent, int, int)
     */
    public void testDragFrame() {
        Point p = new Point(3, 5);
        createAndShowRootFrame();
        // OUTLINE_DRAG_MODE, the frame's location is not changed
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        manager.beginDraggingFrame(frame);
        manager.dragFrame(frame, p.x, p.y);
        assertFalse("the location isn't changed", frame.getLocation().equals(p));
        // LIVE_DRAG_MODE, the frame's location is changed
        desktop.setDragMode(JDesktopPane.LIVE_DRAG_MODE);
        manager.beginDraggingFrame(frame);
        manager.dragFrame(frame, p.x, p.y);
        assertTrue("the location is changed", frame.getLocation().equals(p));
    }

    /*
     * Class under test for void beginResizingFrame(JInternalFrame)
     */
    public void testBeginResizingFrame() {
        // we can only test that there is no crash
        desktop.setDragMode(JDesktopPane.LIVE_DRAG_MODE);
        // the normal case
        manager.beginResizingFrame(frame, 0);
        // the frame without a parent
        manager.beginResizingFrame(new JInternalFrame(), 0);
        // the frame without a JDesktopPane parent
        JPanel parent = new JPanel();
        parent.add(frame);
        manager.beginResizingFrame(frame, 0);
    }

    /*
     * Class under test for void beginDraggingFrame(JComponent)
     */
    public void testBeginDraggingFrame() {
        desktop.setDragMode(JDesktopPane.LIVE_DRAG_MODE);
        // the normal case
        manager.beginDraggingFrame(frame);
        // the frame without a parent
        //manager.beginDraggingFrame(new JInternalFrame());
        // the frame without a JDesktopPane parent
        JPanel parent = new JPanel();
        parent.add(frame);
        manager.beginDraggingFrame(frame);
    }

    /*
     * Class under test for void endResizingFrame(JComponent)
     */
    public void testEndResizingFrame() {
        Rectangle bounds = new Rectangle(1, 2, 50, 60);
        createAndShowRootFrame();
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        manager.beginResizingFrame(frame, 0);
        manager.resizeFrame(frame, bounds.x, bounds.y, bounds.width, bounds.height);
        assertFalse("the bounds aren't changed", frame.getBounds().equals(bounds));
        manager.endResizingFrame(frame);
        assertTrue("the bounds are changed", frame.getBounds().equals(bounds));
    }

    /*
     * Class under test for void endDraggingFrame(JComponent)
     */
    public void testEndDraggingFrame() {
        Point p = new Point(3, 5);
        createAndShowRootFrame();
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        manager.beginDraggingFrame(frame);
        manager.dragFrame(frame, p.x, p.y);
        assertFalse("the location isn't changed", frame.getLocation().equals(p));
        manager.endDraggingFrame(frame);
        assertTrue("the location is changed", frame.getLocation().equals(p));
    }

    /*
     * Class under test for serialization.
     */
    public void testSerialize() throws Exception {
        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(fo);
        so.writeObject(manager);
        // so.flush();
        so.close();
        // reading
        DefaultDesktopManager manager2 = null;
        InputStream fi = new ByteArrayInputStream(fo.toByteArray());
        ObjectInputStream si = new ObjectInputStream(fi);
        manager2 = (DefaultDesktopManager) si.readObject();
        assertNotNull(manager2);
        si.close();
    }
}
