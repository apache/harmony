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

package org.apache.harmony.beans.tests.support.beancontext.mock;

import java.beans.beancontext.BeanContextServiceAvailableEvent;
import java.beans.beancontext.BeanContextServiceRevokedEvent;
import java.beans.beancontext.BeanContextServicesListener;

/**
 * Mock of BeanContextServicesListener
 */
public class MockBeanContextServicesListener implements
        BeanContextServicesListener {

    public BeanContextServiceAvailableEvent lastAvailableEvent;

    public BeanContextServiceRevokedEvent lastRevokedEvent;

    public void clearLastEvent() {
        lastAvailableEvent = null;
        lastRevokedEvent = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.beancontext.BeanContextServicesListener#serviceAvailable(java.beans.beancontext.BeanContextServiceAvailableEvent)
     */
    public void serviceAvailable(BeanContextServiceAvailableEvent bcsae) {
        lastAvailableEvent = bcsae;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.beancontext.BeanContextServiceRevokedListener#serviceRevoked(java.beans.beancontext.BeanContextServiceRevokedEvent)
     */
    public void serviceRevoked(BeanContextServiceRevokedEvent bcsre) {
        lastRevokedEvent = bcsre;
    }

}
