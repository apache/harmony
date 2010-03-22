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
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.io.StringReader;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.DefaultButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingTestCase;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleConstants;

import org.apache.harmony.x.swing.Utilities;

public class FormView_FormInputElementTest extends SwingTestCase {

    private static final char MEAN_CHAR = 'z';
    private static final int DEFAULT_TEXTFIELD_SIZE = 20;
    private static final String BROWSE_BUTTON_DEFAULT_TEXT = "Browse...";
    private static final String SUBMIT_DEFAULT_TEXT = "Submit Query";
    private static final String RESET_DEFAULT_TEXT = "Reset";

    private HTMLDocument document;
    private HTMLEditorKit htmlEditorKit;
    private JEditorPane editorPane;

    private Element elem;

    private FormView formView;

    protected void setUp() throws Exception {
       super.setUp();
       
       setIgnoreNotImplemented(true);

       htmlEditorKit = new HTMLEditorKit();
       editorPane = new JEditorPane();
       editorPane.setEditorKit(htmlEditorKit);
       document = (HTMLDocument) editorPane.getDocument();
    }

    public void testCreateButtonComponent() throws Exception {
        createHTMLSample("button");
        JButton button;

        if (isHarmony()) {
            // VALUE
            createFormViewWithParent("button_default");
            button = (JButton) formView.createComponent();
            assertEquals("button_default", button.getText());
            assertNull(button.getToolTipText());
            assertEquals(button.getPreferredSize(),
                    button.getMaximumSize());
            assertEquals(button.getPreferredSize(),
                    button.getMinimumSize());

            //ACCESSKEY
            checkButtonAccessKey("button_accesskey");

            //TITLE
            checkTitle("button_title");

            // SIZE
            checkButtonSize("button_size");

            // ALIGN
            checkButtonAligns("button");

            // DISABLED
            checkDisabled("button_disabled");
        }
    }

    public void testCreateCheckBoxComponent() throws Exception {
        createHTMLSample("checkbox");
        JCheckBox checkBox;

        // VALUE
        createFormViewWithParent("checkbox_default");
        checkBox = (JCheckBox) formView.createComponent();
        assertEquals("", checkBox.getText());
        assertEquals(checkBox.getPreferredSize(),
                checkBox.getMaximumSize());
        assertEquals(checkBox.getPreferredSize(),
                checkBox.getMinimumSize());
        assertNull(checkBox.getToolTipText());

        // CHECKED
        checkChecked("checkbox_checked");

        // ACCESSKEY
        checkButtonAccessKey("checkbox_accesskey");

        // TITLE
        checkTitle("checkbox_title");

        // SIZE
        checkButtonSize("checkbox_size");

        // ALIGN
        checkButtonAligns("checkbox");

        // DISABLED
        checkDisabled("checkbox_disabled");

    }

    public void testCreateImageComponent() throws Exception {
        createHTMLSample("image");
        JButton image;

        // VALUE
        createFormViewWithParent("image_src");
        image = (JButton) formView.createComponent();
        assertEquals("", image.getText());

        createFormViewWithParent("image_default");
        image = (JButton) formView.createComponent();
        assertEquals("", image.getText());

        // ACCESSKEY
        checkButtonAccessKey("image_accesskey");

        // TITLE
        checkTitle("image_title");
        createFormViewWithParent("image_alt");
        image = (JButton) formView.createComponent();
        if (isHarmony()) {
            assertEquals("alt_attribute", image.getToolTipText());
        } else {
            assertNull(image.getToolTipText());
        }
        createFormViewWithParent("image_alt_title");
        image = (JButton) formView.createComponent();
        if (isHarmony()) {
            assertEquals("title_attribute", image.getToolTipText());
        } else {
            assertNull(image.getToolTipText());
        }

        // ALT
        createFormViewWithParent("image_alt");
        image = (JButton) formView.createComponent();
        if (isHarmony()) {
            assertEquals("alt_attribute", image.getText());
        } else {
            assertEquals("", image.getText());
        }

        // SIZE
        checkButtonSize("image_size");


        // SRC
        createFormViewWithParent("image_src");
        image = (JButton) formView.createComponent();
        if (isHarmony()) {
           assertNotNull(image.getIcon());
        } else {
           assertNull(image.getIcon());
        }
        createFormViewWithParent("image_alt");
        if (isHarmony()) {
            assertNotNull(image.getIcon());
        } else {
            assertNull(image.getIcon());
        }

        // ALIGN
        checkButtonAligns("image");

        // DISABLED
        checkDisabled("image_disabled");
    }

