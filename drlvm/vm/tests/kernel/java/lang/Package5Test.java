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
import java.lang.pkg3.Pkg3Antn;
import java.lang.pkg3.pkg31.Pkg31Antn;
import java.lang.reflect.AnnotatedElement;

import org.apache.harmony.lang.AnnotatedElementTestFrame;

/**
 * @author Alexey V. Varlamov
 */ 
public class Package5Test extends AnnotatedElementTestFrame {
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(Package5Test.class);
    }
    
    static {
        try {
            Class.forName("java.lang.pkg1.Bogus");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            Class.forName("java.lang.pkg2.Bogus");
        } catch (Exception e) {
            throw new RuntimeException(e);    
        }
        try {
            Class.forName("java.lang.pkg4.Bogus");
        } catch (Exception e) {
            throw new RuntimeException(e);    
        }
        try {
            Class.forName("java.lang.pkg5.Bogus");
        } catch (Exception e) {
            throw new RuntimeException(e);    
        }
        try {
            Class.forName("java.lang.pkg6.Bogus");
        } catch (Exception e) {
            throw new RuntimeException(e);    
        }
    }

    protected @Override AnnotatedElement getElement1() throws Throwable {
        Package p = Package.getPackage("java.lang.pkg1");
        assertNotNull("failed to get annotated pkg1", p);
        return p;
    }

    protected @Override AnnotatedElement getElement2() throws Throwable {
        Package p = Package.getPackage("java.lang.pkg2");
        assertNotNull("failed to get annotated pkg2", p);
        return p;
    }

    protected @Override AnnotatedElement getElement3() throws Throwable {
        Package p = Package.getPackage("java.lang");
        assertNotNull("failed to get package", p);
        return p;
    }

    protected @Override AnnotatedElement getElement4() throws Throwable {
        Package p = Package.getPackage("java.lang.pkg4");
        assertNotNull("failed to get annotated pkg5", p);
        return p;
    }

    protected @Override AnnotatedElement getElement5() throws Throwable {
        Package p = Package.getPackage("java.lang.pkg5");
        assertNotNull("failed to get annotated pkg5", p);
        return p;
    }

    protected @Override AnnotatedElement getElement6() throws Throwable {
        Package p = Package.getPackage("java.lang.pkg6");
        assertNotNull("failed to get annotated pkg6", p);
        return p;
    }

    /**
     * Package should not be awared of annotations of nested 
     * or &quot;super&quot; packages.
     */
    public void testNoInheritance() throws Throwable {
        Class.forName("java.lang.pkg3.Bogus");
        Class.forName("java.lang.pkg3.pkg31.Bogus");
        Package pkg3 = Package.getPackage("java.lang.pkg3");
        assertNotNull("pkg3", pkg3);
        Annotation[] an = pkg3.getAnnotations();
        assertNotNull("all in pkg3", an);
        assertEquals("number of Annotations in pkg3", 1, an.length);
        assertNotNull("annotation of pkg3", pkg3.getAnnotation(Pkg3Antn.class));

        Package pkg31 = Package.getPackage("java.lang.pkg3.pkg31");
        assertNotNull("pkg31", pkg31);
        Annotation[] an2 = pkg31.getAnnotations();
        assertNotNull("all in pkg31", an2);
        assertEquals("number of Annotations in pkg31", 1, an2.length);
        assertTrue("annotation of pkg31", pkg31.isAnnotationPresent(Pkg31Antn.class));
    }
}
