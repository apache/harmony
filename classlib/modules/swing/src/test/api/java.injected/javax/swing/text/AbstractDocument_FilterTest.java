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

import java.io.Serializable;
import javax.swing.text.AbstractDocumentTest.DisAbstractedDocument;
import junit.framework.TestCase;

/**
 * Tests filtering fuctionality of AbstractDocument. (insertString, remove,
 * replace through filter)
 *
 */
public class AbstractDocument_FilterTest extends TestCase {
    private EmptyFilter emptyFilter;

    private AbstractDocument doc;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DisAbstractedDocument(new GapContent());
        doc.setDocumentFilter(new Filter());
        // Additional filter
        emptyFilter = new EmptyFilter();
    }

    /**
     * DocumentFilter class that is used as a test filter.
     */
    static class Filter extends DocumentFilter implements Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public void insertString(final FilterBypass fb, final int offset, String str,
                final AttributeSet attrs) throws BadLocationException {
            if (str == "234") {
                str = "12345";
            } else if (str == "ab") {
                super.insertString(fb, 0, "a", attrs);
                super.insertString(fb, fb.getDocument().getLength(), "b", attrs);
                return;
            }
            super.insertString(fb, offset, str, attrs);
        }

        @Override
        public void remove(final FilterBypass fb, int offset, int len)
                throws BadLocationException {
            // The document shall contain "a12345b"
            if (offset == 1 && len == 5) {
                offset = 2;
                len = 3;
            } else if (offset == 0 && len == 1) {
                super.remove(fb, 0, 1);
                super.remove(fb, fb.getDocument().getLength() - 1, 1);
                return;
            }
            super.remove(fb, offset, len);
        }

        @Override
        public void replace(final FilterBypass fb, int offset, int len, String str,
                final AttributeSet attrs) throws BadLocationException {
            if (offset == 1 && len == 5) {
                fb.remove(offset, len);
                fb.insertString(fb.getDocument().getLength(), str, attrs);
                return;
            } else if (str == "test") {
                offset--;
                len += 2;
                str = "!" + str + "@";
            }
            super.replace(fb, offset, len, str, attrs);
        }
    }

    private static class EmptyFilter extends DocumentFilter {
        private boolean insertCalled = false;

        private boolean removeCalled = false;

        private boolean replaceCalled = false;

        @Override
        public void insertString(final FilterBypass fb, final int offset, final String str,
                final AttributeSet attrs) throws BadLocationException {
            insertCalled = true;
            super.insertString(fb, offset, str, attrs);
        }

        @Override
        public void remove(final FilterBypass fb, final int offset, final int length)
                throws BadLocationException {
            removeCalled = true;
            super.remove(fb, offset, length);
        }

        @Override
        public void replace(final FilterBypass fb, final int offset, final int length,
                final String str, final AttributeSet attrs) throws BadLocationException {
            replaceCalled = true;
            super.replace(fb, offset, length, str, attrs);
        }

        public void check(final boolean eInsert, final boolean eRemove, final boolean eReplace) {
            if (eInsert) {
                assertTrue("insertString IS NOT called", insertCalled);
            } else {
                assertFalse("insertString IS called", insertCalled);
            }
            if (eRemove) {
                assertTrue("remove IS NOT called", removeCalled);
            } else {
                assertFalse("remove IS called", removeCalled);
            }
            if (eReplace) {
                assertTrue("replace IS NOT called", replaceCalled);
            } else {
                assertFalse("replace IS called", replaceCalled);
            }
            // Reset call flags
            reset();
        }

        /**
         * Resets call flags to false.
         */
        public void reset() {
            insertCalled = removeCalled = replaceCalled = false;
        }
    }

    /**
     * Generic filter test for insertString.
     */
    public void testInsertString01() throws BadLocationException {
        doc.insertString(0, "234", null);
        assertEquals("12345", doc.getText(0, doc.getLength()));
        doc.insertString(3, "ab", null);
        assertEquals("a12345b", doc.getText(0, doc.getLength()));
        doc.insertString(5, "not filtered", null);
        assertEquals("a1234not filtered5b", doc.getText(0, doc.getLength()));
    }

    /**
     * Tests if filter is called when inserting null or empty string and when
     * invalid position is passed.
     */
    public void testInsertString02() throws BadLocationException {
        doc.setDocumentFilter(emptyFilter);
        // Null string
        doc.insertString(0, null, null);
        emptyFilter.check(false, false, false);
        // Empty string
        doc.insertString(0, "", null);
        emptyFilter.check(false, false, false);
        // Invalid offset
        try {
            doc.insertString(10, "text", null);
        } catch (BadLocationException e) {
        }
        emptyFilter.check(true, false, false);
    }

    /**
     * Generic filter test for remove.
     */
    public void testRemove01() throws BadLocationException {
        doc.insertString(0, "a12345b", null);
        assertEquals("a12345b", doc.getText(0, doc.getLength()));
        doc.remove(1, 5);
        assertEquals("a15b", doc.getText(0, doc.getLength()));
        doc.remove(0, 1);
        assertEquals("15", doc.getText(0, doc.getLength()));
        doc.remove(0, 2);
        assertEquals("", doc.getText(0, doc.getLength()));
    }

    /**
     * Test if filter is called when length of remove is zero, invalid length is
     * passed, or invalid offset is passed.
     */
    public void testRemove02() throws BadLocationException {
        doc.setDocumentFilter(emptyFilter);
        doc.insertString(0, "text", null);
        emptyFilter.reset();
        // Zero-length
        doc.remove(0, 0);
        emptyFilter.check(false, true, false);
        // Invalid length
        try {
            doc.remove(0, 10);
        } catch (BadLocationException e) {
        }
        emptyFilter.check(false, true, false);
        // Invalid offset
        try {
            doc.remove(10, 3);
        } catch (BadLocationException e) {
        }
        emptyFilter.check(false, true, false);
    }

    /**
     * Generic filter test for replace.
     */
    public void testReplace01() throws BadLocationException {
        doc.insertString(0, "a12345b", null);
        assertEquals("a12345b", doc.getText(0, doc.getLength()));
        doc.replace(1, 5, "cdef", null);
        assertEquals("abcdef", doc.getText(0, doc.getLength()));
        doc.replace(1, 3, "test", null);
        assertEquals("!test@f", doc.getText(0, doc.getLength()));
    }

    /**
     * Tests if filter is called when length of remove is zero, invalid length
     * is passed, invalid offset is passed, or string to insert is null or
     * empty.
     */
    public void testReplace02() throws BadLocationException {
        doc.setDocumentFilter(emptyFilter);
        doc.insertString(0, "text", null);
        emptyFilter.reset();
        // Zero-length remove
        doc.replace(0, 0, "test", null);
        emptyFilter.check(false, false, true);
        // Invalid remove length
        try {
            doc.replace(0, 10, "test", null);
        } catch (BadLocationException e) {
        }
        emptyFilter.check(false, false, true);
        // Invalid offset
        try {
            doc.replace(10, 2, "test", null);
        } catch (BadLocationException e) {
        }
        emptyFilter.check(false, false, true);
        // Null string
        try {
            doc.replace(0, 2, null, null);
        } catch (BadLocationException e) {
        }
        emptyFilter.check(false, false, true);
        // Empty string
        try {
            doc.replace(0, 2, "", null);
        } catch (BadLocationException e) {
        }
        emptyFilter.check(false, false, true);
    }
}