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


/**
 * Makes available message digest algorithm functionality.
 */
public abstract class MessageDigest extends java.security.MessageDigestSpi {

	// Key prefix for algorithm name lookup
	private static final String KEY_PREFIX = "MessageDigest.";

	// Name of the digest algorithm represented by the receiver
	private String algorithmName; 

	// Provider of the digest algorithm represented by the receiver. 
	private Provider provider;

	// *** WARNING *** - The provider is not passed as parameter to the
	// constructor. It means it will be uninitialized until an actual binding
	// to a concrete class by a provider is performed, in getInstance.

	// This one is tricky. Providers are supposed to subclass MessageDigestSpi,
	// but method getInstance in MessageDigest returns a MessageDigest.
	// Therefore, there is no way we can return the user-provider class
	// directly (incompatible types).
	// We need to return a MessageDigest (or subclass) instance, but
	// somehow have a reference to the provider digest instance. This is what
	// the Wrapper class below implements.
	static private class Wrapper extends MessageDigest {
		MessageDigestSpi providerDigest;

		Wrapper(MessageDigestSpi providerDigest, String algorithmName) {
			// Just because there is no empty constructor in the superclass
			super(algorithmName); 
			this.providerDigest = providerDigest;
		}

		public Object clone() throws CloneNotSupportedException {
			Wrapper clone = new Wrapper((MessageDigestSpi) providerDigest
					.clone(), getAlgorithm());
			clone.setProvider(getProvider());
			return clone;
		}

		protected byte[] engineDigest() {
			return providerDigest.engineDigest();
		}

		protected void engineReset() {
			providerDigest.engineReset();
		}

		protected void engineUpdate(byte bytesToHash[], int offset, int count) {
			providerDigest.engineUpdate(bytesToHash, offset, count);
		}

		protected void engineUpdate(byte byteToHash) {
			providerDigest.engineUpdate(byteToHash);
		}

		protected int engineGetDigestLength() {
			return providerDigest.engineGetDigestLength();
		}
	}

