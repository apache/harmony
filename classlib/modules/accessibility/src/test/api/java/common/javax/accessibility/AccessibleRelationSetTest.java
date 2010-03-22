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

import java.util.Vector;

import junit.framework.TestCase;

public class AccessibleRelationSetTest extends TestCase {
    private AccessibleRelationSet set;

    private AccessibleRelation[] relations;

    @Override
    public void setUp() {
        set = new AccessibleRelationSet();
        relations = new AccessibleRelation[] {
                new AccessibleRelation(AccessibleRelation.CONTROLLED_BY),
                new AccessibleRelation(AccessibleRelation.MEMBER_OF) };
        set.addAll(relations);
    }

    @Override
    public void tearDown() {
        set = null;
        relations = null;
    }

    public void testAccessibleRelationSet() {
        assertNotNull(set.toArray());

        try {
            new AccessibleRelationSet(null);
            fail("expected null pointer exception");
        } catch (NullPointerException e) {
        }
        AccessibleRelationSet set2 = new AccessibleRelationSet(relations);
        for (AccessibleRelation re : relations) {
            assertTrue("new set did not contain the relation", set2.contains(re
                    .getKey()));
        }
    }

    public void testAddContains() {
        assertTrue("Should contain added item", set
                .contains(AccessibleRelation.CONTROLLED_BY));
        assertTrue("Should contain added item", set
                .contains(AccessibleRelation.MEMBER_OF));

        AccessibleRelation relation = new AccessibleRelation(
                AccessibleRelation.CONTROLLER_FOR);
        assertTrue(set.add(relation));
        assertTrue(set.add(relation));
        assertTrue("Should contain added item", set
                .contains(AccessibleRelation.CONTROLLER_FOR));
        assertFalse("Should not contain not added item", set
                .contains(AccessibleRelation.LABEL_FOR));

    }

    public void testNullOperations() {
        set.clear();
        assertFalse("Empty set should not contain any item", set
                .contains(AccessibleRelation.LABEL_FOR));
        assertNull("Empty set should not contain any item", set
                .get(AccessibleRelation.LABEL_FOR));
        assertFalse("Empty set should not contain any item", set.remove(set
                .get(AccessibleRelation.LABEL_FOR)));
        set.add(new AccessibleRelation(AccessibleRelation.CONTROLLER_FOR));
        set.clear();
        set.addAll(relations);
    }

    public void testDupes() {
        AccessibleRelation dupeRelation = new AccessibleRelation(
                AccessibleRelation.CONTROLLED_BY);
        assertTrue("Add duplicate item", set.add(dupeRelation));
        assertTrue("Add duplicate item", set.add(relations[0]));
        assertNotSame(dupeRelation, set.get(AccessibleRelation.CONTROLLED_BY));
        assertSame(relations[0], set.get(AccessibleRelation.CONTROLLED_BY));
        set.remove(set.get(AccessibleRelation.CONTROLLED_BY));

        set.clear();
        set.addAll(relations);
        set.addAll(relations);
        assertEquals("Should not add duplicate items in addAll",
                relations.length, set.size());
    }

    public void testGet() {
        assertSame("Get should return same value", relations[0], set
                .get(AccessibleRelation.CONTROLLED_BY));
        assertNull("Get should return null if no such element present", set
                .get(AccessibleRelation.CONTROLLER_FOR));
    }

    public void testClear() {
        set.clear();
        assertEquals("Cleared array should be empty", 0, set.size());
    }

    public void testRemove() {
        set.add(new AccessibleRelation(AccessibleRelation.CONTROLLER_FOR));
        assertTrue(set.remove(set.get(AccessibleRelation.CONTROLLER_FOR)));
        assertFalse(set.contains(AccessibleRelation.CONTROLLER_FOR));
    }

    public void testToString() {
        String stateSetString = set.toString();
        assertTrue(
                "String representation should contain elements representation",
                stateSetString.indexOf(relations[0].toString()) >= 0);
        assertTrue(
                "String representation should contain elements representation",
                stateSetString.indexOf(relations[1].toString()) >= 0);

        set.clear();
        set.toString();
    }

    // Regression for HARMONY-2457
    public void test_constructor() {
        TestAccessibleRelationSet obj = new TestAccessibleRelationSet();
        assertNull(obj.relations);
    }

    static class TestAccessibleRelationSet extends AccessibleRelationSet {
        Vector relations;

        TestAccessibleRelationSet() {
            super();
            relations = super.relations;
        }
    }
}
