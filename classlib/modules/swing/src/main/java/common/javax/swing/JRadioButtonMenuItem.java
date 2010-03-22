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

public class JRadioButtonMenuItem extends JMenuItem {
    protected class AccessibleJRadioButtonMenuItem extends AccessibleJMenuItem {
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.RADIO_BUTTON;
        }
    }

    private final static String UI_CLASS_ID = "RadioButtonMenuItemUI";

    public JRadioButtonMenuItem() {
        this(null, null, false);
    }

    public JRadioButtonMenuItem(final Icon icon) {
        this(null, icon, false);
    }

    public JRadioButtonMenuItem(final String text) {
        this(text, null, false);
    }

    public JRadioButtonMenuItem(final String text, final boolean selected) {
        this(text, null, false);
    }

    public JRadioButtonMenuItem(final String text, final Icon icon) {
        this(text, icon, false);
    }

    public JRadioButtonMenuItem(final Icon icon, final boolean selected) {
        this(null, icon, selected);
    }

    public JRadioButtonMenuItem(final String text, final Icon icon,
                                final boolean selected) {
        setDefaultModelAndFocus();
        setSelected(selected);
        init(text, icon);
    }

    public JRadioButtonMenuItem(final Action action) {
        super(action);
    }

    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public AccessibleContext getAccessibleContext() {
        return (accessibleContext == null) ? (accessibleContext = new AccessibleJRadioButtonMenuItem())
                : accessibleContext;
    }

    ButtonModel createDefaultModel() {
        return new JToggleButton.ToggleButtonModel();
    }
}
