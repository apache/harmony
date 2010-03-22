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

import java.util.Arrays;

import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.misc.accessors.AccessorFactory;
import org.apache.harmony.misc.accessors.ArrayAccessor;
import org.apache.harmony.misc.accessors.LockedArray;
import org.apache.harmony.misc.accessors.MemoryAccessor;
import org.apache.harmony.misc.accessors.StringAccessor;

public class ByteBase {
    static MemoryAccessor macc = AccessorFactory.getMemoryAccessor();
    static ArrayAccessor arac = AccessorFactory.getArrayAccessor();
    static StringAccessor stac = AccessorFactory.getStringAccessor();
    static NativeBridge nb = NativeBridge.getInstance();

    static final int NATIVE_BYTE_ORDER = macc.getNativeByteOrder();
    static final int JAVA_BYTE_ORDER = MemoryAccessor.BIG_ENDIAN;

    /**
     *  size of C pointer type, in bytes.
     */
    public static final int POINTER_SIZE = macc.getPointerSize();

    /**
     *  size of C long type, in bytes.
     */
    public static final int CLONG_SIZE = macc.getPointerSize();


    private long addr = 0L;  // (void*) pointer to native memory
    private LockedArray lock = null;
    private byte[] array = null; // wrapped byte array
    private int offset = 0;
    private int size = -1; // -1 - unknown size


    ByteBase(int size, boolean direct) {
       if(direct) {
           this.addr = macc.malloc(size);
       } else {
           this.array = new byte[size];//(byte[]) arac.createArray(byte.class, size);
       }
       this.size = size;
    }

    ByteBase(byte[] arr, int offset, int size) {
       this.array = arr;
       this.offset = offset;
       this.size = size;
    }

    ByteBase(long addr){
        this.addr = addr;
        size = -1;
    }

    /**
     * @see VoidPointer#free()
     */
    public void free() {
        if(addr != 0L) {
            macc.free(addr);
            addr = 0L;
        } else {
            unlockNoCopy();
        }
    }

    /**
     * @see VoidPointer#longLockPointer()
     */
    public long longLockPointer() {
        if(addr == 0L) {
            if (lock == null) {
                lock = arac.lockArrayLong(array);
            }
            return lock.getAddress() + offset;
        }
        return addr;
    }

    /**
     * @see VoidPointer#shortLockPointer()
     */
    long shortLockPointer() {
        if(addr == 0L) {
            if (lock == null) {
                lock = arac.lockArrayShort(array);
            }
            return lock.getAddress() + offset;
        }
        return addr;
    }

    void unlock() {
        if(addr == 0L && lock != null) {
            lock.release();
            lock = null;
        }
    }

    void unlockNoCopy() {
        if(addr == 0L && lock != null) {
            lock.releaseNoCopy();
            lock = null;
        }
    }

    /**
     * @see VoidPointer#isDirect()
     */
    public boolean isDirect() {
        return addr != 0L;
    }

    /**
     * returns the number of elements in memeory referenced by this pointer.
     * Returns -1 if size is unknown.
     */
    public int size() {
        return size;
    }

    /**
     * Returns the element at the specified position.
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public byte get(int index) {
        if (addr == 0) { // throws ArrayIndexOutofBounds
            return array[offset + index];
        }
        checkIndex(index, 1);
        return macc.getByte(addr + index);
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
        checkArraysBounds(from, dst.length, length, 1);
        if (addr == 0) {
            System.arraycopy(array, this.offset, dst, from, length);
        } else {
            macc.getByte(addr, dst, from, length);
        }
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
        checkArraysBounds(offset, src.length, length, 1);
        if (addr == 0) {
            System.arraycopy(src, offset, array, this.offset, length);
        } else {
            macc.setByte(addr, src, offset, length);
        }
    }

    /**
     * Copies <code>length</code> bytes from src byteBase to this byteBase.
     * Offset in destination base is dstOffset. Starting offset in src base
     * is always 0;
     *
     * @param src source byte base
     * @param dstOffset destination
     * @param length
     */
    void copy(ByteBase src, int dstOffset, int length) {
        if (src.size != -1 && src.size < length) {
            // awt.30=wrong number of elements to copy: {0}, size: {1}
            throw new IndexOutOfBoundsException(Messages.getString("awt.30", length, src.size)); //$NON-NLS-1$
        }
        if (size != -1 && size - dstOffset < length) {
            // awt.31=no room to copy: {0}, size: {1}
            throw new IndexOutOfBoundsException(Messages.getString("awt.31", length, src.size)); //$NON-NLS-1$
        }

        byte[] tmp = new byte[length];
        src.get(tmp, 0, length);
        if (addr == 0) {
            System.arraycopy(tmp, 0, array, this.offset + dstOffset, length);
        } else {
            macc.setByte(addr + dstOffset, tmp, 0, length);
        }
    }

