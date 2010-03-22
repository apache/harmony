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

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.DesktopPaneUI;

/**
 * <p>
 * <i>JDesktopPane</i> is a container used to create a virtual desktop.
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JDesktopPane extends JLayeredPane implements Accessible {
    private static final long serialVersionUID = -5428199090710889698L;

    /**
     * Indicates that the entire content of the component should appear,
     * when it is being dragged.
     */
    public static final int LIVE_DRAG_MODE = 0;

    /**
     * Indicates that an outline should appear instead of the entire content
     * of the component, when it is being dragged.
     */
    public static final int OUTLINE_DRAG_MODE = 1;

    protected class AccessibleJDesktopPane extends AccessibleJComponent {
        private static final long serialVersionUID = -44586801937888192L;

        protected AccessibleJDesktopPane() {
            super();
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.DESKTOP_PANE;
        }
    }

    // The style of dragging to use.
    private int dragMode = LIVE_DRAG_MODE;

    // The desktop manager.
    private DesktopManager desktopManager;

    // The currently active internal frame.
    private JInternalFrame selectedFrame;

    public JDesktopPane() {
        setFocusCycleRoot(true);
        updateUI();
    }

    /**
     * Sets the UI object for this component.
     *
     * @param ui the UI object to set
     */
    public void setUI(final DesktopPaneUI ui) {
        // setUI() from super (JComponent) should always be called
        super.setUI(ui);
    }

    /**
     * Returns the UI object for this component.
     *
     * @return UI object for this component
     */
    public DesktopPaneUI getUI() {
        return (DesktopPaneUI) ui;
    }

    /**
     * Resets UI object with the default object from <code>UIManager</code>
     */
    @Override
    public void updateUI() {
        setUI((DesktopPaneUI) UIManager.getUI(this));
    }

    /*
     * Returns all internal frames in the array including iconified frames.
     */
    private JInternalFrame[] getAllFramesInArray(Component[] array) {
        List<JInternalFrame> result = new ArrayList<JInternalFrame>(array.length);
        for (int i = 0; i < array.length; i++) {
            if (array[i] instanceof JInternalFrame) {
                // internal frame
                result.add((JInternalFrame) array[i]);
            } else if (array[i] instanceof JInternalFrame.JDesktopIcon) {
                // iconified internal frame
                result.add(((JInternalFrame.JDesktopIcon) array[i]).getInternalFrame());
            }
        }
        return result.toArray(new JInternalFrame[result.size()]);
    }

    /**
     * Returns all internal frames in the specified layer. Iconified frames
     * are also taken into account.
     *
     * @param layer the layer to search for internal frames
     *
     * @return all internal frames in the specified layer
     */
    public JInternalFrame[] getAllFramesInLayer(final int layer) {
        return getAllFramesInArray(getComponentsInLayer(layer));
    }

    /**
     * Returns all internal frames in the desktop pane including iconified
     * frames.
     *
     * @return all internal frames in the desktop pane
     */
    public JInternalFrame[] getAllFrames() {
        return getAllFramesInArray(getComponents());
    }

    /**
     * Sets the currently active internal frame.
     *
     * @param f the currently active internal frame
     */
    public void setSelectedFrame(final JInternalFrame f) {
        selectedFrame = f;
    }

    /**
     * Returns the currently active internal frame or <code>null</code>,
     * if there is no active frame.
     *
     * @return the currently active internal frame or <code>null</code>
     */
    public JInternalFrame getSelectedFrame() {
        return selectedFrame;
    }

    /**
     * Sets the desktop manager.
     *
     * @param m the desktop manager
     */
    public void setDesktopManager(final DesktopManager m) {
        DesktopManager oldValue = getDesktopManager();
        desktopManager = m;
        firePropertyChange("desktopManager", oldValue, m);
    }

    /**
     * Returns the desktop manager.
     *
     * @return the desktop manager
     */
    public DesktopManager getDesktopManager() {
        return desktopManager;
    }

    /**
     * Returns the accessible context for the desktop pane.
     *
     * @return the accessible context for the desktop pane
     */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJDesktopPane();
        }
        return accessibleContext;
    }

    /**
     * Returns string representation of this desktop pane.
     *
     * @return string representation of this desktop pane
     */
    @Override
    protected String paramString() {
        return super.paramString();
    }

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string "DesktopPaneUI"
     */
    @Override
    public String getUIClassID() {
        return "DesktopPaneUI";
    }

    /**
     * Always returns true.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean isOpaque() {
        return true;
    }

    /**
     * Sets the style of dragging used by desktop pane.
     *
     * @param mode the style of dragging to use
     */
    public void setDragMode(final int mode) {
        LookAndFeel.markPropertyNotInstallable(this, "dragMode");
        dragMode = mode;
    }

    /**
     * Returns the style of dragging used by desktop pane.
     *
     * @return the used style of dragging
     */
    public int getDragMode() {
        return dragMode;
    }
}
