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

import java.lang.reflect.Method;
import java.io.Externalizable;
import java.io.ObjectOutput;
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * @com.intel.drl.spec_ref
 *
 * @author  Mikhail A. Markov
 */
public interface RemoteRef extends Externalizable {

    public static final long serialVersionUID = 3632638527362204081L;

    /** @com.intel.drl.spec_ref */
    public static final String packagePrefix =
            "org.apache.harmony.rmi.remoteref"; //$NON-NLS-1$

    /**
     * @com.intel.drl.spec_ref
     */
    public Object invoke(Remote obj,
                         Method m,
                         Object[] params,
                         long h)
            throws Exception;

    /**
     * @com.intel.drl.spec_ref
     * @deprecated The newCall() -> invoke() -> done() method invocation
     *  triplet is replaced by the single invocation of the 
     *  invoke(Remote, Method, Object[], long) method in RMI v1.2.
     */
    @Deprecated
    public RemoteCall newCall(RemoteObject obj,
                              Operation[] op,
                              int a1,
                              long a2)
            throws RemoteException;

    /**
     * @com.intel.drl.spec_ref
     */
    public String getRefClass(ObjectOutput out);

    /**
     * @com.intel.drl.spec_ref
     */
    public boolean remoteEquals(RemoteRef ref);

    /**
     * @com.intel.drl.spec_ref
     * @deprecated The newCall() -> invoke() -> done() method invocation
     *  triplet is replaced by the single invocation of the 
     *  invoke(Remote, Method, Object[], long) method in RMI v1.2.
     */
    @Deprecated
    public void invoke(RemoteCall call) throws Exception;

    /**
     * @com.intel.drl.spec_ref
     * @deprecated The newCall() -> invoke() -> done() method invocation
     *  triplet is replaced by the single invocation of the 
     *  invoke(Remote, Method, Object[], long) method in RMI v1.2.
     */
    @Deprecated
    public void done(RemoteCall call) throws RemoteException;

    /**
     * @com.intel.drl.spec_ref
     */
    public String remoteToString();

    /**
     * @com.intel.drl.spec_ref
     */
    public int remoteHashCode();
}
