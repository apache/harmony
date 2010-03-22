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
 * @author Sergey Burlak
 */

package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;

public class ViewportLayout implements LayoutManager, Serializable {
    private final UpdateStrategy HORIZONTAL_STRATEGY = new UpdateStrategy() {
        public void updatePosition(final Point position, final int newPos) {
            position.x = newPos;
        }

        public void updateSize(final Dimension size, final int newSize) {
            size.width = newSize;
        }

        public int getViewPosition(final Point viewPosition) {
            return viewPosition.x;
        }

        public int getScrollableLength(final Scrollable view, final int currentViewLength, final int currentViewportLength) {
            if (view.getScrollableTracksViewportWidth() && currentViewportLength > 0) {
                return currentViewportLength;
            } else {
                return currentViewLength;
            }
        }
    };

    private final UpdateStrategy VERTICAL_STRATEGY = new UpdateStrategy() {
        public void updatePosition(final Point position, final int newPos) {
            position.y = newPos;
        }

        public void updateSize(final Dimension size, final int newSize) {
            size.height = newSize;
        }

        public int getViewPosition(final Point viewPosition) {
            return viewPosition.y;
        }

        public int getScrollableLength(final Scrollable view, final int currentViewLength, final int currentViewportLength) {
            if (view.getScrollableTracksViewportHeight() && currentViewportLength > 0) {
                return currentViewportLength;
            } else {
                return currentViewLength;
            }
        }
    };

    public Dimension preferredLayoutSize(final Container c) {
        Component view = ((JViewport)c).getView();
        if (view == null) {
            return new Dimension(0, 0);
        }

        if (view instanceof Scrollable) {
            Scrollable scrView = (Scrollable)view;

            return scrView.getPreferredScrollableViewportSize();
        }

        return new Dimension(view.getPreferredSize());
    }

    public Dimension minimumLayoutSize(final Container c) {
        return new Dimension(4, 4);
    }

    public void addLayoutComponent(final String s, final Component c) {
    }

    public void removeLayoutComponent(final Component c) {
    }

    public void layoutContainer(final Container c) {
        JViewport viewport = (JViewport)c;
        Component view = viewport.getView();
        if (view == null) {
            return;
        }

        Rectangle bounds = viewport.getBounds();
        Dimension viewPrfSize = view.getPreferredSize();

        Point viewPos = viewport.getViewPosition();
        Dimension viewSize = new Dimension();

        HORIZONTAL_STRATEGY.update(view, viewPos, viewSize, bounds.width, viewPrfSize.width);
        VERTICAL_STRATEGY.update(view, viewPos, viewSize, bounds.height, viewPrfSize.height);
        viewport.getView().setSize(viewSize);
        viewport.scrollUnderway = true;
        viewport.setViewPosition(viewPos);
        viewport.scrollUnderway = false;
    }

    private abstract class UpdateStrategy {
        public void update(final Component view, final Point resultPosition, final Dimension resultSize, final int currentViewportLength, final int currentViewLength) {
            int newViewLength;
            if (view instanceof Scrollable) {
                newViewLength = getScrollableLength((Scrollable)view, currentViewLength, currentViewportLength);
            } else {
                newViewLength = Math.max(currentViewportLength, currentViewLength);
            }
            updateSize(resultSize, newViewLength);

            if (currentViewportLength >= currentViewLength) {
                updatePosition(resultPosition, 0);
            } else {
                int currentViewPos = getViewPosition(resultPosition);
                int diff = -currentViewPos + newViewLength - currentViewportLength;
                if (diff < 0) {
                    updatePosition(resultPosition, currentViewPos + diff);
                }
            }
        }

        public abstract void updatePosition(Point position, int newPos);
        public abstract void updateSize(Dimension size, int newSize);
        public abstract int getViewPosition(Point viewPosition);
        public abstract int getScrollableLength(Scrollable view, int currentViewLength, int currentViewportLength);
    }
}

