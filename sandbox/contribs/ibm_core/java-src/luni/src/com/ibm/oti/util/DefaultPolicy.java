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

package com.ibm.oti.util;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.SocketPermission;
import java.net.URL;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.Enumeration;
import java.util.PropertyPermission;
import java.util.Vector;

/**
 * This is a policy class that supports security access based in policy files as
 * defined in 1.2.2
 */
public class DefaultPolicy extends Policy {
	// a list of grant entries with special criteria
	private Vector grantList = new Vector();

	// flag indicating that the policy has been read
	private boolean policyRead = false;

	public DefaultPolicy() {
	}

	/**
	 * Answers a PermissionCollection describing what permissions are available
	 * to the given CodeSource based on the current security policy.
	 * <p>
	 * Note that this method is <em>not</em> called for classes which are in
	 * the system domain (i.e. system classes). System classes are
	 * <em>always</em> given full permissions (i.e. AllPermission). This can
	 * not be changed by installing a new Policy.
	 * 
	 * @param cs
	 *            CodeSource the code source to compute the permissions for.
	 * @return PermissionCollection the permissions the code source should have.
	 */
	public PermissionCollection getPermissions(CodeSource cs) {
		// obtain the permissions list
		final CodeSource codeSource = cs;
		PrivilegedAction action = new PrivilegedAction() {
			public Object run() {
				return getPermissionsImpl(codeSource);
			}
		};

		return (PermissionCollection) AccessController.doPrivileged(action,
				null);
	}

	/**
	 * Answers a PermissionCollection describing what permissions are available
	 * to the given CodeSource based on the current security policy.
	 * <p>
	 * Note that this method is <em>not</em> called for classes which are in
	 * the system domain (i.e. system classes). System classes are
	 * <em>always</em> given full permissions (i.e. AllPermission). This can
	 * not be changed by installing a new Policy.
	 * 
	 * @param cs
	 *            CodeSource the code source to compute the permissions for.
	 * @return PermissionCollection the permissions the code source should have.
	 */
	PermissionCollection getPermissionsImpl(CodeSource cs) {
		// if the policy has not been read, read it in now
		if (!policyRead) {
			getSystemPolicy();
		}

		// see if the CodeSource matches any in the grantList

		URL hisURL = cs.getLocation() != null ? toCanonicalURL(cs.getLocation())
				: null;
		PermissionCollection pc = new Permissions();
		CodeSource hisCS = new CodeSource(hisURL, cs.getCertificates());
		for (int i = 0; i < grantList.size(); i++) {
			GrantHolder grant = (GrantHolder) grantList.elementAt(i);
			// a GrantHolder with a null codesource means it applies to
			// everything
			if ((grant.getCodeSource() == null)
					|| (grant.getCodeSource().implies(hisCS))) {
				Permissions grantPermissions = grant.getPermissions();
				Enumeration ePerm = grantPermissions.elements();
				while (ePerm.hasMoreElements()) {
					pc.add((Permission) ePerm.nextElement());
				}
			}
		}
		return pc;
	}

	/**
	 * stores the contents of a grant entry from the policy file
	 */
	static class GrantHolder {
		// holds the source these permissions apply to
		private CodeSource codeSource;

		// list of people the code must be signed by to have these permissions
		// applied
		private String signedBy;

		// the list of permissions applied to code matching the criteria
		private Permissions permissions;

		/**
		 * sets the codeSource for the receiver
		 * 
		 * @param codeSource
		 *            CodeSource the code source these permissions apply to
		 */
		void setCodeSource(CodeSource codeSource) {
			this.codeSource = codeSource;
		}

		/**
		 * answers the codeSource of the receiver
		 * 
		 * @return CodeSource
		 */
		CodeSource getCodeSource() {
			return this.codeSource;
		}

		/**
		 * answers the permissions collection of the receiver
		 * 
		 * @return Permissions
		 */
		Permissions getPermissions() {
			return this.permissions;
		}

		/**
		 * set the code's signer(s)
		 * 
		 * @param signedBy
		 *            String
		 */
		void setSigner(String signedBy) {
			this.signedBy = signedBy;
		}

		/**
		 * answers with the code's signer(s)
		 * 
		 * @return String
		 */
		String getSigner() {
			return this.signedBy;
		}

		/**
		 * adds a permission to the permissions list
		 * 
		 * @param permission
		 *            Permission
		 */
		void addPermission(Permission permission) {
			if (permissions == null)
				permissions = new Permissions();
			permissions.add(permission);
		}
	}

	/**
	 * reads and tokenizes the policy file
	 */
	static class PolicyTokenizer {
		// character that terminates every line
		private final static int EOL = '\n';

