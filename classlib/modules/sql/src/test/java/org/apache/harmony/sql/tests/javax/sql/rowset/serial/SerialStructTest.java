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

package org.apache.harmony.sql.tests.javax.sql.rowset.serial;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.Map;

import javax.sql.rowset.serial.SerialException;
import javax.sql.rowset.serial.SerialStruct;

import junit.framework.TestCase;

public class SerialStructTest extends TestCase {

    Map<String, Class<?>> map = new HashMap<String, Class<?>>();

    Map<String, Class<?>> emptyMap = new HashMap<String, Class<?>>();

    public SerialStructTest() throws ClassNotFoundException {
        map
                .put(
                        "name_t",
                        Class
                                .forName("org.apache.harmony.sql.tests.javax.sql.rowset.serial.Name"));
        map
                .put(
                        "man_t",
                        Class
                                .forName("org.apache.harmony.sql.tests.javax.sql.rowset.serial.Man"));

    }

    public void testSerialStructSQLDataMapOfStringClassOfQ()
            throws SQLException, ClassNotFoundException {
        Man sdata = new Man(1, new Name("Tony", "Wu"));
        try {
            new SerialStruct((SerialStruct) null, emptyMap);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected.
        }
        try {
            new SerialStruct(sdata, null);
            fail("should throw SerialException.");
        } catch (SerialException e) {
            // expected.
        }
        try {
            new SerialStruct(sdata, null);
            fail("should throw SerialException.");
        } catch (SerialException e) {
            // expected.
        }
        SerialStruct ss = new SerialStruct(sdata, emptyMap);
        assertNotNull(ss);
    }

    public void testSerialStructStructMapOfStringClassOfQ()
            throws SerialException {
        Man sdata = new Man(1, new Name("Tony", "Wu"));
        SerialStruct ss = new SerialStruct(sdata, emptyMap);
        try {
            new SerialStruct((SerialStruct) null, emptyMap);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected.
        }
        new SerialStruct(ss, null);
        SerialStruct ss2 = new SerialStruct(ss, emptyMap);
        assertNotNull(ss2);
    }

    public void testGetAttributes() throws SerialException {
        Man sdata = new Man(1, new Name("Tony", "Wu"));
        SerialStruct ss = new SerialStruct(sdata, emptyMap);
        Object[] o = ss.getAttributes();
        assertSame(o, ss.getAttributes());
    }

    public void testGetAttributesMapOfStringClassOfQ() throws SerialException {
        Man sdata = new Man(1, new Name("Tony", "Wu"));
        SerialStruct ss = new SerialStruct(sdata, emptyMap);
        Object[] o = ss.getAttributes(map);
        assertSame(o, ss.getAttributes(map));
    }

    public void testGetSQLTypeName() throws SQLException {
        Man sdata = new Man(1, new Name("Tony", "Wu"));
        SerialStruct ss = new SerialStruct(sdata, emptyMap);
        assertEquals(sdata.getSQLTypeName(), ss.getSQLTypeName());

    }

}

class Name implements SQLData {
    public String first;

    public String last;

    private String sql_type = "name_t";

    public Name(String first, String last) {
        this.first = first;
        this.last = last;
    }

    public String getSQLTypeName() {
        return sql_type;
    }

    public void readSQL(SQLInput stream, String type) throws SQLException {
        sql_type = type;
        first = stream.readString();
        last = stream.readString();
    }

    public void writeSQL(SQLOutput stream) throws SQLException {
        stream.writeString(first);
        stream.writeString(last);
    }
}

class Man implements SQLData {
    public int id;

    public Name name;

    private String sql_type = "man_t";

    public Man(int i, Name string) {
        id = i;
        name = string;
    }

    public String getSQLTypeName() {
        return sql_type;
    }

    public void readSQL(SQLInput stream, String type) throws SQLException {
        sql_type = type;
        id = stream.readInt();
        name = (Name) stream.readObject();
    }

    public void writeSQL(SQLOutput stream) throws SQLException {
        stream.writeInt(id);
        stream.writeObject(name);
    }
}
