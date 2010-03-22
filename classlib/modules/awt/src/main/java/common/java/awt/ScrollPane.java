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

package java.awt;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import org.apache.harmony.awt.ScrollStateController;
import org.apache.harmony.awt.Scrollable;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.theme.DefaultButton;

public class ScrollPane extends Container implements Accessible {
    private static final long serialVersionUID = 7956609840827222915L;

    public static final int SCROLLBARS_AS_NEEDED = 0;

    public static final int SCROLLBARS_ALWAYS = 1;

    public static final int SCROLLBARS_NEVER = 2;

    final static int HSCROLLBAR_HEIGHT = 16;

    final static int VSCROLLBAR_WIDTH = 16;

    final static int BORDER_SIZE = 2;

    private final static Insets defInsets = new Insets(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE,
            BORDER_SIZE);

    private int scrollbarDisplayPolicy;

    private boolean wheelScrollingEnabled;

    private ScrollPaneAdjustable hAdjustable;

    private ScrollPaneAdjustable vAdjustable;

    private final ScrollStateController stateController;

    private final Scrollable scrollable;

    protected class AccessibleAWTScrollPane extends AccessibleAWTContainer {
        private static final long serialVersionUID = 6100703663886637L;

        protected AccessibleAWTScrollPane() {
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SCROLL_PANE;
        }
    }

    class SPScrollable implements Scrollable {
        public Adjustable getVAdjustable() {
            return vAdjustable;
        }

        public Adjustable getHAdjustable() {
            return hAdjustable;
        }

        public Insets getInsets() {
            return ScrollPane.defInsets;
        }

        public Point getLocation() {
            return getScrollPosition();
        }

        @SuppressWarnings("deprecation")
        public void setLocation(Point p) {
            Component comp = ScrollPane.this.getComponent();
            if (comp != null) {
                comp.move(p.x, p.y);
            }
        }

        public Component getComponent() {
            return ScrollPane.this;
        }

        public Dimension getSize() {
            Dimension size = new Dimension();
            Component comp = ScrollPane.this.getComponent();
            if (comp != null) {
                size.setSize(comp.getSize());
            }
            return size;
        }

        public void doRepaint() {
            ScrollPane.this.doRepaint();
        }

        public int getAdjustableWidth() {
            return getVScrollbarWidth();
        }

        public int getAdjustableHeight() {
            return getHScrollbarHeight();
        }

        public void setAdjustableSizes(Adjustable adj, int vis, int min, int max) {
            ((ScrollPaneAdjustable) adj).setSizes(vis, min, max);
        }

        public int getAdjustableMode(Adjustable adj) {
            return getScrollbarDisplayPolicy();
        }

        public void setAdjustableBounds(Adjustable adj, Rectangle r) {
            ((ScrollPaneAdjustable) adj).setBounds(r);
        }

        public int getWidth() {
            return ScrollPane.this.getWidth();
        }

        public int getHeight() {
            return ScrollPane.this.getHeight();
        }

        public void doRepaint(Rectangle r) {
            ScrollPane.this.doRepaint(r);
        }
    }

