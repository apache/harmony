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
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.AbstractDocument.ElementEdit;
import javax.swing.text.FlowView.FlowStrategy;
import javax.swing.text.FlowViewTest.FlowViewImplWithFactory;
import javax.swing.text.GlyphView.GlyphPainter;
import javax.swing.text.GlyphViewTest.EmptyPainter;
import junit.framework.TestCase;

public class FlowView_FlowStrategyTest extends TestCase {
    private DefaultStyledDocument doc;

    private Element root;

    private Element p1;

    private Element p1L;

    private DefaultDocumentEvent event;

    private FlowView view;

    private View row;

    private FlowStrategy strategy;

    static final List<Object> viewsCreated = new ArrayList<Object>();

    private static final Rectangle alloc = new Rectangle(3, 7, 37, 141);

    private static final int X_AXIS = View.X_AXIS;

    private static final int Y_AXIS = View.Y_AXIS;

    private static boolean excellentBreak;

    protected static class TestStrategy extends FlowStrategy {
        @Override
        protected View createView(FlowView fv, int startOffset, int spanLeft, int rowIndex) {
            View result = super.createView(fv, startOffset, spanLeft, rowIndex);
            viewsCreated.add(result);
            return result;
        }

        @Override
        public String toString() {
            return "logStrategy";
        }
    }

    protected static class FixedPainter extends EmptyPainter {
        protected static final GlyphPainter PAINTER = new FixedPainter();

        private FixedPainter() {
        }

        @Override
        public int getBoundedPosition(GlyphView v, int startOffset, float x, float len) {
            int result = (int) len / PartView.CHAR_WIDTH + startOffset;
            return result <= v.getEndOffset() ? result : v.getEndOffset();
        }

        @Override
        public float getSpan(GlyphView v, int startOffset, int endOffset, TabExpander e, float x) {
            return PartView.CHAR_WIDTH * (endOffset - startOffset);
        }
    }

    protected static class PartView extends GlyphView {
        private static int count = 0;

        private int id = count++;

        static final int CHAR_WIDTH = 4;

        static final int CHAR_HEIGHT = 9;

        public PartView(Element element) {
            super(element);
            setGlyphPainter(FixedPainter.PAINTER);
        }

        @Override
        public float getPreferredSpan(int axis) {
            int result = axis == X_AXIS ? (int) super.getPreferredSpan(axis) : CHAR_HEIGHT;
            return result;
        }

        @Override
        public String toString() {
            final Element e = getElement();
            return "glyph(" + id + ", " + e.getName() + "[" + e.getStartOffset() + ", "
                    + e.getEndOffset() + "]" + ")  [" + getStartOffset() + ", "
                    + getEndOffset() + "]";
        }
    }

    protected static class PartFactory implements ViewFactory {
        public View create(Element element) {
            return new PartView(element);
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        doc.insertString(0, "this is the test text with some long words in p2\n"
        //                   |              |
                //  0123456789012345678901234567890123456789012345678
                //  0         1         2         3         4
                + "Long words: internationalization, localization", null);
        root = doc.getDefaultRootElement();
        p1 = root.getElement(0);
        // Break the first paragraph into several
        doc.writeLock();
        Element[] leaves = new Element[] { doc.createLeafElement(p1, null, 0, 17),
                doc.createLeafElement(p1, null, 17, 32),
                doc.createLeafElement(p1, null, 32, 49) };
        ((BranchElement) p1).replace(0, p1.getElementCount(), leaves);
        doc.writeUnlock();
        p1L = p1.getElement(0);
        // Initialize the view
        view = new FlowViewImplWithFactory(p1, Y_AXIS, new PartFactory());
        strategy = view.strategy = new TestStrategy();
        view.loadChildren(null);
        row = view.createRow();
        view.append(row);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        viewsCreated.clear();
    }

    public void testInsertUpdate() {
        event = doc.new DefaultDocumentEvent(4, 5, EventType.INSERT);
        event.addEdit(new ElementEdit(p1, 0, new Element[] { p1L }, new Element[] { p1L }));
        event.end();
        view.layout(alloc.width, alloc.height);
        assertTrue(view.isAllocationValid());
        view.strategy.insertUpdate(view, event, alloc);
        assertTrue(view.isAllocationValid());
    }

    public void testInsertUpdateNull() {
        view.layout(alloc.width, alloc.height);
        assertTrue(view.isAllocationValid());
        view.strategy.insertUpdate(view, null, null);
        assertFalse(view.isAllocationValid());
        assertFalse(view.isLayoutValid(X_AXIS));
        assertFalse(view.isLayoutValid(Y_AXIS));
    }

