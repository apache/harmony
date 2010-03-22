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
package org.apache.harmony.rmi.common;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Representation of a Java Compiler.
 *
 * @author  Vasily Zakharov
 */
public abstract class JavaCompiler implements RMIProperties {

    /**
     * Compiler-specific options, may be re-initialized by subclasses.
     */
    protected String[] compilerOptions = new String[0];

    /**
     * Compiles the files specified (together with options)
     * with a command line.
     *
     * @param   commandLine
     *          Command line. Can be <code>null</code>.
     *
     * @return  Java Compiler return value.
     *
     * @throws  JavaCompilerException
     *          If some error occurs.
     */
    public int compile(String commandLine) throws JavaCompilerException {
        return compile((commandLine != null)
                ? commandLine.trim().split("\\s") : new String[0]); //$NON-NLS-1$
    }

    /**
     * Compiles the files specified in parameters (together with options).
     *
     * @param   args
     *          Java Compiler options and source files to compile.
     *          Can be <code>null</code>.
     *
     * @return  Java Compiler return value.
     *
     * @throws  JavaCompilerException
     *          If some error occurs.
     */
    public int compile(String[] args) throws JavaCompilerException {
        return compile(args, (String[]) null);
    }

    /**
     * Compiles the specified files with specified options.
     *
     * @param   options
     *          Java Compiler options. Can be <code>null</code>.
     *
     * @param   files
     *          Source files to compile. Can be <code>null</code>.
     *
     * @return  Java Compiler return value.
     *
     * @throws  JavaCompilerException
     *          If some error occurs.
     */
    public int compile(String[] options, File[] files)
            throws JavaCompilerException {
        int length = ((files != null) ? files.length : 0);
        String[] fileNames = new String[length];

        for (int i = 0; i < length; i++) {
            fileNames[i] = files[i].getPath();
        }
        return compile(options, fileNames);
    }

    /**
     * Compiles the specified files with specified options.
     *
     * @param   options
     *          Java Compiler options. Can be <code>null</code>.
     *
     * @param   fileNames
     *          Source files to compile. Can be <code>null</code>.
     *
     * @return  Java Compiler return value.
     *
     * @throws  JavaCompilerException
     *          If some error occurs.
     */
    public int compile(String[] options, String[] fileNames)
            throws JavaCompilerException {
        int optionsLength = ((options != null) ? options.length : 0);
        int fileNamesLength = ((fileNames != null) ? fileNames.length : 0);
        int compilerOptionsLength =
                ((compilerOptions != null) ? compilerOptions.length : 0);
        String[] args;

        if ((fileNamesLength == 0) && (compilerOptionsLength == 0)) {
            args = ((options != null) ? options : new String[0]);
        } else {
            args = new String[
                    compilerOptionsLength + optionsLength + fileNamesLength];

            if (compilerOptionsLength != 0) {
                System.arraycopy(compilerOptions, 0, args,
                        0, compilerOptionsLength);
            }

            if (optionsLength != 0) {
                System.arraycopy(options, 0, args,
                        compilerOptionsLength, optionsLength);
            }

            if (fileNamesLength != 0) {
                System.arraycopy(fileNames, 0, args,
                        compilerOptionsLength + optionsLength, fileNamesLength);
            }
        }
        return run(args);
    }

    /**
     * Runs the compilation process with the specified arguments.
     *
     * This method must be overridden by the subclasses.
     *
     * @param   args
     *          Full non-<code>null</code> arguments list. Can be empty.
     *
     * @return  Java Compiler return value.
     *
     * @throws  JavaCompilerException
     *          If some error occurs.
     *
     * @throws  NullPointerException
     *          If args is <code>null</code>.
     */
    protected abstract int run(String[] args) throws JavaCompilerException;

