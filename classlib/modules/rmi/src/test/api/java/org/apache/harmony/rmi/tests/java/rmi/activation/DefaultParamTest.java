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

package org.apache.harmony.rmi.tests.java.rmi.activation;

import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.rmi.activation.Activatable;
import java.rmi.activation.ActivationDesc;
import java.rmi.activation.ActivationGroup;
import java.rmi.activation.ActivationGroupDesc;
import java.rmi.activation.ActivationGroupID;
import java.rmi.activation.ActivationSystem;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.Properties;

import org.apache.harmony.rmi.JavaInvoker;
import org.apache.harmony.rmi.common.SubProcess;

import junit.framework.TestCase;

public class DefaultParamTest extends TestCase {

    private SubProcess rmid;

    private SubProcess rmiregistry;

    @Override
    public void setUp() {
        try {
            rmid = JavaInvoker.invokeSimilar((String[]) null, "org.apache.harmony.rmi.activation.Rmid", (String[]) null, true, true);
            //Runtime.getRuntime().exec("rmid");
            rmid.pipeError();
            rmid.pipeInput();
            rmid.closeOutput();

            rmiregistry = JavaInvoker.invokeSimilar((String[]) null,
                    "org.apache.harmony.rmi.registry.RegistryImpl", (String[]) null, true, true);
            rmiregistry.pipeError();
            rmiregistry.pipeInput();
            rmiregistry.closeOutput();

            Thread.sleep(5000);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public void tearDown() {
        rmid.destroy();
        rmiregistry.destroy();
    }

    public void testSimpleInstall() throws Exception {
        try {
            Properties props = new Properties();
            ActivationGroupDesc groupDesc = new ActivationGroupDesc(props, null);

            System.out.println("groupDesc = " + groupDesc);

            System.out.flush();
            ActivationSystem as = ActivationGroup.getSystem();

            System.out.println("ActivationSystem = " + as);

            ActivationGroupID groupID = as.registerGroup(groupDesc);
            System.out.println("groupID = " + groupID);
            System.out.println("Activation group descriptor registered.");

            MarshalledObject data = new MarshalledObject("HelloImpl");
            System.out.println("MarshalledObject data = " + data);

            ActivationDesc desc = new ActivationDesc(groupID, "org.apache.harmony.rmi.tests.java.rmi.activation.HelloImpl", "", null);
            System.out.println("Registering ActivationDesc:");
            Remote stub = Activatable.register(desc);
            System.out.println("Activation descriptor registered: " + stub);

            Registry reg = LocateRegistry.getRegistry();
            System.out.println("Registry = " + reg);

            reg.rebind("HelloImpl_Stub", stub);
            System.out.println("Stub bound in registry.");
        } catch (Throwable t) {
            System.out.println("Exception in HelloInstaller: " + t);
            t.printStackTrace();
            fail("Exception in HelloInstaller: " + t);
        }
    }
}
