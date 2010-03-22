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

package javax.sql.rowset.serial;

import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.HashMap;

class DefaultUDTMap<T> {

    public static HashMap<String, Class<?>> DEFAULTMAP = new HashMap<String, Class<?>>();

    public static boolean isDefault(int type) {
        return (type == Types.ARRAY || type == Types.BLOB || type == Types.CLOB
                || type == Types.DATALINK || type == Types.STRUCT || type == Types.JAVA_OBJECT);
    }

    public static SerialDatalink[] processDatalink(Object[] elements)
            throws SerialException {
        SerialDatalink[] ret = new SerialDatalink[elements.length];
        for (int i = 0; i < elements.length; i++) {
            ret[i] = new SerialDatalink((URL) elements[i]);
        }
        return ret;
    }

    public static Struct[] processStruct(Object[] elements)
            throws SerialException {
        Struct[] ret = new Struct[elements.length];
        for (int i = 0; i < elements.length; i++) {
            ret[i] = (Struct) elements[i];
        }
        return ret;
    }

    public static Array[] processArray(Object[] elements)
            throws SerialException {
        Array[] ret = new Array[elements.length];
        for (int i = 0; i < elements.length; i++) {
            ret[i] = (Array) elements[i];
        }
        return ret;
    }

    public static Clob[] processClob(Object[] elements) throws SQLException {
        Clob[] ret = new Clob[elements.length];
        for (int i = 0; i < elements.length; i++) {
            ret[i] = new SerialClob((Clob) elements[i]);
        }
        return ret;
    }

    public static Blob[] processBlob(Object[] elements) throws SQLException {
        Blob[] ret = new Blob[elements.length];
        for (int i = 0; i < elements.length; i++) {
            ret[i] = new SerialBlob((Blob) elements[i]);
        }
        return ret;
    }

    public static Object[] processObject(Object[] elements)
            throws SerialException {
        Object[] ret = new Object[elements.length];
        for (int i = 0; i < elements.length; i++) {
            ret[i] = new SerialJavaObject(elements[i]);
            // TODO according to RI, should do like this, but does it make
            // sense?
            elements[i] = ret[i];
        }
        return ret;
    }
}
