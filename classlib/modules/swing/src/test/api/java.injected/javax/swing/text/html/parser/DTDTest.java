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

import java.io.IOException;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.SwingTestCase;
import junit.framework.TestCase;

/**
 * That's a test for default dtd, doesn't check dtd 4.01 and read method.
 */
public class DTDTest extends TestCase {
    static final String PCDATA = conv("#pcdata");
    static final String HTML = conv("html"); //1
    static final String META = conv("meta"); //2
    static final String BASE = conv("base"); //3
    static final String ISINDEX = conv("isindex"); //4
    static final String HEAD = conv("head"); //5
    static final String BODY = conv("body"); //6
    static final String APPLET = conv("applet"); //7
    static final String PARAM = conv("param"); //8
    static final String P = conv("p"); //9
    static final String TITLE = conv("title"); //10
    static final String STYLE = conv("style"); //11
    static final String LINK = conv("link"); //12
    static final String UNKNOWN = conv("unknown"); //13

    DTD dtd = new DTD("DTDTest1");
    Vector elementNames;

    public static final String SPACE_ENTITY_NAME = "#SPACE";
    public static final String RS_ENTITY_NAME = "#RS";
    public static final String RE_ENTITY_NAME = "#RE";
    private static final int DEFAULT_SIZE = 14;


    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetPutDTD() {
        try {
            DTD dtd1 = DTD.getDTD("DTDTest1");
            DTD dtd2 = DTD.getDTD("DTDTest1");

            int hashCode1 = dtd1.hashCode();
            int hashCode2 = dtd2.hashCode();
            assertEquals("DTDTest1".toLowerCase(), dtd1.getName());
            assertTrue(hashCode1 != hashCode2);
            assertNotSame(dtd, DTD.getDTD("DTDTest1".toLowerCase()));

            String name = "name";
            dtd1 = new DTD("abcd");
            DTD.putDTDHash(name, dtd1);

            assertEquals(dtd1, DTD.getDTD(name));
            assertNotSame(dtd1, DTD.getDTD("abcd"));
            assertEquals(dtd1, DTD.getDTD("Name"));
            assertEquals(dtd1, DTD.getDTD("name"));

            dtd2 = new DTD("abcdef");
            DTD.putDTDHash(name, dtd2);
            assertEquals(dtd2, DTD.getDTD(name));

            DTD.putDTDHash("name1", dtd2);
            assertEquals(dtd2, DTD.getDTD("name"));
            assertEquals(dtd2, DTD.getDTD("name1"));


            //If name contains upper characters that willn't be put to DTD.
            //That's isn't logically and not documented.
            if (SwingTestCase.isHarmony()) {
                name = "Name";
                dtd1 = new DTD("abcd");
                DTD.putDTDHash(name, dtd1);

                assertEquals(dtd1, DTD.getDTD(name));
                assertNotSame(dtd1, DTD.getDTD("abcd"));
                assertEquals(dtd1, DTD.getDTD("Name"));
                assertEquals(dtd1, DTD.getDTD("name"));
            }

            if (SwingTestCase.isHarmony()) {
                try {
                    dtd = DTD.getDTD(null);
                    assertFalse("IllegalArgumentException wasn't thrown", true);
                } catch (IllegalArgumentException e) {
                }
                try {
                    DTD.putDTDHash(null, dtd1);
                    assertFalse("IllegalArgumentException wasn't thrown", true);
                } catch (IllegalArgumentException e) {
                }
                try {
                    DTD.putDTDHash("nn", null);
                    assertFalse("IllegalArgumentException wasn't thrown", true);
                } catch (IllegalArgumentException e) {
                }
            }

          } catch (IOException e) {
              assertFalse("Unexpected IOException", true);
          }
    }

    public void testFILE_VERSION() {
        assertEquals(1, DTD.FILE_VERSION);
    }


    private void checkDefaultElements(final DTD dtd) {
        Vector elements = dtd.elements;
        assertEquals(DEFAULT_SIZE, dtd.elements.size());
        for (int i = 0; i < DEFAULT_SIZE - 1; i++) {
            Utils.checkDTDDefaultElement(((Element)elements.get(i)),
                                         ((String)elementNames.get(i)),
                                         i);
        }

        Utils.checkElement(((Element)elements.get(DEFAULT_SIZE - 1)),
                           null, null, null, null, null, DEFAULT_SIZE - 1,
                           UNKNOWN,
                           true, false, 17);

    }

