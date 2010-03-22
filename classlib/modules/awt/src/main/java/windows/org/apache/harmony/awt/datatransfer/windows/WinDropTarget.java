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
package org.apache.harmony.awt.datatransfer.windows;

import java.awt.EventQueue;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.peer.DropTargetContextPeer;

import org.apache.harmony.awt.ComponentInternals;
import org.apache.harmony.awt.datatransfer.DataProxy;
import org.apache.harmony.awt.datatransfer.DataSnapshot;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.nativebridge.windows.WinDataTransfer;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;
import org.apache.harmony.awt.wtk.NativeWindow;
import org.apache.harmony.awt.wtk.NativeEventQueue.Task;

/**
 * Handles OLE drop opreration on particular heavyweight component,
 * and dispatches DropTarget events. The drop operation runs on 
 * DataTransferThread because DropTarget events are dispatched 
 * synchronously. While handling the OLE callback the data transfer thread 
 * is blocked until event dispatch thread handles the DropTargetEvent. 
 * It is done due to synchronous nature of OLE IDropTarget callbacks. 
 */
public class WinDropTarget implements DropTargetContextPeer, Runnable {

    private final DropTargetContext context;
    private final long hwnd;
    private final long dropTargetPtr;

    private WinDataTransfer.IDataObject dataObject;
    private DataProxy transferable;
    
    private static final int DRAG_NONE = 0;
    private static final int DRAG_ENTER = 1;
    private static final int DRAG_OVER = 2;
    private static final int DRAG_DROP = 3;
    private static final int DRAG_LEAVE = 4;
    private int dragState;
    private int dropAction;
    private DropTargetEvent currentEvent;
    private class DropMonitor {}
    private final Object dropMonitor = new DropMonitor();

    private final WinDTK dtk;
    
    public WinDropTarget(WinDTK dtk, DropTargetContext context) {
        this.dtk = dtk;
        this.context = context;

        ComponentInternals ci = ComponentInternals.getComponentInternals();
        NativeWindow w = ci.getNativeWindow(context.getComponent());
        hwnd = w.getId();
        dropTargetPtr = registerDropTarget();
    }
    
    private long registerDropTarget() {
        Task task = new Task() {
            @Override
            public void perform() {
                long ret = WinDataTransfer.registerDropTarget(
                        hwnd, WinDropTarget.this);
                returnValue = new Long(ret);
            }
        };
        dtk.performTask(task);
        return((Long)task.returnValue).longValue();
    }
    
    public int getTargetActions() {
        return context.getDropTarget().getDefaultActions();
    }

    public void setTargetActions(int actions) {
        context.getDropTarget().setDefaultActions(actions);
    }

    public DropTarget getDropTarget() {
        return context.getDropTarget();
    }

    public DataFlavor[] getTransferDataFlavors() {
        return (transferable != null) ? 
                transferable.getTransferDataFlavors() : new DataFlavor[0];
    }

    public Transferable getTransferable() throws InvalidDnDOperationException {
        if (transferable == null) {
            // awt.14=Transfer data is not available
            throw new InvalidDnDOperationException(Messages.getString("awt.14")); //$NON-NLS-1$
        }
        return transferable;
    }

    public boolean isTransferableJVMLocal() {
        return false;
    }

    public void acceptDrag(int dragAction) {
        // TODO: update dropAction
    }

    public void rejectDrag() {
        // TODO: set dropAction to zero
    }

    public void acceptDrop(int dropAction) {
        // TODO: update dropAction
    }

    public void rejectDrop() {
    }

    public void dropComplete(boolean success) {
    }

