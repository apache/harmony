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
 * @author  Mikhail A. Markov
 */
package org.apache.harmony.rmi.remoteref;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.BindException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.ServerError;
import java.rmi.ServerException;
import java.rmi.StubNotFoundException;
import java.rmi.UnmarshalException;
import java.rmi.server.ExportException;
import java.rmi.server.ObjID;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RemoteCall;
import java.rmi.server.RemoteObjectInvocationHandler;
import java.rmi.server.RemoteRef;
import java.rmi.server.RemoteStub;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.ServerRef;
import java.rmi.server.Skeleton;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import org.apache.harmony.rmi.common.GetBooleanPropAction;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.common.RMIProperties;
import org.apache.harmony.rmi.common.RMIUtil;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.server.RMIReference;
import org.apache.harmony.rmi.server.ServerConnectionManager;
import org.apache.harmony.rmi.transport.Endpoint;
import org.apache.harmony.rmi.transport.RMIObjectInputStream;
import org.apache.harmony.rmi.transport.RMIObjectOutputStream;


/**
 * Implementation of server-side handle for remote objects.
 *
 * @author  Mikhail A. Markov
 */
public class UnicastServerRef extends UnicastRef implements ServerRef {

    private static final long serialVersionUID = 3913676048828136951L;

    /** Implementation which this handle refers to. */
    protected RMIReference ref = null;

    /**
     * Skeleton for this remote object if we use RMI protocol 1.1 or null
     * if we use RMI protocol 1.2.
     */
    protected Skeleton skel = null;

    /**
     * Map with remote methods.
     * Methods hash codes are the keys in the table.
     */
    protected Map remoteMethods = new HashMap();

    /** True if the handled remote object is system. */
    protected boolean isSystem;

    /**
     * ServerConnectionManager accepting connections for this ServerRef.
     */
    protected ServerConnectionManager mgr;

    // The name of Remote class for login purposes.
    private String implClassName = null;

    // Log where to write server-side log of remote calls.
    private static final RMILog serverCallsLog = RMILog.getServerCallsLog();

    // Log where to write server-side log of remote reference activity
    private static final RMILog serverRefLog = RMILog.getServerRefLog();

    // Should we suppress stack traces before sending to client or not
    private static final boolean suppressST =
        ((Boolean) AccessController.doPrivileged(
            new GetBooleanPropAction(RMIProperties.SUPPRESSSTACKTRACES_PROP)))
                .booleanValue();

    // Should we print on server side stack traces or not
    private static final boolean printST =
        ((Boolean) AccessController.doPrivileged(
                new GetBooleanPropAction(RMIProperties.EXCEPTIONTRACE_PROP)))
                    .booleanValue();

    // Should we generate dynamic proxy stubs only or not.
    private static final boolean ignoreStubClasses =
        ((Boolean) AccessController.doPrivileged(
                new GetBooleanPropAction(RMIProperties.IGNORESTUBCLASSES_PROP)))
                    .booleanValue();

    /**
     * Constructs default UnicastServerRef listening on anonymous port.
     */
    public UnicastServerRef() {
        this(0);
    }

    /**
     * Constructs UnicastServerRef listening on the port specified.
     *
     * @param port port where this UnicastServerRef will listen for connections
     */
    public UnicastServerRef(int port) {
        this(port, null, null, new ObjID());
    }

    /**
     * Constructs UnicastServerRef listening on the port specified and
     * having the given client and server socket factories.
     *
     * @param port port where this UnicastServerRef will listen for connections
     * @param csf client-side socket factory for creating client sockets
     * @param ssf server-side socket factory for creating server sockets
     */
    public UnicastServerRef(int port,
                            RMIClientSocketFactory csf,
                            RMIServerSocketFactory ssf) {
        this(port, csf, ssf, new ObjID());
    }

