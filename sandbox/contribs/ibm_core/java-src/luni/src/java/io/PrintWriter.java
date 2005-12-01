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


import java.security.AccessController;

import com.ibm.oti.util.PriviAction;

/**
 * PrintWriter is a class which takes either an OutputStream or Writer and
 * provides convenience methods for printing common data types in a human
 * readable format on the stream. No IOExceptions are thrown by this class.
 * Instead, callers should call checkError() to see if a problem has been
 * encountered in this Writer.
 * 
 */
public class PrintWriter extends Writer {
	/**
	 * The writer to output data to.
	 */
	protected Writer out;

	/**
	 * indicates whether or not this PrintWriter has incurred an error.
	 */
	boolean ioError = false;

	/**
	 * indicates whether or not this PrintWriter should flush its contents after
	 * printing a new line.
	 */
	boolean autoflush = false;

	private final String lineSeparator = (String) AccessController
			.doPrivileged(new PriviAction("line.separator")); //$NON-NLS-1$

	/**
	 * Constructs a new PrintWriter on the OutputStream <code>out</code>. All
	 * writes to the target can now take place through this PrintWriter. By
	 * default, the PrintWriter is set to not autoflush when println() is
	 * called.
	 * 
	 * @param out
	 *            the the OutputStream to provide convenience methods on.
	 */
	public PrintWriter(OutputStream out) {
		this(new OutputStreamWriter(out), false);
	}

	/**
	 * Constructs a new PrintWriter on the OutputStream <code>out</code>. All
	 * writes to the target can now take place through this PrintWriter. By
	 * default, the PrintWriter is set to not autoflush when println() is
	 * called.
	 * 
	 * @param out
	 *            the the OutputStream to provide convenience methods on.
	 * @param autoflush
	 *            whether to flush when println() is called.
	 * 
	 */
	public PrintWriter(OutputStream out, boolean autoflush) {
		this(new OutputStreamWriter(out), autoflush);
	}

	/**
	 * Constructs a new PrintWriter on the Writer <code>wr</code>. All writes
	 * to the target can now take place through this PrintWriter. By default,
	 * the PrintWriter is set to not autoflush when println() is called.
	 * 
	 * @param wr
	 *            the Writer to provide convenience methods on.
	 * 
	 */
	public PrintWriter(Writer wr) {
		this(wr, false);
	}

	/**
	 * Constructs a new PrintWriter on the Writer <code>wr</code>. All writes
	 * to the target can now take place through this PrintWriter. By default,
	 * the PrintWriter is set to not autoflush when println() is called.
	 * 
	 * @param wr
	 *            the Writer to provide convenience methods on.
	 * @param autoflush
	 *            whether to flush when println() is called.
	 * 
	 */
	public PrintWriter(Writer wr, boolean autoflush) {
		super(wr);
		this.autoflush = autoflush;
		out = wr;
	}

	/**
	 * Answers a boolean indicating whether or not this PrintWriter has
	 * encountered an error. If so, the receiver should probably be closed since
	 * futher writes will not actually take place. A side effect of calling
	 * checkError is that the target Writer is flushed.
	 * 
	 * @return boolean has an error occurred in this PrintWriter.
	 */
	public boolean checkError() {
		if (out != null)
			flush();
		return ioError;
	}

