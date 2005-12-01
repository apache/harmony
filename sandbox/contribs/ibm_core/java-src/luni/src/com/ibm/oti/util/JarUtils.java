/* Copyright 2005 The Apache Software Foundation or its licensors, as applicable
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


import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;

/**
 * Helper class containing a number of static convenience methods for use in the
 * processing of JAR files.
 */
public class JarUtils {

	/**
	 * Ensure that this utility class cannot be instantiated.
	 */
	private JarUtils() {
		// NO OP
	}

	// ----------- Verification of a signed jar's signature file follows ------

	/**
	 * Convenience method to used to check that a jar's signature file and its
	 * corresponding signature block file (both supplied as {@link InputStream}
	 * objects) are valid and that neither has been tampered with in respect to
	 * the other.
	 * <p>
	 * In particular, this method will verify that:
	 * <ul>
	 * <li>the signature block file contains a signature that could only have
	 * been created with the private key corresponding to the public key whose
	 * certificate chain is also included in the signature block file.
	 * <li>the message digest of the signature file is precisely equivalent to
	 * the message digest value taken from the decrypted signature block file.
	 * Confirming this demonstrates that there has been no tampering on the
	 * contents of the signature file since the signing took place.
	 * 
	 * @param signature
	 *            the contents of a signed jar's signature file (.SF file) as an
	 *            <code>InputStream</code>. This stream will be closed by the
	 *            method immediately upon its contents being read.
	 * @param signatureBlock
	 *            the contents of a signed jar's binary signature block file as
	 *            an <code>InputStream</code>. This stream will be closed by
	 *            the method immediately upon its contents being read.
	 * @return the certificate chains discovered in the supplied signature block
	 *         file stream in an array of {@link Certificate}.
	 * @throws IOException
	 *             if there was an error encountered while reading from either
	 *             of the supplied input streams.
	 * @throws GeneralSecurityException
	 *             if the signature verification process fails.
	 */
	public static Certificate[] verifySignature(InputStream signature,
			InputStream signatureBlock) throws IOException,
			GeneralSecurityException {
		// STUB IMPLEMENTATION
		return null;
	}

	// ----------- Creating a signed jar file follows --------------

	/**
	 * Creates the PKCS #7 version of a signed jar's digital signature which may
	 * be directly used as the contents of the signature block file associated
	 * with the signing. Receives the signed version of the digest (created
	 * using the signing alias' private key) and constructs the PKCS #7 version
	 * of the digital signature as a <code>byte</code> array.
	 * 
	 * @param encryptedDigest
	 *            the signed version of the message digest value supplied in
	 *            <code>digest</code>. This value will have been previously
	 *            obtained by signing <code>digest</code> with the signer's
	 *            private key using the signature algorithm specified in
	 *            <code>signatureAlgorithm</code>.
	 * @param signerCertificateChain
	 *            the certificate chain associated with the alias signing the
	 *            jar. This value will have been previously obtained as the
	 *            result of a call to
	 *            {@link java.security.KeyStore#getCertificateChain(String)}
	 *            with the signer's alias.
	 * @param digestAlgorithm
	 *            the name of the message digest algorithm used to calculate the
	 *            value supplied in parameter <code>digest</code>.
	 * @param signatureAlgorithm
	 *            the name of the signature algorithm used to calculate the
	 *            value supplied in parameter <code>encryptedDigest</code>.
	 * @return signature data in the PKCS #7 format which may be used as the
	 *         complete contents of the signature block file (e.g. the
	 *         <code>.DSA</code> or <code>.RSA</code> file) created for a
	 *         given jar signing.
	 * @throws IOException
	 */
	public static byte[] getSignatureBlockBytes(byte[] encryptedDigest,
			Certificate[] signerCertificateChain, String digestAlgorithm,
			String signatureAlgorithm) throws IOException {
		// STUB IMPLEMENTATION
		return new byte[0];
	}
}
