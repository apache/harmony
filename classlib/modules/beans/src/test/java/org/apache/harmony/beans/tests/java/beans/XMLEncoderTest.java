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

import java.awt.SystemColor;
import java.awt.font.TextAttribute;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.ExceptionListener;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.Statement;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.apache.harmony.beans.tests.java.beans.EncoderTest.SampleBean;
import org.apache.harmony.beans.tests.support.AType;
import org.apache.harmony.beans.tests.support.StandardBean;
import org.apache.harmony.beans.tests.support.TestEventHandler;
import org.apache.harmony.beans.tests.support.mock.MockBean4Codec;
import org.apache.harmony.beans.tests.support.mock.MockBean4Owner_Owner;
import org.apache.harmony.beans.tests.support.mock.MockBean4Owner_Target;
import org.apache.harmony.beans.tests.support.mock.MockTreeMapClass;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Tests for XMLEncoder
 */
public class XMLEncoderTest extends TestCase {

    public static class DependencyBean {
        private int ints[] = new int[] { 1 };

        private Object ref;

        public int[] getInts() {
            return ints;
        }

        public void setInts(int[] ints) {
            this.ints = ints;
        }

        public Object getRef() {
            return ref;
        }

        public void setRef(Object ref) {
            this.ref = ref;
        }
    }

    public static class VerboseEncoder extends XMLEncoder {

        private PrintWriter out;

        private boolean ident;

        public VerboseEncoder() {
            this(new PrintWriter(System.out, true), true);
        }

