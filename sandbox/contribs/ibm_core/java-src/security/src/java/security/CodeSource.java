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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This class represents a "source of code" which is taken to be an URL
 * representing the location where that code was loaded from, and a list of
 * certificates that were used to verify that code.
 * 
 */
public class CodeSource extends Object implements java.io.Serializable {
	static final long serialVersionUID = 4977541819976013951L;

	/**
	 * The URL which was used to create the receiver.
	 */
	private URL location;

	/**
	 * The Certificates which were used to create the receiver.
	 */
	private transient Certificate[] certificates;

	/**
	 * A hashtable containing the values from the certificates array.
	 */
	private transient Hashtable certificatesSet;

	/**
	 * Constructs a new instance of this class with its url and certificates
	 * fields filled in from the arguments.
	 * 
	 * @param url
	 *            URL the URL.
	 * @param certificates
	 *            Certificate[] the Certificates.
	 */
	public CodeSource(URL url, Certificate[] certificates) {
		location = url;
		if (certificates != null) {
			this.certificates = (Certificate[]) certificates.clone();
			certificatesSet = new Hashtable(certificates.length * 3 / 2);
			for (int i = 0; i < certificates.length; ++i)
				if (certificates[i] != null)
					certificatesSet.put(certificates[i], "ignored");
			if (certificatesSet.size() == 0)
				certificatesSet = null;
		}
	}

	/**
	 * Compares the argument to the receiver, and answers true if they represent
	 * the <em>same</em> object using a class specific comparison. In this
	 * case, the receiver and the object must have the same URL and the same
	 * collection of certificates.
	 * 
	 * 
	 * @param o
	 *            the object to compare with this object
	 * @return <code>true</code> if the object is the same as this object
	 *         <code>false</code> if it is different from this object
	 * @see #hashCode
	 */
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		if (this.getClass() != o.getClass())
			return false;
		CodeSource other = (CodeSource) o;

		// Check if URLs match.
		URL myLocation = this.getLocation();
		if (myLocation == null) {
			if (other.getLocation() != null)
				return false;
		} else {
			if (!myLocation.equals(other.getLocation()))
				return false;
		}

		// URLs match, so check certificates.
		if (certificatesSet == null) {
			if (other.certificatesSet != null)
				return false;
		} else {
			// This code relies on the assumption that, multiple copies
			// of the same certificate do not contribute anything interesting
			// to the differences. For example, if two code sources differ
			// in that the first has two certificate "A"s and one certificate
			// "B", while the other has two certificate "B"s and one certificate
			// "A", they should still be considered the same.
			if (other.certificatesSet == null)
				return false;
			if (certificatesSet.size() != other.certificatesSet.size())
				return false;
			Enumeration keysEnum = certificatesSet.keys();
			while (keysEnum.hasMoreElements())
				if (!other.certificatesSet.containsKey(keysEnum.nextElement()))
					return false;
		}

