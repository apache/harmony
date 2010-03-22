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
 * @author Vitaly A. Provodin
 */

/**
 * Created on 04.02.2005
 */
package org.apache.harmony.jpda.tests.framework;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class defines an interface to keep an information about all started
 * debuggees.
 * <p>
 * As a rule JPDA tests consist of two parts: the test that represents a
 * part of the debugger-side application and the debuggee part. It is a good
 * practice if the JPDA tests control debuggee's lifecycle (usually via JDWP
 * channel), but in some cases it can not be done, for example because of
 * product bugs.
 * <p>
 * This class is aimed to be an additional facility to stop each debuggee
 * activity if it can not be done by the test in the regular way via JDWP
 * channel.
 */
public class DebuggeeRegister {

    LinkedList<DebuggeeWrapper> registered = new LinkedList<DebuggeeWrapper>();
    
    /**
     * Registers started debuggee.
     * 
     * @param debuggee <code>DebuggeeWrapper</code> of the new started
     * debuggee to register
     */
    public void register(DebuggeeWrapper debuggee) {
       registered.add(debuggee); 
    }

    /**
     * Unregisters specified debuggee.
     * 
     * @param debuggee <code>DebuggeeWrapper</code> of the debuggee to unregister
     * returns true if debuggee was registered
     */
    public boolean unregister(DebuggeeWrapper debuggee) {
        return registered.remove(debuggee);
    }

    /**
     * Returns list of all registered DebuggeeWrappers.
     * 
     * @return array of DebuggeeWrappers
     */
    public List getAllRegistered() {
        return registered;
    }

    /**
     * Stops each of registered DebuggeeWrappers by invoking DebuggeeWrapper.stop().
     */
    public void stopAllRegistered() {
        for (Iterator iter = registered.iterator(); iter.hasNext(); ) {
            DebuggeeWrapper wrapper = (DebuggeeWrapper)iter.next();
            if (wrapper != null) {
                wrapper.stop();
            }
        }
        registered.clear();
    }
}
