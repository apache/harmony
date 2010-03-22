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
* @author Alexander T. Simbirtsev
*/
package javax.swing.text.html;

import javax.swing.DefaultButtonModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JToggleButton;
import javax.swing.text.AttributeSet;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import javax.swing.text.html.HTML.Tag;

import org.apache.harmony.x.swing.text.html.form.FormFieldsetModel;
import org.apache.harmony.x.swing.text.html.form.FormOption;

public class HTMLDocument_Reader_FormActionTest extends HTMLDocumentTestCase {

    protected HTMLDocument.HTMLReader reader;
    protected HTMLDocument doc;
    protected HTMLDocument.HTMLReader.TagAction action;
    
    protected void setUp() throws Exception {
        super.setUp();
        doc = new HTMLDocument();
        reader = (HTMLDocument.HTMLReader)doc.getReader(0);
    }

    protected void tearDown() throws Exception {
        action = null;
        doc = null;
        reader = null;
        super.tearDown();
    }

    public void testFormStart_InputButton() throws Exception {
        if (isHarmony()) {
            checkStandardInputStart("button", DefaultButtonModel.class);
        }
    }
    
    public void testFormEnd_InputButton() throws Exception {
        checkEmptyFormEnd(Tag.INPUT, "button");
    }
    
    public void testFormStart_InputCheckBox() throws Exception {
        checkStandardInputStart("checkbox", JToggleButton.ToggleButtonModel.class);
    }
    
    public void testFormEnd_InputCheckBox() throws Exception {
        checkEmptyFormEnd(Tag.INPUT, "checkbox");
    }
    
    public void testFormStart_InputImage() throws Exception {
        checkStandardInputStart("image", DefaultButtonModel.class);
    }
    
    public void testFormEnd_InputImage() throws Exception {
        checkEmptyFormEnd(Tag.INPUT, "image");
    }
    
    public void testFormStart_InputRadio() throws Exception {
        checkStandardInputStart("radio", JToggleButton.ToggleButtonModel.class);
    }
    
    public void testFormEnd_InputRadio() throws Exception {
        checkEmptyFormEnd(Tag.INPUT, "radio");
    }
    
