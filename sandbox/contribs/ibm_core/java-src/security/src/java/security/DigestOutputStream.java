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

package java.security;


import java.io.IOException;

/**
 * This class implements a stream that computes a message digest hash as the
 * bytes are written to it.
 * 
 */

public class DigestOutputStream extends java.io.FilterOutputStream {

	/**
	 * The digest to use when computing the hash.
	 */
	protected MessageDigest digest;

	/**
	 * True if the digest should be computed for the next chunck of bytes
	 * written.
	 */
	private boolean on;

	/**
	 * Constructs a new DigestOutputStream on an existing stream and with the
	 * given MessageDigest.
	 * 
	 * 
	 * 
	 * @param out
	 *            java.io.OutputStream where the bytes will be written to.
	 * @param digest
	 *            MessageDigest digest to use when computing the hash.
	 * 
	 * @see #on
	 * @see MessageDigest
	 */
	public DigestOutputStream(java.io.OutputStream out, MessageDigest digest) {
		super(out);
		setMessageDigest(digest);
		on(true);
	}

	/**
	 * Answers the MessageDigest which the receiver uses when computing the
	 * hash.
	 * 
	 * 
	 * @return MessageDigest the digest the receiver uses when computing the
	 *         hash.
	 */

	public MessageDigest getMessageDigest() {
		return digest;
	}

	/**
	 * Enables or disables the digest function (default is on).
	 * 
	 * 
	 * @param on
	 *            boolean true if the digest should be computed, and false
	 *            otherwise.
	 * 
	 * @see MessageDigest
	 */
	public void on(boolean on) {
		this.on = on;
	}

	/**
	 * Sets the MessageDigest which the receiver will use when computing the
	 * hash.
	 * 
	 * 
	 * @param digest
	 *            MessageDigest the digest to use when computing the hash.
	 * 
	 * @see MessageDigest
	 * @see #on
	 */
	public void setMessageDigest(MessageDigest digest) {
		this.digest = digest;
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * 
	 * @return String a printable representation for the receiver.
	 */
	public String toString() {
		StringBuffer answer = new StringBuffer("DigestOutputStream");
		if (digest != null) {
			answer.append(" : ");
			answer.append(digest.toString());
		}
		if (on)
			answer.append(" : (digest on)");
		else
			answer.append(" : (digest off)");
		return answer.toString();
	}

	/**
	 * Writes <code>length</code> bytes from the byte array
	 * <code>buffer</code> starting at <code>offset</code> and updates the
	 * message digest hash if this function is enabled.
	 * 
	 * 
	 * @param buffer
	 *            byte[] the buffer to be written.
	 * @param offset
	 *            int offset in buffer to begin writing.
	 * @param length
	 *            int number of bytes to write.
	 * 
	 * @exception java.io.IOException
	 *                If an error occurs attempting to write to this stream.
	 * @exception java.lang.IndexOutOfBoundsException
	 *                If offset or count are outside of bounds.
	 * 
	 * @see MessageDigest
	 * @see #on
	 */
	public void write(byte[] buffer, int offset, int length) throws IOException {
		super.write(buffer, offset, length);
	}

	/**
	 * Writes a single byte to the receiver and updates the message digest hash
	 * if this function is enabled.
	 * 
	 * 
	 * @param oneByte
	 *            int the byte to be written.
	 * 
	 * @exception java.io.IOException
	 *                If an error occurs attempting to write to the underlying
	 *                stream.
	 */
	public void write(int oneByte) throws IOException {
		super.write(oneByte);
		if (on)
			digest.engineUpdate((byte) oneByte);
	}

}
