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
 * @author Pavel Dolgov
 */
package java.awt;

import java.lang.reflect.InvocationTargetException;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

import org.apache.harmony.awt.internal.nls.Messages;

public class PopupMenu extends Menu {


    private static final long serialVersionUID = -4620452533522760060L;

    protected class AccessibleAWTPopupMenu extends AccessibleAWTMenu {

        private static final long serialVersionUID = -4282044795947239955L;

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.POPUP_MENU;
        }

    }

    public PopupMenu() throws HeadlessException {
        this(""); //$NON-NLS-1$
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public PopupMenu(String label) throws HeadlessException {
        super(label);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void addNotify() {
        toolkit.lockAWT();
        try {
            super.addNotify();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        toolkit.lockAWT();
        try {
            return super.getAccessibleContext();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void show(final Component origin, final int x, final int y) {
        toolkit.lockAWT();
        try {
            MenuContainer parent = getParent();
            if (parent == null) {
                // awt.71=Parent is null
                throw new NullPointerException(Messages.getString("awt.71")); //$NON-NLS-1$
            }
            if (!(parent instanceof Component)) {
                // awt.11F=parent is not a component
                throw new IllegalArgumentException(Messages.getString("awt.11F")); //$NON-NLS-1$
            }
            if (parent != origin && !( (parent instanceof Container) &&
                    ((Container)parent).isAncestorOf(origin) ) ) {
                // awt.120=origin is not a descendant of parent
                throw new IllegalArgumentException(Messages.getString("awt.120")); //$NON-NLS-1$
            }
            if ( !((Component)parent).isShowing()) {
                // awt.121=parent must be showing on the screen
                throw new RuntimeException(Messages.getString("awt.121")); //$NON-NLS-1$
            }

            if (EventQueue.isDispatchThread()) {
                showImpl(origin, x, y);
            } else {
                try {
                    toolkit.unsafeInvokeAndWait(new Runnable() {
                        public void run() {
                            showImpl(origin, x, y);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * Actually show the popup menu.
     * This method must be called on event dispatch thread
     *
     * @param origin - the component to show menu at
     * @param x - x coordinate relative to origin
     * @param y - y coordinate relative to origin
     */
    //
    private void showImpl(Component origin, int x, int y) {
        Point originLocation = origin.getLocationOnScreen();
        super.show(originLocation.x + x, originLocation.y + y, true);
    }

    @Override
    void hide() {
        if (!isVisible()) {
            return;
        }

        super.hide();
    }

    @Override
    boolean hasDefaultFont() {
        return true;
    }

    @Override
    AccessibleContext createAccessibleContext() {
        return new AccessibleAWTPopupMenu();
    }


}

