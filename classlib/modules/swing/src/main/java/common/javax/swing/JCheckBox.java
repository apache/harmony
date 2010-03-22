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
 * <i>JCheckBox</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JCheckBox extends JToggleButton {
    private static final long serialVersionUID = 2823978782065130270L;

    protected class AccessibleJCheckBox extends AccessibleJToggleButton {
        private static final long serialVersionUID = -7895379006459422318L;

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.CHECK_BOX;
        }
    }

    public static final String BORDER_PAINTED_FLAT_CHANGED_PROPERTY = "borderPaintedFlat";

    private static final String UI_CLASS_ID = "CheckBoxUI";

    private boolean borderPaintedFlat;

    public JCheckBox() {
        super(null, null, false);
    }

    public JCheckBox(Action action) {
        super(action);
    }

    public JCheckBox(Icon icon) {
        super(null, icon, false);
    }

    public JCheckBox(Icon icon, boolean selected) {
        super(null, icon, selected);
    }

    public JCheckBox(String text) {
        super(text, null, false);
    }

    public JCheckBox(String text, boolean selected) {
        super(text, null, selected);
    }

    public JCheckBox(String text, Icon icon) {
        super(text, icon, false);
    }

    public JCheckBox(String text, Icon icon, boolean selected) {
        super(text, icon, selected);
    }

    @Override
    void configurePropertyFromAction(Action action, Object propertyName) {
        if (propertyName == null || propertyName.equals(Action.SMALL_ICON)) {
            return;
        }
        super.configurePropertyFromAction(action, propertyName);
    }

    @Override
    protected void init(String text, Icon icon) {
        setHorizontalAlignment(LEADING);
        super.init(text, icon);
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        return (accessibleContext == null) ? (accessibleContext = new AccessibleJCheckBox())
                : accessibleContext;
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public boolean isBorderPaintedFlat() {
        return borderPaintedFlat;
    }

    public void setBorderPaintedFlat(boolean paintedFlat) {
        boolean oldValue = borderPaintedFlat;
        borderPaintedFlat = paintedFlat;
        firePropertyChange(BORDER_PAINTED_FLAT_CHANGED_PROPERTY, oldValue, borderPaintedFlat);
    }

    @Override
    Object getActionPropertiesFilter() {
        return JRadioButton.NO_ICON_ACTION_PROPERTIES;
    }
}
