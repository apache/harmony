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

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.text.BreakIterator;
import java.text.CharacterIterator;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingTestCase;
import javax.swing.plaf.basic.BasicTextUI;

public class UtilitiesTest extends SwingTestCase {
    JTextComponent textComponent;

    TabExp te = new TabExp();

    JFrame jf;

    //not implemented
    //JEditorPane jtp;
    //not implemented
    //JTextPane jtp;
    JTextArea jta;

    JTextField jtf;

    Document doc_jta;

    Document doc_jtp;

    Document doc_jtf;

    String sLTR = "aaaa";

    String sRTL = "\u05dc" + "\u05dc" + "\u05dc" + "\u05dc";

    boolean bWasException;

    String message;

    class TabExp implements TabExpander {
        public float nextTabStop(final float f, final int i) {
            //System.out.println(" " + f + " " + i);
            return f + i;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        timeoutDelay = 10 * DEFAULT_TIMEOUT_DELAY;
        bWasException = false;
        message = null;
        String content = sRTL + "\t" + sLTR + "\t" + sRTL + "\t\t" + sLTR + "\n" + sLTR + "\t "
                + sLTR + " \t " + sLTR + sRTL + sRTL + sRTL + sRTL + "\n" + sRTL + " " + sRTL
                + "  " + sRTL + "\n" + sRTL + " " + sRTL + "; " + sRTL + "\n" + sRTL + ";"
                + sLTR + " \t" + sRTL + "\t" + sRTL + " " + sRTL + ",";
        jf = new JFrame();
        jta = new JTextArea(content);
        jtf = new JTextField(content);
        //jtp = new JTextPane();
        //jtp.setText(content);
        doc_jta = jta.getDocument();
        //doc_jtp = jtp.getDocument();
        doc_jtf = jtf.getDocument();
        //setContent();
        jf.getContentPane().setLayout(new GridLayout(2, 2));
        jf.getContentPane().add(jta);
        jf.getContentPane().add(jtf);
        //jf.getContentPane().add(jtp);
        jf.setSize(400, 500);
        jf.pack();
    }

    protected void setContent() {
        String str[] = { "Draws the given text, expanding any tabs",
                "that are contained using the given tab expansion technique",
                "Determines the width of the given segment of text taking",
                "Draws the given text, expanding any tabs", };
        SimpleAttributeSet[] attrs = new SimpleAttributeSet[4];
        attrs[0] = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs[0], "SansSerif");
        StyleConstants.setFontSize(attrs[0], 16);
        attrs[1] = new SimpleAttributeSet(attrs[0]);
        StyleConstants.setBold(attrs[1], true);
        attrs[2] = new SimpleAttributeSet(attrs[0]);
        StyleConstants.setItalic(attrs[2], true);
        attrs[3] = new SimpleAttributeSet(attrs[0]);
        StyleConstants.setFontSize(attrs[3], 20);
        try {
            for (int i = 0; i < str.length; i++) {
                doc_jtp.insertString(doc_jtp.getLength(), str[i] + "\n", attrs[i]);
            }
        } catch (BadLocationException ble) {
            System.err.println("Error");
        }
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    void checkWasException() {
        assertTrue(bWasException);
        assertEquals("No more words", message);
        bWasException = false;
        message = null;
    }

    void checkWasException(final String s) {
        assertTrue(bWasException);
        assertEquals(s, message);
        bWasException = false;
        message = null;
    }

    int getTabbedTextOffset(final Segment s, final FontMetrics fm, final int start,
            final int end, final TabExpander t, final int pos) {
        int res = start;
        boolean lastTab = true;
        int ret = 0;
        int tmp1 = 0;
        //TODO end < start
        for (char c = s.first(); c != CharacterIterator.DONE; c = s.next()) {
            if (c == '\t' && t == null) {
                c = ' ';
            }
            if (c == '\t') {
                tmp1 = res;
                res = (int) t.nextTabStop(res, s.getIndex() + pos - s.getBeginIndex());// + s.getIndex());
                lastTab = true;
            } else {
                res += fm.charWidth(c);
                lastTab = false;
            }
            int diff = (lastTab) ? (res - tmp1) : (fm.charWidth(c));
            int tail = (diff / 2) + 1;
            if (res < end + tail) {
                ret++;
            } else {
                return ret;
            }
        }
        return ret;
    }

