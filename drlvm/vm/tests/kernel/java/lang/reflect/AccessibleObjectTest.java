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

/**
 * @author Serguei S.Zapreyev
 */

package java.lang.reflect;

import junit.framework.TestCase;

/*
 * Created on 01.28.2006
 */

@SuppressWarnings(value={"all"}) public class AccessibleObjectTest extends TestCase {

    /**
     *  
     */
    public void test_isAccessible_V() {
        class X {
            private int fld;

            private X() {
                return;
            }
        }
        try {
            Field f1 = X.class.getDeclaredField("fld");
            Constructor m1 = X.class
                    .getDeclaredConstructor(new Class[] { java.lang.reflect.AccessibleObjectTest.class });
            assertTrue("Error1", !f1.isAccessible());
            assertTrue("Error2", !m1.isAccessible());
        } catch (Exception e) {
            fail("Error3: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_setAccessible_Acc_B() {
        class X {
            private int fld;

            private X() {
                return;
            }
        }
        try {
            Field f1 = X.class.getDeclaredField("fld");
            Constructor m1 = X.class
                    .getDeclaredConstructor(new Class[] { java.lang.reflect.AccessibleObjectTest.class });
            assertTrue("Error1", !f1.isAccessible() && !m1.isAccessible());
            AccessibleObject[] aa = new AccessibleObject[] { f1, m1 };
            AccessibleObject.setAccessible(aa, true);
            assertTrue("Error2", f1.isAccessible() && m1.isAccessible());
        } catch (Exception e) {
            fail("Error3: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_setAccessible_B() {
        class X {
            private int fld;

            private X() {
                return;
            }
        }
        try {
            Field f1 = X.class.getDeclaredField("fld");
            f1.setAccessible(true);
            Constructor m1 = X.class
                    .getDeclaredConstructor(new Class[] { java.lang.reflect.AccessibleObjectTest.class });
            m1.setAccessible(true);
            assertTrue("Error1", f1.isAccessible());
            assertTrue("Error2", m1.isAccessible());
        } catch (Exception e) {
            fail("Error3: " + e.toString());
        }
    }
}