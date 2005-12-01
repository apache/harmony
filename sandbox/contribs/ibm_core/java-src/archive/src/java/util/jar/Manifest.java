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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.oti.util.PriviAction;

/**
 * The Manifest class is used to obtain attribute information for a JarFile and
 * it's entries.
 * 
 */
public class Manifest implements Cloneable {
	private Attributes mainAttributes = new Attributes();

	private HashMap entryAttributes = new HashMap();

	private HashMap chunks;

	/**
	 * Contructs a new Manifest instance.
	 */
	public Manifest() {
	}

	/**
	 * Contructs a new Manifest instance using the attributes obtained from is.
	 * 
	 * @param is
	 *            InputStream to parse for attributes
	 * 
	 * @throws IOException
	 *             if an IO error occurs while creating this Manifest
	 * 
	 */
	public Manifest(InputStream is) throws IOException {
		read(is);
	}

	Manifest(InputStream is, boolean readChunks) throws IOException {
		if (readChunks)
			chunks = new HashMap();
		read(is);
	}

	/**
	 * Resets the both the mainAttributes as well as the entry Attributes
	 * associated with this Manifest.
	 */
	public void clear() {
		entryAttributes.clear();
		mainAttributes.clear();
	}

	/**
	 * Returns the Attributes associated with the parameter entry name
	 * 
	 * @param name
	 *            The name of the entry to obtain Attributes for.
	 * @return The Attributes for the entry or null if the entry does not exist.
	 */
	public Attributes getAttributes(String name) {
		return (Attributes) getEntries().get(name);
	}

	/**
	 * Returns a Map containing the Attributes for each entry in the Manifest.
	 * 
	 * @return A Map of entry attributes
	 */
	public Map getEntries() {
		return entryAttributes;
	}

	/**
	 * Returns the main Attributes of the JarFile.
	 * 
	 * @return Main Attributes associated with the source JarFile
	 */
	public Attributes getMainAttributes() {
		return mainAttributes;
	}

	/**
	 * Contructs a new Manifest instance. The new instance will have the same
	 * attributes as those found in the parameter Manifest.
	 * 
	 * @param man
	 *            Manifest instance to obtain attributes from
	 */
	public Manifest(Manifest man) {
		mainAttributes = (Attributes) man.mainAttributes.clone();
		entryAttributes = (HashMap) man.entryAttributes.clone();
	}

	/**
	 * Creates a copy of this Manifest. The returned Manifest will equal the
	 * Manifest from which it was cloned.
	 * 
	 * @return A copy of the receiver.
	 */
	public Object clone() {
		return new Manifest(this);
	}

	static class WriteManifest {
		private static final int LIMIT = 70;

		private static byte[] sepBuf = new byte[] { '\r', '\n' };

		private static Attributes.Name nameAttribute = new Attributes.Name(
				"Name", false);

		byte[] oneByte = new byte[1];

		char[] oneChar = new char[1];

		private Charset charset;

		private byte[] outBuf = new byte[LIMIT];

		OutputStream os;

		/**
		 * Writes out a manifest entry.
		 */
		private void writeEntry(Attributes.Name name, String value)
				throws IOException {
			int offset = 0, limit = LIMIT;
			byte[] out = (name.toString() + ": ").getBytes("ISO8859_1");
			if (out.length > limit) {
				while (out.length - offset >= limit) {
					int len = out.length - offset;
					if (len > limit)
						len = limit;
					if (offset > 0)
						os.write(' ');
					os.write(out, offset, len);
					os.write(sepBuf);
					offset += len;
					limit = LIMIT - 1;
				}
			}
			int size = out.length - offset;
			System.arraycopy(out, offset, outBuf, 0, size);
			for (int i = 0; i < value.length(); i++) {
				oneChar[0] = value.charAt(i);
				byte[] buf;
				if (oneChar[0] < 128 || charset == null) {
					oneByte[0] = (byte) oneChar[0];
					buf = oneByte;
				} else
					buf = charset.encode(CharBuffer.wrap(oneChar, 0, 1)).array();
				if (size + buf.length > limit) {
					if (limit != LIMIT)
						os.write(' ');
					os.write(outBuf, 0, size);
					os.write(sepBuf);
					limit = LIMIT - 1;
					size = 0;
				}
				if (buf.length == 1)
					outBuf[size] = buf[0];
				else
					System.arraycopy(buf, 0, outBuf, size, buf.length);
				size += buf.length;
			}
			if (size > 0) {
				if (limit != LIMIT)
					os.write(' ');
				os.write(outBuf, 0, size);
				os.write(sepBuf);
			}
		}

