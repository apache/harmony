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

package org.apache.harmony.jpda.tests.jdwp.ObjectReference;


import java.util.ArrayList;
import java.util.Random;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class ReferringObjectsDebuggee extends SyncDebuggee {

    static final int maxNum = 17;
    
    static int referringObjNum;
    
    static int nonreferringObjNum;
    
    static {
        referringObjNum = new Random().nextInt(maxNum) + 2;
        nonreferringObjNum = new Random().nextInt(maxNum) + 2;
    }

    @Override
    public void run() {
        ReferringObjectsReferree001 referree = new ReferringObjectsReferree001();
        //Referrer objects which contain reference of referree object       
        ArrayList<ReferringObjectsReferrer001> referringObjs = new ArrayList<ReferringObjectsReferrer001>();
        
        for(int i = 0; i < referringObjNum; i++) {
            referringObjs.add(new ReferringObjectsReferrer001(referree));
        }
        
        //Referrer objects which contain reference which is null 
        ArrayList<ReferringObjectsReferrer001> nonreferringObjs = new ArrayList<ReferringObjectsReferrer001>();
        
        for(int i = 0; i < nonreferringObjNum; i++) {
            nonreferringObjs.add(new ReferringObjectsReferrer001(null));
        }
        
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        logWriter.println("--> Debuggee: ReferringObjectsDebuggee...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        runDebuggee(ReferringObjectsDebuggee.class);
    }

}

class ReferringObjectsReferree001 {
    
}

class ReferringObjectsReferrer001 {
    private boolean isReferrer;
    private ReferringObjectsReferree001 reference;
    
    ReferringObjectsReferrer001(ReferringObjectsReferree001 reference) {
        if(null != reference) {
            isReferrer = true;
        }
        else {
            isReferrer = false;
        }
        this.reference = reference;
    }
}


