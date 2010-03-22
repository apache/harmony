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


public final class Entity implements DTDConstants {
    public String name;

    public int type;

    public char[] data;

    private final static int GENERAL_MASK = DTDConstants.GENERAL;
    
    private final static int PARAMETER_MASK = DTDConstants.PARAMETER;
    
    public Entity(final String name,
                  final int type,
                  final char[] data) {
        this.name = name;
        this.type = type;
        this.data = data;
    }

    Entity(final String name,
           final int type,
           final String data,
           final boolean isGeneral,
           final boolean isParameter) {
        this (name, 
                (type | 
                        (isGeneral ? GENERAL_MASK : 0) | 
                        (isParameter ? PARAMETER_MASK : 0)), 
                data.toCharArray());
    }

    Entity(final String name,
           final char ch) {
        this(name, DTDConstants.CDATA | GENERAL_MASK, new char[] {ch});
    }

    public String getString() {
        return String.valueOf(data);
    }

    public char[] getData() {
        return data;
    }

    public boolean isGeneral() {
        return (type & GENERAL_MASK) != 0;
    }

    public boolean isParameter() {
        return (type & PARAMETER_MASK) != 0;
    }

    public int getType() {
        return type & 0xFFFF;
    }

    public String getName() {
        return name;
    }

    public static int name2type(final String name) {
        if (name.equals("PUBLIC")) {
            return DTDConstants.PUBLIC;
        } else if (name.equals("SDATA")) {
            return DTDConstants.SDATA;
        } else if (name.equals("PI")) {
            return DTDConstants.PI;
        } else if (name.equals("STARTTAG")) {
            return DTDConstants.STARTTAG;
        } else if (name.equals("ENDTAG")) {
            return DTDConstants.ENDTAG;
        } else if (name.equals("MS")) {
            return DTDConstants.MS;
        } else if (name.equals("MD")) {
            return DTDConstants.MD;
        } else if (name.equals("SYSTEM")) {
            return DTDConstants.SYSTEM;
        } else {
            return DTDConstants.CDATA;
        }
    }
}

