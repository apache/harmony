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
 * @author Anton Avtamonov, Sergey Burlak
 */

package javax.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.Serializable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.ViewportUI;

import org.apache.harmony.x.swing.BlitSupport;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class JViewport extends JComponent implements Accessible {
    private static final long serialVersionUID = -633250949788872287L;
    
    protected class ViewListener extends ComponentAdapter implements Serializable {
        private static final long serialVersionUID = 6947347842254105360L;
        
        @Override
        public void componentResized(final ComponentEvent e) {
            fireStateChanged();
            revalidate();
        }
    }

    protected class AccessibleJViewport extends AccessibleJComponent {
        private static final long serialVersionUID = 481322936279100213L;
        
        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.VIEWPORT;
        }
    }

    public static final int BACKINGSTORE_SCROLL_MODE = 2;
    public static final int BLIT_SCROLL_MODE = 1;
    public static final int SIMPLE_SCROLL_MODE = 0;
    private static final String UI_CLASS_ID = "ViewportUI";

    protected Point lastPaintPosition;
    protected boolean isViewSizeSet;
    @Deprecated
    protected boolean backingStore;
    protected transient Image backingStoreImage;
    protected boolean scrollUnderway;

    private final EventListenerList eventListenerList;
    private ViewportLayout viewportLayout;
    private int scrollMode = BLIT_SCROLL_MODE;

    private final ComponentListener viewListener = new ViewListener();

    private BlitSupport blitSupport;

    public JViewport() {
        eventListenerList = new EventListenerList();
        setLayout(createLayoutManager());

        updateUI();
    }

    public Point toViewCoordinates(final Point p) {
        return new Point(p);
    }

    public Dimension toViewCoordinates(final Dimension d) {
        return new Dimension(d);
    }

    @Override
    public final Insets getInsets(final Insets insets) {
        insets.top = 0;
        insets.left = 0;
        insets.bottom = 0;
        insets.right = 0;

        return insets;
    }

    public void setUI(final ViewportUI ui) {
        super.setUI(ui);
    }

    public ViewportUI getUI() {
        return (ViewportUI)ui;
    }

    @Override
    public void updateUI() {
        setUI((ViewportUI)UIManager.getUI(this));
    }

    public void removeChangeListener(final ChangeListener l) {
        eventListenerList.remove(ChangeListener.class, l);
    }

    public void addChangeListener(final ChangeListener l) {
        eventListenerList.add(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return eventListenerList.getListeners(ChangeListener.class);
    }

    @Override
    public final void setBorder(final Border b) {
        if (b != null) {
            throw new IllegalArgumentException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
    }

    protected ViewListener createViewListener() {
        return new ViewListener();
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        return new AccessibleJViewport();
    }

    @Override
    protected String paramString() {
        StringBuilder result = new StringBuilder(super.paramString());
        result.append(",isViewSizeSet=").append(isViewSizeSet)
              .append(",lastPaintPosition=").append(lastPaintPosition)
              .append(",scrollUnderway=").append(scrollUnderway);

        return result.toString();
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    @Override
    public void scrollRectToVisible(final Rectangle r) {
        Point viewPos = getViewPosition();
        Dimension viewSize = getExtentSize();

        int dx;
        int dy;

        if (r.x > 0) {
            if (r.x + r.width > viewSize.width) {
                int dx2 = r.x + r.width - viewSize.width;
                dx = Math.min(r.x, dx2);
            } else {
                dx = 0;
            }
        } else if (r.x < 0) {
            if (r.x + r.width < viewSize.width) {
                int dx2 = r.x + r.width - viewSize.width;
                dx = Math.max(r.x, dx2);
            } else {
                dx = 0;
            }
        } else {
            dx = 0;
        }

        if (r.y > 0) {
            if (r.y + r.height > viewSize.height) {
                int dy2 = r.y + r.height - viewSize.height;
                dy = Math.min(r.y, dy2);
            } else {
                dy = 0;
            }
        } else if (r.y < 0) {
            if (r.y + r.height < viewSize.height) {
                int dy2 = r.y + r.height - viewSize.height;
                dy = Math.max(r.y, dy2);
            } else {
                dy = 0;
            }
        } else {
            dy = 0;
        }

        if (dx != 0 || dy != 0) {
            int x = viewPos.x + dx;
            int y = viewPos.y + dy;

            Point point = new Point(x, y);

            scrollUnderway = true;
            setViewPosition(point);
            scrollUnderway = false;
        }
    }

    public Rectangle getViewRect() {
        return new Rectangle(getViewPosition(), getExtentSize());
    }

    public void setViewPosition(final Point p) {
        if (getView() == null) {
            return;
        }

        if (getViewPosition().equals(p)) {
            return;
        }

        int x = p.x;
        int y = p.y;
        if (getExtentSize().width < getViewSize().width && x + getExtentSize().width > getViewSize().width) {
            x = getViewSize().width - getExtentSize().width;
        }
        if (getExtentSize().height < getViewSize().height && y + getExtentSize().height > getViewSize().height) {
            y = getViewSize().height - getExtentSize().height;
        }
        if (x + getExtentSize().width  < 0) {
            x = 0;
        }
        if (y + getExtentSize().height < 0) {
            y = 0;
        }

        getView().setLocation(-x, -y);

        if (scrollMode == BLIT_SCROLL_MODE && !scrollUnderway && blitSupport != null) {
            blitSupport.paint();
        }

        fireStateChanged();
    }

    public Point getViewPosition() {
        if (getView() == null) {
            return new Point(0, 0);
        }

        Point location = getView().getLocation();
        return new Point(-location.x, -location.y);
    }

    protected LayoutManager createLayoutManager() {
        if (viewportLayout == null) {
            viewportLayout = new ViewportLayout();
        }
        return viewportLayout;
    }

    @Override
    public final Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    public void setViewSize(final Dimension d) {
        if (getView() == null) {
            return;
        }

        getView().setSize(d);

        fireStateChanged();
        isViewSizeSet = true;
    }

    public void setExtentSize(final Dimension d) {
        setSize(d);
        fireStateChanged();
    }

    public Dimension getViewSize() {
        Component view = getView();
        if (view == null) {
            return new Dimension(0, 0);
        }
        return isViewSizeSet ? view.getSize() : view.getPreferredSize();
    }

    public Dimension getExtentSize() {
        return getSize();
    }

    public void setView(final Component c) {
        remove(getView());
        if (c != null) {
            add(c, 0);
            c.addComponentListener(viewListener);

            if (c instanceof JComponent) {
                if (blitSupport == null) {
                    blitSupport = new BlitSupport((JComponent)c);
                } else {
                    blitSupport.setBlitComponent((JComponent)c);
                }
            } else {
                blitSupport = null;
            }
        }
    }

    @Override
    public void remove(final Component c) {
        if (c == null) {
            return;
        }
        c.removeComponentListener(viewListener);
        super.remove(c);
    }

    public Component getView() {
        if (getComponentCount() > 0) {
            return getComponent(0);
        }
        return null;
    }

    @Deprecated
    public void setBackingStoreEnabled(final boolean b) {
        backingStore = b;
    }

    @Override
    public void paint(final Graphics g) {
        lastPaintPosition = getViewPosition();
        if (blitSupport != null) {
            blitSupport.onPaint();
        }

        if ((scrollMode == BACKINGSTORE_SCROLL_MODE
             || scrollMode == BLIT_SCROLL_MODE && RepaintManager.currentManager(this).isDoubleBufferingEnabled())
            && !insideDoubleBuffering()) {

            paintDoubleBuffered(g);
        } else {
            super.paint(g);
        }
    }

    public void setScrollMode(final int mode) {
        scrollMode = mode;
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    @Deprecated
    public boolean isBackingStoreEnabled() {
        return backingStore;
    }

    public int getScrollMode() {
        return scrollMode;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void reshape(final int x, final int y, final int w, final int h) {
        boolean fireEvent = getWidth() != w || getHeight() != h;
        super.reshape(x, y, w, h);
        if (fireEvent) {
            fireStateChanged();
        }
    }


    /*
     * In our implementation special BlitSupport class is used to control blit paintings.
     * Therefore this method is not used, all this functionality is provided by BlitSupport
     */
    protected boolean computeBlit(final int dx, final int dy, final Point blitFrom, final Point blitTo, final Dimension blitSize, final Rectangle blitPaint) {
        if ((Math.abs(dx) >= blitSize.width) || (Math.abs(dy) >= blitSize.height)
                 || ((dx == 0) && (dy == 0)) || ((dx != 0) && (dy != 0))) {

            return false;
        }
        blitSize.setSize(blitSize.width - Math.abs(dx), blitSize.height - Math.abs(dy));
        if (dx < 0) {
            blitTo.setLocation(0, 0);
            blitFrom.setLocation(-dx, -dy);
            blitPaint.setBounds(blitSize.width, 0, -dx, blitSize.height);
        } else if (dx > 0) {
            blitFrom.setLocation(0, 0);
            blitTo.setLocation(dx, dy);
            blitPaint.setBounds(0, 0, dx, blitSize.height);
        } else if (dy < 0) {
            blitTo.setLocation(0, 0);
            blitFrom.setLocation(-dx, -dy);
            blitPaint.setBounds(0, blitSize.height, blitSize.width, -dy);
        } else {
            blitFrom.setLocation(0, 0);
            blitTo.setLocation(dx, dy);
            blitPaint.setBounds(0, 0, blitSize.width, dy);
        }

        return true;
    }

    protected void fireStateChanged() {
        ChangeListener[] changeListeners = getChangeListeners();
        ChangeEvent changeEvent = new ChangeEvent(this);
        for (ChangeListener element : changeListeners) {
            element.stateChanged(changeEvent);
        }
    }

    @Override
    protected void firePropertyChange(final String propName, final Object oldVal, final Object newVal) {
        super.firePropertyChange(propName, oldVal, newVal);
    }

    @Override
    protected void addImpl(final Component c, final Object obj, final int index) {
        if (c != null) {
            super.addImpl(c, null, 0);
        } else {
            removeAll();
        }
    }
}
