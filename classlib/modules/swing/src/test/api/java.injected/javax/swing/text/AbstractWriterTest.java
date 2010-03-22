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
 * @author Vadim L. Bogdanov
 */
package javax.swing.text;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import javax.swing.SwingTestCase;

public class AbstractWriterTest extends SwingTestCase {
    private static class TestAbstractWriter extends AbstractWriter {
        protected TestAbstractWriter(final Writer w, final Document doc) {
            super(w, doc);
        }

        protected TestAbstractWriter(final Writer w, final Document doc, final int pos,
                final int len) {
            super(w, doc, pos, len);
        }

        protected TestAbstractWriter(final Writer w, final Element root) {
            super(w, root);
        }

        protected TestAbstractWriter(final Writer w, final Element root, final int pos,
                final int len) {
            super(w, root, pos, len);
        }

        @Override
        protected void write() throws IOException, BadLocationException {
        }
    }

    private StringWriter out;

    private Document doc;

    private AbstractWriter writer;

    public AbstractWriterTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        out = new StringWriter();
        doc = createDocument();
        writer = new TestAbstractWriter(out, doc);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAbstractWriterWriterDocument() {
        writer = new TestAbstractWriter(out, doc);
        assertSame(doc, writer.getDocument());
        assertEquals(0, writer.getStartOffset());
        assertEquals(doc.getLength(), writer.getEndOffset());
        assertSame(writer.getElementIterator().current(), doc.getDefaultRootElement());
        assertSame(out, writer.getWriter());
    }

    public void testAbstractWriterWriterDocumentIntInt() {
        writer = new TestAbstractWriter(out, doc, 3, 4);
        assertSame(doc, writer.getDocument());
        assertEquals(3, writer.getStartOffset());
        assertEquals(3 + 4, writer.getEndOffset());
        assertSame(writer.getElementIterator().current(), doc.getDefaultRootElement());
        assertSame(out, writer.getWriter());
        writer = new TestAbstractWriter(out, doc, 300, 400);
        assertSame(doc, writer.getDocument());
        assertEquals(300, writer.getStartOffset());
        assertEquals(300 + 400, writer.getEndOffset());
        assertSame(writer.getElementIterator().current(), doc.getDefaultRootElement());
        assertSame(out, writer.getWriter());
    }

    public void testAbstractWriterWriterElement() {
        Element root = doc.getDefaultRootElement().getElement(1);
        writer = new TestAbstractWriter(out, root);
        assertSame(doc, writer.getDocument());
        assertEquals(0, writer.getStartOffset());
        assertFalse(root.getStartOffset() == writer.getStartOffset());
        assertEquals(root.getEndOffset(), writer.getEndOffset());
        assertSame(writer.getElementIterator().current(), root);
        assertSame(out, writer.getWriter());
    }

    public void testAbstractWriterWriterElementIntInt() {
        Element root = doc.getDefaultRootElement().getElement(1);
        writer = new TestAbstractWriter(out, root, 3, 4);
        assertSame(doc, writer.getDocument());
        assertEquals(3, writer.getStartOffset());
        assertEquals(3 + 4, writer.getEndOffset());
        assertSame(writer.getElementIterator().current(), root);
        assertSame(out, writer.getWriter());
        writer = new TestAbstractWriter(out, root, 300, 400);
        assertSame(doc, writer.getDocument());
        assertEquals(300, writer.getStartOffset());
        assertEquals(300 + 400, writer.getEndOffset());
        assertSame(writer.getElementIterator().current(), root);
        assertSame(out, writer.getWriter());
    }

    public void testGetStartOffset() {
        assertEquals(0, writer.getStartOffset());
    }

    public void testGetEndOffset() {
        assertEquals(doc.getLength(), writer.getEndOffset());
    }

    public void testSetLineSeparator() {
        doc.putProperty(DefaultEditorKit.EndOfLineStringProperty, "~");
        writer = new TestAbstractWriter(out, doc);
        assertEquals("~", writer.getLineSeparator());
        String separator = ">>";
        writer.setLineSeparator(separator);
        assertSame(separator, writer.getLineSeparator());
    }

