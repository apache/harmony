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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author  Vasily Zakharov
 */

package org.apache.harmony.jndi.provider.rmi.registry;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.MarshalException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.StubNotFoundException;
import java.rmi.UnknownHostException;
import java.rmi.UnmarshalException;
import java.rmi.activation.ActivateFailedException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.RMIClientSocketFactory;
import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.CommunicationException;
import javax.naming.CompositeName;
import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.ServiceUnavailableException;
import javax.naming.StringRefAddr;
import javax.naming.spi.NamingManager;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * RMI Registry context implementation.
 */
public class RegistryContext implements Context, Referenceable {

    /**
     * System property used to state whether the RMI security manager should be
     * installed.
     */
    public static final String SECURITY_MANAGER = "java.naming.rmi.security.manager"; //$NON-NLS-1$

    /**
     * System property used to supply the name of the
     * {@link RMIClientSocketFactory} to use.
     */
    public static final String CLIENT_SOCKET_FACTORY = "org.apache.harmony.jndi.provider.rmi.registry.clientSocketFactory"; //$NON-NLS-1$

    /**
     * Prefix for RMI URLs.
     */
    public static final String RMI_URL_PREFIX = "rmi:"; //$NON-NLS-1$

    /**
     * Address type for RMI context references.
     */
    public static final String ADDRESS_TYPE = "URL"; //$NON-NLS-1$

    /**
     * Name parser.
     */
    protected static final NameParser nameParser = new AtomicNameParser();

    /**
     * Registry host, stored to produce copies of this context.
     */
    protected String host;

    /**
     * Registry port, stored to produce copies of this context.
     */
    protected int port;

    /**
     * RMI client socket factory, stored to produce copies of this context.
     */
    protected RMIClientSocketFactory csf;

    /**
     * Local environment.
     */
    protected Hashtable<Object, Object> environment;

    /**
     * RMI Registry.
     */
    protected Registry registry;

    /**
     * Reference for this context, initialized by
     * {@link RegistryContextFactory#getObjectInstance(Object, Name, Context, Hashtable)}.
     */
    protected Reference reference;

    /**
     * Creates RMI registry context bound to RMI Registry operating on the
     * specified host and port.
     * 
     * @param host
     *            Host. If <code>null</code>, localhost is assumed.
     * 
     * @param port
     *            Port. If <code>0</code>, default registry port (<code>1099</code>)
     *            is assumed.
     * 
     * @param environment
     *            Context environment, may be <code>null</code> which denotes
     *            empty environment.
     * 
     * @throws NamingException
     *             If some naming error occurs.
     */
    @SuppressWarnings("unchecked")
    public RegistryContext(String host, int port, Hashtable<?, ?> environment)
            throws NamingException {
        this.host = host;
        this.port = port;

        this.environment = ((environment != null) ? (Hashtable<Object, Object>) environment
                .clone()
                : new Hashtable<Object, Object>());

        if (this.environment.get(SECURITY_MANAGER) != null) {
            installSecurityManager();
        }

        String clientSocketFactoryName = (String) this.environment
                .get(CLIENT_SOCKET_FACTORY);

        if (clientSocketFactoryName == null) {
            csf = null;
        } else {
            try {
                csf = (RMIClientSocketFactory) Class.forName(
                        clientSocketFactoryName, true,
                        Thread.currentThread().getContextClassLoader())
                        .newInstance();
            } catch (ClassNotFoundException e) {
                // jndi.79=RMI Client Socket Factory cannot be instantiated
                throw (ConfigurationException) new ConfigurationException(
                        Messages.getString("jndi.79")) //$NON-NLS-1$
                        .initCause(e);
            } catch (InstantiationException e) {
                // jndi.79=RMI Client Socket Factory cannot be instantiated
                throw (ConfigurationException) new ConfigurationException(
                        Messages.getString("jndi.79")) //$NON-NLS-1$
                        .initCause(e);
            } catch (IllegalAccessException e) {
                // jndi.79=RMI Client Socket Factory cannot be instantiated
                throw (NoPermissionException) new NoPermissionException(
                        Messages.getString("jndi.79")) //$NON-NLS-1$
                        .initCause(e);
            }
        }
        registry = getRegistry(host, port, csf);
        reference = null;
    }

    /**
     * Creates RMI registry context by copying the specified context.
     * 
     * @param context
     *            Context to copy.
     */
    @SuppressWarnings("unchecked")
    protected RegistryContext(RegistryContext context) {
        host = context.host;
        port = context.port;
        csf = context.csf;
        environment = (Hashtable<Object, Object>) context.environment.clone();
        registry = context.registry;
        reference = context.reference;
    }

