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
 * @author Pavel Dolgov
 */
#include "WinManagement.h"

#include <windows.h>
#pragma comment(lib, "user32.lib")


class EnumParam {
public:
    RECT list[100];
    int count;
    HWND target;
    RECT bounds;

    void add(RECT & rc) {
        if (count < 100) {
            list[count] = rc;
            count++;
        }
    }

    bool isOverflow() {
        return count == 100;
    }
};

static BOOL CALLBACK enumWindowsProc(HWND hwnd, LPARAM lParam) {
    bool visible = (GetWindowLongPtrW(hwnd, GWL_STYLE) & WS_VISIBLE) != 0;
    BOOL iconic = IsIconic(hwnd);

    if (visible && !iconic) {
        RECT rc;
        EnumParam * param = (EnumParam *)lParam;

        GetWindowRect(hwnd, &rc);
        IntersectRect(&rc, &(param->bounds), &rc);
        if (rc.right <= rc.left || rc.bottom <= rc.top)
            return TRUE;

        HWND next = hwnd;
        while (next != NULL && next != param->target) {
            next = GetNextWindow(next, GW_HWNDPREV);
        }
        bool below = (next == param->target);

        if (!below) {
            if (param->isOverflow()) {
                return FALSE;
            }
            param->add(rc);
        }
    }
    return TRUE;
}

JNIEXPORT jintArray JNICALL Java_org_apache_harmony_awt_nativebridge_windows_WinManagement_getObscuredRegionImpl
        (JNIEnv * env, jclass clazz, jlong hwnd, jint partX, jint partY, jint partW, jint partH) {

    RECT windowRect;
    GetWindowRect((HWND)hwnd, &windowRect);
    partX += windowRect.left;
    partY += windowRect.top;
    RECT part = { partX, partY, partX + partW, partY + partH };

    EnumParam param;
    param.target = ((HWND)hwnd);
    param.count = 0;
    IntersectRect(&(param.bounds), &windowRect, &part);
    EnumWindows(enumWindowsProc, (LPARAM)(&param));
    if (param.isOverflow()) {
        param.list[0] = param.bounds;
        param.count = 1;
    }

    jintArray array = env->NewIntArray(param.count * 4);
    jboolean isCopy;
    jint * data = env->GetIntArrayElements(array, &isCopy);
    for (int i=0; i<param.count; i++) {
        data[i*4 + 0] = param.list[i].left - windowRect.left;
        data[i*4 + 1] = param.list[i].top - windowRect.top;
        data[i*4 + 2] = param.list[i].right - windowRect.left;
        data[i*4 + 3] = param.list[i].bottom - windowRect.top;
    }
    env->ReleaseIntArrayElements(array, data, 0);
    return array;
}
