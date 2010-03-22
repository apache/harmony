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

/**
 * Fragment the heap with large allocations interspersed
 * with medium-size allocation and then check if larger
 * allocation works.
 */
public class Fragment {

    static final int Mb = 1048576;
    static Object o;

    public static void main (String[] args) {
        // heuristically choose the size for "large" allocation
        int large_size = (int)(Runtime.getRuntime().freeMemory() / 2);
        Object[] medium = new Object[200]; // usually enough
        Object[] large = new Object[medium.length];
        try {
            for (int i = 0; i < medium.length; i++) {
                // "medium" objects are 1 Mb
                medium[i] = new byte[Mb];
                // "large" objects are large_size-1 Mb
                large[i] = new byte[large_size-Mb];
                trace(".");
            }
        } catch (OutOfMemoryError e) {
            System.out.println("\nheap filled with 1 Mb and "
                + ((large_size-Mb)/Mb) + " Mb objects");
        }
        // release all "large" objects
        // the "medium" objects end up distributed all over the heap
        large = null;
        try {
            // try allocation of "large_size+1Mb" object
            o = new byte[large_size+Mb];
            System.out.println("PASSED, " + ((large_size+Mb)/Mb) 
                + " Mb allocation succeeded");
        } catch (OutOfMemoryError e) {
            System.out.println("FAILED, " + ((large_size+Mb)/Mb) 
                + " Mb allocation failed");
        }
    }

    public static void trace(Object o) {
        System.out.print(o);
        System.out.flush();
    }
}
