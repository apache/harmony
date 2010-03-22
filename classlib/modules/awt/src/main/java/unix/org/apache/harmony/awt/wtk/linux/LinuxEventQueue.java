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
package org.apache.harmony.awt.wtk.linux;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.nativebridge.CLongPointer;
import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.X11Defs;
import org.apache.harmony.awt.wtk.NativeEvent;
import org.apache.harmony.awt.wtk.NativeEventQueue;
import org.apache.harmony.misc.accessors.ObjectAccessor;
import org.apache.harmony.misc.accessors.AccessorFactory;

public final class LinuxEventQueue extends NativeEventQueue {
    
    public interface Preprocessor {
        public boolean preprocess(X11.XEvent event);
    }

    private static final X11 x11 = X11.getInstance();
    private static final ObjectAccessor objAccessor = 
            AccessorFactory.getObjectAccessor();

    final LinuxWindowFactory factory;
    final long display;
    
    private final long performTaskAtom;

    private final LinkedList preprocessors = new LinkedList();
    private final X11.XEvent curEvent;
    /** Pending areas to be painted: Window ID (long) --&gt; MultiRectArea */
    private final HashMap accumulatedClipRegions = new HashMap();

    LinuxEventQueue(LinuxWindowFactory factory) {
        this.factory = factory;
        display = factory.getDisplay();
        curEvent = x11.createXEvent(false);
        performTaskAtom = x11.XInternAtom(display, 
                "org.apache.harmony.awt.wtk.perform_task", 0); //$NON-NLS-1$
    }
    
    public boolean waitEvent() {
        do {
            x11.XNextEvent(display, curEvent);
        } while (preprocessEvent(curEvent));

        return true;
    }

    public void awake() {
        X11.XEvent event = x11.createXEvent(false);

        event.set_type(X11Defs.MapNotify);
        event.get_xany().set_window(getJavaWindow());
        x11.XSendEvent(display, getJavaWindow(), 0, X11Defs.StructureNotifyMask, event);
        x11.XFlush(display);
    }

    public long getJavaWindow() {
        return factory.getJavaWindow();
    }

    public void dispatchEvent() {
        enqueue(curEvent);
    }
    
    public void addPreprocessor(Preprocessor preprocessor) {
        preprocessors.add(preprocessor);
    }
    
    private void enqueue(X11.XEvent xevent) {
        LinuxEvent event = new LinuxEvent(factory, this, xevent);
        int eventId = event.getEventId();
        if (eventId != NativeEvent.ID_PLATFORM) {
            addEvent(event);
        }
    }

    private boolean preprocessEvent(X11.XEvent event) {
        for (Iterator i = preprocessors.iterator(); i.hasNext(); ) {
            if (((Preprocessor) i.next()).preprocess(event)) {
                return true;
            }
        }
        if (event.get_type() == X11Defs.ClientMessage) {
            return preprocessClientEvent(event.get_xclient());
        }
        return false;
    }

    MultiRectArea getAccumulatedClip(long windowId) {
        Long id = new Long(windowId);
        MultiRectArea clip = (MultiRectArea)accumulatedClipRegions.get(id);
        if (clip == null) {
            clip = new MultiRectArea();
            accumulatedClipRegions.put(id, clip);
        }
        return clip;
    }

    void resetAccumulatedClip(long windowId) {
        accumulatedClipRegions.remove(new Long(windowId));
    }

    public void performTask(Task task) {
        X11.XEvent e = createPerformTaskEvent(task, true);
        synchronized (task) {
            x11.XSendEvent(display, getJavaWindow(), 0, 0, e);
            x11.XFlush(display);
            try {
               task.wait();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    public void performLater(Task task) {
        X11.XEvent e = createPerformTaskEvent(task, true);
        x11.XSendEvent(display, getJavaWindow(), 0, 0, e);
        x11.XFlush(display);
    }
    
    X11.XEvent createPerformTaskEvent(Task task, boolean wait) {
        X11.XEvent e = x11.createXEvent(false);
        long ref = objAccessor.getGlobalReference(task);
        e.set_type(X11Defs.ClientMessage);
        X11.XClientMessageEvent xclient = e.get_xclient();
        xclient.set_message_type(performTaskAtom);
        CLongPointer data = xclient.get_l();
        data.set(0, (ref & 0xFFFFFFFF));
        data.set(1, (ref >> 32));
        data.set(2, wait ? 1 : 0);
        return e;
    }

    private boolean preprocessClientEvent(X11.XClientMessageEvent xclient) {
        if (xclient.get_message_type() == performTaskAtom) {
            performTask(xclient);
            return true;
        } 
        return false;
    }

       private void performTask(X11.XClientMessageEvent xclient) {
        CLongPointer data = xclient.get_l();
        
        long ref = (data.get(0) & 0xFFFFFFFF) | (data.get(1) << 32);
        boolean wait = (data.get(2) != 0);
               
        Task t = (Task)objAccessor.getObjectFromReference(ref);
        if (wait) {
            synchronized(t) {
                t.perform();
                t.notify();
            }
        } else {
            t.perform();
        }
        objAccessor.releaseGlobalReference(ref);
       }
}
