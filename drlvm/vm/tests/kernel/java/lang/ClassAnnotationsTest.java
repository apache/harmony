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
 *   Area for supposed testing: 
 *   - member, local, annonymous, enums, interfaces;
 *   - fields,
 *   - methods
 *   - constructors
 *   - packages
 *   - accessible objects 
 **/

import java.lang.annotation.Annotation;

import junit.framework.TestCase;

/*
 * Created on April 03, 2006
 *
 * This ClassHierarchyTest class is used to test the Core API Class class
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

@Retention(value=RetentionPolicy.RUNTIME)
@interface iat{
    abstract String author() default "Zapreyev";
};
@Retention(value=RetentionPolicy.RUNTIME)
@interface j{};
@Retention(value=RetentionPolicy.SOURCE)
@interface k{};
@Retention(value=RetentionPolicy.CLASS)
@interface l{};

@SuppressWarnings(value={"all"}) public class  ClassAnnotationsTest extends TestCase {
    @iat public class MC1 {
        @iat public class MC1_1 {
            @iat public class MC1_1_1 {
                @iat public class MC1_1_1_1 {
                    @iat public class MC1_1_1_1_1 {
                        @iat public class MC1_1_1_1_1_1 {
                            @iat public class MC1_1_1_1_1_1_1 {                              
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
    @iat public class MC002<X, Y, Z> {
        @iat public class MC002_1<X1, Y1, Z1> {
            @iat public class MC002_1_1<X2, Y2, Z2> {
                @iat public class MC002_1_1_1<X3, Y3, Z3> {
                    @iat public class MC002_1_1_1_1<X4, Y4, Z4> {
                        @iat public class MC002_1_1_1_1_1<X5, Y5, Z5> {
                            @iat public class MC002_1_1_1_1_1_1<X6, Y6, Z6> {                              
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
    @iat interface MI1 {
        @iat interface MI1_1 {
            @iat interface MI1_1_1 {
                @iat interface MI1_1_1_1 {
                    @iat interface MI1_1_1_1_1 {
                        @iat interface MI1_1_1_1_1_1 {
                            @iat interface MI1_1_1_1_1_1_1 {                              
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

    @iat interface MI002<X, Y, Z> {
        @iat interface MI002_1<X1, Y1, Z1> {
            @iat interface MI002_1_1<X2, Y2, Z2> {
                @iat interface MI002_1_1_1<X3, Y3, Z3> {
                    @iat interface MI002_1_1_1_1<X4, Y4, Z4> {
                        @iat interface MI002_1_1_1_1_1<X5, Y5, Z5> {
                            @iat interface MI002_1_1_1_1_1_1<X6, Y6, Z6> {                              
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
    @interface MA7 {
        @Retention(value=RetentionPolicy.RUNTIME)
        @interface MA1_1 {
            @Retention(value=RetentionPolicy.RUNTIME)
            @interface MA1_1_1 {
                @Retention(value=RetentionPolicy.RUNTIME)
                @interface MA1_1_1_1 {
                    @Retention(value=RetentionPolicy.RUNTIME)
                    @interface MA1_1_1_1_1 {
                        @Retention(value=RetentionPolicy.RUNTIME)
                        @interface MA1_1_1_1_1_1 {
                            @Retention(value=RetentionPolicy.RUNTIME)
                            @interface MA1_1_1_1_1_1_1 {                              
                                @Retention(value=RetentionPolicy.RUNTIME)
                                @interface iiii{
                                    abstract String authorSurname() default "Zapreyev";
                                    abstract String[] authorFullName() default {"Zapreyev", "Serguei", "Stepanovich"};
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

    /**
     * 
     */
    public void test_0() {
        @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(blackMarker = @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author = "UNKNOWN")) class LC111 {
            @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii public void inMeth(
                    @MA7.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(
                            //authorSurname="1",
                            authorFullName="2"
                            ) Class p1
                    ){};
			void inMeth2(){inMeth(int.class); inMeth3();};
			void inMeth3(){inMeth(int.class); inMeth2();};
        };
        Annotation aa[][] = null;
        try{
            java.lang.reflect.Method am[] = LC111.class.getDeclaredMethods();
            for (int i = 0; i < am.length; i++) {
                if (am[i].getName().equals("inMeth")) {
                    aa = am[i].getParameterAnnotations();
                }
            }
        } catch (/*NoSuchMethod*/Exception e) {
            fail("test_4, case 005 FAILED: "+e.toString());
        }
        for (int i = 0; i < aa.length; i++) {
            for(int k = 0; k < aa[i].length; k++) {
                if( i == 0 ) {
                try{
                    java.lang.reflect.Method am[] = ((MA7.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).getClass().getDeclaredMethods();
                    long flg = 0;
                    for (int ii = 0; ii < am.length - 1; ii++){
                        //System.out.println(am[ii].getName());
                        if(am[ii].getName().equals("authorSurname")){
                            flg += 1;
                        } else if(am[ii].getName().equals("authorFullName")){
                            flg += 10;
                        }
                    }
                    if (flg != 11) fail("test_4, case 017 FAILED");
                }catch(Exception _){
                    fail("test_4, case 018 FAILED");
                }
                //System.out.println("<<<"+aa[i][k].toString()+">>>");
                if(!((MA7.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorSurname().equals("Zapreyev")) fail("test_4, case 038 FAILED: "+((MA7.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorSurname());
                if(!((MA7.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorFullName()[0].equals("2")) fail("test_4, case 038 FAILED: "+((MA7.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorFullName());
                }
            }
        }
  }

    @Retention(value=RetentionPolicy.RUNTIME)
    @interface MA1 {
        @Retention(value=RetentionPolicy.RUNTIME)
        @interface MA1_1 {
            @Retention(value=RetentionPolicy.RUNTIME)
            @interface MA1_1_1 {
                @Retention(value=RetentionPolicy.RUNTIME)
                @interface MA1_1_1_1 {
                    @Retention(value=RetentionPolicy.RUNTIME)
                    @interface MA1_1_1_1_1 {
                        @Retention(value=RetentionPolicy.RUNTIME)
                        @interface MA1_1_1_1_1_1 {
                            @Retention(value=RetentionPolicy.RUNTIME)
                            @interface MA1_1_1_1_1_1_1 {                              
                                @Retention(value=RetentionPolicy.RUNTIME)
                                @interface iiii{
                                    abstract String authorSurname() default "Zapreyev";
                                    abstract String[] authorFullName() default {"Zapreyev", "Serguei", "Stepanovich"};

                                    public class Prltr{}
                                    public class Brg{}
                                    public class Krstnn{}
                                    public class Arstcrt{}
                                    public class Clrc{}
                                    abstract Class socialClass() default Prltr.class;
                                    abstract Class[] socialClasses() default {Prltr.class, Brg.class, Krstnn.class, Arstcrt.class, Clrc.class};

                                    int primitive() default 777;
                                    @Retention(value=RetentionPolicy.RUNTIME)
                                    @interface internalAnnotation {
                                        boolean attr1() default true ? false : true;
                                        byte attr2() default (byte) 256;
                                        char attr3() default 'Z';
                                        double attr4() default Double.MAX_VALUE;
                                        float attr5() default Float.MIN_VALUE;
                                        int attr6() default 777;
                                        long attr7() default Long.MAX_VALUE + Long.MIN_VALUE;
                                        short attr8() default (short)32655;
                                        abstract MA1_1_1_1_1 itself() default @MA1_1_1_1_1;
                                    };
                                    abstract MA1_1_1_1_1_1_1 blackMarker() default @MA1_1_1_1_1_1_1;
                                    abstract internalAnnotation[] whiteMarkers() default {@internalAnnotation(attr1 = true, attr7 = -1L), @internalAnnotation, @internalAnnotation()};
                                    //abstract int[] whiteMarkers2() default 6 | 13;
                                    public static enum ME1 {
                                        F_S(2),M_S(3),C_S(4),CL_S(1);
                                        ME1(int value) { 
                                            this.value = value; 
                                        }
                                        private final int value;int m1(){return value+m2();};int m2(){return value+m1();};
                                        public static enum E1_ {G_A_T0, P_T0, V_T0, W_T0;
                                            public static enum E1_1 {G_A_T1, P_T1, V_T1, W_T1;
                                                public static enum E1_2 {G_A_T2, P_T2, V_T2, W_T2;
                                                    public static enum E1_3 {G_A_T3, P_T3, V_T3, W_T3;
                                                        public static enum E1_4 {G_A_T4, P_T4, V_T4, W_T4;
                                                            public static enum E1_5 {G_A_T5, P_T5, V_T5, W_T5;
                                                            };
                                                        };
                                                    };
                                                };
                                            };
                                        };
                                    };

                                    abstract ME1 constant() default ME1.M_S;
                                    abstract ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5[] constants() default {ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5, ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.V_T5, ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5};
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
//////////////////////////////////////////////////////////////////////////////////////////////
    @iat class MC003 {};
    @iat(author = "Serguei S. Zapreyev") class InternalX {};
    @j class InternalO {};
    static {
        @iat class Internal2 {};
    }

    /**
     * 
     */
    @j class MC004 {};
    public void test_1() {
        @j class LC1 {};
        if(!MC004.class.getDeclaredAnnotations()[0].annotationType().equals(j.class)) fail("test_1, case 1 FAILED: "+MC004.class.getDeclaredAnnotations()[0].annotationType().equals(j.class));
        if(MC004.class.getDeclaredAnnotations()[0].hashCode()!=0) fail("test_1, case 002 FAILED: "+MC004.class.getDeclaredAnnotations()[0].hashCode());
        if(!MC004.class.getDeclaredAnnotations()[0].equals(LC1.class.getDeclaredAnnotations()[0])) fail("test_1, case 003 FAILED: "+MC004.class.getDeclaredAnnotations()[0]+"|"+LC1.class.getDeclaredAnnotations()[0]);
        if(!MC004.class.getDeclaredAnnotations()[0].toString().equals("@java.lang.j()")) fail("test_1, case 004 FAILED: "+MC004.class.getDeclaredAnnotations()[0].toString());
    }

    /**
     * 
     */
    public void test_2() {
        if(!MC003.class.getDeclaredAnnotations()[0].annotationType().equals(iat.class)) fail("test_2, case 1 FAILED: "+MC003.class.getDeclaredAnnotations()[0].annotationType().equals(iat.class));
        if(!MC003.class.getDeclaredAnnotations()[0].annotationType().getSimpleName().equals("iat")) fail("test_2, case 002 FAILED: "+MC003.class.getDeclaredAnnotations()[0].annotationType().getSimpleName().equals("i"));
        if(MC003.class.getAnnotation(iat.class)==null) fail("test_2, case 003 FAILED: "+MC003.class.getAnnotation(iat.class));
        if(!MC003.class.isAnnotationPresent(iat.class)) fail("test_2, case 004 FAILED: "+MC003.class.isAnnotationPresent(iat.class));
        if(MC003.class.isAnnotation()) fail("test_2, case 005 FAILED: "+MC003.class.isAnnotation());
  }

// METHOD: ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * checks starting from Method.getDeclaredAnnotations()
     */
    @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii public void test_3() {
        @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(blackMarker = @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author = "UNKNOWN")) class LC1 {};
        Annotation ia = LC1.class.getDeclaredAnnotations()[0];
        Annotation aa[] = null;
        try{
            aa = ClassAnnotationsTest.class.getMethod("test_3").getDeclaredAnnotations();
            if(aa.length!=1) fail("test_3, case 0 FAILED: "+aa.length);
            //System.out.println(aa[0]);
            if(aa[0].toString().replaceAll("Enum\\:","").replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\(", "").
                    replaceFirst("blackMarker=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\(author=Zapreyev 2\\)","").
                    replaceFirst("constants=\\[W_T5, V_T5, G_A_T5\\]","").
                    replaceFirst("authorSurname=Zapreyev","").
                    replaceFirst("socialClass=class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Prltr","").
                    replaceFirst("primitive=777","").
                    replaceFirst("socialClasses=\\[","").
                    replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Prltr","").
                    replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Brg","").
                    replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Krstnn","").
                    replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Arstcrt","").
                    replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Clrc","").
                    replaceFirst("whiteMarkers=\\[","").
                    replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                    replaceFirst("attr7=-1","").
                    replaceFirst("attr1=true","").
                    replaceFirst("attr2=0","").
                    replaceFirst("attr5=1\\.4E-45","").
                    replaceFirst("attr6=777","").
                    replaceFirst("attr3=Z","").
                    replaceFirst("attr4=1\\.7976931348623157E308","").
                    replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                    replaceFirst("attr8=32655","").
                    replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                    replaceFirst("attr7=-1","").
                    replaceFirst("attr1=true","").
                    replaceFirst("attr2=0","").
                    replaceFirst("attr5=1\\.4E-45","").
                    replaceFirst("attr6=777","").
                    replaceFirst("attr3=Z","").
                    replaceFirst("attr4=1\\.7976931348623157E308","").
                    replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                    replaceFirst("attr8=32655","").
                    replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                    replaceFirst("attr7=-1","").
                    replaceFirst("attr1=true","").
                    replaceFirst("attr2=0","").
                    replaceFirst("attr5=1\\.4E-45","").
                    replaceFirst("attr6=777","").
                    replaceFirst("attr3=Z","").
                    replaceFirst("attr4=1\\.7976931348623157E308","").
                    replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                    replaceFirst("attr8=32655","").
                    replaceFirst("authorFullName=\\[Zapreyev, Serguei, Stepanovich\\]","").
                    replaceFirst("constant=M_S","").
                    replaceFirst("attr1=false","").
                    replaceFirst("attr1=false","").
                    replaceAll(" ","").
                    replaceAll("\\)","").
                    replaceAll("\\]","").
                    replaceAll("\\,","").length()!=0) fail("test_3, case 1 FAILED: "+aa[0].toString());
/**/            if(!ClassAnnotationsTest.class.getMethod("test_3").isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class)) fail("test_3, case 002 FAILED: "+MC003.class.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class));
/**/            if(ClassAnnotationsTest.class.getMethod("test_3").getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class) == null) fail("test_3, case 003 FAILED: "+MC003.class.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class));
/**/            if(ClassAnnotationsTest.class.getMethod("test_3").getParameterAnnotations().length!=0) fail("test_3, case 004 FAILED: "+ClassAnnotationsTest.class.getMethod("test_3").getParameterAnnotations().length);
        } catch (NoSuchMethodException e) {
            fail("test_3, case 005 FAILED: "+e.toString());
        }
        for (int i = 0; i < aa.length; i++) {
            Class cuCla = aa[i].annotationType();
            String caNa = cuCla.getCanonicalName();
            String name[] = caNa.split("\\.");
            int j = name.length - 1;
            while (cuCla != null) {
                //System.out.println(name[j]);
                if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[j])) fail("test_3, case 005 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
                if(cuCla.getEnumConstants()!=null) fail("test_3, case 006 FAILED: "+cuCla.getEnumConstants());
                if(cuCla.isEnum()) fail("test_3, case 007 FAILED: "+cuCla.isEnum());
                try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassAnnotationsTest")) fail("test_3, case 008 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
                if(cuCla.getEnclosingMethod()!=null) fail("test_3, case 009 FAILED: "+cuCla.getEnclosingMethod());
                if(cuCla.getEnclosingConstructor()!=null) fail("test_3, case 010 FAILED: "+cuCla.getEnclosingConstructor());
                if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_3, case 011 FAILED: "+cuCla.isMemberClass());
                if(cuCla.isLocalClass()) fail("test_3, case 012 FAILED: "+cuCla.isLocalClass());
                if(cuCla.isAnonymousClass()) fail("test_3, case 013 FAILED: "+cuCla.isAnonymousClass());
                if(cuCla.isSynthetic()) fail("test_3, case 014 FAILED: "+cuCla.isSynthetic());
                if(!cuCla.getCanonicalName().equals(caNa)) fail("test_3, case 015 FAILED: "+cuCla.getCanonicalName());
                caNa = caNa.substring(0, caNa.lastIndexOf('.'));
                if(!cuCla.getSimpleName().equals(name[j])) fail("test_3, case 016 FAILED: "+cuCla.getSimpleName());
                j--;
                cuCla = cuCla.getEnclosingClass();
            }
            try{
                java.lang.reflect.Method am[] = ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getDeclaredMethods();
                long flg = 0;
                for (int ii = 0; ii < am.length - 1; ii++){
                    //System.out.println(am[ii].getName());
                    if(am[ii].getName().equals("authorSurname")){
                        flg += 1;
                    } else if(am[ii].getName().equals("authorFullName")){
                        flg += 10;
                    } else if(am[ii].getName().equals("socialClass")){
                        flg += 100;
                    } else if(am[ii].getName().equals("socialClasses")){
                        flg += 1000;
                    } else if(am[ii].getName().equals("primitive")){
                        flg += 10000;
                    } else if(am[ii].getName().equals("blackMarker")){
                        flg += 100000;
                    } else if(am[ii].getName().equals("whiteMarkers")){
                        flg += 1000000;
                    } else if(am[ii].getName().equals("constant")){
                        flg += 10000000;
                    } else if(am[ii].getName().equals("constants")){
                        flg += 100000000;
                    } else if(am[ii].getName().equals("toString")){
                        flg += 1000000000;
                    } else if(am[ii].getName().equals("hashCode")){
                        flg += 10000000000L;
                    } else if(am[ii].getName().equals("equals")){
                        flg += 100000000000L;
                    }
                }
                if (flg != 111111111111L) fail("test_3, case 017 FAILED");
            }catch(Exception _){
                fail("test_3, case 018 FAILED");
            }
            try{
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("primitive").getDefaultValue()!=null) fail("test_3, case 019 FAILED");
                if(((Integer)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("primitive").getDefaultValue()).intValue()!=777) fail("test_3, case 020 FAILED");
            }catch(NoSuchMethodException _){
                fail("test_3, case 021 FAILED");
            }
            try{
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("socialClass").getDefaultValue()!=null) fail("test_3, case 022 FAILED");
                if(!((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName().equals("java.lang.ClassAnnotationsTest$MA1$MA1_1$MA1_1_1$MA1_1_1_1$MA1_1_1_1_1$MA1_1_1_1_1_1$MA1_1_1_1_1_1_1$iiii$Prltr")) fail("test_3, case 023 FAILED: "+((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName());
            }catch(NoSuchMethodException _){
                fail("test_3, case 024 FAILED");
            }
            try{
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("authorSurname").getDefaultValue()!=null) fail("test_3, case 025 FAILED");
                if(!((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSurname").getDefaultValue()).equals("Zapreyev")) fail("test_3, case 026 FAILED: "+((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSername").getDefaultValue()));
            }catch(NoSuchMethodException _){
                fail("test_3, case 027 FAILED");
            }
            try{
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("blackMarker").getDefaultValue()!=null) fail("test_3, case 028 FAILED");
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()).getClass().getSimpleName().equals("MA1_1_1_1_1_1_1")) fail("test_3, case 029 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()));
            }catch(NoSuchMethodException _){
                fail("test_3, case 030 FAILED");
            }
            try{
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("whiteMarkers").getDefaultValue()!=null) fail("test_3, case 031 FAILED");
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.internalAnnotation[])MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("whiteMarkers").getDefaultValue()).length != 3) fail("test_3, case 032 FAILED");
            }catch(NoSuchMethodException _){
                fail("test_3, case 033 FAILED");
            }
            if(aa[i].annotationType() != ia.annotationType()) fail("test_3, case 034 FAILED: "+aa[i].annotationType().toString()+"|"+ia.annotationType().toString());
            if(aa[i].equals((Object) ia)) fail("test_3, case 035 FAILED: "+aa[i].equals((Object) ia));
            if(aa[i].hashCode() == ia.hashCode()) fail("test_3, case 036 FAILED: "+Integer.toString(aa[i].hashCode())+"|"+Integer.toString(ia.hashCode()));
            if(aa[i].toString().equals(ia.toString())) fail("test_3, case 037 FAILED: "+aa[i].toString()+"|"+ia.toString());
            //System.out.println(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
            if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname().equals("Zapreyev")) fail("test_3, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
            if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0].equals("Zapreyev") ||
                    !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1].equals("Serguei") ||
                    !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2].equals("Stepanovich")) fail("test_3, case 039 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2]);
            if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName().equals("Prltr")) fail("test_3, case 040 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName());
            if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName().equals("Prltr") ||
                    !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName().equals("Brg") ||
                    !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName().equals("Krstnn") ||
                    !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName().equals("Arstcrt") ||
                    !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName().equals("Clrc")) fail("test_3, case 041 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName());
            if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author().equals("Zapreyev 2")) fail("test_3, case 042 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author());
            if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()!=true ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()!=(byte) 256 ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()!='Z' ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()!=Double.MAX_VALUE ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()!=Float.MIN_VALUE ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()!=777 ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()!=Long.MAX_VALUE + Long.MIN_VALUE ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()!=32655 ||
                    !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author().equals("Zapreyev 4")) fail("test_3, case 043 FAILED: "+Boolean.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()) +"|"+
                            Byte.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()) +"|"+
                            Character.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()) +"|"+
                            Double.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()) +"|"+
                            Float.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()) +"|"+
                            Integer.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()) +"|"+
                            Long.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()) +"|"+
                            Short.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()) +"|"+
                            ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author());
            if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant()!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.M_S) fail("test_3, case 044 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant());
            if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5 ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.V_T5 ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5) fail("test_3, case 045 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]);
        }
  }

    /**
     * checks starting from Method.getAnnotations()
     */
    @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii public void test_3_1() {
        @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(blackMarker = @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author = "UNKNOWN")) class LC1 {};
        Annotation ia = LC1.class.getDeclaredAnnotations()[0];
        Annotation aa[] = null;
        try{
            aa = ClassAnnotationsTest.class.getMethod("test_3_1").getAnnotations();
            if(aa.length!=1) fail("test_3, case 0 FAILED: "+aa.length);
            if(aa[0].toString().replaceAll("Enum\\:","").replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\(", "").
                    replaceFirst("blackMarker=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\(author=Zapreyev 2\\)","").
                    replaceFirst("constants=\\[W_T5, V_T5, G_A_T5\\]","").
                    replaceFirst("authorSurname=Zapreyev","").
                    replaceFirst("socialClass=class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Prltr","").
                    replaceFirst("primitive=777","").
                    replaceFirst("socialClasses=\\[","").
                    replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Prltr","").
                    replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Brg","").
                    replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Krstnn","").
                    replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Arstcrt","").
                    replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Clrc","").
                    replaceFirst("whiteMarkers=\\[","").
                    replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                    replaceFirst("attr7=-1","").
                    replaceFirst("attr1=true","").
                    replaceFirst("attr2=0","").
                    replaceFirst("attr5=1\\.4E-45","").
                    replaceFirst("attr6=777","").
                    replaceFirst("attr3=Z","").
                    replaceFirst("attr4=1\\.7976931348623157E308","").
                    replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                    replaceFirst("attr8=32655","").
                    replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                    replaceFirst("attr7=-1","").
                    replaceFirst("attr1=true","").
                    replaceFirst("attr2=0","").
                    replaceFirst("attr5=1\\.4E-45","").
                    replaceFirst("attr6=777","").
                    replaceFirst("attr3=Z","").
                    replaceFirst("attr4=1\\.7976931348623157E308","").
                    replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                    replaceFirst("attr8=32655","").
                    replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                    replaceFirst("attr7=-1","").
                    replaceFirst("attr1=true","").
                    replaceFirst("attr2=0","").
                    replaceFirst("attr5=1\\.4E-45","").
                    replaceFirst("attr6=777","").
                    replaceFirst("attr3=Z","").
                    replaceFirst("attr4=1\\.7976931348623157E308","").
                    replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                    replaceFirst("attr8=32655","").
                    replaceFirst("authorFullName=\\[Zapreyev, Serguei, Stepanovich\\]","").
                    replaceFirst("constant=M_S","").
                    replaceFirst("attr1=false","").
                    replaceFirst("attr1=false","").
                    replaceAll(" ","").
                    replaceAll("\\)","").
                    replaceAll("\\]","").
                    replaceAll("\\,","").length()!=0) fail("test_3_1, case 1 FAILED: "+aa[0].toString());
/**/            if(!ClassAnnotationsTest.class.getMethod("test_3_1").isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class)) fail("test_3_1, case 002 FAILED: "+MC003.class.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class));
/**/            if(ClassAnnotationsTest.class.getMethod("test_3_1").getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class) == null) fail("test_3_1, case 003 FAILED: "+MC003.class.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class));
/**/            if(ClassAnnotationsTest.class.getMethod("test_3_1").getParameterAnnotations().length!=0) fail("test_3_1, case 004 FAILED: "+ClassAnnotationsTest.class.getMethod("test_3_1").getParameterAnnotations().length);
        } catch (NoSuchMethodException e) {
            fail("test_3_1, case 005 FAILED: "+e.toString());
        }
        for (int i = 0; i < aa.length; i++) {
            Class cuCla = aa[i].annotationType();
            String caNa = cuCla.getCanonicalName();
            String name[] = caNa.split("\\.");
            int j = name.length - 1;
            while (cuCla != null) {
                //System.out.println(name[j]);
                if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[j])) fail("test_3_1, case 005 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
                if(cuCla.getEnumConstants()!=null) fail("test_3_1, case 006 FAILED: "+cuCla.getEnumConstants());
                if(cuCla.isEnum()) fail("test_3_1, case 007 FAILED: "+cuCla.isEnum());
                try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassAnnotationsTest")) fail("test_3_1, case 008 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
                if(cuCla.getEnclosingMethod()!=null) fail("test_3_1, case 009 FAILED: "+cuCla.getEnclosingMethod());
                if(cuCla.getEnclosingConstructor()!=null) fail("test_3_1, case 010 FAILED: "+cuCla.getEnclosingConstructor());
                if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_3_1, case 011 FAILED: "+cuCla.isMemberClass());
                if(cuCla.isLocalClass()) fail("test_3_1, case 012 FAILED: "+cuCla.isLocalClass());
                if(cuCla.isAnonymousClass()) fail("test_3_1, case 013 FAILED: "+cuCla.isAnonymousClass());
                if(cuCla.isSynthetic()) fail("test_3_1, case 014 FAILED: "+cuCla.isSynthetic());
                if(!cuCla.getCanonicalName().equals(caNa)) fail("test_3_1, case 015 FAILED: "+cuCla.getCanonicalName());
                caNa = caNa.substring(0, caNa.lastIndexOf('.'));
                if(!cuCla.getSimpleName().equals(name[j])) fail("test_3_1, case 016 FAILED: "+cuCla.getSimpleName());
                j--;
                cuCla = cuCla.getEnclosingClass();
            }
            try{
                java.lang.reflect.Method am[] = ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getDeclaredMethods();
                long flg = 0;
                for (int ii = 0; ii < am.length - 1; ii++){
                    //System.out.println(am[ii].getName());
                    if(am[ii].getName().equals("authorSurname")){
                        flg += 1;
                    } else if(am[ii].getName().equals("authorFullName")){
                        flg += 10;
                    } else if(am[ii].getName().equals("socialClass")){
                        flg += 100;
                    } else if(am[ii].getName().equals("socialClasses")){
                        flg += 1000;
                    } else if(am[ii].getName().equals("primitive")){
                        flg += 10000;
                    } else if(am[ii].getName().equals("blackMarker")){
                        flg += 100000;
                    } else if(am[ii].getName().equals("whiteMarkers")){
                        flg += 1000000;
                    } else if(am[ii].getName().equals("constant")){
                        flg += 10000000;
                    } else if(am[ii].getName().equals("constants")){
                        flg += 100000000;
                    } else if(am[ii].getName().equals("toString")){
                        flg += 1000000000;
                    } else if(am[ii].getName().equals("hashCode")){
                        flg += 10000000000L;
                    } else if(am[ii].getName().equals("equals")){
                        flg += 100000000000L;
                    }
                }
                if (flg != 111111111111L) fail("test_3_1, case 017 FAILED");
            }catch(Exception _){
                fail("test_3_1, case 018 FAILED");
            }
            try{
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("primitive").getDefaultValue()!=null) fail("test_3_1, case 019 FAILED");
                if(((Integer)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("primitive").getDefaultValue()).intValue()!=777) fail("test_3_1, case 020 FAILED");
            }catch(NoSuchMethodException _){
                fail("test_3_1, case 021 FAILED");
            }
            try{
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("socialClass").getDefaultValue()!=null) fail("test_3_1, case 022 FAILED");
                if(!((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName().equals("java.lang.ClassAnnotationsTest$MA1$MA1_1$MA1_1_1$MA1_1_1_1$MA1_1_1_1_1$MA1_1_1_1_1_1$MA1_1_1_1_1_1_1$iiii$Prltr")) fail("test_3_1, case 023 FAILED: "+((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName());
            }catch(NoSuchMethodException _){
                fail("test_3_1, case 024 FAILED");
            }
            try{
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("authorSurname").getDefaultValue()!=null) fail("test_3_1, case 025 FAILED");
                if(!((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSurname").getDefaultValue()).equals("Zapreyev")) fail("test_3_1, case 026 FAILED: "+((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSername").getDefaultValue()));
            }catch(NoSuchMethodException _){
                fail("test_3_1, case 027 FAILED");
            }
            try{
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("blackMarker").getDefaultValue()!=null) fail("test_3_1, case 028 FAILED");
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()).getClass().getSimpleName().equals("MA1_1_1_1_1_1_1")) fail("test_3_1, case 029 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()));
            }catch(NoSuchMethodException _){
                fail("test_3_1, case 030 FAILED");
            }
            try{
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("whiteMarkers").getDefaultValue()!=null) fail("test_3_1, case 031 FAILED");
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.internalAnnotation[])MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("whiteMarkers").getDefaultValue()).length != 3) fail("test_3_1, case 032 FAILED");
            }catch(NoSuchMethodException _){
                fail("test_3_1, case 033 FAILED");
            }
            if(aa[i].annotationType() != ia.annotationType()) fail("test_3_1, case 034 FAILED: "+aa[i].annotationType().toString()+"|"+ia.annotationType().toString());
            if(aa[i].equals((Object) ia)) fail("test_3_1, case 035 FAILED: "+aa[i].equals((Object) ia));
            if(aa[i].hashCode() == ia.hashCode()) fail("test_3_1, case 036 FAILED: "+Integer.toString(aa[i].hashCode())+"|"+Integer.toString(ia.hashCode()));
            if(aa[i].toString().equals(ia.toString())) fail("test_3_1, case 037 FAILED: "+aa[i].toString()+"|"+ia.toString());
            //System.out.println(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
            if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname().equals("Zapreyev")) fail("test_3_1, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
            if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0].equals("Zapreyev") ||
                    !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1].equals("Serguei") ||
                    !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2].equals("Stepanovich")) fail("test_3_1, case 039 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2]);
            if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName().equals("Prltr")) fail("test_3_1, case 040 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName());
            if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName().equals("Prltr") ||
                    !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName().equals("Brg") ||
                    !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName().equals("Krstnn") ||
                    !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName().equals("Arstcrt") ||
                    !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName().equals("Clrc")) fail("test_3_1, case 041 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName());
            if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author().equals("Zapreyev 2")) fail("test_3_1, case 042 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author());
            if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()!=true ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()!=(byte) 256 ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()!='Z' ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()!=Double.MAX_VALUE ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()!=Float.MIN_VALUE ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()!=777 ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()!=Long.MAX_VALUE + Long.MIN_VALUE ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()!=32655 ||
                    !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author().equals("Zapreyev 4")) fail("test_3_1, case 043 FAILED: "+Boolean.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()) +"|"+
                            Byte.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()) +"|"+
                            Character.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()) +"|"+
                            Double.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()) +"|"+
                            Float.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()) +"|"+
                            Integer.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()) +"|"+
                            Long.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()) +"|"+
                            Short.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()) +"|"+
                            ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author());
            if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant()!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.M_S) fail("test_3_1, case 044 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant());
            if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5 ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.V_T5 ||
                    ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5) fail("test_3_1, case 045 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]);
        }
  }

    /**
     * checks starting from Method.getParameterAnnotations()
     */
    @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii public void test_4() {
        @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(blackMarker = @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author = "UNKNOWN")) class LC111 {
            @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii public void inMeth(
                    @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(
                                blackMarker=@MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author="AUTHOR"),
                                constants={MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5}
                            ) Class p1,
                    @MA1 @MA1.MA1_1 @MA1.MA1_1.MA1_1_1 @MA1.MA1_1.MA1_1_1.MA1_1_1_1 @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1(author="S&S&Z") Class p2,
                    @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1 Class p3,
                    @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1(author="Serguei Stepanovich") Class p4,
                    @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii Class p5
                    ){};
			void inMeth2(){inMeth(int.class, int.class, int.class, int.class, int.class); inMeth3();};
			void inMeth3(){inMeth(int.class, int.class, int.class, int.class, int.class); inMeth2();};
        };
        Annotation ia = LC111.class.getDeclaredAnnotations()[0];
        Annotation aa[][] = null;
        try{
            java.lang.reflect.Method am[] = LC111.class.getDeclaredMethods();
            //System.out.println(am.length);
            for (int i = 0; i < am.length; i++) {
                //System.out.println(am[i].toString());
                
                if (am[i].getName().equals("inMeth")) {
                    aa = am[i].getParameterAnnotations();
                    if(!am[i].getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class).annotationType().equals(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class)) fail("test_6_1, case 02 FAILED: "+aa.length);
                    if(am[i].getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.class)!=null) fail("test_6_1, case 03 FAILED: "+aa.length);
                    try{am[i].getAnnotation((Class)null); fail("test_6_1, case 03_1 FAILED: "+aa.length);} catch (NullPointerException e) {}
                    if(!am[i].isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class)) fail("test_6_1, case 04 FAILED: "+aa.length);
                    if(am[i].isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.class)) fail("test_6_1, case 05 FAILED: "+aa.length);
                    try{am[i].isAnnotationPresent((Class)null); fail("test_6_1, case 05_1 FAILED: "+aa.length);} catch (NullPointerException e) {}
                }
            }
            //aa = LC111.class.getMethod("inMeth").getParameterAnnotations();
            if(aa.length!=5) fail("test_4, case 0 FAILED: "+aa.length);
        } catch (/*NoSuchMethod*/Exception e) {
            fail("test_4, case 005 FAILED: "+e.toString());
        }
        for (int i = 0; i < aa.length; i++) {
            for(int k = 0; k < aa[i].length; k++) {
                Class cuCla = aa[i][k].annotationType();
                String caNa = cuCla.getCanonicalName();
                String name[] = caNa.split("\\.");
                int j = name.length - 1;
                if( i == 4 ) {
                    //System.out.println(i+"|"+k+"|"+cuCla.getCanonicalName());
                    if (k!=0) {
                        fail("test_4, case 038 FAILED ");
                    }
                }
                if( i == 3 ) {
                    //System.out.println(i+"|"+k+"|"+cuCla.getCanonicalName());
                    if (k!=0) {
                        fail("test_4, case 038 FAILED ");
                    }
                    if (k==0) {
                        if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1)aa[i][k]).author().equals("Serguei Stepanovich")) fail("test_4, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1)aa[i][k]).author());
                    }
                }
                if( i == 2 ) {
                    //System.out.println(i+"|"+k+"|"+cuCla.getCanonicalName());
                    if (k!=0) {
                        fail("test_4, case 038 FAILED ");
                    }
                }
                if( i == 1 ) {
                    //System.out.println(i+"|"+k+"|"+cuCla.getCanonicalName());
                    if (k==0) {
                        if(!((MA1)aa[i][k]).author().equals("Zapreyev 8")) fail("test_4, case 038 FAILED: "+((MA1)aa[i][k]).author());
                    }
                    if (k==1) {
                        if(!((MA1.MA1_1)aa[i][k]).author().equals("Zapreyev 7")) fail("test_4, case 038 FAILED: "+((MA1.MA1_1)aa[i][k]).author());
                    }
                    if (k==2) {
                        if(!((MA1.MA1_1.MA1_1_1)aa[i][k]).author().equals("Zapreyev 6")) fail("test_4, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1)aa[i][k]).author());
                    }
                    if (k==3) {
                        if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1)aa[i][k]).author().equals("Zapreyev 5")) fail("test_4, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1)aa[i][k]).author());
                    }
                    if (k==4) {
                        if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1)aa[i][k]).author().equals("S&S&Z")) fail("test_4, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1)aa[i][k]).author());
                    }
                }
                if( i == 0 ) {
                while (cuCla != null) {
                    //System.out.println(name[j]);
                    if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[j])) fail("test_4, case 005 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
                    if(cuCla.getEnumConstants()!=null) fail("test_4, case 006 FAILED: "+cuCla.getEnumConstants());
                    if(cuCla.isEnum()) fail("test_4, case 007 FAILED: "+cuCla.isEnum());
                    try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassAnnotationsTest")) fail("test_4, case 008 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
                    if(cuCla.getEnclosingMethod()!=null) fail("test_4, case 009 FAILED: "+cuCla.getEnclosingMethod());
                    if(cuCla.getEnclosingConstructor()!=null) fail("test_4, case 010 FAILED: "+cuCla.getEnclosingConstructor());
                    if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_4, case 011 FAILED: "+cuCla.isMemberClass());
                    if(cuCla.isLocalClass()) fail("test_4, case 012 FAILED: "+cuCla.isLocalClass());
                    if(cuCla.isAnonymousClass()) fail("test_4, case 013 FAILED: "+cuCla.isAnonymousClass());
                    if(cuCla.isSynthetic()) fail("test_4, case 014 FAILED: "+cuCla.isSynthetic());
                    if(!cuCla.getCanonicalName().equals(caNa)) fail("test_4, case 015 FAILED: "+cuCla.getCanonicalName());
                    caNa = caNa.substring(0, caNa.lastIndexOf('.'));
                    if(!cuCla.getSimpleName().equals(name[j])) fail("test_4, case 016 FAILED: "+cuCla.getSimpleName());
                    j--;
                    cuCla = cuCla.getEnclosingClass();
                }
                try{
                    java.lang.reflect.Method am[] = ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).getClass().getDeclaredMethods();
                    long flg = 0;
                    for (int ii = 0; ii < am.length - 1; ii++){
                        //System.out.println(am[ii].getName());
                        if(am[ii].getName().equals("authorSurname")){
                            flg += 1;
                        } else if(am[ii].getName().equals("authorFullName")){
                            flg += 10;
                        } else if(am[ii].getName().equals("socialClass")){
                            flg += 100;
                        } else if(am[ii].getName().equals("socialClasses")){
                            flg += 1000;
                        } else if(am[ii].getName().equals("primitive")){
                            flg += 10000;
                        } else if(am[ii].getName().equals("blackMarker")){
                            flg += 100000;
                        } else if(am[ii].getName().equals("whiteMarkers")){
                            flg += 1000000;
                        } else if(am[ii].getName().equals("constant")){
                            flg += 10000000;
                        } else if(am[ii].getName().equals("constants")){
                            flg += 100000000;
                        } else if(am[ii].getName().equals("toString")){
                            flg += 1000000000;
                        } else if(am[ii].getName().equals("hashCode")){
                            flg += 10000000000L;
                        } else if(am[ii].getName().equals("equals")){
                            flg += 100000000000L;
                        }
                    }
                    if (flg != 111111111111L) fail("test_4, case 017 FAILED");
                }catch(Exception _){
                    fail("test_4, case 018 FAILED");
                }
                try{
                    if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).getClass().getMethod("primitive").getDefaultValue()!=null) fail("test_4, case 019 FAILED");
                    if(((Integer)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("primitive").getDefaultValue()).intValue()!=777) fail("test_4, case 020 FAILED");
                }catch(NoSuchMethodException _){
                    fail("test_4, case 021 FAILED");
                }
                try{
                    if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).getClass().getMethod("socialClass").getDefaultValue()!=null) fail("test_4, case 022 FAILED");
                    if(!((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName().equals("java.lang.ClassAnnotationsTest$MA1$MA1_1$MA1_1_1$MA1_1_1_1$MA1_1_1_1_1$MA1_1_1_1_1_1$MA1_1_1_1_1_1_1$iiii$Prltr")) fail("test_4, case 023 FAILED: "+((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName());
                }catch(NoSuchMethodException _){
                    fail("test_4, case 024 FAILED");
                }
                try{
                    if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).getClass().getMethod("authorSurname").getDefaultValue()!=null) fail("test_4, case 025 FAILED");
                    if(!((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSurname").getDefaultValue()).equals("Zapreyev")) fail("test_4, case 026 FAILED: "+((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSername").getDefaultValue()));
                }catch(NoSuchMethodException _){
                    fail("test_4, case 027 FAILED");
                }
                try{
                    if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).getClass().getMethod("blackMarker").getDefaultValue()!=null) fail("test_4, case 028 FAILED");
                    //System.out.println("1XXX>>> >>> >>>>"+MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue());
                    //System.out.println("1XXX>>> >>> >>>>"+MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue().getClass());
                    //System.out.println("1XXX>>> >>> >>>>"+MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue().getClass().getSuperclass());
                    //System.out.println("1XXX>>> >>> >>>>"+MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue().getClass().getInterfaces()[0]);
                    //System.out.println("2XXX>>> >>> >>>>");
                    System.out.println((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue());
                    //System.out.println("3XXX>>> >>> >>>>");
                    if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()).getClass().getSimpleName().equals("MA1_1_1_1_1_1_1")) fail("test_4, case 029 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()));
                }catch(NoSuchMethodException _){
                    fail("test_4, case 030 FAILED");
                }
                try{
                    if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).getClass().getMethod("whiteMarkers").getDefaultValue()!=null) fail("test_4, case 031 FAILED");
                    if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.internalAnnotation[])MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("whiteMarkers").getDefaultValue()).length != 3) fail("test_4, case 032 FAILED");
                }catch(NoSuchMethodException _){
                    fail("test_4, case 033 FAILED");
                }
                if(aa[i][k].annotationType() != ia.annotationType()) fail("test_4, case 034 FAILED: "+aa[i][k].annotationType().toString()+"|"+ia.annotationType().toString());
                if(aa[i][k].equals((Object) ia)) fail("test_4, case 035 FAILED: "+aa[i][k].equals((Object) ia));
                if(aa[i][k].hashCode() == ia.hashCode()) fail("test_4, case 036 FAILED: "+Integer.toString(aa[i][k].hashCode())+"|"+Integer.toString(ia.hashCode()));
                if(aa[i][k].toString().equals(ia.toString())) fail("test_4, case 037 FAILED: "+aa[i][k].toString()+"|"+ia.toString());
                //System.out.println(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorSurname());
                if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorSurname().equals("Zapreyev")) fail("test_4, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorSurname());
                if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorFullName()[0].equals("Zapreyev") ||
                        !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorFullName()[1].equals("Serguei") ||
                        !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorFullName()[2].equals("Stepanovich")) fail("test_4, case 039 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorFullName()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorFullName()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorFullName()[2]);
                if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClass().getSimpleName().equals("Prltr")) fail("test_4, case 040 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClass().getSimpleName());
                if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[0].getSimpleName().equals("Prltr") ||
                        !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[1].getSimpleName().equals("Brg") ||
                        !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[2].getSimpleName().equals("Krstnn") ||
                        !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[3].getSimpleName().equals("Arstcrt") ||
                        !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[4].getSimpleName().equals("Clrc")) fail("test_4, case 041 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[0].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[1].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[2].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[3].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[4].getSimpleName());
                if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).blackMarker().author().equals("AUTHOR")) fail("test_4, case 042 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).blackMarker().author());
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr1()!=true ||
                        ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr2()!=(byte) 256 ||
                        ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr3()!='Z' ||
                        ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr4()!=Double.MAX_VALUE ||
                        ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr5()!=Float.MIN_VALUE ||
                        ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr6()!=777 ||
                        ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr7()!=Long.MAX_VALUE + Long.MIN_VALUE ||
                        ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr8()!=32655 ||
                        !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].itself().author().equals("Zapreyev 4")) fail("test_4, case 043 FAILED: "+Boolean.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr1()) +"|"+
                                Byte.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr2()) +"|"+
                                Character.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr3()) +"|"+
                                Double.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr4()) +"|"+
                                Float.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr5()) +"|"+
                                Integer.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr6()) +"|"+
                                Long.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr7()) +"|"+
                                Short.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr8()) +"|"+
                                ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].itself().author());
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constant()!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.M_S) fail("test_4, case 044 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constant());
                if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[0]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                        ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[1]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                        ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[2]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                        ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[3]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                        ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[4]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5) fail("test_4, case 045 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[2]);
                }
            }
        }
  }

    /**
     * checks for Method.getParameterAnnotations() for regional conditions
     */
    public void test_4_1() {
        class LC111 {
            public void inMeth(
                    @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(
                                blackMarker=@MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author="AUTHOR"),
                                constants={MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5}
                            ) Class p1,
                    @MA1 @MA1.MA1_1 @MA1.MA1_1.MA1_1_1 @MA1.MA1_1.MA1_1_1.MA1_1_1_1 @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1(author="S&S&Z") Class p2,
                    @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1 Class p3,
                    @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1(author="Serguei Stepanovich") Class p4,
                    Class p5
                    ){};
			void inMeth2(){inMeth(int.class, int.class, int.class, int.class, int.class); inMeth3(int.class, int.class, int.class, int.class);};
			void inMeth3(@k @l Class p1, @k Class p2, Class p3, @l Class p4){inMeth(int.class, int.class, int.class, int.class, int.class); inMeth2();};
        };
        Annotation aa[][] = null;
        try{
            java.lang.reflect.Method am[] = LC111.class.getDeclaredMethods();
            for (int i = 0; i < am.length; i++) {
                if (am[i].getName().equals("inMeth")) {
                    aa = am[i].getParameterAnnotations();
                    if (!(aa instanceof Annotation[][] && aa.length == 5)) fail("test_4_1, case 001 FAILED: "+aa.length);
                    if (!(aa[0] instanceof Annotation[] && aa[0].length == 1)) fail("test_4_1, case 002 FAILED: "+aa[0].length);
                    if (!(aa[1] instanceof Annotation[] && aa[1].length == 5)) fail("test_4_1, case 003 FAILED: "+aa[1].length);
                    if (!(aa[2] instanceof Annotation[] && aa[2].length == 1)) fail("test_4_1, case 004 FAILED: "+aa[2].length);
                    if (!(aa[3] instanceof Annotation[] && aa[3].length == 1)) fail("test_4_1, case 005 FAILED: "+aa[3].length);
                    if (!(aa[4] instanceof Annotation[] && aa[4].length == 0)) fail("test_4_1, case 006 FAILED: "+aa[4].length);
                }
                if (am[i].getName().equals("inMeth2")) {
                    aa = am[i].getParameterAnnotations();
                    if (!(aa instanceof Annotation[][] && aa.length == 0)) fail("test_4_1, case 007 FAILED: "+aa.length);
                }
                if (am[i].getName().equals("inMeth3")) {
                    aa = am[i].getParameterAnnotations();
                    if (!(aa.length == 4)) fail("test_4_1, case 005 FAILED: "+aa.length);
                    if (!(aa[0] instanceof Annotation[] && aa[0].length == 0)) fail("test_4_1, case 008 FAILED: "+aa[0].length);
                    if (!(aa[1] instanceof Annotation[] && aa[1].length == 0)) fail("test_4_1, case 009 FAILED: "+aa[1].length);
                    if (!(aa[2] instanceof Annotation[] && aa[2].length == 0)) fail("test_4_1, case 010 FAILED: "+aa[2].length);
                    if (!(aa[3] instanceof Annotation[] && aa[3].length == 0)) fail("test_4_1, case 011 FAILED: "+aa[3].length);
                }
            }
        } catch (/*NoSuchMethod*/Exception e) {
            fail("test_4_1, case 012 FAILED: "+e.toString());
        }
	}

    /**
     * checks for nulls
     */
    @anna public void test_5() {
        try{
            try{
                new ClassAnnotationsTest().getClass().getMethod("test_5").isAnnotationPresent((Class)null);
                fail("test_5, case 1 FAILED: NullPointerException should rise.");
            } catch (NullPointerException _) {
            }
            try{
                new ClassAnnotationsTest().getClass().getMethod("test_5").getAnnotation((Class)null);
                fail("test_5, case 002 FAILED: NullPointerException should rise.");
            } catch (NullPointerException _) {
            }
        } catch (NoSuchMethodException e) {
            fail("test_5, case 003 FAILED: "+e.toString());
        }
  }

