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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.util.EventListener;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Locale;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import org.apache.harmony.awt.ScrollStateController;
import org.apache.harmony.awt.Scrollable;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.state.ListState;

public class List extends Component implements ItemSelectable, Accessible {
    private static final long serialVersionUID = -3304312411574666869L;

    private final static int BORDER_SIZE = 2;
    
    private final static Font DEFAULT_FONT = new Font("dialog", Font.PLAIN, 12); //$NON-NLS-1$

    private final AWTListenerList<ActionListener> actionListeners = new AWTListenerList<ActionListener>(
            this);

    private final AWTListenerList<ItemListener> itemListeners = new AWTListenerList<ItemListener>(
            this);

    private int rows;

    private boolean multipleMode;

    private final ArrayList<String> items = new ArrayList<String>();

    private final ArrayList<Integer> selection = new ArrayList<Integer>();

    private int visibleIndex = -1;

    private int currentIndex; // "focused" item index

    private final ListStateController stateController;

    private final ListScrollable scrollable;

    private final State state;

    private final ScrollPaneAdjustable hAdjustable;

    private final ScrollPaneAdjustable vAdjustable;

    private transient Point scrollLocation;

    private transient int prefWidth;

    protected class AccessibleAWTList extends AccessibleAWTComponent implements
            AccessibleSelection, ItemListener, ActionListener {
        private static final long serialVersionUID = 7924617370136012829L;

        protected class AccessibleAWTListChild extends AccessibleAWTComponent implements
                Accessible {
            private static final long serialVersionUID = 4412022926028300317L;

            private final int accessibleIndexInParent;

            private List parent;

            public AccessibleAWTListChild(List parent, int indexInParent) {
                accessibleIndexInParent = indexInParent;
                this.parent = parent;
                setAccessibleParent(parent);
            }

            public AccessibleContext getAccessibleContext() {
                return this;
            }

            @Override
            public void addFocusListener(FocusListener l) {
                // do nothing
            }

            @Override
            public boolean contains(Point p) {
                return false;
            }

            @Override
            public Accessible getAccessibleAt(Point p) {
                return null;
            }

            @Override
            public Accessible getAccessibleChild(int i) {
                return null;
            }

            @Override
            public int getAccessibleChildrenCount() {
                return 0;
            }

            @Override
            public int getAccessibleIndexInParent() {
                return accessibleIndexInParent;
            }

            @Override
            public AccessibleRole getAccessibleRole() {
                return AccessibleRole.LIST_ITEM;
            }

            @Override
            public AccessibleStateSet getAccessibleStateSet() {
                AccessibleStateSet aStateSet = super.getAccessibleStateSet();
                if (isAccessibleChildSelected(accessibleIndexInParent)) {
                    aStateSet.add(AccessibleState.SELECTED);
                }
                return aStateSet;
            }

            @Override
            public Color getBackground() {
                return parent.getBackground();
            }

            @Override
            public Rectangle getBounds() {
                // return null
                return null;
            }

            @Override
            public Cursor getCursor() {
                return parent.getCursor();
            }

            @Override
            public Font getFont() {
                return parent.getFont();
            }

            @Override
            public FontMetrics getFontMetrics(Font f) {
                return parent.getFontMetrics(f);
            }

            @Override
            public Color getForeground() {
                return parent.getForeground();
            }

            @Override
            public Locale getLocale() throws IllegalComponentStateException {
                return parent.getLocale();
            }

            @Override
            public Point getLocation() {
                // just return null
                return null;
            }

            @Override
            public Point getLocationOnScreen() {
                // just return null
                return null;
            }

            @Override
            public Dimension getSize() {
                // just return null
                return null;
            }

            @Override
            public boolean isEnabled() {
                return parent.isEnabled();
            }

            @Override
            public boolean isFocusTraversable() {
                return false;
            }

            @Override
            public boolean isShowing() {
                // always invisible
                return false;
            }

            @Override
            public boolean isVisible() {
                // always invisible
                return false;
            }

            @Override
            public void removeFocusListener(FocusListener l) {
                // do nothing
            }

            @Override
            public void requestFocus() {
                // do nothing
            }

            @Override
            public void setBackground(Color color) {
                parent.setBackground(color);
            }

            @Override
            public void setBounds(Rectangle r) {
                // do nothing
            }

            @Override
            public void setCursor(Cursor cursor) {
                parent.setCursor(cursor);
            }

            @Override
            public void setEnabled(boolean enabled) {
                parent.setEnabled(enabled);
            }

            @Override
            public void setFont(Font f) {
                parent.setFont(f);
            }

            @Override
            public void setForeground(Color color) {
                parent.setForeground(color);
            }

            @Override
            public void setLocation(Point p) {
                // do nothing
            }

            @Override
            public void setSize(Dimension size) {
                // do nothing
            }

            @Override
            public void setVisible(boolean visible) {
                parent.setVisible(visible);
            }
        }

        public AccessibleAWTList() {
            addActionListener(this);
            addItemListener(this);
        }

        public int getAccessibleSelectionCount() {
            return getSelectedIndexes().length;
        }

        public void clearAccessibleSelection() {
            toolkit.lockAWT();
            try {
                int[] selection = getSelectedIndexes();
                int count = selection.length;
                for (int i = 0; i < count; i++) {
                    deselect(selection[i]);
                }
            } finally {
                toolkit.unlockAWT();
            }
        }

        public void selectAllAccessibleSelection() {
            toolkit.lockAWT();
            try {
                int count = getItemCount();
                if (!isMultipleMode()) {
                    count = Math.min(1, count);
                }
                for (int i = 0; i < count; i++) {
                    select(i);
                }
            } finally {
                toolkit.unlockAWT();
            }
        }

        public void addAccessibleSelection(int i) {
            select(i);
        }

        public void removeAccessibleSelection(int i) {
            deselect(i);
        }

        public boolean isAccessibleChildSelected(int i) {
            return isIndexSelected(i);
        }

        public Accessible getAccessibleSelection(int i) {
            toolkit.lockAWT();
            try {
                int[] selection = getSelectedIndexes();
                if ((i < 0) || (i >= selection.length)) {
                    return null;
                }
                return new AccessibleAWTListChild(List.this, selection[i]);
            } finally {
                toolkit.unlockAWT();
            }
        }

        @Override
        public AccessibleSelection getAccessibleSelection() {
            return this;
        }

        public void itemStateChanged(ItemEvent e) {
            // TODO: find out why listen to ItemEvents
        }

        public void actionPerformed(ActionEvent e) {
            // TODO: find out why listen to ActionEvents
        }

        @Override
        public Accessible getAccessibleAt(Point p) {
            // no specific implementation yet
            return super.getAccessibleAt(p);
        }

        @Override
        public Accessible getAccessibleChild(int i) {
            toolkit.lockAWT();
            try {
                int idx = Math.max(0, i);
                if (!isIdxValid(idx)) {
                    return null;
                }
                return new AccessibleAWTListChild(List.this, idx);
            } finally {
                toolkit.unlockAWT();
            }
        }

        @Override
        public int getAccessibleChildrenCount() {
            return getItemCount();
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.LIST;
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            toolkit.lockAWT();
            try {
                AccessibleStateSet aStateSet = super.getAccessibleStateSet();
                if (isMultipleMode()) {
                    aStateSet.add(AccessibleState.MULTISELECTABLE);
                }
                return aStateSet;
            } finally {
                toolkit.unlockAWT();
            }
        }
    }

