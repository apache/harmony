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
import javax.swing.SwingTestCase;
import org.apache.harmony.awt.text.ComposedTextParams;
import org.apache.harmony.awt.text.PropertyNames;

public class JTextComponent_IMTest extends SwingTestCase {
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
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    private InputMethodEvent getCaretEvent(AttributedCharacterIterator text,
            int committedCharacterCount, TextHitInfo caret, TextHitInfo visiblePosition) {
        return getEvent(InputMethodEvent.CARET_POSITION_CHANGED, text, committedCharacterCount,
                caret, visiblePosition);
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
                if (value != null) {
                    ComposedTextParams params = (ComposedTextParams) value;
                    assertNull(params.getComposedText());
                    assertEquals(0, params.getComposedTextLength());
                }
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

    public void testProcessItME_Caret() {
        setComposedText();
        for (int i = 0; i < 10; i++) {
            int pos = Math.min(12, i + 7);
            ime = getCaretEvent(null, 0, TextHitInfo.afterOffset(i), null);
            jta.processInputMethodEvent(ime);
            assertEquals(pos, jta.getCaretPosition());
            ime = getCaretEvent(null, 0, TextHitInfo.beforeOffset(i), null);
            jta.processInputMethodEvent(ime);
            assertEquals(pos, jta.getCaretPosition());
        }
    }

    public void testProcessIME_Text_NPE() {
        String content = "12345";
        iter = getIterator(content, putSegmentAttribute(map, Color.RED));
        ime = getTextEvent(null, 0, null, null);
        jta.processInputMethodEvent(ime);
        checkComposedTextParams(false, 7, 5);
    }

    public void testProcessIME_Text() {
        String content = "12345";
        iter = getIterator(content, putSegmentAttribute(map, Color.RED));
        ime = getTextEvent(iter, 0, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
        jta.processInputMethodEvent(ime);
        assertEquals(7, jta.getCaretPosition());
        checkComposedTextParams(true, 7, 5);
        assertEquals(initialContent + content, jta.getText());
        content = "abcdett";
        iter = getIterator(content, putSegmentAttribute(map, Color.BLUE));
        ime = getTextEvent(iter, 0, TextHitInfo.afterOffset(1), TextHitInfo.afterOffset(1));
        jta.processInputMethodEvent(ime);
        assertEquals(8, jta.getCaretPosition());
        checkComposedTextParams(true, 7, 7);
        assertEquals(initialContent + content, jta.getText());
        //====================================================
        content = "composite attributes";
        Map<Attribute, Object> map1 = new HashMap<Attribute, Object>();
        putSegmentAttribute(map1, Color.GRAY);
        attrString = new AttributedString(content, map1);
        attrString.addAttribute(SEGMENT_ATTRIBUTE, Color.YELLOW);
        iter = attrString.getIterator();
        ime = getTextEvent(iter, 0, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
        jta.processInputMethodEvent(ime);
        assertEquals(7, jta.getCaretPosition());
        checkComposedTextParams(true, 7, 20);
        assertEquals(initialContent + content, jta.getText());
        //====================================================
        content = "fghij";
        iter = getIterator(content, putSegmentAttribute(map, Color.BLACK));
        ime = getTextEvent(iter, 5, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
        jta.processInputMethodEvent(ime);
        assertEquals(12, jta.getCaretPosition());
        checkComposedTextParams(false, 0, 0);
        assertEquals(initialContent + content, jta.getText());
        String lastCommited = content;
        content = "finish";
        iter = getIterator(content, putSegmentAttribute(map, Color.PINK));
        ime = getTextEvent(iter, 2, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
        jta.processInputMethodEvent(ime);
        assertEquals(14, jta.getCaretPosition());
        checkComposedTextParams(true, 14, 4);
        assertEquals(initialContent + lastCommited + content, jta.getText());
    }

    //InputMethodRequest tests==========================================
    public void testCancelLatestCommittedText() {
        AttributedCharacterIterator.Attribute[] attributes = new AttributedCharacterIterator.Attribute[] { SEGMENT_ATTRIBUTE };
        assertNull(imr.cancelLatestCommittedText(attributes));
        String content = "fghij";
        iter = getIterator(content, putSegmentAttribute(map, Color.BLACK));
        ime = getTextEvent(iter, 5, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
        jta.processInputMethodEvent(ime);
        assertEquals(initialContent + content, jta.getText());
        imr.cancelLatestCommittedText(attributes);
        assertEquals(initialContent, jta.getText());
        content = "klnoprst";
        iter = getIterator(content, putSegmentAttribute(map, Color.BLACK));
        ime = getTextEvent(iter, 5, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
        jta.processInputMethodEvent(ime);
        assertEquals(initialContent + content, jta.getText());
        imr.cancelLatestCommittedText(attributes);
        assertEquals(initialContent + content.substring(5, content.length()), jta.getText());
    }

    private void checkIterator(final AttributedCharacterIterator iterator,
            final int attributesCount, final int start, final int end) {
        assertEquals(attributesCount, iterator.getAttributes().size());
        assertEquals(start, iterator.getBeginIndex());
        assertEquals(end, iterator.getEndIndex());
    }

    private void checkException(final String text) {
        assertTrue(bWasException);
        assertEquals(text, message);
        bWasException = false;
        message = null;
    }

    public void testGetCommittedText() {
        assertEquals(7, imr.getCommittedTextLength());
        String content = "fghij";
        iter = getIterator(content, putSegmentAttribute(map, Color.BLACK));
        ime = getTextEvent(iter, 5, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
        jta.processInputMethodEvent(ime);
        checkIterator(imr.getCommittedText(0, 12, null), 0, 0, 12);
        checkIterator(imr.getCommittedText(2, 6, null), 0, 0, 4);
        jta.setCaretPosition(3);
        iter = getIterator("finish", putSegmentAttribute(map, Color.PINK));
        ime = getTextEvent(iter, 2, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
        jta.processInputMethodEvent(ime);
        checkIterator(imr.getCommittedText(0, 14, null), 0, 0, 14);
        AttributedCharacterIterator it = imr.getCommittedText(0, 14, null);
        String committedText = "IM fitestfghij";
        for (int i = 0; i < 14; i++) {
            it.setIndex(i);
            assertEquals(committedText.charAt(i), it.current());
        }
        checkIterator(imr.getCommittedText(3, 8, null), 0, 0, 5);
        try {
            imr.getCommittedText(2, 20, null);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        checkException("Invalid range");
        try {
            imr.getCommittedText(5, 2, null);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        checkException("Invalid range");
    }

    public void testGetCommittedTextLength() {
        assertEquals(7, imr.getCommittedTextLength());
        iter = getIterator("fghij", putSegmentAttribute(map, Color.BLACK));
        ime = getTextEvent(iter, 5, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
        jta.processInputMethodEvent(ime);
        assertEquals(12, imr.getCommittedTextLength());
        iter = getIterator("finish", putSegmentAttribute(map, Color.PINK));
        ime = getTextEvent(iter, 2, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
        jta.processInputMethodEvent(ime);
        assertEquals(14, imr.getCommittedTextLength());
    }

    public void testGetInsertPositionOffset() {
        setComposedText();
        assertEquals(7, imr.getInsertPositionOffset());
        iter = getIterator("finish", putSegmentAttribute(map, Color.PINK));
        ime = getTextEvent(iter, 2, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
        jta.processInputMethodEvent(ime);
        assertEquals(9, imr.getInsertPositionOffset());
    }

    public void testGetSelectedText() {
        assertNull(imr.getSelectedText(null));
        jta.select(2, 5);
        checkIterator(imr.getSelectedText(null), 0, 0, 3);
        jta.setCaretPosition(7);
        setComposedText();
        jta.select(9, 11);
        checkIterator(imr.getSelectedText(null), 0, 0, 2);
        jta.select(2, 10);
        checkIterator(imr.getSelectedText(null), 0, 0, 8);
    }

    public void testDoubleCancelLatestCommittedText() {
        String content = "fghij";
        iter = getIterator(content, putSegmentAttribute(map, Color.BLACK));
        ime = getTextEvent(iter, 5, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
        jta.processInputMethodEvent(ime);
        content = "klnoprst";
        iter = getIterator(content, putSegmentAttribute(map, Color.BLACK));
        ime = getTextEvent(iter, 5, TextHitInfo.afterOffset(0), TextHitInfo.afterOffset(0));
        jta.processInputMethodEvent(ime);
        assertNotNull(imr.cancelLatestCommittedText(null));
        assertNull(imr.cancelLatestCommittedText(null));
        assertEquals("IM testfghijrst", jta.getText());
    }
}
