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

package thread;

public class ManyThreadsTest implements Runnable{
    private static final int MAX_ITERATION_NUMBER = 100000;
    private static final int PRINT_ITERATION_NUMBER = 10000;
    public static final int MAX_THREADS_COUNTITY = 100;

    private static int threadCounters[] = new int[MAX_THREADS_COUNTITY];
    private static Thread threads[] = new Thread[MAX_THREADS_COUNTITY];

    public static void main(String args[]) throws Exception {
        
        for (int i = 0; i < MAX_ITERATION_NUMBER;) {
            ////System.out.println("Iteration = " + i);

            for (int l = i + PRINT_ITERATION_NUMBER; i < l; ++i) {
                int t = i % MAX_THREADS_COUNTITY;

                if (null != threads[t]){
                    threads[t].join();
                }
                threads[t] = new Thread(new ManyThreadsTest(t));
                threads[t].start();
            }
			allocObjs();
        }

        System.out.println("PASS");
    }

    private int index;

    public ManyThreadsTest(int index){
        this.index = index;
    }

    public void run(){
        threadCounters[index]++;
    }

    public static void allocObjs() {
	for (int xx = 0; xx < 100; xx++) {
		int ia [] = new int[1024 * 1024];
	}
    }
}

