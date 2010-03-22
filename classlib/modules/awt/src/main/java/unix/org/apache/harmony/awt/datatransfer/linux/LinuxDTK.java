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
package org.apache.harmony.awt.datatransfer.linux;

import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.dnd.peer.DropTargetContextPeer;

import org.apache.harmony.awt.datatransfer.DTK;
import org.apache.harmony.awt.datatransfer.NativeClipboard;

public final class LinuxDTK extends DTK {

    protected NativeClipboard newNativeClipboard() {
        return new LinuxSelection("CLIPBOARD"); //$NON-NLS-1$
    }

    protected NativeClipboard newNativeSelection() {
        return new LinuxSelection("PRIMARY"); //$NON-NLS-1$
    }

    public void initDragAndDrop() {
    }

    public void runEventLoop() {
    }

    public DropTargetContextPeer createDropTargetContextPeer(
            DropTargetContext context) {
        return new LinuxDropTarget(this, context);
    }

    public DragSourceContextPeer createDragSourceContextPeer(
            DragGestureEvent dge) {
        return new LinuxDragSource();
    }

    public String getDefaultCharset() {
        return "iso-10646-ucs-2"; //$NON-NLS-1$
    }
}
