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

/**
 * @author Anton V. Karnachuk
 */

/**
 * Created on 11.04.2005
 */
package org.apache.harmony.jpda.tests.jdwp.MultiSession;

import org.apache.harmony.jpda.tests.jdwp.Events.DebuggeeException;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;


/**
 * Debuggee for ExceptionTest unit test.
 * Generates caught DebuggeeException exception.
 */
public class ExceptionDebuggee extends SyncDebuggee {

    public static void main(String[] args) {
        runDebuggee(ExceptionDebuggee.class);
    }
    
    public void run(){
        
        logWriter.println("--> ExceptionDebuggee: STARTED");
        // load and prepare DebuggeeException class
        DebuggeeException ex = new DebuggeeException("dummy exception");
        
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        
        logWriter.println("--> ExceptionDebuggee: Wait for SGNL_CONTINUE...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("--> ExceptionDebuggee: SGNL_CONTINUE has been received!");
        
        try {
            // throw caught exception
            throw new DebuggeeException("Caught debuggee exception");
        } catch (DebuggeeException e) {
            logWriter.println("-- ExceptionDebuggee: Exception: \""+e.getMessage()+"\" was thrown");
        }
        
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        logWriter.println("DUMP{" + ex + "}");
        logWriter.println("-- ExceptionDebuggee: FINISHing...");
    }
}
