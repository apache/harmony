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
 * @author Alexey A. Ivanov, Roman I. Chernyatchik
 */
package javax.swing.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Vector;

import javax.swing.text.AbstractDocument.Content;
import javax.swing.undo.UndoableEdit;

import org.apache.harmony.x.swing.internal.nls.Messages;

public final class StringContent
    implements Content, Serializable {

    private static final String INVALID_START_POSITION_MESSAGE = Messages.getString("swing.0F"); //$NON-NLS-1$

    private final class StringContentEdit
        extends AbstractContentUndoableEdit implements UndoableEdit {

        public StringContentEdit(final int where, final String chars,
                                         final boolean isInsertCommand)
            throws BadLocationException {

            super(where, chars, isInsertCommand);
        }

        protected Vector getPositionsInRange(final Vector positions,
                                             final int where,
                                             final int len) {
             return StringContent.this.getPositionsInRange(positions, where,
                                                           len);
        }

        protected void updateUndoPositions(final Vector undoPos) {
            StringContent.this.updateUndoPositions(undoPos);
        }

        protected void insertItems(final int where, final String chars) {
            StringContent.this.insertItems(where, chars);
        }

        protected void removeItems(final int where, final int len) {
            StringContent.this.removeItems(where, len);
        }
    }



    private static final int DEFAULT_SIZE = 10;
    private static final int MIN_SIZE = 1;

    private int size;
    private int length;
    private char[] innerContent;
    private transient ContentPositions stringContentPositions;


    public StringContent() {
        this(DEFAULT_SIZE);
    }

    public StringContent(final int initialLength) {
        initTransientFields();
        size = initialLength < MIN_SIZE ? MIN_SIZE : initialLength;
        innerContent = new char[size];
        length = 1;
        innerContent[0] = '\n';
    }

    public int length() {
        return length;
    }

    public UndoableEdit insertString(final int where, final String str)
                                     throws BadLocationException {

        if (str == null) {
            throw new NullPointerException();
        }

        checkInvalidOnInsert(where);

        insertItems(where, str);
        // Following code creates "Insert" command.
        return new StringContentEdit(where, str, true);
    }

    public UndoableEdit remove(final int where, final int nitems)
        throws BadLocationException {

        checkInvalidOnRemove(where, nitems);


        // Following code creates "Remove" command and executes it.
        StringContentEdit removeEdit =
            new StringContentEdit(where, getString(where, nitems),
                                          false);
        removeItems(where, nitems);

        return removeEdit;

    }

    public String getString(final int where, final int len)
        throws BadLocationException {
        checkInvalidOnGetChars(where, len);

        return String.copyValueOf(innerContent, where, len);
    }

    public void getChars(final int where, final int len, final Segment chars)
        throws BadLocationException {

        checkInvalidOnGetChars(where, len);

        chars.array = innerContent;
        chars.offset = where;
        chars.count = len;
    }

    public Position createPosition(final int offset)
        throws BadLocationException {

        return stringContentPositions.createPosition(offset);
    }


    protected Vector getPositionsInRange(final Vector vector, final int offset,
                                         final int len) {

         return stringContentPositions.getPositionsInRange(vector, offset, len);
    }

    protected void updateUndoPositions(final Vector positions) {

        stringContentPositions.updateUndoPositions(positions);
    }

    private void checkInvalidOnRemove(final int where, final int nitems)
        throws BadLocationException {
        if (nitems < 0) {
            throw new BadLocationException(Messages.getString("swing.99"), //$NON-NLS-1$
                                            length);
        }

        if (where < 0 || where + nitems >= length) {
            throw new BadLocationException(INVALID_START_POSITION_MESSAGE,
                                           where);
        }
    }

    private void checkInvalidOnGetChars(final int where, final int len)
        throws BadLocationException {
        if (len < 0) {
            throw new BadLocationException(Messages.getString("swing.8C"), //$NON-NLS-1$
                                           length);
        }

        if (where < 0 || where + len > length) {
            throw new BadLocationException(INVALID_START_POSITION_MESSAGE,
                                           where);
        }
    }

    private void checkInvalidOnInsert(final int where)
        throws BadLocationException {
        if (where < 0 || where >= length) {
            throw new BadLocationException(INVALID_START_POSITION_MESSAGE,
                                           where);
        }
    }

    private char[] enlargeInnerContent(final int where, final int newSize) {
        final int newContentSize = newSize > size * 2 ? newSize * 2 : size * 2;
        char[] newContent = new char[newContentSize];
        System.arraycopy(innerContent, 0, newContent, 0, where);
        return newContent;
    }

    private void shiftOldDataBeforeInsert(final int where,
                                          final int insertedStringLength,
                                          final char[] newContent) {
        System.arraycopy(innerContent, where, newContent,
                         where + insertedStringLength,
                         length - where);
    }

    // Add string to content without checkOnInvalid parameters
    private void insertItems(final int where, final String str) {
        final int insertedStringLength = str.length();
        final int newLength = insertedStringLength + length;

        if (str.length() <= 0) {
            return;
        }

        char[] newContent = innerContent;
        // If new content length will exceed current content size
        // we shoult enlarge content's size.
        if (newLength >= size) {
            newContent = enlargeInnerContent(where, newLength);
        }
        shiftOldDataBeforeInsert(where, insertedStringLength, newContent);

        if (newLength >= size) {
            innerContent = newContent;
            size = innerContent.length;
        }
        length = newLength;

        str.getChars(0, insertedStringLength, innerContent, where);
        stringContentPositions.moveMarkIndexes(where, insertedStringLength);
    }

    // Remove part of content without checkOnInvalid parameters
    private void removeItems(final int where, final int nitems) {
        System.arraycopy(innerContent,
                         where + nitems,
                         innerContent,
                         where,
                         length - where - nitems);

        stringContentPositions.setMarkIndexes(where, where + nitems, where);
        stringContentPositions.moveMarkIndexes(where + nitems, -nitems);
        length = length - nitems;
    }

    private void initTransientFields() {
        stringContentPositions = new ContentPositions() {

            protected int getOffsetForDocumentMark(final int index) {
                return index;
            }

            protected int setOffsetForDocumentMark(final int offset) {
                return offset;
            }
        };
    }

    private void readObject(final ObjectInputStream ois)
    throws IOException, ClassNotFoundException {

        ois.defaultReadObject();

        initTransientFields();
    }

    private void writeObject(final ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }
}