    /**
     * Sets the element at the specified position.
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void set(int index, byte value) {
        if(addr == 0) {
           array[offset + index] = value;
        } else {
           checkIndex(index, 1);
           macc.setByte(addr + index, value);
        }
    }

    /**
     * Reads two bytes at the given index, composing them into a char value
     * according.
     *
     * @param index - The index from which the bytes will be read
     * @return The char value at the given index
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public char getChar(int index) {
        if(addr == 0){
            return getCharFromArray(index);
        }
        checkIndex(index, 2);
        return macc.getChar(addr + index);
    }

    /**
     * Writes two bytes to the given index, decomposing them from a char value
     * according
     * @param index - The index from which the bytes will be read
     * @param value - char value
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void setChar(int index, char value) {
        if(addr==0) {
            setCharInArray(index, value);
        } else {
            checkIndex(index, 2);
            macc.setChar(addr + index, value);
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
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void getChar(char[] dst, int offset, int length) {
        checkArraysBounds(offset, dst.length, length, 2);
        if (addr == 0) {
            for (int i = 0, j = offset; i < length; i++, j++) {
                dst[j] = getCharFromArray(i * 2);
            }
        } else {
            macc.getChar(addr, dst, offset, length);
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
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void setChar(char[] src, int offset, int length) {
        checkArraysBounds(offset, src.length, length, 2);
        if (addr == 0) {
            for (int i = 0, j = offset; i < length; i++, j++) {
                setCharInArray(i * 2, src[j]);
            }
        } else {
            macc.setChar(addr, src, offset, length);
        }
    }

    /**
     * Reads two bytes at the given index, composing them into a short value
     * according
     * @param index - The index to which the bytes will be write
     * @return The short value at the given index
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public short getInt16(int index) {
        if(addr == 0){
            return getInt16FromArray(index);
        }
        checkIndex(index, 2);
        return macc.getShort(addr + index);
    }

    /**
     * Writes two bytes to the given index, decomposing them from a short value
     * according
     * @param index - The index from which the bytes will be write
     * @param value - short value
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void setInt16(int index, short value) {
        if(addr == 0) {
            setInt16InArray(index, value);
        } else {
            checkIndex(index, 2);
            macc.setShort(addr + index, value);
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
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void getInt16(short[] dst, int offset, int length) {
        checkArraysBounds(offset, dst.length, length, 2);
        if (addr == 0) {
            for (int i = 0, j = offset; i < length; i++, j++) {
                dst[j] = getInt16FromArray(i * 2);
            }
        } else {
            macc.getShort(addr, dst, offset, length);
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
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void setInt16(short[] src, int offset, int length) {
        checkArraysBounds(offset, src.length, length, 2);
        if (addr == 0) {
            for (int i = 0, j = offset; i < length; i++, j++) {
                setInt16InArray(i * 2, src[j]);
            }
        } else {
            macc.setShort(addr, src, offset, length);
        }
    }


    /**
     * Reads four bytes at the given index, composing them into a float value
     * according
     * @param index - The index from which the bytes will be read
     * @return The float value at the given index
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public float getFloat(int index) {
        if(addr == 0) {
            return Float.intBitsToFloat(getInt32FromArray(index));
        }
        checkIndex(index, 4);
        return macc.getFloat(addr + index);
    }


    /**
     * Writes four bytes to the given index, decomposing them from a float value
     * according
     * @param index - The index from which the bytes will be write
     * @param value - float value
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void setFloat(int index, float value) {
        if (addr == 0) {
            setInt32(index, Float.floatToIntBits(value));
        } else {
            checkIndex(index, 4);
            macc.setFloat(addr + index, value);
        }
    }

    /**
     * Reads four bytes at the given index, composing them into a double value
     * according
     * @param index - The index from which the bytes will be read
     * @return The double value at the given index
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public double getDouble(int index) {
        if(addr == 0) {
            return Double.longBitsToDouble(getInt64FromArray(index));
        }
        checkIndex(index, 8);
        return macc.getDouble(addr + index);
    }

    /**
     * Writes four bytes to the given index, decomposing them from a double value
     * according
     * @param index - The index from which the bytes will be write
     * @param value - double value
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void setDouble(int index, double value) {
        if (addr == 0) {
            setInt64InArray(index, Double.doubleToLongBits(value));
        } else {
            checkIndex(index, 8);
            macc.setDouble(addr + index, value);
        }
    }


    /**
     * This method transfers float values into the given destination array.
     *
     * @param dst    - The array into which values are to be written
     * @param offset - The offset within the array of the first element to be written;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be written to the given array;
     * must be non-negative and no larger than dst.length - offset
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void getFloat(float[] dst, int offset, int length) {
        checkArraysBounds(offset, dst.length, length, 4);
        if (addr == 0) {
            for (int i = 0, j = offset; i < length; i++, j++) {
                dst[j] = Float.intBitsToFloat(getInt32FromArray(i * 4));
            }
        } else {
            macc.getFloat(addr, dst, offset, length);
        }
    }

    /**
     * This method transfers float values from the given destination array.
     *
     * @param src    - The array from which values are to be read
     * @param offset - The offset within the array of the first element to be read;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be read from the given array;
     * must be non-negative and no larger than dst.length - offset
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void setFloat(float[] src, int offset, int length) {
        checkArraysBounds(offset, src.length, length, 4);
        if (addr == 0) {
            for (int i = 0, j = offset; i < length; i++, j++) {
                setInt32InArray(i * 4, Float.floatToIntBits(src[j]));
            }
        } else {
            macc.setFloat(addr, src, offset, length);
        }
    }

    /**
     * This method transfers double values into the given destination array.
     *
     * @param dst    - The array into which values are to be written
     * @param offset - The offset within the array of the first element to be written;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be written to the given array;
     * must be non-negative and no larger than dst.length - offset
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void getDouble(double[] dst, int offset, int length) {
        checkArraysBounds(offset, dst.length, length, 8);
        if (addr == 0) {
            for (int i = 0, j = offset; i < length; i++, j++) {
                dst[j] = Double.longBitsToDouble(getInt64FromArray(i * 8));
            }
        } else {
            macc.getDouble(addr, dst, offset, length);
        }
    }

    /**
     * This method transfers double values from the given destination array.
     *
     * @param src    - The array from which values are to be read
     * @param offset - The offset within the array of the first element to be read;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be read from the given array;
     * must be non-negative and no larger than dst.length - offset
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void setDouble(double[] src, int offset, int length) {
        checkArraysBounds(offset, src.length, length, 8);
        if (addr == 0) {
            for (int i = 0, j = offset; i < length; i++, j++) {
                setInt64InArray(i * 8, Double.doubleToLongBits(src[j]));
            }
        } else {
            macc.setDouble(addr, src, offset, length);
        }
    }

    /**
     * Reads four bytes at the given index, composing them into a int value
     * according
     * @param index - The index from which the bytes will be read
     * @return The int value at the given index
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public int getInt32(int index) {
        if(addr == 0) {
            return getInt32FromArray(index);
        }
        checkIndex(index, 4);
        return macc.getInt(addr + index);
    }

    /**
     * Writes four bytes to the given index, decomposing them from a int value
     * according
     * @param index - The index from which the bytes will be write
     * @param value - int value
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void setInt32(int index, int value) {
        if (addr == 0) {
            setInt32InArray(index, value);
        } else {
            checkIndex(index, 4);
            macc.setInt(addr + index, value);
        }
    }

    /**
     * This method transfers int values into the given destination array.
     *
     * @param dst    - The array into which values are to be written
     * @param offset - The offset within the array of the first element to be written;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be written to the given array;
     * must be non-negative and no larger than dst.length - offset
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void getInt32(int[] dst, int offset, int length) {
        checkArraysBounds(offset, dst.length, length, 4);
        if (addr == 0) {
            for (int i = 0, j = offset; i < length; i++, j++) {
                dst[j] = getInt32FromArray(i * 4);
            }
        } else {
            macc.getInt(addr, dst, offset, length);
        }
    }

    /**
     * This method transfers int values from the given destination array.
     *
     * @param src    - The array from which values are to be read
     * @param offset - The offset within the array of the first element to be read;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be read from the given array;
     * must be non-negative and no larger than dst.length - offset
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void setInt32(int[] src, int offset, int length) {
        checkArraysBounds(offset, src.length, length, 4);
        if (addr == 0) {
            for (int i = 0, j = offset; i < length; i++, j++) {
                setInt32InArray(i * 4, src[j]);
            }
        } else {
            macc.setInt(addr, src, offset, length);
        }
    }

    /**
     * Reads 8 bytes at the given index, composing them into a long value
     * according
     * @param index - The index from which the bytes will be read
     * @return The long value at the given index
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public long getInt64(int index) {
        if(addr == 0) {
            return getInt64FromArray(index);
        }
        checkIndex(index, 8);
        return macc.getLong(addr + index);
    }

    /**
     * Writes 8 bytes to the given index, decomposing them from a long value
     * according
     * @param index - The index from which the bytes will be write
     * @param value - long value
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void setInt64(int index, long value) {
        if (addr == 0) {
            setInt64InArray(index, value);
        } else {
            checkIndex(index, 8);
            macc.setLong(addr + index, value);
        }
    }

    /**
     * This method transfers long values into the given destination array.
     *
     * @param dst    - The array into which values are to be written
     * @param offset - The offset within the array of the first element to be written;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be written to the given array;
     * must be non-negative and no larger than dst.length - offset
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void getInt64(long[] dst, int offset, int length) {
        checkArraysBounds(offset, dst.length, length, 8);
        if (addr == 0) {
            for (int i = 0, j = offset; i < length; i++, j++) {
                dst[j] = getInt64FromArray(i * 8);
            }
        } else {
            macc.getLong(addr, dst, offset, length);
        }
    }

    /**
     * This method transfers long values from the given destination array.
     *
     * @param src    - The array from which values are to be read
     * @param offset - The offset within the array of the first element to be read;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be read from the given array;
     * must be non-negative and no larger than dst.length - offset
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void setInt64(long[] src, int offset, int length) {
        checkArraysBounds(offset, src.length, length, 8);
        if (addr == 0) {
            for (int i = 0, j = offset; i < length; i++, j++) {
                setInt64InArray(i * 8, src[j]);
            }
        } else {
            macc.setLong(addr, src, offset, length);
        }
    }

    /**
     * Reads 4/8 bytes (depending of C long type size on current platform) at
     * the given index, composing them into a long value according
     * @param index - The index from which the bytes will be read
     * @return The long value at the given index
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public long getCLong(int index) {
        if (addr == 0) {
            return CLONG_SIZE == 8
                    ? getInt64FromArray(index)
                    : getInt32FromArray(index) & 0x00000000ffffffffL;
        }
        // native
        checkIndex(index, CLONG_SIZE == 8 ? 8 : 4);
        return CLONG_SIZE == 8
                ? macc.getLong(addr + index)
                : macc.getInt(addr + index) & 0x00000000ffffffffL;
    }

    /**
     * Writes 4/8 bytes (depending of C long type size on current platform) to
     * the given index, decomposing them from a long value according
     * @param index - The index from which the bytes will be write
     * @param value - long value
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void setCLong(int index, long value) {
        if (addr == 0) {
            if (CLONG_SIZE == 8) {
                setInt64InArray(index, value);
            } else {
                setInt32InArray(index, (int) value);
            }
        } else {
            checkIndex(index, CLONG_SIZE == 8 ? 8 : 4);
            if (CLONG_SIZE == 8) { // 64-bit
                macc.setLong(addr + index, value);
            } else {
                macc.setInt(addr + index, (int) value);
            }
        }
    }

    /**
     * This method transfers C long values into the given destination array.
     *
     * @param dst    - The array into which values are to be written
     * @param offset - The offset within the array of the first element to be written;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be written to the given array;
     * must be non-negative and no larger than dst.length - offset
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void getCLong(long[] dst, int offset, int length) {
        checkArraysBounds(offset, dst.length, length, CLONG_SIZE == 8 ? 8 : 4);
        for (int i = 0, j = offset; i < length; i++, j++) {
            dst[j] = getCLong(i * CLONG_SIZE);
        }
    }

    /**
     * This method transfers C long values from the given destination array.
     *
     * @param src    - The array from which values are to be read
     * @param offset - The offset within the array of the first element to be read;
     * must be non-negative and no larger than dst.length
     * @param length - The maximum number of elements to be read from the given array;
     * must be non-negative and no larger than dst.length - offset
     *
     * @throws IndexOutOfBoundsException if from < 0 || from > src.length
     * @throws IndexOutOfBoundsException if length < 0 || length > src.length - from
     * @throws IndexOutOfBoundsException if length > this.size()
     */
    public void setCLong(long[] src, int offset, int length) {
        checkArraysBounds(offset, src.length, length, CLONG_SIZE == 8 ? 8 : 4);
        for (int i = 0, j = offset; i < length; i++, j++) {
            setCLong(i * CLONG_SIZE, src[j]);
        }
    }

