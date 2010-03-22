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

package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.Serializable;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UIDefaults.ProxyLazyValue;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.text.DefaultEditorKit;

/*
 * In order not to break client application UI appearence the values for insets,
 * borders and other size-related keys as well as 'feel'-related values are preserved as in 1.5 release.
 * Color-related values are chosen to provide consistent look of UI components.
 */
public abstract class BasicLookAndFeel extends LookAndFeel implements Serializable {
    private static final String RESOURCE_BUNDLE = "org.apache.harmony.x.swing.plaf.resources.basic.BasicResourceBundle";
    private static final Integer BLINK_RATE = new Integer(600);
    private UIDefaults uiDefaults;

    protected void loadSystemColors(final UIDefaults uiDefs, final String[] customColors, final boolean isNative) {
        if (isNative) {
            loadSystemColors(uiDefs, getBasicSystemColors());
        } else {
            loadColors(uiDefs, customColors);
        }
    }

    protected void initSystemColorDefaults(final UIDefaults uiDefs) {
        Object[] basicSystemColors = getBasicSystemColors();

        for (int i = 0; i < basicSystemColors.length; i += 2) {
            uiDefs.put(basicSystemColors[i], new ColorUIResource((SystemColor)basicSystemColors[i + 1]));
        }
    }

    protected void initComponentDefaults(final UIDefaults uiDefs) {
        Object[] treeFocusInputMap = new Object[] {"ctrl C", "copy", "ctrl V", "paste",
                                                   "ctrl X", "cut", "COPY", "copy", "PASTE", "paste",
                                                   "CUT", "cut", "UP", "selectPrevious",
                                                   "KP_UP", "selectPrevious",
                                                   "shift UP", "selectPreviousExtendSelection",
                                                   "shift ctrl UP", "selectPreviousExtendSelection",
                                                   "shift KP_UP", "selectPreviousExtendSelection",
                                                   "shift ctrl KP_UP", "selectPreviousExtendSelection",
                                                   "DOWN", "selectNext", "KP_DOWN", "selectNext",
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
                                                   "HOME", "selectFirst",
                                                   "shift HOME", "selectFirstExtendSelection",
                                                   "shift ctrl HOME", "selectFirstExtendSelection",
                                                   "END", "selectLast",
                                                   "shift END", "selectLastExtendSelection",
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
                                                   "ctrl LEFT", "scrollLeft", "ctrl KP_LEFT", "scrollLeft",
                                                   "ctrl RIGHT", "scrollRight",
                                                   "ctrl KP_RIGHT", "scrollRight",
                                                   "ADD", "expand", "SUBTRACT", "collapse",
                                                   "SPACE", "toggleSelectionPreserveAnchor",
                                                   "shift ctrl SPACE", "moveSelectionTo"};
        Object[] scrollBarAncestorInputMapRightToLeft = new Object[] {"RIGHT", "negativeUnitIncrement",
                                                                   "KP_RIGHT", "negativeUnitIncrement",
                                                                   "LEFT", "positiveUnitIncrement",
                                                                   "KP_LEFT", "positiveUnitIncrement" };
        Object[] tabbedPaneAncestorInputMap = new Object[] {"ctrl TAB", "navigateNext",
                                                            "ctrl shift TAB", "navigatePrevious",
                                                            "ctrl PAGE_DOWN", "navigatePageDown",
                                                            "ctrl PAGE_UP", "navigatePageUp",
                                                            "ctrl UP", "requestFocus",
                                                            "ctrl KP_UP", "requestFocus" };
        Object[] splitPaneAncestorInputMap = new Object[] {"UP", "negativeIncrement", "DOWN", "positiveIncrement",
                                                           "LEFT", "negativeIncrement", "RIGHT", "positiveIncrement",
                                                           "KP_UP", "negativeIncrement", "KP_DOWN", "positiveIncrement",
                                                           "KP_LEFT", "negativeIncrement", "KP_RIGHT", "positiveIncrement",
                                                           "HOME", "selectMin", "END", "selectMax",
                                                           "F8", "startResize", "F6", "toggleFocus",
                                                           "ctrl TAB", "focusOutForward", "ctrl shift TAB", "focusOutBackward" };
        Object[] auditoryCuesAllAuditoryCues = new Object[] { "CheckBoxMenuItem.commandSound", "InternalFrame.closeSound",
                                                              "InternalFrame.maximizeSound", "InternalFrame.minimizeSound",
                                                              "InternalFrame.restoreDownSound",
                                                              "InternalFrame.restoreUpSound", "MenuItem.commandSound",
                                                              "OptionPane.errorSound", "OptionPane.informationSound",
                                                              "OptionPane.questionSound", "OptionPane.warningSound",
                                                              "PopupMenu.popupSound", "RadioButtonMenuItem.commandSound" };
        Object[] auditoryCuesCueList = new Object[] { "CheckBoxMenuItem.commandSound", "InternalFrame.closeSound",
                                                      "InternalFrame.maximizeSound", "InternalFrame.minimizeSound",
                                                      "InternalFrame.restoreDownSound",
                                                      "InternalFrame.restoreUpSound", "MenuItem.commandSound",
                                                      "OptionPane.errorSound", "OptionPane.informationSound",
                                                      "OptionPane.questionSound", "OptionPane.warningSound",
                                                      "PopupMenu.popupSound", "RadioButtonMenuItem.commandSound" };
        Object[] tableAncestorInputMapRightToLeft = new Object[] {"RIGHT", "selectPreviousColumn", "KP_RIGHT", "selectPreviousColumn",
                                                                  "ctrl RIGHT", "selectPreviousColumnChangeLead", "ctrl KP_RIGHT", "selectPreviousColumnChangeLead",
                                                                  "LEFT", "selectNextColumn", "KP_LEFT", "selectNextColumn",
                                                                  "ctrl LEFT", "selectNextColumnChangeLead", "ctrl KP_LEFT", "selectNextColumnChangeLead",
                                                                  "shift RIGHT", "selectPreviousColumnExtendSelection",
                                                                  "shift ctrl RIGHT", "selectPreviousColumnExtendSelection",
                                                                  "shift KP_RIGHT", "selectPreviousColumnExtendSelection",
                                                                  "shift ctrl KP_RIGHT", "selectPreviousColumnExtendSelection",
                                                                  "shift LEFT", "selectNextColumnExtendSelection",
                                                                  "shift ctrl LEFT", "selectNextColumnExtendSelection",
                                                                  "shift KP_LEFT", "selectNextColumnExtendSelection",
                                                                  "shift ctrl KP_LEFT", "selectNextColumnExtendSelection",
                                                                  "ctrl PAGE_UP", "scrollRightChangeSelection",
                                                                  "ctrl PAGE_DOWN", "scrollLeftChangeSelection",
                                                                  "ctrl shift PAGE_UP", "scrollRightExtendSelection",
                                                                  "ctrl shift PAGE_DOWN", "scrollLeftExtendSelection" };
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
        Object[] radioButtonFocusInputMap = new Object[] {"SPACE", "pressed",
                                                          "released SPACE", "released",
                                                          "RETURN", "pressed" };
        Object[] scrollPaneAncestorInputMapRightToLeft = new Object[] {"ctrl PAGE_UP", "scrollRight",
                                                                       "ctrl PAGE_DOWN", "scrollLeft" };
        Object[] toggleButtonFocusInputMap = new Object[] {"SPACE", "pressed",
                                                           "released SPACE", "released" };
        Object[] treeFocusInputMapRightToLeft = new Object[] {"RIGHT", "selectParent",
                                                              "KP_RIGHT", "selectParent",
                                                              "LEFT", "selectChild",
                                                              "KP_LEFT", "selectChild" };
        Object[] popupMenuSelectedWindowInputMapBindings = new Object[] { "ESCAPE", "cancel",
                                                                          "DOWN", "selectNext",
                                                                          "KP_DOWN", "selectNext",
                                                                          "UP", "selectPrevious",
                                                                          "KP_UP", "selectPrevious",
                                                                          "LEFT", "selectParent",
                                                                          "KP_LEFT", "selectParent",
                                                                          "RIGHT", "selectChild",
                                                                          "KP_RIGHT", "selectChild",
                                                                          "ENTER", "return",
                                                                          "SPACE", "return" };
        Object[] spinnerAncestorInputMap = new Object[] {"UP", "increment",
                                                         "KP_UP", "increment",
                                                         "DOWN", "decrement",
                                                         "KP_DOWN", "decrement" };
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
                                                        "HOME", "minScroll",
                                                        "END", "maxScroll" };
        Object[] rootPaneAncestorInputMap = new Object[] {"shift F10", "postPopup"};
        Object[] tableAncestorInputMap = new Object[] {"ctrl C", "copy", "ctrl V", "paste", "ctrl X", "cut",
                                                       "COPY", "copy", "PASTE", "paste",
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
                                                       "ESCAPE", "cancel", "F2", "startEditing",
                                                       "SPACE", "addToSelection", "ctrl SPACE", "toggleAndAnchor",
                                                       "shift ctrl SPACE", "moveSelectionTo", "shift SPACE", "extendTo"};
        Object[] listFocusInputMapRightToLeft = new Object[] {"LEFT", "selectNextColumn", "KP_LEFT", "selectNextColumn",
                                                              "ctrl LEFT", "selectNextColumnChangeLead", "ctrl KP_LEFT", "selectNextColumnChangeLead",
                                                              "shift LEFT", "selectNextColumnExtendSelection", "shift ctrl LEFT", "selectNextColumnExtendSelection",
                                                              "shift KP_LEFT", "selectNextColumnExtendSelection", "shift ctrl KP_LEFT", "selectNextColumnExtendSelection",
                                                              "RIGHT", "selectPreviousColumn", "KP_RIGHT", "selectPreviousColumn",
                                                              "ctrl RIGHT", "selectPreviousColumnChangeLead", "ctrl KP_RIGHT", "selectPreviousColumnChangeLead",
                                                              "shift RIGHT", "selectPreviousColumnExtendSelection", "shift ctrl RIGHT", "selectPreviousColumnExtendSelection",
                                                              "shift KP_RIGHT", "selectPreviousColumnExtendSelection", "shift ctrl KP_RIGHT", "selectPreviousColumnExtendSelection" };
        Object[] desktopAncestorInputMap = new Object[] {"ctrl F5", "restore", "ctrl F4", "close", "ctrl F7", "move",
                                                         "ctrl F8", "resize", "RIGHT", "right",
                                                         "KP_RIGHT", "right",
                                                         "shift RIGHT", "shrinkRight",
                                                         "shift KP_RIGHT", "shrinkRight",
                                                         "LEFT", "left", "KP_LEFT", "left",
                                                         "shift LEFT", "shrinkLeft",
                                                         "shift KP_LEFT", "shrinkLeft",
                                                         "UP", "up", "KP_UP", "up",
                                                         "shift UP", "shrinkUp", "shift KP_UP", "shrinkUp",
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
                                                         "ctrl F12", "navigateNext", "shift ctrl F12", "navigatePrevious" };
        Object[] scrollPaneAncestorInputMap = new Object[] {"RIGHT", "unitScrollRight", "KP_RIGHT", "unitScrollRight",
                                                            "DOWN", "unitScrollDown", "KP_DOWN", "unitScrollDown",
                                                            "LEFT", "unitScrollLeft", "KP_LEFT", "unitScrollLeft",
                                                            "UP", "unitScrollUp", "KP_UP", "unitScrollUp",
                                                            "PAGE_UP", "scrollUp", "PAGE_DOWN", "scrollDown",
                                                            "ctrl PAGE_UP", "scrollLeft",
                                                            "ctrl PAGE_DOWN", "scrollRight",
                                                            "ctrl HOME", "scrollHome", "ctrl END", "scrollEnd" };
        Object[] tabbedPaneFocusInputMap = new Object[] {"RIGHT", "navigateRight", "KP_RIGHT", "navigateRight",
                                                         "LEFT", "navigateLeft", "KP_LEFT", "navigateLeft",
                                                         "UP", "navigateUp", "KP_UP", "navigateUp",
                                                         "DOWN", "navigateDown", "KP_DOWN", "navigateDown",
                                                         "ctrl DOWN", "requestFocusForVisibleComponent",
                                                         "ctrl KP_DOWN", "requestFocusForVisibleComponent" };
        Object[] listFocusInputMap = new Object[] {"ctrl C", "copy", "ctrl V", "paste",
                                                   "ctrl X", "cut", "COPY", "copy",
                                                   "PASTE", "paste", "CUT", "cut", "UP", "selectPreviousRow",
                                                   "KP_UP", "selectPreviousRow", "shift UP", "selectPreviousRowExtendSelection",
                                                   "shift ctrl UP", "selectPreviousRowExtendSelection", "shift KP_UP", "selectPreviousRowExtendSelection",
                                                   "shift ctrl KP_UP", "selectPreviousRowExtendSelection", "DOWN", "selectNextRow",
                                                   "ctrl DOWN", "selectNextRowChangeLead", "KP_DOWN", "selectNextRow",
                                                   "ctrl KP_DOWN", "selectNextRowChangeLead", "shift DOWN", "selectNextRowExtendSelection",
                                                   "shift ctrl DOWN", "selectNextRowExtendSelection", "shift KP_DOWN", "selectNextRowExtendSelection",
                                                   "shift ctrl KP_DOWN", "selectNextRowExtendSelection", "LEFT", "selectPreviousColumn",
                                                   "ctrl LEFT", "selectPreviousColumnChangeLead", "KP_LEFT", "selectPreviousColumn",
                                                   "ctrl KP_LEFT", "selectPreviousColumnChangeLead", "shift LEFT", "selectPreviousColumnExtendSelection",
                                                   "shift ctrl LEFT", "selectPreviousColumnExtendSelection", "shift KP_LEFT", "selectPreviousColumnExtendSelection",
                                                   "shift ctrl KP_LEFT", "selectPreviousColumnExtendSelection", "RIGHT", "selectNextColumn",
                                                   "ctrl RIGHT", "selectNextColumnChangeLead", "KP_RIGHT", "selectNextColumn",
                                                   "ctrl KP_RIGHT", "selectNextColumnChangeLead", "shift RIGHT", "selectNextColumnExtendSelection",
                                                   "shift ctrl RIGHT", "selectNextColumnExtendSelection", "shift KP_RIGHT", "selectNextColumnExtendSelection",
                                                   "shift ctrl KP_RIGHT", "selectNextColumnExtendSelection", "ctrl SPACE", "selectNextRowExtendSelection",
                                                   "HOME", "selectFirstRow", "ctrl HOME", "selectFirstRowChangeLead",
                                                   "shift HOME", "selectFirstRowExtendSelection", "shift ctrl HOME", "selectFirstRowExtendSelection",
                                                   "END", "selectLastRow", "ctrl END", "selectLastRowChangeLead",
                                                   "shift END", "selectLastRowExtendSelection", "shift ctrl END", "selectLastRowExtendSelection",
                                                   "PAGE_UP", "scrollUp", "ctrl PAGE_UP", "scrollUpChangeLead", "shift PAGE_UP", "scrollUpExtendSelection", "shift ctrl PAGE_UP", "scrollUpExtendSelection",
                                                   "PAGE_DOWN", "scrollDown", "ctrl PAGE_DOWN", "scrollDownChangeLead", "shift PAGE_DOWN", "scrollDownExtendSelection", "shift ctrl PAGE_DOWN", "scrollDownExtendSelection",
                                                   "ctrl A", "selectAll", "ctrl SLASH", "selectAll", "ctrl BACK_SLASH", "clearSelection",
                                                   "ctrl UP", "selectPreviousRowChangeLead",
                                                   "ctrl KP_UP", "selectPreviousRowChangeLead",
                                                   "SPACE", "addToSelection", "ctrl SPACE", "toggleAndAnchor", "shift ctrl SPACE", "moveSelectionTo", "shift SPACE", "extendTo"};
        Object[] toolBarAncestorInputMap = new Object[] {"UP", "navigateUp", "KP_UP", "navigateUp",
                                                         "DOWN", "navigateDown", "KP_DOWN", "navigateDown",
                                                         "LEFT", "navigateLeft", "KP_LEFT", "navigateLeft",
                                                         "RIGHT", "navigateRight", "KP_RIGHT", "navigateRight" };
        Object[] sliderFocusInputMap = new Object[] {"RIGHT", "positiveUnitIncrement", "KP_RIGHT", "positiveUnitIncrement",
                                                     "DOWN", "negativeUnitIncrement", "KP_DOWN", "negativeUnitIncrement",
                                                     "PAGE_DOWN", "negativeBlockIncrement", "LEFT", "negativeUnitIncrement",
                                                     "KP_LEFT", "negativeUnitIncrement", "UP", "positiveUnitIncrement",
                                                     "KP_UP", "positiveUnitIncrement", "PAGE_UP", "positiveBlockIncrement",
                                                     "HOME", "minScroll", "END", "maxScroll" };
        Object[] comboBoxAncestorInputMap = new Object[] {"ESCAPE", "hidePopup", "PAGE_UP", "pageUpPassThrough",
                                                          "PAGE_DOWN", "pageDownPassThrough",
                                                          "HOME", "homePassThrough",
                                                          "END", "endPassThrough",
                                                          "ENTER", "enterPressed" };

