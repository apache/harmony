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

package java.util;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.AccessController;

import com.ibm.oti.util.PriviAction;

/**
 * Properties is a Hashtable where the keys and values must be Strings. Each
 * Properties can have a default Properties which specifies the default values
 * which are used if the key is not in this Properties.
 * 
 * @see Hashtable
 * @see java.lang.System#getProperties
 */
public class Properties extends Hashtable {
	
	static final long serialVersionUID = 4112578634029874840L;

	/**
	 * The default values for this Properties.
	 */
	protected Properties defaults;

	private static final int NONE = 0, SLASH = 1, UNICODE = 2, CONTINUE = 3,
			KEY_DONE = 4, IGNORE = 5;

	/**
	 * Constructs a new Properties object.
	 */
	public Properties() {
		super();
	}

	/**
	 * Constructs a new Properties object using the specified default
	 * properties.
	 * 
	 * @param properties
	 *            the default properties
	 */
	public Properties(Properties properties) {
		defaults = properties;
	}

	private void dumpString(StringBuffer buffer, String string, boolean key) {
		int i = 0;
		if (!key && i < string.length() && string.charAt(i) == ' ') {
			buffer.append("\\ "); //$NON-NLS-1$
			i++;
		}

		for (; i < string.length(); i++) {
			char ch = string.charAt(i);
			switch (ch) {
			case '\t':
				buffer.append("\\t"); //$NON-NLS-1$
				break;
			case '\n':
				buffer.append("\\n"); //$NON-NLS-1$
				break;
			case '\f':
				buffer.append("\\f"); //$NON-NLS-1$
				break;
			case '\r':
				buffer.append("\\r"); //$NON-NLS-1$
				break;
			default:
				if ("\\#!=:".indexOf(ch) >= 0 || (key && ch == ' ')) //$NON-NLS-1$
					buffer.append('\\');
				if (ch >= ' ' && ch <= '~') {
					buffer.append(ch);
				} else {
					String hex = Integer.toHexString(ch);
					buffer.append("\\u"); //$NON-NLS-1$
					for (int j = 0; j < 4 - hex.length(); j++)
						buffer.append("0"); //$NON-NLS-1$
					buffer.append(hex);
				}
			}
		}
	}

	/**
	 * Searches for the property with the specified name. If the property is not
	 * found, look in the default properties. If the property is not found in
	 * the default properties, answer null.
	 * 
	 * @param name
	 *            the name of the property to find
	 * @return the named property value
	 */
	public String getProperty(String name) {
		Object result = get(name);
		String property = result instanceof String ? (String) result : null;
		if (property == null && defaults != null) {
			property = defaults.getProperty(name);
		}
		return property;
	}

	/**
	 * Searches for the property with the specified name. If the property is not
	 * found, look in the default properties. If the property is not found in
	 * the default properties, answer the specified default.
	 * 
	 * @param name
	 *            the name of the property to find
	 * @param defaultValue
	 *            the default value
	 * @return the named property value
	 */
	public String getProperty(String name, String defaultValue) {
		Object result = get(name);
		String property = result instanceof String ? (String) result : null;
		if (property == null && defaults != null) {
			property = defaults.getProperty(name);
		}
		if (property == null)
			return defaultValue;
		return property;
	}

