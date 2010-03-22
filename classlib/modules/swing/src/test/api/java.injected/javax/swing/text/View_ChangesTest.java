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
import java.awt.Shape;
import java.util.ArrayList;
import javax.swing.BasicSwingTestCase;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.CompositeView_ModelViewTest.ChildView;
import javax.swing.text.CompositeView_ModelViewTest.WithChildrenView;
import javax.swing.text.ViewTest.DisAbstractedView;
import javax.swing.text.ViewTestHelpers.ElementPartView;
import javax.swing.undo.UndoableEdit;
import junit.framework.TestCase;

/**
 * Tests changes-related methods of <code>View</code> class.
 *
 */
public class View_ChangesTest extends TestCase {
    /**
     * Class overriding some methods to test View behaviour in respect
     * to {insert,remove,changed}Update.
     */
    private class ChangeView extends WithChildrenView {
        /**
         * The last allocation returned from
         * <code>getChildAllocation</code>.
         */
        Shape childAllocation = null;

        public ChangeView(final Element element) {
            super(element);
            loadChildren(viewFactory);
            viewsCreatedElements.clear();
            replaceViews = null;
        }

        @Override
        public Shape getChildAllocation(final int index, final Shape shape) {
            if (!hasChildren) {
                fail("getChildAllocation is not supposed to be "
                        + "called when there are no children");
            }
            return childAllocation = super.getChildAllocation(index, shape);
        }

        /**
         * Returns a child from <code>children</code> or forwards
         * to <code>super</code> depending on state of flag
         * <code>hasChildren</code>.
         */
        @Override
        public View getView(final int index) {
            if (hasChildren) {
                return super.getView(index);
            }
            return null;
        }

        /**
         * Returns <code>children.length</code> or forwards to
         * <code>super</code> depending on state of flag
         * <code>hasChildren</code>.
         */
        @Override
        public int getViewCount() {
            if (hasChildren) {
                return super.getViewCount();
            }
            return 0;
        }

        @Override
        public void replace(final int index, final int length, final View[] views) {
            replaceIndex = index;
            replaceLength = length;
            replaceViews = views;
            super.replace(index, length, views);
        }

        /**
         * Just a security check: <code>setParent</code> isn't called.
         */
        @Override
        public void setParent(final View parent) {
            super.setParent(parent);
            fail("setParent is not supposed to be called");
        }

        /**
         * Check <code>forwardUpdate</code> parameters which is
         * called from <code>insertUpdate</code>,
         * <code>removeUpdate</code>, or <code>changedUpdate</code>.
         */
        @Override
        protected void forwardUpdate(final ElementChange change, final DocumentEvent event,
                final Shape shape, final ViewFactory factory) {
            forwardUpdateCalled = true;
            if (updateChildrenReturn) {
                assertSame(event.getChange(root), change);
            } else {
                assertNull(change);
            }
            assertSame(docEvent, event);
            assertSame(rect, shape);
            assertSame(viewFactory, factory);
            super.forwardUpdate(change, event, shape, factory);
        }

        /**
         * Check <code>forwardUpdateToView</code> parameters which is
         * called from <code>insertUpdate</code>,
         * <code>removeUpdate</code>, or <code>changedUpdate</code>.
         */
        @Override
        protected void forwardUpdateToView(final View view, final DocumentEvent event,
                final Shape shape, final ViewFactory factory) {
            forwardUpdateToViewCalled = true;
            viewsForwardedTo.add(view);
            assertSame(docEvent, event);
            assertSame(childAllocation, shape);
            assertSame(viewFactory, factory);
            super.forwardUpdateToView(view, event, shape, factory);
        }

        /**
         * Check <code>forwardUpdate</code> parameters which is
         * called from <code>insertUpdate</code>,
         * <code>removeUpdate</code>, or <code>changedUpdate</code>.
         */
        @Override
        protected boolean updateChildren(final ElementChange change, final DocumentEvent event,
                final ViewFactory factory) {
            updateChildrenCalled = true;
            assertSame(event.getChange(root), change);
            assertSame(docEvent, event);
            assertSame(viewFactory, factory);
            assertTrue(super.updateChildren(change, event, factory));
            assertFalse(forwardUpdateCalled);
            assertFalse(updateLayoutCalled);
            return updateChildrenReturn;
        }

