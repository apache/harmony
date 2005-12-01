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


import java.nio.charset.Charset;
import java.security.AccessController;

import com.ibm.oti.util.PriviAction;

/**
 * PrintStream is a class which takes an OutputStream and provides convenience
 * methods for printing common data types in a human readable format on the
 * stream. This is not to be confused with DataOutputStream which is used for
 * encoding common datatypes so that they can be read back in. No IOExceptions
 * are thrown by this class. Instead, callers should call checkError() to see if
 * a problem has been encountered in this Stream.
 * 
 */
public class PrintStream extends FilterOutputStream {

	/**
	 * protect writes to the underlying stream.
	 */
	private Object lock = new Object();

	/**
	 * indicates whether or not this PrintStream has incurred an error.
	 */
	boolean ioError = false;

	/**
	 * indicates whether or not this PrintStream should flush its contents after
	 * printing a new line.
	 */
	boolean autoflush = false;

	private String encoding;

	private final String lineSeparator = (String) AccessController
			.doPrivileged(new PriviAction("line.separator")); //$NON-NLS-1$

	/**
	 * Constructs a new PrintStream on the OutputStream <code>out</code>. All
	 * writes to the target can now take place through this PrintStream. By
	 * default, the PrintStream is set to not autoflush when a newline is
	 * encountered.
	 * 
	 * @param out
	 *            the OutputStream to provide convenience methods on.
	 */
	public PrintStream(OutputStream out) {
		super(out);
		if (out == null)
			throw new NullPointerException();
	}

	/**
	 * Constructs a new PrintStream on the OutputStream <code>out</code>. All
	 * writes to the target can now take place through this PrintStream. The
	 * PrintStream is set to not autoflush if <code>autoflush</code> is
	 * <code>true</code>.
	 * 
	 * @param out
	 *            the OutputStream to provide convenience methods on.
	 * @param autoflush
	 *            indicates whether or not to flush contents upon encountering a
	 *            newline sequence.
	 */
	public PrintStream(OutputStream out, boolean autoflush) {
		super(out);
		if (out == null)
			throw new NullPointerException();
		this.autoflush = autoflush;
	}

	/**
	 * Constructs a new PrintStream on the OutputStream <code>out</code>. All
	 * writes to the target can now take place through this PrintStream. The
	 * PrintStream is set to not autoflush if <code>autoflush</code> is
	 * <code>true</code>.
	 * 
	 * @param out
	 *            the OutputStream to provide convenience methods on.
	 * @param autoflush
	 *            indicates whether or not to flush contents upon encountering a
	 *            newline sequence.
	 * @param enc
	 *            the non-null String describing the desired character encoding.
	 * 
	 * @throws UnsupportedEncodingException
	 *             If the chosen encoding is not supported
	 */
	public PrintStream(OutputStream out, boolean autoflush, String enc)
			throws UnsupportedEncodingException {
		super(out);
		if (out == null)
			throw new NullPointerException();
		this.autoflush = autoflush;
		if (!Charset.isSupported(enc))
			throw new UnsupportedEncodingException(enc);
		encoding = enc;
	}

	/**
	 * Answers a boolean indicating whether or not this PrintStream has
	 * encountered an error. If so, the receiver should probably be closed since
	 * futher writes will not actually take place. A side effect of calling
	 * checkError is that the target OutputStream is flushed.
	 * 
	 * @return <code>true</code> if an error occurred in this PrintStream,
	 *         <code>false</code> otherwise.
	 */
	public boolean checkError() {
		if (out != null)
			flush();
		return ioError;
	}

	/**
	 * Close this PrintStream. This implementation flushes and then closes the
	 * target stream. If an error occurs, set an error in this PrintStream to
	 * <code>true</code>.
	 * 
	 */
	public void close() {
		synchronized (lock) {
			flush();
			if (out != null) {
				try {
					out.close();
					out = null;
				} catch (IOException e) {
					setError();
				}
			}
		}
	}

	/**
	 * Flush this PrintStream to ensure all pending data is sent out to the
	 * target OutputStream. This implementation flushes the target OutputStream.
	 * If an error occurs, set an error in this PrintStream to <code>true</code>.
	 * 
	 */
	public void flush() {
		synchronized (lock) {
			if (out != null) {
				try {
					out.flush();
					return;
				} catch (IOException e) {
				}
			}
		}
		setError();
	}

