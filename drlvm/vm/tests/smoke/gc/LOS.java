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
 * Try large object allocation after filling the heap with small objects.
 *
 */
public class LOS extends Thread {

    final static int chunk_size = 4096;                 // allocation unit
    final static long megabyte = 1048576;               

    // total bytes to allocate
    static long total_allocate = 400 * megabyte;  
    // live set
    static long live_size = 40 * megabyte;        

    static int thread_count = 200;
    static long thread_allocate = total_allocate / thread_count;
    static long thread_live = live_size / thread_count;

    static long total_allocated = 0;    // counter of actually allocated data
    static boolean started = false;

    // Objects of >30k are considired "large"
    final static int large_object_size = 131072*20-4096-12;

    // Objects of <30k are considered "small"
    final static int small_object_size = (131072-4096)/4 - 12;


    public static void main (String[] args) {
        init_from_properties();
        Thread[] threads = new Thread[thread_count];
        for (int i = 0; i < thread_count; i++) {
            threads[i] = new LOS();
            threads[i].start();
        }
        synchronized (LOS.class) {
            LOS.class.notifyAll();
            started = true;
            System.err.println(); System.err.flush();
        }

        for (int i = 0; i < thread_count; i++) {
            try {
                threads[i].join();
                trace(",");
            } catch (InterruptedException e) {}
        }
        trace("\n");

        long smos_space = allocate_max(small_object_size);
        System.out.println("" + (smos_space/1048576) + " Mb available in SmOS");

        long los_space = allocate_max(large_object_size);
        System.out.println("" + (los_space/1048576) + " Mb available in LOS");

        if (los_space * 1.0 < smos_space * 0.8) {
            System.out.println("FAILED, LOS space is too small");
        } else {
            System.out.println("PASSED, LOS available space is on par with SmOS");
        }
    }


    public void run () {
        trace(".");
        synchronized (this.getClass()) {
            if (!started)
                try {
                    this.getClass().wait();
                } catch (InterruptedException e) {}
        }
        long count = thread_allocate / chunk_size;
        /* maintain live set in [live_size/2 , live_size] */
        long reset = thread_live / chunk_size / 2;
        ArrayList list = new ArrayList();
        ArrayList old = new ArrayList();
        for (long i = 0; i < count; i++) {
            list.add(allocate(chunk_size));
            if (i % reset == 0) {
                old = list;
                list = new ArrayList();
                total_allocated += 68;  // a guess at ArrayList size
                old.add(list);
            }
        }
    }

    public Object allocate (int size) {
        final int breadth = 10;
        try {
            exception(size);
        } catch (NullPointerException e) {}
        if (size > chunk_size) {
            ArrayList list = new ArrayList(breadth);
            total_allocated += 28 + 4*breadth; // a guess at ArrayList size
            for (int i=0; i<breadth; i++) {
                list.add(allocate((size-breadth*4-28) / breadth));
            }
            return list;
        } else {
            total_allocated += size + 12;
            return new byte[size];
        }
    }

    /* trying to pseudo-randomize null pointer exception sites. */
    public void exception (int i) {
        if (i%17 == 0) {
            String s = null;
            s.length();
        } else if (i%11 == 0) {
            exception(i/31);
        } else {
            exception(i/2);
        }
    }

    public static long parseSize (String x) throws NumberFormatException {
        int len = x.length();
        long factor = 1;
        switch (x.charAt(len-1)) {
            case 'k': factor = 1024; len--; break;
            case 'm': factor = 1048576; len--; break;
            case 'g': factor = 1073741824; len--; break;
        }
        long result = Long.parseLong(x.substring(0,len));
        return factor * result;
    }

    public static void init_from_properties() {
        try {
            total_allocate = parseSize(System.getProperty("allocate"));
            System.out.println("allocating " + (total_allocate/1048576) + " Mb");
        } catch (Exception e) { /* ignore */ }

        try {
            live_size = parseSize(System.getProperty("live"));
            System.out.println("live set size " + (live_size/1048576) + " Mb");
        } catch (Exception e) { /* ignore */ }

        try {
            thread_count = Integer.parseInt(System.getProperty("threads"));
            System.out.println("using " + thread_count + " threads");
        } catch (Exception e) { /* ignore */ }

        thread_allocate = total_allocate / thread_count;
        thread_live = live_size / thread_count;
    }

    static long allocate_max(int size) {
        long total = 0;
        try {
            ArrayList a = new ArrayList();
            while (true) {
                a.add(new byte[size]);
                total += size + 12;
            }
        } catch (OutOfMemoryError e) {}
        return total;
    }

    public static void trace(Object o) {
        System.out.print(o);
        System.out.flush();
    }
}
