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

package org.apache.harmony.beans.tests.java.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeListenerProxy;

import junit.framework.TestCase;

/**
 * Unit test for VetoableChangeListenerProxy
 */
public class VetoableChangeListenerProxyTest extends TestCase {
    VetoableChangeListenerProxy proxy;

    VetoableChangeListener listener = new MockVetoableChangeListener();

    String name = "mock";

    private static PropertyChangeEvent event = null;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        proxy = new VetoableChangeListenerProxy(name, listener);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testVetoableChangeListenerProxy() throws PropertyVetoException {
        proxy = new VetoableChangeListenerProxy(null, listener);
        assertSame(listener, proxy.getListener());
        assertNull(proxy.getPropertyName());
        PropertyChangeEvent newevent = new PropertyChangeEvent(new Object(),
                "name", new Object(), new Object());
        proxy.vetoableChange(newevent);
        assertSame(newevent, event);
        proxy = new VetoableChangeListenerProxy(name, null);
        assertSame(name, proxy.getPropertyName());
        assertNull(proxy.getListener());
        try {
            proxy.vetoableChange(new PropertyChangeEvent(new Object(), "name",
                    new Object(), new Object()));
            fail("should null pointer");
        } catch (NullPointerException e) {
        }

        proxy = new VetoableChangeListenerProxy(name, listener);
        assertSame(listener, proxy.getListener());
        assertSame(name, proxy.getPropertyName());
        newevent = new PropertyChangeEvent(new Object(), "name", new Object(),
                new Object());
        assertSame(name, proxy.getPropertyName());
        proxy.vetoableChange(newevent);
        assertSame(newevent, event);
    }

    public void testVetoableChange() throws PropertyVetoException {
        PropertyChangeEvent newevent = new PropertyChangeEvent(new Object(),
                "exception", new Object(), new Object());
        try {
            proxy.vetoableChange(newevent);
            fail("should throw exception");
        } catch (PropertyVetoException e) {
        }
        proxy.vetoableChange(null);
        assertNull(event);
    }

    public static class MockVetoableChangeListener implements
            VetoableChangeListener {

        public void vetoableChange(PropertyChangeEvent newevent)
                throws PropertyVetoException {
            event = newevent;
            if (event != null && event.getPropertyName().equals("exception")) {
                throw new PropertyVetoException("", newevent);
            }
        }

    }

}
