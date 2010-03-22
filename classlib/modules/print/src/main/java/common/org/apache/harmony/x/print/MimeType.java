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

/*
 * MimeType.java
 * 
 * represents an object that specifies the MIME type for DocFlavor object
 */

package org.apache.harmony.x.print;

import java.io.Serializable;
import java.util.Vector;

public class MimeType implements Serializable, Cloneable {
    
    private static final long serialVersionUID = -1062742668693502508L;

    private String aType = null; // Media type
    private String aSubtype = null; // Media subtype
    private String params[][]; // parameters names and values array

    public MimeType(String mimeType) {
        parseString(mimeType);
    }

    public String getType() {
        return aType;
    }

    public String getSubtype() {
        return aSubtype;
    }

    public String[][] getParams() {
        return params;
    }

    /*
     * gets parameter value for the given parameter bane,
     * returns null if such parameter is absent
     */
    public String getParameter(String paramName) {
        for (int i = 0; i < params.length; i++) {
            if ((params[i][0]).equals(paramName)) {
                return params[i][1];
            }
        }
        return null;
    }

    public boolean equals(Object obj) {
        return (obj != null)
                && (obj instanceof MimeType)
                && (getCanonicalForm().equals(((MimeType) obj)
                        .getCanonicalForm()));
    }

    public int hashCode() {
        return getCanonicalForm().hashCode();
    }

    public String toString() {
        return getCanonicalForm();
    }

    /*
     * returns canonical for MimeType object.
     */
    public String getCanonicalForm() {
        StringBuilder s = new StringBuilder();
        s.append(aType);
        s.append("/");
        s.append(aSubtype);
        for (int i = 0; i < params.length; i++) {
            s.append("; ");
            s.append(params[i][0]);
            s.append("=\"");
            s.append(params[i][1]);
            s.append("\"");
        }
        return s.toString();
    }

    //-------------------------------------------------------------------------------------

