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

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;

public class FormSelectListModel extends DefaultListModel implements
        FormSelectModel {

    private final Form form;
    private final AttributeSet attributes;
    private final FormRootOptionGroup rootOptionGroup;
    private final ListSelectionModel selectionModel;

    public FormSelectListModel(final Form form, final AttributeSet attr,
                               final ListSelectionModel selectionModel) {
        this.form = form;
        attributes = attr.copyAttributes();
        rootOptionGroup = new FormRootOptionGroup(this);
        this.selectionModel = selectionModel; 
    }

    public FormSelectListModel(final Form form, final AttributeSet attr) {
        this(form, attr, new DefaultListSelectionModel());
    }

    public void addOption(final FormOption option) {
        addElement(option);
    }

    public FormOption getLastOption() {
        return !isEmpty() ? (FormOption)getElementAt(getSize() - 1) : null;
    }

    public FormOption getOption(final int index) {
        return (FormOption)getElementAt(index);
    }

    public Form getForm() {
        return form;
    }

    public AttributeSet getAttributes() {
        return attributes;
    }

    public int getElementType() {
        return FormAttributes.SELECT_LIST_TYPE_INDEX;
    }

    public FormRootOptionGroup getRootOptionGroup() {
        return rootOptionGroup;
    }
    
    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public boolean isEnabled() {
        return (getAttributes().getAttribute(HTML.getAttributeKey("disabled")) == null);
    }

    public String getTitle() {
        return (String)getAttributes().getAttribute(HTML.Attribute.TITLE);
    }

    public int getOptionCount() {
        return getSize();
    }
}
