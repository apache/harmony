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

package javax.accessibility;

import java.util.Arrays;
import java.util.Vector;

import junit.framework.TestCase;

public class AccessibleStateSetTest extends TestCase {
    private AccessibleStateSet stateSet;

    private AccessibleState[] statesArray;

    @Override
    public void setUp() {
        stateSet = new AccessibleStateSet();
        statesArray = new AccessibleState[] { AccessibleState.ACTIVE,
                AccessibleState.ARMED };
        stateSet.addAll(statesArray);
    }

    @Override
    public void tearDown() {
        stateSet = null;
        statesArray = null;
    }

    public void testAccessibleStateSet() throws Exception {
        AccessibleState[] statesArray = { AccessibleState.ACTIVE,
                AccessibleState.ARMED };
        stateSet = new AccessibleStateSet(statesArray);
        assertNotNull(stateSet.states);

        try {
            new AccessibleStateSet(null);
            fail("expected null pointer exception");
        } catch (NullPointerException e) {
        }
    }

    public void testAddContains() throws Exception {
        assertTrue("Must contain added state", stateSet
                .contains(AccessibleState.ACTIVE));
        assertTrue("Must contain added state", stateSet
                .contains(AccessibleState.ARMED));
        boolean added = stateSet.add(AccessibleState.ACTIVE);
        assertEquals("Should not add duplicate item", 2, stateSet.states.size());
        assertFalse("Should not add duplicate item", added);
        assertFalse(stateSet.contains(null));

        assertTrue(stateSet.add(null));
        assertTrue(stateSet.contains(null));

        stateSet.states = null;
        assertFalse(stateSet.contains(null));
        assertNull(stateSet.states);

        stateSet.states = null;
        stateSet.add(AccessibleState.ACTIVE);
    }

    public void testAddAll() {
        stateSet.addAll(statesArray);
        stateSet.addAll(statesArray);
        assertEquals("Should not add duplicate items", statesArray.length,
                stateSet.states.size());

        try {
            stateSet.addAll(null);
            fail("expected null pointer exception");
        } catch (NullPointerException e) {
        }
    }

    public void testRemove() throws Exception {
        boolean removed = stateSet.remove(AccessibleState.ICONIFIED);
        assertFalse("Should not remove non-existing item", removed);
        removed = stateSet.remove(AccessibleState.ACTIVE);
        assertFalse("Should remove existing item", stateSet
                .contains(AccessibleState.ACTIVE));
        assertTrue("Should remove existing item", removed);
    }

    public void testClear() throws Exception {

        stateSet.clear();
        assertEquals("Cleared set should be empty", 0, stateSet.states.size());

        stateSet.states = null;
        stateSet.clear();
    }

    public void testToString() throws Exception {
        String stateSetString = stateSet.toString();
        assertTrue(
                "String representation should contain elements representation",
                stateSetString.indexOf(AccessibleState.ACTIVE.toString()) >= 0);
        assertTrue(
                "String representation should contain elements representation",
                stateSetString.indexOf(AccessibleState.ARMED.toString()) >= 0);

        stateSet.states = null;
        stateSet.toString();

        // regression test for HARMONY-1190
        try {
            new AccessibleStateSet(new AccessibleState[2]).toString();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
        
        assertNull(new AccessibleStateSet(new AccessibleState[0]).toString());
    }

    public void testToArray() throws Exception {
        AccessibleState[] statesReturnedArray = stateSet.toArray();
        assertEquals("Returned array size don't match", statesArray.length,
                statesReturnedArray.length);
        for (int i = 0; i < statesReturnedArray.length; i++)
            assertEquals("Returned element mismatch:" + i, statesArray[i],
                    statesReturnedArray[i]);
        stateSet.states = null;
        Arrays.asList(stateSet.toArray());
    }

    // Regression for HARMONY-2457
    public void test_constructor() {
        TestAccessibleStateSet obj = new TestAccessibleStateSet();
        assertNull(obj.states);
    }

    static class TestAccessibleStateSet extends AccessibleStateSet {
        Vector states;

        TestAccessibleStateSet() {
            super();
            states = super.states;
        }
    }

}
