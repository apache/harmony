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
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.AbstractDocument.LeafElement;
import javax.swing.text.DefStyledDoc_Helpers.ElementBufferWithLogging;
import javax.swing.text.DefaultStyledDocument.ElementBuffer;
import javax.swing.text.DefaultStyledDocument.ElementSpec;

public class DefaultStyledDocumentRTest extends BasicSwingTestCase {
    private static final String ELEMENT_NAME = AbstractDocument.ElementNameAttribute;

    private DefaultStyledDocument doc;

    private Element root;

    private Element paragraph;

    private Element child;

    private int insertOffset = 5;

    private MutableAttributeSet attrs;

    private ElementBuffer buf;

    private ElementSpec[] specs;

    /**
     * Insert one space with attributes containing element name
     * <code>"icon"</code>.
     */
    public void testIconElement() throws Exception {
        attrs.addAttribute(ELEMENT_NAME, "icon");
        doc.insertString(insertOffset, " ", attrs);
        assertEquals(3, paragraph.getElementCount());
        child = paragraph.getElement(0);
        assertEquals(AbstractDocument.ContentElementName, child.getName());
        child = paragraph.getElement(1);
        assertEquals("icon", child.getName());
        assertEquals(insertOffset, child.getStartOffset());
        assertEquals(insertOffset + 1, child.getEndOffset());
        child = paragraph.getElement(2);
        assertEquals(AbstractDocument.ContentElementName, child.getName());
    }

    /**
     * Insert two spaces with attributes containing element name
     * <code>"icon"</code>.
     */
    public void testIconElementTwoSpaces() throws Exception {
        attrs.addAttribute(ELEMENT_NAME, "icon");
        doc.insertString(insertOffset, "  ", attrs);
        assertEquals(3, paragraph.getElementCount());
        child = paragraph.getElement(0);
        assertEquals(AbstractDocument.ContentElementName, child.getName());
        child = paragraph.getElement(1);
        assertEquals("icon", child.getName());
        assertEquals(insertOffset, child.getStartOffset());
        assertEquals(insertOffset + 2, child.getEndOffset());
        child = paragraph.getElement(2);
        assertEquals(AbstractDocument.ContentElementName, child.getName());
    }

    /**
     * Insert not spaces with attributes containing element name
     * <code>"icon"</code>.
     */
    public void testIconElementNotSpaces() throws Exception {
        attrs.addAttribute(ELEMENT_NAME, "icon");
        doc.insertString(insertOffset, "ab", attrs);
        assertEquals(3, paragraph.getElementCount());
        child = paragraph.getElement(0);
        assertEquals(AbstractDocument.ContentElementName, child.getName());
        child = paragraph.getElement(1);
        assertEquals("icon", child.getName());
        assertEquals(insertOffset, child.getStartOffset());
        assertEquals(insertOffset + 2, child.getEndOffset());
        assertEquals("ab", doc.getText(insertOffset, 2));
        child = paragraph.getElement(2);
        assertEquals(AbstractDocument.ContentElementName, child.getName());
    }

    /**
     * Insert not spaces with attributes containing element name
     * <code>"component"</code>.
     */
    public void testComponentElement() throws Exception {
        attrs.addAttribute(ELEMENT_NAME, "component");
        doc.insertString(insertOffset, "ab", attrs);
        assertEquals(3, paragraph.getElementCount());
        child = paragraph.getElement(0);
        assertEquals(AbstractDocument.ContentElementName, child.getName());
        child = paragraph.getElement(1);
        assertEquals("component", child.getName());
        assertEquals(insertOffset, child.getStartOffset());
        assertEquals(insertOffset + 2, child.getEndOffset());
        assertEquals("ab", doc.getText(insertOffset, 2));
        child = paragraph.getElement(2);
        assertEquals(AbstractDocument.ContentElementName, child.getName());
    }

    public void testDeepTreeInsertString01() throws Exception {
        initStructure();
        attrs.addAttribute(ELEMENT_NAME, "content");
        doc.insertString(1, "\n", attrs);
        assertEquals(7, specs.length);
        assertSpec(specs[0], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinNextDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[4], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 1);
        assertSpec(specs[5], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[6], ElementSpec.StartTagType, ElementSpec.JoinNextDirection, 0, 0);
    }

    public void testDeepTreeInsertString02() throws Exception {
        initStructure();
        attrs.addAttribute(ELEMENT_NAME, "content");
        doc.insertString(1, "^", attrs);
        assertEquals(5, specs.length);
        assertSpec(specs[0], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinNextDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.StartTagType, ElementSpec.JoinNextDirection, 0, 0);
        assertSpec(specs[4], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 1);
    }