    /**
     * Returns native pointer value from the specified index.
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public long getAddress(int index) {
        if(addr != 0) {
            checkIndex(index, POINTER_SIZE == 8 ? 8 : 4);
            return macc.getPointer(addr + index);
        }
        return POINTER_SIZE == 8
                ? getInt64FromArray(index)
                : getInt32FromArray(index);
    }

    /**
     *  Sets the native address to the specified index.
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void setAddress(int index, long value) {
        if (addr != 0) {
            checkIndex(index, POINTER_SIZE == 8 ? 8 : 4);
            macc.setPointer(addr + index, value);
        } else {
            if (POINTER_SIZE == 8) {
                setInt64InArray(index, value);
            } else {
                setInt32InArray(index, (int) value);
            }
        }
    }

//    Int8Pointer getPointer(int index) {
//        return NativeBridge.createInt8Pointer(getAddress(index));
//    }

    /**
     * @param index
     * @param p - pointer to set; should be unlocked in a caller method after usage;
     */
    public void setPointer(int index, VoidPointer p) {
        setAddress(index, p.lock());
    }

    /**
     * Convert String to 0 terminated UTF16 (Unicode) string.
     * And set it to the internal buffer.
     *
     * TODO: 1. if size is unknown
     * TODO: 2. add convinient method getChars(addr, size, str) to StringAccessor to avoid substringing in it
     */
    public void setString(String str){
        if (str.length() * 2 > size - 2) { // 2 times lager than str.length and 2 bytes for termination zeroes
            // awt.32=String: '{0}' does not fit
            throw new IndexOutOfBoundsException(Messages.getString("awt.32", str)); //$NON-NLS-1$
        }
        if (addr == 0) {
            setStringInArray(str);
        } else {
            stac.getChars(addr, size, str, 0, str.length());
        }
    }