    /**
     * Called from native method WinDropTarget::DragEnter()
     * @return bit mask of DROPEFFECT_* constants according to dropAction
     */
    public int dragEnter(long dataObjectPtr, int x, int y, 
                         int userAction, int sourceActions) {
        dataObject = new WinDataTransfer.IDataObject(dataObjectPtr);

        DataSnapshot snapshot = new DataSnapshot(dataObject);
        transferable = new DataProxy(snapshot);
        
        DropTargetDragEvent e = new DropTargetDragEvent(
                context, new Point(x, y), 
                WinDragSource.getDndActions(userAction), 
                WinDragSource.getDndActions(sourceActions));
        
        return dispatchEvent(e, DRAG_ENTER);
    }

    /**
     * Called from native method WinDropTarget::DragLeave()
     */
    public void dragLeave() {
        DropTargetEvent e = new DropTargetEvent(context);
        dispatchEvent(e, DRAG_LEAVE);
        dataObject = null;
        transferable = null;
    }

    /**
     * Called from native method WinDropTarget::DragOver()
     * @return bit mask of DROPEFFECT_* constants according to dropAction
     */
    public int dragOver(int x, int y, int userAction, int sourceActions) {
        DropTargetDragEvent e = new DropTargetDragEvent(
                context, new Point(x, y), 
                WinDragSource.getDndActions(userAction), 
                WinDragSource.getDndActions(sourceActions));
        
        return dispatchEvent(e, DRAG_OVER);
    }

    /**
     * Called from native method WinDropTarget::Drop()
     * @return bit mask of DROPEFFECT_* constants according to dropAction
     */
    public int drop(long dataObjectPtr, int x, int y, 
                    int userAction, int sourceActions) {
        if (dataObjectPtr != dataObject.pointer) {
            dataObject = new WinDataTransfer.IDataObject(dataObjectPtr);
            DataSnapshot snapshot = new DataSnapshot(dataObject);
            transferable = new DataProxy(snapshot);
        }

        DropTargetDropEvent e = new DropTargetDropEvent(
                context, new Point(x, y), 
                WinDragSource.getDndActions(userAction), 
                WinDragSource.getDndActions(sourceActions));
        
        int ret = dispatchEvent(e, DRAG_DROP);
        dataObject = null;
        transferable = null;
        return ret;
    }

    /**
     * Call this method from {@link DropTargetContext#removeNotify()}
     */
    public void dispose() {
        WinDataTransfer.revokeDropTarget(hwnd, dropTargetPtr);
    }

    /**
     * Dispatch DropTargetEvent on event dispatch thread.
     * {@link EventQueue#invokeLater(Runnable)} is used to invoke this method.
     */
    public void run() {
        synchronized (dropMonitor) {
            switch (dragState) {
            case DRAG_ENTER:
            {
                DropTargetDragEvent e = (DropTargetDragEvent)currentEvent;
                context.getDropTarget().dragEnter(e);
                dropAction = e.getDropAction();
                break;
            }
            case DRAG_OVER:
            {
                DropTargetDragEvent e = (DropTargetDragEvent)currentEvent;
                context.getDropTarget().dragOver(e);
                dropAction = e.getDropAction();
                break;
            }
            case DRAG_DROP:
            {
                DropTargetDropEvent e = (DropTargetDropEvent)currentEvent;
                context.getDropTarget().drop(e);
                dropAction = e.getDropAction();
                break;
            }
            case DRAG_LEAVE:
            {
                context.getDropTarget().dragExit(currentEvent);
                dropAction = DnDConstants.ACTION_NONE;
                break;
            }
            default:
                dropAction = DnDConstants.ACTION_NONE;
                break;
            }
            dragState = DRAG_NONE;
            
            dropMonitor.notify();
        }
    }
    
    private int dispatchEvent(DropTargetEvent e, int state) {
        synchronized (dropMonitor) {
            try {
                dragState = state;
                currentEvent = e;
                dropAction = DnDConstants.ACTION_NONE;
                EventQueue.invokeLater(this);
                while (dragState != DRAG_NONE) {
                    dropMonitor.wait();
                }
                return WinDragSource.getWinActions(dropAction);
            } catch (InterruptedException ex) {
                return WindowsDefs.DROPEFFECT_NONE;
            }
        }
    }
}
