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


import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;

/**
 * For the generation of secure pseudo-random numbers.
 */
public class SecureRandom extends java.util.Random {
	static final long serialVersionUID = 4940670005562187L;

	// Algorithm name in case users call the empty constructor
	private static final String DEFAULT_ALGORITHM_NAME = "SHA1PRNG"; 

	// Key prefix for algorithm name lookup
	private static final String KEY_PREFIX = "SecureRandom.";

	private static SecureRandom DEFAULT = null; // Default instance for seed
												// generation for static method
												// getSeed

	private Provider provider; // Provider of the secure hash algorithm

	// Actual implementation for the secure hash algorithm
	private SecureRandomSpi secureRandomSpi; 
	
	private byte[] state; // None of these are used, needed for serialization

	private MessageDigest digest;

	private byte[] randomBytes;

	private int randomBytesUsed;

	private long counter;

	/**
	 * Constructs a new instance of this class. Users are encouraged to use
	 * <code>getInstance()</code> instead.
	 * 
	 * An implementation for the highest-priority provider is returned. The
	 * instance returned will not have been seeded.
	 */
	public SecureRandom() {
		super();
		try {
			SecureRandom sr = getInstance(DEFAULT_ALGORITHM_NAME);
			this.secureRandomSpi = sr.secureRandomSpi;
			this.provider = sr.provider;
		} catch (NoSuchAlgorithmException e) {
			// There should be at least a default implementation for secure
			// random
			throw new Error(e.toString());
		}
	}

	/**
	 * Constructs a new instance of this class. Users are encouraged to use
	 * <code>getInstance()</code> instead.
	 * 
	 * An implementation for the highest-priority provider is returned. The
	 * instance returned will be seeded with the parameter.
	 * 
	 * @param seed
	 *            bytes forming the seed for this generator.
	 */
	public SecureRandom(byte[] seed) {
		this();
		setSeed(seed);
	}

	/**
	 * Constructs a new instance of this class.
	 * 
	 * @param secureRandomSpi
	 *            The actual provider-specific generator
	 * @param provider
	 *            The provider of the implementation
	 */
	protected SecureRandom(SecureRandomSpi secureRandomSpi, Provider provider) {
		super();
		this.provider = provider;
		this.secureRandomSpi = secureRandomSpi;
	}

	/**
	 * Creates a SecureRandom for a given SecureRandomSpi from a provider
	 * 
	 * @param provider
	 *            The provider that is supplying the implementation for the
	 *            service
	 * @param algorithmClass
	 *            The class that implements the algorithm
	 * @param algName
	 *            The name of the algorithm
	 * @return a SecureRandom
	 * @throws NoSuchAlgorithmException 
	 */
	private static SecureRandom createSecureRandom(Provider provider,
			Class algorithmClass, String algName)
			throws NoSuchAlgorithmException {
		try {
			SecureRandomSpi secureRandomSpi = (SecureRandomSpi) algorithmClass
					.newInstance();
			SecureRandom result = new SecureRandom(secureRandomSpi, provider);
			return result;
		} catch (IllegalAccessException e) {
			// Intentionally empty
		} catch (InstantiationException e) {
			// Intentionally empty
		}

		throw new NoSuchAlgorithmException(algName);
	}

	/**
	 * Generates a certain number of seed bytes
	 * 
	 * 
	 * @param numBytes
	 *            int Number of seed bytes to generate
	 * @return byte[] The seed bytes generated
	 */
	public byte[] generateSeed(int numBytes) {
		return secureRandomSpi.engineGenerateSeed(numBytes);
	}

	/**
	 * Answers a new SecureRandom which is capable of running the algorithm
	 * described by the argument. The result will be an instance of a subclass
	 * of SecureRandomSpi which implements that algorithm.
	 * 
	 * @param algorithmName
	 *            java.lang.String Name of the algorithm desired
	 * @return SecureRandom a concrete implementation for the algorithm desired.
	 * 
	 * @exception NoSuchAlgorithmException
	 *                If the algorithm cannot be found
	 */

