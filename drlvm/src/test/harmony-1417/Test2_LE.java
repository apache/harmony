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

public class Test2_LE { 
    static int num = 0;
    static int ln = 10000000;
    static int cln = ln/5;
    public static void main(String[] args) { 
        System.out.println("Start Test_LE:");
        Test2_LE test = new Test2_LE();
        long start1, start2, end1, end2, time1, time2;

        start1 = System.currentTimeMillis();
        for(int i=0; i<ln; i++) {
            try {
                if (i%cln == 0)
                    System.out.println("...");
                test.test();
            } catch(Exception e) {
            }
        }
        end1 = System.currentTimeMillis();
        System.out.println("Total time: " + (time1=end1-start1));

        start2 = System.currentTimeMillis();
        for(int i=0; i<ln; i++) {
            try {
                if (i%cln == 0) 
                    System.out.println("...");
                test.test2();
            } catch(Exception e) {
            }
        }
        end2 = System.currentTimeMillis();
        System.out.println("Total time: " + (time2=end2-start2));
        if (time2/time1 > 1)
            System.out.println("Test passed " + time2/time1);
        else
            System.out.println("Test failed " + time2/time1);
    }
    void test() throws Exception {
        Exception e = new Exception();
        throw e; 
    }
    void test2() throws Exception {
        Exception e = new Exception();
	if (e.getMessage()!=null)
	    System.out.println("null");
        throw e;
    }
} 
