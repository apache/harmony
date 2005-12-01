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

package java.security.cert;


import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import com.ibm.oti.util.Msg;

/**
 * This class provides the functionality of a certificate factory algorithm.
 */
public class CertificateFactory {

	// Key prefix for algorithm name lookup
	private static final String KEY_PREFIX = "CertificateFactory.";

	// Certificate type being used
	private String type;

	// Provider of the key algorithm represented by the receiver
	private Provider provider;

	// The actual provider-supplied implementation
	private CertificateFactorySpi certificateFactorySpi;

	/**
	 * Constructs a new instance of this class with the algorithm provided.
	 * 
	 * @param certFacSpi
	 *            CertificateFactorySpi The actual certificate factory
	 *            implementation
	 * @param provider
	 *            Provider The provider of the certificate factory
	 * @param type
	 *            String Certificate type
	 */
	protected CertificateFactory(CertificateFactorySpi certFacSpi,
			Provider provider, String type) {
		super();
		setProvider(provider);
		setType(type);
		this.certificateFactorySpi = certFacSpi;
	}

	/**
	 * Creates a CertificateFactory for a given CertificateFactorySpi from a
	 * provider
	 * 
	 * @param provider
	 *            Provider The provider that is supplying the implementation for
	 *            the service
	 * @param certClass
	 *            Class The class that implements the type wanted
	 * @param type
	 *            String The type desired
	 * 
	 * @return a CertificateFactory for a given CertificateFactorySpi from a
	 *         provider
	 * 
	 * @throws CertificateException
	 *             if parsing problems are detected
	 */
	private static CertificateFactory createCertificateFactory(
			Provider provider, Class certClass, String type)
			throws CertificateException {
		try {
			CertificateFactorySpi providedCertificateFactory = (CertificateFactorySpi) certClass
					.newInstance();
			return new CertificateFactory(providedCertificateFactory, provider,
					type);
		} catch (IllegalAccessException e) {
		} catch (InstantiationException e) {
		}

		throw new CertificateException(type);
	}

	/**
	 * Generates and initializes a Certificate from data from the provided input
	 * stream.
	 * 
	 * @param inStream
	 *            InputStream Stream from where data is read to create the
	 *            Certificate
	 * 
	 * @return Certificate an initialized Certificate
	 * @exception CertificateException
	 *                if parsing problems are detected
	 */
	public final Certificate generateCertificate(InputStream inStream)
			throws CertificateException {
		if (inStream == null) {
			throw new CertificateException(Msg.getString("K00a5", "null"));
		}

		return certificateFactorySpi.engineGenerateCertificate(inStream);
	}

	/**
	 * Generates and initializes a collection of Certificates from data from the
	 * provided input stream.
	 * 
	 * @param inStream
	 *            InputStream Stream from where data is read to create the
	 *            Certificates
	 * 
	 * @return Collection an initialized collection of Certificates
	 * @exception CertificateException
	 *                if parsing problems are detected
	 */
	public final Collection generateCertificates(InputStream inStream)
			throws CertificateException {
		if (inStream == null) {
			throw new CertificateException(Msg.getString("K00a5", "null"));
		}

		return certificateFactorySpi.engineGenerateCertificates(inStream);
	}

	/**
	 * Generates and initializes a Certificate Revocation List from data from
	 * the provided input stream.
	 * 
	 * @param inStream
	 *            InputStream Stream from where data is read to create the CRL
	 * 
	 * @return CRL an initialized Certificate Revocation List
	 * @exception CRLException
	 *                if parsing problems are detected
	 */
	public final CRL generateCRL(InputStream inStream) throws CRLException {

		if (inStream == null) {
			throw new CRLException(Msg.getString("K00a5", "null"));
		}
		return certificateFactorySpi.engineGenerateCRL(inStream);
	}

	/**
	 * Generates a <code>CertPath</code> from data from the provided
	 * <code>InputStream</code>. The default encoding is assumed.
	 * 
	 * @param inStream
	 *            InputStream with PKCS7 or PkiPath encoded data
	 * 
	 * @return CertPath a CertPath initialized from the provided data
	 * 
	 * @throws CertificateException
	 *             if parsing problems are detected
	 */
	public final CertPath generateCertPath(InputStream inStream)
			throws CertificateException {
		if (inStream == null) {
			throw new CertificateException(Msg.getString("K00a5", "null"));
		}

		return certificateFactorySpi.engineGenerateCertPath(inStream);
	}

