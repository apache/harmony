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

#include <X11/Xlib.h>
#include <X11/XKBlib.h>
#include <X11/keysym.h>
#include <X11/extensions/XTest.h>
#include "exceptions.h"
#include "org_apache_harmony_awt_wtk_linux_LinuxWTK.h"

#define CAPS_LOCK_MASK   0x00000001
#define NUM_LOCK_MASK    0x00000002
#define SCROLL_LOCK_MASK 0x00000004

/*
 * Class:     org_apache_harmony_awt_wtk_linux_LinuxWTK
 * Method:    getLockingState
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_apache_harmony_awt_wtk_linux_LinuxWTK_getLockingState
  (JNIEnv *env, jobject cls, jint keyCode){
    Display *dpy = NULL;
    unsigned int states = 0;

    if ((dpy = XOpenDisplay (NULL)) == NULL) {
        throwNewExceptionByName(env, "java/lang/UnsupportedOperationException",
                                       "Cannot open display");
        return 0;
    }

    if (XkbGetIndicatorState(dpy, XkbUseCoreKbd, &states) != Success) {
        XCloseDisplay(dpy);
        throwNewExceptionByName(env, "java/lang/UnsupportedOperationException",
                                       "Error in reading keyboard indicator states");
        return 0;
    }

    XCloseDisplay(dpy);

    switch (keyCode)
    {
        case 20:
            return (states & CAPS_LOCK_MASK);
        case 144:
            return (states & NUM_LOCK_MASK);
        case 145:
            return (states & SCROLL_LOCK_MASK);
        default:
            throwNewExceptionByName(env, "java/lang/IllegalArgumentException",
                                       "Illegal argument");
    }
    
    return 0;
}

/*
 * Class:     org_apache_harmony_awt_wtk_linux_LinuxWTK
 * Method:    setLockingState
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_awt_wtk_linux_LinuxWTK_setLockingState
  (JNIEnv *env, jobject cls, jint keyCode, jboolean on)
{
    Display *dpy = NULL;
    unsigned int states = 0;

    if ((dpy = XOpenDisplay (NULL)) == NULL) {
        throwNewExceptionByName(env, "java/lang/UnsupportedOperationException",
                                       "Cannot open display");
        return;
    }

    if (XkbGetIndicatorState(dpy, XkbUseCoreKbd, &states) != Success) {
        XCloseDisplay(dpy);
        throwNewExceptionByName(env, "java/lang/UnsupportedOperationException",
                                       "Error in reading keyboard indicator states");
        return;
    }

    KeyCode lock_key;
    bool pressed;

    switch (keyCode)
    {
        case 20:
            lock_key = XKeysymToKeycode(dpy,XK_Caps_Lock);
            pressed = states & CAPS_LOCK_MASK;
            break;
        case 144:
            lock_key = XKeysymToKeycode(dpy,XK_Num_Lock);
            pressed = states & NUM_LOCK_MASK;
            break;
        case 145:
            lock_key = XKeysymToKeycode(dpy,XK_Scroll_Lock);
            pressed = states & SCROLL_LOCK_MASK;
            break;
        default:
            XCloseDisplay(dpy);
            throwNewExceptionByName(env, "java/lang/IllegalArgumentException",
                                       "Illegal argument");
            return;
    }

    if ((!pressed && on) || (pressed && !on))
    {
        XTestFakeKeyEvent(dpy, lock_key, true, CurrentTime);
        XTestFakeKeyEvent(dpy, lock_key, false, CurrentTime);
    }

    XCloseDisplay(dpy);
}

