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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarInputStream extends ZipInputStream {

	private Manifest manifest;

	private boolean eos = false;

	private JarEntry mEntry;

	private JarEntry jarEntry;

	private boolean isMeta;

	private JarVerifier verifier;

	private OutputStream verStream;

	/**
	 * Constructs a new JarInputStream from stream
	 */
	public JarInputStream(InputStream stream, boolean verify)
			throws IOException {
		super(stream);
		if (verify)
			verifier = new JarVerifier("JarInputStream");
		if ((mEntry = getNextJarEntry()) == null)
			return;
		String name = mEntry.getName().toUpperCase();
		if (name.equals(JarFile.META_DIR)) {
			mEntry = null; // modifies behavior of getNextJarEntry()
			closeEntry();
			mEntry = getNextJarEntry();
			name = mEntry.getName().toUpperCase();
		}
		if (name.equals(JarFile.MANIFEST_NAME)) {
			mEntry = null;
			manifest = new Manifest(this, verify);
			closeEntry();
			if (verify)
				verifier.setManifest(manifest);
		} else {
			Attributes temp = new Attributes(3);
			temp.map.put("hidden", null);
			mEntry.setAttributes(temp);
		}
	}

	public JarInputStream(InputStream stream) throws IOException {
		this(stream, true);
	}

	/**
	 * Returns the Manifest object associated with this JarInputStream or null
	 * if no manifest entry exists.
	 * 
	 * @return java.util.jar.Manifest
	 */
	public Manifest getManifest() {
		return manifest;
	}

	/**
	 * Returns the next JarEntry contained in this stream or null if no more
	 * entries are present.
	 * 
	 * @return java.util.jar.JarEntry
	 * @exception java.io.IOException
	 *                If an error occurs while reading the entry
	 */
	public JarEntry getNextJarEntry() throws IOException {
		return (JarEntry) getNextEntry();
	}

	public int read(byte[] buffer, int offset, int length) throws IOException {
		if (mEntry != null)
			return -1;
		int r = super.read(buffer, offset, length);
		if (verStream != null && !eos) {
			if (r == -1) {
				eos = true;
				if (isMeta) {
					verifier.addMetaEntry(jarEntry.getName(),
							((ByteArrayOutputStream) verStream).toByteArray());
					verifier.readCertificates();
				} else
					verifier.verifySignatures(
							(JarVerifier.VerifierEntry) verStream, jarEntry);
			} else
				verStream.write(buffer, offset, r);
		}
		return r;
	}

	/**
	 * Returns the next ZipEntry contained in this stream or null if no more
	 * entries are present.
	 * 
	 * @return java.util.zip.ZipEntry
	 * @exception java.io.IOException
	 *                If an error occurs while reading the entry
	 */
	public ZipEntry getNextEntry() throws IOException {
		eos = false;
		if (mEntry != null) {
			jarEntry = mEntry;
			mEntry = null;
			jarEntry.setAttributes(null);
			return jarEntry;
		}
		jarEntry = (JarEntry) super.getNextEntry();
		if (jarEntry == null)
			return null;
		if (verifier != null) {
			isMeta = jarEntry.getName().toUpperCase().startsWith(
					JarFile.META_DIR);
			if (isMeta) {
				verStream = new ByteArrayOutputStream();
			} else {
				verStream = verifier.initEntry(jarEntry.getName());
			}
		}
		return jarEntry;
	}

	protected ZipEntry createZipEntry(String name) {
		JarEntry entry = new JarEntry(name);
		if (manifest != null)
			entry.setAttributes(manifest.getAttributes(name));
		return entry;
	}
}
