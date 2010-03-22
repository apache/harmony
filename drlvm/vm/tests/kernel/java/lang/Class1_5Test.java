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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/*
 * Created on 28.05.2005
 *
 * This Class1_5Test class is used to test the extention of the
 * Core API Class class to 1.5 
 * 
 */

@Documented
@Retention(value=java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(value=java.lang.annotation.ElementType.TYPE)
@interface zzz {
        public int id() default 777;
		public String date() default "[unimplemented]";
}

public class Class1_5Test extends TestCase {

		protected void setUp() throws Exception {
		}

		protected void tearDown() throws Exception {
		}

		/**
		 *  
		 */
		public void test_isAnonymousClass_V() {
			class e {};//Class1_5Test$1e.class
			assertFalse("Non-anonymous class is indicated as anonymous!", 
                       (new e()).getClass().isAnonymousClass());
			assertTrue("anonymous class is indicated as non-anonymous!",
					   (new e() { int i; int m1(){return i+m2();}; int m2(){return i+m1();}}).getClass().isAnonymousClass());
		}

		/**
		 *  
		 */
		enum x{aaa;};
		public void test_isEnum_V() {
			class e {};
			assertFalse("Non-enum class is indicated as enum!", e.class.isEnum());
			assertTrue("enum class is indicated as non-enum!", x.class.isEnum());
		}

		/**
		 *  
		 */
		public void test_isLocalClass_V() {
			class e {};
			assertFalse("Non-local class is indicated as local!",
                       x.class.isLocalClass());
			assertTrue("local class is indicated as non-local!",
                       e.class.isLocalClass());
		}

		/**
		 *  
		 */
		public void test_isMemberClass_V() {
			class e {};
			assertFalse("Non-member class is indicated as member!", 
                       e.class.isMemberClass());
			assertTrue("member class is indicated as non-member!",
                       x.class.isMemberClass());
		}

		/**
		 *  
		 */
		public void test_isSynthetic_V() {
			class e {};
			assertFalse("Non-synthetic class is indicated as synthetic!",
                       e.class.isSynthetic());
			//assertTrue("synthetic class is indicated as non-synthetic!",
            //           (new e() { int i; }).getClass().isSynthetic());
		}

		/**
		 *  
		 */
		public void test_isAnnotation_V() {
			class e {};
			assertFalse("Non-annotation class is indicated as annotation!", 
                        e.class.isAnnotation());
			assertTrue("annotation class is indicated as non-annotation!", 
                        zzz.class.isAnnotation());
		}

		/**
		 *  
		 */
		public void test_isAnnotationPresent_Cla() {
			class e {};
			assertFalse("zzz annotation is not presented for e class!", 
                       e.class.isAnnotationPresent(zzz.class));
            assertFalse("zzz annotation is not presented for zzz class!", 
                       zzz.class.isAnnotationPresent(zzz.class));
			assertTrue("Target annotation is presented for zzz class!",
                       zzz.class.isAnnotationPresent(java.lang.annotation
                                                     .Target.class));
			assertTrue("Documented annotation is presented for zzz class!",
                       zzz.class.isAnnotationPresent(java.lang.annotation
                                                     .Documented.class));
			assertTrue("Retention annotation is presented for zzz class!", 
                       zzz.class.isAnnotationPresent(java.lang.annotation
                                                     .Retention.class));
		}

		/**
		 *  
		 */
//		enum xx{A(77),B(55),C(33);xx(int i) {this.value = i;}};
		enum xx{A,B,C;};
		public void test_getEnumConstants_V() {
			//assertTrue("FAILED: test_getEnumConstants_V.check001: Non-enum class is indicated as enum!", xx.class.getEnumConstants()[0].value() == 77);
			assertTrue("The first constant should be xx.A!",
                       xx.class.getEnumConstants()[0] == xx.A);
			assertTrue("The second constant should not be xx.A!",
                       xx.class.getEnumConstants()[1] != xx.A);
			assertTrue("The third constant should be xx.C!",
                       xx.class.getEnumConstants()[2] == xx.C);
		}

		/**
		 *  
		 */
		class e {};
		//class gg {class gga {class ggb {class ggc {}; class ggg {public int id() {class f {};class ff {};class ff2 {public void vd() {Object ooo = new e() { int i; };}};return 999;}};};};};
        public class gg {
            public class gga {
                public gga(){};
                public class ggb {
                    public ggb(){};
                    class ggc {};
                    public class ggg {
                        public ggg(){};
                        public Object id() {
                            class f {};
                            class ff {};
                            class ff2 {
                                public Object vd() {
                                    Object ooo = new e() { int i;  int m1(){vd(); return i+m2();}; int m2(){return i+m1();}};
                                    return ooo;
                                }
                                public Object vd2() {
									return vd()==null?vd3():vd();
                                }
                                public Object vd3() {
									return vd()==null?vd2():vd();
                                }
                            };
                            return new ff2();
                        }
                    };
                    public Object gggO() {
                        return new ggg();
                    }
                };
                public Object ggbO() {
                    return new ggb();
                }
            };
            public Object ggaO() {
                new e() { int i;  int m1(){return i+m2();}; int m2(){return i+m1();}};
                return new gga();
            }
        };
		public void test_getSimpleName_V() {
			class e {};
			try {
                //##########################################################################################################
                //##########################################################################################################
                //\\assertTrue("FAILED: test_getSimpleName_V.check001: Simple name for java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2$1 should be empty!", Class.forName("java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2$1").getSimpleName().equals(""));
                //\\assertTrue("FAILED: test_getSimpleName_V.check002: Simple name for java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2 should be ff2!", Class.forName("java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2").getSimpleName().equals("ff2"));
                // to avoid bug of local classes naming in org.eclipse.jdt.core.JDTCompilerAdapter
                // ("Class1_5Test$1ff2" instead of "Class1_5Test$gg$gga$ggb$ggg$1ff2")
                try {
                    Object o1, o2, o3, o4, o5;
                    o1 = new gg();
                    o2 = o1.getClass().getMethod("ggaO").invoke(o1/*, null*/);
                    o3 = o2.getClass().getMethod("ggbO").invoke(o2/*, null*/);
                    o4 = o3.getClass().getMethod("gggO").invoke(o3/*, null*/);
                    //\\assertTrue("FAILED: test_getSimpleName_V.check001: Simple name for java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2$1 should be empty!", Class.forName("java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2$1").getSimpleName().equals(""));
                    assertTrue("check001: Simple name for java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2$1 should be empty!", (o5 = o4.getClass().getMethod("id").invoke(o4/*, null*/)).getClass().getMethod("vd").invoke(o5/*, null*/).getClass().getSimpleName().equals(""));
                    //\\assertTrue("check002: Simple name for java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2 should be ff2!", Class.forName("java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2").getSimpleName().equals("ff2"));
                    assertTrue("check002: Simple name for java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2 should be ff2!", (o5 = o4.getClass().getMethod("id").invoke(o4/*, null*/)).getClass().getMethod("vd").invoke(o5/*, null*/).getClass().getEnclosingClass().getSimpleName().equals("ff2"));
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                    fail("exception check001");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    fail("exception check002");
                } catch (java.lang.reflect.InvocationTargetException e) {
                    e.printStackTrace();
                    fail("exception check003");
                }
                //##########################################################################################################
                //##########################################################################################################
				assertTrue("check003: Simple name for " + 
                               "java.lang.Class1_5Test$gg$gga$ggb$ggc should be ggc!",
                           Class.forName("java.lang.Class1_5Test$gg$gga$ggb$ggc")
                               .getSimpleName().equals("ggc"));
				assertTrue("check004: Simple name for " +
                               "java.lang.Class1_5Test$1 should be empty!",
                           Class.forName("java.lang.Class1_5Test$1")
                               .getSimpleName().equals(""));
				Class c = (new Object() { int i;  int m1(){return i+m2();}; int m2(){return i+m1();}}).getClass();
				assertTrue("check005: Simple name for " +
                               "\"(new Object() { int i; })\" should be empty!",
                           c.getSimpleName().equals(""));
				c = (new e() { int i;  int m1(){return i+m2();}; int m2(){return i+m1();}}).getClass();
				assertTrue("check006: Simple name for " +
                               "\"(new e() { int i; })\" should be empty!",
                           c.getSimpleName().equals(""));
				assertTrue("check007: Simple name for " +
                               "java.lang.Class1_5Test$1e should be e!",
                           e.class.getSimpleName().equals("e"));
			} catch (Exception e) {
                e.printStackTrace();
				fail("check008: Error of the class loading!");
			}
		}

		/**
		 *  
		 */
		public void test_getEnclosingClass_V() {
			class e {};
			try {
                //##########################################################################################################
                //##########################################################################################################
                // to avoid bug of local classes naming in org.eclipse.jdt.core.JDTCompilerAdapter
                // ("Class1_5Test$1ff2" instead of "Class1_5Test$gg$gga$ggb$ggg$1ff2")
                try {
                    Object o1, o2, o3, o4, o5;
                    o1 = new gg();
                    o2 = o1.getClass().getMethod("ggaO").invoke(o1/*, null*/);
                    o3 = o2.getClass().getMethod("ggbO").invoke(o2/*, null*/);
                    o4 = o3.getClass().getMethod("gggO").invoke(o3/*, null*/);
                    //\\//\\if(!(o5 = o4.getClass().getMethod("id").invoke(o4/*, null*/)).getClass().getMethod("vd").invoke(o5/*, null*/).getClass().getName().equals("java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2$1")) fail("test_3, case 019 FAILED");
                    if(!(o5 = o4.getClass().getMethod("id").invoke(o4/*, null*/)).getClass().getMethod("vd").invoke(o5/*, null*/).getClass().getName().matches("java\\.lang\\.Class1_5Test.*1ff2\\$1")) fail("test_3, case 019 FAILED");
                    if(!(o5 = o4.getClass().getMethod("id").invoke(o4/*, null*/)).getClass().getMethod("vd").invoke(o5/*, null*/).getClass().getEnclosingClass().getName().matches("java\\.lang\\.Class1_5Test.*1ff2")) fail("test_3, case 019 FAILED");
                    //if(!(o = gg.gga.ggb.ggg.class.getMethod("id").invoke(java.lang.Class1_5Test.gg.gga.ggb.ggg.class.newInstance(), null)).getClass().getMethod("vd").invoke(o, null).getClass().getName().equals("java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2$1")) fail("test_3, case 019 FAILED");
                    //\\//\\assertTrue("FAILED: test_getEnclosingClass_V.check001: Simple name of enclosing class for java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2$1 should be ff2!", Class.forName("java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2$1").getEnclosingClass().getSimpleName().equals("ff2"));
                    assertTrue("check001: Simple name of enclosing class for " +
                                   "java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2$1" +
                                   " should be ff2!",
                               (o5 = o4.getClass().getMethod("id")
                                   .invoke(o4/*, null*/)).getClass()
                                   .getMethod("vd").invoke(o5/*, null*/)
                                   .getClass().getEnclosingClass()
                                   .getSimpleName().equals("ff2"));
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                    fail("check003");
                } catch(IllegalAccessException e) {
                    e.printStackTrace();
                    fail("check004");
                } catch(java.lang.reflect.InvocationTargetException e) {
                    e.printStackTrace();
                    fail("check005");
                }
                //##########################################################################################################
                //##########################################################################################################
			} catch (Exception e) {
                e.printStackTrace();
				assertTrue("check002: Error of the class loading!", false);
			}
		}

		/**
		 *  
		 */
		public void test_getDeclaredAnnotations_V() {
			assertFalse("Target annotation is presented for zzz class!", 
                       zzz.class.getDeclaredAnnotations()[0].toString()
                          .indexOf("java.lang.annotation.Documented") == -1);
            assertFalse("Documented annotation is presented for zzz class!",
                       zzz.class.getDeclaredAnnotations()[1].toString()
                          .indexOf("java.lang.annotation.Retention") == -1);
			assertFalse("Retention annotation is presented for zzz class!",
                       zzz.class.getDeclaredAnnotations()[2].toString()
                          .indexOf("java.lang.annotation.Target") == -1);
		}

		/**
		 *  
		 */
		@zzz() enum www{aaa;};
		public void test_Annotations_V() {
			assertFalse("zzz annotation is presented for www enum!", 
                    www.class.getAnnotations()[0].toString()
                       .indexOf("java.lang.zzz") == -1);
            assertEquals("Only one annotation is presented in enum www!", 
                    1,
                    www.class.getAnnotations().length);
		}

		/**
		 *  
		 */
		public void test_getCanonicalName_V() {
			class e {};
			try {
                //##########################################################################################################
                //##########################################################################################################
                // to avoid bug of local classes naming in org.eclipse.jdt.core.JDTCompilerAdapter
                // ("Class1_5Test$1ff2" instead of "Class1_5Test$gg$gga$ggb$ggg$1ff2")
				//\\//\\assertTrue("FAILED: test_getCanonicalName_V.check001: Canonical name for java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2$1 should be null!", Class.forName("java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2$1").getCanonicalName() == null);
                //\\//\\assertTrue("FAILED: test_getCanonicalName_V.check002: Canonical name for java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2 should be null!", Class.forName("java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2").getCanonicalName() == null);
                try {
                    Object o1, o2, o3, o4, o5;
                    o1 = new gg();
                    o2 = o1.getClass().getMethod("ggaO").invoke(o1/*, null*/);
                    o3 = o2.getClass().getMethod("ggbO").invoke(o2/*, null*/);
                    o4 = o3.getClass().getMethod("gggO").invoke(o3/*, null*/);
                    assertNull("check001: Canonical name for " +
                                   "java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2$1" +
                                   "should be null!",
                               (o5 = o4.getClass().getMethod("id")
                                   .invoke(o4/*, null*/)).getClass()
                                   .getMethod("vd").invoke(o5/*, null*/)
                                   .getClass().getCanonicalName());
                    assertNull("check002: Canonical name for " +
                                   "java.lang.Class1_5Test$gg$gga$ggb$ggg$1ff2" +
                                   "should be null!",
                               (o5 = o4.getClass().getMethod("id")
                                   .invoke(o4/*, null*/)).getClass()
                                   .getMethod("vd").invoke(o5/*, null*/)
                                   .getClass().getEnclosingClass()
                                   .getCanonicalName());
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                    fail("exception check001");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    fail("exception check002");
                } catch (java.lang.reflect.InvocationTargetException e) {
                    e.printStackTrace();
                    fail("exception check003");
                }
                //##########################################################################################################
                //##########################################################################################################
				assertEquals("check003: incorrrect canonical name for " +
                                     "java.lang.Class1_5Test$gg$gga$ggb$ggc:",
                             "java.lang.Class1_5Test.gg.gga.ggb.ggc",
                             Class.forName("java.lang.Class1_5Test$gg$gga$ggb$ggc")
                                 .getCanonicalName());
				assertNull("check004: Canonical name for " + 
                                   "java.lang.Class1_5Test$1 should be null!",
                           Class.forName("java.lang.Class1_5Test$1")
                               .getCanonicalName());
				Class c = (new Object() { int i;  int m1(){return i+m2();}; int m2(){return i+m1();}}).getClass();
                assertNull("check005: Canonical name for " +
                               "\"(new Object() { int i; })\" should be null!",
                           c.getCanonicalName());
				c = (new e() { int i;  int m1(){return i+m2();}; int m2(){return i+m1();}}).getClass();
                assertNull("check006: Canonical name for " +
                               "\"(new e() { int i; })\" should be null!",
                           c.getCanonicalName());
                assertNull("check007: Canonical name for " +
                               "java.lang.Class1_5Test$1e should be null!",
                               e.class.getCanonicalName());
			} catch (Exception e){
				fail("check008: Error of the class loading!");
			}
		}

		/**
		 *  
		 */
		public void test_getAnnotation_Cla() {
			class e {};
			assertNull("zzz annotation is not presented in e class!", 
                    e.class.getAnnotation(zzz.class));
			assertNull("zzz annotation is not presented in zzz class!", 
                    zzz.class.getAnnotation(zzz.class));
			assertFalse("Target annotation is presented in zzz class!", 
                    zzz.class.getAnnotation(java.lang.annotation.Target.class)
                       .toString().indexOf("java.lang.annotation.Target") == -1);
			assertFalse("Documented annotation is presented in zzz class!", 
                    zzz.class.getAnnotation(java.lang.annotation.Documented.class)
                       .toString().indexOf("java.lang.annotation.Documented") == -1);
            assertFalse("Retention annotation is presented in zzz class!", 
                    zzz.class.getAnnotation(java.lang.annotation.Retention.class)
                       .toString().indexOf("java.lang.annotation.Retention") == -1);
		}

		/**
		 *  
		 */
		public void test_cast_Cla() {
			class e {};
			class ee extends e {};
			class eee extends ee {};
			try {
				ee.class.cast((Object)new eee());
			} catch (ClassCastException _) {
				fail("Object of eee can be casted to Object of ee class!");
			}
			try {
				eee.class.cast((Object)new e());
				fail("Object of e cannott be casted to Object of eee class");
			} catch (ClassCastException _) {
                return;
			}
		}

		/**
		 *  
		 */
		public void test_asSubclass_Cla() {
			class e {};
			class ee extends e {};
			class eee extends ee {};
			try {
				eee.class.asSubclass(ee.class);
			} catch (ClassCastException _) {
				fail("eee class can be casted to represent a subclass of ee!");
			}
			try {
				e.class.asSubclass(eee.class);
				fail("e class cannot be casted to represent a subclass of eee!");
			} catch (ClassCastException _) {
                return;
			}
		}

		/**
		 *  
		 */
		public void test_getEnclosingMethod_V() {
			class e {};
			class ee extends e {};
			class eee extends ee {};
			assertFalse("ee class is declared within " + "" +
                            "test_getEnclosingMethod_V method!",
                        eee.class.getEnclosingMethod().toString()
                           .indexOf("test_getEnclosingMethod_V") == -1);
		}

		/**
		 *  
		 */
		public void test_getEnclosingConstructor_V() {
			class eklmn {
                eklmn() {
                    class ee {
                    }; 
                    assertFalse("ee class is declared within eklmn constructor!",
                               ee.class.getEnclosingConstructor().toString()
                                   .indexOf("eklmn") == -1);
                }
            };
			new eklmn();
		}

		/**
		 *  
		 */
		public void test_getGenericSuperclass_V() {
			class e1 {};
			class e2 extends e1 {};
			class e3 extends e2 {};
			assertFalse("e2 is generic superclass for e3!", 
                       e3.class.getGenericSuperclass().toString()
                           .indexOf("e2") == -1);
		}
        
        static final class BC1 extends AC1<String, byte[]>{} 
        static final class BC2 extends AC1<byte[][], Byte>{}
        static abstract class AC1<ValueType, BoundType> {} 

        /**
         * A regression test for HARMONY-5752 
         */
        public void testGenericSupeclass_h5752() throws Exception {
            Type t1 = BC1.class.getGenericSuperclass();
            assertTrue("t1", t1 instanceof ParameterizedType);
            Type[] args1 = ((ParameterizedType)t1).getActualTypeArguments();
            assertEquals("num params t1", 2, args1.length);
            assertEquals("1 param t1", String.class, args1[0]);
            assertTrue("2 param t1 type", args1[1] instanceof GenericArrayType);
            assertEquals("2 param t1", byte.class, ((GenericArrayType)args1[1]).getGenericComponentType());

            Type t2 = BC2.class.getGenericSuperclass();
            assertTrue("t2", t2 instanceof ParameterizedType);
            Type[] args2 = ((ParameterizedType)t2).getActualTypeArguments();
            assertEquals("num params t2", 2, args2.length);
            assertTrue("2 param t2 type", args2[0] instanceof GenericArrayType);
            Type at2 = ((GenericArrayType)args2[0]).getGenericComponentType();
            assertTrue("2 param t2 type2", at2 instanceof GenericArrayType);
            assertEquals("2 param t2", byte.class, ((GenericArrayType)at2).getGenericComponentType());
            assertEquals("2 param t2", Byte.class, args2[1]);
        }

		/**
		 *  
		 */
		interface e1 {};
		interface e2 extends e1 {};
		public void test_getGenericInterfaces_V() {
			class e3 implements e2 {};
			assertFalse("e2 is genericaly implemented by e3!",
                        e3.class.getGenericInterfaces()[0].toString()
                            .indexOf("e2") == -1);
		}

		/**
		 *  
		 */
		public void test_getTypeParameters_V() {
			class e3<T1, T2> implements e2 {};
			assertFalse("T1 is type parameter for e3!",
                        e3.class.getTypeParameters()[0].toString()
                            .indexOf("T1") == -1);
            assertFalse("T2 is type parameter for e3!",
                        e3.class.getTypeParameters()[1].toString()
                        .indexOf("T2") == -1);
		}
	}
