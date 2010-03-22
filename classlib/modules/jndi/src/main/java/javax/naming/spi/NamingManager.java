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

package javax.naming.spi;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.naming.CannotProceedException;
import javax.naming.Context;
import org.apache.harmony.jndi.internal.EnvironmentReader;
import org.apache.harmony.jndi.internal.UrlParser;
import org.apache.harmony.jndi.internal.nls.Messages;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;

/**
 * The <code>NamingManager</code> class should not be instantiated although it
 * can be extended by classes within the <code>javax.naming.spi</code> package -
 * see {@link DirectoryManager}. All its methods are static.
 * <p>
 * The methods are used by service providers for accessing object and state
 * factories and for determining continuation contexts. Many of the methods
 * create objects. These may be <code>Context</code> objects or objects
 * referred to by the naming service.
 * </p>
 * <p>
 * The <code>Name</code> and <code>Hashtable</code> arguments passed to the
 * <code>NamingManager</code> methods remain owned purely by the calling
 * method. They must not be changed or referenced.
 * </p>
 * <p>
 * It should be noted that it is possible for an application to access a
 * namespace other than that supplied by the default <code>InitialContext</code>
 * (as specified by <code>Context.INITIAL_CONTEXT_FACTORY</code>). It is
 * possible to call the following <code>InitialContext</code> methods passing
 * a URL string either as the <code>String</code> or <code>Name</code>
 * parameter: <code>lookup, bin, rebind, unbind, rename, list, listBindings, 
 * destroySubcontext, createSubcontext, lookupLink, getNameParser</code>.
 * This allows you to have one <code>InitialContext</code> object where these
 * methods usually use the default initial context but access a URL
 * <code>Context</code> instead when invoked with a URL string.
 * </p>
 * <p>
 * A URL string is of the format abc:\nnnnnn where abc is the scheme of the URL.
 * (See <code>InitialContext</code> where it refers to RFC1738.) When a URL
 * string is supplied to those <code>InitialContext</code> methods, a URL
 * context is used instead of the default initial context when performing that
 * method. URL context factories are used to create URL contexts. A URL context
 * factory is really just a service provider's implementation of an
 * <code>ObjectFactory</code>. It is not essential that a service provider
 * supplies one if they do not wish to support URL <code>Contexts.</code>
 * </p>
 * <p>
 * See the <code>getURLContext</code> method for a description of how a URL
 * context factory is located.
 * </p>
 * <p>
 * Please note that multithreaded access to this class must be safe. For
 * example, for thread safety, it should not be possible for one thread to read
 * the installed <code>InitialContextFactoryBuilder</code> or
 * <code>ObjectFactoryBuilder</code> while another thread is in the process of
 * setting it.
 * </p>
 * <p>
 * Also note that privileges should be granted to get the context classloader
 * and to read the resource files.
 * </p>
 * 
 * @see DirectoryManager
 */
public class NamingManager {

    /**
     * The property name of <code>CannotProceedException</code> in a context's
     * environment.
     */
    public static final String CPE = "java.naming.spi.CannotProceedException"; //$NON-NLS-1$

    static InitialContextFactoryBuilder icfb;

    static ObjectFactoryBuilder ofb;

    NamingManager() {
        super();
        // package private to prevent it being instanced but make it can be
        // subclassed by DirectoryManager
    }

    /**
     * Install an <code>InitialContextFactoryBuilder</code>. Once this has
     * been set it cannot be reset. Attempts to do so cause an
     * <code>IllegalStateException</code>. The builder can only be installed
     * if the security policy allows the setting of the factory.
     * 
     * @param icfb
     *            the builder to be installed - can be null, but then no builder
     *            is installed.
     * @throws IllegalStateException
     *             if an builder has already been installed.
     * @throws SecurityException
     *             is a security error prevents the installation.
     * @throws NamingException
     *             for other errors encountered.
     */
    public static void setInitialContextFactoryBuilder(
            InitialContextFactoryBuilder icfb) throws IllegalStateException,
            SecurityException, NamingException {
        // check security access
        SecurityManager sm = System.getSecurityManager();
        if (null != sm) {
            sm.checkSetFactory();
        }
        synchronized (NamingManager.class) {
            if (null != NamingManager.icfb) {
                // jndi.1E=InitialContextFactoryBuilder cannot be reset
                throw new IllegalStateException(Messages.getString("jndi.1E")); //$NON-NLS-1$
            }
            NamingManager.icfb = icfb;
        }
    }

