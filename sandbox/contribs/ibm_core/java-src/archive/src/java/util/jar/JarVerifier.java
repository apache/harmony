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

package java.util.jar;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;

import com.ibm.oti.util.BASE64Decoder;
import com.ibm.oti.util.JarUtils;
import com.ibm.oti.util.Msg;

/**
 * Non-public class used by {@link java.util.jar.JarFile} and
 * {@link java.util.jar.JarInputStream} to manage the verification of signed
 * jars. <code>JarFile</code> and <code>JarInputStream</code> objects will
 * be expected to have a <code>JarVerifier</code> instance member which can be
 * used to carry out the tasks associated with verifying a signed jar. These
 * tasks would typically include:
 * <ul>
 * <li>verification of all signed signature files
 * <li>confirmation that all signed data was signed only by the party or
 * parties specified in the signature block data
 * <li>verification that the contents of all signature files (i.e.
 * <code>.SF</code> files) agree with the jar entries information found in the
 * jar maifest.
 * </ul>
 */
class JarVerifier {

	private String jarName;

	private Manifest man;

	private HashMap metaEntries = new HashMap(5);

	private Hashtable signatures = new Hashtable(5);

	private Hashtable certificates = new Hashtable(5);

	private Hashtable verifiedEntries = new Hashtable();

	/**
	 * TODO Type description
	 */
	static class VerifierEntry extends OutputStream {
		/**
		 * Comment for <code>digest</code>
		 */
		MessageDigest digest;

		/**
		 * Comment for <code>hash</code>
		 */
		byte[] hash;

		/**
		 * Comment for <code>certificates</code>
		 */
		java.security.cert.Certificate[] certificates;

		/**
		 * @param digest
		 * @param hash
		 * @param certificates
		 */
		VerifierEntry(MessageDigest digest, byte[] hash,
				java.security.cert.Certificate[] certificates) {
			this.digest = digest;
			this.hash = hash;
			this.certificates = certificates;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.io.OutputStream#write(int)
		 */
		public void write(int value) {
			digest.update((byte) value);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.io.OutputStream#write(byte[], int, int)
		 */
		public void write(byte[] buf, int off, int nbytes) {
			digest.update(buf, off, nbytes);
		}
	}

	/**
	 * Constructs and answers with a new instance of JarVerifier.
	 * 
	 * @param name
	 *            the name of the jar file being verified.
	 */
	JarVerifier(String name) {
		jarName = name;
	}

	/**
	 * Called for each new jar entry read in from the input stream. This method
	 * constructs and returns a new {@link VerifierEntry} which contains the
	 * certificates used to sign the entry and its hash value as specified in
	 * the jar manifest.
	 * 
	 * @param name
	 *            the name of an entry in a jar file which is <b>not</b> in the
	 *            <code>META-INF</code> directory.
	 * @return a new instance of {@link VerifierEntry} which can be used by
	 *         callers as an {@link OutputStream}.
	 */
	VerifierEntry initEntry(String name) {
		// If no manifest is present by the time an entry is found,
		// verification cannot occur. If no signature files have
		// been found, do not verify.
		if (man == null || signatures.size() == 0)
			return null;

		Attributes attributes = man.getAttributes(name);
		// entry has no digest
		if (attributes == null)
			return null;

		Vector certs = new Vector();
		Iterator it = signatures.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			HashMap hm = (HashMap) entry.getValue();
			if (hm.get(name) != null) {
				// Found an entry for entry name in .SF file
				String signatureFile = (String) entry.getKey();

				Vector newCerts = getSignerCertificates(signatureFile,
						certificates);
				Iterator iter = newCerts.iterator();
				while (iter.hasNext()) {
					certs.add(iter.next());
				}
			}
		}

		// entry is not signed
		if (certs.size() == 0)
			return null;
		Certificate[] certificatesArray = new Certificate[certs.size()];
		certs.toArray(certificatesArray);

		String algorithms = attributes.getValue("Digest-Algorithms");
		if (algorithms == null)
			algorithms = "SHA SHA1";
		StringTokenizer tokens = new StringTokenizer(algorithms);
		while (tokens.hasMoreTokens()) {
			String algorithm = tokens.nextToken();
			String hash = attributes.getValue(algorithm + "-Digest");
			if (hash == null)
				continue;
			byte[] hashBytes;
			try {
				hashBytes = hash.getBytes("ISO8859_1");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e.toString());
			}

			try {
				return new VerifierEntry(MessageDigest.getInstance(algorithm),
						hashBytes, certificatesArray);
			} catch (NoSuchAlgorithmException e) {
			}
		}
		return null;
	}

