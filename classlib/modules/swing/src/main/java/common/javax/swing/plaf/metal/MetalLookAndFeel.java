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
 * @author Sergey Burlak
 */

package javax.swing.plaf.metal;

import java.awt.Component;

import javax.swing.JTextField;
import javax.swing.UIDefaults;
import javax.swing.UIDefaults.ActiveValue;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.text.DefaultEditorKit;

/*
 * In order not to break client application UI appearence the values for insets,
 * borders and other size-related keys as well as 'feel'-related values are preserved as in 1.5 release.
 * Color-related values are chosen to provide consistent look of UI components.
 */
public class MetalLookAndFeel extends BasicLookAndFeel {
    private static final String METAL_RESOURCE_BUNDLE = "org.apache.harmony.x.swing.plaf.resources.metal.MetalResourceBundle";

    private static MetalTheme metalTheme;

    static {
        metalTheme = new DefaultMetalTheme();
    }

    /**
     * Initialize system color default values
     * @param uiDefs defaults table
     */
    protected void initSystemColorDefaults(final UIDefaults uiDefs) {
        super.initSystemColorDefaults(uiDefs);

        Object[] systemColors = new Object[] {
                 "activeCaption", getWindowTitleBackground(),
                 "activeCaptionBorder", getPrimaryControlShadow(),
                 "control", getControl(),
                 "controlDkShadow", getControlDarkShadow(),
                 "controlHighlight", getControlHighlight(),
                 "controlLtHighlight", getControlHighlight(),
                 "controlShadow", getControlShadow(),
                 "controlText", getControlTextColor(),
                 "desktop", getDesktopColor(),
                 "inactiveCaption", getWindowTitleInactiveBackground(),
                 "inactiveCaptionBorder", getControlShadow(),
                 "inactiveCaptionText", getWindowTitleInactiveForeground(),
                 "info", getPrimaryControl(),
                 "infoText", getPrimaryControlInfo(),
                 "menu", getMenuBackground(),
                 "menuText", getMenuForeground(),
                 "scrollbar", getControl(),
                 "text", getWindowBackground(),
                 "textHighlight", getTextHighlightColor(),
                 "textHighlightText", getHighlightedTextColor(),
                 "textInactiveText", getInactiveSystemTextColor(),
                 "textText", getUserTextColor(),
                 "window", getWindowBackground(),
                 "windowBorder", getControl(),
                 "windowText", getUserTextColor() };

        uiDefs.putDefaults(systemColors);
    }

