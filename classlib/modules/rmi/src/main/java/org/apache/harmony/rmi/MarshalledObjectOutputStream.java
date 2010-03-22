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
package org.apache.harmony.rmi;

import java.io.IOException;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;

import org.apache.harmony.rmi.transport.RMIObjectOutputStream;


/**
 * The MarshalledObjectOutputStream uses the same serialization rules as it's
 * predecessor RMIObjectOutputStream, but it holds annotations for classes
 * separately from the main stream. It is intended to be used by
 * java.rmi.MarshalledObject class.
 *
 * @author  Mikhail A. Markov
 *
 * @see RMIObjectOutputStream
 */
public class MarshalledObjectOutputStream extends RMIObjectOutputStream {

    // ByteArrayOutputStream to write annotations.
    private ByteArrayOutputStream locStream;

    /**
     * Constructs a MarshalledObjectOutputStream that writes to the specified
     * OutputStream.
     *
     * @param out underlying OutputStream
     *
     * @throws IOException if an I/O error occurred during stream initialization
     */
    public MarshalledObjectOutputStream(OutputStream out) throws IOException {
        super(out);
        locStream = new ByteArrayOutputStream();
        setLocStream(new ObjectOutputStream(locStream));
    }

    /**
     * Returns location annotations.
     *
     * @return location annotations.
     */
    public byte[] getLocBytes() {
        return hasAnnotations() ? locStream.toByteArray() : null;
    }
}
