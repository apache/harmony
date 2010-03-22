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
 * @author Evgueni V. Brevnov
 */

package java.lang.reflect;

import junit.framework.TestCase;
import org.apache.harmony.lang.ProtectedMethod;
import org.apache.harmony.lang.ProtectedSuccessor;

public class ProtectedAccessCheck extends TestCase {
    
    public void test1() {
        B b = new B();
        new A().new Inner1().invoke(b);
    }
}

class A extends ProtectedSuccessor {
    
    class Inner1 {
        public void invoke(Object o) {
            assertEquals(0, status);
            ((B)o).protectedMethod();
            assertEquals(0, status);
            try {
                Method m = ProtectedMethod.class.getDeclaredMethod(
                        "protectedMethod", new Class[] {});
                m.invoke(o, (Object[]) null);
                assertEquals(0, status);                
                assertEquals(20, ((B)o).status);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private void assertEquals(int i1, int i2) {
        if (i1 != i2) throw new RuntimeException("Assertion failed: " + i1 +
                                          " is not equal to " + i2);
    }
}

class B extends A {
    
}