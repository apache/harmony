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

package java.awt;

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.MouseDragGestureRecognizer;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

class DefaultMouseDragGestureRecognizer extends MouseDragGestureRecognizer {
    private static final long serialVersionUID = 1L;

    private boolean active;

    protected DefaultMouseDragGestureRecognizer(DragSource ds, Component c, int act,
            DragGestureListener dgl) {
        super(ds, c, act, dgl);
        events = new ArrayList<InputEvent>();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        events.add(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (active) {
            return;
        }
        events.add(e);
        int distance = 2; // TODO: use desktop property 
        MouseEvent e0 = (MouseEvent) events.get(0);
        if (e0.getPoint().distance(e.getPoint()) >= distance) {
            if (sourceActions != DnDConstants.ACTION_NONE) {
                active = true;
                fireDragGestureRecognized(sourceActions, e.getPoint());
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        active = false;
    }
}
