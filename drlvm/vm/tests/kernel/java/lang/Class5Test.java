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
package java.lang;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

import notfound.MissingAntn;

import org.apache.harmony.lang.AnnotatedElementTestFrame;
import org.apache.harmony.lang.AnnotatedElementTestFrame.TagAntn;
import org.apache.harmony.lang.AnnotatedElementTestFrame.ValAntn;

/**
 * Test of the {@link java.lang.reflect.AnnotatedElement AnnotatedElement} 
 * functionality in {@link java.lang.Class java.lang.Class} class.
 * 
 * @author Alexey V. Varlamov
 */ 
public class Class5Test extends AnnotatedElementTestFrame {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Class5Test.class);
    }
    
    @TagAntn interface First {}

    protected @Override AnnotatedElement getElement1() throws Throwable {
        return First.class;
    }

    protected @Override AnnotatedElement getElement2() throws Throwable {
        @TagAntn @ValAntn class Second {}
        return Second.class;
    }

    protected @Override AnnotatedElement getElement3() throws Throwable {
        return Third.class;
    }

    protected @Override AnnotatedElement getElement4() throws Throwable {
        @MissingAntn class Fourth {} 
        return Fourth.class;
    }

    protected @Override AnnotatedElement getElement5() throws Throwable {
        @MissingClassValAntn class X {} 
        return X.class;
    }
    
    protected @Override AnnotatedElement getElement6() throws Throwable {
        @MissingTypeAntn class X {} 
        return X.class;
    }

    /**
     * For Class instances, getAnnotations() must return
     * annotations declared + inherited from superclasses.
     */
    public void testGetAnnotationsInherited() throws Throwable {
        Annotation[] an = C4.class.getDeclaredAnnotations();
        assertNotNull("declared in C4", an);
        assertEquals("number of Declared Annotations in C4", 1, an.length);
        assertSame("declared in C4", TagAntn.class, an[0].annotationType());

        Annotation[] an2 = C4.class.getAnnotations();
        assertNotNull("all in C4", an2);
        assertEquals("number of all Annotations in C4", 3, an2.length);
        
        List<Class> la = Arrays.asList(new Class[] {
                an2[0].annotationType(), an2[1].annotationType(), 
                an2[2].annotationType()});
        assertTrue("1st annotation", la.contains(SuperTagAntn.class));
        assertTrue("2nd annotation", la.contains(TagAntn.class));
        assertTrue("3rd annotation", la.contains(SuperValAntn.class));
    }
    
    /**
     * For Class instances, getAnnotations() 
     * must not return annotations inherited from superinterfaces.
     */
    public void testGetAnnotationsInherited2() throws Throwable {
        Annotation[] an = I3.class.getAnnotations();
        assertNotNull("all in I3", an);
        assertEquals("number of Annotations in I3", 1, an.length);
        assertSame("annotation of I3", TagAntn.class, an[0].annotationType());

        Annotation[] an2 = CI1.class.getAnnotations();
        assertNotNull("all in CI1", an2);
        assertEquals("number of all Annotations in CI1", 1, an2.length);
        assertSame("annotation of CI1", ValAntn.class, an2[0].annotationType());
        
        Annotation[] an3 = CI2.class.getAnnotations();
        assertNotNull("all in CI2", an3);
        assertEquals("number of all Annotations in CI2", 1, an3.length);
        assertSame("annotation of CI2", SuperValAntn.class, an3[0].annotationType());
        assertSame("annotation value of CI2", CI2.class, 
                ((SuperValAntn)an3[0]).value());
    }
    
    /**
     * For Class instances, annotations inherited from a superclass
     * may be overriden by descendants. 
     * In this case getAnnotations() must return
     * annotations declared + non-overriden inherited.
     */
    public void testGetAnnotationsInheritedOverriden() throws Throwable {
        Annotation[] an = C6.class.getDeclaredAnnotations();
        assertNotNull("declared in C6", an);
        assertEquals("number of Declared Annotations in C6", 1, an.length);
        assertSame("declared in C6", SuperValAntn.class, an[0].annotationType());

        Annotation[] an2 = C6.class.getAnnotations();
        assertNotNull("all in C6", an2);
        assertEquals("number of all Annotations in C6", 2, an2.length);
        
        List<Class> la = Arrays.asList(new Class[] {
                an2[0].annotationType(), an2[1].annotationType(), });
        assertTrue("C6 1st annotation", la.contains(SuperTagAntn.class));
        assertTrue("C6 2rd annotation", la.contains(SuperValAntn.class));

        Annotation[] an3 = C7.class.getAnnotations();
        assertNotNull("declared in C7", an3);
        assertEquals("number of all Annotations in C7", 2, an3.length);
        
        List<Class> la3 = Arrays.asList(new Class[] {
                an3[0].annotationType(), an3[1].annotationType(), });
        assertTrue("C7 1st annotation", la3.contains(SuperTagAntn.class));
        assertTrue("C7 2rd annotation", la3.contains(SuperValAntn.class));
    }
    
    /**
     * For Class instances, isAnnotationPresent() must account for
     * annotations declared + inherited from superclasses.
     */
    public void testIsAnnotationPresentInherited() throws Throwable {
        assertTrue("case 1", C4.class.isAnnotationPresent(SuperTagAntn.class));
        assertTrue("case 2", C4.class.isAnnotationPresent(SuperValAntn.class));
        assertTrue("case 3", C4.class.isAnnotationPresent(TagAntn.class));
        assertTrue("case 4", C6.class.isAnnotationPresent(SuperTagAntn.class));
        assertTrue("case 5", C6.class.isAnnotationPresent(SuperValAntn.class));
        assertTrue("case 6", C5.class.isAnnotationPresent(SuperTagAntn.class));
    }

    /**
     * For Class instances, isAnnotationPresent() must not account for
     * non-inheritable annotations from superclasses.
     */    
    public void testIsAnnotationPresentInherited_Negative() throws Throwable {
        assertFalse("case 1", C4.class.isAnnotationPresent(ValAntn.class));
        assertFalse("case 2", C4.class.isAnnotationPresent(None.class));
        assertFalse("case 3", C5.class.isAnnotationPresent(TagAntn.class));
        assertFalse("case 4", C6.class.isAnnotationPresent(TagAntn.class));
        assertFalse("case 5", C6.class.isAnnotationPresent(ValAntn.class));
        assertFalse("case 6", C5.class.isAnnotationPresent(None.class));
    }
    
    /**
     * For Class instances, getAnnotation() must account for
     * annotations declared + inherited from superclasses.
     */
    public void testGetAnnotationInherited() throws Throwable {
        SuperValAntn an = C5.class.getAnnotation(SuperValAntn.class);
        assertNotNull(an);
        assertSame("value of inherited annotation", C3.class, an.value());
    }
    
    /**
     * For Class instances, annotations inherited from a superclass
     * may be overriden by descendants. 
     * In this case getAnnotation() must return latest overriden annotation.
     */
    public void testGetAnnotationInheritedOverriden() throws Throwable {
        SuperValAntn an = C6.class.getAnnotation(SuperValAntn.class);
        assertNotNull("overriden in C6", an);
        assertSame("value of overriden annotation in C6", C6.class, an.value());

        SuperValAntn an2 = C7.class.getAnnotation(SuperValAntn.class);
        assertNotNull("overriden in C7", an2);
        assertSame("value of overriden annotation in C7", C6.class, an2.value());
    }
}

@interface Third {}

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@interface SuperTagAntn {}

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@interface SuperValAntn {
    Class value();
}

@SuperTagAntn class C1 {}
@ValAntn class C2 extends C1 {}
@SuperValAntn(C3.class) class C3 extends C2 {}
@TagAntn class C4 extends C3 {}
class C5 extends C4 {}
@SuperValAntn(C6.class) class C6 extends C5 {} 
class C7 extends C6 {}

@SuperTagAntn interface I1 {}
@SuperValAntn(I2.class) interface I2 {}
@TagAntn interface I3 extends I1, I2 {}
@ValAntn class CI1 implements I3 {}
@SuperValAntn(CI2.class) class CI2 extends CI1 {}
