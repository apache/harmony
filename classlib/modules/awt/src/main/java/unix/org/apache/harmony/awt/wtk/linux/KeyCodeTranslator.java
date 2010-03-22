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
 * @author Dmitry A. Durnev
 */
package org.apache.harmony.awt.wtk.linux;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import org.apache.harmony.awt.nativebridge.CLongPointer;
import org.apache.harmony.awt.nativebridge.Int8Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.X11Defs;
import org.apache.harmony.awt.wtk.KeyInfo;


final class KeyCodeTranslator {

    private static final NativeBridge bridge = NativeBridge.getInstance();

    private static final X11 x11 = X11.getInstance();

    private static final Map tableXK2VK;

    private static final Map tableVK2XK;

    static {
        tableXK2VK = new HashMap();
        tableXK2VK.put(new Integer(X11Defs.XK_Tab), new Integer(KeyEvent.VK_TAB));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Tab), new Integer(KeyEvent.VK_TAB));
        tableXK2VK.put(new Integer(X11Defs.XK_ISO_Left_Tab), new Integer(KeyEvent.VK_TAB));
        tableXK2VK.put(new Integer(X11Defs.XK_Clear), new Integer(KeyEvent.VK_CLEAR));
        tableXK2VK.put(new Integer(X11Defs.XK_Pause), new Integer(KeyEvent.VK_PAUSE));
        tableXK2VK.put(new Integer(X11Defs.XK_Scroll_Lock), new Integer(KeyEvent.VK_SCROLL_LOCK));
        tableXK2VK.put(new Integer(X11Defs.XK_Escape), new Integer(KeyEvent.VK_ESCAPE));
        tableXK2VK.put(new Integer(X11Defs.XK_Delete), new Integer(KeyEvent.VK_DELETE));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Delete), new Integer(KeyEvent.VK_DELETE));
        tableXK2VK.put(new Integer(X11Defs.XK_Kanji), new Integer(KeyEvent.VK_KANJI));
        tableXK2VK.put(new Integer(X11Defs.XK_Hiragana), new Integer(KeyEvent.VK_HIRAGANA));
        tableXK2VK.put(new Integer(X11Defs.XK_Katakana), new Integer(KeyEvent.VK_KATAKANA));
        tableXK2VK.put(new Integer(X11Defs.XK_Kana_Lock), new Integer(KeyEvent.VK_KANA_LOCK));
        tableXK2VK.put(new Integer(X11Defs.XK_Home), new Integer(KeyEvent.VK_HOME));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Home), new Integer(KeyEvent.VK_HOME));
        tableXK2VK.put(new Integer(X11Defs.XK_Left), new Integer(KeyEvent.VK_LEFT));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Left), new Integer(KeyEvent.VK_LEFT));
        tableXK2VK.put(new Integer(X11Defs.XK_Up), new Integer(KeyEvent.VK_UP));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Up), new Integer(KeyEvent.VK_UP));
        tableXK2VK.put(new Integer(X11Defs.XK_Right), new Integer(KeyEvent.VK_RIGHT));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Right), new Integer(KeyEvent.VK_RIGHT));
        tableXK2VK.put(new Integer(X11Defs.XK_Down), new Integer(KeyEvent.VK_DOWN));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Down), new Integer(KeyEvent.VK_DOWN));
        tableXK2VK.put(new Integer(X11Defs.XK_Page_Up), new Integer(KeyEvent.VK_PAGE_UP));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Page_Up), new Integer(KeyEvent.VK_PAGE_UP));
        tableXK2VK.put(new Integer(X11Defs.XK_Page_Down), new Integer(KeyEvent.VK_PAGE_DOWN));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Page_Down), new Integer(KeyEvent.VK_PAGE_DOWN));
        tableXK2VK.put(new Integer(X11Defs.XK_End), new Integer(KeyEvent.VK_END));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_End), new Integer(KeyEvent.VK_END));
        tableXK2VK.put(new Integer(X11Defs.XK_Insert), new Integer(KeyEvent.VK_INSERT));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Insert), new Integer(KeyEvent.VK_INSERT));
        tableXK2VK.put(new Integer(X11Defs.XK_Undo), new Integer(KeyEvent.VK_UNDO));
        tableXK2VK.put(new Integer(X11Defs.XK_Find), new Integer(KeyEvent.VK_FIND));
        tableXK2VK.put(new Integer(X11Defs.XK_Cancel), new Integer(KeyEvent.VK_CANCEL));
        tableXK2VK.put(new Integer(X11Defs.XK_Help), new Integer(KeyEvent.VK_HELP));
        tableXK2VK.put(new Integer(X11Defs.XK_Num_Lock), new Integer(KeyEvent.VK_NUM_LOCK));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Left), new Integer(KeyEvent.VK_KP_LEFT));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Up), new Integer(KeyEvent.VK_KP_UP));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Right), new Integer(KeyEvent.VK_KP_RIGHT));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Down), new Integer(KeyEvent.VK_KP_DOWN));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_0), new Integer(KeyEvent.VK_NUMPAD0));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_1), new Integer(KeyEvent.VK_NUMPAD1));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_2), new Integer(KeyEvent.VK_NUMPAD2));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_3), new Integer(KeyEvent.VK_NUMPAD3));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_4), new Integer(KeyEvent.VK_NUMPAD4));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_5), new Integer(KeyEvent.VK_NUMPAD5));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_6), new Integer(KeyEvent.VK_NUMPAD6));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_7), new Integer(KeyEvent.VK_NUMPAD7));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_8), new Integer(KeyEvent.VK_NUMPAD8));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_9), new Integer(KeyEvent.VK_NUMPAD9));
        tableXK2VK.put(new Integer(X11Defs.XK_F1), new Integer(KeyEvent.VK_F1));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_F1), new Integer(KeyEvent.VK_F1));
        tableXK2VK.put(new Integer(X11Defs.XK_F2), new Integer(KeyEvent.VK_F2));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_F2), new Integer(KeyEvent.VK_F2));
        tableXK2VK.put(new Integer(X11Defs.XK_F3), new Integer(KeyEvent.VK_F3));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_F3), new Integer(KeyEvent.VK_F3));
        tableXK2VK.put(new Integer(X11Defs.XK_F4), new Integer(KeyEvent.VK_F4));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_F4), new Integer(KeyEvent.VK_F4));
        tableXK2VK.put(new Integer(X11Defs.XK_F5), new Integer(KeyEvent.VK_F5));
        tableXK2VK.put(new Integer(X11Defs.XK_F6), new Integer(KeyEvent.VK_F6));
        tableXK2VK.put(new Integer(X11Defs.XK_F7), new Integer(KeyEvent.VK_F7));
        tableXK2VK.put(new Integer(X11Defs.XK_F8), new Integer(KeyEvent.VK_F8));
        tableXK2VK.put(new Integer(X11Defs.XK_F9), new Integer(KeyEvent.VK_F9));
        tableXK2VK.put(new Integer(X11Defs.XK_F10), new Integer(KeyEvent.VK_F10));
        tableXK2VK.put(new Integer(X11Defs.XK_F11), new Integer(KeyEvent.VK_F11));
        tableXK2VK.put(new Integer(X11Defs.XK_F12), new Integer(KeyEvent.VK_F12));
        tableXK2VK.put(new Integer(X11Defs.XK_F13), new Integer(KeyEvent.VK_F13));
        tableXK2VK.put(new Integer(X11Defs.XK_F14), new Integer(KeyEvent.VK_F14));
        tableXK2VK.put(new Integer(X11Defs.XK_F15), new Integer(KeyEvent.VK_F15));
        tableXK2VK.put(new Integer(X11Defs.XK_F16), new Integer(KeyEvent.VK_F16));
        tableXK2VK.put(new Integer(X11Defs.XK_F17), new Integer(KeyEvent.VK_F17));
        tableXK2VK.put(new Integer(X11Defs.XK_F18), new Integer(KeyEvent.VK_F18));
        tableXK2VK.put(new Integer(X11Defs.XK_F19), new Integer(KeyEvent.VK_F19));
        tableXK2VK.put(new Integer(X11Defs.XK_F20), new Integer(KeyEvent.VK_F20));
        tableXK2VK.put(new Integer(X11Defs.XK_F21), new Integer(KeyEvent.VK_F21));
        tableXK2VK.put(new Integer(X11Defs.XK_F22), new Integer(KeyEvent.VK_F22));
        tableXK2VK.put(new Integer(X11Defs.XK_F23), new Integer(KeyEvent.VK_F23));
        tableXK2VK.put(new Integer(X11Defs.XK_F24), new Integer(KeyEvent.VK_F24));
        tableXK2VK.put(new Integer(X11Defs.XK_Caps_Lock), new Integer(KeyEvent.VK_CAPS_LOCK));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_grave), new Integer(KeyEvent.VK_DEAD_GRAVE));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_acute), new Integer(KeyEvent.VK_DEAD_ACUTE));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_circumflex), new Integer(KeyEvent.VK_DEAD_CIRCUMFLEX));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_tilde), new Integer(KeyEvent.VK_DEAD_TILDE));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_macron), new Integer(KeyEvent.VK_DEAD_MACRON));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_breve), new Integer(KeyEvent.VK_DEAD_BREVE));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_abovedot), new Integer(KeyEvent.VK_DEAD_ABOVEDOT));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_diaeresis), new Integer(KeyEvent.VK_DEAD_DIAERESIS));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_abovering), new Integer(KeyEvent.VK_DEAD_ABOVERING));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_doubleacute), new Integer(KeyEvent.VK_DEAD_DOUBLEACUTE));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_caron), new Integer(KeyEvent.VK_DEAD_CARON));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_cedilla), new Integer(KeyEvent.VK_DEAD_CEDILLA));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_ogonek), new Integer(KeyEvent.VK_DEAD_OGONEK));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_iota), new Integer(KeyEvent.VK_DEAD_IOTA));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_voiced_sound), new Integer(KeyEvent.VK_DEAD_VOICED_SOUND));
        tableXK2VK.put(new Integer(X11Defs.XK_dead_semivoiced_sound), new Integer(KeyEvent.VK_DEAD_SEMIVOICED_SOUND));
        tableXK2VK.put(new Integer(X11Defs.XK_space), new Integer(KeyEvent.VK_SPACE));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Space), new Integer(KeyEvent.VK_SPACE));
        tableXK2VK.put(new Integer(X11Defs.XK_quotedbl), new Integer(KeyEvent.VK_QUOTEDBL));
        tableXK2VK.put(new Integer(X11Defs.XK_dollar), new Integer(KeyEvent.VK_DOLLAR));
        tableXK2VK.put(new Integer(X11Defs.XK_ampersand), new Integer(KeyEvent.VK_AMPERSAND));
        tableXK2VK.put(new Integer(X11Defs.XK_asterisk), new Integer(KeyEvent.VK_ASTERISK));
        tableXK2VK.put(new Integer(X11Defs.XK_plus), new Integer(KeyEvent.VK_PLUS));
        tableXK2VK.put(new Integer(X11Defs.XK_comma), new Integer(KeyEvent.VK_COMMA));
        tableXK2VK.put(new Integer(X11Defs.XK_minus), new Integer(KeyEvent.VK_MINUS));
        tableXK2VK.put(new Integer(X11Defs.XK_period), new Integer(KeyEvent.VK_PERIOD));
        tableXK2VK.put(new Integer(X11Defs.XK_slash), new Integer(KeyEvent.VK_SLASH));
        tableXK2VK.put(new Integer(X11Defs.XK_0), new Integer(KeyEvent.VK_0));
        tableXK2VK.put(new Integer(X11Defs.XK_1), new Integer(KeyEvent.VK_1));
        tableXK2VK.put(new Integer(X11Defs.XK_2), new Integer(KeyEvent.VK_2));
        tableXK2VK.put(new Integer(X11Defs.XK_3), new Integer(KeyEvent.VK_3));
        tableXK2VK.put(new Integer(X11Defs.XK_4), new Integer(KeyEvent.VK_4));
        tableXK2VK.put(new Integer(X11Defs.XK_5), new Integer(KeyEvent.VK_5));
        tableXK2VK.put(new Integer(X11Defs.XK_6), new Integer(KeyEvent.VK_6));
        tableXK2VK.put(new Integer(X11Defs.XK_7), new Integer(KeyEvent.VK_7));
        tableXK2VK.put(new Integer(X11Defs.XK_8), new Integer(KeyEvent.VK_8));
        tableXK2VK.put(new Integer(X11Defs.XK_9), new Integer(KeyEvent.VK_9));
        tableXK2VK.put(new Integer(X11Defs.XK_colon), new Integer(KeyEvent.VK_COLON));
        tableXK2VK.put(new Integer(X11Defs.XK_semicolon), new Integer(KeyEvent.VK_SEMICOLON));
        tableXK2VK.put(new Integer(X11Defs.XK_less), new Integer(KeyEvent.VK_LESS));
        tableXK2VK.put(new Integer(X11Defs.XK_greater), new Integer(KeyEvent.VK_GREATER));
        tableXK2VK.put(new Integer(X11Defs.XK_at), new Integer(KeyEvent.VK_AT));
        tableXK2VK.put(new Integer(X11Defs.XK_A), new Integer(KeyEvent.VK_A));
        tableXK2VK.put(new Integer(X11Defs.XK_B), new Integer(KeyEvent.VK_B));
        tableXK2VK.put(new Integer(X11Defs.XK_C), new Integer(KeyEvent.VK_C));
        tableXK2VK.put(new Integer(X11Defs.XK_D), new Integer(KeyEvent.VK_D));
        tableXK2VK.put(new Integer(X11Defs.XK_E), new Integer(KeyEvent.VK_E));
        tableXK2VK.put(new Integer(X11Defs.XK_F), new Integer(KeyEvent.VK_F));
        tableXK2VK.put(new Integer(X11Defs.XK_G), new Integer(KeyEvent.VK_G));
        tableXK2VK.put(new Integer(X11Defs.XK_H), new Integer(KeyEvent.VK_H));
        tableXK2VK.put(new Integer(X11Defs.XK_I), new Integer(KeyEvent.VK_I));
        tableXK2VK.put(new Integer(X11Defs.XK_J), new Integer(KeyEvent.VK_J));
        tableXK2VK.put(new Integer(X11Defs.XK_K), new Integer(KeyEvent.VK_K));
        tableXK2VK.put(new Integer(X11Defs.XK_L), new Integer(KeyEvent.VK_L));
        tableXK2VK.put(new Integer(X11Defs.XK_M), new Integer(KeyEvent.VK_M));
        tableXK2VK.put(new Integer(X11Defs.XK_N), new Integer(KeyEvent.VK_N));
        tableXK2VK.put(new Integer(X11Defs.XK_O), new Integer(KeyEvent.VK_O));
        tableXK2VK.put(new Integer(X11Defs.XK_P), new Integer(KeyEvent.VK_P));
        tableXK2VK.put(new Integer(X11Defs.XK_Q), new Integer(KeyEvent.VK_Q));
        tableXK2VK.put(new Integer(X11Defs.XK_R), new Integer(KeyEvent.VK_R));
        tableXK2VK.put(new Integer(X11Defs.XK_S), new Integer(KeyEvent.VK_S));
        tableXK2VK.put(new Integer(X11Defs.XK_T), new Integer(KeyEvent.VK_T));
        tableXK2VK.put(new Integer(X11Defs.XK_U), new Integer(KeyEvent.VK_U));
        tableXK2VK.put(new Integer(X11Defs.XK_V), new Integer(KeyEvent.VK_V));
        tableXK2VK.put(new Integer(X11Defs.XK_W), new Integer(KeyEvent.VK_W));
        tableXK2VK.put(new Integer(X11Defs.XK_X), new Integer(KeyEvent.VK_X));
        tableXK2VK.put(new Integer(X11Defs.XK_Y), new Integer(KeyEvent.VK_Y));
        tableXK2VK.put(new Integer(X11Defs.XK_Z), new Integer(KeyEvent.VK_Z));
        tableXK2VK.put(new Integer(X11Defs.XK_underscore), new Integer(KeyEvent.VK_UNDERSCORE));
        tableXK2VK.put(new Integer(X11Defs.XK_a), new Integer(KeyEvent.VK_A));
        tableXK2VK.put(new Integer(X11Defs.XK_b), new Integer(KeyEvent.VK_B));
        tableXK2VK.put(new Integer(X11Defs.XK_c), new Integer(KeyEvent.VK_C));
        tableXK2VK.put(new Integer(X11Defs.XK_d), new Integer(KeyEvent.VK_D));
        tableXK2VK.put(new Integer(X11Defs.XK_e), new Integer(KeyEvent.VK_E));
        tableXK2VK.put(new Integer(X11Defs.XK_f), new Integer(KeyEvent.VK_F));
        tableXK2VK.put(new Integer(X11Defs.XK_g), new Integer(KeyEvent.VK_G));
        tableXK2VK.put(new Integer(X11Defs.XK_h), new Integer(KeyEvent.VK_H));
        tableXK2VK.put(new Integer(X11Defs.XK_i), new Integer(KeyEvent.VK_I));
        tableXK2VK.put(new Integer(X11Defs.XK_j), new Integer(KeyEvent.VK_J));
        tableXK2VK.put(new Integer(X11Defs.XK_k), new Integer(KeyEvent.VK_K));
        tableXK2VK.put(new Integer(X11Defs.XK_l), new Integer(KeyEvent.VK_L));
        tableXK2VK.put(new Integer(X11Defs.XK_m), new Integer(KeyEvent.VK_M));
        tableXK2VK.put(new Integer(X11Defs.XK_n), new Integer(KeyEvent.VK_N));
        tableXK2VK.put(new Integer(X11Defs.XK_o), new Integer(KeyEvent.VK_O));
        tableXK2VK.put(new Integer(X11Defs.XK_p), new Integer(KeyEvent.VK_P));
        tableXK2VK.put(new Integer(X11Defs.XK_q), new Integer(KeyEvent.VK_Q));
        tableXK2VK.put(new Integer(X11Defs.XK_r), new Integer(KeyEvent.VK_R));
        tableXK2VK.put(new Integer(X11Defs.XK_s), new Integer(KeyEvent.VK_S));
        tableXK2VK.put(new Integer(X11Defs.XK_t), new Integer(KeyEvent.VK_T));
        tableXK2VK.put(new Integer(X11Defs.XK_u), new Integer(KeyEvent.VK_U));
        tableXK2VK.put(new Integer(X11Defs.XK_v), new Integer(KeyEvent.VK_V));
        tableXK2VK.put(new Integer(X11Defs.XK_w), new Integer(KeyEvent.VK_W));
        tableXK2VK.put(new Integer(X11Defs.XK_x), new Integer(KeyEvent.VK_X));
        tableXK2VK.put(new Integer(X11Defs.XK_y), new Integer(KeyEvent.VK_Y));
        tableXK2VK.put(new Integer(X11Defs.XK_z), new Integer(KeyEvent.VK_Z));
        tableXK2VK.put(new Integer(X11Defs.XK_braceleft), new Integer(KeyEvent.VK_BRACELEFT));
        tableXK2VK.put(new Integer(X11Defs.XK_braceright), new Integer(KeyEvent.VK_BRACERIGHT));
        tableXK2VK.put(new Integer(X11Defs.XK_multiply), new Integer(KeyEvent.VK_MULTIPLY));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Multiply), new Integer(KeyEvent.VK_MULTIPLY));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Decimal), new Integer(KeyEvent.VK_DECIMAL));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Divide), new Integer(KeyEvent.VK_DIVIDE));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Subtract), new Integer(KeyEvent.VK_SUBTRACT));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Separator), new Integer(KeyEvent.VK_SEPARATOR));
        tableXK2VK.put(new Integer(X11Defs.XK_Meta_L), new Integer(KeyEvent.VK_META));
        tableXK2VK.put(new Integer(X11Defs.XK_Meta_R), new Integer(KeyEvent.VK_META));
        tableXK2VK.put(new Integer(X11Defs.XK_Alt_L), new Integer(KeyEvent.VK_ALT));
        tableXK2VK.put(new Integer(X11Defs.XK_Alt_R), new Integer(KeyEvent.VK_ALT_GRAPH));
        tableXK2VK.put(new Integer(X11Defs.XK_Shift_L), new Integer(KeyEvent.VK_SHIFT));
        tableXK2VK.put(new Integer(X11Defs.XK_Shift_R), new Integer(KeyEvent.VK_SHIFT));
        tableXK2VK.put(new Integer(X11Defs.XK_Control_L), new Integer(KeyEvent.VK_CONTROL));
        tableXK2VK.put(new Integer(X11Defs.XK_Control_R), new Integer(KeyEvent.VK_CONTROL));
        tableXK2VK.put(new Integer(X11Defs.XK_Return), new Integer(KeyEvent.VK_ENTER));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Enter), new Integer(KeyEvent.VK_ENTER));
        tableXK2VK.put(new Integer(X11Defs.XK_equal), new Integer(KeyEvent.VK_EQUALS));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Equal), new Integer(KeyEvent.VK_EQUALS));
        tableXK2VK.put(new Integer(X11Defs.XK_exclam), new Integer(KeyEvent.VK_EXCLAMATION_MARK));
        tableXK2VK.put(new Integer(X11Defs.XK_exclamdown), new Integer(KeyEvent.VK_INVERTED_EXCLAMATION_MARK));
        tableXK2VK.put(new Integer(X11Defs.XK_bracketright), new Integer(KeyEvent.VK_CLOSE_BRACKET));
        tableXK2VK.put(new Integer(X11Defs.XK_bracketleft), new Integer(KeyEvent.VK_OPEN_BRACKET));
        tableXK2VK.put(new Integer(X11Defs.XK_KP_Add), new Integer(KeyEvent.VK_ADD));
        tableXK2VK.put(new Integer(X11Defs.XK_quoteright), new Integer(KeyEvent.VK_QUOTE));
        tableXK2VK.put(new Integer(X11Defs.XK_quoteleft), new Integer(KeyEvent.VK_BACK_QUOTE));
        tableXK2VK.put(new Integer(X11Defs.XK_Kana_Shift), new Integer(KeyEvent.VK_KANA));
        tableXK2VK.put(new Integer(X11Defs.XK_MultipleCandidate), new Integer(KeyEvent.VK_ALL_CANDIDATES));
        tableXK2VK.put(new Integer(X11Defs.XK_PreviousCandidate), new Integer(KeyEvent.VK_PREVIOUS_CANDIDATE));
        tableXK2VK.put(new Integer(X11Defs.XK_backslash), new Integer(KeyEvent.VK_BACK_SLASH));
        tableXK2VK.put(new Integer(X11Defs.XK_BackSpace), new Integer(KeyEvent.VK_BACK_SPACE));
        tableXK2VK.put(new Integer(X11Defs.XK_asciicircum), new Integer(KeyEvent.VK_CIRCUMFLEX));
        tableXK2VK.put(new Integer(X11Defs.XK_Codeinput), new Integer(KeyEvent.VK_CODE_INPUT));
        tableXK2VK.put(new Integer(X11Defs.XK_EuroSign), new Integer(KeyEvent.VK_EURO_SIGN));
        tableXK2VK.put(new Integer(X11Defs.XK_Hiragana), new Integer(KeyEvent.VK_JAPANESE_HIRAGANA));
        tableXK2VK.put(new Integer(X11Defs.XK_Katakana), new Integer(KeyEvent.VK_JAPANESE_KATAKANA));
        tableXK2VK.put(new Integer(X11Defs.XK_parenleft), new Integer(KeyEvent.VK_LEFT_PARENTHESIS));
        tableXK2VK.put(new Integer(X11Defs.XK_parenright), new Integer(KeyEvent.VK_RIGHT_PARENTHESIS));
        tableXK2VK.put(new Integer(X11Defs.XK_Mode_switch), new Integer(KeyEvent.VK_MODECHANGE));
        tableXK2VK.put(new Integer(X11Defs.XK_numbersign), new Integer(KeyEvent.VK_NUMBER_SIGN));
        tableXK2VK.put(new Integer(X11Defs.XK_percent), new Integer(KeyEvent.VK_5));
        tableXK2VK.put(new Integer(X11Defs.XK_question), new Integer(KeyEvent.VK_SLASH));
        tableXK2VK.put(new Integer(X11Defs.XK_bar), new Integer(KeyEvent.VK_BACK_SLASH));

        tableVK2XK = new HashMap();
        tableVK2XK.put(new Integer(KeyEvent.VK_TAB), new Integer(X11Defs.XK_Tab));
        tableVK2XK.put(new Integer(KeyEvent.VK_CLEAR), new Integer(X11Defs.XK_Clear));
        tableVK2XK.put(new Integer(KeyEvent.VK_PAUSE), new Integer(X11Defs.XK_Pause));
        tableVK2XK.put(new Integer(KeyEvent.VK_SCROLL_LOCK), new Integer(X11Defs.XK_Scroll_Lock));
        tableVK2XK.put(new Integer(KeyEvent.VK_ESCAPE), new Integer(X11Defs.XK_Escape));
        tableVK2XK.put(new Integer(KeyEvent.VK_DELETE), new Integer(X11Defs.XK_Delete));
        tableVK2XK.put(new Integer(KeyEvent.VK_KANJI), new Integer(X11Defs.XK_Kanji));
        tableVK2XK.put(new Integer(KeyEvent.VK_HIRAGANA), new Integer(X11Defs.XK_Hiragana));
        tableVK2XK.put(new Integer(KeyEvent.VK_KATAKANA), new Integer(X11Defs.XK_Katakana));
        tableVK2XK.put(new Integer(KeyEvent.VK_KANA_LOCK), new Integer(X11Defs.XK_Kana_Lock));
        tableVK2XK.put(new Integer(KeyEvent.VK_HOME), new Integer(X11Defs.XK_Home));
        tableVK2XK.put(new Integer(KeyEvent.VK_LEFT), new Integer(X11Defs.XK_Left));
        tableVK2XK.put(new Integer(KeyEvent.VK_UP), new Integer(X11Defs.XK_Up));
        tableVK2XK.put(new Integer(KeyEvent.VK_RIGHT), new Integer(X11Defs.XK_Right));
        tableVK2XK.put(new Integer(KeyEvent.VK_DOWN), new Integer(X11Defs.XK_Down));
        tableVK2XK.put(new Integer(KeyEvent.VK_PAGE_UP), new Integer(X11Defs.XK_Page_Up));
        tableVK2XK.put(new Integer(KeyEvent.VK_PAGE_DOWN), new Integer(X11Defs.XK_Page_Down));
        tableVK2XK.put(new Integer(KeyEvent.VK_END), new Integer(X11Defs.XK_End));
        tableVK2XK.put(new Integer(KeyEvent.VK_INSERT), new Integer(X11Defs.XK_Insert));
        tableVK2XK.put(new Integer(KeyEvent.VK_UNDO), new Integer(X11Defs.XK_Undo));
        tableVK2XK.put(new Integer(KeyEvent.VK_FIND), new Integer(X11Defs.XK_Find));
        tableVK2XK.put(new Integer(KeyEvent.VK_CANCEL), new Integer(X11Defs.XK_Cancel));
        tableVK2XK.put(new Integer(KeyEvent.VK_HELP), new Integer(X11Defs.XK_Help));
        tableVK2XK.put(new Integer(KeyEvent.VK_NUM_LOCK), new Integer(X11Defs.XK_Num_Lock));
        tableVK2XK.put(new Integer(KeyEvent.VK_F1), new Integer(X11Defs.XK_F1));
        tableVK2XK.put(new Integer(KeyEvent.VK_F2), new Integer(X11Defs.XK_F2));
        tableVK2XK.put(new Integer(KeyEvent.VK_F3), new Integer(X11Defs.XK_F3));
        tableVK2XK.put(new Integer(KeyEvent.VK_F4), new Integer(X11Defs.XK_F4));
        tableVK2XK.put(new Integer(KeyEvent.VK_F5), new Integer(X11Defs.XK_F5));
        tableVK2XK.put(new Integer(KeyEvent.VK_F6), new Integer(X11Defs.XK_F6));
        tableVK2XK.put(new Integer(KeyEvent.VK_F7), new Integer(X11Defs.XK_F7));
        tableVK2XK.put(new Integer(KeyEvent.VK_F8), new Integer(X11Defs.XK_F8));
        tableVK2XK.put(new Integer(KeyEvent.VK_F9), new Integer(X11Defs.XK_F9));
        tableVK2XK.put(new Integer(KeyEvent.VK_F10), new Integer(X11Defs.XK_F10));
        tableVK2XK.put(new Integer(KeyEvent.VK_F11), new Integer(X11Defs.XK_F11));
        tableVK2XK.put(new Integer(KeyEvent.VK_F12), new Integer(X11Defs.XK_F12));
        tableVK2XK.put(new Integer(KeyEvent.VK_F13), new Integer(X11Defs.XK_F13));
        tableVK2XK.put(new Integer(KeyEvent.VK_F14), new Integer(X11Defs.XK_F14));
        tableVK2XK.put(new Integer(KeyEvent.VK_F15), new Integer(X11Defs.XK_F15));
        tableVK2XK.put(new Integer(KeyEvent.VK_F16), new Integer(X11Defs.XK_F16));
        tableVK2XK.put(new Integer(KeyEvent.VK_F17), new Integer(X11Defs.XK_F17));
        tableVK2XK.put(new Integer(KeyEvent.VK_F18), new Integer(X11Defs.XK_F18));
        tableVK2XK.put(new Integer(KeyEvent.VK_F19), new Integer(X11Defs.XK_F19));
        tableVK2XK.put(new Integer(KeyEvent.VK_F20), new Integer(X11Defs.XK_F20));
        tableVK2XK.put(new Integer(KeyEvent.VK_F21), new Integer(X11Defs.XK_F21));
        tableVK2XK.put(new Integer(KeyEvent.VK_F22), new Integer(X11Defs.XK_F22));
        tableVK2XK.put(new Integer(KeyEvent.VK_F23), new Integer(X11Defs.XK_F23));
        tableVK2XK.put(new Integer(KeyEvent.VK_F24), new Integer(X11Defs.XK_F24));
        tableVK2XK.put(new Integer(KeyEvent.VK_CAPS_LOCK), new Integer(X11Defs.XK_Caps_Lock));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_GRAVE), new Integer(X11Defs.XK_dead_grave));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_ACUTE), new Integer(X11Defs.XK_dead_acute));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_CIRCUMFLEX), new Integer(X11Defs.XK_dead_circumflex));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_TILDE), new Integer(X11Defs.XK_dead_tilde));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_MACRON), new Integer(X11Defs.XK_dead_macron));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_BREVE), new Integer(X11Defs.XK_dead_breve));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_ABOVEDOT), new Integer(X11Defs.XK_dead_abovedot));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_DIAERESIS), new Integer(X11Defs.XK_dead_diaeresis));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_ABOVERING), new Integer(X11Defs.XK_dead_abovering));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_DOUBLEACUTE), new Integer(X11Defs.XK_dead_doubleacute));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_CARON), new Integer(X11Defs.XK_dead_caron));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_CEDILLA), new Integer(X11Defs.XK_dead_cedilla));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_OGONEK), new Integer(X11Defs.XK_dead_ogonek));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_IOTA), new Integer(X11Defs.XK_dead_iota));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_VOICED_SOUND), new Integer(X11Defs.XK_dead_voiced_sound));
        tableVK2XK.put(new Integer(KeyEvent.VK_DEAD_SEMIVOICED_SOUND), new Integer(X11Defs.XK_dead_semivoiced_sound));
        tableVK2XK.put(new Integer(KeyEvent.VK_SPACE), new Integer(X11Defs.XK_space));
        tableVK2XK.put(new Integer(KeyEvent.VK_QUOTEDBL), new Integer(X11Defs.XK_quotedbl));
        tableVK2XK.put(new Integer(KeyEvent.VK_DOLLAR), new Integer(X11Defs.XK_dollar));
        tableVK2XK.put(new Integer(KeyEvent.VK_AMPERSAND), new Integer(X11Defs.XK_ampersand));
        tableVK2XK.put(new Integer(KeyEvent.VK_ASTERISK), new Integer(X11Defs.XK_asterisk));
        tableVK2XK.put(new Integer(KeyEvent.VK_PLUS), new Integer(X11Defs.XK_plus));
        tableVK2XK.put(new Integer(KeyEvent.VK_COMMA), new Integer(X11Defs.XK_comma));
        tableVK2XK.put(new Integer(KeyEvent.VK_MINUS), new Integer(X11Defs.XK_minus));
        tableVK2XK.put(new Integer(KeyEvent.VK_PERIOD), new Integer(X11Defs.XK_period));
        tableVK2XK.put(new Integer(KeyEvent.VK_SLASH), new Integer(X11Defs.XK_slash));
        tableVK2XK.put(new Integer(KeyEvent.VK_0), new Integer(X11Defs.XK_0));
        tableVK2XK.put(new Integer(KeyEvent.VK_1), new Integer(X11Defs.XK_1));
        tableVK2XK.put(new Integer(KeyEvent.VK_2), new Integer(X11Defs.XK_2));
        tableVK2XK.put(new Integer(KeyEvent.VK_3), new Integer(X11Defs.XK_3));
        tableVK2XK.put(new Integer(KeyEvent.VK_4), new Integer(X11Defs.XK_4));
        tableVK2XK.put(new Integer(KeyEvent.VK_5), new Integer(X11Defs.XK_5));
        tableVK2XK.put(new Integer(KeyEvent.VK_6), new Integer(X11Defs.XK_6));
        tableVK2XK.put(new Integer(KeyEvent.VK_7), new Integer(X11Defs.XK_7));
        tableVK2XK.put(new Integer(KeyEvent.VK_8), new Integer(X11Defs.XK_8));
        tableVK2XK.put(new Integer(KeyEvent.VK_9), new Integer(X11Defs.XK_9));
        tableVK2XK.put(new Integer(KeyEvent.VK_COLON), new Integer(X11Defs.XK_colon));
        tableVK2XK.put(new Integer(KeyEvent.VK_SEMICOLON), new Integer(X11Defs.XK_semicolon));
        tableVK2XK.put(new Integer(KeyEvent.VK_LESS), new Integer(X11Defs.XK_less));
        tableVK2XK.put(new Integer(KeyEvent.VK_GREATER), new Integer(X11Defs.XK_greater));
        tableVK2XK.put(new Integer(KeyEvent.VK_AT), new Integer(X11Defs.XK_at));
        tableVK2XK.put(new Integer(KeyEvent.VK_A), new Integer(X11Defs.XK_A));
        tableVK2XK.put(new Integer(KeyEvent.VK_B), new Integer(X11Defs.XK_B));
        tableVK2XK.put(new Integer(KeyEvent.VK_C), new Integer(X11Defs.XK_C));
        tableVK2XK.put(new Integer(KeyEvent.VK_D), new Integer(X11Defs.XK_D));
        tableVK2XK.put(new Integer(KeyEvent.VK_E), new Integer(X11Defs.XK_E));
        tableVK2XK.put(new Integer(KeyEvent.VK_F), new Integer(X11Defs.XK_F));
        tableVK2XK.put(new Integer(KeyEvent.VK_G), new Integer(X11Defs.XK_G));
        tableVK2XK.put(new Integer(KeyEvent.VK_H), new Integer(X11Defs.XK_H));
        tableVK2XK.put(new Integer(KeyEvent.VK_I), new Integer(X11Defs.XK_I));
        tableVK2XK.put(new Integer(KeyEvent.VK_J), new Integer(X11Defs.XK_J));
        tableVK2XK.put(new Integer(KeyEvent.VK_K), new Integer(X11Defs.XK_K));
        tableVK2XK.put(new Integer(KeyEvent.VK_L), new Integer(X11Defs.XK_L));
        tableVK2XK.put(new Integer(KeyEvent.VK_M), new Integer(X11Defs.XK_M));
        tableVK2XK.put(new Integer(KeyEvent.VK_N), new Integer(X11Defs.XK_N));
        tableVK2XK.put(new Integer(KeyEvent.VK_O), new Integer(X11Defs.XK_O));
        tableVK2XK.put(new Integer(KeyEvent.VK_P), new Integer(X11Defs.XK_P));
        tableVK2XK.put(new Integer(KeyEvent.VK_Q), new Integer(X11Defs.XK_Q));
        tableVK2XK.put(new Integer(KeyEvent.VK_R), new Integer(X11Defs.XK_R));
        tableVK2XK.put(new Integer(KeyEvent.VK_S), new Integer(X11Defs.XK_S));
        tableVK2XK.put(new Integer(KeyEvent.VK_T), new Integer(X11Defs.XK_T));
        tableVK2XK.put(new Integer(KeyEvent.VK_U), new Integer(X11Defs.XK_U));
        tableVK2XK.put(new Integer(KeyEvent.VK_V), new Integer(X11Defs.XK_V));
        tableVK2XK.put(new Integer(KeyEvent.VK_W), new Integer(X11Defs.XK_W));
        tableVK2XK.put(new Integer(KeyEvent.VK_X), new Integer(X11Defs.XK_X));
        tableVK2XK.put(new Integer(KeyEvent.VK_Y), new Integer(X11Defs.XK_Y));
        tableVK2XK.put(new Integer(KeyEvent.VK_Z), new Integer(X11Defs.XK_Z));
        tableVK2XK.put(new Integer(KeyEvent.VK_UNDERSCORE), new Integer(X11Defs.XK_underscore));
        tableVK2XK.put(new Integer(KeyEvent.VK_BRACELEFT), new Integer(X11Defs.XK_braceleft));
        tableVK2XK.put(new Integer(KeyEvent.VK_BRACERIGHT), new Integer(X11Defs.XK_braceright));
        tableVK2XK.put(new Integer(KeyEvent.VK_MULTIPLY), new Integer(X11Defs.XK_multiply));
        tableVK2XK.put(new Integer(KeyEvent.VK_DECIMAL), new Integer(X11Defs.XK_KP_Decimal));
        tableVK2XK.put(new Integer(KeyEvent.VK_META), new Integer(X11Defs.XK_Meta_L));
        tableVK2XK.put(new Integer(KeyEvent.VK_ALT), new Integer(X11Defs.XK_Alt_L));
        tableVK2XK.put(new Integer(KeyEvent.VK_ALT_GRAPH), new Integer(X11Defs.XK_Alt_R));
        tableVK2XK.put(new Integer(KeyEvent.VK_SHIFT), new Integer(X11Defs.XK_Shift_L));
        tableVK2XK.put(new Integer(KeyEvent.VK_CONTROL), new Integer(X11Defs.XK_Control_L));
        tableVK2XK.put(new Integer(KeyEvent.VK_ENTER), new Integer(X11Defs.XK_Return));
        tableVK2XK.put(new Integer(KeyEvent.VK_EQUALS), new Integer(X11Defs.XK_equal));
        tableVK2XK.put(new Integer(KeyEvent.VK_EXCLAMATION_MARK), new Integer(X11Defs.XK_exclam));
        tableVK2XK.put(new Integer(KeyEvent.VK_INVERTED_EXCLAMATION_MARK), new Integer(X11Defs.XK_exclamdown));
        tableVK2XK.put(new Integer(KeyEvent.VK_CLOSE_BRACKET), new Integer(X11Defs.XK_bracketright));
        tableVK2XK.put(new Integer(KeyEvent.VK_OPEN_BRACKET), new Integer(X11Defs.XK_bracketleft));
        tableVK2XK.put(new Integer(KeyEvent.VK_QUOTE), new Integer(X11Defs.XK_quoteright));
        tableVK2XK.put(new Integer(KeyEvent.VK_BACK_QUOTE), new Integer(X11Defs.XK_quoteleft));
        tableVK2XK.put(new Integer(KeyEvent.VK_KANA), new Integer(X11Defs.XK_Kana_Shift));
        tableVK2XK.put(new Integer(KeyEvent.VK_ALL_CANDIDATES), new Integer(X11Defs.XK_MultipleCandidate));
        tableVK2XK.put(new Integer(KeyEvent.VK_PREVIOUS_CANDIDATE), new Integer(X11Defs.XK_PreviousCandidate));
        tableVK2XK.put(new Integer(KeyEvent.VK_BACK_SLASH), new Integer(X11Defs.XK_backslash));
        tableVK2XK.put(new Integer(KeyEvent.VK_BACK_SPACE), new Integer(X11Defs.XK_BackSpace));
        tableVK2XK.put(new Integer(KeyEvent.VK_CIRCUMFLEX), new Integer(X11Defs.XK_asciicircum));
        tableVK2XK.put(new Integer(KeyEvent.VK_CODE_INPUT), new Integer(X11Defs.XK_Codeinput));
        tableVK2XK.put(new Integer(KeyEvent.VK_EURO_SIGN), new Integer(X11Defs.XK_EuroSign));
        tableVK2XK.put(new Integer(KeyEvent.VK_JAPANESE_HIRAGANA), new Integer(X11Defs.XK_Hiragana));
        tableVK2XK.put(new Integer(KeyEvent.VK_JAPANESE_KATAKANA), new Integer(X11Defs.XK_Katakana));
        tableVK2XK.put(new Integer(KeyEvent.VK_LEFT_PARENTHESIS), new Integer(X11Defs.XK_parenleft));
        tableVK2XK.put(new Integer(KeyEvent.VK_RIGHT_PARENTHESIS), new Integer(X11Defs.XK_parenright));
        tableVK2XK.put(new Integer(KeyEvent.VK_MODECHANGE), new Integer(X11Defs.XK_Mode_switch));
        tableVK2XK.put(new Integer(KeyEvent.VK_NUMBER_SIGN), new Integer(X11Defs.XK_numbersign));
        tableVK2XK.put(new Integer(KeyEvent.VK_UNDEFINED), new Integer(X11Defs.NoSymbol));
    }

    /**
     * Translates virtual key to KeySym.
     * @param vk virtual key
     * @return KeySym or NoSymbol
     */
    static int VK2XK(int vk) {
        Object xk = tableVK2XK.get(new Integer(vk));

        if (xk != null) {
            return ((Integer)xk).intValue();
        }

        return X11Defs.NoSymbol;
    }

    /**
     * Translates linux key event to internal structure KeyInfo and returns it.
     * @param event instance of XKeyEvent
     * @return instance of KeyInfo
     */
    static KeyInfo translateEvent(X11.XKeyEvent event) {
        KeyInfo res = new KeyInfo();
        int nBytes = 255;
        Int8Pointer buffer = bridge.createInt8Pointer(nBytes, false);
        buffer.fill((byte)0, nBytes);
        CLongPointer keySymPtr = bridge.createCLongPointer(1, false);

        nBytes = x11.XLookupString(event, buffer, nBytes, keySymPtr, null);

        if (nBytes > 0) {
            String str = buffer.getStringUTF();
            res.keyChars.append(str);
        } else {
            res.keyChars.append(KeyEvent.CHAR_UNDEFINED);
        }
        int keySym = (int)keySymPtr.get(0);

        if (tableXK2VK.containsKey(new Integer(keySym))) {
            res.vKey = ((Integer) tableXK2VK.get(new Integer(keySym))).intValue();
            res.keyLocation = deriveLocation(keySym);
        } else {
            res.vKey = KeyEvent.VK_UNDEFINED;
            res.keyLocation = KeyEvent.KEY_LOCATION_STANDARD;
        }

        return res;
    }

    private static int deriveLocation(int keySym) {
        switch (keySym) {
        case X11Defs.XK_Alt_L:
        case X11Defs.XK_Meta_L:
        case X11Defs.XK_Shift_L:
        case X11Defs.XK_Control_L:
            return KeyEvent.KEY_LOCATION_LEFT;
        case X11Defs.XK_Alt_R:
        case X11Defs.XK_Meta_R:
        case X11Defs.XK_Shift_R:
        case X11Defs.XK_Control_R:
            return KeyEvent.KEY_LOCATION_RIGHT;
        case X11Defs.XK_KP_Tab:
        case X11Defs.XK_KP_Delete:
        case X11Defs.XK_KP_Home:
        case X11Defs.XK_KP_Page_Up:
        case X11Defs.XK_KP_Page_Down:
        case X11Defs.XK_KP_End:
        case X11Defs.XK_KP_Insert:
        case X11Defs.XK_KP_Left:
        case X11Defs.XK_KP_Up:
        case X11Defs.XK_KP_Right:
        case X11Defs.XK_KP_Down:
        case X11Defs.XK_KP_0:
        case X11Defs.XK_KP_1:
        case X11Defs.XK_KP_2:
        case X11Defs.XK_KP_3:
        case X11Defs.XK_KP_4:
        case X11Defs.XK_KP_5:
        case X11Defs.XK_KP_6:
        case X11Defs.XK_KP_7:
        case X11Defs.XK_KP_8:
        case X11Defs.XK_KP_9:
        case X11Defs.XK_KP_F1:
        case X11Defs.XK_KP_F2:
        case X11Defs.XK_KP_F3:
        case X11Defs.XK_KP_F4:
        case X11Defs.XK_KP_Space:
        case X11Defs.XK_KP_Multiply:
        case X11Defs.XK_KP_Decimal:
        case X11Defs.XK_KP_Divide:
        case X11Defs.XK_KP_Subtract:
        case X11Defs.XK_KP_Separator:
        case X11Defs.XK_KP_Enter:
        case X11Defs.XK_KP_Equal:
        case X11Defs.XK_KP_Add:
            return KeyEvent.KEY_LOCATION_NUMPAD;
        default:
            return KeyEvent.KEY_LOCATION_STANDARD;
        }
    }

}
