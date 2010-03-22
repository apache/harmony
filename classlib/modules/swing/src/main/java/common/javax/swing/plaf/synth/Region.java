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

import java.util.HashMap;

/**
 * Region used to markup the type of JComponent to be painted
 */
@SuppressWarnings("nls")
public class Region {

    /**
     * All regions depository
     */
    static final HashMap<String, Region> regionsMap = new HashMap<String, Region>();

    public static final Region ARROW_BUTTON = new Region("ArrowButton",
            "ArrowButtonUI", true);

    public static final Region BUTTON = new Region("Button", "ButtonUI", false);

    public static final Region CHECK_BOX = new Region("CheckBox", "CheckBoxUI",
            false);

    public static final Region CHECK_BOX_MENU_ITEM = new Region(
            "CheckBoxMenuItem", "CheckBoxMenuItemUI", false);

    public static final Region COLOR_CHOOSER = new Region("ColorChooser",
            "ColorChooserUI", false);

    public static final Region COMBO_BOX = new Region("ComboBox", "ComboBoxUI",
            false);

    public static final Region DESKTOP_ICON = new Region("DesktopIcon",
            "DesktopIconUI", false);

    public static final Region DESKTOP_PANE = new Region("DesktopPane",
            "DesktopPaneUI", false);

    public static final Region EDITOR_PANE = new Region("EditorPane",
            "EditorPaneUI", false);

    public static final Region FILE_CHOOSER = new Region("FileChooser",
            "FileChooserUI", false);

    public static final Region FORMATTED_TEXT_FIELD = new Region(
            "FormattedTextField", "FormattedTextFieldUI", false);

    public static final Region INTERNAL_FRAME = new Region("InternalFrame",
            null, false);

    public static final Region INTERNAL_FRAME_TITLE_PANE = new Region(
            "InternalFrameTitlePane", "InternalFrameUI", false);

    public static final Region LABEL = new Region("Label", "LabelUI", false);

    public static final Region LIST = new Region("List", "ListUI", false);

    public static final Region MENU = new Region("Menu", "MenuUI", false);

    public static final Region MENU_BAR = new Region("MenuBar", "MenuBarUI",
            false);

    public static final Region MENU_ITEM = new Region("MenuItem", "MenuItemUI",
            false);

    public static final Region MENU_ITEM_ACCELERATOR = new Region(
            "MenuItemAccelerator", "MenuItemAcceleratorUI", false);

    public static final Region OPTION_PANE = new Region("OptionPane",
            "OptionPaneUI", false);

    public static final Region PANEL = new Region("Panel", "PanelUI", false);

    public static final Region PASSWORD_FIELD = new Region("PasswordField",
            "PasswordFieldUI", false);

    public static final Region POPUP_MENU = new Region("PopupMenu",
            "PopupMenuUI", false);

    public static final Region POPUP_MENU_SEPARATOR = new Region(
            "PopupMenuSeparator", "PopupMenuSeparatorUI", false);

    public static final Region PROGRESS_BAR = new Region("ProgressBar",
            "ProgressBarUI", false);

    public static final Region RADIO_BUTTON = new Region("RadioButton",
            "RadioButtonUI", false);

    public static final Region RADIO_BUTTON_MENU_ITEM = new Region(
            "RadioButtonMenuItem", "RadioButtonMenuItemUI", false);

    public static final Region ROOT_PANE = new Region("RootPane", "RootPaneUI",
            false);

    public static final Region SCROLL_BAR = new Region("ScrollBar",
            "ScrollBarUI", false);

    public static final Region SCROLL_BAR_THUMB = new Region("ScrollBarThumb",
            null, true);

    public static final Region SCROLL_BAR_TRACK = new Region("ScrollBarTrack",
            null, true);

    public static final Region SCROLL_PANE = new Region("ScrollPane",
            "ScrollPaneUI", false);

    public static final Region SEPARATOR = new Region("Separator",
            "SeparatorUI", false);

    public static final Region SLIDER = new Region("Slider", "SliderUI", false);

    public static final Region SLIDER_THUMB = new Region("SliderThumb", null,
            true);

    public static final Region SLIDER_TRACK = new Region("SliderTrack", null,
            true);

    public static final Region SPINNER = new Region("Spinner", "SpinnerUI",
            false);

    public static final Region SPLIT_PANE = new Region("SplitPane",
            "SplitPaneUI", false);

    public static final Region SPLIT_PANE_DIVIDER = new Region(
            "SplitPaneDivider", null, true);

    public static final Region TABBED_PANE = new Region("TabbedPane",
            "TabbedPaneUI", false);

    public static final Region TABBED_PANE_CONTENT = new Region(
            "TabbedPaneContent", null, true);

    public static final Region TABBED_PANE_TAB = new Region("TabbedPaneTab",
            null, true);

    public static final Region TABBED_PANE_TAB_AREA = new Region(
            "TabbedPaneTabArea", null, true);

    public static final Region TABLE = new Region("Table", "TableUI", false);

    public static final Region TABLE_HEADER = new Region("TableHeader",
            "TableHeaderUI", false);

    public static final Region TEXT_AREA = new Region("TextArea", "TextAreaUI",
            false);

    public static final Region TEXT_FIELD = new Region("TextField",
            "TextFieldUI", false);

    public static final Region TEXT_PANE = new Region("TextPane", "TextPaneUI",
            false);

    public static final Region TOGGLE_BUTTON = new Region("ToggleButton",
            "ToggleButtonUI", false);

    public static final Region TOOL_BAR = new Region("ToolBar", "ToolBarUI",
            false);

    public static final Region TOOL_BAR_CONTENT = new Region("ToolBarContent",
            null, true);

    public static final Region TOOL_BAR_DRAG_WINDOW = new Region(
            "ToolBarDragWindow", "ToolBarDragWindowUI", false);

    public static final Region TOOL_BAR_SEPARATOR = new Region(
            "ToolBarSeparator", "ToolBarSeparatorUI", false);

    public static final Region TOOL_TIP = new Region("ToolTip", "ToolTipUI",
            false);

    public static final Region TREE = new Region("Tree", "TreeUI", false);

    public static final Region TREE_CELL = new Region("TreeCell", null, true);

    public static final Region VIEWPORT = new Region("Viewport", "ViewportUI",
            false);

    private String name;

    private String ui;

    private boolean isSub;

    protected Region(String name, String ui, boolean subregion) {
        this.name = name;
        this.isSub = subregion;
        this.ui = ui;
        regionsMap.put(name, this);
    }

    /**
     * @return the region corresponds given UI String
     */
    static Region getRegionFromUIID(String ui) {

        // Possible improvement:
        // This method creates additional object(substring) but possibly faster
        // way is the creating two HashMaps that contains the same values but
        // the different keys: UI and name
        return regionsMap.get(ui.substring(0, (ui.length() - 2)));
    }

    public String getName() {

        return this.name;
    }

    public boolean isSubregion() {

        return this.isSub;
    }

    @Override
    public String toString() {

        return this.ui;
    }

    /**
     * @return the region corresponds given name String
     */
    static Region getRegionFromName(String reference) {
        return regionsMap.get(reference);
    }
}