        /**
         * Check <code>updateLayout</code> parameters which is called
         * from <code>insertUpdate</code>, <code>removeUpdate</code>,
         * or <code>changedUpdate</code>.
         */
        @Override
        protected void updateLayout(final ElementChange change, final DocumentEvent event,
                final Shape shape) {
            updateLayoutCalled = true;
            if (updateChildrenReturn) {
                assertSame(event.getChange(root), change);
            } else {
                assertNull(change);
            }
            assertSame(docEvent, event);
            assertSame(rect, shape);
            super.updateLayout(change, event, shape);
        }
    }

    /**
     * View allocation (Shape parameter).
     */
    private static final Rectangle rect = new Rectangle(20, 20);

    private Document doc;

    /**
     * The event used to test the functionality.
     */
    private DocumentEvent docEvent;

    private boolean forwardUpdateCalled;

    private boolean forwardUpdateToViewCalled;

    /**
     * Flag which controls whether anonymous test-view has children or not.
     */
    private boolean hasChildren;

    private Element line;

    /**
     * Index of the first child where change happens (in call to replace).
     */
    private int replaceIndex;

    /**
     * Number of elements to remove (in call to replace).
     */
    private int replaceLength;

    /**
     * Views to add (in call to replace).
     */
    private View[] replaceViews;

    /**
     * The root element where changes in document are tracked.
     */
    private Element root;

    private boolean updateChildrenCalled;

    /**
     * Return value from updateChildren for anonymous test-view.
     */
    private boolean updateChildrenReturn;

    private boolean updateLayoutCalled;

    private View view;

    /**
     * The view factory used in tests.
     */
    private ViewFactory viewFactory;

    /**
     * List of elements for which new views were created.
     */
    private ArrayList<Element> viewsCreatedElements = new ArrayList<Element>();

    /**
     * List of views for which forwardUpdateToView was called.
     */
    private ArrayList<View> viewsForwardedTo = new ArrayList<View>();

    /**
     * Creates document event with type of <code>CHANGE</code>.
     */
    public void createChangeEvent() throws BadLocationException {
        doc.insertString(doc.getLength(), "one\ntwo\n", null);
        view.removeAll();
        ((CompositeView) view).loadChildren(viewFactory);
        viewsCreatedElements.clear();
        replaceViews = null;
        ElementChange change = docEvent.getChange(doc.getDefaultRootElement());
        docEvent = ((AbstractDocument) doc).new DefaultDocumentEvent(docEvent.getLength(),
                docEvent.getOffset(), EventType.CHANGE);
        ((AbstractDocument.DefaultDocumentEvent) docEvent).addEdit((UndoableEdit) change);
    }

    /**
     * The view has <i>no</i> children, and <code>updateChildren</code>
     * is <i>not</i> called as well as other methods involved.
     */
    public void testChangedUpdate01() throws BadLocationException {
        createChangeEvent();
        hasChildren = false;
        assertEquals(0, view.getViewCount());
        view.changedUpdate(docEvent, rect, viewFactory);
        assertFalse(updateChildrenCalled);
        assertFalse(forwardUpdateCalled);
        assertFalse(forwardUpdateToViewCalled);
        assertFalse(updateLayoutCalled);
    }

    /**
     * The view has children and <code>updateChildren</code> returns
     * <code>false</code>.
     */
    public void testChangedUpdate02() throws BadLocationException {
        hasChildren = true;
        createChangeEvent();
        updateChildrenReturn = false;
        assertEquals(4, view.getViewCount());
        view.changedUpdate(docEvent, rect, viewFactory);
        assertTrue(updateChildrenCalled);
        checkUpdatedChildren(4 + 2, 1);
        assertTrue(forwardUpdateCalled);
        assertTrue(forwardUpdateToViewCalled);
        assertEquals(3, viewsForwardedTo.size()); // to all children
        for (int i = 0; i < viewsForwardedTo.size(); i++) {
            assertSame("@ " + i, view.getView(i + 1), viewsForwardedTo.get(i));
        }
        assertTrue(updateLayoutCalled);
    }

