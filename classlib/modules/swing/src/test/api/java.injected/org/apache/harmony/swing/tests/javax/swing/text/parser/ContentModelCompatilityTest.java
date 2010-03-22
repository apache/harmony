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
 * @author Hugo Beilis
 * @author Leonardo Soler
 * @author Gabriel Miretti
 * @version 1.0
 */
package org.apache.harmony.swing.tests.javax.swing.text.parser;

import java.util.BitSet;
import java.util.Vector;

import javax.swing.text.html.parser.AttributeList;
import javax.swing.text.html.parser.ContentModel;
import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.Element;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import javax.swing.text.parser.utils.DTDGetter;

public class ContentModelCompatilityTest extends TestCase {
    DTD dtd;

    ContentModel cm;


    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel()' Verifies that
     * an instance is created. Fields content and next are null and type is 0.
     */
    public void testContentModel001() {
        cm = new ContentModel();
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(0, cm.type);
        assertNull(cm.next);
    }

    /*
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(Element)'
     */
    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(Element)' Null is
     * passed as parameter Verifies that an instance is created. Fields content
     * and next are null and type is 0.
     */
    public void testContentModelElement001() {
        cm = new ContentModel(null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(0, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(Element)'*
     * ContentModel(Element( "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1") .
     * It checks if an instance is created, content is equal to el ,type is 0
     * and next is null.
     */
    public void testContentModelElement003() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm = new ContentModel(el);
        assertNotNull(cm);
        assertEquals(el, cm.content);
        assertEquals(cm2, el.content);
        assertEquals(el, cm.content);
        assertEquals(0, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(Element)'*
     * ContentModel(Element( "",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)) . It checks if an instance is created,
     * content is equal to el ,type is 0 and next is null.
     */
    public void testContentModelElement004() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm = new ContentModel(el);
        assertNotNull(cm);
        assertEquals(el, cm.content);
        assertEquals(cm2, el.content);
        assertEquals(el, cm.content);
        assertEquals(0, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(Element)'*
     * ContentModel(Element( "1",0,true,true,null,null,null,null . It checks if
     * an instance is created, content is equal to el ,type is 0 and next is
     * null.
     */
    public void testContentModelElement005() {
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(el);
        assertNotNull(cm);
        assertEquals(el, cm.content);
        assertNull(el.content);
        assertEquals(el, cm.content);
        assertEquals(0, cm.type);
        assertNull(cm.next);
    }

    /*
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)'
     */
    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=0, ContentModel() Verifies that an
     * instance is created. content is equal to new ContentModel(), type is 0
     * and next is null
     */
    public void testContentModelIntContentModel001() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(0, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(0, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=Integer.MIN_VALUE, ContentModel() Verifies
     * that an instance is created. content is equal to ContentModel(), type is
     * Integer.MIN_VALUE and next is null
     */
    public void testContentModelIntContentModel002() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(Integer.MIN_VALUE, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(Integer.MIN_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=Integer.MAX_VALUE, ContentModel() Verifies
     * that an instance is created. content is equal to ContentModel(), type is
     * Integer.MAX_VALUE and next is null
     */
    public void testContentModelIntContentModel003() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(Integer.MAX_VALUE, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(Integer.MAX_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=42, ContentModel() Verifies that an
     * instance is created. content is equal to ContentModel(), type is 42 and
     * next is null
     */
    public void testContentModelIntContentModel004() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(42, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(42, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=43, ContentModel() Verifies that an
     * instance is created. content is equal to ContentModel(), type is 43 and
     * next is null
     */
    public void testContentModelIntContentModel005() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(43, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(43, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=44, ContentModel() Verifies that an
     * instance is created. content is equal to ContentModel(), type is 44 and
     * next is null
     */
    public void testContentModelIntContentModel006() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(44, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(44, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=63, ContentModel() Verifies that an
     * instance is created. content is equal to ContentModel(), type is 63 and
     * next is null
     */
    public void testContentModelIntContentModel007() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(63, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(63, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=124, ContentModel() Verifies that an
     * instance is created. content is equal to ContentModel(), type is 124 and
     * next is null
     */
    public void testContentModelIntContentModel008() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(124, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(124, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=38, ContentModel() Verifies that an
     * instance is created. content is equal to ContentModel(), type is 38 and
     * next is null
     */
    public void testContentModelIntContentModel009() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(38, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(38, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=0, null Verifies that an instance is
     * created. content is equal to null, type is 0 and next is null
     */
    public void testContentModelIntContentModel010() {
        cm = new ContentModel(0, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(0, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=Integer.MIN_VALUE, null Verifies that an
     * instance is created. content is equal to null, type is Integer.MIN_VALUE
     * and next is null
     */
    public void testContentModelIntContentModel011() {
        cm = new ContentModel(Integer.MIN_VALUE, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(Integer.MIN_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=Integer.MAX_VALUE, null Verifies that an
     * instance is created. content is equal to null, type is Integer.MAX_VALUE
     * and next is null
     */
    public void testContentModelIntContentModel012() {
        cm = new ContentModel(Integer.MAX_VALUE, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(Integer.MAX_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=42, null Verifies that an instance is
     * created. content is equal to null, type is 42 and next is null
     */
    public void testContentModelIntContentModel013() {
        cm = new ContentModel(42, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(42, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=43, null Verifies that an instance is
     * created. content is equal to null, type is 43 and next is null
     */
    public void testContentModelIntContentModel014() {
        cm = new ContentModel(43, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(43, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=44, null Verifies that an instance is
     * created. content is equal to null, type is 44 and next is null
     */
    public void testContentModelIntContentModel015() {
        cm = new ContentModel(44, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(44, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=63, null Verifies that an instance is
     * created. content is equal to null, type is 63 and next is null
     */
    public void testContentModelIntContentModel016() {
        cm = new ContentModel(63, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(63, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=124, null Verifies that an instance is
     * created. content is equal to null, type is 124 and next is null
     */
    public void testContentModelIntContentModel017() {
        cm = new ContentModel(124, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(124, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=38, null Verifies that an instance is
     * created. content is equal to null, type is 38 and next is null
     */
    public void testContentModelIntContentModel018() {
        cm = new ContentModel(38, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(38, cm.type);
        assertNull(cm.next);
    }


    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=0, ContentModel(0,ContentModel()) Verifies
     * that an instance is created. content is equal to
     * ContentModel(0,ContentModel()), type is 0 and next is null
     */
    public void testContentModelIntContentModel028() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm3 = new ContentModel(0, cm2);
        cm = new ContentModel(0, cm3);
        assertNotNull(cm);
        assertEquals(cm3, cm.content);
        assertEquals(0, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=Integer.MIN_VALUE,
     * ContentModel(Integer.MAX_VALUE,ContentModel()) Verifies that an instance
     * is created. content is equal to
     * ContentModel(Integer.MAX_VALUE,ContentModel()), type is Integer.MIN_VALUE
     * and next is null
     */
    public void testContentModelIntContentModel029() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm3 = new ContentModel(Integer.MAX_VALUE, cm2);
        cm = new ContentModel(Integer.MIN_VALUE, cm3);
        assertNotNull(cm);
        assertEquals(cm3, cm.content);
        assertEquals(Integer.MIN_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=Integer.MAX_VALUE,
     * ContentModel(Integer.MIN_VALUE,ContentModel()) Verifies that an instance
     * is created. content is equal to
     * ContentModel(Integer.MIN_VALUE,ContentModel()), type is Integer.MAX_VALUE
     * and next is null
     */
    public void testContentModelIntContentModel030() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm3 = new ContentModel(Integer.MIN_VALUE, cm2);
        cm = new ContentModel(Integer.MAX_VALUE, cm3);
        assertNotNull(cm);
        assertEquals(cm3, cm.content);
        assertEquals(Integer.MAX_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=42, ContentModel(42,null) Verifies that an
     * instance is created. content is equal to ContentModel(42,null), type is
     * 42 and next is null
     */
    public void testContentModelIntContentModel031() {
        ContentModel cm2 = new ContentModel(42, null);
        cm = new ContentModel(42, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(42, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' Parameters type=43, ContentModel(42,null) Verifies that an
     * instance is created. content is equal to ContentModel(42,null), type is
     * 43 and next is null
     */
    public void testContentModelIntContentModel032() {
        ContentModel cm2 = new ContentModel(42, null);
        cm = new ContentModel(43, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(43, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' ContentModel(Element( "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1") .
     * It checks if an instance is created, content is equal to el ,type is 0
     * and next is null.
     */
    public void testContentModelIntContentModel034() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm2 = new ContentModel(el);
        ContentModel cm = new ContentModel(0, cm2);
        assertNotNull(cm);

        assertEquals(cm2, cm.content);
        assertEquals(0, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' ContentModel(Element( "",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)) . It checks if an instance is created,
     * content is equal to el ,type is 0 and next is null.
     */
    public void testContentModelIntContentModel035() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm2 = new ContentModel(el);
        ContentModel cm = new ContentModel(43, cm2);
        assertNotNull(cm);

        assertEquals(cm2, cm.content);
        assertEquals(43, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int,
     * ContentModel)' ContentModel(Element( "1",0,true,true,null,null,null,null ,
     * and a contentmodel is created. Then ContentModel(0,contentmodel) is
     * instantiated It checks if an instance is created, content is equal to el
     * ,type is 0 and next is null.
     */
    public void testContentModelIntContentModel036() {
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(el);
        ContentModel cm2 = new ContentModel(0, cm);
        assertNotNull(cm2);

        assertEquals(cm, cm2.content);
        assertEquals(0, cm2.type);
        assertNull(cm2.next);
    }

    /*
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)'
     */

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=0, ContentModel(), null Verifies that an
     * instance is created. content is equal to new ContentModel(), type is 0
     * and next is null
     */
    public void testContentModelIntObjectContentModel001() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(0, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(0, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MIN_VALUE, ContentModel(), null
     * Verifies that an instance is created. content is equal to ContentModel(),
     * type is Integer.MIN_VALUE and next is null
     */
    public void testContentModelIntObjectContentModel002() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(Integer.MIN_VALUE, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(Integer.MIN_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MAX_VALUE, ContentModel(), null
     * Verifies that an instance is created. content is equal to ContentModel(),
     * type is Integer.MAX_VALUE and next is null
     */
    public void testContentModelIntObjectContentModel003() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(Integer.MAX_VALUE, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(Integer.MAX_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=42, ContentModel(), null Verifies that an
     * instance is created. content is equal to ContentModel(), type is 42 and
     * next is null
     */
    public void testContentModelIntObjectContentModel004() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(42, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(42, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=43, ContentModel(), null Verifies that an
     * instance is created. content is equal to ContentModel(), type is 43 and
     * next is null
     */

    public void testContentModelIntObjectContentModel005() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(43, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(43, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=44, ContentModel(), null Verifies that an
     * instance is created. content is equal to ContentModel(), type is 44 and
     * next is null
     */
    public void testContentModelIntObjectContentModel006() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(44, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(44, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=63, ContentModel(), null Verifies that an
     * instance is created. content is equal to ContentModel(), type is 63 and
     * next is null
     */
    public void testContentModelIntObjectContentModel007() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(63, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(63, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=124, ContentModel(), null Verifies that an
     * instance is created. content is equal to ContentModel(), type is 124 and
     * next is null
     */
    public void testContentModelIntObjectContentModel008() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(124, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(124, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=38, ContentModel(), null Verifies that an
     * instance is created. content is equal to ContentModel(), type is 38 and
     * next is null
     */
    public void testContentModelIntObjectContentModel009() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(38, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(38, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=0, null, null Verifies that an instance is
     * created. content is equal to null, type is 0 and next is null
     */
    public void testContentModelIntObjectContentModel010() {
        cm = new ContentModel(0, null, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(0, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MIN_VALUE, null, null Verifies
     * that an instance is created. content is equal to null, type is
     * Integer.MIN_VALUE and next is null
     */
    public void testContentModelIntObjectContentModel011() {
        cm = new ContentModel(Integer.MIN_VALUE, null, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(Integer.MIN_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MAX_VALUE, null, null Verifies
     * that an instance is created. content is equal to null, type is
     * Integer.MAX_VALUE and next is null
     */
    public void testContentModelIntObjectContentModel012() {
        cm = new ContentModel(Integer.MAX_VALUE, null, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(Integer.MAX_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=42, null, null Verifies that an instance
     * is created. content is equal to null, type is 42 and next is null
     */
    public void testContentModelIntObjectContentModel013() {
        cm = new ContentModel(42, null, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(42, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=43, null, null Verifies that an instance
     * is created. content is equal to null, type is 43 and next is null
     */
    public void testContentModelIntObjectContentModel014() {
        cm = new ContentModel(43, null, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(43, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=44, null, null Verifies that an instance
     * is created. content is equal to null, type is 44 and next is null
     */
    public void testContentModelIntObjectContentModel015() {
        cm = new ContentModel(44, null, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(44, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=63, null, null Verifies that an instance
     * is created. content is equal to null, type is 63 and next is null
     */
    public void testContentModelIntObjectContentModel016() {
        cm = new ContentModel(63, null, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(63, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=124, null, null Verifies that an instance
     * is created. content is equal to null, type is 124 and next is null
     */
    public void testContentModelIntObjectContentModel017() {
        cm = new ContentModel(124, null, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(124, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=38, null, null Verifies that an instance
     * is created. content is equal to null, type is 38 and next is null
     */
    public void testContentModelIntObjectContentModel018() {
        cm = new ContentModel(38, null, null);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(38, cm.type);
        assertNull(cm.next);
    }


    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=0, ContentModel(0,ContentModel()),null
     * Verifies that an instance is created. content is equal to
     * ContentModel(0,ContentModel()), type is 0 and next is null
     */
    public void testContentModelIntObjectContentModel028() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm3 = new ContentModel(0, cm2);
        cm = new ContentModel(0, cm3, null);
        assertNotNull(cm);
        assertEquals(cm3, cm.content);
        assertEquals(0, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MIN_VALUE,
     * ContentModel(Integer.MAX_VALUE,ContentModel()),null Verifies that an
     * instance is created. content is equal to
     * ContentModel(Integer.MAX_VALUE,ContentModel()), type is Integer.MIN_VALUE
     * and next is null
     */
    public void testContentModelIntObjectContentModel029() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm3 = new ContentModel(Integer.MAX_VALUE, cm2);
        cm = new ContentModel(Integer.MIN_VALUE, cm3, null);
        assertNotNull(cm);
        assertEquals(cm3, cm.content);
        assertEquals(Integer.MIN_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MAX_VALUE,
     * ContentModel(Integer.MIN_VALUE,ContentModel()),null Verifies that an
     * instance is created. content is equal to
     * ContentModel(Integer.MIN_VALUE,ContentModel()), type is Integer.MAX_VALUE
     * and next is null
     */
    public void testContentModelIntObjectContentModel030() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm3 = new ContentModel(Integer.MIN_VALUE, cm2);
        cm = new ContentModel(Integer.MAX_VALUE, cm3, null);
        assertNotNull(cm);
        assertEquals(cm3, cm.content);
        assertEquals(Integer.MAX_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=42, ContentModel(42,null),null Verifies
     * that an instance is created. content is equal to ContentModel(42,null),
     * type is 42 and next is null
     */
    public void testContentModelIntObjectContentModel031() {
        ContentModel cm2 = new ContentModel(42, null);
        cm = new ContentModel(42, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(42, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=43, ContentModel(42,null),null Verifies
     * that an instance is created. content is equal to ContentModel(42,null),
     * type is 43 and next is null
     */
    public void testContentModelIntObjectContentModel032() {
        ContentModel cm2 = new ContentModel(42, null);
        cm = new ContentModel(43, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(43, cm.type);
        assertNull(cm.next);
    }


    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=0, ContentModel(), ContentModel() Verifies
     * that an instance is created. content is equal to new ContentModel(), type
     * is 0 and next is ContentModel()
     */
    public void testContentModelIntObjectContentModel034() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(0, cm2, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(0, cm.type);
        assertEquals(cm2, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MIN_VALUE, ContentModel(),
     * ContentModel() Verifies that an instance is created. content is equal to
     * ContentModel(), type is Integer.MIN_VALUE and next is null
     */
    public void testContentModelIntObjectContentModel035() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(Integer.MIN_VALUE, cm2, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(Integer.MIN_VALUE, cm.type);
        assertEquals(cm2, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MAX_VALUE, ContentModel(),
     * ContentModel() Verifies that an instance is created. content is equal to
     * ContentModel(), type is Integer.MAX_VALUE and next is ContentModel()
     */
    public void testContentModelIntObjectContentModel036() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(Integer.MAX_VALUE, cm2, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(Integer.MAX_VALUE, cm.type);
        assertEquals(cm2, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=42, ContentModel(), ContentModel()
     * Verifies that an instance is created. content is equal to ContentModel(),
     * type is 42 and next is ContentModel()
     */
    public void testContentModelIntObjectContentModel037() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(42, cm2, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(42, cm.type);
        assertEquals(cm2, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=43, ContentModel(), ContentModel()
     * Verifies that an instance is created. content is equal to ContentModel(),
     * type is 43 and next is ContentModel()
     */
    public void testContentModelIntObjectContentModel038() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(43, cm2, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(43, cm.type);
        assertEquals(cm2, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=44, ContentModel(), ContentModel()
     * Verifies that an instance is created. content is equal to ContentModel(),
     * type is 44 and next is ContentModel()
     */
    public void testContentModelIntObjectContentModel039() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(44, cm2, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(44, cm.type);
        assertEquals(cm2, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=63, ContentModel(), ContentModel()
     * Verifies that an instance is created. content is equal to ContentModel(),
     * type is 63 and next is ContentModel()
     */
    public void testContentModelIntObjectContentModel040() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(63, cm2, cm2);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(63, cm.type);
        assertEquals(cm2, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=124, ContentModel(), ContentModel()
     * Verifies that an instance is created. content is equal to ContentModel(),
     * type is 124 and next is ContentModel()
     */
    public void testContentModelIntObjectContentModel041() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm4 = new ContentModel();
        cm = new ContentModel(124, cm2, cm4);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(124, cm.type);
        assertEquals(cm4, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=38, ContentModel(), ContentModel()
     * Verifies that an instance is created. content is equal to ContentModel(),
     * type is 38 and next is ContentModel()
     */
    public void testContentModelIntObjectContentModel042() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm4 = new ContentModel();
        cm = new ContentModel(38, cm2, cm4);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(38, cm.type);
        assertEquals(cm4, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=38, new DTDGetter("hi"), null Verifies that
     * an instance is created. content is equal to new DTDGetter("hi"), type is 38
     * and next is null
     */
    public void testContentModelIntObjectContentModel043() {
        dtd = new DTDGetter("hi");
        cm = new ContentModel(38, dtd, null);
        assertNotNull(cm);
        assertNull(cm.next);
        assertEquals(38, cm.type);
        assertEquals(dtd, cm.content);
    }


    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=-38, (element is defined in a dtd as el=
     * "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")),
     * null Verifies that an instance is created. content is equal to element,
     * type is -38 and next is null
     */
    public void testContentModelIntObjectContentModel045() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm = new ContentModel(-38, el, null);
        assertNotNull(cm);
        assertEquals(el, cm.content);
        assertEquals(-38, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=0,
     * ContentModel(Element:"elemento",',',false,false,('*',new
     * ContentModel('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")),
     * null Verifies that an instance is created. content is equal to null, type
     * is 0 and next is null
     */
    public void testContentModelIntObjectContentModel046() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(3);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", '.', false, false, cm2,
                bs1, bs2, al);
        cm2 = new ContentModel(el);
        cm = new ContentModel(0, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(0, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MIN_VALUE, el=
     * "",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)), null Verifies that an instance is
     * created. content is equal to el= "",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)), type is Integer.MIN_VALUE and next is
     * null
     */
    public void testContentModelIntObjectContentModel047() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm = new ContentModel(Integer.MIN_VALUE, el, null);
        assertNotNull(cm);
        assertEquals(el, cm.content);
        assertEquals(Integer.MIN_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MAX_VALUE,
     * Element("1",0,true,true,null,null,null,null), null Verifies that an
     * instance is created. content is equal to
     * Element("1",0,true,true,null,null,null,null), type is Integer.MAX_VALUE
     * and next is null
     */
    public void testContentModelIntObjectContentModel048() {
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(Integer.MAX_VALUE, el, null);
        assertNotNull(cm);
        assertEquals(el, cm.content);
        assertEquals(Integer.MAX_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=42, ContentModel(
     * "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")),
     * null Verifies that an instance is created. content is equal to
     * ContentModel(("elemento",',',false,false,cm2,bs1,bs2,al)), type is 42 and
     * next is null
     */
    public void testContentModelIntObjectContentModel049() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm2 = new ContentModel(el);

        cm = new ContentModel(42, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(42, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=0,
     * ContentModel("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new
     * AttributeList(null))),ContentModel("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null))) Verifies that an instance is created.
     * content is equal to ContentModel(("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)))), type is 0 and next is
     * ("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)))
     */
    public void testContentModelIntObjectContentModel050() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm2 = new ContentModel(el);
        cm = new ContentModel(0, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(0, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MIN_VALUE, ContentModel(el=
     * "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1"),null
     * Verifies that an instance is created. content is equal to
     * ContentModel(DTD.elements), type is Integer.MIN_VALUE and next is null
     */

    public void testContentModelIntObjectContentModel051() {
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(el);
        ContentModel cm2 = new ContentModel(0, cm);
        cm = new ContentModel(Integer.MIN_VALUE, cm2, null);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(Integer.MIN_VALUE, cm.type);
        assertNull(cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=0, null, ContentModel() Verifies that an
     * instance is created. content is equal to null, type is 0 and next is
     * ContentModel()
     */
    public void testContentModelIntObjectContentModel052() {
        ContentModel cmnext = new ContentModel();
        cm = new ContentModel(0, null, cmnext);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(0, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MIN_VALUE, null, ContentModel()
     * Verifies that an instance is created. content is equal to null, type is
     * Integer.MIN_VALUE and next is ContentModel()
     */
    public void testContentModelIntObjectContentModel053() {
        ContentModel cmnext = new ContentModel();
        cm = new ContentModel(Integer.MIN_VALUE, null, cmnext);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(Integer.MIN_VALUE, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MAX_VALUE, null, new
     * ContentModel() Verifies that an instance is created. content is equal to
     * null, type is Integer.MAX_VALUE and next is new ContentModel()
     */
    public void testContentModelIntObjectContentModel054() {
        ContentModel cmnext = new ContentModel();
        cm = new ContentModel(Integer.MAX_VALUE, null, cmnext);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(Integer.MAX_VALUE, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=42, null, null Verifies that an instance
     * is created. content is equal to null, type is 42 and next is new
     * ContentModel()
     */
    public void testContentModelIntObjectContentModel055() {
        ContentModel cmnext = new ContentModel();
        cm = new ContentModel(42, null, cmnext);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(42, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=43, null, null Verifies that an instance
     * is created. content is equal to null, type is 43 and next is new
     * ContentModel()
     */
    public void testContentModelIntObjectContentModel056() {
        ContentModel cmnext = new ContentModel();
        cm = new ContentModel(43, null, cmnext);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(43, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=44, null, null Verifies that an instance
     * is created. content is equal to null, type is 44 and next is new
     * ContentModel()
     */
    public void testContentModelIntObjectContentModel057() {
        ContentModel cmnext = new ContentModel();
        cm = new ContentModel(44, null, cmnext);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(44, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=63, null, null Verifies that an instance
     * is created. content is equal to null, type is 63 and next is new
     * ContentModel()
     */
    public void testContentModelIntObjectContentModel058() {
        ContentModel cmnext = new ContentModel();
        cm = new ContentModel(63, null, cmnext);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(63, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=124, null, null Verifies that an instance
     * is created. content is equal to null, type is 124 and next is new
     * ContentModel()
     */
    public void testContentModelIntObjectContentModel059() {
        ContentModel cmnext = new ContentModel();
        cm = new ContentModel(124, null, cmnext);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(124, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=38, null, null Verifies that an instance
     * is created. content is equal to null, type is 38 and next is new
     * ContentModel()
     */
    public void testContentModelIntObjectContentModel060() {
        ContentModel cmnext = new ContentModel();
        cm = new ContentModel(38, null, cmnext);
        assertNotNull(cm);
        assertNull(cm.content);
        assertEquals(38, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=0, ContentModel(0,ContentModel()),new
     * ContentModel() Verifies that an instance is created. content is equal to
     * ContentModel(0,ContentModel()), type is 0 and next is new ContentModel()
     */
    public void testContentModelIntObjectContentModel070() {

        ContentModel cm2 = new ContentModel();
        ContentModel cm3 = new ContentModel(0, cm2);
        cm = new ContentModel(0, cm3, cm2);
        assertNotNull(cm);
        assertEquals(cm3, cm.content);
        assertEquals(0, cm.type);
        assertEquals(cm2, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MIN_VALUE,
     * ContentModel(Integer.MAX_VALUE,ContentModel()),new ContentModel()
     * Verifies that an instance is created. content is equal to
     * ContentModel(Integer.MAX_VALUE,ContentModel()), type is Integer.MIN_VALUE
     * and next is new ContentModel()
     */
    public void testContentModelIntObjectContentModel071() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm3 = new ContentModel(Integer.MAX_VALUE, cm2);
        cm = new ContentModel(Integer.MIN_VALUE, cm3, cm2);
        assertNotNull(cm);
        assertEquals(cm3, cm.content);
        assertEquals(Integer.MIN_VALUE, cm.type);
        assertEquals(cm2, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MAX_VALUE,
     * ContentModel(Integer.MIN_VALUE,ContentModel()),new ContentModel()
     * Verifies that an instance is created. content is equal to
     * ContentModel(Integer.MIN_VALUE,ContentModel()), type is Integer.MAX_VALUE
     * and next is new ContentModel()
     */
    public void testContentModelIntObjectContentModel072() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm3 = new ContentModel(Integer.MIN_VALUE, cm2);
        cm = new ContentModel(Integer.MAX_VALUE, cm3, cm2);
        assertNotNull(cm);
        assertEquals(cm3, cm.content);
        assertEquals(Integer.MAX_VALUE, cm.type);
        assertEquals(cm2, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=42, ContentModel(42,null),new
     * ContentModel() Verifies that an instance is created. content is equal to
     * ContentModel(42,null), type is 42 and next is new ContentModel()
     */
    public void testContentModelIntObjectContentModel073() {
        ContentModel cmnext = new ContentModel();
        ContentModel cm2 = new ContentModel(42, cmnext);
        cm = new ContentModel(42, cm2, cmnext);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(42, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=43, ContentModel(42,null),new
     * ContentModel() Verifies that an instance is created. content is equal to
     * ContentModel(42,null), type is 43 and next is new ContentModel()
     */
    public void testContentModelIntObjectContentModel074() {
        ContentModel cmnext = new ContentModel();
        ContentModel cm2 = new ContentModel(42, cmnext);
        cm = new ContentModel(43, cm2, cmnext);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(43, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=38, new DTDGetter("hi"), new ContentModel()
     * Verifies that an instance is created. content is equal to
     * new DTDGetter("hi"), type is 38 and next is new ContentModel()
     */
    public void testContentModelIntObjectContentModel076() {
        ContentModel cmnext = new ContentModel();
        dtd = new DTDGetter("hi");
        cm = new ContentModel(38, dtd, cmnext);
        assertNotNull(cm);
        assertEquals(cmnext, cm.next);
        assertEquals(38, cm.type);
        assertEquals(dtd, cm.content);
    }


    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=0,
     * ContentModel(Element:"elemento",',',false,false,('*',new
     * ContentModel('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")),
     * new ContentModel() Verifies that an instance is created. content is equal
     * to null, type is 0 and next is new ContentModel()
     */
    public void testContentModelIntObjectContentModel079() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cmnext = new ContentModel();
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm2 = new ContentModel(el);
        cm = new ContentModel(0, cm2, cmnext);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(0, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MIN_VALUE, el=
     * "",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)), new ContentModel() Verifies that an
     * instance is created. content is equal to el= "",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)), type is Integer.MIN_VALUE and next is
     * new ContentModel()
     */
    public void testContentModelIntObjectContentModel080() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cmnext = new ContentModel();
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm = new ContentModel(Integer.MIN_VALUE, el, cmnext);
        assertNotNull(cm);
        assertEquals(el, cm.content);
        assertEquals(Integer.MIN_VALUE, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MAX_VALUE,
     * Element("1",0,true,true,null,null,null,null), new ContentModel() Verifies
     * that an instance is created. content is equal to
     * Element("1",0,true,true,null,null,null,null), type is Integer.MAX_VALUE
     * and next is new ContentModel()
     */
    public void testContentModelIntObjectContentModel081() {
        ContentModel cmnext = new ContentModel();
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(Integer.MAX_VALUE, el, cmnext);
        assertNotNull(cm);
        assertEquals(el, cm.content);
        assertEquals(Integer.MAX_VALUE, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=42, ContentModel(
     * "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")),
     * new ContentModel() Verifies that an instance is created. content is equal
     * to ContentModel(("elemento",',',false,false,cm2,bs1,bs2,al)), type is 42
     * and next is new ContentModel()
     */
    public void testContentModelIntObjectContentModel082() {
        ContentModel cmnext = new ContentModel();
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm2 = new ContentModel(el);

        cm = new ContentModel(42, cm2, cmnext);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(42, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=0,
     * ContentModel("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new
     * AttributeList(null))),ContentModel("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null))),new ContentModel() Verifies that an
     * instance is created. content is equal to
     * ContentModel(("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)))), type is 0 and next is
     * ("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)))
     */
    public void testContentModelIntObjectContentModel083() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cmnext = new ContentModel();

        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm2 = new ContentModel(el);
        cm = new ContentModel(0, cm2, cmnext);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(0, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.ContentModel(int, Object,
     * ContentModel)' Parameters type=Integer.MIN_VALUE, ContentModel(el=
     * "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1"),new
     * ContentModel() Verifies that an instance is created. content is equal to
     * ContentModel(DTD.elements), type is Integer.MIN_VALUE and next is new
     * ContentModel()
     */

    public void testContentModelIntObjectContentModel084() {
        ContentModel cmnext = new ContentModel();
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(el);
        ContentModel cm2 = new ContentModel(0, cm);
        cm = new ContentModel(Integer.MIN_VALUE, cm2, cmnext);
        assertNotNull(cm);
        assertEquals(cm2, cm.content);
        assertEquals(Integer.MIN_VALUE, cm.type);
        assertEquals(cmnext, cm.next);
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * ContentModel() Expected result: NullPointerException
     */
    public void testToString001() {
        try {
            cm = new ContentModel();
            cm.toString();
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        } 
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * ContentModel(null) Expected result: NullPointerException
     */
    public void testToString002() {
        try {
            cm = new ContentModel(null);
            cm.toString();

            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * ContentModel(Element("elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")
     * Expected: "elemento"
     */
    public void testToString004() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm = new ContentModel(el);

        assertEquals("elemento", cm.toString());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * ContentModel(Element("",'*',true,false,cm2,bs1,bs2,al)) ""
     */

    public void testToString005() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm = new ContentModel(el);

        assertEquals("", cm.toString());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * ContentModel(Element("1",0,true,true,null,null,null,null)) Expected: "1"
     */
    public void testToString006() {
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(el);

        assertEquals("1", cm.toString());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * ContentModel(0,cm2) Should throw NullPointerException
     */
    public void testToString007() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(0, cm2);
            cm.toString();
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * Parameters type=Integer.MIN_VALUE, ContentModel() Should throw
     * NullPointerException
     */
    public void testToString008() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(Integer.MIN_VALUE, cm2);
            cm.toString();
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * Parameters type=Integer.MAX_VALUE, ContentModel() Should throw
     * NullPointerException
     */
    public void testToString009() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(Integer.MAX_VALUE, cm2);
            cm.toString();
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * Parameters type=42, ContentModel() Should throw NullPointerException
     */
    public void testToString010() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(42, cm2);
            cm.toString();
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * Parameters type=43, null Expected result is "null+"
     */
    public void testToString011() {
        cm = new ContentModel(43, null);
        assertEquals("null+", cm.toString());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * Parameters type=44, null Expected result is "()"
     */
    public void testToString012() {
            cm = new ContentModel(44, null);
            assertEquals("()", cm.toString());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * Parameters type=0, ContentModel(0,ContentModel()) Should throw
     * NullPointerException
     */
    public void testToString016() {
        try {
            ContentModel cm2 = new ContentModel();
            ContentModel cm3 = new ContentModel(0, cm2);
            cm = new ContentModel(0, cm3);
            cm.toString();

            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        } 
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * Parameters type=Integer.MIN_VALUE,
     * ContentModel(Integer.MAX_VALUE,ContentModel()) Should throw
     * NullPointerException
     */
    public void testToString017() {
        try {
            ContentModel cm2 = new ContentModel();
            ContentModel cm3 = new ContentModel(Integer.MAX_VALUE, cm2);
            cm = new ContentModel(Integer.MIN_VALUE, cm3);
            cm.toString();
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'l(int,
     * ContentModel)' Parameters type=Integer.MAX_VALUE,
     * ContentModel(Integer.MIN_VALUE,ContentModel()) Should throw
     * NullPointerException
     */
    public void testToString018() {
        try {
            ContentModel cm2 = new ContentModel();
            ContentModel cm3 = new ContentModel(Integer.MIN_VALUE, cm2);
            cm = new ContentModel(Integer.MAX_VALUE, cm3);
            cm.toString();
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * Parameters type=42, ContentModel(42,null) Expected result is "null**"
     */
    public void testToString019() {
        ContentModel cm2 = new ContentModel(42, null);
        cm = new ContentModel(42, cm2);
        assertEquals("null**", cm.toString());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * Parameters type=43, ContentModel(42,null) Expected result is "null*+"
     */
    public void testToString020() {
        ContentModel cm2 = new ContentModel(42, null);
        cm = new ContentModel(43, cm2);
        assertEquals("null*+", cm.toString());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * ContentModel(Element( "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1") .
     * Expected result is "elemento"
     */
    public void testToString022() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm2 = new ContentModel(el);
        ContentModel cm = new ContentModel(0, cm2);
        assertEquals("elemento", cm.toString());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * ContentModel(Element( "",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)) . Expected result is "+"
     */
    public void testToString023() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm2 = new ContentModel(el);
        ContentModel cm = new ContentModel(43, cm2);
        assertEquals("+", cm.toString());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * ContentModel(Element( "1",0,true,true,null,null,null,null , and a
     * contentmodel is created. Then ContentModel(0,contentmodel) is
     * instantiated Expected result is "1"
     */
    public void testToString024() {
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(el);
        ContentModel cm2 = new ContentModel(0, cm);
        assertEquals("1", cm.toString());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.toString()'
     * Parameters type='&', ContentModel('|',null) Expected result is "null|&"
     */
    public void testToString025() {
        ContentModel cm2 = new ContentModel('&', null);
        cm = new ContentModel('|', cm2);
        assertEquals("(())", cm.toString());
    }


    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * ContentModel() Expected result is false
     */
    public void testEmpty001() {
        cm = new ContentModel();
        assertFalse(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * ContentModel(null) Expected result is false
     */
    public void testEmpty002() {
        cm = new ContentModel(null);
        assertFalse(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * ContentModel(0,cm2) Expected result is false
     */
    public void testEmpty003() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(0, cm2);
        assertFalse(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * ContentModel(Element("elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1"))
     * Expected result is false
     */
    public void testEmpty004() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm = new ContentModel(el);
        assertFalse(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * ContentModel(Element("elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")
     * Expected result is false
     */
    public void testEmpty005() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm = new ContentModel(el);

        assertFalse(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * ContentModel(Element("1",0,true,true,null,null,null,null)) Expected
     * result is false
     */
    public void testEmpty006() {
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(el);
        assertFalse(cm.empty());
    }


    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * Parameters type=Integer.MIN_VALUE, ContentModel() Expected result is
     * false
     */
    public void testEmpty008() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(Integer.MIN_VALUE, cm2);
        assertFalse(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * Parameters type=Integer.MAX_VALUE, ContentModel() Expected result is
     * false
     */
    public void testEmpty009() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(Integer.MAX_VALUE, cm2);
        assertFalse(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * Parameters type=42, ContentModel() Expected result is true
     */
    public void testEmpty010() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(42, cm2);
        assertTrue(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * Parameters type=43, null Expected result is false
     */
    public void testEmpty011() {
        cm = new ContentModel(43, null);
        assertFalse(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * Parameters type=44, null Expected result is true
     */
    public void testEmpty012() {
        cm = new ContentModel(44, null);
        assertTrue(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * Parameters type=0, ContentModel(0,ContentModel()) Expected result is
     * false
     */
    public void testEmpty016() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm3 = new ContentModel(0, cm2);
        cm = new ContentModel(0, cm3);
        assertFalse(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * Parameters type=Integer.MIN_VALUE,
     * ContentModel(Integer.MAX_VALUE,ContentModel()) Expected result is false
     */
    public void testEmpty017() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm3 = new ContentModel(Integer.MAX_VALUE, cm2);
        cm = new ContentModel(Integer.MIN_VALUE, cm3);
        assertFalse(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * Parameters type=Integer.MAX_VALUE,
     * ContentModel(Integer.MIN_VALUE,ContentModel()) Expected result is false
     */
    public void testEmpty018() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm3 = new ContentModel(Integer.MIN_VALUE, cm2);
        cm = new ContentModel(Integer.MAX_VALUE, cm3);
        assertFalse(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * Parameters type=42, ContentModel(42,null) Expected result is True
     */
    public void testEmpty019() {
        ContentModel cm2 = new ContentModel(42, null);
        cm = new ContentModel(42, cm2);
        assertTrue(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * Parameters type=43, ContentModel(42,null) Expected result is true
     */
    public void testEmpty020() {
        ContentModel cm2 = new ContentModel(42, null);
        cm = new ContentModel(43, cm2);
        assertTrue(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * ContentModel(Element( "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")
     * Expected result is false
     */
    public void testEmpty022() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm2 = new ContentModel(el);
        ContentModel cm = new ContentModel(0, cm2);
        assertFalse(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * ContentModelel= "",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)) Expected result is false
     */
    public void testEmpty023() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm2 = new ContentModel(el);
        ContentModel cm = new ContentModel(43, cm2);
        assertFalse(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.empty()'
     * ContentModel(0,ContentModel(Element(
     * "1",0,true,true,null,null,null,null)) Expected result is false
     */
    public void testEmpty024() {
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(el);
        ContentModel cm2 = new ContentModel(0, cm);
        assertFalse(cm.empty());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * ContentModel() Expected: null
     */
    public void testFirst001() {
        cm = new ContentModel();
        assertNull(cm.first());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * ContentModel(null) Expected: null
     */
    public void testFirst002() {
        cm = new ContentModel(null);
        assertNull(cm.first());
    }


    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * ContentModel("elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1"))
     * Expected: "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")
     */
    public void testFirst004() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm = new ContentModel(el);
        assertEquals(el, cm.first());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * ContentModel(Element("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)) expected:
     * Element("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null))
     */
    public void testFirst005() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm = new ContentModel(el);
        assertEquals(el, cm.first());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * ContentModel(Element("1",0,true,true,null,null,null,null) expected:
     * Element("1",0,true,true,null,null,null,null)
     */
    public void testFirst006() {
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(el);
        assertEquals(el, cm.first());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * Parameters type=Integer.MIN_VALUE, ContentModel() Expected result is
     * ClassCastException
     */
    public void testFirst008() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(Integer.MIN_VALUE, cm2);
            Element result = cm.first();
            fail("Should raise ClassCastException, but was returned:" + result);
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * Parameters type=Integer.MAX_VALUE, ContentModel() Expected result is
     * ClassCastException
     */
    public void testFirst009() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(Integer.MAX_VALUE, cm2);
            Element result = cm.first();
            fail("Should raise ClassCastException, but was returned:" + result);
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * Parameters type=42, ContentModel() Expected result is null
     */
    public void testFirst010() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(42, cm2);
        assertNull(cm.first());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * Parameters type=43, null Expected result is NullPointerException
     */
    public void testFirst011() {
        try {
            cm = new ContentModel(43, null);
            assertNull(cm.first());
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * Parameters type=44, null Expected result is NullPointerException
     */
    public void testFirst012() {
        try {
            cm = new ContentModel(44, null);
            assertNull(cm.first());
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }


    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * Parameters type=0, ContentModel(0,ContentModel()) Expected result is
     * ClassCastException
     */
    public void testFirst016() {
        try {
            ContentModel cm2 = new ContentModel();
            ContentModel cm3 = new ContentModel(0, cm2);
            cm = new ContentModel(0, cm3);
            Element result = cm.first();
            fail("Should raise ClassCastException, but was returned:" + result);
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * Parameters type=Integer.MIN_VALUE,
     * ContentModel(Integer.MAX_VALUE,ContentModel()) Expected result is
     * ClassCastException
     */
    public void testFirst017() {
        try {
            ContentModel cm2 = new ContentModel();
            ContentModel cm3 = new ContentModel(Integer.MAX_VALUE, cm2);
            cm = new ContentModel(Integer.MIN_VALUE, cm3);
            Element result = cm.first();
            fail("Should raise ClassCastException, but was returned:" + result);
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * Parameters type=Integer.MAX_VALUE,
     * ContentModel(Integer.MIN_VALUE,ContentModel()) Expected result is
     * ClassCastException
     */
    public void testFirst018() {
        try {
            ContentModel cm2 = new ContentModel();
            ContentModel cm3 = new ContentModel(Integer.MIN_VALUE, cm2);
            cm = new ContentModel(Integer.MAX_VALUE, cm3);
            Element result = cm.first();
            fail("Should raise ClassCastException, but was returned:" + result);
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * Parameters type=42, ContentModel(42,null) Expected result is null
     */
    public void testFirst019() {
        ContentModel cm2 = new ContentModel(42, null);
        cm = new ContentModel(42, cm2);
        assertNull(cm.first());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * Parameters type=43, ContentModel(42,null) Expected result is null
     */
    public void testFirst020() {
        ContentModel cm2 = new ContentModel(42, null);
        cm = new ContentModel(43, cm2);
        assertNull(cm.first());
    }


    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * ContentModel(Element( "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1"))
     * Expected result Element( "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")
     */
    public void testFirst022() {
        try {
            dtd = new DTDGetter("TestContentModelSemantic");
            ContentModel cm2 = new ContentModel('*', new ContentModel());
            BitSet bs1 = new BitSet(0);
            bs1.set(1);
            BitSet bs2 = new BitSet(168);
            bs2.set(45);
            AttributeList al = new AttributeList("1");
            Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                    bs1, bs2, al);
            cm2 = new ContentModel(el);
            ContentModel cm = new ContentModel(0, cm2);
            assertEquals(el, cm.first());
            fail("Should raise ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * ContentModel(Element( "",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)) Expected result Element(
     * "",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null))
     */
    public void testFirst023() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm2 = new ContentModel(el);
        ContentModel cm = new ContentModel(43, cm2);
        assertEquals(el, cm.first());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first()'
     * ContentModel(Element( "1",0,true,true,null,null,null,null)) Then
     * ContentModel(0,contentmodel) is instantiated Expected: Element(
     * "1",0,true,true,null,null,null,null)
     */
    public void testFirst024() {
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(el);
        ContentModel cm2 = new ContentModel(0, cm);
        assertEquals(el, cm.first());
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel().first(null) Expected: True
     */
    public void testFirstObject001() {
        cm = new ContentModel();
        assertTrue(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(null).first(null) Expected: True
     */
    public void testFirstObject002() {
        cm = new ContentModel(null);
        assertTrue(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(null).first(ContentModel(null)) Expected: False
     */

    public void testFirstObject003() {
        cm = new ContentModel(null);
        assertFalse(cm.first(cm));
    }


    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel("elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1"))
     * "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")
     * Expected: true
     */
    public void testFirstObject005() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm = new ContentModel(el);
        assertTrue(cm.first(el));
        assertFalse(cm.first(cm));
        assertFalse(cm.first(cm2));
        assertFalse(cm.first(dtd));

        assertFalse(cm.first(bs1));
        assertFalse(cm.first(bs2));
        assertFalse(cm.first(al));
        assertFalse(cm.first(""));
        assertFalse(cm.first("elemento"));
        assertFalse(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null))) "",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)) Expected: true
     */

    public void testFirstObject006() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm = new ContentModel(el);
        assertTrue(cm.first(el));
        assertFalse(cm.first(cm));
        assertFalse(cm.first(cm2));
        assertFalse(cm.first(dtd));

        assertFalse(cm.first(bs1));
        assertFalse(cm.first(bs2));
        assertFalse(cm.first(al));
        assertFalse(cm.first(""));
        assertFalse(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(Element("1",0,true,true,null,null,null,null))
     * Element("1",0,true,true,null,null,null,null) Expected: true
     */

    public void testFirstObject007() {
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(el);
        assertTrue(cm.first(el));
        assertFalse(cm.first(cm));

        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertFalse(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(0, ContentModel()) parameter is the ContentModel in the
     * ContentModel Expected: true
     */
    public void testFirstObject008() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(0, cm2);

        assertFalse(cm.first(cm));
        assertTrue(cm.first(cm2));

        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertFalse(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(Integer.MIN_VALUE, ContentModel()) parameter is the
     * ContentModel in the ContentModel Expected: true
     */
    public void testFirstObject009() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(Integer.MIN_VALUE, cm2);

        assertFalse(cm.first(cm));
        assertTrue(cm.first(cm2));

        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertFalse(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(Integer.MAX_VALUE, ContentModel()) parameter is the
     * ContentModel in the ContentModel Expected: true
     */
    public void testFirstObject010() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(Integer.MAX_VALUE, cm2);

        assertFalse(cm.first(cm));
        assertTrue(cm.first(cm2));

        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertFalse(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(42, ContentModel()) null Expected: true
     */
    public void testFirstObject011() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(42, cm2);

        assertFalse(cm.first(cm));
        assertFalse(cm.first(cm2));
        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertTrue(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(43, ContentModel()) parameter is null Expected: true
     */
    public void testFirstObject012() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(43, cm2);

        assertFalse(cm.first(cm));
        assertFalse(cm.first(cm2));

        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertTrue(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(44, ContentModel()) parameter is null Expected: true
     */
    public void testFirstObject013() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(44, cm2);

        assertFalse(cm.first(cm));
        assertFalse(cm.first(cm2));

        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertTrue(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(63, ContentModel()) parameter is null Expected: true
     */
    public void testFirstObject014() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(63, cm2);
        assertFalse(cm.first(cm));
        assertFalse(cm.first(cm2));

        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertTrue(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(124, ContentModel()) parameter is null Expected:
     * NullPointerException
     */
    public void testFirstObject015() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(124, cm2);
            assertTrue(cm.first(null));
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(38, ContentModel()) parameter is null Expected:
     * NullPointerException
     */
    public void testFirstObject016() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(38, cm2);
            assertTrue(cm.first(null));
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(124, ContentModel()) parameter is same contentModel
     * Expected: ClassCastException
     */
    public void testFirstObject017() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(124, cm2);
            cm.first(cm);
            fail("Should raise ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(38, ContentModel()) parameter is same contentModel Expected:
     * ClassCastException
     */
    public void testFirstObject018() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(38, cm2);

            cm.first(cm);
            fail("Should raise ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(124, ContentModel()) parameter is ContentModel() Expected:
     * ClassCastException
     */
    public void testFirstObject019() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(124, cm2);
            boolean result = cm.first(cm2);
            fail("Should raise ClassCastException, but was returned:" + result);
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(38, ContentModel()) parameter is ContentModel() Expected:
     * ClassCastException
     */
    public void testFirstObject020() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(38, cm2);
            boolean result = cm.first(cm2);
            fail("Should raise ClassCastException, but was returned:" + result);
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(124, ContentModel()) parameter is "" Expected:
     * ClassCastException
     */
    public void testFirstObject021() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(124, cm2);
            cm.first("");
            fail("Should raise ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(38, ContentModel()) parameter is "" Expected:
     * ClassCastException
     */
    public void testFirstObject022() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(38, cm2);

            cm.first(dtd);

            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(0,null); parameter is null expected: true
     */
    public void testFirstObject023() {
        cm = new ContentModel(0, null);
        assertFalse(cm.first(cm));
        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertTrue(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(Integer.MIN_VALUE,null); parameter is null expected: true
     */
    public void testFirstObject024() {
        cm = new ContentModel(Integer.MIN_VALUE, null);
        assertFalse(cm.first(cm));
        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertTrue(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(Integer.MAX_VALUE,null); parameter is null expected: true
     */
    public void testFirstObject025() {
        cm = new ContentModel(Integer.MAX_VALUE, null);
        assertFalse(cm.first(cm));
        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertTrue(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(42,null); parameter is same ContentModel expected:
     * NullPointerException
     */
    public void testFirstObject026() {
        try {
            cm = new ContentModel(42, null);
            cm.first(cm);
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(43,null); parameter is null expected: NullPointerException
     */
    public void testFirstObject027() {
        try {
            cm = new ContentModel(43, null);
            cm.first(null);
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(44,null); parameter is null expected: false
     */
    public void testFirstObject028() {
        cm = new ContentModel(44, null);
        assertFalse(cm.first(cm));
        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertFalse(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(63,null); parameter is null expected: NullPointerException
     */
    public void testFirstObject029() {
        try {
            cm = new ContentModel(63, null);
            cm.first(null);
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(124,null); parameter is "" expected: ClassCastException
     */
    public void testFirstObject030() {
        try {
            cm = new ContentModel(124, null);

            boolean result = cm.first("");
            fail("Should raise ClassCastException, but was returned:" + result);
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(38,null); parameter is null expected: NullPointerException
     */
    public void testFirstObject031() {
        try {
            cm = new ContentModel(38, null);
            boolean result = cm.first(cm);
            fail("Should raise ClassCastException, but was returned:" + result);
        } catch (ClassCastException e) {
            // Expected
        }
    }


    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(0,ContentModel(0,ContentModel()) parameter:
     * ContentModel(0,ContentModel() expected: true
     */
    public void testFirstObject037() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm3 = new ContentModel(0, cm2);
        cm = new ContentModel(0, cm3);
        assertFalse(cm.first(cm2));
        assertTrue(cm.first(cm3));
        assertFalse(cm.first(cm));
        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertFalse(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(Integer.MAX_VALUE,ContentModel(Integer.MIN_VALUE,ContentModel())
     * parameter: DTD.elements expected: true
     */
    public void testFirstObject038() {
        ContentModel cm2 = new ContentModel();
        ContentModel cm3 = new ContentModel(Integer.MIN_VALUE, cm2);
        cm = new ContentModel(Integer.MAX_VALUE, cm3);
        assertFalse(cm.first(cm2));
        assertFalse(cm.first(cm));
        assertTrue(cm.first(cm3));
        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertFalse(cm.first(null));
    }



    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(0,ContentModel(43,ContentModel(Element("elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")
     * parameter: Element("elemento",',',false,false,cm2,bs1,bs2,al) expected:
     * true
     */
    public void testFirstObject040() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm2 = new ContentModel(el);
        ContentModel cm = new ContentModel(0, cm2);
        assertFalse(cm.first(cm));
        assertTrue(cm.first(cm2));
        assertFalse(cm.first(al));
        assertFalse(cm.first(el));
        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));
        assertFalse(cm.first(dtd));
        assertFalse(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(43,ContentModel(Element("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)))) parameter:
     * Element("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)) expected: true
     */
    public void testFirstObject041() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm2 = new ContentModel(el);
        ContentModel cm = new ContentModel(43, cm2);
        assertFalse(cm.first(cm));
        assertFalse(cm.first(cm2));
        assertFalse(cm.first(al));
        assertTrue(cm.first(el));
        assertFalse(cm.first(""));
        assertFalse(cm.first("1"));

        assertFalse(cm.first(dtd));
        assertFalse(cm.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(0,ContentModel(Element("1",0,true,true,null,null,null,null)))
     * parameter: ContentModel(Element("1",0,true,true,null,null,null,null))
     * expected: true
     */

    public void testFirstObject042() {
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(el);
        ContentModel cm2 = new ContentModel(0, cm);
        assertTrue(cm2.first(cm));
        assertFalse(cm2.first(cm2));
        assertFalse(cm2.first(el));
        assertFalse(cm2.first(""));
        assertFalse(cm2.first("1"));

        assertFalse(cm2.first(dtd));
        assertFalse(cm2.first(null));
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(42,ContentModel(42,null)) parameter: ContentModel(42,null)
     * expected: NullPointerException
     */
    public void testFirstObject055() {
        try {
            ContentModel cm2 = new ContentModel(42, null);
            cm = new ContentModel(42, cm2);
            cm.first(cm2);

            fail("Should raise NullPointerException");

        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(42,ContentModel(42,null)) parameter: ContentModel(42,null)
     * expected: NullPointerException
     */
    public void testFirstObject056() {
        try {
            ContentModel cm2 = new ContentModel(42, null);
            cm = new ContentModel(42, cm2);

            cm.first(cm);

            fail("Should raise NullPointerException");

        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(42,ContentModel(42,null)) parameter: "" expected:
     * NullPointerException
     */
    public void testFirstObject057() {
        try {
            ContentModel cm2 = new ContentModel(42, null);
            cm = new ContentModel(42, cm2);

            cm.first("");

            fail("Should raise NullPointerException");

        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(42,ContentModel(42,null)) parameter: null expected:
     * NullPointerException
     */
    public void testFirstObject058() {
        try {
            ContentModel cm2 = new ContentModel(42, null);
            cm = new ContentModel(42, cm2);

            cm.first(null);
            fail("Should raise NullPointerException");

        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(43,ContentModel(42,null)) parameter: ContentModel(43,null)
     * expected: NullPointerException
     */
    public void testFirstObject059() {
        try {
            ContentModel cm2 = new ContentModel(42, null);
            cm = new ContentModel(43, cm2);
            cm.first(cm2);

            fail("Should raise NullPointerException");

        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(43,ContentModel(42,null)) parameter: ContentModel(43,null)
     * expected: NullPointerException
     */
    public void testFirstObject060() {
        try {
            ContentModel cm2 = new ContentModel(42, null);
            cm = new ContentModel(43, cm2);

            cm.first(cm);

            fail("Should raise NullPointerException");

        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(43,ContentModel(42,null)) parameter: "" expected:
     * NullPointerException
     */
    public void testFirstObject061() {
        try {
            ContentModel cm2 = new ContentModel(42, null);
            cm = new ContentModel(43, cm2);

            cm.first("");

            fail("Should raise NullPointerException");

        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.first(Object)'
     * ContentModel(43,ContentModel(42,null)) parameter: null expected:
     * NullPointerException
     */
    public void testFirstObject062() {
        try {
            ContentModel cm2 = new ContentModel(42, null);
            cm = new ContentModel(43, cm2);

            cm.first(null);
            fail("Should raise NullPointerException");

        } catch (NullPointerException e) {
            // Expected
        }
    }

    /*
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     */

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel().getElements(null) Expected: NullPointerException
     */

    public void testGetElements001() {
        try {
            cm = new ContentModel();
            cm.getElements(null);
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(null).getElements(null) Expected: NullPointerException
     */
    public void testGetElements002() {
        try {
            cm = new ContentModel(null);
            cm.getElements(null);
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel("elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")).getElements(null)
     * Expected: NullPointerException
     */
    public void testGetElements004() {
        try {
            dtd = new DTDGetter("TestContentModelSemantic");
            ContentModel cm2 = new ContentModel('*', new ContentModel());
            BitSet bs1 = new BitSet(0);
            bs1.set(1);
            BitSet bs2 = new BitSet(168);
            bs2.set(45);
            AttributeList al = new AttributeList("1");
            Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                    bs1, bs2, al);
            cm = new ContentModel(el);

            cm.getElements(null);

            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)).getElements(null) Expected:
     * NullPointerException
     */
    public void testGetElements005() {
        try {
            dtd = new DTDGetter("TestContentModelSemantic");
            ContentModel cm2 = new ContentModel('*', new ContentModel());
            BitSet bs1 = new BitSet(128);
            bs1.set(1);
            BitSet bs2 = new BitSet(7);
            AttributeList al = new AttributeList("bigC", -2147483648, -1,
                    "value", new Vector(), new AttributeList(null));
            Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                    al);
            cm = new ContentModel(el);
            cm.getElements(null);
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel("1",0,true,true,null,null,null,null).getElements(null)
     * Expected: NullPointerException
     */
    public void testGetElements006() {
        try {
            dtd = new DTDGetter("TestContentModelSemantic");
            Element el = dtd.defineElement("1", 0, true, true, null, null,
                    null, null);
            cm = new ContentModel(el);
            cm.getElements(null);
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel().getElements(new Vector()) Expected: "[null]"
     */

    public void testGetElements007() {
        cm = new ContentModel();
        Vector v = new Vector();
        cm.getElements(v);
        assertEquals(1, v.size());
        assertEquals("[null]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(null).getElements(new Vector()) Expected: "[null]"
     */
    public void testGetElements008() {
        cm = new ContentModel(null);
        Vector v = new Vector();
        cm.getElements(v);
        assertEquals(1, v.size());
        assertEquals("[null]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel("elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")).getElements(new
     * Vector()) Expected: "elemento"
     */
    public void testGetElements010() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm = new ContentModel(el);
        Vector v = new Vector();
        cm.getElements(v);
        assertEquals("[elemento]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel("",',',false,false,('*',new
     * ContentModel()),BitSet(128).set(1),BitSet(7),AttributeList("bigC",-2147483648,-1,"value",new
     * Vector(),new AttributeList(null)).getElements(new Vector()) Expected: ""
     */
    public void testGetElements011() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm = new ContentModel(el);
        Vector v = new Vector();
        cm.getElements(v);
        assertEquals("[]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel("1",0,true,true,null,null,null,null).getElements(new
     * Vector()) Expected: "1"
     */
    public void testGetElements012() {
        dtd = new DTDGetter("TestContentModelSemantic");
        Element el = dtd.defineElement("1", 0, true, true, null, null,
                null, null);
        cm = new ContentModel(el);
        Vector v = new Vector();
        cm.getElements(v);
        assertEquals("[1]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel().getElements(new Vector("el")) Expected: "[el, null]"
     */

    public void testGetElements013() {
        Vector v = new Vector();
        v.add("el");
        cm = new ContentModel();

        cm.getElements(v);
        assertEquals(2, v.size());
        assertEquals("[el, null]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel("elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")).getElements(new
     * Vector()) method is called 4 times Expected: "[elemento, elemento,
     * elemento, elemento]"
     */
    public void testGetElements014() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm = new ContentModel(el);
        Vector v = new Vector();
        cm.getElements(v);
        cm.getElements(v);
        cm.getElements(v);
        cm.getElements(v);
        assertEquals("[elemento, elemento, elemento, elemento]", v
                .toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(0, ContentModel()) parameter: new Vector() expected:
     * ClassCastException
     */
    public void testGetElements015() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(0, cm2);
            Vector v = new Vector();
            cm.getElements(v);
            fail("Should raise ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(Integer.MIN_VALUE, ContentModel()) parameter: new Vector()
     * expected: ClassCastException
     */
    public void testGetElements016() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(Integer.MIN_VALUE, cm2);
            Vector v = new Vector();

            cm.getElements(v);
            fail("Should raise ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(Integer.MAX_VALUE, ContentModel()) parameter: new Vector()
     * expected: ClassCastException
     */
    public void testGetElements017() {
        try {
            ContentModel cm2 = new ContentModel();
            cm = new ContentModel(Integer.MAX_VALUE, cm2);
            Vector v = new Vector();
            cm.getElements(v);
            fail("Should raise ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(42, ContentModel()) parameter: new Vector() expected: null
     */
    public void testGetElements018() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(42, cm2);
        Vector v = new Vector();
        cm.getElements(v);
        assertEquals("[null]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(43, ContentModel()) parameter: new Vector() expected: null
     */
    public void testGetElements019() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(43, cm2);
        Vector v = new Vector();
        cm.getElements(v);
        assertEquals("[null]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(44, ContentModel()) parameter: new Vector() expected: null
     */
    public void testGetElements020() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(44, cm2);
        Vector v = new Vector();
        cm.getElements(v);
        assertEquals("[null]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(63, ContentModel()) parameter: new Vector() expected: null
     */
    public void testGetElements021() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(63, cm2);
        Vector v = new Vector();
        cm.getElements(v);
        assertEquals("[null]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(124, ContentModel()) parameter: new Vector() expected: null
     */
    public void testGetElements022() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(124, cm2);
        Vector v = new Vector();
        cm.getElements(v);
        cm.getElements(v);
        assertEquals("[null, null]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(38, ContentModel()) parameter: new Vector() expected: null
     */
    public void testGetElements023() {
        ContentModel cm2 = new ContentModel();
        cm = new ContentModel(38, cm2);
        Vector v = new Vector();
        cm.getElements(v);
        cm.getElements(v);
        assertEquals("[null, null]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(0,null) parameter: new Vector() expected: null
     */
    public void testGetElements024() {
        cm = new ContentModel(0, null);
        Vector v = new Vector();
        cm.getElements(v);
        assertEquals("[null]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(42,null) parameter: new Vector() expected:
     * NullPointerException
     */
    public void testGetElements025() {
        try {

            cm = new ContentModel(42, null);
            Vector v = new Vector();
            cm.getElements(v);

            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(43,null) parameter: new Vector() expected:
     * NullPointerException
     */
    public void testGetElements026() {
        try {
            cm = new ContentModel(43, null);
            Vector v = new Vector();
            cm.getElements(v);

            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(44,null) parameter: new Vector() expected: ""
     */
    public void testGetElements027() {
        cm = new ContentModel(44, null);
        Vector v = new Vector();
        cm.getElements(v);
        assertEquals("[]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(63,null) parameter: new Vector() expected:
     * NullPointerException
     */
    public void testGetElements028() {
        try {
            cm = new ContentModel(63, null);
            Vector v = new Vector();
            cm.getElements(v);
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(124,null) parameter: new Vector() expected: ""
     */
    public void testGetElements029() {
        cm = new ContentModel(124, null);
        Vector v = new Vector();
        assertNull(cm.content);
        assertEquals(124, cm.type);
        cm.getElements(v);
        assertEquals("[]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(38,null) parameter: new Vector() expected: ""
     */
    public void testGetElements030() {
        cm = new ContentModel(38, null);
        Vector v = new Vector();
        assertNull(cm.content);
        assertEquals(38, cm.type);
        cm.getElements(v);
        assertEquals("[]", v.toString());
    }


    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(0,ContentModel(0,ContentModel())) parameter: new Vector()
     * expected: ClassCastException
     */
    public void testGetElements038() {
        try {
            ContentModel cm2 = new ContentModel();
            ContentModel cm3 = new ContentModel(0, cm2);
            cm = new ContentModel(0, cm3);
            Vector v = new Vector();
            cm.getElements(v);
            fail("Should raise ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(Integer.MIN_VALUE,ContentModel(Integer.MIN_VALUE,ContentModel()))
     * parameter: new Vector() expected: ClassCastException
     */
    public void testGetElements039() {
        try {
            ContentModel cm2 = new ContentModel();
            ContentModel cm3 = new ContentModel(Integer.MAX_VALUE, cm2);
            cm = new ContentModel(Integer.MIN_VALUE, cm3);
            Vector v = new Vector();
            cm.getElements(v);
            fail("Should raise ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(42,ContentModel(42,null)) parameter: new Vector() expected:
     * NullPointerException
     */
    public void testGetElements040() {
        try {
            ContentModel cm2 = new ContentModel(42, null);
            cm = new ContentModel(42, cm2);
            Vector v = new Vector();
            cm.getElements(v);
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(43,ContentModel(42,null)) parameter: new Vector() expected:
     * NullPointerException
     */
    public void testGetElements041() {
        try {
            ContentModel cm2 = new ContentModel(42, null);
            cm = new ContentModel(43, cm2);
            Vector v = new Vector();
            cm.getElements(v);
            fail("Should raise NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(0,ContentModel(43,ContentModel("elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")))
     * parameter: new Vector() expected: "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")
     */
    public void testGetElements043() {
        dtd = new DTDGetter("TestContentModelSemantic");
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(0);
        bs1.set(1);
        BitSet bs2 = new BitSet(168);
        bs2.set(45);
        AttributeList al = new AttributeList("1");
        Element el = dtd.defineElement("elemento", ',', false, false, cm2,
                bs1, bs2, al);
        cm2 = new ContentModel(el);
        ContentModel cm = new ContentModel(0, cm2);
        Vector v = new Vector();
        cm2.getElements(v);
        assertEquals("[elemento]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(0,ContentModel(43,ContentModel("elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")))
     * parameter: new Vector() expected: "elemento",',',false,false,('*',new
     * ContentModel()),BitSet(0).set(1),BitSet(168).set(45),AttributeList("1")
     */
    public void testGetElements044() {
        dtd = new DTDGetter("TestContentModelSemantic");
        Vector v = new Vector();
        ContentModel cm2 = new ContentModel('*', new ContentModel());
        BitSet bs1 = new BitSet(128);
        bs1.set(1);
        BitSet bs2 = new BitSet(7);
        AttributeList al = new AttributeList("bigC", -2147483648, -1,
                "value", new Vector(), new AttributeList(null));
        Element el = dtd.defineElement("", '*', true, false, cm2, bs1, bs2,
                al);
        cm2 = new ContentModel(el);
        ContentModel cm = new ContentModel(43, cm2);
        cm2.getElements(v);
        assertEquals("[]", v.toString());
    }

    /**
     * Test method for
     * 'org.apache.harmony.swing.tests.javax.swing.text.parser.ContentModel.getElements(Vector)'
     * ContentModel(0,ContentModel("1",0,true,true,null,null,null,null))
     * parameter: new Vector() expected: ClassCastException
     */
    public void testGetElements045() {
        try {
            dtd = new DTDGetter("TestContentModelSemantic");
            Element el = dtd.defineElement("1", 0, true, true, null, null,
                    null, null);
            cm = new ContentModel(el);
            ContentModel cm2 = new ContentModel(0, cm);
            Vector v = new Vector();
            cm2.getElements(v);
            fail("Should raise ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }
}
