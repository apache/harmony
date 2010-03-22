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

import java.util.HashMap;
import java.util.Hashtable;
import javax.naming.spi.NamingManager;

import org.apache.harmony.jndi.internal.UrlParser;
import org.apache.harmony.jndi.internal.EnvironmentReader;
import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * An <code>InitialContext</code> object is required as the starting context
 * for any naming operations. Other contexts and subcontexts may be created
 * later. Contexts may consist of different implementations according to the
 * needs of the application. All naming operations are performed relative to a
 * context and names are resolved beginning with the initial context.
 * <p>
 * When constructing an initial context, environment properties from a range of
 * sources may be used to initialize the environment. See the specification of
 * the {@link Context} interface for further details of environment properties.
 * </p>
 * <p>
 * The environment at runtime determines the initial context implementation. By
 * default, the naming frameworks look for the initial context factory class
 * name in the property <code>Context.INITIAL_CONTEXT_FACTORY</code>. When
 * URL strings must be resolved, a different policy is used which is described
 * below.
 * </p>
 * <p>
 * A <code>NoInitialContextException</code> is thrown when it cannot create an
 * initial context. The exception may occur not only during constructor
 * invocation, but may occur later. For example, when a subclass of <code>
 * InitialContext</code>
 * uses the lazy initialization option, <code>
 * InitialContext</code> methods
 * may be invoked later which require the initialization to be completed at that
 * time using the <code>init</code> protected method. In these circumstances,
 * <code>NoInitialContextException
 * </code> may be thrown some time after the
 * constructor was invoked. JNDI applications should be written to be
 * independent of when initial context is actually initialized.
 * </p>
 * <p>
 * If environment property <code>Context.INITIAL_CONTEXT_FACTORY</code> has a
 * non-null value, then the specified initial context factory may experience a
 * problem trying to instantiate an initial context and so throw an exception.
 * It is a responsibility of the service provider implementation as to when an
 * exception is thrown to report the problem to the JNDI application.
 * </p>
 * <p>
 * URL names comprising a String format described by RFC1738 may be components
 * of names passed to naming operations. Typically, the URL is composed of the
 * "scheme" - such as one of http, ldap, dns - followed by additional text. If
 * the JNDI can identify the URL scheme from the specified name, then it is used
 * to construct a classname suffix in the following form:<br>
 * 
 * <pre>
 *            &lt;package_prefix&gt; . &lt;scheme&gt; . &lt;scheme&gt;URLContextFactory
 * </pre>
 * 
 * Several variants of the classname are constructed using each element of the
 * <code>Context.URL_PACKAGE_PREFIXES</code> environment property. Note that
 * an additional package prefix - "com.sun.jndi.url" - is always considered to
 * be at the end of those already present in the value of that environment
 * property. Although a service provider may also provide a URL context
 * implementation as well as a context implementation, it is not required to do
 * so, and so an arbitrary service provider might not provide for creating URL
 * contexts.
 * </p>
 * <p>
 * If a URL context is successfully created for a specified URL scheme, the
 * factory can create contexts for arbitrary URLs of the same scheme.
 * <code>NamingManager.setInitialContextFactoryBuilder</code> may be used to
 * specify an alternate policy for locating factories for initial contexts and
 * URL contexts.
 * </p>
 * <p>
 * On successful completion of <code>InitialContext</code> initialization, the
 * service provider implementation will have returned an appropriate <code>
 * Context</code>
 * object which can be used for looking up and manipulating names which may or
 * may not be URL names. <code>InitialContext</code> methods other than those
 * dealing with environments should delegate context operations to that
 * <code>Context</code> object.
 * </p>
 * 
 * @see Context
 */
public class InitialContext implements Context {

    /**
     * Set to the result of the first successful invocation of <code>
     * NamingManager.getInitialContext</code>
     * by <code>getDefaultInitCtx
     * </code>. Initially null.
     */
    protected Context defaultInitCtx;

    /**
     * Set to true when <code>NamingManager.getInitialContext</code> has been
     * invoked to obtain an initial context. Initially false.
     */
    protected boolean gotDefault;

    /**
     * Contains all those JNDI environment properties that were found in any of
     * the the sources of JNDI environment properties. Initially null.
     */
    protected Hashtable<Object, Object> myProps;

    /**
     * Contains loaded properties for each classloader
     */
    private static Hashtable<ClassLoader, Hashtable<Object, Object>> propsCache = new Hashtable<ClassLoader, Hashtable<Object, Object>>();
    
