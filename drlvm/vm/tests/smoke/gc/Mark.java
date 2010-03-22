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

import java.util.Random;

/**
 * Measures the throughput and the pause times on
 * random object allocation.
 * The test is very slow on interpreter and may take more than 5 minutes
 * to complete.
 */
public class Mark extends Thread {

    static Mark workers[];
    static Mark sleepers[];

    final static long kb = 1024;
    final static long Mb = 1024*kb;
    final static long Gb = 1024*Mb;
    
    static int sleeper_number = 10;
    static int worker_number = 2;
    static long allocate_amount = 400*Mb;
    static long live_amount = 100*Mb;

    static long pause_threshold = 120;
    static long throughput_pause_threshold = 400;

    static boolean verbose = false;
    static boolean report_on_pause = false;
    static int report_interval = 1000;

    public static void main(String[] args) {
        init();

        Thread reporter = new Reporter();
        reporter.start();

        workers = new Mark[worker_number];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Mark(
                allocate_amount/worker_number, 
                live_amount/worker_number);
            workers[i].start();
        }

        sleepers = new Mark[sleeper_number];
        for (int i = 0; i < sleepers.length; i++) {
            sleepers[i] = new Mark(0,0);
            sleepers[i].setDaemon(true);
            sleepers[i].start();
        }

        for (int i = 0; i < workers.length; i++) {
            try {
                workers[i].join();
            } catch (Exception e) {}
        }

        try {
            reporter.interrupt();
            reporter.join();
        } catch (Exception e) {}

