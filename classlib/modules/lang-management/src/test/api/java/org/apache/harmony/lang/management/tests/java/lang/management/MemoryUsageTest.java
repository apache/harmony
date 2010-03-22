/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.lang.management.tests.java.lang.management;

import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryUsage;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import junit.framework.TestCase;

public class MemoryUsageTest extends TestCase {

    private static final String CLASS_NAME = MemoryNotificationInfo.class
            .getName();

    public void testFrom() throws Exception {
        String[] itemNames = { "init", "used", "committed", "max" };
        Object[] items = { Long.valueOf(1024), Long.valueOf(2048), Long.valueOf(4096),
                Long.valueOf(8128) };
        CompositeType muType = new CompositeType(MemoryUsage.class.getName(),
                "memory usage snapshot", itemNames, itemNames, new OpenType[] {
                        SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG });
        CompositeData cd = new CompositeDataSupport(muType, itemNames, items);

        MemoryUsage mu = MemoryUsage.from(cd);
        assertEquals(4096, mu.getCommitted());
        assertEquals(1024, mu.getInit());
        assertEquals(8128, mu.getMax());
        assertEquals(2048, mu.getUsed());

        // test illegal argument exception with an invalid value
        // TODO This does not fail on Sun RI 5.0_7 - a bug has been logged
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6464013
        // http://issues.apache.org/jira/browse/HARMONY-1284
        items = new Object[] { Long.valueOf(-2), Long.valueOf(2048), Long.valueOf(4096),
                Long.valueOf(8128) };
        cd = new CompositeDataSupport(muType, itemNames, items);

        try {
            mu = MemoryUsage.from(cd);
            fail("No illegal argument exception for invalid CompositeData");
        } catch (IllegalArgumentException e) {
        }

        // test illegal argument exception with an invalid type
        itemNames = new String[] { "init", "used", "committed", "maximum" };
        items = new Object[] { Long.valueOf(1024), Long.valueOf(2048), Long.valueOf(4096),
                Integer.valueOf(8128) };

        muType = new CompositeType(MemoryUsage.class.getName(), "memory usage snapshot",
                itemNames, itemNames, new OpenType[] { SimpleType.LONG, SimpleType.LONG,
                        SimpleType.LONG, SimpleType.INTEGER });
        cd = new CompositeDataSupport(muType, itemNames, items);

        try {
            MemoryUsage.from(cd);
            fail("No illegal argument exception for invalid CompositeData");
        } catch (IllegalArgumentException e) {
        }

        // test illegal argument exception with an invalid name
        items = new Object[] { Long.valueOf(1024), Long.valueOf(2048), Long.valueOf(4096),
                Long.valueOf(8128) };

        muType = new CompositeType(MemoryUsage.class.getName(), "memory usage snapshot",
                itemNames, itemNames, new OpenType[] { SimpleType.LONG, SimpleType.LONG,
                        SimpleType.LONG, SimpleType.LONG });
        cd = new CompositeDataSupport(muType, itemNames, items);

        try {
            MemoryUsage.from(cd);
            fail("No illegal argument exception for invalid CompositeData");
        } catch (IllegalArgumentException e) {
        }
    }

    public void test_from_NullCompositeData() {
        assertNull(MemoryUsage.from(null));
    }

    public void test_from_scenario1() throws Exception {
        String[] names = { "init", "used", "committed", "max", "additionName" };
        Object[] values = { 1024L, 2048L, 4096L, 8128L, "additionalValue" };
        OpenType[] types = { SimpleType.LONG, SimpleType.LONG, SimpleType.LONG,
                SimpleType.LONG, SimpleType.STRING };
        CompositeType compositeType = getCompositeType(names, types);
        CompositeData data = new CompositeDataSupport(compositeType, names,
                values);
        MemoryUsage usage = MemoryUsage.from(data);
        assertEquals(values[0], usage.getInit());
        assertEquals(values[1], usage.getUsed());
        assertEquals(values[2], usage.getCommitted());
        assertEquals(values[3], usage.getMax());
    }

