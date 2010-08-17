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

package javax.imageio.plugins.jpeg;

import junit.framework.TestCase;

public class JPEGQTableTest extends TestCase {
	
	public void testToString() {
		String K1Luminance = "JPEGQTable:\n" +
							 "\t16 11 10 16 24 40 51 61 \n" +
							 "\t12 12 14 19 26 58 60 55 \n" +
							 "\t14 13 16 24 40 57 69 56 \n" +
							 "\t14 17 22 29 51 87 80 62 \n" +
							 "\t18 22 37 56 68 109 103 77 \n" +
							 "\t24 35 55 64 81 104 113 92 \n" +
							 "\t49 64 78 87 103 121 120 101 \n" +
							 "\t72 92 95 98 112 100 103 99 \n";
		
		String K2Chrominance = "JPEGQTable:\n" +
							   "\t17 18 24 47 99 99 99 99 \n" +
							   "\t18 21 26 66 99 99 99 99 \n" +
							   "\t24 26 56 99 99 99 99 99 \n" +
							   "\t47 66 99 99 99 99 99 99 \n" +
							   "\t99 99 99 99 99 99 99 99 \n" +
							   "\t99 99 99 99 99 99 99 99 \n" +
							   "\t99 99 99 99 99 99 99 99 \n" +
							   "\t99 99 99 99 99 99 99 99 \n";
						 
		assertEquals(K1Luminance, JPEGQTable.K1Luminance.toString());
		assertEquals(K2Chrominance, JPEGQTable.K2Chrominance.toString());
	}
	
}
