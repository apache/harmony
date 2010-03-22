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
 * @author Igor A. Pyankov 
 */ 
package org.apache.harmony.x.print.ipp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/*
 * class IppAttribute stores IPP attribute (http://ietf.org/rfc/rfc2910.txt?number=2910)
 *  
 * <pre>
 *    The following table specifies the out-of-band values for the
 *    value-tag field.
 *   
 *    Tag Value (Hex)  Meaning
 *   
 *    0x10             unsupported
 *    0x11             reserved for 'default' for definition in a future
 *                     IETF standards track document
 *    0x12             unknown
 *    0x13             no-value
 *    0x14-0x1F        reserved for &quot;out-of-band&quot; values in future IETF
 *                     standards track documents.
 *   
 *    The following table specifies the integer values for the value-tag
 *    field:
 *   
 *    Tag Value (Hex)   Meaning
 *   
 *    0x20              reserved for definition in a future IETF
 *                      standards track document
 *    0x21              integer
 *    0x22              boolean
 *    0x23              enum
 *    0x24-0x2F         reserved for integer types for definition in
 *                      future IETF standards track documents
 *   
 *    NOTE: 0x20 is reserved for &quot;generic integer&quot; if it should ever be
 *    needed.
 *   
 *    The following table specifies the octetString values for the value-tag field:
 *   
 *    Tag Value (Hex)   Meaning
 *   
 *    0x30              octetString with an  unspecified format
 *    0x31              dateTime
 *    0x32              resolution
 *    0x33              rangeOfInteger
 *    0x34              reserved for definition in a future IETF  *  standards track document
 *    0x35              textWithLanguage
 *    0x36              nameWithLanguage
 *    0x37-0x3F         reserved for octetString type definitions in
 *                      future IETF standards track documents
 *   
 *    The following table specifies the character-string values for the
 *    value-tag field:
 *   
 *    Tag Value (Hex)   Meaning
 *   
 *    0x40              reserved for definition in a future IETF standards track document
 *    0x41              textWithoutLanguage
 *    0x42              nameWithoutLanguage
 *    0x43              reserved for definition in a future IETF standards track document
 *    0x44              keyword
 *    0x45              uri
 *    0x46              uriScheme
 *    0x47              charset
 *    0x48              naturalLanguage
 *    0x49              mimeMediaType
 *    0x4A-0x5F         reserved for character string type definitions
 *                      in future IETF standards track documents
 * </pre>
 */
public class IppAttribute {
    // "out-of-band" values for the "value-tag" field.
    public static final byte TAG_UNSUPPORTED = 0x10;
    public static final byte TAG_UNKNOWN = 0x12;
    public static final byte TAG_NO_VALUE = 0x13;

    // integer values for the "value-tag" field.
    public static final byte TAG_INTEGER = 0x21;
    public static final byte TAG_BOOLEAN = 0x22;
    public static final byte TAG_ENUM = 0x23;

    // octetString values for the "value-tag" field.
    public static final byte TAG_OCTETSTRINGUNSPECIFIEDFORMAT = 0x30;
    public static final byte TAG_DATETIME = 0x31;
    public static final byte TAG_RESOLUTION = 0x32;
    public static final byte TAG_RANGEOFINTEGER = 0x33;
    public static final byte TAG_TEXTWITHLANGUAGE = 0x35;
    public static final byte TAG_NAMEWITHLANGUAGE = 0x36;

    // character-string values for the "value-tag" field
    public static final byte TAG_TEXTWITHOUTLANGUAGE = 0x41;
    public static final byte TAG_NAMEWITHOUTLANGUAGE = 0x42;
    public static final byte TAG_KEYWORD = 0x44;
    public static final byte TAG_URI = 0x45;
    public static final byte TAG_URISCHEME = 0x46;
    public static final byte TAG_CHARSET = 0x47;
    public static final byte TAG_NATURAL_LANGUAGE = 0x48;
    public static final byte TAG_MIMEMEDIATYPE = 0x49;

