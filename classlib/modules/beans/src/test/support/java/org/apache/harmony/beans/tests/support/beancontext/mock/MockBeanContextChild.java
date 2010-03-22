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

/**
 * Mock of BeanContextChild
 */
public class MockBeanContextChild implements
        java.beans.beancontext.BeanContextChild {

    private BeanContext ctx;

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.beancontext.BeanContextChild#setBeanContext(java.beans.beancontext.BeanContext)
     */
    public void setBeanContext(BeanContext bc) throws PropertyVetoException {
        ctx = bc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.beancontext.BeanContextChild#getBeanContext()
     */
    public BeanContext getBeanContext() {
        return ctx;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.beancontext.BeanContextChild#addPropertyChangeListener(java.lang.String,
     *      java.beans.PropertyChangeListener)
     */
    public void addPropertyChangeListener(String name,
            PropertyChangeListener pcl) {
        // Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.beancontext.BeanContextChild#removePropertyChangeListener(java.lang.String,
     *      java.beans.PropertyChangeListener)
     */
    public void removePropertyChangeListener(String name,
            PropertyChangeListener pcl) {
        // Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.beancontext.BeanContextChild#addVetoableChangeListener(java.lang.String,
     *      java.beans.VetoableChangeListener)
     */
    public void addVetoableChangeListener(String name,
            VetoableChangeListener vcl) {
        // Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.beancontext.BeanContextChild#removeVetoableChangeListener(java.lang.String,
     *      java.beans.VetoableChangeListener)
     */
    public void removeVetoableChangeListener(String name,
            VetoableChangeListener vcl) {
        // Auto-generated method stub

    }

}
