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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.EventListener;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.ComponentUI;

import org.apache.harmony.x.swing.ButtonCommons;
import org.apache.harmony.x.swing.Utilities;


public class BasicButtonUI extends ButtonUI {

    private static final String PROPERTY_PREFIX = "Button.";
    private static BasicButtonUI basicButtonUI;


    protected int defaultTextIconGap;

    protected int defaultTextShiftOffset;
    
    private int textShiftOffset = 0;

    private Color focusColor;
    private Color disabledTextColor;

    private final Rectangle viewR = new Rectangle();
    private final Rectangle iconR = new Rectangle();
    private final Rectangle textR = new Rectangle();

    public static ComponentUI createUI(final JComponent c) {
        if (basicButtonUI == null) {
            basicButtonUI = new BasicButtonUI();
        }
        return basicButtonUI;
    }

    public void installUI(final JComponent c) {
        final AbstractButton button = (AbstractButton)c;
        installDefaults(button);
        installListeners(button);
        installKeyboardActions(button);
    }

    public void uninstallUI(final JComponent c) {
        final AbstractButton button = (AbstractButton)c;
        uninstallKeyboardActions(button);
        uninstallListeners(button);
        uninstallDefaults(button);
    }

    public Dimension getPreferredSize(final JComponent c) {
        return ButtonCommons.getPreferredSize((AbstractButton)c, null);
    }

    public int getDefaultTextIconGap(final AbstractButton button) {
        return defaultTextIconGap;
    }

    public void paint(final Graphics g, final JComponent c) {
        final AbstractButton button = (AbstractButton)c;
        viewR.setBounds(0, 0, 0, 0);
        String clippedText = ButtonCommons.getPaintingParameters(button, viewR, iconR,
                                                                 textR, button.getIcon());

        paintButtonPressed(g, button);
        paintIcon(g, button, iconR);
        paintText(g, button, textR, clippedText);
        if (isFocusPainted(button)) {
            paintFocus(g, button, viewR, textR, iconR);
        }
    }

    protected void paintButtonPressed(final Graphics g, final AbstractButton button) {
    }

    protected void paintIcon(final Graphics g, final JComponent c, final Rectangle iconRect) {
        final Icon icon = ButtonCommons.getCurrentIcon((AbstractButton)c);
        if (icon != null) {
            icon.paintIcon(c, g, iconRect.x, iconRect.y);
        }
    }

    protected void paintText(final Graphics g, final AbstractButton b,
                             final Rectangle textRect, final String text) {
        paintText(g, (JComponent)b, textRect, text);
    }

    protected void paintText(final Graphics g, final JComponent c,
                             final Rectangle textRect, final String text) {
        final AbstractButton b = (AbstractButton)c;
        final Color color = b.isEnabled() ? b.getForeground() : disabledTextColor;

        final int currentTextShiftOffset = getTextShiftOffset();
        textRect.translate(currentTextShiftOffset, currentTextShiftOffset);
        ButtonCommons.paintText(g, b, textRect, text, color);
    }

    protected void paintFocus(final Graphics g, final AbstractButton b, final Rectangle viewRect,
                              final Rectangle textRect, final Rectangle iconRect) {

        ButtonCommons.paintFocus(g, ButtonCommons.getFocusRect(viewRect, textRect, iconRect), focusColor);
    }

    protected BasicButtonListener createButtonListener(final AbstractButton button) {
        return new BasicButtonListener(button);
    }

    protected void installListeners(final AbstractButton button) {
        BasicButtonListener listener = createButtonListener(button);
        button.addPropertyChangeListener(listener);
        button.addChangeListener(listener);
        button.addMouseListener(listener);
        button.addMouseMotionListener(listener);
        button.addFocusListener(listener);
    }

    protected void uninstallListeners(final AbstractButton button) {
        BasicButtonListener listener = getBasicButtonListener(button);
        button.removePropertyChangeListener(listener);
        button.removeChangeListener(listener);
        button.removeMouseListener(listener);
        button.removeMouseMotionListener(listener);
        button.removeFocusListener(listener);
    }

    protected void installKeyboardActions(final AbstractButton button) {
        BasicButtonListener listener = getBasicButtonListener(button);
        if (listener != null) {
            listener.installKeyboardActions(button);
        }
        SwingUtilities.replaceUIInputMap(button, JComponent.WHEN_FOCUSED,
                                         (InputMap)UIManager.get(getPropertyPrefix() + "focusInputMap"));
    }

    protected void uninstallKeyboardActions(final AbstractButton button) {
        BasicButtonListener listener = getBasicButtonListener(button);
        if (listener != null) {
            listener.uninstallKeyboardActions(button);
        }
        SwingUtilities.replaceUIInputMap(button, JComponent.WHEN_FOCUSED, null);
    }

    protected void installDefaults(final AbstractButton button) {
        LookAndFeel.installColorsAndFont(button, getPropertyPrefix() + "background",
                getPropertyPrefix() + "foreground", getPropertyPrefix() + "font");

        LookAndFeel.installProperty(button, "opaque", Boolean.TRUE);
        LookAndFeel.installBorder(button, getPropertyPrefix() + "border");
        LookAndFeel.installProperty(button, "alignmentX", new Float(0));
        defaultTextIconGap = UIManager.getInt(getPropertyPrefix() + "textIconGap");
        defaultTextShiftOffset = UIManager.getInt(getPropertyPrefix() + "textShiftOffset");

        button.setMargin(UIManager.getInsets(getPropertyPrefix() + "margin"));

        disabledTextColor = button.getBackground().darker();
        focusColor = UIManager.getColor("activeCaptionBorder");
    }

    protected void uninstallDefaults(final AbstractButton button) {
        LookAndFeel.uninstallBorder(button);
        Utilities.uninstallColorsAndFont(button);
        if (Utilities.isUIResource(button.getMargin())) {
            button.setMargin(null);
        }
    }

    protected String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

    protected void setTextShiftOffset() {
        textShiftOffset = defaultTextShiftOffset;
    }

    protected void clearTextShiftOffset() {
        textShiftOffset = 0;
    }

    protected int getTextShiftOffset() {
        return textShiftOffset;
    }

    private boolean isFocusPainted(final AbstractButton button) {
        return (button.isEnabled() && button.isFocusPainted() && button.isFocusOwner()
                && (button.getIcon() != null || !Utilities.isEmptyString(button.getText())));
    }

    private BasicButtonListener getBasicButtonListener(final AbstractButton button) {
        EventListener[] listeners = button.getPropertyChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] instanceof BasicButtonListener) {
                return (BasicButtonListener)listeners[i];
            }
        }

        return null;
    }
}


