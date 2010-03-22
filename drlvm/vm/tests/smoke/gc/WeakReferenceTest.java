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
 * @author Salikh Zakirov
 */  

package gc;

import java.lang.ref.WeakReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;


/**
 * @keyword ref
 */
public class WeakReferenceTest {

    public static void main(String[] args) throws Exception {
        ReferenceQueue queue = new ReferenceQueue();
        Object referent = new WeakReferenceTest();
        Reference ref = new WeakReference(referent, queue);
        if (ref.get() != referent) {
            System.out.println("FAIL: can't get weak referent");
            return;
        }

        // drop strong reference
        referent = null;

        System.gc();
        
        // run finalization to be sure that the reference is enqueued
        System.runFinalization();
        
        Reference enqueued;
        enqueued = queue.poll();
        if (enqueued == null) {
            System.out.println("FAIL: reference was not enqueued");
            return;
        }
        if (ref.get() != null) {
            System.out.println("FAIL: reference was not cleared.");
            return;
        }
        if (enqueued.get() != null) {
            System.out.println("FAIL: reference was not cleared.");
            return;
        }
        System.out.println("PASS");
    }
}
