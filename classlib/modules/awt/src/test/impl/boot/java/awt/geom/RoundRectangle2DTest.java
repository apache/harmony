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
package java.awt.geom;

import java.awt.geom.ShapeTestCase;

public class RoundRectangle2DTest extends ShapeTestCase {

    public RoundRectangle2DTest(String name) {
        super(name);
//        filterImage = createFilter("^(round).*([.]ico)$", "(.*)((affine)|(flat)|(bounds))(.*)");
        filterShape = createFilter("^(round).*([.]shape)$", null);
    }

    public void testSetRoundRect() {
        RoundRectangle2D r = new RoundRectangle2D.Double(1, 2, 3, 4, 5, 6);
        r.setRoundRect(7, 8, 9, 10, 11, 12);
        assertEquals(7.0, r.getX(), 0.0);
        assertEquals(8.0, r.getY(), 0.0);
        assertEquals(9.0, r.getWidth(), 0.0);
        assertEquals(10.0, r.getHeight(), 0.0);
        assertEquals(11.0, r.getArcWidth(), 0.0);
        assertEquals(12.0, r.getArcHeight(), 0.0);
    }

    public void testSetFrame() {
        RoundRectangle2D r = new RoundRectangle2D.Double(1, 2, 3, 4, 5, 6);
        r.setFrame(7, 8, 9, 10);
        assertEquals(7.0, r.getX(), 0.0);
        assertEquals(8.0, r.getY(), 0.0);
        assertEquals(9.0, r.getWidth(), 0.0);
        assertEquals(10.0, r.getHeight(), 0.0);
        assertEquals(5.0, r.getArcWidth(), 0.0);
        assertEquals(6.0, r.getArcHeight(), 0.0);
    }

    public void testGetPathIteratorEmpty() {
        // Regression test HARMONY-1585
        RoundRectangle2D e = new RoundRectangle2D.Double();
        PathIterator p = e.getPathIterator(null);
        checkPathMove(p, false, 0, 0, 0.0);
        checkPathLine(p, false, 0, 0, 0.0);
        checkPathCubic(p, false, 0, 0, 0, 0, 0, 0, 0.0);
        checkPathLine(p, false, 0, 0, 0.0);
        checkPathCubic(p, false, 0, 0, 0, 0, 0, 0, 0.0);
        checkPathLine(p, false, 0, 0, 0.0);
        checkPathCubic(p, false, 0, 0, 0, 0, 0, 0, 0.0);
        checkPathLine(p, false, 0, 0, 0.0);
        checkPathCubic(p, false, 0, 0, 0, 0, 0, 0, 0.0);
        checkPathClose(p, true);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(RoundRectangle2DTest.class);
    }

}
