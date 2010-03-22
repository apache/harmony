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
* @author Alexander Y. Kleymenov
*/

package org.apache.harmony.misc;

/**
 * This class implements Base64 encoding/decoding functionality
 * as specified in RFC 2045 (http://www.ietf.org/rfc/rfc2045.txt).
 */
public class Base64 {
    
    public static byte[] decode(byte[] in) {
        return decode(in, in.length);
    }
    
    public static byte[] decode(byte[] in, int len) {
        int length = len / 4 * 3;
        byte[] out = new byte[length];
        int pad = 0, index = 0, j = 0, bits = 0;
        byte[] bytes = new byte[4];
        byte chr;
        for (int i=0; i<len; i++) {
            if (in[i] == '\n' || in[i] =='\r') {
                continue;
            }
            chr = in[i];
            // char ASCII value
            //  +    43    62
            //  /    47    63
            //  0    48    52
            //     .  .  .
            //  9    57    61 (ASCII + 4)
            //  =    61   pad
            //  A    65    0
            //     .  .  .
            //  Z    90    25 (ASCII - 65)
            //  a    97    26
            //     .  .  .
            //  z    122   51 (ASCII - 71)
            if (chr == '+') {
                bits = 62;
            } else if (chr == '/') {
                bits = 63;
            } else if ((chr >= '0') && (chr <= '9')) {
                bits = chr + 4;
            } else if (chr == '=') {
                bits = 0;
                pad ++;
            } else if ((chr >= 'A') && (chr <= 'Z')) {
                bits = chr - 65;
            } else if ((chr >= 'a') && (chr <= 'z')) {
                bits = chr - 71;
            } else {
                return null;
            }
            bytes[j%4] = (byte) bits;
            if (j%4 == 3) {
                out[index++] = (byte) (bytes[0] << 2 | bytes[1] >> 4);
                if (pad != 2) {
                    out[index++] = (byte) (bytes[1] << 4 | bytes[2] >> 2);
                    if (pad != 1) {
                        out[index++] = (byte) (bytes[2] << 6 | bytes[3]);
                    }
                }
            }
            j++;
        }
        byte[] result = new byte[index];
        System.arraycopy(out, 0, result, 0, index);
        return result;
    }

    private static final byte[] map = new byte[]
        {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 
         'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 
         'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 
         'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', 
         '4', '5', '6', '7', '8', '9', '+', '/'};

    public static String encode(byte[] in) {
        int length = in.length * 4 / 3;
        length += length / 76 + 3; // for crlr
        byte[] out = new byte[length];
        int index = 0, i, crlr = 0, end = in.length - in.length%3;
        for (i=0; i<end; i+=3) {
            out[index++] = map[(in[i] & 0xff) >> 2];
            out[index++] = map[((in[i] & 0x03) << 4) 
                                | ((in[i+1] & 0xff) >> 4)];
            out[index++] = map[((in[i+1] & 0x0f) << 2) 
                                | ((in[i+2] & 0xff) >> 6)];
            out[index++] = map[(in[i+2] & 0x3f)];
            if (((index - crlr)%76 == 0) && (index != 0)) {
                out[index++] = '\n';
                crlr++;
                //out[index++] = '\r';
                //crlr++;
            }
        }
        switch (in.length % 3) {
            case 1:
                out[index++] = map[(in[end] & 0xff) >> 2];
                out[index++] = map[(in[end] & 0x03) << 4];
                out[index++] = '=';
                out[index++] = '=';
                break;
            case 2:
                out[index++] = map[(in[end] & 0xff) >> 2];
                out[index++] = map[((in[end] & 0x03) << 4) 
                                    | ((in[end+1] & 0xff) >> 4)];
                out[index++] = map[((in[end+1] & 0x0f) << 2)];     
                out[index++] = '=';
                break;
        }
        return new String(out, 0, index);
    }
}

