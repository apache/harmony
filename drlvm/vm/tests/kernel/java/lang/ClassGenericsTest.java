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
 *   Area for supposed testing: top, member, local, enums, interfaces 
 *   - fields,
 *   - methods
 *   - constructors
 */

import java.lang.reflect.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import junit.framework.TestCase;

/*
 * Created on April 03, 2006
 *
 * This ClassGenericsTest class is used to test the Core API Class, Method, Constructor, Field classes
 * 
 */

/**
 * ###############################################################################
 * ###############################################################################
 * TODO: 1.
 * ###############################################################################
 * ###############################################################################
 */

@Retention(value=RetentionPolicy.RUNTIME)
@interface igt{
    abstract String author() default "Zapreyev";
};

@SuppressWarnings(value={"unchecked"}) public class ClassGenericsTest<X> extends TestCase {
    @igt class Mc001 {};

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    /**
     * getTypeParameters(), getGenericInterfaces(), getGenericSuperclass() of non-generalized member class
     */
    public void test_1() {
        if(Mc001.class.getTypeParameters().length!=0) fail("test_1, case 001 FAILED: "+Mc001.class.getTypeParameters());
        if(Mc001.class.getGenericInterfaces().length!=0) fail("test_1, case 002 FAILED: "+Mc001.class.getGenericInterfaces());
        if(!Mc001.class.getGenericSuperclass().equals(Object.class)) fail("test_1, case 003 FAILED: "+Mc001.class.getGenericSuperclass());
    }

