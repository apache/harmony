/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author  Mikhail A. Markov
 */
package org.apache.harmony.rmi.common;

import java.security.AccessController;
import java.security.PrivilegedAction;


/**
 * Action for privileged Threads creation.
 * There are 2 ThreadGroups used in RMI runtime:
 *   1) System ThreadGroup, i.e. the topmost non-null ThreadGroup. All RMI
 *      internal threads are created in this group
 *   2) Non-system ThreadGroup, i.e. the child of the system ThreadGroup. There
 *      are 2 cases when the thread is created in this group:
 *        a) When the remote call is accepted on the server side
 *        b) When the unreferenced thread is started
 *
 * There are 2 reasons why 2 threads are needed:
 *   1) If we create all threads in the system ThreadGroup than if the
 *      implementation of remote method has the code starting the new thread,
 *      we'll get AccessControllException requiring
 *      java.lang.RuntimePermission("modifyThreadGroup") and
 *      java.lang.RuntimePermission("modifyThread") permissions because it's not
 *      allowed by default to create a new thread in the system group
 *   2) If we create all threads (including RMI internal ones) in non-system
 *      ThreadGroup then the malicious code could potentially interrupt RMI
 *      internal threads by using Thread.enumerate() static method.
 *
 * @author  Mikhail A. Markov
 */
public class CreateThreadAction implements PrivilegedAction {
    /*
     * System ThreadGroup, i.e. the topmost non-null ThreadGroup.
     */
    private static final ThreadGroup systemGroup = (ThreadGroup)
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    ThreadGroup tg =
                            Thread.currentThread().getThreadGroup();
                    ThreadGroup tg1 = tg.getParent();

                    while (tg1 != null) {
                        tg = tg1;
                        tg1 = tg.getParent();
                    }
                    return tg;
                }
            });

    /*
     * Non-system ThreadGroup: child of the system ThreadGroup.
     */
    private static final ThreadGroup nonSystemGroup = (ThreadGroup)
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    return new ThreadGroup(systemGroup, "RMI Group"); //$NON-NLS-1$
                }
            });

    // Runnable object for Thread creation.
    private Runnable r;

    // The name of thread to be created.
    private String name;

    // True if thread to be created should be a daemon.
    private boolean isDaemon;

    // True if thread to be created should be in the system ThreadGroup.
    private boolean isSystem;

    /**
     * Constructs CreateThreadAction to create a new Thread in the system
     * ThreadGroup.
     *
     * @param r Runnable object for Thread creation
     * @param name name of thread to be created
     * @param isDaemon true if thread to be created should be a daemon
     */
    public CreateThreadAction(Runnable r, String name, boolean isDaemon) {
        this(r, name, isDaemon, true);
    }

    /**
     * Constructs CreateThreadAction to create Thread.
     *
     * @param r Runnable object for Thread creation
     * @param name name of thread to be created
     * @param isDaemon true if thread to be created should be a daemon
     * @param isSystem true if system ThreadGroup should be used and false
     *        otherwise
     */
    public CreateThreadAction(Runnable r,
                              String name,
                              boolean isDaemon,
                              boolean isSystem) {
        this.r = r;
        this.name = name;
        this.isDaemon = isDaemon;
        this.isSystem = isSystem;
    }

    /**
     * Creates the thread.
     *
     * @return started thread
     */
    public Object run() {
        Thread t = new Thread(isSystem ? systemGroup : nonSystemGroup, r, name);
        t.setContextClassLoader(ClassLoader.getSystemClassLoader());
        t.setDaemon(isDaemon);
        return t;
    }
}
