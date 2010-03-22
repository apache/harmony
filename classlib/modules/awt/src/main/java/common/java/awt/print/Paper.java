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
 * @author Igor A. Pyankov
 */
package java.awt.print;

import java.awt.geom.Rectangle2D;

public class Paper implements Cloneable {

    private double paperHeight;
    private double paperWidth;
    private final Rectangle2D.Double paperImageableArea;

    private static final double INCH = 72D;    // inch in pixels
    private static final double INCH2 = 144D;  // double inch
    private static final double LETTER_WIDTH = 612D; // default width in pixels
    private static final double LETTER_HEIGHT = 792D;// default height in pixels

    public Paper() {
        super();
        paperWidth = LETTER_WIDTH;
        paperHeight = LETTER_HEIGHT;
        paperImageableArea = new Rectangle2D.Double(INCH, INCH,
                                    paperWidth - INCH2, paperHeight - INCH2);
    }

    @Override
    public Object clone(){
        Paper clonedPaper;
        try {
            clonedPaper = (Paper)super.clone();
        } catch(CloneNotSupportedException cnse){
            cnse.printStackTrace();
            clonedPaper = null;
        }
        return clonedPaper;
    }

    public double getHeight(){
        return paperHeight;
    }

    public double getImageableWidth(){
        return paperImageableArea.getWidth();
    }

    public double getImageableHeight(){
        return paperImageableArea.getHeight();
    }

    public double getImageableX(){
        return paperImageableArea.getX();
    }

    public double getImageableY(){
        return paperImageableArea.getY();
    }

    public double getWidth(){
        return paperWidth;
    }

    public void setImageableArea(double x,
                                 double y,
                                 double width,
                                 double height){
        paperImageableArea.setRect(x, y, width, height);
    }

    public void setSize(double width,  double height){
        paperWidth = width;
        paperHeight = height;
    }
}
