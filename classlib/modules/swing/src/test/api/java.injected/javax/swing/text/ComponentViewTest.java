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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingTestCase;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.PlainView;


public class ComponentViewTest extends SwingTestCase {
    StyledDocument document;

    JTextPane textPane;

    Element componentElement;

    JButton insertedComponent;

    ComponentView view;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        insertedComponent = new JButton();
        textPane = new JTextPane();
        document = textPane.getStyledDocument();
        document.insertString(0, "Hello\n word!!!", new SimpleAttributeSet());
        textPane.setCaretPosition(3);
        textPane.insertComponent(insertedComponent);
        componentElement = document.getDefaultRootElement().getElement(0).getElement(1);
        view = new ComponentView(componentElement);
    }

    public void testComponentView() {
        assertNotNull(StyleConstants.getComponent(componentElement.getAttributes()));
        componentElement = document.getDefaultRootElement();
        assertNull(view.getParent());
        assertNull(view.getComponent());
    }

    public void testCreateComponent() {
        final Marker createComponentCalled = new Marker();
        view = new ComponentView(componentElement) {
            @Override
            public void setParent(View parent) {
                createComponentCalled.setOccurred();
                super.setParent(parent);
            }
        };
        assertEquals(insertedComponent, view.createComponent());
        JPanel panel = new JPanel();
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setComponent(attrs, panel);
        document.setCharacterAttributes(3, 1, attrs, true);
        assertSame(panel, view.createComponent());
        createComponentCalled.reset();
        view.setParent(textPane.getUI().getRootView(textPane));
        assertTrue(createComponentCalled.isOccurred());
        assertSame(panel, view.createComponent());
        JTextArea textArea = new JTextArea();
        createComponentCalled.reset();
        view.setParent(textArea.getUI().getRootView(textArea));
        assertSame(panel, view.createComponent());
        assertTrue(createComponentCalled.isOccurred());
        createComponentCalled.reset();
        view.setParent(null);
        assertTrue(createComponentCalled.isOccurred());
        createComponentCalled.reset();
        view.setParent(textArea.getUI().getRootView(textArea));
        assertTrue(createComponentCalled.isOccurred());
        assertSame(panel, view.createComponent());
    }

    public void testGetPreferredSpan() {
        Component c;
        assertTrue(0 == view.getPreferredSpan(View.X_AXIS));
        assertTrue(0 == view.getPreferredSpan(View.Y_AXIS));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getPreferredSpan(2);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getPreferredSpan(-1);
            }
        });
        insertedComponent.setPreferredSize(new Dimension(100, 200));
        view.setParent(textPane.getUI().getRootView(textPane));
        c = view.getComponent();
        if (isHarmony()) {
            assertTrue(c.getPreferredSize().width + 2 == view.getPreferredSpan(View.X_AXIS));
        } else {
            assertTrue(c.getPreferredSize().width == view.getPreferredSpan(View.X_AXIS));
        }
        assertTrue(c.getPreferredSize().height == view.getPreferredSpan(View.Y_AXIS));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getPreferredSpan(2);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getPreferredSpan(-1);
            }
        });
        c.setPreferredSize(new Dimension(20, 30));
        if (isHarmony()) {
            assertTrue(22 == view.getPreferredSpan(View.X_AXIS));
            assertTrue(30 == view.getPreferredSpan(View.Y_AXIS));
        } else {
            assertTrue(100 == view.getPreferredSpan(View.X_AXIS));
            assertTrue(200 == view.getPreferredSpan(View.Y_AXIS));
        }
        view.setParent(null);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getPreferredSpan(2);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getPreferredSpan(-1);
            }
        });
        c = view.getComponent();
        if (isHarmony()) {
            assertTrue(0 == view.getPreferredSpan(View.X_AXIS));
            assertTrue(0 == view.getPreferredSpan(View.Y_AXIS));
        } else {
            assertTrue(100 == view.getPreferredSpan(View.X_AXIS));
            assertTrue(200 == view.getPreferredSpan(View.Y_AXIS));
        }
        view.getComponent().setPreferredSize(new Dimension(20, 30));
        if (isHarmony()) {
            assertTrue(0 == view.getPreferredSpan(View.X_AXIS));
            assertTrue(0 == view.getPreferredSpan(View.Y_AXIS));
        } else {
            assertTrue(100 == view.getPreferredSpan(View.X_AXIS));
            assertTrue(200 == view.getPreferredSpan(View.Y_AXIS));
        }
    }

    public void testGetMinimumSpan() {
        Component c;
        assertTrue(0 == view.getMinimumSpan(View.X_AXIS));
        assertTrue(0 == view.getMinimumSpan(View.Y_AXIS));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getMinimumSpan(2);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getMinimumSpan(-1);
            }
        });
        insertedComponent.setMinimumSize(new Dimension(100, 200));
        view.setParent(textPane.getUI().getRootView(textPane));
        c = view.getComponent();
        if (isHarmony()) {
            assertTrue(c.getMinimumSize().width + 2 == view.getMinimumSpan(View.X_AXIS));
        } else {
            assertTrue(c.getMinimumSize().width == view.getMinimumSpan(View.X_AXIS));
        }
        assertTrue(c.getMinimumSize().height == view.getMinimumSpan(View.Y_AXIS));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getMinimumSpan(2);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getMinimumSpan(-1);
            }
        });
        c.setMinimumSize(new Dimension(20, 30));
        if (isHarmony()) {
            assertTrue(22 == view.getMinimumSpan(View.X_AXIS));
            assertTrue(30 == view.getMinimumSpan(View.Y_AXIS));
        } else {
            assertTrue(100 == view.getMinimumSpan(View.X_AXIS));
            assertTrue(200 == view.getMinimumSpan(View.Y_AXIS));
        }
        view.setParent(null);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getMinimumSpan(2);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getMinimumSpan(-1);
            }
        });
        c = view.getComponent();
        if (isHarmony()) {
            assertTrue(0 == view.getMinimumSpan(View.X_AXIS));
            assertTrue(0 == view.getMinimumSpan(View.Y_AXIS));
        } else {
            assertTrue(100 == view.getMinimumSpan(View.X_AXIS));
            assertTrue(200 == view.getMinimumSpan(View.Y_AXIS));
        }
        view.getComponent().setMinimumSize(new Dimension(20, 30));
        if (isHarmony()) {
            assertTrue(0 == view.getMinimumSpan(View.X_AXIS));
            assertTrue(0 == view.getMinimumSpan(View.Y_AXIS));
        } else {
            assertTrue(100 == view.getMinimumSpan(View.X_AXIS));
            assertTrue(200 == view.getMinimumSpan(View.Y_AXIS));
        }
    }

    public void testGetMaximumSpan() {
        Component c;
        assertTrue(0 == view.getMaximumSpan(View.X_AXIS));
        assertTrue(0 == view.getMaximumSpan(View.Y_AXIS));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getMaximumSpan(2);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getMaximumSpan(-1);
            }
        });
        insertedComponent.setMaximumSize(new Dimension(100, 200));
        view.setParent(textPane.getUI().getRootView(textPane));
        c = view.getComponent();
        if (isHarmony()) {
            assertTrue(c.getMaximumSize().width + 2 == view.getMaximumSpan(View.X_AXIS));
        } else {
            assertTrue(c.getMaximumSize().width == view.getMaximumSpan(View.X_AXIS));
        }
        assertTrue(c.getMaximumSize().height == view.getMaximumSpan(View.Y_AXIS));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getMaximumSpan(2);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getMaximumSpan(-1);
            }
        });
        c.setMaximumSize(new Dimension(20, 30));
        if (isHarmony()) {
            assertTrue(22 == view.getMaximumSpan(View.X_AXIS));
            assertTrue(30 == view.getMaximumSpan(View.Y_AXIS));
        } else {
            assertTrue(100 == view.getMaximumSpan(View.X_AXIS));
            assertTrue(200 == view.getMaximumSpan(View.Y_AXIS));
        }
        view.setParent(null);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getMaximumSpan(2);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getMaximumSpan(-1);
            }
        });
        c = view.getComponent();
        if (isHarmony()) {
            assertTrue(0 == view.getMaximumSpan(View.X_AXIS));
            assertTrue(0 == view.getMaximumSpan(View.Y_AXIS));
        } else {
            assertTrue(100 == view.getMaximumSpan(View.X_AXIS));
            assertTrue(200 == view.getMaximumSpan(View.Y_AXIS));
        }
        view.getComponent().setMaximumSize(new Dimension(20, 30));
        if (isHarmony()) {
            assertTrue(0 == view.getMaximumSpan(View.X_AXIS));
            assertTrue(0 == view.getMaximumSpan(View.Y_AXIS));
        } else {
            assertTrue(100 == view.getMaximumSpan(View.X_AXIS));
            assertTrue(200 == view.getMaximumSpan(View.Y_AXIS));
        }
    }

    public void testGetAlignment() {
        JComponent jc;
        assertTrue(View.ALIGN_CENTER == view.getAlignment(View.X_AXIS));
        assertTrue(View.ALIGN_CENTER == view.getAlignment(View.Y_AXIS));
        assertTrue(View.ALIGN_CENTER == view.getAlignment(2));
        assertTrue(View.ALIGN_CENTER == view.getAlignment(-1));
        insertedComponent.setAlignmentX(0.3f);
        insertedComponent.setAlignmentY(0.6f);
        view.setParent(textPane.getUI().getRootView(textPane));
        jc = (JComponent) view.getComponent();
        assertEquals(jc.getAlignmentX(), view.getAlignment(View.X_AXIS), 0.001);
        assertEquals(jc.getAlignmentY(), view.getAlignment(View.Y_AXIS), 0.001);
        assertTrue(View.ALIGN_CENTER == view.getAlignment(2));
        assertTrue(View.ALIGN_CENTER == view.getAlignment(-1));
        jc.setAlignmentX(0.8f);
        jc.setAlignmentY(0.9f);
        if (isHarmony()) {
            assertEquals(0.8f, view.getAlignment(View.X_AXIS), 0.001);
            assertEquals(0.9f, view.getAlignment(View.Y_AXIS), 0.001);
        } else {
            assertEquals(0.3f, view.getAlignment(View.X_AXIS), 0.001);
            assertEquals(0.6f, view.getAlignment(View.Y_AXIS), 0.001);
        }
        jc.setAlignmentX(0.9f);
        jc.setAlignmentY(1.0f);
        if (isHarmony()) {
            assertEquals(0.9f, view.getAlignment(View.X_AXIS), 0.001);
            assertEquals(1.0f, view.getAlignment(View.Y_AXIS), 0.001);
        } else {
            assertEquals(0.3f, view.getAlignment(View.X_AXIS), 0.001);
            assertEquals(0.6f, view.getAlignment(View.Y_AXIS), 0.001);
        }
        view.setParent(null);
        if (isHarmony()) {
            assertEquals(0.9f, view.getAlignment(View.X_AXIS), 0.001);
            assertEquals(1.0f, view.getAlignment(View.Y_AXIS), 0.001);
        } else {
            assertEquals(0.3f, view.getAlignment(View.X_AXIS), 0.001);
            assertEquals(0.6f, view.getAlignment(View.Y_AXIS), 0.001);
        }
        ((JComponent) view.getComponent()).setAlignmentX(0.1f);
        ((JComponent) view.getComponent()).setAlignmentY(0.2f);
        if (isHarmony()) {
            assertEquals(0.1f, view.getAlignment(View.X_AXIS), 0.001);
            assertEquals(0.2f, view.getAlignment(View.Y_AXIS), 0.001);
        } else {
            assertEquals(0.3f, view.getAlignment(View.X_AXIS), 0.001);
            assertEquals(0.6f, view.getAlignment(View.Y_AXIS), 0.001);
        }
    }

    public void testGetComponent() {
        assertNull(view.getComponent());
        assertNotNull(view.createComponent());
        assertNull(view.getComponent());
        view.setParent(null);
        assertNull(view.getComponent());
        assertNotNull(view.createComponent());
        assertNull(view.getComponent());
        view.setParent(textPane.getUI().getRootView(textPane));
        Component c = view.createComponent();
        assertSame(c, view.getComponent());
        JPanel panel = new JPanel();
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setComponent(attrs, panel);
        document.setCharacterAttributes(3, 1, attrs, true);
        assertSame(c, view.getComponent());
        view.setParent(null);
        assertSame(c, view.getComponent());
        JTextArea textArea = new JTextArea();
        view.setParent(textArea.getUI().getRootView(textArea));
        assertSame(c, view.getComponent());
    }

    public void testModelToView() throws BadLocationException {
        testExceptionalCase(new BadLocationCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.modelToView(1, new Rectangle(), Position.Bias.Backward);
            }
        });
        testExceptionalCase(new BadLocationCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.modelToView(5, new Rectangle(), Position.Bias.Forward);
            }
        });
        testExceptionalCase(new BadLocationCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.modelToView(500, new Rectangle(), Position.Bias.Forward);
            }
        });
        Shape box;
        box = view.modelToView(3, new Rectangle(2, 5, 20, 30), Position.Bias.Backward);
        assertNotNull(box);
        checkBounds(2, 5, 30, box);
        box = view.modelToView(3, new Rectangle(2, 3, insertedComponent.getWidth(),
                insertedComponent.getHeight()), Position.Bias.Backward);
        assertNotNull(box);
        checkBounds(2, 3, insertedComponent.getHeight(), box);
        box = view.modelToView(4, new Rectangle(1, 1, 16, 7), Position.Bias.Forward);
        assertNotNull(box);
        checkBounds(17, 1, 7, box);
        box = view.modelToView(4, new Rectangle(0, 0, 1, 1), Position.Bias.Forward);
        assertNotNull(box);
        checkBounds(1, 0, 1, box);
        Ellipse2D ellipse = new Ellipse2D.Float(25, 3, 30, 40);
        box = view.modelToView(4, ellipse, Position.Bias.Forward);
        checkBounds(55, 3, 40, box);
        box = view.modelToView(4, ellipse, Position.Bias.Backward);
        assertNotNull(box);
        checkBounds(55, 3, 40, box);
        Rectangle2D rect = new Rectangle2D() {
            @Override
            public void setRect(double x, double y, double width, double height) {
            }

            @Override
            public int outcode(double x, double y) {
                return 0;
            }

            @Override
            public Rectangle2D createIntersection(Rectangle2D r) {
                return null;
            }

            @Override
            public Rectangle2D createUnion(Rectangle2D r) {
                return null;
            }

            @Override
            public double getX() {
                return 1;
            }

            @Override
            public double getY() {
                return 2;
            }

            @Override
            public double getWidth() {
                return 50;
            }

            @Override
            public double getHeight() {
                return 60;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };
        box = view.modelToView(4, rect, Position.Bias.Backward);
        assertNotNull(box);
        checkBounds(51, 2, 60, box);
    }

    public void testViewToModel() {
        Shape shape;
        shape = new Rectangle(4, 3, 20, 30);
        checkViewToModel(shape, 1, 1);
        checkViewToModel(shape, 1, 5);
        checkViewToModel(shape, 1, 15);
        checkViewToModel(shape, 1, -5);
        checkViewToModel(shape, 12, 5);
        checkViewToModel(shape, 15, 5);
        checkViewToModel(shape, 16, 5);
        checkViewToModel(shape, 30, 5);
        checkViewToModel(shape, 46, 5);
        final Ellipse2D floatEllipse = new Ellipse2D.Float(25, 3, 3, 4);
        checkViewToModelWithEllipse(floatEllipse);
        Ellipse2D intEllipse = new Ellipse2D() {
            @Override
            public double getX() {
                return 25;
            }

            @Override
            public double getY() {
                return 3;
            }

            @Override
            public double getWidth() {
                return 3;
            }

            @Override
            public double getHeight() {
                return 4;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public void setFrame(double x, double y, double w, double h) {
                return;
            }

            public Rectangle2D getBounds2D() {
                return null;
            }
        };
        checkViewToModelWithEllipse(intEllipse);
    }

    public void testPaint() {
        final Marker paintCalled = new Marker();
        JPanel panel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            public void paint(Graphics graphics) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void paintImmediately(Rectangle rect) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void paintImmediately(int x, int y, int width, int height) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void paintComponents(Graphics g) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void paintAll(Graphics g) {
                throw new UnsupportedOperationException();
            }
        };
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setComponent(attrs, panel);
        document.setCharacterAttributes(3, 1, attrs, true);
        view = new ComponentView(componentElement);
        assertNull(view.getParent());
        assertNull(view.getComponent());
        paintCalled.reset();
        view.paint(createTestGraphics(), new Rectangle(10, 10));
        assertFalse(paintCalled.isOccurred());
        view.setParent(textPane.getUI().getRootView(textPane));
        paintCalled.reset();
        view.paint(createTestGraphics(), new Rectangle(100, 100));
        assertFalse(paintCalled.isOccurred());
        assertSame(panel, view.getComponent());
        view.setParent(null);
        paintCalled.reset();
        view.paint(createTestGraphics(), new Rectangle(10, 10));
        assertFalse(paintCalled.isOccurred());
    }

    public void testSetParent() {
        final Marker getViewCountCalled = new Marker();
        view = new ComponentView(componentElement) {
            @Override
            public int getViewCount() {
                getViewCountCalled.setOccurred();
                return super.getViewCount();
            }
        };
        view.setParent(null);
        assertNull(view.getComponent());
        assertNull(view.getParent());
        assertNull(view.getContainer());
        view.setParent(textPane.getUI().getRootView(textPane));
        if (isHarmony()) {
            assertSame(textPane, view.getComponent().getParent());
        } else {
            assertSame(textPane, view.getComponent().getParent().getParent());
        }
        JTextArea textArea = new JTextArea();
        view.setParent(textArea.getUI().getRootView(textArea));
        assertNotNull(view.getParent());
        assertNotNull(view.getContainer());
        if (isHarmony()) {
            assertSame(textPane, view.getComponent().getParent());
        } else {
            assertNotSame(textPane, view.getComponent().getParent());
            assertFalse(textPane.equals(view.getComponent().getParent()));
            assertSame(textPane, view.getComponent().getParent().getParent());
        }
        getViewCountCalled.reset();
        view.setParent(null);
        assertTrue(getViewCountCalled.isOccurred());
        assertNotNull(view.getComponent());
        assertNull(view.getContainer());
        assertNull(view.getParent());
        if (isHarmony()) {
            assertNull(view.getComponent().getParent());
        } else {
            assertNotNull(view.getComponent().getParent());
            assertNull(view.getComponent().getParent().getParent());
        }
        Component c = view.getComponent();
        JPanel panel = new JPanel();
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setComponent(attrs, panel);
        document.setCharacterAttributes(3, 1, attrs, true);
        view.setParent(textPane.getUI().getRootView(textPane));
        assertSame(c, view.getComponent());
        if (isHarmony()) {
            assertSame(textPane, view.getComponent().getParent());
        } else {
            assertNotSame(textPane, view.getComponent().getParent());
            assertFalse(textPane.equals(view.getComponent().getParent()));
            assertSame(textPane, view.getComponent().getParent().getParent());
        }
    }

    public void testSetParent_View() {
        // Regression test for HARMONY-1767 
        PlainDocument doc = new PlainDocument();
        Element e = doc.getDefaultRootElement();
        ComponentView obj = new ComponentView(new TestElement());
        obj.setParent(new PlainView(e));
    }  

    private static class TestElement implements Element {
        public boolean isLeaf() {
            return false;
        }
        public Element getElement(int index) {
            return null;
        }
        public int getElementCount() {
            return 0;
        }
        public int getElementIndex(int offset) {
            return 0;
        }
        public int getEndOffset() {
            return 0;
        }       
        public int getStartOffset() {
            return 0;
        }       
        public AttributeSet getAttributes() {
            return null;
        }
        public String getName() {
            return "AA";
        }
        public Element getParentElement() {
            return null;
        }
        public Document getDocument() {
            return null;
        }
    }

    private void checkBounds(final int x, final int y, final int height, final Shape box) {
        Rectangle bounds = box.getBounds();
        assertEquals(x, bounds.x);
        assertEquals(y, bounds.y);
        assertEquals(0, bounds.width);
        assertEquals(height, bounds.height);
    }

    private void checkViewToModelWithEllipse(final Ellipse2D ellipse) {
        if (isHarmony()) {
            checkViewToModel(ellipse, 25, 15);
            checkViewToModel(ellipse, 26, 5);
        } else {
            testExceptionalCase(new ClassCastCase() {
                @Override
                public void exceptionalAction() throws Exception {
                    checkViewToModel(ellipse, 25, 15);
                }
            });
            testExceptionalCase(new ClassCastCase() {
                @Override
                public void exceptionalAction() throws Exception {
                    checkViewToModel(ellipse, 26, 5);
                }
            });
        }
    }

    private void checkViewToModel(final Shape shape, final int x, final int y) {
        Position.Bias[] bias = new Position.Bias[1];
        int position = view.viewToModel(x, y, shape, bias);
        if (x > shape.getBounds().width / 2 + shape.getBounds().x - 1) {
            assertEquals(Position.Bias.Backward, bias[0]);
            assertEquals(position, componentElement.getEndOffset());
        } else {
            assertEquals(Position.Bias.Forward, bias[0]);
            assertEquals(position, componentElement.getStartOffset());
        }
    }
}