    /**
     * Creates String from 0 terminated UTF16 (Unicode) string.
     * convert UTF16 bytes to String
     */
    public String getString(){
        return addr == 0
                ? getStringFromArray((size) >> 1)
                : stac.createString(addr);
    }

    /**
     * Creates String from 0 terminated UTF16 (Unicode) string of max
     * strlen length in bytes.
     */
    public String getString(long strlen){
        if(addr == 0){
            int max = (size) >> 1;
            if(max > strlen) {
                max = (int) strlen;
            }
            return getStringFromArray(max);
        }
        return stac.createString(addr, strlen);
    }

    /**
     * Creates String from 0 terminated modified UTF8 string.
     */
    public String getStringUTF() {
        long locked = shortLockPointer();
        String str = stac.createStringUTF(locked);
        unlock();
        return str;
    }

    /**
     * Creates String from 0 terminated modified UTF8 native string of max
     * strlen length in bytes.
     *
     * TODO bounds check for Modified UTF-8 strings?
     */
    public String getStringUTF(long strlen) {
        long locked = shortLockPointer();
        String str = stac.createStringUTF(locked, strlen);
        unlock();
        return str;
    }

    /**
     * Convert String to 0 terminated UTF8 (Unicode) string.
     * And set it to buffer
     *
     * TODO add convinient method getUTFChars(addr, size, str) to StringAccessor
     */
    public void setStringUTF(String str){
        long locked = shortLockPointer();
        stac.getUTFChars(locked, size, str, 0, str.length());
        unlock();
    }

