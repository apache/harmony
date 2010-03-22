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

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.geom.Point2D;

import junit.framework.TestCase;

public class GradientPaintTest extends TestCase {

    GradientPaint gp;

    public void testConstructorBad() {
        // Regression for HARMONY-1470
        try {
            new GradientPaint(1, 2, null, 3, 4, new Color(255), false);
            fail("expected NPE");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            new GradientPaint(1, 2, new Color(255), 3, 4, null, false);
            fail("expected NPE");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            new GradientPaint(1, 2, null, 4, 5, new Color(255));
            fail("expected NPE");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            new GradientPaint(1, 2, new Color(255), 4, 5, null);
            fail("expected NPE");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            new GradientPaint(null, new Color(255), new Point2D.Float(),
                    new Color(255));
            fail("expected NPE");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            new GradientPaint(new Point2D.Float(), null, new Point2D.Float(),
                    new Color(255));
            fail("expected NPE");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            new GradientPaint(new Point2D.Float(), new Color(255), null,
                    new Color(255));
            fail("expected NPE");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            new GradientPaint(new Point2D.Float(), new Color(255),
                    new Point2D.Float(), null);
            fail("expected NPE");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testGetPoint1() {
        gp = new GradientPaint(1, 2, Color.green, 3, 4, Color.blue, true);
        assertEquals(new Point2D.Float(1, 2), gp.getPoint1());
    }

    public void testGetPoint2() {
        gp = new GradientPaint(1, 2, Color.green, 3, 4, Color.blue, true);
        assertEquals(new Point2D.Float(3, 4), gp.getPoint2());
    }

    public void testGetColor1() {
        gp = new GradientPaint(1, 2, Color.green, 3, 4, Color.blue, true);
        assertEquals(Color.green, gp.getColor1());
    }

    public void testGetColor2() {
        gp = new GradientPaint(1, 2, Color.green, 3, 4, Color.blue, true);
        assertEquals(Color.blue, gp.getColor2());
    }

    public void testGetCyclic() {
        gp = new GradientPaint(1, 2, Color.green, 3, 4, Color.blue, true);
        assertTrue(gp.isCyclic());
        gp = new GradientPaint(1, 2, Color.green, 3, 4, Color.blue, false);
        assertFalse(gp.isCyclic());
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GradientPaintTest.class);
    }

}
