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

package javax.swing.plaf.basic;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.Reader;
import java.io.StringReader;

import javax.swing.JComponent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.Position.Bias;
import javax.swing.text.html.HTMLEditorKit;

public class BasicHTML {

    public static final String documentBaseKey = "html.base"; //$NON-NLS-1

    public static final String propertyKey = "html"; //$NON-NLS-1

    /**
     * Used to detect HTML strings in {@link #isHTMLString(String)}.
     */
    private static final String detectString = "<html"; //$NON-NLS-1

    public static void updateRenderer(JComponent c, String text) {
        c.putClientProperty(propertyKey, (isHTMLString(text) ? 
                createHTMLView(c, text) : null));
    }

    public static boolean isHTMLString(String s) {
        // Maybe s.trim() would be useful but RI doesn't do it.
        return ((s != null) && s.toLowerCase().startsWith(detectString));
    }

    public static View createHTMLView(JComponent c, String html) {
        return new Renderer(c,html);
    }

    /**
     * Renderer is a RootView for the views obtained from html string.
     */
    static class Renderer extends View {

        /**
         * JComponent that uses this Renderer to draw itself
         */
        private final JComponent component;

        /**
         * Son view is a view that do all the job. But it haven't reference to
         * factory and styles
         */
        private final View son;

        /**
         * The factory obtained from HTMLEditorKit
         */
        private final ViewFactory factory;

        Renderer(JComponent component, String html) {
            
            super(null);
            this.component = component;
            
            HTMLEditorKit kit = new HTMLEditorKit();
            Document doc = kit.createDefaultDocument();
            Reader r = new StringReader(html);
            
            try {
                kit.read(r, doc, 0);
            } catch (Throwable e) {
                // Ignored for now. Need to be tested
            }
            
            factory = kit.getViewFactory();
            son = factory.create(doc.getDefaultRootElement());
            son.setParent(this);
        }
        
        @Override
        public AttributeSet getAttributes() {
            return null;
        }

        @Override
        public float getPreferredSpan(int axis) {
            return son.getPreferredSpan(axis);
        }

        @Override
        public void preferenceChanged(View child, boolean width, boolean height) {
            component.repaint();
        }

        @Override
        public void paint(Graphics g, Shape allocation) {
            Rectangle rect = allocation.getBounds();
            son.setSize(rect.width, rect.height);
            son.paint(g, allocation);
        }

        @Override
        public int getViewCount() {
            return 1;
        }

        @Override
        public View getView(int i) {
            return son;
        }

        @Override
        public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
            return son.viewToModel(x, y, a, bias);
        }

        @Override
        public Element getElement() {
            return son.getElement();
        }

        @Override
        public Container getContainer() {
            return component;
        }

        @Override
        public ViewFactory getViewFactory() {
            return factory;
        }

        @Override
        public Shape modelToView(int pos, Shape shape, Bias bias)
                throws BadLocationException {
            return son.modelToView(pos, shape, bias);
        }

    }

}
