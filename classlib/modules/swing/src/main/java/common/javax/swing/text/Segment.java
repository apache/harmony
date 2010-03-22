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
package javax.swing.text;

import java.text.CharacterIterator;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class Segment implements Cloneable, CharacterIterator {

    public char[] array;

    public int count;

    public int offset;

    private boolean isPartial;

    private int pos;

    public Segment() {
        this(null, 0, 0);
    }

    public Segment(final char[] array, final int offset, final int count) {
        this.array = array;
        this.offset = offset;
        this.count = count;

        this.pos = 0;
        this.isPartial = false;
    }

    @Override
    public Object clone() {
        Object clone;

        try {
            clone = super.clone();
        } catch (final CloneNotSupportedException e) {
            clone = null;
        }

        return clone;
    }

    public char current() {
        if (pos < 0 || pos >= count + offset) {
            return DONE;
        }
        return array[pos];
    }

    public char first() {
        pos = offset;

        if (isEmpty()) {
            return DONE;
        }

        return array[pos];
    }

    public int getBeginIndex() {
        return offset;
    }

    public int getEndIndex() {
        return offset + count;
    }

    public int getIndex() {
        return pos;
    }

    public boolean isPartialReturn() {
        return isPartial;
    }

    public char last() {
        if (isEmpty()) {
            pos = offset + count;
            return DONE;
        }

        pos = offset + count - 1;

        return array[pos];
    }

    public char next() {
        pos++;

        if (pos >= offset + count) {
            pos = offset + count;
            return DONE;
        }

        return array[pos];
    }

    public char previous() {
        if (pos == offset) {
            return DONE;
        }

        return array[--pos];
    }

    public char setIndex(final int position) {
        if (position < 0 || position > offset + count) {
            throw new IllegalArgumentException(Messages.getString("swing.89", position)); //$NON-NLS-1$
        }

        pos = position;

        if (position == offset + count) {
            return DONE;
        }

        return array[pos];
    }

    public void setPartialReturn(final boolean p) {
        isPartial = p;
        return;
    }

    @Override
    public String toString() {
        return array != null ? new String(array, offset, count) : "";
    }

    private boolean isEmpty() {
        if (count == 0 || array == null || array.length == 0) {
            return true;
        }
        return false;
    }

}
