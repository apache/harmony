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


import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.SwingTestCase;

import junit.framework.TestCase;

public class DTD401Test extends TestCase {
    DTD dtd;
    Vector elements;

    protected void setUp() throws Exception {
        dtd = Utils.getFilledDTD();
        super.setUp();
    }

    private void checkBitSet(final BitSet bs, final String descr) {
        boolean isEmptyBs = (bs == null || bs.cardinality() == 0);
        boolean isEmptyDescr = descr == null || descr == "";
        assertFalse(isEmptyBs ^ isEmptyDescr);
        if (!isEmptyBs) {
            String[] names = descr.split("\\|");
            for (int i = 0; i < names.length; i++) {
                int index = ((Element)dtd.elementHash.get(names[i])).getIndex();
                assertTrue(bs.get(index));
            }
            for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))  {
                String name = ((Element)dtd.elements.get(i)).getName();
                boolean result = false;
                for (int j = 0; j < names.length; j++) {
                    if (names[j].equals(name)) {
                        result = true;
                        break;
                    }
                }
                assertTrue(result);
            }
        }
    }

    private String tagsToString(final boolean oStart, final boolean oEnd) {
        return (oStart ? "O" : "-") + " " + (oEnd ? "O" : "-");
    }

    public void testEntities() {
        if (!SwingTestCase.isHarmony()) {
            Utils401.check32Entities(dtd.entityHash);
        } else {
            Utils401.check401Entities(dtd.entityHash);
        }
    }

    public void testElements() {
        if (!SwingTestCase.isHarmony()) {
            return;
        }
        ElementDescrHash hash = new ElementDescrHash();
        ElementsHandler.initAttributes();
        ElementsHandler.initElementsCreation(hash);
        assertEquals(hash.size() + 2, dtd.elementHash.size());
        assertEquals(hash.size() + 2, dtd.elements.size());
        Iterator iter = hash.keySet().iterator();
        for (int i = 0; i < dtd.elements.size(); i++) {
            String name = ((Element)dtd.elements.get(i)).getName();
            if (!hash.containsKey(name)) {
                assertTrue("UNKNOWN".equals(name) || "#PCDATA".equals(name));
            }
        }

        while (iter.hasNext()) {
            String name = (String)iter.next();
            String descr = (String)hash.get(name);
            String atts = (String)ElementsHandler.atts.get(name);
            checkElement(name, descr, atts);
        }
    }


    public void testFields() {
        if (!SwingTestCase.isHarmony()) {
            return;
        }
        ElementDescrHash hash = new ElementDescrHash();
        ElementsHandler.initAttributes();
        ElementsHandler.initElementsCreation(hash);

        Element[] elements = new Element[]
             {dtd.html,
             dtd.meta,
             dtd.base,
             dtd.isindex,
             dtd.head,
             dtd.body,
             dtd.applet,
             dtd.param,
             dtd.p,
             dtd.title};
         for (int i = 0; i < elements.length; i++) {
             Element element = elements[i];
             String name = element.getName();
             String descr = (String)hash.get(name);
             String atts = (String)ElementsHandler.atts.get(name);
             checkElement(element, descr, atts);
       }

    }

    private void checkElement(final Element elem, final String descr,
                              final String atts) {
        assertEquals(dtd.elements.get(elem.getIndex()), elem);
        assertEquals(dtd.elementHash.get(elem.name), elem);
        String[] splDescr = DTDUtilities.splitElementDescr(descr);
        checkTags(elem.oStart, elem.oEnd, splDescr[0]);
        checkBitSet(elem.exclusions, splDescr[2]);
        checkBitSet(elem.inclusions, splDescr[3]);
        checkContentModel(elem.name, elem.content, splDescr[1]);
        checkType(elem, splDescr[1]);
        checkAttributes(elem, atts);
    }

    private void checkElement(final String name, final String descr,
                              final String atts) {
        Element elem = (Element)dtd.elementHash.get(name);
        checkElement(elem, descr, atts);
    }

    private void checkType(final Element elem, final String descr) {
        int type = elem.type;
        if (descr.equals("EMPTY")) {
            assertEquals(DTDConstants.EMPTY, type);
        } else if (descr.equals("CDATA")) {
            assertEquals(DTDConstants.CDATA, type);
        } else {
            assertEquals(DTDConstants.MODEL, type);
        }
    }

    private void checkContentModel(final String name,
                                   final ContentModel contentModel,
                                   final String descr) {
        if (contentModel == null) {
            assertTrue(descr.equals("EMPTY") || descr.equals("CDATA"));
        } else  if (descr.equals("EMPTY")) {
            assertNull(contentModel);
        } else  if (!descr.equals("CDATA")) {
            String content = contentModel.toString();
            if (!checExtraordinaryCase(name, content, descr)) {
               assertEquals(descr, content);
            }
        }
    }

    private boolean checExtraordinaryCase(final String name,
                                          final String content,
                                          final String descr) {
        if (name.equals("COLGROUP")
            || name.equals("TFOOT")
            || name.equals("DIR")
            || name.equals("MENU")
            || name.equals("OPTGROUP")
            || name.equals("OL")
            || name.equals("UL")
            || name.equals("TBODY")
            || name.equals("THEAD")) {
            String s1 = "(" + content.substring(0, content.length() - 1)
                    + ")" + content.charAt(content.length() - 1);
            assertEquals(descr, s1);
            return true;
        } else if (name.equals("TABLE")
                || name.equals("HTML")
                || name.equals("#PCDATA")
                || name.equals("TEXTAREA")
                || name.equals("FIELDSET")
                || name.equals("TITLE")
                || name.equals("HEAD")
                || name.equals("OPTION")
                || name.equals("FRAMESET")) {
            String s1 = "(" + content + ")";
            assertEquals(descr, s1);
            return true;
        } else if (name.equals("ADDRESS")) {
            String s1 = "(" + content.substring(0, content.length() - 4)
              + ")" + content.substring(content.length() - 4, content.length());
             assertEquals(descr, s1);
            return true;
        } else if (name.equals("MAP")) {
            String s1 = "(" + content.substring(0, content.length() - 7)
            + ")" + content.substring(content.length() - 7, content.length());
            assertEquals(descr, s1);
            return true;
        }
        return false;
    }


    private void checkTags(final boolean oStart,
                           final boolean oEnd,
                           final String desc) {
        assertEquals(desc, tagsToString(oStart, oEnd));
    }

    private void checkAttributes(final Element elem,
                                 final String atts) {
        String name = elem.getName();
        if (name.equals("INPUT") || name.equals("FORM")
                || name.equals("FRAMESET")) {
            assertEquals(atts.replaceAll("\\(|\\)", "")
                         .replaceAll("( )+", "").replaceAll("CDATAs;", "CDATA"),
                         elem.atts.paramString()
                         .replaceAll(",", " ").replaceAll("( )+", ""));

        } else {
            assertEquals(atts.replaceAll("\\(|\\)", "").replaceAll("( )+", ""),
                         elem.atts.paramString().replaceAll(",", " ")
                         .replaceAll("( )+", ""));
        }
    }

    static class ElementDescrHash extends Hashtable {

        public synchronized Object put(final Object key, final Object value) {
            String name = (String) key;
            if (name.indexOf("|") >= 0) {
                String[] names = DTDUtilities.replaceAndSplit(name);
                for (int i = 0; i < names.length; i++) {
                    put(names[i], value);
                }
                return null;
            } else {
                return super.put(key, value);
            }
        }
    }

//    public void testSer() {
//        DTDUtilities.createBinaryDTD("C:/jenja.bd");
//    }
}
