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

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.SwingTestCase;
import javax.swing.text.BadLocationException;

public class HTMLEditorKit_InsertHTMLTextActionTest extends SwingTestCase {
    private final String name = "name";
    private final HTML.Tag addTag = HTML.Tag.I;
    private final HTML.Tag alternateAddTag = HTML.Tag.B;
    private final HTML.Tag alternateParentTag = HTML.Tag.I;
    private final String html = "<i><b>html text</b></i>";
    private final HTML.Tag parentTag = HTML.Tag.B;

    private HTMLEditorKit.InsertHTMLTextAction action;
    private JEditorPane pane;
    private HTMLDocument document;

    public HTMLEditorKit_InsertHTMLTextActionTest(final String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);

        pane = new JEditorPane();
        pane.setEditorKit(new HTMLEditorKit());
        document = (HTMLDocument)pane.getDocument();

        action = new HTMLEditorKit.InsertHTMLTextAction(name, html,
                                                        parentTag, addTag,
                                                        alternateParentTag,
                                                        alternateAddTag);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testInsertHTMLTextActionStringStringTagTagTagTag() {
        action = new HTMLEditorKit.InsertHTMLTextAction(name, html,
                                                        parentTag, addTag,
                                                        alternateParentTag,
                                                        alternateAddTag);
        assertSame(name, action.getValue(AbstractAction.NAME));
        assertSame(html, action.html);
        assertSame(parentTag, action.parentTag);
        assertSame(addTag, action.addTag);
        assertSame(alternateParentTag, action.alternateParentTag);
        assertSame(alternateAddTag, action.alternateAddTag);
    }

    public void testInsertHTMLTextActionStringStringTagTag() {
        action = new HTMLEditorKit.InsertHTMLTextAction(name, html,
                                                        parentTag, addTag);
        assertSame(name, action.getValue(AbstractAction.NAME));
        assertSame(html, action.html);
        assertSame(parentTag, action.parentTag);
        assertSame(addTag, action.addTag);
        assertNull(action.alternateParentTag);
        assertNull(action.alternateAddTag);
    }

    public void testActionPerformed() {
        action.actionPerformed(null);
        // TODO: implement
    }

    public void testInsertHTML() throws BadLocationException {
        action.insertHTML(pane, document, 0, html, 0, 0, addTag);
        assertEquals("html text", document.getText(0, document.getLength()));

        testExceptionalCase(new ExceptionalCase() {
            public Class expectedExceptionClass() {
                return RuntimeException.class;
            }
            public void exceptionalAction() throws Exception {
                action.insertHTML(pane, document, document.getLength() + 1,
                                  html, 0, 0, addTag);
            }
        });
    }

    public void testInsertAtBoundary() {
        // TODO: implement
    }
}
