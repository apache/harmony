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


import java.nio.LongBuffer;
import java.nio.ReadOnlyBufferException;

/**
 * LongArrayBuffer, ReadWriteLongArrayBuffer and ReadOnlyLongArrayBuffer compose
 * the implementation of array based long buffers.
 * <p>
 * ReadOnlyLongArrayBuffer extends LongArrayBuffer with all the write methods
 * throwing read only exception.
 * </p>
 * <p>
 * This class is marked final for runtime performance.
 * </p>
 * 
 */
final class ReadOnlyLongArrayBuffer extends LongArrayBuffer {

	static ReadOnlyLongArrayBuffer copy(LongArrayBuffer other, int markOfOther) {
		ReadOnlyLongArrayBuffer buf = new ReadOnlyLongArrayBuffer(other
				.capacity(), other.backingArray, other.offset);
		buf.limit = other.limit();
		buf.position = other.position();
		buf.mark = markOfOther;
		return buf;
	}

	ReadOnlyLongArrayBuffer(int capacity, long[] backingArray, int arrayOffset) {
		super(capacity, backingArray, arrayOffset);
	}

	public LongBuffer asReadOnlyBuffer() {
		return duplicate();
	}

	public LongBuffer compact() {
		throw new ReadOnlyBufferException();
	}

	public LongBuffer duplicate() {
		return copy(this, mark);
	}

	public boolean isReadOnly() {
		return true;
	}

	protected long[] protectedArray() {
		throw new ReadOnlyBufferException();
	}

	protected int protectedArrayOffset() {
		throw new ReadOnlyBufferException();
	}

	protected boolean protectedHasArray() {
		return false;
	}

	public LongBuffer put(long c) {
		throw new ReadOnlyBufferException();
	}

	public LongBuffer put(int index, long c) {
		throw new ReadOnlyBufferException();
	}

	public LongBuffer slice() {
		return new ReadOnlyLongArrayBuffer(remaining(), backingArray, offset
				+ position);
	}

}
