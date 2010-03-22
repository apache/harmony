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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Log {

	private static PrintStream _out = null;

	private static String latestClzname = null;

	private static String latestMethod = null;

	public synchronized static PrintStream getPrintStream() {
		if (null != _out) {
			return _out;
		}

		try {
//			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			// _out =
			// new PrintStream(
			// new FileOutputStream(
			// "/bluetrace_" + sdf.format(new Date()) + ".log",
			// true));
			_out = new PrintStream(new ByteArrayOutputStream());
		} catch (Exception e) {
			e.printStackTrace();
			_out = System.err;
		}
		return _out;
	}

	public synchronized static void close() {
		if (null != _out && System.err != _out) {
			_out.println("Close log.");
			_out.close();
		}
		_out = null;
	}

	private String clzname;

	private String method;

	/**
	 * Constructor.
	 * 
	 * @param clz
	 */
	public Log(Class<?> clz) {
		if (null != clz) {
			clzname = clz.getName();
			int dot = clzname.lastIndexOf('.');
			clzname = clzname.substring(dot + 1);
		}

		// getPrintStream().println();
		// getPrintStream().println(clzname + " inited.");
		// getPrintStream().println();
	}

	/**
	 * Set method.
	 * 
	 * @param method
	 */
	public void setMethod(String method) {
		this.method = method;

		latestClzname = clzname;
		latestMethod = method;
	}

	public void log(boolean b) {
		log("" + b);
	}

	public void log(int i) {
		log("" + i);
	}

	public void log(String msg) {
		_log(clzname, method, msg);
	}

	public void log(Throwable t) {
		if (t == null) {
			log("Throwable instance is null.");
		} else {
			log(t.getMessage());
			log("Throwable occured, type is " + t.getClass().getName());
		}
		//
		// try {
		// StringWriter strbuf = new StringWriter();
		// PrintWriter tempout = new PrintWriter(strbuf);
		// tempout.println(t.getMessage());
		// t.printStackTrace(tempout);
		// tempout.close();
		// BufferedReader tempin =
		// new BufferedReader(
		// new StringReader(strbuf.getBuffer().toString()));
		// String line;
		// while ((line = tempin.readLine()) != null) {
		// line = line.trim();
		// if (line.startsWith("at javax.naming.Test")) {
		// String method =
		// line.substring(
		// "at javax.naming.".length(),
		// line.indexOf("("));
		// String lineNumber =
		// line.substring(
		// line.indexOf(":") + 1,
		// line.indexOf(")"));
		// try {
		// Integer.parseInt(lineNumber);
		// } catch (NumberFormatException e1) {
		// lineNumber = "";
		// }
		// log(method + "(" + lineNumber + ")");
		// }
		// }
		// } catch (IOException e) {
		// }
	}

	/**
	 * Log a message and an exception.
	 * 
	 * @param method
	 * @param msg
	 * @param t
	 */
	public void log(String msg, Throwable t) {
		log(msg);
		log(t);
	}

	private static void _log(String clzname, String method, String msg) {
		if (clzname != null && method != null) {
			latestClzname = clzname;
			latestMethod = method;
		} else {
			clzname = latestClzname;
			method = latestMethod;
		}

		PrintStream out = getPrintStream();

		out.print(clzname);
		out.print("\t");
		out.print(method);
		out.print("\t");
		out.print(msg);
		out.println();
	}

	public static void main(String args[]) throws Exception {

		// NamingException ex = new NamingException("test purpose");
		// ex.setRemainingName(new CompositeName("RemainingName"));
		// ex.setResolvedName(new CompositeName("RemainingName"));
		// ex.setResolvedObj(new Integer(1));
		// ex.setRootCause(new Exception("root exception"));
		// ObjectOutputStream out =
		// (ObjectOutputStream) new ObjectOutputStream(
		// new FileOutputStream("test/data/NamingException.ser"));
		// out.writeObject(ex);
		// out.close();
	}

}