    /**
     * getTypeParameters(), getGenericInterfaces(), getGenericSuperclass() of generalized member class
     */
    @igt interface MI001<T0 extends java.io.Serializable> {};
    @igt interface MI002<T1 extends MI001> {};
    @igt interface MI003<T2> extends MI001 {};
    @igt interface MI004<T2> /*extends MI001<MI001>*/ {
        @igt interface MI005<T21, T22> /*extends MI003<MI002>*/ {
        };
    };
    @igt(author="Serguei Stepanovich Zapreyev") public class Mc002<T3 extends ClassGenericsTest> {
        @igt public class Mc004<T5 extends ClassGenericsTest> {   
        };
    };
    @igt class Mc003<T4 extends Thread &java.io.Serializable &Cloneable> extends java.lang.ClassGenericsTest<? super Class>.Mc002<ClassGenericsTest> implements MI002<MI003<java.io.Serializable>>, MI003<MI003<Cloneable>>, MI004/*<Cloneable>*/.MI005<Type, Type> {};
    /**/@igt class Mc007\u0391<T7 extends Thread &java.io.Serializable &Cloneable> {};
    /**/@igt class Mc008\u0576\u06C0\u06F10<T8 extends Mc007\u0391 &java.io.Serializable &Cloneable> extends java.lang.ClassGenericsTest<? super Mc007\u0391>.Mc002<ClassGenericsTest> implements MI002<MI003<java.io.Serializable>>, MI003<MI003<Cloneable>>, MI004.MI005<Mc007\u0391, Type> {};
    /**/@igt class Mc010\u0576\u06C0\u06F10 {};
    /**/@igt interface MI010\u0576\u06C0\u06F10 {};
    /**/@igt class MC011\u0576\u06C0\u06F10 extends Mc010\u0576\u06C0\u06F10 implements MI010\u0576\u06C0\u06F10 {};
    public void test_2() {
        //ClassGenericsTest<? super Class>.Mc002<ClassGenericsTest>.Mc004<ClassGenericsTest> f1;
        Locale locale = Locale.getDefault();
        Locale locale2;
        locale2 = new Locale("*.UTF8");
        if(locale==locale2){
        Type ap[];
        TypeVariable tv;
        Type ab[];
        Type ai[];
        Type aa[];
        Type aa2[];
        Type oc;
        WildcardType wc;
        Type aa3[];

        ap = Mc003.class.getTypeParameters();
        if(ap.length!=1) fail("test_2, case 001 FAILED: "+ap.length);
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("T4")) fail("test_2, case 002 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$Mc003")) fail("test_2, case 003 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_2, case 004 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_2, case 005 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_2, case 006 FAILED: "+((Class)ab[2]).getName());
        //////////////////////////////////////////////////////////////////////////////////////////////
        ai = Mc003.class.getGenericInterfaces();
        if(ai.length!=3) fail("test_2, case 007 FAILED: "+ai.length);
        if(!((Class)((ParameterizedType)ai[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI002")) fail("test_2, case 008 FAILED: "+((Class)((ParameterizedType)ai[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest")) fail("test_2, case 009 FAILED: "+((Class)((ParameterizedType)ai[0]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[0]).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 010 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI003")) fail("test_2, case 011 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest")) fail("test_2, case 012 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.io.Serializable
        if(aa2.length!=1) fail("test_2, case 013 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.io.Serializable")) fail("test_2, case 014 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());

        if(!((Class)((ParameterizedType)ai[1]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI003")) fail("test_2, case 015 FAILED: "+((Class)((ParameterizedType)ai[1]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[1]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest")) fail("test_2, case 016 FAILED: "+((Class)((ParameterizedType)ai[1]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[1]).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 017 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI003")) fail("test_2, case 018 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest")) fail("test_2, case 019 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.lang.Cloneable
        if(aa2.length!=1) fail("test_2, case 020 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.lang.Cloneable")) fail("test_2, case 021 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());

        if(!((Class)((ParameterizedType)ai[2]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI004$MI005")) fail("test_2, case 022 FAILED: "+((Class)((ParameterizedType)ai[2]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[2]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest$MI004")) fail("test_2, case 023 FAILED: "+((Class)((ParameterizedType)ai[2]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[2]).getActualTypeArguments();
        if(aa.length!=2) fail("test_2, case 002 FAILED: "+aa.length);
        if(!((Class)aa[0]).getName().equals("java.lang.reflect.Type")) fail("test_2, case 024 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)aa[1]).getName().equals("java.lang.reflect.Type")) fail("test_2, case 025 FAILED: "+((Class)((ParameterizedType)aa[1]).getRawType()).getName());
        
        //////////////////////////////////////////////////////////////////////////////////////////////
        if(!(Mc003.class.getGenericSuperclass() instanceof java.lang.reflect.ParameterizedType)) fail("test_2, case 026 FAILED: "+Mc003.class.getGenericSuperclass());
        if(!((Class)((ParameterizedType)Mc003.class.getGenericSuperclass()).getRawType()).equals(Mc002.class)) fail("test_2, case 027 FAILED: "+Mc003.class.getGenericSuperclass());
        if(!((igt)((Class)((ParameterizedType)Mc003.class.getGenericSuperclass()).getRawType()).getAnnotations()[0]).author().equals("Serguei Stepanovich Zapreyev")) fail("test_2, case 028 FAILED: "+((igt)((Class)((ParameterizedType)Mc003.class.getGenericSuperclass()).getRawType()).getAnnotations()[0]).author());
        aa = ((ParameterizedType)Mc003.class.getGenericSuperclass()).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 029 FAILED: "+aa.length);
        if(!((Class)aa[0]).getName().equals("java.lang.ClassGenericsTest")) fail("test_2, case 030 FAILED: "+aa[0]);
        oc = ((ParameterizedType)Mc003.class.getGenericSuperclass()).getOwnerType();
        if(!((Class)((ParameterizedType)oc).getRawType()).equals(java.lang.ClassGenericsTest.class)) fail("test_2, case 031 FAILED: "+Mc003.class.getGenericSuperclass());
        if(((ParameterizedType)oc).getOwnerType()!=null) fail("test_2, case 032 FAILED: "+((ParameterizedType)oc).getOwnerType());
        aa = ((ParameterizedType)oc).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 033 FAILED: "+aa.length);
        wc = (WildcardType)aa[0];
        aa3 = wc.getLowerBounds();
        if(aa3.length!=1) fail("test_2, case 034 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Class.class)) fail("test_2, case 035 FAILED: "+((Class)aa3[0]));
        aa3 = wc.getUpperBounds();
        if(aa3.length!=1) fail("test_2, case 036 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Object.class)) fail("test_2, case 037 FAILED: "+((Class)aa3[0]));
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
        ap = Mc007\u0391.class.getTypeParameters();
        if(ap.length!=1) fail("test_2, case 038 FAILED: "+ap.length);
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("T7")) fail("test_2, case 039 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$Mc007\u0391")) fail("test_2, case 040 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_2, case 041 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_2, case 042 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_2, case 043 FAILED: "+((Class)ab[2]).getName());
        //////////////////////////////////////////////////////////////////////////////////////////////
        ai = Mc007\u0391.class.getGenericInterfaces();
        if(ai.length!=0) fail("test_2, case 001 FAILED: "+ap.length);
        //////////////////////////////////////////////////////////////////////////////////////////////
        if(!(Mc007\u0391.class.getGenericSuperclass() instanceof java.lang.Object)) fail("test_2, case 044 FAILED: "+Mc003.class.getGenericSuperclass());
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
        ap = Mc008\u0576\u06C0\u06F10.class.getTypeParameters();
        if(ap.length!=1) fail("test_2, case 045 FAILED: "+ap.length);
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("T8")) fail("test_2, case 046 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$Mc008\u0576\u06C0\u06F10")) fail("test_2, case 047 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("java.lang.ClassGenericsTest$Mc007\u0391")) fail("test_2, case 048 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_2, case 049 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_2, case 050 FAILED: "+((Class)ab[2]).getName());
        //////////////////////////////////////////////////////////////////////////////////////////////
        ai = Mc008\u0576\u06C0\u06F10.class.getGenericInterfaces();
        if(ai.length!=3) fail("test_2, case 051 FAILED: "+ai.length);
        if(!((Class)((ParameterizedType)ai[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI002")) fail("test_2, case 052 FAILED: "+((Class)((ParameterizedType)ai[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest")) fail("test_2, case 053 FAILED: "+((Class)((ParameterizedType)ai[0]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[0]).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 054 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI003")) fail("test_2, case 055 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest")) fail("test_2, case 056 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.io.Serializable
        if(aa2.length!=1) fail("test_2, case 057 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.io.Serializable")) fail("test_2, case 058 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());

        if(!((Class)((ParameterizedType)ai[1]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI003")) fail("test_2, case 059 FAILED: "+((Class)((ParameterizedType)ai[1]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[1]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest")) fail("test_2, case 060 FAILED: "+((Class)((ParameterizedType)ai[1]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[1]).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 061 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI003")) fail("test_2, case 062 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest")) fail("test_2, case 063 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.lang.Cloneable
        if(aa2.length!=1) fail("test_2, case 064 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.lang.Cloneable")) fail("test_2, case 065 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());

        if(!((Class)((ParameterizedType)ai[2]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI004$MI005")) fail("test_2, case 066 FAILED: "+((Class)((ParameterizedType)ai[2]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[2]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest$MI004")) fail("test_2, case 067 FAILED: "+((Class)((ParameterizedType)ai[2]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[2]).getActualTypeArguments();
        if(aa.length!=2) fail("test_2, case 068 FAILED: "+aa.length);
        if(!((Class)aa[0]).getName().equals("java.lang.ClassGenericsTest$Mc007\u0391")) fail("test_2, case 069 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)aa[1]).getName().equals("java.lang.reflect.Type")) fail("test_2, case 070 FAILED: "+((Class)((ParameterizedType)aa[1]).getRawType()).getName());
        
        //////////////////////////////////////////////////////////////////////////////////////////////
        if(!(Mc008\u0576\u06C0\u06F10.class.getGenericSuperclass() instanceof java.lang.reflect.ParameterizedType)) fail("test_2, case 071 FAILED: "+Mc003.class.getGenericSuperclass());
        if(!((Class)((ParameterizedType)Mc008\u0576\u06C0\u06F10.class.getGenericSuperclass()).getRawType()).equals(Mc002.class)) fail("test_2, case 072 FAILED: "+Mc003.class.getGenericSuperclass());
        if(!((igt)((Class)((ParameterizedType)Mc008\u0576\u06C0\u06F10.class.getGenericSuperclass()).getRawType()).getAnnotations()[0]).author().equals("Serguei Stepanovich Zapreyev")) fail("test_2, case 073 FAILED: "+((igt)((Class)((ParameterizedType)Mc003.class.getGenericSuperclass()).getRawType()).getAnnotations()[0]).author());
        aa = ((ParameterizedType)Mc008\u0576\u06C0\u06F10.class.getGenericSuperclass()).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 074 FAILED: "+aa.length);System.out.println(aa[0]);
        if(!((Class)aa[0]).getName().equals("java.lang.ClassGenericsTest")) fail("test_2, case 075 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        oc = ((ParameterizedType)Mc008\u0576\u06C0\u06F10.class.getGenericSuperclass()).getOwnerType();
        if(!((Class)((ParameterizedType)oc).getRawType()).equals(java.lang.ClassGenericsTest.class)) fail("test_2, case 076 FAILED: "+Mc003.class.getGenericSuperclass());
        if(((ParameterizedType)oc).getOwnerType()!=null) fail("test_2, case 077 FAILED: "+((ParameterizedType)oc).getOwnerType());
        aa = ((ParameterizedType)oc).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 078 FAILED: "+aa.length);
        wc = (WildcardType)aa[0];
        aa3 = wc.getLowerBounds();
        if(aa3.length!=1) fail("test_2, case 079 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Mc007\u0391.class)) fail("test_2, case 080 FAILED: "+((Class)aa3[0]));
        aa3 = wc.getUpperBounds();
        if(aa3.length!=1) fail("test_2, case 081 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Object.class)) fail("test_2, case 082 FAILED: "+((Class)aa3[0]));
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
        //if (RuntimeAdditionalSupport1.openingFlag) {
	        ap = MC011\u0576\u06C0\u06F10.class.getTypeParameters();
		    if(ap.length!=0) fail("test_2, case 083 FAILED: "+ap.length);
        //////////////////////////////////////////////////////////////////////////////////////////////
			ai = MC011\u0576\u06C0\u06F10.class.getGenericInterfaces();
			if(ai.length!=1) fail("test_2, case 084 FAILED: "+ai.length);
			if(!((Class)ai[0]).getName().equals("java.lang.ClassGenericsTest$MI010\u0576\u06C0\u06F10")) fail("test_2, case 085 FAILED: "+((Class)ai[0]).getName());
        //////////////////////////////////////////////////////////////////////////////////////////////
			if(!((Class)MC011\u0576\u06C0\u06F10.class.getGenericSuperclass()).getName().equals("java.lang.ClassGenericsTest$Mc010\u0576\u06C0\u06F10")) fail("test_2, case 086 FAILED: "+((Class)Mc008\u0576\u06C0\u06F10.class.getGenericSuperclass()).getName());
		//}
        }
  }

    /**
     * getGenericType() of generalized type fields
     */
    class Mc005 extends Thread implements java.io.Serializable, Cloneable {
        private static final long serialVersionUID = 0L;
    };
    public Mc003<Mc005> fld0;
    public ClassGenericsTest<? super Class>.Mc002<ClassGenericsTest>.Mc004<ClassGenericsTest> f0;
    public X f111;
    public ClassGenericsTest f112;
    public void test_3() {
        Type ft = null;
        try{
            ft = java.lang.ClassGenericsTest.class.getField("fld0").getGenericType();
            //{boolean b = true; if(b) return;}
        }catch(NoSuchFieldException e){
            fail("test_3, case 001 FAILED: ");
        }
        Type oc1 = (ParameterizedType)ft;
        if(!((Class)((ParameterizedType)oc1).getRawType()).equals(java.lang.ClassGenericsTest.Mc003.class)) fail("test_3, case 002 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if (RuntimeAdditionalSupport1.openingFlag) {
	        if(!((ParameterizedType)oc1).getOwnerType().toString().equals("java.lang.ClassGenericsTest<X>")) fail("test_3, case 003 FAILED: "+((ParameterizedType)oc1).getOwnerType().toString());
		}
        Type aa0[] = ((ParameterizedType)oc1).getActualTypeArguments();
        if(aa0.length!=1) fail("test_3, case 004 FAILED: "+aa0.length);
        if(!((ParameterizedType)aa0[0]).getRawType().equals(Mc005.class)) fail("test_3, case 005 FAILED: "+(/*(Class)*/aa0[0]));
        if(!((ParameterizedType)aa0[0]).getOwnerType().toString().equals("java.lang.ClassGenericsTest<X>")) fail("test_3, case 006 FAILED: "+(/*(Class)*/aa0[0]));

        Class c = (Class)((ParameterizedType)ft).getRawType();

        Type ap[] = c.getTypeParameters();
        if(ap.length!=1) fail("test_3, case 007 FAILED: "+ap.length);
        TypeVariable tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("T4")) fail("test_3, case 008 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$Mc003")) fail("test_3, case 009 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        Type ab[] = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_3, case 010 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_3, case 011 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_3, case 012 FAILED: "+((Class)ab[2]).getName());
        //////////////////////////////////////////////////////////////////////////////////////////////
        Type ai[] = c.getGenericInterfaces();
        if(ai.length!=3) fail("test_3, case 013 FAILED: "+ai.length);
        if(!((Class)((ParameterizedType)ai[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI002")) fail("test_3, case 014 FAILED: "+((Class)((ParameterizedType)ai[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest")) fail("test_3, case 015 FAILED: "+((Class)((ParameterizedType)ai[0]).getOwnerType()).getName());
        Type aa[] = ((ParameterizedType)ai[0]).getActualTypeArguments();
        if(aa.length!=1) fail("test_3, case 016 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI003")) fail("test_3, case 017 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest")) fail("test_3, case 018 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
        Type aa2[] = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.io.Serializable
        if(aa2.length!=1) fail("test_3, case 019 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.io.Serializable")) fail("test_3, case 020 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());

        if(!((Class)((ParameterizedType)ai[1]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI003")) fail("test_3, case 021 FAILED: "+((Class)((ParameterizedType)ai[1]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[1]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest")) fail("test_3, case 022 FAILED: "+((Class)((ParameterizedType)ai[1]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[1]).getActualTypeArguments();
        if(aa.length!=1) fail("test_3, case 023 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI003")) fail("test_3, case 024 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest")) fail("test_3, case 025 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.lang.Cloneable
        if(aa2.length!=1) fail("test_3, case 026 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.lang.Cloneable")) fail("test_3, case 027 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());

        if(!((Class)((ParameterizedType)ai[2]).getRawType()).getName().equals("java.lang.ClassGenericsTest$MI004$MI005")) fail("test_3, case 028 FAILED: "+((Class)((ParameterizedType)ai[2]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[2]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest$MI004")) fail("test_3, case 029 FAILED: "+((Class)((ParameterizedType)ai[2]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[2]).getActualTypeArguments();
        if(aa.length!=2) fail("test_3, case 030 FAILED: "+aa.length);
        if(!((Class)aa[0]).getName().equals("java.lang.reflect.Type")) fail("test_3, case 031 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)aa[1]).getName().equals("java.lang.reflect.Type")) fail("test_3, case 032 FAILED: "+((Class)((ParameterizedType)aa[1]).getRawType()).getName());
        
        //////////////////////////////////////////////////////////////////////////////////////////////
        if(!(c.getGenericSuperclass() instanceof java.lang.reflect.ParameterizedType)) fail("test_3, case 033 FAILED: "+Mc003.class.getGenericSuperclass());
        if(!((Class)((ParameterizedType)c.getGenericSuperclass()).getRawType()).equals(Mc002.class)) fail("test_3, case 034 FAILED: "+Mc003.class.getGenericSuperclass());
        if(!((igt)((Class)((ParameterizedType)c.getGenericSuperclass()).getRawType()).getAnnotations()[0]).author().equals("Serguei Stepanovich Zapreyev")) fail("test_3, case 035 FAILED: "+((igt)((Class)((ParameterizedType)Mc003.class.getGenericSuperclass()).getRawType()).getAnnotations()[0]).author());
        aa = ((ParameterizedType)c.getGenericSuperclass()).getActualTypeArguments();
        if(aa.length!=1) fail("test_3, case 029 FAILED: "+aa.length);
        if(!((Class)aa[0]).getName().equals("java.lang.ClassGenericsTest")) fail("test_3, case 036 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        Type oc = ((ParameterizedType)c.getGenericSuperclass()).getOwnerType();
        if(!((Class)((ParameterizedType)oc).getRawType()).equals(java.lang.ClassGenericsTest.class)) fail("test_3, case 037 FAILED: "+Mc003.class.getGenericSuperclass());
        if(((ParameterizedType)oc).getOwnerType()!=null) fail("test_3, case 038 FAILED: "+((ParameterizedType)oc).getOwnerType());
        aa = ((ParameterizedType)oc).getActualTypeArguments();
        if(aa.length!=1) fail("test_3, case 039 FAILED: "+aa.length);
        WildcardType wc = (WildcardType)aa[0];
        Type aa3[] = wc.getLowerBounds();
        if(aa3.length!=1) fail("test_3, case 040 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Class.class)) fail("test_3, case 041 FAILED: "+((Class)aa3[0]));
        aa3 = wc.getUpperBounds();
        if(aa3.length!=1) fail("test_3, case 042 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Object.class)) fail("test_3, case 043 FAILED: "+((Class)aa3[0]));

        //////////////////////////////////////////////////////////////////////////////////////////////
        try{
            ft = java.lang.ClassGenericsTest.class.getField("f0").getGenericType();
        }catch(NoSuchFieldException e){
            fail("test_3, case 044 FAILED: ");
        }
        oc1 = (ParameterizedType)ft;
        if(!((Class)((ParameterizedType)oc1).getRawType()).equals(java.lang.ClassGenericsTest.Mc002.Mc004.class)) fail("test_3, case 045 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if(!((ParameterizedType)oc1).getOwnerType().toString().equals("java.lang.ClassGenericsTest<? super java.lang.Class>.Mc002<java.lang.ClassGenericsTest>")) fail("test_3, case 046 FAILED: "+((ParameterizedType)oc1).getOwnerType().toString());
        aa0 = ((ParameterizedType)oc1).getActualTypeArguments();
        if(aa0.length!=1) fail("test_3, case 047 FAILED: "+aa0.length);
        if(!((Class)aa0[0]).equals(ClassGenericsTest.class)) fail("test_3, case 048 FAILED: "+(/*(Class)*/aa0[0]));

        if(!((Class)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getRawType()).equals(java.lang.ClassGenericsTest.Mc002.class)) fail("test_3, case 049 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        aa0 = ((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getActualTypeArguments();
        if(aa0.length!=1) fail("test_3, case 050 FAILED: "+aa0.length);
        if(!((Class)aa0[0]).equals(ClassGenericsTest.class)) fail("test_3, case 051 FAILED: "+(/*(Class)*/aa0[0]));
        if(!((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType().toString().equals("java.lang.ClassGenericsTest<? super java.lang.Class>")) fail("test_3, case 052 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if(!((Class)((ParameterizedType)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType()).getRawType()).equals(java.lang.ClassGenericsTest.class)) fail("test_3, case 053 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));

        aa0 = ((ParameterizedType)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType()).getActualTypeArguments();
        if(aa0.length!=1) fail("test_3, case 054 FAILED: "+aa0.length);
        wc = (WildcardType)aa[0];
        aa3 = wc.getLowerBounds();
        if(aa3.length!=1) fail("test_3, case 055 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Class.class)) fail("test_3, case 056 FAILED: "+((Class)aa3[0]));
        aa3 = wc.getUpperBounds();
        if(aa3.length!=1) fail("test_3, case 057 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Object.class)) fail("test_3, case 058 FAILED: "+((Class)aa3[0]));
////////////////////////////////////////////////////////////////////////////////////////////////////
        try{
            ft = java.lang.ClassGenericsTest.class.getField("f111").getGenericType();
        }catch(NoSuchFieldException e){
            fail("test_3, case 059 FAILED: ");
        }
        tv = (TypeVariable)ft;
        if(!tv.getName().equals("X")) fail("test_3, case 060 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest")) fail("test_3, case 061 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_3, case 062 FAILED: "+((Class)ab[0]).getName());
    }

    /**
     * getGenericType() of non-generalized fields
     */
    public void test_4() {
        Type ft = null;
        try{
            ft = java.lang.ClassGenericsTest.class.getField("f112").getGenericType();
        }catch(NoSuchFieldException e){
            fail("test_4, case 001 FAILED: ");
        }
        if(!((Class)ft).getName().equals("java.lang.ClassGenericsTest")) fail("test_4, case 002 FAILED: "+((Class)ft).getName());
    }
    
    /**
     * getGenericExceptionTypes(), getGenericParameterTypes(), getGenericReturnType(), getTypeParameters(), toGenericString() of generalized method
     */
    static class Mc009\u0576\u06C0\u06F1 extends Throwable 
            implements java.io.Serializable, Cloneable {
        private static final long serialVersionUID = 0L;
    };

/* + */    @igt(author="*****") public <UuUuU extends Throwable, TM1, TM2 extends Thread &java.io.Serializable &Cloneable> void foo1For_5(UuUuU a1) throws UuUuU, java.io.IOException {}
/* + */    public <\u0391 extends Throwable, TM1, TM2, TM3, TM4, TM5, TM6, TM7> X foo2For_5()  throws \u0391, java.io.IOException {X f = null; return f;}
/* + */    public <\u0576\u06C0\u06F1 extends Throwable, \u0576\u06C0\u06F11 extends Throwable, \u0576\u06C0\u06F12 extends Throwable, \u0576\u06C0\u06F13 extends Throwable, TM1, TM2 extends Thread &java.io.Serializable &Cloneable> TM2 foo3For_5(\u0576\u06C0\u06F1[] BAAB, TM1 a1, TM2 a2, ClassGenericsTest<? super Class>.Mc002<ClassGenericsTest>.Mc004<ClassGenericsTest> a3) throws \u0576\u06C0\u06F1, Throwable, \u0576\u06C0\u06F13, \u0576\u06C0\u06F12, \u0576\u06C0\u06F11, java.lang.ClassGenericsTest.Mc009\u0576\u06C0\u06F1 {TM2 f = null; return f;}
/* + */    public Mc003<Mc005> foo4For_5(ClassGenericsTest<? super Class>.Mc002<ClassGenericsTest>.Mc004<?> a1, @igt(author="Czar") Mc003<Mc005> a2, @igt(author="President") Mc003<Mc005> ... a3) {return a2;}
///* + */    public void foo4For_5(ClassGenericsTest<? super Class>.Mc002<ClassGenericsTest>.Mc004<?> a1, @igt(author="Czar") Mc003<Mc005> a2, @igt(author="President") Mc003<Mc005> ... a3) {}
/* - */    public ClassGenericsTest<? super Class>.Mc002<ClassGenericsTest>.Mc004<ClassGenericsTest> foo5For_5(X a1, Class<Type> a2,  ClassGenericsTest<? super Class>.Mc002<ClassGenericsTest>.Mc004<ClassGenericsTest> a3) {return a3;}
    public void test_5() {
        Locale locale = Locale.getDefault();
        Locale locale2;
        locale2 = new Locale("*.UTF8");
        if(locale==locale2){
        Type rt;
        TypeVariable tv;
        ParameterizedType oc1;
        Type aa0[];
        Type ap[];
        Type ab[];
        Type aet[];
        Type atp[];
        Type aa3[];
        GenericArrayType gat;
        WildcardType wc;
        Method m = null;
        try{
            //m = ClassGenericsTest.class.getMethod("foo1For_5");
            java.lang.reflect.Method am[] = ClassGenericsTest.class.getDeclaredMethods();
            for (int i = 0; i < am.length; i++) {
                if (am[i].getName().equals("foo1For_5")) {
                    m = am[i];
                }
            }
            //if(aa.length!=5) fail("test_5, case 0 FAILED: "+aa.length);

            if(!((igt)m.getAnnotation(igt.class)).author().equals("*****")) fail("test_3, case 001 FAILED: "+((igt)m.getAnnotation(igt.class)).author());
        } catch (Exception e) {
            fail("test_5, case 002 FAILED: "+e.toString());
        }
        rt = m.getGenericReturnType();
        if(!((Class)rt).getName().equals("void")) fail("test_5, case 003 FAILED: "+((Class)rt).getName());
        ///////
        atp = m.getTypeParameters();
        if(atp.length!=3) fail("test_5, case 004 FAILED: "+atp.length);
        tv = (TypeVariable)atp[0];
        if(!tv.getName().equals("UuUuU")) fail("test_5, case 005 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo1For_5")) fail("test_5, case 006 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 007 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Throwable")) fail("test_5, case 008 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[1];
        if(!tv.getName().equals("TM1")) fail("test_5, case 009 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo1For_5")) fail("test_5, case 010 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 011 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_5, case 012 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[2];
        if(!tv.getName().equals("TM2")) fail("test_5, case 013 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo1For_5")) fail("test_5, case 014 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=3) fail("test_5, case 015 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_5, case 016 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_5, case 017 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_5, case 018 FAILED: "+((Class)ab[2]).getName());
        ///////
        ap = m.getGenericParameterTypes();
        if(ap.length!=1) fail("test_5, case 019 FAILED: "+ap.length);
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("UuUuU")) fail("test_5, case 020 FAILED: "+tv.getName());
        ///////
        aet = m.getGenericExceptionTypes();
        if(aet.length!=2) fail("test_5, case 021 FAILED: "+ap.length);
        tv = (TypeVariable)aet[0];
        if(!tv.getName().equals("UuUuU")) fail("test_5, case 022 FAILED: "+tv.getName());
        if(!((Class)aet[1]).getName().equals("java.io.IOException")) fail("test_5, case 023 FAILED: "+((Class)aet[1]).getName());
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        m = null;
        try{
            java.lang.reflect.Method am[] = ClassGenericsTest.class.getDeclaredMethods();
            for (int i = 0; i < am.length; i++) {
                if (am[i].getName().equals("foo2For_5")) {
                    m = am[i];
                }
            }
            //if(!((igt)m.getAnnotation(igt.class)).author().equals("*****")) fail("test_3, case 003 FAILED: "+((igt)m.getAnnotation(igt.class)).author());
        } catch (Exception e) {
            fail("test_5, case 024 FAILED: "+e.toString());
        }
        rt = m.getGenericReturnType();
        tv = ((TypeVariable)rt);
        if(!tv.getName().equals("X")) fail("test_5, case 025 FAILED: "+((Class)rt).getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest")) fail("test_5, case 026 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ///////
        atp = m.getTypeParameters();
        if(atp.length!=8) fail("test_5, case 027 FAILED: "+atp.length);
        tv = (TypeVariable)atp[0];
        if(!tv.getName().equals("\u0391")) fail("test_5, case 028 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo2For_5")) fail("test_5, case 028 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 030 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Throwable")) fail("test_5, case 031 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[1];
        if(!tv.getName().equals("TM1")) fail("test_5, case 032 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo2For_5")) fail("test_5, case 033 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 034 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_5, case 035 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[2];
        if(!tv.getName().equals("TM2")) fail("test_5, case 036 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo2For_5")) fail("test_5, case 037 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 038 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_5, case 039 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[3];
        if(!tv.getName().equals("TM3")) fail("test_5, case 040 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo2For_5")) fail("test_5, case 041 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 042 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_5, case 043 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[4];
        if(!tv.getName().equals("TM4")) fail("test_5, case 044 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo2For_5")) fail("test_5, case 045 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 046 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_5, case 047 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[5];
        if(!tv.getName().equals("TM5")) fail("test_5, case 048 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo2For_5")) fail("test_5, case 049 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 050 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_5, case 051 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[6];
        if(!tv.getName().equals("TM6")) fail("test_5, case 052 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo2For_5")) fail("test_5, case 053 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 054 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_5, case 055 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[7];
        if(!tv.getName().equals("TM7")) fail("test_5, case 056 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo2For_5")) fail("test_5, case 057 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 058 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_5, case 059 FAILED: "+((Class)ab[0]).getName());
        ///////
        ap = m.getGenericParameterTypes();
        if(ap.length!=0) fail("test_5, case 060 FAILED: "+ap.length);
        //tv = (TypeVariable)ap[0];
        //if(!tv.getName().equals("UuUuU")) fail("test_5, case 003 FAILED: "+tv.getName());
        ///////
        aet = m.getGenericExceptionTypes();
        if(aet.length!=2) fail("test_5, case 061 FAILED: "+ap.length);
        tv = (TypeVariable)aet[0];
        if(!tv.getName().equals("\u0391")) fail("test_5, case 062 FAILED: "+tv.getName());
        if(!((Class)aet[1]).getName().equals("java.io.IOException")) fail("test_5, case 063 FAILED: "+((Class)aet[1]).getName());
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        m = null;
        try{
            java.lang.reflect.Method am[] = ClassGenericsTest.class.getDeclaredMethods();
            for (int i = 0; i < am.length; i++) {
                if (am[i].getName().equals("foo3For_5")) {
                    m = am[i];
                }
            }
            //if(!((igt)m.getAnnotation(igt.class)).author().equals("*****")) fail("test_3, case 003 FAILED: "+((igt)m.getAnnotation(igt.class)).author());
        } catch (Exception e) {
            fail("test_5, case 064 FAILED: "+e.toString());
        }
        rt = m.getGenericReturnType();
        tv = ((TypeVariable)rt);
        if(!tv.getName().equals("TM2")) fail("test_5, case 065 FAILED: "+((Class)rt).getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo3For_5")) fail("test_5, case 066 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ///////
        atp = m.getTypeParameters();
        if(atp.length!=6) fail("test_5, case 067 FAILED: "+atp.length);
        tv = (TypeVariable)atp[0];
        if(!tv.getName().equals("\u0576\u06C0\u06F1")) fail("test_5, case 068 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo3For_5")) fail("test_5, case 069 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 070 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Throwable")) fail("test_5, case 071 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[1];
        if(!tv.getName().equals("\u0576\u06C0\u06F11")) fail("test_5, case 072 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo3For_5")) fail("test_5, case 073 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 074 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Throwable")) fail("test_5, case 075 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[2];
        if(!tv.getName().equals("\u0576\u06C0\u06F12")) fail("test_5, case 076 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo3For_5")) fail("test_5, case 077 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 001 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Throwable")) fail("test_5, case 078 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[3];
        if(!tv.getName().equals("\u0576\u06C0\u06F13")) fail("test_5, case 079 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo3For_5")) fail("test_5, case 080 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 081 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Throwable")) fail("test_5, case 082 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[4];
        if(!tv.getName().equals("TM1")) fail("test_5, case 083 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo3For_5")) fail("test_5, case 084 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_5, case 085 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_5, case 086 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[5];
        if(!tv.getName().equals("TM2")) fail("test_5, case 087 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo3For_5")) fail("test_5, case 088 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=3) fail("test_5, case 089 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_5, case 090 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_5, case 091 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_5, case 092 FAILED: "+((Class)ab[0]).getName());
        ///////
        ap = m.getGenericParameterTypes();
        if(ap.length!=4) fail("test_5, case 093 FAILED: "+ap.length);
        gat = (GenericArrayType)ap[0];
        tv = (TypeVariable)gat.getGenericComponentType();
        if(!tv.getName().equals("\u0576\u06C0\u06F1")) fail("test_5, case 094 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo3For_5")) fail("test_5, case 095 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        tv = (TypeVariable)ap[1];
        if(!tv.getName().equals("TM1")) fail("test_5, case 096 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo3For_5")) fail("test_5, case 097 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        tv = (TypeVariable)ap[2];
        if(!tv.getName().equals("TM2")) fail("test_5, case 098 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo3For_5")) fail("test_5, case 099 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        oc1 = (ParameterizedType)ap[3];
        if(!((Class)((ParameterizedType)oc1).getRawType()).equals(java.lang.ClassGenericsTest.Mc002.Mc004.class)) fail("test_5, case 100 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if(!((ParameterizedType)oc1).getOwnerType().toString().equals("java.lang.ClassGenericsTest<? super java.lang.Class>.Mc002<java.lang.ClassGenericsTest>")) fail("test_5, case 101 FAILED: "+((ParameterizedType)oc1).getOwnerType().toString());
        aa0 = ((ParameterizedType)oc1).getActualTypeArguments();
        if(aa0.length!=1) fail("test_5, case 102 FAILED: "+aa0.length);
        //System.out.println(aa0[0]);
        if(!((Class)aa0[0]).equals(ClassGenericsTest.class)) fail("test_5, case 103 FAILED: "+(aa0[0]));

        if(!((Class)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getRawType()).equals(java.lang.ClassGenericsTest.Mc002.class)) fail("test_5, case 104 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        aa0 = ((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getActualTypeArguments();
        if(aa0.length!=1) fail("test_5, case 105 FAILED: "+aa0.length);
        if(!((Class)aa0[0]).equals(ClassGenericsTest.class)) fail("test_5, case 106 FAILED: "+(aa0[0]));
        if(!((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType().toString().equals("java.lang.ClassGenericsTest<? super java.lang.Class>")) fail("test_5, case 107 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if(!((Class)((ParameterizedType)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType()).getRawType()).equals(java.lang.ClassGenericsTest.class)) fail("test_5, case 108 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));

        aa0 = ((ParameterizedType)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType()).getActualTypeArguments();
        if(aa0.length!=1) fail("test_5, case 109 FAILED: "+aa0.length);
        wc = (WildcardType)aa0[0];
        aa3 = wc.getLowerBounds();
        if(aa3.length!=1) fail("test_5, case 110 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Class.class)) fail("test_2, case 111 FAILED: "+((Class)aa3[0]));
        aa3 = wc.getUpperBounds();
        if(aa3.length!=1) fail("test_5, case 112 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Object.class)) fail("test_5, case 113 FAILED: "+((Class)aa3[0]));
        ///////
        aet = m.getGenericExceptionTypes();
        if(aet.length!=6) fail("test_5, case 114 FAILED: "+ap.length);
        tv = (TypeVariable)aet[0];
        if(!tv.getName().equals("\u0576\u06C0\u06F1")) fail("test_5, case 115 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo3For_5")) fail("test_5, case 116 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        if(!((Class)aet[1]).getName().equals("java.lang.Throwable")) fail("test_5, case 117 FAILED: "+((Class)aet[1]).getName());
        tv = (TypeVariable)aet[2];
        if(!tv.getName().equals("\u0576\u06C0\u06F13")) fail("test_5, case 118 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo3For_5")) fail("test_5, case 119 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        tv = (TypeVariable)aet[3];
        if(!tv.getName().equals("\u0576\u06C0\u06F12")) fail("test_5, case 120 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo3For_5")) fail("test_5, case 121 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        tv = (TypeVariable)aet[4];
        if(!tv.getName().equals("\u0576\u06C0\u06F11")) fail("test_5, case 122 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo3For_5")) fail("test_5, case 123 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        
        if(!((Class)aet[5]).getName().equals("java.lang.ClassGenericsTest$Mc009\u0576\u06C0\u06F1")) fail("test_5, case 1231 FAILED: "+((Class)rt).getName());
       /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        m = null;
        try{
            java.lang.reflect.Method am[] = ClassGenericsTest.class.getDeclaredMethods();
            for (int i = 0; i < am.length; i++) {
                if (am[i].getName().equals("foo4For_5")) {
                    m = am[i];
                }
            }
            //if(!((igt)m.getAnnotation(igt.class)).author().equals("*****")) fail("test_3, case 003 FAILED: "+((igt)m.getAnnotation(igt.class)).author());
        } catch (Exception e) {
            fail("test_5, case 124 FAILED: "+e.toString());
        }
        rt = m.getGenericReturnType();
        oc1 = (ParameterizedType)rt;
        if(!((Class)((ParameterizedType)oc1).getRawType()).equals(java.lang.ClassGenericsTest.Mc003.class)) fail("test_5, case 125 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if (RuntimeAdditionalSupport1.openingFlag) {
            if(!((ParameterizedType)oc1).getOwnerType().toString().equals("java.lang.ClassGenericsTest<X>")) fail("test_5, case 126 FAILED: "+((ParameterizedType)oc1).getOwnerType().toString());
        }
        aa0 = ((ParameterizedType)oc1).getActualTypeArguments();
        if(aa0.length!=1) fail("test_2, case 127 FAILED: "+aa0.length);
        if(!((ParameterizedType)aa0[0]).getRawType().equals(Mc005.class)) fail("test_5, case 128 FAILED: "+(aa0[0]));
        if(!((ParameterizedType)aa0[0]).getOwnerType().toString().equals("java.lang.ClassGenericsTest<X>")) fail("test_5, case 129 FAILED: "+(aa0[0]));

        //Class c = (Class)((ParameterizedType)ft).getRawType();
        Class c = (Class)(oc1).getRawType();

        ap = c.getTypeParameters();
        if(ap.length!=1) fail("test_5, case 130 FAILED: "+ap.length);
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("T4")) fail("test_5, case 131 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$Mc003")) fail("test_5, case 132 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_5, case 133 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_5, case 134 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_5, case 135 FAILED: "+((Class)ab[2]).getName());
        ///////
        atp = m.getTypeParameters();
        if(atp.length!=0) fail("test_5, case 136 FAILED: "+atp.length);
        ///////
        ap = m.getGenericParameterTypes();
        if(ap.length!=3) fail("test_5, case 137 FAILED: "+ap.length);
        oc1 = (ParameterizedType)ap[0];
        if(!((Class)((ParameterizedType)oc1).getRawType()).equals(java.lang.ClassGenericsTest.Mc002.Mc004.class)) fail("test_5, case 138 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if(!((ParameterizedType)oc1).getOwnerType().toString().equals("java.lang.ClassGenericsTest<? super java.lang.Class>.Mc002<java.lang.ClassGenericsTest>")) fail("test_5, case 139 FAILED: "+((ParameterizedType)oc1).getOwnerType().toString());
        aa0 = ((ParameterizedType)oc1).getActualTypeArguments();
        if(aa0.length!=1) fail("test_5, case 140 FAILED: "+aa0.length);

        wc = (WildcardType)aa0[0];
        aa3 = wc.getLowerBounds();
        if(aa3.length!=0) fail("test_5, case 141 FAILED: "+aa3.length);
        //if(!((Class)aa3[0]).equals(Class.class)) fail("test_2, case 050 FAILED: "+((Class)aa3[0]));
        aa3 = wc.getUpperBounds();
        if(aa3.length!=1) fail("test_5, case 142 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Object.class)) fail("test_5, case 143 FAILED: "+((Class)aa3[0]));

        if(!((Class)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getRawType()).equals(java.lang.ClassGenericsTest.Mc002.class)) fail("test_5, case 144 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        aa0 = ((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getActualTypeArguments();
        if(aa0.length!=1) fail("test_5, case 145 FAILED: "+aa0.length);
        if(!((Class)aa0[0]).equals(ClassGenericsTest.class)) fail("test_5, case 146 FAILED: "+(aa0[0]));
        if(!((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType().toString().equals("java.lang.ClassGenericsTest<? super java.lang.Class>")) fail("test_5, case 147 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if(!((Class)((ParameterizedType)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType()).getRawType()).equals(java.lang.ClassGenericsTest.class)) fail("test_5, case 148 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));

        aa0 = ((ParameterizedType)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType()).getActualTypeArguments();
        if(aa0.length!=1) fail("test_5, case 149 FAILED: "+aa0.length);
        wc = (WildcardType)aa0[0];
        aa3 = wc.getLowerBounds();
        if(aa3.length!=1) fail("test_5, case 150 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Class.class)) fail("test_2, case 151 FAILED: "+((Class)aa3[0]));
        aa3 = wc.getUpperBounds();
        if(aa3.length!=1) fail("test_5, case 152 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Object.class)) fail("test_5, case 153 FAILED: "+((Class)aa3[0]));
        ///
        
        oc1 = (ParameterizedType)ap[1];
        if(!((Class)((ParameterizedType)oc1).getRawType()).equals(java.lang.ClassGenericsTest.Mc003.class)) fail("test_5, case 154 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if (RuntimeAdditionalSupport1.openingFlag) {
            if(!((ParameterizedType)oc1).getOwnerType().toString().equals("java.lang.ClassGenericsTest<X>")) fail("test_5, case 155 FAILED: "+((ParameterizedType)oc1).getOwnerType().toString());
        }
        aa0 = ((ParameterizedType)oc1).getActualTypeArguments();
        if(aa0.length!=1) fail("test_2, case 156 FAILED: "+aa0.length);
        if(!((ParameterizedType)aa0[0]).getRawType().equals(Mc005.class)) fail("test_5, case 157 FAILED: "+(aa0[0]));
        if(!((ParameterizedType)aa0[0]).getOwnerType().toString().equals("java.lang.ClassGenericsTest<X>")) fail("test_5, case 158 FAILED: "+(aa0[0]));

        c = (Class)(oc1).getRawType();

        ap = c.getTypeParameters();
        if(ap.length!=1) fail("test_5, case 159 FAILED: "+ap.length);
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("T4")) fail("test_5, case 160 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$Mc003")) fail("test_5, case 161 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_5, case 162 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_5, case 163 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_5, case 164 FAILED: "+((Class)ab[2]).getName());
        ///////
        aet = m.getGenericExceptionTypes();
        if(aet.length!=0) fail("test_5, case 165 FAILED: "+aet.length);
        ///////
        if (RuntimeAdditionalSupport1.openingFlag) {
	        if(!m.toGenericString().equals("public transient java.lang.ClassGenericsTest<X>.Mc003<java.lang.ClassGenericsTest<X>.Mc005> java.lang.ClassGenericsTest.foo4For_5(java.lang.ClassGenericsTest<? super java.lang.Class>.Mc002<java.lang.ClassGenericsTest>.Mc004<?>,java.lang.ClassGenericsTest<X>.Mc003<java.lang.ClassGenericsTest<X>.Mc005>,java.lang.ClassGenericsTest<X>.Mc003<java.lang.ClassGenericsTest<X>.Mc005>[])")) fail("test_5, case 166 FAILED: |"+m.toGenericString()+"|");
		}
        }
  }
    
    /**
     * getGenericExceptionTypes(), getGenericParameterTypes(), getGenericReturnType(), getTypeParameters(), toGenericString() of non-generalized method
     */
    public void foo1For_5_5(int a1) throws java.io.IOException {}
    public void test_5_5() {
        Type rt;
        Type ap[];
        Type aet[];
        Type atp[];
        Method m = null;
        try{
            java.lang.reflect.Method am[] = ClassGenericsTest.class.getDeclaredMethods();
            for (int i = 0; i < am.length; i++) {
                if (am[i].getName().equals("foo1For_5_5")) {
                    m = am[i];
                }
            }
        } catch (Exception e) {
            fail("test_5_5, case 001 FAILED: "+e.toString());
        }
        rt = m.getGenericReturnType();
        if(!((Class)rt).getName().equals("void")) fail("test_5_5, case 002 FAILED: "+((Class)rt).getName());
        ///////
        atp = m.getTypeParameters();
        if(atp.length!=0) fail("test_5_5, case 003 FAILED: "+atp.length);
        ///////
        ap = m.getGenericParameterTypes();
        if(ap.length!=1) fail("test_5_5, case 004 FAILED: "+ap.length);
        if(!((Class)ap[0]).getName().equals("int")) fail("test_5_5, case 005 FAILED: "+((Class)ap[0]).getName());
        ///////
        aet = m.getGenericExceptionTypes();
        if(aet.length!=1) fail("test_5_5, case 006 FAILED: "+ap.length);
        if(!((Class)aet[0]).getName().equals("java.io.IOException")) fail("test_5_5, case 007 FAILED: "+((Class)aet[0]).getName());
        ///////
        if(!m.toGenericString().equals("public void java.lang.ClassGenericsTest.foo1For_5_5(int) throws java.io.IOException")) fail("test_5_5, case 008 FAILED: |"+m.toGenericString()+"|");
	}    

    /**
     * getGenericExceptionTypes(), getGenericParameterTypes(), getGenericReturnType(), getTypeParameters(), toGenericString() of generalized constructor
     */
	class MC006{
/* + */    @igt(author="*****") public <UuUuU extends Throwable, TM1, TM2 extends Thread &java.io.Serializable &Cloneable> MC006(UuUuU a1) throws UuUuU, java.io.IOException {}
/* + */    public <\u0391 extends Throwable, TM1, TM2, TM3, TM4, TM5, TM6, TM7> MC006()  throws \u0391, java.io.IOException {}
/* + */    public <\u0576\u06C0\u06F1 extends Throwable, \u0576\u06C0\u06F11 extends Throwable, \u0576\u06C0\u06F12 extends Throwable, \u0576\u06C0\u06F13 extends Throwable, TM1, TM2 extends Thread &java.io.Serializable &Cloneable> MC006(\u0576\u06C0\u06F1[] BAAB, TM1 a1, TM2 a2, ClassGenericsTest<? super Class>.Mc002<ClassGenericsTest>.Mc004<ClassGenericsTest> a3) throws \u0576\u06C0\u06F1, Throwable, \u0576\u06C0\u06F13, \u0576\u06C0\u06F12, \u0576\u06C0\u06F11 {}
/* + */    public MC006(ClassGenericsTest<? super Class>.Mc002<ClassGenericsTest>.Mc004<?> a1, @igt(author="Czar") Mc003<Mc005> a2, @igt(author="President") Mc003<Mc005> ... a3) {}
/* - */    public MC006(X a1, Class<Type> a2,  ClassGenericsTest<? super Class>.Mc002<ClassGenericsTest>.Mc004<ClassGenericsTest> a3) {}
	}

    public void test_6() {
        TypeVariable tv;
        ParameterizedType oc1;
        Type aa0[];
        Type ap[];
        Type ab[];
        Type aet[];
        Type atp[];
        GenericArrayType gat;
        Constructor<ClassGenericsTest.MC006> m = null;
        try{
			//for(int i = 0; i <ClassGenericsTest.MC006.class.getDeclaredConstructors().length; i++ ) {
			//	System.out.println(ClassGenericsTest.MC006.class.getDeclaredConstructors()[i]);
			//}
            m = ClassGenericsTest.MC006.class.getConstructor(new Class[]{ClassGenericsTest.class, Throwable.class});
            if(!(m.getAnnotation(igt.class)).author().equals("*****")) fail("test_6, case 001 FAILED: "+((igt)m.getAnnotation(igt.class)).author());
        } catch (Exception e) {
            fail("test_6, case 002 FAILED: "+e.toString());
        }
        atp = m.getTypeParameters();
        if(atp.length!=3) fail("test_6, case 004 FAILED: "+atp.length);
        tv = (TypeVariable)atp[0];
        if(!tv.getName().equals("UuUuU")) fail("test_6, case 005 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 006 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 007 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Throwable")) fail("test_6, case 008 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[1];
        if(!tv.getName().equals("TM1")) fail("test_6, case 009 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 010 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 011 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_6, case 012 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[2];
        if(!tv.getName().equals("TM2")) fail("test_6, case 013 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 014 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=3) fail("test_6, case 015 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_6, case 016 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_2, case 017 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_2, case 018 FAILED: "+((Class)ab[2]).getName());
        ///////
        ap = m.getGenericParameterTypes();
        if(ap.length!=1) fail("test_6, case 019 FAILED: "+ap.length);
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("UuUuU")) fail("test_6, case 020 FAILED: "+tv.getName());
        ///////
        aet = m.getGenericExceptionTypes();
        if(aet.length!=2) fail("test_6, case 021 FAILED: "+ap.length);
        tv = (TypeVariable)aet[0];
        if(!tv.getName().equals("UuUuU")) fail("test_6, case 022 FAILED: "+tv.getName());
        if(!((Class)aet[1]).getName().equals("java.io.IOException")) fail("test_6, case 023 FAILED: "+((Class)aet[1]).getName());
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        m = null;
        try{
            m = ClassGenericsTest.MC006.class.getConstructor(new Class[]{ClassGenericsTest.class});
        } catch (Exception e) {
            fail("test_6, case 024 FAILED: "+e.toString());
        }
        ///////
        atp = m.getTypeParameters();
        if(atp.length!=8) fail("test_6, case 027 FAILED: "+atp.length);
        tv = (TypeVariable)atp[0];
        if(!tv.getName().equals("\u0391")) fail("test_6, case 028 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 028 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 030 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Throwable")) fail("test_6, case 031 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[1];
        if(!tv.getName().equals("TM1")) fail("test_6, case 032 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 033 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 034 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_6, case 035 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[2];
        if(!tv.getName().equals("TM2")) fail("test_6, case 036 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 037 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 038 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_6, case 039 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[3];
        if(!tv.getName().equals("TM3")) fail("test_6, case 040 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 041 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 042 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_6, case 043 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[4];
        if(!tv.getName().equals("TM4")) fail("test_6, case 044 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 045 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 046 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_6, case 047 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[5];
        if(!tv.getName().equals("TM5")) fail("test_6, case 048 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 049 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 050 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_6, case 051 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[6];
        if(!tv.getName().equals("TM6")) fail("test_6, case 052 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 053 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 054 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_6, case 055 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[7];
        if(!tv.getName().equals("TM7")) fail("test_6, case 056 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 057 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 058 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_6, case 059 FAILED: "+((Class)ab[0]).getName());
        ///////
        ap = m.getGenericParameterTypes();
        if(ap.length!=0) fail("test_6, case 060 FAILED: "+ap.length);
        //tv = (TypeVariable)ap[0];
        //if(!tv.getName().equals("UuUuU")) fail("test_6, case 003 FAILED: "+tv.getName());
        ///////
        aet = m.getGenericExceptionTypes();
        if(aet.length!=2) fail("test_6, case 061 FAILED: "+ap.length);
        tv = (TypeVariable)aet[0];
        if(!tv.getName().equals("\u0391")) fail("test_6, case 062 FAILED: "+tv.getName());
        if(!((Class)aet[1]).getName().equals("java.io.IOException")) fail("test_6, case 063 FAILED: "+((Class)aet[1]).getName());
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        m = null;
        try{
            m = ClassGenericsTest.MC006.class.getConstructor(new Class[]{java.lang.ClassGenericsTest.class,java.lang.Throwable[].class,java.lang.Object.class,java.lang.Thread.class,java.lang.ClassGenericsTest.Mc002.Mc004.class});
        } catch (Exception e) {
            fail("test_6, case 064 FAILED: "+e.toString());
        }
        ///////
        atp = m.getTypeParameters();
        if(atp.length!=6) fail("test_6, case 067 FAILED: "+atp.length);
        tv = (TypeVariable)atp[0];
        if(!tv.getName().equals("\u0576\u06C0\u06F1")) fail("test_6, case 068 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 069 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 070 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Throwable")) fail("test_6, case 071 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[1];
        if(!tv.getName().equals("\u0576\u06C0\u06F11")) fail("test_6, case 072 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 073 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 074 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Throwable")) fail("test_6, case 075 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[2];
        if(!tv.getName().equals("\u0576\u06C0\u06F12")) fail("test_6, case 076 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 077 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 001 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Throwable")) fail("test_6, case 078 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[3];
        if(!tv.getName().equals("\u0576\u06C0\u06F13")) fail("test_6, case 079 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 080 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 081 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Throwable")) fail("test_6, case 082 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[4];
        if(!tv.getName().equals("TM1")) fail("test_6, case 083 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 084 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 085 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_6, case 086 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[5];
        if(!tv.getName().equals("TM2")) fail("test_6, case 087 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 088 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=3) fail("test_6, case 089 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_6, case 090 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_6, case 091 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_6, case 092 FAILED: "+((Class)ab[0]).getName());
        ///////
        ap = m.getGenericParameterTypes();
        if(ap.length!=4) fail("test_6, case 093 FAILED: "+ap.length);
        gat = (GenericArrayType)ap[0];
        tv = (TypeVariable)gat.getGenericComponentType();
        if(!tv.getName().equals("\u0576\u06C0\u06F1")) fail("test_6, case 094 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 095 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        tv = (TypeVariable)ap[1];
        if(!tv.getName().equals("TM1")) fail("test_6, case 096 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 097 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        tv = (TypeVariable)ap[2];
        if(!tv.getName().equals("TM2")) fail("test_6, case 098 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 099 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        oc1 = (ParameterizedType)ap[3];
        if(!((Class)((ParameterizedType)oc1).getRawType()).equals(java.lang.ClassGenericsTest.Mc002.Mc004.class)) fail("test_6, case 100 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if(!((ParameterizedType)oc1).getOwnerType().toString().equals("java.lang.ClassGenericsTest<? super java.lang.Class>.Mc002<java.lang.ClassGenericsTest>")) fail("test_6, case 101 FAILED: "+((ParameterizedType)oc1).getOwnerType().toString());
        aa0 = ((ParameterizedType)oc1).getActualTypeArguments();
        if(aa0.length!=1) fail("test_6, case 102 FAILED: "+aa0.length);
        //System.out.println(aa0[0]);
        if(!((Class)aa0[0]).equals(ClassGenericsTest.class)) fail("test_6, case 103 FAILED: "+(aa0[0]));

        if(!((Class)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getRawType()).equals(java.lang.ClassGenericsTest.Mc002.class)) fail("test_6, case 104 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        aa0 = ((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getActualTypeArguments();
        if(aa0.length!=1) fail("test_6, case 105 FAILED: "+aa0.length);
        if(!((Class)aa0[0]).equals(ClassGenericsTest.class)) fail("test_6, case 106 FAILED: "+(aa0[0]));
        if(!((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType().toString().equals("java.lang.ClassGenericsTest<? super java.lang.Class>")) fail("test_6, case 107 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if(!((Class)((ParameterizedType)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType()).getRawType()).equals(java.lang.ClassGenericsTest.class)) fail("test_6, case 108 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));

        aa0 = ((ParameterizedType)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType()).getActualTypeArguments();
        if(aa0.length!=1) fail("test_6, case 109 FAILED: "+aa0.length);
        WildcardType wc = (WildcardType)aa0[0];
        Type aa3[] = wc.getLowerBounds();
        if(aa3.length!=1) fail("test_6, case 110 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Class.class)) fail("test_2, case 111 FAILED: "+((Class)aa3[0]));
        aa3 = wc.getUpperBounds();
        if(aa3.length!=1) fail("test_6, case 112 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Object.class)) fail("test_6, case 113 FAILED: "+((Class)aa3[0]));
        ///////
        aet = m.getGenericExceptionTypes();
        if(aet.length!=5) fail("test_6, case 114 FAILED: "+ap.length);
        tv = (TypeVariable)aet[0];
        if(!tv.getName().equals("\u0576\u06C0\u06F1")) fail("test_6, case 115 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 116 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        if(!((Class)aet[1]).getName().equals("java.lang.Throwable")) fail("test_6, case 117 FAILED: "+((Class)aet[1]).getName());
        tv = (TypeVariable)aet[2];
        if(!tv.getName().equals("\u0576\u06C0\u06F13")) fail("test_6, case 118 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 119 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        tv = (TypeVariable)aet[3];
        if(!tv.getName().equals("\u0576\u06C0\u06F12")) fail("test_6, case 120 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 121 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        tv = (TypeVariable)aet[4];
        if(!tv.getName().equals("\u0576\u06C0\u06F11")) fail("test_6, case 122 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$MC006")) fail("test_6, case 123 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
       /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        m = null;
        try{
            m = ClassGenericsTest.MC006.class.getConstructor(new Class[]{java.lang.ClassGenericsTest.class,java.lang.ClassGenericsTest.Mc002.Mc004.class,java.lang.ClassGenericsTest.Mc003.class,java.lang.ClassGenericsTest.Mc003[].class});
        } catch (Exception e) {
            fail("test_6, case 124 FAILED: "+e.toString());
        }
        ///////
        atp = m.getTypeParameters();
        if(atp.length!=0) fail("test_6, case 136 FAILED: "+atp.length);
        ///////
        ap = m.getGenericParameterTypes();
        if(ap.length!=3) fail("test_6, case 137 FAILED: "+ap.length);
        oc1 = (ParameterizedType)ap[0];
        if(!((Class)((ParameterizedType)oc1).getRawType()).equals(java.lang.ClassGenericsTest.Mc002.Mc004.class)) fail("test_6, case 138 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if(!((ParameterizedType)oc1).getOwnerType().toString().equals("java.lang.ClassGenericsTest<? super java.lang.Class>.Mc002<java.lang.ClassGenericsTest>")) fail("test_6, case 139 FAILED: "+((ParameterizedType)oc1).getOwnerType().toString());
        aa0 = ((ParameterizedType)oc1).getActualTypeArguments();
        if(aa0.length!=1) fail("test_6, case 140 FAILED: "+aa0.length);

        wc = (WildcardType)aa0[0];
        aa3 = wc.getLowerBounds();
        if(aa3.length!=0) fail("test_6, case 141 FAILED: "+aa3.length);
        //if(!((Class)aa3[0]).equals(Class.class)) fail("test_2, case 050 FAILED: "+((Class)aa3[0]));
        aa3 = wc.getUpperBounds();
        if(aa3.length!=1) fail("test_6, case 142 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Object.class)) fail("test_6, case 143 FAILED: "+((Class)aa3[0]));

        if(!((Class)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getRawType()).equals(java.lang.ClassGenericsTest.Mc002.class)) fail("test_6, case 144 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        aa0 = ((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getActualTypeArguments();
        if(aa0.length!=1) fail("test_6, case 145 FAILED: "+aa0.length);
        if(!((Class)aa0[0]).equals(ClassGenericsTest.class)) fail("test_6, case 146 FAILED: "+(aa0[0]));
        if(!((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType().toString().equals("java.lang.ClassGenericsTest<? super java.lang.Class>")) fail("test_6, case 147 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if(!((Class)((ParameterizedType)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType()).getRawType()).equals(java.lang.ClassGenericsTest.class)) fail("test_6, case 148 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));

        aa0 = ((ParameterizedType)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType()).getActualTypeArguments();
        if(aa0.length!=1) fail("test_6, case 149 FAILED: "+aa0.length);
        wc = (WildcardType)aa0[0];
        aa3 = wc.getLowerBounds();
        if(aa3.length!=1) fail("test_6, case 150 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Class.class)) fail("test_2, case 151 FAILED: "+((Class)aa3[0]));
        aa3 = wc.getUpperBounds();
        if(aa3.length!=1) fail("test_6, case 152 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Object.class)) fail("test_6, case 153 FAILED: "+((Class)aa3[0]));
        ///
        
        oc1 = (ParameterizedType)ap[1];
        if(!((Class)((ParameterizedType)oc1).getRawType()).equals(java.lang.ClassGenericsTest.Mc003.class)) fail("test_6, case 154 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if (RuntimeAdditionalSupport1.openingFlag) {
            if(!((ParameterizedType)oc1).getOwnerType().toString().equals("java.lang.ClassGenericsTest<X>")) fail("test_6, case 155 FAILED: "+((ParameterizedType)oc1).getOwnerType().toString());
        }
        aa0 = ((ParameterizedType)oc1).getActualTypeArguments();
        if(aa0.length!=1) fail("test_2, case 156 FAILED: "+aa0.length);
        if(!((ParameterizedType)aa0[0]).getRawType().equals(Mc005.class)) fail("test_6, case 157 FAILED: "+(aa0[0]));
        if(!((ParameterizedType)aa0[0]).getOwnerType().toString().equals("java.lang.ClassGenericsTest<X>")) fail("test_6, case 158 FAILED: "+(aa0[0]));

        Class c = (Class)(oc1).getRawType();

        ap = c.getTypeParameters();
        if(ap.length!=1) fail("test_6, case 159 FAILED: "+ap.length);
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("T4")) fail("test_6, case 160 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest$Mc003")) fail("test_6, case 161 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_6, case 162 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_6, case 163 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_6, case 164 FAILED: "+((Class)ab[2]).getName());
        ///////
        aet = m.getGenericExceptionTypes();
        if(aet.length!=0) fail("test_6, case 165 FAILED: "+ap.length);
        ///////
        if (RuntimeAdditionalSupport1.openingFlag) {
	        if(!m.toGenericString().equals("public transient java.lang.ClassGenericsTest$MC006(java.lang.ClassGenericsTest<? super java.lang.Class>.Mc002<java.lang.ClassGenericsTest>.Mc004<?>,java.lang.ClassGenericsTest<X>.Mc003<java.lang.ClassGenericsTest<X>.Mc005>,java.lang.ClassGenericsTest<X>.Mc003<java.lang.ClassGenericsTest<X>.Mc005>[])")) fail("test_6, case 166 FAILED: |"+m.toGenericString()+"|");
		}
//java.lang.ClassGenericsTest.class,java.lang.Object.class,java.lang.Class.class,java.lang.ClassGenericsTest$Mc002$Mc004.class
   }
    
    /**
     * getGenericExceptionTypes(), getGenericParameterTypes(), getGenericReturnType(), getTypeParameters(), toGenericString() of non-generalized constructor
     */
	class MC014{
		public MC014(float a1) throws java.io.IOException {}
	}
    public void test_6_6() {
        Type ap[];
        Type aet[];
        Type atp[];
        Constructor m = null;
        m = null;
        try{
            m = ClassGenericsTest.MC014.class.getConstructor(new Class[]{java.lang.ClassGenericsTest.class,float.class});
        } catch (Exception e) {
            fail("test_6_5, case 001 FAILED: "+e.toString());
        }
        atp = m.getTypeParameters();
        if(atp.length!=0) fail("test_6_5, case 002 FAILED: "+atp.length);
        ///////
        ap = m.getGenericParameterTypes();
        if(ap.length!=2) fail("test_6_5, case 003 FAILED: "+ap.length);
        if(!((Class)ap[0]).getName().equals("java.lang.ClassGenericsTest")) fail("test_6_5, case 004 FAILED: "+((Class)ap[0]).getName());
        if(!((Class)ap[1]).getName().equals("float")) fail("test_6_5, case 005 FAILED: "+((Class)ap[0]).getName());
        ///////
        aet = m.getGenericExceptionTypes();
        if(aet.length!=1) fail("test_6_5, case 006 FAILED: "+ap.length);
        if(!((Class)aet[0]).getName().equals("java.io.IOException")) fail("test_6_5, case 007 FAILED: "+((Class)aet[0]).getName());
        ///////
        if(!m.toGenericString().equals("public java.lang.ClassGenericsTest$MC014(java.lang.ClassGenericsTest,float) throws java.io.IOException")) fail("test_6_5, case 008 FAILED: |"+m.toGenericString()+"|");

	}    
}
