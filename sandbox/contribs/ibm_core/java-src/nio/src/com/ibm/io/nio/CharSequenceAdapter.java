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
import java.nio.ReadOnlyBufferException;

/**
 * This class wraps a char sequence to be a char buffer.
 * <p>
 * Implementation notice:
 * <ul>
 * <li>Char sequence based buffer is always readonly.</li>
 * </ul>
 * </p>
 * 
 */
final class CharSequenceAdapter extends CharBuffer {

	static CharSequenceAdapter copy(CharSequenceAdapter other) {
		CharSequenceAdapter buf = new CharSequenceAdapter(other.sequence);
		buf.limit = other.limit;
		buf.position = other.position;
		buf.mark = other.mark;
		return buf;
	}

	final CharSequence sequence;

	CharSequenceAdapter(CharSequence chseq) {
		super(chseq.length());
		sequence = chseq;
	}

	public CharBuffer asReadOnlyBuffer() {
		return duplicate();
	}

	public CharBuffer compact() {
		throw new ReadOnlyBufferException();
	}

	public CharBuffer duplicate() {
		return copy(this);
	}

	public char get() {
		if (position == limit) {
			throw new BufferUnderflowException();
		}
		return sequence.charAt(position++);
	}

	public char get(int index) {
		if (index < 0 || index >= limit) {
			throw new IndexOutOfBoundsException();
		}
		return sequence.charAt(index);
	}

	public boolean isDirect() {
		return false;
	}

	public boolean isReadOnly() {
		return true;
	}

	public ByteOrder order() {
		return ByteOrder.nativeOrder();
	}

	protected char[] protectedArray() {
		throw new UnsupportedOperationException();
	}

	protected int protectedArrayOffset() {
		throw new UnsupportedOperationException();
	}

	protected boolean protectedHasArray() {
		return false;
	}

	public CharBuffer put(char c) {
		throw new ReadOnlyBufferException();
	}

	public CharBuffer put(int index, char c) {
		throw new ReadOnlyBufferException();
	}

	public CharBuffer slice() {
		return new CharSequenceAdapter(sequence.subSequence(position, limit));
	}

	public CharSequence subSequence(int start, int end) {
		if (start < 0 || start > remaining()) {
			throw new IndexOutOfBoundsException();
		}
		if (end < start || end > remaining()) {
			throw new IndexOutOfBoundsException();
		}
		CharSequenceAdapter result = copy(this);
		result.position = position + start;
		result.limit = position + end;
		return result;
	}
}
