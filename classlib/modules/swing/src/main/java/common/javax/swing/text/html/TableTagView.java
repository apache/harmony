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

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.BitSet;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.text.AttributeSet;
import javax.swing.text.BoxView;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.CSS.FloatValue;

import org.apache.harmony.x.swing.Utilities;

class TableTagView extends BlockView implements ViewFactory {
    private int[] columnWidths = new int[5];
    private AttributeSet cachedAttributes;
    private boolean areColumnSizeRequirementsValid;
    private SizeRequirements[] columnSizeRequirements = new SizeRequirements[5];

    private class TableRowView extends BlockView {
        private BitSet isColumnOccupied = new BitSet(16);

        public TableRowView(final Element elem) {
            super(elem, X_AXIS);
        }

        protected SizeRequirements calculateMajorAxisRequirements(final int axis,
                final SizeRequirements r) {
            SizeRequirements size = r == null ? new SizeRequirements() : r;
            SizeRequirements[] widths = calculateColumnSizeRequirements();
            size.minimum = 0;
            size.preferred = 0;
            for (int i = 0; i < getColumnCount(); i++) {
                size.minimum += widths[i].minimum;
                size.preferred += widths[i].preferred;
                size.maximum = Utilities.safeIntSum(size.maximum, widths[i].maximum);
            }
            return size;
        }

        protected SizeRequirements calculateMinorAxisRequirements(
                int axis, SizeRequirements r) {
            SizeRequirements size = super.calculateMinorAxisRequirements(axis, r);
            size.maximum = size.preferred;
            return size;
        }

        protected void layoutMajorAxis(final int targetSpan, final int axis,
                                       final int[] offsets, final int[] spans) {
            int[] widths = getColumnWidths();

            int currentOffset = 0;
            int widthsIndex = 0;
            for (int i = 0; i < spans.length; i++) {
                // take row spans of cells on previous rows into account
                for (int j = 0; isColumnOccupied.get(widthsIndex + j); j++) {
                    currentOffset += widths[widthsIndex + j];
                    widthsIndex++;
                }

                offsets[i] = currentOffset;
                int colSpan = getColumnSpan(i);
                spans[i] = 0;
                for (int j = 0; j < colSpan; j++) {
                    spans[i] += widths[widthsIndex + j];
                }
                widthsIndex += colSpan;
                currentOffset += spans[i];
            }
        }

        protected void layoutMinorAxis(int targetSpan, int axis,
                                       int[] offsets, int[] spans) {
            for (int viewIndex = 0; viewIndex < getViewCount(); viewIndex++) {
                int rowSpan = getRowSpan(getView(viewIndex));
                offsets[viewIndex] = 0;
                if (rowSpan == 1) {
                    spans[viewIndex] = (int)getView(viewIndex).getPreferredSpan(axis);
                } else {
                    spans[viewIndex] = targetSpan;
                }
            }
        }

        public int getColumnSpan(final int viewIndex) {
            String strSpan = (String)getView(viewIndex).getElement().getAttributes()
                .getAttribute(HTML.Attribute.COLSPAN);
            return strSpan == null ? 1 : Integer.parseInt(strSpan);
        }

        public int getMaxRowSpan() {
            int maxRowSpan = 1;
            for (int viewIndex = 0; viewIndex < getViewCount(); viewIndex++) {
                maxRowSpan = Math.max(maxRowSpan, getRowSpan(getView(viewIndex)));
            }

            return maxRowSpan;
        }

        private int getRowSpan(final View v) {
            String strSpan = (String)v.getElement().getAttributes()
                .getAttribute(HTML.Attribute.ROWSPAN);
            return strSpan == null ? 1 : Integer.parseInt(strSpan);
        }

        public int getColumnCount() {
            int count = 0;
            for (int i = 0; i < getViewCount(); i++) {
                count += getColumnSpan(i);
            }
            count += isColumnOccupied.cardinality();
            return count;
        }

        public void setColumnOccupied(final int column) {
            isColumnOccupied.set(column);
        }