    public void test_from_scenario2() throws Exception {
        String[] names = { "init", "used", "committed" };
        Object[] values = { null, null, null };
        OpenType[] types = { SimpleType.LONG, SimpleType.LONG, SimpleType.LONG };
        CompositeType compositeType = getCompositeType(names, types);
        CompositeData data = new CompositeDataSupport(compositeType, names,
                values);
        try {
            MemoryUsage.from(data);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void test_from_scenario3() throws Exception {
        String[] names = { "init", "used", "committed", "max" };
        Object[] values = { null, 2048L, 4096L, 8128L };
        OpenType[] types = { SimpleType.LONG, SimpleType.LONG, SimpleType.LONG,
                SimpleType.LONG };
        CompositeType compositeType = getCompositeType(names, types);
        CompositeData data = new CompositeDataSupport(compositeType, names,
                values);
        try {
            MemoryUsage.from(data);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    public void test_from_scenario4() throws Exception {
        String[] names = { "init", "used", "committed", "max" };
        Object[] values = { 1024L, null, 4096L, 8128L };
        OpenType[] types = { SimpleType.LONG, SimpleType.LONG, SimpleType.LONG,
                SimpleType.LONG };
        CompositeType compositeType = getCompositeType(names, types);
        CompositeData data = new CompositeDataSupport(compositeType, names,
                values);
        try {
            MemoryUsage.from(data);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    public void test_from_scenario5() throws Exception {
        String[] names = { "init", "used", "committed", "max" };
        Object[] values = { 1024L, 2048L, null, 8128L };
        OpenType[] types = { SimpleType.LONG, SimpleType.LONG, SimpleType.LONG,
                SimpleType.LONG };
        CompositeType compositeType = getCompositeType(names, types);
        CompositeData data = new CompositeDataSupport(compositeType, names,
                values);
        try {
            MemoryUsage.from(data);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    public void test_from_scenario6() throws Exception {
        String[] names = { "init", "used", "committed", "max" };
        Object[] values = { 1024L, 2048L, 4096L, null };
        OpenType[] types = { SimpleType.LONG, SimpleType.LONG, SimpleType.LONG,
                SimpleType.LONG };
        CompositeType compositeType = getCompositeType(names, types);
        CompositeData data = new CompositeDataSupport(compositeType, names,
                values);
        try {
            MemoryUsage.from(data);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    public void test_from_scenario7() throws Exception {
        String[] names = { "noninit", "used", "committed", "max" };
        Object[] values = { 1024L, 2048L, 4096L, 8128L };
        OpenType[] types = { SimpleType.LONG, SimpleType.LONG, SimpleType.LONG,
                SimpleType.LONG };
        CompositeType compositeType = getCompositeType(names, types);
        CompositeData data = new CompositeDataSupport(compositeType, names,
                values);
        try {
            MemoryUsage.from(data);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void test_from_scenario8() throws Exception {
        String[] names = { "init", "used", "committed", "max" };
        Object[] values = { "initValue", 2048L, 4096L, 8128L };
        OpenType[] types = { SimpleType.STRING, SimpleType.LONG,
                SimpleType.LONG, SimpleType.LONG };
        CompositeType compositeType = getCompositeType(names, types);
        CompositeData data = new CompositeDataSupport(compositeType, names,
                values);
        try {
            MemoryUsage.from(data);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    protected CompositeType getCompositeType(String[] typeNames,
            OpenType[] typeTypes) throws Exception {
        return new CompositeType(CLASS_NAME, CLASS_NAME, typeNames, typeNames,
                typeTypes);
    }

    public void testConstructor() {
        try {
            new MemoryUsage(-2, 2048, 4096, 8128);
            fail("No illegal argument exception on init < -1");
        } catch (IllegalArgumentException e) {
        }

        try {
            new MemoryUsage(-1, 2048, 4096, -2);
            fail("No illegal argument exception on max < -1");
        } catch (IllegalArgumentException e) {
        }

        try {
            new MemoryUsage(-1, -1, 4096, 8128);
            fail("No illegal argument exception on used < 0");
        } catch (IllegalArgumentException e) {
        }

        try {
            new MemoryUsage(-1, 2048, -1, 8128);
            fail("No illegal argument exception on committed < 0");
        } catch (IllegalArgumentException e) {
        }

        try {
            new MemoryUsage(-1, 2048, 1024, 8128);
            fail("No illegal argument exception on used > committed");
        } catch (IllegalArgumentException e) {
        }

        try {
            new MemoryUsage(-1, 2048, 4096, 1024);
            fail("No illegal argument exception on committed > max");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetCommitted() {
        MemoryUsage mu = new MemoryUsage(1024, 2048, 4096, 8128);
        assertEquals(4096, mu.getCommitted());
    }

    public void testGetInit() {
        MemoryUsage mu = new MemoryUsage(1024, 2048, 4096, 8128);
        assertEquals(1024, mu.getInit());

        mu = new MemoryUsage(-1, 2048, 4096, 8128);
        assertEquals(-1, mu.getInit());

        mu = new MemoryUsage(-1, 2048, 4096, -1);
        assertEquals(-1, mu.getInit());
    }

    public void testGetMax() {
        MemoryUsage mu = new MemoryUsage(1024, 2048, 4096, 8128);
        assertEquals(8128, mu.getMax());

        mu = new MemoryUsage(1024, 2048, 4096, -1);
        assertEquals(-1, mu.getMax());

        mu = new MemoryUsage(-1, 2048, 4096, -1);
        assertEquals(-1, mu.getMax());
    }

    public void testGetUsed() {
        MemoryUsage mu = new MemoryUsage(1024, 2048, 4096, 8128);
        assertEquals(2048, mu.getUsed());
    }

    public void testToString() {
        MemoryUsage mu = new MemoryUsage(1024, 2048, 4096, 8128);
        assertNotNull(mu.toString());
        assertTrue(mu.toString().length() > 0);
    }

}
