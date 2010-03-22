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
 * A microbenchmark on trigonometric/logarithmic math methods.
 * On DRLVM, specific optimization can be switched on/off from commandline: 
 * -XX:jit.arg.Math_as_magic=true/false.
 */

public class MathTest {
	static void f() {
		Math.abs(-123987.1236d);
		Math.asin(0.7);
		Math.acos(0.7);		
		Math.log(123.123);
		Math.log10(123.123);
		Math.log1p(123.123);		
		Math.sin(12312.123);
		Math.cos(12312.123);
		Math.sqrt(234234.234234);
		Math.tan(234234.12342134);
		Math.atan(2347.234);
		Math.atan2(231.123, 0);
		Math.abs(-123.1231123f);				
	}
    public static void main(String[] args) {    	
    	System.out.println("Warmup started....");
    	for (int i = 0; i < 2500 * 2500; i ++) {
		     f();
		}
    	for (int i = 0; i < 250 * 2500; i ++) {
		     f();
		}
    	System.out.println("Warmup ended....");
    	long start = System.currentTimeMillis();		
		for (int i = 0; i < 2500 * 2500; i ++) {
    		f();
		}
		System.out.println("floor result: " + (System.currentTimeMillis() - start));	
	}
}
