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

package java.security;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Hashtable;

/**
 * Holds permissions which are of an unknown type when a policy file is read
 */
public final class UnresolvedPermission extends Permission implements
		Serializable {
	static final long serialVersionUID = -4821973115467008846L;

	/**
	 * The Certificates which were used to create the receiver.
	 */
	private transient Certificate[] certificates;

	/**
	 * The type of permission this will be
	 */
	private String type;

	/**
	 * the action string
	 */
	private String actions;

	/**
	 * the permission name
	 */
	private String name;

	static Class[] constructorArgs;
	static {
		Class string = String.class;
		constructorArgs = new Class[] { string, string };
	}

	/**
	 * Constructs a new instance of this class with its type, name, and
	 * certificates set to the arguments by definition, actions are ignored
	 * 
	 * @param type
	 *            class of permission object
	 * @param name
	 *            identifies the permission that could not be resolved
	 * @param actions
	 * @param certs
	 */
	public UnresolvedPermission(String type, String name, String actions,
			Certificate[] certs) {
		super(type);
		this.name = name;
		this.certificates = certs;
		this.type = type;
		this.actions = actions;
	}

	/**
	 * Compares the argument to the receiver, and answers true if they represent
	 * the <em>same</em> object using a class specific comparison. In this
	 * case, the receiver and the object must have the same class, permission
	 * name, actions, and certificates
	 * 
	 * @param obj
	 *            the object to compare with this object
	 * @return <code>true</code> if the object is the same as this object,
	 *         <code>false</code> otherwise.
	 * 
	 * @see #hashCode
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof UnresolvedPermission))
			return false;
		UnresolvedPermission perm = (UnresolvedPermission) obj;
		if (!(type.equals(perm.type) && name.equals(perm.name) && actions
				.equals(perm.actions)))
			return false;
		if (certificates == null && perm.certificates == null)
			return true;
		if (certificates.length != perm.certificates.length)
			return false;
		Hashtable set = new Hashtable();
		for (int i = 0; i < certificates.length; i++)
			set.put(certificates[i], certificates[i]);
		for (int i = 0; i < perm.certificates.length; i++)
			if (set.get(perm.certificates[i]) == null)
				return false;
		return true;
	}

	/**
	 * Indicates whether the argument permission is implied by the receiver.
	 * UnresolvedPermission objects imply nothing because nothing is known about
	 * them yet.
	 * 
	 * @param p
	 *            the permission to check
	 * @return always replies false
	 */
	public boolean implies(Permission p) {
		return false;
	}

	/**
	 * Answers a new PermissionCollection for holding permissions of this class.
	 * Answer null if any permission collection can be used.
	 * 
	 * @return a new PermissionCollection or null
	 * 
	 * @see java.security.BasicPermissionCollection
	 */
	public PermissionCollection newPermissionCollection() {
		return new UnresolvedPermissionCollection();
	}

	/**
	 * Answers the actions associated with the receiver. Since
	 * UnresolvedPermission objects have no actions, answer the empty string.
	 * 
	 * @return the actions associated with the receiver.
	 */
	public String getActions() {
		return "";
	}

	/**
	 * Answers an integer hash code for the receiver. Any two objects which
	 * answer <code>true</code> when passed to <code>equals</code> must
	 * answer the same value for this method.
	 * 
	 * @return the receiver's hash
	 * 
	 * @see #equals
	 */
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * @return a printable representation for the receiver.
	 */
	public String toString() {
		return "(unresolved " + type + " " + name + " " + actions + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	Permission resolve(ClassLoader cl) {
		try {
			// Check certificates : the permission loaded must have each
			// certificate this has.
			Class permClass = Class.forName(type, true, cl);
			boolean certified = true;
			Hashtable classCertificatesSet = null;
			if (permClass.getSigners() != null) {
				Certificate[] classCertificates = (Certificate[]) permClass
						.getSigners();
				classCertificatesSet = new Hashtable(
						classCertificates.length * 3 / 2);
				for (int i = 0; i < classCertificates.length; ++i)
					if (classCertificates[i] != null)
						classCertificatesSet.put(classCertificates[i],
								classCertificates[i]);
				if (classCertificatesSet.size() == 0)
					classCertificatesSet = null;
			}
			if (certificates != null) {
				if (classCertificatesSet == null) {
					for (int i = 0; i < certificates.length; ++i)
						if (certificates[i] != null) {
							certified = false;
							break;
						}
				} else {
					for (int i = 0; i < certificates.length; ++i)
						if (certificates[i] != null
								&& !classCertificatesSet
										.containsKey(certificates[i])) {
							certified = false;
							break;
						}
				}
			}
			if (certified) {
				Constructor constructor = permClass
						.getConstructor(constructorArgs);
				return (Permission) constructor.newInstance(new Object[] {
						name, actions });
			}
		} catch (ClassNotFoundException e) {
		} catch (NoSuchMethodException e) {
		} catch (InstantiationException e) {
		} catch (InvocationTargetException e) {
		} catch (IllegalAccessException e) {
		}
		return null;
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		int length = 0;
		if (certificates != null)
			length = certificates.length;
		stream.writeInt(length);
		for (int i = 0; i < length; i++) {
			stream.writeUTF(certificates[i].getType());
			try {
				byte[] encoded = certificates[i].getEncoded();
				stream.writeInt(encoded.length);
				stream.write(encoded);
			} catch (CertificateEncodingException e) {
				stream.writeInt(0);
			}
		}
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		stream.defaultReadObject();
		int count = stream.readInt();
		if (count > 0) {
			certificates = new Certificate[count];
			for (int i = 0; i < count; i++) {
				String certType = stream.readUTF();
				int length = stream.readInt();
				if (length > 0) {
					byte[] encoded = new byte[length];
					stream.read(encoded);
					try {
						CertificateFactory factory = CertificateFactory
								.getInstance(certType);
						certificates[i] = factory
								.generateCertificate(new ByteArrayInputStream(
										encoded));
					} catch (CertificateException e) {
					}
				}
			}
		}
	}
}