//  CONSTRUCTOR: ////////////////////////////////////////////////////////////////////////////////////////////////
     /**
      * checks starting from Constructor.getDeclaredAnnotations()
      */
     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii public void test_6() {
         @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(blackMarker = @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author = "UNKNOWN")) class LC111 {
             @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii public LC111(){};
         };
         Annotation ia = LC111.class.getDeclaredAnnotations()[0];
         Annotation aa[] = null;
         try{
             //aa = LC111.class.getConstructor().getDeclaredAnnotations();
             aa = LC111.class.getDeclaredConstructors()[0].getDeclaredAnnotations();
             if(aa.length!=1) fail("test_6, case 0 FAILED: "+aa.length);
             //System.out.println(aa[0]);
             //System.out.println(aa[0].toString());
             if(aa[0].toString().replaceAll("Enum\\:","").replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\(", "").
                     replaceFirst("blackMarker=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\(author=Zapreyev 2\\)","").
                     replaceFirst("constants=\\[W_T5, V_T5, G_A_T5\\]","").
                     replaceFirst("authorSurname=Zapreyev","").
                     replaceFirst("socialClass=class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Prltr","").
                     replaceFirst("primitive=777","").
                     replaceFirst("socialClasses=\\[","").
                     replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Prltr","").
                     replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Brg","").
                     replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Krstnn","").
                     replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Arstcrt","").
                     replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Clrc","").
                     replaceFirst("whiteMarkers=\\[","").
                     replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                     replaceFirst("attr7=-1","").
                     replaceFirst("attr1=true","").
                     replaceFirst("attr2=0","").
                     replaceFirst("attr5=1\\.4E-45","").
                     replaceFirst("attr6=777","").
                     replaceFirst("attr3=Z","").
                     replaceFirst("attr4=1\\.7976931348623157E308","").
                     replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                     replaceFirst("attr8=32655","").
                     replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                     replaceFirst("attr7=-1","").
                     replaceFirst("attr1=true","").
                     replaceFirst("attr2=0","").
                     replaceFirst("attr5=1\\.4E-45","").
                     replaceFirst("attr6=777","").
                     replaceFirst("attr3=Z","").
                     replaceFirst("attr4=1\\.7976931348623157E308","").
                     replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                     replaceFirst("attr8=32655","").
                     replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                     replaceFirst("attr7=-1","").
                     replaceFirst("attr1=true","").
                     replaceFirst("attr2=0","").
                     replaceFirst("attr5=1\\.4E-45","").
                     replaceFirst("attr6=777","").
                     replaceFirst("attr3=Z","").
                     replaceFirst("attr4=1\\.7976931348623157E308","").
                     replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                     replaceFirst("attr8=32655","").
                     replaceFirst("authorFullName=\\[Zapreyev, Serguei, Stepanovich\\]","").
                     replaceFirst("constant=M_S","").
                     replaceFirst("attr1=false","").
                     replaceFirst("attr1=false","").
                     replaceAll(" ","").
                     replaceAll("\\)","").
                     replaceAll("\\]","").
                     replaceAll("\\,","").length()!=0) fail("test_6, case 1 FAILED: "+aa[0].toString());
 /**/            if(!ClassAnnotationsTest.class.getMethod("test_6").isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class)) fail("test_6, case 002 FAILED: "+MC003.class.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class));
 /**/            if(ClassAnnotationsTest.class.getMethod("test_6").getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class) == null) fail("test_6, case 003 FAILED: "+MC003.class.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class));
 /**/            if(ClassAnnotationsTest.class.getMethod("test_6").getParameterAnnotations().length!=0) fail("test_6, case 004 FAILED: "+ClassAnnotationsTest.class.getMethod("test_6").getParameterAnnotations().length);
         } catch (NoSuchMethodException e) {
             fail("test_6, case 005 FAILED: "+e.toString());
         }
         for (int i = 0; i < aa.length; i++) {
             Class cuCla = aa[i].annotationType();
             String caNa = cuCla.getCanonicalName();
             String name[] = caNa.split("\\.");
             int j = name.length - 1;
             while (cuCla != null) {
                 //System.out.println(name[j]);
                 if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[j])) fail("test_6, case 005 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
                 if(cuCla.getEnumConstants()!=null) fail("test_6, case 006 FAILED: "+cuCla.getEnumConstants());
                 if(cuCla.isEnum()) fail("test_6, case 007 FAILED: "+cuCla.isEnum());
                 try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassAnnotationsTest")) fail("test_6, case 008 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
                 if(cuCla.getEnclosingMethod()!=null) fail("test_6, case 009 FAILED: "+cuCla.getEnclosingMethod());
                 if(cuCla.getEnclosingConstructor()!=null) fail("test_6, case 010 FAILED: "+cuCla.getEnclosingConstructor());
                 if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_6, case 011 FAILED: "+cuCla.isMemberClass());
                 if(cuCla.isLocalClass()) fail("test_6, case 012 FAILED: "+cuCla.isLocalClass());
                 if(cuCla.isAnonymousClass()) fail("test_6, case 013 FAILED: "+cuCla.isAnonymousClass());
                 if(cuCla.isSynthetic()) fail("test_6, case 014 FAILED: "+cuCla.isSynthetic());
                 if(!cuCla.getCanonicalName().equals(caNa)) fail("test_6, case 015 FAILED: "+cuCla.getCanonicalName());
                 caNa = caNa.substring(0, caNa.lastIndexOf('.'));
                 if(!cuCla.getSimpleName().equals(name[j])) fail("test_6, case 016 FAILED: "+cuCla.getSimpleName());
                 j--;
                 cuCla = cuCla.getEnclosingClass();
             }
             try{
                 java.lang.reflect.Method am[] = ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getDeclaredMethods();
                 long flg = 0;
                 for (int ii = 0; ii < am.length - 1; ii++){
                     //System.out.println(am[ii].getName());
                     if(am[ii].getName().equals("authorSurname")){
                         flg += 1;
                     } else if(am[ii].getName().equals("authorFullName")){
                         flg += 10;
                     } else if(am[ii].getName().equals("socialClass")){
                         flg += 100;
                     } else if(am[ii].getName().equals("socialClasses")){
                         flg += 1000;
                     } else if(am[ii].getName().equals("primitive")){
                         flg += 10000;
                     } else if(am[ii].getName().equals("blackMarker")){
                         flg += 100000;
                     } else if(am[ii].getName().equals("whiteMarkers")){
                         flg += 1000000;
                     } else if(am[ii].getName().equals("constant")){
                         flg += 10000000;
                     } else if(am[ii].getName().equals("constants")){
                         flg += 100000000;
                     } else if(am[ii].getName().equals("toString")){
                         flg += 1000000000;
                     } else if(am[ii].getName().equals("hashCode")){
                         flg += 10000000000L;
                     } else if(am[ii].getName().equals("equals")){
                         flg += 100000000000L;
                     }
                 }
                 if (flg != 111111111111L) fail("test_6, case 017 FAILED");
             }catch(Exception _){
                 fail("test_6, case 018 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("primitive").getDefaultValue()!=null) fail("test_6, case 019 FAILED");
                 if(((Integer)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("primitive").getDefaultValue()).intValue()!=777) fail("test_6, case 020 FAILED");
             }catch(NoSuchMethodException _){
                 fail("test_6, case 021 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("socialClass").getDefaultValue()!=null) fail("test_6, case 022 FAILED");
                 if(!((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName().equals("java.lang.ClassAnnotationsTest$MA1$MA1_1$MA1_1_1$MA1_1_1_1$MA1_1_1_1_1$MA1_1_1_1_1_1$MA1_1_1_1_1_1_1$iiii$Prltr")) fail("test_6, case 023 FAILED: "+((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName());
             }catch(NoSuchMethodException _){
                 fail("test_6, case 024 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("authorSurname").getDefaultValue()!=null) fail("test_6, case 025 FAILED");
                 if(!((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSurname").getDefaultValue()).equals("Zapreyev")) fail("test_6, case 026 FAILED: "+((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSername").getDefaultValue()));
             }catch(NoSuchMethodException _){
                 fail("test_6, case 027 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("blackMarker").getDefaultValue()!=null) fail("test_6, case 028 FAILED");
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()).getClass().getSimpleName().equals("MA1_1_1_1_1_1_1")) fail("test_6, case 029 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()));
             }catch(NoSuchMethodException _){
                 fail("test_6, case 030 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("whiteMarkers").getDefaultValue()!=null) fail("test_6, case 031 FAILED");
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.internalAnnotation[])MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("whiteMarkers").getDefaultValue()).length != 3) fail("test_6, case 032 FAILED");
             }catch(NoSuchMethodException _){
                 fail("test_6, case 033 FAILED");
             }
             if(aa[i].annotationType() != ia.annotationType()) fail("test_6, case 034 FAILED: "+aa[i].annotationType().toString()+"|"+ia.annotationType().toString());
             if(aa[i].equals((Object) ia)) fail("test_6, case 035 FAILED: "+aa[i].equals((Object) ia));
             if(aa[i].hashCode() == ia.hashCode()) fail("test_6, case 036 FAILED: "+Integer.toString(aa[i].hashCode())+"|"+Integer.toString(ia.hashCode()));
             if(aa[i].toString().equals(ia.toString())) fail("test_6, case 037 FAILED: "+aa[i].toString()+"|"+ia.toString());
             //System.out.println(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname().equals("Zapreyev")) fail("test_6, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0].equals("Zapreyev") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1].equals("Serguei") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2].equals("Stepanovich")) fail("test_6, case 039 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2]);
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName().equals("Prltr")) fail("test_6, case 040 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName().equals("Prltr") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName().equals("Brg") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName().equals("Krstnn") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName().equals("Arstcrt") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName().equals("Clrc")) fail("test_6, case 041 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author().equals("Zapreyev 2")) fail("test_6, case 042 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author());
             if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()!=true ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()!=(byte) 256 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()!='Z' ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()!=Double.MAX_VALUE ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()!=Float.MIN_VALUE ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()!=777 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()!=Long.MAX_VALUE + Long.MIN_VALUE ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()!=32655 ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author().equals("Zapreyev 4")) fail("test_6, case 043 FAILED: "+Boolean.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()) +"|"+
                             Byte.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()) +"|"+
                             Character.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()) +"|"+
                             Double.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()) +"|"+
                             Float.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()) +"|"+
                             Integer.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()) +"|"+
                             Long.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()) +"|"+
                             Short.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()) +"|"+
                             ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author());
             if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant()!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.M_S) fail("test_6, case 044 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant());
             if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.V_T5 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5) fail("test_6, case 045 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]);
         }
   }

     /**
      * checks starting from Constructor.getAnnotations()
      */
     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii public void test_6_1() {
         @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(blackMarker = @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author = "UNKNOWN")) class LC111 {
             @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii @MA1 @MA1.MA1_1 @MA1.MA1_1.MA1_1_1 public LC111(){};
         };
         Annotation ia = LC111.class.getDeclaredAnnotations()[0];
         Annotation aa[] = null;
         try{
             aa = LC111.class.getDeclaredConstructors()[0].getAnnotations();
             if(!LC111.class.getDeclaredConstructors()[0].getAnnotation(MA1.MA1_1.MA1_1_1.class).annotationType().equals(MA1.MA1_1.MA1_1_1.class)) fail("test_6_1, case 02 FAILED: "+aa.length);
             if(LC111.class.getDeclaredConstructors()[0].getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.class)!=null) fail("test_6_1, case 03 FAILED: "+aa.length);
             try{LC111.class.getDeclaredConstructors()[0].getAnnotation((Class)null); fail("test_6_1, case 03_1 FAILED: "+aa.length);} catch (NullPointerException e) {}
             if(!LC111.class.getDeclaredConstructors()[0].isAnnotationPresent(MA1.class)) fail("test_6_1, case 04 FAILED: "+aa.length);
             if(LC111.class.getDeclaredConstructors()[0].isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.class)) fail("test_6_1, case 05 FAILED: "+aa.length);
             try{LC111.class.getDeclaredConstructors()[0].isAnnotationPresent((Class)null); fail("test_6_1, case 05_1 FAILED: "+aa.length);} catch (NullPointerException e) {}
             if(aa.length!=4) fail("test_6, case 0 FAILED: "+aa.length);
             if(aa[0].toString().replaceAll("Enum\\:","").replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\(", "").
                     replaceFirst("blackMarker=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\(author=Zapreyev 2\\)","").
                     replaceFirst("constants=\\[W_T5, V_T5, G_A_T5\\]","").
                     replaceFirst("authorSurname=Zapreyev","").
                     replaceFirst("socialClass=class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Prltr","").
                     replaceFirst("primitive=777","").
                     replaceFirst("socialClasses=\\[","").
                     replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Prltr","").
                     replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Brg","").
                     replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Krstnn","").
                     replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Arstcrt","").
                     replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Clrc","").
                     replaceFirst("whiteMarkers=\\[","").
                     replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                     replaceFirst("attr7=-1","").
                     replaceFirst("attr1=true","").
                     replaceFirst("attr2=0","").
                     replaceFirst("attr5=1\\.4E-45","").
                     replaceFirst("attr6=777","").
                     replaceFirst("attr3=Z","").
                     replaceFirst("attr4=1\\.7976931348623157E308","").
                     replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                     replaceFirst("attr8=32655","").
                     replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                     replaceFirst("attr7=-1","").
                     replaceFirst("attr1=true","").
                     replaceFirst("attr2=0","").
                     replaceFirst("attr5=1\\.4E-45","").
                     replaceFirst("attr6=777","").
                     replaceFirst("attr3=Z","").
                     replaceFirst("attr4=1\\.7976931348623157E308","").
                     replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                     replaceFirst("attr8=32655","").
                     replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                     replaceFirst("attr7=-1","").
                     replaceFirst("attr1=true","").
                     replaceFirst("attr2=0","").
                     replaceFirst("attr5=1\\.4E-45","").
                     replaceFirst("attr6=777","").
                     replaceFirst("attr3=Z","").
                     replaceFirst("attr4=1\\.7976931348623157E308","").
                     replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                     replaceFirst("attr8=32655","").
                     replaceFirst("authorFullName=\\[Zapreyev, Serguei, Stepanovich\\]","").
                     replaceFirst("constant=M_S","").
                     replaceFirst("attr1=false","").
                     replaceFirst("attr1=false","").
                     replaceAll(" ","").
                     replaceAll("\\)","").
                     replaceAll("\\]","").
                     replaceAll("\\,","").length()!=0) fail("test_6_1, case 1 FAILED: "+aa[0].toString());
 /**/            if(!ClassAnnotationsTest.class.getMethod("test_6_1").isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class)) fail("test_6_1, case 002 FAILED: "+MC003.class.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class));
 /**/            if(ClassAnnotationsTest.class.getMethod("test_6_1").getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class) == null) fail("test_6_1, case 003 FAILED: "+MC003.class.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class));
 /**/            if(ClassAnnotationsTest.class.getMethod("test_6_1").getParameterAnnotations().length!=0) fail("test_6_1, case 004 FAILED: "+ClassAnnotationsTest.class.getMethod("test_6_1").getParameterAnnotations().length);
         } catch (NoSuchMethodException e) {
             fail("test_6_1, case 005 FAILED: "+e.toString());
         }
         for (int i = 0; i < aa.length; i++) {
             Class cuCla = aa[i].annotationType();
             String caNa = cuCla.getCanonicalName();
             String name[] = caNa.split("\\.");
             int j = name.length - 1;
             if (i == 1) {
                 try{
                     if(((MA1)aa[i]).getClass().getMethod("author").getDefaultValue()!=null) fail("test_6_1, case 025 FAILED");
                     if(!((String)MA1.class.getMethod("author").getDefaultValue()).equals("Zapreyev 8")) fail("test_6_1, case 026 FAILED: "+((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSername").getDefaultValue()));
                 }catch(NoSuchMethodException _){
                     fail("test_6_1, case 027 FAILED");
                 }
             }
             if (i == 0) {
             while (cuCla != null) {
                 //System.out.println(name[j]);
                 if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[j])) fail("test_6_1, case 005 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
                 if(cuCla.getEnumConstants()!=null) fail("test_6_1, case 006 FAILED: "+cuCla.getEnumConstants());
                 if(cuCla.isEnum()) fail("test_6_1, case 007 FAILED: "+cuCla.isEnum());
                 try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassAnnotationsTest")) fail("test_6_1, case 008 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
                 if(cuCla.getEnclosingMethod()!=null) fail("test_6_1, case 009 FAILED: "+cuCla.getEnclosingMethod());
                 if(cuCla.getEnclosingConstructor()!=null) fail("test_6_1, case 010 FAILED: "+cuCla.getEnclosingConstructor());
                 if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_6_1, case 011 FAILED: "+cuCla.isMemberClass());
                 if(cuCla.isLocalClass()) fail("test_6_1, case 012 FAILED: "+cuCla.isLocalClass());
                 if(cuCla.isAnonymousClass()) fail("test_6_1, case 013 FAILED: "+cuCla.isAnonymousClass());
                 if(cuCla.isSynthetic()) fail("test_6_1, case 014 FAILED: "+cuCla.isSynthetic());
                 if(!cuCla.getCanonicalName().equals(caNa)) fail("test_6_1, case 015 FAILED: "+cuCla.getCanonicalName());
                 caNa = caNa.substring(0, caNa.lastIndexOf('.'));
                 if(!cuCla.getSimpleName().equals(name[j])) fail("test_6_1, case 016 FAILED: "+cuCla.getSimpleName());
                 j--;
                 cuCla = cuCla.getEnclosingClass();
             }
             try{
                 java.lang.reflect.Method am[] = ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getDeclaredMethods();
                 long flg = 0;
                 for (int ii = 0; ii < am.length - 1; ii++){
                     //System.out.println(am[ii].getName());
                     if(am[ii].getName().equals("authorSurname")){
                         flg += 1;
                     } else if(am[ii].getName().equals("authorFullName")){
                         flg += 10;
                     } else if(am[ii].getName().equals("socialClass")){
                         flg += 100;
                     } else if(am[ii].getName().equals("socialClasses")){
                         flg += 1000;
                     } else if(am[ii].getName().equals("primitive")){
                         flg += 10000;
                     } else if(am[ii].getName().equals("blackMarker")){
                         flg += 100000;
                     } else if(am[ii].getName().equals("whiteMarkers")){
                         flg += 1000000;
                     } else if(am[ii].getName().equals("constant")){
                         flg += 10000000;
                     } else if(am[ii].getName().equals("constants")){
                         flg += 100000000;
                     } else if(am[ii].getName().equals("toString")){
                         flg += 1000000000;
                     } else if(am[ii].getName().equals("hashCode")){
                         flg += 10000000000L;
                     } else if(am[ii].getName().equals("equals")){
                         flg += 100000000000L;
                     }
                 }
                 if (flg != 111111111111L) fail("test_6_1, case 017 FAILED");
             }catch(Exception _){
                 fail("test_6_1, case 018 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("primitive").getDefaultValue()!=null) fail("test_6_1, case 019 FAILED");
                 if(((Integer)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("primitive").getDefaultValue()).intValue()!=777) fail("test_6_1, case 020 FAILED");
             }catch(NoSuchMethodException _){
                 fail("test_6_1, case 021 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("socialClass").getDefaultValue()!=null) fail("test_6_1, case 022 FAILED");
                 if(!((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName().equals("java.lang.ClassAnnotationsTest$MA1$MA1_1$MA1_1_1$MA1_1_1_1$MA1_1_1_1_1$MA1_1_1_1_1_1$MA1_1_1_1_1_1_1$iiii$Prltr")) fail("test_6_1, case 023 FAILED: "+((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName());
             }catch(NoSuchMethodException _){
                 fail("test_6_1, case 024 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("authorSurname").getDefaultValue()!=null) fail("test_6_1, case 025 FAILED");
                 if(!((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSurname").getDefaultValue()).equals("Zapreyev")) fail("test_6_1, case 026 FAILED: "+((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSername").getDefaultValue()));
             }catch(NoSuchMethodException _){
                 fail("test_6_1, case 027 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("blackMarker").getDefaultValue()!=null) fail("test_6_1, case 028 FAILED");
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()).getClass().getSimpleName().equals("MA1_1_1_1_1_1_1")) fail("test_6_1, case 029 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()));
             }catch(NoSuchMethodException _){
                 fail("test_6_1, case 030 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("whiteMarkers").getDefaultValue()!=null) fail("test_6_1, case 031 FAILED");
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.internalAnnotation[])MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("whiteMarkers").getDefaultValue()).length != 3) fail("test_6_1, case 032 FAILED");
             }catch(NoSuchMethodException _){
                 fail("test_6_1, case 033 FAILED");
             }
             if(aa[i].annotationType() != ia.annotationType()) fail("test_6_1, case 034 FAILED: "+aa[i].annotationType().toString()+"|"+ia.annotationType().toString());
             if(aa[i].equals((Object) ia)) fail("test_6_1, case 035 FAILED: "+aa[i].equals((Object) ia));
             if(aa[i].hashCode() == ia.hashCode()) fail("test_6_1, case 036 FAILED: "+Integer.toString(aa[i].hashCode())+"|"+Integer.toString(ia.hashCode()));
             if(aa[i].toString().equals(ia.toString())) fail("test_6_1, case 037 FAILED: "+aa[i].toString()+"|"+ia.toString());
             //System.out.println(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname().equals("Zapreyev")) fail("test_6_1, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0].equals("Zapreyev") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1].equals("Serguei") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2].equals("Stepanovich")) fail("test_6_1, case 039 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2]);
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName().equals("Prltr")) fail("test_6_1, case 040 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName().equals("Prltr") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName().equals("Brg") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName().equals("Krstnn") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName().equals("Arstcrt") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName().equals("Clrc")) fail("test_6_1, case 041 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author().equals("Zapreyev 2")) fail("test_6_1, case 042 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author());
             if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()!=true ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()!=(byte) 256 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()!='Z' ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()!=Double.MAX_VALUE ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()!=Float.MIN_VALUE ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()!=777 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()!=Long.MAX_VALUE + Long.MIN_VALUE ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()!=32655 ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author().equals("Zapreyev 4")) fail("test_6_1, case 043 FAILED: "+Boolean.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()) +"|"+
                             Byte.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()) +"|"+
                             Character.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()) +"|"+
                             Double.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()) +"|"+
                             Float.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()) +"|"+
                             Integer.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()) +"|"+
                             Long.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()) +"|"+
                             Short.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()) +"|"+
                             ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author());
             if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant()!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.M_S) fail("test_6_1, case 044 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant());
             if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.V_T5 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5) fail("test_6_1, case 045 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]);
             }
         }
   }

     /**
      * checks starting from Constructor.getParameterAnnotations()
      */
     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii public void test_7() {
         @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(blackMarker = @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author = "UNKNOWN")) class LC111 {
             @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii public LC111(
                     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(
                                 blackMarker=@MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author="AUTHOR"),
                                 constants={MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5}
                             ) Class p1,
                     @MA1 @MA1.MA1_1 @MA1.MA1_1.MA1_1_1 @MA1.MA1_1.MA1_1_1.MA1_1_1_1 @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1(author="S&S&Z") Class p2,
                     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1 Class p3,
                     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1(author="Serguei Stepanovich") Class p4,
                     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii Class p5
                     ){};
         };
         //Annotation ia = LC111.class.getDeclaredAnnotations()[0];
         Annotation ia = new LC111(int.class, int.class, int.class, int.class, int.class).getClass().getDeclaredAnnotations()[0];
         Annotation aa[][] = null;
         try{
             for (int i = 0; i < LC111.class.getDeclaredConstructors().length; i++) {
//               System.out.println(">>> >>> >>>"+LC111.class.getDeclaredConstructors()[i]);
             }
             java.lang.reflect.Constructor ac = LC111.class.getDeclaredConstructors()[0]; //LC111.class.getConstructor(Class.class, Class.class, Class.class, Class.class, Class.class);
             if(!ac.getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class).annotationType().equals(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class)) fail("test_7, case 02 FAILED: "+aa.length);
             if(ac.getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.class)!=null) fail("test_7, case 03 FAILED: "+aa.length);
             try{ac.getAnnotation((Class)null); fail("test_7, case 03_1 FAILED: "+aa.length);} catch (NullPointerException e) {}
             if(!ac.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class)) fail("test_7, case 04 FAILED: "+aa.length);
             if(ac.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.class)) fail("test_7, case 05 FAILED: "+aa.length);
             try{ac.isAnnotationPresent((Class)null); fail("test_7, case 05_1 FAILED: "+aa.length);} catch (NullPointerException e) {}
             aa = ac.getParameterAnnotations();
             if(aa.length!=5) fail("test_7, case 0 FAILED: "+aa.length);
         } catch (/*NoSuchMethod*/Exception e) {
             e.printStackTrace();
             fail("test_7, case 005 FAILED: "+e.toString());
         }
         for (int i = 0; i < aa.length; i++) {
             for(int k = 0; k < aa[i].length; k++) {
                 Class cuCla = aa[i][k].annotationType();
                 String caNa = cuCla.getCanonicalName();
                 String name[] = caNa.split("\\.");
                 int j = name.length - 1;
                 if( i == 4 ) {
                     
                     if (k!=0) {
                         fail("test_7, case 038 FAILED ");
                     }
                 }
                 if( i == 3 ) {
                     //System.out.println(i+"|"+k+"|"+cuCla.getCanonicalName());
                     if (k!=0) {
                         fail("test_7, case 038 FAILED ");
                     }
                     if (k==0) {
                         if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1)aa[i][k]).author().equals("Serguei Stepanovich")) fail("test_7, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1)aa[i][k]).author());
                     }
                 }
                 if( i == 2 ) {
                     //System.out.println(i+"|"+k+"|"+cuCla.getCanonicalName());
                     if (k!=0) {
                         fail("test_7, case 038 FAILED ");
                     }
                 }
                 if( i == 1 ) {
                     //System.out.println(i+"|"+k+"|"+cuCla.getCanonicalName());
                     if (k==0) {
                         if(!((MA1)aa[i][k]).author().equals("Zapreyev 8")) fail("test_7, case 038 FAILED: "+((MA1)aa[i][k]).author());
                     }
                     if (k==1) {
                         if(!((MA1.MA1_1)aa[i][k]).author().equals("Zapreyev 7")) fail("test_7, case 038 FAILED: "+((MA1.MA1_1)aa[i][k]).author());
                     }
                     if (k==2) {
                         if(!((MA1.MA1_1.MA1_1_1)aa[i][k]).author().equals("Zapreyev 6")) fail("test_7, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1)aa[i][k]).author());
                     }
                     if (k==3) {
                         if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1)aa[i][k]).author().equals("Zapreyev 5")) fail("test_7, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1)aa[i][k]).author());
                     }
                     if (k==4) {
                         if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1)aa[i][k]).author().equals("S&S&Z")) fail("test_7, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1)aa[i][k]).author());
                     }
                 }
                 if( i == 0 ) {
                while (cuCla != null) {
                     //System.out.println(name[j]);
                     if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[j])) fail("test_7, case 005 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
                     if(cuCla.getEnumConstants()!=null) fail("test_7, case 006 FAILED: "+cuCla.getEnumConstants());
                     if(cuCla.isEnum()) fail("test_7, case 007 FAILED: "+cuCla.isEnum());
                     try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassAnnotationsTest")) fail("test_7, case 008 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
                     if(cuCla.getEnclosingMethod()!=null) fail("test_7, case 009 FAILED: "+cuCla.getEnclosingMethod());
                     if(cuCla.getEnclosingConstructor()!=null) fail("test_7, case 010 FAILED: "+cuCla.getEnclosingConstructor());
                     if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_7, case 011 FAILED: "+cuCla.isMemberClass());
                     if(cuCla.isLocalClass()) fail("test_7, case 012 FAILED: "+cuCla.isLocalClass());
                     if(cuCla.isAnonymousClass()) fail("test_7, case 013 FAILED: "+cuCla.isAnonymousClass());
                     if(cuCla.isSynthetic()) fail("test_7, case 014 FAILED: "+cuCla.isSynthetic());
                     if(!cuCla.getCanonicalName().equals(caNa)) fail("test_7, case 015 FAILED: "+cuCla.getCanonicalName());
                     caNa = caNa.substring(0, caNa.lastIndexOf('.'));
                     if(!cuCla.getSimpleName().equals(name[j])) fail("test_7, case 016 FAILED: "+cuCla.getSimpleName());
                     j--;
                     cuCla = cuCla.getEnclosingClass();
                 }
                 try{
                     java.lang.reflect.Method am[] = ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).getClass().getDeclaredMethods();
                     long flg = 0;
                     for (int ii = 0; ii < am.length - 1; ii++){
                         //System.out.println(am[ii].getName());
                         if(am[ii].getName().equals("authorSurname")){
                             flg += 1;
                         } else if(am[ii].getName().equals("authorFullName")){
                             flg += 10;
                         } else if(am[ii].getName().equals("socialClass")){
                             flg += 100;
                         } else if(am[ii].getName().equals("socialClasses")){
                             flg += 1000;
                         } else if(am[ii].getName().equals("primitive")){
                             flg += 10000;
                         } else if(am[ii].getName().equals("blackMarker")){
                             flg += 100000;
                         } else if(am[ii].getName().equals("whiteMarkers")){
                             flg += 1000000;
                         } else if(am[ii].getName().equals("constant")){
                             flg += 10000000;
                         } else if(am[ii].getName().equals("constants")){
                             flg += 100000000;
                         } else if(am[ii].getName().equals("toString")){
                             flg += 1000000000;
                         } else if(am[ii].getName().equals("hashCode")){
                             flg += 10000000000L;
                         } else if(am[ii].getName().equals("equals")){
                             flg += 100000000000L;
                         }
                     }
                     if (flg != 111111111111L) fail("test_7, case 017 FAILED");
                 }catch(Exception _){
                     fail("test_7, case 018 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).getClass().getMethod("primitive").getDefaultValue()!=null) fail("test_7, case 019 FAILED");
                     if(((Integer)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("primitive").getDefaultValue()).intValue()!=777) fail("test_7, case 020 FAILED");
                 }catch(NoSuchMethodException _){
                     fail("test_7, case 021 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).getClass().getMethod("socialClass").getDefaultValue()!=null) fail("test_7, case 022 FAILED");
                     if(!((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName().equals("java.lang.ClassAnnotationsTest$MA1$MA1_1$MA1_1_1$MA1_1_1_1$MA1_1_1_1_1$MA1_1_1_1_1_1$MA1_1_1_1_1_1_1$iiii$Prltr")) fail("test_7, case 023 FAILED: "+((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName());
                 }catch(NoSuchMethodException _){
                     fail("test_7, case 024 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).getClass().getMethod("authorSurname").getDefaultValue()!=null) fail("test_7, case 025 FAILED");
                     if(!((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSurname").getDefaultValue()).equals("Zapreyev")) fail("test_7, case 026 FAILED: "+((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSername").getDefaultValue()));
                 }catch(NoSuchMethodException _){
                     fail("test_7, case 027 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).getClass().getMethod("blackMarker").getDefaultValue()!=null) fail("test_7, case 028 FAILED");
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()).getClass().getSimpleName().equals("MA1_1_1_1_1_1_1")) fail("test_7, case 029 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()));
                 }catch(NoSuchMethodException _){
                     fail("test_7, case 030 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).getClass().getMethod("whiteMarkers").getDefaultValue()!=null) fail("test_7, case 031 FAILED");
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.internalAnnotation[])MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("whiteMarkers").getDefaultValue()).length != 3) fail("test_7, case 032 FAILED");
                 }catch(NoSuchMethodException _){
                     fail("test_7, case 033 FAILED");
                 }
                 if(aa[i][k].annotationType() != ia.annotationType()) fail("test_7, case 034 FAILED: "+aa[i][k].annotationType().toString()+"|"+ia.annotationType().toString());
                 if(aa[i][k].equals((Object) ia)) fail("test_7, case 035 FAILED: "+aa[i][k].equals((Object) ia));
                 if(aa[i][k].hashCode() == ia.hashCode()) fail("test_7, case 036 FAILED: "+Integer.toString(aa[i][k].hashCode())+"|"+Integer.toString(ia.hashCode()));
                 if(aa[i][k].toString().equals(ia.toString())) fail("test_7, case 037 FAILED: "+aa[i][k].toString()+"|"+ia.toString());
                 //System.out.println(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorSurname());
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorSurname().equals("Zapreyev")) fail("test_7, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorSurname());
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorFullName()[0].equals("Zapreyev") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorFullName()[1].equals("Serguei") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorFullName()[2].equals("Stepanovich")) fail("test_7, case 039 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorFullName()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorFullName()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).authorFullName()[2]);
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClass().getSimpleName().equals("Prltr")) fail("test_7, case 040 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClass().getSimpleName());
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[0].getSimpleName().equals("Prltr") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[1].getSimpleName().equals("Brg") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[2].getSimpleName().equals("Krstnn") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[3].getSimpleName().equals("Arstcrt") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[4].getSimpleName().equals("Clrc")) fail("test_7, case 041 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[0].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[1].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[2].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[3].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).socialClasses()[4].getSimpleName());
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).blackMarker().author().equals("AUTHOR")) fail("test_7, case 042 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).blackMarker().author());
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr1()!=true ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr2()!=(byte) 256 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr3()!='Z' ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr4()!=Double.MAX_VALUE ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr5()!=Float.MIN_VALUE ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr6()!=777 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr7()!=Long.MAX_VALUE + Long.MIN_VALUE ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr8()!=32655 ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].itself().author().equals("Zapreyev 4")) fail("test_7, case 043 FAILED: "+Boolean.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr1()) +"|"+
                                 Byte.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr2()) +"|"+
                                 Character.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr3()) +"|"+
                                 Double.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr4()) +"|"+
                                 Float.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr5()) +"|"+
                                 Integer.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr6()) +"|"+
                                 Long.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr7()) +"|"+
                                 Short.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].attr8()) +"|"+
                                 ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).whiteMarkers()[0].itself().author());
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constant()!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.M_S) fail("test_7, case 044 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constant());
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[0]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[1]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[2]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[3]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[4]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5) fail("test_7, case 045 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i][k]).constants()[2]);
                 }
             }
         }
   }

