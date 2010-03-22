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

package java.rmi;

import java.io.IOException;
import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.apache.harmony.rmi.MarshalledObjectInputStream;
import org.apache.harmony.rmi.MarshalledObjectOutputStream;

public final class MarshalledObject<T> implements Serializable {
    private static final long serialVersionUID = 8988374069173025854L;

    private final byte[] objBytes;

    private final byte[] locBytes;

    private final int hash;

    public MarshalledObject(T obj) throws IOException {
        ByteArrayOutputStream objStream = new ByteArrayOutputStream();
        MarshalledObjectOutputStream moStream = new MarshalledObjectOutputStream(objStream);
        moStream.writeObject(obj);
        moStream.flush();
        objBytes = objStream.toByteArray();
        locBytes = moStream.getLocBytes();

        // calculate hash code
        int hash = 0;

        for (int i = 0; i < objBytes.length; ++i) {
            hash = hash * 31 + objBytes[i];
        }
        this.hash = hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MarshalledObject)) {
            return false;
        }
        MarshalledObject anotherObj = (MarshalledObject) obj;
        return (hash == anotherObj.hash) || (Arrays.equals(objBytes, anotherObj.objBytes));
    }

    public T get() throws IOException, ClassNotFoundException {
        if (objBytes == null) {
            return null;
        }
        MarshalledObjectInputStream moin = new MarshalledObjectInputStream(objBytes, locBytes);
        return (T) moin.readObject();
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
