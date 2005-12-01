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


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import com.ibm.platform.struct.PlatformAddress;

/**
 * DirectByteBuffer, ReadWriteDirectByteBuffer and ReadOnlyDirectByteBuffer
 * compose the implementation of platform memory based byte buffers.
 * <p>
 * ReadWriteDirectByteBuffer extends DirectByteBuffer with all the write
 * methods.
 * </p>
 * <p>
 * This class is marked final for runtime performance.
 * </p>
 * 
 */
final class ReadWriteDirectByteBuffer extends DirectByteBuffer {

	static ReadWriteDirectByteBuffer copy(DirectByteBuffer other,
			int markOfOther) {
		ReadWriteDirectByteBuffer buf = new ReadWriteDirectByteBuffer(
				other.safeAddress, other.capacity(), other.offset);
		buf.limit = other.limit();
		buf.position = other.position();
		buf.mark = markOfOther;
		buf.order(other.order());
		return buf;
	}

	ReadWriteDirectByteBuffer(int capacity) {
		super(capacity);
	}

	ReadWriteDirectByteBuffer(SafeAddress address, int capacity, int offset) {
		super(address, capacity, offset);
	}

	public ByteBuffer asReadOnlyBuffer() {
		return ReadOnlyDirectByteBuffer.copy(this, mark);
	}

	public ByteBuffer compact() {
		PlatformAddress effectiveAddress = getEffectiveAddress();
		effectiveAddress.offsetBytes(position).moveTo(effectiveAddress,
				remaining());
		position = limit - position;
		limit = capacity;
		mark = UNSET_MARK;
		return this;
	}

	public ByteBuffer duplicate() {
		return copy(this, mark);
	}

	public boolean isReadOnly() {
		return false;
	}

	protected byte[] protectedArray() {
		throw new UnsupportedOperationException();
	}

	protected int protectedArrayOffset() {
		throw new UnsupportedOperationException();
	}

	protected boolean protectedHasArray() {
		return false;
	}

	public ByteBuffer put(byte value) {
		if (position == limit) {
			throw new BufferOverflowException();
		}
		getBaseAddress().setByte(offset + position++, value);
		return this;
	}

	public ByteBuffer put(int index, byte value) {
		if (index < 0 || index >= limit) {
			throw new IndexOutOfBoundsException();
		}
		getBaseAddress().setByte(offset + index, value);
		return this;

	}

	public ByteBuffer putDouble(double value) {
		int newPosition = position + 8;
		if (newPosition > limit) {
			throw new BufferOverflowException();
		}
		getBaseAddress().setDouble(offset + position, value, order);
		position = newPosition;
		return this;
	}

	public ByteBuffer putDouble(int index, double value) {
		if (index < 0 || index + 8 > limit) {
			throw new IndexOutOfBoundsException();
		}
		getBaseAddress().setDouble(offset + index, value, order);
		return this;
	}

	public ByteBuffer putFloat(float value) {
		int newPosition = position + 4;
		if (newPosition > limit) {
			throw new BufferOverflowException();
		}
		getBaseAddress().setFloat(offset + position, value, order);
		position = newPosition;
		return this;
	}

	public ByteBuffer putFloat(int index, float value) {
		if (index < 0 || index + 4 > limit) {
			throw new IndexOutOfBoundsException();
		}
		getBaseAddress().setFloat(offset + index, value, order);
		return this;
	}

	public ByteBuffer putInt(int value) {
		int newPosition = position + 4;
		if (newPosition > limit) {
			throw new BufferOverflowException();
		}
		getBaseAddress().setInt(offset + position, value, order);
		position = newPosition;
		return this;
	}

	public ByteBuffer putInt(int index, int value) {
		if (index < 0 || index + 4 > limit) {
			throw new IndexOutOfBoundsException();
		}
		getBaseAddress().setInt(offset + index, value, order);
		return this;
	}

	public ByteBuffer putLong(long value) {
		int newPosition = position + 8;
		if (newPosition > limit) {
			throw new BufferOverflowException();
		}
		getBaseAddress().setLong(offset + position, value, order);
		position = newPosition;
		return this;
	}

	public ByteBuffer putLong(int index, long value) {
		if (index < 0 || index + 8 > limit) {
			throw new IndexOutOfBoundsException();
		}
		getBaseAddress().setLong(offset + index, value, order);
		return this;
	}

	public ByteBuffer putShort(short value) {
		int newPosition = position + 2;
		if (newPosition > limit) {
			throw new BufferOverflowException();
		}
		getBaseAddress().setShort(offset + position, value, order);
		position = newPosition;
		return this;
	}

	public ByteBuffer putShort(int index, short value) {
		if (index < 0 || index + 2 > limit) {
			throw new IndexOutOfBoundsException();
		}
		getBaseAddress().setShort(offset + index, value, order);
		return this;
	}

	public ByteBuffer slice() {
		ReadWriteDirectByteBuffer buf = new ReadWriteDirectByteBuffer(
				safeAddress, remaining(), offset + position);
		buf.order = order;
		return buf;
	}

}
