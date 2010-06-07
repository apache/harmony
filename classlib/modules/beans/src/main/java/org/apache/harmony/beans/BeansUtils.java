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

package org.apache.harmony.beans;

public class BeansUtils {

    public static final int getHashCode(Object obj) {
        return obj != null ? obj.hashCode() : 0;
    }

    public static final int getHashCode(boolean bool) {
        return bool ? 1 : 0;
    }

    public static String toASCIILowerCase(String string) {
        char[] charArray = string.toCharArray();
        StringBuilder sb = new StringBuilder(charArray.length);
        for (int index = 0; index < charArray.length; index++) {
            if ('A' <= charArray[index] && charArray[index] <= 'Z') {
                sb.append((char) (charArray[index] + ('a' - 'A')));
            } else {
                sb.append(charArray[index]);
            }
        }
        return sb.toString();
    }

    public static String toASCIIUpperCase(String string) {
        char[] charArray = string.toCharArray();
        StringBuilder sb = new StringBuilder(charArray.length);
        for (int index = 0; index < charArray.length; index++) {
            if ('a' <= charArray[index] && charArray[index] <= 'z') {
                sb.append((char) (charArray[index] - ('a' - 'A')));
            } else {
                sb.append(charArray[index]);
            }
        }
        return sb.toString();
    }

    public static boolean isPrimitiveWrapper(Class<?> wrapper, Class<?> base) {
        return (base == boolean.class) && (wrapper == Boolean.class)
                || (base == byte.class) && (wrapper == Byte.class)
                || (base == char.class) && (wrapper == Character.class)
                || (base == short.class) && (wrapper == Short.class)
                || (base == int.class) && (wrapper == Integer.class)
                || (base == long.class) && (wrapper == Long.class)
                || (base == float.class) && (wrapper == Float.class)
                || (base == double.class) && (wrapper == Double.class);
    }
}
