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

package java.io;


import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;

/**
 * The class FilePermission is responsible for granting access to files or
 * directories. The FilePermission is made up of a pathname and a set of actions
 * which are valid for the pathname.
 * <P>
 * The <code>File.separatorChar</code> must be used in all pathnames when
 * constructing a FilePermission. The following descriptions will assume the
 * char is </code>/</code>. A pathname which ends in "/*", implies all the
 * files and directories contained in that directory. If the pathname ends in
 * "/-", it indicates all the files and directories in that directory
 * <b>recursively</b>.
 * 
 */
public final class FilePermission extends Permission implements Serializable {
	static final long serialVersionUID = 7930732926638008763L;

	// canonical path of this permission
	private transient String canonPath;

	// list of actions permitted for socket permission in order
	private static final String[] actionList = { "read", "write", "execute", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"delete" }; //$NON-NLS-1$

	// "canonicalized" action list
	private String actions;

	// the numeric representation of this action list
	// for implies() to check if one action list is the subset of another.
	transient int mask = -1;

	// global include all permission?
	private transient boolean includeAll = false;

	private transient boolean allDir = false;

	private transient boolean allSubdir = false;

	/**
	 * Constructs a new FilePermission with the path and actions specified.
	 * 
	 * 
	 * @param path
	 *            the path to apply the actions to.
	 * @param actions
	 *            the actions for the <code>path<code>. May be any
	 *							combination of read, write, execute, or delete.
	 */
	public FilePermission(String path, String actions) {
		super(path);
		init(path, actions);
	}

	private void init(final String path, String pathActions) {
		if (pathActions != null && pathActions != "") { //$NON-NLS-1$
			if (path != null) {
				if (path.equals("<<ALL FILES>>")) { //$NON-NLS-1$
					includeAll = true;
				} else {
					canonPath = (String) AccessController
							.doPrivileged(new PrivilegedAction() {
								public Object run() {
									try {
										return new File(path)
												.getCanonicalPath();
									} catch (IOException e) {
										return path;
									}
								}
							});
					int plength = canonPath.length();
					if (plength >= 1) {
						if (canonPath.endsWith("*")) { //$NON-NLS-1$
							if (plength == 1
									|| (canonPath.charAt(plength - 2)) == File.separatorChar)
								allDir = true;
						} else if (canonPath.endsWith("-")) { //$NON-NLS-1$
							if (plength == 1
									|| (canonPath.charAt(plength - 2)) == File.separatorChar)
								allSubdir = true;
						}
					}
				}
				this.actions = toCanonicalActionString(pathActions);
			} else
				throw new NullPointerException(com.ibm.oti.util.Msg
						.getString("K006e")); //$NON-NLS-1$
		} else
			throw new IllegalArgumentException(com.ibm.oti.util.Msg
					.getString("K006d")); //$NON-NLS-1$
	}

	/**
	 * Answer the string representing this permissions actions. It must be of
	 * the form "read,write,execute,delete", all lower case and in the correct
	 * order if there is more than one action.
	 * 
	 * @param action
	 *            the action name
	 * @return the string representing this permission's actions
	 */
	private String toCanonicalActionString(String action) {
		actions = action.trim().toLowerCase();

		// get the numerical representation of the action list
		mask = getMask(actions);

		// convert the mask to a canonical action list.
		int len = actionList.length;
		// the test mask - shift the 1 to the leftmost position of the
		// actionList
		int highestBitMask = 1 << (len - 1);

		// if a bit of mask is set, append the corresponding action to result
		StringBuffer result = new StringBuffer();
		boolean addedItem = false;
		for (int i = 0; i < len; i++) {
			if ((highestBitMask & mask) != 0) {
				if (addedItem)
					result.append(","); //$NON-NLS-1$
				result.append(actionList[i]);
				addedItem = true;
			}
			highestBitMask = highestBitMask >> 1;
		}
		return result.toString();
	}

	/**
	 * Answers the numerical representation of the argument.
	 * 
	 * @param actionNames
	 *            the action names
	 * @return the action mask
	 */
	private int getMask(String actionNames) {
		int actionInt = 0, head = 0, tail = 0;
		do {
			tail = actionNames.indexOf(",", head); //$NON-NLS-1$
			String action = tail > 0 ? actionNames.substring(head, tail).trim()
					: actionNames.substring(head).trim();
			if (action.equals("read")) //$NON-NLS-1$
				actionInt |= 8;
			else if (action.equals("write")) //$NON-NLS-1$
				actionInt |= 4;
			else if (action.equals("execute")) //$NON-NLS-1$
				actionInt |= 2;
			else if (action.equals("delete")) //$NON-NLS-1$
				actionInt |= 1;
			else
				throw new java.lang.IllegalArgumentException(
						com.ibm.oti.util.Msg.getString("K006f", action)); //$NON-NLS-1$
			head = tail + 1;
		} while (tail > 0);
		return actionInt;
	}

