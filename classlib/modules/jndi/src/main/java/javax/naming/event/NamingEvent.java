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
import javax.naming.Binding;

/**
 * An event from a directory or naming service, for passing to a listener.
 * <p>
 * The source of the event is always the <code>EventContext</code> that the
 * listener registered with. Names in the <code>NamingEvent</code> object are
 * all relative to this context.
 * </p>
 * <p>
 * Note the discussion about threads and synchronization in the description for
 * this package.
 * </p>
 */
public class NamingEvent extends EventObject {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    private static final long serialVersionUID = 0x9d18b00289d22f45L;

    /**
     * A <code>NamingEvent</code> type constant, indicating that an object was
     * added.
     */
    public static final int OBJECT_ADDED = 0;

    /**
     * A <code>NamingEvent</code> type constant, indicating that an object was
     * changed.
     */
    public static final int OBJECT_CHANGED = 3;

    /**
     * A <code>NamingEvent</code> type constant, indicating that an object was
     * removed.
     */
    public static final int OBJECT_REMOVED = 1;

    /**
     * A <code>NamingEvent</code> type constant, indicating that an object was
     * renamed.
     */
    public static final int OBJECT_RENAMED = 2;

    /**
     * Some information about the event, whose format is specified by the
     * service provider.
     * 
     * @serial
     */
    protected Object changeInfo;

    /**
     * The binding after the event.
     * 
     * @serial
     */
    protected Binding newBinding;

    /**
     * The binding before the event.
     * 
     * @serial
     */
    protected Binding oldBinding;

    /**
     * The type of this event. Its value is one of the constant event types
     * above.
     * 
     * @serial
     */
    protected int type;

    // the context that generated this event
    private transient EventContext eventContext;

    /**
     * 
     * Constructs an <code>NamingEvent</code> with all parameters.
     * 
     * @param eventContext
     *            the context that generated this event. It is the originator of
     *            this event and cannot be null.
     * @param type
     *            the constant value that specifies the type of event
     * @param newBinding
     *            binding after the event. <code>newBinding</code> might be
     *            null depending on the value of the <code>type</code>
     *            parameter as follows:
     *            <ul>
     *            <li> <code>OBJECT_ADDED</code> - <code>newBinding</code>
     *            cannot be null </li>
     *            <li> <code>OBJECT_CHANGED</code> - <code>newBinding</code>
     *            cannot be null </li>
     *            <li> <code>OBJECT_REMOVED</code> - <code>newBinding</code>
     *            can be null </li>
     *            <li> <code>OBJECT_RENAMED</code> - <code>newBinding</code>
     *            can be null </li>
     *            </ul>
     *            The names are relative to the <code>eventContext</code>
     * @param oldBinding
     *            the binding before the event. <code>oldBinding</code> might
     *            be null depending on the value of the <code>type</code>
     *            parameter as follows:
     *            <ul>
     *            <li> <code>OBJECT_ADDED</code> - <code>oldBinding</code>
     *            can be null </li>
     *            <li> <code>OBJECT_CHANGED</code> - <code>oldBinding</code>
     *            cannot be null </li>
     *            <li> <code>OBJECT_REMOVED</code> - <code>oldBinding</code>
     *            cannot be null </li>
     *            <li> <code>OBJECT_RENAMED</code> - <code>oldBinding</code>
     *            can be null </li>
     *            </ul>
     *            The names are relative to the <code>eventContext</code>
     * @param changeInfo
     *            contain some information about the event and maybe null, the
     *            format of which is specified by the service provider.
     */
    public NamingEvent(EventContext eventContext, int type, Binding newBinding,
            Binding oldBinding, Object changeInfo) {
        super(eventContext);

        this.type = type;
        this.changeInfo = changeInfo;
        this.newBinding = newBinding;
        this.oldBinding = oldBinding;
        this.eventContext = eventContext;

    }

    /**
     * Calls a method to notify the listener of this event.
     * <p>
     * For <code>OBJECT_ADDED</code>, <code>OBJECT_REMOVED</code> or
     * <code>OBJECT_RENAMED</code> type events this method calls the
     * corresponding method in the <code>NamespaceChangedListener</code>
     * interface. For <code>OBJECT_CHANGED</code> type events this method
     * calls <code>objectChanged()</code> in the
     * <code>ObjectChangeListener</code> interface.
     * </p>
     * 
     * @param naminglistener
     *            the listener of this event
     */
    public void dispatch(NamingListener naminglistener) {
        switch (type) {
            case OBJECT_ADDED:
                ((NamespaceChangeListener) naminglistener).objectAdded(this);
                break;
            case OBJECT_REMOVED:
                ((NamespaceChangeListener) naminglistener).objectRemoved(this);
                break;
            case OBJECT_RENAMED:
                ((NamespaceChangeListener) naminglistener).objectRenamed(this);
                break;
            case OBJECT_CHANGED:
                ((ObjectChangeListener) naminglistener).objectChanged(this);
                break;
        }
    }

    /**
     * Gets the change information.
     * 
     * @return the change information object provided by the service provider,
     *         which may be null.
     */
    public Object getChangeInfo() {
        return changeInfo;
    }

    /**
     * Gets the <code>EventContext</code> that generated this event.
     * 
     * @return the <code>EventContext</code> that generated this event.
     */
    public EventContext getEventContext() {
        return eventContext;
    }

    /**
     * Gets the binding after this event.
     * <p>
     * If it exists and is inside the scope that was specified when the listener
     * was registered using <code>EventContext.addNamingListener</code>.
     * Returns null otherwise. Therefore for an <code>OBJECT_RENAMED</code>
     * event, the return value will be non-null if the new name places the
     * binding within the scope for the listener.
     * </p>
     * 
     * @return the binding after this event
     */
    public Binding getNewBinding() {
        return newBinding;
    }

    /**
     * Gets the binding before this event.
     * <p>
     * If it existed and was inside the scope that was specified when the
     * listener was registered using <code>EventContext.addNamingListener</code>.
     * Returns null otherwise. Therefore for an <code>OBJECT_RENAMED</code>
     * event, the return value will be non-null if the old name placed the
     * binding within the scope for the listener.
     * </p>
     * 
     * @return the binding before this event
     */
    public Binding getOldBinding() {
        return oldBinding;
    }

    /**
     * Gets the type of the event.
     * <p>
     * The return value is constrained to a choice from:
     * <code>OBJECT_ADDED</code>, <code>OBJECT_REMOVED</code>,
     * <code>OBJECT_RENAMED</code>, <code>OBJECT_CHANGED</code>.
     * </p>
     * 
     * @return the type of the event
     */
    public int getType() {
        return type;
    }

}