    /*
     * Parses MIME-type content header string as it is described in RFC 2045,
     * 822. Header may contain content type, subtype, parameters separated by
     * ";" and comments in brackets. Type and subtype are mandatory fields and
     * can not be omitted. All character should be US-ASCII. Type, subtype and
     * parameter names are case insencitive, so I always convert them to lower
     * case. Parameter names should not be duplicated. Parameter values can be
     * case sencitive, it depends on parameter. "Charset" parameter is always
     * case insencitive, so I always convert it to the lower case. I do not 
     * convert any other parameter values. Mime string tokens can not include 
     * some special ASCII characters - see isSpecialChar(char) function for 
     * more details. Parameter value can include special chars, however in this 
     * case it should be quotated. In canonical form all parameter values should 
     * be quotated and ordered by parameter names.
     *
     * RFC sources: http://www.rfc.net/rfc2045.html
     *              http://rfc.net/rfc822.html
     */
    private void parseString(String aString) {
        String errMsg = "Illegal mime-type string format!";

        int state = 1; // state for parser 
        int nextState = 0; // next state after the comments end

        // convert String to char array
        char s[] = new char[aString.length()];
        aString.getChars(0, aString.length(), s, 0);

        int len = s.length - 1;

        int cnt1 = 1; // nesting comments level

        // Position of type and subtype in the string:
        int startTypeIndex = 0;
        int startSubtypeIndex = 0;

        // Analized parameter name and value variables:
        int startParamNameIndex = 0;
        int startParamValueIndex = 0;
        String paramName = null;
        String paramValue = null;

        Vector nameVector = new Vector(); // parameter names vector
        Vector valueVector = new Vector(); // parameter values vector

        // string should not be empty, should have at least three characters
        if (len <= 1) {
            throw new IllegalArgumentException(errMsg);
        }

        for (int i = 0; i <= len; i++) {
            switch (state) {
            case 1:
                if (i == len) {
                    throw new IllegalArgumentException(errMsg);
                }
                if (isOKTokenChar(s[i])) {
                    startTypeIndex = i;
                    state = 2;
                    break;
                }
                if (isSpaceChar(s[i])) {
                    break;
                }
                if (s[i] == '(') {
                    nextState = 1;
                    state = 50;
                    break;
                }
                throw new IllegalArgumentException(errMsg);

            case 2:
                if (i == len) {
                    throw new IllegalArgumentException(errMsg);
                }
                if (isOKTokenChar(s[i])) {
                    break;
                }
                if (s[i] == '/') {
                    state = 3;
                    aType = newLowercaseString(s, startTypeIndex, i);
                    break;
                }
                if (s[i] == '(') {
                    nextState = 4;
                    state = 50;
                    aType = newLowercaseString(s, startTypeIndex, i);
                    break;
                }
                if (isSpaceChar(s[i])) {
                    state = 4;
                    aType = newLowercaseString(s, startTypeIndex, i);
                    break;
                }
                throw new IllegalArgumentException(errMsg);

            case 3:
                if (i == len) {
                    throw new IllegalArgumentException(errMsg);
                }
                if (isOKTokenChar(s[i])) {
                    startSubtypeIndex = i;
                    state = 5;
                    break;
                }
                if (s[i] == '(') {
                    nextState = 3;
                    state = 50;
                    break;
                }
                if (isSpaceChar(s[i])) {
                    break;
                }
                throw new IllegalArgumentException(errMsg);

            case 4:
                if (i == len) {
                    throw new IllegalArgumentException(errMsg);
                }
                if (s[i] == '(') {
                    nextState = 4;
                    state = 50;
                    break;
                }
                if (isSpaceChar(s[i])) {
                    break;
                }
                if (s[i] == '/') {
                    startSubtypeIndex = i + 1;
                    state = 3;
                    break;
                }
                throw new IllegalArgumentException(errMsg);

            case 5:
                if (isOKTokenChar(s[i])) {
                    if (i == len) {
                        aSubtype = newLowercaseString(s, startSubtypeIndex,
                                i + 1);
                    }
                    break;
                }
                if ((s[i] == ';') && (i != len)) {
                    aSubtype = newLowercaseString(s, startSubtypeIndex, i);
                    state = 7;
                    break;
                }
                if ((s[i] == '(') && (i != len)) {
                    aSubtype = newLowercaseString(s, startSubtypeIndex, i);
                    nextState = 6;
                    state = 50;
                    break;
                }
                if (isSpaceChar(s[i])) {
                    aSubtype = newLowercaseString(s, startSubtypeIndex, i);
                    state = 6;
                    break;
                }
                throw new IllegalArgumentException(errMsg);

            case 6:
                if (isSpaceChar(s[i])) {
                    break;
                }
                if ((s[i] == '(') && (i != len)) {
                    nextState = 6;
                    state = 50;
                    break;
                }
                if ((s[i] == ';') && (i != len)) {
                    state = 7;
                    break;
                }
                throw new IllegalArgumentException(errMsg);

            case 7:
                if (i == len) {
                    throw new IllegalArgumentException(errMsg);
                }
                if (isSpaceChar(s[i])) {
                    break;
                }
                if (s[i] == '(') {
                    state = 50;
                    nextState = 7;
                    break;
                }
                startParamNameIndex = i;
                if (isOKTokenChar(s[i])) {
                    state = 8;
                    break;
                }
                throw new IllegalArgumentException(errMsg);

            case 8:
                if (i == len) {
                    throw new IllegalArgumentException(errMsg);
                }
                if (isOKTokenChar(s[i])) {
                    break;
                }
                if (isSpaceChar(s[i])) {
                    paramName = newLowercaseString(s, startParamNameIndex, i);
                    state = 10;
                    break;
                }
                if (s[i] == '(') {
                    paramName = newLowercaseString(s, startParamNameIndex, i);
                    state = 50;
                    nextState = 10;
                    break;
                }
                if (s[i] == '=') {
                    paramName = newLowercaseString(s, startParamNameIndex, i);
                    state = 9;
                    break;
                }
                throw new IllegalArgumentException(errMsg);

            case 9:
                if (isOKTokenChar(s[i])) {
                    if (i == len) {
                        paramValue = new String(s, i, 1);
                        addParameter(paramName, paramValue, nameVector,
                                valueVector);
                    } else {
                        startParamValueIndex = i;
                        state = 11;
                    }
                    break;
                }
                if (i == len) {
                    throw new IllegalArgumentException(errMsg);
                }
                if (isSpaceChar(s[i])) {
                    break;
                }
                if (s[i] == '(') {
                    state = 50;
                    nextState = 9;
                    break;
                }
                if (s[i] == '"') {
                    startParamValueIndex = i + 1;
                    state = 13;
                    break;
                }
                throw new IllegalArgumentException(errMsg);

            case 10:
                if (i == len) {
                    throw new IllegalArgumentException(errMsg);
                }
                if (isSpaceChar(s[i])) {
                    break;
                }
                if (s[i] == '=') {
                    state = 9;
                    break;
                }
                throw new IllegalArgumentException(errMsg);

            case 11:
                if (isOKTokenChar(s[i])) {
                    if (i == len) {
                        paramValue = new String(s, startParamValueIndex, i + 1
                                - startParamValueIndex);
                        addParameter(paramName, paramValue, nameVector,
                                valueVector);
                    }
                    break;
                }
                if ((s[i] == ';') && (i != len)) {
                    paramValue = new String(s, startParamValueIndex, i
                            - startParamValueIndex);
                    addParameter(paramName, paramValue, nameVector, valueVector);
                    state = 7;
                    break;
                }
                if (isSpaceChar(s[i])) {
                    paramValue = new String(s, startParamValueIndex, i
                            - startParamValueIndex);
                    addParameter(paramName, paramValue, nameVector, valueVector);
                    state = 12;
                    break;
                }
                if ((s[i] == '(') && (i != len)) {
                    paramValue = new String(s, startParamValueIndex, i
                            - startParamValueIndex);
                    addParameter(paramName, paramValue, nameVector, valueVector);
                    state = 50;
                    nextState = 12;
                    break;
                }
                throw new IllegalArgumentException(errMsg);

            case 12:
                if (isSpaceChar(s[i])) {
                    break;
                }
                if ((s[i] == '(') && (i != len)) {
                    state = 50;
                    nextState = 12;
                    break;
                }
                if ((s[i] == ';') && (i != len)) {
                    state = 7;
                    break;
                }
                throw new IllegalArgumentException(errMsg);

            case 13:
                if ((s[i] == '"') && (s[i - 1] != '\\')) {
                    paramValue = new String(s, startParamValueIndex, i
                            - startParamValueIndex);
                    addParameter(paramName, paramValue, nameVector, valueVector);
                    state = 14;
                    break;
                }
                if (i == len) {
                    throw new IllegalArgumentException(errMsg);
                }
                if (isPrintableASCIIChar(s[i])) {
                    break;
                }
                throw new IllegalArgumentException(errMsg);

            case 14:
                if ((s[i] == ';') && (i != len)) {
                    state = 7;
                    break;
                }
                if (isSpaceChar(s[i])) {
                    break;
                }
                if ((s[i] == '(') && (i != len)) {
                    state = 50;
                    nextState = 14;
                    break;
                }
                throw new IllegalArgumentException(errMsg);

            case 50:
                if (s[i] == '(') {
                    cnt1++;
                }
                if (s[i] == ')') {
                    cnt1--;
                }
                if (cnt1 == 0) {
                    cnt1 = 1;
                    state = nextState;
                    break;
                }
                if (i == len) {
                    throw new IllegalArgumentException(errMsg);
                }
                if (isPrintableASCIIChar(s[i])) {
                    break;
                }
                throw new IllegalArgumentException(errMsg);
            }
        }
        makeParamsArray(nameVector, valueVector);
    }

