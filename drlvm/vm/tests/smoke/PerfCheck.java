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
 * @author Vera Volynets
 */  

import java.lang.reflect.*;

/**
 * This java file starts benchmarks and measures run time. 
 * @keyword XXX_harness
 */
public class PerfCheck {
    public static void main (String[] args) {
	String test;
	// construct the list of tests
       	if (args.length > 0) {
	    // from command-line arguments
	    test = args[0];
	    runTest(test);
	} else {
	    System.out.println("Please enter test to run!");
       	}
    }
 
    public static boolean runTest (String test) {
        trace("The benchmark is: " + test);
        try {
            Class test_class = Class.forName(test);
            Method main = test_class.getMethod("main", 
                new Class[] { String[].class });
	    long itime = System.currentTimeMillis();	
            main.invoke(null, new Object[] { new String[] {} });
	    System.out.println("The test run: " 
		+ (System.currentTimeMillis() - itime) + " ms");
        } catch (Throwable e) {
            System.out.println(test + " failed, " + e);
            return false;
        }
        return true;
    }

    public static void trace (Object o) {
        System.out.println(o);
        System.out.flush();
    }
}
