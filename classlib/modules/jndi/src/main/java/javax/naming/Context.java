/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.naming;

import java.util.Hashtable;

/**
 * The <code>Context</code> interface describes a naming context comprising a
 * collection of bindings (see <code>javax.naming.Binding</code>) and methods
 * for manipulating them. This interface is extended by interfaces
 * <code>javax.naming.directory.DirContext</code> and
 * <code>javax.naming.event.EventContext</code>. The class
 * <code>javax.naming.InitialContext</code> implements the
 * <code>Context</code> interface.
 * <p>
 * Any of the methods may throw a <code>NamingException</code> or one of its
 * subclasses. The specifications for those exceptions explain the circumstances
 * in which they may be thrown.
 * </p>
 * <p>
 * Name parameters to context methods are each relative to the context. Name
 * parameters cannot be null. The empty name, whether of type <code>Name</code>
 * or <code>String</code>, is the name of the initial context. Names may be
 * composed of multiple components; for example, in the filesystem name
 * "usr/include/stdio.h", each of the components "usr", "include", "stdio.h" is
 * an atomic component, but only "stdio.h" is the terminal atomic component. The
 * example name may be used in context operations providing the intervening
 * parent contexts for "usr" and "include" already exist. Finally, if the
 * current context has name "usr/include", then the example name could be the
 * result of <code>composeName("stdio.h","usr/include")</code>.
 * </p>
 * <p>
 * Depending on whether a naming system supports federation, that is, names that
 * may include components from different naming systems, name parameters to
 * context methods may be considered differently as described below.
 * </p>
 * <p>
 * In systems supporting federation, String name parameters are treated as
 * composite names. When <code>Name</code> parameters are not instances of
 * <code>CompositeName</code>, they are deemed to be compound names (<code>CompoundName</code>
 * instances or subclasses of them). Also, when using <code>list()</code> or
 * <code>listBindings()</code> to obtain a <code>NamingEnumeration</code>,
 * all names in the enumeration are string representations of composite names.
 * </p>
 * <p>
 * Where systems do not support federation, a service provider may treat name
 * parameters to context methods and names found using
 * <code>NamingEnumeration</code> may either as composite names or as compound
 * names. See service provider documentation for details.
 * </p>
 * <p>
 * Any <code>Name</code> parameter specified to a context method is owned by
 * the caller and will remain unchanged, but applications should avoid modifying
 * these <code>Name</code> objects while the operation has not completed. Any
 * <code>Name</code> object returned by a context operation becomes owned by
 * the caller.
 * </p>
 * <p>
 * JNDI applications can provide preferences and configuration information, such
 * as security details for authentication to a service, using JNDI environment
 * properties. JNDI environment properties nearly all begin with "java.naming."
 * except for provider-specific properties (explained below). All specified JNDI
 * environment properties together comprise the context environment and methods
 * are available for examining and manipulating that environment. The
 * environment of a context may not necessarily contain all possible JNDI
 * properties; for example, one or more may remain unspecified.
 * </p>
 * <p>
 * The set of standard JNDI environment properties is:
 * 
 * <pre>
 * Property name                       Value type    Notes
 * -------------                       ----------    -----
 * java.naming.applet                  F
 * java.naming.authoritative           F
 * java.naming.batchsize               F
 * java.naming.dns.url                 F
 * java.naming.factory.control         C             see LdapContext
 * java.naming.factory.initial         F
 * java.naming.factory.object          C
 * java.naming.factory.state           C
 * java.naming.factory.url.pkgs        C
 * java.naming.language                F
 * java.naming.provider.url            F
 * java.naming.referral                F
 * java.naming.security.authentication F
 * java.naming.security.credentials    F
 * java.naming.security.principal      F
 * java.naming.security.protocol       F
 * </pre>
 * 
 * </p>
 * <p>
 * For each property above marked with "C" for "concatenate", when encountered
 * while searching sources of environment properties, values are combined into a
 * single list separated by colons and becomes the resulting value of that
 * property.
 * </p>
 * <p>
 * For each property above marked with "F" for "first occurrence", when
 * encountered while searching sources of environment properties, the first
 * value encountered is the resulting value of that property. In the latter
 * case, and with additional JNDI environment properties explained further
 * below, the type and syntax of acceptable property values should be described
 * in the corresponding documentation for the property. In particular, a
 * property may accept a value consisting of several pieces of relevant
 * information, but the search order and precedence for environment properties
 * ensures that the entire value of the first occurrence of a given property is
 * deemed the value to be used.
 * </p>
 * <p>
 * Additional JNDI environment properties may be defined according to the needs
 * of the particular service and/or service providers and a few guidelines
 * should be followed when choosing appropriate names for them. Such additional
 * properties comprise service-specific, feature-specific, or provider-specific
 * properties.
 * </p>
 * <p>
 * Service-specific JNDI properties may be used by all service providers that
 * offer implementations for a given service and would include the service type
 * in the property name prefix. For example, JNDI service providers for Java RMI
 * should name their service-specific JNDI properties using prefix
 * "java.naming.rmi.", or LDAP service providers should use prefix
 * "java.naming.ldap.".
 * </p>
 * <p>
 * Feature-specific JNDI properties may be used by all service providers
 * offering implementations using a particular flavor of a feature and would
 * include the feature name and the particular flavor name in the property name
 * prefix. A common example is SASL used by several service providers for
 * security; appropriate SASL feature-specific properties would use prefix
 * "java.naming.security.sasl.".
 * </p>
 * <p>
 * Provider-specific JNDI properties are used by only a single provider though a
 * provider may offer more than one service provider implementation. The
 * provider should ensure uniqueness of their provider properties, for example,
 * an LDAP service provider from mycom might use a service provider package name
 * such as "com.mycom.jndi.ldap." as their provider-specific prefix.
 * </p>
 * <p>
 * JNDI environment properties can be specified in a <code>Hashtable</code>
 * and passed as the environment parameter when creating an initial context.
 * </p>
 * <p>
 * Two other important sources of JNDI environment properties are resource files
 * provided by applications and applet parameters (each is considered as an
 * application resource file) and by service provider implementations (provider
 * resource files) in the format of Java properties files - see
 * <code>java.util.Properties</code> class for details.
 * </p>
 * <p>
 * At runtime, the application classpath and, where appropriate, the applet
 * codebase attribute is used to locate the classes to run; when creating the
 * first initial context, the JNDI also searches the same path for all files
 * (application resource files) called "jndi.properties"; it is the classpath
 * associated with the context <code>ClassLoader</code> (for example, the
 * return value from <code>Thread.getContextClassLoader()</code> or from
 * <code>ClassLoader.getSystemClassLoader()</code>) which is searched to get
 * the resource files. Further, a path comprising the value of the "java.home"
 * system property followed by "lib/jndi.properties" is checked for a readable
 * file; if one exists, then that file is used as another application resource
 * file. All application resource files found in the application classpath are
 * examined, but JNDI properties set in a file found early will override the
 * same properties also set in a file found later in the classpath.
 * </p>
 * <p>
 * Provider resource files are located according to the package prefix for the
 * service provider's initial context factory and context implementation class
 * in which dot separator characters are converted into slash path separator
 * characters to construct a filepath appended with "jndiprovider.properties".
 * Consider the example where you have a service provider which supplies a
 * context <code>org.apache.harmony.jndi.example.exampleCtx</code>. In this
 * case the package prefix is <code>org.apache.harmony.jndi.example</code>.
 * Substituting slash chars for dots & appending "jndiprovider.properties" gives
 * you <code>org/apache/harmony/jndi/example/jndiprovider.properties</code>.
 * </p>
 * <p>
 * An important part of service provider implementation is to specify certain
 * standard JNDI properties that are using to locate any of the various factory
 * classes needed for the implementation; these are:
 * 
 * <pre>
 * java.naming.factory.control
 * java.naming.factory.object
 * java.naming.factory.state
 * java.naming.factory.url.pkgs - package prefixes used for URL contexts
 * </pre>
 * 
 * </p>
 * <p>
 * When searching for the above 4 properties only provider resource files should
 * be examined. Although other properties may be specified in them for use by
 * the service provider implementation, the JNDI ignores properties from these
 * files other than those related to factories.
 * </p>
 * <p>
 * It should be noted that a provider resource file's properties differ from
 * those in application resource files in that their values are not incorporated
 * into the environment. Instead, they are read when the following methods are
 * invoked with <code>Context</code> and <code>Hashtable</code> parameters:
 * 
 * <pre>
 * ControlFactory.getControlInstance    - uses java.naming.factory.control
 * DirectoryManager.getObjectInstance   - uses java.naming.factory.object
 * DirectoryManager.getStateToBind      - uses java.naming.factory.state
 * NamingManager.getObjectInstance      - uses java.naming.factory.object
 * NamingManager.getStateToBind         - uses java.naming.factory.state
 * </pre>
 * 
 * </p>
 * <p>
 * These methods use their <code>Hashtable</code> parameter to get the
 * environment properties. Then they use the class loader of the
 * <code>Context</code> parameter to look for the provider resource file. If
 * the file is found, then the value of the required property is appended to the
 * value of the required property in the environment. Note that it is appended
 * for use by this method but the environment itself is unaffected.
 * </p>
 * <p>
 * The <code>jndiprovider.properties</code> files may specify additional
 * properties, but documentation for the service provider should clearly
 * describe which properties are valid in this file and under what
 * circumstances.
 * </p>
 * <p>
 * To summarize the search order and precedence for JNDI environment properties,
 * the earliest having highest precedence:
 * 
 * <pre>
 * 1. environment parameter used to initialize an initial context,
 * 2. applet parameters, (only used if that environment param does not exist)
 * 3. system properties, (only used if that environment and applet parameter 
 *    do not exist)
 * 4. application resource files.
 * </pre>
 * 
 * </p>
 * <p>
 * It should be noted that in the case of applet parameters and system
 * properties only a subset of the properties are read. These are the following
 * 7:
 * 
 * <pre>
 * java.naming.dns.url
 * java.naming.factory.control
 * java.naming.factory.initial
 * java.naming.factory.object
 * java.naming.factory.state
 * java.naming.factory.url.pkgs
 * java.naming.provider.url
 * </pre>
 * 
 * </p>
 * <p>
 * For a JNDI property found in more than one of those sources, if it is one of
 * the JNDI factory list properties then values are joined into a
 * colon-separated list, otherwise the first instance of a property defines the
 * value to be used.
 * </p>
 * <p>
 * The above search order and precedence applies when creating contexts for any
 * class implementing the <code>Context</code> interface.
 * </p>
 * <p>
 * Although a subcontext inherits the environment of its parent context,
 * subsequent changes to either's environment has no direct effect on the other.
 * However, applications should avoid dependency on when JNDI properties are
 * used or verified as this depends on the service provider implementation. As
 * the environment of a context can be examined by any object that has a
 * reference to the context, care should be taken to assess the risk to any
 * security details stored in the environment.
 * </p>
 * <p>
 * Multithreaded access to a single <code>Context</code> instance is only safe
 * when client code uses appropriate synchronization and locking.
 * </p>
 * <p>
 * When a <code>NamingEnumeration</code> is returned by a <code>Context</code>
 * method, the operation should not be considered complete, for concurrency
 * purposes, if the NamingEnumeration is still being used or if any referrals
 * are still being followed resulting from that operation.
 * </p>
 */
