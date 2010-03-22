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

public class CubicCurve2DDoubleTest extends GeomTestCase {

    CubicCurve2D.Double c;

    public CubicCurve2DDoubleTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        c = new CubicCurve2D.Double(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Override
    protected void tearDown() throws Exception {
        c = null;
        super.tearDown();
    }

    public void testCreate() {
        assertEquals(new CubicCurve2D.Double(), new CubicCurve2D.Double(0, 0, 0, 0, 0, 0, 0, 0), 0.0f);
    }


    public void testGetX1() {
        assertEquals(1.0, c.getX1(), 0.0);
    }

    public void testGetY1() {
        assertEquals(2.0, c.getY1(), 0.0);
    }

    public void testGetCtrlX1() {
        assertEquals(3.0, c.getCtrlX1(), 0.0);
    }

    public void testGetCtrlY1() {
        assertEquals(4.0, c.getCtrlY1(), 0.0);
    }

    public void testGetCtrlX2() {
        assertEquals(5.0, c.getCtrlX2(), 0.0);
    }

    public void testGetCtrlY2() {
        assertEquals(6.0, c.getCtrlY2(), 0.0);
    }

    public void testGetX2() {
        assertEquals(7.0, c.getX2(), 0.0);
    }

    public void testGetY2() {
        assertEquals(8.0, c.getY2(), 0.0);
    }

    public void testGetP1() {
        assertEquals(new Point2D.Double(1, 2), c.getP1());
    }

    public void testGetCtrlP1() {
        assertEquals(new Point2D.Double(3, 4), c.getCtrlP1());
    }

    public void testGetCtrlP2() {
        assertEquals(new Point2D.Double(5, 6), c.getCtrlP2());
    }

    public void testGetP2() {
        assertEquals(new Point2D.Double(7, 8), c.getP2());
    }

    public void testSetCurve1() {
        c.setCurve(9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0);
        assertEquals(new CubicCurve2D.Double(9, 10, 11, 12, 13, 14, 15, 16), c, 0.0);
    }

    public void testSetCurve2() {
        c.setCurve(9.0f, 10.0f, 11.0f, 12.0f, 13.0f, 14.0f, 15.0f, 16.0f);
        assertEquals(new CubicCurve2D.Double(9, 10, 11, 12, 13, 14, 15, 16), c, 0.0);
    }

    public void testGetBounds2D() {
        for (double[][] element : CubicCurve2DTest.bounds) {
            CubicCurve2D curve = new CubicCurve2D.Double();
            curve.setCurve(element[0], 0);
            assertEquals(
                    cubicToStr(curve),
                    new Rectangle2D.Double(
                            (int)element[1][0],
                            (int)element[1][1],
                            (int)element[1][2],
                            (int)element[1][3]),
                    curve.getBounds2D());
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CubicCurve2DDoubleTest.class);
    }

}
