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
 * @keyword 
 */
public class NPE extends Thread {

    final static int chunk_size = 4011;                 // allocation unit
    final static long megabyte = 1048576;               

    // total bytes to allocate
    final static long total_allocate = 1000 * megabyte;  
    // live set
    final static long live_size = 40 * megabyte;        


    public static void main (String[] args) {
        Thread allocator = new NPE(0);
        Thread exceptionist = new NPE(1);
        allocator.start();
        exceptionist.setDaemon(true);
        exceptionist.start();
        try {
            allocator.join();
        } catch (InterruptedException e) {}
        exceptionist.interrupt();
        System.out.println("PASSED");
    }

    int kind;

    public NPE (int kind) {
        this.kind = kind;
    }

    public void run () {
        switch (kind) {

            /* allocate much memory and cause repetitive garbage collections */
            case 0:
                long count = total_allocate / chunk_size;
                /* maintain live set in [live_size/2 , live_size] */
                long reset = live_size / chunk_size / 2;
                ArrayList list = new ArrayList();
                for (long i = 0; i < count; i++) {
                    list.add(new byte[chunk_size]);
                    if (i % reset == 0) {
                        System.out.print("."); System.out.flush();
                        list = new ArrayList();
                    }
                }
                break;

            /* cause null pointer exceptions repeatedly */
            case 1:
                final int yield_frequency = 1000;
                int c = yield_frequency;
                while (!isInterrupted()) {
                    throwNPE();
                    c--;
                    if (c <= 0) {
                        System.out.print(","); System.out.flush();
                        c = yield_frequency;
                        Thread.yield();
                    }
                }
        }
    }


    void throwNPE() {
        try {
            ((String) null).length();
        } catch (NullPointerException e) { /* ignored */ }
    }

}
