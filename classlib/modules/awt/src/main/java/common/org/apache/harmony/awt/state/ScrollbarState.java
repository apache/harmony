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
 * @author Dmitry A. Durnev, Pavel Dolgov
 */
package org.apache.harmony.awt.state;

import java.awt.Adjustable;
import java.awt.ComponentOrientation;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * ScrollbarState
 */
public interface ScrollbarState extends State {
    static final int DECREASE_HIGHLIGHT = 1;
    static final int INCREASE_HIGHLIGHT = 2;
    static final int NO_HIGHLIGHT = 0;

    Adjustable getAdjustable();
    int getHighlight();
    ComponentOrientation getComponentOrientation();
    Point getLocation(); //location of scrollbar inside component

    boolean isDecreasePressed(); // left/top arrow
    boolean isIncreasePressed(); // right/bottom arrow

    boolean isSliderPressed();
    int getSliderPosition();
    int getSliderSize();
    int getScrollSize();

    boolean isVertical();

    // getters/setters for coordinates in pixels:
    Rectangle getSliderRect();
    Rectangle getIncreaseRect();
    Rectangle getDecreaseRect();
    Rectangle getTrackBounds();
    Rectangle getUpperTrackBounds();
    Rectangle getLowerTrackBounds();
    int getTrackSize();
    void setSliderRect(Rectangle r);
    void setIncreaseRect(Rectangle r);
    void setDecreaseRect(Rectangle r);
    void setTrackBounds(Rectangle r);
    void setUpperTrackBounds(Rectangle r);
    void setLowerTrackBounds(Rectangle r);
    void setTrackSize(int size);

    // set typed value(necessary for scrollpane)
    void setValue(int type, int value);

}
