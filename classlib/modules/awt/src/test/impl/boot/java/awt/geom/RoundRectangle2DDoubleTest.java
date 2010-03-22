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

public class RoundRectangle2DDoubleTest extends GeomTestCase {

    RoundRectangle2D.Double r;

    public RoundRectangle2DDoubleTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        r = new RoundRectangle2D.Double(1, 2, 3, 4, 5, 6);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreate() {
        assertEquals(new RoundRectangle2D.Double(), new RoundRectangle2D.Double(0, 0, 0, 0, 0, 0));
    }

    public void testGetX() {
        assertEquals(1.0, r.getX(), 0.0);
    }

    public void testGetY() {
        assertEquals(2.0, r.getY(), 0.0);
    }

    public void testGetWidth() {
        assertEquals(3.0, r.getWidth(), 0.0);
    }

    public void testGetHeight() {
        assertEquals(4.0, r.getHeight(), 0.0);
    }

    public void testGetArcWidth() {
        assertEquals(5.0, r.getArcWidth(), 0.0);
    }

    public void testGetArcHeight() {
        assertEquals(6.0, r.getArcHeight(), 0.0);
    }

    public void testIsEmpty() {
        assertFalse(r.isEmpty());
        assertTrue(new RoundRectangle2D.Double().isEmpty());
        assertTrue(new RoundRectangle2D.Double(0, 0, -1, -2, 3, 4).isEmpty());
    }

    public void testSetRoundRect1() {
        r.setRoundRect(7.0, 8.0, 9.0, 10.0, 11.0, 12.0);
        assertEquals(new RoundRectangle2D.Double(7, 8, 9, 10, 11, 12), r);
    }

    public void testSetRoundRect2() {
        r.setRoundRect(7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f);
        assertEquals(new RoundRectangle2D.Double(7, 8, 9, 10, 11, 12), r);
    }

    public void testSetRoundRect3() {
        r.setRoundRect(new RoundRectangle2D.Double(7, 8, 9, 10, 11, 12));
        assertEquals(new RoundRectangle2D.Double(7, 8, 9, 10, 11, 12), r);
    }

    public void testGetBounds() {
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4), r.getBounds());
        assertEquals(new Rectangle2D.Double(), new RoundRectangle2D.Double().getBounds());
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(RoundRectangle2DDoubleTest.class);
    }

}
