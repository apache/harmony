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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author  Vasily Zakharov
 */
package org.apache.harmony.rmi;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.Vector;

import org.apache.harmony.rmi.common.SubProcess;


/**
 * Invoker for Virtual Machine.
 *
 * More methods running VM in various configurations are welcome.
 *
 * @author  Vasily Zakharov
 *
 * @todo    Probably in future should be replaced by VM-specific calls
 *          allowing to create VM without using heavy and system-dependent
 *          <code>exec()</code> calls.
 */
public final class JavaInvoker {

    /**
     * Home path.
     */
    private static final String javaHome =
            System.getProperty("java.home");

    /**
     * Class path.
     */
    private static final String javaClassPath =
            System.getProperty("java.class.path");

    /**
     * Security policy file.
     */
    private static final String policy =
            System.getProperty("java.security.policy");

    /**
     * Endorsed directories.
     */
    private static final String javaEndorsedDirs =
            System.getProperty("java.endorsed.dirs");

    /**
     * Boot class path, initialized in static block.
     */
    private static final String bootClassPath;

    /**
     * Executable location, initialized in static block.
     */
    private static final String executable;

    /**
     * Static block initializing {@link #bootClassPath} and {@link #executable}.
     */
    static {
        String vmName = System.getProperty("java.vm.name");

        bootClassPath = System.getProperty(vmName.equals("J9")
                ? "org.apache.harmony.boot.class.path" : "sun.boot.class.path");

        executable = new File(new File(javaHome, "bin"), "java").getPath();
    }

    /**
     * Creates args array for Java invocation.
     *
     * @param   options
     *          Java options.
     *
     * @param   className
     *          Name of the class to run.
     *
     * @param   params
     *          Parameters to pass to the class.
     *
     * @param   useEndorsedDirs
     *          If endorsed dirs (<code>-Djava.endorsed.dirs</code>)
     *          from this VM should be applied.
     *
     * @param   useBootClassPath
     *          If bootclasspath (<code>-Xbootclasspath</code>)
     *          from this VM should be applied.
     *
     * @return  Generated args array.
     */
    private static String[] createArgsArray(
            String[] options, String className, String[] params,
            boolean useEndorsedDirs, boolean useBootClassPath) {
        Vector args = new Vector();
        String useJavaClassPath;

        // Add name of Java executable to run.
        args.add(executable);

        if (useEndorsedDirs) {
            if (javaEndorsedDirs != null) {
                // Apply endorsed dirs.
                args.add("-Djava.endorsed.dirs=" + javaEndorsedDirs);
            }
        }

        if (bootClassPath != null) {
            if (useBootClassPath) {
                // Apply bootclasspath.
                args.add("-Xbootclasspath:" + bootClassPath);
                useJavaClassPath = javaClassPath;
            } else if (javaClassPath != null) { // Class path is set.
                // Append bootclasspath to classpath.
                useJavaClassPath =
                    (javaClassPath + File.pathSeparator + bootClassPath);
            } else { // Class path is not set.
                // Set class path to bootclasspath.
                useJavaClassPath = bootClassPath;
            }
        } else {
            useJavaClassPath = javaClassPath;
        }

        // Apply classpath.
        if (useJavaClassPath != null) {
            args.add("-classpath");
            args.add(useJavaClassPath);
        }

        if (policy != null) {
            // Apply security policy.
            args.add("-Djava.security.policy=" + policy);
        }

        if (options != null) {
            args.addAll(Arrays.asList(options));
        }

        if (className != null) {
            args.add(className);
        }

        if (params != null) {
            args.addAll(Arrays.asList(params));
        }

        return (String[]) args.toArray(new String[args.size()]);
    }

    /**
     * Invokes Java machine with configuration similar to the current one.
     * The properties of current VM are extracted from system properties
     * and passed to new VM.
     *
     * @param   options
     *          Java options.
     *
     * @param   className
     *          Name of the class to run.
     *
     * @param   params
     *          Parameters to pass to the class.
     *
     * @param   useEndorsedDirs
     *          If endorsed dirs (<code>-Djava.endorsed.dirs</code>)
     *          from this VM should be applied.
     *
     * @param   useBootClassPath
     *          If bootclasspath (<code>-Xbootclasspath</code>)
     *          from this VM should be applied.
     *
     * @return  The process created.
     *
     * @throws  IOException
     *          If some error occurs.
     */
    public static SubProcess invokeSimilar(String[] options, String className,
            String[] params, boolean useEndorsedDirs, boolean useBootClassPath)
            throws IOException {
        // @ToDo: Rewrite with ProcessBuilder for Java 5.0.
        return new SubProcess(createArgsArray(options, className, params,
                useEndorsedDirs, useBootClassPath));
    }
}
