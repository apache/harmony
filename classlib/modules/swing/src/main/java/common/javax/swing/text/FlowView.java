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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Position.Bias;

import org.apache.harmony.x.swing.internal.nls.Messages;

public abstract class FlowView extends BoxView {
    public static class FlowStrategy {
        private static FlowStrategy sharedStrategy;

        public void insertUpdate(final FlowView fv,
                                 final DocumentEvent event,
                                 final Rectangle alloc) {
            invalidateFlow(fv, event, alloc);
        }

        public void removeUpdate(final FlowView fv,
                                 final DocumentEvent event,
                                 final Rectangle alloc) {
            invalidateFlow(fv, event, alloc);
        }

        public void changedUpdate(final FlowView fv,
                                  final DocumentEvent event,
                                  final Rectangle alloc) {
            invalidateFlow(fv, event, alloc);
        }

        public void layout(final FlowView fv) {
            final View lv = getLogicalView(fv);
            int offset = lv.getStartOffset();
            final int endOffset = lv.getEndOffset();

            fv.removeAll();

            int rowIndex = 0;
            do {
                View row = fv.createRow();
                fv.append(row);
                offset = layoutRow(fv, rowIndex++, offset);
            } while (offset < endOffset);
        }

        protected void adjustRow(final FlowView fv,
                                 final int rowIndex,
                                 final int desiredSpan,
                                 final int x) {
            View row = fv.getView(rowIndex);
            int maxWeight = BadBreakWeight;
            int index = -1;
            View toBreak = null;
            int breakX = -1;
            int breakSpan = -1;
            int span = 0;
            for (int i = 0; i < row.getViewCount(); i++) {
                View view = row.getView(i);
                int viewSpan = (int)view.getPreferredSpan(fv.getFlowAxis());
                int weight = view.getBreakWeight(fv.getFlowAxis(),
                                                 x + span, desiredSpan - span);
                if (weight >= maxWeight) {
                    maxWeight = weight;
                    index = i;
                    toBreak = view;
                    breakX = x + span;
                    breakSpan = desiredSpan - span;
                }
                span += viewSpan;
            }
            if (index == -1) {
                throw new Error(Messages.getString("swing.err.14")); //$NON-NLS-1$
            }

            View broken = toBreak.breakView(fv.getFlowAxis(),
                                            toBreak.getStartOffset(),
                                            breakX, breakSpan);
            // Set parent of views to be removed from the row back to
            // logical view
            final View logicalView = getLogicalView(fv);
            for (int i = index; i < row.getViewCount(); i++) {
                row.getView(i).setParent(logicalView);
            }
            row.replace(index, row.getViewCount() - index, new View[] {broken});
        }

        protected int layoutRow(final FlowView fv,
                                final int rowIndex,
                                final int pos) {
            final View row = fv.getView(rowIndex);
            final int flowAxis = fv.getFlowAxis();
            final int flowStart = fv.getFlowStart(rowIndex);
            final int flowSpan = fv.getFlowSpan(rowIndex);
            int x = flowStart;
            int rowSpan = flowSpan;
            int span = 0;
            int offset = pos;
            int weight = BadBreakWeight;
            int fix=0;
            View view;

            do {
                rowSpan -= span;
                x += span;

                view = createView(fv, offset, rowSpan, rowIndex);
                if (view != null) {
                    span = (int)view.getPreferredSpan(flowAxis);
                    weight = view.getBreakWeight(flowAxis, offset, rowSpan);
                    if (weight >= ForcedBreakWeight) {
                        final View broken = view.breakView(flowAxis, offset, x, rowSpan);
                        if (view == broken && row.getViewCount() > 0) {
                            fix=1;
                            break;
                        }
                        view = broken;
                    }
                    row.append(view);
                    offset = view.getEndOffset();
                }
            } while (view != null && span <= rowSpan
                     && weight < ForcedBreakWeight);

            if (span > rowSpan) {
                adjustRow(fv, rowIndex, flowSpan, flowStart);
            }

            return row.getEndOffset()+fix;
        }

        protected View getLogicalView(final FlowView fv) {
            return fv.layoutPool;
        }

        protected View createView(final FlowView fv,
                                  final int startOffset,
                                  final int spanLeft,
                                  final int rowIndex) {
            View logical = getLogicalView(fv);
            int index = logical.getViewIndex(startOffset, Bias.Forward);
            if (index == -1) {
                return null;
            }
            View result = logical.getView(index);
            if (startOffset != result.getStartOffset()) {
                return result.createFragment(startOffset,
                                             result.getEndOffset());
            }
            return result;
        }

        static FlowStrategy getSharedStrategy() {
            if (sharedStrategy == null) {
                sharedStrategy = new FlowStrategy();
            }

            return sharedStrategy;
        }

        private void invalidateFlow(final FlowView fv,
                                    final DocumentEvent event,
                                    final Rectangle alloc) {
            if (event == null) {
                fv.layoutChanged(X_AXIS);
                fv.layoutChanged(Y_AXIS);
            }

            if (alloc != null) {
                Component container = fv.getComponent();
                if (container != null) {
                    container.repaint(alloc.x, alloc.y,
                                      alloc.width, alloc.height);
                }
            }
        }

    }

    private static class LogicalView extends CompositeView {
        private float spanX = -1;
        private float spanY = -1;

        public LogicalView(final Element element) {
            super(element);
        }

        public int getResizeWeight(final int axis) {
            return 1;
        }

        public float getMinimumSpan(final int axis) {
            return 0;
        }

