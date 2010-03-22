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

import java.util.Arrays;
import java.util.Vector;

import javax.naming.NamingException;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.ldap.Control;
import javax.naming.ldap.UnsolicitedNotification;
import javax.naming.ldap.UnsolicitedNotificationEvent;
import javax.naming.ldap.UnsolicitedNotificationListener;

import junit.framework.TestCase;

public class UnsolicitedNotificationEventTest extends TestCase {

	@Override
    protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
    protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testConstructor_simple() {
		NamingException exception = new NamingException(
				"MockUnsolicitedNotification: naming exception");
		String[] referral = { "Red", "Blue", };
		UnsolicitedNotification notification = new MockUnsolicitedNotification(
				referral, exception);
		Object src = "source";
		UnsolicitedNotificationEvent event = new UnsolicitedNotificationEvent(
				src, notification);

		assertEquals(src, event.getSource());
		assertEquals(notification, event.getNotification());

		assertSame(notification, event.getNotification());
		assertSame(src, event.getSource());
	}

	public void testConstructor_src_null() {
		NamingException exception = new NamingException(
				"MockUnsolicitedNotification: naming exception");
		String[] referral = { "Red", "Blue", };
		UnsolicitedNotification notification = new MockUnsolicitedNotification(
				referral, exception);
		Object src = null;
		try {
			new UnsolicitedNotificationEvent(
					src, notification);
		} catch (IllegalArgumentException e) {
		}
	}

	public void testConstructor_notification_null() {
		Object src = "source";
		UnsolicitedNotificationEvent event = new UnsolicitedNotificationEvent(
				src, null);

		assertEquals(src, event.getSource());
		assertNull(event.getNotification());
	}

	public void testDispatch() {
		NamingException exception = new NamingException(
				"MockUnsolicitedNotification: naming exception");
		String[] referral = { "Red", "Blue", };
		UnsolicitedNotification notification = new MockUnsolicitedNotification(
				referral, exception);
		Object src = "source";
		UnsolicitedNotificationEvent event = new UnsolicitedNotificationEvent(
				src, notification);
		MockUnsolicitedNotificationListener listener = new MockUnsolicitedNotificationListener();
		event.dispatch(listener);
		assertTrue(listener.hasEvent(event));
	}

	class MockUnsolicitedNotification implements UnsolicitedNotification {
		/**
         * <p></p>
         */
        private static final long serialVersionUID = 1L;

        String[] referrals;

		NamingException exception;

		public MockUnsolicitedNotification(String[] referrals,
				NamingException exception) {
			this.referrals = new String[referrals.length];
			System.arraycopy(referrals, 0, this.referrals, 0, referrals.length);
			this.exception = exception;
		}

		public NamingException getException() {
			return this.exception;
		}

		public String[] getReferrals() {
			return this.referrals;
		}

		public String getID() {
			return null;
		}

		public byte[] getEncodedValue() {
			return null;
		}

		public Control[] getControls() throws NamingException {
			return null;
		}

		@Override
        public boolean equals(Object arg0) {
			if (arg0 instanceof MockUnsolicitedNotification) {
				MockUnsolicitedNotification a = (MockUnsolicitedNotification) arg0;
				return this.exception.equals(a.exception)
						&& Arrays.equals(this.referrals, a.referrals);
			}
			return false;
		}
	}

	class MockUnsolicitedNotificationListener implements
			UnsolicitedNotificationListener {
		Vector<UnsolicitedNotificationEvent> events;

		public MockUnsolicitedNotificationListener() {
			events = new Vector<UnsolicitedNotificationEvent>();
		}

		public void notificationReceived(UnsolicitedNotificationEvent e) {
			this.events.add(e);
		}

		public void namingExceptionThrown(
				NamingExceptionEvent namingexceptionevent) {
		}

		public boolean hasEvent(UnsolicitedNotificationEvent e) {
			return this.events.contains(e);
		}

	}
}
