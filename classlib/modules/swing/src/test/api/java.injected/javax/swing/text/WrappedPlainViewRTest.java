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

import java.awt.FontMetrics;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingTestCase;
import javax.swing.text.Position.Bias;

public class WrappedPlainViewRTest extends SwingTestCase {
    private Document doc;

    private Element root;

    private WrappedPlainView view;

    private JTextArea textArea;

    private JFrame frame;

    private Rectangle shape;

    private int width = 145;

    private int height = 500;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JFrame("WrappedPlainView Test");
        doc = new PlainDocument();
        doc.insertString(0, "one, two, three, four, five\n" + "eins, zwei, drei, vier, funf\n"
                + "uno, dos, tres, cuatro, \tcinco", null);
        root = doc.getDefaultRootElement();
        textArea = new JTextArea(doc);
        textArea.setLineWrap(true);
        frame.getContentPane().add(textArea);
        textArea.setSize(width, height);
        View rootView = textArea.getUI().getRootView(textArea);
        view = (WrappedPlainView) rootView.getView(0);
        shape = new Rectangle(width, height);
        view.setSize(shape.width, shape.height);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        frame.dispose();
    }

    /**
     * Tests the value returned where the point lies below the last line.
     */
    public void testViewToModel() throws BadLocationException {
        final Bias[] bias = new Bias[1];
        width = getTextWidth(root.getElementCount() - 1) * 3 / 4;
        view.setSize(width, height);
        assertEquals(view.getEndOffset() - 1, view.viewToModel(width / 2, height - 10,
                new Rectangle(width, height), bias));
    }

    /**
     * Tests the value returned where the point is in a line which has no
     * breakes in it.
     */
    public void testViewToModelNoBreaks() throws BadLocationException {
        final Bias[] bias = new Bias[1];
        textArea.setSize(500, height);
        shape = new Rectangle(500, height);
        view.setSize(shape.width, shape.height);
        assertTrue("The first line must be not wrapped", 500 > getTextWidth(0));
        FontMetrics metrics = getFontMetrics();
        Element line = root.getElement(0);
        String text = doc.getText(line.getStartOffset(), 2);
        assertEquals(2, view.viewToModel(metrics.stringWidth(text), metrics.getHeight() / 2,
                shape, bias));
    }

    /**
     * This tests that no NPE is thrown when a line is removed.
     */
    public void testInsertUpdate() throws BadLocationException {
        textArea.setText("line1\n\n\n\n");
        assertEquals(5, view.getViewCount());
        assertSame(doc, textArea.getDocument());
        doc.insertString(7, "\n", null);
    }

    /**
     * This tests that no NPE is thrown when a line is removed.
     */
    public void testRemoveUpdate() throws BadLocationException {
        textArea.setText("line1\n\n\n\n");
        assertEquals(5, view.getViewCount());
        assertSame(doc, textArea.getDocument());
        doc.remove(6, 1);
    }

    private int getTextWidth(final int lineNo) throws BadLocationException {
        Element line = root.getElement(lineNo);
        String text = doc.getText(line.getStartOffset(), line.getEndOffset() - 1
                - line.getStartOffset());
        FontMetrics metrics = getFontMetrics();
        return metrics.stringWidth(text);
    }

    private FontMetrics getFontMetrics() {
        return textArea.getFontMetrics(textArea.getFont());
    }
}
