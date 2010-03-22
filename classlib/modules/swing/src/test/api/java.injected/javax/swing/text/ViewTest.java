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
 */
package javax.swing.text;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.text.Position.Bias;
import junit.framework.TestCase;

public class ViewTest extends TestCase {
    /**
     * View class that simply overrides abstract methods of View class.
     * No additional functionality is added.
     */
    public static class DisAbstractedView extends View {
        /**
         * Bounding shape. Used as the last parameter in
         * modelToView, viewToModel.
         */
        static final Rectangle2D bounding = new Rectangle(2, 2);

        /**
         * One of rectangles returned from modelToView.
         */
        static final Rectangle2D r1 = new Rectangle(0, 0, 1, 1);

        /**
         * The second rectangle returned from modelToView.
         */
        static final Rectangle2D r2 = new Rectangle(1, 1, 1, 1);

        /**
         *
         * @param element
         */
        static final Rectangle2D r3;
        static {
            r3 = new Rectangle2D.Double();
            Rectangle2D.union(r1, r2, r3);
        }

        public DisAbstractedView(final Element element) {
            super(element);
        }

        @Override
        public float getPreferredSpan(final int axis) {
            return axis == X_AXIS ? 1.27f : 2.53f;
        }

        @Override
        public Shape modelToView(final int pos, final Shape shape, final Bias bias)
                throws BadLocationException {
            mtvBias = bias;
            if (pos == 1) {
                return r2;
            }
            return r1;
        }

        @Override
        public void paint(final Graphics g, final Shape shape) {
        }

        @Override
        public int viewToModel(final float x, final float y, final Shape shape,
                final Bias[] biasReturn) {
            // Save the parameters
            viewToModelParams.x = x;
            viewToModelParams.y = y;
            viewToModelParams.shape = shape;
            viewToModelParams.bias = biasReturn;
            return 0;
        }
    }

    private static class ResizableView extends DisAbstractedView {
        public ResizableView(final Element element) {
            super(element);
        }

        @Override
        public int getResizeWeight(final int axis) {
            return 1;
        }
    }

    /**
     * Class to store parameters passed to viewToModel method. It is used
     * to assert the parameters are the expected ones.
     */
    private static class ViewToModelParams {
        public Bias[] bias;

        public Shape shape;

        public float x;

        public float y;
    }

    boolean containerGetGraphicsCalled;

    // Used in testPreferenceChanged
    boolean preferenceChangedCalledOnParent;

    View preferenceChangedChild;

    boolean revalidateCalled;

    boolean tooltipGetViewCalled;

    boolean tooltipGetViewIndexCalled;

    /**
     * Document shared among tests.
     */
    private Document doc;

    /**
     * Paragraph element shared among tests. This is the second line of
     * text in the document.
     */
    private Element line;

    /**
     * View under which tests are run.
     */
    private View view;

    /**
     * Bias passes to modelToView method in the last call.
     */
    private static Bias mtvBias;

    /**
     * Parameters passed to viewToModel method.
     */
    private static ViewToModelParams viewToModelParams;

    public void testBreakView() {
        assertSame(view, view.breakView(View.X_AXIS, line.getStartOffset() + 1, 4, 4));
    }

    public void testCreateFragment() {
        assertSame(view, view
                .createFragment(line.getStartOffset() + 1, line.getEndOffset() - 1));
    }

    public void testGetAlignment() {
        assertEquals(0.5f, view.getAlignment(View.X_AXIS), 0.000001f);
        assertEquals(0.5f, view.getAlignment(View.Y_AXIS), 0.000001f);
        assertEquals(0.5f, view.getAlignment(-10), 0.000001f);
        assertEquals(0.5f, view.getAlignment(10), 0.000001f);
    }

    public void testGetAttributes() {
        assertSame(line.getAttributes(), view.getAttributes());
    }

