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
/**
 * @author Michael Danilov
 */
package org.apache.harmony.awt;

import org.apache.harmony.awt.internal.nls.Messages;

/**
 * Analog of StringBuffer but works with bytes.
 */
public class ByteVector {

    private static final int DEF_CAPACITY = 16;

    private byte[] value;
    private int count;

    /**
     * Constructor of new byte buffer.
     */
    public ByteVector() {
        value = new byte[DEF_CAPACITY];
        count = 0;
    }

    /**
     * Constructor of new byte buffer.
     *
     * @param capacity - initial capacity of new buffer.
     */
    public ByteVector(int capacity) {
        value = new byte[capacity];
        count = 0;
    }

    /**
     * Appends byte to the end of this buffer.
     *
     * @param b - byte to be appended.
     */
    public ByteVector append(byte b) {
        ensureCapacity(count + 1);
        value[count++] = b;

        return this;
    }

    /**
     * Appends char (2 bytes) to the end of this buffer.
     *
     * @param c - char to be appended.
     */
    public ByteVector append(char c) {
        ensureCapacity(count + 2);
        value[count++] = (byte) c;
        value[count++] = (byte) (c >> 8);

        return this;
    }

    /**
     * Appends bytes array to the end of this buffer.
     *
     * @param bytes - bytes to be appended.
     */
    public ByteVector append(byte[] bytes) {
        int length = bytes.length;

        ensureCapacity(count + length);
        System.arraycopy(bytes, 0, value, count, length);
        count += length;

        return this;
    }

    /**
     * Appends bytes from param's buffer to the end of this buffer.
     *
     * @param vector - buffer with bytes to be appended.
     */
    public ByteVector append(ByteVector vector) {
        append(vector.getAll());

        return this;
    }

    /**
     * Gets amount of bytes in buffer.
     *
     * @return number of bytes in buffer.
     */
    public int length() {
        return count;
    }

    /**
     * Gets the byte value in this buffer at the specified index.
     *
     * @param index - index of byte to be returned.
     * @return byte value at specified position.
     */
    public byte get(int index) {
        if ((index < 0) || (index >= count)) {
            // awt.33=index is out of range
            throw new RuntimeException(Messages.getString("awt.33")); //$NON-NLS-1$
        }

        return value[index];
    }

    /**
     * Gets all the byte from this buffer.
     *
     * @return array of bytes in this buffer.
     */
    public byte[] getAll() {
        byte[] res;

        res = new byte[count];
        System.arraycopy(value, 0, res, 0, count);

        return res;
    }

    private void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity <= value.length) {
            return;
        }

        int newLength = value.length * 2 + 2;
        byte[] newArray;

        if (minimumCapacity > newLength) {
            newLength = minimumCapacity;
        }
        newArray = new byte[newLength];
        System.arraycopy(value, 0, newArray, 0, count);
        value = newArray;
    }

}
