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

/**
 * Provides the methods to interact with VM Memory Manager that are used by
 * different classes from the <code>java.lang</code> package, such as Object,
 * System, Runtime.
 * <p>
 * This class must be implemented according to the common policy for porting
 * interfaces - see the porting interface overview for more detailes.
 *
 * @author Evgueni Brevnov, Roman S. Bushmanov
 *
 * @api2vm
 */
final class VMMemoryManager {

    /**
     * This class is not supposed to be instantiated.
     */
    private VMMemoryManager() {}

    /**
     * This method satisfies the requirements of the specification for the
     * {@link System#arraycopy(java.lang.Object, int, java.lang.Object, int, int)
     * System.arraycopy(java.lang.Object src, int srcPos, java.lang.Object dest,
     * int destPos, int length)} method.
     * <p>
     * <b>Note:</b> Under design yet. Subjected to change.
     * @api2vm
     */
    static native void arrayCopy(Object src, int srcPos, Object dest,
            int destPos, int len);

    /**
     * Creates a shallow copy of the specified object.
     * <p>
     * <b>Note:</b> This method is used for the {@link java.lang.Object#clone()
     * Object.clone()} method implementation.
     *
     * @param object an object to be cloned
     * @return a copy of the specified object
     * @api2vm
     */
    static native Object clone(Object object);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Runtime#freeMemory() Runtime.freeMemory()} method.
     * @api2vm
     */
    static native long getFreeMemory();

    /**
     * This method satisfies the requirements of the specification for the
     * {@link System#identityHashCode(java.lang.Object)
     * System.identityHashCode(Object x)} method.
     * @api2vm
     */
    static native int getIdentityHashCode(Object object);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Runtime#maxMemory() Runtime.maxMemory()} method.
     * @api2vm
     */
    static native long getMaxMemory();

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Runtime#totalMemory() Runtime.totalMemory()} method.
     * @api2vm
     */
    static native long getTotalMemory();

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Runtime#runFinalization() Runtime.runFinalization()} method.
     * @api2vm
     */
    static void runFinalization() {
        FinalizerThread.runFinalization();
    }

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Runtime#gc() Runtime.gc()} method.
     * @api2vm
     */
    static native void runGC();
}