    public void fill(byte value, int size) {
        if (addr == 0) {
            Arrays.fill(array, offset, size + offset, value);
        } else {
            macc.memset(addr, value, size);
        }
    }


    //-------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------
    /**
     * Gets char from the internal array reordering bytes according platform byte order.
     * It is convinient method to be used in subclasses
     */
    char getCharFromArray(int index) {
        index += offset;
        return NATIVE_BYTE_ORDER == JAVA_BYTE_ORDER
                ? (char) (((array[index++] & 0xff)  << 8) + (array[index] & 0xff))
                : (char) ((array[index++] & 0xff) + ((array[index] & 0xff) << 8));
    }

    /**
     * Sets char in the internal array reordering bytes according platform byte order.
     * It is convinient method to be used in subclasses
     */
    void setCharInArray(int index, char value) {
        index += offset;
        if (NATIVE_BYTE_ORDER == JAVA_BYTE_ORDER) {
            array[index++] = (byte) (value >> 8);
            array[index] = (byte) value;
        } else {
            array[index++] = (byte) value;
            array[index] = (byte) (value >> 8);
        }
    }

    short getInt16FromArray(int index) {
        index += offset;
        return NATIVE_BYTE_ORDER == JAVA_BYTE_ORDER
                ? (short) (((array[index++] & 0xff)  << 8) + (array[index] & 0xff))
                : (short) ((array[index++] & 0xff) + ((array[index] & 0xff) << 8));
    }

