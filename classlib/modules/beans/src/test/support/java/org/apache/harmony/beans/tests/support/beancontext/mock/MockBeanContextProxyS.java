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

import java.beans.beancontext.BeanContextChild;
import java.beans.beancontext.BeanContextProxy;
import java.io.Serializable;

/**
 * Mock of BeanContextProxy
 */
public class MockBeanContextProxyS implements BeanContextProxy, Serializable {

    private static final long serialVersionUID = 1003496111741970301L;

    private String id;

    private BeanContextChild bcc;

    public MockBeanContextProxyS(String id, BeanContextChild bcc) {
        this.id = id;
        this.bcc = bcc;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MockBeanContextProxyS) {
            MockBeanContextProxyS other = (MockBeanContextProxyS) o;
            return id.equals(other.id) && bcc.equals(other.bcc);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode() + bcc.hashCode();
    }

    public BeanContextChild getBeanContextProxy() {
        return bcc;
    }

}
