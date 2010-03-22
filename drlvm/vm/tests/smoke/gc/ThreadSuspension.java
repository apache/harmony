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
 * GC tests have no dependecy on class library.
 *
 * VM currently is not able to suspend thread which is running
 * a computation loop.
 */
public class ThreadSuspension implements Runnable {

    static boolean passed = false;
    static boolean suspended2 = true;
    static boolean suspended3 = true;
    static Thread x1, x2, x3;

    public static void main (String[] args) {
        x1 = new Thread(new ThreadSuspension(1)); 
        x1.setDaemon(true); x1.start();
        x2 = new Thread(new ThreadSuspension(2)); 
        x2.setDaemon(true); x2.start();
        x3 = new Thread(new ThreadSuspension(3)); 
        x3.setDaemon(true); x3.start(); 
        //wait for large timeout interval
        try { 
            synchronized(x1) {
                x1.wait();
            } 
        } catch (Throwable e) {}
        if (passed) {
			//VM successfully suspended the threads
            trace("PASS");
        } else {
            trace("FAIL");
        }
    }

    public ThreadSuspension(int n) {
        number = n;
    }

    public void run() {
        switch (number) {
            case 1:
                try { Thread.sleep(1000); } catch (Throwable e) {}
                trace("forcing gc after 1 s delay");
                System.gc();
                if(suspended2 && suspended3)
                {
					//VM suspendeded both the threads
					trace("gc completed");
					passed = true;
				}
				else
					passed = false;
                synchronized (x1) {
                    x1.notify();
                }
                break;
            case 2:
                int j =0;
                trace("-- starting unsuspendable computation --");
                for (int i=0; i<1000000000; i++) {
                    j = 1000 + j/(i+1);
                }
                trace("-- unsuspendable computation finished --");
                suspended2 = false;
                break;
            case 3:
                trace("-- starting suspendable computation --");
                for (int i=0; i<1000000000; i++) {
                    Thread.yield();
                }
                trace("-- suspendable computation finished --");
                suspended3 = false;
                break;
        }
    }

    public synchronized static void trace(Object o) {
        System.out.println(o);
        System.out.flush();
    }

    int number; // the number of the thread
}
