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

import java.awt.Shape;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position.Bias;

import org.apache.harmony.awt.text.TextUtils;

class NoFramesTagView extends BlockView {
    public NoFramesTagView(final Element elem) {
        super(elem, Y_AXIS);
    }

    public float getMinimumSpan(final int axis) {
        return isTextComponentEditable() ? super.getMinimumSpan(axis) : 0f;
    }

    public float getPreferredSpan(final int axis) {
        return isTextComponentEditable() ? super.getPreferredSpan(axis) : 0f;
    }

    public float getMaximumSpan(final int axis) {
        return isTextComponentEditable() ? super.getMaximumSpan(axis) : 0f;
    }

    public Shape modelToView(final int pos, final Shape shape,
                             final Bias bias) throws BadLocationException {
        return isTextComponentEditable()
            ? super.modelToView(pos, shape, bias)
            : TextUtils.modelToIconOrComponentView(this, pos, shape, bias);
    }

    public int viewToModel(final float x,
                           final float y,
                           final Shape shape,
                           final Bias[] biasReturn) {
        return isTextComponentEditable()
            ? super.viewToModel(x, y, shape, biasReturn)
            : getStartOffset();
    }

    private boolean isTextComponentEditable() {
        return ((JTextComponent)getContainer()).isEditable();
    }
}
