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

import java.io.*;
import java.util.*;

public class Mix extends Thread {

    static Object sync;
    static int period = 8000;
    static long end = 0;

    public static void main(String[] args) {
        if (args.length == 1) {
            thread_number = Integer.parseInt(args[0]);
        }
        sync = Thread.currentThread();
        end = System.currentTimeMillis() + period;
        Thread c = new Mix("concurrent");
        c.start();
        Thread s = new Mix("occupy");
        s.start();
        try {
            c.join();
            s.join();
            System.out.println("\nPASSED");
            System.out.flush();
        } catch (InterruptedException e) {
            System.out.println("\nFAIL, " + e);
        }
    }

    String id;

    public Mix(String id) {
        this.id = id;
    }

    public void run() {
        try {
            if (id.startsWith("compute")) {
                compute();
            } else if (id.startsWith("load")) {
                load();
            } else if (id.startsWith("sleep")) {
                sleep();
            } else if (id.startsWith("exceptions")) {
                exceptions();
            } else if (id.startsWith("deep_exceptions")) {
                deep_exceptions();
            } else if (id.startsWith("concurrent")) {
                concurrent();
            } else if (id.startsWith("allocate")) {
                allocate();
            } else if (id.startsWith("occupy")) {
                occupy();
            } else if (id.startsWith("spawn")) {
                spawn();
            } else if (id.startsWith("contended")) {
                contended(sync);
            } else if (id.startsWith("uncontended")) {
                uncontended();
            } else if (id.startsWith("nothing")) {
                /* do nothing */
            } else {
                error("Unknown thread type: " + id);
            }
        } catch (Throwable e) {
            trace("\n" + id + " terminated by " + e + "\n");
            e.printStackTrace();
        }
    }

    static Random random = new Random(0);
    static String selectThreadType(int i) {
        switch (i % 9) {
            case 0: return "uncontended";
            case 1: return "contended";
            case 2: return "contended";
            case 3: return "deep_exceptions";
            case 4: return "compute";
            case 5: return "spawn";
            case 6: return "allocate";
            case 7: return "load";
            case 8: return "exceptions";
        }
        return "nothing";
    }

    static int thread_number = 60;

