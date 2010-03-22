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

/**
 * This class represents pointer to memory for working with native functions.
 * It is an analogue of <it>char*</it> C type. Every pointed object can be direct
 * native object or Java object. In both cases NativeBridge provides correct
 * passing addresses to native functions, native address is passed as is, Java
 * object will be implicitly locked and address of lock will be passed to native.
 * NULL pointer can't be wrapped by this object, NULL is represents by Java
 * null value.
 */
public class Int8Pointer extends VoidPointer {

    Int8Pointer(int size, boolean direct) {
        super(size, direct);
    }

    Int8Pointer(byte[] arr, int offset, int size) {
        super(arr, offset, size);
    }

    Int8Pointer(long addr){
        super(addr);
    }

    Int8Pointer(VoidPointer p) {
        super(p);
    }

    Int8Pointer(ByteBase base) {
        super(base);
    }

    @Override
    public int size() {
        return byteBase.size();
    }

    /**
     * Returns class that represents the pointer to specified index.
     * @param index - index from which pointer is returned
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Int8Pointer getElementPointer(int index) {
        return new Int8Pointer(byteBase.getBytesBaseElementPointer(index, 1));
    }

    /**
     * Returns the element at the specified position.
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public byte get(int index) {
        return byteBase.get(index);
    }

    /**
     * This method transfers bytes into the given destination array.
     *
     * @param dst    - The array into which bytes are to be written
     * @param from - Initial offset in the destination array.
     * Must be non-negative and no larger than dst.length
     * @param length - The maximum number of bytes to be written to the given array;
     * must be non-negative and no larger than dst.length - from
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > dst.length
     * @throws IndexOutOfBoundsException if length < 0 || length > dst.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void get(byte[] dst, int from, int length) {
        byteBase.get(dst, from, length);
    }

    /**
     * This method transfers bytes from the given destination array.
     *
     * @param src    - The array from which bytes are to be read
     * @param offset - The offset within the array of the first byte to be read;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of bytes to be read from the given array;
     * must be non-negative and no larger than array.length - offset
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void set(byte[] src, int offset, int length) {
        byteBase.set(src, offset, length);
    }

    /**
     * Sets the element at the specified position.
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void set(int index, byte value) {
        byteBase.set(index, value);
    }

    public void fill(byte value, int size) {
        byteBase.fill(value, size);
    }

    /**
     * Creates String from 0 terminated modified UTF8 string.
     */
    public String getStringUTF() {
        return byteBase.getStringUTF();
    }

    /**
     * Creates String from 0 terminated modified UTF8 native string of max
     * strlen length in bytes.
     */
    public String getStringUTF(long strlen) {
        return byteBase.getStringUTF(strlen);
    }

    /**
     * Convert String to 0 terminated UTF8 (Unicode) string.
     * And set it to buffer
     */
    public void setStringUTF(String str){
        byteBase.setStringUTF(str);
    }
}
