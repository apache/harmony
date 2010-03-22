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
  * @author Ilya Berezhniuk
  *
  */

#include <windows.h>
#include "exceptions.h"

// C declarations
extern "C" {
JNIEXPORT jboolean JNICALL Java_org_apache_harmony_awt_wtk_windows_WinWTK_getLockingState
    (JNIEnv *env, jclass cls, jint keyCode);
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_wtk_windows_WinWTK_setLockingState
    (JNIEnv *env, jclass cls, jint keyCode, jboolean on);
}


static inline bool is_kana_present()
{
    return (MapVirtualKey(VK_KANA, 0) != 0);
}

// Translate Java VK to Win32 VK
static inline int get_virt_locking_key(jint code)
{
    switch (code)
    {
        case 20:/*VK_CAPS_LOCK*/
            return VK_CAPITAL;
        case 144:/*VK_NUM_LOCK*/
            return VK_NUMLOCK;
        case 145:/*VK_SCROLL_LOCK*/
            return VK_SCROLL;
        case 262:/*VK_KANA_LOCK*/
            return VK_KANA;
    }

    return 0;
}


/*
 * Class:     org_apache_harmony_awt_wtk_windows_WinWTK
 * Method:    nativeGetLockingState
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_apache_harmony_awt_wtk_windows_WinWTK_getLockingState
    (JNIEnv *env, jclass cls, jint keyCode)
{
    int vk = get_virt_locking_key(keyCode);

    if (vk == 0)
        return JNI_FALSE;

    if (vk == VK_KANA && !is_kana_present())
    {
        throwNewExceptionByName(env, "java/lang/UnsupportedOperationException",
                                       "Keyboard doesn't have KANA key");
        return JNI_FALSE; // We shouldn't reach this return
    }

    return (GetKeyState(vk) & 1) ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     org_apache_harmony_awt_wtk_windows_WinWTK
 * Method:    nativeSetLockingState
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_wtk_windows_WinWTK_setLockingState
    (JNIEnv *env, jclass cls, jint keyCode, jboolean on)
{
    int vk = get_virt_locking_key(keyCode);

    if (vk == 0)
        return;

    if (vk == VK_KANA && !is_kana_present())
    {
        throwNewExceptionByName(env, "java/lang/UnsupportedOperationException",
                                       "Keyboard doesn't have KANA key");
        return; // We shouldn't reach this return
    }

    bool enabled = ((GetKeyState(vk) & 1) != 0);

    if ((enabled && on) || (!enabled && !on))
        return;

    LPARAM extra = GetMessageExtraInfo();

    INPUT inp[2] = {{INPUT_KEYBOARD}, {INPUT_KEYBOARD}};
    inp[0].ki.wVk = inp[1].ki.wVk = vk;
    inp[0].ki.time = inp[1].ki.time = 0;
    inp[0].ki.dwExtraInfo = inp[1].ki.dwExtraInfo = extra;
    inp[0].ki.dwFlags = 0;
    inp[1].ki.dwFlags = KEYEVENTF_KEYUP;

    SendInput(2, inp, sizeof(INPUT));
}
