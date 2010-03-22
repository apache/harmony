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
package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

import org.apache.harmony.x.swing.Utilities;


public class BasicToolBarSeparatorUI extends BasicSeparatorUI {
    private Dimension preferredSize =
            UIManager.getDimension("ToolBar.separatorSize");

    public static ComponentUI createUI(final JComponent c) {
        return new BasicToolBarSeparatorUI();
    }

    protected void installDefaults(final JSeparator s) {
        JToolBar.Separator separator = (JToolBar.Separator)s;
        if (Utilities.isUIResource(separator.getSeparatorSize())) {
            separator.setSeparatorSize(preferredSize);
        }
    }

    public void paint(final Graphics g, final JComponent c) {
        // does nothing
    }

    public Dimension getPreferredSize(final JComponent c) {
        return new Dimension(preferredSize);
    }
}
