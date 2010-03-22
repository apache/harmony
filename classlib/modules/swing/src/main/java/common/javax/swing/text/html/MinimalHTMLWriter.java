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
import java.io.Writer;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AbstractWriter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class MinimalHTMLWriter extends AbstractWriter {
    private static final AttributeSet EMPTY_ATTR_SET = new SimpleAttributeSet();
    private Vector openEmbeddedTags = new Vector();
    private boolean inFontTag;

    public MinimalHTMLWriter(final Writer w, final StyledDocument doc,
                             final int pos, final int len) {

        super(w, doc, pos, len);
    }

    public MinimalHTMLWriter(final Writer w, final StyledDocument doc) {
        super(w, doc);
    }

    protected boolean inFontTag() {
        return inFontTag;
    }

    protected boolean isText(final Element elem) {
        return AbstractDocument.ContentElementName.equals(elem.getName());
    }

    protected void startFontTag(final String style) throws IOException {
        writeStartTag("<font style=\"" + style + "\">");
    }

    protected void endFontTag() throws IOException {
        writeLineSeparator();
        writeEndTag("</font>");
    }

    protected void text(final Element elem) throws IOException,
            BadLocationException {

        String content = getText(elem);
        int textStart = Math.max(getStartOffset(), elem.getStartOffset())
            - elem.getStartOffset();
        int textEnd = Math.min(getEndOffset(), elem.getEndOffset())
            - elem.getStartOffset();
        if (textEnd > textStart && content.charAt(textEnd - 1) == NEWLINE) {
            textEnd--;
        }

        if (textEnd > textStart) {
            write(content.toCharArray(), textStart, textEnd - textStart);
        }
    }

    public void write() throws IOException, BadLocationException {
        writeStartTag("<html>");
        writeHeader();
        writeBody();
        writeEndTag("</html>");
    }

    protected void writeAttributes(final AttributeSet attr) throws IOException {
        for (Enumeration attrs = attr.getAttributeNames();
                attrs.hasMoreElements();) {

            Object a = attrs.nextElement();

            String attrString = writeAttributeAsCSS(a, attr.getAttribute(a));
            if (attrString != null) {
                indent();
                write(attrString);
                write(";");
                writeLineSeparator();
            }
        }
    }

    protected void writeBody() throws IOException, BadLocationException {
        writeStartTag("<body>");

        Element root = getDocument().getDefaultRootElement();
        for (int i = root.getElementIndex(getStartOffset());
             i < root.getElementCount();
             i++) {

            Element e = root.getElement(i);
            if (!inRange(e)) {
                break;
            }
            writeStartParagraph(e);
            writeParagraphElements(e);
            writeEndParagraph();
        }

        writeEndTag("</body>");
    }

    protected void writeContent(final Element elem, final boolean needsIndenting)
            throws IOException, BadLocationException {

        writeNonHTMLAttributes(elem.getAttributes());
        if (needsIndenting) {
            indent();
        }
        writeHTMLTags(elem.getAttributes());
        text(elem);
    }

    protected void writeStartTag(final String tag) throws IOException {
        indent();
        write(tag);
        writeLineSeparator();
        incrIndent();
    }

    protected void writeEndParagraph() throws IOException {
        writeEndHTMLTags(EMPTY_ATTR_SET);
        writeLineSeparator();
        if (inFontTag()) {
            writeEndSpan();
        }
        writeEndTag("</p>");
    }

    protected void writeEndTag(final String endTag) throws IOException {
        decrIndent();
        indent();
        write(endTag);
        writeLineSeparator();
    }

    protected void writeHTMLTags(final AttributeSet attr) throws IOException {
        if (StyleConstants.isBold(attr)) {
            writeHTMLTagIfNeeded(HTML.Tag.B);
        }
        if (StyleConstants.isItalic(attr)) {
            writeHTMLTagIfNeeded(HTML.Tag.I);
        }
        if (StyleConstants.isUnderline(attr)) {
            writeHTMLTagIfNeeded(HTML.Tag.U);
        }
    }

    protected void writeHeader() throws IOException {
        writeStartTag("<head>");

        writeStartTag("<style type=\"text/css\">");
        writeStartTag("<!--");
        writeStyles();
        writeEndTag("-->");
        writeEndTag("</style>");

        writeDocumentTitle();

        writeEndTag("</head>");
    }

    protected void writeImage(final Element elem) throws IOException {
        indent();
    }

    protected void writeComponent(final Element elem) throws IOException {
        indent();
    }

    protected void writeLeaf(final Element elem) throws IOException {
        if (StyleConstants.IconElementName.equals(elem.getName())) {
            writeImage(elem);
        } else if (StyleConstants.ComponentElementName.equals(elem.getName())) {
            writeComponent(elem);
        } else {
            indent();
        }
    }

    protected void writeNonHTMLAttributes(final AttributeSet attr)
            throws IOException {

        writeStartSpan(attr);
    }

    protected void writeStartParagraph(final Element elem) throws IOException {
        writeStartTag("<p class=" + getParagraphStyleName(elem) + ">");
    }

    protected void writeStyles() throws IOException {
        if (!(getDocument() instanceof DefaultStyledDocument)) {
            // XXX: it's not clear what to do in this case
            throw new UnsupportedOperationException(Messages.getString("swing.9F")); //$NON-NLS-1$
        }

        StyledDocument styledDocument = (StyledDocument)getDocument();
        Enumeration styles = ((DefaultStyledDocument)getDocument()).getStyleNames();
        while (styles.hasMoreElements()) {
            String styleName = (String)styles.nextElement();
            if (StyleSheet.DEFAULT_STYLE.equals(styleName)) {
                continue;
            }
            indent();
            write("p." + styleName + " {");
            writeLineSeparator();
            incrIndent();
            writeAttributes(styledDocument.getStyle(styleName));
            decrIndent();
            indent();
            write("}");
            writeLineSeparator();
        }
    }

    private void writeHTMLTagIfNeeded(final HTML.Tag tag) throws IOException {
        if (!openEmbeddedTags.contains(tag)) {
            write("<" + tag.toString() + ">");
            openEmbeddedTags.add(tag);
        }
    }

    private void writeEndHTMLTagIfNeeded(final HTML.Tag tag) throws IOException {
        if (openEmbeddedTags.contains(tag)) {
            write("</" + tag.toString() + ">");
            openEmbeddedTags.remove(tag);
        }
    }

    // HTML tags opening sequence is <b>, <i>, <u>,
    // so, we have to close it in reverce order
    private void writeEndHTMLTags(final AttributeSet attr) throws IOException {
        if (!StyleConstants.isUnderline(attr)) {
            writeEndHTMLTagIfNeeded(HTML.Tag.U);
        }
        if (!StyleConstants.isItalic(attr)) {
            writeEndHTMLTagIfNeeded(HTML.Tag.I);
        }
        if (!StyleConstants.isBold(attr)) {
            writeEndHTMLTagIfNeeded(HTML.Tag.B);
        }
    }

    private static String getParagraphStyleName(final Element par) {
        AttributeSet attrs = par.getAttributes();
        Object style = attrs.getAttribute(StyleConstants.ResolveAttribute);
        return style instanceof Style
            ? ((Style)style).getName()
            : StyleContext.DEFAULT_STYLE;
    }

    private void writeStartSpan(final AttributeSet attr) throws IOException {
        if (inFontTag()) {
            writeEndSpan();
        }

        boolean firstAttr = true;
        for (Enumeration attrs = attr.getAttributeNames();
                attrs.hasMoreElements();) {

            Object a = attrs.nextElement();
            if (StyleConstants.Italic.equals(a)
                    || StyleConstants.Bold.equals(a)
                    || StyleConstants.Underline.equals(a)
                    || a instanceof StyleConstants.ParagraphConstants) {
                continue;
            }
            if (!firstAttr) {
                write("; ");
            } else {
                writeEndHTMLTags(EMPTY_ATTR_SET);
                indent();
                write("<span style=\"");
            }
            String attrString = writeAttributeAsCSS(a, attr.getAttribute(a));
            if (attrString != null) {
                write(attrString);
                firstAttr = false;
            }
        }

        if (!firstAttr) {
            write("\">");
            writeLineSeparator();
            incrIndent();
            inFontTag = true;
        } else {
            writeEndHTMLTags(attr);
        }
    }

    private void writeEndSpan() throws IOException {
        writeEndHTMLTags(EMPTY_ATTR_SET);
        if (!isLineEmpty()) {
            writeLineSeparator();
        }
        writeEndTag("</span>");
        inFontTag = false;
    }

    private String writeAttributeAsCSS(final Object attr, final Object value)
            throws IOException {

        Object cssAttr = convertToCSSAttribute(attr);
        if (cssAttr == null) {
            return null;
        }

        Object cssValue = convertToCSSValue(cssAttr, value);
        return cssAttr.toString() + ": " + cssValue.toString();
    }

    private void writeParagraphElements(final Element paragraph)
            throws IOException, BadLocationException {

        for (int i = paragraph.getElementIndex(getStartOffset());
             i < paragraph.getElementCount();
             i++) {
            Element e = paragraph.getElement(i);
            if (!inRange(e)) {
                break;
            }
            if (isText(e)) {
                writeContent(e, isLineEmpty());
            } else {
                writeLeaf(e);
            }
        }
    }

    private static Object convertToCSSAttribute(final Object attr) {
        return CSS.mapToCSSForced(attr);
    }

    private static Object convertToCSSValue(final Object cssAttr,
                                            final Object value) {
        return cssAttr instanceof CSS.Attribute
            ? ((CSS.Attribute)cssAttr).getConverter().toCSS(value)
            : value;
    }

    // TODO: the same method exists in HTMLWriter
    private void writeDocumentTitle() throws IOException {
        Object title = getDocument().getProperty(Document.TitleProperty);
        if (title == null) {
            return;
        }

        indent();
        write("<title>");
        write(title.toString());
        write("</title>");
        writeLineSeparator();
    }
}
