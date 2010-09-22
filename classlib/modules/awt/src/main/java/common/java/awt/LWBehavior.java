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
package java.awt;

import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.wtk.NativeWindow;


/**
 * A lightweight component specific behaviour.
 *
 */

class LWBehavior implements ComponentBehavior {

    /*
     * only lightweights store their position/size, others get it from native
     * resources each time
     */

    private int x, y, w, h;

    private final Component component;

    private boolean visible;

    private boolean displayable;

    LWBehavior(Component comp) {
        component = comp;
        /* lightweight components are initially visible */
        visible = component.isVisible();
    }

    public void addNotify() {
        displayable = true;
        if (visible) {
            component.repaint();
        }
    }

    public void setVisible(boolean b) {
        visible = b;
        if (visible) {
            component.repaint();
        } else if (isParentShowing()) {
            component.parent.repaint(x, y, w, h);
        }
    }

    public Graphics getGraphics(int translationX, int translationY, int width,
            int height) {
        Graphics g = null;
        Container parent = component.getParent();

        if (parent != null) {
            g = parent.behaviour.getGraphics(translationX + component.x,
                    translationY + component.y, width, height);
        }

        return g;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setBounds(int x, int y, int width, int height) {
        Rectangle oldBounds = new Rectangle(this.x, this.y, this.w, this.h);
        Rectangle newBounds = new Rectangle(x, y, width, height);

        if (oldBounds.equals(newBounds)) {
            return;
        }
        w = width;
        h = height;
        this.x = x;
        this.y = y;

        boolean grow = newBounds.contains(oldBounds);
        boolean shrink = oldBounds.contains(newBounds);
        if (isParentShowing() && !grow) {
            Component parent = component.parent;
            if (!shrink) {
                parent.repaintRegion = new MultiRectArea(oldBounds);
                parent.repaintRegion.substract(newBounds);
            }
            parent.repaint(oldBounds.x, oldBounds.y, oldBounds.width, oldBounds.height);
        }
        if (visible && displayable && !shrink) {
            component.repaint();
        }
    }

    public boolean isLightweight() {
        return true;
    }

    public boolean isOpaque() {
        return false;
    }

    public void setBounds(int x, int y, int w, int h, int bMask) {
        setBounds(component.x, component.y, component.w, component.h);
        onMove(x, y);
    }

    public boolean isDisplayable() {
        return displayable;
    }

    public void setEnabled(boolean value) {
    }

    public void removeNotify() {
        displayable = false;
        if (visible && isParentShowing()) {
            component.parent.repaint(x, y, w, h);
        }
    }

    public NativeWindow getNativeWindow() {
        return component.getHWAncestor().getNativeWindow();
    }

    public void onMove(int x, int y) {
        if (component instanceof Container) {
            Container cont = (Container) component;
            for (int i = 0; i < cont.getComponentCount(); i++) {
                cont.getComponent(i).behaviour.onMove(x, y);
            }
        }

    }

    public void setZOrder(int newIndex, int oldIndex) {
        // do nothing
    }

    public boolean setFocus(boolean focus, Component opposite) {

        // request [native] focus from the nearest heavyweight ancestor
        // don't post event, so call only cb
        Component hw = component.getHWAncestor();
        if (hw != null) {
            hw.behaviour.setFocus(focus, opposite);
        }

        return true;
    }

    private boolean isParentShowing() {
        return component.parent != null && component.parent.visible
            && component.parent.behaviour.isDisplayable();
    }
}