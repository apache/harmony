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

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingTestCase;

public class DefaultHighlighterTest extends SwingTestCase {
    JTextArea jta;

    JFrame jf;

    DefaultHighlighter dh;

    Highlighter.HighlightPainter lpnt;

    boolean bWasException;

    String s;

    Object obj1;

    Object obj2;

    Object obj3;

    Object obj4;

    Object obj5;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jta = new JTextArea("JTextArea for DefaultHighlight Tesing");
        jf = new JFrame();
        dh = new DefaultHighlighter();
        lpnt = DefaultHighlighter.DefaultPainter;
        bWasException = false;
        s = null;
        obj1 = null;
        obj2 = null;
        obj3 = null;
        obj4 = null;
        obj5 = null;
        jta.setHighlighter(dh);
        jf.getContentPane().add(jta);
        jf.setLocation(200, 300);
        jf.setSize(200, 300);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    public void testDefaultHighlighter() {
        DefaultHighlighter dh1 = new DefaultHighlighter();
        assertNotNull(dh1);
    }

    void assertEqualsHighlight(final Highlighter.Highlight h, final int start, final int end,
            final Highlighter.HighlightPainter pnt) {
        assertEquals(start, h.getStartOffset());
        assertEquals(end, h.getEndOffset());
        assertEquals(pnt, h.getPainter());
    }

    public void testAddHighlightBadLocationException() {
        bWasException = false;
        try {
            dh.addHighlight(-8, -1, lpnt);
        } catch (BadLocationException e) {
            bWasException = true;
        }
        if (isHarmony()) {
            assertTrue(bWasException);
        }
        bWasException = false;
        try {
            dh.addHighlight(8, 1, lpnt);
        } catch (BadLocationException e) {
            bWasException = true;
        }
        if (isHarmony()) {
            assertTrue(bWasException);
        }
    }

    public void testAddHighlight() throws Exception {
        try {
            obj1 = dh.addHighlight(1, 5, lpnt);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertNotNull(obj1);
        dh.getHighlights();
        assertEquals(1, dh.getHighlights().length);
        assertEqualsHighlight(dh.getHighlights()[0], 1, 5, lpnt);
        try {
            obj2 = dh.addHighlight(3, 7, lpnt);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        assertNotNull(obj1);
        assertEquals(2, dh.getHighlights().length);
        assertEqualsHighlight(dh.getHighlights()[0], 1, 5, lpnt);
        assertEqualsHighlight(dh.getHighlights()[1], 3, 7, lpnt);
    }

    public void testInstall() throws Exception {
        try {
            obj1 = dh.addHighlight(1, 5, lpnt);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        try {
            obj2 = dh.addHighlight(3, 7, lpnt);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        dh.setDrawsLayeredHighlights(false);
        dh.deinstall(jta);
        dh.install(jta);
        assertEquals(0, dh.getHighlights().length);
        assertFalse(dh.getDrawsLayeredHighlights());
    }

    public void testDeinstall() throws Exception {
        try {
            obj1 = dh.addHighlight(1, 5, lpnt);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        try {
            obj2 = dh.addHighlight(3, 7, lpnt);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        dh.setDrawsLayeredHighlights(false);
        dh.deinstall(new JTextArea());
        assertEquals(2, dh.getHighlights().length);
        assertFalse(dh.getDrawsLayeredHighlights());
    }

    public void testChangeHighlight() throws Exception {
        try {
            obj1 = dh.addHighlight(1, 5, lpnt);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        try {
            obj2 = dh.addHighlight(3, 7, lpnt);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        try {
            dh.changeHighlight(obj2, 2, 6);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        try {
            dh.changeHighlight(obj1, 1, 8);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        assertEquals(2, dh.getHighlights().length);
        assertEqualsHighlight(dh.getHighlights()[0], 1, 8, lpnt);
        assertEqualsHighlight(dh.getHighlights()[1], 2, 6, lpnt);
    }

    public void testChangeHighlightBadLocationException() {
        bWasException = false;
        try {
            obj1 = dh.addHighlight(1, 5, lpnt);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        bWasException = false;
        try {
            dh.changeHighlight(obj1, -1, -8);
        } catch (BadLocationException e) {
            bWasException = true;
        }
        if (isHarmony()) {
            assertTrue(bWasException);
        }
        bWasException = false;
        try {
            dh.changeHighlight(obj1, 8, 1);
        } catch (BadLocationException e) {
            bWasException = true;
        }
        if (isHarmony()) {
            assertTrue(bWasException);
        }
    }

    public void testRemoveHighlight() throws Exception {
        try {
            obj1 = dh.addHighlight(1, 5, lpnt);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        try {
            obj2 = dh.addHighlight(3, 7, lpnt);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        dh.removeHighlight(obj2);
        assertEquals(1, dh.getHighlights().length);
        assertEqualsHighlight(dh.getHighlights()[0], 1, 5, lpnt);
        dh.removeHighlight(obj1);
        assertEquals(0, dh.getHighlights().length);
    }

    public void testSetGetDrawsLayeredHighlights() throws Exception {
        assertTrue(dh.getDrawsLayeredHighlights());
        dh.setDrawsLayeredHighlights(false);
        assertFalse(dh.getDrawsLayeredHighlights());
    }

    public void testRemoveAllHighlights() throws Exception {
        try {
            dh.addHighlight(0, 1, lpnt);
            dh.addHighlight(0, 4, lpnt);
            dh.addHighlight(5, 7, lpnt);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        assertEquals(3, dh.getHighlights().length);
        dh.removeAllHighlights();
        assertEquals(0, dh.getHighlights().length);
    }

    public void testGetHighlights() throws Exception {
        final int N = 50;
        final int M = 25;
        Object[] obj = new Object[N];
        for (int i = 0; i < N; i++) {
            try {
                obj[i] = dh.addHighlight(i, i + 1, lpnt);
            } catch (BadLocationException e) {
                bWasException = true;
                s = e.getMessage();
            }
            assertFalse("Unexpected exception: " + s, bWasException);
        }
        for (int i = 0; i < N; i = i + 2) {
            dh.removeHighlight(obj[i]);
        }
        for (int i = M; i < N; i++) {
            try {
                dh.addHighlight(i + 12000, i + 12100, lpnt);
            } catch (BadLocationException e) {
                bWasException = true;
                s = e.getMessage();
            }
            assertFalse("Unexpected exception: " + s, bWasException);
        }
        Highlighter.Highlight highlights[] = dh.getHighlights();
        for (int i = 0; i < M; i++) {
            assertEqualsHighlight(highlights[i], 2 * i + 1, 2 * (i + 1), lpnt);
        }
        for (int i = M; i < N; i++) {
            assertEqualsHighlight(highlights[i], i + 12000, i + 12100, lpnt);
        }
    }
}