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
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * Serves as the root of other byte buffer impl classes, implements common
 * methods that can be shared by child classes.
 * 
 */
abstract class BaseByteBuffer extends ByteBuffer {

	protected BaseByteBuffer(int capacity) {
		super(capacity);
	}

	public final CharBuffer asCharBuffer() {
		return CharToByteBufferAdapter.wrap(this);
	}

	public final DoubleBuffer asDoubleBuffer() {
		return DoubleToByteBufferAdapter.wrap(this);
	}

	public final FloatBuffer asFloatBuffer() {
		return FloatToByteBufferAdapter.wrap(this);
	}

	public final IntBuffer asIntBuffer() {
		return IntToByteBufferAdapter.wrap(this);
	}

	public final LongBuffer asLongBuffer() {
		return LongToByteBufferAdapter.wrap(this);
	}

	public final ShortBuffer asShortBuffer() {
		return ShortToByteBufferAdapter.wrap(this);
	}

	public final char getChar() {
		return (char) getShort();
	}

	public final char getChar(int index) {
		return (char) getShort(index);
	}

	public final ByteBuffer putChar(char value) {
		return putShort((short) value);
	}

	public final ByteBuffer putChar(int index, char value) {
		return putShort(index, (short) value);
	}
}
