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

package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

import org.apache.harmony.awt.internal.nls.Messages;

public class GradientPaint implements Paint {
    /**
     * The start point color
     */
    Color color1;

    /**
     * The end color point
     */
    Color color2;

    /**
     * The location of the start point
     */
    Point2D point1;

    /**
     * The location of the end point
     */
    Point2D point2;

    /**
     * The indicator of cycle filling. If TRUE filling repeated outside points
     * stripe, if FALSE solid color filling outside.
     */
    boolean cyclic;

    public GradientPaint(Point2D point1, Color color1, Point2D point2,
            Color color2, boolean cyclic) {
        if (point1 == null || point2 == null) {
            // awt.6D=Point is null
            throw new NullPointerException(Messages.getString("awt.6D")); //$NON-NLS-1$
        }
        if (color1 == null || color2 == null) {
            // awt.6E=Color is null
            throw new NullPointerException(Messages.getString("awt.6E")); //$NON-NLS-1$
        }

        this.point1 = point1;
        this.point2 = point2;
        this.color1 = color1;
        this.color2 = color2;
        this.cyclic = cyclic;
    }

    public GradientPaint(float x1, float y1, Color color1, float x2, float y2, Color color2,
            boolean cyclic) {
        this(new Point2D.Float(x1, y1), color1, new Point2D.Float(x2, y2), color2, cyclic);
    }

    public GradientPaint(float x1, float y1, Color color1, float x2, float y2, Color color2) {
        this(x1, y1, color1, x2, y2, color2, false);
    }

    public GradientPaint(Point2D point1, Color color1, Point2D point2, Color color2) {
        this(point1, color1, point2, color2, false);
    }

    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds,
            Rectangle2D userBounds, AffineTransform t, RenderingHints hints) {
        return new GradientPaintContext(cm, t, point1, color1, point2, color2, cyclic);
    }

    public Color getColor1() {
        return color1;
    }

    public Color getColor2() {
        return color2;
    }

    public Point2D getPoint1() {
        return point1;
    }

    public Point2D getPoint2() {
        return point2;
    }

    public int getTransparency() {
        int a1 = color1.getAlpha();
        int a2 = color2.getAlpha();
        return (a1 == 0xFF && a2 == 0xFF) ? OPAQUE : TRANSLUCENT;
    }

    public boolean isCyclic() {
        return cyclic;
    }
}