    /**
     * Returns true when an <code>InitialContextFactoryBuilder</code> has been
     * installed.
     * 
     * @return true when an <code>InitialContextFactoryBuilder</code> has been
     *         installed.
     */
    public static boolean hasInitialContextFactoryBuilder() {
        return null != icfb;
    }

    /**
     * Install an <code>ObjectFactoryBuilder</code>. Once this has been set
     * it cannot be reset. Attempts to do so cause an
     * <code>IllegalStateException</code>. The builder can only be installed
     * if the security policy allows the setting of the factory.
     * 
     * @param ofb
     *            the <code>ObjectFactoryBuilder</code> to be installed - can
     *            be null, but then no builder is installed.
     * @throws IllegalStateException
     *             if an <code>ObjectFactoryBuilder</code> has already been
     *             installed.
     * @throws SecurityException
     *             is a security error prevents the installation.
     * @throws NamingException
     *             for other errors encountered.
     */
    public static synchronized void setObjectFactoryBuilder(
            ObjectFactoryBuilder ofb) throws IllegalStateException,
            SecurityException, NamingException {

        if (null != NamingManager.ofb) {
            // jndi.1F=ObjectFactoryBuilder cannot be reset
            throw new IllegalStateException(Messages.getString("jndi.1F")); //$NON-NLS-1$
        }

        // check security access
        SecurityManager sm = System.getSecurityManager();
        if (null != sm) {
            sm.checkSetFactory();
        }

        NamingManager.ofb = ofb;
    }

    /**
     * Create an <code>InitialContext</code> from either a previously
     * installed <code>InitialContextFactoryBuilder</code> or from the
     * <code>Context.INITIAL_CONTEXT_FACTORY</code> property in the supplied
     * <code> Hashtable h</code> if no builder is installed. An installed
     * <code>InitialContextFactoryBuilder</code> can generate a factory which
     * can be used to create the <code>InitialContext</code>. The
     * <code>Context.INITIAL_CONTEXT_FACTORY</code> property contains the
     * class of a factory which can be used to create the
     * <code>InitialContext</code>.
     * 
     * @param h
     *            a hashtable containing properties and values - may be null
     * @return an <code>InitialContext</code>
     * @throws NoInitialContextException
     *             if the <code>InitialContext</code> cannot be created.
     * @throws NamingException
     */
    public static Context getInitialContext(Hashtable<?, ?> h)
            throws NoInitialContextException, NamingException {

        // if InitialContextFactoryBuilder is set
        if (null != icfb) {
            // create InitialContext using builder
            return icfb.createInitialContextFactory(h).getInitialContext(h);
        }

        // create InitialContext using factory specified in hashtable
        try {
            // get factory class name
            String factoryClassName = (String) h
                    .get(Context.INITIAL_CONTEXT_FACTORY);
            // new factory instance
            Class<?> factoryClass = classForName(factoryClassName);
            InitialContextFactory factory = (InitialContextFactory) factoryClass
                    .newInstance();
            // create initial context instance using the factory
            return factory.getInitialContext(h);
        } catch (NamingException e) {
            // throw NamingException
            throw e;
        } catch (Exception e) {
            // failed, throw NoInitialContextException
            // jndi.20=Failed to create InitialContext using factory specified
            // in hashtable {0}
            NamingException nex = new NoInitialContextException(Messages
                    .getString("jndi.20", h)); //$NON-NLS-1$
            nex.setRootCause(e);
            throw nex;
        }
    }

