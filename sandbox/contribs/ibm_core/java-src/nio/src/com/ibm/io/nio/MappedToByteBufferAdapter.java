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


import java.io.File;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel.MapMode;

/**
 * Implements MappedByteBuffer by wrapping a ByteBuffer.
 * <p>
 * Implementation notice:
 * <ul>
 * <li>After a byte buffer instance is wrapped, it becomes privately owned by
 * the adapter. It must NOT be accessed outside the adapter any more.</li>
 * <li>The byte buffer's position and limit are NOT linked with the adapter.
 * The adapter extends Buffer, thus has its own position and limit.</li>
 * <li>The byte buffer's byte order is linked with the adapter.</li>
 * </ul>
 * </p>
 * 
 */
final class MappedToByteBufferAdapter extends MappedByteBuffer {

	MappedToByteBufferAdapter(File mappedFile, long offset, int size,
			MapMode mapMode) {
		super(mappedFile, offset, size, mapMode);
	}

	MappedToByteBufferAdapter(File mappedFile, long offset, int size,
			MapMode mapMode, ByteBuffer wrappedBuffer) {
		super(mappedFile, offset, size, mapMode, wrappedBuffer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#asCharBuffer()
	 */
	public CharBuffer asCharBuffer() {
		return CharToByteBufferAdapter.wrap(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#asDoubleBuffer()
	 */
	public DoubleBuffer asDoubleBuffer() {
		return DoubleToByteBufferAdapter.wrap(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#asFloatBuffer()
	 */
	public FloatBuffer asFloatBuffer() {
		return FloatToByteBufferAdapter.wrap(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#asIntBuffer()
	 */
	public IntBuffer asIntBuffer() {
		return IntToByteBufferAdapter.wrap(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#asLongBuffer()
	 */
	public LongBuffer asLongBuffer() {
		return LongToByteBufferAdapter.wrap(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#asReadOnlyBuffer()
	 */
	public ByteBuffer asReadOnlyBuffer() {
		MappedToByteBufferAdapter buf = new MappedToByteBufferAdapter(
				mappedFile, offset, capacity, mapMode, wrappedBuffer
						.asReadOnlyBuffer());
		buf.position = position;
		buf.limit = limit;
		buf.mark = mark;
		buf.order = order;
		return buf;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#asShortBuffer()
	 */
	public ShortBuffer asShortBuffer() {
		return ShortToByteBufferAdapter.wrap(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#compact()
	 */
	public ByteBuffer compact() {
		if (mapMode == MapMode.READ_ONLY || wrappedBuffer.isReadOnly()) {
			throw new ReadOnlyBufferException();
		}
		wrappedBuffer.limit(limit);
		wrappedBuffer.position(position);
		wrappedBuffer.compact();
		wrappedBuffer.clear();
		position = limit - position;
		limit = capacity;
		mark = UNSET_MARK;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#duplicate()
	 */
	public ByteBuffer duplicate() {
		MappedToByteBufferAdapter buf = new MappedToByteBufferAdapter(
				mappedFile, offset, capacity, mapMode, wrappedBuffer
						.duplicate());
		buf.position = position;
		buf.limit = limit;
		buf.mark = mark;
		buf.order = order;
		return buf;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#get()
	 */
	public byte get() {
		if (position == limit) {
			throw new BufferUnderflowException();
		}
		return wrappedBuffer.get(position++);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#get(int)
	 */
	public byte get(int index) {
		if (index < 0 || index >= limit) {
			throw new IndexOutOfBoundsException();
		}
		return wrappedBuffer.get(index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#getChar()
	 */
	public char getChar() {
		return (char) getShort();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#getChar(int)
	 */
	public char getChar(int index) {
		return (char) getShort(index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#getDouble()
	 */
	public double getDouble() {
		return Double.longBitsToDouble(getLong());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#getDouble(int)
	 */
	public double getDouble(int index) {
		return Double.longBitsToDouble(getLong(index));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#getFloat()
	 */
	public float getFloat() {
		return Float.intBitsToFloat(getInt());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#getFloat(int)
	 */
	public float getFloat(int index) {
		return Float.intBitsToFloat(getInt(index));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#getInt()
	 */
	public int getInt() {
		int newPosition = position + 4;
		if (newPosition > limit) {
			throw new BufferUnderflowException();
		}
		int result = wrappedBuffer.getInt(position);
		position = newPosition;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#getInt(int)
	 */
	public int getInt(int index) {
		if (index < 0 || index + 4 > limit) {
			throw new IndexOutOfBoundsException();
		}
		return wrappedBuffer.getInt(index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#getLong()
	 */
	public long getLong() {
		int newPosition = position + 8;
		if (newPosition > limit) {
			throw new BufferUnderflowException();
		}
		long result = wrappedBuffer.getLong(position);
		position = newPosition;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#getLong(int)
	 */
	public long getLong(int index) {
		if (index < 0 || index + 8 > limit) {
			throw new IndexOutOfBoundsException();
		}
		return wrappedBuffer.getLong(index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#getShort()
	 */
	public short getShort() {
		int newPosition = position + 2;
		if (newPosition > limit) {
			throw new BufferUnderflowException();
		}
		short result = wrappedBuffer.getShort(position);
		position = newPosition;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#getShort(int)
	 */
	public short getShort(int index) {
		if (index < 0 || index + 2 > limit) {
			throw new IndexOutOfBoundsException();
		}
		return wrappedBuffer.getShort(index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#isDirect()
	 */
	public boolean isDirect() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.Buffer#isReadOnly()
	 */
	public boolean isReadOnly() {
		return mapMode == MapMode.READ_ONLY || wrappedBuffer.isReadOnly();
	}

	/*
	 * Override to keep this.order linked with wrappedBuffer.order
	 */
	public ByteBuffer order(ByteOrder byteOrder) {
		super.order(byteOrder);
		wrappedBuffer.order(byteOrder);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#protectedArray()
	 */
	protected byte[] protectedArray() {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#protectedArrayOffset()
	 */
	protected int protectedArrayOffset() {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#protectedHasArray()
	 */
	protected boolean protectedHasArray() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#put(byte)
	 */
	public ByteBuffer put(byte b) {
		if (mapMode == MapMode.READ_ONLY || wrappedBuffer.isReadOnly()) {
			throw new ReadOnlyBufferException();
		}
		if (position == limit) {
			throw new BufferOverflowException();
		}
		wrappedBuffer.put(position++, b);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#put(int, byte)
	 */
	public ByteBuffer put(int index, byte b) {
		if (mapMode == MapMode.READ_ONLY || wrappedBuffer.isReadOnly()) {
			throw new ReadOnlyBufferException();
		}
		if (index < 0 || index >= limit) {
			throw new IndexOutOfBoundsException();
		}
		wrappedBuffer.put(index, b);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#putChar(char)
	 */
	public ByteBuffer putChar(char value) {
		return putShort((short) value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#putChar(int, char)
	 */
	public ByteBuffer putChar(int index, char value) {
		return putShort(index, (short) value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#putDouble(double)
	 */
	public ByteBuffer putDouble(double value) {
		return putLong(Double.doubleToRawLongBits(value));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#putDouble(int, double)
	 */
	public ByteBuffer putDouble(int index, double value) {
		return putLong(index, Double.doubleToRawLongBits(value));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#putFloat(float)
	 */
	public ByteBuffer putFloat(float value) {
		return putInt(Float.floatToIntBits(value));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#putFloat(int, float)
	 */
	public ByteBuffer putFloat(int index, float value) {
		return putInt(index, Float.floatToIntBits(value));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#putInt(int)
	 */
	public ByteBuffer putInt(int value) {
		if (mapMode == MapMode.READ_ONLY || wrappedBuffer.isReadOnly()) {
			throw new ReadOnlyBufferException();
		}
		int newPosition = position + 4;
		if (newPosition > limit) {
			throw new BufferOverflowException();
		}
		wrappedBuffer.putInt(position, value);
		position = newPosition;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#putInt(int, int)
	 */
	public ByteBuffer putInt(int index, int value) {
		if (mapMode == MapMode.READ_ONLY || wrappedBuffer.isReadOnly()) {
			throw new ReadOnlyBufferException();
		}
		if (index < 0 || index + 4 > limit) {
			throw new IndexOutOfBoundsException();
		}
		wrappedBuffer.putInt(index, value);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#putLong(int, long)
	 */
	public ByteBuffer putLong(int index, long value) {
		if (mapMode == MapMode.READ_ONLY || wrappedBuffer.isReadOnly()) {
			throw new ReadOnlyBufferException();
		}
		if (index < 0 || index + 8 > limit) {
			throw new IndexOutOfBoundsException();
		}
		wrappedBuffer.putLong(index, value);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#putLong(long)
	 */
	public ByteBuffer putLong(long value) {
		if (mapMode == MapMode.READ_ONLY || wrappedBuffer.isReadOnly()) {
			throw new ReadOnlyBufferException();
		}
		int newPosition = position + 8;
		if (newPosition > limit) {
			throw new BufferOverflowException();
		}
		wrappedBuffer.putLong(position, value);
		position = newPosition;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#putShort(int, short)
	 */
	public ByteBuffer putShort(int index, short value) {
		if (mapMode == MapMode.READ_ONLY || wrappedBuffer.isReadOnly()) {
			throw new ReadOnlyBufferException();
		}
		if (index < 0 || index + 2 > limit) {
			throw new IndexOutOfBoundsException();
		}
		wrappedBuffer.putShort(index, value);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#putShort(short)
	 */
	public ByteBuffer putShort(short value) {
		if (mapMode == MapMode.READ_ONLY || wrappedBuffer.isReadOnly()) {
			throw new ReadOnlyBufferException();
		}
		int newPosition = position + 2;
		if (newPosition > limit) {
			throw new BufferOverflowException();
		}
		wrappedBuffer.putShort(position, value);
		position = newPosition;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.ByteBuffer#slice()
	 */
	public ByteBuffer slice() {
		wrappedBuffer.limit(limit);
		wrappedBuffer.position(position);
		MappedToByteBufferAdapter result = new MappedToByteBufferAdapter(
				mappedFile, offset + position, remaining(), mapMode,
				wrappedBuffer.slice());
		wrappedBuffer.clear();
		result.order = order;
		return result;
	}

}