    private void initDefaultElementsVector() {
        elementNames = new Vector();
        elementNames.add(PCDATA); //0
        elementNames.add(HTML); //1
        elementNames.add(META); //2
        elementNames.add(BASE); //3
        elementNames.add(ISINDEX); //4
        elementNames.add(HEAD); //5
        elementNames.add(BODY); //6
        elementNames.add(APPLET); //7
        elementNames.add(PARAM); //8
        elementNames.add(P); //9
        elementNames.add(TITLE); //10
        elementNames.add(STYLE); //11
        elementNames.add(LINK); //12
        elementNames.add(UNKNOWN); //13
    }

    private void checkDefaultElementHash(final DTD dtd) {
        Hashtable elementHash = dtd.elementHash;
        assertEquals(DEFAULT_SIZE, elementHash.size());

        for (int i = 0; i < DEFAULT_SIZE - 1; i++) {
            String name = (String)elementNames.get(i);
            Utils.checkDTDDefaultElement(((Element)elementHash.get(name)),
                                         name,
                                         i);
        }

        Utils.checkElement(((Element)elementHash.get(UNKNOWN)),
                           null, null, null, null, null, DEFAULT_SIZE - 1,
                           UNKNOWN,
                           true, false, 17);
    }

    private void checkDefaultEntityHash(final DTD dtd) {
        Hashtable entityHash = dtd.entityHash;
        assertEquals(3, entityHash.size());
        Set keys = entityHash.keySet();
        Iterator iter = keys.iterator();
        int count = 0;
        while (iter.hasNext()) {
            String name = (String)iter.next();
            Entity entity = (Entity)entityHash.get(name);
            if (SPACE_ENTITY_NAME.equals(name)) {
                count++;
                Utils.checkEntity(entity, name, 0, " ", true, false);
            } else if (RS_ENTITY_NAME.equals(name)) {
                count++;
                Utils.checkEntity(entity, name, 0, "\n", true, false);
            } else if (RE_ENTITY_NAME.equals(name)) {
                count++;
                Utils.checkEntity(entity, name, 0, "\r", true, false);
            }
        }
        assertEquals(3, count);
    }


    public void testDTD() {
        DTD dtd1 = new DTD(null);
        assertNull(dtd1.getName());

        assertEquals("DTDTest1", dtd.name);
        initDefaultElementsVector();
        checkDefaultElements(dtd);
        checkDefaultElementHash(dtd);
        checkDefaultEntityHash(dtd);

        Utils.checkDTDDefaultElement(dtd.pcdata, PCDATA, 0);
        Utils.checkDTDDefaultElement(dtd.html, HTML, 1);
        Utils.checkDTDDefaultElement(dtd.meta, META, 2);
        Utils.checkDTDDefaultElement(dtd.base, BASE, 3);
        Utils.checkDTDDefaultElement(dtd.isindex, ISINDEX, 4);
        Utils.checkDTDDefaultElement(dtd.head, HEAD, 5);
        Utils.checkDTDDefaultElement(dtd.body, BODY, 6);
        Utils.checkDTDDefaultElement(dtd.applet, APPLET, 7);
        Utils.checkDTDDefaultElement(dtd.param, PARAM, 8);
        Utils.checkDTDDefaultElement(dtd.p, P, 9);
        Utils.checkDTDDefaultElement(dtd.title, TITLE, 10);
    }

    public void testToString() {
        assertEquals(dtd.getName(), dtd.toString());
    }

    public void testDefContentModel() {
        Element e1 = new Element();
        e1.name = "e1";
        Element e2 = new Element();
        e2.name = "e2";
        ContentModel contentModel1 = new ContentModel(e1);
        ContentModel contentModel = dtd.defContentModel('|', e2, contentModel1);
        Utils.checkContentModel(contentModel, e2, '|', contentModel1);
    }

    public void testDefAttributeList() {
        String name = "name";
        int type = 22;
        int modifier = 23;
        String value = "value";
        String values = "value1|value2|value3|";
        AttributeList next = new AttributeList("next");
        AttributeList attl = dtd.defAttributeList(name, type, modifier, value,
                                             values, next);
        Vector v = new Vector();
        v.add("value1");
        v.add("value2");
        v.add("value3");
        Utils.checkAttributeList(attl, modifier, type, name, next, v,
                                 value, true);

        attl = dtd.defAttributeList(name, type, modifier, value, null, next);
        Utils.checkAttributeList(attl, modifier, type, name, next, null,
                                 value, true);
        v = new Vector();
        attl = dtd.defAttributeList(name, type, modifier, value, "", next);
        Utils.checkAttributeList(attl, modifier, type, name, next, v,
                                 value, true);
    }