    /*
     * make parameters string array from name and value vectors
     */
    private void makeParamsArray(Vector nameVector, Vector valueVector) {
        int[] arr = new int[nameVector.size()];
        int tmp;

        // Sort parameter names
        for (int i = 0; i < arr.length; i++) {
            arr[i] = i;
        }

        for (int i = 0; i < arr.length - 1; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                if (((String) nameVector.get(arr[j])).compareTo((String)nameVector
                        .get(arr[i])) <= 0) {
                    tmp = arr[i];
                    arr[i] = arr[j];
                    arr[j] = tmp;
                }
            }
        }

        // copy vector values to the string array
        params = new String[arr.length][2];
        for (int i = 0; i < arr.length; i++) {
            params[i][0] = (String) (nameVector.get(arr[i]));
            params[i][1] = (String) (valueVector.get(arr[i]));
        }
    }

    /*
     * add parameter name and value to the vectors.
     */
    private void addParameter(String name, String value, Vector nameVector,
            Vector valueVector) {
        // parameter names should not be duplicated
        if (nameVector.contains(name)) {
            throw new IllegalArgumentException(
                    "Duplicated parameters in mime type");
        }

        nameVector.add(name);

        // charset parameter should be converted to lower case
        if (name.equals(new String("charset"))) {
            valueVector.add(value.toLowerCase());
        } else {
            valueVector.add(value);
        }
    }

    /*
     * take part of the string and convert it to lower case (for types, subtypes
     * and parameter names)
     */
    private String newLowercaseString(char[] arr, int start, int end) {
        String s = new String(arr, start, end - start);
        return s.toLowerCase();
    }

    /*
     * returns true if c is a special character which can not be contained in
     * mime-type tokens. Such characters must be in quoted-string to use within
     * parameter value
     */
    private boolean isSpecialChar(char c) {
        return (c == '(') || (c == ')') || (c == '<') || (c == '>')
                || (c == '@') || (c == ',') || (c == ';') || (c == ':')
                || (c == '\\') || (c == '"') || (c == '/') || (c == '[')
                || (c == ']') || (c == '?') || (c == '=');
    }

    /*
     * returns true if c is a line space character - space or tab
     */
    private boolean isSpaceChar(char c) {
        return (c == ' ') || (c == 9);
    }

    /*
     * returns true if mime-type token can contain character c. Must be
     * printable ASCII character, not separator and not special character
     */
    private boolean isOKTokenChar(char c) {
        return (c < 127) && (c > 31) && (!isSpaceChar(c))
                && (!isSpecialChar(c));
    }

    /*
     * returns true if c is a printable ASCII character
     */
    private boolean isPrintableASCIIChar(char c) {
        //  return (c<127) && (c>31);
        return (c < 128);
    }

} /* End of MimeType class */
