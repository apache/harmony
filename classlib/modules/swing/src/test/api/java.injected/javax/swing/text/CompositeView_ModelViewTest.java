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
 *
 */
package javax.swing.text;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.BasicSwingTestCase;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.CompositeViewTest.CompositeViewImpl;
import javax.swing.text.Position.Bias;
import junit.framework.TestCase;

/**
 * Tests CompositeView methods that deal with model/view conversions.
 *
 */
public class CompositeView_ModelViewTest extends TestCase {
    static PlainDocument doc; // Document used in tests

    static Element root; // Default root element of the document

    static CompositeView view; // View object used in tests

    static ViewFactory factory; // View factory used to create new views

    static Shape shape; // View allocation (area to render into)

    static Bias[] bias; // Place for bias return

    /**
     * View which passed as parameter to modelToView or viewToModel method.
     */
    private static View viewAsked;

    /**
     * Shape which passed as parameter to modelToView or viewToModel method.
     */
    private static Shape shapeAsked;

    /**
     * Width of one position in the view.
     */
    static final int POS_WIDTH = 4;

    /**
     * Height of one child view, or line.
     */
    static final int LINE_HEIGHT = 16;

    /**
     * Stores the same value as <code>shape</code> but with different
     * type. Actually <code>bounds</code> and <code>shape</code>
     * are the same object.
     */
    private static Rectangle bounds;

    /**
     * Manages some children of type ChildView.
     */
    public static class WithChildrenView extends CompositeViewImpl {
        public WithChildrenView(final Element element) {
            super(element);
        }

        @Override
        protected void childAllocation(final int index, final Rectangle rc) {
            // The each view allocation is LINE_HEIGHT in height and
            // represents a line-like rectangle
            rc.y = ((Rectangle) shape).y + LINE_HEIGHT * index;
            rc.height = LINE_HEIGHT;
        }

        @Override
        protected View getViewAtPoint(final int x, final int y, final Rectangle rc) {
            int index = getViewIndex(x, y, rc);
            if (index != -1) {
                childAllocation(index, rc);
                return getView(index);
            }
            return null;
        }
    }

    /**
     * Represents child view managed by WithChildrenView.
     */
    public static class ChildView extends View {
        public ChildView(final Element element) {
            super(element);
        }

        @Override
        public int viewToModel(float x, final float y, final Shape shape,
                final Bias[] biasReturn) {
            assertTrue(shape.contains(x, y));
            viewAsked = this;
            shapeAsked = shape;
            Rectangle rect = shape.getBounds();
            x -= rect.x;
            return getStartOffset() + (int) x / POS_WIDTH;
        }

        @Override
        public Shape modelToView(int pos, final Shape shape, final Bias bias)
                throws BadLocationException {
            Rectangle bounds = shape.getBounds();
            viewAsked = this;
            shapeAsked = shape;
            pos -= getStartOffset();
            return new Rectangle(bounds.x + pos * POS_WIDTH, bounds.y, 1, 16);
        }

        @Override
        public void paint(final Graphics g, final Shape shape) {
        }

        @Override
        public float getPreferredSpan(final int axis) {
            return 0;
        }
    }

    /**
     * View factory to create ChildView instances.
     */
    public static class ChildFactory implements ViewFactory {
        public View create(final Element element) {
            return new ChildView(element);
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.insertString(0, "line1\nline2\n\u05DC\u05DD\nline3\n", null);
        // positions: 012345 678901 2 3 4 567890
        // 0 1 2
        root = doc.getDefaultRootElement();
        view = new WithChildrenView(root);
        view.loadChildren(factory = new ChildFactory());
        shape = new Rectangle(100, 200, 190, 560);
        bounds = (Rectangle) shape;
        bias = new Position.Bias[1];
    }

    private Rectangle getChildRect(final int index) throws BadLocationException {
        View child = view.getView(index);
        return (Rectangle) child.modelToView(child.getStartOffset(), Bias.Forward, child
                .getEndOffset(), Bias.Forward, view.getChildAllocation(index, shape));
    }