    /*
     * The method just return names of attributes tags
     */
    public static String getTagName(byte atag) {
        String sz = "";
        switch (atag) {
        // integer values for the "value-tag" field.
        case TAG_BOOLEAN:
            sz = "BOOLEAN";
            break;
        case TAG_INTEGER:
            sz = "INTEGER";
            break;
        case TAG_ENUM:
            sz = "ENUM";
            break;
        // octetString values for the "value-tag" field.
        case TAG_OCTETSTRINGUNSPECIFIEDFORMAT:
        case TAG_DATETIME:
        case TAG_RESOLUTION:
        case TAG_RANGEOFINTEGER:
        case TAG_TEXTWITHLANGUAGE:
        case TAG_NAMEWITHLANGUAGE:
            sz = "OCTETSTRING";
            break;
        // character-string values for the "value-tag" field
        case TAG_TEXTWITHOUTLANGUAGE:
        case TAG_NAMEWITHOUTLANGUAGE:
        case TAG_KEYWORD:
        case TAG_URI:
        case TAG_URISCHEME:
        case TAG_CHARSET:
        case TAG_NATURAL_LANGUAGE:
        case TAG_MIMEMEDIATYPE:
            sz = "CHARACTERSTRING";
            break;
        default:
            sz = "UNKNOWN_ATTRIBUTE_TAG";
            break;
        }
        return sz;
    }

    protected byte atag;
    protected byte[] aname;
    protected Vector avalue;

    /*
     * Constructors
     */
    public IppAttribute(byte tag, String name, int value) {
        atag = tag;
        aname = name.getBytes();
        avalue = new Vector();
        avalue.add(new Integer(value));
    }

    public IppAttribute(byte tag, String name, String value) {
        atag = tag;
        aname = name.getBytes();
        avalue = new Vector();
        avalue.add(value.getBytes());
    }

    public IppAttribute(byte tag, String name, byte[] value) {
        atag = tag;
        aname = name.getBytes();
        avalue = new Vector();
        avalue.add(value);
    }

    public IppAttribute(byte tag, String name, Vector value) {
        atag = tag;
        aname = name.getBytes();
        avalue = value;
    }

    /*
     * Getters
     */
    public byte getTag() {
        return atag;
    }

    public byte[] getName() {
        return aname;
    }

    public Vector getValue() {
        return avalue;
    }

