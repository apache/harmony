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

package org.apache.harmony.beans.tests.java.beans.beancontext;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.beancontext.BeanContextSupport;
import java.beans.beancontext.BeanContextMembershipListener;
import java.beans.beancontext.BeanContextMembershipEvent;
import java.beans.beancontext.BeanContextChildSupport;
import java.util.Locale;

import junit.framework.TestCase;

public class BeanContextSupport2Test extends TestCase {
    
    //Regression for HARMONY-3774.
    public void test_setLocale_null() throws Exception
    {
        Locale locale = Locale.FRANCE;
        BeanContextSupport beanContextSupport = new BeanContextSupport(null, locale);
        assertEquals(Locale.FRANCE, beanContextSupport.getLocale());
        MyPropertyChangeListener myPropertyChangeListener = new MyPropertyChangeListener();
        beanContextSupport.addPropertyChangeListener("locale", myPropertyChangeListener);
        beanContextSupport.setLocale(null);
        assertEquals(Locale.FRANCE, beanContextSupport.getLocale());
        assertFalse(myPropertyChangeListener.changed);        
    }

    /**
     * Regression test for HARMONY-4011
     */
    public void test4011() {
        BeanContextSupport context = new BeanContextSupport();
        final int[] k = { 0 };
        BeanContextMembershipListener listener =
                new BeanContextMembershipListener() {
                    
            public void childrenAdded(BeanContextMembershipEvent bcme) {
                k[0]++;
            }

            public void childrenRemoved(BeanContextMembershipEvent bcme) {}
        };

        // add listener
        context.addBeanContextMembershipListener(listener);
        context.add(new BeanContextChildSupport());
        assertEquals(1, k[0]);
        
        // add the same listener onse again
        context.addBeanContextMembershipListener(listener);
        context.add(new BeanContextChildSupport());
        assertEquals(2, k[0]);
    }
    
    private class MyPropertyChangeListener implements PropertyChangeListener {
        public boolean changed = false;

        public void propertyChange(PropertyChangeEvent event) {
            changed = true;
        }

        public void reset() {
            changed = false;
        }

    }

}