    /*
     * Class under test for Shape modelToView(int, Bias, int, Bias, Shape)
     */
    public void testModelToViewintBiasintBiasShape() throws BadLocationException {
        Shape res;
        Rectangle rc1;
        Rectangle rc2;
        // Same positions
        res = view.modelToView(0, Bias.Forward, 0, Bias.Forward, shape);
        assertEquals(view.modelToView(0, shape, Bias.Forward), res);
        // Both Forward
        rc1 = (Rectangle) view.modelToView(0, shape, Bias.Forward);
        rc2 = (Rectangle) view.modelToView(1, shape, Bias.Forward);
        res = view.modelToView(0, Bias.Forward, 1, Bias.Forward, shape);
        assertEquals(rc1.union(rc2), res);
        rc1 = (Rectangle) view.modelToView(0, shape, Bias.Forward);
        rc2 = (Rectangle) view.modelToView(2, shape, Bias.Forward);
        res = view.modelToView(0, Bias.Forward, 2, Bias.Forward, shape);
        assertEquals(rc1.union(rc2), res);
        // Different biases
        rc1 = (Rectangle) view.modelToView(0, shape, Bias.Forward);
        rc2 = (Rectangle) view.modelToView(2, shape, Bias.Forward);
        res = view.modelToView(0, Bias.Forward, 2, Bias.Backward, shape);
        assertEquals(rc1.union(rc2), res);
        // On different lines
        rc1 = (Rectangle) view.getView(0).modelToView(6, shape, Bias.Forward);
        rc2 = (Rectangle) view.modelToView(6, shape, Bias.Forward);
        res = view.modelToView(0, Bias.Forward, 6, Bias.Forward, shape);
        assertEquals(rc1.union(rc2), res);
        rc1 = getChildRect(0);
        rc2 = (Rectangle) view.modelToView(7, shape, Bias.Forward);
        res = view.modelToView(0, Bias.Forward, 7, Bias.Forward, shape);
        assertEquals(rc1.union(rc2), res);
        // Assert both previous cases return the same value
        assertEquals(view.modelToView(0, Bias.Forward, 6, Bias.Forward, shape), view
                .modelToView(0, Bias.Forward, 7, Bias.Forward, shape));
        // When range doesn't include first line start
        rc1 = getChildRect(0);
        rc2 = (Rectangle) view.modelToView(9, shape, Bias.Forward);
        res = view.modelToView(2, Bias.Forward, 7, Bias.Forward, shape);
        assertEquals(rc1.union(rc2), res);
        // Both on line 2
        rc1 = (Rectangle) view.modelToView(7, shape, Bias.Forward);
        rc2 = (Rectangle) view.modelToView(9, shape, Bias.Forward);
        res = view.modelToView(7, Bias.Forward, 9, Bias.Forward, shape);
        assertEquals(rc1.union(rc2), res);
        // From line 1 to line 3
        // TODO investigate more carefully modelToView(int, B, int, B, Shape)
        //        Can't use this code 'cause view.getChildAllocation(1, shape) and
        //        view.getChildAllocation(2, shape) return the same instance of
        //        rectangles, tho' the instances MUST be different acc. to the spec
        //
        //        rc1 = (Rectangle)view.getChildAllocation(1, shape);
        //        rc1 = rc1.union((Rectangle)view.getChildAllocation(2, shape));
        //        rc1 = rc1.union((Rectangle)view.getChildAllocation(3, shape));
        //
        //        The work around is to get child allocations from the inside but
        //        not the public interface
        rc1 = shape.getBounds();
        view.childAllocation(0, rc1);
        rc2 = shape.getBounds();
        view.childAllocation(2, rc2);
        res = view.modelToView(1, Bias.Forward, 13, Bias.Forward, shape);
        assertEquals(rc1.union(rc2), res);
        // From line 2 to line 3
        rc1 = getChildRect(1);
        rc2 = (Rectangle) view.modelToView(13, shape, Bias.Forward);
        res = view.modelToView(9, Bias.Forward, 13, Bias.Forward, shape);
        assertEquals(rc1.union(rc2), res);
        // From line 2 to line 4
        rc1 = shape.getBounds();
        view.childAllocation(1, rc1);
        //System.out.println(rc1);
        rc2 = shape.getBounds();
        view.childAllocation(3, rc2);
        res = view.modelToView(9, Bias.Forward, 17, Bias.Forward, shape);
        assertEquals(rc1.union(rc2), res);
    }

