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

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import javax.swing.BasicSwingTestCase;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.AbstractDocument.ElementEdit;

/**
 * Tests mostly methods which are used when processing change
 * notifications.
 *
 * <p>This class uses simple initialization like in
 * <code>SimpleTests</code> but it creates a specialized version of
 * PlainView where tested methods are overridden.
 *
 */
public class PlainView_ChangesTest extends BasicSwingTestCase {
    static final class LineRange {
        public Component host;

        public int line0;

        public int line1;

        public Shape shape;

        public LineRange(final int line0, final int line1, final Shape shape,
                final Component host) {
            this.line0 = line0;
            this.line1 = line1;
            this.shape = shape;
            this.host = host;
        }

        /**
         * Checks that fields have expected values.
         */
        public void check(final int s, final int e, final Shape a, final Container container) {
            assertEquals("Unexpected start line", s, line0);
            assertEquals("Unexpected end line", e, line1);
            assertSame("Unexpected shape", a, shape);
            assertSame("Unexpected container/host", container, host);
        }
    }

    static final class PreferenceChange {
        public boolean height;

        public boolean width;

        public PreferenceChange(final boolean width, final boolean height) {
            this.width = width;
            this.height = height;
        }

        public void check(final boolean width, final boolean height) {
            assertEquals("Width has unexpected value", width, this.width);
            assertEquals("Height has unexpected value", height, this.height);
        }
    }

    private boolean callSuperDamageRange;

    private boolean callSuperUpdateDamage;

    private Container container;

    private Document doc;

    private DocumentEvent event;

    private ViewFactory factory;

    private LineRange lineRange;

    private Rectangle paintRect;

    private PreferenceChange preferenceChange;

    private Element root;

    private Shape shape;

    private boolean updateDamageCalled;

    private PlainView view;

    public void testChangedUpdate() {
        view.changedUpdate(event, shape, null);
        assertTrue(updateDamageCalled);
    }

    public void testDamageLineRange() {
        view.updateMetrics();
        final int height = view.metrics.getHeight();
        final int y = 300;
        callSuperDamageRange = true;
        shape = new Rectangle(200, y, 300, 500);
        view.damageLineRange(0, 0, shape, view.getContainer());
        assertTrue(paintRect.equals(new Rectangle(200, y, 300, height)));
        assertTrue(paintRect.equals(view.lineToRect(shape, 0)));
        paintRect = null;
        view.damageLineRange(1, 1, shape, view.getContainer());
        assertTrue(paintRect.equals(new Rectangle(200, y + height, 300, height)));
        assertTrue(paintRect.equals(view.lineToRect(shape, 1)));
        paintRect = null;
        view.damageLineRange(2, 2, shape, view.getContainer());
        assertTrue(paintRect.equals(new Rectangle(200, y + height * 2, 300, height)));
        assertTrue(paintRect.equals(view.lineToRect(shape, 2)));
        paintRect = null;
        view.damageLineRange(0, 2, shape, view.getContainer());
        assertEquals(new Rectangle(200, 300, 300, 3 * height), paintRect);
        Rectangle r0 = view.lineToRect(shape, 0);
        Rectangle r1 = view.lineToRect(shape, 1);
        Rectangle r2 = view.lineToRect(shape, 2);
        // Union all the rectangles
        r0 = r0.union(r1).union(r2);
        assertEquals(r0, paintRect);
    }

    /**
     * Tests that <code>insertUpdate</code> calls
     * <code>updateDamage</code> to perform the actual updates.
     */
    public void testInsertUpdateDamage() {
        view.insertUpdate(event, shape, null);
        assertTrue(updateDamageCalled);
    }

    public void testLineToRect() {
        view.updateMetrics();
        final int height = view.metrics.getHeight();
        assertEquals(new Rectangle(0, 0, 500, height), view.lineToRect(shape, 0));
        assertEquals(new Rectangle(0, height, 500, height), view.lineToRect(shape, 1));
        assertEquals(new Rectangle(30, 50 + height, 70, height), view.lineToRect(
                shape = new Rectangle2D.Float(30f, 50f, 70f, 10f), 1));
    }

