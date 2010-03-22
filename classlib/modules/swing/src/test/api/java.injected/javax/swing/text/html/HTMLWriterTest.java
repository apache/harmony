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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.swing.SwingTestCase;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;

public class HTMLWriterTest extends SwingTestCase {
    private static final String HTML_TEXT = "normal <b>bold</b>";

    private static class TestHTMLDocument extends HTMLDocument {
        public void callWriteLock() {
            writeLock();
        }
        public void callWriteUnlock() {
            writeUnlock();
        }
    }

    private static class TestHTMLWriter extends HTMLWriter {
        public TestHTMLWriter(final Writer w, final HTMLDocument doc) {
            super(w, doc);
        }

        public TestHTMLWriter(final Writer w, final HTMLDocument doc,
                              final int pos, final int len) {
            super(w, doc, pos, len);
        }

        protected void incrIndent() {
            super.incrIndent();
        }

        protected int getIndentLevel() {
            return super.getIndentLevel();
        }
        protected void setLineLength(final int len) {
            super.setLineLength(len);
        }
        protected boolean getCanWrapLines() {
            return super.getCanWrapLines();
        }
        protected int getLineLength() {
            return super.getLineLength();
        }
    }

    private TestHTMLDocument doc;
    private Element root;
    private Element body;
    private StringWriter out;
    private TestHTMLWriter writer;

    public HTMLWriterTest(final String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);

        createDocument(HTML_TEXT);
        out = new StringWriter();
        writer = new TestHTMLWriter(out, doc);
        writer.setLineSeparator("~");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testWrite() throws Exception {
        final String content = "<html>\n"
                             + "  <head>\n"
                             + "    \n"
                             + "  </head>\n"
                             + "  <body>\n"
                             + "    <p>\n"
                             + "      Body text. <i>Italic text</i>\n"
                             + "    </p>\n"
                             + "    Text outside paragraphs.\n"
                             + "  </body>\n"
                             + "</html>\n"
                             + "<!-- mycomment1 -->\n"
                             + "<!-- mycomment2 -->\n";
        createDocument(content);
        writer = new TestHTMLWriter(out, doc);
        writer.write();
        assertEquals(content, out.toString());
    }

    public void testWriteLineSeparator() throws Exception {
        doc.putProperty(StyledEditorKit.EndOfLineStringProperty, "`");
        writer = new TestHTMLWriter(out, doc);
        writer.writeLineSeparator();
        assertEquals("`", out.toString());

        writer.setLineSeparator("~");
        writer.writeLineSeparator();
        assertEquals("`~", out.toString());
    }

    public void testOutput() throws Exception {
        String content = "abc<def";
        writer.output(content.toCharArray(), 0, content.length());
        assertEquals(content, out.toString());

        content = "abc&lt;&gt;def";
        out = new StringWriter();
        createDocument(content);
        createWriter();
        writer.text(body);
        assertEquals(content, out.toString());
    }

    public void testHTMLWriterWriterHTMLDocument() {
        assertEquals(80, writer.getLineLength());
    }

    public void testHTMLWriterWriterHTMLDocumentIntInt() {
        assertEquals(80, writer.getLineLength());
    }

    public void testComment() throws Exception {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(HTML.Tag.I, HTML.Tag.I);
        attrs.addAttribute(HTML.Attribute.COMMENT, "comment body");

        doc.callWriteLock();
        Element elem = doc.createBranchElement(body, attrs);
        setTag(elem, HTML.Tag.COMMENT);
        doc.callWriteUnlock();

        writer.incrIndent();
        writer.writeEmbeddedTags(attrs);
        writer.comment(elem);
        assertEquals("<i><!--comment body-->~", out.toString());
    }

    public void testEmptyTag() throws Exception {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(HTML.Tag.I, HTML.Tag.I);

        writer.incrIndent();
        writer.writeEmbeddedTags(attrs);
        setTag(body, HTML.Tag.CONTENT);
        writer.emptyTag(body);
        writer.emptyTag(body);
        assertEquals("<i>  </i>normal boldnormal bold", out.toString());

        out = new StringWriter();
        writer = new TestHTMLWriter(out, doc);
        writer.setLineSeparator("~");
        writer.incrIndent();
        writer.writeEmbeddedTags(attrs);
        setTag(body, HTML.Tag.COMMENT);
        writer.emptyTag(body);
        assertEquals("<i>  </i><!---->~", out.toString());

        setTag(body, HTML.Tag.P);
        out = new StringWriter();
        writer = new TestHTMLWriter(out, doc);
        writer.setLineSeparator("~");
        writer.incrIndent();
        writer.writeEmbeddedTags(attrs);
        writer.emptyTag(body);
        assertEquals("<i>  </i><p>~", out.toString());
    }

