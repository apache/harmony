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
 * @author Dmitry A. Durnev
 */
package org.apache.harmony.awt.wtk.windows;

import org.apache.harmony.awt.wtk.NativeCursor;

public class WinCursor implements NativeCursor {
    final long hCursor;

    final WinEventQueue eventQueue;

  /*is this a system cursor?(such cursors are shared and can't be destroyed
          by user*/
    final boolean system;

    WinCursor(WinEventQueue eventQueue, final long handle, final boolean system) {
        hCursor = handle;
        this.system = system;
        this.eventQueue = eventQueue;
    }

    WinCursor(WinEventQueue eventQueue, final long handle) {
        this(eventQueue, handle, true); //create system(predefined) cursors by default
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeCursor#setCursor()
     */
    public void setCursor(long winID) {
        WinEventQueue.Task task = new WinEventQueue.Task() {
            @Override
            public void perform() {
                WinEventQueue.win32.SetCursor(hCursor);
            }
        };
        eventQueue.performTask(task);
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeCursor#destroyCursor()
     */
    public void destroyCursor() {
        if (!system) {
            WinEventQueue.win32.DestroyCursor(hCursor);
        }
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeCursor#getId()
     */
    public long getId() {
        return hCursor;
    }

}
