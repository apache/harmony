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
import java.util.Iterator;
import java.util.List;

/**
 * An immutable certificate path that can be validated. All certificates in the
 * path are of the same type (i.e., X509).
 * 
 * A <code>CertPath</code> can be represented as a byte array in at least one
 * supported encoding when serialized.
 * 
 * When a <code>List</code> of the certificates is obtained it must be
 * immutable.
 * 
 * A <code>CertPath</code> must be thread-safe without requiring coordinated
 * access.
 */
public abstract class CertPath implements Serializable {

	private String type;

	/**
	 * Creates a <code>CertPath</code> of the specified type.
	 * <code>CertPath</code>s should normally be created via
	 * <code>CertificateFactory</code>.
	 * 
	 * @param type
	 *            standard name of the <code>Certificate</code> type in the
	 *            <code>CertPath</code>
	 */
	protected CertPath(String type) {
		this.type = type;
	}

	/**
	 * Returns the type of <code>Certificate</code> in the
	 * <code>CertPath</code>
	 * 
	 * @return <code>Certificate</code> type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Return an <code>Iterator</code> over the supported encodings for a
	 * representation of the certificate path.
	 * 
	 * @return <code>Iterator</code> over supported encodings (as
	 *         <code>String</code>s)
	 */
	public abstract Iterator getEncodings();

	/**
	 * Returns true if <code>Certificate</code>s in the list are the same
	 * type and the lists are equal (and by implication the certificates
	 * contained within are the same).
	 * 
	 * @param other
	 *            <code>CertPath</code> to be compared for equality
	 */
	public boolean equals(Object other) {

		// Test that object is a CertPath
		if ((other instanceof CertPath) == false) {
			return false;
		}
		CertPath comparePath = (CertPath) other;

		// Compare the certificate types
		if (comparePath.type != this.type) {
			return false;
		}

		// Compare the certificate lists
		if (comparePath.getCertificates() != this.getCertificates()) {
			return false;
		}

		return true;
	}

	/**
	 * Returns an immutable List of the <code>Certificate</code>s contained
	 * in the <code>CertPath</code>.
	 * 
	 * @return list of <code>Certificate</code>s in the <code>CertPath</code>
	 */
	public abstract List getCertificates();

	/**
	 * Overrides Object.hashCode() Defined as: hashCode = 31 *
	 * path.getType().hashCode() + path.getCertificates().hashCode();
	 * 
	 * @return hash code for CertPath object
	 */
	public int hashCode() {
		return (31 * this.getType().hashCode())
				+ this.getCertificates().hashCode();
	}

	/**
	 * Returns a <code>String</code> representation of the
	 * <code>CertPath</code>
	 * <code>Certificate</code>s. It is the result of
	 * calling <code>toString</code> on all <code>Certificate</code>s in
	 * the <code>List</code>. <code>Certificate</code>s
	 * 
	 * @return string representation of <code>CertPath</code>
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		for (Iterator iter = this.getCertificates().iterator(); iter.hasNext();) {
			Certificate element = (Certificate) iter.next();
			buffer.append(element.toString());
		}

		return buffer.toString();
	}

	/**
	 * Returns an encoding of the <code>CertPath</code> using the default
	 * encoding
	 * 
	 * @return default encoding of the <code>CertPath</code>
	 * @throws CertificateEncodingException
	 */
	public abstract byte[] getEncoded() throws CertificateEncodingException;

	/**
	 * Returns an encoding of the <code>CertPath</code> using the specified
	 * encoding
	 * 
	 * @param encoding
	 *            encoding that should be generated
	 * @return default encoding of the <code>CertPath</code>
	 * @throws CertificateEncodingException
	 */
	public abstract byte[] getEncoded(String encoding)
			throws CertificateEncodingException;

	/**
	 * Returns a <code>CertPath.CertPathRep</code> that can be serialized
	 * 
	 * @return CertPath.CertPathRep
	 * @throws ObjectStreamException
	 */
	protected Object writeReplace() throws ObjectStreamException {

		byte[] data;
		try {
			data = this.getEncoded(type);
		} catch (CertificateEncodingException e) {
			throw new NotSerializableException(e.getMessage());
		}

		return new CertPathRep(this.type, data);

	}

	/**
	 * Used for serialization of <code>CertPath</code>
	 */
	protected static class CertPathRep implements Serializable {

		private String type;

		private byte[] data;

		/**
		 * Creates a serializable form of <code>CertPath</code>
		 * 
		 * @param type
		 *            type of the <code>CertPath</code>
		 * @param data
		 *            encoded representation of the <code>CertPath</code>
		 */
		protected CertPathRep(String type, byte[] data) {
			this.type = type;
			this.data = data;
		}

		/**
		 * Reads a <code>CertPath</code> from its serialized representation
		 * 
		 * @return new <code>CertPath</code> from encoded form
		 * @throws ObjectStreamException
		 */
		protected Object readResolve() throws ObjectStreamException {

			CertPath path;
			try {
				CertificateFactory cf = CertificateFactory.getInstance(type);
				ByteArrayInputStream baos = new ByteArrayInputStream(data);
				path = cf.generateCertPath(baos);
			} catch (CertificateException e) {
				throw new InvalidObjectException(e.getMessage());
			}
			return path;
		}
	}
}