	/**
	 * Generates a <code>CertPath</code> from data from the provided
	 * <code>InputStream</code>. The encoding is that specified by the
	 * encoding parameter.
	 * 
	 * @param inStream
	 *            InputStream containing certificate path data in specified
	 *            encoding
	 * @param encoding
	 *            encoding of the data in the input stream
	 * 
	 * @return CertPath a CertPath initialized from the provided data
	 * 
	 * @throws CertificateException
	 *             if parsing problems are detected
	 * @throws UnsupportedOperationException
	 *             if the provider does not implement this method
	 */
	public final CertPath generateCertPath(InputStream inStream, String encoding)
			throws CertificateException {
		if (inStream == null) {
			throw new CertificateException(Msg.getString("K00a5", "null"));
		}
		if (encoding == null) {
			throw new CertificateException(Msg.getString("K00a5", encoding));
		}

		return certificateFactorySpi.engineGenerateCertPath(inStream, encoding);
	}

	/**
	 * Generates a <code>CertPath</code> from the provided List of
	 * Certificates. The encoding is the default encoding.
	 * 
	 * @param certificates
	 *            List containing certificates in a format supported by the
	 *            CertificateFactory
	 * 
	 * @return CertPath a CertPath initialized from the provided data
	 * 
	 * @throws CertificateException
	 *             if parsing problems are detected
	 * @throws UnsupportedOperationException
	 *             if the provider does not implement this method
	 */
	public final CertPath generateCertPath(List certificates)
			throws CertificateException {
		if (certificates == null) {
			throw new CertificateException(Msg.getString("K00a5", "null"));
		}
		return certificateFactorySpi.engineGenerateCertPath(certificates);
	}

	/**
	 * Returns an Iterator over the supported CertPath encodings (as Strings).
	 * The first element is the default encoding.
	 * 
	 * @return Iterator Iterator over supported CertPath encodings (as Strings)
	 */
	public final Iterator getCertPathEncodings() {
		return certificateFactorySpi.engineGetCertPathEncodings();
	}

	/**
	 * Generates and initializes a collection of Certificate Revocation List
	 * from data from the provided input stream.
	 * 
	 * @param inStream
	 *            InputStream Stream from where data is read to create the CRLs
	 * 
	 * @return Collection an initialized collection of Certificate Revocation
	 *         List
	 * @exception CRLException
	 *                if parsing problems are detected
	 */
	public final Collection generateCRLs(InputStream inStream)
			throws CRLException {
		if (inStream == null) {
			throw new CRLException(Msg.getString("K00a5", "null"));
		}
		return certificateFactorySpi.engineGenerateCRLs(inStream);
	}

	/**
	 * Answers a new CertificateFactory of the given type.
	 * 
	 * @param type
	 *            java.lang.String Type of certificate desired
	 * @return CertificateFactory a concrete implementation for the certificate
	 *         type desired.
	 * 
	 * @exception CertificateException
	 *                If the type cannot be found
	 */
	public static final CertificateFactory getInstance(String type)
			throws CertificateException {
		// *** NOTE *** - This method is declared to return CertificateFactory,
		// while providers are supposed to subclass CertificateFactorySpi. So,
		// we cannot simply return an instance of a subclass of
		// CertificateFactorySpi. This is why we have the instance variable
		// pointing to the actual implementation

		if (type == null)
			throw new CertificateException(Msg.getString("K0337"));

		return toCertificateFactoryImplementation(type);
	}

	/**
	 * Answers a new CertificateFactory of the given type.
	 * 
	 * @param type
	 *            java.lang.String Type of certificatem desired
	 * @param providerName
	 *            java.lang.String Name of the provider which has to implement
	 *            the algorithm
	 * @return CertificateFactory a concrete implementation for the certificate
	 *         type desired.
	 * 
	 * @exception CertificateException
	 *                If the type cannot be found
	 * @exception NoSuchProviderException
	 *                If the provider cannot be found
	 */
	public static final CertificateFactory getInstance(String type,
			String providerName) throws CertificateException,
			NoSuchProviderException {
		// *** NOTE *** - This method is declared to return CertificateFactory,
		// while providers are supposed to subclass CertificateFactorySpi. So,
		// we cannot simply return an instance of a subclass of
		// CertificateFactorySpi. This is why we have the instance variable
		// pointing to the actual implementation

		if (providerName == null)
			throw new java.lang.IllegalArgumentException();

		if (type == null)
			throw new CertificateException(Msg.getString("K0337"));

		Provider provider = Security.getProvider(providerName);
		if (provider == null)
			throw new NoSuchProviderException(providerName);

		return toCertificateFactoryImplementation(type, provider);
	}

