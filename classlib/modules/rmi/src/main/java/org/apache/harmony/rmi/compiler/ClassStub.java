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
package org.apache.harmony.rmi.compiler;

import java.lang.reflect.Method;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.harmony.rmi.common.ClassList;
import org.apache.harmony.rmi.common.RMIHash;
import org.apache.harmony.rmi.common.RMIHashException;
import org.apache.harmony.rmi.common.RMIUtil;
import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Generates RMI stub code for a particular class.
 *
 * @author  Vasily Zakharov
 */
final class ClassStub implements RmicConstants {

    /**
     * Should the stub support v1.1.
     */
    final boolean v11;

    /**
     * Should the stub support v1.2.
     */
    final boolean v12;

    /**
     * Should the stub support both v1.1 and v1.2.
     */
    final boolean vCompat;

    /**
     * Indenter to write source files.
     */
    Indenter indenter;

    /**
     * Name of the class to generate stub for.
     */
    private final String className;

    /**
     * Package of the class to generate stub for.
     */
    private final String packageName;

    /**
     * Stub class name.
     */
    private final String stubName;

    /**
     * Skeleton class name.
     */
    private final String skelName;

    /**
     * List of remote interfaces for the class.
     */
    private final Class[] interfaces;

    /**
     * List of remote interfaces methods for the class.
     */
    private final Vector methods;

    /**
     * <code>true</code> if methods exist in {@link #methods} vector.
     */
    private final boolean methodsExist;

    /**
     * Class interface hash.
     */
    private final long interfaceHash;

    /**
     * Creates <code>ClassStub</code> instance for specified version and class.
     *
     * @param   version
     *          Version of the stub to create.
     *
     * @param   className
     *          Name of the class to load.
     *
     * @throws  RMICompilerException
     *          If version number is incorrect or some other error occurs.
     */
    ClassStub(int version, String className) throws RMICompilerException {
        this(version, getClass(className));
    }

    /**
     * Creates <code>ClassStub</code> instance for specified version and class.
     *
     * @param   version
     *          Version of the stub to create.
     *
     * @param   cls
     *          Class to load.
     *
     * @throws  RMICompilerException
     *          If version number is incorrect or some other error occurs.
     */
    ClassStub(int version, Class cls) throws RMICompilerException {
        // Check version.
        if ((version < MIN_VERSION) || (version > MAX_VERSION)) {
            // rmi.50=Incorrect version specified.
            throw new RMICompilerException(Messages.getString("rmi.50")); //$NON-NLS-1$
        }

        // Set appropriate version flags.
        switch (version) {
        case VERSION_V11:
            v11 = true;
            v12 = false;
            vCompat = false;
            break;
        case VERSION_V12:
            v11 = false;
            v12 = true;
            vCompat = false;
            break;
        case VERSION_VCOMPAT:
            v11 = true;
            v12 = true;
            vCompat = true;
            break;
        default:
            // rmi.51=Version currently unsupported.
            throw new RMICompilerException(Messages.getString("rmi.51")); //$NON-NLS-1$
        }

        className = RMIUtil.getCanonicalName(cls);

        // Check if the specified class is interface.
        if (cls.isInterface()) {
            // rmi.52=Class {0} is interface, and so does not need an RMI stub.
            throw new RMICompilerException(Messages.getString("rmi.52", className)); //$NON-NLS-1$
        }

        // Check if the specified class is remote.
        if (!Remote.class.isAssignableFrom(cls)) {
            // rmi.53=Class {0} does not implement a remote interface, and so does not need an RMI stub.
            throw new RMICompilerException(Messages.getString("rmi.53", className)); //$NON-NLS-1$
        }

        // Check if the specified class implements any remote interfaces.
        if (!new ClassList(cls.getInterfaces()).contains(Remote.class)) {
            // rmi.54=Class {0} does not directly implement a remote interface, and so does not need an RMI stub.
            throw new RMICompilerException(Messages.getString("rmi.54", className)); //$NON-NLS-1$
        }

        // Initialize class variables.
        packageName = RMIUtil.getPackageName(cls);

        String shortClassName = RMIUtil.getShortName(cls);
        stubName = shortClassName + stubSuffix;
        skelName = shortClassName + skelSuffix;

        try {
            interfaces = RMIUtil.getRemoteInterfaces(cls);
        } catch (IllegalArgumentException e) {
            throw new RMICompilerException(e);
        }

        methods = new Vector();

        // Create temporal method stubs table (sorted, no duplicates).
        TreeMap methodMap = new TreeMap();

        // Extract remote methods from remote interfaces of cls and ancestors.
        for (int i = 0; i < interfaces.length; i++) {
            // Add public methods from this interface to the map.
            RMIHash.getSortedMethodMap(methodMap, interfaces[i].getMethods());
        }

        if (v11) {
            try {
                // Calculate interface hash value for the set of methods found.
                interfaceHash = RMIHash.getInterfaceHash(methodMap);
            } catch (RMIHashException e) {
                throw new RMICompilerException(e.getMessage(), e.getCause());
            }
        } else {
            interfaceHash = (-1L);
        }

        // Put the complete MethodStub objects to methods list.
        int n = 0;
        for (Iterator i = methodMap.values().iterator(); i.hasNext(); ) {
            methods.add(new MethodStub((Method) i.next(), n++));
        }

        methodsExist = (n > 0);
    }

