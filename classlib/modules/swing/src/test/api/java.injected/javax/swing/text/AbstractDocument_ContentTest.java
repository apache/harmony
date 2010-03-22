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

import javax.swing.BasicSwingTestCase;
import javax.swing.text.AbstractDocument.Content;

/**
 * This class is to test methods of AbstractDocument.Content inteface.
 *
 */
public class AbstractDocument_ContentTest extends BasicSwingTestCase {
    /**
     * This is a shared object under which will be run. It must be initialized
     * to contain a test string:
     *
     * "This is a test string."
     *  0123456789012345678901
     */
    Content obj;

    /**
     * Shared segment of text which is used in testGetCharsXXX methods.
     */
    Segment text;

    public AbstractDocument_ContentTest() {
        super();
    }

    public AbstractDocument_ContentTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        obj = new GapContent();
        obj.insertString(0, "This is a test string.");
        text = new Segment();
    }

    /**
     * Tests that Position IS NOT changed, after an insertion has occured
     * after this Position.
     */
    public void testCreatePositionInsertAfter() throws BadLocationException {
        Position pos = obj.createPosition(8);
        obj.insertString(10, "big ");
        assertEquals(8, pos.getOffset());
    }

    /**
     * Tests that Position IS NOT changed, after an insertion has occured
     * before this Position.
     */
    public void testCreatePositionInsertBefore() throws BadLocationException {
        Position pos = obj.createPosition(10);
        obj.insertString(9, " big");
        assertEquals(14, pos.getOffset());
    }

    /**
     * Tests that Position IS NOT changed, after an insertion has occured
     * after this Position.
     */
    public void testCreatePositionRemoveAfter() throws BadLocationException {
        Position pos = obj.createPosition(8);
        obj.remove(10, 5);
        assertEquals(8, pos.getOffset());
    }

    /**
     * Tests that Position IS changed, after an insertion has occured
     * before this Position.
     */
    public void testCreatePositionRemoveBefore() throws BadLocationException {
        Position pos = obj.createPosition(15);
        obj.remove(10, 5);
        assertEquals(10, pos.getOffset());
    }

    public void testCreatePositionInvalid() {
        try {
            obj.createPosition(-1);
            if (BasicSwingTestCase.isHarmony()) {
                fail("BadLocationException should be thrown");
            }
        } catch (BadLocationException e) {
        }
    }

    public void testCreatePositionFarAway() throws BadLocationException {
        Position pos = obj.createPosition(150);
        obj.insertString(10, "far away ");
        assertEquals(159, pos.getOffset());
    }

    public void testGetCharsAfterGap() throws BadLocationException {
        // Move the gap
        obj.insertString(10, "big ");
        obj.getChars(19, 8, text);
        assertEquals("string.\n", text.toString());
    }

    public void testGetCharsBeforeGap() throws BadLocationException {
        // The gap is in the end of the buffer
        obj.getChars(0, 4, text);
        assertEquals("This", text.toString());
    }

    public void testGetCharsInvalidPosition() {
        try {
            obj.getChars(30, 4, text);
            fail("BadLocationException should be thrown.");
        } catch (BadLocationException e) {
        }
    }

    public void testGetCharsInvalidLength() {
        try {
            obj.getChars(15, 15, text);
            fail("BadLocationException should be thrown.");
        } catch (BadLocationException e) {
        }
    }

    public void testGetCharsNegativeLength() {
        try {
            obj.getChars(0, -2, new Segment());
            fail("BadLocationException must be thrown: negative length");
        } catch (BadLocationException e) {
        }
    }

    public void testGetCharsPartial() throws BadLocationException {
        // Move the gap
        obj.insertString(10, "big ");
        text.setPartialReturn(true);
        obj.getChars(8, 10, text);
        assertEquals("a big ", text.toString());
    }

    public void testGetCharsWithGap() throws BadLocationException {
        // Move the gap
        obj.insertString(10, "big ");
        obj.getChars(8, 10, text);
        assertEquals("a big test", text.toString());
    }

    public void testGetCharsFullLength() throws BadLocationException {
        obj.getChars(0, obj.length(), text);
        assertEquals("This is a test string.\n", text.toString());
    }

    public void testGetCharsImpliedChar() throws BadLocationException {
        obj.getChars(15, 8, text);
        assertEquals("string.\n", text.toString());
    }

    public void testGetStringAfterGap() throws BadLocationException {
        obj.insertString(10, "big ");
        String str = obj.getString(14, 4);
        assertEquals("test", str);
    }

    public void testGetStringBeforeGap() throws BadLocationException {
        obj.insertString(15, "of ");
        String str = obj.getString(8, 6);
        assertEquals("a test", str);
    }

    public void testGetStringInvalidLength() {
        try {
            obj.getString(15, 15);
            fail("BadLocationException should be thrown.");
        } catch (BadLocationException e) {
        }
    }

    public void testGetStringNegativeLength() {
        try {
            obj.getString(0, -2);
            fail("BadLocationException must be thrown: negative length");
        } catch (BadLocationException e) {
        }
    }

    public void testGetStringInvalidPosition() {
        try {
            obj.getString(30, 5);
            fail("BadLocationException should be thrown.");
        } catch (BadLocationException e) {
        }
    }

    public void testGetStringWithGap() throws BadLocationException {
        obj.insertString(10, "big ");
        String str = obj.getString(8, 10);
        assertEquals("a big test", str);
    }

    public void testGetStringFullLength() throws BadLocationException {
        String str = obj.getString(0, obj.length());
        assertEquals("This is a test string.\n", str);
    }

    public void testGetStringImpliedChar() throws BadLocationException {
        String str = obj.getString(15, 8);
        assertEquals("string.\n", str);
    }

    public void testInsertStringValid() throws BadLocationException {
        obj.insertString(10, "big ");
        assertEquals("a big test", obj.getString(8, 10));
    }

    public void testInsertStringInvalid() {
        try {
            obj.insertString(30, "text");
            fail("BadLocationException should be thrown.");
        } catch (BadLocationException e) {
        }
    }

    public void testLength() {
        // actual string length is 22,
        // but there is implied character at the end of the buffer, so we add 1
        assertEquals(23, obj.length());
    }

    public void testRemoveValid() throws BadLocationException {
        obj.remove(10, 5);
        assertEquals("a string", obj.getString(8, 8));
    }

    /**
     * Removes a portion of content at its end. shiftGapStartDown method
     * should be called.
     */
    public void testRemoveBackEnd() throws BadLocationException {
        obj.remove(14, 8);
        assertEquals("This is a test", obj.getString(0, obj.length() - 1));
    }

    /**
     * Removes a portion of content in the middle. shiftGapStartDown method
     * should be called.
     */
    public void testRemoveBack() throws BadLocationException {
        // Shift the gap so that it's in the middle
        obj.remove(8, 2);
        // Test
        obj.remove(0, 8);
        assertEquals("test string.", obj.getString(0, obj.length() - 1));
    }

    /**
     * Tries to remove implicit character at the end of the content.
     */
    public void testRemoveImplicit() {
        try {
            obj.remove(22, 1);
            fail("BadLocationException should be thrown");
        } catch (BadLocationException e) {
        }
    }

    public void testRemoveInvalidLength() {
        try {
            obj.remove(15, 15);
            fail("BadLocationException should be thrown.");
        } catch (BadLocationException e) {
        }
    }

    public void testRemoveInvalidPosition() {
        try {
            obj.remove(30, 15);
            fail("BadLocationException should be thrown.");
        } catch (BadLocationException e) {
        }
    }
}
