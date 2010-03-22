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
 *
 * TODO: implement char array support
 */
public class Int16Pointer extends VoidPointer {

    private static final int INT16_SIZE_FACTOR = 2;

    //-- TODO: char array support is unimplemented yet
    private char[] array;
    private int offset;


    Int16Pointer(long addr) {
        super(addr);
    }

    Int16Pointer(int size, boolean direct) {
        super(size * INT16_SIZE_FACTOR, direct);
    }

    Int16Pointer(ByteBase base) {
        super(base);
    }

//    Int16Pointer(char[] arr) {
//       this(arr, 0, arr.length);
//    }
//
//    Int16Pointer(char[] arr, int offset, int size) {
//        this.array = arr;
//        this.offset = offset;
//        this.size = size;
//    }

    Int16Pointer(VoidPointer p) {
        super(p);
    }

    /**
     * returns the number of elements in array referenced by this object.
     * If size is unknown returns -1.
     */
    @Override
    public int size() {
        return byteBase.size() / INT16_SIZE_FACTOR;
    }

    /** returns the element at the specified position. */
    public short get(int index) {
        return (byteBase == null) ? (short) array[offset + index] : byteBase.getInt16(index * INT16_SIZE_FACTOR);
    }

    /** sets the element at the specified position. */
    public void set(int index, short value) {
        if(byteBase == null) {
           array[offset + index] = (char) value;
        } else {
            byteBase.setInt16(index * INT16_SIZE_FACTOR, value);
        }
    }

    /** returns the element at the specified position. */
    public char getChar(int index) {
        return (byteBase == null) ? array[offset+index] : byteBase.getChar(index * INT16_SIZE_FACTOR);
    }

    /** sets the element at the specified position. */
    public void setChar(int index, char value) {
        if(byteBase == null) {
           array[offset + index] = value;
        } else {
            byteBase.setChar(index * INT16_SIZE_FACTOR, value);
        }
    }

    /**
     * This method transfers chars into the given destination array.
     *
     * @param dst    - The array into which chars are to be written
     * @param offset - The offset within the array of the first element to be written;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be written to the given array;
     * must be non-negative and no larger than dst.length - offset
     */
    public void getChar(char[] dst, int offset, int length) {
        if (byteBase != null) {
            byteBase.getChar(dst, offset, length);
        } else {
            throw new UnsupportedOperationException("not implemented"); //$NON-NLS-1$
        }
    }

    /**
     * This method transfers chars from the given destination array.
     *
     * @param src    - The array from which chars are to be read
     * @param offset - The offset within the array of the first element to be read;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be read from the given array;
     * must be non-negative and no larger than dst.length - offset
     */
    public void setChar(char[] src, int offset, int length) {
        if (byteBase != null) {
            byteBase.setChar(src, offset, length);
        } else {
            throw new UnsupportedOperationException("not implemented"); //$NON-NLS-1$
        }
    }

    /**
     * This method transfers short values into the given destination array.
     *
     * @param dst    - The array into which values are to be written
     * @param offset - The offset within the array of the first element to be written;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be written to the given array;
     * must be non-negative and no larger than dst.length - offset
     */
    public void get(short[] dst, int offset, int length) {
        if (byteBase != null) {
            byteBase.getInt16(dst, offset, length);
        } else {
            throw new UnsupportedOperationException("not implemented"); //$NON-NLS-1$
        }
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
    public void set(short[] src, int offset, int length) {
        if (byteBase != null) {
            byteBase.setInt16(src, offset, length);
        } else {
            throw new UnsupportedOperationException("not implemented"); //$NON-NLS-1$
        }
    }

    /**
     * returns class that represents the pointer to specified index.
     */
    public Int16Pointer getElementPointer(int index) {
        if (byteBase != null) {
            return new Int16Pointer(byteBase.getBytesBaseElementPointer(index * INT16_SIZE_FACTOR, INT16_SIZE_FACTOR));
        }
        throw new UnsupportedOperationException("not implemented"); //$NON-NLS-1$
    }

    /**
     * @see VoidPointer#isDirect()
     */
    @Override
    public boolean isDirect() {
        return byteBase != null ? byteBase.isDirect() : false;
    }

    /** convert UTF16 bytes to String */
    public String getString(){
        if(byteBase != null) {
            return byteBase.getString();
        }
        throw new UnsupportedOperationException("not implemented"); //$NON-NLS-1$
    }

    /** convert UTF16 bytes to String */
    public String getString(long strlen){
        if(byteBase != null) {
            return byteBase.getString(strlen);
        }
        throw new UnsupportedOperationException("not implemented"); //$NON-NLS-1$
    }

    /**
     * Convert String to 0 terminated UTF16 (Unicode) string.
     * And set it to buffer
     */
    public void setString(String str){
        if (byteBase != null) {
            byteBase.setString(str);
        } else {
            throw new UnsupportedOperationException("not implemented"); //$NON-NLS-1$
        }
    }

    public void fill(short value, int size) {
        if (byteBase != null) {
            int s = size();
            for (int i = 0; i < size && i < s; i++) {
                byteBase.setInt16(i * INT16_SIZE_FACTOR, value);
            }
        } else {
            throw new UnsupportedOperationException("not implemented"); //$NON-NLS-1$
        }
    }
}
