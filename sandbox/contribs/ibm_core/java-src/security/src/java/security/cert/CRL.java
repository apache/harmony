/* Copyright 1998, 2002 The Apache Software Foundation or its licensors, as applicable
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


/**
 * This class represents Certificate Revocation Lists (CRLs). They are used to
 * indicate that a given Certificate has expired already.
 * 
 * @see CertificateFactory
 */
public abstract class CRL {

	private String type; // Type of this CRL

	/**
	 * Constructs a new instance of this class for a given CRL type.
	 * 
	 * @param type
	 *            String type of this CRL
	 */
	protected CRL(String type) {
		super();
		this.type = type;
	}

	/**
	 * Answers the type of this CRL.
	 * 
	 * @return String the type of this CRL.
	 */
	public final String getType() {
		return type;
	}

	/**
	 * Answers if a given Certificate has been revoked or not.
	 * 
	 * @param cert
	 *            Certificate The Certificate to test
	 * 
	 * @return true if the certificate has been revoked false if the certificate
	 *         has not been revoked yet
	 */
	public abstract boolean isRevoked(Certificate cert);

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * @return a printable representation for the receiver.
	 */
	public abstract String toString();
}
