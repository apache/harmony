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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.nio.ShortBuffer;

/**
 * This class wraps a byte buffer to be a short buffer.
 * <p>
 * Implementation notice:
 * <ul>
 * <li>After a byte buffer instance is wrapped, it becomes privately owned by
 * the adapter. It must NOT be accessed outside the adapter any more.</li>
 * <li>The byte buffer's position and limit are NOT linked with the adapter.
 * The adapter extends Buffer, thus has its own position and limit.</li>
 * </ul>
 * </p>
 * 
 */
final class ShortToByteBufferAdapter extends ShortBuffer {

	static ShortBuffer wrap(ByteBuffer byteBuffer) {
		return new ShortToByteBufferAdapter(byteBuffer.slice());
	}

	private final ByteBuffer byteBuffer;

	ShortToByteBufferAdapter(ByteBuffer byteBuffer) {
		super((byteBuffer.capacity() >> 1));
		this.byteBuffer = byteBuffer;
		this.byteBuffer.clear();
	}

	public ShortBuffer asReadOnlyBuffer() {
		ShortToByteBufferAdapter buf = new ShortToByteBufferAdapter(byteBuffer
				.asReadOnlyBuffer());
		buf.limit = limit;
		buf.position = position;
		buf.mark = mark;
		return buf;
	}

	public ShortBuffer compact() {
		if (byteBuffer.isReadOnly()) {
			throw new ReadOnlyBufferException();
		}
		byteBuffer.limit(limit << 1);
		byteBuffer.position(position << 1);
		byteBuffer.compact();
		byteBuffer.clear();
		position = limit - position;
		limit = capacity;
		mark = UNSET_MARK;
		return this;
	}

	public ShortBuffer duplicate() {
		ShortToByteBufferAdapter buf = new ShortToByteBufferAdapter(byteBuffer
				.duplicate());
		buf.limit = limit;
		buf.position = position;
		buf.mark = mark;
		return buf;
	}

	public short get() {
		if (position == limit) {
			throw new BufferUnderflowException();
		}
		return byteBuffer.getShort(position++ << 1);
	}

	public short get(int index) {
		if (index < 0 || index >= limit) {
			throw new IndexOutOfBoundsException();
		}
		return byteBuffer.getShort(index << 1);
	}

	public boolean isDirect() {
		return byteBuffer.isDirect();
	}

	public boolean isReadOnly() {
		return byteBuffer.isReadOnly();
	}

	public ByteOrder order() {
		return byteBuffer.order();
	}

	protected short[] protectedArray() {
		throw new UnsupportedOperationException();
	}

	protected int protectedArrayOffset() {
		throw new UnsupportedOperationException();
	}

	protected boolean protectedHasArray() {
		return false;
	}

	public ShortBuffer put(short c) {
		if (position == limit) {
			throw new BufferOverflowException();
		}
		byteBuffer.putShort(position++ << 1, c);
		return this;
	}

	public ShortBuffer put(int index, short c) {
		if (index < 0 || index >= limit) {
			throw new IndexOutOfBoundsException();
		}
		byteBuffer.putShort(index << 1, c);
		return this;
	}

	public ShortBuffer slice() {
		byteBuffer.limit(limit << 1);
		byteBuffer.position(position << 1);
		ShortBuffer result = new ShortToByteBufferAdapter(byteBuffer.slice());
		byteBuffer.clear();
		return result;
	}

}
