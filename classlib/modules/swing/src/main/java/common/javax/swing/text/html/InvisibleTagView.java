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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.Position.Bias;

class InvisibleTagView extends View {
    InvisibleTagView(final Element elem) {
        super(elem);
    }

    public float getPreferredSpan(final int axis) {
        return 0f;
    }

    public void paint(Graphics g, Shape alloc) {
    }

    public Shape modelToView(int pos, Shape shape, Bias bias) throws BadLocationException {
        Rectangle r = shape.getBounds();
        return new Rectangle(r.x, r.y, 0, r.height);
    }

    public int viewToModel(float x, float y, Shape shape, Bias[] biasReturn) {
        return getStartOffset();
    }

    public boolean isVisible() {
        return false;
    }
}
