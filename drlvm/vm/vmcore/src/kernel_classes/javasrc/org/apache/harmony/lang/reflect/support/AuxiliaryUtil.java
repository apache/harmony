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

package org.apache.harmony.lang.reflect.support;

/**
 * @author Serguei S. Zapreyev
 */
public final class AuxiliaryUtil {
        
    /**
     * For temporary using until I has problem with Character.codePointAt(int) using.
     */
/**/    private static int codePointAt(char[] a, int index) {
/**/        int ch1 = a[index]; // NullPointerException or IndexOutOfBoundsException may be arisen here
/**/        if (ch1 >= 0xD800 && ch1 <= 0xDBFF) {
/**/            if (index++ < a.length) {
/**/                int ch2 = a[index];
/**/                if (ch2 >= 0xDC00 && ch2 <= 0xDFFF) {
/**/                    return ((ch1 - 0xD800) << 10 | (ch2 - 0xDC00)) + 65536;
/**/                }
/**/            }
/**/        }
/**/        return ch1;
/**/    }
        
    /**
     * To transform to UTF8 representation.
     */
    public static String toUTF8(String ini) {
        if (ini == null) return ini;
        StringBuffer sb = new StringBuffer();
        int cp;
        int dgt;
/**/        char ca[] = ini.toCharArray();
        for (int i = 0; i < ini.length(); ) {
/**/            //if((cp = ini.codePointAt(i)) <= '\u007F') {
/**/            if((cp = codePointAt(ca, i)) <= '\u007F') {
                sb.append(Character.toString((char)cp));
                i++;
            } else if (cp <= '\u07FF') {
                sb.append("\\0");
                dgt = 0xC0 + ((cp & 0x7C0) >> 6);
                sb.append(Integer.toString(dgt, 16));
                sb.append("\\0");
                dgt = 0x80 + (cp & 0x3F);
                sb.append(Integer.toString(dgt, 16));
                i++;
            } else if (cp <= '\uFFFF') {
                sb.append("\\0");
                dgt = 0xE0 + ((cp & 0xF000) >> 12);
                sb.append(Integer.toString(dgt, 16));
                sb.append("\\0");
                dgt = 0x80 + ((cp & 0xFC0) >> 6);
                sb.append(Integer.toString(dgt, 16));
                sb.append("\\0");
                dgt = 0x80 + (cp & 0x3F);
                sb.append(Integer.toString(dgt, 16));
                i++;
            } else { // > '\uFFFF'
                sb.append("\\0ED");
                sb.append("\\0");
                dgt = 0xA0 + (((cp & 0xF0000) >> 16) - 1);
                sb.append(Integer.toString(dgt, 16));
                sb.append("\\0");
                dgt = 0x80 + ((cp & 0xFC00) >> 10);
                sb.append(Integer.toString(dgt, 16));
                sb.append("\\0ED");
                sb.append("\\0");
                dgt = 0xB0 + ((cp & 0x3C0) >> 6);
                sb.append(Integer.toString(dgt, 16));
                sb.append("\\0");
                dgt = 0x80 + (cp & 0x3F);
                sb.append(Integer.toString(dgt, 16));
                i += 2; 
            }
        }
        return sb.toString();
    }
}