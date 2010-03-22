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
package java.rmi.server;

import java.io.InvalidObjectException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.rmi.UnexpectedException;

import org.apache.harmony.rmi.common.RMIHash;
import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * @com.intel.drl.spec_ref
 *
 * @author  Mikhail A. Markov
 */
public class RemoteObjectInvocationHandler extends RemoteObject
        implements InvocationHandler {

    private static final long serialVersionUID = 2L;

    /**
     * @com.intel.drl.spec_ref
     */
    public RemoteObjectInvocationHandler(RemoteRef ref) {
        super(ref);

        if (ref == null) {
            // rmi.20=RemoteRef parameter could not be null.
            throw new NullPointerException(Messages.getString("rmi.20")); //$NON-NLS-1$
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public Object invoke(Object proxy, Method m, Object[] args)
            throws Throwable {
        Class mClass = m.getDeclaringClass();

        if (m.getDeclaringClass() == Object.class) {
            return invokeObjectMethod(proxy, m, args);
        } else if (!(proxy instanceof Remote)) {
            // rmi.21=Proxy does not implement Remote interface.
            throw new IllegalArgumentException(Messages.getString("rmi.21")); //$NON-NLS-1$
        } else {
            return invokeRemoteMethod(proxy, m, args);
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    private void readObjectNoData() throws InvalidObjectException {
        // rmi.22=No data in stream for class {0} 
        throw new InvalidObjectException(Messages.getString("rmi.22", //$NON-NLS-1$
                this.getClass().getName()));
    }

    /*
     * Invokes methods from Object class.
     */
    private Object invokeObjectMethod(Object proxy, Method m, Object[] args) {
        String mName = m.getName();

        if (mName.equals("hashCode")) { //$NON-NLS-1$
            // return result of hashCode method call from RemoteObject class
            return new Integer(hashCode());
        } else if (mName.equals("equals")) { //$NON-NLS-1$
            Object obj = args[0];
            return new Boolean((proxy == obj) // the same object?
                    || (obj != null && Proxy.isProxyClass(obj.getClass())
                            && equals(Proxy.getInvocationHandler(obj))));
        } else if (mName.equals("toString")) { //$NON-NLS-1$
            Class[] interf = proxy.getClass().getInterfaces();

            if (interf.length == 0) {
                return "Proxy[" + toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
            }
            String str = "Proxy[interf:["; //$NON-NLS-1$

            for (int i = 0; i < interf.length - 1; ++i) {
                str += interf[i].getName() + ", "; //$NON-NLS-1$
            }
            return str + interf[interf.length - 1].getName() + "], " //$NON-NLS-1$
                    + toString() + "]"; //$NON-NLS-1$
        } else {
            // rmi.23=Illegal method from Object class: {0}
            throw new IllegalArgumentException(Messages.getString("rmi.23", m)); //$NON-NLS-1$
        }
    }

    /*
     * Invokes Remote methods.
     */
    private Object invokeRemoteMethod(Object proxy, Method m, Object[] args)
            throws Throwable {
        try {
            return ref.invoke((Remote) proxy, m, args,
                    RMIHash.getMethodHash(m));
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            Method m1 = proxy.getClass().getMethod(m.getName(),
                    m.getParameterTypes());
            Class[] declaredEx = m1.getExceptionTypes();

            for (int i = 0; i < declaredEx.length; ++i) {
                if (declaredEx[i].isAssignableFrom(ex.getClass())) {
                    throw ex;
                }
            }
            // rmi.24=Unexpected exception
            throw new UnexpectedException(Messages.getString("rmi.24"), ex); //$NON-NLS-1$
        }
    }
}