	/**
	 * Lists the mappings in this Properties to the specified PrintStream in a
	 * human readable form.
	 * 
	 * @param out
	 *            the PrintStream
	 */
	public void list(PrintStream out) {
		if (out == null)
			throw new NullPointerException();
		StringBuffer buffer = new StringBuffer(80);
		Enumeration keys = propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			buffer.append(key);
			buffer.append('=');
			String property = (String) get(key);
			Properties def = defaults;
			while (property == null) {
				property = (String) def.get(key);
				def = def.defaults;
			}
			if (property.length() > 40) {
				buffer.append(property.substring(0, 37));
				buffer.append("..."); //$NON-NLS-1$
			} else
				buffer.append(property);
			out.println(buffer.toString());
			buffer.setLength(0);
		}
	}

	/**
	 * Lists the mappings in this Properties to the specified PrintWriter in a
	 * human readable form.
	 * 
	 * @param writer
	 *            the PrintWriter
	 */
	public void list(PrintWriter writer) {
		if (writer == null)
			throw new NullPointerException();
		StringBuffer buffer = new StringBuffer(80);
		Enumeration keys = propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			buffer.append(key);
			buffer.append('=');
			String property = (String) get(key);
			Properties def = defaults;
			while (property == null) {
				property = (String) def.get(key);
				def = def.defaults;
			}
			if (property.length() > 40) {
				buffer.append(property.substring(0, 37));
				buffer.append("..."); //$NON-NLS-1$
			} else
				buffer.append(property);
			writer.println(buffer.toString());
			buffer.setLength(0);
		}
	}

	/**
	 * Loads properties from the specified InputStream. The properties are of
	 * the form <code>key=value</code>, one property per line.
	 * 
	 * @param in
	 *            the input stream
	 * @throws IOException 
	 */
	public synchronized void load(InputStream in) throws IOException {
		int mode = NONE, unicode = 0, count = 0;
		char nextChar, buf[] = new char[40];
		int offset = 0, keyLength = -1;
		boolean firstChar = true;
		byte[] inbuf = new byte[256];
		int inbufCount = 0, inbufPos = 0;

		while (true) {
			if (inbufPos == inbufCount) {
				if ((inbufCount = in.read(inbuf)) == -1)
					break;
				inbufPos = 0;
			}
			nextChar = (char) (inbuf[inbufPos++] & 0xff);

			if (offset == buf.length) {
				char[] newBuf = new char[buf.length * 2];
				System.arraycopy(buf, 0, newBuf, 0, offset);
				buf = newBuf;
			}
			if (mode == UNICODE) {
				int digit = Character.digit(nextChar, 16);
				if (digit >= 0) {
					unicode = (unicode << 4) + digit;
					if (++count < 4)
						continue;
				}
				mode = NONE;
				buf[offset++] = (char) unicode;
				if (nextChar != '\n')
					continue;
			}
			if (mode == SLASH) {
				mode = NONE;
				switch (nextChar) {
				case '\r':
					mode = CONTINUE; // Look for a following \n
					continue;
				case '\n':
					mode = IGNORE; // Ignore whitespace on the next line
					continue;
				case 'b':
					nextChar = '\b';
					break;
				case 'f':
					nextChar = '\f';
					break;
				case 'n':
					nextChar = '\n';
					break;
				case 'r':
					nextChar = '\r';
					break;
				case 't':
					nextChar = '\t';
					break;
				case 'u':
					mode = UNICODE;
					unicode = count = 0;
					continue;
				}
			} else {
				switch (nextChar) {
				case '#':
				case '!':
					if (firstChar) {
						while (true) {
							if (inbufPos == inbufCount) {
								if ((inbufCount = in.read(inbuf)) == -1) {
									inbufPos = -1;
									break;
								}
								inbufPos = 0;
							}
							nextChar = (char) inbuf[inbufPos++]; // & 0xff
																	// not
																	// required
							if (nextChar == '\r' || nextChar == '\n')
								break;
						}
						continue;
					}
					break;
				case '\n':
					if (mode == CONTINUE) { // Part of a \r\n sequence
						mode = IGNORE; // Ignore whitespace on the next line
						continue;
					}
				// fall into the next case
				case '\r':
					mode = NONE;
					firstChar = true;
					if (offset > 0) {
						if (keyLength == -1) {
							keyLength = offset;
						}
						String temp = new String(buf, 0, offset);
						put(temp.substring(0, keyLength), temp
								.substring(keyLength));
					}
					keyLength = -1;
					offset = 0;
					continue;
				case '\\':
					if (mode == KEY_DONE) {
						keyLength = offset;
					}
					mode = SLASH;
					continue;
				case ':':
				case '=':
					if (keyLength == -1) { // if parsing the key
						mode = NONE;
						keyLength = offset;
						continue;
					}
					break;
				}
				if (Character.isWhitespace(nextChar)) {
					if (mode == CONTINUE)
						mode = IGNORE;
					// if key length == 0 or value length == 0
					if (offset == 0 || offset == keyLength || mode == IGNORE)
						continue;
					if (keyLength == -1) { // if parsing the key
						mode = KEY_DONE;
						continue;
					}
				}
				if (mode == IGNORE || mode == CONTINUE)
					mode = NONE;
			}
			firstChar = false;
			if (mode == KEY_DONE) {
				keyLength = offset;
				mode = NONE;
			}
			buf[offset++] = nextChar;
		}
		if (keyLength >= 0) {
			String temp = new String(buf, 0, offset);
			put(temp.substring(0, keyLength), temp.substring(keyLength));
		}
	}

	/**
	 * Answers all of the property names that this Properties contains.
	 * 
	 * @return an Enumeration containing the names of all properties
	 */
	public Enumeration propertyNames() {
		if (defaults == null)
			return keys();

		Hashtable set = new Hashtable(defaults.size() + size());
		Enumeration keys = defaults.propertyNames();
		while (keys.hasMoreElements()) {
			set.put(keys.nextElement(), set);
		}
		keys = keys();
		while (keys.hasMoreElements()) {
			set.put(keys.nextElement(), set);
		}
		return set.keys();
	}

	/**
	 * Saves the mappings in this Properties to the specified OutputStream,
	 * putting the specified comment at the beginning. The output from this
	 * method is suitable for being read by the load() method.
	 * 
	 * @param out
	 *            the OutputStream
	 * @param comment
	 *            the comment
	 * 
	 * @exception ClassCastException
	 *                when the key or value of a mapping is not a String
	 * 
	 * @deprecated Does not throw an IOException, use store()
	 */
	public void save(OutputStream out, String comment) {
		try {
			store(out, comment);
		} catch (IOException e) {
		}
	}

	/**
	 * Maps the specified key to the specified value. If the key already exists,
	 * the old value is replaced. The key and value cannot be null.
	 * 
	 * @param name
	 *            the key
	 * @param value
	 *            the value
	 * @return the old value mapped to the key, or null
	 */
	public synchronized Object setProperty(String name, String value) {
		return put(name, value);
	}

	private static String lineSeparator;

	/**
	 * Stores the mappings in this Properties to the specified OutputStream,
	 * putting the specified comment at the beginning. The output from this
	 * method is suitable for being read by the load() method.
	 * 
	 * @param out
	 *            the OutputStream
	 * @param comment
	 *            the comment
	 * @throws IOException 
	 * 
	 * @exception ClassCastException
	 *                when the key or value of a mapping is not a String
	 */
	public synchronized void store(OutputStream out, String comment)
			throws IOException {
		if (lineSeparator == null)
			lineSeparator = (String) AccessController
					.doPrivileged(new PriviAction("line.separator")); //$NON-NLS-1$

		StringBuffer buffer = new StringBuffer(200);
		OutputStreamWriter writer = new OutputStreamWriter(out, "ISO8859_1"); //$NON-NLS-1$
		if (comment != null)
			writer.write("#" + comment + lineSeparator); //$NON-NLS-1$
		writer.write("#" + new Date() + lineSeparator); //$NON-NLS-1$
		Iterator entryItr = entrySet().iterator();
		while (entryItr.hasNext()) {
			MapEntry entry = (MapEntry) entryItr.next();
			String key = (String) entry.getKey();
			dumpString(buffer, key, true);
			buffer.append('=');
			dumpString(buffer, (String) entry.getValue(), false);
			buffer.append(lineSeparator);
			writer.write(buffer.toString());
			buffer.setLength(0);
		}
		writer.flush();
	}

}
