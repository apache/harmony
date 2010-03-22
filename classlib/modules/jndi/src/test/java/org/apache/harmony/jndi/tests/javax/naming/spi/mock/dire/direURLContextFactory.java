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
package org.apache.harmony.jndi.tests.javax.naming.spi.mock.dire;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.directory.Attributes;
import javax.naming.spi.DirObjectFactory;

import org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirContext3;
import org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest;
import org.apache.harmony.jndi.tests.javax.naming.util.Log;

public class direURLContextFactory implements DirObjectFactory {

	Log log = new Log(direURLContextFactory.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object,
	 *      javax.naming.Name, javax.naming.Context, java.util.Hashtable)
	 */
	public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> h)
			throws Exception {
		// NamingManagerTest.issueIndicatedExceptions(h);
		// if (NamingManagerTest.returnNullIndicated(h)) {
		// return null;
		// }
		log
				.setMethod("getObjectInstance(Object o, Name n, Context c, Hashtable h)");
		log.log("wrong method called!");
		Hashtable<String, Object> r = new Hashtable<String, Object>();
		if (null != o) {
			r.put("o", o);
		}
		if (null != n) {
			r.put("n", n);
		}
		if (null != c) {
			r.put("c", c);
		}
		if (null != h) {
			r.put("h", h);
		}
		return new MockDirContext3(r);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.spi.DirObjectFactory#getObjectInstance(java.lang.Object,
	 *      javax.naming.Name, javax.naming.Context, java.util.Hashtable,
	 *      javax.naming.directory.Attributes)
	 */
	public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> h,
			Attributes a) throws Exception {
		NamingManagerTest.issueIndicatedExceptions(h);
		if (NamingManagerTest.returnNullIndicated(h)) {
			return null;
		}
		Hashtable<String, Object> r = new Hashtable<String, Object>();
		if (null != o) {
			r.put("o", o);
		}
		if (null != n) {
			r.put("n", n);
		}
		if (null != c) {
			r.put("c", c);
		}
		if (null != h) {
			r.put("h", h);
		}
		if (null != a) {
			r.put("a", a);
		}
		return new MockDirContext3(r);
	}

}
