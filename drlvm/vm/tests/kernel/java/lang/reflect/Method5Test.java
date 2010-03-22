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
package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.apache.harmony.lang.AnnotatedElementTestFrame;
import org.apache.harmony.lang.AnnotatedElementTestFrame.MissingClassValAntn;
import org.apache.harmony.lang.AnnotatedElementTestFrame.MissingTypeAntn;
import org.apache.harmony.lang.AnnotatedElementTestFrame.TagAntn;
import org.apache.harmony.lang.AnnotatedElementTestFrame.ValAntn;

/**
 * @author Alexey V. Varlamov
 */ 
public class Method5Test extends AnnotatedElementTestFrame {

    protected @Override AnnotatedElement getElement1() throws Throwable {
        return AnnotatedMethod.class.getMethod("foo");
    }
    
    protected @Override AnnotatedElement getElement2() throws Throwable {
        return AnnotatedMethod.class.getMethod("bar");
    }

    protected @Override AnnotatedElement getElement3() throws Throwable {
        return AnnotatedMethod.class.getMethod("buz", String.class);
    }
    
    /**
     * Provides an instance to be tested. The instance must be annotated
     * by the notfound.MissingAntn.
     */
    protected @Override AnnotatedElement getElement4() throws Throwable {
        return AnnotatedMethod.class.getMethod("i", int.class);
    }

    /**
     * Provides an instance to be tested. The instance must be annotated
     * by the MissingClassValAntn.
     */
    protected @Override AnnotatedElement getElement5() throws Throwable {
        return AnnotatedMethod.class.getMethod("l", long.class);
    }

