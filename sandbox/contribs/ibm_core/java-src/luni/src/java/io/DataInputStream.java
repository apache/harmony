/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
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
 * DataInputStream is a filter class which can read typed data from a Stream.
 * Typically, this stream has been written by a DataOutputStream. Types that can
 * be read include byte, 16-bit short, 32-bit int, 32-bit float, 64-bit long,
 * 64-bit double, byte strings, and UTF Strings.
 * 
 * @see DataOutputStream
 */
public class DataInputStream extends FilterInputStream implements DataInput {
	/**
	 * Constructs a new DataInputStream on the InputStream <code>in</code>.
	 * All reads can now be filtered through this stream. Note that data read by
	 * this Stream is not in a human readable format and was most likely created
	 * by a DataOutputStream.
	 * 
	 * @param in
	 *            the target InputStream to filter reads on.
	 * 
	 * @see DataOutputStream
	 * @see RandomAccessFile
	 */
	public DataInputStream(InputStream in) {
		super(in);
	}

	/**
	 * Reads bytes from the source stream into the byte array
	 * <code>buffer</code>. The number of bytes actually read is returned.
	 * 
	 * @param buffer
	 *            the buffer to read bytes into
	 * @return the number of bytes actually read or -1 if end of stream.
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#write(byte[])
	 * @see DataOutput#write(byte[], int, int)
	 */
	public final int read(byte[] buffer) throws IOException {
		return in.read(buffer, 0, buffer.length);
	}

	/**
	 * Read at most <code>length</code> bytes from this DataInputStream and
	 * stores them in byte array <code>buffer</code> starting at
	 * <code>offset</code>. Answer the number of bytes actually read or -1 if
	 * no bytes were read and end of stream was encountered.
	 * 
	 * @param buffer
	 *            the byte array in which to store the read bytes.
	 * @param offset
	 *            the offset in <code>buffer</code> to store the read bytes.
	 * @param length
	 *            the maximum number of bytes to store in <code>buffer</code>.
	 * @return the number of bytes actually read or -1 if end of stream.
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#write(byte[])
	 * @see DataOutput#write(byte[], int, int)
	 */
	public final int read(byte[] buffer, int offset, int length)
			throws IOException {
		return in.read(buffer, offset, length);
	}

	/**
	 * Reads a boolean from this stream.
	 * 
	 * @return the next boolean value from the source stream.
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#writeBoolean(boolean)
	 */
	public final boolean readBoolean() throws IOException {
		int temp = in.read();
		if (temp >= 0)
			return temp != 0;
		throw new EOFException();
	}

	/**
	 * Reads an 8-bit byte value from this stream.
	 * 
	 * @return the next byte value from the source stream.
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#writeByte(int)
	 */
	public final byte readByte() throws IOException {
		int temp = in.read();
		if (temp >= 0)
			return (byte) temp;
		throw new EOFException();
	}

