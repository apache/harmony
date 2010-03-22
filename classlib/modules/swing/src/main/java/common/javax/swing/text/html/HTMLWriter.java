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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import javax.swing.text.AbstractWriter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.Entity;

import org.apache.harmony.x.swing.text.html.form.FormSelectModel;
import org.apache.harmony.x.swing.text.html.form.FormTextModel;
import org.apache.harmony.x.swing.text.html.form.FormOptionGroup;

public class HTMLWriter extends AbstractWriter {
    private static final int DEFAULT_LINE_LENGTH = 80;
    private static DTD dtd;

    private boolean writingContent;
    private boolean indentEmptyTag = true;
    private boolean preformatted;
    private boolean toWriteHead = true;
    private Stack elemsStack;
    private Vector openEmbeddedTags = new Vector();
    private boolean isOptionGroupOpen;

    public HTMLWriter(final Writer w, final HTMLDocument doc) {
        super(w, doc);
        setLineLength(DEFAULT_LINE_LENGTH);
    }

    public HTMLWriter(final Writer w, final HTMLDocument doc,
                      final int pos, final int len) {
        super(w, doc, pos, len);
        setLineLength(DEFAULT_LINE_LENGTH);
    }

    public void write() throws IOException, BadLocationException {
        if (elemsStack == null) {
            elemsStack = new Stack();
        }
        ElementIterator it = getElementIterator();

        Element e;
        while ((e = it.next()) != null) {
            if (!elemsStack.isEmpty()) {
                while (!elemsStack.isEmpty()
                        && elemsStack.peek() != e.getParentElement()) {
                    if (!synthesizedElement((Element)elemsStack.peek())) {
                        decrIndent();
                    }
                    if (!preformatted && getCurrentLineLength() != 0) {
                        writeLineSeparator();
                    }
                    endTag((Element)elemsStack.pop());
                }
            }

            if (isEmptyTag(getHTMLTag(e.getAttributes()))) {
                emptyTag(e);
            } else if (matchNameAttribute(e.getAttributes(), HTML.Tag.TITLE)) {
                continue;
            } else {
                if (getCurrentLineLength() != 0 && getCanWrapLines()) {
                    writeLineSeparator();
                }
                startTag(e);
                elemsStack.push(e);
                if (!synthesizedElement(e)) {
                    incrIndent();
                }
                indentEmptyTag = !preformatted;
                if (matchNameAttribute(e.getAttributes(), HTML.Tag.HEAD)) {
                    writeDocumentProperties();
                }
            }
        }

        while (!elemsStack.isEmpty()) {
            if (!synthesizedElement((Element)elemsStack.peek())) {
                decrIndent();
            }
            if (!preformatted && getCurrentLineLength() != 0) {
                writeLineSeparator();
            }
            endTag((Element)elemsStack.pop());
        }

        writeAdditionalComment();
    }

    protected void comment(final Element elem)
        throws BadLocationException, IOException {

        if (!matchNameAttribute(elem.getAttributes(), HTML.Tag.COMMENT)) {
            return;
        }

        Object comment = elem.getAttributes().getAttribute(HTML.Attribute.COMMENT);
        write("<!--");
        if (comment != null) {
            write(comment.toString());
        }
        write("-->");
        writeLineSeparator();
    }

    protected void emptyTag(final Element elem)
        throws BadLocationException, IOException {

        if (indentEmptyTag) {
            indent();
            indentEmptyTag = false;
        }
        closeOutUnwantedEmbeddedTags(elem.getAttributes());
        writeEmbeddedTags(elem.getAttributes());
        if (matchNameAttribute(elem.getAttributes(), HTML.Tag.CONTENT)) {
            text(elem);
        } else if (matchNameAttribute(elem.getAttributes(), HTML.Tag.COMMENT)) {
            comment(elem);
        } else {
            write("<" + getHTMLTag(elem.getAttributes()).toString());
            writeAttributes(elem.getAttributes());
            write('>');
            if (!elem.isLeaf()) {
                writeLineSeparator();
            }
        }
    }

    protected boolean isBlockTag(final AttributeSet attr) {
        HTML.Tag tag = getHTMLTag(attr);
        return tag == null ? false : tag.isBlock();
    }

    protected boolean matchNameAttribute(final AttributeSet attr,
                                         final HTML.Tag tag) {
        return tag == null ? false : tag.equals(getHTMLTag(attr));
    }

    protected boolean synthesizedElement(final Element elem) {
        return matchNameAttribute(elem.getAttributes(), HTML.Tag.IMPLIED);
    }

    protected void output(final char[] chars, final int start, final int length)
        throws IOException {

        if (!writingContent) {
            super.output(chars, start, length);
            return;
        }

        StringBuilder buffer = new StringBuilder();
        int writtenLength = 0;
        for (int i = 0; i < length; i++) {
            String entity = getEntity(chars[start + i]);
            if (entity != null) {
                buffer.append(chars, start + writtenLength, i - writtenLength);
                buffer.append(entity);
                writtenLength = i + 1;
            }
        }
        if (writtenLength == 0) {
            super.output(chars, start, length);
        } else {
            buffer.append(chars, start + writtenLength, length - writtenLength);
            char[] content = buffer.toString().toCharArray();
            super.output(content, 0, content.length);
        }
    }

