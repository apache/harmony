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

import java.sql.Ref;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;

import javax.sql.rowset.serial.SerialException;
import javax.sql.rowset.serial.SerialRef;

import junit.framework.TestCase;

/**
 * @tests SerialRef
 */
public class SerialRefTest extends TestCase {

    private SerialRef sr;

    private SerialRef sr2;

    private MockRef ref;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ref = new MockRef();
        sr = new SerialRef(ref);

        MockAbnormalRef maf = new MockAbnormalRef();
        sr2 = new SerialRef(maf);
    }

    /**
     * @tests javax.sql.rowset.serial.SerialRef#SerialRef(Ref ref)
     */
    public void testConstructorRef() throws SerialException, SQLException {
        try {
            new SerialRef(null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            ref.setBaseTypeName(null);
            new SerialRef(ref);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests javax.sql.rowset.serial.SerialRef#getBaseTypeName()
     */
    public void testGetBaseTypeName() throws SQLException {
        assertEquals(MockRef.BASE_TYPE_NAME, sr.getBaseTypeName());
    }

    /**
     * @tests javax.sql.rowset.serial.SerialRef#getObject()
     */
    public void testGetObject() throws SQLException {
        assertSame(ref.obj1, sr.getObject());

        sr.setObject(null);
        assertNull(sr.getObject());

        Object obj = new Object();
        sr.setObject(obj);
        assertSame(obj, sr.getObject());

        try {
            sr2.getObject();
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            sr2.setObject(obj);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

    }

    /**
     * @tests javax.sql.rowset.serial.SerialRef#getObject(Map)
     */
    public void testGetObjectLjava_util_Map() throws SQLException {
        try {
            assertNull(sr.getObject(null));
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        Map<String, Class<?>> map = new Hashtable<String, Class<?>>();
        assertNull(sr.getObject(map));

        map.put("MockRef", MockRef.class);
        assertNull(sr.getObject(map));

        sr.setObject("MockRef1");
        assertNull(sr.getObject(map));

        sr.setObject("MockRef");
        assertSame(MockRef.class, sr.getObject(map));
    }

    static class MockRef implements Ref {
        private static final String BASE_TYPE_NAME = "MockBaseTypeName";

        String baseTypeName = BASE_TYPE_NAME;

        Object obj1 = new Object();

        Object obj2 = new Object();

        public String getBaseTypeName() throws SQLException {
            return baseTypeName;
        }

        public Object getObject() throws SQLException {
            return obj1;
        }

        public Object getObject(Map<String, Class<?>> map) throws SQLException {
            return obj2;
        }

        public void setObject(Object value) throws SQLException {
            obj1 = value;
        }

        public void setBaseTypeName(String name) {
            baseTypeName = name;
        }
    };

    static class MockAbnormalRef extends MockRef {
        @Override
        public Object getObject() throws SQLException {
            throw new SQLException();
        }

        @Override
        public Object getObject(Map<String, Class<?>> map) throws SQLException {
            throw new SQLException();
        }

        @Override
        public void setObject(Object value) throws SQLException {
            throw new SQLException();
        }
    };

}