    int getTabbedTextWidth(final Segment s, final FontMetrics fm, final int x,
            final TabExpander t, final int pos) {
        return getTabbedTextEnd(s, fm, x, t, pos) - x;
    }

    int getTabbedTextEnd(final Segment s, final FontMetrics fm, final int x,
            final TabExpander t, final int pos) {
        int res = x;
        String str = "";
        boolean isNullTabExpander = (t == null);
        int segmentOffset = pos - s.getBeginIndex();
        boolean isTab = false;
        for (char c = s.first(); c != CharacterIterator.DONE; c = s.next()) {
            isTab = (c == '\t');
            if (isTab && !isNullTabExpander) {
                res = (int) t.nextTabStop(fm.stringWidth(str) + res, s.getIndex()
                        + segmentOffset);
                str = "";
            } else {
                str += (isTab) ? ' ' : c;
                isTab = false;
            }
        }
        return isTab ? res : (res + fm.stringWidth(str));
    }

    void drawTabbedTextTest(final JTextComponent c) {
        //According to my experiments this is only for one line
        Document doc = c.getDocument();
        Graphics g = c.getGraphics();
        Element root = doc.getDefaultRootElement();
        Segment seg = new Segment();
        for (int i = 0; i < root.getElementCount(); i++) {
            Element currentElem = root.getElement(i);
            int start = currentElem.getStartOffset();
            int end = currentElem.getEndOffset();
            for (int j = start; j < end; j++) {
                try {
                    doc.getText(start, (end - start), seg);
                } catch (BadLocationException e) {
                    assertTrue("Unexpected Exception: " + e.getMessage(), false);
                }
                FontMetrics fm = g.getFontMetrics();
                assertEquals(Utilities.drawTabbedText(seg, 23, 24, g, te, j), getTabbedTextEnd(
                        seg, fm, 23, te, j));
                assertEquals(Utilities.drawTabbedText(seg, 23, 24, g, null, j),
                        getTabbedTextEnd(seg, fm, 23, null, j));
                assertEquals(Utilities.drawTabbedText(seg, 23, 24, g, te, 100),
                        getTabbedTextEnd(seg, fm, 23, te, 100));
                assertEquals(Utilities.drawTabbedText(seg, 23, 24, g, null, 100),
                        getTabbedTextEnd(seg, fm, 23, null, 100));
            }
        }
    }

    public void testDrawTabbedText() {
        drawTabbedTextTest(jta);
        drawTabbedTextTest(jtf);
        //drawTabbedTextTest(jtp);
    }

    public void getTabbedTextWidthTest(final JTextComponent c) {
        //According to my experiments this is only for one line
        Document doc = c.getDocument();
        Graphics g = c.getGraphics();
        Element root = doc.getDefaultRootElement();
        Segment seg = new Segment();
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < root.getElementCount(); i++) {
            Element currentElem = root.getElement(i);
            int start = currentElem.getStartOffset();
            int end = currentElem.getEndOffset();
            for (int j = start; j < end; j++) {
                try {
                    doc.getText(start, end - start, seg);
                } catch (BadLocationException e) {
                    assertTrue("Unexpected Exception: " + e.getMessage(), false);
                }
                assertEquals(Utilities.getTabbedTextWidth(seg, fm, 23, te, j),
                        getTabbedTextWidth(seg, fm, 23, te, j));
                assertEquals(Utilities.getTabbedTextWidth(seg, fm, 23, null, j),
                        getTabbedTextWidth(seg, fm, 23, null, j));
                assertEquals(Utilities.getTabbedTextWidth(seg, fm, 23, te, 100),
                        getTabbedTextWidth(seg, fm, 23, te, 100));
                assertEquals(Utilities.getTabbedTextWidth(seg, fm, 23, null, 100),
                        getTabbedTextWidth(seg, fm, 23, null, 100));
            }
        }
    }

