/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
* @author Ivan Popov
*/

package com.sun.jdi;

import com.sun.jdi.VirtualMachineManager;

/**
 * This class is for replacing corresponding empty class in JDI API provided by Eclipse JDT, 
 * it works as a wrapper for @link org.apache.harmony.tools.internal.jdi.Bootstrap.
 */
public class Bootstrap {

    /**
     * Redirects initialization of JDI implementation to 
     * @link org.apache.harmony.tools.internal.jdi.Bootstrap#virtualMachineManager().
     * 
     * @return instance of VirtualMachineManager created by JDI implementation
     */
    static public VirtualMachineManager virtualMachineManager() {
        return org.apache.harmony.tools.internal.jdi.Bootstrap.virtualMachineManager();
    }

}
