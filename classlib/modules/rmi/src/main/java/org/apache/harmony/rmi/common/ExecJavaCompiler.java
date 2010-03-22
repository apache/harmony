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

import java.io.IOException;


/**
 * This class represents a Java Compiler executed as an external program.
 *
 * @author  Vasily Zakharov
 */
public class ExecJavaCompiler extends JavaCompiler {

    /**
     * Default name of a program to execute.
     */
    public static final String DEFAULT_COMPILER_PROGRAM = "javac"; //$NON-NLS-1$

    /**
     * Name of the system variable specifying the Java location.
     */
    public static final String JAVA_HOME_VARIABLE = "JAVA_HOME"; //$NON-NLS-1$

    /**
     * Path from {@link #JAVA_HOME_VARIABLE JAVA_HOME}
     * to {@linkplain #JAVA_COMPILER_PATTERN Javac executable}.
     */
    public static final String JAVA_COMPILER_PATH = "bin"; //$NON-NLS-1$

    /**
     * File name pattern describing Javac executable file name.
     */
    public static final String JAVA_COMPILER_PATTERN =
            "((javac)|(JAVAC))(|(\\.(exe|EXE)))"; //$NON-NLS-1$

    /**
     * Creates uninitialized instance of this class.
     * Note that using this constructor in most cases
     * requires overriding {@link #run(String[])} method.
     */
    protected ExecJavaCompiler() {}

    /**
     * Configures this class to use the
     * {@linkplain #DEFAULT_COMPILER_PROGRAM default program name} to compile.
     *
     * @param   search
     *          If <code>true</code> the constructor tries to locate the Javac
     *          executable at {@link #JAVA_HOME_VARIABLE JAVA_HOME} location,
     *          this is equivalent to {@link #ExecJavaCompiler(String)
     *          ExecJavaCompiler(null)}.
     *          Otherwise, the {@linkplain #DEFAULT_COMPILER_PROGRAM}
     *          is used in hope that it would be found in system path,
     *          this is equivalent to {@link #ExecJavaCompiler(String)
     *          ExecJavaCompiler(DEFAULT_COMPILER_PROGRAM)}.
     *
     * @throws  JavaCompilerException
     *          If error occurs during searching for executable
     *          at {@link #JAVA_HOME_VARIABLE}.
     */
    public ExecJavaCompiler(boolean search) throws JavaCompilerException {
        this(search ? null : DEFAULT_COMPILER_PROGRAM);
    }

    /**
     * Configures this class to use the specified program to compile.
     * If <code>programName</code> is <code>null</code>, tries to locate
     * the Javac executable at {@link #JAVA_HOME_VARIABLE JAVA_HOME} location.
     *
     * @param   programName
     *          Name of the program to execute. Can be <code>null</code>.
     *
     * @throws  JavaCompilerException
     *          If <code>programName</code> is <code>null</code>
     *          and error occurs during searching for Javac executable
     *          at {@link #JAVA_HOME_VARIABLE}.
     */
    public ExecJavaCompiler(String programName) throws JavaCompilerException {
        compilerOptions = new String[] { (programName == null)
                ? getFileFromVariable(JAVA_HOME_VARIABLE, JAVA_COMPILER_PATH,
                        JAVA_COMPILER_PATTERN, "Java compiler").getPath() //$NON-NLS-1$
                : programName };
    }

    /**
     * Runs the compilation process with the specified arguments.
     *
     * @param   args
     *          Full arguments list.
     *
     * @return  Java Compiler return value.
     *
     * @throws  JavaCompilerException
     *          If execution failed for some reason.
     */
    protected int run(String[] args) throws JavaCompilerException {
        try {
            // @ToDo: Rewrite with ProcessBuilder for Java 5.0.
            return new SubProcess(args, System.out, null, System.err).waitFor();
        } catch (IOException e) {
            throw new JavaCompilerException(e);
        }
    }
}
