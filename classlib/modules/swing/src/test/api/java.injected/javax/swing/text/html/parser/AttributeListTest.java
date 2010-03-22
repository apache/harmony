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

import java.util.Vector;
import junit.framework.TestCase;

public class AttributeListTest extends TestCase {
       AttributeList attrList;
       String name = "first";
       int type = 26;
       int modifier = 16;
       String value = "value";
       Vector values = new Vector();
       AttributeList next = new AttributeList("second");



    protected void setUp() throws Exception {
        super.setUp();
        attrList = new AttributeList(name, type, modifier, value, values, next);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }


    public void testAttributeListConstr1() {
        Utils.checkAttributeList(attrList, modifier, type,
                                 name, next, values, value, true);
    }


    public void testAttributeListConstr2() {
        assertEquals(0, next.modifier);
        assertEquals("second", next.name);
        assertNull(next.next);
        assertEquals(0, next.type);
        assertNull(next.value);
        assertNull(next.values);

        AttributeList attributeList = new AttributeList(null);
        assertNull(attributeList.name);

        attributeList = new AttributeList("AAA");
        assertEquals("AAA", attributeList.name);
    }

    public void testGetNext() {
        assertEquals(next, attrList.getNext());
    }


    public void testGetValue() {
        assertEquals(value, attrList.getValue());
    }


    public void testGetValues() {
        assertEquals(values.elements().getClass(),
                     attrList.getValues().getClass());
    }


    public void testGetModifier() {
        assertEquals(modifier, attrList.getModifier());
    }


    public void testGetType() {
        assertEquals(type, attrList.getType());
    }


    public void testGetName() {
        assertEquals(name, attrList.getName());
    }

    String[] names = new String[] {null,
                                   "CDATA",
                                   "ENTITY",
                                   "ENTITIES",
                                   "ID",
                                   "IDREF",
                                   "IDREFS",
                                   "NAME",
                                   "NAMES",
                                   "NMTOKEN",
                                   "NMTOKENS",
                                   "NOTATION",
                                   "NUMBER",
                                   "NUMBERS",
                                   "NUTOKEN",
                                   "NUTOKENS"};

    public void testType2name() {
        for (int i = 0; i < 300; i++) {
            assertEquals(i < 16 ? names[i] : null, AttributeList.type2name(i));
        }
    }


    public void testName2type() {
        for (int i = 0; i < 300; i++) {
            if (i > 0 && i < 16) {
                assertEquals(1, AttributeList.name2type(names[i]
                                                              .toLowerCase()));
                assertEquals(i, AttributeList.name2type(names[i]));
            } else {
                try {
                    assertEquals(1, AttributeList.name2type(null));
                    assertFalse("NPE not thrown", true);
                } catch (NullPointerException e) {
                }
            }
        }
        assertEquals(1, AttributeList.name2type("test"));
    }

    public void testSerialization() {
        AttributeList attributeList = (AttributeList)Utils
            .doSerialization(attrList);
        Utils.checkAttributeList(attributeList,
                                 modifier, type, name,
                                 next, values, value, false);
    }
}
