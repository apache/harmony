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

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;

import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.SwingTestCase;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit.InsertHTMLTextAction;

public class HTMLEditorKitTest extends SwingTestCase {
    private static final String HTML_TEXT = "<title>t</title>html <i>text</i>";
    private static final String LOADED_HTML_TEXT = "  \nhtml text";
    private HTMLEditorKit editorKit;
    private HTMLDocument document;

    public HTMLEditorKitTest(final String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);

        editorKit = new HTMLEditorKit();
        document = (HTMLDocument)editorKit.createDefaultDocument();
        document.setAsynchronousLoadPriority(-1);  // synchronous loading
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHTMLEditorKit() {
        editorKit = new HTMLEditorKit();

        assertNotNull(editorKit.getActions());
    }

    public void testClone() {
        // TODO: implement
    }

    public void testCreateDefaultDocument() {
        Document doc = editorKit.createDefaultDocument();
        assertTrue(doc instanceof HTMLDocument);

        HTMLDocument htmlDoc = (HTMLDocument)doc;
        assertSame(editorKit.getParser(), htmlDoc.getParser());
        assertEquals(4, htmlDoc.getAsynchronousLoadPriority());
        assertNotNull(htmlDoc.getStyleSheet());
        assertFalse(editorKit.getStyleSheet().equals(htmlDoc.getStyleSheet()));

        assertTrue(Arrays.asList(htmlDoc.getStyleSheet().getStyleSheets())
                   .contains(editorKit.getStyleSheet()));
    }

    public void testDeinstallJEditorPane() {
        JEditorPane pane = new JEditorPane();
        int mouseListenersCount = pane.getMouseListeners().length;
        int mouseMotionListenersCount = pane.getMouseMotionListeners().length;

        editorKit.install(pane);
        editorKit.deinstall(pane);

        assertEquals(mouseListenersCount,
                     pane.getMouseListeners().length);
        assertEquals(mouseMotionListenersCount,
                     pane.getMouseMotionListeners().length);
    }

    public void testGetAccessibleContext() {
        // TODO: implement
    }

    public void testGetActions() throws Exception {
        Action[] ancestorActions = new StyledEditorKit().getActions();
        Action[] actions = editorKit.getActions();
        assertEquals(12, actions.length - ancestorActions.length);

        Action[] predefinedInsertHTMLTextActions = createPredefinedInsertHTMLTextActions();
        for (int i = 0; i < predefinedInsertHTMLTextActions.length; i++) {
            Action action = findActionWithName(actions,
                predefinedInsertHTMLTextActions[i].getValue(Action.NAME));
            if (action != null) {
                assertTrue("Action is not same" + action.getValue(Action.NAME),
                           compareInsertHTMLTextActions(action,
                                                        predefinedInsertHTMLTextActions[i]));
            } else {
                fail("Action not found: " + predefinedInsertHTMLTextActions[i].getValue(Action.NAME));
            }
        }
    }

    public void testNextLinkAction() throws Exception {
        Action action = findActionWithName(
            editorKit.getActions(), "next-link-action");
        assertNotNull(action);

        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        pane.setEditorKit(editorKit);
        document = ((HTMLDocument)pane.getDocument());
        document.setAsynchronousLoadPriority(-1);  // synchronous loading
        pane.setText("<p><a href=http://a.com>a.com</a>text<a href=http://b.com>b.com</a></p>");
        pane.setCaretPosition(0);

        action.actionPerformed(new ActionEvent(pane, 0, null));
        Element e = document.getCharacterElement(pane.getCaretPosition());
        assertEquals("http://a.com", getURLString(e));
        action.actionPerformed(new ActionEvent(pane, 0, null));
        e = document.getCharacterElement(pane.getCaretPosition());
        assertEquals("http://b.com", getURLString(e));
    }

    public void testPreviousLinkAction() throws Exception {
        Action action = findActionWithName(
            editorKit.getActions(), "previous-link-action");
        assertNotNull(action);

        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        pane.setEditorKit(editorKit);
        document = ((HTMLDocument)pane.getDocument());
        document.setAsynchronousLoadPriority(-1);  // synchronous loading
        pane.setText("<p><a href=http://a.com>a.com</a>text<a href=http://b.com>b.com</a></p>");
        pane.setCaretPosition(document.getLength() - 1);

        action.actionPerformed(new ActionEvent(pane, 0, null));
        Element e = document.getCharacterElement(pane.getCaretPosition());
        assertEquals("http://b.com", getURLString(e));
        action.actionPerformed(new ActionEvent(pane, 0, null));
        e = document.getCharacterElement(pane.getCaretPosition());
        assertEquals("http://a.com", getURLString(e));
    }

