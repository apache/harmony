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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.text.html.parser;

import java.awt.Rectangle;
import java.util.Vector;

import javax.swing.SwingTestCase;

public class ContentModelTest extends SwingTestCase {
    ContentModel contentModel;
    Element[] elements;
    char[] types;
    Vector elemVec;
    ContentModel contentModel1;
    ContentModel contentModel2;
    ContentModel contentModel3;
    ContentModel contentModel4;
    ContentModel contentModel5;
    ContentModel contentModel6;
    ContentModel contentModel7;
    ContentModel contentModel8;
    ContentModel contentModel9;
    ContentModel contentModel10;
    ContentModel contentModel11;
    ContentModel contentModel12;
    ContentModel contentModel13;
    ContentModel contentModel14;


    protected void setUp() throws Exception {
        init();
        resetElemVec();
        super.setUp();
    }

    private void resetElemVec() {
        if (elemVec == null) {
            elemVec = new Vector();
        } else {
            elemVec.removeAllElements();
        }
    }

    private void init() {
        if (elements == null) {
            elements = new Element[7];
            for (int i = 0; i < 7; i++) {
                elements[i] = new Element();
                elements[i].name = Integer.toString(i);
            }
        }
        if (types == null) {
            types = new char[] {'*', '?', '+', ',', '|', '&'};
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testContentModelElement() {
        contentModel = new ContentModel(elements[0]);
        Utils.checkContentModel(contentModel, elements[0], 0, null);

        contentModel = new ContentModel(null);
        Utils.checkContentModel(contentModel, null, 0, null);
    }

    public void testContentModel() {
        contentModel = new ContentModel();
        Utils.checkContentModel(contentModel, null, 0, null);
    }

    public void testContentModelIntContentModel() {
        contentModel1 = new ContentModel(elements[0]);

        contentModel = new ContentModel('*', contentModel1);
        Utils.checkContentModel(contentModel, contentModel1, '*', null);

        contentModel = new ContentModel('?', contentModel1);
        Utils.checkContentModel(contentModel, contentModel1, '?', null);

        contentModel = new ContentModel('+', contentModel1);
        Utils.checkContentModel(contentModel, contentModel1, '+', null);
    }

    public void testContentModelIntObjectContentModel() {
        contentModel1 = new ContentModel(elements[0]);
        contentModel = new ContentModel(',', elements[1], contentModel1);
        Utils.checkContentModel(contentModel, elements[1], ',',
                                contentModel1);

        contentModel = new ContentModel('|', elements[1], contentModel1);
        Utils.checkContentModel(contentModel, elements[1], '|',
                                contentModel1);

        contentModel = new ContentModel('&', elements[1], contentModel1);
        Utils.checkContentModel(contentModel, elements[1], '&',
                                contentModel1);

        contentModel2 = new ContentModel(elements[1]);
        contentModel = new ContentModel(',', contentModel2, contentModel1);
        Utils.checkContentModel(contentModel, contentModel2, ',',
                                contentModel1);

        contentModel = new ContentModel('|', contentModel2, contentModel1);
        Utils.checkContentModel(contentModel, contentModel2, '|',
                                contentModel1);


        contentModel = new ContentModel('&', contentModel2, contentModel1);
        Utils.checkContentModel(contentModel, contentModel2, '&',
                                contentModel1);
    }

    public void testIllegalArgumentException_Object() {
        if (SwingTestCase.isHarmony()) {
            contentModel1 = new ContentModel(elements[0]);
            try {
                contentModel = new ContentModel(',', new Rectangle(),
                                            contentModel1);
                throwException("1");
            } catch (IllegalArgumentException e) {
            }

            try {
                contentModel = new ContentModel(',', null,
                                            contentModel1);
                throwException("2");
            } catch (IllegalArgumentException e) {
            }

            try {
                contentModel = new ContentModel('|', null,
                                            contentModel1);
                throwException("3");
            } catch (IllegalArgumentException e) {
            }

            try {
                contentModel = new ContentModel('&', null,
                                            contentModel1);
                throwException("4");
            } catch (IllegalArgumentException e) {
            }

            try {
                contentModel = new ContentModel(',', elements[2],
                                            null);
                throwException("5");
            } catch (IllegalArgumentException e) {
            }

            try {
                contentModel = new ContentModel('|', elements[2],
                                            null);
                throwException("6");
            } catch (IllegalArgumentException e) {
            }

            try {
                contentModel = new ContentModel('&', elements[2],
                                            null);
                throwException("7");
            } catch (IllegalArgumentException e) {
            }

            try {
                contentModel = new ContentModel('+', null);
                throwException("8");
            } catch (IllegalArgumentException e) {
            }

            try {
                contentModel = new ContentModel('*', null);
                throwException("9");
            } catch (IllegalArgumentException e) {
            }

            try {
                contentModel = new ContentModel('?', null);
                throwException("10");
            } catch (IllegalArgumentException e) {
            }
        }
    }

    public void testIllegalArgumentException_Type() {
        if (SwingTestCase.isHarmony()) {
            contentModel1 = new ContentModel(elements[0]);
            try {
                contentModel = new ContentModel(',', contentModel1);
                throwException("0");
            } catch (IllegalArgumentException e) {
            }
            try {
                contentModel = new ContentModel('&', contentModel1);
                throwException("1");
            } catch (IllegalArgumentException e) {
            }
            try {
                contentModel = new ContentModel('|', contentModel1);
                throwException("2");
            } catch (IllegalArgumentException e) {
            }

            try {
                contentModel = new ContentModel('a', contentModel1);
                throwException("3");
            } catch (IllegalArgumentException e) {
            }

            try {
                contentModel = new ContentModel('+', elements[0],
                                                contentModel1);
                throwException("4");
            } catch (IllegalArgumentException e) {
            }
            try {
                contentModel = new ContentModel('*', elements[0],
                                                contentModel1);
                throwException("5");
            } catch (IllegalArgumentException e) {
            }
            try {
                contentModel = new ContentModel('?', elements[0],
                                                contentModel1);
                throwException("6");
            } catch (IllegalArgumentException e) {
            }

            try {
                contentModel = new ContentModel('a', elements[0],
                                                contentModel1);
                throwException("7");
            } catch (IllegalArgumentException e) {
            }
        }
    }

    private void throwException(final String s) {
        assertFalse("IllegalArgumentException wasn't thrown:" + s, true);
    }


    public void testFirst() throws Exception{
        initContentModels();
        assertEquals(elements[1], contentModel1.first());
        assertEquals(elements[2], contentModel2.first());
        assertEquals(elements[3], contentModel3.first());
        assertEquals(elements[1], contentModel4.first());
        assertNull(contentModel5.first());
        assertNull(contentModel6.first());
        assertEquals(elements[1], contentModel7.first());
        assertNull(contentModel8.first());
        assertNull(contentModel9.first());
    }
    
    public void testFirst2() throws Exception{
        //regression for HARMONY-1350
        DTD dtd = DTD.getDTD("tmp");
        ContentModel model1 = new ContentModel (dtd.getElement(2));
        ContentModel model2 = new ContentModel (dtd.getElement(3));
        ContentModel model = new ContentModel ('&', model1, model2);
        assertNull(model.first());
    }


    public void testToString() {
        if (!SwingTestCase.isHarmony()) {
            return;
        }
        initContentModels();
        assertEquals("1", contentModel1.toString());
        assertEquals("2", contentModel2.toString());
        assertEquals("3", contentModel3.toString());
        assertEquals("1+", contentModel4.toString());
        assertEquals("2*", contentModel5.toString());
        assertEquals("4|3", contentModel6.toString());
        assertEquals("1+,2*", contentModel7.toString());
        assertEquals("(4|3)&(1+,2*)", contentModel8.toString());
        assertEquals("null", contentModel9.toString());
        assertEquals("1?", contentModel10.toString());
        assertEquals("1?,3*", contentModel11.toString());
        assertEquals("(1?,3*)|(1?,3*)", contentModel12.toString());
        assertEquals("(4|3),((1?,3*)|(1?,3*))", contentModel13.toString());
        assertEquals("2*&null", contentModel14.toString());
    }

    private void initContentModels() {
        contentModel1 = new ContentModel(elements[1]);
        contentModel2 = new ContentModel(elements[2]);
        contentModel3 = new ContentModel(elements[3]);
        contentModel4 = new ContentModel('+', contentModel1);
        contentModel5 = new ContentModel('*', contentModel2);
        contentModel6 = new ContentModel('|', elements[4], contentModel3);
        contentModel7 = new ContentModel(',', contentModel4, contentModel5);
        contentModel8 = new ContentModel('&', contentModel6, contentModel7);
        contentModel9 = new ContentModel();
        contentModel10 = new ContentModel('?', contentModel1);
        contentModel11 = new ContentModel(',',
                                          new ContentModel('?', contentModel1),
                                          new ContentModel('*', contentModel3));
        contentModel12 = new ContentModel('|', contentModel11, contentModel11);
        contentModel13 = new ContentModel(',', contentModel6, contentModel12);
        contentModel14 = new ContentModel('&', contentModel5, contentModel9);
    }

    public void testFirstObject() {
        if (!SwingTestCase.isHarmony()) {
            return;
        }
        initContentModels();
        Object rect = new Rectangle();
        assertTrue(contentModel1.first(elements[1]));
        assertTrue(contentModel1.first(null));
        assertFalse(contentModel1.first(rect));
        assertFalse(contentModel1.first(contentModel9));
        assertTrue(contentModel1.first(contentModel1));
        assertFalse(contentModel1.first(elements[2]));

        assertTrue(contentModel4.first(elements[1]));
        assertTrue(contentModel4.first(null));
        assertFalse(contentModel4.first(contentModel9));
        assertTrue(contentModel4.first(contentModel1));
        assertFalse(contentModel4.first(elements[2]));

        assertFalse(contentModel5.first(elements[1]));
        assertTrue(contentModel5.first(null));
        assertFalse(contentModel5.first(rect));
        assertFalse(contentModel5.first(contentModel9));
        assertFalse(contentModel5.first(contentModel1));
        assertTrue(contentModel5.first(contentModel2));
        assertTrue(contentModel5.first(elements[2]));

        assertFalse(contentModel6.first(elements[1]));
        assertTrue(contentModel6.first(null));
        assertFalse(contentModel6.first(rect));
        assertFalse(contentModel6.first(contentModel9));
        assertFalse(contentModel6.first(contentModel1));
        assertFalse(contentModel6.first(contentModel2));
        assertFalse(contentModel6.first(elements[2]));
        assertTrue(contentModel6.first(elements[4]));
        assertTrue(contentModel6.first(elements[3]));
        assertTrue(contentModel6.first(contentModel3));
        assertTrue(contentModel6.first(contentModel6));


        assertTrue(contentModel7.first(elements[1]));
        assertTrue(contentModel7.first(null));
        assertFalse(contentModel7.first(rect));
        assertFalse(contentModel7.first(contentModel5));
        assertTrue(contentModel7.first(contentModel1));
        assertFalse(contentModel7.first(contentModel2));
        assertFalse(contentModel7.first(elements[2]));
        assertFalse(contentModel7.first(elements[4]));
        assertFalse(contentModel7.first(elements[3]));
        assertFalse(contentModel7.first(elements[5]));
        assertFalse(contentModel7.first(contentModel3));

        assertTrue(contentModel8.first(elements[1]));
        assertTrue(contentModel8.first(null));
        assertFalse(contentModel8.first(rect));
        assertFalse(contentModel8.first(contentModel5));
        assertTrue(contentModel8.first(contentModel6));
        assertTrue(contentModel8.first(contentModel6));
        assertFalse(contentModel8.first(contentModel2));
        assertFalse(contentModel8.first(elements[2]));
        assertTrue(contentModel8.first(elements[4]));
        assertTrue(contentModel8.first(elements[3]));
        assertFalse(contentModel8.first(elements[5]));

        assertFalse(contentModel9.first(elements[1]));
        assertTrue(contentModel9.first(null));
        assertFalse(contentModel9.first(rect));
        assertFalse(contentModel9.first(contentModel5));
        assertFalse(contentModel9.first(contentModel6));

        assertTrue(contentModel10.first(elements[1]));
        assertTrue(contentModel10.first(null));
        assertFalse(contentModel10.first(rect));
        assertFalse(contentModel10.first(contentModel5));
        assertTrue(contentModel10.first(contentModel1));
        assertFalse(contentModel10.first(contentModel2));
        assertFalse(contentModel10.first(elements[2]));

        assertTrue(contentModel11.first(elements[1]));
        assertTrue(contentModel11.first(null));
        assertFalse(contentModel11.first(rect));
        assertFalse(contentModel11.first(contentModel5));
        assertTrue(contentModel11.first(contentModel1));
        assertFalse(contentModel11.first(contentModel2));
        assertTrue(contentModel11.first(contentModel3));
        assertFalse(contentModel11.first(elements[2]));
        assertFalse(contentModel11.first(elements[4]));
        assertTrue(contentModel11.first(elements[3]));
        assertFalse(contentModel11.first(elements[5]));
        assertFalse(contentModel11.first(contentModel7));

        assertTrue(contentModel12.first(elements[1]));
        assertTrue(contentModel12.first(null));
        assertFalse(contentModel12.first(rect));
        assertFalse(contentModel12.first(contentModel5));
        assertTrue(contentModel12.first(contentModel1));
        assertFalse(contentModel12.first(contentModel2));
        assertTrue(contentModel12.first(contentModel3));
        assertFalse(contentModel12.first(elements[2]));
        assertFalse(contentModel12.first(elements[4]));
        assertTrue(contentModel12.first(elements[3]));
        assertFalse(contentModel12.first(elements[5]));
        assertFalse(contentModel12.first(contentModel7));
        assertTrue(contentModel12.first(contentModel11));
        assertTrue(contentModel12.first(contentModel12));

        assertTrue(contentModel13.first(contentModel6));
        assertTrue(contentModel13.first(elements[4]));
        assertTrue(contentModel13.first(elements[3]));
        assertTrue(contentModel13.first(null));
        assertTrue(contentModel13.first(contentModel3));
        assertFalse(contentModel13.first(elements[1]));
        assertFalse(contentModel13.first(rect));
        assertFalse(contentModel13.first(contentModel5));
        assertFalse(contentModel13.first(contentModel1));
        assertFalse(contentModel13.first(elements[2]));
        assertFalse(contentModel13.first(elements[5]));
        assertFalse(contentModel13.first(contentModel7));
        assertFalse(contentModel13.first(contentModel11));
        assertFalse(contentModel13.first(contentModel12));

        assertTrue(contentModel14.first(contentModel5));
        assertTrue(contentModel14.first(null));
        assertTrue(contentModel14.first(contentModel9));
        assertTrue(contentModel14.first(elements[2]));
        assertFalse(contentModel14.first(elements[1]));
        assertFalse(contentModel14.first(rect));
        assertFalse(contentModel14.first(contentModel1));
        assertFalse(contentModel14.first(contentModel6));
        assertFalse(contentModel14.first(contentModel3));
        assertFalse(contentModel14.first(elements[4]));
        assertFalse(contentModel14.first(elements[3]));
        assertFalse(contentModel14.first(elements[5]));
        assertFalse(contentModel14.first(contentModel11));
    }

    private void checkElemVec(final Vector v) {
        assertEquals(v.size(), elemVec.size());
        for (int i = 0; i < v.size(); i++) {
            assertEquals(v.get(i), elemVec.get(i));
        }
    }

    public void testGetElements() {
        resetElemVec();
        Vector v = new Vector();
        initContentModels();

        contentModel1.getElements(elemVec);
        v.add(elements[1]);
        checkElemVec(v);

        contentModel2.getElements(elemVec);
        v.add(elements[2]);
        checkElemVec(v);

        contentModel3.getElements(elemVec);
        v.add(elements[3]);
        checkElemVec(v);

        contentModel4.getElements(elemVec);
        v.add(elements[1]);
        checkElemVec(v);

        contentModel5.getElements(elemVec);
        v.add(elements[2]);
        checkElemVec(v);

        if (!SwingTestCase.isHarmony()) {
            return;
        }

        contentModel6.getElements(elemVec);
        v.add(elements[4]);
        v.add(elements[3]);
        checkElemVec(v);

        contentModel7.getElements(elemVec);
        v.add(elements[1]);
        v.add(elements[2]);
        checkElemVec(v);

        contentModel8.getElements(elemVec);
        v.add(elements[4]);
        v.add(elements[3]);
        v.add(elements[1]);
        v.add(elements[2]);
        checkElemVec(v);

        contentModel9.getElements(elemVec);
        checkElemVec(v);
    }

    public void testEmpty() {
        //ClassCastException on RI
        if (!SwingTestCase.isHarmony()) {
            return;
        }
        initContentModels();
        assertFalse(contentModel1.empty());
        assertFalse(contentModel2.empty());
        assertFalse(contentModel3.empty());
        assertFalse(contentModel4.empty());
        assertTrue(contentModel5.empty());
        assertFalse(contentModel6.empty());
        assertFalse(contentModel7.empty());
        assertFalse(contentModel8.empty());
        assertTrue(contentModel9.empty());
        assertTrue(contentModel10.empty());
        assertTrue(contentModel11.empty());
        assertTrue(contentModel12.empty());
        assertFalse(contentModel13.empty());
        assertTrue(contentModel14.empty());
    }

    public void testSerialization() {
        contentModel1 = new ContentModel(elements[0]);
        contentModel = new ContentModel('|', elements[1], contentModel1);
        contentModel2 = (ContentModel)Utils.doSerialization(contentModel);
        assertEquals('|', contentModel2.type);
        assertTrue(contentModel2.content instanceof Element);
        assertEquals(elements[1].name, ((Element)contentModel2.content).name);
        contentModel3 = contentModel2.next;
        assertEquals(0, contentModel3.type);
        assertEquals("0", ((Element)contentModel3.content).name);
        assertNull(contentModel3.next);
    }
}