    /**
     * The view has children and <code>updateChildren</code> returns
     * <code>true</code>.
     */
    public void testChangedUpdate03() throws BadLocationException {
        hasChildren = true;
        createChangeEvent();
        assertEquals(1, docEvent.getChange(root).getIndex());
        updateChildrenReturn = true;
        assertEquals(4, view.getViewCount());
        view.changedUpdate(docEvent, rect, viewFactory);
        assertTrue(updateChildrenCalled);
        checkUpdatedChildren(4 + 2, 1);
        assertTrue(forwardUpdateCalled);
        assertFalse(forwardUpdateToViewCalled);
        assertTrue(updateLayoutCalled);
    }

    /**
     * As if attributes are changed in the range 7-18:
     *    the second paragraph (6-15), and
     *    the third one (15, 19).
     * <code>updateChilren</code> returns <code>true</code>
     * (child views represent entire elements).
     */
    public void testChangedUpdate04() throws BadLocationException {
        hasChildren = true;
        changeDocument();
        updateChildrenReturn = true;
        Element prevLastLine = root.getElement(root.getElementCount() - 2);
        docEvent = ((AbstractDocument) doc).new DefaultDocumentEvent(line.getStartOffset() + 1,
                prevLastLine.getEndOffset() - 2 - line.getStartOffset(), EventType.CHANGE);
        view.changedUpdate(docEvent, rect, viewFactory);
        assertFalse(updateChildrenCalled);
        assertTrue(forwardUpdateCalled);
        assertTrue(forwardUpdateToViewCalled);
        assertEquals(2, viewsForwardedTo.size());
        for (int i = 0; i < viewsForwardedTo.size(); i++) {
            assertSame("@ " + i, view.getView(i + 1), viewsForwardedTo.get(i));
        }
        assertTrue(updateLayoutCalled);
    }

    /**
     * As if attributes are changed in the range 7-18:
     *    the second paragraph (6-15), and
     *    the third one (15, 19).
     * <code>updateChilren</code> returns <code>false</code>
     * (child views represent entire elements).
     */
    public void testChangedUpdate05() throws BadLocationException {
        hasChildren = true;
        changeDocument();
        updateChildrenReturn = false;
        Element prevLastLine = root.getElement(root.getElementCount() - 2);
        docEvent = ((AbstractDocument) doc).new DefaultDocumentEvent(line.getStartOffset() + 1,
                prevLastLine.getEndOffset() - 2 - line.getStartOffset(), EventType.CHANGE);
        view.changedUpdate(docEvent, rect, viewFactory);
        assertFalse(updateChildrenCalled);
        assertTrue(forwardUpdateCalled);
        assertTrue(forwardUpdateToViewCalled);
        assertEquals(2, viewsForwardedTo.size());
        for (int i = 0; i < viewsForwardedTo.size(); i++) {
            assertSame("@ " + i, view.getView(i + 1), viewsForwardedTo.get(i));
        }
        assertTrue(updateLayoutCalled);
    }

