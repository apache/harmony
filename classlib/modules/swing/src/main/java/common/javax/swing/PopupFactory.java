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
 * @author Anton Avtamonov
 */
package javax.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class PopupFactory {
    private static class ReleasedPopupStorage {
        private static final int STORAGE_CAPACITY = 5;

        private Map popups = new HashMap();

        public HWPopup retrieve(final Window owner) {
            Set popupSet = (Set)popups.get(owner);
            if (popupSet == null) {
                return null;
            }
            Iterator it = popupSet.iterator();
            HWPopup result = (HWPopup)it.next();
            it.remove();
            if (popupSet.isEmpty()) {
                popups.remove(owner);
            }

            return result;
        }

        public boolean store(final HWPopup popup) {
            Container owner = popup.getPopupOwner();
            if (owner == null) {
                return false;
            }
            Set popupSet = (Set)popups.get(owner);
            if (popupSet == null) {
                popupSet = new HashSet();
                popups.put(owner, popupSet);
            }
            if (popupSet.size() < STORAGE_CAPACITY) {
                popupSet.add(popup);
                return true;
            }

            return false;
        }

        public void remove(final HWPopup popup) {
            Container owner = popup.getPopupOwner();
            Set popupSet = (Set)popups.get(owner);
            if (popupSet != null) {
                popupSet.remove(popup);
                if (popupSet.isEmpty()) {
                    popups.remove(owner);
                }
            }
        }
    }

    private class HWPopup extends Popup {
        private class FactoryPopupWindow extends PopupWindow {
            public FactoryPopupWindow(final Window owner) {
                super(owner);
            }

            public void hide() {
                PopupFactory.this.releasePopup(HWPopup.this);
                super.hide();
            }

            public void dispose() {
                PopupFactory.this.dropPopup(HWPopup.this);
                super.dispose();
            }
        }

        public HWPopup(final Window owner, final Component content, final int x, final int y) {
            popupWindow = new FactoryPopupWindow(owner);
            reinit(content, x, y);
        }

        public void hide() {
            popupWindow.hide();
        }

        public void reinit(final Component content, final int x, final int y) {
            popupWindow.init(content, x, y);
        }

        public void dispose() {
            popupWindow.dispose();
        }

        public Container getPopupOwner() {
            return popupWindow.getOwner();
        }
    }

    private class LWPopup extends Popup {
        private final JPanel contentPane;
        private final JLayeredPane lp;

        public LWPopup(final Window owner, final Component content, final int x, final int y) {
            contentPane = new JPanel(new BorderLayout());
            lp = getLayeredPane(owner);

            contentPane.add(content);
            contentPane.setSize(content.getPreferredSize());
            Point location = new Point(x, y);
            SwingUtilities.convertPointFromScreen(location, lp);
            contentPane.setLocation(location);
        }

        public void show() {
            lp.add(contentPane, JLayeredPane.POPUP_LAYER);
        }

        public void hide() {
            lp.remove(contentPane);
        }
    }


    private static PopupFactory instance = new PopupFactory();
    private ReleasedPopupStorage popupStorage = new ReleasedPopupStorage();
    private boolean lwPopupsEnabled;

    public static void setSharedInstance(final PopupFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException(Messages.getString("swing.53")); //$NON-NLS-1$
        }

        instance = factory;
    }

    public static PopupFactory getSharedInstance() {
        return instance;
    }

    public Popup getPopup(final Component owner, final Component content,
                          final int x, final int y) throws IllegalArgumentException {
        if (content == null) {
            throw new IllegalArgumentException(Messages.getString("swing.52")); //$NON-NLS-1$
        }

        Window ownerWindow = owner instanceof Window ? (Window)owner : SwingUtilities.getWindowAncestor(owner);
        if (ownerWindow == null) {
            ownerWindow = JFrame.getSharedOwner();
        }

        if (lwPopupsEnabled) {
            JLayeredPane lp = getLayeredPane(ownerWindow);
            if (lp != null) {
                Point lpLocation = lp.getLocation();
                SwingUtilities.convertPointToScreen(lpLocation, lp);
                Rectangle surfaceBounds = new Rectangle(lpLocation, lp.getSize());
                Dimension prefSize = content.getPreferredSize();
                Rectangle contentBounds = new Rectangle(x, y, prefSize.width, prefSize.height);
                if (surfaceBounds.contains(contentBounds)) {
                    return new LWPopup(ownerWindow, content, x, y);
                }
            }
        }

        HWPopup result = popupStorage.retrieve(ownerWindow);
        if (result != null) {
            result.reinit(content, x, y);
            return result;
        }

        return new HWPopup(ownerWindow, content, x, y);
    }

    void setLWPopupsEnabled(final boolean enabled) {
        lwPopupsEnabled = enabled;
    }

    private void releasePopup(final HWPopup popup) {
        if (popupStorage.store(popup)) {
            return;
        }

        popup.dispose();
    }

    private void dropPopup(final HWPopup popup) {
        popupStorage.remove(popup);
    }

    private static JLayeredPane getLayeredPane(final Window w) {
        if (w instanceof JFrame) {
            return ((JFrame)w).getLayeredPane();
        } else if (w instanceof JDialog) {
            return ((JDialog)w).getLayeredPane();
        }

        return null;
    }
}
