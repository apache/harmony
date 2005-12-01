/* Copyright 1998, 2004 The Apache Software Foundation or its licensors, as applicable
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

package java.io;


/**
 * Writer is an Abstract class for writing Character Streams. Subclasses of
 * writer must implement the methods <code>write(char[], int, int)</code>,
 * <code>close()</code> and <code>flush()</code>.
 * 
 * @see Reader
 */
public abstract class Writer {
	/** The object used to syncronize access to the writer. */
	protected Object lock;

	/**
	 * Constructs a new character stream Writer using <code>this</code> as the
	 * Object to synchronize critical regions around.
	 * 
	 */
	protected Writer() {
		super();
		lock = this;
	}

	/**
	 * Constructs a new character stream Writer using <code>lock</code> as the
	 * Object to synchronize critical regions around.
	 * 
	 * @param lock
	 *            the Object to synchronize critical regions around.
	 */
	protected Writer(Object lock) {
		if (lock != null)
			this.lock = lock;
		else
			throw new NullPointerException();
	}

	/**
	 * Close this Writer. This must be implemented by any concrete subclasses.
	 * The implementation should free any resources associated with the Writer.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to close this Writer.
	 */
	public abstract void close() throws IOException;

	/**
	 * Flush this Writer. This must be implemented by any concrete subclasses.
	 * The implementation should ensure all buffered characters are written out.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to flush this Writer.
	 */
	public abstract void flush() throws IOException;

	/**
	 * Writes the entire character buffer <code>buf</code> to this Writer.
	 * 
	 * @param buf
	 *            the non-null array containing characters to write.
	 * 
	 * @throws IOException
	 *             If this Writer has already been closed or some other
	 *             IOException occurs.
	 */
	public void write(char buf[]) throws IOException {
		write(buf, 0, buf.length);
	}

	/**
	 * Writes <code>count</code> characters starting at <code>offset<code> in
	 * <code>buf</code> to this Writer.  This abstract method must be implemented
	 * by concrete subclasses.
	 *
	 * @param 		buf			the non-null array containing characters to write.
	 * @param 		offset 		offset in buf to retrieve characters
	 * @param 		count 		maximum number of characters to write
	 *
	 * @throws 		IOException 					If this Writer has already been closed or some other IOException occurs.
	 * @throws		ArrayIndexOutOfBoundsException 	If offset or count are outside of bounds.
	 */
	public abstract void write(char buf[], int offset, int count)
			throws IOException;

	/**
	 * Writes the specified character <code>oneChar</code> to this Writer.
	 * This implementation writes the low order two bytes of
	 * <code>oneChar</code> to the Stream.
	 * 
	 * @param oneChar
	 *            The character to write
	 * 
	 * @throws IOException
	 *             If this Writer has already been closed or some other
	 *             IOException occurs.
	 */
	public void write(int oneChar) throws IOException {
		synchronized (lock) {
			char oneCharArray[] = new char[1];
			oneCharArray[0] = (char) oneChar;
			write(oneCharArray);
		}
	}

	/**
	 * Writes the characters from the String <code>str</code> to this Writer.
	 * 
	 * @param str
	 *            the non-null String containing the characters to write.
	 * 
	 * @throws IOException
	 *             If this Writer has already been closed or some other
	 *             IOException occurs.
	 */
	public void write(String str) throws IOException {
		char buf[] = new char[str.length()];
		str.getChars(0, buf.length, buf, 0);
		write(buf);
	}

	/**
	 * Writes <code>count</code> number of characters starting at
	 * <code>offset</code> from the String <code>str</code> to this Writer.
	 * 
	 * @param str
	 *            the non-null String containing the characters to write.
	 * @param offset
	 *            the starting point to retrieve characters.
	 * @param count
	 *            the number of characters to retrieve and write.
	 * 
	 * @throws IOException
	 *             If this Writer has already been closed or some other
	 *             IOException occurs.
	 * @throws ArrayIndexOutOfBoundsException
	 *             If offset or count are outside of bounds.
	 */
	public void write(String str, int offset, int count) throws IOException {
		if (count >= 0) { // other cases tested by getChars()
			char buf[] = new char[count];
			str.getChars(offset, offset + count, buf, 0);

			synchronized (lock) {
				write(buf);
			}
		} else
			throw new StringIndexOutOfBoundsException();
	}
}
