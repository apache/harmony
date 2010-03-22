/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.tests.tools.javac;

import junit.framework.TestCase;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.sun.tools.javac.Main;

public class MainTest extends TestCase {

    public void test_main() throws Exception {
        StringWriter out = new StringWriter();
        String testStr = "no_this_test.java";
        Main.compile(new String[]{testStr}, new PrintWriter(out));
        assertTrue("The output should have " + testStr, out.toString().contains(testStr));
    }

	public void test_nothing() {
		// bogus test
	}
}
