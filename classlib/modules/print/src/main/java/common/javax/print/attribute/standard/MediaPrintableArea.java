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

package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

public final class MediaPrintableArea implements DocAttribute, PrintJobAttribute,
        PrintRequestAttribute {
    private static final long serialVersionUID = -1597171464050795793L;

    public static final int INCH = 25400;

    public static final int MM = 1000;

    private final int x;

    private final int y;

    private final int width;

    private final int height;

    public MediaPrintableArea(int x, int y, int width, int height, int units) {
        if ((x < 0) || (y < 0) || (width <= 0) || (height <= 0) || (units < 1)) {
            throw new IllegalArgumentException("Valid values are: x >= 0, "
                    + "y >= 0, width > 0, height > 0, units >= 1");
        }
        this.x = x * units;
        this.y = y * units;
        this.width = width * units;
        this.height = height * units;
    }

    public MediaPrintableArea(float x, float y, float width, float height, int units) {
        if ((x < 0.0f) || (y < 0.0f) || (width <= 0.0f) || (height <= 0.0f) || (units < 1)) {
            throw new IllegalArgumentException("Valid values are: x >= 0.0, "
                    + "y >= 0.0, width > 0.0, height > 0.0, units > 1");
        }
        this.x = Math.round(x * units);
        this.y = Math.round(y * units);
        this.width = Math.round(width * units);
        this.height = Math.round(height * units);
    }

    @Override
    public boolean equals(Object object) {
        boolean outcome = false;
        if (object instanceof MediaPrintableArea) {
            MediaPrintableArea mpArea = (MediaPrintableArea) object;
            if ((x == mpArea.x) && (y == mpArea.y) && (width == mpArea.width)
                    && (height == mpArea.height)) {
                outcome = true;
            }
        }
        return outcome;
    }

    public final Class<? extends Attribute> getCategory() {
        return MediaPrintableArea.class;
    }

    public final String getName() {
        return "media-printable-area";
    }

    public float getX(int units) {
        if (units < 1) {
            throw new IllegalArgumentException("units is less than 1");
        }
        return ((float) x) / units;
    }

    public float getY(int units) {
        if (units < 1) {
            throw new IllegalArgumentException("units is less than 1");
        }
        return ((float) y) / units;
    }

    public float getWidth(int units) {
        if (units < 1) {
            throw new IllegalArgumentException("units is less than 1");
        }
        return ((float) width) / units;
    }

    public float getHeight(int units) {
        if (units < 1) {
            throw new IllegalArgumentException("units is less than 1");
        }
        return ((float) height) / units;
    }

    public float[] getPrintableArea(int units) {
        return new float[] { getX(units), getY(units), getWidth(units), getHeight(units) };
    }

    @Override
    public int hashCode() {
        return ((y + height) | ((x + width) << 16));
    }

    @Override
    public String toString() {
        return toString(MM, "mm");
    }

    public String toString(int units, String unitsName) {
        if (unitsName == null) {
            unitsName = "";
        }
        float[] pa = getPrintableArea(units);
        return ("x=" + pa[0] + " y=" + pa[1] + " width=" + pa[2] + " height=" + pa[3] + " " + unitsName);
    }
}
