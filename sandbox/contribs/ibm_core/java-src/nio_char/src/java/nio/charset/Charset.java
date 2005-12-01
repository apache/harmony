/* Copyright 2004 The Apache Software Foundation or its licensors, as applicable
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

package java.nio.charset;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.spi.CharsetProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.ibm.icu4jni.charset.CharsetProviderICU;

/**
 * A charset defines a mapping between a Unicode character sequence and a byte
 * sequence. It facilitate the encoding from a Unicode character sequence into a
 * byte sequence, and the decoding from a byte sequence into a Unicode character
 * sequence.
 * <p>
 * A charset has a canonical name, which are usually in uppercase. Typically it
 * also has one or more aliases. The name string can only consist of the
 * following characters: '0' - '9', 'A' - 'Z', 'a' - 'z', '.', ':'. '-' and '_'.
 * The first character of the name must be a digit or a letter.
 * </p>
 * <p>
 * The following charsets should be supported by any java platforms: US-ASCII,
 * ISO-8859-1, UTF-8, UTF-16BE, UTF-16LE, UTF-16.
 * </p>
 * <p>
 * Additional charsets can be made available by configuring one or more charset
 * providers through provider configuration files. Such files are always named
 * as "java.nio.charset.spi.CharsetProvider" and located in the
 * "META-INF/services" sub folder of one or more classpaths. The files should be
 * encoded in "UTF-8". Each line of their content specifies the class name of a
 * charset provider which extends <code>java.nio.spi.CharsetProvider</code>.
 * A line should ends with '\r', '\n' or '\r\n'. Leading and trailing
 * whitespaces are trimed. Blank lines, and lines (after trimed) starting with
 * "#" which are regarded as comments, are both ignored. Duplicates of already
 * appeared names are also ignored. Both the configuration files and the
 * provider classes will be loaded using the thread context class loader.
 * </p>
 * <p>
 * This class is thread-safe.
 * </p>
 * 
 * @see java.nio.charset.spi.CharsetProvider
 * 
 */
public abstract class Charset implements Comparable {

	/*
	 * --------------------------------------------------------------------
	 * Constants
	 * --------------------------------------------------------------------
	 */

	/*
	 * the name of configuration files where charset provider class names can be
	 * specified.
	 */
	private static final String PROVIDER_CONFIGURATION_FILE_NAME = "META-INF/services/java.nio.charset.spi.CharsetProvider"; //$NON-NLS-1$

	/*
	 * the encoding of configuration files
	 */
	private static final String PROVIDER_CONFIGURATION_FILE_ENCODING = "UTF-8"; //$NON-NLS-1$

	/*
	 * the comment string used in configuration files
	 */
	private static final String PROVIDER_CONFIGURATION_FILE_COMMENT = "#"; //$NON-NLS-1$

	/*
	 * --------------------------------------------------------------------
	 * Class variables
	 * --------------------------------------------------------------------
	 */

	// the shared pattern used to check charset names
	// private static Pattern _charsetNamePattern = Pattern
	// .compile("[0-9A-Za-z][0-9A-Za-z\\x2e\\x3a\\x2d\\x5f]*"); //$NON-NLS-1$
	// built in provider instance, assuming thread-safe
	private static CharsetProviderICU _builtInProvider = null;

	// cached built in charsets
	private static TreeMap _builtInCharsets = null;

	/*
	 * --------------------------------------------------------------------
	 * Instance variables
	 * --------------------------------------------------------------------
	 */

	// a cached instance of encoder for each thread
	ThreadLocal cachedEncoder = new ThreadLocal() {
		protected synchronized Object initialValue() {
			CharsetEncoder e = newEncoder();
			e.onMalformedInput(CodingErrorAction.REPLACE);
			e.onUnmappableCharacter(CodingErrorAction.REPLACE);
			return e;
		}
	};

	// a cached instance of decoder for each thread
	ThreadLocal cachedDecoder = new ThreadLocal() {
		protected synchronized Object initialValue() {
			CharsetDecoder d = newDecoder();
			d.onMalformedInput(CodingErrorAction.REPLACE);
			d.onUnmappableCharacter(CodingErrorAction.REPLACE);
			return d;
		}
	};