    /**
     * Create an object from either a previously installed
     * <code>ObjectFactoryBuilder</code> or from a supplied reference or from
     * the <code>Context.OBJECT_FACTORIES</code> property in the supplied
     * <code>Hashtable h</code>.
     * <p>
     * An installed <code>ObjectFactoryBuilder</code> can generate a factory
     * which can be used to create the object instance to return to caller. Any
     * encountered exceptions are thrown.
     * </p>
     * <p>
     * If an <code>ObjectFactoryBuilder</code> has not been installed then the
     * supplied <code>Object o</code> may provide a <code>Reference</code>
     * or <code>Referenceable</code> object. If so, then that
     * <code>Object o</code> may have an associated class in a factory which
     * could be loaded and used to create the object instance. If the factory
     * class cannot be loaded then the <code>URLClassLoader</code> may be able
     * to load a class from the list of URLs specified in the reference's
     * factory class location. Any exceptions encountered are passed up.
     * </p>
     * <p>
     * If a reference is supplied but no factory class can be loaded from it
     * then this method returns the supplied object <code>o</code>.
     * </p>
     * <p>
     * If a factory class loads successfully and can then be used to create an
     * object instance then that instance is returned to the caller.
     * </p>
     * <p>
     * If no factory name was associated with the <code>Reference</code>
     * object <code>o</code> then see whether the <code>Reference</code> or
     * <code>Referenceable</code> object has any <code>StringRefAddrs</code>
     * of address type URL or url in its address list. For each entry in the
     * list, in the order they appear in the list, it may be possible to use the
     * URL factory to create the object. A URL in a <code>StringRefAddr</code>
     * should have a scheme which can be used to locate the associated URL
     * context factory in the same way as in the <code>getURLContext</code>
     * method. (The scheme is the part which comes before :\. For example the
     * URL http://www.apache.org has the scheme http.) A URL with no scheme
     * would be ignored for these purposes.
     * </p>
     * <p>
     * If no <code>ObjectFactoryBuilder</code> was installed, no factory class
     * name is supplied with a <code>Reference</code> and no URL contexts
     * succeeded in creating an <code>Object</code> then try the factories in
     * <code>Context.OBJECT_FACTORIES</code> for this environment. Also try
     * the provider resource file belonging to the context <code>c</code>.
     * (See <code>Context</code> description for details of Provider resource
     * files.) If any factory throws an exception then pass that back to the
     * caller - no further factories are tried.
     * </p>
     * <p>
     * If all factories fail to load or create the <code>Object</code> then
     * return the argument object <code>o</code> as the returned object.
     * </p>
     * 
     * @param o
     *            an object which may provide reference or location information.
     *            May be null.
     * @param n
     *            The name of the <code>Object</code> relative to the default
     *            initial context(or relative to the Context c if it is
     *            supplied)
     * @param c
     *            the <code>Context</code> to which the <code>Name</code> is
     *            relative
     * @param h
     *            a <code>Hashtable</code> containing environment properties
     *            and values - may be null
     * @return a new <code>Object</code> or the supplied <code>Object o</code>
     *         if one cannot be created.
     * @throws NamingException
     *             if one is encountered
     * @throws Exception
     *             if any other exception is encountered
     */
    public static Object getObjectInstance(Object o, Name n, Context c,
            Hashtable<?, ?> h) throws NamingException, Exception {

        // 1. try ObjectFactoryBuilder, if it is set
        if (null != ofb) {
            // use the builder to create an object factory
            ObjectFactory factory = ofb.createObjectFactory(o, h);
            // get object instance using the factory and return
            return factory.getObjectInstance(o, n, c, h);
        }

        // 2. see whether o is a Referenceable or a Reference
        Reference ref = null;
        if (o instanceof Referenceable) {
            ref = ((Referenceable) o).getReference();
        }
        if (o instanceof Reference) {
            ref = (Reference) o;
        }
        // if o is a Referenceable or a Reference
        if (null != ref) {
            // if a factory class name is supplied by the reference, use it to
            // create
            if (null != ref.getFactoryClassName()) {
                return getObjectInstanceByFactoryInReference(ref, o, n, c, h);
            }
            // see if ref has any StringRefAddrs of address type URL,
            Object result = getObjectInstanceByUrlRefAddr(n, c, h, ref);
            // if success, return it
            if (null != result) {
                return result;
            }
        }

        // 3. try Context.OBJECT_FACTORIES
        Object result = getObjectInstanceByObjectFactory(o, n, c, h);
        if (null != result) {
            return result;
        }

        // all failed, just return o
        return o;
    }

    private static Object getObjectInstanceByObjectFactory(Object o, Name n,
            Context c, Hashtable<?, ?> h) throws NamingException, Exception {
        // obtain object factories from hashtable and service provider resource
        // file
        String fnames[] = EnvironmentReader
                .getFactoryNamesFromEnvironmentAndProviderResource(h, c,
                        Context.OBJECT_FACTORIES);
        for (String element : fnames) {
            // new factory instance by its class name
            ObjectFactory factory = null;
            try {
                factory = (ObjectFactory) classForName(element).newInstance();
            } catch (Exception e) {
                continue;
            }
            // create object using factory
            Object obj = factory.getObjectInstance(o, n, c, h);
            if (null != obj) {
                return obj;
            }
        }
        // no object factory succeeded, return null
        return null;
    }