	/**
	 * Close this PrintWriter. This implementation flushes and then closes the
	 * target writer. If an error occurs, set an error in this PrintWriter to
	 * <code>true</code>.
	 * 
	 */
	public void close() {
		synchronized (lock) {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					setError();
				}
				out = null;
			}
		}
	}

	/**
	 * Flush this PrintWriter to ensure all pending data is sent out to the
	 * target Writer. This implementation flushes the target writer. If an error
	 * occurs, set an error in this PrintWriter to <code>true</code>.
	 */
	public void flush() {
		synchronized (lock) {
			if (out != null) {
				try {
					out.flush();
				} catch (IOException e) {
					setError();
				}
			} else {
				setError();
			}
		}
	}

	private void newline() {
		print(lineSeparator);
		if (autoflush)
			flush();
	}

	/**
	 * Prints the String representation of the character array parameter
	 * <code>charArray</code> to the target Writer.
	 * 
	 * @param charArray
	 *            the character array to print on this Writer.
	 */
	public void print(char[] charArray) {
		print(new String(charArray, 0, charArray.length));
	}

	/**
	 * Prints the String representation of the character parameter
	 * <code>ch</code> to the target Writer.
	 * 
	 * @param ch
	 *            the character to print on this Writer.
	 */
	public void print(char ch) {
		print(String.valueOf(ch));
	}

	/**
	 * Prints the String representation of the <code>double</code> parameter
	 * <code>dnum</code> to the target Writer.
	 * 
	 * @param dnum
	 *            the <code>double</code> to print on this Writer.
	 */
	public void print(double dnum) {
		print(String.valueOf(dnum));
	}

	/**
	 * Prints the String representation of the <code>float</code> parameter
	 * <code>fnum</code> to the target Writer.
	 * 
	 * @param fnum
	 *            the <code>float</code> to print on this Writer.
	 */
	public void print(float fnum) {
		print(String.valueOf(fnum));
	}

	/**
	 * Prints the String representation of the <code>int</code> parameter
	 * <code>inum</code> to the target Writer.
	 * 
	 * @param inum
	 *            the <code>int</code> to print on this Writer.
	 */
	public void print(int inum) {
		print(String.valueOf(inum));
	}

	/**
	 * Prints the String representation of the <code>long</code> parameter
	 * <code>lnum</code> to the target Writer.
	 * 
	 * @param lnum
	 *            the <code>long</code> to print on this Writer.
	 */
	public void print(long lnum) {
		print(String.valueOf(lnum));
	}

	/**
	 * Prints the String representation of the Object parameter <code>obj</code>
	 * to the target Writer.
	 * 
	 * @param obj
	 *            the Object to print on this Writer.
	 */
	public void print(Object obj) {
		print(String.valueOf(obj));
	}

	/**
	 * Prints the String representation of the <code>String</code> parameter
	 * <code>str</code> to the target Writer.
	 * 
	 * @param str
	 *            the <code>String</code> to print on this Writer.
	 */
	public void print(String str) {
		write(str != null ? str : String.valueOf((Object) null));
	}

	/**
	 * Prints the String representation of the <code>boolean</code> parameter
	 * <code>bool</code> to the target Writer.
	 * 
	 * @param bool
	 *            the <code>boolean</code> to print on this Writer.
	 */
	public void print(boolean bool) {
		print(String.valueOf(bool));
	}

	/**
	 * Prints the String representation of the System property
	 * <code>"line.separator"</code> to the target Writer.
	 */
	public void println() {
		synchronized (lock) {
			newline();
		}
	}

	/**
	 * Prints the String representation of the character array parameter
	 * <code>charArray</code> to the target Writer followed by the System
	 * property <code>"line.separator"</code>.
	 * 
	 * @param charArray
	 *            the character array to print on this Writer.
	 */
	public void println(char[] charArray) {
		println(new String(charArray, 0, charArray.length));
	}

	/**
	 * Prints the String representation of the character parameter
	 * <code>ch</code> to the target Writer followed by the System property
	 * <code>"line.separator"</code>.
	 * 
	 * @param ch
	 *            the character to print on this Writer.
	 */
	public void println(char ch) {
		println(String.valueOf(ch));
	}

	/**
	 * Prints the String representation of the <code>double</code> parameter
	 * <code>dnum</code> to the target Writer followed by the System property
	 * <code>"line.separator"</code>.
	 * 
	 * @param dnum
	 *            the double to print on this Writer.
	 */
	public void println(double dnum) {
		println(String.valueOf(dnum));
	}

	/**
	 * Prints the String representation of the <code>float</code> parameter
	 * <code>fnum</code> to the target Writer followed by the System property
	 * <code>"line.separator"</code>.
	 * 
	 * @param fnum
	 *            the float to print on this Writer.
	 */
	public void println(float fnum) {
		println(String.valueOf(fnum));
	}

	/**
	 * Prints the String representation of the <code>int</code> parameter
	 * <code>inum</code> to the target Writer followed by the System property
	 * <code>"line.separator"</code>.
	 * 
	 * @param inum
	 *            the int to print on this Writer.
	 */
	public void println(int inum) {
		println(String.valueOf(inum));
	}

	/**
	 * Prints the String representation of the <code>long</code> parameter
	 * <code>lnum</code> to the target Writer followed by the System property
	 * <code>"line.separator"</code>.
	 * 
	 * @param lnum
	 *            the long to print on this Writer.
	 */
	public void println(long lnum) {
		println(String.valueOf(lnum));
	}

	/**
	 * Prints the String representation of the <code>Object</code> parameter
	 * <code>obj</code> to the target Writer followed by the System property
	 * <code>"line.separator"</code>.
	 * 
	 * @param obj
	 *            the <code>Object</code> to print on this Writer.
	 */
	public void println(Object obj) {
		println(String.valueOf(obj));
	}

	/**
	 * Prints the String representation of the <code>String</code> parameter
	 * <code>str</code> to the target Writer followed by the System property
	 * <code>"line.separator"</code>.
	 * 
	 * @param str
	 *            the <code>String</code> to print on this Writer.
	 */
	public void println(String str) {
		synchronized (lock) {
			print(str);
			newline();
		}
	}

	/**
	 * Prints the String representation of the <code>boolean</code> parameter
	 * <code>bool</code> to the target Writer followed by the System property
	 * <code>"line.separator"</code>.
	 * 
	 * @param bool
	 *            the boolean to print on this Writer.
	 */
	public void println(boolean bool) {
		println(String.valueOf(bool));
	}

	/**
	 * Set the flag indicating that this PrintWriter has encountered an IO
	 * error.
	 */
	protected void setError() {
		synchronized (lock) {
			ioError = true;
		}
	}

	/**
	 * Writes the entire character buffer
	 * 
	 * @buf to this Writer.
	 * 
	 * @param buf
	 *            the non-null array containing characters to write.
	 */
	public void write(char buf[]) {
		write(buf, 0, buf.length);
	}

	/**
	 * Writes <code>count</code> characters starting at <code>offset</code>
	 * in <code>buf<code>
	 * to this Writer.
	 *
	 * @param 		buf			the non-null array containing characters to write.
	 * @param 		offset 		offset in buf to retrieve characters
	 * @param 		count 		maximum number of characters to write
	 *
	 * @throws		ArrayIndexOutOfBoundsException 	If offset or count are outside of bounds.
	 */
	public void write(char buf[], int offset, int count) {
		doWrite(buf, offset, count);
	}

	/**
	 * Writes the specified character to this Writer. This implementation writes
	 * the low order two bytes to the target writer.
	 * 
	 * @param oneChar
	 *            The character to write
	 */
	public void write(int oneChar) {
		doWrite(new char[] { (char) oneChar }, 0, 1);
	}
	
	private final void doWrite(char buf[], int offset, int count) {
		synchronized (lock) {
			if (out != null) {
				try {
					out.write(buf, offset, count);
				} catch (IOException e) {
					setError();
				}
			} else {
				setError();
			}
		}
	}
	
	/**
	 * Writes the characters from the String <code>str</code> to this Writer.
	 * 
	 * @param str
	 *            the non-null String containing the characters to write.
	 */
	public void write(String str) {
		write(str.toCharArray());
	}

	/**
	 * Writes <code>count</code> characters from the String <code>str</code>
	 * starting at <code>offset</code> to this Writer.
	 * 
	 * @param str
	 *            the non-null String containing the characters to write.
	 * @param offset
	 *            where in <code>str</code> to get chars from.
	 * @param count
	 *            how many characters to write.
	 * 
	 * @throws ArrayIndexOutOfBoundsException
	 *             If offset or count are outside of bounds.
	 */
	public void write(String str, int offset, int count) {
		write(str.substring(offset, offset + count).toCharArray());
	}
}