public interface Context {

    /**
     * A constant containing environment property name "java.naming.applet". The
     * property may remain unspecified or may be specified within the
     * environment parameter used when creating an initial context. When this
     * environment property is specified, its value must be the currently
     * executing instance of <code>java.applet.Applet</code> to enable the
     * operation of initial context creation to search applet parameters first
     * for other environment properties which may have been specified, before
     * searching for properties in the constructor environment parameter, system
     * properties, and application resource files.
     */
    public static final String APPLET = "java.naming.applet"; //$NON-NLS-1$

    /**
     * A constant containing environment property name
     * "java.naming.authoritative". An application specifies this property to
     * indicate whether naming requests must be made to the most authoritative
     * naming service instance or not. The property may remain unspecified or
     * may be specified with a string value. If unspecified, the property value
     * is considered to be "false". A value of "true" means requests should be
     * made to the most authoritative naming service replicas or caches that may
     * be available. Any value other than "true" means requests may be made to
     * any instance of the naming service which need not be, but may include,
     * the most authoritative.
     */
    public static final String AUTHORITATIVE = "java.naming.authoritative"; //$NON-NLS-1$

    /**
     * A constant containing environment property name "java.naming.batchsize".
     * An application specifies this property to indicate a preference to
     * receive operation results in batches of the given size from the service
     * provider. The property may remain unspecified or may be specified with an
     * integer expressed as a string value. If unspecified, the batch size of
     * operation results is determined by the service provider. The service
     * provider implementation may use or ignore the specified value.
     */
    public static final String BATCHSIZE = "java.naming.batchsize"; //$NON-NLS-1$

