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
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.text.html.HTML;


public class FormTextModel extends PlainDocument implements FormElement {

    public static final boolean ENABLE_MAX_LENGTH_BOUND = true;
    public static final boolean DISABLE_MAX_LENGTH_BOUND = false;
    
    private final Form form;
    private final AttributeSet attributes;
    private final int maxLength;
    private String initialContent;
    
    public FormTextModel(final Form form, final AttributeSet attr) {
        this(form, attr, DISABLE_MAX_LENGTH_BOUND);
    }

    public FormTextModel(final Form form, final AttributeSet attr, final boolean enableLengthBound) {
        this.form = form;
        attributes = attr.copyAttributes();
        if (enableLengthBound) {
            String maxLengthAttr = (String)attributes.getAttribute(HTML.Attribute.MAXLENGTH);
            maxLength = maxLengthAttr != null ? Integer.parseInt(maxLengthAttr) : Integer.MAX_VALUE;
        } else {
            maxLength = Integer.MAX_VALUE;
        }
    }

    public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        int insertLength = Math.min(text.length() - length, maxLength - getLength());
        if (insertLength > 0) {
            super.replace(offset, length, text.substring(0, insertLength), attrs);
        }
    }
    
    public Form getForm() {
        return form;
    }

    public AttributeSet getAttributes() {
        return attributes;
    }

    public int getElementType() {
        int result = FormAttributes.getTypeAttributeIndex(attributes);
        return result != FormAttributes.INPUT_TYPE_INDEX_UNDEFINED ? result : FormAttributes.TEXTAREA_TYPE_INDEX;
    }
    
    public String getInitialContent() {
        return initialContent;
    }

    public void setInitialContent(final String initialContent) {
        this.initialContent = initialContent;
        try {
            insertString(getLength(), initialContent, null);
        } catch (BadLocationException e) { }
    }
}