	/**
	 * Reads a 16-bit character value from this stream.
	 * 
	 * @return the next <code>char</code> value from the source stream.
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#writeChar(int)
	 */
	public final char readChar() throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		if ((b1 | b2) >= 0)
			return (char) ((b1 << 8) + b2);
		throw new EOFException();
	}

	/**
	 * Reads a 64-bit <code>double</code> value from this stream.
	 * 
	 * @return the next <code>double</code> value from the source stream.
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#writeDouble(double)
	 */
	public final double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	/**
	 * Reads a 32-bit <code>float</code> value from this stream.
	 * 
	 * @return the next <code>float</code> value from the source stream.
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#writeFloat(float)
	 */
	public final float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	/**
	 * Reads bytes from this stream into the byte array <code>buffer</code>.
	 * This method will block until <code>buffer.length</code> number of bytes
	 * have been read.
	 * 
	 * @param buffer
	 *            to read bytes into
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#write(byte[])
	 * @see DataOutput#write(byte[], int, int)
	 */
	public final void readFully(byte[] buffer) throws IOException {
		readFully(buffer, 0, buffer.length);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.DataInput#readFully(byte[], int, int)
	 */
	public final void readFully(byte[] buffer, int offset, int length)
			throws IOException {
		if (buffer != null) {
			// avoid int overflow
			if (0 <= offset && offset <= buffer.length && 0 <= length
					&& length <= buffer.length - offset) {
				while (length > 0) {
					int result = in.read(buffer, offset, length);
					if (result >= 0) {
						offset += result;
						length -= result;
					} else
						throw new EOFException();
				}
			} else
				throw new IndexOutOfBoundsException();
		} else
			throw new NullPointerException(com.ibm.oti.util.Msg
					.getString("K0047")); //$NON-NLS-1$
	}

	/**
	 * Reads a 32-bit integer value from this stream.
	 * 
	 * @return the next <code>int</code> value from the source stream.
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#writeInt(int)
	 */
	public final int readInt() throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		int b3 = in.read();
		int b4 = in.read();
		if ((b1 | b2 | b3 | b4) >= 0)
			return ((b1 << 24) + (b2 << 16) + (b3 << 8) + b4);
		throw new EOFException();
	}

	/**
	 * Answers a <code>String</code> representing the next line of text
	 * available in this BufferedReader. A line is represented by 0 or more
	 * characters followed by <code>'\n'</code>, <code>'\r'</code>,
	 * <code>"\n\r"</code> or end of stream. The <code>String</code> does
	 * not include the newline sequence.
	 * 
	 * @return the contents of the line or null if no characters were read
	 *         before end of stream.
	 * 
	 * @throws IOException
	 *             If the DataInputStream is already closed or some other IO
	 *             error occurs.
	 * 
	 * @deprecated Use BufferedReader
	 */
	public final String readLine() throws IOException {
		StringBuffer line = new StringBuffer(80); // Typical line length
		boolean foundTerminator = false;
		while (true) {
			int nextByte = in.read();
			switch (nextByte) {
			case -1:
				if (line.length() == 0 && !foundTerminator)
					return null;
				return line.toString();
			case (byte) '\r':
				if (foundTerminator) {
					((PushbackInputStream) in).unread(nextByte);
					return line.toString();
				}
				foundTerminator = true;
				/* Have to be able to peek ahead one byte */
				if (!(in.getClass() == PushbackInputStream.class))
					in = new PushbackInputStream(in);
				break;
			case (byte) '\n':
				return line.toString();
			default:
				if (foundTerminator) {
					((PushbackInputStream) in).unread(nextByte);
					return line.toString();
				}
				line.append((char) nextByte);
			}
		}
	}

	/**
	 * Reads a 64-bit <code>long</code> value from this stream.
	 * 
	 * @return the next <code>long</code> value from the source stream.
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#writeLong(long)
	 */
	public final long readLong() throws IOException {
		int i1 = readInt();
		int b1 = in.read();
		int b2 = in.read();
		int b3 = in.read();
		int b4 = in.read();
		if ((b1 | b2 | b3 | b4) >= 0)
			return (((long) i1) << 32) + ((long) b1 << 24) + (b2 << 16)
					+ (b3 << 8) + b4;
		throw new EOFException();
	}

	/**
	 * Reads a 16-bit <code>short</code> value from this stream.
	 * 
	 * @return the next <code>short</code> value from the source stream.
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#writeShort(int)
	 */
	public final short readShort() throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		if ((b1 | b2) >= 0)
			return (short) ((b1 << 8) + b2);
		throw new EOFException();
	}

	/**
	 * Reads an unsigned 8-bit <code>byte</code> value from this stream and
	 * returns it as an int.
	 * 
	 * @return the next unsigned byte value from the source stream.
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#writeByte(int)
	 */
	public final int readUnsignedByte() throws IOException {
		int temp = in.read();
		if (temp >= 0)
			return temp;
		throw new EOFException();
	}

	/**
	 * Reads a 16-bit unsigned <code>short</code> value from this stream and
	 * returns it as an int.
	 * 
	 * @return the next unsigned <code>short</code> value from the source
	 *         stream.
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#writeShort(int)
	 */
	public final int readUnsignedShort() throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		if ((b1 | b2) >= 0)
			return ((b1 << 8) + b2);
		throw new EOFException();
	}

	/**
	 * Reads a UTF format String from this Stream.
	 * 
	 * @return the next UTF String from the source stream.
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#writeUTF(java.lang.String)
	 */
	public final String readUTF() throws IOException {
		int utfSize = readUnsignedShort();
		return decodeUTF(utfSize);
	}

	static final int MAX_BUF_SIZE = 8192;

	static boolean useShared = true;

	static byte[] byteBuf = new byte[0];

	static char[] charBuf = new char[0];

	String decodeUTF(int utfSize) throws IOException {
		byte[] buf;
		char[] out = null;
		boolean makeBuf = true;
		if (utfSize <= MAX_BUF_SIZE && useShared) {
			synchronized (byteBuf) {
				if (useShared) {
					useShared = false;
					makeBuf = false;
				}
			}
		}
		if (makeBuf) {
			buf = new byte[utfSize];
			out = new char[utfSize];
		} else {
			if (byteBuf.length < utfSize)
				byteBuf = new byte[utfSize];
			if (charBuf.length < utfSize) {
				charBuf = new char[utfSize];
			}
			buf = byteBuf;
			out = charBuf;
		}

		readFully(buf, 0, utfSize);
		String result;
		result = com.ibm.oti.util.Util.convertUTF8WithBuf(buf, out, 0, utfSize);
		if (!makeBuf)
			useShared = true;
		return result;
	}

	/**
	 * Reads a UTF format String from the DataInput Stream <code>in</code>.
	 * 
	 * @param in
	 *            the input stream to read from
	 * @return the next UTF String from the source stream.
	 * 
	 * @throws IOException
	 *             If a problem occurs reading from this DataInputStream.
	 * 
	 * @see DataOutput#writeUTF(java.lang.String)
	 */
	public static final String readUTF(DataInput in) throws IOException {
		return in.readUTF();
	}

	/**
	 * Skips <code>count</code> number of bytes in this stream. Subsequent
	 * <code>read()</code>'s will not return these bytes unless
	 * <code>reset()</code> is used.
	 * 
	 * @param count
	 *            the number of bytes to skip.
	 * @return the number of bytes actually skipped.
	 * 
	 * @throws IOException
	 *             If the stream is already closed or another IOException
	 *             occurs.
	 */
	public final int skipBytes(int count) throws IOException {
		int skipped = 0;
		long skip;
		while (skipped < count && (skip = in.skip(count - skipped)) != 0)
			skipped += skip;
		if (skipped >= 0)
			return skipped;
		throw new EOFException();
	}

}
