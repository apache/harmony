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
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import javax.swing.Icon;
import javax.swing.JTextPane;
import javax.swing.SwingTestCase;
import javax.swing.plaf.metal.MetalIconFactory;

public class IconViewTest extends SwingTestCase {
    StyledDocument document;

    JTextPane textPane;

    Element iconElement;

    Icon icon;

    IconView view;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        textPane = new JTextPane();
        document = textPane.getStyledDocument();
        icon = MetalIconFactory.getCheckBoxMenuItemIcon();
        document.insertString(0, "Hello\n word!!!", new SimpleAttributeSet());
        textPane.setCaretPosition(3);
        textPane.insertIcon(icon);
        iconElement = document.getDefaultRootElement().getElement(0).getElement(1);
        view = new IconView(iconElement);
    }

    public void testIconView() {
        assertNotNull(StyleConstants.getIcon(iconElement.getAttributes()));
        iconElement = document.getDefaultRootElement();
        view = new IconView(iconElement);
        assertNull(StyleConstants.getIcon(iconElement.getAttributes()));
        assertNull(view.getParent());
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getPreferredSpan(View.X_AXIS);
            }
        });
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getPreferredSpan(View.Y_AXIS);
            }
        });
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.paint(createTestGraphics(), new Rectangle(10, 10));
            }
        });
    }

    public void testGetPreferredSpan() {
        if (isHarmony()) {
            assertEquals(icon.getIconWidth() + 2, view.getPreferredSpan(View.X_AXIS), 1);
        } else {
            assertEquals(icon.getIconWidth(), view.getPreferredSpan(View.X_AXIS), 1);
        }
        assertEquals(icon.getIconHeight(), view.getPreferredSpan(View.Y_AXIS), 1);
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
        MutableAttributeSet attrs = new SimpleAttributeSet();
        icon = MetalIconFactory.getRadioButtonIcon();
        StyleConstants.setIcon(attrs, icon);
        document.setCharacterAttributes(3, 1, attrs, true);
        view = new IconView(iconElement);
        if (isHarmony()) {
            assertEquals(icon.getIconWidth() + 2, view.getPreferredSpan(View.X_AXIS), 0.1);
        } else {
            assertEquals(icon.getIconWidth(), view.getPreferredSpan(View.X_AXIS), 0.1);
        }
        assertEquals(icon.getIconHeight(), view.getPreferredSpan(View.Y_AXIS), 0.1);
    }

    public void testGetAlignment() {
        assertEquals(View.ALIGN_CENTER, view.getAlignment(View.X_AXIS), 0.1);
        assertEquals(View.ALIGN_RIGHT, view.getAlignment(View.Y_AXIS), 0.1);
        assertEquals(View.ALIGN_CENTER, view.getAlignment(2), 0.1);
        assertEquals(View.ALIGN_CENTER, view.getAlignment(-1), 0.1);
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
        assertEquals(3, view.getStartOffset());
        view.modelToView(3, new Rectangle(), Position.Bias.Backward);
        view.modelToView(3, new Rectangle(), Position.Bias.Forward);
        assertEquals(4, view.getEndOffset());
        view.modelToView(4, new Rectangle(), Position.Bias.Backward);
        view.modelToView(4, new Rectangle(), Position.Bias.Forward);
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
        box = view.modelToView(3,
                new Rectangle(2, 3, icon.getIconWidth(), icon.getIconHeight()),
                Position.Bias.Backward);
        assertNotNull(box);
        checkBounds(2, 3, icon.getIconHeight(), box);
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

    public void testPaint() {
        Shape shape;
        Graphics g = createTestGraphics();
        shape = new Rectangle(4, 3, 2, 3);
        checkPaintIcon(g, shape, 10, 15);
        shape = new Rectangle(4, 3, 30, 40);
        checkPaintIcon(g, shape, 10, 15);
        shape = new Ellipse2D() {
            @Override
            public double getX() {
                return 12;
            }

            @Override
            public double getY() {
                return 4;
            }

            @Override
            public double getWidth() {
                return 30;
            }

            @Override
            public double getHeight() {
                return 5;
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
        checkPaintIcon(g, shape, 2, 4);
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

    private void checkBounds(int x, int y, int height, Shape box) {
        Rectangle bounds = box.getBounds();
        assertEquals(x, bounds.x);
        assertEquals(y, bounds.y);
        assertEquals(0, bounds.width);
        assertEquals(height, bounds.height);
    }

    private void checkViewToModel(Shape shape, int x, int y) {
        Position.Bias[] bias = new Position.Bias[1];
        int position = view.viewToModel(x, y, shape, bias);
        if (x > shape.getBounds().width / 2 + shape.getBounds().x - 1) {
            assertEquals(Position.Bias.Backward, bias[0]);
            assertEquals(position, iconElement.getEndOffset());
        } else {
            assertEquals(Position.Bias.Forward, bias[0]);
            assertEquals(position, iconElement.getStartOffset());
        }
    }

    private void checkPaintIcon(Graphics g, Shape shape, int iconWidth, int iconHeight) {
        textPane.setCaretPosition(3);
        icon = createIcon(shape, iconWidth, iconHeight);
        textPane.insertIcon(icon);
        iconElement = document.getDefaultRootElement().getElement(0).getElement(1);
        view = new IconView(iconElement);
        assertEquals(StyleConstants.getIcon(view.getElement().getAttributes()), icon);
        view.paint(g, shape);
    }

    private Icon createIcon(Shape shape, final int width, final int height) {
        final Rectangle bounds = shape.getBounds();
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                if (isHarmony()) {
                    assertEquals(x, bounds.x + 1);
                } else {
                    assertEquals(x, bounds.x);
                }
                assertEquals(y, bounds.y);
            }

            public int getIconWidth() {
                return width;
            }

            public int getIconHeight() {
                return height;
            }
        };
    }
}