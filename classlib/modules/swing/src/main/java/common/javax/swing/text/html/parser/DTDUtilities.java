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
package javax.swing.text.html.parser;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

class DTDUtilities implements DTDConstants {
    static final String SPLIT_PATTERN = "\\|";
    static final String REPLACE_PATTERN = " |\\(|\\)";
    static void putAttribute(final Hashtable atts,
                             final String name,
                             final String value) {
        if (name.indexOf("|") >= 0) {
           String[] names = replaceAndSplit(name);
           for (int i = 0; i < names.length; i++) {
               putAttribute(atts, names[i], value);
           }
        } else {
            atts.put(name, value);
        }
    };

    static String[] replaceAndSplit(final String s) {
        return s.replaceAll(REPLACE_PATTERN, "").split(SPLIT_PATTERN);
    }

    static void handleElement(final Object obj,
                              final String name,
                              final String descr,
                              final Hashtable attrTable) {
        //For testing
        if (obj instanceof Hashtable) {
            ((Hashtable)obj).put(name, descr);
            return;
        }
        DTD dtd = (DTD)obj;
        if (name.indexOf("|") >= 0) {
            String[] names = replaceAndSplit(name);
            for (int i = 0; i < names.length; i++) {
                handleElement(dtd, names[i], descr, attrTable);
            }
         } else {
             String[] splDescr = splitElementDescr(descr);
             boolean oStart = isOmitTag(splDescr[0], true);
             boolean oEnd = isOmitTag(splDescr[0], false);
             ContentModel contentModel = createContentModel(dtd, splDescr[1]);
             String[] exclusions = splitString(splDescr[2]);
             String[] inclusions = splitString(splDescr[3]);
             AttributeList atts =
                 createAttributeList(dtd, (String)attrTable.get(name));
             int type = getType(oEnd, splDescr[1]);
             dtd.defElement(name, type, oStart, oEnd, contentModel,
                            exclusions, inclusions, atts);
        }
    };

    static int getType(final boolean oEnd, final String descr) {
        if (descr.startsWith("EMPTY") && oEnd) {
            return EMPTY;
        } else if (descr.startsWith("CDATA")) {
            return CDATA;
        } else {
            return MODEL;
        }
    }

    static boolean isOmitTag(final String descr, final boolean isStart) {
        return descr.charAt(isStart ? 0 : 2) == 'O';
    }

    static void handleEntity(final DTD dtd, final String name,
                              final int value) {
        Entity entity = new Entity(name, (char)value);
        dtd.entityHash.put(name, entity);
        dtd.entityHash.put(new Integer(value), entity);
    }

    static AttributeList createAttributeList(final DTD dtd, final String spec) {
        String[] specs = spec.replaceAll("( )+", " ")
            .replaceAll("( )*\\|( )*", "|").split(" ");
        AttributeList result = null;
        String values = "";
        int modifier = 0;
        for (int i = specs.length - 3; i >= 0; i -= 3) {
            modifier = AttributeList.nameToModifier(specs[i + 2]);
            values = specs[i + 1];
            values = values.indexOf(")") >= 0
               ? values.replaceAll("\\(|\\)", "") : null;
            result = dtd.defAttributeList(specs[i + 0],
                                      AttributeList.name2type(specs[i + 1]),
                                      modifier,
                                      modifier == DEFAULT ? specs[i + 2] : null,
                                      values,
                                      result);

        }
        return result;
    }


    static String[] splitString(final String s) {
        return s == null ? null : s.split("\\|");
    }

    static boolean isBalanced(final String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == ')' && count < 0) {
                count++;
            } else if (ch == '(') {
                count--;
            }
        }
        return count == 0;
    }

    static ContentModel createContentModel(final DTD dtd, final String descr) {
        if (descr.startsWith("EMPTY") || descr.startsWith("CDATA")) {
            return null;
        }
        int length = descr.length();
        if (descr.matches("([a-zA-Z]|[0-9]|#)+")) {
            return new ContentModel((Element)dtd.elementHash.get(descr));
        } else if (descr.matches("\\((.)*\\)")
                   && isBalanced(descr.substring(1, length - 1))) {
            return createContentModel(dtd,
                                      descr.substring(1, length - 1));
        } else if (descr.matches("\\((.)+\\)[+*?]")
                   || descr.matches("([a-zA-Z]|[0-9]|#)+[+|*|?]")) {
            int index = length - 1;
            return
               new ContentModel(descr.charAt(index),
                                createContentModel(dtd,
                                                   descr.substring(0, index)));
        } else {
            int count = 0;
            char ch = 0;
            for (int i = descr.length() - 1; i >= 0; i--) {
                ch = descr.charAt(i);
                if ("|,&".indexOf(ch) >= 0 && count == 0) {
                    count = i;
                    break;
                } else if (ch == ')') {
                    count++;
                } else if (ch == '(') {
                    count--;
                }

            }
            return
               new ContentModel(ch,
                               createContentModel(dtd,
                                                  descr.substring(0, count)),
                               createContentModel(dtd,
                                                  descr.substring(count + 1,
                                                                  length)));
        }
    }

    static String[] splitElementDescr(final String descr) {
        String tags = descr.substring(0, 3);
        String allContent = descr.substring(4, descr.length());
        allContent = allContent.replaceAll("( )+", "");
        int incIndex = allContent.indexOf("+(");
        int excIndex = allContent.indexOf("-(");
        String content = "";
        String exc = null;
        String inc = null;

        if (incIndex >= 0 && excIndex >= 0) {
            if (incIndex > excIndex) {
                inc = allContent.substring(incIndex, excIndex);
                exc = allContent.substring(excIndex, allContent.length());
                content = allContent.substring(0, incIndex);
            } else {
                inc = allContent.substring(excIndex, incIndex);
                exc = allContent.substring(incIndex, allContent.length());
                content = allContent.substring(0, excIndex);
            }
        } else if (incIndex >= 0) {
            inc = allContent.substring(incIndex, allContent.length());
            content = allContent.substring(0, incIndex);
        } else if (excIndex >= 0) {
            exc = allContent.substring(excIndex, allContent.length());
            content = allContent.substring(0, excIndex);
        } else {
            content = allContent;
        }
        inc = inc == null ? null : inc.substring(2, inc.length() - 1);
        exc = exc == null ? null : exc.substring(2, exc.length() - 1);
        return new String[]{tags, content, exc, inc};
    }


    static void initDTD(final DTD dtd) {
        EntitiesHandler.initEntitiesCreation(dtd);
        ElementsHandler.initAttributes();
        putAllElementsIntoHash(dtd);
        ElementsHandler.initElementsCreation(dtd);
    }

    static void putAllElementsIntoHash(final DTD dtd) {
        Iterator iter = ElementsHandler.atts.keySet().iterator();
        while (iter.hasNext()) {
            String name = (String)iter.next();
            dtd.defElement(name, EMPTY, false, false, null, null, null, null);
        }
        dtd.defElement("#PCDATA", EMPTY, false, false, null, null, null, null);
    }
    
    public static void createBinaryDTD(final String fileName, DTD dtd) {
        try {
            FileOutputStream stream = new FileOutputStream(fileName);
            Asn1Dtd asn1 = new Asn1Dtd(dtd);
            byte[] enc = asn1.getEncoded();
            stream.write(enc);
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void createBinaryDTD(final String fileName) {
        DTD dtd = new DTD("tmp");
        initDTD(dtd);
        createBinaryDTD(fileName, dtd);
    }
}
