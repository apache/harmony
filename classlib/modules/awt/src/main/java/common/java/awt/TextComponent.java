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
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.text.BreakIterator;
import java.util.EventListener;
import java.util.HashMap;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleText;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.Position.Bias;

import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.state.TextComponentState;
import org.apache.harmony.awt.text.AWTTextAction;
import org.apache.harmony.awt.text.ActionNames;
import org.apache.harmony.awt.text.ActionSet;
import org.apache.harmony.awt.text.RootViewContext;
import org.apache.harmony.awt.text.TextCaret;
import org.apache.harmony.awt.text.TextFactory;
import org.apache.harmony.awt.text.TextKit;

public class TextComponent extends Component implements Accessible {
    private static final long serialVersionUID = -2214773872412987419L;

    /**
     * Maps KeyEvents to keyboard actions
     */
    static class KeyMap {
        private static final HashMap<AWTKeyStroke, Object> actions = new HashMap<AWTKeyStroke, Object>();
        static {
            add(KeyEvent.VK_ENTER, 0, ActionNames.insertBreakAction);
            add(KeyEvent.VK_TAB, 0, ActionNames.insertTabAction);
            add(KeyEvent.VK_DELETE, 0, ActionNames.deleteNextCharAction);
            add(KeyEvent.VK_BACK_SPACE, 0, ActionNames.deletePrevCharAction);
            add(KeyEvent.VK_LEFT, 0, ActionNames.backwardAction);
            add(KeyEvent.VK_RIGHT, 0, ActionNames.forwardAction);
            add(KeyEvent.VK_UP, 0, ActionNames.upAction);
            add(KeyEvent.VK_DOWN, 0, ActionNames.downAction);
            add(KeyEvent.VK_HOME, 0, ActionNames.beginLineAction);
            add(KeyEvent.VK_END, 0, ActionNames.endLineAction);
            add(KeyEvent.VK_PAGE_UP, 0, ActionNames.pageUpAction);
            add(KeyEvent.VK_PAGE_DOWN, 0, ActionNames.pageDownAction);
            add(KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK, ActionNames.nextWordAction);
            add(KeyEvent.VK_LEFT, InputEvent.CTRL_MASK, ActionNames.previousWordAction);
            add(KeyEvent.VK_PAGE_UP, InputEvent.CTRL_MASK, ActionNames.beginAction);
            add(KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_MASK, ActionNames.endAction);
            add(KeyEvent.VK_HOME, InputEvent.CTRL_MASK, ActionNames.beginAction);
            add(KeyEvent.VK_END, InputEvent.CTRL_MASK, ActionNames.endAction);
            add(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK, ActionNames.selectionBackwardAction);
            add(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK, ActionNames.selectionForwardAction);
            add(KeyEvent.VK_UP, InputEvent.SHIFT_MASK, ActionNames.selectionUpAction);
            add(KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK, ActionNames.selectionDownAction);
            add(KeyEvent.VK_HOME, InputEvent.SHIFT_MASK, ActionNames.selectionBeginLineAction);
            add(KeyEvent.VK_END, InputEvent.SHIFT_MASK, ActionNames.selectionEndLineAction);
            add(KeyEvent.VK_PAGE_UP, InputEvent.SHIFT_MASK, ActionNames.selectionPageUpAction);
            add(KeyEvent.VK_PAGE_DOWN, InputEvent.SHIFT_MASK,
                    ActionNames.selectionPageDownAction);
            add(KeyEvent.VK_LEFT, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK,
                    ActionNames.selectionPreviousWordAction);
            add(KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK,
                    ActionNames.selectionNextWordAction);
            add(KeyEvent.VK_PAGE_UP, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK,
                    ActionNames.selectionBeginAction);
            add(KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK,
                    ActionNames.selectionEndAction);
            add(KeyEvent.VK_A, InputEvent.CTRL_MASK, ActionNames.selectAllAction);
            add(KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK, ActionNames.pasteAction);
            add(KeyEvent.VK_V, InputEvent.CTRL_MASK, ActionNames.pasteAction);
            add(KeyEvent.VK_INSERT, InputEvent.CTRL_MASK, ActionNames.copyAction);
            add(KeyEvent.VK_C, InputEvent.CTRL_MASK, ActionNames.copyAction);
            add(KeyEvent.VK_X, InputEvent.CTRL_MASK, ActionNames.cutAction);
        }

