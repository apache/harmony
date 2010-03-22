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

import java.util.Stack;

import javax.swing.ComboBoxModel;
import javax.swing.ListSelectionModel;
import javax.swing.text.AttributeSet;

import org.apache.harmony.x.swing.text.html.form.FormOption;
import org.apache.harmony.x.swing.text.html.form.FormSelectListModel;

final class FormViewUtils {

    private FormViewUtils() {
    }

    public static boolean selectOption(final FormOption option,
                                         final boolean isMultiple,
                                         final boolean notSelected) {

        if (isMultiple || notSelected) {
            AttributeSet attrs = option.getAttributes();

            // SELECTED
            Object attribute = attrs.getAttribute(HTML.Attribute.SELECTED);
            if (attribute != null) {
                option.setSelection(true);
                return true;
            }
        }
        return false;
    }

    public static void resetSimpleSelection(final ComboBoxModel model) {
        Object element;
        FormOption option;
        Stack options = new Stack();

        boolean notSelected = true;
        for (int i = model.getSize() - 1; i >= 0; i--) {
            element = model.getElementAt(i);
            if (element instanceof FormOption) {
                option = (FormOption) element;
                notSelected &= !selectElement(option, false, notSelected);
                if (option.isSelected()) {
                    model.setSelectedItem(element);
                }
                options.push(option);
            }
        }
        if (notSelected) {
            while (!options.isEmpty()) {
                option = (FormOption)options.pop();
                if (option.isEnabled()) {
                    model.setSelectedItem(option);
                    break;
                }
            }
        }
    }

    public static void resetMultipleSelection(final FormSelectListModel model) {
        Object item;
        FormOption option;

        ListSelectionModel selectionModel = model.getSelectionModel();
        selectionModel.clearSelection();

        for (int i = 0; i < model.getSize(); i++) {
            item = model.getElementAt(i);

            if (item instanceof FormOption) {
                option = (FormOption) item;
                selectElement(option, true, true);
                if (option.isSelected()) {
                    selectionModel.addSelectionInterval(i, i);
                }
            }
        }
    }

    private static boolean selectElement(final FormOption option,
                                      final boolean multiple,
                                      final boolean notSelected) {

        option.setSelection(false);
        if (option.getDepth() == 0) {
            return selectOption(option, multiple, notSelected);
        }
        return false;
    }

}
