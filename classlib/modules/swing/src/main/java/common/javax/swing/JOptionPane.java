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

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.plaf.OptionPaneUI;

import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class JOptionPane extends JComponent implements Accessible {

    protected class AccessibleJOptionPane extends AccessibleJComponent {
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.OPTION_PANE;
        }
    };

    private static class CloseOwnerListener implements PropertyChangeListener {
        private final Object owner;

        public CloseOwnerListener(final Object owner) {
            this.owner = owner;
        }

        public void propertyChange(final PropertyChangeEvent event) {
            if (owner instanceof Window) {
                ((Window)owner).dispose();
            } else if (owner instanceof JInternalFrame) {
                ((JInternalFrame)owner).dispose();
            }
        }
    };

    private class ClosingInternalFrameListener extends InternalFrameAdapter {
        private class Lock {}
        public final Object lock = new Lock();

        public void internalFrameClosed(final InternalFrameEvent e) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    };

    public static final String INITIAL_SELECTION_VALUE_PROPERTY = "initialSelectionValue";
    public static final String INITIAL_VALUE_PROPERTY = "initialValue";
    public static final String INPUT_VALUE_PROPERTY = "inputValue";
    public static final String MESSAGE_PROPERTY = "message";
    public static final String MESSAGE_TYPE_PROPERTY = "messageType";
    public static final String ICON_PROPERTY = "icon";
    public static final String OPTION_TYPE_PROPERTY = "optionType";
    public static final String OPTIONS_PROPERTY = "options";
    public static final String SELECTION_VALUES_PROPERTY = "selectionValues";
    public static final String VALUE_PROPERTY = "value";
    public static final String WANTS_INPUT_PROPERTY = "wantsInput";

    private static final String UI_CLASS_ID = "OptionPaneUI";
    private static final String INITIAL_MESSAGE = "JOptionPane message";
    private static final String CLOSE_OWNER_PROPERTY_NAME = "closeOwner";

    public static final int DEFAULT_OPTION = -1;
    public static final int YES_NO_OPTION = 0;
    public static final int YES_NO_CANCEL_OPTION = 1;
    public static final int OK_CANCEL_OPTION = 2;

    public static final int CLOSED_OPTION = -1;
    public static final int OK_OPTION = 0;
    public static final int YES_OPTION = 0;
    public static final int NO_OPTION = 1;
    public static final int CANCEL_OPTION = 2;

    public static final int PLAIN_MESSAGE = -1;
    public static final int ERROR_MESSAGE = 0;
    public static final int INFORMATION_MESSAGE = 1;
    public static final int WARNING_MESSAGE = 2;
    public static final int QUESTION_MESSAGE = 3;

    public static final Object UNINITIALIZED_VALUE = "uninitializedValue";

    protected Icon icon;
    protected Object initialSelectionValue;
    protected Object initialValue;
    protected Object inputValue = UNINITIALIZED_VALUE;

    protected Object message = INITIAL_MESSAGE;
    protected int messageType = PLAIN_MESSAGE;

    protected Object[] options;
    protected int optionType = DEFAULT_OPTION;

    protected Object[] selectionValues;
    protected Object value = UNINITIALIZED_VALUE;

    protected boolean wantsInput;

    private static Frame rootFrame;

    public JOptionPane() {
        updateUI();
    }

    public JOptionPane(final Object message) {
        setMessage(message);
        updateUI();
    }

    public JOptionPane(final Object message, final int messageType) {
        setMessage(message);
        setMessageType(messageType);
        updateUI();
    }

    public JOptionPane(final Object message, final int messageType, final int optionType) {
        setMessage(message);
        setMessageType(messageType);
        setOptionType(optionType);
        updateUI();
    }

    public JOptionPane(final Object message, final int messageType, final int optionType, final Icon icon) {
        setMessage(message);
        setMessageType(messageType);
        setOptionType(optionType);
        setIcon(icon);
        updateUI();
    }

    public JOptionPane(final Object message, final int messageType, final int optionType, final Icon icon, final Object[] options) {
        setMessage(message);
        setMessageType(messageType);
        setOptionType(optionType);
        setIcon(icon);
        setOptions(options);
        updateUI();
    }

    public JOptionPane(final Object message, final int messageType, final int optionType, final Icon icon, final Object[] options, final Object initialValue) {
        setMessage(message);
        setMessageType(messageType);
        setOptionType(optionType);
        setIcon(icon);
        setOptions(options);
        setInitialValue(initialValue);
        updateUI();
    }

    public static String showInputDialog(final Object message) throws HeadlessException {
        return (String)showInputDialog(null, message, null, QUESTION_MESSAGE, null, null, null);
    }

    public static String showInputDialog(final Object message, final Object initialSelectionValue) {
        return (String)showInputDialog(null, message, null, QUESTION_MESSAGE, null, null, initialSelectionValue);
    }

    public static String showInputDialog(final Component parentComponent, final Object message) throws HeadlessException {
        return (String)showInputDialog(parentComponent, message, null, QUESTION_MESSAGE, null, null, null);
    }

    public static String showInputDialog(final Component parentComponent, final Object message,
            final Object initialSelectionValue) {
        return (String)showInputDialog(parentComponent, message, null, QUESTION_MESSAGE, null, null, initialSelectionValue);
    }

    public static String showInputDialog(final Component parentComponent, final Object message,
            final String title, final int messageType) throws HeadlessException {
        return (String)showInputDialog(parentComponent, message, title, messageType, null, null, null);
    }

    public static Object showInputDialog(final Component parentComponent, final Object message,
            final String title, final int messageType, final Icon icon,
            final Object[] selectionValues, final Object initialSelectionValue) throws HeadlessException {
        JOptionPane pane = new JOptionPane(message, messageType, OK_CANCEL_OPTION, icon);
        pane.setSelectionValues(selectionValues);
        pane.setInitialSelectionValue(initialSelectionValue);
        pane.setWantsInput(true);

        JDialog dialog = pane.createDialog(parentComponent, title);
        setDialogDecorations(dialog, messageType);
        dialog.setVisible(true);

        return pane.getInputValue();
    }

    public static void showMessageDialog(final Component parentComponent, final Object message) throws HeadlessException {
        showMessageDialog(parentComponent, message, null, INFORMATION_MESSAGE, null);
    }

    public static void showMessageDialog(final Component parentComponent, final Object message,
            final String title, final int messageType) throws HeadlessException {
        showMessageDialog(parentComponent, message, title, messageType, null);
    }

    public static void showMessageDialog(final Component parentComponent, final Object message,
            final String title, final int messageType, final Icon icon) throws HeadlessException {
        JOptionPane pane = new JOptionPane(message, messageType, DEFAULT_OPTION, icon);
        JDialog dialog = pane.createDialog(parentComponent, title);
        setDialogDecorations(dialog, messageType);
        dialog.setVisible(true);
    }

    public static int showConfirmDialog(final Component parentComponent,
            final Object message) throws HeadlessException {
        return showConfirmDialog(parentComponent, message, null,
                                 YES_NO_CANCEL_OPTION, QUESTION_MESSAGE, null);
    }

    public static int showConfirmDialog(final Component parentComponent, final Object message,
            final String title, final int optionType) throws HeadlessException {
        return showConfirmDialog(parentComponent, message, title,
                                 optionType, QUESTION_MESSAGE, null);
    }

    public static int showConfirmDialog(final Component parentComponent, final Object message,
            final String title, final int optionType, final int messageType) throws HeadlessException {
        return showConfirmDialog(parentComponent, message, title, optionType, messageType, null);
    }

    public static int showConfirmDialog(final Component parentComponent, final Object message,
            final String title, final int optionType, final int messageType,
            final Icon icon) throws HeadlessException {
        JOptionPane pane = new JOptionPane(message, messageType, optionType, icon);
        JDialog dialog = pane.createDialog(parentComponent, title);
        setDialogDecorations(dialog, messageType);
        dialog.setVisible(true);

        return getResultedIndex(pane);
    }

    private ClosingInternalFrameListener createClosingInternalFrameListener() {
        return new ClosingInternalFrameListener();
    }

    private static void showInternalFrameAndWaitTillClosed(final JInternalFrame iFrame, final JOptionPane pane,
                                                           final Component parentComponent) {
        SwingUtilities.getWindowAncestor(parentComponent).setVisible(true);
        ClosingInternalFrameListener listener = pane.createClosingInternalFrameListener();
        iFrame.addInternalFrameListener(listener);
        iFrame.setVisible(true);

        synchronized (listener.lock) {
            try {
                listener.lock.wait();
            } catch (InterruptedException e) {}
        }
    }

    public static int showOptionDialog(final Component parentComponent, final Object message,
            final String title, final int optionType, final int messageType,
            final Icon icon, final Object[] options,
            final Object initialValue) throws HeadlessException {
        JOptionPane pane = new JOptionPane(message, messageType, optionType, icon, options, initialValue);
        JDialog dialog = pane.createDialog(parentComponent, title);
        setDialogDecorations(dialog, messageType);
        dialog.setVisible(true);

        return getResultedIndex(pane);
    }

    public static void showInternalMessageDialog(final Component parentComponent, final Object message) {
        showInternalMessageDialog(parentComponent, message, "Message", INFORMATION_MESSAGE, null);
    }

    public static void showInternalMessageDialog(final Component parentComponent, final Object message,
            final String title, final int messageType) {
        showInternalMessageDialog(parentComponent, message, title, messageType, null);
    }

    public static void showInternalMessageDialog(final Component parentComponent, final Object message,
            final String title, final int messageType,
            final Icon icon) {
        JOptionPane pane = new JOptionPane(message, messageType, DEFAULT_OPTION, icon);
        JDialog dialog = pane.createDialog(parentComponent, title);
        setDialogDecorations(dialog, messageType);
        JInternalFrame internalFrame = pane.createInternalFrame(parentComponent, title);
        showInternalFrameAndWaitTillClosed(internalFrame, pane, parentComponent);
    }

    public static int showInternalConfirmDialog(final Component parentComponent, final Object message) {
        return showInternalConfirmDialog(parentComponent, message, "Select an Option", YES_NO_CANCEL_OPTION,
                                        INFORMATION_MESSAGE, null);
    }

    public static int showInternalConfirmDialog(final Component parentComponent, final Object message,
            final String title, final int optionType) {
        return showInternalConfirmDialog(parentComponent, message, title, optionType,
                                        INFORMATION_MESSAGE, null);
    }

    public static int showInternalConfirmDialog(final Component parentComponent, final Object message,
            final String title, final int optionType,
            final int messageType) {
        return showInternalConfirmDialog(parentComponent, message, title, optionType,
                                        messageType, null);
    }

    public static int showInternalConfirmDialog(final Component parentComponent, final Object message,
            final String title, final int optionType,
            final int messageType, final Icon icon) {
        JOptionPane pane = new JOptionPane(message, messageType, optionType, icon);
        JDialog dialog = pane.createDialog(parentComponent, title);
        setDialogDecorations(dialog, messageType);
        JInternalFrame internalFrame = pane.createInternalFrame(parentComponent, title);
        showInternalFrameAndWaitTillClosed(internalFrame, pane, parentComponent);

        return getResultedIndex(pane);
    }

    public static int showInternalOptionDialog(final Component parentComponent, final Object message,
            final String title, final int optionType,
            final int messageType, final Icon icon,
            final Object[] options, final Object initialValue) {
        JOptionPane pane = new JOptionPane(message, messageType, optionType, icon, options, initialValue);
        JDialog dialog = pane.createDialog(parentComponent, title);
        setDialogDecorations(dialog, messageType);
        JInternalFrame internalFrame = pane.createInternalFrame(parentComponent, title);
        showInternalFrameAndWaitTillClosed(internalFrame, pane, parentComponent);

        return getResultedIndex(pane);
    }

    public static String showInternalInputDialog(final Component parentComponent, final Object message) {
        return (String)showInternalInputDialog(parentComponent, message, null, INFORMATION_MESSAGE, null, null, null);
    }

    public static String showInternalInputDialog(final Component parentComponent, final Object message,
            final String title, final int messageType) {
        return (String)showInternalInputDialog(parentComponent, message, title, messageType, null, null, null);
    }

    public static Object showInternalInputDialog(final Component parentComponent, final Object message,
            final String title, final int messageType,
            final Icon icon, final Object[] selectionValues,
            final Object initialSelectionValue) {
        JOptionPane pane = new JOptionPane(message, messageType, OK_CANCEL_OPTION, icon);
        pane.setSelectionValues(selectionValues);
        if (selectionValues != null) {
            pane.setInitialSelectionValue(initialSelectionValue);
        } else {
            pane.setWantsInput(true);
            pane.setInitialSelectionValue(initialSelectionValue);
        }
        JDialog dialog = pane.createDialog(parentComponent, title);
        setDialogDecorations(dialog, messageType);
        JInternalFrame internalFrame = pane.createInternalFrame(parentComponent, title);
        showInternalFrameAndWaitTillClosed(internalFrame, pane, parentComponent);

        return pane.getInputValue();
    }

    public JDialog createDialog(final Component parentComponent,
                                final String title) throws HeadlessException {

        Frame parentFrame = getFrameForComponent(parentComponent);
        JDialog dialog = new JDialog(parentFrame,
                                     (title != null) ? title : (String)getClientProperty("defaultTitle"),
                                     true);
        dialog.add(this);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parentFrame);
        addPropertyChangeListener(CLOSE_OWNER_PROPERTY_NAME, new CloseOwnerListener(dialog));

        return dialog;
    }

    public JInternalFrame createInternalFrame(final Component parentComponent, final String title) {
        JDesktopPane desktop = JOptionPane.getDesktopPaneForComponent(parentComponent);
        Container parent = (desktop != null) ? desktop : parentComponent.getParent();
        if (parent == null) {
            throw new RuntimeException(Messages.getString("swing.1E","JOptionPane")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        JInternalFrame frame = new JInternalFrame(title);
        frame.putClientProperty("JInternalFrame.optionDialog", Boolean.TRUE);
        parent.add(frame);
        frame.add(this);
        frame.pack();
        addPropertyChangeListener(CLOSE_OWNER_PROPERTY_NAME, new CloseOwnerListener(frame));

        return frame;
    }

    public static Frame getFrameForComponent(final Component parentComponent) throws HeadlessException {
        Frame result = null;
        if (parentComponent instanceof Frame) {
            return (Frame)parentComponent;
        }
        result = (Frame)SwingUtilities.getAncestorOfClass(Frame.class, parentComponent);
        return (result != null) ? result : getRootFrame();
    }

    public static JDesktopPane getDesktopPaneForComponent(final Component parentComponent) {
        return (JDesktopPane)SwingUtilities.getAncestorOfClass(JDesktopPane.class, parentComponent);
    }

    public static void setRootFrame(final Frame rootFrame) {
        JOptionPane.rootFrame = rootFrame;
    }

    public static Frame getRootFrame() throws HeadlessException {
        return (rootFrame != null) ? rootFrame : JFrame.getSharedOwner();
    }

    public void setUI(final OptionPaneUI ui) {
        super.setUI(ui);
    }

    public OptionPaneUI getUI() {
        return (OptionPaneUI)ui;
    }

    public void updateUI() {
        setUI((OptionPaneUI)UIManager.getUI(this));
    }

    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public void setMessage(final Object newMessage) {
        Object oldValue = message;
        message = newMessage;
        firePropertyChange(MESSAGE_PROPERTY, oldValue, newMessage);
    }

    public Object getMessage() {
        return message;
    }

    public void setIcon(final Icon newIcon) {
        Icon oldValue = icon;
        icon = newIcon;
        firePropertyChange(ICON_PROPERTY, oldValue, newIcon);
    }

    public Icon getIcon() {
        return icon;
    }

    public void setValue(final Object newValue) {
        Object oldValue = value;
        value = newValue;
        firePropertyChange(VALUE_PROPERTY, oldValue, newValue);
    }

    public Object getValue() {
        return value;
    }

    public void setOptions(final Object[] newOptions) {
        Object[] oldValue = options;
        options = newOptions;
        firePropertyChange(OPTIONS_PROPERTY, oldValue, newOptions);
    }

    public Object[] getOptions() {
        return ((options != null) ? ((Object[]) options.clone()) : null);
    }

    public void setInitialValue(final Object newValue) {
        Object oldValue = initialValue;
        initialValue = newValue;
        firePropertyChange(INITIAL_VALUE_PROPERTY, oldValue, newValue);
    }

    public Object getInitialValue() {
        return initialValue;
    }

    public void setMessageType(final int type) {
        switch (type) {
        case ERROR_MESSAGE:
        case INFORMATION_MESSAGE:
        case WARNING_MESSAGE:
        case QUESTION_MESSAGE:
        case PLAIN_MESSAGE:
            int oldValue = messageType;
            messageType = type;
            firePropertyChange(MESSAGE_TYPE_PROPERTY, oldValue, type);
            break;
        default:
            throw new RuntimeException(Messages.getString("swing.1F","JOptionPane") + //$NON-NLS-1$ //$NON-NLS-2$
                    "JOptionPane.ERROR_MESSAGE, JOptionPane.INFORMATION_MESSAGE, " + //$NON-NLS-1$
                    "JOptionPane.WARNING_MESSAGE, JOptionPane.QUESTION_MESSAGE " + //$NON-NLS-1$
                    "or JOptionPane.PLAIN_MESSAGE"); //$NON-NLS-1$
        }
    }

    public int getMessageType() {
        return messageType;
    }

    public void setOptionType(final int newType) {
        switch (newType) {
        case DEFAULT_OPTION:
        case YES_NO_OPTION:
        case YES_NO_CANCEL_OPTION:
        case OK_CANCEL_OPTION:
            int oldValue = optionType;
            optionType = newType;
            firePropertyChange(OPTION_TYPE_PROPERTY, oldValue, newType);
            break;
        default:
            throw new RuntimeException(Messages.getString("swing.20")); //$NON-NLS-1$
        }
    }

    public int getOptionType() {
        return optionType;
    }

    public void setSelectionValues(final Object[] newValues) {
        Object oldSelectionValues = selectionValues;
        selectionValues = newValues;
        firePropertyChange(SELECTION_VALUES_PROPERTY, oldSelectionValues, newValues);
        setWantsInput(selectionValues != null);
    }

    public Object[] getSelectionValues() {
        return selectionValues;
    }

    public void setInitialSelectionValue(final Object newValue) {
        Object oldInitialSelectionValue = initialSelectionValue;
        initialSelectionValue = newValue;
        firePropertyChange(INITIAL_SELECTION_VALUE_PROPERTY, oldInitialSelectionValue, newValue);
    }

    public Object getInitialSelectionValue() {
        return initialSelectionValue;
    }

    public void setInputValue(final Object newValue) {
        Object oldValue = inputValue;
        inputValue = newValue;
        firePropertyChange(INPUT_VALUE_PROPERTY, oldValue, newValue);
    }

    public Object getInputValue() {
        return inputValue;
    }

    public int getMaxCharactersPerLineCount() {
        return Integer.MAX_VALUE;
    }

    public void setWantsInput(final boolean newValue) {
        boolean oldValue = wantsInput;
        wantsInput = newValue;
        firePropertyChange(WANTS_INPUT_PROPERTY, oldValue, newValue);
    }

    public boolean getWantsInput() {
        return wantsInput;
    }

    public void selectInitialValue() {
        getUI().selectInitialValue(this);
    }

    public AccessibleContext getAccessibleContext() {
        return (accessibleContext == null) ? (accessibleContext = new AccessibleJOptionPane())
                : accessibleContext;
   }

    private static int getResultedIndex(final JOptionPane pane) {
        Object value = pane.getValue();
        if (value instanceof Integer) {
            return ((Integer)value).intValue();
        }

        Object[] options = pane.getOptions();
        if (Utilities.isEmptyArray(options)) {
            return -1;
        }

        for (int i = 0; i < options.length; i++) {
            if (options[i] == value) {
                return i;
            }
        }

        return -1;
    }
    
    private static int messageTypeToRootPaneDecoration(final int messageType) {
        switch (messageType) {
        case ERROR_MESSAGE:
            return JRootPane.ERROR_DIALOG;
        case INFORMATION_MESSAGE:
            return JRootPane.INFORMATION_DIALOG;
        case QUESTION_MESSAGE:
            return JRootPane.QUESTION_DIALOG;
        case WARNING_MESSAGE:
            return JRootPane.WARNING_DIALOG;
        default:
            return JRootPane.PLAIN_DIALOG;
        }
    }
    
    private static void setDialogDecorations(final JDialog dialog, final int messageType) {
        if (!JDialog.isDefaultLookAndFeelDecorated()) {
            return;
        }
        dialog.getRootPane().setWindowDecorationStyle(messageTypeToRootPaneDecoration(messageType));
    }
}
