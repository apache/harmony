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
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.zip.ZipEntry;

/**
 * JarFile is used to read jar entries and their associated data from jar files.
 * 
 * @see JarInputStream
 * @see JarEntry
 */
public class JarFile extends java.util.zip.ZipFile {
	public static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

	static final String META_DIR = "META-INF/";

	private Manifest manifest;

	private ZipEntry manifestEntry;

	JarVerifier verifier;

	static final class JarFileInputStream extends FilterInputStream {
		private long count;

		private ZipEntry zipEntry;

		private JarVerifier verifier;

		private JarVerifier.VerifierEntry entry;

		private MessageDigest digest;

		JarFileInputStream(InputStream is, ZipEntry ze, JarVerifier ver) {
			super(is);
			if (ver != null) {
				zipEntry = ze;
				verifier = ver;
				count = zipEntry.getSize();
				entry = verifier.initEntry(ze.getName());
				if (entry != null)
					digest = entry.digest;
			}
		}

		public int read() throws IOException {
			int r = super.read();
			if (entry != null) {
				if (r != -1) {
					digest.update((byte) r);
					count--;
				}
				if (r == -1 || count <= 0) {
					JarVerifier.VerifierEntry temp = entry;
					entry = null;
					verifier.verifySignatures(temp, zipEntry);
				}
			}
			return r;
		}

		public int read(byte[] buf, int off, int nbytes) throws IOException {
			int r = super.read(buf, off, nbytes);
			if (entry != null) {
				if (r != -1) {
					int size = r;
					if (count < size)
						size = (int) count;
					digest.update(buf, off, size);
					count -= r;
				}
				if (r == -1 || count <= 0) {
					JarVerifier.VerifierEntry temp = entry;
					entry = null;
					verifier.verifySignatures(temp, zipEntry);
				}
			}
			return r;
		}

		public long skip(long nbytes) throws IOException {
			long cnt = 0, rem = 0;
			byte[] buf = new byte[4096];
			while (cnt < nbytes) {
				int x = read(buf, 0,
						(rem = nbytes - cnt) > buf.length ? buf.length
								: (int) rem);
				if (x == -1)
					return cnt;
				cnt += x;
			}
			return cnt;
		}
	}

	/**
	 * Create a new JarFile using the contents of file.
	 * 
	 * @param file
	 *            java.io.File
	 * @exception java.io.IOException
	 *                If the file cannot be read.
	 */
	public JarFile(File file) throws IOException {
		this(file, true);
	}

	/**
	 * Create a new JarFile using the contents of file.
	 * 
	 * @param file
	 *            java.io.File
	 * @param verify
	 *            verify a signed jar file
	 * @exception java.io.IOException
	 *                If the file cannot be read.
	 */
	public JarFile(File file, boolean verify) throws IOException {
		super(file);
		if (verify)
			verifier = new JarVerifier(file.getPath());
		readMetaEntries();
	}

	/**
	 * Create a new JarFile using the contents of file.
	 * 
	 * @param file
	 *            java.io.File
	 * @param verify
	 *            verify a signed jar file
	 * @param mode
	 *            the mode to use, either OPEN_READ or OPEN_READ | OPEN_DELETE
	 * @exception java.io.IOException
	 *                If the file cannot be read.
	 */
	public JarFile(File file, boolean verify, int mode) throws IOException {
		super(file, mode);
		if (verify)
			verifier = new JarVerifier(file.getPath());
		readMetaEntries();
	}

	/**
	 * Create a new JarFile from the contents of the file specified by filename.
	 * 
	 * @param filename
	 *            java.lang.String
	 * @exception java.io.IOException
	 *                If fileName cannot be opened for reading.
	 */
	public JarFile(String filename) throws IOException {
		this(filename, true);

	}

	/**
	 * Create a new JarFile from the contents of the file specified by filename.
	 * 
	 * @param filename
	 *            java.lang.String
	 * @param verify
	 *            verify a signed jar file
	 * @exception java.io.IOException
	 *                If fileName cannot be opened for reading.
	 */
	public JarFile(String filename, boolean verify) throws IOException {
		super(filename);
		if (verify)
			verifier = new JarVerifier(filename);
		readMetaEntries();
	}