    public void testForm_InputRadioGroups() throws Exception {
        String name1 = "name1";
        String name2 = "name2";
        String name3 = "";
        action = reader.new FormAction();

        reader.handleStartTag(Tag.FORM, new SimpleAttributeSet(), 0);
        reader.handleStartTag(Tag.INPUT, createInputNamedAttributes(name1), 0);
        reader.handleStartTag(Tag.INPUT, createInputNamedAttributes(name2), 0);
        reader.handleStartTag(Tag.INPUT, createInputNamedAttributes(name3), 0);
        reader.handleStartTag(Tag.INPUT, createInputNamedAttributes(name1), 0);
        reader.handleStartTag(Tag.INPUT, createInputNamedAttributes(null), 0);
        reader.handleStartTag(Tag.INPUT, createInputNamedAttributes(name2), 0);
        reader.handleStartTag(Tag.INPUT, createInputNamedAttributes(name3), 0);
        reader.handleStartTag(Tag.INPUT, createInputNamedAttributes(null), 0);
        reader.handleEndTag(Tag.FORM, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(13, reader.parseBuffer.size());

        DefaultButtonModel model1 = (DefaultButtonModel)getModel((ElementSpec)reader.parseBuffer.get(2));
        DefaultButtonModel model2 = (DefaultButtonModel)getModel((ElementSpec)reader.parseBuffer.get(3));
        DefaultButtonModel model3 = (DefaultButtonModel)getModel((ElementSpec)reader.parseBuffer.get(4));
        DefaultButtonModel model4 = (DefaultButtonModel)getModel((ElementSpec)reader.parseBuffer.get(5));
        DefaultButtonModel model5 = (DefaultButtonModel)getModel((ElementSpec)reader.parseBuffer.get(6));
        DefaultButtonModel model6 = (DefaultButtonModel)getModel((ElementSpec)reader.parseBuffer.get(7));
        DefaultButtonModel model7 = (DefaultButtonModel)getModel((ElementSpec)reader.parseBuffer.get(8));
        DefaultButtonModel model8 = (DefaultButtonModel)getModel((ElementSpec)reader.parseBuffer.get(9));
        assertNotNull(model1.getGroup());
        assertNotNull(model2.getGroup());
        assertNotNull(model3.getGroup());
        assertNotNull(model4.getGroup());
        assertNotNull(model5.getGroup());
        assertNotNull(model6.getGroup());
        assertNotNull(model7.getGroup());
        assertNotNull(model8.getGroup());
        assertSame(model1.getGroup(), model4.getGroup());
        assertSame(model2.getGroup(), model6.getGroup());
        assertSame(model3.getGroup(), model7.getGroup());
        assertSame(model5.getGroup(), model8.getGroup());
        assertNotSame(model1.getGroup(), model2.getGroup());
        assertNotSame(model3.getGroup(), model5.getGroup());

        reader.handleStartTag(Tag.FORM, new SimpleAttributeSet(), 0);
        reader.handleStartTag(Tag.INPUT, createInputNamedAttributes(name1), 0);
        reader.handleStartTag(Tag.INPUT, createInputNamedAttributes(null), 0);
        reader.handleStartTag(Tag.INPUT, createInputNamedAttributes(name1), 0);
        assertEquals(18, reader.parseBuffer.size());
        DefaultButtonModel model9 = (DefaultButtonModel)getModel((ElementSpec)reader.parseBuffer.get(15));
        DefaultButtonModel model10 = (DefaultButtonModel)getModel((ElementSpec)reader.parseBuffer.get(16));
        DefaultButtonModel model11 = (DefaultButtonModel)getModel((ElementSpec)reader.parseBuffer.get(17));
        assertNotNull(model9.getGroup());
        assertNotNull(model11.getGroup());
        if (!isHarmony()) {
            assertNotNull(model10.getGroup());
        } else {
            assertNull(model10.getGroup());
        }
        assertNotSame(model9.getGroup(), model1.getGroup());
        assertNotSame(model11.getGroup(), model1.getGroup());
        
    }

    private SimpleAttributeSet createInputNamedAttributes(String name) {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(HTML.Attribute.TYPE, "radio");
        if (name != null) {
            attr.addAttribute(HTML.Attribute.NAME, name);
        }
        return attr;
    }
    
    public void testFormStart_InputReset() throws Exception {
        checkStandardInputStart("reset", DefaultButtonModel.class);
    }
    
    public void testFormEnd_InputReset() throws Exception {
        checkEmptyFormEnd(Tag.INPUT, "reset");
    }
    
    public void testFormStart_InputSubmit() throws Exception {
        checkStandardInputStart("submit", DefaultButtonModel.class);
    }
    
    public void testFormEnd_InputSubmit() throws Exception {
        checkEmptyFormEnd(Tag.INPUT, "submit");
    }
    
    public void testFormStart_InputText() throws Exception {
        checkStandardInputStart("text", PlainDocument.class);
    }
    
    public void testFormEnd_InputText() throws Exception {
        checkEmptyFormEnd(Tag.INPUT, "text");
    }
    
    public void testFormStart_InputPassword() throws Exception {
        checkStandardInputStart("password", PlainDocument.class);
    }
    
    public void testFormEnd_InputPassword() throws Exception {
        checkEmptyFormEnd(Tag.INPUT, "password");
    }
    
    public void testFormStart_InputFile() throws Exception {
        checkStandardInputStart("file", PlainDocument.class);
    }
    
    public void testFormEnd_InputFile() throws Exception {
        checkEmptyFormEnd(Tag.INPUT, "file");
    }
    
    public void testFormStart_InputHidden() throws Exception {
        checkStandardInputStart("hidden", null);
    }
    
    public void testFormEnd_InputHidden() throws Exception {
        checkEmptyFormEnd(Tag.INPUT, "hidden");
    }
    
    public void testFormStart_InputNull() throws Exception {
        checkStandardInputStart(null, PlainDocument.class);
    }
    
    public void testFormEnd_InputNull() throws Exception {
        checkEmptyFormEnd(Tag.INPUT, null);
    }
    
    public void testFormStart_Button() throws Exception {
        if (isHarmony()) {
            checkStandardFormStart(Tag.BUTTON, DefaultButtonModel.class);
        }
    }
    
    public void testFormEnd_Button() throws Exception {
        if (isHarmony()) {
            checkEmptyFormEnd(Tag.BUTTON, null);
        }
    }
    
    public void testFormStart_SelectCombo1() throws Exception {
        checkStandardFormStart(Tag.SELECT, DefaultComboBoxModel.class);
    }
    
    public void testFormStart_SelectCombo2() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(HTML.Attribute.SIZE, "1");
        checkStandardFormStart(Tag.SELECT, DefaultComboBoxModel.class, attr);
    }
    
