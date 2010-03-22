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

import javax.swing.text.html.parser.ContentModel;

import javax.swing.text.parser.utils.DTDGetter;

import javax.swing.text.html.parser.Element;
import junit.framework.TestCase;

/**
 *  This tests checks the compatibility with the RI's ContentModel, only
 *  at semantic level. 
 *  The test recreate the construction of a content model in the RI, then
 *  call a method and finally compare the result with RI's result.
 * 
 *  The content models have the form (el1 bop el2)uop, 
 *  where el1 and el2 are elements, bop is a binary operator
 *  and upo is a unary operator
 * 
 **/
public class ContentModelSemanticTest extends TestCase {

    DTDGetter dtd = new DTDGetter("TestContentModelSemantic");

    public void testContentModelSemantic_0_1() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', ',', 0, 0, el1, el2);

        assertEquals(cm1.toString(), "(el1 , el2)?");
    }

    public void testContentModelSemantic_0_2() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', ',', 0, 0, el1, el2);

        assertEquals(cm1.empty(), true);
    }

    public void testContentModelSemantic_0_3() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', ',', 0, 0, el1, el2);

        assertNull(cm1.first());
    }

    public void testContentModelSemantic_0_4() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', ',', 0, 0, el1, el2);

        assertTrue(cm1.first(el1));
    }

    public void testContentModelSemantic_0_5() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', ',', 0, 0, el1, el2);

        assertFalse(cm1.first(el2));
    }

    public void testContentModelSemantic_1_1() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', ',', 0, 0, el1, el2);

        assertEquals(cm1.toString(), "(el1 , el2)+");
    }

    public void testContentModelSemantic_1_2() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', ',', 0, 0, el1, el2);

        assertEquals(cm1.empty(), false);
    }

    public void testContentModelSemantic_1_3() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', ',', 0, 0, el1, el2);

        assertEquals(cm1.first().getName(), "el1");
    }

    public void testContentModelSemantic_1_4() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', ',', 0, 0, el1, el2);

        assertTrue(cm1.first(el1));
    }

    public void testContentModelSemantic_1_5() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', ',', 0, 0, el1, el2);

        assertFalse(cm1.first(el2));

    }

    public void testContentModelSemantic_2_1() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', ',', 0, 0, el1, el2);

        assertEquals(cm1.toString(), "(el1 , el2)*");

    }

    public void testContentModelSemantic_2_2() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', ',', 0, 0, el1, el2);

        assertEquals(cm1.empty(), true);

    }

    public void testContentModelSemantic_2_3() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', ',', 0, 0, el1, el2);

        assertNull(cm1.first());

    }

    public void testContentModelSemantic_2_4() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', ',', 0, 0, el1, el2);

        assertTrue(cm1.first(el1));
    }

    public void testContentModelSemantic_2_5() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', ',', 0, 0, el1, el2);

        assertFalse(cm1.first(el2));
    }

    public void testContentModelSemantic_3_1() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', '|', 0, 0, el1, el2);

        assertEquals(cm1.toString(), "(el1 | el2)?");
    }

    public void testContentModelSemantic_3_2() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', '|', 0, 0, el1, el2);

        assertEquals(cm1.empty(), true);
    }

    public void testContentModelSemantic_3_3() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', '|', 0, 0, el1, el2);

        assertNull(cm1.first());
    }

    public void testContentModelSemantic_3_4() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', '|', 0, 0, el1, el2);

        assertTrue(cm1.first(el1));
    }

    public void testContentModelSemantic_3_5() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', '|', 0, 0, el1, el2);

        assertTrue(cm1.first(el2));
    }

    public void testContentModelSemantic_4_1() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', '|', 0, 0, el1, el2);

        assertEquals(cm1.toString(), "(el1 | el2)+");
    }

    public void testContentModelSemantic_4_2() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', '|', 0, 0, el1, el2);

        assertEquals(cm1.empty(), false);
    }

    public void testContentModelSemantic_4_3() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', '|', 0, 0, el1, el2);

        assertNull(cm1.first());
    }

    public void testContentModelSemantic_4_4() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', '|', 0, 0, el1, el2);

        assertTrue(cm1.first(el1));
    }

    public void testContentModelSemantic_4_5() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', '|', 0, 0, el1, el2);

        assertTrue(cm1.first(el2));
    }

    public void testContentModelSemantic_5_1() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', '|', 0, 0, el1, el2);

        assertEquals(cm1.toString(), "(el1 | el2)*");
    }

    public void testContentModelSemantic_5_2() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', '|', 0, 0, el1, el2);

        assertEquals(cm1.empty(), true);
    }

    public void testContentModelSemantic_5_3() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', '|', 0, 0, el1, el2);

        assertNull(cm1.first());
    }

    public void testContentModelSemantic_5_4() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', '|', 0, 0, el1, el2);

        assertTrue(cm1.first(el1));
    }

    public void testContentModelSemantic_5_5() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', '|', 0, 0, el1, el2);

        assertTrue(cm1.first(el2));
    }

    public void testContentModelSemantic_6_1() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', '&', 0, 0, el1, el2);

        assertEquals(cm1.toString(), "(el1 & el2)?");
    }

    public void testContentModelSemantic_6_2() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', '&', 0, 0, el1, el2);

        assertEquals(cm1.empty(), true);
    }

    public void testContentModelSemantic_6_3() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', '&', 0, 0, el1, el2);

        assertNull(cm1.first());
    }

    public void testContentModelSemantic_6_4() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', '&', 0, 0, el1, el2);

        assertTrue(cm1.first(el1));
    }

    public void testContentModelSemantic_6_5() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('?', '&', 0, 0, el1, el2);

        assertTrue(cm1.first(el2));
    }

    public void testContentModelSemantic_7_1() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', '&', 0, 0, el1, el2);

        assertEquals(cm1.toString(), "(el1 & el2)+");
    }

    public void testContentModelSemantic_7_2() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', '&', 0, 0, el1, el2);

        assertEquals(cm1.empty(), false);
    }

    public void testContentModelSemantic_7_3() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', '&', 0, 0, el1, el2);

        assertNull(cm1.first());
    }

    public void testContentModelSemantic_7_4() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', '&', 0, 0, el1, el2);

        assertTrue(cm1.first(el1));
    }

    public void testContentModelSemantic_7_5() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('+', '&', 0, 0, el1, el2);

        assertTrue(cm1.first(el2));
    }

    public void testContentModelSemantic_8_1() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', '&', 0, 0, el1, el2);

        assertEquals(cm1.toString(), "(el1 & el2)*");
    }

    public void testContentModelSemantic_8_2() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', '&', 0, 0, el1, el2);

        assertEquals(cm1.empty(), true);
    }

    public void testContentModelSemantic_8_3() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', '&', 0, 0, el1, el2);

        assertNull(cm1.first());
    }

    public void testContentModelSemantic_8_4() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', '&', 0, 0, el1, el2);

        assertTrue(cm1.first(el1));
    }

    public void testContentModelSemantic_8_5() {
        Element el1 = newElement("el1");
        Element el2 = newElement("el2");
        ContentModel cm1 = newContentModel('*', '&', 0, 0, el1, el2);

        assertTrue(cm1.first(el2));
    }

    Element newElement(String name) {
        return dtd.defineElement(name, 0, false, false, null, null, null, null);
    }

    private ContentModel newContentModel(int op1, int op2, int opel1,
            int opel2, Element el1, Element el2) {
        ContentModel cmel2 = new ContentModel(opel2, el2, null);
        ContentModel cmel1 = new ContentModel(opel1, el1, cmel2);
        ContentModel cm2 = new ContentModel(op2, cmel1, null);

        return new ContentModel(op1, cm2, null);
    }
}