    public void testDeepTreeInsert01() throws Exception {
        initStructure();
        ElementSpec[] specs = {
                new ElementSpec(null, ElementSpec.EndTagType), // 0
                new ElementSpec(null, ElementSpec.EndTagType), // 1
                new ElementSpec(null, ElementSpec.StartTagType), // 2
                new ElementSpec(null, ElementSpec.StartTagType), // 3
                new ElementSpec(null, ElementSpec.ContentType, // 4
                        "\n".toCharArray(), 0, 1),
                new ElementSpec(null, ElementSpec.EndTagType), // 5
                new ElementSpec(null, ElementSpec.StartTagType), // 6
        };
        specs[2].setDirection(ElementSpec.JoinNextDirection);
        specs[6].setDirection(ElementSpec.JoinNextDirection);
        doc.insert(1, specs);
        final Element html = doc.getDefaultRootElement();
        assertEquals(2, html.getElementCount());
        final Element head = html.getElement(0);
        assertEquals(1, head.getElementCount());
        Element p = head.getElement(0);
        assertChildren(p, new int[] { 0, 1 });
        final Element body = html.getElement(1);
        assertEquals(2, body.getElementCount());
        p = body.getElement(0);
        assertEquals("paragraph", p.getName());
        assertChildren(p, new int[] { 1, 2 });
        p = body.getElement(1);
        assertEquals("p1", p.getName());
        assertChildren(p, new int[] { 2, 6, 6, 7 });
    }

