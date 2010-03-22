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

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;

import org.apache.harmony.awt.internal.nls.Messages;

public class InputMethodRequestsImpl implements InputMethodRequests {
    private TextKit textKit;
    public InputMethodRequestsImpl(final TextKit textKit) {
       this.textKit = textKit;
    }
    /**
     * Uses by getCommittedText. Joins two iterators. Supposes
     * that iterators ranges doesn't crpss.
     */
    final class IteratorConcatination implements
                AttributedCharacterIterator {
        AttributedCharacterIterator iterator = null;
        AttributedCharacterIterator first;
        AttributedCharacterIterator second;
        int start;
        int end;
        int gapStart;
        int gapEnd;
        int index;
        private static final boolean FORWARD = true;
        private static final boolean BACKWARD = false;

        public IteratorConcatination(final AttributedCharacterIterator
                                     iterator1,
                                     final AttributedCharacterIterator
                                     iterator2) {
            int begin1 = iterator1.getBeginIndex();
            int begin2 = iterator2.getBeginIndex();
            int end1 = iterator1.getEndIndex();
            int end2 = iterator2.getEndIndex();
            iterator = (begin1 < begin2) ? iterator1 : iterator2;
            first = (begin1 < begin2) ? iterator1 : iterator2;
            second = (begin1 < begin2) ? iterator2 : iterator1;
            start = Math.min(begin1, begin2);
            end = Math.max(end1, end2);
            gapStart = Math.min(end1, end2);
            gapEnd = Math.max(begin1, begin2);
            index = start;

        }

        public char current() {
            return iterator.current();
        }

        public char first() {
            return changeIndex(start);
        }

        @Override
        public Object clone() {
            try {
                IteratorConcatination newIterator = (IteratorConcatination)
                                                              super.clone();
                return newIterator;
            } catch (final CloneNotSupportedException e) {
                return null;
            }
        }

        public Set<Attribute> getAllAttributeKeys() {
            HashSet<Attribute> attributeKeys = new HashSet<Attribute>(first
                    .getAllAttributeKeys());
            attributeKeys.addAll(second.getAllAttributeKeys());
            return attributeKeys;
        }

        public Object getAttribute(final Attribute attribute) {
            return iterator.getAttribute(attribute);
        }

        public Map<Attribute, Object> getAttributes() {
            return iterator.getAttributes();
        }

        public int getBeginIndex() {
            return first.getBeginIndex();
        }

        public int getEndIndex() {
            return second.getEndIndex();
        }

        public int getIndex() {
            return iterator.getIndex();
        }

        public int getRunLimit() {
            return iterator.getRunLimit();
        }

        public int getRunLimit(final Attribute attribute) {
            return iterator.getRunLimit(attribute);
        }

        public int getRunLimit(final Set<? extends Attribute> set) {
            return iterator.getRunLimit(set);
        }

        public int getRunStart() {
            return iterator.getRunStart();
        }

        public int getRunStart(final Attribute attribute) {
            return iterator.getRunStart(attribute);
        }

        public int getRunStart(final Set<? extends Attribute> set) {
            return iterator.getRunStart(set);
        }

        public char last() {
            return changeIndex(end);
        }

        public char next() {
            return index < end ? changeIndex(index + 1, FORWARD) : DONE;
        }

        public char previous() {
            return index > start ? changeIndex(index - 1, BACKWARD) : DONE;
        }

        public char setIndex(final int ind) {
            if (!inRange(ind)) {
                // awt.28=bad index: {0}
                throw new IllegalArgumentException(Messages.getString("awt.28", ind)); //$NON-NLS-1$
            }
            return changeIndex(ind, BACKWARD);
        }

        boolean inGap(final int offset) {
            return (offset >= gapStart && offset < gapEnd);
        }

        boolean inRange(final int offset) {
            return (offset >= start && offset < end);
        }

        char changeIndex(final int newIndex) {
            return changeIndex(newIndex, FORWARD);
        }

        char changeIndex(final AttributedCharacterIterator iter,
                         final int ind) {
            iterator = iter;
            index = ind;
            return iterator.setIndex(ind);
        }

        char changeIndex(final int newIndex, final boolean direction) {
            if (inGap(newIndex)) {
                return (direction) ? changeIndex(second, gapEnd)
                        : changeIndex(first, gapStart - 1);
            }
            return newIndex < gapStart ? changeIndex(first, newIndex)
                    : changeIndex(second, newIndex);
        }
    }

    public AttributedCharacterIterator cancelLatestCommittedText(
            final AttributedCharacterIterator.Attribute[] attributes) {

        ComposedTextParams composedTextParams = getComposedTextParams();
        int lastCommittedTextLength = composedTextParams
            .getLastCommittedTextLength();
        int lastCommittedTextStart = composedTextParams
            .getLastCommittedTextStart();
        if (lastCommittedTextLength == 0) {
            return null;
        }

        String committedText;
        try {
            committedText = getText(lastCommittedTextStart,
                                    lastCommittedTextLength);
            textKit.getDocument().remove(lastCommittedTextStart,
                            lastCommittedTextLength);
        } catch (BadLocationException e) {
            return null;
        }
        return getSimpleIterator(committedText);
    }

