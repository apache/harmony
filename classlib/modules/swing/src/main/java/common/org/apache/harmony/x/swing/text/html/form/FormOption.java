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
import javax.swing.text.html.HTML;
import javax.swing.text.html.Option;

public class FormOption extends Option {

    private final FormOptionGroup group;

    public FormOption(final FormOptionGroup group, final AttributeSet attr) {
        super(attr);
        this.group = group;
    }

    public FormOption(final AttributeSet attr) {
        this(null, attr);
    }

    public int getDepth() {
        return (group != null) ? group.getDepth() + 1 : 0;
    }
    
    public String getTitle() {
        final String title = (String)getAttributes().getAttribute(HTML.Attribute.TITLE);
        return title != null ? title : ((group != null) ? group.getTitle() : null);
    }
    
    public boolean isEnabled() {
        final boolean enabled = (getAttributes().getAttribute(HTML.getAttributeKey("disabled")) == null);
        return enabled && (group == null || group.isEnabled());
    }
}