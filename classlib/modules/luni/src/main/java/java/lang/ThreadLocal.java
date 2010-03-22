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

package java.lang;

/*
 * Note that the Harmony VM "DRLVM" uses a different implementation of ThreadLocal.
 * See DRLVM's classes here:
 * http://svn.apache.org/viewvc/harmony/enhanced/drlvm/trunk/vm/vmcore/src/kernel_classes/
 */

/**
 * Implements a thread-local storage, that is, a variable for which each thread
 * has its own value. All threads share the same {@code ThreadLocal} object,
 * but each sees a different value when accessing it, and changes made by one
 * thread do not affect the other threads. The implementation supports
 * {@code null} values.
 *
 * @see java.lang.Thread
 */
public class ThreadLocal<T> {
    /**
     * Creates a new thread-local variable.
     */
    public ThreadLocal() {
        super();
    }

    /**
     * Returns the value of this variable for the current thread. If an entry
     * doesn't yet exist for this variable on this thread, this method will
     * create an entry, populating the value with the result of
     * {@link #initialValue()}.
     *
     * @return the current value of the variable for the calling thread.
     */
    @SuppressWarnings("unchecked")
    public T get() {
        return (T) Thread.currentThread().getThreadLocal(this);
    }

    /**
     * Provides the initial value of this variable for the current thread.
     * The default implementation returns {@code null}.
     *
     * @return the initial value of the variable.
     */
    protected T initialValue() {
        return null;
    }

    /**
     * Sets the value of this variable for the current thread. If set to
     * {@code null}, the value will be set to null and the underlying entry will
     * still be present.
     *
     * @param value the new value of the variable for the caller thread.
     */
    public void set(T value) {
        Thread.currentThread().setThreadLocal(this, value);
    }

    /**
     * Removes the entry for this variable in the current thread. If this call
     * is followed by a {@link #get()} before a {@link #set},
     * {@code #get()} will call {@link #initialValue()} and create a new
     * entry with the resulting value.
     *
     * @since 1.5
     */
    public void remove() {
        /*
         * TODO Consider adding an explicit removeThreadLocal method to Thread
         * for VM implementations to take advantage of extra possible space.
         */
        Thread.currentThread().setThreadLocal(this, initialValue());
    }
}
