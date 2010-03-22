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
 * Created on 31.01.2005
 */
package org.apache.harmony.jpda.tests.share;

import org.apache.harmony.jpda.tests.framework.TestOptions;

/**
 * This class provides additional options for unit tests.
 * <p>
 * Currently the following additional options are supported:
 * <ul>
 *   <li><i>jpda.settings.debuggeeLaunchKind=auto|manual</i> - enables
 *       manual launching of debuggee VM for debugging purpose.
 * </ul>
 *  
 */
public class JPDATestOptions extends TestOptions {

    /**
     * Returns kind of launching debuggee VM, which can be "auto" or "manual".
     * 
     * @return option "jpda.settings.debuggeeLaunchKind" or "auto" by default.
     */
    public String getDebuggeeLaunchKind() {
        return System.getProperty("jpda.settings.debuggeeLaunchKind", "auto");
    }

}