    /**
     * Provides an instance to be tested. The instance must be annotated
     * by the MissingTypeAntn.
     */
    protected @Override AnnotatedElement getElement6() throws Throwable {
        return AnnotatedMethod.class.getMethod("ch", char.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(Method5Test.class);
    }

    protected final Annotation[][] getParamAnnotations(Member obj) throws Throwable {
        assertTrue("illegal argument: " + obj,
                (obj instanceof Method) || (obj instanceof Constructor));
        Method m = obj.getClass().getMethod("getParameterAnnotations");
        return (Annotation[][])m.invoke(obj);
    }

    protected Member getParamElement1() throws Throwable {
        return AnnotatedParamMethod.class.getMethod("foo");
    }
    
    protected Member getParamElement2() throws Throwable {
        return AnnotatedParamMethod.class.getMethod("bar", 
                Object.class, Class.class, boolean.class);
    }
    
    protected Member getParamElement3() throws Throwable {
        return AnnotatedParamMethod.class.getMethod("buz", 
                String.class, int.class);
    }
    
    /**
     * getParameterAnnotations() should return empty array
     * if member has no parameters.
     */
    public void testGetParameterAnnotations1() throws Throwable {
        Annotation[][] an = getParamAnnotations(getParamElement1());
        assertNotNull(an);
        assertEquals(0, an.length);
    }

    /**
     * getParameterAnnotations() should return array of annotation 
     * arrays for member parameters, in declaration order.
     */
    public void testGetParameterAnnotations2() throws Throwable {
        Annotation[][] an = getParamAnnotations(getParamElement2());
        assertNotNull(an);
        assertEquals("param num", 3, an.length);
        
        assertEquals("1st param annotation num", 1, an[0].length);
        assertSame("1st param annotation type", P1Antn.class, 
                an[0][0].annotationType());

        assertEquals("2nd param annotation num", 1, an[1].length);
        assertSame("2nd param annotation type", P2Antn.class, 
                an[1][0].annotationType());
        assertEquals("2nd param annotation value", 123, ((P2Antn)an[1][0]).value());
        
        assertEquals("3rd param annotation num", 2, an[2].length);
        assertSame("3rd param annotation 1 type", P3Antn.class, 
                an[2][0].annotationType());
        assertSame("3rd param annotation 2 type", ValAntn.class, 
                an[2][1].annotationType());
        assertEquals("3rd param annotation 2 value", "xyz", ((ValAntn)an[2][1]).value());
    }

    /**
     * For each parameter having no annotations,
     * getParameterAnnotations() should return corresponding 
     * nested arrays with zero size. 
     */
    public void testGetParameterAnnotations3() throws Throwable {
        Annotation[][] an = getParamAnnotations(getParamElement3());
        assertNotNull(an);
        assertEquals("param num", 2, an.length);
        
        assertEquals("1st param annotation num", 0, an[0].length);

        assertEquals("2nd param annotation num", 1, an[1].length);
        assertSame("2nd param annotation type", TagAntn.class, 
                an[1][0].annotationType());
    }
    
    /**
     * getParameterAnnotations() should return cloned arrays
     * which can be safely modified by a caller.
     */
    public void testGetParameterAnnotationsImmutable() throws Throwable {
        Member m = getParamElement2();
        Annotation[][] an0 = getParamAnnotations(m);
        assertNotNull(an0);
        assertEquals("param num", 3, an0.length);
        an0[1] = an0[2];
        an0[1][1] = null;
        an0[2] = new Annotation[0];
        
        Annotation[][] an = getParamAnnotations(m);
        
        assertEquals("1st param annotation num", 1, an[0].length);
        assertSame("1st param annotation type", P1Antn.class, 
                an[0][0].annotationType());

        assertEquals("2nd param annotation num", 1, an[1].length);
        assertSame("2nd param annotation type", P2Antn.class, 
                an[1][0].annotationType());
        assertEquals("2nd param annotation value", 123, ((P2Antn)an[1][0]).value());
        
        assertEquals("3rd param annotation num", 2, an[2].length);
        assertSame("3rd param annotation 1 type", P3Antn.class, 
                an[2][0].annotationType());
        assertSame("3rd param annotation 2 type", ValAntn.class, 
                an[2][1].annotationType());
        assertEquals("3rd param annotation 2 value", "xyz", ((ValAntn)an[2][1]).value());
    }
    
    static class A {
        private Object obj;
        class InA {
            Object o = obj; 
        }
    }
    
    enum E { E1, E2, E3}
    
    static class B1 {
        public Object foo() { return null;}
        public Object bar(int i, Object... o) { return null;}
        public static void main1(String... s){}
        public static void main2(String[] s){}
    }

    static class B2 extends B1 {
        public String foo() { return "";}
        public String bar(int i, Object... o) { return "";}
    }
        
    /**
     * isSynthetic() should return true if and only if
     * the target method does not appear in the source code. 
     */
    public void testIsSynthetic() throws Exception {
        assertFalse("case1.1: ordinary method", 
                AnnotatedMethod.class.getMethod("foo").isSynthetic());
        assertFalse("case1.2: ordinary vararg method", 
                B1.class.getMethod("main1", String[].class).isSynthetic());
        
        Method[] ms = A.class.getDeclaredMethods();
        assertTrue(ms != null && ms.length > 0);
        for (Method m : ms){
            assertTrue("case2: " + m, m.isSynthetic());
        }

        // XXX bug in compiler? 
        //assertTrue("case3: EnumType.values()", 
          //      E.class.getMethod("values").isSynthetic());
    }
    
    /**
     * isBridge() should return true if and only if
     * the target method does not appear in the source code
     * and overrides covariant return type method. 
     */
    public void testIsBridge() throws Exception {
        assertFalse("case1.1: ordinary method", 
                AnnotatedMethod.class.getMethod("foo").isBridge());
        assertFalse("case1.2: ordinary vararg method", 
                B1.class.getMethod("main1", String[].class).isBridge());
        
        Method[] ms = B1.class.getMethods();
        assertTrue(ms != null && ms.length > 0);
        for (Method m : ms){
            assertFalse("case2.1: " + m, m.isBridge());
        }
        
        ms = B2.class.getDeclaredMethods();
        assertTrue(ms != null && ms.length > 0);
        for (Method m : ms){
            //System.out.println("case2.2 " + m);
            assertTrue("case2.2: " + m, 
                    m.getReturnType() != Object.class ^ m.isBridge());
        }

        assertFalse("case3: EnumType.values()", 
                E.class.getMethod("values").isBridge());
    }
    
    /**
     * isVarArgs() should return true if and only if
     * the target method is declared with varargs. 
     */
    public void testIsVarargs() throws Exception {
        assertFalse("case1: ordinary method", 
                AnnotatedMethod.class.getMethod("foo").isVarArgs());
        
        assertTrue("case2: varargs method", 
                B1.class.getMethod("main1", String[].class).isVarArgs());

        assertFalse("case3: ordinary method", 
                B1.class.getMethod("main2", String[].class).isVarArgs());
    }
    
    static abstract class C {
        protected abstract <T extends Throwable> 
        OneParamType<? extends T> go(T t) throws T, Error;
    }

    /**
     * toGenericString() should return a string exactly matching
     * the API specification.
     */
    public void testToGenericString() throws Exception {
        String s = C.class.getDeclaredMethod("go", 
                Throwable.class).toGenericString();
        System.out.println(s);
        assertEquals(
                // Should method type parameter be followed by a type bound? It is unspecified.
                // The known reference implementation doesn't do it as well:
                //"protected abstract <T extends java.lang.Throwable>"
                "protected abstract <T>"
                + " java.lang.reflect.OneParamType<? extends T>"
                + " java.lang.reflect.Method5Test$C.go(T)"
                + " throws T,java.lang.Error",
                s);
    }
    
    /**
     * toGenericString() should return a string exactly matching
     * the API specification.
     */
    public void testToGenericString2() throws Exception {
        String s = B2.class.getDeclaredMethod("bar", 
                int.class, Object[].class).toGenericString();
        // it is unspecified which method should be returned:
        // original (overriden with covariant return) or
        // compiler-generated bridge.
        // different implementations behave differently,
        // so relaxed this check to allow both variants.
        assertTrue("start: " + s, s.startsWith("public java.lang.")); // String or Object
        assertTrue("end: " + s, 
                s.endsWith(" java.lang.reflect.Method5Test$B2."
                        + "bar(int,java.lang.Object[])"));
    }
}

abstract class AnnotatedMethod {
    @TagAntn public abstract void foo();
    @TagAntn @ValAntn public Object[] bar() {return null;}
    public String buz(String s){return s;}
    @notfound.MissingAntn public void i(int i) {}
    @MissingClassValAntn public void l(long l){}
    @MissingTypeAntn public void ch(char ch){}
}

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@interface P1Antn {}

@Retention(RetentionPolicy.RUNTIME)
@interface P2Antn {
    int value();
}

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@interface P3Antn {
    String[] meta1() default {};
    P1Antn meta2() default @P1Antn;
}

class AnnotatedParamMethod {
    @TagAntn public void foo(){}
    @ValAntn("abc") public void bar(@P1Antn Object p1, 
            @P2Antn(123) Class p2, @P3Antn @ValAntn("xyz") boolean p3){}
    public void buz(String s, @TagAntn int i){}
}