    /**
     * A constant containing environment property name "java.naming.dns.url".
     * The property specifies a DNS-scheme URL including the DNS host including
     * domain names, if any; for example, "dns://9.28.36.7/apache.org". If the
     * application uses any JNDI URL with DNS names and a search for this
     * property fails, then the naming operation will throw a
     * <code>ConfigurationException</code>.
     */
    public static final String DNS_URL = "java.naming.dns.url"; //$NON-NLS-1$

    /**
     * A constant containing environment property name
     * "java.naming.factory.initial". The property specifies the name of the
     * factory class, fully-qualified, that will be used to create an initial
     * context; for example, "mycom.jndi.testing.spi.DazzleContextFactory". If
     * the property is not specified, any operation requiring an initial context
     * will throw a <code>NoInitialContextException</code>.
     */
    public static final String INITIAL_CONTEXT_FACTORY = "java.naming.factory.initial"; //$NON-NLS-1$

    /**
     * A constant containing environment property name "java.naming.language".
     * The property indicates the preferred language for operations with the
     * service provider. The property may remain unspecified or should be a
     * string comprising a list of language tags according to RFC 1766 separated
     * by colons. When not specified, the language preference is selected by the
     * service provider.
     */
    public static final String LANGUAGE = "java.naming.language"; //$NON-NLS-1$

