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

final class HeadlessEventDispatchThread extends EventDispatchThread  {

    HeadlessEventDispatchThread(Toolkit toolkit, Dispatcher dispatcher ) {
        super(toolkit, dispatcher);
    }

    /**
     * Initialise and run the main event loop
     */
    @Override
    public void run() {
        try {
            runModalLoop(null);
        } finally {
            toolkit.shutdownWatchdog.forceShutdown();
        }
    }
    
    @Override
    void runModalLoop(ModalContext context) {
        long lastPaintTime = System.currentTimeMillis();
        while (!shutdownPending && (context == null || context.isModalLoopRunning())) {
            try {
                EventQueue eventQueue = toolkit.getSystemEventQueueImpl();
    
                toolkit.shutdownWatchdog.setNativeQueueEmpty(true);
                AWTEvent ae = eventQueue.getNextEventNoWait();
                if (ae != null) {
                    eventQueue.dispatchEvent(ae);
                    long curTime = System.currentTimeMillis();
                    if (curTime - lastPaintTime > 10) {
                        toolkit.onQueueEmpty();
                        lastPaintTime = System.currentTimeMillis();
                    }
                } else {
                    toolkit.shutdownWatchdog.setAwtQueueEmpty(true);
                    toolkit.onQueueEmpty();
                    lastPaintTime = System.currentTimeMillis();
                    waitForAnyEvent();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }   

    private void waitForAnyEvent() {
        EventQueue eventQueue = toolkit.getSystemEventQueueImpl();
        if (!eventQueue.isEmpty()) {
            return;
        }
        
        Object eventMonitor = toolkit.getEventMonitor();
        synchronized(eventMonitor) {
            try {
                eventMonitor.wait();
            } catch (InterruptedException e) {}
        }
    }

}
