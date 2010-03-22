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
 * @author Serguei S.Zapreyev
 */

package java.lang;

/**
 *   Areas for supposed testing a classes' hierarchy
 *   and new 1.5 getEnclosingClass, getEnumConstants, isEnum, asSubclass,
 *   getEnclosingMethod, getEnclosingConstructor, isMemberClass, isLocalClass,
 *   isAnonymousClass, isSynthetic, getCanonicalName, getSimpleName methods:
 *   I.   member, inner, nested, local, annonymous, proxy, synthetic classes;
 *        different packages classes;
 *        casting, naming, IS...ing, GET...ing, enums, ...;
 *   II.  member interfaces;
 *   III. the same for annotations and annotated;
 *   IV.  the same for enums;
 *   V.   the same for generalized.
 **/

import java.lang.annotation.Annotation;

import junit.framework.TestCase;

/*
 * Created on April 03, 2006
 *
 * This ClassHierarchyTest class is used to test the Core API 1.5 Class class
 * 
 */

/**
 * ###############################################################################
 * ###############################################################################
 * TODO: 1.
 * ###############################################################################
 * ###############################################################################
 */
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.GenericDeclaration;

@Retention(value=RetentionPolicy.RUNTIME)
@interface i{
    abstract String author() default "Zapreyev";
};

@SuppressWarnings(value={"unchecked"}) public class ClassHierarchyTest extends TestCase {
    @i public class MC001 {
        @i public class MC001_01 {
            @i public class MC001_01_01 {
                @i public class MC001_01_01_01 {
                    @i public class MC001_01_01_01_01 {
                        @i public class MC001_01_01_01_01_01 {
                            @i public class MC001_01_01_01_01_01_01 {                              
                            };
                        };
                    };
                };
            };
        };
    };
    
    @Retention(value=RetentionPolicy.RUNTIME)
    @interface anna{
        abstract String author() default "Zapreyev";
    };
    @i public class MC002<X, Y, Z> {
        @i public class MC002_01<X1, Y1, Z1> {
            @i public class MC002_01_01<X2, Y2, Z2> {
                @i public class MC002_01_01_01<X3, Y3, Z3> {
                    @i public class MC002_01_01_01_01<X4, Y4, Z4> {
                        @i public class MC002_01_01_01_01_01<X5, Y5, Z5> {
                            @i public class MC002_01_01_01_01_01_01<X6, Y6, Z6> {                              
                            };
                        };
                    };
                };
            };
        };
    };

    @Retention(value=RetentionPolicy.RUNTIME)
    @interface ii{
        abstract String author() default "Zapreyev";
        @Retention(value=RetentionPolicy.RUNTIME)
        @interface iii{
            abstract String author() default "Zapreyev";
        };
    };
    @i interface MI001 {
        @i interface MI001_01 {
            @i interface MI001_01_01 {
                @i interface MI001_01_01_01 {
                    @i interface MI001_01_01_01_01 {
                        @i interface MI001_01_01_01_01_01 {
                            @i interface MI001_01_01_01_01_01_01 {                              
                                @Retention(value=RetentionPolicy.RUNTIME)
                                @interface iiii{
                                    abstract String author() default "Zapreyev";
                                };
                            };
                        };
                    };
                };
            };
        };
    };

    @i interface MI002<X, Y, Z> {
        @i interface MI002_01<X1, Y1, Z1> {
            @i interface MI002_01_01<X2, Y2, Z2> {
                @i interface MI002_01_01_01<X3, Y3, Z3> {
                    @i interface MI002_01_01_01_01<X4, Y4, Z4> {
                        @i interface MI002_01_01_01_01_01<X5, Y5, Z5> {
                            @i interface MI002_01_01_01_01_01_01<X6, Y6, Z6> {                              
                                @Retention(value=RetentionPolicy.RUNTIME)
                                @interface iiii{
                                    abstract String author() default "Zapreyev";
                                };
                            };
                        };
                    };
                };
            };
        };
    };

