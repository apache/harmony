/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.beans.tests.java.beans;

import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.ExceptionListener;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.Statement;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.List;

import junit.framework.TestCase;

import org.apache.harmony.beans.tests.support.mock.MockBean4Codec;
import org.apache.harmony.beans.tests.support.mock.MockBean4CodecBadGetter;
import org.apache.harmony.beans.tests.support.mock.MockExceptionListener;
import org.apache.harmony.beans.tests.support.mock.MockFooLiYang;
@SuppressWarnings("unchecked")
public class EncoderTest extends TestCase {

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(EncoderTest.class);
    }

    public static class VerboseEncoder extends Encoder {

        private PrintWriter out;

        private boolean ident;

        public VerboseEncoder() {
            this(new PrintWriter(System.out, true), true);
        }

        public VerboseEncoder(PrintWriter out, boolean ident) {
            this.out = out;
            this.ident = ident;
        }

        @Override
        public Object get(Object arg0) {
            String identStr = ident ? ident() : "";
            out.println(identStr + "get()> " + arg0);
            Object result = super.get(arg0);
            out.println(identStr + "get()< " + result);
            return result;
        }

        @Override
        public PersistenceDelegate getPersistenceDelegate(Class type) {
            PersistenceDelegate result = super.getPersistenceDelegate(type);
            return result;
        }

        @Override
        public Object remove(Object arg0) {
            String identStr = ident ? ident() : "";
            out.println(identStr + "remove()> " + arg0);
            Object result = super.remove(arg0);
            out.println(identStr + "remove()< " + result);
            return result;
        }

        @Override
        public void writeExpression(Expression arg0) {
            String identStr = ident ? ident() : "";
            out.println(identStr + "writeExpression()> " + string(arg0));
            super.writeExpression(arg0);
            out.println(identStr + "writeExpression()< ");
        }

        @Override
        public void writeStatement(Statement arg0) {
            String identStr = ident ? ident() : "";
            out.println(identStr
                    + new Exception().getStackTrace()[1].getClassName()
                    + ".writeStatement()> " + string(arg0));
            super.writeStatement(arg0);
            out.println(identStr + "writeStatement()< ");
        }

        @Override
        public void writeObject(Object arg0) {
            String identStr = ident ? ident() : "";
            out.println(identStr + "writeObject()> " + arg0);
            super.writeObject(arg0);
            out.println(identStr + "writeObject()< ");
        }
    }

    public static class VerbosePD extends DefaultPersistenceDelegate {

        @Override
        protected void initialize(Class arg0, Object arg1, Object arg2,
                Encoder arg3) {
            System.out.println(ident() + "PDinitialize()> " + arg0 + ", "
                    + arg1 + ", " + arg2);
            super.initialize(arg0, arg1, arg2, arg3);
            System.out.println(ident() + "PDinitialize()< ");
        }

        @Override
        protected Expression instantiate(Object arg0, Encoder arg1) {
            System.out.println(ident() + "PDinstantiate()> " + arg0);
            Expression result = super.instantiate(arg0, arg1);
            System.out.println(ident() + "PDinstantiate()< " + result);
            return result;
        }

        @Override
        protected boolean mutatesTo(Object arg0, Object arg1) {
            System.out
                    .println(ident() + "PDmutatesTo()> " + arg0 + ", " + arg1);
            boolean result = super.mutatesTo(arg0, arg1);
            System.out.println(ident() + "PDmutatesTo()< " + result);
            return result;
        }

        @Override
        public void writeObject(Object arg0, Encoder arg1) {
            System.out.println(ident() + "PDwriteObject()> " + arg0);
            super.writeObject(arg0, arg1);
            System.out.println(ident() + "PDwriteObject()< ");
        }
    }

    public static class SampleBean {
        String myid = "default ID";

        int i = 1;

        SampleBean ref;

        public String getMyid() {
            return myid;
        }

        public void setMyid(String myid) {
            this.myid = myid;
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        public SampleBean getRef() {
            return ref;
        }

        public void setRef(SampleBean ref) {
            this.ref = ref;
        }

        @Override
        public String toString() {
            String superResult = super.toString();
            superResult.substring(superResult.indexOf("@"));
            return "myid=" + myid;
        }
    }

    public static String ident() {
        Exception ex = new Exception();
        int level = ex.getStackTrace().length;
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < level; i++) {
            buf.append("  ");
        }
        return buf.toString();
    }

    public static String string(Statement stat) {
        String str = "(" + stat.getTarget() + ")." + stat.getMethodName() + "(";
        Object args[] = stat.getArguments();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                str += ", ";
            }
            str += args[i];
        }
        str = str + ")";
        return str;
    }

    public static String string(Expression exp) {
        String str = "";
        try {
            str += str + exp.getValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        str += "=" + string((Statement) exp);
        return str;
    }

    public static class MockEncoder extends Encoder {

        @Override
        public void writeObject(Object o) {
            super.writeObject(o);
        }
    }

    public void testGetExceptionListener() {
        MockEncoder enc = new MockEncoder();
        assertNotNull(enc.getExceptionListener());

        MockExceptionListener l = new MockExceptionListener();
        enc.setExceptionListener(l);
        assertSame(l, enc.getExceptionListener());

        enc.writeObject(new MockBean4CodecBadGetter());
        assertTrue(l.size() > 0);
    }

    public void testSetExceptionListener_Null() {
        MockEncoder enc = new MockEncoder();
        ExceptionListener l = enc.getExceptionListener();
        enc.setExceptionListener(null);
        assertSame(l, enc.getExceptionListener());

        ExceptionListener l2 = new MockExceptionListener();
        enc.setExceptionListener(l2);
        enc.setExceptionListener(null);
        assertSame(l.getClass(), enc.getExceptionListener().getClass());
    }

    public void testSetExceptionListener() {
        MockEncoder enc = new MockEncoder();
        assertNotNull(enc.getExceptionListener());

        MockExceptionListener l = new MockExceptionListener();
        enc.setExceptionListener(l);
        assertSame(l, enc.getExceptionListener());

        enc.writeObject(new MockBean4CodecBadGetter());
        assertTrue(l.size() > 0);
    }

    public void testWriteExpression() {
        // covered by testWriteObject()
    }

    public void testWriteExpression_Null() {
        Encoder enc = new Encoder();
        try {
            enc.writeExpression(null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testWriteStatement() {
        // covered by testWriteObject()
    }

    public void testWriteStatement_Null() {
        Encoder enc = new Encoder();
        try {
            enc.writeStatement(null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testWriteObject_Null() {
        StringWriter sbwriter = new StringWriter();
        VerboseEncoder enc = new VerboseEncoder(
                new PrintWriter(sbwriter, true), false);
        enc.writeObject(null);
        String trace = sbwriter.toString();

        final String LS = System.getProperty("line.separator");
        assertEquals("writeObject()> null" + LS + "writeObject()< " + LS, trace);
    }

    public void testWriteObject() {
        StringWriter sbwriter = new StringWriter();
        VerboseEncoder enc = new VerboseEncoder(
                new PrintWriter(sbwriter, true), false);
        SampleBean b = new SampleBean();
        b.setI(3);
        b.setMyid("new name");
        enc.writeObject(b);
        String trace = sbwriter.toString();

        final String LS = System.getProperty("line.separator");
        int lastIndex = 0, index = 0;

        index = trace
                .indexOf(
                        "writeObject()> myid=new name"
                                + LS
                                + "get()> myid=new name"
                                + LS
                                + "get()< null"
                                + LS
                                + "remove()> myid=new name"
                                + LS
                                + "remove()< null"
                                + LS
                                + "writeExpression()> myid=new name=(class org.apache.harmony.beans.tests.java.beans.EncoderTest$SampleBean).new()"
                                + LS, lastIndex);
        assertTrue(lastIndex <= index);
        lastIndex = index;

        index = trace
                .indexOf("writeObject()> myid=new name" + LS
                        + "get()> myid=new name" + LS
                        + "get()< myid=default ID" + LS
                        + "writeExpression()> 3=(myid=new name).getI()" + LS,
                        lastIndex);
        assertTrue(lastIndex <= index);
        lastIndex = index;

        index = trace.indexOf("get()> 3" + LS + "get()< 1" + LS, lastIndex);
        assertTrue(lastIndex <= index);
        lastIndex = index;

        index = trace.indexOf(
                "writeExpression()> new name=(myid=new name).getMyid()" + LS
                        + "get()> new name" + LS + "get()< new name" + LS
                        + "writeExpression()< " + LS, lastIndex);
        assertTrue(lastIndex <= index);
        lastIndex = index;

        index = trace.indexOf(
                "writeStatement()> (myid=new name).setMyid(new name)" + LS
                        + "get()> myid=new name" + LS
                        + "get()< myid=default ID" + LS + "get()> new name"
                        + LS + "get()< new name" + LS + "writeStatement()< "
                        + LS, lastIndex);
        assertTrue(lastIndex <= index);
        lastIndex = index;
    }

    public void testGetPersistenceDelegate_Null() {
        Encoder enc = new Encoder();
        PersistenceDelegate pd = enc.getPersistenceDelegate(null);
        assertNotNull(pd);
    }

    public void testGetPersistenceDelegate_ArrayClass() {
        Encoder enc = new Encoder();
        PersistenceDelegate pd = enc.getPersistenceDelegate(int[].class);
        assertFalse(pd instanceof DefaultPersistenceDelegate);
    }

    public void testGetPersistenceDelegate_ProxyClass() {
        Encoder enc = new Encoder();
        enc.getPersistenceDelegate(Proxy.getProxyClass(ClassLoader
                .getSystemClassLoader(), new Class[] { List.class }));
    }

    public void testGetPersistenceDelegate_BeanInfo() {
        Encoder enc = new Encoder();
        PersistenceDelegate pd = enc
                .getPersistenceDelegate(MockFooLiYang.class);
        assertTrue(pd instanceof DefaultPersistenceDelegate);
    }

    public void testGetPersistenceDelegate_Default() {
        Encoder enc = new Encoder();
        Encoder enc2 = new Encoder();

        PersistenceDelegate pd1 = enc.getPersistenceDelegate(SampleBean.class);
        assertTrue(pd1 instanceof DefaultPersistenceDelegate);

        PersistenceDelegate pd2 = enc.getPersistenceDelegate(SampleBean.class);
        assertTrue(pd2 instanceof DefaultPersistenceDelegate);

        PersistenceDelegate pd3 = enc2
                .getPersistenceDelegate(MockBean4Codec.class);
        assertTrue(pd3 instanceof DefaultPersistenceDelegate);

        assertSame(pd1, pd2);
        assertSame(pd1, pd3);
    }

    public void testSetPersistenceDelegate_Null() {
        // Regression for HARMONY-1304
        Encoder enc = new Encoder();
        PersistenceDelegate pd = enc.getPersistenceDelegate(EncoderTest.class);

        try {
            enc.setPersistenceDelegate(null, pd);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            enc.setPersistenceDelegate(EncoderTest.class, null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testSetPersistenceDelegate() {
        Encoder enc = new Encoder();
        PersistenceDelegate pd = enc.getPersistenceDelegate(EncoderTest.class);
        assertTrue(pd instanceof DefaultPersistenceDelegate);

        enc.setPersistenceDelegate(EncoderTest.class, new VerbosePD());
        assertTrue(enc.getPersistenceDelegate(EncoderTest.class) instanceof VerbosePD);

        Encoder enc2 = new Encoder();
        assertTrue(enc2.getPersistenceDelegate(EncoderTest.class) instanceof VerbosePD);
    }

    public void testGet_NullParam() {
        Encoder enc = new Encoder();

        assertNull(enc.get(null));
    }

    public void testGet_String() {
        Encoder enc = new Encoder();

        String str = "string";
        assertSame(str, enc.get(str));
    }

    public void testGet_Integer() {
        MockEncoder enc = new MockEncoder();

        Integer integer = new Integer(8);
        assertNull(enc.get(integer));
        enc.writeObject(integer);
        assertEquals(integer, enc.get(integer));
        assertNull(enc.get(new Integer(integer.intValue())));

        Double d = new Double(8);
        assertNull(enc.get(d));
        enc.writeObject(d);
        assertEquals(d, enc.get(d));
    }

    public void testRemove_Null() {
        Encoder enc = new Encoder();

        assertNull(enc.remove(null));
    }

    public void testRemove_String() {
        MockEncoder enc = new MockEncoder();

        String str = "string";
        assertSame(str, enc.get(str));
        assertNull(enc.remove(str));

        enc.writeObject(str);
        assertSame(str, enc.get(str));
        assertNull(enc.remove(str));
    }

    public void testRemove_Integer() {
        MockEncoder enc = new MockEncoder();

        Integer integer = new Integer(8);
        assertNull(enc.remove(integer));

        enc.writeObject(integer);
        assertEquals(integer, enc.get(integer));
        assertEquals(integer, enc.remove(integer));

        assertNull(enc.get(integer));
        assertNull(enc.remove(integer));
    }

}