        public VerboseEncoder(PrintWriter out, boolean ident) {
            super(System.out);
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
        public PersistenceDelegate getPersistenceDelegate(Class<?> type) {
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
            out.println(identStr + "writeStatement()> " + string(arg0));
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

    public XMLEncoderTest() {
        super();
    }

    public XMLEncoderTest(String s) {
        super(s);
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

    public void testWriteExpression_Scenario1() {
        XMLEncoder xmlEncoder = new XMLEncoder((OutputStream) null);
        try {
            xmlEncoder.writeExpression((Expression) null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    public void testWriteExpression_Scenario2() {
        XMLEncoder xmlEncoder = new XMLEncoder(new ByteArrayOutputStream());
        try {
            xmlEncoder.writeExpression((Expression) null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    public void testWriteStatement() {
        // covered by testWriteStatement

         //Regression for HARMONY-1521
         //no exception expected
         new XMLEncoder(new ByteArrayOutputStream()).writeStatement(null);
    }

    public void testWriteObject_Null() throws Exception {
        assertCodedXML(null, "/xml/null.xml");
    }

    public void testWriteObject_Integer() throws Exception {
        assertCodedXML(new Integer(3), "/xml/int.xml");
    }

    public void testWriteObject_StringCodec() throws Exception {
        SampleBean b = new SampleBean();
        b.setMyid("<Li Yang> & \"liyang'");
        SampleBean c = new SampleBean();
        c.setMyid("a child");
        b.setRef(c);
        assertCodedXML(b, "/xml/SampleBean_StringCodec.xml");
    }

    public void testWriteObject_IntArray() throws Exception {
        assertCodedXML(new int[] { 1, 2, 3 }, "/xml/IntArray.xml");
    }

    public void testWriteObject_PropertyDependency() throws Exception {
        DependencyBean b = new DependencyBean();
        b.getInts()[0] = 888;
        b.setRef(b.getInts());
        assertCodedXML(b, "/xml/DependencyBean.xml");
    }

    public void testWriteObject_NoChange() throws Exception {
        assertCodedXML(new MockBean4Codec(), "/xml/MockBean4Codec_NoChange.xml");
    }

    public void testWriteObject_BornFriendChange() throws Exception {
        MockBean4Codec b = new MockBean4Codec();
        b.getBornFriend().getZarr()[0] = 888;
        b.setNill(b.getBornFriend());

        assertCodedXML(b, "/xml/MockBean4Codec_BornFriendChange.xml");
    }

    /* RI fails on this. Because even though we have change the class
       eception, it does not occurred on the output. */
    public void testWriteObject_ManyChanges() throws Exception {
        assertCodedXML(MockBean4Codec.getInstanceOfManyChanges(),
                "/xml/MockBean4Codec_ManyChanges.xml");
    }

    /* RI fails on this. Because even though we have change the class
       eception, it does not occurred on the output. */
    public void testWriteObject_ManyChanges_2() throws Exception {
        assertCodedXML(MockBean4Codec.getInstanceOfManyChanges2(),
                "/xml/MockBean4Codec_ManyChanges_2.xml");
    }

    public void testWriteObject_SetOwner() throws Exception {
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        XMLEncoder enc = new XMLEncoder(temp);

        MockBean4Owner_Target t = new MockBean4Owner_Target();
        MockBean4Owner_Owner o = new MockBean4Owner_Owner();
        t.setV(o);
        enc.setOwner(o);

        assertCodedXML(t, "/xml/MockBean4Owner_SetOwner.xml", temp, enc);

    }

    public void testWriteObject_SetOwnerWithWriteStatement() throws Exception {
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        XMLEncoder enc = new XMLEncoder(temp);

        MockBean4Owner_Target t = new MockBean4Owner_Target();
        MockBean4Owner_Owner o = new MockBean4Owner_Owner();
        t.setV(o);
        enc.setOwner(o);

        enc.writeStatement(new Statement(o, "loading", new Object[] {}));

        assertCodedXML(t, "/xml/MockBean4Owner_SetOwnerWithWriteStatement.xml",
                temp, enc);

    }

    /* TODO HARMONY fails on this test case
    public void testWriteObject_StaticField() throws Exception {
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        XMLEncoder enc = new XMLEncoder(temp);

        enc.setPersistenceDelegate(MockBean4StaticField.class,
                new MockBean4StaticField_PD());

        assertCodedXML(MockBean4StaticField.inst,
                "/xml/MockBean4StaticField.xml", temp, enc);

    }
    */

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testWriteObject_MockTreeMap() throws Exception {
        Map<String, TreeMap<String, String>> innerTreeMap = new MockTreeMapClass();
        TreeMap resultTreeMap = innerTreeMap.get("outKey");
        resultTreeMap.put("innerKey", "innerValue");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLEncoder xmlEncoder = new XMLEncoder(baos);

        assertCodedXML(innerTreeMap, "/xml/MockTreeMap.xml", baos, xmlEncoder);
        assertEquals(1, innerTreeMap.size());
    }

    public static enum Element {
        ELEMENTA, ELEMENTB, ELEMENTC
    }

    public static class MockEnumObject {

        Element element;

        public Element getElement() {
            return element;
        }

        public void setElement(Element element) {
            this.element = element;
        }
    }

    public void testWriteObject_EnumObject() throws Exception {
        MockEnumObject mockEnumObject = new MockEnumObject();
        mockEnumObject.setElement(Element.ELEMENTA);
        assertCodedXML(mockEnumObject, "/xml/MockEnumObject.xml");
    }

    public static class Mock2EnumObject {

        Element element = Element.ELEMENTA;

        public Element getElement() {
            return element;
        }

        public void setElement(Element element) {
            this.element = element;
        }
    }

    public static class ElementPersistenceDelegate extends
            DefaultPersistenceDelegate {
    }

    public void testWriteObject_2EnumObject() throws Exception {
        Mock2EnumObject mockEnumObject = new Mock2EnumObject();
        Encoder encoder = new Encoder();
        encoder.setPersistenceDelegate(Element.class,
                new ElementPersistenceDelegate());
        assertFalse(encoder.getPersistenceDelegate(Element.class) instanceof ElementPersistenceDelegate);
        mockEnumObject.setElement(Element.ELEMENTB);
        assertCodedXML(mockEnumObject, "/xml/Mock2EnumObject.xml");
    }

    public void testClose() {
        ByteArrayOutputStream out = new ByteArrayOutputStream() {
            boolean closeCalled = false;

            @Override
            public void close() throws IOException {
                if (closeCalled) {
                    throw new IOException("close already called!");
                }
                closeCalled = true;
                super.close();
            }
        };
        XMLEncoder enc = new XMLEncoder(out);
        enc.writeObject(new Integer(3));
        assertEquals(0, out.size());

        enc.close();

        assertTrue(out.size() > 0);
        try {
            out.close();
            fail();
        } catch (IOException e) {
            assertEquals("close already called!", e.getMessage());
        }
    }

    public void testFlush() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLEncoder enc = new XMLEncoder(out);
        Integer i = new Integer(3);
        enc.writeObject(i);
        assertEquals(0, out.size());
        assertNotNull(enc.get(i));

        enc.flush();

        assertTrue(out.size() > 0);
        assertNull(enc.get(i));
    }

    public void testXMLEncoder_Null() throws NullPointerException {
        new XMLEncoder(null);
    }

    public void testGetOwner() {
        XMLEncoder enc = new XMLEncoder(System.out);
        assertNull(enc.getOwner());
    }

    public void testSetOwner() {
        XMLEncoder enc = new XMLEncoder(System.out);
        Object owner = Boolean.FALSE;

        enc.setOwner(owner);
        assertSame(owner, enc.getOwner());

        enc.setOwner(null);
        assertNull(enc.getOwner());
    }

    private void assertCodedXML(Object obj, String xmlFile) throws Exception {
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        XMLEncoder enc = new XMLEncoder(temp);

        assertCodedXML(obj, xmlFile, temp, enc);
    }

    private void assertCodedXML(Object obj, String xmlFile,
            ByteArrayOutputStream temp, XMLEncoder enc) throws Exception {
        if (enc == null || temp == null) {
            temp = new ByteArrayOutputStream();
            enc = new XMLEncoder(temp);
        }
        enc.writeObject(obj);
        enc.close();
        
        assertXMLContent(obj, temp.toByteArray(), xmlFile);
            
    }

    private void assertXMLContent(Object obj, byte[] bytes, String xmlFile) throws Exception {
        InputStream refIn;
        InputStreamReader xml;

        XMLReader xmlReader;
        XMLReader refXmlReader;
        TestEventHandler handler = new TestEventHandler();
        TestEventHandler refHandler = new TestEventHandler();
        String saxParserClassName = System.getProperty("org.xml.sax.driver");
        String version = System.getProperty("java.version");
        xml = new InputStreamReader(new ByteArrayInputStream(bytes), "UTF-8");
        refIn = XMLEncoderTest.class.getResourceAsStream(xmlFile);
        if (refIn == null) {
            throw new Error("resource " + xmlFile + " not exist in "
                    + XMLEncoderTest.class.getPackage());
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(refIn, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while((line = br.readLine()) != null){
        	sb.append(line + "\n");
        }
        refIn.close();
        String refString = sb.toString();
        refString = refString.replace("${version}", version);
        if(obj != null){
        	refString = refString.replace("${classname}", obj.getClass().getName());
        }
        if (saxParserClassName == null) {
            xmlReader = XMLReaderFactory.createXMLReader();
            refXmlReader = XMLReaderFactory.createXMLReader();
        } else {
            xmlReader = XMLReaderFactory.createXMLReader(saxParserClassName);
            refXmlReader = XMLReaderFactory.createXMLReader(saxParserClassName);
        }

        xmlReader.setContentHandler(handler);
        xmlReader.setErrorHandler(handler);
        xmlReader.parse(new InputSource(xml));

        refXmlReader.setContentHandler(refHandler);
        refXmlReader.setErrorHandler(refHandler);
        refXmlReader.parse(new InputSource(new StringReader(refString)));

        assertEquals("Generated XML differs from the sample,", refHandler.root,
                handler.root);
    }

    /**
     * The test checks that java.lang.Boolean exemplar store is correct
     */
    public void testEncodeBoolean() {
        XMLEncoder e = new XMLEncoder(System.out);
        e.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        try {
            e.writeObject(new Boolean(true));
        } finally {
            e.close();
        }
    }

    /**
     * The test checks that java.lang.Byte exemplar store is correct
     */
    public void testEncodeByte() {
        XMLEncoder e = new XMLEncoder(System.out);
        e.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        try {
            e.writeObject(new Byte((byte) 123));
        } finally {
            e.close();
        }
    }

    /**
     * The test checks that java.lang.Character exemplar store is correct
     */
    public void testEncodeCharacter() {
        XMLEncoder e = new XMLEncoder(System.out);
        e.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        try {
            e.writeObject(new Character('a'));
        } finally {
            e.close();
        }
    }

    /**
     * The test checks that java.lang.Class exemplar store is correct
     */
    public void testEncodeClass() {
        XMLEncoder e = new XMLEncoder(System.out);
        e.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        try {
            e.writeObject(Object.class);
        } finally {
            e.close();
        }
    }

    /**
     * The test checks that java.lang.Double exemplar store is correct
     */
    public void testEncodeDouble() {
        XMLEncoder e = new XMLEncoder(System.out);
        e.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        try {
            e.writeObject(new Double(0.01));
        } finally {
            e.close();
        }
    }

    /**
     * The test checks that java.lang.Float exemplar store is correct
     */
    public void testEncodeFloat() {
        XMLEncoder e = new XMLEncoder(System.out);
        e.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        try {
            e.writeObject(new Float((float) 0.01));
        } finally {
            e.close();
        }
    }

    /**
     * The test checks that java.lang.Integer exemplar store is correct
     */
    public void testEncodeInteger() {
        XMLEncoder e = new XMLEncoder(System.out);
        e.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        try {
            e.writeObject(new Integer(1));
        } finally {
            e.close();
        }
    }

    /**
     * The test checks that java.lang.Long exemplar store is correct
     */
    public void testEncodeLong() {
        XMLEncoder e = new XMLEncoder(System.out);
        e.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        try {
            e.writeObject(new Long(1));
        } finally {
            e.close();
        }
    }

    /**
     * The test checks that java.lang.Short exemplar store is correct
     */
    public void testEncodeShort() {
        XMLEncoder e = new XMLEncoder(System.out);
        e.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        try {
            e.writeObject(new Short((short) 1));
        } finally {
            e.close();
        }
    }

    /**
     * The test checks that java.lang.String exemplar store is correct
     */
    public void testEncodeString() {
        XMLEncoder e = new XMLEncoder(System.out);
        e.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        try {
            e.writeObject(new String("hello"));
        } finally {
            e.close();
        }
    }

    /**
     * The test checks that array exemplar store is correct
     */
    public void testEncodeArray() {
        XMLEncoder e = new XMLEncoder(System.out);
        e.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        try {
            e.writeObject(new int[] { 1, 2, 3 });
        } finally {
            e.close();
        }
    }

    /**
     * The test checks that null exemplar store is correct
     */
    public void testEncodeNull() {
        XMLEncoder e = new XMLEncoder(System.out);
        e.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        try {
            e.writeObject(null);
        } finally {
            e.close();
        }
    }

    /**
     * The test checks that complex scenario store is correct
     */
    public void testEncodingScenario1() {
        XMLEncoder e = new XMLEncoder(System.out);
        e.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        StandardBean bean1 = new StandardBean("bean1");

        StandardBean bean2 = new StandardBean();
        bean2.setText(null);

        bean1.setPeer(bean2);
        bean2.setPeer(bean1);

        try {
            e.writeObject(bean1);
            e.writeObject(bean2);
        } finally {
            e.close();
        }
    }

    public static class MockClass {

        private Date date = null;

        public MockClass() {

        }

        public MockClass(Date date) {
            this.date = date;
        }

        public boolean equals(Object obj) {
            MockClass mockObj = (MockClass) obj;
            if (date != null && mockObj.date != null) {
                return date.equals(mockObj.date);
            }
            return false;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    public void testEncodingScenario2() {
        XMLEncoder xmlEncoder = new XMLEncoder(System.out);
        xmlEncoder.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        xmlEncoder.writeObject(new MockClass(new Date()));
    }

    /**
     * The test checks that encoder can handle writeExpression in initialize
     */
    public void testEncodeExpressionAsStatement() {
        XMLEncoder e = new XMLEncoder(System.out);
        e.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                fail("Exception " + e.getClass() + " is thrown: "
                        + e.getMessage());
            }
        });

        try {
            final Object object = new Object();
            e.setPersistenceDelegate(AType.class,
                    new DefaultPersistenceDelegate() {
                        @Override
                        protected void initialize(Class<?> type,
                                Object oldInstance, Object newInstance,
                                Encoder out) {
                            out.writeExpression(new Expression(object,
                                    oldInstance, "go", new Object[] {}));
                        }
                    });
            AType a = new AType();

            // e.writeObject(object);
            e.writeObject(a);
            e.writeObject(object);
        } finally {
            e.close();
        }
    }

    /**
     * This is a regression test for HARMONY-5707.
     */
    public void test5707() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(baos);
        TstBean5707 bean1 = new TstBean5707();

        encoder.writeObject(bean1);
        encoder.close();        
    }

    public class TstBean5707 {
        int val;

        public TstBean5707() {
            val = 0;
        }

        public TstBean5707(int n) {
            val = n;
        }

        public TstBean5707 getProp1() {
            return new TstBean5707(val);
        }

        public void setProp1(TstBean5707 val) {}

        public boolean equals(Object obj) {
            if (obj instanceof TstBean5707) {
                return ((TstBean5707) obj).val == val;
            }

            return false;
        }
    }

    public void testWriteObject_StaticFields() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLEncoder xmlEncoder = new XMLEncoder(bos);
        xmlEncoder.setPersistenceDelegate(MockObject.class,
                new MockObjectPersistenceDelegate());
        xmlEncoder.writeObject(MockObject.inst);
        xmlEncoder.writeObject(MockObject.inst);
        xmlEncoder.writeObject(SystemColor.activeCaption);
        xmlEncoder.writeObject(SystemColor.activeCaption);
        xmlEncoder.writeObject(TextAttribute.FAMILY);
        xmlEncoder.writeObject(TextAttribute.FAMILY);
        xmlEncoder.close();
        assertXMLContent(null, bos.toByteArray(), "/xml/StaticField.xml");
    }

    public static class MockObject {
        public static MockObject inst = new MockObject();
    }

    public static class MockObjectPersistenceDelegate extends
            PersistenceDelegate {
        protected Expression instantiate(Object oldInstance, Encoder enc) {
            Expression exp = null;
            try {
                exp = new Expression(MockObject.class.getField("inst"), "get",
                        new Object[] { null });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return exp;
        }
    }

    public void testWriteObject_ChangedObject() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLEncoder xmlEncoder = new XMLEncoder(bos);
        Sample sample = new Sample("oldName");
        xmlEncoder.writeObject(sample);
        sample.setName("newName");
        xmlEncoder.writeObject(sample);
        xmlEncoder.close();
        assertXMLContent(null, bos.toByteArray(), "/xml/ChangedObject.xml");
    }

    public static class Sample {

        String name;

        public Sample() {
            name = null;
        }

        public Sample(String n) {
            name = n;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public void testWriteObject_ClassID() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLEncoder xmlEncoder = new XMLEncoder(bos);
        ChildClass childClazz = new ChildClass();
        childClazz.setClazz(ChildClass.class);
        xmlEncoder.writeObject(childClazz);
        xmlEncoder.close();
        assertXMLContent(null, bos.toByteArray(), "/xml/ClassID.xml");
    }

    public static class ParentClass {

        Class<?> clazz = Collection.class;

        public Class<?> getClazz() {
            return clazz;
        }

        public void setClazz(Class<?> clazz) {
            this.clazz = clazz;
        }
    }

    public static class ChildClass extends ParentClass {
    }

    public void testWriteObject_ObjectID() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLEncoder xmlEncoder = new XMLEncoder(bos);
        ExampleA exampleAI = new ExampleA("exampleAI");
        xmlEncoder.writeObject(exampleAI);
        xmlEncoder.writeObject(exampleAI);
        ExampleA exampleAII = new ExampleA("exampleAI");
        xmlEncoder.writeObject(exampleAII);
        xmlEncoder.writeObject(exampleAII);

        ExampleB exampleBI = new ExampleB("exampleBI");
        xmlEncoder.writeObject(exampleBI);
        xmlEncoder.writeObject(exampleBI);
        ExampleB exampleBII = new ExampleB("exampleBII");
        xmlEncoder.writeObject(exampleBII);
        xmlEncoder.writeObject(exampleBII);

        ExampleC exampleCI = new ExampleC("exampleCI");
        xmlEncoder.writeObject(exampleCI);
        xmlEncoder.writeObject(exampleCI);
        ExampleC exampleCII = new ExampleC("exampleCII");
        xmlEncoder.writeObject(exampleCII);
        xmlEncoder.writeObject(exampleCII);

        xmlEncoder.close();

        assertXMLContent(null, bos.toByteArray(), "/xml/ObjectID.xml");
    }

    public static class ExampleA {

        private String name;

        public ExampleA() {

        }

        public ExampleA(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class ExampleB {

        private String name;

        public ExampleB() {

        }

        public ExampleB(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class ExampleC {

        private String name;

        public ExampleC() {

        }

        public ExampleC(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class MockCharProperty {
        private char property;

        public char getProperty() {
            return property;
        }

        public void setProperty(char property) {
            this.property = property;
        }

        public boolean equals(Object obj) {
            if (obj instanceof MockCharProperty) {
                return ((MockCharProperty) obj).property == (this.property);
            }
            return false;
        }
    }

    public void testMockCharProperty() {
        MockCharProperty expectedObj = new MockCharProperty();
        MockCharProperty actualObj;
        ByteArrayOutputStream baos;
        ByteArrayInputStream bais;
        XMLEncoder xmlEncoder;
        XMLDecoder xmlDecoder;
        char ch;
        for (int index = 1; index < 65536; index++) {
            ch = (char) index;
            if (invalidCharacter(ch)) {
                expectedObj.setProperty(ch);
                baos = new ByteArrayOutputStream();
                xmlEncoder = new XMLEncoder(baos);
                xmlEncoder.writeObject(expectedObj);
                xmlEncoder.close();
                assertTrue(baos.toString().contains("<char code=\"#"));

                bais = new ByteArrayInputStream(baos.toByteArray());
                xmlDecoder = new XMLDecoder(bais);
                actualObj = (MockCharProperty) xmlDecoder.readObject();
                xmlDecoder.close();
                assertEquals(expectedObj, actualObj);
            }
        }
    }

    public static class MockStringProperty {
        private String property;

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public boolean equals(Object obj) {
            if (obj instanceof MockStringProperty) {
                return ((MockStringProperty) obj).property
                        .equals(this.property);
            }
            return false;
        }
    }

    public void testMockStringProperty() {
        MockStringProperty expectedObj = new MockStringProperty();
        MockStringProperty actualObj;
        ByteArrayOutputStream baos;
        ByteArrayInputStream bais;
        XMLEncoder xmlEncoder;
        XMLDecoder xmlDecoder;
        char ch;
        for (int index = 0; index < 65536; index++) {
            ch = (char) index;
            if (invalidCharacter(ch)) {
                expectedObj.setProperty(stringWithChar(ch));
                baos = new ByteArrayOutputStream();
                xmlEncoder = new XMLEncoder(baos);
                xmlEncoder.writeObject(expectedObj);
                xmlEncoder.close();
                assertTrue(baos.toString().contains("<char code=\"#"));

                bais = new ByteArrayInputStream(baos.toByteArray());
                xmlDecoder = new XMLDecoder(bais);
                actualObj = (MockStringProperty) xmlDecoder.readObject();
                xmlDecoder.close();
                assertEquals(expectedObj, actualObj);
            }
        }
    }

    private String stringWithChar(char character) {
        return "a string with a " + character + " character";
    }

    private boolean invalidCharacter(char c) {
        return ((0x0000 <= c && c < 0x0009) || (0x000a < c && c < 0x000d)
                || (0x000d < c && c < 0x0020) || (0xd7ff < c && c < 0xe000) || c == 0xfffe);
    }

    public static class MockUnmodifiableCollection {

        private Collection<String> property = new ArrayList<String>();

        public Collection<String> getProperty() {
            return Collections.unmodifiableCollection(property);
        }

        public void setProperty(Collection<String> set) {
            property.clear();
            property.addAll(set);
        }
    }

    public void testWriteObject_MockUnmodifiableCollection() throws Exception {
        MockUnmodifiableCollection mockCollections = new MockUnmodifiableCollection();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$UnmodifiableCollection",
                propertyClass.getName());
        assertSamePD(propertyClass);
        mockCollections.setProperty(Arrays
                .asList(new String[] { "A", "B", "C" }));
        assertCodedXML(mockCollections, "/xml/MockUnmodifiableCollection.xml");
    }

    public static class MockUnmodifiableList {

        private List<String> property = new LinkedList<String>();

        public Collection<String> getProperty() {
            return Collections.unmodifiableList(property);
        }

        public void setProperty(Collection<String> set) {
            property.clear();
            property.addAll(set);
        }
    }

    public void testWriteObject_MockUnmodifiableList() throws Exception {
        MockUnmodifiableList mockCollections = new MockUnmodifiableList();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$UnmodifiableList",
                propertyClass.getName());
        assertSamePD(propertyClass);
        mockCollections.setProperty(Arrays
                .asList(new String[] { "A", "B", "C" }));
        assertCodedXML(mockCollections, "/xml/MockUnmodifiableList.xml");
    }

    public static class MockUnmodifiableRandomAccessList {

        private List<String> property = new ArrayList<String>();

        public Collection<String> getProperty() {
            return Collections.unmodifiableList(property);
        }

        public void setProperty(Collection<String> set) {
            property.clear();
            property.addAll(set);
        }
    }

    public void testWriteObject_MockUnmodifiableRandomAccessList()
            throws Exception {
        MockUnmodifiableRandomAccessList mockCollections = new MockUnmodifiableRandomAccessList();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$UnmodifiableRandomAccessList",
                propertyClass.getName());
        assertSamePD(propertyClass);
        mockCollections.setProperty(Arrays
                .asList(new String[] { "A", "B", "C" }));
        assertCodedXML(mockCollections,
                "/xml/MockUnmodifiableRandomAccessList.xml");
    }

    public static class MockUnmodifiableSet {

        private Set<String> property = new HashSet<String>();

        public Collection<String> getProperty() {
            return Collections.unmodifiableSet(property);
        }

        public void setProperty(Collection<String> set) {
            property.clear();
            property.addAll(set);
        }
    }

    public void testWriteObject_MockUnmodifiableSet() throws Exception {
        MockUnmodifiableSet mockCollections = new MockUnmodifiableSet();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$UnmodifiableSet",
                propertyClass.getName());
        assertSamePD(propertyClass);
        Set<String> prop = new HashSet<String>();
        prop.add("A");
        prop.add("B");
        prop.add("C");
        mockCollections.setProperty(prop);
        assertCodedXML(mockCollections, "/xml/MockUnmodifiableSet.xml");
    }

    public static class MockUnmodifiableSortedSet {

        private SortedSet<String> property = new TreeSet<String>();

        public Collection<String> getProperty() {
            return Collections.unmodifiableSortedSet(property);
        }

        public void setProperty(Collection<String> set) {
            property.clear();
            property.addAll(set);
        }
    }

    public void testWriteObject_MockUnmodifiableSortedSet() throws Exception {
        MockUnmodifiableSortedSet mockCollections = new MockUnmodifiableSortedSet();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$UnmodifiableSortedSet",
                propertyClass.getName());
        assertSamePD(propertyClass);
        Set<String> prop = new HashSet<String>();
        prop.add("A");
        prop.add("B");
        prop.add("C");
        mockCollections.setProperty(prop);
        assertCodedXML(mockCollections, "/xml/MockUnmodifiableSortedSet.xml");
    }

    public static class MockUnmodifiableMap {

        private Map<String, String> property = new HashMap<String, String>();

        public Map<String, String> getProperty() {
            return Collections.unmodifiableMap(property);
        }

        public void setProperty(Map<String, String> prop) {
            property.clear();
            property.putAll(prop);
        }
    }

    public void testWriteObject_MockUnmodifiableMap() throws Exception {
        MockUnmodifiableMap mockCollections = new MockUnmodifiableMap();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$UnmodifiableMap",
                propertyClass.getName());
        assertSamePD(propertyClass);
        Map<String, String> prop = new HashMap<String, String>();
        prop.put("A", "a");
        prop.put("B", "b");
        prop.put("C", "c");
        mockCollections.setProperty(prop);
        assertCodedXML(mockCollections, "/xml/MockUnmodifiableMap.xml");
    }

    public static class MockUnmodifiableSortedMap {

        private SortedMap<String, String> property = new TreeMap<String, String>();

        public Map<String, String> getProperty() {
            return Collections.unmodifiableSortedMap(property);
        }

        public void setProperty(Map<String, String> prop) {
            property.clear();
            property.putAll(prop);
        }
    }

    public void testWriteObject_MockUnmodifiableSortedMap() throws Exception {
        MockUnmodifiableSortedMap mockCollections = new MockUnmodifiableSortedMap();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$UnmodifiableSortedMap",
                propertyClass.getName());
        assertSamePD(propertyClass);
        Map<String, String> prop = new HashMap<String, String>();
        prop.put("A", "a");
        prop.put("B", "b");
        prop.put("C", "c");
        mockCollections.setProperty(prop);
        assertCodedXML(mockCollections, "/xml/MockUnmodifiableSortedMap.xml");
    }

    public static class MockSynchronizedCollection {

        private Collection<String> property = new ArrayList<String>();

        public Collection<String> getProperty() {
            return Collections.synchronizedCollection(property);
        }

        public void setProperty(Collection<String> prop) {
            property.clear();
            property.addAll(prop);
        }
    }

    public void testWriteObject_MockSynchronizedCollection() throws Exception {
        MockSynchronizedCollection mockCollections = new MockSynchronizedCollection();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$SynchronizedCollection",
                propertyClass.getName());
        assertSamePD(propertyClass);
        mockCollections.setProperty(Arrays
                .asList(new String[] { "A", "B", "C" }));
        assertCodedXML(mockCollections, "/xml/MockSynchronizedCollection.xml");
    }

    public static class MockSynchronizedList {

        private List<String> property = new LinkedList<String>();

        public Collection<String> getProperty() {
            return Collections.synchronizedList(property);
        }

        public void setProperty(Collection<String> prop) {
            property.clear();
            property.addAll(prop);
        }
    }

    public void testWriteObject_MockSynchronizedList() throws Exception {
        MockSynchronizedList mockCollections = new MockSynchronizedList();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$SynchronizedList",
                propertyClass.getName());
        assertSamePD(propertyClass);
        mockCollections.setProperty(Arrays
                .asList(new String[] { "A", "B", "C" }));
        assertCodedXML(mockCollections, "/xml/MockSynchronizedList.xml");
    }

    public static class MockSynchronizedRandomAccessList {

        private List<String> property = new ArrayList<String>();

        public Collection<String> getProperty() {
            return Collections.synchronizedList(property);
        }

        public void setProperty(Collection<String> prop) {
            property.clear();
            property.addAll(prop);
        }
    }

    public void testWriteObject_MockSynchronizedRandomAccessList()
            throws Exception {
        MockSynchronizedRandomAccessList mockCollections = new MockSynchronizedRandomAccessList();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$SynchronizedRandomAccessList",
                propertyClass.getName());
        assertSamePD(propertyClass);
        mockCollections.setProperty(Arrays
                .asList(new String[] { "A", "B", "C" }));
        assertCodedXML(mockCollections,
                "/xml/MockSynchronizedRandomAccessList.xml");
    }

    public static class MockSynchronizedSet {

        private Set<String> property = new HashSet<String>();

        public Collection<String> getProperty() {
            return Collections.synchronizedSet(property);
        }

        public void setProperty(Collection<String> prop) {
            property.clear();
            property.addAll(prop);
        }
    }

    public void testWriteObject_MockSynchronizedSet() throws Exception {
        MockSynchronizedSet mockCollections = new MockSynchronizedSet();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$SynchronizedSet",
                propertyClass.getName());
        assertSamePD(propertyClass);
        mockCollections.setProperty(Arrays
                .asList(new String[] { "A", "B", "C" }));
        assertCodedXML(mockCollections, "/xml/MockSynchronizedSet.xml");
    }

    public static class MockSynchronizedSortedSet {

        private SortedSet<String> property = new TreeSet<String>();

        public Collection<String> getProperty() {
            return Collections.synchronizedSortedSet(property);
        }

        public void setProperty(Collection<String> prop) {
            property.clear();
            property.addAll(prop);
        }
    }

    public void testWriteObject_MockSynchronizedSortedSet() throws Exception {
        MockSynchronizedSortedSet mockCollections = new MockSynchronizedSortedSet();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$SynchronizedSortedSet",
                propertyClass.getName());
        assertSamePD(propertyClass);
        mockCollections.setProperty(Arrays
                .asList(new String[] { "A", "B", "C" }));
        assertCodedXML(mockCollections, "/xml/MockSynchronizedSortedSet.xml");
    }

    public static class MockSynchronizedMap {

        private Map<String, String> property = new HashMap<String, String>();

        public Map<String, String> getProperty() {
            return Collections.synchronizedMap(property);
        }

        public void setProperty(Map<String, String> prop) {
            property.clear();
            property.putAll(prop);
        }
    }

    public void testWriteObject_MockSynchronizedMap() throws Exception {
        MockSynchronizedMap mockCollections = new MockSynchronizedMap();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$SynchronizedMap",
                propertyClass.getName());
        assertSamePD(propertyClass);
        Map<String, String> prop = new HashMap<String, String>();
        prop.put("A", "a");
        prop.put("B", "b");
        prop.put("C", "c");
        mockCollections.setProperty(prop);
        assertCodedXML(mockCollections, "/xml/MockSynchronizedMap.xml");
    }

