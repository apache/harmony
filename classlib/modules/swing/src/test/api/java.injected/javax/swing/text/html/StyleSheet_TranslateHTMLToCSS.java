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
package javax.swing.text.html;

import java.awt.Rectangle;
import java.io.StringReader;

import javax.swing.BasicSwingTestCase;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;
import javax.swing.text.View;
import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.StyleContext.NamedStyle;
import javax.swing.text.html.HTMLDocument.BlockElement;
import javax.swing.text.html.HTMLDocument.RunElement;

public class StyleSheet_TranslateHTMLToCSS extends BasicSwingTestCase {
    private static class TestDocument extends HTMLDocument {
        public TestDocument() {
            super();
        }

        public TestDocument(final StyleSheet ss) {
            super(ss);
        }

        public void lockWrite() {
            writeLock();
        }

        public void unlockWrite() {
            writeUnlock();
        }
    }

    private TestDocument doc;
    private StyleSheet ss;
    private AttributeSet attr;

    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        doc = new TestDocument();
        doc.insertString(0, "normal test text", null);
        ss = new StyleSheet();

        doc.lockWrite();
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        doc.unlockWrite();
    }

    public void testTranslateHTMLToCSSBody() throws Exception {
        AbstractElement body =
            (AbstractElement)doc.getDefaultRootElement().getElement(0);
        assertEquals("body", body.getName());
        assertTrue(body instanceof BlockElement);

        body.addAttribute(HTML.Attribute.BGCOLOR, "#ffffff");
        body.addAttribute(HTML.Attribute.BACKGROUND, "bg.jpg");
        body.addAttribute(HTML.Attribute.TEXT, "black");
        body.addAttribute(HTML.Attribute.LINK, "blue");
        body.addAttribute(HTML.Attribute.ALINK, "red");
        body.addAttribute(HTML.Attribute.VLINK, "purple");
        attr = ss.translateHTMLToCSS(body);
        assertSame(NamedStyle.class, attr.getClass());
        assertNull(((NamedStyle)attr).getName());
        assertEquals(3, attr.getAttributeCount());
        assertEquals(isHarmony() ? "url(bg.jpg)" : "bg.jpg",
                     attr.getAttribute(CSS.Attribute.BACKGROUND_IMAGE)
                     .toString());
        assertEquals("#ffffff",
                     attr.getAttribute(CSS.Attribute.BACKGROUND_COLOR)
                     .toString());
        assertEquals("black",
                     attr.getAttribute(CSS.Attribute.COLOR).toString());
    }

    public void testTranslateHTMLToCSSP() throws Exception {
        AbstractElement p =
            (AbstractElement)doc.getDefaultRootElement()
                             .getElement(0).getElement(0);
        assertEquals("p", p.getName());
        assertTrue(p instanceof BlockElement);

        p.addAttribute(HTML.Attribute.BGCOLOR, "#ffffff");
        p.addAttribute(HTML.Attribute.BACKGROUND, "bg.jpg");
        p.addAttribute(HTML.Attribute.TEXT, "black");
        p.addAttribute(HTML.Attribute.LINK, "blue");
        p.addAttribute(HTML.Attribute.ALINK, "red");
        p.addAttribute(HTML.Attribute.VLINK, "purple");

        attr = ss.translateHTMLToCSS(p);
        assertSame(NamedStyle.class, attr.getClass());
        assertNull(((NamedStyle)attr).getName());

        assertEquals(4, attr.getAttributeCount());
        assertEquals(isHarmony() ? "url(bg.jpg)" : "bg.jpg",
                     attr.getAttribute(CSS.Attribute.BACKGROUND_IMAGE)
                     .toString());
        assertEquals("#ffffff",
                     attr.getAttribute(CSS.Attribute.BACKGROUND_COLOR)
                     .toString());
        assertEquals("black",
                     attr.getAttribute(CSS.Attribute.COLOR).toString());

        assertEquals("0",
                     attr.getAttribute(CSS.Attribute.MARGIN_TOP).toString());
    }

    public void testTranslateHTMLToCSSPContent() throws Exception {
        AbstractElement content =
            (AbstractElement)doc.getDefaultRootElement()
                             .getElement(0).getElement(0).getElement(0);
        assertEquals("content", content.getName());
        assertTrue(content instanceof RunElement);

        content.addAttribute(HTML.Attribute.BGCOLOR, "#ffffff");
        content.addAttribute(HTML.Attribute.BACKGROUND, "bg.jpg");
        content.addAttribute(HTML.Attribute.TEXT, "black");
        content.addAttribute(HTML.Attribute.LINK, "blue");
        content.addAttribute(HTML.Attribute.ALINK, "red");
        content.addAttribute(HTML.Attribute.VLINK, "purple");

        attr = ss.translateHTMLToCSS(content);
        assertSame(NamedStyle.class, attr.getClass());
        assertNull(((NamedStyle)attr).getName());
        if (isHarmony()) {
            assertEquals("url(bg.jpg)",
                         attr.getAttribute(CSS.Attribute.BACKGROUND_IMAGE)
                         .toString());
            assertEquals("#ffffff",
                         attr.getAttribute(CSS.Attribute.BACKGROUND_COLOR)
                         .toString());
            assertEquals("black",
                         attr.getAttribute(CSS.Attribute.COLOR).toString());
        } else {
            assertEquals(0, attr.getAttributeCount());
        }
    }

    public void testTranslateHTMLToCSSStyledDocument() throws Exception {
        StyledDocument doc = new DefaultStyledDocument();
        doc.insertString(0, "line1\nline2", null);

        MutableAttributeSet mas = new SimpleAttributeSet();
        mas.addAttribute(HTML.Attribute.BGCOLOR, "#ffffff");
        mas.addAttribute(HTML.Attribute.TEXT, "black");
        doc.setParagraphAttributes(0, 1, mas, false);

        AbstractElement branch =
            (AbstractElement)doc.getDefaultRootElement().getElement(0);
        assertEquals("paragraph", branch.getName());
        assertTrue(branch instanceof BranchElement);
        assertSame(BranchElement.class, branch.getClass());


        attr = ss.translateHTMLToCSS(branch);
        assertSame(NamedStyle.class, attr.getClass());
        assertNull(((NamedStyle)attr).getName());

        assertEquals(2, attr.getAttributeCount());
        assertEquals("#ffffff",
                     attr.getAttribute(CSS.Attribute.BACKGROUND_COLOR)
                     .toString());
        assertEquals("black",
                     attr.getAttribute(CSS.Attribute.COLOR).toString());
    }

    public void testTranslateHTMLToCSSA() throws Exception {
        doc.remove(0, doc.getLength());
        HTMLEditorKit kit = new HTMLEditorKit();
        kit.read(new StringReader("<a href=\"http://go\">link</a>"), doc, 0);

        AbstractElement body =
            (AbstractElement)doc.getDefaultRootElement().getElement(1);
        assertEquals("body", body.getName());

        body.addAttribute(HTML.Attribute.BGCOLOR, "#ffffff");
        body.addAttribute(HTML.Attribute.BACKGROUND, "bg.jpg");
        body.addAttribute(HTML.Attribute.TEXT, "black");
        body.addAttribute(HTML.Attribute.LINK, "blue");
        body.addAttribute(HTML.Attribute.ALINK, "red");
        body.addAttribute(HTML.Attribute.VLINK, "purple");

        AbstractElement a = (AbstractElement)doc.getCharacterElement(2);
        assertNotNull(a.getAttribute(HTML.Tag.A));
        attr = ss.translateHTMLToCSS(a);
        assertNull(((NamedStyle)attr).getName());
        assertEquals(0, attr.getAttributeCount());
    }

    public void testTranslateHTMLToCSSAlignLeft() throws Exception {
        AbstractElement branch =
            (AbstractElement)doc.getDefaultRootElement().getElement(0);
        assertEquals("body", branch.getName());

        branch.addAttribute(HTML.Attribute.ALIGN, "left");
        attr = ss.translateHTMLToCSS(branch);
        assertEquals(1, attr.getAttributeCount());
        assertEquals("left",
                     attr.getAttribute(CSS.Attribute.TEXT_ALIGN).toString());
    }

    public void testTranslateHTMLToCSSAlignCenter() throws Exception {
        AbstractElement branch =
            (AbstractElement)doc.getDefaultRootElement().getElement(0);
        assertEquals("body", branch.getName());

        branch.addAttribute(HTML.Attribute.ALIGN, "center");
        attr = ss.translateHTMLToCSS(branch);
        assertEquals(1, attr.getAttributeCount());
        assertEquals("center",
                     attr.getAttribute(CSS.Attribute.TEXT_ALIGN).toString());
    }

    public void testTranslateHTMLToCSSAlignRight() throws Exception {
        AbstractElement branch =
            (AbstractElement)doc.getDefaultRootElement().getElement(0);
        assertEquals("body", branch.getName());

        branch.addAttribute(HTML.Attribute.ALIGN, "right");
        attr = ss.translateHTMLToCSS(branch);
        assertEquals(1, attr.getAttributeCount());
        assertEquals("right",
                     attr.getAttribute(CSS.Attribute.TEXT_ALIGN).toString());
    }

    public void testTranslateHTMLToCSSAlignJustify() throws Exception {
        AbstractElement branch =
            (AbstractElement)doc.getDefaultRootElement().getElement(0);
        assertEquals("body", branch.getName());

        branch.addAttribute(HTML.Attribute.ALIGN, "justify");
        attr = ss.translateHTMLToCSS(branch);
        assertEquals(1, attr.getAttributeCount());
        assertEquals("justify",
                     attr.getAttribute(CSS.Attribute.TEXT_ALIGN).toString());
    }

    public void testTranslateHTMLToCSSAlignTop() throws Exception {
        AbstractElement branch =
            (AbstractElement)doc.getDefaultRootElement().getElement(0);
        assertEquals("body", branch.getName());

        branch.addAttribute(HTML.Attribute.ALIGN, "top");
        attr = ss.translateHTMLToCSS(branch);
        if (isHarmony()) {
            assertEquals(0, attr.getAttributeCount());
            assertNull(attr.getAttribute(CSS.Attribute.TEXT_ALIGN));
        } else {
            assertEquals(1, attr.getAttributeCount());
            assertEquals("top",
                         attr.getAttribute(CSS.Attribute.TEXT_ALIGN).toString());
        }
    }

    public void testTranslateHTMLToCSSAlignBottom() throws Exception {
        AbstractElement branch =
            (AbstractElement)doc.getDefaultRootElement().getElement(0);
        assertEquals("body", branch.getName());

        branch.addAttribute(HTML.Attribute.ALIGN, "bottom");
        attr = ss.translateHTMLToCSS(branch);
        if (isHarmony()) {
            assertEquals(0, attr.getAttributeCount());
            assertNull(attr.getAttribute(CSS.Attribute.TEXT_ALIGN));
        } else {
            assertEquals(1, attr.getAttributeCount());
            assertEquals("bottom",
                         attr.getAttribute(CSS.Attribute.TEXT_ALIGN).toString());
        }
    }

    public void testTranslateHTMLToCSSAlignChar() throws Exception {
        AbstractElement branch =
            (AbstractElement)doc.getDefaultRootElement().getElement(0);
        assertEquals("body", branch.getName());

        branch.addAttribute(HTML.Attribute.ALIGN, "char");
        attr = ss.translateHTMLToCSS(branch);
        if (isHarmony()) {
            assertEquals(0, attr.getAttributeCount());
            assertNull(attr.getAttribute(CSS.Attribute.TEXT_ALIGN));
        } else {
            assertEquals(1, attr.getAttributeCount());
            assertEquals("char",
                         attr.getAttribute(CSS.Attribute.TEXT_ALIGN).toString());
        }
    }

    public void testTranslateHTMLToCSSVAlignTop() throws Exception {
        AbstractElement branch =
            (AbstractElement)doc.getDefaultRootElement().getElement(0);
        assertEquals("body", branch.getName());

        branch.addAttribute(HTML.Attribute.VALIGN, "top");
        attr = ss.translateHTMLToCSS(branch);
        assertEquals(1, attr.getAttributeCount());
        assertEquals("top",
                     attr.getAttribute(CSS.Attribute.VERTICAL_ALIGN).toString());
    }

    public void testTranslateHTMLToCSSVAlignMiddle() throws Exception {
        AbstractElement branch =
            (AbstractElement)doc.getDefaultRootElement().getElement(0);
        assertEquals("body", branch.getName());

        branch.addAttribute(HTML.Attribute.VALIGN, "middle");
        attr = ss.translateHTMLToCSS(branch);
        assertEquals(1, attr.getAttributeCount());
        assertEquals("middle",
                     attr.getAttribute(CSS.Attribute.VERTICAL_ALIGN).toString());
    }

    public void testTranslateHTMLToCSSVAlignBottom() throws Exception {
        AbstractElement branch =
            (AbstractElement)doc.getDefaultRootElement().getElement(0);
        assertEquals("body", branch.getName());

        branch.addAttribute(HTML.Attribute.VALIGN, "bottom");
        attr = ss.translateHTMLToCSS(branch);
        assertEquals(1, attr.getAttributeCount());
        assertEquals("bottom",
                     attr.getAttribute(CSS.Attribute.VERTICAL_ALIGN).toString());
    }

    public void testTranslateHTMLToCSSVAlignBaseline() throws Exception {
        AbstractElement branch =
            (AbstractElement)doc.getDefaultRootElement().getElement(0);
        assertEquals("body", branch.getName());

        branch.addAttribute(HTML.Attribute.VALIGN, "baseline");
        attr = ss.translateHTMLToCSS(branch);
        assertEquals(1, attr.getAttributeCount());
        assertEquals("baseline",
                     attr.getAttribute(CSS.Attribute.VERTICAL_ALIGN).toString());
    }

    public void testTranslateHTMLToCSSWidth() throws Exception {
        AbstractElement branch = (AbstractElement)doc.getParagraphElement(0);
        assertEquals("p", branch.getName());

        branch.addAttribute(HTML.Attribute.WIDTH, "50%");
        attr = ss.translateHTMLToCSS(branch);
        assertEquals(2, attr.getAttributeCount());
        assertEquals("50%",
                     attr.getAttribute(CSS.Attribute.WIDTH).toString());
        assertEquals("0",
                     attr.getAttribute(CSS.Attribute.MARGIN_TOP).toString());
    }

    public void testTranslateHTMLToCSSHeight() throws Exception {
        AbstractElement branch = (AbstractElement)doc.getParagraphElement(0);
        assertEquals("p", branch.getName());

        branch.addAttribute(HTML.Attribute.HEIGHT, "331");
        attr = ss.translateHTMLToCSS(branch);
        assertEquals(2, attr.getAttributeCount());
        assertEquals("331" + (isHarmony() ? "pt" : ""),
                     attr.getAttribute(CSS.Attribute.HEIGHT).toString());
        assertEquals("0",
                     attr.getAttribute(CSS.Attribute.MARGIN_TOP).toString());
    }

    public void testTranslateHTMLToCSSStyle() throws Exception {
        AbstractElement branch = (AbstractElement)doc.getParagraphElement(0);
        assertEquals("p", branch.getName());

        branch.addAttribute(CSS.Attribute.BORDER_BOTTOM_WIDTH, "1pt");
        attr = ss.translateHTMLToCSS(branch);
        assertEquals(2, attr.getAttributeCount());
        assertEquals("1pt", attr.getAttribute(CSS.Attribute.BORDER_BOTTOM_WIDTH)
                            .toString());
        assertEquals("0",
                     attr.getAttribute(CSS.Attribute.MARGIN_TOP).toString());
    }

    public void testTranslateHTMLToCSSViewAttributes() throws Exception {
        final Marker gva = new Marker(true);
        final Marker th2c = new Marker(true);
        ss = new StyleSheet() {
            public AttributeSet getViewAttributes(View v) {
                gva.setOccurred();
                return super.getViewAttributes(v);
            }
            public AttributeSet translateHTMLToCSS(AttributeSet htmlAttrSet) {
                th2c.setOccurred();
                return super.translateHTMLToCSS(htmlAttrSet);
            }
        };
        doc = new TestDocument(ss);
        doc.lockWrite();
        final AbstractElement branch =
            (AbstractElement)doc.getParagraphElement(0);
        assertEquals("p", branch.getName());

        assertFalse(gva.isOccurred());
        assertFalse(th2c.isOccurred());
        final View view = new InlineView(branch);
        assertTrue(gva.isOccurred());
        assertTrue(th2c.isOccurred());

        final AttributeSet va = view.getAttributes();
        assertFalse(gva.isOccurred());
        assertFalse(th2c.isOccurred());

        assertSame(va, view.getAttributes());
        assertEquals(1, va.getAttributeCount());
        assertEquals("0",
                     va.getAttribute(CSS.Attribute.MARGIN_TOP).toString());

        branch.addAttribute(HTML.Attribute.WIDTH, "200");

        assertFalse(gva.isOccurred());
        assertFalse(th2c.isOccurred());

        view.changedUpdate(new DocumentEvent() {
            public int getOffset() {
                return branch.getStartOffset();
            }
            public int getLength() {
                return branch.getEndOffset() - branch.getStartOffset();
            }
            public Document getDocument() {
                return doc;
            }
            public EventType getType() {
                return EventType.CHANGE;
            }
            public ElementChange getChange(final Element elem) {
                return null;
            }
        }, new Rectangle(), null);
        assertTrue(gva.isOccurred());
        assertTrue(th2c.isOccurred());

        final AttributeSet mva = view.getAttributes();
        assertFalse(gva.isOccurred());
        assertFalse(th2c.isOccurred());

        assertNotSame(va, mva);
        assertSame(mva, view.getAttributes());
        assertEquals(2, mva.getAttributeCount());
    }
}
