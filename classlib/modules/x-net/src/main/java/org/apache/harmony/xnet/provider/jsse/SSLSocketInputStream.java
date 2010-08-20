/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.xnet.provider.jsse;

import org.apache.harmony.xnet.provider.jsse.SSLSocketImpl;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class provides input data stream functionality
 * for SSLSocket. It accumulates the application data
 * received by SSL protocol.
 */
public final class SSLSocketInputStream
        extends InputStream {

    // the ssl socket owning the stream
    private final SSLSocketImpl owner;
    
    /**
     * Creates the application data input stream for specified socket.
     * @param   owner the socket which will provide this input stream
     * to client applications.
     */
    protected SSLSocketInputStream(SSLSocketImpl owner) {
        this.owner = owner;
    }

    // ------------------ InputStream implementation -------------------

    /**
     * Returns the number of bytes available for reading without blocking.
     * @return the number of available bytes.
     * @throws  IOException
     */
    @Override
    public int available() throws IOException {
        return owner.available();
    }

    /**
     * Closes the stream
     * @throws  IOException
     */
    @Override
    public void close() throws IOException {
    }

    /**
     * Reads one byte. If there is no data in the underlying buffer,
     * this operation can block until the data will be
     * available.
     * @return read value.
     * @throws  IOException
     */
    @Override
    public int read() throws IOException {
        byte[] data = new byte[1];
        int ret = read(data, 0, 1);
        if (ret == 1) {
            return (data[0] & 0xFF);
        } else if (ret == 0) {
            return -1;
        } else {
            throw new IOException();
        }
    }

    /**
     * Method acts as described in spec for superclass.
     * @see java.io.InputStream#read(byte[])
     */
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Method acts as described in spec for superclass.
     * @see java.io.InputStream#read(byte[],int,int)
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return owner.readAppData(b, off, len);
    }

    /**
     * Method acts as described in spec for superclass.
     * @see java.io.InputStream#skip(long)
     */
    @Override
    public long skip(long n) throws IOException {
        long i = 0;
        int av = available();
        if (av < n) {
            n = av;
        }
        while ((i < n) && (read() != -1)) {
            i++;
        }
        return i;
    }
}