        public float getPreferredSpan(final int axis) {
            if (axis == X_AXIS) {
                if (spanX == -1) {
                    spanX = getSpanX();
                }
                return spanX;
            }

            if (spanY == -1) {
                spanY = getSpanY();
            }
            return spanY;
        }

        public float getMaximumSpan(final int axis) {
            return getPreferredSpan(axis);
        }

        public void paint(final Graphics g, final Shape shape) {
            throw new UnsupportedOperationException(Messages.getString("swing.8B")); //$NON-NLS-1$
        }

        public void preferenceChanged(final View child,
                                      final boolean width,
                                      final boolean height) {
            if (width) {
                spanX = -1;
            }
            if (height) {
                spanY = -1;
            }
            super.preferenceChanged(child, width, height);
        }

        public AttributeSet getAttributes() {
            final View parent = getParent();
            return parent != null ? parent.getAttributes() : null;
        }
        
        protected void loadChildren(final ViewFactory factory) {
            if (factory != null) {
                super.loadChildren(factory);
            }
        }

        protected void forwardUpdateToView(final View view,
                                           final DocumentEvent event,
                                           final Shape shape,
                                           final ViewFactory factory) {
            view.setParent(this);
            super.forwardUpdateToView(view, event, shape, factory);
        }

        protected void childAllocation(final int index, final Rectangle rc) {
        }

        protected View getViewAtPoint(final int x, final int y,
                                      final Rectangle alloc) {
            throw new UnsupportedOperationException(Messages.getString("swing.8B")); //$NON-NLS-1$
        }

        protected int getViewIndexAtPosition(final int pos) {
            if (pos < getStartOffset() || pos >= getEndOffset()) {
                return -1;
            }
            return super.getViewIndexAtPosition(pos);
        }

        protected boolean isAfter(final int x, final int y,
                                  final Rectangle rc) {
            throw new UnsupportedOperationException(Messages.getString("swing.8B")); //$NON-NLS-1$
        }

        protected boolean isBefore(final int x, final int y,
                                   final Rectangle rc) {
            throw new UnsupportedOperationException(Messages.getString("swing.8B")); //$NON-NLS-1$
        }

        private float getSpanX() {
            float span = 0;
            for (int i = 0; i < getViewCount(); i++) {
                span += getView(i).getPreferredSpan(X_AXIS);
            }
            return span;
        }

        private float getSpanY() {
            float span = 0;
            for (int i = 0; i < getViewCount(); i++) {
                span = Math.max(span, getView(i).getPreferredSpan(Y_AXIS));
            }
            return span;
        }
    }

    protected View layoutPool;
    protected int layoutSpan = Short.MAX_VALUE;

    protected FlowStrategy strategy = FlowStrategy.getSharedStrategy();

    public FlowView(final Element element, final int axis) {
        super(element, axis);
    }

    protected abstract View createRow();

    public int getFlowAxis() {
        return getAxis() == Y_AXIS ? X_AXIS : Y_AXIS;
    }

    public int getFlowStart(final int rowIndex) {
        return 0;
    }

    public int getFlowSpan(final int rowIndex) {
        return layoutSpan;
    }

    public void insertUpdate(final DocumentEvent event,
                             final Shape alloc, final ViewFactory factory) {
        layoutPool.insertUpdate(event, alloc, factory);
        strategy.insertUpdate(this, event, shapeToRect(alloc));
    }

    public void removeUpdate(final DocumentEvent event,
                             final Shape alloc, final ViewFactory factory) {
        layoutPool.removeUpdate(event, alloc, factory);
        strategy.removeUpdate(this, event, shapeToRect(alloc));
    }

    public void changedUpdate(final DocumentEvent event,
                              final Shape alloc, final ViewFactory factory) {
        layoutPool.changedUpdate(event, alloc, factory);
        strategy.changedUpdate(this, event, shapeToRect(alloc));
    }

    protected SizeRequirements
        calculateMinorAxisRequirements(final int axis,
                                       final SizeRequirements sr) {
        SizeRequirements result = sr != null ? sr : new SizeRequirements();
        result.minimum = (int)layoutPool.getMinimumSpan(axis);
        result.preferred = (int)layoutPool.getPreferredSpan(axis);
        result.maximum = Integer.MAX_VALUE;
        result.alignment = ALIGN_CENTER;
        return result;
    }

    protected int getViewIndexAtPosition(final int pos) {
        if (pos < getStartOffset() || pos >= getEndOffset()) {
            return -1;
        }
        return super.getViewIndexAtPosition(pos);
    }

    protected void layout(final int width, final int height) {
        int span = getFlowAxis() == X_AXIS ? width : height;
        if (span != layoutSpan) {
            layoutSpan = span;
            layoutChanged(X_AXIS);
            layoutChanged(Y_AXIS);
        }

        if (!isAllocationValid()) {
            int h = (int)getPreferredSpan(Y_AXIS);
            strategy.layout(this);
            if (h != (int)getPreferredSpan(Y_AXIS)) {
                preferenceChanged(null, false, true);
            }
        }

        super.layout(width, height);
    }

    protected void loadChildren(final ViewFactory factory) {
        if (layoutPool == null) {
            createLogicalView();
            strategy.insertUpdate(this, null, null);
        } else if (layoutPool.getViewCount() == 0) {
            ((CompositeView)layoutPool).loadChildren(getViewFactory());
            strategy.insertUpdate(this, null, null);
        }
    }

    private void createLogicalView() {
        if (layoutPool == null) {
            layoutPool = new LogicalView(getElement());
            layoutPool.setParent(this);
        }
    }

    private Rectangle shapeToRect(final Shape alloc) {
        return alloc != null ? alloc.getBounds() : null;
    }
}