    class ListStateController extends ScrollStateController implements MouseListener,
            KeyListener, FocusListener, MouseMotionListener {
        boolean scrollPressed;

        public ListStateController(Scrollable scrollable) {
            super(scrollable);
        }

        public void mouseClicked(MouseEvent e) {
            if (getClient().contains(e.getPoint()) && e.getClickCount() == 2) {
                fireActionEvent(e.getWhen(), e.getModifiers());
            }
        }

        public void mouseEntered(MouseEvent e) {
            // nothing to do
        }

        public void mouseExited(MouseEvent e) {
            // nothing to do
        }

        public void mousePressed(MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) {
                return;
            }
            Point pt = e.getPoint();
            if (getClient().contains(pt)) {
                if (e.getClickCount() > 1) {
                    // don't select/deselect on
                    // double-click
                    return;
                }
                int idx = getItemIndex(e.getY());
                if (checkIdx(idx)) {
                    requestFocus();
                    boolean selected = isIndexSelected(idx);
                    int sel = selected ? ItemEvent.DESELECTED : ItemEvent.SELECTED;
                    if (!selected) {
                        select(idx);
                    } else {
                        deselect(idx);
                    }
                    fireItemEvent(sel);
                }
            } else if (vAdjustable.getBounds().contains(pt)
                    || hAdjustable.getBounds().contains(pt)) {
                scrollPressed = true;
            }
        }

