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
import java.util.BitSet;
import java.util.Vector;

import junit.framework.TestCase;

public class ElementTest extends TestCase {
    Element elem = new Element();
    AttributeList atts1 = new AttributeList("atts1");
    ContentModel contentModel = new ContentModel(new Element());
    Object data = new Rectangle();
    BitSet exclusions = new BitSet();
    BitSet inclusions = new BitSet();
    int index = 4;
    String name = "testElement";
    boolean oEnd = false;
    boolean oStart = true;
    int type = 26;

    protected void setUp() throws Exception {
        super.setUp();
        elem = new Element();
    }

    public void testElement() {
        Utils.checkElement(elem, null, null, null, null, null, 0, null, false,
                           false, 19);
    }

    public void testGetters() {
        Utils.initElement(elem, atts1, contentModel, data, inclusions,
                          exclusions, index, name, oEnd, oStart, type);
        Utils.checkElement(elem, atts1, contentModel, data, inclusions,
                          exclusions, index, name, oEnd, oStart, type);
    }

    public void testAttributes() {
        Vector values1 = new Vector();
        values1.add("a");
        values1.add("b");
        atts1 = new AttributeList("atts1", 0, 0, "a", values1, null);

        elem.atts = atts1;
        assertEquals(atts1, elem.getAttribute("atts1"));
        assertNull(elem.getAttribute(null));
        assertEquals(atts1, elem.getAttributeByValue("a"));
        assertEquals(atts1, elem.getAttributeByValue("b"));
        assertNull(elem.getAttributeByValue("c"));
        assertNull(elem.getAttribute("atts2"));

        Vector values2 = new Vector();
        values2.add("swing");
        values2.add("awt");
        AttributeList atts2 = new AttributeList("atts2", 0, 0, "swing",
                                                values2, atts1);
        elem.atts = atts2;
        assertEquals(atts1, elem.getAttribute("atts1"));
        assertEquals(atts2, elem.getAttribute("atts2"));
        assertEquals(atts1, elem.getAttributeByValue("a"));
        assertEquals(atts1, elem.getAttributeByValue("b"));
        assertEquals(atts2, elem.getAttributeByValue("swing"));
        assertEquals(atts2, elem.getAttributeByValue("awt"));
        assertNull(elem.getAttributeByValue("c"));
        assertNull(elem.getAttributeByValue("2d"));
    }

    public void testIsEmpty() {
        assertFalse(elem.isEmpty());
        Utils.initElement(elem, atts1, contentModel, data, inclusions,
                          exclusions, index, name, oEnd, oStart, type);
        assertFalse(elem.isEmpty());
        Utils.initElement(elem, atts1, contentModel, data, inclusions,
                  exclusions, index, name, false, true, type);
        assertFalse(elem.isEmpty());
        Utils.initElement(elem, atts1, null, null, null,
                  null, index, name, oEnd, oStart, type);
        assertFalse(elem.isEmpty());
        Utils.initElement(elem, atts1, null, null, null,
                  null, index, name, oEnd, oStart, 17);
        assertTrue(elem.isEmpty());
    }

    public void testName2type() {
       String[] names = Utils.getDTDConstantsNames();
       for (int i = 0; i < names.length; i++) {
          String key = names[i];
          int type = Element.name2type(key);
          if (key.equals("ANY")) {
              assertEquals(DTDConstants.ANY, type);
          } else if (key.equals("CDATA")) {
              assertEquals(DTDConstants.CDATA, type);
          } else if (key.equals("EMPTY")) {
              assertEquals(DTDConstants.EMPTY, type);
          } else if (key.equals("RCDATA")) {
              assertEquals(DTDConstants.RCDATA, type);
          } else {
              assertEquals(0, type);
          }
       }
   }

   public void testSerialization() {
       Element intElement = (Element)contentModel.content;
       intElement.name = "A";
       Utils.initElement(elem, atts1, contentModel, data, inclusions,
                         exclusions, index, name, oEnd, oStart, type);
       Element element = (Element)Utils.doSerialization(elem);
       assertEquals(atts1.name, element.atts.getName());
       assertEquals(contentModel.type, element.getContent().type);
       assertEquals(intElement.getName(),
                    ((Element)element.getContent().content).getName());
       Utils.checkElement(element, atts1,
                          contentModel, data, inclusions,
                          exclusions, index, name, oEnd, oStart,
                          type, false);
   }
}