        public boolean isColumnOccupied(final int column) {
            return isColumnOccupied.get(column);
        }

        public int[] updateRowMarkup(final int[] prevRowRowSpans) {
            isColumnOccupied.clear();

            int col = 0;
            int[] spans = prevRowRowSpans;
            for (int viewIndex = 0; viewIndex < getViewCount(); viewIndex++) {
                spans = enshureCapacity(spans, col + 1);
                boolean occupied = spans[col] > 0;
                if (occupied) {
                    setColumnOccupied(col);
                    spans[col]--;
                } else {
                    int rowSpan = getRowSpan(getView(viewIndex));
                    int colSpan = getColumnSpan(viewIndex);
                    enshureCapacity(spans, col + colSpan);
                    for (; colSpan > 0; colSpan--) {
                        if (rowSpan > 1) {
                            spans[col] = rowSpan - 1;
                        }
                    }
                }

                col++;
            }

            return spans;
        }

        private int[] enshureCapacity(final int[] arr, final int len) {
            if (len > arr.length) {
                int[] newArr = new int[len + 16];
                System.arraycopy(arr, 0, newArr, 0, arr.length);
                return newArr;
            }
            return arr;
        }
    }

    private class TableCellView extends BlockView {
        private AttributeSet attrs;

        public TableCellView(final Element elem) {
            super(elem, Y_AXIS);
        }

        public AttributeSet getAttributes() {
            if (attrs == null) {
                attrs = new CompositeAttributeSet(
                    super.getAttributes(), getAdditionalCellAttrs());
            }
            return attrs;
        }

        public void changedUpdate(final DocumentEvent e, final Shape s,
                                  final ViewFactory f) {
            attrs = null;
            super.changedUpdate(e, s, f);
        }
    }

    // THEAD, TFOOT, TBODY are not supported
    private static class RowViewIterator {
        private View currentParent;
        private TableRowView currentRowView;
        private int currentIndex;

        public RowViewIterator(final View root) {
            currentParent = root;
            next();
        }

        public boolean isValid() {
            return currentRowView != null;
        }

         public void next() {
            while (currentIndex < currentParent.getViewCount()) {
                if (currentParent.getView(currentIndex) instanceof TableRowView) {
                    currentRowView = (TableRowView)currentParent.getView(currentIndex);
                    currentIndex++;
                    return;
                }
                currentIndex++;
            }

            if (currentIndex >= currentParent.getViewCount()) {
                currentRowView = null;
            }
        }

        public TableRowView getView() {
            return currentRowView;
        }
    }

    public TableTagView(final Element elem) {
        super(elem, Y_AXIS);
    }

    public View create(final Element elem) {
        HTML.Tag tag = HTMLEditorKit.getHTMLTagByElement(elem);

        if (HTML.Tag.TR.equals(tag)) {
            return createTableRow(elem);
        } else if (HTML.Tag.TD.equals(tag) || HTML.Tag.TH.equals(tag)) {
            return createTableCell(elem);
        } else if (HTML.Tag.CAPTION.equals(tag)) {
            return new BlockView(elem, BlockView.Y_AXIS);
        } else if (HTML.Tag.COLGROUP.equals(tag)) {
            return new InvisibleTagView(elem);
        } else if (HTML.Tag.COL.equals(tag)) {
            return new InvisibleTagView(elem);
        }

        return super.getViewFactory().create(elem);
    }

    public void changedUpdate(final DocumentEvent e, final Shape s,
                              final ViewFactory f) {
        cachedAttributes = null;
        super.changedUpdate(e, s, f);
    }

    public AttributeSet getAttributes() {
        if (cachedAttributes == null) {
            cachedAttributes = new CompositeAttributeSet(
                super.getAttributes(), getAdditionalTableAttrs());
        }

        return cachedAttributes;
    }

    public ViewFactory getViewFactory() {
        return this;
    }

