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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.StringReader;

import javax.swing.Box;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingTestCase;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;

import org.apache.harmony.x.swing.text.html.form.FormAttributes;

public class FormViewTest extends SwingTestCase {

    private HTMLDocument doc;

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
        doc = (HTMLDocument) editorPane.getDocument();

        StringBuffer src = new StringBuffer();
        src.append("<HTML>");
        src.append("<HEAD></HEAD>");
        src.append("<BODY>");
        src.append("   Hello word!");
        src.append("<FORM ACTION = \"\">");
        src.append("    <INPUT TYPE = \"button\" NAME =\"button_name\" "
                   + "VALUE = \"button:JButton\" ID=\"button\">");
        src.append("    <INPUT TYPE = \"checkbox\" NAME =\"checkbox_name\" "
                   + "VALUE = \"checkbox:JCheckBox\" ID=\"checkbox\">");
        src.append("    <INPUT TYPE = \"checkbox\" NAME =\"checked "
                   + "checkbox_name\" VALUE = \"checked checkbox:JCheckBox\" "
                   + "CHECKED ID=\"checked checkbox\">");
        src.append("    <INPUT TYPE = \"image\" NAME =\"image_name\" "
                   + "VALUE = \"image:JButton\" ID=\"image\">");
        src.append("    <INPUT TYPE = \"password\" NAME =\"password_name\" "
                   + "VALUE = \"password:JPasswordField\" ID=\"password\">");
        src.append("    <INPUT TYPE = \"radio\" NAME =\"radio1_name\" "
                   + "value = \"radio:JRadioButton\" ID=\"radio\">");
        src.append("    <INPUT TYPE = \"radio\" NAME =\"radio2_name\" "
                   + "value = \"radio:JRadioButton\" ID=\"radio2\">");
        src.append("    <INPUT TYPE = \"radio\" NAME =\"checked radio_name\" "
                   + "value = \"checked radio:JRadioButton\" CHECKED "
                   + "ID=\"checked radio\">");
        src.append("    <INPUT TYPE = \"reset\" NAME =\"reset_name\" "
                   + "VALUE = \"reset:JButton\" ID=\"reset\">");
        src.append("    <INPUT TYPE = \"submit\" NAME =\"submit_name\" "
                   + "VALUE = \"submit:JButton\" ID=\"submit\">");
        src.append("    <INPUT TYPE = \"text\" NAME =\"text_name\" "
                   + "VALUE = \"text:JTextField\" ID=\"text\">");
        src.append("    <INPUT TYPE = \"text\" "
                   + "NAME =\"disabled_text_name\" disabled VALUE = "
                   + "\"disabled_text:JTextField\" ID=\"disabled_text\">");
        src.append("    <INPUT TYPE = \"file\" NAME =\"file_name\" "
                   + "VALUE = \"file:JTextField\" ID=\"file\">");
        src.append("    <SELECT NAME = \"select_name\" ID=\"select_0\">");
        src.append("    </SELECT>");
        src.append("    <SELECT NAME = \"select_name\" ID=\"select\">");
        src.append("        <OPTION VALUE = \"case1\" SELECTED> case1");
        src.append("    </SELECT>");
        src.append("    <SELECT NAME = \"select_name\" ID=\"select_many\">");
        src.append("        <OPTION VALUE = \"case1\"> case1");
        src.append("        <OPTION VALUE = \"case2\" SELECTED> case2");
        src.append("        <OPTION VALUE = \"case3\" SELECTED> case3");
        src.append("    </SELECT>");
        src.append("    <SELECT NAME = \"select_name\" MULTIPLE "
                   + "ID=\"select_multiple\">");
        src.append("        <OPTION VALUE = \"case1\"> case1");
        src.append("    </SELECT>");
        src.append("    <SELECT NAME = \"select_name\" MULTIPLE "
                   + "ID=\"select_multiple_many\">");
        src.append("        <OPTION VALUE = \"case1\"> case1");
        src.append("        <OPTION VALUE = \"case2\" SELECTED> case2");
        src.append("        <OPTION VALUE = \"case3\" SELECTED> case3");
        src.append("    </SELECT>");
        src.append("    <TEXTAREA NAME = \"textarea_name\" WRAP=\"virtual\" "
                   + "COLS=\"50\" ROWS=\"5\" ID=\"textArea\">");
        src.append("         JTextArea in a JScrollPane");
        src.append("    </TEXTAREA>");
        src.append("    <INPUT TYPE = \"text\" NAME =\"text_maxlength\" "
                   + "MAXLENGTH = \"10\" VALUE = \"text:JTextField\" "
                   + "ID=\"text_maxlength\">");
        src.append("    <INPUT TYPE = \"submit\" NAME =\"submit3_name\" "
                   + "VALUE = \"submit2:JButton\" ID=\"submit2\">");
        src.append("</FORM>");
        src.append("    <INPUT TYPE = \"submit\" NAME =\"submit3_name\" "
                   + "VALUE = \"submit3:JButton\" ID=\"submit3\">");
        src.append("</BODY>");
        src.append("</HTML>");
        StringReader reader = new StringReader(src.toString());
        htmlEditorKit.read(reader, doc, 0);
    }

    public void testFormView() throws Exception {

        // Wrong element
        elem = createElement();
        formView = new FormView(elem);
        assertNull(formView.getComponent());
        assertSame(elem, formView.getElement());
        assertSame(elem.getAttributes(), formView.getAttributes());
        assertNull(formView.createComponent());
        assertNull(formView.getComponent());
        assertNull(getModelAttribute(elem));
        assertNull(StyleConstants.getComponent(elem.getAttributes()));

        formView.setParent(editorPane.getUI().getRootView(editorPane));
        assertNull(StyleConstants.getComponent(elem.getAttributes()));
        assertNull(formView.createComponent());
        assertNull(formView.getComponent());
        assertNull(getModelAttribute(elem));

        // Wrong element type name
        createFormView(FormAttributes.INPUT_TYPE_SUBMIT);
        MutableAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(HTML.Attribute.TYPE, "my_type");
        doc.setCharacterAttributes(22, 1, attrs, false);
        assertEquals("my_type",
                     elem.getAttributes().getAttribute(HTML.Attribute.TYPE));
        assertNull(formView.createComponent());
        assertNull(formView.getComponent());
        assertNotNull(getModelAttribute(elem));
        assertNull(StyleConstants.getComponent(elem.getAttributes()));

        formView.setParent(editorPane.getUI().getRootView(editorPane));
        assertNull(StyleConstants.getComponent(elem.getAttributes()));
        assertNull(formView.createComponent());
        assertNull(formView.getComponent());
        assertNotNull(getModelAttribute(elem));

        // Element type "password"
        createFormView(FormAttributes.INPUT_TYPE_PASSWORD);
        assertNull(StyleConstants.getComponent(elem.getAttributes()));
        assertNull(StyleConstants.getComponent(elem.getAttributes()));
        Object modelAttribute = getModelAttribute(elem);
        assertNull(formView.getComponent());
        assertSame(elem, formView.getElement());
        assertSame(elem.getAttributes(), formView.getAttributes());
        Component component = formView.createComponent();
        assertNotNull(component);
        assertNull(formView.getComponent());
        assertSame(modelAttribute, getModelAttribute(elem));
        assertNotSame(component, formView.createComponent());
        component = formView.createComponent();

        formView.setParent(editorPane.getUI().getRootView(editorPane));
        assertNotNull(component);
        Component cashedComponent = formView.getComponent();
        assertNotNull(cashedComponent);
        assertSame(cashedComponent, formView.getComponent());
        assertNotSame(component, cashedComponent);
        assertNotSame(component, formView.createComponent());
        assertNotSame(formView.getComponent(), formView.createComponent());
        assertSame(modelAttribute, getModelAttribute(elem));
        assertSame(cashedComponent, formView.getComponent());

        // Check component attribute
        createFormView(FormAttributes.INPUT_TYPE_TEXT);
        assertNull(StyleConstants.getComponent(elem.getAttributes()));
        JButton button = new JButton();
        attrs = new SimpleAttributeSet();
        StyleConstants.setComponent(attrs, button);
        doc.setCharacterAttributes(23, 1, attrs, false);
        assertNotNull(StyleConstants.getComponent(elem.getAttributes()));
        assertNull(formView.getComponent());

        formView.setParent(editorPane.getUI().getRootView(editorPane));
        component = formView.getComponent();
        assertNotSame(button, component);
        assertTrue(component instanceof JTextField);
        assertNotSame(component,
                      StyleConstants.getComponent(elem.getAttributes()));

        formView.setParent(null);
        assertSame(component, formView.getComponent());
        formView.setParent(editorPane.getUI().getRootView(editorPane));
        assertSame(component, formView.getComponent());
    }

    public void testFormView_Models() throws Exception {
        createFormView(FormAttributes.INPUT_TYPE_BUTTON);
        if (isHarmony()) {
            //TODO uncomment when HTMLDocument would be implemented
            /*
             *  assertNotNull(getModelAttribute(elem));
             */
        } else {
            assertNull(getModelAttribute(elem));
        }

        testModel(FormAttributes.INPUT_TYPE_CHECKBOX);
        testModel(FormAttributes.INPUT_TYPE_IMAGE);
        testModel(FormAttributes.INPUT_TYPE_PASSWORD);
        testModel(FormAttributes.INPUT_TYPE_RADIO);
        testModel(FormAttributes.INPUT_TYPE_RESET);
        testModel(FormAttributes.INPUT_TYPE_SUBMIT);
        testModel(FormAttributes.INPUT_TYPE_TEXT);
        testModel(FormAttributes.INPUT_TYPE_FILE);
        testModel("textArea");
        testModel("select_0");
        testModel("select");
        testModel("select_many");
        testModel("select_multiple");
        testModel("select_multiple_many");

        //HTML 4.0 innovations
        if (isHarmony()) {
            //TODO Button
            //TODO Select with option groups
        }
    }

    public void testCreateComponent() throws Exception {
        Component component;

        createFormView(FormAttributes.INPUT_TYPE_BUTTON);
        if (isHarmony()) {
            component = formView.createComponent();
            assertTrue(component instanceof JButton);
            checkCreateComponent(formView, component);
        } else {
            assertNull(formView.createComponent());
            formView.setParent(editorPane.getUI().getRootView(editorPane));
            assertNull(formView.createComponent());
        }

        createFormView(FormAttributes.INPUT_TYPE_CHECKBOX);
        component = formView.createComponent();
        assertTrue(component instanceof JCheckBox);
        checkCreateComponent(formView, component);

        createFormView(FormAttributes.INPUT_TYPE_IMAGE);
        component = formView.createComponent();
        assertTrue(component instanceof JButton);
        checkCreateComponent(formView, component);

        createFormView(FormAttributes.INPUT_TYPE_PASSWORD);
        component = formView.createComponent();
        assertTrue(component instanceof JPasswordField);
        checkCreateComponent(formView, component);

        createFormView(FormAttributes.INPUT_TYPE_RADIO);
        component = formView.createComponent();
        assertTrue(component instanceof JRadioButton);
        checkCreateComponent(formView, component);

        createFormView(FormAttributes.INPUT_TYPE_RESET);
        component = formView.createComponent();
        assertTrue(component instanceof JButton);
        checkCreateComponent(formView, component);

        createFormView(FormAttributes.INPUT_TYPE_SUBMIT);
        component = formView.createComponent();
        assertTrue(component instanceof JButton);
        checkCreateComponent(formView, component);

        createFormView(FormAttributes.INPUT_TYPE_TEXT);
        component = formView.createComponent();
        assertTrue(component instanceof JTextField);
        checkCreateComponent(formView, component);

        createFormView("text_maxlength");
        component = formView.createComponent();
        assertTrue(component instanceof JTextField);
        assertTrue(getModelAttribute(elem) instanceof PlainDocument);

        createFormView(FormAttributes.INPUT_TYPE_FILE);
        Container container = (Container) formView.createComponent();
        assertTrue(container instanceof Box);
        assertEquals(container.getComponentCount(), 3);
        assertTrue(container.getComponent(0) instanceof JTextField);
        assertTrue(container.getComponent(2) instanceof JButton);
        checkCreateComponent(formView, container);

        createFormView("textArea");
        container = (Container) formView.createComponent();
        assertTrue(container instanceof JScrollPane);
        assertEquals(container.getComponentCount(), 3);
        assertTrue(container.getComponent(0) instanceof JViewport);
        assertTrue(((JViewport) container.getComponent(0)).getComponent(0)
                   instanceof JTextArea);
        checkCreateComponent(formView, container);

        createFormView("select_0");
        component = formView.createComponent();
        assertTrue(component instanceof JComboBox);
        checkCreateComponent(formView, component);

        createFormView("select");
        component = formView.createComponent();
        assertTrue(component instanceof JComboBox);
        checkCreateComponent(formView, component);

        createFormView("select_many");
        component = formView.createComponent();
        assertTrue(component instanceof JComboBox);
        checkCreateComponent(formView, component);

        createFormView("select_multiple");
        container = (Container) formView.createComponent();
        assertTrue(container instanceof JScrollPane);
        assertEquals(container.getComponentCount(), 3);

        assertTrue(container.getComponent(0) instanceof JViewport);
        assertTrue(((JViewport) container.getComponent(0)).getComponent(0)
                   instanceof JList);
        checkCreateComponent(formView, container);

        createFormView("select_multiple_many");
        container = (Container) formView.createComponent();
        assertTrue(container instanceof JScrollPane);
        assertEquals(container.getComponentCount(), 3);

        assertTrue(container.getComponent(0) instanceof JViewport);
        assertTrue(((JViewport) container.getComponent(0)).getComponent(0)
                   instanceof JList);
        checkCreateComponent(formView, container);
    }

    public void testCreateComponent_Models() throws Exception {
        Component component;

        createFormView(FormAttributes.INPUT_TYPE_BUTTON);
        component = formView.createComponent();
        if (isHarmony()) {
            assertSame(getModelAttribute(elem),
                      ((JButton) component).getModel());
             
        } else {
            assertNull(component);
        }

        createFormView(FormAttributes.INPUT_TYPE_CHECKBOX);
        component = formView.createComponent();
        assertSame(getModelAttribute(elem), ((JCheckBox) component).getModel());
        checkWithoutlModel(14,
                           FormAttributes.INPUT_TYPE_CHECKBOX);
        assertNotNull(((JCheckBox)formView.createComponent()).getModel());


        createFormView(FormAttributes.INPUT_TYPE_IMAGE);
        component = formView.createComponent();
        assertSame(getModelAttribute(elem), ((JButton) component).getModel());
        checkWithoutlModel(16,
                           FormAttributes.INPUT_TYPE_IMAGE);
        assertNotNull(((JButton)formView.createComponent()).getModel());


        createFormView(FormAttributes.INPUT_TYPE_PASSWORD);
        component = formView.createComponent();
        assertSame(getModelAttribute(elem), ((JTextField) component)
                .getDocument());
        checkWithoutlModel(17,
                           FormAttributes.INPUT_TYPE_PASSWORD);
        assertNotNull(((JTextField)formView.createComponent()).getDocument());


        createFormView(FormAttributes.INPUT_TYPE_RADIO);
        component = formView.createComponent();
        assertSame(getModelAttribute(elem), ((JRadioButton) component)
                .getModel());
        checkWithoutlModel(18,
                           FormAttributes.INPUT_TYPE_RADIO);
        assertNotNull(((JRadioButton)formView.createComponent()).getModel());


        createFormView(FormAttributes.INPUT_TYPE_RESET);
        component = formView.createComponent();
        assertSame(getModelAttribute(elem), ((JButton) component).getModel());
        checkWithoutlModel(21,
                           FormAttributes.INPUT_TYPE_RESET);
        assertNotNull(((JButton)formView.createComponent()).getModel());


        createFormView(FormAttributes.INPUT_TYPE_SUBMIT);
        component = formView.createComponent();
        assertSame(getModelAttribute(elem), ((JButton) component).getModel());
        checkWithoutlModel(22,
                           FormAttributes.INPUT_TYPE_SUBMIT);
        assertNotNull(((JButton)formView.createComponent()).getModel());


        createFormView(FormAttributes.INPUT_TYPE_TEXT);
        component = formView.createComponent();
        assertSame(getModelAttribute(elem), ((JTextField) component)
                .getDocument());
        checkWithoutlModel(23,
                FormAttributes.INPUT_TYPE_TEXT);
        assertNotNull(((JTextField)formView.createComponent()).getDocument());

        createFormView(FormAttributes.INPUT_TYPE_FILE);
        component = formView.createComponent();
        assertSame(getModelAttribute(elem),
                ((JTextField) ((Container) component).getComponent(0))
                        .getDocument());
        checkWithoutlModel(25,
                FormAttributes.INPUT_TYPE_FILE);
        Component c = ((Box)formView.createComponent()).getComponent(0);
        assertNotNull(((JTextField)c).getDocument());

        createFormView("textArea");
        component = formView.createComponent();
        assertSame(getModelAttribute(elem),
                ((JTextArea) ((Container) ((Container) component)
                        .getComponent(0)).getComponent(0)).getDocument());
        checkWithoutlModel(31, "textArea", "textarea");
        JViewport viewPort = ((JScrollPane)formView.createComponent()).getViewport();
        assertNotNull(((JTextArea)viewPort.getComponent(0)).getDocument());

        createFormView("select_0");
        component = formView.createComponent();
        assertSame(getModelAttribute(elem), ((JComboBox) component).getModel());
        if (isHarmony()) {
            checkWithoutlModel(26, "select_0", "select");
        } else {
            testExceptionalCase(new NullPointerCase() {
                public void exceptionalAction() throws Exception {
                    checkWithoutlModel(26, "select_0", "select");
                }
            });
        }

        createFormView("select");
        component = formView.createComponent();
        assertSame(getModelAttribute(elem), ((JComboBox) component).getModel());

        createFormView("select_many");
        component = formView.createComponent();
        assertSame(getModelAttribute(elem), ((JComboBox) component).getModel());

        createFormView("select_multiple");
        formView = new FormView(elem);
        component = formView.createComponent();
        assertSame(getModelAttribute(elem),
                ((JList) ((Container) ((Container) component).getComponent(0))
                        .getComponent(0)).getModel());

        createFormView("select_multiple_many");
        component = formView.createComponent();
        assertSame(getModelAttribute(elem),
                ((JList) ((Container) ((Container) component).getComponent(0))
                        .getComponent(0)).getModel());
    }

    public void testGetMaximumSpan() throws Exception {

        elem = doc.getElement(FormAttributes.INPUT_TYPE_TEXT);
        formView = new FormView(elem) {
            protected Component createComponent() {
                return null;
            }
        };
        assertNull(formView.createComponent());
        checkFormViewSpans(0, 0, 0, 0);


        final Marker getPrefferdSpanCalled = new Marker();
        elem = doc.getElement(FormAttributes.INPUT_TYPE_RESET);
        formView = new FormView(elem) {
            protected Component createComponent() {
                Component component = new JCheckBox();
                ((JCheckBox) component).setModel((ButtonModel) elem
                        .getAttributes().getAttribute(
                                StyleConstants.ModelAttribute));
                component.setMinimumSize(new Dimension(10, 10));
                component.setPreferredSize(new Dimension(20, 20));
                component.setMaximumSize(new Dimension(30, 30));
                return component;
            }

            public float getPreferredSpan(int axis) {
                getPrefferdSpanCalled.setOccurred();
                return super.getPreferredSpan(axis);
            }
        };
        assertFalse(getPrefferdSpanCalled.isOccurred());
        checkFormViewSpans(0, 0, 30, 30);


        // Wrong element type
        elem = doc.getElement(FormAttributes.INPUT_TYPE_SUBMIT);
        formView = new FormView(elem) {
            protected Component createComponent() {
                Component component = super.getComponent();
                if (component == null) {
                    return null;
                }
                component.setMinimumSize(new Dimension(10, 10));
                component.setPreferredSize(new Dimension(20, 20));
                component.setMaximumSize(new Dimension(30, 30));
                return component;
            };
        };
        MutableAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(HTML.Attribute.TYPE, "my_type");
        doc.setCharacterAttributes(22, 1, attrs, false);
        assertEquals("my_type",
                     elem.getAttributes().getAttribute(HTML.Attribute.TYPE));
        checkFormViewSpans(0, 0, 0, 0);


        elem = doc.getElement(FormAttributes.INPUT_TYPE_RESET);
        formView = new FormView(elem) {
            private boolean called;

            protected Component createComponent() {
                Component component;
                if (!called) {
                    component = super.createComponent();
                    called = true;
                    component.setMinimumSize(new Dimension(100, 100));
                    component.setPreferredSize(new Dimension(2000, 2000));
                    component.setMaximumSize(new Dimension(3000, 3000));
                } else {
                    component = new JPanel();
                    component.setMinimumSize(new Dimension(10, 10));
                    component.setPreferredSize(new Dimension(20, 20));
                    component.setMaximumSize(new Dimension(30, 30));
                }
                return component;
            }
        };
        checkFormViewSpans(0, 0, 2000, 2000);
        formView.setParent(null);
        checkFormViewSpans(0, 0, 2000, 2000);

        // Wrong element
        elem = createElement();
        formView = new FormView(elem) {
            protected Component createComponent() {
                Component component = new JPanel();
                component.setMinimumSize(new Dimension(10, 10));
                component.setPreferredSize(new Dimension(20, 20));
                component.setMaximumSize(new Dimension(30, 30));
                return component;
            }
        };
        checkFormViewSpans(0, 0, 30, 30);
    }

    public void testGetMaximumSpan_WithValidElements() throws Exception {
        createFormView(FormAttributes.INPUT_TYPE_BUTTON);
        checkMaxSpan(formView);

        createFormView(FormAttributes.INPUT_TYPE_CHECKBOX);
        checkMaxSpan(formView);

        createFormView(FormAttributes.INPUT_TYPE_IMAGE);
        checkMaxSpan(formView);

        createFormView(FormAttributes.INPUT_TYPE_PASSWORD);
        checkMaxSpan(formView);

        createFormView(FormAttributes.INPUT_TYPE_RADIO);
        checkMaxSpan(formView);

        createFormView(FormAttributes.INPUT_TYPE_RESET);
        checkMaxSpan(formView);

        createFormView(FormAttributes.INPUT_TYPE_SUBMIT);
        checkMaxSpan(formView);

        createFormView(FormAttributes.INPUT_TYPE_TEXT);
        checkMaxSpan(formView);

        createFormView(FormAttributes.INPUT_TYPE_FILE);
        checkMaxSpan(formView);

        createFormView("textArea");
        checkMaxSpan(formView);

        createFormView("select_0");
        checkMaxSpan(formView);

        createFormView("select");
        checkMaxSpan(formView);

        createFormView("select_many");
        checkMaxSpan(formView);

        createFormView("select_multiple");
        // getPreferred span doesn't handle
        checkFormViewSpans(0, 0);
        formView.setParent(editorPane.getUI().getRootView(editorPane));
        
        boolean notEqual = Math.abs(formView.getPreferredSpan(View.X_AXIS)
                                    - formView.getMaximumSpan(View.X_AXIS))
                           > 0.00001;
        if (isHarmony()) {
            assertFalse(notEqual);
        } else {
            assertTrue(notEqual);
        }
        notEqual = Math.abs(formView.getPreferredSpan(View.Y_AXIS)
                            - formView.getMaximumSpan(View.Y_AXIS))
                   > 0.00001;
        if (isHarmony()) {
            assertFalse(notEqual);
        } else {
            assertTrue(notEqual);
        }

        createFormView("select_multiple_many");
        // getPreferred span doesn't handle
        checkFormViewSpans(0, 0);
        formView.setParent(editorPane.getUI().getRootView(editorPane));
        notEqual = Math.abs(formView.getPreferredSpan(View.X_AXIS)
                            - formView.getMaximumSpan(View.X_AXIS))
                   > 0.00001;
        if (isHarmony()) {
            assertFalse(notEqual);
        } else {
            assertTrue(notEqual);
        }
        notEqual = Math.abs(formView.getPreferredSpan(View.Y_AXIS)
                            - formView.getMaximumSpan(View.Y_AXIS))
                   > 0.00001;
        if (isHarmony()) {
            assertFalse(notEqual);
        } else {
            assertTrue(notEqual);
        }
    }

    public void testImageSubmit() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void testSubmitData() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void testActionPerformed() throws Exception {
        Component component;

        elem = doc.getElement("submit2");
        formView = new FormView(elem) {
            public void actionPerformed(ActionEvent event) {
                super.actionPerformed(event);
            }

            protected void submitData(String data) {
                super.submitData(data);
            }
        };
        component = formView.createComponent();
        JPanel panel = new JPanel() {
            public int getComponentCount() {
                return super.getComponentCount();
            }
        };
        panel.add(component);
        ((JButton) component).doClick();
        
        throw new UnsupportedOperationException("Not implemented");
    }

    public void testIsElementSuccessfull() throws Exception {
        StringBuffer src = new StringBuffer();
        src.append("<HTML><HEAD></HEAD><BODY> Test");
        src.append("<FORM ACTION = \"\">");
        src.append("    <INPUT TYPE = \"checkbox\" NAME =\"checkbox_name\" "
                   + "VALUE = \"checkbox:JCheckBox\" ID=\"checkbox\">");
        src.append("    <INPUT TYPE = \"checkbox\" NAME =\"checked "
                   + "checkbox_name\" VALUE = \"checked checkbox:JCheckBox\" "
                   + "CHECKED ID=\"checked checkbox\">");
        src.append("</FORM></BODY></HTML>");
        StringReader reader = new StringReader(src.toString());
        doc = new HTMLDocument();
        editorPane.setDocument(doc);
        htmlEditorKit.read(reader, doc, 0);
        // 1. Test checkbox button. Successful should be only checked buttons.

        src = new StringBuffer();
        src.append("<HTML><HEAD></HEAD><BODY> Test");
        src.append("<FORM ACTION = \"\">");
        src.append("    <INPUT TYPE = \"submit\" disabled NAME =\"submit_name\""
                   + " VALUE = \"submit:JButton\" ID=\"submit\">");
        src.append("    <INPUT TYPE = \"checkbox\" disabled NAME =\"checked "
                   + "checkbox_name\" VALUE = \"checked checkbox:JCheckBox\" "
                   + "CHECKED ID=\"checked checkbox\">");
        src.append("    <INPUT TYPE = \"button\" disabled NAME =\"button_name\""
                   + " VALUE = \"button:JButton\" ID=\"button\">");
        src.append("    <INPUT TYPE = \"file\" disabled NAME =\"file_name\" "
                   + "VALUE = \"file:JTextField\" ID=\"file\">");
        src.append("    <SELECT NAME = \"select_name\" disabled MULTIPLE "
                   + "ID=\"select_multiple_many\">");
        src.append("       <OPTION VALUE = \"case1\"> case1");
        src.append("       <OPTION VALUE = \"case2\" SELECTED> case2");
        src.append("       <OPTION VALUE = \"case3\" SELECTED> case3");
        src.append("    </SELECT>");
        src.append("    <SELECT NAME = \"select_name\" MULTIPLE "
                   + "ID=\"select_multiple_many\">");
        src.append("       <OPTION VALUE = \"case1\"> case1");
        src.append("       <OPTION VALUE = \"case2\" disabled SELECTED> case2");
        src.append("    </SELECT>");
        src.append("    <TEXTAREA NAME = \"textarea_name\" disabled WRAP="
                   + "\"virtual\" COLS=\"50\" ROWS=\"5\" ID=\"textArea\">");
        src.append("         JTextArea in a JScrollPane");
        src.append("    </TEXTAREA>");
        src.append("</FORM></BODY></HTML>");
        reader = new StringReader(src.toString());
        doc = new HTMLDocument();
        editorPane.setDocument(doc);
        htmlEditorKit.read(reader, doc, 0);
        // TODO
        // 1. Test disable attribute in
        // a) INPUT
        // b) SELECT
        // d) OPTION
        // c) TEXTAREA

        src = new StringBuffer();
        src.append("<HTML><HEAD></HEAD><BODY> Test");
        src.append("<FORM ACTION = \"\">");
        src.append("    <INPUT TYPE = \"submit\" NAME =\"submit_name1\" "
                   + "VALUE = \"submi1t:JButton\" ID=\"submit1\">");
        src.append("    <INPUT TYPE = \"submit\" NAME =\"submit_name2\" "
                   + "VALUE = \"submit2:JButton\" ID=\"submit2\">");
        src.append("</FORM></BODY></HTML>");
        reader = new StringReader(src.toString());
        doc = new HTMLDocument();
        editorPane.setDocument(doc);
        htmlEditorKit.read(reader, doc, 0);
        // 1. Test submit button. Successful should be only armed button.

        if (isHarmony()) {
            /**
             * HTML 4.0 specific tags
             */
            src = new StringBuffer();
            src.append("<HTML><HEAD></HEAD><BODY> Test");
            src.append("<FORM ACTION = \"\">");
            src.append("    <BUTTON TYPE = \"submit1\" disabled "
                       + "NAME=\"button_submit1_name\" value=\"submit1:JButton\">");
            src.append("       Send");
            src.append("       <IMG src=\"\" alt=\"alt\">");
            src.append("    </BUTTON>");
            src .append("    <SELECT NAME = \"select_name\" MULTIPLE "
                        + "ID=\"select_multiple_many\">");
            src.append("        <OPTGROUP disabled label=\"Option gr1\">");
            src.append("            <OPTION VALUE = \"case1\"> case1");
            src.append("            <OPTION VALUE = \"case2\" disabled "
                       + "SELECTED> case2");
            src.append("        </OPTGROUP>");
            src.append("        <OPTGROUP label=\"Option gr2\">");
            src.append("            <OPTION VALUE = \"case1\"> case1");
            src.append("            <OPTION VALUE = \"case2\" disabled "
                       + "SELECTED> case2");
            src.append("        </OPTGROUP>");
            src.append("    </SELECT>");
            src.append("    <INPUT TYPE = \"submit\" NAME =\"submit_name2\" "
                       + "VALUE = \"submit2:JButton\" ID=\"submit2\">");
            src.append("</FORM></BODY></HTML>");
            reader = new StringReader(src.toString());
            doc = new HTMLDocument();
            editorPane.setDocument(doc);
            htmlEditorKit.read(reader, doc, 0);
            // TODO
            // 1. Test disable attribute in
            // a) BUTTON
            // b) OPTGROUP
            // 2. Test submit button. Successful should be only armed button.
        }

        src = new StringBuffer();
        src.append("<HTML><HEAD></HEAD><BODY> Test");
        src.append("<FORM ACTION = \"\">");
        src.append("    <INPUT TYPE = \"checkbox\" NAME =\"checkbox_name\" "
                   + "VALUE = \"checkbox:JCheckBox\" ID=\"checkbox\">");
        src.append("    <INPUT TYPE = \"checkbox\" NAME =\"checked "
                   + "checkbox_name\" VALUE = \"checked checkbox:JCheckBox\" "
                   + "CHECKED ID=\"checked checkbox\">");
        src.append("</FORM></BODY></HTML>");
        reader = new StringReader(src.toString());
        doc = new HTMLDocument();
        editorPane.setDocument(doc);
        htmlEditorKit.read(reader, doc, 0);
        // TODO
        // 1. Test checkbox button. Successful should be only checked buttons.

        src = new StringBuffer();
        src.append("<HTML><HEAD></HEAD><BODY> Test");
        src.append("<FORM ACTION = \"\">");
        src.append("    <INPUT TYPE = \"radio\" NAME =\"radio_name\" "
                   + "value = \"radio:JRadioButton\" ID=\"radio\">");
        src.append("    <INPUT TYPE = \"radio\" NAME =\"radio_name\" "
                   + "value = \"radio:JRadioButton\" CHECKED ID=\"radio\">");
        src.append("    <INPUT TYPE = \"radio\" NAME =\"checked radio_name\" "
                   + "value = \"checked radio:JRadioButton\" ID=\"checked "
                   + "radio\">");
        src.append("</FORM></BODY></HTML>");
        reader = new StringReader(src.toString());
        doc = new HTMLDocument();
        editorPane.setDocument(doc);
        htmlEditorKit.read(reader, doc, 0);
        // TODO
        // 1. Test radioButton.
        // a) For buttons with the same name successful should be only selected
        // button.
        // b) Test Unchecked buttons

        src = new StringBuffer();
        src.append("<HTML><HEAD></HEAD><BODY> Test");
        src.append("<FORM ACTION = \"\">");
        src.append("    <SELECT NAME = \"select_name\" MULTIPLE "
                   + "ID=\"select_multiple_many\">");
        src.append("       <OPTION VALUE = \"case1\"> case1");
        src.append("       <OPTION VALUE = \"case1\"> SELECTED case2");
        src.append("       <OPTION VALUE = \"case1\"> SELECTED case2");
        src.append("       <OPTION VALUE = \"case2\" disabled SELECTED> case3");
        src.append("    </SELECT>");
        src.append("</FORM></BODY></HTML>");
        reader = new StringReader(src.toString());
        doc = new HTMLDocument();
        editorPane.setDocument(doc);
        htmlEditorKit.read(reader, doc, 0);
        // TODO
        // 1. Successful should be only selected items.

        src = new StringBuffer();
        src.append("<HTML><HEAD></HEAD><BODY> Test");
        src.append("<FORM ACTION = \"\">");
        src.append("    <INPUT TYPE = \"file\" NAME =\"file_name\" "
                   + "VALUE = \"file:JTextField\" ID=\"file\">");
        src.append("</FORM></BODY></HTML>");
        reader = new StringReader(src.toString());
        doc = new HTMLDocument();
        editorPane.setDocument(doc);
        htmlEditorKit.read(reader, doc, 0);
        // TODO
        // 1. File is successful element

        // TODO
        // 1. If current value of element is unset, element may considered as
        // not successfull

        src = new StringBuffer();
        src.append("<HTML><HEAD></HEAD><BODY> Test");
        src.append("<FORM ACTION = \"\">");
        src.append("    <INPUT TYPE = \"reset\" NAME =\"reset_name\" "
                   + "VALUE = \"reset:JButton\" ID=\"reset\">");
        src.append("</FORM></BODY></HTML>");
        reader = new StringReader(src.toString());
        doc = new HTMLDocument();
        editorPane.setDocument(doc);
        htmlEditorKit.read(reader, doc, 0);
        // TODO
        // 1. Reset isn't successful element

        src = new StringBuffer();
        src.append("<HTML><HEAD></HEAD><BODY> Test");
        src.append("<FORM ACTION = \"\">");
        src.append("    <INPUT TYPE = \"reset\" NAME =\"reset_name\" "
                   + "VALUE = \"reset:JButton\" ID=\"reset\">");
        src.append("</FORM></BODY></HTML>");
        reader = new StringReader(src.toString());
        doc = new HTMLDocument();
        editorPane.setDocument(doc);
        htmlEditorKit.read(reader, doc, 0);
        // TODO
        // 1. OBJECT with attribute "declare" isn't successful element.

        // TODO
        // 1. Tests on invisible elements.
        throw new UnsupportedOperationException("Not implemented");
    }

    private void checkCreateComponent(final FormView formView,
                                      final Component component) {

        Component comp = component;
        assertNotSame(comp, formView.createComponent());
        formView.setParent(editorPane.getUI().getRootView(editorPane));
        assertNotNull(formView.getComponent());
        comp = formView.createComponent();
        assertNotSame(comp, formView.getComponent());
        assertNotSame(formView.getComponent(),
                      StyleConstants.getComponent(elem.getAttributes()));
        assertNotSame(comp,
                      StyleConstants.getComponent(formView.getElement()
                      .getAttributes()));
        assertNull(formView.createComponent().getParent());
        assertNotNull(formView.getComponent().getParent());
    }

    private void checkFormViewSpans(final double x1, final double y1) {
        assertEquals(x1, y1, 0.0001);
    }

    private void checkFormViewSpans(final double x1, final double y1,
                final double x2, final double y2) {
        checkFormViewSpans(x1, y1);
        formView.setParent(editorPane.getUI().getRootView(editorPane));
        checkFormViewSpans(x2, y2);
    }

    private void checkMaxSpan(final FormView formView) {
        checkFormViewSpans(formView.getPreferredSpan(View.X_AXIS),
                           formView.getPreferredSpan(View.X_AXIS));
        checkFormViewSpans(formView.getPreferredSpan(View.Y_AXIS),
                           formView.getPreferredSpan(View.Y_AXIS));

        formView.setParent(editorPane.getUI().getRootView(editorPane));

        checkFormViewSpans(formView.getPreferredSpan(View.X_AXIS),
                           formView.getPreferredSpan(View.X_AXIS));
        checkFormViewSpans(formView.getPreferredSpan(View.Y_AXIS),
                           formView.getPreferredSpan(View.Y_AXIS));
    }

    private void checkWithoutlModel(final int offset, final String id) {
        checkWithoutlModel(offset, id, id);
    }

    private void checkWithoutlModel(final int offset, final String id,
                                    final String type) {
        elem = doc.getElement(id);
        MutableAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttributes(elem.getAttributes());
        attrs.removeAttribute(StyleConstants.ModelAttribute);
        doc.setCharacterAttributes(offset, 1, attrs, true);
    
        assertNull(elem.getAttributes().getAttribute(StyleConstants
                                                     .ModelAttribute));
        createFormView(id);
        assertNotNull(formView.createComponent());
        assertNull(elem.getAttributes().getAttribute(StyleConstants
                  .ModelAttribute));
    }

    private Element createElement() {
        return new Element() {

            private MutableAttributeSet attrs = new SimpleAttributeSet();

            public AttributeSet getAttributes() {
                return attrs;
            }

            public Document getDocument() {
                return null;
            }

            public Element getElement(int index) {
                return null;
            }

            public int getElementCount() {
                return 0;
            }

            public int getElementIndex(int offset) {
                return 0;
            }

            public int getEndOffset() {
                return 2;
            }

            public String getName() {
                return "element";
            }

            public Element getParentElement() {
                return null;
            }

            public int getStartOffset() {
                return 0;
            }

            public boolean isLeaf() {
                return true;
            }
        };
    }

    private void createFormView(final String id) {
        elem = doc.getElement(id);
        formView = new FormView(elem);
    }

    private Object getModelAttribute(final Element element) {
        return element.getAttributes().getAttribute(
                StyleConstants.ModelAttribute);
    }

    private void testModel(String id) {
        createFormView(id);
        assertNotNull(getModelAttribute(elem));
    }
}
