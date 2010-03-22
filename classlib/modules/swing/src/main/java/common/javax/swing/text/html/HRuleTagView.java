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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.Position.Bias;

import org.apache.harmony.awt.text.TextUtils;

class HRuleTagView extends View {
    private static final int INSET = 3;
    private static final int MINIMUM_SIZE = 2;
    private static final Color SHADOW = Color.BLACK;
    private static final Color LIGHT_SHADOW = Color.LIGHT_GRAY;

    private AttributeSet attrs;

    HRuleTagView(final Element elem) {
        super(elem);
    }

    public float getPreferredSpan(final int axis) {
        if (axis == Y_AXIS) {
            return getSizeAttr() + 2 * INSET;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    public void paint(final Graphics g, final Shape allocation) {
        Rectangle r = allocation.getBounds();
        int totalWidth = r.width;
        r.grow(0, -INSET);
        r.width = getWidthAttr();
        calculatePosition(r, totalWidth);

        if (getNoshadeAttr()) {
            g.setColor(SHADOW);
            g.fillRect(r.x, r.y, r.width, r.height);
        } else {
            g.setColor(SHADOW);
            g.drawLine(r.x, r.y, r.x + r.width, r.y);
            int size = getSizeAttr() - 1;
            g.drawLine(r.x, r.y, r.x, r.y + size);
            g.setColor(LIGHT_SHADOW);
            g.drawLine(r.x + 1, r.y + size, r.x + r.width, r.y + size);
            g.drawLine(r.x + r.width, r.y + 1, r.x + r.width, r.y + size);
        }
    }

    public int viewToModel(final float x, final float y,
                           final Shape shape, final Bias[] biasReturn) {
        final Rectangle bounds = shape.getBounds();
        if (x > bounds.width / 2 + bounds.x - 1) {
            biasReturn[0] = Position.Bias.Backward;
            return getEndOffset();
        }
        biasReturn[0] = Position.Bias.Forward;
        return getStartOffset();
    }

    public Shape modelToView(final int pos, final Shape shape,
                             final Bias bias) throws BadLocationException {
        return TextUtils.modelToIconOrComponentView(this, pos, shape, bias);
    }

    public int getBreakWeight(final int axis, final float pos, final float len) {
        return ForcedBreakWeight;
    }

    public int getResizeWeight(final int axis) {
        return axis == X_AXIS ? 1 : 0;
    }

    public AttributeSet getAttributes() {
        if (attrs == null) {
            attrs = ((HTMLDocument)getDocument()).getStyleSheet()
                .getViewAttributes(this);
        }
        return attrs;
    }

    public void changedUpdate(final DocumentEvent event,
                              final Shape shape,
                              final ViewFactory factory) {
        attrs = ((HTMLDocument)getDocument()).getStyleSheet()
            .getViewAttributes(this);
        super.changedUpdate(event, shape, factory);
    }

    private int getSizeAttr() {
        Object value = getElement().getAttributes()
            .getAttribute(HTML.Attribute.SIZE);

        if (value != null) {
            int size = Integer.parseInt((String)value);
            if (size > MINIMUM_SIZE) {
                return size;
            }
        }
        return MINIMUM_SIZE;
    }

    private boolean getNoshadeAttr() {
        Object value = getElement().getAttributes()
            .getAttribute(HTML.Attribute.NOSHADE);
        return value != null;
    }

    private int getWidthAttr() {
        Object value = getAttributes().getAttribute(CSS.Attribute.WIDTH);
        return value != null
            ? ((CSS.FloatValue)value).intValue(this)
            : ((BoxView)getParent().getParent()).getWidth();
    }

    private int getAlignAttr() {
        Object value = getAttributes().getAttribute(StyleConstants.Alignment);
        return value != null
            ? ((Integer)value).intValue()
            : StyleConstants.ALIGN_CENTER;
    }

    private void calculatePosition(final Rectangle r, final int totalWidth) {
        int align = getAlignAttr();
        if (align == StyleConstants.ALIGN_CENTER) {
            r.x += (totalWidth - r.width) / 2;
        } else if (align == StyleConstants.ALIGN_RIGHT) {
            r.x += totalWidth - r.width;
        }
    }
}