    /**
     * Returns the class object for the specified class name.
     *
     * @param   className
     *          Class name.
     *
     * @return  Class object for the specified class name.
     *
     * @throws  RMICompilerException
     *          If class is not found.
     */
    private static Class getClass(String className)
            throws RMICompilerException {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            // rmi.55=Class not found: {0}
            throw new RMICompilerException(Messages.getString("rmi.55", className)); //$NON-NLS-1$
        }
    }

    /**
     * Returns stub class name for loaded class.
     *
     * @return  Stub class name.
     */
    String getStubClassName() {
        return stubName;
    }

    /**
     * Returns skeleton class name for loaded class.
     *
     * @return  Skeleton class name.
     */
    String getSkeletonClassName() {
        return skelName;
    }

    /**
     * Returns stub source code for the loaded class (v1.1/v1.2).
     *
     * @return  String containing the stub source code for loaded class.
     */
    String getStubSource() {
        indenter = new Indenter();

        return (getStubHeader("stub") + getPackageStatement() + EOLN //$NON-NLS-1$
                + getStubClassDeclaration() + indenter.hIncrease()
                + (v12 ? (EOLN + getSerialVersionUID()) : "") //$NON-NLS-1$
                + (v11 ? (EOLN + getInterfaceHash() + (methodsExist
                        ? ((vCompat ? EOLN + getNewInvoke() : "") //$NON-NLS-1$
                        + EOLN + getOperationsArrayDeclaration()) : "")) : "") //$NON-NLS-1$ //$NON-NLS-2$
                + ((v12 && methodsExist) ? (EOLN
                        + getMethodVariablesDeclaration() + EOLN
                        + getStaticInitializationBlock()) : "") + EOLN //$NON-NLS-1$
                + getStubConstructors()
                + (methodsExist ? getMethodImplementations() : "") //$NON-NLS-1$
                + indenter.decrease() + '}' + EOLN + indenter.assertEmpty());
    }

    /**
     * Returns skeleton source code for the loaded class (v1.1).
     *
     * @return  String containing the skeleton source code for loaded class.
     */
    String getSkeletonSource() {
        indenter = new Indenter();

        return (getStubHeader("skeleton") + getPackageStatement() + EOLN //$NON-NLS-1$
                + getSkeletonClassDeclaration()
                + indenter.hIncrease() + EOLN + getInterfaceHash() + EOLN
                + getOperationsArrayDeclaration() + EOLN
                + getOperationsMethod() + EOLN + getDispatchMethod()
                + indenter.decrease() + '}' + EOLN + indenter.assertEmpty());
    }

    /**
     * Returns stub source code header
     * (Stub/Skeleton v1.1/v1.2/vCompat).
     *
     * @param   type
     *          Header type, <code>"stub"</code> or <code>"skeleton"</code>.
     *
     * @return  Stub/skeleton header.
     */
    private String getStubHeader(String type) {
        return ("/*" + EOLN + " * RMI " + type + " class" + EOLN //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + " * for class " + className + EOLN //$NON-NLS-1$
                + " * Compatible with stub protocol version " //$NON-NLS-1$
                + (v11 ? "1.1" : "") + (vCompat ? "/" : "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                + (v12 ? "1.2" : "") + EOLN + " *" + EOLN //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + " * Generated by DRL RMI Compiler (rmic)." + EOLN //$NON-NLS-1$
                + " *" + EOLN + " * DO NOT EDIT!!!" + EOLN //$NON-NLS-1$ //$NON-NLS-2$
                + " * Contents subject to change without notice!" + EOLN //$NON-NLS-1$
                + " */" + EOLN); //$NON-NLS-1$
    }

    /**
     * Returns <code>package</code> statement
     * (Stub/Skeleton v1.1/v1.2/vCompat).
     *
     * @return  <code>package</code> statement for stub/skeleton class.
     */
    private String getPackageStatement() {
        return ((packageName == null) ? "" //$NON-NLS-1$
                : ("package " + packageName + ';' + EOLN + EOLN)); //$NON-NLS-1$
    }

    /**
     * Returns stub class declaration
     * (Stub v1.1/v1.2/vCompat).
     *
     * @return  Stub class declaration statement.
     */
    private String getStubClassDeclaration() {
        StringBuilder buffer = new StringBuilder("public final class " //$NON-NLS-1$
                + stubName + " extends java.rmi.server.RemoteStub" + EOLN //$NON-NLS-1$
                + indenter.tIncrease(2) + "implements "); //$NON-NLS-1$

        // Add implemented interfaces list.
        for (int i = 0; i < interfaces.length; i++) {
            buffer.append(((i > 0) ? ", " : "" ) + interfaces[i].getName()); //$NON-NLS-1$ //$NON-NLS-2$
        }

        buffer.append(" {" + EOLN); //$NON-NLS-1$

        return buffer.toString();
    }

    /**
     * Returns skeleton class declaration
     * (Skeleton v1.1/vCompat).
     *
     * @return  Skeleton class declaration statement.
     */
    private String getSkeletonClassDeclaration() {
        return ("public final class " + skelName //$NON-NLS-1$
                + " implements java.rmi.server.Skeleton {" + EOLN); //$NON-NLS-1$
    }

    /**
     * Returns <code>serialVersionUID</code> declaration
     * (Stub v1.2/vCompat).
     *
     * @return  <code>serialVersionUID</code> declaration statement.
     */
    private String getSerialVersionUID() {
        return (indenter.indent()
                    + "private static final long serialVersionUID = 2;" + EOLN); //$NON-NLS-1$
    }

    /**
     * Returns <code>interfaceHash</code> declaration
     * (Stub/Skeleton v1.1/vCompat).
     *
     * @return  <code>interfaceHash</code> declaration statement.
     */
    private String getInterfaceHash() {
        return (indenter.indent() + "private static final long " //$NON-NLS-1$
                + interfaceHashVarName + " = " + interfaceHash + "L;" + EOLN); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Returns <code>useNewInvoke</code> declaration
     * (Stub vCompat).
     *
     * @return  <code>useNewInvoke</code> declaration statement.
     */
    private String getNewInvoke() {
        return (indenter.indent()
                + "private static boolean " + useNewInvoke + ';'+ EOLN); //$NON-NLS-1$
    }

    /**
     * Returns <code>operations</code> array declaration
     * (Stub/Skeleton v1.1/vCompat).
     *
     * @return  <code>operations</code> array declaration statement.
     */
    private String getOperationsArrayDeclaration() {
        StringBuilder buffer = new StringBuilder(indenter.indent()
                + "private static final java.rmi.server.Operation[]" //$NON-NLS-1$
                + " operations = {"); //$NON-NLS-1$

        if (methodsExist) {
            buffer.append(EOLN + indenter.hIncrease());

            for (Iterator i = methods.iterator(); i.hasNext(); ) {
                buffer.append(((MethodStub) i.next()).getOpsArrayElement()
                        + (i.hasNext() ? "," : "" ) + EOLN); //$NON-NLS-1$ //$NON-NLS-2$
            }

            buffer.append(indenter.decrease());
        }

        buffer.append("};" + EOLN); //$NON-NLS-1$

        return buffer.toString();
    }

    /**
     * Returns <code>getOperations()</code> method declaration
     * (Skeleton v1.1/vCompat).
     *
     * @return  <code>getOperations()</code> declaration statement.
     */
    private String getOperationsMethod() {
        return (indenter.indent()
                + "public java.rmi.server.Operation[] getOperations() {" + EOLN //$NON-NLS-1$
                + indenter.tIncrease()
                + "return (java.rmi.server.Operation[]) operations.clone();" //$NON-NLS-1$
                + EOLN + indenter.indent() + '}' + EOLN);
    }

    /**
     * Returns <code>dispatch()</code> method declaration
     * (Skeleton v1.1/vCompat).
     *
     * @return  <code>dispatch()</code> method declaration statement.
     */
    private String getDispatchMethod() {
        StringBuilder buffer = new StringBuilder(indenter.indent()
                + "public void dispatch(java.rmi.Remote obj, " //$NON-NLS-1$
                + "java.rmi.server.RemoteCall call, int opnum, long hash) " //$NON-NLS-1$
                + "throws java.lang.Exception {" + EOLN + indenter.hIncrease()); //$NON-NLS-1$

        if (vCompat) {
            buffer.append(indenter.indent() + "if (opnum < 0) {" + EOLN //$NON-NLS-1$
                    + indenter.increase());

            if (methodsExist) {
                for (Iterator i = methods.iterator(); i.hasNext(); ) {
                    buffer.append(((MethodStub) i.next()).getHashCheck());
                }
                buffer.append('{' + EOLN + indenter.increase());
            }

            buffer.append("throw new java.rmi.UnmarshalException(" //$NON-NLS-1$
                    + "\"Invalid method hash: \" + hash);" + EOLN //$NON-NLS-1$
                    + indenter.decrease() + '}' + EOLN
                    + (methodsExist ? (indenter.tDecrease() + "} else {") : "") //$NON-NLS-1$ //$NON-NLS-2$
                    + EOLN);
        }

        buffer.append(indenter.indent() + "if (hash != interfaceHash) {" + EOLN //$NON-NLS-1$
                + indenter.increase()
                + "throw new java.rmi.server.SkeletonMismatchException(" + EOLN //$NON-NLS-1$
                + indenter.tIncrease(2)
                + "\"Interface hash mismatch, expected: \" + interfaceHash" //$NON-NLS-1$
                + " + \", received: \" + hash);" + EOLN //$NON-NLS-1$
                + indenter.decrease() + '}' + EOLN + ((vCompat && methodsExist)
                        ? (indenter.decrease() + '}' + EOLN) : "") + EOLN //$NON-NLS-1$
                + indenter.indent() + className + " server = " //$NON-NLS-1$
                    + '(' + className + ") obj;" + EOLN + EOLN); //$NON-NLS-1$

        if (methodsExist) {
            buffer.append(indenter.indent() + "switch (opnum) {" + EOLN); //$NON-NLS-1$

            for (Iterator i = methods.iterator(); i.hasNext(); ) {
                buffer.append(EOLN + ((MethodStub) i.next()).getDispatchCase());
            }

            buffer.append(EOLN + indenter.indent() + "default:" + EOLN); //$NON-NLS-1$

            indenter.increase();
        }

        buffer.append(indenter.indent()
                + "throw new java.rmi.UnmarshalException(" //$NON-NLS-1$
                + "\"Invalid method number: \" + opnum);" + EOLN //$NON-NLS-1$
                + (methodsExist ? (indenter.decrease() + '}' + EOLN) : "") //$NON-NLS-1$
                + indenter.decrease() + '}' + EOLN);

        return buffer.toString();
    }

    /**
     * Returns method variables declaration block
     * (Stub v1.2/vCompat).
     *
     * @return  Variables declaration block.
     */
    private String getMethodVariablesDeclaration() {
        StringBuilder buffer = new StringBuilder();

        for (Iterator i = methods.iterator(); i.hasNext(); ) {
            buffer.append(((MethodStub) i.next()).getVariableDecl());
        }

        return buffer.toString();
    }

    /**
     * Creates static initialization block
     * (Stub v1.2/vCompat).
     *
     * @return  Static initialization declaration block.
     */
    private String getStaticInitializationBlock() {
        StringBuilder buffer = new StringBuilder(indenter.indent()
                + "static {" + EOLN + indenter.increase() + "try {" + EOLN //$NON-NLS-1$ //$NON-NLS-2$
                + indenter.hIncrease());

        if (vCompat) {
            buffer.append(indenter.indent()
                    + "java.rmi.server.RemoteRef.class.getMethod(\"invoke\", " //$NON-NLS-1$
                    + "new java.lang.Class[] {java.rmi.Remote.class, " //$NON-NLS-1$
                    + "java.lang.reflect.Method.class, java.lang.Object[].class" //$NON-NLS-1$
                    + ", long.class});" + EOLN + EOLN); //$NON-NLS-1$
        }

        for (Iterator i = methods.iterator(); i.hasNext(); ) {
            buffer.append(((MethodStub) i.next()).getVariableInit());
        }

        buffer.append((vCompat ? (EOLN + indenter.indent() + useNewInvoke
                + " = true;" + EOLN) : "") + indenter.decrease() //$NON-NLS-1$ //$NON-NLS-2$
                + "} catch (java.lang.NoSuchMethodException e) {" + EOLN //$NON-NLS-1$
                + indenter.increase()
                + (vCompat ? (useNewInvoke + " = false;") //$NON-NLS-1$
                        : ("throw new java.lang.NoSuchMethodError(" + EOLN //$NON-NLS-1$
                                + indenter.tIncrease(2)
                                + "\"Stub class initialization failed: " //$NON-NLS-1$
                                + ((packageName != null)
                                        ? (packageName + '.') : "") //$NON-NLS-1$
                                + stubName + "\");")) + EOLN //$NON-NLS-1$
                + indenter.decrease() + '}' + EOLN
                + indenter.decrease() + '}' + EOLN);

        return buffer.toString();
    }

    /**
     * Returns stub constructors
     * (Stub v1.1/v1.2/vCompat).
     *
     * @return  Stub constructors code.
     */
    private String getStubConstructors() {
        StringBuilder buffer = new StringBuilder();

        if (v11) {
            buffer.append(indenter.indent() + "public " + stubName //$NON-NLS-1$
                    + "() {" + EOLN + indenter.tIncrease() + "super();" + EOLN //$NON-NLS-1$ //$NON-NLS-2$
                    + indenter.indent() + '}' + EOLN + EOLN);
        }

        buffer.append(indenter.indent() + "public " + stubName //$NON-NLS-1$
                + "(java.rmi.server.RemoteRef ref) {" + EOLN //$NON-NLS-1$
                + indenter.tIncrease() + "super(ref);" + EOLN //$NON-NLS-1$
                + indenter.indent() + '}' + EOLN);

        return buffer.toString();
    }

    /**
     * Returns remote methods implementation
     * (Stub v1.1/v1.2/vCompat).
     *
     * @return  Stub method implementations code.
     */
    private String getMethodImplementations() {
        StringBuilder buffer = new StringBuilder();

        for (Iterator i = methods.iterator(); i.hasNext(); ) {
            buffer.append(EOLN + ((MethodStub) i.next()).getStubImpl());
        }

        return buffer.toString();
    }

    /**
     * Generates RMI stub code for a particular method.
     */
    private final class MethodStub {

        /**
         * The method name (via {@link Method#getName()}).
         */
        private final String name;

        /**
         * The name of the interface declaring this method.
         */
        private final String interfaceName;

        /**
         * The method parameters (via {@link Method#getParameterTypes()}).
         */
        private final Class[] parameters;

        /**
         * The method parameters class names.
         */
        private final String[] paramClassNames;

        /**
         * The method parameters names.
         */
        private final String[] paramNames;

        /**
         * Number of parameters for this method.
         */
        private final int numParams;

        /**
         * The method return type (via {@link Method#getReturnType()}).
         */
        private final Class retType;

        /**
         * The method return type name.
         */
        private final String retTypeName;

        /**
         * Exceptions that this method throws.
         */
        private final Vector exceptions;

        /**
         * Exceptions that must be caught in method stub.
         */
        private final ClassList catches;

        /**
         * The method long signature
         * (via {@link RMIUtil#getLongMethodSignature(Method)
         * getLongMethodSignature()}).
         */
        private final String longSign;

        /**
         * The method short signature
         * (via {@link RMIUtil#getShortMethodSignature(Method)
         * getShortMethodSignature()}).
         */
        private final String shortSign;

        /**
         * The method hash (via {@link RMIHash#getMethodHash(Method)
         * getMethodHash()}).
         */
        private final long hash;

        /**
         * The method number in the stub.
         */
        private final int number;

        /**
         * Name of the variable containing this method in the stub
         * (<code>$method_<em>name</em>_<em>number</em></code>).
         */
        private final String varName;

        /**
         * Whether this method throws {@link Exception}.
         */
        private final boolean throwsException;

        /**
         * Creates method stub instance.
         *
         * @param   method
         *          Method to process.
         *
         * @param   number
         *          Method number in sorted methods table.
         *
         * @throws  RMICompilerException
         *          If some error occurs.
         */
        MethodStub(Method method, int number) throws RMICompilerException {
            this.name = method.getName();
            this.interfaceName = method.getDeclaringClass().getName();
            this.parameters = method.getParameterTypes();
            this.numParams = parameters.length;
            this.retType = method.getReturnType();
            this.retTypeName = RMIUtil.getCanonicalName(retType);
            this.longSign = RMIUtil.getLongMethodSignature(method);
            this.shortSign = RMIUtil.getShortMethodSignature(method);
            this.number = number;
            this.varName = (methodVarPrefix + name + '_' + number);

            try {
                this.hash = RMIHash.getMethodHash(method);
            } catch (RMIHashException e) {
                throw new RMICompilerException(e.getMessage(), e);
            }

            // Create parameter names array.
            paramClassNames = new String[numParams];
            paramNames = new String[numParams];
            for (int i = 0; i < numParams; i++) {
                Class parameter = parameters[i];
                paramClassNames[i] = RMIUtil.getCanonicalName(parameter);
                paramNames[i] = RmicUtil.getParameterName(parameter, i + 1);
            }

            // Create list of exceptions declared thrown.
            Class[] exceptionsArray = method.getExceptionTypes();
            exceptions = new Vector(exceptionsArray.length);
            exceptions.addAll(Arrays.asList(exceptionsArray));

            // Create list of exceptions to be caught.
            catches = new ClassList(true);

            // Add standard exceptions to make sure they are first in the list.
            catches.add(RuntimeException.class);
            catches.add(RemoteException.class);

            // Add declared thrown exceptions.
            catches.addAll(exceptions);

            throwsException = catches.contains(Exception.class);
        }

        /**
         * Returns <code>operations</code> array element for this method
         * (Stub/Skeleton v1.1/vCompat)
         *
         * @return  <code>operations</code> array element for this method.
         */
        String getOpsArrayElement() {
            return (indenter.indent() +
                        "new java.rmi.server.Operation(\"" + longSign + "\")"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        /**
         * Returns hash checking code for this method
         * (Skeleton vCompat).
         *
         * @return  Hash checking code for this method..
         */
        String getHashCheck() {
            return ("if (hash == " + hash + "L) {" + EOLN //$NON-NLS-1$ //$NON-NLS-2$
                    + indenter.tIncrease() + "opnum = " + number + ';' + EOLN //$NON-NLS-1$
                    + indenter.indent() + "} else "); //$NON-NLS-1$
        }

        /**
         * Returns <code>dispatch()</code> method case for this method
         * (Skeleton v1.1/vCompat).
         *
         * @return  <code>dispatch()</code> method case for this method.
         */
        String getDispatchCase() {
            StringBuilder buffer = new StringBuilder(indenter.indent()
                    + "case " + number + ": {    // " + shortSign + EOLN + EOLN //$NON-NLS-1$ //$NON-NLS-2$
                    + indenter.hIncrease());

            if (numParams > 0) {
                // Add parameters declaration.
                for (int i = 0; i < numParams; i++) {
                    buffer.append(indenter.indent() + paramClassNames[i]
                            + ' ' + paramNames[i] + ';' + EOLN);
                }

                // Access input stream.
                buffer.append(EOLN + indenter.indent() + "try {" + EOLN //$NON-NLS-1$
                        + indenter.increase() + "java.io.ObjectInput " //$NON-NLS-1$
                        + inputStreamName + " = call.getInputStream();" + EOLN); //$NON-NLS-1$

                boolean objectParametersExist = false;

                // Add parameters initialization, read them from the stream.
                for (int i = 0; i < numParams; i++) {
                    buffer.append(indenter.indent() + paramNames[i] + " = " //$NON-NLS-1$
                            + RmicUtil.getReadObjectString(parameters[i],
                                    inputStreamName) + ';' + EOLN);

                    if (!parameters[i].isPrimitive()) {
                        objectParametersExist = true;
                    }
                }

                // Add catch block.
                buffer.append(indenter.tDecrease()
                        + "} catch (java.io.IOException e) {" + EOLN //$NON-NLS-1$
                        + indenter.indent()
                        + "throw new java.rmi.UnmarshalException(" //$NON-NLS-1$
                        + "\"Error unmarshalling arguments\", e);" + EOLN //$NON-NLS-1$
                        + (objectParametersExist ? (indenter.tDecrease()
                        + "} catch (java.lang.ClassNotFoundException e) {" //$NON-NLS-1$
                        + EOLN + indenter.indent()
                        + "throw new java.rmi.UnmarshalException(" //$NON-NLS-1$
                        + "\"Error unmarshalling arguments\", e);" + EOLN) : "") //$NON-NLS-1$ //$NON-NLS-2$
                        + indenter.tDecrease() + "} finally {" + EOLN); //$NON-NLS-1$
            }
            // Release input stream.
            buffer.append(indenter.indent()
                    + "call.releaseInputStream();" + EOLN); //$NON-NLS-1$

            if (numParams > 0) {
                buffer.append(indenter.decrease() + '}' + EOLN);
            }

            buffer.append(EOLN + indenter.indent() + ((retType != void.class)
                    ? (retTypeName + ' ' + retVarName + " = ") : "") //$NON-NLS-1$ //$NON-NLS-2$
                    + "server." + name + '('); //$NON-NLS-1$

            for (int i = 0; i < numParams; i++) {
                buffer.append(((i > 0) ? ", " : "") + paramNames[i]); //$NON-NLS-1$ //$NON-NLS-2$
            }

            buffer.append(");" + EOLN + EOLN + indenter.indent() + "try {" //$NON-NLS-1$ //$NON-NLS-2$
                    + EOLN + indenter.increase() + ((retType != void.class)
                    ? ("java.io.ObjectOutput " + outputStreamName + " = ") : "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    + "call.getResultStream(true);" + EOLN //$NON-NLS-1$
                    + ((retType != void.class) ? (indenter.indent()
                            + RmicUtil.getWriteObjectString(retType, retVarName,
                                    outputStreamName) + ';' + EOLN) : "") //$NON-NLS-1$
                    + indenter.decrease() + "} catch (java.io.IOException e) {" //$NON-NLS-1$
                    + EOLN + indenter.tIncrease()
                    + "throw new java.rmi.MarshalException(" //$NON-NLS-1$
                    + "\"Error marshalling return\", e);" + EOLN //$NON-NLS-1$
                    + indenter.indent() + '}' + EOLN + EOLN
                    + indenter.indent() + "break;" + EOLN //$NON-NLS-1$
                    + indenter.decrease() + '}' + EOLN);

            return buffer.toString();
        }

        /**
         * Returns source code for the method variable declaration
         * (Stub v1.2/vCompat).
         *
         * @return  Method variable declaration.
         */
        String getVariableDecl() {
            return (indenter.indent()
                    + "private static java.lang.reflect.Method" //$NON-NLS-1$
                    + ' ' + varName + ';' + EOLN);
        }

        /**
         * Returns source code for the method variable initialization
         * (Stub v1.2/vCompat).
         *
         * @return  Method variable initialization.
         */
        String getVariableInit() {
            StringBuilder buffer = new StringBuilder(indenter.indent()
                    + varName + " = " + interfaceName + ".class.getMethod(\"" //$NON-NLS-1$ //$NON-NLS-2$
                    + name + "\", new java.lang.Class[] {"); //$NON-NLS-1$

            if (numParams > 0) {
                // Write method parameters.
                for (int i = 0; i < numParams; i++) {
                    buffer.append(((i > 0) ? ", " : "") //$NON-NLS-1$ //$NON-NLS-2$
                            + paramClassNames[i] + ".class"); //$NON-NLS-1$
                }
            }

            buffer.append("});" + EOLN); //$NON-NLS-1$

            return buffer.toString();
        }

        /**
         * Returns stub implementation for this method
         * (Stub v1.1/v1.2/vCompat).
         *
         * @return  Stub implementation for this method.
         */
        String getStubImpl() {
            return (getStubImplHeader()
                    + (vCompat ? (indenter.indent() + "if (" + useNewInvoke //$NON-NLS-1$
                            + ") {" + EOLN + indenter.hIncrease()) : "") //$NON-NLS-1$ //$NON-NLS-2$
                    + (v12 ? getStubImplCodeV12() : "") + (vCompat //$NON-NLS-1$
                            ? (indenter.tDecrease() + "} else {" + EOLN) : "") //$NON-NLS-1$ //$NON-NLS-2$
                    + (v11 ? getStubImplCodeV11() : "") //$NON-NLS-1$
                    + (vCompat ? (indenter.decrease() + '}' + EOLN) : "") //$NON-NLS-1$
                    + (throwsException ? "" : (indenter.hDecrease() //$NON-NLS-1$
                            + getStubImplCatchBlock()))
                    + indenter.decrease() + '}' + EOLN);
        }

        /**
         * Returns header for the stub implementation for this method
         * (Stub v1.1/v1.2/vCompat).
         *
         * @return  Stub implementation header for this method.
         */
        private String getStubImplHeader() {
            StringBuilder buffer = new StringBuilder(indenter.indent()
                    + "// Implementation of " + shortSign + EOLN //$NON-NLS-1$
                    + indenter.indent() + "public " + retTypeName //$NON-NLS-1$
                    + ' ' + name + '(');

            // Write method parameters.
            for (int i = 0; i < numParams; i++) {
                buffer.append(((i > 0) ? ", " : "") //$NON-NLS-1$ //$NON-NLS-2$
                        + paramClassNames[i] + ' ' + paramNames[i]);
            }

            buffer.append(')' + EOLN + indenter.tIncrease(2) + "throws "); //$NON-NLS-1$

            // Write exceptions declared thrown.
            for (Iterator i = exceptions.iterator(); i.hasNext(); ) {
                buffer.append(((Class) i.next()).getName()
                        + (i.hasNext() ? ", " : "" )); //$NON-NLS-1$ //$NON-NLS-2$
            }

            buffer.append(" {" + EOLN + indenter.hIncrease() //$NON-NLS-1$
                    + (throwsException ? "" : (indenter.indent() //$NON-NLS-1$
                            + "try {" + EOLN + indenter.hIncrease()))); //$NON-NLS-1$

            return buffer.toString();
        }

        /**
         * Returns the stub implementation code section source for this method
         * (Stub v1.1/vCompat).
         *
         * @return  Stub implementation code for this method.
         */
        private String getStubImplCodeV11() {
            StringBuilder buffer = new StringBuilder(indenter.indent()
                    + "java.rmi.server.RemoteCall call = " //$NON-NLS-1$
                    + "ref.newCall((java.rmi.server.RemoteObject) this, " //$NON-NLS-1$
                    + "operations, " + number + ", " + interfaceHashVarName //$NON-NLS-1$ //$NON-NLS-2$
                    + ");" + EOLN); //$NON-NLS-1$

            if (numParams > 0) {
                buffer.append(EOLN + indenter.indent() + "try {" + EOLN //$NON-NLS-1$
                    + indenter.increase() + "java.io.ObjectOutput " //$NON-NLS-1$
                    + outputStreamName + " = call.getOutputStream();" + EOLN); //$NON-NLS-1$

                for (int i = 0; i < numParams; i++) {
                    buffer.append(indenter.indent()
                            + RmicUtil.getWriteObjectString(parameters[i],
                                    paramNames[i], outputStreamName)
                            + ';' + EOLN);
                }

                buffer.append(indenter.decrease()
                        + "} catch (java.io.IOException e) {" + EOLN //$NON-NLS-1$
                        + indenter.tIncrease()
                        + "throw new java.rmi.MarshalException(" //$NON-NLS-1$
                        + "\"Error marshalling arguments\", e);" + EOLN //$NON-NLS-1$
                        + indenter.indent() + '}' + EOLN);
            }

            buffer.append(EOLN + indenter.indent()
                    + "ref.invoke(call);" + EOLN); //$NON-NLS-1$

            if (retType != void.class) {
                buffer.append(EOLN + indenter.indent()
                        + retTypeName + ' ' + retVarName + ';' + EOLN + EOLN
                        + indenter.indent() + "try {" + EOLN //$NON-NLS-1$
                        + indenter.increase() + "java.io.ObjectInput " //$NON-NLS-1$
                        + inputStreamName + " = call.getInputStream();" + EOLN //$NON-NLS-1$
                        + indenter.indent() + retVarName + " = " //$NON-NLS-1$
                        + RmicUtil.getReadObjectString(retType, inputStreamName)
                        + ';' + EOLN + indenter.decrease()
                        + "} catch (java.io.IOException e) {" + EOLN //$NON-NLS-1$
                        + indenter.tIncrease()
                        + "throw new java.rmi.UnmarshalException(" //$NON-NLS-1$
                        + "\"Error unmarshalling return value\", e);" + EOLN //$NON-NLS-1$
                        + (!retType.isPrimitive() ? (indenter.indent()
                        + "} catch (java.lang.ClassNotFoundException e) {" //$NON-NLS-1$
                        + EOLN + indenter.tIncrease()
                        + "throw new java.rmi.UnmarshalException(" //$NON-NLS-1$
                        + "\"Error unmarshalling return value\", e);" + EOLN) //$NON-NLS-1$
                        : "") + indenter.indent() + "} finally {" + EOLN //$NON-NLS-1$ //$NON-NLS-2$
                        + indenter.tIncrease() + "ref.done(call);" + EOLN //$NON-NLS-1$
                        + indenter.indent() + '}' + EOLN + EOLN
                        + indenter.indent() + "return " + retVarName + ';' //$NON-NLS-1$
                        + EOLN);
            } else {
                buffer.append(EOLN + indenter.indent()
                        + "ref.done(call);" + EOLN); //$NON-NLS-1$
            }

            return buffer.toString();
        }

        /**
         * Returns the stub  implementation code section source for this method
         * (Stub v1.2/vCompat).
         *
         * @return  Stub implementation code for this method.
         */
        private String getStubImplCodeV12() {
            StringBuilder buffer = new StringBuilder(indenter.indent());

            if (retType != void.class) {
                buffer.append("java.lang.Object " + retVarName + " = "); //$NON-NLS-1$ //$NON-NLS-2$
            }

            buffer.append("ref.invoke(this, " + varName + ", "); //$NON-NLS-1$ //$NON-NLS-2$

            if (numParams > 0) {
                buffer.append("new java.lang.Object[] {"); //$NON-NLS-1$

                // Write invocation parameters.
                for (int i = 0; i < numParams; i++) {
                    buffer.append(((i > 0) ? ", " : "") //$NON-NLS-1$ //$NON-NLS-2$
                            + RmicUtil.getObjectParameterString(
                                    parameters[i], paramNames[i]));
                }
                buffer.append('}');
            } else {
                buffer.append("null"); //$NON-NLS-1$
            }

            buffer.append(", " + hash + "L);" + EOLN); //$NON-NLS-1$ //$NON-NLS-2$

            // Write return statement.
            if (retType != void.class) {
                buffer.append(indenter.indent() + "return " //$NON-NLS-1$
                        + RmicUtil.getReturnObjectString(retType, retVarName)
                        + ';' + EOLN);
            }

            return buffer.toString();
        }

        /**
         * Returns the stub implementation catch block for this method
         * (Stub v1.1/v1.2/vCompat).
         *
         * @return  Stub implementation catch block for this method.
         */
        private String getStubImplCatchBlock() {
            StringBuilder buffer = new StringBuilder();

            for (Iterator i = catches.iterator(); i.hasNext(); ) {
                buffer.append(indenter.indent() + "} catch (" //$NON-NLS-1$
                        + ((Class) i.next()).getName() + " e) {" + EOLN //$NON-NLS-1$
                        + indenter.tIncrease() + "throw e;" + EOLN); //$NON-NLS-1$
            }

            buffer.append(indenter.indent()
                    + "} catch (java.lang.Exception e) {" + EOLN //$NON-NLS-1$
                    + indenter.tIncrease()
                    + "throw new java.rmi.UnexpectedException(" //$NON-NLS-1$
                    + "\"Undeclared checked exception\", e);" + EOLN //$NON-NLS-1$
                    + indenter.indent() + '}' + EOLN);

            return buffer.toString();
        }
    }
}
