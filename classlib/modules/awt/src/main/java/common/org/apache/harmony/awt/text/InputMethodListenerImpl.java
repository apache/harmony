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
package org.apache.harmony.awt.text;

import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.font.TextHitInfo;
import java.text.AttributedCharacterIterator;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;

public class InputMethodListenerImpl implements InputMethodListener {
    private TextKit textKit;

    public InputMethodListenerImpl(final TextKit textKit) {
        this.textKit = textKit;
    }

    public void caretPositionChanged(final InputMethodEvent ime) {
         updateCaretPosition(ime.getCaret());
    }

    public void inputMethodTextChanged(final InputMethodEvent ime) {
        AttributedCharacterIterator content = ime.getText();
        ComposedTextParams composedTextParams = getComposedTextParams();
        int composedTextLength = composedTextParams.getComposedTextLength();
        if (content == null) {
            try {
                removeIMText(composedTextLength);
            } catch (final BadLocationException e) { }
            composedTextParams.setComposedTextLength(0);
            return;
        }
        int commitCount = ime.getCommittedCharacterCount();
        int start = content.getBeginIndex(); //realy start always 0???
        int end = content.getEndIndex();
        int lastComposedTextLength = composedTextLength;
        int composedStart = start + commitCount;
        composedTextLength = end - composedStart;
        boolean updateInsertPoint = (lastComposedTextLength == 0
                                     && composedTextLength > 0);
        composedTextParams.setComposedTextLength(composedTextLength);
        if (updateInsertPoint) {
            composedTextParams.setComposedTextStart(getCaretPosition()
                                                   + commitCount);
        }
        try {
            removeIMText(lastComposedTextLength);
            insertIMText(content, start, composedStart, end);
        } catch (final BadLocationException e) {
        }
        updateCaretPosition(ime.getCaret());
    }

    private void updateCaretPosition(final TextHitInfo imCaret) {
        Document doc = textKit.getDocument();
        ComposedTextParams composedTextParams = getComposedTextParams();
        if (composedTextParams.getComposedTextLength() > 0 && imCaret != null) {
            textKit.getCaret().setDot(Math.min(doc.getLength(),
                                      composedTextParams.getComposedTextStart()
                                      + imCaret.getInsertionIndex()),
                                      Position.Bias.Forward);
        }
    }

    private String getSubString(final AttributedCharacterIterator iterator,
                                final int start,
                                final int end) {
        int length = end - start;
        if (length == 0) {
            return null;
        }
        char[] characters = new char[length];
        iterator.setIndex(start);
        int count = 0;
        while (count < length) {
            characters[count++] = iterator.current();
            iterator.next();
        }
        return new String(characters);
    }

    private void insertIMText(final AttributedCharacterIterator iterator,
                              final int start, final int composedStart,
                              final int end)
          throws BadLocationException {
         Document doc = textKit.getDocument();
         ComposedTextParams composedTextParams = getComposedTextParams();
         int lastInsertPosition = composedTextParams.getComposedTextStart();
         int lastCommittedTextLength = composedTextParams
             .getLastCommittedTextLength();
         String text = getSubString(iterator, start, composedStart);
         if (text != null) {
             doc.insertString(lastInsertPosition, text, null);
         }
         lastCommittedTextLength = composedStart - start;
         lastInsertPosition += lastCommittedTextLength;
         text = getSubString(iterator, composedStart, end);
         if (text != null) {
             //TODO in composedText it need replace with attributes
             doc.insertString(lastInsertPosition, text, null/*as*/);
             composedTextParams.setComposedText(iterator, composedStart, end);
         }
         composedTextParams.setComposedTextStart(lastInsertPosition);
         composedTextParams.setLastCommittedTextStart(lastInsertPosition
                                                     - lastCommittedTextLength);
         doc.putProperty(PropertyNames.COMPOSED_TEXT_PROPERTY,
                         composedTextParams);
    }

    private void  removeIMText(final int lastComposedTextLength)
        throws BadLocationException {
        int selectionStart = textKit.getSelectionStart();
        int selectionEnd = textKit.getSelectionEnd();
        ComposedTextParams composedTextParams = getComposedTextParams();
        Document doc = textKit.getDocument();
        if (lastComposedTextLength == 0 || selectionStart != selectionEnd) {
            doc.remove(selectionStart, selectionEnd - selectionStart);
            composedTextParams.setComposedTextStart(getCaretPosition());
            return;
        }
        doc.remove(composedTextParams.getComposedTextStart(),
                   lastComposedTextLength);
        composedTextParams.resetComposedText();
    }

    private int getCaretPosition() {
        return textKit.getCaret().getDot();
    }

    private ComposedTextParams getComposedTextParams() {
        return TextUtils.getComposedTextParams(textKit);
    }
}

