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
package exception;

/**
 * Testt checks that reported stack deth is right
 */
class ExceptionStackTest {
    // Large enought but not too for test performace reasona
    private static final int MAX_DEPTH = 100;

    // Constructor calls itself and throw runtime exception on max depth
    ExceptionStackTest(int c) {
        if (c > 0) {
            new ExceptionStackTest(c - 1);
        } else {
            throw new RuntimeException("Test");
        }
    }

    // run test and check that it throws exception with right stack depth
    public static void main(String[] args) {
        try {
            new ExceptionStackTest(MAX_DEPTH);
            System.out.println("FAIL");
        } catch (RuntimeException rte) {
            StackTraceElement[] stack = rte.getStackTrace();
            int stackLength = stack.length;

            if (stackLength < MAX_DEPTH) {
                System.out.println("FAIL : Stack length should be more "
                    + MAX_DEPTH + ", but it's " + stackLength);
            } else {
                System.out.println("PASS : Stack length is " + stackLength);
            }
        } catch (Throwable th) {
            System.out.println("FAIL");
        }
    }
}
