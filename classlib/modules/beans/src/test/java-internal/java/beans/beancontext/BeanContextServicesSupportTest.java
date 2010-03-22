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
 * @author Sergey A. Krivenko
 */
package java.beans.beancontext;

import java.util.Locale;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test class for java.beans.beancontext.BeanContextServicesSupport.
 * <p>
 * 
 * @author Sergey A. Krivenko
 */

public class BeanContextServicesSupportTest extends TestCase {

    /** STANDARD BEGINNING * */

    /**
     * No arguments constructor to enable serialization.
     * <p>
     */
    public BeanContextServicesSupportTest() {
        super();
    }

    /**
     * Constructs this test case with the given name.
     * <p>
     * 
     * @param name -
     *            The name for this test case.
     *            <p>
     */
    public BeanContextServicesSupportTest(String name) {
        super(name);
    }

    /** TEST CONSTRUCTORS * */

    /**
     * Test constructor with BeanContextServices, Locale, boolean, boolean
     * parameters.
     * <p>
     * 
     * @see BeanContextServicesSupport#BeanContextServicesSupport(
     *      BeanContextServices, Locale, boolean, boolean)
     */
    public void testConstructorBeanContextServicesLocalebooleanboolean() {
        new BeanContextServicesSupport(null, null, true, true);
    }

    /**
     * Test constructor with BeanContextServices, Locale, boolean parameters
     * 
     * @see BeanContextServicesSupport#BeanContextServicesSupport(
     *      BeanContextServices, Locale, boolean)
     */
    public void testConstructorBeanContextServicesLocaleboolean() {
        new BeanContextServicesSupport(null, null, true);
    }

    /**
     * Test constructor with BeanContextServices, Locale parameters.
     * <p>
     * 
     * @see BeanContextServicesSupport#BeanContextServicesSupport(
     *      BeanContextServices, Locale)
     */
    public void testConstructorBeanContextServicesLocale() {
        new BeanContextServicesSupport(null, null);
    }

    /**
     * Test constructor with BeanContextServices parameter.
     * <p>
     * 
     * @see BeanContextServicesSupport#BeanContextServicesSupport(
     *      BeanContextServices)
     */
    public void testConstructorBeanContextServices() {
        new BeanContextServicesSupport(null);
    }

    /**
     * * Test constructor with no parameters.
     * <p>
     * 
     * @see BeanContextServicesSupport#BeanContextServicesSupport()
     */
    public void testConstructor() {
        new BeanContextServicesSupport();
    }

    /** TEST METHODS * */

    /**
     * Test method createBCSChild() with Object, Object parameters.
     * <p>
     */
    public void testCreateBCSChildObjectObject() {
        // Just call the method
        BeanContextServicesSupport sup = new BeanContextServicesSupport();
        sup.createBCSChild(new Object(), new Object());
    }

    /**
     * Test method addService() with Class, BeanContextServiceProvider, boolean
     * parameters.
     * <p>
     */
    public void testAddServiceClassBeanContextServiceProviderboolean() {
        // Instantiate services and add service
        BeanContextServicesSupport sup = new BeanContextServicesSupport();
        sup.addService(Object.class, getProvider(), true);

        assertEquals("One service should be registered", 1, sup.services.size());
    }

    /**
     * Test method revokeService() with Class, BeanContextServiceProvider,
     * boolean parameters.
     * <p>
     */
    public void testRevokeServiceClassBeanContextServiceProviderboolean() {
        // Instantiate services, add and remove service
        BeanContextServicesSupport sup = new BeanContextServicesSupport();
        BeanContextServiceProvider pr = getProvider();
        sup.addService(Object.class, pr, true);
        sup.revokeService(Object.class, pr, true);

        assertEquals("No service should be registered", 0, sup.services.size());
    }

    /**
     * Test method addService() with Class, BeanContextServiceProvider
     * parameters.
     * <p>
     */
    public void testAddServiceClassBeanContextServiceProvider() {
        // Instantiate services and add service
        BeanContextServicesSupport sup = new BeanContextServicesSupport();
        sup.addService(Object.class, getProvider());
    }