    /**
     * Initialize component default values in the defaults table
     * @param uiDefs defaults table
     */
    protected void initComponentDefaults(final UIDefaults uiDefs) {
        UIDefaults.ActiveValue controlTextFont = controlTextFontActiveValue();
        UIDefaults.ActiveValue menuTextFont = menuTextFontActiveValue();
        UIDefaults.ActiveValue userTextFont = userTextFontActiveValue();
        UIDefaults.ActiveValue subTextFont = subTextFontActiveValue();
        UIDefaults.ActiveValue windowTitleFont = windowTitleFontActiveValue();
        UIDefaults.ActiveValue systemTextFont = systemTextFontActiveValue();

        Object[] componentColorDefaults = new Object[] {
                 "Button.disabledText", getInactiveControlTextColor(),
                 "Button.focus", getFocusColor(),
                 "Button.select", getControlShadow(),
                 "CheckBox.disabledText", getInactiveControlTextColor(),
                 "CheckBox.focus", getFocusColor(),
                 "Checkbox.select", getControlShadow(),
                 "CheckBoxMenuItem.acceleratorForeground", getAcceleratorForeground(),
                 "CheckBoxMenuItem.acceleratorSelectionForeground", getAcceleratorSelectedForeground(),
                 "CheckBoxMenuItem.disabledForeground", getMenuDisabledForeground(),
                 "CheckBoxMenuItem.selectionBackground", getMenuSelectedBackground(),
                 "CheckBoxMenuItem.selectionForeground", getMenuSelectedForeground(),
                 "ComboBox.selectionBackground", getPrimaryControlShadow(),
                 "ComboBox.selectionForeground", getControlTextColor(),
                 "DesktopIcon.background", getControl(),
                 "DesktopIcon.foreground", getControlTextColor(),
                 "Label.disabledForeground", getInactiveSystemTextColor(),
                 "Label.foreground", getSystemTextColor(),
                 "Menu.acceleratorForeground", getAcceleratorForeground(),
                 "Menu.acceleratorSelectionForeground", getAcceleratorSelectedForeground(),
                 "Menu.disabledForeground", getMenuDisabledForeground(),
                 "Menu.selectionBackground", getMenuSelectedBackground(),
                 "Menu.selectionForeground", getMenuSelectedForeground(),
                 "MenuItem.acceleratorForeground", getAcceleratorForeground(),
                 "MenuItem.acceleratorSelectionForeground", getAcceleratorSelectedForeground(),
                 "MenuItem.disabledForeground", getMenuDisabledForeground(),
                 "MenuItem.selectionBackground", getMenuSelectedBackground(),
                 "MenuItem.selectionForeground", getMenuSelectedForeground(),
                 "OptionPane.errorDialog.border.background", new ColorUIResource(0x80, 0x40, 0x40),
                 "OptionPane.errorDialog.titlePane.background", new ColorUIResource(0xE0, 0xB0, 0xB0),
                 "OptionPane.errorDialog.titlePane.foreground", new ColorUIResource(0, 0x20, 0),
                 "OptionPane.errorDialog.titlePane.shadow", new ColorUIResource(0xB0, 0x70, 0x70),
                 "OptionPane.questionDialog.border.background", new ColorUIResource(0x40, 0x70, 0x40),
                 "OptionPane.questionDialog.titlePane.background", new ColorUIResource(0xB0, 0xE0, 0xB0),
                 "OptionPane.questionDialog.titlePane.foreground", new ColorUIResource(0, 0x20, 0),
                 "OptionPane.questionDialog.titlePane.shadow", new ColorUIResource(0x70, 0xB0, 0x70),
                 "OptionPane.warningDialog.border.background", new ColorUIResource(0x70, 0x70, 0x30),
                 "OptionPane.warningDialog.titlePane.background", new ColorUIResource(0xE0, 0xE0, 0xA0),
                 "OptionPane.warningDialog.titlePane.foreground", new ColorUIResource(0x30, 0x30, 0),
                 "OptionPane.warningDialog.titlePane.shadow", new ColorUIResource(0xB0, 0xB0, 0x60),
                 "ProgressBar.foreground", getPrimaryControlShadow(),
                 "ProgressBar.selectionBackground", getPrimaryControlDarkShadow(),
                 "RadioButton.disabledText", getInactiveControlTextColor(),
                 "RadioButton.focus", getFocusColor(),
                 "RadioButton.select", getControlShadow(),
                 "RadioButtonMenuItem.acceleratorForeground", getAcceleratorForeground(),
                 "RadioButtonMenuItem.acceleratorSelectionForeground", getAcceleratorSelectedForeground(),
                 "RadioButtonMenuItem.disabledForeground", getMenuDisabledForeground(),
                 "RadioButtonMenuItem.selectionBackground", getMenuSelectedBackground(),
                 "RadioButtonMenuItem.selectionForeground", getMenuSelectedForeground(),
                 "ScrollBar.background", getControl(),
                 "ScrollBar.darkShadow", getControlDarkShadow(),
                 "ScrollBar.highlight", getControlHighlight(),
                 "ScrollBar.shadow", getControlShadow(),
                 "ScrollBar.thumb", getPrimaryControlShadow(),
                 "ScrollBar.thumbHighlight", getPrimaryControl(),
                 "ScrollBar.thumbShadow", getPrimaryControlDarkShadow(),
                 "Separator.background", getSeparatorBackground(),
                 "Separator.foreground", getSeparatorForeground(),
                 "Slider.focus", getFocusColor(),
                 "Slider.foreground", getPrimaryControlShadow(),
                 "TabbedPane.background", getControlShadow(),
                 "TabbedPane.focus", getPrimaryControlDarkShadow(),
                 "TabbedPane.light", getControl(),
                 "TabbedPane.selected", getControl(),
                 "TabbedPane.selectHighlight", getControlHighlight(),
                 "TabbedPane.tabAreaBackground", getControl(),
                 "Table.gridColor", getControlShadow(),
                 "TitledBorder.titleColor", getSystemTextColor(),
                 "ToggleButton.disabledText", getInactiveControlTextColor(),
                 "ToggleButton.focus", getFocusColor(),
                 "ToggleButton.select", getControlShadow(),
                 "ToolBar.background", getMenuBackground(),
                 "ToolBar.dockingBackground", getMenuBackground(),
                 "ToolBar.dockingForeground", getPrimaryControlDarkShadow(),
                 "ToolBar.floatingBackground", getMenuBackground(),
                 "ToolBar.floatingForeground", getPrimaryControl(),
                 "ToolBar.foreground", getMenuForeground(),
                 "ToolTip.backgroundInactive", getControl(),
                 "ToolTip.foregroundInactive", getControlDarkShadow(),
                 "Tree.hash", getPrimaryControl(),
                 "Tree.line", getPrimaryControl(),
                 "Tree.selectionBorderColor", getFocusColor(),
                 "Tree.textBackground", getWindowBackground(),
        };

        Object[] tableAncestorInputMap = new Object[] {"ctrl C", "copy", "ctrl V", "paste",
                                                       "ctrl X", "cut", "COPY", "copy", "PASTE", "paste",
                                                       "CUT", "cut", "RIGHT", "selectNextColumn",
                                                       "ctrl RIGHT", "selectNextColumnChangeLead",
                                                       "KP_RIGHT", "selectNextColumn",
                                                       "ctrl KP_RIGHT", "selectNextColumnChangeLead",
                                                       "LEFT", "selectPreviousColumn",
                                                       "ctrl LEFT", "selectPreviousColumnChangeLead",
                                                       "KP_LEFT", "selectPreviousColumn",
                                                       "ctrl KP_LEFT", "selectPreviousColumnChangeLead",
                                                       "DOWN", "selectNextRow",
                                                       "ctrl DOWN", "selectNextRowChangeLead",
                                                       "KP_DOWN", "selectNextRow",
                                                       "ctrl KP_DOWN", "selectNextRowChangeLead",
                                                       "UP", "selectPreviousRow",
                                                       "ctrl UP", "selectPreviousRowChangeLead",
                                                       "KP_UP", "selectPreviousRow",
                                                       "ctrl KP_UP", "selectPreviousRowChangeLead",
                                                       "shift RIGHT", "selectNextColumnExtendSelection",
                                                       "shift ctrl RIGHT", "selectNextColumnExtendSelection",
                                                       "shift KP_RIGHT", "selectNextColumnExtendSelection",
                                                       "shift ctrl KP_RIGHT", "selectNextColumnExtendSelection",
                                                       "shift LEFT", "selectPreviousColumnExtendSelection",
                                                       "shift ctrl LEFT", "selectPreviousColumnExtendSelection",
                                                       "shift KP_LEFT", "selectPreviousColumnExtendSelection",
                                                       "shift ctrl KP_LEFT", "selectPreviousColumnExtendSelection",
                                                       "shift DOWN", "selectNextRowExtendSelection",
                                                       "shift ctrl DOWN", "selectNextRowExtendSelection",
                                                       "shift KP_DOWN", "selectNextRowExtendSelection",
                                                       "shift ctrl KP_DOWN", "selectNextRowExtendSelection",
                                                       "shift UP", "selectPreviousRowExtendSelection",
                                                       "shift ctrl UP", "selectPreviousRowExtendSelection",
                                                       "shift KP_UP", "selectPreviousRowExtendSelection",
                                                       "shift ctrl KP_UP", "selectPreviousRowExtendSelection",
                                                       "PAGE_UP", "scrollUpChangeSelection",
                                                       "PAGE_DOWN", "scrollDownChangeSelection",
                                                       "HOME", "selectFirstColumn",
                                                       "END", "selectLastColumn",
                                                       "shift PAGE_UP", "scrollUpExtendSelection",
                                                       "shift PAGE_DOWN", "scrollDownExtendSelection",
                                                       "shift HOME", "selectFirstColumnExtendSelection",
                                                       "shift END", "selectLastColumnExtendSelection",
                                                       "ctrl PAGE_UP", "scrollLeftChangeSelection",
                                                       "ctrl PAGE_DOWN", "scrollRightChangeSelection",
                                                       "ctrl HOME", "selectFirstRow",
                                                       "ctrl END", "selectLastRow",
                                                       "ctrl shift PAGE_UP", "scrollRightExtendSelection",
                                                       "ctrl shift PAGE_DOWN", "scrollLeftExtendSelection",
                                                       "ctrl shift HOME", "selectFirstRowExtendSelection",
                                                       "ctrl shift END", "selectLastRowExtendSelection",
                                                       "TAB", "selectNextColumnCell",
                                                       "shift TAB", "selectPreviousColumnCell",
                                                       "ENTER", "selectNextRowCell",
                                                       "shift ENTER", "selectPreviousRowCell",
                                                       "ctrl A", "selectAll", "ctrl SLASH", "selectAll", "ctrl BACK_SLASH", "clearSelection",
                                                       "F2", "startEditing", "ESCAPE", "cancel",
                                                       "SPACE", "addToSelection", "ctrl SPACE", "toggleAndAnchor",
                                                       "shift ctrl SPACE", "moveSelectionTo", "shift SPACE", "extendTo"};
        Object[] editorPaneFocusInputMap = new Object[] {"ctrl C", DefaultEditorKit.copyAction,
                                                         "ctrl V", DefaultEditorKit.pasteAction,
                                                         "ctrl X", DefaultEditorKit.cutAction,
                                                         "COPY", DefaultEditorKit.copyAction,
                                                         "PASTE", DefaultEditorKit.pasteAction,
                                                         "CUT", DefaultEditorKit.cutAction,
                                                         "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                                                         "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
                                                         "shift RIGHT", DefaultEditorKit.selectionForwardAction,
                                                         "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
                                                         "ctrl LEFT", DefaultEditorKit.previousWordAction,
                                                         "ctrl KP_LEFT", DefaultEditorKit.previousWordAction,
                                                         "ctrl RIGHT", DefaultEditorKit.nextWordAction,
                                                         "ctrl KP_RIGHT", DefaultEditorKit.nextWordAction,
                                                         "ctrl shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
                                                         "ctrl shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
                                                         "ctrl shift RIGHT", DefaultEditorKit.selectionNextWordAction,
                                                         "ctrl shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
                                                         "ctrl A", DefaultEditorKit.selectAllAction,
                                                         "HOME", DefaultEditorKit.beginLineAction,
                                                         "END", DefaultEditorKit.endLineAction,
                                                         "shift HOME", DefaultEditorKit.selectionBeginLineAction,
                                                         "shift END", DefaultEditorKit.selectionEndLineAction,
                                                         "UP", DefaultEditorKit.upAction,
                                                         "KP_UP", DefaultEditorKit.upAction,
                                                         "DOWN", DefaultEditorKit.downAction,
                                                         "KP_DOWN", DefaultEditorKit.downAction,
                                                         "PAGE_UP", DefaultEditorKit.pageUpAction,
                                                         "PAGE_DOWN", DefaultEditorKit.pageDownAction,
                                                         "shift PAGE_UP", "selection-page-up",
                                                         "shift PAGE_DOWN", "selection-page-down",
                                                         "ctrl shift PAGE_UP", "selection-page-left",
                                                         "ctrl shift PAGE_DOWN", "selection-page-right",
                                                         "shift UP", DefaultEditorKit.selectionUpAction,
                                                         "shift KP_UP", DefaultEditorKit.selectionUpAction,
                                                         "shift DOWN", DefaultEditorKit.selectionDownAction,
                                                         "shift KP_DOWN", DefaultEditorKit.selectionDownAction,
                                                         "ENTER", DefaultEditorKit.insertBreakAction,
                                                         "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                                                         "ctrl H", DefaultEditorKit.deletePrevCharAction,
                                                         "DELETE", DefaultEditorKit.deleteNextCharAction,
                                                         "RIGHT", DefaultEditorKit.forwardAction,
                                                         "LEFT", DefaultEditorKit.backwardAction,
                                                         "KP_RIGHT", DefaultEditorKit.forwardAction,
                                                         "KP_LEFT", DefaultEditorKit.backwardAction,
                                                         "TAB", DefaultEditorKit.insertTabAction,
                                                         "ctrl BACK_SLASH", "unselect",
                                                         "ctrl HOME", DefaultEditorKit.beginAction,
                                                         "ctrl END", DefaultEditorKit.endAction,
                                                         "ctrl shift HOME", DefaultEditorKit.selectionBeginAction,
                                                         "ctrl shift END", DefaultEditorKit.selectionEndAction,
                                                         "ctrl T", "next-link-action",
                                                         "ctrl shift T", "previous-link-action",
                                                         "ctrl SPACE", "activate-link-action",
                                                         "control shift O", "toggle-componentOrientation" };
        Object[] comboBoxAncestorInputMap = new Object[] {"ESCAPE", "hidePopup", "PAGE_UP", "pageUpPassThrough",
                                                          "PAGE_DOWN", "pageDownPassThrough",
                                                          "HOME", "homePassThrough",
                                                          "END", "endPassThrough",
                                                          "DOWN", "selectNext",
                                                          "KP_DOWN", "selectNext",
                                                          "alt DOWN", "togglePopup",
                                                          "alt KP_DOWN", "togglePopup",
                                                          "alt UP", "togglePopup",
                                                          "alt KP_UP", "togglePopup",
                                                          "SPACE", "spacePopup",
                                                          "ENTER", "enterPressed",
                                                          "UP", "selectPrevious",
                                                          "KP_UP", "selectPrevious" };
        Object[] desktopAncestorInputMap = new Object[] {"ctrl F5", "restore", "ctrl F4", "close",
                                                         "ctrl F7", "move", "ctrl F8", "resize",
                                                         "RIGHT", "right", "KP_RIGHT", "right",
                                                         "shift RIGHT", "shrinkRight",
                                                         "shift KP_RIGHT", "shrinkRight",
                                                         "LEFT", "left", "KP_LEFT", "left",
                                                         "shift LEFT", "shrinkLeft",
                                                         "shift KP_LEFT", "shrinkLeft",
                                                         "UP", "up", "KP_UP", "up",
                                                         "shift UP", "shrinkUp",
                                                         "shift KP_UP", "shrinkUp",
                                                         "DOWN", "down", "KP_DOWN", "down",
                                                         "shift DOWN", "shrinkDown",
                                                         "shift KP_DOWN", "shrinkDown",
                                                         "ESCAPE", "escape",
                                                         "ctrl F9", "minimize",
                                                         "ctrl F10", "maximize",
                                                         "ctrl F6", "selectNextFrame",
                                                         "ctrl TAB", "selectNextFrame",
                                                         "ctrl alt F6", "selectNextFrame",
                                                         "shift ctrl alt F6", "selectPreviousFrame",
                                                         "ctrl F12", "navigateNext",
                                                         "shift ctrl F12", "navigatePrevious" };
        Object[] auditoryCuesDefaultCueList = new Object[] { "OptionPane.errorSound", "OptionPane.informationSound",
                                                             "OptionPane.questionSound", "OptionPane.warningSound" };
        Object[] formattedTextFieldFocusInputMap = new Object[] {"ctrl C", DefaultEditorKit.copyAction,
                                                                 "ctrl V", DefaultEditorKit.pasteAction,
                                                                 "ctrl X", DefaultEditorKit.cutAction,
                                                                 "COPY", DefaultEditorKit.copyAction,
                                                                 "PASTE", DefaultEditorKit.pasteAction,
                                                                 "CUT", DefaultEditorKit.cutAction,
                                                                 "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                                                                 "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
                                                                 "shift RIGHT", DefaultEditorKit.selectionForwardAction,
                                                                 "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
                                                                 "ctrl LEFT", DefaultEditorKit.previousWordAction,
                                                                 "ctrl KP_LEFT", DefaultEditorKit.previousWordAction,
                                                                 "ctrl RIGHT", DefaultEditorKit.nextWordAction,
                                                                 "ctrl KP_RIGHT", DefaultEditorKit.nextWordAction,
                                                                 "ctrl shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
                                                                 "ctrl shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
                                                                 "ctrl shift RIGHT", DefaultEditorKit.selectionNextWordAction,
                                                                 "ctrl shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
                                                                 "ctrl A", DefaultEditorKit.selectAllAction,
                                                                 "HOME", DefaultEditorKit.beginLineAction,
                                                                 "END", DefaultEditorKit.endLineAction,
                                                                 "shift HOME", DefaultEditorKit.selectionBeginLineAction,
                                                                 "shift END", DefaultEditorKit.selectionEndLineAction,
                                                                 "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                                                                 "ctrl H", DefaultEditorKit.deletePrevCharAction,
                                                                 "DELETE", DefaultEditorKit.deleteNextCharAction,
                                                                 "RIGHT", DefaultEditorKit.forwardAction,
                                                                 "LEFT", DefaultEditorKit.backwardAction,
                                                                 "KP_RIGHT", DefaultEditorKit.forwardAction,
                                                                 "KP_LEFT", DefaultEditorKit.backwardAction,
                                                                 "ENTER", JTextField.notifyAction,
                                                                 "ctrl BACK_SLASH", "unselect",
                                                                 "control shift O", "toggle-componentOrientation",
                                                                 "ESCAPE", "reset-field-edit",
                                                                 "UP", "increment",
                                                                 "KP_UP", "increment",
                                                                 "DOWN", "decrement",
                                                                 "KP_DOWN", "decrement" };
        Object[] treeFocusInputMap = new Object[] {"ctrl C", "copy", "ctrl V", "paste",
                                                   "ctrl X", "cut", "COPY", "copy",
                                                   "PASTE", "paste",
                                                   "CUT", "cut", "UP", "selectPrevious",
                                                   "KP_UP", "selectPrevious",
                                                   "shift UP", "selectPreviousExtendSelection",
                                                   "shift ctrl UP", "selectPreviousExtendSelection",
                                                   "shift KP_UP", "selectPreviousExtendSelection",
                                                   "shift ctrl KP_UP", "selectPreviousExtendSelection",
                                                   "DOWN", "selectNext",
                                                   "KP_DOWN", "selectNext",
                                                   "shift DOWN", "selectNextExtendSelection",
                                                   "shift ctrl DOWN", "selectNextExtendSelection",
                                                   "shift KP_DOWN", "selectNextExtendSelection",
                                                   "shift ctrl KP_DOWN", "selectNextExtendSelection",
                                                   "RIGHT", "selectChild", "KP_RIGHT", "selectChild",
                                                   "LEFT", "selectParent", "KP_LEFT", "selectParent",
                                                   "PAGE_UP", "scrollUpChangeSelection",
                                                   "shift PAGE_UP", "scrollUpExtendSelection",
                                                   "PAGE_DOWN", "scrollDownChangeSelection",
                                                   "shift PAGE_DOWN", "scrollDownExtendSelection",
                                                   "HOME", "selectFirst", "shift HOME", "selectFirstExtendSelection",
                                                   "shift ctrl HOME", "selectFirstExtendSelection",
                                                   "END", "selectLast", "shift END", "selectLastExtendSelection",
                                                   "shift ctrl END", "selectLastExtendSelection",
                                                   "F2", "startEditing", "ctrl A", "selectAll",
                                                   "ctrl SLASH", "selectAll",
                                                   "ctrl BACK_SLASH", "clearSelection",
                                                   "ctrl SPACE", "toggleSelectionPreserveAnchor",
                                                   "shift SPACE", "extendSelection",
                                                   "ctrl HOME", "selectFirstChangeLead",
                                                   "ctrl END", "selectLastChangeLead",
                                                   "ctrl UP", "selectPreviousChangeLead",
                                                   "ctrl KP_UP", "selectPreviousChangeLead",
                                                   "ctrl DOWN", "selectNextChangeLead",
                                                   "ctrl KP_DOWN", "selectNextChangeLead",
                                                   "ctrl PAGE_DOWN", "scrollDownChangeLead",
                                                   "ctrl shift PAGE_DOWN", "scrollDownExtendSelection",
                                                   "ctrl PAGE_UP", "scrollUpChangeLead",
                                                   "ctrl shift PAGE_UP", "scrollUpExtendSelection",
                                                   "ctrl LEFT", "scrollLeft",
                                                   "ctrl KP_LEFT", "scrollLeft",
                                                   "ctrl RIGHT", "scrollRight",
                                                   "ctrl KP_RIGHT", "scrollRight",
                                                   "ADD", "expand", "SUBTRACT", "collapse",
                                                   "SPACE", "toggleSelectionPreserveAnchor",
                                                   "shift ctrl SPACE", "moveSelectionTo"};
        Object[] listFocusInputMap = new Object[] {"ctrl C", "copy", "ctrl V", "paste",
                                                   "ctrl X", "cut", "COPY", "copy",
                                                   "PASTE", "paste", "CUT", "cut",
                                                   "UP", "selectPreviousRow", "KP_UP", "selectPreviousRow",
                                                   "shift UP", "selectPreviousRowExtendSelection",
                                                   "shift ctrl UP", "selectPreviousRowExtendSelection",
                                                   "shift KP_UP", "selectPreviousRowExtendSelection",
                                                   "shift ctrl KP_UP", "selectPreviousRowExtendSelection",
                                                   "DOWN", "selectNextRow", "KP_DOWN", "selectNextRow",
                                                   "ctrl DOWN", "selectNextRowChangeLead", "ctrl KP_DOWN", "selectNextRowChangeLead",
                                                   "shift DOWN", "selectNextRowExtendSelection",
                                                   "shift ctrl DOWN", "selectNextRowExtendSelection",
                                                   "shift KP_DOWN", "selectNextRowExtendSelection",
                                                   "shift ctrl KP_DOWN", "selectNextRowExtendSelection",
                                                   "LEFT", "selectPreviousColumn", "KP_LEFT", "selectPreviousColumn",
                                                   "ctrl LEFT", "selectPreviousColumnChangeLead", "ctrl KP_LEFT", "selectPreviousColumnChangeLead",
                                                   "shift LEFT", "selectPreviousColumnExtendSelection",
                                                   "shift ctrl LEFT", "selectPreviousColumnExtendSelection",
                                                   "shift KP_LEFT", "selectPreviousColumnExtendSelection",
                                                   "shift ctrl KP_LEFT", "selectPreviousColumnExtendSelection",
                                                   "RIGHT", "selectNextColumn", "KP_RIGHT", "selectNextColumn",
                                                   "ctrl RIGHT", "selectNextColumnChangeLead", "ctrl KP_RIGHT", "selectNextColumnChangeLead",
                                                   "shift RIGHT", "selectNextColumnExtendSelection",
                                                   "shift ctrl RIGHT", "selectNextColumnExtendSelection",
                                                   "shift KP_RIGHT", "selectNextColumnExtendSelection",
                                                   "shift ctrl KP_RIGHT", "selectNextColumnExtendSelection",
                                                   "HOME", "selectFirstRow", "ctrl HOME", "selectFirstRowChangeLead",
                                                   "shift HOME", "selectFirstRowExtendSelection", "shift ctrl HOME", "selectFirstRowExtendSelection",
                                                   "END", "selectLastRow", "ctrl END", "selectLastRowChangeLead",
                                                   "shift END", "selectLastRowExtendSelection", "shift ctrl END", "selectLastRowExtendSelection",
                                                   "PAGE_UP", "scrollUp", "ctrl PAGE_UP", "scrollUpChangeLead",
                                                   "shift PAGE_UP", "scrollUpExtendSelection", "shift ctrl PAGE_UP", "scrollUpExtendSelection",
                                                   "PAGE_DOWN", "scrollDown", "ctrl PAGE_DOWN", "scrollDownChangeLead",
                                                   "shift PAGE_DOWN", "scrollDownExtendSelection",
                                                   "shift ctrl PAGE_DOWN", "scrollDownExtendSelection",
                                                   "ctrl A", "selectAll",
                                                   "ctrl SLASH", "selectAll", "ctrl BACK_SLASH", "clearSelection",
                                                   "ctrl UP", "selectPreviousRowChangeLead",
                                                   "ctrl KP_UP", "selectPreviousRowChangeLead",
                                                   "SPACE", "addToSelection", "ctrl SPACE", "toggleAndAnchor",
                                                   "shift ctrl SPACE", "moveSelectionTo", "shift SPACE", "extendTo"};
        Object[] scrollBarAncestorInputMap = new Object[] {"RIGHT", "positiveUnitIncrement",
                                                        "KP_RIGHT", "positiveUnitIncrement",
                                                        "DOWN", "positiveUnitIncrement",
                                                        "KP_DOWN", "positiveUnitIncrement",
                                                        "PAGE_DOWN", "positiveBlockIncrement",
                                                        "LEFT", "negativeUnitIncrement",
                                                        "KP_LEFT", "negativeUnitIncrement",
                                                        "UP", "negativeUnitIncrement",
                                                        "KP_UP", "negativeUnitIncrement",
                                                        "PAGE_UP", "negativeBlockIncrement",
                                                        "HOME", "minScroll", "END", "maxScroll" };
        Object[] scrollPaneAncestorInputMap = new Object[] {"RIGHT", "unitScrollRight",
                                                            "KP_RIGHT", "unitScrollRight",
                                                            "DOWN", "unitScrollDown",
                                                            "KP_DOWN", "unitScrollDown",
                                                            "LEFT", "unitScrollLeft",
                                                            "KP_LEFT", "unitScrollLeft",
                                                            "UP", "unitScrollUp",
                                                            "KP_UP", "unitScrollUp",
                                                            "PAGE_UP", "scrollUp",
                                                            "PAGE_DOWN", "scrollDown",
                                                            "ctrl PAGE_UP", "scrollLeft",
                                                            "ctrl PAGE_DOWN", "scrollRight",
                                                            "ctrl HOME", "scrollHome", "ctrl END", "scrollEnd" };
        Object[] sliderFocusInputMap = new Object[] {"RIGHT", "positiveUnitIncrement",
                                                     "KP_RIGHT", "positiveUnitIncrement",
                                                     "DOWN", "negativeUnitIncrement",
                                                     "KP_DOWN", "negativeUnitIncrement",
                                                     "PAGE_DOWN", "negativeBlockIncrement",
                                                     "ctrl PAGE_DOWN", "negativeBlockIncrement",
                                                     "LEFT", "negativeUnitIncrement",
                                                     "KP_LEFT", "negativeUnitIncrement",
                                                     "UP", "positiveUnitIncrement",
                                                     "KP_UP", "positiveUnitIncrement",
                                                     "PAGE_UP", "positiveBlockIncrement",
                                                     "ctrl PAGE_UP", "positiveBlockIncrement",
                                                     "HOME", "minScroll", "END", "maxScroll" };
        Object[] splitPaneAncestorInputMap = new Object[] {"UP", "negativeIncrement",
                                                           "DOWN", "positiveIncrement",
                                                           "LEFT", "negativeIncrement",
                                                           "RIGHT", "positiveIncrement",
                                                           "KP_UP", "negativeIncrement",
                                                           "KP_DOWN", "positiveIncrement",
                                                           "KP_LEFT", "negativeIncrement",
                                                           "KP_RIGHT", "positiveIncrement",
                                                           "HOME", "selectMin", "END", "selectMax",
                                                           "F8", "startResize", "F6", "toggleFocus",
                                                           "ctrl TAB", "focusOutForward",
                                                           "ctrl shift TAB", "focusOutBackward" };
        Object[] tabbedPaneAncestorInputMap = new Object[] {"ctrl PAGE_DOWN", "navigatePageDown",
                                                            "ctrl PAGE_UP", "navigatePageUp",
                                                            "ctrl UP", "requestFocus",
                                                            "ctrl KP_UP", "requestFocus" };
        Object[] tabbedPaneFocusInputMap = new Object[] {"RIGHT", "navigateRight", "KP_RIGHT", "navigateRight",
                                                         "LEFT", "navigateLeft", "KP_LEFT", "navigateLeft",
                                                         "UP", "navigateUp", "KP_UP", "navigateUp",
                                                         "DOWN", "navigateDown", "KP_DOWN", "navigateDown",
                                                         "ctrl DOWN", "requestFocusForVisibleComponent",
                                                         "ctrl KP_DOWN", "requestFocusForVisibleComponent" };
        Object[] textFieldFocusInputMap = new Object[] {"ctrl C", DefaultEditorKit.copyAction,
                                                        "ctrl V", DefaultEditorKit.pasteAction,
                                                        "ctrl X", DefaultEditorKit.cutAction,
                                                        "COPY", DefaultEditorKit.copyAction,
                                                        "PASTE", DefaultEditorKit.pasteAction,
                                                        "CUT", DefaultEditorKit.cutAction,
                                                        "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                                                        "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
                                                        "shift RIGHT", DefaultEditorKit.selectionForwardAction,
                                                        "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
                                                        "ctrl LEFT", DefaultEditorKit.previousWordAction,
                                                        "ctrl KP_LEFT", DefaultEditorKit.previousWordAction,
                                                        "ctrl RIGHT", DefaultEditorKit.nextWordAction,
                                                        "ctrl KP_RIGHT", DefaultEditorKit.nextWordAction,
                                                        "ctrl shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
                                                        "ctrl shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
                                                        "ctrl shift RIGHT", DefaultEditorKit.selectionNextWordAction,
                                                        "ctrl shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
                                                        "ctrl A", DefaultEditorKit.selectAllAction,
                                                        "HOME", DefaultEditorKit.beginLineAction,
                                                        "END", DefaultEditorKit.endLineAction,
                                                        "shift HOME", DefaultEditorKit.selectionBeginLineAction,
                                                        "shift END", DefaultEditorKit.selectionEndLineAction,
                                                        "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                                                        "ctrl H", DefaultEditorKit.deletePrevCharAction,
                                                        "DELETE", DefaultEditorKit.deleteNextCharAction,
                                                        "RIGHT", DefaultEditorKit.forwardAction,
                                                        "LEFT", DefaultEditorKit.backwardAction,
                                                        "KP_RIGHT", DefaultEditorKit.forwardAction,
                                                        "KP_LEFT", DefaultEditorKit.backwardAction,
                                                        "ENTER", JTextField.notifyAction,
                                                        "ctrl BACK_SLASH", "unselect",
                                                        "control shift O", "toggle-componentOrientation" };
        Object[] toolBarAncestorInputMap = new Object[] {"UP", "navigateUp", "KP_UP", "navigateUp",
                                                         "DOWN", "navigateDown", "KP_DOWN", "navigateDown",
                                                         "LEFT", "navigateLeft", "KP_LEFT", "navigateLeft",
                                                         "RIGHT", "navigateRight", "KP_RIGHT", "navigateRight" };
        Object[] componentDefaults = new Object[] {
                 "AuditoryCues.defaultCueList", auditoryCuesDefaultCueList,
                 "Button.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders", "getButtonBorder"),
                 "Button.focusInputMap", new UIDefaults.LazyInputMap(new Object[] { "SPACE", "pressed", "released SPACE", "released" }),
                 "Button.font", controlTextFont,
                 "CheckBox.focusInputMap", new UIDefaults.LazyInputMap(new Object[] { "SPACE", "pressed", "released SPACE", "released" }),
                 "CheckBox.font", controlTextFont,
                 "CheckBox.icon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getCheckBoxIcon"),
                 "CheckBoxMenuItem.acceleratorFont", subTextFont,
                 "CheckBoxMenuItem.arrowIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getMenuItemArrowIcon"),
                 "CheckBoxMenuItem.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders$MenuItemBorder"),
                 "CheckBoxMenuItem.borderPainted", Boolean.TRUE,
                 "CheckBoxMenuItem.checkIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getCheckBoxMenuItemIcon"),
                 "CheckBoxMenuItem.commandSound", "sounds/MenuItemCommand.wav",
                 "CheckBoxMenuItem.font", menuTextFont,
                 "ComboBox.ancestorInputMap", new UIDefaults.LazyInputMap(comboBoxAncestorInputMap),
                 "ComboBox.font", controlTextFont,
                 "Desktop.ancestorInputMap", new UIDefaults.LazyInputMap(desktopAncestorInputMap),
                 "DesktopIcon.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders", "getDesktopIconBorder"),
                 "DesktopIcon.font", controlTextFont,
                 "DesktopIcon.width", new Integer(160),
                 "EditorPane.focusInputMap", new UIDefaults.LazyInputMap(editorPaneFocusInputMap),
                 "EditorPane.font", userTextFont,
                 "FileChooser.ancestorInputMap", new UIDefaults.LazyInputMap(new Object[] { "ESCAPE", "cancelSelection", "BACK_SPACE", "Go Up", "ENTER", "approveSelection" }),
                 "FileChooser.detailsViewIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getFileChooserDetailViewIcon"),
                 "FileChooser.homeFolderIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getFileChooserHomeFolderIcon"),
                 "FileChooser.listViewIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getFileChooserListViewIcon"),
                 "FileChooser.newFolderIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getFileChooserNewFolderIcon"),
                 "FileChooser.upFolderIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getFileChooserUpFolderIcon"),
                 "FileView.computerIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getTreeComputerIcon"),
                 "FileView.directoryIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getTreeFolderIcon"),
                 "FileView.fileIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getTreeLeafIcon"),
                 "FileView.floppyDriveIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getTreeFloppyDriveIcon"),
                 "FileView.hardDriveIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getTreeHardDriveIcon"),
                 "FormattedTextField.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders", "getTextFieldBorder"),
                 "FormattedTextField.focusInputMap", new UIDefaults.LazyInputMap(formattedTextFieldFocusInputMap),
                 "FormattedTextField.font", userTextFont,
                 "InternalFrame.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders$InternalFrameBorder"),
                 "InternalFrame.closeIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getInternalFrameCloseIcon", new Object[] { new Integer(16) }),
                 "InternalFrame.closeSound", "sounds/FrameClose.wav",
                 "InternalFrame.icon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getInternalFrameDefaultMenuIcon"),
                 "InternalFrame.iconifyIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getInternalFrameMinimizeIcon", new Object[] { new Integer(16) }),
                 "InternalFrame.maximizeIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getInternalFrameMaximizeIcon", new Object[] { new Integer(16) }),
                 "InternalFrame.maximizeSound", "sounds/FrameMaximize.wav",
                 "InternalFrame.minimizeIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getInternalFrameAltMaximizeIcon", new Object[] { new Integer(16) }),
                 "InternalFrame.minimizeSound", "sounds/FrameMinimize.wav",
                 "InternalFrame.optionDialogBorder", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders$OptionDialogBorder"),
                 "InternalFrame.paletteBorder", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders$PaletteBorder"),
                 "InternalFrame.paletteCloseIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory$PaletteCloseIcon"),
                 "InternalFrame.paletteTitleHeight", new Integer(12),
                 "InternalFrame.restoreDownSound", "sounds/FrameRestoreDown.wav",
                 "InternalFrame.restoreUpSound", "sounds/FrameRestoreUp.wav",
                 "InternalFrame.titleFont", windowTitleFont,
                 "Label.font", controlTextFont,
                 "List.focusCellHighlightBorder", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource$LineBorderUIResource", new Object[] { getPrimaryControlShadow() }),
                 "List.focusInputMap", new UIDefaults.LazyInputMap(listFocusInputMap),
                 "List.font", controlTextFont,
                 "Menu.acceleratorFont", subTextFont,
                 "Menu.arrowIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getMenuArrowIcon"),
                 "Menu.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders$MenuItemBorder"),
                 "Menu.borderPainted", Boolean.TRUE,
                 "Menu.checkIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getMenuItemCheckIcon"),
                 "Menu.font", menuTextFont,
                 "Menu.menuPopupOffsetX", new Integer(0),
                 "Menu.menuPopupOffsetY", new Integer(0),
                 "Menu.submenuPopupOffsetX", new Integer(-5),
                 "Menu.submenuPopupOffsetY", new Integer(-2),
                 "MenuBar.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders$MenuBarBorder"),
                 "MenuBar.font", menuTextFont,
                 "MenuBar.windowBindings", new Object[] { "F10", "takeFocus" },
                 "MenuItem.acceleratorDelimiter", "-",
                 "MenuItem.acceleratorFont", subTextFont,
                 "MenuItem.arrowIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getMenuItemArrowIcon"),
                 "MenuItem.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders$MenuItemBorder"),
                 "MenuItem.borderPainted", Boolean.TRUE,
                 "MenuItem.checkIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getMenuItemCheckIcon"),
                 "MenuItem.commandSound", "sounds/MenuItemCommand.wav",
                 "MenuItem.font", menuTextFont,
                 "OptionPane.errorSound", "sounds/OptionPaneError.wav",
                 "OptionPane.informationSound", "sounds/OptionPaneInformation.wav",
                 "OptionPane.questionSound", "sounds/OptionPaneQuestion.wav",
                 "OptionPane.warningSound", "sounds/OptionPaneWarning.wav",
                 "OptionPane.windowBindings", new Object[] { "ESCAPE", "close" },
                 "OptionPane.errorIcon", makeIcon(MetalLookAndFeel.class, "icons/Error.gif"),
                 "OptionPane.informationIcon", makeIcon(MetalLookAndFeel.class, "icons/Inform.gif"),
                 "OptionPane.questionIcon", makeIcon(MetalLookAndFeel.class, "icons/Question.gif"),
                 "OptionPane.warningIcon", makeIcon(MetalLookAndFeel.class, "icons/Warn.gif"),
                 "PasswordField.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders", "getTextBorder"),
                 "PasswordField.focusInputMap", new UIDefaults.LazyInputMap(textFieldFocusInputMap),
                 "PasswordField.font", userTextFont,
                 "PopupMenu.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders$PopupMenuBorder"),
                 "PopupMenu.font", menuTextFont,
                 "PopupMenu.popupSound", "sounds/PopupMenuPopup.wav",
                 "ProgressBar.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource$LineBorderUIResource", new Object[] { getControlDarkShadow(), new Integer(1) }),
                 "ProgressBar.cellLength", new Integer(3),
                 "ProgressBar.cellSpacing", new Integer(2),
                 "ProgressBar.font", controlTextFont,
                 "RadioButton.focusInputMap", new UIDefaults.LazyInputMap(new Object[] { "SPACE", "pressed", "released SPACE", "released" }),
                 "RadioButton.font", controlTextFont,
                 "RadioButton.icon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getRadioButtonIcon"),
                 "RadioButtonMenuItem.acceleratorFont", subTextFont,
                 "RadioButtonMenuItem.arrowIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getMenuItemArrowIcon"),
                 "RadioButtonMenuItem.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders$MenuItemBorder"),
                 "RadioButtonMenuItem.borderPainted", Boolean.TRUE,
                 "RadioButtonMenuItem.checkIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getRadioButtonMenuItemIcon"),
                 "RadioButtonMenuItem.commandSound", "sounds/MenuItemCommand.wav",
                 "RadioButtonMenuItem.font", menuTextFont,
                 "RootPane.colorChooserDialogBorder", new MetalLazyValue("javax.swing.plaf.metal.MetalBorders$QuestionDialogBorder"),
                 "RootPane.defaultButtonWindowKeyBindings", new Object[] { "ENTER", "press", "released ENTER", "release", "ctrl ENTER", "press", "ctrl released ENTER", "release" },
                 "RootPane.errorDialogBorder", new MetalLazyValue("javax.swing.plaf.metal.MetalBorders$ErrorDialogBorder"),
                 "RootPane.fileChooserDialogBorder", new MetalLazyValue("javax.swing.plaf.metal.MetalBorders$QuestionDialogBorder"),
                 "RootPane.frameBorder", new MetalLazyValue("javax.swing.plaf.metal.MetalBorders$FrameBorder"),
                 "RootPane.informationDialogBorder", new MetalLazyValue("javax.swing.plaf.metal.MetalBorders$DialogBorder"),
                 "RootPane.plainDialogBorder", new MetalLazyValue("javax.swing.plaf.metal.MetalBorders$DialogBorder"),
                 "RootPane.questionDialogBorder", new MetalLazyValue("javax.swing.plaf.metal.MetalBorders$QuestionDialogBorder"),
                 "RootPane.warningDialogBorder", new MetalLazyValue("javax.swing.plaf.metal.MetalBorders$WarningDialogBorder"),
                 "ScrollBar.allowsAbsolutePositioning", Boolean.TRUE,
                 "ScrollBar.ancestorInputMap", new UIDefaults.LazyInputMap(scrollBarAncestorInputMap),
                 "ScrollBar.width", new Integer(17),
                 "ScrollPane.ancestorInputMap", new UIDefaults.LazyInputMap(scrollPaneAncestorInputMap),
                 "ScrollPane.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders$ScrollPaneBorder"),
                 "Slider.focusInputMap", new UIDefaults.LazyInputMap(sliderFocusInputMap),
                 "Slider.focusInsets", new InsetsUIResource(0, 0, 0, 0),
                 "Slider.horizontalThumbIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getHorizontalSliderThumbIcon"),
                 "Slider.majorTickLength", new Integer(6),
                 "Slider.trackWidth", new Integer(5),
                 "Slider.verticalThumbIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getVerticalSliderThumbIcon"),
                 "Spinner.ancestorInputMap", new UIDefaults.LazyInputMap(new Object[] { "UP", "increment", "KP_UP", "increment", "DOWN", "decrement", "KP_DOWN", "decrement" }),
                 "Spinner.arrowButtonBorder", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders", "getButtonBorder"),
                 "Spinner.arrowButtonInsets", new InsetsUIResource(0, 0, 0, 0),
                 "Spinner.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders", "getTextFieldBorder"),
                 "Spinner.font", controlTextFont,
                 "SplitPane.ancestorInputMap", new UIDefaults.LazyInputMap(splitPaneAncestorInputMap),
                 "SplitPane.dividerSize", new Integer(10),
                 "TabbedPane.ancestorInputMap", new UIDefaults.LazyInputMap(tabbedPaneAncestorInputMap),
                 "TabbedPane.focusInputMap", new UIDefaults.LazyInputMap(tabbedPaneFocusInputMap),
                 "TabbedPane.font", controlTextFont,
                 "TabbedPane.tabAreaInsets", new InsetsUIResource(4, 2, 0, 6),
                 "TabbedPane.tabInsets", new InsetsUIResource(0, 9, 1, 9),
                 "Table.ancestorInputMap", new UIDefaults.LazyInputMap(tableAncestorInputMap),
                 "Table.focusCellHighlightBorder", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource$LineBorderUIResource", new Object[] { getPrimaryControlShadow() }),
                 "Table.font", userTextFont,
                 "Table.scrollPaneBorder", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders$ScrollPaneBorder"),
                 "TableHeader.cellBorder", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders$TableHeaderBorder"),
                 "TableHeader.font", userTextFont,
                 "TextArea.focusInputMap", new UIDefaults.LazyInputMap(editorPaneFocusInputMap),
                 "TextArea.font", userTextFont,
                 "TextField.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders", "getTextFieldBorder"),
                 "TextField.focusInputMap", new UIDefaults.LazyInputMap(textFieldFocusInputMap),
                 "TextField.font", userTextFont,
                 "TextPane.focusInputMap", new UIDefaults.LazyInputMap(editorPaneFocusInputMap),
                 "TextPane.font", userTextFont,
                 "TitledBorder.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource$LineBorderUIResource", new Object[] { getControlShadow() }),
                 "TitledBorder.font", controlTextFont,
                 "ToggleButton.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders", "getToggleButtonBorder"),
                 "ToggleButton.focusInputMap", new UIDefaults.LazyInputMap(new Object[] { "SPACE", "pressed", "released SPACE", "released" }),
                 "ToggleButton.font", controlTextFont,
                 "ToolBar.ancestorInputMap", new UIDefaults.LazyInputMap(toolBarAncestorInputMap),
                 "ToolBar.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalBorders$ToolBarBorder"),
                 "ToolBar.font", menuTextFont,
                 "ToolTip.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource$LineBorderUIResource", new Object[] { getPrimaryControlDarkShadow() }),
                 "ToolTip.borderInactive", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource$LineBorderUIResource", new Object[] { getControlDarkShadow() }),
                 "ToolTip.font", systemTextFont,
                 "ToolTip.hideAccelerator", Boolean.FALSE,
                 "Tree.ancestorInputMap", new UIDefaults.LazyInputMap(new Object[] { "ESCAPE", "cancel" }),
                 "Tree.closedIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getTreeFolderIcon"),
                 "Tree.collapsedIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getTreeControlIcon", new Object[] { Boolean.TRUE }),
                 "Tree.expandedIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getTreeControlIcon", new Object[] { Boolean.FALSE }),
                 "Tree.focusInputMap", new UIDefaults.LazyInputMap(treeFocusInputMap),
                 "Tree.font", userTextFont,
                 "Tree.leafIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getTreeLeafIcon"),
                 "Tree.openIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.metal.MetalIconFactory", "getTreeFolderIcon"),
                 "Tree.rowHeight", new Integer(0),

        };

        super.initComponentDefaults(uiDefs);

        uiDefs.putDefaults(componentColorDefaults);
        uiDefs.putDefaults(componentDefaults);
    }

    /**
     * Initialize class default values
     * @param uiDefs defaults table
     */
    protected void initClassDefaults(final UIDefaults uiDefs) {
        super.initClassDefaults(uiDefs);

        Object[] classDefaults = new Object[] {
                "SliderUI", "javax.swing.plaf.metal.MetalSliderUI",
                "ComboBoxUI", "javax.swing.plaf.metal.MetalComboBoxUI",
                "SplitPaneUI", "javax.swing.plaf.metal.MetalSplitPaneUI",
                "LabelUI", "javax.swing.plaf.metal.MetalLabelUI",
                "FileChooserUI", "javax.swing.plaf.metal.MetalFileChooserUI",
                "ScrollPaneUI", "javax.swing.plaf.metal.MetalScrollPaneUI",
                "SeparatorUI", "javax.swing.plaf.metal.MetalSeparatorUI",
                "RootPaneUI", "javax.swing.plaf.metal.MetalRootPaneUI",
                "ToolBarUI", "javax.swing.plaf.metal.MetalToolBarUI",
                "CheckBoxUI", "javax.swing.plaf.metal.MetalCheckBoxUI",
                "ToggleButtonUI", "javax.swing.plaf.metal.MetalToggleButtonUI",
                "TabbedPaneUI", "javax.swing.plaf.metal.MetalTabbedPaneUI",
                "InternalFrameUI", "javax.swing.plaf.metal.MetalInternalFrameUI",
                "PopupMenuSeparatorUI", "javax.swing.plaf.metal.MetalPopupMenuSeparatorUI",
                "ToolTipUI", "javax.swing.plaf.metal.MetalToolTipUI",
                "ProgressBarUI", "javax.swing.plaf.metal.MetalProgressBarUI",
                "TextFieldUI", "javax.swing.plaf.metal.MetalTextFieldUI",
                "TreeUI", "javax.swing.plaf.metal.MetalTreeUI",
                "ScrollBarUI", "javax.swing.plaf.metal.MetalScrollBarUI",
                "RadioButtonUI", "javax.swing.plaf.metal.MetalRadioButtonUI",
                "DesktopIconUI", "javax.swing.plaf.metal.MetalDesktopIconUI",
                "ButtonUI", "javax.swing.plaf.metal.MetalButtonUI"
        };

        uiDefs.putDefaults(classDefaults);
    }

    /**
     * Returns the table with defaults values
     * @return UIDefaults result
     */
    public UIDefaults getDefaults() {
        createDefaultTheme();
        UIDefaults result = super.getDefaults();
        metalTheme.addCustomEntriesToTable(result);
        result.addResourceBundle(METAL_RESOURCE_BUNDLE);

        return result;
    }

    /**
     * Return name of this Look and Feel
     * @return String name
     */
    public String getName() {
        return "Metal L&F";
    }

    /**
     * Return Look and Feel id
     * @return String id
     */
    public String getID() {
        return "Metal";
    }

    /**
     * Return description of this Look and Feel
     * @return String description
     */
    public String getDescription() {
        return "Metal L&F";
    }

    /**
     * Invoked on invalid operation
     */
    public void provideErrorFeedback(final Component c) {
        super.provideErrorFeedback(c);
    }

    /**
     * Return true because this Look and Feel is supported for all platforms
     * @return boolean true
     */
    public boolean isSupportedLookAndFeel() {
        return true;
    }

    /**
     * Return false because this is not native look and feel
     * @return boolean false
     */
    public boolean isNativeLookAndFeel() {
        return false;
    }

    /**
     * Return true due RootPaneUI instances support Window decorations in JRootPane
     * @return boolean true
     */
    public boolean getSupportsWindowDecorations() {
        return true;
    }

    /**
     * Initialize default metal theme
     */
    protected void createDefaultTheme() {
        if (metalTheme == null) {
            metalTheme = new DefaultMetalTheme();
        }
    }

    /**
     * Set the theme
     */
    public static void setCurrentTheme(final MetalTheme theme) {
        metalTheme = theme;
    }

    /**
     * Return window title font
     * @return FontUIResource font
     */
    public static FontUIResource getWindowTitleFont() {
        return metalTheme.getWindowTitleFont();
    }

    /**
     * Return user text font
     * @return FontUIResource font
     */
    public static FontUIResource getUserTextFont() {
        return metalTheme.getUserTextFont();
    }

    /**
     * Return system text font
     * @return FontUIResource font
     */
    public static FontUIResource getSystemTextFont() {
        return metalTheme.getSystemTextFont();
    }

    /**
     * Return sub text font
     * @return FontUIResource font
     */
    public static FontUIResource getSubTextFont() {
        return metalTheme.getSubTextFont();
    }

    /**
     * Return menu text font
     * @return FontUIResource font
     */
    public static FontUIResource getMenuTextFont() {
        return metalTheme.getMenuTextFont();
    }

    /**
     * Return control text font
     * @return FontUIResource font
     */
    public static FontUIResource getControlTextFont() {
        return metalTheme.getControlTextFont();
    }

    /**
     * Return window title font
     * @return FontUIResource font
     */
    public static ColorUIResource getWindowTitleInactiveForeground() {
        return metalTheme.getWindowTitleInactiveForeground();
    }

    /**
     * Return window title inactive background color
     * @return ColorUIResource color
     */
    public static ColorUIResource getWindowTitleInactiveBackground() {
        return metalTheme.getWindowTitleInactiveBackground();
    }

    /**
     * Return window title foreround color
     * @return ColorUIResource color
     */
    public static ColorUIResource getWindowTitleForeground() {
        return metalTheme.getWindowTitleForeground();
    }

    /**
     * Return window title background color
     * @return ColorUIResource color
     */
    public static ColorUIResource getWindowTitleBackground() {
        return metalTheme.getWindowTitleBackground();
    }

    /**
     * Return window background color
     * @return ColorUIResource color
     */
    public static ColorUIResource getWindowBackground() {
        return metalTheme.getWindowBackground();
    }

    /**
     * Return white color
     * @return ColorUIResource color
     */
    public static ColorUIResource getWhite() {
        return metalTheme.getWhite();
    }

    /**
     * Return user text color
     * @return ColorUIResource color
     */
    public static ColorUIResource getUserTextColor() {
        return metalTheme.getUserTextColor();
    }

    /**
     * Return text highlight color
     * @return ColorUIResource color
     */
    public static ColorUIResource getTextHighlightColor() {
        return metalTheme.getTextHighlightColor();
    }

    /**
     * Return system text color
     * @return ColorUIResource color
     */
    public static ColorUIResource getSystemTextColor() {
        return metalTheme.getSystemTextColor();
    }

    /**
     * Return separator foreground color
     * @return ColorUIResource color
     */
    public static ColorUIResource getSeparatorForeground() {
        return metalTheme.getSeparatorForeground();
    }

    /**
     * Return separator background color
     * @return ColorUIResource color
     */
    public static ColorUIResource getSeparatorBackground() {
        return metalTheme.getSeparatorBackground();
    }

    /**
     * Return primary control shadow color
     * @return ColorUIResource color
     */
    public static ColorUIResource getPrimaryControlShadow() {
        return metalTheme.getPrimaryControlShadow();
    }

    /**
     * Return primary control info color
     * @return ColorUIResource color
     */
    public static ColorUIResource getPrimaryControlInfo() {
        return metalTheme.getPrimaryControlInfo();
    }

    /**
     * Return primary control highlight color
     * @return ColorUIResource color
     */
    public static ColorUIResource getPrimaryControlHighlight() {
        return metalTheme.getPrimaryControlHighlight();
    }

    /**
     * Return primary control dark shadow color
     * @return ColorUIResource color
     */
    public static ColorUIResource getPrimaryControlDarkShadow() {
        return metalTheme.getPrimaryControlDarkShadow();
    }

    /**
     * Return primary control color
     * @return ColorUIResource color
     */
    public static ColorUIResource getPrimaryControl() {
        return metalTheme.getPrimaryControl();
    }

    /**
     * Return menu selected foreground color
     * @return ColorUIResource color
     */
    public static ColorUIResource getMenuSelectedForeground() {
        return metalTheme.getMenuSelectedForeground();
    }

    /**
     * Return menu selected background color
     * @return ColorUIResource color
     */
    public static ColorUIResource getMenuSelectedBackground() {
        return metalTheme.getMenuSelectedBackground();
    }

    /**
     * Return menu foreground color
     * @return ColorUIResource color
     */
    public static ColorUIResource getMenuForeground() {
        return metalTheme.getMenuForeground();
    }

    /**
     * Return menu disabled foreground color
     * @return ColorUIResource color
     */
    public static ColorUIResource getMenuDisabledForeground() {
        return metalTheme.getMenuDisabledForeground();
    }

    /**
     * Return menu background color
     * @return ColorUIResource color
     */
    public static ColorUIResource getMenuBackground() {
        return metalTheme.getMenuBackground();
    }

    /**
     * Return inactive system text color
     * @return ColorUIResource color
     */
    public static ColorUIResource getInactiveSystemTextColor() {
        return metalTheme.getInactiveSystemTextColor();
    }

    /**
     * Return inactive control text color
     * @return ColorUIResource color
     */
    public static ColorUIResource getInactiveControlTextColor() {
        return metalTheme.getInactiveControlTextColor();
    }

    /**
     * Return highlighted text color
     * @return ColorUIResource color
     */
    public static ColorUIResource getHighlightedTextColor() {
        return metalTheme.getHighlightedTextColor();
    }

    /**
     * Return focus color
     * @return ColorUIResource color
     */
    public static ColorUIResource getFocusColor() {
        return metalTheme.getFocusColor();
    }

    /**
     * Return desktop color
     * @return ColorUIResource color
     */
    public static ColorUIResource getDesktopColor() {
        return metalTheme.getDesktopColor();
    }

    /**
     * Return control text color
     * @return ColorUIResource color
     */
    public static ColorUIResource getControlTextColor() {
        return metalTheme.getControlTextColor();
    }

    /**
     * Return control shadow color
     * @return ColorUIResource color
     */
    public static ColorUIResource getControlShadow() {
        return metalTheme.getControlShadow();
    }

    /**
     * Return control info color
     * @return ColorUIResource color
     */
    public static ColorUIResource getControlInfo() {
        return metalTheme.getControlInfo();
    }

    /**
     * Return control highlight color
     * @return ColorUIResource color
     */
    public static ColorUIResource getControlHighlight() {
        return metalTheme.getControlHighlight();
    }

    /**
     * Return control disabled color
     * @return ColorUIResource color
     */
    public static ColorUIResource getControlDisabled() {
        return metalTheme.getControlDisabled();
    }

    /**
     * Return control dark shadow color
     * @return ColorUIResource color
     */
    public static ColorUIResource getControlDarkShadow() {
        return metalTheme.getControlDarkShadow();
    }

    /**
     * Return control color
     * @return ColorUIResource color
     */
    public static ColorUIResource getControl() {
        return metalTheme.getControl();
    }
    
    /**
     * Return black color
     * @return ColorUIResource color
     */
    public static ColorUIResource getBlack() {
        return metalTheme.getBlack();
    }

    /**
     * Return accelerator selected foreground color
     * @return ColorUIResource color
     */
    public static ColorUIResource getAcceleratorSelectedForeground() {
        return metalTheme.getAcceleratorSelectedForeground();
    }

    /**
     * Return accelerator foreground color
     * @return ColorUIResource color
     */
    public static ColorUIResource getAcceleratorForeground() {
        return metalTheme.getAcceleratorForeground();
    }

    public static MetalTheme getCurrentTheme() {
        return metalTheme;
    }
    
    /**
     * Return system text font active value
     * @return ActiveValue font
     */
    private static ActiveValue systemTextFontActiveValue() {
        return new UIDefaults.ActiveValue() {
            public Object createValue(final UIDefaults uiDefaults) {
                return getSystemTextFont();
            }
        };
    }

    /**
     * Return window title font active value
     * @return ActiveValue font
     */
    private static ActiveValue windowTitleFontActiveValue() {
        return new UIDefaults.ActiveValue() {
            public Object createValue(final UIDefaults uiDefaults) {
                return getWindowTitleFont();
            }
        };
    }

    /**
     * Return sub text font active value
     * @return ActiveValue font
     */
    private static ActiveValue subTextFontActiveValue() {
        return new UIDefaults.ActiveValue() {
            public Object createValue(final UIDefaults uiDefaults) {
                return getSubTextFont();
            }
        };
    }

    /**
     * Return user text font active value
     * @return ActiveValue font
     */
    private static ActiveValue userTextFontActiveValue() {
        return new UIDefaults.ActiveValue() {
            public Object createValue(final UIDefaults uiDefaults) {
                return getUserTextFont();
            }
        };
    }

    /**
     * Return menu text font active value
     * @return ActiveValue font
     */
    private static ActiveValue menuTextFontActiveValue() {
        return new UIDefaults.ActiveValue() {
            public Object createValue(final UIDefaults uiDefaults) {
                return getMenuTextFont();
            }
        };
    }

    /**
     * Return control text font active value
     * @return ActiveValue font
     */
    private static ActiveValue controlTextFontActiveValue() {
        return new UIDefaults.ActiveValue() {
            public Object createValue(final UIDefaults uiDefaults) {
                return getControlTextFont();
            }
        };
    }

    final class MetalLazyValue implements UIDefaults.LazyValue {
        private String className;
        private String methodName;

        public MetalLazyValue(final String className, final String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        public MetalLazyValue(final String className) {
            this(className, null);
        }

        public Object createValue(final UIDefaults uiDefaults) {
            Object result = null;

            try {
                Class classObj = Class.forName(className);
                result = (methodName == null)
                        ? classObj.newInstance()
                        : classObj.getMethod(methodName).invoke(null);
            } catch (final Exception e) {
                e.printStackTrace();
            }

            return result;
        }
    }
}
