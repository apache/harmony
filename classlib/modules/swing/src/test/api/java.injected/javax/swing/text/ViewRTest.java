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
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BasicSwingTestCase;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.ViewTest.DisAbstractedView;

public class ViewRTest extends BasicSwingTestCase implements DocumentListener {
    private static class Factory implements ViewFactory {
        public View create(Element element) {
            final String name = element.getName();
            if (AbstractDocument.SectionElementName.equals(name)) {
                return new BoxView(element, View.Y_AXIS) {
                    @Override
                    protected void forwardUpdateToView(View view, DocumentEvent event,
                            Shape shape, ViewFactory factory) {
                        viewsForwardedTo.add(view);
                        super.forwardUpdateToView(view, event, shape, factory);
                    }
                };
            }
            if (AbstractDocument.ParagraphElementName.equals(name)) {
                return new ParagraphView(element);
            }
            if (AbstractDocument.ContentElementName.equals(name)) {
                return new LabelView(element);
            }
            throw new Error("Unexpected element name: " + name);
        }
    }

    private Document doc;

    private Element root;

    private View view;

    private DocumentEvent event;

    private static final ViewFactory factory = new Factory();

    private static final Shape shape = new Rectangle(5, 7, 452, 217);

    private static final List<View> viewsForwardedTo = new ArrayList<View>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        root = doc.getDefaultRootElement();
    }

    /**
     * Tests the case where a mutation occured on element boundaries, so that
     * both paragraphs changed but the section where these paragraphs lie
     * is not changed.
     * In this case the update must be forwarded to these both paragraphs.
     * <p>
     * In more detail, the document structure for this test is like this:
     * <pre>
     * section
     * |
     * +--paragraph
     * |  +--content [0, 5; plain]
     * |  +--content [5, 12; italic\n]
     * |
     * +--paragraph
     *    +--content [12, 13; \n]   // represents implied new line char
     * </pre>
     * Then an unattributed text, which doesn't contain the new line char,
     * is inserted at <code>doc.getLength()</code>.
     * This insert causes the element structure to be rebuild, however
     * no changes occur to the section element.
     * <p>Thus <code>change</code> parameter to
     * <code>forwardUpdate</code> will be <code>null</code>.
     */
    public void testInsertUpdate01() throws BadLocationException {
        MutableAttributeSet italic = new SimpleAttributeSet();
        StyleConstants.setItalic(italic, true);
        // Init the document structure
        doc.insertString(doc.getLength(), "plain", null);
        doc.insertString(doc.getLength(), "italic\n", italic);
        // Init the view hierarchy
        view = factory.create(root);
        ((CompositeView) view).loadChildren(factory);
        doc.addDocumentListener(this);
        // Perform the change tested against
        doc.insertString(doc.getLength(), "second", null);
        // Both paragraph elements will be modified,
        // but the section will not
        assertNotNull(event.getChange(root.getElement(0)));
        assertNotNull(event.getChange(root.getElement(1)));
        assertNull(event.getChange(root));
        viewsForwardedTo.clear();
        assertEquals(2, view.getViewCount());
        view.insertUpdate(event, shape, factory);
        assertEquals(2, view.getViewCount());
        assertEquals(2, viewsForwardedTo.size());
        assertSame(view.getView(0), viewsForwardedTo.get(0));
        assertSame(view.getView(1), viewsForwardedTo.get(1));
        viewsForwardedTo.clear();
    }

    /**
     * Tests the case where a mutation occured on element boundaries, so that
     * both the existing paragraph needs to rebuild its elements and a new
     * paragraph is created. Because of the latter, the section is modified
     * too.
     * In this case the view must create a new child to represent the new
     * paragrap as well as notify the previous paragraph so that it updates
     * itself.
     * <p>
     * In more detail, the document structure for this test is like this:
     * <pre>
     * section
     * |
     * +--paragraph
     * |  +--content [0, 5; plain]
     * |  +--content [5, 12; italic\n]
     * |
     * +--paragraph
     *    +--content [12, 19; second ]
     *    +--content [19, 31; italic again]
     *    +--content [31, 32; \n]   // represents implied new line char
     * </pre>
     * Then an unattributed text, which contains the new line char,
     * is inserted at <code>doc.getLength()</code>.
     * This insert causes the element structure to be rebuild:
     * <ol>
     *     <li>The second paragraph re-creates its last element.
     *         This element is mapped to the inserted text.</li>
     *     <li>The new paragraph is created, which will contain the
     *         implied new line char.<li>
     * </ol>
     * <p>Thus <code>change</code> parameter to
     * <code>forwardUpdate</code> will be not <code>null</code>.
     */
    public void testInsertUpdate02() throws BadLocationException {
        MutableAttributeSet italic = new SimpleAttributeSet();
        StyleConstants.setItalic(italic, true);
        // Init the document structure
        doc.insertString(doc.getLength(), "plain", null);
        doc.insertString(doc.getLength(), "italic\n", italic);
        doc.insertString(doc.getLength(), "second ", null);
        doc.insertString(doc.getLength(), "italic again", italic);
        // Init the view hierarchy
        view = factory.create(root);
        ((CompositeView) view).loadChildren(factory);
        doc.addDocumentListener(this);
        // Perform the change tested against
        doc.insertString(doc.getLength(), " & plain again\n", null);
        // Both paragraph elements will be modified,
        // but the section will not
        assertNotNull(event.getChange(root.getElement(1)));
        assertNull(event.getChange(root.getElement(2)));
        assertNotNull(event.getChange(root));
        viewsForwardedTo.clear();
        assertEquals(2, view.getViewCount());
        view.insertUpdate(event, shape, factory);
        assertEquals(3, view.getViewCount());
        assertEquals(1, viewsForwardedTo.size());
        assertSame(view.getView(1), viewsForwardedTo.get(0));
        viewsForwardedTo.clear();
    }

    public void testUpdateLayout() throws Exception {
        doc = new PlainDocument();
        root = doc.getDefaultRootElement();
        doc.insertString(0, "1\n\n\n\n", null);
        doc.addDocumentListener(this);
        doc.insertString(3, "\n2", null);
        final Marker marker = new Marker();
        final JTextArea area = new JTextArea(doc) {
            private static final long serialVersionUID = 1L;

            @Override
            public void repaint() {
                marker.setOccurred();
            }
        };
        view = new DisAbstractedView(root) {
            @Override
            public Container getContainer() {
                return area;
            }
        };
        assertEquals(0, view.getViewCount());
        assertNull(view.getParent());
        marker.reset();
        assertNotNull(event.getChange(root));
        view.updateLayout(event.getChange(root), event, shape);
        assertTrue(marker.isOccurred());
        marker.reset();
        view.updateLayout(null, event, shape);
        assertFalse(marker.isOccurred());
    }

    public void insertUpdate(DocumentEvent e) {
        event = e;
    }

    public void removeUpdate(DocumentEvent e) {
    }

    public void changedUpdate(DocumentEvent e) {
    }
}
