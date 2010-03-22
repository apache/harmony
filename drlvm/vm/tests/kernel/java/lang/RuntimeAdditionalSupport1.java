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
 * @author Serguei S.Zapreyev
 */

package java.lang;

import junit.framework.TestCase;
/*
 * Created on 03.29.2006
 *
 * This RuntimeAdditionalSupport1Test class is used to support 
 * RuntimeAdditionalSupport2Test
 */

public class RuntimeAdditionalSupport1 extends TestCase {
	public static boolean openingFlag = false; // to avoid tests failures because of 1.5 eclipse compiler bugs
	public static void main(String[] args) {
		Runtime.getRuntime().exit(Integer.parseInt(args[0]));
	}
	
    /**
     * stupid test to cheat the tutor
     */
    public void test_1() {
	}
}
