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

package j.l;
public class Test3 {
    public static void main(final String[] args) throws Exception {
        Package p = org.apache.xalan.Version.class.getPackage();
        if (p==null) {
            System.out.println("Error0: test didn't work");
            return;
        }
        System.out.println(p.getName());
        System.out.println(p.getSpecificationVersion());
        if (p.getSpecificationVersion()!=null) {
            try {
                p.isCompatibleWith("");
                System.out.println("Test failed (1)");
            } catch (NumberFormatException e) {
                System.out.println("Test case #1 passed");
            } catch (Throwable e) {
                  e.printStackTrace();
                  System.out.println("Test failed (2)");
            }
            try {
                System.out.println(p.isCompatibleWith(null));
                System.out.println("Test failed (3)");
            } catch (NullPointerException e) {
                System.out.println("Test case #2 passed");
            } catch (Throwable e) {
                  e.printStackTrace();
                  System.out.println("Test failed (4)");
            }
            try {
                System.out.println(p.isCompatibleWith("1"));
                System.out.println("Test case #3 passed");
            } catch (Throwable e) {
                e.printStackTrace();
                System.out.println("Test failed (5)");
            }
            try {
                System.out.println(p.isCompatibleWith("a"));
                System.out.println("Test failed (6)");
            } catch (NumberFormatException e) {
                System.out.println("Test case #4 passed");
            }
        } else {
                System.out.println("Test case #1 failed");
                System.out.println("Test case #2 failed");
                System.out.println("Test case #3 failed");
                System.out.println("Test case #4 failed");
        }
        System.out.println("-----------------------------------");
        p = Package.getPackage("j.l");
        System.out.println(p.getSpecificationVersion());
        try {
            p.isCompatibleWith("");
            System.out.println("Test failed (7)");
        } catch (NumberFormatException e) {
            System.out.println("Test case #5 passed");
        } catch (Throwable e) {
              e.printStackTrace();
              System.out.println("Test failed (8)");
        }
        try {
            System.out.println(p.isCompatibleWith(null));
            System.out.println("Test failed (9)");
        } catch (NumberFormatException e) {
            System.out.println("Test case #6 passed");
        } catch (Throwable e) {
              e.printStackTrace();
              System.out.println("Test failed (10)");
        }

        try {
            System.out.println(p.isCompatibleWith("1"));
            System.out.println("Test failed (11)");
        } catch (NumberFormatException e) {
            System.out.println("Test case #7 passed");
        } catch (Throwable e) {
              e.printStackTrace();
              System.out.println("Test failed (12)");
        }
        try {
            System.out.println(p.isCompatibleWith("a"));
            System.out.println("Test failed (13)");
        } catch (NumberFormatException e) {
            System.out.println("Test case #8 passed");
        } catch (Throwable e) {
              e.printStackTrace();
              System.out.println("Test failed (14)");
        }
    }
}
