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

package javax.swing;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * <p>
 * <i>JCheckBoxMenuItem</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JCheckBoxMenuItem extends JMenuItem {
    private static final long serialVersionUID = 7596676985032928624L;

    protected class AccessibleJCheckBoxMenuItem extends AccessibleJMenuItem {
        private static final long serialVersionUID = -5343091705345502936L;

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.CHECK_BOX;
        }
    }

    private final static String UI_CLASS_ID = "CheckBoxMenuItemUI";

    public JCheckBoxMenuItem() {
        this(null, null, false);
    }

    public JCheckBoxMenuItem(Icon icon) {
        this(null, icon, false);
    }

    public JCheckBoxMenuItem(String text) {
        this(text, null, false);
    }

    public JCheckBoxMenuItem(String text, Icon icon) {
        this(text, icon, false);
    }

    public JCheckBoxMenuItem(String text, boolean selected) {
        this(text, null, selected);
    }

    public JCheckBoxMenuItem(String text, Icon icon, boolean selected) {
        setDefaultModelAndFocus();
        setSelected(selected);
        init(text, icon);
    }

    public JCheckBoxMenuItem(Action action) {
        super(action);
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public void setState(boolean b) {
        setSelected(b);
    }

    public boolean getState() {
        return isSelected();
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        return (accessibleContext == null) ? (accessibleContext = new AccessibleJCheckBoxMenuItem())
                : accessibleContext;
    }

    @Override
    ButtonModel createDefaultModel() {
        return new JToggleButton.ToggleButtonModel();
    }
}
