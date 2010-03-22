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
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EventListener;

import javax.swing.AbstractAction;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.Position.Bias;

import org.apache.harmony.awt.text.AWTTextAction;
import org.apache.harmony.awt.text.ActionNames;
import org.apache.harmony.awt.text.ActionSet;
import org.apache.harmony.awt.text.TextKit;
import org.apache.harmony.awt.text.TextUtils;
import org.apache.harmony.x.swing.StringConstants;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class DefaultCaret extends Rectangle implements Caret, FocusListener,
        MouseListener, MouseMotionListener {

    public static final int ALWAYS_UPDATE = 2;

    public static final int NEVER_UPDATE = 1;

    public static final int UPDATE_WHEN_ON_EDT = 0;

    protected EventListenerList listenerList = new EventListenerList();

    protected transient ChangeEvent changeEvent  = new ChangeEvent(this);

    private int mark;

    private int dot;

    private transient Position.Bias dotBias = Position.Bias.Forward;

    private transient Position.Bias  markBias = Position.Bias.Forward;

    private boolean async;


    //Position in document to follow the adjucent caret position
    //on document updates. Help to get new bias (dot) after
    //document updates.
    private int  dotTrack;

    //current policy (for 1.5.0 sets by setUpdatePolicy)
    private int selectedPolicy = UPDATE_WHEN_ON_EDT;

    private int blinkRate;

    //installed text component
    private Component  component;

    TextKit textKit;

    //current selection color
    private Color selectionColor;

    //current caret color
    private Color caretColor;

    //value selectionColor in case of JTextComponent.getSelectionColor()
    //equals null
    private static final Color  DEF_SEL_COLOR  = new Color(192, 224, 255);

    //value selectionColor in case of JTextComponent.getCaretColor()
    //equals null
    private static final Color  DEF_CARET_COLOR  = new Color(0, 0, 0);

    //delay for magicTimer
    static final int DEFAULT_MAGIC_DELAY = 600;

    private static final int APEX_NUMBER = 3;

    private static final int TRIANGLE_HEIGHT = 4;

    private static final int RIGHT_TRIANGLE_WIDTH = 5;

    private static final int LEFT_TRIANGLE_WIDTH  = 4;

    //current painter for selection
    private transient DefaultHighlighter.DefaultHighlightPainter painter;

    //reference to current selection, if any
    private transient Object selectionTag;

    //DocumentListener for document of current JTextComponent
    private transient DocumentListener dh = new DocumentHandler();

    //PropertyChangeListener for current JTextComponent
    private transient PropertyHandler pch;

    //used by mouseClicked method
    private static final transient AWTTextAction SELECT_WORD_ACTION =
        ActionSet.actionMap.get(ActionNames.selectWordAction);

    //used by mouseClicked method
    private static final transient AWTTextAction SELECT_LINE_ACTION =
        ActionSet.actionMap.get(ActionNames.selectLineAction);

    Point magicCaretPosition;

    private transient boolean isVisible;

    private boolean isSelectionVisible;

    //Timer to repaint caret according to current blinkRate
    transient Object blinkTimer;

    //Timer to set magicCaretPosition to current caret position
    transient Object magicTimer;

    //defines whether caret be painting or blinking, if blink on
    transient boolean shouldDraw = true;

    //defines x coordinates of flag, in case of bidirectional text
    private transient int[] triangleX = new int[APEX_NUMBER];

    //defines y coordinates of flag, in case of bidirectional text
    private transient int[] triangleY = new int[APEX_NUMBER];

    //used for modelToView calls
    private transient Position.Bias[] bias = new Position.Bias[1];

    //for DnD support, selection doesn't change when drag
    private boolean handleMouseDrag = true;

    //flag to restore selection
    private boolean restoreSelection;

    //used when JTextComponent has NavigationFilter
    private transient FilterBypass filterBypass = new FilterBypass(this);

    private transient NavigationFilter navigationFilter;

    private Highlighter highlighter;

    private Document document;

    private boolean isBidiDocument;

    //This variable remembers last coordinates, where mouse button
    //was pressed. Handling MouseEvent in MouseClicked method depends
    //on this.
    private int[] lastPressPoint = new int[2];

    //action for blinkTimer
    Object blinkAction;

    //Action for magic timer. If MagicCaretPosition still null,
    //MagicCaretPosition set to current caret position, else do nothing.
    Object magicAction;

    private class PropertyHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent evt) {
            String proptertyName = evt.getPropertyName();
            Object  newValue = evt.getNewValue();
            if (StringConstants.TEXT_COMPONENT_DOCUMENT_PROPERTY
                    .equals(proptertyName)) {
                Document oldDoc = (Document)evt.getOldValue();
                document = textKit.getDocument();
                if (oldDoc != null) {
                    oldDoc.removeDocumentListener(dh);
                }
                if (document != null) {
                    updateBidiInfo();
                    document.addDocumentListener(dh);
                    setDot(0);
                }
            } else if (StringConstants.TEXT_COMPONENT_CARET_COLOR_PROPERTY
                    .equals(proptertyName)) {
                caretColor = (Color)newValue;
            } else if (StringConstants.TEXT_COMPONENT_SELECTION_COLOR_PROPERTY
                    .equals(proptertyName)) {
                selectionColor = (Color)newValue;
                painter = new DefaultHighlighter.DefaultHighlightPainter(
                                                                selectionColor);
            } else if (StringConstants.TEXT_COMPONENT_HIGHLIGHTER_PROPERTY
                    .equals(proptertyName)) {
                highlighter = (Highlighter)newValue;
            } else if (StringConstants.TEXT_COMPONENT_NAV_FILTER_NAME
                    .equals(proptertyName)) {
                navigationFilter = (NavigationFilter)newValue;
            }
        }
    }

    //DocumentListener, to change dot, dotBias, mark, current selection on
    // document updates
    //according to AsynchronousMovement property.
    //Don't repaint caret, JText component do painting on document updates.
    private class DocumentHandler implements DocumentListener {

        /*
         * Returns position after string removing (length - length of string,
         * offset position where string is removed). Pos - position, which
         * should new position be defined for
         */
        private int newPosOnRemove(final int pos, final int length,
                                   final int offset) {
            int endUpdate = offset + length;
            if ((pos > offset) && (endUpdate <= pos)) {
                return pos - length;
            }
            if ((pos > offset) && (endUpdate > pos)) {
                return offset;
            }
            return pos;
        }

        /*
         * Returns position after string inserting (length - length of string,
         * offset position where string is added). Pos - position, which
         * should new position be defined for
         */
        private int newPosOnInsert(final int pos, final int length,
                                   final int offset) {
            if (offset <= pos) {
                return pos + length;
            }
            return pos;
        }

        /**
         * Caret position don't change on changeUpdate of Document
         */
        public void changedUpdate(final DocumentEvent e) {

        }

        public synchronized void removeUpdate(final DocumentEvent e) {
            final int docLength = document.getLength();
            final boolean condOnNeverUpdate =
                (docLength <= Math.max(dot, mark));
            final boolean isNeverUpdate = (selectedPolicy == NEVER_UPDATE);
            final boolean isUpdateWhenOnEdt =
                (selectedPolicy == UPDATE_WHEN_ON_EDT);
            final boolean isUpdateWhenOnEdtAndAsync =
                (isUpdateWhenOnEdt && !EventQueue.isDispatchThread());
            if ((isNeverUpdate || isUpdateWhenOnEdtAndAsync)
                && !condOnNeverUpdate) {
                return;
            }

            removeUpdate(e, isNeverUpdate || isUpdateWhenOnEdtAndAsync,
                         docLength);
        }

        private void removeUpdate(final DocumentEvent e,
                                  final boolean trivialUpdate,
                                  final int docLength) {
            int length = e.getLength();
            int offset = e.getOffset();
            int newMark;
            int newDot;
            Position.Bias newBias;

            if (trivialUpdate) {
                newMark = Math.min(mark, docLength);
                newDot = Math.min(dot, docLength);
                newBias = dotBias;
            } else {
                newMark = newPosOnRemove(mark, length, offset);
                dotTrack = newPosOnRemove(dotTrack, length, offset);
                newDot = newPosOnRemove(dot, length, offset);
                newBias = getNewBias(dotTrack, newDot);
            }

            mark = newMark;
            moveDot(newDot, newBias);
            if (dot == mark) {
                markBias = dotBias;
            }
        }

        public synchronized void insertUpdate(final DocumentEvent e) {
            if ((selectedPolicy == NEVER_UPDATE)
                || ((selectedPolicy == UPDATE_WHEN_ON_EDT) &&
                        !EventQueue.isDispatchThread())) {
                return;
            }

            int length = e.getLength();
            int offset = e.getOffset();
            String s = null;
            try {
                s = document.getText(offset, 1);
            } catch (final BadLocationException ex) {
            }

            if (offset == dot && (length + offset) != document.getLength()) {
                dotBias = Position.Bias.Backward;
            }

            if (s.equals("\n")) {
                dotBias = Position.Bias.Forward;
            }

            mark = newPosOnInsert(mark, length, offset);
            moveDot(newPosOnInsert(dot, length, offset), dotBias);
            if (dot == mark) {
                markBias = dotBias;
            }
            updateBidiInfo();
        }

    }

    /**
     * Default Implementation if NavigationFilter.FilterBypass. Used in setDot()
     * and moveDot(), when component.getNavigationFilter doesn't equal null.
     */
    private class FilterBypass extends NavigationFilter.FilterBypass {
        DefaultCaret caret;

        FilterBypass(final DefaultCaret dc) {
            caret = dc;
        }

        @Override
        public Caret getCaret() {
            return caret;
        }

        @Override
        public void setDot(final int i, final Bias b) {
            caret.internalSetDot(i, b);
        }

        @Override
        public void moveDot(final int i, final Bias b) {
            caret.internalMoveDot(i, b);
        }

    }

    /**
     * Sets all fiels to default values
     */
    public DefaultCaret() {
        blinkTimer = createTimer(false, 0);
        magicTimer = createTimer(true, 0);       
        painter = new DefaultHighlighter.DefaultHighlightPainter(
                selectionColor);
    }

    public void addChangeListener(final ChangeListener changeListener) {
        if (changeListener != null) {
            listenerList.add(ChangeListener.class, changeListener);
        }
    }

    /**
     * Adds selection according to current dot and mark, if isSelectionVisible
     * equals true.
     *
     */
    private void addHighlight() {
        if (mark == dot) {
            return;
        }
        if (!isSelectionVisible) {
            restoreSelection = true;
            removeHighlight();
            return;
        }

        if (selectionTag == null) {
             selectionTag = addHighlight(Math.min(dot, mark),
                                         Math.max(dot, mark));
        }
    }

    protected void adjustVisibility(final Rectangle r) {
        if (r != null) {
            textKit.scrollRectToVisible(new Rectangle(r.x, r.y, r.width + 1, r.height));
        }
    }

    /**
     * Repaint caret according to current dot and dotBias.
     *
     */
    private void calcNewPos() {
        Rectangle p = null;
        try {
            p = textKit.modelToView(dot, dotBias);
        } catch (final BadLocationException e) {
        }
        if (p == null) {
            return;
        }
        damage(p);
    }

    /**
     * Sets dot to new position i, repaint caret, sets magic caret position to
     * null. Calls fireStateChanged.
     *
     * @param i new dot
     */
    private void changeDot(final int i) {
        dot = i;
        magicCaretPosition = null;
        shouldDraw = true;
        dotTrack = changePosTrack(dot, dotBias);
        calcNewPos();
        fireStateChanged();
    }

    /**
     * Changes current selection, if any. Removes current selection, if dot
     * equals mark.
     */
    private void changeHighlight() {
        if (dot == mark) {
            if (selectionTag != null) {
                removeHighlight();
            }
        } else {
            if (selectionTag == null) {
                addHighlight();
            } else {
                changeHighlight(selectionTag, Math.min(dot, mark),
                                    Math.max(dot, mark));
            }
        }
    }

    /*
     * Calculate new adjacent position value according to pos and bias.
     *
     * @param newPos offset, which should new adjucent position be defined for
     * @param newBias bias of pos
     * @return new adjucent position
     */
    private int changePosTrack(final int newPos, final Position.Bias newBias) {
        if (newBias == Position.Bias.Forward) {
            return newPos + 1;
        } else {
            return newPos;
        }
    }

    protected synchronized void damage(final Rectangle r) {
        repaint();
        if (r == null) {
            return;
        }
        x = r.x;
        y = r.y;
        width = 0;
        height = r.height - 2;
        adjustVisibility(r);
        repaint();
    }

    /**
     * Stops timer for blinking.
     *
     */
    public void deinstall(final JTextComponent comp) {
        if (component == null || comp != component) {
            return;
        }
        if (document != null) {
            document.removeDocumentListener(dh);
        }
        component.removePropertyChangeListener(pch);
        component.removeMouseListener(this);
        component.removeMouseMotionListener(this);
        component.removeFocusListener(this);
        stopTimer(blinkTimer);
        stopTimer(magicTimer);
        highlighter = null;
        component = null;
        textKit = null;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj;
    }

    protected void fireStateChanged() {
        if (isVisible) {
            TextUtils.setNativeCaretPosition(this, component);
        }
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener)listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    /**
     * Sets isSelectionVisible to true
     *
     */
    public void focusGained(final FocusEvent fe) {
        isSelectionVisible = true;
        if (restoreSelection) {
            addHighlight();
            restoreSelection = false;
        }
        if (isComponentEditable()) {
            setVisible(true);
            repaint();
        } else {
            setVisible(false);
        }
    }

    /**
     * Sets isSelectionVisible to true
     *
     */
    public void focusLost(final FocusEvent fe) {
        setVisible(false);
        isSelectionVisible = true;
        Component c = fe.getOppositeComponent();
        if (c != null
            && !fe.isTemporary()
            && isRestoreSelectionCondition(c)) {
            restoreSelection = true;
            removeHighlight();
        }
        repaint();
    }

    private boolean getAsynchronousMovement() {
        return async;
    }

    public int getBlinkRate() {
        return blinkRate;
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);

    }

    protected final JTextComponent getComponent() {
        return component instanceof JTextComponent
            ? (JTextComponent)component : null;
    }

    /*
     * Defines flag direction, to paint caret in case of bidirectional text
     *
     * @return true - if flag in the left direction, false - if flag in the
     *         right direction
     */
    private boolean getDirection() {
        AbstractDocument ad = ((AbstractDocument)document);
        int length = ad.getLength();

        boolean currentDirection = (dot >= 1) ? ad.isLeftToRight(dot - 1)
                : false;
        boolean nextDirection = (dot <= length) ? ad.isLeftToRight(dot) : true;

        if (currentDirection == nextDirection) {
            return (currentDirection) ? false : true;
        }
        if (currentDirection) {
            return (dotBias == Position.Bias.Backward) ? false : true;
        } else {
            return (dotBias == Position.Bias.Backward) ? true : false;
        }
    }

    public int getDot() {
        return dot;
    }

    /**
     * Returns current dot bias
     *
     * @return dotBias dot bias
     */
    Position.Bias getDotBias() {
        return dotBias;
    }

    public <T extends EventListener> T[] getListeners(final Class<T> c) {
        T[] evL = null;
        try {
            evL = listenerList.getListeners(c);
        } catch (final ClassCastException e) {
            throw e;
        }
        return evL;
    }

    public Point getMagicCaretPosition() {
        return magicCaretPosition;
        //return stubMagicCaretPosition;
    }

    public int getMark() {
        return mark;
    }

    /**
     * Get new bias for current position and its adjucent position
     *
     * @param posTrack adjucent position
     * @param pos current position
     * @return bias
     */
    private Position.Bias getNewBias(final int posTrack, final int pos) {
        return (posTrack == pos) ? Position.Bias.Backward
                : Position.Bias.Forward;
    }

    protected Highlighter.HighlightPainter getSelectionPainter() {
        return painter;
    }

    public int getUpdatePolicy() {
        return selectedPolicy;
    }

    /**
     * Adds listeners to c, if c doesn't equal null.
     * Adds DocumentListener to
     * c, if c doesn't equals null. Sets textUI, caretColor, selectionColor to
     * value from c, if they don't equal null. Sets new painter according to
     * selectionColor.
     */
    public void install(final JTextComponent c) {
        setComponent(c);

        component.addMouseListener(this);
        component.addMouseMotionListener(this);
        component.addFocusListener(this);
        component.addPropertyChangeListener(getPropertyHandler());
        highlighter = c.getHighlighter();
        navigationFilter = c.getNavigationFilter();
        painter = new DefaultHighlighter.DefaultHighlightPainter(
                                        selectionColor);
    }

    void setComponent(final Component c) {
        component = c;
        textKit = TextUtils.getTextKit(component);
        document = textKit.getDocument();
        updateBidiInfo();
        if (document != null) {
            document.addDocumentListener(dh);
        }
        selectionColor = getSelectionColor();
        caretColor = getCaretColor();
    }

    public boolean isActive() {
        //return shouldDraw;
        return isVisible;
    }

    public boolean isSelectionVisible() {
        return isSelectionVisible;
    }

    public boolean isVisible() {
        return isVisible;
    }

    //MouseClicked is called if mouse button was released farther
    //than 5 pixels (from place, where mouse button was pressed).
    //Sometimes, it is not enough. For example, when width of
    //a letter is smaller than 5 pixels.
    //So it's necessarily to filter these mouse events.

    private boolean needClick(final MouseEvent e) {
        return selectionTag == null
               || (Math.abs(e.getX() - lastPressPoint[0]) > 1 && Math
                       .abs(e.getY() - lastPressPoint[1]) > 1);

    }

    public void mouseClicked(final MouseEvent me) {
        if (!needClick(me)) {
            return;
        }
        int clickCount = me.getClickCount();
        if (me.getButton() == MouseEvent.BUTTON1) {
            if (clickCount == 1) {
                positionCaret(me);
            }
            if (clickCount == 2) {
                SELECT_WORD_ACTION.performAction(textKit);
            }
            if (clickCount == 3) {
                SELECT_LINE_ACTION.performAction(textKit);
            }
        }
    }

    /**
     * Calculates offset, which correspond MouseEvent. If DragAndDropCondition
     * return false, moveDot is called.
     */
    public void mouseDragged(final MouseEvent me) {

        int offset = textKit.viewToModel(new Point(me.getX(), me
                .getY()), bias);
        if (offset < 0) {
            return;
        }
        int mask = MouseEvent.BUTTON1_DOWN_MASK;
        if ((me.getModifiersEx() & mask) != mask) {
            return;
        }
        if (handleMouseDrag) { //(! DragAndDropCondition(offset))
            moveDot(offset, bias[0]);
        }
    }

    /**
     * If component.getDragEnabled() returns true and offset in selection,
     * return true. Otherwise, returns false.
     */
    private boolean isDragAndDropCondition(final int offset) {
        return isDragEnabled()
                ? (offset >= Math.min(dot, mark) && offset <= Math.max(dot,                                                                       mark))
                : false;
    }

    public void mouseEntered(final MouseEvent me) {
    }

    public void mouseExited(final MouseEvent me) {
    }

    public void mouseMoved(final MouseEvent me) {
    }

    /**
     * Calculates offset, which corresponds to MouseEvent. If shift-button isn't
     * pressed, DragAndDropCondition return false, then setDot is called.
     */
    public void mousePressed(final MouseEvent me) {
        int offset;
        int mask = MouseEvent.SHIFT_DOWN_MASK;
        if (me.getButton() != MouseEvent.BUTTON1 || !component.isEnabled()) {
            return;
        }
        component.requestFocusInWindow();
        offset = textKit.viewToModel(new Point(me.getX(), me.getY()),
                                    bias);
        if (offset < 0) {
            return;
        }
        rememberPressPoint(me);
        if ((me.getModifiersEx() & mask) == mask) {
            moveDot(offset, bias[0]);
        } else {
            boolean condition = isDragAndDropCondition(offset);
            if (!condition) {
                setDot(offset, bias[0]);
            }
            handleMouseDrag = !(condition && dot != mark);
        }

    }

    private void rememberPressPoint(final MouseEvent e) {
        lastPressPoint[0] = e.getX();
        lastPressPoint[1] = e.getY();
    }

    public void mouseReleased(final MouseEvent me) {

    }

    protected void moveCaret(final MouseEvent me) {
        int offset = textKit.viewToModel(new Point(me.getX(), me
                .getY()), bias);
        if (offset >= 0) {
            moveDot(offset);
        }
    }

    /**
     * Calls changeDot, don't change mark. Adds or changes highlight, it depends
     * on current dot and mark
     *
     */
    public void moveDot(final int i) {
        moveDot(i, Position.Bias.Forward);
    }

    /**
     * If current JTextComponent has NavigationFilter then call
     * getComponent.getNavigationFilter.moveDot. Otherwise, calls changeDot,
     * don't change mark. Sets new dot, new dotBias. Adds or changes highlight,
     * it dependes on current dot and mark
     *
     * @param i new dot
     * @param b new dot bias
     */

    void moveDot(final int i, final Position.Bias b) {
        if (navigationFilter == null) {
            internalMoveDot(i, b);
        } else {
            navigationFilter.moveDot(filterBypass, i, b);
        }
    }

    private void internalMoveDot(final int i, final Position.Bias b) {
        dotBias = b;
        changeDot(i);
        changeHighlight();
    }

    public void paint(final Graphics g) {
        try {
            if (!isVisible || !shouldDraw) {
                return;
            }
            Rectangle p = textKit.modelToView(dot, dotBias);
            if (p == null) {
                return;
            }
            this.setBounds(p.x, p.y, 0, p.height - 2);
            g.setColor(caretColor);
            g.drawRect(x, y, width, height);

            if (isBidiDocument) {
                triangleX[0] = x;
                triangleX[1] = x;
                triangleX[2] = getDirection() ? (x - LEFT_TRIANGLE_WIDTH)
                        : (x + RIGHT_TRIANGLE_WIDTH);
                triangleY[0] = y;
                triangleY[1] = y + TRIANGLE_HEIGHT;
                triangleY[2] = y;
                g.fillPolygon(triangleX, triangleY, APEX_NUMBER);
            }
        } catch (BadLocationException e) {

        }
    }

    protected void positionCaret(final MouseEvent me) {
        int offset = textKit.viewToModel(new Point(me.getX(), me
                .getY()), bias);
        if (offset >= 0) {
            setDot(offset, bias[0]);
        }
    }

    /**
     * Reads object by default, reads string representation dotBias, markBias,
     * sets dotBias, markBias. Sets blinkTimer, magicTimer, textUI, dh, pch,
     * selectWord, selectLine, triangleX, triangleY, bias.
     *
     * @param s
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(final ObjectInputStream s) throws IOException,
            ClassNotFoundException {

        s.defaultReadObject();
        String forwardString = Position.Bias.Forward.toString();
        dotBias = (forwardString.equals(s.readUTF())) ? Position.Bias.Forward
                : Position.Bias.Backward;

        markBias = (forwardString.equals(s.readUTF())) ? Position.Bias.Forward
                : Position.Bias.Backward;
        painter = new DefaultHighlighter.DefaultHighlightPainter(
                                                       selectionColor);

        dh = new DocumentHandler();
        pch = new PropertyHandler();

        if (component != null) {
            component.addPropertyChangeListener(pch);
            if (textKit.getDocument() != null) {
                textKit.getDocument().addDocumentListener(dh);
            }
        }

        blinkTimer = createTimer(false, 0);
        magicTimer = createTimer(true, 0);

        triangleX = new int[APEX_NUMBER];
        triangleY = new int[APEX_NUMBER];

        bias = new Position.Bias[1];
    }

    public void removeChangeListener(final ChangeListener chL) {
        listenerList.remove(ChangeListener.class, chL);
    }

    /**
     * Removes current selection, selectionTag doesn't equal null. Sets
     * selectionTag to null.
     */
    private void removeHighlight() {
        if (selectionTag == null) {
            return;
        }
        removeHighlight(selectionTag);
        selectionTag = null;
    }

    protected final synchronized void repaint() {
        if (component != null) {
            if (!isBidiDocument) {
                //Commented since current 2d implementation cannot properly repaint region
                // of 1 pixel width.
                //component.repaint(0, x, y, width + 1, height + 1);
                component.repaint(0, x-2, y, width + 4, height + 1);
            } else {
                component.repaint(0, x - 5, y, width + 10, height + 1);
            }
        }
    }

    final void setAsynchronousMovement(final boolean b) {
        async = b;
        int i = (b) ? DefaultCaret.ALWAYS_UPDATE
                : DefaultCaret.UPDATE_WHEN_ON_EDT;
        setUpdatePolicy(i);
    }

    /**
     * Restarts timer for blinking, if blink on
     *
     */
    public void setBlinkRate(final int i) {
        if (i < 0) {
            throw new IllegalArgumentException(Messages.getString("swing.64", i)); //$NON-NLS-1$
        }
        blinkRate = i;
        stopTimer(blinkTimer);
        if (blinkRate > 0) {
            setTimerDelay(blinkTimer, blinkRate);
            if (isVisible) {
                startTimer(blinkTimer);
            }
        }
    }

    public void setDot(final int i) {
        setDot(i, Position.Bias.Forward);
        return;
    }

    /**
     * If current JTextComponent has NavigationFilter then call
     * getComponent.getNavigationFilter.setDot. Otherwise, sets dot and mark to
     * i, sets dotBias and markBias to b. Removes highlight, if any.
     *
     * @param i new dot
     * @param b new dotBias
     */

    void setDot(final int i, final Position.Bias b) {
        if (navigationFilter == null) {
            internalSetDot(i, b);
        } else {
            navigationFilter.setDot(filterBypass, i, b);
        }
    }

    private void internalSetDot(final int i, final Position.Bias b) {
        dotBias = b;
        markBias = b;
        mark = i;
        changeDot(i);
        removeHighlight();
    }

    public void setMagicCaretPosition(final Point p) {
        magicCaretPosition = p;
        //stubMagicCaretPosition = p;
    }

    public void setSelectionVisible(final boolean b) {
        isSelectionVisible = b;
        if (b) {
            addHighlight();
            restoreSelection = false;
        } else {
            restoreSelection = true;
            removeHighlight();
        }
    }

    public void setUpdatePolicy(final int policy) {
        if (policy >= 0 && policy <= 2) {
            selectedPolicy = policy;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void setVisible(final boolean b) {
        isVisible = b;
        if (b) {
            startTimer(magicTimer);
            if (blinkRate > 0) {
                startTimer(blinkTimer);
            }
        } else {
            stopTimer(blinkTimer);
            stopTimer(magicTimer);
        }
    }

    /*
     * The format of the string is based on 1.5 release behavior
     * of BasicTextUI.BasicCaret class which can be revealed
     * using the following code:
     *
     *     JTextArea textArea = new JTextArea();
     *     System.out.println(textArea.getCaret());
     *     System.out.println(textArea.getCaret().getClass().getName());
     */
    @Override
    public String toString() {
        return "Dot=(" + dot + ", " + dotBias.toString() + ") " + "Mark=("
               + mark + ", " + markBias.toString() + ")";
    }

    private void updateBidiInfo() {
        isBidiDocument = TextUtils.isBidirectional(document);
    }

    /**
     * Writes object bu default, writes string representation dotBias, markBias
     *
     * @param s
     * @throws IOException
     */
    private void writeObject(final ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeUTF(dotBias.toString());
        s.writeUTF(markBias.toString());
    }

    private PropertyChangeListener getPropertyHandler() {
        if (pch == null) {
            pch = new PropertyHandler();
        }
        return pch;
    }

    Object createTimer(final boolean isMagicTimer, final int delay) {
        return isMagicTimer ? new javax.swing.Timer(DEFAULT_MAGIC_DELAY,
                                        (ActionListener)getMagicAction())
            :  new javax.swing.Timer(delay, (ActionListener)getBlinkAction());
    }

    void startTimer(final Object timer) {
         ((Timer)timer).start();
    }

    void setTimerDelay(final Object timer, final int delay) {
        ((Timer)timer).setDelay(delay);
    }

    void stopTimer(final Object timer) {
        ((javax.swing.Timer)timer).stop();
    }

    Object getMagicAction() {
        if (magicAction == null) {
            magicAction =  new AbstractAction() {
                public void actionPerformed(final ActionEvent e) {
                    if (magicCaretPosition == null) {
                         magicCaretPosition = new Point(x, y);
                    }
                }
            };
        }
        return magicAction;
    }

    Object getBlinkAction() {
        if (blinkAction == null) {
            blinkAction = new AbstractAction() {
                public void actionPerformed(final ActionEvent e) {
                    shouldDraw = !shouldDraw;
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            repaint();
                        }
                    });
                }
            };
       }
       return blinkAction;
   }

   boolean isRestoreSelectionCondition(final Component c) {
       return SwingUtilities.windowForComponent(c) == SwingUtilities
               .windowForComponent(component);
   }

   Color getCaretColor() {
       JTextComponent textComponent = getComponent();
       Color componentsColor = textComponent.getCaretColor();
       return componentsColor != null ? componentsColor : DEF_CARET_COLOR;
   }

   Color getSelectionColor() {
       JTextComponent textComponent = getComponent();
       Color componentsColor = textComponent.getSelectionColor();
       return componentsColor != null ? componentsColor : DEF_SEL_COLOR;
   }

   boolean isComponentEditable() {
       return ((JTextComponent)component).isEditable() && component.isEnabled();
   }

   boolean isDragEnabled() {
       return ((JTextComponent)component).getDragEnabled();
   }

   Object addHighlight(final int p0, final int p1) {
       if (highlighter != null && component.isEnabled()) {
           Object result = null;
           try {
              result = highlighter.addHighlight(p0, p1, painter);
           } catch (BadLocationException e) {
           }
           return result;
       } else {
           return null;
       }
   }

   void changeHighlight(final Object tag, final int p0, final int p1) {
       if (highlighter != null) {
           try {
                highlighter.changeHighlight(tag, p0, p1);
           } catch (final BadLocationException e) {
           }
       }
   }

   void removeHighlight(final Object tag) {
       if (highlighter != null) {
           highlighter.removeHighlight(tag);
       }
   }

   void setMagicCaretPosition(final int pos,
                                     final int direction,
                                     final Point oldPoint) {
       try {
           Point newPoint = null;
           if (direction == SwingConstants.SOUTH
                  || direction == SwingConstants.NORTH) {
               if (oldPoint == null) {
                    Rectangle r =
                        textKit.modelToView(pos,
                                 Position.Bias.Forward).getBounds();
                        newPoint = new Point(r.x, r.y);
                    } else {
                        newPoint = oldPoint;
                    }
                }

               setMagicCaretPosition(newPoint);
       } catch (BadLocationException e) {
                e.printStackTrace();
       }
   }
}
