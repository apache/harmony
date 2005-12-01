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

package java.security.cert;


import java.io.ByteArrayInputStream;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Abstract class to represent identity certificates. It represents a way to
 * verify the binding of a Principal and its public key. Examples are X.509,
 * PGP, and SDSI.
 */
public abstract class Certificate implements Serializable {
	
	static final long serialVersionUID = -6751606818319535583L;

	private String type; // Type of certificate

	/**
	 * Constructs a new instance of this class with its type.
	 * 
	 * @param type
	 *            String Type of certificate
	 */
	protected Certificate(String type) {
		this.type = type;
	}

	/**
	 * Compares the argument to the receiver, and answers true if they represent
	 * the <em>same</em> object using a class specific comparison. The
	 * implementation in Object answers true only if the argument is the exact
	 * same object as the receiver (==).
	 * 
	 * @param o
	 *            the object to compare with this object
	 * @return <code>true</code> if the object is the same as this object
	 *         <code>false</code> if it is different from this object
	 * @see #hashCode
	 */
	public boolean equals(Object o) {
		if (o == null)
			return false;

		Certificate c = null;
		try {
			c = (Certificate) o;
		} catch (ClassCastException e) {
			return false;
		}

		try {
			return Arrays.equals(this.getEncoded(), c.getEncoded());
		} catch (CertificateEncodingException cee) {
			return false;
		}
	}

	/**
	 * Answers the encoded representation for this certificate.
	 * 
	 * @return the encoded representation for this certificate.
	 */
	public abstract byte[] getEncoded() throws CertificateEncodingException;

	/**
	 * Answers the public key corresponding to this certificate.
	 * 
	 * @return the public key corresponding to this certificate.
	 */
	public abstract PublicKey getPublicKey();

	/**
	 * Answers the certificate type represented by the receiver.
	 * 
	 * @return the certificate type represented by the receiver.
	 */
	public final String getType() {
		return type;
	}

	/**
	 * Answers an integer hash code for the receiver. Any two objects which
	 * answer <code>true</code> when passed to <code>equals</code> must
	 * answer the same value for this method.
	 * 
	 * @return the receiver's hash
	 * 
	 * @see #equals
	 */
	public int hashCode() {
		try {
			CRC32 hash = new CRC32();
			hash.update(getEncoded());
			return (int) hash.getValue();
		} catch (CertificateEncodingException cee) {
			return super.hashCode();
		}
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * @return a printable representation for the receiver.
	 */
	public abstract String toString();

	/**
	 * Verifies that this certificate was signed with the given public key.
	 * 
	 * @param key
	 *            PublicKey public key for which verification should be
	 *            performed.
	 * 
	 * @exception CertificateException
	 *                if encoding errors are detected
	 * @exception NoSuchAlgorithmException
	 *                if an unsupported algorithm is detected
	 * @exception InvalidKeyException
	 *                if an invalid key is detected
	 * @exception NoSuchProviderException
	 *                if there is no default provider
	 * @exception SignatureException
	 *                if signature errors are detected
	 */
	public abstract void verify(PublicKey key) throws CertificateException,
			NoSuchAlgorithmException, InvalidKeyException,
			NoSuchProviderException, SignatureException;

	/**
	 * Verifies that this certificate was signed with the given public key. Uses
	 * the signature algorithm given by the provider.
	 * 
	 * @param key
	 *            PublicKey public key for which verification should be
	 *            performed.
	 * @param sigProvider
	 *            String the name of teh signature provider.
	 * 
	 * @exception CertificateException
	 *                if encoding errors are detected
	 * @exception NoSuchAlgorithmException
	 *                if an unsupported algorithm is detected
	 * @exception InvalidKeyException
	 *                if an invalid key is detected
	 * @exception NoSuchProviderException
	 *                if there is no default provider
	 * @exception SignatureException
	 *                if signature errors are detected
	 */
	public abstract void verify(PublicKey key, String sigProvider)
			throws CertificateException, NoSuchAlgorithmException,
			InvalidKeyException, NoSuchProviderException, SignatureException;

	/**
	 * Representation of a Certificate used for serialization.
	 */
	protected static class CertificateRep implements Serializable {

		static final long serialVersionUID = -8563758940495660020L;

		String type;

		byte[] data;

		/**
		 * Create this representation of a certificate.
		 * 
		 * @param type
		 *            the type of the certificate
		 * @param encoded
		 *            the encoded representation of the certificate
		 */
		protected CertificateRep(String type, byte[] encoded) {
			this.type = type;
			data = encoded;
		}

		/**
		 * Return the certificate represented by the receiver.
		 */
		protected Object readResolve() throws ObjectStreamException {
			try {
				CertificateFactory factory = CertificateFactory
						.getInstance(type);
				Certificate certificate = factory
						.generateCertificate(new ByteArrayInputStream(data));
				return certificate;
			} catch (CertificateException e) {
				throw new InvalidObjectException(e.toString());
			}
		}
	}

	/**
	 * Return a representation of the receiver for serialization.
	 */
	protected Object writeReplace() throws ObjectStreamException {
		byte[] encoded = null;
		try {
			encoded = getEncoded();
		} catch (CertificateEncodingException e) {
			throw new NotSerializableException(e.toString());
		}
		return new CertificateRep(type, encoded);
	}
}
