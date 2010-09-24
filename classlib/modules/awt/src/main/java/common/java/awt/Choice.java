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

import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

import org.apache.harmony.awt.ButtonStateController;
import org.apache.harmony.awt.ChoiceStyle;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.state.ChoiceState;

public class Choice extends Component implements ItemSelectable, Accessible {
    private static final long serialVersionUID = -4075310674757313071L;

    private static final int BORDER_SIZE = 2;

    static final Insets INSETS = new Insets(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE);

    final ChoiceStyle popupStyle;
    
    int selectedIndex = -1;

    private final AWTListenerList<ItemListener> itemListeners = new AWTListenerList<ItemListener>(
            this);

    private final java.util.List<String> items = new ArrayList<String>();

    private final State state;

    private final ChoiceStateController stateController;

    protected class AccessibleAWTChoice extends Component.AccessibleAWTComponent implements
            AccessibleAction {
        private static final long serialVersionUID = 7175603582428509322L;

        public AccessibleAWTChoice() {
            // default constructor is public
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.COMBO_BOX;
        }

        @Override
        public AccessibleAction getAccessibleAction() {
            return this;
        }

        public int getAccessibleActionCount() {
            return 0;
        }

        public boolean doAccessibleAction(int i) {
            return false;
        }

        public String getAccessibleActionDescription(int i) {
            return null;
        }

    }

    class State extends Component.ComponentState implements ChoiceState {

        final Dimension textSize = new Dimension();

        public boolean isPressed() {
            return stateController.isPressed();
        }

        public String getText() {
            return getSelectedItem();
        }

        public Dimension getTextSize() {
            return textSize;
        }

        public void setTextSize(Dimension size) {
            textSize.setSize(size);
        }

        public Rectangle getButtonBounds() {
            return new Rectangle(w - h + 2, 2, h - 4, h - 4);
        }

        public Rectangle getTextBounds() {
            return new Rectangle(4, 3, w - h - 5, h - 6);
        }

    }

    class ChoiceStateController extends ButtonStateController implements MouseWheelListener {

        /**
         * popup window containing list of items
         */
        private final ChoicePopupBox popup;

        public ChoiceStateController() {
            super(Choice.this);
            popup = createPopup();
        }

        protected ChoicePopupBox createPopup() {
            return new ChoicePopupBox(Choice.this);
        }

        @Override
        public void focusLost(FocusEvent fe) {
            super.focusLost(fe);
            popup.hide();
        }

        @Override
        public void keyPressed(KeyEvent e) {
            boolean alt = e.isAltDown();
            int blockSize = ChoicePopupBox.PAGE_SIZE - 1;
            int count = getItemCount();
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                    if (alt) {
                        popup();
                        break;
                    }
                case KeyEvent.VK_LEFT:
                    changeSelection(-1);
                    break;
                case KeyEvent.VK_DOWN:
                    if (alt) {
                        popup();
                        break;
                    }
                case KeyEvent.VK_RIGHT:
                    changeSelection(1);
                    break;
                case KeyEvent.VK_PAGE_UP:
                    changeSelection(-blockSize);
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    changeSelection(blockSize);
                    break;
                case KeyEvent.VK_HOME:
                    changeSelection(-getSelectedIndex());
                    break;
                case KeyEvent.VK_END:
                    changeSelection(count - getSelectedIndex());
                    break;
            }
        }

        /**
         * Moves selection up(incr &lt; 0) or down(incr &gt; 0)
         * 
         * @param incr distance from the current item in items
         */
        void changeSelection(int incr) {
            int newSel = getValidIndex(getSelectedIndex() + incr);
            if (popup.isVisible()) {
                popup.changeSelection(incr);
                newSel = popup.selectedItem;
            }
            selectAndFire(newSel);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            // don't call super here as in keyPressed
        }

        /**
         * Called on mouse release
         */
        @Override
        protected void fireEvent() {
            popup();
        }