		return true;
	}

	/**
	 * Answers an integer hash code for the receiver. Any two objects which
	 * answer <code>true</code> when passed to <code>.equals</code> must
	 * answer the same value for this method.
	 * 
	 * 
	 * @return int the receiver's hash.
	 * 
	 * @see #equals
	 */
	public int hashCode() {
		URL myLocation = this.getLocation();
		if (myLocation == null) {
			return 1313;
		}
		return myLocation.hashCode();
	}

	/**
	 * Answers the certificates held onto by the receiver.
	 * 
	 * 
	 * @return Certificate[] the receiver's certificates
	 */
	public final Certificate[] getCertificates() {
		if (certificates == null)
			return null;
		return (Certificate[]) certificates.clone();
	}

	/**
	 * Answers the receiver's location.
	 * 
	 * 
	 * @return URL the receiver's URL
	 */
	public final URL getLocation() {
		return location;
	}

	/**
	 * Indicates whether the argument code source is implied by the receiver.
	 * 
	 * 
	 * @return boolean <code>true</code> if the argument code source is
	 *         implied by the receiver, and <code>false</code> if it is not.
	 * @param other
	 *            CodeSource the code source to check
	 */
	public boolean implies(CodeSource other) {
		if (other == null)
			return false;
		if (this == other)
			return true;

		// Check certificates: If I have certificates,
		// then they must all be in the other one.
		if (certificatesSet != null) {
			if (other.certificatesSet == null)
				return false;
			Enumeration keysEnum = certificatesSet.keys();
			while (keysEnum.hasMoreElements())
				if (!other.certificatesSet.containsKey(keysEnum.nextElement()))
					return false;
		}

		// Check the URLs. There are some very subtle rules being encoded
		// here. 
		URL myURL = this.getLocation();
		if (myURL != null) {
			URL hisURL = other.getLocation();
			if (hisURL == null)
				return false;
			if (myURL.equals(hisURL))
				return true;
			if (!myURL.getProtocol().equals(hisURL.getProtocol()))
				return false;
			if (myURL.getHost() != null) {
				if (hisURL.getHost() == null
						|| !new java.net.SocketPermission(myURL.getHost(),
								"resolve")
								.implies(new java.net.SocketPermission(hisURL
										.getHost(), "resolve")))
					return false;
			}
			if (myURL.getPort() != -1 && myURL.getPort() != hisURL.getPort())
				return false;
			String myFile = myURL.getFile();
			String hisFile = hisURL.getFile();
			if (myFile != null && !myFile.equals(hisFile)) {
				if (myFile.endsWith("/-")) {
					if (!hisFile.startsWith(myFile.substring(0,
							myFile.length() - 1)))
						return false;
				} else if (myFile.endsWith("/*")) {
					if ((!hisFile.startsWith(myFile.substring(0, myFile
							.length() - 1)))
							|| (hisFile.indexOf('/', myFile.length()) > 0))
						return false;
				} else if (!myFile.endsWith("/")) {
					if (!hisFile.equals(myFile + "/"))
						return false;
				} else
					return false;
			}
			if (myURL.getRef() != null
					&& !myURL.getRef().equals(hisURL.getRef()))
				return false;
		}
		return true;
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * 
	 * @return a printable representation for the receiver.
	 */
	public String toString() {
		StringBuffer result = new StringBuffer("CodeSource : "); //$NON-NLS-1$

		if (certificates == null || certificates.length == 0) {
			result.append(location + " : no certificates"); //$NON-NLS-1$
		} else {
			result.append(location + " : " + certificates); //$NON-NLS-1$
		}
		return result.toString();
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		if (certificates == null) {
			stream.writeInt(0);
		} else {
			stream.writeInt(certificates.length);
			for (int i = 0; i < certificates.length; i++) {
				stream.writeUTF(certificates[i].getType());
				try {
					byte[] encoded = certificates[i].getEncoded();
					stream.writeInt(encoded.length);
					stream.write(encoded);
				} catch (CertificateEncodingException e) {
					stream.writeInt(0);
				}
			}
		}
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		stream.defaultReadObject();
		int count = stream.readInt();
		if (count > 0) {
			certificates = new Certificate[count];
			for (int i = 0; i < count; i++) {
				String type = stream.readUTF();
				int length = stream.readInt();
				if (length > 0) {
					byte[] encoded = new byte[length];
					stream.read(encoded);
					try {
						CertificateFactory factory = CertificateFactory
								.getInstance(type);
						certificates[i] = factory
								.generateCertificate(new ByteArrayInputStream(
										encoded));
					} catch (CertificateException e) {
					}
				}
			}
			certificatesSet = new Hashtable(certificates.length * 3 / 2);
			for (int i = 0; i < certificates.length; ++i)
				if (certificates[i] != null)
					certificatesSet.put(certificates[i], "ignored");
			if (certificatesSet.size() == 0)
				certificatesSet = null;
		}
	}
}