    private static Object getObjectInstanceByUrlRefAddr(Name n, Context c,
            Hashtable<?, ?> h, Reference ref) throws NamingException {
        // obtain pkg prefixes from hashtable and service provider resource file
        String pkgPrefixes[] = EnvironmentReader
                .getFactoryNamesFromEnvironmentAndProviderResource(h, c,
                        Context.URL_PKG_PREFIXES);
        // for each RefAddr
        Enumeration<RefAddr> enumeration = ref.getAll();
        while (enumeration.hasMoreElements()) {
            RefAddr addr = enumeration.nextElement();
            // if it is StringRefAddr and type is URL
            if (addr instanceof StringRefAddr
                    && addr.getType().equalsIgnoreCase("URL")) { //$NON-NLS-1$
                // get the url address
                String url = (String) ((StringRefAddr) addr).getContent();
                // try create using url context factory
                Object obj = getObjectInstanceByUrlContextFactory(url, n, c, h,
                        pkgPrefixes, UrlParser.getScheme(url));
                // if success, return the created obj
                if (null != obj) {
                    return obj;
                }
            }
        }
        // failed to create using any StringRefAddr of address type URL, return
        // null
        return null;
    }

    private static Object getObjectInstanceByUrlContextFactory(String url,
            Name n, Context c, Hashtable<?, ?> h, String pkgPrefixes[],
            String schema) throws NamingException {
        // if schema is empty or null, fail, return null
        if (null == schema || 0 == schema.length()) {
            return null;
        }

        for (String element : pkgPrefixes) {
            ObjectFactory factory = null;
            try {
                // create url context factory instance
                String clsName = element + "." //$NON-NLS-1$
                        + schema + "." //$NON-NLS-1$
                        + schema + "URLContextFactory"; //$NON-NLS-1$
                factory = (ObjectFactory) classForName(clsName).newInstance();
            } catch (Exception e) {
                // failed to create factory, continue trying
                continue;
            }
            try {
                // create obj using url context factory
                Object obj = factory.getObjectInstance(url, n, c, h);
                // if create success, return it
                if (null != obj) {
                    return obj;
                }
            } catch (Exception e) {
                // throw NamingException, if factory fails
                if (e instanceof NamingException) {
                    throw (NamingException) e;
                }
                // jndi.21=Failed to create object instance
                NamingException nex = new NamingException(Messages
                        .getString("jndi.21")); //$NON-NLS-1$
                nex.setRootCause(e);
                throw nex;
            }
        }
        // fail to create using url context factory, return null
        return null;
    }

    private static Object getObjectInstanceByFactoryInReference(Reference ref,
            Object o, Name n, Context c, Hashtable<?, ?> h) throws Exception {
        ObjectFactory factory = null;

        // try load the factory by its class name
        try {
            factory = (ObjectFactory) classForName(ref.getFactoryClassName())
                    .newInstance();
        } catch (ClassNotFoundException e) {
            // Ignore.
        }

        // try load the factory from its class location
        if (null == factory && null != ref.getFactoryClassLocation()) {
            factory = (ObjectFactory) loadFactoryFromLocation(ref
                    .getFactoryClassName(), ref.getFactoryClassLocation());
        }
        // if factory cannot be loaded
        if (null == factory) {
            // return o
            return o;
        }

        // get object instance using the factory and return it
        return factory.getObjectInstance(ref, n, c, h);
    }

    /*
     * If cannot load class, return null. Throws any exceptions except
     * ClassNotFoundException
     */
    private static Object loadFactoryFromLocation(String clsName,
            String location) throws Exception {

        // convert location into an array of URL, separated by ' '
        StringTokenizer st = new StringTokenizer(location, " "); //$NON-NLS-1$
        URL urls[] = new URL[st.countTokens()];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = new URL(st.nextToken());
        }

        // new a URLClassLoader from the URLs
        URLClassLoader l = new URLClassLoader(urls);

