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

import java.awt.event.InputEvent;

public class DragSourceDragEvent extends DragSourceEvent {

    private static final long serialVersionUID = 481346297933902471L;
    private int userAction;
    private int targetAction;
    private int modifiers;
    private int modifiersEx;

    public DragSourceDragEvent(DragSourceContext dsc, int dropAction, int action, int modifiers) {
        super(dsc);

        initFields(dropAction, action, modifiers);
    }

    public DragSourceDragEvent(DragSourceContext dsc, int dropAction, int action,
            int modifiers, int x, int y)
    {
        super(dsc, x, y);

        initFields(dropAction, action, modifiers);
    }

    private void initFields(int dropAction, int action, int modifiers) {
        userAction = dropAction;
        targetAction = action;
        this.modifiers = getInputModifiers(modifiers);
        this.modifiersEx = modifiers;
    }

    public int getUserAction() {
        return userAction;
    }

    public int getTargetActions() {
        return targetAction;
    }

    public int getGestureModifiersEx() {
        return modifiersEx;
    }

    public int getGestureModifiers() {
        return modifiers;
    }

    public int getDropAction() {
        return (userAction & targetAction & getDragSourceContext().getSourceActions());
    }

    private int getInputModifiers(int modifiersEx) {
        // TODO: share this code with KeyEvent.getModifiers()
        int modifiers = 0;

        if ((modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0) {
            modifiers |= InputEvent.SHIFT_MASK;
        }
        if ((modifiersEx & InputEvent.CTRL_DOWN_MASK) != 0) {
            modifiers |= InputEvent.CTRL_MASK;
        }
        if ((modifiersEx & InputEvent.META_DOWN_MASK) != 0) {
            modifiers |= InputEvent.META_MASK;
        }
        if ((modifiersEx & InputEvent.ALT_DOWN_MASK) != 0) {
            modifiers |= InputEvent.ALT_MASK;
        }
        if ((modifiersEx & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
            modifiers |= InputEvent.ALT_GRAPH_MASK;
        }
        if ((modifiersEx & InputEvent.BUTTON1_DOWN_MASK) != 0) {
            modifiers |= InputEvent.BUTTON1_MASK;
        }
        if ((modifiersEx & InputEvent.BUTTON2_DOWN_MASK) != 0) {
            modifiers |= InputEvent.BUTTON2_MASK;
        }
        if ((modifiersEx & InputEvent.BUTTON3_DOWN_MASK) != 0) {
            modifiers |= InputEvent.BUTTON3_MASK;
        }

        return modifiers;
    }
}
