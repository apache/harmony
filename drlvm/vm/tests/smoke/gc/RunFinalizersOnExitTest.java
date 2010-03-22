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
 * The test for Run Finilazers on exit for 3 types of object.
 */
public class RunFinalizersOnExitTest {

    private static final int ALIVE_QUANTITY = 128;
    private static final int PHANTOM_QUANTITY = 128;
    private static final int DIED_QUANTITY = 128;

    private static int aliveFinalized = 0;
    private static int phantomFinalized = 0;
    private static int diedFinalized = 0;

    private static AliveFinalizer aliveArray[];
    private static PhantomFinalizer phantomArray[];

    private static Object counterLock = new Object();

    private static boolean stop = false;

    private static class AliveFinalizer {
        protected void finalize() {            
            synchronized(counterLock ) {
            aliveFinalized ++;
                updateInfo();
                checkPass();
            }
        }
    }

    private static class PhantomFinalizer {
        protected void finalize() {            
            synchronized(counterLock ) {
            phantomFinalized ++;
                updateInfo();
                checkPass();
            }
        }
    }

    private static class DiedFinalizer {
        protected void finalize() {            
            synchronized(counterLock ) {
        if (phantomArray[0] != null) {
                    for (int i=0; i<PHANTOM_QUANTITY; i++) {
                        phantomArray[i] = null;
                    }
                }
            diedFinalized ++;
                updateInfo();
                checkPass();
            }
        }
    }

    public static void main(String[] args) {
    updateInfo();
        System.runFinalizersOnExit(true);   

        aliveArray = new AliveFinalizer[ALIVE_QUANTITY];
        phantomArray = new PhantomFinalizer[PHANTOM_QUANTITY];

        for (int i=0; i<DIED_QUANTITY; i++) {
            new DiedFinalizer();
        }

        for (int i=0; i<ALIVE_QUANTITY; i++) {
            aliveArray[i] = new AliveFinalizer();
        }

        for (int i=0; i<PHANTOM_QUANTITY; i++) {
            phantomArray[i] = new PhantomFinalizer();
        }

    }

    private static void checkPass() {
       if ((!stop) &&
                (aliveFinalized == ALIVE_QUANTITY) &&
                (phantomFinalized == PHANTOM_QUANTITY ) &&
                (diedFinalized == DIED_QUANTITY)) {
            stop = true;
            System.err.println("\nPASSED");
            System.err.flush();
            System.out.println("\nPASSED");
            System.out.flush();
        } else if ((!stop) &&
                (aliveFinalized  > 0) &&
                (diedFinalized < DIED_QUANTITY)) {
            stop = true;
            System.err.println("\nFAILED");
            System.err.flush();
            System.out.println("\nFAILED");
            System.out.flush();
        } else if ((!stop) &&
                (phantomFinalized  > 0) &&
                (diedFinalized < DIED_QUANTITY)) {
            stop = true;
            System.err.println("\nFAILED");
            System.err.flush();
            System.out.println("\nFAILED");
            System.out.flush();
        }
    }

    private static void updateInfo() {
        int alivePercent = 100 * aliveFinalized / (ALIVE_QUANTITY);
        int phantomPercent = 100 * phantomFinalized / (PHANTOM_QUANTITY);
        int diedPercent = 100 * diedFinalized / (DIED_QUANTITY);

        if (!stop) {

            System.err.print("\r(" + alivePercent);
            System.err.print("%, " + phantomPercent);
            System.err.print("%, " + diedPercent);
            System.err.print("%)");
            System.err.flush();    
        }
    }
}

