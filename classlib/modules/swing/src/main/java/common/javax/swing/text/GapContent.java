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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Vector;

import javax.swing.text.ContentPositions.DocumentMark;
import javax.swing.undo.UndoableEdit;

import org.apache.harmony.x.swing.internal.nls.Messages;

@SuppressWarnings("serial")
public class GapContent
    implements AbstractDocument.Content, Serializable {

    private final class GapContentEdit extends AbstractContentUndoableEdit
            implements UndoableEdit {

        public GapContentEdit(final int where, final String chars,
                              final boolean isInsertCommand)
            throws BadLocationException {

            super(where, chars, isInsertCommand);
        }

        @Override
        protected Vector getPositionsInRange(final Vector positions,
                                             final int where,
                                             final int length) {
             return GapContent.this.getPositionsInRange(positions, where,
                                                           length);
        }

        @Override
        protected void updateUndoPositions(final Vector undoPos) {
            GapContent.this.updateUndoPositions(undoPos, pos, len);

        }

        @Override
        protected void insertItems(final int where, final String chars) {
            try {
                GapContent.this.insertItems(where, chars.toCharArray(), len);
            } catch (BadLocationException e) { }

        }

        @Override
        protected void removeItems(final int where, final int length) {
            try {
                GapContent.this.removeItems(where, length);
            } catch (BadLocationException e) { }
        }
    }

    private final class GapContentPositions extends ContentPositions {
        /**
         * Resets internal index in Position implementation to be zero if
         * the position offset is zero. This ensures that position isn't moved
         * any more.
         */
        protected void resetMarksAtZero() {
           GapContent.this.resetMarksAtZero();
        }

        @Override
        protected int setOffsetForDocumentMark(final int offset) {
            if (offset == 0 || offset < gapStart) {
                return offset;
            }
            return offset + (gapEnd - gapStart);
        }

        @Override
        protected int getOffsetForDocumentMark(final int index) {
            if (index == 0 || index < gapStart) {
                return index;
            }
            return index - (gapEnd - gapStart);
        }
    }

    private static final int DEFAULT_SIZE = 10;
    private static final int MININIMUM_SIZE = 2;

    private char[] array;
    private int gapEnd;
    private int gapStart;
    private transient GapContentPositions gapContentPositions;

    private transient Segment textBuffer;

    public GapContent() {
        this(DEFAULT_SIZE);
    }

    public GapContent(final int initialLength) {
        int length = initialLength;
        if (length < MININIMUM_SIZE) {
            length = MININIMUM_SIZE;
        }
        array = (char[])allocateArray(length);

        // Put the implied character into the storage
        array[length - 1] = '\n';
        gapEnd = length - 1;

        initTransientFields();
    }

    public Position createPosition(final int offset)
        throws BadLocationException {

       return gapContentPositions.createPosition(offset);
    }

    public void getChars(final int offset,
                         final int length,
                         final Segment chars)
        throws BadLocationException {

        if (length < 0) {
            throw new BadLocationException(Messages.getString("swing.8C"), //$NON-NLS-1$
                                           length);
        }
        if (offset < 0 || length > length() - offset) {
            throw new BadLocationException(Messages.getString("swing.8D"), offset); //$NON-NLS-1$
        }

        if (offset + length <= gapStart) {
            // The whole portion requested is before the gap
            chars.array  = array;
            chars.offset = offset;
            chars.count  = length;
        } else if (offset < gapStart) {
            // The gap is in the middle of the portion requested
            if (chars.isPartialReturn()) {
                chars.array  = array;
                chars.offset = offset;
                chars.count  = gapStart - offset;
            } else {
                char[] result = new char[length];
                int beforeGapLen = gapStart - offset;
                int afterGapLen  = length - beforeGapLen;

                System.arraycopy(array, offset, result, 0, beforeGapLen);
                System.arraycopy(array, gapEnd, result, beforeGapLen,
                                 afterGapLen);

                chars.array  = result;
                chars.offset = 0;
                chars.count  = length;
            }
        } else {
            // where is somewhere in the gap or after it
            chars.array  = array;
            chars.offset = offset + (gapEnd - gapStart);
            chars.count  = length;
        }
    }

    public String getString(final int offset, final int length)
        throws BadLocationException {

        getChars(offset, length, textBuffer);
        return textBuffer.toString();
    }

    public UndoableEdit insertString(final int offset, final String str)
        throws BadLocationException {

        insertItems(offset, str.toCharArray(), str.length());
        return new GapContentEdit(offset, str, true);
    }

    public int length() {
        return getArrayLength() - (gapEnd - gapStart);
    }

    public UndoableEdit remove(final int offset, final int nitems)
        throws BadLocationException {

        GapContentEdit de = new GapContentEdit(offset,
                                           getString(offset, nitems),
                                           false);
        removeItems(offset, nitems);

        return de;
    }

    protected Object allocateArray(final int len) {
        return new char[len];
    }

    protected final Object getArray() {
        return array;
    }

    protected int getArrayLength() {
        return array.length;
    }

    /**
     * Returns the index of the first character right after the gap.
     */
    protected final int getGapEnd() {
        return gapEnd;
    }

    /**
     * Returns the index of the first character in the gap.
     */
    protected final int getGapStart() {
        return gapStart;
    }

    /**
     * This method returns a vector with instances of UndoPosRef (inner class)
     * which store information to restore position offset after undo/redo.
     */
    protected Vector getPositionsInRange(final Vector vector,
                                         final int offset,
                                         final int len) {

        return gapContentPositions.getPositionsInRange(vector, offset, len);
    }

    protected void replace(final int position, final int rmSize,
                           final Object addItems, final int addSize) {
        try {
            removeItems(position, rmSize);
            insertItems(position, addItems, addSize);
        } catch (BadLocationException e) {
        }
    }

    protected void resetMarksAtZero() {
        gapContentPositions.deletePositions();

        if (gapStart > 0) {
            return;
        }

        for (int i = 0; i < gapContentPositions.positionList.size(); i++) {
            DocumentMark dm =
                (DocumentMark)gapContentPositions.positionList.get(i);

            if (dm.index <= gapEnd) {
                dm.index = 0;
            } else if (dm.index > gapEnd) {
                break;
            }
        }
    }

    protected void shiftEnd(final int newSize) {
        final Object oldArray = array;
        final int len = getArrayLength();
        final int oldStart = gapStart;
        final int oldEnd = gapEnd;
        final int oldEndOff = len - oldEnd;

        array = (char[])allocateArray((newSize << 1) + 2);
        final int sizeDiff = array.length - len;

        gapStart = oldStart;
        gapEnd = getArrayLength() - oldEndOff;

        System.arraycopy(oldArray, 0, array, 0, oldStart);
        System.arraycopy(oldArray, oldEnd, array, gapEnd, oldEndOff);

        gapContentPositions.deletePositions();
        gapContentPositions.moveMarkIndexes(oldStart, -1, sizeDiff);
    }

    protected void shiftGap(final int newGapStart) {
        if (newGapStart == gapStart) {
            return;
        }

        final int gapSize     = gapEnd - gapStart;
        final int oldGapStart = gapStart;
        final int oldGapEnd   = gapEnd;
        final int newGapEnd   = newGapStart + gapSize;
        final int gapDiff = gapStart - newGapStart;

        // Move items affected and adjust the gap position
        if (newGapStart < gapStart) {
            System.arraycopy(array, newGapStart, array, gapEnd - gapDiff,
                             gapDiff);
        } else {
            System.arraycopy(array, gapEnd, array, gapStart, -gapDiff);
        }
        gapStart = newGapStart;
        gapEnd  -= gapDiff;

        gapContentPositions.deletePositions();
        if (newGapStart < oldGapStart) { // This is shift left
            gapContentPositions.moveMarkIndexes(newGapStart, oldGapStart,
                                                gapSize);
        } else {
            gapContentPositions.moveMarkIndexes(oldGapEnd, newGapEnd - 1,
                                                -gapSize);
        }
        gapContentPositions.resetMarksAtZero();
    }

    protected void shiftGapEndUp(final int newGapEnd) {
        gapContentPositions.deletePositions();
        gapContentPositions.setMarkIndexes(gapEnd, newGapEnd, newGapEnd);

        gapEnd = newGapEnd;

        gapContentPositions.resetMarksAtZero();
    }

    protected void shiftGapStartDown(final int newGapStart) {
        gapContentPositions.deletePositions();
        gapContentPositions.setMarkIndexes(newGapStart, gapStart, gapEnd);

        gapStart = newGapStart;

        gapContentPositions.resetMarksAtZero();
    }

    /**
     * Restores offset of positions that fall into the range. It is used by
     * Undo/Redo implementation (DocumentEdit inner class).
     * The <code>vector</code> parameter is the vector returned by
     * <code>getPositionsInRange</code> method.
     */
    protected void updateUndoPositions(final Vector vector,
                                       final int offset,
                                       final int len) {

       gapContentPositions.updateUndoPositions(vector);
    }

    private void initTransientFields() {
        textBuffer    = new Segment();
        gapContentPositions = new GapContentPositions();
    }

    final void insertItems(final int where, final Object addItems,
                           final int addSize)
        throws BadLocationException {

        if (addSize == 0) {
            return;
        }

        if (where < 0 || where > length()) {
            throw new BadLocationException(Messages.getString("swing.8E"), where); //$NON-NLS-1$
        }

        shiftGap(where);
        ensureCapacity(addSize);

        System.arraycopy(addItems, 0, array, gapStart, addSize);
        gapStart += addSize;
    }

    private void ensureCapacity(final int sizeRequired) {
        int gapSize = gapEnd - gapStart;
        if (sizeRequired >= gapSize) {
            shiftEnd(length() + sizeRequired);
        }
    }

    private void readObject(final ObjectInputStream ois)
        throws IOException, ClassNotFoundException {

        ois.defaultReadObject();

        initTransientFields();
    }

    final void removeItems(final int where, final int nitems)
        throws BadLocationException {

        if (where < 0 || where + nitems >= length()) {

            throw new BadLocationException(Messages.getString("swing.7F"), where); //$NON-NLS-1$
        }

        if (nitems == 0) {
            return;
        }

        if (where + nitems == gapStart) {
            shiftGapStartDown(where);
        } else {
            shiftGap(where);
            shiftGapEndUp(gapEnd + nitems);
        }
    }

    private void writeObject(final ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }
}
