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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.BasicSwingTestCase;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AbstractDocument.LeafElement;
import junit.framework.TestCase;
import org.apache.harmony.x.swing.StringConstants;

/**
 * Tests functionality of AbstractDocument class.
 *
 */
public class AbstractDocumentTest extends TestCase {
    /**
     * The content of the document.
     * (Implementation of AbstractDocument.Content)
     */
    private static GapContent content;

    /**
     * Shared document object.
     */
    private static AbstractDocument doc;

    /**
     * Document property keys.
     */
    static Integer[] keys = new Integer[] { new Integer(1), new Integer(2), new Integer(3) };

    /**
     * Document property values.
     */
    static String[] values = new String[] { "one", "two", "three" };

    /**
     * Initializes fixture for tests.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DisAbstractedDocument(content = new GapContent());
    }

    /**
     * Implements abstract methods of AbstractDocument without changing
     * any other.
     */
    protected static class DisAbstractedDocument extends AbstractDocument {
        private static final long serialVersionUID = 1L;

        protected DisAbstractedDocument(final Content content) {
            super(content);
            createRoot();
        }

        public DisAbstractedDocument(final Content content, final AttributeContext context) {
            super(content, context);
            createRoot();
        }

        private BranchElement defRoot;

        protected void createRoot() {
            writeLock();
            defRoot = new BranchElement(null, null);
            defRoot.replace(0, 0, new Element[] { new LeafElement(defRoot, null, 0, 1) });
            writeUnlock();
        }

        @Override
        public Element getParagraphElement(final int offset) {
            return defRoot.getElement(0);
        }

        @Override
        public Element getDefaultRootElement() {
            return defRoot;
        }
    }

    /**
     * Tests constructor
     * AbstractDocument(AbstractDocument.Content,
     *                  AbstractDocument.AttributeContext)
     */
    public void testAbstractDocumentContentAttributeContext() {
        GapContent content = new GapContent();
        StyleContext context = new StyleContext();
        AbstractDocument doc = new DisAbstractedDocument(content, context);
        assertSame(content, doc.getContent());
        assertSame(context, doc.getAttributeContext());
        Object BidiProperty = BasicSwingTestCase.isHarmony() ? StringConstants.BIDI_PROPERTY
                : "i18n";
        assertTrue(doc.getProperty(BidiProperty).equals(Boolean.FALSE));
    }

    /**
     * Tests constructor AbstractDocument(AbstractDocument.Content)
     */
    public void testAbstractDocumentContent() {
        GapContent content = new GapContent();
        AbstractDocument doc = new DisAbstractedDocument(content);
        assertSame(content, doc.getContent());
        assertSame(StyleContext.getDefaultStyleContext(), doc.getAttributeContext());
    }

    public void testBAD_LOCATION() {
        assertEquals("document location failure", AbstractDocument.BAD_LOCATION);
    }

    public void testCreateLeafElement() throws BadLocationException {
        Element leaf = doc.createLeafElement(null, null, 0, 1);
        assertTrue(leaf instanceof LeafElement);
        assertNull(leaf.getParentElement());
        assertEquals(0, leaf.getStartOffset());
        assertEquals(1, leaf.getEndOffset());
        doc.insertString(0, "01234", null);
        Element leaf2 = doc.createLeafElement(leaf, null, 1, 3);
        assertTrue(leaf2 instanceof LeafElement);
        assertSame(leaf, leaf2.getParentElement());
        assertEquals(1, leaf2.getStartOffset());
        assertEquals(3, leaf2.getEndOffset());
        doc.remove(0, 5);
        assertEquals(0, leaf2.getStartOffset());
        assertEquals(0, leaf2.getEndOffset());
    }