	private void newline() {
		print(lineSeparator);
	}

	/**
	 * Prints the String representation of the character array parameter
	 * <code>charArray</code> to the target OutputStream.
	 * 
	 * @param charArray
	 *            the character array to print on this PrintStream.
	 */
	public void print(char[] charArray) {
		print(new String(charArray, 0, charArray.length));
	}

	/**
	 * Prints the String representation of the character parameter
	 * <code>ch</code> to the target OutputStream.
	 * 
	 * @param ch
	 *            the character to print on this PrintStream.
	 */
	public void print(char ch) {
		print(String.valueOf(ch));
	}

	/**
	 * Prints the String representation of the <code>double</code> parameter
	 * <code>dnum</code> to the target OutputStream.
	 * 
	 * @param dnum
	 *            the <code>double</code> to print on this PrintStream.
	 */
	public void print(double dnum) {
		print(String.valueOf(dnum));
	}

	/**
	 * Prints the String representation of the <code>float</code> parameter
	 * <code>fnum</code> to the target OutputStream.
	 * 
	 * @param fnum
	 *            the <code>float</code> to print on this PrintStream.
	 */
	public void print(float fnum) {
		print(String.valueOf(fnum));
	}

	/**
	 * Obtains the <code>int</code> argument as a <code>String</code> and
	 * prints it to the target {@link OutputStream}.
	 * 
	 * @param inum
	 *            the <code>int</code> to print on this PrintStream.
	 */
	public void print(int inum) {
		print(String.valueOf(inum));
	}

	/**
	 * Prints the String representation of the <code>long</code> parameter
	 * <code>lnum</code> to the target OutputStream.
	 * 
	 * @param lnum
	 *            the <code>long</code> to print on this PrintStream.
	 */
	public void print(long lnum) {
		print(String.valueOf(lnum));
	}

	/**
	 * Prints the String representation of the Object parameter <code>obj</code>
	 * to the target OutputStream.
	 * 
	 * @param obj
	 *            the Object to print on this PrintStream.
	 */
	public void print(Object obj) {
		print(String.valueOf(obj));
	}

	/**
	 * Prints the String representation of the <code>String</code> parameter
	 * <code>str</code> to the target OutputStream.
	 * 
	 * @param str
	 *            the <code>String</code> to print on this PrintStream.
	 */
	public void print(String str) {
		synchronized (lock) {
			if (out == null) {
				setError();
				return;
			}
			if (str == null) {
				print("null"); //$NON-NLS-1$
				return;
			}

			try {
				if (encoding == null)
					write(str.getBytes());
				else
					write(str.getBytes(encoding));
			} catch (IOException e) {
				setError();
			}
		}
	}

	/**
	 * Prints the String representation of the <code>boolean</code> parameter
	 * <code>bool</code> to the target OutputStream.
	 * 
	 * @param bool
	 *            the <code>boolean</code> to print on this PrintStream.
	 */
	public void print(boolean bool) {
		print(String.valueOf(bool));
	}

	/**
	 * Prints the String representation of the System property
	 * <code>"line.separator"</code> to the target OutputStream.
	 * 
	 */
	public void println() {
		newline();
	}

	/**
	 * Prints the String representation of the character array parameter
	 * <code>charArray</code> to the target OutputStream followed by the
	 * System property <code>"line.separator"</code>.
	 * 
	 * @param charArray
	 *            the character array to print on this PrintStream.
	 */
	public void println(char[] charArray) {
		println(new String(charArray, 0, charArray.length));
	}

	/**
	 * Prints the String representation of the character parameter
	 * <code>ch</code> to the target OutputStream followed by the System
	 * property <code>"line.separator"</code>.
	 * 
	 * @param ch
	 *            the character to print on this PrintStream.
	 */
	public void println(char ch) {
		println(String.valueOf(ch));
	}

	/**
	 * Prints the String representation of the <code>double</code> parameter
	 * <code>dnum</code> to the target OutputStream followed by the System
	 * property <code>"line.separator"</code>.
	 * 
	 * @param dnum
	 *            the double to print on this PrintStream.
	 */
	public void println(double dnum) {
		println(String.valueOf(dnum));
	}

