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
* @author Maxim V. Makarov
*/

package javax.security.auth;

import junit.framework.TestCase;

/**
 * Tests AuthPermission class
 */
public class AuthPermissionTest extends TestCase {

    private AuthPermission ap;
    private AuthPermission ap1;

   
    @Override
    protected void setUp() throws Exception {
        ap = new AuthPermission("name");
        ap1 = new AuthPermission("createLoginContext");
    }

    /**
     * Constructor for AuthPermissionTest.
     * 
     * @param name
     */
    public AuthPermissionTest(String name) {
        super(name);
    }
    
    /**
     * The target name "createLoginContext" is deprecated
     * and it should be replaced by "createLoginContext.*"   
     */
    public void testConstructor_01() {
        assertEquals("createLoginContext.*",ap1.getName());
    }
    
    /**
     * Checks that target name is correct 
     */
    public void testConstructor_02() {
        assertEquals("name",ap.getName());
    }
    
    /**
     * Target name should not be null
     */
    public void testConstructor_03() {
      try {  
        ap = new AuthPermission(null);
        fail("no expected NPE");
      } catch (NullPointerException e) {}
    }
    
    /**
     * Action should be ignored 
     */
    public void testConstructor_04() {
        try {  
          ap = new AuthPermission("name", null);
          ap = new AuthPermission("name", "read");
        } catch (NullPointerException e) {
            fail("action is not ignored");    
        }
      }
    
    /**
     * the target name "createLoginContext.{name}" is correct 
     */
    public void testConstructor_05() {
        ap1 = new AuthPermission("createLoginContext.name");
        assertEquals("createLoginContext.name",ap1.getName());
    }

}