    public void testCreateBranchElement() {
        Element branch = doc.createBranchElement(null, null);
        assertTrue(branch instanceof BranchElement);
        assertNull(branch.getParentElement());
        assertNull(branch.getElement(0));
        assertNull(branch.getElement(1));
        assertEquals(0, branch.getElementCount());

        // Since this branch element has no children yet, it has no start and
        // end offsets. Thus calling get{Start,End}Offset on an empty branch
        // element causes the exception being thrown.
        try {
            assertEquals(0, branch.getStartOffset());
            fail("getStartOffset is expected to throw NullPointerException");
        } catch (NullPointerException e) {
        }

        try {
            assertEquals(1, branch.getEndOffset());
            fail("getEndOffset is expected to throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Tests method insertString.
     */
    public void testInsertString01() throws BadLocationException {
        int length = doc.getLength();
        assertEquals(0, length);
        assertEquals("", doc.getText(0, length));
        doc.insertString(0, "01234", null);
        length = doc.getLength();
        assertEquals(5, length);
        assertEquals("01234", doc.getText(0, length));
        try {
            doc.insertString(-1, "invalid", null);
            fail("BadLocationException should be thrown");
        } catch (BadLocationException e) {
        }
    }

    /**
     * Tests method insertString when passing null as text.
     */
    public void testInsertString02() throws BadLocationException {
        int length = doc.getLength();
        assertEquals(0, length);
        assertEquals("", doc.getText(0, length));
        doc.insertString(0, null, null);
        // Try to insert null at inappropriate position
        doc.insertString(-1, null, null);
    }

    public void testRemove() throws BadLocationException {
        doc.insertString(0, "01234", null);
        doc.remove(1, 2);
        assertEquals("034", doc.getText(0, doc.getLength()));
        // Invalid offset into the model but remove length is zero - nothing
        // is done (no exception)
        doc.remove(-1, 0);
        // Invalid offset and non-zero remove length - exception is thrown
        try {
            doc.remove(-1, 1);
            fail("BadLocationException 'invalid offset' should be thrown");
        } catch (BadLocationException e) {
        }
        // Invalid length
        try {
            doc.remove(0, 4);
            fail("BadLocationException 'invalid length' should be thrown");
        } catch (BadLocationException e) {
        }
    }

    /**
     * General functionality tests for replace.
     */
    public void testReplace01() throws BadLocationException {
        doc.insertString(0, "01234", null);
        doc.replace(1, 2, "abcde", null);
        assertEquals("0abcde34", doc.getText(0, doc.getLength()));
        doc.replace(0, 5, null, null);
        assertEquals("e34", doc.getText(0, doc.getLength()));
        doc.replace(0, 2, "", null);
        assertEquals("4", doc.getText(0, doc.getLength()));
        // Invalid offset into the model
        try {
            doc.replace(-1, 0, "some text", null);
            fail("BadLocationException 'invalid offset' should be thrown");
        } catch (BadLocationException e) {
        }
        // Invalid length
        try {
            doc.replace(0, 4, "some text", null);
            fail("BadLocationException 'invalid length' should be thrown");
        } catch (BadLocationException e) {
        }
    }

    /**
     * Tests that insert listener is not called when calling replace with
     * null or empty text to insert.
     */
    public void testReplace02() throws BadLocationException {
        doc.insertString(0, "01234", null);
        final class DocListener implements DocumentListener {
            private boolean insert = false;

            private boolean remove = false;

            public void changedUpdate(final DocumentEvent event) {
                fail("changedUpdate is not expected to be called");
            }

            public void insertUpdate(final DocumentEvent event) {
                insert = true;
            }

            public void removeUpdate(final DocumentEvent event) {
                remove = true;
            }

            public void check(final boolean eInsert, final boolean eRemove) {
                assertEquals(eInsert, insert);
                assertEquals(eRemove, remove);
                insert = remove = false;
            }
        }
        DocListener listener = new DocListener();
        doc.addDocumentListener(listener);
        doc.replace(0, 2, null, null);
        assertEquals("234", doc.getText(0, doc.getLength()));
        listener.check(false, true);
        doc.replace(0, 2, "", null);
        assertEquals("4", doc.getText(0, doc.getLength()));
        listener.check(false, true);
        doc.replace(0, 0, "", null);
        listener.check(false, false);
    }

    /**
     * Tests methods putProperty and getProperty.
     */
    public void testPutGetProperty() {
        for (int i = 0; i < keys.length; i++) {
            doc.putProperty(keys[i], values[i]);
        }
        // Check
        for (int i = 0; i < keys.length; i++) {
            assertEquals(values[i], doc.getProperty(keys[i]));
        }
        // Test property removal
        doc.putProperty(keys[0], null);
        assertNull(doc.getProperty(keys[0]));
    }

    /**
     * Tests method void getText(int, int, Segment).
     */
    public void testGetTextintintSegment() throws BadLocationException {
        Segment txt = new Segment();
        doc.insertString(0, "01234abcde", null);
        doc.insertString(5, "!", null);
        doc.getText(0, 5, txt);
        assertEquals("01234", txt.toString());
        assertEquals(0, txt.offset);
        assertEquals(5, txt.count);
        assertSame(content.getArray(), txt.array);
        doc.getText(6, 5, txt);
        assertEquals("abcde", txt.toString());
        int gapLength = content.getGapEnd() - content.getGapStart();
        assertEquals(6 + gapLength, txt.offset);
        assertEquals(5, txt.count);
        assertSame(content.getArray(), txt.array);
        doc.getText(0, 11, txt);
        assertEquals("01234!abcde", txt.toString());
        assertEquals(0, txt.offset);
        assertEquals(11, txt.count);
        assertNotSame(content.getArray(), txt.array);
        txt.setPartialReturn(true);
        doc.getText(0, 11, txt);
        assertEquals("01234!", txt.toString());
        assertEquals(0, txt.offset);
        assertEquals(6, txt.count);
        assertSame(content.getArray(), txt.array);
        try {
            doc.getText(-1, 5, txt);
            fail("BadLocationException: \"invalid offset\" must be thrown.");
        } catch (BadLocationException e) {
        }
        try {
            doc.getText(12, 1, txt);
            fail("BadLocationException: \"invalid offset\" must be thrown.");
        } catch (BadLocationException e) {
        }
        try {
            doc.getText(0, 13, txt);
            fail("BadLocationException: \"invalid length\" must be thrown.");
        } catch (BadLocationException e) {
        }
    }

    /**
     * Tests method String getText(int, int).
     */
    public void testGetTextintint() throws BadLocationException {
        doc.insertString(0, "01234abcde", null);
        doc.insertString(5, "!", null);
        // before the gap
        assertEquals("01234", doc.getText(0, 5));
        // after the gap
        assertEquals("abcde", doc.getText(6, 5));
        // the gap is the middle
        assertEquals("01234!abcde", doc.getText(0, 11));
        try {
            doc.getText(-1, 5);
            fail("BadLocationException: \"invalid offset\" must be thrown.");
        } catch (BadLocationException e) {
        }
        try {
            doc.getText(12, 1);
            fail("BadLocationException: \"invalid offset\" must be thrown.");
        } catch (BadLocationException e) {
        }
        try {
            doc.getText(0, 13);
            fail("BadLocationException: \"invalid length\" must be thrown.");
        } catch (BadLocationException e) {
        }
    }

    /**
     * Tests createPosition method.
     */
    public void testCreatePosition() throws BadLocationException {
        try {
            doc.createPosition(-2);
            if (BasicSwingTestCase.isHarmony()) {
                fail("BadLocationException should be thrown");
            }
        } catch (BadLocationException e) {
        }
        Position pos0 = doc.createPosition(0);
        Position pos5 = doc.createPosition(5);
        assertEquals(0, pos0.getOffset());
        assertEquals(5, pos5.getOffset());
        doc.insertString(0, "01234", null);
        assertEquals(0, pos0.getOffset());
        assertEquals(10, pos5.getOffset());
    }

    /**
     * Tests both methods at once: getStartPosition and getEndPosition
     * @throws BadLocationException
     */
    public void testGetStartEndPosition() throws BadLocationException {
        Position start = doc.getStartPosition();
        Position end = doc.getEndPosition();
        assertEquals(0, start.getOffset());
        assertEquals(1, end.getOffset());
        doc.insertString(0, "01234", null);
        assertEquals(0, start.getOffset());
        assertEquals(6, end.getOffset());
        doc.insertString(2, "abcde", null);
        assertEquals(0, start.getOffset());
        assertEquals(11, end.getOffset());
        assertSame(start, doc.getStartPosition());
        assertSame(end, doc.getEndPosition());
        doc.remove(0, 6);
        assertEquals("e234", doc.getText(0, doc.getLength()));
        assertEquals(0, start.getOffset());
        assertEquals(5, end.getOffset());
    }

    public void testGetRootElements() {
        Element[] roots = doc.getRootElements();
        assertEquals(2, roots.length);
        assertNotNull(roots[0]);
        assertSame(roots[0], doc.getDefaultRootElement());
        assertNotNull(roots[1]);
        assertSame(roots[1], doc.getBidiRootElement());
    }

    public void testGetBidiRootElement() {
        Element root = doc.getBidiRootElement();
        assertNotNull(root);
        assertTrue(root instanceof BranchElement);
        assertEquals("bidi root", root.getName());
        assertEquals(0, root.getStartOffset());
        assertEquals(1, root.getEndOffset());
        Enumeration<?> elements = ((BranchElement) root).children();
        Element element = null;
        int count = 0;
        while (elements.hasMoreElements()) {
            count++;
            element = (Element) elements.nextElement();
        }
        // if the document is empty there should be only one child
        assertEquals(1, count);
        assertTrue(element instanceof LeafElement);
        assertEquals("bidi level", element.getName());
        assertSame(AbstractDocument.BidiElementName, element.getName());
        assertEquals(0, element.getStartOffset());
        assertEquals(1, element.getEndOffset());
    }

    public void testGetContent() {
        assertSame(content, doc.getContent());
    }

    public void testGetAttributeContext() {
        assertSame(StyleContext.getDefaultStyleContext(), doc.getAttributeContext());
    }

    public void testSetGetDocumentProperties() {
        Hashtable<Object, Object> table = new Hashtable<Object, Object>();
        for (int i = 0; i < keys.length; i++) {
            table.put(keys[i], values[i]);
        }
        assertNotSame(table, doc.getDocumentProperties());
        doc.setDocumentProperties(table);
        assertSame(table, doc.getDocumentProperties());
        for (int i = 0; i < keys.length; i++) {
            assertEquals(values[i], doc.getProperty(keys[i]));
        }
    }

    public void testDump() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.dump(new PrintStream(out));
        assertEquals("<paragraph>\n" + "  <content>\n" + "    [0,1][\n" + "]\n"
                + "<bidi root>\n" + "  <bidi level\n" + "    bidiLevel=0\n" + "  >\n"
                + "    [0,1][\n" + "]\n", filterNewLines(out.toString()));
    }

    public static String filterNewLines(final String str) {
        return str.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
    }

    /**
     * Tests setDocumentFilter and getDocumentFilter methods.
     */
    public void testSetGetDocumentFilter() {
        assertNull(doc.getDocumentFilter());
        DocumentFilter filter = new DocumentFilter();
        doc.setDocumentFilter(filter);
        assertSame(filter, doc.getDocumentFilter());
    }

    public void testGetLength() throws BadLocationException {
        int length = doc.getLength();
        assertEquals(0, length);
        doc.insertString(0, "01234", null);
        length = doc.getLength();
        assertEquals(5, length);
    }

    /**
     * Tests setAsynchronousLoadPriority and getAsynchronousLoadPriority
     * methods.
     */
    public void testSetGetAsynchronousLoadPriority() {
        // Test the default
        assertEquals(-1, doc.getAsynchronousLoadPriority());
        // Change the default
        doc.setAsynchronousLoadPriority(10);
        assertEquals(10, doc.getAsynchronousLoadPriority());
    }

    /**
     * Tests setAsynchronousLoadPriority and getAsynchronousLoadPriority
     * methods: it asserts they use document property.
     */
    public void testSetGetAsynchronousLoadPriority02() {
        final String key = "load priority";
        final Dictionary<?, ?> properties = doc.getDocumentProperties();
        assertEquals(1, properties.size());
        assertNotNull(properties.get("i18n"));
        // Test the default
        assertEquals(-1, doc.getAsynchronousLoadPriority());
        // Change the default
        assertNull(doc.getProperty(key));
        doc.setAsynchronousLoadPriority(10);
        assertEquals(2, properties.size());
        Object value = doc.getProperty(key);
        assertTrue(value instanceof Integer);
        assertEquals(10, ((Integer) value).intValue());
        assertEquals(10, doc.getAsynchronousLoadPriority());
        doc.putProperty(key, new Integer(-255));
        assertEquals(-255, doc.getAsynchronousLoadPriority());
        doc.putProperty(key, "123");
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(-1, doc.getAsynchronousLoadPriority());
        } else {
            try {
                doc.getAsynchronousLoadPriority();
                fail("ClassCastException is expected");
            } catch (ClassCastException e) {
            }
        }
        doc.putProperty(key, null);
        assertEquals(1, properties.size());
        assertEquals(-1, doc.getAsynchronousLoadPriority());
    }
}