    /**
     * A constant containing environment property name
     * "java.naming.factory.object". The property specifies a list of object
     * factories to be used when the application requests an instance of a
     * specified object. The value is a string comprising a list of fully
     * qualified object factory class names separated by colons.
     */
    public static final String OBJECT_FACTORIES = "java.naming.factory.object"; //$NON-NLS-1$

    /**
     * A constant containing environment property name
     * "java.naming.provider.url". The property specifies configuration options
     * for use by the a service provider. The property may remain unspecified or
     * should be a URL string; for example, "ldap://ahost.myfirm.com:389". If
     * not specified, the service provider selects its default configuration.
     */
    public static final String PROVIDER_URL = "java.naming.provider.url"; //$NON-NLS-1$

    /**
     * A constant containing environment property name "java.naming.referral".
     * The property specifies how the service provider should process any
     * referrals encountered during a naming operation. The property may remain
     * unspecified or specified as one of the following strings:
     * <ul>
     * <li>"follow" service provider should always follow referrals</li>
     * <li>"ignore" service provider should ignore referrals</li>
     * <li>"throw" service provider should throw ReferralException if it
     * encounters a referral</li>
     * </ul>
     * When not specified, the service provider selects a default value.
     */
    public static final String REFERRAL = "java.naming.referral"; //$NON-NLS-1$

    /**
     * A constant containing environment property name
     * "java.naming.security.authentication". The property specifies the
     * security level to be used in naming operations. The property may remain
     * unspecified or be one of the strings "none", "simple", "strong". When not
     * specified, the service provider selects a default value.
     */
    public static final String SECURITY_AUTHENTICATION = "java.naming.security.authentication"; //$NON-NLS-1$

    /**
     * A constant containing environment property name
     * "java.naming.security.credentials". The property specifies credentials of
     * the security principal so that the caller can be authenticated to the
     * naming service. The property may remain unspecified or be a value
     * according to the authentication scheme controlling access to the service.
     * When not specified, the service provider determines how to respond to
     * service requests affected by the lack of security credentials.
     */
    public static final String SECURITY_CREDENTIALS = "java.naming.security.credentials"; //$NON-NLS-1$