    public void testCreatePasswordComponent() throws Exception {
        JPasswordField passwordField;
        createHTMLSample("password");

        // VALUE
        createFormViewWithParent("password_default");
        passwordField = (JPasswordField) formView.createComponent();
        assertEquals("password_default", passwordField.getText());
        assertNull(passwordField.getToolTipText());

        // READONLY
        checkTextReadonly("password_readonly");

        createFormViewWithParent("password_accesskey");
        passwordField = (JPasswordField) formView.createComponent();

        // TITLE
        checkTitle("password_title");

        //SIZE
        checkTextSize("password", passwordField.getEchoChar());

        //DIR
        checkTextDir("password_rtl");

        // ALIGN
        checkTextAligns("password");

        // DISABLED
        checkDisabled("password_disabled");
    }

    public void testCreateRadioComponent() throws Exception {
        createHTMLSample("radio");
        JRadioButton radioButton;

        // VALUE
        createFormViewWithParent("radio_default");
        radioButton = (JRadioButton) formView.createComponent();
        assertEquals("", radioButton.getText());
        assertNull(radioButton.getToolTipText());

        // CHECKED
        checkChecked("radio_checked");

        // ACCESSKEY
        checkButtonAccessKey("radio_accesskey");

        // SIZE
        checkButtonSize("radio_size");

        //TITLE
        checkTitle("radio_title");

        // ALIGN
        checkButtonAligns("radio");

        // DISABLED
        checkDisabled("radio_disabled");
    }

    public void testRadioGroups() throws Exception {
        StringBuffer htmlSrc = new StringBuffer();
        htmlSrc.append("<HTML> <HEAD></HEAD><BODY>");
        htmlSrc.append("   Hello word!");
        htmlSrc.append("<FORM ACTION = \"\">");
        htmlSrc.append("    <INPUT TYPE = \"radio\" NAME =\"name\" ID=\"1\">");
        htmlSrc.append("    <INPUT TYPE = \"radio\" NAME =\"name\" ID=\"2\">");
        htmlSrc.append("    <INPUT TYPE = \"radio\" NAME =\"name\" ID=\"3\">");
        htmlSrc.append("</FORM></BODY></HTML>");
        StringReader reader = new StringReader(htmlSrc.toString());
        htmlEditorKit.read(reader, document, 0);

        AttributeSet attrs = document.getElement("1").getAttributes();
        DefaultButtonModel sourceModel1;
        sourceModel1 = (DefaultButtonModel) attrs.getAttribute(StyleConstants
                .ModelAttribute);
        assertEquals(0, sourceModel1.getGroup().getButtonCount());
        attrs = document.getElement("2").getAttributes();
        DefaultButtonModel sourceModel2;
        sourceModel2 = (DefaultButtonModel) attrs.getAttribute(StyleConstants
                .ModelAttribute);
        assertEquals(0, sourceModel2.getGroup().getButtonCount());

        createFormViewWithParent("1");
        assertEquals(0, sourceModel1.getGroup().getButtonCount());
    }

    public void testCreateResetComponent() throws Exception {
        createHTMLSample("reset");
        JButton resetButton;

        //Default VALUE
        createFormViewWithParent("reset");
        resetButton = (JButton) formView.createComponent();
        assertEquals(RESET_DEFAULT_TEXT,  resetButton.getText());

        // VALUE
        createFormViewWithParent("reset_default");
        resetButton = (JButton) formView.createComponent();
        assertEquals("reset_default", resetButton.getText());
        assertNull(resetButton.getToolTipText());

        // ACCESSKEY
        checkButtonAccessKey("reset_accesskey");

        // SIZE
        checkButtonSize("reset_size");

        //TITLE
        checkTitle("reset_title");

        // ALIGN
        checkButtonAligns("reset");

        // DISABLED
        checkDisabled("reset_disabled");
    }

    public void testCreateSubmitComponent() throws Exception {
        createHTMLSample("submit");
        JButton submitButton;

        //Default VALUE
        createFormViewWithParent("submit");
        submitButton = (JButton) formView.createComponent();
        assertEquals(SUBMIT_DEFAULT_TEXT,  submitButton.getText());

        // VALUE
        createFormViewWithParent("submit_default");
        submitButton = (JButton) formView.createComponent();
        assertEquals("submit_default", submitButton.getText());
        assertNull(submitButton.getToolTipText());

        // ACCESSKEY
        checkButtonAccessKey("submit_accesskey");

        // SIZE
        checkButtonSize("submit_size");

        //TITLE
        checkTitle("submit_title");

        // ALIGN
        checkButtonAlign("submit");

        // DISABLED
        checkDisabled("submit_disabled");
    }

