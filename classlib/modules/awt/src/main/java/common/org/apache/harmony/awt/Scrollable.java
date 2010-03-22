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
 * @author Dmitry A. Durnev
 */
package org.apache.harmony.awt;

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * An internal AWT interface similar to
 * javax.swing.Scrollable. Should be implemented
 * by a Container with a single child component
 * and scrolling behavior, such as
 * ScrollPane.
 */
public interface Scrollable {

    /**
     * Constants for possible scrollbar
     * display policies, define which scrollbars
     * should be displayed and when they should
     * be displayed
     */
    public static final int AS_NEEDED = 0;

    public static final int ALWAYS = 1;

    public static final int NEVER = 2;

    public static final int HORIZONTAL_ONLY = 3;

    public static final int VERTICAL_ONLY = 4;

    /**     
     * @return Adjustable interface
     * of vertical scrollbar
     */
    Adjustable getVAdjustable();
    
    /**     
     * @return Adjustable interface
     * of horizontal scrollbar
     */
    Adjustable getHAdjustable();

    /**
     * Gets container insets NOT including
     * scrollbars(adjustables) area
     */
    Insets getInsets();

    /**
     * Gets scroll location
     * @return current scroll location, i. e.
     * (0, 0) point of container viewport
     * in child component coordinates
     */
    Point getLocation();

    /**
     * Scrolls component to the specified location
     * within child component
     * @param p new scroll location in
     * Container coordinates
     */
    void setLocation(Point p);

    /**
     * Gets the current child component to scroll.
     * @return component location of which should be changed
     * while scrolling
     */
    Component getComponent();

    /**
     * Gets the current scroll size
     * @return size of the child component being scrolled inside
     * container, which typically exceeds size of the
     * container itself
     */
    Dimension getSize();

    /**
     * Repaints all the viewport(client area) of the Container
     */
    void doRepaint();
    
    /**
     * Repaints the specified rectangle of the container
     */
    void doRepaint(Rectangle r);

    /**
     * Gets vertical scrollbar width
     * @return width of the vertical adjustable
     */
    int getAdjustableWidth();

    /**
     * Gets horizontal scrollbar height
     * @return height of the horizontal adjustable
     */
    int getAdjustableHeight();

    /**
     * Internal AWT method for changing adjustable minimum,
     * maximum and visible amount
     * @param adj Adjustable to be changed
     * @param vis new visible amount
     * @param min new minimum value
     * @param max new maximum value
     */
    void setAdjustableSizes(Adjustable adj, int vis, int min, int max);

    /**
     * Gets scrollbar display policy for adjustable 
     * @param adj scrollbar
     * @return one of 5 constants
     * identifying when the specified scrollbar is displayed
     */
    int getAdjustableMode(Adjustable adj);

    /**
     * Sets scrollbar bounds relative to container origin
     * @param adj scrollbar being changed
     * @param r new bounds
     */
    void setAdjustableBounds(Adjustable adj, Rectangle r);

    /**
     * @return width of the container
     */
    int getWidth();

    /**
     * @return height of the container
     */
    int getHeight();
}
