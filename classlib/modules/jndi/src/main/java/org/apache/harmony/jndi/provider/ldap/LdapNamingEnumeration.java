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

package org.apache.harmony.jndi.provider.ldap;

import java.util.Collection;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.naming.CommunicationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * TODO: dynamic load elements from server
 */
public class LdapNamingEnumeration<T> implements NamingEnumeration<T> {

    private LinkedList<T> values;

    private NamingException exception;

    /**
     * flag to indicate whether all element have been added
     */
    private boolean isFinished;

    /**
     * max time to wait next element in millisconds
     */
    private long timeout = DEFAULT_TIMEOUT;

    private static final long DEFAULT_TIMEOUT = 30000;

    /**
     * This constructor equals to
     * <code>LdapNamingEnumeration(Collection<T>, NamingException, true)</code>
     */
    public LdapNamingEnumeration(Collection<T> list, NamingException ex) {
        this(list, ex, true);
    }

    /**
     * <code>list</code> and <code>ex</code> both can be <code>null</code>,
     * <code>null</code> of <code>list</code> will be treated as empty List.
     * 
     * @param list
     *            elements added to the enumeration.
     * @param ex
     *            exception would be thrown when over iterate.
     * @param isFinished
     *            if all elements have been added.
     */
    public LdapNamingEnumeration(Collection<T> list, NamingException ex,
            boolean isFinished) {
        if (list == null) {
            values = new LinkedList<T>();
        } else {
            values = new LinkedList<T>(list);
        }

        exception = ex;
        this.isFinished = isFinished;
    }

    /**
     * release all relative resources, current implementation just set
     * enumeration values to <code>null</code> which indicate the enumeration
     * is closed.
     */
    public void close() {
        // no other resources need to release
        synchronized (values) {
            values.clear();
            values = null;
        }
    }

    public boolean hasMore() throws NamingException {
        // has been closed
        if (values == null) {
            return false;
        }

        if (!values.isEmpty()) {
            return true;
        }

        synchronized (values) {
            if (values.isEmpty() && !isFinished) {
                waitMoreElement();
                if (!values.isEmpty()) {
                    return true;
                }
            }
        }

        close();

        if (exception != null) {
            throw exception;
        }

        if (!isFinished) {
            // ldap.31=Read LDAP response message time out
            throw new CommunicationException(Messages.getString("ldap.31")); //$NON-NLS-1$
        }

        return false;
    }

    /**
     * Retrieves the next element. <code>NoSuchElementException</code> will be
     * thrown, if there is no other elements or <code>close()</code> has been
     * invoked.
     */
    public T next() throws NamingException {
        if (values == null || (values.isEmpty() && isFinished)) {
            throw new NoSuchElementException();
        }

        synchronized (values) {
            if (values.isEmpty() && !isFinished) {
                waitMoreElement();
                // wait timeout
                if (values.isEmpty() && !isFinished) {
                    if (exception != null) {
                        throw exception;
                    }
                    // ldap.31=Read LDAP response message time out
                    throw new CommunicationException(Messages
                            .getString("ldap.31")); //$NON-NLS-1$
                } else if (values.isEmpty()) {
                    throw new NoSuchElementException();
                }
            }
            return values.poll();
        }
    }

    public boolean hasMoreElements() {
        if (values == null) {
            return false;
        }

        if (!values.isEmpty()) {
            return true;
        }

        synchronized (values) {
            if (values.isEmpty() && !isFinished) {
                waitMoreElement();
                if (!values.isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    public T nextElement() {
        if (values == null || (values.isEmpty() && isFinished)) {
            throw new NoSuchElementException();
        }

        synchronized (values) {
            if (values.isEmpty() && !isFinished) {
                waitMoreElement();
                if (values.isEmpty()) {
                    throw new NoSuchElementException();
                }
            }
            return values.poll();
        }
    }

    private void waitMoreElement() {
        try {
            values.wait(timeout);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    void setException(NamingException exception) {
        this.exception = exception;
    }

    void add(T pair, boolean isFinished) {
        if (values == null) {
            return;
        }

        synchronized (values) {
            values.add(pair);
            if (isFinished) {
                this.isFinished = true;
            }
            values.notifyAll();
        }
    }

    void add(Collection<T> list, boolean isFinished) {
        if (values == null) {
            return;
        }

        synchronized (values) {
            values.addAll(list);
            if (isFinished) {
                this.isFinished = true;
            }
            values.notifyAll();
        }
    }

    boolean isFinished() {
        return isFinished;
    }

    void setFinished() {
        synchronized (values) {
            isFinished = true;
            values.notifyAll();
        }
    }
}
