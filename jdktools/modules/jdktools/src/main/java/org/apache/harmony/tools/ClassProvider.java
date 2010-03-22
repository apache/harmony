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

package org.apache.harmony.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.util.ClassPath;

/**
 * This class locates a class in the given class path with the given name. You
 * can use this class to get a JavaClass object which represents the found 
 * class.
 *
 * This class depends on Apache Byte Code Engineering Library (BCEL) 5.0 or
 * later. Please see http://jakarta.apache.org/bcel for more information
 * about this library.
 */
public class ClassProvider {

    /**
     * Loading class files from a class path.
     */
    private ClassPath classpath;

    /**
     * Verbose output or not.
     */
    private boolean verbose;

    /**
     * Keeps loaded and parsed class files.
     */
    private Map cache;

    /**
     * Constructs a <code>ClassProvider</code> object.
     * 
     * @param bootClasspath - a path that will be prepended to the default 
     * class path.
     * @param classpath - a path that will be apppended to the default class 
     * path.
     * @param verbose - a verbose output.
     */
    public ClassProvider(String bootClasspath, String classpath, boolean verbose) {
        StringBuffer pathString = new StringBuffer();

        // Append the given boot class path, if any.
        if (bootClasspath != null) {
            pathString.append(bootClasspath).append(File.pathSeparatorChar);
        }

        // Append the default class path.
        pathString.append(getSystemClassPath()).append(File.pathSeparatorChar);

        // Append the given class path, if any.
        if (classpath != null) {
            pathString.append(classpath).append(File.pathSeparatorChar);
        }

        if (verbose) {
            System.out.println("class.path: " + pathString.toString());
        }

        this.classpath = new ClassPath(pathString.toString());
        this.verbose = verbose;
        this.cache = new HashMap();
    }

    /**
     * Returns the system class path.
     *
     * @return the system class path.
     */
    private String getSystemClassPath() {
        String sep = System.getProperty("path.separator");
        StringBuffer cp = new StringBuffer();
        for (Enumeration e = System.getProperties().propertyNames(); e.hasMoreElements();) {
            // Enumerate all the system properties.
            String prop = (String) e.nextElement();
            if (prop.endsWith("class.path")) {
                // Add the value of a property to the class path, if its
                // name ends with "class.path".
                cp.append(System.getProperty(prop)).append(sep);
            }
        }
        return cp.toString();
    }

    /**
     * Returns a JavaClass object which represents a class file with the given
     * name.
     * 
     * @param name - a fully qualified name of a class.
     * @return a JavaClass object which represents a class file with the given
     * name.
     * @throws ClassNotFoundException if a class file with the given name is
     * not found.
     */
    public synchronized JavaClass getJavaClass(String name)
            throws ClassNotFoundException {
        try {
            // Try to get the class from the cache.
            JavaClass result = (JavaClass) cache.get(name);
            // If cache doesn't contain such a class load it from a class path.
            if (result == null) {
                // Get a file and parse its contents.
                ClassPath.ClassFile cf = classpath.getClassFile(name);
                InputStream is = cf.getInputStream();
                ClassParser parser = new ClassParser(is, cf.getPath());
                result = parser.parse();
                // Put the parsed class file into the cache.
                cache.put(name, result);

                if (verbose) {
                    StringBuffer s = new StringBuffer();
                    // If we use BCEL 5.1 or later one day we definitely
                    // should remove the following if and replace 
                    // cf.getPath() with cf.getBase()!
                    if (!(is instanceof FileInputStream)) {
                        s.append("class.path:");
                    }
                    s.append(cf.getPath());
                    System.out.println(name + " loaded from " + s);
                }
            } else {
                if (verbose) {
                    System.out.println(name + " retrieved from a cache");
                }
            }
            return result;
        } catch (Exception e) {
            throw new ClassNotFoundException(name, e);
        }
    }
}