    /**
     * A constant containing environment property name
     * "java.naming.security.principal". The property the name of the security
     * principal to be used when the caller needs to be authenticated to the
     * naming service. The property may remain unspecified or be a value
     * according to the authentication scheme controlling access to the service.
     * When not specified, the service provider determines how to respond to
     * service requests affected by the lack of a security principal.
     */
    public static final String SECURITY_PRINCIPAL = "java.naming.security.principal"; //$NON-NLS-1$

    /**
     * A constant containing environment property name
     * "java.naming.security.protocol". The property the name of the security
     * protocol to be used with the naming service. The property may remain
     * unspecified or be specified as a string according to the service provider
     * implementation. When not specified, the service provider determines how
     * to respond to service requests.
     */
    public static final String SECURITY_PROTOCOL = "java.naming.security.protocol"; //$NON-NLS-1$

    /**
     * A constant containing environment property name
     * "java.naming.factory.state". The property specifies a list of state
     * factories to be used when the application requests the state of a
     * specified object. The value is a string comprising a list of fully
     * qualified state factory class names separated by colons. The property may
     * remain unspecified.
     */
    public static final String STATE_FACTORIES = "java.naming.factory.state"; //$NON-NLS-1$

    /**
     * A constant containing environment property name
     * "java.naming.factory.url.pkgs". The property specifies a list of package
     * prefixes that are used to load URL context factories. The value is a
     * string comprising a list of package prefixes for class names of URL
     * context factory classes separated by colons. The property may remain
     * unspecified. In any case, prefix "com.sun.jndi.url" is automatically
     * added to the list of specified package prefixes or used as the only
     * package prefix when the property is unspecified.
     */
    public static final String URL_PKG_PREFIXES = "java.naming.factory.url.pkgs"; //$NON-NLS-1$

    /**
     * Adds or replaces the environment property specified by the non-null
     * string parameter into the environment of this context with the specified
     * object value. Returns the previous property value if replaced, or null if
     * the property did not exist in the environment.
     * 
     * @param s
     *            the name of the property to add
     * @param o
     *            the value of the property to add
     * @return the previous property value if replaced, or null if the property
     *         did not exist in the environment.
     * @throws NamingException
     *             if an error occurs.
     */
    public Object addToEnvironment(String s, Object o) throws NamingException;

    /**
     * Binds the specified name to the specified object in this context. The
     * specified name may not be null. The specified object may be null when a
     * name bound to a null object is meaningful in the semantics of the
     * underlying naming system, otherwise a <code>NamingException</code> is
     * thrown.
     * 
     * @param n
     *            a <code>Name</code>, may not be null
     * @param o
     *            an object to bind with the name, may be null
     * @throws NamingException
     *             if an error occurs.
     */
    public void bind(Name n, Object o) throws NamingException;

    /**
     * Binds the specified name to the specified object in this context. The
     * specified name may not be null. The specified object may be null when a
     * name bound to a null object is meaningful in the semantics of the
     * underlying naming system, otherwise a <code>NamingException</code> is
     * thrown.
     * 
     * @param s
     *            a name in string, may not be null
     * @param o
     *            an object to bind with the name, may be null
     * @throws NamingException
     *             if an error occurs.
     */
    public void bind(String s, Object o) throws NamingException;

    /**
     * Closes this context. The result of any further operations on a closed
     * context is undefined.
     * 
     * @throws NamingException
     *             if an error occurs.
     */
    public void close() throws NamingException;

    /**
     * Combines two names into a composite name according to the syntax for this
     * context. The name <code>pfx</code> is expected to be the name of one or
     * more of the immediate parent contexts of this context. The name
     * <code>n</code> is a name relative to this context. Neither
     * <code>pfx</code> nor <code>n</code> may be null. The combined result
     * is a name which is relative to the specified parent context names.
     * 
     * @param n
     *            a <code>Name</code>, may not be null
     * @param pfx
     *            a <code>Name</code> serves as prefix, may not be null
     * @return the combined name
     * @throws NamingException
     *             if an error occurs.
     */
    public Name composeName(Name n, Name pfx) throws NamingException;

