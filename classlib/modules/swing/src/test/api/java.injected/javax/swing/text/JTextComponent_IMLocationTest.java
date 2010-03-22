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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputMethodEvent;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingWaitTestCase;
import org.apache.harmony.awt.text.ComposedTextParams;
import org.apache.harmony.awt.text.PropertyNames;

public class JTextComponent_IMLocationTest extends SwingWaitTestCase {
    JTextArea jta;

    JFrame jf;

    InputMethodEvent ime;

    Map<Attribute, Object> map;

    AbstractDocument doc;

    AttributedCharacterIterator iter;

    AttributedString attrString;

    static final AttributedCharacterIterator.Attribute SEGMENT_ATTRIBUTE = AttributedCharacterIterator.Attribute.INPUT_METHOD_SEGMENT;

    static final String initialContent = "IM test";

    boolean bWasException;

    InputMethodRequests imr;

    String message;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        map = new HashMap<Attribute, Object>();
        jf = new JFrame();
        jta = new JTextArea();
        jta.setText(initialContent);
        doc = (AbstractDocument) jta.getDocument();
        imr = jta.getInputMethodRequests();
        bWasException = false;
        message = null;
        jf.getContentPane().add(jta);
        jf.setSize(200, 300);
        jf.setVisible(true);
        component = jf;
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    private InputMethodEvent getTextEvent(AttributedCharacterIterator text,
            int committedCharacterCount, TextHitInfo caret, TextHitInfo visiblePosition) {
        return getEvent(InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, text,
                committedCharacterCount, caret, visiblePosition);
    }

    private InputMethodEvent getEvent(final int id, final AttributedCharacterIterator text,
            final int committedCharacterCount, final TextHitInfo caret,
            final TextHitInfo visiblePosition) {
        return new InputMethodEvent(jta, id, text, committedCharacterCount, caret,
                visiblePosition);
    }

    private Map<Attribute, Object> putSegmentAttribute(final Map<Attribute, Object> map, final Object value) {
        map.put(SEGMENT_ATTRIBUTE, value);
        return map;
    }

    private AttributedCharacterIterator getIterator(final String text, final Map<Attribute, Object> map) {
        attrString = new AttributedString(text, map);
        return attrString.getIterator();
    }

    private void setComposedText() {
        String content = "12345";
        iter = getIterator(content, putSegmentAttribute(map, Color.RED));
        ime = getTextEvent(iter, 0, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
        jta.processInputMethodEvent(ime);
        assertEquals(7, jta.getCaretPosition());
        checkComposedTextParams(true, 7, 5);
        assertEquals(initialContent + content, jta.getText());
    }

    private void checkComposedTextParams(final boolean shouldBe, final int start,
            final int length) {
        if (isHarmony()) {
            Object value = doc.getProperty(PropertyNames.COMPOSED_TEXT_PROPERTY);
            if (!shouldBe) {
                assertNull(value);
                return;
            }
            assertTrue(value instanceof ComposedTextParams);
            ComposedTextParams params = (ComposedTextParams) value;
            assertEquals(start, params.getComposedTextStart());
            assertEquals(length, params.getComposedTextLength());
            AttributedString text = params.getComposedText();
            AttributedCharacterIterator iter1 = attrString.getIterator();
            AttributedCharacterIterator iter2 = text.getIterator();
            assertEquals(iter1.getAttributes(), iter2.getAttributes());
            assertEquals(iter1.getRunStart(SEGMENT_ATTRIBUTE), iter2
                    .getRunStart(SEGMENT_ATTRIBUTE));
            assertEquals(Math.min(iter1.getRunLimit(SEGMENT_ATTRIBUTE), iter2.getEndIndex()),
                    iter2.getRunLimit(SEGMENT_ATTRIBUTE));
        }
    }

    public void testGetLocationOffset() {
        try {
            setComposedText();
            Rectangle rect;
            Point location = jta.getLocationOnScreen();
            for (int i = 0; i < 7; i++) {
                rect = jta.modelToView(i);
                rect.translate(location.x, location.y);
                assertNull(imr.getLocationOffset(rect.x, rect.y));
            }
            for (int i = 7; i < 13; i++) {
                rect = jta.modelToView(i);
                rect.translate(location.x, location.y);
                assertEquals(TextHitInfo.afterOffset(i - 7), imr.getLocationOffset(rect.x,
                        rect.y));
            }
        } catch (BadLocationException e) {
        }
    }

    public void testGetTextLocation() {
        try {
            setComposedText();
            int pos = 7;
            Rectangle rect = jta.modelToView(pos);
            Point location = jta.getLocationOnScreen();
            rect.translate(location.x, location.y);
            for (int i = 0; i < 10; i++) {
                assertEquals(rect, imr.getTextLocation(TextHitInfo.beforeOffset(i)));
                assertEquals(rect, imr.getTextLocation(TextHitInfo.afterOffset(i)));
            }
            iter = getIterator("klnoprst", putSegmentAttribute(map, Color.BLACK));
            ime = getTextEvent(iter, 5, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
            jta.processInputMethodEvent(ime);
            pos = 12;
            rect = jta.modelToView(pos);
            rect.translate(location.x, location.y);
            for (int i = 0; i < 10; i++) {
                assertEquals(rect, imr.getTextLocation(TextHitInfo.beforeOffset(i)));
                assertEquals(rect, imr.getTextLocation(TextHitInfo.afterOffset(i)));
            }
        } catch (BadLocationException e) {
            assertFalse("unexpectedException", true);
        }
    }
}
