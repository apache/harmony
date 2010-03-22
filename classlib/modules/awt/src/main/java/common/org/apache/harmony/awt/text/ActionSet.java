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
 * @author Alexander T. Simbirtsev, Evgeniya G. Maenkova
 */

package org.apache.harmony.awt.text;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.util.HashMap;
import javax.swing.SwingConstants;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;

//TODO: complete bidirectional text support (in almost all actions)
//TODO: lock/unlock document document (in almost all actions)
/**
 * Implements a set of text actions that should be used as a base for
 * text component implementations.
 */
public final class ActionSet implements ActionNames {

    private static class InsertBreakAction extends AWTTextAction {
        public InsertBreakAction() {
            isEditAction = true;
        }
        @Override
        public void internalPerformAction(final TextKit tk) {
            tk.replaceSelectedText("\n"); //$NON-NLS-1$
        }
    }

    private static class InsertTabAction extends AWTTextAction {
        public InsertTabAction() {
            isEditAction = true;
        }
        @Override
        public void internalPerformAction(final TextKit tk) {
            tk.replaceSelectedText("\t"); //$NON-NLS-1$
        }
    }

    private static class CutAction extends AWTTextAction {
        public CutAction() {
            isEditAction = true;
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
            TextUtils.cut(tk);
        }
    }

    private static class CopyAction extends AWTTextAction {
        @Override
        public void internalPerformAction(final TextKit tk) {
            TextUtils.copy(tk);
        }
    }

    private static class PasteAction extends AWTTextAction {
        public PasteAction() {
            isEditAction = true;
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
            TextUtils.paste(tk);
        }
    }

    private static class BeepAction extends AWTTextAction {
        @Override
        public void internalPerformAction(final TextKit tk) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private static class SelectAllAction extends AWTTextAction {
        @Override
        public void internalPerformAction(final TextKit tk) {
            TextCaret caret = tk.getCaret();
            int length = tk.getDocument().getLength();
            caret.setDot(0, Position.Bias.Backward);
            caret.moveDot(length, Position.Bias.Backward);
        }
    }

    private static class DumpModelAction extends AWTTextAction {
        @Override
        public void internalPerformAction(final TextKit tk) {
            Document doc = tk.getDocument();
            if (doc instanceof AbstractDocument) {
                ((AbstractDocument) doc).dump(System.err);
            }
        }
    }

    private static class DeletePrevCharAction extends AWTTextAction {
        public DeletePrevCharAction() {
            isEditAction = true;
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
            TextCaret caret = tk.getCaret();
            int curPos = caret.getDot();
            Document doc = tk.getDocument();
            try {
                String selText = tk.getSelectedText();
                if (selText == null || selText.length() == 0) {
                    if (curPos > 0) {
                        doc.remove(curPos - 1, 1);
                    }
                } else {
                    int selStart = tk.getSelectionStart();
                    int selEnd = tk.getSelectionEnd();
                    doc.remove(selStart, selEnd - selStart);
                }
            } catch (final BadLocationException e) {
                // this should not happen ever if doc.remove works fine
                e.printStackTrace();
            }
        }
    }

    private static class PreviousWordAction extends AWTTextAction {
        private boolean isMovingCaret;

