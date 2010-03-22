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
 * Created on May 03, 2006
 *
 * This ClassGenericsTest3 class is used to test the Core API  Class, Method, Constructor classes
 * 
 */

/**
 * ###############################################################################
 * ###############################################################################
 * TODO: 1.
 * ###############################################################################
 * ###############################################################################
 */

@SuppressWarnings(value={"all"}) public class ClassGenericsTest3<$X$> extends TestCase {
    class $Mc3$01 {};

    interface $MI3$01<$T$0 extends java.io.Serializable> {};
    interface $MI3$02<_T$_1$ extends $MI3$01> {};
    interface $MI3$03<T2$> extends $MI3$01 {};
    interface $MI3$04<T$2> {
        interface $MI3$05<_T$21_, _$T22> {
        };
    };
    public class $Mc3$02<$_T3_$ extends ClassGenericsTest3> {
        public class $Mc3$04<T5 extends ClassGenericsTest3> {
        };
    };
    public class $Mc4$<_> {
        public class $Mc5<_> {
        };
        public class Mc5<_> {
        };
    };
    public interface $Mi4$<_> {
        public interface $$$$$Mi5<_> {
        };
        public interface $$$$Mi5<_> {
        };
        public interface $$$Mi5<_> {
        };
        public interface $$Mi5<_> {
        };
        public interface $Mi5<_> {
        };
        public interface Mi5<_> {
        };
        public interface Mi5$<_> {
        };
    };
    public interface $Mi4<_> {
/*  //compiler errors are risen here:
        public interface $$Mi5<_> {
        };
        public interface $Mi5<_> {
        };
        public interface Mi5<_> {
        };
        public interface Mi5$<_> {
        };
*/
    };
    //class C1<$_T4_$_ extends $Mc4$.$Mc5 &$Mi4$.$Mi5 &$Mi4$.Mi5 &$Mi4$> extends $Mc4$<? super Class>.Mc5<Class> implements $Mi4$<$Mi4$.$Mi5<java.io.Serializable>>, $Mi4$.Mi5$<Cloneable>, $Mi4$.Mi5 {};
    //class C1<$_T4_$_ extends $Mc4$<Integer>.$Mc5<Integer> &$Mi4$.$Mi5 &$Mi4$.Mi5 &$Mi4$> extends $Mc4$<Float>.Mc5<Class> implements  $Mi4$.Mi5 {};
    class C1<$_T4_$_ extends $Mc4$<Integer>.$Mc5<Integer> &$Mi4$.$$$$$Mi5 &$Mi4$.$$$$Mi5 &$Mi4$.$$$Mi5 &$Mi4$.$$Mi5 &$Mi4$.$Mi5 &$Mi4$.Mi5 &$Mi4$> implements  $Mi4$.Mi5 {};
    public void test_0() {
        Type ap[];
        TypeVariable tv;
        Type ab[];
        ap = $Mc4$.class.getTypeParameters();
        ap = $Mc4$.$Mc5.class.getTypeParameters();
        ap = $Mc4$.Mc5.class.getTypeParameters();
        ap = $Mi4$.class.getTypeParameters();
        ap = $Mi4$.$Mi5.class.getTypeParameters();
        ap = $Mi4$.Mi5.class.getTypeParameters();
        ap = C1.class.getTypeParameters();
        if(ap.length!=1) fail("test_2, case 001 FAILED: "+ap.length);
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("$_T4_$_")) fail("test_2, case 002 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest3$C1")) fail("test_2, case 003 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(!((Class)((ParameterizedType)ab[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest3$$Mc4$$$Mc5")) fail("test_2, case 004 FAILED: "+((Class)((ParameterizedType)ab[0]).getRawType()).getName());
		//if(RuntimeAdditionalSupport1.openingFlag || RuntimeAdditionalTest0.os.equals("Lin")) {
		if(true) {
		// is it a bug?:
/*???*/     //if(!((ParameterizedType)ab[0]).getOwnerType().toString().equals("java.lang.ClassGenericsTest3$$Mc4$<java.lang.Integer>.$Mc5<java.lang.Integer>")) fail("test_2, case 005 FAILED: "+((ParameterizedType)ab[0]).getOwnerType().toString());
/*???*/		if(!((ParameterizedType)ab[0]).getOwnerType().toString().equals("java.lang.ClassGenericsTest3<$X$>.$Mc4$<java.lang.Integer>")) fail("test_2, case 005 FAILED: "+((ParameterizedType)ab[0]).getOwnerType().toString());
		} else {
		// but it is the bug in eclipse compiler?:
			if(!((ParameterizedType)ab[0]).getOwnerType().toString().equals("java.lang.ClassGenericsTest3.$Mc4$<java.lang.Integer>")) fail("test_2, case 005 FAILED: "+((ParameterizedType)ab[0]).getOwnerType().toString());
		}
        if(!((Class)ab[1]).getName().equals("java.lang.ClassGenericsTest3$$Mi4$$$$$$$Mi5") || !((Class)ab[1]).getSimpleName().equals("$$$$$Mi5")) fail("test_2, case 006 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[2]).getName().equals("java.lang.ClassGenericsTest3$$Mi4$$$$$$Mi5") || !((Class)ab[2]).getSimpleName().equals("$$$$Mi5")) fail("test_2, case 006 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[3]).getName().equals("java.lang.ClassGenericsTest3$$Mi4$$$$$Mi5") || !((Class)ab[3]).getSimpleName().equals("$$$Mi5")) fail("test_2, case 006 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[4]).getName().equals("java.lang.ClassGenericsTest3$$Mi4$$$$Mi5") || !((Class)ab[4]).getSimpleName().equals("$$Mi5")) fail("test_2, case 006 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[5]).getName().equals("java.lang.ClassGenericsTest3$$Mi4$$$Mi5") || !((Class)ab[5]).getSimpleName().equals("$Mi5")) fail("test_2, case 006 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[6]).getName().equals("java.lang.ClassGenericsTest3$$Mi4$$Mi5") || !((Class)ab[6]).getSimpleName().equals("Mi5")) fail("test_2, case 007 FAILED: "+((Class)ab[2]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[7]).getName().equals("java.lang.ClassGenericsTest3$$Mi4$")) fail("test_2, case 008 FAILED: "+((Class)ab[7]).getName());
	}
    class $Mc3$03<$_T4_$_ extends Thread &java.io.Serializable &Cloneable> extends java.lang.ClassGenericsTest3<? super Class>.$Mc3$02<ClassGenericsTest3> implements $MI3$02<$MI3$03<java.io.Serializable>>, $MI3$03<$MI3$03<Cloneable>>, $MI3$04.$MI3$05<Type, Type> {};
    /**
     * use "$" symbol in identifiers for generalized member class
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
        ap = $Mc3$03.class.getTypeParameters();
        if(ap.length!=1) fail("test_2, case 001 FAILED: "+ap.length);
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("$_T4_$_")) fail("test_2, case 002 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest3$$Mc3$03")) fail("test_2, case 003 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_2, case 004 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_2, case 005 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_2, case 006 FAILED: "+((Class)ab[2]).getName());
        //////////////////////////////////////////////////////////////////////////////////////////////
        ai = $Mc3$03.class.getGenericInterfaces();
        if(ai.length!=3) fail("test_2, case 007 FAILED: "+ai.length);
        if(!((Class)((ParameterizedType)ai[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest3$$MI3$02")) fail("test_2, case 008 FAILED: "+((Class)((ParameterizedType)ai[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest3")) fail("test_2, case 009 FAILED: "+((Class)((ParameterizedType)ai[0]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[0]).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 010 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest3$$MI3$03")) fail("test_2, case 011 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest3")) fail("test_2, case 012 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.io.Serializable
        if(aa2.length!=1) fail("test_2, case 013 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.io.Serializable")) fail("test_2, case 014 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());

        if(!((Class)((ParameterizedType)ai[1]).getRawType()).getName().equals("java.lang.ClassGenericsTest3$$MI3$03")) fail("test_2, case 015 FAILED: "+((Class)((ParameterizedType)ai[1]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[1]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest3")) fail("test_2, case 016 FAILED: "+((Class)((ParameterizedType)ai[1]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[1]).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 017 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest3$$MI3$03")) fail("test_2, case 018 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest3")) fail("test_2, case 019 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.lang.Cloneable
        if(aa2.length!=1) fail("test_2, case 020 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.lang.Cloneable")) fail("test_2, case 021 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());

        if(!((Class)((ParameterizedType)ai[2]).getRawType()).getName().equals("java.lang.ClassGenericsTest3$$MI3$04$$MI3$05")) fail("test_2, case 022 FAILED: "+((Class)((ParameterizedType)ai[2]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[2]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest3$$MI3$04")) fail("test_2, case 023 FAILED: "+((Class)((ParameterizedType)ai[2]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[2]).getActualTypeArguments();
        if(aa.length!=2) fail("test_2, case 002 FAILED: "+aa.length);
        if(!((Class)aa[0]).getName().equals("java.lang.reflect.Type")) fail("test_2, case 024 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)aa[1]).getName().equals("java.lang.reflect.Type")) fail("test_2, case 025 FAILED: "+((Class)((ParameterizedType)aa[1]).getRawType()).getName());
        
        //////////////////////////////////////////////////////////////////////////////////////////////
        if(!($Mc3$03.class.getGenericSuperclass() instanceof java.lang.reflect.ParameterizedType)) fail("test_2, case 026 FAILED: "+$Mc3$03.class.getGenericSuperclass());
        if(!((Class)((ParameterizedType)$Mc3$03.class.getGenericSuperclass()).getRawType()).equals($Mc3$02.class)) fail("test_2, case 027 FAILED: "+$Mc3$03.class.getGenericSuperclass());
        aa = ((ParameterizedType)$Mc3$03.class.getGenericSuperclass()).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 029 FAILED: "+aa.length);
        if(!((Class)aa[0]).getName().equals("java.lang.ClassGenericsTest3")) fail("test_2, case 030 FAILED: "+((Class)aa[0]).getName());
        oc = ((ParameterizedType)$Mc3$03.class.getGenericSuperclass()).getOwnerType();
        if(!((Class)((ParameterizedType)oc).getRawType()).equals(java.lang.ClassGenericsTest3.class)) fail("test_2, case 031 FAILED: "+$Mc3$03.class.getGenericSuperclass());
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
  }

    /**
     * use "$" symbol in identifiers for generalized type fields
     */
    class $Mc3$05 extends Thread implements java.io.Serializable, Cloneable {
        private static final long serialVersionUID = 0L;
    };
    public $Mc3$03<$Mc3$05> fld0;
    public ClassGenericsTest3<? super Class>.$Mc3$02<ClassGenericsTest3>.$Mc3$04<ClassGenericsTest3> f0;
    public $X$ f111;
    public ClassGenericsTest3 f112;
    public void test_3() {
        Type ft = null;
        try{
            ft = java.lang.ClassGenericsTest3.class.getField("fld0").getGenericType();
            //{boolean b = true; if(b) return;}
        }catch(NoSuchFieldException e){
            fail("test_3, case 001 FAILED: ");
        }
        Type oc1 = (ParameterizedType)ft;
        if(!((Class)((ParameterizedType)oc1).getRawType()).equals(java.lang.ClassGenericsTest3.$Mc3$03.class)) fail("test_3, case 002 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if (RuntimeAdditionalSupport1.openingFlag) {
	        if(!((ParameterizedType)oc1).getOwnerType().toString().equals("java.lang.ClassGenericsTest3<$X$>")) fail("test_3, case 003 FAILED: "+((ParameterizedType)oc1).getOwnerType().toString());
		}
        Type aa0[] = ((ParameterizedType)oc1).getActualTypeArguments();
        if(aa0.length!=1) fail("test_3, case 004 FAILED: "+aa0.length);
        if(!((ParameterizedType)aa0[0]).getRawType().equals($Mc3$05.class)) fail("test_3, case 005 FAILED: "+(/*(Class)*/aa0[0]));
        if(!((ParameterizedType)aa0[0]).getOwnerType().toString().equals("java.lang.ClassGenericsTest3<$X$>")) fail("test_3, case 006 FAILED: "+(/*(Class)*/aa0[0]));

        Class c = (Class)((ParameterizedType)ft).getRawType();

        Type ap[] = c.getTypeParameters();
        if(ap.length!=1) fail("test_3, case 007 FAILED: "+ap.length);
        TypeVariable tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("$_T4_$_")) fail("test_3, case 008 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest3$$Mc3$03")) fail("test_3, case 009 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        Type ab[] = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_3, case 010 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_3, case 011 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_3, case 012 FAILED: "+((Class)ab[2]).getName());
        //////////////////////////////////////////////////////////////////////////////////////////////
        Type ai[] = c.getGenericInterfaces();
        if(ai.length!=3) fail("test_3, case 013 FAILED: "+ai.length);
        if(!((Class)((ParameterizedType)ai[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest3$$MI3$02")) fail("test_3, case 014 FAILED: "+((Class)((ParameterizedType)ai[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest3")) fail("test_3, case 015 FAILED: "+((Class)((ParameterizedType)ai[0]).getOwnerType()).getName());
        Type aa[] = ((ParameterizedType)ai[0]).getActualTypeArguments();
        if(aa.length!=1) fail("test_3, case 016 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest3$$MI3$03")) fail("test_3, case 017 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest3")) fail("test_3, case 018 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
        Type aa2[] = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.io.Serializable
        if(aa2.length!=1) fail("test_3, case 019 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.io.Serializable")) fail("test_3, case 020 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());

        if(!((Class)((ParameterizedType)ai[1]).getRawType()).getName().equals("java.lang.ClassGenericsTest3$$MI3$03")) fail("test_3, case 021 FAILED: "+((Class)((ParameterizedType)ai[1]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[1]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest3")) fail("test_3, case 022 FAILED: "+((Class)((ParameterizedType)ai[1]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[1]).getActualTypeArguments();
        if(aa.length!=1) fail("test_3, case 023 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest3$$MI3$03")) fail("test_3, case 024 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest3")) fail("test_3, case 025 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.lang.Cloneable
        if(aa2.length!=1) fail("test_3, case 026 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.lang.Cloneable")) fail("test_3, case 027 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());

        if(!((Class)((ParameterizedType)ai[2]).getRawType()).getName().equals("java.lang.ClassGenericsTest3$$MI3$04$$MI3$05")) fail("test_3, case 028 FAILED: "+((Class)((ParameterizedType)ai[2]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[2]).getOwnerType()).getName().equals("java.lang.ClassGenericsTest3$$MI3$04")) fail("test_3, case 029 FAILED: "+((Class)((ParameterizedType)ai[2]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[2]).getActualTypeArguments();
        if(aa.length!=2) fail("test_3, case 030 FAILED: "+aa.length);
        if(!((Class)aa[0]).getName().equals("java.lang.reflect.Type")) fail("test_3, case 031 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)aa[1]).getName().equals("java.lang.reflect.Type")) fail("test_3, case 032 FAILED: "+((Class)((ParameterizedType)aa[1]).getRawType()).getName());
        
        //////////////////////////////////////////////////////////////////////////////////////////////
        if(!(c.getGenericSuperclass() instanceof java.lang.reflect.ParameterizedType)) fail("test_3, case 033 FAILED: "+$Mc3$03.class.getGenericSuperclass());
        if(!((Class)((ParameterizedType)c.getGenericSuperclass()).getRawType()).equals($Mc3$02.class)) fail("test_3, case 034 FAILED: "+$Mc3$03.class.getGenericSuperclass());
        aa = ((ParameterizedType)c.getGenericSuperclass()).getActualTypeArguments();
        if(aa.length!=1) fail("test_3, case 029 FAILED: "+aa.length);System.out.println(aa[0]);
        if(!((Class)aa[0]).getName().equals("java.lang.ClassGenericsTest3")) fail("test_3, case 036 FAILED: "+((Class)aa[0]).getName());
        Type oc = ((ParameterizedType)c.getGenericSuperclass()).getOwnerType();
        if(!((Class)((ParameterizedType)oc).getRawType()).equals(java.lang.ClassGenericsTest3.class)) fail("test_3, case 037 FAILED: "+$Mc3$03.class.getGenericSuperclass());
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
            ft = java.lang.ClassGenericsTest3.class.getField("f0").getGenericType();
        }catch(NoSuchFieldException e){
            fail("test_3, case 044 FAILED: ");
        }
        oc1 = (ParameterizedType)ft;
        if(!((Class)((ParameterizedType)oc1).getRawType()).equals(java.lang.ClassGenericsTest3.$Mc3$02.$Mc3$04.class)) fail("test_3, case 045 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if(!((ParameterizedType)oc1).getOwnerType().toString().equals("java.lang.ClassGenericsTest3<? super java.lang.Class>.$Mc3$02<java.lang.ClassGenericsTest3>")) fail("test_3, case 046 FAILED: "+((ParameterizedType)oc1).getOwnerType().toString());
        aa0 = ((ParameterizedType)oc1).getActualTypeArguments();
        if(aa0.length!=1) fail("test_3, case 047 FAILED: "+aa0.length);
        if(!((Class)aa0[0]).equals(ClassGenericsTest3.class)) fail("test_3, case 048 FAILED: "+(/*(Class)*/aa0[0]));

        if(!((Class)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getRawType()).equals(java.lang.ClassGenericsTest3.$Mc3$02.class)) fail("test_3, case 049 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        aa0 = ((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getActualTypeArguments();
        if(aa0.length!=1) fail("test_3, case 050 FAILED: "+aa0.length);
        if(!((Class)aa0[0]).equals(ClassGenericsTest3.class)) fail("test_3, case 051 FAILED: "+(/*(Class)*/aa0[0]));
        if(!((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType().toString().equals("java.lang.ClassGenericsTest3<? super java.lang.Class>")) fail("test_3, case 052 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));
        if(!((Class)((ParameterizedType)((ParameterizedType)((ParameterizedType)oc1).getOwnerType()).getOwnerType()).getRawType()).equals(java.lang.ClassGenericsTest3.class)) fail("test_3, case 053 FAILED: "+((Class)((ParameterizedType)oc1).getRawType()));

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
            ft = java.lang.ClassGenericsTest3.class.getField("f111").getGenericType();
        }catch(NoSuchFieldException e){
            fail("test_3, case 059 FAILED: ");
        }
        tv = (TypeVariable)ft;
        if(!tv.getName().equals("$X$")) fail("test_3, case 060 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest3")) fail("test_3, case 061 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_3, case 062 FAILED: "+((Class)ab[0]).getName());
    }
    
    /**
     * use "$" symbol in identifiers for generalized method
     */
    static class $Mc3$09 extends Throwable implements java.io.Serializable, Cloneable {
        private static final long serialVersionUID = 0L;
    };
    public <U$uUuU_ extends Throwable, TM1, TM2 extends Thread &java.io.Serializable &Cloneable> void foo1For_5(U$uUuU_ a1) throws U$uUuU_, java.io.IOException {}
    public void test_5() {
        Type rt;
        TypeVariable tv;
        Type ap[];
        Type ab[];
        Type aet[];
        Type atp[];
        Method m = null;
        try{
            java.lang.reflect.Method am[] = ClassGenericsTest3.class.getDeclaredMethods();
            for (int i = 0; i < am.length; i++) {
                if (am[i].getName().equals("foo1For_5")) {
                    m = am[i];
                }
            }
        } catch (Exception e) {
            fail("test_5, case 001 FAILED: "+e.toString());
        }
        rt = m.getGenericReturnType();
        if(!((Class)rt).getName().equals("void")) fail("test_5, case 003 FAILED: "+((Class)rt).getName());
        ///////
        atp = m.getTypeParameters();
        if(atp.length!=3) fail("test_5, case 004 FAILED: "+atp.length);
        tv = (TypeVariable)atp[0];
        if(!tv.getName().equals("U$uUuU_")) fail("test_5, case 005 FAILED: "+tv.getName());
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
        if(!tv.getName().equals("U$uUuU_")) fail("test_5, case 020 FAILED: "+tv.getName());
        ///////
        aet = m.getGenericExceptionTypes();
        if(aet.length!=2) fail("test_5, case 021 FAILED: "+ap.length);
        tv = (TypeVariable)aet[0];
        if(!tv.getName().equals("U$uUuU_")) fail("test_5, case 022 FAILED: "+tv.getName());
        if(!((Class)aet[1]).getName().equals("java.io.IOException")) fail("test_5, case 023 FAILED: "+((Class)aet[1]).getName());
  }

    /**
     * use "$" symbol in identifiers for generalized constructor
     */
	class $Mc3$06{
	    public <U$uUuU_ extends Throwable, TM1, TM2 extends Thread &java.io.Serializable &Cloneable &$MI3$01> $Mc3$06(U$uUuU_ a1) throws U$uUuU_, java.io.IOException {}
	}

    public void test_6() {
        TypeVariable tv;
        Type ap[];
        Type ab[];
        Type aet[];
        Type atp[];
        Constructor m = null;
        try{
            m = ClassGenericsTest3.$Mc3$06.class.getConstructor(new Class[]{ClassGenericsTest3.class, Throwable.class});
        } catch (Exception e) {
            fail("test_6, case 001 FAILED: "+e.toString());
        }
        atp = m.getTypeParameters();
        if(atp.length!=3) fail("test_6, case 004 FAILED: "+atp.length);
        tv = (TypeVariable)atp[0];
        if(!tv.getName().equals("U$uUuU_")) fail("test_6, case 005 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest3$$Mc3$06")) fail("test_6, case 006 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 007 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Throwable")) fail("test_6, case 008 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[1];
        if(!tv.getName().equals("TM1")) fail("test_6, case 009 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest3$$Mc3$06")) fail("test_6, case 010 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=1) fail("test_6, case 011 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Object")) fail("test_6, case 012 FAILED: "+((Class)ab[0]).getName());
        tv = (TypeVariable)atp[2];
        if(!tv.getName().equals("TM2")) fail("test_6, case 013 FAILED: "+tv.getName());
        if(!((Constructor)tv.getGenericDeclaration()).getName().equals("java.lang.ClassGenericsTest3$$Mc3$06")) fail("test_6, case 014 FAILED: "+((Constructor)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(ab.length!=4) fail("test_6, case 015 FAILED: "+ab.length);
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_6, case 016 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_6, case 017 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_6, case 018 FAILED: "+((Class)ab[2]).getName());
        if(!((Class)ab[3]).getName().equals("java.lang.ClassGenericsTest3$$MI3$01")) fail("test_6, case 0181 FAILED: "+((Class)ab[2]).getName());
        ///////
        ap = m.getGenericParameterTypes();
        if(ap.length!=1) fail("test_6, case 019 FAILED: "+ap.length);
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("U$uUuU_")) fail("test_6, case 020 FAILED: "+tv.getName());
        ///////
        aet = m.getGenericExceptionTypes();
        if(aet.length!=2) fail("test_6, case 021 FAILED: "+ap.length);
        tv = (TypeVariable)aet[0];
        if(!tv.getName().equals("U$uUuU_")) fail("test_6, case 022 FAILED: "+tv.getName());
        if(!((Class)aet[1]).getName().equals("java.io.IOException")) fail("test_6, case 023 FAILED: "+((Class)aet[1]).getName());
	}
}