	/**
	 * Create a new MessageDigest with its algorithm set to the argument.
	 * 
	 * 
	 * @param algorithmName
	 *            java.lang.String the algorithm that the receiver will
	 *            represent
	 */
	protected MessageDigest(String algorithmName) {
		setAlgorithm(algorithmName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/**
	 * Computes and answers the final hash value that the receiver represents.
	 * After the digest is computed the receiver is reset.
	 * 
	 * @return the hash the receiver computed
	 * 
	 * @see #reset
	 */
	public byte[] digest() {
		byte[] answer = engineDigest();
		reset();
		return answer;
	}

	/**
	 * Includes the bytes of the argument in the hash value computed by the
	 * receiver, and then computes the final digest value.
	 * 
	 * @param bytesToHash
	 *            byte[] the source array
	 * @return the hash the receiver computed
	 * 
	 * @see #update(byte)
	 * @see #update(byte[])
	 * @see #update(byte[], int, int)
	 * @see #digest()
	 * @see #digest(byte[])
	 * @see #digest(byte[], int, int)
	 */
	public byte[] digest(byte bytesToHash[]) {
		update(bytesToHash);
		return digest();
	}

	/**
	 * Computes the digest and stores it into the buffer passed as parameter.
	 * 
	 * @param computedDigest
	 *            byte[] the array into which to store the digest
	 * @param offset
	 *            the starting offset into the array
	 * @param count
	 *            the number of bytes available to store the digest
	 * 
	 * @exception DigestException
	 *                If an error occurs
	 * 
	 * @return the number of bytes copied
	 * @see #digest()
	 * @see #digest(byte[])
	 * @see #digest(byte[], int, int)
	 */
	public int digest(byte computedDigest[], int offset, int count)
			throws DigestException {
		byte[] digest = digest();
		int toCopy = count;
		if (digest.length < toCopy)
			toCopy = digest.length;
		System.arraycopy(digest, 0, computedDigest, offset, toCopy);
		return toCopy;
	}

	/**
	 * Answers the standard Java Security name for the algorithm being used by
	 * the receiver.
	 * 
	 * @return String the name of the algorithm
	 */
	public final String getAlgorithm() {
		return algorithmName;
	}

	/**
	 * Return the engine digest length in bytes. Default is 0.
	 * 
	 * @return int the engine digest length in bytes
	 * 
	 */
	public final int getDigestLength() {
		return engineGetDigestLength();
	}

	/**
	 * Answers a new MessageDigest which is capable of running the algorithm
	 * described by the argument. The result will be an instance of a subclass
	 * of MessageDigest which implements that algorithm.
	 * 
	 * 
	 * @param algorithmName
	 *            java.lang.String Name of the algorithm desired
	 * @return MessageDigest a concrete implementation for the algorithm
	 *         desired.
	 * 
	 * @exception NoSuchAlgorithmException
	 *                If the algorithm cannot be found
	 */
	public static MessageDigest getInstance(String algorithmName)
			throws NoSuchAlgorithmException {

		if (algorithmName == null)
			throw new IllegalArgumentException();

		Provider[] providers = Security.getProviders();
		for (int i = 0; i < providers.length; i++) {
			Provider provider = providers[i];
			MessageDigest digest = toMessageDigestImplementation(algorithmName,
					provider);
			if (digest != null)
				return digest;
		}

		// Scanned all, found nothing
		throw new NoSuchAlgorithmException(algorithmName);
	}

	/**
	 * Answers a new MessageDigest which is capable of running the algorithm
	 * described by the argument. The result will be an instance of a subclass
	 * of MessageDigest which implements that algorithm.
	 * 
	 * 
	 * @param algorithmName
	 *            java.lang.String Name of the algorithm desired
	 * @param providerName
	 *            java.lang.String Name of the provider which has to implement
	 *            the algorithm
	 * @return MessageDigest a concrete implementation for the algorithm
	 *         desired.
	 * 
	 * @exception NoSuchAlgorithmException
	 *                If the algorithm cannot be found
	 * @exception NoSuchProviderException
	 *                If the provider cannot be found
	 */
	public static MessageDigest getInstance(String algorithmName,
			String providerName) throws NoSuchAlgorithmException,
			NoSuchProviderException {

		if (providerName == null)
			throw new java.lang.IllegalArgumentException();
		if (algorithmName == null)
			throw new java.lang.IllegalArgumentException();

		Provider provider = Security.getProvider(providerName);
		if (provider == null)
			throw new NoSuchProviderException(providerName);

		MessageDigest digest = toMessageDigestImplementation(algorithmName,
				provider);
		if (digest == null)
			throw new NoSuchAlgorithmException(algorithmName);
		return digest;
	}

	/**
	 * Answers a new MessageDigest which is capable of running the algorithm
	 * described by the argument. The result will be an instance of a subclass
	 * of MessageDigest which implements that algorithm.
	 * 
	 * 
	 * @param algorithm
	 *            java.lang.String Name of the algorithm desired
	 * @param provider
	 *            Provider Provider which has to implement the algorithm
	 * @return MessageDigest a concrete implementation for the algorithm
	 *         desired.
	 * 
	 * @exception NoSuchAlgorithmException
	 *                If the algorithm cannot be found
	 */
	public static MessageDigest getInstance(String algorithm, Provider provider)
			throws NoSuchAlgorithmException {
		if ((algorithm == null) || (provider == null)) {
			throw new IllegalArgumentException();
		}

		return toMessageDigestImplementation(algorithm, provider);

	}

	/**
	 * Returns the Provider of the digest represented by the receiver.
	 * 
	 * @return Provider an instance of a subclass of java.security.Provider
	 */
	public final Provider getProvider() {
		return provider;
	}

	/**
	 * Does a simply byte-per-byte compare of the two digests.
	 * 
	 * @param digesta
	 *            One of the digests to compare
	 * @param digestb
	 *            The digest to compare to
	 * 
	 * @return <code>true</code> if the two hashes are equal
	 *         <code>false</code> if the two hashes are not equal
	 */
	public static boolean isEqual(byte[] digesta, byte[] digestb) {
		return java.util.Arrays.equals(digesta, digestb);
	}

	/**
	 * Puts the receiver back in an initial state, such that it is ready to
	 * compute a new hash.
	 * 
	 * @see java.security.MessageDigest.Wrapper#engineReset()
	 */
	public void reset() {
		engineReset();
	}

	/**
	 * Set the algorithm being used by the receiver to the argument which should
	 * be a standard Java Security algorithm name.
	 * 
	 * @param algorithmName
	 *            String the name of the algorithm
	 */
	void setAlgorithm(String algorithmName) {
		this.algorithmName = algorithmName;
	}

	/**
	 * Set the provider being used by the receiver to the argument which should
	 * be an instance of a subclass of Provider
	 * 
	 * @param provider
	 *            the Provider for the receiver
	 */
	void setProvider(Provider provider) {
		this.provider = provider;
	}

	/**
	 * Answers a MessageDigest for the algorithm name supplied by the given
	 * provider.
	 * 
	 * 
	 * @param algorithmName
	 *            java.lang.String the name of the algorithm to search for
	 * @param provider
	 *            java.security.Provider the provider desired for the algorithm.
	 * 
	 * @return The message digest for the algorithm name supplied by the given
	 *         provider.
	 * 
	 */
	private static MessageDigest toMessageDigestImplementation(
			String algorithmName, Provider provider) {
		// First try to find the class corresponding to the algorithm name
		String digestClassName;
		try {
			digestClassName = provider
					.lookupProperty(KEY_PREFIX, algorithmName);
			if (digestClassName == null)
				return null;
		} catch (ClassCastException e) {
			return null;
		}

		// Now try to instantiate the digest.
		try {
			Class digestClass = Class.forName(digestClassName, true, provider
					.getClass().getClassLoader());
			MessageDigestSpi providedDigest = (MessageDigestSpi) digestClass
					.newInstance();
			MessageDigest digest;
			if (providedDigest instanceof MessageDigest)
				digest = (MessageDigest) providedDigest;
			else
				digest = new Wrapper(providedDigest, algorithmName);
			digest.setProvider(provider);
			return digest;
		} catch (ClassNotFoundException ex) {
			// Intentionally empty
		} catch (IllegalAccessException e) {
			// Intentionally empty
		} catch (InstantiationException e) {
			// Intentionally empty
		} catch (ClassCastException e) {
			// Intentionally empty
		}
		return null;
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * @return a printable representation for the receiver.
	 */
	public String toString() {
		return "MessageDigest : algorithm " + getAlgorithm() //$NON-NLS-1$
				+ " from provider " + getProvider().getName(); //$NON-NLS-1$
	}

	/**
	 * Includes the bytes of the argument in the hash value computed by the
	 * receiver.
	 * 
	 * @param bytesToHash
	 *            byte[] the source array
	 */
	public void update(byte bytesToHash[]) {
		engineUpdate(bytesToHash, 0, bytesToHash.length);
	}

	/**
	 * Includes a range of bytes from the first argument in the hash value
	 * computed by the receiver.
	 * 
	 * @param bytesToHash
	 *            byte[] the source array
	 * @param offset
	 *            the starting offset into the array
	 * @param count
	 *            the number of bytes to include in the hash
	 */
	public void update(byte bytesToHash[], int offset, int count) {
		engineUpdate(bytesToHash, offset, count);
	}

	/**
	 * Includes the argument in the hash value computed
	 * by the receiver.
	 *
	 * @param		byteToHash byte
	 *					the byte to feed to the hash algorithm
	 *
	 * @see			#reset()
	 */
	public void update(byte byteToHash) {
		engineUpdate(byteToHash);
	}
}
