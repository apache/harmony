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
 * @author Roman S. Bushmanov
 */

package java.lang;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * @com.intel.drl.spec_ref
 */

public class ThreadGroup implements Thread.UncaughtExceptionHandler {

    /**
     * Indent used to print information about thread group
     */
    private final static String LISTING_INDENT = "    ";

    /**
     * ThreadGroup lock object
     */
    private static class ThreadGroupLock {};
    private final static ThreadGroupLock lock = new ThreadGroupLock();

    /**
     * This group's max priority
     */
    int maxPriority = Thread.MAX_PRIORITY;

    /**
     * This group's name
     */
    String name;

    /**
     * Indicates if this thread group was marked as daemon
     */
    private boolean daemon;

    /**
     * Indicates if this thread group was already destroyed
     */
    private boolean destroyed = false;

    /**
     * List of subgroups of this thread group
     */
    private LinkedList<ThreadGroup> groups = new LinkedList<ThreadGroup>();

    /**
     * Parent thread group of this thread group.
     *
     * FIXME: this field must be private. It is changed to package-private
     * to be accessible from FT SecurityManager class. Both SecurityManager
     * and ThreadGroup are considered as non-Kernel by FT, but ThreadGroup
     * is Kernel now in DRL.
     */
    ThreadGroup parent;

    /**
     * All threads in the group.
     */
    private LinkedList<Thread> threads = new LinkedList<Thread>();

