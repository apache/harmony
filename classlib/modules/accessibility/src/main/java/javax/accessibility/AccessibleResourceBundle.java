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

package javax.accessibility;

import java.util.ListResourceBundle;

@Deprecated
public class AccessibleResourceBundle extends ListResourceBundle {
    // Localized values representing the accessible roles and states of the accessible components.
    @SuppressWarnings("nls")
    private final Object[][] contents = {
        {"alert", "alert"},
        {"column_header", "column header"},
        {"canvas", "canvas"},
        {"combobox", "combobox"},
        {"desktopIcon", "desktop icon"},
        {"internalFrame", "internal frame"},
        {"desktopPane", "desktop pane"},
        {"optionPane", "option pane"},
        {"window", "window"},
        {"frame", "frame"},
        {"dialog", "dialog"},
        {"colorChooser", "color chooser"},
        {"directoryPane", "directory pane"},
        {"fileChooser", "file chooser"},
        {"filler", "filler"},
        {"hyperlink", "hyperlink"},
        {"icon", "icon"},
        {"label", "label"},
        {"rootPane", "root pane"},
        {"glassPane", "glass pane"},
        {"layeredPane", "layered pane"},
        {"list", "list"},
        {"listItem", "list item"},
        {"menuBar", "menu bar"},
        {"popupMenu", "popup menu"},
        {"menu", "menu"},
        {"menuItem", "menu item"},
        {"separator", "separator"},
        {"pageTabList", "page tab list"},
        {"pageTab", "page tab"},
        {"panel", "panel"},
        {"progressBar", "progress"},
        {"passwordText", "password"},
        {"pushButton", "push button"},
        {"toggleButton", "toggle button"},
        {"checkBox", "check box"},
        {"radioButton", "radio button"},
        {"rowHeader", "row header"},
        {"scrollPane", "scroll pane"},
        {"scrollBar", "scroller"},
        {"viewport", "viewport"},
        {"slider", "slider"},
        {"splitPane", "split pane"},
        {"table", "table"},
        {"text", "text"},
        {"tree", "tree"},
        {"toolBar", "tool bar"},
        {"toolTip", "tool tip"},
        {"awtComponent", "AWT component"},
        {"swingComponent", "Swing component"},
        {"unknown", "unknown"},
        {"statusBar", "status bar"},
        {"dateEditor", "date editor"},
        {"spinBox", "spinner"},
        {"fontChooser", "font chooser"},
        {"groupBox", "group"},
        {"header", "header"},
        {"footer", "footer"},
        {"paragraph", "paragraph"},
        {"ruler", "ruler"},
        {"editBar", "edit bar"},
        {"progressMonitor", "progress monitor"},
        {"labelFor", "label for"},
        {"labeledBy", "labeled by"},
        {"memberOf", "member of"},
        {"controllerFor", "controller for"},
        {"controlledBy", "controlled by"},
        {"flowsTo", "flows to"},
        {"flowsFrom", "flows from"},
        {"subwindowOf", "sub-window of"},
        {"parentWindowOf", "parent window of"},
        {"embeds", "embeds"},
        {"embeddedBy", "embedded by"},
        {"childNodeOf", "child node of"},
        {"active", "active"},
        {"pressed", "pressed"},
        {"armed", "armed"},
        {"busy", "busy"},
        {"checked", "checked"},
        {"editable", "editable"},
        {"expandable", "expandable"},
        {"collapsed", "collapsed"},
        {"expanded", "expanded"},
        {"enabled", "enabled"},
        {"focusable", "focusable"},
        {"focused", "focused"},
        {"iconified", "iconified"},
        {"modal", "modal"},
        {"opaque", "opaque"},
        {"resizable", "resizable"},
        {"multiSelectable", "multi-selectable"},
        {"selectable", "selectable"},
        {"selected", "selected"},
        {"showing", "showing"},
        {"visible", "visible"},
        {"vertical", "vertical"},
        {"horizontal", "horizontal"},
        {"singleLine", "singleLine"},
        {"multiLine", "multi line"},
        {"transient", "transient"},
        {"managesDescendants", "manages descendants"},
        {"indeterminate", "indeterminate"},
        {"truncated", "truncated"},
    };

    @Override
    public Object[][] getContents() {
        return contents;
    }
}