    public void testActivateLinkAction() throws Exception {
        Action action = findActionWithName(
            editorKit.getActions(), "activate-link-action");
        assertNotNull(action);

        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        pane.setEditorKit(editorKit);
        document = ((HTMLDocument)pane.getDocument());
        document.setAsynchronousLoadPriority(-1);  // synchronous loading
        pane.setText("<p><a href=http://a.com>a.com</a>text<a href=http://b.com>b.com</a></p>");
        pane.setCaretPosition(1);

        class TestHyperlinkListener implements HyperlinkListener {
            public boolean occured;

            public void hyperlinkUpdate(HyperlinkEvent event) {
                occured = true;
            }
        }

        TestHyperlinkListener listener = new TestHyperlinkListener();
        pane.addHyperlinkListener(listener);

        action.actionPerformed(new ActionEvent(pane, 0, null));
        assertTrue(listener.occured);
    }

    public void testInsertHRAction() throws Exception {
        InsertHTMLTextAction action = (InsertHTMLTextAction)findActionWithName(
            editorKit.getActions(), "InsertHR");
        assertNotNull(action);

        JEditorPane pane = new JEditorPane();
        pane.setEditorKit(editorKit);
        document = ((HTMLDocument)pane.getDocument());
        document.setAsynchronousLoadPriority(-1);  // synchronous loading
        pane.setText("<p>test</p>");
        final int pos = document.getLength() - 1;
        pane.setCaretPosition(pos);

        action.actionPerformed(new ActionEvent(pane, 0, null));
        Element e = document.getCharacterElement(pos + 1);
        assertEquals(HTML.Tag.HR, getHTMLTagByElement(e));
        assertNotNull(e);
        HTML.Tag parentTag = getHTMLTagByElement(e.getParentElement());
        assertTrue(HTML.Tag.P.equals(parentTag) || HTML.Tag.IMPLIED.equals(parentTag));
    }

    public void testGetContentType() {
        assertEquals("text/html", editorKit.getContentType());
    }

    public void testGetInputAttributes() throws Exception {
        JEditorPane pane = new JEditorPane();
        editorKit.install(pane);
        editorKit.read(new StringReader("normal<i>italic</i>"),
                       pane.getDocument(), 0);

        pane.setCaretPosition(pane.getDocument().getLength() - 1);
        assertNotNull(editorKit.getInputAttributes());
    }

    public void testGetViewFactory() {
        ViewFactory factory = editorKit.getViewFactory();
        assertTrue(factory instanceof HTMLEditorKit.HTMLFactory);
        assertSame(factory, editorKit.getViewFactory());
        assertSame(factory, new HTMLEditorKit().getViewFactory());
    }

    public void testInsertHTML() throws Exception {
        final String HTML_TEXT2 = "<i>_another text_</i>";
        final String HTML_TEXT3 = ("");
        final String INSERTION_RESULT = "  \nhtml_another text_ text";

        editorKit.read(new StringReader(HTML_TEXT), document, 0);
        String s = document.getText(0, document.getLength());
        assertEquals(LOADED_HTML_TEXT, s);

        editorKit.insertHTML(document, 7, HTML_TEXT2, 0, 0, HTML.Tag.I);
        assertEquals(INSERTION_RESULT, document.getText(0, document.getLength()));

        // test pos > document's length
        testExceptionalCase(new ExceptionalCase() {
            public Class expectedExceptionClass() {
                return BadLocationException.class;
            }
            public void exceptionalAction() throws Exception {
                editorKit.insertHTML(document, document.getLength() + 1,
                                     HTML_TEXT3, 0, 0, HTML.Tag.P);
            }
        });

        // test pos < 0
        testExceptionalCase(new ExceptionalCase() {
            public Class expectedExceptionClass() {
                return BadLocationException.class;
            }
            public void exceptionalAction() throws Exception {
                editorKit.insertHTML(document, -1, HTML_TEXT2, 0, 0, HTML.Tag.I);
            }
        });

        // empty insertion, no exception should be thrown
        editorKit.insertHTML(document, -1, HTML_TEXT3, 0, 0, HTML.Tag.I);
    }

    public void testInstallJEditorPane() {
        JEditorPane pane = new JEditorPane();
        int mouseListenersCount = pane.getMouseListeners().length;
        int mouseMotionListenersCount = pane.getMouseMotionListeners().length;

        editorKit.install(pane);

        assertEquals(mouseListenersCount + 1,
                     pane.getMouseListeners().length);
        assertEquals(mouseMotionListenersCount + 1,
                     pane.getMouseMotionListeners().length);
    }