    /**
     * Combines two names into a composite name according to the syntax for this
     * context. The name <code>pfx</code> is expected to be the name of one or
     * more of the immediate parent contexts of this context. The name
     * <code>s</code> is a name relative to this context. Neither
     * <code>pfx</code> nor <code>s</code> may be null. The combined result
     * is a name which is relative to the specified parent context names.
     * 
     * @param s
     *            a name in string, may not be null
     * @param pfx
     *            a name in string, serves as prefix, may not be null
     * @return the combined name in string
     * @throws NamingException
     *             if an error occurs.
     */
    public String composeName(String s, String pfx) throws NamingException;

    /**
     * Creates a new context with the specified name as a child of this context
     * and creates a binding for the name with the new context object in this
     * context. This is analogous to creating a new lower level in a
     * hierarchical naming system.
     * 
     * @param n
     *            the name of the new subcontext
     * @return the created subcontext
     * @throws NamingException
     *             if an error occurs.
     */
    public Context createSubcontext(Name n) throws NamingException;

    /**
     * Creates a new context with the specified name as a child of this context
     * and creates a binding for the name with the new context object in this
     * context. This is analogous to creating a new lower level in a
     * hierarchical naming system.
     * 
     * @param s
     *            the name of the new subcontext, in string
     * @return the created subcontext
     * @throws NamingException
     *             if an error occurs.
     */
    public Context createSubcontext(String s) throws NamingException;

    /**
     * Removes a child context with the specified name from this context
     * together with any attributes associated with that name. If the specified
     * context does not exist, but intervening contexts do exist, then the
     * operation is is considered to succeed.
     * <p>
     * Care must be taken with composite names crossing multiple naming systems.
     * A composite name containing a name component which is bound to an object
     * in a different naming system cannot be used to destroy that name
     * subcontext because the subcontext is not of the same type as the context
     * containing the binding. <code>Unbind()</code> can be used to destroy
     * the binding of the specified name in this context to the object in the
     * other naming system. To remove the context object in the other naming
     * system, first obtain a context belonging to the other naming system, then
     * use <code>destroySubcontext()</code> on that context.
     * </p>
     * 
     * @param n
     *            the name of the subcontext to destroy
     * @throws NamingException
     *             if an error occurs.
     */
    public void destroySubcontext(Name n) throws NamingException;

    /**
     * Removes a child context with the specified name from this context
     * together with any attributes associated with that name. If the specified
     * context does not exist, but intervening contexts do exist, then the
     * operation is considered to succeed.
     * <p>
     * Care must be taken with composite names crossing multiple naming systems.
     * A composite name containing a name component which is bound to an object
     * in a different naming system cannot be used to destroy that name
     * subcontext because the subcontext is not of the same type as the context
     * containing the binding. <code>Unbind()</code> can be used to destroy
     * the binding of the specified name in this context to the object in the
     * other naming system. To remove the context object in the other naming
     * system, first obtain a context belonging to the other naming system, then
     * use <code>destroySubcontext()</code> on that context.
     * </p>
     * 
     * @param s
     *            the name of the subcontext to destroy
     * @throws NamingException
     *             if an error occurs.
     */
    public void destroySubcontext(String s) throws NamingException;

    /**
     * Returns a non-null reference to the current environment properties for
     * this context. The only proper ways to modify the properties for this
     * context are using the <code>addToEnvironment()</code> and
     * <code>removeFromEnvironment()</code> methods.
     * 
     * @return a non-null reference to the current environment properties for
     *         this context, which should not be modified
     * @throws NamingException
     *             if an error occurs.
     */
    public Hashtable<?, ?> getEnvironment() throws NamingException;