    public void testGetTabbedTextWidth() {
        getTabbedTextWidthTest(jta);
        getTabbedTextWidthTest(jtf);
        //getTabbedTextWidthTest(jtp);
    }

    int getTabbedTextOffsetRound(final Segment s, final FontMetrics fm, final int start,
            final int end, final TabExpander t, final int pos, final boolean round) {
        String str = "";
        int segmentOffset = pos - s.getBeginIndex();
        boolean isTab = false;
        boolean isNullTabExpander = (t == null);
        int currentEnd = start;
        int currentIndex = 0;
        int prevEnd = start;
        int tabEnd = start;
        for (char c = s.first(); c != CharacterIterator.DONE; c = s.next()) {
            isTab = (c == '\t');
            if (isTab && !isNullTabExpander) {
                tabEnd = (int) t.nextTabStop(currentEnd, s.getIndex() + segmentOffset);
                str = "";
            } else {
                str += (isTab) ? ' ' : c;
                isTab = false;
            }
            currentEnd = isTab ? tabEnd : (tabEnd + fm.stringWidth(str));
            int delta = (round) ? (currentEnd - prevEnd) / 2 : 0;
            if (currentEnd > end + delta) {
                break;
            }
            currentIndex++;
            prevEnd = currentEnd;
        }
        return currentIndex;
    }

    void getTabbedTextOffsetRoundTest_BoundaryCases(final JTextComponent c) {
        Document doc = c.getDocument();
        Graphics g = c.getGraphics();
        assertNotNull(g);
        Element root = doc.getDefaultRootElement();
        Segment seg = new Segment();
        for (int i = 0; i < root.getElementCount(); i++) {
            Element currentElem = root.getElement(i);
            int start = currentElem.getStartOffset();
            int end = currentElem.getEndOffset();
            try {
                doc.getText(start, (end - start), seg);
            } catch (BadLocationException e) {
                assertTrue("Unexpected Exception: " + e.getMessage(), false);
            }
            int textOffset = start + 9;
            FontMetrics fm = getFontMetrics(c.getFont(), 10);
            int textWidth = getTabbedTextWidth(seg, fm, 23, te, textOffset);
            for (int k = -3; k < textWidth + 4; k = (k == 0) ? k = textWidth + 1 : k + 1) {
                int target = 23 + k;
                int offset = (k <= 0) ? 0 : seg.count;
                assertEquals(offset, Utilities.getTabbedTextOffset(seg, fm, 23, target, te,
                        textOffset, false));
                assertEquals(offset, Utilities.getTabbedTextOffset(seg, fm, 23, target, te,
                        textOffset, true));
                assertEquals(offset, Utilities.getTabbedTextOffset(seg, fm, 23, target, null,
                        textOffset, false));
                assertEquals(offset, Utilities.getTabbedTextOffset(seg, fm, 23, target, null,
                        textOffset, true));
            }
        }
    }

    public void testGetTabbedTextOffsetRound_BoundaryCases() {
        getTabbedTextOffsetRoundTest_BoundaryCases(jta);
        getTabbedTextOffsetRoundTest_BoundaryCases(jtf);
        //getTabbedTextOffsetRoundTest(jtp);
    }

