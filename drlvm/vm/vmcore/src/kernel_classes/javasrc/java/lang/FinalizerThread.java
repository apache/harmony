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
/*
 * @author Salikh Zakirov, Pavel Afremov
 */
package java.lang;

/**
 * Dedicated finalizer thread.
 */
class FinalizerThread extends Thread {
    
    /**
     * Wake up permanent finalizer threads or create additional 
     * temporary threads, and wait until they start.
     * @param wait - says to wait untill all finalizer threads complite work. 
     */ 
    public static void startFinalization(boolean wait) {
        if (!enabled) {
            return;
        }

        //wakeup finalazer threads
        wakeupFinalization();

        // work balance system creates balancing threads
        spawnBalanceThreads();

        // if flag is raised up waits finalization complete
        if (wait) {
            waitFinalizationEnd();
        }
    }

    /**
     * VM calls this thread from Runtime.runFinalization().
     */
    public static void runFinalization() {
        /* BEGIN: added for NATIVE FINALIZER THREAD */
        if(NATIVE_FINALIZER_THREAD)
            runFinalizationInNativeFinalizerThreads();
        else
        /* END: added for NATIVE FINALIZER THREAD */
            startFinalization(true);
    }

    /*
     * Staic package private part
     */

    /**
     * Initializes finalization system. Starts permanent thread.
     */
    static void initialize() {
        /* BEGIN: added for NATIVE FINALIZER THREAD */
        if (NATIVE_FINALIZER_THREAD)
            return;
        /* END: added for NATIVE FINALIZER THREAD */
        
        if (TRACE) {
            trace("FinalizerThread: static initialization started");
        }

        processorsQuantity = getProcessorsQuantity();

        // -XDvm.finalize=0 disables the finalizer thread
        if (! isNativePartEnabled()) {
            warn("finalizer thread have not been created");
        } else {
            (new FinalizerThread(true)).start();
            enabled = true;
        }

        if (TRACE) {
            trace("FinalizerThread: static initialization complete");
        }
    }

    /**
     * VM calls this method to request finalizer thread shutdown.
     */
    static void shutdown(boolean startFinalizationOnExit) {
        /* BEGIN: added for NATIVE FINALIZER THREAD */
        if(NATIVE_FINALIZER_THREAD) {
            finalizerShutDown(startFinalizationOnExit);
            return;
        }
        /* END: added for NATIVE FINALIZER THREAD */
        if (TRACE) {
            trace("shutting down finalizer thread");
        }

        if (startFinalizationOnExit) {
            doFinalizationOnExit();
        }
        
        synchronized (workLock) {
            shutdown = true;
            workLock.notifyAll();
        }
    }

    /* added for NATIVE FINALIZER THREAD
     * A flag to indicate whether the finalizer threads are native threads or Java threads.
     */
    private static final boolean NATIVE_FINALIZER_THREAD = getNativeFinalizerThreadFlagFromVM();

    /* BEGIN: These three methods are added for NATIVE FINALIZER THREAD */
    /**
     * This method gets the flag that indicates
     * whether VM uses native finalizer threads or Java finalizer threads.
     */
    private static native boolean getNativeFinalizerThreadFlagFromVM();
    
    /**
     * This method implements runFinalization() method in native finalizer threads.
     */
    private static native void runFinalizationInNativeFinalizerThreads();
    
    /**
     * This method does finalization work related to VM shutdown in native finalizer threads.
     */
    private static native void finalizerShutDown(boolean finalizeOnExit);
    /* END: These three methods are added for NATIVE FINALIZER THREAD */
    
    /*
     * Staic private part
     */
        
    // Maximum quantity of finalizers threads
    private static final int MAX_THREADS = 256;

    // create separate class for finalizer workLock to easier debugging
    private static class FinalizerWorkLock {};

    // Lock used to wake up permanent finalizer threads and synchronize change of state of work
    private static Object workLock = new FinalizerWorkLock();

    // Shows that finalizers works in state on exit
    // Used by VM. It should be package private to eliminate compiler warning.
    static boolean onExit = false;

    // create separate class for finalizer waitFinishLock to easier debugging
    private static class FinalizerWaitFinishLock {};

    /*
     * Lock used to to synchronize quantity of active threads and to wake up finalizer starter thread
     * when new finalization tasks aren't available and finalizer threads finish work.
     */
    private static Object waitFinishLock = new FinalizerWaitFinishLock();

    /*
     * The Quantity of active finalizer threads which shoud be stopped to wake up finalizer starter
     * thread. When is thread is started this counter is incremented when thread is stoppig it's decremeted.
     * Zero means finalization tasks aren't available and finalizer threads aren't working.
     */
    private static int waitFinishCounter = 0;

    // Indicates processors quantity in the system
    private static int processorsQuantity;

    // true means finalizer threads is enabled
    private static boolean enabled = false;

    // true means finalizer threads need to shut down
    private static boolean shutdown = false;

    /**
     * Gets quantity of processors in the System
     */
    private static native int getFinalizersQuantity();

