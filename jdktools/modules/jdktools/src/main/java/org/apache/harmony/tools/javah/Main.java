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

package org.apache.harmony.tools.javah;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.harmony.tools.ClassProvider;
import org.apache.harmony.tools.Mangler;

/**
 * This is a tool that allows you to create JNI-style header files.
 */
public class Main {

    /**
     * Prints the usage information.
     */
    public static void usage() {
        System.out.println("JNI-style header files generator.");
        System.out.println();
        System.out.println("Usage: " + Main.class.getName()
                + " [options] <class names>\n");
        System.out.println("[options]");
        System.out.println("  -bootclasspath <path> The given path will be prepended to");
        System.out.println("                        the default class path.\n");
        System.out.println("  -classpath <path>     The given path will be appended to");
        System.out.println("                        the default class path.\n");
        System.out.println("  -o <file>             All the output will be redirected into");
        System.out.println("                        the specified file.\n");
        System.out.println("  -d <directory>        All the header files will be created in the specified");
        System.out.println("                        directory which will be created if it is necessary.\n");
        System.out.println("  -inner                Inner classes will be processed automatically.\n");
        System.out.println("  -verbose              Allows running commentary.\n");
        System.out.println("<class names>           A list of the fully qualified class names");
        System.out.println("                        separated by a space.");
    }

    /**
     * A convenient way to run this tool from a command line.
     */
    public static void main(String args[]) {
        if (args.length < 1) {
            usage();
            System.exit(1);
        }

        String pathSep = null;
        try {
            pathSep = System.getProperty("path.separator");
        } catch (SecurityException e) {
            // ignore
        }
        
        Set names = new HashSet();
        Map options = new HashMap();
        int i = 0;
        while (i < args.length) {
            if (args[i].equals("-bootclasspath") || args[i].equals("-classpath")) {
                String path = (String) args[i + 1];

                // We can concatenate several options, if there is no 
                // security restriction; otherwise, the new option replaces
                // the old one.
                if (pathSep != null) {
                    String prevPath = (String) options.get(args[i]);
                    if (prevPath != null) {
                        path = prevPath + pathSep + path;
                    }
                }

                options.put(args[i], path);
                i++;
            } else if (args[i].equals("-d") || args[i].equals("-o")) {
                options.put(args[i], new File(args[i + 1]));
                i++;
            } else if (args[i].equals("-inner") || args[i].equals("-verbose")) {
                options.put(args[i], Boolean.valueOf(true));
            } else {
                names.add(args[i]);
            }
            i++;
        }
        System.exit(run(options, names));
    }

