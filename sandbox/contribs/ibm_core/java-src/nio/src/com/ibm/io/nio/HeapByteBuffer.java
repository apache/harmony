/* Copyright 2004 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.io.nio;


import java.nio.BufferUnderflowException;

import com.ibm.platform.Endianness;

/**
 * HeapByteBuffer, ReadWriteHeapByteBuffer and ReadOnlyHeapByteBuffer compose
 * the implementation of array based byte buffers.
 * <p>
 * HeapByteBuffer implements all the shared readonly methods and is extended by
 * the other two classes.
 * </p>
 * <p>
 * All methods are marked final for runtime performance.
 * </p>
 * 
 */
abstract class HeapByteBuffer extends BaseByteBuffer {

	protected final byte[] backingArray;

	protected final int offset;

	HeapByteBuffer(byte[] backingArray) {
		this(backingArray, backingArray.length, 0);
	}

	HeapByteBuffer(int capacity) {
		this(new byte[capacity], capacity, 0);
	}

	HeapByteBuffer(byte[] backingArray, int capacity, int offset) {
		super(capacity);
		this.backingArray = backingArray;
		this.offset = offset;

		if (offset + capacity > backingArray.length) {
			throw new IndexOutOfBoundsException();
		}
	}

	public final byte get() {
		if (position == limit) {
			throw new BufferUnderflowException();
		}
		return backingArray[offset + position++];
	}

	public final byte get(int index) {
		if (index < 0 || index >= limit) {
			throw new IndexOutOfBoundsException();
		}
		return backingArray[offset + index];
	}

	public final double getDouble() {
		return Double.longBitsToDouble(getLong());
	}

	public final double getDouble(int index) {
		return Double.longBitsToDouble(getLong(index));
	}

	public final float getFloat() {
		return Float.intBitsToFloat(getInt());
	}

	public final float getFloat(int index) {
		return Float.intBitsToFloat(getInt(index));
	}

	public final int getInt() {
		int newPosition = position + 4;
		if (newPosition > limit) {
			throw new BufferUnderflowException();
		}
		int result = loadInt(position);
		position = newPosition;
		return result;
	}

	public final int getInt(int index) {
		if (index < 0 || index + 4 > limit) {
			throw new IndexOutOfBoundsException();
		}
		return loadInt(index);
	}

	public final long getLong() {
		int newPosition = position + 8;
		if (newPosition > limit) {
			throw new BufferUnderflowException();
		}
		long result = loadLong(position);
		position = newPosition;
		return result;
	}

	public final long getLong(int index) {
		if (index < 0 || index + 8 > limit) {
			throw new IndexOutOfBoundsException();
		}
		return loadLong(index);
	}

	public final short getShort() {
		int newPosition = position + 2;
		if (newPosition > limit) {
			throw new BufferUnderflowException();
		}
		short result = loadShort(position);
		position = newPosition;
		return result;
	}

	public final short getShort(int index) {
		if (index < 0 || index + 2 > limit) {
			throw new IndexOutOfBoundsException();
		}
		return loadShort(index);
	}

	public final boolean isDirect() {
		return false;
	}

	protected final int loadInt(int index) {
		int baseOffset = offset + index;
		int bytes = 0;
		for (int i = 0; i < 4; i++) {
			bytes = bytes << 8;
			bytes = bytes | (backingArray[baseOffset + i] & 0xFF);
		}
		return (order == Endianness.BIG_ENDIAN) ? bytes : swap(bytes);
	}

	protected final long loadLong(int index) {
		int baseOffset = offset + index;
		long bytes = 0;
		for (int i = 0; i < 8; i++) {
			bytes = bytes << 8;
			bytes = bytes | (backingArray[baseOffset + i] & 0xFF);
		}
		return (order == Endianness.BIG_ENDIAN) ? bytes : swap(bytes);
	}

	protected final short loadShort(int index) {
		int baseOffset = offset + index;
		short bytes = (short) (backingArray[baseOffset] << 8);
		bytes |= (backingArray[baseOffset + 1] & 0xFF);
		return (order == Endianness.BIG_ENDIAN) ? bytes : swap(bytes);
	}

	protected final void store(int index, int value) {
		int baseOffset = offset + index;
		int bytes = (order == Endianness.BIG_ENDIAN) ? value : swap(value);
		for (int i = 3; i >= 0; i--) {
			backingArray[baseOffset + i] = (byte) (bytes & 0xFF);
			bytes = bytes >> 8;
		}
	}

	protected final void store(int index, long value) {
		int baseOffset = offset + index;
		long bytes = (order == Endianness.BIG_ENDIAN) ? value : swap(value);
		for (int i = 7; i >= 0; i--) {
			backingArray[baseOffset + i] = (byte) (bytes & 0xFF);
			bytes = bytes >> 8;
		}
	}

	protected final void store(int index, short value) {
		int baseOffset = offset + index;
		short bytes = (order == Endianness.BIG_ENDIAN) ? value : swap(value);
		backingArray[baseOffset] = (byte) ((bytes >> 8) & 0xFF);
		backingArray[baseOffset + 1] = (byte) (bytes & 0xFF);
	}

	private int swap(int value) {
		short left = (short) (value >> 16);
		short right = (short) value;
		int topEnd = swap(right) << 16;
		int btmEnd = swap(left) & 0xFFFF;
		return topEnd | btmEnd;
	}

	private long swap(long value) {
		int left = (int) (value >> 32);
		int right = (int) value;
		long topEnd = ((long) swap(right)) << 32;
		long btmEnd = swap(left) & 0xFFFFFFFFL;
		return topEnd | btmEnd;
	}

	private short swap(short value) {
		int topEnd = value << 8;
		int btmEnd = (value >> 8) & 0xFF;
		return (short) (topEnd | btmEnd);
	}
}
