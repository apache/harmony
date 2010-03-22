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

import java.lang.reflect.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import org.apache.harmony.test.TestResources;

import junit.framework.TestCase;

/*
 * Created on May 03-06, 2006
 *
 * This ClassGenericsTest4 class is used to test the Core API  Class, Method, 
 * Constructor classes
 * 
 */

@SuppressWarnings(value={"all"}) public class ClassGenericsTest4<$X$> extends TestCase {
    class $Mc3$01 {};

    interface $MI3$01<$T$0 extends java.io.Serializable> {};
    interface $MI3$02<_T$_1$ extends $MI3$01> {};
    interface $MI3$03<T2$> extends $MI3$01 {};
    interface $MI3$04<T$2> {
        interface $MI3$05<_T$21_, _$T22> {
        };
    };
    public class $Mc3$02<$_T3_$ extends ClassGenericsTest4> {
        public class $Mc3$04<T5 extends ClassGenericsTest4> {
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
/*  //XXX's compiler errors are risen here:
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

    /**
     * processing generalized local classes
     */
    public void test_1()  throws Exception {
		class $Mc3$01<T> {};
		class $Mc3$02<$_T3_$ extends ClassGenericsTest4> {
		    class $Mc3$04<T5 extends $Mc3$01<$Mi4<Cloneable>> &$Mi4$.$$$$$Mi5 &$Mi4$.$$$$Mi5 &$Mi4$.$$$Mi5 &$Mi4$.$$Mi5 &$Mi4$.$Mi5 &$Mi4$.Mi5 &$Mi4$, T6 extends $Mc3$01<$Mi4<? super Cloneable>> &$Mi4$.$$$$$Mi5, T7 extends $Mc3$01<$Mi4<? extends java.io.Serializable>> &$Mi4$.$$$$$Mi5> implements $Mi4$.$$$$$Mi5 ,$Mi4$.$$$$Mi5 ,$Mi4$.$$$Mi5 ,$Mi4$.$$Mi5 ,$Mi4$.$Mi5 ,$Mi4$.Mi5 ,$Mi4$ {
		    };
		};
        if(!$Mc3$01.class.getName().equals("java.lang.ClassGenericsTest4$1$Mc3$01")) fail("test_1, case 001 FAILED: "+$Mc3$01.class.getName());
        if(!$Mc3$01.class.getSimpleName().equals("$Mc3$01")) fail("test_1, case 002 FAILED: "+$Mc3$01.class.getSimpleName());
        if($Mc3$01.class.getCanonicalName() != null) fail("test_1, case 0021 FAILED: "+$Mc3$01.class.getCanonicalName());
		Type at[] = $Mc3$01.class.getTypeParameters();
		TypeVariable tv = (TypeVariable)at[0];
        if(!tv.getName().equals("T")) fail("test_1, case 003 FAILED: "+tv.getName());
		Class cc = (Class)tv.getBounds()[0];
        if(!cc.getName().equals("java.lang.Object")) fail("test_1, case 004 FAILED: "+cc.getName());

        if(!$Mc3$02.class.getName().equals("java.lang.ClassGenericsTest4$1$Mc3$02")) fail("test_1, case 005 FAILED: "+$Mc3$02.class.getName());
        if(!$Mc3$02.class.getSimpleName().equals("$Mc3$02")) fail("test_1, case 006 FAILED: "+$Mc3$02.class.getSimpleName());
        if($Mc3$02.class.getCanonicalName() != null) fail("test_1, case 007 FAILED: "+$Mc3$02.class.getCanonicalName());
		at = $Mc3$02.class.getTypeParameters();
		tv = (TypeVariable)at[0];
        if(!tv.getName().equals("$_T3_$")) fail("test_1, case 008 FAILED: "+tv.getName());
		cc = (Class)tv.getBounds()[0];
        if(!cc.getName().equals("java.lang.ClassGenericsTest4")) fail("test_1, case 009 FAILED: "+cc.getName());

        if(!$Mc3$02.$Mc3$04.class.getName().equals("java.lang.ClassGenericsTest4$1$Mc3$02$$Mc3$04")) fail("test_1, case 010 FAILED: "+$Mc3$02.$Mc3$04.class.getName());
        if(!$Mc3$02.$Mc3$04.class.getSimpleName().equals("$Mc3$04")) fail("test_1, case 011 FAILED: "+$Mc3$02.$Mc3$04.class.getSimpleName());
        if($Mc3$02.$Mc3$04.class.getCanonicalName() != null) fail("test_1, case 012 FAILED: "+$Mc3$02.$Mc3$04.class.getCanonicalName());
		Type ab[] = null;ParameterizedType pt = null;
if(RuntimeAdditionalSupport1.openingFlag) {
try{
		at = $Mc3$02.$Mc3$04.class.getTypeParameters();
		tv = (TypeVariable)at[0];
        if(!tv.getName().equals("T5")) fail("test_1, case 0121 FAILED: "+tv.getName());
		/*Type*/ ab/*[]*/ = tv.getBounds();
		/*ParameterizedType*/ pt = (ParameterizedType)ab[0];
        if(!pt.toString().equals("java.lang.ClassGenericsTest4$1$Mc3$01<java.lang.ClassGenericsTest4.$Mi4<java.lang.Cloneable>>")) fail("test_1, case 013 FAILED: "+tv.getName());

        if(!((Class)((ParameterizedType)ab[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest4$1$Mc3$01")) fail("test_1, case 014 FAILED: "+((Class)((ParameterizedType)ab[0]).getRawType()).getName());
		if(((ParameterizedType)ab[0]).getOwnerType()!=null) fail("test_1, case 015 FAILED: "+((ParameterizedType)ab[0]).getOwnerType());
        if(!((Class)ab[1]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$$$Mi5") || !((Class)ab[1]).getSimpleName().equals("$$$$$Mi5")) fail("test_1, case 016 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[2]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$$Mi5") || !((Class)ab[2]).getSimpleName().equals("$$$$Mi5")) fail("test_1, case 017 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[3]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$Mi5") || !((Class)ab[3]).getSimpleName().equals("$$$Mi5")) fail("test_1, case 018 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[4]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$Mi5") || !((Class)ab[4]).getSimpleName().equals("$$Mi5")) fail("test_1, case 019 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[5]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$Mi5") || !((Class)ab[5]).getSimpleName().equals("$Mi5")) fail("test_1, case 020 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[6]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$Mi5") || !((Class)ab[6]).getSimpleName().equals("Mi5")) fail("test_1, case 021 FAILED: "+((Class)ab[2]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[7]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$")) fail("test_1, case 022 FAILED: "+((Class)ab[7]).getName());

		tv = (TypeVariable)at[1];
        if(!tv.getName().equals("T6")) fail("test_1, case 023 FAILED: "+tv.getName());
		ab = tv.getBounds();
		pt = (ParameterizedType)ab[0];
        if(!pt.toString().equals("java.lang.ClassGenericsTest4$1$Mc3$01<java.lang.ClassGenericsTest4.$Mi4<? super java.lang.Cloneable>>")) fail("test_1, case 024 FAILED: "+tv.getName());

        if(!((Class)((ParameterizedType)ab[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest4$1$Mc3$01")) fail("test_1, case 025 FAILED: "+((Class)((ParameterizedType)ab[0]).getRawType()).getName());
		if(((ParameterizedType)ab[0]).getOwnerType()!=null) fail("test_1, case 026 FAILED: "+((ParameterizedType)ab[0]).getOwnerType());
        if(!((Class)ab[1]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$$$Mi5") || !((Class)ab[1]).getSimpleName().equals("$$$$$Mi5")) fail("test_1, case 027 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());

		tv = (TypeVariable)at[2];
        if(!tv.getName().equals("T7")) fail("test_1, case 028 FAILED: "+tv.getName());
		ab = tv.getBounds();
		pt = (ParameterizedType)ab[0];
        if(!pt.toString().equals("java.lang.ClassGenericsTest4$1$Mc3$01<java.lang.ClassGenericsTest4.$Mi4<? extends java.io.Serializable>>")) fail("test_1, case 029 FAILED: "+tv.getName());

        if(!((Class)((ParameterizedType)ab[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest4$1$Mc3$01")) fail("test_1, case 030 FAILED: "+((Class)((ParameterizedType)ab[0]).getRawType()).getName());
		if(((ParameterizedType)ab[0]).getOwnerType()!=null) fail("test_1, case 031 FAILED: "+((ParameterizedType)ab[0]).getOwnerType());
        if(!((Class)ab[1]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$$$Mi5") || !((Class)ab[1]).getSimpleName().equals("$$$$$Mi5")) fail("test_1, case 032 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());

		at = $Mc3$02.$Mc3$04.class.getGenericInterfaces();

        if(!((Class)at[0]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$$$Mi5") || !((Class)at[0]).getSimpleName().equals("$$$$$Mi5")) fail("test_1, case 033 FAILED: "+((Class)at[0]).getName()+"|"+((Class)at[0]).getSimpleName());
        if(!((Class)at[1]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$$Mi5") || !((Class)at[1]).getSimpleName().equals("$$$$Mi5")) fail("test_1, case 034 FAILED: "+((Class)at[1]).getName()+"|"+((Class)at[1]).getSimpleName());
        if(!((Class)at[2]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$Mi5") || !((Class)at[2]).getSimpleName().equals("$$$Mi5")) fail("test_1, case 035 FAILED: "+((Class)at[2]).getName()+"|"+((Class)at[2]).getSimpleName());
        if(!((Class)at[3]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$Mi5") || !((Class)at[3]).getSimpleName().equals("$$Mi5")) fail("test_1, case 036 FAILED: "+((Class)at[3]).getName()+"|"+((Class)at[3]).getSimpleName());
        if(!((Class)at[4]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$Mi5") || !((Class)at[4]).getSimpleName().equals("$Mi5")) fail("test_1, case 037 FAILED: "+((Class)at[4]).getName()+"|"+((Class)at[4]).getSimpleName());
        if(!((Class)at[5]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$Mi5") || !((Class)at[5]).getSimpleName().equals("Mi5")) fail("test_1, case 038 FAILED: "+((Class)at[5]).getName()+"|"+((Class)at[5]).getSimpleName());
        if(!((Class)at[6]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$")) fail("test_1, case 039 FAILED: "+((Class)at[6]).getName());
} catch (java.lang.reflect.GenericSignatureFormatError e) {System.out.println("ClassGenericsTest4.test_1.XXX_case_1 is excluded temporarily because the used compiler generates incorrect signature which leads to the case falure.");}
}
		Class ac[] = new Class[3];
		meth(ac);
        if(!ac[0].getName().equals("java.lang.ClassGenericsTest4$2$Mc3$01")) fail("test_1, case 040 FAILED: "+ac[0].getName());
        if(!ac[0].getSimpleName().equals("$Mc3$01")) fail("test_1, case 041 FAILED: "+ac[0].getSimpleName());
        if(ac[0].getCanonicalName() != null) fail("test_1, case 042 FAILED: "+ac[0].getCanonicalName());
		at = ac[0].getTypeParameters();
		tv = (TypeVariable)at[0];
        if(!tv.getName().equals("T")) fail("test_1, case 043 FAILED: "+tv.getName());
		cc = (Class)tv.getBounds()[0];
        if(!cc.getName().equals("java.lang.Object")) fail("test_1, case 044 FAILED: "+cc.getName());

        if(!ac[1].getName().equals("java.lang.ClassGenericsTest4$2$Mc3$02")) fail("test_1, case 045 FAILED: "+ac[1].getName());
        if(!ac[1].getSimpleName().equals("$Mc3$02")) fail("test_1, case 046 FAILED: "+ac[1].getSimpleName());
        if(ac[1].getCanonicalName() != null) fail("test_1, case 047 FAILED: "+ac[1].getCanonicalName());
		at = ac[1].getTypeParameters();
		tv = (TypeVariable)at[0];
        if(!tv.getName().equals("$_T3_$")) fail("test_1, case 048 FAILED: "+tv.getName());
		cc = (Class)tv.getBounds()[0];
        if(!cc.getName().equals("java.lang.ClassGenericsTest4")) fail("test_1, case 049 FAILED: "+cc.getName());

        if(!ac[2].getName().equals("java.lang.ClassGenericsTest4$2$Mc3$02$$Mc3$04")) fail("test_1, case 050 FAILED: "+ac[2].getName());
        if(!ac[2].getSimpleName().equals("$Mc3$04")) fail("test_1, case 051 FAILED: "+ac[2].getSimpleName());
        if(ac[2].getCanonicalName() != null) fail("test_1, case 052 FAILED: "+ac[2].getCanonicalName());
if(RuntimeAdditionalSupport1.openingFlag) {
try{
		at = ac[2].getTypeParameters();
		tv = (TypeVariable)at[0];
        if(!tv.getName().equals("T5")) fail("test_1, case 053 FAILED: "+tv.getName());
		ab = tv.getBounds();
		pt = (ParameterizedType)ab[0];
        if(!pt.toString().equals("java.lang.ClassGenericsTest4$2$Mc3$01<java.lang.ClassGenericsTest4.$Mi4<java.lang.Cloneable>>")) fail("test_1, case 054 FAILED: "+tv.getName());

        if(!((Class)((ParameterizedType)ab[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest4$2$Mc3$01")) fail("test_1, case 055 FAILED: "+((Class)((ParameterizedType)ab[0]).getRawType()).getName());
		if(((ParameterizedType)ab[0]).getOwnerType()!=null) fail("test_1, case 056 FAILED: "+((ParameterizedType)ab[0]).getOwnerType());
        if(!((Class)ab[1]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$$$Mi5") || !((Class)ab[1]).getSimpleName().equals("$$$$$Mi5")) fail("test_1, case 057 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[2]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$$Mi5") || !((Class)ab[2]).getSimpleName().equals("$$$$Mi5")) fail("test_1, case 058 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[3]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$Mi5") || !((Class)ab[3]).getSimpleName().equals("$$$Mi5")) fail("test_1, case 059 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[4]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$Mi5") || !((Class)ab[4]).getSimpleName().equals("$$Mi5")) fail("test_1, case 060 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[5]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$Mi5") || !((Class)ab[5]).getSimpleName().equals("$Mi5")) fail("test_1, case 061 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[6]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$Mi5") || !((Class)ab[6]).getSimpleName().equals("Mi5")) fail("test_1, case 062 FAILED: "+((Class)ab[2]).getName()+"|"+((Class)ab[2]).getSimpleName());
        if(!((Class)ab[7]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$")) fail("test_1, case 063 FAILED: "+((Class)ab[7]).getName());

		tv = (TypeVariable)at[1];
        if(!tv.getName().equals("T6")) fail("test_1, case 064 FAILED: "+tv.getName());
		ab = tv.getBounds();
		pt = (ParameterizedType)ab[0];
        if(!pt.toString().equals("java.lang.ClassGenericsTest4$2$Mc3$01<java.lang.ClassGenericsTest4.$Mi4<? super java.lang.Cloneable>>")) fail("test_1, case 065 FAILED: "+tv.getName());

        if(!((Class)((ParameterizedType)ab[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest4$2$Mc3$01")) fail("test_1, case 066 FAILED: "+((Class)((ParameterizedType)ab[0]).getRawType()).getName());
		if(((ParameterizedType)ab[0]).getOwnerType()!=null) fail("test_1, case 067 FAILED: "+((ParameterizedType)ab[0]).getOwnerType());
        if(!((Class)ab[1]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$$$Mi5") || !((Class)ab[1]).getSimpleName().equals("$$$$$Mi5")) fail("test_1, case 068 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());

		tv = (TypeVariable)at[2];
        if(!tv.getName().equals("T7")) fail("test_1, case 069 FAILED: "+tv.getName());
		ab = tv.getBounds();
		pt = (ParameterizedType)ab[0];
        if(!pt.toString().equals("java.lang.ClassGenericsTest4$2$Mc3$01<java.lang.ClassGenericsTest4.$Mi4<? extends java.io.Serializable>>")) fail("test_1, case 070 FAILED: "+tv.getName());

        if(!((Class)((ParameterizedType)ab[0]).getRawType()).getName().equals("java.lang.ClassGenericsTest4$2$Mc3$01")) fail("test_1, case 071 FAILED: "+((Class)((ParameterizedType)ab[0]).getRawType()).getName());
		if(((ParameterizedType)ab[0]).getOwnerType()!=null) fail("test_1, case 072 FAILED: "+((ParameterizedType)ab[0]).getOwnerType());
        if(!((Class)ab[1]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$$$Mi5") || !((Class)ab[1]).getSimpleName().equals("$$$$$Mi5")) fail("test_1, case 073 FAILED: "+((Class)ab[1]).getName()+"|"+((Class)ab[2]).getSimpleName());

		at = ac[2].getGenericInterfaces();

        if(!((Class)at[0]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$$$Mi5") || !((Class)at[0]).getSimpleName().equals("$$$$$Mi5")) fail("test_1, case 074 FAILED: "+((Class)at[0]).getName()+"|"+((Class)at[0]).getSimpleName());
        if(!((Class)at[1]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$$Mi5") || !((Class)at[1]).getSimpleName().equals("$$$$Mi5")) fail("test_1, case 075 FAILED: "+((Class)at[1]).getName()+"|"+((Class)at[1]).getSimpleName());
        if(!((Class)at[2]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$$Mi5") || !((Class)at[2]).getSimpleName().equals("$$$Mi5")) fail("test_1, case 076 FAILED: "+((Class)at[2]).getName()+"|"+((Class)at[2]).getSimpleName());
        if(!((Class)at[3]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$$Mi5") || !((Class)at[3]).getSimpleName().equals("$$Mi5")) fail("test_1, case 077 FAILED: "+((Class)at[3]).getName()+"|"+((Class)at[3]).getSimpleName());
        if(!((Class)at[4]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$$Mi5") || !((Class)at[4]).getSimpleName().equals("$Mi5")) fail("test_1, case 078 FAILED: "+((Class)at[4]).getName()+"|"+((Class)at[4]).getSimpleName());
        if(!((Class)at[5]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$$Mi5") || !((Class)at[5]).getSimpleName().equals("Mi5")) fail("test_1, case 079 FAILED: "+((Class)at[5]).getName()+"|"+((Class)at[5]).getSimpleName());
        if(!((Class)at[6]).getName().equals("java.lang.ClassGenericsTest4$$Mi4$")) fail("test_1, case 080 FAILED: "+((Class)at[6]).getName());
} catch (java.lang.reflect.GenericSignatureFormatError e) {System.out.println("ClassGenericsTest4.test_1.XXX_case_2 is excluded temporarily because the used compiler generates incorrect signature which leads to the case falure.");}
}
    }
    public void meth(Class ac[]) {
		class $Mc3$01<T> {};
		class $Mc3$02<$_T3_$ extends ClassGenericsTest4> {
		    class $Mc3$04<T5 extends $Mc3$01<$Mi4<Cloneable>> &$Mi4$.$$$$$Mi5 &$Mi4$.$$$$Mi5 &$Mi4$.$$$Mi5 &$Mi4$.$$Mi5 &$Mi4$.$Mi5 &$Mi4$.Mi5 &$Mi4$, T6 extends $Mc3$01<$Mi4<? super Cloneable>> &$Mi4$.$$$$$Mi5, T7 extends $Mc3$01<$Mi4<? extends java.io.Serializable>> &$Mi4$.$$$$$Mi5> implements $Mi4$.$$$$$Mi5 ,$Mi4$.$$$$Mi5 ,$Mi4$.$$$Mi5 ,$Mi4$.$$Mi5 ,$Mi4$.$Mi5 ,$Mi4$.Mi5 ,$Mi4$ {
		    };
		};
		ac[0] = $Mc3$01.class;
		ac[1] = $Mc3$02.class;
		ac[2] = $Mc3$02.$Mc3$04.class;
	}

    /**
     * checks for cases when generalized classes have been loaded by another class loader
     */
    public void test_2 () throws Exception {
        Locale locale = Locale.getDefault();
        Locale locale2;
        locale2 = new Locale("*.UTF8");
        if(locale==locale2){
		ClassLoader ld = TestResources.getLoader();
		Class temp = ld.loadClass("org.apache.harmony.lang.generics.TemplateSet");
		Class ac[] = temp.getDeclaredClasses();
        Type ap[];
        TypeVariable tv;
        Type ab[];
        Type ai[];
        Type aa[];
        Type aa2[];
        Type oc;
        WildcardType wc;
        Type aa3[];

		Class Mc003 = null;
		for(int i = 0; i < ac.length; i ++){
			if(ac[i].getName().equals("org.apache.harmony.lang.generics.TemplateSet$Mc003")) {
				Mc003 = ac[i];
			}
		}
        ap = Mc003.getTypeParameters();
        if(ap.length!=1) fail("test_2, case 001 FAILED: "+ap.length);
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("T4")) fail("test_2, case 002 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$Mc003")) fail("test_2, case 003 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_2, case 004 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_2, case 005 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_2, case 006 FAILED: "+((Class)ab[2]).getName());
        //////////////////////////////////////////////////////////////////////////////////////////////
        ai = Mc003.getGenericInterfaces();
        if(ai.length!=3) fail("test_2, case 007 FAILED: "+ai.length);
        if(!((Class)((ParameterizedType)ai[0]).getRawType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$MI002")) fail("test_2, case 008 FAILED: "+((Class)((ParameterizedType)ai[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[0]).getOwnerType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet")) fail("test_2, case 009 FAILED: "+((Class)((ParameterizedType)ai[0]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[0]).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 010 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$MI003")) fail("test_2, case 011 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet")) fail("test_2, case 012 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.io.Serializable
        if(aa2.length!=1) fail("test_2, case 013 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.io.Serializable")) fail("test_2, case 014 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());

        if(!((Class)((ParameterizedType)ai[1]).getRawType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$MI003")) fail("test_2, case 015 FAILED: "+((Class)((ParameterizedType)ai[1]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[1]).getOwnerType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet")) fail("test_2, case 016 FAILED: "+((Class)((ParameterizedType)ai[1]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[1]).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 017 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$MI003")) fail("test_2, case 018 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet")) fail("test_2, case 019 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.lang.Cloneable
        if(aa2.length!=1) fail("test_2, case 020 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.lang.Cloneable")) fail("test_2, case 021 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());

        if(!((Class)((ParameterizedType)ai[2]).getRawType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$MI004$MI005")) fail("test_2, case 022 FAILED: "+((Class)((ParameterizedType)ai[2]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[2]).getOwnerType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$MI004")) fail("test_2, case 023 FAILED: "+((Class)((ParameterizedType)ai[2]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[2]).getActualTypeArguments();
        if(aa.length!=2) fail("test_2, case 002 FAILED: "+aa.length);
        if(!((Class)aa[0]).getName().equals("java.lang.reflect.Type")) fail("test_2, case 024 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)aa[1]).getName().equals("java.lang.reflect.Type")) fail("test_2, case 025 FAILED: "+((Class)((ParameterizedType)aa[1]).getRawType()).getName());
        
        //////////////////////////////////////////////////////////////////////////////////////////////
        if(!(Mc003.getGenericSuperclass() instanceof java.lang.reflect.ParameterizedType)) fail("test_2, case 026 FAILED: "+Mc003.getGenericSuperclass());
        if(!((Class)((ParameterizedType)Mc003.getGenericSuperclass()).getRawType()).getSimpleName().equals("Mc002")) fail("test_2, case 027 FAILED: "+Mc003.getGenericSuperclass());
        if(!((Class)((ParameterizedType)Mc003.getGenericSuperclass()).getRawType()).getAnnotations()[0].annotationType().getSimpleName().equals("igt")) fail("test_2, case 028 FAILED: "+((Class)((ParameterizedType)Mc003.getGenericSuperclass()).getRawType()).getAnnotations()[0].annotationType().getSimpleName());
        aa = ((ParameterizedType)Mc003.getGenericSuperclass()).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 029 FAILED: "+aa.length);
        if(!((Class)aa[0]).getName().equals("org.apache.harmony.lang.generics.TemplateSet")) fail("test_2, case 030 FAILED: "+((Class)aa[0]).getName());
        oc = ((ParameterizedType)Mc003.getGenericSuperclass()).getOwnerType();
        if(!((Class)((ParameterizedType)oc).getRawType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet")) fail("test_2, case 031 FAILED: "+Mc003.getGenericSuperclass());
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
		Class Mc007\u0391 = null;
		for(int i = 0; i < ac.length; i ++){
			if(ac[i].getName().equals("org.apache.harmony.lang.generics.TemplateSet$Mc007\u0391")) {
				Mc007\u0391 = ac[i];
			}
		}
        ap = Mc007\u0391.getTypeParameters();
        if(ap.length!=1) fail("test_2, case 038 FAILED: "+ap.length);
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("T7")) fail("test_2, case 039 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$Mc007\u0391")) fail("test_2, case 040 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("java.lang.Thread")) fail("test_2, case 041 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_2, case 042 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_2, case 043 FAILED: "+((Class)ab[2]).getName());
        //////////////////////////////////////////////////////////////////////////////////////////////
        ai = Mc007\u0391.getGenericInterfaces();
        if(ai.length!=0) fail("test_2, case 001 FAILED: "+ap.length);
        //////////////////////////////////////////////////////////////////////////////////////////////
        if(!(Mc007\u0391.getGenericSuperclass() instanceof java.lang.Object)) fail("test_2, case 044 FAILED: "+Mc007\u0391.getGenericSuperclass());
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
		Class Mc008\u0576\u06C0\u06F10 = null;
		for(int i = 0; i < ac.length; i ++){
			if(ac[i].getName().equals("org.apache.harmony.lang.generics.TemplateSet$Mc008\u0576\u06C0\u06F10")) {
				Mc008\u0576\u06C0\u06F10 = ac[i];
			}
		}
        ap = Mc008\u0576\u06C0\u06F10.getTypeParameters();
        if(ap.length!=1) fail("test_2, case 045 FAILED: "+ap.length);
        tv = (TypeVariable)ap[0];
        if(!tv.getName().equals("T8")) fail("test_2, case 046 FAILED: "+tv.getName());
        if(!((Class)tv.getGenericDeclaration()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$Mc008\u0576\u06C0\u06F10")) fail("test_2, case 047 FAILED: "+((Class)tv.getGenericDeclaration()).getName());
        ab = tv.getBounds();
        if(!((Class)ab[0]).getName().equals("org.apache.harmony.lang.generics.TemplateSet$Mc007\u0391")) fail("test_2, case 048 FAILED: "+((Class)ab[0]).getName());
        if(!((Class)ab[1]).getName().equals("java.io.Serializable")) fail("test_2, case 049 FAILED: "+((Class)ab[1]).getName());
        if(!((Class)ab[2]).getName().equals("java.lang.Cloneable")) fail("test_2, case 050 FAILED: "+((Class)ab[2]).getName());
        //////////////////////////////////////////////////////////////////////////////////////////////
        ai = Mc008\u0576\u06C0\u06F10.getGenericInterfaces();
        if(ai.length!=3) fail("test_2, case 051 FAILED: "+ai.length);
        if(!((Class)((ParameterizedType)ai[0]).getRawType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$MI002")) fail("test_2, case 052 FAILED: "+((Class)((ParameterizedType)ai[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[0]).getOwnerType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet")) fail("test_2, case 053 FAILED: "+((Class)((ParameterizedType)ai[0]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[0]).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 054 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$MI003")) fail("test_2, case 055 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet")) fail("test_2, case 056 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.io.Serializable
        if(aa2.length!=1) fail("test_2, case 057 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.io.Serializable")) fail("test_2, case 058 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());

        if(!((Class)((ParameterizedType)ai[1]).getRawType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$MI003")) fail("test_2, case 059 FAILED: "+((Class)((ParameterizedType)ai[1]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[1]).getOwnerType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet")) fail("test_2, case 060 FAILED: "+((Class)((ParameterizedType)ai[1]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[1]).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 061 FAILED: "+aa.length);
        if(!((Class)((ParameterizedType)aa[0]).getRawType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$MI003")) fail("test_2, case 062 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)((ParameterizedType)aa[0]).getOwnerType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet")) fail("test_2, case 063 FAILED: "+((Class)((ParameterizedType)aa[0]).getOwnerType()).getName());
        aa2 = ((ParameterizedType)aa[0]).getActualTypeArguments(); //java.lang.Cloneable
        if(aa2.length!=1) fail("test_2, case 064 FAILED: "+aa.length);
        if(!((Class)aa2[0]).getName().equals("java.lang.Cloneable")) fail("test_2, case 065 FAILED: "+((Class)((ParameterizedType)aa2[0]).getRawType()).getName());

        if(!((Class)((ParameterizedType)ai[2]).getRawType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$MI004$MI005")) fail("test_2, case 066 FAILED: "+((Class)((ParameterizedType)ai[2]).getRawType()).getName());
        if(!((Class)((ParameterizedType)ai[2]).getOwnerType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$MI004")) fail("test_2, case 067 FAILED: "+((Class)((ParameterizedType)ai[2]).getOwnerType()).getName());
        aa = ((ParameterizedType)ai[2]).getActualTypeArguments();
        if(aa.length!=2) fail("test_2, case 068 FAILED: "+aa.length);
        if(!((Class)aa[0]).getName().equals("org.apache.harmony.lang.generics.TemplateSet$Mc007\u0391")) fail("test_2, case 069 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        if(!((Class)aa[1]).getName().equals("java.lang.reflect.Type")) fail("test_2, case 070 FAILED: "+((Class)((ParameterizedType)aa[1]).getRawType()).getName());
        
        //////////////////////////////////////////////////////////////////////////////////////////////
        if(!(Mc008\u0576\u06C0\u06F10.getGenericSuperclass() instanceof java.lang.reflect.ParameterizedType)) fail("test_2, case 071 FAILED: "+Mc008\u0576\u06C0\u06F10.getGenericSuperclass());
        if(!((Class)((ParameterizedType)Mc008\u0576\u06C0\u06F10.getGenericSuperclass()).getRawType()).getSimpleName().equals("Mc002")) fail("test_2, case 072 FAILED: "+((Class)((ParameterizedType)Mc008\u0576\u06C0\u06F10.getGenericSuperclass()).getRawType()));
        if(!((Class)((ParameterizedType)Mc008\u0576\u06C0\u06F10.getGenericSuperclass()).getRawType()).getAnnotations()[0].annotationType().getSimpleName().equals("igt")) fail("test_2, case 073 FAILED: "+((Class)((ParameterizedType)Mc008\u0576\u06C0\u06F10.getGenericSuperclass()).getRawType()).getAnnotations()[0].annotationType().getSimpleName());
        aa = ((ParameterizedType)Mc008\u0576\u06C0\u06F10.getGenericSuperclass()).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 074 FAILED: "+aa.length);
        if(!((Class)aa[0]).getName().equals("org.apache.harmony.lang.generics.TemplateSet")) fail("test_2, case 075 FAILED: "+((Class)((ParameterizedType)aa[0]).getRawType()).getName());
        oc = ((ParameterizedType)Mc008\u0576\u06C0\u06F10.getGenericSuperclass()).getOwnerType();
        if(!((Class)((ParameterizedType)oc).getRawType()).getName().equals("org.apache.harmony.lang.generics.TemplateSet")) fail("test_2, case 076 FAILED: "+((Class)((ParameterizedType)oc).getRawType()).getName());
        if(((ParameterizedType)oc).getOwnerType()!=null) fail("test_2, case 077 FAILED: "+((ParameterizedType)oc).getOwnerType());
        aa = ((ParameterizedType)oc).getActualTypeArguments();
        if(aa.length!=1) fail("test_2, case 078 FAILED: "+aa.length);
        wc = (WildcardType)aa[0];
        aa3 = wc.getLowerBounds();
        if(aa3.length!=1) fail("test_2, case 079 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Mc007\u0391)) fail("test_2, case 080 FAILED: "+((Class)aa3[0]));
        aa3 = wc.getUpperBounds();
        if(aa3.length!=1) fail("test_2, case 081 FAILED: "+aa3.length);
        if(!((Class)aa3[0]).equals(Object.class)) fail("test_2, case 082 FAILED: "+((Class)aa3[0]));
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
		Class MC011\u0576\u06C0\u06F10 = null;
		for(int i = 0; i < ac.length; i ++){
			if(ac[i].getName().equals("org.apache.harmony.lang.generics.TemplateSet$MC011\u0576\u06C0\u06F10")) {
				MC011\u0576\u06C0\u06F10 = ac[i];
			}
		}
	    ap = MC011\u0576\u06C0\u06F10.getTypeParameters();
		if(ap.length!=0) fail("test_2, case 083 FAILED: "+ap.length);
        //////////////////////////////////////////////////////////////////////////////////////////////
		ai = MC011\u0576\u06C0\u06F10.getGenericInterfaces();
		if(ai.length!=1) fail("test_2, case 084 FAILED: "+ai.length);
		if(!((Class)ai[0]).getName().equals("org.apache.harmony.lang.generics.TemplateSet$MI010\u0576\u06C0\u06F10")) fail("test_2, case 085 FAILED: "+((Class)ai[0]).getName());
        //////////////////////////////////////////////////////////////////////////////////////////////
		if(!((Class)MC011\u0576\u06C0\u06F10.getGenericSuperclass()).getName().equals("org.apache.harmony.lang.generics.TemplateSet$Mc010\u0576\u06C0\u06F10")) fail("test_2, case 086 FAILED: "+((Class)Mc008\u0576\u06C0\u06F10.getGenericSuperclass()).getName());
        }	
}
    /**
     * spoiled signature
     */
    public void test_3 () throws Exception {

		final String RESOURCE_PATH = "test.resource.path";
		ClassLoader ld = TestResources.getLoader();

        String path = System.getProperty(RESOURCE_PATH);

		/*
		 * package org.apache.harmony.lang.generics;
		 * import java.lang.reflect.TypeVariable;
		 * public class BadSignatureTemplate<$X$> {
		 * 
		 *     public TypeVariable[] test_1()  throws Exception {
		 * 		class $Mc3$01<T> {};
		 * 		class $Mc3$02<$_T3_$ extends Class> {
		 * 		    class $Mc3$04<T5 extends $Mc3$01<Cloneable>> {
		 * 		    };
		 * 		};
		 * 		return $Mc3$02.$Mc3$04.class.getTypeParameters();
		 * 	}
		 * }
		 */
		/*
		 * package org.apache.harmony.lang.generics;
		 * import junit.framework.TestCase;
		 * public class BadSignatureTemplate<$X$> extends TestCase {
		 *     public void test_1()  throws Exception {
		 * 		class $Mc3$01<T> {};
		 * 		class $Mc3$02<$_T3_$ extends Class> {
		 * 		    class $Mc3$04<T5 extends $Mc3$01<Cloneable>> {
		 * 		    };
		 * 		};
		 * 		$Mc3$02.$Mc3$04.class.getTypeParameters();
		 * 	}
		 * }
		 */
		byte BadSignatureTemplate[] = {
		/**/
		-54,-2,-70,-66,0,0,0,49,0,32,10,0,5,0,18,7,0,20,10,0,24,0,25,7,0,
		26,7,0,27,1,0,6,60,105,110,105,116,62,1,0,3,40,41,86,1,0,4,67,111,100,
		101,1,0,15,76,105,110,101,78,117,109,98,101,114,84,97,98,108,101,1,0,6,116,101,115,
		116,95,49,1,0,35,40,41,91,76,106,97,118,97,47,108,97,110,103,47,114,101,102,108,101,
		99,116,47,84,121,112,101,86,97,114,105,97,98,108,101,59,1,0,10,69,120,99,101,112,116,
		105,111,110,115,7,0,28,1,0,9,83,105,103,110,97,116,117,114,101,1,0,42,60,36,88,
		36,58,76,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,59,62,76,106,97,118,
		97,47,108,97,110,103,47,79,98,106,101,99,116,59,1,0,10,83,111,117,114,99,101,70,105,
		108,101,1,0,25,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,
		46,106,97,118,97,12,0,6,0,7,7,0,29,1,0,70,111,114,103,47,97,112,97,99,104,
		101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,66,
		97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,36,49,36,77,99,51,
		36,48,50,36,36,77,99,51,36,48,52,1,0,7,36,77,99,51,36,48,50,1,0,12,73,
		110,110,101,114,67,108,97,115,115,101,115,1,0,7,36,77,99,51,36,48,52,7,0,30,12,
		0,31,0,11,1,0,53,111,114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,
		47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,
		114,101,84,101,109,112,108,97,116,101,1,0,16,106,97,118,97,47,108,97,110,103,47,79,98,
		106,101,99,116,1,0,19,106,97,118,97,47,108,97,110,103,47,69,120,99,101,112,116,105,111,
		110,1,0,62,111,114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,
		110,103,47,103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,
		101,109,112,108,97,116,101,36,49,36,77,99,51,36,48,50,1,0,15,106,97,118,97,47,108,
		97,110,103,47,67,108,97,115,115,1,0,17,103,101,116,84,121,112,101,80,97,114,97,109,101,
		116,101,114,115,0,33,0,4,0,5,0,0,0,0,0,2,0,1,0,6,0,7,0,1,0,
		8,0,0,0,29,0,1,0,1,0,0,0,5,42,-73,0,1,-79,0,0,0,1,0,9,0,
		0,0,6,0,1,0,0,0,5,0,1,0,10,0,11,0,2,0,8,0,0,0,31,0,1,
		0,1,0,0,0,7,19,0,2,-74,0,3,-80,0,0,0,1,0,9,0,0,0,6,0,1,
		0,0,0,13,0,12,0,0,0,4,0,1,0,13,0,3,0,14,0,0,0,2,0,15,0,
		16,0,0,0,2,0,17,0,22,0,0,0,18,0,2,0,19,0,0,0,21,0,0,0,2,
		0,19,0,23,0,0,
		/**/
		/*
		-54,-2,-70,-66,0,0,0,49,0,32,10,0,5,0,17,7,0,19,10,0,23,0,24,7,0,
		25,7,0,26,1,0,6,60,105,110,105,116,62,1,0,3,40,41,86,1,0,4,67,111,100,
		101,1,0,15,76,105,110,101,78,117,109,98,101,114,84,97,98,108,101,1,0,6,116,101,115,
		116,95,49,1,0,10,69,120,99,101,112,116,105,111,110,115,7,0,27,1,0,9,83,105,103,
		110,97,116,117,114,101,1,0,50,60,36,88,36,58,76,106,97,118,97,47,108,97,110,103,47,
		79,98,106,101,99,116,59,62,76,106,117,110,105,116,47,102,114,97,109,101,119,111,114,107,47,
		84,101,115,116,67,97,115,101,59,1,0,10,83,111,117,114,99,101,70,105,108,101,1,0,25,
		66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,46,106,97,118,97,
		12,0,6,0,7,7,0,28,1,0,70,111,114,103,47,97,112,97,99,104,101,47,104,97,114,
		109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,
		110,97,116,117,114,101,84,101,109,112,108,97,116,101,36,49,36,77,99,51,36,48,50,36,36,
		77,99,51,36,48,52,1,0,7,36,77,99,51,36,48,50,1,0,12,73,110,110,101,114,67,
		108,97,115,115,101,115,1,0,7,36,77,99,51,36,48,52,7,0,29,12,0,30,0,31,1,
		0,53,111,114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,
		47,103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,
		112,108,97,116,101,1,0,24,106,117,110,105,116,47,102,114,97,109,101,119,111,114,107,47,84,
		101,115,116,67,97,115,101,1,0,19,106,97,118,97,47,108,97,110,103,47,69,120,99,101,112,
		116,105,111,110,1,0,62,111,114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,
		47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,
		114,101,84,101,109,112,108,97,116,101,36,49,36,77,99,51,36,48,50,1,0,15,106,97,118,
		97,47,108,97,110,103,47,67,108,97,115,115,1,0,17,103,101,116,84,121,112,101,80,97,114,
		97,109,101,116,101,114,115,1,0,35,40,41,91,76,106,97,118,97,47,108,97,110,103,47,114,
		101,102,108,101,99,116,47,84,121,112,101,86,97,114,105,97,98,108,101,59,0,33,0,4,0,
		5,0,0,0,0,0,2,0,1,0,6,0,7,0,1,0,8,0,0,0,29,0,1,0,1,
		0,0,0,5,42,-73,0,1,-79,0,0,0,1,0,9,0,0,0,6,0,1,0,0,0,6,
		0,1,0,10,0,7,0,2,0,8,0,0,0,36,0,1,0,1,0,0,0,8,19,0,2,
		-74,0,3,87,-79,0,0,0,1,0,9,0,0,0,10,0,2,0,0,0,16,0,7,0,17,
		0,11,0,0,0,4,0,1,0,12,0,3,0,13,0,0,0,2,0,14,0,15,0,0,0,
		2,0,16,0,21,0,0,0,18,0,2,0,18,0,0,0,20,0,0,0,2,0,18,0,22,
		0,0,
		*/
		};

byte BadSignatureTemplate$1$Mc3$02[] = {
		/*
		-54,-2,-70,-66,0,0,0,49,0,31,9,0,3,0,22,10,0,4,0,23,7,0,24,7,0,
		25,1,0,7,36,77,99,51,36,48,50,1,0,12,73,110,110,101,114,67,108,97,115,115,101,
		115,7,0,26,1,0,7,36,77,99,51,36,48,52,1,0,6,116,104,105,115,36,48,1,0,
		55,76,111,114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,
		47,103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,
		112,108,97,116,101,59,1,0,6,60,105,110,105,116,62,1,0,58,40,76,111,114,103,47,97,
		112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,
		99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,59,41,
		86,1,0,4,67,111,100,101,1,0,15,76,105,110,101,78,117,109,98,101,114,84,97,98,108,
		101,1,0,9,83,105,103,110,97,116,117,114,101,1,0,44,60,36,95,84,51,95,36,58,76,
		106,97,118,97,47,108,97,110,103,47,67,108,97,115,115,59,62,76,106,97,118,97,47,108,97,
		110,103,47,79,98,106,101,99,116,59,1,0,10,83,111,117,114,99,101,70,105,108,101,1,0,
		25,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,46,106,97,118,
		97,1,0,15,69,110,99,108,111,115,105,110,103,77,101,116,104,111,100,7,0,27,12,0,28,
		0,29,12,0,9,0,10,12,0,11,0,30,1,0,62,111,114,103,47,97,112,97,99,104,101,
		47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,66,97,
		100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,36,49,36,77,99,51,36,
		48,50,1,0,16,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,1,0,70,111,
		114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,
		110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,
		116,101,36,49,36,77,99,51,36,48,50,36,36,77,99,51,36,48,52,1,0,53,111,114,103,
		47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,
		114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,
		1,0,6,116,101,115,116,95,49,1,0,35,40,41,91,76,106,97,118,97,47,108,97,110,103,
		47,114,101,102,108,101,99,116,47,84,121,112,101,86,97,114,105,97,98,108,101,59,1,0,3,
		40,41,86,0,32,0,3,0,4,0,0,0,1,16,16,0,9,0,10,0,0,0,1,0,0,
		0,11,0,12,0,1,0,13,0,0,0,38,0,2,0,2,0,0,0,10,42,43,-75,0,1,
		42,-73,0,2,-79,0,0,0,1,0,14,0,0,0,10,0,2,0,0,0,9,0,9,0,11,
		0,4,0,15,0,0,0,2,0,16,0,17,0,0,0,2,0,18,0,19,0,0,0,4,0,
		20,0,21,0,6,0,0,0,18,0,2,0,3,0,0,0,5,0,0,0,7,0,3,0,8,
		0,0,
		*/
		/**/
		-54,-2,-70,-66,0,0,0,49,0,30,9,0,3,0,22,10,0,4,0,23,7,0,24,7,0,
		25,1,0,7,36,77,99,51,36,48,50,1,0,12,73,110,110,101,114,67,108,97,115,115,101,
		115,7,0,26,1,0,7,36,77,99,51,36,48,52,1,0,6,116,104,105,115,36,48,1,0,
		55,76,111,114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,
		47,103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,
		112,108,97,116,101,59,1,0,6,60,105,110,105,116,62,1,0,58,40,76,111,114,103,47,97,
		112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,
		99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,59,41,
		86,1,0,4,67,111,100,101,1,0,15,76,105,110,101,78,117,109,98,101,114,84,97,98,108,
		101,1,0,9,83,105,103,110,97,116,117,114,101,1,0,44,60,36,95,84,51,95,36,58,76,
		106,97,118,97,47,108,97,110,103,47,67,108,97,115,115,59,62,76,106,97,118,97,47,108,97,
		110,103,47,79,98,106,101,99,116,59,1,0,10,83,111,117,114,99,101,70,105,108,101,1,0,
		25,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,46,106,97,118,
		97,1,0,15,69,110,99,108,111,115,105,110,103,77,101,116,104,111,100,7,0,27,12,0,28,
		0,29,12,0,9,0,10,12,0,11,0,29,1,0,62,111,114,103,47,97,112,97,99,104,101,
		47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,66,97,
		100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,36,49,36,77,99,51,36,
		48,50,1,0,16,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,1,0,70,111,
		114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,
		110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,
		116,101,36,49,36,77,99,51,36,48,50,36,36,77,99,51,36,48,52,1,0,53,111,114,103,
		47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,
		114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,
		1,0,6,116,101,115,116,95,49,1,0,3,40,41,86,0,32,0,3,0,4,0,0,0,1,
		16,16,0,9,0,10,0,0,0,1,0,0,0,11,0,12,0,1,0,13,0,0,0,38,0,
		2,0,2,0,0,0,10,42,43,-75,0,1,42,-73,0,2,-79,0,0,0,1,0,14,0,0,
		0,10,0,2,0,0,0,11,0,9,0,13,0,4,0,15,0,0,0,2,0,16,0,17,0,
		0,0,2,0,18,0,19,0,0,0,4,0,20,0,21,0,6,0,0,0,18,0,2,0,3,
		0,0,0,5,0,0,0,7,0,3,0,8,0,0,
		/**/
		};

		byte BadSignatureTemplate$1$Mc3$02$$Mc3$04[] = {
		/*
		-54,-2,-70,-66,0,0,0,49,0,28,9,0,3,0,20,10,0,4,0,21,7,0,22,7,0,
		24,1,0,6,116,104,105,115,36,49,7,0,25,1,0,7,36,77,99,51,36,48,50,1,0,
		12,73,110,110,101,114,67,108,97,115,115,101,115,1,0,64,76,111,114,103,47,97,112,97,99,
		104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,
		66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,36,49,36,77,99,
		51,36,48,50,59,1,0,6,60,105,110,105,116,62,1,0,67,40,76,111,114,103,47,97,112,
		97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,99,
		115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,36,49,36,
		77,99,51,36,48,50,59,41,86,1,0,4,67,111,100,101,1,0,15,76,105,110,101,78,117,
		109,98,101,114,84,97,98,108,101,1,0,9,83,105,103,110,97,116,117,114,101,7,0,26,1,
		0,7,36,77,99,51,36,48,49,1,0,110,60,84,53,58,76,111,114,103,47,97,112,97,99,
		104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,
		66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,46,49,36,77,99,
		51,36,48,49,60,76,106,97,118,97,47,108,97,110,103,47,67,108,111,110,101,97,98,108,101,
		59,62,59,62,76,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,59,1,0,10,
		83,111,117,114,99,101,70,105,108,101,1,0,25,66,97,100,83,105,103,110,97,116,117,114,101,
		84,101,109,112,108,97,116,101,46,106,97,118,97,12,0,5,0,9,12,0,10,0,27,1,0,
		70,111,114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,
		103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,
		108,97,116,101,36,49,36,77,99,51,36,48,50,36,36,77,99,51,36,48,52,1,0,7,36,
		77,99,51,36,48,52,1,0,16,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,
		1,0,62,111,114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,
		103,47,103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,
		109,112,108,97,116,101,36,49,36,77,99,51,36,48,50,1,0,62,111,114,103,47,97,112,97,
		99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,99,115,
		47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,36,49,36,77,
		99,51,36,48,49,1,0,3,40,41,86,0,32,0,3,0,4,0,0,0,1,16,16,0,5,
		0,9,0,0,0,1,0,0,0,10,0,11,0,1,0,12,0,0,0,34,0,2,0,2,0,
		0,0,10,42,43,-75,0,1,42,-73,0,2,-79,0,0,0,1,0,13,0,0,0,6,0,1,
		0,0,0,10,0,3,0,14,0,0,0,2,0,17,0,18,0,0,0,2,0,19,0,8,0,
		0,0,26,0,3,0,6,0,0,0,7,0,0,0,15,0,0,0,16,0,0,0,3,0,6,
		0,23,0,0,
		*/
		/**/
		-54,-2,-70,-66,0,0,0,49,0,28,9,0,3,0,20,10,0,4,0,21,7,0,22,7,0,
		24,1,0,6,116,104,105,115,36,49,7,0,25,1,0,7,36,77,99,51,36,48,50,1,0,
		12,73,110,110,101,114,67,108,97,115,115,101,115,1,0,64,76,111,114,103,47,97,112,97,99,
		104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,
				66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,36,49,36,77,99,
		51,36,48,50,59,1,0,6,60,105,110,105,116,62,1,0,67,40,76,111,114,103,47,97,112,
		97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,99,
		115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,36,49,36,
		77,99,51,36,48,50,59,41,86,1,0,4,67,111,100,101,1,0,15,76,105,110,101,78,117,
		109,98,101,114,84,97,98,108,101,1,0,9,83,105,103,110,97,116,117,114,101,7,0,26,1,
		0,7,36,77,99,51,36,48,49,1,0,110,60,84,53,58,76,111,114,103,47,97,112,97,99,
		104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,
		66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,46,49,36,77,99,
		51,36,48,49,60,76,106,97,118,97,47,108,97,110,103,47,67,108,111,110,101,97,98,108,101,
		59,62,59,62,76,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,59,1,0,10,
		83,111,117,114,99,101,70,105,108,101,1,0,25,66,97,100,83,105,103,110,97,116,117,114,101,
		84,101,109,112,108,97,116,101,46,106,97,118,97,12,0,5,0,9,12,0,10,0,27,1,0,
		70,111,114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,
		103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,
		108,97,116,101,36,49,36,77,99,51,36,48,50,36,36,77,99,51,36,48,52,1,0,7,36,
		77,99,51,36,48,52,1,0,16,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,
		1,0,62,111,114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,
		103,47,103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,
		109,112,108,97,116,101,36,49,36,77,99,51,36,48,50,1,0,62,111,114,103,47,97,112,97,
		99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,99,115,
		47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,36,49,36,77,
		99,51,36,48,49,1,0,3,40,41,86,0,32,0,3,0,4,0,0,0,1,16,16,0,5,
		0,9,0,0,0,1,0,0,0,10,0,11,0,1,0,12,0,0,0,34,0,2,0,2,0,
		0,0,10,42,43,-75,0,1,42,-73,0,2,-79,0,0,0,1,0,13,0,0,0,6,0,1,
		0,0,0,12,0,3,0,14,0,0,0,2,0,17,0,18,0,0,0,2,0,19,0,8,0,
		0,0,26,0,3,0,6,0,0,0,7,0,0,0,15,0,0,0,16,0,0,0,3,0,6,
		0,23,0,0,
		/**/
		};

		byte BadSignatureTemplate$1$Mc3$01[] = {
		/*
		-54,-2,-70,-66,0,0,0,49,0,28,9,0,3,0,18,10,0,4,0,19,7,0,20,7,0,
		23,1,0,6,116,104,105,115,36,48,1,0,55,76,111,114,103,47,97,112,97,99,104,101,47,
		104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,66,97,100,
		83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,59,1,0,6,60,105,110,105,
		116,62,1,0,58,40,76,111,114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,
		47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,
		114,101,84,101,109,112,108,97,116,101,59,41,86,1,0,4,67,111,100,101,1,0,15,76,105,
		110,101,78,117,109,98,101,114,84,97,98,108,101,1,0,9,83,105,103,110,97,116,117,114,101,
		1,0,40,60,84,58,76,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,59,62,
		76,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,59,1,0,10,83,111,117,114,
		99,101,70,105,108,101,1,0,25,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,
		108,97,116,101,46,106,97,118,97,1,0,15,69,110,99,108,111,115,105,110,103,77,101,116,104,
		111,100,7,0,24,12,0,25,0,26,12,0,5,0,6,12,0,7,0,27,1,0,62,111,114,
		103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,
		101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,
		101,36,49,36,77,99,51,36,48,49,1,0,7,36,77,99,51,36,48,49,1,0,12,73,110,
		110,101,114,67,108,97,115,115,101,115,1,0,16,106,97,118,97,47,108,97,110,103,47,79,98,
		106,101,99,116,1,0,53,111,114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,
		47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,
		114,101,84,101,109,112,108,97,116,101,1,0,6,116,101,115,116,95,49,1,0,35,40,41,91,
		76,106,97,118,97,47,108,97,110,103,47,114,101,102,108,101,99,116,47,84,121,112,101,86,97,
		114,105,97,98,108,101,59,1,0,3,40,41,86,0,32,0,3,0,4,0,0,0,1,16,16,
		0,5,0,6,0,0,0,1,0,0,0,7,0,8,0,1,0,9,0,0,0,34,0,2,0,
		2,0,0,0,10,42,43,-75,0,1,42,-73,0,2,-79,0,0,0,1,0,10,0,0,0,6,
		0,1,0,0,0,8,0,4,0,11,0,0,0,2,0,12,0,13,0,0,0,2,0,14,0,
		15,0,0,0,4,0,16,0,17,0,22,0,0,0,10,0,1,0,3,0,0,0,21,0,0,
		*/
		/**/
		-54,-2,-70,-66,0,0,0,49,0,27,9,0,3,0,18,10,0,4,0,19,7,0,20,7,0,
		23,1,0,6,116,104,105,115,36,48,1,0,55,76,111,114,103,47,97,112,97,99,104,101,47,
		104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,66,97,100,
		83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,101,59,1,0,6,60,105,110,105,
		116,62,1,0,58,40,76,111,114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,
		47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,
		114,101,84,101,109,112,108,97,116,101,59,41,86,1,0,4,67,111,100,101,1,0,15,76,105,
		110,101,78,117,109,98,101,114,84,97,98,108,101,1,0,9,83,105,103,110,97,116,117,114,101,
		1,0,40,60,84,58,76,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,59,62,
		76,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,59,1,0,10,83,111,117,114,
		99,101,70,105,108,101,1,0,25,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,
		108,97,116,101,46,106,97,118,97,1,0,15,69,110,99,108,111,115,105,110,103,77,101,116,104,
		111,100,7,0,24,12,0,25,0,26,12,0,5,0,6,12,0,7,0,26,1,0,62,111,114,
		103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,47,108,97,110,103,47,103,101,110,
		101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,114,101,84,101,109,112,108,97,116,
		101,36,49,36,77,99,51,36,48,49,1,0,7,36,77,99,51,36,48,49,1,0,12,73,110,
		110,101,114,67,108,97,115,115,101,115,1,0,16,106,97,118,97,47,108,97,110,103,47,79,98,
		106,101,99,116,1,0,53,111,114,103,47,97,112,97,99,104,101,47,104,97,114,109,111,110,121,
		47,108,97,110,103,47,103,101,110,101,114,105,99,115,47,66,97,100,83,105,103,110,97,116,117,
		114,101,84,101,109,112,108,97,116,101,1,0,6,116,101,115,116,95,49,1,0,3,40,41,86,
		0,32,0,3,0,4,0,0,0,1,16,16,0,5,0,6,0,0,0,1,0,0,0,7,0,
		8,0,1,0,9,0,0,0,34,0,2,0,2,0,0,0,10,42,43,-75,0,1,42,-73,0,
		2,-79,0,0,0,1,0,10,0,0,0,6,0,1,0,0,0,10,0,4,0,11,0,0,0,
		2,0,12,0,13,0,0,0,2,0,14,0,15,0,0,0,4,0,16,0,17,0,22,0,0,
		0,10,0,1,0,3,0,0,0,21,0,0,
		/**/
		};
		path = path+File.separator+"org"+File.separator+"apache"+File.separator+"harmony"+File.separator+"lang"+File.separator+"generics";
        File f0 = new File(path);
		f0.mkdir();
        File f2 = new File(path+File.separator+"BadSignatureTemplate.class");
		f2.createNewFile();
		System.out.println(path+File.separator+"BadSignatureTemplate.class");
		FileOutputStream fos = new FileOutputStream(f2);
		fos.write(BadSignatureTemplate);
		fos.flush();
		fos.close();
        f2 = new File(path+File.separator+"BadSignatureTemplate$1$Mc3$02.class");
		f2.createNewFile();
		fos = new FileOutputStream(f2);
		fos.write(BadSignatureTemplate$1$Mc3$02);
		fos.flush();
		fos.close();
        f2 = new File(path+File.separator+"BadSignatureTemplate$1$Mc3$02$$Mc3$04.class");
		f2.createNewFile();
		fos = new FileOutputStream(f2);
		fos.write(BadSignatureTemplate$1$Mc3$02$$Mc3$04);
		fos.flush();
		fos.close();
        f2 = new File(path+File.separator+"BadSignatureTemplate$1$Mc3$01.class");
		f2.createNewFile();
		fos = new FileOutputStream(f2);
		fos.write(BadSignatureTemplate$1$Mc3$01);
		fos.flush();
		fos.close();

		Class temp = ld.loadClass("org.apache.harmony.lang.generics.BadSignatureTemplate");
		/*Class ac[] = */temp.getDeclaredClasses();
		

        Method m = null;
        try{
            java.lang.reflect.Method am[] = temp.getDeclaredMethods();
            for (int ii = 0; ii < am.length; ii++) {
                if (am[ii].getName().equals("test_1")) {
                    m = am[ii];
                }
            }
			m.getName();
        } catch (Exception e) {
            fail("test_3, case 001 FAILED: "+e.toString());
        }
        try{
			Object o = temp.newInstance();
            Object o2 = o.getClass().getDeclaredMethod("test_1").invoke(o, (Object[])null);
			fail("test_3, case 002 FAILED: GenericSignatureFormatError should be risen.");
			System.out.println(((TypeVariable[])o2)[0].getName());
			System.out.println(((ParameterizedType)((TypeVariable[])o2)[0].getBounds()[0]).toString());
        } catch (java.lang.reflect.InvocationTargetException e) {
			if (!(e.getCause() instanceof java.lang.reflect.GenericSignatureFormatError)) {
				fail("test_3, case 003 FAILED: GenericSignatureFormatError should be risen.");
			}
        }	
	}
}