    public void testCreateTextComponent() throws Exception {
        JTextField textField;
        createHTMLSample("text");

        // VALUE
        createFormViewWithParent("text_default");
        textField = (JTextField) formView.createComponent();
        assertEquals("text_default", textField.getText());
        assertNull(textField.getToolTipText());

        // DISABLED
        checkDisabled("text_disabled");

        // READONLY
        checkTextReadonly("text_readonly");

        //SIZE
        checkTextSize("text", MEAN_CHAR);

        // TITLE
        checkTitle("text_title");

        // ALIGN
        checkTextAligns("text");

        //DIR
        checkTextDir("text_rtl");
    }

    public void testCreateFileComponent() throws Exception {
        createHTMLSample("file");

        //Default VALUE
        createFormViewWithParent("file");
        Box box = (Box)formView.createComponent();;
        JTextField filePath = (JTextField)box.getComponent(0);
        JButton browseButton = (JButton)box.getComponent(2);
        assertEquals("", filePath.getText());
        assertEquals(BROWSE_BUTTON_DEFAULT_TEXT,  browseButton.getText());

        //VALUE
        createFormViewWithParent("file_default");
        box = (Box)formView.createComponent();;
        filePath = (JTextField)box.getComponent(0);
        browseButton = (JButton)box.getComponent(2);
        checkTextSize(filePath, MEAN_CHAR, false);
        assertEquals(BROWSE_BUTTON_DEFAULT_TEXT,  browseButton.getText());

        // SIZE
        createFormViewWithParent("file_size");
        box = (Box)formView.createComponent();;
        filePath = (JTextField)box.getComponent(0);
        Box.Filler filler = (Box.Filler)box.getComponent(1);
        browseButton = (JButton)box.getComponent(2);
        assertEquals(new Dimension(5, 0), filler.getMinimumSize());
        assertEquals(new Dimension(5, 0), filler.getPreferredSize());
        assertEquals(new Dimension(5, 32767), filler.getMaximumSize());
        checkTextSize(filePath, MEAN_CHAR, true);

        // DIR
        createFormViewWithParent("file_rtl");
        box = (Box)formView.createComponent();;
        filePath = (JTextField)box.getComponent(0);
        if (isHarmony()) {
            assertEquals(box.getComponentOrientation(),
                         ComponentOrientation.RIGHT_TO_LEFT);
            assertEquals(filePath.getComponentOrientation(),
                    ComponentOrientation.RIGHT_TO_LEFT);
        }

        // READONLY
        createFormViewWithParent("file_readonly");
        box = (Box)formView.createComponent();;
        filePath = (JTextField)box.getComponent(0);
        if (isHarmony()) {
            assertFalse(filePath.isEditable());
        } else {
            assertTrue(filePath.isEditable());
        }

        // TITLE
        createFormViewWithParent("file_title");
        box = (Box)formView.createComponent();
        filePath = (JTextField)box.getComponent(0);
        browseButton = (JButton)box.getComponent(2);
        if (isHarmony()) {
            assertEquals("title_attribute",
                         browseButton.getToolTipText());
            assertEquals("title_attribute",
                         filePath.getToolTipText());
        } else {
            assertNull(browseButton.getToolTipText());
            assertNull(filePath.getToolTipText());
        }

        // ALIGN
        createFormViewWithParent("file_default");
        checkFileAlign();
        createFormViewWithParent("file_align_top");
        checkFileAlign();
        createFormViewWithParent("file_align_right");
        checkFileAlign();
        createFormViewWithParent("file_align_justify");
        checkFileAlign();

        // DISABLED
        createFormViewWithParent("file_disabled");
        box = (Box)formView.createComponent();;
        filePath = (JTextField)box.getComponent(0);
        browseButton = (JButton)box.getComponent(2);
        if (isHarmony()) {
            assertFalse(browseButton.isEnabled());
            assertFalse(filePath.isEnabled());
        } else {
            assertTrue(browseButton.isEnabled());
            assertTrue(filePath.isEnabled());
        }
    }

    private void checkFileAlign() {
        Box box = (Box)formView.createComponent();
        JTextField filePath = (JTextField)box.getComponent(0);
        JButton browseButton = (JButton)box.getComponent(2);
        checkAlign(box, JComponent.CENTER_ALIGNMENT,
                   JComponent.BOTTOM_ALIGNMENT);
        checkAlign(filePath, JComponent.CENTER_ALIGNMENT,
                   JComponent.CENTER_ALIGNMENT);
        checkAlign(browseButton, JComponent.LEFT_ALIGNMENT,
                   JComponent.CENTER_ALIGNMENT);
    }