        public PreviousWordAction(final String name) {
            this.isMovingCaret =
                selectionPreviousWordAction.equals(name);
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
            try {
                int oldPos = tk.getCaret().getDot();
                Document doc = tk.getDocument();
                int length = doc.getLength();
                int newPos = (0 == length) ? 0 : TextUtils.getPreviousWord(
                    doc, (length != oldPos) ? oldPos : oldPos - 1);
                TextUtils.changeCaretPosition(tk, newPos, isMovingCaret);
                TextUtils.setCurrentPositionAsMagic(tk);
            } catch (final BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private static class NextVisualPositionAction extends AWTTextAction {
        private int direction = 0;

        private boolean isMovingCaret;

        private static final Position.Bias[] auxBiasArray = new Position.Bias[1];

        public NextVisualPositionAction(final String name) {
            if (backwardAction.equals(name)) {
                isMovingCaret = false;
                direction = SwingConstants.WEST;
            } else if (forwardAction.equals(name)) {
                direction = SwingConstants.EAST;
                isMovingCaret = false;
            } else if (upAction.equals(name)) {
                direction = SwingConstants.NORTH;
                isMovingCaret = false;
            } else if (downAction.equals(name)) {
                direction = SwingConstants.SOUTH;
                isMovingCaret = false;
            } else if (selectionForwardAction.equals(name)) {
                direction = SwingConstants.EAST;
                isMovingCaret = true;
            } else if (selectionBackwardAction.equals(name)) {
                direction = SwingConstants.WEST;
                isMovingCaret = true;
            } else if (selectionUpAction.equals(name)) {
                direction = SwingConstants.NORTH;
                isMovingCaret = true;
            } else if (selectionDownAction.equals(name)) {
                direction = SwingConstants.SOUTH;
                isMovingCaret = true;
            }
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
            TextCaret caret = tk.getCaret();
            int oldPos = caret.getDot();
            try {
                Shape shape = tk.getVisibleRect();
                int newPos = tk.getRootView().getNextVisualPositionFrom(
                    oldPos,
                    caret.getDotBias(), shape,
                    direction,
                    auxBiasArray);

                if (newPos >= 0) {
                    Point pt = caret.getMagicCaretPosition();
                    TextUtils.changeCaretPosition(
                        tk, newPos, isMovingCaret, auxBiasArray[0]);
                    caret.setMagicCaretPosition(oldPos, direction, pt);
                }
            } catch (final BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private static class DeleteNextCharAction extends AWTTextAction {
        public DeleteNextCharAction() {
            isEditAction = true;
        }
        @Override
        public void internalPerformAction(final TextKit tk) {
             TextCaret caret = tk.getCaret();
             int curPos = caret.getDot();
             Document doc = tk.getDocument();
             try {
                 String selText = tk.getSelectedText();
                 if (selText == null || selText.length() == 0) {
                     if (curPos < doc.getLength()) {
                         doc.remove(curPos, 1);
                     }
                 } else {
                     int selStart = tk.getSelectionStart();
                     int selEnd = tk.getSelectionEnd();
                     doc.remove(selStart, selEnd - selStart);
             }
             } catch (final BadLocationException e) {
                // this should not happen ever if doc.remove works fine
                   e.printStackTrace();
             }
         }
    }

    private static class EndAction extends AWTTextAction {
        private final boolean isMovingCaret;
        public EndAction(final String name) {
            isMovingCaret = selectionEndAction.equals(name);
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
                int length = tk.getDocument().getLength();
                TextUtils.changeCaretPosition(tk, length, isMovingCaret);
                TextUtils.setCurrentPositionAsMagic(tk);

         }
    }

    private static class EndLineAction extends AWTTextAction {
        private final boolean isMovingCaret;
        public EndLineAction(final String name) {
            isMovingCaret = selectionEndLineAction.equals(name);
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
            try {
                TextCaret caret = tk.getCaret();
                int oldPos = caret.getDot();
                int newPos = TextUtils.getRowEnd(tk, oldPos);
                TextUtils.changeCaretPosition(tk, newPos, isMovingCaret);
                TextUtils.setCurrentPositionAsMagic(tk);
            } catch (final BadLocationException e) {
                 e.printStackTrace();
            }
         }
    }

    private static class EndParagraphAction extends AWTTextAction {
        private final boolean isMovingCaret;
        public EndParagraphAction(final String name) {
            isMovingCaret = selectionEndParagraphAction.equals(name);
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
            Document doc = tk.getDocument();
            int curPos = tk.getCaret().getDot();
            Element elem = getParagraphElement(doc, curPos);
            int endPos = elem.getEndOffset();
            if (endPos <= doc.getLength()) {
                TextUtils.changeCaretPosition(tk, endPos, isMovingCaret);
                TextUtils.setCurrentPositionAsMagic(tk);
            }
        }
    }


    private static class BeginAction extends AWTTextAction {
        private final boolean isMovingCaret;
        public BeginAction(final String name) {
            isMovingCaret = (selectionBeginAction.equals(name));
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
                TextUtils.changeCaretPosition(tk, 0, isMovingCaret);
                TextUtils.setCurrentPositionAsMagic(tk);

        }
    }

    private static class BeginLineAction extends AWTTextAction {
        private final boolean isMovingCaret;
        public BeginLineAction(final String name) {
            isMovingCaret = selectionBeginLineAction.equals(name);
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
            try {
               TextCaret caret = tk.getCaret();
               int oldPos = caret.getDot();
               int newPos = TextUtils.getRowStart(tk, oldPos);
               TextUtils.changeCaretPosition(tk, newPos, isMovingCaret);
               TextUtils.setCurrentPositionAsMagic(tk);
            } catch (final BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private static class BeginParagraphAction extends AWTTextAction {
        private final boolean isMovingCaret;
        public BeginParagraphAction(final String name) {
            isMovingCaret =  selectionBeginParagraphAction.equals(name);
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
            Document doc = tk.getDocument();
            int curPos = tk.getCaret().getDot();
            Element elem = getParagraphElement(doc, curPos);
            int begPos = elem.getStartOffset();
            TextUtils.changeCaretPosition(tk, begPos, isMovingCaret);
            TextUtils.setCurrentPositionAsMagic(tk);
        }
    }

    private static class BeginWordAction extends AWTTextAction {
        private final boolean isMovingCaret;
        public BeginWordAction(final String name) {
            isMovingCaret = selectionBeginWordAction.equals(name);
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
            try {
                int oldPos = tk.getCaret().getDot();
                int newPos = TextUtils.getWordStart(tk, oldPos);
                TextUtils.changeCaretPosition(tk, newPos, isMovingCaret);
            } catch (final BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private static class EndWordAction extends AWTTextAction {
        private final boolean isMovingCaret;
        public EndWordAction(final String name) {
            isMovingCaret = selectionEndWordAction.equals(name);
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
            try {
                int oldPos = tk.getCaret().getDot();
                int length = tk.getDocument().getLength();

                if (length != 0 && oldPos != length) {
                    int newPos = TextUtils.getWordEnd(tk, oldPos);
                    TextUtils.changeCaretPosition(tk, newPos, isMovingCaret);
            }
            } catch (final BadLocationException e) {
                e.printStackTrace();
            }
        }
    }


    private static class NextWordAction extends AWTTextAction {
        private final boolean isMovingCaret;
        public NextWordAction(final String name) {
             isMovingCaret = selectionNextWordAction.equals(name);
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
            try {
                int oldPos = tk.getCaret().getDot();
                int length = tk.getDocument().getLength();

                if (length != 0 && oldPos != length) {
                    int newPos = TextUtils.getNextWord(tk.getDocument(),
                                                       oldPos);
                    if (newPos == 0 && oldPos != 0) {
                        newPos = TextUtils.getWordEnd(tk, oldPos);
                    }
                    TextUtils.changeCaretPosition(tk, newPos, isMovingCaret);
                    TextUtils.setCurrentPositionAsMagic(tk);
                }
            } catch (final BadLocationException e) {
                e.printStackTrace();
            }
         }
     }

    private static class VerticalPageAction extends AWTTextAction {
        private int direction = 0;
        private boolean isMovingCaret;

        public VerticalPageAction(final String name) {
            if (pageUpAction.equals(name)) {
                direction = SwingConstants.NORTH;
                isMovingCaret = false;
            } else if (pageDownAction.equals(name)) {
                direction = SwingConstants.SOUTH;
                isMovingCaret = false;
            } else if (selectionPageUpAction.equals(name)) {
                direction = SwingConstants.NORTH;
                isMovingCaret = true;
            } else if (selectionPageDownAction.equals(name)) {
                direction = SwingConstants.SOUTH;
                isMovingCaret = true;
            }
        }

        private boolean isUnableToMoveDown(final TextKit tk,
                final int pos) {
            return ((pos == tk.getDocument().getLength()
                    && direction == SwingConstants.SOUTH));
        }

        private boolean isUnableToMoveUp(final TextKit tk,
                final int pos) throws BadLocationException {

            return (TextUtils.getRowStart(tk, pos) == 0
                    && direction == SwingConstants.NORTH);
        }


        @Override
        public void internalPerformAction(final TextKit tk) {
            try {
                TextCaret caret = tk.getCaret();
                int oldPos = caret.getDot();
                if (!isUnableToMoveDown(tk, oldPos)
                        && !isUnableToMoveUp(tk, oldPos)) {
                    Point magicPt = caret.getMagicCaretPosition();

                    caret.setMagicCaretPosition(oldPos, direction, magicPt);
                    magicPt = caret.getMagicCaretPosition();

                    int height = tk.getVisibleRect().height;

                    Point oldPoint = tk.modelToView(oldPos).getBounds()
                    .getLocation();
                    Point newPoint = new Point(magicPt.x, oldPoint.y);
                    newPoint.y += (direction == SwingConstants.NORTH)
                    ? -height : height;
                    if (newPoint.y < 0) {
                        newPoint.y = 0;
                    }
                    int newPos = tk.viewToModel(newPoint,
                                                new Position.Bias[1]);
                    Rectangle r = tk.getVisibleRect();
                    r.y += (direction == SwingConstants.NORTH) ? -height
                            : height;

                    Rectangle wholeRect = tk.getComponent().getBounds();
                    if (r.y < 0) {
                        r.y = 0;
                    } else if (r.y + r.height > wholeRect.height) {
                        r.y -= r.y + r.height - wholeRect.height;
                    }
                    tk.scrollRectToVisible(r);
                    TextUtils.changeCaretPosition(tk, newPos,
                                                  isMovingCaret);
                }
            } catch (final BadLocationException e) {
                    e.printStackTrace();
                }
        }
    }

    private static class SelectLineAction extends AWTTextAction {
        @Override
        public void internalPerformAction(final TextKit tk) {
            TextCaret caret = tk.getCaret();
            int curPos = caret.getDot();
            try {
                int start = TextUtils.getRowStart(tk, curPos);
                int end = TextUtils.getRowEnd(tk, curPos);
                caret.setDot(start, Position.Bias.Forward);
                caret.moveDot(end, Position.Bias.Backward);
            } catch (final BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private static class SelectParagraphAction extends AWTTextAction {
        @Override
        public void internalPerformAction(final TextKit tk) {
            Document doc = tk.getDocument();
            TextCaret caret = tk.getCaret();
            int curPos = caret.getDot();
            Element elem = getParagraphElement(doc, curPos);
            int start = elem.getStartOffset();
            int end = elem.getEndOffset();
            final int length = doc.getLength();
            if (end >= length) {
                end = length;
            }
            caret.setDot(start, Position.Bias.Forward);
            caret.moveDot(end, Position.Bias.Backward);
        }
    }

    private static class SelectWordAction extends AWTTextAction {
        @Override
        public void internalPerformAction(final TextKit tk) {
            TextCaret caret = tk.getCaret();
            int curPos = caret.getDot();
            try {
               int length = tk.getDocument().getLength();
               if (length != 0) {
                   int start = TextUtils.getWordStart(tk, curPos);
                   int end = TextUtils.getWordEnd(tk, curPos);
                   caret.setDot(start, Position.Bias.Forward);
                   caret.moveDot(end, Position.Bias.Backward);
                }
            } catch (final BadLocationException e) {
                   e.printStackTrace();
            }
        }
    }

    private static class UnselectAction extends AWTTextAction {
        @Override
        public void internalPerformAction(final TextKit tk) {
            TextCaret caret = tk.getCaret();
            caret.setDot(caret.getDot(),
                         caret.getDotBias());
        }
    }

    private static class ToggleComponentOrientationAction
        extends AWTTextAction {
        @Override
        public void internalPerformAction(final TextKit tk) {
            Component component = tk.getComponent();
            if (component.getComponentOrientation().isLeftToRight()) {
                component.setComponentOrientation(ComponentOrientation
                                                  .RIGHT_TO_LEFT);
            } else {
                component.setComponentOrientation(ComponentOrientation
                                                  .LEFT_TO_RIGHT);
            }
        }
    }

    private static class PageAction extends AWTTextAction {
        protected int direction = 0;

        private static final Position.Bias[] biasRet = new Position.Bias[1];

        public PageAction(final String name) {
            if (selectionPageLeftAction.equals(name)) {
                direction = SwingConstants.NORTH;
            } else if (selectionPageRightAction.equals(name)) {
                direction = SwingConstants.SOUTH;
            }
        }

        @Override
        public void internalPerformAction(final TextKit tk) {
            Rectangle rect = tk.getVisibleRect();
            Point pt;
            int newPos;
            try {
                if (direction == SwingConstants.NORTH) {
                    pt = new Point(rect.x + 1, rect.y + 1);
                    newPos = tk.viewToModel(pt, biasRet);
                    newPos = TextUtils.getRowStart(tk, newPos);
                } else {
                    pt = new Point(rect.x + 1, rect.y + rect.height - 1);
                    newPos = tk.viewToModel(pt, biasRet);
                    newPos = TextUtils.getRowEnd(tk, newPos);
                }
                TextUtils.changeCaretPosition(tk, newPos, true);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private static Element getParagraphElement(final Document doc,
                                               final int offset) {
        Element root = doc.getDefaultRootElement();
        return root.getElement(root.getElementIndex(offset));
    }

    public static HashMap<String, AWTTextAction> actionMap;
    static {
        actionMap = new HashMap<String, AWTTextAction>();
        actionMap.put(deletePrevCharAction,  new DeletePrevCharAction());
        actionMap.put(insertBreakAction, new InsertBreakAction());
        actionMap.put(previousWordAction, new PreviousWordAction(previousWordAction));
        actionMap.put(selectionPreviousWordAction, new PreviousWordAction(selectionPreviousWordAction));
        actionMap.put(nextWordAction, new NextWordAction(nextWordAction));
        actionMap.put(selectionNextWordAction, new NextWordAction(selectionNextWordAction));
        actionMap.put(insertTabAction, new InsertTabAction());
        actionMap.put(dumpModelAction, new DumpModelAction());
        actionMap.put(beepAction, new BeepAction());
        actionMap.put(copyAction, new CopyAction());
        actionMap.put(cutAction, new CutAction());
        actionMap.put(pasteAction, new PasteAction());
        actionMap.put(selectAllAction, new SelectAllAction());
        actionMap.put(backwardAction, new NextVisualPositionAction(backwardAction));
        actionMap.put(forwardAction, new NextVisualPositionAction(forwardAction));
        actionMap.put(upAction, new NextVisualPositionAction(upAction));
        actionMap.put(downAction, new NextVisualPositionAction(downAction));
        actionMap.put(selectionBackwardAction, new NextVisualPositionAction(selectionBackwardAction));
        actionMap.put(selectionForwardAction, new NextVisualPositionAction(selectionForwardAction));
        actionMap.put(selectionUpAction, new NextVisualPositionAction(selectionUpAction));
        actionMap.put(selectionDownAction, new NextVisualPositionAction(selectionDownAction));
        actionMap.put(deleteNextCharAction, new DeleteNextCharAction());
        actionMap.put(endAction, new EndAction(endAction));
        actionMap.put(selectionEndAction, new EndAction(selectionEndAction));
        actionMap.put(endLineAction, new EndLineAction(endLineAction));
        actionMap.put(selectionEndLineAction, new EndLineAction(selectionEndLineAction));
        actionMap.put(endParagraphAction, new EndParagraphAction(endParagraphAction));
        actionMap.put(selectionEndParagraphAction, new EndParagraphAction(selectionEndParagraphAction));
        actionMap.put(endWordAction, new EndWordAction(endWordAction));
        actionMap.put(selectionEndWordAction, new EndWordAction(selectionEndWordAction));
        actionMap.put(beginAction, new BeginAction(beginAction));
        actionMap.put(selectionBeginAction, new BeginAction(selectionBeginAction));
        actionMap.put(beginLineAction, new BeginLineAction(beginLineAction));
        actionMap.put(selectionBeginLineAction, new BeginLineAction(selectionBeginLineAction));
        actionMap.put(beginParagraphAction, new BeginParagraphAction(beginParagraphAction));
        actionMap.put(selectionBeginParagraphAction, new BeginParagraphAction(selectionBeginParagraphAction));
        actionMap.put(beginWordAction, new BeginWordAction(beginWordAction));
        actionMap.put(selectionBeginWordAction, new BeginWordAction(selectionBeginWordAction));
        actionMap.put(selectWordAction, new SelectWordAction());
        actionMap.put(selectLineAction, new SelectLineAction());
        actionMap.put(selectParagraphAction, new SelectParagraphAction());
        actionMap.put(unselectAction, new UnselectAction());
        actionMap.put(pageUpAction, new VerticalPageAction(pageUpAction));
        actionMap.put(pageDownAction, new VerticalPageAction(pageDownAction));
        actionMap.put(selectionPageUpAction, new VerticalPageAction(selectionPageUpAction));
        actionMap.put(selectionPageDownAction, new VerticalPageAction(selectionPageDownAction));
        actionMap.put(toggleComponentOrientationAction, new ToggleComponentOrientationAction());
        actionMap.put(selectionPageLeftAction, new PageAction(selectionPageLeftAction));
        actionMap.put(selectionPageRightAction, new PageAction(selectionPageRightAction));
        actionMap.put(beepAction, new BeepAction());
    }
}