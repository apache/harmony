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
 * @author Vadim L. Bogdanov
 */
package javax.swing.text.html;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.SwingTestCase;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;

public class HTMLEditorKit_HTMLTextActionTest extends SwingTestCase {
    private static final String HTML_TEXT = "<i>Italic text</i>";

    private static class TestHTMLTextAction extends HTMLEditorKit.HTMLTextAction {
        public TestHTMLTextAction(final String name) {
            super(name);
        }

        public void actionPerformed(final ActionEvent e) {
        }
    }

    private JEditorPane editorPane;
    private HTMLEditorKit editorKit;
    private HTMLDocument document;
    private HTMLEditorKit.HTMLTextAction action;

    public HTMLEditorKit_HTMLTextActionTest(final String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);

        editorPane = new JEditorPane();
        editorKit = new HTMLEditorKit();
        editorPane.setEditorKit(editorKit);
        document = (HTMLDocument)editorPane.getDocument();
        action = new TestHTMLTextAction("name");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHTMLTextAction() {
        action = new TestHTMLTextAction("actionName");
        assertEquals("actionName", action.getValue(AbstractAction.NAME));
    }

    public void testElementCountToTag() throws Exception {
        loadHTML();
        final int offset = editorPane.getDocument().getLength();

        int bodyCount = action.elementCountToTag(document,
                                                 offset,
                                                 HTML.Tag.BODY);
        assertEquals(1, bodyCount);

        assertEquals(-1, action.elementCountToTag(document,
                                                  offset,
                                                  HTML.Tag.HEAD));
    }

    public void testFindElementMatchingTag() throws Exception {
        loadHTML();
        final int offset = editorPane.getDocument().getLength();
        Element[] elems = action.getElementsAt(document, offset);

        Element bodyElem = action.findElementMatchingTag(document, offset,
                                                         HTML.Tag.BODY);
        assertEquals(HTML.Tag.BODY, getHTMLTagByElement(bodyElem));
        assertTrue(Arrays.asList(elems).contains(bodyElem));

        assertNull(action.findElementMatchingTag(document, offset,
                                                 HTML.Tag.HEAD));

        assertNotNull(action.findElementMatchingTag(document,
                                                    document.getLength() + 1,
                                                    HTML.Tag.BODY));
    }

    public void testGetElementsAt() throws Exception {
        loadHTML();
        final int offset = editorPane.getDocument().getLength();

        Element[] elems = action.getElementsAt(
                action.getHTMLDocument(editorPane), offset);
        assertEquals(editorPane.getDocument().getDefaultRootElement(), elems[0]);
        assertEquals(0, elems[elems.length - 1].getElementCount());

        for (int i = 0; i < elems.length; i++) {
            assertTrue(elems[i].getStartOffset() <= offset
                       && elems[i].getEndOffset() >= offset);
        }

        elems = action.getElementsAt(document, document.getLength());
        assertEquals(editorPane.getDocument().getDefaultRootElement(), elems[0]);

        elems = action.getElementsAt(document, document.getLength() + 1);
        assertEquals(editorPane.getDocument().getDefaultRootElement(), elems[0]);
    }

    public void testGetHTMLDocument() {
        assertSame(editorPane.getDocument(),
                   action.getHTMLDocument(editorPane));
    }

    public void testGetHTMLEditorKit() {
        assertSame(editorPane.getEditorKit(),
                   action.getHTMLEditorKit(editorPane));

        editorPane.setEditorKit(new StyledEditorKit());
        testExceptionalCase(new IllegalArgumentCase() {
            public void exceptionalAction() throws Exception {
                action.getHTMLEditorKit(editorPane);
            }
        });
    }

    private void loadHTML() throws IOException, BadLocationException {
        StringReader in = new StringReader(HTML_TEXT);
        editorKit.read(in, editorPane.getDocument(), 0);
    }

    private static HTML.Tag getHTMLTagByElement(final Element elem) {
        return (HTML.Tag)elem.getAttributes()
                .getAttribute(StyleConstants.NameAttribute);
    }
}