    public void testRead() throws Exception {
        final StringReader in1 = new StringReader(HTML_TEXT);
        final StringReader in2 = new StringReader("another text");
        final StringReader in3 = new StringReader("");
        final StringReader in4 = new StringReader("");
        final StringReader in5 = new StringReader("");
        final String text2 = "  \nhtml\nanother text\n text";

        editorKit.read(in1, document, 0);
        String s = document.getText(0, document.getLength());
        assertEquals(LOADED_HTML_TEXT, s);
        testExceptionalCase(new ExceptionalCase() {
            public Class expectedExceptionClass() {
                return IOException.class;
            }
            public void exceptionalAction() throws Exception {
                in1.ready();
            }
        });

        editorKit.read(in2, document, 7);
        assertEquals(text2, document.getText(0, document.getLength()));

        // test pos > document's length
        testExceptionalCase(new ExceptionalCase() {
            public Class expectedExceptionClass() {
                return BadLocationException.class;
            }
            public void exceptionalAction() throws Exception {
                editorKit.read(in3, document, document.getLength() + 1);
            }
        });

        // test pos outside BODY
        testExceptionalCase(new ExceptionalCase() {
            public Class expectedExceptionClass() {
                return RuntimeException.class;
            }
            public void exceptionalAction() throws Exception {
                editorKit.read(in4, document, 0);
            }
        });

        // test pos < 0
        testExceptionalCase(new ExceptionalCase() {
            public Class expectedExceptionClass() {
                return RuntimeException.class;
            }
            public void exceptionalAction() throws Exception {
                editorKit.read(in5, document, -1);
            }
        });
    }

    public void testSetIsAutoFormSubmission() {
        assertTrue(editorKit.isAutoFormSubmission());

        editorKit.setAutoFormSubmission(false);
        assertFalse(editorKit.isAutoFormSubmission());
    }

    public void testSetGetDefaultCursor() {
        assertSame(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR),
                   editorKit.getDefaultCursor());

