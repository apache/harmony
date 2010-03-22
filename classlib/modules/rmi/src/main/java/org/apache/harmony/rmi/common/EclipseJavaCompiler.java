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

import java.lang.reflect.InvocationTargetException;


/**
 * This class represents an Eclipse Java Compiler
 * executed with a simple Java call.
 *
 * @author  Vasily Zakharov
 */
public class EclipseJavaCompiler extends MethodJavaCompiler {

    /**
     * Eclipse Compiler class name.
     */
    public static final String ECLIPSE_COMPILER_CLASS_NAME =
            "org.eclipse.jdt.internal.compiler.batch.Main"; //$NON-NLS-1$

    /**
     * Eclipse Compiler method name.
     */
    protected final static String ECLIPSE_COMPILER_METHOD_NAME = "compile"; //$NON-NLS-1$

    /**
     * Eclipse Compiler method signature.
     */
    protected static final Class[] ECLIPSE_COMPILER_METHOD_SIGNATURE =
            new Class[] { String.class };

    /**
     * Name of the environment variable specifying the Eclipse location.
     */
    public static final String ECLIPSE_HOME_VARIABLE = "ECLIPSE_HOME"; //$NON-NLS-1$

    /**
     * Path from {@link #ECLIPSE_HOME_VARIABLE ECLIPSE_HOME}
     * to {@linkplain #ECLIPSE_JAR_PATTERN Eclipse Compiler JAR}.
     */
    public static final String ECLIPSE_JAR_PATH = "plugins"; //$NON-NLS-1$

    /**
     * File name pattern describing Eclipse Compiler JAR file name.
     */
    public static final String ECLIPSE_JAR_PATTERN =
            "^org.eclipse.jdt.core_.+\\.jar$"; //$NON-NLS-1$

    /**
     * Creates instance of this class, equivalent to
     * {@link #EclipseJavaCompiler(ClassLoader) EclipseJavaCompiler(null)}.
     *
     * @throws  JavaCompilerException
     *          If error occurs while finding or loading
     *          the Eclipse Compiler class or method.
     */
    public EclipseJavaCompiler() throws JavaCompilerException {
        this(null);
    }

    /**
     * Creates instance of this class by trying to load the
     * {@linkplain #ECLIPSE_COMPILER_CLASS_NAME Eclipse Compiler class}
     * with the specified class loader.
     *
     * If the specified class loader is <code>null</code>, tries to load
     * with the default class loader, and if failed, tries to load
     * from {@link #ECLIPSE_HOME_VARIABLE ECLIPSE_HOME} location.
     *
     * @param   classLoader
     *          Class loader to use to load the compiler class.
     *          Can be <code>null</code>.
     *
     * @throws  JavaCompilerException
     *          If error occurs while finding or loading
     *          the Eclipse Compiler class or method.
     */
    public EclipseJavaCompiler(ClassLoader classLoader)
            throws JavaCompilerException {
        Class compilerClass = null;

        try {
            compilerClass = getClass(ECLIPSE_COMPILER_CLASS_NAME, classLoader);
        } catch (JavaCompilerException e) {
            if (classLoader != null) {
                throw e;
            }
        }

        if (compilerClass == null) {
            compilerClass = getClass(ECLIPSE_COMPILER_CLASS_NAME,
                    getClassLoaderFromJarFile(
                    getFileFromVariable(ECLIPSE_HOME_VARIABLE,
                            ECLIPSE_JAR_PATH, ECLIPSE_JAR_PATTERN,
                            "Eclipse Compiler JAR"))); //$NON-NLS-1$
        }

        compilerMethod = getMethod(compilerClass, ECLIPSE_COMPILER_METHOD_NAME,
                ECLIPSE_COMPILER_METHOD_SIGNATURE);
    }

    /**
     * {@inheritDoc}
     */
    public int compile(String commandString) throws JavaCompilerException {
        try {
            Object ret = compilerMethod.invoke(null,
                    new Object[] { commandString });
            return (((Boolean) ret).booleanValue() ? 0 : -1);
        } catch (IllegalAccessException e) {
            throw new JavaCompilerException(e);
        } catch (InvocationTargetException e) {
            throw new JavaCompilerException(e.getCause());
        }
    }

    /**
     * {@inheritDoc}
     */
    protected int run(String[] args) throws JavaCompilerException {
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                buffer.append(' ');
            }
            buffer.append(args[i]);
        }
        return compile(buffer.toString());
    }
}