	private final String canonicalName;

	// the aliases set
	private final HashSet aliasesSet;

	/*
	 * -------------------------------------------------------------------
	 * Global initialization
	 * -------------------------------------------------------------------
	 */
	static {
		/*
		 * create built-in charset provider even if no privilege to access
		 * charset provider.
		 */
		_builtInProvider = (CharsetProviderICU) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return new CharsetProviderICU();
					}
				});
	}

	/*
	 * -------------------------------------------------------------------
	 * Constructors
	 * -------------------------------------------------------------------
	 */

	/**
	 * Constructs a <code>Charset</code> object. Duplicated aliases are
	 * ignored.
	 * 
	 * @param canonicalName
	 *            the canonical name of the charset
	 * @param aliases
	 *            an array containing all aliases of the charset
	 * @throws IllegalCharsetNameException
	 *             on an illegal value being supplied for either
	 *             <code>canonicalName</code> or for any element of
	 *             <code>aliases</code>.
	 * 
	 */
	protected Charset(String canonicalName, String[] aliases)
			throws IllegalCharsetNameException {
		// check whether the given canonical name is legal
		checkCharsetName(canonicalName);
		this.canonicalName = canonicalName;
		// check each aliase and put into a set
		this.aliasesSet = new HashSet();
		if (null != aliases) {
			for (int i = 0; i < aliases.length; i++) {
				checkCharsetName(aliases[i]);
				this.aliasesSet.add(aliases[i]);
			}
		}
	}

	/*
	 * -------------------------------------------------------------------
	 * Methods
	 * -------------------------------------------------------------------
	 */

	/*
	 * Checks whether a character is a special character that can be used in
	 * charset names, other than letters and digits.
	 */
	private static boolean isSpecial(char c) {
		return ('-' == c || '.' == c || ':' == c || '_' == c);
	}

	/*
	 * Checks whether a given string is a legal charset name. To be compatible
	 * with JDK's behavior, allow empty strings and strings not starting with a
	 * letter or digit. These are not allowed according to the 1.4.2 javadoc.
	 */
	private static void checkCharsetName(String name) {
		boolean isDigit;
		boolean isLetter;
		int length = name.length();
		for (int i = 0; i < length; i++) {
			char c = name.charAt(i);
			isLetter = ('a' <= c && c <= 'z' || 'A' <= c && c <= 'Z');
			isDigit = ('0' <= c && c <= '9');
			if (isDigit || isLetter || isSpecial(c)) {
				continue;
			}
			throw new IllegalCharsetNameException(name);
		}
	}

	/*
	 * Use privileged code to get the context class loader.
	 */
	private static ClassLoader getContextClassLoader() {
		final Thread t = Thread.currentThread();
		return (ClassLoader) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return t.getContextClassLoader();
					}
				});
	}

	/*
	 * Add the charsets supported by the given provider to the map.
	 */
	private static void addCharsets(CharsetProvider cp, TreeMap charsets) {
		Iterator it = cp.charsets();
		while (it.hasNext()) {
			Charset cs = (Charset) it.next();
			// Only new charsets will be added
			if (!charsets.containsKey(cs.name())) {
				charsets.put(cs.name(), cs);
			}
		}
	}

	/*
	 * Read a configuration file and add the charsets supported by the providers
	 * specified by this configuration file to the map.
	 */
	private static void loadConfiguredCharsets(URL configFile, ClassLoader cl,
			TreeMap charsets) {
		BufferedReader reader = null;
		try {
			InputStream is = configFile.openStream();
			// Read each line for charset provider class names
			reader = new BufferedReader(new InputStreamReader(is,
					PROVIDER_CONFIGURATION_FILE_ENCODING));
			String providerClassName = reader.readLine();
			while (null != providerClassName) {
				// Trim leading and trailing whitespaces
				providerClassName = providerClassName.trim();
				// Skip comments and blank lines
				if (!providerClassName
						.startsWith(PROVIDER_CONFIGURATION_FILE_COMMENT)
						&& !"".equals(providerClassName)) { //$NON-NLS-1$
					// Load the charset provider
					Object cp = null;
					try {
						Class c = Class.forName(providerClassName, true, cl);
						cp = c.newInstance();
					} catch (SecurityException ex) {
						// assume no permission to use charset provider
						throw ex;
					} catch (Exception ex) {
						throw new Error(ex.getMessage(), ex);
					}
					// Put the charsets supported by this provider into the map
					addCharsets((CharsetProvider) cp, charsets);
				}
				// Read the next line of the config file
				providerClassName = reader.readLine();
			}
		} catch (IOException ex) {
			// Can't read this configuration file, ignore
		} finally {
			try {
				if (null != reader) {
					reader.close();
				}
			} catch (IOException ex) {
				// Ignore closing exception
			}
		}
	}

	/**
	 * Gets a map of all available charsets supported by the runtime.
	 * <p>
	 * The returned map contains mappings from canonical names to corresponding
	 * instances of <code>Charset</code>. The canonical names can be
	 * considered as case-insensitive.
	 * </p>
	 * 
	 * @return an unmodifiable map of all available charsets supported by the
	 *         runtime
	 */
	public static SortedMap availableCharsets() {
		// Initialize the built-in charsets map cache if necessary
		if (null == _builtInCharsets) {
			synchronized (Charset.class) {
				if (null == _builtInCharsets) {
					_builtInCharsets = new TreeMap(IgnoreCaseComparator
							.getInstance());
					_builtInProvider.putCharsets(_builtInCharsets);
				}
			}
		}

		// Add built-in charsets
		TreeMap charsets = (TreeMap) _builtInCharsets.clone();

		// Collect all charsets provided by charset providers
		final ClassLoader cl = getContextClassLoader();
		if (null != cl) {
			try {
				// Load all configuration files
				Enumeration e = cl
						.getResources(PROVIDER_CONFIGURATION_FILE_NAME);
				// Examine each configuration file
				while (e.hasMoreElements()) {
					loadConfiguredCharsets((URL) e.nextElement(), cl, charsets);
				}
			} catch (IOException ex) {
				// Unexpected ClassLoader exception, ignore
			}
		}

		return Collections.unmodifiableSortedMap(charsets);
	}

	/*
	 * Read a configuration file and try to find the desired charset among those
	 * which are supported by the providers specified in this configuration
	 * file.
	 */
	private static Charset searchConfiguredCharsets(String charsetName,
			URL configFile, ClassLoader cl) {
		BufferedReader reader = null;
		try {
			InputStream is = configFile.openStream();
			// Read each line for charset provider class names
			reader = new BufferedReader(new InputStreamReader(is,
					PROVIDER_CONFIGURATION_FILE_ENCODING));
			String providerClassName = reader.readLine();
			while (null != providerClassName) {
				// Trim leading and trailing whitespaces
				providerClassName = providerClassName.trim();
				// Skip comments and blank lines
				if (!providerClassName
						.startsWith(PROVIDER_CONFIGURATION_FILE_COMMENT)
						&& !"".equals(providerClassName)) { //$NON-NLS-1$
					// Load the charset provider
					Object cp = null;
					try {
						Class c = Class.forName(providerClassName, true, cl);
						cp = c.newInstance();
					} catch (SecurityException ex) {
						// assume no permission to use charset provider
						throw ex;
					} catch (Exception ex) {
						throw new Error(ex.getMessage(), ex);
					}
					// Try to get the desired charset from this provider
					Charset cs = ((CharsetProvider) cp)
							.charsetForName(charsetName);
					if (null != cs) {
						return cs;
					}
				}
				// Read the next line of the config file
				providerClassName = reader.readLine();
			}
			return null;
		} catch (IOException ex) {
			// Can't read this configuration file
			return null;
		} finally {
			try {
				if (null != reader) {
					reader.close();
				}
			} catch (IOException ex) {
				// Ignore closing exception
			}
		}
	}

	/*
	 * Gets a <code> Charset </code> instance for the specified charset name. If
	 * the charset is not supported, returns null instead of throwing an
	 * exception.
	 */
	private static Charset forNameInternal(String charsetName)
			throws IllegalCharsetNameException {
		checkCharsetName(charsetName);
		// Try built-in charsets
		Charset cs = _builtInProvider.charsetForName(charsetName);
		if (null != cs) {
			return cs;
		}

		// Collect all charsets provided by charset providers
		final ClassLoader cl = getContextClassLoader();
		if (null != cl) {
			try {
				// Load all configuration files
				Enumeration e = cl
						.getResources(PROVIDER_CONFIGURATION_FILE_NAME);
				// Examine each configuration file
				while (e.hasMoreElements()) {
					cs = searchConfiguredCharsets(charsetName, (URL) e
							.nextElement(), cl);
					if (null != cs) {
						return cs;
					}
				}
			} catch (IOException ex) {
				// Unexpected ClassLoader exception, ignore
			}
		}
		return null;
	}

	/**
	 * Gets a <code>Charset</code> instance for the specified charset name.
	 * 
	 * @param charsetName
	 *            the name of the charset
	 * @return a <code>Charset</code> instance for the specified charset name
	 * @throws IllegalCharsetNameException
	 *             If the specified charset name is illegal.
	 * @throws UnsupportedCharsetException
	 *             If the desired charset is not supported by this runtime.
	 */
	public static Charset forName(String charsetName)
			throws IllegalCharsetNameException, UnsupportedCharsetException {
		/*
		 * The following ugly checks try to keep compatibility with JDK. It
		 * rejects empty names and names starting with a non-letter&digit
		 * character.
		 */
		if (null == charsetName || "".equals(charsetName)) { //$NON-NLS-1$
			throw new IllegalArgumentException();
		}
		char initial = charsetName.charAt(0);
		if (isSpecial(initial)) {
			throw new IllegalArgumentException();
		}

		Charset c = forNameInternal(charsetName);
		if (null != c) {
			return c;
		}
		throw new UnsupportedCharsetException(charsetName);
	}

	/**
	 * Determines whether the specified charset is supported by this runtime.
	 * 
	 * @param charsetName
	 *            the name of the charset
	 * @return true if the specified charset is supported, otherwise false
	 * @throws IllegalCharsetNameException
	 *             If the specified charset name is illegal.
	 */
	public static boolean isSupported(String charsetName)
			throws IllegalCharsetNameException {
		if (null == charsetName) {
			throw new IllegalCharsetNameException(charsetName);
		}
		return null != forNameInternal(charsetName);
	}

	/**
	 * Determines whether this charset is a super set of the given charset.
	 * 
	 * @param charset
	 *            a given charset
	 * @return true if this charset is a super set of the given charset,
	 *         otherwise false
	 */
	public abstract boolean contains(Charset charset);

	/**
	 * Gets a new instance of encoder for this charset.
	 * 
	 * @return a new instance of encoder for this charset
	 */
	public abstract CharsetEncoder newEncoder();

	/**
	 * Gets a new instance of decoder for this charset.
	 * 
	 * @return a new instance of decoder for this charset
	 */
	public abstract CharsetDecoder newDecoder();

	/**
	 * Gets the canonical name of this charset.
	 * 
	 * @return this charset's name in canonical form. 
	 */
	public final String name() {
		return this.canonicalName;
	}

	/**
	 * Gets the set of this charset's aliases.
	 * 
	 * @return an unmodifiable set of this charset's aliases
	 */
	public final Set aliases() {
		return Collections.unmodifiableSet(this.aliasesSet);
	}

	/**
	 * Gets the name of this charset for the default locale.
	 * 
	 * @return the name of this charset for the default locale
	 */
	public String displayName() {
		return this.canonicalName;
	}

	/**
	 * Gets the name of this charset for the specified locale.
	 * 
	 * @param l
	 *            a certain locale
	 * @return the name of this charset for the specified locale
	 */
	public String displayName(Locale l) {
		return this.canonicalName;
	}

	/**
	 * Answers whether this charset is registered in the IANA Charset Registry.
	 * 
	 * @return true
	 */
	public final boolean isRegistered() {
		return true;
	}

	/**
	 * Answers true if this charset supports encoding, otherwise false.
	 * 
	 * @return true
	 */
	public boolean canEncode() {
		return true;
	}

	/**
	 * Encodes the content of the give character buffer and outputs to a byte
	 * buffer that is to be returned.
	 * <p>
	 * The default action in case of encoding errors is
	 * <code>CodingErrorAction.REPLACE</code>.
	 * </p>
	 * 
	 * @param buffer
	 *            the character buffer containing the content to be encoded
	 * @return the result of the encoding
	 */
	public final ByteBuffer encode(CharBuffer buffer) {
		CharsetEncoder e = (CharsetEncoder) this.cachedEncoder.get();
		try {
			return e.encode(buffer);
		} catch (CharacterCodingException ex) {
			throw new Error(ex.getMessage(), ex);
		}
	}

	/**
	 * Encodes a string and outputs to a byte buffer that is to be retured.
	 * <p>
	 * The default action in case of encoding errors is
	 * <code>CodingErrorAction.REPLACE</code>.
	 * </p>
	 * 
	 * @param s
	 *            the string to be encoded
	 * @return the result of the encoding
	 */
	public final ByteBuffer encode(String s) {
		return encode(CharBuffer.wrap(s));
	}

	/**
	 * Decodes the content of the give byte buffer and outputs to a character
	 * buffer that is to be retured.
	 * <p>
	 * The default action in case of decoding errors is
	 * <code>CodingErrorAction.REPLACE</code>.
	 * </p>
	 * 
	 * @param buffer
	 *            the byte buffer containing the content to be decoded
	 * @return a character buffer containing the output of the dencoding
	 */
	public final CharBuffer decode(ByteBuffer buffer) {
		CharsetDecoder d = (CharsetDecoder) this.cachedDecoder.get();
		try {
			return d.decode(buffer);
		} catch (CharacterCodingException ex) {
			throw new Error(ex.getMessage(), ex);
		}
	}

	/*
	 * -------------------------------------------------------------------
	 * Methods implementing parent interface Comparable
	 * -------------------------------------------------------------------
	 */

	/**
	 * Compares this charset with the given charset.
	 * 
	 * @param obj
	 *            the given object to be compared with
	 * @return a negative integer if less than the given object, a positive
	 *         integer if larger than it, or 0 if equal to it
	 */
	public final int compareTo(Object obj) {
		Charset that = (Charset) obj;
		return this.canonicalName.compareToIgnoreCase(that.canonicalName);
	}

	/*
	 * -------------------------------------------------------------------
	 * Methods overriding parent class Object
	 * -------------------------------------------------------------------
	 */

	/**
	 * Determines whether this charset equals to the given object. They are
	 * considered to be equal if they have the same canonical name.
	 * 
	 * @param obj
	 *            the given object to be compared with
	 * @return true if they have the same canonical name, otherwise false
	 */
	public final boolean equals(Object obj) {
		if (obj instanceof Charset) {
			Charset that = (Charset) obj;
			return this.canonicalName.equals(that.canonicalName);
		}
		return false;
	}

	/**
	 * Gets the hash code of this charset.
	 * 
	 * @return the hash code of this charset
	 */
	public final int hashCode() {
		return this.canonicalName.hashCode();
	}

	/**
	 * Gets a string representation of this charset. Usually this contains the
	 * canonical name of the charset.
	 * 
	 * @return a string representation of this charset
	 */
	public final String toString() {
		return "Charset[" + this.canonicalName + "]"; //$NON-NLS-1$//$NON-NLS-2$
	}

	/**
	 * A comparator that ignores case.
	 */
	static class IgnoreCaseComparator implements Comparator {

		// the singleton
		private static Comparator c = new IgnoreCaseComparator();

		/*
		 * Default constructor.
		 */
		private IgnoreCaseComparator() {
			// no action
		}

		/*
		 * Gets a single instance.
		 */
		public static Comparator getInstance() {
			return c;
		}

		/*
		 * Compares two strings ignoring case.
		 */
		public int compare(Object obj1, Object obj2) {
			String s1 = (String) obj1;
			String s2 = (String) obj2;

			return s1.compareToIgnoreCase(s2);
		}
	}
}
