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


import org.apache.harmony.lang.AnnotatedElementTestFrame.MissingClassValAntn;
import org.apache.harmony.lang.AnnotatedElementTestFrame.MissingTypeAntn;
import org.apache.harmony.lang.AnnotatedElementTestFrame.TagAntn;
import org.apache.harmony.lang.AnnotatedElementTestFrame.ValAntn;

/**
 * @author Alexey V. Varlamov
 */ 
public class Ctor5Test extends Method5Test {
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(Ctor5Test.class);
    }

    protected @Override AnnotatedElement getElement1() throws Throwable {
        return AnnotatedCtor.class.getConstructor();
    }
    
    protected @Override AnnotatedElement getElement2() throws Throwable {
        return AnnotatedCtor.class.getConstructor(Object.class);
    }

    protected @Override AnnotatedElement getElement3() throws Throwable {
        return AnnotatedCtor.class.getConstructor(String.class);
    }
    
    /**
     * Provides an instance to be tested. The instance must be annotated
     * by the notfound.MissingAntn.
     */
    protected @Override AnnotatedElement getElement4() throws Throwable {
        return AnnotatedCtor.class.getConstructor(int.class);
    }

    /**
     * Provides an instance to be tested. The instance must be annotated
     * by the MissingClassValAntn.
     */
    protected @Override AnnotatedElement getElement5() throws Throwable {
        return AnnotatedCtor.class.getConstructor(long.class);
    }

    /**
     * Provides an instance to be tested. The instance must be annotated
     * by the MissingTypeAntn.
     */
    protected @Override AnnotatedElement getElement6() throws Throwable {
        return AnnotatedCtor.class.getConstructor(char.class);
    }

    
    @Override
    protected Member getParamElement1() throws Throwable {
        return AnnotatedParamCtor.class.getConstructor();
    }
    
    @Override
    protected Member getParamElement2() throws Throwable {
        return AnnotatedParamCtor.class.getConstructor( 
                Object.class, Class.class, boolean.class);
    }
    
    @Override
    protected Member getParamElement3() throws Throwable {
        return AnnotatedParamCtor.class.getConstructor( 
                String.class, int.class);
    }
    
    static class A {
        private Object obj;
        class InA {
            Object o = obj; 
        }
    }
    
    enum E { E1, E2, E3}
    
    static class B {
        public B() {}
        public B(Object o) {}
        public B(Object o1, int... i) {}
        public B(Object[] o) {}
    }
        
    /**
     * isSynthetic() should return true if and only if
     * the target c-tor does not appear in the source code. 
     */
    public void testIsSynthetic() throws Exception {
        assertFalse("case1.1: ordinary ctor", 
                AnnotatedCtor.class.getConstructor().isSynthetic());
        assertFalse("case1.2: implicit default c-tor", 
                A.class.getDeclaredConstructor().isSynthetic());
        
        Constructor[] cs = B.class.getConstructors();
        for (Constructor<?> c : cs){
            assertFalse("case2: " + c, c.isSynthetic());
        }
        // XXX how to force synthetic c-tor ?
    }
    
    /**
     * isVarArgs() should return true if and only if
     * the target method is declared with varargs. 
     */
    public void testIsVarargs() throws Exception {
        assertFalse("case1: ordinary c-tor", 
                AnnotatedCtor.class.getConstructor().isVarArgs());
        
        assertTrue("case2: varargs c-tor", 
                B.class.getConstructor(Object.class, int[].class).isVarArgs());

        assertFalse("case3: ordinary c-tor", 
                B.class.getConstructor(Object[].class).isVarArgs());
    }
    
    static abstract strictfp class C {
        protected  <T extends Throwable & Comparable<Throwable>> C(T num, OneParamType<? super T> l) throws T, Error {}
    }

    /**
     * toGenericString() should return a string exactly matching
     * the API specification.
     */
    public void testToGenericString() throws Exception {
        String s = C.class.getDeclaredConstructor(Throwable.class, 
                OneParamType.class).toGenericString();
        System.out.println(s);
        assertEquals(
                // Should constructor type parameter be followed by a type bound? It is unspecified.
                // The known reference implementation doesn't do it as well:
                //"protected <T extends java.lang.Throwable & "
                //+ "java.lang.Comparable<java.lang.Throwable>>"
                "protected strictfp <T>"
                + " java.lang.reflect.Ctor5Test$C(T,java.lang.reflect.OneParamType<? super T>)"
                + " throws T,java.lang.Error",
                s);
    }
    
    /**
     * toGenericString() should return a string exactly matching
     * the API specification.
     */
    public void testToGenericString2() throws Exception {
        String s = B.class.getConstructor(Object.class, int[].class).toGenericString();
        System.out.println(s);
        assertEquals("public java.lang.reflect.Ctor5Test$B("
                + "java.lang.Object,int[])", s);
    }

}

class AnnotatedCtor {
    @TagAntn public AnnotatedCtor(){}
    @TagAntn @ValAntn public AnnotatedCtor(Object param){}
    public AnnotatedCtor(@P1Antn String s){}    
    @notfound.MissingAntn public AnnotatedCtor(int i) {}
    @MissingClassValAntn public AnnotatedCtor(long l){}
    @MissingTypeAntn public AnnotatedCtor(char ch){}
}

class AnnotatedParamCtor {
    @TagAntn public AnnotatedParamCtor(){}
    @ValAntn("abc") public AnnotatedParamCtor(@P1Antn Object p1, 
            @P2Antn(123) Class p2, @P3Antn @ValAntn("xyz") boolean p3){}
    public AnnotatedParamCtor(String s, @TagAntn int i){}
}
