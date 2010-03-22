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

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.SeparatorUI;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class JSeparator extends JComponent implements SwingConstants, Accessible {

    protected class AccessibleJSeparator extends AccessibleJComponent {
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SEPARATOR;
        }
    }

    private final static String UI_CLASS_ID = "SeparatorUI";

    private int orientation;

    public JSeparator() {
        this(SwingConstants.HORIZONTAL);
    }

    public JSeparator(final int orientation) {
        checkOrientation(orientation);
        this.orientation = orientation;
        setFocusable(false);
        updateUI();
    }

    public SeparatorUI getUI() {
        return (SeparatorUI)ui;
    }

    public void setUI(final SeparatorUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
        setUI(UIManager.getUI(this));
    }

    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(final int orientation) {
        checkOrientation(orientation);
        this.orientation = orientation;
    }

    public AccessibleContext getAccessibleContext() {
        return (accessibleContext == null)
                ? (accessibleContext = new AccessibleJSeparator())
                : accessibleContext;
    }

    private void checkOrientation(final int orientation) {
        if (orientation == SwingConstants.HORIZONTAL
            || orientation == SwingConstants.VERTICAL) {
            return;
        }

        throw new IllegalArgumentException(Messages.getString("swing.47")); //$NON-NLS-1$
    }

}
