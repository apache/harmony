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
 * @author Pavel Dolgov
 */
package java.awt.dnd;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorMap;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.peer.ComponentPeer;
import java.util.HashMap;
import java.util.Map;
import java.util.TooManyListenersException;

import junit.framework.TestCase;

/**
 * Unit test for class java.awt.dnd.DropTarget
 */
@SuppressWarnings("serial")
public class DropTargetTest extends TestCase {
    
    static class DTL implements DropTargetListener {
        
        public boolean exited;
        public boolean dropped;
        public boolean changed;
        public boolean hovered;
        public boolean entered;

        public void dragExit(DropTargetEvent dte) {
            exited = true;
        }

        public void drop(DropTargetDropEvent dtde) {
            dropped = true;
        }

        public void dropActionChanged(DropTargetDragEvent dtde) {
            changed = true;
        }

        public void dragOver(DropTargetDragEvent dtde) {
            hovered = true;
        }

        public void dragEnter(DropTargetDragEvent dtde) {
            entered = true;
        }
    }
    
    static class FM implements FlavorMap {
        public Map<String, DataFlavor> getFlavorsForNatives(String[] natives) {
            return new HashMap<String, DataFlavor>();
        }
        public Map<DataFlavor, String> getNativesForFlavors(DataFlavor[] flavors) {
            return new HashMap<DataFlavor, String>();
        }
    }
    
    static class AddRemoveDropTarget extends DropTarget {
        public boolean added;
        public boolean removed;
        
        public AddRemoveDropTarget(Component c) {
            super(c, null);
        }
        
