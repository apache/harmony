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

package java.lang.reflect;

import junit.framework.TestCase;

/*
 * Created on 01.28.2006
 */

@SuppressWarnings(value={"all"}) public class FieldTest extends TestCase {
    static public boolean sfld1 = true;

    public boolean ifld1 = false;

    static public byte sfld2 = (byte) 7;

    public byte ifld2 = (byte) 5;

    static public char sfld3 = 'G';

    public char ifld3 = 'l';

    static public double sfld4 = 777.d;

    public double ifld4 = 55.5d;

    static public float sfld5 = 7.77f;

    public float ifld5 = .555f;

    static public int sfld6 = 777;

    public int ifld6 = 555;

    static public long sfld7 = 777l;

    public long ifld7 = 555l;

    static public short sfld8 = Short.MAX_VALUE;

    public short ifld8 = Short.MIN_VALUE;

    /**
     *  
     */
    public void test_equals_Obj() {
        class X {
            public int Xfld = 777;
        }
        class Y {
            public int Xfld = 777;
        }
        try {
            Field f1 = X.class.getField("Xfld");
            Field f2 = Y.class.getField("Xfld");
            assertEquals("Error: equal fields should coincide", f1, f1);
            assertTrue("Error: coincidence of the unequal fields is detected",
                    !f1.equals(f2));
        } catch (NoSuchFieldException _) {
            fail("Error: unfound field");
        }
    }

