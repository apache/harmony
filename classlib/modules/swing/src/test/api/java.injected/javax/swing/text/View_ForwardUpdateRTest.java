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
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.text.CompositeView_ModelViewTest.ChildView;
import javax.swing.text.CompositeView_ModelViewTest.WithChildrenView;
import javax.swing.text.Position.Bias;
import junit.framework.TestCase;

public class View_ForwardUpdateRTest extends TestCase {
    private class ParentView extends WithChildrenView {
        private final String name;

        protected ParentView(Element element, String name) {
            super(element);
            this.name = name;
            loadChildren(viewFactory);
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName() + " on " + getElement();
        }

        public String getModelRange() {
            return "[" + getStartOffset() + ", " + getEndOffset() + "]";
        }

        public String getInfo() {
            return getName() + getModelRange();
        }

        @Override
        public int getViewIndex(int pos, Bias bias) {
            int result = super.getViewIndex(pos, bias);
            logger.println("\t" + getInfo() + ".getViewIndex(" + pos + ", " + bias + ") = "
                    + result);
            return result;
        }

        @Override
        protected void forwardUpdate(ElementChange change, DocumentEvent event, Shape shape,
                ViewFactory factory) {
            logger.println(">>> " + getInfo() + ".forwardUpdate");
            forwardUpdates.add(this);
            super.forwardUpdate(change, event, shape, factory);
            logger.println("<<< " + getInfo() + ".forwardUpdate");
        }

        @Override
        protected void forwardUpdateToView(View view, DocumentEvent event, Shape shape,
                ViewFactory factory) {
            viewsForwardedTo.add(view);
            super.forwardUpdateToView(view, event, shape, factory);
        }
    }

    private final class RootView extends ParentView {
        public RootView(Element element) {
            super(element, "root");
        }
    }

    private final class ParagraphView extends ParentView {
        protected ParagraphView(Element element) {
            super(element, "paragraph");
        }
    }

    private static final class ContentView extends ChildView {
        public ContentView(Element element) {
            super(element);
        }

        @Override
        public String toString() {
            return "content on " + getElement();
        }
    }

    private static final class Logger {
        private final boolean active;

        public Logger(final boolean active) {
            this.active = active;
        }

        public void print(final Object object) {
            print(object.toString());
        }

        public void print(final String message) {
            if (active) {
                System.out.print(message);
            }
        }

        public void println() {
            println("");
        }

        public void println(final Object object) {
            println(object.toString());
        }

        public void println(final String message) {
            print(message + "\n");
        }
    }

    private static final Rectangle rect = new Rectangle(200, 100);

    private static final Logger logger = new Logger(false);

    private Document doc;

    private Element root;

    private DocumentEvent docEvent;

    private ViewFactory viewFactory = new ViewFactory() {
        public View create(Element element) {
            final String name = element.getName();
            if (AbstractDocument.SectionElementName.equals(name)) {
                return new RootView(element);
            } else if (AbstractDocument.ParagraphElementName.equals(name)) {
                return new ParagraphView(element);
            } else if (AbstractDocument.ContentElementName.equals(name)) {
                return new ContentView(element);
            }
            return null;
        }
    };

    private View view;

    private List<ParentView> forwardUpdates = new ArrayList<ParentView>();

    private List<View> viewsForwardedTo = new ArrayList<View>();

    public void testForwardUpdate01() throws BadLocationException {
        doc = new PlainDocument();
        doc.insertString(doc.getLength(), "line1", null);
        doc.insertString(doc.getLength(), "\nline2", null);
        root = doc.getDefaultRootElement();
        view = viewFactory.create(root);
        assertEquals(2, root.getElementCount());
        assertEquals(2, view.getViewCount());
        docEvent = new DocumentEvent() {
            public int getOffset() {
                return 0;
            }

            public int getLength() {
                return doc.getLength() + 1;
            }

            public Document getDocument() {
                return doc;
            }

            public EventType getType() {
                return EventType.CHANGE;
            }

            public ElementChange getChange(Element elem) {
                return null;
            }
        };
        view.forwardUpdate(null, docEvent, rect, viewFactory);
        assertEquals(2, viewsForwardedTo.size());
        assertSame(view.getView(0), viewsForwardedTo.get(0));
        assertSame(view.getView(1), viewsForwardedTo.get(1));
    }

    public void testForwardUpdate02() throws BadLocationException {
        initStyledDocument();
        final MutableAttributeSet fontSize = new SimpleAttributeSet();
        StyleConstants.setFontSize(fontSize, 36);
        List<View> expected = new ArrayList<View>();
        logger.print(view);
        for (int i = 0; i < view.getViewCount(); i++) {
            View child = view.getView(i);
            expected.add(child);
            logger.print("\t" + child);
            for (int j = 0; j < child.getViewCount(); j++) {
                expected.add(child.getView(j));
                logger.print("\t\t" + child.getView(j));
            }
        }
        logger.println();
        ((StyledDocument) doc).setCharacterAttributes(0, doc.getLength() + 1, fontSize, false);
        view.forwardUpdate(docEvent.getChange(root), docEvent, rect, viewFactory);
        logger.println();
        assertEquals(expected.size(), viewsForwardedTo.size());
        for (int i = 0; i < viewsForwardedTo.size(); i++) {
            assertSame("@ " + i, expected.get(i), viewsForwardedTo.get(i));
            logger.print(viewsForwardedTo.get(i));
        }
    }

    public void testForwardUpdate03() throws BadLocationException {
        initStyledDocument();
        final List<View> expected = new ArrayList<View>();
        // We will use the view for the second paragraph
        view = view.getView(1);
        logger.print(view);
        for (int i = 0; i < view.getViewCount(); i++) {
            expected.add(view.getView(i));
            logger.print("\t" + view.getView(i));
        }
        logger.println();
        final MutableAttributeSet fontSize = new SimpleAttributeSet();
        StyleConstants.setFontSize(fontSize, 36);
        ((StyledDocument) doc).setCharacterAttributes(0, doc.getLength(), fontSize, false);
        assertEquals(-1, view.getViewIndex(docEvent.getOffset(), Bias.Forward));
        assertEquals(view.getViewCount() - 1, view.getViewIndex(docEvent.getOffset()
                + docEvent.getLength(), Bias.Forward));
        view.forwardUpdate(docEvent.getChange(view.getElement()), docEvent, rect, viewFactory);
        logger.println();
        assertEquals(expected.size(), viewsForwardedTo.size());
        for (int i = 0; i < viewsForwardedTo.size(); i++) {
            assertSame("@ " + i, expected.get(i), viewsForwardedTo.get(i));
            logger.print(viewsForwardedTo.get(i));
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CompositeView_ModelViewTest.shape = rect;
    }

    private void initStyledDocument() throws BadLocationException {
        doc = new DefaultStyledDocument();
        final MutableAttributeSet bold = new SimpleAttributeSet();
        StyleConstants.setBold(bold, true);
        doc.insertString(doc.getLength(), "plain", null);
        doc.insertString(doc.getLength(), "bold", bold);
        doc.insertString(doc.getLength(), "\nline2", null);
        root = doc.getDefaultRootElement();
        view = viewFactory.create(root);
        doc.addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                fail("insertUpdate is not expected");
            }

            public void removeUpdate(DocumentEvent e) {
                fail("removeUpdate is not expected");
            }

            public void changedUpdate(DocumentEvent e) {
                docEvent = e;
            }
        });
    }
}
