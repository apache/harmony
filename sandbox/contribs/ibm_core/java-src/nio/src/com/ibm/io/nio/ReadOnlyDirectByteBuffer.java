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


import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

/**
 * DirectByteBuffer, ReadWriteDirectByteBuffer and ReadOnlyDirectByteBuffer
 * compose the implementation of platform memory based byte buffers.
 * <p>
 * ReadOnlyDirectByteBuffer extends DirectByteBuffer with all the write methods
 * throwing read only exception.
 * </p>
 * <p>
 * This class is marked final for runtime performance.
 * </p>
 * 
 */
final class ReadOnlyDirectByteBuffer extends DirectByteBuffer {

	static ReadOnlyDirectByteBuffer copy(DirectByteBuffer other, int markOfOther) {
		ReadOnlyDirectByteBuffer buf = new ReadOnlyDirectByteBuffer(
				other.safeAddress, other.capacity(), other.offset);
		buf.limit = other.limit();
		buf.position = other.position();
		buf.mark = markOfOther;
		buf.order(other.order());
		return buf;
	}

	protected ReadOnlyDirectByteBuffer(SafeAddress address, int capacity,
			int offset) {
		super(address, capacity, offset);
	}

	public ByteBuffer asReadOnlyBuffer() {
		return copy(this, mark);
	}

	public ByteBuffer compact() {
		throw new ReadOnlyBufferException();
	}

	public ByteBuffer duplicate() {
		return copy(this, mark);
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

	public boolean isReadOnly() {
		return true;
	}

	public ByteBuffer put(byte value) {
		throw new ReadOnlyBufferException();
	}

	public ByteBuffer put(int index, byte value) {
		throw new ReadOnlyBufferException();
	}

	public ByteBuffer putDouble(double value) {
		throw new ReadOnlyBufferException();
	}

	public ByteBuffer putDouble(int index, double value) {
		throw new ReadOnlyBufferException();
	}

	public ByteBuffer putFloat(float value) {
		throw new ReadOnlyBufferException();
	}

	public ByteBuffer putFloat(int index, float value) {
		throw new ReadOnlyBufferException();
	}

	public ByteBuffer putInt(int value) {
		throw new ReadOnlyBufferException();
	}

	public ByteBuffer putInt(int index, int value) {
		throw new ReadOnlyBufferException();
	}

	public ByteBuffer putLong(int index, long value) {
		throw new ReadOnlyBufferException();
	}

	public ByteBuffer putLong(long value) {
		throw new ReadOnlyBufferException();
	}

	public ByteBuffer putShort(int index, short value) {
		throw new ReadOnlyBufferException();
	}

	public ByteBuffer putShort(short value) {
		throw new ReadOnlyBufferException();
	}

	public ByteBuffer slice() {
		ReadOnlyDirectByteBuffer buf = new ReadOnlyDirectByteBuffer(
				safeAddress, remaining(), offset + position);
		buf.order = order;
		return buf;
	}

}
