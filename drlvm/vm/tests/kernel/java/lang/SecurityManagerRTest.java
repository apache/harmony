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
 * @author Vladimir Ivanov
 */

package java.lang;

import org.apache.harmony.test.ReversibleSecurityManager;

import junit.framework.TestCase;

public class SecurityManagerRTest extends TestCase {

    public void testSetSM() {
        SecurityManager sm = System.getSecurityManager();
        try {
            System.setSecurityManager(new ReversibleSecurityManager());
            new Test().m();
        } finally {
            System.setSecurityManager(sm);
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SecurityManagerRTest.class);
    }

}

class Test {
    void m() {
        new test1();
    }

    class test1 {
    }
}
