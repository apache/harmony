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

import org.apache.harmony.x.swing.Utilities;

/**
 * Represents a basic interval of text. There are three non-abstract
 * implementations:
 * <ol>
 *     <li><code>UnselectedTextInterval</code>,</li>
 *     <li><code>SelectedTextInterval</code>,</li>
 *     <li><code>ComposedTextInterval</code>.</li>
 * </ol>
 */
abstract class TextInterval implements Cloneable {

    /**
     * Defines how text is actually painted.
     */
    interface TextIntervalPainter {
        /**
         * Paints selected range of text.
         *
         * @param g graphics to paint on.
         * @param start the start of the selected range.
         * @param end the end of the selected range.
         * @param x the <var>x</var> coordinate of the text.
         * @param y the <var>y</var> coordinate of the text.
         *          It assumed to be the baseline position.
         * @return the <var>x</var> coordinate where the next interval is
         *         to be painted.
         *
         * @throws BadLocationException if interval represents invalid
         *                              model range.
         */
        int paintSelected(Graphics g, int start, int end, int x, int y)
            throws BadLocationException;

        /**
         * Paints unselected range of text.
         *
         * @param g graphics to paint on.
         * @param start the start of the unselected range.
         * @param end the end of the unselected range.
         * @param x the <var>x</var> coordinate of the text.
         * @param y the <var>y</var> coordinate of the text.
         *          It assumed to be the baseline position.
         * @return the <var>x</var> coordinate where the next interval is
         *         to be painted.
         *
         * @throws BadLocationException if interval represents invalid
         *                              model range.
         */
        int paintUnselected(Graphics g, int start, int end, int x, int y)
            throws BadLocationException;

        /**
         * Paints composed text.
         *
         * @param g graphics to paint on.
         * @param start the start of the composed text.
         * @param end the end of the composed text.
         * @param x the <var>x</var> coordinate of the text.
         * @param y the <var>y</var> coordinate of the text.
         *          It assumed to be the baseline position.
         * @return the <var>x</var> coordinate where the next interval is
         *         to be painted.
         *
         * @throws BadLocationException if interval represents invalid
         *                              model range.
         */
        int paintComposed(Graphics g, int start, int end, int x, int y)
            throws BadLocationException;
    }

    /**
     * Start of the interval.
     */
    protected int start;
    /**
     * End of the interval.
     */
    protected int end;
    /**
     * Painter to do actual text painting.
     */
    protected final TextIntervalPainter painter;

    /**
     * Creates a text interval.
     *
     * @param start the start of the interval.
     * @param end the end of the interval.
     * @param painter the painter to use.
     */
    public TextInterval(final int start, final int end,
                        final TextIntervalPainter painter) {
        this.start = start;
        this.end = end;
        this.painter = painter;
    }

    /**
     * Returns dissection of this interval and <code>another</code>.
     *
     * @param another the interval to dissect with.
     * @return pairwise disjoint intervals.
     */
    public abstract TextInterval[] dissect(TextInterval another);

    /**
     * Returns the string type of the interval.
     * It is used in <code>toString</code> method.
     *
     * @return the string type of the interval.
     */
    public abstract String getType();

    /**
     * Checks whether this interval is empty.
     *
     * @return <code>true</code> if the interval start is equal to its end;
     *         <code>false</code> otherwise.
     */
    public final boolean isEmpty() {
        return start == end;
    }

    /**
     * Checks whether two intervals intersect.
     *
     * @param another the interval to check intersection with.
     * @return <code>true</code> if this interval intersects
     *         with <code>another</code>; <code>false</code> otherwise.
     */
    public final boolean intersects(final TextInterval another) {
        return Utilities.range(start, another.start, another.end)
               != Utilities.range(end, another.start, another.end);
    }

    /**
     * Paints text which falls in this interval.
     *
     * @param g graphics to paint on.
     * @param x the <var>x</var> coordinate.
     * @param y the <var>y</var> coordinate.
     *        It assumed to be the baseline of text.
     * @return the <var>x</var> coordinate where the next text interval should
     *         be painted.
     * @throws BadLocationException if interval represents invalid model range.
     */
    public abstract int paint(final Graphics g, final int x, final int y)
        throws BadLocationException;

    /**
     * Creates a copy of the interval with new start and end.
     *
     * @param start the new start of the interval.
     * @param end the new end of the interval.
     * @return the interval with the new start and end.
     */
    public final TextInterval create(final int start, final int end) {
        TextInterval result;
        try {
            result = (TextInterval)clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
        result.start = start;
        result.end = end;
        return result;
    }

    /**
     * Converts interval to a string.
     * @return the string representation of the interval.
     */
    public String toString() {
        return getType() + "[" + start + ", " + end + "]";
    }

    /**
     * Dissects <em>selected</em> and <em>composed</em> text intervals.
     * <p><i>This method is meant for internal usage only.</i>
     *
     * @param selected the interval with selected text.
     * @param composed the interval with composed text.
     * @return pairwise disjoint intervals.
     */
    static TextInterval[] dissect(final SelectedTextInterval selected,
                                  final ComposedTextInterval composed) {
        if (selected.isEmpty()) {
            return new TextInterval[] {};
        }
        if (composed.isEmpty()) {
            return new TextInterval[] {selected};
        }
        if (!selected.intersects(composed)) {
            return new TextInterval[] {selected};
        }

        if (composed.start > selected.start) {
            if (composed.end >= selected.end) {
                return new TextInterval[] {
                    selected.create(selected.start, composed.start),
                    composed
                };
            } else {
                return new TextInterval[] {
                    selected.create(selected.start, composed.start),
                    composed,
                    selected.create(composed.end, selected.end),
                };
            }
        } else {
            if (composed.end >= selected.end) {
                return new TextInterval[] {composed};
            } else {
                return new TextInterval[] {
                    composed,
                    selected.create(composed.end, selected.end)
                };
            }
        }
    }

    /**
     * Dissects <em>unselected</em> and <em>selected or composed</em>
     * text intervals.
     * <p><i>This method is meant for internal usage only.</i>
     *
     * @param ordinary the interval with unselected text.
     * @param selectedOrComposed the interval with selected or composed text.
     * @return pairwise disjoint intervals.
     */
    static TextInterval[] dissect(final UnselectedTextInterval ordinary,
                                  final TextInterval selectedOrComposed) {
        if (selectedOrComposed.isEmpty()) {
            return new TextInterval[] {ordinary};
        }
        if (!ordinary.intersects(selectedOrComposed)) {
            return new TextInterval[] {ordinary};
        }

        if (ordinary.start >= selectedOrComposed.start) {
            if (ordinary.end > selectedOrComposed.end) {
                return new TextInterval[] {
                    selectedOrComposed.create(ordinary.start,
                                              selectedOrComposed.end),
                    ordinary.create(selectedOrComposed.end, ordinary.end)
                };
            } else {
                return new TextInterval[] {
                    selectedOrComposed.create(ordinary.start, ordinary.end)
                };
            }
        } else {
            if (ordinary.end > selectedOrComposed.end) {
                return new TextInterval[] {
                    ordinary.create(ordinary.start, selectedOrComposed.start),
                    selectedOrComposed,
                    ordinary.create(selectedOrComposed.end, ordinary.end)
                };
            } else {
                return new TextInterval[] {
                    ordinary.create(ordinary.start, selectedOrComposed.start),
                    selectedOrComposed.create(selectedOrComposed.start,
                                              ordinary.end)
                };
            }
        }
    }
}
