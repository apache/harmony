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
package javax.swing.plaf.basic;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.PanelUI;

public class BasicPanelUI extends PanelUI {

    private static BasicPanelUI commonBasicPanelUI;

    private static final String PROPERTY_PREFIX = "Panel.";

    /**
     * Uninstalls all necessary UI properties from the component
     */
    public void uninstallUI(final JComponent component) {
        uninstallDefaults((JPanel)component);
    }

    /**
     * Installs all necessary UI properties on the component
     */
    public void installUI(final JComponent component) {
        installDefaults((JPanel)component);
    }

    /**
     *
     * Creates <code>BasicPanelUI</code> for component it is an instance
     * of <code>JPanel</code> class
     *
     * @return same <code>BasicPanelUI</code> instance for all <code>JPanel</code>
     * components
     */
    public static ComponentUI createUI(final JComponent component) {
        if (commonBasicPanelUI == null) {
            commonBasicPanelUI = new BasicPanelUI();
        }

        return commonBasicPanelUI;
    }

    protected void uninstallDefaults(final JPanel panel) {
        LookAndFeel.uninstallBorder(panel);
    }

    /**
     * Installs default properties for given panel
     */
    protected void installDefaults(final JPanel panel) {
        LookAndFeel.installColorsAndFont(panel, PROPERTY_PREFIX + "background",
                                         PROPERTY_PREFIX + "foreground",
                                         PROPERTY_PREFIX + "font");
        LookAndFeel.installBorder(panel, PROPERTY_PREFIX + "border");
    }

}

