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

package java.beans;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import junit.framework.TestCase;

public class ProxyPersistenceDelegateTest extends TestCase {

    private ProxyPersistenceDelegate pd = null;

    protected void setUp() throws Exception {
        super.setUp();
        pd = new ProxyPersistenceDelegate();
    }

    protected void tearDown() throws Exception {
        pd = null;
        super.tearDown();
    }

    /*
     * test mutatesTo method
     */
    public void test_MutatesTo() {
        // Regression for Harmony-4022
        Object proxy1 = Proxy.newProxyInstance(
                this.getClass().getClassLoader(), new Class[] {
                        ITestReturnObject.class, ITestReturnString.class },
                new TestProxyHandler(new TestProxyImpl()));
        Object proxy2 = Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[] { ITestReturnObject.class }, new TestProxyHandler(
                        new TestProxyImpl()));

        assertFalse(pd.mutatesTo(null, null));
        assertFalse(pd.mutatesTo(new MockAObject(), new MockAObject()));
        assertFalse(pd.mutatesTo(new MockAObject(), new MockBObject()));
        assertFalse(pd.mutatesTo(proxy1, null));
        assertFalse(pd.mutatesTo(proxy1, proxy2));
        assertTrue(pd.mutatesTo(proxy1, proxy1));
    }

    public class MockAObject {

    }

    public class MockBObject {

    }

    public static interface ITestReturnObject {
        Object f();
    }

    public static interface ITestReturnString {
        String f();
    }

    public static class TestProxyImpl implements ITestReturnObject,
            ITestReturnString {
        public String f() {
            return null;
        }
    }

    public static class TestProxyHandler implements InvocationHandler {
        private Object proxied;

        public TestProxyHandler(Object object) {
            proxied = object;
        }

        public Object invoke(Object object, Method method, Object[] args)
                throws Throwable {
            return method.invoke(proxied, args);
        }
    }

}
