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

import javax.swing.JToggleButton;
import javax.swing.text.AttributeSet;

public class FormToggleButtonModel extends JToggleButton.ToggleButtonModel
        implements FormElement {

    private final Form form;
    private final AttributeSet attributes;

    public FormToggleButtonModel(final Form form, final AttributeSet attr) {
        this.form = form;
        attributes = attr.copyAttributes();
    }
    
    public Form getForm() {
        return form;
    }

    public AttributeSet getAttributes() {
        return attributes;
    }

    public int getElementType() {
        return FormAttributes.getTypeAttributeIndex(attributes);
    }
}