    public void testGetLineSeparator() {
        final String lineSeparator = System.getProperty("line.separator");
        assertEquals(System.getProperty("line.separator"), writer.getLineSeparator());
        try {
            System.setProperty("line.separator", "<SYS>");
            assertFalse(System.getProperty("line.separator").equals(writer.getLineSeparator()));
            writer = new TestAbstractWriter(out, doc);
            assertEquals("<SYS>", writer.getLineSeparator());
            doc.putProperty(DefaultEditorKit.EndOfLineStringProperty, "<DOC>");
            writer = new TestAbstractWriter(out, doc);
            assertEquals("<DOC>", writer.getLineSeparator());
        } finally {
            System.setProperty("line.separator", lineSeparator);
        }
    }

    public void testGetWriter() {
        assertSame(out, writer.getWriter());
    }

    public void testGetDocument() {
        assertSame(doc, writer.getDocument());
    }

    public void testGetElementIterator() {
        Element root = doc.getDefaultRootElement().getElement(1);
        writer = new TestAbstractWriter(out, root, 3, 4);
        assertSame(writer.getElementIterator().current(), root);
    }

    public void testGetText() throws BadLocationException {
        Element elem = doc.getDefaultRootElement().getElement(1);
        assertEquals(doc.getText(elem.getStartOffset(), elem.getEndOffset()
                - elem.getStartOffset()), writer.getText(elem));
    }

    public void testInRange() {
        Element root = doc.getDefaultRootElement();
        Element elem = root.getElement(1);
        writer = new TestAbstractWriter(out, doc, elem.getStartOffset(), elem.getEndOffset()
                - elem.getStartOffset());
        assertTrue(writer.inRange(root));
        assertTrue(writer.inRange(elem));
        assertFalse(writer.inRange(root.getElement(0)));
    }

    public void testSetGetLineLength() {
        assertEquals(100, writer.getLineLength());
        writer.setLineLength(150);
        assertEquals(150, writer.getLineLength());
    }

    public void testIsLineEmpty() throws IOException {
        if (!isHarmony()) {
            writer.writeLineSeparator();
        }
        assertTrue(writer.isLineEmpty());
        writer.indent();
        assertTrue(writer.isLineEmpty());
        final char content[] = { 'a' };
        writer.output(content, 0, content.length);
        assertFalse(writer.isLineEmpty());
        writer.writeLineSeparator();
        assertTrue(writer.isLineEmpty());
    }

    public void testSetGetCurrentLineLength() throws IOException {
        char content[] = { 'a', 'b' };
        writer.output(content, 0, content.length);
        assertEquals(2, writer.getCurrentLineLength());
        writer.setCurrentLineLength(0);
        assertEquals(0, writer.getCurrentLineLength());
        writer.output(content, 0, content.length);
        assertEquals(2, writer.getCurrentLineLength());
        assertEquals("abab", out.toString());
    }

    public void testSetGetCanWrapLines() {
        assertTrue(writer.getCanWrapLines());
        writer.setCanWrapLines(false);
        assertFalse(writer.getCanWrapLines());
    }

    public void testIncrIndent() {
        assertEquals(0, writer.getIndentLevel());
        writer.incrIndent();
        assertEquals(1, writer.getIndentLevel());
        writer.incrIndent();
        assertEquals(2, writer.getIndentLevel());
        writer.setLineLength(writer.getIndentLevel() * writer.getIndentSpace() + 1);
        writer.incrIndent();
        assertEquals(2, writer.getIndentLevel());
    }

    public void testDecrIndent() {
        assertEquals(0, writer.getIndentLevel());
        writer.decrIndent();
        assertEquals(-1, writer.getIndentLevel());
        writer.incrIndent();
        writer.incrIndent();
        writer.incrIndent();
        writer.decrIndent();
        assertEquals(1, writer.getIndentLevel());
    }

    public void testGetIndentLevel() {
        assertEquals(0, writer.getIndentLevel());
        writer.incrIndent();
        writer.incrIndent();
        writer.incrIndent();
        writer.decrIndent();
        assertEquals(2, writer.getIndentLevel());
    }

