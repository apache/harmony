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
 * @author Anton Avtamonov
 */
package org.apache.harmony.x.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.JComponent;
import javax.swing.RepaintManager;

import org.apache.harmony.awt.ComponentInternals;
import org.apache.harmony.awt.gl.MultiRectArea;

import org.apache.harmony.x.swing.internal.nls.Messages;


/**
 * Blit engine which allows to 'repaint' moving components using Graphics.copyArea() (blitting mode).
 * To use this class a component and its parent should be JComponent.
 * Parent can be dynamically changed which causes blitting reset.
 */
public class BlitSupport {
    private JComponent blitingComponent;
    private JComponent parent;
    private boolean wasPainted;
    private Point lastPaintPosition;
    private Rectangle lastPaintedRect;

    /**
     * Constructs BlitSupport with no component set.
     * In this case component must be set before first call to {@link #paint()} method.
     */
    public BlitSupport() {
    }

    /**
     * Constructs BlitSupport for the specified component.
     * @param c JComponent which drag-related repaints should be optimized
     */
    public BlitSupport(final JComponent c) {
        setBlitComponent(c);
    }

    /**
     * Sets blitting component.
     * @param c JComponent which drag-related repaints shoudl be optimized
     */
    public void setBlitComponent(final JComponent c) {
        blitingComponent = c;
        parent = null;
        resetBlitting();
    }

    /**
     * Should be called from the place where component position was changed and optimized repainting is required.
     * Note that if optimized repainting was not called or failed (returned false) the 'normal' repainting will occur automatically.
     * Method must be called from the event-dispatching thread.
     *
     * @return <code>true</code> if optimized repainting was successful, <code>false</code> otherwise
     */
    public boolean paint() {
        if (!EventQueue.isDispatchThread()) {
            return false;
        }

        if (!initialize()) {
            return false;
        }

        if (lastPaintedRect == null || lastPaintedRect.isEmpty()) {
            return false;
        }

        Rectangle parentBounds = parent.getVisibleRect();

        Graphics g = parent.getGraphics();
        if (g == null) {
            resetBlitting();
            return false;
        }

        Point currentLocation = blitingComponent.getLocation();
        Rectangle visibleBounds = getVisibleBounds();

        int dx = currentLocation.x - lastPaintPosition.x;
        int dy = currentLocation.y - lastPaintPosition.y;

        Rectangle copyRect = new Rectangle(lastPaintedRect);
        copyRect.translate(dx, dy);
        if (!parentBounds.intersects(copyRect)) {
            resetBlitting();
            return false;
        }

        Rectangle adjustedCopyRect = copyRect.intersection(parentBounds);
        if (isObscured(adjustedCopyRect.x - dx, adjustedCopyRect.y - dy, adjustedCopyRect.width, adjustedCopyRect.height)) {
            return false;
        }

        RepaintManager.currentManager(blitingComponent).markCompletelyClean(blitingComponent);
        RepaintManager.currentManager(blitingComponent).markCompletelyClean(parent);

        g.copyArea(adjustedCopyRect.x - dx, adjustedCopyRect.y - dy, adjustedCopyRect.width, adjustedCopyRect.height, dx, dy);

        wasPainted = false;
        MultiRectArea affectedArea = new MultiRectArea(visibleBounds);
        affectedArea.add(lastPaintedRect);
        affectedArea.substract(adjustedCopyRect);
        if (!affectedArea.isEmpty()) {
            Shape oldClip = g.getClip();

            g.setClip(affectedArea);
            parent.paint(g);

            g.setClip(oldClip);
        }
        if (!wasPainted) {
            onPaint();
        }
        g.dispose();

        return true;
    }

    /**
     * Must be called from Component.paint() or related method to synchromize
     * 'normal' and optimized paints. Method must be called from
     * the event-dispatching thread.
     *
     */
    public void onPaint() {
        initialize();

        if (!EventQueue.isDispatchThread()) {
            return;
        }

        lastPaintPosition = blitingComponent.getLocation();
        lastPaintedRect = getVisibleBounds();
        wasPainted = true;
    }


    private Rectangle getVisibleBounds() {
        Rectangle result = blitingComponent.getVisibleRect();
        result.translate(blitingComponent.getX(), blitingComponent.getY());

        return result;
    }

    private boolean initialize() {
        if (blitingComponent == null) {
            throw new IllegalStateException(Messages.getString("swing.71")); //$NON-NLS-1$
        }

        if (parent == null) {
            Container blitParent = blitingComponent.getParent();
            if (!(blitParent instanceof JComponent)) {
                return false;
            }

            parent = (JComponent)blitParent;
        }

        return true;
    }

    private boolean isObscured(final int x, final int y, final int width, final int height) {
        ComponentInternals ci = ComponentInternals.getComponentInternals();
        MultiRectArea obscuredArea = ci.getObscuredRegion(parent);
        ci.addObscuredRegions(obscuredArea, blitingComponent, parent);
        if (obscuredArea == null || obscuredArea.isEmpty()) {
            return false;
        }
        return obscuredArea.intersects(x, y, width, height);
    }

    private void resetBlitting() {
        wasPainted = false;
        lastPaintPosition = null;
        lastPaintedRect = null;
    }
}