	/**
	 * Prints the String representation of the <code>float</code> parameter
	 * <code>fnum</code> to the target OutputStream followed by the System
	 * property <code>"line.separator"</code>.
	 * 
	 * @param fnum
	 *            the float to print on this PrintStream.
	 */
	public void println(float fnum) {
		println(String.valueOf(fnum));
	}

	/**
	 * Obtains the <code>int</code> argument as a <code>String</code> and
	 * prints it to the target {@link OutputStream} followed by the System
	 * property <code>"line.separator"</code>.
	 * 
	 * @param inum
	 *            the int to print on this PrintStream.
	 */
	public void println(int inum) {
		println(String.valueOf(inum));
	}

	/**
	 * Prints the String representation of the <code>long</code> parameter
	 * <code>lnum</code> to the target OutputStream followed by the System
	 * property <code>"line.separator"</code>.
	 * 
	 * @param lnum
	 *            the long to print on this PrintStream.
	 */
	public void println(long lnum) {
		println(String.valueOf(lnum));
	}

	/**
	 * Prints the String representation of the <code>Object</code> parameter
	 * <code>obj</code> to the target OutputStream followed by the System
	 * property <code>"line.separator"</code>.
	 * 
	 * @param obj
	 *            the <code>Object</code> to print on this PrintStream.
	 */
	public void println(Object obj) {
		println(String.valueOf(obj));
	}

	/**
	 * Prints the String representation of the <code>String</code> parameter
	 * <code>str</code> to the target OutputStream followed by the System
	 * property <code>"line.separator"</code>.
	 * 
	 * @param str
	 *            the <code>String</code> to print on this PrintStream.
	 */
	public void println(String str) {
		synchronized (lock) {
			print(str);
			newline();
		}
	}

	/**
	 * Prints the String representation of the <code>boolean</code> parameter
	 * <code>bool</code> to the target OutputStream followed by the System
	 * property <code>"line.separator"</code>.
	 * 
	 * @param bool
	 *            the boolean to print on this PrintStream.
	 */
	public void println(boolean bool) {
		println(String.valueOf(bool));
	}

	protected void setError() {
		ioError = true;
	}

	/**
	 * Writes <code>count</code> <code>bytes</code> from the byte array
	 * <code>buffer</code> starting at <code>offset</code> to this
	 * PrintStream. This implementation writes the <code>buffer</code> to the
	 * target OutputStream and if this PrintStream is set to autoflush, flushes
	 * it. If an error occurs, set an error in this PrintStream to
	 * <code>true</code>.
	 * 
	 * @param buffer
	 *            the buffer to be written
	 * @param offset
	 *            offset in buffer to get bytes
	 * @param count
	 *            number of bytes in buffer to write
	 * 
	 * @throws IndexOutOfBoundsException
	 *             If offset or count are outside of bounds.
	 */
	public void write(byte[] buffer, int offset, int count) {
		if (buffer != null) {
			// avoid int overflow
			if (0 <= offset && offset <= buffer.length && 0 <= count
					&& count <= buffer.length - offset) {
				synchronized (lock) {
					if (out == null) {
						setError();
						return;
					}
					try {
						out.write(buffer, offset, count);
						if (autoflush)
							flush();
					} catch (IOException e) {
						setError();
					}
				}
			} else
				throw new ArrayIndexOutOfBoundsException(com.ibm.oti.util.Msg
						.getString("K002f")); //$NON-NLS-1$
		} else
			throw new NullPointerException();
	}

	/**
	 * Writes the specified byte <code>oneByte</code> to this PrintStream.
	 * Only the low order byte of <code>oneByte</code> is written. This
	 * implementation writes <code>oneByte</code> to the target OutputStream.
	 * If <code>oneByte</code> is equal to the character <code>'\n'</code>
	 * and this PrintSteam is set to autoflush, the target OutputStream is
	 * flushed.
	 * 
	 * @param oneByte
	 *            the byte to be written
	 */
	public void write(int oneByte) {
		synchronized (lock) {
			if (out == null) {
				setError();
				return;
			}
			try {
				out.write(oneByte);
				if (autoflush && (oneByte & 0xFF) == '\n')
					flush();
			} catch (IOException e) {
				setError();
			}
		}
	}
}
