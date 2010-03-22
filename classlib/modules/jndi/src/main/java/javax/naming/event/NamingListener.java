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

package javax.naming.event;

import java.util.EventListener;

/**
 * This is a root listener interface that provides a method needed by all its
 * subinterfaces.
 * <p>
 * The method is <code>namingExceptionThrown</code>, which is required for
 * notification of problems when registering a listener, or problems when
 * getting information to send an event to a listener. When a listener is
 * notified of a <code>NamingExceptionEvent</code> it is automatically
 * deregistered.
 * </p>
 */
public interface NamingListener extends EventListener {

    /**
     * This method is called by a naming or directory service provider when a
     * naming exception occurs whilst the service provider is trying to register
     * or prepare an event notification for the listener.
     * 
     * @param namingExceptionEvent
     *            the event notification
     */
    void namingExceptionThrown(NamingExceptionEvent namingExceptionEvent);

}