        private static void add(int vk, int mask, String actionName) {
            Object action = ActionSet.actionMap.get(actionName);
            actions.put(AWTKeyStroke.getAWTKeyStroke(vk, mask), action);
        }

        static AWTTextAction getAction(KeyEvent e) {
            return (AWTTextAction) actions.get(AWTKeyStroke.getAWTKeyStrokeForEvent(e));
        }
    }

    protected class AccessibleAWTTextComponent extends AccessibleAWTComponent implements
            AccessibleText, TextListener {
        private static final long serialVersionUID = 3631432373506317811L;

        public AccessibleAWTTextComponent() {
            // only add this as listener
            TextComponent.this.addTextListener(this);
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.TEXT;
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet result = super.getAccessibleStateSet();
            if (isEditable()) {
                result.add(AccessibleState.EDITABLE);
            }
            return result;
        }

        @Override
        public AccessibleText getAccessibleText() {
            return this;
        }

        public int getCaretPosition() {
            return TextComponent.this.getCaretPosition();
        }

        public int getCharCount() {
            return document.getLength();
        }

        public int getSelectionEnd() {
            return TextComponent.this.getSelectionEnd();
        }

        public int getSelectionStart() {
            return TextComponent.this.getSelectionStart();
        }

        public int getIndexAtPoint(Point p) {
            return -1;
        }

        public Rectangle getCharacterBounds(int arg0) {
            // TODO: implement
            return null;
        }

        public String getSelectedText() {
            String selText = TextComponent.this.getSelectedText();
            return ("".equals(selText) ? null : selText); //$NON-NLS-1$
        }

        public String getAfterIndex(int part, int index) {
            int offset = 0;
            switch (part) {
                case AccessibleText.CHARACTER:
                    return (index == document.getLength()) ? null : getCharacter(index + 1);
                case AccessibleText.WORD:
                    try {
                        offset = getWordEnd(index) + 1;
                        offset = getWordEnd(offset + 1);
                    } catch (final BadLocationException e) {
                        return null;
                    }
                    return getWord(offset);
                case AccessibleText.SENTENCE:
                    // not implemented yet
                default:
                    return null;
            }
        }

        public String getAtIndex(int part, int index) {
            if (document.getLength() <= 0) {
                return null; // compatibility
            }
            switch (part) {
                case AccessibleText.CHARACTER:
                    return getCharacter(index);
                case AccessibleText.WORD:
                    return getWord(index);
                case AccessibleText.SENTENCE:
                    return getLine(index);
                default:
                    return null;
            }
        }

        public String getBeforeIndex(int part, int index) {
            int offset = 0;
            switch (part) {
                case AccessibleText.CHARACTER:
                    return (index == 0) ? null : getCharacter(index - 1);
                case AccessibleText.WORD:
                    try {
                        offset = getWordStart(index) - 1;
                        offset = getWordStart(offset - 1);
                    } catch (final BadLocationException e) {
                        return null;
                    }
                    return (offset < 0) ? null : getWord(offset);
                case AccessibleText.SENTENCE:
                    BreakIterator bi = BreakIterator.getSentenceInstance();
                    bi.setText(getText());
                    offset = bi.preceding(index);
                    offset = bi.previous() + 1;
                    return (offset < 0) ? null : getLine(offset);
                default:
                    return null;
            }
        }

        public AttributeSet getCharacterAttribute(int arg0) {
            // TODO: implement
            return null;
        }

        public void textValueChanged(TextEvent e) {
            // TODO: implement
        }
    }