    /**
     * Constructs UnicastServerRef listening on the port specified,
     * using specified client and server socket factories and
     * having the given ObjID.
     *
     * @param port port where this UnicastServerRef will listen for connections
     * @param csf client-side socket factory for creating client sockets
     * @param ssf server-side socket factory for creating server sockets
     * @param objId Object ID of remote object
     */
    public UnicastServerRef(int port,
                            RMIClientSocketFactory csf,
                            RMIServerSocketFactory ssf,
                            ObjID objId) {
        super();
        isLocal = true;
        ep = new Endpoint(port, csf, ssf);
        this.objId = objId;
    }

    /**
     * Constructs UnicastServerRef using specified Endpoint and ObjID.
     *
     * @param ep Endpoint for remote calls
     * @param objId Object ID of remote object
     */
    public UnicastServerRef(Endpoint ep, ObjID objId) {
        super(ep, objId);
    }

    /**
     * @see ServerRef.exportObject(Remote, Object)
     */
    public RemoteStub exportObject(Remote obj, Object data)
            throws RemoteException {
        return (RemoteStub) exportObject(obj, data, false, true, false);
    }

    /**
     * @see ServerRef.getClientHost()
     */
    public String getClientHost() throws ServerNotActiveException {
        String host = ServerConnectionManager.getClientHost();

        if (host == null) {
            // rmi.5B=There are no in-progress RMI calls in the current thread.
            throw new ServerNotActiveException(Messages.getString("rmi.5B")); //$NON-NLS-1$
        }
        return host;
    }

    /**
     * @see RemoteRef.getRefClass(ObjectOutput)
     */
    public String getRefClass(ObjectOutput out) {
        return "UnicastServerRef"; //$NON-NLS-1$
    }

    /**
     * For this type of ref no additional data is written to the stream.
     */
    public void writeExternal(ObjectOutput out) throws IOException {
    }

    /**
     * For this type of ref no additional data is read from the stream.
     */
    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException {
    }