    /**
     * Runs a tool.
     * 
     * @param options - a <code>java.util.Map</code> of the following key-value
     * pairs.
     * <p> Note, "-d" can not be specified with "-o" at the same time.
     * 
     * <li> <i>key</i> - "-d"
     * <li> <i>value</i> - a <code>java.io.File</code> object which represents 
     * a directory where the generated header files will be created. 
     * If this directory does not exist it will be created.
     * 
     * <li> <i>key</i> - "-o"
     * <li> <i>value</i> - a <code>java.io.File</code> object where 
     * the output will be directed to.
     * 
     * <li> <i>key</i> - "-bootclasspath"
     * <li> <i>value</i> - a <code>java.lang.String</code> which is a path 
     * where bootstrap classes are located.
     * 
     * <li> <i>key</i> - "-classpath"
     * <li> <i>value</i> - a <code>java.lang.String</code> which is a path 
     * where classes are located.
     * 
     * <li> <i>key</i> - "-inner"
     * <li> <i>value</i> - a <code>java.lang.Boolean</code> which is equal to
     * <code>true</code>, if inner classes should be processed automatically,
     * that is, as if they are specified as elements of 
     * the <code>classNames</code> parameter.
     * 
     * <li> <i>key</i> - "-verbose"
     * <li> <i>value</i> - a <code>java.lang.Boolean</code> which is equal to
     * <code>true</code>, if the verbose output is needed.
     * 
     * @param classNames - a set of the fully qualified class names.
     * @return <code>0</code> if there is no error; <code>1</code> otherwise.
     */
    public static int run(Map options, Set classNames) {
        File outDirectory = getFile(options, "-d");
        File outFile = getFile(options, "-o");

        if (outDirectory != null && outFile != null) {
            System.err.println("Error: You can not use both -d and -o options.");
            System.err.println();
            usage();
            return 1;
        }

        String bootClasspath = getString(options, "-bootclasspath");
        String classpath = getString(options, "-classpath");

        boolean inner = getBoolean(options, "-inner");
        boolean verbose = getBoolean(options, "-verbose");

        ClassProvider classProvider = new ClassProvider(bootClasspath,
                classpath, verbose);
        try {
            String n = System.getProperty("line.separator");
            String warning =
                  "/*" + n
                + " * THE FILE HAS BEEN AUTOGENERATED BY THE javah TOOL." + n
                + " * Please be aware that all changes made to this file manually" + n
                + " * will be overwritten by the tool if it runs again." + n
                + " */" + n
                + n
                + "#include <jni.h>" + n
                + n;

            StringBuffer result = new StringBuffer();
            Set innerNames = new HashSet();
            File file = outFile;
            Iterator namesIter = classNames.iterator();
            while (namesIter.hasNext()) {

                // Parse the next class.
                Clazz clazz = new Clazz(classProvider, (String) namesIter.next());

                if (inner) {
                    // Get the inner class names and store them 
                    // in a separate set.
                    String innerClassNames[] = clazz.getInnerClassNames();
                    for (int i = 0; i < innerClassNames.length; i++) {
                        innerNames.add(innerClassNames[i]);
                    }
                }

                if (outFile == null) {
                    // Each header should be written into the separate file.

                    String fileName = Mangler.mangleFileName(
                            clazz.getName()) + ".h";

                    if (outDirectory != null) {
                        if (!outDirectory.exists()) {
                            outDirectory.mkdir();
                        }
                        fileName = outDirectory.getPath() + File.separator
                                + fileName;
                    }

                    // Reset the output file.
                    file = new File(fileName);

                    // Reset the result buffer.
                    result = new StringBuffer();
                }

                // Append the next header.
                result.append(Main.getHeader(clazz));

                // Process the inner class names, if any.
                if (!namesIter.hasNext() && !innerNames.isEmpty()) {
                    // Remove the inner class names that have already
                    // been processed.
                    innerNames.removeAll(classNames);

                    // Reset the loop iterator.
                    classNames = new HashSet(innerNames);
                    namesIter = classNames.iterator();

                    // Clear the set of the inner class names.
                    innerNames.clear();
                }

                // Write the result into a file.
                if (outFile == null || !namesIter.hasNext()) {
                    FileWriter writer = new FileWriter(file);
                    try {
                        writer.write(warning);
                        writer.write(result.toString());
                        if (verbose) {
                            System.out.println(file.getName() + " created");
                        }
                    } finally {
                        writer.close();
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error:");
            e.printStackTrace();
            return 1;
        }

        return 0;
    }

    /**
     * Returns a value of an option with the given name. The return type
     * is <code>java.io.File</code>. 
     */
    private static File getFile(Map options, String name) {
        try {
            return (File) options.get(name);
        } catch (ClassCastException e) {
            throw new RuntimeException(
                    "'" + name + "': expected java.io.File", e);
        }
    }
    
    /**
     * Returns a value of an option with the given name. The return type
     * is <code>java.lang.String</code>. 
     */
    private static String getString(Map options, String name) {
        try {
            return (String) options.get(name);
        } catch (ClassCastException e) {
            throw new RuntimeException(
                    "'" + name + "': expected java.lang.String", e);
        }
    }
    
    /**
     * Returns a value of an option with the given name. The return type
     * is <code>java.lang.Boolean</code>. 
     */
    private static boolean getBoolean(Map options, String name) {
        try {
            Object value = options.get(name);
            if (value != null) {
                return ((Boolean) value).booleanValue();
            }
        } catch (ClassCastException e) {
            throw new RuntimeException(
                    "'" + name + "': expected java.lang.Boolean", e);
        }
        return false;
    }

    /**
     * Returns a string that represents a JNI-style header file.
     */
    public static String getHeader(Clazz clazz) {
        String n = System.getProperty("line.separator");
        StringBuffer result = new StringBuffer();

        result.append(n);
        result.append("/* Header for class " + clazz.getName() + " */" + n);
        result.append(n);

        String headerMacro = "_" + 
                Mangler.mangleMacro(clazz.getName()).toUpperCase() + "_H";
        result.append("#ifndef " + headerMacro + n);
        result.append("#define " + headerMacro + n);
        result.append(n);

        result.append("#ifdef __cplusplus" + n);
        result.append("extern \"C\" {" + n);
        result.append("#endif" + n);
        result.append(n);

        result.append(clazz);
        result.append(n);

        result.append("#ifdef __cplusplus" + n);
        result.append("}" + n);
        result.append("#endif" + n);
        result.append(n);

        result.append("#endif /* " + headerMacro + " */" + n);
        result.append(n);

        return result.toString();
    }
}
