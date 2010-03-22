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

package tests.support;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.BasicPermission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;

public class Support_PermissionCollection {

	public static void main(String[] args) throws Exception {
		System.setSecurityManager(new SecurityManager());
		java.net.URL[] urls = new java.net.URL[1];
		Class<?> c = null;
		java.net.URLClassLoader ucl = null;
		try {
			URL url = new URL(args[0]);
			urls[0] = url;
			ucl = URLClassLoader.newInstance(urls, null);
			c = Class.forName("coucou.FileAccess", true, ucl);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ProtectionDomain pd = c.getProtectionDomain();
		PermissionCollection coll = Policy.getPolicy().getPermissions(pd);
		Class<?> myPermission = Class.forName("mypackage.MyPermission");
		BasicPermission myperm = (BasicPermission) myPermission.newInstance();
		System.out.println(
		        coll.implies(new java.io.FilePermission("test1.txt", "write"))
				+ ","
				+ coll.implies(myperm)
                + ","
                + coll.implies(new java.io.FilePermission("test2.txt", "write"))
				+ ","
				+ coll.implies(new java.io.FilePermission("test3.txt", "read"))
				+ ",");
	}
}