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

package javax.swing;

import java.awt.Rectangle;
import java.io.Serializable;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.harmony.awt.gl.MultiRectArea;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>DefaultListSelectionModel</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class DefaultListSelectionModel implements ListSelectionModel, Cloneable, Serializable {
    private static final long serialVersionUID = -3207109908101807625L;

    private static final int NOT_SET = -1;

    private static class Segment extends Rectangle {
        private static final long serialVersionUID = 1L;

        public Segment(int begin, int end) {
            super(Math.min(begin, end), 0, Math.abs(end - begin) + 1, 1);
        }

        public void add(Segment s) {
            if (s == null || s.isEmpty()) {
                return;
            } else if (isEmpty()) {
                setBounds(s);
            } else {
                setBounds(this.union(s));
            }
        }

        public int getBeginIndex() {
            return x;
        }

        public int getEndIndex() {
            return x + width - 1;
        }

        public int getLength() {
            return width;
        }
    }

    private static class Selection extends MultiRectArea implements Cloneable {
        private static final Rectangle EMPTY_RECTANGLE = new Rectangle();

        public Selection() {
            super();
        }

        private Selection(Selection s) {
            super(s);
        }

        public void clear() {
            intersect(EMPTY_RECTANGLE);
        }

        public boolean contains(int index) {
            return contains(index, 0);
        }

        public void insertIndices(int index, int length, boolean multiSelectionAllowed) {
            MultiRectArea modified = new MultiRectArea();
            Rectangle[] rects = getRectangles();
            for (int i = 0; i < rects.length; i++) {
                Rectangle rect = (Rectangle) rects[i].clone();
                if (index < rect.x) {
                    rect.x += length;
                } else if (rect.x <= index && index < rect.x + rect.width) {
                    if (multiSelectionAllowed) {
                        rect.width += length;
                    } else {
                        rect.x += length;
                    }
                }
                modified.add(rect);
            }
            clear();
            add(modified);
        }

        public void removeIndices(int index, int length) {
            MultiRectArea modified = new MultiRectArea();
            Rectangle[] rects = getRectangles();
            for (int i = 0; i < rects.length; i++) {
                Rectangle rect = rects[i];
                int rectEnd = rect.x + rect.width - length - 1;
                if (index <= rect.x) {
                    if (rectEnd >= index) {
                        int rectBegin = rect.x - length < index ? index : rect.x - length;
                        modified.add(new Segment(rectBegin, rectEnd));
                    }
                } else if (rect.x < index && index < rect.x + rect.width) {
                    if (rectEnd < index - 1) {
                        rectEnd = index - 1;
                    }
                    modified.add(new Segment(rect.x, rectEnd));
                } else {
                    modified.add((Rectangle) rect.clone());
                }
            }
            clear();
            add(modified);
        }

        public Segment getDifferenceBounds(Selection anotherSelection) {
            MultiRectArea thisFromAnother = MultiRectArea.subtract(this, anotherSelection);
            MultiRectArea anotherFromThis = MultiRectArea.subtract(anotherSelection, this);
            MultiRectArea diff = MultiRectArea.union(thisFromAnother, anotherFromThis);
            if (diff.isEmpty()) {
                return null;
            }
            Rectangle diffBounds = diff.getBounds();
            return new Segment(diffBounds.x, diffBounds.x + diffBounds.width - 1);
        }

        public int getBeginIndex() {
            return getBounds().x;
        }

        public int getEndIndex() {
            Rectangle bounds = getBounds();
            return bounds.x + bounds.width - 1;
        }

        @Override
        public Object clone() {
            return new Selection(this);
        }
    }

    protected boolean leadAnchorNotificationEnabled = true;

    protected EventListenerList listenerList = new EventListenerList();

    private Selection selection = new Selection();

    private int anchorSelectionIndex = NOT_SET;

    private int leadSelectionIndex = NOT_SET;

    private Segment adjustingInterval;

    private int selectionMode = MULTIPLE_INTERVAL_SELECTION;

    private boolean valueIsAdjusting;

    public void setSelectionInterval(int intervalEnd1, int intervalEnd2) {
        if (!isValidInterval(intervalEnd1, intervalEnd2)) {
            return;
        }
        Selection oldSelection = (Selection) selection.clone();
        selection.clear();
        setSelectionAndUpdateLeadAnchor(intervalEnd1, intervalEnd2, oldSelection);
    }

    public void addSelectionInterval(int intervalEnd1, int intervalEnd2) {
        if (!isValidInterval(intervalEnd1, intervalEnd2)) {
            return;
        }
        Selection oldSelection = (Selection) selection.clone();
        if (selectionMode == SINGLE_SELECTION || selectionMode == SINGLE_INTERVAL_SELECTION) {
            selection.clear();
        }
        setSelectionAndUpdateLeadAnchor(intervalEnd1, intervalEnd2, oldSelection);
    }

    public void removeSelectionInterval(int intervalEnd1, int intervalEnd2) {
        if (!isValidInterval(intervalEnd1, intervalEnd2)) {
            return;
        }
        Segment interval = new Segment(intervalEnd1, intervalEnd2);
        Selection oldSelection = (Selection) selection.clone();
        selection.substract(interval);
        int oldAnchor = anchorSelectionIndex;
        int oldLead = leadSelectionIndex;
        anchorSelectionIndex = intervalEnd1;
        leadSelectionIndex = intervalEnd2;
        doNotification(selection.getDifferenceBounds(oldSelection), oldAnchor, oldLead);
    }

    public void clearSelection() {
        Selection oldSelection = (Selection) selection.clone();
        selection.clear();
        doNotification(selection.getDifferenceBounds(oldSelection), anchorSelectionIndex,
                leadSelectionIndex);
    }

    public boolean isSelectedIndex(int index) {
        return selection.contains(index);
    }

    public boolean isSelectionEmpty() {
        return selection.isEmpty();
    }

    public int getMaxSelectionIndex() {
        return isSelectionEmpty() ? NOT_SET : selection.getEndIndex();
    }

    public int getMinSelectionIndex() {
        return isSelectionEmpty() ? NOT_SET : selection.getBeginIndex();
    }

    public void insertIndexInterval(int index, int length, boolean before) {
        if (!isValidInterval(index, length)) {
            return;
        }
        Selection oldSelection = (Selection) selection.clone();
        int insertionIndex = before ? index : index + 1;
        selection.insertIndices(index, length, selectionMode != SINGLE_SELECTION);
        int oldAnchor = anchorSelectionIndex;
        int oldLead = leadSelectionIndex;
        if (anchorSelectionIndex >= insertionIndex) {
            anchorSelectionIndex += length;
        }
        if (leadSelectionIndex >= insertionIndex) {
            leadSelectionIndex += length;
        }
        doNotification(selection.getDifferenceBounds(oldSelection), oldAnchor, oldLead);
    }

    public void removeIndexInterval(int intervalEnd1, int intervalEnd2) {
        if (!isValidInterval(intervalEnd1, intervalEnd2)) {
            return;
        }
        Selection oldSelection = (Selection) selection.clone();
        Segment removalInterval = new Segment(intervalEnd1, intervalEnd2);
        selection.removeIndices(removalInterval.getBeginIndex(), removalInterval.getLength());
        int oldAnchor = anchorSelectionIndex;
        int oldLead = leadSelectionIndex;
        anchorSelectionIndex = adjustLeadAnchorIndexForIndicesRemoval(anchorSelectionIndex,
                removalInterval);
        leadSelectionIndex = adjustLeadAnchorIndexForIndicesRemoval(leadSelectionIndex,
                removalInterval);
        doNotification(selection.getDifferenceBounds(oldSelection), oldAnchor, oldLead);
    }

    public void setAnchorSelectionIndex(int anchorIndex) {
        int oldAnchor = anchorSelectionIndex;
        anchorSelectionIndex = anchorIndex;
        doNotification(null, oldAnchor, leadSelectionIndex);
    }

    public int getAnchorSelectionIndex() {
        return anchorSelectionIndex;
    }

    public void setLeadSelectionIndex(int leadIndex) {
        if (leadIndex < 0 && anchorSelectionIndex < 0) {
            leadSelectionIndex = leadIndex;
        }
        if (leadIndex < 0 || anchorSelectionIndex < 0) {
            return;
        }
        Selection oldSelection = (Selection) selection.clone();
        int oldLead = leadSelectionIndex;
        leadSelectionIndex = leadIndex;
        Segment oldSegment = new Segment(anchorSelectionIndex, oldLead);
        Segment newSegment = new Segment(anchorSelectionIndex, leadSelectionIndex);
        if (selection.contains(anchorSelectionIndex)) {
            selection.substract(oldSegment);
            selection.add(newSegment);
        } else {
            selection.add(oldSegment);
            selection.substract(newSegment);
        }
        doNotification(selection.getDifferenceBounds(oldSelection), anchorSelectionIndex,
                oldLead);
    }

    public void moveLeadSelectionIndex(int leadIndex) {
        if (leadIndex < 0 || leadSelectionIndex == leadIndex) {
            return;
        }
        int oldIndex = leadSelectionIndex;
        leadSelectionIndex = leadIndex;
        doNotification(null, anchorSelectionIndex, oldIndex);
    }

    public int getLeadSelectionIndex() {
        return leadSelectionIndex;
    }

    public void setLeadAnchorNotificationEnabled(boolean enabled) {
        leadAnchorNotificationEnabled = enabled;
    }

    public boolean isLeadAnchorNotificationEnabled() {
        return leadAnchorNotificationEnabled;
    }

    public int getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(int selectionMode) {
        if (selectionMode != SINGLE_SELECTION && selectionMode != SINGLE_INTERVAL_SELECTION
                && selectionMode != MULTIPLE_INTERVAL_SELECTION) {
            throw new IllegalArgumentException(Messages.getString("swing.08")); //$NON-NLS-1$
        }
        this.selectionMode = selectionMode;
    }

    public void setValueIsAdjusting(boolean isAdjusting) {
        valueIsAdjusting = isAdjusting;
        if (!isAdjusting) {
            fireValueChanged(isAdjusting);
        }
    }

    public boolean getValueIsAdjusting() {
        return valueIsAdjusting;
    }

    public void addListSelectionListener(ListSelectionListener l) {
        listenerList.add(ListSelectionListener.class, l);
    }

    public void removeListSelectionListener(ListSelectionListener l) {
        listenerList.remove(ListSelectionListener.class, l);
    }

    public ListSelectionListener[] getListSelectionListeners() {
        return getListeners(ListSelectionListener.class);
    }

    public <T extends java.util.EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        DefaultListSelectionModel result = new DefaultListSelectionModel();
        result.anchorSelectionIndex = anchorSelectionIndex;
        result.leadSelectionIndex = leadSelectionIndex;
        result.leadAnchorNotificationEnabled = leadAnchorNotificationEnabled;
        result.valueIsAdjusting = valueIsAdjusting;
        result.selectionMode = selectionMode;
        result.selection = (Selection) selection.clone();
        return result;
    }

    @Override
    public String toString() {
        return getClass().toString() + ": leadIndex=" + leadSelectionIndex + ", anchorIndex="
                + anchorSelectionIndex + ", isEmpty=" + isSelectionEmpty();
    }

    protected void fireValueChanged(boolean isAdjusting) {
        if (adjustingInterval != null) {
            fireValueChanged(adjustingInterval.getBeginIndex(),
                    adjustingInterval.getEndIndex(), isAdjusting);
            adjustingInterval = null;
        }
    }

    protected void fireValueChanged(int firstIndex, int lastIndex) {
        fireValueChanged(firstIndex, lastIndex, getValueIsAdjusting());
    }

    protected void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
        fireListSelectionEvent(new ListSelectionEvent(this, firstIndex, lastIndex, isAdjusting));
    }

    private void fireListSelectionEvent(ListSelectionEvent event) {
        ListSelectionListener[] listeners = getListSelectionListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].valueChanged(event);
        }
    }

    private void doNotification(Segment changedInterval, int oldAnchorIndex, int oldLeadIndex) {
        Segment fireInterval = changedInterval;
        if (leadAnchorNotificationEnabled) {
            Segment anchorLeadInterval = getLeadAnchorInterval(oldAnchorIndex, oldLeadIndex);
            fireInterval = mergeIntervals(fireInterval, anchorLeadInterval);
        }
        if (fireInterval == null) {
            return;
        }
        if (valueIsAdjusting) {
            adjustingInterval = mergeIntervals(adjustingInterval, fireInterval);
        }
        fireValueChanged(fireInterval.getBeginIndex(), fireInterval.getEndIndex());
    }

    private Segment mergeIntervals(Segment interval1, Segment interval2) {
        Segment result = interval1;
        if (result != null) {
            result.add(interval2);
        } else {
            result = interval2;
        }
        return result;
    }

    private Segment getLeadAnchorInterval(int oldAnchorIndex, int oldLeadIndex) {
        Segment anchorInterval = createInterval(oldAnchorIndex, anchorSelectionIndex);
        Segment leadInterval = createInterval(oldLeadIndex, leadSelectionIndex);
        return mergeIntervals(anchorInterval, leadInterval);
    }

    private Segment createInterval(int oldLeadAnchorIndex, int newLeadAnchorIndex) {
        if (oldLeadAnchorIndex == newLeadAnchorIndex) {
            return null;
        }
        if (oldLeadAnchorIndex == NOT_SET) {
            return new Segment(newLeadAnchorIndex, newLeadAnchorIndex);
        }
        if (newLeadAnchorIndex == NOT_SET) {
            return new Segment(oldLeadAnchorIndex, oldLeadAnchorIndex);
        }
        return new Segment(oldLeadAnchorIndex, newLeadAnchorIndex);
    }

    private int adjustLeadAnchorIndexForIndicesRemoval(int leadAnchorIndex,
            Segment removalInterval) {
        int result = leadAnchorIndex;
        if (result >= removalInterval.getBeginIndex()) {
            if (result < removalInterval.getEndIndex()) {
                result = removalInterval.getBeginIndex() - 1;
            } else {
                result -= removalInterval.getLength();
            }
        }
        return result;
    }

    private void setSelectionAndUpdateLeadAnchor(int intervalEnd1, int intervalEnd2,
            Selection oldSelection) {
        int oldAnchor = anchorSelectionIndex;
        int oldLead = leadSelectionIndex;
        if (selectionMode == SINGLE_SELECTION) {
            anchorSelectionIndex = intervalEnd2;
        } else {
            anchorSelectionIndex = intervalEnd1;
        }
        leadSelectionIndex = intervalEnd2;
        selection.add(new Segment(anchorSelectionIndex, leadSelectionIndex));
        doNotification(selection.getDifferenceBounds(oldSelection), oldAnchor, oldLead);
    }

    private boolean isValidInterval(int n1, int n2) {
        if (n1 == -1 || n2 == -1) {
            return false;
        }
        if (n1 < -1 || n2 < -1) {
            // According to the API specification
            throw new IndexOutOfBoundsException();
        }
        return true;
    }
}
