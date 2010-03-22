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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.OptionPaneUI;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class BasicOptionPaneUI extends OptionPaneUI {

    public static class ButtonAreaLayout implements LayoutManager {
        protected boolean syncAllWidths;
        protected int padding;
        protected boolean centersChildren = true;

        public ButtonAreaLayout(final boolean syncAllWidths, final int padding) {
            this.syncAllWidths = syncAllWidths;
            this.padding = padding;
        }

        public void setSyncAllWidths(final boolean syncAllWidths) {
            this.syncAllWidths = syncAllWidths;
        }

        public boolean getSyncAllWidths() {
            return syncAllWidths;
        }

        public void setPadding(final int padding) {
            this.padding = padding;
        }

        public int getPadding() {
            return padding;
        }

        public void setCentersChildren(final boolean centersChildren) {
            this.centersChildren = centersChildren;
        }

        public boolean getCentersChildren() {
            return centersChildren;
        }

        public void layoutContainer(final Container parent) {
            Insets insets = parent.getInsets();
            int childX = insets.left;
            int childY = insets.top;

            int numChildren = parent.getComponentCount();
            int maxChildWidth = getMaxChildWidth(parent);
            int extraWidthSpace = parent.getSize().width - preferredLayoutSize(parent).width;
            int realPadding = padding;
            if (extraWidthSpace > 0) {
                if (centersChildren) {
                    childX += extraWidthSpace/2;
                } else {
                    realPadding = (numChildren > 0) ? extraWidthSpace/(numChildren - 1) : 0;
                }
            }
            boolean isLTR = parent.getComponentOrientation().isLeftToRight();
            for (int i = 0; i < numChildren; i++) {
                Component child = parent.getComponent(isLTR ? i : numChildren - 1 - i);
                Dimension prefSize = child.getPreferredSize();
                int childWidth = syncAllWidths ? maxChildWidth : prefSize.width;
                int childHeight = prefSize.height;

                child.setLocation(childX, childY);
                child.setSize(childWidth, childHeight);
                childX += realPadding + childWidth;
            }
        }

        public Dimension minimumLayoutSize(final Container parent) {
            return preferredLayoutSize(parent);
        }

        private int getMaxChildWidth(final Container parent) {
            int result = 0;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Dimension prefSize = parent.getComponent(i).getPreferredSize();
                if (prefSize.width > result) {
                    result = prefSize.width;
                }
            }
            return (result > 0) ? result : 10;
        }

        private int getMaxChildHeight(final Container parent) {
            int result = 0;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Dimension prefSize = parent.getComponent(i).getPreferredSize();
                if (prefSize.height > result) {
                    result = prefSize.height;
                }
            }
            return (result > 0) ? result : 10;
        }

        public Dimension preferredLayoutSize(final Container parent) {
            if (parent == null) {
                return new Dimension();
            }

            int totalWidth = 0;
            int totalHeight = getMaxChildHeight(parent);
            int numChildren = parent.getComponentCount();
            if (syncAllWidths) {
                int maxChildWidth = getMaxChildWidth(parent);
                totalWidth = (maxChildWidth + padding)*numChildren - padding;
            } else {
                for (int i = 0; i < numChildren; i++) {
                    Dimension prefSize = parent.getComponent(i).getPreferredSize();
                    totalWidth += (prefSize.width > 0) ? prefSize.width : 10;
                }
                totalWidth += padding*(numChildren - 1);
            }

            return Utilities.addInsets(new Dimension(totalWidth, totalHeight),
                                       parent.getInsets());
        }

        public void addLayoutComponent(final String name, final Component c) {
        }

        public void removeLayoutComponent(final Component c) {
        }

    };

    public class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent event) {
            if (event.getSource() != BasicOptionPaneUI.this.optionPane) {
                return;
            }

            final JOptionPane pane = BasicOptionPaneUI.this.optionPane;
            final String propName = event.getPropertyName();
            
            if (JOptionPane.ICON_PROPERTY.equals(propName) ||
                JOptionPane.MESSAGE_PROPERTY.equals(propName) ||
                JOptionPane.OPTIONS_PROPERTY.equals(propName) ||
                JOptionPane.INITIAL_VALUE_PROPERTY.equals(propName)) {

                uninstallComponents();
                installComponents();
                pane.revalidate();
            } else if (JOptionPane.SELECTION_VALUES_PROPERTY.equals(propName) ||
                       JOptionPane.WANTS_INPUT_PROPERTY.equals(propName)) {

                initValues(pane);
                pane.revalidate();
            } else if (JOptionPane.INITIAL_SELECTION_VALUE_PROPERTY.equals(propName)) {
                setInputValue(event.getNewValue());
            } else if (StringConstants.ANCESTOR_PROPERTY_NAME.equals(propName)
                    && (event.getOldValue() == null)) {

                selectInitialValue(pane);
                String soundEventName = getSoundEffectName();
                ((BasicLookAndFeel)UIManager.getLookAndFeel())
                .fireSoundAction(pane, PROPERTY_PREFIX + soundEventName);
            }
        }

        private void initValues(final JOptionPane pane) {
            uninstallComponents();
            if (pane.getWantsInput()) {
                final Object[] selectionValues = pane.getSelectionValues();
                final Object initialValue = pane.getInitialSelectionValue();
                inputComponent = (selectionValues == null)
                        ? createTextField(pane, initialValue)
                        : createComboBox(selectionValues, initialValue);
            } else {
                inputComponent = null;
            }
            installComponents();
        }

        private JComponent createComboBox(final Object[] selectionValues, final Object initialValue) {
            JComboBox comboBox = new JComboBox(selectionValues);
            comboBox.setSelectedItem(initialValue);
            return comboBox;
        }

        private JComponent createTextField(final JOptionPane pane, Object initialValue) {
            final JTextField result = new JTextField();
            result.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    pane.setInputValue(result.getText());
                    if (initialFocusComponent instanceof AbstractButton) {
                        ((AbstractButton)initialFocusComponent).doClick();
                    }
                }
            });
            setTextFieldValue(result, initialValue);
            return result;
        }
    }

    public class ButtonActionListener implements ActionListener {
        protected int buttonIndex;

        public ButtonActionListener(final int buttonIndex) {
            this.buttonIndex = buttonIndex;
        }

        public void actionPerformed(final ActionEvent e) {
            JOptionPane pane = BasicOptionPaneUI.this.optionPane;
            Object[] options = pane.getOptions();
            if (!Utilities.isEmptyArray(options)) {
                pane.setValue(options[buttonIndex]);
            } else {
                pane.setValue(new Integer(buttonIndex));
            }

            Object[] buttons = getButtons();
            assert !Utilities.isEmptyArray(buttons) && buttonIndex < buttons.length;
            if (cancelButton.text != null && !cancelButton.text.equals(buttons[buttonIndex])) {
                ((BasicOptionPaneUI)pane.getUI()).resetInputValue();
            }
            CloseAction.doClose(pane);
        }
    };

    private static class ButtonInfo {
        public int mnemonic;
        public String text;

        public String toString() {
            return text;
        }
    }

    private static class CloseAction extends AbstractAction {
        public static void doClose(final JOptionPane pane) {
            if (JOptionPane.UNINITIALIZED_VALUE.equals(pane.getInputValue())) {
                pane.setInputValue(null);
            }
            pane.putClientProperty("closeOwner", Boolean.TRUE);
        }

        public void actionPerformed(final ActionEvent e) {
            doClose((JOptionPane)e.getSource());
        }
    };

    private static class WindowFocusGainedListener implements WindowFocusListener {
        private final JComponent focusTarget;

        public WindowFocusGainedListener(final JComponent focusTarget) {
            this.focusTarget = focusTarget;
        }

        public void windowGainedFocus(final WindowEvent e) {
            focusTarget.requestFocusInWindow();
            e.getWindow().removeWindowFocusListener(this);
        }

        public void windowLostFocus(final WindowEvent e) {
        }
    }

    public static final int MinimumWidth = 262;
    public static final int MinimumHeight = 90;

    protected JOptionPane optionPane;
    protected Dimension minimumSize;
    protected JComponent inputComponent;
    protected Component initialFocusComponent;
    protected boolean hasCustomComponents;
    protected PropertyChangeListener propertyChangeListener;

    private static final int NUM_DEFAULT_MESSAGES = 4;

    private static final int DEFAULT_BUTTON_PADDING = 6;
    private static final String PROPERTY_PREFIX = "OptionPane.";
    private Icon[] defaultIcons;
    private static final CloseAction closeAction = new CloseAction();

    private String inputTitleText;
    private String messageTitleText;
    private String defaultTitleText;

    private final ButtonInfo yesButton = new ButtonInfo();
    private final ButtonInfo noButton = new ButtonInfo();
    private final ButtonInfo okButton = new ButtonInfo();
    private final ButtonInfo cancelButton = new ButtonInfo();

    private int buttonClickThreshhold;

    private Border messageAreaBorder;
    private Color messageForeground;
    private Border buttonAreaBorder;
    
    public static ComponentUI createUI(final JComponent c) {
        return new BasicOptionPaneUI();
    }

    public void installUI(final JComponent c) {
        optionPane = (JOptionPane)c;
        installDefaults();
        optionPane.setLayout(createLayoutManager());
        installComponents();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(final JComponent c) {
        uninstallKeyboardActions();
        uninstallListeners();
        uninstallComponents();
        uninstallDefaults();
        optionPane = null;
    }

    public Dimension getMinimumOptionPaneSize() {
        return (minimumSize != null) ? minimumSize : new Dimension(MinimumWidth, MinimumHeight);
    }

    public Dimension getPreferredSize(final JComponent c) {
        if (c == null) {
            return null;
        }

        Dimension layoutSize = (c.getLayout() != null) ? c.getLayout().preferredLayoutSize(c) : null;
        Dimension minimumSize = getMinimumOptionPaneSize();
        if (layoutSize == null) {
            return minimumSize;
        }
        return  new Dimension(Math.max(layoutSize.width, minimumSize.width),
                              Math.max(layoutSize.height, minimumSize.height));
    }

    public void selectInitialValue(final JOptionPane op) {
        JRootPane rootPane = op.getRootPane();
        if (rootPane != null && initialFocusComponent instanceof JButton) {
            rootPane.setDefaultButton((JButton)initialFocusComponent);
        }

        JComponent focusOwner = (inputComponent != null) ? inputComponent
                : ((initialFocusComponent instanceof JComponent)
                        ? (JComponent)initialFocusComponent : null);
        if (focusOwner != null) {
            Window w = SwingUtilities.getWindowAncestor(focusOwner);
            if (w != null) {
                w.addWindowFocusListener(new WindowFocusGainedListener(focusOwner));
            }
        }
    }

    public boolean containsCustomComponents(final JOptionPane op) {
        return hasCustomComponents;
    }

    protected void installDefaults() {
        LookAndFeel.installColorsAndFont(optionPane, PROPERTY_PREFIX + "background",
                                         PROPERTY_PREFIX + "foreground", PROPERTY_PREFIX + "font");
        LookAndFeel.installBorder(optionPane, PROPERTY_PREFIX + "border");
        minimumSize = UIManager.getDimension(PROPERTY_PREFIX + "minimumSize");

        defaultTitleText = UIManager.getString(PROPERTY_PREFIX + "titleText");
        messageTitleText = UIManager.getString(PROPERTY_PREFIX + "messageDialogTitle");
        inputTitleText = UIManager.getString(PROPERTY_PREFIX + "inputDialogTitle");

        okButton.text = UIManager.getString(PROPERTY_PREFIX + "okButtonText");
        yesButton.text = UIManager.getString(PROPERTY_PREFIX + "yesButtonText");
        noButton.text = UIManager.getString(PROPERTY_PREFIX + "noButtonText");
        cancelButton.text = UIManager.getString(PROPERTY_PREFIX + "cancelButtonText");

        okButton.mnemonic = UIManager.getInt(PROPERTY_PREFIX + "okButtonMnemonic");
        yesButton.mnemonic = UIManager.getInt(PROPERTY_PREFIX + "yesButtonMnemonic");
        noButton.mnemonic = UIManager.getInt(PROPERTY_PREFIX + "noButtonMnemonic");
        cancelButton.mnemonic = UIManager.getInt(PROPERTY_PREFIX + "cancelButtonMnemonic");

        buttonClickThreshhold = UIManager.getInt(PROPERTY_PREFIX + "buttonClickThreshhold");

        messageAreaBorder = UIManager.getBorder(PROPERTY_PREFIX + "messageAreaBorder");
        messageForeground = UIManager.getColor(PROPERTY_PREFIX + "messageForeground");
        buttonAreaBorder = UIManager.getBorder(PROPERTY_PREFIX + "buttonAreaBorder");
    }

    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(optionPane);
    }

    protected void installComponents() {
        installTitle();
        optionPane.add(createMessageArea());
        Container separator = createSeparator();
        if (separator != null) {
            optionPane.add(separator);
        }
        optionPane.add(createButtonArea());
        optionPane.setMinimumSize(getMinimumOptionPaneSize());
        if (!optionPane.getWantsInput()){
            inputComponent = null;
        }
        if (!optionPane.getComponentOrientation().isLeftToRight()) {
            optionPane.applyComponentOrientation(optionPane.getComponentOrientation());
        }
    }

    protected void uninstallComponents() {
        optionPane.removeAll();
        inputComponent = null;
    }

    protected LayoutManager createLayoutManager() {
        return new BoxLayout(optionPane, BoxLayout.Y_AXIS);
    }

    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        optionPane.addPropertyChangeListener(propertyChangeListener);
    }

    protected void uninstallListeners() {
        if (optionPane != null) {
            optionPane.removePropertyChangeListener(propertyChangeListener);
        }
        propertyChangeListener = null;
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    protected void installKeyboardActions() {
        SwingUtilities.replaceUIInputMap(optionPane, JComponent.WHEN_IN_FOCUSED_WINDOW,
                       LookAndFeel.makeComponentInputMap(optionPane, (Object[])UIManager.get(PROPERTY_PREFIX + "windowBindings")));

        ActionMap actionMap = new ActionMapUIResource();
        actionMap.put(StringConstants.CLOSE_ACTION, closeAction);
        actionMap.setParent(((BasicLookAndFeel)UIManager.getLookAndFeel())
                            .getAudioActionMap());
        SwingUtilities.replaceUIActionMap(optionPane, actionMap);
    }

    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIInputMap(optionPane, JComponent.WHEN_IN_FOCUSED_WINDOW, null);
        SwingUtilities.replaceUIActionMap(optionPane, null);
    }

    protected Container createMessageArea() {
        Object message = getMessage();
        JComponent messageArea = new JPanel(new BorderLayout());
        messageArea.setBorder(messageAreaBorder);
        JPanel messageAndGapPanel = new JPanel();
        messageAndGapPanel.setLayout(new BoxLayout(messageAndGapPanel, BoxLayout.X_AXIS));
        messageArea.add(messageAndGapPanel, BorderLayout.CENTER);

        if (getIcon() != null) {
            JPanel gapPanel = new JPanel();
            gapPanel.setPreferredSize(new Dimension(15, 1));
            messageAndGapPanel.add(gapPanel);
        }
        JPanel plainTextPanel = new JPanel(new GridBagLayout());
        messageAndGapPanel.add(plainTextPanel);

        hasCustomComponents |= (message instanceof Component);
        GridBagConstraints constrains = createConstrains();
        addMessageComponents(plainTextPanel, constrains, message, getMaxCharactersPerLineCount(), true);

        if (inputComponent != null) {
            addMessageComponents(plainTextPanel, constrains, inputComponent, getMaxCharactersPerLineCount(), true);
        }

        addIcon(messageArea);

        return messageArea;
    }

    protected void addMessageComponents(final Container container,
                                        final GridBagConstraints cons,
                                        final Object msg, final int maxll,
                                        final boolean internallyCreated) {

        if (msg == null) {
            return;
        }

        if (msg instanceof Component) {
            addToContainer(container, cons, (Component)msg);
        } else if (msg instanceof Icon) {
            addMessageComponents(container, cons, new JLabel((Icon)msg), maxll, true);
        } else if (msg instanceof Object[]) {
            final Object[] array = (Object[])msg;
            for (int i = 0; i < array.length; i++) {
                addMessageComponents(container, cons, array[i], maxll, internallyCreated);
            }
        } else {
            addMessageComponents(container, cons, msg.toString(), maxll, internallyCreated);
        }
    }

    protected Object getMessage() {
        return (optionPane != null) ? optionPane.getMessage() : null;
    }

    protected void addIcon(final Container top) {
        final JLabel label = new JLabel(getIcon());
        label.setVerticalAlignment(SwingConstants.TOP);
        top.add(label, BorderLayout.LINE_START);
    }

    protected Icon getIcon() {
        if (optionPane == null) {
            return null;
        }

        Icon icon = optionPane.getIcon();
        return (icon != null) ? icon : getIconForType(optionPane.getMessageType());
    }

    protected Icon getIconForType(final int messageType) {
        if (optionPane == null) {
            throw new NullPointerException(Messages.getString("swing.03","optionPane")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (defaultIcons == null) {
            installIcons();
        }

        if (messageType < 0 || NUM_DEFAULT_MESSAGES <= messageType) {
            return null;
        }

        return defaultIcons[messageType];
    }

    protected int getMaxCharactersPerLineCount() {
        return optionPane.getMaxCharactersPerLineCount();
    }

    protected void burstStringInto(final Container c, final String d,
                                   final int maxll) {
        String str = d;
        int firstSpace;
        GridBagConstraints constrains = createConstrains();
        while(str.length() > maxll && (firstSpace = findSpaceToCutAt(str, maxll)) > 0) {
            addLabel(c, str.substring(0, firstSpace), constrains);
            str = str.substring(firstSpace);
            str = trimFristSpace(str);
        }
        if (str.length() > 0) {
            addLabel(c, str, constrains);
        }
    }

    protected Container createSeparator() {
        return null;
    }

    protected Container createButtonArea() {
        JPanel buttonArea = new JPanel();
        buttonArea.setLayout(new ButtonAreaLayout(getSizeButtonsToSameWidth(), DEFAULT_BUTTON_PADDING));
        buttonArea.setBorder(buttonAreaBorder);
        addButtonComponents(buttonArea, getButtons(), getInitialValueIndex());

        return buttonArea;
    }

    protected void addButtonComponents(final Container container,
                                       final Object[] buttons,
                                       final int initialIndex) {

        if (Utilities.isEmptyArray(buttons)){
            return;
        }

        int horMargin = getHorizontalButtonMarginSize(buttons.length);
        for (int iButton = 0; iButton < buttons.length; iButton++) {
            Object curButton = buttons[iButton];
            if (curButton instanceof Component) {
                hasCustomComponents = true;
            } else {
                curButton = createButton(curButton, iButton, horMargin, buttonClickThreshhold);
            }
            container.add((Component)curButton);
            if (iButton == initialIndex) {
                initialFocusComponent = (Component)curButton;
            }
        }
    }

    protected ActionListener createButtonActionListener(final int buttonIndex) {
        return new ButtonActionListener(buttonIndex);
    }

    protected Object[] getButtons() {
        if (optionPane == null) {
            return null;
        }

        Object[] result = optionPane.getOptions();
        if (!Utilities.isEmptyArray(result)) {
            return result;
        }
        switch (optionPane.getOptionType()) {
        case JOptionPane.DEFAULT_OPTION:
            result = new Object[] {okButton};
            break;
        case JOptionPane.YES_NO_OPTION:
            result = new Object[] {yesButton, noButton};
            break;
        case JOptionPane.YES_NO_CANCEL_OPTION:
            result = new Object[] {yesButton, noButton, cancelButton};
            break;
        case JOptionPane.OK_CANCEL_OPTION:
            result = new Object[] {okButton, cancelButton};
            break;
        default:
            assert false : "illegal option";
        }

        return result;
    }

    protected boolean getSizeButtonsToSameWidth() {
        return true;
    }

    protected int getInitialValueIndex() {
        if (optionPane == null) {
            return -1;
        }

        Object[] options = optionPane.getOptions();
        if (Utilities.isEmptyArray(options)) {
            return 0;
        }

        Object value = optionPane.getInitialValue();
        if (value == null) {
            return -1;
        }

        for (int i = options.length - 1; i >= 0; i--) {
            if (value.equals(options[i])) {
                return i;
            }
        }

        return -1;
    }

    protected void resetInputValue() {
        optionPane.setInputValue(getInputValue());
    }

    private Object getInputValue() {
        if (inputComponent == null) {
            return null;
        }
        
        if (inputComponent instanceof JTextField) {
            return ((JTextField)inputComponent).getText();
        } else if (inputComponent instanceof JComboBox) {
            return ((JComboBox)inputComponent).getSelectedItem();
        }
        
        return null;
    }

    private void setInputValue(final Object value) {
        if (inputComponent == null) {
            return;
        }
        
        if (inputComponent instanceof JTextField) {
            setTextFieldValue((JTextField)inputComponent, value);
        } else if (inputComponent instanceof JComboBox) {
            ((JComboBox)inputComponent).setSelectedItem(value);
        }
    }

    private void setTextFieldValue(final JTextField textField, final Object value) {
        String text = (value != null) ? value.toString() : null;
        textField.setText(text);
        if (text != null) {
            textField.setSelectionStart(0);
            textField.setSelectionEnd(text.length());
        }
    }

    private void installTitle() {
        String title = null;
        switch (optionPane.getOptionType()) {
        case JOptionPane.DEFAULT_OPTION:
            title = messageTitleText;
            break;
        case JOptionPane.YES_NO_OPTION:
            title = defaultTitleText;
            break;
        case JOptionPane.YES_NO_CANCEL_OPTION:
            title = defaultTitleText;
            break;
        case JOptionPane.OK_CANCEL_OPTION:
            title = inputTitleText;
            break;
        default:
            assert false : "illegal option";
        }
        optionPane.putClientProperty("defaultTitle", title);
    }

    private String getSoundEffectName() {
        switch (optionPane.getMessageType()) {
        case JOptionPane.ERROR_MESSAGE:
            return "errorSound";
        case JOptionPane.INFORMATION_MESSAGE:
            return "informationSound";
        case JOptionPane.WARNING_MESSAGE:
            return "warningSound";
        case JOptionPane.QUESTION_MESSAGE:
            return "questionSound";
        }

        return null;
    }

    private void installIcons() {
        if (defaultIcons == null) {
            defaultIcons = new Icon[NUM_DEFAULT_MESSAGES];
        }

        defaultIcons[JOptionPane.ERROR_MESSAGE] =
            UIManager.getIcon(PROPERTY_PREFIX + "errorIcon");
        defaultIcons[JOptionPane.INFORMATION_MESSAGE] =
            UIManager.getIcon(PROPERTY_PREFIX + "informationIcon");
        defaultIcons[JOptionPane.QUESTION_MESSAGE] =
            UIManager.getIcon(PROPERTY_PREFIX + "questionIcon");
        defaultIcons[JOptionPane.WARNING_MESSAGE] =
            UIManager.getIcon(PROPERTY_PREFIX + "warningIcon");
    }

    private void addLabel(final Container c, final String str, final GridBagConstraints cons) {
        final JLabel label = new JLabel(str);
        label.setForeground(messageForeground);
        label.setAlignmentX(0);
        addToContainer(c, cons, label);
    }

    private String trimFristSpace(final String str) {
        return (str.indexOf(' ') == 0) ? str.substring(1) : str;
    }

    private int findSpaceToCutAt(final String str, final int maxCharNum) {
        int prevIndex = str.indexOf(' ');
        int newIndex = prevIndex;
        while (newIndex <= maxCharNum && newIndex < str.length() && newIndex != -1) {
            prevIndex = newIndex;
            newIndex = str.indexOf(' ', prevIndex + 1);
        }

        return prevIndex;
    }

    private int getHorizontalButtonMarginSize(final int numButtons) {
        return (numButtons <= 2) ? 8 : ((numButtons == 3) ? 4 : 14);
    }

    private JButton createButton(final Object curButton,
                                 final int iButton, final int horMargin,
                                 final int threshold) {
        JButton result;
        int mnemonic = 0;
        if (curButton instanceof Icon) {
            result = new JButton((Icon)curButton);
        } else if (curButton instanceof ButtonInfo){
            result = new JButton(((ButtonInfo)curButton).text);
            mnemonic = ((ButtonInfo)curButton).mnemonic;
        } else {
            result = new JButton(curButton.toString());
        }
        result.setMultiClickThreshhold(threshold);
        result.addActionListener(createButtonActionListener(iButton));
        result.setMnemonic(mnemonic);
        result.setMargin(new Insets(2, horMargin, 2, horMargin));
        return result;
    }

    private void addMessageComponents(final Container container,
                                      final GridBagConstraints cons,
                                      final String str, final int maxll,
                                      final boolean internallyCreated) {
      if (Utilities.isEmptyString(str)) {
          return;
      }

      final int strLength = str.length();
      final int strDelimIndex = str.indexOf("\n");
      if (strDelimIndex < 0) {
          if (strLength <= maxll) {
              addLabel(container, str, cons);
          } else {
              final Box box = new Box(BoxLayout.Y_AXIS);
              box.setAlignmentX(0);
              burstStringInto(box, str, maxll);
              addMessageComponents(container, cons, box, maxll, true);
          }
      } else {
          final boolean leadingDelimiter = strDelimIndex == 0;
          final Object leadingMsg = leadingDelimiter ? (Object)createStringDelimiter() : str.substring(0, strDelimIndex);
          addMessageComponents(container, cons, leadingMsg, maxll, leadingDelimiter);
          addMessageComponents(container, cons, str.substring(strDelimIndex + 1, strLength), maxll, internallyCreated);
      }
  }

    private void addToContainer(final Container container,
                                final GridBagConstraints constrains,
                                final Component child) {

        constrains.gridy++;
        container.add(child, constrains);
    }

    private Component createStringDelimiter() {
        return new Component() {
            public Dimension getPreferredSize() {
                return new Dimension(1, Utilities.getFontMetrics(optionPane).getHeight());
            }
        };
    }

    private GridBagConstraints createConstrains() {
        return new GridBagConstraints(0, 0,
                                      GridBagConstraints.NONE,
                                      GridBagConstraints.BOTH,
                                      1.0, 0,
                                      GridBagConstraints.LINE_START,
                                      GridBagConstraints.HORIZONTAL,
                                      new Insets(0, 0, 0, 0), 0, 0);

    }
}
