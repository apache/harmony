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

package org.apache.harmony.jndi.tests.javax.naming.ldap;

import javax.naming.event.NamingExceptionEvent;
import javax.naming.ldap.UnsolicitedNotificationEvent;
import javax.naming.ldap.UnsolicitedNotificationListener;

public class MockUnsolicitedNotificationListener implements
		UnsolicitedNotificationListener {

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.ldap.UnsolicitedNotificationListener#notificationReceived(javax.naming.ldap.UnsolicitedNotificationEvent)
	 */
	public void notificationReceived(UnsolicitedNotificationEvent e) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.event.NamingListener#namingExceptionThrown(javax.naming.event.NamingExceptionEvent)
	 */
	public void namingExceptionThrown(NamingExceptionEvent namingexceptionevent) {

	}

}