    public void testRemoveUpdate() {
        event = doc.new DefaultDocumentEvent(4, 5, EventType.REMOVE);
        event.addEdit(new ElementEdit(p1, 0, new Element[] { p1L }, new Element[] { p1L }));
        event.end();
        view.layout(alloc.width, alloc.height);
        assertTrue(view.isAllocationValid());
        view.strategy.removeUpdate(view, event, alloc);
        assertTrue(view.isAllocationValid());
    }

    public void testRemoveUpdateNull() {
        view.layout(alloc.width, alloc.height);
        assertTrue(view.isAllocationValid());
        view.strategy.removeUpdate(view, null, null);
        assertFalse(view.isAllocationValid());
        assertFalse(view.isLayoutValid(X_AXIS));
        assertFalse(view.isLayoutValid(Y_AXIS));
    }

    public void testChangedUpdate() {
        event = doc.new DefaultDocumentEvent(4, 5, EventType.CHANGE);
        event.addEdit(new ElementEdit(p1, 0, new Element[] { p1L }, new Element[] { p1L }));
        event.end();
        view.layout(alloc.width, alloc.height);
        assertTrue(view.isAllocationValid());
        view.strategy.changedUpdate(view, event, alloc);
        assertTrue(view.isAllocationValid());
    }

    public void testChangedUpdateNull() {
        view.layout(alloc.width, alloc.height);
        assertTrue(view.isAllocationValid());
        view.strategy.changedUpdate(view, null, null);
        assertFalse(view.isAllocationValid());
        assertFalse(view.isLayoutValid(X_AXIS));
        assertFalse(view.isLayoutValid(Y_AXIS));
    }

    /**
     * Tests <code>adjustRow</code> in ordinary "environment".
     */
    public void testAdjustRowOrdinary() {
        view.append(row);
        View[] views = new View[view.layoutPool.getViewCount() - 1];
        for (int i = 0; i < views.length; i++) {
            views[i] = view.layoutPool.getView(i);
        }
        row.replace(0, 0, views);
        view.layoutSpan = (int) (view.layoutPool.getView(0).getPreferredSpan(X_AXIS) + view.layoutPool
                .getView(1).getPreferredSpan(X_AXIS) / 2);
        assertEquals(2, row.getViewCount());
        strategy.adjustRow(view, 0, view.layoutSpan, 0);
        assertEquals(2, row.getViewCount());
        // The first physical view will be the same as the logical one (copied)
        assertSame(view.layoutPool.getView(0), row.getView(0));
        assertSame(row, view.layoutPool.getView(0).getParent());
        assertSame(row, row.getView(0).getParent());
        // The second is broken
        final View second = row.getView(1);
        assertEquals(view.layoutPool.getView(1).getStartOffset(), second.getStartOffset());
        assertEquals(22, row.getView(1).getEndOffset());
        assertSame(view.layoutPool, view.layoutPool.getView(1).getParent());
        assertSame(row, second.getParent());
        assertEquals(0, row.getStartOffset());
        assertEquals(22, row.getEndOffset());
    }

    /**
     * Tests <code>adjustRow</code> in a situation where the first and the third
     * views return <code>GoodBreakWeight</code> but the second returns
     * <code>ExcellentBreakWeight</code>. Here the second view will be broken
     * although it fits (the third doesn't fit the layoutSpan only).
     */
    public void testAdjustRowGoodExcellentGood() {
        View[] views = new View[view.layoutPool.getViewCount()];
        view.layoutSpan = 0;
        for (int i = 0; i < views.length; i++) {
            excellentBreak = i == 1;
            // Special view to get the required break weights easily
            views[i] = new PartView(view.layoutPool.getView(i).getElement()) {
                private final boolean excellent = excellentBreak;

                @Override
                public int getBreakWeight(int axis, float x, float len) {
                    return excellent ? ExcellentBreakWeight : GoodBreakWeight;
                }

                @Override
                public View breakView(int axis, int start, float x, float len) {
                    return createFragment(start, start + (getEndOffset() - getStartOffset())
                            / 2);
                }
            };
            int span = (int) views[i].getPreferredSpan(X_AXIS);
            if (i == views.length - 1) {
                span = span / 2;
            }
            view.layoutSpan += span;
        }
        row.replace(0, 0, views);
        // layoutSpan includes the first and the second views entirely and
        // only the half of the third.
        // The view will break on the second one, that's why the number of
        // children reduces from 3 to 2.
        assertEquals(3, row.getViewCount());
        strategy.adjustRow(view, 0, view.layoutSpan, 0);
        assertEquals(2, row.getViewCount());
        View child = row.getView(0);
        assertEquals(0, child.getStartOffset());
        assertEquals(17, child.getEndOffset());
        child = row.getView(1);
        assertEquals(17, child.getStartOffset());
        assertEquals(24, child.getEndOffset());
        assertEquals(0, row.getStartOffset());
        assertEquals(24, row.getEndOffset());
    }

