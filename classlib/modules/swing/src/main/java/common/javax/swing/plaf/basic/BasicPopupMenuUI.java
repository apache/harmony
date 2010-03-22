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
package javax.swing.plaf.basic;

import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.PopupMenuUI;

import org.apache.harmony.x.swing.Utilities;


public class BasicPopupMenuUI extends PopupMenuUI {

    private static final String PROPERTY_PREFIX = "PopupMenu";

    protected JPopupMenu popupMenu;

    public static ComponentUI createUI(final JComponent c) {
        return new BasicPopupMenuUI();
    }

    public void installDefaults() {
        LookAndFeel.installColorsAndFont(popupMenu, PROPERTY_PREFIX + ".background",
                                         PROPERTY_PREFIX + ".foreground",
                                         PROPERTY_PREFIX + ".font");
        LookAndFeel.installBorder(popupMenu, PROPERTY_PREFIX + ".border");
        LookAndFeel.installProperty(popupMenu, "opaque", Boolean.TRUE);
    }

    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(popupMenu);
        Utilities.uninstallColorsAndFont(popupMenu);
    }

    protected void installKeyboardActions() {
        if (popupMenu == null) {
            return;
        }
        Utilities.installKeyboardActions(popupMenu, JComponent.WHEN_FOCUSED,
                                         PROPERTY_PREFIX + ".selectedWindowInputMapBindings",
                                         PROPERTY_PREFIX + ".selectedWindowInputMapBindings.RightToLeft");

    }

    protected void uninstallKeyboardActions() {
        Utilities.uninstallKeyboardActions(popupMenu, JComponent.WHEN_FOCUSED);
    }

    protected void installListeners() {
        RootPaneFocusHandler.attach();
    }

    protected void uninstallListeners() {
        RootPaneFocusHandler.detach();
    }

    public void installUI(final JComponent c) {
        popupMenu = (JPopupMenu)c;
        if (Utilities.isUIResource(popupMenu.getLayout())) {
            popupMenu.setLayout(new DefaultMenuLayout(popupMenu, BoxLayout.Y_AXIS));
        }
        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(final JComponent c) {
        uninstallKeyboardActions();
        uninstallListeners();
        uninstallDefaults();
        popupMenu = null;
    }

    public boolean isPopupTrigger(final MouseEvent event) {
        if (event == null) {
            throw new NullPointerException();
        }
 
        return false;
    }
}

