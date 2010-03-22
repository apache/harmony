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
package org.apache.harmony.rmi.server;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Extension of WeakReference. It could contain strong reference to the object.
 * Also hashCode() and equals() methods are overridden. This class is used for
 * storing exported rmi objects.
 *
 * @author  Mikhail A. Markov
 */
public class RMIReference extends WeakReference {

    // strong reference to the object this class refers to
    private Object strongRef = null;

    // Hash code for the referenced object.
    private int objHashCode;

    /**
     * Constructs RMIReference from the given Object.
     */
    public RMIReference(Object obj) {
        super(obj);
        objHashCode = System.identityHashCode(obj);
    }

    /**
     * Constructs RMIReference from the given Object and ReferenceQueue.
     */
    public RMIReference(Object obj, ReferenceQueue queue) {
        super(obj, queue);
        objHashCode = System.identityHashCode(obj);
    }

    /**
     * If the given parameter is true then makes this reference strong;
     * otherwise makes this reference weak.
     *
     * @param strong if true then this reference should be made strong
     */
    public synchronized void makeStrong(boolean strong) {
        if (strong) {
            if (strongRef == null) {
                strongRef = get();


                if (DGCImpl.dgcLog.isLoggable(RMILog.VERBOSE)) {
                    // rmi.log.9F=Strongly referenced {0}
                    DGCImpl.dgcLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.9F", //$NON-NLS-1$
                            strongRef));
                }
            }
        } else {
            if (strongRef != null
                    && DGCImpl.dgcLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.log.10D=Weakly referenced {0}
                DGCImpl.dgcLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.10D", //$NON-NLS-1$
                        strongRef));
            }
            strongRef = null;
        }
    }

    /**
     * Returns true if the given object is an instance of this class and refer
     * to the same object as this reference and false otherwise.
     *
     * @param obj another object to be compared
     *
     * @return true if the given object is an instance of this class and refer
     *         to the same object as this reference and false otherwise
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof RMIReference)) {
            return false;
        }
        Object refObj = get();

        if (refObj == null) {
            return false;
        }
        return refObj == ((WeakReference) obj).get();
    }

    /**
     * Returns hash code for the referenced object.
     *
     * @return hash code for the referenced object
     */
    public int hashCode() {
        return objHashCode;
    }
}
