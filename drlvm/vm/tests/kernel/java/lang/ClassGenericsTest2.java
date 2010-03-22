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
 *   Area for supposed testing is arrays returning mrthods for:  
 *   - classes, interfaces
 *   - methods
 *   - constructors
 **/

import java.lang.reflect.*;

import junit.framework.TestCase;

/*
 * Created on May 02, 2006
 *
 * This ClassGenericsTest2 class is used to test the Core API  Class, Method, Constructor classes
 * 
 */

/**
 * ###############################################################################
 * ###############################################################################
 * TODO: 1.
 * ###############################################################################
 * ###############################################################################
 */

@SuppressWarnings(value={"unchecked"}) public class ClassGenericsTest2<X> extends TestCase {
    class Mc201 {};

    interface MI201<T0 extends java.io.Serializable> {};
    interface MI202<T1 extends MI201> {};
    interface MI203<T2> extends MI201 {};
    interface MI204<T2> {
        interface MI205<T21, T22> {
        };
    };
    public class Mc202<T3 extends ClassGenericsTest2> {
        public class Mc204<T5 extends ClassGenericsTest2> {
        };
    };
    class Mc203<T4 extends Thread &java.io.Serializable &Cloneable> extends java.lang.ClassGenericsTest2<? super Class>.Mc202<ClassGenericsTest2> implements MI202<MI203<java.io.Serializable>>, MI203<MI203<Cloneable>>, MI204.MI205<Type, Type> {};
    /**
     * check immutability for results of getTypeParameters(), getGenericInterfaces() methods of generalized member class
     * and the attendant reflect implementation methods for WildcardType(getLowerBounds(), getUpperBounds()),
     * TypeVariable(getBounds()), ParameterizedType(getActualTypeArguments())
     */
    public void test_2() {
        Type ap[];
        TypeVariable tv;
        Type ab[];
        Type ai[];
        Type aa[];
        Type aa2[];
        Type oc;
        WildcardType wc;
        Type aa3[];
        //////////////////////////////////////////////////////////////////////////////////////////////
        ai = Mc203.class.getInterfaces();
//---
        ai[0] = null;
        ai = Mc203.class.getInterfaces();
        if(ai.length!=3) fail("test_2, case 001 FAILED: "+ai.length);
        if(!((Class)ai[0]).getName().equals("java.lang.ClassGenericsTest2$MI202")) fail("test_2, case 002 FAILED: "+((Class)((ParameterizedType)ai[0]).getRawType()).getName());

        ap = Mc203.class.getTypeParameters();
        tv = (TypeVariable)ap[0];
        ab = tv.getBounds();
//---
		ab[0] = null;
        ab = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_2, case 003 FAILED: "+((Class)ab[0]).getName());
//---
        ap[0] = null;
        ap = Mc203.class.getTypeParameters();
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("T4")) fail("test_2, case 004 FAILED: "+tv.getName());
//---
        ap = null;
        ap = Mc203.class.getTypeParameters();
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("T4")) fail("test_2, case 005 FAILED: "+tv.getName());

        //////////////////////////////////////////////////////////////////////////////////////////////