	/**
	 * Answers a new CertificateFactory of the given type.
	 * 
	 * @param type
	 *            java.lang.String Type of certificatem desired
	 * @param provider
	 *            java.security.Provider Provider which has to implement the
	 *            algorithm
	 * @return CertificateFactory a concrete implementation for the certificate
	 *         type desired.
	 * 
	 * @exception CertificateException
	 *                If the type cannot be found
	 */
	public static final CertificateFactory getInstance(String type,
			Provider provider) throws CertificateException {
		if (type == null)
			throw new CertificateException(Msg.getString("K0337"));

		if (provider == null) {
			throw new IllegalArgumentException();
		}

		return toCertificateFactoryImplementation(type, provider);
	}

	/**
	 * Returns the Provider of the certificate factory represented by the
	 * receiver.
	 * 
	 * @return Provider an instance of a subclass of java.security.Provider
	 */
	public final Provider getProvider() {
		return provider;
	}

	/**
	 * Returns the Certificate type
	 * 
	 * @return String type of certificate being used
	 */
	public final String getType() {
		return type;
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
	 * Set the certificate type being used by the receiver to the argument.
	 * 
	 * @param type
	 *            String the type of the certificate
	 */
	void setType(String type) {
		this.type = type;
	}

	/**
	 * Answers a CertificateFactory for the type name
	 * 
	 * @param type
	 *            java.lang.String the type of certificate desired
	 * 
	 * @return The CertificateFactory for the type name .
	 * @throws CertificateException
	 *             If the requested type is not available
	 */
	private static CertificateFactory toCertificateFactoryImplementation(
			String type) throws CertificateException {

		Provider[] providers = Security.getProviders();
		for (int i = 0; i < providers.length; i++) {
			Provider provider = providers[i];
			try {
				return toCertificateFactoryImplementation(type, provider);
			} catch (CertificateException e) {
				// Just skip to next provider
			}
		}

		// Scanned all, found nothing
		throw new CertificateException(type);
	}

	/**
	 * Answers a CertificateFactory for the type name
	 * 
	 * @param type
	 *            java.lang.String the type of certificate desired
	 * 
	 * @param provider
	 *            java.security.Provider the provider desired for the type.
	 * 
	 * @return The CertificateFactory for the type name .
	 * @throws CertificateException
	 *             If the requested type is not available
	 */
	private static CertificateFactory toCertificateFactoryImplementation(
			String type, Provider provider) throws CertificateException {

		// First try to find the class corresponding to the algorithm name
		String certificateFactoryClassName;
		try {
			certificateFactoryClassName = lookupProperty(provider, KEY_PREFIX,
					type);
		} catch (ClassCastException e) {
			throw new CertificateException(type);
		}

		// If not found, exception
		if (certificateFactoryClassName == null)
			throw new CertificateException(type);

		// Now try to instantiate the certificate factory.
		try {
			Class certFactClass = Class.forName(certificateFactoryClassName,
					true, provider.getClass().getClassLoader());
			return createCertificateFactory(provider, certFactClass, type);
		} catch (ClassNotFoundException ex) {
			throw new CertificateException(type);
		}
	}

	private static String lookupProperty(Provider provider, String property) {
		String result = provider.getProperty(property);
		if (result != null)
			return result;
		String upper = property.toUpperCase();
		Enumeration keyEnum = provider.keys();
		while (keyEnum.hasMoreElements()) {
			String key = (String) keyEnum.nextElement();
			if (key.toUpperCase().equals(upper))
				return provider.getProperty(key);
		}
		return null;
	}

	private static String lookupProperty(Provider provider, String prefix,
			String name) {
		String property = prefix + name;
		String result = lookupProperty(provider, property);
		if (result != null)
			return result;
		result = lookupProperty(provider, "Alg.Alias." + property);
		if (result != null)
			return provider.getProperty(prefix + result);
		return null;
	}
}