//   FIELD: ////////////////////////////////////////////////////////////////////////////////////////////////

     /**
      * checks starting from Field.getDeclaredAnnotations()
      */
     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii public void test_8() {
         @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(blackMarker = @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author = "UNKNOWN")) class LC111 {
                     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(
                                 blackMarker=@MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author="AUTHOR"),
                                 constants={MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5}
                             )
                     @MA1
                     @MA1.MA1_1
                     @MA1.MA1_1.MA1_1_1
                     @MA1.MA1_1.MA1_1_1.MA1_1_1_1
                     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1(author="S&S&Z")
                     public int fld;
					 int m1(){return fld+m2();};
					 int m2(){return fld+m1();};
         };
         Annotation ia = LC111.class.getDeclaredAnnotations()[0];
         Annotation aa[] = null;
         try{
             java.lang.reflect.Field af = LC111.class.getDeclaredField("fld");
             aa = af.getDeclaredAnnotations();
             if(aa.length!=6) fail("test_8, case 01 FAILED: "+aa.length);
             if(!af.getAnnotation(MA1.MA1_1.MA1_1_1.class).annotationType().equals(MA1.MA1_1.MA1_1_1.class)) fail("test_8, case 02 FAILED: "+aa.length);
             if(af.getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.class)!=null) fail("test_8, case 03 FAILED: "+aa.length);
             try{af.getAnnotation((Class)null); fail("test_8, case 03_1 FAILED: "+aa.length);} catch (NullPointerException e) {}
             if(!af.isAnnotationPresent(MA1.class)) fail("test_8, case 04 FAILED: "+aa.length);
             if(af.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.class)) fail("test_8, case 05 FAILED: "+aa.length);
             try{af.isAnnotationPresent((Class)null); fail("test_8, case 05_1 FAILED: "+aa.length);} catch (NullPointerException e) {}
         } catch (/*NoSuchMethod*/Exception e) {
             fail("test_8, case 005 FAILED: "+e.toString());
         }
         for (int i = 0; i < aa.length; i++) {
                 Class cuCla = aa[i].annotationType();
                 String caNa = cuCla.getCanonicalName();
                 String name[] = caNa.split("\\.");
                 int j = name.length - 1;
                 if( i == 5 ) {
                     if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1)aa[i]).author().equals("S&S&Z")) fail("test_8, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1)aa[i]).author());
                 }
                 if( i == 4 ) {
                     if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1)aa[i]).author().equals("Zapreyev 5")) fail("test_8, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1)aa[i]).author());
                 }
                 if( i == 3 ) {
                     //System.out.println(i+"|"+k+"|"+cuCla.getCanonicalName());
                     if(!((MA1.MA1_1.MA1_1_1)aa[i]).author().equals("Zapreyev 6")) fail("test_8, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1)aa[i]).author());
                 }
                 if( i == 2 ) {
                     //System.out.println(i+"|"+k+"|"+cuCla.getCanonicalName());
                     if(!((MA1.MA1_1)aa[i]).author().equals("Zapreyev 7")) fail("test_8, case 038 FAILED: "+((MA1.MA1_1)aa[i]).author());
                 }
                 if( i == 1 ) {
                     //System.out.println(i+"|"+k+"|"+cuCla.getCanonicalName());
                         if(!((MA1)aa[i]).author().equals("Zapreyev 8")) fail("test_8, case 038 FAILED: "+((MA1)aa[i]).author());
                 }
                 if( i == 0 ) {
                 while (cuCla != null) {
                     //System.out.println(name[j]);
                     if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[j])) fail("test_8, case 005 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
                     if(cuCla.getEnumConstants()!=null) fail("test_8, case 006 FAILED: "+cuCla.getEnumConstants());
                     if(cuCla.isEnum()) fail("test_8, case 007 FAILED: "+cuCla.isEnum());
                     try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassAnnotationsTest")) fail("test_8, case 008 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
                     if(cuCla.getEnclosingMethod()!=null) fail("test_8, case 009 FAILED: "+cuCla.getEnclosingMethod());
                     if(cuCla.getEnclosingConstructor()!=null) fail("test_8, case 010 FAILED: "+cuCla.getEnclosingConstructor());
                     if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_8, case 011 FAILED: "+cuCla.isMemberClass());
                     if(cuCla.isLocalClass()) fail("test_8, case 012 FAILED: "+cuCla.isLocalClass());
                     if(cuCla.isAnonymousClass()) fail("test_8, case 013 FAILED: "+cuCla.isAnonymousClass());
                     if(cuCla.isSynthetic()) fail("test_8, case 014 FAILED: "+cuCla.isSynthetic());
                     if(!cuCla.getCanonicalName().equals(caNa)) fail("test_8, case 015 FAILED: "+cuCla.getCanonicalName());
                     caNa = caNa.substring(0, caNa.lastIndexOf('.'));
                     if(!cuCla.getSimpleName().equals(name[j])) fail("test_8, case 016 FAILED: "+cuCla.getSimpleName());
                     j--;
                     cuCla = cuCla.getEnclosingClass();
                 }
                 try{
                     java.lang.reflect.Method am[] = ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getDeclaredMethods();
                     long flg = 0;
                     for (int ii = 0; ii < am.length - 1; ii++){
                         //System.out.println(am[ii].getName());
                         if(am[ii].getName().equals("authorSurname")){
                             flg += 1;
                         } else if(am[ii].getName().equals("authorFullName")){
                             flg += 10;
                         } else if(am[ii].getName().equals("socialClass")){
                             flg += 100;
                         } else if(am[ii].getName().equals("socialClasses")){
                             flg += 1000;
                         } else if(am[ii].getName().equals("primitive")){
                             flg += 10000;
                         } else if(am[ii].getName().equals("blackMarker")){
                             flg += 100000;
                         } else if(am[ii].getName().equals("whiteMarkers")){
                             flg += 1000000;
                         } else if(am[ii].getName().equals("constant")){
                             flg += 10000000;
                         } else if(am[ii].getName().equals("constants")){
                             flg += 100000000;
                         } else if(am[ii].getName().equals("toString")){
                             flg += 1000000000;
                         } else if(am[ii].getName().equals("hashCode")){
                             flg += 10000000000L;
                         } else if(am[ii].getName().equals("equals")){
                             flg += 100000000000L;
                         }
                     }
                     if (flg != 111111111111L) fail("test_8, case 017 FAILED");
                 }catch(Exception _){
                     fail("test_8, case 018 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("primitive").getDefaultValue()!=null) fail("test_8, case 019 FAILED");
                     if(((Integer)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("primitive").getDefaultValue()).intValue()!=777) fail("test_8, case 020 FAILED");
                 }catch(NoSuchMethodException _){
                     fail("test_8, case 021 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("socialClass").getDefaultValue()!=null) fail("test_8, case 022 FAILED");
                     if(!((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName().equals("java.lang.ClassAnnotationsTest$MA1$MA1_1$MA1_1_1$MA1_1_1_1$MA1_1_1_1_1$MA1_1_1_1_1_1$MA1_1_1_1_1_1_1$iiii$Prltr")) fail("test_8, case 023 FAILED: "+((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName());
                 }catch(NoSuchMethodException _){
                     fail("test_8, case 024 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("authorSurname").getDefaultValue()!=null) fail("test_8, case 025 FAILED");
                     if(!((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSurname").getDefaultValue()).equals("Zapreyev")) fail("test_8, case 026 FAILED: "+((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSername").getDefaultValue()));
                 }catch(NoSuchMethodException _){
                     fail("test_8, case 027 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("blackMarker").getDefaultValue()!=null) fail("test_8, case 028 FAILED");
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()).getClass().getSimpleName().equals("MA1_1_1_1_1_1_1")) fail("test_8, case 029 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()));
                 }catch(NoSuchMethodException _){
                     fail("test_8, case 030 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("whiteMarkers").getDefaultValue()!=null) fail("test_8, case 031 FAILED");
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.internalAnnotation[])MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("whiteMarkers").getDefaultValue()).length != 3) fail("test_8, case 032 FAILED");
                 }catch(NoSuchMethodException _){
                     fail("test_8, case 033 FAILED");
                 }
                 if(aa[i].annotationType() != ia.annotationType()) fail("test_8, case 034 FAILED: "+aa[i].annotationType().toString()+"|"+ia.annotationType().toString());
                 if(aa[i].equals((Object) ia)) fail("test_8, case 035 FAILED: "+aa[i].equals((Object) ia));
                 if(aa[i].hashCode() == ia.hashCode()) fail("test_8, case 036 FAILED: "+Integer.toString(aa[i].hashCode())+"|"+Integer.toString(ia.hashCode()));
                 if(aa[i].toString().equals(ia.toString())) fail("test_8, case 037 FAILED: "+aa[i].toString()+"|"+ia.toString());
                 //System.out.println(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname().equals("Zapreyev")) fail("test_8, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0].equals("Zapreyev") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1].equals("Serguei") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2].equals("Stepanovich")) fail("test_8, case 039 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2]);
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName().equals("Prltr")) fail("test_8, case 040 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName());
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName().equals("Prltr") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName().equals("Brg") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName().equals("Krstnn") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName().equals("Arstcrt") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName().equals("Clrc")) fail("test_8, case 041 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName());
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author().equals("AUTHOR")) fail("test_8, case 042 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author());
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()!=true ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()!=(byte) 256 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()!='Z' ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()!=Double.MAX_VALUE ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()!=Float.MIN_VALUE ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()!=777 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()!=Long.MAX_VALUE + Long.MIN_VALUE ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()!=32655 ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author().equals("Zapreyev 4")) fail("test_8, case 043 FAILED: "+Boolean.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()) +"|"+
                                 Byte.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()) +"|"+
                                 Character.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()) +"|"+
                                 Double.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()) +"|"+
                                 Float.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()) +"|"+
                                 Integer.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()) +"|"+
                                 Long.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()) +"|"+
                                 Short.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()) +"|"+
                                 ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author());
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant()!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.M_S) fail("test_8, case 044 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant());
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[3]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[4]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5) fail("test_8, case 045 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]);
                 }
         }
   }

     /**
      * checks starting from Field.getAnnotations()
      */
     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii public void test_9() {
         @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(blackMarker = @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author = "UNKNOWN")) class LC111 {
                     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(
                                 blackMarker=@MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author="AUTHOR"),
                                 constants={MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5, MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5}
                             )
                     @MA1
                     @MA1.MA1_1
                     @MA1.MA1_1.MA1_1_1
                     @MA1.MA1_1.MA1_1_1.MA1_1_1_1
                     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1(author="S&S&Z")
                     public int fld;
					 int m1(){return fld+m2();};
					 int m2(){return fld+m1();};
         };
         Annotation ia = LC111.class.getAnnotations()[0];
         Annotation aa[] = null;
         try{
             java.lang.reflect.Field af = LC111.class.getDeclaredField("fld");
             aa = af.getDeclaredAnnotations();
             if(aa.length!=6) fail("test_9, case 0 FAILED: "+aa.length);
             if(!af.getAnnotation(MA1.MA1_1.MA1_1_1.class).annotationType().equals(MA1.MA1_1.MA1_1_1.class)) fail("test_9, case 02 FAILED: "+aa.length);
             if(af.getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.class)!=null) fail("test_9, case 03 FAILED: "+aa.length);
             try{af.getAnnotation((Class)null); fail("test_9, case 03_1 FAILED: "+aa.length);} catch (NullPointerException e) {}
             if(!af.isAnnotationPresent(MA1.class)) fail("test_9, case 04 FAILED: "+aa.length);
             if(af.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.class)) fail("test_9, case 05 FAILED: "+aa.length);
             try{af.isAnnotationPresent((Class)null); fail("test_9, case 05_1 FAILED: "+aa.length);} catch (NullPointerException e) {}
         } catch (/*NoSuchMethod*/Exception e) {
             fail("test_9, case 005 FAILED: "+e.toString());
         }
         for (int i = 0; i < aa.length; i++) {
                 Class cuCla = aa[i].annotationType();
                 String caNa = cuCla.getCanonicalName();
                 String name[] = caNa.split("\\.");
                 int j = name.length - 1;
                 if( i == 5 ) {
                     if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1)aa[i]).author().equals("S&S&Z")) fail("test_9, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1)aa[i]).author());
                 }
                 if( i == 4 ) {
                     if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1)aa[i]).author().equals("Zapreyev 5")) fail("test_9, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1)aa[i]).author());
                 }
                 if( i == 3 ) {
                     //System.out.println(i+"|"+k+"|"+cuCla.getCanonicalName());
                     if(!((MA1.MA1_1.MA1_1_1)aa[i]).author().equals("Zapreyev 6")) fail("test_9, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1)aa[i]).author());
                 }
                 if( i == 2 ) {
                     //System.out.println(i+"|"+k+"|"+cuCla.getCanonicalName());
                     if(!((MA1.MA1_1)aa[i]).author().equals("Zapreyev 7")) fail("test_9, case 038 FAILED: "+((MA1.MA1_1)aa[i]).author());
                 }
                 if( i == 1 ) {
                     //System.out.println(i+"|"+k+"|"+cuCla.getCanonicalName());
                         if(!((MA1)aa[i]).author().equals("Zapreyev 8")) fail("test_9, case 038 FAILED: "+((MA1)aa[i]).author());
                 }
                 if( i == 0 ) {
                 while (cuCla != null) {
                     //System.out.println(name[j]);
                     if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[j])) fail("test_9, case 005 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
                     if(cuCla.getEnumConstants()!=null) fail("test_9, case 006 FAILED: "+cuCla.getEnumConstants());
                     if(cuCla.isEnum()) fail("test_9, case 007 FAILED: "+cuCla.isEnum());
                     try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassAnnotationsTest")) fail("test_9, case 008 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
                     if(cuCla.getEnclosingMethod()!=null) fail("test_9, case 009 FAILED: "+cuCla.getEnclosingMethod());
                     if(cuCla.getEnclosingConstructor()!=null) fail("test_9, case 010 FAILED: "+cuCla.getEnclosingConstructor());
                     if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_9, case 011 FAILED: "+cuCla.isMemberClass());
                     if(cuCla.isLocalClass()) fail("test_9, case 012 FAILED: "+cuCla.isLocalClass());
                     if(cuCla.isAnonymousClass()) fail("test_9, case 013 FAILED: "+cuCla.isAnonymousClass());
                     if(cuCla.isSynthetic()) fail("test_9, case 014 FAILED: "+cuCla.isSynthetic());
                     if(!cuCla.getCanonicalName().equals(caNa)) fail("test_9, case 015 FAILED: "+cuCla.getCanonicalName());
                     caNa = caNa.substring(0, caNa.lastIndexOf('.'));
                     if(!cuCla.getSimpleName().equals(name[j])) fail("test_9, case 016 FAILED: "+cuCla.getSimpleName());
                     j--;
                     cuCla = cuCla.getEnclosingClass();
                 }
                 try{
                     java.lang.reflect.Method am[] = ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getDeclaredMethods();
                     long flg = 0;
                     for (int ii = 0; ii < am.length - 1; ii++){
                         //System.out.println(am[ii].getName());
                         if(am[ii].getName().equals("authorSurname")){
                             flg += 1;
                         } else if(am[ii].getName().equals("authorFullName")){
                             flg += 10;
                         } else if(am[ii].getName().equals("socialClass")){
                             flg += 100;
                         } else if(am[ii].getName().equals("socialClasses")){
                             flg += 1000;
                         } else if(am[ii].getName().equals("primitive")){
                             flg += 10000;
                         } else if(am[ii].getName().equals("blackMarker")){
                             flg += 100000;
                         } else if(am[ii].getName().equals("whiteMarkers")){
                             flg += 1000000;
                         } else if(am[ii].getName().equals("constant")){
                             flg += 10000000;
                         } else if(am[ii].getName().equals("constants")){
                             flg += 100000000;
                         } else if(am[ii].getName().equals("toString")){
                             flg += 1000000000;
                         } else if(am[ii].getName().equals("hashCode")){
                             flg += 10000000000L;
                         } else if(am[ii].getName().equals("equals")){
                             flg += 100000000000L;
                         }
                     }
                     if (flg != 111111111111L) fail("test_9, case 017 FAILED");
                 }catch(Exception _){
                     fail("test_9, case 018 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("primitive").getDefaultValue()!=null) fail("test_9, case 019 FAILED");
                     if(((Integer)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("primitive").getDefaultValue()).intValue()!=777) fail("test_9, case 020 FAILED");
                 }catch(NoSuchMethodException _){
                     fail("test_9, case 021 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("socialClass").getDefaultValue()!=null) fail("test_9, case 022 FAILED");
                     if(!((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName().equals("java.lang.ClassAnnotationsTest$MA1$MA1_1$MA1_1_1$MA1_1_1_1$MA1_1_1_1_1$MA1_1_1_1_1_1$MA1_1_1_1_1_1_1$iiii$Prltr")) fail("test_9, case 023 FAILED: "+((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName());
                 }catch(NoSuchMethodException _){
                     fail("test_9, case 024 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("authorSurname").getDefaultValue()!=null) fail("test_9, case 025 FAILED");
                     if(!((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSurname").getDefaultValue()).equals("Zapreyev")) fail("test_9, case 026 FAILED: "+((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSername").getDefaultValue()));
                 }catch(NoSuchMethodException _){
                     fail("test_9, case 027 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("blackMarker").getDefaultValue()!=null) fail("test_9, case 028 FAILED");
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()).getClass().getSimpleName().equals("MA1_1_1_1_1_1_1")) fail("test_9, case 029 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()));
                 }catch(NoSuchMethodException _){
                     fail("test_9, case 030 FAILED");
                 }
                 try{
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("whiteMarkers").getDefaultValue()!=null) fail("test_9, case 031 FAILED");
                     if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.internalAnnotation[])MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("whiteMarkers").getDefaultValue()).length != 3) fail("test_9, case 032 FAILED");
                 }catch(NoSuchMethodException _){
                     fail("test_9, case 033 FAILED");
                 }
                 if(aa[i].annotationType() != ia.annotationType()) fail("test_9, case 034 FAILED: "+aa[i].annotationType().toString()+"|"+ia.annotationType().toString());
                 if(aa[i].equals((Object) ia)) fail("test_9, case 035 FAILED: "+aa[i].equals((Object) ia));
                 if(aa[i].hashCode() == ia.hashCode()) fail("test_9, case 036 FAILED: "+Integer.toString(aa[i].hashCode())+"|"+Integer.toString(ia.hashCode()));
                 if(aa[i].toString().equals(ia.toString())) fail("test_9, case 037 FAILED: "+aa[i].toString()+"|"+ia.toString());
                 //System.out.println(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname().equals("Zapreyev")) fail("test_9, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0].equals("Zapreyev") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1].equals("Serguei") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2].equals("Stepanovich")) fail("test_9, case 039 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2]);
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName().equals("Prltr")) fail("test_9, case 040 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName());
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName().equals("Prltr") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName().equals("Brg") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName().equals("Krstnn") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName().equals("Arstcrt") ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName().equals("Clrc")) fail("test_9, case 041 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName());
                 if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author().equals("AUTHOR")) fail("test_9, case 042 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author());
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()!=true ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()!=(byte) 256 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()!='Z' ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()!=Double.MAX_VALUE ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()!=Float.MIN_VALUE ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()!=777 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()!=Long.MAX_VALUE + Long.MIN_VALUE ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()!=32655 ||
                         !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author().equals("Zapreyev 4")) fail("test_9, case 043 FAILED: "+Boolean.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()) +"|"+
                                 Byte.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()) +"|"+
                                 Character.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()) +"|"+
                                 Double.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()) +"|"+
                                 Float.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()) +"|"+
                                 Integer.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()) +"|"+
                                 Long.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()) +"|"+
                                 Short.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()) +"|"+
                                 ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author());
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant()!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.M_S) fail("test_9, case 044 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant());
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[3]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5 ||
                         ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[4]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5) fail("test_9, case 045 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]);
                 }
         }
   }

