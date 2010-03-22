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
 * @author Rustem Rafikov
 */
package org.apache.harmony.awt.nativebridge;

public class FloatPointer extends VoidPointer {

    private static final int FLOAT_SIZE_FACTOR = 4;

    FloatPointer(int size, boolean direct) {
        super(size * FLOAT_SIZE_FACTOR, direct);
    }

    FloatPointer(VoidPointer p) {
        super(p);
    }

    FloatPointer(long addr) {
        super(addr);
    }

    FloatPointer(ByteBase base) {
        super(base);
    }

    /**
     * Returns the element at the specified position.
     * @param index
     */
    public float get(int index) {
        return byteBase.getFloat(index * FLOAT_SIZE_FACTOR);
    }

    /**
     * Sets the element at the specified position.
     * @param index
     * @param value
     */
    public void set(int index, float value) {
        byteBase.setFloat(index * FLOAT_SIZE_FACTOR, value);
    }


    /**
     * This method transfers int values into the given destination array.
     *
     * @param dst    - The array into which values are to be written
     * @param offset - The offset within the array of the first element to be written;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be written to the given array;
     * must be non-negative and no larger than dst.length - offset
     */
    public void get(float[] dst, int offset, int length) {
        byteBase.getFloat(dst, offset, length);
    }

    /**
     * This method transfers short values from the given destination array.
     *
     * @param src    - The array from which values are to be read
     * @param offset - The offset within the array of the first element to be read;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be read from the given array;
     * must be non-negative and no larger than dst.length - offset
     */
    public void set(float[] src, int offset, int length) {
        byteBase.setFloat(src, offset, length);
    }

    /**
     * returns class that represents the pointer to specified index.
     */
    public FloatPointer getElementPointer(int index) {
        return new FloatPointer(byteBase.getBytesBaseElementPointer(index * FLOAT_SIZE_FACTOR, FLOAT_SIZE_FACTOR));
    }

    @Override
    public int size() {
        return byteBase.size() / FLOAT_SIZE_FACTOR;
    }

    public void fill(float value, int size) {
        int thisSize = size();
        for (int i = 0; i < size && i < thisSize; i++) {
            byteBase.setFloat(i * FLOAT_SIZE_FACTOR, value);
        }
    }

}
