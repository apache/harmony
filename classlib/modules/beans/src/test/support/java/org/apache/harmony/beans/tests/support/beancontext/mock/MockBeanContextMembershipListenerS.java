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

import java.beans.beancontext.BeanContextMembershipEvent;
import java.beans.beancontext.BeanContextMembershipListener;
import java.io.Serializable;

/**
 * Mock of BeanContextMembershipListener
 */
public class MockBeanContextMembershipListenerS implements BeanContextMembershipListener,
        Serializable {

    private static final long serialVersionUID = -7929429772961643840L;

    public String id;

    public boolean lastEventAdd = false;

    public boolean lastEventRemove = false;

    public BeanContextMembershipEvent lastEvent;

    public MockBeanContextMembershipListenerS(String id) {
        this.id = id;
    }

    public void clearLastEvent() {
        lastEventAdd = false;
        lastEventRemove = false;
        lastEvent = null;
    }

    public void childrenAdded(BeanContextMembershipEvent bcme) {
        lastEventAdd = true;
        lastEvent = bcme;
    }

    public void childrenRemoved(BeanContextMembershipEvent bcme) {
        lastEventRemove = true;
        lastEvent = bcme;
    }

}
