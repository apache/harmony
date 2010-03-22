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

import java.awt.SerializeTestCase;
import java.awt.Tools;
import java.util.Arrays;

public class GeomTestCase extends SerializeTestCase {

    public GeomTestCase(String name) {
        super(name);
    }

    void fail(String message, Object expected, Object actual) {
        if (message == null) {
            fail("expected: <" + expected + "> but was: <" + actual + ">");
        } else {
            fail(message + " expected: <" + expected + "> but was: <" + actual + ">");
        }
    }

    String concat(String msg1, String msg2) {
        if (msg1 == null) {
            return msg2;
        }
        if (msg2 == null) {
            return msg2;
        }
        return msg1 + ". " + msg2;
    }

    public void assertEquals(String message, Line2D a1, Line2D a2) {
        if (!a1.getClass().equals(a2.getClass()) ||
            !a1.getP1().equals(a2.getP1()) ||
            !a1.getP2().equals(a2.getP2()))
        {
            fail(message, a1, a2);
        }
    }

    public void assertEquals(Line2D a1, Line2D a2) {
        assertEquals(null, a1, a2);

    }

    public void assertEquals(String message, Arc2D a1, Arc2D a2, double delta) {
        if (!a1.getClass().equals(a2.getClass()) ||
            Math.abs(a1.getX() - a2.getX()) > delta ||
            Math.abs(a1.getY() - a2.getY()) > delta ||
            Math.abs(a1.getWidth() - a2.getWidth()) > delta ||
            Math.abs(a1.getHeight() - a2.getHeight()) > delta ||
            Math.abs(a1.getAngleStart() - a2.getAngleStart()) > delta ||
            Math.abs(a1.getAngleExtent() - a2.getAngleExtent()) > delta)
        {
            fail(message, arcToStr(a1), arcToStr(a2));
        }
    }

    public void assertEquals(String message, Arc2D a1, Arc2D a2) {
        assertEquals(message, a1, a2, 0.0);
    }

    public void assertEquals(Arc2D a1, Arc2D a2, double delta) {
        assertEquals(null, a1, a2, delta);
    }

    public void assertEquals(Arc2D a1, Arc2D a2) {
        assertEquals(null, a1, a2, 0.0);
    }

    public void assertEquals(String message, RoundRectangle2D a1, RoundRectangle2D a2) {
        if (!a1.getClass().equals(a2.getClass()) ||
            a1.getX() != a2.getX() ||
            a1.getY() != a2.getY() ||
            a1.getWidth() != a2.getWidth() ||
            a1.getHeight() != a2.getHeight() ||
            a1.getArcWidth() != a2.getArcWidth() ||
            a1.getArcHeight() != a2.getArcHeight())
        {
            fail(message, a1, a2);
        }
    }

    public void assertEquals(RoundRectangle2D a1, RoundRectangle2D a2) {
        assertEquals(null, a1, a2);
    }

    public void assertEquals(String message, Point2D a1, Point2D a2, double delta) {
        if (!a1.getClass().equals(a2.getClass()) ||
            Math.abs(a1.getX() - a2.getX()) > delta ||
            Math.abs(a1.getY() - a2.getY()) > delta)
        {
            fail(message, a1, a2);
        }
    }

    public void assertEquals(Point2D a1, Point2D a2, double delta) {
        assertEquals(null, a1, a2, delta);
    }

    public boolean equalRects(Rectangle2D a1, Rectangle2D a2, double delta) {
        return
            !(Math.abs(a1.getX() - a2.getX()) > delta ||
              Math.abs(a1.getY() - a2.getY()) > delta ||
              Math.abs(a1.getWidth() - a2.getWidth()) > delta ||
              Math.abs(a1.getHeight() - a2.getHeight()) > delta);
    }

    public void assertEquals(String message, Rectangle2D a1, Rectangle2D a2, double delta) {
        if (!a1.getClass().equals(a2.getClass()) || !equalRects(a1, a2, delta)) {
            fail(message, a1, a2);
        }
    }