    /**
     * @com.intel.drl.spec_ref
     */
    public ThreadGroup(String name) {
        this(Thread.currentThread().group, name);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public ThreadGroup(ThreadGroup parent, String name) {
        if (parent == null) {
            throw new NullPointerException(
                "The parent thread group specified is null!");
        }
        parent.checkAccess();
        this.name = name;
        this.parent = parent;
        this.daemon = parent.daemon;
        this.maxPriority = parent.maxPriority;
        parent.add(this);
    }

    /**
     * This constructor is used to create the system thread group
     */
    ThreadGroup() {
        this.parent = null;
        this.name = "system";
        this.daemon = false;
    }

    /**
     * @com.intel.drl.spec_ref Note: A thread is supposed to be active if and
     *                         only if it is alive.
     */
    public int activeCount() {
        int count = 0;
        List groupsCopy = null;  // a copy of subgroups list
        List threadsCopy = null; // a copy of threads list
        synchronized (lock) {
            if (destroyed) {
                return 0;
            }
            threadsCopy = (List)threads.clone();
            groupsCopy = (List)groups.clone();
        }

        for (Object thread : threadsCopy) {
            if (((Thread)thread).isAlive()) {
                count++;
            }
        }

        for (Object group : groupsCopy) {
            count += ((ThreadGroup)group).activeCount();
        }
        return count;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public int activeGroupCount() {
        int count;
        List groupsCopy = null; // a copy of subgroups list
        synchronized (lock) {
            if (destroyed) {
                return 0;
            }
            count = groups.size();
            groupsCopy = (List)groups.clone();
        }
        for (Object group : (List)groupsCopy) {
            count += ((ThreadGroup)group).activeGroupCount();
        }
        return count;
    }

    /**
     * @com.intel.drl.spec_ref Note: This implementation always returns
     *                         <code>false</code>.
     * @deprecated
     */
    public boolean allowThreadSuspension(boolean b) {
        return false;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final void checkAccess() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkAccess(this);
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final void destroy() {
        checkAccess();
        synchronized (lock) {
            if (destroyed) {
                throw new IllegalThreadStateException(
                        "The thread group " + name + " is already destroyed!");
            }
            nonsecureDestroy();
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public int enumerate(Thread[] list) {
        checkAccess();
        return enumerate(list, 0, true);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public int enumerate(Thread[] list, boolean recurse) {
        checkAccess();
        return enumerate(list, 0, recurse);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public int enumerate(ThreadGroup[] list) {
        checkAccess();
        return enumerate(list, 0, true);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public int enumerate(ThreadGroup[] list, boolean recurse) {
        checkAccess();
        return enumerate(list, 0, recurse);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final int getMaxPriority() {
        return maxPriority;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final String getName() {
        return name;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final ThreadGroup getParent() {
        if (parent != null) {
            parent.checkAccess();
        }
        return parent;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final void interrupt() {
        checkAccess();
        nonsecureInterrupt();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final boolean isDaemon() {
        return daemon;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public void list() {
        list("");
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final boolean parentOf(ThreadGroup group) {
        ThreadGroup parent = group;
        while (parent != null) {
            if (this == parent) {
                return true;
            }
            parent = parent.parent;
        }
        return false;
    }

    /**
     * @com.intel.drl.spec_ref
     * @deprecated
     */
    public final void resume() {
        checkAccess();
        nonsecureResume();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final void setDaemon(boolean daemon) {
        checkAccess();
        this.daemon = daemon;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final void setMaxPriority(int priority) {
        checkAccess();

        /*
         *  GMJ : note that this is to match a known bug in the RI
         *  http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4708197
         *  We agreed to follow bug for now to prevent breaking apps
         */
        if (priority > Thread.MAX_PRIORITY) {
            return;
        }
        if (priority < Thread.MIN_PRIORITY) {
            this.maxPriority = Thread.MIN_PRIORITY;
            return;
        }
        int new_priority = (parent != null && parent.maxPriority < priority)
                            ? parent.maxPriority
                            : priority;

        nonsecureSetMaxPriority(new_priority);
    }

    /**
     * @com.intel.drl.spec_ref
     * @deprecated
     */
    public final void stop() {
        checkAccess();
        nonsecureStop();
    }

    /**
     * @com.intel.drl.spec_ref
     * @deprecated
     */
    public final void suspend() {
        checkAccess();
        nonsecureSuspend();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public String toString() {
        return getClass().getName() + "[name=" + name + ",maxpri="
            + maxPriority + "]";
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public void uncaughtException(Thread thread, Throwable throwable) {
        if(parent != null){
           parent.uncaughtException(thread, throwable);
           return;
        }
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        if(defaultHandler != null){
            defaultHandler.uncaughtException(thread, throwable);
            return;
        }
        if(throwable instanceof ThreadDeath){
            return;
        }
        System.err.println("Uncaught exception in " + thread.getName() + ":");
        throwable.printStackTrace();
    }

    /**
     * Adds a thread to this thread group
     */
    void add(Thread thread) {
        synchronized (lock) {
            if (destroyed) {
                throw new IllegalThreadStateException(
                        "The thread group is already destroyed!");
            }
            threads.add(thread);
        }
    }

    /**
     * Checks that group is not destroyed
     */
    void checkGroup() {
        synchronized (lock) {
            if (destroyed) {
                throw new IllegalThreadStateException(
                        "The thread group is already destroyed!");
            }
        }
    }

    /**
     * Removes a thread from this thread group
     */
    void remove(Thread thread) {
        synchronized (lock) {
            if (destroyed) {
                return;
            }
            threads.remove(thread);
            thread.group = null;
            if (daemon && threads.isEmpty() && groups.isEmpty()) {
                // destroy this group
                if (parent != null) {
                    parent.remove(this);
                    destroyed = true;
                }
            }
        }
    }

    /**
     * Adds a subgroup to this thread group
     */
    private void add(ThreadGroup group) {
        synchronized (lock) {
            if (destroyed) {
                throw new IllegalThreadStateException(
                        "The thread group is already destroyed!");
            }
            groups.add(group);
        }
    }

    /**
     * Used by GetThreadGroupChildren() jvmti function.
     * @return Object[] array of 2 elements: first - Object[] array of active
     * child threads; second - Object[] array of child groups.
     */
    @SuppressWarnings("unused")
    private Object[] getActiveChildren() {
        ArrayList<Thread> threadsCopy = new ArrayList<Thread>(threads.size());
        ArrayList<ThreadGroup> groupsCopy = new ArrayList<ThreadGroup>(groups.size());

        synchronized (lock) {
            if (destroyed) {
                return new Object[] {null, null};
            }

            for (Thread thread : threads) {
                threadsCopy.add(thread);
            }

            for (ThreadGroup group : groups) {
                groupsCopy.add(group);
            }
        }

        ArrayList<Thread> activeThreads = new ArrayList<Thread>(threadsCopy.size());

        // filter out alive threads
        for (Thread thread : threadsCopy) {
            if (thread.isAlive()) {
                activeThreads.add(thread);
            }
        }

        return new Object[] {activeThreads.toArray(), groupsCopy.toArray()};
    }

    /**
     * Copies all the threads contained in the snapshot of this thread group to
     * the array specified starting from the specified position. <br>
     * If the specified array is not long enough to take all the threads of this
     * thread group, the exta threads are silently ignored. <br>
     *
     * @param list an array to copy threads to
     * @param offset position in this array to start copying from
     * @param recurse indicates if the threads contained in the subgroups of
     *        this thread group should be recursively copied to the array
     *        specified
     * @return the number of threads in the array after the copying is
     *         done
     */
    private int enumerate(Thread[] list, int offset, boolean recurse) {
        if (list.length == 0) {
            return 0;
        }
        List groupsCopy = null;  // a copy of subgroups list
        List threadsCopy = null; // a copy of threads list
        synchronized (lock) {
            if (destroyed) {
                return offset;
            }
            threadsCopy = (List)threads.clone();
            if (recurse) {
                groupsCopy = (List)groups.clone();
            }
        }
        for (Object thread : threadsCopy) {
            if (((Thread)thread).isAlive()) {
                list[offset++] = ((Thread)thread);
                if (offset == list.length) {
                    return offset;
                }
            }
        }
        if (recurse) {
            for (Iterator it = groupsCopy.iterator(); offset < list.length
                && it.hasNext();) {
                offset = ((ThreadGroup)it.next()).enumerate(list, offset, true);
            }
        }
        return offset;
    }

    /**
     * Copies all the subgroups contained in the snapshot of this thread group
     * to the array specified starting from the specified position. <br>
     * If the specified array is not long enough to take all the subgroups of
     * this thread group, the exta subgroups are silently ignored. <br>
     *
     * @param list an array to copy subgroups to
     * @param offset position in this array to start copying from
     * @param recurse indicates if the subgroups contained in the subgroups of
     *        this thread group should be recursively copied to the array
     *        specified
     * @return the number of subgroups in the array after the copying
     *         is done
     */
    private int enumerate(ThreadGroup[] list, int offset, boolean recurse) {
        if (destroyed) {
            return offset;
        }
        int firstGroupIdx = offset;
        synchronized (lock) {
            for (Object group : groups) {
                list[offset++] = (ThreadGroup)group;
                if (offset == list.length) {
                    return offset;
                }
            }
        }
        if (recurse) {
            int lastGroupIdx = offset;
            for (int i = firstGroupIdx; offset < list.length
                && i < lastGroupIdx; i++) {
                offset = list[i].enumerate(list, offset, true);
            }
        }
        return offset;
    }

    /**
     * Recursively prints the information about this thread group using
     * <code>prefix</code> string as indent.
     */
    private void list(String prefix) {
        System.out.println(prefix + toString());
        prefix += LISTING_INDENT;
        List groupsCopy = null;   // a copy of subgroups list
        List threadsCopy = null;  // a copy of threads list
        synchronized (lock) {
            threadsCopy = (List)threads.clone();
            groupsCopy = (List)groups.clone();
        }
        for (Object thread : threadsCopy) {
            System.out.println(prefix + (Thread)thread);
        }
        for (Object group : groupsCopy) {
            ((ThreadGroup)group).list(prefix);
        }
    }

    /**
     * Destroys this thread group without any security checks. We add this
     * method to avoid calls to the checkAccess() method on subgroups.
     * All non-empty subgroups are removed recursievely.
     * If at least one subgroup is not empty, IllegalThreadStateException
     * will be thrown.
     * @return false if this ThreadGroup is not empty
     */
    private void nonsecureDestroy() {

        List groupsCopy = null;

        synchronized (lock) {
            if (threads.size() > 0) {
                throw new IllegalThreadStateException("The thread group " + name +
                        " is not empty");
            }
            destroyed = true;
            groupsCopy = (List)groups.clone();
        }

        if (parent != null) {
            parent.remove(this);
        }

        for (Object group : groupsCopy) {
            ((ThreadGroup)group).nonsecureDestroy();
        }
    }

    /**
     * Interrupts this thread group without any security checks. We add this
     * method to avoid calls to the checkAccess() method on subgroups
     */
    private void nonsecureInterrupt() {
        synchronized (lock) {
            for (Object thread : threads) {
                ((Thread)thread).interrupt();
            }
            for (Object group : groups) {
                ((ThreadGroup)group).nonsecureInterrupt();
            }
        }
    }

    /**
     * Resumes this thread group without any security checks. We add this method
     * to avoid calls to the checkAccess() method on subgroups
     */
    private void nonsecureResume() {
        synchronized (lock) {
            for (Object thread : threads) {
                ((Thread)thread).resume();
            }
            for (Object group : groups) {
                ((ThreadGroup)group).nonsecureResume();
            }
        }
    }

    /**
     * Sets the maximum priority allowed for this thread group and its subgroups.
     * We add this method to avoid calls to the checkAccess() method on subgroups
     */
    private void nonsecureSetMaxPriority(int priority) {
        synchronized (lock) {
            this.maxPriority = priority;

            for (Object group : groups) {
                ((ThreadGroup)group).nonsecureSetMaxPriority(priority);
            }
        }
    }

    /**
     * Stops this thread group without any security checks.
     * We add this method to avoid calls to the checkAccess() method on subgroups
     */
    private void nonsecureStop() {
        synchronized (lock) {
            for (Object thread : threads) {
                ((Thread)thread).stop();
            }
            for (Object group : groups) {
                ((ThreadGroup)group).nonsecureStop();
            }
        }
    }

    /**
     * Suspends this thread group without any security checks.
     * We add this method to avoid calls to the checkAccess() method on subgroups
     */
    private void nonsecureSuspend() {
        synchronized (lock) {
            for (Object thread : threads) {
                ((Thread)thread).suspend();
            }
            for (Object group : groups) {
                ((ThreadGroup)group).nonsecureSuspend();
            }
        }
    }

    /**
     * Removes the specified thread group from this group.
     *
     * @param group group to be removed from this one
     */
    private void remove(ThreadGroup group) {
        synchronized (lock) {
            groups.remove(group);
            if (daemon && threads.isEmpty() && groups.isEmpty()) {
                // destroy this group
                if (parent != null) {
                    parent.remove(this);
                    destroyed = true;
                }
            }
        }
    }
}
