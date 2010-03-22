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

package org.apache.harmony.tools.javap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.harmony.tools.ClassProvider;

/**
 * This is a tool that allows you to disassemble class files.
 */
public class Main {

    /**
     * Prints the usage information.
     */
    public static void usage() {
        System.out.println("Class file disassembler.");
        System.out.println();
        System.out.println("Usage: " + Main.class.getName()
                + " [options] <class names>\n");
        System.out.println("[options]");
        System.out.println("  -bootclasspath <path> The given path will be prepended to");
        System.out.println("                        the default class path.\n");
        System.out.println("  -classpath <path>     The given path will be appended to");
        System.out.println("                        the default class path.\n");
        System.out.println("  -extdirs <dirs>       Override extension locations.\n");

        System.out.println("  -package              Print package visible, protected");
        System.out.println("                        and public classes and members. (default)\n");
        System.out.println("  -public               Print public classes and members.\n");
        System.out.println("  -protected            Print protected and public classes and members.\n");
        System.out.println("  -private              Print all classes and members.\n");
        System.out.println("  -c                    Print the code.\n");
        System.out.println("  -l                    Print line numbers and local variables.\n");
        System.out.println("  -s                    Print type signatures.\n");

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
            if (args[i].equals("-bootclasspath") 
                    || args[i].equals("-classpath")
                    || args[i].equals("-extdirs")) {
                String path = (String) args[i + 1];

                // Process the "-extdirs" option as "-bootclasspath".
                String opt = (String) args[i];
                if (opt.equals("-extdirs")) {
                    opt = "-bootclasspath";
                }

                // We can concatenate several options, if there is no 
                // security restriction; otherwise, the new option replaces
                // the old one.
                if (pathSep != null) {
                    String prevPath = (String) options.get(opt);
                    if (prevPath != null) {
                        path = prevPath + pathSep + path;
                    }
                }

                options.put(opt, path);
                i++;
            } else if (args[i].equals("-c") 
                    || args[i].equals("-package")
                    || args[i].equals("-public")
                    || args[i].equals("-protected")
                    || args[i].equals("-private")
                    || args[i].equals("-l")
                    || args[i].equals("-s")
                    || args[i].equals("-inner")
                    || args[i].equals("-verbose")) {
                options.put(args[i], Boolean.valueOf(true));
            } else if (args[i].startsWith("-")) {
                System.err.println("Unknown option: " + args[i]);
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
     * 
     * <li> <i>key</i> - "-bootclasspath"
     * <li> <i>value</i> - a <code>java.lang.String</code> which is a path 
     * where bootstrap classes are located.
     * 
     * <li> <i>key</i> - "-classpath"
     * <li> <i>value</i> - a <code>java.lang.String</code> which is a path 
     * where classes are located.
     * 
     * <li> <i>key</i> - "-c"
     * <li> <i>value</i> - a <code>java.lang.Boolean</code> which is equal to
     * <code>true</code>, if the code should be printed.
     * 
     * <li> <i>key</i> - "-package"
     * <li> <i>value</i> - a <code>java.lang.Boolean</code> which is equal to
     * <code>true</code>, if package visible, protected and public
     * classes and members should be printed.
     * 
     * <li> <i>key</i> - "-public"
     * <li> <i>value</i> - a <code>java.lang.Boolean</code> which is equal to
     * <code>true</code>, if public classes and members should be printed.
     * 
     * <li> <i>key</i> - "-protected"
     * <li> <i>value</i> - a <code>java.lang.Boolean</code> which is equal to
     * <code>true</code>, if protected and public classes and members 
     * should be printed.
     * 
     * <li> <i>key</i> - "-private"
     * <li> <i>value</i> - a <code>java.lang.Boolean</code> which is equal to
     * <code>true</code>, if all classes and members should be printed.
     * 
     * <li> <i>key</i> - "-l"
     * <li> <i>value</i> - a <code>java.lang.Boolean</code> which is equal to
     * <code>true</code>, if line numbers and local variables should be printed.
     * 
     * <li> <i>key</i> - "-s"
     * <li> <i>value</i> - a <code>java.lang.Boolean</code> which is equal to
     * <code>true</code>, if type signatures should be printed.
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
        String bootClasspath = getString(options, "-bootclasspath");
        String classpath = getString(options, "-classpath");

        boolean c = getBoolean(options, "-c");
        boolean l = getBoolean(options, "-l");
        boolean s = getBoolean(options, "-s");
        boolean pack = getBoolean(options, "-package");
        boolean pub = getBoolean(options, "-public");
        boolean prot = getBoolean(options, "-protected");
        boolean priv = getBoolean(options, "-private");

        // The default option is "-package".
        if (!pub && !prot && !priv) {
            pack = true;
        }

        // Show all classes and their members.
        if (priv) {
            pack = true;
        }
        // Show package private, protected and public classes and their members.
        if (pack) {
            prot = true;
        }
        // Show protected and public classes and their members.
        if (prot) {
            pub = true;
        }

        boolean inner = getBoolean(options, "-inner");
        boolean verbose = getBoolean(options, "-verbose");

        ClassProvider classProvider = new ClassProvider(bootClasspath,
                classpath, verbose);

        StringBuffer result = new StringBuffer();
        try {
            String n = System.getProperty("line.separator");

            Set innerNames = new HashSet();
            Iterator namesIter = classNames.iterator();
            while (namesIter.hasNext()) {

                // Parse the next class.
                Clazz clazz = new Clazz(classProvider,
                        (String) namesIter.next(), verbose);

                // Set the output filter before we call clazz.toString().
                clazz.includeCode(c);
                clazz.includeLineNumbers(l);
                clazz.includeLocalVariables(l);
                clazz.includeTypeSignatures(s);
                clazz.includePackagePrivate(pack);
                clazz.includePublic(pub);
                clazz.includeProtected(prot);
                clazz.includePrivate(priv);

                if (inner) {
                    // Get the inner class names and store them 
                    // in a separate set.
                    String innerClassNames[] = clazz.getInnerClassNames();
                    for (int i = 0; i < innerClassNames.length; i++) {
                        innerNames.add(innerClassNames[i]);
                    }
                }

                // Process the next class.
                result.append(clazz.toString());
                result.append(n);

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
                    inner = false;
                }

            }

        } catch (Exception e) {
            System.err.println("Error:");
            e.printStackTrace();
            return 1;
        }

        // Print the result.
        System.out.print(result.toString());

        return 0;
    }

    private static String getString(Map options, String name) {
        try {
            return (String) options.get(name);
        } catch (ClassCastException e) {
            throw new RuntimeException(
                    "'" + name + "': expected java.lang.String", e);
        }
    }
    
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
}
