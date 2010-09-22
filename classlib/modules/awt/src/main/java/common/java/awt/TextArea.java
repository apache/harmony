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

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;
import java.util.Set;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.View;
import org.apache.harmony.awt.ScrollStateController;
import org.apache.harmony.awt.Scrollable;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.text.TextFactory;
import org.apache.harmony.awt.wtk.NativeWindow;

public class TextArea extends TextComponent {
    protected class AccessibleAWTTextArea extends AccessibleAWTTextComponent {
        private static final long serialVersionUID = 3472827823632144419L;

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet set = super.getAccessibleStateSet();
            set.add(AccessibleState.MULTI_LINE);
            return set;
        }
    }

    /**
     * Scrolling behavior implementation
     */
    class TextScrollable implements Scrollable {
        public Adjustable getVAdjustable() {
            return vAdjustable;
        }

        public Adjustable getHAdjustable() {
            return hAdjustable;
        }

        public Insets getInsets() {
            return getNativeInsets();
        }

        public Point getLocation() {
            Point p = getViewPosition();
            p.setLocation(-p.x, -p.y);
            return p;
        }

        public void setLocation(Point p) {
            Point pos = new Point(-p.x, -p.y);
            setViewPosition(pos);
        }

        public Component getComponent() {
            return TextArea.this;
        }

        public Dimension getSize() {
            return getModelRect().getSize();
        }

        public void doRepaint() {
            TextArea.this.doRepaint();
        }

        public int getAdjustableWidth() {
            return (vAdjustable != null ? vAdjustable.getBounds().width : 0);
        }

        public int getAdjustableHeight() {
            return (hAdjustable != null ? hAdjustable.getBounds().height : 0);
        }

        public void setAdjustableSizes(Adjustable adj, int vis, int min, int max) {
            ((ScrollPaneAdjustable) adj).setSizes(vis, min, max);
        }

        public int getAdjustableMode(Adjustable adj) {
            switch (getScrollbarVisibility()) {
                case SCROLLBARS_BOTH:
                    return Scrollable.ALWAYS;
                case SCROLLBARS_HORIZONTAL_ONLY:
                    return Scrollable.HORIZONTAL_ONLY;
                case SCROLLBARS_NONE:
                    return Scrollable.NEVER;
                case SCROLLBARS_VERTICAL_ONLY:
                    return Scrollable.VERTICAL_ONLY;
                default:
                    return Scrollable.NEVER;
            }
        }

        public void setAdjustableBounds(Adjustable adj, Rectangle r) {
            ((ScrollPaneAdjustable) adj).setBounds(r);
        }

        public int getWidth() {
            return TextArea.this.getWidth();
        }

        public int getHeight() {
            return TextArea.this.getHeight();
        }

        public void doRepaint(Rectangle r) {
            TextArea.this.doRepaint(r);
        }
    }

    /**
     * Helper class which filters out all mouse
     * events on non-client area and switches cursors
     * (default cursor is always shown above non-client area)
     */
    class MouseEventFilter implements MouseListener, MouseMotionListener {
        private final MouseListener mListener;

        private final MouseMotionListener mmListener;

        private boolean inside = true;

        boolean clientDrag;

        boolean scrollDrag;

        public MouseEventFilter(MouseListener ml, MouseMotionListener mml) {
            mListener = ml;
            mmListener = mml;
        }

        private boolean accept(MouseEvent e) {
            return getClient().contains(e.getPoint());
        }

        public void mouseClicked(MouseEvent e) {
            if (inside = accept(e)) {
                mListener.mouseClicked(e);
            } else {
                setDefaultCursor();
            }
        }

        public void mouseEntered(MouseEvent e) {
            inside = accept(e);
            if (!inside) {
                setDefaultCursor();
            }
            mListener.mouseEntered(e);
        }

        public void mouseExited(MouseEvent e) {
            mListener.mouseExited(e);
        }

        public void mousePressed(MouseEvent e) {
            if (inside = accept(e)) {
                clientDrag = true;
                mListener.mousePressed(e);
            } else {
                scrollDrag = true;
                setDefaultCursor();
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (inside = accept(e) || clientDrag) {
                mListener.mouseReleased(e);
            } else {
                setDefaultCursor();
            }
            clientDrag = false;
            scrollDrag = false;
        }

        public void mouseDragged(MouseEvent e) {
            if (!scrollDrag && (accept(e) || clientDrag)) {
                mmListener.mouseDragged(e);
            }
        }

        public void mouseMoved(MouseEvent e) {
            if (accept(e)) {
                if (!inside) {
                    setCursor();
                    inside = true;
                }
                mmListener.mouseMoved(e);
            } else if (inside) {
                setDefaultCursor();
                inside = false;
            }
        }

        private void setDefaultCursor() {
            Window topLevel = getWindowAncestor();
            if (topLevel == null) {
                return;
            }
            NativeWindow wnd = topLevel.getNativeWindow();
            if (wnd == null) {
                return;
            }
            Cursor.getDefaultCursor().getNativeCursor().setCursor(wnd.getId());
        }
    }

    private static final long serialVersionUID = 3692302836626095722L;

    public static final int SCROLLBARS_BOTH = 0;

    public static final int SCROLLBARS_VERTICAL_ONLY = 1;

    public static final int SCROLLBARS_HORIZONTAL_ONLY = 2;

    public static final int SCROLLBARS_NONE = 3;

    private int rows;

    private int columns;

    private int scrollbarVisibility = SCROLLBARS_BOTH;

    private ScrollPaneAdjustable hAdjustable;

    private ScrollPaneAdjustable vAdjustable;

    private final ScrollStateController stateController;

    private final Scrollable scrollable;

    private MouseEventFilter filter;

    public TextArea() throws HeadlessException {
        this(new String(), 0, 0, SCROLLBARS_BOTH);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public TextArea(String text, int rows, int columns, int scrollbars)
            throws HeadlessException {
        super();
        toolkit.lockAWT();
        try {
            Toolkit.checkHeadless();
            setFont(new Font("Dialog", Font.PLAIN, 12)); // QUICK FIX //$NON-NLS-1$
            setText(text);
            this.rows = Math.max(0, rows);
            this.columns = Math.max(0, columns);
            if ((scrollbars < SCROLLBARS_BOTH) || (scrollbars > SCROLLBARS_NONE)) {
                scrollbars = SCROLLBARS_BOTH;
            }
            scrollbarVisibility = scrollbars;
            if (noHorizontalScroll()) {
                replaceView();
            }
            setFocusTraversalKeys();
            // init scrolling
            hAdjustable = new ScrollPaneAdjustable(this, Adjustable.HORIZONTAL);
            vAdjustable = new ScrollPaneAdjustable(this, Adjustable.VERTICAL);
            scrollable = new TextScrollable();
            stateController = new ScrollStateController(scrollable);
            addScrolling();
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * Excludes &lt;Tab>, &lt;Shift-Tab> from default focus traversal keys sets
     */
    private void setFocusTraversalKeys() {
        int id = KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS;
        Set<AWTKeyStroke> set = new HashSet<AWTKeyStroke>(getFocusTraversalKeys(id));
        AWTKeyStroke tab = AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0);
        AWTKeyStroke shiftTab = AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
                InputEvent.SHIFT_DOWN_MASK);
        set.remove(tab);
        setFocusTraversalKeys(id, set);
        id = KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS;
        set = new HashSet<AWTKeyStroke>(getFocusTraversalKeys(id));
        set.remove(shiftTab);
        setFocusTraversalKeys(id, set);
    }

    /**
     * Replaces default plain view with wrapped plain view
     * to get word-wrapping behavior of TextArea without
     * horizontal scrollbar
     */
    private void replaceView() {
        TextFactory factory = TextFactory.getTextFactory();
        View view = factory.createWrappedPlainView(document.getDefaultRootElement());
        rootViewContext.getView().replace(0, 1, new View[] { view });
    }

    private boolean noHorizontalScroll() {
        return ((scrollbarVisibility == SCROLLBARS_NONE) || (scrollbarVisibility == SCROLLBARS_VERTICAL_ONLY));
    }

    public TextArea(String text, int rows, int columns) throws HeadlessException {
        this(text, rows, columns, SCROLLBARS_BOTH);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public TextArea(String text) throws HeadlessException {
        this(text, 0, 0, SCROLLBARS_BOTH);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public TextArea(int rows, int columns) throws HeadlessException {
        this(new String(), rows, columns, SCROLLBARS_BOTH);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void append(String str) {
        try {
            document.insertString(document.getLength(), str, null);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
        //        toolkit.lockAWT();
        //        try {
        if (isDisplayable()) {
            // update caret position on text append
            int newCaretPos = document.getLength();
            if (caret.getDot() != newCaretPos) {
                caret.setDot(newCaretPos, caret.getDotBias());
            }
        }
        //        } finally {
        //            toolkit.unlockAWT();
        //        }
    }

    public void insert(String str, int pos) {
        int oldPos = caret.getDot();
        try {
            document.insertString(pos, str, null);
        } catch (BadLocationException e) {
            throw new IndexOutOfBoundsException();
        }
        //        toolkit.lockAWT();
        //        try {
        if (isDisplayable()) {
            // update caret position on text insertion
            int newCaretPos = pos + str.length();
            if (caret.getDot() != newCaretPos) {
                caret.setDot(newCaretPos, caret.getDotBias());
            }
        } else if (caret.getDot() != oldPos) {
            // move caret back:
            caret.setDot(oldPos, caret.getDotBias());
        }
        //        } finally {
        //            toolkit.unlockAWT();
        //        }
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

    @Deprecated
    public void appendText(String str) {
        append(str);
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

    public int getColumns() {
        toolkit.lockAWT();
        try {
            return columns;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public Dimension getMinimumSize() {
        toolkit.lockAWT();
        try {
            return minimumSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension getMinimumSize(int rows, int columns) {
        toolkit.lockAWT();
        try {
            return minimumSize(rows, columns);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension getPreferredSize(int rows, int columns) {
        toolkit.lockAWT();
        try {
            return preferredSize(rows, columns);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        toolkit.lockAWT();
        try {
            return preferredSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getRows() {
        toolkit.lockAWT();
        try {
            return rows;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getScrollbarVisibility() {
        toolkit.lockAWT();
        try {
            return scrollbarVisibility;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void insertText(String str, int pos) {
        insert(str, pos);
    }

    @Deprecated
    @Override
    public Dimension minimumSize() {
        toolkit.lockAWT();
        try {
            if ((rows > 0) && (columns > 0)) {
                return minimumSize(rows, columns);
            }
            return super.minimumSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public Dimension minimumSize(int rows, int columns) {
        toolkit.lockAWT();
        try {
            Dimension minSize = calcSize(rows, columns);
            if (minSize == null) {
                return super.minimumSize();
            }
            return minSize;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    protected String paramString() {
        /* The format is based on 1.5 release behavior 
         * which can be revealed by the following code:
         * System.out.println(new TextArea());
         */
        toolkit.lockAWT();
        try {
            String strScrollbarVis = null;
            switch (getScrollbarVisibility()) {
                case SCROLLBARS_BOTH:
                    strScrollbarVis = "both"; //$NON-NLS-1$
                    break;
                case SCROLLBARS_HORIZONTAL_ONLY:
                    strScrollbarVis = "horizontal only"; //$NON-NLS-1$
                    break;
                case SCROLLBARS_NONE:
                    strScrollbarVis = "none"; //$NON-NLS-1$
                    break;
                case SCROLLBARS_VERTICAL_ONLY:
                    strScrollbarVis = "vertical only"; //$NON-NLS-1$
                    break;
            }
            return (super.paramString() + ",rows=" + getRows() + ",columns=" + getColumns() //$NON-NLS-1$ //$NON-NLS-2$
                    + ",scrollbarVisibility=" + strScrollbarVis); //$NON-NLS-1$
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    @Override
    public Dimension preferredSize() {
        toolkit.lockAWT();
        try {
            if ((rows > 0) && (columns > 0)) {
                return preferredSize(rows, columns);
            }
            return super.preferredSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public Dimension preferredSize(int rows, int columns) {
        toolkit.lockAWT();
        try {
            Dimension prefSize = calcSize(rows, columns);
            if (prefSize == null) {
                return super.preferredSize();
            }
            return prefSize;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void replaceRange(String str, int start, int end) {
        int length = end - start;
        int oldPos = caret.getDot();
        try {
            document.replace(start, length, str, null);
        } catch (BadLocationException e) {
            // ignore exception silently
        }
        toolkit.lockAWT();
        try {
            if (isDisplayable()) {
                // update caret position on text replacement
                int newCaretPos = start + str.length();
                if (caret.getDot() != newCaretPos) {
                    caret.setDot(newCaretPos, caret.getDotBias());
                }
            } else if (caret.getDot() != oldPos) {
                // move caret back:
                caret.setDot(oldPos, caret.getDotBias());
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void replaceText(String str, int start, int end) {
        replaceRange(str, start, end);
    }

    public void setColumns(int columns) {
        toolkit.lockAWT();
        try {
            if (columns < 0) {
                // awt.69=columns less than zero.
                throw new IllegalArgumentException(Messages.getString("awt.69")); //$NON-NLS-1$
            }
            this.columns = columns;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setRows(int rows) {
        toolkit.lockAWT();
        try {
            if (rows < 0) {
                // awt.6A=rows less than zero.
                throw new IllegalArgumentException(Messages.getString("awt.6A")); //$NON-NLS-1$
            }
            this.rows = rows;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    boolean isPrepainter() {
        return true;
    }

    @Override
    Dimension getDefaultMinimumSize() {
        return calcSize(10, 60);
    }

    @Override
    Dimension getDefaultPreferredSize() {
        if (getFont() == null) {
            return null;
        }
        return getDefaultMinimumSize();
    }

    /**
     * Calculates minimum size required for specified
     * number of rows and columns
     */
    private Dimension calcSize(int rows, int columns) {
        FontMetrics fm = getFontMetrics(getFont());
        if ((fm == null) || !isDisplayable()) {
            return null;
        }
        int avWidth = fm.charWidth('_'); // take width of an 'average' character
        return new Dimension(avWidth * columns + 4, (fm.getHeight() + 1) * rows + 4);
    }

    /**
     * Sets scrolling position to point.
     * If no horizontal scrolling is available
     * x scroll coordinate of point is changed to 0
     * before setting the position.
     */
    @Override
    void setViewPosition(Point point) {
        if (noHorizontalScroll()) {
            point.x = 0; // don't allow horizontal scrolling
        }
        super.setViewPosition(point);
    }

    /**
     * Scrolls(updates scroll position) to make rectangle r visible.
     * Also updates necessary scrollbar values.
     * @see TextComponent.scrollRectToVisible(Rectangle)
     */
    @Override
    void scrollRectToVisible(Rectangle r) {
        super.scrollRectToVisible(r);
        // update scrollbar positions:
        if ((hAdjustable != null) && !hAdjustable.getBounds().isEmpty()) {
            hAdjustable.setValue(getViewPosition().x + getInsets().left);
        }
        if ((vAdjustable != null) && !vAdjustable.getBounds().isEmpty()) {
            vAdjustable.setValue(getViewPosition().y + getInsets().top);
        }
    }

    @Override
    AccessibleContext createAccessibleContext() {
        return new AccessibleAWTTextArea();
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

    /**
     * Adds scrolling capabilities to TextArea:
     * initializes all required fields and scrollbar listeners
     */
    private void addScrolling() {
        addAWTComponentListener(stateController);
        addAWTMouseWheelListener(stateController);
        hAdjustable.addAWTAdjustmentListener(stateController);
        vAdjustable.addAWTAdjustmentListener(stateController);
    }

    @Override
    void prepaint(Graphics g) {
        super.prepaint(g);
        Shape oldClip = g.getClip();
        if (vAdjustable != null) {
            Rectangle r = vAdjustable.getBounds();
            g.clipRect(r.x, r.y, r.width, r.height);
            vAdjustable.prepaint(g);
        }
        if (hAdjustable != null) {
            g.setClip(oldClip);
            Rectangle r = hAdjustable.getBounds();
            g.clipRect(r.x, r.y, r.width, r.height);
            hAdjustable.prepaint(g);
        }
        g.setClip(oldClip);
    }

    /**
     * Adds scrollbar area to default insets
     */
    @Override
    Insets getInsets() {
        Insets ins = super.getInsets();
        if (scrollable == null) {
            return ins;
        }
        int sv = getScrollbarVisibility();
        if ((sv == SCROLLBARS_BOTH) || (sv == SCROLLBARS_HORIZONTAL_ONLY)) {
            ins.bottom += scrollable.getAdjustableHeight();
        }
        if ((sv == SCROLLBARS_BOTH) || (sv == SCROLLBARS_VERTICAL_ONLY)) {
            ins.right += scrollable.getAdjustableWidth();
        }
        return ins;
    }

    /**
     * Re-calculates internal layout of TextArea:
     * lays out scrollbars and sets their values
     */
    @Override
    void revalidate() {
        stateController.layoutScrollbars();
        super.revalidate();
    }

    /**
     * Returns the rectangle required to contain all the text, not only
     * visible part(which is component's bounds),
     * relative to TextArea origin
     */
    @Override
    Rectangle getModelRect() {
        Rectangle mRect = super.getModelRect();
        if (noHorizontalScroll()) {
            return mRect;
        }
        int xSpan = (int) rootViewContext.getView().getPreferredSpan(View.X_AXIS);
        
        mRect.width = Math.max(xSpan, mRect.width);
        
        return mRect;
    }

    /**
     * Creates mouse event filter
     * @return mouse event filter for caret
     */
    MouseEventFilter createFilter() {
        filter = new MouseEventFilter((MouseListener) caret, (MouseMotionListener) caret);
        return filter;
    }

    @Override
    MouseMotionListener getMotionHandler() {
        if (filter == null) {
            filter = createFilter();
        }
        return filter;
    }

    @Override
    MouseListener getMouseHandler() {
        if (filter == null) {
            filter = createFilter();
        }
        return filter;
    }

    @Override
    String autoName() {
        return ("text" + toolkit.autoNumber.nextTextArea++); //$NON-NLS-1$
    }
}