    /**
     * Tests <code>forwardUpdateToView</code> whether it calls
     * {insert,remove,changed}Update depending on event type.
     */
    public void testForwardUpdateToView() {
        // Class to store which function is called
        class Params {
            boolean change = false;

            boolean insert = false;

            boolean remove = false;
        }
        final Params params = new Params();
        view = new DisAbstractedView(line);
        View child = new DisAbstractedView(root.getElement(0)) {
            @Override
            public void changedUpdate(final DocumentEvent event, final Shape shape,
                    final ViewFactory factory) {
                params.change = true;
            }

            @Override
            public void insertUpdate(final DocumentEvent event, final Shape shape,
                    final ViewFactory factory) {
                params.insert = true;
            }

            @Override
            public void removeUpdate(final DocumentEvent event, final Shape shape,
                    final ViewFactory factory) {
                params.remove = true;
            }
        };
        view.forwardUpdateToView(child, ((AbstractDocument) doc).new DefaultDocumentEvent(0, 0,
                EventType.INSERT), rect, viewFactory);
        assertTrue(params.insert);
        params.insert = false;
        assertFalse(params.remove);
        params.remove = false;
        assertFalse(params.change);
        params.change = false;
        view.forwardUpdateToView(child, ((AbstractDocument) doc).new DefaultDocumentEvent(0, 0,
                EventType.REMOVE), rect, viewFactory);
        assertFalse(params.insert);
        params.insert = false;
        assertTrue(params.remove);
        params.remove = false;
        assertFalse(params.change);
        params.change = false;
        view.forwardUpdateToView(child, ((AbstractDocument) doc).new DefaultDocumentEvent(0, 0,
                EventType.CHANGE), rect, viewFactory);
        assertFalse(params.insert);
        params.insert = false;
        assertFalse(params.remove);
        params.remove = false;
        assertTrue(params.change);
        params.change = false;
        view.forwardUpdateToView(child, ((AbstractDocument) doc).new DefaultDocumentEvent(0, 0,
                null), rect, viewFactory);
        assertFalse(params.insert);
        params.insert = false;
        assertFalse(params.remove);
        params.remove = false;
        if (BasicSwingTestCase.isHarmony()) {
            assertFalse(params.change);
            params.change = false;
        } else {
            assertTrue(params.change);
            params.change = false;
        }
    }

    /**
     * The view has <i>no</i> children, and <code>updateChildren</code>
     * is <i>not</i> called.
     */
    public void testInsertUpdate01() throws BadLocationException {
        doc.insertString(line.getStartOffset() + 2, "one\ntwo\n", null);
        hasChildren = false;
        assertEquals(0, view.getViewCount());
        view.insertUpdate(docEvent, rect, viewFactory);
        assertFalse(updateChildrenCalled);
        assertFalse(forwardUpdateCalled);
        assertFalse(forwardUpdateToViewCalled);
        assertFalse(updateLayoutCalled);
    }

    /**
     * The view has children and <code>updateChildren</code> returns
     * <code>false</code>. (Views may represent parts of an Element.)
     */
    public void testInsertUpdate02() throws BadLocationException {
        doc.insertString(line.getStartOffset() + 2, "one\ntwo\n", null);
        hasChildren = true;
        updateChildrenReturn = false;
        assertEquals(2, view.getViewCount());
        view.insertUpdate(docEvent, rect, viewFactory);
        assertTrue(updateChildrenCalled);
        checkUpdatedChildren(2 + 2, 1);
        assertTrue(forwardUpdateCalled);
        assertTrue(forwardUpdateToViewCalled);
        assertEquals(4 - 1, viewsForwardedTo.size()); // first elem not affected
        for (int i = 0; i < viewsForwardedTo.size(); i++) {
            assertSame("@ " + i, view.getView(i + 1), viewsForwardedTo.get(i));
        }
        assertTrue(updateLayoutCalled);
    }

    /**
     * The view has children and <code>updateChildren</code> returns
     * <code>true</code>. (Views represent entire Elements.)
     */
    public void testInsertUpdate03() throws BadLocationException {
        doc.insertString(line.getStartOffset() + 2, "one\ntwo\n", null);
        hasChildren = true;
        updateChildrenReturn = true;
        assertEquals(2, view.getViewCount());
        view.insertUpdate(docEvent, rect, viewFactory);
        assertTrue(updateChildrenCalled);
        checkUpdatedChildren(2 + 2, 1);
        assertTrue(forwardUpdateCalled);
        assertFalse(forwardUpdateToViewCalled);
        assertTrue(updateLayoutCalled);
    }

    /**
     * Insert text so that structure changes occur at index of 2, while
     * <code>updateChildren</code> returns <code>true</code>. The result
     * is that changes must be forwarded to the first two view children.
     */
    public void testInsertUpdate04() throws BadLocationException {
        hasChildren = true;
        changeDocument();
        // Event method will be tested upon
        doc.insertString(doc.getLength(), "insert4", null);
        assertEquals(2, docEvent.getChange(root).getIndex());
        updateChildrenReturn = true;
        assertEquals(4, view.getViewCount());
        view.insertUpdate(docEvent, rect, viewFactory);
        assertTrue(updateChildrenCalled);
        checkUpdatedChildren(4 + 0, 2);
        assertTrue(forwardUpdateCalled);
        assertFalse(forwardUpdateToViewCalled);
        assertTrue(updateLayoutCalled);
    }

