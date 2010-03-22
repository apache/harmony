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

public class QuadCurve2DDoubleTest extends GeomTestCase {

    QuadCurve2D.Double q;

    public QuadCurve2DDoubleTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        q = new QuadCurve2D.Double(1, 2, 3, 4, 5, 6);
    }

    @Override
    protected void tearDown() throws Exception {
        q = null;
        super.tearDown();
    }

    public void testCreate() {
        assertEquals(new QuadCurve2D.Double(), new QuadCurve2D.Double(0, 0, 0, 0, 0, 0), 0.0);
    }

    public void testGetX1() {
        assertEquals(1.0, q.getX1(), 0.0);
    }

    public void testGetY1() {
        assertEquals(2.0, q.getY1(), 0.0);
    }

    public void testGetCtrlX() {
        assertEquals(3.0, q.getCtrlX(), 0.0);
    }

    public void testGetCtrlY() {
        assertEquals(4.0, q.getCtrlY(), 0.0);
    }

    public void testGetX2() {
        assertEquals(5.0, q.getX2(), 0.0);
    }

    public void testGetY2() {
        assertEquals(6.0, q.getY2(), 0.0);
    }

    public void testGetP1() {
        assertEquals(new Point2D.Double(1, 2), q.getP1());
    }

    public void testGetCtrlPt() {
        assertEquals(new Point2D.Double(3, 4), q.getCtrlPt());
    }

    public void testGetP2() {
        assertEquals(new Point2D.Double(5, 6), q.getP2());
    }

    public void testSetCurve1() {
        q.setCurve(7.0, 8.0, 9.0, 10.0, 11.0, 12.0);
        assertEquals(new QuadCurve2D.Double(7, 8, 9, 10, 11, 12), q, 0.0);
    }

    public void testSetCurve2() {
        q.setCurve(7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f);
        assertEquals(new QuadCurve2D.Double(7, 8, 9, 10, 11, 12), q, 0.0);
    }

    public void testGetBounds2D() {
        for (double[][] element : QuadCurve2DTest.bounds) {
            QuadCurve2D curve = new QuadCurve2D.Double();
            curve.setCurve(element[0], 0);
            assertEquals(
                    quadToStr(curve),
                    new Rectangle2D.Double(
                            (int)element[1][0],
                            (int)element[1][1],
                            (int)element[1][2],
                            (int)element[1][3]),
                    curve.getBounds2D());
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(QuadCurve2DDoubleTest.class);
    }

}
