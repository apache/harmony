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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;


/**
 * This class represents a Java Compiler executed with a simple Java call.
 * The class provides multiple constructors to allow flexible specification
 * of what class and method must be used.
 *
 * @author  Vasily Zakharov
 */
public class MethodJavaCompiler extends JavaCompiler {

    /**
     * Default name of a method to call in a compiler class.
     */
    public static final String DEFAULT_COMPILER_METHOD = "main"; //$NON-NLS-1$

    /**
     * Compiler method signature.
     */
    protected static final Class[] COMPILER_METHOD_SIGNATURE =
            new Class[] { String[].class };

    /**
     * Compiler instance.
     */
    protected Object compilerInstance;

    /**
     * Compiler method.
     */
    protected Method compilerMethod;

    /**
     * Creates uninitialized instance of this class.
     * Note that using this constructor in most cases
     * requires overriding {@link #run(String[])} method.
     */
    protected MethodJavaCompiler() {}

    /**
     * Configures this class to use {@link #DEFAULT_COMPILER_METHOD} method
     * of the specified class to compile.
     *
     * Equivalent to {@link #MethodJavaCompiler(String, ClassLoader, String)
     * MethodJavaCompiler(className, null, null)}.
     *
     * @param   className
     *          Name of the compiler class to use.
     *
     * @throws  JavaCompilerException
     *          If error occurs while finding or loading
     *          the compiler class or method.
     */
    public MethodJavaCompiler(String className) throws JavaCompilerException  {
        this(className, null, null);
    }

    /**
     * Configures this class to use {@link #DEFAULT_COMPILER_METHOD} method
     * of the specified class (loaded by the specified class loader) to compile.
     *
     * Equivalent to {@link #MethodJavaCompiler(String, ClassLoader, String)
     * MethodJavaCompiler(className, classLoader, null)}.
     *
     * @param   className
     *          Name of the compiler class to use.
     *
     * @param   classLoader
     *          Class loader to use to instantiate the specified class.
     *          Can be <code>null</code>, in this case
     *          the default class loader is used.
     *
     * @throws  JavaCompilerException
     *          If error occurs while finding or loading
     *          the compiler class or method.
     */
    public MethodJavaCompiler(String className, ClassLoader classLoader)
            throws JavaCompilerException {
        this(className, classLoader, null);
    }

    /**
     * Configures this class to use the specified class and method to compile.
     *
     * Equivalent to {@link #MethodJavaCompiler(String, ClassLoader, String)
     * MethodJavaCompiler(className, null, methodName)}.
     *
     * @param   className
     *          Name of the compiler class to use.
     *
     * @param   methodName
     *          Name of the method to use.
     *          Can be <code>null</code>, in this case the
     *          {@linkplain #DEFAULT_COMPILER_METHOD default method name}
     *          is used.
     *
     * @throws  JavaCompilerException
     *          If error occurs while finding or loading
     *          the compiler class or method.
     */
    public MethodJavaCompiler(String className, String methodName)
            throws JavaCompilerException {
        this(className, null, methodName);
    }

    /**
     * Configures this class to use the specified class
     * (loaded by the specified class loader) and method to compile.
     *
     * Equivalent to {@link #MethodJavaCompiler(Class, String)
     * MethodJavaCompiler(Class.forName(className, true, classLoader),
     * methodName)}.
     *
     * @param   className
     *          Name of the compiler class to use.
     *
     * @param   classLoader
     *          Class loader to use to instantiate the specified class.
     *          Can be <code>null</code>, in this case
     *          the default class loader is used.
     *
     * @param   methodName
     *          Name of the method to use.
     *          Can be <code>null</code>, in this case the
     *          {@linkplain #DEFAULT_COMPILER_METHOD default method name}
     *          is used.
     *
     * @throws  JavaCompilerException
     *          If error occurs while finding or loading
     *          the compiler class or method.
     */
    public MethodJavaCompiler(
            String className, ClassLoader classLoader, String methodName)
            throws JavaCompilerException {
        this(getClass(className, classLoader), methodName);
    }

    /**
     * Configures this class to use method
     * {@link #DEFAULT_COMPILER_METHOD}<code>(String[])</code>
     * of the specified class to compile.
     *
     * Equivalent to {@link #MethodJavaCompiler(Class, String)
     * MethodJavaCompiler(c, null)}.
     *
     * @param   c
     *          Compiler class to use.
     *
     * @throws  JavaCompilerException
     *          If error occurs while finding or loading
     *          the compiler method.
     */
    public MethodJavaCompiler(Class c) throws JavaCompilerException {
        this(c, null);
    }