    /**
     * No structural changes occurred to the <code>root</code> element.
     * <code>updateChildren</code> must not be called in this case.
     */
    public void testInsertUpdate05() throws BadLocationException {
        doc.insertString(line.getStartOffset() + 2, "one", null);
        // This should not cause any line map restructure
        assertNull(docEvent.getChange(root));
        hasChildren = true;
        updateChildrenReturn = true;
        assertEquals(2, view.getViewCount());
        view.insertUpdate(docEvent, rect, viewFactory);
        assertFalse(updateChildrenCalled);
        assertTrue(forwardUpdateCalled);
        assertTrue(forwardUpdateToViewCalled);
        assertEquals(1, viewsForwardedTo.size());
        assertSame(view.getView(1), viewsForwardedTo.get(0));
        assertTrue(updateLayoutCalled);
    }

    /**
     * <code>viewFactory</code> parameter is <code>null</code>.
     */
    public void testInsertUpdate06() throws BadLocationException {
        doc.insertString(line.getStartOffset() + 2, "one\ntwo\n", null);
        hasChildren = true;
        updateChildrenReturn = true;
        assertEquals(2, view.getViewCount());
        try {
            view.insertUpdate(docEvent, rect, viewFactory = null);
            // So we should not check for this invalid parameter
            // (viewFactory == null)
            fail("Calling insertUpdate with null factory must result " + " in exception");
        } catch (NullPointerException e) {
        }
        assertTrue(updateChildrenCalled);
        // The exception must have occurred in updateChildren
        assertFalse(forwardUpdateCalled);
        assertFalse(forwardUpdateToViewCalled);
        assertFalse(updateLayoutCalled);
    }

    /**
     * <code>updateChildren</code> returns <code>true</code>.
     * (Views represent entire elements.)
     */
    public void testInsertUpdate07() throws BadLocationException {
        hasChildren = true;
        changeDocument();
        doc.insertString(2, "^^^\n", null);
        assertEquals(0, docEvent.getChange(root).getIndex());
        updateChildrenReturn = true;
        assertEquals(4, view.getViewCount());
        view.insertUpdate(docEvent, rect, viewFactory);
        assertTrue(updateChildrenCalled);
        checkUpdatedChildren(4 + 1, 1);
        assertTrue(forwardUpdateCalled);
        assertFalse(forwardUpdateToViewCalled);
        assertTrue(updateLayoutCalled);
    }

    /**
     * <code>updateChildren</code> returns <code>false</code>.
     * (Views represent partial elements.)
     */
    public void testInsertUpdate08() throws BadLocationException {
        hasChildren = true;
        changeDocument();
        doc.insertString(2, "^^^\n", null);
        assertEquals(0, docEvent.getChange(root).getIndex());
        updateChildrenReturn = false;
        assertEquals(4, view.getViewCount());
        view.insertUpdate(docEvent, rect, viewFactory);
        assertTrue(updateChildrenCalled);
        checkUpdatedChildren(4 + 1, 1);
        assertTrue(forwardUpdateCalled);
        assertTrue(forwardUpdateToViewCalled);
        assertEquals(2, viewsForwardedTo.size());
        for (int i = 0; i < viewsForwardedTo.size(); i++) {
            assertSame("@ " + i, view.getView(i), viewsForwardedTo.get(i));
        }
        assertTrue(updateLayoutCalled);
    }

