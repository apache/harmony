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
 * @author Michael Danilov, Pavel Dolgov
 */
package java.awt;

import java.awt.event.InvocationEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.EmptyStackException;

public class EventQueue {
    
    private final EventQueueCoreAtomicReference coreRef = 
            new EventQueueCoreAtomicReference();
    
    private static final class EventQueueCoreAtomicReference {
        private EventQueueCore core;

        /*synchronized*/ EventQueueCore get() { 
            return core;
        }

        /*synchronized*/ void set(EventQueueCore newCore) { 
            core = newCore;
        }
    }

    public static boolean isDispatchThread() {
        return Thread.currentThread() instanceof EventDispatchThread;
    }

    public static void invokeLater(Runnable runnable) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        InvocationEvent event = new InvocationEvent(toolkit, runnable);
        toolkit.getSystemEventQueueImpl().postEvent(event);
    }

    public static void invokeAndWait(Runnable runnable)
            throws InterruptedException, InvocationTargetException {

        if (isDispatchThread()) {
            throw new Error();
        }

        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        final Object notifier = new Object();  //$NON-LOCK-1$
        InvocationEvent event = new InvocationEvent(
                toolkit, runnable, notifier, true);

        synchronized (notifier) {
            toolkit.getSystemEventQueueImpl().postEvent(event);
            notifier.wait();
        }

        Exception exception = event.getException();

        if (exception != null) {
            throw new InvocationTargetException(exception);
        }
    }

    private static EventQueue getSystemEventQueue() {
        Thread th = Thread.currentThread();
        if (th instanceof EventDispatchThread) {
            return ((EventDispatchThread)th).toolkit.getSystemEventQueueImpl();
        }
        return null;
    }
    
    public static long getMostRecentEventTime() {
        EventQueue eq = getSystemEventQueue();
        return (eq != null) ? 
                eq.getMostRecentEventTimeImpl() : System.currentTimeMillis();
    }
    
    private long getMostRecentEventTimeImpl() {
        return getCore().getMostRecentEventTime();
    }

    public static AWTEvent getCurrentEvent() {
        EventQueue eq = getSystemEventQueue();
        return (eq != null) ? 
                eq.getCurrentEventImpl() : null;
    }

    private AWTEvent getCurrentEventImpl() {
        return getCore().getCurrentEvent();
    }

    public EventQueue() {
        setCore(new EventQueueCore(this));
    }

    EventQueue(Toolkit t) {
        setCore(new EventQueueCore(this, t));
    }

    public void postEvent(AWTEvent event) {
        event.isPosted = true;
        getCore().postEvent(event);
    }

    public AWTEvent getNextEvent() throws InterruptedException {
        return getCore().getNextEvent();
    }
    
    AWTEvent getNextEventNoWait() {
        return getCore().getNextEventNoWait();
    }

    public AWTEvent peekEvent() {
        return getCore().peekEvent();
    }

    public AWTEvent peekEvent(int id) {
        return getCore().peekEvent(id);
    }

    public void push(EventQueue newEventQueue) {
        getCore().push(newEventQueue);
    }
    
    protected void pop() throws EmptyStackException {
        getCore().pop();
    }

    protected void dispatchEvent(AWTEvent event) {
        getCore().dispatchEventImpl(event);
    }

    boolean isEmpty() {
        return getCore().isEmpty();
    }

    EventQueueCore getCore() {
        return coreRef.get();
    }
    
    void setCore(EventQueueCore newCore) {
        coreRef.set((newCore != null) ? newCore : new EventQueueCore(this));
    }
}
