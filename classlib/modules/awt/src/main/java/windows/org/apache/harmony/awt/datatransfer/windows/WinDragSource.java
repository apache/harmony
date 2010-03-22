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

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.event.InputEvent;

import org.apache.harmony.awt.ContextStorage;
import org.apache.harmony.awt.datatransfer.DataSnapshot;
import org.apache.harmony.awt.datatransfer.DataSource;
import org.apache.harmony.awt.datatransfer.DragSourceEventProxy;
import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.awt.nativebridge.windows.WinDataTransfer;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;
import org.apache.harmony.awt.wtk.windows.WinEventQueue;

/**
 * Starts drag operation and handles callbacks from OLE.
 * The callbacks simply do EventQueue.invokeLater(new DragSourceEventProxy(...))
 */
public class WinDragSource implements DragSourceContextPeer {

    private static final Win32 win32 = Win32.getInstance();

    private Cursor cursor;
    private DragSourceContext context;

    private final WinEventQueue winEventQueue;
    
    private int userAction;
    private int targetActions;
    private Point mouseLocation;
    
    public WinDragSource() {
        winEventQueue = (WinEventQueue)ContextStorage.getNativeEventQueue();
    }

    public void startDrag(DragSourceContext dsc, Cursor c, Image di, Point ioff)
            throws InvalidDnDOperationException {

        synchronized (this) {
            context = dsc;
            cursor = c;
        }
        mouseLocation = getCurrentMouseLocation();
        userAction = 0;
        targetActions = 0;
        
        DataSource dataSource = new DataSource(context.getTransferable());
        final DataSnapshot snapshot = new DataSnapshot(dataSource);
        final int srcActions = getWinActions(context.getSourceActions());

        WinEventQueue.Task task = new WinEventQueue.Task() {
            @Override
            public void perform() {
                WinDataTransfer.startDrag(snapshot, 
                        WinDragSource.this, srcActions);
            }
        };
        winEventQueue.performLater(task);
    }

    public Cursor getCursor() {
        synchronized (this) {
            return cursor;
        }
    }

    public void setCursor(Cursor c) throws InvalidDnDOperationException {
        synchronized (this) {
            cursor = c;
        }
        
        // TODO: implement native cursor update,
        // and in native method WinDragSource::GiveFeedback() 
        // change return value to S_OK to suppress default OLE cursors
    }

    public void transferablesFlavorsChanged() {
        // TODO: call this method from IAdviseSink::OnDataChange()
    }

    static int getDndActions(int winActions) {
        int dndActions = 0;
        if ((winActions & WindowsDefs.DROPEFFECT_COPY) != 0) {
            dndActions |= DnDConstants.ACTION_COPY;
        }
        if ((winActions & WindowsDefs.DROPEFFECT_MOVE) != 0) {
            dndActions |= DnDConstants.ACTION_MOVE;
        }
        if ((winActions & WindowsDefs.DROPEFFECT_LINK) != 0) {
            dndActions |= DnDConstants.ACTION_LINK;
        }
        return dndActions;
    }
    
    static int getWinActions(int dndActions) {
        int winActions = 0;
        if ((dndActions & DnDConstants.ACTION_COPY) != 0) {
            winActions |= WindowsDefs.DROPEFFECT_COPY;
        }
        if ((dndActions & DnDConstants.ACTION_MOVE) != 0) {
            winActions |= WindowsDefs.DROPEFFECT_MOVE;
        }
        if ((dndActions & DnDConstants.ACTION_LINK) != 0) {
            winActions |= WindowsDefs.DROPEFFECT_LINK;
        }
        return winActions;
    }
    
    /**
     * Called from native method WinDragSource::GiveFeedback()
     * @param winActions - drop actions acceptable for drop target
     * @param scroll - scrolling is asked by drop target
     */
    public void giveFeedback(int winActions, boolean scroll) {
        int dndActions = getDndActions(winActions);
        updateLocationAndActions(dndActions);
    }
    
    /**
     * Called from native method WinDragSource::QueryContinueDrag()
     */
    public void continueDrag() {
        updateLocationAndActions(targetActions);
    }
    
