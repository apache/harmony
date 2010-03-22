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

package org.apache.harmony.drlvm.thread;

import org.apache.harmony.drlvm.VMHelper;
import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

public class ThreadHelper {

    public static final int THREAD_SHIFT = 16;
    public static final int RECURSION_BOUND_IN_PLACE = 0xf400;
    public static final int HI_BITS = 0xffff0000;
    public static final int RECURSION_INC_IN_PLACE = 0x800;
    public static final int ZEROED_BITS_FOR_INITIAL_LOCK = 0xffff0400;

    public static final int LOCK_WORD_OFFSET    = getLockWordOffset();
    public static final int TLS_THREAD_ID_OFFSET= getThreadIdOffset();
    public static final int THREAD_JAVA_OBJECT_OFFSET = getThreadJavaObjectOffset();

    @Inline
    static int getThreadId() {
        Address tlsAddr = VMHelper.getTlsBaseAddress();
        Address tlsThreadIdFieldAddr = tlsAddr.plus(TLS_THREAD_ID_OFFSET);
        return tlsThreadIdFieldAddr.loadInt();
    }

    @Inline
    public static Thread getCurrentThread() {
        Address tlsThread = VMHelper.getTlsBaseAddress();
        Address javaObjectPtr = tlsThread.plus(THREAD_JAVA_OBJECT_OFFSET).loadAddress();
        if (javaObjectPtr.isZero()) {
            return null;
        } else {
            return (Thread)javaObjectPtr.loadAddress().toObjectReference().toObject();
        }
    }

    @Inline
    static void monitorEnterUseReservation(Object obj) {
        Address lockWordPtr = ObjectReference.fromObject(obj).toAddress().plus(LOCK_WORD_OFFSET);
        int threadId = getThreadId();
        int lockword = lockWordPtr.loadInt();
        int new_lockword = (threadId<<THREAD_SHIFT) ^ lockword; 
                                           
        if ((new_lockword & HI_BITS) == 0){
           // comparison above is some kind of tricky, two things are checked at once; 
           // if we got zero it means that there is NO fat lock here, and thread_id stored in lockword
           // is the same as current thread_id

           if ( new_lockword <= RECURSION_BOUND_IN_PLACE ) { 
                //This is the most frequent path.
                //lockword updated without atomic operation.
                //this path works for reservation locks (already reserved) and thin locks if the lock 
                //was already captured by this thread.
               lockword+= RECURSION_INC_IN_PLACE;
               lockWordPtr.store(lockword);
               return ;
            }
        } else {
            // available possibilities here:
            // 1. fat lock - 0x80000000 is set to 1      ; need goto slow path
            // 2. captured thin lock for another thread  ; need goto slow path
            // 3. reserved lock for another thread.      ; need goto slow path
            // 4. non-locked thin lock.
            // 5. The first lock for the object.               
            if ((lockword & ZEROED_BITS_FOR_INITIAL_LOCK)==0 ) { 
                // no locks was here. Reserve it.
                new_lockword+=RECURSION_INC_IN_PLACE;
                if (lockWordPtr.attempt(lockword, new_lockword)) {
                    return;
                }
            } else {                        
                if ((lockword & HI_BITS)==0) {
                    if (lockWordPtr.attempt(lockword, new_lockword)) {
                        return;
                    }
                }
            }
        } 
        VMHelper.monitorEnter(obj);    
    }

    public static final int FAT_LOCK_MASK = 0x80000000;
    public static final int RECURSION_MASK = 0x0000f800;

    @Inline
    static void monitorExit(Object obj) {
        Address lockWordPtr = ObjectReference.fromObject(obj).toAddress().plus(LOCK_WORD_OFFSET);
        int lockword = lockWordPtr.loadInt();
        if (((lockword & (FAT_LOCK_MASK|RECURSION_MASK)))!=0) {
            if ((lockword & FAT_LOCK_MASK)==0) {
                lockword-=RECURSION_INC_IN_PLACE;
                lockWordPtr.store(lockword);
                return;
            }
        } else {
            lockword&=~HI_BITS;
            lockWordPtr.store(lockword);
            return;
        }
        VMHelper.monitorExit(obj);    
    }

    private static native int getThreadIdOffset();
    private static native int getLockWordOffset();
    private static native int getThreadJavaObjectOffset();
}


