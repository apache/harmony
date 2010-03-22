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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.text;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.event.DocumentEvent;

import org.apache.harmony.awt.text.TextUtils;

public class FieldView extends PlainView {

    public FieldView(final Element elem) {
        super(elem);
    }

    protected Shape adjustAllocation(final Shape shape) {
        Component comp = getComponent();
        allocation = TextUtils
            .getFieldViewAllocation(this,
                                    TextUtils.getTextFieldKit(comp),
                                    shape,
                                    comp.getComponentOrientation());
        return allocation;
    }

    private Shape getAllocation() {
        return allocation;
    }

    protected FontMetrics getFontMetrics() {
       conditionalUpdateMetrics();
       return metrics;
    }

    public float getPreferredSpan(final int axis) {
       FontMetrics fm = getFontMetrics();
       if (fm == null) {
           return 0;
       }
       int start = getElement().getStartOffset();
       int end = getElement().getEndOffset() - 1;
       Segment text = new Segment();
       try {
           getDocument().getText(start, end - start, text);
       } catch (final BadLocationException e) {
       }
       return (axis == View.X_AXIS)
               ? TextUtils.getTabbedTextWidth(text, getFontMetrics(), 0,
                                              this, start)
               : fm.getHeight();
    }

    public int getResizeWeight(final int axis) {
       return (axis == View.X_AXIS) ? 1 : 0;
    }

    private Shape allocation;

    public void insertUpdate(final DocumentEvent changes, final Shape a,
                             final ViewFactory f) {
       super.insertUpdate(changes, adjustAllocation(a), f);
    }

    final Rectangle getOldAllocation(final Shape a) {
        if (getAllocation() == null) {
            return adjustAllocation(a).getBounds();
        }
        Rectangle shape = getAllocation().getBounds();
        Shape sh = TextUtils
            .getFieldViewAllocation(this,
                                    TextUtils.getTextFieldKit(getComponent()),
                                    a,
                                    getComponent().getComponentOrientation());
        shape.setLocation(sh.getBounds().getLocation());
        return shape;

    }

    public Shape modelToView(final int pos, final Shape a,
                             final Position.Bias b)
        throws BadLocationException {
       return super.modelToView(pos, getOldAllocation(a), b);
    }

    public void paint(final Graphics g, final Shape a) {
        super.paint(g, adjustAllocation(a));
    }

    public void removeUpdate(final DocumentEvent changes, final Shape a,
                             final ViewFactory f) {
        Shape oldAllocation = getAllocation();
        adjustAllocation(a);
        super.removeUpdate(changes, oldAllocation, f);
    }

    public int viewToModel(final float fx, final float fy, final Shape a,
                           final Position.Bias[] bias) {
        return super.viewToModel(fx, fy, adjustAllocation(a), bias);
    }
}