    /*
     * The method return byte array of attribute
     * Then these bytes can be wrote to http request
     */
    public byte[] getBytes() {
        ByteArrayOutputStream bbuf = new ByteArrayOutputStream();
        DataOutputStream dbuf = new DataOutputStream(bbuf);
        byte[] bv;

        try {
            for (int ii = avalue.size(), i = 0; i < ii; i++) {
                dbuf.writeByte(atag);
                if (i == 0) {
                    dbuf.writeShort(aname.length);
                    dbuf.write(aname);
                } else {
                    dbuf.writeShort(0);
                }
                switch (atag) {
                // integer values for the "value-tag" field.
                case TAG_BOOLEAN:
                    dbuf.writeShort(1);
                    dbuf.write(((Integer) avalue.get(i)).intValue());
                    break;
                case TAG_INTEGER:
                case TAG_ENUM:
                    dbuf.writeShort(4);
                    dbuf.writeInt(((Integer) avalue.get(i)).intValue());
                    break;
                // octetString values for the "value-tag" field.
                case TAG_OCTETSTRINGUNSPECIFIEDFORMAT:
                case TAG_DATETIME:
                case TAG_RESOLUTION:
                case TAG_RANGEOFINTEGER:
                case TAG_TEXTWITHLANGUAGE:
                case TAG_NAMEWITHLANGUAGE:
                    bv = (byte[]) avalue.get(i);
                    dbuf.writeShort(bv.length);
                    dbuf.write(bv);
                    break;
                // character-string values for the "value-tag" field
                case TAG_TEXTWITHOUTLANGUAGE:
                case TAG_NAMEWITHOUTLANGUAGE:
                case TAG_KEYWORD:
                case TAG_URI:
                case TAG_URISCHEME:
                case TAG_CHARSET:
                case TAG_NATURAL_LANGUAGE:
                case TAG_MIMEMEDIATYPE:
                    bv = (byte[]) avalue.get(i);
                    dbuf.writeShort(bv.length);
                    dbuf.write(bv);
                    break;
                default:
                    break;
                }
            }
            dbuf.flush();
            dbuf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bbuf.toByteArray();
    }

    /*
     * @see java.lang.Object#toString()
     * 
     * Returns readable form of attribute
     */
    public String toString() {
        ByteArrayOutputStream bbuf = new ByteArrayOutputStream();
        DataOutputStream dbuf = new DataOutputStream(bbuf);

        try {
            dbuf.writeBytes("attribute tag: 0x" + Integer.toHexString(atag)
                    + "(" + getTagName(atag) + ")" + "\n");
            dbuf.writeBytes("attribute name: " + new String(aname) + "\n");

            switch (atag) {
            // integer values for the "value-tag" field.
            case TAG_INTEGER:
            case TAG_BOOLEAN:
            case TAG_ENUM:
                for (int ii = avalue.size(), i = 0; i < ii; i++) {
                    Integer v = (Integer) avalue.get(i);
                    dbuf.writeBytes(v.toString() + "\n");
                }
                break;
            // octetString values for the "value-tag" field.
            case TAG_OCTETSTRINGUNSPECIFIEDFORMAT:
            case TAG_RESOLUTION:
            case TAG_TEXTWITHLANGUAGE:
            case TAG_NAMEWITHLANGUAGE:
                for (int ii = avalue.size(), i = 0; i < ii; i++) {
                    byte[] bv = (byte[]) avalue.get(i);
                    dbuf.writeBytes(new String(bv) + "\n");
                }
                break;
            case TAG_DATETIME:
                /*
                 * <pre> A date-time specification.
                 * 
                 * field octets contents range ----- ------ -------- ----- 1 1-2
                 * year 0..65536 2 3 month 1..12 3 4 day 1..31 4 5 hour 0..23 5
                 * 6 minutes 0..59 6 7 seconds 0..60 (use 60 for leap-second) 7
                 * 8 deci-seconds 0..9 8 9 direction from UTC '+' / '-' 9 10
                 * hours from UTC 0..11 10 11 minutes from UTC 0..59 For
                 * example, Tuesday May 26, 1992 at 1:30:15 PM EDT would be
                 * displayed as: 1992-5-26,13:30:15.0,-4:0 </pre>
                 */
                for (int ii = avalue.size(), i = 0; i < ii; i++) {
                    byte[] bv = (byte[]) avalue.get(i);
                    ByteArrayInputStream bi = new ByteArrayInputStream(bv);
                    DataInputStream di = new DataInputStream(bi);

                    dbuf.writeBytes(Integer.toString(di.readShort()) + "-"
                            + Integer.toString(di.readByte()) + "-"
                            + Integer.toString(di.readByte()) + ","
                            + Integer.toString(di.readByte()) + ":"
                            + Integer.toString(di.readByte()) + ":"
                            + Integer.toString(di.readByte()) + "."
                            + Integer.toString(di.readByte()) + ","
                            + (char) di.readByte()
                            + Integer.toString(di.readByte()) + ":"
                            + Integer.toString(di.readByte()) + "\n");
                }
                break;
            case TAG_RANGEOFINTEGER:
                for (int ii = avalue.size(), i = 0; i < ii; i++) {
                    byte[] bv = (byte[]) avalue.get(i);
                    ByteArrayInputStream bi = new ByteArrayInputStream(bv);
                    DataInputStream di = new DataInputStream(bi);
                    dbuf.writeBytes(Integer.toString(di.readInt()) + "..."
                            + Integer.toString(di.readInt()) + "\n");
                }
                break;
            // character-string values for the "value-tag" field
            case TAG_TEXTWITHOUTLANGUAGE:
            case TAG_NAMEWITHOUTLANGUAGE:
            case TAG_KEYWORD:
            case TAG_URI:
            case TAG_URISCHEME:
            case TAG_CHARSET:
            case TAG_NATURAL_LANGUAGE:
            case TAG_MIMEMEDIATYPE:
                for (int ii = avalue.size(), i = 0; i < ii; i++) {
                    byte[] bv = (byte[]) avalue.get(i);
                    dbuf.writeBytes(new String(bv) + "\n");
                }
                break;
            default:
                for (int ii = avalue.size(), i = 0; i < ii; i++) {
                    //byte[] bv = (byte[]) avalue.get(i);
                    //dbuf.writeBytes(new String(bv) + "\n");
                }
                break;
            }

            dbuf.flush();
            dbuf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bbuf.toString();
    }

}