    public void testIsBlockTag() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        assertFalse(writer.isBlockTag(attrs));

        attrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.BODY);
        assertTrue(writer.isBlockTag(attrs));

        attrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.I);
        assertFalse(writer.isBlockTag(attrs));
    }

    public void testMatchNameAttribute() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        assertFalse(writer.matchNameAttribute(attrs, null));
        assertFalse(writer.matchNameAttribute(attrs, HTML.Tag.BODY));

        attrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.I);
        assertTrue(writer.matchNameAttribute(attrs, HTML.Tag.I));
        assertFalse(writer.matchNameAttribute(attrs, HTML.Tag.BODY));
        assertFalse(writer.matchNameAttribute(attrs, null));
    }

    public void testStartTag() throws Exception {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(HTML.Tag.I, HTML.Tag.I);
        writer.writeEmbeddedTags(attrs);
        setTag(body, HTML.Tag.P);
        writer.incrIndent();
        writer.startTag(body);
        assertEquals("<i></i>  <p>~", out.toString());

        out = new StringWriter();
        writer = new TestHTMLWriter(out, doc);
        writer.setLineSeparator("~");
        writer.writeEmbeddedTags(attrs);
        setTag(body, HTML.Tag.IMPLIED);
        writer.incrIndent();
        writer.startTag(body);
        assertEquals("<i>", out.toString());

        out = new StringWriter();
        writer = new TestHTMLWriter(out, doc);
        writer.setLineSeparator("~");
        writer.writeEmbeddedTags(attrs);
        setTag(body, HTML.Tag.BODY);
        writer.incrIndent();
        writer.setLineLength(5);
        writer.startTag(body);
        assertEquals("<i></i>  <head>~~  </head>~  <body>~", out.toString());
    }

    public void testEndTag() throws Exception {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(HTML.Tag.I, HTML.Tag.I);
        writer.writeEmbeddedTags(attrs);
        writer.incrIndent();
        writer.endTag(body);
        assertEquals("<i></i>  </body>~", out.toString());

        out = new StringWriter();
        writer = new TestHTMLWriter(out, doc);
        writer.setLineSeparator("~");
        writer.writeEmbeddedTags(attrs);
        setTag(body, HTML.Tag.IMPLIED);
        writer.incrIndent();
        writer.endTag(body);
        assertEquals("<i>", out.toString());
    }

    public void testSynthesizedElement() {
        assertFalse(writer.synthesizedElement(root));

        setTag(root, HTML.Tag.BODY);
        assertFalse(writer.synthesizedElement(root));

        setTag(root, HTML.Tag.IMPLIED);
        assertTrue(writer.synthesizedElement(root));
    }

    public void testText() throws Exception {
        String content = "abc&lt;&gt; def";
        createDocument(content);
        createWriter();
        writer.setLineLength(7);
        writer.incrIndent();
        writer.text(body);
        assertEquals("abc&lt;&gt; ~  def", out.toString());

        out = new StringWriter();
        createWriter();

        writer.setLineLength(7);
        writer.incrIndent();
        setTag(body, HTML.Tag.PRE);
        writer.startTag(body);
        writer.text(body);
        assertEquals("  <pre>abc&lt;&gt; def", out.toString());
    }

    public void testSelectContent() throws Exception {
        String content = "<select>\n  <option selected>Component1</option>\n"
                + "  <option>Component2</option>\n</select>";
        createDocument(content);
        createWriter();
        Element elem = doc.getElement(body, StyleConstants.NameAttribute,
                                      HTML.Tag.SELECT);
        writer.selectContent(elem.getAttributes());
        assertEquals("  <option selected>Component1~  <option>Component2~",
                     out.toString());

        content = "<select multiple>\n  <option selected>Component1</option>\n"
                + "  <option>Component2</option>\n</select>";
        createDocument(content);
        out = new StringWriter();
        createWriter();
        elem = doc.getElement(body, StyleConstants.NameAttribute,
                              HTML.Tag.SELECT);
        writer.selectContent(elem.getAttributes());
        assertEquals("  <option selected>Component1~  <option>Component2~",
                     out.toString());

        if (isHarmony()) {
            content = "<select>\n  <optgroup label=optgr>\n"
                    + "    <option selected>Comp1</option>\n"
                    + "    <option>Comp2</option>\n  </optgroup>\n</select>";
            createDocument(content);
            out = new StringWriter();
            createWriter();
            elem = doc.getElement(body, StyleConstants.NameAttribute,
                                  HTML.Tag.SELECT);
            writer.selectContent(elem.getAttributes());
            assertEquals("  <optgroup label=\"optgr\">~"
                         + "    <option selected>Comp1~    <option>Comp2~"
                         + "  </optgroup>",
                         out.toString());
        }
    }

    public void testTextAreaContent() throws Exception {
        String content = "<textarea>\n   First line&lt;.\n"
                + "   Second line.\n   </textarea>";
        createDocument(content);
        createWriter();

        Element elem = doc.getElement(body, StyleConstants.NameAttribute,
                                      HTML.Tag.TEXTAREA);
        writer.textAreaContent(elem.getAttributes());
        assertEquals("     First line&lt;.~     Second line.~     ~",
                     out.toString());
    }

    public void testWriteAttributes() throws IOException {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(HTML.Tag.H1, HTML.Tag.H2);
        attrs.addAttribute(StyleConstants.Bold, StyleConstants.Bold);
        attrs.addAttribute(HTML.Attribute.ENDTAG, HTML.Attribute.ENDTAG);
        attrs.addAttribute(HTML.Attribute.COLOR, "red");
        attrs.addAttribute(CSS.Attribute.MARGIN, new Integer(10));

        writer.writeAttributes(attrs);
        assertEquals(" style=\"margin: 10\" color=\"red\"", out.toString());
    }

    public void testWriteEmbeddedTags() throws IOException {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(HTML.Tag.I, HTML.Tag.B);
        attrs.addAttribute(HTML.Tag.H1, HTML.Tag.H2);

        writer.writeEmbeddedTags(attrs);
        writer.writeEmbeddedTags(attrs);
        assertTrue("<i><h1>".equals(out.toString())
                   || "<h1><i>".equals(out.toString()));
    }

    public void testCloseOutUnwantedEmbeddedTags() throws IOException {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(HTML.Tag.I, HTML.Tag.B);
        attrs.addAttribute(HTML.Tag.H1, HTML.Tag.H2);
        writer.writeEmbeddedTags(attrs);

        SimpleAttributeSet attrs2 = new SimpleAttributeSet();
        attrs2.addAttribute(HTML.Tag.I, HTML.Tag.B);
        writer.closeOutUnwantedEmbeddedTags(attrs2);
        assertTrue("<i><h1></h1>".equals(out.toString())
                   || "<h1><i></i></h1>".equals(out.toString()));

        out = new StringWriter();
        createWriter();
        writer.writeEmbeddedTags(attrs);
        attrs2.addAttribute(HTML.Tag.H1, HTML.Tag.H2);
        writer.closeOutUnwantedEmbeddedTags(attrs2);
        assertTrue("<i><h1>".equals(out.toString())
                   || "<h1><i>".equals(out.toString()));
    }

    public void testWriteOption() throws IOException {
        writer.incrIndent();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        Option option = new Option(attrs);
        writer.writeOption(option);
        assertEquals("  <option>~", out.toString());

        out = new StringWriter();
        createWriter();
        writer.incrIndent();
        attrs.addAttribute(HTML.Attribute.SELECTED, Boolean.valueOf(true));
        option = new Option(attrs);
        writer.writeOption(option);
        assertEquals("  <option selected>~", out.toString());

        out = new StringWriter();
        createWriter();
        writer.incrIndent();
        attrs.addAttribute(HTML.Attribute.VALUE, "option_value");
        option = new Option(attrs);
        writer.writeOption(option);
        assertEquals("  <option value=option_value selected>~", out.toString());

        out = new StringWriter();
        createWriter();
        attrs.addAttribute(HTML.Attribute.VALUE, "");
        option = new Option(attrs);
        option.setLabel("option_label");
        writer.incrIndent();
        writer.writeOption(option);
        assertEquals("  <option value= selected>option_label~",
                     out.toString());
    }

    public void testWriteDocumentBase() throws Exception {
        if (!isHarmony()) {
            return;
        }

        String content = "<html><head><base href=\"http://my.site.com/index.html\""
            + "</head></html>";
        createDocument(content);
        createWriter();

        writer.write();

        assertEquals("<html>~  <head>~    <base href=\"http://my.site.com/index.html\">    ~"
                     + "  </head>~</html>~",
                     out.toString());
    }

    private void createDocument(final String content) throws Exception {
        doc = new TestHTMLDocument();
        doc.setAsynchronousLoadPriority(-1);  // synchronous loading

        new HTMLEditorKit().read(new StringReader(content), doc, 0);

        root = doc.getDefaultRootElement();
        body = doc.getElement(root, StyleConstants.NameAttribute, HTML.Tag.BODY);
    }

    private void createWriter() {
        writer = new TestHTMLWriter(out, doc);
        writer.setLineSeparator("~");
    }

    private void setTag(final Element elem, final HTML.Tag tag) {
        doc.callWriteLock();
        ((AbstractDocument.AbstractElement)elem).addAttribute(
                StyleConstants.NameAttribute, tag);
        doc.callWriteUnlock();
    }
}
