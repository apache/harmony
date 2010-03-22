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
 * @author Pavel Rebriy
 */

package SingleStep1;

import junit.framework.TestCase;

public class SingleStep1 extends TestCase {

    static boolean status = false;

    public static void main(String[] args) {
        new SingleStep1().test();
        if (status) {
            System.out.println("Test passed!");
        } else {
            System.out.println("Test failed!");
        }
        assertTrue(status);
    }

    public void test() {
        // set the first breakpoint to the next line
        Object obj = new Object();
        int z = 10;
        z = z * 2;
        // set the second breakpoint to the next line
        System.out.println("done: " + z + obj);
        assertTrue(status);
        return;
    }
}