    /*
     * Class under test for Shape modelToView(int, Shape, Bias)
     */
    public void testModelToViewintShapeBias() throws BadLocationException {
        assertEquals(new Rectangle(bounds.x, bounds.y, 1, LINE_HEIGHT), view.modelToView(0,
                shape, Bias.Forward));
        assertSame(view.getView(0), viewAsked);
        assertEquals(new Rectangle(bounds.x + 3 * POS_WIDTH, bounds.y, 1, LINE_HEIGHT), view
                .modelToView(3, shape, Bias.Forward));
        assertSame(view.getView(0), viewAsked);
        assertEquals(LINE_HEIGHT, ((Rectangle) shapeAsked).height);
        assertEquals(bounds.y, ((Rectangle) shapeAsked).y);
        assertEquals(new Rectangle(bounds.x + 1 * POS_WIDTH, bounds.y + LINE_HEIGHT, 1,
                LINE_HEIGHT), view.modelToView(7, shape, Bias.Forward));
        assertSame(view.getView(1), viewAsked);
        assertEquals(LINE_HEIGHT, ((Rectangle) shapeAsked).height);
        assertEquals(bounds.y + LINE_HEIGHT, ((Rectangle) shapeAsked).y);
        assertEquals(new Rectangle(bounds.x + 2 * POS_WIDTH, bounds.y + 3 * LINE_HEIGHT, 1,
                LINE_HEIGHT), view.modelToView(17, shape, Bias.Forward));
        assertSame(view.getView(3), viewAsked);
        assertEquals(LINE_HEIGHT, ((Rectangle) shapeAsked).height);
        assertEquals(bounds.y + 3 * LINE_HEIGHT, ((Rectangle) shapeAsked).y);
    }

    /**
     * Tests viewToModel(float, float, Shape, Bias[]).
     * For isBefore, isAfter the Y is used only. In this test either
     * isBefore or isAfter is true.
     */
    public void testViewToModel01() {
        CompositeViewTest.useBoth = false;
        CompositeViewTest.useX = false;
        int x, y;
        // Before the allocation
        x = bounds.x;
        y = bounds.y - 1;
        assertTrue(view.isBefore(x, y, bounds));
        assertEquals(0, view.viewToModel(x, y, shape, bias));
        // After the allocation
        y = bounds.y + bounds.height + 1;
        assertTrue(view.isAfter(x, y, bounds));
        assertEquals(doc.getLength(), view.viewToModel(x, y, shape, bias));
    }

    /**
     * Tests viewToModel(float, float, Shape, Bias[]).
     * For isBefore, isAfter (always false in the test) the Y is used only.
     */
    public void testViewToModel02() {
        CompositeViewTest.useBoth = false;
        CompositeViewTest.useX = false;
        int x, y;
        // In the first box
        x = bounds.x + 5;
        y = bounds.y + 4;
        assertEquals(5 / POS_WIDTH, view.viewToModel(x, y, shape, bias));
        assertSame(view.getView(0), viewAsked);
        assertEquals(view.getChildAllocation(0, shape), shapeAsked);
        viewAsked = null;
        shapeAsked = null;
        // X coordinate is not in the shape
        x = bounds.x - 1;
        y = bounds.y + 4 + LINE_HEIGHT;
        // Assert the condition used internally
        assertFalse(view.isBefore(x, y, bounds));
        assertFalse(view.isAfter(x, y, bounds));
        assertEquals(-1, view.getViewIndex(x, y, shape));
        assertNull(view.getViewAtPoint(x, y, shape.getBounds()));
        // We will get -1, however it's invalid model position
        assertEquals(-1, view.viewToModel(x, y, shape, bias));
        // No view was asked
        assertNull(viewAsked);
        assertNull(shapeAsked);
        // One may easily the assertions above are also true for this case
        x = bounds.x + bounds.width;
        y = bounds.y + 4 + LINE_HEIGHT;
        assertEquals(-1, view.viewToModel(x, y, shape, bias));
        assertNull(viewAsked);
        assertNull(shapeAsked);
        // In the second box
        x = bounds.x;
        y = bounds.y + 4 + LINE_HEIGHT;
        assertEquals(1, view.getViewIndex(x, y, shape));
        assertSame(view.getView(1), view.getViewAtPoint(x, y, shape.getBounds()));
        assertEquals(view.getView(1).getStartOffset(), view.viewToModel(x, y, shape, bias));
        assertSame(view.getView(1), viewAsked);
        assertEquals(LINE_HEIGHT, ((Rectangle) shapeAsked).height);
        assertEquals(bounds.y + LINE_HEIGHT, ((Rectangle) shapeAsked).y);
        viewAsked = null;
        shapeAsked = null;
    }

