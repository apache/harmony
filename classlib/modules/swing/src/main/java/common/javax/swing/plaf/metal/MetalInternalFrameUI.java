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
 * @author Vadim L. Bogdanov
 */

package javax.swing.plaf.metal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicInternalFrameUI;

public class MetalInternalFrameUI extends BasicInternalFrameUI {
    protected static String IS_PALETTE = "JInternalFrame.isPalette";

    private static String IS_OPTION_DIALOG = "JInternalFrame.optionDialog";

    private MetalInternalFramePropertyChangeListener metalPropertyChangeListener;
    private MetalInternalFrameTitlePane titlePane;


    private class MetalInternalFramePropertyChangeListener implements PropertyChangeListener {
        public MetalInternalFramePropertyChangeListener() {
        }

        public void propertyChange(final PropertyChangeEvent e) {
            if (IS_PALETTE.equals(e.getPropertyName())) {
                setPalette(((Boolean)e.getNewValue()).booleanValue());
            } else if (IS_OPTION_DIALOG.equals(e.getPropertyName())) {
                setBorder();
            }
        }
    }

    public MetalInternalFrameUI(final JInternalFrame frame) {
        super(frame);
    }

    protected JComponent createNorthPane(final JInternalFrame f) {
        titlePane = new MetalInternalFrameTitlePane(f);
        return titlePane;
    }

    protected void uninstallComponents() {
        // nothing else to do
        super.uninstallComponents();
    }

    protected void installKeyboardActions() {
        // Note: may be we don't need to call super here
        // because we don't have menu in title pane
        super.installKeyboardActions();
    }

    protected void uninstallKeyboardActions() {
        // Note: may be we don't need to call super here
        // because we don't have menu in title pane
        super.uninstallKeyboardActions();
    }

    protected void installListeners() {
        super.installListeners();

        if (metalPropertyChangeListener == null) {
            metalPropertyChangeListener = new MetalInternalFramePropertyChangeListener();
        }
        frame.addPropertyChangeListener(metalPropertyChangeListener);
    }

    protected void uninstallListeners() {
        frame.removePropertyChangeListener(metalPropertyChangeListener);

        super.uninstallListeners();
    }

    public void installUI(final JComponent c) {
        super.installUI(c);

        setPalette(isPropertySet(IS_PALETTE));
    }

    public void uninstallUI(final JComponent c) {
        // Note: nothing else?
        super.uninstallUI(c);
    }

    public static ComponentUI createUI(final JComponent c) {
        return new MetalInternalFrameUI((JInternalFrame)c);
    }

    public void setPalette(final boolean b) {
        titlePane.setPalette(b);

        setBorder();
        // the layer isn't changed
    }
    
    private void setBorder() {
        if (titlePane.isPalette) {
            LookAndFeel.installBorder(frame, "InternalFrame.paletteBorder");
        } else if (isPropertySet(IS_OPTION_DIALOG)) {
            LookAndFeel.installBorder(frame, "InternalFrame.optionDialogBorder");
        } else {
            LookAndFeel.installBorder(frame, "InternalFrame.border");
        }
    }
    
    private boolean isPropertySet(final String propertyName) {
        Boolean b = (Boolean)frame.getClientProperty(propertyName);
        return b != null && b.booleanValue();
    }
}
