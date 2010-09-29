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
package org.apache.harmony.rmi.server;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.Operation;
import java.rmi.server.RemoteCall;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteRef;

import junit.framework.TestCase;

public class RemoteObjectTest extends TestCase {

    class MyRef implements RemoteRef {

        /*
         * (non-Javadoc)
         * 
         * @see java.rmi.server.RemoteRef#done(java.rmi.server.RemoteCall)
         */
        public void done(RemoteCall call) throws RemoteException {

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.rmi.server.RemoteRef#getRefClass(java.io.ObjectOutput)
         */
        public String getRefClass(ObjectOutput out) {
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.rmi.server.RemoteRef#invoke(java.rmi.Remote,
         * java.lang.reflect.Method, java.lang.Object[], long)
         */
        public Object invoke(Remote obj, Method m, Object[] params, long h)
                throws Exception {
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.rmi.server.RemoteRef#invoke(java.rmi.server.RemoteCall)
         */
        public void invoke(RemoteCall call) throws Exception {

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.rmi.server.RemoteRef#newCall(java.rmi.server.RemoteObject,
         * java.rmi.server.Operation[], int, long)
         */
        public RemoteCall newCall(RemoteObject obj, Operation[] op, int a1,
                long a2) throws RemoteException {
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.rmi.server.RemoteRef#remoteEquals(java.rmi.server.RemoteRef)
         */
        public boolean remoteEquals(RemoteRef ref) {
            return false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.rmi.server.RemoteRef#remoteHashCode()
         */
        public int remoteHashCode() {
            return 0;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.rmi.server.RemoteRef#remoteToString()
         */
        public String remoteToString() {
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
         */
        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
         */
        public void writeExternal(ObjectOutput out) throws IOException {

        }

    }

    class MyRemoteObject extends RemoteObject {
        public MyRemoteObject(RemoteRef ref) {
            this.ref = ref;
        }
    }

    public void testGetRef() {
        MyRef ref = new MyRef();
        MyRemoteObject obj = new MyRemoteObject(ref);

        assertSame(ref, obj.getRef());
    }
}
