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
 * @author Alexey V. Varlamov
 */
package java.lang.reflect;

import org.apache.harmony.lang.AnnotatedElementTestFrame;
import org.apache.harmony.lang.AnnotatedElementTestFrame.MissingClassValAntn;
import org.apache.harmony.lang.AnnotatedElementTestFrame.MissingTypeAntn;
import org.apache.harmony.lang.AnnotatedElementTestFrame.TagAntn;
import org.apache.harmony.lang.AnnotatedElementTestFrame.ValAntn;

class AnnotatedField {
    @TagAntn public Object foo;
    @TagAntn @ValAntn public Object[] bar;
    public static transient volatile TwoParamType<String, ?> buz;
    @notfound.MissingAntn public int i;
    @MissingClassValAntn public long l;
    @MissingTypeAntn public char ch;
}

class TwoParamType<K, V> {}
class OneParamType<U> {}

public class Field5Test extends AnnotatedElementTestFrame {

    protected @Override AnnotatedElement getElement1() throws Throwable {
        return AnnotatedField.class.getField("foo");
    }
    
    protected @Override AnnotatedElement getElement2() throws Throwable {
        return AnnotatedField.class.getField("bar");
    }

    protected @Override AnnotatedElement getElement3() throws Throwable {
        return AnnotatedField.class.getField("buz");
    }
    
    /**
     * Provides an instance to be tested. The instance must be annotated
     * by the notfound.MissingAntn.
     */
    protected @Override AnnotatedElement getElement4() throws Throwable {
        return AnnotatedField.class.getField("i");
    }

    /**
     * Provides an instance to be tested. The instance must be annotated
     * by the MissingClassValAntn.
     */
    protected @Override AnnotatedElement getElement5() throws Throwable {
        return AnnotatedField.class.getField("l");
    }

    /** 
     * Provides an instance to be tested. The instance must be annotated
     * by the MissingTypeAntn.
     */
    protected @Override AnnotatedElement getElement6() throws Throwable {
        return AnnotatedField.class.getField("ch");
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(Field5Test.class);
    }
    
    class A {
        {
            assert true;
        }
    }
    enum E { E1, E2, E3}
    static class B {
        public E foo;
    }
    
    /**
     * isSynthetic() should return true if and only if
     * the target field does not appear in the source code. 
     */
    public void testIsSynthetic() throws Exception {
        assertFalse("case1.1: ordinary field", 
                AnnotatedField.class.getField("foo").isSynthetic());
        assertFalse("case1.2: ordinary field", 
                B.class.getField("foo").isSynthetic());
        
        Field[] fs = A.class.getDeclaredFields();
        assertTrue(fs != null && fs.length > 0);
        for (Field f : fs){
            assertTrue("case2: " + f.getName(), f.isSynthetic());
        }
        
        fs = E.class.getFields();
        assertTrue(fs != null && fs.length > 0);
        for (Field f : fs){
            assertFalse("case3: " + f.getName(), f.isSynthetic());
        }        
    }
    
    /**
     * isEnumConstant() should return true if and only if
     * the target field is an element of an enumeration. 
     */
    public void testIsEnumConstant() throws Exception {
        assertFalse("case1: ordinary field", 
                AnnotatedField.class.getField("foo").isEnumConstant());
        
        Field[] fs = E.class.getFields();
        assertTrue(fs != null && fs.length > 0);
        for (Field f : fs){
            assertTrue("case2: " + f.getName(), f.isEnumConstant());
        }
        
        assertFalse("case3: enum-typed member field", 
                B.class.getField("foo").isEnumConstant());
    }
    
    enum E2 {
        E1;
        public int i;
    }
    /**
     * isEnumConstant() should not return true 
     * for ordinary members (not elements) of an enumeration. 
     */
    public void testIsEnumConstant2() throws Exception {
        assertFalse(E2.class.getField("i").isEnumConstant());
    }
    
    /**
     * toGenericString() should return a string exactly matching
     * the API specification.
     */
    public void testToGenericString() throws Exception {
        String s = AnnotatedField.class.getField("buz").toGenericString();
        System.out.println(s);
        assertEquals(
                "public static transient volatile"
                + " java.lang.reflect.TwoParamType<java.lang.String, ?>"
                + " java.lang.reflect.AnnotatedField.buz",
                s);
    }
    
    
    public final int INSTANCE_I = 10;
    
    /**
     * Regression test for HARMONY-4927
     */    
    public void testAccessFinalInstance() throws Throwable {
        Field fi = this.getClass().getField("INSTANCE_I");
        final Object oldVal = fi.get(this);
        final Object newVal = new Integer(2134523);
        
        try {
            fi.set(this, newVal);
            fail("Should not modify final field");
        } catch (IllegalAccessException expected) {
            assertEquals(oldVal, fi.get(this));
        }
        
        fi.setAccessible(true);
        fi.set(this, newVal);        
        assertEquals(newVal, fi.get(this));
    }
    
    public static final int STATIC_I = 10;
    
    public void testAccessFinalStatic() throws Throwable {
        Field fi = this.getClass().getField("STATIC_I");
        final Object oldVal = fi.get(null);
        final Object newVal = new Integer(2134523);
        
        try {
            fi.set(null, newVal);
            fail("Should not modify final field");
        } catch (IllegalAccessException expected) {
            assertEquals(oldVal, fi.get(null));
        }
        
        try {
            fi.setAccessible(true);
            fi.set(this, newVal);        
            fail("Should not modify final field");
        } catch (IllegalAccessException expected) {
            assertEquals(oldVal, fi.get(null));
        }
    }

}
