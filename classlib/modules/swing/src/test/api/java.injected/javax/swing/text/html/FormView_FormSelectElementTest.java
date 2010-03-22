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

import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingTestCase;
import javax.swing.text.Element;

public class FormView_FormSelectElementTest extends SwingTestCase {

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

        public void testModelAttributes() throws Exception {
            createHTMLSample();

        }

        public void testCreateComponent() throws Exception {
            createHTMLSample();
            createFormViewWithParent("option_disabled_attribute");
            JComboBox comboBox = (JComboBox)formView.getComponent();
            assertFalse(comboBox.isEditable());

            createFormViewWithParent("select_with_dir_title");
            comboBox = (JComboBox)formView.getComponent();

            if (isHarmony()) {
                assertEquals("TITLE", comboBox.getToolTipText());
                assertEquals(ComponentOrientation.RIGHT_TO_LEFT,
                             comboBox.getComponentOrientation());
            }


        }

        public void testCreateMultipleSelectionComponent() throws Exception {
            createHTMLSample();

            createFormViewWithParent("select_multiple_name_only");
            JScrollPane pane = (JScrollPane)formView.getComponent();
            JList list = (JList)pane.getViewport().getComponent(0);
            int[] selectedIndices = list.getSelectedIndices();
            assertEquals(0, selectedIndices.length);

            createFormViewWithParent("select_multiple");
            pane = (JScrollPane)formView.getComponent();
            list = (JList)pane.getViewport().getComponent(0);
            selectedIndices = list.getSelectedIndices();
            assertEquals(3, selectedIndices.length);
            assertEquals(0, selectedIndices[0]);
            assertEquals(1, selectedIndices[1]);
            assertEquals(4, selectedIndices[2]);

            //HTML 4.0 feature
            if (isHarmony()) {
                createFormViewWithParent("select_optiongroup");
                pane = (JScrollPane)formView.getComponent();
                list = (JList)pane.getViewport().getComponent(0);
                selectedIndices = list.getSelectedIndices();
                assertEquals(2, selectedIndices.length);
                assertEquals(0, selectedIndices[0]);
                assertEquals(2, selectedIndices[1]);
            }
        }

        public void testCreateSimpleSelectionComponent() throws Exception {
            createHTMLSample();

            createFormViewWithParent("select");
            JComboBox comboBox1 = (JComboBox)formView.getComponent();
            createFormViewWithParent("select_name_only");
            JComboBox comboBox2 = (JComboBox)formView.getComponent();

            assertEquals(comboBox1.getMinimumSize(),
                         comboBox1.getPreferredSize());
            if (isHarmony()) {
                assertEquals(comboBox1.getMaximumSize(),
                             comboBox1.getPreferredSize());
            }

            assertEquals(comboBox1.getPreferredSize(),
                         comboBox2.getPreferredSize());

            createFormViewWithParent("select_simple_options");
            comboBox1 = (JComboBox)formView.getComponent();
            assertEquals(comboBox1.getSelectedIndex(), 0);

            createFormViewWithParent("options_label_attr");
            comboBox1 = (JComboBox)formView.getComponent();
            assertEquals(comboBox1.getSelectedIndex(), 0);

            createFormViewWithParent("selected_disabled_options1");
            comboBox1 = (JComboBox)formView.getComponent();
            assertEquals(comboBox1.getSelectedIndex(), 1);

            createFormViewWithParent("selected_disabled_options2");
            comboBox1 = (JComboBox)formView.getComponent();
            assertEquals(comboBox1.getSelectedIndex(), 1);

            if (isHarmony()) {
                createFormViewWithParent("option_disabled_attribute");
                comboBox1 = (JComboBox)formView.getComponent();
                assertEquals(comboBox1.getSelectedIndex(), 4);
            }
        }


        private void createFormViewWithParent(final String id) {
            elem = document.getElement(id);
            formView = new FormView(elem);
            formView.setParent(editorPane.getUI().getRootView(editorPane));
        }