        @Override
        public void addNotify(ComponentPeer peer) {
            added = true;
            super.addNotify(peer);
        }
        @Override
        public void removeNotify(ComponentPeer peer) {
            removed = true;
            super.removeNotify(peer);
        }
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(DropTargetTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testDropTargetComponentintDropTargetListenerbooleanFlavorMap() {
        Component c = new Component() {};
        int ops = 1;
        DropTargetListener dtl = new DTL();
        FlavorMap fm = new FM();
        DropTarget dt = new DropTarget(c, ops, dtl, false, fm);
        assertSame(c, dt.getComponent());
        assertSame(dt, c.getDropTarget());
        assertEquals(ops, dt.getDefaultActions());
        assertFalse(dt.isActive());
        assertSame(fm, dt.getFlavorMap());
        try {
            dt.removeDropTargetListener(new DTL());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
        dt.removeDropTargetListener(dtl);
    }

    public void testDropTargetComponentDropTargetListener() {
    }

    public void testDropTargetComponentintDropTargetListenerboolean() {
    }

    public void testDropTargetComponentintDropTargetListener() {
    }

    public void testDropTarget() {
        DropTarget dt = new DropTarget();
        assertNull(dt.getComponent());
        assertEquals(DnDConstants.ACTION_COPY_OR_MOVE, dt.getDefaultActions());
        assertSame(SystemFlavorMap.getDefaultFlavorMap(), dt.getFlavorMap());
        assertTrue(dt.isActive());
    }

    public void testCreateDropTargetAutoScroller() {
        final Component comp = new Component() {};
        final Point start = new Point(10, 10);
        DropTarget dt = new DropTarget(comp, new DTL()) {
            @Override
            protected DropTargetAutoScroller createDropTargetAutoScroller(
                    Component c, Point p) {
                assertSame(comp, c);
                assertSame(start, p);
                DropTargetAutoScroller ret = 
                    super.createDropTargetAutoScroller(c, p);
                assertNotNull(ret);
                return ret;
            }
        };
        dt.initializeAutoscrolling(start);
    }

    public void testRemoveNotify() {
    }
    
    public void testAddNotify() {
        // Regression for HARMONY-2492
        new DropTarget().addNotify(null);
    }

    public void testAddRemoveNotify() {
        Frame f = new Frame("DropTargetTest");
        Component c = new Component() {};
        AddRemoveDropTarget dt = new AddRemoveDropTarget(c);
        f.add(c);
        assertFalse(dt.added);
        f.addNotify();
        assertTrue(dt.added);
        assertFalse(dt.removed);
        f.dispose();
        assertTrue(dt.removed);
    }

    public void testRemoveDropTargetListener()
            throws TooManyListenersException {
        DropTarget dt = new DropTarget();
        DropTargetListener dtl = new DTL();
        dt.removeDropTargetListener(null);
        dt.removeDropTargetListener(new DTL());
        dt.addDropTargetListener(dtl);
        dt.removeDropTargetListener(null);
        try {
            dt.removeDropTargetListener(new DTL());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
        dt.removeDropTargetListener(dtl);
        try {
            dt.addDropTargetListener(new DTL());
        } catch (TooManyListenersException e) {
            fail("Listener wasn't removed");
        }
    }

    public void testAddDropTargetListener() {
        DropTarget dt = new DropTarget();
        DropTargetListener dtl = new DTL();
        
        try {
            dt.addDropTargetListener(null);
        } catch (TooManyListenersException e) {
            fail("Null listener should be ignored");
        }
        try {
            dt.addDropTargetListener(dt);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        } catch (TooManyListenersException e) {
            fail("Adding this listener should cause IllegalArgumentException");
        }
        try {
            dt.addDropTargetListener(dtl);
        } catch (TooManyListenersException e) {
            fail("TooManyListenersException " + e.getMessage());
        }
        try {
            dt.addDropTargetListener(null);
        } catch (TooManyListenersException e) {
            fail("Null listener should be ignored");
        }
        try {
            dt.addDropTargetListener(dtl);
            fail("TooManyListenersException expected");
        } catch (TooManyListenersException e) {
        }
        try {
            dt.addDropTargetListener(new DTL());
            fail("TooManyListenersException expected");
        } catch (TooManyListenersException e) {
        }
    }

    public void testDragExit() {
        DropTarget dt = new DropTarget();
        DropTargetEvent dte = 
            new DropTargetEvent(dt.getDropTargetContext());
        dt.dragExit(dte);
        DTL dtl = new DTL();
        try {
            dt.addDropTargetListener(dtl);
        } catch (TooManyListenersException e) {
            fail("TooManyListenersException");
        }
        dt.dragExit(null);
        dt.dragExit(dte);
        assertTrue(dtl.exited);
        dtl.exited = false;
        dt.setActive(false);
        dt.dragExit(dte);
        assertFalse(dtl.exited);
        dt.removeDropTargetListener(dtl);
        dt.dragExit(dte);
        assertFalse(dtl.exited);
        dt.dragExit(null);
    }

    public void testDrop() {
        DropTarget dt = new DropTarget();
        DropTargetDropEvent dtde = 
            new DropTargetDropEvent(dt.getDropTargetContext(), new Point(0, 0),
            DnDConstants.ACTION_COPY, DnDConstants.ACTION_COPY);
        dt.drop(dtde);
        DTL dtl = new DTL();
        try {
            dt.addDropTargetListener(dtl);
        } catch (TooManyListenersException e) {
            fail("TooManyListenersException");
        }
        dt.drop(null);
        dt.drop(dtde);
        assertTrue(dtl.dropped);
        dtl.dropped = false;
        dt.setActive(false);
        dt.drop(dtde);
        assertFalse(dtl.dropped);
        dt.removeDropTargetListener(dtl);
        dt.drop(dtde);
        assertFalse(dtl.dropped);
        try {
            dt.drop(null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    public void testDropActionChanged() {
        DropTarget dt = new DropTarget();
        DropTargetDragEvent dtde = 
            new DropTargetDragEvent(dt.getDropTargetContext(), new Point(0, 0),
            DnDConstants.ACTION_COPY, DnDConstants.ACTION_COPY);
        dt.dropActionChanged(dtde);
        DTL dtl = new DTL();
        try {
            dt.addDropTargetListener(dtl);
        } catch (TooManyListenersException e) {
            fail("TooManyListenersException");
        }
        try {
            dt.dropActionChanged(null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
        dt.dropActionChanged(dtde);
        assertTrue(dtl.changed);
        dtl.changed = false;
        dt.setActive(false);
        dt.dropActionChanged(dtde);
        assertFalse(dtl.changed);
        dt.removeDropTargetListener(dtl);
        dt.dropActionChanged(dtde);
        assertFalse(dtl.changed);
        dt.dropActionChanged(null);
    }

    public void testDragOver() {
        DropTarget dt = new DropTarget();
        DropTargetDragEvent dtde = 
            new DropTargetDragEvent(dt.getDropTargetContext(), new Point(0, 0),
            DnDConstants.ACTION_COPY, DnDConstants.ACTION_COPY);
        dt.dragOver(dtde);
        DTL dtl = new DTL();
        try {
            dt.addDropTargetListener(dtl);
        } catch (TooManyListenersException e) {
            fail("TooManyListenersException");
        }
        try {
            dt.dragOver(null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
        dt.dragOver(dtde);
        assertTrue(dtl.hovered);
        dtl.hovered = false;
        dt.setActive(false);
        dt.dragOver(dtde);
        assertFalse(dtl.hovered);
        dt.removeDropTargetListener(dtl);
        dt.dragOver(dtde);
        assertFalse(dtl.hovered);
        dt.dragOver(null);
    }

    public void testDragEnter() {
        DropTarget dt = new DropTarget();
        DropTargetDragEvent dtde = 
            new DropTargetDragEvent(dt.getDropTargetContext(), new Point(0, 0),
            DnDConstants.ACTION_COPY, DnDConstants.ACTION_COPY);
        dt.dragEnter(dtde);
        DTL dtl = new DTL();
        try {
            dt.addDropTargetListener(dtl);
        } catch (TooManyListenersException e) {
            fail("TooManyListenersException");
        }
        try {
            dt.dragEnter(null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
        dt.dragEnter(dtde);
        assertTrue(dtl.entered);
        dtl.entered = false;
        dt.setActive(false);
        dt.dragEnter(dtde);
        assertFalse(dtl.entered);
        dt.removeDropTargetListener(dtl);
        dt.dragEnter(dtde);
        assertFalse(dtl.entered);
        dt.dragEnter(null);
    }

    public void testGetDropTargetContext() {
        Component c = new Component() {};
        int ops = 1;
        DropTargetListener dtl = new DTL();
        FlavorMap fm = new FM();
        DropTarget dt = new DropTarget(c, ops, dtl, false, fm);
        DropTargetContext dtc = dt.getDropTargetContext();
        assertSame(c, dtc.getComponent());
        assertSame(dt, dtc.getDropTarget());
    }

    public void testCreateDropTargetContext() {
    }

    public void testSetFlavorMap() {
        DropTarget dt = new DropTarget();
        assertSame(SystemFlavorMap.getDefaultFlavorMap(), dt.getFlavorMap());
        FlavorMap fm = new FM();
        dt.setFlavorMap(fm);
        assertSame(fm, dt.getFlavorMap());
        dt.setFlavorMap(null);
        assertSame(SystemFlavorMap.getDefaultFlavorMap(), dt.getFlavorMap());
    }

    public void testGetFlavorMap() {
        FlavorMap fm = new FM();
        DropTarget dt = new DropTarget(null, 1, null, false, fm);
        assertSame(fm, dt.getFlavorMap());
        dt = new DropTarget();
        assertSame(SystemFlavorMap.getDefaultFlavorMap(), dt.getFlavorMap());
    }

    public void testUpdateAutoscroll() {
    }

    public void testInitializeAutoscrolling() {
    }

    public void testSetComponent() {
        DropTarget dt = new DropTarget();
        Component c = new Component() {};
        assertNull(dt.getComponent());
        dt.setComponent(c);
        assertSame(c, dt.getComponent());
        assertSame(dt, c.getDropTarget());
        dt.setComponent(null);
        assertNull(dt.getComponent());
        assertNull(c.getDropTarget());
    }

    public void testGetComponent() {
        DropTarget dt = new DropTarget();
        Component c = new Component() {};
        assertNull(dt.getComponent());
        dt = new DropTarget(c, null);
        assertSame(c, dt.getComponent());
    }

    public void testSetActive() {
        DropTarget dt = new DropTarget(null, 1, null, true);
        assertTrue(dt.isActive());
        dt.setActive(false);
        assertFalse(dt.isActive());
        dt.setActive(true);
        assertTrue(dt.isActive());
    }

    public void testSetDefaultActions() {
        DropTarget dt = new DropTarget();
        testCheckDefaultAction(dt, DnDConstants.ACTION_NONE);
        testCheckDefaultAction(dt, DnDConstants.ACTION_COPY);
        testCheckDefaultAction(dt, DnDConstants.ACTION_COPY_OR_MOVE);
        testCheckDefaultAction(dt, DnDConstants.ACTION_LINK);
        testCheckDefaultAction(dt, DnDConstants.ACTION_MOVE);
        testCheckDefaultAction(dt, DnDConstants.ACTION_REFERENCE);
        testCheckDefaultAction(dt, 0xFF, DnDConstants.ACTION_COPY_OR_MOVE);
        testCheckDefaultAction(dt, 
                DnDConstants.ACTION_LINK|DnDConstants.ACTION_COPY_OR_MOVE);
        testCheckDefaultAction(dt, 
                DnDConstants.ACTION_LINK|~DnDConstants.ACTION_COPY_OR_MOVE,
                DnDConstants.ACTION_LINK);
        testCheckDefaultAction(dt, 
                DnDConstants.ACTION_COPY_OR_MOVE|~DnDConstants.ACTION_LINK, 
                DnDConstants.ACTION_COPY_OR_MOVE);
    }
    
    private void testCheckDefaultAction(DropTarget dt, int ops) {
        testCheckDefaultAction(dt, ops, ops);
    }

    private void testCheckDefaultAction(DropTarget dt, int ops, int expected) {
        dt.setDefaultActions(ops);
        assertEquals(expected, dt.getDefaultActions());
    }

    public void testIsActive() {
        Component c = new Component() {};
        DropTarget dt = new DropTarget(c, 1, null, true);
        assertTrue(dt.isActive());
        dt = new DropTarget(c, 1, null, false);
        assertFalse(dt.isActive());

        dt = new DropTarget(null, 1, null, false);
        assertTrue(dt.isActive());
        dt.setComponent(c);
        assertTrue(dt.isActive());
    }

    public void testClearAutoscroll() {
    }

    public void testGetDefaultActions() {
        checkGetDefaultActions(DnDConstants.ACTION_NONE);
        checkGetDefaultActions(DnDConstants.ACTION_COPY);
        checkGetDefaultActions(DnDConstants.ACTION_COPY_OR_MOVE);
        checkGetDefaultActions(DnDConstants.ACTION_LINK);
        checkGetDefaultActions(DnDConstants.ACTION_MOVE);
        checkGetDefaultActions(DnDConstants.ACTION_REFERENCE);
    }
    
    private void checkGetDefaultActions(int ops) {
        DropTarget dt = new DropTarget(null, ops, null);
        assertEquals(ops, dt.getDefaultActions());
    }
    
    public void testSetDropTarget() {
        Component c = new Component() {};
        DropTarget dt = new DropTarget();
        c.setDropTarget(dt);
        assertSame(dt, c.getDropTarget());
        assertSame(c, dt.getComponent());

        DropTarget dt2 = new DropTarget();
        c.setDropTarget(dt2);
        assertSame(dt2, c.getDropTarget());
        assertSame(c, dt2.getComponent());
        assertNull(dt.getComponent());
        
        c.setDropTarget(null);
        assertNull(c.getDropTarget());
        assertNull(dt.getComponent());
    }
}
