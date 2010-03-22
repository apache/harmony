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
 * @author Michael Danilov
 */
package java.awt.dnd;

import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.io.Serializable;
import java.util.TooManyListenersException;

import org.apache.harmony.awt.internal.nls.Messages;

public class DragSourceContext implements DragSourceListener,
        DragSourceMotionListener, Serializable
{

    private static final long serialVersionUID = -115407898692194719L;

    protected static final int DEFAULT = 0;

    protected static final int ENTER = 1;

    protected static final int OVER = 2;

    protected static final int CHANGED = 3;

    private static final int EXIT = DEFAULT;

    private final DragSource dragSource;
    private final DragGestureEvent trigger;
    private final Transferable transferable;
    private final Component component;
    private final DragSourceContextPeer peer;

    private int sourceAction;
    private DragSourceListener listener;
    private Cursor cursor;
    private boolean defaultCursor;
    private int lastTargetAction;
    private int lastStatus;

    public DragSourceContext(DragSourceContextPeer dscp, DragGestureEvent trigger,
            Cursor dragCursor, Image dragImage, Point offset,
            Transferable t, DragSourceListener dsl)
    {
        if (dscp == null) {
            // awt.179=Context peer is null.
            throw new NullPointerException(Messages.getString("awt.179")); //$NON-NLS-1$
        }
        if (trigger == null) {
            // awt.17A=Trigger event is null.
            throw new NullPointerException(Messages.getString("awt.17A")); //$NON-NLS-1$
        }
        if (trigger.getDragAction() == DnDConstants.ACTION_NONE) {
            // awt.17B=Can't init ACTION_NONE drag.
            throw new RuntimeException(Messages.getString("awt.17B")); //$NON-NLS-1$
        }
        if ((dragImage != null) && (offset == null)) {
            // awt.17C=Image offset is null.
            throw new NullPointerException(Messages.getString("awt.17C")); //$NON-NLS-1$
        }
        if (t == null) {
            // awt.17D=Transferable is null.
            throw new NullPointerException(Messages.getString("awt.17D")); //$NON-NLS-1$
        }
        if (trigger.getComponent() == null) {
            // awt.17E=Component associated with the trigger event is null.
            throw new IllegalArgumentException(Messages.getString("awt.17E")); //$NON-NLS-1$
        }
        if (trigger.getDragSource() == null) {
            // awt.17F=DragSource for the trigger event is null.
            throw new IllegalArgumentException(Messages.getString("awt.17F")); //$NON-NLS-1$
        }
        if (trigger.getSourceAsDragGestureRecognizer().getSourceActions()
                == DnDConstants.ACTION_NONE)
        {
            // awt.180=Source actions for the DragGestureRecognizer associated with the trigger event are equal to DnDConstants.ACTION_NONE.
            throw new IllegalArgumentException(Messages.getString("awt.180")); //$NON-NLS-1$
        }

        this.trigger = trigger;
        transferable = t;
        dragSource = trigger.getDragSource();
        sourceAction = trigger.getDragAction();
        component = trigger.getComponent();
        peer = dscp;

        try {
            addDragSourceListener(dsl);
        } catch (TooManyListenersException e) {
        }
        lastTargetAction = DnDConstants.ACTION_NONE;
        lastStatus = DEFAULT;
        setCursor(dragCursor);
    }

    public DragGestureEvent getTrigger() {
        return trigger;
    }

    public Transferable getTransferable() {
        return transferable;
    }

    public DragSource getDragSource() {
        return dragSource;
    }

    public int getSourceActions() {
        return sourceAction;
    }

    public Component getComponent() {
        return component;
    }

    public Cursor getCursor() {
        return cursor;
    }

    public synchronized void setCursor(Cursor c) {
        cursor = c;

        defaultCursor = (cursor == null);
        if (defaultCursor) {
            updateCurrentCursor(sourceAction, lastTargetAction, lastStatus);
        } else {
            peer.setCursor(cursor);
        }
    }

    public synchronized void addDragSourceListener(DragSourceListener dsl) throws TooManyListenersException {
        if (dsl == null) {
            return;
        }
        if (dsl == this) {
            // awt.181=Attempt to register context as its listener.
            throw new IllegalArgumentException(Messages.getString("awt.181")); //$NON-NLS-1$
        }
        if (listener != null) {
            // awt.173=One listener is already exist.
            throw new TooManyListenersException(Messages.getString("awt.173")); //$NON-NLS-1$
        }

        listener = dsl;
    }

    public synchronized void removeDragSourceListener(DragSourceListener dsl) {
        if (listener != dsl) {
            // awt.182=dsl is not current listener.
            throw new IllegalArgumentException(Messages.getString("awt.182")); //$NON-NLS-1$
        }

        listener = null;
    }

    protected synchronized void updateCurrentCursor(int dropOp, int targetAct, int status) {
        if (!defaultCursor) {
            return;
        }
        if ((status < DEFAULT) || (status > CHANGED)) {
            // awt.183=Invalid status.
            throw new RuntimeException(Messages.getString("awt.183")); //$NON-NLS-1$
        }

        int possibleOps = dropOp & ((status == DEFAULT) ? DnDConstants.ACTION_NONE : targetAct);
        int theOperation;
        boolean opEnabled;

        if (possibleOps == DnDConstants.ACTION_NONE) {
            theOperation = findBestAction(dropOp);
            opEnabled = false;
        } else {
            theOperation = findBestAction(possibleOps);
            opEnabled = true;
        }

        peer.setCursor(findCursor(theOperation, opEnabled));
    }

    private void updateCursor(int dropOp, int targetAct, int status) {
        lastTargetAction = targetAct;
        lastStatus = status;

        updateCurrentCursor(dropOp, targetAct, status);
    }

    private int findBestAction(int actions) {
        if ((actions & DnDConstants.ACTION_MOVE) != 0) {
            return DnDConstants.ACTION_MOVE;
        } else if ((actions & DnDConstants.ACTION_COPY) != 0) {
            return DnDConstants.ACTION_COPY;
        } else  if ((actions & DnDConstants.ACTION_LINK) != 0) {
            return DnDConstants.ACTION_LINK;
        } else {
            return DnDConstants.ACTION_MOVE;
        }
    }

    private Cursor findCursor(int action, boolean enabled) {
        switch (action) {
        case DnDConstants.ACTION_MOVE:
            return (enabled ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop);
        case DnDConstants.ACTION_COPY:
            return (enabled ? DragSource.DefaultCopyDrop : DragSource.DefaultCopyNoDrop);
        case DnDConstants.ACTION_LINK:
            return (enabled ? DragSource.DefaultLinkDrop : DragSource.DefaultLinkNoDrop);
        default:
            // awt.184=Invalid action.
            throw new RuntimeException(Messages.getString("awt.184")); //$NON-NLS-1$
        }
    }

    public void transferablesFlavorsChanged() {
        peer.transferablesFlavorsChanged();
    }

    public void dragEnter(DragSourceDragEvent dsde) {
        if (listener != null) {
            listener.dragEnter(dsde);
        }
        updateCursor(sourceAction, dsde.getTargetActions(), ENTER);
    }

    public void dragOver(DragSourceDragEvent dsde) {
        if (listener != null) {
            listener.dragOver(dsde);
        }
        updateCursor(sourceAction, dsde.getTargetActions(), OVER);
    }

    public void dropActionChanged(DragSourceDragEvent dsde) {
        sourceAction = dsde.getDropAction();
        if (listener != null) {
            listener.dropActionChanged(dsde);
        }
        updateCursor(sourceAction, dsde.getTargetActions(), CHANGED);
    }

    public void dragExit(DragSourceEvent dse) {
        if (listener != null) {
            listener.dragExit(dse);
        }
        updateCursor(sourceAction, DnDConstants.ACTION_NONE, EXIT);
    }

    public void dragDropEnd(DragSourceDropEvent dsde) {
        if (listener != null) {
            listener.dragDropEnd(dsde);
        }
    }

    public void dragMouseMoved(DragSourceDragEvent dsde) {
        DragSourceMotionListener[] listeners = dragSource.getDragSourceMotionListeners();

        for (DragSourceMotionListener element : listeners) {
            element.dragMouseMoved(dsde);
        }
    }

}
