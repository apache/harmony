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


/**
 * @com.intel.drl.spec_ref
 *
 * @author  Mikhail A. Markov
 */
public abstract class RemoteStub extends RemoteObject {

    private static final long serialVersionUID = -1585587260594494182L;

    /**
     * @com.intel.drl.spec_ref
     */
    protected RemoteStub(RemoteRef ref) {
        super(ref);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    protected RemoteStub() {
        super();
    }

    /**
     * @com.intel.drl.spec_ref
     * @deprecated This method is deprecated since Java v1.2. The constructor
     *  {@link #RemoteStub(java.rmi.server.RemoteRef)} should be used instead.
     */
    @Deprecated
    protected static void setRef(RemoteStub stub, RemoteRef ref) {
        stub.ref = ref;
    }
}