    /**
     * Test method hasService() with Class parameter.
     * <p>
     */
    public void testHasServiceClass() {
        // Instantiate services and add service
        BeanContextServicesSupport sup = new BeanContextServicesSupport();
        Class<?> cl = new Object().getClass();
        sup.addService(cl, getProvider(), true);

        assertTrue("Service not found", sup.hasService(cl));
    }

    /**
     * Test method getBeanContextServicesPeer() with no parameters.
     * <p>
     */
    public void testGetBeanContextServicesPeer() {
        // Instantiate services
        BeanContextServicesSupport sup = new BeanContextServicesSupport();

        assertTrue("The objects are not equal", sup
                .getBeanContextServicesPeer().equals(sup));
    }

    /**
     * Test method releaseBeanContextResources() with no parameters.
     * <p>
     */
    public void testReleaseBeanContextResources() {
        // Instantiate services
        BeanContextServicesSupport sup = new BeanContextServicesSupport();
        sup.releaseBeanContextResources();

        assertNull("The resources are not released", sup.proxy);
    }

    /**
     * Test method initializeBeanContextResources() with no parameters.
     * <p>
     */
    public void testInitializeBeanContextResources() {
        // Instantiate services
        BeanContextServicesSupport sup = new BeanContextServicesSupport();
        sup.initializeBeanContextResources();

        // if (sup.proxy == null) {
        // fail("The resources are not initialized");
        // }
    }

    /**
     * Test method hasService() with Class=null parameter.
     * <p>
     */
    public void test_hasServiceLjava_lang_Class() {
        BeanContextServicesSupport obj = new BeanContextServicesSupport();
        try {
            obj.hasService(null);
            fail("NullPointerException expected");
        } catch (NullPointerException t) {
        }
    }

    /**
     * Test method removeBeanContextServicesListener() with
     * BeanContextServicesListener=null parameter.
     * <p>
     */
    public void test_removeBeanContextServicesListenerLjava_beans_beancontext_BeanContextServicesListener() {
        BeanContextServicesSupport obj = new BeanContextServicesSupport();
        try {
            obj.removeBeanContextServicesListener(null);
            fail("NullPointerException expected");
        } catch (NullPointerException t) {
        }
    }

    /**
     * Test method serviceAvailable() with BeanContextServiceAvailableEvent=null
     * parameter.
     * <p>
     */
    public void test_serviceAvailableLjava_beans_beancontext_BeanContextServiceAvailableEvent() {
        BeanContextServicesSupport obj = new BeanContextServicesSupport();
        try {
            obj.serviceAvailable(null);
            fail("NullPointerException expected");
        } catch (NullPointerException t) {
        }
    }

    /**
     * Test method serviceRevoked() with BeanContextServiceRevokedEvent=null
     * parameter.
     * <p>
     */
    public void test_serviceRevokedLjava_beans_beancontext_BeanContextServiceRevokedEvent() {
        BeanContextServicesSupport obj = new BeanContextServicesSupport();
        try {
            obj.serviceRevoked(null);
            fail("NullPointerException expected");
        } catch (NullPointerException t) {
        }
    }

    /** UTILITY METHODS * */

    /**
     * Fake implementation of provider
     */
    @SuppressWarnings("unchecked")
    private BeanContextServiceProvider getProvider() {

        return new BeanContextServiceProvider() {
            
            public java.util.Iterator getCurrentServiceSelectors(
                    BeanContextServices bcs, Class serviceClass) {

                return bcs.getCurrentServiceSelectors(serviceClass);
            }

            public Object getService(BeanContextServices bcs, Object requestor,
                    Class serviceClass, Object serviceSelector) {

                return null;
            }

            public void releaseService(BeanContextServices bcs,
                    Object requestor, Object service) {
            }
        };
    }

    /** STANDARD ENDING * */

    /**
     * Start testing from the command line.
     * <p>
     */
    public static Test suite() {
        return new TestSuite(BeanContextServicesSupportTest.class);
    }

    /**
     * Start testing from the command line.
     * <p>
     * 
     * @param args -
     *            Command line parameters.
     *            <p>
     */
    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
}