    /**
     * Contians properties load from java.home/lib/jndi.properties
     */
    private static Hashtable<Object, Object> libProperties = null;

    /**
     * Constructs an <code>InitialContext</code> instance without using any
     * environment properties. This constructor is effectively the same as using
     * constructor <code>InitialContext((Hashtable)null)</code>.
     * 
     * @throws NamingException
     *             If failed to create an <code>InitialContext</code>.
     */
    public InitialContext() throws NamingException {
        this(null);
    }

    /**
     * Constructs an <code>InitialContext</code> instance using environment
     * properties in the supplied parameter which may be null.
     * 
     * @param environment
     *            the JNDI environment properties used to create the context
     * @throws NamingException
     *             If failed to create an <code>InitialContext</code>.
     */
    public InitialContext(Hashtable<?, ?> environment) throws NamingException {
        internalInit(environment);
    }

    /**
     * Constructs an <code>InitialContext</code> instance by indicating
     * whether a lazy initialization is desired. Effectively, this is the same
     * as using constructor <code>InitialContext()
     * </code> if lazy
     * initialization is not indicated.
     * <p>
     * This constructor may be invoked with a parameter value of true and the
     * implementation will defer initialization of the instance. This may be
     * used in an <code>InitialContext</code> subclass constructor in which
     * later action will set up a <code>Hashtable</code> object with
     * appropriate environment properties and pass that to the <code>init</code>
     * method to complete initialization of the <code>InitialContext</code>
     * object.
     * </p>
     * 
     * @param doNotInit
     *            Specifies whether to initialize the new instance.
     * @throws NamingException
     *             If failed to create an <code>InitialContext</code>.
     */
    protected InitialContext(boolean doNotInit) throws NamingException {
        if (!doNotInit) {
            internalInit(null);
        }
    }

    /**
     * Does private initialization.
     * 
     * @param env
     *            the JNDI environment properties used to create the context
     * @throws NamingException
     *             If failed to create an InitialContext.
     */
    @SuppressWarnings("unchecked")
    private void internalInit(Hashtable<?, ?> env) throws NamingException {

        // 1. Read the environment parameter used to create this Context
        if (null == env) {
            myProps = new Hashtable<Object, Object>();
        } else {
            myProps = (Hashtable<Object, Object>) env.clone();
        }

        // 2. Read Applet parameters
        EnvironmentReader.readAppletParameters(myProps.get(Context.APPLET),
                myProps);

        // 3. Read System properties
        EnvironmentReader.readSystemProperties(myProps);

        // 4.1 Read application/applet resource files
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (propsCache.containsKey(cl)) {
            EnvironmentReader.mergeEnvironment(propsCache.get(cl), myProps,
                    true);
        } else {
            Hashtable<Object, Object> appProps = new Hashtable<Object, Object>();
            EnvironmentReader.readApplicationResourceFiles(appProps);
            propsCache.put(cl, appProps);
            EnvironmentReader.mergeEnvironment(appProps, myProps, true);
        }
        
        // 4.2 Read "java.home"/lib/jndi.properties
        if (libProperties == null) {
            Hashtable<Object, Object> props = new Hashtable<Object, Object>();
            EnvironmentReader.readLibraryResourceFile(props);
            libProperties = props;
        }
        
        EnvironmentReader.mergeEnvironment(libProperties, myProps, true);
        
        
        // 5. No need to read service provider resource files

        // if JNDI standard property "java.naming.factory.initial" has a
        // non-null value
        if (myProps.containsKey(INITIAL_CONTEXT_FACTORY)) {
            // call getDefaultInitCtx() to initialize gotDefault and
            // defaultInitCtx
            getDefaultInitCtx();
        }

    }

    /**
     * Uses the specified environment parameter together with other JNDI
     * properties to initialize this <code>InitialContext</code> object. The
     * <code>myProps</code> field will be filled with found JNDI properties.
     * If JNDI standard property "java.naming.factory.initial" has a non-null
     * value, then <code>getDefaultInitCtx</code> is invoked to try to
     * initialize fields <code>gotDefault</code> and
     * <code>defaultInitCtx</code> of the <code>InitialContext</code>
     * object.
     * 
     * @param env
     *            the JNDI environment properties supplied to create the context
     * @throws NamingException
     *             If naming problems are encountered during initialization of
     *             these fields.
     */
    protected void init(Hashtable<?, ?> env) throws NamingException {
        this.internalInit(env);
    }

