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
package org.apache.harmony.awt.datatransfer.linux;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.peer.DragSourceContextPeer;

public class LinuxDragSource implements DragSourceContextPeer {

    private Cursor cursor;
    private DragSourceContext context;
    
    public Cursor getCursor() {
        synchronized (this) {
            return cursor;
        }
    }

    public void setCursor(Cursor c) throws InvalidDnDOperationException {
        synchronized (this) {
            cursor = c;
        }
    }

    public void startDrag(DragSourceContext dsc, Cursor c, Image di, Point ioff)
            throws InvalidDnDOperationException {
        synchronized (this) {
            context = dsc;
            cursor = c;
        }

    }

    public void transferablesFlavorsChanged() {
    }

}