    /**
     * Returns the complete name as a string for this context in the namespace.
     * For example, in a namespace accessed using a file system service provider
     * on a computer running the Windows operating system,
     * <code>getNameInNamespace()</code> will return a string comprising the
     * current working disk drive such as "F:\". The returned name is never null
     * and should not be used in any naming operations.
     * 
     * @return the complete name as a string for this context in the namespace
     * @throws NamingException
     *             if an error occurs.
     * @throws OperationNotSupportedException
     *             in cases of naming systems where a full name has no meaning.
     */
    public String getNameInNamespace() throws NamingException;

    /**
     * Returns a parser object for the named context. When using a federation of
     * naming systems in which each has its own rules for parsing names for its
     * namespace, each naming system will have a different parser. The parser
     * for a given context can parse a name composed of several components into
     * atomic components according to the rules for the naming system associated
     * with the specified context.
     * 
     * @param n
     *            a <code>Name</code>
     * @return a parser object for the named context
     * @throws NamingException
     *             if an error occurs.
     */
    public NameParser getNameParser(Name n) throws NamingException;

    /**
     * Returns a parser object for the named context. When using a federation of
     * naming systems in which each has its own rules for parsing names for its
     * namespace, each naming system will have a different parser. The parser
     * for a given context can parse a name composed of several components into
     * atomic components according to the rules for the naming system associated
     * with the specified context.
     * 
     * @param s
     *            a name in string
     * @return a parser object for the named context
     * @throws NamingException
     *             if an error occurs.
     */
    public NameParser getNameParser(String s) throws NamingException;

    /**
     * Returns an enumeration of the bindings of the context for the specified
     * name excluding any bindings for any subcontexts. If any binding for the
     * context is changed before closing the enumeration, the state of the
     * enumeration is undefined. Each element of the enumeration is a
     * <code>NameClassPair</code> object.
     * 
     * @param n
     *            a <code>Name</code>
     * @return an enumeration of the bindings of the context for the specified
     *         name excluding any bindings for any subcontexts
     * @throws NamingException
     *             if an error occurs.
     */
    public NamingEnumeration<NameClassPair> list(Name n) throws NamingException;

    /**
     * Returns an enumeration of the bindings of the context for the specified
     * name excluding any bindings for any subcontexts. If any binding for the
     * context is changed before closing the enumeration, the state of the
     * enumeration is undefined. Each element of the enumeration is a
     * <code>NameClassPair</code> object.
     * 
     * @param s
     *            a name in string
     * @return an enumeration of the bindings of the context for the specified
     *         name excluding any bindings for any subcontexts
     * @throws NamingException
     *             if an error occurs.
     */
    public NamingEnumeration<NameClassPair> list(String s)
            throws NamingException;

    /**
     * Returns an enumeration of the bindings of the context for the specified
     * name excluding any bindings for any subcontexts. If any binding for the
     * context is changed before closing the enumeration, the state of the
     * enumeration is undefined. Each element of the enumeration is a
     * <code>Binding</code> object.
     * 
     * @param n
     *            a <code>Name</code>
     * @return an enumeration of the bindings of the context for the specified
     *         name excluding any bindings for any subcontexts
     * @throws NamingException
     *             if an error occurs.
     */
    public NamingEnumeration<Binding> listBindings(Name n)
            throws NamingException;

    /**
     * Returns an enumeration of the bindings of the context for the specified
     * name excluding any bindings for any subcontexts. If any binding for the
     * context is changed before closing the enumeration, the state of the
     * enumeration is undefined. Each element of the enumeration is a
     * <code>Binding</code> object.
     * 
     * @param s
     *            a name in string
     * @return an enumeration of the bindings of the context for the specified
     *         name excluding any bindings for any subcontexts
     * @throws NamingException
     *             if an error occurs.
     */
    public NamingEnumeration<Binding> listBindings(String s)
            throws NamingException;

    /**
     * Returns the object bound to the specified name in this context. If the
     * specified name is empty, a new instance of this context is returned,
     * complete with its own environment properties.
     * 
     * @param n
     *            a <code>Name</code> to lookup
     * @return the object bound to the specified name in this context
     * @throws NamingException
     *             if an error occurs.
     */
    public Object lookup(Name n) throws NamingException;

