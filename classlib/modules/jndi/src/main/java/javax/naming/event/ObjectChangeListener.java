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
 * The listener interface to get notification of object change events.
 * <p>
 * These object change events include naming events with type
 * <code>OBJECT_CHANGED</code>. These events could mean that a bound object
 * has had its attributes changed somehow, or has been replaced altogether. The
 * listener can work out what has changed by querying the
 * <code>NamingEvent</code> object that is passed to the
 * <code>objectChanged</code> notification method.
 */
public interface ObjectChangeListener extends NamingListener {

    /**
     * This method is called by a service provider to notify a listener that a
     * bound object has changed in some way.
     * <p>
     * The changes can be deduced by querying <code>namingEvent</code>,
     * especially <code>NamingEvent.getNewBinding()</code> and
     * <code>NamingEvent.getOldBinding()</code>.
     * </p>
     * 
     * @param namingEvent
     *            the event notification
     */
    void objectChanged(NamingEvent namingEvent);

}
