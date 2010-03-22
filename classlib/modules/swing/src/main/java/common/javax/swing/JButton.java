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

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import org.apache.harmony.x.swing.StringConstants;

/**
 * <p>
 * <i>JButton</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JButton extends AbstractButton implements Accessible {
    private static final long serialVersionUID = 8822265937932828454L;

    protected class AccessibleJButton extends AccessibleAbstractButton {
        private static final long serialVersionUID = -1171440163825721899L;

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PUSH_BUTTON;
        }
    }

    private boolean defaultCapable = true;

    private static final String UI_CLASS_ID = "ButtonUI";

    public JButton(String text, Icon icon) {
        setModel(new DefaultButtonModel());
        init(text, icon);
    }

    public JButton(Icon icon) {
        this(null, icon);
    }

    public JButton(Action action) {
        setModel(new DefaultButtonModel());
        setAction(action);
        init(getText(), getIcon());
    }

    public JButton(String text) {
        this(text, null);
    }

    public JButton() {
        this(null, null);
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        return (accessibleContext == null) ? (accessibleContext = new AccessibleJButton())
                : accessibleContext;
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public void setDefaultCapable(boolean defaultCapable) {
        boolean oldValue = this.defaultCapable;
        this.defaultCapable = defaultCapable;
        firePropertyChange(StringConstants.DEFAULT_CAPABLE_PROPERTY_CHANGED, oldValue,
                defaultCapable);
    }

    public boolean isDefaultCapable() {
        return defaultCapable;
    }

    public boolean isDefaultButton() {
        final JRootPane rootPane = getRootPane();
        return isDefaultButton(rootPane);
    }

    @Override
    public void removeNotify() {
        final JRootPane rootPane = getRootPane();
        if (isDefaultButton(rootPane)) {
            rootPane.setDefaultButton(null);
        }
        super.removeNotify();
    }

    private boolean isDefaultButton(JRootPane rootPane) {
        return (rootPane != null) && (rootPane.getDefaultButton() == this);
    }
}
