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
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Shape;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Position.Bias;

import org.apache.harmony.awt.ComponentInternals;
import org.apache.harmony.awt.text.RootViewContext;


class RootView extends View {
    private View son;
    private Document document;
    private RootViewContext.ViewFactoryGetter viewFactoryGetter;

    /**
     * Parent of the view hierarchy. If model changes, only child of root view
     * is replaced.
     */
    RootViewContext rootViewContext = new RootViewContext() {
        public View getView() {
            return RootView.this;
        }

        public void setComponent(final Component c) {
            component = c;
        }

        public void setDocument(final Document d) {
            document = d;
        }

        public void setViewFactoryGetter(final ViewFactoryGetter vfg) {
            viewFactoryGetter = vfg;
        }

    };

    RootView(final Element e) {
        super(e);
    }

    public void preferenceChanged(final View view, final boolean b1,
                                  final boolean b2) {
        if (component != null && (b1 || b2)) {
            ComponentInternals.getComponentInternals().getTextKit(component)
            .revalidate();
        }
    }

    public Shape getChildAllocation(final int i, final Shape shape) {
        return shape;
    }

    public int getResizeWeight(final int i) {
        int resizeWeight;
        readLock();
        try {
            resizeWeight = son.getResizeWeight(i);
        } finally {
            readUnlock();
        }
        return resizeWeight;
    }

    public int getNextVisualPositionFrom(final int p, final Bias b,
                                         final Shape shape,
                                         final int direction,
                                         final Bias[] biasRet)
            throws BadLocationException {
        int position;
        readLock();
        try {
            position =  (son != null)
            ? son.getNextVisualPositionFrom(p, b, shape, direction,
                                            biasRet) : 0;
        } finally {
            readUnlock();
        }
        return position;
    }

    public float getMaximumSpan(final int axis) {
        //son.getMaximumSpan() sometimes returns very strange things
        //So getMaximumSpan(View.Y_AXIS) may be more than
        //getMinimumSpan(View.Y_AXIS) (TextComponentDemo, for example).
        return Integer.MAX_VALUE;
    }

    public float getMinimumSpan(final int axis) {
        float minSpan;
        readLock();
        try {
            minSpan = (son != null) ? son.getMinimumSpan(axis) : 10;
        } finally {
            readUnlock();
        }
        return minSpan;
    }

    public Element getElement() {
        return son.getElement();
    }


    public int getEndOffset() {
        int result;
        readLock();
        try {
            result = (son != null) ? son.getEndOffset() : 0;
        } finally {
            readUnlock();
        }
        return result;
    }

    public int getStartOffset() {
        int result;
        readLock();
        try {
            result = (son != null) ? son.getStartOffset() : 0;
        } finally {
            readUnlock();
        }
        return result;
    }


    public Document getDocument() {
        return document;
    }

    public ViewFactory getViewFactory() {
        return viewFactoryGetter.getViewFactory();
    }

    public View getParent() {
        return null;
    }

    public float getPreferredSpan(final int axis) {
        float prefSpan;
        readLock();
        try {
            prefSpan = (son != null) ? son.getPreferredSpan(axis) : 10;
        } finally {
            readUnlock();
        }
        return prefSpan;
    }

    public void insertUpdate(final DocumentEvent changes, final Shape a,
                             final ViewFactory f) {
        if (son != null) {
            son.insertUpdate(changes, a, f);
        }
    }

    public Shape modelToView(final int p, final Shape shape,
                             final Position.Bias b)
            throws BadLocationException {
        Shape sh = null;
        readLock();
        try {
            sh = (son != null) ? son.modelToView(p, shape, b) : null;
        } finally {
            readUnlock();
        }
        return sh;
    }

    public Shape modelToView(final int p0, final Bias b0, final int p1,
                             final Bias b1, final Shape shape)
            throws BadLocationException {
        Shape sh = null;
        readLock();
        try {
            sh = (son != null) ? son.modelToView(p0, b0, p1, b1, shape)
                : null;
        } finally {
            readUnlock();
        }
        return sh;
    }

    public void paint(final Graphics g, final Shape a) {
        if (son != null) {
            son.paint(g, a);
        }
    }

    public void removeUpdate(final DocumentEvent changes, final Shape a,
                             final ViewFactory f) {
        son.removeUpdate(changes, a, f);
    }

    public void changedUpdate(final DocumentEvent changes, final Shape a,
                              final ViewFactory f) {
        son.changedUpdate(changes, a, f);
    }

    public void setSize(final float width, final float height) {
        if (son == null) {
            return;
        }
        readLock();
        try {
            son.setSize(width, height);
        } finally {
            readUnlock();
        }
    }

    public int viewToModel(final float fx, final float fy, final Shape a,
                           final Position.Bias[] bias) {
        int offset;
        readLock();
        try {
            offset = (son != null) ? son.viewToModel(fx, fy, a, bias) : 0;
        } finally {
            readUnlock();
        }
        return offset;
    }

    public Container getContainer() {
        if (component instanceof Container) {
            return (Container)component;
        }

        return null;
    }

    Component getComponent() {
        return component;
    }

    public void replace(final int i1, final int i2, final View[] views) {
        if (son != null) {
            son.setParent(null);
        }
        son = views[0];
        son.setParent(this);
    }

    public int getViewCount() {
        return 1;
    }

    public View getView(final int index) {
        return son;
    }

    public AttributeSet getAttributes() {
        return null;
    }

    public String getToolTipText(final float x, final float y,
                                 final Shape shape) {
        String text = null;
        readLock();
        try {
           text  = (son != null) ? son.getToolTipText(x, y, shape) : null;
        } finally {
            readUnlock();
        }
        return text;
    }

    private void readLock() {
        if (document instanceof AbstractDocument) {
            ((AbstractDocument)document).readLock();
        }
    }

    private void readUnlock() {
        if (document instanceof AbstractDocument) {
            ((AbstractDocument)document).readUnlock();
        }
    }
}