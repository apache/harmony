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

class FinalizeStackTest {
    private static final int MAX_DEPTH = 1000000;

    FinalizeStackTest(int c) {
        if (c > 0) {
            new FinalizeStackTest(c - 1);
        } else {
            System.out.println("PASS");
        }
    }

    protected void finalize() {
        // empty
    }

    public static void main(String[] args) {
        try {
            new FinalizeStackTest(MAX_DEPTH);
        } catch (StackOverflowError soe) {
            System.out.println("PASS : " + soe);
        } catch (Throwable th) {
            System.out.println("FAIL");
        }
    }
}