		// token types
		final static int TOK_CHAR = 0;

		final static int TOK_STRING = 1;

		final static int TOK_QUOTEDSTRING = 2;

		// holds on to the stream
		private InputStreamReader policyData;

		private char[] inbuf = new char[1024];

		private int inbufCount = 0, inbufPos = 0;

		// flag to say when the EOF is reached
		private boolean endOfFile = false;

		// place where the current string token is stored
		String sval;

		// place where the current char token is stored
		char cval;

		private char[] buf = new char[120];

		/**
		 * Constructs a new instance of this class and sets the policy stream
		 * 
		 * @param policyData
		 *            InputStream
		 */
		PolicyTokenizer(InputStreamReader policyData) {
			this.policyData = policyData;
		}

		/**
		 * Reads and throws away all characters until the end of the current
		 * line
		 */
		private void ignoreToEOL() throws IOException {
			while (true) {
				if (inbufPos == inbufCount) {
					if ((inbufCount = policyData.read(inbuf)) == -1) {
						inbufPos = -1;
						endOfFile = true;
						break;
					}
					inbufPos = 0;
				}
				if (inbuf[inbufPos++] == EOL)
					break; // & 0xff not required
			}
		}

		/**
		 * Reads and throws away all characters until the end of the slash star
		 * comment
		 */
		private void findEndOfComment() throws IOException {
			char lastChar = 0;
			while (true) {
				if (inbufPos == inbufCount) {
					if ((inbufCount = policyData.read(inbuf)) == -1) {
						inbufPos = -1;
						endOfFile = true;
						break;
					}
					inbufPos = 0;
				}
				char c = (char) inbuf[inbufPos++]; // & 0xff not required
				if (c == '/' && lastChar == '*')
					break;
				lastChar = c;
			}
		}

		/**
		 * answers with the end of file flag's status
		 * 
		 * @return boolean
		 */
		boolean isAtEOF() {
			return endOfFile;
		}

		/**
		 * reads the next token in the stream and puts it in sval answers the
		 * type of token that was read
		 * 
		 * @return int
		 */
		int nextToken() {
			boolean insideQuotes = false;
			int lastChar = ' ';

			char c;
			int length = 0;
			try {
				while (true) {
					if (inbufPos == inbufCount) {
						if ((inbufCount = policyData.read(inbuf)) == -1) {
							inbufPos = -1;
							endOfFile = true;
							break;
						}
						inbufPos = 0;
					}
					c = inbuf[inbufPos++];

					// new line or tab chars become spaces
					if (c == '\n' || c == '\r' || c == '\t')
						c = ' ';
					// double backslashes are turned into single backslashes
					if (c == '\\' && lastChar == '\\') {
						lastChar = ' ';
						continue;
					}

					if (insideQuotes) {
						if (c == '"' && lastChar != '\\')
							break;
					} else {
						// leading spaces are ignored. other spaces are token
						// separators
						if (c == ' ') {
							if (length == 0)
								continue;
							break;
						}
						// semicolons, commas, and brackets outside quotes are
						// also token separators
						if (c == ';' || c == ',' || c == '{' || c == '}') {
							// semicolons are also tokens, so push back the
							// semi-colon if
							// it is interpreted as a separator
							if (length == 0) {
								cval = c;
								return TOK_CHAR;
							}
							inbufPos--;
							break;
						}
						// handle quotes, but allow backslashed quotes
						if (c == '"' && lastChar != '\\') {
							if (length > 0) {
								inbufPos--;
								break;
							}
							insideQuotes = true;
							continue;
						}
						// handle comments which are outside quotes
						if (c == '/' && lastChar == '/') {
							length = 0;
							ignoreToEOL();
							lastChar = ' ';
							continue;
						}
						// handle /* */ comments
						if (c == '*' && lastChar == '/') {
							length--;
							lastChar = c;
							findEndOfComment();
							continue;
						}
					}
					if (length == buf.length) {
						char[] newBuf = new char[buf.length * 2];
						System.arraycopy(buf, 0, newBuf, 0, length);
						buf = newBuf;
					}
					buf[length++] = c;
					lastChar = c;
				}
			} catch (IOException e) {
				endOfFile = true;
			}
			sval = new String(buf, 0, length);

			// if inside a quoted string, then it was terminated by an end quote
			if (insideQuotes)
				return TOK_QUOTEDSTRING;
			return TOK_STRING;
		}

		/**
		 * reads and discards tokens until a matching one is found.
		 * 
		 * @param stopChar
		 *            stop tossing away tokens after this one is encountered
		 */
		void skipTokens(char stopChar) {
			while (!endOfFile) {
				if (nextToken() == TOK_CHAR && cval == stopChar)
					break;
			}
		}
	}