        /**
         * Shows popup list if it's not visible, hides otherwise
         */
        private void popup() {
            if (!popup.isVisible()) {
                popup.show();
            } else {
                popup.hide();
            }
        }

        @Override
        public void mousePressed(MouseEvent me) {
            super.mousePressed(me);
            // TODO: show/hide popup here
            // if (me.getButton() != MouseEvent.BUTTON1) {
            // return;
            // }
            // if (me.isPopupTrigger()) {
            // popup();
            // }
        }

        public void mouseWheelMoved(MouseWheelEvent e) {
            changeSelection(e.getUnitsToScroll());
        }

    }

    /**
     * Creates customized choice
     * 
     * @param choiceStyle style of custom choice: defines custom list popup
     *        window location and size
     */
    Choice(ChoiceStyle choiceStyle) {
        super();
        state = new State();
        popupStyle = choiceStyle;
        stateController = new ChoiceStateController();

        addAWTMouseListener(stateController);
        addAWTKeyListener(stateController);
        addAWTFocusListener(stateController);
        addAWTMouseWheelListener(stateController);
    }

    public Choice() throws HeadlessException {
        this(new ChoiceStyle() {

            public int getPopupX(int x, int width, int choiceWidth, int screenWidth) {
                return x;
            }

            public int getPopupWidth(int choiceWidth) {
                return choiceWidth;
            }

        });
        toolkit.lockAWT();
        try {

        } finally {
            toolkit.unlockAWT();
        }
    }

    public void add(String item) {
        toolkit.lockAWT();
        try {
            if (item == null) {
                // awt.103=item is null
                throw new NullPointerException(Messages.getString("awt.103")); //$NON-NLS-1$
            }
            if (items.size() == 0) {
                selectedIndex = 0;
            }
            items.add(items.size(), item);

        } finally {
            toolkit.unlockAWT();
        }
    }

    public void remove(String item) {
        toolkit.lockAWT();
        try {
            int index = items.indexOf(item);
            if (index == -1) {
                // awt.104=item doesn't exist in the choice menu
                throw new IllegalArgumentException(Messages.getString("awt.104")); //$NON-NLS-1$
            }
            if (selectedIndex == index) {
                selectedIndex = 0;
            }
            items.remove(item);
            if (selectedIndex > index) {
                selectedIndex--;
            }

        } finally {
            toolkit.unlockAWT();
        }
    }

    public void remove(int position) {
        toolkit.lockAWT();
        try {
            if (selectedIndex == position) {
                selectedIndex = 0;
            }
            items.remove(position);

            if (selectedIndex > position) {
                selectedIndex--;
            }
            if (items.size() == 0) {
                selectedIndex = -1;
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void removeAll() {
        toolkit.lockAWT();
        try {
            items.clear();
            selectedIndex = -1;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void insert(String item, int index) {
        if (index < 0) {
            // awt.105=index less than zero
            throw new IllegalArgumentException(Messages.getString("awt.105")); //$NON-NLS-1$
        }
        
        if (item == null) {
            // awt.105='item' parameter is null
            throw new NullPointerException(Messages.getString("awt.01", "item")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        toolkit.lockAWT();
        try {
            int idx = Math.min(items.size(), index);
            if (items.size() == 0) {
                selectedIndex = 0;
            }
            items.add(idx, item);
            if (idx <= selectedIndex) {
                selectedIndex = 0;
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void addNotify() {
        toolkit.lockAWT();
        try {
            super.addNotify();
            setSize(getMinimumSize());
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
    protected String paramString() {
        /*
         * The format is based on 1.5 release behavior which can be revealed by
         * the following code: System.out.println(new Choice());
         */

        toolkit.lockAWT();
        try {
            return (super.paramString() + ",current=" + getSelectedItem()); //$NON-NLS-1$
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Object[] getSelectedObjects() {
        toolkit.lockAWT();
        try {
            if (items.size() > 0) {
                return new Object[] { items.get(selectedIndex) };
            }
            return null;
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

    public void addItem(String item) {
        toolkit.lockAWT();
        try {
            add(item);
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public int countItems() {
        toolkit.lockAWT();
        try {
            return getItemCount();
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

    public int getSelectedIndex() {
        toolkit.lockAWT();
        try {
            return selectedIndex;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public String getSelectedItem() {
        toolkit.lockAWT();
        try {
            if (selectedIndex < 0) {
                return null;
            }
            return items.get(selectedIndex);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void select(int pos) {
        toolkit.lockAWT();
        try {
            if (selectedIndex != pos) { // to avoid dead loop in repaint()
                if (pos >= items.size() || pos < 0) {
                    // awt.106=specified position is greater than the number of items
                    throw new IllegalArgumentException(
                            Messages.getString("awt.106")); //$NON-NLS-1$
                }
                selectedIndex = pos;
                repaint();
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void select(String str) {
        toolkit.lockAWT();
        try {
            int idx = items.indexOf(str);
            if (idx >= 0) {
                select(idx);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (ItemListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getItemListeners();
        }
        return super.getListeners(listenerType);
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
        if (toolkit.eventTypeLookup.getEventMask(e) == AWTEvent.ITEM_EVENT_MASK) {
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

    @Override
    ComponentBehavior createBehavior() {
        return new HWBehavior(this);
    }

    @Override
    String autoName() {
        return ("choice" + toolkit.autoNumber.nextChoice++); //$NON-NLS-1$
    }

    /**
     * Widest list item must fit into Choice minimum size
     */
    @Override
    Dimension getDefaultMinimumSize() {
        Dimension minSize = new Dimension();
        if (!isDisplayable()) {
            return minSize;
        }
        int hGap = 2 * BORDER_SIZE;
        int vGap = hGap;
        Font font = getFont();
        FontMetrics fm = getFontMetrics(font);
        minSize.height = fm.getHeight() + vGap + 1;
        minSize.width = hGap + 16; // TODO: use arrow button size
        Graphics2D gr = (Graphics2D) getGraphics();
        FontRenderContext frc = gr.getFontRenderContext();
        int maxItemWidth = 5; // TODO: take width of some char
        for (int i = 0; i < items.size(); i++) {
            String item = getItem(i);
            int itemWidth = font.getStringBounds(item, frc).getBounds().width;
            if (itemWidth > maxItemWidth) {
                maxItemWidth = itemWidth;
            }
        }
        minSize.width += maxItemWidth;
        gr.dispose();
        return minSize;
    }

    @Override
    boolean isPrepainter() {
        return true;
    }

    @Override
    void prepaint(Graphics g) {
        toolkit.theme.drawChoice(g, state);
    }

    @Override
    void setFontImpl(Font f) {
        super.setFontImpl(f);
        setSize(getWidth(), getDefaultMinimumSize().height);
    }

    @SuppressWarnings("deprecation")
    int getItemHeight() {
        FontMetrics fm = toolkit.getFontMetrics(getFont());
        int itemHeight = fm.getHeight() + 2;
        return itemHeight;
    }

    void fireItemEvent() {
        postEvent(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, getSelectedItem(),
                ItemEvent.SELECTED));
    }

    /**
     * This method is necessary because <code> public select(int) </code> should
     * not fire any events
     */
    void selectAndFire(int idx) {
        if (idx == getSelectedIndex()) {
            return;
        }
        select(idx);
        fireItemEvent();
    }

    /**
     * Gets the nearest valid index
     * 
     * @param idx any integer value
     * @return valid item index nearest to idx
     */
    int getValidIndex(int idx) {
        return Math.min(getItemCount() - 1, Math.max(0, idx));
    }

    @Override
    AccessibleContext createAccessibleContext() {
        return new AccessibleAWTChoice();
    }
}
