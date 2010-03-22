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
package org.apache.harmony.jndi.tests.javax.naming.util;

import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;

public final class Util {

	/**
	 * Only entry to obtain an InitialContext instance.
	 * 
	 * @return a new InitialContext instance
	 */
	public static InitialContext getInitialContext() {
		try {
			Properties p = new Properties();
			p.load(Util.class.getClassLoader().getResourceAsStream(
					"jndi.properties"));
			/*
			 * Hashtable ht = new Hashtable(); ht.put(
			 * Context.INITIAL_CONTEXT_FACTORY,
			 * "dazzle.jndi.testing.spi.DazzleContextFactory");
			 */
			return new InitialContext(p);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Clean up, opposite to getInitialContext().
	 * 
	 * @param ctx
	 *            the InitialContext to clean up
	 */
	public static void cleanUpInitialContext(InitialContext ctx) {

	}

	/**
	 * Only entry to obtain an InitialDirContext instance.
	 * 
	 * @return a new InitialDirContext instance
	 */
	public static InitialDirContext getInitialDirContext() {
		try {
			Properties p = new Properties();
			p.load(Util.class.getClassLoader().getResourceAsStream(
					"jndi.properties"));
			Hashtable<String, String> ht = new Hashtable<String, String>();
			ht.put(Context.INITIAL_CONTEXT_FACTORY,
					"dazzle.jndi.testing.spi.DazzleContextFactory");
			return new InitialDirContext(p);
		} catch (Exception e) {
			// e.printStackTrace();
			return null;
		}
	}

	/**
	 * Clean up, opposite to getInitialDirContext().
	 * 
	 * @param ctx
	 *            the InitialDirContext to clean up
	 */
	public static void cleanUpInitialDirContext(InitialDirContext ctx) {

	}

	/**
	 * Format an Attribute to String
	 * 
	 * @param a
	 * @return the string representation
	 */
	public static String toString(Attribute a) {
		if (a == null) {
			return "NULL";
		}

		try {
			StringBuffer buf = new StringBuffer();
			buf.append(a.getID());
			if (a.isOrdered()) {
				buf.append("+o");
			}
			buf.append("=");
			if (a.size() == 0) {
				buf.append("null");
			} else if (a.size() == 1) {
				buf.append(a.get());
			} else {
				buf.append("[");
				for (int i = 0; i < a.size(); i++) {
					if (i != 0) {
						buf.append(",");
					}
					buf.append(a.get(i) == null ? "null" : a.get(i));
				}
				buf.append("]");
			}
			return buf.toString();
		} catch (Throwable e) {
			e.printStackTrace();
			return "NULL";
		}
	}

	/**
	 * Format an Attributes to String
	 * 
	 * @param as
	 * @return the string representation
	 */
	public static String toString(Attributes as) {
		if (as == null) {
			return "NULL";
		}

		try {
			if (as.size() == 0) {
				return "{}";
			}
            StringBuffer buf = new StringBuffer();
            buf.append("{ ");
            NamingEnumeration<? extends Attribute> enumeration = as.getAll();
            int i = 0;
            while (enumeration.hasMoreElements()) {
            	Attribute a = enumeration.nextElement();
            	if (i != 0) {
            		buf.append(", ");
            	}
            	buf.append(toString(a));
            	i++;
            }
            buf.append(" }");
            return buf.toString();
		} catch (Throwable e) {
			e.printStackTrace();
			return "NULL";
		}
	}

	/**
	 * Format a SearchResult to String
	 * 
	 * @param r
	 * @return the string representation
	 */
	public static String toString(SearchResult r) {
		StringBuffer buf = new StringBuffer();
		buf.append(r.getName());
		buf.append(" ");
		buf.append(toString(r.getAttributes()));
		buf.append(" = ");
		buf.append(r.getObject());
		return buf.toString();
	}

	/**
	 * Format a Context to String
	 * 
	 * @param ctx
	 * @return the string representation
	 */
	public static String toString(Context ctx) {
		if (ctx == null) {
			return "NULL";
		}

		try {
			StringBuffer buf = new StringBuffer();
			return toString(buf, 0, ctx);
		} catch (Throwable e) {
			e.printStackTrace();
			return "NULL";
		}
	}

	/**
	 * Format a DirContext to String
	 * 
	 * @param ctx
	 * @return the string representation
	 */
	public static String toString(DirContext ctx) {
		if (ctx == null) {
			return "NULL";
		}

		try {
			StringBuffer buf = new StringBuffer();
			return toString(buf, 0, ctx);
		} catch (Throwable e) {
			e.printStackTrace();
			return "NULL";
		}
	}

	private static String toString(StringBuffer buf, int i, Context ctx)
			throws NamingException {

		int j = i + 4;
		space(buf, i);
		buf.append(ctx + " {").append("\n");

		NamingEnumeration<Binding> enumeration = ctx.listBindings("");
		while (enumeration.hasMoreElements()) {
			Binding r = enumeration.nextElement();
			space(buf, j);
			if (r.getName() != null) {
				buf.append(r.getName());
				if (ctx instanceof DirContext) {
					buf.append(" ");
					buf.append(toString(((DirContext) ctx).getAttributes(r
							.getName())));
				}
			}
			buf.append(" = ");
			if (r.getObject() instanceof Context) {
				toString(buf, j, (Context) r.getObject());
			} else {
				buf.append(r.getObject() == null ? "" : r.getObject());
			}
			buf.append("\n");
		}

		space(buf, i);
		buf.append("}");
		return buf.toString();
	}

	private static void space(StringBuffer buf, int n) {
		for (int i = 0; i < n; i++) {
			buf.append(' ');
		}
	}

	public static void main(String[] args) throws Exception {
		Context ctx = Util.getInitialContext();
		ctx.bind("test", new Integer(200));
	}
}