    public void testFormStart_SelectList1() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(HTML.Attribute.MULTIPLE, "multiple");
        checkStandardFormStart(Tag.SELECT, DefaultListModel.class, attr);
    }
    
    public void testFormStart_SelectList2() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(HTML.Attribute.SIZE, "2");
        checkStandardFormStart(Tag.SELECT, DefaultListModel.class, attr);
    }
    
    public void testFormStart_SelectList3() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(HTML.Attribute.MULTIPLE, "multiple");
        attr.addAttribute(HTML.Attribute.SIZE, "1");
        checkStandardFormStart(Tag.SELECT, DefaultListModel.class, attr);
    }
    
    public void testFormStart_SelectList4() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(HTML.Attribute.SIZE, "kva");
        checkStandardFormStart(Tag.SELECT, DefaultComboBoxModel.class, attr);
    }
    
    public void testFormEnd_Select() throws Exception {
        checkEmptyFormEnd(Tag.SELECT, null);
    }
    
    public void testFormStart_Option() throws Exception {
        String text = "text";
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new FormAction();
        action.start(Tag.OPTION, attr);
        assertEquals(0, reader.parseBuffer.size());
    }
    
    public void testForm_OptionCombo() throws Exception {
        String text = "text";
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new FormAction();
        action.start(Tag.SELECT, attr);
        action.start(Tag.OPTION, attr);
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        final AttributeSet specAttr = spec.getAttributes();
        assertEquals(3, specAttr.getAttributeCount());
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {' '});
        Object contentModel = specAttr.getAttribute(StyleConstants.ModelAttribute);
        assertNotNull(contentModel);
        assertTrue(contentModel instanceof DefaultComboBoxModel);
        DefaultComboBoxModel model = (DefaultComboBoxModel)contentModel;
        assertEquals(1, model.getSize());
        assertNotNull(model.getElementAt(0));
        reader.handleText(text.toCharArray(), 0);
        assertEquals(1, model.getSize());
        assertNotNull(model.getElementAt(0));
        action.end(Tag.OPTION);

        action.start(Tag.OPTION, attr);
        assertEquals(2, model.getSize());
        assertNotNull(model.getElementAt(1));
        reader.handleText(text.toCharArray(), 0);
        assertEquals(2, model.getSize());
        assertNotNull(model.getElementAt(1));
        action.end(Tag.OPTION);
        action.end(Tag.SELECT);
        assertEquals(3, specAttr.getAttributeCount());
    }
    
    public void testForm_OptionList() throws Exception {
        String text = "text";
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(HTML.Attribute.MULTIPLE, "multiple");
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new FormAction();
        action.start(Tag.SELECT, attr);
        action.start(Tag.OPTION, attr);
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        final AttributeSet specAttr = spec.getAttributes();
        assertEquals(4, specAttr.getAttributeCount());
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {' '});
        Object contentModel = specAttr.getAttribute(StyleConstants.ModelAttribute);
        assertNotNull(contentModel);
        assertTrue(contentModel instanceof DefaultListModel);
        DefaultListModel model = (DefaultListModel)contentModel;
        assertEquals(1, model.getSize());
        assertNotNull(model.getElementAt(0));
        reader.handleText(text.toCharArray(), 0);
        assertEquals(1, model.getSize());
        assertNotNull(model.getElementAt(0));
        action.end(Tag.OPTION);

        action.start(Tag.OPTION, attr);
        assertEquals(2, model.getSize());
        assertNotNull(model.getElementAt(1));
        reader.handleText(text.toCharArray(), 0);
        assertEquals(2, model.getSize());
        assertNotNull(model.getElementAt(1));
        action.end(Tag.OPTION);
        action.end(Tag.SELECT);
        assertEquals(4, specAttr.getAttributeCount());
    }
    
    public void testForm_SelectOption() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new FormAction();
        action.start(Tag.SELECT, attr);
        assertEquals(2, reader.parseBuffer.size());
        action.start(Tag.OPTION, attr);
        action.end(Tag.OPTION);
        action.start(Tag.OPTION, attr);
        action.end(Tag.OPTION);
        action.end(Tag.SELECT);
        assertEquals(2, reader.parseBuffer.size());
    }
    
    public void testForm_Attributes() throws Exception {
        String text = "text";
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new FormAction();
        action.start(Tag.SELECT, attr);
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        final AttributeSet specAttr = spec.getAttributes();
        assertNotSame(specAttr, attr);
    }
    
    public void testForm_OptionAttributes() throws Exception {
        String text = "text";
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new FormAction();
        action.start(Tag.SELECT, attr);
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        final AttributeSet specAttr = spec.getAttributes();
        assertEquals(3, specAttr.getAttributeCount());
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {' '});
        Object contentModel = specAttr.getAttribute(StyleConstants.ModelAttribute);
        assertNotNull(contentModel);
        assertTrue(contentModel instanceof DefaultComboBoxModel);
        DefaultComboBoxModel model = (DefaultComboBoxModel)contentModel;

        action.start(Tag.OPTION, attr);
        assertEquals(1, model.getSize());
        assertNotNull(model.getElementAt(0));
        Option option = (Option)model.getElementAt(0);
        assertFalse(option.isSelected());
        assertNull(option.getLabel());
        assertNull(option.getValue());
        reader.handleText(text.toCharArray(), 0);
        assertEquals(1, model.getSize());
        assertNotNull(model.getElementAt(0));
        option = (Option)model.getElementAt(0);
        assertFalse(option.isSelected());
        assertEquals(text, option.getLabel());
        assertEquals(text, option.getValue());
        action.end(Tag.OPTION);

        final String value = "value";
        final String label = "label";
        attr.addAttribute(HTML.Attribute.SELECTED, "true");
        attr.addAttribute(HTML.Attribute.VALUE, value);
        if (isHarmony()) {
            attr.addAttribute(HTML.Attribute.LABEL, label);
        } else {
            attr.addAttribute("label", label);
        }
        action.start(Tag.OPTION, attr);
        assertEquals(2, model.getSize());
        assertNotNull(model.getElementAt(1));
        option = (Option)model.getElementAt(1);
        assertTrue(option.isSelected());
        if (isHarmony()) {
            assertEquals(label, option.getLabel());
            assertEquals(0, ((FormOption)option).getDepth());
        } else {
            assertNull(option.getLabel());
        }
        assertEquals(value, option.getValue());
        reader.handleText(text.toCharArray(), 0);
        assertEquals(2, model.getSize());
        assertNotNull(model.getElementAt(1));
        option = (Option)model.getElementAt(1);
        assertTrue(option.isSelected());
        assertEquals(value, option.getValue());
        if (isHarmony()) {
            assertEquals(0, ((FormOption)option).getDepth());
        }
        assertEquals(text, option.getLabel());
        action.end(Tag.OPTION);
        action.end(Tag.SELECT);
        assertEquals(3, specAttr.getAttributeCount());
    }
    
    public void testForm_OptionGroupAttributes() throws Exception {
        if (!isHarmony()) {
            return;
        }
        String text = "text";
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new FormAction();
        action.start(Tag.SELECT, attr);
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        final AttributeSet specAttr = spec.getAttributes();
        assertEquals(3, specAttr.getAttributeCount());
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {' '});
        Object contentModel = specAttr.getAttribute(StyleConstants.ModelAttribute);
        assertNotNull(contentModel);
        assertTrue(contentModel instanceof DefaultComboBoxModel);
        DefaultComboBoxModel model = (DefaultComboBoxModel)contentModel;

        final String value = "value";
        final String label = "label";
        attr.addAttribute(HTML.Attribute.SELECTED, "true");
        attr.addAttribute(HTML.Attribute.LABEL, label);
        action.start(Tag.OPTGROUP, attr);
        assertEquals(1, model.getSize());
        assertNotNull(model.getElementAt(0));
        FormOption option = (FormOption)model.getElementAt(0);
        assertFalse(option.isSelected());
        assertEquals(label, option.getLabel());
        assertNull(option.getValue());
        reader.handleText(text.toCharArray(), 0);
        assertEquals(1, model.getSize());
        assertNotNull(model.getElementAt(0));
        option = (FormOption)model.getElementAt(0);
        assertFalse(option.isSelected());
        assertEquals(label, option.getLabel());
        assertNull(option.getValue());
        assertEquals(0, option.getDepth());

        attr.addAttribute(HTML.Attribute.SELECTED, "true");
        attr.addAttribute(HTML.Attribute.VALUE, value);
        attr.addAttribute(HTML.Attribute.LABEL, label);
        action.start(Tag.OPTGROUP, attr);
        assertEquals(2, model.getSize());
        assertNotNull(model.getElementAt(1));
        option = (FormOption)model.getElementAt(1);
        assertFalse(option.isSelected());
        assertEquals(label, option.getLabel());
        assertNull(option.getValue());
        reader.handleText(text.toCharArray(), 0);
        assertEquals(2, model.getSize());
        assertNotNull(model.getElementAt(1));
        option = (FormOption)model.getElementAt(1);
        assertFalse(option.isSelected());
        assertEquals(label, option.getLabel());
        assertNull(option.getValue());
        assertEquals(1, option.getDepth());
        action.end(Tag.OPTGROUP);
        action.end(Tag.OPTGROUP);
        action.end(Tag.SELECT);
        assertEquals(3, specAttr.getAttributeCount());
    }
    
    public void testFormEnd_Option() throws Exception {
        checkEmptyFormEnd(Tag.OPTION, null);
    }
    
    public void testFormEnd_Optgroup() throws Exception {
        if (isHarmony()) {
            checkEmptyFormEnd(Tag.OPTGROUP, null);
        }
    }
    
    public void testFormStart_TextArea() throws Exception {
        checkStandardFormStart(Tag.TEXTAREA, PlainDocument.class);
    }
    
    public void testFormEnd_TextArea() throws Exception {
        checkEmptyFormEnd(Tag.TEXTAREA, null);
    }
    
    public void testFormStart_FieldSet() throws Exception {
        if (!isHarmony()) {
            return;
        }
        String text = "text";
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new FormAction();
        action.start(Tag.FIELDSET, attr);
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        final AttributeSet specAttr = spec.getAttributes();
        assertEquals(3, specAttr.getAttributeCount());
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {' '});
        Object contentModel = specAttr.getAttribute(StyleConstants.ModelAttribute);
        assertNotNull(contentModel);
        assertTrue(contentModel instanceof FormFieldsetModel);
    }
    
    public void testFormEnd_FieldSet() throws Exception {
        if (isHarmony()) {
            checkEmptyFormEnd(Tag.FIELDSET, null);
        }
    }
    
    public void testFormStartEnd_Legend_InsideFieldSet() throws Exception {
        if (!isHarmony()) {
            return;
        }
        String text1 = "text1";
        String text2 = "text2";
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        SimpleAttributeSet legendAttr1 = new SimpleAttributeSet();
        legendAttr1.addAttribute("bbbb", "aaaa");
        SimpleAttributeSet legendAttr2 = new SimpleAttributeSet();
        legendAttr2.addAttribute("bb", "aa");
        action = reader.new FormAction();
        action.start(Tag.FIELDSET, attr);
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        final AttributeSet specAttr = spec.getAttributes();
        assertEquals(3, specAttr.getAttributeCount());
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {' '});
        Object contentModel = specAttr.getAttribute(StyleConstants.ModelAttribute);
        assertNotNull(contentModel);
        assertTrue(contentModel instanceof FormFieldsetModel);
        FormFieldsetModel fieldSet = (FormFieldsetModel)contentModel;
        assertNull(fieldSet.getLegend());
        assertNull(fieldSet.getLegendAttributes());

        action.start(Tag.LEGEND, legendAttr1);
        reader.handleText(text1.toCharArray(), 0);
        action.end(Tag.LEGEND);
        assertEquals(text1, fieldSet.getLegend());
        assertNotNull(fieldSet.getLegendAttributes());
        assertEquals(2, fieldSet.getLegendAttributes().getAttributeCount());
        checkAttributes(fieldSet.getLegendAttributes(), "bbbb", "aaaa");
        checkAttributes(fieldSet.getLegendAttributes(), StyleConstants.NameAttribute, Tag.LEGEND);
        assertNotSame(legendAttr1, fieldSet.getLegendAttributes());
        
        action.start(Tag.LEGEND, legendAttr2);
        reader.handleText(text2.toCharArray(), 0);
        action.end(Tag.LEGEND);
        assertEquals(text1, fieldSet.getLegend());
        assertNotNull(fieldSet.getLegendAttributes());
        assertEquals(2, fieldSet.getLegendAttributes().getAttributeCount());
        checkAttributes(fieldSet.getLegendAttributes(), StyleConstants.NameAttribute, Tag.LEGEND);
        checkAttributes(fieldSet.getLegendAttributes(), "bbbb", "aaaa");
    }
    
    public void testFormStartEnd_Legend_OutsideFieldSet() throws Exception {
        if (!isHarmony()) {
            return;
        }
        String text1 = "text1";
        String text2 = "text2";
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new FormAction();
        action.start(Tag.FIELDSET, attr);
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        final AttributeSet specAttr = spec.getAttributes();
        assertEquals(3, specAttr.getAttributeCount());
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {' '});
        Object contentModel = specAttr.getAttribute(StyleConstants.ModelAttribute);
        assertNotNull(contentModel);
        assertTrue(contentModel instanceof FormFieldsetModel);
        FormFieldsetModel fieldSet = (FormFieldsetModel)contentModel;
        assertNull(fieldSet.getLegend());
        assertNull(fieldSet.getLegendAttributes());
        action.end(Tag.FIELDSET);

        action.start(Tag.LEGEND, attr);
        reader.handleText(text1.toCharArray(), 0);
        action.end(Tag.LEGEND);
        assertNull(fieldSet.getLegend());
        assertNull(fieldSet.getLegendAttributes());
    }
    
    public void testFormEnd_Legend() throws Exception {
        if (isHarmony()) {
            checkEmptyFormEnd(Tag.LEGEND, null);
        }
    }

    private void checkStandardInputStart(final String type, final Class modelClass) throws Exception {
        checkStandardFormStart(Tag.INPUT, modelClass, type, null);
    }
    
    private void checkStandardFormStart(final Tag tag, final Class modelClass) throws Exception {
        checkStandardFormStart(tag, modelClass, null, null);
    }
    
    private void checkStandardFormStart(final Tag tag, final Class modelClass, final AttributeSet attr) throws Exception {
        checkStandardFormStart(tag, modelClass, null, attr);
    }
    
    private void checkStandardFormStart(final Tag tag, final Class modelClass, final String type, final AttributeSet additionalAttr) throws Exception {
        doFormStart(tag, type, additionalAttr);
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        checkOpenImpliedSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(1);
        AttributeSet specAttr = spec.getAttributes();
        
        int addendum = additionalAttr != null ? additionalAttr.getAttributeCount() : 0;
        int attrNum = 2;
        if (modelClass != null) {
            attrNum++;
        }
        if (Tag.INPUT.equals(tag)) {
            attrNum++;
        }
        assertEquals(attrNum + addendum, specAttr.getAttributeCount());

        if (additionalAttr != null) {
            assertTrue(specAttr.containsAttributes(additionalAttr));
        }
        checkAttributes(specAttr, StyleConstants.NameAttribute, tag);
        if (type != null) {
            checkAttributes(specAttr, HTML.Attribute.TYPE, type);
        }
        checkAttributes(specAttr, "aaaa", "bbbb");
        if (modelClass != null) {
            assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {' '});
            Object contentModel = getModel(spec);
            assertNotNull(contentModel);
            assertTrue(modelClass.isAssignableFrom(contentModel.getClass()));
        }
    }

    private Object getModel(final ElementSpec spec) {
        return spec.getAttributes().getAttribute(StyleConstants.ModelAttribute);
    }

    private void doFormStart(final Tag tag, final String type, final AttributeSet additionalAttr) {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        if (additionalAttr != null) {
            attr.addAttributes(additionalAttr);
        }
        if (type != null) {
            attr.addAttribute(HTML.Attribute.TYPE, type);
        }
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new FormAction();
        
        action.start(tag, attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
    }
    
    private void checkEmptyFormEnd(final Tag tag, final String type) throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        if (type != null) {
            attr.addAttribute(HTML.Attribute.TYPE, type);
        }

        action = reader.new FormAction();
        action.start(tag, attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        int numSpecs = reader.parseBuffer.size();
        action.end(tag);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(numSpecs, reader.parseBuffer.size());
    }
}
