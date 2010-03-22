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
 * @author Alexey A. Ivanov
 */
package javax.swing.text.html;

import javax.swing.text.AttributeSet;

public class Option {
    private final AttributeSet attr;
    private String label;
    private boolean selected;

    public Option(final AttributeSet attr) {
        this.attr = attr.copyAttributes();
        selected = this.attr.isDefined(HTML.Attribute.SELECTED);
    }

    public AttributeSet getAttributes() {
        return attr;
    }

    public String getLabel() {
        return label != null ? label : (String)getAttributes().getAttribute(HTML.Attribute.LABEL);
    }

    public String getValue() {
        final String value = (String)getAttributes().getAttribute(HTML.Attribute.VALUE);
        return value != null ? value : getLabel();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    protected void setSelection(final boolean state) {
        selected = state;
    }

    public String toString() {
        return label;
    }
}