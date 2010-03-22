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

import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteStub;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.UID;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.server.ExportManager;


/**
 * The RMIObjectOutputStream is a subclass of ObjectOutputStream performing
 * serialization for RMI calls. The following rules are used in addition to
 * normal serialization ones:
 *   - if codebase URL is available for a class, the class will be annotated
 *     with this URL
 *   - remote objects are represented in RMIOutputStream by serialized forms
 *     of their stubs
 *
 * @author  Mikhail A. Markov
 */
public class RMIObjectOutputStream extends ObjectOutputStream {

    // ObjectOutputStream to write annotations.
    private ObjectOutputStream locStream;

    /** True if at least one of written annotations is not null. */
    protected boolean hasAnnotations;

    // True if this stream was created in RemoteCall.getResultStream() method.
    private boolean isResultStream = false;

    // UID to be written to the stream as DGC ack UID.
    private UID uid = new UID();

    /**
     * Constructs a RMIObjectOutputStream that writes to the specified
     * OutputStream.
     *
     * @param out underlying OutputStream
     *
     * @throws IOException if an I/O error occurred during stream initialization
     */
    public RMIObjectOutputStream(OutputStream out) throws IOException {
        this(out, false);
    }

    /**
     * Constructs a RMIObjectOutputStream that writes to the specified
     * OutputStream.
     *
     * @param out underlying OutputStream
     * @param isResultStream true if this stream was created
     *        in RemoteCall.getResultStream() method
     *
     * @throws IOException if an I/O error occurred during stream initialization
     */
    public RMIObjectOutputStream(OutputStream out, boolean isResultStream)
            throws IOException {
        super(out);
        this.isResultStream = isResultStream;
        locStream = this;
        hasAnnotations = false;
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                enableReplaceObject(true);
                return null;
            }});
    }

    /**
     * Replaces exported Remote objects with their stubs.
     *
     * @param obj Object to be replaced if needed
     *
     * @return stub for exported Remote object or unmodified object otherwise
     *
     * @throws IOException
     */
    protected Object replaceObject(Object obj) throws IOException {
        return ((obj instanceof Remote) && !(obj instanceof RemoteStub)
                && ExportManager.isExported((Remote) obj))
                ? RemoteObject.toStub((Remote) obj) : obj;
    }

    /**
     * Annotates specified class with it's codebase URL if available.
     *
     * @param cl class to be annotated
     */
    protected void annotateClass(Class cl) throws IOException {
        String annot = RMIClassLoader.getClassAnnotation(cl);
        hasAnnotations |= (annot != null);
        locStream.writeObject(annot);
    }

    /**
     * Annotates specified proxy class with it's codebase URL if available.
     *
     * @param cl proxy class to be annotated
     */
    protected void annotateProxyClass(Class cl) throws IOException {
        annotateClass(cl);
    }

    /**
     * Flushes the stream.
     *
     * @throws IOException If an I/O error has occurred.
     */
    public void flush() throws IOException {
        super.flush();

        if (locStream != this) {
            locStream.flush();
        }
    }

    /**
     * Sets annotation's stream to the value specified.
     * Subclasses should call this method if they want to write annotations to
     * the stream other then one for objects themselves.
     *
     * @param out stream for annotations
     */
    protected void setLocStream(ObjectOutputStream out) {
        locStream = out;
    }

    /**
     * Returns true if at least one of written annotations is not null and
     * false otherwise.
     *
     * @return true if at least one of written annotations is not null
     */
    protected boolean hasAnnotations() {
        return hasAnnotations;
    }

    /**
     * Write specified obj (possibly primitive) to the stream.
     *
     * @param obj object (possibly primitive) to be written to the stream
     * @param cl type of object to be written to the stream
     *
     * @throws IOException if an I/O error occurred during serialization
     */
    public void writeRMIObject(Object obj, Class cl) throws IOException {
        if (cl.isPrimitive()) {
            if (cl == Boolean.TYPE) {
                writeBoolean(((Boolean) obj).booleanValue());
            } else if (cl == Byte.TYPE) {
                writeByte(((Byte) obj).byteValue());
            } else if (cl == Short.TYPE) {
                writeShort(((Short) obj).shortValue());
            } else if (cl == Integer.TYPE) {
                writeInt(((Integer) obj).intValue());
            } else if (cl == Long.TYPE) {
                writeLong(((Long) obj).longValue());
            } else if (cl == Float.TYPE) {
                writeFloat(((Float) obj).floatValue());
            } else if (cl == Double.TYPE) {
                writeDouble(((Double) obj).doubleValue());
            } else if (cl == Character.TYPE) {
                writeChar(((Character) obj).charValue());
            } else if (cl == Void.TYPE) {
            } else {
                // rmi.7E=Unable to serialize primitive class: {0}
                throw new IOException(Messages.getString("rmi.7E", cl));//$NON-NLS-1$
            }
        } else {
            writeObject(obj);
        }
    }

    /**
     * Returns true if this stream was created in RemoteCall.getResultStream()
     * method and false otherwise.
     *
     * @return true if this stream was created in RemoteCall.getResultStream()
     *         method and false otherwise
     */
    public boolean isResultStream() {
        return isResultStream;
    }

    /**
     * Writes DGC ack UID to this stream.
     *
     * @throws IOException if any I/O error occurred while writing
     */
    public void writeUID() throws IOException {
        uid.write(this);
    }

    /**
     * Returns uid used for DGC ack.
     *
     * @return uid used for DGC ack
     */
    public UID getUID() {
        return uid;
    }
}
