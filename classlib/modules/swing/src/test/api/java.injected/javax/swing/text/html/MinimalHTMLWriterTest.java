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
package javax.swing.text.html;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingTestCase;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class MinimalHTMLWriterTest extends SwingTestCase {
    private static class TestMinimalHTMLWriter extends MinimalHTMLWriter {
        public TestMinimalHTMLWriter(final Writer w, final StyledDocument doc) {
            super(w, doc);
        }

        public TestMinimalHTMLWriter(final Writer w, final StyledDocument doc,
                                        final int pos, final int len) {
            super(w, doc, pos, len);
        }

        protected void incrIndent() {
            super.incrIndent();
        }

        protected int getIndentLevel() {
            return super.getIndentLevel();
        }

        protected int getLineLength() {
            return super.getLineLength();
        }
    }

    private StyledDocument doc;
    private TestMinimalHTMLWriter writer;
    private Writer out;
    private Element iconElement;
    private Element componentElement;

    public MinimalHTMLWriterTest(final String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();

        createDocument();
        out = new StringWriter();
        createWriter();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testText() throws Exception {
        Element par = doc.getParagraphElement(0);
        Element text = par.getElement(par.getElementCount() - 1);
        writer.text(text);
        assertEquals("green text", out.toString());
    }

    public void testWriteAttributes() throws Exception {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setItalic(attrs, true);
        StyleConstants.setFontFamily(attrs, "serif");

        writer.writeAttributes(attrs);
        if (isHarmony()) {
            assertTrue("  font-style: italic;~  font-family: serif;~".equals(out.toString())
                       ^ "  font-family: serif;~  font-style: italic;~".equals(out.toString()));
        } else {
            assertTrue("  family:serif;~  italic:italic;~".equals(out.toString())
                       ^ "  italic:italic;~  family:serif;~".equals(out.toString()));
        }
    }

    public void testWrite() throws Exception {
        writer = new TestMinimalHTMLWriter(out, doc) {
            protected void writeHeader() throws IOException {
                write("_header_");
            }
            protected void writeBody() throws IOException, BadLocationException {
                write("_body");
            }
        };
        setupWriter();

        writer.write();
        assertEquals("  <html>~_header__body  </html>~", out.toString());
    }

    public void testMinimalHTMLWriterWriterStyledDocumentIntInt() {
        assertEquals(100, writer.getLineLength());
        assertFalse(writer.inFontTag());
    }

    public void testMinimalHTMLWriterWriterStyledDocument() {
        assertEquals(100, writer.getLineLength());
        assertFalse(writer.inFontTag());
    }

    public void testInFontTag() throws Exception {
        assertFalse(writer.inFontTag());

        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, Color.GREEN);
        writer.writeNonHTMLAttributes(attrs);
        assertTrue(writer.inFontTag());
    }

    public void testIsText() {
        assertFalse(writer.isText(doc.getParagraphElement(0)));
        assertTrue(writer.isText(doc.getCharacterElement(0)));
        assertFalse(writer.isText(iconElement));
    }

    public void testStartFontTag() throws Exception {
        assertFalse(writer.inFontTag());
        int indentLevel = writer.getIndentLevel();
        writer.startFontTag("italic");
        assertFalse(writer.inFontTag());
        assertEquals(indentLevel + 1, writer.getIndentLevel());
        writer.startFontTag("bold");
        assertEquals("  <font style=\"italic\">~    <font style=\"bold\">~",
                     out.toString());
    }

    public void testEndFontTag() throws Exception {
        int indentLevel = writer.getIndentLevel();
        writer.startFontTag("italic");
        writer.endFontTag();
        assertEquals(indentLevel, writer.getIndentLevel());
        assertEquals("  <font style=\"italic\">~~  </font>~",
                     out.toString());
    }

    public void testWriteBody() throws Exception {
        writer = new TestMinimalHTMLWriter(out, doc)  {
            protected void writeContent(final Element elem,
                                        final boolean needsIndenting)
                    throws IOException, BadLocationException {

                if (needsIndenting) {
                    indent();
                }
                write("_content_");
            }

            protected void writeLeaf(final Element elem) throws IOException {
                write("_leaf_");
            }
        };
        setupWriter();

        writer.writeBody();
        assertEquals("  <body>~    <p class=default>~"
                     + "      _content__content__content_~    </p>~"
                     + "    <p class=myStylea>~      _content__leaf__leaf_~"
                     + "    </p>~  </body>~",
                     out.toString());

        out = new StringWriter();
        Element e = doc.getParagraphElement(0).getElement(1);
        writer = new TestMinimalHTMLWriter(out, doc, e.getStartOffset(),
                                           e.getEndOffset() - e.getStartOffset());
        setupWriter();
        writer.writeBody();
        assertEquals("  <body>~    <p class=default>~      <i>italic text</i>~"
                     + "    </p>~  </body>~",
                     out.toString());
    }

    public void testWriteContent() throws Exception {
        writer.writeContent(doc.getCharacterElement(0), true);
        writer.writeContent(doc.getCharacterElement(0), true);
        assertEquals("  <b>bold text   bold text ",
                     out.toString());

        out = new StringWriter();
        createWriter();
        writer.writeContent(doc.getCharacterElement(0), false);
        writer.writeContent(
            doc.getParagraphElement(doc.getLength() - 1).getElement(0), false);
        assertEquals("<b>bold text </b>  <span style=\"color: #00ff00\">~"
                     + " more green text",
                     out.toString());
    }

    public void testWriteHTMLTags() throws Exception {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setItalic(attrs, true);
        writer.writeHTMLTags(attrs);
        assertEquals("<i>", out.toString());

        StyleConstants.setBold(attrs, true);
        writer.writeHTMLTags(attrs);
        assertEquals("<i><b>", out.toString());

        attrs = new SimpleAttributeSet();
        StyleConstants.setUnderline(attrs, true);
        StyleConstants.setStrikeThrough(attrs, true);
        writer.writeHTMLTags(attrs);
        assertEquals("<i><b><u>", out.toString());
    }

    public void testWriteHeader() throws Exception {
        writer = new TestMinimalHTMLWriter(out, doc) {
            protected void writeStyles() throws IOException {
                indent();
                write("...styles...");
                writeLineSeparator();
            }
        };
        setupWriter();

        writer.writeHeader();
        String result = out.toString();
        if (!isHarmony()) {
            result = result.replaceFirst("<style>", "<style type=\"text/css\">");
        }
        assertEquals("  <head>~    <style type=\"text/css\">~"
                     + "      <!--~        ...styles...~"
                     + "      -->~    </style>~  </head>~",
                     result);
    }

    public void testWriteImage() throws Exception {
        writer.incrIndent();
        writer.writeLeaf(iconElement);
        assertEquals("    ", out.toString());

        writer.writeLeaf(doc.getCharacterElement(0));
        assertEquals("        ", out.toString());
    }

    public void testWriteComponent() throws Exception {
        writer.incrIndent();
        writer.writeLeaf(componentElement);
        assertEquals("    ", out.toString());

        writer.writeLeaf(doc.getCharacterElement(0));
        assertEquals("        ", out.toString());
    }

    public void testWriteLeaf() throws Exception {
        writer = new TestMinimalHTMLWriter(out, doc) {
            protected void writeImage(final Element elem) throws IOException {
                super.writeImage(elem);
                write("_image_");
            }

            protected void writeComponent(final Element elem) throws IOException {
                super.writeComponent(elem);
                write("_component_");
            }
        };
        setupWriter();

        writer.writeLeaf(doc.getCharacterElement(0));
        assertEquals("  ", out.toString());

        writer.writeLeaf(iconElement);
        assertEquals("    _image_", out.toString());

        writer.writeLeaf(componentElement);
        assertEquals("    _image_  _component_", out.toString());
    }

    public void testWriteNonHTMLAttributes() throws Exception {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setItalic(attrs, true);
        StyleConstants.setForeground(attrs, Color.RED);
        StyleConstants.setLineSpacing(attrs, 1.0f);
        int indentLevel = writer.getIndentLevel();
        writer.writeNonHTMLAttributes(attrs);

        assertEquals("  <span style=\"color: #ff0000\">~",
                     out.toString());
        assertTrue(writer.inFontTag());
        assertEquals(indentLevel + 1, writer.getIndentLevel());
        writer.writeEndParagraph();

        out = new StringWriter();
        createWriter();
        attrs = new SimpleAttributeSet();
        writer.writeNonHTMLAttributes(attrs);
        assertEquals("", out.toString());
        assertFalse(writer.inFontTag());
    }

    public void testWriteStartParagraph() throws Exception {
        Element par = doc.getParagraphElement(0);
        writer.writeStartParagraph(par);
        assertEquals("  <p class=default>~", out.toString());

        out = new StringWriter();
        createWriter();
        par = doc.getParagraphElement(doc.getLength() - 1);
        writer.writeStartParagraph(par);
        assertEquals("  <p class=myStylea>~", out.toString());
    }

    public void testWriteEndParagraph() throws Exception {
        writer.writeEndParagraph();
        assertEquals("~</p>~", out.toString());

        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setItalic(attrs, true);
        StyleConstants.setFontFamily(attrs, "serif");
        writer.incrIndent();

        writer.writeNonHTMLAttributes(attrs);
        writer.writeHTMLTags(attrs);
        writer.writeEndParagraph();
        assertEquals("~</p>~  <span style=\"font-family: serif\">~<i></i>"
                     + "~  </span>~</p>~",
                     out.toString());
    }

    public void testWriteStartTag() throws Exception {
        int indentLevel = writer.getIndentLevel();
        writer.writeStartTag("the_tag");
        assertEquals("  the_tag~", out.toString());
        assertEquals(indentLevel + 1, writer.getIndentLevel());
    }

    public void testWriteEndTag() throws Exception {
        int indentLevel = writer.getIndentLevel();
        writer.writeEndTag("the_tag");
        assertEquals("the_tag~", out.toString());
        assertEquals(indentLevel - 1, writer.getIndentLevel());
    }

    public void testWriteStyles() throws Exception {
        writer = new TestMinimalHTMLWriter(out, doc) {
            protected void writeAttributes(final AttributeSet attr)
                    throws IOException {

                indent();
                write("_attributes_");
                writeLineSeparator();
            }
        };
        setupWriter();

        writer.writeStyles();

        String output = out.toString();
        // order in which styles are written is not important
        output = output.replaceFirst("p.myStylea", "p.myStyle");

        assertEquals("  p.myStyle {~    _attributes_~  }~"
                     + "  p.myStyle {~    _attributes_~  }~",
                     output);
    }

    private void createWriter() {
        writer = new TestMinimalHTMLWriter(out, doc);
        setupWriter();
    }

    private void setupWriter() {
        writer.setLineSeparator("~");
        writer.incrIndent();
    }

    private void createDocument() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        MutableAttributeSet boldStyle = new SimpleAttributeSet();
        StyleConstants.setBold(boldStyle, true);
        MutableAttributeSet italicStyle = new SimpleAttributeSet();
        StyleConstants.setItalic(italicStyle, true);
        MutableAttributeSet colorStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(colorStyle, Color.GREEN);

        Style style = doc.addStyle("myStyle", null);
        StyleConstants.setBackground(style, Color.RED);
        style = doc.addStyle("myStylea", null);
        StyleConstants.setForeground(style, Color.GREEN);

        doc.insertString(0, "bold text ", boldStyle);
        doc.insertString(doc.getLength(), "italic text", italicStyle);
        doc.insertString(doc.getLength(),
                         "green text\n more green text",
                         colorStyle);

        doc.setLogicalStyle(doc.getLength() - 1, style);

        MutableAttributeSet attrs = new SimpleAttributeSet();
        Icon icon =  new Icon() {
            public int getIconHeight() {
                return 0;
            }

            public int getIconWidth() {
                return 0;
            }

            public void paintIcon(final Component c, final Graphics g,
                                  final int x, final int y) {
            }
        };
        StyleConstants.setIcon(attrs, icon);
        attrs.addAttribute(AbstractDocument.ElementNameAttribute,
                           StyleConstants.IconElementName);
        doc.insertString(doc.getLength(), "ppp", attrs);

        iconElement = doc.getCharacterElement(doc.getLength() - 1);

        attrs = new SimpleAttributeSet();
        StyleConstants.setComponent(attrs, new JLabel("lab1"));
        attrs.addAttribute(AbstractDocument.ElementNameAttribute,
                           StyleConstants.ComponentElementName);
        doc.insertString(doc.getLength(), "ccc", attrs);
        componentElement = doc.getCharacterElement(doc.getLength() - 1);
    }
}
