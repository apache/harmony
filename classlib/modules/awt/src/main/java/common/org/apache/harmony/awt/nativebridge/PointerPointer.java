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
package org.apache.harmony.awt.nativebridge;

public class PointerPointer extends VoidPointer {

    private static final int PP_SIZE_FACTOR = ByteBase.POINTER_SIZE;

    PointerPointer(int size, boolean direct) {
        super(size * PP_SIZE_FACTOR, direct);
    }

    PointerPointer(ByteBase base) {
        super(base);
    }

    /**
     * Creates Pointer based on native pointer to pointer
     */
    PointerPointer(long ptrPtr) {
        super(ptrPtr);
    }
    
    /**
     * Creates Pointer to the given pointer.
     * Pointer should be unlocked after usage of the created PointerPointer
     * @param pointer
     * @param direct
     */
    PointerPointer(VoidPointer pointer, boolean direct) {
        super(PP_SIZE_FACTOR, direct);
        long addr = (pointer != null) ? pointer.longLockPointer() : 0;
        setAddress(0, addr);
    }

    /** returns the number of elements in array referenced by this object. If size is unknown returns -1.  */
    @Override
    public int size() {
    int tmp = byteBase.size();
        return  tmp == -1 ? -1 : tmp / PP_SIZE_FACTOR;
    }

    /** returns the element at the specified position. */
    public VoidPointer get(int index) {
        long addr = byteBase.getAddress(index * PP_SIZE_FACTOR);
        return (addr != 0) ? new Int8Pointer(addr) : null;
    }

    /** returns the element at the specified position. */
    public long getAddress(int index) {
        return byteBase.getAddress(index * PP_SIZE_FACTOR);
    }

    /** sets the element at the specified position. */
    public void set(int index, VoidPointer value) {
        byteBase.setAddress(index * PP_SIZE_FACTOR, value.lock());
        value.release();
    }

    /** sets the element at the specified position. */
    public void setAddress(int index, long value) {
        byteBase.setAddress(index * PP_SIZE_FACTOR, value);
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
    public void get(VoidPointer[] dst, int offset, int length) {
        // TODO Implement
        throw new UnsupportedOperationException("Not implemented"); //$NON-NLS-1$
    }

    public void getAddress(long[] dst, int offset, int length) {
        // TODO Implement
        throw new UnsupportedOperationException("Not implemented"); //$NON-NLS-1$
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
    public void set(VoidPointer[] src, int offset, int length) {
        // TODO Implement
        throw new UnsupportedOperationException("Not implemented"); //$NON-NLS-1$
    }

    public void setAddress(long[] src, int offset, int length) {
        // TODO Implement
        throw new UnsupportedOperationException("Not implemented"); //$NON-NLS-1$
    }

    /**
     * returns class that represents the pointer to specified index.
     */
    public PointerPointer getElementPointer(int index) {
        return new PointerPointer(byteBase.getBytesBaseElementPointer(index * PP_SIZE_FACTOR, PP_SIZE_FACTOR));
    }

    public void fill(long value, int size) {
        int thisSize = size();
        for (int i = 0; i < size && i < thisSize; i++) {
            byteBase.setAddress(i * PP_SIZE_FACTOR, value);
        }
    }
}
