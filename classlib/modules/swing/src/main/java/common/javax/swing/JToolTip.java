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
 * @author Sergey Burlak
 */
package javax.swing;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.ToolTipUI;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class JToolTip extends JComponent implements Accessible {
    private static final String COMPONENT = "component";
    private static final String TIP_TEXT = "tiptext";
    private String tipText;
    private JComponent comp;

    protected class AccessibleJToolTip extends AccessibleJComponent {
        public String getAccessibleDescription() {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
        public AccessibleRole getAccessibleRole() {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
    }

    public JToolTip() {
        updateUI();
    }

    public ToolTipUI getUI() {
        return (ToolTipUI)ui;
    }

    public void updateUI() {
        setUI((ToolTipUI)UIManager.getUI(this));
    }

    public String getUIClassID() {
        return "ToolTipUI";
    }

    public void setTipText(final String tipText) {
        if (tipText != this.tipText || (tipText != null && !tipText.equals(this.tipText))) {
            String oldValue = this.tipText;
            this.tipText = tipText;
            firePropertyChange(TIP_TEXT, oldValue, tipText);
        }
    }

    public String getTipText() {
        return tipText;
    }

    public void setComponent(final JComponent c) {
        if (c != comp) {
            JComponent oldValue = comp;
            comp = c;
            firePropertyChange(COMPONENT, oldValue, c);
        }
    }

    public JComponent getComponent() {
        return comp;
    }

    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJToolTip();
        }

        return accessibleContext;
    }
}
