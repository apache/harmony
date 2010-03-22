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
 * Exercises the allocation from finalizer.
 *
 */
public class FinAlloc {

    final static int kb = 1024;
    final static int Mb = 1024*1024;
    static Object o;
    static int size = 177*kb;

    public static void main (String[] args) {
        try {
            for (int i = 0; i < 39; i++) {
                o = new FinAlloc();
                trace(".");
                allocate(size);           // allocate lots of garbage
                size = (size*17) & (16*Mb - 1);
                if (i % 10 == 0) {
                    System.runFinalization();
                }
            }
        } catch (OutOfMemoryError e) {
            trace("o\n");
        }
        System.gc();
        System.runFinalization();
        System.out.println("PASSED");
    }

    public void finalize() {
        try {
            allocate(size);       // ensure allocation from finalizer is working
            trace(",");
        } catch (OutOfMemoryError e) {
            trace("x");
        }
    }

    public static Object allocate(int size) {
        ArrayList a = new ArrayList(size/16384 + 1);
        if (size > 16384) {
            for (int i = 0; i < size/16384; i++) {
                a.add(new byte[16384]);
            }
        } else {
            a.add(new byte[size]);
        }
        return a;
    }

    public static void trace(Object o) {
        System.err.print(o);
        System.err.flush();
    }
}