    /**
     * Gets quantity of processors in the System
     */
    private static native int getProcessorsQuantity();

    /**
     * Do finalizations in native mode for specified quantity of finalizable object
     */
    private static native int doFinalization(int quantity);
    
    private static native void fillFinalizationQueueOnExit();

    /**
     * Returns true if native part of finalization system is
     * turned on, and false otherwise.
     */
    private static native boolean isNativePartEnabled();

    /**
     * Returns true if current thread is finalizer thread
     */
    private static boolean isFinalizerThread() {
        return ((Thread.currentThread()) instanceof FinalizerThread);
    }
    
    /**
     * Wakes up permanent finalizer threads
     */
    private static void wakeupFinalization() {
        synchronized (workLock) {
            workLock.notifyAll();
        }
    }

    /**
     * Waits when finalization work is completed and then returns.
     */
    private static void waitFinalizationEnd() {
        if (isFinalizerThread()) {
            return;
        }
        
        synchronized (waitFinishLock) {
            if (waitFinishCounter > 0) {
                try {
                    waitFinishLock.wait();
                } catch (InterruptedException e) {}
            }
        }
    }

    /**
     * Starts additional temporary threads to make sure
     * finalization system keeps number of unfinalized objects
     * at acceptable level.
     * Waits until created thraed starts and then return.
     * It is called from startFinalization() only.
     */
    private static void spawnBalanceThreads() {
        /* finalizer threads shouldn't be spawn by finalizer thread,
         * in this case balancing can't work
         */
        if (isFinalizerThread()) {
            return;
        }

        FinalizerThread newThread = null;
        if (waitFinishCounter >= MAX_THREADS) {
			Thread.yield();
        } else {
            try {
                for (int i = 0; i < processorsQuantity; i++) {
                    newThread = new FinalizerThread(false);

                    synchronized (newThread.startLock){
                        newThread.start();

                        // waiting when new thread will be started
                        try {
                            newThread.startLock.wait();
                        } catch (InterruptedException e) {}
                    }
                }
            } catch (OutOfMemoryError e) {}
        }
    } 

    private static void doFinalizationOnExit() {
        System.gc();
        startFinalization(true);

        fillFinalizationQueueOnExit();
        
        synchronized (workLock) {
            onExit = true;
        }
        startFinalization(true);
    }
    
    /**
     * Waits when new finalization task are available and finalizer threads can continue work
     */
    private static void waitNewTask() {
        if (getFinalizersQuantity() != 0) {
            return;
        }

        synchronized (workLock) {
            synchronized (waitFinishLock) {
                waitFinishCounter --;

                if (waitFinishCounter == 0) {
                    waitFinishLock.notifyAll();
                }
            }

            while ((getFinalizersQuantity() == 0) && (!shutdown)) {
                try {
                    workLock.wait();
                } catch (InterruptedException e) {}
            }
            
            synchronized (waitFinishLock) {
                waitFinishCounter ++;
            }
        }
    }

    private static final boolean TRACE = false;

    /**
     * debug output.
     */
    private static void trace (Object o) {
        /*
        System.err.println(o);
        System.err.flush();
        */
    }

    /**
     * Prints warning.
     */
    private static void warn (Object o) {
        System.err.println("FinalizerThread: " + o);
        System.err.flush();
    }

    public FinalizerThread () {
        this (true);
    }

    // create separate class for finalizer startLock to easier debugging
    private class FinalizerStartLock {};

    private FinalizerStartLock startLock = new FinalizerStartLock();

    protected FinalizerThread (boolean permanent) {
        super(Thread.systemThreadGroup, "FinalizerThread");
        this.permanent = permanent;
        this.setDaemon(true);
    }

    public void run() {
        // don't put any code here, before try block

        try {
            synchronized (waitFinishLock) {
                waitFinishCounter ++;
            }

            /* notify that finalizer thread has started.
             * Don't put any code whith any memory allocation before here!
             * It should be granted that notify is called in any case!
             */
            synchronized (startLock) {
                startLock.notify();
            }

            if (TRACE) {
                if (permanent) {
                    trace("permanent finalization thread started");
                } else {
                    trace("temporary finalization thread started");
                }
            }

            while (true) {
                int n = doFinalization(128);

                synchronized (workLock) {
                    if (shutdown) {

                        if (TRACE) {
                            trace("terminated by shutdown request");
                        }
                        break;
                    }
                }
                
                if (0 == n) {
                    if (permanent) {
                        waitNewTask();
                    } else {
                        break;
                    }
                }
            }
        } catch (Throwable th) {
            warn("FinalizerThread terminated by " + th);
            throw new RuntimeException("FinalizerThread interrupted", th);
        } finally {
            synchronized (waitFinishLock) {
                waitFinishCounter --;

                if (waitFinishCounter == 0) {
                    waitFinishLock.notifyAll();
                }
            }

            if (TRACE) {
                trace("FinalizerThread completed");
            }
        }
    }

    /**
     * Indicates that thread shouldn't be destroyed when finalization is complete
     */
    private boolean permanent;
}
