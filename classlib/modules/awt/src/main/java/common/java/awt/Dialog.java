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

package java.awt;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

import org.apache.harmony.awt.internal.nls.Messages;

public class Dialog extends Window {
    private static final long serialVersionUID = 5920926903803293709L;

    private DialogModalContext modalContext;

    private final class DialogModalContext extends ModalContext {
        private Window restoreActive;

        void runModalLoop(Window active) {
            restoreActive = active;
            super.runModalLoop();
        }

        @Override
        public void endModalLoop() {
            if (restoreActive != null) {
                restoreActive.toFront();
                restoreActive = null;
            }
            super.endModalLoop();
        }
    }

    protected class AccessibleAWTDialog extends AccessibleAWTWindow {
        private static final long serialVersionUID = 4837230331833941201L;

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            toolkit.lockAWT();
            try {
                AccessibleStateSet set = super.getAccessibleStateSet();
                if (isModal()) {
                    set.add(AccessibleState.MODAL);
                }
                return set;
            } finally {
                toolkit.unlockAWT();
            }
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            toolkit.lockAWT();
            try {
                return AccessibleRole.DIALOG;
            } finally {
                toolkit.unlockAWT();
            }
        }
    }

    public Dialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, gc);
        toolkit.lockAWT();
        try {
            setTitle(title);
            setModal(modal);
            setResizable(true);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, gc);
        toolkit.lockAWT();
        try {
            setTitle(title);
            setModal(modal);
            setFocusable(true);
            setResizable(true);
            setUndecorated(false);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dialog(Frame owner, String title, boolean modal) {
        this(owner, title, modal, null);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dialog(Frame owner, String title) {
        this(owner, title, false, null);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dialog(Dialog owner, String title, boolean modal) {
        this(owner, title, modal, null);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dialog(Dialog owner, String title) {
        this(owner, title, false, null);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dialog(Frame owner, boolean modal) {
        this(owner, "", modal, null); //$NON-NLS-1$
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dialog(Frame owner) {
        this(owner, "", false, null); //$NON-NLS-1$
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dialog(Dialog owner) {
        this(owner, "", false, null); //$NON-NLS-1$
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    protected String paramString() {
        toolkit.lockAWT();
        try {
            return super.paramString() + ",title=" + getTitle() //$NON-NLS-1$
                    + (isResizable() ? ",resizable" : "") + (isModal() ? ",modal" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void dispose() {
        toolkit.lockAWT();
        try {
            if (modalContext != null && modalContext.isModalLoopRunning()) {
                modalContext.endModalLoop();
            }
            super.dispose();
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

    @Deprecated
    @Override
    public void hide() {
        toolkit.lockAWT();
        try {
            hideImpl();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @SuppressWarnings("deprecation")
    void hideImpl() {
        if (modalContext != null && modalContext.isModalLoopRunning()) {
            modalContext.endModalLoop();
            super.hide();
        } else {
            super.hide();
        }
    }

    @Deprecated
    @Override
    public void show() {
        showImpl();
    }

    @SuppressWarnings("deprecation")
    void showImpl() {
        if (isModal()) {
            if (EventQueue.isDispatchThread()) {
                showModal();
            } else {
                toolkit.lockAWT();
                try {
                    toolkit.unsafeInvokeAndWait(new Runnable() {
                        public void run() {
                            showModal();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } finally {
                    toolkit.unlockAWT();
                }
            }
        } else {
            super.show();
        }
    }

    @Override
    public String getTitle() {
        toolkit.lockAWT();
        try {
            return super.getTitle();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isModal() {
        toolkit.lockAWT();
        try {
            return modalContext != null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public boolean isResizable() {
        toolkit.lockAWT();
        try {
            return super.isResizable();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public boolean isUndecorated() {
        toolkit.lockAWT();
        try {
            return super.isUndecorated();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setModal(boolean modal) {
        toolkit.lockAWT();
        try {
            if (modal == isModal()) {
                return;
            }
            if (isVisible()) {
                // awt.124=Cannot change the modality while the dialog is visible
                throw new IllegalComponentStateException(Messages.getString("awt.124")); //$NON-NLS-1$
            }
            modalContext = modal ? new DialogModalContext() : null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void setResizable(boolean resizable) {
        toolkit.lockAWT();
        try {
            super.setResizable(resizable);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
    }

    @Override
    public void setUndecorated(boolean undecorated) {
        toolkit.lockAWT();
        try {
            super.setUndecorated(undecorated);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @SuppressWarnings("deprecation")
    private void showModal() {
        Collection<Window> otherWindows;
        Window active;
        toolkit.lockAWT();
        try {
            active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            otherWindows = disableOtherWindows();
            super.show();
        } finally {
            toolkit.unlockAWT();
        }
        modalContext.runModalLoop(active);
        enableOtherWindows(otherWindows);
    }

    private Collection<Window> disableOtherWindows() {
        Iterator<?> i = toolkit.windows.iterator();
        LinkedList<Window> result = new LinkedList<Window>();
        while (i.hasNext()) {
            Object obj = i.next();
            if (obj instanceof Window) {
                Window w = (Window) obj;
                if (w.isEnabled() && w != this) {
                    w.setEnabled(false);
                    result.add(w);
                }
            }
        }
        return result;
    }

    private void enableOtherWindows(Collection<Window> disabledWindows) {
        Iterator<Window> i = disabledWindows.iterator();
        while (i.hasNext()) {
            Window w = i.next();
            w.setEnabled(true);
        }
    }

    @Override
    AccessibleContext createAccessibleContext() {
        return new AccessibleAWTDialog();
    }

    @Override
    String autoName() {
        int number = toolkit.autoNumber.nextDialog++;
        return "dialog" + Integer.toString(number); //$NON-NLS-1$
    }

    @Override
    Color getDefaultBackground() {
        return SystemColor.control;
    }

    void runModalLoop() {
        modalContext.runModalLoop();
    }

    void endModalLoop() {
        modalContext.endModalLoop();
    }
}
