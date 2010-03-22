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
 * @author Hugo Beilis
 * @author Leonardo Soler
 * @author Gabriel Miretti
 * @version 1.0
 */
package org.apache.harmony.jndi.tests.javax.naming.ldap;

import javax.naming.ldap.UnsolicitedNotificationEvent;

import junit.framework.TestCase;

import org.apache.harmony.jndi.tests.javax.naming.spi.mock.ldap.MockUnsolicitedNotification;

public class TestUnsolicitedNotificationEvent extends TestCase {

	/**
	 * <p>Test method for 'javax.naming.ldap.UnsolicitedNotificationEvent.UnsolicitedNotificationEvent(Object, UnsolicitedNotification)'</p>
	 * <p>This is the constructor method that constructs a new instance of UnsolicitedNotificationEvent. In this case we are sending 
	 * two null arguments. This is not specified in the API.</p>
	 * <p>The expected result is an Illegal argument exception.</p>
	 */
	public void testUnsolicitedNotificationEvent001() {
        new UnsolicitedNotificationEvent(new Object(), null);

        try {
            new UnsolicitedNotificationEvent(null, null);
            fail("The arguments could not be null.");
        } catch (IllegalArgumentException e) {}
    }

	/**
	 * <p>Test method for 'javax.naming.ldap.UnsolicitedNotificationEvent.UnsolicitedNotificationEvent(Object, UnsolicitedNotification)'</p>
	 * <p>This is the constructor method that constructs a new instance of UnsolicitedNotificationEvent. In this case we are sending 
	 * one null arguments. This is not specified in the API.</p>
	 * <p>The expected result is an Illegal Argument exception.</p>
	 */
	public void testUnsolicitedNotificationEvent003() {
        try {
            MockUnsolicitedNotification u = new MockUnsolicitedNotification();
            new UnsolicitedNotificationEvent(null, u);
            fail("The arguments could not be null.");
        } catch (IllegalArgumentException e) {}
    }
	
	/**
	 * <p>Test method for 'javax.naming.ldap.UnsolicitedNotificationEvent.UnsolicitedNotificationEvent(Object, UnsolicitedNotification)'</p>
	 * <p>This is the constructor method that constructs a new instance of UnsolicitedNotificationEvent. In this case we are sending 
	 * one null arguments. This is not specified in the API.</p>
	 * <p>The expected result is an Illegal Argument exception.</p>
	 */
	public void testUnsolicitedNotificationEvent004() {
        MockUnsolicitedNotification u = new MockUnsolicitedNotification();
        new UnsolicitedNotificationEvent(new Object(), u);
    }

	/**
	 * <p>Test method for 'javax.naming.ldap.UnsolicitedNotificationEvent.getNotification()'</p>
	 * <p>Here we are testing if the method returns the unsolicited notification. In this case we create a notification
	 * with an object and a null notification as the parameters.</p>
	 * <p>The expected result is a not null notification.</p>
	 */
	public void testGetNotification001() {
        UnsolicitedNotificationEvent une = new UnsolicitedNotificationEvent(
                new Object(), null);

        assertNull(une.getNotification());
    }

	/**
	 * <p>Test method for 'javax.naming.ldap.UnsolicitedNotificationEvent.getNotification()'</p>
	 * <p>Here we are testing if the method returns the unsolicited notification. In this case we create a notification
	 * with an object and a null notification as the parameters.</p>
	 * <p>The expected result is a not null notification.</p>
	 */
	public void testGetNotification002() {
        Object x = new Object();
        MockUnsolicitedNotification u = new MockUnsolicitedNotification();
        UnsolicitedNotificationEvent une = new UnsolicitedNotificationEvent(x, u);
        assertEquals(u, une.getNotification());
    }
	
	/**
	 * <p>Test method for 'javax.naming.ldap.UnsolicitedNotificationEvent.dispatch(UnsolicitedNotificationListener)'</p>
	 * <p>Here this method invokes the notificationReceived() method on a listener using this event. In this case we are 
	 * sending as a parameter a null listener.</p>
	 * <p>The expected result is a null pointer exception.</p>
	 */
	public void testDispatch001() {
        Object x = new Object();
        UnsolicitedNotificationEvent une = new UnsolicitedNotificationEvent(x,
                new MockUnsolicitedNotification());
        try {
            une.dispatch(null);
            fail("Failed notification is null.");
        } catch (NullPointerException e) {}
    }
	
	/**
	 * <p>Test method for 'javax.naming.ldap.UnsolicitedNotificationEvent.dispatch(UnsolicitedNotificationListener)'</p>
	 * <p>Here this method invokes the notificationReceived() method on a listener using this event. In this case we are 
	 * sending as a parameter a non null listener.</p>
	 * <p>The expected result is a null pointer exception.</p>
	 */
	public void testDispatch002() {
        Object x = new Object();
        MockUnsolicitedNotification u = new MockUnsolicitedNotification();
        MockUnsolicitedNotification f = new MockUnsolicitedNotification();
        UnsolicitedNotificationEvent une = new UnsolicitedNotificationEvent(x, u);
        une.dispatch(f);
        assertTrue(f.getFlag());
    }
}
