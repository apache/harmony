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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BasicSwingTestCase;
import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.text.FlowView.FlowStrategy;
import javax.swing.text.ViewTestHelpers.ChildrenFactory;
import javax.swing.text.ViewTestHelpers.ElementPartView;

/**
 * Tests the majority of methods of FlowView class.
 * @see javax.swing.text.FlowView_ChangesTest
 *
 */
public class FlowViewTest extends BasicSwingTestCase {
    protected static class FlowViewImpl extends FlowView {
        private int count = 0;

        public FlowViewImpl(final Element element, final int axis) {
            super(element, axis);
        }

        @Override
        protected View createRow() {
            return new BoxView(getElement(), getAxis() == Y_AXIS ? X_AXIS : Y_AXIS) {
                private final int id = count++;

                @Override
                public String toString() {
                    return "row(" + id + ")";
                }

                @Override
                protected void loadChildren(ViewFactory factory) {
                    return;
                }

                @Override
                public int getStartOffset() {
                    return getViewCount() > 0 ? getView(0).getStartOffset() : super
                            .getStartOffset();
                }

                @Override
                public int getEndOffset() {
                    int count = getViewCount();
                    return count > 0 ? getView(count - 1).getEndOffset() : super.getEndOffset();
                }
            };
        }

        @Override
        public String toString() {
            return "flow";
        }
    }

    protected static class FlowViewImplWithFactory extends FlowViewImpl {
        private final ViewFactory factory;

        public FlowViewImplWithFactory(final Element element, final int axis) {
            this(element, axis, new ChildrenFactory());
        }

        public FlowViewImplWithFactory(final Element element, final int axis,
                final ViewFactory factory) {
            super(element, axis);
            this.factory = factory;
        }

        @Override
        public ViewFactory getViewFactory() {
            return factory;
        }

        @Override
        public String toString() {
            return "theFlow";
        }
    }

    private Document doc;

    private Element root;

