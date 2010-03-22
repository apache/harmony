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

import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.beancontext.BeanContext;
import java.beans.beancontext.BeanContextChild;
import java.io.Serializable;

/**
 * Mock of BeanContextChild
 */
public class MockBeanContextChildS implements BeanContextChild, Serializable {

    private static final long serialVersionUID = 8671685325002756158L;

    public String id;

    private BeanContext ctx;

    public MockBeanContextChildS(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MockBeanContextChildS) {
            return id.equals(((MockBeanContextChildS) o).id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public void setBeanContext(BeanContext bc) throws PropertyVetoException {
        ctx = bc;
    }

    public BeanContext getBeanContext() {
        return ctx;
    }

    public void addPropertyChangeListener(String name, PropertyChangeListener pcl) {

    }

    public void removePropertyChangeListener(String name, PropertyChangeListener pcl) {

    }

    public void addVetoableChangeListener(String name, VetoableChangeListener vcl) {

    }

    public void removeVetoableChangeListener(String name, VetoableChangeListener vcl) {
    }

}
