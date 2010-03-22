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

import java.util.*;
import java.lang.ref.*;

public class StressRef {

    static long ref_count;
    static ReferenceQueue queue;

    public static void main(String[] args) {
        ref_count = 0;
        queue = new ReferenceQueue();
        Vector v = new Vector();
        for (int i = 0; i < 100; i++) {
            v.add(weak_stress());
            if (i % 10 == 0) trace("."); 
        }
        trace("allocation complete\n");

        check_integrity(v);
        trace("integrity checked\n");

        check_ref_count();
        trace("reference queues checked\n");

        test_weak_hashmap();
        trace("weak hashmaps checked\n");

        // we need to keep variable v alive up to this point
        System.out.println("PASS, " + v.size());
    }

    static void check_ref_count() {
        System.gc();
        System.runFinalization();
        System.gc();
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        long count = 0;
        while (queue.poll() != null) count++;
        if (ref_count - count != 0) {
            trace("warning: enqueued reference count differs: "
                + ref_count + " allocated, " + count + " enqueued\n");
        } 
        if (ref_count - count > 10) {
            System.out.println("FAIL: too many unqueued references");
            System.exit(1);
        }
    }

    static void check_integrity(Vector v) {
        // check the contents of the survivor structs
        Enumeration i = v.elements();
        long count = 0, threshold = 10;
        long ref_count = 0, reset_count = 0;
        while (i.hasMoreElements()) {
            Vector inner = (Vector) i.nextElement();
            Enumeration ii = inner.elements();
            while (ii.hasMoreElements()) {
                Object o = ii.nextElement();
                if (null != o && o instanceof Reference) {
                    Reference ref = (Reference) o;
                    ref_count++;
                    o = ref.get();
                    if (null == o) 
                        reset_count++;
                }
                if (null != o && o instanceof Struct) {
                    Struct stru = (Struct) o;
                    count++;
                    if (count > threshold) {
                        threshold *= 2;
                        trace(",");
                    }

                    // check integrity
                    if (!stru.check()) {
                        System.out.println("FAIL: corrupted data");
                        System.exit(1);
                    }
                }
            }
        }
        System.out.println("\n" + ref_count + " references total, " + reset_count + " reset");
    }

    /// structure used to check integrity
    public static class Struct {

        final static int dead_beef = 0xDEADBEEF;
        final static int cafe_bebe = 0xCAFEBEBE;

        int i,j;

        public Struct() {
            i = dead_beef;
            j = cafe_bebe;
        }

        public boolean check() {
            return (dead_beef == i && cafe_bebe == j);
        }
    }

    public static Object weak_stress() {
        int i = 1;
        Vector v = new Vector();
        try {
            while (i < 10000000) {
                Struct stru = new Struct();

                // pseudo-randomize reference distribution
                Reference ref;
                if (i % 53 < 23) { 
                    ref = new WeakReference(stru, queue);
                } else if (i % 97 < 33) {
                    ref = new SoftReference(stru, queue);
                } else {
                    ref = new PhantomReference(stru, queue);
                }

                i *= 3;
                // pseudo-randomly distribute different cases
                if (i % 31 < 15) {
                    // live reference, dead referent
                    v.add(ref);
                    // clear some references
                    if (i % 53 > 25 ) {
                        ref.clear();
                    } else {
                        ref_count++; // we expect this one to be enqueued, 
                                     // if it isn't soft reference
                    }
                } else if (i % 17 < 12) {
                    // dead reference, live referent
                    v.add(stru);
                } else {
                    // live reference, live referent
                    v.add(ref);
                    v.add(stru);
                    if (i % 5 < 2)
                        ref.clear();
                }
            }
        } catch (OutOfMemoryError e) { 
            // prevent failures
        }
        return v;
    }

    public static void test_weak_hashmap () {
        WeakHashMap m = new WeakHashMap();
        try {
            int size = 0;
            int pSize = 0;
            for (int i = 0; i < 10000; i++) {
                m.put(new int[512 * 512], null);
                pSize = size;
                size = m.size();
                if (pSize >= size) {
                    break;
                }
            }
            if (pSize < size) {
                System.out.println("FAIL: test failed: pSize > size, where pSize = " + pSize + " size = " + size);
            }
        } catch (OutOfMemoryError e) {
            System.out.println("FAIL: OutOfMemoryError should not be thrown");
        }
    }
    
    public static void trace(Object o) { 
        System.err.print(o); 
        System.err.flush(); 
    }
}
