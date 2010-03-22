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
package org.apache.harmony.lang.annotation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.apache.harmony.lang.AnnotatedElementTestFrame.MissingClassValAntn;

import junit.framework.TestCase;

/**
 * The test of annotation implementation, covering all possible types of elements.
 * 
 * @author Alexey V. Varlamov
 */
@AllTypesAntn
public class AllTypesTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AllTypesTest.class);
    }
    
    /**
     * The annotation instance should return correct default values 
     * of elements if no actual values were specified. 
     */
    public void testGetAnnotation_ByDefault() throws Throwable {
        AllTypesAntn antn = AllTypesAntn.class.getAnnotation(AllTypesAntn.class);
        assertNotNull("the annotation", antn);
        Method[] ms = AllTypesAntn.class.getDeclaredMethods();
        for (Method m : ms) {
            Object defaultValue = m.getDefaultValue();
            String name = m.getName();
            assertNotNull("null defaultValue of " + name, defaultValue);
            Object value = m.invoke(antn);
            assertNotNull("null value of " + name, value);
            Class<?> def = m.getReturnType();
            Class<?> real = value.getClass();
            if (def.isAnnotation()) {
                assertTrue("nested antn value type : def=" + def + "; real=" + real, def.isInstance(value));
            } else if (def.isPrimitive()) {
                assertSame("unboxed value type", def, real.getField("TYPE").get(null));
            } else {
                assertSame("value type", def, real);
            }
            
            if (m.getReturnType().isArray()) {
                int len = Array.getLength(defaultValue);
                assertEquals("array length of " + m.getName(), len, Array.getLength(value));
                for (int i = 0; i < len; i++) {
                    assertEquals(m.getName() + "["+i+"]", Array.get(defaultValue, i), Array.get(value, i));
                }
            } else {
                assertEquals(m.getName(), defaultValue, value);
            }
        }
    }
    
    /**
     * The annotation instance should return correct actual values 
     * of elements overriding their defaults. 
     */
    public void testGetAnnotation_NoDefault() throws Throwable {
        AllTypesAntn2 antn = AllTypesAntn2.class.getAnnotation(AllTypesAntn2.class);
        assertNotNull("the annotation", antn);
        Method[] ms = AllTypesAntn2.class.getDeclaredMethods();
        for (Method m : ms) {
            Object defaultValue = m.getDefaultValue();
            String name = m.getName();
            assertNull("defaultValue of " + name, defaultValue);
            Object value = m.invoke(antn);
            assertNotNull("null value of " + name, value);
            Class<?> def = m.getReturnType();
            Class<?> real = value.getClass();
            if (def.isAnnotation()) {
                assertTrue("nested antn value type : def=" + def + "; real=" + real, def.isInstance(value));
            } else if (def.isPrimitive()) {
                assertSame("unboxed value type", def, real.getField("TYPE").get(null));
            } else {
                assertSame("value type", def, real);
            }
        }
    }
    
    /**
     * The equals() method of annotation instance 
     * must true if equivalent annotation instance is passed, false otherwise.
     */
    @AllTypesAntn(intArrayValue=346546)
    public void testEquals() throws Throwable {
        AllTypesAntn antn = AllTypesAntn.class.getAnnotation(AllTypesAntn.class);
        assertTrue("case1", antn.equals(antn));
        Object obj = this.getClass().getAnnotation(AllTypesAntn.class);
        assertEquals("case2", obj, antn);
        assertEquals("case2_1", obj.hashCode(), antn.hashCode());
        AllTypesAntn2 antn2 = AllTypesAntn2.class.getAnnotation(AllTypesAntn2.class);
        assertEquals("case3", antn2, AllTypesAntn2.class.getAnnotation(AllTypesAntn2.class));
        assertFalse("case4", antn.equals(antn2));
        assertFalse("case5", antn.equals(
                this.getClass().getMethod("testEquals").getAnnotation(AllTypesAntn.class)));
    }

    /**
     * The equals() method of annotation instance 
     * must correctly work with alternative implementations.
     */
    public void testEquals_ForeignImpl() throws Throwable {
        AllTypesAntn antn = AllTypesAntn.class.getAnnotation(AllTypesAntn.class);
        assertTrue(antn.equals(new AllTypesAntn.MockedImpl()));
    }
    
    /**
     * The toString() method of annotation instance 
     * must return meaningful description.
     */
    public void testToString() throws Throwable {
        String s = AllTypesAntn.class.getAnnotation(AllTypesAntn.class).toString();
        assertNotNull(s);
        assertTrue(s.length() != 0);
        assertTrue(s.startsWith("@org.apache.harmony.lang.annotation.AllTypesAntn"));
    }
    
    /**
     * The annotationType() method of annotation instance 
     * must return correct Class.
     */
    public void testAnnotationType() throws Throwable {
        assertSame(AllTypesAntn.class, 
                AllTypesAntn.class.getAnnotation(AllTypesAntn.class).annotationType());
    }

    /**
     * An annotation should defer throwing TypeNotPresentException
     * for a Class-valued member until the member accessed.
     */
    @MissingClassValAntn
    public void testAnnotation_ElementError() throws Throwable {
        MissingClassValAntn antn = this.getClass().getMethod(
                "testAnnotation_ElementError").getAnnotation(MissingClassValAntn.class);
        try {
            antn.clss();
            fail("Misconfigured test: "
                    + "notfound.MissingClass should not be in classpath");
        } catch (TypeNotPresentException tnpe) {
            assertTrue("reported type name: " + tnpe.typeName(), 
                    tnpe.typeName().matches("notfound.MissingClass"));
        }

        try {
            antn.clssArray();
            fail("Misconfigured test: "
                    + "notfound.MissingClass should not be in classpath");
        } catch (TypeNotPresentException tnpe) {
            assertTrue("reported type name: " + tnpe.typeName(), 
                    tnpe.typeName().matches("notfound.MissingClass"));
        }
    }

    /**
     * Tests that annotation can be serialized and deserialized without
     * exceptions, and that deserialization really produces deeply cloned
     * objects.
     */
    public void testSerialization() throws Throwable {
        Object[] arr = new Object[] {
                AllTypesAntn.class.getAnnotation(AllTypesAntn.class),
                AllTypesAntn2.class.getAnnotation(AllTypesAntn2.class)
        };
        for (Object data : arr) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            putObjectToStream(data, bos);
            ByteArrayInputStream bis = new ByteArrayInputStream(bos
                    .toByteArray());
            assertEquals(data, getObjectFromStream(bis));
        }
    }
    
    /**
     * Serializes specified object to an output stream.
     */
    static void putObjectToStream(Object obj, OutputStream os)
        throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(obj);
        oos.flush();
        oos.close();
    }

    /**
     * Deserializes single object from an input stream.
     */
    static Object getObjectFromStream(InputStream is) throws IOException,
        ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(is);
        Object result = ois.readObject();
        ois.close();
        return result;
    }
}
