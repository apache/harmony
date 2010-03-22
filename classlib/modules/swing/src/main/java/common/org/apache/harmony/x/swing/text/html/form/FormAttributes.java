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
package org.apache.harmony.x.swing.text.html.form;

import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Tag;

public class FormAttributes {
    public static final String INPUT_TYPE_BUTTON = "button";
    public static final String INPUT_TYPE_CHECKBOX = "checkbox";
    public static final String INPUT_TYPE_FILE = "file";
    public static final String INPUT_TYPE_IMAGE = "image";
    public static final String INPUT_TYPE_PASSWORD = "password";
    public static final String INPUT_TYPE_RADIO = "radio";
    public static final String INPUT_TYPE_RESET = "reset";
    public static final String INPUT_TYPE_SUBMIT = "submit";
    public static final String INPUT_TYPE_TEXT = "text";
    
    public static final int INPUT_TYPE_INDEX_UNDEFINED = -1;
    public static final int INPUT_TYPE_BUTTON_INDEX = 0;
    public static final int INPUT_TYPE_CHECKBOX_INDEX = 1;
    public static final int INPUT_TYPE_FILE_INDEX = 2;
    public static final int INPUT_TYPE_IMAGE_INDEX = 3;
    public static final int INPUT_TYPE_PASSWORD_INDEX = 4;
    public static final int INPUT_TYPE_RADIO_INDEX = 5;
    public static final int INPUT_TYPE_RESET_INDEX = 6;
    public static final int INPUT_TYPE_SUBMIT_INDEX = 7;
    public static final int INPUT_TYPE_TEXT_INDEX = 8;

    public static final int TEXTAREA_TYPE_INDEX = 9;
    public static final int SELECT_LIST_TYPE_INDEX = 10;
    public static final int SELECT_COMBOBOX_TYPE_INDEX = 11;
    public static final int FIELDSET_TYPE_INDEX = 12;
    
    public static int getTypeAttributeIndex(final String type) {
        if (INPUT_TYPE_BUTTON.compareToIgnoreCase(type) == 0) {
            return INPUT_TYPE_BUTTON_INDEX;
        } else if (INPUT_TYPE_CHECKBOX.compareToIgnoreCase(type) == 0) {
            return INPUT_TYPE_CHECKBOX_INDEX;
        } else if (INPUT_TYPE_IMAGE.compareToIgnoreCase(type) == 0) {
            return INPUT_TYPE_IMAGE_INDEX;
        } else if (INPUT_TYPE_PASSWORD.compareToIgnoreCase(type) == 0) {
            return INPUT_TYPE_PASSWORD_INDEX;
        } else if (INPUT_TYPE_RADIO.compareToIgnoreCase(type) == 0) {
            return INPUT_TYPE_RADIO_INDEX;
        } else if (INPUT_TYPE_RESET.compareToIgnoreCase(type) == 0) {
            return INPUT_TYPE_RESET_INDEX;
        } else if (INPUT_TYPE_SUBMIT.compareToIgnoreCase(type) == 0) {
            return INPUT_TYPE_SUBMIT_INDEX;
        } else if (INPUT_TYPE_TEXT.compareToIgnoreCase(type) == 0) {
            return INPUT_TYPE_TEXT_INDEX;
        } else if (INPUT_TYPE_FILE.compareToIgnoreCase(type) == 0) {
            return INPUT_TYPE_FILE_INDEX;
        }

        return INPUT_TYPE_INDEX_UNDEFINED;
    }

    public static int getElementTypeIndex(final AttributeSet attr) {
        Object tag = attr.getAttribute(StyleConstants.NameAttribute);
        if (Tag.INPUT.equals(tag)) {
            return getTypeAttributeIndex(attr);
        } else if (Tag.TEXTAREA.equals(tag)) {
            return TEXTAREA_TYPE_INDEX;
        } else if (HTML.getTag("fieldset").equals(tag)) {
            return FIELDSET_TYPE_INDEX;
        } else if (Tag.SELECT.equals(tag)) {
            return isListSelect(attr) ? SELECT_LIST_TYPE_INDEX : SELECT_COMBOBOX_TYPE_INDEX;
        }
        /*
         * Uncomment when BUTTON is implemented
         * 
         * else if (HTML.Tag.BUTTON.equals(tag)) { 
         *     return BUTTON_TYPE_INDEX; 
         * }
         */
        return INPUT_TYPE_INDEX_UNDEFINED;
    }

    public static boolean isListSelect(final AttributeSet attr) {
        boolean result = attr.getAttribute(HTML.Attribute.MULTIPLE) != null;
        result |= (getSelectSize(attr) > 1);
        return result;
    }

    public static int getSelectSize(final AttributeSet attrs) {
        final String sizeAttr = (String)attrs.getAttribute(HTML.Attribute.SIZE);
        int result = 0;
        if (sizeAttr != null) {
            try {
                result = Integer.parseInt(sizeAttr);
            } catch (NumberFormatException e) {
            }
        }
        return result;
    }

    public static int getTypeAttributeIndex(final AttributeSet attr) {
        final String type = (String)attr.getAttribute(HTML.Attribute.TYPE);
        if (type == null) {
            return INPUT_TYPE_INDEX_UNDEFINED;
        }
        return getTypeAttributeIndex(type);
    }
}
