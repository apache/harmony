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

package org.apache.harmony.jndi.tests.javax.naming.ldap;

import javax.naming.ldap.BasicControl;

import junit.framework.TestCase;

public class BasicControlTest extends TestCase {

    /**
     * <p>
     * Test method for 'javax.naming.ldap.BasicControl.BasicControl'
     * </p>
     */
    public void testBasicControl() {
        // no exceptions expected
        new BasicControl(null);
        new BasicControl("");
        new BasicControl("1.2.3.333");
        new BasicControl("", true, null);
        new BasicControl("", false, new byte[0]);
        new BasicControl(null, false, null);
    }

    /**
     * Test method for {@link javax.naming.ldap.BasicControl#isCritical()}.
     */
    public void testIsCritical() {
        BasicControl bc = new BasicControl("fixture");
        assertFalse(bc.isCritical());

        bc = new BasicControl(null, false, null);
        assertFalse(bc.isCritical());

        bc = new BasicControl(null, true, null);
        assertTrue(bc.isCritical());
    }

    /**
     * @tests javax.naming.ldap.BasicControl#getID()
     */
    public void testGetID() {
        String ID = "somestring";
        assertSame(ID, new BasicControl(ID).getID());

        assertNull(new BasicControl(null).getID());

        assertNull(new BasicControl(null, false, new byte[1]).getID());
    }

    /**
     * <p>
     * Test method for 'javax.naming.ldap.BasicControl.getEncodedValue()'
     * </p>
     * <p>
     * Here we are testing the return method of the encoded value of
     * BasicControl. In this case we send an encoded value null.
     * </p>
     * <p>
     * The expected result is a null encoded value.
     * </p>
     */
    public void testGetEncodedValue() {
        assertNull(new BasicControl("control", true, null).getEncodedValue());

        // spec says the byte[] is NOT copied
        byte[] test = new byte[15];
        BasicControl bc = new BasicControl("control", true, test);
        assertSame(test, bc.getEncodedValue());
    }

}