	public static SecureRandom getInstance(String algorithmName)
			throws NoSuchAlgorithmException {
		if (algorithmName == null)
			throw new java.lang.IllegalArgumentException();

		return toSecureRandomImplementation(algorithmName);
	}

	/**
	 * Answers a new SecureRandom which is capable of running the algorithm
	 * described by the argument. The result will be an instance of a subclass
	 * of SecureRandomSpi which implements that algorithm.
	 * 
	 * @param algorithmName
	 *            java.lang.String Name of the algorithm desired
	 * @param providerName
	 *            java.lang.String Name of the provider which has to implement
	 *            the algorithm
	 * @return SecureRandom a concrete implementation for the algorithm desired.
	 * 
	 * @exception NoSuchAlgorithmException
	 *                If the algorithm cannot be found
	 * @exception NoSuchProviderException
	 *                If the provider cannot be found
	 */
	public static SecureRandom getInstance(String algorithmName,
			String providerName) throws NoSuchAlgorithmException,
			NoSuchProviderException {

		if (providerName == null)
			throw new java.lang.IllegalArgumentException();
		if (algorithmName == null)
			throw new java.lang.IllegalArgumentException();

		Provider provider = Security.getProvider(providerName);
		if (provider == null)
			throw new NoSuchProviderException(providerName);

		return toSecureRandomImplementation(algorithmName, provider);
	}

	/**
	 * Answers a new SecureRandom which is capable of running the algorithm
	 * described by the argument. The result will be an instance of a subclass
	 * of SecureRandomSpi which implements that algorithm.
	 * 
	 * @param algorithm
	 *            java.lang.String Name of the algorithm desired
	 * @param provider
	 *            java.security.Provider Provider which has to implement the
	 *            algorithm
	 * @return SecureRandom a concrete implementation for the algorithm desired.
	 * 
	 * @exception NoSuchAlgorithmException
	 *                If the algorithm cannot be found
	 */
	public static SecureRandom getInstance(String algorithm, Provider provider)
			throws NoSuchAlgorithmException {
		if ((algorithm == null) || (provider == null)) {
			throw new IllegalArgumentException();
		}

		return toSecureRandomImplementation(algorithm, provider);
	}

	/**
	 * Returns the Provider of the secure random represented by the receiver.
	 * 
	 * @return Provider an instance of a subclass of java.security.Provider
	 */
	public final Provider getProvider() {
		return provider;
	}

	/**
	 * Returns the given number of seed bytes, computed using the seed
	 * generation algorithm used by this class.
	 * 
	 * @param numBytes
	 *            int the given number of seed bytes
	 * @return byte[] The seed bytes generated
	 */
	public static byte[] getSeed(int numBytes) {
		if (DEFAULT == null) {
			DEFAULT = new SecureRandom();
		}
		byte[] result = new byte[numBytes];
		DEFAULT.nextBytes(result);
		return result;
	}

	/**
	 * Generates an integer with a given number of pseudo-random bits.
	 * 
	 * @param numBits
	 *            int Number of bits to use for the generation
	 * @return an integer with a given number of pseudo-random bits
	 */
	protected final int next(int numBits) {
		if (numBits == 0)
			return 0;
		int need = numBits / 8;
		int extra = numBits - (need * 8);
		if (extra > 0)
			need++;
		byte[] data = new byte[need];
		nextBytes(data);
		int result = data[0] & 0xff;
		if (extra > 0)
			result >>= (8 - extra);
		for (int i = 1; i < need; i++)
			result = result << 8 | (data[i] & 0xff);
		return result;
	}

	/**
	 * Generates a certain number of random bytes
	 * 
	 * 
	 * @param bytes
	 *            byte[] array to be filled with random bytes
	 */
	public void nextBytes(byte[] bytes) {
		secureRandomSpi.engineNextBytes(bytes);
	}

	/**
	 * Reseeds this random object
	 * 
	 * 
	 * @param seed
	 *            byte[] Bytes to use to reseed the receiver.
	 */
	public void setSeed(byte[] seed) {
		secureRandomSpi.engineSetSeed(seed);
	}