    void setInt16InArray(int index, short value) {
        index += offset;
        if (NATIVE_BYTE_ORDER == JAVA_BYTE_ORDER) {
            array[index++] = (byte) (value >> 8);
            array[index] = (byte) value;
        } else {
            array[index++] = (byte) value;
            array[index] = (byte) (value >> 8);
        }
    }

    int getInt32FromArray(int index) {
        index += offset;
        if (NATIVE_BYTE_ORDER == JAVA_BYTE_ORDER) {
            return (array[index++] & 0xff) << 24
                    + ((array[index++] & 0xff) << 16)
                    + ((array[index++] & 0xff) << 8)
                    + array[index];
        }
        return (array[index++] & 0xff)
                + ((array[index++] & 0xff) << 8)
                + ((array[index++] & 0xff) << 16)
                + (array[index] << 24);
    }

    void setInt32InArray(int index, int value) {
        index += offset;

        if (NATIVE_BYTE_ORDER == JAVA_BYTE_ORDER) {
            array[index++] = (byte) (value >> 24);
            array[index++] = (byte) (value >> 16);
            array[index++] = (byte) (value >> 8);
            array[index] = (byte) value;
        } else {
            array[index++] = (byte) value;
            array[index++] = (byte) (value >> 8);
            array[index++] = (byte) (value >> 16);
            array[index] = (byte) (value >> 24);
        }
    }

