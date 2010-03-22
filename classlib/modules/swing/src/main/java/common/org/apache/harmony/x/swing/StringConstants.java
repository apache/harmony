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
 * @author Vadim L. Bogdanov, Anton Avtamonov
 */

package org.apache.harmony.x.swing;

/**
 * This interface contains string constants used in Swing implementation.
 * Usually representing constants are names of the bound properties which are referenced
 * from the multiple locations.
 *
 * These constants must be kept unlocalized.
 *
 */
public interface StringConstants {
    String ENABLED_PROPERTY_CHANGED = "enabled";
    String FONT_PROPERTY_CHANGED = "font";
    String TOOLTIP_PROPERTY_CHANGED = "ToolTipText";
    String EDITABLE_PROPERTY_CHANGED = "editable";
    String MODEL_PROPERTY_CHANGED = "model";
    String SELECTION_MODEL_PROPERTY = "selectionModel";
    String RENDERER_PROPERTY_CHANGED = "renderer";
    String EDITOR_PROPERTY_CHANGED = "editor";
    String ACTION_PROPERTY_CHANGED = "action";
    String BACKGROUND_PROPERTY_CHANGED = "background";
    String FOREGROUND_PROPERTY_CHANGED = "foreground";
    String MNEMONIC_INDEX_PROPERTY_CHANGED = "displayedMnemonicIndex";
    String BORDER_PROPERTY_CHANGED = "border";
    String ICON_TEXT_GAP_PROPERTY_CHANGED = "iconTextGap";
    String ANCESTOR_PROPERTY_NAME = "ancestor";
    String LABEL_PROPERTY_CHANGED = "label";
    String VISIBLE_PROPERTY_CHANGED = "visible";
    String DEFAULT_CAPABLE_PROPERTY_CHANGED = "defaultCapable";
    String TRANSFER_HANDLER_PROPERTY_NAME = "transferHandler";
    String HIDE_ON_INVOKER_PRESSED_PROPERTY = "HideOnInvokerPressed";



    String BUTTON_PRESSED_ACTION = "pressed";
    String BUTTON_RELEASED_ACTION = "released";
    String CLOSE_ACTION = "close";
    String MNEMONIC_ACTION = "mnemonic";

    String UI_PROPERTY = "UI";

    String BIDI_PROPERTY = "i18n";

    String VERTICAL_SCROLLBAR_PROPERTY = "verticalScrollBar";
    String HORIZONTAL_SCROLLBAR_PROPERTY = "horizontalScrollBar";
    String VERTICAL_SCROLLBAR_POLICY_PROPERTY = "verticalScrollBarPolicy";
    String HORIZONTAL_SCROLLBAR_POLICY_PROPERTY = "horizontalScrollBarPolicy";
    String COLUMN_HEADER_PROPERTY = "columnHeader";
    String ROW_HEADER_PROPERTY = "rowHeader";
    String VIEWPORT_PROPERTY = "viewport";
    String COMPONENT_ORIENTATION = "componentOrientation";
    String OPAQUE_PROPERTY = "opaque";
    String IS_TABLE_EDITOR = "isTableEditor";

    String INTERNAL_FRAME_ICONABLE_PROPERTY = "iconable";
    String INTERNAL_FRAME_MAXIMIZABLE_PROPERTY = "maximizable";
    String INTERNAL_FRAME_CLOSABLE_PROPERTY = "closable";
    String INTERNAL_FRAME_RESIZABLE_PROPERTY = "resizable";

    String TEXT_COMPONENT_DOCUMENT_PROPERTY = "document";
    String TEXT_COMPONENT_LINE_WRAP_PROPERTY = "lineWrap";
    String TEXT_COMPONENT_WRAP_STYLE_WORD_PROPERTY = "wrapStyleWord";
    String TEXT_COMPONENT_CARET_COLOR_PROPERTY = "caretColor";
    String TEXT_COMPONENT_SELECTION_COLOR_PROPERTY = "selectionColor";
    String TEXT_COMPONENT_HIGHLIGHTER_PROPERTY = "highlighter";
    String TEXT_COMPONENT_DISABLED_TEXT_COLOR = "disabledTextColor";
    String TEXT_COMPONENT_SELECTED_TEXT_COLOR = "selectedTextColor";
    String TEXT_COMPONENT_MARGIN_PROPERTY = "margin";
    String TEXT_COMPONENR_KEYMAP_PROPERTY = "keymap";
    String TEXT_COMPONENT_NAV_FILTER_NAME = "navigationFilter";
    String IGNORE_CHARSET_DIRECTIVE = "IgnoreCharsetDirective";

    String EDITOR_PANE_EDITOR_KIT_PROPERTY = "editorKit";

    String PASSWORD_FIELD_ECHO_CHAR_PROPERTY = "echoChar";

    String ACCELERATOR_PROPERTY = "accelerator";

    String ICON_IMAGE_PROPERTY = "iconImage";

    String PROGRESS_STRING_PAINTED_PROPERTY = "stringPainted";
    String ORIENTATION = "orientation";
    String INDETERMINATE_PROPERTY = "indeterminate";

    String VALUE_PROPERTY_NAME = "value";

    String EXTENDED_SUPPORT_ENABLED_PROPERTY = "extendedSupportEnabled";

    String LIGHTWEIGHT_POPUP_ENABLED_PROPERTY_CHANGED = "lightWeightPopupEnabled";
}
