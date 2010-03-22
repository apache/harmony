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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

public class DTD implements DTDConstants {
    public static final int FILE_VERSION = 1;

    public String name;

    public Vector<Element> elements = new Vector<Element>();

    public Hashtable<String, Element> elementHash = new Hashtable<String, Element>();
    public Hashtable<Object, Entity> entityHash = new Hashtable<Object, Entity>();

    public final Element pcdata;
    public final Element html;
    public final Element meta;
    public final Element base;
    public final Element isindex;
    public final Element head;
    public final Element body;
    public final Element applet;
    public final Element param;
    public final Element p;
    public final Element title;

    private static final Hashtable dtdHash = new Hashtable();

    private static final String pattern = "(\\|)+";

    private boolean readDTD;
    
    /**
     * Created DTD will not be pushed to DTD hash.
     * @throws IllegalArgumentException if name equals to null
     */
    public static DTD getDTD(final String name) throws IOException {
        String key = name.toLowerCase();
        Object dtd = dtdHash.get(key);
        return dtd == null ? new DTD(name.toLowerCase()) : (DTD)dtd;
    }

    /**
     *
     * @throws IllegalArgumentException if dtd or name equal to null
     */
    public static void putDTDHash(final String name,
                                  final DTD dtd) {
        dtdHash.put(name.toLowerCase(), dtd);
    }

    protected DTD(final String name) {
        //TODO may be it need change the order
        this.name = name;

        pcdata = createDefaultElement(HTMLConstants.PCDATA_ELEMENT_NAME, 0);
        putElement(pcdata);

        html = createDefaultElement(HTMLConstants.HTML_ELEMENT_NAME, 1);
        putElement(html);

        meta = createDefaultElement(HTMLConstants.META_ELEMENT_NAME, 2);
        putElement(meta);

        base = createDefaultElement(HTMLConstants.BASE_ELEMENT_NAME, 3);
        putElement(base);

        isindex = createDefaultElement(HTMLConstants.ISINDEX_ELEMENT_NAME, 4);
        putElement(isindex);

        head = createDefaultElement(HTMLConstants.HEAD_ELEMENT_NAME, 5);
        putElement(head);

        body = createDefaultElement(HTMLConstants.BODY_ELEMENT_NAME, 6);
        putElement(body);

        applet = createDefaultElement(HTMLConstants.APPLET_ELEMENT_NAME, 7);
        putElement(applet);

        param = createDefaultElement(HTMLConstants.PARAM_ELEMENT_NAME, 8);
        putElement(param);

        p = createDefaultElement(HTMLConstants.P_ELEMENT_NAME, 9);
        putElement(p);

        title = createDefaultElement(HTMLConstants.TITLE_ELEMENT_NAME, 10);
        putElement(title);

        putElement(createDefaultElement(HTMLConstants.STYLE_ELEMENT_NAME, 11));
        putElement(createDefaultElement(HTMLConstants.LINK_ELEMENT_NAME, 12));
        putElement(createDefaultElement(HTMLConstants.SCRIPT_ELEMENT_NAME, 13));
        
        putElement(new Element(14, HTMLConstants.UNKNOWN_ELEMENT_NAME, false,
                               true, null, null, EMPTY, null, null, null));

        entityHash.put(HTMLConstants.SPACE_ENTITY_NAME,
                       createDefaultEntity(HTMLConstants.SPACE_ENTITY_NAME,
                                           " "));
        entityHash.put(HTMLConstants.RS_ENTITY_NAME,
                       createDefaultEntity(HTMLConstants.RS_ENTITY_NAME,
                                           "\n"));
        entityHash.put(HTMLConstants.RE_ENTITY_NAME,
                       createDefaultEntity(HTMLConstants.RE_ENTITY_NAME,
                                           "\r"));
    }

    private void putElement(final Element element) {
        elements.add(element);
        elementHash.put(element.getName(), element);
    }

    private Element createDefaultElement(final String name,
                                         final int index) {
        return new Element(index, name, false, false, null, null,
                           ANY, null, null, null);
    }

    private Entity createDefaultEntity(final String name,
                                       final String data) {
        return new Entity(name, 0, data, true, false);
    }

    public void read(final DataInputStream stream) throws IOException {
        // converts from DataInputStream into a byte array
        byte[] enc = new byte[1024];
        ByteArrayOutputStream bs = new ByteArrayOutputStream();

        int iRead = 0;
        while (iRead != -1) {
            iRead = stream.read(enc, 0, enc.length);
            if (iRead > 0) {
                bs.write(enc, 0, iRead);
            }
        }
        enc = bs.toByteArray();

        // decode the byte array
        Asn1Dtd asn1 = new Asn1Dtd(enc);
        
        // sets attributes
        asn1.getDTD(this);
        setReading(true);
    }