    /*
     * Initializes the default initial context.
     * 
     * @throws NamingException If failed to initialize this InitialContext.
     */
    private void initializeDefaultInitCtx() throws NamingException {
        if (!this.gotDefault) {
            this.defaultInitCtx = NamingManager.getInitialContext(myProps);
            if (null == this.defaultInitCtx) {
                throw new NoInitialContextException(
                        "Failed to create an initial context."); //$NON-NLS-1$
            }
            this.gotDefault = true;
        }
    }

    /**
     * Gets the default underlying <code>Context</code> implementation. If
     * <code>gotDefault</code> is true, returns the value of <code>
     * defaultInitCtx</code>.
     * Otherwise, calls <code>NamingManager.getInitialContext
     * </code> to return
     * an initial context for the current environment into
     * <code>defaultInitCtx</code>, then <code>gotDefault</code> is set
     * true. If the resulting context object is null, a
     * <code>NoInitialContextException
     * </code> is thrown, otherwise the value
     * of <code>defaultInitCtx</code> is returned.
     * 
     * @return the default context
     * @throws NoInitialContextException
     *             If <code>NamingManager.getInitialContext</code> returns
     *             null.
     * @throws NamingException
     *             If failed to create the default context.
     */
    protected Context getDefaultInitCtx() throws NamingException {
        initializeDefaultInitCtx();
        return this.defaultInitCtx;
    }

    /**
     * Returns a non-null context for the specified name of Name representation.
     * <p>
     * If an initial context factory builder has been defined, then the
     * specified <code>Name</code> parameter is ignored and the result of
     * <code>
     * getDefaultInitCtx</code> is returned. Otherwise, if the first
     * component of the name is not a URL string, then it returns the result of
     * invoking <code>getDefaultInitCtx</code>. Otherwise, it attempts to
     * return a URL context
     * {@link javax.naming.spi.NamingManager#getURLContext(String, Hashtable)},
     * but if unsuccessful, returns the result of invoking
     * <code>getDefaultInitCtx</code>.
     * </p>
     * 
     * @param name
     *            a name used in a naming operation which may not be null
     * @return a context which may be a URL context
     * @throws NamingException
     *             If failed to get the desired context.
     */
    protected Context getURLOrDefaultInitCtx(Name name) throws NamingException {
        // If the name has components
        if (0 < name.size()) {
            return getURLOrDefaultInitCtx(name.get(0));
        }
        return getDefaultInitCtx();
    }

    /**
     * Returns a non-null context for the specified name of string
     * representation.
     * <p>
     * If an initial context factory builder has been defined, then the
     * specified name parameter is ignored and the result of <code>
     * getDefaultInitCtx</code>
     * is returned. Otherwise, if the name is not a URL string, then it returns
     * the result of invoking <code>getDefaultInitCtx
     * </code>. Otherwise, it
     * attempts to return a URL context
     * {@link javax.naming.spi.NamingManager#getURLContext(String, Hashtable)},
     * but if unsuccessful, returns the result of invoking <code>
     * getDefaultInitCtx</code>.
     * </p>
     * 
     * @param name
     *            a name used in a naming operation which may not be null
     * @return a context which may be a URL context
     * @throws NamingException
     *             If failed to get the desired context.
     */
    protected Context getURLOrDefaultInitCtx(String name)
            throws NamingException {

        /*
         * If an initial context factory builder has been defined, then the
         * specified name parameter is ignored and the result of
         * getDefaultInitCtx() is returned.
         */
        if (NamingManager.hasInitialContextFactoryBuilder()) {
            return getDefaultInitCtx();
        }

        if (null == name) {
            // jndi.00=name must not be null
            throw new NullPointerException(Messages.getString("jndi.00")); //$NON-NLS-1$
        }

        // If the name has components
        String scheme = UrlParser.getScheme(name);
        Context ctx = null;
        if (null != scheme) {
            synchronized (contextCache) {
                if (contextCache.containsKey(scheme)) {
                    return contextCache.get(scheme);
                }

                // So the first component is a valid URL
                ctx = NamingManager.getURLContext(scheme, myProps);
                if (null == ctx) {
                    ctx = getDefaultInitCtx();
                }
                contextCache.put(scheme, ctx);
            }
            return ctx;
        }
        return getDefaultInitCtx();
    }

