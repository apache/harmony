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

package stress;

public class Finalizer {

    static int create_count, finalize_count;
    static int number_of_classes = 10000;
    static Object obj;

    final static int tick_rate = 100;

    public static void main(String[] args) {
        
        create_count = 0;
        finalize_count = 0;

        for (int i = 0; i < number_of_classes; i++) {
            synchronized (Finalizer.class) {
                obj = new Finalizer();
                obj = new byte[1024];
                create_count += 1;
            }
            if (i % tick_rate == 0) {
                trace(".");
                System.gc();
            }
        }

        obj = null;
        System.gc();
        System.runFinalization();

        synchronized (Finalizer.class) {
            if (create_count == finalize_count) {
                System.out.println("PASSED");
            } else {
                System.out.println("FAILED: " + create_count + " objects created, "
                    + finalize_count + " objects finalized. The numbers should match");
            }
        }
    }

    public void finalize() {
        synchronized(this.getClass()) {
            finalize_count += 1;    
            if (finalize_count % tick_rate == 0) trace("x");
        }
        allocate(10);
        try {
            throw_and_catch(2);
        } catch (NullPointerException e) {}
    }

    void allocate(int n) {
        for (int i = 0; i < n; i++) {
            byte[] b = new byte[i];
            if (b.length >= 0) new Object();
        }
    }

    void throw_and_catch(int depth) {
        if (0 == depth) {
            ((String) null).length();
        } else {
            try {
                ((String) null).length();
            } catch (NullPointerException e) {}
            throw_and_catch(depth - 1);
        }
    }

    public static void trace(Object o) {
        System.out.print(o);
        System.out.flush();
    }

}