	/**
	 * Answers the actions associated with the receiver.
	 * 
	 * @return the actions associated with the receiver.
	 */
	public String getActions() {
		return actions;
	}

	/**
	 * Check to see if this permission is equal to another. The two are equal if
	 * <code>obj</code> is a FilePermission, they have the same path, and they
	 * have the same actions.
	 * 
	 * @param obj
	 *            the object to check equality with.
	 * @return <code>true</code> if the two are equal, <code>false</code>
	 *         otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj instanceof FilePermission) {
			FilePermission fp = (FilePermission) obj;
			if (fp.actions != actions)
				if (fp.actions == null || !fp.actions.equals(actions))
					return false;

			/* Matching actions and both are <<ALL FILES>> ? */
			if (fp.includeAll || includeAll)
				return fp.includeAll == includeAll;
			return fp.canonPath.equals(canonPath);
		}
		return false;
	}

	/**
	 * Indicates whether the argument permission is implied by the receiver.
	 * 
	 * @param p
	 *            java.security.Permission the permission to check.
	 * @return <code>true</code> if the argument permission is implied by the
	 *         receiver, and <code>false</code> if it is not.
	 */
	public boolean implies(Permission p) {
		int match = impliesMask(p);
		return match != 0 && match == ((FilePermission) p).mask;
	}

	/**
	 * Answers an int describing what masks are implied by a specific
	 * permission.
	 * 
	 * @param p
	 *            the permission
	 * @return the mask applied to the given permission
	 */
	int impliesMask(Permission p) {
		if (!(p instanceof FilePermission))
			return 0;
		FilePermission fp = (FilePermission) p;
		int matchedMask = mask & fp.mask;
		// Can't match any bits?
		if (matchedMask == 0)
			return 0;

		// Is this permission <<ALL FILES>>
		if (includeAll)
			return matchedMask;

		// We can't imply all files
		if (fp.includeAll)
			return 0;

		// Scan the length of p checking all match possibilities
		// \- implies everything except \
		int thisLength = canonPath.length();
		if (allSubdir && thisLength == 2
				&& !fp.canonPath.equals(File.separator))
			return matchedMask;

		boolean includeDir = false;
		boolean lastIsSlash = false;
		int pLength = fp.canonPath.length();
		for (int i = 0; i < pLength; i++) {
			char pChar = fp.canonPath.charAt(i);
			// Is p longer than this permissions canonLength?
			if (i >= thisLength) {
				// If not includeDir then is has to be a mismatch.
				if (!includeDir)
					return 0;
				/**
				 * If we have * for this and the separator is not the last char
				 * it is invalid. IE: this is '/a/*' and p is '/a/b/c' we should
				 * fail on the separator after the b.
				 */
				if (pChar == File.separatorChar && (i != pLength - 1))
					return 0;
			} else {
				// Can safely get cChar since it's in range.
				char cChar = canonPath.charAt(i);
				// Is this permission include all? (must have matched up until
				// this point).
				if (lastIsSlash && cChar == '-') {
					// Checked at constructor for separator/-
					if (!allSubdir)
						return 0;
					// If we've already seen '*' return 0, can't group - and *.
					if (includeDir)
						return 0;
					return matchedMask;
				}
				// Is this permission include a dir? Continue the check
				// afterwards.
				if (lastIsSlash && cChar == '*') {
					/* Checked at constructor for File.separator/* */
					if (!allDir)
						return 0;
					// Cannot have two *'s in a row.
					if (includeDir)
						return 0;
					// * does not match -
					if (fp.allSubdir)
						return 0;
					// Set the fact that we have seen a * in this permission.
					includeDir = true;
					continue;
				}
				// Are the characters matched?
				if (cChar != pChar)
					return 0;
				// Is is a separator char? Needed for /* and /-
				lastIsSlash = cChar == File.separatorChar;
			}
		}
		// Must have matched upto this point or it's a valid file in an include
		// all directory
		return pLength == thisLength || includeDir ? matchedMask : 0;
	}

	/**
	 * Answers a new PermissionCollection in which to place FilePermission
	 * Objects.
	 * 
	 * @return A new PermissionCollection suitable for storing FilePermission
	 *         objects.
	 */
	public java.security.PermissionCollection newPermissionCollection() {
		return new FilePermissionCollection();
	}

	/**
	 * Answers an int representing the hash code value for this FilePermission.
	 * 
	 * @return int the hash code value for this FilePermission.
	 */
	public int hashCode() {
		return (canonPath == null ? getName().hashCode() : canonPath.hashCode())
				+ mask;
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		stream.defaultReadObject();
		init(getName(), actions);
	}
}
