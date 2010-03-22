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

import java.beans.beancontext.BeanContextServiceProvider;
import java.beans.beancontext.BeanContextServices;
import java.beans.beancontext.BeanContextServicesSupport;
import java.util.Iterator;

import junit.framework.TestCase;

/*
 * Regression test for HARMONY-1369
 */
public class BeanContextServicesSupport2Test extends TestCase {

    public void test() {
        BeanContextServiceProvider bcsp = new BCSP();
        BCSS serviceSupport = new BCSS(new BeanContextServicesSupport());
        assertTrue("Expected first addService to return true", serviceSupport
                .addService(Boolean.TYPE, bcsp, false));
        assertFalse("Expected second addService to return false", serviceSupport
                .addService(Boolean.TYPE, bcsp, false));
    }

    class BCSP implements BeanContextServiceProvider {
        public Iterator getCurrentServiceSelectors(BeanContextServices p0,
                Class p1) {
            return null;
        }

        public void releaseService(BeanContextServices p0, Object p1, Object p2) {
            return;
        }

        public Object getService(BeanContextServices p0, Object p1, Class p2,
                Object p3) {
            return null;
        }
    }

    class BCSS extends BeanContextServicesSupport {
        public BCSS() {
            super();
        }

        public BCSS(BeanContextServicesSupport peer) {
            super(peer);
        }

        public boolean addService(Class serviceClass,
                BeanContextServiceProvider bcsp, boolean firevent) {
            return super.addService(serviceClass, bcsp, firevent);
        }
    }

}