    protected void startTag(final Element elem)
        throws IOException, BadLocationException {

        if (synthesizedElement(elem)) {
            return;
        }

        closeOutUnwantedEmbeddedTags(elem.getAttributes());
        indent();

        if (matchNameAttribute(elem.getAttributes(), HTML.Tag.HEAD)) {
            toWriteHead = false;
        } else if (toWriteHead
                && matchNameAttribute(elem.getAttributes(), HTML.Tag.BODY)) {
            writeSynthesizedHead();
        }

        HTML.Tag tag = getHTMLTag(elem.getAttributes());
        writeTag(tag, elem.getAttributes());

        preformatted = tag.isPreformatted();
        if (!preformatted) {
            writeLineSeparator();
        }

        if (matchNameAttribute(elem.getAttributes(), HTML.Tag.TEXTAREA)) {
            textAreaContent(elem.getAttributes());
        } else if (matchNameAttribute(elem.getAttributes(), HTML.Tag.SELECT)) {
            selectContent(elem.getAttributes());
        }
    }

    protected void endTag(final Element elem) throws IOException {
        if (synthesizedElement(elem)) {
            return;
        }

        closeOutUnwantedEmbeddedTags(elem.getAttributes());
        if (!preformatted) {
            indent();
        }
        HTML.Tag tag = getHTMLTag(elem.getAttributes());
        write("</" + tag + ">");
        writeLineSeparator();
    }

    protected void text(final Element elem)
        throws BadLocationException, IOException {

        writingContent = true;
        super.text(elem);
        writingContent = false;
    }

    protected void selectContent(final AttributeSet attr) throws IOException {
        FormSelectModel model =
            (FormSelectModel)attr.getAttribute(StyleConstants.ModelAttribute);
        incrIndent();
        for (int i = 0; i < model.getOptionCount(); i++) {
            if (model.getOption(i) instanceof FormOptionGroup) {
                startOptionGroup(model.getOption(i));
            } else {
                writeOption(model.getOption(i));
            }
        }
        endOptionGroup();
        decrIndent();
    }

    protected void textAreaContent(final AttributeSet attr)
        throws BadLocationException, IOException {

        FormTextModel model = (FormTextModel)attr
                .getAttribute(StyleConstants.ModelAttribute);
        incrIndent();
        writingContent = true;
        indent();
        write(model.getInitialContent());
        writingContent = false;
        decrIndent();
        writeLineSeparator();
    }

    protected void writeAttributes(final AttributeSet attr) throws IOException {
        writeCSSAttributes(attr, " style=\"", "\"");

        for (Enumeration attrs = attr.getAttributeNames();
             attrs.hasMoreElements();) {

            Object a = attrs.nextElement();
            if (a instanceof HTML.Tag
                    || a instanceof StyleConstants
                    || a instanceof CSS.Attribute
                    || HTML.Attribute.ENDTAG.equals(a)) {
                continue;
            }
            write(" " + a + "=\"" + attr.getAttribute(a) + "\"");
        }
    }

    protected void writeEmbeddedTags(final AttributeSet attr) throws IOException {
        for (Enumeration attrs = attr.getAttributeNames();
             attrs.hasMoreElements();) {

            Object a = attrs.nextElement();
            if (a instanceof HTML.Tag && !openEmbeddedTags.contains(a)) {
                Object value = attr.getAttribute(a);
                writeTag((HTML.Tag)a,
                         value instanceof AttributeSet ? (AttributeSet)value : null);
                openEmbeddedTags.add(a);
            }
        }
    }

    protected void closeOutUnwantedEmbeddedTags(final AttributeSet attr)
        throws IOException {

        int start = 0;
        while (start < openEmbeddedTags.size()
                && attr.isDefined(openEmbeddedTags.get(start))) {
            start++;
        }

        for (int i = openEmbeddedTags.size() - 1; i >= start; i--) {
            Object a = openEmbeddedTags.get(i);
            write("</" + a + ">");
            openEmbeddedTags.remove(a);
        }
    }

    protected void writeLineSeparator() throws IOException {
        super.writeLineSeparator();
    }

    protected void writeOption(final Option option) throws IOException {
        indent();
        StringBuilder buffer = new StringBuilder(50);
        buffer.append("<option");
        String value =
            (String)option.getAttributes().getAttribute(HTML.Attribute.VALUE);
        if (value != null) {
            buffer.append(" value=");
            buffer.append(value);
        }
        if (option.isSelected()) {
            buffer.append(" selected");
        }
        buffer.append(">");
        if (option.getLabel() != null) {
            buffer.append(option.getLabel());
        }

        write(buffer.toString());
        writeLineSeparator();
    }

