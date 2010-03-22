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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.lang.reflect.Method;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Generates class and method hash codes necessary for RMI.
 *
 * @author  Vasily Zakharov
 */
public final class RMIHash {

    /**
     * This class cannot be instantiated.
     */
    private RMIHash() {}

    /**
     * Calculates RMI method hash
     * as specified in Chapter 8.3 of RMI Specification.
     *
     * @param   method
     *          Method to calculate RMI hash for.
     *
     * @return  RMI hash for the specified method.
     *
     * @throws  RMIHashException
     *          If some error occurs.
     */
    public static long getMethodHash(Method method) throws RMIHashException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(buffer);

            dataStream.writeUTF(RMIUtil.getExtendedMethodDescriptor(method));
            dataStream.close();

            return getHash(buffer.toByteArray());
        } catch (IOException e) {
            // rmi.42=Failed to calculate hash for method {0}
            throw new RMIHashException(Messages.getString("rmi.42", //$NON-NLS-1$
                    method), e);
        } catch (NoSuchAlgorithmException e) {
            // rmi.42=Failed to calculate hash for method {0}
            throw new RMIHashException(Messages.getString("rmi.42", //$NON-NLS-1$
                    method), e);
        }
    }

    /**
     * Calculates RMI interface hash
     * as specified in Chapter 8.3 of RMI Specification.
     *
     * @param   cls
     *          Class to calculate RMI hash for.
     *
     * @return  RMI hash for the specified class.
     *
     * @throws  RMIHashException
     *          If some error occurs.
     */
    public static long getInterfaceHash(Class cls) throws RMIHashException {
        try {
            return getInterfaceHash(getSortedMethodMap(cls.getMethods()));
        } catch (RMIHashException e) {
            // rmi.43=Failed to calculate interface hash for class {0}
            throw new RMIHashException(Messages.getString("rmi.43", //$NON-NLS-1$
                    cls), e.getCause());
        }
    }

    /**
     * Calculates RMI interface hash
     * as specified in Chapter 8.3 of RMI Specification.
     *
     * @param   methodMap
     *          Map containing methods of the class to calculate hash for.
     *
     * @return  RMI hash for the specified interface.
     *
     * @throws  RMIHashException
     *          If some error occurs.
     */
    public static long getInterfaceHash(SortedMap methodMap)
            throws RMIHashException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(buffer);

            dataStream.writeInt(1);

            for (Iterator i = methodMap.values().iterator(); i.hasNext(); ) {
                Method method = (Method) i.next();

                dataStream.writeUTF(method.getName());
                dataStream.writeUTF(RMIUtil.getMethodDescriptor(method));

                Class[] exceptions = method.getExceptionTypes();
                TreeSet exceptionSet = new TreeSet();

                for (int j = 0; j < exceptions.length; j++) {
                    exceptionSet.add(exceptions[j].getName());
                }

                for (Iterator k = exceptionSet.iterator(); k.hasNext(); ) {
                    dataStream.writeUTF((String) k.next());
                }
            }

            dataStream.close();

            return getHash(buffer.toByteArray());
        } catch (IOException e) {
            // rmi.44=Failed to calculate interface hash for specified set of methods
            throw new RMIHashException(Messages.getString("rmi.44"), e); //$NON-NLS-1$
        } catch (NoSuchAlgorithmException e) {
            // rmi.44=Failed to calculate interface hash for specified set of methods
            throw new RMIHashException(Messages.getString("rmi.44"), e); //$NON-NLS-1$
        }
    }

    /**
     * Moves methods from the specified array to the newly created map
     * sorting them properly for RMI interface hash calculation.
     *
     * @param   methods
     *          Methods to sort.
     *
     * @return  The created method map.
     */
    public static SortedMap getSortedMethodMap(Method[] methods) {
        return getSortedMethodMap(null, methods);
    }

    /**
     * Adds methods from the specified array to the specified map
     * sorting them properly for RMI interface hash calculation.
     *
     * @param   methodMap
     *          Map to store sorted methods to.
     *
     * @param   methods
     *          Methods to sort.
     *
     * @return  The updated method map.
     */
    public static SortedMap getSortedMethodMap(
            SortedMap methodMap, Method[] methods) {
        if (methodMap == null) {
            methodMap = new TreeMap();
        }

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            methodMap.put(RMIUtil.getExtendedMethodDescriptor(method), method);
        }

        return methodMap;
    }

    /**
     * Calculates RMI hash value for the specified byte array,
     * as specified in Chapter 8.3 of RMI Specification.
     *
     * @param   buffer
     *          Byte array to calculate RMI hash for.
     *
     * @return  RMI hash value for the specified byte array.
     *
     * @throws  NoSuchAlgorithmException
     *          Should never occur.
     */
    private static long getHash(byte[] buffer) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(buffer); //$NON-NLS-1$

        long hash = 0;

        int length = digest.length;

        if (length > 8) {
            length = 8;
        }

        for (int i = 0; i < length; i++) {
            hash += ((long) (digest[i] & 0xff)) << (i * 8);
        }

        return hash;
    }
}