    public void assertEquals(Rectangle2D a1, Rectangle2D a2, double delta) {
        assertEquals(null, a1, a2, delta);
    }

    public void assertEquals(String msg, double[] a1, double a2[], int length, double delta) {
        if (a1.length < length) {
            fail(concat(msg, "Wrong array length " + a1.length + ", needed at least " + length));
        }
        if (a2.length < length) {
            fail(concat(msg, "Wrong array length " + a2.length + ", needed at least " + length));
        }
        for(int i = 0; i < length; i++) {
            if (a1[i] != a2[i]) {
                fail(msg, Arrays.toString(a1), Arrays.toString(a2));
            }
        }
    }

    public void assertEquals(double[] a1, double a2[], int length, double delta) {
        assertEquals(null, a1, a2, length, delta);
    }

    public void assertEquals(String msg, float[] a1, float a2[], int length, float delta) {
        if (a1.length < length) {
            fail(concat(msg, "Wrong array length " + a1.length + ", needed at least " + length));
        }
        if (a2.length < length) {
            fail(concat(msg, "Wrong array length " + a2.length + ", needed at least " + length));
        }
        for(int i = 0; i < length; i++) {
            if (a1[i] != a2[i]) {
                fail(msg, Arrays.toString(a1), Arrays.toString(a2));
            }
        }
    }

    public void assertEquals(float[] a1, float a2[], int length, float delta) {
        assertEquals(null, a1, a2, length, delta);
    }

    public void assertEquals(String msg, int[] a1, int a2[], int length) {
        if (a1.length < length) {
            fail(concat(msg, "Wrong array length " + a1.length + ", needed at least " + length));
        }
        if (a2.length < length) {
            fail(concat(msg, "Wrong array length " + a2.length + ", needed at least " + length));
        }
        for(int i = 0; i < length; i++) {
            if (a1[i] != a2[i]) {
                fail(msg, Arrays.toString(a1), Arrays.toString(a2));
            }
        }
    }

    public void assertEquals(int[] a1, int a2[], int length) {
        assertEquals(null, a1, a2, length);
    }

/*
    void assertEquals(String message, GeneralPath a1, GeneralPath a2, double delta) {
        if (!a1.getClass().equals(a2.getClass()) ||
            a1.getWindingRule() != a2.getWindingRule() ||
            !Tools.PathIterator.equals(a1.getPathIterator(null), a2.getPathIterator(null), delta))
        {
            fail(message, a1, a2);
        }
    }

    void assertEquals(GeneralPath a1, GeneralPath a2, float delta) {
        assertEquals(null, a1, a2, delta);
    }

    void assertEquals(String message, PathIterator a1, PathIterator a2, double delta) {
        if (!a1.getClass().equals(a2.getClass()) ||
            a1.getWindingRule() != a2.getWindingRule() ||
            !Tools.PathIterator.equals(a1, a2, delta))
        {
            fail(message, a1, a2);
        }
    }

    void assertEquals(String message, GeneralPath a1, GeneralPath a2, float delta) {
        if (!a1.getClass().equals(a2.getClass()) ||
            a1.getWindingRule() != a2.getWindingRule() ||
            !Tools.PathIterator.equals(a1.getPathIterator(null), a2.getPathIterator(null), delta))
        {
            fail(message, a1, a2);
        }
    }

    void assertEquals(GeneralPath a1, GeneralPath a2, double delta) {
        assertEquals(null, a1, a2, delta);
    }
*/

    void assertEquals(GeneralPath a1, GeneralPath a2, double delta) {
        assertEquals(null, a1.getPathIterator(null), a2.getPathIterator(null), delta);
    }
    void assertEquals(GeneralPath a1, GeneralPath a2, float delta) {
        assertEquals(null, a1.getPathIterator(null), a2.getPathIterator(null), delta);
    }

