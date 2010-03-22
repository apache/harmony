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

import java.awt.Component;
import java.awt.event.*;

public abstract class MouseDragGestureRecognizer extends DragGestureRecognizer
        implements MouseListener, MouseMotionListener
{

    private static final long serialVersionUID = 6220099344182281120L;

    protected MouseDragGestureRecognizer(
            DragSource ds, Component c, int act, DragGestureListener dgl)
    {
        super(ds, c, act, dgl);
    }

    protected MouseDragGestureRecognizer(DragSource ds, Component c, int act) {
        super(ds, c, act);
    }

    protected MouseDragGestureRecognizer(DragSource ds, Component c) {
        super(ds, c);
    }

    protected MouseDragGestureRecognizer(DragSource ds) {
        super(ds);
    }

    @Override
    protected void registerListeners() {
        if (component != null) {
            component.addMouseListener(this);
            component.addMouseMotionListener(this);
        }
    }

    @Override
    protected void unregisterListeners() {
        if (component != null) {
            component.removeMouseListener(this);
            component.removeMouseMotionListener(this);
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

}