        public void mouseReleased(MouseEvent e) {
            scrollPressed = false;
        }

        public void keyPressed(KeyEvent e) {
            // awt.72=Key event for unfocused component
            assert isFocusOwner() : Messages.getString("awt.72"); //$NON-NLS-1$
            int keyCode = e.getKeyCode();
            switch (keyCode) {
                case KeyEvent.VK_UP:
                    scrollByUnit(vAdjustable, -1);
                    break;
                case KeyEvent.VK_LEFT:
                    scrollByUnit(hAdjustable, -1);
                    break;
                case KeyEvent.VK_DOWN:
                    scrollByUnit(vAdjustable, 1);
                    break;
                case KeyEvent.VK_RIGHT:
                    scrollByUnit(hAdjustable, 1);
                    break;
                case KeyEvent.VK_PAGE_UP:
                    scrollByBlock(-1);
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    scrollByBlock(1);
                    break;
                case KeyEvent.VK_HOME:
                    selectVisible(0);
                    break;
                case KeyEvent.VK_END:
                    int lastIdx = getItemCount() - 1;
                    selectVisible(lastIdx);
                    break;
                case KeyEvent.VK_ENTER:
                    fireActionEvent(e.getWhen(), e.getModifiers());
                    break;
                case KeyEvent.VK_SPACE:
                    if (isMultipleMode()) {
                        boolean deselect = isIndexSelected(currentIndex);
                        if (deselect) {
                            deselect(currentIndex);
                        } else {
                            select(currentIndex);
                        }
                        fireItemEvent(deselect ? ItemEvent.DESELECTED : ItemEvent.SELECTED);
                    }
                    break;
            }
        }

        private void selectVisible(int idx) {
            int count = getItemCount();
            if (count <= 0) {
                return;
            }
            idx = Math.max(0, Math.min(count - 1, idx));
            makeVisible(idx);
            if (!isMultipleMode()) {
                select(idx);
                fireItemEvent(ItemEvent.SELECTED);
            }
        }

        private void scrollByBlock(int val) {
            int itemsPerBlock = getClient().height / getItemSize().height;
            scrollByUnit(vAdjustable, val * itemsPerBlock);
        }

        private void scrollByUnit(Adjustable adj, int val) {
            if (adj == vAdjustable) {
                int visIdx = getCurrentIndex() + val;
                selectVisible(visIdx);
            } else if (adj == hAdjustable) {
                updateAdjValue(adj, val * adj.getUnitIncrement());
            }
        }

        private void updateAdjValue(Adjustable adj, final int increment) {
            int oldVal = adj.getValue();
            adj.setValue(oldVal + increment);
        }

        public void keyReleased(KeyEvent e) {
            // nothing to do
        }

