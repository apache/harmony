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
import java.awt.Cursor;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.FlavorMap;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.io.Serializable;
import java.util.EventListener;

import org.apache.harmony.awt.ListenerList;
import org.apache.harmony.awt.internal.nls.Messages;


// TODO: think of synchronization
public class DragSource implements Serializable {

    private static final long serialVersionUID = 6236096958971414066L;

    public static final Cursor DefaultMoveDrop;

    public static final Cursor DefaultMoveNoDrop;

    public static final Cursor DefaultCopyDrop;

    public static final Cursor DefaultCopyNoDrop;

    public static final Cursor DefaultLinkDrop;

    public static final Cursor DefaultLinkNoDrop;

    private static final int DEFAULT_DRAG_THRESHOLD = 5;

    private static DragSource defaultSource;
    private static DragSourceContext curContext;

    private final ListenerList<DragSourceListener> dragSourceListeners;
    private final ListenerList<DragSourceMotionListener> dragSourceMotionListeners;

    static {
        if (GraphicsEnvironment.isHeadless()) {
            DefaultMoveDrop = DefaultMoveNoDrop = DefaultCopyDrop = null;
            DefaultCopyNoDrop = DefaultLinkDrop = DefaultLinkNoDrop = null;
        } else {
            Toolkit toolkit = Toolkit.getDefaultToolkit();

            DefaultMoveDrop = getDefaultCursor(toolkit, "dnd.MoveCursor"); //$NON-NLS-1$
            DefaultMoveNoDrop = getDefaultCursor(toolkit, "dnd.NoMoveCursor"); //$NON-NLS-1$
            DefaultCopyDrop = getDefaultCursor(toolkit, "dnd.CopyCursor"); //$NON-NLS-1$
            DefaultCopyNoDrop = getDefaultCursor(toolkit, "dnd.NoCopyCursor"); //$NON-NLS-1$
            DefaultLinkDrop = getDefaultCursor(toolkit, "dnd.LinkCursor"); //$NON-NLS-1$
            DefaultLinkNoDrop = getDefaultCursor(toolkit, "dnd.NoLinkCursor"); //$NON-NLS-1$
        }
    }

    private static Cursor getDefaultCursor(Toolkit toolkit, String name) {
        try {
            return (Cursor) toolkit.getDesktopProperty(name);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(Messages.getString("awt.170", e)); //$NON-NLS-1$
        }
    }

    public static DragSource getDefaultDragSource() {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }

        if (defaultSource == null) {
            defaultSource = new DragSource();
        }

        return defaultSource;
    }

    public static boolean isDragImageSupported() {
        return false;
    }

    public static int getDragThreshold() {
        int threshold = Integer.getInteger("awt.dnd.drag.threshold", -1).intValue(); //$NON-NLS-1$

        if (threshold <= 0) {
            Object val = Toolkit.getDefaultToolkit().getDesktopProperty(
                    "DnD.gestureMotionThreshold" //$NON-NLS-1$
            );

            if (val != null && val instanceof Integer) {
                threshold = ((Integer) val).intValue();
            }

            if (threshold <= 0) {
                threshold = DEFAULT_DRAG_THRESHOLD;
            }
        }

        return threshold;
    }

    public DragSource() throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }

        dragSourceListeners = new ListenerList<DragSourceListener>();
        dragSourceMotionListeners = new ListenerList<DragSourceMotionListener>();
    }

    public DragSourceListener[] getDragSourceListeners() {
        return dragSourceListeners.getUserListeners(new DragSourceListener[0]);
    }

    public void addDragSourceListener(DragSourceListener dsl) {
        dragSourceListeners.addUserListener(dsl);
    }

    public void removeDragSourceListener(DragSourceListener dsl) {
        dragSourceListeners.removeUserListener(dsl);
    }

    public DragSourceMotionListener[] getDragSourceMotionListeners() {
        return dragSourceMotionListeners.getUserListeners(new DragSourceMotionListener[0]);
    }

    public void addDragSourceMotionListener(DragSourceMotionListener dsml) {
        dragSourceMotionListeners.addUserListener(dsml);
    }

    public void removeDragSourceMotionListener(DragSourceMotionListener dsml) {
        dragSourceMotionListeners.removeUserListener(dsml);
    }

    @SuppressWarnings("unchecked")
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (DragSourceListener.class.isAssignableFrom(listenerType)) {
            return (T[])getDragSourceListeners();
        } else if (DragSourceMotionListener.class.isAssignableFrom(
                listenerType)) {
            return (T[])getDragSourceMotionListeners();
        }

        return (T[])new EventListener[0];
    }

    protected DragSourceContext createDragSourceContext(
            DragSourceContextPeer dscp,
            DragGestureEvent dgl, Cursor dragCursor, 
            Image dragImage, Point imageOffset,
            Transferable t, DragSourceListener dsl)
    {
        return new DragSourceContext(dscp, dgl, dragCursor, 
                                     dragImage, imageOffset, t, dsl);
    }

    public FlavorMap getFlavorMap() {
        return SystemFlavorMap.getDefaultFlavorMap();
    }

    public void startDrag(DragGestureEvent trigger, Cursor dragCursor, 
                          Image dragImage, Point imageOffset, 
                          Transferable transferable, DragSourceListener dsl,
                          FlavorMap flavorMap)
            throws InvalidDnDOperationException {

        if (curContext != null) {
            // awt.171=Attempt to start a drag while an existing drag operation is still executing.
            throw new InvalidDnDOperationException(Messages.getString("awt.171"));  //$NON-NLS-1$
        }

        DragSourceContextPeer peer =
            Toolkit.getDefaultToolkit().createDragSourceContextPeer(trigger);
        curContext = createDragSourceContext(peer, trigger, dragCursor,
                                             dragImage, imageOffset,
                                             transferable, dsl);

        peer.startDrag(curContext, dragCursor, dragImage, imageOffset);
        curContext = null;
    }

    public void startDrag(DragGestureEvent trigger, 
                          Cursor dragCursor,
                          Image dragImage, 
                          Point dragOffset, 
                          Transferable transferable, 
                          DragSourceListener dsl)
            throws InvalidDnDOperationException {

        startDrag(trigger, dragCursor, dragImage, 
                  dragOffset, transferable, dsl, null);
    }

    public void startDrag(DragGestureEvent trigger, 
                          Cursor dragCursor, 
                          Transferable transferable, 
                          DragSourceListener dsl, 
                          FlavorMap flavorMap)
            throws InvalidDnDOperationException {

        startDrag(trigger, dragCursor, null, null, 
                  transferable, dsl, flavorMap);
    }

    public void startDrag(DragGestureEvent trigger,
                          Cursor dragCursor, 
                          Transferable transferable,
                          DragSourceListener dsl)
            throws InvalidDnDOperationException {

        startDrag(trigger, dragCursor, transferable, dsl, null);
    }

    public <T extends DragGestureRecognizer> T createDragGestureRecognizer(
            Class<T> recognizerAbstractClass, Component c, int actions, DragGestureListener dgl) {

        Toolkit t = Toolkit.getDefaultToolkit();
        return t.createDragGestureRecognizer(recognizerAbstractClass, this, c, actions, dgl);
    }

    public DragGestureRecognizer createDefaultDragGestureRecognizer(
                            Component c, 
                            int actions, 
                            DragGestureListener dgl) {

        Toolkit t = Toolkit.getDefaultToolkit();
        return t.createDragGestureRecognizer(
                MouseDragGestureRecognizer.class, this, c, actions, dgl);
    }

}