    private void checkAlign(final JComponent component,
                           final float alignmentX, final float alignmentY) {
        assertEquals(alignmentX, component.getAlignmentX(), 0.0001);
        assertEquals(alignmentY, component.getAlignmentY(), 0.0001);
    }

    private void checkDisabled(final String id) {
        createFormViewWithParent(id);
        JComponent component = (JComponent) formView.createComponent();
        if (isHarmony()) {
            assertFalse(component.isEnabled());
        } else {
            assertTrue(component.isEnabled());
        }
    }

    private void checkChecked(final String id) {
        JToggleButton togleButton;
        createFormViewWithParent(id);
        togleButton = (JToggleButton) formView.createComponent();
        assertTrue(togleButton.isSelected());
    }

    private void checkButtonAccessKey(final String id) {
        AbstractButton button;
        if (isHarmony()) {
            createFormViewWithParent(id);
            button = (AbstractButton) formView.createComponent();
            assertEquals(Utilities.keyCodeToKeyChar(button.getMnemonic()),
            'U');
        }
    }

    private void checkButtonAlign(final String id) {
        createFormViewWithParent(id);
        checkAlign((JComponent) formView.createComponent(),
                        JComponent.LEFT_ALIGNMENT,
                        JComponent.BOTTOM_ALIGNMENT);
    }

    private void checkButtonAligns(final String type) {
        checkButtonAlign(type + "_default");
        checkButtonAlign(type + "_align_top");
        checkButtonAlign(type + "_align_right");
        checkButtonAlign(type + "_align_justify");
    }

    private void checkButtonSize(final String id) {
        createFormViewWithParent(id);
        JComponent component = (JComponent) formView.createComponent();
        if (isHarmony()) {
          assertEquals(component.getPreferredSize().width,
                       100);
        }
        assertEquals(component.getPreferredSize(),
                component.getMaximumSize());
        assertEquals(component.getPreferredSize(),
                component.getMinimumSize());
    }

    private void checkTextAlign(final String id) {
        createFormViewWithParent(id);
        checkAlign((JComponent) formView.createComponent(),
                  JComponent.CENTER_ALIGNMENT,
                  JComponent.BOTTOM_ALIGNMENT);
    }

    private void checkTextAligns(final String type) {
        checkTextAlign(type + "_default");
        checkTextAlign(type + "_align_top");
        checkTextAlign(type + "_align_right");
        checkTextAlign(type + "_align_justify");
    }

    private void checkTextDir(final String id) {
        createFormViewWithParent(id);
        JTextField passwordField = (JTextField) formView.createComponent();
        if (isHarmony()) {
            assertEquals(passwordField.getComponentOrientation(),
                         ComponentOrientation.RIGHT_TO_LEFT);
        }
    }

    private void checkTextReadonly(final String id) {
        JTextComponent textComponent;
        createFormViewWithParent(id);
        textComponent = (JTextComponent) formView.createComponent();
        if (isHarmony()) {
            assertFalse(textComponent.isEditable());
        } else {
            assertTrue(textComponent.isEditable());
        }
    }

    private void checkTextSize(final String type, final char ch) {
        JTextComponent textComponent;

        createFormViewWithParent(type + "_size");
        textComponent = (JTextComponent) formView.createComponent();
        checkTextSize(textComponent, ch, true);

        createFormViewWithParent(type + "_default");
        textComponent = (JTextComponent) formView.createComponent();
        checkTextSize(textComponent, ch, false);

    }
    private void checkTextSize(final JTextComponent textComponent,
                              final char ch, final boolean sizeWasSet) {
        final FontMetrics fontMetrics
            = textComponent.getFontMetrics(textComponent.getFont());
        final int charWidth = fontMetrics.charWidth(ch);
        Dimension size = textComponent.getPreferredSize();


        size.width = (sizeWasSet ? 100 : DEFAULT_TEXTFIELD_SIZE) * charWidth;
        if (isHarmony()) {
            assertEquals(size, textComponent.getPreferredSize());
            assertEquals(size, textComponent.getMaximumSize());
            size = new Dimension(DEFAULT_TEXTFIELD_SIZE * charWidth,
                                 size.height);
            assertEquals(size, textComponent.getMinimumSize());
        }
    }

