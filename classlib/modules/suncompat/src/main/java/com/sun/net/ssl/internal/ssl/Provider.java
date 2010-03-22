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

package com.sun.net.ssl.internal.ssl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import org.apache.harmony.xnet.provider.jsse.JSSEProvider;

public final class Provider extends java.security.Provider {

    private static final java.security.Provider jsse = new JSSEProvider();

    public Provider() {
        super(jsse.getName(), jsse.getVersion(), jsse.getInfo());
        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                putAll(jsse);
                return null;
            }
        });
    }
}
