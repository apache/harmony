/* Copyright 1998, 2002 The Apache Software Foundation or its licensors, as applicable
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


import java.io.Serializable;

/**
 * This class is a Service Provider Interface (therefore the Spi suffix) for
 * secure random number generation algorithms to be supplied by providers.
 * 
 */
public abstract class SecureRandomSpi implements Serializable {
	/**
	 * Constructs a new instance of this class.
	 */
	public SecureRandomSpi() {
		// Intentionally empty
	}

	/**
	 * Generates a certain number of seed bytes
	 * 
	 * @param numBytes
	 *            int Number of seed bytes to generate
	 * @return byte[] The seed bytes generated
	 */
	protected abstract byte[] engineGenerateSeed(int numBytes);

	/**
	 * Generates a certain number of random bytes
	 * 
	 * 
	 * @param bytes
	 *            byte[] array to be filled with random bytes
	 */
	protected abstract void engineNextBytes(byte[] bytes);

	/**
	 * Reseeds this random object
	 * 
	 * 
	 * @param seed
	 *            byte[] The number of seed bytes to generate
	 */
	protected abstract void engineSetSeed(byte[] seed);
}