    void getTabbedTextOffsetRoundTest(final JTextComponent c) {
        Document doc = c.getDocument();
        Graphics g = c.getGraphics();
        Element root = doc.getDefaultRootElement();
        Segment seg = new Segment();
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < root.getElementCount(); i++) {
            Element currentElem = root.getElement(i);
            int start = currentElem.getStartOffset();
            int end = currentElem.getEndOffset();
            try {
                doc.getText(start, (end - start), seg);
            } catch (BadLocationException e) {
                assertTrue("Unexpected Exception: " + e.getMessage(), false);
            }
            int offset = start + 8;
            int textWidth = Utilities.getTabbedTextWidth(seg, fm, 23, te, offset);
            for (int k = 0; k < textWidth; k++) {
                int target = 23 + k;
                assertEquals(getTabbedTextOffsetRound(seg, fm, 23, target, te, offset, false),
                        Utilities.getTabbedTextOffset(seg, fm, 23, target, te, offset, false));
                assertEquals(getTabbedTextOffsetRound(seg, fm, 23, target, te, offset, true),
                        Utilities.getTabbedTextOffset(seg, fm, 23, target, te, offset, true));
            }
        }
    }

    public void testGetTabbedTextOffsetRound() {
        getTabbedTextOffsetRoundTest(jta);
        getTabbedTextOffsetRoundTest(jtf);
        //getTabbedTextOffsetRoundTest(jtp);
    }

    void getTabbedTextOffsetRoundTest_NoTabExpander(final JTextComponent c) {
        Document doc = c.getDocument();
        Graphics g = c.getGraphics();
        FontMetrics fm = g.getFontMetrics();
        Element root = doc.getDefaultRootElement();
        Segment seg = new Segment();
        for (int i = 0; i < root.getElementCount(); i++) {
            Element currentElem = root.getElement(i);
            int start = currentElem.getStartOffset();
            int end = currentElem.getEndOffset();
            int offset = start + 50;
            try {
                doc.getText(start, (end - start), seg);
            } catch (BadLocationException e) {
                assertTrue("Unexpected Exception: " + e.getMessage(), false);
            }
            int textWidth = Utilities.getTabbedTextWidth(seg, fm, 23, te, offset);
            for (int k = 0; k < textWidth; k += 100) {
                int target = 23 + k;
                assertEquals(
                        getTabbedTextOffsetRound(seg, fm, 23, target, null, offset, false),
                        Utilities.getTabbedTextOffset(seg, fm, 23, target, null, offset, false));
                assertEquals(getTabbedTextOffsetRound(seg, fm, 23, target, null, offset, true),
                        Utilities.getTabbedTextOffset(seg, fm, 23, target, null, offset, true));
            }
        }
    }

    public void testGetTabbedTextOffsetRound_NoTabExpander() {
        getTabbedTextOffsetRoundTest_NoTabExpander(jta);
        getTabbedTextOffsetRoundTest_NoTabExpander(jtf);
        //getTabbedTextOffsetRoundTest_NoTabExpander(jtp);
    }

    void getTabbedTextOffsetTest(final JTextComponent c) {
        Document doc = c.getDocument();
        Graphics g = c.getGraphics();
        Element root = doc.getDefaultRootElement();
        Segment seg = new Segment();
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < root.getElementCount(); i++) {
            Element currentElem = root.getElement(i);
            int start = currentElem.getStartOffset();
            int end = currentElem.getEndOffset();
            try {
                doc.getText(start, (end - start), seg);
            } catch (BadLocationException e) {
                assertTrue("Unexpected Exception: " + e.getMessage(), false);
            }
            int offset = start + 6;
            int textWidth = Utilities.getTabbedTextWidth(seg, fm, 23, te, offset);
            for (int k = -3; k < textWidth + 4; k++) {
                int target = 23 + k;
                assertEquals(getTabbedTextOffsetRound(seg, fm, 23, target, te, offset, true),
                        Utilities.getTabbedTextOffset(seg, fm, 23, target, te, offset));
                assertEquals(getTabbedTextOffsetRound(seg, fm, 23, target, null, offset, true),
                        Utilities.getTabbedTextOffset(seg, fm, 23, target, null, offset));
            }
        }
    }

    public void testGetTabbedTextOffset() {
        getTabbedTextOffsetTest(jta);
        getTabbedTextOffsetTest(jtf);
        //getTabbedTextOffsetTest(jtp);
    }

    int getBreakLocation(final Segment s, final FontMetrics fm, final int start, final int end,
            final TabExpander t, final int pos) {
        int offset = s.offset;
        int index = Utilities.getTabbedTextOffset(s, fm, start, end, t, pos, false);
        int fullIndex = offset + index;
        BreakIterator bi = BreakIterator.getWordInstance();
        bi.setText(s);
        if (bi.last() <= fullIndex) {
            return bi.last() - offset;
        }
        if (bi.isBoundary(fullIndex)) {
            return Character.isWhitespace(s.array[fullIndex]) ? index + 1 : index;
        }
        int prev = bi.preceding(fullIndex);
        if (prev == bi.first()) {
            return index;
        }
        return prev - offset;
    }

    void getBreakLocationTest(final JTextComponent c) {
        Document doc = c.getDocument();
        Graphics g = c.getGraphics();
        FontMetrics fm = g.getFontMetrics();
        Element root = doc.getDefaultRootElement();
        Segment seg = new Segment();
        for (int i = 0; i < root.getElementCount(); i++) {
            Element currentElem = root.getElement(i);
            int start = currentElem.getStartOffset();
            int end = currentElem.getEndOffset();
            for (int j = start; j < Math.min(start + 10, end); j++) {
                try {
                    doc.getText(start, (end - start), seg);
                } catch (BadLocationException e) {
                    assertTrue("Unexpected Exception: " + e.getMessage(), false);
                }
                int textWidth = Utilities.getTabbedTextWidth(seg, fm, 23, te, j);
                for (int k = 0; k < textWidth; k++) {
                    int target = 23 + k;
                    //TODO This test have to fail
                    assertEquals(Utilities.getBreakLocation(seg, fm, 23, target, te, j),
                            getBreakLocation(seg, fm, 23, target, te, j));
                    assertEquals(Utilities.getBreakLocation(seg, fm, 23, target, null, j),
                            getBreakLocation(seg, fm, 23, target, null, j));
                }
            }
        }
    }

    public void testGetBreakLocation() {
        if (isHarmony()) {
            getBreakLocationTest(jta);
        }
    }

    private void getParagraphElementTest(final JTextComponent c) {
        AbstractDocument ad = (AbstractDocument) c.getDocument();
        if (ad instanceof PlainDocument) {
            assertNull(Utilities.getParagraphElement(c, 5000));
            assertNull(Utilities.getParagraphElement(c, -5000));
        } else {
            assertEquals(ad.getParagraphElement(5000), Utilities.getParagraphElement(c, 5000));
            assertEquals(ad.getParagraphElement(-5000), Utilities.getParagraphElement(c, -5000));
        }
        Element rootElement = ad.getDefaultRootElement();
        for (int i = 0; i < rootElement.getElementCount(); i++) {
            Element elem = rootElement.getElement(i);
            int start = elem.getStartOffset();
            int end = elem.getEndOffset();
            for (int j = start; j < end; j++) {
                assertEquals(elem, Utilities.getParagraphElement(c, j));
                assertEquals(elem, ad.getParagraphElement(j));
            }
        }
    }

    public void testGetParagraphElement() {
        getParagraphElementTest(jta);
        //getParagraphElementTest(jtp);
        getParagraphElementTest(jtf);
    }

    int getPositionAbove(final JTextComponent c, final int p, final int x)
            throws BadLocationException {
        int p0 = Utilities.getRowStart(c, p);
        if (p0 == 0) {
            return -1;
        }
        int end = p0 - 1;
        int diff = Integer.MAX_VALUE;
        int offset = 0;
        int start = Utilities.getRowStart(c, end);
        for (int i = start; i <= end; i++) {
            Rectangle rect = c.modelToView(i);
            assertNotNull(rect);
            int locDiff = Math.abs(rect.x - x);
            if (locDiff <= diff) {
                diff = locDiff;
                offset = i;
            }
        }
        return offset;
    }

    int getPositionBelow(final JTextComponent c, final int p, final int x)
            throws BadLocationException {
        int p0 = Utilities.getRowEnd(c, p);
        int length = c.getDocument().getLength();
        if (p0 == length) {
            return p;
        }
        int start = p0 + 1;
        int diff = Integer.MAX_VALUE;
        int offset = 0;
        int end = Utilities.getRowEnd(c, start);
        for (int i = start; i <= end; i++) {
            Rectangle rect = c.modelToView(i);
            assertNotNull(rect);
            int locDiff = Math.abs(rect.x - x);
            if (locDiff < diff) {
                diff = locDiff;
                offset = i;
            }
        }
        return offset;
    }

    void getPositionAboveBelowTest(final JTextComponent c) {
        BasicTextUI ui = (BasicTextUI) c.getUI();
        Document doc = c.getDocument();
        int length = doc.getLength();
        for (int i = 0; i < length; i++) {
            int utilAbove = 0;
            int utilAbove1 = 0;
            int utilBelow = 0;
            int utilBelow1 = 0;
            int utilAboveT = 0;
            int utilAbove1T = 0;
            int utilBelowT = 0;
            int utilBelow1T = 0;
            int appendix = 23;
            Rectangle rect = null;
            try {
                rect = ui.modelToView(c, i);
            } catch (BadLocationException e) {
            }
            assertNotNull(rect);
            try {
                utilBelow = Utilities.getPositionBelow(c, i, rect.x);
                utilBelowT = getPositionBelow(c, i, rect.x);
                utilBelow1 = Utilities.getPositionBelow(c, i, rect.x + appendix);
                utilBelow1T = Utilities.getPositionBelow(c, i, rect.x + appendix);
            } catch (BadLocationException e) {
                assertFalse("Unexpected exception: " + e.getMessage(), true);
            }
            assertEquals(utilAboveT, utilAbove);
            assertEquals(utilAbove1T, utilAbove1);
            assertEquals(utilBelow1, utilBelow1T);
            assertEquals(utilBelow, utilBelowT);
        }
        try {
            Utilities.getPositionAbove(c, -10, 100);
        } catch (BadLocationException e) {
            message = e.getMessage();
            bWasException = true;
        }
        checkWasException("Position not represented by view");
        try {
            Utilities.getPositionAbove(c, 5000, 100);
        } catch (BadLocationException e) {
            message = e.getMessage();
            bWasException = true;
        }
        checkWasException("Position not represented by view");
        try {
            Utilities.getPositionBelow(c, -10, 100);
        } catch (BadLocationException e) {
            message = e.getMessage();
            bWasException = true;
        }
        checkWasException("Position not represented by view");
        try {
            Utilities.getPositionBelow(c, 5000, 100);
        } catch (BadLocationException e) {
            message = e.getMessage();
            bWasException = true;
        }
        checkWasException("Position not represented by view");
    }

    public void testGetPositionAboveBelow() {
        getPositionAboveBelowTest(jta);
        //getPositionAboveBelowTest(jtp);
        getPositionAboveBelowTest(jtf);
    }

    // HARMONY-2745
    public void testGetPositionAbove() throws BadLocationException {
        jta = new JTextArea();
        assertEquals(-1, Utilities.getPositionAbove(jta, 1, 0));
    }

    // HARMONY-2745
    public void testGetPositionBelow() throws BadLocationException {
        jta = new JTextArea();
        assertEquals(-1, Utilities.getPositionBelow(jta, 1, 0));
    }

    void getWordStartTest(final JTextComponent c) {
        AbstractDocument ad = (AbstractDocument) c.getDocument();
        BreakIterator bi = BreakIterator.getWordInstance();
        int length = ad.getLength();
        try {
            bi.setText(ad.getText(0, ad.getLength()));
        } catch (BadLocationException e) {
        }
        int iteratorWordStart = 0;
        bi.first();
        for (int i = 0; i < length; i++) {
            int utilitiesWordStart = 0;
            if (i < length - 1) {
                iteratorWordStart = bi.preceding(i + 1);
            } else {
                bi.last();
                iteratorWordStart = bi.previous();
            }
            try {
                utilitiesWordStart = Utilities.getWordStart(c, i);
            } catch (BadLocationException e) {
            }
            assertEquals(iteratorWordStart, utilitiesWordStart);
        }
        /* According to spec */
        try {
            Utilities.getWordStart(c, length + 10);
        } catch (BadLocationException e) {
            message = e.getMessage();
            bWasException = true;
        }
        checkWasException("No word at " + (length + 10));
        try {
            Utilities.getWordStart(c, -1);
        } catch (BadLocationException e) {
            message = e.getMessage();
            bWasException = true;
        }
        checkWasException("No word at -1");
        /**/
    }

    public void testGetWordStart() {
        getWordStartTest(jta);
        //getWordStartTest(jtp);
        getWordStartTest(jtf);
    }

    void getWordEndTest(final JTextComponent c) {
        AbstractDocument ad = (AbstractDocument) c.getDocument();
        BreakIterator bi = BreakIterator.getWordInstance();
        int length = ad.getLength();
        try {
            bi.setText(ad.getText(0, length));
        } catch (BadLocationException e) {
        }
        bi.first();
        for (int i = 0; i < length; i++) {
            int utilitiesWordEnd = 0;
            int iteratorWordEnd = bi.following(i);
            try {
                utilitiesWordEnd = Utilities.getWordEnd(c, i);
            } catch (BadLocationException e) {
            }
            assertEquals(iteratorWordEnd, utilitiesWordEnd);
        }
        /* According to spec */
        try {
            Utilities.getWordEnd(c, length + 10);
        } catch (BadLocationException e) {
            message = e.getMessage();
            bWasException = true;
        }
        checkWasException("No word at " + (length + 10));
        try {
            Utilities.getWordEnd(c, -1);
        } catch (BadLocationException e) {
            message = e.getMessage();
            bWasException = true;
        }
        checkWasException("No word at -1");
        /**/
    }

    public void testGetWordEnd() {
        getWordEndTest(jta);
        //getWordEndTest(jtp);
        getWordEndTest(jtf);
    }

    // may be in future...
    int getRowStart(final JTextComponent c, final int pos) throws BadLocationException {
        Rectangle r = null;
        r = c.modelToView(pos);
        for (int i = pos; i >= 0; i--) {
            Rectangle tmp = null;
            tmp = c.modelToView(i);
            if (tmp.y < r.y) {
                return i + 1;
            }
        }
        return 0;
    }

    int getRowEnd(final JTextComponent c, final int pos) throws BadLocationException {
        int length = c.getDocument().getLength();
        if (c instanceof JTextField) {
            return length;
        }
        Rectangle r = null;
        r = c.modelToView(pos);
        for (int i = pos; i <= length; i++) {
            Rectangle tmp = null;
            tmp = c.modelToView(i);
            if (tmp.y > r.y) {
                return i - 1;
            }
        }
        return length;
    }

    void getRowStartEndTest(final JTextComponent c) throws Exception {
        Document doc = c.getDocument();
        View root = c.getUI().getRootView(c).getView(0);
        assertNotNull(root);
        int length = doc.getLength();
        for (int i = 0; i < length; i++) {
            /*
             * int index1 = root.getViewIndex(i, Position.Bias.Forward); View
             * view1 = root.getView(index1); int index2 = view1.getViewIndex(i,
             * Position.Bias.Forward); View view2 = view1.getView(index2); View
             * view = (c instanceof JTextField) ? view1 : view2;
             *
             * int start = view.getStartOffset(); int end = view.getEndOffset() -
             * 1;
             */
            int utilitiesRowStart = 0;
            int utilitiesRowEnd = 0;
            try {
                utilitiesRowStart = Utilities.getRowStart(c, i);
                utilitiesRowEnd = Utilities.getRowEnd(c, i);
            } catch (BadLocationException e) {
            }
            assertEquals(getRowEnd(c, i), utilitiesRowEnd);
            assertEquals(getRowStart(c, i), utilitiesRowStart);
        }
        try {
            Utilities.getRowStart(c, 5000);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        checkWasException("Position not represented by view");
        try {
            Utilities.getRowEnd(c, 5000);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        ;
        checkWasException("Position not represented by view");
        try {
            Utilities.getRowStart(c, -10);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        ;
        checkWasException("Position not represented by view");
        try {
            Utilities.getRowEnd(c, -10);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        ;
        checkWasException("Position not represented by view");
        textComponent = c;
        textComponent.setSize(0, 0);
        assertEquals(-1, Utilities.getRowStart(c, -20));
        assertEquals(-1, Utilities.getRowEnd(c, 5000));
        assertEquals(-1, Utilities.getRowStart(c, 5));
        assertEquals(-1, Utilities.getRowEnd(c, 6));
    }

    public void testGetRowStartEnd() throws Exception {
        getRowStartEndTest(jta);
        //getRowStartEndTest(jtp);
        getRowStartEndTest(jtf);
    }

    void getPreviousWordTest(final JTextComponent c) {
        AbstractDocument ad = (AbstractDocument) c.getDocument();
        BreakIterator bi = BreakIterator.getWordInstance();
        int length = ad.getLength();
        String content = null;
        try {
            content = ad.getText(0, ad.getLength());
            bi.setText(content);
        } catch (BadLocationException e) {
        }
        assertNotNull(content);
        bi.first();
        for (int i = 0; i < length; i++) {
            int utilitiesPrevWord = 0;
            int iteratorPrevWord = bi.preceding(i);
            while (iteratorPrevWord > 0
                    && ((content.charAt(iteratorPrevWord) == ' ' || content
                            .charAt(iteratorPrevWord) == '\n') || content
                            .charAt(iteratorPrevWord) == '\t')) {
                iteratorPrevWord = bi.preceding(iteratorPrevWord);
            }
            try {
                utilitiesPrevWord = Utilities.getPreviousWord(c, i);
            } catch (BadLocationException e) {
            }
            if (iteratorPrevWord == -1) {
                iteratorPrevWord = 0;
            }
            assertEquals(iteratorPrevWord, utilitiesPrevWord);
        }
        try {
            Utilities.getPreviousWord(c, -1);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        checkWasException();
        try {
            Utilities.getPreviousWord(c, 5000);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        checkWasException();
    }

    public void testGetPreviousWord() {
        getPreviousWordTest(jta);
        //getPreviousWordTest(jtp);
        getPreviousWordTest(jtf);
    }

    void getNextWordTest(final JTextComponent c) {
        AbstractDocument ad = (AbstractDocument) c.getDocument();
        BreakIterator bi = BreakIterator.getWordInstance();
        int length = ad.getLength();
        String content = null;
        try {
            content = ad.getText(0, ad.getLength());
            bi.setText(content);
        } catch (BadLocationException e) {
        }
        assertNotNull(content);
        bi.first();
        for (int i = 0; i < length; i++) {
            int utilitiesNextWord = 0;
            int iteratorNextWord = bi.following(i);
            while (iteratorNextWord < length
                    && ((content.charAt(iteratorNextWord) == ' ' || content
                            .charAt(iteratorNextWord) == '\n') || content
                            .charAt(iteratorNextWord) == '\t')) {
                iteratorNextWord = bi.following(iteratorNextWord);
            }
            try {
                utilitiesNextWord = Utilities.getNextWord(c, i);
            } catch (BadLocationException e) {
            }
            if (iteratorNextWord == length) {
                iteratorNextWord = 0;
            }
            assertEquals(iteratorNextWord, utilitiesNextWord);
        }
        try {
            Utilities.getNextWord(c, length + 10);
        } catch (BadLocationException e) {
            message = e.getMessage();
            bWasException = true;
        }
        checkWasException();
        bWasException = false;
        message = null;
        try {
            Utilities.getNextWord(c, -1);
        } catch (BadLocationException e) {
            message = e.getMessage();
            bWasException = true;
        }
        checkWasException();
    }

    public void testGetNextWord() {
        getNextWordTest(jta);
        getNextWordTest(jtf);
        //getNextWordTest(jtp);
    }

    // HARMONY-2744
    public void testGetNextWord02() {
        jta = new JTextArea("");
        try {
            Utilities.getNextWord(jta, 0);
            fail("BadLocationException expected");
        } catch (BadLocationException e) {
            // "No more words"
        }
    }

    // HARMONY-2744
    public void testGetNextWord03() {
        jta = new JTextArea("a");
        try {
            Utilities.getNextWord(jta, 0);
            fail("BadLocationException expected");
        } catch (BadLocationException e) {
            // "No more words"
        }

        try {
            Utilities.getNextWord(jta, 1);
            fail("BadLocationException expected");
        } catch (BadLocationException e) {
            // "No more words"
        }
    }

    // HARMONY-2744
    public void testGetNextWord04() throws Exception {
        jta = new JTextArea("a b");
        assertEquals(2, Utilities.getNextWord(jta, 0));
        assertEquals(2, Utilities.getNextWord(jta, 1));
        try {
            Utilities.getNextWord(jta, 2);
            fail("BadLocationException expected");
        } catch (BadLocationException e) {
            // "No more words"
        }
    }
}