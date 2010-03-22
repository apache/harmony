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

package tests.api.javax.security.auth.x500;

import javax.security.auth.x500.X500Principal;

public class X500PrincipalTest extends junit.framework.TestCase {

	/**
	 * @tests javax.security.auth.x500.X500Principal#X500Principal(java.lang.String)
	 */
	public void test_ConstructorLjava_lang_String() {
		X500Principal principal = new X500Principal(
				"CN=Hermione Granger, O=Apache Software Foundation, OU=Harmony, L=Hogwarts, ST=Hants, C=GB");
		String name = principal.getName();
		String expectedOuput = "CN=Hermione Granger,O=Apache Software Foundation,OU=Harmony,L=Hogwarts,ST=Hants,C=GB";
		assertEquals("Output order precedence problem", expectedOuput, name);
	}

	/**
	 * @tests javax.security.auth.x500.X500Principal#getName(java.lang.String)
	 */
	public void test_getNameLjava_lang_String() {
		X500Principal principal = new X500Principal(
				"CN=Dumbledore, OU=Administration, O=Hogwarts School, C=GB");
		String canonical = principal.getName(X500Principal.CANONICAL);
		String expected = "cn=dumbledore,ou=administration,o=hogwarts school,c=gb";
		assertEquals("CANONICAL output differs from expected result", expected,
				canonical);
	}
}