    /**
     * Handles key actions and updates document on
     * key typed events
     */
    final class KeyHandler implements KeyListener {
        public void keyPressed(KeyEvent e) {
            performAction(e);
        }

        public void keyReleased(KeyEvent e) {
        }

        public void keyTyped(KeyEvent e) {
            if (insertCharacter(e)) {
                //                repaint();
            } else {
                performAction(e);
            }
        }

        boolean insertCharacter(KeyEvent e) {
            if (!isEditable()) {
                return false;
            }
            char ch = e.getKeyChar();
            if (Character.getType(ch) != Character.CONTROL || ch > 127) {
                getTextKit().replaceSelectedText(Character.toString(ch));
                e.consume();
                return true;
            }
            return false;
        }

        void performAction(KeyEvent e) {
            AWTTextAction action = KeyMap.getAction(e);
            if (action != null) {
                performTextAction(action);
                e.consume();
            }
        }
    }

    /**    
     * Handles Document changes, updates view
     */
    final class DocumentHandler implements DocumentListener {
        private Rectangle getScrollPosition() {
            Rectangle pos = getClient();
            pos.translate(scrollPosition.x, scrollPosition.y);
            return pos;
        }

        public void insertUpdate(DocumentEvent e) {
            if (getBounds().isEmpty()) {
                // update view only when component is "viewable"
                return;
            }
            getRootView().insertUpdate(e, getScrollPosition(), getRootView().getViewFactory());
            updateBidiInfo();
            generateEvent();
        }

        private void updateBidiInfo() {
            // TODO catch BIDI chars here
        }

        public void removeUpdate(DocumentEvent e) {
            if (getBounds().isEmpty()) {
                // update view only when component is "viewable"
                return;
            }
            getRootView().removeUpdate(e, getScrollPosition(), getRootView().getViewFactory());
            generateEvent();
        }

        public void changedUpdate(DocumentEvent e) {
            if (getBounds().isEmpty()) {
                // update view only when component is "viewable"
                return;
            }
            getRootView().changedUpdate(e, getScrollPosition(), getRootView().getViewFactory());
            generateEvent();
        }

        View getRootView() {
            return rootViewContext.getView();
        }
    }

    /**
     * Implements all necessary document/view/caret operations for text handling
     */
    class TextKitImpl implements TextKit, ViewFactory, RootViewContext.ViewFactoryGetter {
        public boolean isEditable() {
            return TextComponent.this.isEditable();
        }

        public void replaceSelectedText(String text) {
            int dot = caret.getDot();
            int mark = caret.getMark();
            try {
                int start = Math.min(dot, mark);
                int length = Math.abs(dot - mark);
                synchronized(TextComponent.this) {
                    document.replace(start, length, text, null);
                }
            } catch (final BadLocationException e) {
            }
        }

        public TextCaret getCaret() {
            return caret;
        }

        public Document getDocument() {
            return document;
        }

        public String getSelectedText() {
            String s = null;
            int dot = caret.getDot();
            int mark = caret.getMark();
            if (dot == mark) {
                return null;
            }
            try {
                s = document.getText(Math.min(dot, mark), Math.abs(dot - mark));
            } catch (final BadLocationException e) {
            }
            return s;
        }

        public int getSelectionStart() {
            return Math.min(caret.getDot(), caret.getMark());
        }

        public int getSelectionEnd() {
            return Math.max(caret.getDot(), caret.getMark());
        }

        public Rectangle getVisibleRect() {
            return new Rectangle(-scrollPosition.x, -scrollPosition.y, w, h);
        }

        public View getRootView() {
            return rootViewContext.getView();
        }

        public Rectangle modelToView(int pos) throws BadLocationException {
            return modelToView(pos, Bias.Forward);
        }

        public Rectangle modelToView(int pos, Bias bias) throws BadLocationException {
            Rectangle mRect = getModelRect();
            if (mRect.isEmpty()) {
                return null;
            }
            Shape shape = getRootView().modelToView(pos, mRect, bias);
            if (shape != null) {
                return shape.getBounds();
            }
            return null;
        }

