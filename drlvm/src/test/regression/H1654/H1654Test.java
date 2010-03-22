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

package org.apache.harmony.drlvm.tests.regression.h1654;

import junit.framework.TestCase;
import java.security.*;

public class H1654Test extends TestCase {

    public void test1() {
        ProtectionDomain pd = H1654Test.class.getProtectionDomain();
        System.out.println(pd.getPermissions().toString());
        boolean permissionGranted = pd.getPermissions().toString().indexOf("java.lang.RuntimePermission exitVM") != -1;
        assertTrue("permission was not granted", permissionGranted);
    }

    public void test2() {
        ProtectionDomain pd = H1654Test.class.getProtectionDomain();
        assertTrue("pd does not imply the exitVM permission ", pd.implies(new RuntimePermission("exitVM")));
    }
}
