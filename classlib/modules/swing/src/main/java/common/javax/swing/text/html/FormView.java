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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.html.FormViewComponentFactory.InputImageIcon;

import org.apache.harmony.x.swing.Utilities;
import org.apache.harmony.x.swing.text.html.form.Form;
import org.apache.harmony.x.swing.text.html.form.FormAttributes;
import org.apache.harmony.x.swing.text.html.form.FormButtonModel;
import org.apache.harmony.x.swing.text.html.form.FormElement;
import org.apache.harmony.x.swing.text.html.form.FormSelectComboBoxModel;
import org.apache.harmony.x.swing.text.html.form.FormSelectListModel;
import org.apache.harmony.x.swing.text.html.form.FormTextModel;
import org.apache.harmony.x.swing.text.html.form.FormToggleButtonModel;
import org.apache.harmony.x.swing.internal.nls.Messages;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class FormView extends ComponentView implements ActionListener {
    private static final int EMPTY_SPAN = 0;

    protected class MouseEventListener extends MouseAdapter {
        public void mouseReleased(final MouseEvent evt) {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
    }

    /**
     * @deprecated
     */
    public static final String RESET = new String("Reset");

    /**
     * @deprecated
     */
    public static final String SUBMIT = new String("Submit Query");

    private int inputTypeIndex = FormAttributes.INPUT_TYPE_INDEX_UNDEFINED;

    public FormView(final Element elem) {
        super(elem);
    }

    public float getMaximumSpan(final int axis) {
        if (axis != View.X_AXIS && axis != View.Y_AXIS) {
            throw new IllegalArgumentException(Messages.getString("swing.00", axis)); //$NON-NLS-1$ 
        }
        if (getComponent() == null || getParent() == null) {
            return EMPTY_SPAN;
        }

        Object tag = getElement().getAttributes()
                             .getAttribute(StyleConstants.NameAttribute);
        if (HTML.Tag.INPUT.equals(tag)
            || HTML.Tag.TEXTAREA.equals(tag)
            || HTML.Tag.BUTTON.equals(tag)) {

            return getPreferredSpan(axis);
        } else if (HTML.Tag.SELECT.equals(tag)) {
            if (getAttributes().getAttribute(HTML.Attribute.MULTIPLE) == null) {
                return getPreferredSpan(axis);
            }
        }
        if (axis == View.X_AXIS) {
            return getComponent().getMaximumSize().width + 2;
        } else {
            return getComponent().getMaximumSize().height;
        }
    }

    public void actionPerformed(final ActionEvent event) {
        final Object source = event.getSource();
        try {
            switch (inputTypeIndex) {
            case FormAttributes.INPUT_TYPE_PASSWORD_INDEX:
//                Document doc = ((JTextComponent) source).getDocument();
                /*
                 * If password is last element in form, form
                 * should be submitted.
                 *
                 * determineValidControls();
                 */
                break;
            case FormAttributes.INPUT_TYPE_TEXT_INDEX:
                /*
                 * If text is last element in form, form
                 * should be submitted.
                 *
                 * determineValidControls();
                 */
                break;
           case FormAttributes.INPUT_TYPE_SUBMIT_INDEX:
                final Form form = ((FormButtonModel)((AbstractButton)source)
                                                    .getModel()).getForm();
                determineValidControls(form);
                /*
                 * TODO Submit form.
                 */
                break;
            case FormAttributes.INPUT_TYPE_RESET_INDEX:
                resetForm(((FormButtonModel)((AbstractButton)source).getModel())
                          .getForm());
                break;
            default:
                // Do nothing
                break;
            }
        } catch (ClassCastException e) {
            // Do nothing
        }
    }

    public void preferenceChanged(final View child, final boolean width,
            final boolean height) {

        if (getParent() != null) {
            if (inputTypeIndex == FormAttributes.INPUT_TYPE_IMAGE_INDEX) {
                final AbstractButton image = (AbstractButton) getComponent();
                if (image == null) {
                    return;
                }
                final Dimension size;
                final Icon icon = image.getIcon();
                if (!(icon instanceof InputImageIcon)
                    || ((InputImageIcon)icon).imageWasLoaded()) {
                    image.setBorderPainted(false);
                    size = new Dimension(icon.getIconWidth(),
                                         icon.getIconHeight());
                } else {
                    size = image.getPreferredSize();
                }
                image.setMinimumSize(size);
                image.setPreferredSize(size);
                image.setMaximumSize(size);
                image.setContentAreaFilled(false);
                image.setFocusPainted(false);
            }
        }
        super.preferenceChanged(this, width, height);
    }

    protected Component createComponent() {
        try {
            final AttributeSet attrs = getElement().getAttributes();
            final FormElement model = (FormElement)attrs
                .getAttribute(StyleConstants.ModelAttribute);

//            if (model == null) {
//                return null;
//            }
//            inputTypeIndex = model.getElementType();
            inputTypeIndex = FormAttributes.getElementTypeIndex(attrs);

            switch (inputTypeIndex) {

            case FormAttributes.INPUT_TYPE_BUTTON_INDEX:
                return FormViewComponentFactory
                           .createInputButtonComponent(model, attrs);

            case FormAttributes.INPUT_TYPE_IMAGE_INDEX:
                return FormViewComponentFactory
                           .createInputImageComponent(model, attrs, this);

            case FormAttributes.INPUT_TYPE_RESET_INDEX:
                return FormViewComponentFactory
                           .createInputResetComponent(model, attrs, this);

            case FormAttributes.INPUT_TYPE_SUBMIT_INDEX:
                return FormViewComponentFactory
                           .createInputSubmitComponent(model, attrs, this);

            case FormAttributes.INPUT_TYPE_CHECKBOX_INDEX:
                return FormViewComponentFactory
                           .createInputCheckBoxComponent(model, attrs);

            case FormAttributes.INPUT_TYPE_RADIO_INDEX:
                return FormViewComponentFactory
                           .createInputRadioComponent(model, attrs);

            case FormAttributes.INPUT_TYPE_FILE_INDEX:
                return FormViewComponentFactory
                           .createInputFileComponent(model, attrs);

            case FormAttributes.INPUT_TYPE_PASSWORD_INDEX:
                return FormViewComponentFactory
                           .createInputPasswordComponent(model, attrs, this);

            case FormAttributes.INPUT_TYPE_TEXT_INDEX:
                return FormViewComponentFactory
                           .createInputTextComponent(model, attrs, this);

            case FormAttributes.TEXTAREA_TYPE_INDEX:
                return FormViewComponentFactory
                           .createTextAreaComponent(model, attrs, this);

            case FormAttributes.SELECT_LIST_TYPE_INDEX:
                return FormViewComponentFactory
                           .createSelectMultipleComponent(model, attrs);

            case FormAttributes.SELECT_COMBOBOX_TYPE_INDEX:
                return FormViewComponentFactory
                           .createSelectSimpleComponent(model, attrs);
            /*
             * TODO Uncomment this, when BUTTON model would be implemented.
             *
             *  case FormAttributes.BUTTON_TYPE_INDEX:
             *      return FormViewComponentFactory
             *                 .createButtonComponent(model, attrs, this);
             */
            default:
                // Do nothing
                break;
            }
        } catch (ClassCastException e) {
            // Do nothing
        }
        return null;

    }

    protected void imageSubmit(final String imageData) {
        // TODO implement imageSubmit
        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
    }

    protected void submitData(final String data) {
        // TODO implement submitData
        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
    }

    private void determineValidControls(final Form form) {
        FormElement formElement;

        for (int i = 0; i < form.getElementsCount(); i++) {
            formElement = form.getElement(i);

            switch (formElement.getElementType()) {
            case FormAttributes.INPUT_TYPE_BUTTON_INDEX :
            case FormAttributes.INPUT_TYPE_IMAGE_INDEX :
            case FormAttributes.INPUT_TYPE_RESET_INDEX :
            case FormAttributes.INPUT_TYPE_SUBMIT_INDEX :
            case FormAttributes.INPUT_TYPE_CHECKBOX_INDEX :
            case FormAttributes.INPUT_TYPE_RADIO_INDEX :
            case FormAttributes.INPUT_TYPE_FILE_INDEX :
            case FormAttributes.INPUT_TYPE_PASSWORD_INDEX :
            case FormAttributes.INPUT_TYPE_TEXT_INDEX :
            case FormAttributes.TEXTAREA_TYPE_INDEX :
            case FormAttributes.SELECT_LIST_TYPE_INDEX :
            case FormAttributes.SELECT_COMBOBOX_TYPE_INDEX :
                throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
            default :
                // Do nothing
                break;
            }
        }
    }

    private void resetForm(final Form form) {
        FormElement formElement;
        AttributeSet attrs;

        for (int i = 0; i < form.getElementsCount(); i++) {
            formElement = form.getElement(i);

            attrs = formElement.getAttributes();
            switch (formElement.getElementType()) {
            case FormAttributes.INPUT_TYPE_BUTTON_INDEX :
            case FormAttributes.INPUT_TYPE_IMAGE_INDEX :
            case FormAttributes.INPUT_TYPE_RESET_INDEX :
            case FormAttributes.INPUT_TYPE_SUBMIT_INDEX :
                //Do nothing
                break;
            case FormAttributes.INPUT_TYPE_CHECKBOX_INDEX :
            case FormAttributes.INPUT_TYPE_RADIO_INDEX :
                resetToogleButton((FormToggleButtonModel)formElement, attrs);
                break;
            case FormAttributes.INPUT_TYPE_FILE_INDEX :
                resetText((FormTextModel)formElement,
                          attrs, false);
                break;
            case FormAttributes.INPUT_TYPE_PASSWORD_INDEX :
            case FormAttributes.INPUT_TYPE_TEXT_INDEX :
            case FormAttributes.TEXTAREA_TYPE_INDEX :
                resetText((FormTextModel)formElement,
                          attrs, true);
                break;
            case FormAttributes.SELECT_LIST_TYPE_INDEX :
                FormViewUtils.resetMultipleSelection((FormSelectListModel)
                                                         formElement);
                break;
            case FormAttributes.SELECT_COMBOBOX_TYPE_INDEX :
                FormViewUtils.resetSimpleSelection((FormSelectComboBoxModel)
                                                       formElement);
                break;
            default :
                // Do nothing
                break;
            }
        }
    }

    private void resetText(final FormTextModel document,
                           final AttributeSet attrs,
                           final boolean loadDefaultText) {
        try {
            document.remove(0, document.getLength());

            String initialContent = document.getInitialContent();
            if (initialContent == null) {
                initialContent = (String)attrs.getAttribute(HTML.Attribute.VALUE);
            }

            if (loadDefaultText && !Utilities.isEmptyString(initialContent)) {
                document.insertString(0, initialContent, null);
            }
        } catch (BadLocationException e) {
        }
    }

    private void resetToogleButton(final JToggleButton.ToggleButtonModel model,
                                   final AttributeSet attrs) {
        //CHECKED
        model.setSelected(attrs.getAttribute(HTML.Attribute.CHECKED) != null);
    }
}