    @Retention(value=RetentionPolicy.RUNTIME)
    @interface MA001 {
        @Retention(value=RetentionPolicy.RUNTIME)
        @interface MA001_01 {
            @Retention(value=RetentionPolicy.RUNTIME)
            @interface MA001_01_01 {
                @Retention(value=RetentionPolicy.RUNTIME)
                @interface MA001_01_01_01 {
                    @Retention(value=RetentionPolicy.RUNTIME)
                    @interface MA001_01_01_01_01 {
                        @Retention(value=RetentionPolicy.RUNTIME)
                        @interface MA001_01_01_01_01_01 {
                            @Retention(value=RetentionPolicy.RUNTIME)
                            @interface MA001_01_01_01_01_01_01 {                              
                                @Retention(value=RetentionPolicy.RUNTIME)
                                @interface iiii{
                                    abstract String author() default "Zapreyev 1";
                                };
                                abstract String author() default "Zapreyev 2";
                            };
                            abstract String author() default "Zapreyev 3";
                        };
                        abstract String author() default "Zapreyev 4";
                    };
                    abstract String author() default "Zapreyev 5";
                };
                abstract String author() default "Zapreyev 6";
            };
            abstract String author() default "Zapreyev 7";
        };
        abstract String author() default "Zapreyev 8";
    };
    
//===================================================================================================
//===================================================================================================
//===================================================================================================

///////////////////////// I:    
    /**
     * member classes
     */
    public void test_1() {
        if(!MC001.class.getEnclosingClass().equals(ClassHierarchyTest.class)) fail("test_1, case 004 FAILED: "+MC001.class.getEnclosingClass());
        if(MC001.class.getEnumConstants()!=null) fail("test_1, case 009 FAILED: "+MC001.class.getEnumConstants());
        if(MC001.class.isEnum()) fail("test_1, case 000 FAILED: "+MC001.class.isEnum());
        try{MC001.class.asSubclass(ClassHierarchyTest.class); fail("test_1, case 011 FAILED: "+MC001.class.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
        if(MC001.class.getEnclosingMethod()!=null) fail("test_1, case 013 FAILED: "+MC001.class.getEnclosingMethod());
        if(MC001.class.getEnclosingConstructor()!=null) fail("test_1, case 014 FAILED: "+MC001.class.getEnclosingConstructor());
        if(!MC001.class.isMemberClass()) fail("test_1, case 017 FAILED: "+MC001.class.isMemberClass());
        if(MC001.class.isLocalClass()) fail("test_1, case 018 FAILED: "+MC001.class.isLocalClass());
        if(MC001.class.isAnonymousClass()) fail("test_1, case 019 FAILED: "+MC001.class.isAnonymousClass());
        if(MC001.class.isSynthetic()) fail("test_1, case 020 FAILED: "+MC001.class.isSynthetic());
        if(!MC001.class.getCanonicalName().equals("java.lang.ClassHierarchyTest.MC001")) fail("test_1, case 021 FAILED: "+MC001.class.getCanonicalName());
        if(!MC001.class.getSimpleName().equals("MC001")) fail("test_1, case 022 FAILED: "+MC001.class.getSimpleName());
    }
    
    /**
     * member interface
     */
    public void test_1_1() {
        if(!MI001.class.getEnclosingClass().equals(ClassHierarchyTest.class)) fail("test_1, case 004 FAILED: "+MI001.class.getEnclosingClass());
        if(MI001.class.getEnumConstants()!=null) fail("test_1, case 009 FAILED: "+MI001.class.getEnumConstants());
        if(MI001.class.isEnum()) fail("test_1, case 000 FAILED: "+MI001.class.isEnum());
        try{MI001.class.asSubclass(ClassHierarchyTest.class); fail("test_1, case 011 FAILED: "+MI001.class.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
        if(MI001.class.getEnclosingMethod()!=null) fail("test_1, case 013 FAILED: "+MI001.class.getEnclosingMethod());
        if(MI001.class.getEnclosingConstructor()!=null) fail("test_1, case 014 FAILED: "+MI001.class.getEnclosingConstructor());
        if(!MI001.class.isMemberClass()) fail("test_1, case 017 FAILED: "+MI001.class.isMemberClass());
        if(MI001.class.isLocalClass()) fail("test_1, case 018 FAILED: "+MI001.class.isLocalClass());
        if(MI001.class.isAnonymousClass()) fail("test_1, case 019 FAILED: "+MI001.class.isAnonymousClass());
        if(MI001.class.isSynthetic()) fail("test_1, case 020 FAILED: "+MI001.class.isSynthetic());
        if(!MI001.class.getCanonicalName().equals("java.lang.ClassHierarchyTest.MI001")) fail("test_1, case 021 FAILED: "+MI001.class.getCanonicalName());
        if(!MI001.class.getSimpleName().equals("MI001")) fail("test_1, case 022 FAILED: "+MI001.class.getSimpleName());
    }
   
    /**
     * deeply nested member classes
     */
    public void test_2() {
        Class cuCla = MC001.MC001_01.MC001_01_01.MC001_01_01_01.MC001_01_01_01_01.MC001_01_01_01_01_01.MC001_01_01_01_01_01_01.class;
        String caNa = cuCla.getCanonicalName();
        //String name[] = caNa.split("\\$");
        String name[] = caNa.split("\\.");
        int i = name.length - 1;
        while (cuCla != null) {
            if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[i])) fail("test_2, case 002 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
            if(cuCla.getEnumConstants()!=null) fail("test_2, case 009 FAILED: "+cuCla.getEnumConstants());
            if(cuCla.isEnum()) fail("test_2, case 000 FAILED: "+cuCla.isEnum());
            try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassHierarchyTest")) fail("test_2, case 011 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
            if(cuCla.getEnclosingMethod()!=null) fail("test_2, case 013 FAILED: "+cuCla.getEnclosingMethod());
            if(cuCla.getEnclosingConstructor()!=null) fail("test_2, case 014 FAILED: "+cuCla.getEnclosingConstructor());
            if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_1, case 017 FAILED: "+cuCla.isMemberClass());
            if(cuCla.isLocalClass()) fail("test_2, case 018 FAILED: "+cuCla.isLocalClass());
            if(cuCla.isAnonymousClass()) fail("test_2, case 019 FAILED: "+cuCla.isAnonymousClass());
            if(cuCla.isSynthetic()) fail("test_2, case 020 FAILED: "+cuCla.isSynthetic());
            if(!cuCla.getCanonicalName().equals(caNa)) fail("test_2, case 021 FAILED: "+cuCla.getCanonicalName());
            caNa = caNa.substring(0, caNa.lastIndexOf('.'));
            if(!cuCla.getSimpleName().equals(name[i])) fail("test_2, case 022 FAILED: "+cuCla.getSimpleName());
            i--;
            cuCla = cuCla.getEnclosingClass();
        }
    }
    
    /**
     * deeply nested member interfaces
     */
    public void test_2_1() {
        Class cuCla = MI001.MI001_01.MI001_01_01.MI001_01_01_01.MI001_01_01_01_01.MI001_01_01_01_01_01.MI001_01_01_01_01_01_01.class;
        String caNa = cuCla.getCanonicalName();
        //String name[] = caNa.split("\\$");
        String name[] = caNa.split("\\.");
        int i = name.length - 1;
        while (cuCla != null) {
            if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[i])) fail("test_2, case 002 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
            if(cuCla.getEnumConstants()!=null) fail("test_2, case 009 FAILED: "+cuCla.getEnumConstants());
            if(cuCla.isEnum()) fail("test_2, case 000 FAILED: "+cuCla.isEnum());
            try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassHierarchyTest")) fail("test_2, case 011 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
            if(cuCla.getEnclosingMethod()!=null) fail("test_2, case 013 FAILED: "+cuCla.getEnclosingMethod());
            if(cuCla.getEnclosingConstructor()!=null) fail("test_2, case 014 FAILED: "+cuCla.getEnclosingConstructor());
            if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_1, case 017 FAILED: "+cuCla.isMemberClass());
            if(cuCla.isLocalClass()) fail("test_2, case 018 FAILED: "+cuCla.isLocalClass());
            if(cuCla.isAnonymousClass()) fail("test_2, case 019 FAILED: "+cuCla.isAnonymousClass());
            if(cuCla.isSynthetic()) fail("test_2, case 020 FAILED: "+cuCla.isSynthetic());
            if(!cuCla.getCanonicalName().equals(caNa)) fail("test_2, case 021 FAILED: "+cuCla.getCanonicalName());
            caNa = caNa.substring(0, caNa.lastIndexOf('.'));
            if(!cuCla.getSimpleName().equals(name[i])) fail("test_2, case 022 FAILED: "+cuCla.getSimpleName());
            i--;
            cuCla = cuCla.getEnclosingClass();
        }
    }

    /**
     * unnested local class
     */
    public void test_3() {
        @i class LC001 {};
        if(!LC001.class.getEnclosingClass().equals(ClassHierarchyTest.class)) fail("test_3, case 004 FAILED: "+LC001.class.getEnclosingClass());
        if(LC001.class.getEnumConstants()!=null) fail("test_3, case 009 FAILED: "+LC001.class.getEnumConstants());
        if(LC001.class.isEnum()) fail("test_3, case 000 FAILED: "+LC001.class.isEnum());
        try{LC001.class.asSubclass(ClassHierarchyTest.class); fail("test_3, case 011 FAILED: "+LC001.class.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
        if(!LC001.class.getEnclosingMethod().getName().equals("test_3")) fail("test_3, case 013 FAILED: "+LC001.class.getEnclosingMethod().getName());
        if(LC001.class.getEnclosingConstructor()!=null) fail("test_3, case 014 FAILED: "+LC001.class.getEnclosingConstructor());
        if(LC001.class.isMemberClass()) fail("test_3, case 017 FAILED: "+LC001.class.isMemberClass());
        if(!LC001.class.isLocalClass()) fail("test_3, case 018 FAILED: "+LC001.class.isLocalClass());
        if(LC001.class.isAnonymousClass()) fail("test_3, case 019 FAILED: "+LC001.class.isAnonymousClass());
        if(LC001.class.isSynthetic()) fail("test_3, case 020 FAILED: "+LC001.class.isSynthetic());
        if(LC001.class.getCanonicalName()!=null) fail("test_3, case 021 FAILED: "+LC001.class.getCanonicalName());
        if(!LC001.class.getSimpleName().equals("LC001")) fail("test_3, case 022 FAILED: "+LC001.class.getSimpleName());
    }

    /**
     * local               ClassHierarchyTest$1$LC003$1A.class   (1)
     * anonymous           ClassHierarchyTest$1$LC003$1.class    (2)
     * member of anonymous ClassHierarchyTest$1$LC003$1$A.class  (3)
     * local               ClassHierarchyTest$1$LC003$1AAA.class (4)
     */
    static int f = 0;
    static Object ooo = null;
    public void test_4() { // now it is a class access control bug, so, closed for a while
                            // It's strange that if we use javac instead of ecj
                            // then we have no such problem
                            //   <<< A member of the "class java.lang.ClassHierarchyTest$1" with "" modifiers can not be accessed from the "class java.lang.ClassHierarchyTest">>>
                            // so it should be investigated.
        @i
        class LC002 {
        }
        ;
        class $LC003 {
            public Class value() {
                class A extends $LC003 { // (1)
                }
                ;
                Object o = new LC002() { // (2)
                    class A {            // (3)
                    }
                    Class m1() {
                        return A.class;
                    }
                    Class m2() {
						return m3() == null? m1():m3();
                    }
                    Class m3() {
                        return m2();
                    }
                };
                ooo = o;
                if (f < 1) {
                    f += 1;
                    return A.class;
                }
                f += 1;
                return o.getClass();
            }

            public Class value2() {
                class AAA {             // (4)
                }
                ;
                return AAA.class;
            }
        }
        class X$1Y {
        }
        class X {
            Class m2() {
                class Y { // it has "X$2Y" name!!! So, compiler provides the
                          // difference with the previous "X$1Y"
                }
                return Y.class;
            }
                    Class m4() {
						return m5() == null? m2():m5();
                    }
                    Class m5() {
                        return m4();
                    }
        }
        Class cuCla = new $LC003().value(); // ClassHierarchyTest$1$LC003$1A.class   (1)
        if(!cuCla.getEnclosingClass().equals($LC003.class)) fail("test_4, case 004 FAILED: "+cuCla.getEnclosingClass());
        if(cuCla.getEnumConstants()!=null) fail("test_4, case 009 FAILED: "+cuCla.getEnumConstants());
        if(cuCla.isEnum()) fail("test_4, case 000 FAILED: "+cuCla.isEnum());
        try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_4, case 011 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
        if(!cuCla.getEnclosingMethod().getName().equals("value")) fail("test_4, case 013 FAILED: "+cuCla.getEnclosingMethod().getName());
        if(cuCla.getEnclosingConstructor()!=null) fail("test_4, case 014 FAILED: "+cuCla.getEnclosingConstructor());
        if(cuCla.isMemberClass()) fail("test_4, case 017 FAILED: "+cuCla.isMemberClass());
        if(!cuCla.isLocalClass()) fail("test_4, case 018 FAILED: "+cuCla.isLocalClass());
        if(cuCla.isAnonymousClass()) fail("test_4, case 019 FAILED: "+cuCla.isAnonymousClass());
        if(cuCla.isSynthetic()) fail("test_4, case 020 FAILED: "+cuCla.isSynthetic());
        if(cuCla.getCanonicalName()!=null) fail("test_4, case 021 FAILED: "+cuCla.getCanonicalName());
        if(!cuCla.getSimpleName().equals("A")) fail("test_4, case 022 FAILED: "+cuCla.getSimpleName());
        
        cuCla = new $LC003().value(); // ClassHierarchyTest$1$LC003$1.class    (2)
        if(!cuCla.getEnclosingClass().equals($LC003.class)) fail("test_4, case 023 FAILED: "+cuCla.getEnclosingClass());
        if(cuCla.getEnumConstants()!=null) fail("test_4, case 024 FAILED: "+cuCla.getEnumConstants());
        if(cuCla.isEnum()) fail("test_4, case 025 FAILED: "+cuCla.isEnum());
        try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_4, case 026 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
        if(!cuCla.getEnclosingMethod().getName().equals("value")) fail("test_4, case 027 FAILED: "+cuCla.getEnclosingMethod().getName());
        if(cuCla.getEnclosingConstructor()!=null) fail("test_4, case 028 FAILED: "+cuCla.getEnclosingConstructor());
        if(cuCla.isMemberClass()) fail("test_4, case 029 FAILED: "+cuCla.isMemberClass());
        if(cuCla.isLocalClass()) fail("test_4, case 030 FAILED: "+cuCla.isLocalClass());
        if(!cuCla.isAnonymousClass()) fail("test_4, case 031 FAILED: "+cuCla.isAnonymousClass());
        if(cuCla.isSynthetic()) fail("test_4, case 032 FAILED: "+cuCla.isSynthetic());
        if(cuCla.getCanonicalName()!=null) fail("test_4, case 033 FAILED: "+cuCla.getCanonicalName());
        if(!cuCla.getSimpleName().equals("")) fail("test_4, case 034 FAILED: "+cuCla.getSimpleName());
        
        cuCla = new $LC003().value2(); // ClassHierarchyTest$1$LC003$1AAA.class (4)
        if(!cuCla.getEnclosingClass().equals($LC003.class)) fail("test_4, case 035 FAILED: "+cuCla.getEnclosingClass());
        if(cuCla.getEnumConstants()!=null) fail("test_4, case 036 FAILED: "+cuCla.getEnumConstants());
        if(cuCla.isEnum()) fail("test_4, case 037 FAILED: "+cuCla.isEnum());
        try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_4, case 038 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
        if(!cuCla.getEnclosingMethod().getName().equals("value2")) fail("test_4, case 039 FAILED: "+cuCla.getEnclosingMethod().getName());
        if(cuCla.getEnclosingConstructor()!=null) fail("test_4, case 040 FAILED: "+cuCla.getEnclosingConstructor());
        if(cuCla.isMemberClass()) fail("test_4, case 041 FAILED: "+cuCla.isMemberClass());
        if(!cuCla.isLocalClass()) fail("test_4, case 042 FAILED: "+cuCla.isLocalClass());
        if(cuCla.isAnonymousClass()) fail("test_4, case 043 FAILED: "+cuCla.isAnonymousClass());
        if(cuCla.isSynthetic()) fail("test_4, case 044 FAILED: "+cuCla.isSynthetic());
        if(cuCla.getCanonicalName()!=null) fail("test_4, case 045 FAILED: "+cuCla.getCanonicalName());
        if(!cuCla.getSimpleName().equals("AAA")) fail("test_4, case 046 FAILED: "+cuCla.getSimpleName());
        
        try {
            cuCla = (Class) new $LC003().value().getDeclaredMethod("m1").invoke(ooo, (Object[])null); // ClassHierarchyTest$1$LC003$1$A.class  (3)
            Class tc = Class.forName("java.lang.ClassHierarchyTest$1$LC003$1");
            if(!cuCla.getEnclosingClass().equals(tc)) fail("test_4, case 047 FAILED: "+cuCla.getEnclosingClass());
            if(cuCla.getEnumConstants()!=null) fail("test_4, case 048 FAILED: "+cuCla.getEnumConstants());
            if(cuCla.isEnum()) fail("test_4, case 049 FAILED: "+cuCla.isEnum());
            try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_4, case 050 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
            if(cuCla.getEnclosingMethod()!=null) fail("test_4, case 051 FAILED: "+cuCla.getEnclosingMethod());
            if(cuCla.getEnclosingConstructor()!=null) fail("test_4, case 052 FAILED: "+cuCla.getEnclosingConstructor());
            if(!cuCla.isMemberClass()) fail("test_4, case 053 FAILED: "+cuCla.isMemberClass());
            if(cuCla.isLocalClass()) fail("test_4, case 054 FAILED: "+cuCla.isLocalClass());
            if(cuCla.isAnonymousClass()) fail("test_4, case 055 FAILED: "+cuCla.isAnonymousClass());
            if(cuCla.isSynthetic()) fail("test_4, case 056 FAILED: "+cuCla.isSynthetic());
            if(cuCla.getCanonicalName()!=null) fail("test_4, case 057 FAILED: "+cuCla.getCanonicalName());
            if(!cuCla.getSimpleName().equals("A")) fail("test_4, case 058 FAILED: "+cuCla.getSimpleName());
        } catch(ClassNotFoundException e) {
            fail(e.getMessage());
        } catch(IllegalAccessException e) {
            fail(e.getMessage());
        } catch(IllegalArgumentException e) {
            fail(e.getMessage());
        } catch(java.lang.reflect.InvocationTargetException e) {
            fail(e.getMessage());
        } catch(NoSuchMethodException e) {
            fail(e.getMessage());
        }
        
        cuCla = X$1Y.class; // ClassHierarchyTest$1X$1Y.class  (3)
        if(!cuCla.getEnclosingClass().equals(ClassHierarchyTest.class)) fail("test_4, case 059 FAILED: "+cuCla.getEnclosingClass());
        if(cuCla.getEnumConstants()!=null) fail("test_4, case 060 FAILED: "+cuCla.getEnumConstants());
        if(cuCla.isEnum()) fail("test_4, case 061 FAILED: "+cuCla.isEnum());
        try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_4, case 062 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
        if(!cuCla.getEnclosingMethod().getName().equals("test_4")) fail("test_4, case 063 FAILED: "+cuCla.getEnclosingMethod().getName());
        if(cuCla.getEnclosingConstructor()!=null) fail("test_4, case 064 FAILED: "+cuCla.getEnclosingConstructor());
        if(cuCla.isMemberClass()) fail("test_4, case 065 FAILED: "+cuCla.isMemberClass());
        if(!cuCla.isLocalClass()) fail("test_4, case 066 FAILED: "+cuCla.isLocalClass());
        if(cuCla.isAnonymousClass()) fail("test_4, case 067 FAILED: "+cuCla.isAnonymousClass());
        if(cuCla.isSynthetic()) fail("test_4, case 068 FAILED: "+cuCla.isSynthetic());
        if(cuCla.getCanonicalName()!=null) fail("test_4, case 069 FAILED: "+cuCla.getCanonicalName());
        if(!cuCla.getSimpleName().equals("X$1Y")) fail("test_4, case 070 FAILED: "+cuCla.getSimpleName());
        
        try {
            cuCla = (Class) X.class.getDeclaredMethod("m2").invoke(new X(), (Object[])null); // ClassHierarchyTest$1$LC003$1$A.class  (3)
            if(!cuCla.getEnclosingClass().equals(X.class)) fail("test_4, case 071 FAILED: "+cuCla.getEnclosingClass());
            if(cuCla.getEnumConstants()!=null) fail("test_4, case 072 FAILED: "+cuCla.getEnumConstants());
            if(cuCla.isEnum()) fail("test_4, case 073 FAILED: "+cuCla.isEnum());
            try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_4, case 074 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
            if(!cuCla.getEnclosingMethod().getName().equals("m2")) fail("test_4, case 075 FAILED: "+cuCla.getEnclosingMethod().getName());
            if(cuCla.getEnclosingConstructor()!=null) fail("test_4, case 076 FAILED: "+cuCla.getEnclosingConstructor());
            if(cuCla.isMemberClass()) fail("test_4, case 077 FAILED: "+cuCla.isMemberClass());
            if(!cuCla.isLocalClass()) fail("test_4, case 078 FAILED: "+cuCla.isLocalClass());
            if(cuCla.isAnonymousClass()) fail("test_4, case 079 FAILED: "+cuCla.isAnonymousClass());
            if(cuCla.isSynthetic()) fail("test_4, case 080 FAILED: "+cuCla.isSynthetic());
            if(cuCla.getCanonicalName()!=null) fail("test_4, case 081 FAILED: "+cuCla.getCanonicalName());
            if(!cuCla.getSimpleName().equals("Y")) fail("test_4, case 082 FAILED: "+cuCla.getSimpleName());
        } catch(IllegalAccessException e) {
            fail(e.getMessage());
        } catch(IllegalArgumentException e) {
            fail(e.getMessage());
        } catch(java.lang.reflect.InvocationTargetException e) {
            fail(e.getMessage());
        } catch(NoSuchMethodException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Constructor's local class
     */
    public void test_4_1() {
        @i class LC001 {
            public Class c;
            public LC001() {
                @i class LC001_01 {
                }
                c = LC001_01.class;
            }
        };
        LC001 lc = new LC001();
        if(!lc.c.getEnclosingClass().getName().equals("java.lang.ClassHierarchyTest$2LC001")) fail("test_3, case 004 FAILED: "+lc.c.getEnclosingClass().getName());
        if(lc.c.getEnumConstants()!=null) fail("test_3, case 009 FAILED: "+lc.c.getEnumConstants());
        if(lc.c.isEnum()) fail("test_3, case 000 FAILED: "+lc.c.isEnum());
        try{lc.c.asSubclass(ClassHierarchyTest.class); fail("test_3, case 011 FAILED: "+lc.c.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
        if(lc.c.getEnclosingMethod()!=null) fail("test_3, case 013 FAILED: "+lc.c.getEnclosingMethod().getName());
        if(!lc.c.getEnclosingConstructor().getName().equals("java.lang.ClassHierarchyTest$2LC001")) fail("test_3, case 014 FAILED: "+lc.c.getEnclosingConstructor());
        if(lc.c.isMemberClass()) fail("test_3, case 017 FAILED: "+lc.c.isMemberClass());
        if(!lc.c.isLocalClass()) fail("test_3, case 018 FAILED: "+lc.c.isLocalClass());
        if(lc.c.isAnonymousClass()) fail("test_3, case 019 FAILED: "+lc.c.isAnonymousClass());
        if(lc.c.isSynthetic()) fail("test_3, case 020 FAILED: "+lc.c.isSynthetic());
        if(lc.c.getCanonicalName()!=null) fail("test_3, case 021 FAILED: "+lc.c.getCanonicalName());
        if(!lc.c.getSimpleName().equals("LC001_01")) fail("test_3, case 022 FAILED: "+lc.c.getSimpleName());
    }

    /**
     * proxy class
     */
    public void test_5() {
        class LIH implements java.lang.reflect.InvocationHandler {
            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable
            {
                return null;
            }
        }
        Class cuCla = java.lang.reflect.Proxy.newProxyInstance(java.io.Serializable.class.getClassLoader(),
                         new Class[] { java.io.Serializable.class },
                         new LIH()).getClass();

        if(cuCla.getEnclosingClass()!=null) fail("test_5, case 004 FAILED: "+cuCla.getEnclosingClass());
        if(cuCla.getEnumConstants()!=null) fail("test_5, case 009 FAILED: "+cuCla.getEnumConstants());
        if(cuCla.isEnum()) fail("test_5, case 000 FAILED: "+cuCla.isEnum());
        try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_5, case 011 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
        if(cuCla.getEnclosingMethod()!=null) fail("test_5, case 013 FAILED: "+cuCla.getEnclosingMethod());
        if(cuCla.getEnclosingConstructor()!=null) fail("test_5, case 014 FAILED: "+cuCla.getEnclosingConstructor());
        if(cuCla.isMemberClass()) fail("test_5, case 017 FAILED: "+cuCla.isMemberClass());
        if(cuCla.isLocalClass()) fail("test_5, case 018 FAILED: "+cuCla.isLocalClass());
        if(cuCla.isAnonymousClass()) fail("test_5, case 019 FAILED: "+cuCla.isAnonymousClass());
        if(cuCla.isSynthetic()) fail("test_5, case 020 FAILED: "+cuCla.isSynthetic());
        if(!cuCla.getCanonicalName().replaceFirst("\\$Proxy", "").matches("\\d+")) fail("test_5, case 021 FAILED: "+cuCla.getCanonicalName());
        if(!cuCla.getSimpleName().replaceFirst("\\$Proxy", "").matches("\\d+")) fail("test_5, case 022 FAILED: "+cuCla.getSimpleName());
    }

///////////////////////// II:   
    /**
     * member interfaces
     */
    public void test_6() {
        if(!MI001.class.getEnclosingClass().equals(ClassHierarchyTest.class)) fail("test_1, case 004 FAILED: "+MI001.class.getEnclosingClass());
        if(MI001.class.getEnumConstants()!=null) fail("test_1, case 009 FAILED: "+MI001.class.getEnumConstants());
        if(MI001.class.isEnum()) fail("test_1, case 000 FAILED: "+MI001.class.isEnum());
        try{MI001.class.asSubclass(ClassHierarchyTest.class); fail("test_1, case 011 FAILED: "+MI001.class.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
        if(MI001.class.getEnclosingMethod()!=null) fail("test_1, case 013 FAILED: "+MI001.class.getEnclosingMethod());
        if(MI001.class.getEnclosingConstructor()!=null) fail("test_1, case 014 FAILED: "+MI001.class.getEnclosingConstructor());
        if(!MI001.class.isMemberClass()) fail("test_1, case 017 FAILED: "+MI001.class.isMemberClass());
        if(MI001.class.isLocalClass()) fail("test_1, case 018 FAILED: "+MI001.class.isLocalClass());
        if(MI001.class.isAnonymousClass()) fail("test_1, case 019 FAILED: "+MI001.class.isAnonymousClass());
        if(MI001.class.isSynthetic()) fail("test_1, case 020 FAILED: "+MI001.class.isSynthetic());
        if(!MI001.class.getCanonicalName().equals("java.lang.ClassHierarchyTest.MI001")) fail("test_1, case 021 FAILED: "+MI001.class.getCanonicalName());
        if(!MI001.class.getSimpleName().equals("MI001")) fail("test_1, case 022 FAILED: "+MI001.class.getSimpleName());
    }

    /**
     * deeply nested member interfaces
     */
    public void test_7() {
        Class cuCla = MI001.MI001_01.MI001_01_01.MI001_01_01_01.MI001_01_01_01_01.MI001_01_01_01_01_01.MI001_01_01_01_01_01_01.class;
        String caNa = cuCla.getCanonicalName();
        //String name[] = caNa.split("\\$");
        String name[] = caNa.split("\\.");
        int i = name.length - 1;
        while (cuCla != null) {
            if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[i])) fail("test_2, case 002 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
            if(cuCla.getEnumConstants()!=null) fail("test_2, case 009 FAILED: "+cuCla.getEnumConstants());
            if(cuCla.isEnum()) fail("test_2, case 000 FAILED: "+cuCla.isEnum());
            try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassHierarchyTest")) fail("test_2, case 011 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
            if(cuCla.getEnclosingMethod()!=null) fail("test_2, case 013 FAILED: "+cuCla.getEnclosingMethod());
            if(cuCla.getEnclosingConstructor()!=null) fail("test_2, case 014 FAILED: "+cuCla.getEnclosingConstructor());
            if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_1, case 017 FAILED: "+cuCla.isMemberClass());
            if(cuCla.isLocalClass()) fail("test_2, case 018 FAILED: "+cuCla.isLocalClass());
            if(cuCla.isAnonymousClass()) fail("test_2, case 019 FAILED: "+cuCla.isAnonymousClass());
            if(cuCla.isSynthetic()) fail("test_2, case 020 FAILED: "+cuCla.isSynthetic());
            if(!cuCla.getCanonicalName().equals(caNa)) fail("test_2, case 021 FAILED: "+cuCla.getCanonicalName());
            caNa = caNa.substring(0, caNa.lastIndexOf('.'));
            if(!cuCla.getSimpleName().equals(name[i])) fail("test_2, case 022 FAILED: "+cuCla.getSimpleName());
            i--;
            cuCla = cuCla.getEnclosingClass();
        }
    }
    
///////////////////////// III:  
    /**
      * member annotations as interfaces
      */
     @MA001 @MA001.MA001_01 @MA001.MA001_01.MA001_01_01 @MA001.MA001_01.MA001_01_01.MA001_01_01_01 @MA001.MA001_01.MA001_01_01.MA001_01_01_01.MA001_01_01_01_01 @MA001.MA001_01.MA001_01_01.MA001_01_01_01.MA001_01_01_01_01.MA001_01_01_01_01_01 @MA001.MA001_01.MA001_01_01.MA001_01_01_01.MA001_01_01_01_01.MA001_01_01_01_01_01.MA001_01_01_01_01_01_01 @MA001.MA001_01.MA001_01_01.MA001_01_01_01.MA001_01_01_01_01.MA001_01_01_01_01_01.MA001_01_01_01_01_01_01.iiii public void test_8() {
             Class cuCla = MA001.MA001_01.MA001_01_01.MA001_01_01_01.MA001_01_01_01_01.MA001_01_01_01_01_01.MA001_01_01_01_01_01_01.class;
             String caNa = cuCla.getCanonicalName();
             //String name[] = caNa.split("\\$");
             String name[] = caNa.split("\\.");
             int i = name.length - 1;
             while (cuCla != null) {
                 if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[i])) fail("test_2, case 002 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
                 if(cuCla.getEnumConstants()!=null) fail("test_2, case 009 FAILED: "+cuCla.getEnumConstants());
                 if(cuCla.isEnum()) fail("test_2, case 000 FAILED: "+cuCla.isEnum());
                 try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassHierarchyTest")) fail("test_2, case 011 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
                 if(cuCla.getEnclosingMethod()!=null) fail("test_2, case 013 FAILED: "+cuCla.getEnclosingMethod());
                 if(cuCla.getEnclosingConstructor()!=null) fail("test_2, case 014 FAILED: "+cuCla.getEnclosingConstructor());
                 if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_1, case 017 FAILED: "+cuCla.isMemberClass());
                 if(cuCla.isLocalClass()) fail("test_2, case 018 FAILED: "+cuCla.isLocalClass());
                 if(cuCla.isAnonymousClass()) fail("test_2, case 019 FAILED: "+cuCla.isAnonymousClass());
                 if(cuCla.isSynthetic()) fail("test_2, case 020 FAILED: "+cuCla.isSynthetic());
                 if(!cuCla.getCanonicalName().equals(caNa)) fail("test_2, case 021 FAILED: "+cuCla.getCanonicalName());
                 caNa = caNa.substring(0, caNa.lastIndexOf('.'));
                 if(!cuCla.getSimpleName().equals(name[i])) fail("test_2, case 022 FAILED: "+cuCla.getSimpleName());
                 i--;
                 cuCla = cuCla.getEnclosingClass();
             }
     }
     
     /**
      * member annotations as annotations
      */
      @MA001 @MA001.MA001_01 @MA001.MA001_01.MA001_01_01 @MA001.MA001_01.MA001_01_01.MA001_01_01_01 @MA001.MA001_01.MA001_01_01.MA001_01_01_01.MA001_01_01_01_01 @MA001.MA001_01.MA001_01_01.MA001_01_01_01.MA001_01_01_01_01.MA001_01_01_01_01_01 @MA001.MA001_01.MA001_01_01.MA001_01_01_01.MA001_01_01_01_01.MA001_01_01_01_01_01.MA001_01_01_01_01_01_01 @MA001.MA001_01.MA001_01_01.MA001_01_01_01.MA001_01_01_01_01.MA001_01_01_01_01_01.MA001_01_01_01_01_01_01.iiii public void test_9() {
          Annotation aa[] = null;
          try{
              aa = ClassHierarchyTest.class.getMethod("test_9").getAnnotations();
          } catch (NoSuchMethodException e) {
              fail("test_9, case 009 FAILED: "+e.toString());
          }
          for (int i = 0; i < aa.length; i++) {
              Class cuCla = aa[i].annotationType();
              String caNa = cuCla.getCanonicalName();
              String name[] = caNa.split("\\.");
              int j = name.length - 1;
              while (cuCla != null) {
                  if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[j])) fail("test_9, case 002 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
                  if(cuCla.getEnumConstants()!=null) fail("test_9, case 009 FAILED: "+cuCla.getEnumConstants());
                  if(cuCla.isEnum()) fail("test_9, case 000 FAILED: "+cuCla.isEnum());
                  try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassHierarchyTest")) fail("test_9, case 011 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
                  if(cuCla.getEnclosingMethod()!=null) fail("test_9, case 013 FAILED: "+cuCla.getEnclosingMethod());
                  if(cuCla.getEnclosingConstructor()!=null) fail("test_9, case 014 FAILED: "+cuCla.getEnclosingConstructor());
                  if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_9, case 017 FAILED: "+cuCla.isMemberClass());
                  if(cuCla.isLocalClass()) fail("test_9, case 018 FAILED: "+cuCla.isLocalClass());
                  if(cuCla.isAnonymousClass()) fail("test_9, case 019 FAILED: "+cuCla.isAnonymousClass());
                  if(cuCla.isSynthetic()) fail("test_9, case 020 FAILED: "+cuCla.isSynthetic());
                  if(!cuCla.getCanonicalName().equals(caNa)) fail("test_9, case 021 FAILED: "+cuCla.getCanonicalName());
                  caNa = caNa.substring(0, caNa.lastIndexOf('.'));
                  if(!cuCla.getSimpleName().equals(name[j])) fail("test_9, case 022 FAILED: "+cuCla.getSimpleName());
                  j--;
                  cuCla = cuCla.getEnclosingClass();
              }
          }
      }
      
///////////////////////// IV:    
      /**
       * member enums
       */
      public static enum ME001 {
          F_S(2),M_S(3),C_S(4),CL_S(1);
          ME001(int value) { 
              cccccc1 = ME001_01_.class;
              this.value = value; 
          }
          private final int value;
          public static Class cccc2;
          public Class ccccc3;
          public Class cccccc1;
          {
              class XXXX{};
              ccccc3 = ME001_01_.class;
          }
          public int value() { 
              class XXX{};
              cccc2 = ME001_01_.class;
              return value; 
          }
          public static enum ME001_01_ {G_A_T, P_T, V_T, W_T;
              public static enum ME001_01_1 {G_A_T, P_T, V_T, W_T;
                  public static enum ME001_01_2 {G_A_T, P_T, V_T, W_T;
                      public static enum ME001_01_3 {G_A_T, P_T, V_T, W_T;
                          public static enum ME001_01_4 {G_A_T, P_T, V_T, W_T;
                              public static enum ME001_01_5 {G_A_T, P_T, V_T, W_T;
                                  private final int value = 0;
                                  public int value() { 
                                      return value; 
                                  }
                                  public static Class pop() {
                                      class XXX{};
                                      return XXX.class; 
                                  }
                              };
                          };
                      };
                  };
              };
          };
      };
      public static enum ME001_01_ {G_A_T, P_T, V_T, W_T};
      public void test_10() {
          //Class cuCla = ME001.cccc2; - null
          //Class cuCla = new ME001(77).cccccc1;
          Class cuCla = ME001.ME001_01_.ME001_01_1.ME001_01_2.ME001_01_3.ME001_01_4.ME001_01_5.class;
          if(!cuCla.getEnclosingClass().equals(java.lang.ClassHierarchyTest.ME001.ME001_01_.ME001_01_1.ME001_01_2.ME001_01_3.ME001_01_4.class)) fail("test_1, case 004 FAILED: "+cuCla.getEnclosingClass());
          if(cuCla.getEnumConstants()==null) fail("test_1, case 009 FAILED: "+cuCla.getEnumConstants());
          ME001.ME001_01_.ME001_01_1.ME001_01_2.ME001_01_3.ME001_01_4.ME001_01_5 ae[] = (ME001.ME001_01_.ME001_01_1.ME001_01_2.ME001_01_3.ME001_01_4.ME001_01_5[])cuCla.getEnumConstants();
          for (int i = 0; i < ae.length; i++) {
              if (!ae[i].equals(ME001.ME001_01_.ME001_01_1.ME001_01_2.ME001_01_3.ME001_01_4.ME001_01_5.G_A_T) &&
                  !ae[i].equals(ME001.ME001_01_.ME001_01_1.ME001_01_2.ME001_01_3.ME001_01_4.ME001_01_5.P_T) &&
                  !ae[i].equals(ME001.ME001_01_.ME001_01_1.ME001_01_2.ME001_01_3.ME001_01_4.ME001_01_5.V_T) &&
                  !ae[i].equals(ME001.ME001_01_.ME001_01_1.ME001_01_2.ME001_01_3.ME001_01_4.ME001_01_5.W_T)) {
                  fail("test_1, case 001 FAILED: "+ae[i]+" is not this enum's constant");
              }
          }
          if(!cuCla.isEnum()) fail("test_1, case 000 FAILED: "+cuCla.isEnum());
          try{cuCla.asSubclass(ME001.ME001_01_.ME001_01_1.ME001_01_2.ME001_01_3.ME001_01_4.class); fail("test_1, case 011 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
          if(cuCla.getEnclosingMethod()!=null) fail("test_1, case 013 FAILED: "+cuCla.getEnclosingMethod());
          if(cuCla.getEnclosingConstructor()!=null) fail("test_1, case 014 FAILED: "+cuCla.getEnclosingConstructor());
          if(!cuCla.isMemberClass()) fail("test_1, case 017 FAILED: "+cuCla.isMemberClass());
          if(cuCla.isLocalClass()) fail("test_1, case 018 FAILED: "+cuCla.isLocalClass());
          if(cuCla.isAnonymousClass()) fail("test_1, case 019 FAILED: "+cuCla.isAnonymousClass());
          if(cuCla.isSynthetic()) fail("test_1, case 020 FAILED: "+cuCla.isSynthetic());
          if(!cuCla.getCanonicalName().equals("java.lang.ClassHierarchyTest.ME001.ME001_01_.ME001_01_1.ME001_01_2.ME001_01_3.ME001_01_4.ME001_01_5")) fail("test_1, case 021 FAILED: "+cuCla.getCanonicalName());
          if(!cuCla.getSimpleName().equals("ME001_01_5")) fail("test_1, case 022 FAILED: "+cuCla.getSimpleName());

          if (RuntimeAdditionalSupport1.openingFlag) {
              cuCla = ME001.ME001_01_.ME001_01_1.ME001_01_2.ME001_01_3.ME001_01_4.ME001_01_5.pop();
              if(!cuCla.getEnclosingClass().equals(java.lang.ClassHierarchyTest.ME001.ME001_01_.ME001_01_1.ME001_01_2.ME001_01_3.ME001_01_4.ME001_01_5.class)) fail("test_1, case 004 FAILED: "+cuCla.getEnclosingClass());
              if(cuCla.getEnumConstants()!=null) fail("test_1, case 009 FAILED: "+cuCla.getEnumConstants());
              if(cuCla.isEnum()) fail("test_1, case 000 FAILED: "+cuCla.isEnum());
              try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_1, case 011 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
              if(!cuCla.getEnclosingMethod().getName().equals("pop")) fail("test_1, case 013 FAILED: "+cuCla.getEnclosingMethod());
              if(cuCla.getEnclosingConstructor()!=null) fail("test_1, case 014 FAILED: "+cuCla.getEnclosingConstructor());
              if(cuCla.isMemberClass()) fail("test_1, case 017 FAILED: "+cuCla.isMemberClass());
              if(!cuCla.isLocalClass()) fail("test_1, case 018 FAILED: "+cuCla.isLocalClass());
              if(cuCla.isAnonymousClass()) fail("test_1, case 019 FAILED: "+cuCla.isAnonymousClass());
              if(cuCla.isSynthetic()) fail("test_1, case 020 FAILED: "+cuCla.isSynthetic());
              if(cuCla.getCanonicalName()!=null) fail("test_1, case 021 FAILED: "+cuCla.getCanonicalName());
              if(!cuCla.getSimpleName().equals("XXX")) fail("test_1, case 022 FAILED: "+cuCla.getSimpleName());
          }
          
      }

///////////////////////// V:    
      /**
       * casting
       */
      interface XXX{};
      enum tmp15$ implements XXX {
          PENNY(1),NICKEL(5),DIME(10),QUARTER(25);
          tmp15$(int value) {this.value = value;}
          private final int value;
          public int value() {
              return value;
          }
          enum yyy$1$2$3 {WHITE, GREY, BLACK};
      }
	  static Object obj;
      public void test_11() {
          class A {
          }
          class B extends A {
          }
          class $tmp15 {
          }
          class $tmp16 {
          }
          try {
              Object o = new $tmp16();
              tmp15$.class.cast(o);
              tmp15$ x = (tmp15$) o; obj = x;
              fail("test_11, case 001 FAILED: ClassCastException should be risen");
          } catch (ClassCastException e) {
          }
          try {
              Object o = new $tmp16();
              tmp15$ i = (tmp15$) o; obj = i;
              fail("test_11, case 002 FAILED: ClassCastException should be risen");
          } catch (ClassCastException e) {
          }
          class CC<T extends Number> {
          }
          CC<Integer> io = new CC<Integer>();
          CC<Float> fo = new CC<Float>();
          fo.getClass().cast(io);
          class Str {
          }
          class CCC<T extends Str & java.io.Serializable> {
          }
          class CCCc<T1 extends Str & java.io.Serializable, T2> {
          }
          class Xx extends Str implements java.io.Serializable {static final long serialVersionUID = 0;
          }
          class Yy extends Str implements java.io.Serializable {static final long serialVersionUID = 0;
          }
          CCC<?> io2 = new CCC<Xx>();
          CCC<? extends Str> fo2 = new CCC<Yy>();
          if (!io2.getClass().getName().equals("java.lang.ClassHierarchyTest$1CCC")) {
              fail("test_11, case 003 FAILED: "+io2.getClass().getName());
          }
          if (!fo2.getClass().cast(io2).getClass().getName().equals("java.lang.ClassHierarchyTest$1CCC")) {
              fail("test_11, case 004 FAILED: "+io2.getClass().getName());
          }
          CCCc<Xx, Yy> fo3 = new CCCc<Xx, Yy>();
          Object o = new CCCc<Yy, Yy>();
          try {
              // fo3 = (CCCc<Xx,Yy>) new CCCc<Yy,Yy>(); // inconvertible types: found : CCCc<Yy,Yy> - required: CCCc<Xx,Yy>
              fo3 = (CCCc<Xx, Yy>) o; // but it is run (look just above)!
              // so, it's expected the exceptions will risen while running deeply
              fo3.getClass().cast(new CCCc<Yy, Yy>()); // so, looks like our cast() implementation is not worse
          } catch (Exception e) {
              fail("test_11, case 005 FAILED: "+e.toString());
          }
          try {
              Object ooo = (Object) fo2;
              if (fo3.getClass().isAssignableFrom(fo2.getClass())) {
                  fail("test_11, case 006 FAILED");
              }
              if (ooo instanceof CCCc) {
                  fail("test_11, case 007 FAILEDn");
              }
              fo3.getClass().cast(fo2);
              fail("test_11, case 008 FAILED: ClassCastException should be risen");
          } catch (ClassCastException e) {
              //e.printStackTrace();
          }
          try {
              Object o1 = new CCCc<Xx, Yy>();
              Object o2 = new CCCc<Xx, Yy>();
              Object o3 = o1.getClass().cast(o2);obj = o3;
              Object o4 = o2.getClass().cast(o1);obj = o4;
              Object o5 = o2.getClass().cast(null);obj = o5;
              Class cuCla1 = o1.getClass();
              Class cuCla2 = o2.getClass();
              Class cuCla3 = cuCla1.asSubclass(cuCla2);obj = cuCla3;
          } catch (ClassCastException e) {
              fail("test_11, case 009 FAILED: ClassCastException should not be risen");
          }
          try {
              Object o1 = new CCCc<Xx, Xx>();
              Object o2 = new CCCc<Xx, Yy>();
              Object o3 = o1.getClass().cast(o2);obj = o3;
              Object o4 = o2.getClass().cast(o1);obj = o4;
              Object o5 = o2.getClass().cast(null);obj = o5;
              Class cuCla1 = o1.getClass();
              Class cuCla2 = o2.getClass();
              Class cuCla3 = cuCla1.asSubclass(cuCla2);obj = cuCla3;
          } catch (ClassCastException e) {
              fail("test_11, case 010 FAILED: ClassCastException should  notbe risen");
          }
          try {
              Object o1 = new CCCc<Xx, Xx>();
              Object o2 = new CCCc<Yy, Yy>();
              Object o3 = o1.getClass().cast(o2);obj = o3;
              Object o4 = o2.getClass().cast(o1);obj = o4;
              Object o5 = o2.getClass().cast(null);obj = o5;
              Class cuCla1 = o1.getClass();
              Class cuCla2 = o2.getClass();
              Class cuCla3 = cuCla1.asSubclass(cuCla2);obj = cuCla3;
          } catch (ClassCastException e) {
              fail("test_11, case 011 FAILED: ClassCastException should not be risen");
          }
          try {
              Object o1 = new CCCc<Xx, Xx>();
              Object o2 = new CCCc<Yy, Xx>();
              Object o3 = o1.getClass().cast(o2);obj = o3;
              Object o4 = o2.getClass().cast(o1);obj = o4;
              Object o5 = o2.getClass().cast(null);obj = o5;
              Class cuCla1 = o1.getClass();
              Class cuCla2 = o2.getClass();
              Class cuCla3 = cuCla1.asSubclass(cuCla2);obj = cuCla3;
          } catch (ClassCastException e) {
              fail("test_11, case 012 FAILED: ClassCastException should not be risen");
          }
          try {
              Object o1 = new CCCc();
              Object o2 = new CCCc<Xx, Yy>();
              Object o3 = o1.getClass().cast(o2);obj = o3;
              Object o4 = o2.getClass().cast(o1);obj = o4;
              Object o5 = o2.getClass().cast(null);obj = o5;
              Class cuCla1 = o1.getClass();
              Class cuCla2 = o2.getClass();
              Class cuCla3 = cuCla1.asSubclass(cuCla2);obj = cuCla3;
          } catch (ClassCastException e) {
              fail("test_11, case 013 FAILED: ClassCastException should not be risen");
          }
          try {
              Object o1 = new CCCc<Xx, Yy>();
              Object o2 = new CCCc();
              Object o3 = o1.getClass().cast(o2);obj = o3;
              Object o4 = o2.getClass().cast(o1);obj = o4;
              Object o5 = o2.getClass().cast(null);obj = o5;
              Class cuCla1 = o1.getClass();
              Class cuCla2 = o2.getClass();
              Class cuCla3 = cuCla1.asSubclass(cuCla2);obj = cuCla3;
          } catch (ClassCastException e) {
              fail("test_11, case 014 FAILED: ClassCastException should not be risen");
          }
          
          class Str2 {
          }
          class CCC2<T extends Str & java.io.Serializable> {
          }
          class CCCc2<T1 extends Str & java.io.Serializable, T2> {
          }
          class Xx2 extends Str implements java.io.Serializable {static final long serialVersionUID = 0;
          }
          class Yy2 extends Str implements java.io.Serializable {static final long serialVersionUID = 0;
          }
          CCC2<? extends Str> fo22 = new CCC2<Yy2>();
          CCCc2<Xx2, Yy2> fo33 = new CCCc2<Xx2, Yy2>();
          Object o2 = new CCCc2<Yy, Yy>();
          try {
              fo33 = (CCCc2<Xx2, Yy2>) o2;
              fo33.getClass().cast(new CCCc2<Yy2, Yy2>());
              try {
                  fo33.getClass().cast(new CCCc<Yy2, Yy2>());
                  fail("test_11, case 015 FAILED: ClassCastException should be risen");
              } catch (ClassCastException e) {
              }
              fo33.getClass().cast(new CCCc2<Yy, Yy>());
          } catch (Exception e) {
              e.printStackTrace();
              fail("test_11, case 016 FAILED: "+e.toString());
          }
          try {
              fo33.getClass().cast(fo22);
              fail("test_11, case 017 FAILED: ClassCastException should be risen");
          } catch (ClassCastException e) {
              //e.printStackTrace();
          }

      }

      /**
       * generalized member class
       */
      @anna public void test_12() {
          Class cuCla = new MC002<Object, java.lang.reflect.Proxy, MC002<Integer, Void, Character>.MC002_01<Integer, Void, Character>.MC002_01_01<Integer, Void, Character>.MC002_01_01_01<Integer, Void, Character>.MC002_01_01_01_01<Integer, Void, Character>.MC002_01_01_01_01_01<Integer, Void, Character>.MC002_01_01_01_01_01_01<Integer, Void, Character>>().getClass();
          if(!cuCla.getEnclosingClass().equals(ClassHierarchyTest.class)) fail("test_1, case 004 FAILED: "+cuCla.getEnclosingClass());
          if(cuCla.getEnumConstants()!=null) fail("test_1, case 009 FAILED: "+cuCla.getEnumConstants());
          if(cuCla.isEnum()) fail("test_1, case 000 FAILED: "+cuCla.isEnum());
          try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_1, case 011 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
          if(cuCla.getEnclosingMethod()!=null) fail("test_1, case 013 FAILED: "+cuCla.getEnclosingMethod());
          if(cuCla.getEnclosingConstructor()!=null) fail("test_1, case 014 FAILED: "+cuCla.getEnclosingConstructor());
          if(!cuCla.isMemberClass()) fail("test_1, case 017 FAILED: "+cuCla.isMemberClass());
          if(cuCla.isLocalClass()) fail("test_1, case 018 FAILED: "+cuCla.isLocalClass());
          if(cuCla.isAnonymousClass()) fail("test_1, case 019 FAILED: "+cuCla.isAnonymousClass());
          if(cuCla.isSynthetic()) fail("test_1, case 020 FAILED: "+cuCla.isSynthetic());
          if(!cuCla.getCanonicalName().equals("java.lang.ClassHierarchyTest.MC002")) fail("test_1, case 021 FAILED: "+cuCla.getCanonicalName());
          if(!cuCla.getSimpleName().equals("MC002")) fail("test_1, case 022 FAILED: "+cuCla.getSimpleName());

          if(cuCla.getGenericInterfaces().length != 0) fail("test_1, case 022 FAILED: "+cuCla.getGenericInterfaces().length);
          if(!((Class)cuCla.getGenericSuperclass()).getName().equals("java.lang.Object")) fail("test_1, case 022 FAILED: "+((Class)cuCla.getGenericSuperclass()).getName());
          if(cuCla.getTypeParameters().length == 0) fail("test_1, case 022 FAILED: "+cuCla.getTypeParameters().length);
  /* !!! */        System.out.println(111);
          
          Annotation aaa = null;
          try{aaa = ClassHierarchyTest.class.getMethod("test_12").getAnnotations()[0];}catch(NoSuchMethodException e){fail("test_1, case 022 FAILED: Internal Error");}
          try{if(cuCla.cast(aaa) == null) fail("test_1, case 022 FAILED: "+cuCla.getSimpleName());}catch(Exception e){/*e.printStackTrace();*/}

          if(cuCla.getAnnotation(aaa.annotationType()) != null) fail("test_1, case 022 FAILED: "+cuCla.getSimpleName());
          if(cuCla.getAnnotations().length == 0) fail("test_1, case 022 FAILED: "+cuCla.getSimpleName());
          if(cuCla.getDeclaredAnnotations().length == 0) fail("test_1, case 022 FAILED: "+cuCla.getSimpleName());
          if(cuCla.isAnnotation()) fail("test_1, case 022 FAILED: "+cuCla.getSimpleName());
          if(cuCla.isAnnotationPresent(aaa.annotationType())) fail("test_1, case 022 FAILED: "+cuCla.getSimpleName());
      }
      
      /**
       * generalized member interface
       */
//Commented because of the drlvm issue
      public void te_st_12_1() { // it is eclipse's bug
                                // It's strange that if we use javac instead of ecj
                                // then we have no such problem
                                //   <<< java.lang.NoClassDefFoundError: Ajava/lang/ClassHierarchyTest$1xxx: class name in class data does not match class name passed>>>
                                // so it should be investigated.
          
          
          // The wollowing crashes bvm:
          //class x implements MI002.MI002_01.MI002_01_01.MI002_01_01_01.MI002_01_01_01_01.MI002_01_01_01_01_01.MI002_01_01_01_01_01_01<java.io.Serializable, java.lang.reflect.Type, java.lang.reflect.InvocationHandler> {};
          //Class cuCla = (Class)x.class.getGenericInterfaces()[0];
          class xxx implements MI002.MI002_01.MI002_01_01.MI002_01_01_01.MI002_01_01_01_01.MI002_01_01_01_01_01.MI002_01_01_01_01_01_01<java.io.Serializable, java.lang.reflect.Type, java.lang.reflect.InvocationHandler> {};
          Class cuCla = (Class)((java.lang.reflect.ParameterizedType)xxx.class.getGenericInterfaces()[0]).getRawType();
          if(!cuCla.getEnclosingClass().equals(MI002.MI002_01.MI002_01_01.MI002_01_01_01.MI002_01_01_01_01.MI002_01_01_01_01_01.class)) fail("test_1, case 004 FAILED: "+cuCla.getEnclosingClass());
          if(cuCla.getEnumConstants()!=null) fail("test_1, case 009 FAILED: "+cuCla.getEnumConstants());
          if(cuCla.isEnum()) fail("test_1, case 000 FAILED: "+cuCla.isEnum());
          try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_1, case 011 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
          if(cuCla.getEnclosingMethod()!=null) fail("test_1, case 013 FAILED: "+cuCla.getEnclosingMethod());
          if(cuCla.getEnclosingConstructor()!=null) fail("test_1, case 014 FAILED: "+cuCla.getEnclosingConstructor());
          if(!cuCla.isMemberClass()) fail("test_1, case 017 FAILED: "+cuCla.isMemberClass());
          if(cuCla.isLocalClass()) fail("test_1, case 018 FAILED: "+cuCla.isLocalClass());
          if(cuCla.isAnonymousClass()) fail("test_1, case 019 FAILED: "+cuCla.isAnonymousClass());
          if(cuCla.isSynthetic()) fail("test_1, case 020 FAILED: "+cuCla.isSynthetic());
          if(!cuCla.getCanonicalName().equals("java.lang.ClassHierarchyTest.MI002.MI002_01.MI002_01_01.MI002_01_01_01.MI002_01_01_01_01.MI002_01_01_01_01_01.MI002_01_01_01_01_01_01")) fail("test_1, case 021 FAILED: "+cuCla.getCanonicalName());
          if(!cuCla.getSimpleName().equals("MI002_01_01_01_01_01_01")) fail("test_1, case 022 FAILED: "+cuCla.getSimpleName());
      }
      
      /**
       * deeply nested generalized member classes
       */
      public void test_13() {
          Class cuCla = new MC002<Object, java.lang.reflect.Proxy, MC002<Integer, Void, Character>.MC002_01<Integer, Void, Character>.MC002_01_01<Integer, Void, Character>.MC002_01_01_01<Integer, Void, Character>.MC002_01_01_01_01<Integer, Void, Character>.MC002_01_01_01_01_01<Integer, Void, Character>.MC002_01_01_01_01_01_01<Integer, Void, Character>>().getClass();
          String caNa = cuCla.getCanonicalName();
          //String name[] = caNa.split("\\$");
          String name[] = caNa.split("\\.");
          int i = name.length - 1;
          while (cuCla != null) {
              if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[i])) fail("test_2, case 002 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
              if(cuCla.getEnumConstants()!=null) fail("test_2, case 009 FAILED: "+cuCla.getEnumConstants());
              if(cuCla.isEnum()) fail("test_2, case 000 FAILED: "+cuCla.isEnum());
              try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassHierarchyTest")) fail("test_2, case 011 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
              if(cuCla.getEnclosingMethod()!=null) fail("test_2, case 013 FAILED: "+cuCla.getEnclosingMethod());
              if(cuCla.getEnclosingConstructor()!=null) fail("test_2, case 014 FAILED: "+cuCla.getEnclosingConstructor());
              if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_1, case 017 FAILED: "+cuCla.isMemberClass());
              if(cuCla.isLocalClass()) fail("test_2, case 018 FAILED: "+cuCla.isLocalClass());
              if(cuCla.isAnonymousClass()) fail("test_2, case 019 FAILED: "+cuCla.isAnonymousClass());
              if(cuCla.isSynthetic()) fail("test_2, case 020 FAILED: "+cuCla.isSynthetic());
              if(!cuCla.getCanonicalName().equals(caNa)) fail("test_2, case 021 FAILED: "+cuCla.getCanonicalName());
              caNa = caNa.substring(0, caNa.lastIndexOf('.'));
              if(!cuCla.getSimpleName().equals(name[i])) fail("test_2, case 022 FAILED: "+cuCla.getSimpleName());
              i--;
              cuCla = cuCla.getEnclosingClass();
          }
      }
      
      /**
       * deeply nested generalized member interfaces
       */
//Commented because of the drlvm issue
      public void te_st_13_1() { // it is eclipse's bug
                                // It's strange that if we use javac instead of ecj
                                // then we have no such problem
                                //   <<< java.lang.NoClassDefFoundError: Ajava/lang/ClassHierarchyTest$1xxx: class name in class data does not match class name passed>>>
                                // so it should be investigated.
          //Class cuCla = MI001.MI001_01.MI001_01_01.MI001_01_01_01.MI001_01_01_01_01.MI001_01_01_01_01_01.MI001_01_01_01_01_01_01.class;
          class xxx implements MI002.MI002_01.MI002_01_01.MI002_01_01_01.MI002_01_01_01_01.MI002_01_01_01_01_01.MI002_01_01_01_01_01_01<java.io.Serializable, java.lang.reflect.Type, java.lang.reflect.InvocationHandler> {};
          Class cuCla = (Class)((java.lang.reflect.ParameterizedType)xxx.class.getGenericInterfaces()[0]).getRawType();
          String caNa = cuCla.getCanonicalName();
          //String name[] = caNa.split("\\$");
          String name[] = caNa.split("\\.");
          int i = name.length - 1;
          while (cuCla != null) {
              if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[i])) fail("test_2, case 002 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
              if(cuCla.getEnumConstants()!=null) fail("test_2, case 009 FAILED: "+cuCla.getEnumConstants());
              if(cuCla.isEnum()) fail("test_2, case 000 FAILED: "+cuCla.isEnum());
              try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassHierarchyTest")) fail("test_2, case 011 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
              if(cuCla.getEnclosingMethod()!=null) fail("test_2, case 013 FAILED: "+cuCla.getEnclosingMethod());
              if(cuCla.getEnclosingConstructor()!=null) fail("test_2, case 014 FAILED: "+cuCla.getEnclosingConstructor());
              if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_1, case 017 FAILED: "+cuCla.isMemberClass());
              if(cuCla.isLocalClass()) fail("test_2, case 018 FAILED: "+cuCla.isLocalClass());
              if(cuCla.isAnonymousClass()) fail("test_2, case 019 FAILED: "+cuCla.isAnonymousClass());
              if(cuCla.isSynthetic()) fail("test_2, case 020 FAILED: "+cuCla.isSynthetic());
              if(!cuCla.getCanonicalName().equals(caNa)) fail("test_2, case 021 FAILED: "+cuCla.getCanonicalName());
              caNa = caNa.substring(0, caNa.lastIndexOf('.'));
              if(!cuCla.getSimpleName().equals(name[i])) fail("test_2, case 022 FAILED: "+cuCla.getSimpleName());
              i--;
              cuCla = cuCla.getEnclosingClass();
          }
      }

      /**
       * unnested generalized local class
       */
      public <T0> void test_14() {
          @i class LC000<T1 extends T0> {
              
          }
          //@i class LC001<T1 extends T0 &java.io.Serializable &java.lang.reflect.Type &java.lang.reflect.GenericDeclaration> {
          @i class LC001<T1 extends LC000 &java.io.Serializable &java.lang.reflect.Type &GenericDeclaration> {
              java.lang.reflect.TypeVariable<?>[] getTypeParameters() {
                  return (java.lang.reflect.TypeVariable<?>[])null;
              }
          };
		  new LC001().getTypeParameters();
          if(!LC001.class.getEnclosingClass().equals(ClassHierarchyTest.class)) fail("test_3, case 004 FAILED: "+LC001.class.getEnclosingClass());
          if(LC001.class.getEnumConstants()!=null) fail("test_3, case 009 FAILED: "+LC001.class.getEnumConstants());
          if(LC001.class.isEnum()) fail("test_3, case 000 FAILED: "+LC001.class.isEnum());
          try{LC001.class.asSubclass(ClassHierarchyTest.class); fail("test_3, case 011 FAILED: "+LC001.class.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
          if(!LC001.class.getEnclosingMethod().getName().equals("test_14")) fail("test_3, case 013 FAILED: "+LC001.class.getEnclosingMethod().getName());
          if(LC001.class.getEnclosingConstructor()!=null) fail("test_3, case 014 FAILED: "+LC001.class.getEnclosingConstructor());
          if(LC001.class.isMemberClass()) fail("test_3, case 017 FAILED: "+LC001.class.isMemberClass());
          if(!LC001.class.isLocalClass()) fail("test_3, case 018 FAILED: "+LC001.class.isLocalClass());
          if(LC001.class.isAnonymousClass()) fail("test_3, case 019 FAILED: "+LC001.class.isAnonymousClass());
          if(LC001.class.isSynthetic()) fail("test_3, case 020 FAILED: "+LC001.class.isSynthetic());
          if(LC001.class.getCanonicalName()!=null) fail("test_3, case 021 FAILED: "+LC001.class.getCanonicalName());
          if(!LC001.class.getSimpleName().equals("LC001")) fail("test_3, case 022 FAILED: "+LC001.class.getSimpleName());
      }

      /**
       * generalized local               ClassHierarchyTest$1$LC003$1A.class   (1)
       * generalized anonymous           ClassHierarchyTest$1$LC003$1.class    (2)
       * generalized member of anonymous ClassHierarchyTest$1$LC003$1$A.class  (3)
       * generalized local               ClassHierarchyTest$1$LC003$1AAA.class (4)
       */
      static int f2 = 0;
      static Object ooo2 = null;
      public <T0 extends MC002> void test_15() { // it is eclipse's bug
                                // It's strange that if we use javac instead of ecj
                                // then we have no such problem
                                //   <<< java.lang.NoClassDefFoundError: Ajava/lang/ClassHierarchyTest$1xxx: class name in class data does not match class name passed>>>
                                // so it should be investigated.
          @i
          class LC002<T1 extends T0> {
          }
          ;
          class $LC003<T2 extends ClassHierarchyTest &java.lang.reflect.ParameterizedType> {
              public Class value() {
                  class A<T3 extends T2> extends $LC003 { // (1)
                  }
                  ;
                  Object o = new LC002<T0>() { // (2)
                      class A<T5, T6, T7, T8, T9> {            // (3)
                      }
                      Class m1() {
                          return A.class;
                      }
                      Class m2() {
                          return m1() == null? m3():m1();
                      }
                      Class m3() {
                          return m2() == null? m1():m2();
                      }
                  };
                  ooo2 = o;
                  if (f2 < 1) {
                      f2 += 1;
                      return A.class;
                  }
                  f2 += 1;
                  return o.getClass();
              }

              public Class value2() {
                  class AAA<T10> {             // (4)
                  }
                  ;
                  return AAA.class;
              }
          }
          class X$1Y<T11> {
          }
          class X<T12, T13 extends ClassHierarchyTest> {
              Class m2() {
                  class Y<T121, T131> { // it has "X$2Y" name!!! So, compiler provides the
                            // difference with the previous "X$1Y"
                  }
                  return Y.class;
              }
          }
		  new X().m2();
          Class cuCla = new $LC003().value(); // ClassHierarchyTest$1$LC003$1A.class   (1)
          if(!cuCla.getEnclosingClass().equals($LC003.class)) fail("test_4, case 004 FAILED: "+cuCla.getEnclosingClass());
          if(cuCla.getEnumConstants()!=null) fail("test_4, case 009 FAILED: "+cuCla.getEnumConstants());
          if(cuCla.isEnum()) fail("test_4, case 000 FAILED: "+cuCla.isEnum());
          try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_4, case 011 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
          if(!cuCla.getEnclosingMethod().getName().equals("value")) fail("test_4, case 013 FAILED: "+cuCla.getEnclosingMethod().getName());
          if(cuCla.getEnclosingConstructor()!=null) fail("test_4, case 014 FAILED: "+cuCla.getEnclosingConstructor());
          if(cuCla.isMemberClass()) fail("test_4, case 017 FAILED: "+cuCla.isMemberClass());
          if(!cuCla.isLocalClass()) fail("test_4, case 018 FAILED: "+cuCla.isLocalClass());
          if(cuCla.isAnonymousClass()) fail("test_4, case 019 FAILED: "+cuCla.isAnonymousClass());
          if(cuCla.isSynthetic()) fail("test_4, case 020 FAILED: "+cuCla.isSynthetic());
          if(cuCla.getCanonicalName()!=null) fail("test_4, case 021 FAILED: "+cuCla.getCanonicalName());
          if(!cuCla.getSimpleName().equals("A")) fail("test_4, case 022 FAILED: "+cuCla.getSimpleName());
          
          cuCla = new $LC003().value(); // ClassHierarchyTest$1$LC003$1.class    (2)
          if(!cuCla.getEnclosingClass().equals($LC003.class)) fail("test_4, case 023 FAILED: "+cuCla.getEnclosingClass());
          if(cuCla.getEnumConstants()!=null) fail("test_4, case 024 FAILED: "+cuCla.getEnumConstants());
          if(cuCla.isEnum()) fail("test_4, case 025 FAILED: "+cuCla.isEnum());
          try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_4, case 026 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
          if(!cuCla.getEnclosingMethod().getName().equals("value")) fail("test_4, case 027 FAILED: "+cuCla.getEnclosingMethod().getName());
          if(cuCla.getEnclosingConstructor()!=null) fail("test_4, case 028 FAILED: "+cuCla.getEnclosingConstructor());
          if(cuCla.isMemberClass()) fail("test_4, case 029 FAILED: "+cuCla.isMemberClass());
          if(cuCla.isLocalClass()) fail("test_4, case 030 FAILED: "+cuCla.isLocalClass());
          if(!cuCla.isAnonymousClass()) fail("test_4, case 031 FAILED: "+cuCla.isAnonymousClass());
          if(cuCla.isSynthetic()) fail("test_4, case 032 FAILED: "+cuCla.isSynthetic());
          if(cuCla.getCanonicalName()!=null) fail("test_4, case 033 FAILED: "+cuCla.getCanonicalName());
          if(!cuCla.getSimpleName().equals("")) fail("test_4, case 034 FAILED: "+cuCla.getSimpleName());
          
          cuCla = new $LC003().value2(); // ClassHierarchyTest$1$LC003$1AAA.class (4)
          if(!cuCla.getEnclosingClass().equals($LC003.class)) fail("test_4, case 035 FAILED: "+cuCla.getEnclosingClass());
          if(cuCla.getEnumConstants()!=null) fail("test_4, case 036 FAILED: "+cuCla.getEnumConstants());
          if(cuCla.isEnum()) fail("test_4, case 037 FAILED: "+cuCla.isEnum());
          try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_4, case 038 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
          if(!cuCla.getEnclosingMethod().getName().equals("value2")) fail("test_4, case 039 FAILED: "+cuCla.getEnclosingMethod().getName());
          if(cuCla.getEnclosingConstructor()!=null) fail("test_4, case 040 FAILED: "+cuCla.getEnclosingConstructor());
          if(cuCla.isMemberClass()) fail("test_4, case 041 FAILED: "+cuCla.isMemberClass());
          if(!cuCla.isLocalClass()) fail("test_4, case 042 FAILED: "+cuCla.isLocalClass());
          if(cuCla.isAnonymousClass()) fail("test_4, case 043 FAILED: "+cuCla.isAnonymousClass());
          if(cuCla.isSynthetic()) fail("test_4, case 044 FAILED: "+cuCla.isSynthetic());
          if(cuCla.getCanonicalName()!=null) fail("test_4, case 045 FAILED: "+cuCla.getCanonicalName());
          if(!cuCla.getSimpleName().equals("AAA")) fail("test_4, case 046 FAILED: "+cuCla.getSimpleName());
          
          try {
              cuCla = (Class) new $LC003().value().getDeclaredMethod("m1").invoke(ooo2, (Object[])null); // ClassHierarchyTest$1$LC003$1$A.class  (3)
              Class tc = Class.forName("java.lang.ClassHierarchyTest$2$LC003$1");
              if(!cuCla.getEnclosingClass().equals(tc)) fail("test_4, case 047 FAILED: "+cuCla.getEnclosingClass());
              if(cuCla.getEnumConstants()!=null) fail("test_4, case 048 FAILED: "+cuCla.getEnumConstants());
              if(cuCla.isEnum()) fail("test_4, case 049 FAILED: "+cuCla.isEnum());
              try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_4, case 050 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
              if(cuCla.getEnclosingMethod()!=null) fail("test_4, case 051 FAILED: "+cuCla.getEnclosingMethod());
              if(cuCla.getEnclosingConstructor()!=null) fail("test_4, case 052 FAILED: "+cuCla.getEnclosingConstructor());
              if(!cuCla.isMemberClass()) fail("test_4, case 053 FAILED: "+cuCla.isMemberClass());
              if(cuCla.isLocalClass()) fail("test_4, case 054 FAILED: "+cuCla.isLocalClass());
              if(cuCla.isAnonymousClass()) fail("test_4, case 055 FAILED: "+cuCla.isAnonymousClass());
              if(cuCla.isSynthetic()) fail("test_4, case 056 FAILED: "+cuCla.isSynthetic());
              if(cuCla.getCanonicalName()!=null) fail("test_4, case 057 FAILED: "+cuCla.getCanonicalName());
              if(!cuCla.getSimpleName().equals("A")) fail("test_4, case 058 FAILED: "+cuCla.getSimpleName());
          } catch(ClassNotFoundException e) {
              fail(e.getMessage());
          } catch(IllegalAccessException e) {
              fail(e.getMessage());
          } catch(IllegalArgumentException e) {
              fail(e.getMessage());
          } catch(java.lang.reflect.InvocationTargetException e) {
              fail(e.getMessage());
          } catch(NoSuchMethodException e) {
              fail(e.getMessage());
          }
          
          cuCla = X$1Y.class; // ClassHierarchyTest$1X$1Y.class  (3)
          if(!cuCla.getEnclosingClass().equals(ClassHierarchyTest.class)) fail("test_4, case 059 FAILED: "+cuCla.getEnclosingClass());
          if(cuCla.getEnumConstants()!=null) fail("test_4, case 060 FAILED: "+cuCla.getEnumConstants());
          if(cuCla.isEnum()) fail("test_4, case 061 FAILED: "+cuCla.isEnum());
          try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_4, case 062 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
          if(!cuCla.getEnclosingMethod().getName().equals("test_15")) fail("test_4, case 063 FAILED: "+cuCla.getEnclosingMethod().getName());
          if(cuCla.getEnclosingConstructor()!=null) fail("test_4, case 064 FAILED: "+cuCla.getEnclosingConstructor());
          if(cuCla.isMemberClass()) fail("test_4, case 065 FAILED: "+cuCla.isMemberClass());
          if(!cuCla.isLocalClass()) fail("test_4, case 066 FAILED: "+cuCla.isLocalClass());
          if(cuCla.isAnonymousClass()) fail("test_4, case 067 FAILED: "+cuCla.isAnonymousClass());
          if(cuCla.isSynthetic()) fail("test_4, case 068 FAILED: "+cuCla.isSynthetic());
          if(cuCla.getCanonicalName()!=null) fail("test_4, case 069 FAILED: "+cuCla.getCanonicalName());
          if(!cuCla.getSimpleName().equals("X$1Y")) fail("test_4, case 070 FAILED: "+cuCla.getSimpleName());
          
          try {
              cuCla = (Class) X.class.getDeclaredMethod("m2").invoke(new X(), (Object[])null); // ClassHierarchyTest$1$LC003$1$A.class  (3)
              if(!cuCla.getEnclosingClass().equals(X.class)) fail("test_4, case 071 FAILED: "+cuCla.getEnclosingClass());
              if(cuCla.getEnumConstants()!=null) fail("test_4, case 072 FAILED: "+cuCla.getEnumConstants());
              if(cuCla.isEnum()) fail("test_4, case 073 FAILED: "+cuCla.isEnum());
              try{cuCla.asSubclass(ClassHierarchyTest.class); fail("test_4, case 074 FAILED: "+cuCla.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
              if(!cuCla.getEnclosingMethod().getName().equals("m2")) fail("test_4, case 075 FAILED: "+cuCla.getEnclosingMethod().getName());
              if(cuCla.getEnclosingConstructor()!=null) fail("test_4, case 076 FAILED: "+cuCla.getEnclosingConstructor());
              if(cuCla.isMemberClass()) fail("test_4, case 077 FAILED: "+cuCla.isMemberClass());
              if(!cuCla.isLocalClass()) fail("test_4, case 078 FAILED: "+cuCla.isLocalClass());
              if(cuCla.isAnonymousClass()) fail("test_4, case 079 FAILED: "+cuCla.isAnonymousClass());
              if(cuCla.isSynthetic()) fail("test_4, case 080 FAILED: "+cuCla.isSynthetic());
              if(cuCla.getCanonicalName()!=null) fail("test_4, case 081 FAILED: "+cuCla.getCanonicalName());
              if(!cuCla.getSimpleName().equals("Y")) fail("test_4, case 082 FAILED: "+cuCla.getSimpleName());
          } catch(IllegalAccessException e) {
              fail(e.getMessage());
          } catch(IllegalArgumentException e) {
              fail(e.getMessage());
          } catch(java.lang.reflect.InvocationTargetException e) {
              fail(e.getMessage());
          } catch(NoSuchMethodException e) {
              fail(e.getMessage());
          }
      }

      /**
       * Constructor's generalized local class
       */
      public <T0> void test_16() {
          @i class LC000<T1 extends T0> {
              
          }
          @i class LC001<T1 extends LC000 &java.io.Serializable &java.lang.reflect.Type &GenericDeclaration> {
              public Class c;
              java.lang.reflect.TypeVariable<?>[] getTypeParameters() {
                  return (java.lang.reflect.TypeVariable<?>[])null;
              }
              public <T2 extends T0, T3 extends T1, T4 extends LC000<T0>>LC001() {
                  @i class LC001_01 {
                  }
                  c = LC001_01.class;
              }
          };
          LC001 lc = new LC001(); 
		  lc.getTypeParameters();
          if(!lc.c.getEnclosingClass().getName().equals("java.lang.ClassHierarchyTest$4LC001")) fail("test_3, case 004 FAILED: "+lc.c.getEnclosingClass().getName());
          if(lc.c.getEnumConstants()!=null) fail("test_3, case 009 FAILED: "+lc.c.getEnumConstants());
          if(lc.c.isEnum()) fail("test_3, case 000 FAILED: "+lc.c.isEnum());
          try{lc.c.asSubclass(ClassHierarchyTest.class); fail("test_3, case 011 FAILED: "+lc.c.asSubclass(ClassHierarchyTest.class));}catch(Exception e){/*e.printStackTrace();*/}
          if(lc.c.getEnclosingMethod()!=null) fail("test_3, case 013 FAILED: "+lc.c.getEnclosingMethod().getName());
          if(!lc.c.getEnclosingConstructor().getName().equals("java.lang.ClassHierarchyTest$4LC001")) fail("test_3, case 014 FAILED: "+lc.c.getEnclosingConstructor());
          if(lc.c.isMemberClass()) fail("test_3, case 017 FAILED: "+lc.c.isMemberClass());
          if(!lc.c.isLocalClass()) fail("test_3, case 018 FAILED: "+lc.c.isLocalClass());
          if(lc.c.isAnonymousClass()) fail("test_3, case 019 FAILED: "+lc.c.isAnonymousClass());
          if(lc.c.isSynthetic()) fail("test_3, case 020 FAILED: "+lc.c.isSynthetic());
          if(lc.c.getCanonicalName()!=null) fail("test_3, case 021 FAILED: "+lc.c.getCanonicalName());
          if(!lc.c.getSimpleName().equals("LC001_01")) fail("test_3, case 022 FAILED: "+lc.c.getSimpleName());
      }
}