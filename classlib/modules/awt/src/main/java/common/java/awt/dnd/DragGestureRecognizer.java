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
import java.awt.Point;
import java.awt.event.InputEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TooManyListenersException;

import org.apache.harmony.awt.internal.nls.Messages;

public abstract class DragGestureRecognizer implements Serializable {

    private static final long serialVersionUID = 8996673345831063337L;

    protected DragSource dragSource;

    protected int sourceActions;

    protected Component component;

    protected ArrayList<InputEvent> events;

    protected transient DragGestureListener dragGestureListener;

    protected DragGestureRecognizer(DragSource ds, Component c, int sa, DragGestureListener dgl) {
        if (ds == null) {
            // awt.172=Drag source is null.
            throw new IllegalArgumentException(Messages.getString("awt.172")); //$NON-NLS-1$
        }

        dragSource = ds;
        component = c;
        sourceActions = sa;

        try {
            addDragGestureListener(dgl);
        } catch (TooManyListenersException e) {
        }

        events = null;
    }

    protected DragGestureRecognizer(DragSource ds, Component c, int sa) {
        this(ds, c, sa, null);
    }

    protected DragGestureRecognizer(DragSource ds, Component c) {
        this(ds, c, DnDConstants.ACTION_NONE, null);
    }

    protected DragGestureRecognizer(DragSource ds) {
        this(ds, null, DnDConstants.ACTION_NONE, null);
    }

    public DragSource getDragSource() {
        return dragSource;
    }

    public synchronized Component getComponent() {
        return component;
    }

    public synchronized void setComponent(Component c) {
        unregisterListeners();
        component = c;
        if (dragGestureListener != null) {
            registerListeners();
        }
    }

    public synchronized int getSourceActions() {
        return sourceActions;
    }

    public synchronized void setSourceActions(int actions) {
        sourceActions = actions;
    }

    protected synchronized void appendEvent(InputEvent awtie) {
        if (awtie == null) {
            return;
        }

        if (events == null) {
            events = new ArrayList<InputEvent>();
        }

        events.add(awtie);
    }

    public InputEvent getTriggerEvent() {
        if ((events == null) || events.isEmpty()) {
            return null;
        }

        return events.get(0);
    }

    public void resetRecognizer() {
        events.clear();
    }

    public synchronized void addDragGestureListener(DragGestureListener dgl)
            throws TooManyListenersException {
        if (dgl == null) {
            return;
        }
        if (dragGestureListener != null) {
            // awt.173=One listener is already exist.
            throw new TooManyListenersException(Messages.getString("awt.173")); //$NON-NLS-1$
        }

        dragGestureListener = dgl;
        registerListeners();
    }

    public synchronized void removeDragGestureListener(DragGestureListener dgl) {
        if (dragGestureListener != dgl) {
            // awt.174=dgl is not current listener.
            throw new IllegalArgumentException(Messages.getString("awt.174")); //$NON-NLS-1$
        }
        if (dragGestureListener == null) {
            return;
        }

        unregisterListeners();
        dragGestureListener = null;
    }

    protected synchronized void fireDragGestureRecognized(int dragAction, Point p) {
        if (dragGestureListener != null) {
            // Supposed that we are on event dispatch thread
            try {
                DragGestureEvent event = new DragGestureEvent(this, dragAction, p, events);

                dragGestureListener.dragGestureRecognized(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        resetRecognizer();
    }

    protected abstract void registerListeners();

    protected abstract void unregisterListeners();

}
