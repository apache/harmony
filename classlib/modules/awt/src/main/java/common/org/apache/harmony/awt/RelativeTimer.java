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
package org.apache.harmony.awt;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.harmony.awt.internal.nls.Messages;

/**
 * Relative timer class. Basic class for PeriodicTimer and SingleShotTimer.
 * "Relative" means that there is no binding with absolute time.
 * All future events are planned relative to current time.
 * All timers live on one thread.
 */
public abstract class RelativeTimer {

    private static final DeltaList deltaList = new DeltaList();

    final long interval;
    final Runnable handler;

    private DeltaListEntry deltaEntry;

    RelativeTimer(long interval, Runnable handler) {
        if (interval <= 0) {
            // awt.52=Time interval can't be <= 0
            throw new IllegalArgumentException(Messages.getString("awt.52")); //$NON-NLS-1$
        }
        if (handler == null) {
            // awt.53=Handler can't be null
            throw new IllegalArgumentException(Messages.getString("awt.53")); //$NON-NLS-1$
        }

        this.interval = interval;
        this.handler = handler;
        deltaEntry = null;
    }

    /**
     * Gets handler for this timer's events.
     *
     * @return this timer's handler.
     */
    public Runnable getHandler() {
        return handler;
    }

    /**
     * Starts ticking of this timer.
     * Time when timer's handler is invoked first time is following:
     * current_time + interval.
     */
    public void start() {
        synchronized (deltaList) {
            if (isRunning()) {
                return;
            }

            deltaEntry = new DeltaListEntry();
            deltaList.add(deltaEntry);
        }
    }

    /**
     * Stops ticking of this timer.
     */
    public void stop() {
        synchronized (deltaList) {
            if (!isRunning()) {
                return;
            }

            deltaList.remove(deltaEntry);
            deltaEntry = null;
        }
    }

    /**
     * Returns true if this timer is ticking.
     */
    public boolean isRunning() {
        return (deltaEntry != null);
    }

    void handle() {
        handler.run();
    }

    private class DeltaListEntry {

        long time;
        boolean stopped;

        DeltaListEntry() {
            charge();
            stopped = false;
        }

        void handle() {
            RelativeTimer.this.handle();
            if (!stopped) {
                charge();
            }
        }

        private void charge() {
            time = System.currentTimeMillis() + interval;
        }

    }

    private static class DeltaList {

        private final LinkedList<DeltaListEntry> list;
        private DeltaThread thread;
        private long time;

        DeltaList() {
            list = new LinkedList<DeltaListEntry>();
            time = 0;
            thread = null;
        }

        void add(DeltaListEntry entry) {
            if (list.isEmpty()) {
                time = Long.MAX_VALUE;
                insert(entry, false);
                thread = new DeltaThread();
                thread.setName("Relative Timer"); //$NON-NLS-1$
                thread.setDaemon(true);
                thread.start();
            } else {
                insert(entry, true);
            }
        }

        void remove(DeltaListEntry entry) {
            if (Thread.currentThread() == thread) {
                if (list.contains(entry)) {
                    removeNotLast(entry);
                } else {
                    entry.stopped = true;
                }
            } else {
                if (list.size() == 1) {
                    list.clear();
                    thread.interrupt();
                } else {
                    removeNotLast(entry);
                }
            }
        }

        private void removeNotLast(DeltaListEntry entry) {
            int index = list.indexOf(entry);

            list.remove(entry);
            if (index != list.size()) {
                if (index == 0) {
                    firstRemoved();
                } else {
                    list.get(index).time += entry.time;
                }
            }
        }

        private void loop() {
            synchronized (this) {
                while (!list.isEmpty()) {
                    long delay = time - System.currentTimeMillis();

                    if (delay > 0) {
                        try {
                            wait(delay);
                        } catch (InterruptedException e) {
                            if (list.isEmpty()) {
                                break;
                            }
                        }
                    }

                    handle();
                }
            }
            time = 0;
            thread = null;
        }

        private void handle() {
            long curTime = System.currentTimeMillis();

            if (time <= curTime) {
                while (list.getFirst().time <= curTime) {
                    DeltaListEntry first = list.removeFirst();

                    if (!list.isEmpty()) {
                        firstRemoved();
                    }
                    try {
                        first.handle();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    if (!first.stopped) {
                        insert(first, false);
                    } else if (list.isEmpty()) {
                        break;
                    }
                }
            }
        }

        private void firstRemoved() {
            DeltaListEntry nextFirst = list.getFirst();

            time += nextFirst.time;
            nextFirst.time = time;
        }

        private void insert(DeltaListEntry entry, boolean interrupt) {
            long oldTime = list.isEmpty() ? Long.MAX_VALUE : time;
            Iterator<DeltaListEntry> i = list.iterator();

            do {
                if (!i.hasNext()) {
                    list.addLast(entry);
                    break;
                }
                DeltaListEntry next = i.next();

                if (next.time <= entry.time) {
                    entry.time -= next.time;
                } else {
                    next.time -= entry.time;
                    list.add(list.indexOf(next), entry);
                    break;
                }
            } while (true);

            time = list.getFirst().time;
            if ((time < oldTime) && interrupt) {
                thread.interrupt();
            }
        }

        private class DeltaThread extends Thread {
            @Override
            public void run() {
                loop();
            }
        };
    }
}
