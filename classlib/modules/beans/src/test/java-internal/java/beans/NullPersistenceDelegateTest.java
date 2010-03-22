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

package java.beans;

import junit.framework.TestCase;

/**
 * Test the internal class java.beans.NullPersistenceDelegate.
 */
public class NullPersistenceDelegateTest extends TestCase {
    
    private PersistenceDelegate pd = null;
    
    private Encoder enc = null;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        enc = new Encoder();
        pd = enc.getPersistenceDelegate(null);
    }
    
    public void test_instantiate_LObject_LEncoder() throws Exception {
        Expression exp = pd.instantiate(null, enc);
        assertNull(exp);
        
        exp = pd.instantiate(new Object(), enc);
        assertNull(exp);
        
        exp = pd.instantiate(null, null);
        assertNull(exp);
        
        exp = pd.instantiate(new Object(), null);
        assertNull(exp);
    }
    
    public void test_mutatesTo_LObject_Object() {
        assertFalse(pd.mutatesTo(null, null));
        assertFalse(pd.mutatesTo(null, new Object()));
        assertFalse(pd.mutatesTo(new Object(), null));

        Object o = new Object();
        assertTrue(pd.mutatesTo(o, o));
        assertTrue(pd.mutatesTo(new Object(), new Object()));
        assertFalse(pd.mutatesTo(new Object(), new Integer(1)));
    }
    
    public void test_writeObject_LObject_LEncoder() {
        pd.writeObject(null, null);
    }
    
    public void test_initialize() {
        pd.initialize(null, null, null, null);
    }
    
}