    /**
     * {@inheritDoc}
     */
    public Object lookup(Name name) throws NamingException {
        if (name.isEmpty()) {
            return cloneContext();
        }
        String stringName = getMyComponents(name);

        try {
            return getObjectInstance(stringName, registry.lookup(stringName));
        } catch (NotBoundException e) {
            // jndi.7A=Name is not bound: {0}
            throw (NameNotFoundException) new NameNotFoundException(Messages
                    .getString("jndi.7A", stringName)).initCause(e); //$NON-NLS-1$
        } catch (RemoteException e) {
            throw (NamingException) newNamingException(e).fillInStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object lookup(String name) throws NamingException {
        return lookup(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public Object lookupLink(Name name) throws NamingException {
        return lookup(name);
    }

    /**
     * {@inheritDoc}
     */
    public Object lookupLink(String name) throws NamingException {
        return lookupLink(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public void bind(Name name, Object obj) throws NamingException {
        if (name.isEmpty()) {
            // jndi.7B=Cannot bind empty name
            throw new InvalidNameException(Messages.getString("jndi.7B")); //$NON-NLS-1$
        }
        String stringName = getMyComponents(name);

        try {
            registry.bind(stringName, getStateToBind(stringName, obj));
        } catch (AlreadyBoundException e) {
            // jndi.7C=Name is already bound: {0}
            throw (NameAlreadyBoundException) new NameAlreadyBoundException(
                    Messages.getString("jndi.7C", stringName)).initCause(e); //$NON-NLS-1$
        } catch (RemoteException e) {
            throw (NamingException) newNamingException(e).fillInStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void bind(String name, Object obj) throws NamingException {
        bind(new CompositeName(name), obj);
    }

    /**
     * {@inheritDoc}
     */
    public void rebind(Name name, Object obj) throws NamingException {
        if (name.isEmpty()) {
            // jndi.7D=Cannot rebind empty name
            throw new InvalidNameException(Messages.getString("jndi.7D")); //$NON-NLS-1$
        }
        String stringName = getMyComponents(name);

        try {
            registry.rebind(stringName, getStateToBind(stringName, obj));
        } catch (RemoteException e) {
            throw (NamingException) newNamingException(e).fillInStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rebind(String name, Object obj) throws NamingException {
        rebind(new CompositeName(name), obj);
    }

    /**
     * {@inheritDoc}
     */
    public void unbind(Name name) throws NamingException {
        if (name.isEmpty()) {
            // jndi.7E=Cannot unbind empty name
            throw new InvalidNameException(Messages.getString("jndi.7E")); //$NON-NLS-1$
        }
        String stringName = getMyComponents(name);

        try {
            registry.unbind(stringName);
        } catch (NotBoundException e) {
            // Returning ok if target name is not found, by the specification.
        } catch (RemoteException e) {
            throw (NamingException) newNamingException(e).fillInStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unbind(String name) throws NamingException {
        unbind(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public Context createSubcontext(Name name)
            throws OperationNotSupportedException {
        // jndi.7F=RMI Registry is a flat context and doesn't support
        // subcontexts
        throw new OperationNotSupportedException(Messages.getString("jndi.7F")); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public void destroySubcontext(Name name)
            throws OperationNotSupportedException {
        // jndi.7F=RMI Registry is a flat context and doesn't support
        // subcontexts
        throw new OperationNotSupportedException(Messages.getString("jndi.7F")); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    public void destroySubcontext(String name) throws NamingException {
        destroySubcontext(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public void rename(Name oldName, Name newName) throws NamingException {
        bind(newName, lookup(oldName));
        unbind(oldName);
    }

    /**
     * {@inheritDoc}
     */
    public void rename(String oldName, String newName) throws NamingException {
        rename(new CompositeName(oldName), new CompositeName(newName));
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration<NameClassPair> list(Name name)
            throws NamingException {
        if (name.isEmpty()) {
            try {
                return new NameClassPairEnumeration(registry.list());
            } catch (RemoteException e) {
                throw (NamingException) newNamingException(e)
                        .fillInStackTrace();
            }
        }
        Object obj = lookup(name);

        if (obj instanceof Context) {
            try {
                return ((Context) obj).list(""); //$NON-NLS-1$
            } finally {
                ((Context) obj).close();
            }
        }
        // jndi.80=Name specifies an object that is not a context: {0}
        throw new NotContextException(Messages.getString("jndi.80", name)); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration<NameClassPair> list(String name)
            throws NamingException {
        return list(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration<Binding> listBindings(Name name)
            throws NamingException {
        if (name.isEmpty()) {
            try {
                return new BindingEnumeration(registry.list(), this);
            } catch (RemoteException e) {
                throw (NamingException) newNamingException(e)
                        .fillInStackTrace();
            }
        }
        Object obj = lookup(name);

        if (obj instanceof Context) {
            try {
                return ((Context) obj).listBindings(""); //$NON-NLS-1$
            } finally {
                ((Context) obj).close();
            }
        }
        // jndi.80=Name specifies an object that is not a context: {0}
        throw new NotContextException(Messages.getString("jndi.80", name)); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration<Binding> listBindings(String name)
            throws NamingException {
        return listBindings(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public NameParser getNameParser(Name name) {
        return nameParser;
    }

    /**
     * {@inheritDoc}
     */
    public NameParser getNameParser(String name) throws NamingException {
        return getNameParser(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        return ((Name) prefix.clone()).addAll(name);
    }

    /**
     * {@inheritDoc}
     */
    public String composeName(String name, String prefix)
            throws NamingException {
        return composeName(new CompositeName(name), new CompositeName(prefix))
                .toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getNameInNamespace() {
        return ""; //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    public Hashtable<?, ?> getEnvironment() {
        return (Hashtable<?, ?>) environment.clone();
    }

    /**
     * {@inheritDoc}
     */
    public Object addToEnvironment(String propName, Object propVal)
            throws NoPermissionException {
        if (propName.equals(SECURITY_MANAGER)) {
            installSecurityManager();
        }
        return environment.put(propName, propVal);
    }

    /**
     * {@inheritDoc}
     */
    public Object removeFromEnvironment(String propName) {
        return environment.remove(propName);
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        environment = null;
        registry = null;
    }

    /**
     * {@inheritDoc}
     */
    public Reference getReference() throws NamingException {
        if (reference == null) {
            if ((host == null) || (host.equals("localhost"))) { //$NON-NLS-1$
                // jndi.81=Cannot create reference for RMI registry that is
                // being accessed using localhost
                throw new ConfigurationException(Messages.getString("jndi.81")); //$NON-NLS-1$
            }
            reference = new Reference(RegistryContext.class.getName(),
                    new StringRefAddr(ADDRESS_TYPE, RMI_URL_PREFIX + "//" //$NON-NLS-1$
                            + host + ((port > 0) ? (":" + port) : "")), //$NON-NLS-1$ //$NON-NLS-2$
                    RegistryContextFactory.class.getName(), null);
        }
        return (Reference) reference.clone();
    }

    /**
     * Initializes reference for this context instance. Called by
     * {@link RegistryContextFactory#getObjectInstance(Object, Name, Context, Hashtable)}.
     * 
     * @param reference
     *            Reference for this context instance.
     */
    protected void setReference(Reference reference) {
        this.reference = reference;
    }

    /**
     * Returns a clone of this context.
     * 
     * @return Clone of this context.
     */
    protected RegistryContext cloneContext() {
        return new RegistryContext(this);
    }

    /**
     * Verifies that the specified name is valid for this context and returns
     * string representation of that name for this provider.
     * 
     * Returns returns first component of a {@link CompositeName} or a string
     * representation of a name otherwise.
     * 
     * @param name
     *            Name to verify.
     * 
     * @return {@link CompositeName#get(int) CompositeName#get(0)} if
     *         <code>name</code> is a {@link CompositeName},
     *         {@link Object#toString() name.toString()} otherwise.
     */
    protected String getMyComponents(Name name) {
        if (name instanceof CompositeName) {
            return name.get(0);
        }
        return name.toString();
    }

    /**
     * Prepares object for binding. It calls
     * {@link NamingManager#getStateToBind(Object, Name, Context, Hashtable)}
     * and makes the resulting object {@link Remote} by wrapping it into
     * {@link RemoteReferenceWrapper}.
     * 
     * @param name
     *            Object name.
     * 
     * @param obj
     *            Object to prepare for binding.
     * 
     * @return Object ready for binding.
     * 
     * @throws NamingException
     *             If some naming error occurs.
     * 
     * @throws RemoteException
     *             If remote exception occurs.
     */
    protected Remote getStateToBind(String name, Object obj)
            throws NamingException, RemoteException {
        obj = NamingManager.getStateToBind(obj, new CompositeName().add(name),
                this, environment);

        if (obj instanceof Remote) {
            return (Remote) obj;
        }

        if (obj instanceof Reference) {
            return new RemoteReferenceWrapper((Reference) obj);
        }

        if (obj instanceof Referenceable) {
            return new RemoteReferenceWrapper(((Referenceable) obj)
                    .getReference());
        }
        // jndi.82=Cannot bind to RMI Registry object that is neither Remote nor
        // Reference nor Referenceable
        throw new IllegalArgumentException(Messages.getString("jndi.82")); //$NON-NLS-1$
    }

    /**
     * Processes object returned from {@linkplain Registry RMI registry}. It
     * unwraps {@link RemoteReference} if necessary and calls
     * {@link NamingManager#getObjectInstance(Object, Name, Context, Hashtable)}.
     * 
     * @param name
     *            Object name.
     * 
     * @param remote
     *            Returned object.
     * 
     * @return Processed object.
     * 
     * @throws NamingException
     *             If some naming error occurs.
     * 
     * @throws RemoteException
     *             If remote exception occurs.
     */
    protected Object getObjectInstance(String name, Remote remote)
            throws NamingException, RemoteException {
        Object obj;

        obj = ((remote instanceof RemoteReference) ? ((RemoteReference) remote)
                .getReference() : (Object) remote);

        try {
            return NamingManager.getObjectInstance(obj, new CompositeName()
                    .add(name), this, environment);
        } catch (NamingException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            // jndi.83=NamingManager.getObjectInstance() failed
            throw (NamingException) new NamingException(Messages
                    .getString("jndi.83")).initCause(e); //$NON-NLS-1$
        }
    }

    /**
     * Prepares a new {@link NamingException} wrapping the specified
     * {@link RemoteException} source exception. Source exception becomes a
     * {@linkplain NamingException#getCause() cause} of the generated exception.
     * 
     * The particular subclass of {@link NamingException} returned depends on
     * the particular subclass of source {@link RemoteException}.
     * 
     * If source exception is not of a specific class or is not a
     * {@link RemoteException} or is <code>null</code>, then plain
     * {@link NamingException} is returned.
     * 
     * Note: {@link Throwable#fillInStackTrace()} should be called before
     * throwing the generated exception, to provide the proper (not including
     * this method) stack trace for the exception.
     * 
     * Example of use:
     * 
     * <code>try {
     *     ...
     * } catch (RemoteException e) {
     *     throw (NamingException) newNamingException(e).fillInStackTrace();
     * }</code>
     * 
     * @param e
     *            Source {@link RemoteException}.
     * 
     * @return Generated {@link NamingException} exception.
     */
    @SuppressWarnings("deprecation")
    protected NamingException newNamingException(Throwable e) {
        NamingException ret = (e instanceof AccessException) ? new NoPermissionException()
                : (e instanceof ConnectException) ? new ServiceUnavailableException()
                        : (e instanceof ConnectIOException)
                                || (e instanceof ExportException)
                                || (e instanceof MarshalException)
                                || (e instanceof UnmarshalException) ? new CommunicationException()
                                : (e instanceof ActivateFailedException)
                                        || (e instanceof NoSuchObjectException)
                                        || (e instanceof java.rmi.server.SkeletonMismatchException)
                                        || (e instanceof java.rmi.server.SkeletonNotFoundException)
                                        || (e instanceof StubNotFoundException)
                                        || (e instanceof UnknownHostException) ? new ConfigurationException()
                                        : (e instanceof ServerException) ? newNamingException(e
                                                .getCause())
                                                : new NamingException();

        if (ret.getCause() == null) {
            ret.initCause(e);
        }
        return ret;
    }

    /**
     * Installs {@link RMISecurityManager} if it is not already installed.
     * 
     * @throws NoPermissionException
     *             If security manager other than {@link RMISecurityManager} is
     *             installed and prohibits installing a new security manager.
     */
    protected void installSecurityManager() throws NoPermissionException {
        if (!(System.getSecurityManager() instanceof RMISecurityManager)) {
            try {
                System.setSecurityManager(new RMISecurityManager());
            } catch (SecurityException e) {
                // jndi.84=Cannot install RMISecurityManager
                throw (NoPermissionException) new NoPermissionException(
                        Messages.getString("jndi.84")).initCause(e); //$NON-NLS-1$
            }
        }
    }

    /**
     * Creates reference to the {@linkplain Registry RMI registry} located on
     * the specified host and port.
     * 
     * @param host
     *            Host. If <code>null</code>, localhost is assumed. May not
     *            be <code>null</code> if <code>csf</code> is not
     *            <code>null</code>.
     * 
     * @param port
     *            Port. If <code>0</code>, default registry port (<code>1099</code>)
     *            is assumed. May not be <code>0</code> if <code>csf</code>
     *            is not <code>null</code>.
     * 
     * @param csf
     *            RMIClientSocketFactory that is used to create socket
     *            connections to the registry. If <code>null</code>, default
     *            socket factory is used. See
     *            {@link LocateRegistry#getRegistry(String, int, RMIClientSocketFactory)}.
     * 
     * @return Registry reference.
     * 
     * @throws NamingException
     *             If getting registry failed.
     */
    protected Registry getRegistry(String host, int port,
            RMIClientSocketFactory csf) throws NamingException {
        try {
            return ((csf != null) ? LocateRegistry.getRegistry(host, port, csf)
                    : ((host != null) ? ((port != 0) ? LocateRegistry
                            .getRegistry(host, port) : LocateRegistry
                            .getRegistry(host)) : ((port != 0) ? LocateRegistry
                            .getRegistry(port) : LocateRegistry.getRegistry())));
        } catch (RemoteException e) {
            throw (NamingException) new NamingException().initCause(e);
        }
    }

}
