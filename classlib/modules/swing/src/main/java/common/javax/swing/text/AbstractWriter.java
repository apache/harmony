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
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Enumeration;

public abstract class AbstractWriter {
    protected static final char NEWLINE = 10;
    private static final char[] SPACES = createArrayOfSpaces(64);

    private Document doc;
    private Writer out;
    private ElementIterator iterator;
    private int startOffset;
    private int endOffset;
    private int indentSpace = 2;
    private int lineLength = 100;
    private boolean canWrapLines = true;
    private int indentLevel;
    private int currentLineLength;
    private String lineSeparator;
    private boolean isLineStillEmpty;

    protected AbstractWriter(final Writer w, final Document doc) {
        this(w, doc.getDefaultRootElement(), 0, doc.getLength());
    }

    protected AbstractWriter(final Writer w, final Document doc,
                             final int pos, final int len) {
        this(w, doc.getDefaultRootElement(), pos, len);
    }

    protected AbstractWriter(final Writer w, final Element root) {
        this(w, root, 0, root.getEndOffset());
    }

    protected AbstractWriter(final Writer w, final Element root,
                             final int pos, final int len) {
        doc = root.getDocument();
        out = w;
        iterator = new ElementIterator(root);
        startOffset = pos;
        endOffset = pos + len;
        initLineSeparator();
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public void setLineSeparator(final String value) {
        lineSeparator = value;
    }


    public String getLineSeparator() {
        return lineSeparator;
    }

    protected Writer getWriter() {
        return out;
    }

    protected Document getDocument() {
        return doc;
    }

    protected ElementIterator getElementIterator() {
        return iterator;
    }

    protected String getText(final Element elem) throws BadLocationException {
        return getDocument().getText(elem.getStartOffset(),
                                     elem.getEndOffset() - elem.getStartOffset());
    }

    protected boolean inRange(final Element next) {
        int start = next.getStartOffset();
        int end = next.getEndOffset();
        return getStartOffset() <= start && start < getEndOffset()
                || getStartOffset() < end && end <= getEndOffset()
                || start <= getStartOffset() && getEndOffset() <= end;
    }

    protected void setLineLength(final int l) {
        lineLength = l;
    }

    protected int getLineLength() {
        return lineLength;
    }

    protected boolean isLineEmpty() {
        return getCurrentLineLength() == 0 || isLineStillEmpty;
    }

    protected void setCurrentLineLength(final int length) {
        currentLineLength = length;
    }

    protected int getCurrentLineLength() {
        return currentLineLength;
    }

    protected void setCanWrapLines(final boolean newValue) {
        canWrapLines = newValue;
    }

    protected boolean getCanWrapLines() {
        return canWrapLines;
    }

    protected void incrIndent() {
        if ((getIndentLevel() + 1) * getIndentSpace() <= getLineLength()) {
            indentLevel++;
        }
    }

    protected void decrIndent() {
        indentLevel--;
    }

    protected int getIndentLevel() {
        return indentLevel;
    }

    protected void indent() throws IOException {
        boolean wasLineEmpty = isLineEmpty();

        int indentLineLength = getIndentLevel() * getIndentSpace();
        char indentLine[];
        if (indentLineLength <= SPACES.length) {
            indentLine = SPACES;
        } else {
            indentLine = createArrayOfSpaces(indentLineLength);
        }
        output(indentLine, 0, indentLineLength);

        isLineStillEmpty = wasLineEmpty;
    }

    protected void setIndentSpace(final int space) {
        indentSpace = space;
    }

    protected int getIndentSpace() {
        return indentSpace;
    }

    protected void output(final char[] content,
                          final int start, final int length)
        throws IOException {

        out.write(content, start, length);
        setCurrentLineLength(getCurrentLineLength() + length);
        isLineStillEmpty = false;
    }
    protected void text(final Element elem) throws BadLocationException,
                                                   IOException {
        String content = getText(elem);
        int textStart = Math.max(getStartOffset(), elem.getStartOffset())
            - elem.getStartOffset();
        int textEnd = Math.min(getEndOffset(), elem.getEndOffset())
            - elem.getStartOffset();

        if (textEnd > textStart) {
            write(content.toCharArray(), textStart, textEnd - textStart);
        }
    }

    protected void write(final char ch) throws IOException {
        char chars[] = {ch};
        write(chars, 0, 1);
    }

    protected void write(final char[] chars, final int start,
                         final int length) throws IOException {
        boolean canWrapLines = getCanWrapLines();

        int writtenLength = 0;
        boolean firstLine = true;
        while (writtenLength < length) {
            if (!firstLine && canWrapLines) {
                indent();
            }
            firstLine = false;
            int lineLength = findNewline(chars, start + writtenLength,
                                         length - writtenLength);
            if (!canWrapLines) {
                output(chars, start + writtenLength, lineLength);
            } else {
                writeWrappedLine(chars, start + writtenLength, lineLength);
            }
            writtenLength += lineLength + 1;
            if (writtenLength <= length) {
                writeLineSeparator();
            }
        }
    }

    protected void write(final String content) throws IOException {
        char chars[] = content.toCharArray();
        write(chars, 0, chars.length);
    }

    protected void writeAttributes(final AttributeSet attrs)
        throws IOException {

        StringBuilder content = new StringBuilder();
        for (Enumeration keys = attrs.getAttributeNames(); keys.hasMoreElements();) {
            Object key = keys.nextElement();
            content.append(" ");
            content.append(key);
            content.append("=");
            content.append(attrs.getAttribute(key));
        }

        write(content.toString());
    }

    protected void writeLineSeparator() throws IOException {
        char chars[] = getLineSeparator().toCharArray();
        output(chars, 0, chars.length);
        setCurrentLineLength(0);
    }

    protected abstract void write() throws IOException, BadLocationException;

    private void initLineSeparator() {
        Object separator = (String)getDocument().getProperty(
                DefaultEditorKit.EndOfLineStringProperty);
        if (!(separator instanceof String)) {
            PrivilegedAction action = new PrivilegedAction() {
                public Object run() {
                    return System.getProperty("line.separator");
                }
            };

            separator = AccessController.doPrivileged(action);
        }
        lineSeparator = (String)separator;
    }

    private int findNewline(final char[] chars, final int start, final int length) {
        for (int i = 0; i < length; i++) {
            if (chars[start + i] == NEWLINE) {
                return i;
            }
        }
        return length;
    }

    private int findWrappedLineBreak(final char[] chars, final int start,
                                     final int length) {
        for (int i = 0; i < length; i++) {
            if (Character.isWhitespace(chars[start + i])) {
                return i + 1;
            }
        }
        return length;
    }

    private void writeWrappedLine(final char[] chars, final int start,
                                  final int length) throws IOException {
        int writtenLength = 0;
        boolean firstLine = true;
        while (writtenLength < length) {
            int lineLength = findWrappedLineBreak(chars, start + writtenLength,
                                                  length - writtenLength);
            if (!firstLine
                    && getCurrentLineLength() + lineLength >= getLineLength()) {
                writeLineSeparator();
                indent();
            }
            output(chars, start + writtenLength, lineLength);
            writtenLength += lineLength;
            firstLine = false;
        }
    }

    private static char[] createArrayOfSpaces(final int length) {
        char[] spaces = new char[length];
        Arrays.fill(spaces, ' ');
        return spaces;
    }
}