    /**
     * Tests nextTabStop method with TabSize set to 4.
     */
    public void testNextTabStop02() {
        // Set tab size to 4
        doc.putProperty(PlainDocument.tabSizeAttribute, new Integer(4));
        assertEquals(4, view.getTabSize());
        // Update metrics 'cause view state isn't fully initialized yet
        view.updateMetrics();
        float tabPos = view.getTabSize() * view.metrics.charWidth('m');
        // Test tab stop positions
        assertEquals(tabPos, view.nextTabStop(0.0f, 0), 0.00001f);
        assertEquals(tabPos, view.nextTabStop(tabPos - 0.02f, 0), 0.00001f);
        assertEquals(tabPos * 2, view.nextTabStop(tabPos, 0), 0.00001f);
    }

    /**
     * Tests nextTabStop method with TabSize set to a negative value.
     */
    public void testNextTabStop03() {
        // Set tab size to -4
        doc.putProperty(PlainDocument.tabSizeAttribute, new Integer(-4));
        assertEquals(-4, view.getTabSize());
        // Update metrics 'cause view state isn't fully initialized yet
        view.updateMetrics();
        float tabPos = view.getTabSize() * view.metrics.charWidth('m');
        // Test tab stop positions
        assertEquals(tabPos, view.nextTabStop(0.0f, 0), 0.00001f);
        assertEquals(tabPos, view.nextTabStop(-tabPos - 0.2f, 0), 0.00001f);
        assertEquals(0.0f, view.nextTabStop(-tabPos, 0), 0.00001f);
        assertEquals(-tabPos, view.nextTabStop(-tabPos * 2, 0), 0.00001f);
        assertEquals(tabPos * 2, view.nextTabStop(tabPos, 0), 0.00001f);
    }

    /**
     * Tests nextTabStop method with TabSize set to zero.
     */
    public void testNextTabStop04() {
        // Set tab size to 0
        doc.putProperty(PlainDocument.tabSizeAttribute, new Integer(0));
        assertEquals(0, view.getTabSize());
        // Update metrics 'cause view state isn't fully initialized yet
        view.updateMetrics();
        float tabPos = view.getTabSize() * view.metrics.charWidth('m');
        assertEquals(0.0f, tabPos, 0.00001f);
        // Test tab stop positions
        assertEquals(tabPos, view.nextTabStop(0.0f, 0), 0.00001f);
        assertEquals(tabPos + 0.2f, view.nextTabStop(tabPos + 0.2f, 0), 0.00001f);
        assertEquals(4.75f, view.nextTabStop(4.75f, 0), 0.00001f);
    }

    public void testRemoveUpdate() {
        view.removeUpdate(event, shape, null);
        assertTrue(updateDamageCalled);
    }

    /**
     * Tests updateDamage with insert event
     */
    public void testUpdateDamage01() throws BadLocationException {
        createEvent();
        doc.insertString(0, "1:0123\n2:\n3:abcdefg", null);
        //                   0123456 789 012345678
        view.updateDamage(event, shape, factory);
        assertNull(lineRange);
        preferenceChange.check(true, true);
        preferenceChange = null;
        doc.insertString(14, "0123210", null);
        view.updateDamage(event, shape, factory);
        lineRange.check(2, 2, shape, container);
        lineRange = null;
        preferenceChange.check(true, false);
        preferenceChange = null;
        doc.insertString(14, "\n", null);
        view.updateDamage(event, shape, factory);
        if (!isHarmony()) {
            assertNull(lineRange);
        } else {
            lineRange.check(2, 3, shape, container);
            lineRange = null;
        }
        preferenceChange.check(true, true);
        preferenceChange = null;
        doc.insertString(15, "\n", null);
        view.updateDamage(event, shape, factory);
        if (!isHarmony()) {
            assertNull(lineRange);
        } else {
            lineRange.check(2, 4, shape, container);
            lineRange = null;
        }
        preferenceChange.check(isHarmony() ? false : true, true);
        preferenceChange = null;
        doc.insertString(15, "line1\nline2 change\nline3", null);
        view.updateDamage(event, shape, factory);
        if (!isHarmony()) {
            assertNull(lineRange);
        } else {
            lineRange.check(2, 6, shape, container);
            lineRange = null;
        }
        preferenceChange.check(isHarmony() ? false : true, true);
        preferenceChange = null;
    }

