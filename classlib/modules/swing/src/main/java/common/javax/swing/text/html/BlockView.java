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
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BoxView;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.StyleSheet.BoxPainter;

public class BlockView extends BoxView {
    private AttributeSet attrs;
    private BoxPainter boxPainter;

    private CSS.Length width;
    private CSS.Length height;
    private float effectiveWidth;
    private float effectiveHeight;

    public BlockView(final Element elem, final int axis) {
        super(elem, axis);
    }

    public float getAlignment(final int axis) {
        return 0;
    }

    public AttributeSet getAttributes() {
        if (attrs == null) {
            attrs = getStyleSheet().getViewAttributes(this);
        }
        return attrs;
    }

    public void setParent(final View parent) {
        super.setParent(parent);
        if (parent != null) {
            setPropertiesFromAttributes();
        }
    }

    public void changedUpdate(final DocumentEvent event,
                              final Shape shape,
                              final ViewFactory factory) {
        attrs = getStyleSheet().getViewAttributes(this);
        setPropertiesFromAttributes();
        super.changedUpdate(event, shape, factory);
    }

    public void paint(final Graphics g, final Shape allocation) {
        Rectangle rc = allocation.getBounds();

        // Fix for HARMONY-4755, boxPainter is only initialized
        // after setPropertiesFromAttributes() is called.
        if (boxPainter != null) {
            boxPainter.paint(g, rc.x, rc.y, rc.width, rc.height, this);
        }
        super.paint(g, allocation);
    }

    public void setSize(final float width, final float height) {
        boolean widthChanged = effectiveWidth != calculateEffectiveSize(X_AXIS);
        boolean heightChanged = effectiveHeight
                                != calculateEffectiveSize(Y_AXIS);
        if (widthChanged || heightChanged) {
            preferenceChanged(this, widthChanged, heightChanged);
            return;
        }
        super.setSize(width, height);
    }

    protected SizeRequirements
        calculateMajorAxisRequirements(final int axis,
                                       final SizeRequirements r) {

        SizeRequirements result = super.calculateMajorAxisRequirements(axis, r);
        float size = calculateEffectiveSize(axis);
        if (size - getFullInsets(axis) > result.minimum) {
            result.minimum   = (int)size - getFullInsets(axis);
            result.preferred = result.minimum;
            result.maximum   = result.minimum;
        }
        return result;
    }

    protected SizeRequirements
        calculateMinorAxisRequirements(final int axis,
                                       final SizeRequirements r) {

        SizeRequirements result = super.calculateMinorAxisRequirements(axis, r);
        float size = calculateEffectiveSize(axis);
        if (size - getFullInsets(axis) > result.minimum) {
            result.minimum   = (int)size - getFullInsets(axis);
            result.preferred = result.minimum;
            result.maximum   = result.minimum;
        }
        return result;
    }

    protected StyleSheet getStyleSheet() {
        return ((HTMLDocument)getDocument()).getStyleSheet();
    }

    protected void setPropertiesFromAttributes() {
        boxPainter = getStyleSheet().getBoxPainter(getAttributes());
        boxPainter.setView(this);
        setInsets((short)boxPainter.getInset(TOP, this),
                  (short)boxPainter.getInset(LEFT, this),
                  (short)boxPainter.getInset(BOTTOM, this),
                  (short)boxPainter.getInset(RIGHT, this));
        width  = getSizeAttribute(X_AXIS);
        height = getSizeAttribute(Y_AXIS);
    }

    private float calculateEffectiveSize(final int axis) {
        if (axis == X_AXIS) {
            effectiveWidth = calculateEffectiveSize(width);
            return effectiveWidth;
        } else {
            effectiveHeight = calculateEffectiveSize(height);
            return effectiveHeight;
        }
    }

    private float calculateEffectiveSize(final CSS.Length size) {
        return size != null ? size.floatValue(this) : 0;
    }

    private CSS.Length getSizeAttribute(final int axis) {
        return (CSS.Length)getAttributes().getAttribute(axis == X_AXIS
                                                        ? CSS.Attribute.WIDTH
                                                        : CSS.Attribute.HEIGHT);
    }

    private int getFullInsets(final int axis) {
        return axis == X_AXIS ? getLeftInset() + getRightInset()
                              : getTopInset() + getBottomInset();
    }
}

