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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.NamingException;

import junit.framework.TestCase;
import org.apache.harmony.jndi.tests.javax.naming.util.Log;

public class InitialContextLibTest extends TestCase {
	private static final Log log = new Log(InitialContextLibTest.class);

	private static final String jndiProp = "jndi.properties";

	public void testConstructor_Lib() throws NamingException, IOException {
		//Comment this test case out because this test case 
		//needs complex configuration about jndi properties.

//		log.setMethod("testConstructor_Lib");
//		InitialContext context = new InitialContext();
//		Hashtable props = context.getEnvironment();
//		Hashtable expected = readAllProps(null);
//		assertEquals(expected, props);
	}

	void printHashtable(Hashtable<?, ?> env) {
		// TO DO: Need to remove
		Enumeration<?> keys = env.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			log.log(key + "=" + env.get(key));
		}
	}

	static Properties readAllProps(Hashtable<?, ?> env) throws IOException {
		// env param
		Properties props = new Properties();
		if (env != null) {
			props = mergProps(props, env);
		}

		// sys prop
		props = mergSysProps(props, System.getProperties());

		// application resource
		// ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		Enumeration<?> resources = classLoader.getResources(jndiProp);
		while (resources.hasMoreElements()) {
			URL url = (URL) resources.nextElement();
			InputStream fis = url.openStream();
			Properties resource = new Properties();
			resource.load(fis);
			fis.close();
			props = mergProps(props, resource);
		}

		// lib resource
		/*
		 * try { File file = new File(System.getProperty("java.home"), "lib");
		 * file = new File(file, jndiProp); FileInputStream fis = new
		 * FileInputStream(file); Properties resource = new Properties();
		 * resource.load(fis); fis.close(); props = mergProps(props, resource); }
		 * catch (FileNotFoundException e) { System.out.println(e.toString()); }
		 */
		return props;
	}

	static Properties mergProps(Properties props, Hashtable<?, ?> env) {
		Properties resource = new Properties();
		resource.putAll(props);

		Hashtable<String, String> items = getItemsType();
		Enumeration<?> keys = env.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			String type = items.get(key);
			Object oldObj = resource.get(key);
			Object newObj = env.get(key);
			if (type == null) {
				resource.put(key, newObj);
				continue;
			}

			if (type.equals("F")) {
				if ((oldObj == null) && (newObj != null)) {
					resource.put(key, env.get(key));
				}
			} else if (type.equals("C")) {
				if ((oldObj != null) && (newObj != null)) {
					resource.put(key, (String) oldObj + ":" + (String) newObj);
				} else if ((oldObj == null) && (newObj != null)) {
					resource.put(key, newObj);
				}
			}
		}

		return resource;
	}

	static Properties mergSysProps(Properties props, Hashtable<?, ?> env) {
		Properties resource = new Properties();
		resource.putAll(props);

		Hashtable<String, String> items = getSystemItemsType();
		Enumeration<String> keys = items.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			String type = items.get(key);
			Object oldObj = resource.get(key);
			Object newObj = env.get(key);

			if (type.equals("F")) {
				if ((oldObj == null) && (newObj != null)) {
					resource.put(key, env.get(key));
				}
			} else if (type.equals("C")) {
				if ((oldObj == null) && (newObj != null)) {
					resource.put(key, newObj);
				}
			}
		}
		return resource;
	}

	static Hashtable<String, String> getItemsType() {
		Hashtable<String, String> hashtable = new Hashtable<String, String>();
		hashtable.put("java.naming.factory.initial", "F");
		hashtable.put("java.naming.provider.url", "F");
		hashtable.put("java.naming.factory.control", "C");
		hashtable.put("java.naming.applet", "F");
		hashtable.put("java.naming.authoritative", "F");
		hashtable.put("java.naming.batchsize", "F");
		hashtable.put("java.naming.dns.url", "F");
		hashtable.put("java.naming.factory.object", "C");
		hashtable.put("java.naming.factory.state", "C");
		hashtable.put("java.naming.factory.url.pkgs", "C");
		hashtable.put("java.naming.language", "F");
		hashtable.put("java.naming.referral", "F");
		hashtable.put("java.naming.security.authentication", "F");
		hashtable.put("java.naming.security.credentials", "F");
		hashtable.put("java.naming.security.principal", "F");
		hashtable.put("java.naming.security.protocol", "F");
		return hashtable;
	}

	static Hashtable<String, String> getSystemItemsType() {
		Hashtable<String, String> hashtable = new Hashtable<String, String>();
		hashtable.put("java.naming.factory.initial", "F");
		hashtable.put("java.naming.provider.url", "F");
		hashtable.put("java.naming.factory.control", "C");
		// hashtable.put("java.naming.applet", "F");
		// hashtable.put("java.naming.authoritative", "F");
		// hashtable.put("java.naming.batchsize", "F");
		hashtable.put("java.naming.dns.url", "F");
		hashtable.put("java.naming.factory.object", "C");
		hashtable.put("java.naming.factory.state", "C");
		hashtable.put("java.naming.factory.url.pkgs", "C");
		// hashtable.put("java.naming.language", "F");
		// hashtable.put("java.naming.referral", "F");
		// hashtable.put("java.naming.security.authentication", "F");
		// hashtable.put("java.naming.security.credentials", "F");
		// hashtable.put("java.naming.security.principal", "F");
		// hashtable.put("java.naming.security.protocol", "F");
		return hashtable;
	}

}
