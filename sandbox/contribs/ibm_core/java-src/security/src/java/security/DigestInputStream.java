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
 * bytes are read from it.
 * 
 */
public class DigestInputStream extends java.io.FilterInputStream {

	/**
	 * The digest to use when computing the hash.
	 */
	protected MessageDigest digest;

	/**
	 * True if the digest should be computed for the next chunck of bytes read.
	 */
	private boolean on;

	/**
	 * Constructs a new DigestInputStream on an existing stream with the given
	 * MessageDigest.
	 * 
	 * 
	 * 
	 * @param in
	 *            java.io.InputStream source of the bytes to digest.
	 * @param digest
	 *            java.security.MessageDigest digest to use when computing the
	 *            hash.
	 * 
	 * @see #on
	 * @see MessageDigest
	 */
	public DigestInputStream(java.io.InputStream in, MessageDigest digest) {
		super(in);
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
	 * 
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
	 * Reads the next byte and answers it as an int. Updates the digest for the
	 * byte if this fuction is enabled.
	 * 
	 * 
	 * @return int the byte which was read or -1 at end of stream.
	 * 
	 * @exception java.io.IOException
	 *                If reading the source stream causes an IOException.
	 */
	public int read() throws IOException {
		int result = super.read();
		if (on && result >= 0)
			digest.engineUpdate((byte) result);
		return result;
	}

	/**
	 * Reads at most <code>count</code> bytes from the Stream and stores them
	 * in the byte array <code>buffer</code> starting at <code>offset</code>.
	 * Answer the number of bytes actually read or -1. Updates the digest for
	 * the bytes being read if this fuction is enabled.
	 * 
	 * 
	 * @param buffer
	 *            byte[] the byte array in which to store the read bytes.
	 * @param offset
	 *            int the offset in <code>buffer</code> to store the read
	 *            bytes.
	 * @param count
	 *            int the maximum number of bytes to store in
	 *            <code>buffer</code>.
	 * @return int the number of bytes actually read or -1 if end of stream.
	 * 
	 * @exception java.io.IOException
	 *                If reading the source stream causes an IOException.
	 */

	public int read(byte[] buffer, int offset, int count) throws IOException {
		int read = super.read(buffer, offset, count);
		if (on && read > 0)
			digest.engineUpdate(buffer, offset, read);
		return read;
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
		StringBuffer answer = new StringBuffer("DigestInputStream");
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
}
