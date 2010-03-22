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
package javax.swing.text;

import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Position.Bias;
import javax.swing.text.PlainViewI18N_LineViewTest.PlainViewI18NWithTextArea;
import junit.framework.TestCase;

public class PlainViewI18N_BidiTextViewTest extends TestCase implements DocumentListener {
    private View view;

    private PlainViewI18N parent;

    private PlainDocument doc;

    private Element root;

    private int startOffset;

    private int endOffset;

    private FontMetrics metrics;

    private final Rectangle shape = new Rectangle(27, 74, 91, 41);

    private DocumentEvent insertEvent;

    private DocumentEvent removeEvent;;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.insertString(0, PlainViewI18N_LineViewTest.defText, null);
        root = doc.getDefaultRootElement();
        startOffset = root.getElement(1).getStartOffset() + 1;
        endOffset = root.getElement(3).getEndOffset() - 1;
        parent = new PlainViewI18NWithTextArea(root, doc);
    }

    public void testGetEndOffset() {
        view = parent.new LTRTextView(root, startOffset, endOffset);
        assertEquals(startOffset, view.getStartOffset());
        assertEquals(endOffset, view.getEndOffset());
    }

    public void testGetStartOffset() {
        view = parent.new RTLTextView(root, startOffset, endOffset);
        assertEquals(startOffset, view.getStartOffset());
        assertEquals(endOffset, view.getEndOffset());
    }

    private void init() throws BadLocationException {
        view.setParent(parent);
        Container container = view.getContainer();
        metrics = container.getFontMetrics(container.getFont());
        shape.width = getTextWidth(startOffset, endOffset - startOffset);
        shape.height = metrics.getHeight();
        parent.setSize(shape.width, shape.height);
    }

    private String getText(int offset, int length) throws BadLocationException {
        return view.getDocument().getText(offset, length);
    }

    private int getTextWidth(int offset, int length) throws BadLocationException {
        return getTextWidth(getText(offset, length));
    }

    private int getTextWidth(String text) {
        return metrics.stringWidth(text);
    }

    private void setOffsets(Element element) {
        startOffset = element.getStartOffset();
        endOffset = element.getEndOffset();
    }

    public void testModelToViewLTR() throws BadLocationException {
        final Element line = root.getElement(0);
        setOffsets(line);
        view = parent.new LTRTextView(line, startOffset, endOffset);
        init();
        assertEquals(new Rectangle(shape.x, shape.y, 1, shape.height), view.modelToView(
                startOffset, shape, Bias.Forward));
        assertEquals(new Rectangle(shape.x + getTextWidth(startOffset, 1), shape.y, 1,
                shape.height), view.modelToView(startOffset + 1, shape, Bias.Forward));
        assertEquals(new Rectangle(
                shape.x + getTextWidth(startOffset, endOffset - startOffset), shape.y, 1,
                shape.height), view.modelToView(endOffset, shape, Bias.Forward));
        // Invalid values
        try {
            view.modelToView(startOffset - 1, shape, Bias.Forward);
            fail("BadLocationException must be thrown");
        } catch (BadLocationException e) {
        }
        try {
            view.modelToView(endOffset + 1, shape, Bias.Forward);
            fail("BadLocationException must be thrown");
        } catch (BadLocationException e) {
        }
    }

    public void testModelToViewRTL() throws BadLocationException {
        final Element line = root.getElement(1);
        setOffsets(line);
        view = parent.new RTLTextView(line, startOffset, endOffset);
        init();
        assertEquals(new Rectangle(shape.x
        // all RTLtext (both symbols)
                + getTextWidth(startOffset, endOffset - startOffset - 1), shape.y, 1,
                shape.height), view.modelToView(startOffset, shape, Bias.Forward));
        assertEquals(new Rectangle(shape.x
        // the second symbol in the model
                + getTextWidth(startOffset + 1, endOffset - startOffset - 2), shape.y, 1,
                shape.height), view.modelToView(startOffset + 1, shape, Bias.Forward));
        // no text at all
        assertEquals(new Rectangle(shape.x, shape.y, 1, shape.height), view.modelToView(
                endOffset, shape, Bias.Forward));
        // Invalid values
        try {
            view.modelToView(startOffset - 1, shape, Bias.Forward);
            fail("BadLocationException must be thrown");
        } catch (BadLocationException e) {
        }
        try {
            view.modelToView(endOffset + 1, shape, Bias.Forward);
            fail("BadLocationException must be thrown");
        } catch (BadLocationException e) {
        }
    }

    public static class PreferenceChange {
        private final View child;

        private final boolean width;

        private final boolean height;

        public PreferenceChange(View child, boolean width, boolean height) {
            this.child = child;
            this.width = width;
            this.height = height;
        }

        public void check(View child, boolean width, boolean height) {
            assertSame("Unexpected child", child, this.child);
            assertEquals("Width", width, this.width);
            assertEquals("Height", height, this.height);
        }
    }

    private PreferenceChange preferenceParams;

    public void testInsertUpdateLTR() throws Exception {
        final Element line = root.getElement(0);
        setOffsets(line);
        view = parent.new LTRTextView(line, startOffset, endOffset) {
            @Override
            public void preferenceChanged(View child, boolean w, boolean h) {
                preferenceParams = new PreferenceChange(child, w, h);
            }
        };
        init();
        doc.addDocumentListener(this);
        doc.insertString(startOffset + 1, PlainViewI18N_LineViewTest.LTR, null);
        view.insertUpdate(insertEvent, shape, null);
        preferenceParams.check(view, true, false);
    }

    public void testInsertUpdateRTL() throws Exception {
        final Element line = root.getElement(1);
        setOffsets(line);
        view = parent.new RTLTextView(line, startOffset, endOffset) {
            @Override
            public void preferenceChanged(View child, boolean w, boolean h) {
                preferenceParams = new PreferenceChange(child, w, h);
            }
        };
        init();
        doc.addDocumentListener(this);
        doc.insertString(startOffset + 1, PlainViewI18N_LineViewTest.RTL, null);
        view.insertUpdate(insertEvent, shape, null);
        preferenceParams.check(view, true, false);
    }

    public void testRemoveUpdateLTR() throws Exception {
        final Element line = root.getElement(0);
        setOffsets(line);
        view = parent.new LTRTextView(line, startOffset, endOffset) {
            @Override
            public void preferenceChanged(View child, boolean w, boolean h) {
                preferenceParams = new PreferenceChange(child, w, h);
            }
        };
        init();
        doc.addDocumentListener(this);
        doc.remove(startOffset + 1, 1);
        view.removeUpdate(removeEvent, shape, null);
        preferenceParams.check(view, true, false);
    }

    public void testRemoveUpdateRTL() throws Exception {
        final Element line = root.getElement(1);
        setOffsets(line);
        view = parent.new RTLTextView(line, startOffset, endOffset) {
            @Override
            public void preferenceChanged(View child, boolean w, boolean h) {
                preferenceParams = new PreferenceChange(child, w, h);
            }
        };
        init();
        doc.addDocumentListener(this);
        doc.remove(startOffset + 1, 1);
        view.removeUpdate(removeEvent, shape, null);
        preferenceParams.check(view, true, false);
    }

    public void changedUpdate(DocumentEvent event) {
    }

    public void insertUpdate(DocumentEvent event) {
        insertEvent = event;
    }

    public void removeUpdate(DocumentEvent event) {
        removeEvent = event;
    }
    //    public void testBidiTextView() {
    //    }
}
