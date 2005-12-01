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

package com.ibm.oti.util;


import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class implements a password-protected input stream. The algorithm used
 * for protection is a Vigenere (repeated-key) cipher. The encrypted data is the
 * result of <original data> XOR KEYKEYKEY...
 */
public class PasswordProtectedInputStream extends FilterInputStream {

	private byte[] password; // Password to use to decrypt the input bytes

	private int pwdIndex = 0; // Index into the password array.

	/**
	 * Constructs a new instance of the receiver.
	 * 
	 * @param in
	 *            java.io.InputStream The actual input stream where to read the
	 *            bytes from.
	 * @param password
	 *            byte[] password bytes to use to decrypt the input bytes
	 */
	public PasswordProtectedInputStream(InputStream in, byte[] password) {
		super(in);
		this.password = password;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		int read = in.read();
		if (read >= 0) {
			read ^= password[pwdIndex];
			pwdIndex = (pwdIndex + 1) % password.length;
		}
		return read;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte b[], int off, int len) throws IOException {
		int read = in.read(b, off, len);
		if (read > 0) {
			int lastIndex = off + read;
			for (int i = off; i < lastIndex; i++) {
				b[i] ^= password[pwdIndex];
				pwdIndex = (pwdIndex + 1) % password.length;
			}
		}
		return read;
	}

	/**
	 * Skips over and discards <code>n</code> bytes of data from the input
	 * stream. The <code>skip</code> method may, for a variety of reasons, end
	 * up skipping over some smaller number of bytes, possibly <code>0</code>.
	 * The actual number of bytes skipped is returned.
	 * <p>
	 * The <code>skip </code>method of <code>FilterInputStream</code> calls
	 * the <code>skip</code> method of its underlying input stream with the
	 * same argument, and returns whatever value that method does.
	 * 
	 * @param n
	 *            the number of bytes to be skipped.
	 * @return the actual number of bytes skipped.
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @since JDK1.0
	 */

	public long skip(long n) throws IOException {
		long skip = super.skip(n);
		pwdIndex += skip;
		return skip;
	}
}
