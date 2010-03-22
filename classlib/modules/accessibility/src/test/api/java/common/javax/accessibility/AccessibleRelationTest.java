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

import junit.framework.TestCase;

public class AccessibleRelationTest extends TestCase {

    public void testGetKey() {
        AccessibleRelation relation = new AccessibleRelation(AccessibleRelation.LABEL_FOR);
        assertEquals(AccessibleRelation.LABEL_FOR, relation.getKey());
    }

    public void testAccessibleRelation() {
        AccessibleRelation relation = new AccessibleRelation(AccessibleRelation.LABEL_FOR);
        assertEquals(0, relation.getTarget().length);
    }

    public void testSetGetTarget() {
        AccessibleRelation relation = new AccessibleRelation(AccessibleRelation.LABEL_FOR);
        StringBuffer target = new StringBuffer("text");
        relation.setTarget(target);
        assertEquals(1, relation.getTarget().length);
        assertSame(target, relation.getTarget()[0]);

        StringBuffer[] targets = new StringBuffer[] { target, target };
        relation.setTarget(targets);
        assertEquals(2, relation.getTarget().length);
        assertNotSame(targets, relation.getTarget());

        relation.setTarget((Object[]) null);
        assertNotNull(relation.getTarget());
        assertEquals(0, relation.getTarget().length);

        relation.setTarget((Object) null);
        assertNotNull(relation.getTarget());
        assertEquals(1, relation.getTarget().length);
        assertNull(relation.getTarget()[0]);
    }

    /**
     * @add tests
     *      {@link javax.accessibility.AccessibleRelation#AccessibleRelation(String, Object)}
     */
    public void test_constructor_Ljava_lang_StringLjava_lang_Object() {
        AccessibleRelation relation = new AccessibleRelation(
                AccessibleRelation.LABEL_FOR, new String("test"));
        assertEquals("target[0] did not equals to parameter passed in", "test",
                (relation.getTarget()[0]).toString());
    }

    /**
     * @add tests
     *      {@link javax.accessibility.AccessibleRelation#AccessibleRelation(String, Object[])}
     */
    public void test_constructor_Ljava_lang_String$Ljava_lang_Object() {
        AccessibleRelation relation = new AccessibleRelation("test",
                new Object[2]);
        assertEquals(2, relation.getTarget().length);
        relation = new AccessibleRelation("test", null);
        assertEquals(0, relation.getTarget().length);
    }
}
