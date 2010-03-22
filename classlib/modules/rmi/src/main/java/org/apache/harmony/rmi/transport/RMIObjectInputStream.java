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
 * @author  Mikhail A. Markov
 */
package org.apache.harmony.rmi.transport;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.IOException;
import java.rmi.server.RMIClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.harmony.rmi.common.GetBooleanPropAction;
import org.apache.harmony.rmi.common.RMIProperties;
import org.apache.harmony.rmi.internal.nls.Messages;

import org.apache.harmony.kernel.vm.VM;


/**
 * The RMIObjectInputStream is a subclass of ObjectInputStream performing
 * deserialization for RMI calls.
 *
 * @author  Mikhail A. Markov
 */
public class RMIObjectInputStream extends ObjectInputStream {

    // Annotations for serialized objects.
    private ObjectInputStream locStream;

    // True if this stream was created for handling a RemoteCall
    private boolean isRCallStream = false;

    // True if we need DGC ack call.
    private boolean needDGCAck = false;

    /*
     * If true we'll not load classes from places other then local classpath and
     * location set by java.rmi.server.codebase property.
     */
    private static boolean useCodebaseOnly =
            ((Boolean) AccessController.doPrivileged(new GetBooleanPropAction(
                    RMIProperties.USECODEBASEONLY_PROP))).booleanValue();

    /**
     * Constructs a RMIObjectInputStream that reads from the specified
     * InputStream. This stream will be a non-RemoteCall stream (i.e.
     * isRemoteCallStream() method will return false).
     *
     * @param in underlying InputStream
     *
     * @throws IOException if an I/O error occurred during stream initialization
     */
    public RMIObjectInputStream(InputStream in) throws IOException {
        this (in, false);
    }

    /**
     * Constructs a RMIObjectInputStream that reads from the specified
     * InputStream.
     *
     * @param in underlying InputStream
     * @param isRCallStream true if this stream was created for handling
     *        a RemoteCall and false otherwise
     *
     * @throws IOException if an I/O error occurred during stream initialization
     */
    public RMIObjectInputStream(InputStream in, boolean isRCallStream)
            throws IOException {
        super(in);
        this.isRCallStream = isRCallStream;
        locStream = this;
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                enableResolveObject(true);
                return null;
            }});
    }

    /**
     * Resolves annotated class. To resolves class method calls
     * Class.forName(classname, false, classloader) where classloader is
     * the first non-null class loader up in the execution stack or null
     * if no non-null class loaders were found in the stack.
     *
     * @param streamCl annotated class
     *
     * @throws IOException if an I/O exception occurred
     * @throws ClassNotFoundException if class of a serialized object
     *         could not be found
     */
    protected Class resolveClass(ObjectStreamClass streamCl)
            throws IOException, ClassNotFoundException {
        String annot = (locStream == null) ? null
                : (String) locStream.readObject();

        if (useCodebaseOnly) {
            annot = null;
        }
        Class cl = RMIClassLoader.loadClass(annot, streamCl.getName(),
                VM.getNonBootstrapClassLoader());
        return cl;
    }

    /**
     * Resolves annotated proxy class.
     *
     * @param interf array of interfaces which proxy class should implement
     *
     * @throws IOException if an I/O exception occurred
     * @throws ClassNotFoundException if class of a serialized object
     *         could not be found
     */
    protected Class resolveProxyClass(String[] interf)
            throws IOException, ClassNotFoundException {
        String annot = (locStream == null) ? null
                : (String) locStream.readObject();

        if (useCodebaseOnly) {
            annot = null;
        }
        Class cl = RMIClassLoader.loadProxyClass(annot, interf,
                VM.getNonBootstrapClassLoader());
        return cl;
    }

    /**
     * Sets annotation's stream to the value specified.
     * Subclasses should call this method if they want to read annotations from
     * the stream other then one for objects themselves.
     *
     * @param in stream for annotations
     */
    protected void setLocStream(ObjectInputStream in) {
        locStream = in;
    }

    /**
     * Reads object (possibly primitive value) from the stream.
     *
     * @param cl expected class to be read from the stream
     * @param loader ClassLoader for classes resolving (ClassLoader which
     *        will be specified in the calls to RMIClassLoader.)
     *
     * @return object read from the stream
     *
     * @throws IOException if an I/O error occurred during deserialization
     * @throws ClassNotFoundException if class of a serialized object
     *         could not be found
     */
    public synchronized Object readRMIObject(Class cl)
            throws IOException, ClassNotFoundException {
        if (cl.isPrimitive()) {
            if (cl == Boolean.TYPE) {
                return new Boolean(readBoolean());
            } else if (cl == Byte.TYPE) {
                return new Byte(readByte());
            } else if (cl == Short.TYPE) {
                return new Short(readShort());
            } else if (cl == Integer.TYPE) {
                return new Integer(readInt());
            } else if (cl == Long.TYPE) {
                return new Long(readLong());
            } else if (cl == Float.TYPE) {
                return new Float(readFloat());
            } else if (cl == Double.TYPE) {
                return new Double(readDouble());
            } else if (cl == Character.TYPE) {
                return new Character(readChar());
            } else if (cl == Void.TYPE) {
                return null;
            } else {
                // rmi.7F=Unknown primitive class: {0}
                throw new IOException(Messages.getString("rmi.7F", cl)); //$NON-NLS-1$
            }
        } else {
            return readObject();
        }
    }

    /**
     * Returns true if this stream was created for handling a RemoteCall and
     * false otherwise.
     *
     * @return true if this stream was created for handling a RemoteCall and
     *         false otherwise
     */
    public boolean isRemoteCallStream() {
        return isRCallStream;
    }

    /**
     * Sets the flag of DGC ack call to the given value.
     *
     * @param need true if we need DGC ack call and false otherwise
     */
    public void needDGCAck(boolean need) {
        needDGCAck = need;
    }

    /**
     * Returns the value of DGC ack call field.
     *
     * @return the value of DGC ack call field
     */
    public boolean isDGCAckNeeded() {
        return needDGCAck;
    }
}