    private FlowView view;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.insertString(0, "line1\nline2\nline3", null);
        root = doc.getDefaultRootElement();
        view = new FlowViewImpl(root, View.Y_AXIS);
    }

    public void testSetParent() {
        assertNull(view.getParent());
        assertNull(view.layoutPool);
        assertEquals(0, view.getViewCount());
        assertNull(view.getViewFactory());
        final View parent = new BoxView(root, View.X_AXIS);
        view.setParent(parent);
        assertNotNull(view.getParent());
        assertEquals(0, view.getViewCount());
        assertNotNull(view.layoutPool);
        assertSame(view, view.layoutPool.getParent());
        assertSame(root, view.layoutPool.getElement());
        assertEquals(0, view.layoutPool.getViewCount());
    }

    public void testSetParentWithFactory() {
        view = new FlowViewImplWithFactory(root, View.Y_AXIS);
        assertNull(view.getParent());
        assertNull(view.layoutPool);
        assertEquals(0, view.getViewCount());
        assertNotNull(view.getViewFactory());
        final View parent = new BoxView(root, View.X_AXIS);
        view.setParent(parent);
        assertNotNull(view.getParent());
        assertEquals(0, view.getViewCount());
        assertNotNull(view.layoutPool);
        assertSame(view, view.layoutPool.getParent());
        assertSame(root, view.layoutPool.getElement());
        assertEquals(root.getElementCount(), view.layoutPool.getViewCount());
    }

    /**
     * Tests <code>getViewIndexAtPosition()</code> when views represent entire
     * elements.
     */
    public void testGetViewIndexAtPositionEntire() {
        assertEquals(0, view.getViewCount());
        final ViewFactory vf = new ChildrenFactory();
        final Element first = root.getElement(0);
        final Element second = root.getElement(1);
        final int middle = (first.getStartOffset() + first.getEndOffset()) / 2;
        View[] views = new View[] { vf.create(first), vf.create(second) };
        view.replace(0, 0, views);
        assertEquals(-1, view.getViewIndexAtPosition(-1));
        assertEquals(0, view.getViewIndexAtPosition(first.getStartOffset()));
        assertEquals(0, view.getViewIndexAtPosition(middle));
        assertEquals(1, view.getViewIndexAtPosition(first.getEndOffset()));
        assertEquals(1, view.getViewIndexAtPosition(second.getStartOffset()));
        assertEquals(1, view.getViewIndexAtPosition(second.getEndOffset() - 1));
        assertEquals(-1, view.getViewIndexAtPosition(second.getEndOffset()));
        assertEquals(-1, view.getViewIndexAtPosition(second.getEndOffset() + 2));
        assertNull(view.layoutPool);
    }

    /**
     * Tests <code>getViewIndexAtPosition()</code> when views represent portions
     * of elements.
     */
    public void testGetViewIndexAtPositionPartial() {
        assertEquals(0, view.getViewCount());
        final ViewFactory vf = new ChildrenFactory();
        final Element first = root.getElement(0);
        final Element second = root.getElement(1);
        final int middle = (first.getStartOffset() + first.getEndOffset()) / 2;
        View[] views = new View[] { new ElementPartView(first, first.getStartOffset(), middle),
                new ElementPartView(first, middle, first.getEndOffset()), vf.create(second) };
        view.replace(0, 0, views);
        assertEquals(-1, view.getViewIndexAtPosition(-1));
        assertEquals(0, view.getViewIndexAtPosition(first.getStartOffset()));
        assertEquals(0, view.getViewIndexAtPosition(middle - 1));
        assertEquals(1, view.getViewIndexAtPosition(middle));
        assertEquals(1, view.getViewIndexAtPosition(middle + 1));
        assertEquals(1, view.getViewIndexAtPosition(first.getEndOffset() - 1));
        assertEquals(2, view.getViewIndexAtPosition(first.getEndOffset()));
        assertEquals(2, view.getViewIndexAtPosition(second.getStartOffset()));
        assertEquals(2, view.getViewIndexAtPosition(second.getEndOffset() - 1));
        assertEquals(-1, view.getViewIndexAtPosition(second.getEndOffset()));
        assertEquals(-1, view.getViewIndexAtPosition(second.getEndOffset() + 2));
        assertNull(view.layoutPool);
    }

    public void testLoadChildren() {
        assertNull(view.layoutPool);
        assertNull(view.getViewFactory());
        view.loadChildren(new ChildrenFactory());
        assertNotNull(view.layoutPool);
        assertSame(view, view.layoutPool.getParent());
        assertSame(root, view.layoutPool.getElement());
        assertEquals(0, view.layoutPool.getViewCount());
    }

    public void testLoadChildrenWithFactory() {
        view = new FlowViewImplWithFactory(root, View.Y_AXIS);
        assertNull(view.layoutPool);
        assertNotNull(view.getViewFactory());
        view.loadChildren(new ChildrenFactory());
        assertNotNull(view.layoutPool);
        assertSame(view, view.layoutPool.getParent());
        assertSame(root, view.layoutPool.getElement());
        assertEquals(root.getElementCount(), view.layoutPool.getViewCount());
    }

    public void testLoadChildrenWithFactoryEmtpyPool() {
        view = new FlowViewImplWithFactory(root, View.Y_AXIS);
        assertNull(view.layoutPool);
        assertNotNull(view.getViewFactory());
        view.loadChildren(null);
        assertNotNull(view.layoutPool);
        view.layoutPool.removeAll();
        view.loadChildren(null);
        assertEquals(root.getElementCount(), view.layoutPool.getViewCount());
    }

    public void testLoadChildrenEmtpyPoolNullFactory() {
        view = new FlowViewImplWithFactory(root, View.Y_AXIS);
        assertNull(view.layoutPool);
        assertNotNull(view.getViewFactory());
        view.loadChildren(null);
        assertNotNull(view.layoutPool);
        view.layoutPool.removeAll();
        ((CompositeView) view.layoutPool).loadChildren(null);
        assertEquals(0, view.layoutPool.getViewCount());
    }

    public void testLoadChildrenStrategy() {
        final boolean[] called = new boolean[] { false };
        view = new FlowViewImplWithFactory(root, View.Y_AXIS);
        view.strategy = new FlowStrategy() {
            @Override
            public void insertUpdate(FlowView fv, DocumentEvent event, Rectangle alloc) {
                called[0] = true;
                assertSame(view, fv);
                assertNull(event);
                assertNull(alloc);
                super.insertUpdate(fv, event, alloc);
            }
        };
        view.loadChildren(null);
        assertTrue(called[0]);
    }

    public void testCalculateMinorAxisRequirements() {
        view.layoutPool = new PlainView(root) {
            @Override
            public float getPreferredSpan(int axis) {
                return axis == X_AXIS ? -13 : 13;
            }

            @Override
            public float getMinimumSpan(int axis) {
                return axis == X_AXIS ? -19 : 7;
            }

            @Override
            public float getMaximumSpan(int axis) {
                return axis == X_AXIS ? -7 : 19;
            }

            @Override
            public float getAlignment(int axis) {
                return axis == X_AXIS ? 0.312f : 0.213f;
            }
        };
        SizeRequirements xSR = view.calculateMinorAxisRequirements(View.X_AXIS, null);
        assertEquals(-19, xSR.minimum);
        assertEquals(-13, xSR.preferred);
        assertEquals(Integer.MAX_VALUE, xSR.maximum);
        assertEquals(0.5f, xSR.alignment, 1e-5f);
        SizeRequirements ySR = view.calculateMinorAxisRequirements(View.Y_AXIS, xSR);
        assertSame(xSR, ySR);
        assertEquals(7, xSR.minimum);
        assertEquals(13, xSR.preferred);
        assertEquals(Integer.MAX_VALUE, xSR.maximum);
        assertEquals(0.5f, xSR.alignment, 1e-5f);
    }

    public void testLayout() {
        final List<Integer> layoutChanges = new ArrayList<Integer>();
        final Marker prefMarker = new Marker();
        view = new FlowViewImplWithFactory(root, View.Y_AXIS) {
            @Override
            public void layoutChanged(int axis) {
                layoutChanges.add(new Integer(axis));
                super.layoutChanged(axis);
            }

            @Override
            public void preferenceChanged(View child, boolean width, boolean height) {
                assertNull(child);
                assertFalse(width);
                assertTrue(height);
                prefMarker.setOccurred();
                super.preferenceChanged(child, width, height);
            }
        };
        view.loadChildren(null);
        assertEquals(root.getElementCount(), view.layoutPool.getViewCount());
        assertEquals(0, view.getViewCount());
        layoutChanges.clear();
        assertFalse(view.isAllocationValid());
        assertEquals(Short.MAX_VALUE, view.layoutSpan);
        final boolean[] called = new boolean[] { false };
        final int width = 513;
        final int height = 137;
        view.strategy = new FlowStrategy() {
            @Override
            public void layout(FlowView fv) {
                assertSame(view, fv);
                super.layout(fv);
                called[0] = true;
            }
        };
        view.layout(width, height);
        assertEquals(width, view.layoutSpan);
        assertEquals(1, view.getViewCount());
        assertTrue(called[0]);
        assertTrue(view.isAllocationValid());
        assertEquals(2, layoutChanges.size());
        assertEquals(View.X_AXIS, layoutChanges.get(0).intValue());
        assertEquals(View.Y_AXIS, layoutChanges.get(1).intValue());
        layoutChanges.clear();
        called[0] = false;
        view.layout(width, height);
        assertFalse(called[0]);
        assertEquals(0, layoutChanges.size());
        view.layoutChanged(View.X_AXIS);
        layoutChanges.clear();
        assertFalse(view.isAllocationValid());
        view.layout(width, height);
        assertTrue(called[0]);
        assertEquals(0, layoutChanges.size());
        called[0] = false;
        view.layout(width, height - 1);
        assertFalse(called[0]);
        assertEquals(0, layoutChanges.size());
        view.layout(width - 1, height);
        assertEquals(width - 1, view.layoutSpan);
        assertTrue(called[0]);
        assertEquals(2, layoutChanges.size());
        assertEquals(View.X_AXIS, layoutChanges.get(0).intValue());
        assertEquals(View.Y_AXIS, layoutChanges.get(1).intValue());
        layoutChanges.clear();
        // Test if preferenceChanged() is called
        view.removeAll();
        int prefSpan = (int) view.getPreferredSpan(View.Y_AXIS);
        assertEquals(0, prefSpan);
        view.layout(width, height);
        assertTrue(view.getViewCount() != 0);
        assertEquals(getSpanY(), (int) view.getPreferredSpan(View.Y_AXIS));
        assertEquals(isHarmony(), prefMarker.isOccurred());
    }

    public void testFlowView() {
        assertSame(root, view.getElement());
        assertEquals(View.Y_AXIS, view.getAxis());
        assertEquals(Short.MAX_VALUE, view.layoutSpan);
        assertNull(view.layoutPool);
        assertNotNull(view.strategy);
        FlowView other = new FlowViewImpl(root, View.Y_AXIS);
        assertSame(view.strategy, other.strategy);
    }

    public void testGetFlowAxis() {
        assertEquals(View.Y_AXIS, view.getAxis());
        assertEquals(View.X_AXIS, view.getFlowAxis());
        view = new FlowViewImpl(root, View.X_AXIS);
        assertEquals(View.X_AXIS, view.getAxis());
        assertEquals(View.Y_AXIS, view.getFlowAxis());
        view = new FlowViewImpl(root, 10);
        assertEquals(10, view.getAxis());
        assertEquals(View.Y_AXIS, view.getFlowAxis());
        view.setAxis(View.Y_AXIS);
        assertEquals(View.X_AXIS, view.getFlowAxis());
    }

    public void testGetFlowStart() {
        assertEquals(0, view.getViewCount());
        assertEquals(0, view.getFlowStart(0));
        assertEquals(0, view.getFlowStart(1));
        assertEquals(0, view.getFlowStart(2));
        view.setInsets((short) 5, (short) 7, (short) 6, (short) 3);
        assertEquals(0, view.getFlowStart(0));
        assertEquals(0, view.getFlowStart(1));
        assertEquals(0, view.getFlowStart(2));
    }

    public void testGetFlowSpan() {
        assertEquals(0, view.getViewCount());
        assertEquals(Short.MAX_VALUE, view.layoutSpan);
        view.layoutSpan = -10;
        assertEquals(-10, view.getFlowSpan(0));
        assertEquals(-10, view.getFlowSpan(1));
        assertEquals(-10, view.getFlowSpan(2));
        view.setInsets((short) 5, (short) 7, (short) 6, (short) 3);
        assertEquals(-10, view.getFlowSpan(0));
        assertEquals(-10, view.getFlowSpan(1));
        assertEquals(-10, view.getFlowSpan(2));
    }

    public void testGetSpanNoRow() throws Exception {
        view = new FlowViewImplWithFactory(root, View.Y_AXIS);
        view.loadChildren(null);
        assertEquals(0, view.getViewCount());
        view.layoutPool.replace(1, view.layoutPool.getViewCount() - 1, null);
        assertEquals(1, view.layoutPool.getViewCount());
        final View child = view.layoutPool.getView(0);
        int childX = (int) child.getPreferredSpan(View.X_AXIS);
        int childY = (int) child.getPreferredSpan(View.Y_AXIS);
        assertEquals(childX, (int) child.getMinimumSpan(View.X_AXIS));
        assertEquals(childY, (int) child.getMinimumSpan(View.Y_AXIS));
        assertEquals(childX, (int) child.getMaximumSpan(View.X_AXIS));
        assertEquals(childY, (int) child.getMaximumSpan(View.Y_AXIS));
        assertEquals(0, (int) view.layoutPool.getMinimumSpan(View.X_AXIS));
        assertEquals(0, (int) view.layoutPool.getMinimumSpan(View.Y_AXIS));
        assertEquals(childX, (int) view.layoutPool.getPreferredSpan(View.X_AXIS));
        assertEquals(childY, (int) view.layoutPool.getPreferredSpan(View.Y_AXIS));
        assertEquals(childX, (int) view.layoutPool.getMaximumSpan(View.X_AXIS));
        assertEquals(childY, (int) view.layoutPool.getMaximumSpan(View.Y_AXIS));
        assertEquals(0, (int) view.getMinimumSpan(View.X_AXIS));
        assertEquals(0, (int) view.getMinimumSpan(View.Y_AXIS));
        assertEquals(childX, (int) view.getPreferredSpan(View.X_AXIS));
        assertEquals(0, (int) view.getPreferredSpan(View.Y_AXIS));
        assertEquals(Integer.MAX_VALUE, (int) view.getMaximumSpan(View.X_AXIS));
        assertEquals(0, (int) view.getMaximumSpan(View.Y_AXIS));
    }

    public void testGetSpanOneRowNoChildren() throws Exception {
        view = new FlowViewImplWithFactory(root, View.Y_AXIS);
        view.loadChildren(null);
        assertEquals(0, view.getViewCount());
        view.layoutPool.replace(1, view.layoutPool.getViewCount() - 1, null);
        assertEquals(1, view.layoutPool.getViewCount());
        final View row = view.createRow();
        view.append(row);
        final View child = view.layoutPool.getView(0);
        int childX = (int) child.getPreferredSpan(View.X_AXIS);
        int childY = (int) child.getPreferredSpan(View.Y_AXIS);
        assertEquals(childX, (int) child.getMinimumSpan(View.X_AXIS));
        assertEquals(childY, (int) child.getMinimumSpan(View.Y_AXIS));
        assertEquals(childX, (int) child.getMaximumSpan(View.X_AXIS));
        assertEquals(childY, (int) child.getMaximumSpan(View.Y_AXIS));
        assertEquals(0, (int) view.layoutPool.getMinimumSpan(View.X_AXIS));
        assertEquals(0, (int) view.layoutPool.getMinimumSpan(View.Y_AXIS));
        assertEquals(childX, (int) view.layoutPool.getPreferredSpan(View.X_AXIS));
        assertEquals(childY, (int) view.layoutPool.getPreferredSpan(View.Y_AXIS));
        assertEquals(childX, (int) view.layoutPool.getMaximumSpan(View.X_AXIS));
        assertEquals(childY, (int) view.layoutPool.getMaximumSpan(View.Y_AXIS));
        assertEquals(0, (int) view.getMinimumSpan(View.X_AXIS));
        assertEquals(0, (int) view.getMinimumSpan(View.Y_AXIS));
        assertEquals(childX, (int) view.getPreferredSpan(View.X_AXIS));
        assertEquals(0, (int) view.getPreferredSpan(View.Y_AXIS));
        assertEquals(Integer.MAX_VALUE, (int) view.getMaximumSpan(View.X_AXIS));
        assertEquals(Integer.MAX_VALUE, (int) view.getMaximumSpan(View.Y_AXIS));
    }

    public void testGetSpanOneRowOneChild() throws Exception {
        view = new FlowViewImplWithFactory(root, View.Y_AXIS);
        view.loadChildren(null);
        assertEquals(0, view.getViewCount());
        view.layoutPool.replace(1, view.layoutPool.getViewCount() - 1, null);
        assertEquals(1, view.layoutPool.getViewCount());
        final View row = view.createRow();
        view.append(row);
        final View child = view.layoutPool.getView(0);
        row.append(child);
        int childX = (int) child.getPreferredSpan(View.X_AXIS);
        int childY = (int) child.getPreferredSpan(View.Y_AXIS);
        assertEquals(childX, (int) child.getMinimumSpan(View.X_AXIS));
        assertEquals(childY, (int) child.getMinimumSpan(View.Y_AXIS));
        assertEquals(childX, (int) child.getMaximumSpan(View.X_AXIS));
        assertEquals(childY, (int) child.getMaximumSpan(View.Y_AXIS));
        assertEquals(0, (int) view.layoutPool.getMinimumSpan(View.X_AXIS));
        assertEquals(0, (int) view.layoutPool.getMinimumSpan(View.Y_AXIS));
        assertEquals(childX, (int) view.layoutPool.getPreferredSpan(View.X_AXIS));
        assertEquals(childY, (int) view.layoutPool.getPreferredSpan(View.Y_AXIS));
        assertEquals(childX, (int) view.layoutPool.getMaximumSpan(View.X_AXIS));
        assertEquals(childY, (int) view.layoutPool.getMaximumSpan(View.Y_AXIS));
        assertEquals(0, (int) view.getMinimumSpan(View.X_AXIS));
        assertEquals(childY, (int) view.getMinimumSpan(View.Y_AXIS));
        assertEquals(childX, (int) view.getPreferredSpan(View.X_AXIS));
        assertEquals(childY, (int) view.getPreferredSpan(View.Y_AXIS));
        assertEquals(Integer.MAX_VALUE, (int) view.getMaximumSpan(View.X_AXIS));
        assertEquals(Integer.MAX_VALUE, (int) view.getMaximumSpan(View.Y_AXIS));
    }

    public void testGetSpanNoRowFlexible() throws Exception {
        ChildrenFactory factory = new ChildrenFactory();
        factory.makeFlexible();
        view = new FlowViewImplWithFactory(root, View.Y_AXIS, factory);
        view.loadChildren(null);
        assertEquals(0, view.getViewCount());
        view.layoutPool.replace(1, view.layoutPool.getViewCount() - 1, null);
        assertEquals(1, view.layoutPool.getViewCount());
        final View child = view.layoutPool.getView(0);
        int childPrefX = (int) child.getPreferredSpan(View.X_AXIS);
        int childPrefY = (int) child.getPreferredSpan(View.Y_AXIS);
        assertEquals(0, (int) view.layoutPool.getMinimumSpan(View.X_AXIS));
        assertEquals(0, (int) view.layoutPool.getMinimumSpan(View.Y_AXIS));
        assertEquals(childPrefX, (int) view.layoutPool.getPreferredSpan(View.X_AXIS));
        assertEquals(childPrefY, (int) view.layoutPool.getPreferredSpan(View.Y_AXIS));
        assertEquals(childPrefX, (int) view.layoutPool.getMaximumSpan(View.X_AXIS));
        assertEquals(childPrefY, (int) view.layoutPool.getMaximumSpan(View.Y_AXIS));
        assertEquals(0, (int) view.getMinimumSpan(View.X_AXIS));
        assertEquals(0, (int) view.getMinimumSpan(View.Y_AXIS));
        assertEquals(childPrefX, (int) view.getPreferredSpan(View.X_AXIS));
        assertEquals(0, (int) view.getPreferredSpan(View.Y_AXIS));
        assertEquals(Integer.MAX_VALUE, (int) view.getMaximumSpan(View.X_AXIS));
        assertEquals(0, (int) view.getMaximumSpan(View.Y_AXIS));
    }

    public void testGetSpanOneRowNoChildrenFlexible() throws Exception {
        ChildrenFactory factory = new ChildrenFactory();
        factory.makeFlexible();
        view = new FlowViewImplWithFactory(root, View.Y_AXIS, factory);
        view.loadChildren(null);
        assertEquals(0, view.getViewCount());
        view.layoutPool.replace(1, view.layoutPool.getViewCount() - 1, null);
        assertEquals(1, view.layoutPool.getViewCount());
        final View row = view.createRow();
        view.append(row);
        final View child = view.layoutPool.getView(0);
        int childPrefX = (int) child.getPreferredSpan(View.X_AXIS);
        int childPrefY = (int) child.getPreferredSpan(View.Y_AXIS);
        assertEquals(0, (int) view.layoutPool.getMinimumSpan(View.X_AXIS));
        assertEquals(0, (int) view.layoutPool.getMinimumSpan(View.Y_AXIS));
        assertEquals(childPrefX, (int) view.layoutPool.getPreferredSpan(View.X_AXIS));
        assertEquals(childPrefY, (int) view.layoutPool.getPreferredSpan(View.Y_AXIS));
        assertEquals(childPrefX, (int) view.layoutPool.getMaximumSpan(View.X_AXIS));
        assertEquals(childPrefY, (int) view.layoutPool.getMaximumSpan(View.Y_AXIS));
        assertEquals(0, (int) view.getMinimumSpan(View.X_AXIS));
        assertEquals(0, (int) view.getMinimumSpan(View.Y_AXIS));
        assertEquals(childPrefX, (int) view.getPreferredSpan(View.X_AXIS));
        assertEquals(0, (int) view.getPreferredSpan(View.Y_AXIS));
        assertEquals(Integer.MAX_VALUE, (int) view.getMaximumSpan(View.X_AXIS));
        assertEquals(Integer.MAX_VALUE, (int) view.getMaximumSpan(View.Y_AXIS));
    }

    public void testGetSpanOneRowOneChildFlexible() throws Exception {
        ChildrenFactory factory = new ChildrenFactory();
        factory.makeFlexible();
        view = new FlowViewImplWithFactory(root, View.Y_AXIS, factory);
        view.loadChildren(null);
        assertEquals(0, view.getViewCount());
        view.layoutPool.replace(1, view.layoutPool.getViewCount() - 1, null);
        assertEquals(1, view.layoutPool.getViewCount());
        final View row = view.createRow();
        view.append(row);
        final View child = view.layoutPool.getView(0);
        row.append(child);
        int childMinY = (int) child.getMinimumSpan(View.Y_AXIS);
        int childPrefX = (int) child.getPreferredSpan(View.X_AXIS);
        int childPrefY = (int) child.getPreferredSpan(View.Y_AXIS);
        assertEquals(0, (int) view.layoutPool.getMinimumSpan(View.X_AXIS));
        assertEquals(0, (int) view.layoutPool.getMinimumSpan(View.Y_AXIS));
        assertEquals(childPrefX, (int) view.layoutPool.getPreferredSpan(View.X_AXIS));
        assertEquals(childPrefY, (int) view.layoutPool.getPreferredSpan(View.Y_AXIS));
        assertEquals(childPrefX, (int) view.layoutPool.getMaximumSpan(View.X_AXIS));
        assertEquals(childPrefY, (int) view.layoutPool.getMaximumSpan(View.Y_AXIS));
        assertEquals(0, (int) view.getMinimumSpan(View.X_AXIS));
        assertEquals(childMinY, (int) view.getMinimumSpan(View.Y_AXIS));
        assertEquals(childPrefX, (int) view.getPreferredSpan(View.X_AXIS));
        assertEquals(childPrefY, (int) view.getPreferredSpan(View.Y_AXIS));
        assertEquals(Integer.MAX_VALUE, (int) view.getMaximumSpan(View.X_AXIS));
        assertEquals(Integer.MAX_VALUE, (int) view.getMaximumSpan(View.Y_AXIS));
    }

    public void testGetAttributesLayoutPool() {
        view = new FlowViewImplWithFactory(root, View.Y_AXIS);
        view.loadChildren(null);
        assertSame(view.getAttributes(), view.layoutPool.getAttributes());
        view.layoutPool.setParent(null);
        assertNull(view.layoutPool.getAttributes());
    }

    private int getSpanY() {
        int span = 0;
        View row = view.getView(0);
        for (int i = 0; i < row.getViewCount(); i++) {
            span = Math.max(span, (int) row.getView(i).getPreferredSpan(View.Y_AXIS));
        }
        return span;
    }
}