    public void testDefElement() {
        String name = "newElement";
        int type = 234;
        boolean omitStart = true;
        boolean omitEnd = true;
        ContentModel contentModel = null;
        String[] exclusions = new String[] {HTML, BASE};
        String[] inclusions = new String[] {APPLET, BODY};
        AttributeList attl = new AttributeList("attributeList");
        Element elem = dtd.defElement(name, type, omitStart, omitEnd,
                                      contentModel, exclusions, inclusions,
                                      attl);
        BitSet excl = new BitSet();
        excl.set(1);
        excl.set(3);
        BitSet incl = new BitSet();
        incl.set(6);
        incl.set(7);
        Utils.checkElement(elem, attl, contentModel, null, incl, excl,
                           DEFAULT_SIZE,
                           name, omitEnd, omitStart, 234);
        assertEquals(DEFAULT_SIZE + 1, dtd.elements.size());
        assertEquals(name, ((Element)dtd.elements.get(DEFAULT_SIZE)).getName());
        dtd.elements.remove(DEFAULT_SIZE);
        dtd.elementHash.remove(name);

        elem = dtd.defElement(HTML, 123, false, false, null, null, null, null);
        assertEquals(14, dtd.elements.size());
        elem = (Element)dtd.elementHash.get(HTML);
        assertEquals(123, elem.getType());
        elem.type = 19;
    }

    static String conv(final String name) {
        return SwingTestCase.isHarmony() ? name.toUpperCase() : name;
    }

    public void testDefEntityStringintString() {
        String name = "newStringEntity";
        int type = 123;
        String data = "AbcD";
        Entity entity = dtd.defEntity(name, type, data);
        Utils.checkEntity(entity, name, type, data, false, false);
        assertEquals(entity, dtd.entityHash.get(name));
        dtd.entityHash.remove(name);

        name = "#SPACE";
        entity = dtd.defEntity(name, type, data);
        Utils.checkEntity(entity, name, 0, " ", true, false);
        assertEquals(3, dtd.entityHash.size());
    }

    public void testDefEntityStringintint() {
        String name = "newCharEntity";
        int type = 123;
        char data = 'J';
        Entity entity = dtd.defEntity(name, type, data);
        Utils.checkEntity(entity, name, type, Character.toString(data),
                          false, false);
        assertEquals(entity, dtd.entityHash.get(name));
        assertEquals(4, dtd.entityHash.size());
        dtd.entityHash.remove(name);


        name = "#SPACE";
        entity = dtd.defEntity(name, type, data);
        Utils.checkEntity(entity, name, 0, " ", true, false);
        assertEquals(3, dtd.entityHash.size());
    }

    public void testDefineAttributes() {
        String name = HTML;
        AttributeList attl = new AttributeList("new AttributeList");
        dtd.defineAttributes(HTML, attl);
        Element element = (Element)dtd.elementHash.get(name);
        assertEquals(attl, element.getAttributes());
        element.atts = null;

        name = "newElement";
        dtd.defineAttributes(name, attl);
        element = (Element)dtd.elementHash.get(name);
        assertEquals(DEFAULT_SIZE + 1, dtd.elements.size());
        assertEquals(attl, element.getAttributes());
        dtd.elements.remove(DEFAULT_SIZE);
        dtd.elementHash.remove(name);
    }

    public void testDefineElement() {
        Element elem = (Element)dtd.elementHash.get(HTML);
        String name = elem.getName();
        int type = elem.getType();
        boolean omitStart = elem.omitStart();
        boolean omitEnd = elem.omitEnd();
        ContentModel content = elem.getContent();
        BitSet exclusions = elem.exclusions;
        BitSet inclusions = elem.inclusions;
        AttributeList atts = elem.getAttributes();
        //check exactly the same
        Element elem1 = dtd.defineElement(name, type, omitStart, omitEnd,
                                          content, exclusions, inclusions,
                                          atts);
        assertEquals(elem, elem1);
        Utils.checkElement(elem1, atts, content, null, exclusions, inclusions,
                           1, name, omitEnd, omitStart, type);
        assertEquals(DEFAULT_SIZE, dtd.elements.size());

        // change fields
        type = 245;
        elem1 = dtd.defineElement(name, type, omitStart, omitEnd,
                                  content, exclusions, inclusions,
                                  atts);
        Utils.checkElement(elem1, atts, content, null, exclusions, inclusions,
                           1, name, omitEnd, omitStart, type);
        elem1 = (Element)dtd.elementHash.get(name); //TODO dtdHash isn't changed
        elem1 = (Element)dtd.elements.get(1);
        Utils.checkElement(elem1, atts, content, null, exclusions, inclusions,
                           1, name, omitEnd, omitStart, type);
        elem1.type = 19;
        assertEquals(DEFAULT_SIZE, dtd.elements.size());

        // other name
        name = "test";
        elem1 = dtd.defineElement(name, type, omitStart, omitEnd,
                                  content, exclusions, inclusions,
                                  atts);
        Utils.checkElement(elem1, atts, content, null, exclusions, inclusions,
                           DEFAULT_SIZE, name, omitEnd, omitStart, type);

        elem1 = (Element)dtd.elementHash.get(name);
        Utils.checkElement(elem1, atts, content, null, exclusions, inclusions,
                           DEFAULT_SIZE, name, omitEnd, omitStart, type);
        assertEquals(DEFAULT_SIZE + 1, dtd.elements.size());
        dtd.elementHash.remove(name);
        dtd.elements.remove(DEFAULT_SIZE);
    }

