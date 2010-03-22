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
package org.apache.harmony.awt.datatransfer.linux;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.peer.DropTargetContextPeer;

import org.apache.harmony.awt.ContextStorage;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.wtk.linux.LinuxEventQueue;

public class LinuxDropTarget implements DropTargetContextPeer {
    
    private final LinuxDTK dtk;
    private final LinuxEventQueue nativeQueue;
    
    private final DropTargetContext context;
    private Transferable transferable;
    
    public LinuxDropTarget(LinuxDTK dtk, DropTargetContext context) {
        this.dtk = dtk;
        this.context = context;
        nativeQueue = (LinuxEventQueue)ContextStorage.getNativeEventQueue();
    }

    public void acceptDrag(int dragAction) {
    }

    public void acceptDrop(int dropAction) {
    }

    public void dropComplete(boolean success) {
    }

    public DropTarget getDropTarget() {
        return context.getDropTarget();
    }

    public int getTargetActions() {
        return context.getDropTarget().getDefaultActions();
    }

    public DataFlavor[] getTransferDataFlavors() {
        return (transferable != null) ? 
                transferable.getTransferDataFlavors() : new DataFlavor[0];
    }

    public Transferable getTransferable() throws InvalidDnDOperationException {
        if (transferable == null) {
            // awt.07=Transfer data is not available
            throw new InvalidDnDOperationException(
                    Messages.getString("awt.07")); //$NON-NLS-1$
        }
        return transferable;
    }

    public boolean isTransferableJVMLocal() {
        return false;
    }

    public void rejectDrag() {
    }

    public void rejectDrop() {
    }

    public void setTargetActions(int actions) {
        context.getDropTarget().setDefaultActions(actions);
    }

}