    /**
     * Exports remote object so it becomes available for remote calls.
     *
     * @param obj remote object implementation
     * @param data additional data needed for exporting the object (not used)
     * @param useProxyStubs If true then Proxy stubs will be generated if stub
     *        class could not be found in classpath and codebase; if false Proxy
     *        stubs will not be tried (this is needed for
     *        UnicastRemoteObject.exportObject(Remote) method because it
     *        returns RemoteStub class (but Proxy class could not be casted
     *        to it)
     * @param startListen if false, ServerSocket listening thread will not be
     *        started (this is used for DGC, for example); otherwise listening
     *        thread will be started and object becomes available for
     *        connections from clients
     * @param isSystem if true then existence of this object will not prevent
     *        VM from exiting (for example, for rmiregistry)
     *
     * @throws RemoteException if any exception occurred while trying to export
     *         the object
     */
    public Remote exportObject(Remote obj,
                               Object data,
                               boolean useProxyStubs,
                               boolean startListen,
                               boolean isSystem)
            throws RemoteException {
        this.isSystem = isSystem;
        ref = new RMIReference(obj);
        implClassName = obj.getClass().getName();
        Remote stub = null;

        // obtain class directly implementing Remote interface
        Class remoteClass = RMIUtil.getRemoteClass(obj.getClass());

        // load and instantiate skel class if any
        skel = getSkelInstance(remoteClass);
        boolean isProxyStub = false;

        if (serverRefLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.log.95=Obtaining stub class for: {0}
            serverRefLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.95", //$NON-NLS-1$
                    remoteClass.getName()));
        }
        Class stubClass = null;

        if (!useProxyStubs || !ignoreStubClasses) {
            // load stub class statically generated by rmic
            stubClass = loadStubClass(remoteClass, !useProxyStubs);

            // rmi.log.96=Loaded "static" stub for {0}
            if (serverRefLog.isLoggable(RMILog.VERBOSE)) {
                serverRefLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.96", //$NON-NLS-1$
                        remoteClass.getName()));
            }
        }

        if (stubClass == null) {
            // try to create dynamic proxy stub class
            try {
                stubClass = Proxy.getProxyClass(obj.getClass().getClassLoader(),
                        RMIUtil.getRemoteInterfaces(obj.getClass()));
                //stubClass = RMIClassLoader.loadProxyClass(null,
                //        RMIUtil.getRemoteInterfacesNames(obj.getClass()),
                //        obj.getClass().getClassLoader());

                if (serverRefLog.isLoggable(RMILog.VERBOSE)) {
                    // rmi.log.97=Loaded dynamic stub for {0}
                    serverRefLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.97", //$NON-NLS-1$
                            remoteClass.getName()));
                }
                isProxyStub = true;
            } catch (Exception ex) {
                // rmi.62=Unable to create dynamic proxy stub class
                throw new StubNotFoundException(Messages.getString("rmi.62"), ex); //$NON-NLS-1$
            }
        }

        try {

            // add remote method which could be invoked to the hash table
            remoteMethods = RMIUtil.getRemoteMethods(obj.getClass());

            /*
             * Make all remote methods accessible (for protected or
             * package-protected cases).
             */
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    AccessibleObject.setAccessible(
                            (Method []) (remoteMethods.values().toArray(
                                    new Method[remoteMethods.size()])), true);
                    return null;
                }
            });

            // init stub
            UnicastRef rref = getClientRef(ep, objId);

            if (!isProxyStub) {
                stub = (RemoteStub) stubClass.getConstructor(
                        new Class[] { RemoteRef.class }).newInstance(
                                new Object[] { rref });
            } else {
                stub = (Remote) stubClass.getConstructor(
                        new Class[] { InvocationHandler.class }).newInstance(
                            new Object[] {
                                new RemoteObjectInvocationHandler( rref ) });
            }

            if (serverRefLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.log.98=Instantiated stub: {0}
                serverRefLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.98", stub)); //$NON-NLS-1$
            }

            if (startListen) {
                // start listening thread
                mgr = ServerConnectionManager.getMgr(ep);
            }
        } catch (BindException be) {
            // rmi.63=Unable to export object: port {0} already in use
            throw new ExportException(Messages.getString("rmi.63", //$NON-NLS-1$
                    ep.getPort()), be);
        } catch (Exception ex) {
            // rmi.64=Unable to export object on port {0}
            throw new ExportException(Messages.getString("rmi.64", //$NON-NLS-1$
                    ep.getPort()), ex);
        }
        return stub;
    }

    /**
     * Returns true if force parameter is false and there are no in-progress
     * calls to the object handled by this ref and false otherwise. This method
     * could be overridden by subclasses to "really" unexport handled object.
     *
     * @param force if true then we may not care about active calls
     *
     * @return true if force parameter is false and there are no in-progress
     *         calls to the object handled by this ref and false otherwise
     */
    public boolean unexportObject(boolean force) {
        return force ? true : !mgr.hasActiveCalls();
    }

    /**
     * Returns true if the handled Remote object is system and false otherwise.
     *
     * @return true if the handled Remote object is system and false otherwise
     */
    public boolean isSystem() {
        return isSystem;
    }

    /**
     * Performs actual remote method invocation.
     *
     * @param call RemoteCall
     *
     * @throws IOException if any I/O error occurred during remote method call
     */
    public void processCall(RemoteCall call) throws IOException {
        // read method and parameters
        RMIObjectInputStream oin = (RMIObjectInputStream) call.getInputStream();
        int op = oin.readInt(); // read operation
        long h = oin.readLong(); // read method hash

        if (op != -1) {
            // Using 1.1. RMI protocol version
            if (skel == null) {
                // rmi.65=Skeleton class not found.
                throw new UnmarshalException(Messages.getString("rmi.65")); //$NON-NLS-1$
            }
            String m = skel.getOperations()[op].toString();
            logServerCall(m);

            try {
                skel.dispatch((Remote) ref.get(), call, op, h);
            } catch (Throwable t) {
                Exception ex = prepareException(m, t);
                RMIObjectOutputStream oout =
                        (RMIObjectOutputStream) call.getResultStream(false);
                oout.writeObject(ex);
            }

            try {
                call.getOutputStream().flush();
            } catch (IOException ioe) {
            }
            return;
        }

        // Using 1.2 RMI protocol version
        Method m = (Method) remoteMethods.get(new Long(h));

        if (m == null) {
            // rmi.66=Method with hash = {0} not found.
            throw new UnmarshalException(Messages.getString("rmi.66", h)); //$NON-NLS-1$
        }
        logServerCall(m.toString());
        Object[] params = readParams(m, oin);
        call.releaseInputStream();
        Object toReturn = null;
        Throwable toThrow = null;

        // locally call the method
        try {
            toReturn = m.invoke(ref.get(), params);
        } catch (InvocationTargetException ite) {
            toThrow = prepareException(m.toString(),
                    ((InvocationTargetException) ite).getTargetException());
        } catch (Throwable t) {
            toThrow = prepareException(m.toString(), t);
        }

        // return result of method call
        RMIObjectOutputStream oout =
                (RMIObjectOutputStream) call.getResultStream(toThrow == null);

        try {
            if (toThrow != null) {
                oout.writeObject(toThrow);
            } else if (toReturn != null) {
                oout.writeRMIObject(toReturn, m.getReturnType());
            } else if (m.getReturnType() != Void.TYPE) {
                oout.writeObject(null);
            }
            oout.flush();
        } catch (Error er) {
            // rmi.67=Error occurred while marshalling return value
            throw new ServerError(Messages.getString("rmi.67"), er); //$NON-NLS-1$
        }
    }

    /**
     * Loads stub class for the given remote class.
     *
     * @param c Class whose stub should be loaded
     * @param throwException should we throw StubNotFoundException in case of
     *        failure or silently return null
     *
     * @return loaded stub or null if throwException is false and any failure
     *         occurred during stub loading
     *
     * @throws StubNotFoundException if throwException parameter is true and any
     *         failure occurred during stub loading
     */
    protected Class loadStubClass(Class c, boolean throwException)
            throws StubNotFoundException {
        String stubName = c.getName() + "_Stub"; //$NON-NLS-1$
        ClassLoader cl = c.getClassLoader();

        try {
            if (cl != null) {
                return cl.loadClass(stubName);
            } else {
                return Class.forName(stubName);
            }
        } catch (ClassNotFoundException cnfe) {
            if (throwException) {
                // rmi.68=Stub {0} not found.
                throw new StubNotFoundException(Messages.getString("rmi.68", stubName), cnfe); //$NON-NLS-1$
            }
        }
        return null;
    }

    /**
     * Loads and instantiates skel class for the given remote class.
     *
     * @param c Class whose skel should be loaded and instantiated
     *
     * @return created skel class or null if any Exception occurred during
     *         skel loading or instantiating
     */
    protected Skeleton getSkelInstance(Class c) {
        String skelName = c.getName() + "_Skel"; //$NON-NLS-1$
        ClassLoader cl = c.getClassLoader();

        try {
            Class skelClass;

            if (cl != null) {
                skelClass = cl.loadClass(skelName);
            } else {
                skelClass = Class.forName(skelName);
            }
            return (Skeleton) skelClass.newInstance();
        } catch (Exception ex) {
        }
        return null;
    }

    /**
     * Creates client-side reference holding the given Endpoint and Object ID.
     *
     * @param ep Endpoint for UnicastRef creation
     * @param objId Object ID for UnicastRef creation
     *
     * @return created client-sice reference
     */
    protected UnicastRef getClientRef(Endpoint ep, ObjID objId) {
        if (ep.getClientSocketFactory() == null) {
            return new UnicastRef(ep, objId, true);
        } else {
            return new UnicastRef2(ep, objId, true);
        }
    }

    // Reads parameters for the given method from the specified InputStream.
    private Object[] readParams(Method m, RMIObjectInputStream oin)
            throws RemoteException {
        Class[] paramTypes = m.getParameterTypes();
        Object[] params = new Object[paramTypes.length];

        try {
            for (int i = 0; i < paramTypes.length; ++i) {
                params[i] = oin.readRMIObject(paramTypes[i]);
            }
        } catch (RemoteException re) {
            // rmi.69=RemoteException occurred while unmarshalling arguments
            throw new ServerException(Messages.getString("rmi.69"), re); //$NON-NLS-1$
        } catch (IOException ioe) {
            // rmi.6A=IOException occurred while unmarshalling arguments
            throw new UnmarshalException(Messages.getString("rmi.6A"), ioe); //$NON-NLS-1$
        } catch (ClassNotFoundException cnfe) {
            // rmi.6B=ClassNotFoundException occurred while unmarshalling arguments
            throw new UnmarshalException(Messages.getString("rmi.6B"), cnfe); //$NON-NLS-1$
        } catch (Error er) {
            // rmi.6C=Error occurred while unmarshalling arguments
            throw new ServerError(Messages.getString("rmi.6C"), er); //$NON-NLS-1$
        }
        return params;
    }

    /*
     * Prepares Exception to be sent to client as a result of remote method
     * call.
     *
     * @param m Method string representation which cause the exception
     * @param t Throwable to be processed.
     *
     * @return prepared Exception
     */
    private Exception prepareException(String m, Throwable t) {
        Throwable preparedEx = null;
        logServerException(m, t);

        if (t instanceof Error) {
            // rmi.6D=Error occurred while remote method invocation
            preparedEx = new ServerError(Messages.getString("rmi.6D"), (Error) t); //$NON-NLS-1$
        } else if (t instanceof RemoteException) {
            // rmi.6E=RemoteException occurred while remote method invocation
            preparedEx = new ServerException(Messages.getString("rmi.6E"), //$NON-NLS-1$
                    (RemoteException) t);
        } else {
            preparedEx = t;
        }
        Exception toReturn = (Exception) preparedEx;

        if (suppressST) {
            // clear stack traces
            StackTraceElement[] emptyST = new StackTraceElement[0];

            for (; preparedEx != null; preparedEx = preparedEx.getCause()) {
                preparedEx.setStackTrace(emptyST);
            }
        }
        return toReturn;
    }

    // Logs remote method call.
    private void logServerCall(String m) {
        if (serverCallsLog.isLoggable(RMILog.VERBOSE)) {
            String client = ServerConnectionManager.getClientHost();

            if (client != null) {
                // rmi.log.99=Remote call from [{0}]
                client = Messages.getString("rmi.log.99", client); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                // rmi.log.9A=Local remote call
                client = Messages.getString("rmi.log.9A"); //$NON-NLS-1$
            }
            // rmi.log.9B={0}: method:[{1}], class:[{2}].
            serverCallsLog.log(RMILog.VERBOSE,Messages.getString("rmi.log.9B", //$NON-NLS-1$
                    new Object[]{client, m, implClassName}));
        }
    }

    // Logs Exception thrown by remote method call.
    private void logServerException(String m, Throwable t) {
        if (printST || serverCallsLog.isLoggable(RMILog.BRIEF)) {
            String client = ServerConnectionManager.getClientHost();

            if (client != null) {
                // rmi.log.9C=from {0}
                client = Messages.getString("rmi.log.9C", client); //$NON-NLS-1$
            } else {
                // rmi.log.9D=locally
                client = Messages.getString("rmi.log.9D"); //$NON-NLS-1$
            }
            // rmi.log.9E=Exception thrown while calling [{0}] method of {1} class requested {2}:
            String logMsg = Messages.getString("rmi.log.9E", //$NON-NLS-1$
                    new Object[]{m, implClassName, client});

            if (printST) {
                synchronized (System.err) {
                    System.err.println(logMsg);
                    t.printStackTrace(System.err);
                }
            }

            if (serverCallsLog.isLoggable(RMILog.BRIEF)) {
                // rmi.log.9E=Exception thrown while calling [{0}] method of {1} class requested {2}:
                serverCallsLog.log(RMILog.BRIEF,Messages.getString("rmi.log.9E", //$NON-NLS-1$ 
                                new Object[]{ m, implClassName, client}), t);
            }
        }
    }
}
