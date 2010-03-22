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
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.FlowViewTest.FlowViewImplWithFactory;
import javax.swing.text.ViewTestHelpers.ChildView;
import junit.framework.TestCase;

/**
 * Tests FlowView methods which react to changes in the associated document.
 *
 */
public class FlowView_ChangesTest extends TestCase implements DocumentListener {
    private AbstractDocument doc;

    private Element root;

    private FlowView view;

    private DocumentEvent event;

    private Rectangle alloc = new Rectangle(11, 21, 453, 217);

    /**
     * Stores information which event-method was called and on which view.
     */
    private static class Changes {
        static final Object INSERT = "insert";

        static final Object REMOVE = "remove";

        static final Object CHANGE = "change";

        private final View view;

        private final Object method;

        Changes(final View view, final Object method) {
            this.view = view;
            this.method = method;
        }

        final void check(final View view, final Object method) {
            assertSame(this.view, view);
            assertSame(this.method, method);
        }
    }

    /**
     * Stores information which view changed its preferences, which child
     * cause the change, and how preferences were changed.
     */
    private static class Preferences {
        private final View view;

        private final View child;

        private final boolean width;

        private final boolean height;

        Preferences(final View view, final View child, final boolean width, final boolean height) {
            this.view = view;
            this.child = child;
            this.width = width;
            this.height = height;
        }

        final void check(final View view, final View child, final boolean width,
                final boolean height) {
            assertSame("Host view", this.view, view);
            assertSame("Child", this.child, child);
            assertEquals("Width", this.width, width);
            assertEquals("Height", this.height, height);
        }
    }

    /**
     * List of views which got update event.
     */
    private final List<Changes> changes = new ArrayList<Changes>();

    /**
     * List of views which changed their preferences.
     */
    private final List<Preferences> preferences = new ArrayList<Preferences>();

    private class FlowFactory implements ViewFactory {
        private int count = 0;

        public View create(Element element) {
            return new ChildView(element, count++) {
                @Override
                public void insertUpdate(DocumentEvent event, Shape shape, ViewFactory factory) {
                    changes.add(new Changes(this, Changes.INSERT));
                    preferenceChanged(null, true, false);
                }

                @Override
                public void removeUpdate(DocumentEvent event, Shape shape, ViewFactory factory) {
                    changes.add(new Changes(this, Changes.REMOVE));
                    preferenceChanged(null, false, true);
                }

                @Override
                public void changedUpdate(DocumentEvent event, Shape shape, ViewFactory factory) {
                    changes.add(new Changes(this, Changes.CHANGE));
                    preferenceChanged(null, true, true);
                }

                @Override
                public String toString() {
                    return "child(" + getID() + ")";
                }
            };
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.insertString(0, "line1\nline2\nline3", null);
        root = doc.getDefaultRootElement();
        view = new FlowViewImplWithFactory(root, View.Y_AXIS, new FlowFactory()) {
            private int count = 0;

            @Override
            protected View createRow() {
                return new BoxView(getElement(), X_AXIS) {
                    private final int id = count++;

                    @Override
                    public void insertUpdate(DocumentEvent event, Shape shape,
                            ViewFactory factory) {
                        changes.add(new Changes(this, Changes.INSERT));
                        super.insertUpdate(event, shape, factory);
                    }

                    @Override
                    public void removeUpdate(DocumentEvent event, Shape shape,
                            ViewFactory factory) {
                        changes.add(new Changes(this, Changes.REMOVE));
                        super.removeUpdate(event, shape, factory);
                    }

                    @Override
                    public void changedUpdate(DocumentEvent event, Shape shape,
                            ViewFactory factory) {
                        changes.add(new Changes(this, Changes.CHANGE));
                        super.changedUpdate(event, shape, factory);
                    }

                    @Override
                    public void preferenceChanged(View child, boolean width, boolean height) {
                        preferences.add(new Preferences(this, child, width, height));
                        super.preferenceChanged(child, width, height);
                    }

                    @Override
                    public String toString() {
                        return "row(" + id + ")";
                    }

                    @Override
                    protected void loadChildren(ViewFactory factory) {
                        return;
                    }
                };
            }

            @Override
            public void preferenceChanged(View child, boolean width, boolean height) {
                preferences.add(new Preferences(this, child, width, height));
                super.preferenceChanged(child, width, height);
            }

            @Override
            public String toString() {
                return "flow";
            }
        };
        view.layoutPool = new BoxView(root, View.X_AXIS) {
            @Override
            public void insertUpdate(DocumentEvent event, Shape shape, ViewFactory factory) {
                changes.add(new Changes(this, Changes.INSERT));
                super.insertUpdate(event, shape, factory);
            }

            @Override
            public void removeUpdate(DocumentEvent event, Shape shape, ViewFactory factory) {
                changes.add(new Changes(this, Changes.REMOVE));
                super.removeUpdate(event, shape, factory);
            }

            @Override
            public void changedUpdate(DocumentEvent event, Shape shape, ViewFactory factory) {
                changes.add(new Changes(this, Changes.CHANGE));
                super.changedUpdate(event, shape, factory);
            }

            @Override
            public void preferenceChanged(View child, boolean width, boolean height) {
                preferences.add(new Preferences(this, child, width, height));
                super.preferenceChanged(child, width, height);
            }

            @Override
            public String toString() {
                return "pool";
            }
        };
        view.layoutPool.setParent(view);
        view.layout(alloc.width, alloc.height);
        changes.clear();
        preferences.clear();
        doc.addDocumentListener(this);
    }

