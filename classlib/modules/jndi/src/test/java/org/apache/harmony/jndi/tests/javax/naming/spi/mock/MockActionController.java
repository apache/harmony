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

package org.apache.harmony.jndi.tests.javax.naming.spi.mock;

import java.util.Hashtable;

import javax.naming.NamingException;

public class MockActionController implements DazzleActionController {
	private Hashtable<String, String> env;

	public MockActionController() {
		this.env = new Hashtable<String, String>();
	}

	public MockActionController(Hashtable<String, String> env) {
		this.env = env;
	}

	public void addAction(String action, String value) {
		this.env.put(action, value);
	}

	@SuppressWarnings("unchecked")
    public Object doActions() throws NamingException {
		Hashtable<String, String> actions = (Hashtable<String, String>) this.env.clone();
		this.env.clear();

		if (actions == null) {
			return RETURN_NORMAL;
		}

		if (actions.get(THROW_RUNTIMEEXCEPTION) != null) {
			throw new RuntimeException("Mock runtime exception!");
		}

		if (actions.get(THROW_NAMINGEXCEPTION) != null) {
			throw new NamingException("Mock NamingException");
		}

		if (actions.get(THROW_NULLPOINTEREXCEPTION) != null) {
			throw new NullPointerException("Mock NullPointerException");
		}

		if (actions.get(RETURN_NULL) != null) {
			return null;
		}

		return RETURN_NORMAL;
	}

}
