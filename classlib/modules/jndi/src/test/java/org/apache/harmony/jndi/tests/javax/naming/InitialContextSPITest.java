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

package org.apache.harmony.jndi.tests.javax.naming;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.NamingException;

import junit.framework.TestCase;
import org.apache.harmony.jndi.tests.javax.naming.util.Log;

public class InitialContextSPITest extends TestCase {
	private final Log log = new Log(InitialContextSPITest.class);

	public void testConstructor_SPI() throws NamingException {
		// log.setMethod("testConstructor_SPI");
		// Hashtable env = new Hashtable();
		// env.put(Context.INITIAL_CONTEXT_FACTORY,
		// "dazzle.jndi.testing.spi.DazzleContextFactory");
		//        
		// InitialContext context = new InitialContext(env);
		// Hashtable props = context.getEnvironment();
		//        
		// Hashtable expected = new Hashtable();
		// expected.put("java.naming.factory.initial",
		// "dazzle.jndi.testing.spi.DazzleContextFactory");
		// assertEquals(expected, props);
		// //printHashtable(props);
	}

	void printHashtable(Hashtable<?, ?> env) {
		// TO DO: Need to remove
		Enumeration<?> keys = env.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			log.log(key + "=" + env.get(key));
		}
	}
}