    private void checkTitle(final String id) {
        createFormViewWithParent(id);
        JComponent checkBox = (JComponent) formView.createComponent();
        if (isHarmony()) {
            assertEquals("title_attribute", checkBox.getToolTipText());
        } else {
            assertNull(checkBox.getToolTipText());
        }
    }

    private void createFormViewWithParent(final String id) {
        elem = document.getElement(id);
        formView = new FormView(elem);
        formView.setParent(editorPane.getUI().getRootView(editorPane));
    }

    private void createHTMLSample(final String typeName) throws Exception {
        StringBuffer htmlSrc = new StringBuffer();
        htmlSrc.append("<HTML>");
        htmlSrc.append("<HEAD></HEAD>");
        htmlSrc.append("<BODY>");
        htmlSrc.append("   Hello word!");
        htmlSrc.append("<FORM ACTION = \"\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" "
                + "ID=\"" + typeName + "\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\""
                + "NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "_default\" "
                + "ID=\"" + typeName + "_default\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\""
                + "NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "_align_top\" "
                + "ALIGN = \"top\""
                + "ID=\"" + typeName + "_align_top\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\""
                + "NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "_align_right\" "
                + "ALIGN = \"right\""
                + "ID=\"" + typeName + "_align_right\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\""
                + "NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "_align_justify\" "
                + "ALIGN = \"justify\""
                + "ID=\"" + typeName + "_align_justify\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" "
                + "NAME =\"\" "
                + "VALUE = \"" + typeName + "_empty_name\" "
                + "ID=\"" + typeName + "_empty_name\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" "
                + "NAME =\"" + typeName + "_name\"  "
                + "ID=\"" + typeName + "_name_only\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" "
                + "SIZE = \"100\" "
                + "NAME =\"" + typeName + "_name1\" "
                + "VALUE = \"" + typeName + "_size\" "
                + "ID=\"" + typeName + "_size\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName
                + "\" MAXLENGTH = \"10\" "
                + "NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "_maxlength\" "
                + "ID=\"" + typeName + "_maxlength\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" CHECKED "
                + "NAME =\"" + typeName + "_name\"" + ""
                + "VALUE = \"" + typeName + "_checked\" "
                + "ID=\"" + typeName + "_checked\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" "
                + "SRC = \"\" "
                + "NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "_src\""
                + "ID=\"" + typeName + "_src\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" "
                + "DIR = \"rtl\" "
                + "NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "_rtl\" "
                + "ID=\"" + typeName + "_rtl\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" "
                + "ALT = \"alt_attribute\" NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "_alt\" "
                + "ID=\"" + typeName + "_alt\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" "
                + "ACCESSKEY=\"U\" "
                + "NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "_accesskey\" "
                + "ID=\"" + typeName + "_accesskey\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" "
                + "TABINDEX = \"1\" "
                + "NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "_tabindex\" "
                + "ID=\"" + typeName + "_tabindex\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" "
                + "TITLE = \"title_attribute\" NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "_title\" "
                + "ID=\"" + typeName + "_title\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" "
                + "ALIGN = \"right\" NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "\" "
                + "ID=\"" + typeName + "_align_right\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" "
                + "ALIGN = \"center\" NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "\" "
                + "ID=\"" + typeName + "_align_center\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" READONLY "
                + "NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "_readonly\" "
                + "ID=\"" + typeName + "_readonly\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" DISABLED "
                + "NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "_disabled\" "
                + "ID=\"" + typeName + "_disabled\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" "
                + "ALT = \"alt_attribute\" "
                + "TITLE = \"title_attribute\" "
                + "NAME =\"" + typeName + "_name\" "
                + "VALUE = \"" + typeName + "_alt_title\" "
                + "ID=\"" + typeName + "_alt_title\">");
        htmlSrc.append("    <INPUT TYPE = \"" + typeName + "\" "
                + "USEMAP=\"#map\">");
        htmlSrc.append("    <INPUT TYPE = \"submit\" NAME =\"submit_name\" "
                + "VALUE = \"submit:JTextField\" "
                + "ID=\"submit\">");
        htmlSrc.append("</FORM>");
        htmlSrc.append("<MAP name=\"map\">");
        htmlSrc.append("    <P> map_title");
        htmlSrc.append("    <A href=\"\" shape=\"rect\" coords=\"0,0,100,50\"> "
                        + "href</A>");
        htmlSrc.append("</MAP>");
        htmlSrc.append("</BODY>");
        htmlSrc.append("</HTML>");

        StringReader reader = new StringReader(htmlSrc.toString());
        htmlEditorKit.read(reader, document, 0);
    }
}