    public void testInsertUpdate() throws BadLocationException {
        assertEquals(alloc.width, view.layoutSpan);
        assertEquals(1, view.getViewCount());
        assertTrue(view.isAllocationValid());
        View row = view.getView(0);
        assertEquals(root.getElementCount(), row.getViewCount());
        for (int i = 0; i < row.getViewCount(); i++) {
            assertSame(view.layoutPool.getView(i), row.getView(i));
        }
        doc.insertString(1, "^^^", null);
        view.insertUpdate(event, alloc, view.getViewFactory());
        assertFalse(view.isAllocationValid());
        assertEquals(2, changes.size());
        changes.get(0).check(view.layoutPool, Changes.INSERT);
        changes.get(1).check(view.layoutPool.getView(0), Changes.INSERT);
        assertEquals(2, preferences.size());
        preferences.get(0).check(view.getView(0), view.layoutPool.getView(0),
                true, false);
        preferences.get(1).check(view, view.getView(0), true, false);
    }

    public void testRemoveUpdate() throws BadLocationException {
        assertTrue(view.isAllocationValid());
        doc.remove(1, 1);
        view.removeUpdate(event, alloc, view.getViewFactory());
        assertFalse(view.isAllocationValid());
        assertEquals(2, changes.size());
        changes.get(0).check(view.layoutPool, Changes.REMOVE);
        changes.get(1).check(view.layoutPool.getView(0), Changes.REMOVE);
        assertEquals(2, preferences.size());
        preferences.get(0).check(view.getView(0), view.layoutPool.getView(0),
                false, true);
        preferences.get(1).check(view, view.getView(0), false, true);
    }

    public void testChangedUpdate() {
        assertTrue(view.isAllocationValid());
        event = doc.new DefaultDocumentEvent(1, 1, EventType.CHANGE);
        view.changedUpdate(event, alloc, view.getViewFactory());
        assertFalse(view.isAllocationValid());
        assertEquals(2, changes.size());
        changes.get(0).check(view.layoutPool, Changes.CHANGE);
        changes.get(1).check(view.layoutPool.getView(0), Changes.CHANGE);
        assertEquals(2, preferences.size());
        preferences.get(0).check(view.getView(0), view.layoutPool.getView(0),
                true, true);
        preferences.get(1).check(view, view.getView(0), true, true);
    }

    public void insertUpdate(DocumentEvent e) {
        event = e;
    }

    public void removeUpdate(DocumentEvent e) {
        event = e;
    }

    public void changedUpdate(DocumentEvent e) {
        event = e;
    }
}
