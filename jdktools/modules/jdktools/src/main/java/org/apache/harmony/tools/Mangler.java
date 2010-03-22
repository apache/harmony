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

package org.apache.harmony.tools;

/**
 * A helper class to mangle a string according to the Java Native Interface
 * Specification.
 */
public class Mangler {

    /**
     * An array of the <code>String</code> pairs. This array 
     * contains the common substitute string pairs.
     */
    private static final String COMMON_TABLE[] = {
        "_", "_1",
        ";", "_2",
        "[", "_3",
        "./", "_"
    };

    /**
     * A field names substitution table.
     */
    private static final String FIELD_TABLE[][] = {
        { ".$", "_" }
    };
    
    /**
     * A method names substitution table.
     */
    private static final String METHOD_TABLE[][] = {
        COMMON_TABLE,
        { "$", "_" }
    };

    /**
     * A class names substitution table.
     */
    private static final String CLASS_TABLE[][] = {
        COMMON_TABLE,
        { "$", "_00024" }
    };
    
    /**
     * A macros substitution table.
     */
    private static final String MACRO_TABLE[][] = {
        { ".$", "_" }
    };
    
    /**
     * A file names substitution table.
     */
    private static final String FILE_TABLE[][] = {
        { ".$", "_" }
    };
    
    /**
     * Returns a mangled string. The given string will be translated according
     * the given substitution table. This table consists of one or more
     * <code>String</code> arrays that contain the substitute string pair.
     * The first element of each <code>String</code> pair in the array
     * contains a set of chars represented as a string. Any of the given 
     * chars will be replaced with the second string of the pair
     * in the result string. If table is null all the unicode
     * chars will be replaced with an appropriate substitute string only.
     * 
     * @param name - a string to be mangled.
     * @param table - a substitution table. <code>null</code> is possible.
     * @return a mangled string.
     */
    private static String mangle(String name, String table[][]) {
        StringBuffer result = new StringBuffer();

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            // Check the given table, if it is not null.
            if (table != null) {
                boolean found = false;
                for (int k = 0; !found && k < table.length; k++) {
                    int l = 0;
                    while (l < table[k].length) {
                        if (table[k][l].indexOf(c) != -1) {
                            result.append(table[k][l + 1]);
                            found = true;
                            break;
                        }
                        l += 2;
                    }
                }
                // If c is found in the given substitution table we continue.
                if (found) {
                    continue;
                }
            }

            if (Character.UnicodeBlock.of(c) != Character.UnicodeBlock.BASIC_LATIN) {
                // We have to prepare a string that looks like "_0XXXX",
                // where "XXXX" is a string representation of a Unicode
                // character. If the given string length is less than 4
                // we have to prepend a missing number of '0' to the result.
                String s = Integer.toHexString((int) c);
                char code[] = new char[] {'0','0','0','0'};
                int len = s.length();
                int align = code.length - len;
                int begin = len - code.length;
                s.getChars(begin < 0 ? 0 : begin, len, code,
                        align < 0 ? 0 : align);
                result.append("_0").append(code);
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Returns a mangled string. All the unicode chars of the given string
     * will be replaced with the <code>_0XXXX</code> char sequence,
     * where XXXX is a numeric representation of a Unicode character.
     * 
     * @param name - a string to be mangled.
     * @return a mangled string.
     */
    public static String mangleUnicode(String name) {
        return Mangler.mangle(name, null);
    }

    /**
     * Returns a mangled string that represents the given field name.
     * 
     * @param name - a string to be mangled.
     * @return a mangled string.
     */
    public static String mangleFieldName(String name) {
        return Mangler.mangle(name, FIELD_TABLE);
    }

    /**
     * Returns a mangled string that represents the given method name.
     * 
     * @param name - a string to be mangled.
     * @return a mangled string.
     */
    public static String mangleMethodName(String name) {
        return Mangler.mangle(name, METHOD_TABLE);
    }

    /**
     * Returns a mangled string that represents the given class name.
     * 
     * @param name - a string to be mangled.
     * @return a mangled string.
     */
    public static String mangleClassName(String name) {
        return Mangler.mangle(name, CLASS_TABLE);
    }

    /**
     * Returns a mangled string that represents the given macro.
     * 
     * @param name - a string to be mangled.
     * @return a mangled string.
     */
    public static String mangleMacro(String name) {
        return Mangler.mangle(name, MACRO_TABLE);
    }

    /**
     * Returns a mangled string that represents the given file name.
     * 
     * @param name - a string to be mangled.
     * @return a mangled string.
     */
    public static String mangleFileName(String name) {
        return Mangler.mangle(name, FILE_TABLE);
    }

}
