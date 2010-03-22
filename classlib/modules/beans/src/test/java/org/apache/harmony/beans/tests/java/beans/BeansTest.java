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

package org.apache.harmony.beans.tests.java.beans;

import java.applet.Applet;
import java.awt.Component;
import java.beans.AppletInitializer;
import java.beans.Beans;
import java.beans.beancontext.BeanContext;
import java.beans.beancontext.BeanContextSupport;
import java.io.Externalizable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.harmony.beans.tests.support.SampleBean;
import org.apache.harmony.beans.tests.support.mock.CorruptedSerBean;
import org.apache.harmony.beans.tests.support.mock.MockAppletInitializer;
import org.apache.harmony.beans.tests.support.mock.MockJavaBean;
import org.apache.harmony.beans.tests.support.mock.WrongSerBean;

/**
 * Unit test for java.beans.Beans
 */
public class BeansTest extends TestCase {

    private final String MOCK_JAVA_BEAN2 = "tests.api.java.beans.mock.MockJavaBean2";

    private final String MOCK_JAVA_BEAN2_FILE = "binary/java/beans/mock/MockJavaBean2.bin";

    private final String MOCK_JAVA_BEAN2_SFILE = "serialization/java/beans/mock/MockJavaBean2.ser";

    /*
     * public Beans()
     */
    public void testBeans() {
        new Beans();
    }

    /*
     * public static Object getInstanceOf(Object bean, Class targetType)
     */
    public void testGetInstanceOf() {
        MockJavaBean bean = new MockJavaBean();
        Class<Component> type = Component.class;
        Object obj = Beans.getInstanceOf(bean, type);

        assertSame(bean, obj);
    }

    public void testGetInstanceOf_BeanNull() {
        Class<Component> type = Component.class;
        Object obj = Beans.getInstanceOf(null, type);

        assertNull(obj);
    }

    public void testGetInstanceOf_TargetTypeNull() {
        MockJavaBean bean = new MockJavaBean();
        Object obj = Beans.getInstanceOf(bean, null);

        assertSame(bean, obj);
    }

    /*
     * Class under test for Object instantiate(ClassLoader, String)
     */
    public void testInstantiateClassLoaderString_Class() throws Exception {

        ClassLoader loader = new BinClassLoader();
        Object bean = Beans.instantiate(loader, MOCK_JAVA_BEAN2);

        assertEquals("as_class", (String) bean.getClass().getMethod(
                "getPropertyOne", (Class[]) null).invoke(bean, (Object[]) null));
        assertSame(loader, bean.getClass().getClassLoader());
    }

    public void testInstantiateClassLoaderString_Ser() throws Exception {
        ClassLoader loader = new SerClassLoader();
        Object bean = Beans.instantiate(loader, MOCK_JAVA_BEAN2);

        assertEquals("as_object", (String) bean.getClass().getMethod(
                "getPropertyOne", (Class[]) null).invoke(bean, (Object[]) null));
        assertSame(loader, bean.getClass().getClassLoader());
    }

    public void testInstantiateClassLoaderString_ClassLoaderNull()
            throws Exception {
        Object bean = Beans.instantiate(null, MockJavaBean.class.getName());

        assertEquals(bean.getClass(), MockJavaBean.class);
        assertSame(ClassLoader.getSystemClassLoader(), bean.getClass()
                .getClassLoader());
    }