    /** Create and maintain a fixed number of concurrent threads. */
    public void concurrent() {
        trace("C");
        Thread[] threads = new Thread[thread_number];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Mix("sleep" + i);
            threads[i].start();
        }
        while (System.currentTimeMillis() < end) {
            for (int i = 0; i < threads.length; i++) {
                if (threads[i].isAlive()) continue;
                threads[i] = new Mix(selectThreadType(i) + i);
                threads[i].start();
            }
            sleep(1);
        }
        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {}
        }
    }

    static int uncontended_period = period/5;
    /** Uncontended synchronization. */
    public void uncontended() {
        trace("u");
        while (System.currentTimeMillis() < end) {
            synchronized (this) {
                nothing();
            }
            nothing();
        }
    }

    static int contended_period = period/5;
    /** Uncontended synchronization. */
    public void contended(Object sync) {
        trace("c");
        while (System.currentTimeMillis() < end) {
            synchronized (sync) {
                nothing();
            }
            nothing();
        }
    }

    /** return a byte array for the class. */
    public byte[] getClassbytes(String classname) {
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            in = getClass().getClassLoader().getResourceAsStream(classname);
            out = new ByteArrayOutputStream();
            byte buf[] = new byte[1024];
            int n = in.read(buf);
            while (n > 0) {
                out.write(buf,0,n);
                n = in.read(buf);
            }
            return out.toByteArray();
        } catch (IOException e) {
            trace(e);
            return null;
        } finally {
            if (null != in) try { in.close(); } catch (Throwable e) {}
            if (null != out) try { out.close(); } catch (Throwable e) {}
        }
    }

    /** Throw excepions repeatedly. */
    public void exceptions() {
        trace("e");
        for (int i = 0; i < 100000; i++) {
            if (System.currentTimeMillis() > end) break;
            try {
                ((String)null).length();
            } catch (NullPointerException e) {}
            int z;
            try {
                for (int x = 3; x >= 0; x--) {
                    z = 10/x;
		    z = 10/z;
                }
            } catch (ArithmeticException e) {}
            try {
                throw new RuntimeException("runtime exception");
            } catch (RuntimeException e) {}
        }
    }

    /** Helper function for throwing exceptions from a deep stack. Synchronized. */
    public synchronized void deep_exception(int depth) {
        if (depth <= 0) {
            throw new RuntimeException("deep exception");
        } else {
            deep_exception(depth-1);
        }
    }

    static int deep_exception_depth = 100;
    static int deep_exception_times = 1000;
    /** Throw exceptions from a deep stack. */
    public void deep_exceptions() {
        trace("d");
        for (int i = 0; i < deep_exception_times; i++) {
            if (System.currentTimeMillis() > end) break;
            try {
                deep_exception(deep_exception_depth);
            } catch (Exception e) {}
        }
    }

    /** maximum number of classes to load. */
    static int load_limit = 10000;

    /** Load class repeatedly. */
    public void load() throws ClassNotFoundException {
        trace("l");
        final byte classbytes[] = getClassbytes("stress/Mix.class");
        while (System.currentTimeMillis() < end) {

            // count loaded classes globally
            synchronized (getClass()) {
                if (load_limit <= 0) return;
                load_limit--;
            }

            new ClassLoader(getClass().getClassLoader()) {
                public Class loadClass(String name) throws ClassNotFoundException {
                    if (name.equals("stress.Mix"))
                        return defineClass(name, classbytes, 0,
                           classbytes.length);
                    else
                        return super.loadClass(name);
                }
            }.loadClass("stress.Mix");

            // slow down if we almost reached the limit
            if (load_limit < 1000) {
                try {
                    Thread.sleep(100/(load_limit+1) + 1);
                } catch (Throwable e) { /* ignore */ }
            }
        }
    }

    public Object allocate_chunk(int size) {
        return new byte[size];
    }

    static int spawn_period = period;
    /** Start new threads at high rate. */
    public void spawn() {
        trace("s");
        while (System.currentTimeMillis() < end) {
            Thread n = new Mix("nothing");
            n.start();
        }
    }

    static long allocate_size = 512*1048576;
    static int allocate_chunk_size = 512;
    static int allocate_chunk_list_size = 10;
    /** Allocates garbage at high rate. */
    public void allocate() {
        trace("a");
        List a = new LinkedList();
        long allocated = 0;
        while (allocated < allocate_size) {
            if (System.currentTimeMillis() > end) break;
            a.add(allocate_chunk(allocate_chunk_size));
            allocated += allocate_chunk_size;
            if (a.size() > allocate_chunk_list_size) a.remove(0);
        }
    }

    static int occupy_period = period;
    static long occupy_size = 96*1048576;
    static int occupy_chunk_size = 100000;
    /** Keeps heap occupied with low allocation rate. */
    public void occupy() {
        trace("o");
        List a = new LinkedList();
        long live_size = 0;
        while (live_size < occupy_size) {
            a.add(allocate_chunk(occupy_chunk_size));
            live_size += occupy_chunk_size;
        }

        while (System.currentTimeMillis() < end) {
            sleep(1);
            a.add(allocate_chunk(occupy_chunk_size));
            a.remove(0);
        }
    }

    // dummy placeholder for computation result (to avoid optimizations)
    static int result;
    static int compute_limit = 100000;
    /** Computataional cycle. */
    public void compute() {
        trace("+");
        int s = 0;
        for (int i = 0; i < compute_limit; i++) {
            if (System.currentTimeMillis() > end) break;
            s += i + compute_limit/(1+i);
            Thread.yield();
        }
        result = s;
    }

    static int sleep_interval = 300;
    /** Sleeper. */
    public void sleep() {
        trace(".");
        sleep(sleep_interval);
    }

    /** sleep. */
    public static void sleep(int ms) {
        try { 
            Thread.sleep(ms);
        } catch (InterruptedException e) {}
    }

    /** do nothing. */
    public static void nothing() {
    }

    public static void error(Object message) {
        System.out.println("FAILED, " + message);
        System.exit(1);
    }

    public static void trace(Object o) {
        System.err.print(o);
        System.err.flush();
    }
}
