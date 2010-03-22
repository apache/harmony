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
 * @author Alexei Fedotov
 */  
package stress;

public class Stack {

    // the minimal reference limit for DRLVM on Windows/Interpreter
    // which does not cause StackOverflowError
    static final int minDepth = 130;
    // the maximal reference limit for DRLVM on linux/OPT
    // which cause StackOverflowError
    static int depth = 75000;
    static final int maxDepth = 12000000;

    synchronized boolean testMinDepth(int i) {
        System.out.println("" + i);
        if (i < minDepth && testMinDepth(++i)) {
            return true;
        }
        return true;
    }

    synchronized boolean test(int i) {
        System.out.println(i);
        if (i < depth && test(++i)) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        boolean pass = false;
        boolean minDepthPass = false;
            
        Stack test = new Stack();
        
        // check that StackOverflowError is NOT thrown 
        // for the predefined minimal number of iterations 
        try {
            test.testMinDepth(0);
            System.out.println("Test passed for " + minDepth + " iterations\n");
            minDepthPass = true;
        } catch (StackOverflowError e) {
            System.out.println("\nStackOverflowError has been thrown too early");
        }

        // check that StackOverflowError is thrown 
        // after some reasonable number of iterations 
        if (minDepthPass) {
            try {
                while (!pass && depth <= maxDepth) {
                    pass = test.test(0);
                    depth *= 2;
                }
                System.out.println("StackOverflowError has not been thrown after " + depth / 2 + " iterations");
            } catch (Throwable e) {
                if (e instanceof StackOverflowError) {
                    System.out.println("\nGot expected StackOverflowError");
                    pass = true;
                } else {
                    System.out.println("\nGot unexpected exception: " + e.toString());
                    pass = false;
                }
            }
        }
        System.out.println("Stack test " + (pass ? "passed" : "failed"));
    }
}
