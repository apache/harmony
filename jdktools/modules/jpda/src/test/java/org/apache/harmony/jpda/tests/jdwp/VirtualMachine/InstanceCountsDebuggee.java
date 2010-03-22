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

package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import java.util.ArrayList;
import java.util.Random;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class InstanceCountsDebuggee extends SyncDebuggee {

	static final int maxNum = 17;
	
	static int reachableObjNumOfClass1;
	
	static int reachableObjNumOfClass2;
    
	static {
        reachableObjNumOfClass1 = new Random().nextInt(maxNum) + 2;
        reachableObjNumOfClass2 = new Random().nextInt(maxNum) + 2;
	}

	@SuppressWarnings("unchecked")
    @Override
	public void run() {
		//Objects reachable for garbage collection purpose
		ArrayList reachableObjs = new ArrayList();
        
		for(int i = 0; i < reachableObjNumOfClass1; i++) {
			reachableObjs.add(new MockClass1(true));
		}
        for(int i = 0; i < reachableObjNumOfClass2; i++) {
            reachableObjs.add(new MockClass2(true));
        }
       
		//Objects unreachable
		for(int i = 0; i < reachableObjNumOfClass1/2; i++) {
			new MockClass1(false);
		}
        for(int i = 0; i < reachableObjNumOfClass2/2; i++) {
            new MockClass2(false);
        }
        
		synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        logWriter.println("--> Debuggee: InstancesDebuggee...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
	}

	public static void main(String[] args) {
		runDebuggee(InstanceCountsDebuggee.class);
	}

}

class MockClass1 {
	private boolean isReachable;
	MockClass1(boolean isReachable){
		this.isReachable = isReachable;
	}
}

class MockClass2{
    private boolean isReachable;
    
    MockClass2(boolean isReachable){
        this.isReachable = isReachable;
    }    
}