        private void createHTMLSample() throws Exception {
            StringBuffer htmlSrc = new StringBuffer();
            htmlSrc.append("<HTML>");
            htmlSrc.append("<HEAD></HEAD>");
            htmlSrc.append("<BODY>");
            htmlSrc.append("   Hello word!");
            htmlSrc.append("<FORM ACTION = \"\">");
            htmlSrc.append("    <SELECT "
                    + "ID=\"select\">"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT "
                    + "NAME =\"select_name_only\"  "
                    + "ID=\"select_name_only\">"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT multiple "
                    + "NAME =\"select_multiple_name_only\"  "
                    + "ID=\"select_multiple_name_only\">"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT size = 1 "
                    + "NAME =\"select_size1_name_only\"  "
                    + "ID=\"select_size1_name_only\">"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT size = 5 "
                    + "NAME =\"select_size5_name_only\"  "
                    + "ID=\"select_size5_name_only\">"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT "
                    + "SIZE =\"10\"  "
                    + "ID=\"select_size_only\">"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT "
                    + "NAME =\"select_simple_options\"  "
                    + "ID=\"select_simple_options\">"
                    + "   <OPTION>"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION>    text2"
                    + "      </OPTION>"
                    + "   <OPTION>    text3"
                    + "      </OPTION>"
                    + "</SELECT>");
              htmlSrc.append("    <SELECT dir =\"RTL\" title = \"TITLE\" "
                      + "NAME =\"select_with_dir_title\"  "
                      + "ID=\"select_with_dir_title\">"
                      + "   <OPTION>"
                      + "        text1"
                      + "   </OPTION>"
                      + "   <OPTION>    text2"
                      + "      </OPTION>"
                      + "   <OPTION>    text3"
                      + "      </OPTION>"
                      + "</SELECT>");
            htmlSrc.append("    <SELECT "
                    + "NAME =\"options_label_attr\"  "
                    + "ID=\"options_label_attr\">"
                    + "   <OPTION value = \"val1\" label = \"lab1\">"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION value = \"val2\" label = \"lab2\"> text2"
                    + "      </OPTION>"
                    + "   <OPTION label = \"lab3\">  text3"
                    + "      </OPTION>"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT "
                    + "NAME =\"selected_disabled_options1\"  "
                    + "ID=\"selected_disabled_options1\">"
                    + "   <OPTION disabled value = \"val1\" label = \"lab1\">"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION disabled selected value = \"val2\", "
                    + "            label = \"lab2\"> text2"
                    + "      </OPTION>"
                    + "   <OPTION label = \"lab3\">  text3"
                    + "      </OPTION>"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT "
                    + "NAME =\"selected_disabled_options2\"  "
                    + "ID=\"selected_disabled_options2\">"
                    + "   <OPTION disabled selected = \"val1\" label = \"lab1\">"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION disabled selected value = \"val2\" "
                    + "            label = \"lab2\"> text2"
                    + "      </OPTION>"
                    + "   <OPTION label = \"lab3\">  text3"
                    + "      </OPTION>"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT "
                    + "NAME =\"incorrect_selection\"  "
                    + "ID=\"incorrect_selection\">"
                    + "   <OPTION selected value = \"val1\" label = \"lab1\">"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION selected value = \"val2\" "
                    + "            label = \"lab2\"> text2"
                    + "      </OPTION>"
                    + "   <OPTION label = \"lab3\">  text3"
                    + "      </OPTION>"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT "
                    + "NAME =\"option_dir_attribute\"  "
                    + "ID=\"option_dir_attribute\">"
                    + "   <OPTION value = \"val1\" label = \"lab1\" "
                    + "           dir = \"rtl\">"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION value = \"val1\"  dir = \"rtl\">"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION value = \"val2\" dir = \"rtl\" "
                    + "            label = \"lab2\"> text2"
                    + "      </OPTION>"
                    + "   <OPTION value = \"val2\" dir = \"rtl\"> text2"
                    + "      </OPTION>"
                    + "   <OPTION dir = \"rtl\">  text3"
                    + "      </OPTION>"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT "
                    + "NAME =\"option_title_attribute\"  "
                    + "ID=\"option_title_attribute\">"
                    + "   <OPTION value = \"val1\" label = \"lab1\" "
                    + "           title = \"title1\">"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION value = \"val1\"  title = \"title2\">"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION value = \"val2\" title = \"title3\" "
                    + "            label = \"lab2\"> text2"
                    + "      </OPTION>"
                    + "   <OPTION value = \"val2\" title = \"title4\"> text2"
                    + "      </OPTION>"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT "
                    + "NAME =\"option_disabled_attribute\"  "
                    + "ID=\"option_disabled_attribute\">"
                    + "   <OPTION value = \"val1\" label = \"lab1\" "
                    + "           title = \"title1\" disabled>"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION value = \"val2\"  disabled>"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION value = \"val3\" title = \"title3\" "
                    + "            disabled label = \"lab2\"> text2"
                    + "   </OPTION>"
                    + "   <OPTION value = \"val4\" disabled> text2"
                    + "      </OPTION>"
                    + "   <OPTION>  text3"
                    + "      </OPTION>"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT disabled "
                    + "NAME =\"select_disabled\"  "
                    + "ID=\"select_disabled\">"
                    + "   <OPTION value = \"val1\" label = \"lab1\">"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION value = \"val2\" label = \"lab2\"> text2"
                    + "      </OPTION>"
                    + "   <OPTION label = \"lab3\">  text3"
                    + "      </OPTION>"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT multiple "
                    + "NAME =\"select_multiple\"  "
                    + "ID=\"select_multiple\">"
                    + "   <OPTION selected value = \"val1\" label = \"lab1\">"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION selected value = \"val2\" label = \"lab2\">"
                    + "        text2"
                    + "      </OPTION>"
                    + "   <OPTION label = \"lab3\">  text4"
                    + "      </OPTION>"
                    + "   <OPTION label = \"lab3\">  text5"
                    + "      </OPTION>"
                    + "   <OPTION selected >  very_long_text"
                    + "      </OPTION>"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT multiple "
                    + "NAME =\"select_multiple\"  "
                    + "ID=\"select_optiongroup\">"
                    + "   <OPTION selected value = \"val1\" label = \"lab1\">"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTIONGROUP selected value = \"val2\" label = \"lab2\">"
                    + "      <OPTION label = \"lab3\">  text4 </OPTION>"
                    + "      <OPTION label = \"lab3\" selected>  text5 </OPTION>"
                    + "   </OPTIONGROUP>"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT size = 1 "
                    + "NAME =\"incorrect_select_size1\"  "
                    + "ID=\"incorrect_select_size1\">"
                    + "   <OPTION selected value = \"val1\" label = \"lab1\">"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION selected value = \"val2\" label = \"lab2\"> text2"
                    + "      </OPTION>"
                    + "   <OPTION selected label = \"lab3\">  text3"
                    + "      </OPTION>"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT size = 1 "
                    + "NAME =\"select_size1\"  "
                    + "ID=\"select_size1\">"
                    + "   <OPTION value = \"val1\" label = \"lab1\">"
                    + "        text1"
                    + "   </OPTION>"
                    + "   <OPTION value = \"val2\" label = \"lab2\"> text2"
                    + "      </OPTION>"
                    + "   <OPTION label = \"lab3\">  text3"
                    + "      </OPTION>"
                    + "</SELECT>");
            htmlSrc.append("    <SELECT "
                    + "NAME =\"complex_content\"  "
                    + "ID=\"complex_content\">"
                    + "   <OPTION value = \"val1\" >"
                    + "       <TABLE>"
                    + "       <TR><TH>text1<TD>val1"
                    + "       <TR><TH>text2<TD>val2"
                    + "       </TABLE>"
                    + "   </OPTION>"
                    + "   <OPTION value = \"val1\" label = \"lab1\">"
                    + "       <TABLE>"
                    + "       <TR><TH>text1<TD>val1"
                    + "       <TR><TH>text2<TD>val2"
                    + "       </TABLE>"
                    + "      </OPTION>"
                    + "   <OPTION value = \"val2\"> +"
                    + "       <TEXTAREA ID=\"textarea\">textarea</TEXTAREA>"
                    + "      </OPTION>"
                    + "   <OPTION value = \"val2\" label = \"lab2\">"
                    + "       <TEXTAREA ID=\"textarea\">textarea</TEXTAREA>"
                    + "      </OPTION>"
                    + "   <OPTION label = \"lab3\">  <b> bold <b>"
                    + "      </OPTION>"
                    + "</SELECT>");
            htmlSrc.append("</FORM>");
            htmlSrc.append("</BODY>");
            htmlSrc.append("</HTML>");

            StringReader reader = new StringReader(htmlSrc.toString());
            htmlEditorKit.read(reader, document, 0);
        }
    }