    /**
     * Configures this class to use the specified class and method to compile.
     *
     * @param   c
     *          Compiler class to use.
     *
     * @param   methodName
     *          Name of the method to use.
     *          Can be <code>null</code>, in this case the
     *          {@linkplain #DEFAULT_COMPILER_METHOD default method name}
     *          is used.
     *
     * @throws  JavaCompilerException
     *          If error occurs while finding or loading
     *          the compiler method.
     */
    public MethodJavaCompiler(Class c, String methodName)
            throws JavaCompilerException {
        this(getMethod(c, ((methodName != null)
                ? methodName : DEFAULT_COMPILER_METHOD)));
    }

    /**
     * Configures this class to use the specified method to compile.
     *
     * @param   method
     *          Compiler method to use.
     *
     * @throws  JavaCompilerException
     *          If specified method is an instance method,
     *          and the compiler class cannot be instantiated.
     */
    public MethodJavaCompiler(Method method) throws JavaCompilerException {
        try {
            compilerInstance = (Modifier.isStatic(method.getModifiers())
                    ? null : method.getDeclaringClass().newInstance());
        } catch (InstantiationException e) {
            throw new JavaCompilerException(e);
        } catch (IllegalAccessException e) {
            throw new JavaCompilerException(e);
        } catch (ExceptionInInitializerError e) {
            throw new JavaCompilerException(e);
        }
        compilerMethod = method;
    }

    /**
     * Runs the compilation process with the specified arguments.
     *
     * @param   args
     *          Full arguments list.
     *
     * @return  Java Compiler return value.
     *          For this particular implementation, always <code>0</code>,
     *          unless exception is thrown.
     *
     * @throws  JavaCompilerException
     *          If some error occurs during invocation.
     */
    protected int run(String[] args) throws JavaCompilerException {
        try {
            compilerMethod.invoke(compilerInstance, new Object[] { args });
            return 0;
        } catch (IllegalAccessException e) {
            throw new JavaCompilerException(e);
        } catch (InvocationTargetException e) {
            throw new JavaCompilerException(e.getCause());
        }
    }

    /**
     * Tries to create a class loader for the specified JAR file.
     *
     * @param   jar
     *          Jar file to create class loader for.
     *
     * @return  Class loader for the specified JAR file.
     *
     * @throws  JavaCompilerException
     *          If file name parsing failed.
     */
    protected static ClassLoader getClassLoaderFromJarFile(File jar)
            throws JavaCompilerException {
        try {
            return new URLClassLoader(new URL[] { jar.toURI().toURL() });
        } catch (MalformedURLException e) {
            throw new JavaCompilerException(e);
        }
    }

    /**
     * Loads the specified class with the specified class loader.
     * Wraps the possible exceptions to {@link JavaCompilerException}.
     *
     * @param   className
     *          Name of the class to get.
     *
     * @param   classLoader
     *          Class loader to use to instantiate the specified class.
     *          Can be <code>null</code>, in this case
     *          the default class loader is used.
     *
     * @return  The loaded class.
     *
     * @throws  JavaCompilerException
     *          If error occurs while trying to load the class.
     */
    protected static Class getClass(String className, ClassLoader classLoader)
            throws JavaCompilerException {
        try {
            return Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new JavaCompilerException(e);
        } catch (LinkageError e) {
            throw new JavaCompilerException(e);
        }
    }

    /**
     * Returns the specified compiler method for the specified class.
     * Wraps the possible exceptions to {@link JavaCompilerException}.
     *
     * @param   c
     *          Class to get method for.
     *
     * @param   methodName
     *          Name of the method to get.
     *
     * @return  The resulting {@link Method} object.
     *
     * @throws  JavaCompilerException
     *          If method is not found.
     */
    protected static Method getMethod(Class c, String methodName)
            throws JavaCompilerException {
        return getMethod(c, methodName, COMPILER_METHOD_SIGNATURE);
    }

    /**
     * Returns the specified method for the specified class.
     * Wraps the possible exceptions to {@link JavaCompilerException}.
     *
     * @param   c
     *          Class to get method for.
     *
     * @param   methodName
     *          Name of the method to get.
     *
     * @param   methodSignature
     *          Signature of the method to get.
     *
     * @return  The resulting {@link Method} object.
     *
     * @throws  JavaCompilerException
     *          If method is not found.
     */
    protected static Method getMethod(
            Class c, String methodName, Class[] methodSignature)
            throws JavaCompilerException {
        try {
            return c.getMethod(methodName, methodSignature);
        } catch (NoSuchMethodException e) {
            throw new JavaCompilerException(e);
        }
    }
}