    public AttributedCharacterIterator
        getCommittedText(final int beginIndex, final int endIndex,
                     final AttributedCharacterIterator.Attribute[]
                                                       attributes) {
        if (beginIndex < 0 || endIndex < beginIndex
                || endIndex > textKit.getDocument().getLength()) {
            // awt.29=Invalid range
            throw new IllegalArgumentException(Messages.getString("awt.29")); //$NON-NLS-1$
        }
        AttributedCharacterIterator result = null;
        ComposedTextParams composedTextParams = getComposedTextParams();
        int lastInsertPosition = composedTextParams.getComposedTextStart();
        int composedTextLength = composedTextParams.getComposedTextLength();
        try {
            int composedTextEnd = lastInsertPosition + composedTextLength;
            int start = Math.min(lastInsertPosition,
                                beginIndex);
            int end = Math.min(endIndex, lastInsertPosition);
            String textBefore = getText(start, end - start);
            String textAfter = null;
            end = endIndex + composedTextLength;
            if (composedTextEnd < endIndex + composedTextLength) {
                start = composedTextEnd;
                textAfter = getText(start, end - start);
            }
            result = getSimpleIterator(textBefore, textAfter);
        } catch (BadLocationException e) {
        }
        return result;
    }

    public int getCommittedTextLength() {
        return textKit.getDocument().getLength()
           - getComposedTextParams().getComposedTextLength();
    }

    public int getInsertPositionOffset() {
        return getComposedTextParams().getComposedTextStart();
    }

    public TextHitInfo getLocationOffset(final int x, final int y) {
        Point p = new Point(x, y);
        final Component component = textKit.getComponent();
        if (!component.isDisplayable()) {
            return null;
        }
        Point location = component.getLocationOnScreen();
        p.translate(-location.x, -location.y);
        if (!component.contains(p)) {
            // Return null if the location is outside the component.
            return null;
        }
        int offset = textKit.viewToModel(p, new Position.Bias[1]);
        ComposedTextParams composedTextParams = getComposedTextParams();
        int lastInsertPosition = composedTextParams.getComposedTextStart();
        int composedTextLength = composedTextParams.getComposedTextLength();
        if (composedTextLength == 0) {
            return null;
        } else if (offset >= lastInsertPosition
                && offset <=  lastInsertPosition + composedTextLength) {
            return TextHitInfo.afterOffset(offset - lastInsertPosition);
        } else {
            return null;
        }
    }

    public AttributedCharacterIterator getSelectedText(
            final AttributedCharacterIterator.Attribute[] attributes) {
        return getSimpleIterator(textKit.getSelectedText());
    }

    AttributedCharacterIterator getIterator(final int start,
                                            final int end) {
        AttributedString s = getAttributedString(start, end);
        return (s == null) ? null : s.getIterator(null, start, end);
    }

    AttributedString getAttributedString(final int start, final int end) {
        String s = null;
        try {
            s = getText(0, end);
        } catch (final BadLocationException e) { }
        if (s == null || start == end) {
            return null;
        }
        AttributedString attributedString = new AttributedString(s);
        for (int i = start; i < end; i++) {
            AttributeSet ass = getAttributeSet(i);
            Enumeration<?> names;
            for (names = ass.getAttributeNames();
                 names.hasMoreElements();) {
                Object key = names.nextElement();
                if (key instanceof AttributedCharacterIterator.Attribute) {
                    AttributedCharacterIterator.Attribute attribute =
                        (AttributedCharacterIterator.Attribute)key;
                    Object value = ass.getAttribute(key);
                    attributedString.addAttribute(attribute, value,
                                                  i - start,
                                                  i + 1 - start);
                }
            }
        }
        return attributedString;
    }

    AttributeSet getAttributeSet(final int offset) {
        Document doc = textKit.getDocument();
        Element elem = getLeafElement(doc.getDefaultRootElement(), offset);
        return (elem == null) ? null : elem.getAttributes();
    }

    Element getLeafElement(final Element element, final int offset) {
        int count = element.getElementCount();
        if (count == 0) {
            return element;
        }
        int index = element.getElementIndex(offset);
        return index < 0 ? element : getLeafElement(element.
                                                    getElement(index),
                                                    offset);
    }


    public Rectangle getTextLocation(final TextHitInfo offset) {
        Point location = textKit.getComponent().getLocationOnScreen();
        Rectangle rect = null;
        try {
            rect = textKit.modelToView(getComposedTextParams()
                                       .getComposedTextStart());
            rect.translate(location.x, location.y);
        } catch (BadLocationException e) {
        }
        return rect;
    }

    private AttributedCharacterIterator
         getSimpleIterator(final String text) {
        return (text == null) ? null
                : new AttributedString(text).getIterator();
    }

    private AttributedCharacterIterator
         getSimpleIterator(final String text1, final String text2) {
        if (text1 == null) {
            return getSimpleIterator(text2);
        } else if (text2 == null) {
            return getSimpleIterator(text1);
        } else {
            return getSimpleIterator(text1 + text2);
        }
    }

    private ComposedTextParams getComposedTextParams() {
        return TextUtils.getComposedTextParams(textKit);
    }
    private String getText(final int pos, final int length)
        throws BadLocationException {
        return textKit.getDocument().getText(pos, length);
    }
}
