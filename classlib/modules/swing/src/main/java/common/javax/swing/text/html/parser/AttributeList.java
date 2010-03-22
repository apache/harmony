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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

public final class AttributeList implements DTDConstants, Serializable {
    public String name;

    public int type;

    public Vector<?> values;

    public int modifier;

    public String value;

    public AttributeList next;
    
    //TODO It need check type FIXED, IMPLIED, REQUIRED?
    public AttributeList(final String name,
                         final int type,
                         final int modifier,
                         final String value,
                         final Vector<?> values,
                         final AttributeList next) {
        this.name = name;
        this.type = type;
        this.modifier = modifier;
        this.value = value;
        this.values = values;
        this.next = next;
    }

    public static String type2name(final int type) {
        init();
        return type >= 0 && type < names.length ? names[type] : null;
    }

    public static int name2type(final String name) {
        init();
        for (int i = 0; i < names.length; i++) {
                if (name.equals(names[i])) {
                    return i;
                }
        }
        return 1;
    }

    static int nameToModifier(final String name) {
        if ("#IMPLIED".equals(name)) {
            return DTDConstants.IMPLIED;
        } else if ("#REQUIRED".equals(name)) {
            return DTDConstants.REQUIRED;
        } else if ("#FIXED".equals(name)) {
            return DTDConstants.FIXED;
        } else {
            return DTDConstants.DEFAULT;
        }
    }

    static String modifierToName(final int modifier) {
        if (modifier == DTDConstants.IMPLIED) {
            return "#IMPLIED";
        } else if (modifier == DTDConstants.REQUIRED) {
            return "#REQUIRED";
        } else if (modifier == DTDConstants.FIXED) {
            return "#FIXED";
        } else {
            return "#DEFAULT";
        }
    }

    public AttributeList(final String name) {
        this.name = name;
    }


    AttributeList() {
    }

    public String toString() {
        return name;
    }

    final String paramString() {
        //name type modifier,next.name next.type next.modifier
        String name = this.name;
        String type = values == null ? type2name(this.type) : valuesToString();
        String modifier = modifierToName(this.modifier);
        modifier = "#DEFAULT".equals(modifier) ? value : modifier;
        String result = name + " " + type + " " + modifier;
        return next == null ? result : result + "," + next.paramString();
    }

    final String valuesToString() {
        if (values != null) {
            String result = "";
            for (int i = 0; i < values.size(); i++) {
                result += "|" + values.get(i);
            }
            return result.substring(1, result.length());
        }
        return null;
    }

    public AttributeList getNext() {
        return next;
    }

    public String getValue() {
        return value;
    }

    public Enumeration<?> getValues() {
        // avoids a NullPointerException if values is null (same as RI)
        return values == null ? null : values.elements();
    }

    public int getModifier() {
        return modifier;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    private static String[] names;

    private static void init() {
        if (names == null) {
            names = new String[] {null,
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
                                  "NUTOKENS"
                                  };
        }
    }

    final boolean containsValue(final String value) {
        return values == null ? false : values.contains(value);
    }
}