    public void testInstantiateClassLoaderString_BeanNameNull()
            throws Exception {
        try {
            Beans.instantiate(null, null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Class under test for Object instantiate(ClassLoader, String, BeanContext)
     */
    public void testInstantiateClassLoaderStringBeanContext_Class()
            throws Exception {

        ClassLoader loader = new BinClassLoader();
        BeanContext context = new BeanContextSupport();
        Object bean = Beans.instantiate(loader, MOCK_JAVA_BEAN2, context);

        assertEquals("as_class", (String) bean.getClass().getMethod(
                "getPropertyOne", (Class[]) null).invoke(bean, (Object[]) null));
        assertSame(loader, bean.getClass().getClassLoader());
        assertTrue(context.contains(bean));
    }

    public void testInstantiateClassLoaderStringBeanContext_Ser()
            throws Exception {
        ClassLoader loader = new SerClassLoader();
        BeanContext context = new BeanContextSupport();
        Object bean = Beans.instantiate(loader, MOCK_JAVA_BEAN2, context);

        assertEquals("as_object", (String) bean.getClass().getMethod(
                "getPropertyOne", (Class[]) null).invoke(bean, (Object[]) null));
        assertSame(loader, bean.getClass().getClassLoader());
        assertTrue(context.contains(bean));
    }

    public void testInstantiateClassLoaderStringBeanContext_ClassLoaderNull()
            throws Exception {
        BeanContext context = new BeanContextSupport();
        Object bean = Beans.instantiate(null, MockJavaBean.class.getName(),
                context);

        assertEquals(bean.getClass(), MockJavaBean.class);
        assertSame(ClassLoader.getSystemClassLoader(), bean.getClass()
                .getClassLoader());
        assertTrue(context.contains(bean));
    }

    public void testInstantiateClassLoaderStringBeanContext_BeanNameNull()
            throws Exception {
        BeanContext context = new BeanContextSupport();
        ClassLoader loader = createSpecificClassLoader();

        try {
            Beans.instantiate(loader, null, context);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testInstantiateClassLoaderStringBeanContext_ContextNull()
            throws Exception {
        ClassLoader loader = createSpecificClassLoader();
        Object bean = Beans.instantiate(loader, MockJavaBean.class.getName(),
                null);

        assertEquals(bean.getClass(), MockJavaBean.class);
    }

    /*
     * Class under test for Object instantiate(ClassLoader, String, BeanContext,
     * AppletInitializer)
     */
    public void testInstantiateClassLoaderStringBeanContextAppletInitializer_Class()
            throws Exception {
        ClassLoader loader = new BinClassLoader();
        BeanContext context = new BeanContextSupport();
        AppletInitializer appInit = new MockAppletInitializer();
        Object bean = Beans.instantiate(loader, MOCK_JAVA_BEAN2, context,
                appInit);

        assertEquals("as_class", (String) bean.getClass().getMethod(
                "getPropertyOne", (Class[]) null).invoke(bean, (Object[]) null));
        assertSame(loader, bean.getClass().getClassLoader());
        assertTrue(context.contains(bean));
    }

    public void testInstantiateClassLoaderStringBeanContextAppletInitializer_Ser()
            throws Exception {

        ClassLoader loader = new SerClassLoader();
        BeanContext context = new BeanContextSupport();
        AppletInitializer appInit = new MockAppletInitializer();
        Object bean = Beans.instantiate(loader, MOCK_JAVA_BEAN2, context,
                appInit);

        assertEquals("as_object", (String) bean.getClass().getMethod(
                "getPropertyOne", (Class[]) null).invoke(bean, (Object[]) null));
        assertSame(loader, bean.getClass().getClassLoader());
        assertTrue(context.contains(bean));
    }

    public void testInstantiateClassLoaderStringBeanContextAppletInitializer_LoaderNull()
            throws Exception {
        String beanName = "org.apache.harmony.beans.tests.support.mock.MockJavaBean";
        BeanContext context = new BeanContextSupport();
        AppletInitializer appInit = new MockAppletInitializer();

        Object bean = Beans.instantiate(null, beanName, context, appInit);

        assertSame(ClassLoader.getSystemClassLoader(), bean.getClass()
                .getClassLoader());
        assertEquals(beanName, bean.getClass().getName());
        assertTrue(context.contains(bean));
    }

    public void testInstantiateClassLoaderStringBeanContextAppletInitializer_BeanNull()
            throws Exception {
        ClassLoader loader = createSpecificClassLoader();
        BeanContext context = new BeanContextSupport();
        AppletInitializer appInit = new MockAppletInitializer();

        try {
            Beans.instantiate(loader, null, context, appInit);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testInstantiateClassLoaderStringBeanContextAppletInitializer_ContextNull()
            throws Exception {
        ClassLoader loader = createSpecificClassLoader();
        String beanName = "org.apache.harmony.beans.tests.support.mock.MockJavaBean";
        AppletInitializer appInit = new MockAppletInitializer();
        Object bean = Beans.instantiate(loader, beanName, null, appInit);

        assertSame(ClassLoader.getSystemClassLoader(), bean.getClass()
                .getClassLoader());
        assertEquals(beanName, bean.getClass().getName());
    }

    public void testInstantiateClassLoaderStringBeanContextAppletInitializer_InitializerNull()
            throws Exception {
        ClassLoader loader = createSpecificClassLoader();
        String beanName = "org.apache.harmony.beans.tests.support.mock.MockJavaBean";
        BeanContext context = new BeanContextSupport();
        Object bean = Beans.instantiate(loader, beanName, context, null);

        assertSame(ClassLoader.getSystemClassLoader(), bean.getClass()
                .getClassLoader());
        assertEquals(beanName, bean.getClass().getName());
    }

    // public void
    // testInstantiateClassLoaderStringBeanContextAppletInitializer_AppletBean()
    // throws IOException, ClassNotFoundException {
    // String beanName = MockAppletBean.class.getName(); BeanContext context =
    // new BeanContextSupport(); MockAppletInitializer appInit = new
    // MockAppletInitializer(); MockAppletBean bean = (MockAppletBean)
    // Beans.instantiate(null, beanName, context, appInit);
    // assertSame(ClassLoader.getSystemClassLoader(), bean.getClass()
    // .getClassLoader()); assertEquals(beanName, bean.getClass().getName());
    // assertTrue(context.contains(bean));
    // assertTrue(appInit.activateHasBeenCalled());
    // assertTrue(appInit.initializeHasBeenCalled());
    // assertTrue(bean.initHasBeenCalled());
    // }

    // public void
    // testInstantiateClassLoaderStringBeanContextAppletInitializer_AppletBean_SER()
    // throws IOException, ClassNotFoundException {
    //    
    // String beanName = MockAppletBean2.class.getName(); BeanContext context =
    // new BeanContextSupport(); MockAppletInitializer appInit = new
    // MockAppletInitializer(); MockAppletBean2 bean = (MockAppletBean2)
    // Beans.instantiate(null, beanName, context, appInit);
    // assertSame(ClassLoader.getSystemClassLoader(), bean.getClass()
    // .getClassLoader()); assertEquals(beanName, bean.getClass().getName());
    // assertTrue(context.contains(bean));
    // assertTrue(appInit.activateHasBeenCalled());
    // assertTrue(appInit.initializeHasBeenCalled());
    // assertFalse(bean.initHasBeenCalled());
    //     
    // }

    // public void
    // testInstantiateClassLoaderStringBeanContextAppletInitializer_AppletBean_2()
    // throws IOException, ClassNotFoundException {
    //    
    // String beanName = MockAppletBean.class.getName(); BeanContext context =
    // new BeanContextSupport(); MockAppletInitializer appInit = new
    // MockAppletInitializer(); MockAppletBean bean = (MockAppletBean)
    // Beans.instantiate(null, beanName, context, null);
    //     
    // }

    /*
     * public static boolean isInstanceOf(Object bean, Class targetType)
     */
    public void testIsInstanceOf() {
        MockJavaBean bean = new MockJavaBean();

        assertTrue(Beans.isInstanceOf(bean, Serializable.class));
    }

    public void testIsInstanceOf_BeanNull() {
        try {
            Beans.isInstanceOf(null, Serializable.class);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testIsInstanceOf_TypeNull() {
        MockJavaBean bean = new MockJavaBean();
        assertFalse(Beans.isInstanceOf(bean, null));
    }

    public void testIsInstanceOf_TypeInvalid() {
        MockJavaBean bean = new MockJavaBean();
        assertFalse(Beans.isInstanceOf(bean, String.class));
    }

    public void testSetDesignTime() {
        boolean value = Beans.isDesignTime();
        try {
            Beans.setDesignTime(true);
            assertTrue(Beans.isDesignTime());

            Beans.setDesignTime(false);
            assertFalse(Beans.isDesignTime());
        } finally {
            Beans.setDesignTime(value);
        }
    }

    public void testSetGuiAvailable() {
        boolean value = Beans.isGuiAvailable();
        try {
            Beans.setGuiAvailable(true);
            assertTrue(Beans.isGuiAvailable());

            Beans.setGuiAvailable(false);
            assertFalse(Beans.isGuiAvailable());
        } finally {
            Beans.setGuiAvailable(value);
        }
    }

    public void testIsGuiAvailableDefault() {
        assertTrue("GUI is available by default", Beans.isGuiAvailable());
    }

    /**
     * The test checks the method instantiate() using specific classloader for
     * class loading
     */
    public void testLoadBySpecificClassLoader() throws Exception {
        String beanName = "org.apache.harmony.beans.tests.support.SampleBean";

        ClassLoader cls = createSpecificClassLoader();
        Object bean = Beans.instantiate(cls, beanName);
        SampleBean sampleBean;

        assertNotNull(bean);
        assertEquals(bean.getClass(), SampleBean.class);

        sampleBean = (SampleBean) bean;
        assertNull(sampleBean.getText());
    }

    /**
     * The test checks the method instantiate() using default classloader for
     * class loading
     */
    public void testLoadByDefaultClassLoader() throws Exception {
        String beanName = "org.apache.harmony.beans.tests.support.SampleBean";

        Object bean = Beans.instantiate(null, beanName);
        SampleBean sampleBean;

        assertNotNull(bean);
        assertEquals(bean.getClass(), SampleBean.class);

        sampleBean = (SampleBean) bean;
        assertNull(sampleBean.getText());
    }

    // regression test for HARMONY-358
    public void testInstantiate() throws Exception {
        try {
            Class.forName(this.getClass().getName(), true, null);
            fail("This test is designed to run from classpath rather then from bootclasspath");
        } catch (ClassNotFoundException ok) {
        }
        assertNotNull(Beans.instantiate(null, this.getClass().getName()));
    }

    // regression test for HARMONY-402
    public void test_isInstanceOf_Object_Class() {
        ObjectBean bean = new ObjectBean();
        // correct non-null targetType
        Class<Externalizable> targetType = Externalizable.class;

        assertTrue(Beans.isInstanceOf(bean, targetType));

        // null targetType
        targetType = null;
        assertFalse(Beans.isInstanceOf(bean, targetType));
    }

    public void test_instantiate_with_empty_serialization_file()
            throws Exception {
        final String BEANS_NAME = "org/apache/harmony/beans/tests/support/mock/EmptySerBean.ser";

        try {
            Beans.instantiate(null, BEANS_NAME);
            fail("should throw ClassNotFoundException.");
        } catch (ClassNotFoundException e) {
            // expected
        }
    }

    // Regression for HARMONY-3777
    public void test_instantiate_with_applet() throws Exception {
        Applet applet = (Applet) Beans.instantiate(null, "java.applet.Applet");
        assertNotNull(applet.getAppletContext());
        assertTrue(applet.isActive());
    }

    /*
     * Test instantiate a bean with corrupted .ser file. First failed to create
     * an instance by deserialize from a corrupted .ser file, then successfully
     * load the class and create an instance of it.
     */
    public void test_instantiate_withCorruptedSer() throws IOException,
            ClassNotFoundException {
        Object bean = Beans.instantiate(null,
                "org.apache.harmony.beans.tests.support.mock.CorruptedSerBean");
        assertTrue(bean instanceof CorruptedSerBean);
    }

    /*
     * Test instantiate a bean with wrong .ser file, which means the definition
     * of the class changes after .ser file is created. First failed to create
     * an instance by deserialize from a corrupted .ser file, which will cause a
     * InvalidClassException, then successfully load the class and create an
     * instance of it.
     */
    public void test_instantiate_withWrongSer() throws IOException,
            ClassNotFoundException {
        Object bean = Beans.instantiate(null,
                "org.apache.harmony.beans.tests.support.mock.WrongSerBean");
        assertTrue(bean instanceof WrongSerBean);
    }

    /*
     * Test instantiate a bean with wrong but not corrupted .ser file First
     * failed to create an instance by deserialize from a wrong .ser file, which
     * will cause a ClassNotFoundException.
     */
    public void test_instantiate_ClassNotFoundExceptionThrowing()
            throws IOException {
        ClassLoader loader = new WrongSerClassLoader();
        try {
            Beans
                    .instantiate(loader,
                            "org.apache.harmony.beans.tests.support.mock.WrongSerBean2");
            fail("Should throw a ClassNotFoundException");
        } catch (ClassNotFoundException ex) {
            // expected
        }
    }

    /*
     * Test instantiate bean with corrupted .ser file and wrong class name. This
     * will cause an IOException.
     */
    public void test_instantiate_IOExceptionThrowing()
            throws ClassNotFoundException {
        ClassLoader loader = new CorruptedSerClassLoader();
        try {
            Beans.instantiate(loader, "NotExistBean2");
            fail("Should throw a IOException");
        } catch (IOException ex) {
            // expected
        }
    }

    public static Test suite() {
        return new TestSuite(BeansTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    private ClassLoader createSpecificClassLoader() {
        return new ClassLoader() {
        };
    }

    private class ObjectBean implements Externalizable {

        private static final long serialVersionUID = 637071583755213744L;

        public void writeExternal(ObjectOutput out) {
        };

        public void readExternal(ObjectInput in) {
        };
    }

    private class BinClassLoader extends ClassLoader {

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (!MOCK_JAVA_BEAN2.equals(name)) {
                return super.findClass(name);
            }

            try {
                // makes sense to get actual file size?
                byte[] buf = new byte[10000];

                InputStream in = getResourceAsStream(MOCK_JAVA_BEAN2_FILE);

                int sz = 0;

                // read whole file
                int read;

                while ((read = in.read(buf, sz, buf.length - sz)) >= 0) {
                    sz += read;
                }

                return defineClass(MOCK_JAVA_BEAN2, buf, 0, sz);
            } catch (IOException e) {
                throw (ClassNotFoundException) new ClassNotFoundException(e
                        .toString()).initCause(e);
            }
        }
    }

    private class SerClassLoader extends BinClassLoader {

        private final String MOCK_JAVA_BEAN2_SNAME = MOCK_JAVA_BEAN2.replace(
                '.', '/')
                + ".ser";;

        @Override
        protected URL findResource(String name) {
            if (!MOCK_JAVA_BEAN2_SNAME.equals(name)) {
                return super.findResource(name);
            }

            return getResource(MOCK_JAVA_BEAN2_SFILE);
        }
    }

    /*
     * A classloader for loading NotExistBean.ser, of which coresponding
     * NotExistBean.java file is deleted.
     */
    private class WrongSerClassLoader extends ClassLoader {
        @Override
        protected URL findResource(String name) {
            return getResource("org/apache/harmony/beans/tests/support/mock/NotExistBean.ser");
        }
    }

    /*
     * A classloader for loading corrupted .ser file.
     */
    private class CorruptedSerClassLoader extends ClassLoader {
        @Override
        protected URL findResource(String name) {
            return getResource("org/apache/harmony/beans/tests/support/mock/CorruptedSerBean.ser");
        }
    }

}