    /**
     * Tests <code>getBreakWeight</code> with default view setup.
     */
    public void testGetBreakWeight01() {
        final float maxX = view.getMaximumSpan(View.X_AXIS); // = preferredSpan
        final float maxY = view.getMaximumSpan(View.Y_AXIS);
        assertEquals(maxX, view.getPreferredSpan(View.X_AXIS), 0.0001f);
        // By default return Bad, but...
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.X_AXIS, 0.0f, maxX));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS, 0.0f, maxY));
        // ...but Good when length where to break is greater than
        // the length of the view
        assertEquals(View.GoodBreakWeight, view.getBreakWeight(View.X_AXIS, 0.0f, maxX + 0.01f));
        assertEquals(View.GoodBreakWeight, view.getBreakWeight(View.Y_AXIS, 0.0f, maxY + 0.01f));
    }

    /**
     * Tests <code>getBreakWeight</code> with resizable setup.
     */
    public void testGetBreakWeight02() {
        view = new ResizableView(line);
        final float maxX = view.getPreferredSpan(View.X_AXIS);
        final float maxY = view.getPreferredSpan(View.Y_AXIS);
        // By default return Bad, but...
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.X_AXIS, 0.0f, maxX));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS, 0.0f, maxY));
        // ...but Good when length where to break is greater than
        // the length of the view
        assertEquals(View.GoodBreakWeight, view.getBreakWeight(View.X_AXIS, 0.0f, maxX + 0.01f));
        assertEquals(View.GoodBreakWeight, view.getBreakWeight(View.Y_AXIS, 0.0f, maxY + 0.01f));
    }

    public void testGetChildAllocation() {
        // Always returns null whatever parameters are
        assertNull(view.getChildAllocation(0, null));
        assertNull(view.getChildAllocation(0, new Rectangle()));
        assertNull(view.getChildAllocation(2, new Rectangle()));
    }

    public void testGetContainer() {
        assertNull(view.getContainer());
    }

    public void testGetDocument() {
        assertSame(doc, view.getDocument());
    }

    public void testGetElement() {
        assertSame(line, view.getElement());
    }

    public void testGetEndOffset() throws BadLocationException {
        assertEquals(12, view.getEndOffset());
        doc.insertString(0, "line\n", null);
        assertEquals(17, view.getEndOffset());
    }

    public void testGetGraphics() {
        view = new DisAbstractedView(doc.getDefaultRootElement().getElement(0)) {
            @Override
            public Container getContainer() {
                return new JComponent() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Graphics getGraphics() {
                        containerGetGraphicsCalled = true;
                        return null;
                    }
                };
            }
        };
        assertFalse(containerGetGraphicsCalled);
        assertNull(view.getGraphics());
        assertTrue(containerGetGraphicsCalled);
    }

    /**
     * Tests <code>getMaximumSpan</code> with default view setup.
     */
    public void testGetMaximumSpan01() {
        assertEquals(view.getPreferredSpan(View.X_AXIS), view.getMaximumSpan(View.X_AXIS),
                0.000001f);
        assertEquals(view.getPreferredSpan(View.Y_AXIS), view.getMaximumSpan(View.Y_AXIS),
                0.000001f);
    }

    /**
     * Tests <code>getMaximumSpan</code> with resizable view.
     */
    public void testGetMaximumSpan02() {
        view = new ResizableView(line);
        assertEquals(Integer.MAX_VALUE, view.getMaximumSpan(View.X_AXIS), 0.000001f);
        assertEquals(Integer.MAX_VALUE, view.getMaximumSpan(View.Y_AXIS), 0.000001f);
    }

    /**
     * Tests <code>getMinimumSpan</code> with default view setup.
     */
    public void testGetMinimumSpan01() {
        assertEquals(view.getPreferredSpan(View.X_AXIS), view.getMinimumSpan(View.X_AXIS),
                0.000001f);
        assertEquals(view.getPreferredSpan(View.Y_AXIS), view.getMinimumSpan(View.Y_AXIS),
                0.000001f);
    }

    /**
     * Tests <code>getMinimumSpan</code> with resizable view.
     */
    public void testGetMinimumSpan02() {
        view = new ResizableView(line);
        assertEquals(0.0f, view.getMinimumSpan(View.X_AXIS), 0.000001f);
        assertEquals(0.0f, view.getMinimumSpan(View.Y_AXIS), 0.000001f);
    }

    public void testGetParent() {
        assertNull(view.getParent());
        View parent = getLine1View(doc);
        view.setParent(parent);
        assertSame(parent, view.getParent());
    }

    public void testGetResizeWeight() {
        assertEquals(0, view.getResizeWeight(View.X_AXIS));
        assertEquals(0, view.getResizeWeight(View.Y_AXIS));
    }

    public void testGetStartOffset() throws BadLocationException {
        assertEquals(6, view.getStartOffset());
        doc.insertString(0, "line\n", null);
        assertEquals(11, view.getStartOffset());
    }

    public void testGetToolTipText() {
        view = new DisAbstractedView(doc.getDefaultRootElement().getElement(0)) {
            @Override
            public View getView(final int index) {
                tooltipGetViewCalled = true;
                return super.getView(index);
            }

            @Override
            public int getViewIndex(final float x, final float y, final Shape allocation) {
                tooltipGetViewIndexCalled = true;
                return super.getViewIndex(x, y, allocation);
            }
        };
        assertNull(view.getToolTipText(0.0f, 0.0f, new Rectangle(2, 2)));
        // getToolTipText calls getViewIndex to get child at this location
        assertTrue(tooltipGetViewIndexCalled);
        // As there's no children at all, getView didn't get called and null
        // is return (which is asserted above)
        assertFalse(tooltipGetViewCalled);
    }

    public void testGetView() {
        // Always returns null as there are no children
        assertNull(view.getView(-1));
        assertNull(view.getView(0));
        assertNull(view.getView(10));
    }

    public void testGetViewCount() {
        assertEquals(0, view.getViewCount());
        view.append(getLine1View(doc));
        // View does not have children by default, nothing is changed
        assertEquals(0, view.getViewCount());
    }

    public void testGetViewFactory() {
        assertNull(view.getViewFactory());
    }

    /*
     * Class under test for int getViewIndex(float, float, Shape)
     */
    public void testGetViewIndexfloatfloatShape() {
        // As this view has no children, the method always returns -1
        assertEquals(-1, view.getViewIndex(0.0f, 0.0f, null));
        assertEquals(-1, view.getViewIndex(0.0f, 0.0f, new Rectangle(5, 5)));
        assertEquals(-1, view.getViewIndex(0.5f, 0.5f, new Rectangle(5, 5)));
    }

    /*
     * Class under test for int getViewIndex(int, Position.Bias)
     */
    public void testGetViewIndexintBias() {
        // The method should return -1
        assertEquals(-1, view.getViewIndex(0, Bias.Forward));
        assertEquals(-1, view.getViewIndex(0, Bias.Backward));
        assertEquals(-1, view.getViewIndex(7, Bias.Forward));
        assertEquals(-1, view.getViewIndex(7, Bias.Backward));
    }

    public void testIsVisible() {
        assertTrue(view.isVisible());
    }

    /*
     * Class under test for
     * Shape modelToView(int, Position.Bias, int, Position.Bias, Shape)
     */
    public void testModelToViewintBiasintBiasShape() throws BadLocationException {
        Shape a = view
                .modelToView(1, Bias.Forward, 2, Bias.Forward, DisAbstractedView.bounding);
        assertEquals(DisAbstractedView.r3, a);
    }

    /*
     * Class under test for Shape modelToView(int, Shape)
     */
    @SuppressWarnings("deprecation")
    public void testModelToViewintShape() throws BadLocationException {
        Shape a = view.modelToView(0, DisAbstractedView.bounding);
        assertSame(DisAbstractedView.r1, a);
        assertEquals(Bias.Forward, mtvBias);
    }

    public void testPreferenceChanged() {
        View parent = new DisAbstractedView(doc.getDefaultRootElement()) {
            @Override
            public Container getContainer() {
                return new JTextComponent() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void revalidate() {
                        revalidateCalled = true;
                        super.revalidate();
                    }
                };
            }

            @Override
            public void preferenceChanged(final View child, final boolean w, final boolean h) {
                preferenceChangedCalledOnParent = true;
                preferenceChangedChild = child;
                // Assert the conditions are true
                assertFalse(revalidateCalled);
                assertNull(getParent());
                // Call superclass
                super.preferenceChanged(child, w, h);
                // Component's revalidate method is still not called
                assertFalse(revalidateCalled);
            }
        };
        View child = getLine1View(doc);
        // Assert we get no exceptions or errors
        assertNull(view.getParent());
        view.preferenceChanged(child, true, false);
        assertFalse(preferenceChangedCalledOnParent);
        // Set parent and test, that parent gets called
        view.setParent(parent);
        view.preferenceChanged(child, true, false);
        assertTrue(preferenceChangedCalledOnParent);
        // The view on which we called the method is the child for parent
        assertSame(view, preferenceChangedChild);
    }

    public void testSetParent() {
        View parent = getLine1View(doc);
        view.setParent(parent);
        assertSame(parent, view.getParent());
        view.setParent(null);
        assertNull(view.getParent());
    }

    public void testSetSize() {
        // XXX View.setSize does nothing?
        view.setSize(10f, 20f);
        //assertEquals(10f, /*0.0*/view.getMaximumSpan(View.X_AXIS), 0.00001f);
        //assertEquals(20f, /*0.0*/view.getMaximumSpan(View.Y_AXIS), 0.00001f);
    }

    public void testView() {
        View v = new DisAbstractedView(line);
        assertSame(line, v.getElement());
        assertSame(doc, v.getDocument());
        v = new DisAbstractedView(null);
        assertNull(v.getElement());
        try {
            assertNull(v.getDocument());
            fail("This causes the exception 'cause element is used " + "to get the document");
        } catch (NullPointerException e) {
        }
        try {
            assertEquals(0, v.getStartOffset());
            fail("This causes the exception 'cause element is used " + "to get the offset");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Class under test for int viewToModel(float, float, Shape)
     */
    @SuppressWarnings("deprecation")
    public void testViewToModelfloatfloatShape() {
        Rectangle r = new Rectangle();
        view.viewToModel(0.1f, 0.2f, r);
        assertEquals(0.1f, viewToModelParams.x, 0.00001f);
        assertEquals(0.2f, viewToModelParams.y, 0.00001f);
        assertSame(r, viewToModelParams.shape);
        assertEquals(1, viewToModelParams.bias.length);
        assertEquals(Bias.Forward, viewToModelParams.bias[0]);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.insertString(0, "01234\nabcde", null);
        line = doc.getDefaultRootElement().getElement(1);
        view = new DisAbstractedView(line);
        viewToModelParams = new ViewToModelParams();
        mtvBias = null;
    }

    /**
     * Creates a new view for first line of text within the document.
     * @return view for line 1
     */
    static final View getLine1View(final Document doc) {
        return new DisAbstractedView(doc.getDefaultRootElement().getElement(0));
    }
}
