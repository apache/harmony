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
 * @author Dennis Ushakov
 */

package javax.accessibility;

@SuppressWarnings("nls")
public class AccessibleRole extends AccessibleBundle {
    public static final AccessibleRole ALERT = new AccessibleRole("alert");
    public static final AccessibleRole COLUMN_HEADER = new AccessibleRole("columnHeader");
    public static final AccessibleRole CANVAS = new AccessibleRole("canvas");
    public static final AccessibleRole COMBO_BOX = new AccessibleRole("combobox");
    public static final AccessibleRole DESKTOP_ICON = new AccessibleRole("desktopIcon");
    public static final AccessibleRole INTERNAL_FRAME = new AccessibleRole("internalFrame");
    public static final AccessibleRole DESKTOP_PANE = new AccessibleRole("desktopPane");
    public static final AccessibleRole OPTION_PANE = new AccessibleRole("optionPane");
    public static final AccessibleRole WINDOW = new AccessibleRole("window");
    public static final AccessibleRole FRAME = new AccessibleRole("frame");
    public static final AccessibleRole DIALOG = new AccessibleRole("dialog");
    public static final AccessibleRole COLOR_CHOOSER = new AccessibleRole("colorChooser");
    public static final AccessibleRole DIRECTORY_PANE = new AccessibleRole("directoryPane");
    public static final AccessibleRole FILE_CHOOSER = new AccessibleRole("fileChooser");
    public static final AccessibleRole FILLER = new AccessibleRole("filler");
    public static final AccessibleRole HYPERLINK = new AccessibleRole("hyperlink");
    public static final AccessibleRole ICON = new AccessibleRole("icon");
    public static final AccessibleRole LABEL = new AccessibleRole("label");
    public static final AccessibleRole ROOT_PANE = new AccessibleRole("rootPane");
    public static final AccessibleRole GLASS_PANE = new AccessibleRole("glassPane");
    public static final AccessibleRole LAYERED_PANE = new AccessibleRole("layeredPane");
    public static final AccessibleRole LIST = new AccessibleRole("list");
    public static final AccessibleRole LIST_ITEM = new AccessibleRole("listItem");
    public static final AccessibleRole MENU_BAR = new AccessibleRole("menuBar");
    public static final AccessibleRole POPUP_MENU = new AccessibleRole("popupMenu");
    public static final AccessibleRole MENU = new AccessibleRole("menu");
    public static final AccessibleRole MENU_ITEM = new AccessibleRole("menuItem");
    public static final AccessibleRole SEPARATOR = new AccessibleRole("separator");
    public static final AccessibleRole PAGE_TAB_LIST = new AccessibleRole("pageTabList");
    public static final AccessibleRole PAGE_TAB = new AccessibleRole("pageTab");
    public static final AccessibleRole PANEL = new AccessibleRole("panel");
    public static final AccessibleRole PROGRESS_BAR = new AccessibleRole("progressBar");
    public static final AccessibleRole PASSWORD_TEXT = new AccessibleRole("passwordText");
    public static final AccessibleRole PUSH_BUTTON = new AccessibleRole("pushButton");
    public static final AccessibleRole TOGGLE_BUTTON = new AccessibleRole("toggleButton");
    public static final AccessibleRole CHECK_BOX = new AccessibleRole("checkBox");
    public static final AccessibleRole RADIO_BUTTON = new AccessibleRole("radioButton");
    public static final AccessibleRole ROW_HEADER = new AccessibleRole("rowHeader");
    public static final AccessibleRole SCROLL_PANE = new AccessibleRole("scrollPane");
    public static final AccessibleRole SCROLL_BAR = new AccessibleRole("scrollBar");
    public static final AccessibleRole VIEWPORT = new AccessibleRole("viewport");
    public static final AccessibleRole SLIDER = new AccessibleRole("slider");
    public static final AccessibleRole SPLIT_PANE = new AccessibleRole("splitPane");
    public static final AccessibleRole TABLE = new AccessibleRole("table");
    public static final AccessibleRole TEXT = new AccessibleRole("text");
    public static final AccessibleRole TREE = new AccessibleRole("tree");
    public static final AccessibleRole TOOL_BAR = new AccessibleRole("toolBar");
    public static final AccessibleRole TOOL_TIP = new AccessibleRole("toolTip");
    public static final AccessibleRole AWT_COMPONENT = new AccessibleRole("awtComponent");
    public static final AccessibleRole SWING_COMPONENT = new AccessibleRole("swingComponent");
    public static final AccessibleRole UNKNOWN = new AccessibleRole("unknown");
    public static final AccessibleRole STATUS_BAR = new AccessibleRole("statusBar");
    public static final AccessibleRole DATE_EDITOR = new AccessibleRole("dateEditor");
    public static final AccessibleRole SPIN_BOX = new AccessibleRole("spinBox");
    public static final AccessibleRole FONT_CHOOSER = new AccessibleRole("fontChooser");
    public static final AccessibleRole GROUP_BOX = new AccessibleRole("groupBox");
    public static final AccessibleRole HEADER = new AccessibleRole("header");
    public static final AccessibleRole FOOTER = new AccessibleRole("footer");
    public static final AccessibleRole PARAGRAPH = new AccessibleRole("paragraph");
    public static final AccessibleRole RULER = new AccessibleRole("ruler");
    public static final AccessibleRole EDITBAR = new AccessibleRole("editBar");
    public static final AccessibleRole PROGRESS_MONITOR = new AccessibleRole("progressMonitor");

    protected AccessibleRole(final String key) {
        this.key = key;
    }
}

