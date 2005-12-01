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


import java.io.IOException;
import java.security.cert.Certificate;
import java.util.zip.ZipEntry;

public class JarEntry extends ZipEntry {
	private Attributes attributes;

	JarFile parentJar;

	Certificate certificates[];

	/**
	 * Create a new JarEntry named name
	 * 
	 * @param name
	 *            The name of the new JarEntry
	 */
	public JarEntry(String name) {
		super(name);
	}

	/**
	 * Create a new JarEntry using the values obtained from entry.
	 * 
	 * @param entry
	 *            The ZipEntry to obtain values from.
	 */
	public JarEntry(ZipEntry entry) {
		super(entry);
	}

	/**
	 * Returns the Attributes object associated with this entry or null if none
	 * exists.
	 * 
	 * @return java.util.jar.Attributes Attributes for this entry
	 * @exception java.io.IOException
	 *                If an error occurs obtaining the Attributes
	 */
	public Attributes getAttributes() throws IOException {
		if (attributes != null || parentJar == null)
			return attributes;
		Manifest manifest = parentJar.getManifest();
		if (manifest == null)
			return null;
		return attributes = manifest.getAttributes(getName());
	}

	/**
	 * Returns an array of Certificate Objects associated with this entry or
	 * null if none exist.
	 * 
	 * @return java.security.cert.Certificate[] Certificates for this entry
	 */
	public Certificate[] getCertificates() {
		return certificates;
	}

	void setAttributes(Attributes attrib) {
		attributes = attrib;
	}

	/**
	 * Create a new JarEntry using the values obtained from je.
	 * 
	 * @param je
	 *            The JarEntry to obtain values from
	 */
	public JarEntry(JarEntry je) {
		super(je);
		parentJar = je.parentJar;
		attributes = je.attributes;
		certificates = je.certificates;
	}
}
