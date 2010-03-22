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
package java.awt;

import java.awt.Dimension;


public class DimensionTest extends SerializeTestCase {

    static { 
        SERIALIZATION_TEST = true;
    }
    
    Dimension d;

    public DimensionTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        d = new Dimension(2, 3);
    }

    @Override
    protected void tearDown() throws Exception {
        d = null;
        super.tearDown();
    }

    public void testCreate1() {
        assertEquals(new Dimension(), new Dimension(0, 0));
    }

    public void testCreate2() {
        assertEquals(new Dimension(new Dimension(4, 5)), new Dimension(4, 5));
    }

    public void testCreate3() {
        assertEquals(new Dimension(4, 5), new Dimension(4, 5));
    }

    public void testEquals() {
        assertTrue(d.equals(new Dimension(2, 3)));
        assertTrue(!d.equals(new Dimension(2, 4)));
        assertTrue(!d.equals(new Dimension(1, 3)));
        assertTrue(!d.equals(new Dimension(1, 4)));
    }

    public void testGetWidth() {
        assertEquals(d.getWidth(), 2.0, 0.0);
    }

    public void testGetHeight() {
        assertEquals(d.getHeight(), 3.0, 0.0);
    }

    public void testGetSize() {
        assertEquals(d.getSize(), new Dimension(2, 3));
    }

    public void testHashCode() {
        assertTrue(d.hashCode() == new Dimension(2, 3).hashCode());
        assertTrue(d.hashCode() != new Dimension(2, 4).hashCode());
        assertTrue(d.hashCode() != new Dimension(1, 3).hashCode());
        assertTrue(d.hashCode() != new Dimension(1, 4).hashCode());
    }

    public void testSetSize1() {
        d.setSize(new Dimension(4, 5));
        assertEquals(d, new Dimension(4, 5));
    }

    public void testSetSize2() {
        d.setSize(4, 5);
        assertEquals(d, new Dimension(4, 5));
    }

    public void testSetSize3() {
        d.setSize(4.0, 5.0);
        assertEquals(d, new Dimension(4, 5));
        d.setSize(4.3, 5.7);
        assertEquals(d, new Dimension(5, 6));
        d.setSize(3.5, 4.5);
        assertEquals(d, new Dimension(4, 5));
    }

    public void testToString() {
        assertEquals(d.toString(), "java.awt.Dimension[width=2,height=3]");
    }

    public void testSerializeRead1() {
        checkRead(new Dimension());
    }

    public void testSerializeRead2() {
        checkRead(new Dimension(1, 2));
    }

    public void testSerializeWrite1() {
        checkWrite(new Dimension());
    }

    public void testSerializeWrite2() {
        checkWrite(new Dimension(1, 2));
    }

    public void createSerializeTemplates() {
        saveSerialized(new Dimension());
        saveSerialized(new Dimension(1, 2));
    }

    public static void main(String[] args) {
//        new DimensionTest("").createSerializeTemplates();
        junit.textui.TestRunner.run(DimensionTest.class);
    }

}
