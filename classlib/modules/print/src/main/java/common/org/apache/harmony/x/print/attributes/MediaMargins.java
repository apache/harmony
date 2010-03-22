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

/*
 * MediaMargins.java
 * 
 * We launch additional printing attribute:
 *     org.apache.harmony.x.print.attributes.MediaMargins. 
 * 
 * This is a printing attribute used to set paper margins for the printed 
 * document. The MediaMargins is specified to be left, right, top and bottom 
 * paper margins. It is independent of a particular media. Sometimes it is 
 * more convenient for a client to use margins instead of printable area.
 * 
 * This attribute can be easy calculated if we have MediaSize and 
 * MediaPrintableArea attributes for the printed document, so in this case we 
 * do not need to set it clearly. However, it can be very helpful if we do not
 * know size of the printed document's Media size (for example, if Media 
 * attribute will be set later or it is MediaTray attribute) - we can not use 
 * MediaPrintableArea in this case. 
 * 
 * It is supposed that our default printers will support MediaMargins  
 * attribute. 
 * 
 * In case of the conflict between MediaPrintableArea and MediaMargins 
 * attribute the following way of conflict resolving is supposed:
 * 
 * - if MediaSize, MediaPrintableArea and MediaMargins attributes are present,
 *   MediaPrintableArea should be used to calculate paper margins;
 * 
 * - if Media attribute is not defined or it is defined, but we are unable to 
 *   get paper size using it, MediaMargins attribute should be used and 
 *   MediaPrintableArea should be ignored.
 *
 * Like MediaPrintableArea attribute, MediaMargins values are storet internally
 * as integers in units of micrometers (please, see 
 * javax.print.attribute.standard.MediaPrintableArea class API Specification
 * for more details).
 */

package org.apache.harmony.x.print.attributes;

import javax.print.attribute.DocAttribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;

