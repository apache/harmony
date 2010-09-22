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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.EventListener;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.View;

import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.text.AWTTextAction;
import org.apache.harmony.awt.text.ActionNames;
import org.apache.harmony.awt.text.ActionSet;
import org.apache.harmony.awt.text.PropertyNames;
import org.apache.harmony.awt.text.TextFactory;
import org.apache.harmony.awt.text.TextFieldKit;

public class TextField extends TextComponent {
    protected class AccessibleAWTTextField extends AccessibleAWTTextComponent {
        private static final long serialVersionUID = 6219164359235943158L;

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            final AccessibleStateSet set = super.getAccessibleStateSet();
            set.add(AccessibleState.SINGLE_LINE);
            return set;
        }
    }

    /**
     * Implementation of text field specific text operations
     */
    final class TextFieldKitImpl implements TextFieldKit {
        /**
         * used in horizontal text scrolling
         */
        BoundedRangeModel boundedRangeModel;

        public int getHorizontalAlignment() {
            return 10; // LEADING
        }

        /**
         * Gets current bounded range model.
         * Creates default bounded range model if necessary
         * and adds listener to update current horizontal scroll
         * position.
         */
        public BoundedRangeModel getHorizontalVisibility() {
            if (boundedRangeModel == null) {
                final int prefWidth = (int) rootViewContext.getView().getPreferredSpan(View.X_AXIS);
                final int value = getMaxScrollOffset();
                final int max = Math.max(prefWidth, value);
                boundedRangeModel = new DefaultBoundedRangeModel(value, max - value, 0, max);
                boundedRangeModel.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        scrollPosition.x = -boundedRangeModel.getValue();
                    }
                });
            }
            return boundedRangeModel;
        }

        public boolean echoCharIsSet() {
            return TextField.this.echoChar != 0;
        }

        public char getEchoChar() {
            return TextField.this.getEchoChar();
        }

        public Insets getInsets() {
            return TextField.this.getNativeInsets();
        }
    }

    private static final long serialVersionUID = -2966288784432217853L;

    private final AWTListenerList<ActionListener> actionListeners = new AWTListenerList<ActionListener>(
            this);

    private int columns;

    private char echoChar;

    public TextField(String text) throws HeadlessException {
        this(text, (text != null ? text.length() : 0));
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public TextField(int columns) throws HeadlessException {
        this(new String(), columns);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public TextField() throws HeadlessException {
        this(new String(), 0);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public TextField(String text, int columns) throws HeadlessException {
        super();
        toolkit.lockAWT();
        try {
            Toolkit.checkHeadless();
            setTextFieldKit(new TextFieldKitImpl());
            this.columns = Math.max(0, columns);
            addAWTKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if ((e.getKeyCode() == KeyEvent.VK_ENTER) && !e.isAltDown()
                            && !e.isControlDown()) {
                        generateActionEvent(e.getWhen(), e.getModifiers());
                    }
                }
            });
        } finally {
            toolkit.unlockAWT();
        }
        setText(text);
    }

    @Override
    public void addNotify() {
        document.putProperty(PropertyNames.FILTER_NEW_LINES, Boolean.TRUE);
        setText(getText()); // remove all new lines in already existing text
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

    public Dimension getMinimumSize(int cols) {
        toolkit.lockAWT();
        try {
            return minimumSize(cols);
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

    public Dimension getPreferredSize(int columns) {
        toolkit.lockAWT();
        try {
            return preferredSize(columns);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public Dimension minimumSize(int columns) {
        toolkit.lockAWT();
        try {
            final Dimension minSize = calcSize(columns);
            if (minSize == null) {
                return super.minimumSize();
            }
            return minSize;
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * Calculates minimum size required for <code>cols</code> columns
     */
    private Dimension calcSize(int cols) {
        final FontMetrics fm = getFontMetrics(getFont());
        if ((fm == null) || !isDisplayable()) {
            return null;
        }
        return new Dimension(fm.charWidth('_') * cols + 6, fm.getHeight() + 6);
    }

    @Deprecated
    @Override
    public Dimension minimumSize() {
        toolkit.lockAWT();
        try {
            if ((columns > 0)) {
                return minimumSize(columns);
            }
            return super.minimumSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    protected String paramString() {
        /* The format is based on 1.5 release behavior
         * which can be revealed by the following code:
         * 
         * TextField tf = new TextField();
         * tf.setEchoChar('q');
         * System.out.println(tf);
         */
        toolkit.lockAWT();
        try {
            String paramStr = super.paramString();
            if (echoCharIsSet()) {
                paramStr += ",echo=" + getEchoChar(); //$NON-NLS-1$
            }
            return paramStr;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public Dimension preferredSize(int columns) {
        toolkit.lockAWT();
        try {
            final Dimension prefSize = calcSize(columns);
            if (prefSize == null) {
                return super.preferredSize();
            }
            return prefSize;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    @Override
    public Dimension preferredSize() {
        toolkit.lockAWT();
        try {
            if (columns > 0) {
                return preferredSize(columns);
            }
            return super.preferredSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setColumns(int columns) {
        toolkit.lockAWT();
        try {
            if (columns < 0) {
                // awt.102=columns less than zero.
                throw new IllegalArgumentException(Messages.getString("awt.102")); //$NON-NLS-1$
            }
            this.columns = columns;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void setText(String text) {
        super.setText(text); // no AWT lock here!
    }

    public boolean echoCharIsSet() {
        toolkit.lockAWT();
        try {
            return (echoChar != 0);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public char getEchoChar() {
        toolkit.lockAWT();
        try {
            return echoChar;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setEchoChar(char ch) {
        toolkit.lockAWT();
        try {
            if (echoChar == ch) {
                return;
            }
            echoChar = ch;
        } finally {
            toolkit.unlockAWT();
        }
        repaint();
    }

    @Deprecated
    public void setEchoCharacter(char ch) {
        setEchoChar(ch);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (ActionListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getActionListeners();
        }
        return super.getListeners(listenerType);
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

    @Override
    protected void processEvent(AWTEvent e) {
        if (toolkit.eventTypeLookup.getEventMask(e) == AWTEvent.ACTION_EVENT_MASK) {
            processActionEvent((ActionEvent) e);
        } else {
            super.processEvent(e);
        }
    }

    protected void processActionEvent(ActionEvent e) {
        for (final ActionListener listener : actionListeners.getUserListeners()) {
            switch (e.getID()) {
                case ActionEvent.ACTION_PERFORMED:
                    listener.actionPerformed(e);
                    break;
            }
        }
    }

    @Override
    boolean isPrepainter() {
        return true;
    }

    /**
     * Creates password view instead of default plain view.
     * Necessary to be able to set echo character.
     */
    @Override
    View createView() {
        final TextFactory factory = TextFactory.getTextFactory();
        final View v = factory.createPasswordView(document.getDefaultRootElement());
        return v;
    }

    @Override
    Dimension getDefaultMinimumSize() {
        return calcSize(getText().length());
    }

    @Override
    Dimension getDefaultPreferredSize() {
        if (getFont() == null) {
            return null;
        }
        return getDefaultMinimumSize();
    }

    private void generateActionEvent(long when, int modifiers) {
        postEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getText(), when,
                modifiers));
    }

    /**
     * Horizontally scrolls text to make specified rectangle
     * visible. Uses bounded range model value for
     * scrolling. Repaints TextField.
     */
    @Override
    void scrollRectToVisible(Rectangle r) {
        final int x = r.x;
        final Insets insets = getTextFieldKit().getInsets();
        final BoundedRangeModel brm = getTextFieldKit().getHorizontalVisibility();
        final int oldValue = brm.getValue();
        final int width = getModelRect().width;
        if (x > width - insets.right) {
            final int newVal = oldValue + (x - width + insets.right) + 2;
            brm.setValue(newVal);
            repaint();
        }
        if (x < insets.left) {
            brm.setValue(oldValue - (insets.left - x) - 2);
            repaint();
        }
    }

    /**
     * Returns just the same rectangle as getClient().
     * Bounded range model takes care of actual text size.
     */
    @Override
    Rectangle getModelRect() {
        return getClient();
    }

    /**
     * Calculates maximum horizontal scroll value as
     * difference between actual text size and text field component
     * client area size.
     */
    final int getMaxScrollOffset() {
        final Insets ins = getNativeInsets();
        final int prefSpanX = (int) rootViewContext.getView().getPreferredSpan(View.X_AXIS);
        final int prefWidth = prefSpanX + ins.left + ins.right;
        final int width = getWidth();
        final int diff = prefWidth - width;
        return (diff >= 0) ? diff + 1 : 0;
    }

    @Override
    AccessibleContext createAccessibleContext() {
        return new AccessibleAWTTextField();
    }

    @Override
    String autoName() {
        return ("textfield" + toolkit.autoNumber.nextTextField++); //$NON-NLS-1$
    }

    /**
     * Handles text actions.
     * Ignores new line insertion into text.
     */
    @Override
    void performTextAction(AWTTextAction action) {
        if (action != ActionSet.actionMap.get(ActionNames.insertBreakAction)) {
            super.performTextAction(action);
        }
    }
}
