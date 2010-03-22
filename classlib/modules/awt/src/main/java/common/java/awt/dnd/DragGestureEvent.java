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
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import org.apache.harmony.awt.internal.nls.Messages;

public class DragGestureEvent extends EventObject {

    private static final long serialVersionUID = 9080172649166731306L;

    private final DragGestureRecognizer recognizer;

    private final Point origin;

    private final List<InputEvent> eventList;

    private final int action;

    @SuppressWarnings("unchecked")
    public DragGestureEvent(DragGestureRecognizer dgr, int act, Point ori,
            List<? extends InputEvent> evs) {
        super(dgr);

        if (dgr.getComponent() == null) {
            // awt.185=Component is null.
            throw new IllegalArgumentException(Messages.getString("awt.185")); //$NON-NLS-1$
        }
        if (dgr.getDragSource() == null) {
            // awt.186=DragSource is null.
            throw new IllegalArgumentException(Messages.getString("awt.186")); //$NON-NLS-1$
        }
        if (!DnDConstants.isValidAction(act)) {
            // awt.184=Invalid action.
            throw new IllegalArgumentException(Messages.getString("awt.184")); //$NON-NLS-1$
        }
        if (ori == null) {
            // awt.187=Origin is null.
            throw new IllegalArgumentException(Messages.getString("awt.187")); //$NON-NLS-1$
        }
        if (evs == null) {
            // awt.188=Event list is null.
            throw new IllegalArgumentException(Messages.getString("awt.188")); //$NON-NLS-1$
        }
        if (evs.isEmpty()) {
            // awt.189=Event list is empty.
            throw new IllegalArgumentException(Messages.getString("awt.189")); //$NON-NLS-1$
        }

        recognizer = dgr;
        action = act;
        origin = ori;
        eventList = (List<InputEvent>) evs;
    }

    public DragSource getDragSource() {
        return recognizer.getDragSource();
    }

    public DragGestureRecognizer getSourceAsDragGestureRecognizer() {
        return recognizer;
    }

    public Point getDragOrigin() {
        return new Point(origin);
    }

    public Component getComponent() {
        return recognizer.getComponent();
    }

    public int getDragAction() {
        return action;
    }

    public Object[] toArray(Object[] array) {
        return eventList.toArray(array);
    }

    public Object[] toArray() {
        return eventList.toArray();
    }

    public Iterator<InputEvent> iterator() {
        return eventList.iterator();
    }

    public InputEvent getTriggerEvent() {
        return recognizer.getTriggerEvent();
    }

    public void startDrag(Cursor dragCursor, Transferable transferable)
            throws InvalidDnDOperationException {
        DragSourceListener[] listeners = recognizer.dragSource.getDragSourceListeners();
        DragSourceListener dsl = listeners.length > 0 ? new DragSourceMulticaster(listeners)
                : null;
        startDrag(dragCursor, transferable, dsl);
    }

    public void startDrag(Cursor dragCursor, Image dragImage, Point imageOffset,
            Transferable transferable, DragSourceListener dsl)
            throws InvalidDnDOperationException {

        recognizer.getDragSource().startDrag(this, dragCursor, dragImage, imageOffset,
                transferable, dsl);
    }

    public void startDrag(Cursor dragCursor, Transferable transferable, DragSourceListener dsl)
            throws InvalidDnDOperationException {

        recognizer.getDragSource().startDrag(this, dragCursor, transferable, dsl);
    }

}