	/**
	 * Reseeds this random object with the eight bytes described by the
	 * representation of the long provided.
	 * 
	 * 
	 * @param seed
	 *            long Number whose representation to use to reseed the
	 *            receiver.
	 */
	public void setSeed(long seed) {
		// For compatibility with Random. This is called by the empty
		// constructor, but the receiver is still being constructed, so we
		// must not forward to the concrete implementation if we have none.
		if (secureRandomSpi == null)
			return; // no-op when running constructor.

		// A long representation is 8 bytes
		byte[] representation = new byte[8];

		for (int i = 7; i >= 0; i--) {
			representation[i] = (byte) (seed & 0xFF);
			seed = seed >>> 8;
		}
		setSeed(representation);
	}

	/**
	 * Answers a class which implements the secure random algorithm named by the
	 * argument. Check all providers for a matching algorithm.
	 * 
	 * @param algorithmName
	 *            java.lang.String the name of the algorithm to search for
	 * 
	 * @return The secure random for the algorithm name supplied by any
	 *         provider.
	 * 
	 * @throws NoSuchAlgorithmException
	 *             If the provider does not implement such algorithm
	 */
	private static SecureRandom toSecureRandomImplementation(
			String algorithmName) throws NoSuchAlgorithmException {

		Provider[] providers = Security.getProviders();
		for (int i = 0; i < providers.length; i++) {
			Provider provider = providers[i];
			try {
				return toSecureRandomImplementation(algorithmName, provider);
			} catch (NoSuchAlgorithmException e) {
				// Just skip to next provider
			}
		}

		// Scanned all, found nothing
		throw new NoSuchAlgorithmException(algorithmName);
	}

	/**
	 * Answers a SecureRandom for the algorithm name supplied by the given
	 * provider.
	 * 
	 * @param algorithmName
	 *            java.lang.String the name of the algorithm to search for
	 * @param provider
	 *            java.security.Provider the provider desired for the algorithm.
	 * 
	 * @return The secure random for the algorithm name supplied by the given
	 *         provider.
	 * 
	 * @throws NoSuchAlgorithmException
	 *             If the provider does not implement such algorithm
	 */
	private static SecureRandom toSecureRandomImplementation(
			String algorithmName, Provider provider)
			throws NoSuchAlgorithmException {

		// First try to find the class corresponding to the algorithm name
		String secureRandomClassName;
		try {
			secureRandomClassName = provider.lookupProperty(KEY_PREFIX,
					algorithmName);
		} catch (ClassCastException e) {
			throw new NoSuchAlgorithmException(algorithmName);
		}

		// If not found, exception
		if (secureRandomClassName == null)
			throw new NoSuchAlgorithmException(algorithmName);

		// Now try to instantiate the digest.
		try {
			Class secureRandomClass = Class.forName(secureRandomClassName,
					true, provider.getClass().getClassLoader());
			return createSecureRandom(provider, secureRandomClass,
					algorithmName);
		} catch (ClassNotFoundException ex) {
			throw new NoSuchAlgorithmException(algorithmName);
		}
	}

	private static final ObjectStreamField[] serialPersistentFields = {
			// These fields are used
			new ObjectStreamField("provider", Provider.class),
			new ObjectStreamField("secureRandomSpi", SecureRandomSpi.class),
			// These fields are for backwards compatibility
			new ObjectStreamField("state", byte[].class),
			new ObjectStreamField("digest", MessageDigest.class),
			new ObjectStreamField("randomBytes", byte[].class),
			new ObjectStreamField("randomBytesUsed", Integer.TYPE),
			new ObjectStreamField("counter", Long.TYPE) };

	private void writeObject(ObjectOutputStream stream) throws IOException {
		ObjectOutputStream.PutField fields = stream.putFields();
		fields.put("provider", provider);
		fields.put("secureRandomSpi", secureRandomSpi);
		stream.writeFields();
	}
}
