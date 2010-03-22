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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Position.Bias;

/**
 * This is utility class to manage child view creation.
 *
 */
public final class ViewTestHelpers {
    private ViewTestHelpers() {
    }

    /**
     * Default child view height.
     */
    private static final int HEIGHT = 16;

    /**
     * Default child view width.
     */
    private static final int WIDTH = 25;

    /**
     * Width multipliers to change the default width.
     */
    private static final int[] WIDTH_MULTIPLIER = new int[] { 1, 3, 4, 2 };

    /**
     * Height multipliers to change the default height.
     */
    private static final int[] HEIGHT_MULTIPLIER = new int[] { 2, 1, 3, 1 };

    /**
     * Alignments for children to change the default alignment (center = 0.5).
     */
    private static final float[] ALIGNMENT = new float[] { 0.0f, 0.25f, 1.0f, 0.5f };

    /**
     * One character width. It is used in model/view translations.
     */
    public static final int POS = 4;

    /**
     * Returns the width of the child. It uses multipliers to change widths of
     * children where applicable.
     *
     * @param id child number (index)
     * @return the child width
     */
    public static int getWidth(final int id) {
        return 0 <= id && id < WIDTH_MULTIPLIER.length ? WIDTH * WIDTH_MULTIPLIER[id] : WIDTH;
    }

    /**
     * Returns the height of the child. It uses multipliers to change heights
     * of children where applicable.
     *
     * @param id child number (index)
     * @return the child height
     */
    public static int getHeight(final int id) {
        return 0 <= id && id < HEIGHT_MULTIPLIER.length ? HEIGHT * HEIGHT_MULTIPLIER[id]
                : HEIGHT;
    }

    /**
     * Returns child desired alignment. It uses <code>ALIGNMENT</code> array
     * where possible.
     *
     * @param axis axis along which alignment is requested (current unused)
     * @param id child number (index)
     * @return the child alignment
     */
    public static float getAlign(final int axis, final int id) {
        return 0 <= id && id < ALIGNMENT.length ? ALIGNMENT[id] : 0.5f;
    }

    /**
     * Represents children of the main view. Methods are implemented so that
     * the behavior is predictable.
     */
    public static class ChildView extends View {
        private final int id;

        public ChildView(final Element element, final int ID) {
            super(element);
            id = ID;
        }

        @Override
        public float getPreferredSpan(final int axis) {
            return axis == X_AXIS ? getWidth(id) : getHeight(id);
        }

        public int getID() {
            return id;
        }

        @Override
        public Shape modelToView(final int pos, final Shape shape, final Bias bias)
                throws BadLocationException {
            Rectangle bounds = shape.getBounds();
            bounds.x += (pos - getStartOffset()) * POS;
            bounds.width = 1;
            bounds.height = (int) getPreferredSpan(Y_AXIS);
            return bounds;
        }

        @Override
        public void paint(final Graphics g, final Shape shape) {
        }

        @Override
        public int viewToModel(final float x, final float y, final Shape shape,
                final Bias[] biasReturn) {
            // We don't set biasReturn to a value
            return (getStartOffset() + (int) (x - shape.getBounds().x) / POS);
        }

        @Override
        public float getAlignment(final int axis) {
            return getAlign(axis, id);
        }

        @Override
        public void insertUpdate(final DocumentEvent event, final Shape shape,
                final ViewFactory factory) {
            preferenceChanged(this, true, true);
            super.insertUpdate(event, shape, factory);
        }
    }

    /**
     * Represents children of the main view when <code>flexibleChildren</code>
     * is set to <code>true</code>. The only difference between
     * <code>ChildView</code> and this class (<code>FlexibleChildView</code>)
     * is this class returns different values for its size preferences:
     * minimum span, maximum and preferred. Its <code>getResizeWeight</code>
     * also returns positive value, so that views are considered resizable.
     */
    public static class FlexibleChildView extends ChildView {
        public FlexibleChildView(final Element element, final int ID) {
            super(element, ID);
        }

        private static final float[] min = new float[] { 0.25f, 1.0f, 0.5f };

        private static final float[] max = new float[] { 5.0f, 4.0f, 15.0f };

        @Override
        public float getMaximumSpan(final int axis) {
            float result;
            if (getID() < max.length) {
                result = max[getID()] * (axis == X_AXIS ? WIDTH : HEIGHT);
            } else {
                result = super.getMaximumSpan(axis);
            }
            return result;
        }

        @Override
        public float getMinimumSpan(final int axis) {
            float result;
            if (getID() < min.length) {
                result = min[getID()] * (axis == X_AXIS ? WIDTH : HEIGHT);
            } else {
                result = super.getMinimumSpan(axis);
            }
            return result;
        }

        @Override
        public int getResizeWeight(final int axis) {
            int id = getID();
            if (id < min.length) {
                int result = (int) ((axis == X_AXIS ? WIDTH : HEIGHT) * (max[id] - min[id]));
                return result;
            }
            return super.getResizeWeight(axis);
        }
    }

    /**
     * Represents a child view which responsible for rendering of a portion
     * of the element but not the entire element.
     */
    public static class ElementPartView extends ChildView {
        private final Position start;

        private final Position end;

        public ElementPartView(final Element element, final int startOffset, final int endOffset) {
            super(element, -1);
            start = createPosition(startOffset);
            end = createPosition(endOffset);
        }

        @Override
        public int getStartOffset() {
            return start.getOffset();
        }

        @Override
        public int getEndOffset() {
            return end.getOffset();
        }

        private Position createPosition(final int offset) {
            Position result = null;
            try {
                result = getDocument().createPosition(offset);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    /**
     * Represents view factory to create children
     *  of type <code>ChildView</code>.
     */
    public static class ChildrenFactory implements ViewFactory {
        /**
         * Determins the ID of the next child created.
         */
        private int count = 0;

        /**
         * Determins which type of children this factory returns.
         * By default it returns ordinary children.
         */
        private boolean flexibleChildren = false;

        /**
         * Creates child views. The type of
         * the returned view depends on the state of
         *  <code>flexibleChildren</code> flag.
         */
        public View create(final Element element) {
            if (flexibleChildren) {
                return new FlexibleChildView(element, count++);
            }
            return new ChildView(element, count++);
        }

        /**
         * Resets ID counter. The next view created will get id == 0.
         */
        public void resetID() {
            count = 0;
        }

        /**
         * Sets <code>flexibleChildren</code> to <code>true</code> and
         * resets the ID counter.
         */
        public void makeFlexible() {
            flexibleChildren = true;
            resetID();
        }
    }

    private static final String text = "javax.swing.text.";

    private static final String plaf = "javax.swing.plaf.basic.";

    public static String getViewShortClassName(final View view) {
        String result = view.getClass().getName();
        if (result.startsWith(text)) {
            result = result.substring(text.length());
        } else if (result.startsWith(plaf)) {
            result = result.substring(plaf.length());
        }
        return result;
    }
}
