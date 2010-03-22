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

package javax.naming.ldap;

import java.util.EventObject;

/**
 * This event is fired when an LDAP server sends an unsolicited notification.
 * (See RFC2251).
 */
public class UnsolicitedNotificationEvent extends EventObject {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    private static final long serialVersionUID = -2382603380799883705L;

    /**
     * The specific notification.
     * 
     * @serial
     */
    private UnsolicitedNotification notice;

    /**
     * Constructs an <code>UnsolicitedNotificationEvent</code> instance using
     * the supplied <code>UnsolicitedNotification</code> instance.
     * 
     * @param o
     *            the source of the event which cannot be null
     * @param un
     *            the <code>UnsolicitedNotification</code> instance which
     *            cannot be null
     */
    public UnsolicitedNotificationEvent(Object o, UnsolicitedNotification un) {
        super(o);
        this.notice = un;
    }

    /**
     * Returns the <code>UnsolicitedNotification</code> instance associated
     * with this event.
     * 
     * @return the <code>UnsolicitedNotification</code> instance associated
     *         with this event
     */
    public UnsolicitedNotification getNotification() {
        return notice;
    }

    /**
     * Uses this event to trigger a notification received on the supplied
     * listener.
     * 
     * @param unl
     *            the listener to dispatch this event to. It cannot be null.
     */
    public void dispatch(UnsolicitedNotificationListener unl) {
        unl.notificationReceived(this);
    }

}