		/**
		 * Writes out the attribute information of the receiver to the specified
		 * OutputStream
		 * 
		 * 
		 * @param manifest
		 *            the attribute information of the receiver
		 * @param out
		 *            The OutputStream to write to.
		 * 
		 * @throws IOException
		 *             If an error occurs writing the Manifest
		 */
		void write(Manifest manifest, OutputStream out) throws IOException {
			os = out;
			String encoding = (String) AccessController
					.doPrivileged(new PriviAction("manifest.write.encoding"));
			if (encoding != null) {
				if ("".equals(encoding))
					encoding = "UTF8";
				charset = Charset.forName(encoding);				
			}
			String version = manifest.mainAttributes
					.getValue(Attributes.Name.MANIFEST_VERSION);
			if (version != null) {
				writeEntry(Attributes.Name.MANIFEST_VERSION, version);
				Iterator entries = manifest.mainAttributes.keySet().iterator();
				while (entries.hasNext()) {
					Attributes.Name name = (Attributes.Name) entries.next();
					if (!name.equals(Attributes.Name.MANIFEST_VERSION))
						writeEntry(name, manifest.mainAttributes.getValue(name));
				}
			}
			os.write(sepBuf);
			Iterator i = manifest.entryAttributes.keySet().iterator();
			while (i.hasNext()) {
				String key = (String) i.next();
				writeEntry(nameAttribute, key);
				Attributes attrib = (Attributes) manifest.entryAttributes
						.get(key);
				Iterator entries = attrib.keySet().iterator();
				while (entries.hasNext()) {
					Attributes.Name name = (Attributes.Name) entries.next();
					writeEntry(name, attrib.getValue(name));
				}
				os.write(sepBuf);
			}
		}
	}

	/**
	 * Writes out the attribute information of the receiver to the specified
	 * OutputStream
	 * 
	 * @param os
	 *            The OutputStream to write to.
	 * 
	 * @throws IOException
	 *             If an error occurs writing the Manifest
	 */
	public void write(OutputStream os) throws IOException {
		new WriteManifest().write(this, os);
	}

	/**
	 * Contructs a new Manifest instance obtaining Attribute information from
	 * the paramter InputStream.
	 * 
	 * @param is
	 *            The InputStream to read from
	 * @throws IOException
	 *             If an error occurs reading the Manifest.
	 */
	public void read(InputStream is) throws IOException {
		new InitManifest(is, mainAttributes, entryAttributes, chunks, null);
	}

	/**
	 * Returns the hashCode for this instance.
	 * 
	 * @return This Manifest's hashCode
	 */
	public int hashCode() {
		return mainAttributes.hashCode() ^ entryAttributes.hashCode();
	}

	/**
	 * Determines if the receiver is equal to the parameter Object. Two
	 * Manifests are equal if they have identical main Attributes as well as
	 * identical entry Attributes.
	 * 
	 * @param o
	 *            The Object to compare against.
	 * @return <code>true</code> if the manifests are equal,
	 *         <code>false</code> otherwise
	 */
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o.getClass() != this.getClass())
			return false;
		if (!mainAttributes.equals(((Manifest) o).mainAttributes))
			return false;
		return entryAttributes.equals(((Manifest) o).entryAttributes);
	}

	byte[] getChunk(String name) {
		return (byte[]) chunks.get(name);
	}

	void removeChunks() {
		chunks = null;
	}
}
