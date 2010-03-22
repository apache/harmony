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
 * @author Roman I. Chernyatchik
 */
package javax.swing.text.html;

import java.awt.ComponentOrientation;
import java.io.StringReader;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingTestCase;
import javax.swing.text.Element;

public class FormView_FormTextAreaElementTest extends SwingTestCase {

    private static final int DEFAULT_COLS_COUNT = 20;
    private static final int DEFAULT_ROWS_COUNT = 3;

    private HTMLDocument document;
    private HTMLEditorKit htmlEditorKit;
    private JEditorPane editorPane;

    private Element elem;

    private FormView formView;

    private JScrollPane scrollPane;
    private JTextArea textArea;

    protected void setUp() throws Exception {
       super.setUp();
       setIgnoreNotImplemented(true);

       htmlEditorKit = new HTMLEditorKit();
       editorPane = new JEditorPane();
       editorPane.setEditorKit(htmlEditorKit);
       document = (HTMLDocument) editorPane.getDocument();
    }

    public void testCreateComponent() throws Exception {
        createHTMLSample();

        //ROWS, COLUMNS
        createFormViewWithParent("textarea");
            assertEquals(DEFAULT_COLS_COUNT, textArea.getColumns());

       if (isHarmony()) {
            assertEquals(DEFAULT_ROWS_COUNT, textArea.getRows());
       } else {
           assertEquals(1, textArea.getRows());
       }

       if (isHarmony()) {
            assertEquals(scrollPane.getPreferredSize(),
                         scrollPane.getMaximumSize());
            assertEquals(scrollPane.getPreferredSize(),
                         scrollPane.getMinimumSize());
        }
        assertEquals("", textArea.getText());

        createFormViewWithParent("textarea_cols");
        assertEquals(30, textArea.getColumns());

        createFormViewWithParent("textarea_rows");
        assertEquals(7, textArea.getRows());

        if (isHarmony()) {
            // READONLY
            createFormViewWithParent("textarea_readonly");
            assertEquals(30, textArea.getColumns());
            assertEquals(7, textArea.getRows());
            assertFalse(textArea.isEditable());

            // DISABLED
            createFormViewWithParent("textarea_disabled");
            assertFalse(textArea.isEnabled());

            // TITLE
            createFormViewWithParent("textarea_title");
            assertEquals("title_title", textArea.getToolTipText());

            //DIR
            createFormViewWithParent("textarea_rtl");
            assertEquals(ComponentOrientation.RIGHT_TO_LEFT,
                         textArea.getComponentOrientation());
        }

        //ALIGN
        createFormViewWithParent("textarea_align_top");
        assertEquals(JComponent.CENTER_ALIGNMENT,
                textArea.getAlignmentX(),
                0.0001);
        assertEquals(JComponent.CENTER_ALIGNMENT,
                scrollPane.getAlignmentX(),
                0.0001);
        assertEquals(JComponent.CENTER_ALIGNMENT,
                textArea.getAlignmentY(),
                0.0001);
        assertEquals(JComponent.BOTTOM_ALIGNMENT,
                scrollPane.getAlignmentY(),
                0.0001);
    }


    private void createFormViewWithParent(final String id) {
        elem = document.getElement(id);
        formView = new FormView(elem);
        formView.setParent(editorPane.getUI().getRootView(editorPane));
        scrollPane = (JScrollPane) formView.createComponent();
        textArea = (JTextArea) scrollPane.getViewport().getComponent(0);
    }

    private void createHTMLSample() throws Exception {
        StringBuffer htmlSrc = new StringBuffer();
        htmlSrc.append("<HTML>");
        htmlSrc.append("<HEAD></HEAD>");
        htmlSrc.append("<BODY>");
        htmlSrc.append("   Hello word!");
        htmlSrc.append("<FORM ACTION = \"\">");
        htmlSrc.append("    <TEXTAREA "
                + "ID=\"textarea\">"
                + "</TEXTAREA>");
        htmlSrc.append("    <TEXTAREA "
                + "NAME =\"textarea_name_only\"  "
                + "ID=\"textarea_name_only\">"
                + "</TEXTAREA>");
        htmlSrc.append("    <TEXTAREA "
                + "NAME =\"textarea_name\" "
                + "COLS=\"30\""
                + "ID=\"textarea_cols\">"
                + "textarea_cols"
                + "</TEXTAREA>");
        htmlSrc.append("    <TEXTAREA "
                + "NAME =\"textarea_name\" "
                + "ROWS=\"7\""
                + "ID=\"textarea_rows\">"
                + "textarea_rows"
                + "</TEXTAREA>");
        htmlSrc.append("    <TEXTAREA "
                + "NAME =\"textarea_name\" "
                + "ROWS=\"7\" COLS=\"30\""
                + "ID=\"textarea_default\">"
                + "textarea_default"
                + "</TEXTAREA>");
        htmlSrc.append("    <TEXTAREA "
                + "READONLY "
                + "NAME =\"textarea_name\" "
                + "ROWS=\"7\" COLS=\"30\""
                + "ID=\"textarea_readonly\">"
                + "textarea_readonly"
                + "</TEXTAREA>");
        htmlSrc.append("    <TEXTAREA "
                + "DISABLED "
                + "NAME =\"textarea_name\" "
                + "ROWS=\"7\" COLS=\"30\""
                + "ID=\"textarea_disabled\">"
                + "textarea_disabled"
                + "</TEXTAREA>");
        htmlSrc.append("    <TEXTAREA "
                + "TABINDEX = \"1\" "
                + "NAME =\"textarea_name\" "
                + "ID=\"textarea_tabindex\">"
                + "textarea_tabindex"
                + "</TEXTAREA>");
        htmlSrc.append("    <TEXTAREA "
                + "ACCESSKEY=\"U\" "
                + "NAME =\"textarea_name\" "
                + "ID=\"textarea_accesskey\">"
                + "textarea_accesskey"
                + "</TEXTAREA>");
        htmlSrc.append("    <TEXTAREA "
                + "TITLE = \"title_title\" "
                + "NAME =\"textarea_name\" "
                + "ID=\"textarea_title\">"
                + "textarea_title"
                + "</TEXTAREA>");
        htmlSrc.append("    <TEXTAREA "
                + "DIR = \"rtl\" "
                + "NAME =\"textarea_name\" "
                + "ID=\"textarea_rtl\">"
                + "textarea_rtl"
                + "</TEXTAREA>");
        htmlSrc.append("    <TEXTAREA "
                + "NAME =\"textarea_name\" "
                + "VALUE = \"textarea_value\" "
                + "ROWS=\"7\" COLS=\"30\""
                + "ID=\"textarea_value\">"
                + "</TEXTAREA>");
        htmlSrc.append("    <TEXTAREA "
                + "NAME =\"textarea_align_top\" "
                + "VALUE = \"textarea_align_top\" "
                + "ROWS=\"7\" COLS=\"30\""
                + "ALIGN = \"top\""
                + "ID=\"textarea_align_top\">"
                + "top"
                + "</TEXTAREA>");
        htmlSrc.append("</FORM>");
        htmlSrc.append("</BODY>");
        htmlSrc.append("</HTML>");

        StringReader reader = new StringReader(htmlSrc.toString());
        htmlEditorKit.read(reader, document, 0);
    }
}