//   CLASS: ////////////////////////////////////////////////////////////////////////////////////////////////

     /**
      * checks starting from Class.getDeclaredAnnotations()
      */
     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(primitive=777) public void test_10() {
         @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(blackMarker = @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author = "UNKNOWN")) @MA1 @MA1.MA1_1 @MA1.MA1_1.MA1_1_1 class LC111 {
             public LC111(){};
         };
         Annotation ia = null; //LC111.class.getDeclaredAnnotations()[0];
         try {
             ia = ClassAnnotationsTest.class.getMethod("test_10").getAnnotations()[0];
         } catch (NoSuchMethodException e) {
             fail("test_10, case 000 FAILED: "+e.toString());
         }
         Annotation aa[] = null;
         if(!LC111.class.getAnnotation(MA1.MA1_1.MA1_1_1.class).annotationType().equals(MA1.MA1_1.MA1_1_1.class)) fail("test_10, case 02 FAILED: "+aa.length);
         if(LC111.class.getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.class)!=null) fail("test_10, case 03 FAILED: "+aa.length);
         try{LC111.class.getAnnotation((Class)null); fail("test_10, case 03_1 FAILED: "+aa.length);} catch (NullPointerException e) {}
         if(!LC111.class.isAnnotationPresent(MA1.class)) fail("test_10, case 04 FAILED: "+aa.length);
         if(LC111.class.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.class)) fail("test_10, case 05 FAILED: "+aa.length);
         try{LC111.class.isAnnotationPresent((Class)null); fail("test_10, case 05_1 FAILED: "+aa.length);} catch (NullPointerException e) {}
         aa = LC111.class.getDeclaredAnnotations();
         if(aa.length!=4) fail("test_6, case 0 FAILED: "+aa.length);
         for (int i = 0; i < aa.length; i++) {
             Class cuCla = aa[i].annotationType();
             String caNa = cuCla.getCanonicalName();
             String name[] = caNa.split("\\.");
             int j = name.length - 1;
             //if (i == 1) {
             if (cuCla.getSimpleName().equals("MA1")) {
                 try{
                     if(((MA1)aa[i]).getClass().getMethod("author").getDefaultValue()!=null) fail("test_10, case 025 FAILED");
                     if(!((String)MA1.class.getMethod("author").getDefaultValue()).equals("Zapreyev 8")) fail("test_10, case 026 FAILED: "+((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSername").getDefaultValue()));
                 }catch(NoSuchMethodException _){
                     fail("test_10, case 027 FAILED");
                 }
             }
             //if (i == 0) {
             if (cuCla.getSimpleName().equals("iiii")) {
                 try{
                     if(aa[i].toString().replaceAll("Enum\\:","").replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\(", "").
                             replaceFirst("blackMarker=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\(author=UNKNOWN\\)","").
                             replaceFirst("constants=\\[W_T5, V_T5, G_A_T5\\]","").
                             replaceFirst("authorSurname=Zapreyev","").
                             replaceFirst("socialClass=class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Prltr","").
                             replaceFirst("primitive=777","").
                             replaceFirst("socialClasses=\\[","").
                             replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Prltr","").
                             replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Brg","").
                             replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Krstnn","").
                             replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Arstcrt","").
                             replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Clrc","").
                             replaceFirst("whiteMarkers=\\[","").
                             replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                             replaceFirst("attr7=-1","").
                             replaceFirst("attr1=true","").
                             replaceFirst("attr2=0","").
                             replaceFirst("attr5=1\\.4E-45","").
                             replaceFirst("attr6=777","").
                             replaceFirst("attr3=Z","").
                             replaceFirst("attr4=1\\.7976931348623157E308","").
                             replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                             replaceFirst("attr8=32655","").
                             replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                             replaceFirst("attr7=-1","").
                             replaceFirst("attr1=true","").
                             replaceFirst("attr2=0","").
                             replaceFirst("attr5=1\\.4E-45","").
                             replaceFirst("attr6=777","").
                             replaceFirst("attr3=Z","").
                             replaceFirst("attr4=1\\.7976931348623157E308","").
                             replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                             replaceFirst("attr8=32655","").
                             replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                             replaceFirst("attr7=-1","").
                             replaceFirst("attr1=true","").
                             replaceFirst("attr2=0","").
                             replaceFirst("attr5=1\\.4E-45","").
                             replaceFirst("attr6=777","").
                             replaceFirst("attr3=Z","").
                             replaceFirst("attr4=1\\.7976931348623157E308","").
                             replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                             replaceFirst("attr8=32655","").
                             replaceFirst("authorFullName=\\[Zapreyev, Serguei, Stepanovich\\]","").
                             replaceFirst("constant=M_S","").
                             replaceFirst("attr1=false","").
                             replaceFirst("attr1=false","").
                             replaceAll(" ","").
                             replaceAll("\\)","").
                             replaceAll("\\]","").
                             replaceAll("\\,","").length()!=0) fail("test_10, case 1 FAILED: "+aa[0].toString());
         /**/            if(!ClassAnnotationsTest.class.getMethod("test_10").isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class)) fail("test_10, case 002 FAILED: "+MC003.class.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class));
         /**/            if(ClassAnnotationsTest.class.getMethod("test_10").getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class) == null) fail("test_10, case 003 FAILED: "+MC003.class.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class));
         /**/            if(ClassAnnotationsTest.class.getMethod("test_10").getParameterAnnotations().length!=0) fail("test_10, case 004 FAILED: "+ClassAnnotationsTest.class.getMethod("test_10").getParameterAnnotations().length);
                 } catch (NoSuchMethodException e) {
                     fail("test_10, case 005 FAILED: "+e.toString());
                 }
             while (cuCla != null) {
                 //System.out.println(name[j]);
                 if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[j])) fail("test_10, case 005 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
                 if(cuCla.getEnumConstants()!=null) fail("test_10, case 006 FAILED: "+cuCla.getEnumConstants());
                 if(cuCla.isEnum()) fail("test_10, case 007 FAILED: "+cuCla.isEnum());
                 try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassAnnotationsTest")) fail("test_10, case 008 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
                 if(cuCla.getEnclosingMethod()!=null) fail("test_10, case 009 FAILED: "+cuCla.getEnclosingMethod());
                 if(cuCla.getEnclosingConstructor()!=null) fail("test_10, case 010 FAILED: "+cuCla.getEnclosingConstructor());
                 if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_10, case 011 FAILED: "+cuCla.isMemberClass());
                 if(cuCla.isLocalClass()) fail("test_10, case 012 FAILED: "+cuCla.isLocalClass());
                 if(cuCla.isAnonymousClass()) fail("test_10, case 013 FAILED: "+cuCla.isAnonymousClass());
                 if(cuCla.isSynthetic()) fail("test_10, case 014 FAILED: "+cuCla.isSynthetic());
                 if(!cuCla.getCanonicalName().equals(caNa)) fail("test_10, case 015 FAILED: "+cuCla.getCanonicalName());
                 caNa = caNa.substring(0, caNa.lastIndexOf('.'));
                 if(!cuCla.getSimpleName().equals(name[j])) fail("test_10, case 016 FAILED: "+cuCla.getSimpleName());
                 j--;
                 cuCla = cuCla.getEnclosingClass();
             }
             try{
                 java.lang.reflect.Method am[] = ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getDeclaredMethods();
                 long flg = 0;
                 for (int ii = 0; ii < am.length - 1; ii++){
                     //System.out.println(am[ii].getName());
                     if(am[ii].getName().equals("authorSurname")){
                         flg += 1;
                     } else if(am[ii].getName().equals("authorFullName")){
                         flg += 10;
                     } else if(am[ii].getName().equals("socialClass")){
                         flg += 100;
                     } else if(am[ii].getName().equals("socialClasses")){
                         flg += 1000;
                     } else if(am[ii].getName().equals("primitive")){
                         flg += 10000;
                     } else if(am[ii].getName().equals("blackMarker")){
                         flg += 100000;
                     } else if(am[ii].getName().equals("whiteMarkers")){
                         flg += 1000000;
                     } else if(am[ii].getName().equals("constant")){
                         flg += 10000000;
                     } else if(am[ii].getName().equals("constants")){
                         flg += 100000000;
                     } else if(am[ii].getName().equals("toString")){
                         flg += 1000000000;
                     } else if(am[ii].getName().equals("hashCode")){
                         flg += 10000000000L;
                     } else if(am[ii].getName().equals("equals")){
                         flg += 100000000000L;
                     }
                 }
                 if (flg != 111111111111L) fail("test_10, case 017 FAILED");
             }catch(Exception _){
                 fail("test_10, case 018 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("primitive").getDefaultValue()!=null) fail("test_10, case 019 FAILED");
                 if(((Integer)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("primitive").getDefaultValue()).intValue()!=777) fail("test_10, case 020 FAILED");
             }catch(NoSuchMethodException _){
                 fail("test_10, case 021 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("socialClass").getDefaultValue()!=null) fail("test_10, case 022 FAILED");
                 if(!((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName().equals("java.lang.ClassAnnotationsTest$MA1$MA1_1$MA1_1_1$MA1_1_1_1$MA1_1_1_1_1$MA1_1_1_1_1_1$MA1_1_1_1_1_1_1$iiii$Prltr")) fail("test_10, case 023 FAILED: "+((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName());
             }catch(NoSuchMethodException _){
                 fail("test_10, case 024 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("authorSurname").getDefaultValue()!=null) fail("test_10, case 025 FAILED");
                 if(!((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSurname").getDefaultValue()).equals("Zapreyev")) fail("test_10, case 026 FAILED: "+((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSername").getDefaultValue()));
             }catch(NoSuchMethodException _){
                 fail("test_10, case 027 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("blackMarker").getDefaultValue()!=null) fail("test_10, case 028 FAILED");
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()).getClass().getSimpleName().equals("MA1_1_1_1_1_1_1")) fail("test_10, case 029 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()));
             }catch(NoSuchMethodException _){
                 fail("test_10, case 030 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("whiteMarkers").getDefaultValue()!=null) fail("test_10, case 031 FAILED");
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.internalAnnotation[])MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("whiteMarkers").getDefaultValue()).length != 3) fail("test_10, case 032 FAILED");
             }catch(NoSuchMethodException _){
                 fail("test_10, case 033 FAILED");
             }
             if(aa[i].annotationType() != ia.annotationType()) fail("test_10, case 034 FAILED: "+aa[i].annotationType().toString()+"|"+ia.annotationType().toString());
             if(!aa[i].annotationType().getSimpleName().equals(ia.annotationType().getSimpleName())) fail("test_10, case 035_1 FAILED: "+aa[i].annotationType().getSimpleName().equals(ia.annotationType().getSimpleName()));
             if(aa[i].equals((Object) ia)) fail("test_10, case 035 FAILED: "+aa[i].equals((Object) ia));
             if(aa[i].hashCode() == ia.hashCode()) fail("test_10, case 036 FAILED: "+Integer.toString(aa[i].hashCode())+"|"+Integer.toString(ia.hashCode()));
             if(aa[i].toString().equals(ia.toString())) fail("test_10, case 037 FAILED: "+aa[i].toString()+"|"+ia.toString());
             //System.out.println(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname().equals("Zapreyev")) fail("test_10, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0].equals("Zapreyev") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1].equals("Serguei") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2].equals("Stepanovich")) fail("test_10, case 039 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2]);
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName().equals("Prltr")) fail("test_10, case 040 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName().equals("Prltr") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName().equals("Brg") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName().equals("Krstnn") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName().equals("Arstcrt") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName().equals("Clrc")) fail("test_10, case 041 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author().equals("UNKNOWN")) fail("test_10, case 042 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author());
             if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()!=true ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()!=(byte) 256 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()!='Z' ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()!=Double.MAX_VALUE ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()!=Float.MIN_VALUE ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()!=777 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()!=Long.MAX_VALUE + Long.MIN_VALUE ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()!=32655 ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author().equals("Zapreyev 4")) fail("test_10, case 043 FAILED: "+Boolean.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()) +"|"+
                             Byte.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()) +"|"+
                             Character.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()) +"|"+
                             Double.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()) +"|"+
                             Float.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()) +"|"+
                             Integer.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()) +"|"+
                             Long.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()) +"|"+
                             Short.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()) +"|"+
                             ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author());
             if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant()!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.M_S) fail("test_10, case 044 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant());
             if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.V_T5 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5) fail("test_10, case 045 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]);
             }
         }
   }

     /**
      * checks starting from Class.getAnnotations()
      */
     @Retention(value=RetentionPolicy.RUNTIME)
     @interface ia1{
         abstract String author() default "Zapreyev";
         @Retention(value=RetentionPolicy.RUNTIME)
         @interface ia2{
             abstract String author() default "Zapreyev";
         };
     };
     @Retention(value=RetentionPolicy.RUNTIME)
     @interface ca1{
         abstract String author1() default "Zapreyev1";
         @Retention(value=RetentionPolicy.RUNTIME)
         @interface ca2{
             abstract String author2() default "Zapreyev2";
             @Retention(value=RetentionPolicy.CLASS)
             @interface ca3{
                 abstract String author3() default "Zapreyev3";
                 @Retention(value=RetentionPolicy.SOURCE)
                 @interface ca4{
                     abstract String author4() default "Zapreyev4";
                     @Retention(value=RetentionPolicy.RUNTIME)
                     //@interface ca5<T extends Class>{ //...\tiger-dev\vm\tests\kernel\java\lang\ClassAnnotationsTest.java:2142: @interface may not have type parameters
                     @interface ca5{
                         @ca4 @ca3 @ca2 @ca1 abstract String author51() default "Zapreyev51";
                         //abstract T author52(); //...tiger-dev\vm\tests\kernel\java\lang\ClassAnnotationsTest.java:2144: invalid type for annotation member
                     };
                 };
             };
         };
     };
     @ia1 @ia1.ia2 interface LI111 {
     };
     @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii public void test_11() {
         @ca1 @ca1.ca2 @ca1.ca2.ca3 @ca1.ca2.ca3.ca4 @ca1.ca2.ca3.ca4.ca5 class LC222 implements ia1 {
             public String author() {return "Hello, it's me!";}
             public Class annotationType() {return (Class)null;}
             public int hasCode() {return 777;}
             public boolean equals() {return false;}
             public void invokeAll() {if (this.hashCode()==777) this.equals(); this.annotationType(); this.author(); this.invokeInvokeAll();}
             public void invokeInvokeAll() {this.invokeAll();}
         };
         
         Annotation an1 = LC222.class.getAnnotation(ca1.ca2.ca3.ca4.ca5.class);
         if(!an1.annotationType().equals(ca1.ca2.ca3.ca4.ca5.class)) fail("test_11, case 02 FAILED: "+an1.annotationType());
         if(!LC222.class.getAnnotation(ca1.ca2.ca3.ca4.ca5.class).annotationType().equals(ca1.ca2.ca3.ca4.ca5.class)) fail("test_11, case 02 FAILED: "+an1.annotationType());
         try{
             if(((ca1.ca2.ca3.ca4.ca5)an1).getClass().getMethod("author51").getDefaultValue()!=null) fail("test_11, case 019 FAILED");
             if(!((String)ca1.ca2.ca3.ca4.ca5.class.getMethod("author51").getDefaultValue()).equals("Zapreyev51")) fail("test_11, case 020 FAILED");
         }catch(NoSuchMethodException _){
             fail("test_11, case 021 FAILED");
         }

         
         @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii(blackMarker = @MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1(author = "UNKNOWN")) @MA1 @MA1.MA1_1 @MA1.MA1_1.MA1_1_1 class LC111 extends LC222 implements LI111 {
             public LC111(){};
         };
         Annotation ia = null; //LC111.class.getDeclaredAnnotations()[0];
         try {
             ia = ClassAnnotationsTest.class.getMethod("test_11").getAnnotations()[0];
         } catch (NoSuchMethodException e) {
             fail("test_11, case 000 FAILED: "+e.toString());
         }
         Annotation aa[] = null;
         if(!LC111.class.getAnnotation(MA1.MA1_1.MA1_1_1.class).annotationType().equals(MA1.MA1_1.MA1_1_1.class)) fail("test_11, case 02 FAILED: "+aa.length);
         if(LC111.class.getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.class)!=null) fail("test_11, case 03 FAILED: "+aa.length);
         try{LC111.class.getAnnotation((Class)null); fail("test_11, case 03_1 FAILED: "+aa.length);} catch (NullPointerException e) {}
         if(!LC111.class.isAnnotationPresent(MA1.class)) fail("test_11, case 04 FAILED: "+aa.length);
         if(LC111.class.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.class)) fail("test_11, case 05 FAILED: "+aa.length);
         try{LC111.class.isAnnotationPresent((Class)null); fail("test_11, case 05_1 FAILED: "+aa.length);} catch (NullPointerException e) {}
         aa = LC111.class.getAnnotations();
         if(aa.length!=4) fail("test_6, case 0 FAILED: "+aa.length);
         for (int i = 0; i < aa.length; i++) {
             Class cuCla = aa[i].annotationType();
             String caNa = cuCla.getCanonicalName();
             String name[] = caNa.split("\\.");
             int j = name.length - 1;
             //if (i == 1) {
             if (cuCla.getSimpleName().equals("MA1")) {
                 try{
                     if(((MA1)aa[i]).getClass().getMethod("author").getDefaultValue()!=null) fail("test_11, case 025 FAILED");
                     if(!((String)MA1.class.getMethod("author").getDefaultValue()).equals("Zapreyev 8")) fail("test_11, case 026 FAILED: "+((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSername").getDefaultValue()));
                 }catch(NoSuchMethodException _){
                     fail("test_11, case 027 FAILED");
                 }
             }
             //if (i == 0) {
             if (cuCla.getSimpleName().equals("iiii")) {
                 try{
                     if(aa[i].toString().replaceAll("Enum\\:","").replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\(", "").
                             replaceFirst("blackMarker=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\(author=UNKNOWN\\)","").
                             replaceFirst("constants=\\[W_T5, V_T5, G_A_T5\\]","").
                             replaceFirst("authorSurname=Zapreyev","").
                             replaceFirst("socialClass=class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Prltr","").
                             replaceFirst("primitive=777","").
                             replaceFirst("socialClasses=\\[","").
                             replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Prltr","").
                             replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Brg","").
                             replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Krstnn","").
                             replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Arstcrt","").
                             replaceFirst("class java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$Clrc","").
                             replaceFirst("whiteMarkers=\\[","").
                             replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                             replaceFirst("attr7=-1","").
                             replaceFirst("attr1=true","").
                             replaceFirst("attr2=0","").
                             replaceFirst("attr5=1\\.4E-45","").
                             replaceFirst("attr6=777","").
                             replaceFirst("attr3=Z","").
                             replaceFirst("attr4=1\\.7976931348623157E308","").
                             replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                             replaceFirst("attr8=32655","").
                             replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                             replaceFirst("attr7=-1","").
                             replaceFirst("attr1=true","").
                             replaceFirst("attr2=0","").
                             replaceFirst("attr5=1\\.4E-45","").
                             replaceFirst("attr6=777","").
                             replaceFirst("attr3=Z","").
                             replaceFirst("attr4=1\\.7976931348623157E308","").
                             replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                             replaceFirst("attr8=32655","").
                             replaceFirst("\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\$MA1_1_1_1_1_1\\$MA1_1_1_1_1_1_1\\$iiii\\$internalAnnotation\\(","").
                             replaceFirst("attr7=-1","").
                             replaceFirst("attr1=true","").
                             replaceFirst("attr2=0","").
                             replaceFirst("attr5=1\\.4E-45","").
                             replaceFirst("attr6=777","").
                             replaceFirst("attr3=Z","").
                             replaceFirst("attr4=1\\.7976931348623157E308","").
                             replaceFirst("itself=\\@java\\.lang\\.ClassAnnotationsTest\\$MA1\\$MA1_1\\$MA1_1_1\\$MA1_1_1_1\\$MA1_1_1_1_1\\(author=Zapreyev 4\\)","").
                             replaceFirst("attr8=32655","").
                             replaceFirst("authorFullName=\\[Zapreyev, Serguei, Stepanovich\\]","").
                             replaceFirst("constant=M_S","").
                             replaceFirst("attr1=false","").
                             replaceFirst("attr1=false","").
                             replaceAll(" ","").
                             replaceAll("\\)","").
                             replaceAll("\\]","").
                             replaceAll("\\,","").length()!=0) fail("test_11, case 1 FAILED: "+aa[0].toString());
         /**/            if(!ClassAnnotationsTest.class.getMethod("test_11").isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class)) fail("test_11, case 002 FAILED: "+MC003.class.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class));
         /**/            if(ClassAnnotationsTest.class.getMethod("test_11").getAnnotation(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class) == null) fail("test_11, case 003 FAILED: "+MC003.class.isAnnotationPresent(MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class));
         /**/            if(ClassAnnotationsTest.class.getMethod("test_11").getParameterAnnotations().length!=0) fail("test_11, case 004 FAILED: "+ClassAnnotationsTest.class.getMethod("test_11").getParameterAnnotations().length);
                 } catch (NoSuchMethodException e) {
                     fail("test_11, case 005 FAILED: "+e.toString());
                 }
             while (cuCla != null) {
                 //System.out.println(name[j]);
                 if(cuCla.getEnclosingClass() != null && cuCla.getEnclosingClass().getSimpleName().equals(name[j])) fail("test_11, case 005 FAILED: "+cuCla.getEnclosingClass().getSimpleName());
                 if(cuCla.getEnumConstants()!=null) fail("test_11, case 006 FAILED: "+cuCla.getEnumConstants());
                 if(cuCla.isEnum()) fail("test_11, case 007 FAILED: "+cuCla.isEnum());
                 try{cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ); if(!cuCla.getCanonicalName().equals("java.lang.ClassAnnotationsTest")) fail("test_11, case 008 FAILED: "+ cuCla.asSubclass( cuCla.getEnclosingClass() != null ? cuCla.getEnclosingClass() : cuCla ));}catch(Exception e){/*e.printStackTrace();*/}
                 if(cuCla.getEnclosingMethod()!=null) fail("test_11, case 009 FAILED: "+cuCla.getEnclosingMethod());
                 if(cuCla.getEnclosingConstructor()!=null) fail("test_11, case 010 FAILED: "+cuCla.getEnclosingConstructor());
                 if(cuCla.getEnclosingClass() != null && !cuCla.isMemberClass()) fail("test_11, case 011 FAILED: "+cuCla.isMemberClass());
                 if(cuCla.isLocalClass()) fail("test_11, case 012 FAILED: "+cuCla.isLocalClass());
                 if(cuCla.isAnonymousClass()) fail("test_11, case 013 FAILED: "+cuCla.isAnonymousClass());
                 if(cuCla.isSynthetic()) fail("test_11, case 014 FAILED: "+cuCla.isSynthetic());
                 if(!cuCla.getCanonicalName().equals(caNa)) fail("test_11, case 015 FAILED: "+cuCla.getCanonicalName());
                 caNa = caNa.substring(0, caNa.lastIndexOf('.'));
                 if(!cuCla.getSimpleName().equals(name[j])) fail("test_11, case 016 FAILED: "+cuCla.getSimpleName());
                 j--;
                 cuCla = cuCla.getEnclosingClass();
             }
             try{
                 java.lang.reflect.Method am[] = ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getDeclaredMethods();
                 long flg = 0;
                 for (int ii = 0; ii < am.length - 1; ii++){
                     //System.out.println(am[ii].getName());
                     if(am[ii].getName().equals("authorSurname")){
                         flg += 1;
                     } else if(am[ii].getName().equals("authorFullName")){
                         flg += 10;
                     } else if(am[ii].getName().equals("socialClass")){
                         flg += 100;
                     } else if(am[ii].getName().equals("socialClasses")){
                         flg += 1000;
                     } else if(am[ii].getName().equals("primitive")){
                         flg += 10000;
                     } else if(am[ii].getName().equals("blackMarker")){
                         flg += 100000;
                     } else if(am[ii].getName().equals("whiteMarkers")){
                         flg += 1000000;
                     } else if(am[ii].getName().equals("constant")){
                         flg += 10000000;
                     } else if(am[ii].getName().equals("constants")){
                         flg += 100000000;
                     } else if(am[ii].getName().equals("toString")){
                         flg += 1000000000;
                     } else if(am[ii].getName().equals("hashCode")){
                         flg += 10000000000L;
                     } else if(am[ii].getName().equals("equals")){
                         flg += 100000000000L;
                     }
                 }
                 if (flg != 111111111111L) fail("test_11, case 017 FAILED");
             }catch(Exception _){
                 fail("test_11, case 018 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("primitive").getDefaultValue()!=null) fail("test_11, case 019 FAILED");
                 if(((Integer)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("primitive").getDefaultValue()).intValue()!=777) fail("test_11, case 020 FAILED");
             }catch(NoSuchMethodException _){
                 fail("test_11, case 021 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("socialClass").getDefaultValue()!=null) fail("test_11, case 022 FAILED");
                 if(!((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName().equals("java.lang.ClassAnnotationsTest$MA1$MA1_1$MA1_1_1$MA1_1_1_1$MA1_1_1_1_1$MA1_1_1_1_1_1$MA1_1_1_1_1_1_1$iiii$Prltr")) fail("test_11, case 023 FAILED: "+((Class)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("socialClass").getDefaultValue()).getName());
             }catch(NoSuchMethodException _){
                 fail("test_11, case 024 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("authorSurname").getDefaultValue()!=null) fail("test_11, case 025 FAILED");
                 if(!((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSurname").getDefaultValue()).equals("Zapreyev")) fail("test_11, case 026 FAILED: "+((String)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("authorSername").getDefaultValue()));
             }catch(NoSuchMethodException _){
                 fail("test_11, case 027 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("blackMarker").getDefaultValue()!=null) fail("test_11, case 028 FAILED");
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()).getClass().getSimpleName().equals("MA1_1_1_1_1_1_1")) fail("test_11, case 029 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1)MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("blackMarker").getDefaultValue()));
             }catch(NoSuchMethodException _){
                 fail("test_11, case 030 FAILED");
             }
             try{
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).getClass().getMethod("whiteMarkers").getDefaultValue()!=null) fail("test_11, case 031 FAILED");
                 if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.internalAnnotation[])MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.class.getMethod("whiteMarkers").getDefaultValue()).length != 3) fail("test_11, case 032 FAILED");
             }catch(NoSuchMethodException _){
                 fail("test_11, case 033 FAILED");
             }
             if(aa[i].annotationType() != ia.annotationType()) fail("test_11, case 034 FAILED: "+aa[i].annotationType().toString()+"|"+ia.annotationType().toString());
             if(!aa[i].annotationType().getSimpleName().equals(ia.annotationType().getSimpleName())) fail("test_11, case 035_1 FAILED: "+aa[i].annotationType().getSimpleName().equals(ia.annotationType().getSimpleName()));
             if(aa[i].equals((Object) ia)) fail("test_11, case 035 FAILED: "+aa[i].equals((Object) ia));
             if(aa[i].hashCode() == ia.hashCode()) fail("test_11, case 036 FAILED: "+Integer.toString(aa[i].hashCode())+"|"+Integer.toString(ia.hashCode()));
             if(aa[i].toString().equals(ia.toString())) fail("test_11, case 037 FAILED: "+aa[i].toString()+"|"+ia.toString());
             //System.out.println(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname().equals("Zapreyev")) fail("test_11, case 038 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorSurname());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0].equals("Zapreyev") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1].equals("Serguei") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2].equals("Stepanovich")) fail("test_11, case 039 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).authorFullName()[2]);
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName().equals("Prltr")) fail("test_11, case 040 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClass().getSimpleName());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName().equals("Prltr") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName().equals("Brg") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName().equals("Krstnn") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName().equals("Arstcrt") ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName().equals("Clrc")) fail("test_11, case 041 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[0].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[1].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[2].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[3].getSimpleName()+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).socialClasses()[4].getSimpleName());
             if(!((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author().equals("UNKNOWN")) fail("test_11, case 042 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).blackMarker().author());
             if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()!=true ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()!=(byte) 256 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()!='Z' ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()!=Double.MAX_VALUE ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()!=Float.MIN_VALUE ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()!=777 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()!=Long.MAX_VALUE + Long.MIN_VALUE ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()!=32655 ||
                     !((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author().equals("Zapreyev 4")) fail("test_11, case 043 FAILED: "+Boolean.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr1()) +"|"+
                             Byte.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr2()) +"|"+
                             Character.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr3()) +"|"+
                             Double.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr4()) +"|"+
                             Float.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr5()) +"|"+
                             Integer.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr6()) +"|"+
                             Long.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr7()) +"|"+
                             Short.toString(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].attr8()) +"|"+
                             ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).whiteMarkers()[0].itself().author());
             if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant()!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.M_S) fail("test_11, case 044 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constant());
             if(((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.W_T5 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.V_T5 ||
                     ((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]!=MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii.ME1.E1_.E1_1.E1_2.E1_3.E1_4.E1_5.G_A_T5) fail("test_11, case 045 FAILED: "+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[0]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[1]+"|"+((MA1.MA1_1.MA1_1_1.MA1_1_1_1.MA1_1_1_1_1.MA1_1_1_1_1_1.MA1_1_1_1_1_1_1.iiii)aa[i]).constants()[2]);
             }
         }
   }
}