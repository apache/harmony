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

import java.awt.Point;

public class PointTest extends SerializeTestCase {

    static { 
        SERIALIZATION_TEST = true;
    }
    
    public PointTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreate1() {
        assertEquals(new Point(0, 0), new Point());
    }

    public void testCreate2() {
        assertEquals(new Point(1, 2), new Point(new Point(1, 2)));
    }

    public void testGetX() {
        assertEquals(1, (int)new Point(1, 2).getX());
    }

    public void testGetY() {
        assertEquals(2, (int)new Point(1, 2).getY());
    }

    public void testGetLocation() {
        assertEquals(new Point(1, 2), new Point(1, 2).getLocation());
    }

    public void testSetLocationInt() {
        Point p = new Point(1, 2);
        p.setLocation(3, 4);
        assertEquals(new Point(3, 4), p);
    }

    public void testSetLocationDouble1() {
        Point p = new Point(1, 2);
        p.setLocation(3.0, 4.0);
        assertEquals(new Point(3, 4), p);
    }

    public void testSetLocationDouble2() {
        Point p = new Point(1, 2);
        p.setLocation(5.3, 6.7);
        assertEquals(new Point(5, 7), p);
    }

    public void testSetLocationDouble3() {
        Point p = new Point(1, 2);
        p.setLocation(7.5, 8.5);
        assertEquals(new Point(8, 9), p);
    }

    public void testSetLocationDouble4() {
        // Regression test HARMONY-1878
        Point p = new Point(1, 2);
        double x = (double)Integer.MAX_VALUE + (double)1;
        double y = (double)Integer.MIN_VALUE - (double)1;
        p.setLocation(x, y);
        assertEquals(new Point(Integer.MAX_VALUE, Integer.MIN_VALUE), p);
    }    

    public void testSetLocationDouble5() {
        // Regression test HARMONY-1878
        Point p = new Point(1, 2);
        p.setLocation(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        assertEquals(new Point(Integer.MAX_VALUE, Integer.MIN_VALUE), p);
    }    

    public void testSetLocationPoint() {
        Point p = new Point(1, 2);
        p.setLocation(new Point(3, 4));
        assertEquals(new Point(3, 4), p);
    }
    
    public void testMove() {
        Point p = new Point(1, 2);
        p.move(3, 4);
        assertEquals(new Point(3, 4), p);
    }

    public void testTranslate() {
        Point p = new Point(1, 2);
        p.translate(3, 4);
        assertEquals(new Point(4, 6), p);
    }

    public void testToString() {
        assertEquals("java.awt.Point[x=1,y=2]", new Point(1, 2).toString());
    }

    public void testEquals() {
        assertTrue(new Point(1, 2).equals(new Point(1, 2)));
        assertFalse(new Point(3, 2).equals(new Point(1, 2)));
        assertFalse(new Point(1, 3).equals(new Point(1, 2)));
        assertFalse(new Point(3, 3).equals(new Point(1, 2)));
    }

    public void testSerializeRead1() {
        checkRead(new Point());
    }

    public void testSerializeRead2() {
        checkRead(new Point(1, 2));
    }

    public void testSerializeWrite1() {
        checkWrite(new Point());
    }

    public void testSerializeWrite2() {
        checkWrite(new Point(1, 2));
    }

    public void createSerializeTemplates() {
        saveSerialized(new Point());
        saveSerialized(new Point(1, 2));
    }

    public static void main(String[] args) {
//        new PointTest("").createSerializeTemplates();
        junit.textui.TestRunner.run(PointTest.class);
    }

}