    public void testIndent() throws IOException {
        writer.indent();
        assertEquals("", out.toString());
        writer.setIndentSpace(3);
        writer.incrIndent();
        writer.incrIndent();
        writer.indent();
        assertEquals("      ", out.toString());
    }

    public void testSetGetIndentSpace() {
        assertEquals(2, writer.getIndentSpace());
        writer.setIndentSpace(5);
        assertEquals(5, writer.getIndentSpace());
    }

    public void testOutput() throws IOException {
        final char content[] = { 'h', 'e', 'l', 'l', 'o' };
        writer.output(content, 1, 3);
        assertEquals("ell", out.toString());
        assertEquals(3, writer.getCurrentLineLength());
        assertFalse(writer.isLineEmpty());
    }

    public void testText() throws BadLocationException, IOException {
        Element root = doc.getDefaultRootElement();
        writer = new TestAbstractWriter(out, doc, 3, root.getElement(0).getEndOffset());
        writer.setLineSeparator(">>");
        writer.text(root.getElement(0));
        assertEquals("st line>>", out.toString());
        out = new StringWriter();
        writer = new TestAbstractWriter(out, doc, 3, root.getElement(0).getEndOffset());
        writer.setLineSeparator(">>");
        writer.text(root.getElement(1));
        assertEquals("sec", out.toString());
        out = new StringWriter();
        writer = new TestAbstractWriter(out, doc, 3, root.getElement(0).getEndOffset());
        writer.text(root.getElement(2));
        assertEquals("", out.toString());
    }

    public void testWriteChar() throws IOException {
        writer.incrIndent();
        writer.setLineSeparator(">>");
        writer.setCanWrapLines(false);
        writer.write('g');
        assertEquals("g", out.toString());
    }

    public void testWriteCharArrayIntInt() throws IOException {
        final char content[] = " first line\nsecond line\n".toCharArray();
        // no wrap tests
        writer.incrIndent();
        writer.setLineSeparator(">>");
        writer.setCanWrapLines(false);
        writer.write(content, 1, content.length - 2);
        assertEquals("first line>>second line", out.toString());
        out = new StringWriter();
        writer = new TestAbstractWriter(out, doc);
        writer.incrIndent();
        writer.setLineSeparator(">>");
        writer.setCanWrapLines(false);
        writer.setLineLength(5);
        writer.write(content, 0, content.length);
        assertEquals(" first line>>second line>>", out.toString());
        assertTrue(writer.isLineEmpty());
        // tests with wrap
        out = new StringWriter();
        writer = new TestAbstractWriter(out, doc);
        writer.incrIndent();
        writer.setLineSeparator(">>");
        writer.write(content, 1, content.length - 1);
        assertEquals("first line>>  second line>>", out.toString());
        out = new StringWriter();
        writer = new TestAbstractWriter(out, doc);
        writer.incrIndent();
        writer.setLineSeparator(">>");
        writer.setLineLength(2);
        writer.write(content, 0, content.length);
        assertEquals(" >>  first >>  line>>  second >>  line>>", out.toString());
    }

    public void testWriteString() throws IOException {
        final String content = " first line\nsecond line\n";
        writer.incrIndent();
        writer.setLineSeparator(">>");
        writer.setLineLength(2);
        writer.write(content);
        assertEquals(" >>  first >>  line>>  second >>  line>>", out.toString());
    }

    public void testWriteAttributes() throws IOException {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute("key1", "value1");
        attrs.addAttribute("key2", "value2");
        attrs.addAttribute("key3", "value3");
        writer.writeAttributes(attrs);
        assertEquals(" key3=value3 key2=value2 key1=value1", out.toString());
    }

    public void testWriteLineSeparator() throws IOException {
        final char chars[] = { 'a' };
        writer.output(chars, 0, 1);
        assertFalse(writer.isLineEmpty());
        writer.setLineSeparator("b");
        writer.writeLineSeparator();
        assertTrue(writer.isLineEmpty());
        assertEquals("ab", out.toString());
    }

    private Document createDocument() {
        DefaultStyledDocument d = new DefaultStyledDocument();
        try {
            d.insertString(0, "first line\nsecond line\nthird line", null);
        } catch (final BadLocationException e) {
            throw new RuntimeException("Failed to create the document");
        }
        return d;
    }
}