    long getInt64FromArray(int index) {
        index += offset;
        if (NATIVE_BYTE_ORDER == JAVA_BYTE_ORDER) {
            return  (array[index++] & 0xffl) << 56
                    + (array[index++] & 0xffl) << 48
                    + (array[index++] & 0xffl) << 40
                    + (array[index++] & 0xffl) << 32
                    + (array[index++] & 0xffl) << 24
                    + ((array[index++] & 0xffl) << 16)
                    + ((array[index++] & 0xffl) << 8)
                    + array[index];
        }
        return (array[index++] & 0xffl)
                + ((array[index++] & 0xffl) << 8)
                + ((array[index++] & 0xffl) << 16)
                + ((array[index++] & 0xffl) << 24)
                + ((array[index++] & 0xffl) << 32)
                + ((array[index++] & 0xffl) << 40)
                + ((array[index++] & 0xffl) << 48)
                + ((array[index] & 0xffl) << 56);
    }

    void setInt64InArray(int index, long value) {
        index += offset;

        if (NATIVE_BYTE_ORDER == JAVA_BYTE_ORDER) {
            array[index++] = (byte) (value >> 56);
            array[index++] = (byte) (value >> 48);
            array[index++] = (byte) (value >> 40);
            array[index++] = (byte) (value >> 32);
            array[index++] = (byte) (value >> 24);
            array[index++] = (byte) (value >> 16);
            array[index++] = (byte) (value >> 8);
            array[index] = (byte) value;
        } else {
            array[index++] = (byte) value;
            array[index++] = (byte) (value >> 8);
            array[index++] = (byte) (value >> 16);
            array[index++] = (byte) (value >> 24);
            array[index++] = (byte) (value >> 32);
            array[index++] = (byte) (value >> 40);
            array[index++] = (byte) (value >> 48);
            array[index] = (byte) (value >> 56);
        }
    }

    String getStringFromArray(int length) {
        char[] carr = new char[length];
        int i = 0;
        int index = offset;
        while(i < length) {
            char ch = (char) ((array[index++] & 0xff) + ((array[index++] & 0xff) << 8));
            if(ch == 0){
                break;
            }
            carr[i++] = ch;
        }
        return new String(carr, 0, i);
    }

    void setStringInArray(String str) {
        char[] chars = str.toCharArray();
        int index = offset;
        for (char element : chars) {
            array[index++] = (byte) element;
            array[index++] = (byte) (element >> 8);
        }
        //-- 2 null value are terminated the string
        array[index++] = 0;
        array[index] = 0;
    }


    final void checkIndex(int index, int numBytes) throws IndexOutOfBoundsException {
        if (size == -1) { // was created using "long addr"
            return;
        }
        if (index < 0 || index + numBytes - 1 > size) {
            // awt.33=index is out of range
            throw new IndexOutOfBoundsException(Messages.getString("awt.33")); //$NON-NLS-1$
        }
    }

    final void checkArraysBounds(int toIdx, int srcDstLength, int copyLength, int bytesInData) {
        if (size == -1) {
            return;
        }
        if (toIdx < 0 || toIdx > srcDstLength) {
            // awt.34=Initial offset in the destination array is wrong: {0}
            throw new IndexOutOfBoundsException(Messages.getString("awt.34", toIdx)); //$NON-NLS-1$
        }
        if (copyLength < 0 || copyLength > srcDstLength - toIdx || copyLength * bytesInData > size) {
            // awt.35=Wrong number of elements to copy: {0}
            throw new IndexOutOfBoundsException(Messages.getString("awt.35", copyLength)); //$NON-NLS-1$
        }
    }

    ByteBase getBytesBaseElementPointer(int index, int numBytes) {
        checkIndex(index, numBytes);
        ByteBase sub;
        if (addr == 0) {
            sub = new ByteBase(array, index + offset, this.size - index);
        } else {
            sub = new ByteBase(this.addr + index);
            if(this.size > 0) {
               sub.size = this.size - index;
            }
        }
        return sub;
    }

    public Int8Pointer getElementPointer(int index) {
        return new Int8Pointer(getBytesBaseElementPointer(index, 1));
    }
}
