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

import java.util.ArrayList;
import java.util.List;

import org.apache.harmony.awt.text.PropertyNames;


public class PlainDocument extends AbstractDocument {
    public static final String lineLimitAttribute = "lineLimit";

    public static final String tabSizeAttribute = "tabSize";

    private static final int DEFAULT_TABSIZE = 8;

    // Default root element. Stores line map.
    private BranchElement defRoot;

    public PlainDocument() {
        this(new GapContent());
    }

    public PlainDocument(final Content content) {
        super(content);
        putProperty(tabSizeAttribute, new Integer(DEFAULT_TABSIZE));
        defRoot = (BranchElement)createDefaultRoot();
    }

    public Element getDefaultRootElement() {
        return defRoot;
    }

    /**
     * Shortcut method to get element from default root.
     */
    public Element getParagraphElement(final int offset) {
        return defRoot.getElement(defRoot.getElementIndex(offset));
    }

    /**
     * It is expected that the result is of type
     * <code>AbstractDocument.BranchElement</code>. If this is not the case,
     * the constructor will fail, the exception being thrown
     * <code>ClassCastException</code>.
     */
    protected AbstractElement createDefaultRoot() {
        BranchElement root = (BranchElement)createBranchElement(null, null);
        root.replace(0, 0, new Element[] {
            createLeafElement(root, null,
                              getStartPosition().getOffset(),
                              getEndPosition().getOffset())
        });
        return root;
    }

    protected void insertUpdate(final DefaultDocumentEvent event,
                                final AttributeSet attrs) {
        List   lines  = null;
        String text   = null;
        int    offset = event.getOffset();
        int    length = event.getLength();

        try {
            text = getText(offset, length);
        } catch (final BadLocationException e) { }

        boolean hasLineBreak = text.indexOf('\n') != -1;
        boolean prevCharIsLineBreak = false;
        try {
            prevCharIsLineBreak = offset > 0
                                  && getText(offset - 1, 1).charAt(0) == '\n';
        } catch (final BadLocationException e) { }
        boolean lastCharIsLineBreak = text.charAt(text.length() - 1) == '\n';

        int lineIndex;
        int lineStart;
        int lineEnd;

        if (prevCharIsLineBreak && !lastCharIsLineBreak) {
            // If the inserted text does not end with new line char
            // and at the same time the previous character in the document
            // is new line char, we need to remove two paragraphs:
            // the one just before the insertion point (inserted text will
            // join it) and the following

            lineIndex = defRoot.getElementIndex(offset - 1);
            Element prevLine = defRoot.getElement(lineIndex);
            Element nextLine = defRoot.getElement(lineIndex + 1);

            lineStart = prevLine.getStartOffset();
            lineEnd   = nextLine.getEndOffset();

            lines = new ArrayList();
            try {
                breakLines(getText(lineStart, lineEnd - lineStart), lineStart,
                           lines);
            } catch (final BadLocationException e) { }

            Element[] lineArray =
                (Element[])lines.toArray(new Element[lines.size()]);

            updateStructure(event, lineIndex,
                            new Element[] {prevLine, nextLine},
                            lineArray);
        } else if (hasLineBreak) {
            // Paragraph structure is changed
            lineIndex = defRoot.getElementIndex(offset);
            Element line = defRoot.getElement(lineIndex);
            lineStart = line.getStartOffset();
            lineEnd   = line.getEndOffset();

            lines = new ArrayList();
            try {
                breakLines(getText(lineStart, lineEnd - lineStart), lineStart,
                           lines);
            } catch (final BadLocationException e) { }

            Element[] lineArray =
                (Element[])lines.toArray(new Element[lines.size()]);
            updateStructure(event, lineIndex, new Element[] {line}, lineArray);
        }

        super.insertUpdate(event, attrs);
    }

    protected void removeUpdate(final DefaultDocumentEvent event) {
        String text   = null;
        int    offset = event.getOffset();
        int    length = event.getLength();

        try {
            text = getText(offset, length);
        } catch (final BadLocationException e) {
            return;
        }

        boolean hasLineBreak = text.indexOf('\n') != -1;

        if (hasLineBreak) {
            // Paragraph structure is changed
            int index1 = defRoot.getElementIndex(offset);
            int index2 = defRoot.getElementIndex(offset + length);
            Element line1 = defRoot.getElement(index1);
            Element line2 = defRoot.getElement(index2);

            int count = index2 - index1 + 1;
            Element[] removed = new Element[count];
            System.arraycopy(defRoot.elements, index1, removed, 0, count);

            updateStructure(
                    event,
                    index1,
                    removed,
                    new Element[] {createLeafElement(defRoot,
                                                     null,
                                                     line1.getStartOffset(),
                                                     line2.getEndOffset())});
        }

        super.removeUpdate(event);
    }

    final void doInsert(final int offset, final String text,
                        final AttributeSet attrs) throws BadLocationException {
        super.doInsert(offset,
                       getFilterNewLines() ? text.replace('\n', ' ')
                                           : text,
                       attrs);
    }

    private void breakLines(final String text, final int start,
                            final List lines) {
        final int length = text.length();

        int prevLineBreak = 0;
        int lineBreak = text.indexOf('\n');
        do {
            ++lineBreak;

            lines.add(createLeafElement(defRoot, null,
                                        start + prevLineBreak,
                                        start + lineBreak));
            prevLineBreak = lineBreak;
            lineBreak = text.indexOf('\n', prevLineBreak);
        } while (prevLineBreak < length);
    }

    private boolean getFilterNewLines() {
        Object value = getProperty(PropertyNames.FILTER_NEW_LINES);
        if (value != null) {
            return ((Boolean)value).booleanValue();
        }
        return false;
    }

    private void updateStructure(final DefaultDocumentEvent event,
                                 final int index,
                                 final Element[] removed,
                                 final Element[] added) {
        event.addEdit(new ElementEdit(defRoot, index, removed, added));
        defRoot.replace(index, removed.length, added);
    }

}