        public Component getComponent() {
            return TextComponent.this;
        }

        public int viewToModel(Point p, Bias[] biasRet) {
            return getRootView().viewToModel(p.x, p.y, getModelRect(), biasRet);
        }

        public void scrollRectToVisible(Rectangle rect) {
            TextComponent.this.scrollRectToVisible(rect);
        }

        public boolean isScrollBarArea(int x, int y) {
            return false;
        }

        public void addCaretListeners(EventListener listener) {
            // do nothing
        }

        public void paintLayeredHighlights(Graphics g, int p0, int p1, Shape shape, View view) {
            caret.getHighlighter().paintLayeredHighlights(g, p0, p1, shape, view);
        }

        public void revalidate() {
            TextComponent.this.revalidate();
        }

        public Color getDisabledTextColor() {
            return SystemColor.textInactiveText;
        }

        public Color getSelectedTextColor() {
            return SystemColor.textHighlightText;
        }

        public View create(Element element) {
            // do nothing
            return null;
        }

        public ViewFactory getViewFactory() {
            return this;
        }
    }

    class State extends Component.ComponentState implements TextComponentState {
        final Dimension textSize = new Dimension();

        public String getText() {
            return TextComponent.this.getText();
        }

        public Dimension getTextSize() {
            return textSize;
        }

        public void setTextSize(Dimension size) {
            textSize.setSize(size);
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && isEditable();
        }

        public Rectangle getClient() {
            return TextComponent.this.getClient();
        }

        public Insets getInsets() {
            return getNativeInsets();
        }
    }

    protected volatile transient TextListener textListener;

    private final AWTListenerList<TextListener> textListeners = new AWTListenerList<TextListener>(
            this);

    AbstractDocument document;

    final transient TextCaret caret;

    final transient RootViewContext rootViewContext;

    final Point scrollPosition = new Point();

    private boolean editable;

    private final Insets BORDER = new Insets(2, 2, 2, 2);

    private final State state;

    TextComponent() {
        state = new State();
        editable = true;
        dispatchToIM = true; // had been disabled by createBehavior()
        setFont(new Font("DialogInput", Font.PLAIN, 12)); // QUICK FIX //$NON-NLS-1$
        document = new PlainDocument();
        //        text = new StringBuffer();
        setTextKit(new TextKitImpl());
        rootViewContext = createRootViewContext();
        rootViewContext.getView().append(createView());
        rootViewContext.getView().setSize(w, h);
        caret = createCaret();
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        addAWTMouseListener(getMouseHandler());
        addAWTMouseMotionListener(getMotionHandler());
        addAWTFocusListener((FocusListener) caret);
        addAWTKeyListener(new KeyHandler());
        // document handler must be added after caret's listener has been added!
        document.addDocumentListener(new DocumentHandler());
    }

    /**
     * Gets current mouse motion events handler.
     * To be overridden.
     */
    MouseMotionListener getMotionHandler() {
        return (MouseMotionListener) caret;
    }

    /**
     * Gets current mouse events handler.
     * To be overridden.
     */
    MouseListener getMouseHandler() {
        return (MouseListener) caret;
    }

    /**
     * Re-calculates internal layout, repaints component
     */
    void revalidate() {
        // to be overridden by TextArea
        invalidate();
        validate();
        repaint();
    }

    /**
     * Creates and initializes root view associated with this
     * TextComponent, document, text kit.
     * @return this component's root view
     */
    private RootViewContext createRootViewContext() {
        TextFactory factory = TextFactory.getTextFactory();
        RootViewContext c = factory.createRootView(null);
        c.setComponent(this);
        c.setDocument(document);
        c.setViewFactoryGetter((TextKitImpl) getTextKit());
        return c;
    }

    /**
     * Creates default plain view
     */
    View createView() {
        TextFactory factory = TextFactory.getTextFactory();
        View v = factory.createPlainView(document.getDefaultRootElement());
        return v;
    }