    /**
     * Returns the object bound to the specified name in this context. If the
     * specified name is empty, a new instance of this context is returned,
     * complete with its own environment properties.
     * 
     * @param s
     *            a name to lookup
     * @return the object bound to the specified name in this context
     * @throws NamingException
     *             if an error occurs.
     */
    public Object lookup(String s) throws NamingException;

    /**
     * Returns the object bound to the specified name in this context by
     * following any links. If the specified name is not a link, then the object
     * is returned.
     * 
     * @param n
     *            a <code>Name</code> to lookup
     * @return the object bound to the specified name in this context by
     *         following any links
     * @throws NamingException
     *             if an error occurs.
     */
    public Object lookupLink(Name n) throws NamingException;

    /**
     * Returns the object bound to the specified name in this context by
     * following any links. If the specified name is not a link, then the object
     * is returned.
     * 
     * @param s
     *            a name in string to lookup
     * @return the object bound to the specified name in this context by
     *         following any links
     * @throws NamingException
     *             if an error occurs.
     */
    public Object lookupLink(String s) throws NamingException;

    /**
     * Binds the specified name to the specified object, replacing any existing
     * binding for the specified name. The specified name may not be empty. The
     * specified object may be null.
     * 
     * @param n
     *            a <code>Name</code> to rebind, may not be null
     * @param o
     *            an object to bind with the name, may be null
     * @throws NamingException
     *             if an error occurs.
     */
    public void rebind(Name n, Object o) throws NamingException;

    /**
     * Binds the specified name to the specified object, replacing any existing
     * binding for the specified name. The specified name may not be empty. The
     * specified object may be null.
     * 
     * @param s
     *            a name in string to rebind, may not be null
     * @param o
     *            an object tobind with the name, may be null
     * @throws NamingException
     *             if an error occurs.
     */
    public void rebind(String s, Object o) throws NamingException;

    /**
     * Removes the environment property specified by the non-null parameter from
     * the environment of this context. Returns the value that the property had
     * before removal, or null if the property did not exist in the environment.
     * 
     * @param s
     *            a property name
     * @return the value that the property had before removal, or null if the
     *         property did not exist in the environment
     * @throws NamingException
     *             if an error occurs.
     */
    public Object removeFromEnvironment(String s) throws NamingException;

    /**
     * Binds a specified new name to the object, and any attributes, previously
     * bound to the specified old name. The old name is removed from the
     * bindings for this context.
     * 
     * @param nOld
     *            the old name
     * @param nNew
     *            the new name
     * @throws NameAlreadyBoundException
     *             if the new is already bound
     * @throws NamingException
     *             if an error occurs.
     */
    public void rename(Name nOld, Name nNew) throws NamingException;

    /**
     * Binds a specified new name to the object, and any attributes, previously
     * bound to the specified old name. The old name is removed from the
     * bindings for this context. Neither the new nor the old name may be empty.
     * 
     * @param sOld
     *            the old name in string
     * @param sNew
     *            the new name in string
     * @throws NameAlreadyBoundException
     *             if the new is already bound
     * @throws NamingException
     *             if an error occurs.
     */
    public void rename(String sOld, String sNew) throws NamingException;

    /**
     * Removes the terminal atomic name component of the specified name from the
     * bindings in this context, together with any attributes associated with
     * the terminal atomic name. Providing that other parts of the specified
     * name exist in this context's bindings, the operation succeeds whether or
     * not the terminal atomic name exists, otherwise a
     * <code>NameNotFoundException</code> is thrown. Any intermediate contexts
     * remain unchanged.
     * 
     * @param n
     *            a <code>Name</code> to unbind
     * @throws NamingException
     *             if an error occurs.
     */
    public void unbind(Name n) throws NamingException;

    /**
     * Removes the terminal atomic name component of the specified name from the
     * bindings in this context, together with any attributes associated with
     * the terminal atomic name. Providing that other parts of the specified
     * name exist in this context's bindings, the operation succeeds whether or
     * not the terminal atomic name exists, otherwise a
     * <code>NameNotFoundException</code> is thrown. Any intermediate contexts
     * remain unchanged.
     * 
     * @param s
     *            a name in string to unbind
     * @throws NamingException
     *             if an error occurs.
     */
    public void unbind(String s) throws NamingException;

}
