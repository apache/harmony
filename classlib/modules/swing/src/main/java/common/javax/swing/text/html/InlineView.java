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
 * @author Alexey A. Ivanov
 */
package javax.swing.text.html;

import java.awt.Shape;
import java.text.BreakIterator;
import java.text.CharacterIterator;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.Segment;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

public class InlineView extends LabelView {
    private AttributeSet attrs;
    private boolean canBreak;

    public InlineView(final Element elem) {
        super(elem);
        canBreak = canBreak();
    }

    public int getBreakWeight(final int axis, final float x, final float len) {
        if (!canBreak) {
            return BadBreakWeight;
        }
        return super.getBreakWeight(axis, x, len);
    }

    public View breakView(final int axis, final int startOffset,
                          final float x, final float len) {
        if (!canBreak) {
            return this;
        }
        return super.breakView(axis, startOffset, x, len);
    }

    public AttributeSet getAttributes() {
        if (attrs == null) {
            attrs = getStyleSheet().getViewAttributes(this);
        }
        return attrs;
    }

    public void changedUpdate(final DocumentEvent event, final Shape alloc,
                              final ViewFactory factory) {
        attrs = getStyleSheet().getViewAttributes(this);
        canBreak = canBreak();
        super.changedUpdate(event, alloc, factory);
    }

    protected StyleSheet getStyleSheet() {
        return ((HTMLDocument)getDocument()).getStyleSheet();
    }

    protected void setPropertiesFromAttributes() {
        // TODO setPropertiesFromAttrs: 'text-decoration' for overline, blink
        // TODO setPropertiesFromAttrs: 'vertical-align' for alignment???
        super.setPropertiesFromAttributes();
        canBreak = canBreak();
    }

    final int getLongestWordSpan() {
        int result = 0;

        final int startOffset = getStartOffset();
        final int endOffset = getEndOffset();

        final BreakIterator bi = BreakIterator.getWordInstance();
        final Segment text = getText(startOffset, endOffset);
        bi.setText(text);

        int prev;
        int curr = bi.first() - text.offset;
        int next;
        do {
            prev = curr;
            next = bi.next();
            curr = next != BreakIterator.DONE ? next - text.offset
                                              : endOffset - startOffset;
            if (!isWhitespace(getText(startOffset + prev,
                                      startOffset + curr))) {
                result = Math.max(result,
                                  (int)getPartialSpan(startOffset + prev,
                                                      startOffset + curr));
            }
        } while (next != BreakIterator.DONE);

        return result;
    }

    private boolean canBreak() {
        Object ws = getAttributes().getAttribute(CSS.Attribute.WHITE_SPACE);
        return ws == null
               || ((CSS.WhiteSpace)ws).getIndex() != CSS.WhiteSpace.NOWRAP;
    }

    // TODO refactor isWhitespace: text.GlyphView and text.html.InlineView
    private static boolean isWhitespace(final CharacterIterator text) {
        boolean result;
        char c = text.first();
        do {
            result = Character.isWhitespace(c);
            c = text.next();
        } while (result && c != CharacterIterator.DONE);
        return result;
    }
}
