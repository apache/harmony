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
import javax.swing.BasicSwingTestCase;
import javax.swing.JTextArea;
import javax.swing.SwingTestCase;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent.ElementChange;

/**
 * Tests WrappedPlainView methods which can be tested with more simple
 * initialization (without actual GUI being created).
 */
public class WrappedPlainView_SimpleTest extends SwingTestCase {
    public class WrappedPlainViewImpl extends WrappedPlainView {
        public WrappedPlainViewImpl(final Element element, final boolean wordWrap) {
            super(element, wordWrap);
        }

        public WrappedPlainViewImpl(final Element element) {
            super(element);
        }

        @Override
        public Container getContainer() {
            return textArea;
        }

        @Override
        public void preferenceChanged(final View child, final boolean width,
                final boolean height) {
            preferenceParams = new PreferenceChanged(child, width, height);
            super.preferenceChanged(child, width, height);
        }

        @Override
        protected boolean updateChildren(final ElementChange change, final DocumentEvent event,
                final ViewFactory factory) {
            factoryUsed = factory;
            return super.updateChildren(change, event, factory);
        }
    }

    private static class PreferenceChanged {
        public View child;

        public boolean width;

        public boolean height;

        public PreferenceChanged(final View child, final boolean width, final boolean height) {
            this.child = child;
            this.width = width;
            this.height = height;
        }

        public void check(final View child, final boolean width, final boolean height) {
            assertEquals("Children are different", this.child, child);
            assertEquals("Width is different", this.width, width);
            assertEquals("Height is different", this.height, height);
        }
    }

    private PreferenceChanged preferenceParams;

    private ViewFactory factoryUsed;

    private Document doc;

    private Element root;

    private WrappedPlainView view;

    private JTextArea textArea;

    private DocumentEvent docEvent;

    private final int width = 40;

    private final int height = 100;

