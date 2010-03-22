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

import junit.framework.TestCase;

public class Point2DFloatTest extends TestCase {

    Point2D.Float p;

    public Point2DFloatTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        p = new Point2D.Float(1, 2);
    }

    @Override
    protected void tearDown() throws Exception {
        p = null;
        super.tearDown();
    }

    public void testCreate() {
        assertEquals(new Point2D.Float(), new Point2D.Float(0, 0));
    }

    public void testGetX() {
        assertEquals(1.0, p.getX(), 0.0);
    }

    public void testGetY() {
        assertEquals(2.0, p.getY(), 0.0);
    }

    public void testSetLocation1() {
        p.setLocation(3.0, 4.0);
        assertEquals(new Point2D.Float(3, 4), p);
    }

    public void testSetLocation2() {
        p.setLocation(3.0f, 4.0f);
        assertEquals(new Point2D.Float(3, 4), p);
    }

    public void testToString() {
        assertEquals("java.awt.geom.Point2D$Float[x=1.0,y=2.0]", p.toString());
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Point2DFloatTest.class);
    }

}
