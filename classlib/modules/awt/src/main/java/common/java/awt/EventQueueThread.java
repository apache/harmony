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
package java.awt;

import java.awt.event.ActionEvent;

/**
 * Thread for dispatching events in non-system EventQueue
 */
final class EventQueueThread extends Thread {

    private static final class StopThreadEvent extends ActionEvent {
        private static final long serialVersionUID = 1L;

        StopThreadEvent(Thread thread) {
            super(thread, ACTION_PERFORMED, "stopThread"); //$NON-NLS-1$
        }
    }

    private final EventQueueCore core;

    EventQueueThread(EventQueueCore core) {
        super("AWT-EventQueueThread"); //$NON-NLS-1$
        this.core = core;
        setDaemon(false);
    }

    @Override
    public void run() {
        while (!core.isEmpty(1000)) {
            try {
                AWTEvent e = core.getActiveEventQueue().getNextEvent();
                if ((e instanceof StopThreadEvent) && e.getSource() == this) {
                    return;
                }
                core.dispatchEvent(e);
            } catch (InterruptedException e) {
                // do nothing
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void postStopEvent() {
        core.postEvent(new StopThreadEvent(this));
    }
}
