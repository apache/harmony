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
package org.apache.harmony.rmi.tests.java.rmi.activation;

import java.rmi.RemoteException;
import java.rmi.activation.ActivationDesc;
import java.rmi.activation.ActivationException;
import java.rmi.activation.ActivationGroup;
import java.rmi.activation.ActivationGroupDesc;
import java.rmi.activation.ActivationGroupID;
import java.rmi.activation.ActivationID;
import java.rmi.activation.ActivationInstantiator;
import java.rmi.activation.ActivationMonitor;
import java.rmi.activation.ActivationSystem;
import java.rmi.activation.UnknownGroupException;
import java.rmi.activation.UnknownObjectException;

import junit.framework.TestCase;

public class ActivationGroupIDTest extends TestCase {

	class MyActivationSystem implements ActivationSystem {

		public MyActivationSystem() throws ActivationException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.rmi.activation.ActivationSystem#activeGroup(java.rmi.activation
		 * .ActivationGroupID, java.rmi.activation.ActivationInstantiator, long)
		 */
		public ActivationMonitor activeGroup(ActivationGroupID gID,
				ActivationInstantiator aInst, long incarnation)
				throws UnknownGroupException, ActivationException,
				RemoteException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seejava.rmi.activation.ActivationSystem#getActivationDesc(java.rmi.
		 * activation.ActivationID)
		 */
		public ActivationDesc getActivationDesc(ActivationID aID)
				throws ActivationException, UnknownObjectException,
				RemoteException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.rmi.activation.ActivationSystem#getActivationGroupDesc(java.
		 * rmi.activation.ActivationGroupID)
		 */
		public ActivationGroupDesc getActivationGroupDesc(ActivationGroupID gID)
				throws ActivationException, UnknownGroupException,
				RemoteException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.rmi.activation.ActivationSystem#registerGroup(java.rmi.activation
		 * .ActivationGroupDesc)
		 */
		public ActivationGroupID registerGroup(ActivationGroupDesc gDesc)
				throws ActivationException, RemoteException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.rmi.activation.ActivationSystem#registerObject(java.rmi.activation
		 * .ActivationDesc)
		 */
		public ActivationID registerObject(ActivationDesc aDesc)
				throws ActivationException, UnknownGroupException,
				RemoteException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seejava.rmi.activation.ActivationSystem#setActivationDesc(java.rmi.
		 * activation.ActivationID, java.rmi.activation.ActivationDesc)
		 */
		public ActivationDesc setActivationDesc(ActivationID aID,
				ActivationDesc aDesc) throws ActivationException,
				UnknownObjectException, UnknownGroupException, RemoteException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.rmi.activation.ActivationSystem#setActivationGroupDesc(java.
		 * rmi.activation.ActivationGroupID,
		 * java.rmi.activation.ActivationGroupDesc)
		 */
		public ActivationGroupDesc setActivationGroupDesc(
				ActivationGroupID gID, ActivationGroupDesc gDesc)
				throws ActivationException, UnknownGroupException,
				RemoteException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.rmi.activation.ActivationSystem#shutdown()
		 */
		public void shutdown() throws RemoteException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.rmi.activation.ActivationSystem#unregisterGroup(java.rmi.activation
		 * .ActivationGroupID)
		 */
		public void unregisterGroup(ActivationGroupID gID)
				throws ActivationException, UnknownGroupException,
				RemoteException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.rmi.activation.ActivationSystem#unregisterObject(java.rmi.activation
		 * .ActivationID)
		 */
		public void unregisterObject(ActivationID aID)
				throws ActivationException, UnknownObjectException,
				RemoteException {

		}

	}

	public void testEquals() throws ActivationException {
		ActivationSystem as = new MyActivationSystem();

		ActivationGroupID agid = new ActivationGroupID(as);
		ActivationGroupID agid2 = agid;
		ActivationGroupID agid3 = new ActivationGroupID(as);

		assertSame(agid2, agid);
		assertFalse(agid.equals(agid3));
	}

	public void testGetSystem() throws ActivationException {
		ActivationSystem as = new MyActivationSystem();
		ActivationGroupID agid = new ActivationGroupID(as);

		assertSame(as, agid.getSystem());
	}

	public void testHashcode() throws ActivationException {
		ActivationSystem as = new MyActivationSystem();

		ActivationGroupID agid = new ActivationGroupID(as);
		ActivationGroupID agid3 = new ActivationGroupID(as);

		assertTrue(agid.hashCode() != agid3.hashCode());
	}

	public void testToString() throws ActivationException {
		ActivationSystem as = new MyActivationSystem();

		ActivationGroupID agid = new ActivationGroupID(as);
		ActivationGroupID agid3 = new ActivationGroupID(as);

		assertTrue(!agid.toString().equals(agid3.toString()));
	}

}