    /**
     *  
     */
    public void test_get_Obj() {
        class X1 {
            public int Xfld = 777;
        }
        X1 x = new X1();
        x.Xfld = 333;
        try {
            Field f1 = X1.class.getField("Xfld");
            assertTrue("Error1: x.Xfld should be equal 333", ((Integer) (f1
                    .get(x))).intValue() == 333);
        } catch (Exception e) {
            //e.printStackTrace();
            fail("Error2: " + e.toString());
        }
        try {
            Field f1 = X1.class.getField("Xfld");
            f1.get(null);
            fail("Error3: NullPointerException should be risen just above");
        } catch (NullPointerException _) {
            // The specified object is null and the field is an instance field
        } catch (Exception e) {
            fail("Error4: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getBoolean_Obj() {
        try {
            sfld1 = true;
            Field f1 = FieldTest.class.getField("sfld1");
            assertTrue("Error1",
                    ((Boolean) (f1.get(null))).booleanValue() == true);
            Field f2 = FieldTest.class.getField("ifld1");
            FieldTest ft = new FieldTest();
            ft.ifld1 = true;
            assertTrue("Error2", ((Boolean) (f2.get(ft))).booleanValue());
            assertTrue("Error3", f2.getBoolean(ft));
        } catch (Exception e) {
            fail("Error3: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getByte_Obj() {
        try {
            sfld2 = (byte) 7;
            Field f1 = FieldTest.class.getField("sfld2");
            assertEquals("Error1", 7, ((Byte) (f1.get(null))).byteValue());
            Field f2 = FieldTest.class.getField("ifld2");
            FieldTest ft = new FieldTest();
            ft.ifld2 = 6;
            assertEquals("Error2", 6, ((Byte) (f2.get(ft))).byteValue());
            assertEquals("Error2", 6, f2.getByte(ft));
        } catch (Exception e) {
            fail("Error3: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getChar_Obj() {
        try {
            sfld3 = 'G';
            Field f1 = FieldTest.class.getField("sfld3");
            assertEquals("Error1", 
                    'G', ((Character) (f1.get(null))).charValue());
            Field f2 = FieldTest.class.getField("ifld3");
            FieldTest ft = new FieldTest();
            ft.ifld3 = 'm';
            assertEquals("Error2", 'm', ((Character) (f2.get(ft))).charValue());
            assertEquals("Error2", 'm', f2.getChar(ft));
        } catch (Exception e) {
            fail("Error3: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getDeclaringClass_V() {
        class X {
            public int Xfld = 777;
        }
        X x = new X();
        x.Xfld = 333;
        try {
            Field f1 = X.class.getField("Xfld");
            assertEquals("Error1", "java.lang.reflect.FieldTest$2X",
                    f1.getDeclaringClass().getName());
        } catch (NoSuchFieldException _) {
            fail("Error2");
        }
    }

    /**
     *  
     */
    public void test_getDouble_Obj() {
        try {
            sfld4 = 777.d;
            Field f1 = FieldTest.class.getField("sfld4");
            assertEquals("Error1", 777.d,
                    ((Double) (f1.get(null))).doubleValue());
            Field f2 = FieldTest.class.getField("ifld4");
            FieldTest ft = new FieldTest();
            ft.ifld4 = 11.1d;
            assertEquals("Error2", 
                    11.1d, ((Double) (f2.get(ft))).doubleValue());
            assertEquals("Error2", 
                    11.1d, f2.getDouble(ft));
        } catch (Exception e) {
            fail("Error3: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getFloat_Obj() {
        try {
            sfld5 = 7.77f;
            Field f1 = FieldTest.class.getField("sfld5");
            assertEquals("Error1", 
                    7.77f, ((Float) (f1.get(null))).floatValue());
            Field f2 = FieldTest.class.getField("ifld5");
            FieldTest ft = new FieldTest();
            ft.ifld5 = .9999f;
            assertEquals("Error2", .9999f, ((Float) (f2.get(ft))).floatValue());
            assertEquals("Error2", .9999f, f2.getFloat(ft));
        } catch (Exception e) {
            fail("Error3: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getInt_Obj() {
        try {
            sfld6 = 777;
            Field f1 = FieldTest.class.getField("sfld6");
            assertEquals("Error1", 777, ((Integer) (f1.get(null))).intValue());
            Field f2 = FieldTest.class.getField("ifld6");
            FieldTest ft = new FieldTest();
            ft.ifld6 = 222;
            assertEquals("Error2", 222, ((Integer) (f2.get(ft))).intValue());
            assertEquals("Error2", 222, f2.getInt(ft));
        } catch (Exception e) {
            fail("Error3: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getLong_Obj() {
        try {
            sfld7 = 777l;
            Field f1 = FieldTest.class.getField("sfld7");
            assertEquals("Error1", 777l, ((Long) (f1.get(null))).longValue());
            Field f2 = FieldTest.class.getField("ifld7");
            FieldTest ft = new FieldTest();
            ft.ifld7 = 4444l;
            assertEquals("Error2", 4444l, ((Long) (f2.get(ft))).longValue());
            assertEquals("Error2", 4444l, f2.getLong(ft));
        } catch (Exception e) {
            fail("Error3: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getModifiers_V() {
        class X {
            public int Xfld;

            final int Yfld = 777;
        }
        new X();
        try {
            Field f1 = X.class.getField("Xfld");
            assertTrue("Error1", java.lang.reflect.Modifier.isPublic(f1
                    .getModifiers()));
            Field af[] = X.class.getDeclaredFields();
            for (int i = 0; i < af.length; i++) {
                if (af[i].getName().equals("Yfld"))
                    assertTrue("Error2", java.lang.reflect.Modifier
                            .isFinal(af[i].getModifiers()));
            }
        } catch (NoSuchFieldException _) {
            fail("Error3");
        }
    }

    /**
     *  
     */
    public void test_getName_V() {
        class X {
            public int Xfld;

            final int Yfld = 777;
        }
        new X();
        try {
            Field f1 = X.class.getField("Xfld");
            assertEquals("Error1", "Xfld", f1.getName());
            Field af[] = X.class.getDeclaredFields();
            int res = 0;
            for (int i = 0; i < af.length; i++) {
                if (af[i].getName().equals("Yfld")
                        || af[i].getName().equals("Xfld"))
                    res++;
            }
            assertEquals("Error2", 2, res);
        } catch (NoSuchFieldException _) {
            fail("Error3");
        }
    }

    /**
     *  
     */
    public void test_getShort_Obj() {
        try {
            sfld8 = Short.MAX_VALUE;
            Field f1 = FieldTest.class.getField("sfld8");
            assertEquals("Error1",
                    Short.MAX_VALUE, ((Short) (f1.get(null))).shortValue());
            Field f2 = FieldTest.class.getField("ifld8");
            FieldTest ft = new FieldTest();
            ft.ifld8 = Short.MIN_VALUE;
            assertEquals("Error2",
                    Short.MIN_VALUE, ((Short) (f2.get(ft))).shortValue());
            assertEquals("Error2",
                    Short.MIN_VALUE, f2.getShort(ft));
        } catch (Exception e) {
            fail("Error3: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getType_V() {
        Field af[] = FieldTest.class.getDeclaredFields();
        int res = 0;
        for (int i = 0; i < af.length; i++) {
            if (af[i].getType().getName().equals("boolean"))
                res += 1;
            if (af[i].getType().getName().equals("byte"))
                res += 10;
            if (af[i].getType().getName().equals("char"))
                res += 100;
            if (af[i].getType().getName().equals("double"))
                res += 1000;
            if (af[i].getType().getName().equals("float"))
                res += 10000;
            if (af[i].getType().getName().equals("int"))
                res += 100000;
            if (af[i].getType().getName().equals("long"))
                res += 1000000;
            if (af[i].getType().getName().equals("short"))
                res += 10000000;
        }
        assertEquals("Error1", 22222222, res);
    }

    /**
     *  
     */
    public void test_hashCode_V() {
        try {
            Field f1 = FieldTest.class.getField("sfld8");
            assertEquals("Error1", 
                    f1.getDeclaringClass().getName().hashCode() ^ 
                        f1.getName().hashCode(),
                    f1.hashCode());
        } catch (NoSuchFieldException _) {
            fail("Error2");
        }
    }

    /**
     *  
     */
    public void test_set_Obj_Obj() {
        class X2 {
            X2 xx;

            int Yfld = 777;

            public int m() {
                return Yfld;
            };
        }
        X2 x = new X2();
        try {
            Field f1 = X2.class.getDeclaredField("Yfld");
            f1.set(x, new Integer(345));
            assertTrue("Error1", x.m() == 345);

            f1 = X2.class.getDeclaredField("xx");
            f1.set(x, x);
            assertTrue("Error2", x.xx.Yfld == 345);
            assertTrue("Error3", x.xx.m() == 345);
            assertTrue("Error4", x.xx.xx.xx.xx.xx.xx.xx.xx.xx.xx.equals(x));
            Field f2 = x.xx.xx.xx.xx.xx.xx.xx.xx.getClass().getDeclaredField(
                    "Yfld");
            assertTrue("Error5", x.Yfld == ((Integer) f2
                    .get(x.xx.xx.xx.xx.xx.xx.xx.xx.xx)).intValue());
        } catch (Exception e) {
            //e.printStackTrace();
            fail("Error6: " + e.toString());
        }
    }
    /**
     *  
     */
    static class XX {
        static int Yfld = 777;
    }
    public void test_set_Obj_Obj_2() {
        XX x = new XX();
        try {
            Field f1 = XX.class.getDeclaredField("Yfld");
            f1.set(x, new Integer(345));
            assertTrue("Error1", x.Yfld == 345);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error2: " + e.toString());
        }
    }
    public void test_set_Null_Negative() throws Throwable {
        try {
            Field f1 = XX.class.getDeclaredField("Yfld");
            f1.set(null, null);
            fail("null value should not be unboxed to primitive");
        } catch (IllegalArgumentException ok) {}
    }

    /**
     *  
     */
    public void test_setBoolean_Obj() {
        try {
            Field f1 = FieldTest.class.getField("sfld1");
            f1.setBoolean((Object) null, false);
            assertFalse("Error1", ((Boolean) (f1.get(null))).booleanValue());
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_setByte_Obj() {
        try {
            Field f1 = FieldTest.class.getField("sfld2");
            f1.setByte((Object) null, (byte) 8);
            assertEquals("Error1",
                    (byte) 8, ((Byte) (f1.get(null))).byteValue());
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_setChar_Obj() {
        try {
            Field f1 = FieldTest.class.getField("sfld3");
            f1.setChar((Object) null, 'Z');
            assertEquals("Error1",
                    'Z', ((Character) (f1.get(null))).charValue());
            FieldTest.class.getField("ifld3");
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_setDouble_Obj() {
        try {
            Field f1 = FieldTest.class.getField("sfld4");
            f1.setDouble((Object) null, 12.3d);
            assertEquals("Error1",
                    12.3d, ((Double) (f1.get(null))).doubleValue());
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_setFloat_Obj() {
        try {
            Field f1 = FieldTest.class.getField("sfld5");
            f1.setFloat((Object) null, 0.0057f);
            assertEquals("Error1",
                    0.0057f, ((Float) (f1.get(null))).floatValue());
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_setInt_Obj() {
        try {
            Field f1 = FieldTest.class.getField("sfld6");
            f1.setInt((Object) null, 222333444);
            assertEquals("Error1",
                    222333444, ((Integer) (f1.get(null))).intValue());
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_setLong_Obj() {
        try {
            Field f1 = FieldTest.class.getField("sfld7");
            f1.setLong((Object) null, 99999999999l);
            assertEquals("Error1",
                    99999999999l, ((Long) (f1.get(null))).longValue());
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_setShort_Obj() {
        try {
            Field f1 = FieldTest.class.getField("sfld8");
            f1.setShort((Object) null,
                    (short) (Short.MAX_VALUE + Short.MIN_VALUE));
            assertEquals("Error1",
                    (short) (Short.MAX_VALUE + Short.MIN_VALUE), 
                    ((Short) (f1.get(null))).shortValue());
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_toString_Obj() {
        class X {
            int Yfld = 777;
        }
        new X();
        try {
            Field f1 = X.class.getDeclaredField("Yfld");
            assertEquals("Error1 ", 
                    "int java.lang.reflect.FieldTest$5X.Yfld", f1.toString());
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }
    
    static class RefFld {
        public Object o;
        public static Object so;
        public Integer i;
        public static Integer si;
    }
    
    public void testSet_Obj() throws Throwable {
        Field f = RefFld.class.getField("o");
        RefFld obj = new RefFld();
        Object val = "dmjb";
        f.set(obj, val);
        assertSame(val, f.get(obj));
        f.set(obj, null);
        assertNull(f.get(obj));
    }
    
    public void testSet_Obj_Static() throws Throwable {
        Field f = RefFld.class.getField("so");
        RefFld obj = new RefFld();
        Object val = "dmjb";
        f.set(obj, val);
        assertSame(val, f.get(null));
        f.set(null, null);
        assertNull(f.get(obj));
    }
    
    public void testSet_BoxObj() throws Throwable {
        Field f = RefFld.class.getField("i");
        RefFld obj = new RefFld();
        Object val = new Integer(35434);
        f.set(obj, val);
        assertSame(val, f.get(obj));
        f.set(obj, null);
        assertNull(f.get(obj));
    }
    
    public void testSet_BoxObj_Static() throws Throwable {
        Field f = RefFld.class.getField("si");
        RefFld obj = new RefFld();
        Object val = new Integer(589878);
        f.set(obj, val);
        assertSame(val, f.get(null));
        f.set(null, null);
        assertNull(f.get(obj));
    }
    
    public void testSet_Obj_Invalid() throws Throwable {
        try {
            Field f = RefFld.class.getField("si");
            f.set(null, "345");
            fail("IllegalArgumentException was not thrown on incompartible value");
        } catch (IllegalArgumentException ok) {}
    }
}