    void assertEquals(String message, PathIterator a1, PathIterator a2, double delta) {
        if (!a1.getClass().equals(a2.getClass()) || !Tools.PathIterator.equals(a1, a2, delta)) {
            fail(message, a1, a2);
        }
    }

    void assertEquals(String message, PathIterator a1, PathIterator a2, float delta) {
        if (!a1.getClass().equals(a2.getClass()) || !Tools.PathIterator.equals(a1, a2, delta)) {
            fail(message, a1, a2);
        }
    }

    void assertEquals(PathIterator a1, PathIterator a2, double delta) {
        assertEquals(null, a1, a2, delta);
    }

    void assertEquals(PathIterator a1, PathIterator a2, float delta) {
        assertEquals(null, a1, a2, delta);
    }

    void assertEquals(String message, QuadCurve2D a1, QuadCurve2D a2, double delta) {
        if (!a1.getClass().equals(a2.getClass()) ||
            Math.abs(a1.getX1() - a2.getX1()) > delta ||
            Math.abs(a1.getY1() - a2.getY1()) > delta ||
            Math.abs(a1.getX2() - a2.getX2()) > delta ||
            Math.abs(a1.getY2() - a2.getY2()) > delta ||
            Math.abs(a1.getCtrlX() - a2.getCtrlX()) > delta ||
            Math.abs(a1.getCtrlY() - a2.getCtrlY()) > delta)
        {
            fail(message, quadToStr(a1), quadToStr(a2));
        }
    }

    void assertEquals(QuadCurve2D a1, QuadCurve2D a2, double delta) {
        assertEquals(null, a1, a2, delta);
    }

    void assertEquals(String message, CubicCurve2D a1, CubicCurve2D a2, double delta) {
        if (!a1.getClass().equals(a2.getClass()) ||
            Math.abs(a1.getX1() - a2.getX1()) > delta ||
            Math.abs(a1.getY1() - a2.getY1()) > delta ||
            Math.abs(a1.getX2() - a2.getX2()) > delta ||
            Math.abs(a1.getY2() - a2.getY2()) > delta ||
            Math.abs(a1.getCtrlX1() - a2.getCtrlX1()) > delta ||
            Math.abs(a1.getCtrlY1() - a2.getCtrlY1()) > delta ||
            Math.abs(a1.getCtrlX2() - a2.getCtrlX2()) > delta ||
            Math.abs(a1.getCtrlY2() - a2.getCtrlY2()) > delta)
        {
            fail(message, cubicToStr(a1), cubicToStr(a2));
        }
    }

    void assertEquals(CubicCurve2D a1, CubicCurve2D a2, double delta) {
        assertEquals(null, a1, a2, delta);
    }

    String doubleToStr(double a) {
        if (a == (int)a) {
            return "" + (int)a;
        }
        return "" + a;
    }

    String arcToStr(Arc2D a) {
        return a.getClass().getName() +
            "[x=" + a.getX() +
            ",y=" + a.getY() +
            ",width=" + a.getWidth() +
            ",height=" + a.getHeight() +
            ",start=" + a.getAngleStart() +
            ",extent=" + a.getAngleExtent() + "]";
    }

    String quadToStr(QuadCurve2D a) {
        return a.getClass().getName() +
            "[x1=" + a.getX1() +
            ",y1=" + a.getY1() +
            ",x2=" + a.getCtrlX() +
            ",y2=" + a.getCtrlY() +
            ",x3=" + a.getX2() +
            ",y3=" + a.getY2() + "]";
    }

    String cubicToStr(CubicCurve2D a) {
        return a.getClass().getName() +
            "[x1=" + a.getX1() +
            ",y1=" + a.getY1() +
            ",x2=" + a.getCtrlX1() +
            ",y2=" + a.getCtrlY1() +
            ",x3=" + a.getCtrlX2() +
            ",y3=" + a.getCtrlY2() +
            ",x4=" + a.getX2() +
            ",y4=" + a.getY2() + "]";
    }

}
