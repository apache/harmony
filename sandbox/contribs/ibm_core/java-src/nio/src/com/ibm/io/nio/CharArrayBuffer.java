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
import java.nio.ByteOrder;
import java.nio.CharBuffer;

/**
 * CharArrayBuffer, ReadWriteCharArrayBuffer and ReadOnlyCharArrayBuffer compose
 * the implementation of array based char buffers.
 * <p>
 * CharArrayBuffer implements all the shared readonly methods and is extended by
 * the other two classes.
 * </p>
 * <p>
 * All methods are marked final for runtime performance.
 * </p>
 * 
 */
abstract class CharArrayBuffer extends CharBuffer {

	protected final char[] backingArray;

	protected final int offset;

	CharArrayBuffer(char[] array) {
		this(array.length, array, 0);
	}

	CharArrayBuffer(int capacity) {
		this(capacity, new char[capacity], 0);
	}

	CharArrayBuffer(int capacity, char[] backingArray, int offset) {
		super(capacity);
		this.backingArray = backingArray;
		this.offset = offset;
	}

	public final char get() {
		if (position == limit) {
			throw new BufferUnderflowException();
		}
		return backingArray[offset + position++];
	}

	public final char get(int index) {
		if (index < 0 || index >= limit) {
			throw new IndexOutOfBoundsException();
		}
		return backingArray[offset + index];
	}

	public final boolean isDirect() {
		return false;
	}

	public final ByteOrder order() {
		return ByteOrder.nativeOrder();
	}

	public final CharSequence subSequence(int start, int end) {
		if (start < 0 || start > remaining()) {
			throw new IndexOutOfBoundsException();
		}
		if (end < start || end > remaining()) {
			throw new IndexOutOfBoundsException();
		}
		CharBuffer result = duplicate();
		result.limit(position + end);
		result.position(position + start);
		return result;
	}

	public final String toString() {
		return String.copyValueOf(backingArray, offset + position, remaining());
	}
}
