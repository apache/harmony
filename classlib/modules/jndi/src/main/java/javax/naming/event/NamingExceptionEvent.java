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

import java.util.EventObject;

import javax.naming.NamingException;

/**
 * An event object contains a <code>NamingException</code>.
 */
public class NamingExceptionEvent extends EventObject {

    private static final long serialVersionUID = 0xbc4f019fab3b5a30L;

    /**
     * Associated exception of this event
     * 
     * @serial
     */
    private NamingException exception;

    /**
     * Constructs a <code>NamingExceptionEvent</code> instance with a
     * <code>EventContext</code> and a <code>NamingException</code>.
     * 
     * @param eventContext
     *            context that generated this event. It is the originator of
     *            this event and cannot be null.
     * @param namingException
     *            the associated exception and cannot be null.
     */
    public NamingExceptionEvent(EventContext eventContext,
            NamingException namingException) {
        super(eventContext);
        this.exception = namingException;
    }

    /**
     * Calls a method to notify the listener that a naming exception has been
     * thrown.
     * 
     * @param naminglistener
     *            the listener to be notified
     */
    public void dispatch(NamingListener naminglistener) {
        naminglistener.namingExceptionThrown(this);
    }

    /**
     * Gets the source of the event.
     * 
     * @return the source of the event
     */
    public EventContext getEventContext() {
        return (EventContext) getSource();
    }

    /**
     * Gets the associated <code>NamingException</code>.
     * 
     * @return the associated <code>NamingException</code>
     */
    public NamingException getException() {
        return exception;
    }

}
