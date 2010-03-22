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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.text;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.im.InputContext;
import java.awt.im.InputMethodRequests;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleEditableText;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleText;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.TextUI;
import javax.swing.plaf.basic.BasicTextUI;

import org.apache.harmony.awt.ComponentInternals;
import org.apache.harmony.awt.text.AWTHighlighter;
import org.apache.harmony.awt.text.ComposedTextParams;
import org.apache.harmony.awt.text.InputMethodListenerImpl;
import org.apache.harmony.awt.text.InputMethodRequestsImpl;
import org.apache.harmony.awt.text.PropertyNames;
import org.apache.harmony.awt.text.TextCaret;
import org.apache.harmony.awt.text.TextKit;
import org.apache.harmony.awt.text.TextUtils;
import org.apache.harmony.x.swing.StringConstants;

import org.apache.harmony.x.swing.internal.nls.Messages;


public abstract class JTextComponent extends JComponent implements Scrollable,
        Accessible {

    public class AccessibleJTextComponent extends
            JComponent.AccessibleJComponent implements AccessibleText,
            CaretListener, DocumentListener, AccessibleAction,
            AccessibleEditableText {

        //current caretPosition, it is changed only by method caretUpdate
        private int caretPosition;

        /**
         * If new document is installed to text component, accessible should to
         * remove itself (as DocumentListener) from old document, and add to
         * new document.
         */
        private class DocumentPropertyChangeListener implements
               PropertyChangeListener {
            public void propertyChange(final PropertyChangeEvent e) {
                Object oldValue = e.getOldValue();
                Object newValue = e.getNewValue();
                if (oldValue != null) {
                    ((Document) oldValue).removeDocumentListener(
                         (AccessibleJTextComponent) getAccessibleContext());
                }
                if (e.getNewValue() != null) {
                    ((Document) newValue).addDocumentListener(
                        (AccessibleJTextComponent) getAccessibleContext());
                }
            }
        }

        /**
         * Adds PropertyChangeListener to text component. If possible,
         * adds document listener. Initializes variable, which watch the current
         * caret position.
         */
        public AccessibleJTextComponent() {
            addCaretListener(this);
            JTextComponent.this.addPropertyChangeListener(
                StringConstants.TEXT_COMPONENT_DOCUMENT_PROPERTY,
                new DocumentPropertyChangeListener());
            if (document != null) {
                document.addDocumentListener(this);
            }
            if (caret != null) {
                caretPosition = getCaretPosition();
            }
            caretPosition = 0;
        }

        /**
         * If current document is instanceof Plain document, it does nothing.
         */
        public void setAttributes(final int i1, final int i2,
                                  final AttributeSet as) {
            checkPositions(i1, i2, document.getLength());
            if (document instanceof DefaultStyledDocument) {
                ((DefaultStyledDocument) document).setCharacterAttributes(i1,
                        i2 - i1, as, true);
            }
        }

        private void checkPositions(final int i1, final int i2,
                                   final int length) {
            if (i1 < 0 || i2 < i1 || i2 > length) {
                throw new IllegalArgumentException(Messages.getString("swing.90",i1, i2)); //$NON-NLS-1$
            }
        }

        public AttributeSet getCharacterAttribute(final int offset) {
            Element elem = document.getDefaultRootElement();
            while (elem.getElementCount() > 0) {
                elem = elem.getElement(elem.getElementIndex(offset));
            }
            return elem.getAttributes();
        }

        public void removeUpdate(final DocumentEvent e) {
            documentUpdate(e);
        }

        private void documentUpdate(final DocumentEvent e) {
            firePropertyChange(AccessibleContext.ACCESSIBLE_TEXT_PROPERTY,
                    null, new Integer(e.getOffset()));
        }

        public void insertUpdate(final DocumentEvent e) {
            documentUpdate(e);
        }

        public void changedUpdate(final DocumentEvent e) {
            documentUpdate(e);
        }

        /**
         * If e.getDot() equals to e.getMark, there is only one
         * PropertyChangeEvent.
         */
        public void caretUpdate(final CaretEvent e) {
            int dot = e.getDot();
            int mark = e.getMark();
            firePropertyChange(AccessibleContext.ACCESSIBLE_CARET_PROPERTY,
                               new Integer(caretPosition), new Integer(dot));
            if (dot != mark) {
                firePropertyChange(AccessibleContext.
                                   ACCESSIBLE_SELECTION_PROPERTY, null,
                                   getSelectedText());
            }
        }

        public AccessibleText getAccessibleText() {
            return this;
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet result = super.getAccessibleStateSet();
            if (isEditable) {
                result.add(AccessibleState.EDITABLE);
            }
            return result;
        }

        /**
         * Returns  AccessibleRole.TEXT.
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.TEXT;
        }

        public AccessibleEditableText getAccessibleEditableText() {
            return this;
        }

        public AccessibleAction getAccessibleAction() {
            return this;
        }

        public void setTextContents(final String s) {
            JTextComponent.this.setText(s);
        }

        public void insertTextAtIndex(final int i, final String s) {
            try {
                document.insertString(i, s, null);
            } catch (final BadLocationException e) {
            }
        }

        public void replaceText(final int i1, final int i2, final String s) {
            checkPositions(i1, i2, document.getLength());
            try {
                replaceString(i1, i2 - i1, s, null);
            } catch (final BadLocationException e) {
            }
        }

        public String getTextRange(final int p0, final int p1) {
            String result = null;
            try {
                result = JTextComponent.this.getText(p0, p1 - p0);
            } catch (final BadLocationException e) {
            }
            return result;
        }

        public String getBeforeIndex(final int part, final int index) {
            int offset = 0;
            switch (part) {
            case AccessibleText.CHARACTER:
                return (index == 0) ? null : getCharacter(index - 1);
            case AccessibleText.WORD:
                try {
                    offset = TextUtils.getWordStart(textKit, index) - 1;
                } catch (final BadLocationException e) {
                    return null;
                }
                return (offset < 0) ? null : getWord(offset);
            case AccessibleText.SENTENCE:
                Element elem = TextUtils.getParagraphElement(document, index);
                offset = elem.getStartOffset() - 1;
                return (offset < 0) ? null : getLine(offset);
            default:
                return null;
            }
        }

        public String getAtIndex(final int part, final int index) {
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

        /**
         * Returns char by index.
         */
        private String getCharacter(final int index) {
            String s = null;
            try {
                s = document.getText(index, 1);
            } catch (final BadLocationException e) {
            }
            return s;
        }

        /**
         * Returns word by index.
         */
        private String getWord(final int index) {
            int start = 0;
            int length = 0;
            String result = null;
            try {
                start = TextUtils.getWordStart(textKit, index);
                length = TextUtils.getWordEnd(textKit, index) - start;
                result = document.getText(start, length);
            } catch (final BadLocationException e) {
            }

            return result;
        }

        /**
         * Returns line by index.
         */
        private String getLine(final int index) {
            Element elem = TextUtils.getParagraphElement(document, index);
            if (elem == null) {
                return null;
            }
            int start = elem.getStartOffset();
            int length = elem.getEndOffset() - start;
            String result = null;
            try {
                result = document.getText(start, length);
            } catch (final BadLocationException e) {
            }
            return result;
        }

        public String getAfterIndex(final int part, final int index) {
            int offset = 0;
            switch (part) {
            case AccessibleText.CHARACTER:
                return (index == document.getLength()) ? null
                        : getCharacter(index + 1);
            case AccessibleText.WORD:
                try {
                    offset = TextUtils.getWordEnd(textKit, index)
                        + 1;
                } catch (final BadLocationException e) {
                    return null;
                }

                return getWord(offset);
            case AccessibleText.SENTENCE:
                Element elem = TextUtils.getParagraphElement(document, index);
                offset = elem.getEndOffset() + 1;
                return (offset < 0) ? null : getLine(offset);
            default:
                return null;
            }
        }

        public String getAccessibleActionDescription(final int index) {
            return (String) getActions()[index].getValue(Action.NAME);
        }

        public String getSelectedText() {
            return JTextComponent.this.getSelectedText();
        }

        public Rectangle getCharacterBounds(final int i) {
            Rectangle forward = null;
            Rectangle backward = null;
            try {
                forward = ((BasicTextUI) ui).modelToView(JTextComponent.this, i,
                        Position.Bias.Forward);
                backward = ((BasicTextUI) ui).modelToView(JTextComponent.this,
                        Math.min(document.getLength(), i + 1),
                        Position.Bias.Backward);
            } catch (final BadLocationException e) {
            }
            if (forward == null || backward == null) {
                return null;
            }
            forward.width = Math.abs(forward.x - backward.x) + 1;
            forward.x = Math.min(forward.x, backward.x);
            return forward;
        }

        public int getIndexAtPoint(final Point p) {
            return JTextComponent.this.viewToModel(p);
        }

        public void selectText(final int p0, final int p1) {
            JTextComponent.this.select(p0, p1);
        }

        public void delete(final int i1, final int i2) {
            checkPositions(i1, i2, document.getLength());
            try {
                document.remove(i1, i2 - i1);
            } catch (final BadLocationException e) {
            }
        }

        /**
         * This operations performs by setCaretPosition and moveCaretPosition
         * methods of JTextComponent.
         */
        public void cut(final int p0, final int p1) {
            setCaretPosition(p0);
            moveCaretPosition(Math.max(p1, p0));
            JTextComponent.this.cut();
        }

        public boolean doAccessibleAction(final int index) {
            getActions()[index].actionPerformed(null);
            return true;
        }

        public void paste(final int p) {
            setCaretPosition(p);
            JTextComponent.this.paste();
        }

        public int getSelectionStart() {
            return JTextComponent.this.getSelectionStart();
        }

        public int getSelectionEnd() {
            return JTextComponent.this.getSelectionEnd();
        }

        public int getCharCount() {
            return document.getLength();
        }

        public int getCaretPosition() {
            return JTextComponent.this.getCaretPosition();
        }

        public int getAccessibleActionCount() {
            return getActions().length;
        }

    }

    public static final String FOCUS_ACCELERATOR_KEY = "focusAcceleratorKey";
    /**
     * Name of default keymap, which is parent to keymap installed by UI.
     * Default keymap has not key binding. It has only default action
     * (DefaultEditorKit.DefaultKeyTypedAction)
     */
    public static final String DEFAULT_KEYMAP = "default";

    private static final Object DEFAULT_ACTION_KEY = new Object();  //$NON-LOCK-1$
    // last focused text component
    private static JTextComponent focusedComponent;
    // class to store all keymaps,which were added to JTextComponent
    private static HashMap keymaps;
    // current caret
    private Caret caret;
    // curent highlighter
    private transient Highlighter highlighter;
    // defines, whether drag is enabled
    private boolean isDragEnabled;
    // defines, whether component is editable
    private boolean isEditable;
    // current caret color
    private Color caretColor;
    // current disabled text color
    private Color disabledTextColor;
    // current selected text color
    private Color selectedTextColor;
    // current selection color
    private Color selectionColor;
    // current margin
    private Insets margin;
    // current model
    private Document document;
    // current navigation filter
    private transient NavigationFilter navigationFilter;
    // current focus accelerator
    private char focusAccelerator = '\0';
    // current keymap
    private transient Keymap currentKeyMap;
    // current component Orientation
    private boolean componentOrientation;
    // ChangeCaretListener, which was added to current caret (an removed from
    // old caret)
    private final CaretListenerImpl changeCaretListener =
        new CaretListenerImpl();

    // TransferHandler class used by cut, copy, paste methods, DnD support.
    private final transient TransferHandler transferHandler =
        new TransferHandler(
            null);
    // InputMethodListsner to receive InputMethodEvent and make appropriate
    // document insert
    private transient InputMethodListenerImpl inputMethodListener;
    // InputMethodRequest for method getInputRequest
    private transient InputMethodRequests inputMethodRequests;
    // CaretEvent realization, used by ChangeCaretListener
    private final transient CaretEventImpl caretEvent =
        new CaretEventImpl();

    private TextKitImpl textKit = new TextKitImpl();

    /**
     * Initializes default keymap. Adds default keymap to JTextComponent
     * Sets to null variable, that indicate last focused component
     */
    static {
        keymaps = new HashMap();
        addKeymap(DEFAULT_KEYMAP, null);
        getKeymap(DEFAULT_KEYMAP).setDefaultAction(
                new DefaultEditorKit.DefaultKeyTypedAction());
        focusedComponent = null;
    }

    public static class KeyBinding {
        public KeyStroke key;
        public String actionName;
        public KeyBinding(final KeyStroke k, final String s) {
            key = k;
            actionName = s;
        }
    }

    /**
     *    Keymap interface implementation to store keymap, which was added to
     * JTextComponent. There are used two vectors: actions and keystrokes.
     * actions.get(i) is action for keystroke.get(i), for any i between 0 and
     * actions.size().
     *    Also this class provides possibility to create ActionMap and InputMap
     * by this KeyMap (see method setKeymap).
     */
    private static class KeyMap implements Keymap {
        private final String name;
        private Keymap parent;

        KeyMap(final String s, final Keymap k) {
            name = s;
            parent = k;
        }
        Action defaultAction;
        // vector to store actions
        ArrayList actions = new ArrayList();
        // vector to store keystrokes
        ArrayList keystrokes = new ArrayList();

        public void addActionForKeyStroke(final KeyStroke key, final Action a) {
            actions.add(0, a);
            keystrokes.add(0, key);
        }

        public Action getAction(final KeyStroke key) {
            int index = keystrokes.lastIndexOf(key);
            if (index >= 0) {
                return (Action) actions.get(index);
            }
            return (parent != null) ? parent.getAction(key) : null;
        }

        public Action[] getBoundActions() {
            return (Action[])actions.toArray(new Action[actions.size()]);
        }

        public KeyStroke[] getBoundKeyStrokes() {
            return (KeyStroke[])keystrokes.toArray(new KeyStroke[keystrokes
                    .size()]);
        }

        public Action getDefaultAction() {
            return (defaultAction == null && parent != null) ? parent
                    .getDefaultAction() : defaultAction;
        }

        public KeyStroke[] getKeyStrokesForAction(final Action a) {
            ArrayList keys = new ArrayList();
            int size = actions.size();
            int i = actions.indexOf(a);
            while (i >= 0) {
                keys.add(0, keystrokes.get(i++));
                if (i == size) {
                    break;
                }
                int tmpIndex = actions.subList(i, size).indexOf(a);
                i = (tmpIndex >= 0) ? tmpIndex + i : -1;
            }
            return keys.size() == 0 ? null : (KeyStroke[])keys
                    .toArray(new KeyStroke[keys.size()]);
        }

        public String getName() {
            return name;
        }

        public Keymap getResolveParent() {
            return parent;
        }

        public boolean isLocallyDefined(final KeyStroke key) {
            return keystrokes.contains(key);
        }

        public void removeBindings() {
            actions.clear();
            keystrokes.clear();
        }

        public void removeKeyStrokeBinding(final KeyStroke key) {
            for (int index = keystrokes.lastIndexOf(key); index >= 0;
                 index = keystrokes.lastIndexOf(key)) {

                keystrokes.remove(index);
                actions.remove(index);
            }
        }

        public void setDefaultAction(final Action a) {
            defaultAction = a;
        }

        public void setResolveParent(final Keymap k) {
            parent = k;
        }

        /*
         * The format of the string is based on 1.5 release behavior
         * which can be revealed using the following code:
         *
         *     KeyStroke keyStrokeX = KeyStroke.getKeyStroke(KeyEvent.VK_X,
         *                                                   InputEvent.CTRL_MASK);
         *     JTextArea jtc  = new JTextArea();
         *     jtc.getKeymap().addActionForKeyStroke(keyStrokeX, new TextAction("x") {
         *         public void actionPerformed(ActionEvent e) {
         *         }
         *         public String toString() {
         *             return "Text action";
         *         }
         *     });
         *     System.out.println(jtc.getKeymap());
         */
        public String toString() {
            String s = "Keymap[" + getName() + "]{";
            for (int i = 0; i < keystrokes.size(); i++) {
                if (i > 0) {
                    s += ", ";
                }
                s += keystrokes.get(i) + "=" + actions.get(i);
            }
            s += "}";
            return s;
        }

    }

    public static Keymap addKeymap(final String s, final Keymap parent) {
        KeyMap keyMap = new KeyMap(s, parent);
        Object key = s == null ? keyMap : (Object) s;
        keymaps.put(key, keyMap);
        return keyMap;
    }

    /**
     * This method used by TextAction (TextAction.getLastFocusedTextComponent)
     * to get last focused component
     * @return last focused text component
     */
    static final JTextComponent getLastFocusedTextComponent() {
        return focusedComponent;
    }

    public static Keymap getKeymap(final String name) {
        Object result = keymaps.get(name);
        return result == null ? null : (Keymap)result;
    }

    public static void loadKeymap(final Keymap keymap,
            final JTextComponent.KeyBinding[] keys, final Action[] actions) {
        for (int i = 0; i < keys.length; i++) {
            int index = -1;
            for (int j = 0; j < actions.length; j++) {
                if (actions[j].getValue(Action.NAME)
                        .equals(keys[i].actionName)) {
                    index = j;
                    break;
                }
            }
            if (index >= 0) {
                keymap.addActionForKeyStroke(keys[i].key, actions[index]);
            }
        }
    }

    public static Keymap removeKeymap(final String name) {
        return (Keymap)keymaps.remove(name);
    }

    public JTextComponent() {
        ComponentInternals.getComponentInternals()
            .setTextKit(this, textKit);
        inputMethodListener = new InputMethodListenerImpl(textKit);
        updateUI();
        enableEvents(InputEvent.INPUT_METHOD_EVENT_MASK);
        addFocusListener(new FocusListenerImpl());
        setEditable(true);
    }

    private class TextKitImpl implements TextKit {
        TextCaret textCaret = new TextCaret() {
            public AWTHighlighter getHighlighter() {
                return null;
            }
            public void setComponent(final Component c) {
            }
            public void paint(final Graphics g) {

            }
            public void setMagicCaretPosition(final int pos,
                                         final int direction,
                                         final Point oldPoint) {
                try {
                    Point newPoint = null;
                    if (direction == SwingConstants.SOUTH
                            || direction == SwingConstants.NORTH) {
                        View view =
                            getUI().getRootView(JTextComponent.this);
                        Shape shape = getVisibleRect();
                        if (oldPoint == null) {
                            Rectangle r =
                                view.modelToView(
                                    pos, shape,
                                    Position.Bias.Forward).getBounds();
                            newPoint = new Point(r.x, r.y);
                        } else {
                            newPoint = oldPoint;
                        }
                    }
                    JTextComponent.this.getCaret().
                        setMagicCaretPosition(newPoint);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }

            public int getDot() {
                return JTextComponent.this.getCaret().getDot();
            }

            public int getMark() {
                return JTextComponent.this.getCaret().getMark();
            }

            public void setDot(final int pos,
                                         final Position.Bias b) {
                Caret caret = JTextComponent.this.getCaret();
                if (caret instanceof DefaultCaret) {
                    ((DefaultCaret)caret).setDot(pos, b);
                } else {
                    caret.setDot(pos);
                }
            }

            public Point getMagicCaretPosition() {
                return JTextComponent.this.getCaret().
                    getMagicCaretPosition();
            }

            public void moveCaretPosition(final int pos) {
                JTextComponent.this.moveCaretPosition(pos);
            }

            public void setMagicCaretPosition(final Point pt) {
                JTextComponent.this.getCaret().setMagicCaretPosition(pt);
            }

            public void moveDot(final int pos, final Position.Bias b) {
                Caret caret = JTextComponent.this.getCaret();
                if (caret instanceof DefaultCaret) {
                    ((DefaultCaret)caret).moveDot(pos, b);
                } else {
                    caret.moveDot(pos);
                }
            }

            public Position.Bias getDotBias() {
                if (caret instanceof DefaultCaret) {
                    return ((DefaultCaret)caret).getDotBias();
                } else {
                    return Position.Bias.Forward;
                }
            }
        };

        public boolean isEditable() {
            return JTextComponent.this.isEditable();
        }

        public void scrollRectToVisible(final Rectangle rect) {
            JTextComponent.this.scrollRectToVisible(rect);
        }

        public int viewToModel(final Point p, final Position.Bias[] biasRet) {
            return ((BasicTextUI)ui).viewToModel(JTextComponent.this,
                                                 p, biasRet);
        }

        public Rectangle modelToView(final int p) throws BadLocationException {
            return JTextComponent.this.modelToView(p);

        }
        public Rectangle modelToView(final int pos, final Position.Bias bias)
            throws BadLocationException {
            return ((BasicTextUI)ui).modelToView(JTextComponent.this,
                                                 pos, bias);
        }
        public Component getComponent() {
            return JTextComponent.this;
        }

        public void revalidate() {
            JTextComponent.this.revalidate();
        }

        public Document getDocument() {
            return document;
        }

        public int getSelectionStart() {
            return JTextComponent.this.getSelectionStart();
        }

        public int getSelectionEnd() {
            return JTextComponent.this.getSelectionEnd();
        }

        public Color getDisabledTextColor() {
            return JTextComponent.this.getDisabledTextColor();
        }

        public Color getSelectedTextColor() {
            return JTextComponent.this.getSelectedTextColor();
        }

        public void replaceSelectedText(final String str) {
            JTextComponent.this.replaceSelection(str);
        }

        public TextCaret getCaret() {
            return textCaret;
        }

        public String getSelectedText() {
            return JTextComponent.this.getSelectedText();
        }

        public Rectangle getVisibleRect() {
            return JTextComponent.this.getVisibleRect();
        }

        public View getRootView() {
            return JTextComponent.this.getUI().getRootView(JTextComponent.this);
        }

        public boolean isScrollBarArea(final int x, final int y) {
            return false;
        }

        public void addCaretListeners(final EventListener listener) {
        }

        public void paintLayeredHighlights(final Graphics g, final int p0,
                                           final int p1, final Shape viewBounds,
                                           final View v) {
            if (highlighter instanceof LayeredHighlighter) {
                ((LayeredHighlighter)highlighter)
                    .paintLayeredHighlights(g, p0, p1, viewBounds,
                                            JTextComponent.this, v);
            }

        }
    }

    private static class KeyMapWrapper {
        private class ActionMapWrapper extends ActionMap {
            public Action get(final Object key) {
                if (key == DEFAULT_ACTION_KEY) {
                    return keymap.getDefaultAction();
                }

                if (lastAccessedAction != null) {
                    if (checkAction(lastAccessedAction, key)) {
                        return lastAccessedAction;
                    }
                }
                final Action[] boundActions = keymap.getBoundActions();
                if (boundActions != null) {
                    for (int i = 0; i < boundActions.length; i++) {
                        final Action action = boundActions[i];
                        if (checkAction(action, key)) {
                            lastAccessedAction = action;
                            return action;
                        }
                    }
                }

                return super.get(key);
            }

            public int size() {
                final Action[] boundActions = keymap.getBoundActions();
                int result = 1 + ((boundActions != null) ? boundActions.length : 0);
                return result + super.size();
            }

            private boolean checkAction(final Action action,  final Object key) {
                if (action == key) {
                    return true;
                }
                final Object curKey = action.getValue(Action.NAME);
                return (curKey != null && curKey.equals(key));
            }
        }

        private class InputMapWrapper extends InputMap {
            public Object get(final KeyStroke keystroke) {
                Object key = null;
                final Action action = keymap.getAction(keystroke);
                lastAccessedAction = action;
                if (action != null) {
                    key = action.getValue(Action.NAME);
                    if (key == null) {
                       return action;
                    }
                }
                if (key == null) {
                    key = super.get(keystroke);
                }
                return key == null && isKeyStrokeForDefaultAction(keystroke)
                        ? DEFAULT_ACTION_KEY : key;
            }

            public int size() {
                final KeyStroke[] boundKeyStrokes = keymap.getBoundKeyStrokes();
                int result = (boundKeyStrokes != null) ? boundKeyStrokes.length : 0;
                return result + super.size();
            }

            private boolean isKeyStrokeForDefaultAction(final KeyStroke keystroke) {
                return (keystroke.getKeyEventType() == KeyEvent.KEY_TYPED)
                       && ((keystroke.getModifiers() & InputEvent.ALT_DOWN_MASK) == 0)
                       && ((keystroke.getModifiers() & InputEvent.CTRL_DOWN_MASK) == 0);
            }
        }

        private final Keymap keymap;
        private final ActionMapWrapper actionMap;
        private final InputMapWrapper inputMap;
        private Action lastAccessedAction;

        public KeyMapWrapper(final Keymap k) {
            keymap = k;
            actionMap = new ActionMapWrapper();
            inputMap = new InputMapWrapper();
        }

        public ActionMap getActionMap() {
            return actionMap;
        }

        public InputMap getInputMap() {
            return inputMap;
        }
    }


    // ChangeListener implementation. In state Changed call fireCaretUpdate.
    private class CaretListenerImpl implements ChangeListener, Serializable {

        public void stateChanged(final ChangeEvent ce) {
            if (caret != null) {
                fireCaretUpdate(caretEvent);
            }
        }

        public String toString() {
            return "dot=" + caret.getDot() + ",mark=" + caret.getMark();
        }
    }

    /** CaretEvent implementation.
     */
    private class CaretEventImpl extends CaretEvent {
        public CaretEventImpl() {
            super(JTextComponent.this);
        }

        public int getDot() {
            return caret.getDot();
        }

        public int getMark() {
            return caret.getMark();
        }
    }


    /**
     * FocusListener implementation to set variable, which indicates last
     * focused text component.
     */
    final class FocusListenerImpl extends FocusAdapter {
        public void focusGained(final FocusEvent fe) {
            focusedComponent = JTextComponent.this;
        }
    }

  public void addCaretListener(final CaretListener listener) {
        listenerList.add(CaretListener.class, listener);
    }

    public void copy() {
        TextUtils.copy(textKit);
    }

    public void cut() {
        TextUtils.cut(textKit);
    }

    protected void fireCaretUpdate(final CaretEvent ce) {
        CaretListener[] listeners = getCaretListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].caretUpdate(ce);
        }
        if (ce != null) {
            handleComposedText(ce.getDot());
        }
    }

    private void handleComposedText(final int dot) {
        ComposedTextParams composedTextParams = getComposedTextParams();
        int lastInsertPosition = composedTextParams.getComposedTextStart();
        int composedTextLength = composedTextParams.getComposedTextLength();
        if (composedTextLength > 0 && (dot < lastInsertPosition
                || dot > lastInsertPosition +  composedTextLength)) {
        // TODO: Uncomment when InputContext is fully supported by awt
//            SwingUtilities.invokeLater(new Runnable() {
//                public void run() {
//                    getInputContext().endComposition();
//                }
//            });
        }
    }

    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJTextComponent();
        }
        return accessibleContext;

    }

    public Action[] getActions() {
        return ((TextUI)ui).getEditorKit(this).getActions();
    }

    public Caret getCaret() {
        return caret;
    }

    public Color getCaretColor() {
        return caretColor;
    }

    public CaretListener[] getCaretListeners() {
        return (CaretListener[])listenerList.getListeners(CaretListener.class);
    }

    public int getCaretPosition() {
        return caret.getDot();
    }

    public Color getDisabledTextColor() {
        return disabledTextColor;
    }

    public Document getDocument() {
        return document;
    }

    public boolean getDragEnabled() {
        return isDragEnabled;
    }

    public char getFocusAccelerator() {
        return focusAccelerator;
    }

    public Highlighter getHighlighter() {
        return highlighter;
    }

    public InputMethodRequests getInputMethodRequests() {
        if (inputMethodRequests == null) {
            inputMethodRequests = new InputMethodRequestsImpl(textKit);
        }
        return inputMethodRequests;
    }

    public Keymap getKeymap() {
        return currentKeyMap;
    }

    public Insets getMargin() {
        return margin;
    }

    public NavigationFilter getNavigationFilter() {
        return navigationFilter;
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableBlockIncrement(final Rectangle rect,
                                           final int orientation,
                                           final int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return rect.width;
        } else if (orientation == SwingConstants.VERTICAL) {
            return rect.height;
        } else {
            throw new IllegalArgumentException(Messages.getString("swing.41", orientation)); //$NON-NLS-1$
        }
    }

    public boolean getScrollableTracksViewportHeight() {
        Container c = getParent();
        if (c instanceof JViewport) {
            int height = getPreferredSize().height;
            return  height < ((JViewport)c).getExtentSize().height;
        }
        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        Container c = getParent();
        if (c instanceof JViewport) {
            int width = getPreferredSize().width;
            return width < ((JViewport)c).getExtentSize().width;
        }
        return false;
    }

    public int getScrollableUnitIncrement(final Rectangle rect,
                                          final int orientation,
                                          final int direction) {
        final int DEFAULT_UNIT_NUMBER = 10;
        if (orientation == SwingConstants.HORIZONTAL) {
            return rect.width / DEFAULT_UNIT_NUMBER;
        } else if (orientation == SwingConstants.VERTICAL) {
            return rect.height / DEFAULT_UNIT_NUMBER;
        } else {
            throw new IllegalArgumentException(Messages.getString("swing.41", orientation)); //$NON-NLS-1$
        }
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
        } catch (final BadLocationException e) { }
        return s;
    }

    public Color getSelectedTextColor() {
        return selectedTextColor;
    }

    public Color getSelectionColor() {
        return selectionColor;
    }

    public int getSelectionEnd() {
        return Math.max(caret.getDot(), caret.getMark());
    }

    public int getSelectionStart() {
        return Math.min(caret.getDot(), caret.getMark());
    }

    public String getText() {
        String s = null;
        try {
            s = getText(0, document.getLength());
        } catch (final BadLocationException e) {
        }
        return s;
    }

    public String getText(final int pos, final int length)
        throws BadLocationException {
        return document.getText(pos, length);
    }

    public String getToolTipText(final MouseEvent me) {
        String toolTipText = super.getToolTipText();
        return (toolTipText != null) ? toolTipText
                : ((BasicTextUI) ui).getToolTipText(this, new Point(me.getX(),
                        me.getY()));
    }

    public TextUI getUI() {
        return (TextUI)ui;
    }

    public boolean isEditable() {
        return isEditable;
    }

    public Rectangle modelToView(final int a0) throws BadLocationException {
        return ((TextUI) ui).modelToView(this, a0);
    }

    public void moveCaretPosition(final int pos) {
        if (document == null) {
            return;
        }
        if (pos < 0 || pos > document.getLength()) {
            throw new IllegalArgumentException(Messages.getString("swing.91", pos)); //$NON-NLS-1$
        }
        caret.moveDot(pos);
    }

    /*
     * The format of the string is based on 1.5 release behavior
     * of JTextArea class which can be revealed using the following code:
     *
     *     System.out.println(new JTextArea().toString());
     */
    protected String paramString() {
        return super.paramString() + "," + "caretColor=" + caretColor + ","
                + "disabledTextColor=" + disabledTextColor + "," + "editable="
                + isEditable + "," + "margin=" + margin + ","
                + "selectedTextColor=" + selectedTextColor + ","
                + "selectionColor=" + selectionColor;

    }

    public void paste() {
        TextUtils.paste(textKit);
    }

    /**
     * First, event is handlede by component's input method listener.
     * Second, events is handles by others input method listsners.
     *
     */
    protected void processInputMethodEvent(final InputMethodEvent e) {
        TextUtils.processIMEvent(inputMethodListener, e);
        super.processInputMethodEvent(e);
    }

    public void removeNotify() {
        super.removeNotify();
        if (focusedComponent == this) {
            focusedComponent = null;
        }
    }

    public void read(final Reader reader, final Object property)
        throws IOException {
        try {
            document.remove(0, document.getLength());
            ((TextUI)ui).getEditorKit(this).read(reader, document, 0);
        } catch (final BadLocationException e) {
        }
        document.putProperty(Document.StreamDescriptionProperty, property);
    }

    private void readObject(final ObjectInputStream s) throws IOException,
            ClassNotFoundException {
        s.defaultReadObject();
    }

    public void removeCaretListener(final CaretListener listener) {
        listenerList.remove(CaretListener.class, listener);
    }

        //See comments to setText method.
    public synchronized void replaceSelection(final String text) {
        int dot = caret.getDot();
        int mark = caret.getMark();
        try {
            int start = Math.min(dot, mark);
            int length = Math.abs(dot - mark);
            if (document instanceof AbstractDocument) {
               ((AbstractDocument)document).replace(start, length, text, null);
            } else {
                replaceString(start, length, text, null);
            }
        } catch (final BadLocationException e) { }
    }

    private void replaceString(final int offset, final int length,
                               final String text, final AttributeSet as)
        throws BadLocationException {
        document.remove(offset, length);
        document.insertString(offset, text, as);
    }
    public void select(final int p0, final int p1) {
        int length = document.getLength();
        int start = Math.max(Math.min(p0, length), 0);
        int end = Math.max(start, Math.min(p1, length));
        caret.setDot(start);
        caret.moveDot(end);
    }

    public void selectAll() {
        caret.setDot(0); //TODO bias
        int length = document.getLength();
        if (caret instanceof DefaultCaret) {
            ((DefaultCaret)caret).moveDot(length, Position.Bias.Backward);
        } else {
            caret.moveDot(length);
        }
    }

    public void setCaret(final Caret c) {
        if (caret == c) {
            return;
        }
        if (caret != null) {
            caret.removeChangeListener(changeCaretListener);
            caret.deinstall(this);
        }
        if (c != null) {
                c.install(this);
                c.addChangeListener(changeCaretListener);
        }
        Caret old = caret;
        caret = c;
        firePropertyChange("caret", old, c);
    }

    public void setCaretColor(final Color c) {
        firePropertyChange(StringConstants.TEXT_COMPONENT_CARET_COLOR_PROPERTY,
                           caretColor, c);
        caretColor = c;
    }

    public void setCaretPosition(final int pos) {
        if (document == null) {
            return;
        }
        if (pos < 0 || pos > document.getLength()) {
            throw new IllegalArgumentException(Messages.getString("swing.91", pos)); //$NON-NLS-1$
        }
        caret.setDot(pos);
    }

    /**
     * Sets documents property java.awt.font.RUN_DIRECTION according to
     * direction.
     */
    public void setComponentOrientation(final ComponentOrientation direction) {
        componentOrientation = direction == ComponentOrientation.RIGHT_TO_LEFT;
        setDocumentDirection();
        super.setComponentOrientation(direction);
    }

    public void setDisabledTextColor(final Color c) {
        Color old = disabledTextColor;
        disabledTextColor = c;
        firePropertyChange(StringConstants.TEXT_COMPONENT_DISABLED_TEXT_COLOR,
                           old, c);
    }

    /**
     * Sets Document Property java.awt.font.RUN_DIRECTION to
     * false. Removes all highlights if doc isn't current document.
     */
    public void setDocument(final Document doc) {
        Document old = document;
        if (doc != document && highlighter != null) {
            highlighter.removeAllHighlights();
        }
        document = doc;
        firePropertyChange(StringConstants.TEXT_COMPONENT_DOCUMENT_PROPERTY,
                           old, doc);
        setDocumentDirection();
    }

    /**
     * Sets documents property java.awt.font.RUN_DIRECTION according to
     * direction.
     */
    private void setDocumentDirection() {
        if (document != null) {
            document.putProperty(TextAttribute.RUN_DIRECTION, new Boolean(
                    componentOrientation));
        }
    }

    public void setDragEnabled(final boolean b) {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        isDragEnabled = b; //TODO it may be no enought(some L&F)
        //TODO Really it will not work, because javax.swing.Transfer doesn't
        //provide enough functionality. Appropriate TransferHandler sets by
        // BasicTextUI. But I don't know now, what should I do, if it
        // TransferHandler null here....
        if (b & (getTransferHandler() == null)) {
            setTransferHandler(transferHandler);
        }
    }

    public void setEditable(final boolean isEditable) {
        boolean old = this.isEditable;
        this.isEditable = isEditable;
        if (isEditable) {
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
        firePropertyChange(StringConstants.EDITABLE_PROPERTY_CHANGED,
                           old, isEditable);
    }

    public void setFocusAccelerator(final char accelerator) {
        char c = Character.toUpperCase(accelerator);
        char old = focusAccelerator;
        focusAccelerator = c;
        // 1.4.2
        firePropertyChange(FOCUS_ACCELERATOR_KEY, old, c);
        //1.5.0
        //firePropertyChange("focusAccelerator", old, c);
    }

    public void setHighlighter(final Highlighter h) {
        Highlighter old = highlighter;
        if ((h == null) && (highlighter != null)) {
            highlighter.deinstall(this);
        } else if (h != null) {
            h.install(this);
        }
        highlighter = h;
        firePropertyChange(StringConstants.TEXT_COMPONENT_HIGHLIGHTER_PROPERTY,
                           old, h);
    }

    /**
     * Performs modification in ActionMap/InputMap structure.
     * If keymap is not null, getAction/InputMap().getParent() corresponds
     * to current keymap.
     * Keymap may be previously added to JTextComponent, then all keymap
     * changes will be reflected on appropriate ActionMap/InputMap.
     */
    public void setKeymap(final Keymap k) {
        Keymap old = currentKeyMap;
        if (currentKeyMap != null) {
            getActionMap().setParent(getActionMap().getParent().getParent());
            getInputMap().setParent(getInputMap().getParent().getParent());
        }

        currentKeyMap = k;

        if (k != null) {
            KeyMapWrapper wrapper = new KeyMapWrapper(k);
            InputMap im = wrapper.getInputMap();
            ActionMap am = wrapper.getActionMap();
            am.setParent(getActionMap().getParent());
            im.setParent(getInputMap().getParent());
            getActionMap().setParent(am);
            getInputMap().setParent(im);
        }
        firePropertyChange(StringConstants.TEXT_COMPONENR_KEYMAP_PROPERTY,
                           old, k);
    }

    public void setMargin(final Insets insets) {
        Insets old = margin;
        margin = insets;
        firePropertyChange(StringConstants.TEXT_COMPONENT_MARGIN_PROPERTY,
                           old, insets);
    }

    public void setNavigationFilter(final NavigationFilter filter) {
        NavigationFilter old = navigationFilter;
        navigationFilter = filter;
        firePropertyChange(StringConstants.TEXT_COMPONENT_NAV_FILTER_NAME,
                           old, filter);
    }

    public void setSelectedTextColor(final Color c) {
        Color old = selectedTextColor;
        selectedTextColor = c;
        firePropertyChange(StringConstants.TEXT_COMPONENT_SELECTED_TEXT_COLOR,
                           old, c);
    }

    public void setSelectionColor(final Color c) {
        Color old = selectionColor;
        selectionColor = c;
        firePropertyChange(StringConstants
                           .TEXT_COMPONENT_SELECTION_COLOR_PROPERTY, old, c);
    }

    public void setSelectionEnd(final int pos) {
        int start = getSelectionStart();
        if (pos < start) {
            caret.setDot(pos);
        } else {
           caret.setDot(start);
           caret.moveDot(Math.max(start, pos));
        }
    }

    public void setSelectionStart(final int pos) {
        int end = getSelectionEnd();
        if (pos > end) {
           caret.setDot(pos);
        } else {
           caret.setDot(end);
           caret.moveDot(Math.min(pos, end));
        }
    }

    //     Some clarification why (replace) differs from (remove, insert).
    // Replace is implemented is single transaction, so document filter
    // either permit this operation or not.
    // In the case of (remove, insert) document filter can permit remove
    // and forbid insert, for example. That'll be incorrectly.
    //     If document isn't instance of AbstractDocument, document filter
    // cann't be installed. So such problem doesn't appears.

    public synchronized void setText(final String text) {
         try {
            int length = document.getLength();
            if (document instanceof AbstractDocument) {
                ((AbstractDocument)document).replace(0, length, text, null);
            } else {
                document.remove(0, length);
                document.insertString(0, text, null);
            }
        } catch (final BadLocationException e) {
        }
    }

    /**
     * If textUI is not null, keymap was added to JTextComponent with name
     * equals textUI class name.
     */
    public void setUI(final TextUI textUI) {
        if (textUI != null) {
            addKeymap(textUI.getClass().getName(), JTextComponent.getKeymap(
                    JTextComponent.DEFAULT_KEYMAP));
        }
        super.setUI(textUI);
    }

    /**
     * If current UI is not null, keymap is removed (keymap, which has got name
     * as UI class name).
     */
    public void updateUI() {
        if (ui != null) {
            removeKeymap(ui.getClass().getName());
        }
        setUI(UIManager.getUI(this));
    }

    public int viewToModel(final Point p) {
        return ((TextUI) ui).viewToModel(this, p);
    }

    public void write(final Writer writer) throws IOException {
        try {
            ((TextUI)ui).getEditorKit(this).write(writer, document, 0,
                    document.getLength());
        } catch (final BadLocationException e) { }
    }

    private ComposedTextParams getComposedTextParams() {
        Object currentProperty = document
           .getProperty(PropertyNames.COMPOSED_TEXT_PROPERTY);
        if (!(currentProperty instanceof ComposedTextParams)) {
           ComposedTextParams result = new ComposedTextParams(document);
           int caretPosition = getCaretPosition();
           result.setComposedTextStart(caretPosition);
           result.setLastCommittedTextStart(caretPosition);
           return result;
        }
        return (ComposedTextParams)currentProperty;
    }
}
