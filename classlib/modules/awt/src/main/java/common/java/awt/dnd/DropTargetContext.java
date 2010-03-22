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

package java.awt.dnd;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.peer.DropTargetContextPeer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.harmony.awt.internal.nls.Messages;

public class DropTargetContext implements Serializable {

    private static final long serialVersionUID = -634158968993743371L;

    protected class TransferableProxy implements Transferable {

        protected boolean isLocal;

        protected Transferable transferable;

        TransferableProxy(boolean isLocal, Transferable transferable) {
            this.isLocal = isLocal;
            this.transferable = transferable;
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException,
                IOException {

            if (isLocal && flavor.isMimeTypeSerializedObject()) {
                Object data = transferable.getTransferData(flavor);
                if (data instanceof Serializable) {
                    return getSerializedCopy((Serializable) data);
                }
            }

            return transferable.getTransferData(flavor);
        }

        private Object getSerializedCopy(Serializable data) {
            try {
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
                objOut.writeObject(data);
                ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
                ObjectInputStream objIn = new ObjectInputStream(byteIn);
                return objIn.readObject();
            } catch (Exception e) {
                return data;
            }
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return transferable.isDataFlavorSupported(flavor);
        }

        public DataFlavor[] getTransferDataFlavors() {
            return transferable.getTransferDataFlavors();
        }
    }

    final DropTarget target;

    DropTargetContextPeer peer;

    DropTargetContext(DropTarget target) {
        this.target = target;
    }

    protected Transferable createTransferableProxy(Transferable t, boolean local) {
        return new TransferableProxy(local, t);
    }

    protected List<DataFlavor> getCurrentDataFlavorsAsList() {
        if (peer != null) {
            return Arrays.asList(peer.getTransferDataFlavors());
        }
        return new ArrayList<DataFlavor>();
    }

    public void addNotify(DropTargetContextPeer peer) {
        this.peer = peer;
    }

    public DropTarget getDropTarget() {
        return target;
    }

    protected Transferable getTransferable() throws InvalidDnDOperationException {
        if (peer == null) {
            // awt.07=Transfer data is not available
            throw new InvalidDnDOperationException(Messages.getString("awt.07")); //$NON-NLS-1$
        }
        return new TransferableProxy(peer.isTransferableJVMLocal(), peer.getTransferable());
    }

    protected boolean isDataFlavorSupported(DataFlavor flavor) {
        if (peer != null) {
            DataFlavor[] df = peer.getTransferDataFlavors();
            for (DataFlavor element : df) {
                if (element.equals(flavor)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected DataFlavor[] getCurrentDataFlavors() {
        if (peer != null) {
            return peer.getTransferDataFlavors();
        }
        return new DataFlavor[0];
    }

    public Component getComponent() {
        return target.getComponent();
    }

    public void dropComplete(boolean success) throws InvalidDnDOperationException {
        if (peer != null) {
            peer.dropComplete(success);
        }
    }

    protected void setTargetActions(int actions) {
        if (peer != null) {
            peer.setTargetActions(actions);
        }
        target.setDefaultActions(actions);
    }

    protected void acceptDrop(int dragOperation) {
        if (peer != null) {
            peer.acceptDrop(dragOperation);
        }
    }

    protected void acceptDrag(int dragOperation) {
        if (peer != null) {
            peer.acceptDrag(dragOperation);
        }
    }

    public void removeNotify() {
        peer = null;
    }

    protected void rejectDrop() {
        if (peer != null) {
            peer.rejectDrop();
        }
    }

    protected void rejectDrag() {
        if (peer != null) {
            peer.rejectDrag();
        }
    }

    protected int getTargetActions() {
        if (peer != null) {
            return peer.getTargetActions();
        }
        return target.getDefaultActions();
    }
}
