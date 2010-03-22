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
package org.apache.harmony.lang;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

import notfound.MissingAntn;
import notfound.MissingClass;
import notfound.MissingEnum;

import junit.framework.TestCase;

/**
 * Basic framework for testing implementations of the 
 * {@link java.lang.reflect.AnnotatedElement AnnotatedElement} interface.
 * Expected usage: concrete test extending this class should only provide 
 * objects to be tested, via realization of several abstract methods.
 * @see #getElement1()
 * @see #getElement2()
 * @see #getElement3()
 * @see #getElement4()  
 * @see #getElement5()
 * @see #getElement6()
 * 
 * @author Alexey V. Varlamov
 */
public abstract class AnnotatedElementTestFrame extends TestCase {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TagAntn {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ValAntn {
        String value() default "<unspecified>";
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    public @interface None{}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface MissingClassValAntn {
        Class clss() default MissingClass.class;
        Class[] clssArray() default MissingClass.class;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface MissingTypeAntn {
        MissingAntn antn() default @MissingAntn;
        MissingEnum enm() default MissingEnum.A;
        MissingAntn[] antnArray() default @MissingAntn;
        MissingEnum[] enmArray() default MissingEnum.A;
    }

    /**
     * Provides an instance to be tested. The instance must be annotated
     * exactly by the single type {@link TagAntn TagAntn}.
     */
    protected abstract AnnotatedElement getElement1() throws Throwable;

    /**
     * Provides an instance to be tested. The instance must be annotated
     * exactly by the two types {@link TagAntn TagAntn} and {@link ValAntn ValAntn}.
     */
    protected abstract AnnotatedElement getElement2() throws Throwable;

    /**
     * Provides an instance to be tested. The instance must not be annotated.
     */
    protected abstract AnnotatedElement getElement3() throws Throwable;

    /**
     * Provides an instance to be tested. The instance must be annotated
     * by the notfound.MissingAntn.
     */
    protected abstract AnnotatedElement getElement4() throws Throwable;

    /**
     * Provides an instance to be tested. The instance must be annotated
     * by the MissingClassValAntn.
     */
    protected abstract AnnotatedElement getElement5() throws Throwable;

    /**
     * Provides an instance to be tested. The instance must be annotated
     * by the MissingTypeAntn.
     */
    protected abstract AnnotatedElement getElement6() throws Throwable;

    /**
     * getAnnotation(Class) should return
     * an annotation presented on the element1.
     */
    public void testGetAnnotation() throws Throwable {
        Annotation an = getElement1().getAnnotation(TagAntn.class);
        assertNotNull(an);
        assertSame(TagAntn.class, an.annotationType());
    }
    
    /**
     * getAnnotation(Class) should return
     * an annotation presented on the element2.
     */
    public void testGetAnnotation2() throws Throwable {
        Annotation an = getElement2().getAnnotation(ValAntn.class);
        assertNotNull(an);
        assertSame(ValAntn.class, an.annotationType());
    }

    /**
     * getAnnotation(Class) should return
     * null for unexpected annotation type.
     */
    public void testGetAnnotation_Negative() throws Throwable {
        assertNull("case 1", getElement2().getAnnotation(None.class));
        assertNull("case 2", getElement2().getAnnotation(None.class));
    }

    /**
     * getAnnotation(Class) should return
     * null for non-annotated instance.
     */
    public void testGetAnnotation_Negative2() throws Throwable {
        assertNull("case 1", getElement3().getAnnotation(TagAntn.class));
        assertNull("case 2", getElement3().getAnnotation(None.class));
    }
    
    /**
     * getAnnotation(Class) should throw NPE on null argument.
     */
    public void testGetAnnotation_Null() throws Throwable {
        try {
            getElement1().getAnnotation(null);
            fail("failed to throw NPE");
        } catch (NullPointerException ok) {}
    }

    /**
     * getDeclaredAnnotations() should return
     * all annotations presented on the element1.
     */
    public void testGetDeclaredAnnotations() throws Throwable {
        Annotation[] an = getElement1().getDeclaredAnnotations();
        assertNotNull(an);
        assertEquals("number of Declared Annotations", 1, an.length);
        assertSame(TagAntn.class, an[0].annotationType());
    }
    
    /**
     * getDeclaredAnnotations() should return
     * all annotations presented on the element2.
     */
    public void testGetDeclaredAnnotations2() throws Throwable {
        Annotation[] an = getElement2().getDeclaredAnnotations();
        assertNotNull(an);
        assertEquals("number of Declared Annotations", 2, an.length);
        List<Class> la = Arrays.asList(new Class[] {
                an[0].annotationType(), an[1].annotationType()});
        assertTrue("1st annotation", la.contains(TagAntn.class));
        assertTrue("2nd annotation", la.contains(ValAntn.class));
    }
    
    /**
     * getDeclaredAnnotations() should return
     * empty array for the element3.
     */
    public void testGetDeclaredAnnotations3() throws Throwable {
        Annotation[] an = getElement3().getDeclaredAnnotations();
        assertNotNull(an);
        assertEquals(0, an.length);
    }

    /**
     * getDeclaredAnnotations() should return cloned array
     * which can be safely modified by a caller.
     */
    public void testGetDeclaredAnnotationsImmutable() throws Throwable {
        AnnotatedElement el = getElement1();
        Annotation[] an = el.getDeclaredAnnotations();
        assertNotNull(an);
        assertEquals("number of Declared Annotations", 1, an.length);
        assertSame(TagAntn.class, an[0].annotationType());
        an[0] = null;
        Annotation[] an2 = el.getDeclaredAnnotations();
        assertNotNull(an2);
        assertEquals("number of second Declared Annotations", 1, an2.length);
        assertNotNull("array is not immutable", an2[0]);
        assertSame(TagAntn.class, an2[0].annotationType());
    }
    
    /**
     * getAnnotations() should return
     * all annotations presented on the element1.
     */
    public void testGetAnnotations() throws Throwable {
        Annotation[] an = getElement1().getAnnotations();
        assertNotNull(an);
        assertEquals("number of Annotations", 1, an.length);
        assertSame(TagAntn.class, an[0].annotationType());
    }
    
    /**
     * getAnnotations() should return
     * all annotations presented on the element2.
     */
    public void testGetAnnotations2() throws Throwable {
        Annotation[] an = getElement2().getAnnotations();
        assertNotNull(an);
        assertEquals("number of Annotations", 2, an.length);
        List<Class> la = Arrays.asList(new Class[] {
                an[0].annotationType(), an[1].annotationType()});
        assertTrue("1st annotation", la.contains(TagAntn.class));
        assertTrue("2nd annotation", la.contains(ValAntn.class));
    }
    
    /**
     * getAnnotations() should return
     * empty array for the element3.
     */
    public void testGetAnnotations3() throws Throwable {
        Annotation[] an = getElement3().getAnnotations();
        assertNotNull(an);
        assertEquals(0, an.length);
    }
    
    /**
     * getAnnotations() should skip unresolved annotation
     * thus should return empty array for the element4.
     */
    public void testGetAnnotations4() throws Throwable {
        Annotation[] an = getElement4().getAnnotations();
        assertNotNull(an);
        assertEquals(0, an.length);
    }
    
    /**
     * getAnnotations() should return
     * all annotations presented on the element5.
     */
    public void testGetAnnotations5() throws Throwable {
        Annotation[] an = getElement5().getAnnotations();
        assertNotNull(an);
        assertEquals("number of Annotations", 1, an.length);
        assertSame(MissingClassValAntn.class, an[0].annotationType());
    }
    
    /**
     * getAnnotations() should throw NoClassDefFoundError
     * for the element6.
     */
    public void testGetAnnotations6() throws Throwable {
        try {
            getElement6().getAnnotations();
            fail("Misconfigured test");
        } catch (TypeNotPresentException tnpe) {
            assertTrue("reported type name: " + tnpe.typeName(), 
                    tnpe.typeName().matches("notfound.Missing(.)+"));
        } catch (NoClassDefFoundError e) {
            assertTrue("reported type name: " + e.getMessage(), 
                    e.getMessage().matches("notfound.Missing(.)+"));
        }
    }

    /**
     * getAnnotations() should return cloned array
     * which can be safely modified by a caller.
     */
    public void testGetAnnotationsImmutable() throws Throwable {
        AnnotatedElement el = getElement1();
        Annotation[] an = el.getAnnotations();
        assertNotNull(an);
        assertEquals("number of Annotations", 1, an.length);
        assertSame(TagAntn.class, an[0].annotationType());
        an[0] = null;
        Annotation[] an2 = el.getAnnotations();
        assertNotNull(an2);
        assertEquals("number of second Annotations", 1, an2.length);
        assertNotNull("array is not immutable", an2[0]);
        assertSame(TagAntn.class, an2[0].annotationType());
    }
    
    /**
     * isAnnotationPresent(Class) should return true
     * for the annotation(s) presented.
     */
    public void testIsAnnotationPresent() throws Throwable {
        assertTrue("case 1", getElement1().isAnnotationPresent(TagAntn.class));
        assertTrue("case 2", getElement2().isAnnotationPresent(TagAntn.class));
        assertTrue("case 3", getElement2().isAnnotationPresent(ValAntn.class));
    }
        
    /**
     * isAnnotationPresent(Class) should return false
     * for the annotation(s) not presented.
     */
    public void testIsAnnotationPresent_Negative() throws Throwable {
        assertFalse("case 1", getElement1().isAnnotationPresent(ValAntn.class));
        assertFalse("case 2", getElement1().isAnnotationPresent(None.class));
        assertFalse("case 3", getElement2().isAnnotationPresent(None.class));
        assertFalse("case 4", getElement3().isAnnotationPresent(TagAntn.class));
    }
    
    /**
     * isAnnotationPresent(Class) should throw NPE on null argument
     */
    public void testIsAnnotationPresent_Null() throws Throwable {
        try {
            getElement1().isAnnotationPresent(null);
            fail("failed to throw NPE");
        } catch (NullPointerException ok) {}
    }
}