    private void startOptionGroup(final Option option) throws IOException {
        if (isOptionGroupOpen) {
            endOptionGroup();
        }

        isOptionGroupOpen = true;
        indent();
        StringBuilder buffer = new StringBuilder(50);
        buffer.append("<optgroup");
        if (option.getLabel() != null) {
            buffer.append(" label=\"");
            buffer.append(option.getLabel());
            buffer.append("\"");
        }
        buffer.append(">");

        write(buffer.toString());
        writeLineSeparator();
        incrIndent();
    }

    private void endOptionGroup() throws IOException {
        if (!isOptionGroupOpen) {
            return;
        }

        decrIndent();
        indent();
        write("</optgroup>");
        writeLineSeparator();
        isOptionGroupOpen = false;
    }

    protected boolean getCanWrapLines() {
        return super.getCanWrapLines() && (!preformatted && writingContent);
    }

    private static HTML.Tag getHTMLTag(final AttributeSet attrs) {
        return (HTML.Tag)attrs.getAttribute(StyleConstants.NameAttribute);
    }

    private String getEntity(final char ch) {
        boolean useName = ch == '<' || ch == '>' || ch == '&' || ch == '"';
        Entity entity = (Entity)getDTD().entityHash.get(new Integer(ch));
        if (entity == null) {
            return null;
        } else if (useName) {
            return "&" + entity.getName() + ";";
        } else {
            return "&#" + Integer.toString(ch) + ";";
        }
    }

    private static DTD getDTD() {
        if (dtd == null) {
            try {
                dtd = DTD.getDTD("writer");
                dtd.read(new DataInputStream(
                        dtd.getClass().getResourceAsStream("transitional401.bdtd")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return dtd;
    }

    private boolean isEmptyTag(final HTML.Tag tag) {
        if (HTML.Tag.CONTENT.equals(tag) || HTML.Tag.COMMENT.equals(tag)) {
            return true;
        }
        return getDTD().getElement(tag.toString()).isEmpty();
    }

    private void writeTag(final HTML.Tag tag, final AttributeSet attrs)
        throws IOException {

        write("<" + tag);
        if (attrs != null) {
            writeAttributes(attrs);
        }
        write('>');
    }

    private void writeSynthesizedHead() throws IOException {
        writeTag(HTML.Tag.HEAD, null);
        writeLineSeparator();
        writeLineSeparator();
        indent();
        write("</" + HTML.Tag.HEAD + ">");
        writeLineSeparator();
        indent();
    }

    private void writeDocumentProperties() throws IOException {
        writeEmbeddedStyleSheet();
        writeDocumentTitle();
        writeDocumentBase();
    }

    private void writeEmbeddedStyleSheet() throws IOException {
        StyleSheet ss = ((HTMLDocument)getDocument()).getStyleSheet();
        Enumeration styles = ss.getStyleNames();
        boolean firstRule = true;
        while (styles.hasMoreElements()) {
            String styleName = (String)styles.nextElement();
            if (StyleSheet.DEFAULT_STYLE.equals(styleName)) {
                continue;
            }
            if (firstRule) {
                indent();
                write("<style type=\"text/css\">");
                writeLineSeparator();
                incrIndent();
                indent();
                write("<!--");
                writeLineSeparator();
                incrIndent();
                firstRule = false;
            }
            Style styleRule = ss.getStyle(styleName);
            indent();
            write(styleName);
            writeCSSAttributes(styleRule, " { ", " }");
            writeLineSeparator();
        }
        if (!firstRule) {
            decrIndent();
            indent();
            write("-->");
            writeLineSeparator();
            decrIndent();
            indent();
            write("</style>");
            writeLineSeparator();
        }
    }

    private void writeCSSAttributes(final AttributeSet attr,
                                    final String begin,
                                    final String end) throws IOException {
        boolean firstCSSAttr = true;
        for (Enumeration attrs = attr.getAttributeNames();
            attrs.hasMoreElements();) {

            Object a = attrs.nextElement();
            if (a instanceof CSS.Attribute) {
                if (firstCSSAttr) {
                    firstCSSAttr = false;
                    write(begin);
                } else {
                    write("; ");
                }
                write(a + ": " + attr.getAttribute(a));
            }
        }
        if (!firstCSSAttr) {
            write(end);
        }
    }

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

    private void writeDocumentBase() throws IOException {
        URL url = (URL)getDocument().getProperty(HTMLDocument.INITIAL_BASE_PROPERTY);
        if (url == null) {
            return;
        }

        indent();
        write("<base href=\"");
        write(url.toString());
        write("\">");
    }

    private void writeAdditionalComment() throws IOException {
        Object comments = getDocument().getProperty(HTMLDocument.AdditionalComments);
        if (comments == null) {
            return;
        }

        Vector strings = (Vector)comments;
        for (Iterator it = strings.iterator(); it.hasNext();) {
            write("<!--");
            write(it.next().toString());
            write("-->");
            writeLineSeparator();
        }
    }
}
