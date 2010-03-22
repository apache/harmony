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
package javax.swing.text.html;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SizeRequirements;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.html.StyleSheet.BoxPainter;

public class ParagraphView extends javax.swing.text.ParagraphView {
    private AttributeSet attrs;
    private BoxPainter boxPainter;
    private float xAlign;
    private CSS.Length width;
    private int effectiveWidth;

    public ParagraphView(final Element elem) {
        super(elem);
    }

    public float getMinimumSpan(final int axis) {
        if (!isVisible()) {
            return 0;
        }
        return super.getMinimumSpan(axis);
    }

    public float getPreferredSpan(final int axis) {
        if (!isVisible()) {
            return 0;
        }
        return super.getPreferredSpan(axis);
    }

    public float getMaximumSpan(final int axis) {
        if (!isVisible()) {
            return 0;
        }
        return super.getMaximumSpan(axis);
    }

    public AttributeSet getAttributes() {
        return attrs;
    }

    public float getAlignment(final int axis) {
        if (axis == X_AXIS) {
            return xAlign;
        }
        return super.getAlignment(axis);
    }

    public void setParent(final View parent) {
        super.setParent(parent);
        if (parent != null) {
            setPropertiesFromAttributes();
        }
    }

    public boolean isVisible() {
        final int count = getLayoutViewCount();
        boolean result = false;
        for (int i = 0; i < count && !result; i++) {
            View child = getLayoutView(i);
            result = child.getElement().getAttributes()
                          .getAttribute(HTML.Attribute.IMPLIED_NEW_LINE) == null
                     && child.isVisible();
        }
        return result;
    }

    public void setSize(final float width, final float height) {
        if (effectiveWidth != calculateEffectiveWidth()) {
            preferenceChanged(this, true, true);
        }
        super.setSize(width, height);
    }

    public void paint(final Graphics g, final Shape a) {
        Rectangle rc = a.getBounds();
        boxPainter.paint(g, rc.x, rc.y, rc.width, rc.height, this);

        super.paint(g, a);
    }

    protected SizeRequirements
        calculateMinorAxisRequirements(final int axis,
                                       final SizeRequirements r) {
        SizeRequirements result = super.calculateMinorAxisRequirements(axis, r);
        result.minimum = getMinimumSpan();
        effectiveWidth = calculateEffectiveWidth();
        if (effectiveWidth > result.minimum) {
            result.minimum   = effectiveWidth;
            result.preferred = result.minimum;
            result.maximum   = result.minimum;
        }
        return result;
    }

//    protected SizeRequirements
//        calculateMajorAxisRequirements(final int axis,
//                                       final SizeRequirements sr) {
//
//        SizeRequirements result = super.calculateMajorAxisRequirements(axis,
//                                                                       sr);
//        result.alignment = getAlign();
//        return result;
//    }

    protected StyleSheet getStyleSheet() {
        return ((HTMLDocument)getDocument()).getStyleSheet();
    }

    protected void setPropertiesFromAttributes() {
        attrs = getStyleSheet().getViewAttributes(this);
        super.setPropertiesFromAttributes();
        boxPainter = getStyleSheet().getBoxPainter(getAttributes());
        boxPainter.setView(this);
        setInsets((short)boxPainter.getInset(TOP, this),
                  (short)boxPainter.getInset(LEFT, this),
                  (short)boxPainter.getInset(BOTTOM, this),
                  (short)boxPainter.getInset(RIGHT, this));

        xAlign = getAlign();
        width = (CSS.Length)getAttributes().getAttribute(CSS.Attribute.WIDTH);
    }

    private int calculateEffectiveWidth() {
        return width != null ? effectiveWidth = width.intValue(this) : 0;
    }

    private int getMinimumSpan() {
        int result = 0;
        final int count = getLayoutViewCount();
        for (int i = 0; i < count; i++) {
            View child = getLayoutView(i);
            if (child instanceof InlineView) {
                result = Math.max(result,
                                  ((InlineView)child).getLongestWordSpan());
            } else {
                result = Math.max(result, (int)child.getMinimumSpan(X_AXIS));
            }
        }
        return result;
    }

    private float getAlign() {
        final View parent = getParent();
        if (parent == null) {
            return 0.0f;
        }

        CSS.TextAlign ta =
            (CSS.TextAlign)parent.getAttributes()
                           .getAttribute(CSS.Attribute.TEXT_ALIGN);
        int textAlign = ta != null ? ((Integer)ta.fromCSS()).intValue()
                                   : StyleConstants.ALIGN_LEFT;
        switch (textAlign) {
        case StyleConstants.ALIGN_LEFT:
            return 0.0f;
        case StyleConstants.ALIGN_CENTER:
            return 0.5f;
        case StyleConstants.ALIGN_RIGHT:
            return 1.0f;
        default:
            return 0.0f;
        }
    }
}