    /**
     * Tests viewToModel(float, float, Shape, Bias[]).
     * The body is the same as in testViewToModel02
     * (irrelevant asserts are removed) but:
     * for isBefore and isAfter, the X is used only.
     */
    public void testViewToModel03() {
        CompositeViewTest.useBoth = false;
        CompositeViewTest.useX = true;
        int x, y;
        // In the first box
        x = bounds.x + 5;
        y = bounds.y + 4;
        assertEquals(5 / POS_WIDTH, view.viewToModel(x, y, shape, bias));
        assertSame(view.getView(0), viewAsked);
        assertEquals(view.getChildAllocation(0, shape), shapeAsked);
        viewAsked = null;
        shapeAsked = null;
        // X coordinate is not in the shape
        x = bounds.x - 1;
        y = bounds.y + 4 + LINE_HEIGHT;
        // Assert the condition used internally
        assertTrue(view.isBefore(x, y, bounds));
        assertEquals(0, view.viewToModel(x, y, shape, bias));
        // No view was asked
        assertNull(viewAsked);
        assertNull(shapeAsked);
        x = bounds.x + bounds.width + 1;
        y = bounds.y + 4 + LINE_HEIGHT;
        assertTrue(view.isAfter(x, y, bounds));
        assertEquals(doc.getLength(), view.viewToModel(x, y, shape, bias));
        assertNull(viewAsked);
        assertNull(shapeAsked);
        // In the second box
        x = bounds.x;
        y = bounds.y + 4 + LINE_HEIGHT;
        assertEquals(1, view.getViewIndex(x, y, shape));
        assertSame(view.getView(1), view.getViewAtPoint(x, y, shape.getBounds()));
        assertEquals(view.getView(1).getStartOffset(), view.viewToModel(x, y, shape, bias));
        assertSame(view.getView(1), viewAsked);
        assertEquals(LINE_HEIGHT, ((Rectangle) shapeAsked).height);
        assertEquals(bounds.y + LINE_HEIGHT, ((Rectangle) shapeAsked).y);
        viewAsked = null;
        shapeAsked = null;
    }

    /**
     * Tests viewToModel(float, float, Shape, Bias[]).
     * For isBefore, isAfter the Y is used only. In this test either
     * isBefore or isAfter is true.
     * <p>In this test modified <code>root</code> is used so that
     * <code>view.getStartOffset() != 0</code> and
     * <code>view.getEndOffset() != doc.getLength()</code>.
     */
    public void testViewToModel04() {
        // Modify the root
        final int oldCount = root.getElementCount();
        BranchElement docRoot = (BranchElement) root;
        final Element[] empty = new Element[0];
        docRoot.replace(0, 1, empty);
        docRoot.replace(docRoot.getElementCount() - 1, 1, empty);
        final int newCount = root.getElementCount();
        assertEquals(oldCount - 2, newCount);
        // Re-load children
        view.removeAll();
        view.loadChildren(factory);
        CompositeViewTest.useBoth = false;
        CompositeViewTest.useX = false;
        int x, y;
        // Before the allocation
        x = bounds.x;
        y = bounds.y - 1;
        assertTrue(view.isBefore(x, y, bounds));
        int offset = view.viewToModel(x, y, shape, bias);
        assertEquals(view.getStartOffset(), offset);
        assertEquals(root.getElement(0).getStartOffset(), offset);
        // After the allocation
        y = bounds.y + bounds.height + 1;
        assertTrue(view.isAfter(x, y, bounds));
        bias[0] = null;
        offset = view.viewToModel(x, y, shape, bias);
        assertEquals(view.getEndOffset() - 1, offset);
        assertEquals(root.getElement(root.getElementCount() - 1).getEndOffset() - 1, offset);
        if (BasicSwingTestCase.isHarmony()) {
            assertSame(Bias.Backward, bias[0]);
        }
    }

    public void testViewToModel05() {
        final boolean[] methodCalled = new boolean[] { false };
        view = new WithChildrenView(root) {
            @Override
            protected View getViewAtPoint(int x, int y, Rectangle rc) {
                methodCalled[0] = true;
                return super.getViewAtPoint(x, y, rc);
            }
        };
        view.loadChildren(factory);
        assertFalse(methodCalled[0]);
        view.viewToModel(bounds.x + bounds.width / 2, bounds.y + bounds.height, shape, bias);
        assertTrue(methodCalled[0]);
        assertSame(view.getView(view.getViewIndex(bounds.x, bounds.y, shape)), view
                .getViewAtPoint(bounds.x, bounds.y, view.getInsideAllocation(shape)));
    }
}