    /**
     * In this test view representing <code>line</code> is replaced with two
     * view which represent parts of the <code>line</code> Element.
     * <p>
     * <code>updateChildren</code> returns <code>true</code>, i.e. it is
     * considered a view represents an entire element.
     */
    public void testInsertUpdate09() throws BadLocationException {
        createPartialViews();
        updateChildrenReturn = true;
        view.insertUpdate(docEvent, rect, viewFactory);
        assertFalse(updateChildrenCalled);
        assertTrue(forwardUpdateCalled);
        assertTrue(forwardUpdateToViewCalled);
        assertEquals(BasicSwingTestCase.isHarmony() ? 2 : 1, viewsForwardedTo.size());
        for (int i = 0; i < viewsForwardedTo.size(); i++) {
            assertSame("@ " + i, view.getView(i + 1), viewsForwardedTo.get(i));
            assertTrue("@ " + i, view.getView(i + 1) instanceof ElementPartView);
        }
        assertTrue(updateLayoutCalled);
    }

    /**
     * In this test view representing <code>line</code> is replaced with two
     * view which represent parts of the <code>line</code> Element.
     * (Same as in <code>testInsertUpdate09</code> except for the below).
     * <p>
     * <code>updateChildren</code> returns <code>false</code>, i.e. it is
     * considered a view may represent a portion of element.
     */
    public void testInsertUpdate10() throws BadLocationException {
        createPartialViews();
        updateChildrenReturn = false;
        view.insertUpdate(docEvent, rect, viewFactory);
        assertFalse(updateChildrenCalled);
        assertTrue(forwardUpdateCalled);
        assertTrue(forwardUpdateToViewCalled);
        assertEquals(BasicSwingTestCase.isHarmony() ? 2 : 1, viewsForwardedTo.size());
        for (int i = 0; i < viewsForwardedTo.size(); i++) {
            assertSame("@ " + i, view.getView(i + 1), viewsForwardedTo.get(i));
            assertTrue("@ " + i, view.getView(i + 1) instanceof ElementPartView);
        }
        assertTrue(updateLayoutCalled);
    }

    private void createPartialViews() throws BadLocationException {
        hasChildren = true;
        changeDocument();
        final int offset = (line.getStartOffset() + line.getEndOffset()) / 2;
        doc.insertString(offset, "^^^^", null);
        View[] parts = new View[2];
        parts[0] = new ElementPartView(line, line.getStartOffset(), offset + 2);
        parts[1] = new ElementPartView(line, offset + 2, line.getEndOffset());
        view.replace(1, 1, parts);
    }

    /**
     * The view has <i>no</i> children, and <code>updateChildren</code>
     * is <i>not</i> called as well as other methods involved.
     */
    public void testRemoveUpdate01() throws BadLocationException {
        changeDocument();
        doc.remove(line.getStartOffset(), 9);
        hasChildren = false;
        assertEquals(0, view.getViewCount());
        view.removeUpdate(docEvent, rect, viewFactory);
        assertFalse(updateChildrenCalled);
        assertFalse(forwardUpdateCalled);
        assertFalse(forwardUpdateToViewCalled);
        assertFalse(updateLayoutCalled);
    }

    /**
     * The view has children and <code>updateChildren</code> returns
     * <code>false</code>.
     * <p>
     * Exactly one element is removed.
     */
    public void testRemoveUpdate02() throws BadLocationException {
        hasChildren = true;
        changeDocument();
        doc.remove(line.getStartOffset(), 9);
        updateChildrenReturn = false;
        assertEquals(4, view.getViewCount());
        view.removeUpdate(docEvent, rect, viewFactory);
        assertTrue(updateChildrenCalled);
        checkUpdatedChildren(4 - 1, 2);
        assertTrue(forwardUpdateCalled);
        assertTrue(forwardUpdateToViewCalled);
        assertEquals(2, viewsForwardedTo.size());
        assertSame(view.getView(0), viewsForwardedTo.get(0));
        assertSame(view.getView(1), viewsForwardedTo.get(1));
        assertTrue(updateLayoutCalled);
    }

