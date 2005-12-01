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


/**
 * This class is a Service Provider Interface (therefore the Spi suffix) for
 * digest algorithms to be supplied by providers. Examples of digest algorithms
 * are MD5 and SHA.
 * 
 * A digest is a secure hash function for a stream of bytes, like a fingerprint
 * for the stream of bytes.
 * 
 */
public abstract class MessageDigestSpi {
	/**
	 * Constructs a new instance of this class
	 * 
	 */
	public MessageDigestSpi() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/**
	 * Actually does the work of computing the final hash value that the
	 * receiver represents, and answers the resulting value. Note that the
	 * caller must invoke <code>reset</code> before using the receiver
	 * further.
	 * 
	 * 
	 * @return the hash the receiver computed
	 * 
	 * @see MessageDigest#reset
	 */
	protected abstract byte[] engineDigest();

	/**
	 * Returns the digest value in the buffer provided.
	 * 
	 * @param buffer
	 *            where to store the resultant digest
	 * @param offset
	 *            where in <code>buffer</code> to store the digest
	 * @param length
	 *            how many bytes in <code>buffer</code> are available for
	 *            writing the digest to
	 * @return the number of bytes in <code>buffer</code> used to actually
	 *         store the digest value
	 * 
	 * @throws DigestException
	 *             if <code>length</code> is not big enough for the result
	 *             digest
	 */
	protected int engineDigest(byte[] buffer, int offset, int length)
			throws DigestException {
		byte[] digest = engineDigest();
		int digestLength = engineGetDigestLength();
		if (digestLength > length)
			throw new DigestException(); // Can't fit in the buffer

		System.arraycopy(digest, 0, buffer, offset, digestLength);
		return digestLength;
	}

	/**
	 * Return the engine digest length in bytes. Default is 0.
	 * @return int the engine digest length in bytes
	 * 
	 */
	protected int engineGetDigestLength() {
		return 0;
	}

	/**
	 * Puts the receiver back in an initial state, such that it is ready to
	 * compute a new hash.
	 * 
	 * @see MessageDigest#reset()
	 */
	protected abstract void engineReset();

	/**
	 * Includes a range of bytes from the first argument in the hash value
	 * computed by the receiver.
	 * 
	 * 
	 * @param bytesToHash
	 *            byte[] the source array
	 * @param offset
	 *            the starting offset into the array
	 * @param count
	 *            the number of bytes to include in the hash
	 */
	protected abstract void engineUpdate(byte bytesToHash[], int offset,
			int count);

	/**
	 * Includes the argument in the hash value computed by the receiver.
	 * 
	 * 
	 * @param byteToHash
	 *            byte the byte to feed to the hash algorithm
	 * 
	 * @see MessageDigest#reset()
	 */
	protected abstract void engineUpdate(byte byteToHash);
}