	/**
	 * Add a new meta entry to the internal collection of data held on each jar
	 * entry in the <code>META-INF</code> directory including the manifest
	 * file itself. Files associated with the signing of a jar would also be
	 * added to this collection.
	 * 
	 * @param name
	 *            the name of the file located in the <code>META-INF</code>
	 *            directory.
	 * @param buf
	 *            the file bytes for the file called <code>name</code>.
	 * @see #removeMetaEntries()
	 */
	void addMetaEntry(String name, byte[] buf) {
		metaEntries.put(name.toUpperCase(), buf);
	}

	/**
	 * If the associated jar file is signed, check on the validity of all of the
	 * known signatures.
	 * 
	 * @return <code>true</code> if the associated jar is signed and an
	 *         internal check verifies the validity of the signature(s).
	 *         <code>false</code> if the associated jar file has no entries at
	 *         all in its <code>META-INF</code> directory. This situation is
	 *         indicative of an invalid jar file.
	 *         <p>
	 *         Will also return true if the jar file is <i>not</i> signed.
	 *         </p>
	 * @throws SecurityException
	 *             if the jar file is signed and it is determined that a
	 *             signature block file contains an invalid signature for the
	 *             corresponding signature file.
	 */
	synchronized boolean readCertificates() {
		if (metaEntries == null)
			return false;
		Iterator it = metaEntries.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			if (key.endsWith(".DSA") || key.endsWith(".RSA")) {
				verifyCertificate(key);
				// Check for recursive class load
				if (metaEntries == null)
					return false;
				it.remove();
			}
		}
		return true;
	}

	/**
	 * @param certFile
	 */
	private void verifyCertificate(String certFile) {
		// Found Digital Sig, .SF should already have been read
		String signatureFile = certFile.substring(0, certFile.lastIndexOf('.'))
				+ ".SF";
		byte[] sfBytes = (byte[]) metaEntries.get(signatureFile);
		if (sfBytes == null)
			return;

		byte[] sBlockBytes = (byte[]) metaEntries.get(certFile);
		try {
			Certificate[] signerCertChain = JarUtils.verifySignature(
					new ByteArrayInputStream(sfBytes),
					new ByteArrayInputStream(sBlockBytes));
			if (signerCertChain != null) {
				this.certificates.put(signatureFile, signerCertChain);
			}
		} catch (IOException e) {
			return;
		} catch (GeneralSecurityException e) {
			/* [MSG "K00eb", "{0} failed verification of {1}"] */
			throw new SecurityException(Msg.getString("K00eb", jarName,
					signatureFile));
		}

		// Verify manifest hash in .sf file
		Attributes attributes = new Attributes();
		HashMap hm = new HashMap();
		try {
			new InitManifest(new ByteArrayInputStream(sfBytes), attributes, hm,
					null, "Signature-Version");
		} catch (IOException e) {
			return;
		}

		boolean createdBySigntool = false;
		String createdByValue = attributes.getValue("Created-By");
		if (createdByValue != null) {
			createdBySigntool = createdByValue.indexOf("signtool") != -1;
		}
		byte[] manifest = (byte[]) metaEntries.get(JarFile.MANIFEST_NAME);
		if (manifest == null)
			return;
		String digestAttribute = createdBySigntool ? "-Digest"
				: "-Digest-Manifest";
		if (!verify(attributes, digestAttribute, manifest, false)) {
			Iterator it = hm.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				byte[] chunk = man.getChunk((String) entry.getKey());
				if (chunk == null)
					return;
				if (!verify((Attributes) entry.getValue(), "-Digest", chunk,
						createdBySigntool))
					/* [MSG "K00ec", "{0} has invalid digest for {1} in {2}"] */
					throw new SecurityException(Msg.getString("K00ec",
							new Object[] { signatureFile, entry.getKey(),
									jarName }));
			}
		}
		metaEntries.put(signatureFile, null);
		signatures.put(signatureFile, hm);
	}

	/**
	 * Associate this verifier with the specified {@link Manifest} object.
	 * 
	 * @param mf
	 *            a <code>java.util.jar.Manifest</code> object.
	 */
	void setManifest(Manifest mf) {
		man = mf;
	}

	/**
	 * Verifies that the digests stored in the manifest match the decrypted
	 * digests from the .SF file. This indicates the validity of the signing,
	 * not the integrity of the file, as it's digest must be calculated and
	 * verified when its contents are read.
	 * 
	 * @param entry
	 *            the {@link VerifierEntry} associated with the specified
	 *            <code>zipEntry</code>.
	 * @param zipEntry
	 *            an entry in the jar file
	 * @throws SecurityException
	 *             if the digest value stored in the manifest does <i>not</i>
	 *             agree with the decrypted digest as recovered from the
	 *             <code>.SF</code> file.
	 * @see #initEntry(String)
	 */
	void verifySignatures(VerifierEntry entry, ZipEntry zipEntry) {
		byte[] digest = entry.digest.digest();
		if (!MessageDigest.isEqual(digest, BASE64Decoder.decode(entry.hash)))
			/* [MSG "K00ec", "{0} has invalid digest for {1} in {2}"] */
			throw new SecurityException(Msg.getString("K00ec", new Object[] {
					JarFile.MANIFEST_NAME, zipEntry.getName(), jarName }));
		verifiedEntries.put(zipEntry.getName(), entry.certificates);
		if (zipEntry instanceof JarEntry)
			((JarEntry) zipEntry).certificates = (Certificate[]) entry.certificates
					.clone();
	}

	/**
	 * Returns a <code>boolean</code> indication of whether or not the
	 * associated jar file is signed.
	 * 
	 * @return <code>true</code> if the jar is signed, <code>false</code>
	 *         otherwise.
	 */
	boolean isSignedJar() {
		return certificates.size() > 0;
	}

	/*
	 * @param attributes @param entry @param data @param ignoreSecondEndline
	 * @return
	 */
	private boolean verify(Attributes attributes, String entry, byte[] data,
			boolean ignoreSecondEndline) {
		String algorithms = attributes.getValue("Digest-Algorithms");
		if (algorithms == null)
			algorithms = "SHA SHA1";
		StringTokenizer tokens = new StringTokenizer(algorithms);
		while (tokens.hasMoreTokens()) {
			String algorithm = tokens.nextToken();
			String hash = attributes.getValue(algorithm + entry);
			if (hash == null)
				continue;

			MessageDigest md;
			try {
				md = MessageDigest.getInstance(algorithm);
			} catch (NoSuchAlgorithmException e) {
				continue;
			}
			if (ignoreSecondEndline && data[data.length - 1] == '\n'
					&& data[data.length - 2] == '\n') {
				md.update(data, 0, data.length - 1);
			} else {
				md.update(data, 0, data.length);
			}
			byte[] b = md.digest();
			byte[] hashBytes;
			try {
				hashBytes = hash.getBytes("ISO8859_1");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e.toString());
			}
			return MessageDigest.isEqual(b, BASE64Decoder.decode(hashBytes));
		}
		return false;
	}

	/**
	 * Returns all of the {@link java.security.cert.Certificate} instances that
	 * were used to verify the signature on the jar entry called
	 * <code>name</code>.
	 * 
	 * @param name
	 *            the name of a jar entry.
	 * @return an array of {@link java.security.cert.Certificate}.
	 */
	Certificate[] getCertificates(String name) {
		Certificate[] verifiedCerts = (Certificate[]) verifiedEntries.get(name);
		if (verifiedCerts == null) {
			return null;
		}
		return (Certificate[]) verifiedCerts.clone();
	}

	/**
	 * Remove all entries from the internal collection of data held about each
	 * jar entry in the <code>META-INF</code> directory.
	 * 
	 * @see #addMetaEntry(String, byte[])
	 */
	void removeMetaEntries() {
		metaEntries = null;
	}

	/**
	 * Returns a <code>Vector</code> of all of the
	 * {@link java.security.cert.Certificate}s that are associated with the
	 * signing of the named signature file.
	 * 
	 * @param signatureFileName
	 *            the name of a signature file
	 * @param certificates
	 *            a <code>Map</code> of all of the certificate chains
	 *            discovered so far while attempting to verify the jar that
	 *            contains the signature file <code>signatureFileName</code>.
	 *            This object will have been previously set in the course of one
	 *            or more calls to
	 *            {@link #verifyJarSignatureFile(String, String, String, Map, Map)}
	 *            where it was passed in as the last argument.
	 * @return all of the <code>Certificate</code> entries for the signer of
	 *         the jar whose actions led to the creation of the named signature
	 *         file.
	 */
	public static Vector getSignerCertificates(String signatureFileName,
			Map certificates) {
		Vector result = new Vector();
		Certificate[] certChain = (Certificate[]) certificates
				.get(signatureFileName);
		if (certChain != null) {
			for (int i = 0; i < certChain.length; i++) {
				result.add(certChain[i]);
			}// end for all certificates
		}// end if at least one cert found
		return result;
	}
}