    /**
     * Tests updateDamage with remove event
     */
    public void testUpdateDamage02() throws BadLocationException {
        createEvent();
        doc.insertString(0, "1:0123\n2:\n3:abcdefg", null);
        //                   0123456 789 012345678
        // Update view
        assertNull(preferenceChange);
        view.updateDamage(event, shape, factory);
        assertNull(lineRange);
        preferenceChange.check(true, true);
        preferenceChange = null;
        // The widest line doesn't change, and line number neither
        doc.remove(2, 2); // "01" => "1:23\n2:\n3:..."
        view.updateDamage(event, shape, factory);
        lineRange.check(0, 0, shape, container);
        lineRange = null;
        assertNull(preferenceChange);
        // The widest line doesn't change, but line number changes
        doc.remove(4, 1); // "\n" => "1:232:\n3:..."
        view.updateDamage(event, shape, factory);
        assertNull(lineRange);
        preferenceChange.check(isHarmony() ? false : true, true);
        preferenceChange = null;
        // Again the widest line doesn't change, and line number stays the same
        doc.remove(4, 2);
        view.updateDamage(event, shape, factory);
        lineRange.check(0, 0, shape, container);
        assertNull(preferenceChange);
    }

    /**
     * Tests updateDamage with change event
     */
    public void testUpdateDamage03() throws BadLocationException {
        createEvent();
        doc.insertString(0, "1:0123\n2:\n3:abcdefg", null);
        //                   0123456 789 012345678
        // Update view
        assertNull(preferenceChange);
        view.updateDamage(event, shape, factory);
        assertNull(lineRange);
        preferenceChange.check(true, true);
        preferenceChange = null;
        event = ((AbstractDocument) doc).new DefaultDocumentEvent(3, 14,
                DocumentEvent.EventType.CHANGE);
        view.updateDamage(event, shape, factory);
        lineRange.check(0, 0, shape, container);
        lineRange = null;
        assertNull(preferenceChange);
        event = ((AbstractDocument) doc).new DefaultDocumentEvent(7, 12,
                DocumentEvent.EventType.CHANGE);
        view.updateDamage(event, shape, factory);
        lineRange.check(1, 1, shape, container);
        lineRange = null;
        assertNull(preferenceChange);
        // We remove the first and second lines, but the widest one isn't
        // changed, therefore the preferred width is not changed
        ElementEdit ee = new ElementEdit(root, 0, null, new Element[] { root.getElement(0),
                root.getElement(1) });
        ((DefaultDocumentEvent) event).addEdit(ee);
        view.updateDamage(event, shape, factory);
        assertNull(lineRange);
        preferenceChange.check(isHarmony() ? false : true, true);
    }

    public void testUpdateMetrics() {
        assertNull(view.metrics);
        view.updateMetrics();
        assertNotNull(view.metrics);
        assertNull(preferenceChange);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        root = doc.getDefaultRootElement();
        view = new PlainView(root) {
            @Override
            public Container getContainer() {
                if (container == null) {
                    container = new JTextArea() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void repaint(final int x, final int y, final int w, final int h) {
                            if (paintRect == null) {
                                paintRect = new Rectangle(x, y, w, h);
                            } else {
                                paintRect.add(new Rectangle(x, y, w, h));
                            }
                        }
                    };
                }
                return container;
            }

            @Override
            public void preferenceChanged(final View child, final boolean width,
                    final boolean height) {
                preferenceChange = new PreferenceChange(width, height);
                assertNull(child);
                super.preferenceChanged(child, width, height);
            }

            @Override
            protected void damageLineRange(final int line0, final int line1, final Shape shape,
                    final Component host) {
                if (callSuperDamageRange) {
                    super.damageLineRange(line0, line1, shape, host);
                } else {
                    lineRange = new LineRange(line0, line1, shape, host);
                }
            }

            @Override
            protected void updateDamage(final DocumentEvent changes, final Shape a,
                    final ViewFactory f) {
                if (callSuperUpdateDamage) {
                    super.updateDamage(changes, a, f);
                } else {
                    assertSame(event, changes);
                    assertSame(shape, a);
                    assertNull(f);
                    updateDamageCalled = true;
                }
            }
        };
        shape = new Rectangle(500, 500);
        event = ((AbstractDocument) doc).new DefaultDocumentEvent(0, 0,
                DocumentEvent.EventType.CHANGE);
        factory = new ViewFactory() {
            public View create(final Element element) {
                fail("factory.create() isn't supposed to be called");
                return null;
            }
        };
    }

    private void createEvent() {
        doc.addDocumentListener(new DocumentListener() {
            public void changedUpdate(final DocumentEvent changes) {
                fail("changedUpdate isn't supposed to be called");
            }

            public void insertUpdate(final DocumentEvent changes) {
                event = changes;
            }

            public void removeUpdate(final DocumentEvent changes) {
                event = changes;
            }
        });
        callSuperUpdateDamage = true;
    }
}