    /**
     * The view has children and <code>updateChildren</code> returns
     * <code>true</code>.
     * <p>
     * Exactly one element is removed.
     */
    public void testRemoveUpdate03() throws BadLocationException {
        hasChildren = true;
        changeDocument();
        doc.remove(line.getStartOffset(), 9);
        updateChildrenReturn = true;
        assertEquals(4, view.getViewCount());
        view.removeUpdate(docEvent, rect, viewFactory);
        assertTrue(updateChildrenCalled);
        checkUpdatedChildren(4 - 1, 2);
        assertTrue(forwardUpdateCalled);
        assertTrue(forwardUpdateToViewCalled);
        assertEquals(1, viewsForwardedTo.size());
        assertSame(view.getView(0), viewsForwardedTo.get(0));
        assertTrue(updateLayoutCalled);
    }

    /**
     * The view has children and <code>updateChildren</code> returns
     * <code>true</code>.
     * <p>
     * Text removed is within one element.
     */
    public void testRemoveUpdate04() throws BadLocationException {
        hasChildren = true;
        changeDocument();
        doc.remove(line.getStartOffset() + 1, 2);
        updateChildrenReturn = true;
        assertEquals(4, view.getViewCount());
        view.removeUpdate(docEvent, rect, viewFactory);
        assertFalse(updateChildrenCalled);
        assertTrue(forwardUpdateCalled);
        assertTrue(forwardUpdateToViewCalled);
        assertEquals(1, viewsForwardedTo.size());
        assertSame(view.getView(1), viewsForwardedTo.get(0));
        assertTrue(updateLayoutCalled);
    }

    /**
     * The view has children and <code>updateChildren</code> returns
     * <code>true</code>.
     * <p>
     * New line character is removed.
     */
    public void testRemoveUpdate05() throws BadLocationException {
        hasChildren = true;
        changeDocument();
        doc.remove(line.getEndOffset() - 1, 1);
        updateChildrenReturn = true;
        assertEquals(4, view.getViewCount());
        view.removeUpdate(docEvent, rect, viewFactory);
        assertTrue(updateChildrenCalled);
        checkUpdatedChildren(4 - 1, 2);
        assertTrue(forwardUpdateCalled);
        assertFalse(forwardUpdateToViewCalled);
        //        assertEquals(1, viewsForwardedTo.size());
        //        assertEquals(view.getView(1), viewsForwardedTo.get(0));
        assertTrue(updateLayoutCalled);
    }

    /**
     * The view has children and <code>updateChildren</code> returns
     * <code>false</code>.
     * <p>
     * New line character is removed.
     */
    public void testRemoveUpdate06() throws BadLocationException {
        hasChildren = true;
        changeDocument();
        doc.remove(line.getEndOffset() - 1, 1);
        updateChildrenReturn = false;
        assertEquals(4, view.getViewCount());
        view.removeUpdate(docEvent, rect, viewFactory);
        assertTrue(updateChildrenCalled);
        checkUpdatedChildren(4 - 1, 2);
        assertTrue(forwardUpdateCalled);
        assertTrue(forwardUpdateToViewCalled);
        assertEquals(1, viewsForwardedTo.size());
        assertSame(view.getView(1), viewsForwardedTo.get(0));
        assertTrue(updateLayoutCalled);
    }

    /**
     * This test-method is similar to testRemoveUpdate02, but the text removed
     * is in the first paragraph.
     * <p>
     * Exactly one element is removed.
     */
    public void testRemoveUpdate07() throws BadLocationException {
        hasChildren = true;
        changeDocument();
        doc.remove(0, line.getStartOffset());
        updateChildrenReturn = false;
        assertEquals(4, view.getViewCount());
        view.removeUpdate(docEvent, rect, viewFactory);
        assertTrue(updateChildrenCalled);
        checkUpdatedChildren(4 - 1, 2);
        assertTrue(forwardUpdateCalled);
        assertTrue(forwardUpdateToViewCalled);
        assertEquals(1, viewsForwardedTo.size());
        assertSame(view.getView(0), viewsForwardedTo.get(0));
        assertTrue(updateLayoutCalled);
    }