        Object basicCompDefaults[] = {
                                      "AuditoryCues.allAuditoryCues", auditoryCuesAllAuditoryCues,
                                      "AuditoryCues.cueList", auditoryCuesCueList,
                                      "AuditoryCues.noAuditoryCues", new Object[] { "mute" },
                                      "Button.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getButtonBorder"),
                                      "Button.focusInputMap", new UIDefaults.LazyInputMap(new Object[] {"SPACE", "pressed", "released SPACE", "released", "ENTER", "pressed", "released ENTER", "released" }),
                                      "Button.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "Button.margin", new InsetsUIResource(2, 14, 2, 14),
                                      "Button.textIconGap", new Integer(4),
                                      "Button.textShiftOffset", new Integer(0),
                                      "CheckBox.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getRadioButtonBorder"),
                                      "CheckBox.focusInputMap", new UIDefaults.LazyInputMap(new Object[] { "SPACE", "pressed", "released SPACE", "released" }),
                                      "CheckBox.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "CheckBox.icon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicIconFactory", "getCheckBoxIcon"),
                                      "CheckBox.margin", new InsetsUIResource(2, 2, 2, 2),
                                      "CheckBox.textIconGap", new Integer(4),
                                      "CheckBox.textShiftOffset", new Integer(0),
                                      "CheckBoxMenuItem.acceleratorFont", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "CheckBoxMenuItem.arrowIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicIconFactory", "getMenuItemArrowIcon"),
                                      "CheckBoxMenuItem.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders$MarginBorder"),
                                      "CheckBoxMenuItem.borderPainted", Boolean.FALSE,
                                      "CheckBoxMenuItem.checkIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicIconFactory", "getCheckBoxMenuItemIcon"),
                                      "CheckBoxMenuItem.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "CheckBoxMenuItem.margin", new InsetsUIResource(2, 2, 2, 2),
                                      "ColorChooser.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "ColorChooser.swatchesRecentSwatchSize", new DimensionUIResource(10, 10),
                                      "ColorChooser.swatchesSwatchSize", new DimensionUIResource(10, 10),
                                      "ComboBox.ancestorInputMap", new UIDefaults.LazyInputMap(comboBoxAncestorInputMap),
                                      "ComboBox.font", lazyValueForFont("SansSerif", Font.PLAIN, 12),
                                      "Desktop.ancestorInputMap", new UIDefaults.LazyInputMap(desktopAncestorInputMap),
                                      "DesktopIcon.border",  new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getInternalFrameBorder"),
                                      "EditorPane.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders$MarginBorder"),
                                      "EditorPane.caretBlinkRate", BLINK_RATE,
                                      "EditorPane.font", lazyValueForFont("Serif", Font.PLAIN, 12),
                                      "EditorPane.margin", new InsetsUIResource(3, 3, 3, 3),
                                      "FileChooser.ancestorInputMap", new UIDefaults.LazyInputMap(new Object[] {"ESCAPE", "cancelSelection" }),
                                      "FileChooser.detailsViewIcon", makeIcon(BasicLookAndFeel.class, "icons/DetailsView.gif"),
                                      "FileChooser.homeFolderIcon", makeIcon(BasicLookAndFeel.class, "icons/HomeFolder.gif"),
                                      "FileChooser.listViewIcon", makeIcon(BasicLookAndFeel.class, "icons/ListView.gif"),
                                      "FileChooser.newFolderIcon", makeIcon(BasicLookAndFeel.class, "icons/NewFolder.gif"),
                                      "FileChooser.upFolderIcon", makeIcon(BasicLookAndFeel.class, "icons/UpFolder.gif"),
                                      "FileView.computerIcon", makeIcon(BasicLookAndFeel.class, "icons/Computer.gif"),
                                      "FileView.directoryIcon", makeIcon(BasicLookAndFeel.class, "icons/Directory.gif"),
                                      "FileView.fileIcon", makeIcon(BasicLookAndFeel.class, "icons/File.gif"),
                                      "FileView.floppyDriveIcon", makeIcon(BasicLookAndFeel.class, "icons/FloppyDrive.gif"),
                                      "FileView.hardDriveIcon", makeIcon(BasicLookAndFeel.class, "icons/HardDrive.gif"),
                                      "FormattedTextField.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getTextFieldBorder"),
                                      "FormattedTextField.caretBlinkRate", BLINK_RATE,
                                      "FormattedTextField.focusInputMap", new UIDefaults.LazyInputMap(formattedTextFieldFocusInputMap),
                                      "FormattedTextField.font", lazyValueForFont("SansSerif", Font.PLAIN, 12),
                                      "FormattedTextField.margin", new InsetsUIResource(0, 0, 0, 0),
                                      "InternalFrame.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getInternalFrameBorder"),
                                      "InternalFrame.closeIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicIconFactory", "createEmptyFrameIcon"),
                                      "InternalFrame.icon", makeIcon(BasicLookAndFeel.class, "icons/Logo.gif"),
                                      "InternalFrame.iconifyIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicIconFactory", "createEmptyFrameIcon"),
                                      "InternalFrame.maximizeIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicIconFactory", "createEmptyFrameIcon"),
                                      "InternalFrame.minimizeIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicIconFactory", "createEmptyFrameIcon"),
                                      "InternalFrame.titleFont", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "InternalFrame.windowBindings", new Object[] { "shift ESCAPE", "showSystemMenu", "ctrl SPACE", "showSystemMenu", "ESCAPE", "hideSystemMenu" },
                                      "Label.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "List.cellRenderer", makeListCellRenderer(),
                                      "List.focusCellHighlightBorder", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource$LineBorderUIResource", new Object[] {new ColorUIResource(Color.BLACK)}),
                                      "List.focusInputMap", new UIDefaults.LazyInputMap(listFocusInputMap),
                                      "List.focusInputMap.RightToLeft", new UIDefaults.LazyInputMap(listFocusInputMapRightToLeft),
                                      "List.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "Menu.acceleratorFont", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "Menu.arrowIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicIconFactory", "getMenuArrowIcon"),
                                      "Menu.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders$MarginBorder"),
                                      "Menu.borderPainted", Boolean.FALSE,
                                      "Menu.checkIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicIconFactory", "getMenuItemCheckIcon"),
                                      "Menu.crossMenuMnemonic", Boolean.TRUE,
                                      "Menu.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "Menu.margin", new InsetsUIResource(2, 2, 2, 2),
                                      "Menu.menuPopupOffsetX", new Integer(0),
                                      "Menu.menuPopupOffsetY", new Integer(0),
                                      "Menu.shortcutKeys", new int[] { KeyEvent.ALT_MASK },
                                      "Menu.submenuPopupOffsetX", new Integer(-2),
                                      "Menu.submenuPopupOffsetY", new Integer(-2),
                                      "MenuBar.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getMenuBarBorder"),
                                      "MenuBar.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "MenuBar.windowBindings", new Object[] {"F10", "takeFocus"},
                                      "MenuItem.acceleratorDelimiter", new String("+"),
                                      "MenuItem.acceleratorFont", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "MenuItem.arrowIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicIconFactory", "getMenuItemArrowIcon"),
                                      "MenuItem.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders$MarginBorder"),
                                      "MenuItem.borderPainted", Boolean.FALSE,
                                      "MenuItem.checkIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicIconFactory", "getMenuItemCheckIcon"),
                                      "MenuItem.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "MenuItem.margin", new InsetsUIResource(2, 2, 2, 2),
                                      "OptionPane.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource$EmptyBorderUIResource", new Object[] { new Integer(10), new Integer(10), new Integer(12), new Integer(10)}),
                                      "OptionPane.buttonAreaBorder", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource$EmptyBorderUIResource", new Object[] {new Integer(6), new Integer(0), new Integer(0), new Integer(0)}),
                                      "OptionPane.buttonClickThreshhold", new Integer(500),
                                      "OptionPane.errorIcon", makeIcon(BasicLookAndFeel.class, "icons/Error.gif"),
                                      "OptionPane.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "OptionPane.informationIcon", makeIcon(BasicLookAndFeel.class, "icons/Inform.gif"),
                                      "OptionPane.messageAreaBorder", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource$EmptyBorderUIResource", new Object[] {new Integer(0), new Integer(0), new Integer(0), new Integer(0)}),
                                      "OptionPane.minimumSize", new DimensionUIResource(262, 90),
                                      "OptionPane.questionIcon", makeIcon(BasicLookAndFeel.class, "icons/Question.gif"),
                                      "OptionPane.warningIcon", makeIcon(BasicLookAndFeel.class, "icons/Warn.gif"),
                                      "OptionPane.windowBindings", new Object[] {"ESCAPE", "close"},
                                      "Panel.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "PasswordField.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getTextFieldBorder"),
                                      "PasswordField.caretBlinkRate", BLINK_RATE,
                                      "PasswordField.font", lazyValueForFont("MonoSpaced", Font.PLAIN, 12),
                                      "PasswordField.margin", new InsetsUIResource(0, 0, 0, 0),
                                      "PopupMenu.border",  new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getInternalFrameBorder"),
                                      "PopupMenu.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "PopupMenu.selectedWindowInputMapBindings", new UIDefaults.LazyInputMap(popupMenuSelectedWindowInputMapBindings),
                                      "PopupMenu.selectedWindowInputMapBindings.RightToLeft", new UIDefaults.LazyInputMap(new Object[] { "LEFT", "selectChild", "KP_LEFT", "selectChild", "RIGHT", "selectParent", "KP_RIGHT", "selectParent" }),
                                      "ProgressBar.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getProgressBarBorder"),
                                      "ProgressBar.verticalSize", new DimensionUIResource(12, 146),
                                      "ProgressBar.horizontalSize", new DimensionUIResource(146, 12),
                                      "ProgressBar.cellLength", new Integer(3),
                                      "ProgressBar.cellSpacing", new Integer(2),
                                      "ProgressBar.cycleTime", new Integer(500),
                                      "ProgressBar.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "ProgressBar.repaintInterval", new Integer(60),
                                      "RadioButton.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getRadioButtonBorder"),
                                      "RadioButton.focusInputMap", new UIDefaults.LazyInputMap(radioButtonFocusInputMap),
                                      "RadioButton.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "RadioButton.icon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicIconFactory", "getRadioButtonIcon"),
                                      "RadioButton.margin", new InsetsUIResource(2, 2, 2, 2),
                                      "RadioButton.textIconGap", new Integer(4),
                                      "RadioButton.textShiftOffset", new Integer(0),
                                      "RadioButtonMenuItem.acceleratorFont", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "RadioButtonMenuItem.arrowIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicIconFactory", "getMenuItemArrowIcon"),
                                      "RadioButtonMenuItem.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders$MarginBorder"),
                                      "RadioButtonMenuItem.borderPainted", Boolean.FALSE,
                                      "RadioButtonMenuItem.checkIcon", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicIconFactory", "getRadioButtonMenuItemIcon"),
                                      "RadioButtonMenuItem.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "RadioButtonMenuItem.margin", new InsetsUIResource(2, 2, 2, 2),
                                      "RootPane.defaultButtonWindowKeyBindings", new Object[] { "ENTER", "press", "released ENTER", "release", "ctrl ENTER", "press", "ctrl released ENTER", "release"},
                                      "RootPane.ancestorInputMap", new UIDefaults.LazyInputMap(rootPaneAncestorInputMap),
                                      "ScrollBar.ancestorInputMap", new UIDefaults.LazyInputMap(scrollBarAncestorInputMap),
                                      "ScrollBar.ancestorInputMap.RightToLeft",  new UIDefaults.LazyInputMap(scrollBarAncestorInputMapRightToLeft),
                                      "ScrollBar.maximumThumbSize", new DimensionUIResource(4096, 4096),
                                      "ScrollBar.minimumThumbSize", new DimensionUIResource(8, 8),
                                      "ScrollBar.width", new Integer(16),
                                      "ScrollPane.ancestorInputMap", new UIDefaults.LazyInputMap(scrollPaneAncestorInputMap),
                                      "ScrollPane.ancestorInputMap.RightToLeft", new UIDefaults.LazyInputMap(scrollPaneAncestorInputMapRightToLeft),
                                      "ScrollPane.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getTextFieldBorder"),
                                      "ScrollPane.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "Slider.focusInputMap", new UIDefaults.LazyInputMap(sliderFocusInputMap),
                                      "Slider.focusInputMap.RightToLeft", new UIDefaults.LazyInputMap(new Object[] {"RIGHT", "negativeUnitIncrement", "KP_RIGHT", "negativeUnitIncrement", "LEFT", "positiveUnitIncrement", "KP_LEFT", "positiveUnitIncrement" }),
                                      "Slider.focusInsets", new InsetsUIResource(2, 2, 2, 2),
                                      "Spinner.ancestorInputMap", new UIDefaults.LazyInputMap(spinnerAncestorInputMap),
                                      "Spinner.arrowButtonSize", new DimensionUIResource(16, 5),
                                      "Spinner.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getTextFieldBorder"),
                                      "Spinner.editorBorderPainted", Boolean.FALSE,
                                      "Spinner.font", lazyValueForFont("MonoSpaced", Font.PLAIN, 12),
                                      "SplitPane.ancestorInputMap",  new UIDefaults.LazyInputMap(splitPaneAncestorInputMap),
                                      "SplitPane.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getSplitPaneBorder"),
                                      "SplitPane.dividerSize", new Integer(7),
                                      "SplitPaneDivider.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getSplitPaneDividerBorder"),
                                      "TabbedPane.ancestorInputMap",  new UIDefaults.LazyInputMap(tabbedPaneAncestorInputMap),
                                      "TabbedPane.contentBorderInsets", new InsetsUIResource(2, 2, 3, 3),
                                      "TabbedPane.focusInputMap", new UIDefaults.LazyInputMap(tabbedPaneFocusInputMap),
                                      "TabbedPane.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "TabbedPane.selectedTabPadInsets", new InsetsUIResource(2, 2, 2, 1),
                                      "TabbedPane.tabAreaInsets", new InsetsUIResource(3, 2, 0, 2),
                                      "TabbedPane.tabInsets", new InsetsUIResource(0, 4, 1, 4),
                                      "TabbedPane.tabRunOverlay", new Integer(2),
                                      "TabbedPane.textIconGap", new Integer(4),
                                      "Table.ancestorInputMap", new UIDefaults.LazyInputMap(tableAncestorInputMap),
                                      "Table.ancestorInputMap.RightToLeft", new UIDefaults.LazyInputMap(tableAncestorInputMapRightToLeft),
                                      "Table.focusCellHighlightBorder", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource$LineBorderUIResource", new Object[] {new ColorUIResource(Color.BLACK) }),
                                      "Table.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "Table.scrollPaneBorder", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource", "getLoweredBevelBorderUIResource"),
                                      "TableHeader.cellBorder", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource$BevelBorderUIResource", new Object[] {new Integer(0), null, null, null, null}),
                                      "TableHeader.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "TextArea.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders$MarginBorder"),
                                      "TextArea.caretBlinkRate", BLINK_RATE,
                                      "TextArea.font", lazyValueForFont("MonoSpaced", Font.PLAIN, 12),
                                      "TextArea.margin", new InsetsUIResource(0, 0, 0, 0),
                                      "TextField.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getTextFieldBorder"),
                                      "TextField.caretBlinkRate", BLINK_RATE,
                                      "TextField.font", lazyValueForFont("SansSerif", Font.PLAIN, 12),
                                      "TextField.margin", new InsetsUIResource(0, 0, 0, 0),
                                      "TextPane.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders$MarginBorder"),
                                      "TextPane.caretBlinkRate", BLINK_RATE,
                                      "TextPane.font", lazyValueForFont("Serif", Font.PLAIN, 12),
                                      "TextPane.margin", new InsetsUIResource(3, 3, 3, 3),
                                      "TitledBorder.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource", "getEtchedBorderUIResource"),
                                      "TitledBorder.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "ToggleButton.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.basic.BasicBorders", "getToggleButtonBorder"),
                                      "ToggleButton.focusInputMap",  new UIDefaults.LazyInputMap(toggleButtonFocusInputMap),
                                      "ToggleButton.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "ToggleButton.margin", new InsetsUIResource(2, 14, 2, 14),
                                      "ToggleButton.textIconGap", new Integer(4),
                                      "ToggleButton.textShiftOffset", new Integer(0),
                                      "ToolBar.ancestorInputMap", new UIDefaults.LazyInputMap(toolBarAncestorInputMap),
                                      "ToolBar.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource", "getEtchedBorderUIResource"),
                                      "ToolBar.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "ToolBar.separatorSize", new DimensionUIResource(10, 10),
                                      "ToolTip.border", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource", "getBlackLineBorderUIResource"),
                                      "ToolTip.font", lazyValueForFont("SansSerif", Font.PLAIN, 12),
                                      "Tree.ancestorInputMap", new UIDefaults.LazyInputMap(new Object[] {"ESCAPE", "cancel" }),
                                      "Tree.changeSelectionWithFocus", Boolean.TRUE,
                                      "Tree.closedIcon", makeIcon(BasicLookAndFeel.class, "icons/TreeClosed.gif"),
                                      "Tree.drawsFocusBorderAroundIcon", Boolean.FALSE,
                                      "Tree.editorBorder", new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource", "getBlackLineBorderUIResource"),
                                      "Tree.focusInputMap",  new UIDefaults.LazyInputMap(treeFocusInputMap),
                                      "Tree.focusInputMap.RightToLeft", new UIDefaults.LazyInputMap(treeFocusInputMapRightToLeft),
                                      "Tree.font", lazyValueForFont("Dialog", Font.PLAIN, 12),
                                      "Tree.leafIcon", makeIcon(BasicLookAndFeel.class, "icons/TreeLeaf.gif"),
                                      "Tree.leftChildIndent", new Integer(7),
                                      "Tree.openIcon", makeIcon(BasicLookAndFeel.class, "icons/TreeOpen.gif"),
                                      "Tree.rightChildIndent", new Integer(13),
                                      "Tree.rowHeight", new Integer(16),
                                      "Tree.scrollsOnExpand", Boolean.TRUE,
                                      "Viewport.font", lazyValueForFont("Dialog", Font.PLAIN, 12) };
        Object[] componentColors = {
                                    "Button.background", uiDefs.get("control"),
                                    "Button.darkShadow", uiDefs.get("controlDkShadow"),
                                    "Button.foreground", uiDefs.get("controlText"),
                                    "Button.highlight", uiDefs.get("controlLtHighlight"),
                                    "Button.light", uiDefs.get("controlHighlight"),
                                    "Button.shadow", uiDefs.get("controlShadow"),
                                    "CheckBox.background", uiDefs.get("control"),
                                    "CheckBox.foreground", uiDefs.get("controlText"),
                                    "CheckBoxMenuItem.acceleratorForeground", uiDefs.get("menuText"),
                                    "CheckBoxMenuItem.acceleratorSelectionForeground", uiDefs.get("textHighlightText"),
                                    "CheckBoxMenuItem.background", uiDefs.get("menu"),
                                    "CheckBoxMenuItem.foreground", uiDefs.get("menuText"),
                                    "CheckBoxMenuItem.selectionBackground", uiDefs.get("textHighlight"),
                                    "CheckBoxMenuItem.selectionForeground", uiDefs.get("textHighlightText"),
                                    "ColorChooser.background", uiDefs.get("control"),
                                    "ColorChooser.foreground", uiDefs.get("controlText"),
                                    "ColorChooser.swatchesDefaultRecentColor", uiDefs.get("control"),
                                    "ComboBox.background", uiDefs.get("control"),
                                    "ComboBox.buttonBackground", uiDefs.get("control"),
                                    "ComboBox.buttonDarkShadow", uiDefs.get("controlDkShadow"),
                                    "ComboBox.buttonHighlight", uiDefs.get("controlLtHighlight"),
                                    "ComboBox.buttonShadow", uiDefs.get("controlShadow"),
                                    "ComboBox.disabledBackground", uiDefs.get("control"),
                                    "ComboBox.disabledForeground", uiDefs.get("textInactiveText"),
                                    "ComboBox.foreground", uiDefs.get("controlText"),
                                    "ComboBox.selectionBackground", uiDefs.get("textHighlight"),
                                    "ComboBox.selectionForeground", uiDefs.get("textHighlightText"),
                                    "Desktop.background", uiDefs.get("desktop"),
                                    "EditorPane.background", uiDefs.getColor("window"),
                                    "EditorPane.caretForeground", uiDefs.get("textText"),
                                    "EditorPane.foreground", uiDefs.get("textText"),
                                    "EditorPane.inactiveForeground", uiDefs.get("textInactiveText"),
                                    "EditorPane.selectionBackground", uiDefs.get("textHighlight"),
                                    "EditorPane.selectionForeground", uiDefs.get("textHighlightText"),
                                    "FormattedTextField.background", uiDefs.get("window"),
                                    "FormattedTextField.caretForeground", uiDefs.get("textText"),
                                    "FormattedTextField.foreground", uiDefs.get("textText"),
                                    "FormattedTextField.inactiveBackground", uiDefs.get("control"),
                                    "FormattedTextField.inactiveForeground", uiDefs.get("textInactiveText"),
                                    "FormattedTextField.selectionBackground", uiDefs.get("textHighlight"),
                                    "FormattedTextField.selectionForeground", uiDefs.get("textHighlightText"),
                                    "InternalFrame.activeTitleBackground", uiDefs.get("activeCaption"),
                                    "InternalFrame.activeTitleForeground", uiDefs.get("controlText"),
                                    "InternalFrame.borderColor", uiDefs.get("control"),
                                    "InternalFrame.borderDarkShadow", uiDefs.get("controlDkShadow"),
                                    "InternalFrame.borderHighlight", uiDefs.get("controlLtHighlight"),
                                    "InternalFrame.borderLight", uiDefs.get("controlHighlight"),
                                    "InternalFrame.borderShadow", uiDefs.get("controlShadow"),
                                    "InternalFrame.inactiveTitleBackground", uiDefs.get("inactiveCaption"),
                                    "InternalFrame.inactiveTitleForeground", uiDefs.get("inactiveCaptionText"),
                                    "Label.background", uiDefs.get("control"),
                                    "Label.disabledForeground", uiDefs.get("controlLtHighlight"),
                                    "Label.disabledShadow", uiDefs.get("controlShadow"),
                                    "Label.foreground", uiDefs.get("controlText"),
                                    "List.background", uiDefs.get("window"),
                                    "List.foreground", uiDefs.get("textText"),
                                    "List.selectionBackground", uiDefs.get("textHighlight"),
                                    "List.selectionForeground", uiDefs.get("textHighlightText"),
                                    "Menu.acceleratorForeground", uiDefs.get("menuText"),
                                    "Menu.acceleratorSelectionForeground", uiDefs.get("textHighlightText"),
                                    "Menu.background", uiDefs.get("menu"),
                                    "Menu.foreground", uiDefs.get("menuText"),
                                    "Menu.selectionBackground", uiDefs.get("textHighlight"),
                                    "Menu.selectionForeground", uiDefs.get("textHighlightText"),
                                    "MenuBar.background", uiDefs.get("menu"),
                                    "MenuBar.foreground", uiDefs.get("menuText"),
                                    "MenuBar.highlight", uiDefs.get("controlLtHighlight"),
                                    "MenuBar.shadow", uiDefs.get("controlShadow"),
                                    "MenuItem.acceleratorForeground", uiDefs.get("menuText"),
                                    "MenuItem.acceleratorSelectionForeground", uiDefs.get("textHighlightText"),
                                    "MenuItem.background", uiDefs.get("menu"),
                                    "MenuItem.foreground", uiDefs.get("menuText"),
                                    "MenuItem.selectionBackground", uiDefs.get("textHighlight"),
                                    "MenuItem.selectionForeground", uiDefs.get("textHighlightText"),
                                    "OptionPane.background", uiDefs.get("control"),
                                    "OptionPane.foreground", uiDefs.get("controlText"),
                                    "OptionPane.messageForeground", uiDefs.get("controlText"),
                                    "Panel.background", uiDefs.get("control"),
                                    "Panel.foreground", uiDefs.get("textText"),
                                    "PasswordField.background", uiDefs.get("window"),
                                    "PasswordField.caretForeground", uiDefs.get("textText"),
                                    "PasswordField.foreground", uiDefs.get("textText"),
                                    "PasswordField.inactiveBackground", uiDefs.get("control"),
                                    "PasswordField.inactiveForeground", uiDefs.get("textInactiveText"),
                                    "PasswordField.selectionBackground", uiDefs.get("textHighlight"),
                                    "PasswordField.selectionForeground", uiDefs.get("textHighlightText"),
                                    "PopupMenu.background", uiDefs.get("menu"),
                                    "PopupMenu.foreground", uiDefs.get("menuText"),
                                    "ProgressBar.background", uiDefs.get("control"),
                                    "ProgressBar.foreground", uiDefs.get("textHighlight"),
                                    "ProgressBar.selectionBackground", uiDefs.get("textHighlight"),
                                    "ProgressBar.selectionForeground", uiDefs.get("control"),
                                    "RadioButton.background", uiDefs.get("control"),
                                    "RadioButton.darkShadow", uiDefs.get("controlDkShadow"),
                                    "RadioButton.foreground", uiDefs.get("controlText"),
                                    "RadioButton.highlight", uiDefs.get("controlLtHighlight"),
                                    "RadioButton.light", uiDefs.get("controlHighlight"),
                                    "RadioButton.shadow", uiDefs.get("controlShadow"),
                                    "RadioButtonMenuItem.acceleratorForeground", uiDefs.get("menuText"),
                                    "RadioButtonMenuItem.acceleratorSelectionForeground", uiDefs.get("textHighlightText"),
                                    "RadioButtonMenuItem.background", uiDefs.get("menu"),
                                    "RadioButtonMenuItem.foreground", uiDefs.get("menuText"),
                                    "RadioButtonMenuItem.selectionBackground", uiDefs.get("textHighlight"),
                                    "RadioButtonMenuItem.selectionForeground", uiDefs.get("textHighlightText"),
                                    "ScrollBar.background", uiDefs.get("control"),
                                    "ScrollBar.foreground", uiDefs.get("control"),
                                    "ScrollBar.thumb", uiDefs.get("control"),
                                    "ScrollBar.thumbDarkShadow", uiDefs.get("controlDkShadow"),
                                    "ScrollBar.thumbHighlight", uiDefs.get("controlLtHighlight"),
                                    "ScrollBar.thumbShadow", uiDefs.get("controlShadow"),
                                    "ScrollBar.track", uiDefs.get("scrollbar"),
                                    "ScrollBar.trackHighlight", uiDefs.get("controlDkShadow"),
                                    "ScrollPane.background", uiDefs.get("control"),
                                    "ScrollPane.foreground", uiDefs.get("controlText"),
                                    "Separator.background", uiDefs.get("controlLtHighlight"),
                                    "Separator.foreground", uiDefs.get("controlShadow"),
                                    "Separator.highlight", uiDefs.get("controlLtHighlight"),
                                    "Separator.shadow", uiDefs.get("controlShadow"),
                                    "Slider.background", uiDefs.get("control"),
                                    "Slider.focus", uiDefs.get("controlDkShadow"),
                                    "Slider.foreground", uiDefs.get("control"),
                                    "Slider.highlight", uiDefs.get("controlLtHighlight"),
                                    "Slider.shadow", uiDefs.get("controlShadow"),
                                    "Spinner.background", uiDefs.get("control"),
                                    "Spinner.foreground", uiDefs.get("control"),
                                    "SplitPane.background", uiDefs.get("control"),
                                    "SplitPane.darkShadow", uiDefs.get("controlDkShadow"),
                                    "SplitPane.highlight", uiDefs.get("controlLtHighlight"),
                                    "SplitPane.shadow", uiDefs.get("controlShadow"),
                                    "TabbedPane.background", uiDefs.get("control"),
                                    "TabbedPane.darkShadow", uiDefs.get("controlDkShadow"),
                                    "TabbedPane.focus", uiDefs.get("controlText"),
                                    "TabbedPane.foreground", uiDefs.get("controlText"),
                                    "TabbedPane.highlight", uiDefs.get("controlLtHighlight"),
                                    "TabbedPane.light", uiDefs.get("controlHighlight"),
                                    "TabbedPane.shadow", uiDefs.get("controlShadow"),
                                    "Table.background", uiDefs.get("window"),
                                    "Table.focusCellBackground", uiDefs.get("window"),
                                    "Table.focusCellForeground", uiDefs.get("controlText"),
                                    "Table.foreground", uiDefs.get("controlText"),
                                    "Table.gridColor", uiDefs.get("controlDkShadow"),
                                    "Table.selectionBackground", uiDefs.get("textHighlight"),
                                    "Table.selectionForeground", uiDefs.get("textHighlightText"),
                                    "TableHeader.background", uiDefs.get("control"),
                                    "TableHeader.foreground", uiDefs.get("controlText"),
                                    "TextArea.background", uiDefs.get("window"),
                                    "TextArea.caretForeground", uiDefs.get("textText"),
                                    "TextArea.foreground", uiDefs.get("textText"),
                                    "TextArea.inactiveForeground", uiDefs.get("textInactiveText"),
                                    "TextArea.selectionBackground", uiDefs.get("textHighlight"),
                                    "TextArea.selectionForeground", uiDefs.get("textHighlightText"),
                                    "TextField.background", uiDefs.get("window"),
                                    "TextField.caretForeground", uiDefs.get("textText"),
                                    "TextField.darkShadow", uiDefs.get("controlDkShadow"),
                                    "TextField.foreground", uiDefs.get("textText"),
                                    "TextField.highlight", uiDefs.get("controlLtHighlight"),
                                    "TextField.inactiveBackground", uiDefs.get("control"),
                                    "TextField.inactiveForeground", uiDefs.get("textInactiveText"),
                                    "TextField.light", uiDefs.get("controlHighlight"),
                                    "TextField.selectionBackground", uiDefs.get("textHighlight"),
                                    "TextField.selectionForeground", uiDefs.get("textHighlightText"),
                                    "TextField.shadow", uiDefs.get("controlShadow"),
                                    "TextPane.background", uiDefs.get("window"),
                                    "TextPane.caretForeground", uiDefs.get("textText"),
                                    "TextPane.foreground", uiDefs.get("textText"),
                                    "TextPane.inactiveForeground", uiDefs.get("textInactiveText"),
                                    "TextPane.selectionBackground", uiDefs.get("textHighlight"),
                                    "TextPane.selectionForeground", uiDefs.get("textHighlightText"),
                                    "TitledBorder.titleColor", uiDefs.get("controlText"),
                                    "ToggleButton.background", uiDefs.get("control"),
                                    "ToggleButton.darkShadow", uiDefs.get("controlDkShadow"),
                                    "ToggleButton.foreground", uiDefs.get("controlText"),
                                    "ToggleButton.highlight", uiDefs.get("controlLtHighlight"),
                                    "ToggleButton.light", uiDefs.get("controlHighlight"),
                                    "ToggleButton.shadow", uiDefs.get("controlShadow"),
                                    "ToolBar.background", uiDefs.get("control"),
                                    "ToolBar.darkShadow", uiDefs.get("controlDkShadow"),
                                    "ToolBar.dockingBackground", uiDefs.get("control"),
                                    "ToolBar.dockingForeground", new ColorUIResource(Color.GREEN),
                                    "ToolBar.floatingBackground", uiDefs.get("control"),
                                    "ToolBar.floatingForeground", uiDefs.get("controlDkShadow"),
                                    "ToolBar.foreground", uiDefs.get("controlText"),
                                    "ToolBar.highlight", uiDefs.get("controlLtHighlight"),
                                    "ToolBar.light", uiDefs.get("controlHighlight"),
                                    "ToolBar.shadow", uiDefs.get("controlShadow"),
                                    "ToolTip.background", uiDefs.get("info"),
                                    "ToolTip.foreground", uiDefs.get("infoText"),
                                    "Tree.background", uiDefs.get("window"),
                                    "Tree.foreground", uiDefs.get("textText"),
                                    "Tree.hash", uiDefs.get("controlDkShadow"),
                                    "Tree.selectionBackground", uiDefs.get("textHighlight"),
                                    "Tree.selectionBorderColor", uiDefs.get("controlText"),
                                    "Tree.selectionForeground", uiDefs.get("textHighlightText"),
                                    "Tree.textBackground", uiDefs.get("text"),
                                    "Tree.textForeground", uiDefs.get("textText"),
                                    "Viewport.background", uiDefs.get("control"),
                                    "Viewport.foreground", uiDefs.get("textText") };

        uiDefs.putDefaults(componentColors);
        uiDefs.putDefaults(basicCompDefaults);
    }

    protected void initClassDefaults(final UIDefaults uiDefs) {
        Object[] basicComponentUIs = { "InternalFrameUI", "javax.swing.plaf.basic.BasicInternalFrameUI",
                                       "ViewportUI", "javax.swing.plaf.basic.BasicViewportUI",
                                       "ScrollBarUI", "javax.swing.plaf.basic.BasicScrollBarUI",
                                       "ToolTipUI", "javax.swing.plaf.basic.BasicToolTipUI",
                                       "MenuItemUI", "javax.swing.plaf.basic.BasicMenuItemUI",
                                       "MenuUI", "javax.swing.plaf.basic.BasicMenuUI",
                                       "TextAreaUI", "javax.swing.plaf.basic.BasicTextAreaUI",
                                       "PopupMenuUI", "javax.swing.plaf.basic.BasicPopupMenuUI",
                                       "ScrollPaneUI", "javax.swing.plaf.basic.BasicScrollPaneUI",
                                       "SliderUI", "javax.swing.plaf.basic.BasicSliderUI",
                                       "ComboBoxUI", "javax.swing.plaf.basic.BasicComboBoxUI",
                                       "RadioButtonUI", "javax.swing.plaf.basic.BasicRadioButtonUI",
                                       "FormattedTextFieldUI", "javax.swing.plaf.basic.BasicFormattedTextFieldUI",
                                       "TreeUI", "javax.swing.plaf.basic.BasicTreeUI",
                                       "MenuBarUI", "javax.swing.plaf.basic.BasicMenuBarUI",
                                       "RadioButtonMenuItemUI", "javax.swing.plaf.basic.BasicRadioButtonMenuItemUI",
                                       "ProgressBarUI", "javax.swing.plaf.basic.BasicProgressBarUI",
                                       "ToolBarUI", "javax.swing.plaf.basic.BasicToolBarUI",
                                       "ColorChooserUI", "javax.swing.plaf.basic.BasicColorChooserUI",
                                       "ToolBarSeparatorUI", "javax.swing.plaf.basic.BasicToolBarSeparatorUI",
                                       "TabbedPaneUI", "javax.swing.plaf.basic.BasicTabbedPaneUI",
                                       "DesktopPaneUI", "javax.swing.plaf.basic.BasicDesktopPaneUI",
                                       "TableUI", "javax.swing.plaf.basic.BasicTableUI",
                                       "PanelUI", "javax.swing.plaf.basic.BasicPanelUI",
                                       "CheckBoxMenuItemUI", "javax.swing.plaf.basic.BasicCheckBoxMenuItemUI",
                                       "PasswordFieldUI", "javax.swing.plaf.basic.BasicPasswordFieldUI",
                                       "CheckBoxUI", "javax.swing.plaf.basic.BasicCheckBoxUI",
                                       "TableHeaderUI", "javax.swing.plaf.basic.BasicTableHeaderUI",
                                       "SplitPaneUI", "javax.swing.plaf.basic.BasicSplitPaneUI",
                                       "EditorPaneUI", "javax.swing.plaf.basic.BasicEditorPaneUI",
                                       "ListUI", "javax.swing.plaf.basic.BasicListUI",
                                       "SpinnerUI", "javax.swing.plaf.basic.BasicSpinnerUI",
                                       "DesktopIconUI", "javax.swing.plaf.basic.BasicDesktopIconUI",
                                       "TextFieldUI", "javax.swing.plaf.basic.BasicTextFieldUI",
                                       "TextPaneUI", "javax.swing.plaf.basic.BasicTextPaneUI",
                                       "LabelUI", "javax.swing.plaf.basic.BasicLabelUI",
                                       "ButtonUI", "javax.swing.plaf.basic.BasicButtonUI",
                                       "ToggleButtonUI", "javax.swing.plaf.basic.BasicToggleButtonUI",
                                       "OptionPaneUI", "javax.swing.plaf.basic.BasicOptionPaneUI",
                                       "PopupMenuSeparatorUI", "javax.swing.plaf.basic.BasicPopupMenuSeparatorUI",
                                       "RootPaneUI", "javax.swing.plaf.basic.BasicRootPaneUI",
                                       "SeparatorUI", "javax.swing.plaf.basic.BasicSeparatorUI" };

        uiDefs.putDefaults(basicComponentUIs);
    }

    public UIDefaults getDefaults() {
        if (uiDefaults == null) {
            uiDefaults = new UIDefaults();
            initClassDefaults(uiDefaults);
            initSystemColorDefaults(uiDefaults);
            initComponentDefaults(uiDefaults);
            uiDefaults.addResourceBundle(RESOURCE_BUNDLE);
        }

        return uiDefaults;
    }

    protected ActionMap getAudioActionMap() {
        ActionMapUIResource result = new ActionMapUIResource();
        Object[] actions = (Object[])getDefaults().get("AuditoryCues.cueList");
        if (actions == null) {
            return result;
        }
        for (int i = 0; i < actions.length; i++) {
            result.put(actions[i], createAudioAction(actions[i]));
        }

        return result;
    }

    protected Action createAudioAction(final Object key) {
        return new AudioAction((String)key);
    }

    void fireSoundAction(final JComponent c, final String name) {
        ActionMap map = SwingUtilities.getUIActionMap(c);
        if (map != null) {
            playSound(map.get(name));
        }
    }

    protected void playSound(final Action action) {
        if (action == null) {
            return;
        }

        String key = (String)action.getValue(Action.NAME);
        Object[] actions = (Object[])UIManager.getDefaults().get("AuditoryCues.playList");
        if (actions == null) {
            return;
        }
        for (int i = 0; i < actions.length; i++) {
            if (actions[i].equals(key)) {
                action.actionPerformed(new ActionEvent(this, 10001, null));
                break;
            }
        }
    }

    private Object makeListCellRenderer() {
        return new UIDefaults.ActiveValue() {
            public Object createValue(final UIDefaults uiDefaults) {
                return new DefaultListCellRenderer.UIResource();
            }
        };
    }

    private static ProxyLazyValue lazyValueForFont(final String fontName, final int style, final int size) {
        return new UIDefaults.ProxyLazyValue("javax.swing.plaf.FontUIResource",
                                             new Object[] {fontName, new Integer(style), new Integer(size)});
    }

    private void loadColors(final UIDefaults uiDefs, final String[] colors) {
        for (int i = 0; i < colors.length; i += 2) {
            uiDefs.put(colors[i], new ColorUIResource(Integer.decode(colors[i + 1]).intValue()));
        }
    }

    private void loadSystemColors(final UIDefaults uiDefs, final Object[] colors) {
        for (int i = 0; i < colors.length; i += 2) {
            uiDefs.put(colors[i], (colors[i + 1]));
        }
    }

    private Object[] getBasicSystemColors() {
        Object[] basicSystemColors = {"desktop", SystemColor.desktop,
                                      "activeCaption", SystemColor.activeCaption,
                                      "activeCaptionText", SystemColor.activeCaptionText,
                                      "activeCaptionBorder", SystemColor.activeCaptionBorder,
                                      "inactiveCaption", SystemColor.inactiveCaption,
                                      "inactiveCaptionText", SystemColor.inactiveCaptionText,
                                      "inactiveCaptionBorder", SystemColor.inactiveCaptionBorder,
                                      "window", SystemColor.window,
                                      "windowBorder", SystemColor.windowBorder,
                                      "windowText", SystemColor.windowText,
                                      "menu", SystemColor.menu,
                                      "menuText", SystemColor.menuText,
                                      "text", SystemColor.text,
                                      "textText", SystemColor.textText,
                                      "textHighlight", SystemColor.textHighlight,
                                      "textHighlightText", SystemColor.textHighlightText,
                                      "textInactiveText", SystemColor.textInactiveText,
                                      "control", SystemColor.control,
                                      "controlText", SystemColor.controlText,
                                      "controlHighlight", SystemColor.controlHighlight,
                                      "controlLtHighlight", SystemColor.controlLtHighlight,
                                      "controlShadow", SystemColor.controlShadow,
                                      "controlDkShadow", SystemColor.controlDkShadow,
                                      "scrollbar", SystemColor.scrollbar,
                                      "info", SystemColor.info,
                                      "infoText", SystemColor.infoText };
        return basicSystemColors;
    }

    final class AudioAction extends AbstractAction {
        private String key;

        AudioAction(final String key) {
            this.key = key;
            this.putValue(Action.NAME, key);
        }

        /**
         * Create instance of Runnable which plays sound mapped with key
         * @param String key
         * @return Runnable result
         */
        private Runnable createRunnableSound(final String key) {
            String pathToSound = getDefaults().getString(key);
            if (pathToSound == null) {
                return defaultRunnable();
            }
            URL url = BasicLookAndFeel.class.getResource(pathToSound);
            if (url == null) {
                return (Runnable)BasicLookAndFeel.getDesktopPropertyValue(pathToSound, defaultRunnable());
            }

            return getSoundRunnableFromURL(url);
        }

        /**
         * Create Runnable to get sound from URL
         * @param URL url
         * @return Runnable result
         */
        private Runnable getSoundRunnableFromURL(final URL url) {
            /* TODO return back when sound is implemented
            try {
                final AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
                final AudioFormat audioFormat = audioStream.getFormat();
                final DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                if (AudioSystem.isLineSupported(info)) {
                    return newRunnable(audioStream, audioFormat, info);
                }
            } catch (final Exception ignored) {
            }*/
            return defaultRunnable();
        }

        /**
         * Return Runnable to play the sound from audioStream
         * @param AudioInputStream audioStream
         * @param AudioFormat audioFormat
         * @param DataLine.Info info
         * @return Runnable result
         */
        /*
         * TODO uncomment when javax.sound is implemented
        private Runnable newRunnable(final AudioInputStream audioStream, final AudioFormat audioFormat, final DataLine.Info info) {
            return new Runnable() {
                public void run() {
                    try {
                        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                        line.open(audioFormat);
                        line.start();
                        byte[] data = new byte[audioStream.available()];
                        audioStream.read(data);
                        line.write(data, 0, data.length);
                        line.drain();
                        line.stop();
                        line.close();
                    } catch (final Exception ignored) {
                        ignored.printStackTrace();
                    }
                }
            };
        }
        */

        /**
         * Perform action
         */
        public void actionPerformed(final ActionEvent e) {
            new Thread(createRunnableSound(key)).start();
        }

        /**
         * Empty Runnable
         * @return Runnable result
         */
        private Runnable defaultRunnable() {
            return new Runnable() {
                public void run() {
                }
            };
        }
    }
}