        Cursor newCursor = new Cursor(Cursor.DEFAULT_CURSOR);
        editorKit.setDefaultCursor(newCursor);
        assertSame(newCursor, editorKit.getDefaultCursor());
    }

    public void testSetGetLinkCursor() {
        assertSame(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR),
                   editorKit.getLinkCursor());

        Cursor newCursor = new Cursor(Cursor.DEFAULT_CURSOR);
        editorKit.setLinkCursor(newCursor);
        assertSame(newCursor, editorKit.getLinkCursor());
    }

    public void testSetStyleSheet() {
        StyleSheet ss = new StyleSheet();
        editorKit.setStyleSheet(ss);
        assertSame(ss, editorKit.getStyleSheet());

        editorKit = new HTMLEditorKit();
        assertSame(ss, editorKit.getStyleSheet());
    }

    public void testGetStyleSheet() {
        StyleSheet ss = editorKit.getStyleSheet();
        assertNotNull(ss);
        assertSame(ss, editorKit.getStyleSheet());

        editorKit = new HTMLEditorKit();
        assertSame(ss, editorKit.getStyleSheet());
    }

    public void testWrite() throws Exception {
        StringWriter writer = new StringWriter();
        final String content = "Hello, World!";
        final int start = 1;
        final int end = 4;
        DefaultStyledDocument doc = new DefaultStyledDocument();
        doc.insertString(0, content, null);

        editorKit.write(writer, doc, start, end);
        String output = writer.toString();
        assertTrue(output.indexOf("<html>") != -1);
        if (isHarmony()) {
            assertFalse(output.indexOf(content) != -1);
            assertTrue(output.indexOf(content.substring(start, end)) != -1);
        }

        writer = new StringWriter();
        doc = (HTMLDocument)editorKit.createDefaultDocument();
        doc.insertString(0, content, null);
        editorKit.write(writer, doc, start, end);
        output = writer.toString();
        assertTrue(output.indexOf("<html>") != -1);
        assertFalse(output.indexOf(content) != -1);
        assertTrue(output.indexOf(content.substring(start, end)) != -1);
    }

    public void testGetParser() {
        HTMLEditorKit.Parser parser = editorKit.getParser();
        assertNotNull(parser);
        assertSame(parser, editorKit.getParser());
        assertSame(parser, new HTMLEditorKit().getParser());
    }

    public void testCreateInputAttributes() throws Exception {
        document.insertAfterStart(document.getDefaultRootElement(), "<b>bold</b>");
        Element e = document.getDefaultRootElement().getElement(0);
        SimpleAttributeSet attrSet = new SimpleAttributeSet();
        editorKit.createInputAttributes(e, attrSet);
        assertTrue(attrSet.containsAttribute(StyleConstants.NameAttribute,
                                             HTML.Tag.CONTENT));
        assertEquals("bold", attrSet.getAttribute(CSS.Attribute.FONT_WEIGHT).toString());
    }

    public void testParserCallback() {
        Object implied = HTMLEditorKit.ParserCallback.IMPLIED;
        assertTrue(implied instanceof String);
        assertFalse("".equals(implied));
    }

    private Action[] createPredefinedInsertHTMLTextActions() {
        Action[] actions = {
            new HTMLEditorKit.InsertHTMLTextAction("InsertOrderedList",
                "<ol><li></li></ol>", HTML.Tag.BODY, HTML.Tag.OL),
            new HTMLEditorKit.InsertHTMLTextAction("InsertOrderedListItem",
                "<ol><li></li></ol>",
                HTML.Tag.OL, HTML.Tag.LI, HTML.Tag.BODY, HTML.Tag.OL),
            new HTMLEditorKit.InsertHTMLTextAction("InsertUnorderedList",
                "<ul><li></li></ul>", HTML.Tag.BODY, HTML.Tag.UL),
            new HTMLEditorKit.InsertHTMLTextAction("InsertUnorderedListItem",
                "<ul><li></li></ul>",
                HTML.Tag.UL, HTML.Tag.LI, HTML.Tag.BODY, HTML.Tag.UL),
            new HTMLEditorKit.InsertHTMLTextAction("InsertTable",
                "<table border=1><tr><td></td></tr></table>",
                HTML.Tag.BODY, HTML.Tag.TABLE),
            new HTMLEditorKit.InsertHTMLTextAction("InsertTableDataCell",
                "<table border=1><tr><td></td></tr></table>",
                HTML.Tag.TR, HTML.Tag.TD, HTML.Tag.BODY, HTML.Tag.TABLE),
            new HTMLEditorKit.InsertHTMLTextAction("InsertTableRow",
                "<table border=1><tr><td></td></tr></table>",
                HTML.Tag.TABLE, HTML.Tag.TR, HTML.Tag.BODY, HTML.Tag.TABLE),
            new HTMLEditorKit.InsertHTMLTextAction("InsertPre", "<pre></pre>",
                HTML.Tag.BODY, HTML.Tag.PRE),
        };

        return actions;
    }

    private boolean compareInsertHTMLTextActions(final Action a1,
                                                 final Action a2) {
        if (!(a1 instanceof HTMLEditorKit.InsertHTMLTextAction)
                || !(a2 instanceof HTMLEditorKit.InsertHTMLTextAction)) {
            return false;
        }
        HTMLEditorKit.InsertHTMLTextAction htmlAction1 = (HTMLEditorKit.InsertHTMLTextAction)a1;
        HTMLEditorKit.InsertHTMLTextAction htmlAction2 = (HTMLEditorKit.InsertHTMLTextAction)a2;
        return compareActionFields(htmlAction1.addTag, htmlAction2.addTag)
            && compareActionFields(htmlAction1.alternateAddTag, htmlAction2.alternateAddTag)
            && compareActionFields(htmlAction1.alternateParentTag, htmlAction2.alternateParentTag)
            && compareActionFields(htmlAction1.html, htmlAction2.html)
            && compareActionFields(htmlAction1.parentTag, htmlAction2.parentTag);
    }

    private boolean compareActionFields(final Object f1, final Object f2) {
        return f1 != null && f1.equals(f2) || f1 == f2;
    }

    private Action findActionWithName(final Action[] actions, final Object name) {
        for (int i = 0; i < actions.length; i++) {
            if (name.equals(actions[i].getValue(Action.NAME))) {
                return actions[i];
            }
        }
        return null;
    }

    private static HTML.Tag getHTMLTagByElement(final Element elem) {
        final Object result = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
        return (result instanceof HTML.Tag) ? (HTML.Tag)result : null;
    }


    private static String getURLString(final Element e) {
        AttributeSet aSet = (AttributeSet)e.getAttributes()
                .getAttribute(HTML.Tag.A);
        return aSet == null
                ? null
                : (String)aSet.getAttribute(HTML.Attribute.HREF);
    }
}