	/**
	 * Parses policy information in the Policy File format and creates a
	 * grantList with the entries. Does nothing if the Policy File has parse
	 * errors
	 * 
	 * @param data
	 *            InputStream Stream containing the policy data
	 */
	private void readPolicy(InputStream data, URL source, boolean allowExpand) {
		policyRead = false;
	}

	/**
	 * Answers with a new instance of the passed permissionClass or null if it
	 * is not recognised.
	 * 
	 * @param permissionClass
	 *            String The type of <CODE>Permission</CODE> it is expected to
	 *            answer with
	 * @param permissionName
	 *            String The rights which may be needed for the <CODE>Permission</CODE>
	 * @param permissionAction
	 *            String The action to which the <CODE>Permission</CODE>
	 *            applies
	 * @return Permission
	 */
	Permission createPermission(String permissionClass, String permissionName,
			String permissionAction) {

		try {
			Class pc = Class.forName(permissionClass);
			Constructor pcc = pc.getConstructor(new Class[] { String.class,
					String.class });

			return (Permission) pcc.newInstance(new Object[] { permissionName,
					permissionAction });
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Reads the system policy
	 */
	private void getSystemPolicy() {
		boolean allowExpandProperties = !"false".equalsIgnoreCase(Security
				.getProperty("policy.expandProperties"));

		// see if we're allowed to use command line specified properties,
		// default of true
		boolean allowPropertyPolicies = !"false".equalsIgnoreCase(Security
				.getProperty("allowSystemProperty"));
		// if we're allowed to use system defined properties and they specified
		// a policy on the command line using -Djava.security.policy==someURL
		// then it must be the only file loaded
		if (allowPropertyPolicies) {
			String policyFile = System.getProperty("java.security.policy");

			// if the system property java.security.policy exists and
			// the policy.allowSystemProperty is not false in the security
			// properties file
			if (policyFile != null) {
				boolean exclusiveFile = policyFile.charAt(0) == '=';
				// chop off the extra equals
				if (exclusiveFile)
					policyFile = policyFile.substring(1);
				try {
					URL policyURL;
					try {
						policyURL = new URL(policyFile);
					} catch (MalformedURLException m) {
						// Assume its a file in the current directory
						policyURL = new URL("file:" + policyFile);
					}
					InputStream in = policyURL.openStream();
					readPolicy(in, policyURL, allowExpandProperties);
					in.close();
				} catch (IOException e) {
				}
				if (exclusiveFile)
					return;
			}
		}
		// go through all the properties to read system defined policy files
		int policyNum = 1;
		String policyFile;
		while ((policyFile = Security.getProperty("policy.url." + policyNum++)) != null) {
			try {
				URL policyURL = new URL(expandTags(policyFile, false, true)
						.replace('\\', '/'));
				InputStream in = policyURL.openStream();
				readPolicy(in, policyURL, allowExpandProperties);
				in.close();
			} catch (IOException e) {
			}
		}

		// if the policy can't be read then use the default one
		if (!policyRead) {
			// make sure the grant list is empty
			grantList.removeAllElements();
			// add the permissions for all code
			GrantHolder defaultGrant = new GrantHolder();
			Permission[] defaults = defaultSystemPermissionList();
			for (int i = 0; i < defaults.length; ++i)
				defaultGrant.addPermission(defaults[i]);
			policyRead = true;

			grantList.addElement(defaultGrant);

			// Extensions get AllPermission
			try {
				String javaHome = System.getProperty("java.home");
				CodeSource extensionCS = new CodeSource(new File(javaHome,
						"lib/ext/*").toURL(),
						(java.security.cert.Certificate[]) null);
				GrantHolder extensionGrant = new GrantHolder();
				extensionGrant.setCodeSource(extensionCS);
				extensionGrant.addPermission(new AllPermission());
				grantList.addElement(extensionGrant);
			} catch (MalformedURLException e) {
			}
		}
	}

	/**
	 * Causes the policy file to be re-read with the next call to getPermissions
	 * 
	 */
	public void refresh() {
		// clear the cached security policy
		policyRead = false;
		grantList.removeAllElements();
	}

	/**
	 * Parses and expands property tags from the passed string. These tags occur
	 * in the format ${propertyname} If the allowBlankProperties flag is set, it
	 * answers with a null string should a property be not found.
	 * 
	 * @param text
	 *            String String which may contain property tags
	 * @param allowBlankProperties
	 *            boolean Properties that don't expand are allowed?
	 */
	String expandTags(String text, boolean allowBlankProperties,
			boolean allowExpand) {
		if (!allowExpand)
			return text;

		StringBuffer textBuf = null;
		int curPos = 0, length = text.length();
		while (curPos < length) {
			int nextToken = text.indexOf("${", curPos);
			// if there are no more properties, add what remains of the string
			if (nextToken == -1) {
				if (curPos == 0)
					return text;
				textBuf.append(text.substring(curPos));
				break;
			}
			// to avoid (most) StringBuffer growth make it twice as large
			if (textBuf == null)
				textBuf = new StringBuffer(text.length() * 2);
			// where the closing brace is
			int tagEnd = text.indexOf('}', nextToken);
			// add the string up to the token start
			textBuf.append(text.substring(curPos, nextToken));
			if (tagEnd > nextToken) {
				curPos = tagEnd + 1;
				String tagVal = expandProperty(text.substring(nextToken + 2,
						tagEnd));
				if (tagVal != null) {
					textBuf.append(tagVal);
					// Don't create UNC paths. If the system property ends with
					// a slash, remove any starting slash from the text to
					// append.
					// In the case where user.home is \,
					// ${user.home}/.java.policy will expand //.java.policy
					// which is a UNC path and very slow to resolve.
					if (tagVal.length() > 0) {
						char ch = tagVal.charAt(tagVal.length() - 1);
						if ((ch == '\\' || ch == '/') && curPos < length) {
							ch = text.charAt(curPos);
							if (ch == '\\' || ch == '/')
								curPos++;
						}
					}
				} else if (!allowBlankProperties)
					return null;
			} else {
				// if there is no closing brace
				curPos = nextToken + 2;
				textBuf.append("${");
			}
		}
		return textBuf == null ? text : textBuf.toString();
	}

	/**
	 * Answers a property tag with it's system property value
	 * 
	 * @param tag
	 *            String The string value whose property gets answered.
	 * @return String
	 */
	String expandProperty(String tag) {
		// expand the abbreviated tag ${/} to ${file.separator}
		if (tag.equals("/"))
			tag = "file.separator";
		return System.getProperty(tag);
	}

	/**
	 * The following is a list of default permissions which are used in a case
	 * where no system policy is available
	 */
	private static Permission[] defaultSystemPermissionList() {
		return new Permission[] {
				// Allow threads to stop themselves and exit.
				// new RuntimePermission("stopThread"),
				new RuntimePermission("exitVM"),
				// Allow listening on unprivileged ports.
				new SocketPermission("localhost:1024-", "listen"),
				// Standard property access.
				new PropertyPermission("java.version", "read"),
				new PropertyPermission("java.vendor", "read"),
				new PropertyPermission("java.vendor.url", "read"),
				new PropertyPermission("java.class.version", "read"),
				new PropertyPermission("os.name", "read"),
				new PropertyPermission("os.version", "read"),
				new PropertyPermission("os.arch", "read"),
				new PropertyPermission("file.separator", "read"),
				new PropertyPermission("path.separator", "read"),
				new PropertyPermission("line.separator", "read"),
				new PropertyPermission("java.specification.version", "read"),
				new PropertyPermission("java.specification.vendor", "read"),
				new PropertyPermission("java.specification.name", "read"),
				new PropertyPermission("java.vm.specification.version", "read"),
				new PropertyPermission("java.vm.specification.vendor", "read"),
				new PropertyPermission("java.vm.specification.name", "read"),
				new PropertyPermission("java.vm.version", "read"),
				new PropertyPermission("java.vm.vendor", "read"),
				new PropertyPermission("java.vm.name", "read"), };
	}

	private static URL toCanonicalURL(URL orgURL) {
		if (orgURL.getProtocol().equals("jar"))
			// get URL from JarURL
			try {
				// Create a URL for the resource the jar refers to
				JarURLConnection jarCon = (JarURLConnection) orgURL
						.openConnection();
				String jarURL = toCanonicalURL(jarCon.getJarFileURL())
						.toString();
				String entryName = jarCon.getEntryName();
				if (entryName == null)
					entryName = "";
				jarURL = new StringBuffer(jarURL.length() + entryName.length()
						+ 2).append(jarURL).append("!/").append(entryName)
						.toString();
				return new URL("jar", null, -1, jarURL);
			} catch (IOException ioe) {
				// Continue using the jar URL.
			}

		if (orgURL.getProtocol().equals("file")) {
			// canonize for comparison
			String fileName;
			if ((fileName = orgURL.getFile()) == null)
				fileName = "";
			String host = orgURL.getHost();
			if (host != null && host.length() > 0)
				fileName = new StringBuffer(host.length() + fileName.length()
						+ 2).append("//").append(host).append(fileName)
						.toString();
			try {
				return new File(fileName).toURL();
			} catch (MalformedURLException ex) {
			}
		}

		return orgURL;
	}
}