    public ScrollPane() throws HeadlessException {
        this(SCROLLBARS_AS_NEEDED);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public ScrollPane(int scrollbarDisplayPolicy) throws HeadlessException {
        toolkit.lockAWT();
        try {
            Toolkit.checkHeadless();
            switch (scrollbarDisplayPolicy) {
                case SCROLLBARS_ALWAYS:
                case SCROLLBARS_AS_NEEDED:
                case SCROLLBARS_NEVER:
                    break;
                default:
                    // awt.146=illegal scrollbar display policy
                    throw new IllegalArgumentException(Messages.getString("awt.146")); //$NON-NLS-1$
            }
            this.scrollbarDisplayPolicy = scrollbarDisplayPolicy;
            setWheelScrollingEnabled(true);
            hAdjustable = new ScrollPaneAdjustable(this, Adjustable.HORIZONTAL);
            vAdjustable = new ScrollPaneAdjustable(this, Adjustable.VERTICAL);
            scrollable = new SPScrollable();
            stateController = new ScrollStateController(scrollable);
            addAWTComponentListener(stateController);
            hAdjustable.addAWTAdjustmentListener(stateController);
            vAdjustable.addAWTAdjustmentListener(stateController);
            // The initial size of this container is set to 100x100:
            setSize(100, 100);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void addNotify() {
        toolkit.lockAWT();
        try {
            super.addNotify();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        toolkit.lockAWT();
        try {
            return super.getAccessibleContext();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public String paramString() {
        /* The format is based on 1.5 release behavior 
         * which can be revealed by the following code:
         * System.out.println(new ScrollPane());
         */
        toolkit.lockAWT();
        try {
            Point scrollPos = new Point();
            try {
                scrollPos = getScrollPosition();
            } catch (NullPointerException npe) {
            }
            Insets ins = getInsets();
            String strPolicy = ""; //$NON-NLS-1$
            switch (getScrollbarDisplayPolicy()) {
                case SCROLLBARS_ALWAYS:
                    strPolicy = "always"; //$NON-NLS-1$
                    break;
                case SCROLLBARS_AS_NEEDED:
                    strPolicy = "as-needed"; //$NON-NLS-1$
                    break;
                case SCROLLBARS_NEVER:
                    strPolicy = "never"; //$NON-NLS-1$
                    break;
            }
            return (super.paramString() + ",ScrollPosition=(" + scrollPos.x + "," + scrollPos.x //$NON-NLS-1$ //$NON-NLS-2$
                    + ")" + ",Insets=(" + ins.left + "," + ins.top + "," + ins.right + "," //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                    + ins.bottom + ")" + ",ScrollbarDisplayPolicy=" + strPolicy //$NON-NLS-1$ //$NON-NLS-2$
                    + ",wheelScrollingEnabled=" + isWheelScrollingEnabled()); //$NON-NLS-1$
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void doLayout() {
        toolkit.lockAWT();
        try {
            layout();
        } finally {
            toolkit.unlockAWT();
        }
    }

    protected boolean eventTypeEnabled(int type) {
        toolkit.lockAWT();
        try {
            return (isWheelScrollingEnabled() && (type == MouseEvent.MOUSE_WHEEL));
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    @Override
    public void layout() {
        toolkit.lockAWT();
        try {
            Component scrollComp = getComponent();
            if (scrollComp != null) {
                Rectangle clientRect = getClient();
                Dimension prefSize = scrollComp.getPreferredSize();
                Dimension viewSize = clientRect.getSize();
                int newWidth = Math.max(viewSize.width, prefSize.width);
                int newHeight = Math.max(viewSize.height, prefSize.height);
                scrollComp.setSize(newWidth, newHeight);
                stateController.layoutScrollbars();
                //set value if current value is invalid:
                Point oldScrollPos = getScrollPosition();
                setScrollPosition(oldScrollPos);
                Point scrollPos = getScrollPosition();
                // correct component's position even if
                // value is the same as old
                if (oldScrollPos.equals(scrollPos)) {
                    scrollComp.setLocation(scrollPos);
                }
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    protected void processMouseWheelEvent(MouseWheelEvent e) {
        toolkit.lockAWT();
        try {
            stateController.mouseWheelMoved(e);
            super.processMouseWheelEvent(e);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    protected final void addImpl(Component comp, Object constraints, int index) {
        toolkit.lockAWT();
        try {
            if (index > 0) {
                // awt.147=position greater than 0
                throw new IllegalArgumentException(Messages.getString("awt.147")); //$NON-NLS-1$
            }
            if (getComponentCount() > 0) {
                remove(0);
            }
            super.addImpl(comp, constraints, index);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Adjustable getHAdjustable() {
        toolkit.lockAWT();
        try {
            return hAdjustable;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getHScrollbarHeight() {
        toolkit.lockAWT();
        try {
            return isDisplayable() ? HSCROLLBAR_HEIGHT : 0;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Point getScrollPosition() {
        toolkit.lockAWT();
        try {
            Component comp = getComponent();
            if (comp == null) {
                // awt.148=child is null
                throw new NullPointerException(Messages.getString("awt.148")); //$NON-NLS-1$
            }
            return new Point(hAdjustable.getValue(), vAdjustable.getValue());
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getScrollbarDisplayPolicy() {
        toolkit.lockAWT();
        try {
            return scrollbarDisplayPolicy;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Adjustable getVAdjustable() {
        toolkit.lockAWT();
        try {
            return vAdjustable;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getVScrollbarWidth() {
        toolkit.lockAWT();
        try {
            return isDisplayable() ? VSCROLLBAR_WIDTH : 0;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension getViewportSize() {
        toolkit.lockAWT();
        try {
            return getClient().getSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isWheelScrollingEnabled() {
        toolkit.lockAWT();
        try {
            return wheelScrollingEnabled;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void printComponents(Graphics g) {
        toolkit.lockAWT();
        try {
            // just call super
            // TODO: find out why override
            super.printComponents(g);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public final void setLayout(LayoutManager mgr) {
        toolkit.lockAWT();
        try {
            //don't let user set layout: throw error
            // awt.149=ScrollPane controls layout
            throw new AWTError(Messages.getString("awt.149")); //$NON-NLS-1$
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setScrollPosition(Point p) {
        toolkit.lockAWT();
        try {
            setScrollPosition(p.x, p.y);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setScrollPosition(int x, int y) {
        toolkit.lockAWT();
        try {
            Component child = getComponent();
            Dimension vSize = getViewportSize();
            int maxX = child.getWidth() - vSize.width;
            int maxY = child.getHeight() - vSize.height;
            int newX = Math.max(0, Math.min(x, maxX));
            int newY = Math.max(0, Math.min(y, maxY));
            int oldX = hAdjustable.getValue();
            int oldY = vAdjustable.getValue();
            if (newX != oldX) {
                hAdjustable.setValue(newX);
            }
            if (newY != oldY) {
                vAdjustable.setValue(newY);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setWheelScrollingEnabled(boolean handleWheel) {
        toolkit.lockAWT();
        try {
            wheelScrollingEnabled = handleWheel;
            long mask = AWTEvent.MOUSE_WHEEL_EVENT_MASK;
            if (wheelScrollingEnabled) {
                enableEvents(mask);
            } else {
                disableEvents(mask);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    ComponentBehavior createBehavior() {
        return new HWBehavior(this);
    }

    @Override
    boolean isPrepainter() {
        return true;
    }

    @Override
    void prepaint(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, w, h);
        // draw pressed button frame:
        DefaultButton.drawButtonFrame(g, new Rectangle(new Point(), getSize()), true);
        vAdjustable.prepaint(g);
        hAdjustable.prepaint(g);
    }

    Component getComponent() {
        if (getComponentCount() > 0) {
            return getComponent(0);
        }
        return null;
    }

    @Override
    Insets getNativeInsets() {
        if (!isDisplayable()) {
            return super.getNativeInsets();
        }
        Insets insets = (Insets) defInsets.clone();
        insets.bottom += hAdjustable.getBounds().height;
        insets.right += vAdjustable.getBounds().width;
        return insets;
    }

    @Override
    String autoName() {
        return ("scrollpane" + toolkit.autoNumber.nextScrollPane++); //$NON-NLS-1$
    }

    Dimension calculateMinimumSize() {
        return getSize(); // FIXME: component should do this
    }

    Dimension calculatePreferredSize() {
        return getMinimumSize(); // FIXME: component should do this
    }

    private void doRepaint(Rectangle r) {
        if (isDisplayable()) {
            invalidate();
            if (isShowing() && (r != null)) {
                repaint(r.x, r.y, r.width, r.height);
            }
        }
    }

    private void doRepaint() {
        stateController.layoutScrollbars();
        doRepaint(new Rectangle(new Point(), getSize()));
    }

    @Override
    AccessibleContext createAccessibleContext() {
        return new AccessibleAWTScrollPane();
    }
}
