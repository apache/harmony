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
package javax.swing;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

public class JRadioButton extends JToggleButton {
    protected class AccessibleJRadioButton extends AccessibleJToggleButton {
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.RADIO_BUTTON;
        }
    };

    private static final String UI_CLASS_ID = "RadioButtonUI";

    // this constant is also used by JCheckBox
    static final Object NO_ICON_ACTION_PROPERTIES = new Object() {  //$NON-LOCK-1$
        public boolean equals(final Object o) {
            return !Action.SMALL_ICON.equals(o);
        }
    };


    public JRadioButton() {
        super(null, null, false);
    }

    public JRadioButton(final Action action) {
        super(action);
    }

    public JRadioButton(final Icon icon) {
        super(null, icon, false);
    }

    public JRadioButton(final Icon icon, final boolean selected) {
        super(null, icon, selected);
    }

    public JRadioButton(final String text) {
        super(text, null, false);
    }

    public JRadioButton(final String text, final boolean selected) {
        super(text, null, selected);
    }

    public JRadioButton(final String text, final Icon icon) {
        super(text, icon, false);
    }

    public JRadioButton(final String text, final Icon icon, final boolean selected) {
        super(text, icon, selected);
    }

    void configurePropertyFromAction(final Action action, final Object propertyName) {
        if (propertyName == null || propertyName.equals(Action.SMALL_ICON)) {
            return;
        }
        super.configurePropertyFromAction(action, propertyName);
    }

    protected void init(final String text, final Icon icon) {
        setHorizontalAlignment(LEADING);
        super.init(text, icon);
    }

    public AccessibleContext getAccessibleContext() {
        return (accessibleContext == null) ? (accessibleContext = new AccessibleJRadioButton())
                : accessibleContext;
    }

    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    Object getActionPropertiesFilter() {
        return NO_ICON_ACTION_PROPERTIES;
    }
}

