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

import java.util.ArrayList;

/**
 * The test for Runtime.freeMemory().
 *
 * @keyword
 */
public class Free {

    public static final int Mb = 1048576;
    public static final int kb = 1024;
    public static void main(String[] args) {
        Runtime rt = Runtime.getRuntime();
        long allocated = 0;
        ArrayList a = new ArrayList(100); // prevent later reallocations
        long free = rt.freeMemory();
        trace("free = " + free + ", total = " + rt.totalMemory() + ", max = " + rt.maxMemory());
        int size = 1;
        try {
            while (size < free/4) {
                long free_before = rt.freeMemory();
                Object o = new byte[size];
                long free_after = rt.freeMemory();
                trace("allocated " + size + " bytes, free decreased by " + (free_before - free_after)
                    + ", free = " + rt.freeMemory() + ", total = " + rt.totalMemory());
                a.add(o); // prevent object from being collected
                allocated += size;
                size *= 2;
                if (rt.freeMemory() > free) {
                    fail("Amount of free memory increases");
                }
                free = rt.freeMemory();
            }
            if (allocated > 0) {
                System.out.println("PASSED");
            } else {
                fail("no free memory");
            }
        } catch (OutOfMemoryError e) {
            fail(e);
        }
    }

    public static void trace(Object o) {
        System.err.println(o);
        System.err.flush();
    }

    public static void fail(Object o) {
        System.out.println("FAILED, " + o);
        System.exit(1);
    }
}
