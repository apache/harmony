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
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

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

    public static void main(String[] args) {

        // VerboseEncoder enc = new VerboseEncoder();
        //
        // MockBean4Codec b = new MockBean4Codec();
        // b.getBornFriend().getZarr()[0] = 888;
        // b.setNill(b.getBornFriend());
        //
        // enc.writeObject(b);
        // enc.flush();

        junit.textui.TestRunner.run(XMLEncoderTest.class);
    }

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

        @SuppressWarnings("unchecked")
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

    public void testWriteObject_MockTreeMap() throws Exception {
        Map<String, TreeMap<String, String>> innerTreeMap = new MockTreeMapClass();
        TreeMap resultTreeMap = innerTreeMap.get("outKey");
        resultTreeMap.put("innerKey", "innerValue");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLEncoder xmlEncoder = new XMLEncoder(baos);

        assertCodedXML(innerTreeMap, "/xml/MockTreeMap.xml", baos, xmlEncoder);
        assertEquals(1, innerTreeMap.size());
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
                        @SuppressWarnings("unchecked")
                        @Override
                        protected void initialize(Class type,
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
}