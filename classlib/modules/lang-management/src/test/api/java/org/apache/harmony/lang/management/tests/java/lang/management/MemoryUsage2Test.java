/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 */

package org.apache.harmony.lang.management.tests.java.lang.management;

import java.lang.management.MemoryUsage;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import junit.framework.TestCase;

public class MemoryUsage2Test extends TestCase {

    private MemoryUsage mu;

    private static final long GOOD_MU_INIT_VAL = 1024;

    private static final long GOOD_MU_USED_VAL = 2 * 1024;

    private static final long GOOD_MU_COMMITTED_VAL = 5 * 1024;

    private static final long GOOD_MU_MAX_VAL = 10 * 1024;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mu = new MemoryUsage(GOOD_MU_INIT_VAL, GOOD_MU_USED_VAL,
                GOOD_MU_COMMITTED_VAL, GOOD_MU_MAX_VAL);
    }

    /*
     * Test method for 'java.lang.management.MemoryUsage.MemoryUsage(long, long,
     * long, long)'
     */
    public void testMemoryUsage() throws Exception {
        // Expect IllegalArgumentException if the value of init or max is
        // negative but not -1
        MemoryUsage memUsage = new MemoryUsage(-1, GOOD_MU_USED_VAL,
                GOOD_MU_COMMITTED_VAL, -1);
        assertEquals(-1, memUsage.getInit());
        assertEquals(GOOD_MU_USED_VAL, memUsage.getUsed());
        assertEquals(GOOD_MU_COMMITTED_VAL, memUsage.getCommitted());
        assertEquals(-1, memUsage.getMax());
        
        try {
            memUsage = new MemoryUsage(-2, GOOD_MU_USED_VAL,
                    GOOD_MU_COMMITTED_VAL, -1);
            fail("should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }

        try {
            memUsage = new MemoryUsage(GOOD_MU_INIT_VAL, GOOD_MU_USED_VAL,
                    GOOD_MU_COMMITTED_VAL, -200);
            fail("should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }

        // Expect IllegalArgumentException if the value of used or committed
        // is negative
        try {
            memUsage = new MemoryUsage(GOOD_MU_INIT_VAL, -1,
                    GOOD_MU_COMMITTED_VAL, GOOD_MU_MAX_VAL);
            fail("should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }

        try {
            memUsage = new MemoryUsage(GOOD_MU_INIT_VAL, GOOD_MU_USED_VAL,
                    -399, GOOD_MU_MAX_VAL);
            fail("should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }

        // Expect IllegalArgumentException if the value of used is greater
        // than the value of committed
        try {
            memUsage = new MemoryUsage(GOOD_MU_INIT_VAL, 3 * GOOD_MU_USED_VAL,
                    GOOD_MU_COMMITTED_VAL, GOOD_MU_MAX_VAL);
            fail("should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }

        // Expect IllegalArgumentException if the value of committed is greater
        // than the value of max...
        try {
            memUsage = new MemoryUsage(GOOD_MU_INIT_VAL, GOOD_MU_USED_VAL,
                    4 * GOOD_MU_COMMITTED_VAL, GOOD_MU_MAX_VAL);
            fail("should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }

        // ... but only if the max has been defined
        memUsage = new MemoryUsage(GOOD_MU_INIT_VAL, GOOD_MU_USED_VAL,
                4 * GOOD_MU_COMMITTED_VAL, -1);
    }

    /*
     * Test method for 'java.lang.management.MemoryUsage.getCommitted()'
     */
    public void testGetCommitted() {
        assertEquals(GOOD_MU_COMMITTED_VAL, mu.getCommitted());
    }

    /*
     * Test method for 'java.lang.management.MemoryUsage.getInit()'
     */
    public void testGetInit() {
        assertEquals(GOOD_MU_INIT_VAL, mu.getInit());
    }

    /*
     * Test method for 'java.lang.management.MemoryUsage.getMax()'
     */
    public void testGetMax() {
        assertEquals(GOOD_MU_MAX_VAL, mu.getMax());
    }

    /*
     * Test method for 'java.lang.management.MemoryUsage.getUsed()'
     */
    public void testGetUsed() {
        assertEquals(GOOD_MU_USED_VAL, mu.getUsed());
    }

    /*
     * Test method for 'java.lang.management.MemoryUsage.from(CompositeData)'
     */
    public void testFrom() throws Exception {
        MemoryUsage memUsage = MemoryUsage.from(createGoodCompositeData());
        assertNotNull(memUsage);
        assertEquals(GOOD_MU_INIT_VAL, memUsage.getInit());
        assertEquals(GOOD_MU_USED_VAL, memUsage.getUsed());
        assertEquals(GOOD_MU_COMMITTED_VAL, memUsage.getCommitted());
        assertEquals(GOOD_MU_MAX_VAL, memUsage.getMax());
    }

    private static CompositeData createGoodCompositeData() throws OpenDataException {
        String[] names = { "init", "used", "committed", "max" };
        Object[] values = {
        /* init */new Long(GOOD_MU_INIT_VAL),
        /* used */new Long(GOOD_MU_USED_VAL),
        /* committed */new Long(GOOD_MU_COMMITTED_VAL),
        /* max */new Long(GOOD_MU_MAX_VAL) };
        CompositeType cType = createGoodMemoryUsageCompositeType();
            return new CompositeDataSupport(cType, names, values);
    }

    private static CompositeType createGoodMemoryUsageCompositeType() throws OpenDataException {
            String[] typeNames = { "init", "used", "committed", "max" };
            String[] typeDescs = { "init", "used", "committed", "max" };
            OpenType[] typeTypes = { SimpleType.LONG, SimpleType.LONG,
                    SimpleType.LONG, SimpleType.LONG };
            return new CompositeType(MemoryUsage.class.getName(),
                    MemoryUsage.class.getName(), typeNames, typeDescs,
                    typeTypes);
    }

}
