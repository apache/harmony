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
 * @author Alexey A. Ivanov, Evgeniya G. Maenkova
 */
package org.apache.harmony.awt.text;

import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;

/**
 * Stores composed text parameters: the  composed text itself,
 * its length and where it starts in a document. Also stores the latest
 * committed text start. Instance of this class is used
 * as a value of document property
 *  <code>AbstractDocument.ComposedTextProperty</code>.
 *
 */
public class ComposedTextParams {
    private Position lastCommittedTextStart;
    private int composedTextLength;
    private Document document;
    private Position composedTextStart;
    private AttributedString composedText;

    public ComposedTextParams(final Document document) {
        this.document = document;
    }

    public int getComposedTextStart() {
        return composedTextStart.getOffset();
    }

    public void setComposedTextStart(final int offset) {
        try {
            composedTextStart = document.createPosition(offset);
        } catch (BadLocationException e) {
        }
    }

    public int getLastCommittedTextStart() {
        return lastCommittedTextStart.getOffset();
    }

    public void setLastCommittedTextStart(final int offset) {
        try {
            lastCommittedTextStart = document.createPosition(offset);
        } catch (BadLocationException e) {
        }
    }

    public void setComposedTextLength(final int length) {
        composedTextLength = length;
    }

    public int getComposedTextLength() {
        return composedTextLength;
    }

    public int getLastCommittedTextLength() {
        return getComposedTextStart() - getLastCommittedTextStart();
    }

    public void setComposedText(final AttributedString text) {
        composedText = text;
    }

    public AttributedString getComposedText() {
        return composedText;
    }

    public void resetComposedText() {
        composedTextLength = 0;
        composedText = null;
    }

    public void setComposedText(final AttributedCharacterIterator iterator,
                                final int composedStart, final int end) {
        composedText = new AttributedString(iterator, composedStart, end);
        composedTextLength = end - composedStart;
    }
}
