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

import java.awt.LayoutManager;
import java.awt.FlowLayout;
import java.io.Serializable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.PanelUI;

/**
 * <code>JPanel</code> is a simple lightweight container.
 *
 */
public class JPanel extends JComponent implements Accessible, Serializable {

    protected class AccessibleJPanel extends JComponent.AccessibleJComponent {
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PANEL;
        }
    }

    private final static String UI_CLASS_ID = "PanelUI";

    /**
     *  Creates <code>JPanel</code> with given layout and buffering strategy
     *
     * @param layout specifies layout for the panel
     * @param isDoubleBuffered specifies buffering strategy for the panel
     */
    public JPanel(final LayoutManager layout, final boolean isDoubleBuffered) {
        setDoubleBuffered(isDoubleBuffered);
        setLayout(layout);
        LookAndFeel.installProperty(this, "opaque", Boolean.TRUE);
        updateUI();
    }

    /**
     *
     * Creates buffered <code>JPanel</code> with given layout
     *
     * @param layout specifies layout for the panel
     */
    public JPanel(final LayoutManager layout) {
        this(layout, true);
    }

    /**
     *
     * Creates <code>JPanel</code> with flowlayout and given buffering strategy
     *
     * @param isDoubleBuffered specifies the value for buffering strategy
     */
    public JPanel(final boolean isDoubleBuffered) {
        this(new FlowLayout(), isDoubleBuffered);
    }

    /**
     * Creates doubleBuffered <code>JPanel</code> with flowlayout
     */
    public JPanel() {
        this(new FlowLayout(), true);
    }

    /**
     *
     *  Sets new value for look-and-feel object for the panel
     *
     * @param ui new look-and-feel renderer
     */
    public void setUI(final PanelUI ui) {
        super.setUI(ui);
    }

    /**
     *
     * Gets the value of the look-and-feel object of the panel
     *
     * @return the <code>PanelUI</code> - components look-and-feel object
     */
    public PanelUI getUI() {
        return (PanelUI)ui;
    }

    /**
     *
     * Gets <code>AccessibleContext</code> of the panel.
     *
     * @return an <code>AccessibleJPanel</code> object as the <code>AccessibleContext</code> for this JPanel
     */
    public AccessibleContext getAccessibleContext() {
        return (accessibleContext == null) ? (accessibleContext = new AccessibleJPanel())
                                           : accessibleContext;
    }

    /**
     *
     *  Returns string that describes <code>JPanel</code>. Is usefull for debugging purposes.
     *
     * @return <code>JPanel</code> string representation
     */
    protected String paramString() {
        return super.paramString();
    }

    /**
     *
     * Returns name for panel look-and-feel class
     *
     * @return <code>"PanelUI"</code>
     */
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    /**
     * Sets value taken from current L&F to UI property of the panel
     */
    public void updateUI() {
        setUI((PanelUI)UIManager.getUI(this));
    }
}