    public void testDefineEntity() {
        Entity entity = (Entity)dtd.entityHash.get("#SPACE");
        String name = entity.getName();
        int type = entity.getType();
        String data = entity.getString();
        boolean isGeneral = entity.isGeneral();
        boolean isParameter = entity.isParameter();

        Entity entity1 = dtd.defineEntity(name, type, data.toCharArray());
        Utils.checkEntity(entity1, name, type, data, isGeneral, isParameter);
        assertEquals(3, dtd.entityHash.size());

        entity1 = (Entity)dtd.entityHash.get(name);
        Utils.checkEntity(entity1, name, type, data, isGeneral, isParameter);

        data = "data";
        entity1 = dtd.defineEntity(name, type, data.toCharArray());
        //Attention: data wasn't updated
        Utils.checkEntity(entity1, name, type, " ", isGeneral, isParameter);
        assertEquals(3, dtd.entityHash.size());

        data = entity1.getString();
        type = 235;
        entity1 = dtd.defineEntity(name, type, data.toCharArray());
        //Attention: type wasn't updated
        Utils.checkEntity(entity1, name, 0, " ", isGeneral, isParameter);
        assertEquals(3, dtd.entityHash.size());

        name = "newEntity";
        entity1 = dtd.defineEntity(name, type, data.toCharArray());
        Utils.checkEntity(entity1, name, type, data, false, false);
        assertEquals(4, dtd.entityHash.size());
        entity1 = (Entity)dtd.entityHash.get(name);
        Utils.checkEntity(entity1, name, type, data, false, false);
        dtd.entityHash.remove(name);
    }

    public void testGetElementint() {
        initDefaultElementsVector();
        for (int i = 0; i < DEFAULT_SIZE; i++) {
            assertEquals(elementNames.get(i), dtd.getElement(i).getName());
        }
        //ArrayIndexOutOfBoundsException on RI
        if (SwingTestCase.isHarmony()) {
           assertNull(dtd.getElement(DEFAULT_SIZE));
        }
    }

    public void testGetElementString() {
        initDefaultElementsVector();
        for (int i = 0; i < DEFAULT_SIZE; i++) {
            String name = (String)elementNames.get(i);
            assertEquals(name, dtd.getElement(name).getName());
        }
        String name = "test1";
        Utils.checkDTDDefaultElement(dtd.getElement(name), name,
                                     DEFAULT_SIZE);
        assertEquals(DEFAULT_SIZE + 1, dtd.elementHash.size());
        assertEquals(DEFAULT_SIZE + 1, dtd.elements.size());

        dtd.elements.remove(DEFAULT_SIZE);
        dtd.elementHash.remove(name);
    }

    public void testGetEntityint() {
        //RI
        if (!SwingTestCase.isHarmony()) {
            assertNull(dtd.getEntity(0));
            assertNull(dtd.getEntity(1));
            assertNull(dtd.getEntity(2));
            assertNull(dtd.getEntity(' '));
            assertNull(dtd.getEntity('\n'));
            assertNull(dtd.getEntity('\r'));
        }
    }

    public void testGetEntityString() {
        assertEquals(dtd.entityHash.get(SPACE_ENTITY_NAME),
                     dtd.getEntity(SPACE_ENTITY_NAME));
        assertEquals(dtd.entityHash.get(RS_ENTITY_NAME),
                     dtd.getEntity(RS_ENTITY_NAME));
        assertEquals(dtd.entityHash.get(RE_ENTITY_NAME),
                     dtd.getEntity(RE_ENTITY_NAME));
        assertNull(dtd.getEntity("Test"));
    }

    public void testGetName() {
        assertEquals("DTDTest1", dtd.getName());
    }
}
