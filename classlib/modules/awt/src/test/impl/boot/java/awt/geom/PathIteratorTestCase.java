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

import java.util.NoSuchElementException;

public class PathIteratorTestCase extends GeomTestCase {

    double dcoords[] = new double[6];
    float fcoords[] = new float[6];

    public PathIteratorTestCase(String name) {
        super(name);
    }

    public void checkPathDone(PathIterator p, boolean last) {
        if (last) {
            assertTrue("Expected path completed", p.isDone());
            try {
                p.currentSegment(dcoords);
                fail("Expected exception NoSuchElementException");
            }catch(NoSuchElementException e) {
            }
            try {
                p.currentSegment(fcoords);
                fail("Expected exception NoSuchElementException");
            }catch(NoSuchElementException e) {
            }
        } else {
            assertFalse("Expected path continue", p.isDone());
        }
    }

    public void checkPathRule(PathIterator p, int rule) {
        assertEquals("Rule", rule, p.getWindingRule());
    }

    public void checkPathMove(PathIterator p, boolean last, double x1, double y1, double delta) {
        checkPathDone(p, false);
        assertEquals("Segment type", PathIterator.SEG_MOVETO, p.currentSegment(dcoords));
        assertEquals("Coordinates", new double[] {x1, y1}, dcoords, 2, delta);
        p.next();
        checkPathDone(p, last);
    }

    public void checkPathMove(PathIterator p, boolean last, float x1, float y1, float delta) {
        checkPathDone(p, false);
        assertEquals("Segment type", PathIterator.SEG_MOVETO, p.currentSegment(fcoords));
        assertEquals("Coordinates", new float[] {x1, y1}, fcoords, 2, delta);
        p.next();
        checkPathDone(p, last);
    }

    public void checkPathLine(PathIterator p, boolean last, double x1, double y1, double delta) {
        checkPathDone(p, false);
        assertEquals("Segment type", PathIterator.SEG_LINETO, p.currentSegment(dcoords));
        assertEquals("Coordinates", new double[] {x1, y1}, dcoords, 2, delta);
        p.next();
        checkPathDone(p, last);
    }

    public void checkPathLine(PathIterator p, boolean last, float x1, float y1, float delta) {
        checkPathDone(p, false);
        assertEquals("Segment type", PathIterator.SEG_LINETO, p.currentSegment(fcoords));
        assertEquals("Coordinates", new float[] {x1, y1}, fcoords, 2, delta);
        p.next();
        checkPathDone(p, last);
    }

    public void checkPathQuad(PathIterator p, boolean last, double x1, double y1, double x2, double y2, double delta) {
        checkPathDone(p, false);
        assertEquals("Segment type", PathIterator.SEG_QUADTO, p.currentSegment(dcoords));
        assertEquals("Coordinates", new double[] {x1, y1, x2, y2}, dcoords, 4, delta);
        p.next();
        checkPathDone(p, last);
    }

    public void checkPathQuad(PathIterator p, boolean last, float x1, float y1, float x2, float y2, float delta) {
        checkPathDone(p, false);
        assertEquals("Segment type", PathIterator.SEG_QUADTO, p.currentSegment(fcoords));
        assertEquals("Coordinates", new float[] {x1, y1, x2, y2}, fcoords, 4, delta);
        p.next();
        checkPathDone(p, last);
    }

    public void checkPathCubic(PathIterator p, boolean last, double x1, double y1, double x2, double y2, double x3, double y3, double delta) {
        checkPathDone(p, false);
        assertEquals("Segment type", PathIterator.SEG_CUBICTO, p.currentSegment(dcoords));
        assertEquals("Coordinates", new double[] {x1, y1, x2, y2, x3, y3}, dcoords, 6, delta);
        p.next();
        checkPathDone(p, last);
    }

    public void checkPathCubic(PathIterator p, boolean last, float x1, float y1, float x2, float y2, float x3, float y3, float delta) {
        checkPathDone(p, false);
        assertEquals("Segment type", PathIterator.SEG_CUBICTO, p.currentSegment(fcoords));
        assertEquals("Coordinates", new float[] {x1, y1, x2, y2, x3, y3}, fcoords, 6, delta);
        p.next();
        checkPathDone(p, last);
    }

    public void checkPathClose(PathIterator p, boolean last) {
        checkPathDone(p, false);
        assertEquals("Segment type", PathIterator.SEG_CLOSE, p.currentSegment(dcoords));
        p.next();
        checkPathDone(p, last);
    }

}