        // try load factory by URLClassLoader
        try {
            // return the new instance
            return l.loadClass(clsName).newInstance();
        } catch (ClassNotFoundException e) {
            // return null if class loading failed
            return null;
        }
    }

    /**
     * Get the state of an Object.
     * <p>
     * The <code>Context.STATE_FACTORIES</code> property from the
     * <code>Hashtable h</code> together with the
     * <code>Context.STATE_FACTORIES</code> property from the provider
     * resource file of the <code>Context c</code> provides the list of
     * factories tried to get an object's state.
     * </p>
     * <p>
     * Each factory in the list is attempted to be loaded using the context
     * class loader. Once a class is loaded then it can be used to create a new
     * instance of it to obtain the factory which can then use its
     * <code>getStateToBind</code> to find the return object. Once an object
     * is found then it is not necessary to examine further factories and the
     * object is returned it as the return parameter.
     * </p>
     * <p>
     * If no factory is loaded or all loaded factories fail to return an object
     * then return the supplied <code>Object o</code> as the return param.
     * </p>
     * <p>
     * Note for service provider implementors: Classes which implement the
     * <code>StateFactory</code> interface must be public with a public
     * constructor that has no parameters.
     * </p>
     * 
     * @param o
     *            an object which may provide reference or location information.
     *            May be null.
     * @param n
     *            the name of the <code>Object</code> relative to the default
     *            initial context (or relative to the Context c if it is
     *            supplied)
     * @param c
     *            the <code>Context</code> to which the <code>Name</code> is
     *            relative
     * @param h
     *            a <code>Hashtable</code> containing environment properties
     *            and values - may be null
     * @return the state of the specified object
     * @throws NamingException
     *             if one is encountered
     */
    public static Object getStateToBind(Object o, Name n, Context c,
            Hashtable<?, ?> h) throws NamingException {

        // obtain state factories from hashtable and service provider resource
        // file
        String fnames[] = EnvironmentReader
                .getFactoryNamesFromEnvironmentAndProviderResource(h, c,
                        Context.STATE_FACTORIES);

        for (String element : fnames) {
            // new factory instance by its class name
            StateFactory factory = null;
            try {
                factory = (StateFactory) classForName(element).newInstance();
            } catch (Exception e) {
                continue;
            }
            // try obtain state using the factory
            Object state = factory.getStateToBind(o, n, c, h);
            // if a state obtained successfully, return it
            if (null != state) {
                return state;
            }
        }

        // all factories failed, return the input argument o
        return o;
    }

    /**
     * Creates a URL <code>Context</code> which can subsequently be used to
     * resolve any URLs with the URL scheme s. A <code>URLContextFactory</code>
     * is a type of <code>ObjectFactory</code> used to create a
     * <code>URLContext</code> when <code>getObjectInstance</code> is
     * invoked with the <code>Object o</code> set to null (see the description
     * of <code>ObjectFactory</code>).
     * <p>
     * This <code>getURLContext</code> method tries to locate the
     * <code>URLContextFactory</code> based on the
     * <code>Context.URL_PKG_PREFIXES</code> property which contains the
     * prefixes to be tried as the start of the package name. (See
     * <code>Context</code>).
     * </p>
     * <p>
     * Each package prefix entry (and finally the default value) are tried to
     * find the class which can be used to create the Context.
     * </p>
     * <p>
     * A full class name is derived as
     * <code>packageprefix.s.sURLContextFactory</code> where <code>s</code>
     * is the scheme.
     * </p>
     * <p>
     * For example if a scheme is abc and the package prefix to try is com.ibm
     * then the factory class to try is
     * <code>com.ibm.abc.abcURLContextFactory</code>. Once a factory is
     * created then a <code>Context</code> is created using the special use of
     * <code>ObjectFactory.getObjectInstance</code>.
     * </p>
     * <p>
     * Once a first factory is created, it is used to create the context, and NO
     * further attempts will be made on other pkg prefixes.
     * </p>
     * 
     * @param schema
     *            the URL scheme to which the Context will relate
     * @param envmt
     *            a <code>Hashtable</code> containing environment properties
     *            and values - may be null
     * @return the URL <code>Context</code> or null if no
     *         <code>URLContextFactory</code> instance can be created and
     *         therefore a Context cannot be created.
     * @throws NamingException
     *             if one is encountered.
     */
    public static Context getURLContext(String schema, Hashtable<?, ?> envmt)
            throws NamingException {

        if (null == schema || 0 == schema.length() || null == envmt) {
            return null;
        }

        // obtain pkg prefixes from hashtable
        String pkgPrefixes[] = EnvironmentReader
                .getFactoryNamesFromEnvironmentAndProviderResource(envmt, null,
                        Context.URL_PKG_PREFIXES);

        for (String element : pkgPrefixes) {
            // create factory instance
            ObjectFactory factory;
            try {
                String clsName = element + "." //$NON-NLS-1$
                        + schema + "." //$NON-NLS-1$
                        + schema + "URLContextFactory"; //$NON-NLS-1$
                factory = (ObjectFactory) classForName(clsName).newInstance();
            } catch (Exception ex) {
                // fail to create factory, continue to try another
                continue;
            }
            try {
                // create url context using the factory, and return it
                return (Context) factory.getObjectInstance(null, null, null,
                        envmt);
            } catch (NamingException e) {
                // find NamingException, throw it
                throw e;
            } catch (Exception e) {
                // other exception, throw as NamingException
                // jndi.22=other exception happens: {0}
                NamingException nex = new NamingException(Messages.getString(
                        "jndi.22", e.toString())); //$NON-NLS-1$
                nex.setRootCause(e);
                throw nex;
            }
        }

        // cannot create context instance from any pkg prefixes, return null
        return null;
    }

    /**
     * Create the next context when using federation. All the information
     * required to do this is contained in the
     * <code>CannotProceedException</code> <code>e</code>. If the resolved
     * object is null then throw the supplied
     * <code>CannotProceedException</code> <code>e</code> using the stack
     * details from this thread. The resolved object in <code>e</code> may
     * already be a <code>Context</code>. This is the case where the service
     * provider gives an explicit pointer to the next naming system. A Context
     * object is returned as the continuation context, but need not be the same
     * object instance as the resolved object.
     * <p>
     * If the resolved object is not already a <code>Context</code> then it is
     * necessary to use the resolved object together with the
     * <code>altName</code> name, the <code>altNameCtx</code> context and
     * the environment hashtable to get an instance of the object. This should
     * then be a context which is returned as the continuation context. If an
     * instance cannot be obtained then throw the supplied
     * <code>CannotProceedException</code> using the stack details from this
     * thread.
     * </p>
     * <p>
     * This method is responsible for setting the property denoted by the
     * <code>CPE</code> string to be the supplied
     * <code>CannotProceedException</code> for the exception <code>e</code>
     * environment. The continuation context should then inherit this property.
     * </p>
     * 
     * @param cpe
     *            the <code>CannotProceedException</code> generated by the
     *            context of the previous naming system when it can proceed no
     *            further.
     * @return the next Context when using federation
     * @throws NamingException
     *             if the resolved object is null or if a context cannot be
     *             obtained from it either directly or indirectly.
     */
    @SuppressWarnings("unchecked")
    public static Context getContinuationContext(CannotProceedException cpe)
            throws NamingException {

        Context ctx = null;

        // set CPE property of the env
        if (cpe.getEnvironment() == null) {
            cpe.setEnvironment(new Hashtable<String, CannotProceedException>());
        }
        ((Hashtable<String, CannotProceedException>) cpe.getEnvironment()).put(
                CPE, cpe);

        // if resolved object is null
        if (null == cpe.getResolvedObj()) {
            // re-throw cpe
            cpe.fillInStackTrace();
            throw cpe;
        }

        // if cpe's resolved obj is Context
        if (cpe.getResolvedObj() instanceof Context) {
            // accept it as the continuation context
            ctx = (Context) cpe.getResolvedObj();
        } else {
            // otherwise, call getObjectInstance() to get a context instance
            try {
                ctx = (Context) getObjectInstance(cpe.getResolvedObj(), cpe
                        .getAltName(), cpe.getAltNameCtx(), cpe
                        .getEnvironment());
            } catch (Exception ex) {
                // throw back CPE in case of any exception
                throw cpe;
            }
            // if ctx cannot be obtained
            if (null == ctx) {
                // re-throw CPE
                cpe.fillInStackTrace();
                throw cpe;
            }
        }

        // return the continuation context
        return ctx;
    }

    private static Class<?> classForName(final String className)
            throws ClassNotFoundException {

        Class<?> cls = AccessController
                .doPrivileged(new PrivilegedAction<Class<?>>() {
                    public Class<?> run() {
                        // try thread context class loader first
                        try {
                            return Class.forName(className, true, Thread
                                    .currentThread().getContextClassLoader());
                        } catch (ClassNotFoundException e) {
                            // Ignored.
                        }
                        // try system class loader second
                        try {
                            return Class.forName(className, true, ClassLoader
                                    .getSystemClassLoader());
                        } catch (ClassNotFoundException e1) {
                            // Ignored.
                        }
                        // return null, if fail to load class
                        return null;
                    }
                });

        if (cls == null) {
            // jndi.1C=class {0} not found
            throw new ClassNotFoundException(Messages.getString(
                    "jndi.1C", className)); //$NON-NLS-1$
        }

        return cls;
    }

}