        System.out.println("PASSED");
    }

    static void init() {
        try {
            int n = Integer.parseInt(System.getProperty("workers"));
            if (n > 0 && n < 10000) {
                worker_number = n;
            }
        } catch (Exception e) {}

        try {
            int n = Integer.parseInt(System.getProperty("threads"));
            if (n > 0 && n < worker_number) {
                sleeper_number = n - worker_number;
            }
        } catch (Exception e) {}

        try {
            int n = Integer.parseInt(System.getProperty("sleepers"));
            if (n > 0 && n < 10000) {
                sleeper_number = n;;
            }
        } catch (Exception e) {}

        try {
            long n = parseSize(System.getProperty("allocate"));
            if (n > 0)
                allocate_amount = n;
        } catch (Exception e) {}
        
        try {
            long n = parseSize(System.getProperty("live"));
            if (n > 0)
                live_amount = n;
        } catch (Exception e) {}

        try {
            int n = Integer.parseInt(System.getProperty("interval"));
            if (n > 0)
                report_interval = n;
        } catch (Exception e) {}

        verbose = (System.getProperty("silent") == null);
        report_on_pause = (System.getProperty("report_on_pause") != null);

        if (verbose) {
            System.out.println("allocating " + mb(allocate_amount) + " Mb"
                + " on " + worker_number + " workers with " + sleeper_number + " sleepers"
                + ", live size " + mb(live_amount) + " Mb"
                + ", pause threshold " + pause_threshold + " ms"
            );
        }
    }

    public static long parseSize (String x) throws NumberFormatException {
        int len = x.length();
        long factor = 1;
        switch (x.charAt(len-1)) {
            case 'k': factor = kb; len--; break;
            case 'm': factor = Mb; len--; break;
            case 'g': factor = Gb; len--; break;
        }
        long result = Long.parseLong(x.substring(0,len));
        return factor * result;
    }

    public static long mb (long size) {
        return (size + Mb/2)/Mb;
    }

    // benchmarking information
    static long abs_max_pause = 0;
    static long max_pause = 0;
    static long min_pause = Long.MAX_VALUE;
    static long sum_pause = 0;
    static long sum_time = 0;
    static int num_pause = 0;

    static long min_throughput = Long.MAX_VALUE;
    static long abs_min_throughput = Long.MAX_VALUE;
    static long sum_min_throughput = 0;
    static int num_min_throughput = 0;
    static long sum_throughput = 0;

    static long last_system_pause_start = 0;
    static long last_system_pause_end = 0;
    static int num_system_pause_reports = 0;
    static long sum_system_pause = 0;
    static int num_system_pause = 0;
    static long start_time = System.currentTimeMillis();
    static long end_time = start_time;
    static long max_system_pause = 0;

    // per-thread throughput measurements
    long cumulative_amount = 0;
    long cumulative_time = 0;

    // last - end of the previous pause
    // start - start of the pause
    // end - end of the pause
    // amount - the amount allocated since last pause
    void record(long last, long start, long end, long amount) {

        synchronized (Mark.class) {

            long time = end - last;
            long pause = end - start;

            // system pause = intersection of pauses on all worker threads
            // we assume that the records are coming fairly quick

            if (start >= last_system_pause_end) {
                // We got new pause, abandon old data
                last_system_pause_start = start;
                last_system_pause_end = end;
                num_system_pause_reports = 1;
            } else {
                // We are dealing with the same pause
                // Refine the system pause boundaries
                if (start > last_system_pause_start)
                    last_system_pause_start = start;
                if (end < last_system_pause_end)
                    last_system_pause_end = end;
                num_system_pause_reports += 1;

                // Check if we had enough reports to consider
                // the pause system. Note, that if there is just 1 
                // worker thread, no system pause accounting will take place.
                if (num_system_pause_reports == worker_number) {
                    // record the system pause if it is valid
                    if (last_system_pause_start < last_system_pause_end) {
                        long system_pause = last_system_pause_end - last_system_pause_start;
                        sum_system_pause += system_pause;
                        num_system_pause += 1;

                        if (system_pause > max_system_pause)
                            max_system_pause = system_pause;

                        if (report_on_pause)
                            Mark.class.notify();
                    }
                } else if (num_system_pause_reports >= worker_number) {
                    System.out.println("WARNING: too many pause reports!");
                }
            }


            sum_time += time;
            sum_pause += pause;
            num_pause += 1;

            sum_throughput += amount; // use sum_time to get average per-thread throughput

            if (pause > max_pause) {
                max_pause = pause;
            }

            if (pause > abs_max_pause) {
                abs_max_pause = pause;
            }

            if (pause < min_pause) {
                min_pause = pause;
            }

            cumulative_time += time;
            cumulative_amount += amount;

            if (cumulative_time > throughput_pause_threshold) {
                long throughput = cumulative_amount / cumulative_time;
                if (throughput < min_throughput) {
                    min_throughput = throughput;
                }
                if (throughput < abs_min_throughput) {
                    abs_min_throughput = throughput;
                }

                cumulative_time = 0;
                cumulative_amount = 0;
            }

            end_time = end; // update the total clock time measurement

        } // synchronized (Mark.class)
    } // record()

    static class Reporter extends Thread {

        public void run() {
            while (!isInterrupted()) {
                synchronized (Mark.class) {
                    try {
                        Mark.class.wait(report_interval);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                report();
            }
            stats();
        }

        void report() {

            // nothing to report
            if (0 == num_pause) return;

            long now = System.currentTimeMillis();
            long avg_pause = sum_pause / num_pause;
            long avg_throughput = sum_throughput / sum_time;
            long sys_throughput = sum_throughput / (now - start_time);

            if (verbose) {

                long percent = 100l * sum_throughput / allocate_amount;

                String report = 
                    "Pause max " + max_pause + " ms, "
                        + (min_pause < Long.MAX_VALUE ? "min " + min_pause + " ms, " : "")
                        + "avg " + avg_pause + " ms, "
                        + "sys " + sum_system_pause + " ms";

                final String spaces = "                              ";
                // a little bit of report alignment
                int len = 60 - report.length();
                if (len < 0) len = 8 - report.length()%8;
                report += spaces.substring(0, len);

                if (sys_throughput > 0 || avg_throughput > 0) 
                    report += "Throughput (bytes/ms) "
                        + "avg " + avg_throughput + ", "
                        + "sys " + sys_throughput
                        + (min_throughput < Long.MAX_VALUE ? ", min " + min_throughput : "")
                    + " -- " + percent + "%";
                System.out.println(report);
            }

            sum_min_throughput += min_throughput;
            num_min_throughput += 1;

            min_throughput = Long.MAX_VALUE;
            min_pause = Long.MAX_VALUE;
            max_pause = 0;
        }

        void stats() {

            if (0 == num_pause) {
                System.out.println(
                    "No registered pauses with threshold " 
                    + pause_threshold + " ms");
                return;
            }

            long avg_pause = sum_pause / num_pause;
            long avg_throughput = sum_throughput / sum_time;
            long sys_throughput = sum_throughput / (end_time - start_time);

            System.out.println("=================");
            System.out.println(""
                + "time " + sum_time + " ms" 
                + " / " + worker_number + " worker threads, " 
                + sleeper_number + " sleeper threads, "
                + "clock " + (end_time - start_time) + " ms"
            );
            //System.out.println("Pause threshold " + pause_threshold + " ms");
            System.out.println(""
                + "thread pause " + sum_pause + " ms"
                + " / " + num_pause + ", "
                + "max " + abs_max_pause + " ms, "
                + "avg " + avg_pause + " ms"
                + ", threshold " + pause_threshold + " ms"
            );
            if (num_system_pause > 0) System.out.println(""
                + "system pause " + sum_system_pause + " ms"
                + " / " + num_system_pause + ", "
                + "max " + max_system_pause + " ms, "
                + "avg " + (sum_system_pause/num_system_pause) + " ms"
            );
            System.out.println("throughput (bytes/ms) "
                + "avg " + avg_throughput + ", "
                + "sys " + sys_throughput + ", "
                + (abs_min_throughput < Long.MAX_VALUE ? "min " + abs_min_throughput : "")
            );
            System.out.println("=================");
        }
    }

    boolean sleeper = false;

    long target; // how many bytes the thread needs to allocate
    long amount; // how many bytes the thread already allocated
    long live_target; // how many bytes the threads needs to keep alive
    long live = 0; // how many bytes the threads has live now
    Object links[][];

    public Mark(long allocate, long live) {

        if (0 == allocate) {
            sleeper = true;
        } else {
            this.target = allocate;
            this.live_target = live;

            // reserve memory to keep objects live
            links = new Object[2048][];
            int size = (int)(live_target / links.length / 64 + 1);
            for (int i = 0; i < links.length; i++) {
                links[i] = new Object[size];
            }

            live_target -= links.length * size * 4 + 12;
        }
    }

    public void run() {
        if (sleeper) sleep();
        amount = 0;
        long last_amount = amount;
        long last = System.currentTimeMillis();
        long start = 0, end = 0;
        while (amount < target) {
            start = System.currentTimeMillis();
            Object o = allocate();
            end = System.currentTimeMillis();
            amount += size(o);
            if (end - start > pause_threshold) {
                record(last, start, end, amount - last_amount);
                last_amount = amount;
                last = end;
            }
            handle(o);
        }
        record(last, start, end, amount - last_amount);
    }

    /// Mostly sleeping function
    void sleep() {
        while (true) {
            // discard the allocated object immediately
            allocate();
            try { 
                Thread.sleep(random.nextInt(100) + 1); 
            } catch (Exception e) {}
        }
    }

    void handle(Object o) {
        int size = size(o);
        // do nothing if the object is larger than live size target
        if (size > live_target) return;

        // store the object
        live += store(o);

        // free objects at random to keep live size around target
        while (live > live_target) {
            int freed = remove();
            if (0 == freed) break;
            live -= freed;
        }
    }

    // returns the delta of the live size
    int store(Object o) {
        int i = random.nextInt(links.length);
        int j = random.nextInt(links[i].length);
        int old_size = size(links[i][j]);
        links[i][j] = o;
        int size = size(links[i][j]);
        return (size - old_size);
    }

    // find one non-null element in links[][], 
    // starting from links[i][j]
    // remove it and return its size
    int remove(int i, int j) {
        while (i < links.length && j < links[i].length && null == links[i][j] ) {
            j++;
            if (j >= links[i].length) {
                j = 0;
                i++;
            }
        }

        if (i < links.length && j < links[i].length) {
            int size = size(links[i][j]);
            links[i][j] = null;
            return size;
        }

        return 0;
    }

    // removes one random element from links[][]
    // and returns its size
    int remove() {
        int i = random.nextInt(links.length);
        int j = random.nextInt(links[i].length);

        int size = remove(i,j);
        if (size > 0) return size;

        return remove(0,0);
    }

    static Random random = new Random();

    static Object allocate() {
        if (random.nextInt(10) < 4) {
            // array probability 0.4
            return allocateArray(random_size(array_size_distribution));
        } else {
            // regular object probability 0.6
            return allocateObject(random_size(object_size_disribution));
        }
    }

    /// logarithm base 2
    static int log(int n) {
        if (n < 0) n = -n;
        int l = 0;
        while (n > 1) {
            l += 1;
            n = n >> 1;
        }
        return l;
    }

    static int[] convert_probability_to_distribution(int[] probability) {
        int len = probability.length;
        int[] distribution = new int[len];
        for (int i = 1; i < len; i++) {
            distribution[i] = distribution[i-1] + probability[i];
        }
        return distribution;
    }

    static int random_size(int[] size_distribution) {
        int len = size_distribution.length;
        int limit = size_distribution[len-1];
        int n = random.nextInt(limit);
        int i;
        for (i = 1; i < array_size_distribution.length; i++) {
            if (n < array_size_distribution[i]) break;
        }
        if (i < len - 1) {
            // 0 - 240
            return (4 + 8*i);
        } else {
            // 248+
            int size = 256;
            if (random.nextInt(2) == 0) size *= 2; // 512
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 1024
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 2048
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 4096
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 8192
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 16384
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 32768
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 65536
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 131072
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 262144
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 512k
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 1024k
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 2048k
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 4096k
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 8M
            else return size;
            if (random.nextInt(2) == 0) size *= 2; // 16M
            return size;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // the data got from EHWA with -Dcharacterize_heap=on, ObjectSizeHistogram.txt, numArray###
    static int array_size_probability[] = new int[] {
        /* 0 */ 0, /* 8 */ 137, /* 16 */ 2087, /* ... */ 1188, 707, 646, 604, 620, 1216, 361, 458, 240, 1053, 387, 
        676, 624, 474, 333, 248, 176, 112, 123, 69, 58, 30, 36, 24, 25, 17, 12, /* 240 */ 12, /* 248+ */ 464
    };

    static int array_size_distribution[] = convert_probability_to_distribution(array_size_probability);

    static int object_size_probability[] = new int[] {
        /* 0 */ 0, /* 8 */ 1324, /* 16 */ 987, /* 24 */ 14610, /* 32 */ 405, 1737, 433, 14,
        66,45,10,4,1,4,3,2,0,0,0,0,0,0,6,0,0,3,0,0,0,0,0, /* 248+ */ 3
    };

    static int object_size_disribution[] = convert_probability_to_distribution(object_size_probability);

    ///////////////////////////////////////////////////////////////////////////////////////////

    static Object allocateArray(int size) {
        if (random.nextInt(3) == 0) return new byte[size-12];
        else if (random.nextInt(2) == 0) return new Object[(size-12)/4];
        else return new double[(size-12)/8];
    }

    static Object allocateObject(int size) {
        switch (size) {
            case 8: return new Object();
            case 16: return new Object16();
            case 24: return new Object24();
            case 32: return new Object32();
            case 40: return new Object40();
            case 48: return new Object48();
            case 56: return new Object56();
            case 64: return new Object64();
            case 72: return new Object72();
            case 80: return new Object80();
            case 88: return new Object88();
            case 96: return new Object96();
            case 104: return new Object104();
            case 112: return new Object112();
            case 120: return new Object120();
            case 128: return new Object128();
            case 136: return new Object136();
            case 144: return new Object144();
            case 152: return new Object152();
            case 160: return new Object160();
            case 168: return new Object168();
            case 176: return new Object176();
            case 184: return new Object184();
            case 192: return new Object192();
            case 200: return new Object200();
            case 208: return new Object208();
            case 216: return new Object216();
            case 224: return new Object224();
            case 232: return new Object232();
            case 240: return new Object240();
            case 248: return new Object248();
            case 256: return new Object256();
            default: return new Object264();
        }
    }

    //static byte_array_class = new byte[0].getClass();
    static int size(Object o) {
        if (null == o) return 0;
        if (o instanceof byte[]) {
            byte[] b = (byte[]) o;
            return b.length + 12;
        } else if (o instanceof Object[]) {
            Object[] a = (Object[])o;
            return a.length*4 + 12;
        } else if (o instanceof double[]) {
            double[] a = (double[])o;
            return a.length*8 + 12;
        } else if (o instanceof Object264) return 264;
        else if (o instanceof Object256) return 256;
        else if (o instanceof Object248) return 248;
        else if (o instanceof Object240) return 240;
        else if (o instanceof Object232) return 232;
        else if (o instanceof Object224) return 224;
        else if (o instanceof Object216) return 216;
        else if (o instanceof Object208) return 208;
        else if (o instanceof Object200) return 200;
        else if (o instanceof Object192) return 192;
        else if (o instanceof Object184) return 184;
        else if (o instanceof Object176) return 176;
        else if (o instanceof Object168) return 168;
        else if (o instanceof Object160) return 160;
        else if (o instanceof Object152) return 152;
        else if (o instanceof Object144) return 144;
        else if (o instanceof Object136) return 136;
        else if (o instanceof Object128) return 128;
        else if (o instanceof Object120) return 120;
        else if (o instanceof Object112) return 112;
        else if (o instanceof Object104) return 104;
        else if (o instanceof Object96) return 96;
        else if (o instanceof Object88) return 88;
        else if (o instanceof Object80) return 80;
        else if (o instanceof Object72) return 72;
        else if (o instanceof Object64) return 64;
        else if (o instanceof Object56) return 56;
        else if (o instanceof Object48) return 48;
        else if (o instanceof Object40) return 40;
        else if (o instanceof Object32) return 32;
        else if (o instanceof Object24) return 24;
        else if (o instanceof Object16) return 16;
        else return 8; /// XXX all unknown objects will be considered Objects (size == 8)
    }

    static class Object16 { Object f16; byte b16; }
    static class Object24 extends Object16 { double f24; }
    static class Object32 extends Object24 { Object f32; Object ff32; }
    static class Object40 extends Object32 { Object f40; short s40; }
    static class Object48 extends Object40 { long f48; }
    static class Object56 extends Object48 { Object f56; byte b56; }
    static class Object64 extends Object56 { long f64; }
    static class Object72 extends Object64 { double f72; }
    static class Object80 extends Object72 { Object f80; int i80; }
    static class Object88 extends Object80 { Object f88; int i88; }
    static class Object96 extends Object88 { long f96; }
    static class Object104 extends Object96 { Object f104; Object o104; }
    static class Object112 extends Object104 { Object f112; byte b112; }
    static class Object120 extends Object112 { long f120; }
    static class Object128 extends Object120 { long f128; }
    static class Object136 extends Object128 { Object f136; int i136; }
    static class Object144 extends Object136 { double f144; }
    static class Object152 extends Object144 { Object f152; Object o152; }
    static class Object160 extends Object152 { double f160; }
    static class Object168 extends Object160 { Object f168; Object o168; }
    static class Object176 extends Object168 { Object f176; byte b176; }
    static class Object184 extends Object176 { long f184; }
    static class Object192 extends Object184 { Object f192; Object[] a192; }
    static class Object200 extends Object192 { double f200; }
    static class Object208 extends Object200 { Object f208; Object[] a208; }
    static class Object216 extends Object208 { long f216; }
    static class Object224 extends Object216 { Object f224; int i224; }
    static class Object232 extends Object224 { double f232; }
    static class Object240 extends Object232 { Object f240; byte b240; }
    static class Object248 extends Object240 { Object f248; Object o248; }
    static class Object256 extends Object248 { double f256; }
    static class Object264 extends Object256 { Object f264; Object[] a264; }
}
