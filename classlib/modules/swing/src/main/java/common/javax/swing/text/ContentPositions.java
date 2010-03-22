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

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.harmony.x.swing.internal.nls.Messages;

abstract class ContentPositions {
    public final class DocumentMark implements Position, Comparable {
        protected int index;

        protected final Reference ref;

        private DocumentMark(final int offset) {
            setOffset(offset);
            ref = null;
        }

        private DocumentMark(final int offset, final Position pos) {
            setOffset(offset);
            this.ref = new MarkReference(pos, this, positionQueue);
            insertPosition(this);
        }

        /*
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(final Object obj) {
            return compare((DocumentMark) obj);
        }

        public int getOffset() {
            return getOffsetForDocumentMark(index);
        }

        protected int compare(final DocumentMark dm) {
            return index - dm.index;
        }

        protected boolean isEqual(final DocumentMark dm) {
            return compare(dm) == 0;
        }

        protected boolean isGreater(final DocumentMark dm) {
            return compare(dm) > 0;
        }

        protected boolean isLess(final DocumentMark dm) {
            return compare(dm) < 0;
        }

        protected void setOffset(final int offset) {
            index = setOffsetForDocumentMark(offset);
        }
    }

    private static final class MarkReference extends PhantomReference {
        protected final DocumentMark hostMark;

        public MarkReference(final Object userMark,
                final DocumentMark hostMark, final ReferenceQueue queue) {
            super(userMark, queue);
            this.hostMark = hostMark;
        }
    }

    private final class UndoPosRef {
        private int documentOffset;

        private WeakReference posWeakRef;

        protected UndoPosRef(final WeakReference weakReference) {
            posWeakRef = weakReference;
            DocumentMark positionMark = (DocumentMark) posWeakRef.get();
            if (positionMark != null) {
                documentOffset = positionMark.getOffset();
            }
        }

        protected void restorePos() {
            DocumentMark positionMark = (DocumentMark) posWeakRef.get();
            if (positionMark != null) {
                positionMark.setOffset(documentOffset);
            }
        }
    }

    private final class WeakPosition implements Position {
        private final DocumentMark mark;

        public WeakPosition(final int offset) {
            mark = new DocumentMark(offset, this);
        }

        public int getOffset() {
            return mark.getOffset();
        }
    }

    protected List positionList = new ArrayList();

    private ReferenceQueue positionQueue = new ReferenceQueue();

    private DocumentMark searchMark = new DocumentMark(0);

    public Position createPosition(final int offset)
            throws BadLocationException {

        if (offset < 0) {
            throw new BadLocationException(Messages.getString("swing.85"), offset); //$NON-NLS-1$
        }

        return new WeakPosition(offset);
    }

    /**
     * Deletes position instances from the list that were cleared by the garbage
     * collector. It is called (or must be called) in the methods modifying this
     * list before the modification is done.
     */
    public void deletePositions() {
        MarkReference ref;
        while ((ref = (MarkReference) positionQueue.poll()) != null) {
            removePosition(ref.hostMark);
            ref.clear();
        }
    }

    public Vector getPositionsInRange(final Vector vector, final int offset,
            final int len) {
        Vector vect = vector;
        if (vect == null) {
            vect = new Vector();
        }

        deletePositions();

        for (int i = getStartIndexByOffset(offset); i < positionList.size();
             i++) {

            DocumentMark documentMark = (DocumentMark) positionList.get(i);

            final int markOffset = documentMark.getOffset();
            assert offset <= markOffset : "Failed @ " + i + ": " + offset
                    + " > " + markOffset;
            if (markOffset <= offset + len) {
                vect.add(new UndoPosRef(new WeakReference(documentMark)));
            } else {
                break;
            }
        }
        return vect;
    }

    public void moveMarkIndexes(final int startIndex, final int diff) {

        moveMarkIndexesByPosListIndexes(getStartIndexByIndex(startIndex),
                positionList.size(), diff);

    }

    public void moveMarkIndexes(final int startIndex, final int endIndex,
            final int diff) {

        moveMarkIndexesByPosListIndexes(getStartIndexByIndex(startIndex),
                getEndIndexByIndex(endIndex), diff);
    }

    public void setMarkIndexes(final int startIndex, final int endIndex,
            final int newIndex) {

        final int limit = getEndIndexByIndex(endIndex);
        for (int i = getStartIndexByIndex(startIndex); i < limit; i++) {
            DocumentMark dm = (DocumentMark) positionList.get(i);
            dm.index = newIndex;
        }
    }

    public void updateUndoPositions(final Vector positions) {

        UndoPosRef undoPositionReference;

        deletePositions();

        for (int i = 0; i < positions.size(); i++) {
            Object item = positions.get(i);
            if (!(item instanceof UndoPosRef)) {
                continue;
            }

            undoPositionReference = (UndoPosRef) item;
            undoPositionReference.restorePos();
        }

        Collections.sort(positionList);
    }

    protected abstract int getOffsetForDocumentMark(final int index);

    protected abstract int setOffsetForDocumentMark(final int offset);

    private int getStartIndexByOffset(final int offset) {
        searchMark.setOffset(offset);
        return getStartIndexByIndex(searchMark.index);
    }

    private int getStartIndexByIndex(final int index) {
        if (index == 0) {
            // Marks at index 0 must never be updated
            for (int i = 0; i < positionList.size(); i++) {
                DocumentMark dm = (DocumentMark) positionList.get(i);
                if (dm.index > 0) {
                    return i;
                }
            }
            return positionList.size();
        }

        searchMark.index = index;
        int result = findIndex(searchMark);
        if (result > 0) {
            DocumentMark dm;
            do {
                dm = (DocumentMark) positionList.get(result - 1);
            } while (!dm.isLess(searchMark) && --result > 0);
        }
        return result;
    }

    private int getEndIndexByIndex(final int index) {
        final int lastMark = positionList.size() - 1;
        if (index == -1 || lastMark < 0) {
            return lastMark + 1;
        }

        DocumentMark dm = (DocumentMark) positionList.get(lastMark);
        if (index >= dm.index) {
            return lastMark + 1;
        }

        searchMark.index = index;
        int result = findIndex(searchMark);
        if (result < lastMark) {
            do {
                dm = (DocumentMark) positionList.get(result);
            } while (!dm.isGreater(searchMark) && ++result < lastMark);
        }
        return result;
    }

    private int findIndex(final DocumentMark documentMark) {
        int result = Collections.binarySearch(positionList, documentMark);
        return result < 0 ? -result - 1 : result + 1;
    }

    private void moveMarkIndexesByPosListIndexes(final int startPosListIndex,
            final int endPosListIndex, final int diff) {

        for (int i = startPosListIndex; i < endPosListIndex; i++) {
            DocumentMark dm = (DocumentMark) positionList.get(i);
            dm.index += diff;
        }
    }

    private void insertPosition(final DocumentMark documentMark) {
        deletePositions();

        final int index = findIndex(documentMark);
        positionList.add(index, documentMark);
    }

    private void removePosition(final DocumentMark position) {
        int foundPos = Collections.binarySearch(positionList, position);
        int pos = foundPos;
        Position current = null;
        while (pos >= 0
                && (current = (Position) positionList.get(pos)) != position
                && current.getOffset() == position.getOffset()) {
            pos--;
        }
        if (current == position) {
            positionList.remove(pos);
            return;
        }

        current = null;
        pos = foundPos + 1;
        while (pos < positionList.size()
                && (current = (Position) positionList.get(pos)) != position
                && current.getOffset() == position.getOffset()) {
            pos++;
        }
        if (current == position) {
            positionList.remove(pos);
        }
    }
}