    /**
     * This test-method is similar to testRemoveUpdate03, but the text removed
     * is in the first paragraph.
     * <p>
     * Exactly one element is removed.
     */
    public void testRemoveUpdate08() throws BadLocationException {
        hasChildren = true;
        changeDocument();
        doc.remove(0, line.getStartOffset());
        updateChildrenReturn = true;
        assertEquals(4, view.getViewCount());
        view.removeUpdate(docEvent, rect, viewFactory);
        assertTrue(updateChildrenCalled);
        checkUpdatedChildren(4 - 1, 2);
        assertTrue(forwardUpdateCalled);
        assertFalse(forwardUpdateToViewCalled);
        assertTrue(updateLayoutCalled);
    }

    /**
     * Tests <code>updateLayout</code> when element change is not
     * <code>null</code>.
     */
    public void testUpdateLayout01() throws BadLocationException {
        final class Params {
            View child;

            boolean height;

            boolean width;
        }
        final Params params = new Params();
        view = new DisAbstractedView(line) {
            @Override
            public void preferenceChanged(final View child, final boolean width,
                    final boolean height) {
                params.child = child;
                params.width = width;
                params.height = height;
            }
        };
        // Insert string to fill docEvent
        doc.insertString(line.getStartOffset() + 2, "one\ntwo\n", null);
        view.updateLayout(docEvent.getChange(root), docEvent, rect);
        assertNull(params.child);
        assertTrue(params.width);
        assertTrue(params.height);
    }

    /**
     * Tests <code>updateLayout</code> when element change is
     * <code>null</code>: seems like it has no side effects and
     * probably does nothing in this case.
     */
    public void testUpdateLayout02() throws BadLocationException {
        final boolean[] called = new boolean[1];
        view = new DisAbstractedView(line) {
            @Override
            public void preferenceChanged(final View child, final boolean width,
                    final boolean height) {
                called[0] = true;
            }
        };
        // Insert string to fill docEvent
        doc.insertString(line.getStartOffset() + 2, "one\ntwo\n", null);
        view.updateLayout(null, docEvent, rect);
        assertFalse(called[0]);
    }

    /**
     * Sets up the test fixture for changes tests.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.insertString(0, "01234\nabcde", null);
        root = doc.getDefaultRootElement();
        line = root.getElement(1);
        viewFactory = new ViewFactory() {
            public View create(final Element element) {
                viewsCreatedElements.add(element);
                return new ChildView(element);
            }
        };
        // We create anonymous subclass of View where we override
        // update methods to assert parameters passed
        view = new ChangeView(root);
        // Document listener to catch events on insert and remove
        // (so that they are real but not synthetic). But for event of
        // type <code>CHANGE</code> we create it ourselves.
        DocumentListener listener = new DocumentListener() {
            public void changedUpdate(final DocumentEvent event) {
                docEvent = event;
            }

            public void insertUpdate(final DocumentEvent event) {
                docEvent = event;
            }

            public void removeUpdate(final DocumentEvent event) {
                docEvent = event;
            }
        };
        doc.addDocumentListener(listener);
        CompositeView_ModelViewTest.shape = rect;
    }

    private void changeDocument() throws BadLocationException {
        doc.insertString(doc.getLength(), "one\ntwo\n", null);
        line = root.getElement(1);
        view.removeAll();
        ((CompositeView) view).loadChildren(viewFactory);
        viewsCreatedElements.clear();
        replaceViews = null;
    }

    /**
     * Checks that child views were updated as expected.
     *
     * @param count the new number of children
     * @param length the number of child views removed
     */
    private void checkUpdatedChildren(final int count, final int length) {
        Element[] added = docEvent.getChange(root).getChildrenAdded();
        assertEquals("added and created are different", added.length, viewsCreatedElements
                .size());
        for (int i = 0; i < added.length; i++) {
            assertSame("Elements different @ " + i, added[i], viewsCreatedElements.get(i));
        }
        assertEquals("Child view count is unexpected", count, view.getViewCount());
        assertEquals("Replace index is unexpected", docEvent.getChange(root).getIndex(),
                replaceIndex);
        assertEquals("Replace length is unexpected", length, replaceLength);
        assertEquals("Replace views.length is unexpected", added.length, replaceViews.length);
    }
    /*public void testUpdateChildren() {
     // tested in testInsertUpdate etc.
     }

     public void testForwardUpdate() {
     // tested in testInsertUpdate05
     }*/
}
