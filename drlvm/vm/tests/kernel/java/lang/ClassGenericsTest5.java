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

import junit.framework.TestCase;

/*
 * Created on June 03, 2006
 *
 * This ClassGenericsTest5 class is used to test the Core API  Class, Method, 
 * Field, Constructor classes
 * 
 */

/**
 * ###############################################################################
 * ###############################################################################
 * TODO: 1. Do clear should I use "$" instead of "." in the commented fragments?
 * ###############################################################################
 * ###############################################################################
 */

public class ClassGenericsTest5/*<X>*/ extends TestCase {
    
    static class B1 {
        public Object foo() { return null;}
        public Object bar(int i, Object... o) { return null;}
        public Object bar2(int i, Object[][][] o) { return null;}
        public Object bar3(int i, int[][][] ai1, int... ai2) { return null;}
        public <X> Object bar4(int i, int[][][] ai1, OneParamType<Void[][][]>[] ai00, OneParamType<X[][][]> ai0, Object... ai2) { return null;}
        public static void main1(String... s){}
        public static void main2(String[] s){}
    }

    static class B2 extends B1 {
        public String foo() { return "";}
        public String bar(int i, Object... o) { return "";}
        public String bar2(int i, Object[][][] o) { return "";}
        public Object bar3(int i, int[][][] ai1, int... ai2) { return null;}
    }

	static abstract class OneParamType<X> {
	}
    static abstract class C {
        protected abstract <T extends Throwable> 
        OneParamType<? extends T> go(T t) throws T, Error;
        protected abstract <T extends Throwable> 
        OneParamType<? extends String> go2(T t) throws T, Error;
    }
    
    static abstract strictfp class CC {
        protected  <T extends Throwable & Comparable<Throwable>> CC(T num, OneParamType<? super T> l) throws T, Error {}
    }

