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

/**
 * The listener interface to get notification of namespace change events.
 * <p>
 * These events include naming events with event type <code>OBJECT_ADDED</code>,
 * <code>OBJECT_RENAMED</code>, or <code>OBJECT_REMOVED</code>. A service
 * provider will call one of these interface methods to notify a listener of an
 * event, passing in a <code>NamingEvent</code> parameter. This
 * <code>NamingEvent</code> provides methods to get various bits information
 * about the event.
 * </p>
 */
public interface NamespaceChangeListener extends NamingListener {

    /**
     * Called by a service provider when there is a new binding.
     * 
     * @param namingevent
     *            the event notification
     */
    void objectAdded(NamingEvent namingevent);

    /**
     * Called by a service provider when a binding is removed.
     * 
     * @param namingevent
     *            the event notification
     */
    void objectRemoved(NamingEvent namingevent);

    /**
     * Called by a service provider when a binding is renamed.
     * 
     * @param namingevent
     *            the event notification
     */
    void objectRenamed(NamingEvent namingevent);

}
