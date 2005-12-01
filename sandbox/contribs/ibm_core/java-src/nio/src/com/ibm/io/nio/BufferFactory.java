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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel.MapMode;

/**
 * Provide factory service of buffer classes.
 * <p>
 * Since all buffer impl classes are package private (except DirectByteBuffer),
 * this factory is the only entrance to access buffer functions from outside of
 * the impl package.
 * </p>
 * 
 */
public final class BufferFactory {

	/**
	 * Returns a new byte buffer based on the specified byte array.
	 * 
	 * @param array
	 *            The byte array
	 * @return A new byte buffer based on the specified byte array.
	 */
	public static ByteBuffer newByteBuffer(byte array[]) {
		return new ReadWriteHeapByteBuffer(array);
	}

	/**
	 * Returns a new array based byte buffer with the specified capacity.
	 * 
	 * @param capacity
	 *            The capacity of the new buffer
	 * @return A new array based byte buffer with the specified capacity.
	 */
	public static ByteBuffer newByteBuffer(int capacity) {
		return new ReadWriteHeapByteBuffer(capacity);
	}

	/**
	 * Returns a new char buffer based on the specified char array.
	 * 
	 * @param array
	 *            The char array
	 * @return A new char buffer based on the specified char array.
	 */
	public static CharBuffer newCharBuffer(char array[]) {
		return new ReadWriteCharArrayBuffer(array);
	}

	/**
	 * Returns a new readonly char buffer based on the specified char sequence.
	 * 
	 * @param chseq
	 *            The char sequence
	 * @return A new readonly char buffer based on the specified char sequence.
	 */
	public static CharBuffer newCharBuffer(CharSequence chseq) {
		return new CharSequenceAdapter(chseq);
	}

	/**
	 * Returns a new array based char buffer with the specified capacity.
	 * 
	 * @param capacity
	 *            The capacity of the new buffer
	 * @return A new array based char buffer with the specified capacity.
	 */
	public static CharBuffer newCharBuffer(int capacity) {
		return new ReadWriteCharArrayBuffer(capacity);
	}

	/**
	 * Returns a new direct byte buffer with the specified capacity.
	 * 
	 * @param capacity
	 *            The capacity of the new buffer
	 * @return A new direct byte buffer with the specified capacity.
	 */
	public static ByteBuffer newDirectByteBuffer(int capacity) {
		return new ReadWriteDirectByteBuffer(capacity);
	}

	/**
	 * Returns a new double buffer based on the specified double array.
	 * 
	 * @param array
	 *            The double array
	 * @return A new double buffer based on the specified double array.
	 */
	public static DoubleBuffer newDoubleBuffer(double array[]) {
		return new ReadWriteDoubleArrayBuffer(array);
	}

	/**
	 * Returns a new array based double buffer with the specified capacity.
	 * 
	 * @param capacity
	 *            The capacity of the new buffer
	 * @return A new array based double buffer with the specified capacity.
	 */
	public static DoubleBuffer newDoubleBuffer(int capacity) {
		return new ReadWriteDoubleArrayBuffer(capacity);
	}

	/**
	 * Returns a new float buffer based on the specified float array.
	 * 
	 * @param array
	 *            The float array
	 * @return A new float buffer based on the specified float array.
	 */
	public static FloatBuffer newFloatBuffer(float array[]) {
		return new ReadWriteFloatArrayBuffer(array);
	}

	/**
	 * Returns a new array based float buffer with the specified capacity.
	 * 
	 * @param capacity
	 *            The capacity of the new buffer
	 * @return A new array based float buffer with the specified capacity.
	 */
	public static FloatBuffer newFloatBuffer(int capacity) {
		return new ReadWriteFloatArrayBuffer(capacity);
	}

	/**
	 * Returns a new array based int buffer with the specified capacity.
	 * 
	 * @param capacity
	 *            The capacity of the new buffer
	 * @return A new array based int buffer with the specified capacity.
	 */
	public static IntBuffer newIntBuffer(int capacity) {
		return new ReadWriteIntArrayBuffer(capacity);
	}

	/**
	 * Returns a new int buffer based on the specified int array.
	 * 
	 * @param array
	 *            The int array
	 * @return A new int buffer based on the specified int array.
	 */
	public static IntBuffer newIntBuffer(int array[]) {
		return new ReadWriteIntArrayBuffer(array);
	}

	/**
	 * Returns a new array based long buffer with the specified capacity.
	 * 
	 * @param capacity
	 *            The capacity of the new buffer
	 * @return A new array based long buffer with the specified capacity.
	 */
	public static LongBuffer newLongBuffer(int capacity) {
		return new ReadWriteLongArrayBuffer(capacity);
	}

	/**
	 * Returns a new long buffer based on the specified long array.
	 * 
	 * @param array
	 *            The long array
	 * @return A new long buffer based on the specified long array.
	 */
	public static LongBuffer newLongBuffer(long array[]) {
		return new ReadWriteLongArrayBuffer(array);
	}

	/**
	 * Returns a new byte buffer mapped to the specified region of file.
	 * 
	 * @param mappedFile
	 *            The file to be mapped
	 * @param offset
	 *            The offset of the region
	 * @param size
	 *            The size of the region
	 * @param mapMode
	 *            The map mode
	 * @return A new byte buffer mapped to the specified region of file.
	 */
	public static MappedByteBuffer newMappedByteBuffer(File mappedFile,
			long offset, int size, MapMode mapMode) {
		return new MappedToByteBufferAdapter(mappedFile, offset, size, mapMode);
	}

	/**
	 * Returns a new array based short buffer with the specified capacity.
	 * 
	 * @param capacity
	 *            The capacity of the new buffer
	 * @return A new array based short buffer with the specified capacity.
	 */
	public static ShortBuffer newShortBuffer(int capacity) {
		return new ReadWriteShortArrayBuffer(capacity);
	}

	/**
	 * Returns a new short buffer based on the specified short array.
	 * 
	 * @param array
	 *            The short array
	 * @return A new short buffer based on the specified short array.
	 */
	public static ShortBuffer newShortBuffer(short array[]) {
		return new ReadWriteShortArrayBuffer(array);
	}

}