//---
        ai = Mc203.class.getGenericInterfaces();
        aa = ((ParameterizedType)ai[2]).getActualTypeArguments();
		aa[0] = null;
		aa[1] = null;
        aa = ((ParameterizedType)ai[2]).getActualTypeArguments();
        if(aa.length!=2) fail("test_2, case 006 FAILED: "+aa.length);
        if(!((Class)aa[0]).getName().equals("java.lang.reflect.Type")) fail("test_2, case 007 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)aa[1]).getName().equals("java.lang.reflect.Type")) fail("test_2, case 008 FAILED: "+((Class)((ParameterizedType)aa[1]).getRawType()).getName());
//---
        ai = Mc203.class.getGenericInterfaces();
        ai[2] = null;
        ai = Mc203.class.getGenericInterfaces();
        if(!((Class)((ParameterizedType)ai[2]).getRawType()).getName().equals("java.lang.ClassGenericsTest2$MI204$MI205")) fail("test_2, case 009 FAILED: "+((Class)((ParameterizedType)ai[2]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[2]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest2$MI204")) fail("test_2, case 010 FAILED: "+((Class)((ParameterizedType)ai[2]).getOwnerType()).getName());
//---
        ai = Mc203.class.getGenericInterfaces();
        aa = ((ParameterizedType)ai[1]).getActualTypeArguments();
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.lang.Cloneable
		aa2[0] = null;
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.lang.Cloneable
        if(aa2.length!=1) fail("test_2, case 009 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.lang.Cloneable")) fail("test_2, case 011 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());
//---
        ai = Mc203.class.getGenericInterfaces();
        aa = ((ParameterizedType)ai[1]).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 012 FAILED: "+aa.length);
		aa[0] = null;
        aa = ((ParameterizedType)ai[1]).getActualTypeArguments();
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest2$MI203")) fail("test_2, case 013 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest2")) fail("test_2, case 014 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
//---
        ai = Mc203.class.getGenericInterfaces();
        ai[1] = null;
        ai = Mc203.class.getGenericInterfaces();
        if(!((Class)((ParameterizedType)ai[1]).getRawType()).getName().equals("java.lang.ClassGenericsTest2$MI203")) fail("test_2, case 015 FAILED: "+((Class)((ParameterizedType)ai[1]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[1]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest2")) fail("test_2, case 016 FAILED: "+((Class)((ParameterizedType)ai[1]).getOwnerType()).getName());
//---
        ai = Mc203.class.getGenericInterfaces();
        aa = ((ParameterizedType)ai[0]).getActualTypeArguments();
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.io.Serializable
		aa2[0] = null;
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.io.Serializable
        if(aa2.length!=1) fail("test_2, case 017 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.io.Serializable")) fail("test_2, case 018 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());
//---
        ai = Mc203.class.getGenericInterfaces();
        aa = ((ParameterizedType)ai[0]).getActualTypeArguments();
		aa[0] = null;
        aa = ((ParameterizedType)ai[0]).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 019 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest2$MI203")) fail("test_2, case 020 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest2")) fail("test_2, case 021 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
//---
        ai = Mc203.class.getGenericInterfaces();
        ai[0] = null;
        ai = Mc203.class.getGenericInterfaces();
        if(ai.length!=3) fail("test_2, case 022 FAILED: "+ai.length);
        if(!((Class)((ParameterizedType)ai[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest2$MI202")) fail("test_2, case 023 FAILED: "+((Class)((ParameterizedType)ai[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest2")) fail("test_2, case 024 FAILED: "+((Class)((ParameterizedType)ai[0]).getOwnerType()).getName());

        ai = Mc203.class.getInterfaces();
        ai[0] = null;
        ai = Mc203.class.getInterfaces();
        if(ai.length!=3) fail("test_2, case 025 FAILED: "+ai.length);
        if(!((Class)ai[0]).getName().equals("java.lang.ClassGenericsTest2$MI202")) fail("test_2, case 026 FAILED: "+((Class)((ParameterizedType)ai[0]).getRawType()).getName());
		//////////////////////////////////////////////////////////////////////////////////////////////
        aa = ((ParameterizedType)Mc203.class.getGenericSuperclass()).getActualTypeArguments();
		aa[0] = null;
        aa = ((ParameterizedType)Mc203.class.getGenericSuperclass()).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 027 FAILED: "+aa.length);
        if(!((Class)aa[0]).getName().equals("java.lang.ClassGenericsTest2")) fail("test_2, case 028 FAILED: "+((Class)aa[0]).getName());
//---
        oc = ((ParameterizedType)Mc203.class.getGenericSuperclass()).getOwnerType();
        aa = ((ParameterizedType)oc).getActualTypeArguments();
		aa[0] = null;
        aa = ((ParameterizedType)oc).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 029 FAILED: "+aa.length);
        wc = (WildcardType)aa[0];
//---
        aa3 = wc.getLowerBounds();
		aa3[0] = null;
        aa3 = wc.getLowerBounds();
        if(aa3.length!=1) fail("test_2, case 030 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Class.class)) fail("test_2, case 031 FAILED: "+((Class)aa3[0]));
//---
        aa3 = wc.getUpperBounds();
		aa3[0] = null;
        aa3 = wc.getUpperBounds();
        if(aa3.length!=1) fail("test_2, case 032 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Object.class)) fail("test_2, case 033 FAILED: "+((Class)aa3[0]));
  }
    
    /**
     * check immutability for results of getGenericExceptionTypes(), getGenericParameterTypes(), getTypeParameters() of generalized method
     * and the attendant reflect implementation methods for WildcardType(getLowerBounds(), getUpperBounds()),
     * TypeVariable(getBounds()), ParameterizedType(getActualTypeArguments())
     */
    class Mc205 extends Thread implements java.io.Serializable, Cloneable {
        private static final long serialVersionUID = 0L;
    };
    static class Mc209 extends Throwable implements java.io.Serializable, Cloneable {
        private static final long serialVersionUID = 0L;
    };
    public <UuUuU extends Throwable, TM1, TM2 extends Thread &java.io.Serializable &Cloneable> void foo1For_5(UuUuU a1) throws UuUuU, java.io.IOException {}
    public void test_5() {
       TypeVariable tv;
        Type ap[];
        Type ab[];
        Type aet[];
        Type atp[];
        Method m = null;
        try{
            java.lang.reflect.Method am[] = ClassGenericsTest2.class.getDeclaredMethods();
            for (int i = 0; i < am.length; i++) {
                if (am[i].getName().equals("foo1For_5")) {
                    m = am[i];
                }
            }
        } catch (Exception e) {
            fail("test_5, case 001 FAILED: "+e.toString());
        }
        ///////
        atp = m.getTypeParameters();
        if(atp.length!=3) fail("test_5, case 002 FAILED: "+atp.length);
//---
		atp[2] = null;
        atp = m.getTypeParameters();
        if(atp.length!=3) fail("test_5, case 003 FAILED: "+atp.length);
        tv = (TypeVariable)atp[2];
        if(!tv.getName().equals("TM2")) fail("test_5, case 004 FAILED: "+tv.getName());
        if(!((Method)tv.getGenericDeclaration()).getName().equals("foo1For_5")) fail("test_5, case 005 FAILED: "+((Method)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
//---
		ab[1] = null;
        ab = tv.getBounds();
        if(ab.length!=3) fail("test_5, case 006 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_5, case 007 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_2, case 008 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_2, case 009 FAILED: "+((Class)ab[2]).getName());
        ///////
        ap = m.getGenericParameterTypes();
        if(ap.length!=1) fail("test_5, case 010 FAILED: "+ap.length);
//---
        ap[0] = null;
        ap = m.getGenericParameterTypes();
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("UuUuU")) fail("test_5, case 011 FAILED: "+tv.getName());
        ///////
        aet = m.getGenericExceptionTypes();
        if(aet.length!=2) fail("test_5, case 012 FAILED: "+ap.length);
//---
        aet[0] = null;
        aet = m.getGenericExceptionTypes();
        if(aet.length!=2) fail("test_5, case 013 FAILED: "+ap.length);
        tv = (TypeVariable)aet[0];
        if(!tv.getName().equals("UuUuU")) fail("test_5, case 014 FAILED: "+tv.getName());
        if(!((Class)aet[1]).getName().equals("java.io.IOException")) fail("test_5, case 015 FAILED: "+((Class)aet[1]).getName());
  }

    /**
     * check immutability for results of getGenericExceptionTypes(), getGenericParameterTypes(), getTypeParameters() of generalized constructor
     * and the attendant reflect implementation methods for WildcardType(getLowerBounds(), getUpperBounds()),
     * TypeVariable(getBounds()), ParameterizedType(getActualTypeArguments())
     */
	class MC006{
	    public <UuUuU extends Throwable, TM1, TM2 extends Thread &java.io.Serializable &Cloneable> MC006(UuUuU a1) throws UuUuU, java.io.IOException {}
	}

    public void test_6() {
        TypeVariable tv;
        Type ap[];
        Type ab[];
        Type aet[];
        Type atp[];
        Constructor m = null;
        try{
            m = ClassGenericsTest2.MC006.class.getConstructor(new Class[]{ClassGenericsTest2.class, Throwable.class});
        } catch (Exception e) {
            fail("test_6, case 001 FAILED: "+e.toString());
        }
        ///////
        atp = m.getTypeParameters();
        if(atp.length!=3) fail("test_6, case 002 FAILED: "+atp.length);
//---
		atp[2] = null;
        atp = m.getTypeParameters();
        tv = (TypeVariable)atp[2];
        if(!tv.getName().equals("TM2")) fail("test_6, case 003 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest2$MC006")) fail("test_6, case 004 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
//---
		ab[1] = null;
        ab = tv.getBounds();
        if(ab.length!=3) fail("test_6, case 005 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_6, case 006 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_2, case 007 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_2, case 008 FAILED: "+((Class)ab[2]).getName());
        ///////
        ap = m.getGenericParameterTypes();
        if(ap.length!=1) fail("test_6, case 009 FAILED: "+ap.length);
//---
        ap[0] = null;
        ap = m.getGenericParameterTypes();
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("UuUuU")) fail("test_6, case 010 FAILED: "+tv.getName());
        ///////
        aet = m.getGenericExceptionTypes();
        if(aet.length!=2) fail("test_6, case 011 FAILED: "+ap.length);
//---
        aet[0] = null;
        aet = m.getGenericExceptionTypes();
        if(aet.length!=2) fail("test_6, case 012 FAILED: "+ap.length);
        tv = (TypeVariable)aet[0];
        if(!tv.getName().equals("UuUuU")) fail("test_6, case 013 FAILED: "+tv.getName());
        if(!((Class)aet[1]).getName().equals("java.io.IOException")) fail("test_6, case 014 FAILED: "+((Class)aet[1]).getName());
	}
}