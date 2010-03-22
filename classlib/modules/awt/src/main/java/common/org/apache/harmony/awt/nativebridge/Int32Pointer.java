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
 * @author Sergey V. Kuksenko
 */
package org.apache.harmony.awt.nativebridge;

public class Int32Pointer extends VoidPointer {

    private static final int INT32_SIZE_FACTOR = 4;

    Int32Pointer(int size, boolean direct) {
        super(size * INT32_SIZE_FACTOR, direct);
    }

    Int32Pointer(VoidPointer p) {
        super(p);
    }

    Int32Pointer(long addr) {
        super(addr);
    }

    Int32Pointer(ByteBase base) {
        super(base);
    }

    /** returns the number of elements in array referenced by this object. If size is unknown returns -1.  */
    @Override
    public int size() {
         return byteBase.size() / INT32_SIZE_FACTOR;
    }

    /** returns the element at the specified position. */
    public int get(int index) {
        return byteBase.getInt32(index * INT32_SIZE_FACTOR);
    }

    /** sets the element at the specified position. */
    public void set(int index, int value) {
        byteBase.setInt32(index * INT32_SIZE_FACTOR, value);
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
    public void get(int[] dst, int offset, int length) {
        byteBase.getInt32(dst, offset, length);
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
    public void set(int[] src, int offset, int length) {
        byteBase.setInt32(src, offset, length);
    }


    /**
     * returns class that represents the pointer to specified index.
     */
    public Int32Pointer getElementPointer(int index) {
        return new Int32Pointer(byteBase.getBytesBaseElementPointer(index * INT32_SIZE_FACTOR, INT32_SIZE_FACTOR));
    }

    /**
     * Fills the memory with the gven value
     * @param value
     * @param size
     */
    public void fill(int value, int size) {
        int thisSize = size();
        for (int i = 0; i < size && i < thisSize; i++) {
            byteBase.setInt32(i * INT32_SIZE_FACTOR, value);
        }
    }
}
