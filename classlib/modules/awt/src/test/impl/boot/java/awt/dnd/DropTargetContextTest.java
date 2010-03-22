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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.peer.DropTargetContextPeer;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * Unit test for class java.awt.dnd.DropTargetContext
 */
public class DropTargetContextTest extends TestCase {
    
    static class LocalObject implements Transferable {
        
        final Object data;
        final DataFlavor df;
        
        LocalObject(Object data) {
            this.data = data;
            df = new DataFlavor(data.getClass(), "Object");
        }

        public Object getTransferData(DataFlavor flavor) 
                throws UnsupportedFlavorException, IOException {
            if (df.equals(flavor)) {
                return data;
            }
            return null;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return df.equals(flavor);
        }

        public DataFlavor[] getTransferDataFlavors() {
            try {
                DataFlavor f = (DataFlavor)df.clone();
                return new DataFlavor[] { f };
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
        
    }
    
    static class DropTargetContextStub implements DropTargetContextPeer {
        
        int actions;
        DropTarget dt;
        final LocalObject transferable;

        public boolean acceptedDrag;
        public boolean rejectedDrag;
        public int dragAction;

        public boolean acceptedDrop;
        public boolean rejectedDrop;
        public int dropAction;

        public boolean dropCompleted;
        public boolean dropSuccessful;
        
        
        DropTargetContextStub(Object data) {
            this(data, DnDConstants.ACTION_COPY_OR_MOVE);
        }

        DropTargetContextStub(Object data, int actions) {
            transferable = new LocalObject(data);
            this.actions = actions;
        }

        public int getTargetActions() {
            return actions;
        }

        public void setTargetActions(int actions) {
            this.actions = actions;
        }

        public DropTarget getDropTarget() {
            return dt;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return transferable.getTransferDataFlavors();
        }

        public Transferable getTransferable() 
                throws InvalidDnDOperationException {
            return transferable;
        }

        public boolean isTransferableJVMLocal() {
            return true;
        }

        public void acceptDrag(int dragAction) {
            acceptedDrag = true;
            this.dragAction = dragAction;
        }

        public void rejectDrag() {
            rejectedDrag = true;
        }

        public void acceptDrop(int dropAction) {
            acceptedDrop = true;
            this.dropAction = dropAction;
        }

        public void rejectDrop() {
            rejectedDrop = true;
        }

        public void dropComplete(boolean success) {
            dropCompleted = true;
            dropSuccessful = success;
        }
        
    }
    
    static class NotSerializable {
        String name;
        int value;
        NotSerializable(String n, int v) {
            name = n;
            value = v;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof NotSerializable) {
                NotSerializable ns = (NotSerializable)obj;
                return ((name != null && name.equals(ns.name))
                        || (name == null && ns.name == null))
                    && (value == ns.value);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            int h = (name != null) ? name.hashCode() : 8726345;
            return (h^value);
        }
    }
    
    DropTarget dt;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DropTargetContextTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        dt = new DropTarget();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testDropTargetContext() {
        DropTargetContext c = new DropTargetContext(dt);
        assertSame(dt, c.getDropTarget());
    }

    public void testCreateTransferableProxyNull() {
        DropTargetContext c = new DropTargetContext(dt);
        Transferable t = c.createTransferableProxy(null, true);
        assertNotNull(t);
        try {
            t.getTransferDataFlavors();
            fail("NPE expected");
        } catch (NullPointerException e) {}
        try {
            t.isDataFlavorSupported(DataFlavor.stringFlavor);
            fail("NPE expected");
        } catch (NullPointerException e) {}
    }

    public void testCreateTransferableProxyLocal() throws Exception {
        DropTargetContext c = new DropTargetContext(dt);
        String s = "test";
        LocalObject obj = new LocalObject(s);
        Transferable t = c.createTransferableProxy(obj, true);
        assertTrue(t.getTransferDataFlavors().length > 0);

        Object data1 = t.getTransferData(DataFlavor.stringFlavor);
        assertEquals(s, data1);
        assertNotSame(s, data1);
        
        Object data2 = t.getTransferData(new DataFlavor(String.class, "String"));
        assertEquals(s, data2);
        assertNotSame(s, data2);
    }

    public void testCreateTransferableProxyNotLocal() throws Exception {
        DropTargetContext c = new DropTargetContext(dt);
        String s = "test";
        LocalObject obj = new LocalObject(s);
        Transferable t = c.createTransferableProxy(obj, false);
        assertTrue(t.getTransferDataFlavors().length > 0);

        Object data1 = t.getTransferData(DataFlavor.stringFlavor);
        assertEquals(s, data1);
        assertSame(s, data1);
        
        Object data2 = t.getTransferData(new DataFlavor(String.class, "String"));
        assertEquals(s, data2);
        assertSame(s, data2);
    }

    public void testCreateTransferableProxyNotSerializable() throws Exception {
        DropTargetContext c = new DropTargetContext(dt);
        NotSerializable ns = new NotSerializable("test", 12);
        LocalObject obj = new LocalObject(ns);
        Transferable t = c.createTransferableProxy(obj, true);
        assertTrue(t.getTransferDataFlavors().length > 0);

        Object data = t.getTransferData(
                new DataFlavor(NotSerializable.class, "NotSerializable"));
        assertEquals(ns, data);
        assertSame(ns, data);
    }

    public void testAddNotify() {
        NotSerializable ns = new NotSerializable("test", 12);
        DropTargetContext c = new DropTargetContext(dt);
        DropTargetContextStub stub = new DropTargetContextStub(ns);
        c.addNotify(stub);
        DropTargetContext.TransferableProxy proxy =
            (DropTargetContext.TransferableProxy)c.getTransferable();
        assertSame(stub.transferable, proxy.transferable);
    }

    public void testRemoveNotify() {
        NotSerializable ns = new NotSerializable("test", 12);
        DropTargetContext c = new DropTargetContext(dt);
        c.removeNotify();
        
        DropTargetContextStub stub = new DropTargetContextStub(ns);
        c.addNotify(stub);
        assertNotNull(c.getTransferable());

        c.removeNotify();
        try {
            c.getTransferable();
            fail("InvalidDnDOperationException expected");
        } catch (InvalidDnDOperationException e) {}
    }

    public void testGetDropTarget() {
        DropTargetContext c = new DropTargetContext(dt);
        assertSame(dt, c.getDropTarget());
    }

    public void testGetTransferable() {
        NotSerializable ns = new NotSerializable("test", 12);
        DropTargetContext c = new DropTargetContext(dt);
        try {
            c.getTransferable();
            fail("InvalidDnDOperationException expected");
        } catch (InvalidDnDOperationException e) {}

        DropTargetContextStub stub = new DropTargetContextStub(ns);
        c.addNotify(stub);
        try {
            c.getTransferable();
        } catch (InvalidDnDOperationException e) {
            fail("InvalidDnDOperationException");
        }

        assertTrue(c.getTransferable() 
                instanceof DropTargetContext.TransferableProxy);
        
        DropTargetContext.TransferableProxy proxy =
            (DropTargetContext.TransferableProxy)c.getTransferable();
        assertSame(stub.transferable, proxy.transferable);
    }

    public void testIsDataFlavorSupported() {
        NotSerializable ns = new NotSerializable("test", 12);
        LocalObject lo = new LocalObject(ns);

        DropTargetContext c = new DropTargetContext(dt);
        assertFalse(c.isDataFlavorSupported(lo.df));

        DropTargetContextStub stub = new DropTargetContextStub(ns);
        c.addNotify(stub);

        assertTrue(c.isDataFlavorSupported(lo.df));
        assertFalse(c.isDataFlavorSupported(DataFlavor.javaFileListFlavor));
    }

    public void testGetCurrentDataFlavors() {
        DropTargetContext c = new DropTargetContext(dt);
        assertNotNull(c.getCurrentDataFlavors());
        assertEquals(0, c.getCurrentDataFlavors().length);

        NotSerializable ns = new NotSerializable("test", 12);
        LocalObject lo = new LocalObject(ns);
        DropTargetContextStub stub = new DropTargetContextStub(ns);
        c.addNotify(stub);

        assertNotNull(c.getCurrentDataFlavors());
        assertEquals(1, c.getCurrentDataFlavors().length);
        assertEquals(lo.df, c.getCurrentDataFlavors()[0]);
        assertNotSame(c.getCurrentDataFlavors(), c.getCurrentDataFlavors());
        assertNotSame(c.getCurrentDataFlavors()[0], 
                c.getCurrentDataFlavors()[0]);
    }

    public void testGetCurrentDataFlavorsAsList() {
        DropTargetContext c = new DropTargetContext(dt);
        assertNotNull(c.getCurrentDataFlavorsAsList());
        assertEquals(0, c.getCurrentDataFlavorsAsList().size());

        NotSerializable ns = new NotSerializable("test", 12);
        LocalObject lo = new LocalObject(ns);
        DropTargetContextStub stub = new DropTargetContextStub(ns);
        c.addNotify(stub);

        assertNotNull(c.getCurrentDataFlavorsAsList());
        assertEquals(1, c.getCurrentDataFlavorsAsList().size());
        assertEquals(lo.df, c.getCurrentDataFlavorsAsList().get(0));
        assertNotSame(c.getCurrentDataFlavorsAsList(), 
                c.getCurrentDataFlavorsAsList());
        assertNotSame(c.getCurrentDataFlavorsAsList().get(0), 
                c.getCurrentDataFlavorsAsList().get(0));
    }

    @SuppressWarnings("serial")
    public void testGetComponent() {
        Component comp = new Component() {};
        dt.setComponent(comp);
        DropTargetContext c = new DropTargetContext(dt);
        assertSame(comp, c.getComponent());
    }

    public void testDropComplete() {
        DropTargetContext c = new DropTargetContext(dt);
        c.dropComplete(true);

        NotSerializable ns = new NotSerializable("test", 12);
        DropTargetContextStub stub = new DropTargetContextStub(ns);
        c.addNotify(stub);

        assertFalse(stub.dropCompleted);
        c.dropComplete(true);
        assertTrue(stub.dropCompleted);
        assertTrue(stub.dropSuccessful);

        stub.dropCompleted = false;
        c.dropComplete(false);
        assertTrue(stub.dropCompleted);
        assertFalse(stub.dropSuccessful);
    }

    public void testAcceptDrop() {
        DropTargetContext c = new DropTargetContext(dt);
        c.acceptDrop(DnDConstants.ACTION_COPY);

        NotSerializable ns = new NotSerializable("test", 12);
        DropTargetContextStub stub = new DropTargetContextStub(ns);
        c.addNotify(stub);

        assertFalse(stub.acceptedDrop);
        c.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        assertTrue(stub.acceptedDrop);
        assertEquals(DnDConstants.ACTION_COPY_OR_MOVE, stub.dropAction);

        stub.acceptedDrop  = false;
        c.acceptDrop(DnDConstants.ACTION_LINK);
        assertTrue(stub.acceptedDrop);
        assertEquals(DnDConstants.ACTION_LINK, stub.dropAction);
    }

    public void testAcceptDrag() {
        DropTargetContext c = new DropTargetContext(dt);
        c.acceptDrag(DnDConstants.ACTION_COPY);

        NotSerializable ns = new NotSerializable("test", 12);
        DropTargetContextStub stub = new DropTargetContextStub(ns);
        c.addNotify(stub);

        assertFalse(stub.acceptedDrag);
        c.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
        assertTrue(stub.acceptedDrag);
        assertEquals(DnDConstants.ACTION_COPY_OR_MOVE, stub.dragAction);

        stub.acceptedDrop  = false;
        c.acceptDrag(DnDConstants.ACTION_LINK);
        assertTrue(stub.acceptedDrag);
        assertEquals(DnDConstants.ACTION_LINK, stub.dragAction);
    }

    public void testRejectDrop() {
        DropTargetContext c = new DropTargetContext(dt);
        c.rejectDrop();

        NotSerializable ns = new NotSerializable("test", 12);
        DropTargetContextStub stub = new DropTargetContextStub(ns);
        c.addNotify(stub);

        assertFalse(stub.rejectedDrop);
        c.rejectDrop();
        assertTrue(stub.rejectedDrop);
    }

    public void testRejectDrag() {
        DropTargetContext c = new DropTargetContext(dt);
        c.rejectDrag();

        NotSerializable ns = new NotSerializable("test", 12);
        DropTargetContextStub stub = new DropTargetContextStub(ns);
        c.addNotify(stub);

        assertFalse(stub.rejectedDrag);
        c.rejectDrag();
        assertTrue(stub.rejectedDrag);
    }

    public void testGetTargetActions() {
        try {
            DropTargetContext c = new DropTargetContext(null);
            c.getTargetActions();
            fail("NPE expected");
        } catch (NullPointerException e) {}
        
        DropTargetContext c = new DropTargetContext(dt);
        assertEquals(dt.getDefaultActions(), c.getTargetActions());
        dt.setDefaultActions(DnDConstants.ACTION_LINK);
        assertEquals(dt.getDefaultActions(), c.getTargetActions());
        dt.setDefaultActions(DnDConstants.ACTION_COPY_OR_MOVE);
        assertEquals(dt.getDefaultActions(), c.getTargetActions());

        NotSerializable ns = new NotSerializable("test", 12);
        DropTargetContextStub stub = new DropTargetContextStub(ns);
        c.addNotify(stub);
        
        assertEquals(stub.getTargetActions(), c.getTargetActions());
        stub.setTargetActions(DnDConstants.ACTION_LINK);
        assertEquals(stub.getTargetActions(), c.getTargetActions());
        stub.setTargetActions(DnDConstants.ACTION_NONE);
        assertEquals(stub.getTargetActions(), c.getTargetActions());
    }

    public void testSetTargetActions() {
        try {
            DropTargetContext c = new DropTargetContext(null);
            c.setTargetActions(DnDConstants.ACTION_COPY_OR_MOVE);
            fail("NPE expected");
        } catch (NullPointerException e) {}
        
        DropTargetContext c = new DropTargetContext(dt);
        assertEquals(DnDConstants.ACTION_COPY_OR_MOVE, c.getTargetActions());
        assertEquals(DnDConstants.ACTION_COPY_OR_MOVE, dt.getDefaultActions());

        c.setTargetActions(DnDConstants.ACTION_NONE);
        assertEquals(DnDConstants.ACTION_NONE, dt.getDefaultActions());
        assertEquals(DnDConstants.ACTION_NONE, c.getTargetActions());

        c.setTargetActions(DnDConstants.ACTION_LINK);
        assertEquals(DnDConstants.ACTION_LINK, dt.getDefaultActions());
        assertEquals(DnDConstants.ACTION_LINK, c.getTargetActions());

        NotSerializable ns = new NotSerializable("test", 12);
        DropTargetContextStub stub = new DropTargetContextStub(ns);

        assertEquals(DnDConstants.ACTION_COPY_OR_MOVE, stub.getTargetActions());
        
        c.addNotify(stub);

        assertEquals(DnDConstants.ACTION_COPY_OR_MOVE, stub.getTargetActions());
        assertEquals(DnDConstants.ACTION_COPY_OR_MOVE, c.getTargetActions());
        assertEquals(DnDConstants.ACTION_LINK, dt.getDefaultActions());

        c.setTargetActions(DnDConstants.ACTION_NONE);
        assertEquals(DnDConstants.ACTION_NONE, dt.getDefaultActions());
        assertEquals(DnDConstants.ACTION_NONE, c.getTargetActions());
        
        c.setTargetActions(DnDConstants.ACTION_LINK);
        assertEquals(DnDConstants.ACTION_LINK, dt.getDefaultActions());
        assertEquals(DnDConstants.ACTION_LINK, c.getTargetActions());
    }

}