    /**
     * Called from native method WinDataTransfer::startDrag()
     * after drag operation is finished
     * @param winAction - drop action taken
     * @param success - drop was completed successfully
     */
    public void endDrag(int winAction, boolean success) {
        int dndAction = getDndActions(winAction);
        DragSourceEventProxy r = new DragSourceEventProxy(
                context,
                DragSourceEventProxy.DRAG_DROP_END,
                dndAction, success,
                mouseLocation, getCurrentModifiers());
        EventQueue.invokeLater(r);
    }
    
    private void updateLocationAndActions(int newTargetActions) {
        Point newLocation = getCurrentMouseLocation();
        int modifiers = getCurrentModifiers();
        int newUserAction = getUserAction(modifiers, newTargetActions);

        if (!newLocation.equals(mouseLocation)) {
            mouseLocation.setLocation(newLocation);
            if (newTargetActions != 0) {
                DragSourceEventProxy r = new DragSourceEventProxy(
                        context,
                        DragSourceEventProxy.DRAG_MOUSE_MOVED,
                        newUserAction, newTargetActions, 
                        mouseLocation, modifiers);
                EventQueue.invokeLater(r);
            }
        }
        
        if (newUserAction != userAction || newTargetActions != targetActions) {
            int type = 0;
            if (targetActions == 0 && newTargetActions != 0) {
                type = DragSourceEventProxy.DRAG_ENTER;
            } else if (targetActions != 0 && newTargetActions == 0) {
                type = DragSourceEventProxy.DRAG_EXIT;
            } else {
                type = DragSourceEventProxy.DRAG_ACTION_CHANGED;
            }
            userAction = newUserAction;
            targetActions = newTargetActions;
            if (type != 0) {
                DragSourceEventProxy r = new DragSourceEventProxy(
                        context,
                        type, newUserAction, newTargetActions, 
                        mouseLocation, modifiers);
                EventQueue.invokeLater(r);
            }
        }
    }
    
    private static Point getCurrentMouseLocation() {
        Win32.POINT lpPoint = win32.createPOINT(false);
        win32.GetCursorPos(lpPoint);
        return new Point(lpPoint.get_x(), lpPoint.get_y());
    }
    
    private static int getCurrentModifiers() {
        int modifiers = 0;
        modifiers |= ((win32.GetKeyState(WindowsDefs.VK_SHIFT) & 0x80) != 0) ?
                InputEvent.SHIFT_DOWN_MASK : 0;
        modifiers |= ((win32.GetKeyState(WindowsDefs.VK_CONTROL) & 0x80) != 0) ?
                InputEvent.CTRL_DOWN_MASK : 0;
        modifiers |= ((win32.GetKeyState(WindowsDefs.VK_MENU) & 0x80) != 0) ?
                InputEvent.ALT_DOWN_MASK : 0;
        modifiers |= ((win32.GetKeyState(WindowsDefs.VK_LBUTTON) & 0x80) != 0) ?
                InputEvent.BUTTON1_DOWN_MASK : 0;
        modifiers |= ((win32.GetKeyState(WindowsDefs.VK_MBUTTON) & 0x80) != 0) ?
                InputEvent.BUTTON2_DOWN_MASK : 0;
        modifiers |= ((win32.GetKeyState(WindowsDefs.VK_RBUTTON) & 0x80) != 0) ?
                InputEvent.BUTTON3_DOWN_MASK : 0;
        return modifiers;
    }
    
    private static int getUserAction(int modifiers, int targetActions) {
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            return ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) ?
                    DnDConstants.ACTION_LINK : DnDConstants.ACTION_COPY;
        }
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
            return DnDConstants.ACTION_MOVE;
        }
        if ((targetActions & DnDConstants.ACTION_MOVE) != 0) {
            return DnDConstants.ACTION_MOVE;
        }
        if ((targetActions & DnDConstants.ACTION_COPY) != 0) {
            return DnDConstants.ACTION_COPY;
        }
        if ((targetActions & DnDConstants.ACTION_LINK) != 0) {
            return DnDConstants.ACTION_LINK;
        }
        return DnDConstants.ACTION_NONE;
    }
}