    public void testDeepTreeInsert02() throws Exception {
        initStructure();
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.EndTagType), // 0
                new ElementSpec(null, ElementSpec.EndTagType), // 1
                new ElementSpec(null, ElementSpec.StartTagType), // 2
                new ElementSpec(null, ElementSpec.StartTagType), // 3
                new ElementSpec(null, ElementSpec.ContentType, // 4
                        "^".toCharArray(), 0, 1), };
        specs[2].setDirection(ElementSpec.JoinNextDirection);
        specs[3].setDirection(ElementSpec.JoinNextDirection);
        doc.insert(1, specs);
        final Element html = doc.getDefaultRootElement();
        assertEquals(2, html.getElementCount());
        final Element head = html.getElement(0);
        assertEquals(1, head.getElementCount());
        Element p = head.getElement(0);
        assertChildren(p, new int[] { 0, 1 });
        final Element body = html.getElement(1);
        assertEquals(1, body.getElementCount());
        p = body.getElement(0);
        assertEquals("p1", p.getName());
        assertChildren(p, new int[] { 1, 2, 2, 6, 6, 7 });
    }

    public void testDeepTreeInsertSpecs() throws Exception {
        initStructure();
        ElementSpec[] specs = {
                new ElementSpec(null, ElementSpec.ContentType, "\n".toCharArray(), 0, 1),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.EndTagType), };
        doc.insert(0, specs);
        final Element html = doc.getDefaultRootElement();
        assertEquals(2, html.getElementCount());
        final Element head = html.getElement(0);
        assertEquals(1, head.getElementCount());
        Element p = head.getElement(0);
        assertChildren(p, new int[] { 0, 1, 1, 2 });
        assertEquals("\n\n", doc.getText(0, 2));
        final Element body = html.getElement(1);
        assertEquals(1, body.getElementCount());
        p = body.getElement(0);
        assertEquals("p1", p.getName());
        assertChildren(p, new int[] { 2, 6, 6, 7 });
    }

    public void testHTMLInsert() throws Exception {
        createEmptyHTMLStructure();
        doc.insertString(0, "0000", DefStyledDoc_Helpers.bold);
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.ContentType, "^^^^".toCharArray(), 0, 4), };
        doc.insert(0, specs);
        final Element html = doc.getDefaultRootElement();
        assertEquals(1, html.getElementCount());
        final Element body = html.getElement(0);
        assertEquals(2, body.getElementCount());
        Element child = body.getElement(0);
        assertEquals("content", child.getName());
        assertTrue(child.isLeaf());
        child = body.getElement(1);
        assertEquals("p", child.getName());
        assertChildren(child, new int[] { 4, 8, 8, 9 });
    }

    public void testCreate01() throws Exception {
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, "^".toCharArray(), 0, 1),
                new ElementSpec(null, ElementSpec.EndTagType), };
        doc.create(specs);
        root = doc.getDefaultRootElement();
        assertEquals(2, root.getElementCount());
        Element child = root.getElement(0);
        assertTrue(child.isLeaf());
        assertEquals(0, child.getStartOffset());
        assertEquals(1, child.getEndOffset());
        child = root.getElement(1);
        assertFalse(child.isLeaf());
        assertEquals(1, child.getStartOffset());
        assertEquals(2, child.getEndOffset());
        assertEquals(1, child.getElementCount());
        assertTrue(child.getElement(0).isLeaf());
    }

    public void testCreate02() throws Exception {
        ElementSpec[] specs = { new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, "^".toCharArray(), 0, 1),
                new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.EndTagType), };
        doc.create(specs);
        root = doc.getDefaultRootElement();
        assertEquals(2, root.getElementCount());
        Element child = root.getElement(0);
        assertFalse(child.isLeaf());
        assertEquals(0, child.getStartOffset());
        assertEquals(1, child.getEndOffset());
        assertEquals(1, child.getElementCount());
        child = child.getElement(0);
        assertFalse(child.isLeaf());
        assertEquals(1, child.getElementCount());
        assertTrue(child.getElement(0).isLeaf());
        child = root.getElement(1);
        assertFalse(child.isLeaf());
        assertEquals(1, child.getStartOffset());
        assertEquals(2, child.getEndOffset());
        assertEquals(1, child.getElementCount());
        assertTrue(child.getElement(0).isLeaf());
    }

    /**
     * Tests insertion of a character at the start of a paragraph where
     * the previous paragraph and the character inserted have equal attribute
     * sets whereas the next paragraph to which the character inserted should
     * belong has other attribute set.
     * @throws BadLocationException
     */
    public void testInsertString01() throws BadLocationException {
        doc.remove(0, doc.getLength());
        doc.buffer = new ElementBufferWithLogging(doc, root) {
            private static final long serialVersionUID = 1L;

            @Override
            public void insert(int offset, int length, ElementSpec[] spec,
                    DefaultDocumentEvent event) {
                specs = spec;
                super.insert(offset, length, spec, event);
            }
        };
        StyleConstants.setBold(attrs, true);
        doc.insertString(0, "b", attrs);
        doc.insertString(0, "\n", null);
        doc.insertString(1, "1", null);
        ElementSpec[] ess = { new ElementSpec(null, ElementSpec.EndTagType),
                new ElementSpec(null, ElementSpec.StartTagType),
                new ElementSpec(null, ElementSpec.ContentType, 1) };
        ess[1].setDirection(ElementSpec.JoinNextDirection);
        assertEquals(ess.length, specs.length);
        for (int i = 0; i < ess.length; i++) {
            assertEquals("@ " + i, ess[i].getType(), specs[i].getType());
            assertEquals("@ " + i, ess[i].getDirection(), specs[i].getDirection());
        }
        int[][] offsets = { { 0, 1 }, { 1, 2, 2, 3, 3, 4 } };
        for (int i = 0; i < root.getElementCount(); i++) {
            Element paragraph = root.getElement(i);
            for (int j = 0, oi = 0; j < paragraph.getElementCount(); j++) {
                Element content = paragraph.getElement(j);
                assertEquals("root[" + i + "].start", offsets[i][oi++], content
                        .getStartOffset());
                assertEquals("root[" + i + "].end", offsets[i][oi++], content.getEndOffset());
                if (i == 1 && j == 1) {
                    assertTrue(StyleConstants.isBold(content.getAttributes()));
                } else {
                    assertEquals(0, content.getAttributes().getAttributeCount());
                }
            }
        }
    }

    /**
     * Tests insertion of a character at the start of a styled run where
     * the previous run and the character inserted have equal attribute
     * sets whereas the next run has other attribute set.
     * @throws BadLocationException
     */
    public void testInsertString02() throws BadLocationException {
        doc.remove(0, doc.getLength());
        doc.buffer = new ElementBufferWithLogging(doc, root) {
            private static final long serialVersionUID = 1L;

            @Override
            public void insert(int offset, int length, ElementSpec[] spec,
                    DefaultDocumentEvent event) {
                specs = spec;
                super.insert(offset, length, spec, event);
            }
        };
        StyleConstants.setBold(attrs, true);
        doc.insertString(0, "b", attrs);
        doc.insertString(0, "\n", null);
        doc.insertString(1, "1", null);
        doc.insertString(2, "1", null);
        assertEquals(1, specs.length);
        assertEquals(ElementSpec.ContentType, specs[0].getType());
        assertEquals(ElementSpec.JoinPreviousDirection, specs[0].getDirection());
        int[][] offsets = { { 0, 1 }, { 1, 3, 3, 4, 4, 5 } };
        for (int i = 0; i < root.getElementCount(); i++) {
            Element paragraph = root.getElement(i);
            for (int j = 0, oi = 0; j < paragraph.getElementCount(); j++) {
                Element content = paragraph.getElement(j);
                assertEquals("root[" + i + "].start", offsets[i][oi++], content
                        .getStartOffset());
                assertEquals("root[" + i + "].end", offsets[i][oi++], content.getEndOffset());
                if (i == 1 && j == 1) {
                    assertTrue(StyleConstants.isBold(content.getAttributes()));
                } else {
                    assertEquals(0, content.getAttributes().getAttributeCount());
                }
            }
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefStyledDoc_Helpers.DefStyledDocWithLogging();
        doc.insertString(0, "test  text", null);
        root = doc.getDefaultRootElement();
        paragraph = root.getElement(0);
        attrs = new SimpleAttributeSet();
    }

    private void initStructure() throws BadLocationException {
        doc = new DefStyledDoc_Helpers.DefStyledDocWithLogging();
        root = doc.getDefaultRootElement();
        buf = new DefStyledDoc_Helpers.ElementBufferWithLogging(doc, root) {
            private static final long serialVersionUID = 1L;

            @Override
            public void insert(int offset, int length, ElementSpec[] spec,
                    DefaultDocumentEvent event) {
                super.insert(offset, length, specs = spec, event);
            }
        };
        doc.buffer = buf;
        doc.writeLock();
        try {
            doc.getContent().insertString(0, "\n0000");
            final BranchElement html = (BranchElement) root;
            html.addAttribute(ELEMENT_NAME, "html");
            final BranchElement head = createBranch(html);
            head.addAttribute(ELEMENT_NAME, "head");
            final BranchElement implied = createBranch(head);
            implied.addAttribute(ELEMENT_NAME, "p-implied");
            final LeafElement content0 = createLeaf(implied, 0, 1);
            content0.addAttribute(ELEMENT_NAME, "head-content");
            final BranchElement body = createBranch(html);
            body.addAttribute(ELEMENT_NAME, "body");
            final BranchElement p1 = createBranch(body);
            p1.addAttribute(ELEMENT_NAME, "p1");
            final LeafElement content1 = createLeaf(p1, 1, 5);
            content1.addAttribute(ELEMENT_NAME, "leaf1");
            final LeafElement content2 = createLeaf(p1, 5, 6);
            content2.addAttribute(ELEMENT_NAME, "leaf2");
            implied.replace(0, 0, new Element[] { content0 });
            p1.replace(0, 0, new Element[] { content1, content2 });
            head.replace(0, 0, new Element[] { implied });
            body.replace(0, 0, new Element[] { p1 });
            html.replace(0, 1, new Element[] { head, body });
        } finally {
            doc.writeUnlock();
        }
    }

    private void createEmptyHTMLStructure() {
        doc = new DefStyledDoc_Helpers.DefStyledDocWithLogging();
        root = doc.getDefaultRootElement();
        buf = new DefStyledDoc_Helpers.ElementBufferWithLogging(doc, root) {
            private static final long serialVersionUID = 1L;

            @Override
            public void insert(int offset, int length, ElementSpec[] spec,
                    DefaultDocumentEvent event) {
                super.insert(offset, length, specs = spec, event);
            }
        };
        doc.buffer = buf;
        doc.writeLock();
        try {
            final BranchElement html = (BranchElement) root;
            html.addAttribute(ELEMENT_NAME, "html");
            final BranchElement body = createBranch(html);
            body.addAttribute(ELEMENT_NAME, "body");
            final BranchElement p = createBranch(body);
            p.addAttribute(ELEMENT_NAME, "p");
            final LeafElement content = createLeaf(p, 0, 1);
            content.addAttribute(ELEMENT_NAME, "leaf1");
            p.replace(0, 0, new Element[] { content });
            body.replace(0, 0, new Element[] { p });
            html.replace(0, 1, new Element[] { body });
        } finally {
            doc.writeUnlock();
        }
    }

    private BranchElement createBranch(final Element parent) {
        return (BranchElement) doc.createBranchElement(parent, null);
    }

    private LeafElement createLeaf(final Element parent, final int start, final int end) {
        return (LeafElement) doc.createLeafElement(parent, null, start, end);
    }

    private static void assertChildren(final Element element, final int[] offsets) {
        DefStyledDoc_Helpers.assertChildren(element, offsets);
    }

    private static void assertSpec(final ElementSpec spec, final short type,
            final short direction, final int offset, final int length) {
        DefStyledDoc_Helpers.assertSpec(spec, type, direction, offset, length);
    }
}
