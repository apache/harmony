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
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleText;
import javax.swing.plaf.LabelUI;
import javax.swing.text.AttributeSet;
import org.apache.harmony.luni.util.NotImplementedException;
import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>JLabel</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JLabel extends JComponent implements SwingConstants, Accessible {
    private static final long serialVersionUID = 1992522068352176692L;

    protected Component labelFor;

    private String text;

    private Icon icon;

    private Icon disabledIcon;

    private Icon defaultDisabledIcon;

    private int horizontalAlignment;

    private int horizontalTextPosition = TRAILING;

    private int verticalAlignment = CENTER;

    private int verticalTextPosition = CENTER;

    private int iconTextGap = 4;

    private int displayedMnemonicIndex = -1;

    private int displayedMnemonic;

    private AbstractAction labelForAction;

    private static final String UI_CLASS_ID = "LabelUI";

    private static final String DISPLAYED_MNEMONIC_CHANGED_PROPERTY = "displayedMnemonic";

    private static final String LABEL_FOR_CHANGED_PROPERTY = "labelFor";

    //TODO: implement
    protected class AccessibleJLabel extends AccessibleJComponent implements AccessibleText {
        private static final long serialVersionUID = -3843546598023238741L;

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.LABEL;
        }

        public String getAfterIndex(final int part, final int index)
                throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getAtIndex(final int part, final int index)
                throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getBeforeIndex(final int part, final int index)
                throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getCaretPosition() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public AttributeSet getCharacterAttribute(final int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Rectangle getCharacterBounds(final int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getCharCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getIndexAtPoint(final Point p) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getSelectedText() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getSelectionEnd() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getSelectionStart() throws NotImplementedException {
            throw new NotImplementedException();
        }
    }

    public JLabel() {
        this("");
    }

    public JLabel(final Icon icon) {
        this(icon, CENTER);
    }

    public JLabel(final Icon icon, final int horizontalAlignment) {
        this(null, icon, horizontalAlignment);
    }

    public JLabel(final String text) {
        this(text, LEADING);
    }

    public JLabel(final String text, final int horizontalAlignment) {
        this(text, null, horizontalAlignment);
    }

    public JLabel(final String text, final Icon icon, final int horizontalAlignment) {
        checkHorizontalAligment(horizontalAlignment);
        this.text = text;
        this.icon = icon;
        this.horizontalAlignment = horizontalAlignment;
        updateUI();
    }

    public void setUI(final LabelUI ui) {
        super.setUI(ui);
    }

    public LabelUI getUI() {
        return (LabelUI) ui;
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    @Override
    public void updateUI() {
        setUI((LabelUI) UIManager.getUI(this));
    }

    public void setIcon(final Icon icon) {
        Icon oldValue = this.icon;
        this.icon = icon;
        this.defaultDisabledIcon = null;
        firePropertyChange(AbstractButton.ICON_CHANGED_PROPERTY, oldValue, icon);
    }

    public Icon getIcon() {
        return icon;
    }

    public void setDisabledIcon(final Icon icon) {
        Icon oldValue = this.disabledIcon;
        this.disabledIcon = icon;
        firePropertyChange(AbstractButton.DISABLED_ICON_CHANGED_PROPERTY, oldValue, icon);
    }

    public Icon getDisabledIcon() {
        if (disabledIcon != null) {
            return disabledIcon;
        }
        if (defaultDisabledIcon == null && icon instanceof ImageIcon) {
            defaultDisabledIcon = new ImageIcon(GrayFilter
                    .createDisabledImage(((ImageIcon) icon).getImage()));
        }
        return defaultDisabledIcon;
    }

    public void setText(final String text) {
        String oldValue = this.text;
        this.text = text;
        firePropertyChange(AbstractButton.TEXT_CHANGED_PROPERTY, oldValue, text);
        setDisplayedMnemonicIndex(Utilities.getDisplayedMnemonicIndex(text, Utilities
                .keyCodeToKeyChar(getDisplayedMnemonic())));
    }

    public String getText() {
        return text;
    }

    public void setLabelFor(final Component c) {
        Component oldValue = this.labelFor;
        this.labelFor = c;
        firePropertyChange(LABEL_FOR_CHANGED_PROPERTY, oldValue, c);
    }

    public Component getLabelFor() {
        return labelFor;
    }

    public void setIconTextGap(final int iconTextGap) {
        LookAndFeel.markPropertyNotInstallable(this,
                StringConstants.ICON_TEXT_GAP_PROPERTY_CHANGED);
        int oldValue = this.iconTextGap;
        this.iconTextGap = iconTextGap;
        firePropertyChange(StringConstants.ICON_TEXT_GAP_PROPERTY_CHANGED, oldValue,
                iconTextGap);
    }

    public int getIconTextGap() {
        return iconTextGap;
    }

    public void setVerticalTextPosition(final int textPosition) {
        checkVerticalKey(textPosition, "incorrect vertical text position specified");
        int oldValue = this.verticalTextPosition;
        this.verticalTextPosition = textPosition;
        firePropertyChange(AbstractButton.VERTICAL_TEXT_POSITION_CHANGED_PROPERTY, oldValue,
                textPosition);
    }

    public int getVerticalTextPosition() {
        return verticalTextPosition;
    }

    public void setVerticalAlignment(final int alignment) {
        checkVerticalKey(alignment, "incorrect vertical aligment specified");
        int oldValue = this.verticalAlignment;
        this.verticalAlignment = alignment;
        firePropertyChange(AbstractButton.VERTICAL_ALIGNMENT_CHANGED_PROPERTY, oldValue,
                alignment);
    }

    public int getVerticalAlignment() {
        return verticalAlignment;
    }

    public void setHorizontalTextPosition(final int textPosition) {
        checkHorizontalKey(textPosition, "incorrect horizontal text position specified");
        int oldValue = this.horizontalTextPosition;
        this.horizontalTextPosition = textPosition;
        firePropertyChange(AbstractButton.HORIZONTAL_TEXT_POSITION_CHANGED_PROPERTY, oldValue,
                textPosition);
    }

    public int getHorizontalTextPosition() {
        return horizontalTextPosition;
    }

    public void setHorizontalAlignment(final int alignment) {
        checkHorizontalAligment(alignment);
        int oldValue = this.horizontalAlignment;
        this.horizontalAlignment = alignment;
        firePropertyChange(AbstractButton.HORIZONTAL_ALIGNMENT_CHANGED_PROPERTY, oldValue,
                alignment);
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setDisplayedMnemonicIndex(final int index) {
        if (index < -1 || index >= 0 && (text == null || index >= text.length())) {
            throw new IllegalArgumentException(Messages.getString("swing.14")); //$NON-NLS-1$
        }
        int oldValue = this.displayedMnemonicIndex;
        this.displayedMnemonicIndex = index;
        firePropertyChange(StringConstants.MNEMONIC_INDEX_PROPERTY_CHANGED, oldValue, index);
    }

    public int getDisplayedMnemonicIndex() {
        return displayedMnemonicIndex;
    }

    public void setDisplayedMnemonic(final int key) {
        setDisplayedMnemonic(key, Utilities.keyCodeToKeyChar(key));
    }

    public void setDisplayedMnemonic(final char keyChar) {
        setDisplayedMnemonic(Utilities.keyCharToKeyCode(keyChar), keyChar);
    }

    public int getDisplayedMnemonic() {
        return displayedMnemonic;
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJLabel();
        }
        return accessibleContext;
    }

    @Override
    public boolean imageUpdate(final Image image, final int infoflags, final int x,
            final int y, final int w, final int h) {
        Icon currentIcon = isEnabled() ? getIcon() : getDisabledIcon();
        if (image != null && (currentIcon instanceof ImageIcon)
                && !image.equals(((ImageIcon) currentIcon).getImage())) {
            return false;
        }
        return super.imageUpdate(image, infoflags, x, y, w, h);
    }

    protected int checkVerticalKey(final int key, final String message) {
        return Utilities.checkVerticalKey(key, message);
    }

    protected int checkHorizontalKey(final int key, final String message) {
        return Utilities.checkHorizontalKey(key, message);
    }

    private void setDisplayedMnemonic(final int keyCode, final char keyChar) {
        int oldValue = this.displayedMnemonic;
        if (oldValue != keyCode) {
            if (displayedMnemonic != 0) {
                getInputMap(WHEN_IN_FOCUSED_WINDOW, true).remove(
                        KeyStroke.getKeyStroke(displayedMnemonic, InputEvent.ALT_DOWN_MASK));
            }
            this.displayedMnemonic = keyCode;
            String mnemonic = "mnemonic";
            getInputMap(WHEN_IN_FOCUSED_WINDOW, true).put(
                    KeyStroke.getKeyStroke(keyCode, InputEvent.ALT_DOWN_MASK), mnemonic);
            getActionMap(true).put(mnemonic, getLabelForAction());
            firePropertyChange(DISPLAYED_MNEMONIC_CHANGED_PROPERTY, oldValue, keyCode);
            setDisplayedMnemonicIndex(Utilities.getDisplayedMnemonicIndex(text, keyChar));
        }
    }

    private void checkHorizontalAligment(final int alignment) {
        checkHorizontalKey(alignment, "Incorrect horizontal alignment is specified");
    }

    private AbstractAction getLabelForAction() {
        if (labelForAction == null) {
            labelForAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(final ActionEvent e) {
                    if (isVisible() && isEnabled() && labelFor != null && labelFor.isVisible()
                            && labelFor.isEnabled()) {
                        labelFor.requestFocus();
                    }
                }
            };
        }
        return labelForAction;
    }
}
