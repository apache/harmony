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

package javax.swing.plaf.synth;

import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;

import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.harmony.x.swing.internal.nls.Messages;
import org.xml.sax.SAXException;

import org.apache.harmony.luni.util.NotImplementedException;

public class SynthLookAndFeel extends BasicLookAndFeel implements Serializable {

    /** Path used in UIDefaults */
    private final static String pathToSynthLaf = "javax.swing.plaf.synth.SynthLookAndFeel"; //$NON-NLS-1$

    private static SynthStyleFactory currentFactory;

    private UIDefaults uiDefaults;

    public static SynthStyle getStyle(JComponent c, Region r) {

        return currentFactory.getStyle(c, r);
    }

    public static SynthStyleFactory getStyleFactory() {

        return currentFactory;
    }

    public static void setStyleFactory(SynthStyleFactory proposed) {
        currentFactory = proposed;
    }

    /**
     * Creates the Synth UI object corresponds JComponent given. (This method
     * used by UIManager because all the UIs classes are package-protected
     * according to spec, so reference in defaults table points to
     * SynthLookAndFeel)
     * 
     * @see SynthLookAndFeel#initClassDefaults(UIDefaults)
     */
    @SuppressWarnings("nls")
    public static ComponentUI createUI(JComponent c)
            throws NotImplementedException {

        // Commented because UI's patch is not ready for now

        // String uiClassID = c.getUIClassID().intern();
        //
        // if (uiClassID == "InternalFrameUI") {
        // return SynthInternalFrameUI.createUI(c);
        // } else if (uiClassID == "ViewportUI") {
        // return SynthViewportUI.createUI(c);
        // } else if (uiClassID == "ScrollBarUI") {
        // SynthScrollBarUI.createUI(c);
        // } else if (uiClassID == "ToolTipUI") {
        // return SynthToolTipUI.createUI(c);
        // } else if (uiClassID == "MenuItemUI") {
        // return SynthMenuItemUI.createUI(c);
        // } else if (uiClassID == "MenuUI") {
        // return SynthMenuUI.createUI(c);
        // } else if (uiClassID == "TextAreaUI") {
        // return SynthTextAreaUI.createUI(c);
        // } else if (uiClassID == "PopupMenuUI") {
        // return SynthPopupMenuUI.createUI(c);
        // } else if (uiClassID == "ScrollPaneUI") {
        // return SynthScrollPaneUI.createUI(c);
        // } else if (uiClassID == "SliderUI") {
        // return SynthSliderUI.createUI(c);
        // } else if (uiClassID == "ComboBoxUI") {
        // return SynthComboBoxUI.createUI(c);
        // } else if (uiClassID == "RadioButtonUI") {
        // return SynthRadioButtonUI.createUI(c);
        // } else if (uiClassID == "FormattedTextFieldUI") {
        // return SynthFormattedTextFieldUI.createUI(c);
        // } else if (uiClassID == "TreeUI") {
        // return SynthTreeUI.createUI(c);
        // } else if (uiClassID == "MenuBarUI") {
        // return SynthMenuBarUI.createUI(c);
        // } else if (uiClassID == "RadioButtonMenuItemUI") {
        // return SynthRadioButtonMenuItemUI.createUI(c);
        // } else if (uiClassID == "ProgressBarUI") {
        // return SynthProgressBarUI.createUI(c);
        // } else if (uiClassID == "ToolBarUI") {
        // return SynthToolBarUI.createUI(c);
        // } else if (uiClassID == "ColorChooserUI") {
        // return SynthColorChooserUI.createUI(c);
        // } else if (uiClassID == "ToolBarSeparatorUI") {
        // return SynthToolBarSeparatorUI.createUI(c);
        // } else if (uiClassID == "TabbedPaneUI") {
        // return SynthTabbedPaneUI.createUI(c);
        // } else if (uiClassID == "DesktopPaneUI") {
        // return SynthDesktopPaneUI.createUI(c);
        // } else if (uiClassID == "TableUI") {
        // return SynthTableUI.createUI(c);
        // } else if (uiClassID == "PanelUI") {
        // return SynthPanelUI.createUI(c);
        // } else if (uiClassID == "CheckBoxMenuItemUI") {
        // return SynthCheckBoxMenuItemUI.createUI(c);
        // } else if (uiClassID == "PasswordFieldUI") {
        // return SynthPasswordFieldUI.createUI(c);
        // } else if (uiClassID == "CheckBoxUI") {
        // return SynthCheckBoxUI.createUI(c);
        // } else if (uiClassID == "TableHeaderUI") {
        // return SynthTableHeaderUI.createUI(c);
        // } else if (uiClassID == "SplitPaneUI") {
        // return SynthSplitPaneUI.createUI(c);
        // } else if (uiClassID == "EditorPaneUI") {
        // return SynthEditorPaneUI.createUI(c);
        // } else if (uiClassID == "ListUI") {
        // return SynthListUI.createUI(c);
        // } else if (uiClassID == "SpinnerUI") {
        // return SynthSpinnerUI.createUI(c);
        // } else if (uiClassID == "DesktopIconUI") {
        // return SynthDesktopIconUI.createUI(c);
        // } else if (uiClassID == "TextFieldUI") {
        // return SynthTextFieldUI.createUI(c);
        // } else if (uiClassID == "TextPaneUI") {
        // return SynthTextPaneUI.createUI(c);
        // } else if (uiClassID == "ButtonUI") {
        // return SynthButtonUI.createUI(c);
        // } else if (uiClassID == "LabelUI") {
        // return SynthLabelUI.createUI(c);
        // } else if (uiClassID == "ToggleButtonUI") {
        // SynthToggleButtonUI.createUI(c);
        // } else if (uiClassID == "OptionPaneUI") {
        // return SynthOptionPaneUI.createUI(c);
        // } else if (uiClassID == "PopupMenuSeparatorUI") {
        // return SynthPopupMenuSeparatorUI.createUI(c);
        // } else if (uiClassID == "RootPaneUI") {
        // return SynthRootPaneUI.createUI(c);
        // } else if (uiClassID == "SeparatorUI") {
        // return SynthSeparatorUI.createUI(c);
        // }
        // compatible with RI
        return null;
    }

