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
 * @author Pavel Afremov
 */

package gc;

/**
 * Tests synchronized finalizers in multiple threads.
 */
public class SynchronizedFinalizersTest implements Runnable {
    int n;
    byte[] array;

    static final int N_THREADS = 100;
    static final int N_OBJECTS = 100;
    static final int OBJECT_SIZE = 20000;

    static int started = 0;
    static int finished = 0;
    static int created = 0;
    static int finalized = 0;

    static boolean stop = false;

    public SynchronizedFinalizersTest(int nn) {
        n=nn;
        synchronized(SynchronizedFinalizersTest.class) {
            created++;
            array = new byte[OBJECT_SIZE];
            checkPass();
        }
    }

    public static void main(String[] args) {
	updateInfo();  	

        for (int i=0;i<N_THREADS ;i++) {
            new Thread(new SynchronizedFinalizersTest(i)).start();
        }
    }
 
    public void run() {
        try {
            synchronized(SynchronizedFinalizersTest.class) {
                started++;
                updateInfo();
            }

            for (int i=0;i<N_OBJECTS;i++) {
                synchronized(SynchronizedFinalizersTest.class) {
					new SynchronizedFinalizersTest(n*N_OBJECTS+i);
				}

                if (stop) {
                    break;
                }
            }
        } catch (Throwable e) {
	    if (!stop) {
	        stop = true;
                System.err.println("Err:"+e+" in "+n);                                
                System.err.println("FAILED");                                
                System.err.flush();
                System.out.println("Err:"+e+" in "+n);                                
                System.out.println("FAILED");                                
                System.out.flush();
            }
        } finally {
            synchronized(SynchronizedFinalizersTest.class) {
                finished++;
                updateInfo();
                checkPass();
            }
        }
    }
 
    protected void finalize() {            
        synchronized(SynchronizedFinalizersTest.class) {
            finalized ++;
            updateInfo();
            checkPass();
        }
    }

    private static void checkPass() {
	if ((!stop) &&
                (started == N_THREADS) &&
                (finished == N_THREADS) &&
                (created == (N_THREADS * (N_OBJECTS+1)))) {
	    stop = true;
            System.err.println("\nPASSED");
            System.err.flush();
            System.out.println("\nPASSED");
            System.out.flush();
	}
    }
    private static void updateInfo() {
	if (!stop) {
            int startedPercent = 100 * started / (N_THREADS);
            int finishedPercent = 100 * finished / (N_THREADS);
            int createdPercent = 100 * created / (N_THREADS * (N_OBJECTS+1));
            int finalizedPercent = 100 * finalized / (N_THREADS * (N_OBJECTS+1));

            System.err.print("\r(" + startedPercent);
            System.err.print("%, " + finishedPercent);
            System.err.print("%, " + createdPercent);
            System.err.print("%, " + finalizedPercent);
            System.err.print("%)");
            System.err.flush();
        }    
    }
}

