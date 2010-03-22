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
package org.apache.harmony.jndi.tests.javax.naming.spi.mock.ldap;

import javax.naming.NamingException;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.ldap.Control;
import javax.naming.ldap.UnsolicitedNotification;
import javax.naming.ldap.UnsolicitedNotificationEvent;
import javax.naming.ldap.UnsolicitedNotificationListener;

/**
 * <p>Implementation of the interfaces UnsolicitedNotificationListener and UnsolicitedNotification. This class has the intention of
 * give us a notification or a listener to test another classes.</p> 
 *
 */
public class MockUnsolicitedNotification implements
		UnsolicitedNotificationListener, UnsolicitedNotification {
	/**
	 * @serialField Serial of the class.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * <p>This flag has the intention of give us a way to know if a notification was received.</p>
	 */
	private boolean flag;

	/**
	 * <p>Constructor method of this mock, here we call the inherit constructor and also we initialized the rest of fields.</p>
	 *
	 */
	public MockUnsolicitedNotification() {
		super();
		flag=false;//Flag to know if a the notification was received.
		
	}
	/**
	 * <p>Here we received the notification, so we set the flag true.</p>
	 */
	public void notificationReceived(UnsolicitedNotificationEvent arg0) {
		flag=true;

	}

	/**
	 * <p>Method to see if the notification was received.</p>
	 * @return The flag of the notification.
	 */
	public boolean getFlag(){
		return flag;
	}
	
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void namingExceptionThrown(NamingExceptionEvent arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * <p>Method not implemented yet.</p>
	 */
	public String[] getReferrals() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NamingException getException() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * <p>Method not implemented yet.</p>
	 */
	public String getID() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * <p>Method implemented for testing serialization on UnsolicitedNotificationEvent.</p>
	 */
	public long getIDSerial() {
		// TODO Auto-generated method stub
		return this.serialVersionUID;
	}

	/**
	 * <p>Method not implemented yet.</p>
	 */
	public byte[] getEncodedValue() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Control[] getControls() throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

}