    /**
     * Locates the file from the given environment
     * variable using the specified parameters.
     *
     * @param   variableName
     *          Name of the environment variable to use.
     *
     * @param   path
     *          Path from the directory designated by the specified environment
     *          variable to the directory, containing the file needed.
     *
     * @param   pattern
     *          Pattern (regular expression)
     *          describing the file that has to be returned.
     *          The first file matching this pattern would be returned.
     *
     * @param   name
     *          Descriptive name of the file being searched for,
     *          to be mentioned in exception message if error occurs.
     *
     * @return  The first file matching the
     *          <code>$variableName/path/pattern</code>.
     *
     * @throws  JavaCompilerException
     *          If no matching file is found.
     */
    protected static File getFileFromVariable(String variableName, String path,
            final String pattern, String name) throws JavaCompilerException {
        String parent;

        try {
            parent = System.getenv(variableName);
        } catch (Error e) {
            // TODO
            // Workaround until System.getenv() is implemented.
            parent = System.getProperty(variableName);
        }

        if (parent == null) {
            // rmi.4C={0} variable not found
            throw new JavaCompilerException(Messages.getString("rmi.4C", variableName)); //$NON-NLS-1$
        }

        File[] files = new File(parent, path).listFiles(
                new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        // Ignoring dir.
                        return (name.matches(pattern));
                    }
                });

        if ((files == null) || (files.length < 1)) {
            // rmi.4D={0} not found
            throw new JavaCompilerException(Messages.getString("rmi.4D", name)); //$NON-NLS-1$
        }
        return files[0];
    }

    /**
     * Locates Java Compiler using the following algorithm:
     *
     * 1. Check if {@link #JAVA_COMPILER_CLASS_PROPERTY} system property is set.
     *    If set, configure the returned compiler to use the specified class.
     *    Also check {@link #JAVA_COMPILER_METHOD_PROPERTY} system property,
     *    if set, configure the returned compiler to use the specified method,
     *    otherwise, the
     *    {@linkplain MethodJavaCompiler#DEFAULT_COMPILER_METHOD default method}
     *    is used.
     *
     * 2. Check if {@link #JAVA_COMPILER_EXECUTABLE_PROPERTY} system property
     *    is set. If found, configure the returned compiler to use the specified
     *    executable.
     *
     * 3. Check if the Eclipse Compiler class
     *    ({@link EclipseJavaCompiler#ECLIPSE_COMPILER_CLASS_NAME})
     *    is accessible via classpath, if so, use it to compile.
     *
     * 4. Check if
     *    {@link EclipseJavaCompiler#ECLIPSE_HOME_VARIABLE ECLIPSE_HOME}
     *    environment variable is set. If set, locate Eclipse compiler JAR
     *    ({@link EclipseJavaCompiler#ECLIPSE_HOME_VARIABLE
     *    $ECLIPSE_HOME}<code>/</code>{@link
     *    EclipseJavaCompiler#ECLIPSE_JAR_PATH plugins}<code>/</code>{@link
     *    EclipseJavaCompiler#ECLIPSE_JAR_PATTERN org.eclipse.jdt.core_*.jar}),
     *    load ({@link EclipseJavaCompiler#ECLIPSE_COMPILER_CLASS_NAME
     *    Eclipse Compiler class}) from it and use it to compile.
     *
     * 5. Check if {@link ExecJavaCompiler#JAVA_HOME_VARIABLE JAVA_HOME}
     *    environment variable is set. If set, search if
     *    {@link ExecJavaCompiler#JAVA_HOME_VARIABLE
     *    $JAVA_HOME}<code>/</code>{@link ExecJavaCompiler#JAVA_COMPILER_PATH
     *    bin}<code>/</code>{@link ExecJavaCompiler#JAVA_COMPILER_PATTERN
     *    javac} executable, if found, execute it to compile.
     *
     * 6. If nothing found, just execute
     *    {@link ExecJavaCompiler#DEFAULT_COMPILER_PROGRAM javac}
     *    hoping it would be found in the system path.
     *
     * @param   verbose
     *          If notes and warnings should be printed to {@link System#err}.
     *
     * @return  Configured {@link JavaCompiler} implementation.
     *
     * @throws  JavaCompilerException
     *          If compiler could not be found of configured.
     */
    public static final JavaCompiler locateJavaCompiler(boolean verbose)
            throws JavaCompilerException {

        // Checking JAVA_COMPILER_CLASS_PROPERTY.
        String compilerClassName =
                System.getProperty(JAVA_COMPILER_CLASS_PROPERTY);

        if (compilerClassName != null) {
            String compilerMethodName =
                    System.getProperty(JAVA_COMPILER_METHOD_PROPERTY);

            if (verbose) {
                // rmi.console.08=NOTE: Using compiler class: 
                System.err.println(Messages.getString("rmi.console.08") + //$NON-NLS-1$
                        compilerClassName + ((compilerMethodName != null)
                                ? (", method: " + compilerMethodName) : "")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return new MethodJavaCompiler(
                    compilerClassName, compilerMethodName);
        }

        // Checking JAVA_COMPILER_EXECUTABLE_PROPERTY.
        String executableName =
            System.getProperty(JAVA_COMPILER_EXECUTABLE_PROPERTY);

        if (executableName != null) {
            if (verbose) {
                // rmi.console.09=NOTE: Using compiler executable: {0}
                System.err.println(Messages.getString("rmi.console.09", //$NON-NLS-1$
                        executableName));
            }
            return new ExecJavaCompiler(executableName);
        }

        // Trying to run Eclipse Compiler.
        try {
            JavaCompiler compiler = new EclipseJavaCompiler();

            if (verbose) {
                // rmi.console.0A=NOTE: Using Eclipse Compiler
                System.err.println(Messages.getString("rmi.console.0A")); //$NON-NLS-1$
            }
            return compiler;
        } catch (JavaCompilerException e) {
            if (verbose) {
                // rmi.console.0B=NOTE: Eclipse Compiler class not found: {0}
                System.err.println(Messages.getString("rmi.console.0B", e)); //$NON-NLS-1$
            }
        }

        // Trying to run Javac executable from JAVA_HOME.
        try {
            JavaCompiler compiler = new ExecJavaCompiler(true);

            if (verbose) {
                // rmi.console.0C=NOTE: Using JAVA_HOME Javac compiler
                System.err.println(Messages.getString("rmi.console.0C")); //$NON-NLS-1$
            }
            return compiler;
        } catch (JavaCompilerException e) {
            if (verbose) {
                // rmi.console.0D=NOTE: JAVA_HOME Javac compiler not found: {0}
                System.err.println(Messages.getString("rmi.console.0D", e)); //$NON-NLS-1$
            }
        }

        // Trying to run Javac executable from system path, as a last resort.
        return new ExecJavaCompiler(false);
    }
}