    public Object lookup(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).lookup(name);
    }

    public Object lookup(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).lookup(name);
    }

    public void bind(Name name, Object obj) throws NamingException {
        getURLOrDefaultInitCtx(name).bind(name, obj);
    }

    public void bind(String name, Object obj) throws NamingException {
        getURLOrDefaultInitCtx(name).bind(name, obj);
    }

    public void rebind(Name name, Object obj) throws NamingException {
        getURLOrDefaultInitCtx(name).rebind(name, obj);
    }

    public void rebind(String name, Object obj) throws NamingException {
        getURLOrDefaultInitCtx(name).rebind(name, obj);
    }

    public void unbind(Name name) throws NamingException {
        getURLOrDefaultInitCtx(name).unbind(name);
    }

    public void unbind(String name) throws NamingException {
        getURLOrDefaultInitCtx(name).unbind(name);
    }

    public void rename(Name oldName, Name newName) throws NamingException {
        getURLOrDefaultInitCtx(oldName).rename(oldName, newName);
    }

    public void rename(String oldName, String newName) throws NamingException {
        getURLOrDefaultInitCtx(oldName).rename(oldName, newName);
    }

    public NamingEnumeration<NameClassPair> list(Name name)
            throws NamingException {
        return getURLOrDefaultInitCtx(name).list(name);
    }

    public NamingEnumeration<NameClassPair> list(String name)
            throws NamingException {
        return getURLOrDefaultInitCtx(name).list(name);
    }

    public NamingEnumeration<Binding> listBindings(Name name)
            throws NamingException {
        return getURLOrDefaultInitCtx(name).listBindings(name);
    }

    public NamingEnumeration<Binding> listBindings(String name)
            throws NamingException {
        return getURLOrDefaultInitCtx(name).listBindings(name);
    }

    public void destroySubcontext(Name name) throws NamingException {
        getURLOrDefaultInitCtx(name).destroySubcontext(name);
    }

    public void destroySubcontext(String name) throws NamingException {
        getURLOrDefaultInitCtx(name).destroySubcontext(name);
    }

    public Context createSubcontext(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).createSubcontext(name);
    }

    public Context createSubcontext(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).createSubcontext(name);
    }

    public Object lookupLink(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).lookupLink(name);
    }

    public Object lookupLink(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).lookupLink(name);
    }

    public NameParser getNameParser(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).getNameParser(name);
    }

    public NameParser getNameParser(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).getNameParser(name);
    }

    /**
     * Combines two names into a composite name according to the syntax for this
     * context. The name <code>prefix</code> is expected to be the name of one
     * or more of the immediate parent contexts of this context, so should be an
     * empty name for an <code>InitialContext</code>. <code>name</code> is
     * a name relative to this context. Neither <code>prefix</code> nor
     * <code>name</code> may be null.
     * 
     * @param name
     *            a <code>Name</code>, may not be null
     * @param prefix
     *            a <code>Name</code> serves as prefix, may not be null
     * @return the combined name
     * @throws NamingException
     *             if an error occurs.
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        if (null == name) {
            throw new NullPointerException();
        }
        return (Name) name.clone();
    }

    /**
     * Combines two names into a composite name according to the syntax for this
     * context. The name <code>prefix</code> is expected to be the name of one
     * or more of the immediate parent contexts of this context, so should be an
     * empty string for an <code>InitialContext</code>. <code>name</code>
     * is a name relative to this context.
     * 
     * @param name
     *            a <code>Name</code>, may not be null
     * @param prefix
     *            a <code>Name</code> serves as prefix, may not be null
     * @return the combined name
     * @throws NamingException
     *             if an error occurs.
     */
    public String composeName(String name, String prefix)
            throws NamingException {
        return name;
    }

    public Object addToEnvironment(String propName, Object propVal)
            throws NamingException {
        synchronized (contextCache) {
            myProps.put(propName, propVal);
            contextCache.clear();
        }
        return getDefaultInitCtx().addToEnvironment(propName, propVal);
    }

    public Object removeFromEnvironment(String propName) throws NamingException {
        synchronized (contextCache) {
            myProps.remove(propName);
            contextCache.clear();
        }
        return getDefaultInitCtx().removeFromEnvironment(propName);
    }

    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return getDefaultInitCtx().getEnvironment();
    }

    public void close() throws NamingException {
        if (this.gotDefault) {
            getDefaultInitCtx().close();
        }
    }

    public String getNameInNamespace() throws NamingException {
        return getDefaultInitCtx().getNameInNamespace();
    }

    private HashMap<String, Context> contextCache = new HashMap<String, Context>();
}
