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
 * @author Sergey Burlak, Anton Avtamonov
 */

package javax.swing.plaf.metal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;

public class MetalScrollPaneUI extends BasicScrollPaneUI {
    private PropertyChangeListener scrollBarSwapListener;

    // TODO: there is no clear scenario of swapping scrolbars to check what is expected here
    private class ScrollBarSwapListener implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent e) {
        }
    }

    public static ComponentUI createUI(final JComponent x) {
        return new MetalScrollPaneUI();
    }

    public void installUI(final JComponent c) {
        super.installUI(c);
    }

    public void uninstallUI(final JComponent c) {
        super.uninstallUI(c);
    }

    public void installListeners(final JScrollPane scrollPane) {
        super.installListeners(scrollPane);
        scrollBarSwapListener = createScrollBarSwapListener();
        scrollPane.addPropertyChangeListener(scrollBarSwapListener);
    }

    public void uninstallListeners(final JScrollPane scrollPane) {
        super.uninstallListeners(scrollPane);
        scrollPane.removePropertyChangeListener(scrollBarSwapListener);
        scrollBarSwapListener = null;
    }

    protected PropertyChangeListener createScrollBarSwapListener() {
        return new ScrollBarSwapListener();
    }
}