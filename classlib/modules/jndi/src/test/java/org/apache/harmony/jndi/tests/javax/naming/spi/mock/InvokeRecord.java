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

import java.util.ArrayList;

import org.apache.harmony.jndi.tests.javax.naming.util.Log;

public class InvokeRecord {

	private static final Log log = new Log(null);

	private static final ArrayList<Object> params = new ArrayList<Object>();

	private static String urlSchema = null;

	public static String getLatestUrlSchema() {
		return urlSchema;
	}

	public static void set(String s, Object p1) {
		urlSchema = s;
		params.clear();
		params.add(p1);
	}

	public static void set(String s, Object p1, Object p2) {
		urlSchema = s;
		params.clear();
		params.add(p1);
		params.add(p2);
	}

	public static void set(String s, Object p1, Object p2, Object p3) {
		urlSchema = s;
		params.clear();
		params.add(p1);
		params.add(p2);
		params.add(p3);
	}

	public static void set(String s, Object p1, Object p2, Object p3, Object p4) {
		urlSchema = s;
		params.clear();
		params.add(p1);
		params.add(p2);
		params.add(p3);
		params.add(p4);
	}

	public static void set(String s, Object p1, Object p2, Object p3,
			Object p4, Object p5) {
		urlSchema = s;
		params.clear();
		params.add(p1);
		params.add(p2);
		params.add(p3);
		params.add(p4);
		params.add(p5);
	}

	public static boolean equals(String s, Object p1) {
		ArrayList<Object> tmp = new ArrayList<Object>();
		tmp.add(p1);
		return equals(s, tmp);
	}

	public static boolean equals(String s, Object p1, Object p2) {
		ArrayList<Object> tmp = new ArrayList<Object>();
		tmp.add(p1);
		tmp.add(p2);
		return equals(s, tmp);
	}

	public static boolean equals(String s, Object p1, Object p2, Object p3) {
		ArrayList<Object> tmp = new ArrayList<Object>();
		tmp.add(p1);
		tmp.add(p2);
		tmp.add(p3);
		return equals(s, tmp);
	}

	public static boolean equals(String s, Object p1, Object p2, Object p3,
			Object p4) {
		ArrayList<Object> tmp = new ArrayList<Object>();
		tmp.add(p1);
		tmp.add(p2);
		tmp.add(p3);
		tmp.add(p4);
		return equals(s, tmp);
	}

	public static boolean equals(String s, Object p1, Object p2, Object p3,
			Object p4, Object p5) {
		ArrayList<Object> tmp = new ArrayList<Object>();
		tmp.add(p1);
		tmp.add(p2);
		tmp.add(p3);
		tmp.add(p4);
		tmp.add(p5);
		return equals(s, tmp);
	}

	private static boolean equals(String s, ArrayList<Object> tmp) {
		boolean r = (urlSchema == null ? s == null : urlSchema.equals(s))
				&& tmp.equals(params);
		if (!r) {
			log.log("expected: " + s + ", " + tmp);
			log.log("but it's: " + urlSchema + ", " + params);
		}
		return r;
	}

}