	/**
	 * Return an enumeration containing the JarEntrys contained in this JarFile.
	 * 
	 * @return java.util.Enumeration
	 * @exception java.lang.IllegalStateException
	 *                If this JarFile has been closed.
	 */
	public Enumeration entries() {
		class JarFileEnumerator implements Enumeration {
			Enumeration ze;

			JarFile jf;

			JarFileEnumerator(Enumeration zenum, JarFile jf) {
				ze = zenum;
				this.jf = jf;
			}

			public boolean hasMoreElements() {
				return ze.hasMoreElements();
			}

			public Object nextElement() {
				JarEntry je = new JarEntry((ZipEntry) ze.nextElement());
				je.parentJar = jf;
				if (verifier != null)
					je.certificates = verifier.getCertificates(je.getName());
				return je;
			}
		}
		return new JarFileEnumerator(super.entries(), this);
	}

	/**
	 * Return the JarEntry specified by name or null if no such entry exists.
	 * 
	 * @param name
	 *            the name of the entry in the jar file
	 * @return java.util.jar.JarEntry
	 */
	public JarEntry getJarEntry(String name) {
		return (JarEntry) getEntry(name);
	}

	/**
	 * Returns the Manifest object associated with this JarFile or null if no
	 * manifest entry exists.
	 * 
	 * @return java.util.jar.Manifest
	 */
	public Manifest getManifest() throws IOException {
		if (manifest != null)
			return manifest;
		if (manifestEntry != null) {
			ByteArrayInputStream is = (ByteArrayInputStream) super
					.getInputStream(manifestEntry);
			if (verifier != null) {
				byte[] buf = new byte[is.available()];
				is.mark(buf.length);
				is.read(buf, 0, buf.length);
				is.reset();
				verifier.addMetaEntry(manifestEntry.getName(), buf);
			}
			try {
				manifest = new Manifest(is, verifier != null);
			} finally {
				is.close();
			}
			manifestEntry = null;
		}
		return manifest;
	}

	private void readMetaEntries() throws IOException {
		ZipEntry[] metaEntries = this.getMetaEntriesImpl(null);
		int dirLength = META_DIR.length();

		boolean signed = false;
		if (null != metaEntries) {
			for (int i = 0; i < metaEntries.length; i++) {
				ZipEntry entry = metaEntries[i];
				String entryName = entry.getName();
				if (manifestEntry == null
						&& manifest == null
						&& entryName.regionMatches(true, dirLength,
								MANIFEST_NAME, dirLength, MANIFEST_NAME
										.length()
										- dirLength)) {
					manifestEntry = entry;
					if (verifier == null)
						break;
				} else if (verifier != null
						&& entryName.length() > dirLength
						&& (entryName.regionMatches(true,
								entryName.length() - 3, ".SF", 0, 3)
								|| entryName.regionMatches(true, entryName
										.length() - 4, ".DSA", 0, 4) || entryName
								.regionMatches(true, entryName.length() - 4,
										".RSA", 0, 4))) {
					signed = true;
					if (manifest == null)
						verifier.setManifest(getManifest());
					InputStream is = super.getInputStream(entry);
					byte[] buf = new byte[is.available()];
					is.read(buf, 0, buf.length);
					is.close();
					verifier.addMetaEntry(entryName, buf);
				}
			}
		}
		if (!signed)
			verifier = null;
	}

	/**
	 * Return an InputStream for reading the decompressed contents of ze.
	 * 
	 * @param ze
	 *            the ZipEntry to read from
	 * @return java.io.InputStream
	 * @exception java.io.IOException
	 *                If an error occured while creating the InputStream.
	 */
	public InputStream getInputStream(ZipEntry ze) throws IOException {
		if (manifestEntry != null)
			getManifest();
		if (verifier != null) {
			if (verifier.readCertificates()) {
				verifier.removeMetaEntries();
				if (manifest != null)
					manifest.removeChunks();
				if (!verifier.isSignedJar())
					verifier = null;
			}
		}
		InputStream in = super.getInputStream(ze);
		if (in == null)
			return null;
		return new JarFileInputStream(in, ze, ze.getSize() >= 0 ? verifier
				: null);
	}

	/**
	 * Return the JarEntry specified by name or null if no such entry exists
	 * 
	 * @param name
	 *            the name of the entry in the jar file
	 * @return java.util.jar.JarEntry
	 */
	public ZipEntry getEntry(String name) {
		ZipEntry ze = super.getEntry(name);
		if (ze == null)
			return ze;
		JarEntry je = new JarEntry(ze);
		je.parentJar = this;
		if (verifier != null)
			je.certificates = verifier.getCertificates(je.getName());
		return je;
	}

	private native ZipEntry[] getMetaEntriesImpl(byte[] buf);

}