    private final Rectangle shape = new Rectangle(width, height);

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.addDocumentListener(new DocumentListener() {
            public void changedUpdate(final DocumentEvent e) {
                docEvent = e;
            }

            public void insertUpdate(final DocumentEvent e) {
                docEvent = e;
            }

            public void removeUpdate(final DocumentEvent e) {
                docEvent = e;
            }
        });
        doc.insertString(0, "one, two, three, four, five\n" + "eins, zwei, drei, vier, funf\n"
                + "uno, dos, tres, cuatro, cinco", null);
        root = doc.getDefaultRootElement();
        textArea = new JTextArea(doc);
        view = new WrappedPlainViewImpl(root);
    }

    public void testLoadChildren() {
        assertEquals(0, view.getViewCount());
        view.loadChildren(null);
        assertEquals(root.getElementCount(), view.getViewCount());
    }

    /*
     * Class under test for void WrappedPlainView(Element)
     */
    public void testWrappedPlainViewElement() throws BadLocationException {
        view = new WrappedPlainViewImpl(root);
        assertSame(root, view.getElement());
        assertEquals("Major axis expected to be Y", View.Y_AXIS, view.getAxis());
        Element line2 = root.getElement(1);
        view.setSize(width, height);
        final int start = line2.getStartOffset();
        int breakOffset = view.calculateBreakPosition(start, line2.getEndOffset());
        Container container = view.getContainer();
        FontMetrics metrics = container.getFontMetrics(container.getFont());
        // Assert: text from start up to breakOffset fits into width, but
        //         it doesn't fit if we add the very next character
        if (isHarmony()) {
            // 1.5 considers a symbol fits in available width if
            // its half fits. We require it to entirely fit.
            assertTrue(width >= metrics.stringWidth(doc.getText(start, breakOffset - start)));
        }
        assertFalse(width >= metrics.stringWidth(doc.getText(start, breakOffset - start + 1)));
    }

    /*
     * Class under test for void WrappedPlainView(Element, boolean)
     */
    public void testWrappedPlainViewElementboolean() throws BadLocationException {
        view = new WrappedPlainViewImpl(root, true);
        assertSame(root, view.getElement());
        assertEquals("Major axis expected to be Y", View.Y_AXIS, view.getAxis());
        Element line2 = root.getElement(1);
        view.setSize(width, height);
        int breakOffset = view.calculateBreakPosition(line2.getStartOffset(), line2
                .getEndOffset());
        assertEquals(34, breakOffset);
        assertEquals(" zw", doc.getText(breakOffset - 1, 3));
    }

    public void testNextTabStop() {
        Container container = view.getContainer();
        FontMetrics metrics = container.getFontMetrics(container.getFont());
        view.setSize(300, 100);
        float tabPos = view.getTabSize() * metrics.charWidth('m');
        assertEquals(8, view.getTabSize());
        assertEquals(tabPos, view.nextTabStop(0.0f, 0), 0.00001f);
        assertEquals(tabPos, view.nextTabStop(10.0f, 0), 0.00001f);
        assertEquals(tabPos, view.nextTabStop(tabPos - 1, 0), 0.00001f);
        assertEquals(tabPos * 2, view.nextTabStop(tabPos, 0), 0.00001f);
        // Setting tab size to 4 has no effect on already initialized view
        doc.putProperty(PlainDocument.tabSizeAttribute, new Integer(4));
        assertEquals(4, view.getTabSize());
        assertEquals(tabPos, view.nextTabStop(0.0f, 0), 0.00001f);
        assertEquals(tabPos * 2, view.nextTabStop(tabPos, 0), 0.00001f);
    }

    public void testGetTabSize() {
        assertEquals(8, view.getTabSize());
        doc.putProperty(PlainDocument.tabSizeAttribute, new Integer(4));
        assertEquals(4, view.getTabSize());
    }

    public void testGetLineBuffer() {
        Segment buffer = view.getLineBuffer();
        assertNotNull(buffer);
        assertSame(buffer, view.getLineBuffer());
        assertNotSame(buffer, new WrappedPlainView(root).getLineBuffer());
    }

    /**
     * Tests calculateBreakPosition method with word wrapping turned off.
     */
    public void testCalculateBreakPosition01() throws BadLocationException {
        Container container = view.getContainer();
        FontMetrics metrics = container.getFontMetrics(container.getFont());
        Element line = root.getElement(0);
        int start = line.getStartOffset();
        int end = line.getEndOffset();
        int width = metrics.stringWidth(doc.getText(0, 7))
                - metrics.stringWidth(doc.getText(6, 1)) / 2 - 1;
        view.setSize(width, height);
        assertEquals(6, view.calculateBreakPosition(start, end));
        assertFalse(" ".equals(doc.getText(5, 1)));
        if (!isHarmony()) {
            // The next assertion may fail on 1.5 as it considers a symbol
            // fits on the line if only half of it fits.
            return;
        }
        start = 6;
        int index = start + 1;
        while (width > metrics.stringWidth(doc.getText(start, index - start)) && index <= end) {
            ++index;
        }
        if (index < end) {
            --index;
            assertEquals(index, view.calculateBreakPosition(start, end));
            assertFalse(" ".equals(doc.getText(index - 1, 1)));
        } else {
            fail("didn't managed to find another break location");
        }
    }

    /**
     * Tests calculateBreakPosition method with word wrapping turned on.
     */
    public void testCalculateBreakPosition02() throws BadLocationException {
        view = new WrappedPlainViewImpl(root, true);
        Container container = view.getContainer();
        FontMetrics metrics = container.getFontMetrics(container.getFont());
        Element line = root.getElement(0);
        int start = line.getStartOffset();
        int end = line.getEndOffset();
        int width = metrics.stringWidth(doc.getText(0, 7))
                - metrics.stringWidth(doc.getText(6, 1)) / 2 - 1;
        view.setSize(width, height);
        assertEquals(5, view.calculateBreakPosition(start, end));
        assertTrue(" ".equals(doc.getText(4, 1)));
        start = 6;
        int index = start + 1;
        while (width > metrics.stringWidth(doc.getText(start, index - start)) && index <= end) {
            ++index;
        }
        if (index < end) {
            // Now move back to the space
            do {
                --index;
            } while (!" ".equals(doc.getText(index, 1)));
            assertEquals(index + 1, view.calculateBreakPosition(start, end));
            assertTrue(" ".equals(doc.getText(index, 1)));
        } else {
            fail("didn't managed to find another break location");
        }
    }

    /**
     * Tests calculateBreakPosition method with word wrapping turned on and
     * very-very long string.
     */
    public void testCalculateBreakPosition03() throws BadLocationException {
        String veryLongString = "aVeryVeryVeryLongString";
        doc.insertString(root.getElement(1).getStartOffset(), veryLongString, null);
        Container container = view.getContainer();
        FontMetrics metrics = container.getFontMetrics(container.getFont());
        Element line = root.getElement(1);
        int start = line.getStartOffset();
        int end = line.getEndOffset();
        int width = metrics.stringWidth(veryLongString) / 2;
        view.setSize(width, height);
        int breakPos = view.calculateBreakPosition(start, end);
        // Create a new view with word wrapping
        view = new WrappedPlainViewImpl(root, true);
        view.setSize(width, height);
        assertEquals(breakPos, view.calculateBreakPosition(start, end));
        assertFalse(" ".equals(doc.getText(breakPos - 1, 1)));
    }

    public void testInsertUpdate() {
        view.loadChildren(null);
        view.insertUpdate(docEvent, shape, null);
        if (BasicSwingTestCase.isHarmony()) {
            assertNotNull(factoryUsed);
            assertSame(factoryUsed, view.viewFactory);
            assertEquals("javax.swing.text.WrappedPlainView$LineView", factoryUsed.create(root)
                    .getClass().getName());
        }
    }

    public void testRemoveUpdate() {
        view.loadChildren(null);
        view.removeUpdate(docEvent, shape, null);
        if (BasicSwingTestCase.isHarmony()) {
            assertNotNull(factoryUsed);
            assertSame(factoryUsed, view.viewFactory);
        }
    }

    public void testChangedUpdate() {
        view.loadChildren(null);
        view.changedUpdate(docEvent, shape, null);
        if (BasicSwingTestCase.isHarmony()) {
            assertNotNull(factoryUsed);
            assertSame(factoryUsed, view.viewFactory);
        }
    }

    public void testSetSize() {
        assertFalse(view.isLayoutValid(View.X_AXIS));
        assertFalse(view.isLayoutValid(View.Y_AXIS));
        view.setSize(width, height);
        assertTrue(view.isLayoutValid(View.X_AXIS));
        assertTrue(view.isLayoutValid(View.Y_AXIS));
        assertNotNull(preferenceParams);
        preferenceParams.check(null, true, true);
    }
}
