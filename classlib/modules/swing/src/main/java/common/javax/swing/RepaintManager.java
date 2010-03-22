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
package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.harmony.awt.ClipRegion;
import org.apache.harmony.awt.ComponentInternals;
import org.apache.harmony.awt.gl.MultiRectArea;

public class RepaintManager {
    private Set invalidRoots = Collections.synchronizedSet(new HashSet());
    private Image offscreenImage;
    private VolatileImage volatileOffscreenImage;

    private Dimension maximumSize;
    private boolean doubleBufferingEnabled = true;
    private Map dirtyRegions = new Hashtable();
    private Map optimizedDirtyRegions = new HashMap();
    private int numberOfScheduledPaintEvents;

    private static final Rectangle COMPLETELY_DIRTY_RECT = new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);

    private static RepaintManager instance;
    
    public RepaintManager() {
        maximumSize = GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance() ? 
                new Dimension(0,0)
                : Toolkit.getDefaultToolkit().getScreenSize();
    }

    private final Runnable paintEvent = new Runnable() {
        public void run() {
            boolean shouldPaint = false;
            synchronized(RepaintManager.this) {
                numberOfScheduledPaintEvents--;
                shouldPaint = numberOfScheduledPaintEvents == 0;
            }
            if (shouldPaint) {
                validateInvalidComponents();
                paintDirtyRegions();
            }
        }
    };


    public static RepaintManager currentManager(final Component c) {
        if (instance == null) {
            instance = new RepaintManager();
        }

        return instance;
    }

    public static RepaintManager currentManager(final JComponent c) {
        return currentManager((Component)c);
    }


    public static void setCurrentManager(final RepaintManager repaintManager) {
        instance = repaintManager;
    }

    /**
     * Method doesn't perform component invalidation. It just adds component
     * validation root to the list of roots waiting for validation and schedules
     * it.
     */
    public void addInvalidComponent(final JComponent invalidComponent) {
        // implementation is done according to the black-box testing and contradict to the
        // spec: component is not marked as invalid (needed layout)
        final Component root = getValidationRoot(invalidComponent);
        if (root != null && !invalidRoots.contains(root) && !root.isValid() && root.isShowing()) {
            invalidRoots.add(root);
            scheduleProcessingEvent();
        }
    }

    public void removeInvalidComponent(final JComponent component) {
        invalidRoots.remove(component);
    }

    public void validateInvalidComponents() {
        while(!invalidRoots.isEmpty()) {
            List processingRoots;
            synchronized(invalidRoots) {
                processingRoots = new ArrayList(invalidRoots);
                invalidRoots.clear();
            }
            for (Iterator it = processingRoots.iterator(); it.hasNext(); ) {
                Component c = (Component)it.next();
                c.validate();
            }
        }
    }

    public void addDirtyRegion(final JComponent c, final int x, final int y, final int w, final int h) {
        if (c == null || w <= 0 || h <= 0 || !c.isShowing()) {
            return;
        }

        Window ancestingWindow = SwingUtilities.getWindowAncestor(c);
        if (ancestingWindow == null || !ComponentInternals.getComponentInternals().wasPainted(ancestingWindow)) {
            return;
        }

        Rectangle dirtyRect = new Rectangle(x, y, w, h);
        MultiRectArea previousValue = (MultiRectArea)dirtyRegions.get(c);
        if (previousValue != null) {
            previousValue.add(dirtyRect);
        } else {
            dirtyRegions.put(c, new MultiRectArea(dirtyRect));
        }

        scheduleProcessingEvent();
    }

    public Rectangle getDirtyRegion(final JComponent c) {
        MultiRectArea result = (MultiRectArea)dirtyRegions.get(c);
        return result != null ? result.getBounds() : new Rectangle();
    }

    public void markCompletelyDirty(final JComponent c) {
        addDirtyRegion(c, COMPLETELY_DIRTY_RECT.x, COMPLETELY_DIRTY_RECT.y, COMPLETELY_DIRTY_RECT.width, COMPLETELY_DIRTY_RECT.height);
    }

    public void markCompletelyClean(final JComponent c) {
        dirtyRegions.remove(c);
    }

    public boolean isCompletelyDirty(final JComponent c) {
        MultiRectArea dirtyRect = (MultiRectArea)dirtyRegions.get(c);
        if (dirtyRect == null) {
            return false;
        }
        Rectangle dirtyBounds = dirtyRect.getBounds();
        return dirtyBounds.width == COMPLETELY_DIRTY_RECT.width
               && dirtyBounds.height == COMPLETELY_DIRTY_RECT.height;
    }

    public void paintDirtyRegions() {
        prepareOptimizedDirtyRegions();
        for (Iterator it = optimizedDirtyRegions.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            MultiRectArea repaintRegion = (MultiRectArea)entry.getValue();
            if (!repaintRegion.isEmpty()) {
                ((JComponent)entry.getKey()).paintImmediately(new ClipRegion(repaintRegion));
            }
        }
    }

    // According to Spec as a offscreen buffer we can use offscreen image of any type,
    // which can keep own content. Our implementation of VolatileImage based on GDI Bitmap and
    // can't lost content. For performance reason as a offscreen buffer we use VolatileImage.
    public Image getOffscreenBuffer(final Component c, final int proposedWidth, final int proposedHeight) {
        int adjustedWidth = Math.min(proposedWidth, maximumSize.width);
        int adjustedHeight = Math.min(proposedHeight, maximumSize.height);

        if (offscreenImage != null
            && offscreenImage.getWidth(c) >= adjustedWidth
            && offscreenImage.getHeight(c) >= adjustedHeight) {

            fillImage(offscreenImage, c.getBackground(), adjustedWidth, adjustedHeight);
        } else {
            if (offscreenImage != null) {
                offscreenImage.flush();
            }

            offscreenImage = c.createVolatileImage(adjustedWidth, adjustedHeight);
        }

        return offscreenImage;
    }

    public Image getVolatileOffscreenBuffer(final Component c, final int proposedWidth, final int proposedHeight) {
        int adjustedWidth = Math.min(proposedWidth, maximumSize.width);
        int adjustedHeight = Math.min(proposedHeight, maximumSize.height);

        if (volatileOffscreenImage != null
            && volatileOffscreenImage.getWidth(c) >= adjustedWidth
            && volatileOffscreenImage.getHeight(c) >= adjustedHeight) {

            if (volatileOffscreenImage.contentsLost()) {
                volatileOffscreenImage.validate(c.getGraphicsConfiguration());
            }

            fillImage(volatileOffscreenImage, c.getBackground(), adjustedWidth, adjustedHeight);
        } else {
            if (volatileOffscreenImage != null) {
                volatileOffscreenImage.flush();
            }

            volatileOffscreenImage = c.createVolatileImage(adjustedWidth, adjustedHeight);
        }

        return volatileOffscreenImage;
    }

    public void setDoubleBufferMaximumSize(final Dimension d) {
        maximumSize = d;
    }

    public Dimension getDoubleBufferMaximumSize() {
        return maximumSize;
    }

    public void setDoubleBufferingEnabled(final boolean isEnabled) {
        doubleBufferingEnabled = isEnabled;
    }

    public boolean isDoubleBufferingEnabled() {
        return doubleBufferingEnabled;
    }


    /*
     * That is not the best way to do scheduling.
     * There are two issues with it:
     * 1. Each repaint() call puts an event to the queue. All such events excepting
     *    the latest one do nothing, but queue is loaded anyway
     * 2. There could be a case when no painting is done at all.
     *    Such situation could be if repaint() is scheduled regularly.
     *    <code>
     *    An example:
     *       final Runnable overload = new Runnable() {
     *           public void run() {
     *               SwingUtilities.invokeLater(this);
     *               component.repaint();
     *           }
     *       };
     *    </code>
     *    In this example component will never be painted since because every time
     *    repaint() is processed another repaint is sceduled and therefore effective
     *    painting is delayed.
     */
    private void scheduleProcessingEvent() {
        synchronized(this) {
            numberOfScheduledPaintEvents++;
        }
        EventQueue.invokeLater(paintEvent);
    }

    private Component getValidationRoot(final Component c) {
        if (c == null) {
            return null;
        }
        Component root = c;
        while (!(root instanceof JComponent)
                || !((JComponent) root).isValidateRoot()) {
            Container parent = root.getParent();

            if (parent == null) {
                break;
            } else {
                root = parent;
            }
        }
        return root;
    }

    private Map prepareOptimizedDirtyRegions() {
        optimizedDirtyRegions.clear();
        Set dirtyRegionsCopy;
        synchronized(dirtyRegions) {
            dirtyRegionsCopy = new HashSet(dirtyRegions.entrySet());
            dirtyRegions.clear();
        }
        for (Iterator dirties = dirtyRegionsCopy.iterator(); dirties.hasNext(); ) {
            Map.Entry entry = (Map.Entry)dirties.next();
            JComponent c = (JComponent)entry.getKey();
            MultiRectArea dirtyRect = (MultiRectArea)entry.getValue();
            dirtyRect.intersect(c.getVisibleRect());

            if (mergeWithParent(c, dirtyRect)) {
                continue;
            }
            if (mergeWithChildren(c, dirtyRect)) {
                continue;
            }
            optimizedDirtyRegions.put(c, dirtyRect);
        }

        return optimizedDirtyRegions;
    }

    private boolean mergeWithParent(final Component comp, final MultiRectArea compDirtyRegion) {
        Iterator optimized = optimizedDirtyRegions.entrySet().iterator();
        while (optimized.hasNext()) {
            Map.Entry optEntry = (Map.Entry)optimized.next();
            JComponent optC = (JComponent)optEntry.getKey();
            MultiRectArea optDirtyRegion = (MultiRectArea)optEntry.getValue();
            if (SwingUtilities.isDescendingFrom(comp, optC)) {
                ClipRegion.convertRegion(comp, compDirtyRegion, optC);
                optDirtyRegion.add(compDirtyRegion);
                return true;
            }
        }

        return false;
    }

    private boolean mergeWithChildren(final Component comp, final MultiRectArea compDirtyRegion) {
        Iterator optimized = optimizedDirtyRegions.entrySet().iterator();
        boolean foundChildren = false;
        while (optimized.hasNext()) {
            Map.Entry optEntry = (Map.Entry)optimized.next();
            JComponent optC = (JComponent)optEntry.getKey();
            MultiRectArea optDirtyRegion = (MultiRectArea)optEntry.getValue();
            if (SwingUtilities.isDescendingFrom(optC, comp)) {
                ClipRegion.convertRegion(optC, optDirtyRegion, comp);
                compDirtyRegion.add(optDirtyRegion);
                optimized.remove();
                foundChildren = true;
            }
        }
        if (foundChildren) {
            optimizedDirtyRegions.put(comp, compDirtyRegion);
        }

        return foundChildren;
    }


    private static void fillImage(final Image image, final Color c, final int width, final int height) {
        Graphics g = image.getGraphics();
        g.setColor(c);
        g.fillRect(0, 0, width, height);
        g.dispose();
    }
}
