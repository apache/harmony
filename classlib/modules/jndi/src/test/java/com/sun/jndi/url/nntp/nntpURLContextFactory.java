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

package com.sun.jndi.url.nntp;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest;

public class nntpURLContextFactory implements ObjectFactory {

	public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> h)
			throws Exception {

		NamingManagerTest.issueIndicatedExceptions(h);
		if (NamingManagerTest.returnNullIndicated(h)) {
			return null;
		}

		return new MockObject(o, n, c, h);
	}

	public static class MockObject {
		private Object o;

		private Name n;

		private Context c;

		private Hashtable<?, ?> envmt;

		public MockObject(Object o, Name n, Context c, Hashtable<?, ?> envmt) {
			this.o = o;
			this.n = n;
			this.c = c;
			this.envmt = envmt;
		}

		@Override
        public String toString() {
			String s = "MockObject {";

			s += "Object= " + o + "\n";
			s += "Name= " + n + "\n";
			s += "Context= " + c + "\n";
			s += "Env= " + envmt;

			s += "}";

			return s;
		}

		@Override
        public boolean equals(Object obj) {
			if (obj instanceof MockObject) {
				MockObject theOther = (MockObject) obj;
				if (o != theOther.o) {
					return false;
				}

				boolean nameEqual = (null == n ? null == theOther.n : n
						.equals(theOther.n));
				if (!nameEqual) {
					return false;
				}

				if (c != theOther.c) {
					return false;
				}

				boolean envmtEqual = (null == envmt ? null == theOther.envmt
						: envmt.equals(theOther.envmt));
				if (!envmtEqual) {
					return false;
				}

				return true;
			}
            return false;
		}
	}
}
