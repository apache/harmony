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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Filter out exception trace stack and any other sensitive or unnecessary
 * content from the log file.
 */
public class FileFilter {

	private static final int FILTER_EXCEPTION = 1;

	private static final int FILTER_SUCCEED = 2;

	// Can't initilaize
	private FileFilter() {
	}

	private static boolean junitStartsWith(String line, String pattern) {
		if (null == line) {
			return false;
		} else if (line.trim().startsWith("[junit]")) {
			if (line.trim().substring(7).trim().startsWith(pattern)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Filter out exception trace stack and any other sensitive or unnecessary
	 * content from the log file.
	 * 
	 * @param logFile
	 * @param flag
	 */
	private static void filter(String logFile, int flag) {
		FileInputStream fis = null;
		BufferedReader reader = null;
		FileOutputStream fos = null;
		PrintStream ps = null;

		try {
			String pre = null;
			String nxt = null;

			File file = new File(logFile);

			fis = new FileInputStream(logFile);
			reader = new BufferedReader(new InputStreamReader(fis));
			fos = new FileOutputStream(file.getParent()
					+ System.getProperty("file.separator") + "Clean_"
					+ file.getName());
			ps = new PrintStream(fos);

			while (true) {
				nxt = reader.readLine();
				if (null != pre) {
					// filter out exception stack trace
					if (0 != (flag & FILTER_EXCEPTION)) {
						if (junitStartsWith(pre, "at ")) {
							if (nxt == null) {
								break;
							}
                            pre = nxt;
							continue;
						}
					}

					// filter out succeeded testcase
					if (0 != (flag & FILTER_SUCCEED)) {
						if (junitStartsWith(pre, "Testcase: ")) {
							if (junitStartsWith(nxt, "Testcase: ")
									|| junitStartsWith(nxt, "Testsuite: ")
									|| junitStartsWith(nxt, "TEST ")
									|| null == nxt) {
								if (nxt == null) {
									break;
								}
                                pre = nxt;
								continue;
							}
						}
					}

					ps.println(pre);
				}

				if (null == nxt) {
					break;
				}
                pre = nxt;
			}
		} catch (Exception e) {
			System.out.println("Filter exception: " + e.getMessage());
		} finally {
			try {
				reader.close();
			} catch (Exception e) {
			}
			try {
				fis.close();
			} catch (Exception e) {
			}
			try {
				ps.close();
			} catch (Exception e) {
			}
			try {
				fos.close();
			} catch (Exception e) {
			}
		}
	}

	private static void deleteOldLog(String logFile) {
		File file = new File(logFile);
		file.delete();
	}

	public static void main(String[] args) {
		try {
			filter(args[0], Integer.parseInt(args[1]));
			deleteOldLog(args[0]);
		} catch (Exception e) {
			System.out.println("Filter init exception: " + e.getMessage());
		}
	}
}