    /**     
     * @return new text caret associated with this TextComponent
     */
    TextCaret createCaret() {
        TextFactory factory = TextFactory.getTextFactory();
        TextCaret c = factory.createCaret();
        c.setComponent(this);
        return c;
    }

    @Override
    ComponentBehavior createBehavior() {
        return new HWBehavior(this);
    }

    @Override
    public void addNotify() {
        //        toolkit.lockAWT();
        //        try {
        super.addNotify();
        //        } finally {
        //            toolkit.unlockAWT();
        //        }
        //      ajust caret position if was invalid
        int maxPos = document.getLength();
        if (getCaretPosition() > maxPos) {
            caret.setDot(maxPos, caret.getDotBias());
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

    public String getText() {
        toolkit.lockAWT();
        try {
            return document.getText(0, document.getLength());
        } catch (BadLocationException e) {
            return ""; //$NON-NLS-1$
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    protected String paramString() {
        /* The format is based on 1.5 release behavior 
         * which can be revealed by the following code:
         * System.out.println(new TextField());
         */
        toolkit.lockAWT();
        try {
            return (super.paramString() + ",text=" + getText() //$NON-NLS-1$
                    + (isEditable() ? ",editable" : "") + ",selection=" + getSelectionStart() //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    + "-" + getSelectionEnd()); //$NON-NLS-1$
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void enableInputMethods(boolean enable) {
        toolkit.lockAWT();
        try {
            // TODO: implement
            super.enableInputMethods(enable);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public Color getBackground() {
        toolkit.lockAWT();
        try {
            if (isDisplayable() && !isBackgroundSet()) {
                return (isEditable() ? SystemColor.window : SystemColor.control);
            }
            return super.getBackground();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getCaretPosition() {
        toolkit.lockAWT();
        try {
            return caret.getDot();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public String getSelectedText() {
        toolkit.lockAWT();
        try {
            String s = null;
            try {
                int length = getSelectionEnd() - getSelectionStart();
                s = document.getText(getSelectionStart(), length);
            } catch (final BadLocationException e) {
            }
            return s;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getSelectionEnd() {
        toolkit.lockAWT();
        try {
            return Math.max(caret.getDot(), caret.getMark());
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getSelectionStart() {
        toolkit.lockAWT();
        try {
            return Math.min(caret.getDot(), caret.getMark());
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isEditable() {
        toolkit.lockAWT();
        try {
            return editable;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void removeNotify() {
        toolkit.lockAWT();
        try {
            super.removeNotify();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void select(int selectionStart, int selectionEnd) {
        toolkit.lockAWT();
        try {
            setSelectionEnd(selectionEnd);
            setSelectionStart(selectionStart);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void selectAll() {
        toolkit.lockAWT();
        try {
            select(0, document.getLength());
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void setBackground(Color c) {
        toolkit.lockAWT();
        try {
            super.setBackground(c);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setCaretPosition(int position) {
        toolkit.lockAWT();
        try {
            if (position < 0) {
                // awt.101=position less than zero.
                throw new IllegalArgumentException(Messages.getString("awt.101")); //$NON-NLS-1$
            }
            position = Math.min(document.getLength(), position);
        } finally {
            toolkit.unlockAWT();
        }
        caret.setDot(position, caret.getDotBias());
    }

    public void setEditable(boolean b) {
        toolkit.lockAWT();
        try {
            if (editable != b) {  // to avoid dead loop in repaint()
                editable = b;
                repaint(); // background color changes
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setSelectionEnd(int selectionEnd) {
        //        toolkit.lockAWT();
        //        try {
        selectionEnd = Math.min(selectionEnd, document.getLength());
        int start = getSelectionStart();
        if (selectionEnd < start) {
            caret.setDot(selectionEnd, caret.getDotBias());
        } else {
            caret.setDot(start, caret.getDotBias());
            caret.moveDot(Math.max(start, selectionEnd), caret.getDotBias());
        }
        //        } finally {
        //            toolkit.unlockAWT();
        //        }
    }

    public void setSelectionStart(int selectionStart) {
        //        toolkit.lockAWT();
        //        try {
        selectionStart = Math.max(selectionStart, 0);
        int end = getSelectionEnd();
        if (selectionStart > end) {
            caret.setDot(selectionStart, caret.getDotBias());
        } else {
            caret.setDot(end, caret.getDotBias());
            caret.moveDot(Math.min(end, selectionStart), caret.getDotBias());
        }
        //        } finally {
        //            toolkit.unlockAWT();
        //        }
    }

    public void setText(String text) {
        toolkit.lockAWT();
        try {
            if (text == null) {
                text = ""; //$NON-NLS-1$
            }
        } finally {
            toolkit.unlockAWT();
        }
        try {
            if (isDisplayable()) {
                // reset position to 0 if was displayable
                caret.setDot(0, caret.getDotBias());
            }
            int oldCaretPos = caret.getDot();
            synchronized (this) {
                document.replace(0, document.getLength(), text, null);
            }
            if (!isDisplayable() && (oldCaretPos != caret.getDot())) {
                // return caret back to emulate "no movement"
                caret.setDot(oldCaretPos, caret.getDotBias());
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (TextListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getTextListeners();
        }
        return super.getListeners(listenerType);
    }

    public void addTextListener(TextListener l) {
        textListeners.addUserListener(l);
        // for compatibility only:
        textListener = AWTEventMulticaster.add(textListener, l);
    }

    public void removeTextListener(TextListener l) {
        textListeners.removeUserListener(l);
        // for compatibility only:
        textListener = AWTEventMulticaster.remove(textListener, l);
    }

    public TextListener[] getTextListeners() {
        return textListeners.getUserListeners(new TextListener[0]);
    }

    @Override
    protected void processEvent(AWTEvent e) {
        if (toolkit.eventTypeLookup.getEventMask(e) == AWTEvent.TEXT_EVENT_MASK) {
            processTextEvent((TextEvent) e);
        } else {
            super.processEvent(e);
        }
    }

    protected void processTextEvent(TextEvent e) {
        for (TextListener listener : textListeners.getUserListeners()) {
            switch (e.getID()) {
                case TextEvent.TEXT_VALUE_CHANGED:
                    listener.textValueChanged(e);
                    break;
            }
        }
    }

    /**
     * Calculates and sets new scroll position
     * to make rectangle visible
     * @param r rectangle to make visible by
     * scrolling
     */
    void scrollRectToVisible(final Rectangle r) {
        Point viewPos = getViewPosition();
        Insets ins = getInsets();
        Dimension viewSize = getClient().getSize();
        if ((viewSize.height <= 0) || (viewSize.width <= 0)) {
            return; // FIX
        }
        int dx;
        int dy;
        r.x -= ins.left;
        r.y -= ins.top;
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
            setViewPosition(point);
            repaint();
        }
    }

    /**
     * Sets new scroll position
     * @param point scroll position to set
     */
    void setViewPosition(Point point) {
        scrollPosition.setLocation(-point.x, -point.y);
    }

    /**
     * Gets current scroll position
     */
    Point getViewPosition() {
        return new Point(-scrollPosition.x, -scrollPosition.y);
    }

    Rectangle getClient() {
        Insets insets = getInsets();
        return new Rectangle(insets.left, insets.top, w - insets.right - insets.left, h
                - insets.top - insets.bottom);
    }

    /**
     * Gets the rectangle with height required to hold all text
     * and width equal to component's client width(only visible
     * part of text fits this width)
     */
    Rectangle getModelRect() {
        Rectangle clientRect = getClient();
        clientRect.translate(scrollPosition.x, scrollPosition.y);
        View view = rootViewContext.getView();
        int ySpan = (int) view.getPreferredSpan(View.Y_AXIS);
        clientRect.height = ySpan;
        return clientRect;
    }

    private void generateEvent() {
        postEvent(new TextEvent(this, TextEvent.TEXT_VALUE_CHANGED));
    }

    @Override
    void prepaint(Graphics g) {
        toolkit.theme.drawTextComponentBackground(g, state);
        g.setFont(getFont());
        g.setColor(isEnabled() ? getForeground() : SystemColor.textInactiveText);
        Rectangle r = getModelRect();
        rootViewContext.getView().setSize(r.width, r.height);
        Rectangle client = getClient();
        Shape oldClip = g.getClip();
        g.clipRect(client.x, client.y, client.width, client.height);
        document.readLock();
        try {
            rootViewContext.getView().paint(g, r);
            caret.paint(g);
        } finally {
            document.readUnlock();
        }
       
        g.setClip(oldClip);
    }

    @Override
    Insets getNativeInsets() {
        return (Insets) BORDER.clone();
    }

    @Override
    Insets getInsets() {
        // to be overridden by TextArea
        return getNativeInsets();
    }

    @Override
    AccessibleContext createAccessibleContext() {
        return new AccessibleAWTTextComponent();
    }

    private String getLine(int index) {
        String result = null;
        BreakIterator bi = BreakIterator.getSentenceInstance();
        bi.setText(getText());
        int end = bi.following(index);
        int start = bi.preceding(end - 1);
        try {
            result = document.getText(start, end - start);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String getWord(int index) {
        int start = 0;
        int length = 0;
        String result = null;
        try {
            start = getWordStart(index);
            length = getWordEnd(index) - start;
            result = document.getText(start, length);
        } catch (final BadLocationException e) {
        }
        return result;
    }

    private String getCharacter(int index) {
        String s = null;
        try {
            s = document.getText(index, 1);
        } catch (final BadLocationException e) {
        }
        return s;
    }

    private int getWordEnd(final int pos) throws BadLocationException {
        BreakIterator bi = BreakIterator.getWordInstance();
        int length = document.getLength();
        if (pos < 0 || pos > length) {
            // awt.2B=No word at {0}
            throwException(Messages.getString("awt.2B", pos), pos); //$NON-NLS-1$
        }
        String content = document.getText(0, length);
        bi.setText(content);
        return (pos < bi.last()) ? bi.following(pos) : pos;
    }

    private int getWordStart(final int pos) throws BadLocationException {
        BreakIterator bi = BreakIterator.getWordInstance();
        int length = document.getLength();
        if (pos < 0 || pos > length) {
            // awt.2B=No word at {0}
            throwException(Messages.getString("awt.2B", pos), pos); //$NON-NLS-1$
        }
        String content = null;
        content = document.getText(0, length);
        bi.setText(content);
        int iteratorWordStart = pos;
        if (pos < length - 1) {
            iteratorWordStart = bi.preceding(pos + 1);
        } else {
            bi.last();
            iteratorWordStart = bi.previous();
        }
        return iteratorWordStart;
    }

    private static void throwException(final String s, final int i) throws BadLocationException {
        throw new BadLocationException(s, i);
    }

    @Override
    void setEnabledImpl(boolean value) {
        if (value != isEnabled()) { // to avoid dead loop in repaint()
            super.setEnabledImpl(value);
            if (isShowing()) {
                repaint();
            }
        }
    }

    @Override
    void postprocessEvent(AWTEvent e, long eventMask) {
        // have to call system listeners without AWT lock
        // to avoid deadlocks in code common with UI text
        if (eventMask == AWTEvent.FOCUS_EVENT_MASK) {
            preprocessFocusEvent((FocusEvent) e);
        } else if (eventMask == AWTEvent.KEY_EVENT_MASK) {
            preprocessKeyEvent((KeyEvent) e);
        } else if (eventMask == AWTEvent.MOUSE_EVENT_MASK) {
            preprocessMouseEvent((MouseEvent) e);
        } else if (eventMask == AWTEvent.MOUSE_MOTION_EVENT_MASK) {
            preprocessMouseMotionEvent((MouseEvent) e);
        } else {
            super.postprocessEvent(e, eventMask);
        }
    }

    void performTextAction(AWTTextAction action) {
        action.performAction(getTextKit());
    }
}
