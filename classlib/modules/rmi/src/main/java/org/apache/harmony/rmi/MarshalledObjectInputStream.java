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
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import org.apache.harmony.rmi.transport.RMIObjectInputStream;


/**
 * The MarshalledObjectInputStream uses the same deserialization rules as it's
 * predecessor RMIObjectInputStream. It is intended to be used by
 * java.rmi.MarshalledObject class.
 *
 * @author  Mikhail A. Markov
 *
 * @see RMIObjectInputStream
 */
public class MarshalledObjectInputStream extends RMIObjectInputStream {

    /**
     * Constructs a MarshalledObjectOutputStream from 2 arrays: array with
     * serialized objects, and array with annotations for the objects.
     *
     * @param objBytes serialized objects array
     * @param locBytes annotations for serialized objects
     *
     * @throws IOException if an I/O error occurred during streams initialization
     */
    public MarshalledObjectInputStream(byte[] objBytes, byte[] locBytes)
            throws IOException {
        super(new ByteArrayInputStream(objBytes));
        setLocStream((locBytes == null) ? null : new ObjectInputStream(
                    new ByteArrayInputStream(locBytes)));
    }
}
