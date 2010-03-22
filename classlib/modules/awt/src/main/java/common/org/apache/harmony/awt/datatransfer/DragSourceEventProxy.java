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
package org.apache.harmony.awt.datatransfer;

import java.awt.Point;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;

/**
 * Dispatches DragSource events on event dispatch thread 
 * in conjunction with {@link java.awt.EventQueue#invokeLater(Runnable)}
 */
public class DragSourceEventProxy implements Runnable {
    public static final int DRAG_ENTER = 1;
    public static final int DRAG_OVER = 2;
    public static final int DRAG_ACTION_CHANGED = 3;
    public static final int DRAG_MOUSE_MOVED = 4;
    public static final int DRAG_EXIT = 5;
    public static final int DRAG_DROP_END = 6;
    
    private final DragSourceContext context;
    
    private final int type;
    private final int userAction;
    private final int targetActions;
    private final int x;
    private final int y;
    private final int modifiers;
    private final boolean success;
    
    public DragSourceEventProxy(DragSourceContext context, 
            int type, int userAction, int targetActions,
            Point location, int modifiers) {
        this.context = context;
        this.type = type;
        this.userAction = userAction;
        this.targetActions = targetActions;
        this.x = location.x;
        this.y = location.y;
        this.modifiers = modifiers;
        this.success = false;
    }
    
    public DragSourceEventProxy(DragSourceContext context,
            int type, int userAction, boolean success,
            Point location, int modifiers) {
        this.context = context;
        this.type = type;
        this.userAction = userAction;
        this.targetActions = userAction;
        this.x = location.x;
        this.y = location.y;
        this.modifiers = modifiers;
        this.success = success;
    }
    
    public void run() {
        switch (type) {
        case DRAG_ENTER:
            context.dragEnter(newDragSourceDragEvent());
            break;
        case DRAG_OVER:
            context.dragOver(newDragSourceDragEvent());
            break;
        case DRAG_ACTION_CHANGED:
            context.dropActionChanged(newDragSourceDragEvent());
            break;
        case DRAG_MOUSE_MOVED:
            context.dragMouseMoved(newDragSourceDragEvent());
            break;
        case DRAG_EXIT:
            context.dragExit(new DragSourceEvent(context, x, y));
            break;
        case DRAG_DROP_END:
            context.dragExit(new DragSourceDropEvent(
                    context, userAction, success, x, y));
            break;
        }
    }
    
    private DragSourceDragEvent newDragSourceDragEvent() {
        return new DragSourceDragEvent(
                context, userAction, targetActions, modifiers, x, y);
    }
}