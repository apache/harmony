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

package Breakpoint1;

import junit.framework.TestCase;

public class Breakpoint1 extends TestCase {
    static boolean status = false;

    static public void main(String args[]) {
        Breakpoint1 instance = new Breakpoint1();
        instance.test();
        instance.test();
        if(status) {
            System.out.println("Test passed!");
        } else {
            System.out.println("Test failed!");
        }
        assertTrue(status);        
        return;
    }

    public void test() {
        /*
         * Simple sequention of operations for
         * creating breakpoints here.
         */
        int a = 2;
        int b = a + 3;
        int c = b + 4;
        int d = c + 5;
        int e = d + 6;
        int f = e + 7;
        int g = f + 9;
        int h = g + 10;
        int i = h + 11;
        int j = i + 12;
        assertTrue(status);
        return;
    }
}


