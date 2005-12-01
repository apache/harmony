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
import java.nio.IntBuffer;
import java.nio.ReadOnlyBufferException;

/**
 * This class wraps a byte buffer to be a int buffer.
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
final class IntToByteBufferAdapter extends IntBuffer {

	static IntBuffer wrap(ByteBuffer byteBuffer) {
		return new IntToByteBufferAdapter(byteBuffer.slice());
	}

	private final ByteBuffer byteBuffer;

	IntToByteBufferAdapter(ByteBuffer byteBuffer) {
		super((byteBuffer.capacity() >> 2));
		this.byteBuffer = byteBuffer;
		this.byteBuffer.clear();
	}

	public IntBuffer asReadOnlyBuffer() {
		IntToByteBufferAdapter buf = new IntToByteBufferAdapter(byteBuffer
				.asReadOnlyBuffer());
		buf.limit = limit;
		buf.position = position;
		buf.mark = mark;
		return buf;
	}

	public IntBuffer compact() {
		if (byteBuffer.isReadOnly()) {
			throw new ReadOnlyBufferException();
		}
		byteBuffer.limit(limit << 2);
		byteBuffer.position(position << 2);
		byteBuffer.compact();
		byteBuffer.clear();
		position = limit - position;
		limit = capacity;
		mark = UNSET_MARK;
		return this;
	}

	public IntBuffer duplicate() {
		IntToByteBufferAdapter buf = new IntToByteBufferAdapter(byteBuffer
				.duplicate());
		buf.limit = limit;
		buf.position = position;
		buf.mark = mark;
		return buf;
	}

	public int get() {
		if (position == limit) {
			throw new BufferUnderflowException();
		}
		return byteBuffer.getInt(position++ << 2);
	}

	public int get(int index) {
		if (index < 0 || index >= limit) {
			throw new IndexOutOfBoundsException();
		}
		return byteBuffer.getInt(index << 2);
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

	protected int[] protectedArray() {
		throw new UnsupportedOperationException();
	}

	protected int protectedArrayOffset() {
		throw new UnsupportedOperationException();
	}

	protected boolean protectedHasArray() {
		return false;
	}

	public IntBuffer put(int c) {
		if (position == limit) {
			throw new BufferOverflowException();
		}
		byteBuffer.putInt(position++ << 2, c);
		return this;
	}

	public IntBuffer put(int index, int c) {
		if (index < 0 || index >= limit) {
			throw new IndexOutOfBoundsException();
		}
		byteBuffer.putInt(index << 2, c);
		return this;
	}

	public IntBuffer slice() {
		byteBuffer.limit(limit << 2);
		byteBuffer.position(position << 2);
		IntBuffer result = new IntToByteBufferAdapter(byteBuffer.slice());
		byteBuffer.clear();
		return result;
	}

}
