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
 * @author Denis M. Kishenko
 */
package java.awt.geom;

import java.awt.Rectangle;

import junit.framework.TestCase;

public class FlatteningPathIteratorTest extends TestCase {

    PathIterator p;
    FlatteningPathIterator fp;

    public FlatteningPathIteratorTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        p = new Rectangle(1, 2, 3, 4).getPathIterator(null);
        fp = new FlatteningPathIterator(p, 10, 5);
    }

    @Override
    protected void tearDown() throws Exception {
        fp = null;
        super.tearDown();
    }

    public void testCreateInvalid() {
        try {
            new FlatteningPathIterator(p, -1, 5);
            fail("expected IllegalArgumentException");
        } catch(IllegalArgumentException e) {
            // expected
        }

        try {
            new FlatteningPathIterator(p, 1, -5);
            fail("expected IllegalArgumentException");
        } catch(IllegalArgumentException e) {
            // expected
        }
        
        // Regression test HARMONY-1419
        try {
            new FlatteningPathIterator(null, -1, 5);
            fail("expected IllegalArgumentException");
        } catch(IllegalArgumentException e) {
            // expected
        }
        
        try {
            new FlatteningPathIterator(null, 1, 5);
            fail("expected NPE");
        } catch(NullPointerException e) {
            // expected
        }
        
    }

    public void testGetFlatness() {
        assertEquals("Flatness", 10, fp.getFlatness(), 0.0);
    }

    public void testGetRecursionLimit() {
        assertEquals("Limit", 5, fp.getRecursionLimit(), 0.0);
    }

    public void testGetWindingRule() {
        assertEquals("Rule", p.getWindingRule(), fp.getWindingRule());
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FlatteningPathIteratorTest.class);
    }

}
