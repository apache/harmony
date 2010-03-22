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

import java.awt.Point;

import junit.framework.TestCase;

public class Point2DTest extends TestCase {

    final static double DELTA = 0.001;

    Point p;

    public Point2DTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        p = new Point(1, 2);
    }

    @Override
    protected void tearDown() throws Exception {
        p = null;
        super.tearDown();
    }

    public void testSetLocation() {
        p.setLocation(new Point(3, 4));
        assertEquals(new Point(3, 4), p);
    }

    public void testClone() {
        assertEquals(p, p.clone());
    }

    public void testEquals() {
        assertTrue(p.equals(p));
        assertTrue(p.equals(new Point(1, 2)));
        assertFalse(p.equals(new Point(1, 3)));
        assertFalse(p.equals(new Point(0, 2)));
        assertFalse(p.equals(new Point(0, 3)));
        assertFalse(p.equals(new Point2D.Double(0, 3)));
    }

    public void testHashCode() {
        assertTrue(p.hashCode() == new Point(1, 2).hashCode());
        assertTrue(p.hashCode() != new Point(1, 3).hashCode());
        assertTrue(p.hashCode() != new Point(0, 2).hashCode());
        assertTrue(p.hashCode() != new Point(0, 0).hashCode());
    }

    public void testDistanceSq1() {
        assertEquals(8.0, Point2D.distanceSq(1, 2, 3, 4), 0.0);
        assertEquals(0.0, Point2D.distanceSq(1, 2, 1, 2), 0.0);
    }

    public void testDistanceSq2() {
        assertEquals(8.0, p.distanceSq(3, 4), 0.0);
        assertEquals(0.0, p.distanceSq(1, 2), 0.0);
    }

    public void testDistanceSq3() {
        assertEquals(8.0, p.distanceSq(new Point(3, 4)), 0.0);
        assertEquals(0.0, p.distanceSq(new Point(1, 2)), 0.0);
    }

    public void testDistance1() {
        assertEquals(Math.sqrt(8.0), Point2D.distance(1, 2, 3, 4), DELTA);
        assertEquals(0.0, Point2D.distance(1, 2, 1, 2), DELTA);
    }

    public void testDistance2() {
        assertEquals(Math.sqrt(8.0), p.distance(3, 4), DELTA);
        assertEquals(0.0, p.distance(1, 2), DELTA);
    }

    public void testDistance3() {
        assertEquals(Math.sqrt(8.0), p.distance(new Point(3, 4)), DELTA);
        assertEquals(0.0, p.distance(new Point(1, 2)), DELTA);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Point2DTest.class);
    }

}