    /**
     * Renew the synth styles for the JComponent. This method isn't fully
     * correct, but does what needs (The method is unused in package)
     */
    public static void updateStyles(Component c) {
        c.setName(c.getName() + " "); //$NON-NLS-1$
    }

    /**
     * Calculates the region corresponds JComponent given
     */
    public static Region getRegion(JComponent c) {

        return Region.getRegionFromUIID(c.getUIClassID());
    }

    @Override
    public String getName() {

        return "Synth Look and Feel"; //$NON-NLS-1$
    }

    @Override
    public String getID() {

        return "Synth"; //$NON-NLS-1$
    }

    @Override
    public String getDescription() {

        return Messages.getString("swing.B4"); //$NON-NLS-1$
    }

    @Override
    public boolean isNativeLookAndFeel() {

        return false;
    }

    @Override
    public boolean isSupportedLookAndFeel() {

        return true;
    }

    @Override
    public UIDefaults getDefaults() {

        if (uiDefaults == null) {
            uiDefaults = new UIDefaults();
            initClassDefaults(uiDefaults);
            initComponentDefaults(uiDefaults);
        }

        return uiDefaults;
    }

    @Override
    public void initialize() {
        // Do nothing
    }

    @Override
    public void uninitialize() {
        // Do nothing
    }

    @SuppressWarnings("unused")
    public void load(InputStream input, Class<?> resourceBase)
            throws ParseException, IllegalArgumentException {

        if (input == null || resourceBase == null) {
            throw new IllegalArgumentException(Messages
                    .getString("swing.err.1D")); //$NON-NLS-1$
        }

        try {

            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            saxParser.parse(input, new XMLSynthParser(resourceBase));

        } catch (ParserConfigurationException e) {
            throw new ParseException(e.getMessage(), 0);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new ParseException(e.getMessage(), 0);
        } catch (IOException e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }

    /** The default implementation returns false */
    public boolean shouldUpdateStyleOnAncestorChanged() {

        return false;
    }

    @SuppressWarnings("nls")
    @Override
    protected void initClassDefaults(UIDefaults defaults) {
        Object[] initDefaults = { "InternalFrameUI", pathToSynthLaf,
                "ViewportUI", pathToSynthLaf, "ScrollBarUI", pathToSynthLaf,
                "ToolTipUI", pathToSynthLaf, "MenuItemUI", pathToSynthLaf,
                "MenuUI", pathToSynthLaf, "TextAreaUI", pathToSynthLaf,
                "PopupMenuUI", pathToSynthLaf, "ScrollPaneUI", pathToSynthLaf,
                "SliderUI", pathToSynthLaf, "ComboBoxUI", pathToSynthLaf,
                "RadioButtonUI", pathToSynthLaf, "FormattedTextFieldUI",
                pathToSynthLaf, "TreeUI", pathToSynthLaf, "MenuBarUI",
                pathToSynthLaf, "RadioButtonMenuItemUI", pathToSynthLaf,
                "ProgressBarUI", pathToSynthLaf, "ToolBarUI", pathToSynthLaf,
                "ColorChooserUI", pathToSynthLaf, "ToolBarSeparatorUI",
                pathToSynthLaf, "TabbedPaneUI", pathToSynthLaf,
                "DesktopPaneUI", pathToSynthLaf, "TableUI", pathToSynthLaf,
                "PanelUI", pathToSynthLaf, "CheckBoxMenuItemUI",
                pathToSynthLaf, "PasswordFieldUI", pathToSynthLaf,
                "CheckBoxUI", pathToSynthLaf, "TableHeaderUI", pathToSynthLaf,
                "SplitPaneUI", pathToSynthLaf, "EditorPaneUI", pathToSynthLaf,
                "ListUI", pathToSynthLaf, "SpinnerUI", pathToSynthLaf,
                "DesktopIconUI", pathToSynthLaf, "TextFieldUI", pathToSynthLaf,
                "TextPaneUI", pathToSynthLaf, "ButtonUI", pathToSynthLaf,
                "LabelUI", pathToSynthLaf, "ToggleButtonUI", pathToSynthLaf,
                "OptionPaneUI", pathToSynthLaf, "PopupMenuSeparatorUI",
                pathToSynthLaf, "RootPaneUI", pathToSynthLaf, "SeparatorUI",
                pathToSynthLaf };
        defaults.putDefaults(initDefaults);
    }

}
