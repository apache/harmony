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

public class Ellipse2DFloatTest extends TestCase {

    Ellipse2D.Float e;

    @Override
    protected void setUp() throws Exception {
        e = new Ellipse2D.Float(1.0f, 2.0f, 3.0f, 4.0f);
    }

    @Override
    protected void tearDown() throws Exception {
        e = null;
    }

    public void testCreate1() {
        e = new Ellipse2D.Float();
        assertNotNull(e);
        assertEquals(e.x, 0.0f, 0.0f);
        assertEquals(e.y, 0.0f, 0.0f);
        assertEquals(e.width, 0.0f, 0.0f);
        assertEquals(e.height, 0.0f, 0.0f);
    }

    public void testCreate2() {
        assertNotNull(e);
        assertEquals(e.x, 1.0f, 0.0f);
        assertEquals(e.y, 2.0f, 0.0f);
        assertEquals(e.width, 3.0f, 0.0f);
        assertEquals(e.height, 4.0f, 0.0f);
    }

    public void testGetX() {
        assertEquals(e.getX(), 1.0f, 0.0f);
    }

    public void testGetY() {
        assertEquals(e.getY(), 2.0f, 0.0f);
    }

    public void testGetWidth() {
        assertEquals(e.getWidth(), 3.0f, 0.0f);
    }

    public void testGetHeight() {
        assertEquals(e.getHeight(), 4.0f, 0.0f);
    }

    public void testGetBounds2D() {
        assertEquals(e.getBounds2D(), new Rectangle2D.Float(1.0f, 2.0f, 3.0f, 4.0f));
    }

    public void testIsEmpty() {
        assertFalse(e.isEmpty());
        assertTrue(new Ellipse2D.Float().isEmpty());
    }

    public void testSetFrame1() {
        e.setFrame(5.0, 6.0, 7.0, 8.0);
        assertEquals(e.x, 5.0, 0.0);
        assertEquals(e.y, 6.0, 0.0);
        assertEquals(e.width, 7.0, 0.0);
        assertEquals(e.height, 8.0, 0.0);
    }

    public void testSetFrame2() {
        e.setFrame(5.0f, 6.0f, 7.0f, 8.0f);
        assertEquals(e.x, 5.0f, 0.0f);
        assertEquals(e.y, 6.0f, 0.0f);
        assertEquals(e.width, 7.0f, 0.0f);
        assertEquals(e.height, 8.0f, 0.0f);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Ellipse2DFloatTest.class);
    }

}
