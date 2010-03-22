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
package java.rmi.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;


/**
 * @com.intel.drl.spec_ref
 *
 * @author  Mikhail A. Markov
 */
public final class UID implements Serializable {

    private static final long serialVersionUID = 1086053664494604050L;
    private int unique;
    private long time;
    private short count;

    // Sequentially increasing counter for initializing count field.
    private static short countCounter = Short.MIN_VALUE;

    // Time when last UID was created.
    private static long lastCreationTime = System.currentTimeMillis();

    // Lock object for synchronization.
    private static class Lock {}
    private static final Object lock = new Lock();

    // unique identifier for this VM.
    private static final int vmUnique = lock.hashCode();

    /**
     * @com.intel.drl.spec_ref
     */
    public UID(short num) {
        unique = 0;
        time = 0;
        count = num;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public UID() {
        synchronized (lock) {
            if (countCounter == Short.MAX_VALUE) {
                long curTime = System.currentTimeMillis();

                while (curTime - lastCreationTime < 1000) {
                    try {
                        // sleep for a while
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                    }
                    curTime = System.currentTimeMillis();
                }
                lastCreationTime = curTime;
                countCounter = Short.MIN_VALUE;
            }
            unique = vmUnique;
            time = lastCreationTime;
            count = countCounter++;
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public String toString() {
        return "UID[" + Integer.toString(unique, 16) + ":" //$NON-NLS-1$ //$NON-NLS-2$
                      + Long.toString(time, 16) + ":" //$NON-NLS-1$
                      + Integer.toString(count, 16) + "]"; //$NON-NLS-1$
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public boolean equals(Object obj) {
        if (obj != null && (obj instanceof UID)) {
            UID uid = (UID) obj;
            return (unique == uid.unique)
                   && (time == uid.time)
                   && (count == uid.count);
        }
        return false;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public void write(DataOutput out) throws IOException {
        out.writeInt(unique);
        out.writeLong(time);
        out.writeShort(count);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public int hashCode() {
        return (int) (time ^ count);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static UID read(DataInput in) throws IOException {
        UID uid = new UID((short) -1);
        uid.unique = in.readInt();
        uid.time = in.readLong();
        uid.count = in.readShort();
        return uid;
    }
}