    public String toString() {
        return name;
    }

    protected ContentModel defContentModel(final int type,
                                           final Object content,
                                           final ContentModel next) {
        return new ContentModel(type, content, next);
    }

    /**
     *
     * @param values attributes split by '|'
     */
    protected AttributeList defAttributeList(final String name,
                                             final int type,
                                             final int modifier,
                                             final String value,
                                             final String values,
                                             final AttributeList next) {

        return new AttributeList(name, type, modifier, value,
                                 createAttributeNamesVector(values), next);
    }

    private Vector createAttributeNamesVector(final String values) {
        if (values == null) {
            return null;
        }
        Vector result = new Vector();
        String[] tokens = values.split(pattern);
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (!"".equals(token)) {
               result.add(token);
            }
        }
        return result;
    }

    /**
     * If element exists but doesn't correspond to this parameters, it will be
     * updated.
     */
    protected Element defElement(final String name,
                                 final int type,
                                 final boolean oStart,
                                 final boolean oEnd,
                                 final ContentModel content,
                                 final String[] exclusions,
                                 final String[] inclusions,
                                 final AttributeList atts) {
        return defineElement(name, type, oStart, oEnd, content,
                             createBitSetByStrings(exclusions),
                             createBitSetByStrings(inclusions),
                             atts);

    }

    private BitSet createBitSetByStrings(final String[] names) {
        if (names == null) {
            return null;
        }
        BitSet result = new BitSet();
        for (int i = 0; i < names.length; i++) {
            Element elem = (Element)elementHash.get(names[i]);
            if (elem == null) {
                elem = defineElement(names[i], DTDConstants.ANY, false, false, null, null, null, null);
            }
            if (!names[i].equals("")) {
                result.set(elem.getIndex());
            }
        }

        return result;
    }

    protected Entity defEntity(final String name,
                               final int type,
                               final String str) {
//        return defineEntity(name, type, str == null ? null : str.toCharArray());
        return defineEntity(name, type, str.toCharArray());
    }

    public Entity defEntity(final String name,
                            final int type,
                            final int ch) {
        return defineEntity(name, type, new char[] {(char)ch});
    }

    /**
     * Updated attributes of corresponding element, if this one exists.
     * Otherwise, new elements are created with these attributes and put to
     * elementsHash.
     */
    public void defineAttributes(final String name,
                                 final AttributeList atts) {
        Object obj = elementHash.get(name);
        Element elem;
        if (obj == null) {
            elem = createDefaultElement(name, elements.size());
            putElement(elem);
        } else {
            elem = (Element)obj;
        }
        elem.atts = atts;
    }

    /**
     * If element exists but doesn't correspond to this parameters, it will be
     * updated.
     */
    public Element defineElement(final String name,
                                 final int type,
                                 final boolean omitStart,
                                 final boolean omitEnd,
                                 final ContentModel contentModel,
                                 final BitSet exclusions,
                                 final BitSet inclusions,
                                 final AttributeList atts) {
        Object obj = elementHash.get(name);
        Element result;
        if (obj == null) {
            result = new Element(elements.size(), name, omitStart, omitEnd,
                                 exclusions, inclusions, type, contentModel,
                                 atts, null);
            putElement(result);
        } else {
            result = (Element)obj;
            result.updateElement(result.index, name, omitStart, omitEnd,
                                 exclusions, inclusions, type, contentModel,
                                 atts, null);
        }
        return result;
    }

    /**
     * If entity with this name exists, it will not be updated.
     */
    public Entity defineEntity(final String name,
                               final int type,
                               final char[] data) {
        Object obj = entityHash.get(name);
        Entity result;
        if (obj == null) {
            result = new Entity(name, type, data);
            entityHash.put(name, result);
            if (readDTD) {
                for (int i=0; i<data.length; i++) {
                    entityHash.put(Integer.valueOf(data[i]), result);
                }
            }
        } else {
            result = (Entity)obj;
        }
        return result;
    }

    /**
     *
     * @return if index < 0 or elements.size() <= index returns null.
     */
    public Element getElement(final int index) {
        // same as RI
        return (Element) elements.elementAt(index);
    }

    public Element getElement(final String name) {
        Object obj = elementHash.get(name);
        if (obj != null) {
            return (Element)obj;
        }
        Element element = createDefaultElement(name, elements.size());
        putElement(element);
        return element;
    }

    public Entity getEntity(final int index) {
        return entityHash.get(Integer.valueOf(index));
    }

    public Entity getEntity(final String name) {
        return (Entity)entityHash.get(name);
    }

    public String getName() {
        return name;
    }
    
    void setReading (boolean b) {
        readDTD = b;
    }
    
    boolean isRead () {
        return readDTD;
    }
}