        public void keyTyped(KeyEvent e) {
            // nothing to do
        }

        public void fireActionEvent(long when, int mod) {
            if (!checkIdx(currentIndex)) {
                return;
            }
            postEvent(new ActionEvent(List.this, ActionEvent.ACTION_PERFORMED,
                    getItem(currentIndex), when, mod));
        }

        public void fireItemEvent(int sel) {
            postEvent(new ItemEvent(List.this, ItemEvent.ITEM_STATE_CHANGED,
                    getItem(currentIndex), sel));
        }

        public void focusGained(FocusEvent e) {
            doRepaint(getMinRect(currentIndex, currentIndex));
        }

        public void focusLost(FocusEvent e) {
            doRepaint(getMinRect(currentIndex, currentIndex));
        }

        public void mouseDragged(MouseEvent e) {
            if (scrollPressed) {
                return;
            }
            int y = e.getY();
            int idx = getItemIndex(y);
            if (checkIdx(idx)) {
                selectVisible(idx);
            }
        }

        private boolean checkIdx(int idx) {
            return (idx >= 0) && (idx < getItemCount());
        }

        public void mouseMoved(MouseEvent e) {
            // nothing to do
        }
    }

    class ListScrollable implements Scrollable {
        public Adjustable getVAdjustable() {
            return vAdjustable;
        }

        public Adjustable getHAdjustable() {
            return hAdjustable;
        }

        public Insets getInsets() {
            return List.this.getInsets();
        }

        public Point getLocation() {
            return new Point(scrollLocation);
        }

        public void setLocation(Point p) {
            if (scrollLocation == null) {
                scrollLocation = new Point();
            }
            scrollLocation.setLocation(p);
        }

        public Component getComponent() {
            return List.this;
        }

        public void doRepaint() {
            List.this.doRepaint();
        }

        public int getAdjustableWidth() {
            return vAdjustable.getBounds().width;
        }

        public int getAdjustableHeight() {
            return hAdjustable.getBounds().height;
        }

        public void setAdjustableSizes(Adjustable adj, int vis, int min, int max) {
            ((ScrollPaneAdjustable) adj).setSizes(vis, min, max);
        }

        public int getAdjustableMode(Adjustable adj) {
            return AS_NEEDED;
        }

        public void setAdjustableBounds(Adjustable adj, Rectangle r) {
            ((ScrollPaneAdjustable) adj).setBounds(r);
        }

        public Dimension getSize() {
            Dimension prefSize = getPreferredSize(getItemCount());
            // don't include borders:
            // (return pure content size)
            prefSize.width -= 2 * BORDER_SIZE;
            prefSize.height -= 2 * BORDER_SIZE;
            return prefSize;
        }

        public int getWidth() {
            return List.this.getWidth();
        }

        public int getHeight() {
            return List.this.getHeight();
        }

        public void doRepaint(Rectangle r) {
            List.this.doRepaint(r);
        }
    }

    class State extends ComponentState implements ListState {
        public Rectangle getItemBounds(int idx) {
            return List.this.getItemBounds(idx);
        }

        public Rectangle getClient() {
            return List.this.getClient();
        }

        public int getItemCount() {
            return List.this.getItemCount();
        }

        public boolean isSelected(int idx) {
            return List.this.isSelected(idx);
        }

        public String getItem(int idx) {
            return List.this.getItem(idx);
        }

        public int getCurrentIndex() {
            return List.this.getCurrentIndex();
        }
    }

    public List(int rows) throws HeadlessException {
        this(rows, false);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public List() throws HeadlessException {
        this(0, false);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public List(int rows, boolean multipleMode) throws HeadlessException {
        toolkit.lockAWT();
        try {
            Toolkit.checkHeadless();
            this.rows = ((rows != 0) ? rows : 4);
            this.multipleMode = multipleMode;
            scrollLocation = new Point();
            hAdjustable = new ScrollPaneAdjustable(this, Adjustable.HORIZONTAL);
            vAdjustable = new ScrollPaneAdjustable(this, Adjustable.VERTICAL);
            scrollable = new ListScrollable();
            stateController = new ListStateController(scrollable);
            state = new State();
            addAWTMouseListener(stateController);
            addAWTMouseMotionListener(stateController);
            addAWTKeyListener(stateController);
            addAWTFocusListener(stateController);
            addAWTComponentListener(stateController);
            addAWTMouseWheelListener(stateController);
            hAdjustable.addAdjustmentListener(stateController);
            vAdjustable.addAdjustmentListener(stateController);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void add(String item) {
        toolkit.lockAWT();
        try {
            add(item, -1);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void add(String item, int index) {
        toolkit.lockAWT();
        try {
            String str = (item != null ? item : ""); //$NON-NLS-1$
            int pos = index;
            int size = items.size();
            if ((pos < 0) || (pos > size)) {
                pos = size;
            }
            items.add(pos, str);
            updatePrefWidth();
            doRepaint();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void remove(String item) {
        toolkit.lockAWT();
        try {
            int index = items.indexOf(item);
            if (index < 0) {
                // awt.73=no such item
                throw new IllegalArgumentException(Messages.getString("awt.73")); //$NON-NLS-1$
            }
            remove(index);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void remove(int position) {
        toolkit.lockAWT();
        try {
            selection.remove(new Integer(position));
            items.remove(position);
            // decrease all selected indices greater than position
            // by 1, because items list is shifted to the left
            for (int i = 0; i < selection.size(); i++) {
                Integer idx = selection.get(i);
                int val = idx.intValue();
                if (val > position) {
                    selection.set(i, new Integer(val - 1));
                }
            }
            updatePrefWidth();
            doRepaint();
        } catch (IndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException(e.getMessage());
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void clear() {
        toolkit.lockAWT();
        try {
            removeAll();
        } finally {
            toolkit.unlockAWT();
        }
        return;
    }

    public void removeAll() {
        toolkit.lockAWT();
        try {
            items.clear();
            selection.clear();
            currentIndex = 0;
            scrollable.setLocation(new Point());
            doRepaint();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    protected String paramString() {
        toolkit.lockAWT();
        try {
            return (super.paramString() + ",selected=" + getSelectedItem()); //$NON-NLS-1$
        } finally {
            toolkit.unlockAWT();
        }
    }

    public String getItem(int index) {
        toolkit.lockAWT();
        try {
            return items.get(index);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void addNotify() {
        toolkit.lockAWT();
        try {
            super.addNotify();
            updatePrefWidth();
            updateIncrements();
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
    public Dimension getMinimumSize() {
        toolkit.lockAWT();
        try {
            return minimumSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension getMinimumSize(int rows) {
        toolkit.lockAWT();
        try {
            if (!isDisplayable()) {
                return new Dimension();
            }
            Graphics gr = getGraphics();
            Dimension charSize = getMaxCharSize(gr);
            int minRowHeight = charSize.height + 1;
            final int MIN_CHARS_IN_ROW = 12;
            int hGap = 2 * BORDER_SIZE;
            int vGap = hGap;
            int minWidth = charSize.width * MIN_CHARS_IN_ROW + hGap;
            gr.dispose();
            return new Dimension(minWidth, rows * minRowHeight + vGap);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension getPreferredSize(int rows) {
        toolkit.lockAWT();
        try {
            Dimension minSize = getMinimumSize(rows);
            if (items.isEmpty()) {
                return minSize;
            }
            if (!isDisplayable()) {
                return new Dimension();
            }
            int maxItemWidth = minSize.width;
            Graphics2D gr = (Graphics2D) getGraphics();
            FontRenderContext frc = gr.getFontRenderContext();
            Font font = getFont();
            for (int i = 0; i < items.size(); i++) {
                String item = getItem(i);
                int itemWidth = font.getStringBounds(item, frc).getBounds().width;
                if (itemWidth > maxItemWidth) {
                    maxItemWidth = itemWidth;
                }
            }
            gr.dispose();
            return new Dimension(maxItemWidth, minSize.height);
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

    @Deprecated
    @Override
    public Dimension minimumSize() {
        toolkit.lockAWT();
        try {
            if (isMinimumSizeSet()) {
                return super.minimumSize();
            }
            return getMinimumSize(getRows());
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public Dimension minimumSize(int rows) {
        toolkit.lockAWT();
        try {
            return getMinimumSize(rows);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public Dimension preferredSize(int rows) {
        toolkit.lockAWT();
        try {
            return getPreferredSize(getRows());
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    @Override
    public Dimension preferredSize() {
        toolkit.lockAWT();
        try {
            if (isPreferredSizeSet()) {
                return super.preferredSize();
            }
            return getPreferredSize(getRows());
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void removeNotify() {
        toolkit.lockAWT();
        try {
            super.removeNotify();
            updatePrefWidth();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void addItem(String item) {
        toolkit.lockAWT();
        try {
            add(item);
        } finally {
            toolkit.unlockAWT();
        }
        return;
    }

    @Deprecated
    public void addItem(String item, int index) {
        toolkit.lockAWT();
        try {
            add(item, index);
        } finally {
            toolkit.unlockAWT();
        }
        return;
    }

    @Deprecated
    public boolean allowsMultipleSelections() {
        toolkit.lockAWT();
        try {
            return isMultipleMode();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public int countItems() {
        toolkit.lockAWT();
        try {
            return getItemCount();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void delItem(int position) {
        toolkit.lockAWT();
        try {
            remove(position);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void delItems(int start, int end) {
        toolkit.lockAWT();
        try {
            for (int i = start; i <= end; i++) {
                remove(start);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void deselect(int index) {
        toolkit.lockAWT();
        try {
            int oldFocusedIdx = currentIndex;
            currentIndex = index;
            selection.remove(new Integer(index));
            // repaint only deselected item's Rectangle &
            // previously "focused" item's Rectangle
            doRepaint(getMinRect(index, oldFocusedIdx));
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getItemCount() {
        toolkit.lockAWT();
        try {
            return items.size();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public String[] getItems() {
        toolkit.lockAWT();
        try {
            String[] itemsArr = new String[items.size()];
            items.toArray(itemsArr);
            return itemsArr;
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

    public int getSelectedIndex() {
        toolkit.lockAWT();
        try {
            int selectedIndex = -1;
            if (selection.size() == 1) {
                selectedIndex = selection.get(0).intValue();
            }
            return selectedIndex;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int[] getSelectedIndexes() {
        toolkit.lockAWT();
        try {
            Integer[] selArr = new Integer[selection.size()];
            selection.toArray(selArr);
            int[] intArr = new int[selArr.length];
            for (int i = 0; i < selArr.length; i++) {
                intArr[i] = selArr[i].intValue();
            }
            return intArr;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public String getSelectedItem() {
        toolkit.lockAWT();
        try {
            int selectedIndex = getSelectedIndex();
            if (selectedIndex < 0) {
                return null;
            }
            return items.get(selectedIndex);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public String[] getSelectedItems() {
        toolkit.lockAWT();
        try {
            int size = selection.size();
            String[] selItemsArr = new String[size];
            Integer[] selArr = new Integer[size];
            selection.toArray(selArr);
            for (int i = 0; i < selItemsArr.length; i++) {
                selItemsArr[i] = items.get(selArr[i].intValue());
            }
            return selItemsArr;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Object[] getSelectedObjects() {
        toolkit.lockAWT();
        try {
            return getSelectedItems();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getVisibleIndex() {
        toolkit.lockAWT();
        try {
            return visibleIndex;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isIndexSelected(int position) {
        toolkit.lockAWT();
        try {
            return selection.contains(new Integer(position));
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isMultipleMode() {
        toolkit.lockAWT();
        try {
            return multipleMode;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public boolean isSelected(int pos) {
        toolkit.lockAWT();
        try {
            return isIndexSelected(pos);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void makeVisible(int index) {
        toolkit.lockAWT();
        try {
            visibleIndex = index;
            if (!isDisplayable() || !isIdxValid(index)) {
                return;
            }
            // scroll if necessary
            Rectangle itemRect = getItemBounds(index);
            Rectangle clientRect = getClient();
            int dy = itemRect.y - clientRect.y;
            if (dy < 0) {
                stateController.updateAdjValue(vAdjustable, dy);
            }
            dy = itemRect.y + itemRect.height - (clientRect.y + clientRect.height);
            if (dy > 0) {
                stateController.updateAdjValue(vAdjustable, dy);
            }
            doRepaint(getMinRect(currentIndex, visibleIndex));
            currentIndex = visibleIndex;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void replaceItem(String newValue, int index) {
        toolkit.lockAWT();
        try {
            items.set(index, newValue);
            updatePrefWidth();
            doRepaint();
        } catch (IndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException(e.getMessage());
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void select(int index) {
        toolkit.lockAWT();
        try {
            // if an item was already selected -
            // do nothing
            if (isIndexSelected(index)) {
                return;
            }
            if (!multipleMode && (selection.size() == 1)) {
                deselect(getSelectedIndex());
            }
            selection.add(new Integer(index));
            // repaint only item's rectangle & old "focused" item's rect
            doRepaint(getMinRect(index, currentIndex));
            currentIndex = index;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setMultipleMode(boolean multipleMode) {
        toolkit.lockAWT();
        try {
            if (!multipleMode && (this.multipleMode != multipleMode)) {
                selection.clear();
                int curIdx = getCurrentIndex();
                if (curIdx >= 0) {
                    selection.add(new Integer(curIdx));
                }
            }
            this.multipleMode = multipleMode;
            doRepaint();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void setMultipleSelections(boolean mm) {
        toolkit.lockAWT();
        try {
            setMultipleMode(mm);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (ActionListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getActionListeners();
        } else if (ItemListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getItemListeners();
        } else {
            return super.getListeners(listenerType);
        }
    }

    public void addActionListener(ActionListener l) {
        actionListeners.addUserListener(l);
    }

    public void removeActionListener(ActionListener l) {
        actionListeners.removeUserListener(l);
    }

    public ActionListener[] getActionListeners() {
        return actionListeners.getUserListeners(new ActionListener[0]);
    }

    public void addItemListener(ItemListener l) {
        itemListeners.addUserListener(l);
    }

    public void removeItemListener(ItemListener l) {
        itemListeners.removeUserListener(l);
    }

    public ItemListener[] getItemListeners() {
        return itemListeners.getUserListeners(new ItemListener[0]);
    }

    @Override
    protected void processEvent(AWTEvent e) {
        long eventMask = toolkit.eventTypeLookup.getEventMask(e);
        if (eventMask == AWTEvent.ACTION_EVENT_MASK) {
            processActionEvent((ActionEvent) e);
        } else if (eventMask == AWTEvent.ITEM_EVENT_MASK) {
            processItemEvent((ItemEvent) e);
        } else {
            super.processEvent(e);
        }
    }

    protected void processItemEvent(ItemEvent e) {
        for (Iterator<?> i = itemListeners.getUserIterator(); i.hasNext();) {
            ItemListener listener = (ItemListener) i.next();
            switch (e.getID()) {
                case ItemEvent.ITEM_STATE_CHANGED:
                    listener.itemStateChanged(e);
                    break;
            }
        }
    }

    protected void processActionEvent(ActionEvent e) {
        for (Iterator<?> i = actionListeners.getUserIterator(); i.hasNext();) {
            ActionListener listener = (ActionListener) i.next();
            switch (e.getID()) {
                case ActionEvent.ACTION_PERFORMED:
                    listener.actionPerformed(e);
                    break;
            }
        }
    }

    @Override
    String autoName() {
        return ("list" + toolkit.autoNumber.nextList++); //$NON-NLS-1$
    }

    Dimension getMinimumSizeImpl() {
        return getMinimumSize(getRows());
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
        toolkit.theme.drawList(g, state, false);
        vAdjustable.prepaint(g);
        hAdjustable.prepaint(g);
    }

    private Dimension getMaxCharSize(Graphics g) {
        final FontRenderContext frc = ((Graphics2D) g).getFontRenderContext();
        return getListFont().getStringBounds("W", frc).getBounds().getSize(); //$NON-NLS-1$
    }
    
    private Font getListFont() {
        final Font f = getFont();
        return f == null ? DEFAULT_FONT : f;
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
    void setFontImpl(Font f) {
        super.setFontImpl(f);
        if (isDisplayable()) {
            invalidate();
            updateIncrements();
        }
    }

    private Rectangle getItemBounds(int pos) {
        Dimension itemSize = getItemSize();
        Point p = new Point(BORDER_SIZE, pos * itemSize.height + BORDER_SIZE);
        Rectangle itemRect = new Rectangle(p, itemSize);
        itemRect.translate(scrollLocation.x, scrollLocation.y);
        return itemRect;
    }

    @SuppressWarnings("deprecation")
    private Dimension getItemSize() {
        FontMetrics fm = toolkit.getFontMetrics(getListFont());
        int itemHeight = fm.getHeight() + 2;
        return new Dimension(prefWidth - 2 * BORDER_SIZE, itemHeight);
    }

    private int getItemIndex(int y) {
        return (y - BORDER_SIZE - scrollLocation.y) / getItemSize().height;
    }

    private int getCurrentIndex() {
        return (isDisplayable() ? currentIndex : -1);
    }

    private Rectangle getClient() {
        Insets ins = getNativeInsets();
        return new Rectangle(ins.left, ins.top, getWidth() - (ins.left + ins.right),
                getHeight() - (ins.top + ins.bottom));
    }

    @Override
    Insets getInsets() {
        if (!isDisplayable()) {
            return super.getInsets();
        }
        Insets insets = new Insets(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE);
        return insets;
    }

    @Override
    Insets getNativeInsets() {
        Insets insets = getInsets();
        insets.bottom += hAdjustable.getBounds().height;
        insets.right += vAdjustable.getBounds().width;
        return insets;
    }

    private void updatePrefWidth() {
        Dimension prefSize = getPreferredSize(1);
        prefWidth = prefSize.width;
    }

    private void updateIncrements() {
        Dimension itemSize = getItemSize();
        Dimension clientSize = getClient().getSize();
        vAdjustable.setUnitIncrement(itemSize.height);
        vAdjustable.setBlockIncrement(clientSize.height);
        Graphics gr = getGraphics();
        hAdjustable.setUnitIncrement(getMaxCharSize(gr).width);
        hAdjustable.setBlockIncrement(clientSize.width);
        gr.dispose();
    }

    private boolean isIdxValid(int index) {
        return ((index >= 0) && (index < getItemCount()));
    }

    private Rectangle getMinRect(int idx1, int idx2) {
        if (!isDisplayable()) {
            return null;
        }
        Rectangle r1 = getItemBounds(idx1);
        Rectangle r2 = getItemBounds(idx2);
        Rectangle minRect = r1.union(r2);
        minRect.width = getClient().width - minRect.x + 1;
        minRect.grow(1, 1);
        return minRect;
    }

    @Override
    AccessibleContext createAccessibleContext() {
        return new AccessibleAWTList();
    }
}