    public static class MockSynchronizedSortedMap {

        private SortedMap<String, String> property = new TreeMap<String, String>();

        public Map<String, String> getProperty() {
            return Collections.synchronizedSortedMap(property);
        }

        public void setProperty(Map<String, String> prop) {
            property.clear();
            property.putAll(prop);
        }
    }

    public void testWriteObject_MockSynchronizedSortedMap() throws Exception {
        MockSynchronizedSortedMap mockCollections = new MockSynchronizedSortedMap();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$SynchronizedSortedMap",
                propertyClass.getName());
        assertSamePD(propertyClass);
        Map<String, String> prop = new HashMap<String, String>();
        prop.put("A", "a");
        prop.put("B", "b");
        prop.put("C", "c");
        mockCollections.setProperty(prop);
        assertCodedXML(mockCollections, "/xml/MockSynchronizedSortedMap.xml");
    }

    public static class MockCheckedCollection {

        private Collection<String> property = new ArrayList<String>();

        public Collection<String> getProperty() {
            return Collections.checkedCollection(property, String.class);
        }

        public void setProperty(Collection<String> prop) {
            property.clear();
            property.addAll(prop);
        }
    }

    public void testWriteObject_MockCheckedCollection() throws Exception {
        MockCheckedCollection mockCollections = new MockCheckedCollection();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$CheckedCollection",
                propertyClass.getName());
        assertSamePD(propertyClass);
        mockCollections.setProperty(Arrays
                .asList(new String[] { "A", "B", "C" }));
        assertCodedXML(mockCollections, "/xml/MockCheckedCollection.xml");
    }

    public static class MockCheckedList {

        private List<String> property = new LinkedList<String>();

        public Collection<String> getProperty() {
            return Collections.checkedList(property, String.class);
        }

        public void setProperty(Collection<String> prop) {
            property.clear();
            property.addAll(prop);
        }
    }

    public void testWriteObject_MockCheckedList() throws Exception {
        MockCheckedList mockCollections = new MockCheckedList();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$CheckedList",
                propertyClass.getName());
        assertSamePD(propertyClass);
        mockCollections.setProperty(Arrays
                .asList(new String[] { "A", "B", "C" }));
        assertCodedXML(mockCollections, "/xml/MockCheckedList.xml");
    }

    public static class MockCheckedRandomAccessList {

        private List<String> property = new ArrayList<String>();

        public Collection<String> getProperty() {
            return Collections.checkedList(property, String.class);
        }

        public void setProperty(Collection<String> prop) {
            property.clear();
            property.addAll(prop);
        }
    }

    public void testWriteObject_MockCheckedRandomAccessList() throws Exception {
        MockCheckedRandomAccessList mockCollections = new MockCheckedRandomAccessList();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$CheckedRandomAccessList",
                propertyClass.getName());
        assertSamePD(propertyClass);
        mockCollections.setProperty(Arrays
                .asList(new String[] { "A", "B", "C" }));
        assertCodedXML(mockCollections, "/xml/MockCheckedRandomAccessList.xml");
    }

    public static class MockCheckedSet {

        private Set<String> property = new HashSet<String>();

        public Collection<String> getProperty() {
            return Collections.checkedSet(property, String.class);
        }

        public void setProperty(Collection<String> prop) {
            property.clear();
            property.addAll(prop);
        }
    }

    public void testWriteObject_MockCheckedSet() throws Exception {
        MockCheckedSet mockCollections = new MockCheckedSet();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$CheckedSet",
                propertyClass.getName());
        assertSamePD(propertyClass);
        mockCollections.setProperty(Arrays
                .asList(new String[] { "A", "B", "C" }));
        assertCodedXML(mockCollections, "/xml/MockCheckedSet.xml");
    }

    public static class MockCheckedSortedSet {

        private SortedSet<String> property = new TreeSet<String>();

        public Collection<String> getProperty() {
            return Collections.checkedSortedSet(property, String.class);
        }

        public void setProperty(Collection<String> prop) {
            property.clear();
            property.addAll(prop);
        }
    }

    public void testWriteObject_MockCheckedSortedSet() throws Exception {
        MockCheckedSortedSet mockCollections = new MockCheckedSortedSet();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$CheckedSortedSet",
                propertyClass.getName());
        assertSamePD(propertyClass);
        mockCollections.setProperty(Arrays
                .asList(new String[] { "A", "B", "C" }));
        assertCodedXML(mockCollections, "/xml/MockCheckedSortedSet.xml");
    }

    public static class MockCheckedMap {

        private Map<String, String> property = new HashMap<String, String>();

        public Map<String, String> getProperty() {
            return Collections.checkedMap(property, String.class, String.class);
        }

        public void setProperty(Map<String, String> prop) {
            property.clear();
            property.putAll(prop);
        }
    }

    public void testWriteObject_MockCheckedMap() throws Exception {
        MockCheckedMap mockCollections = new MockCheckedMap();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$CheckedMap",
                propertyClass.getName());
        assertSamePD(propertyClass);
        Map<String, String> prop = new HashMap<String, String>();
        prop.put("A", "a");
        prop.put("B", "b");
        prop.put("C", "c");
        mockCollections.setProperty(prop);
        assertCodedXML(mockCollections, "/xml/MockCheckedMap.xml");
    }

    public static class MockCheckedSortedMap {

        private SortedMap<String, String> property = new TreeMap<String, String>();

        public Map<String, String> getProperty() {
            return Collections.checkedSortedMap(property, String.class,
                    String.class);
        }

        public void setProperty(Map<String, String> prop) {
            property.clear();
            property.putAll(prop);
        }
    }

    public void testWriteObject_MockCheckedSortedMap() throws Exception {
        MockCheckedSortedMap mockCollections = new MockCheckedSortedMap();
        Class<?> propertyClass = mockCollections.getProperty().getClass();
        assertEquals("java.util.Collections$CheckedSortedMap",
                propertyClass.getName());
        assertSamePD(propertyClass);
        Map<String, String> prop = new HashMap<String, String>();
        prop.put("A", "a");
        prop.put("B", "b");
        prop.put("C", "c");
        mockCollections.setProperty(prop);
        assertCodedXML(mockCollections, "/xml/MockCheckedSortedMap.xml");
    }

    public static class MockGetPropertyClass {

        private Set<String> property = new HashSet<String>();

        public Set<String> getProperty() {
            return new HashSet<String>(property);
        }

        public void setProperty(Set<String> set) {
            property.clear();
            property.addAll(set);
        }
    }

    public void testWriteObject_MockGetPropertyClass() throws Exception {
        MockGetPropertyClass mockCollections = new MockGetPropertyClass();
        assertSamePD(mockCollections.getProperty().getClass());
        Set<String> prop = new HashSet<String>();
        prop.add("A");
        prop.add("B");
        prop.add("C");
        mockCollections.setProperty(prop);
        assertCodedXML(mockCollections, "/xml/MockGetPropertyClass.xml");
    }

    public static class MockListImplements implements List<String> {

        private List<String> property = new ArrayList<String>();

        public List<String> getProperty() {
            return property;
        }

        public void setProperty(List<String> prop) {
            property = prop;
        }

        public boolean add(String o) {
            return property.add(o);
        }

        public void add(int index, String o) {
            property.add(index, o);
        }

        public boolean addAll(Collection<? extends String> c) {
            return property.addAll(c);
        }

        public boolean addAll(int index, Collection<? extends String> c) {
            return property.addAll(index, c);
        }

        public void clear() {
            property.clear();
        }

        public boolean contains(Object o) {
            return property.contains(o);
        }

        public boolean containsAll(Collection<?> c) {
            return property.containsAll(c);
        }

        public String get(int index) {
            return property.get(index);
        }

        public int indexOf(Object o) {
            return property.indexOf(o);
        }

        public boolean isEmpty() {
            return property.isEmpty();
        }

        public Iterator<String> iterator() {
            return property.iterator();
        }

        public int lastIndexOf(Object o) {
            return property.lastIndexOf(o);
        }

        public ListIterator<String> listIterator() {
            return property.listIterator();
        }

        public ListIterator<String> listIterator(int index) {
            return property.listIterator(index);
        }

        public boolean remove(Object o) {
            return property.remove(o);
        }

        public String remove(int index) {
            return property.remove(index);
        }

        public boolean removeAll(Collection<?> c) {
            return property.removeAll(c);
        }

        public boolean retainAll(Collection<?> c) {
            return property.retainAll(c);
        }

        public String set(int index, String o) {
            return property.set(index, o);
        }

        public int size() {
            return property.size();
        }

        public List<String> subList(int fromIndex, int toIndex) {
            return property.subList(fromIndex, toIndex);
        }

        public Object[] toArray() {
            return property.toArray();
        }

        public <T> T[] toArray(T[] a) {
            return property.toArray(a);
        }
    }

    public void testWriteObject_MockListImplements() throws Exception {
        List<String> mockListImplements = new MockListImplements();
        mockListImplements.add("A");
        assertCodedXML(mockListImplements, "/xml/MockListImplements.xml");
    }

    public static class MockSetImplements implements Set<String> {

        private Set<String> property;

        public MockSetImplements() {
            property = new HashSet<String>();
        }

        public Set<String> getProperty() {
            return property;
        }

        public void setProperty(Set<String> prop) {
            property = prop;
        }

        public boolean add(String o) {
            return property.add(o);
        }

        public void clear() {
            property.clear();
        }

        public boolean contains(final Object o) {
            return property.contains(o);
        }

        public boolean containsAll(final Collection<?> c) {
            return property.containsAll(c);
        }

        public boolean isEmpty() {
            return property.isEmpty();
        }

        public Iterator<String> iterator() {
            return property.iterator();
        }

        public boolean remove(final Object o) {
            return property.remove(o);
        }

        public boolean removeAll(final Collection<?> c) {
            return property.removeAll(c);
        }

        public boolean retainAll(final Collection<?> c) {
            return property.retainAll(c);
        }

        public int size() {
            return property.size();
        }

        public Object[] toArray() {
            return property.toArray();
        }

        public <T> T[] toArray(final T[] a) {
            return property.toArray(a);
        }

        public int hashCode() {
            return property.hashCode();
        }

        public boolean addAll(Collection<? extends String> c) {
            return property.addAll(c);
        }
    }

    public void testWriteObject_MockSetImplements() throws Exception {
        Set<String> mockSetImplements = new MockSetImplements();
        mockSetImplements.add("A");
        assertCodedXML(mockSetImplements, "/xml/MockSetImplements.xml");
    }

    private Encoder encoder = new Encoder();

    private void assertSamePD(Class<?> clazz) {
        assertSame(encoder.getPersistenceDelegate(clazz),
                encoder.getPersistenceDelegate(clazz));
    }

}