    /**
     * few checks for signatured methods
     */
    public void test_1() throws Exception {
        //if(!C.class.getDeclaredMethod("go2",Throwable.class).toGenericString().equals("protected abstract <T> java.lang.ClassGenericsTest5$OneParamType<? extends java.lang.String> java.lang.ClassGenericsTest5$C.go2(T) throws T,java.lang.Error")) 
		//	fail("test_1, case 001 FAILED: "+C.class.getDeclaredMethod("go2",Throwable.class).toGenericString());
        if(!C.class.getDeclaredMethod("go2",Throwable.class).toGenericString().equals("protected abstract <T> java.lang.ClassGenericsTest5.OneParamType<? extends java.lang.String> java.lang.ClassGenericsTest5$C.go2(T) throws T,java.lang.Error")) 
			fail("test_1, case 001 FAILED: "+C.class.getDeclaredMethod("go2",Throwable.class).toGenericString());

        if(!C.class.getDeclaredMethod("go2",Throwable.class).getTypeParameters()[0].toString().equals("T")) 
			fail("test_1, case 002 FAILED: "+C.class.getDeclaredMethod("go2",Throwable.class).getTypeParameters()[0].toString());
		
        if(!B2.class.getDeclaredMethod("bar",int.class, Object[].class).toGenericString().equals("public java.lang.String java.lang.ClassGenericsTest5$B2.bar(int,java.lang.Object[])")) 
			fail("test_1, case 003 FAILED: "+B2.class.getDeclaredMethod("bar",int.class, Object[].class).toGenericString());
		
        if(!B2.class.getDeclaredMethod("bar",int.class, Object[].class).getParameterTypes()[1].toString().equals("class [Ljava.lang.Object;")) 
			fail("test_1, case 004 FAILED: "+B2.class.getDeclaredMethod("bar",int.class, Object[].class).getParameterTypes()[1].toString());
		
        if(!B2.class.getDeclaredMethod("bar2",int.class, Object[][][].class).toGenericString().equals("public java.lang.String java.lang.ClassGenericsTest5$B2.bar2(int,java.lang.Object[][][])")) 
			fail("test_1, case 005 FAILED: "+B2.class.getDeclaredMethod("bar2",int.class, Object[][][].class).toGenericString());
		
        if(!B2.class.getDeclaredMethod("bar3",int.class, int[][][].class, int[].class).toGenericString().equals("public java.lang.Object java.lang.ClassGenericsTest5$B2.bar3(int,int[][][],int[])")) 
			fail("test_1, case 006 FAILED: "+B2.class.getDeclaredMethod("bar3",int.class, int[][][].class, int[].class).toGenericString());

		int res = 0;
        java.lang.reflect.Method[] ms = B1.class.getDeclaredMethods();
		for(int i = 0; i < ms.length; i++){
	       //if(ms[i].toGenericString().equals("public <X> java.lang.Object java.lang.ClassGenericsTest5$B1.bar4(int,int[][][],java.lang.ClassGenericsTest5$OneParamType<java.lang.Void[][][]>[],java.lang.ClassGenericsTest5$OneParamType<X[][][]>,java.lang.Object[])")) res += 1;
	       if(ms[i].toGenericString().equals("public <X> java.lang.Object java.lang.ClassGenericsTest5$B1.bar4(int,int[][][],java.lang.ClassGenericsTest5.OneParamType<java.lang.Void[][][]>[],java.lang.ClassGenericsTest5.OneParamType<X[][][]>,java.lang.Object[])")) res += 1;
	       if(ms[i].toGenericString().equals("public java.lang.Object java.lang.ClassGenericsTest5$B1.bar3(int,int[][][],int[])")) res += 10;
	       if(ms[i].toGenericString().equals("public java.lang.Object java.lang.ClassGenericsTest5$B1.bar2(int,java.lang.Object[][][])")) res += 100;
	       if(ms[i].toGenericString().equals("public java.lang.Object java.lang.ClassGenericsTest5$B1.bar(int,java.lang.Object[])")) res += 1000;
	       if(ms[i].toGenericString().equals("public java.lang.Object java.lang.ClassGenericsTest5$B1.foo()")) res += 10000;
	       if(ms[i].toGenericString().equals("public static void java.lang.ClassGenericsTest5$B1.main2(java.lang.String[])")) res += 100000;
	       if(ms[i].toGenericString().equals("public static void java.lang.ClassGenericsTest5$B1.main1(java.lang.String[])")) res += 1000000;
		}
        if(res!=1111111) fail("test_1, case 007 FAILED: "+"The uncoincedence has been detected!");
		
        //if(!B1.class.getDeclaredMethod("bar4",int.class, int[][][].class, OneParamType[].class, OneParamType.class, Object[].class).toGenericString().equals("public <X> java.lang.Object java.lang.ClassGenericsTest5$B1.bar4(int,int[][][],java.lang.ClassGenericsTest5$OneParamType<java.lang.Void[][][]>[],java.lang.ClassGenericsTest5$OneParamType<X[][][]>,java.lang.Object[])")) 
		//	fail("test_1, case 008 FAILED: "+B1.class.getDeclaredMethod("bar4",int.class, int[][][].class, OneParamType[].class, OneParamType.class, Object[].class).toGenericString());
        if(!B1.class.getDeclaredMethod("bar4",int.class, int[][][].class, OneParamType[].class, OneParamType.class, Object[].class).toGenericString().equals("public <X> java.lang.Object java.lang.ClassGenericsTest5$B1.bar4(int,int[][][],java.lang.ClassGenericsTest5.OneParamType<java.lang.Void[][][]>[],java.lang.ClassGenericsTest5.OneParamType<X[][][]>,java.lang.Object[])")) 
			fail("test_1, case 008 FAILED: "+B1.class.getDeclaredMethod("bar4",int.class, int[][][].class, OneParamType[].class, OneParamType.class, Object[].class).toGenericString());

		java.lang.reflect.Method mmm=B1.class.getDeclaredMethod("bar4",int.class, int[][][].class, OneParamType[].class, OneParamType.class, Object[].class);
        if(!(mmm.getGenericParameterTypes()[1] instanceof java.lang.reflect.GenericArrayType)) fail("test_1, case 009 FAILED: "+(mmm.getGenericParameterTypes()[1] instanceof java.lang.reflect.GenericArrayType));
        if(!(mmm.getGenericParameterTypes()[2] instanceof java.lang.reflect.GenericArrayType)) fail("test_1, case 010 FAILED: "+(mmm.getGenericParameterTypes()[2] instanceof java.lang.reflect.GenericArrayType));
        if((mmm.getGenericParameterTypes()[3] instanceof java.lang.reflect.GenericArrayType)) fail("test_1, case 011 FAILED: "+(mmm.getGenericParameterTypes()[3] instanceof java.lang.reflect.GenericArrayType));

        //if(!CC.class.getDeclaredConstructor(Throwable.class,OneParamType.class).toGenericString().equals("protected strictfp <T> java.lang.ClassGenericsTest5$CC(T,java.lang.ClassGenericsTest5$OneParamType<? super T>) throws T,java.lang.Error")) fail("test_1, case 012 FAILED: "+CC.class.getDeclaredConstructor(Throwable.class,OneParamType.class).toGenericString());
        if(!CC.class.getDeclaredConstructor(Throwable.class,OneParamType.class).toGenericString().equals("protected strictfp <T> java.lang.ClassGenericsTest5$CC(T,java.lang.ClassGenericsTest5.OneParamType<? super T>) throws T,java.lang.Error")) 
			fail("test_1, case 012 FAILED: "+CC.class.getDeclaredConstructor(Throwable.class,OneParamType.class).toGenericString());
  }
}