public final class MediaMargins 
        implements DocAttribute,
                   PrintJobAttribute,
                   PrintRequestAttribute
{

    private static final long serialVersionUID = -7745492737636484477L;
        
    public static final int INCH = Size2DSyntax.INCH;   // 25 400
    public static final int MM = Size2DSyntax.MM;       //  1 000
    private int x1;     // left margin
    private int x2;     // right margin
    private int y1;     // top margin
    private int y2;     // bottom margin
    private int units;  // original units
    
    /*
     * Constructs a MediaMargins object from integer values.
     * 
     * Parameters:
     *      x1 - left margin
     *      y1 - top margin
     *      x2 - right margin
     *      y2 - bottom margin
     *      units - units in which the values are expressed
     * 
     * Throws IllegalArgumentException if margins value are < 0
     * or units < 1 
     */
    public MediaMargins(int xx1, int yy1, int xx2, int yy2, int myunits) {
        if ((xx1 < 0) || (yy1 < 0) || (xx2 < 0) || (yy2 < 0) || (myunits < 1)) {
            throw new IllegalArgumentException("Incorrect margins!");
        }
        x1 = xx1 * myunits;
        y1 = yy1 * myunits;
        x2 = xx2 * myunits;
        y2 = yy2 * myunits;
        units = myunits;
    }
    
    /*
     * Constructs a MediaMargins object from float values.
     * 
     * Parameters:
     *      x1 - left margin
     *      y1 - top margin
     *      x2 - right margin
     *      y2 - bottom margin
     *      units - units in which the values are expressed
     * 
     * Throws IllegalArgumentException if margins value are < 0
     * or units < 1 
     */
    public MediaMargins(float xx1, float yy1, float xx2, float yy2, int myunits) {
        if ((xx1 < 0) || (yy1 < 0) || (xx2 < 0) || (yy2 < 0) || (myunits < 1)) {
            throw new IllegalArgumentException("Incorrect margins!");
        }
        x1 = Math.round(xx1 * myunits);
        y1 = Math.round(yy1 * myunits);
        x2 = Math.round(xx2 * myunits);
        y2 = Math.round(yy2 * myunits);
        units = myunits;
    }
    
    /*
     * Constructs a MediaMargins object from MediaSize and MediaPrintableArea.
     * 
     * Parameters:
     *      size - MediaSize attribute
     *      area - MediaPrintableArea attribute
     * 
     * Throws IllegalArgumentException if size is null or area is null or 
     * if given MediaPrintableArea is too big for the given MediaSize.
     */
    
    public MediaMargins(MediaSize size, MediaPrintableArea area) {
        if ((size == null) || (area == null)) {
            throw new IllegalArgumentException("Incorrect margins!");
        }
        this.x1 = Math.round(area.getX(MM) * MM);
        this.x2 = Math.round((size.getX(MM) - area.getX(MM) - area.getWidth(MM)) 
                * MM);
        this.y1 = Math.round(area.getY(MM) * MM);
        this.y2 = Math.round((size.getY(MM) - area.getY(MM) - area.getHeight(MM))
                * MM);
        if ((x1 < 0) || (y1 < 0) || (x2 < 0) || (y2 < 0)) {
            throw new IllegalArgumentException("Incorrect margins!");
        }
    }
    
    /*
     * Returns whether this margins attribute is equal to the passed object.
     * The following conditions must be true:
     *      - objectis not null;
     *      - object is an instance of MediaMargins class
     *      - The margins values are the same
     */
    public boolean equals(Object object) {
        if (object instanceof MediaMargins) {
            MediaMargins m = (MediaMargins) object;
            return (x1 == m.x1) && (y1 == m.y1) && (x2 == m.x2) && (y2 == m.y2);
        } 
        return false;
    }
    
    /*
     * Returns the printing category attribute class 
     * (an instance of java.lang.Class class).
     * For the MediaMargins the category class is MediaMargins class itself.
     */
    public final Class getCategory() {
        return MediaMargins.class;
    }
    
    /*
     * Returns the attribute category name - "media-margins"
     */
    public final String getName() {
        return "media-margins";
    }
    
    /*
     * Returns the left margin value in the specified units.
     * 
     * Throws IllegalArgumentException if units < 1.
     */
    public float getX1(int myunits) {
        if (myunits < 1) {
            throw new IllegalArgumentException("units is less than 1");
        }
        return ((float) x1) / myunits;
    }
    
    /*
     * Returns the top margin value in the specified units.
     * 
     * Throws IllegalArgumentException if units < 1.
     */
    public float getY1(int myunits) {
        if (myunits < 1) {
            throw new IllegalArgumentException("units is less than 1");
        }
        return ((float) y1) / myunits;
    }
    
    /*
     * Returns the right margin value in the specified units.
     * 
     * Throws IllegalArgumentException if units < 1.
     */
    public float getX2(int myunits) {
        if (myunits < 1) {
            throw new IllegalArgumentException("units is less than 1");
        }
        return ((float) x2) / myunits;
    }
    
    /*
     * Returns the bottom margin value in the specified units.
     * 
     * Throws IllegalArgumentException if units < 1.
     */
    public float getY2(int myunits) {
        if (myunits < 1) {
            throw new IllegalArgumentException("units is less than 1");
        }
        return ((float) y2) / myunits;
    }
    
    /*
     * Returns the margins value as an array of 4 float values in the order 
     * left, top, right, bottom in given units.
     * 
     * Throws IllegalArgumentException if units < 1.
     */
    public float[] getMargins(int myunits) {
        return new float [] { getX1(myunits), 
                              getY1(myunits),
                              getX2(myunits),
                              getY2(myunits) };
    }
    
    /*
     * Returns a hash code value for the attribute
     */
    public int hashCode() {
        return ((x1 + x2) | ((y1 + y2) << 16));
    }
    
    /*
     * Returns a string version of this margins attribute in mm
     */
    public String toString() {
        return toString(MM, "mm");
    }
    
    /*
     * Returns a string version of this margins attribute in the given units.
     * 
     * Parameters:
     *      units - units conversion factor (for example, INCH or MM)
     *      unitsName - units name string. If null, no units name is appended to
     *                  the result string.
     * 
     * Throws IllegalArgumentException if units < 1.
     */
    public String toString(int myunits, String unitsName) {
        StringBuilder s = new StringBuilder();
        s.append("x1=");
        s.append(getX1(myunits));
        s.append(" y1=");
        s.append(getY1(myunits));
        s.append(" x2=");
        s.append(getX2(myunits));
        s.append(" y2=");
        s.append(getY2(myunits));
        s.append(" "); 
        s.append((unitsName == null) ? "" : unitsName);    
        return s.toString();
    }
    
    /*
     * Calculate MediaPrintableArea attribute for tis margins and the given 
     * MediaSize object.
     */
    public MediaPrintableArea getMediaPrintableArea(MediaSize size) {
        return new MediaPrintableArea(getX1(MM), 
                                      getY1(MM),
                                      size.getX(MM) - getX1(MM) - getX2(MM),
                                      size.getY(MM) - getY1(MM) - getY2(MM),
                                      MM);
    }
    
} /* End of MediaMargins */
