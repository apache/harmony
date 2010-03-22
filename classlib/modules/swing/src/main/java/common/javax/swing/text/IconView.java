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
 * @author Roman I. Chernyatchik
 */
package javax.swing.text;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.Icon;
import javax.swing.text.Position.Bias;

import org.apache.harmony.awt.text.TextUtils;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class IconView extends View {

    public IconView(final Element element) {
        super(element);
    }

    public float getAlignment(final int axis) {
        return axis == Y_AXIS ? View.ALIGN_RIGHT : View.ALIGN_CENTER;
    }

    public float getPreferredSpan(final int axis) {
        isAxisValid(axis);
        final Icon icon = (Icon)getAttributes().getAttribute(StyleConstants
                                                             .IconAttribute);
        return axis == View.X_AXIS ? icon.getIconWidth() + 2
                                     : icon.getIconHeight();
    }

    public Shape modelToView(final int pos, final Shape shape, final Bias bias)
        throws BadLocationException {

        return TextUtils.modelToIconOrComponentView(this, pos, shape, bias);
    }

    public int viewToModel(final float x, final float y, final Shape shape,
                           final Bias[] biasReturn) {

        final Rectangle bounds = shape.getBounds();
        if (x > bounds.width / 2 + bounds.x - 1) {
            biasReturn[0] = Position.Bias.Backward;
            return getEndOffset();
        }
        biasReturn[0] = Position.Bias.Forward;
        return getStartOffset();
    }

    public void paint(final Graphics g, final Shape shape) {

        final Rectangle bounds =  shape.getBounds();
        StyleConstants.getIcon(getElement().getAttributes())
            .paintIcon(getComponent(), g, bounds.x + 1, bounds.y);
    }

    private void isAxisValid(final int axis) {
        if (axis != X_AXIS && axis != Y_AXIS) {
            throw new IllegalArgumentException(Messages.getString("swing.00", axis)); //$NON-NLS-1$
        }
    }
 }
