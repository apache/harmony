/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.drlvm.tests.regression.h3225;

import java.io.File;
import java.io.IOException;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import junit.framework.TestCase;

/**
 * Loads classes from the list and measures load time.
 * 
 * @see http://issues.apache.org/jira/browse/HARMONY-2615
 */
public class PerfTest extends TestCase {
    public static void main(String args[]) {
        (new PerfTest()).test();
    }

    public void test() {
        try {
            PerfTest test = new PerfTest();
            test.readProperty(BOOT_CLASS_PATH_PROPERTY);
//            test.readProperty(CLASS_PATH_PROPERTY);
            test.load();
            System.out.println("SUCCESS");
        } catch (Throwable e) {
            System.out.println("Unexpected " + e);
            e.printStackTrace(System.out);
        }
    }

    private static final String JAR_SUFFIX = ".jar";

    private static final String CLASS_SUFFIX = ".class";

    private static final int CLASS_SUFFIX_LENGTH = CLASS_SUFFIX.length();

    private static final String BOOT_CLASS_PATH_PROPERTY = "sun.boot.class.path";

    private static final String CLASS_PATH_PROPERTY = "java.class.path";

    private static final boolean LOG_CLASSES = true;

    /**
	 * Class name storage.
	 */
    private Set<String> classNames = new TreeSet<String>();

    /**
	 * Reads all class names from the specified Jar.
	 */
    private int readJar(String fileName) throws IOException {
        JarFile jarFile = new JarFile(fileName);
        int num = 0;

        System.out.print("Reading " + fileName + ": ");
        for (Enumeration e = jarFile.entries(); e.hasMoreElements(); ) {
            JarEntry jarEntry = (JarEntry) e.nextElement();

            if (jarEntry.isDirectory()) {
                continue;
            }
            String entryName = jarEntry.getName();

            if (!entryName.endsWith(CLASS_SUFFIX)) {
                continue;
            }
            String className = entryName.substring(0, entryName.length()
                    - CLASS_SUFFIX_LENGTH).replace('/', '.');

            Loader loader = new Loader();
            Throwable result = loader.verifyClass(className);
            if (null == result) {
                if (LOG_CLASSES) {
                    System.out.println("Added " + className);
                }
                classNames.add(className);
                num++;
            } else {
                if (LOG_CLASSES) {
                    System.out.println("Skipped " + className + " due to "
                        + result);
                }
            }
        }
        System.out.println(num + " class files");
        return num;
    }

    /**
	 * Reads all class names from all jars listed in the specified property.
	 */
    private void readProperty(String propertyName) throws IOException {
        System.out.println("Reading from property: " + propertyName);

        String propertyValue = System.getProperty(propertyName);

        if (propertyValue == null) {
            throw new IOException("Property not found: " + propertyName);
        }

        StringTokenizer tokenizer = new StringTokenizer(
                propertyValue, File.pathSeparator);

        int num = 0;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            if (!token.endsWith(JAR_SUFFIX)) {
                System.out.println("Ignoring " + token);
                continue;
            }
            File file = new File(token);

            if (!file.isFile()) {
                System.out.println("Missing " + token);
                continue;
            }
            num += readJar(token);
        }
        System.out.println("Got " + num + " classes from "
            + propertyName);
    }


    /**
	 * Tries to load all known classes.
	 */
    private void load() throws IOException {

        System.out.println("Loading classes");
        long total = System.currentTimeMillis();

        for (Iterator<String> i = classNames.iterator(); i.hasNext(); ) {
            String className = i.next();
            Loader loader = new Loader();
            assertNull("Failed to verify " + className, loader.verifyClass(className));
        }
        System.out.println("Total time: " + (System.currentTimeMillis() - total));
    }

    final static int LENGTH = 1024;
    /**
     * Use a static buffer for speed.
     */
    static byte[] buffer = new byte[LENGTH];

    /**
	 * Tries to load a class.
	 */
    class Loader extends ClassLoader {

        public Throwable verifyClass(String name) {
        	try {
	            final String path = name.replace('.', '/') + ".class";
	            java.io.InputStream is = ClassLoader.getSystemResourceAsStream(path);
	            if (is == null) {
	                return new IOException("Cannot find " + path);
	            }
	            int offset = 0, bytes_read = 0;
                while ((bytes_read = is.read(buffer, offset, LENGTH - offset)) > 0)
                {
                    offset += bytes_read;
                }
	            if (bytes_read != -1) {
	                return new IOException("Class " + name
	                		+ " is too big, please increase LENGTH = " + LENGTH);
	            }

                defineClass(name, buffer, 0, offset).getConstructors();
                return null;
            } catch (Throwable e) {
                return e;
            }
        }
    }
}