    /**
     * Adjust the missing row. (Causes expected exception.)
     */
    public void testAdjustRowNoRow() {
        assertEquals(1, view.getViewCount());
        try {
            strategy.adjustRow(view, 1, view.layoutSpan, 0);
            fail("NullPointerException or ArrayIndexOutOfBoundsException " + "is expected");
        } catch (NullPointerException e) {
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    /**
     * Layout the missing row. (Causes expected exception.)
     */
    public void testLayoutRowNoRow() {
        assertEquals(1, view.getViewCount());
        try {
            strategy.layoutRow(view, 1, 0);
            fail("NullPointerException or ArrayIndexOutOfBoundsException " + "is expected");
        } catch (NullPointerException e) {
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    /**
     * Tests the situation where the layoutSpan is exhausted.
     */
    public void testLayoutRowSpanExhausted() {
        view.layoutSpan = (int) (view.layoutPool.getView(0).getPreferredSpan(X_AXIS) + view.layoutPool
                .getView(1).getPreferredSpan(X_AXIS) / 2);
        assertEquals(98, view.layoutSpan);
        int nextOffset = strategy.layoutRow(view, 0, 5);
        assertEquals(2, row.getViewCount());
        assertEquals(1, view.getViewCount());
        assertEquals(2, viewsCreated.size());
        final View child1 = row.getView(0);
        assertSame(viewsCreated.get(0), child1);
        assertEquals(5, child1.getStartOffset());
        assertEquals(view.layoutPool.getView(0).getEndOffset(), child1.getEndOffset());
        assertSame(viewsCreated.get(1), view.layoutPool.getView(1));
        final View child2 = row.getView(1);
        assertNotSame(viewsCreated.get(1), child2);
        assertEquals(child1.getEndOffset(), child2.getStartOffset());
        assertEquals(27, child2.getEndOffset());
        assertTrue(view.layoutSpan > child1.getPreferredSpan(X_AXIS)
                + child2.getPreferredSpan(X_AXIS));
        assertEquals(child2.getEndOffset(), nextOffset);
        assertEquals(row.getEndOffset(), nextOffset);
    }

    /**
     * Tests the situation where <code>createView</code> returns
     * <code>null</code>.
     */
    public void testLayoutRowNullReturned() {
        strategy = view.strategy = new TestStrategy() {
            private int count = 0;

            @Override
            protected View createView(FlowView fv, int startOffset, int spanLeft, int rowIndex) {
                return count++ == 2 ? null : super.createView(fv, startOffset, spanLeft,
                        rowIndex);
            }
        };
        assertEquals(Short.MAX_VALUE, view.layoutSpan);
        int nextOffset = strategy.layoutRow(view, 0, 5);
        assertEquals(2, row.getViewCount());
        assertEquals(1, view.getViewCount());
        assertEquals(2, viewsCreated.size());
        final View child1 = row.getView(0);
        assertSame(viewsCreated.get(0), child1);
        assertEquals(5, child1.getStartOffset());
        assertEquals(view.layoutPool.getView(0).getEndOffset(), child1.getEndOffset());
        assertSame(viewsCreated.get(1), view.layoutPool.getView(1));
        final View child2 = row.getView(1);
        assertSame(viewsCreated.get(1), child2);
        assertEquals(child1.getEndOffset(), child2.getStartOffset());
        assertEquals(view.layoutPool.getView(1).getEndOffset(), child2.getEndOffset());
        assertTrue(view.layoutSpan > child1.getPreferredSpan(X_AXIS)
                + child2.getPreferredSpan(X_AXIS));
        assertEquals(child2.getEndOffset(), nextOffset);
        assertEquals(row.getEndOffset(), nextOffset);
    }

    /**
     * Tests the situation where a view requests a forced break.
     */
    public void testLayoutRowForcedBreak() {
        strategy = view.strategy = new TestStrategy() {
            private int count = 0;

            @Override
            protected View createView(FlowView fv, int startOffset, int spanLeft, int rowIndex) {
                if (count++ == 1) {
                    Element e = fv.layoutPool.getView(count - 1).getElement();
                    return new PartView(e) {
                        private final int half = (getEndOffset() - getStartOffset()) / 2;
                        {
                            viewsCreated.add(this);
                        }

                        @Override
                        public int getBreakWeight(int axis, float x, float len) {
                            if (len > CHAR_WIDTH * half) {
                                return ForcedBreakWeight;
                            }
                            return super.getBreakWeight(axis, x, len);
                        }

                        @Override
                        public View breakView(int axis, int startOffset, float x, float len) {
                            if (len > CHAR_WIDTH * half) {
                                return createFragment(startOffset, startOffset + half);
                            }
                            return super.breakView(axis, startOffset, x, len);
                        }
                    };
                }
                return super.createView(fv, startOffset, spanLeft, rowIndex);
            }
        };
        assertEquals(Short.MAX_VALUE, view.layoutSpan);
        int nextOffset = strategy.layoutRow(view, 0, 5);
        assertEquals(2, row.getViewCount());
        assertEquals(1, view.getViewCount());
        assertEquals(2, viewsCreated.size());
        final View child1 = row.getView(0);
        assertSame(viewsCreated.get(0), child1);
        assertEquals(5, child1.getStartOffset());
        assertEquals(view.layoutPool.getView(0).getEndOffset(), child1.getEndOffset());
        assertNotSame(viewsCreated.get(1), view.layoutPool.getView(1));
        final View forced = (View) viewsCreated.get(1);
        assertEquals(view.layoutPool.getView(1).getStartOffset(), forced.getStartOffset());
        assertEquals(view.layoutPool.getView(1).getEndOffset(), forced.getEndOffset());
        assertNotSame(viewsCreated.get(1), row.getView(1));
        final View child2 = row.getView(1);
        assertEquals(child1.getEndOffset(), child2.getStartOffset());
        assertEquals((view.layoutPool.getView(1).getEndOffset() + view.layoutPool.getView(1)
                .getStartOffset()) / 2, child2.getEndOffset());
        assertTrue(view.layoutSpan > child1.getPreferredSpan(X_AXIS)
                + child2.getPreferredSpan(X_AXIS));
        assertEquals(child2.getEndOffset(), nextOffset);
        assertEquals(row.getEndOffset(), nextOffset);
    }

    /**
     * Tests how FlowView lays out the second paragraph (with very long words).
     */
    public void testLayout() {
        view = new FlowViewImplWithFactory(root.getElement(1), Y_AXIS, new PartFactory());
        strategy = view.strategy = new TestStrategy();
        view.loadChildren(null);
        view.layoutSpan = PartView.CHAR_WIDTH * 13;
        strategy.layout(view);
        String[] text = new String[] { "Long words: ", "international", "ization, ",
                "localization\n" };
        assertEquals(text.length, view.getViewCount());
        for (int i = 0; i < view.getViewCount(); i++) {
            final View row = view.getView(i);
            assertEquals("i = " + i + " " + row, 1, row.getViewCount());
            assertEquals(text[i], getText(row.getView(0)));
        }
    }

    /**
     * Tests that "old" children contained are removed from
     * the <code>FlowView</code> before layout is performed.
     */
    public void testLayoutRemoved() {
        View[] dummy = new View[] { new PlainView(p1L), new PlainView(p1) };
        view.replace(0, view.getViewCount(), dummy);
        assertEquals(2, view.getViewCount());
        view.layoutSpan = Integer.MAX_VALUE;
        view.strategy.layout(view);
        assertEquals(1, view.getViewCount());
    }

    public void testGetLogicalView() {
        assertSame(view.layoutPool, view.strategy.getLogicalView(view));
    }

    public void testCreateView() {
        assertSame(view.layoutPool.getView(0), strategy.createView(view, 0, view.layoutSpan, 0));
        assertSame(view.layoutPool.getView(0), strategy
                .createView(view, 0, view.layoutSpan, -1));
        assertSame(view.layoutPool.getView(0), strategy.createView(view, 0, -1, 0));
        View created = strategy.createView(view, 3, view.layoutSpan, 0);
        View logical = view.layoutPool.getView(0);
        assertNotSame(logical, created);
        assertEquals(3, created.getStartOffset());
        assertEquals(logical.getEndOffset(), created.getEndOffset());
        created = strategy.createView(view, 3, 2, 0);
        assertEquals(3, created.getStartOffset());
        assertEquals(logical.getEndOffset(), created.getEndOffset());
        created = strategy.createView(view, logical.getEndOffset(), 2, 0);
        logical = view.layoutPool.getView(1);
        assertSame(logical, created);
        created = strategy.createView(view, logical.getStartOffset() + 2, 2, 0);
        assertNotSame(logical, created);
        assertEquals(logical.getStartOffset() + 2, created.getStartOffset());
        assertEquals(logical.getEndOffset(), created.getEndOffset());
        logical = view.layoutPool.getView(view.layoutPool.getViewCount() - 1);
        created = strategy.createView(view, logical.getEndOffset() - 1, 2, 0);
        assertEquals(logical.getEndOffset() - 1, created.getStartOffset());
        assertEquals(logical.getEndOffset(), created.getEndOffset());
        try {
            assertNull(strategy.createView(view, logical.getEndOffset(), 2, 0));
        } catch (ArrayIndexOutOfBoundsException e) {
            if (BasicSwingTestCase.isHarmony()) {
                fail("ArrayIndexOutOfBoundsException is unexpected");
            }
        }
    }

    private static String getText(View v) {
        return ((GlyphView) v).getText(v.getStartOffset(), v.getEndOffset()).toString();
    }
}
