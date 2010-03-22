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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.lang.management;

import java.lang.reflect.Method;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;

/**
 * Abstract implementation of the {@link DynamicMBean} interface that provides
 * behaviour required by a dynamic MBean. This class is subclassed by all of the
 * concrete MXBean types in this package.
 */
public abstract class DynamicMXBeanImpl implements DynamicMBean {

    protected MBeanInfo info;

    /**
     * @param info
     */
    protected void setMBeanInfo(MBeanInfo info) {
        this.info = info;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#getAttributes(java.lang.String[])
     */
    public AttributeList getAttributes(String[] attributes) {
        AttributeList result = new AttributeList();
        for (int i = 0; i < attributes.length; i++) {
            try {
                Object value = getAttribute(attributes[i]);
                result.add(new Attribute(attributes[i], value));
            } catch (Exception e) {
                // It is alright if the returned AttributeList is smaller in
                // size than the length of the input array.
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }// end for
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#setAttributes(javax.management.AttributeList)
     */
    public AttributeList setAttributes(AttributeList attributes) {
        AttributeList result = new AttributeList();

        for (int i = 0; i < attributes.size(); i++) {
            Attribute attrib = (Attribute) attributes.get(i);
            String attribName = null;
            Object attribVal = null;
            try {
                this.setAttribute(attrib);
                attribName = attrib.getName();
                // Note that the below getAttribute call will throw an
                // AttributeNotFoundException if the named attribute is not
                // readable for this bean. This is perfectly alright - the set
                // has worked as requested - it just means that the caller
                // does not get this information returned to them in the
                // result AttributeList.
                attribVal = getAttribute(attribName);
                result.add(new Attribute(attribName, attribVal));
            } catch (Exception e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }// end for
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#getMBeanInfo()
     */
    public MBeanInfo getMBeanInfo() {
        return info;
    }

    /**
     * Simple enumeration of the different kinds of access that may be required
     * of a dynamic MBean attribute.
     */
    enum AttributeAccessType {
        READING, WRITING
    };

    /**
     * Tests to see if this <code>DynamicMXBean</code> has an attribute with
     * the name <code>attributeName</code>. If the test is passed, the
     * {@link MBeanAttributeInfo}representing the attribute is returned.
     * 
     * @param attributeName
     *            the name of the attribute being queried
     * @param access
     *            an {@link AttributeAccessType}indication of whether the
     *            caller is looking for a readable or writable attribute.
     * @return if the named attribute exists and is readable or writable
     *         (depending on what was specified in <code>access</code>, an
     *         instance of <code>MBeanAttributeInfo</code> that describes the
     *         attribute, otherwise <code>null</code>.
     */
    protected MBeanAttributeInfo getPresentAttribute(String attributeName,
            AttributeAccessType access) {
        MBeanAttributeInfo[] attribs = info.getAttributes();
        MBeanAttributeInfo result = null;

        for (int i = 0; i < attribs.length; i++) {
            MBeanAttributeInfo attribInfo = attribs[i];
            if (attribInfo.getName().equals(attributeName)) {
                if (access.equals(AttributeAccessType.READING)) {
                    if (attribInfo.isReadable()) {
                        result = attribInfo;
                        break;
                    }
                } else {
                    if (attribInfo.isWritable()) {
                        result = attribInfo;
                        break;
                    }
                }
            }// end if
        }// end for
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#getAttribute(java.lang.String)
     */
    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException,
            ReflectionException {
        Object result = null;
        Method getterMethod = null;
        MBeanAttributeInfo attribInfo = getPresentAttribute(attribute,
                AttributeAccessType.READING);
        if (attribInfo == null) {
            throw new AttributeNotFoundException("No such attribute : "
                    + attribute);
        }

        try {
            String getterPrefix = attribInfo.isIs() ? "is" : "get";
            getterMethod = this.getClass().getMethod(getterPrefix + attribute,
                    (Class[]) null);
        } catch (Exception e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            throw new ReflectionException(e);
        }

        String realReturnType = getterMethod.getReturnType().getName();
        String openReturnType = attribInfo.getType();
        result = invokeMethod(getterMethod, (Object[]) null);

        try {
            if (!realReturnType.equals(openReturnType)) {
                result = ManagementUtils
                        .convertToOpenType(result, Class
                                .forName(openReturnType), Class
                                .forName(realReturnType));
            }// end if conversion necessary
        } catch (ClassNotFoundException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            throw new MBeanException(e);
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
     */
    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {

        // In Java 5.0 platform MXBeans the following applies for all
        // attribute setter methods :
        // 1. no conversion to open MBean types necessary
        // 2. all setter arguments are single value (i.e. not array or
        // collection types).
        // 3. all return null

        Class<?> argType = null;

        // Validate the attribute
        MBeanAttributeInfo attribInfo = getPresentAttribute(
                attribute.getName(), AttributeAccessType.WRITING);
        if (attribInfo == null) {
            throw new AttributeNotFoundException("No such attribute : "
                    + attribute);
        }

        try {
            // Validate supplied parameter is of the expected type
            argType = ManagementUtils.getClassMaybePrimitive(attribInfo
                    .getType());
        } catch (ClassNotFoundException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            throw new ReflectionException(e);
        }

        if (argType.isPrimitive()) {
            if (!ManagementUtils.isWrapperClass(
                    attribute.getValue().getClass(), argType)) {
                throw new InvalidAttributeValueException(attribInfo.getName()
                        + " is a " + attribInfo.getType() + " attribute");
            }
        } else if (!argType.equals(attribute.getValue().getClass())) {
            throw new InvalidAttributeValueException(attribInfo.getName()
                    + " is a " + attribInfo.getType() + " attribute");
        }

        Method setterMethod = null;
        try {
            setterMethod = this.getClass().getMethod(
                    "set" + attribute.getName(), new Class[] { argType });
        } catch (Exception e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            throw new ReflectionException(e);
        }

        invokeMethod(setterMethod, attribute.getValue());
        try {
            setterMethod.invoke(this, attribute.getValue());
        } catch (Exception e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            Throwable root = e.getCause();
            if (root instanceof RuntimeException) {
                throw (RuntimeException) root;
            } else {
                throw new MBeanException((Exception) root);
            }// end else
        }// end catch
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#invoke(java.lang.String,
     *      java.lang.Object[], java.lang.String[])
     */
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        Object result = null;

        // If null is passed in for the signature argument (if invoking a
        // method with no args for instance) then avoid any NPEs by working
        // with a zero length String array instead.
        String[] localSignature = signature;
        if (localSignature == null) {
            localSignature = new String[0];
        }

        // Validate that we have the named action
        MBeanOperationInfo opInfo = getPresentOperation(actionName,
                localSignature);
        if (opInfo == null) {
            throw new ReflectionException(
                    new NoSuchMethodException(actionName),
                    "No such operation : " + actionName);
        }

        // For Java 5.0 platform MXBeans, no conversion
        // to open MBean types is necessary for any of the arguments.
        // i.e. they are all simple types.
        Method operationMethod = null;
        try {
            Class<?>[] argTypes = new Class[localSignature.length];
            for (int i = 0; i < localSignature.length; i++) {
                argTypes[i] = ManagementUtils
                        .getClassMaybePrimitive(localSignature[i]);
            }// end for
            operationMethod = this.getClass().getMethod(actionName, argTypes);
        } catch (Exception e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            throw new ReflectionException(e);
        }

        String realReturnType = operationMethod.getReturnType().getName();
        String openReturnType = opInfo.getReturnType();
        result = invokeMethod(operationMethod, params);

        try {
            if (!realReturnType.equals(openReturnType)) {
                result = ManagementUtils
                        .convertToOpenType(result, Class
                                .forName(openReturnType), Class
                                .forName(realReturnType));
            }
        } catch (ClassNotFoundException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            throw new MBeanException(e);
        }// end catch

        return result;
    }

    /**
     * Tests to see if this <code>DynamicMXBean</code> has an operation with
     * the name <code>actionName</code>. If the test is passed, the
     * {@link MBeanOperationInfo}representing the operation is returned to the
     * caller.
     * 
     * @param actionName
     *            the name of a possible method on this
     *            <code>DynamicMXBean</code>
     * @param signature
     *            the list of parameter types for the named operation in the
     *            correct order
     * @return if the named operation exists, an instance of
     *         <code>MBeanOperationInfo</code> that describes the operation,
     *         otherwise <code>null</code>.
     */
    protected MBeanOperationInfo getPresentOperation(String actionName,
            String[] signature) {
        MBeanOperationInfo[] operations = info.getOperations();
        MBeanOperationInfo result = null;

        for (int i = 0; i < operations.length; i++) {
            MBeanOperationInfo opInfo = operations[i];
            if (opInfo.getName().equals(actionName)) {
                // Do parameter numbers match ?
                if (signature.length == opInfo.getSignature().length) {
                    // Do parameter types match ?
                    boolean match = true;
                    MBeanParameterInfo[] parameters = opInfo.getSignature();
                    for (int j = 0; j < parameters.length; j++) {
                        MBeanParameterInfo paramInfo = parameters[j];
                        if (!paramInfo.getType().equals(signature[j])) {
                            match = false;
                            break;
                        }
                    }// end for all parameters
                    if (match) {
                        result = opInfo;
                        break;
                    }
                }// end if parameter counts match
            }// end if operation names match
        }// end for all operations

        return result;
    }

    /**
     * @param params
     * @param operationMethod
     * @return the result of the reflective method invocation
     * @throws MBeanException
     */
    private Object invokeMethod(Method operationMethod, Object... params)
            throws MBeanException {
        Object result = null;
        try {
            result = operationMethod.invoke(this, params);
        } catch (Exception e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            Throwable root = e.getCause();
            if (root instanceof RuntimeException) {
                throw (RuntimeException) root;
            } else {
                throw new MBeanException((Exception) root);
            }// end else
        }// end catch
        return result;
    }
}
