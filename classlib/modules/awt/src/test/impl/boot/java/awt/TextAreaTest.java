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
 * @author Dmitry A. Durnev
 */
package java.awt;

import junit.framework.TestCase;

public class TextAreaTest extends TestCase {
    TextArea area;
    Frame frame;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        area = new TextArea();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if ((frame != null) && frame.isDisplayable()) {
            frame.dispose();
        }
    }

    /*
     * Test method for 'java.awt.TextArea.addNotify()'
     */
    public void testAddNotify() {
        frame = new Frame();
        frame.add(area);
        assertNull(area.getGraphics());
        assertFalse(area.isDisplayable());
        frame.addNotify();
        assertTrue(area.isDisplayable());
        assertNotNull(area.getGraphics());
    }

    /*
     * Test method for 'java.awt.TextArea.getAccessibleContext()'
     */
    public void testGetAccessibleContext() {
        assertTrue(area.getAccessibleContext() instanceof 
                   TextArea.AccessibleAWTTextArea);

    }

    /*
     * Test method for 'java.awt.TextArea.paramString()'
     */
    public void testParamString() {
        String paramStr = area.paramString();
        assertEquals(0, paramStr.indexOf("text"));
        assertTrue(paramStr.indexOf(",rows=0") >= 0);
        assertTrue(paramStr.indexOf(",columns=0") >= 0);
        assertTrue(paramStr.indexOf(",scrollbarVisibility=both") >= 0);
    }

    /*
     * Test method for 'java.awt.TextArea.getMinimumSize()'
     */
    public void testGetMinimumSize() {
        Dimension minSize = new Dimension();
        assertEquals(minSize, area.getMinimumSize());
        minSize.setSize(13, 16);
        area.setMinimumSize(minSize);
        assertNotSame(minSize, area.getMinimumSize());
        assertEquals(minSize, area.getMinimumSize());
        area.setMinimumSize(null);
        assertEquals(new Dimension(), area.getMinimumSize());
        frame = new Frame();
        frame.add(area);
        frame.addNotify();
        assertEquals("By default minimum size is set for 10 rows and 60 columns",
                     area.getMinimumSize(10, 60), area.getMinimumSize());

    }

    /*
     * Test method for 'java.awt.TextArea.minimumSize()'
     */
    @SuppressWarnings("deprecation")
    public void testMinimumSize() {
        Dimension minSize = new Dimension();
        assertEquals(minSize, area.minimumSize());
        minSize.setSize(130, 160);
        area.setMinimumSize(minSize);
        assertNotSame(minSize, area.minimumSize());
        assertEquals(minSize, area.minimumSize());
        area.setMinimumSize(null);
        assertEquals(new Dimension(), area.minimumSize());
        frame = new Frame();
        frame.add(area);
        frame.addNotify();
        assertEquals("By default minimum size is set for 10 rows and 60 columns",
                     area.minimumSize(10, 60), area.minimumSize());
        int rows = 15;
        area.setRows(rows);
        assertEquals("By default minimum size is set for 10 rows and 60 columns",
                     area.minimumSize(10, 60), area.minimumSize());
        int cols = 80;
        area.setColumns(cols);
        assertEquals(area.minimumSize(rows, cols), area.minimumSize());

    }

    /*
     * Test method for 'java.awt.TextArea.getPreferredSize()'
     */
    public void testGetPreferredSize() {
        Dimension prefSize = new Dimension();
        assertEquals(prefSize, area.getPreferredSize());
        prefSize.setSize(4, 5);
        area.setPreferredSize(prefSize);
        assertNotSame(prefSize, area.getPreferredSize());
        assertEquals(prefSize, area.getPreferredSize());
        area.setPreferredSize(null);
        assertEquals(new Dimension(), area.getPreferredSize());
        frame = new Frame();
        frame.add(area);
        frame.addNotify();
        assertEquals("By default preferred size is equal to minimum size",
                     area.getMinimumSize(), area.getPreferredSize());
    }

    /*
     * Test method for 'java.awt.TextArea.preferredSize()'
     */
    @SuppressWarnings("deprecation")
    public void testPreferredSize() {
        Dimension prefSize = new Dimension();
        assertEquals(area.minimumSize(), area.preferredSize());
        prefSize.setSize(40, 50);
        area.setPreferredSize(prefSize);
        assertNotSame(prefSize, area.preferredSize());
        assertEquals(prefSize, area.preferredSize());
        area.setPreferredSize(null);
        assertEquals(new Dimension(), area.preferredSize());
        frame = new Frame();
        frame.add(area);
        frame.addNotify();
        assertEquals("By default preferred size is equal to minimum size",
                     area.minimumSize(), area.preferredSize());
    }

    /*
     * Test method for 'java.awt.TextArea.TextArea()'
     */
    public void testTextArea() {
        assertNotNull(area);
        assertEquals("", area.getText());
        assertEquals(0, area.getColumns());
        assertEquals(0, area.getRows());
        assertEquals(TextArea.SCROLLBARS_BOTH, area.getScrollbarVisibility());

    }

    /*
     * Test method for 'java.awt.TextArea.TextArea(String, int, int, int)'
     */
    public void testTextAreaStringIntIntInt() {
        String text = "text";
        int cols = 12;
        int rows = 5;
        int scrollbars = TextArea.SCROLLBARS_NONE;
        area = new TextArea(text, rows, cols, scrollbars);
        assertEquals(text, area.getText());
        assertEquals(cols, area.getColumns());
        assertEquals(rows, area.getRows());
        assertEquals(scrollbars, area.getScrollbarVisibility());
        scrollbars = -1;
        area = new TextArea(text, rows, cols, scrollbars);
        assertEquals(TextArea.SCROLLBARS_BOTH, area.getScrollbarVisibility());

    }

    /*
     * Test method for 'java.awt.TextArea.TextArea(String, int, int)'
     */
    public void testTextAreaStringIntInt() {
        String text = "text";
        int cols = 12;
        int rows = 5;
        area = new TextArea(text, rows, cols);
        assertEquals(text, area.getText());
        assertEquals(cols, area.getColumns());
        assertEquals(rows, area.getRows());
        assertEquals(TextArea.SCROLLBARS_BOTH, area.getScrollbarVisibility());
        cols = -12;
        area = new TextArea(text, rows, cols);
        assertEquals(0, area.getColumns());
        assertEquals(rows, area.getRows());
        rows = -666;
        area = new TextArea(text, rows, cols);
        assertEquals(0, area.getColumns());
        assertEquals(0, area.getColumns());
    }

    /*
     * Test method for 'java.awt.TextArea.TextArea(String)'
     */
    public void testTextAreaString() {
        String text = "text";
        area = new TextArea(text);
        assertEquals(text, area.getText());
        assertEquals(0, area.getColumns());
        assertEquals(0, area.getRows());
        assertEquals(TextArea.SCROLLBARS_BOTH, area.getScrollbarVisibility());
        area = new TextArea(text = null);
        assertEquals("", area.getText());
    }

    /*
     * Test method for 'java.awt.TextArea.TextArea(int, int)'
     */
    public void testTextAreaIntInt() {
        int cols = 12;
        int rows = 15;
        area = new TextArea(rows, cols);
        assertEquals("", area.getText());
        assertEquals(cols, area.getColumns());
        assertEquals(rows, area.getRows());
    }

    /*
     * Test method for 'java.awt.TextArea.append(String)'
     */
    public void testAppend() {
        String text = "text";
        area.setText(text);
        String str = "\nappended text";

        area.append(str);
        assertEquals(text + str, area.getText());
        assertEquals(0, area.getRows());
        assertEquals(0, area.getCaretPosition());
    }

    /*
     * Test method for 'java.awt.TextArea.insert(String, int)'
     */
    public void testInsert() {
        String text = "text";
        area.setText(text);
        String str = "inserted text\n";
        area.insert(str, 0);
        assertEquals(str + text, area.getText());
        assertEquals(0, area.getRows());
        assertEquals(0, area.getCaretPosition());
    }

    /*
     * Test method for 'java.awt.TextArea.appendText(String)'
     */
    @SuppressWarnings("deprecation")
    public void testAppendText() {
        String text = "text";
        area.setText(text);
        String str = "\nappended text";
        area.appendText(str);
        assertEquals(text + str, area.getText());
        assertEquals(0, area.getColumns());
    }

    /*
     * Test method for 'java.awt.TextArea.getColumns()'
     */
    public void testGetColumns() {
        assertEquals(0, area.getColumns());
    }

    /*
     * Test method for 'java.awt.TextArea.getMinimumSize(int, int)'
     */
    public void testGetMinimumSizeIntInt() {
        int rows = 1;
        int cols = 1;
        Dimension minSize = new Dimension();
        assertEquals(minSize, area.getMinimumSize(rows, cols));
        minSize.setSize(12, 13);
        area.setMinimumSize(minSize);
        assertEquals(minSize, area.getMinimumSize(rows, cols));
        area.setMinimumSize(null);
        assertEquals(new Dimension(), area.getMinimumSize(rows, cols));
        frame = new Frame();
        frame.add(area);
        frame.addNotify();

        assertTrue(area.getMinimumSize(rows, cols).width > 0);
        assertTrue(area.getMinimumSize(rows, cols).height > 0);
        int dw = (area.getMinimumSize(rows, cols * 2).width -
                  area.getMinimumSize(rows, cols).width);
        int dw1 = (area.getMinimumSize(rows, cols * 3).width -
                area.getMinimumSize(rows, cols * 2).width);
        assertEquals(dw, dw1);
        int dh = (area.getMinimumSize(rows * 2, cols).height -
                area.getMinimumSize(rows, cols).height);
      int dh1 = (area.getMinimumSize(rows * 3, cols).height -
              area.getMinimumSize(rows * 2, cols).height);
      assertEquals(dh1, dh);

    }

    /*
     * Test method for 'java.awt.TextArea.getPreferredSize(int, int)'
     */
    public void testGetPreferredSizeIntInt() {
        int rows = 2;
        int cols = 3;
        Dimension prefSize = new Dimension();
        assertEquals(area.getMinimumSize(rows, cols),
                     area.getPreferredSize(rows, cols));
        prefSize.setSize(12, 13);
        area.setPreferredSize(prefSize);
        assertEquals(prefSize, area.getPreferredSize(rows, cols));
        area.setPreferredSize(null);
        assertEquals(new Dimension(), area.getPreferredSize(rows, cols));
        frame = new Frame();
        frame.add(area);
        frame.addNotify();

        assertEquals(area.getMinimumSize(rows, cols),
                     area.getPreferredSize(rows, cols));
    }

    /*
     * Test method for 'java.awt.TextArea.getRows()'
     */
    public void testGetRows() {
        assertEquals(0, area.getRows());
    }

    /*
     * Test method for 'java.awt.TextArea.getScrollbarVisibility()'
     */
    public void testGetScrollbarVisibility() {
        assertEquals(TextArea.SCROLLBARS_BOTH, area.getScrollbarVisibility());
    }

    /*
     * Test method for 'java.awt.TextArea.insertText(String, int)'
     */
    @SuppressWarnings("deprecation")
    public void testInsertText() {
        String text = "text";
        area.setText(text);
        String str = "inserted text\n";
        area.insertText(str, 0);
        assertEquals(str + text, area.getText());
        assertEquals(0, area.getColumns());
        assertEquals(0, area.getCaretPosition());
    }

    /*
     * Test method for 'java.awt.TextArea.minimumSize(int, int)'
     */
    @SuppressWarnings("deprecation")
    public void testMinimumSizeIntInt() {
        frame = new Frame();
        frame.add(area);
        frame.addNotify();
        int rows = 10;
        int cols = 25;
        assertEquals(area.getMinimumSize(rows, cols),
                     area.minimumSize(rows, cols));
    }

    /*
     * Test method for 'java.awt.TextArea.preferredSize(int, int)'
     */
    @SuppressWarnings("deprecation")
    public void testPreferredSizeIntInt() {
        frame = new Frame();
        frame.add(area);
        frame.addNotify();
        int rows = 10;
        int cols = 25;
        assertEquals(area.getPreferredSize(rows, cols),
                     area.preferredSize(rows, cols));
    }

    /*
     * Test method for 'java.awt.TextArea.replaceRange(String, int, int)'
     */
    public void testReplaceRange() {
        int start = 8;
        int end = 11;
        String text = "This is old text";
        area.setText(text);
        String str = "brand new";
        area.replaceRange(str, start, end);
        assertEquals("This is brand new text", area.getText());
        assertEquals("", area.getSelectedText());
        assertEquals(0, area.getRows());
        assertEquals(0, area.getCaretPosition());
    }

    /*
     * Test method for 'java.awt.TextArea.replaceText(String, int, int)'
     */
    @SuppressWarnings("deprecation")
    public void testReplaceText() {
        String text = "This is old text";
        area.setText(text);
        String str = "new\n";
        area.replaceText(str, 8, 12);
        assertEquals("This is new\ntext", area.getText());
        assertEquals(0, area.getColumns());
        assertEquals(0, area.getCaretPosition());
    }

    /*
     * Test method for 'java.awt.TextArea.setColumns(int)'
     */
    public void testSetColumns() {
        int cols = 80;
        area.setColumns(cols);
        assertEquals(cols, area.getColumns());
        try {
            area.setColumns(-1);
        } catch (IllegalArgumentException iae) {
            assertEquals(cols, area.getColumns());
            return;
        }
        fail("no exception was thrown!");

    }

    /*
     * Test method for 'java.awt.TextArea.setRows(int)'
     */
    public void testSetRows() {
        int rows = 25;
        area.setRows(rows);
        assertEquals(rows, area.getRows());
        try {
            area.setRows(-1);
        } catch (IllegalArgumentException iae) {
            assertEquals(rows, area.getRows());
            return;
        }
        fail("no exception was thrown!");
    }

}