    public void preferenceChanged(View child, boolean width, boolean height) {
        areColumnSizeRequirementsValid = false;

        super.preferenceChanged(this, width, height);

        for (int i = 0; i < getViewCount(); i++) {
            if (getView(i) instanceof BoxView) {
                ((BoxView)getView(i)).layoutChanged(X_AXIS);
            }
        }
    }

    protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
        super.layoutMajorAxis(targetSpan, axis, offsets, spans);
        int row = 0;

        for (RowViewIterator rowIt = new RowViewIterator(this); rowIt.isValid();
                rowIt.next()) {
            int maxRowSpan = rowIt.getView().getMaxRowSpan();
            maxRowSpan = Math.min(maxRowSpan - 1, spans.length - row);
            for (; maxRowSpan > 0; maxRowSpan--) {
                spans[row] += spans[row + maxRowSpan];
            }

            row++;
        }
    }

    protected void forwardUpdate(ElementChange change, DocumentEvent event, Shape shape, ViewFactory factory) {
        boolean xValid = isLayoutValid(X_AXIS);

        super.forwardUpdate(change, event, shape, factory);

        if (xValid && !isLayoutValid(X_AXIS)) {
            Rectangle rc = shape.getBounds();
            getContainer().repaint(rc.x, rc.y, ((BoxView)getParent()).getWidth(), rc.height);
        }
    }

    protected TableRowView createTableRow(final Element elem) {
        return new TableRowView(elem);
    }

    protected TableCellView createTableCell(final Element elem) {
        return new TableCellView(elem);
    }

    protected SizeRequirements calculateMajorAxisRequirements(
            final int axis, final SizeRequirements r) {
        SizeRequirements size = super.calculateMajorAxisRequirements(axis, r);
        size.maximum = size.preferred;
        return size;
    }

    protected SizeRequirements calculateMinorAxisRequirements(
            final int axis, final SizeRequirements r) {
        areColumnSizeRequirementsValid = isLayoutValid(X_AXIS);
        SizeRequirements size = super.calculateMinorAxisRequirements(axis, r);
        size.maximum = size.preferred;
        return size;
    }

    int getCaptionHeight() {
        View captionView = getView(0);
        if (captionView != null && HTML.Tag.CAPTION.equals
                (HTMLEditorKit.getHTMLTagByElement(captionView.getElement()))) {
            return (int)captionView.getPreferredSpan(Y_AXIS);
        }
        return 0;
    }

    private int getColumnCount() {
        if (!areColumnSizeRequirementsValid) {
            updateRowViewColumnOccupied();
        }

        int count = 0;
        for (RowViewIterator rowIt = new RowViewIterator(this);
                rowIt.isValid(); rowIt.next()) {
            count = Math.max(count, rowIt.getView().getColumnCount());
        }
        return count;
    }

    private void updateRowViewColumnOccupied() {
        int[] rowSpans = new int[1];

        for (RowViewIterator rowIt = new RowViewIterator(this); rowIt.isValid();
                rowIt.next()) {
            rowSpans = rowIt.getView().updateRowMarkup(rowSpans);
        }
    }

    /*
     * This function is based on the autolayout algorithm described here:
     * http://www.w3.org/TR/html4/appendix/notes.html#h-B.5.2
     */
    private int[] getColumnWidths() {
//        if (isLayoutValid(X_AXIS)) {
//            return columnWidths;
//        }

        int columnCount = getColumnCount();
        if (columnCount > columnWidths.length) {
            columnWidths = new int[columnCount];
        }

        SizeRequirements[] sizes = calculateColumnSizeRequirements();
        int minTableWidth = 0;
        int maxTableWidth = 0;
        for (int col = 0; col < getColumnCount(); col++) {
            columnWidths[col] = sizes[col].minimum;
            minTableWidth += columnWidths[col];
            maxTableWidth += sizes[col].maximum;
        }

        int delta = maxTableWidth - minTableWidth;

        if (delta > 0) {
            for (int col = 0; col < getColumnCount(); col++) {
                columnWidths[col] += (double)(sizes[col].maximum - columnWidths[col])
                    / (double)delta
                    * (double)(getWidth() - minTableWidth);
            }
        }

        return columnWidths;
    }

    private SizeRequirements[] calculateColumnSizeRequirements() {
        if (areColumnSizeRequirementsValid) {
            return columnSizeRequirements;
        }

        int columnCount = getColumnCount();
        if (columnSizeRequirements.length < columnCount) {
            SizeRequirements[] oldSizeReqs = columnSizeRequirements;
            columnSizeRequirements = new SizeRequirements[columnCount];
            System.arraycopy(oldSizeReqs, 0, columnSizeRequirements, 0,
                             oldSizeReqs.length);
        }

        for (int col = 0; col < columnCount; col++) {
            if (columnSizeRequirements[col] == null) {
                columnSizeRequirements[col] = new SizeRequirements();
            } else {
                columnSizeRequirements[col].minimum = 0;
                columnSizeRequirements[col].maximum = 0;
                columnSizeRequirements[col].preferred = 0;
            }
        }

        for (RowViewIterator rowIt = new RowViewIterator(this); rowIt.isValid(); rowIt.next()) {
            int spansCount = 0;
            TableRowView rowView = rowIt.getView();
            for (int col = 0; col < rowView.getViewCount(); col++) {
                while (rowView.isColumnOccupied(col + spansCount)) {
                    spansCount++;
                }
                int colSpan = rowView.getColumnSpan(col);
                for (int i = 0; i < colSpan; i++) {
                    joinSizeRequirements(columnSizeRequirements[col + spansCount + i],
                        (int)rowView.getView(col).getMinimumSpan(X_AXIS) / colSpan,
                        (int)rowView.getView(col).getPreferredSpan(X_AXIS) / colSpan,
                        (int)rowView.getView(col).getMaximumSpan(X_AXIS) / colSpan);
                }
                spansCount += colSpan - 1;
            }
        }

        areColumnSizeRequirementsValid = true;
        return columnSizeRequirements;
    }

    private void joinSizeRequirements(final SizeRequirements size, final int min,
                                      final int pref, final int max) {
        size.minimum = Math.max(size.minimum, min);
        size.preferred = Math.max(size.preferred, pref);

        if (size.maximum > 0) {
            size.maximum = Math.min(size.maximum, max);
        } else {
            size.maximum = Math.max(size.maximum, max);
        }
        // getColumnWidths() fails without this
        size.maximum = Math.min(size.maximum, Short.MAX_VALUE);
    }

    private Object getCellSpacingAttr() {
        Object cellSpacing = getElement().getAttributes()
            .getAttribute(HTML.Attribute.CELLSPACING);
        if (cellSpacing == null) {
            cellSpacing = "1";
        }
        return FloatValue.factory.toCSS(Float.valueOf((String)cellSpacing));
    }

    private AttributeSet getAdditionalCellAttrs() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();

        Object v = getCellSpacingAttr();
        if (v != null) {
            attrs.addAttribute(CSS.Attribute.MARGIN_LEFT, v);
            attrs.addAttribute(CSS.Attribute.MARGIN_TOP, v);
        }

        v = getElement().getAttributes().getAttribute(HTML.Attribute.BORDER);
        if (v != null) {
            getStyleSheet().addCSSAttribute(attrs, CSS.Attribute.BORDER, "inset 1px");
        }
        return attrs;
    }

    private AttributeSet getAdditionalTableAttrs() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();

        Object v = getCellSpacingAttr();
        if (v != null) {
            attrs.addAttribute(CSS.Attribute.PADDING_RIGHT, v);
            attrs.addAttribute(CSS.Attribute.PADDING_BOTTOM, v);
        }

        v = getElement().getAttributes().getAttribute(HTML.Attribute.BORDER);
        if (v instanceof String) {
            String strValue = (String)v;
            getStyleSheet().addCSSAttribute(attrs, CSS.Attribute.BORDER,
                                            "inset " + strValue + "pt");
        }
        return attrs;
    }
}
