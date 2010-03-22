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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import java.util.ArrayList;
import java.util.Random;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class InstancesDebuggee extends SyncDebuggee {

	static final int maxNum = 17;
	
	static int reachableObjNum;
	
	static int unreachableObjNum;
	
	static {
		reachableObjNum = new Random().nextInt(maxNum) + 2;
		unreachableObjNum = new Random().nextInt(maxNum) + 2;
	}

	@Override
	public void run() {
		//Objects reachable for garbage collection purpose
		
		ArrayList<MockClass> reachableObjs = new ArrayList<MockClass>();
        
		for(int i = 0; i < reachableObjNum; i++) {
			reachableObjs.add(new MockClass(true));
		}
       
		//Objects unreachable
		for(int i = 0; i < unreachableObjNum; i++) {
			new MockClass(false);
		}
		synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        logWriter.println("--> Debuggee: InstancesDebuggee...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
	}

	public static void main(String[] args) {
		runDebuggee(InstancesDebuggee.class);
	}

}

class MockClass {
	private boolean isReachable;
	MockClass(boolean isReachable){
		this.isReachable = isReachable;
	}
}


