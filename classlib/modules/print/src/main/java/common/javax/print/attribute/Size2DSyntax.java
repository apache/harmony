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

package javax.print.attribute;

import java.io.Serializable;

public abstract class Size2DSyntax implements Cloneable, Serializable {
    private static final long serialVersionUID = 5584439964938660530L;
    
    public static final int INCH = 25400;

    public static final int MM = 1000;

    private final int x;

    private final int y;

    protected Size2DSyntax(int x, int y, int units) {
        if ((x < 0) || (y < 0) || (units < 1)) {
            throw new IllegalArgumentException("Valid values are:" + "x>=0, y>=0, units>=1");
        }
        this.x = x * units;
        this.y = y * units;
    }

    protected Size2DSyntax(float x, float y, int units) {
        if ((x < 0.0f) || (y < 0.0f) || (units < 1)) {
            throw new IllegalArgumentException("Valid values are:" + "x>=0.0, y>=0.0, units>=1");
        }
        this.x = Math.round(x * units);
        this.y = Math.round(y * units);
    }

    @Override
    public boolean equals(Object object) {
        if ((object instanceof Size2DSyntax) && (x == ((Size2DSyntax) object).x)
                && (y == ((Size2DSyntax) object).y)) {
            return true;
        }
        return false;
    }

    public float[] getSize(int units) {
        return new float[] { getX(units), getY(units) };
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

    protected int getXMicrometers() {
        return x;
    }

    protected int getYMicrometers() {
        return y;
    }

    @Override
    public int hashCode() {
        return (y | (x << 16));
    }

    @Override
    public String toString() {
        return (x + "x" + y + " um");
    }

    public String toString(int units, String unitsName) {
        if (unitsName == null) {
            unitsName = "";
        }
        return (getX(units) + "x" + getX(units) + " " + unitsName);
    }
}
